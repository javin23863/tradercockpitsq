from __future__ import annotations

from datetime import datetime, timezone
import json
import unittest
from unittest.mock import patch

from tradercockpit.market_data import (
    FINNHUB_PROVIDER_ID,
    MARKET_API_KEY_ENV,
    MARKET_QUOTES_SCHEMA,
    MarketDataProvider,
    MarketQuote,
    FinnhubQuoteProvider,
    market_provider_from_env,
    market_quotes_record,
    unavailable_quotes_record,
    watchlist_from_env,
)
from tradercockpit.runtime_status import runtime_status_record


class _StaticProvider:
    """Minimal MarketDataProvider double returning fixed quotes."""

    def __init__(self, quotes):
        self._quotes = quotes

    def fetch_quotes(self, symbols):
        return [q for q in self._quotes if q.symbol in set(symbols)]


class _FailingProvider:
    def fetch_quotes(self, symbols):
        raise RuntimeError("feed offline")


class MarketDataReadModelTests(unittest.TestCase):
    def test_no_provider_yields_explicit_unavailable_with_placeholders(self) -> None:
        record = market_quotes_record(None, ("es", "nq"))

        self.assertEqual(record["schema"], MARKET_QUOTES_SCHEMA)
        self.assertEqual(record["scope"], "live_current")
        self.assertFalse(record["historical_fallback"])
        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "provider_not_configured")
        self.assertIsNone(record["provider"])
        self.assertEqual(record["quotes"], [])
        self.assertEqual([row["symbol"] for row in record["watchlist"]], ["ES", "NQ"])
        for row in record["watchlist"]:
            self.assertEqual(row["status"], "unavailable")
            self.assertIsNone(row["last"])
            self.assertIsNone(row["change_percent"])
            self.assertIsNone(row["observed_at"])

    def test_provider_hookup_is_documented_and_never_fabricates_values(self) -> None:
        record = unavailable_quotes_record(())
        hookup = record["provider_hookup"]
        self.assertEqual(
            hookup["interface"],
            "tradercockpit.market_data.MarketDataProvider.fetch_quotes",
        )
        self.assertEqual(hookup["watchlist_env"], "TRADERCOCKPIT_WATCHLIST")
        self.assertEqual(hookup["credential_env"], MARKET_API_KEY_ENV)
        self.assertEqual(record["watchlist"], [])
        self.assertEqual(record["quotes"], [])

    def test_connected_provider_reports_only_provider_values(self) -> None:
        observed = datetime(2026, 9, 2, 13, 30, tzinfo=timezone.utc)
        provider = _StaticProvider(
            [
                MarketQuote("ES", 5308.25, 0.48, observed, currency="USD"),
                MarketQuote("NQ", 18725.5, None, observed),
            ]
        )

        record = market_quotes_record(provider, ("ES", "NQ", "CL"), provider_id="example-feed")

        self.assertEqual(record["status"], "current")
        self.assertIsNone(record["reason_code"])
        self.assertEqual(record["provider"], {"id": "example-feed"})
        by_symbol = {row["symbol"]: row for row in record["watchlist"]}
        self.assertEqual(by_symbol["ES"]["status"], "current")
        self.assertEqual(by_symbol["ES"]["last"], 5308.25)
        self.assertEqual(by_symbol["ES"]["change_percent"], 0.48)
        self.assertEqual(by_symbol["ES"]["currency"], "USD")
        self.assertTrue(by_symbol["ES"]["observed_at"].endswith("Z"))
        self.assertIsNone(by_symbol["NQ"]["change_percent"])
        # CL was requested but not returned by the provider -> truthful unavailable, no fabrication.
        self.assertEqual(by_symbol["CL"]["status"], "unavailable")
        self.assertIsNone(by_symbol["CL"]["last"])
        self.assertEqual([row["symbol"] for row in record["quotes"]], ["ES", "NQ"])

    def test_failing_provider_fails_closed_without_partial_values(self) -> None:
        record = market_quotes_record(_FailingProvider(), ("ES",))

        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "provider_read_failed")
        self.assertEqual(record["quotes"], [])
        self.assertEqual(record["watchlist"][0]["symbol"], "ES")
        self.assertEqual(record["watchlist"][0]["status"], "unavailable")

    def test_watchlist_env_is_operator_config_and_deduplicates(self) -> None:
        self.assertEqual(watchlist_from_env({}), ())
        self.assertEqual(
            watchlist_from_env({"TRADERCOCKPIT_WATCHLIST": " es , nq ,ES,, cl "}),
            ("ES", "NQ", "CL"),
        )

    def test_provider_double_satisfies_runtime_protocol(self) -> None:
        self.assertIsInstance(_StaticProvider([]), MarketDataProvider)

    def test_quote_shapes_fail_closed(self) -> None:
        now = datetime(2026, 9, 2, tzinfo=timezone.utc)
        with self.assertRaises(ValueError):
            MarketQuote("", 1.0, None, now)
        with self.assertRaises(ValueError):
            MarketQuote("ES", float("nan"), None, now)
        with self.assertRaises(ValueError):
            MarketQuote("ES", 1.0, float("inf"), now)
        with self.assertRaises(ValueError):
            MarketQuote("ES", 1.0, None, now.replace(tzinfo=None))


