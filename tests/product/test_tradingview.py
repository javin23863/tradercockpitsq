from __future__ import annotations

import json
import unittest

from tradercockpit.market_bars import market_bars_record
from tradercockpit.market_data import market_provider_from_env, market_quotes_record
from tradercockpit.tradingview import TradingViewQuoteProvider


class TradingViewProviderTests(unittest.TestCase):
    def test_scanner_quotes_omit_recommendation_columns(self):
        def transport(url, headers, data):
            payload = json.loads(data.decode("utf-8"))
            self.assertEqual(payload["columns"], ["close", "change", "currency", "update_mode"])
            self.assertNotIn("Recommend.All", payload["columns"])
            return 200, json.dumps(
                {"data": [{"s": "NASDAQ:AAPL", "d": [190.5, 1.25, "USD", "realtime"]}]}
            ).encode()

        provider = TradingViewQuoteProvider(transport=transport)
        record = market_quotes_record(provider, ("NASDAQ:AAPL",), provider_id=provider.provider_id)
        self.assertEqual(record["status"], "current")
        self.assertEqual(record["quotes"][0]["last"], 190.5)
        self.assertEqual(record["provider"]["id"], "tradingview")

        bars = market_bars_record(provider, "NASDAQ:AAPL")
        self.assertEqual(bars["reason_code"], "producer_history_unavailable")

    def test_tradingview_flag_selects_provider_after_schwab_missing(self):
        provider = market_provider_from_env({"TRADINGVIEW_MARKET_DATA": "1"})
        self.assertEqual(provider.provider_id, "tradingview")


if __name__ == "__main__":
    unittest.main()
