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
from ipaddress import ip_address
from math import isfinite
from pathlib import Path

from tradercockpit.atomic_io import atomic_write_json
import base64
import json
import os
import secrets
import time
from typing import Callable, Mapping, Protocol, Sequence, runtime_checkable
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlparse
from urllib.request import Request, urlopen


MARKET_QUOTES_SCHEMA = "tc.market-quotes.v1"
WATCHLIST_ENV = "TRADERCOCKPIT_WATCHLIST"
MARKET_API_KEY_ENV = "TRADERCOCKPIT_MARKET_API_KEY"
SCHWAB_CLIENT_ID_ENV = "SCHWAB_CLIENT_ID"
SCHWAB_CLIENT_SECRET_ENV = "SCHWAB_CLIENT_SECRET"
SCHWAB_REFRESH_TOKEN_ENV = "SCHWAB_REFRESH_TOKEN"
SCHWAB_CALLBACK_URL_ENV = "SCHWAB_CALLBACK_URL"
SCHWAB_PROVIDER_ID = "schwab"
SCHWAB_AUTHORIZE_PATH = "/api/market/schwab/authorize"
SCHWAB_CALLBACK_PATH_DEFAULT = "/api/market/schwab/callback"
SCHWAB_AUTHORIZE_URL = "https://api.schwabapi.com/v1/oauth/authorize"
SCHWAB_TOKEN_URL = "https://api.schwabapi.com/v1/oauth/token"
SCHWAB_QUOTES_URL = "https://api.schwabapi.com/marketdata/v1/quotes"
SCHWAB_OAUTH_NAME = "schwab-oauth.json"
SCHWAB_OAUTH_SCHEMA = "tc.schwab-oauth.v1"
FINNHUB_PROVIDER_ID = "finnhub"
FINNHUB_QUOTE_URL = "https://finnhub.io/api/v1/quote"
QUOTE_TIMEOUT_SECONDS = 10

QuoteTransport = Callable[[str, dict[str, str]], tuple[int, bytes]]
TokenTransport = Callable[[str, dict[str, str], bytes | None], tuple[int, bytes]]
# ponytail: process-local access tokens; refresh when expired. Persist only the refresh token.
_SCHWAB_ACCESS: dict[str, tuple[str, float]] = {}


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


def _is_loopback_host(host: str) -> bool:
    if host in {"localhost", "127.0.0.1", "::1"}:
        return True
    try:
        return ip_address(host.split("%", 1)[0]).is_loopback
    except ValueError:
        return False


def _schwab_authorize_path(environ: Mapping[str, str] | None = None) -> str | None:
    source = environ if environ is not None else os.environ
    has_app = bool((source.get(SCHWAB_CLIENT_ID_ENV) or "").strip() and (source.get(SCHWAB_CLIENT_SECRET_ENV) or "").strip())
    has_refresh = bool((source.get(SCHWAB_REFRESH_TOKEN_ENV) or "").strip())
    if has_app and not has_refresh:
        return SCHWAB_AUTHORIZE_PATH
    return None


def _provider_hookup(environ: Mapping[str, str] | None = None) -> dict[str, object]:
    hookup: dict[str, object] = {
        "interface": "tradercockpit.market_data.MarketDataProvider.fetch_quotes",
        "watchlist_env": WATCHLIST_ENV,
        "credential_env": [
            SCHWAB_CLIENT_ID_ENV,
            SCHWAB_CLIENT_SECRET_ENV,
            SCHWAB_REFRESH_TOKEN_ENV,
            MARKET_API_KEY_ENV,
        ],
        "historical_fx_indices": {
            "producer": "strategyquant_x",
            "source": "dukascopy",
            "pipeline": "native",
            "detail": (
                "Forex and indices history is downloaded inside StrategyQuant X Data Manager "
                "(instruments such as EURUSD_dukascopy). TraderCockpit does not fetch Dukascopy."
            ),
        },
        "detail": (
            "Live quotes use Schwab Market Data (operator SCHWAB_CLIENT_ID / SCHWAB_CLIENT_SECRET "
            "plus SCHWAB_REFRESH_TOKEN or loopback OAuth) when those are set; otherwise Finnhub "
            "via TRADERCOCKPIT_MARKET_API_KEY. Watchlist symbols are requested as-is. "
            "Historical FX/indices stay in native SQX Dukascopy. FRED is a separate macro series "
            "producer, not the ticker."
        ),
    }
    authorize_path = _schwab_authorize_path(environ)
    if authorize_path:
        hookup["authorize_path"] = authorize_path
    return hookup


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


