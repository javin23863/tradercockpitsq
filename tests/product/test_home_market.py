from __future__ import annotations

from datetime import datetime, timedelta, timezone
import unittest

from tradercockpit.home_market import (
    HOME_MARKET_OVERVIEW_SCHEMA,
    MarketOverviewObservation,
    error_market_overview_record,
    market_overview_record,
)


class HomeMarketOverviewTests(unittest.TestCase):
    def test_unconfigured_producer_is_explicit_and_has_no_historical_fallback(self) -> None:
        record = market_overview_record()

        self.assertEqual(record["schema"], HOME_MARKET_OVERVIEW_SCHEMA)
        self.assertEqual(record["scope"], "live_current")
        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "producer_not_configured")
        self.assertFalse(record["historical_fallback"])
        self.assertIsNone(record["producer"])
        self.assertIsNone(record["context"])
        self.assertEqual(record["freshness"]["state"], "unavailable")

    def test_current_observation_preserves_only_producer_owned_context(self) -> None:
        now = datetime(2026, 9, 2, 12, 0, tzinfo=timezone.utc)
        observation = MarketOverviewObservation(
            producer="example-live-feed",
            observed_at=now - timedelta(seconds=4),
            instrument="EURUSD",
            timeframe="M1",
            session="producer-session",
            market_state="producer-state",
            descriptors=(("venue", "example"), ("mode", "streaming")),
        )

        record = market_overview_record(observation, now=now, stale_after_seconds=30)

        self.assertEqual(record["status"], "current")
        self.assertIsNone(record["reason_code"])
        self.assertEqual(record["producer"], {"id": "example-live-feed"})
        self.assertEqual(
            record["context"],
            {
                "instrument": "EURUSD",
                "timeframe": "M1",
                "session": "producer-session",
                "market_state": "producer-state",
                "descriptors": {"venue": "example", "mode": "streaming"},
            },
        )
        self.assertEqual(record["freshness"]["state"], "current")
        self.assertEqual(record["freshness"]["age_seconds"], 4)
        self.assertFalse(record["historical_fallback"])

    def test_stale_observation_remains_visible_and_is_not_promoted_to_current(self) -> None:
        now = datetime(2026, 9, 2, 12, 0, tzinfo=timezone.utc)
        observation = MarketOverviewObservation(
            producer="example-live-feed",
            observed_at=now - timedelta(seconds=31),
            instrument="BTC-USD",
        )

        record = market_overview_record(observation, now=now, stale_after_seconds=30)

        self.assertEqual(record["status"], "stale")
        self.assertEqual(record["reason_code"], "producer_observation_stale")
        self.assertEqual(record["context"]["instrument"], "BTC-USD")
        self.assertEqual(record["freshness"]["age_seconds"], 31)
        self.assertEqual(record["freshness"]["state"], "stale")

    def test_invalid_observation_shapes_fail_closed(self) -> None:
        now = datetime.now(timezone.utc)
        with self.assertRaises(ValueError):
            MarketOverviewObservation(producer="", observed_at=now, instrument="EURUSD")
        with self.assertRaises(ValueError):
            MarketOverviewObservation(producer="feed", observed_at=now.replace(tzinfo=None), instrument="EURUSD")
        with self.assertRaises(ValueError):
            MarketOverviewObservation(
                producer="feed",
                observed_at=now,
                instrument="EURUSD",
                descriptors=(("venue", "one"), ("venue", "two")),
            )
        with self.assertRaises(ValueError):
            market_overview_record(
                MarketOverviewObservation(producer="feed", observed_at=now, instrument="EURUSD"),
                stale_after_seconds=0,
            )

    def test_error_state_exposes_no_partial_market_context(self) -> None:
        record = error_market_overview_record()
        self.assertEqual(record["status"], "error")
        self.assertEqual(record["reason_code"], "producer_read_failed")
        self.assertIsNone(record["producer"])
        self.assertIsNone(record["context"])
        self.assertFalse(record["historical_fallback"])


if __name__ == "__main__":
    unittest.main()
