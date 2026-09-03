"""Consumer/operator MetaTrader 5 live quotes, bars, and account snapshot.

Uses the official MetaTrader5 package when present. The browser never chooses a
terminal path. Login and optional terminal path come from the process environment
or the operator secrets file. Order placement is not exposed on this seam.
"""

from __future__ import annotations

from datetime import datetime, timezone
from math import isfinite
import os
from typing import Any, Mapping, Protocol, Sequence, runtime_checkable

from tradercockpit.market_data import MarketQuote

MT5_PROVIDER_ID = "metatrader5"
MT5_LOGIN_ENV = "MT5_LOGIN"
MT5_PASSWORD_ENV = "MT5_PASSWORD"
MT5_SERVER_ENV = "MT5_SERVER"
MT5_TERMINAL_PATH_ENV = "MT5_TERMINAL_PATH"
MT5_WATCHLIST_ENV = "MT5_WATCHLIST"


@runtime_checkable
class MetaTraderApi(Protocol):
    def initialize(self, path: str | None = None) -> bool: ...
    def login(self, login: int, password: str, server: str) -> bool: ...
    def last_error(self) -> tuple[int, str]: ...
    def symbol_info_tick(self, symbol: str) -> Any: ...
    def copy_rates_from_pos(self, symbol: str, timeframe: int, start_pos: int, count: int) -> Any: ...
    def account_info(self) -> Any: ...
    def shutdown(self) -> None: ...


class _OfficialMetaTraderApi:
    def __init__(self) -> None:
        import MetaTrader5 as mt5  # type: ignore[import-not-found]

        self._mt5 = mt5

    def initialize(self, path: str | None = None) -> bool:
        return bool(self._mt5.initialize(path) if path else self._mt5.initialize())

    def login(self, login: int, password: str, server: str) -> bool:
        return bool(self._mt5.login(login, password=password, server=server))

    def last_error(self) -> tuple[int, str]:
        extra = self._mt5.last_error()
        if isinstance(extra, tuple) and len(extra) >= 2:
            return int(extra[0]), str(extra[1])
        return -1, "unknown"

    def symbol_info_tick(self, symbol: str) -> Any:
        return self._mt5.symbol_info_tick(symbol)

    def copy_rates_from_pos(self, symbol: str, timeframe: int, start_pos: int, count: int) -> Any:
        return self._mt5.copy_rates_from_pos(symbol, timeframe, start_pos, count)

    def account_info(self) -> Any:
        return self._mt5.account_info()

    def shutdown(self) -> None:
        self._mt5.shutdown()


def _clean_symbol(symbol: str) -> str:
    if not isinstance(symbol, str) or not symbol.strip():
        raise ValueError("symbol must be a non-empty string")
    return symbol.strip().upper()


def _tick_last(tick: Any) -> float | None:
    if tick is None:
        return None
    last = getattr(tick, "last", None)
    bid = getattr(tick, "bid", None)
    ask = getattr(tick, "ask", None)
    for candidate in (last, bid, ask):
        if isinstance(candidate, (int, float)) and not isinstance(candidate, bool) and isfinite(float(candidate)) and float(candidate) > 0:
            return float(candidate)
    return None


def _tick_change_percent(tick: Any, last: float) -> float | None:
    prev = getattr(tick, "prev_close", None) or getattr(tick, "close", None)
    if not isinstance(prev, (int, float)) or isinstance(prev, bool) or not isfinite(float(prev)) or float(prev) == 0:
        return None
    return ((last - float(prev)) / float(prev)) * 100.0


