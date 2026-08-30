from __future__ import annotations

from hashlib import sha256
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.app_server import sqx_preset_response
from tradercockpit.sqx_presets import (
    SQX_BUILD,
    SQX_REFERENCE_COMMIT,
    SqxPresetDescriptor,
    get_sqx_preset,
    preset_catalog,
    runtime_preset_status,
)


class SqxPresetCatalogTests(unittest.TestCase):
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

    def test_unknown_preset_fails_closed(self) -> None:
        with self.assertRaises(KeyError):
            get_sqx_preset("sqx-does-not-exist")
        status, payload = sqx_preset_response(None, "sqx-does-not-exist")
        self.assertEqual(status, 404)
        self.assertEqual(payload["error"], "not_found")

    def test_runtime_file_is_verified_by_exact_hash(self) -> None:
        payload = b"reviewed preset bytes"
        digest = sha256(payload).hexdigest()
        descriptor = SqxPresetDescriptor(
            preset_id="fixture",
            label="Fixture",
            market="fixture",
            source_relative_path="internal/web/BUILDER/simpleTemplates/Fixture.xml",
            sha256_hex=digest,
            source_build=SQX_BUILD,
            reference_commit=SQX_REFERENCE_COMMIT,
        )

        with TemporaryDirectory() as tmp:
            home = Path(tmp)
            path = descriptor.runtime_path(home)
            path.parent.mkdir(parents=True)

            missing = runtime_preset_status(descriptor, home)
            self.assertFalse(missing["available"])
            self.assertEqual(missing["status"], "preset_missing")

            path.write_bytes(b"wrong")
            mismatch = runtime_preset_status(descriptor, home)
            self.assertFalse(mismatch["available"])
            self.assertEqual(mismatch["status"], "hash_mismatch")
            self.assertNotEqual(mismatch["verified_sha256"], digest)

            path.write_bytes(payload)
            verified = runtime_preset_status(descriptor, home)
            self.assertTrue(verified["available"])
            self.assertEqual(verified["status"], "verified")
            self.assertEqual(verified["verified_sha256"], digest)

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


if __name__ == "__main__":
    unittest.main()
