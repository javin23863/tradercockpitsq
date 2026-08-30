from __future__ import annotations

from hashlib import sha256
from http.server import ThreadingHTTPServer
import json
from pathlib import Path
import subprocess
from tempfile import TemporaryDirectory
from threading import Thread
import unittest
from unittest.mock import patch
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from tradercockpit.app_server import make_handler, sqx_preset_launch_response, sqx_preset_response
from tradercockpit.sqx_presets import (
    SQX_BUILD,
    SQX_REFERENCE_COMMIT,
    SqxPresetDescriptor,
    SqxPresetRuntimeError,
    get_sqx_preset,
    launch_sqx_preset,
    preset_catalog,
    runtime_preset_status,
)


class SqxPresetCatalogTests(unittest.TestCase):
    @staticmethod
    def _descriptor(payload: bytes = b"reviewed preset bytes") -> SqxPresetDescriptor:
        return SqxPresetDescriptor(
            preset_id="fixture",
            label="Fixture",
            market="fixture",
            source_relative_path="internal/web/BUILDER/simpleTemplates/Fixture.xml",
            sha256_hex=sha256(payload).hexdigest(),
            source_build=SQX_BUILD,
            reference_commit=SQX_REFERENCE_COMMIT,
        )

    @staticmethod
    def _runtime(
        root: Path,
        descriptor: SqxPresetDescriptor,
        preset_bytes: bytes,
        *,
        launcher_bytes: bytes = b"trusted sqcli fixture",
    ) -> tuple[Path, str]:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal").mkdir(exist_ok=True)
        (root / "internal/SQUANT.dat").write_bytes(b"144")
        (root / "sqcli.exe").write_bytes(launcher_bytes)
        path = descriptor.runtime_path(root)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(preset_bytes)
        return root, sha256(launcher_bytes).hexdigest()

    def test_reviewed_market_presets_are_exact(self) -> None:
        catalog = preset_catalog()
        self.assertEqual(catalog["schema"], "tc.sqx-preset-catalog.v1")
        self.assertEqual(catalog["source_build"], "144.2953")
        self.assertEqual(catalog["reference_commit"], SQX_REFERENCE_COMMIT)
        self.assertEqual(
            [(item["preset_id"], item["market"], item["source_sha256"]) for item in catalog["presets"]],
            [
                ("sqx-default-forex", "forex", "92a7b7cdd6065e0f0f50aa5a5c01a6e4d5123cbe77fbc94fd8083dd9d1007f31"),
                ("sqx-default-futures", "futures", "a792e499205470c832e079647f33e52ce11e3a119a28889819b35e84b93b813b"),
                ("sqx-default-stockpicker", "stocks", "4705d1ec2db13f364803f2ec13e54c6b69cbc55fb3daebd3d882523d97d44268"),
            ],
        )
        self.assertTrue(all(item["runtime"]["status"] == "runtime_not_configured" for item in catalog["presets"]))
        self.assertTrue(all(item["runtime"]["launch_available"] is False for item in catalog["presets"]))

    def test_unknown_preset_fails_closed(self) -> None:
        with self.assertRaises(KeyError):
            get_sqx_preset("sqx-does-not-exist")
        status, payload = sqx_preset_response(None, "sqx-does-not-exist")
        self.assertEqual(status, 404)
        self.assertEqual(payload["error"], "not_found")

    def test_runtime_preset_and_launcher_identity_are_verified_separately(self) -> None:
        preset_bytes = b"reviewed preset bytes"
        descriptor = self._descriptor(preset_bytes)

        with TemporaryDirectory() as tmp:
            home = Path(tmp)
            (home / "internal/web/SQUANT").mkdir(parents=True)
            (home / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
            (home / "internal").mkdir(exist_ok=True)
            (home / "internal/SQUANT.dat").write_bytes(b"144")
            launcher_bytes = b"trusted launcher"
            (home / "sqcli.exe").write_bytes(launcher_bytes)
            launcher_hash = sha256(launcher_bytes).hexdigest()
            path = descriptor.runtime_path(home)
            path.parent.mkdir(parents=True)

            missing = runtime_preset_status(descriptor, home)
            self.assertFalse(missing["available"])
            self.assertEqual(missing["status"], "preset_missing")

            path.write_bytes(b"wrong")
            mismatch = runtime_preset_status(descriptor, home)
            self.assertFalse(mismatch["available"])
            self.assertEqual(mismatch["status"], "hash_mismatch")

            path.write_bytes(preset_bytes)
            untrusted = runtime_preset_status(descriptor, home)
            self.assertTrue(untrusted["available"])
            self.assertFalse(untrusted["launch_available"])
            self.assertEqual(untrusted["launch_status"], "launcher_identity_unconfigured")
            self.assertEqual(untrusted["launcher_sha256"], launcher_hash)

            wrong_launcher = runtime_preset_status(
                descriptor,
                home,
                expected_launcher_sha256="0" * 64,
            )
            self.assertFalse(wrong_launcher["launch_available"])
            self.assertEqual(wrong_launcher["launch_status"], "launcher_hash_mismatch")

            verified = runtime_preset_status(
                descriptor,
                home,
                expected_launcher_sha256=launcher_hash,
            )
            self.assertTrue(verified["available"])
            self.assertTrue(verified["launch_available"])
            self.assertEqual(verified["launch_status"], "verified")

    def test_single_preset_response_preserves_source_identity(self) -> None:
        status, payload = sqx_preset_response(None, "sqx-default-futures")
        self.assertEqual(status, 200)
        preset = payload["preset"]
        self.assertEqual(preset["source_build"], "144.2953")
        self.assertEqual(
            preset["source_relative_path"],
            "internal/web/BUILDER/simpleTemplates/DefaultFutures.xml",
        )
        self.assertEqual(
            preset["source_sha256"],
            "a792e499205470c832e079647f33e52ce11e3a119a28889819b35e84b93b813b",
        )
        self.assertFalse(preset["runtime"]["available"])

    def test_malformed_build_marker_is_structured_runtime_refusal(self) -> None:
        preset_bytes = b"fixture preset"
        descriptor = self._descriptor(preset_bytes)
        with TemporaryDirectory() as tmp:
            home, launcher_hash = self._runtime(Path(tmp), descriptor, preset_bytes)
            (home / "internal/SQUANT.dat").write_bytes(b"\xff\xfe\xfd")

            status = runtime_preset_status(
                descriptor,
                home,
                expected_launcher_sha256=launcher_hash,
            )
            self.assertTrue(status["available"])
            self.assertFalse(status["launch_available"])
            self.assertEqual(status["launch_status"], "sqx_build_invalid")

            with patch("tradercockpit.sqx_presets.get_sqx_preset", return_value=descriptor):
                with self.assertRaises(SqxPresetRuntimeError) as caught:
                    launch_sqx_preset(
                        descriptor.preset_id,
                        home,
                        expected_launcher_sha256=launcher_hash,
                    )
            self.assertEqual(caught.exception.code, "sqx_build_invalid")

    def test_launch_uses_direct_cli_and_immutable_preset_snapshot(self) -> None:
        preset_bytes = b"exact source preset"
        descriptor = self._descriptor(preset_bytes)
        submitted: list[list[str]] = []

        with TemporaryDirectory() as tmp:
            home, launcher_hash = self._runtime(Path(tmp), descriptor, preset_bytes)
            source = descriptor.runtime_path(home)

            def runner(command, **kwargs):
                submitted.append(list(command))
                self.assertEqual(kwargs["cwd"], str(home))
                self.assertTrue(kwargs["capture_output"])
                self.assertTrue(kwargs["text"])
                self.assertFalse(kwargs["check"])
                if len(submitted) == 1:
                    file_arg = next(item for item in command if item.startswith("file="))
                    staged = Path(file_arg.split("=", 1)[1])
                    self.assertNotEqual(staged, source)
                    self.assertEqual(staged.read_bytes(), preset_bytes)
                    # Mutating the runtime source after snapshot verification must not
                    # change the bytes supplied to this launch.
                    source.write_bytes(b"changed after snapshot")
                    self.assertEqual(staged.read_bytes(), preset_bytes)
                return subprocess.CompletedProcess(command, 0, "", "")

            with patch("tradercockpit.sqx_presets.get_sqx_preset", return_value=descriptor):
                receipt = launch_sqx_preset(
                    descriptor.preset_id,
                    home,
                    expected_launcher_sha256=launcher_hash,
                    runner=runner,
                )

        self.assertEqual(receipt["schema"], "tc.sqx-preset-launch.v1")
        self.assertEqual(receipt["state"], "submitted")
        self.assertEqual(receipt["launcher_sha256"], launcher_hash)
        self.assertEqual(receipt["control_requests_submitted"], 2)
        self.assertEqual(len(submitted), 2)
        self.assertEqual(submitted[0][1:4], ["-project", "action=loadconfig", "name=Builder"])
        self.assertEqual(submitted[1][1:], ["-project", "action=start", "name=Builder"])
        self.assertEqual(
            [(item["action"], item["state"], item["exit_code"]) for item in receipt["receipts"]],
            [("loadconfig", "completed", 0), ("start", "completed", 0)],
        )

    def test_launcher_execution_error_is_structured(self) -> None:
        preset_bytes = b"fixture preset"
        descriptor = self._descriptor(preset_bytes)
        with TemporaryDirectory() as tmp:
            home, launcher_hash = self._runtime(Path(tmp), descriptor, preset_bytes)

            def runner(_command, **_kwargs):
                raise OSError("cannot execute")

            with patch("tradercockpit.sqx_presets.get_sqx_preset", return_value=descriptor):
                with self.assertRaises(SqxPresetRuntimeError) as caught:
                    launch_sqx_preset(
                        descriptor.preset_id,
                        home,
                        expected_launcher_sha256=launcher_hash,
                        runner=runner,
                    )

        self.assertEqual(caught.exception.code, "sqx_command_failed")
        self.assertEqual(len(caught.exception.receipts), 1)
        self.assertEqual(caught.exception.receipts[0]["state"], "launch_failed")

    def test_partial_native_launch_receipt_is_preserved_by_api(self) -> None:
        preset_bytes = b"fixture preset"
        descriptor = self._descriptor(preset_bytes)
        calls = 0
        with TemporaryDirectory() as tmp:
            home, launcher_hash = self._runtime(Path(tmp), descriptor, preset_bytes)

            def runner(command, **_kwargs):
                nonlocal calls
                calls += 1
                return subprocess.CompletedProcess(command, 0 if calls == 1 else 7, "", "")

            def launcher(preset_id: str, sqx_home: Path | str | None):
                return launch_sqx_preset(
                    preset_id,
                    sqx_home,
                    expected_launcher_sha256=launcher_hash,
                    runner=runner,
                )

            with patch("tradercockpit.sqx_presets.get_sqx_preset", return_value=descriptor):
                status, payload = sqx_preset_launch_response(
                    home,
                    descriptor.preset_id,
                    launcher=launcher,
                )

        self.assertEqual(status, 502)
        self.assertEqual(payload["reason_code"], "sqx_command_rejected")
        self.assertTrue(payload["partial_side_effect"])
        self.assertEqual(payload["control_requests_completed"], 1)
        self.assertEqual(
            [(item["action"], item["state"]) for item in payload["receipts"]],
            [("loadconfig", "completed"), ("start", "rejected")],
        )

    def test_launch_api_maps_runtime_refusal_without_fabricating_success(self) -> None:
        def refusing_launcher(preset_id: str, sqx_home: Path | str | None):
            raise SqxPresetRuntimeError(
                "launcher_identity_unconfigured",
                "trusted SQX launcher identity is not configured",
            )

        status, payload = sqx_preset_launch_response(
            None,
            "sqx-default-forex",
            launcher=refusing_launcher,
        )
        self.assertEqual(status, 503)
        self.assertEqual(payload["error"], "producer_not_configured")
        self.assertEqual(payload["reason_code"], "launcher_identity_unconfigured")

    def test_launch_http_boundary_rejects_cross_site_form_and_accepts_json_object(self) -> None:
        calls: list[str] = []

        def launcher(preset_id: str, _sqx_home: Path | str | None):
            calls.append(preset_id)
            return {
                "schema": "tc.sqx-preset-launch.v1",
                "preset_id": preset_id,
                "market": "forex",
                "sqx_build": SQX_BUILD,
                "source_sha256": get_sqx_preset(preset_id).sha256_hex,
                "launcher_sha256": "a" * 64,
                "project": "Builder",
                "state": "submitted",
                "control_requests_submitted": 2,
                "receipts": [
                    {"sequence": 1, "action": "loadconfig", "state": "completed", "exit_code": 0},
                    {"sequence": 2, "action": "start", "state": "completed", "exit_code": 0},
                ],
            }

        with TemporaryDirectory() as web_tmp:
            Path(web_tmp, "index.html").write_text("<main>TraderCockpit</main>", encoding="utf-8")
            server = ThreadingHTTPServer(
                ("127.0.0.1", 0),
                make_handler(Path(web_tmp), None, None, sqx_launcher=launcher),
            )
            thread = Thread(target=server.serve_forever, daemon=True)
            thread.start()
            endpoint = (
                f"http://127.0.0.1:{server.server_port}"
                "/api/sqx-presets/sqx-default-forex/launch"
            )
            try:
                form_request = Request(
                    endpoint,
                    data=b"x=1",
                    method="POST",
                    headers={"Content-Type": "application/x-www-form-urlencoded"},
                )
                with self.assertRaises(HTTPError) as caught:
                    urlopen(form_request)
                self.assertEqual(caught.exception.code, 400)
                self.assertEqual(calls, [])

                json_request = Request(
                    endpoint,
                    data=b"{}",
                    method="POST",
                    headers={"Content-Type": "application/json"},
                )
                with urlopen(json_request) as response:
                    payload = json.loads(response.read())
                self.assertEqual(response.status, 202)
                self.assertEqual(payload["state"], "submitted")
                self.assertEqual(calls, ["sqx-default-forex"])
            finally:
                server.shutdown()
                server.server_close()
                thread.join()


if __name__ == "__main__":
    unittest.main()
