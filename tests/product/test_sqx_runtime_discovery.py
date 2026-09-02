from __future__ import annotations

from hashlib import sha256
from http.server import ThreadingHTTPServer
import json
import os
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Thread
import unittest
from unittest.mock import patch
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from tradercockpit.app_server import make_handler
from tradercockpit.native_runtime_config import load_native_runtime_config
from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.sqx_presets import SQX_BUILD
from tradercockpit.sqx_runtime_discovery import (
    SQX_RUNTIME_DISCOVERY_SCHEMA,
    bind_discovered_runtime,
    candidate_id_for,
    clear_saved_runtime,
    discover_sqx_runtimes,
    inspect_sqx_home,
    native_runtime_discovery_record,
    NativeRuntimeDiscoveryError,
)


def _write_home(root: Path, *, build: str = "2953", version: bytes = b"144fixture", launcher: bytes | None = b"trusted launcher") -> Path:
    (root / "internal/web/SQUANT").mkdir(parents=True)
    (root / "internal/web/SQUANT/build.dat").write_text(build, encoding="utf-8")
    (root / "internal/SQUANT.dat").write_bytes(version)
    if launcher is not None:
        (root / "sqcli.exe").write_bytes(launcher)
    return root


class SqxRuntimeDiscoveryTests(unittest.TestCase):
    def test_bindable_home_is_discovered_without_launching_sqx(self) -> None:
        with TemporaryDirectory() as tmp, patch(
            "tradercockpit.sqx_gateway.SqxNativeControlGateway.launch_builder",
            side_effect=AssertionError("discovery launched SQX"),
        ):
            home = _write_home(Path(tmp) / "sqx")
            found = discover_sqx_runtimes(search_roots=(home,))
        self.assertEqual(len(found), 1)
        self.assertTrue(found[0]["bindable"])
        self.assertEqual(found[0]["observed_build"], SQX_BUILD)
        self.assertEqual(found[0]["launcher_sha256"], sha256(b"trusted launcher").hexdigest())
        self.assertEqual(found[0]["candidate_id"], candidate_id_for(home))

    def test_wrong_build_is_listed_but_not_bindable(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _write_home(Path(tmp) / "sqx", build="999")
            record = inspect_sqx_home(home)
        self.assertFalse(record["bindable"])
        self.assertEqual(record["reason_code"], "sqx_build_mismatch")
        self.assertIsNone(record["observed_build"])

    def test_missing_launcher_is_not_bindable(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _write_home(Path(tmp) / "sqx", launcher=None)
            record = inspect_sqx_home(home)
            found = discover_sqx_runtimes(search_roots=(home,))
        self.assertFalse(record["bindable"])
        self.assertEqual(record["reason_code"], "sqx_launcher_missing")
        self.assertEqual(found, [])

    def test_bind_writes_observed_launcher_digest(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = _write_home(root / "sqx")
            data_root = root / "data"
            payload = bind_discovered_runtime(
                data_root,
                candidate_id_for(home),
                search_roots=(home,),
            )
            saved_home, digest = load_native_runtime_config(data_root)
        self.assertEqual(payload["schema"], SQX_RUNTIME_DISCOVERY_SCHEMA)
        self.assertEqual(saved_home, home.resolve())
        self.assertEqual(digest, sha256(b"trusted launcher").hexdigest())
        self.assertEqual(payload["saved"]["launcher_sha256"], digest)
        self.assertTrue(payload["saved"]["matches_observed_launcher"])

    def test_process_pin_refuses_bind(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _write_home(Path(tmp) / "sqx")
            with self.assertRaises(NativeRuntimeDiscoveryError) as raised:
                bind_discovered_runtime(
                    Path(tmp) / "data",
                    candidate_id_for(home),
                    process_home=home,
                    search_roots=(home,),
                )
        self.assertEqual(raised.exception.code, "process_runtime_pinned")

    def test_clear_removes_saved_pointer(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = _write_home(root / "sqx")
            data_root = root / "data"
            bind_discovered_runtime(data_root, candidate_id_for(home), search_roots=(home,))
            payload = clear_saved_runtime(data_root, search_roots=(home,))
            self.assertIsNone(payload["saved"])
            self.assertEqual(load_native_runtime_config(data_root), (None, None))

    def test_unknown_candidate_id_is_refused(self) -> None:
        with TemporaryDirectory() as tmp:
            with self.assertRaises(NativeRuntimeDiscoveryError) as raised:
                bind_discovered_runtime(Path(tmp), "tc-sqx-home:sha256:" + ("ab" * 32))
        self.assertEqual(raised.exception.code, "candidate_not_found")

    def test_recovery_asks_to_bind_when_a_home_is_found(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _write_home(Path(tmp) / "sqx")
            record = native_runtime_discovery_record(Path(tmp) / "data", search_roots=(home,))
        self.assertEqual(record["recovery"]["action"], "bind")
        self.assertEqual(record["recovery"]["reason_code"], "runtime_not_configured")
        self.assertFalse(record["process_pinned"])


class SqxRuntimeDiscoveryHttpTests(unittest.TestCase):
    def _server(self, root: Path, *, sqx_home=None, trusted=None, store=True):
        web = root / "web"
        web.mkdir(parents=True)
        (web / "index.html").write_text("<main>TraderCockpit</main>", encoding="utf-8")
        research_store = FileResearchCustodyStore(root / "data") if store else None
        server = ThreadingHTTPServer(
            ("127.0.0.1", 0),
            make_handler(web, sqx_home, trusted, research_store),
        )
        thread = Thread(target=server.serve_forever, daemon=True)
        thread.start()
        return server, thread, research_store

    def _json(self, url: str, *, method: str = "GET", payload: dict[str, object] | None = None):
        headers = {"Accept": "application/json"}
        data = None
        if payload is not None:
            headers["Content-Type"] = "application/json"
            data = json.dumps(payload).encode("utf-8")
        request = Request(url, data=data, method=method, headers=headers)
        try:
            with urlopen(request, timeout=2) as response:
                return response.status, json.loads(response.read().decode("utf-8"))
        except HTTPError as exc:
            return exc.code, json.loads(exc.read().decode("utf-8"))

    def test_http_discovers_and_binds_by_candidate_id_only(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = _write_home(root / "sqx")
            server, thread, store = self._server(root)
            base = f"http://127.0.0.1:{server.server_port}"
            try:
                with patch.dict(os.environ, {"SQX_HOME": str(home)}), patch(
                    "tradercockpit.sqx_gateway.SqxNativeControlGateway.launch_builder",
                    side_effect=AssertionError("native-runtime HTTP launched SQX"),
                ):
                    status, payload = self._json(base + "/api/native-runtime")
                    self.assertEqual(status, 200)
                    self.assertEqual(payload["schema"], SQX_RUNTIME_DISCOVERY_SCHEMA)
                    match = next(item for item in payload["candidates"] if Path(item["home_path"]) == home.resolve())
                    self.assertTrue(match["bindable"])

                    status, rejected = self._json(
                        base + "/api/native-runtime",
                        method="POST",
                        payload={"action": "bind", "candidate_id": match["candidate_id"], "home_path": str(home)},
                    )
                    self.assertEqual(status, 400)
                    self.assertEqual(rejected["reason_code"], "candidate_id_invalid")

                    status, bound = self._json(
                        base + "/api/native-runtime",
                        method="POST",
                        payload={"action": "bind", "candidate_id": match["candidate_id"]},
                    )
                    self.assertEqual(status, 200)
                    self.assertEqual(bound["saved"]["candidate_id"], match["candidate_id"])
                    saved_home, digest = load_native_runtime_config(store.root)
                    self.assertEqual(saved_home, home.resolve())
                    self.assertEqual(digest, match["launcher_sha256"])

                    status, cleared = self._json(
                        base + "/api/native-runtime",
                        method="POST",
                        payload={"action": "clear"},
                    )
                    self.assertEqual(status, 200)
                    self.assertIsNone(cleared["saved"])
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

    def test_http_process_pin_refuses_bind(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = _write_home(root / "sqx")
            digest = sha256(b"trusted launcher").hexdigest()
            server, thread, _store = self._server(root, sqx_home=home, trusted=digest)
            base = f"http://127.0.0.1:{server.server_port}"
            try:
                with patch.dict(os.environ, {"SQX_HOME": str(home)}):
                    status, payload = self._json(base + "/api/native-runtime")
                    self.assertEqual(status, 200)
                    self.assertTrue(payload["process_pinned"])
                    match = next(item for item in payload["candidates"] if Path(item["home_path"]) == home.resolve())
                    status, refused = self._json(
                        base + "/api/native-runtime",
                        method="POST",
                        payload={"action": "bind", "candidate_id": match["candidate_id"]},
                    )
                self.assertEqual(status, 409)
                self.assertEqual(refused["reason_code"], "process_runtime_pinned")
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

    def test_http_unknown_candidate_and_query_fail_closed(self) -> None:
        with TemporaryDirectory() as tmp:
            server, thread, _store = self._server(Path(tmp))
            base = f"http://127.0.0.1:{server.server_port}"
            try:
                status, payload = self._json(base + "/api/native-runtime?refresh=true")
                self.assertEqual(status, 400)
                status, payload = self._json(
                    base + "/api/native-runtime",
                    method="POST",
                    payload={"action": "bind", "candidate_id": "tc-sqx-home:sha256:" + ("cd" * 32)},
                )
                self.assertEqual(status, 409)
                self.assertEqual(payload["reason_code"], "candidate_not_found")
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

    def test_http_write_requires_data_root(self) -> None:
        with TemporaryDirectory() as tmp:
            server, thread, _store = self._server(Path(tmp), store=False)
            base = f"http://127.0.0.1:{server.server_port}"
            try:
                status, payload = self._json(
                    base + "/api/native-runtime",
                    method="POST",
                    payload={"action": "clear"},
                )
                self.assertEqual(status, 503)
                self.assertEqual(payload["reason_code"], "session_store_unbound")
            finally:
                server.shutdown()
                server.server_close()
                thread.join()


if __name__ == "__main__":
    unittest.main()
