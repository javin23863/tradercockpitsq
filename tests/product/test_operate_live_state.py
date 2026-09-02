from __future__ import annotations

import unittest

from tradercockpit.operate_live_state import (
    LIVE_DEPLOYMENT_SCHEMA,
    LIVE_RISK_SCHEMA,
    LIVE_SIGNALS_SCHEMA,
    SCOPED_PERFORMANCE_SCHEMA,
    live_deployment_record,
    live_risk_record,
    live_signals_record,
    scoped_performance_record,
)
from tradercockpit.runtime_status import runtime_status_record


class OperateLiveStateTests(unittest.TestCase):
    def test_live_signals_is_fail_closed(self) -> None:
        payload = live_signals_record()
        self.assertEqual(payload["schema"], LIVE_SIGNALS_SCHEMA)
        self.assertEqual(payload["status"], "unavailable")
        self.assertEqual(payload["reason_code"], "deployment_not_connected")
        self.assertFalse(payload["historical_fallback"])
        self.assertEqual(payload["signals"], [])

    def test_live_risk_is_fail_closed(self) -> None:
        payload = live_risk_record()
        self.assertEqual(payload["schema"], LIVE_RISK_SCHEMA)
        self.assertEqual(payload["status"], "unavailable")
        self.assertEqual(payload["reason_code"], "account_not_connected")
        self.assertIsNone(payload["limits"])

    def test_scoped_performance_is_fail_closed(self) -> None:
        payload = scoped_performance_record()
        self.assertEqual(payload["schema"], SCOPED_PERFORMANCE_SCHEMA)
        self.assertEqual(payload["status"], "unavailable")
        self.assertEqual(payload["reason_code"], "deployment_not_connected")
        self.assertIsNone(payload["metrics"])

    def test_live_deployment_is_fail_closed(self) -> None:
        payload = live_deployment_record()
        self.assertEqual(payload["schema"], LIVE_DEPLOYMENT_SCHEMA)
        self.assertEqual(payload["status"], "unavailable")
        self.assertEqual(payload["reason_code"], "execution_not_connected")
        self.assertFalse(payload["historical_fallback"])

    def test_runtime_status_embeds_operate_live_records(self) -> None:
        payload = runtime_status_record(None)
        for key in ("live_signals", "live_risk", "scoped_performance", "live_deployment"):
            record = payload[key]
            self.assertEqual(record["status"], "unavailable")
            self.assertFalse(record["historical_fallback"])
            self.assertIsNone(record["producer"])


if __name__ == "__main__":
    unittest.main()
