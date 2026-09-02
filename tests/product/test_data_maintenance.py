from __future__ import annotations

from http.server import ThreadingHTTPServer
from hashlib import sha256
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Thread
import unittest
from unittest.mock import patch
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from zipfile import ZipFile

from tradercockpit.app_server import make_handler
from tradercockpit.data_maintenance import (
    CRASH_LOG_NAME,
    DATA_MAINTENANCE_API_PATH,
    DATA_MAINTENANCE_SCHEMA,
    DATA_ROOT_MANIFEST_SCHEMA,
    MANIFEST_NAME,
    DataMaintenanceError,
    backup_data_root,
    data_maintenance_status,
    record_crash,
    restore_data_root,
)
from tradercockpit.desktop import main as desktop_main
from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.runtime_status import runtime_status_record


SECRET_TOKEN = "sk-or-v1-super-secret-token-value"
OAUTH_TOKEN = "ya29.oauth-refresh-token-value"


class DataMaintenanceTests(unittest.TestCase):
    def _root(self, tmp: str) -> Path:
        root = Path(tmp)
        (root / "research" / "v1" / "current").mkdir(parents=True)
        (root / "research" / "v1" / "current" / "pointer.json").write_text(
            '{"schema":"tc.research-current.v1"}\n',
            encoding="utf-8",
        )
        (root / "google-oauth.json").write_text(
            json.dumps({"schema": "tc.google-oauth.v1", "refresh_token": OAUTH_TOKEN}),
            encoding="utf-8",
        )
        (root / "schwab-oauth.json").write_text(
            json.dumps({"schema": "tc.schwab-oauth.v1", "refresh_token": "schwab-secret-token"}),
            encoding="utf-8",
        )
        (root / "keys.env").write_text(f"OPENROUTER_API_KEY={SECRET_TOKEN}\n", encoding="utf-8")
        backups = root / "backups"
        backups.mkdir()
        (backups / "nested-old.zip").write_bytes(b"old-zip-bytes")
        return root

    def test_status_writes_v1_manifest_and_keeps_secrets_out_of_read_models(self) -> None:
        with TemporaryDirectory() as tmp:
            root = self._root(tmp)
            record = data_maintenance_status(root)
            self.assertEqual(record["schema"], DATA_MAINTENANCE_SCHEMA)
            self.assertEqual(record["status"], "ready")
            self.assertEqual(record["data_root_version"], 1)
            self.assertIsNone(record["last_backup"])
            self.assertIsNone(record["crash_log"])
            manifest = json.loads((root / MANIFEST_NAME).read_text(encoding="utf-8"))
            self.assertEqual(manifest["schema"], DATA_ROOT_MANIFEST_SCHEMA)
            self.assertEqual(manifest["version"], 1)

            encoded = json.dumps(runtime_status_record(data_root=root), sort_keys=True)
            self.assertNotIn("google-oauth.json", encoded)
            self.assertNotIn("schwab-oauth.json", encoded)
            self.assertNotIn(OAUTH_TOKEN, encoded)
            self.assertNotIn(SECRET_TOKEN, encoded)
            self.assertNotIn("keys.env", encoded)
            self.assertNotIn("traceback", encoded)

    def test_backup_zip_contains_custody_json_not_nested_zips_or_keys_env(self) -> None:
        with TemporaryDirectory() as tmp:
            root = self._root(tmp)
            result = backup_data_root(root)
            self.assertEqual(result["schema"], DATA_MAINTENANCE_SCHEMA)
            self.assertGreater(result["file_count"], 0)
            archive = root / "backups" / result["name"]
            self.assertTrue(archive.is_file())
            self.assertEqual(result["sha256"], sha256(archive.read_bytes()).hexdigest())
            with ZipFile(archive) as handle:
                names = set(handle.namelist())
            self.assertIn(MANIFEST_NAME, names)
            self.assertIn("google-oauth.json", names)
            self.assertIn("research/v1/current/pointer.json", names)
            self.assertNotIn("keys.env", names)
            self.assertFalse(any(name.endswith(".zip") and name.replace("\\", "/").startswith("backups/") for name in names))

            status = data_maintenance_status(root)
            self.assertEqual(status["last_backup"]["name"], result["name"])
            self.assertEqual(status["last_backup"]["sha256"], result["sha256"])
            encoded = json.dumps(status, sort_keys=True)
            self.assertNotIn("google-oauth.json", encoded)
            self.assertNotIn(OAUTH_TOKEN, encoded)
            self.assertNotIn(SECRET_TOKEN, encoded)

    def test_unknown_schema_is_refused(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / MANIFEST_NAME).write_text(
                json.dumps({"schema": "tc.data-root.v99", "version": 99}),
                encoding="utf-8",
            )
            status = data_maintenance_status(root)
            self.assertEqual(status["status"], "unavailable")
            self.assertEqual(status["reason_code"], "unknown_schema")
            with self.assertRaises(DataMaintenanceError) as raised:
                backup_data_root(root)
            self.assertEqual(raised.exception.code, "unknown_schema")

    def test_restore_rejects_path_escape_in_archive_name_and_zip_members(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            backups = root / "backups"
            backups.mkdir()
            evil = backups / "evil.zip"
            with ZipFile(evil, "w") as handle:
                handle.writestr(
                    MANIFEST_NAME,
                    json.dumps({"schema": DATA_ROOT_MANIFEST_SCHEMA, "version": 1}),
                )
                handle.writestr("../escaped.txt", "nope")
            with self.assertRaises(DataMaintenanceError) as member:
                restore_data_root(root, "evil.zip")
            self.assertEqual(member.exception.code, "restore_path_escape")
            self.assertFalse((root.parent / "escaped.txt").exists())
            self.assertFalse((root / "escaped.txt").exists())

            for archive in ("../evil.zip", "..\\evil.zip", "C:\\Windows\\evil.zip", "backups/evil.zip"):
                with self.subTest(archive=archive):
                    with self.assertRaises(DataMaintenanceError) as basename:
                        restore_data_root(root, archive)
                    self.assertEqual(basename.exception.code, "restore_path_escape")

    def test_restore_keeps_backups_dir_and_requires_valid_manifest(self) -> None:
        with TemporaryDirectory() as tmp:
            root = self._root(tmp)
            first = backup_data_root(root)
            (root / "scratch.json").write_text("keep-me-not-in-first-zip\n", encoding="utf-8")
            second = backup_data_root(root)
            (root / "marker.json").write_text("after-second\n", encoding="utf-8")
            restored = restore_data_root(root, first["name"])
            self.assertEqual(restored["status"], "ready")
            self.assertTrue((root / "backups" / first["name"]).is_file())
            self.assertTrue((root / "backups" / second["name"]).is_file())
            self.assertTrue((root / "google-oauth.json").is_file())
            self.assertEqual((root / "scratch.json").read_text(encoding="utf-8"), "keep-me-not-in-first-zip\n")

            bad = root / "backups" / "no-manifest.zip"
            with ZipFile(bad, "w") as handle:
                handle.writestr("only.txt", "x")
            with self.assertRaises(DataMaintenanceError) as missing:
                restore_data_root(root, "no-manifest.zip")
            self.assertEqual(missing.exception.code, "manifest_missing")

            unknown = root / "backups" / "unknown-schema.zip"
            with ZipFile(unknown, "w") as handle:
                handle.writestr(MANIFEST_NAME, json.dumps({"schema": "tc.data-root.v2", "version": 2}))
            with self.assertRaises(DataMaintenanceError) as schema:
                restore_data_root(root, "unknown-schema.zip")
            self.assertEqual(schema.exception.code, "unknown_schema")

    def test_crash_log_redacts_secret_env_and_status_omits_traceback(self) -> None:
        environ = {
            "OPENROUTER_API_KEY": SECRET_TOKEN,
            "STRIPE_SECRET_KEY": "sk_test_stripe_secret_value",
            "PATH": "C:\\Windows\\System32",
        }
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            try:
                raise RuntimeError(f"failed with {SECRET_TOKEN}")
            except RuntimeError as exc:
                record_crash(root, exc, environ=environ)
            crash = json.loads((root / CRASH_LOG_NAME).read_text(encoding="utf-8"))
            self.assertEqual(crash["schema"], "tc.crash-log.v1")
            self.assertEqual(crash["exception_type"], "RuntimeError")
            self.assertIn("[redacted]", crash["traceback"])
            self.assertNotIn(SECRET_TOKEN, crash["traceback"])
            self.assertNotIn("sk_test_stripe_secret_value", crash["traceback"])
            self.assertNotIn("OPENROUTER_API_KEY=", json.dumps(crash))
            status = data_maintenance_status(root)
            self.assertEqual(status["crash_log"], {"present": True, "recorded_at": crash["recorded_at"]})
            encoded = json.dumps(status, sort_keys=True)
            self.assertNotIn("traceback", encoded)
            self.assertNotIn(SECRET_TOKEN, encoded)
            self.assertNotIn("RuntimeError", encoded)

    def test_status_shape_unbound_and_ready(self) -> None:
        unbound = data_maintenance_status(None)
        self.assertEqual(
            set(unbound),
            {
                "schema",
                "status",
                "data_root_version",
                "last_backup",
                "crash_log",
                "reason_code",
                "detail",
            },
        )
        self.assertEqual(unbound["status"], "unavailable")
        self.assertEqual(unbound["reason_code"], "data_root_unbound")
        with TemporaryDirectory() as tmp:
            ready = data_maintenance_status(Path(tmp))
        self.assertEqual(set(ready), set(unbound))
        self.assertEqual(ready["status"], "ready")
        self.assertIsNone(ready["reason_code"])

    def test_desktop_main_records_crash_then_reraises(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            with patch("tradercockpit.desktop.run_desktop", side_effect=RuntimeError("desktop exploded")), self.assertRaises(
                RuntimeError
            ):
                desktop_main(["--data-root", str(root), "--web-root", str(root)])
            crash = json.loads((root / CRASH_LOG_NAME).read_text(encoding="utf-8"))
            self.assertEqual(crash["exception_type"], "RuntimeError")
            self.assertIn("desktop exploded", crash["traceback"])

    def test_http_backup_and_restore_are_loopback_basename_only(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            web = root / "web"
            web.mkdir()
            (web / "index.html").write_text("<main>TraderCockpit</main>", encoding="utf-8")
            data = root / "data"
            store = FileResearchCustodyStore(data)
            (data / "note.json").write_text('{"ok":true}\n', encoding="utf-8")
            server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(web, None, None, store))
            thread = Thread(target=server.serve_forever, daemon=True)
            thread.start()
            base = f"http://127.0.0.1:{server.server_port}"
            try:
                request = Request(
                    base + DATA_MAINTENANCE_API_PATH,
                    data=json.dumps({"action": "backup"}).encode("utf-8"),
                    headers={"Content-Type": "application/json"},
                    method="POST",
                )
                with urlopen(request, timeout=2) as response:
                    backup = json.loads(response.read().decode("utf-8"))
                self.assertEqual(backup["status"], "ready")
                self.assertTrue((data / "backups" / backup["name"]).is_file())

                (data / "note.json").write_text('{"ok":false}\n', encoding="utf-8")
                restore = Request(
                    base + DATA_MAINTENANCE_API_PATH,
                    data=json.dumps({"action": "restore", "archive": backup["name"]}).encode("utf-8"),
                    headers={"Content-Type": "application/json"},
                    method="POST",
                )
                with urlopen(restore, timeout=2) as response:
                    restored = json.loads(response.read().decode("utf-8"))
                self.assertEqual(restored["status"], "ready")
                self.assertEqual(json.loads((data / "note.json").read_text(encoding="utf-8")), {"ok": True})

                escaped = Request(
                    base + DATA_MAINTENANCE_API_PATH,
                    data=json.dumps({"action": "restore", "archive": "../evil.zip"}).encode("utf-8"),
                    headers={"Content-Type": "application/json"},
                    method="POST",
                )
                with self.assertRaises(HTTPError) as error:
                    urlopen(escaped, timeout=2)
                self.assertEqual(error.exception.code, 400)
                body = json.loads(error.exception.read().decode("utf-8"))
                self.assertEqual(body["reason_code"], "restore_path_escape")
            finally:
                server.shutdown()
                server.server_close()
                thread.join()


if __name__ == "__main__":
    unittest.main()
