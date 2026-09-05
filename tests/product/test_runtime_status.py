from __future__ import annotations

from hashlib import sha256
import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

from tradercockpit.assistant import OPENROUTER_API_KEY_ENV
from tradercockpit.runtime_status import (
    RUNTIME_STATUS_SCHEMA,
    research_backend_recovery_detail,
    runtime_status_record,
)


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
        self.assertEqual(research["binding"]["source"], "none")
        self.assertEqual(
            research["detail"],
            research_backend_recovery_detail("runtime_not_configured"),
        )
        self.assertIn("SQX_HOME", research["detail"])
        self.assertIn("--sqx-home", research["detail"])
        self.assertIn("browser cannot choose", research["detail"])
        self.assertFalse(research["execution"]["available"])
        self.assertEqual(research["execution"]["reason_code"], "runtime_not_configured")
        self.assertEqual(research["execution"]["detail"], research["detail"])
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
            ["idea", "configuration", "native-job", "candidate", "candidate-membership", "historical-result", "proof"],
        )

        for key in ("market_data", "account", "model", "provider"):
            self.assertEqual(payload[key]["status"], "unavailable")
        live = payload["live_producers"]
        self.assertEqual(live["schema"], "tc.live-producers.v1")
        self.assertEqual(live["status"], "unavailable")
        self.assertEqual(live["tradingview"]["id"], "tradingview")
        self.assertEqual(live["metatrader"]["id"], "metatrader")
        self.assertFalse(live["tradingview"]["live_quotes"])
        self.assertFalse(live["metatrader"]["live_pnl"])
        extensions = payload["extensions"]
        self.assertEqual(extensions["status"], "ready")
        self.assertIsNone(extensions["reason_code"])
        self.assertEqual(extensions["nav_authority"], "platform")
        self.assertEqual(extensions["slot_count"], 3)
        self.assertEqual(extensions["addon_count"], 7)
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
        self.assertIn("Verified StrategyQuant X 144.2953", research["detail"])
        self.assertEqual(
            research["execution"]["detail"],
            research_backend_recovery_detail("trusted_launcher_not_configured"),
        )
        self.assertIn("SQX_LAUNCHER_SHA256", research["execution"]["detail"])
        self.assertIn("browser cannot choose", research["execution"]["detail"])

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
        self.assertIsNone(research["execution"]["detail"])
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
        self.assertEqual(
            research["detail"],
            research_backend_recovery_detail("sqx_build_mismatch"),
        )
        self.assertIn("144.2953", research["detail"])
        self.assertNotIn("9999", research["detail"])
        self.assertEqual(research["execution"]["detail"], research["detail"])
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
        self.assertNotIn(str(tmp), encoded)

    def test_fail_closed_recovery_copy_contains_no_filesystem_path(self) -> None:
        with TemporaryDirectory() as tmp:
            missing = Path(tmp) / "missing-sqx"
            encoded = json.dumps(runtime_status_record(missing), sort_keys=True)
            payload = json.loads(encoded)

        research = payload["research_backend"]
        self.assertEqual(research["reason_code"], "runtime_not_configured")
        self.assertEqual(
            research["detail"],
            research_backend_recovery_detail("runtime_not_configured"),
        )
        self.assertNotIn(str(missing), encoded)
        self.assertNotIn("missing-sqx", encoded)
        self.assertNotIn(str(tmp), encoded)

    def test_ambiguous_installs_use_recovery_copy_without_a_path(self) -> None:
        payload = runtime_status_record(
            None,
            runtime_binding="none",
            runtime_unavailable_reason="sqx_install_ambiguous",
        )
        research = payload["research_backend"]
        self.assertEqual(research["reason_code"], "sqx_install_ambiguous")
        self.assertEqual(research["execution"]["reason_code"], "sqx_install_ambiguous")
        self.assertEqual(research["binding"]["source"], "none")
        self.assertIn("More than one", research["detail"])
        self.assertIn("SQX_HOME", research["detail"])
        self.assertIn("browser cannot choose", research["detail"])
        encoded = json.dumps(payload)
        self.assertNotIn("Downloads", encoded)
        self.assertNotIn("C:\\\\", encoded)

    def test_trusted_launcher_digest_and_hash_failures_carry_recovery_copy(self) -> None:
        launcher = b"trusted launcher"
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp), launcher=launcher)
            invalid = runtime_status_record(home, "not-a-digest")
            mismatch = runtime_status_record(home, "0" * 64)

        self.assertEqual(
            invalid["research_backend"]["execution"]["reason_code"],
            "trusted_launcher_digest_invalid",
        )
        self.assertEqual(
            invalid["research_backend"]["execution"]["detail"],
            research_backend_recovery_detail("trusted_launcher_digest_invalid"),
        )
        self.assertEqual(
            mismatch["research_backend"]["execution"]["reason_code"],
            "sqx_launcher_hash_mismatch",
        )
        self.assertEqual(
            mismatch["research_backend"]["execution"]["detail"],
            research_backend_recovery_detail("sqx_launcher_hash_mismatch"),
        )
        self.assertNotIn(str(home), json.dumps(invalid))
        self.assertNotIn(str(home), json.dumps(mismatch))

    def test_unknown_runtime_reason_uses_generic_process_side_recovery(self) -> None:
        detail = research_backend_recovery_detail("not_a_real_reason")
        self.assertIn("SQX_HOME", detail)
        self.assertIn("--sqx-home", detail)
        self.assertIn("browser cannot choose", detail)
        self.assertNotIn("/", detail)
        self.assertNotIn("\\", detail)


if __name__ == "__main__":
    unittest.main()
