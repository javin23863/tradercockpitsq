from __future__ import annotations

from hashlib import sha256
import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

from tradercockpit.assistant import OPENROUTER_API_KEY_ENV
from tradercockpit.runtime_status import RUNTIME_STATUS_SCHEMA, runtime_status_record


class RuntimeStatusTests(unittest.TestCase):
    def _runtime(
        self,
        root: Path,
        *,
        build: str = "2953",
        launcher: bytes = b"trusted launcher",
    ) -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text(build, encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        (root / "sqcli.exe").write_bytes(launcher)
        return root

    def test_unconfigured_status_is_truthful_and_application_stays_ready(self) -> None:
        with patch.dict("os.environ", {OPENROUTER_API_KEY_ENV: ""}, clear=False):
            payload = runtime_status_record(None)

        self.assertEqual(payload["schema"], RUNTIME_STATUS_SCHEMA)
        self.assertEqual(payload["application"]["status"], "ready")
        research = payload["research_backend"]
        self.assertEqual(research["status"], "unavailable")
        self.assertFalse(research["configured"])
        self.assertFalse(research["verified"])
        self.assertIsNone(research["build"])
        self.assertEqual(research["reason_code"], "runtime_not_configured")
        self.assertFalse(research["execution"]["available"])
        self.assertEqual(research["execution"]["reason_code"], "runtime_not_configured")
        self.assertFalse(research["execution"]["launcher_verified"])
        self.assertIsNone(research["execution"]["launcher_sha256"])
        self.assertTrue(research["execution"]["gateway_implemented"])
        self.assertFalse(research["execution"]["gateway_available"])
        self.assertTrue(research["execution"]["requires_approved_configuration"])

        custody = payload["research_custody"]
        self.assertEqual(custody["status"], "unavailable")
        self.assertEqual(custody["reason_code"], "store_not_bound")
        contract = custody["contract"]
        self.assertEqual(contract["status"], "ready")
        self.assertEqual(contract["current_update"], "compare-and-set")
        self.assertFalse(contract["active_subject"])
        self.assertEqual(
            contract["record_kinds"],
            ["idea", "configuration", "native-job", "candidate", "historical-result", "proof", "promotion", "export", "deployment"],
        )

        for key in ("market_data", "macro_series", "account", "model", "provider", "live_signals", "live_risk", "scoped_performance", "live_deployment", "prop_simulation"):
            self.assertEqual(payload[key]["status"], "unavailable")
        extensions = payload["extensions"]
        self.assertEqual(extensions["status"], "ready")
        self.assertIsNone(extensions["reason_code"])
        self.assertEqual(extensions["registry"]["schema"], "tc.capability-registry.v1")
        self.assertEqual(len(extensions["registry"]["capabilities"]), 6)
        self.assertEqual(extensions["registry"]["addons"], [])
        self.assertEqual(payload["provider"]["reason_code"], "provider_not_configured")
        self.assertEqual(payload["model"]["default_model"], "z-ai/glm-5.3-flash")
        self.assertEqual(payload["assistant"]["status"], "unavailable")

    def test_verified_runtime_without_launcher_trust_stays_inspection_only(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            payload = runtime_status_record(home)

        research = payload["research_backend"]
        self.assertEqual(research["status"], "ready")
        self.assertTrue(research["configured"])
        self.assertTrue(research["verified"])
        self.assertEqual(research["producer"], "strategyquant-x")
        self.assertEqual(research["build"], "144.2953")
        self.assertTrue(research["inspection"]["available"])
        self.assertFalse(research["execution"]["available"])
        self.assertFalse(research["execution"]["launcher_verified"])
        self.assertTrue(research["execution"]["gateway_implemented"])
        self.assertFalse(research["execution"]["gateway_available"])
        self.assertTrue(research["execution"]["requires_approved_configuration"])
        self.assertEqual(
            research["execution"]["reason_code"],
            "trusted_launcher_not_configured",
        )
        self.assertEqual(
            research["runtime"]["launcher"]["reason_code"],
            "trusted_launcher_not_configured",
        )

    def test_verified_trusted_launcher_exposes_approval_gated_execution_boundary(self) -> None:
        launcher = b"trusted launcher"
        trusted = sha256(launcher).hexdigest()
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp), launcher=launcher)
            payload = runtime_status_record(home, trusted)

        research = payload["research_backend"]
        self.assertEqual(research["status"], "ready")
        self.assertTrue(research["verified"])
        self.assertEqual(research["build"], "144.2953")
        self.assertTrue(research["execution"]["launcher_verified"])
        self.assertEqual(research["execution"]["launcher_sha256"], trusted)
        self.assertTrue(research["execution"]["gateway_implemented"])
        self.assertTrue(research["execution"]["gateway_available"])
        self.assertTrue(research["execution"]["available"])
        self.assertIsNone(research["execution"]["reason_code"])
        self.assertTrue(research["execution"]["requires_approved_configuration"])
        self.assertEqual(research["runtime"]["launcher"]["expected_sha256"], trusted)
        self.assertEqual(research["runtime"]["launcher"]["observed_sha256"], trusted)
        self.assertTrue(research["runtime"]["execution"]["gateway_implemented"])
        self.assertTrue(research["runtime"]["execution"]["gateway_available"])
        self.assertTrue(research["runtime"]["execution"]["available"])
        self.assertFalse(research["runtime"]["execution"]["launch_authorization"])
        self.assertTrue(research["runtime"]["execution"]["requires_approved_configuration"])

    def test_invalid_configured_runtime_is_not_reported_as_ready(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp), build="9999")
            payload = runtime_status_record(home, "0" * 64)

        research = payload["research_backend"]
        self.assertEqual(research["status"], "invalid")
        self.assertTrue(research["configured"])
        self.assertFalse(research["verified"])
        self.assertIsNone(research["build"])
        self.assertEqual(research["reason_code"], "sqx_build_mismatch")
        self.assertFalse(research["inspection"]["available"])
        self.assertFalse(research["execution"]["available"])
        self.assertTrue(research["execution"]["gateway_implemented"])
        self.assertFalse(research["execution"]["gateway_available"])
        self.assertEqual(research["execution"]["reason_code"], "sqx_build_mismatch")

    def test_public_payload_contains_no_runtime_filesystem_path(self) -> None:
        launcher = b"trusted launcher"
        trusted = sha256(launcher).hexdigest()
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp), launcher=launcher)
            encoded = json.dumps(runtime_status_record(home, trusted), sort_keys=True)

        self.assertNotIn(str(home), encoded)
        self.assertNotIn("build.dat", encoded)
        self.assertNotIn("SQUANT.dat", encoded)


if __name__ == "__main__":
    unittest.main()
