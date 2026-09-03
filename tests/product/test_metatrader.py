from __future__ import annotations

from datetime import datetime, timezone
import json
import unittest

from tradercockpit.market_bars import market_bars_record
from tradercockpit.market_data import market_provider_from_env, market_quotes_record
from tradercockpit.metatrader import MetaTraderQuoteProvider
from tradercockpit.operate_live_state import live_risk_record, live_signals_record


class _Tick:
    def __init__(self, last, time, prev_close=None, bid=None):
        self.last = last
        self.time = time
        self.prev_close = prev_close
        self.bid = bid
        self.ask = None


class _Account:
    login = 123456
    server = "Demo-Server"
    currency = "USD"
    balance = 10000.0
    equity = 10050.0
    margin = 200.0
    margin_free = 9850.0
    profit = 50.0


class _FakeMt5:
    def __init__(self):
        self.initialized = False
        self.logged_in = None

    def initialize(self, path=None):
        self.initialized = True
        self.path = path
        return True

    def login(self, login, password, server):
        self.logged_in = (login, password, server)
        return True

    def last_error(self):
        return (1, "ok")

    def symbol_info_tick(self, symbol):
        if symbol != "EURUSD":
            return None
        return _Tick(1.085, datetime(2026, 9, 3, tzinfo=timezone.utc).timestamp(), prev_close=1.08)

    def copy_rates_from_pos(self, symbol, timeframe, start_pos, count):
        return [
            {"time": 1756800000, "open": 1.08, "high": 1.09, "low": 1.07, "close": 1.085},
            {"time": 1756800900, "open": 1.085, "high": 1.086, "low": 1.084, "close": 1.086},
        ]

    def account_info(self):
        return _Account()

    def shutdown(self):
        return None


class MetaTraderProviderTests(unittest.TestCase):
    def test_quotes_and_bars_come_from_terminal_not_invented_signals(self):
        provider = MetaTraderQuoteProvider("123456", "secret", "Demo-Server", api=_FakeMt5())
        record = market_quotes_record(provider, ("EURUSD", "NOPE"), provider_id=provider.provider_id)
        self.assertEqual(record["status"], "current")
        self.assertEqual(record["quotes"][0]["symbol"], "EURUSD")
        self.assertEqual(record["quotes"][0]["last"], 1.085)
        encoded = json.dumps(record)
        self.assertNotIn("secret", encoded)
        self.assertEqual(live_signals_record()["reason_code"], "deployment_not_connected")

        bars = market_bars_record(provider, "EURUSD", timeframe="M15", count=2)
        self.assertEqual(bars["status"], "current")
        self.assertEqual(bars["closes"], [1.085, 1.086])
        self.assertEqual(bars["producer"]["id"], "metatrader5")

        risk = live_risk_record(provider.account_snapshot())
        self.assertEqual(risk["status"], "current")
        self.assertEqual(risk["limits"]["equity"], 10050.0)

    def test_mt5_credentials_win_provider_selection(self):
        provider = market_provider_from_env(
            {
                "MT5_LOGIN": "123456",
                "MT5_PASSWORD": "secret",
                "MT5_SERVER": "Demo-Server",
                "SCHWAB_CLIENT_ID": "cid",
                "SCHWAB_CLIENT_SECRET": "csecret",
                "SCHWAB_REFRESH_TOKEN": "rtoken",
            }
        )
        self.assertEqual(provider.provider_id, "metatrader5")


if __name__ == "__main__":
    unittest.main()
