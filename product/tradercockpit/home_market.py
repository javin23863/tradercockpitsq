"""Canonical live/current Market Overview read model for TraderCockpit Home."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Mapping


HOME_MARKET_OVERVIEW_SCHEMA = "tc.home-market-overview.v1"
DEFAULT_STALE_AFTER_SECONDS = 30


def _non_empty(value: str, field: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{field} must be a non-empty string")
    return value.strip()


@dataclass(frozen=True, slots=True)
class MarketOverviewObservation:
    """One producer-owned live/current market context observation.

    This intentionally carries context and producer identity only. It does not define
    a TraderCockpit quote schema, synthetic market condition, or historical fallback.
    """

    producer: str
    observed_at: datetime
    instrument: str
    timeframe: str | None = None
    session: str | None = None
    market_state: str | None = None
    descriptors: tuple[tuple[str, str], ...] = ()

    def __post_init__(self) -> None:
        _non_empty(self.producer, "producer")
        _non_empty(self.instrument, "instrument")
        if self.observed_at.tzinfo is None or self.observed_at.utcoffset() is None:
            raise ValueError("observed_at must be timezone-aware")
        for field_name in ("timeframe", "session", "market_state"):
            value = getattr(self, field_name)
            if value is not None:
                _non_empty(value, field_name)
        seen: set[str] = set()
        for key, value in self.descriptors:
            normalized = _non_empty(key, "descriptor key")
            _non_empty(value, "descriptor value")
            if normalized in seen:
                raise ValueError("descriptor keys must be unique")
            seen.add(normalized)


def _iso_utc(value: datetime) -> str:
    return value.astimezone(timezone.utc).isoformat().replace("+00:00", "Z")


def _base_record() -> dict[str, object]:
    return {
        "schema": HOME_MARKET_OVERVIEW_SCHEMA,
        "scope": "live_current",
        "historical_fallback": False,
    }


def unavailable_market_overview_record() -> dict[str, object]:
    return {
        **_base_record(),
        "status": "unavailable",
        "reason_code": "producer_not_configured",
        "detail": "No live/current market-data producer is configured.",
        "producer": None,
        "context": None,
        "freshness": {
            "state": "unavailable",
            "observed_at": None,
            "age_seconds": None,
            "stale_after_seconds": DEFAULT_STALE_AFTER_SECONDS,
        },
    }


def error_market_overview_record(reason_code: str = "producer_read_failed") -> dict[str, object]:
    return {
        **_base_record(),
        "status": "error",
        "reason_code": _non_empty(reason_code, "reason_code"),
        "detail": "The live/current market-data producer could not provide a valid observation.",
        "producer": None,
        "context": None,
        "freshness": {
            "state": "error",
            "observed_at": None,
            "age_seconds": None,
            "stale_after_seconds": DEFAULT_STALE_AFTER_SECONDS,
        },
    }


def market_overview_record(
    observation: MarketOverviewObservation | None = None,
    *,
    now: datetime | None = None,
    stale_after_seconds: int = DEFAULT_STALE_AFTER_SECONDS,
) -> dict[str, object]:
    """Return one secret-free live/current Market Overview snapshot.

    Missing producer state remains explicit. A supplied observation is classified only
    by timestamp freshness; TraderCockpit does not infer market direction, volatility,
    session meaning, or any other quantitative descriptor.
    """

    if observation is None:
        return unavailable_market_overview_record()
    if not isinstance(observation, MarketOverviewObservation):
        raise TypeError("observation must be MarketOverviewObservation or None")
    if not isinstance(stale_after_seconds, int) or isinstance(stale_after_seconds, bool) or stale_after_seconds <= 0:
        raise ValueError("stale_after_seconds must be a positive integer")

    current = now or datetime.now(timezone.utc)
    if current.tzinfo is None or current.utcoffset() is None:
        raise ValueError("now must be timezone-aware")
    age_seconds = max(0, int((current - observation.observed_at).total_seconds()))
    freshness_state = "stale" if age_seconds > stale_after_seconds else "current"

    descriptors: Mapping[str, str] = dict(observation.descriptors)
    return {
        **_base_record(),
        "status": freshness_state,
        "reason_code": "producer_observation_stale" if freshness_state == "stale" else None,
        "detail": (
            "Live/current market context is stale and remains visible with its producer timestamp."
            if freshness_state == "stale"
            else "Live/current market context is current according to the configured freshness window."
        ),
        "producer": {
            "id": observation.producer,
        },
        "context": {
            "instrument": observation.instrument,
            "timeframe": observation.timeframe,
            "session": observation.session,
            "market_state": observation.market_state,
            "descriptors": dict(descriptors),
        },
        "freshness": {
            "state": freshness_state,
            "observed_at": _iso_utc(observation.observed_at),
            "age_seconds": age_seconds,
            "stale_after_seconds": stale_after_seconds,
        },
    }
