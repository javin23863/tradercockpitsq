from __future__ import annotations

import unittest

from tradercockpit.operate_prop_simulation import PROP_SIMULATION_SCHEMA, prop_simulation_record
from tradercockpit.runtime_status import runtime_status_record


class OperatePropSimulationTests(unittest.TestCase):
    def test_prop_simulation_is_fail_closed(self) -> None:
        payload = prop_simulation_record()
        self.assertEqual(payload["schema"], PROP_SIMULATION_SCHEMA)
        self.assertEqual(payload["status"], "unavailable")
        self.assertEqual(payload["reason_code"], "simulation_account_not_connected")
        self.assertFalse(payload["historical_fallback"])
        self.assertIsNone(payload["account"])
        self.assertIsNone(payload["metrics"])
        self.assertIsNone(payload["challenge"])

    def test_runtime_status_embeds_prop_simulation(self) -> None:
        payload = runtime_status_record(None)
        record = payload["prop_simulation"]
        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "simulation_account_not_connected")
        self.assertFalse(record["historical_fallback"])
        self.assertIsNone(record["producer"])


if __name__ == "__main__":
    unittest.main()
