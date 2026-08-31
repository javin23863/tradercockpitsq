from __future__ import annotations

import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.runtime_status import RUNTIME_STATUS_SCHEMA, runtime_status_record


class RuntimeStatusTests(unittest.TestCase):
    def _runtime(self, root: Path, *, build: str = "2953") -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text(build, encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        return root

    def test_unconfigured_status_is_truthful_and_application_stays_ready(self) -> None:
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
        self.assertEqual(
            research["execution"]["reason_code"],
            "trusted_native_gateway_not_implemented",
        )
        self.assertIsNone(research["execution"]["launcher_sha256"])

        custody = payload["research_custody"]
        self.assertEqual(custody["status"], "ready")
        self.assertEqual(custody["current_update"], "compare-and-set")
        self.assertFalse(custody["active_subject"])
        self.assertEqual(
            custody["record_kinds"],
            ["idea", "configuration", "native-job", "candidate", "historical-result", "proof"],
        )

        for key in ("market_data", "account", "model", "provider", "extensions"):
            self.assertEqual(payload[key]["status"], "unavailable")
        self.assertEqual(payload["provider"]["reason_code"], "provider_not_configured")

    def test_verified_runtime_reports_exact_build_but_not_execution_readiness(self) -> None:
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
        self.assertEqual(
            research["execution"]["reason_code"],
            "trusted_native_gateway_not_implemented",
        )

    def test_invalid_configured_runtime_is_not_reported_as_ready(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp), build="9999")
            payload = runtime_status_record(home)

        research = payload["research_backend"]
        self.assertEqual(research["status"], "invalid")
        self.assertTrue(research["configured"])
        self.assertFalse(research["verified"])
        self.assertIsNone(research["build"])
        self.assertEqual(research["reason_code"], "sqx_build_mismatch")
        self.assertFalse(research["inspection"]["available"])
        self.assertFalse(research["execution"]["available"])

    def test_public_payload_contains_no_runtime_filesystem_path(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            encoded = json.dumps(runtime_status_record(home), sort_keys=True)

        self.assertNotIn(str(home), encoded)
        self.assertNotIn("build.dat", encoded)
        self.assertNotIn("SQUANT.dat", encoded)


if __name__ == "__main__":
    unittest.main()
