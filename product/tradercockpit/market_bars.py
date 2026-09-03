"""Historical OHLC bars from a connected live-market producer.

This is not an SQX substitute. Native SQX Dukascopy history stays on the research
producer. These bars are the consumer/operator's MetaTrader or TradingView context.
"""

from __future__ import annotations

from typing import Any

MARKET_BARS_SCHEMA = "tc.market-bars.v1"
MARKET_BARS_API_PATH = "/api/market/bars"
DEFAULT_BAR_COUNT = 100
MAX_BAR_COUNT = 500


def unavailable_bars_record(
    *,
    symbol: str | None = None,
    timeframe: str | None = None,
    reason_code: str = "producer_not_configured",
    detail: str = "No live market-data producer is connected for historical bars.",
) -> dict[str, object]:
    return {
        "schema": MARKET_BARS_SCHEMA,
        "scope": "live_current",
        "historical_fallback": False,
        "status": "unavailable",
        "reason_code": reason_code,
        "detail": detail,
        "producer": None,
        "symbol": symbol,
        "timeframe": timeframe,
        "bars": [],
        "closes": [],
    }


def market_bars_record(
    provider: Any | None,
    symbol: str,
    *,
    timeframe: str = "M15",
    count: int = DEFAULT_BAR_COUNT,
) -> dict[str, object]:
    cleaned = symbol.strip().upper() if isinstance(symbol, str) else ""
    period = timeframe.strip().upper() if isinstance(timeframe, str) else ""
    if not cleaned:
        return unavailable_bars_record(reason_code="symbol_required", detail="Bars require one symbol.")
    if not period:
        return unavailable_bars_record(symbol=cleaned, reason_code="timeframe_required", detail="Bars require a timeframe.")
    if not isinstance(count, int) or isinstance(count, bool) or count <= 0:
        return unavailable_bars_record(symbol=cleaned, timeframe=period, reason_code="count_invalid", detail="count must be a positive integer.")
    limited = min(count, MAX_BAR_COUNT)
    if provider is None:
        return unavailable_bars_record(symbol=cleaned, timeframe=period)
    fetch = getattr(provider, "fetch_bars", None)
    if not callable(fetch):
        provider_id = getattr(provider, "provider_id", "connected")
        return unavailable_bars_record(
            symbol=cleaned,
            timeframe=period,
            reason_code="producer_history_unavailable",
            detail=f"{provider_id} is connected for live quotes but does not supply historical bars.",
        )
    try:
        rows = fetch(cleaned, timeframe=period, count=limited)
    except Exception as extra:
        return unavailable_bars_record(
            symbol=cleaned,
            timeframe=period,
            reason_code="producer_read_failed",
            detail=f"The connected market-data producer failed: {extra}",
        )
    bars = [row for row in rows if isinstance(row, dict) and isinstance(row.get("close"), (int, float))]
    closes = [float(row["close"]) for row in bars]
    provider_id = str(getattr(provider, "provider_id", "connected"))
    return {
        "schema": MARKET_BARS_SCHEMA,
        "scope": "live_current",
        "historical_fallback": False,
        "status": "current" if bars else "unavailable",
        "reason_code": None if bars else "producer_empty",
        "detail": "Producer bars for consumer/operator context. Native SQX history is not substituted.",
        "producer": {"id": provider_id},
        "symbol": cleaned,
        "timeframe": period,
        "bars": bars,
        "closes": closes,
    }
