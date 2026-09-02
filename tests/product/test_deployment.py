from __future__ import annotations

import unittest

from tradercockpit.deployment import (
    DEPLOYMENT_MODE_ENV,
    DEPLOYMENT_STATUS_SCHEMA,
    deployment_mode,
    deployment_status_record,
)


_READY_ACCOUNT = {"status": "ready"}
_ACTIVE_MEMBERSHIP = {"membership_status": "active"}
_ENFORCED_PROVIDER = {"spend_boundary": {"provider_enforced": True}}


class DeploymentModeTests(unittest.TestCase):
    def test_defaults_to_personal_and_is_ready(self) -> None:
        self.assertEqual(deployment_mode({}), "personal")
        record = deployment_status_record({})
        self.assertEqual(record["schema"], DEPLOYMENT_STATUS_SCHEMA)
        self.assertEqual(record["mode"], "personal")
        self.assertEqual(record["status"], "ready")

    def test_invalid_mode_fails_closed(self) -> None:
        with self.assertRaises(ValueError):
            deployment_mode({DEPLOYMENT_MODE_ENV: "enterprise"})
        record = deployment_status_record({DEPLOYMENT_MODE_ENV: "enterprise"})
        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "deployment_mode_invalid")

    def test_commercial_is_not_ready_without_per_tenant_isolation(self) -> None:
        # Even with hosted auth, billing and provider-enforced spend all satisfied, a
        # single-data-root build is not multi-tenant, so commercial fails closed.
        record = deployment_status_record(
            {DEPLOYMENT_MODE_ENV: "commercial"},
            account=_READY_ACCOUNT,
            membership=_ACTIVE_MEMBERSHIP,
            provider=_ENFORCED_PROVIDER,
        )
        self.assertEqual(record["mode"], "commercial")
        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "commercial_not_ready")
        self.assertTrue(record["requirements"]["hosted_consumer_auth"])
        self.assertTrue(record["requirements"]["membership_billing"])
        self.assertTrue(record["requirements"]["provider_enforced_spend"])
        self.assertFalse(record["requirements"]["per_tenant_isolation"])
        self.assertEqual(record["unmet"], ["per_tenant_isolation"])

    def test_commercial_lists_every_unmet_prerequisite(self) -> None:
        record = deployment_status_record({DEPLOYMENT_MODE_ENV: "commercial"})
        self.assertEqual(record["reason_code"], "commercial_not_ready")
        self.assertEqual(
            record["unmet"],
            ["hosted_consumer_auth", "membership_billing", "per_tenant_isolation", "provider_enforced_spend"],
        )


if __name__ == "__main__":
    unittest.main()
