"""Live/current market quotes and OHLC bar-series read models.

TraderCockpit does not embed a market-data feed. This module defines the typed read
models Home and Signals & Models consume, plus the provider seam an operator wires
to a real market-data API.

Design rules (see docs/product-architecture-v1.md):

- No prices, changes, symbols, timestamps, or OHLC bars are hard-coded. The
  watchlist is operator configuration (``TRADERCOCKPIT_WATCHLIST``). Quotes and
  bars come only from a connected provider.
- With no provider configured, the record is an explicit ``provider_not_configured``
  state. The UI renders that truthfully and never fabricates values.
- Last/change quotes are not a substitute for bars.
- ``MarketDataProvider`` is the single hookup point for a live API. Implement
  ``fetch_quotes`` and, when the feed can supply OHLC, ``fetch_bars``.
"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from math import isfinite
from typing import Mapping, Protocol, Sequence, runtime_checkable


MARKET_QUOTES_SCHEMA = "tc.market-quotes.v1"
MARKET_BARS_SCHEMA = "tc.market-bars.v1"
WATCHLIST_ENV = "TRADERCOCKPIT_WATCHLIST"
ALLOWED_BAR_TIMEFRAMES = frozenset({"M1", "M5", "M15", "M30", "H1", "H4", "D1", "W1"})
MAX_BARS = 2000


def _clean_symbol(symbol: str) -> str:
    if not isinstance(symbol, str) or not symbol.strip():
        raise ValueError("symbol must be a non-empty string")
    return symbol.strip().upper()


@dataclass(frozen=True, slots=True)
class MarketQuote:
    """One producer-owned live quote. Values come from a connected provider only."""

    symbol: str
    last: float
    change_percent: float | None
    observed_at: datetime
    currency: str | None = None

    def __post_init__(self) -> None:
        object.__setattr__(self, "symbol", _clean_symbol(self.symbol))
        if not isinstance(self.last, (int, float)) or isinstance(self.last, bool) or not isfinite(float(self.last)):
            raise ValueError("last must be a finite number")
        if self.change_percent is not None:
            if not isinstance(self.change_percent, (int, float)) or isinstance(self.change_percent, bool) or not isfinite(float(self.change_percent)):
                raise ValueError("change_percent must be a finite number or None")
        if self.observed_at.tzinfo is None or self.observed_at.utcoffset() is None:
            raise ValueError("observed_at must be timezone-aware")
        if self.currency is not None and (not isinstance(self.currency, str) or not self.currency.strip()):
            raise ValueError("currency must be a non-empty string or None")


@dataclass(frozen=True, slots=True)
class MarketBar:
    """One producer-owned OHLC bar. Values come from a connected provider only."""

    symbol: str
    timeframe: str
    open_time: datetime
    open: float
    high: float
    low: float
    close: float
    volume: float | None = None

    def __post_init__(self) -> None:
        object.__setattr__(self, "symbol", _clean_symbol(self.symbol))
        timeframe = _clean_timeframe(self.timeframe)
        object.__setattr__(self, "timeframe", timeframe)
        if self.open_time.tzinfo is None or self.open_time.utcoffset() is None:
            raise ValueError("open_time must be timezone-aware")
        for name in ("open", "high", "low", "close"):
            value = getattr(self, name)
            if not isinstance(value, (int, float)) or isinstance(value, bool) or not isfinite(float(value)):
                raise ValueError(f"{name} must be a finite number")
        high = float(self.high)
        low = float(self.low)
        if high < low:
            raise ValueError("high must be greater than or equal to low")
        if high < float(self.open) or high < float(self.close):
            raise ValueError("high must be at least open and close")
        if low > float(self.open) or low > float(self.close):
            raise ValueError("low must be at most open and close")
        if self.volume is not None:
            if not isinstance(self.volume, (int, float)) or isinstance(self.volume, bool) or not isfinite(float(self.volume)) or float(self.volume) < 0:
                raise ValueError("volume must be a finite non-negative number or None")


def _clean_timeframe(value: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ValueError("timeframe must be a non-empty string")
    normalized = value.strip().upper()
    if normalized not in ALLOWED_BAR_TIMEFRAMES:
        raise ValueError("timeframe is not a registered bar request")
    return normalized


@runtime_checkable
class MarketDataProvider(Protocol):
    """The single seam for a live market-data API.

    Implement this against any real feed and pass it to ``market_quotes_record``.
    It must return one :class:`MarketQuote` per resolvable symbol; unresolved symbols
    may be omitted and are reported as unavailable in the read model.

    ``fetch_bars`` is optional. When absent, the bars read model reports
    ``bars_not_supported`` rather than synthesizing candles.
    """

    def fetch_quotes(self, symbols: Sequence[str]) -> Sequence[MarketQuote]:
        ...


def watchlist_from_env(environ: Mapping[str, str] | None = None) -> tuple[str, ...]:
    """Resolve the operator-configured watchlist. Empty when unset (never hard-coded)."""

    import os

    source = environ if environ is not None else os.environ
    raw = source.get(WATCHLIST_ENV, "") or ""
    symbols: list[str] = []
    seen: set[str] = set()
    for token in raw.split(","):
        candidate = token.strip().upper()
        if candidate and candidate not in seen:
            seen.add(candidate)
            symbols.append(candidate)
    return tuple(symbols)


def _provider_hookup() -> dict[str, object]:
    return {
        "interface": "tradercockpit.market_data.MarketDataProvider.fetch_quotes",
        "watchlist_env": WATCHLIST_ENV,
        "detail": (
            "Connect a live market-data API by implementing MarketDataProvider.fetch_quotes "
            "and, when the feed can supply OHLC, fetch_bars. Configure symbols via the "
            "watchlist env var. No quote or bar values are produced until a provider is connected."
        ),
    }


def _base_record() -> dict[str, object]:
    return {
        "schema": MARKET_QUOTES_SCHEMA,
        "scope": "live_current",
        "historical_fallback": False,
        "provider_hookup": _provider_hookup(),
    }


def _watchlist_placeholders(symbols: Sequence[str]) -> list[dict[str, object]]:
    return [
        {
            "symbol": _clean_symbol(symbol),
            "status": "unavailable",
            "last": None,
            "change_percent": None,
            "currency": None,
            "observed_at": None,
        }
        for symbol in symbols
    ]


def unavailable_quotes_record(
    symbols: Sequence[str] = (),
    *,
    reason_code: str = "provider_not_configured",
    detail: str = "No live market-data provider is connected.",
) -> dict[str, object]:
    return {
        **_base_record(),
        "status": "unavailable",
        "reason_code": reason_code,
        "detail": detail,
        "provider": None,
        "watchlist": _watchlist_placeholders(symbols),
        "quotes": [],
    }


def _iso_utc(value: datetime) -> str:
    return value.astimezone(timezone.utc).isoformat().replace("+00:00", "Z")


def market_quotes_record(
    provider: MarketDataProvider | None = None,
    symbols: Sequence[str] = (),
    *,
    provider_id: str | None = None,
) -> dict[str, object]:
    """Return one secret-free live-quotes snapshot for the configured watchlist.

    With no provider, this is an explicit unavailable stub carrying the configured
    watchlist as placeholders (no fabricated values). With a provider, quotes are
    produced only from that provider; a failing provider yields an error record.
    """

    resolved = tuple(_clean_symbol(symbol) for symbol in symbols)
    if provider is None:
        return unavailable_quotes_record(resolved)

    try:
        raw_quotes = provider.fetch_quotes(resolved)
        quotes = [quote for quote in raw_quotes if isinstance(quote, MarketQuote)]
    except Exception as exc:  # provider errors must fail closed, never fabricate
        return unavailable_quotes_record(
            resolved,
            reason_code="provider_read_failed",
            detail=f"The connected market-data provider failed: {exc}",
        )

    by_symbol = {quote.symbol: quote for quote in quotes}
    watchlist: list[dict[str, object]] = []
    for symbol in resolved or tuple(by_symbol):
        quote = by_symbol.get(symbol)
        if quote is None:
            watchlist.append(
                {
                    "symbol": symbol,
                    "status": "unavailable",
                    "last": None,
                    "change_percent": None,
                    "currency": None,
                    "observed_at": None,
                }
            )
            continue
        watchlist.append(
            {
                "symbol": quote.symbol,
                "status": "current",
                "last": float(quote.last),
                "change_percent": None if quote.change_percent is None else float(quote.change_percent),
                "currency": quote.currency,
                "observed_at": _iso_utc(quote.observed_at),
            }
        )

    return {
        **_base_record(),
        "status": "current",
        "reason_code": None,
        "detail": "Live quotes provided by the connected market-data provider.",
        "provider": {"id": provider_id or "connected"},
        "watchlist": watchlist,
        "quotes": [row for row in watchlist if row["status"] == "current"],
    }


def _bars_base_record() -> dict[str, object]:
    return {
        "schema": MARKET_BARS_SCHEMA,
        "scope": "live_current",
        "historical_fallback": False,
        "provider_hookup": {
            **_provider_hookup(),
            "bars_interface": "tradercockpit.market_data.MarketDataProvider.fetch_bars",
        },
    }


def unavailable_bars_record(
    *,
    symbol: str | None = None,
    timeframe: str | None = None,
    reason_code: str = "provider_not_configured",
    detail: str = "No live market-data provider is connected.",
) -> dict[str, object]:
    return {
        **_bars_base_record(),
        "status": "unavailable",
        "reason_code": reason_code,
        "detail": detail,
        "provider": None,
        "symbol": symbol,
        "timeframe": timeframe,
        "bars": [],
    }


def requested_bar_instrument(
    symbol: str | None,
    timeframe: str | None,
    watchlist: Sequence[str] = (),
) -> tuple[str | None, str | None, str | None]:
    """Resolve the requested chart instrument without inventing a market.

    Returns ``(symbol, timeframe, reason_code)``. A reason means the request is
    incomplete; callers must not invent a default instrument or timeframe.
    """

    resolved_symbol = None
    if isinstance(symbol, str) and symbol.strip():
        resolved_symbol = _clean_symbol(symbol)
    elif watchlist:
        resolved_symbol = _clean_symbol(watchlist[0])
    if resolved_symbol is None:
        return None, None, "instrument_unspecified"
    if not isinstance(timeframe, str) or not timeframe.strip():
        return resolved_symbol, None, "timeframe_unspecified"
    try:
        return resolved_symbol, _clean_timeframe(timeframe), None
    except ValueError:
        return resolved_symbol, None, "timeframe_invalid"


def market_bars_record(
    provider: MarketDataProvider | None = None,
    *,
    symbol: str | None = None,
    timeframe: str | None = None,
    watchlist: Sequence[str] = (),
    provider_id: str | None = None,
) -> dict[str, object]:
    """Return one secret-free OHLC series for the requested instrument.

    Last/change quotes are never used as a bar substitute. Missing provider,
    missing ``fetch_bars``, empty producer output, and malformed bars all fail
    closed with an empty ``bars`` list.
    """

    resolved_symbol, resolved_timeframe, request_reason = requested_bar_instrument(symbol, timeframe, watchlist)
    if request_reason == "instrument_unspecified":
        return unavailable_bars_record(
            reason_code="instrument_unspecified",
            detail="No instrument is selected and no watchlist symbol is configured.",
        )
    if request_reason == "timeframe_unspecified":
        return unavailable_bars_record(
            symbol=resolved_symbol,
            reason_code="timeframe_unspecified",
            detail="A registered timeframe is required to request bars.",
        )
    if request_reason == "timeframe_invalid":
        return unavailable_bars_record(
            symbol=resolved_symbol,
            reason_code="timeframe_invalid",
            detail="Timeframe is not a registered bar request.",
        )

    if provider is None:
        return unavailable_bars_record(
            symbol=resolved_symbol,
            timeframe=resolved_timeframe,
        )
    fetch_bars = getattr(provider, "fetch_bars", None)
    if not callable(fetch_bars):
        return unavailable_bars_record(
            symbol=resolved_symbol,
            timeframe=resolved_timeframe,
            reason_code="bars_not_supported",
            detail="The connected market-data provider does not supply OHLC bars.",
        )

    try:
        raw_bars = fetch_bars(resolved_symbol, resolved_timeframe)
        bars = [bar for bar in raw_bars if isinstance(bar, MarketBar)]
    except Exception as exc:  # provider errors must fail closed, never fabricate
        return unavailable_bars_record(
            symbol=resolved_symbol,
            timeframe=resolved_timeframe,
            reason_code="provider_read_failed",
            detail=f"The connected market-data provider failed: {exc}",
        )

    matching = [
        bar
        for bar in bars
        if bar.symbol == resolved_symbol and bar.timeframe == resolved_timeframe
    ]
    matching.sort(key=lambda bar: bar.open_time)
    seen: set[datetime] = set()
    unique: list[MarketBar] = []
    for bar in matching:
        if bar.open_time in seen:
            continue
        seen.add(bar.open_time)
        unique.append(bar)
    if len(unique) > MAX_BARS:
        unique = unique[-MAX_BARS:]
    if not unique:
        return unavailable_bars_record(
            symbol=resolved_symbol,
            timeframe=resolved_timeframe,
            reason_code="bars_empty",
            detail="The connected provider returned no OHLC bars for this instrument.",
        )

    return {
        **_bars_base_record(),
        "status": "current",
        "reason_code": None,
        "detail": "OHLC bars provided by the connected market-data provider.",
        "provider": {"id": provider_id or "connected"},
        "symbol": resolved_symbol,
        "timeframe": resolved_timeframe,
        "bars": [
            {
                "open_time": _iso_utc(bar.open_time),
                "open": float(bar.open),
                "high": float(bar.high),
                "low": float(bar.low),
                "close": float(bar.close),
                "volume": None if bar.volume is None else float(bar.volume),
            }
            for bar in unique
        ],
    }
