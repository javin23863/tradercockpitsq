from __future__ import annotations

from datetime import datetime, timezone
import unittest

from tradercockpit.market_data import (
    MARKET_BARS_SCHEMA,
    MARKET_QUOTES_SCHEMA,
    MarketBar,
    MarketDataProvider,
    MarketQuote,
    market_bars_record,
    market_quotes_record,
    requested_bar_instrument,
    unavailable_quotes_record,
    watchlist_from_env,
)


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


class _BarsProvider:
    def __init__(self, bars):
        self._bars = bars

    def fetch_quotes(self, symbols):
        return []

    def fetch_bars(self, symbol, timeframe):
        return [bar for bar in self._bars if bar.symbol == symbol and bar.timeframe == timeframe]


class _FailingBarsProvider:
    def fetch_quotes(self, symbols):
        return []

    def fetch_bars(self, symbol, timeframe):
        raise RuntimeError("ohlc offline")


class MarketBarsReadModelTests(unittest.TestCase):
    def test_no_provider_yields_unavailable_without_invented_instrument(self) -> None:
        record = market_bars_record(None)

        self.assertEqual(record["schema"], MARKET_BARS_SCHEMA)
        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "instrument_unspecified")
        self.assertEqual(record["bars"], [])
        self.assertNotIn("ES", str(record))
        self.assertNotIn("H1", str(record))

    def test_quotes_only_provider_does_not_synthesize_candles(self) -> None:
        record = market_bars_record(_StaticProvider([]), symbol="ES", timeframe="M15")

        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "bars_not_supported")
        self.assertEqual(record["symbol"], "ES")
        self.assertEqual(record["timeframe"], "M15")
        self.assertEqual(record["bars"], [])

    def test_missing_timeframe_fails_closed(self) -> None:
        record = market_bars_record(None, symbol="NQ")
        self.assertEqual(record["reason_code"], "timeframe_unspecified")
        self.assertEqual(record["symbol"], "NQ")
        self.assertIsNone(record["timeframe"])
        self.assertEqual(record["bars"], [])

    def test_invalid_timeframe_fails_closed(self) -> None:
        record = market_bars_record(None, symbol="NQ", timeframe="15m")
        self.assertEqual(record["reason_code"], "timeframe_invalid")
        self.assertEqual(record["bars"], [])

    def test_watchlist_supplies_symbol_without_inventing_a_market(self) -> None:
        symbol, timeframe, reason = requested_bar_instrument(None, "M15", ("nq", "es"))
        self.assertEqual((symbol, timeframe, reason), ("NQ", "M15", None))

    def test_connected_provider_reports_only_provider_ohlc(self) -> None:
        opened = datetime(2026, 9, 3, 13, 30, tzinfo=timezone.utc)
        provider = _BarsProvider(
            [
                MarketBar("ES", "M15", opened, 100.0, 101.5, 99.5, 101.0, volume=12),
                MarketBar("ES", "M15", datetime(2026, 9, 3, 13, 45, tzinfo=timezone.utc), 101.0, 102.0, 100.5, 100.8),
            ]
        )

        record = market_bars_record(provider, symbol="es", timeframe="m15", provider_id="example-bars")

        self.assertEqual(record["status"], "current")
        self.assertIsNone(record["reason_code"])
        self.assertEqual(record["provider"], {"id": "example-bars"})
        self.assertEqual(record["symbol"], "ES")
        self.assertEqual(record["timeframe"], "M15")
        self.assertEqual(len(record["bars"]), 2)
        self.assertEqual(record["bars"][0]["open"], 100.0)
        self.assertEqual(record["bars"][0]["high"], 101.5)
        self.assertEqual(record["bars"][0]["low"], 99.5)
        self.assertEqual(record["bars"][0]["close"], 101.0)
        self.assertEqual(record["bars"][0]["volume"], 12.0)
        self.assertTrue(record["bars"][0]["open_time"].endswith("Z"))

    def test_empty_or_failing_provider_fails_closed(self) -> None:
        empty = market_bars_record(_BarsProvider([]), symbol="ES", timeframe="M15")
        self.assertEqual(empty["reason_code"], "bars_empty")
        self.assertEqual(empty["bars"], [])

        failed = market_bars_record(_FailingBarsProvider(), symbol="ES", timeframe="M15")
        self.assertEqual(failed["reason_code"], "provider_read_failed")
        self.assertEqual(failed["bars"], [])

    def test_bar_shapes_fail_closed(self) -> None:
        now = datetime(2026, 9, 3, tzinfo=timezone.utc)
        with self.assertRaises(ValueError):
            MarketBar("ES", "M15", now.replace(tzinfo=None), 1.0, 2.0, 0.5, 1.5)
        with self.assertRaises(ValueError):
            MarketBar("ES", "M15", now, 1.0, 0.5, 0.4, 0.8)
        with self.assertRaises(ValueError):
            MarketBar("ES", "M15", now, 1.0, 1.2, 1.1, 1.1)
        with self.assertRaises(ValueError):
            MarketBar("ES", "M15", now, 1.0, 2.0, 0.5, 1.5, volume=-1)
        with self.assertRaises(ValueError):
            MarketBar("ES", "15m", now, 1.0, 2.0, 0.5, 1.5)


if __name__ == "__main__":
    unittest.main()