class FinnhubProviderTests(unittest.TestCase):
    def test_missing_key_keeps_provider_unconfigured(self) -> None:
        self.assertIsNone(market_provider_from_env({}))
        self.assertIsNone(market_provider_from_env({MARKET_API_KEY_ENV: "  "}))

    def test_quotes_come_from_finnhub_json_and_omit_unresolved_symbols(self) -> None:
        calls = []
        observed = datetime(2026, 9, 2, 12, 0, tzinfo=timezone.utc)
        unix = int(observed.timestamp())

        def transport(url, headers):
            calls.append((url, headers))
            symbol = "AAPL" if "AAPL" in url else "NOPE"
            if symbol == "AAPL":
                return 200, json.dumps({"c": 190.5, "dp": 1.25, "t": unix}).encode()
            return 200, b'{"c": 0, "dp": 0, "t": 0}'

        provider = FinnhubQuoteProvider("sk-test-key", transport=transport)
        record = market_quotes_record(provider, ("AAPL", "NOPE"), provider_id=provider.provider_id)
        self.assertEqual(record["status"], "current")
        self.assertEqual(record["provider"], {"id": FINNHUB_PROVIDER_ID})
        by_symbol = {row["symbol"]: row for row in record["watchlist"]}
        self.assertEqual(by_symbol["AAPL"]["last"], 190.5)
        self.assertEqual(by_symbol["AAPL"]["change_percent"], 1.25)
        self.assertEqual(by_symbol["AAPL"]["observed_at"], "2026-09-02T12:00:00Z")
        self.assertEqual(by_symbol["NOPE"]["status"], "unavailable")
        self.assertIsNone(by_symbol["NOPE"]["last"])
        dumped = json.dumps(record)
        self.assertNotIn("sk-test-key", dumped)
        self.assertNotIn("X-Finnhub-Token", dumped)
        self.assertEqual(calls[0][1]["X-Finnhub-Token"], "sk-test-key")
        self.assertIn("symbol=AAPL", calls[0][0])
        self.assertNotIn("token=", calls[0][0])

    def test_status_overview_uses_first_quote_without_leaking_the_key(self) -> None:
        observed = datetime.now(timezone.utc)

        def transport(_url, _headers):
            return 200, json.dumps({"c": 190.5, "dp": 1.25, "t": int(observed.timestamp())}).encode()

        with patch.dict("os.environ", {"TRADERCOCKPIT_WATCHLIST": "AAPL"}):
            payload = runtime_status_record(
                None,
                market_provider=FinnhubQuoteProvider("sk-test-key", transport=transport),
            )
        self.assertEqual(payload["market_data"]["status"], "current")
        self.assertEqual(payload["market_data"]["producer"]["id"], FINNHUB_PROVIDER_ID)
        self.assertEqual(payload["market_data"]["context"]["instrument"], "AAPL")
        self.assertNotIn("sk-test-key", json.dumps(payload))

    def test_rejected_credential_fails_closed(self) -> None:
        def transport(_url, _headers):
            return 401, b'{"error": "Invalid API key"}'

        record = market_quotes_record(FinnhubQuoteProvider("sk-bad", transport=transport), ("AAPL",))
        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "provider_read_failed")
        self.assertEqual(record["quotes"], [])
        self.assertNotIn("sk-bad", json.dumps(record))

    def test_provider_from_env_uses_product_key(self) -> None:
        provider = market_provider_from_env({MARKET_API_KEY_ENV: "sk-env"})
        self.assertIsInstance(provider, FinnhubQuoteProvider)
        self.assertEqual(provider.provider_id, FINNHUB_PROVIDER_ID)


if __name__ == "__main__":
    unittest.main()
