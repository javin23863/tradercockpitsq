from __future__ import annotations

from hashlib import sha256
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

import tradercockpit.sqx_presets as presets
from tradercockpit.app_server import sqx_preset_response
from tradercockpit.sqx_presets import (
    SQX_BUILD,
    SqxPresetDescriptor,
    SqxPresetRuntimeError,
    get_sqx_preset,
    preset_catalog,
    runtime_preset_status,
    verified_sqx_home,
)


class SqxPresetCatalogTests(unittest.TestCase):
    def _runtime(self, root: Path, *, build: str = "2953", major: bytes = b"144") -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text(build, encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(major + b"fixture")
        return root

    def test_catalog_is_exact_and_explicitly_read_only(self) -> None:
        catalog = preset_catalog()
        self.assertEqual(catalog["schema"], "tc.sqx-preset-catalog.v1")
        self.assertEqual(catalog["source_build"], "144.2953")
        self.assertFalse(catalog["execution_available"])
        self.assertEqual(catalog["execution_reason"], "trusted_native_gateway_not_implemented")
        self.assertEqual(
            [(item["preset_id"], item["market"], item["source_sha256"]) for item in catalog["presets"]],
            [
                ("sqx-default-forex", "forex", "92a7b7cdd6065e0f0f50aa5a5c01a6e4d5123cbe77fbc94fd8083dd9d1007f31"),
                ("sqx-default-futures", "futures", "a792e499205470c832e079647f33e52ce11e3a119a28889819b35e84b93b813b"),
                ("sqx-default-stockpicker", "stocks", "4705d1ec2db13f364803f2ec13e54c6b69cbc55fb3daebd3d882523d97d44268"),
            ],
        )
        self.assertTrue(all(item["runtime"]["status"] == "runtime_not_configured" for item in catalog["presets"]))
        self.assertFalse(hasattr(presets, "launch_sqx_preset"))
        self.assertFalse(hasattr(presets, "_start_sqx"))

    def test_unknown_preset_fails_closed(self) -> None:
        with self.assertRaises(KeyError):
            get_sqx_preset("sqx-does-not-exist")
        status, payload = sqx_preset_response(None, "sqx-does-not-exist")
        self.assertEqual(status, 404)
        self.assertEqual(payload["error"], "not_found")

    def test_verified_home_requires_exact_build(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self.assertEqual(verified_sqx_home(home), home.resolve())

        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp), build="9999")
            with self.assertRaises(SqxPresetRuntimeError) as caught:
                verified_sqx_home(home)
            self.assertEqual(caught.exception.code, "sqx_build_mismatch")

        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp), major=b"143")
            with self.assertRaises(SqxPresetRuntimeError) as caught:
                verified_sqx_home(home)
            self.assertEqual(caught.exception.code, "sqx_build_mismatch")

    def test_runtime_preset_is_verified_by_exact_hash(self) -> None:
        payload = b"reviewed preset bytes"
        descriptor = SqxPresetDescriptor(
            preset_id="fixture",
            label="Fixture",
            market="fixture",
            source_relative_path="internal/web/BUILDER/simpleTemplates/Fixture.xml",
            sha256_hex=sha256(payload).hexdigest(),
            source_build=SQX_BUILD,
        )
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            path = descriptor.runtime_path(home)
            path.parent.mkdir(parents=True)

            missing = runtime_preset_status(descriptor, home)
            self.assertEqual(missing["status"], "preset_missing")

            path.write_bytes(b"wrong")
            mismatch = runtime_preset_status(descriptor, home)
            self.assertEqual(mismatch["status"], "hash_mismatch")

            path.write_bytes(payload)
            verified = runtime_preset_status(descriptor, home)
            self.assertTrue(verified["available"])
            self.assertEqual(verified["status"], "verified")
            self.assertEqual(verified["verified_sha256"], descriptor.sha256_hex)
            self.assertEqual(verified["observed_build"], SQX_BUILD)

    def test_preset_symlink_escape_is_refused(self) -> None:
        payload = b"preset"
        descriptor = SqxPresetDescriptor(
            preset_id="fixture",
            label="Fixture",
            market="fixture",
            source_relative_path="internal/web/BUILDER/simpleTemplates/Fixture.xml",
            sha256_hex=sha256(payload).hexdigest(),
        )
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx")
            outside = root / "outside.xml"
            outside.write_bytes(payload)
            path = descriptor.runtime_path(home)
            path.parent.mkdir(parents=True)
            try:
                path.symlink_to(outside)
            except OSError as exc:
                self.skipTest(f"symlinks unavailable: {exc}")
            status = runtime_preset_status(descriptor, home)
            self.assertFalse(status["available"])
            self.assertEqual(status["status"], "preset_path_escape")

    def test_single_preset_response_contains_only_source_and_runtime_identity(self) -> None:
        status, payload = sqx_preset_response(None, "sqx-default-futures")
        self.assertEqual(status, 200)
        preset = payload["preset"]
        self.assertEqual(preset["source_build"], "144.2953")
        self.assertNotIn("reference_commit", preset)
        self.assertNotIn("launch_available", preset["runtime"])


if __name__ == "__main__":
    unittest.main()
