from __future__ import annotations

import json
import unittest

from tradercockpit.onboarding import (
    CUSTOMER_REASON_COPY,
    customer_copy,
    onboarding_status_record,
    telemetry_status_record,
)
from tradercockpit.runtime_status import runtime_status_record


class OnboardingTests(unittest.TestCase):
    def test_customer_copy_uses_known_codes_and_falls_back(self) -> None:
        self.assertEqual(customer_copy("signed_out"), CUSTOMER_REASON_COPY["signed_out"])
        self.assertEqual(customer_copy("brand_new_code", "Keep this detail."), "Keep this detail.")
        self.assertEqual(customer_copy("brand_new_code"), "brand new code")

    def test_telemetry_is_fail_closed_and_secret_free(self) -> None:
        record = telemetry_status_record()
        self.assertEqual(record["schema"], "tc.telemetry-policy.v1")
        self.assertFalse(record["enabled"])
        self.assertEqual(record["reason_code"], "telemetry_disabled")
        encoded = json.dumps(record)
        self.assertNotIn("OPENROUTER", encoded)
        self.assertNotIn("sk-or", encoded)
        self.assertNotIn("refresh_token", encoded)

    def test_first_run_when_native_or_data_root_missing(self) -> None:
        payload = onboarding_status_record(
            research={"verified": False, "reason_code": "runtime_not_configured", "detail": "raw"},
            account={"status": "unavailable", "reason_code": "signed_out"},
            membership={"status": "unavailable", "reason_code": "checkout_not_configured"},
            maintenance={"status": "unavailable", "reason_code": "data_root_unbound"},
            assistant={"status": "unavailable", "reason_code": "provider_not_configured"},
        )
        self.assertEqual(payload["schema"], "tc.onboarding.v1")
        self.assertTrue(payload["first_run"])
        self.assertEqual(payload["status"], "incomplete")
        self.assertEqual(payload["steps"][0]["detail"], CUSTOMER_REASON_COPY["runtime_not_configured"])
        ids = [step["id"] for step in payload["steps"]]
        self.assertEqual(ids, ["native_runtime", "account", "membership", "data_root", "secrets"])

    def test_ready_when_every_step_is_ready(self) -> None:
        payload = onboarding_status_record(
            research={"verified": True, "reason_code": None, "detail": "verified"},
            account={"status": "ready", "reason_code": None},
            membership={"status": "ready", "reason_code": None},
            maintenance={"status": "ready", "reason_code": None},
            assistant={"status": "ready", "reason_code": None},
        )
        self.assertFalse(payload["first_run"])
        self.assertEqual(payload["status"], "ready")
        self.assertIsNone(payload["reason_code"])
        self.assertTrue(all(step["status"] == "ready" for step in payload["steps"]))

    def test_runtime_status_includes_onboarding_and_telemetry(self) -> None:
        payload = runtime_status_record(None)
        self.assertEqual(payload["onboarding"]["schema"], "tc.onboarding.v1")
        self.assertTrue(payload["onboarding"]["first_run"])
        self.assertFalse(payload["telemetry"]["enabled"])
        encoded = json.dumps(payload)
        self.assertNotIn("sk-or", encoded)


if __name__ == "__main__":
    unittest.main()
