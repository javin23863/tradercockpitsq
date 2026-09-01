from __future__ import annotations

from hashlib import sha256
from http.server import ThreadingHTTPServer
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Thread
import unittest
from unittest.mock import patch
from urllib.parse import urlsplit
from urllib.request import Request, urlopen

from tradercockpit.app_server import make_handler
from tradercockpit.desktop import start_desktop_server


class NativeRuntimeHttpTests(unittest.TestCase):
    def _fixture(self, root: Path) -> tuple[Path, Path, str]:
        web = root / "web"
        web.mkdir(parents=True)
        (web / "index.html").write_text("<main>TraderCockpit</main>", encoding="utf-8")

        home = root / "sqx"
        (home / "internal/web/SQUANT").mkdir(parents=True)
        (home / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (home / "internal/SQUANT.dat").write_bytes(b"144fixture")
        launcher = b"trusted launcher"
        (home / "sqcli.exe").write_bytes(launcher)
        return web, home, sha256(launcher).hexdigest()

    def _read_status(self, url: str) -> dict[str, object]:
        with urlopen(url, timeout=2) as response:
            self.assertEqual(response.status, 200)
            return json.loads(response.read().decode("utf-8"))

    def test_canonical_server_uses_server_side_trusted_launcher_identity(self) -> None:
        with TemporaryDirectory() as tmp:
            web, home, trusted = self._fixture(Path(tmp))
            server = ThreadingHTTPServer(
                ("127.0.0.1", 0),
                make_handler(web, home, trusted),
            )
            thread = Thread(target=server.serve_forever, daemon=True)
            thread.start()
            try:
                payload = self._read_status(
                    f"http://127.0.0.1:{server.server_port}/api/status"
                )
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

        research = payload["research_backend"]
        self.assertTrue(research["execution"]["launcher_verified"])
        self.assertEqual(research["execution"]["launcher_sha256"], trusted)
        self.assertTrue(research["execution"]["gateway_implemented"])
        self.assertTrue(research["execution"]["gateway_available"])
        self.assertTrue(research["execution"]["available"])
        self.assertIsNone(research["execution"]["reason_code"])
        self.assertTrue(research["execution"]["requires_approved_configuration"])

    def test_desktop_uses_the_same_trusted_launcher_status_path(self) -> None:
        with TemporaryDirectory() as tmp:
            web, home, trusted = self._fixture(Path(tmp))
            runtime = start_desktop_server(
                web_root=web,
                data_root=Path(tmp) / "data",
                sqx_home=home,
                trusted_launcher_sha256=trusted,
            )
            try:
                parsed = urlsplit(runtime.url)
                payload = self._read_status(
                    f"{parsed.scheme}://{parsed.netloc}/api/status"
                )
            finally:
                runtime.close()

        research = payload["research_backend"]
        self.assertTrue(research["runtime"]["launcher"]["verified"])
        self.assertEqual(research["runtime"]["launcher"]["observed_sha256"], trusted)
        self.assertTrue(research["runtime"]["execution"]["gateway_implemented"])
        self.assertTrue(research["runtime"]["execution"]["gateway_available"])
        self.assertTrue(research["runtime"]["execution"]["available"])
        self.assertFalse(research["runtime"]["execution"]["launch_authorization"])
        self.assertTrue(research["runtime"]["execution"]["requires_approved_configuration"])

    def test_desktop_status_does_not_call_native_launcher(self) -> None:
        with TemporaryDirectory() as tmp, patch(
            "tradercockpit.sqx_gateway.SqxNativeControlGateway.launch_builder",
            side_effect=AssertionError("status path launched SQX"),
        ):
            web, home, trusted = self._fixture(Path(tmp))
            runtime = start_desktop_server(
                web_root=web,
                data_root=Path(tmp) / "data",
                sqx_home=home,
                trusted_launcher_sha256=trusted,
            )
            try:
                parsed = urlsplit(runtime.url)
                payload = self._read_status(f"{parsed.scheme}://{parsed.netloc}/api/status")
            finally:
                runtime.close()
        self.assertTrue(payload["research_backend"]["runtime"]["launcher"]["verified"])
        self.assertFalse(payload["research_backend"]["runtime"]["execution"]["launch_authorization"])

    def test_desktop_explicit_native_job_reaches_trusted_launcher(self) -> None:
        captured: dict[str, object] = {}

        def fake_launch(store, sqx_home, trusted_launcher_sha256, **kwargs):
            captured["sqx_home"] = Path(sqx_home)
            captured["trusted"] = trusted_launcher_sha256
            captured["kwargs"] = kwargs
            return {
                "schema": "tc.research-native-job.v1",
                "reused": False,
                "state": "submitted",
            }

        with TemporaryDirectory() as tmp, patch(
            "tradercockpit.app_server.launch_approved_builder_configuration",
            side_effect=fake_launch,
        ):
            web, home, trusted = self._fixture(Path(tmp))
            runtime = start_desktop_server(
                web_root=web,
                data_root=Path(tmp) / "data",
                sqx_home=home,
                trusted_launcher_sha256=trusted,
            )
            try:
                parsed = urlsplit(runtime.url)
                base = f"{parsed.scheme}://{parsed.netloc}"
                request = Request(
                    f"{base}/api/research/native-jobs",
                    data=json.dumps(
                        {
                            "action": "launch-builder",
                            "configuration_entity_id": "configuration:00000000-0000-4000-8000-000000000001",
                            "expected_configuration_revision": "configuration:" + "a" * 64,
                        }
                    ).encode("utf-8"),
                    method="POST",
                    headers={
                        "Content-Type": "application/json",
                        "Origin": base,
                    },
                )
                with urlopen(request, timeout=2) as response:
                    self.assertEqual(response.status, 201)
                    payload = json.loads(response.read().decode("utf-8"))
            finally:
                runtime.close()

        self.assertEqual(payload["state"], "submitted")
        self.assertEqual(captured["sqx_home"], home.resolve())
        self.assertEqual(captured["trusted"], trusted)
        kwargs = captured["kwargs"]
        assert isinstance(kwargs, dict)
        self.assertEqual(
            kwargs["configuration_entity_id"],
            "configuration:00000000-0000-4000-8000-000000000001",
        )


if __name__ == "__main__":
    unittest.main()
