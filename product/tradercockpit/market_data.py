"""Live/current market quotes read model and provider seam.

TraderCockpit does not embed a market-data feed. This module defines the typed read
model the Home market ticker and Market Overview consume, plus the provider seam an
operator wires to a real market-data API.

Design rules (see docs/product-architecture-v1.md):

- No prices, changes, symbols, or timestamps are hard-coded. The watchlist is
  operator configuration (``TRADERCOCKPIT_WATCHLIST``) and quotes come only from a
  connected provider.
- With no provider configured, the record is an explicit ``provider_not_configured``
  state. The UI renders that truthfully and never fabricates values.
- ``MarketDataProvider`` is the single hookup point for a live API. Implement
  ``fetch_quotes`` against any feed (broker, vendor, websocket poller) and pass the
  provider to :func:`market_quotes_record`.
"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from math import isfinite
import json
import os
from typing import Callable, Mapping, Protocol, Sequence, runtime_checkable
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen


MARKET_QUOTES_SCHEMA = "tc.market-quotes.v1"
WATCHLIST_ENV = "TRADERCOCKPIT_WATCHLIST"
MARKET_API_KEY_ENV = "TRADERCOCKPIT_MARKET_API_KEY"
FINNHUB_PROVIDER_ID = "finnhub"
FINNHUB_QUOTE_URL = "https://finnhub.io/api/v1/quote"
QUOTE_TIMEOUT_SECONDS = 10

QuoteTransport = Callable[[str, dict[str, str]], tuple[int, bytes]]


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


@runtime_checkable
class MarketDataProvider(Protocol):
    """The single seam for a live market-data API.

    Implement this against any real feed and pass it to ``market_quotes_record``.
    It must return one :class:`MarketQuote` per resolvable symbol; unresolved symbols
    may be omitted and are reported as unavailable in the read model.
    """

    def fetch_quotes(self, symbols: Sequence[str]) -> Sequence[MarketQuote]:
        ...


def watchlist_from_env(environ: Mapping[str, str] | None = None) -> tuple[str, ...]:
    """Resolve the operator-configured watchlist. Empty when unset (never hard-coded)."""

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
        "credential_env": MARKET_API_KEY_ENV,
        "detail": (
            "Connect Finnhub by setting TRADERCOCKPIT_MARKET_API_KEY. "
            "Watchlist symbols are requested as-is (no ES to ES=F mapping). "
            "No quote values are produced until that key is set."
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


def _urllib_get(url: str, headers: dict[str, str]) -> tuple[int, bytes]:
    request = Request(url, headers=headers, method="GET")
    try:
        with urlopen(request, timeout=QUOTE_TIMEOUT_SECONDS) as response:  # noqa: S310 - fixed vendor URL
            return int(response.status), response.read()
    except HTTPError as extra:
        return int(extra.code), extra.read()
    except URLError as extra:
        raise RuntimeError(f"market-data provider unreachable: {extra.reason}") from extra
    except TimeoutError as extra:
        raise RuntimeError("market-data provider timed out") from extra


class FinnhubQuoteProvider:
    """Operator-credential Finnhub REST quotes. The key never enters a read model."""

    provider_id = FINNHUB_PROVIDER_ID

    def __init__(self, api_key: str, *, transport: QuoteTransport | None = None) -> None:
        key = api_key.strip() if isinstance(api_key, str) else ""
        if not key:
            raise ValueError("Finnhub API key must be a non-empty string")
        self._key = key
        self._send = transport or _urllib_get

    def fetch_quotes(self, symbols: Sequence[str]) -> Sequence[MarketQuote]:
        quotes: list[MarketQuote] = []
        for symbol in symbols:
            cleaned = _clean_symbol(symbol)
            status, body = self._send(
                f"{FINNHUB_QUOTE_URL}?{urlencode({'symbol': cleaned})}",
                {
                    "Accept": "application/json",
                    "X-Finnhub-Token": self._key,
                    "User-Agent": "TraderCockpit/1.0",
                },
            )
            if status in {401, 403}:
                raise RuntimeError("market-data provider rejected the credential")
            if status >= 400:
                raise RuntimeError(f"market-data provider failed ({status})")
            try:
                payload = json.loads(body.decode("utf-8"))
            except (UnicodeDecodeError, json.JSONDecodeError) as extra:
                raise RuntimeError("market-data provider returned non-JSON") from extra
            if not isinstance(payload, dict):
                raise RuntimeError("market-data provider returned a malformed quote")
            last = payload.get("c")
            unix = payload.get("t")
            if not isinstance(last, (int, float)) or isinstance(last, bool) or not isfinite(float(last)):
                continue
            if not isinstance(unix, (int, float)) or isinstance(unix, bool) or unix <= 0:
                continue
            change = payload.get("dp")
            if change is not None and (
                not isinstance(change, (int, float)) or isinstance(change, bool) or not isfinite(float(change))
            ):
                change = None
            quotes.append(
                MarketQuote(
                    cleaned,
                    float(last),
                    None if change is None else float(change),
                    datetime.fromtimestamp(int(unix), tz=timezone.utc),
                    currency="USD",
                )
            )
        return quotes


def market_provider_from_env(
    environ: Mapping[str, str] | None = None,
    *,
    transport: QuoteTransport | None = None,
) -> FinnhubQuoteProvider | None:
    source = environ if environ is not None else os.environ
    key = (source.get(MARKET_API_KEY_ENV) or "").strip()
    if not key:
        return None
    return FinnhubQuoteProvider(key, transport=transport)
