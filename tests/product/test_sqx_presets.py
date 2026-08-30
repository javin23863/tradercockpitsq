from __future__ import annotations

from hashlib import sha256
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

from tradercockpit.app_server import sqx_preset_launch_response, sqx_preset_response
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
            self.assertFalse(verified["launch_available"])
            self.assertEqual(verified["launch_status"], "sqx_launcher_missing")

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

    def test_launch_submits_exact_native_builder_commands(self) -> None:
        submitted: list[str] = []
        status = {
            "available": True,
            "status": "verified",
            "verified_sha256": get_sqx_preset("sqx-default-futures").sha256_hex,
            "launch_available": True,
            "launch_status": "verified",
            "launch_detail": "Verified SQX launcher and exact build.",
            "observed_build": SQX_BUILD,
        }

        with TemporaryDirectory() as tmp, patch(
            "tradercockpit.sqx_presets.runtime_preset_status",
            return_value=status,
        ):
            receipt = launch_sqx_preset(
                "sqx-default-futures",
                Path(tmp),
                ensure_channel=lambda home: None,
                post_command=lambda command: (submitted.append(command) or 202, "accepted"),
            )

        self.assertEqual(receipt["schema"], "tc.sqx-preset-launch.v1")
        self.assertEqual(receipt["preset_id"], "sqx-default-futures")
        self.assertEqual(receipt["state"], "submitted")
        self.assertEqual(receipt["control_requests_submitted"], 2)
        self.assertEqual(len(submitted), 2)
        self.assertIn("DefaultFutures.xml", submitted[0])
        self.assertEqual(submitted[1], "-project action=start name=Builder")

    def test_launch_api_maps_runtime_refusal_without_fabricating_success(self) -> None:
        def refusing_launcher(preset_id: str, sqx_home: Path | str | None):
            raise SqxPresetRuntimeError("sqx_launcher_missing", "SQX launcher is missing")

        status, payload = sqx_preset_launch_response(
            None,
            "sqx-default-forex",
            launcher=refusing_launcher,
        )
        self.assertEqual(status, 503)
        self.assertEqual(payload["error"], "producer_not_configured")
        self.assertEqual(payload["reason_code"], "sqx_launcher_missing")

    def test_launch_api_returns_real_launcher_receipt(self) -> None:
        def launcher(preset_id: str, sqx_home: Path | str | None):
            return {
                "schema": "tc.sqx-preset-launch.v1",
                "preset_id": preset_id,
                "market": "forex",
                "sqx_build": SQX_BUILD,
                "source_sha256": get_sqx_preset(preset_id).sha256_hex,
                "project": "Builder",
                "state": "submitted",
                "control_requests_submitted": 2,
                "receipts": [
                    {"sequence": 1, "http_status": 202},
                    {"sequence": 2, "http_status": 202},
                ],
            }

        status, payload = sqx_preset_launch_response(
            Path("C:/StrategyQuantX"),
            "sqx-default-forex",
            launcher=launcher,
        )
        self.assertEqual(status, 202)
        self.assertEqual(payload["preset_id"], "sqx-default-forex")
        self.assertEqual(payload["control_requests_submitted"], 2)


if __name__ == "__main__":
    unittest.main()
