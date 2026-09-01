from __future__ import annotations

import unittest

from tradercockpit.home_market import HOME_MARKET_OVERVIEW_SCHEMA
from tradercockpit.runtime_status import runtime_status_record


class HomeMarketStatusBindingTests(unittest.TestCase):
    def test_runtime_status_embeds_the_canonical_market_overview_record(self) -> None:
        payload = runtime_status_record(None)
        market = payload["market_data"]

        self.assertEqual(market["schema"], HOME_MARKET_OVERVIEW_SCHEMA)
        self.assertEqual(market["scope"], "live_current")
        self.assertEqual(market["status"], "unavailable")
        self.assertEqual(market["reason_code"], "producer_not_configured")
        self.assertFalse(market["historical_fallback"])
        self.assertIsNone(market["producer"])
        self.assertIsNone(market["context"])


if __name__ == "__main__":
    unittest.main()
