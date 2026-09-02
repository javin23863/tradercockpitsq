from __future__ import annotations

import os
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch


from tradercockpit.windows_release import (
    EXECUTABLE_NAME,
    SIGNING_CERT_PATH_ENV,
    SIGNING_CERT_PASSWORD_ENV,
    WindowsReleaseError,
    apply_trusted_update,
    build_install_manifest,
    read_install_manifest,
    rollback_install,
    sha256_file,
    sign_executable,
    signing_config_from_environ,
    signing_status,
    write_install_manifest,
)


class WindowsReleaseTests(unittest.TestCase):
    def test_signing_status_reports_signing_not_configured_without_env(self) -> None:
        with patch.dict(os.environ, {}, clear=True):
            status = signing_status()
        self.assertEqual(status["reason_code"], "signing_not_configured")
        self.assertEqual(status["status"], "not_configured")

    def test_signing_config_requires_cert_and_password(self) -> None:
        with patch.dict(os.environ, {SIGNING_CERT_PATH_ENV: "/tmp/cert.pfx"}, clear=True):
            self.assertIsNone(signing_config_from_environ())
        with patch.dict(
            os.environ,
            {SIGNING_CERT_PATH_ENV: "/tmp/cert.pfx", SIGNING_CERT_PASSWORD_ENV: "secret"},
            clear=True,
        ):
            config = signing_config_from_environ()
        self.assertIsNotNone(config)
        assert config is not None
        self.assertEqual(config.cert_path, Path("/tmp/cert.pfx").expanduser())

    def test_sign_executable_skips_when_signing_not_configured(self) -> None:
        with TemporaryDirectory() as tmp:
            exe = Path(tmp) / EXECUTABLE_NAME
            exe.write_bytes(b"payload")
            with patch.dict(os.environ, {}, clear=True):
                result = sign_executable(exe)
        self.assertEqual(result["status"], "skipped")
        self.assertEqual(result["reason_code"], "signing_not_configured")

    def test_install_manifest_round_trip(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp) / "Programs" / "TraderCockpitSQ"
            root.mkdir(parents=True)
            manifest = build_install_manifest(
                version="0.1.0",
                install_root=root,
                executable_sha256="abc",
            )
            write_install_manifest(root, manifest)
            loaded = read_install_manifest(root)
        self.assertEqual(loaded["schema"], "tc.windows-install.v1")
        self.assertEqual(loaded["version"], "0.1.0")
        self.assertEqual(loaded["executable_sha256"], "abc")

    def test_apply_trusted_update_rejects_untrusted_payload(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp) / "install"
            root.mkdir()
            installed = root / EXECUTABLE_NAME
            installed.write_bytes(b"v1")
            write_install_manifest(
                root,
                build_install_manifest(
                    version="0.1.0",
                    install_root=root,
                    executable_sha256=sha256_file(installed),
                ),
            )
            payload = Path(tmp) / EXECUTABLE_NAME
            payload.write_bytes(b"v2")
            with self.assertRaises(WindowsReleaseError) as ctx:
                apply_trusted_update(
                    root,
                    payload,
                    expected_sha256="0" * 64,
                    version="0.2.0",
                )
        self.assertEqual(ctx.exception.code, "update_payload_untrusted")

    def test_update_and_rollback_restore_previous_payload(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp) / "install"
            root.mkdir()
            v1 = root / EXECUTABLE_NAME
            v1.write_bytes(b"version-one")
            write_install_manifest(
                root,
                build_install_manifest(
                    version="0.1.0",
                    install_root=root,
                    executable_sha256=sha256_file(v1),
                ),
            )

            payload = Path(tmp) / EXECUTABLE_NAME
            payload.write_bytes(b"version-two")
            digest_v2 = sha256_file(payload)
            apply_trusted_update(root, payload, expected_sha256=digest_v2, version="0.2.0")

            updated = (root / EXECUTABLE_NAME).read_bytes()
            self.assertEqual(updated, b"version-two")
            manifest = read_install_manifest(root)
            self.assertEqual(manifest["version"], "0.2.0")
            self.assertIn("previous", manifest)

            rollback_install(root)
            restored = (root / EXECUTABLE_NAME).read_bytes()
            self.assertEqual(restored, b"version-one")
            rolled = read_install_manifest(root)
            self.assertEqual(rolled["version"], "0.1.0")


if __name__ == "__main__":
    unittest.main()