class MetaTraderQuoteProvider:
    """Logged-in MT5 terminal quotes and bars. Credentials never enter a read model."""

    provider_id = MT5_PROVIDER_ID

    def __init__(
        self,
        login: str,
        password: str,
        server: str,
        *,
        terminal_path: str | None = None,
        api: MetaTraderApi | None = None,
    ) -> None:
        login_text = login.strip() if isinstance(login, str) else ""
        password_text = password.strip() if isinstance(password, str) else ""
        server_text = server.strip() if isinstance(server, str) else ""
        if not login_text or not password_text or not server_text:
            raise ValueError("MetaTrader quotes require login, password, and server")
        try:
            self._login = int(login_text)
        except ValueError as extra:
            raise ValueError("MT5_LOGIN must be an account number") from extra
        self._password = password_text
        self._server = server_text
        self._terminal_path = terminal_path.strip() if isinstance(terminal_path, str) and terminal_path.strip() else None
        self._api = api
        self._ready = False

    def _connect(self) -> MetaTraderApi:
        if self._api is None:
            try:
                self._api = _OfficialMetaTraderApi()
            except ImportError as extra:
                raise RuntimeError("MetaTrader5 Python package is not installed") from extra
        if self._ready:
            return self._api
        if not self._api.initialize(self._terminal_path):
            code, detail = self._api.last_error()
            raise RuntimeError(f"MetaTrader terminal initialize failed ({code}): {detail}")
        if not self._api.login(self._login, self._password, self._server):
            code, detail = self._api.last_error()
            raise RuntimeError(f"MetaTrader login failed ({code}): {detail}")
        self._ready = True
        return self._api

    def fetch_quotes(self, symbols: Sequence[str]) -> Sequence[MarketQuote]:
        api = self._connect()
        quotes: list[MarketQuote] = []
        for raw in symbols:
            symbol = _clean_symbol(raw)
            tick = api.symbol_info_tick(symbol)
            last = _tick_last(tick)
            if last is None:
                continue
            observed = getattr(tick, "time", None)
            if isinstance(observed, (int, float)) and not isinstance(observed, bool):
                stamp = datetime.fromtimestamp(float(observed), tz=timezone.utc)
            else:
                stamp = datetime.now(timezone.utc)
            quotes.append(MarketQuote(symbol, last, _tick_change_percent(tick, last), stamp))
        return quotes

    def fetch_bars(self, symbol: str, *, timeframe: str = "M15", count: int = 100) -> list[dict[str, object]]:
        api = self._connect()
        cleaned = _clean_symbol(symbol)
        if not isinstance(count, int) or isinstance(count, bool) or count <= 0 or count > 5000:
            raise ValueError("count must be a positive integer up to 5000")
        tf = _TIMEFRAMES.get(timeframe.strip().upper() if isinstance(timeframe, str) else "")
        if tf is None:
            raise ValueError("timeframe is not a supported MetaTrader period")
        rows = api.copy_rates_from_pos(cleaned, tf, 0, count)
        if rows is None:
            return []
        bars: list[dict[str, object]] = []
        for row in rows:
            try:
                time_value = int(row["time"] if isinstance(row, dict) else row["time"])
                bars.append(
                    {
                        "time": datetime.fromtimestamp(time_value, tz=timezone.utc).isoformat().replace("+00:00", "Z"),
                        "open": float(row["open"]),
                        "high": float(row["high"]),
                        "low": float(row["low"]),
                        "close": float(row["close"]),
                    }
                )
            except (KeyError, TypeError, ValueError):
                continue
        return bars

    def account_snapshot(self) -> dict[str, object]:
        info = self._connect().account_info()
        if info is None:
            raise RuntimeError("MetaTrader account is not available")
        return {
            "login": getattr(info, "login", None),
            "server": getattr(info, "server", None),
            "currency": getattr(info, "currency", None),
            "balance": getattr(info, "balance", None),
            "equity": getattr(info, "equity", None),
            "margin": getattr(info, "margin", None),
            "margin_free": getattr(info, "margin_free", None),
            "profit": getattr(info, "profit", None),
        }


_TIMEFRAMES = {
    "M1": 1,
    "M5": 5,
    "M15": 15,
    "M30": 30,
    "H1": 16385,
    "H4": 16388,
    "D1": 16408,
    "W1": 32769,
    "MN1": 49153,
}


def metatrader_credentials(environ: Mapping[str, str] | None = None) -> tuple[str, str, str, str | None] | None:
    source = environ if environ is not None else os.environ
    login = (source.get(MT5_LOGIN_ENV) or "").strip()
    password = (source.get(MT5_PASSWORD_ENV) or "").strip()
    server = (source.get(MT5_SERVER_ENV) or "").strip()
    if not login or not password or not server:
        return None
    path = (source.get(MT5_TERMINAL_PATH_ENV) or "").strip() or None
    return login, password, server, path


def metatrader_provider_from_env(
    environ: Mapping[str, str] | None = None,
    *,
    api: MetaTraderApi | None = None,
) -> MetaTraderQuoteProvider | None:
    creds = metatrader_credentials(environ)
    if creds is None:
        return None
    login, password, server, path = creds
    return MetaTraderQuoteProvider(login, password, server, terminal_path=path, api=api)