def _urllib_request(url: str, headers: dict[str, str], data: bytes | None = None) -> tuple[int, bytes]:
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


def _urllib_get(url: str, headers: dict[str, str]) -> tuple[int, bytes]:
    return _urllib_request(url, headers, None)


def schwab_oauth_path(data_root: Path | str) -> Path:
    return Path(data_root) / SCHWAB_OAUTH_NAME


def _load_schwab_oauth(data_root: Path | str) -> dict[str, object]:
    path = schwab_oauth_path(data_root)
    if not path.is_file():
        return {}
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return {}
    if not isinstance(payload, dict) or payload.get("schema") != SCHWAB_OAUTH_SCHEMA:
        return {}
    return payload


def _write_schwab_oauth(data_root: Path | str, payload: dict[str, object]) -> None:
    atomic_write_json(schwab_oauth_path(data_root), payload)


def load_schwab_refresh_token(data_root: Path | str | None) -> str:
    if data_root is None:
        return ""
    token = _load_schwab_oauth(data_root).get("refresh_token")
    return token.strip() if isinstance(token, str) else ""


def schwab_callback_uri(environ: Mapping[str, str] | None = None) -> str:
    source = environ if environ is not None else os.environ
    raw = (source.get(SCHWAB_CALLBACK_URL_ENV) or "").strip()
    if not raw:
        raise ValueError("SCHWAB_CALLBACK_URL must be a loopback http URL registered with Schwab")
    parsed = urlparse(raw)
    if parsed.scheme != "http" or not parsed.hostname or not _is_loopback_host(parsed.hostname):
        raise ValueError("SCHWAB_CALLBACK_URL must be a loopback http URL")
    if parsed.path and parsed.path != "/":
        return raw
    raise ValueError("SCHWAB_CALLBACK_URL must include a callback path")


def schwab_callback_path(environ: Mapping[str, str] | None = None) -> str:
    try:
        return urlparse(schwab_callback_uri(environ)).path
    except ValueError:
        return SCHWAB_CALLBACK_PATH_DEFAULT


def _schwab_basic_headers(client_id: str, client_secret: str) -> dict[str, str]:
    token = base64.b64encode(f"{client_id}:{client_secret}".encode("utf-8")).decode("ascii")
    return {
        "Authorization": f"Basic {token}",
        "Content-Type": "application/x-www-form-urlencoded",
        "Accept": "application/json",
        "User-Agent": "TraderCockpit/1.0",
    }


