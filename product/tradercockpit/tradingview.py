"""TradingView scanner quotes for consumer/operator watchlists.

Headless market data only (atilaahmettaner skill). Does not drive TradingView
Desktop, does not compute signals, and does not place trades. Desktop CDP
automation stays on the operator MCP (tradesdontlie), not in this product.
"""

from __future__ import annotations

from datetime import datetime, timezone
from math import isfinite
import json
import os
from typing import Callable, Mapping, Sequence
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from tradercockpit.market_data import MarketQuote

TRADINGVIEW_PROVIDER_ID = "tradingview"
TRADINGVIEW_ENABLED_ENV = "TRADINGVIEW_MARKET_DATA"
TRADINGVIEW_WATCHLIST_ENV = "TRADINGVIEW_WATCHLIST"
TRADINGVIEW_SCANNER_URL = "https://scanner.tradingview.com/global/scan"
QUOTE_TIMEOUT_SECONDS = 10

QuoteTransport = Callable[[str, dict[str, str], bytes | None], tuple[int, bytes]]


def _clean_symbol(symbol: str) -> str:
    if not isinstance(symbol, str) or not symbol.strip():
        raise ValueError("symbol must be a non-empty string")
    return symbol.strip().upper()


def _urllib_request(url: str, headers: dict[str, str], data: bytes | None) -> tuple[int, bytes]:
    request = Request(url, data=data, headers=headers, method="POST" if data is not None else "GET")
    try:
        with urlopen(request, timeout=QUOTE_TIMEOUT_SECONDS) as response:  # noqa: S310 - fixed vendor URL
            return int(response.status), response.read()
    except HTTPError as extra:
        return int(extra.code), extra.read()
    except URLError as extra:
        raise RuntimeError(f"market-data provider unreachable: {extra.reason}") from extra
    except TimeoutError as extra:
        raise RuntimeError("market-data provider timed out") from extra


def _normalize_ticker(symbol: str) -> str:
    cleaned = _clean_symbol(symbol)
    return cleaned if ":" in cleaned else cleaned


class TradingViewQuoteProvider:
    """Public TradingView scanner quotes. No session cookie required."""

    provider_id = TRADINGVIEW_PROVIDER_ID

    def __init__(self, *, transport: QuoteTransport | None = None) -> None:
        self._send = transport or _urllib_request

    def fetch_quotes(self, symbols: Sequence[str]) -> Sequence[MarketQuote]:
        tickers = [_normalize_ticker(symbol) for symbol in symbols]
        if not tickers:
            return []
        body = json.dumps(
            {
                "symbols": {"tickers": tickers, "query": {"types": []}},
                "columns": ["close", "change", "currency", "update_mode"],
            }
        ).encode("utf-8")
        status, raw = self._send(
            TRADINGVIEW_SCANNER_URL,
            {
                "Accept": "application/json",
                "Content-Type": "application/json",
                "User-Agent": "TraderCockpit/1.0",
            },
            body,
        )
        if status >= 400:
            raise RuntimeError(f"market-data provider failed ({status})")
        try:
            payload = json.loads(raw.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError) as extra:
            raise RuntimeError("market-data provider returned non-JSON") from extra
        rows = payload.get("data") if isinstance(payload, dict) else None
        if not isinstance(rows, list):
            raise RuntimeError("market-data provider returned a malformed body")
        by_symbol = {ticker: None for ticker in tickers}
        for row in rows:
            if not isinstance(row, dict):
                continue
            symbol = row.get("s")
            values = row.get("d")
            if not isinstance(symbol, str) or not isinstance(values, list) or not values:
                continue
            last = values[0]
            if not isinstance(last, (int, float)) or isinstance(last, bool) or not isfinite(float(last)):
                continue
            change = values[1] if len(values) > 1 and isinstance(values[1], (int, float)) and not isinstance(values[1], bool) else None
            currency = values[2] if len(values) > 2 and isinstance(values[2], str) and values[2].strip() else None
            by_symbol[_clean_symbol(symbol)] = MarketQuote(
                _clean_symbol(symbol),
                float(last),
                None if change is None else float(change),
                datetime.now(timezone.utc),
                currency=currency,
            )
        return [quote for quote in (by_symbol.get(_clean_symbol(symbol)) for symbol in tickers) if quote is not None]


def tradingview_enabled(environ: Mapping[str, str] | None = None) -> bool:
    source = environ if environ is not None else os.environ
    raw = (source.get(TRADINGVIEW_ENABLED_ENV) or "").strip().lower()
    return raw in {"1", "true", "yes", "on"}


def tradingview_provider_from_env(
    environ: Mapping[str, str] | None = None,
    *,
    transport: QuoteTransport | None = None,
) -> TradingViewQuoteProvider | None:
    if not tradingview_enabled(environ):
        return None
    return TradingViewQuoteProvider(transport=transport)
