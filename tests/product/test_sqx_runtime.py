from __future__ import annotations

from hashlib import sha256
import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.sqx_runtime import SQX_RUNTIME_SCHEMA, sqx_runtime_descriptor


class SqxRuntimeDescriptorTests(unittest.TestCase):
    def _runtime(self, root: Path, *, launcher: bytes = b"trusted launcher") -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        (root / "sqcli.exe").write_bytes(launcher)
        return root

    def test_unconfigured_runtime_fails_closed(self) -> None:
        payload = sqx_runtime_descriptor(None, None)
        self.assertEqual(payload["schema"], SQX_RUNTIME_SCHEMA)
        self.assertEqual(payload["status"], "unavailable")
        self.assertFalse(payload["build"]["verified"])
        self.assertFalse(payload["inspection"]["available"])
        self.assertFalse(payload["execution"]["available"])
        self.assertEqual(payload["execution"]["reason_code"], "runtime_not_configured")

    def test_verified_build_without_trusted_launcher_stays_read_only(self) -> None:
        with TemporaryDirectory() as tmp:
            payload = sqx_runtime_descriptor(self._runtime(Path(tmp)), None)

        self.assertEqual(payload["status"], "ready")
        self.assertTrue(payload["build"]["verified"])
        self.assertEqual(payload["build"]["observed"], "144.2953")
        self.assertTrue(payload["inspection"]["available"])
        self.assertEqual(payload["launcher"]["status"], "unavailable")
        self.assertFalse(payload["launcher"]["configured"])
        self.assertFalse(payload["launcher"]["verified"])
        self.assertEqual(payload["launcher"]["reason_code"], "trusted_launcher_not_configured")
        self.assertFalse(payload["execution"]["available"])
        self.assertEqual(payload["execution"]["reason_code"], "trusted_launcher_not_configured")

    def test_matching_trusted_launcher_is_verified_but_gateway_stays_disabled(self) -> None:
        launcher = b"trusted launcher"
        trusted = sha256(launcher).hexdigest()
        with TemporaryDirectory() as tmp:
            payload = sqx_runtime_descriptor(self._runtime(Path(tmp), launcher=launcher), trusted)

        self.assertEqual(payload["launcher"]["status"], "ready")
        self.assertTrue(payload["launcher"]["configured"])
        self.assertTrue(payload["launcher"]["verified"])
        self.assertEqual(payload["launcher"]["expected_sha256"], trusted)
        self.assertEqual(payload["launcher"]["observed_sha256"], trusted)
        self.assertFalse(payload["execution"]["available"])
        self.assertTrue(payload["execution"]["launcher_verified"])
        self.assertFalse(payload["execution"]["gateway_available"])
        self.assertEqual(
            payload["execution"]["reason_code"],
            "trusted_native_gateway_not_implemented",
        )

    def test_malformed_trust_digest_fails_before_launcher_identity_is_accepted(self) -> None:
        with TemporaryDirectory() as tmp:
            payload = sqx_runtime_descriptor(self._runtime(Path(tmp)), "not-a-digest")

        self.assertEqual(payload["launcher"]["status"], "invalid")
        self.assertTrue(payload["launcher"]["configured"])
        self.assertFalse(payload["launcher"]["verified"])
        self.assertIsNone(payload["launcher"]["expected_sha256"])
        self.assertIsNone(payload["launcher"]["observed_sha256"])
        self.assertEqual(payload["launcher"]["reason_code"], "trusted_launcher_digest_invalid")
        self.assertFalse(payload["execution"]["available"])

    def test_launcher_hash_mismatch_is_explicit_and_never_execution_ready(self) -> None:
        with TemporaryDirectory() as tmp:
            payload = sqx_runtime_descriptor(self._runtime(Path(tmp)), "0" * 64)

        self.assertEqual(payload["launcher"]["status"], "invalid")
        self.assertFalse(payload["launcher"]["verified"])
        self.assertEqual(payload["launcher"]["expected_sha256"], "0" * 64)
        self.assertIsInstance(payload["launcher"]["observed_sha256"], str)
        self.assertEqual(payload["launcher"]["reason_code"], "sqx_launcher_hash_mismatch")
        self.assertFalse(payload["execution"]["available"])
        self.assertEqual(payload["execution"]["reason_code"], "sqx_launcher_hash_mismatch")

    def test_missing_launcher_is_invalid_when_trust_is_configured(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            (home / "sqcli.exe").unlink()
            payload = sqx_runtime_descriptor(home, "0" * 64)

        self.assertEqual(payload["launcher"]["status"], "invalid")
        self.assertEqual(payload["launcher"]["reason_code"], "sqx_launcher_missing")
        self.assertFalse(payload["execution"]["available"])

    def test_launcher_symlink_escape_is_refused(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx")
            external = root / "external.exe"
            external.write_bytes(b"external launcher")
            launcher = home / "sqcli.exe"
            launcher.unlink()
            try:
                launcher.symlink_to(external)
            except (OSError, NotImplementedError) as exc:
                self.skipTest(f"symlink creation unavailable: {exc}")
            trusted = sha256(external.read_bytes()).hexdigest()
            payload = sqx_runtime_descriptor(home, trusted)

        self.assertEqual(payload["launcher"]["status"], "invalid")
        self.assertEqual(payload["launcher"]["reason_code"], "sqx_launcher_path_escape")
        self.assertFalse(payload["launcher"]["verified"])
        self.assertFalse(payload["execution"]["available"])

    def test_public_descriptor_never_contains_runtime_root(self) -> None:
        launcher = b"trusted launcher"
        trusted = sha256(launcher).hexdigest()
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp), launcher=launcher)
            encoded = json.dumps(sqx_runtime_descriptor(home, trusted), sort_keys=True)

        self.assertNotIn(str(home), encoded)
        self.assertIn("sqcli.exe", encoded)


if __name__ == "__main__":
    unittest.main()