def _parse_json_object(body: bytes) -> dict[str, object]:
    try:
        payload = json.loads(body.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as extra:
        raise RuntimeError("market-data provider returned non-JSON") from extra
    if not isinstance(payload, dict):
        raise RuntimeError("market-data provider returned a malformed body")
    return payload


def _schwab_token_request(
    client_id: str,
    client_secret: str,
    fields: dict[str, str],
    *,
    transport: TokenTransport | None = None,
) -> dict[str, object]:
    send = transport or _urllib_request
    status, body = send(SCHWAB_TOKEN_URL, _schwab_basic_headers(client_id, client_secret), urlencode(fields).encode("utf-8"))
    if status in {401, 403}:
        raise RuntimeError("market-data provider rejected the credential")
    if status >= 400:
        raise RuntimeError(f"market-data provider failed ({status})")
    payload = _parse_json_object(body)
    access = payload.get("access_token")
    if not isinstance(access, str) or not access.strip():
        raise RuntimeError("market-data provider returned no access token")
    return payload


def begin_schwab_oauth(data_root: Path | str, environ: Mapping[str, str] | None = None) -> str:
    source = environ if environ is not None else os.environ
    client_id = (source.get(SCHWAB_CLIENT_ID_ENV) or "").strip()
    if not client_id or not (source.get(SCHWAB_CLIENT_SECRET_ENV) or "").strip():
        raise ValueError("Schwab OAuth requires SCHWAB_CLIENT_ID and SCHWAB_CLIENT_SECRET")
    redirect_uri = schwab_callback_uri(source)
    state = secrets.token_urlsafe(32)
    existing = _load_schwab_oauth(data_root)
    refresh = existing.get("refresh_token")
    payload: dict[str, object] = {"schema": SCHWAB_OAUTH_SCHEMA, "oauth_state": state}
    if isinstance(refresh, str) and refresh.strip():
        payload["refresh_token"] = refresh.strip()
    _write_schwab_oauth(data_root, payload)
    return (
        f"{SCHWAB_AUTHORIZE_URL}?"
        f"{urlencode({'client_id': client_id, 'redirect_uri': redirect_uri, 'response_type': 'code', 'state': state})}"
    )


def complete_schwab_oauth(
    data_root: Path | str,
    code: str,
    state: str,
    environ: Mapping[str, str] | None = None,
    *,
    transport: TokenTransport | None = None,
) -> None:
    source = environ if environ is not None else os.environ
    client_id = (source.get(SCHWAB_CLIENT_ID_ENV) or "").strip()
    client_secret = (source.get(SCHWAB_CLIENT_SECRET_ENV) or "").strip()
    if not client_id or not client_secret:
        raise ValueError("Schwab OAuth requires SCHWAB_CLIENT_ID and SCHWAB_CLIENT_SECRET")
    expected = _load_schwab_oauth(data_root).get("oauth_state")
    if not isinstance(expected, str) or not expected or state != expected:
        raise ValueError("Schwab OAuth state mismatch")
    redirect_uri = schwab_callback_uri(source)
    payload = _schwab_token_request(
        client_id,
        client_secret,
        {"grant_type": "authorization_code", "code": code, "redirect_uri": redirect_uri},
        transport=transport,
    )
    refresh = payload.get("refresh_token")
    if not isinstance(refresh, str) or not refresh.strip():
        raise RuntimeError("market-data provider returned no refresh token")
    access = payload.get("access_token")
    expires = payload.get("expires_in")
    if isinstance(access, str) and isinstance(expires, (int, float)) and not isinstance(expires, bool):
        _SCHWAB_ACCESS[refresh.strip()] = (access.strip(), time.monotonic() + max(30.0, float(expires) - 30.0))
    _write_schwab_oauth(data_root, {"schema": SCHWAB_OAUTH_SCHEMA, "refresh_token": refresh.strip()})


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
            if not isinstance(last, (int, float)) or isinstance(last, bool) or not isfinite(float(last)) or float(last) <= 0:
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


def _schwab_quote_row(symbol: str, payload: object) -> MarketQuote | None:
    if not isinstance(payload, dict):
        return None
    quote = payload.get("quote")
    if not isinstance(quote, dict):
        return None
    last = quote.get("lastPrice")
    if not isinstance(last, (int, float)) or isinstance(last, bool) or not isfinite(float(last)) or float(last) <= 0:
        return None
    raw_time = quote.get("quoteTime")
    if raw_time is None:
        raw_time = quote.get("quoteTimeInLong")
    if raw_time is None:
        raw_time = quote.get("tradeTime")
    if not isinstance(raw_time, (int, float)) or isinstance(raw_time, bool) or raw_time <= 0:
        return None
    millis = float(raw_time)
    unix = int(millis / 1000) if millis > 10_000_000_000 else int(millis)
    change = quote.get("netPercentChangeInDouble")
    if change is None:
        change = quote.get("netPercentChange")
    if change is not None and (
        not isinstance(change, (int, float)) or isinstance(change, bool) or not isfinite(float(change))
    ):
        change = None
    reference = payload.get("reference")
    currency = None
    if isinstance(reference, dict):
        raw_currency = reference.get("currency")
        if isinstance(raw_currency, str) and raw_currency.strip():
            currency = raw_currency.strip()
    return MarketQuote(
        symbol,
        float(last),
        None if change is None else float(change),
        datetime.fromtimestamp(unix, tz=timezone.utc),
        currency=currency,
    )


class SchwabQuoteProvider:
    """Operator-credential Schwab Market Data quotes. Tokens never enter a read model."""

    provider_id = SCHWAB_PROVIDER_ID

    def __init__(
        self,
        client_id: str,
        client_secret: str,
        refresh_token: str,
        *,
        transport: QuoteTransport | None = None,
        token_transport: TokenTransport | None = None,
        on_refresh: Callable[[str], None] | None = None,
    ) -> None:
        cid = client_id.strip() if isinstance(client_id, str) else ""
        secret = client_secret.strip() if isinstance(client_secret, str) else ""
        token = refresh_token.strip() if isinstance(refresh_token, str) else ""
        if not cid or not secret or not token:
            raise ValueError("Schwab quotes require client id, client secret, and refresh token")
        self._client_id = cid
        self._client_secret = secret
        self._refresh_token = token
        self._send = transport or _urllib_get
        self._token_send = token_transport or _urllib_request
        self._on_refresh = on_refresh

    def _access_token(self) -> str:
        cached = _SCHWAB_ACCESS.get(self._refresh_token)
        now = time.monotonic()
        if cached is not None and cached[1] > now:
            return cached[0]
        payload = _schwab_token_request(
            self._client_id,
            self._client_secret,
            {"grant_type": "refresh_token", "refresh_token": self._refresh_token},
            transport=self._token_send,
        )
        access = str(payload["access_token"]).strip()
        expires = payload.get("expires_in")
        ttl = float(expires) if isinstance(expires, (int, float)) and not isinstance(expires, bool) else 1800.0
        rotated = payload.get("refresh_token")
        if isinstance(rotated, str) and rotated.strip() and rotated.strip() != self._refresh_token:
            _SCHWAB_ACCESS.pop(self._refresh_token, None)
            self._refresh_token = rotated.strip()
            if self._on_refresh is not None:
                self._on_refresh(self._refresh_token)
        _SCHWAB_ACCESS[self._refresh_token] = (access, now + max(30.0, ttl - 30.0))
        return access

    def fetch_quotes(self, symbols: Sequence[str]) -> Sequence[MarketQuote]:
        cleaned = [_clean_symbol(symbol) for symbol in symbols]
        if not cleaned:
            return []
        access = self._access_token()
        status, body = self._send(
            f"{SCHWAB_QUOTES_URL}?{urlencode({'symbols': ','.join(cleaned)})}",
            {
                "Accept": "application/json",
                "Authorization": f"Bearer {access}",
                "User-Agent": "TraderCockpit/1.0",
            },
        )
        if status in {401, 403}:
            _SCHWAB_ACCESS.pop(self._refresh_token, None)
            raise RuntimeError("market-data provider rejected the credential")
        if status >= 400:
            raise RuntimeError(f"market-data provider failed ({status})")
        payload = _parse_json_object(body)
        quotes: list[MarketQuote] = []
        for symbol in cleaned:
            row = _schwab_quote_row(symbol, payload.get(symbol))
            if row is not None:
                quotes.append(row)
        return quotes


def market_provider_from_env(
    environ: Mapping[str, str] | None = None,
    *,
    transport: QuoteTransport | None = None,
    token_transport: TokenTransport | None = None,
    data_root: Path | str | None = None,
) -> SchwabQuoteProvider | FinnhubQuoteProvider | None:
    source = environ if environ is not None else os.environ
    client_id = (source.get(SCHWAB_CLIENT_ID_ENV) or "").strip()
    client_secret = (source.get(SCHWAB_CLIENT_SECRET_ENV) or "").strip()
    refresh = (source.get(SCHWAB_REFRESH_TOKEN_ENV) or "").strip() or load_schwab_refresh_token(data_root)
    if client_id and client_secret and refresh:
        def persist(token: str) -> None:
            if data_root is None:
                return
            _write_schwab_oauth(data_root, {"schema": SCHWAB_OAUTH_SCHEMA, "refresh_token": token})

        return SchwabQuoteProvider(
            client_id,
            client_secret,
            refresh,
            transport=transport,
            token_transport=token_transport,
            on_refresh=persist if data_root is not None else None,
        )
    key = (source.get(MARKET_API_KEY_ENV) or "").strip()
    if not key:
        return None
    return FinnhubQuoteProvider(key, transport=transport)
