"""Isolated, explicit metadata read through SQX's installed MetaTrader5 dependency.

initialize is not attach-only: a terminal closing after the identity check can be
reopened by the API. The caller must explain that race before this explicit action.
No login, trading, symbol selection, or native data import occurs here. An explicit
history request can retrieve a bounded range of bars from the terminal/broker.
"""
from __future__ import annotations

import ctypes
from ctypes import wintypes
from datetime import datetime, timedelta, timezone
from hashlib import sha256
import json
import math
from numbers import Integral, Real
from pathlib import Path
import re
import sys

TEXT_FIELDS = ("name", "description", "path", "currency_base", "currency_profit", "currency_margin")
INT_FIELDS = ("digits", "spread", "trade_calc_mode", "swap_mode")
NUMBER_FIELDS = ("point", "trade_tick_size", "trade_tick_value", "trade_tick_value_profit",
                 "trade_tick_value_loss", "trade_contract_size", "volume_min", "volume_max",
                 "volume_step", "swap_long", "swap_short")
SYMBOL_FIELDS = TEXT_FIELDS + INT_FIELDS + NUMBER_FIELDS + ("spread_float",)
MAX_SYMBOLS = 4096
HISTORY_PERIODS = {"M1": 60, "M5": 300, "M15": 900, "M30": 1800, "H1": 3600, "H4": 14400, "D1": 86400}
HISTORY_COLUMNS = ("time", "open", "high", "low", "close", "tick_volume", "spread", "real_volume")
MAX_HISTORY_BARS = 10000


class ProbeError(ValueError):
    def __init__(self, code, api_error_code=None):
        super().__init__(code)
        self.api_error_code = api_error_code


def process_identity(pid):
    """Read only the existing process image and creation time; never start a process."""
    kernel = ctypes.WinDLL("kernel32", use_last_error=True)
    kernel.OpenProcess.argtypes = [wintypes.DWORD, wintypes.BOOL, wintypes.DWORD]
    kernel.OpenProcess.restype = wintypes.HANDLE
    kernel.CloseHandle.argtypes = [wintypes.HANDLE]
    kernel.GetExitCodeProcess.argtypes = [wintypes.HANDLE, ctypes.POINTER(wintypes.DWORD)]
    kernel.QueryFullProcessImageNameW.argtypes = [wintypes.HANDLE, wintypes.DWORD, wintypes.LPWSTR, ctypes.POINTER(wintypes.DWORD)]
    kernel.GetProcessTimes.argtypes = [wintypes.HANDLE] + [ctypes.POINTER(wintypes.FILETIME)] * 4
    handle = kernel.OpenProcess(0x1000, False, pid)
    if not handle:
        raise ValueError("mt5_terminal_changed")
    try:
        code = wintypes.DWORD()
        path = ctypes.create_unicode_buffer(32768)
        size = wintypes.DWORD(len(path))
        times = [wintypes.FILETIME() for _ in range(4)]
        if (not kernel.GetExitCodeProcess(handle, ctypes.byref(code)) or code.value != 259
                or not kernel.QueryFullProcessImageNameW(handle, 0, path, ctypes.byref(size))
                or not kernel.GetProcessTimes(handle, *(ctypes.byref(t) for t in times))):
            raise ValueError("mt5_terminal_changed")
        return {"pid": pid, "path": str(Path(path.value).resolve()),
                "started": str((times[0].dwHighDateTime << 32) | times[0].dwLowDateTime)}
    finally:
        kernel.CloseHandle(handle)


def _text(value):
    return value if isinstance(value, str) and 0 < len(value) <= 512 and value.strip() and all(ord(c) >= 32 for c in value) else None


def _symbol(value):
    result = {}
    for key in SYMBOL_FIELDS:
        item = getattr(value, key, None)
        if key in TEXT_FIELDS:
            result[key] = _text(item)
        elif key in INT_FIELDS:
            result[key] = item if type(item) is int and abs(item) <= 2**53 else None
        elif key == "spread_float":
            result[key] = item if type(item) is bool else None
        else:
            result[key] = item if type(item) in (int, float) and math.isfinite(item) and abs(item) <= 1e100 else None
    if not result["name"]:
        raise ValueError("mt5_metadata_invalid")
    return result


def read_metadata(request, mt5, identity_reader=process_identity):
    if (not isinstance(request, dict) or not {"process", "data_paths", "symbol"} <= set(request)
            or set(request) - {"process", "data_paths", "symbol", "symbol_filter", "history"}):
        raise ValueError("mt5_request_invalid")
    history = request.get("history")
    if history is not None:
        try:
            if (set(history) != {"broker_sha256", "timeframe", "date_from", "date_to"}
                    or not re.fullmatch(r"[a-f0-9]{64}", history["broker_sha256"])
                    or history["timeframe"] not in HISTORY_PERIODS
                    or not all(re.fullmatch(r"[0-9]{4}-[0-9]{2}-[0-9]{2}", history[k]) for k in ("date_from", "date_to"))
                    or _text(request["symbol"]) is None):
                raise ValueError()
            start, end = (datetime.strptime(history[k], "%Y-%m-%d").replace(tzinfo=timezone.utc) for k in ("date_from", "date_to"))
            if not datetime(1970, 1, 1, tzinfo=timezone.utc) <= start < end or end.date() > datetime.now(timezone.utc).date():
                raise ValueError()
            if (end - start).total_seconds() / HISTORY_PERIODS[history["timeframe"]] > MAX_HISTORY_BARS:
                raise ValueError()
        except (TypeError, KeyError, ValueError):
            raise ValueError("mt5_history_request_invalid") from None
    symbol_filter = request.get("symbol_filter")
    if symbol_filter is not None and (not isinstance(symbol_filter, str) or not symbol_filter.strip()
            or not re.fullmatch(r"[A-Za-z0-9._# /-]{2,64}", symbol_filter)):
        raise ValueError("mt5_request_invalid")
    expected = request["process"]
    if (not isinstance(expected, dict) or set(expected) != {"pid", "path", "started"}
            or type(expected["pid"]) is not int or expected["pid"] <= 0
            or not isinstance(expected["started"], str) or not expected["started"].isdigit()
            or not isinstance(expected["path"], str) or not Path(expected["path"]).is_absolute()
            or Path(expected["path"]).name.lower() != "terminal64.exe"
            or not isinstance(request["data_paths"], list) or not request["data_paths"]
            or not all(isinstance(p, str) and Path(p).is_absolute() for p in request["data_paths"])
            or (request["symbol"] is not None and _text(request["symbol"]) is None)):
        raise ValueError("mt5_request_invalid")

    def check_identity():
        if identity_reader(expected["pid"]) != expected:
            raise ValueError("mt5_terminal_changed")

    def broker_info():
        account = mt5.account_info()
        result = {key: _text(getattr(account, key, None)) for key in ("company", "server", "currency")}
        if not all(result.values()):
            raise ValueError("mt5_account_unavailable")
        return result

    check_identity()
    try:
        # No portable flag, credentials, or automatic terminal discovery.
        if not mt5.initialize(expected["path"], timeout=5000):
            error = mt5.last_error()
            number = error[0] if isinstance(error, (tuple, list)) and error else None
            number = number if type(number) is int and -(2**31) <= number < 2**31 else None
            raise ProbeError("mt5_connection_failed", number)
        check_identity()
        info = mt5.terminal_info()
        if info is None or getattr(info, "connected", None) is not True:
            raise ValueError("mt5_terminal_disconnected")
        terminal_path = str(Path(info.path).resolve())
        data_path = str(Path(info.data_path).resolve())
        if terminal_path != str(Path(expected["path"]).parent) or data_path not in request["data_paths"]:
            raise ValueError("mt5_terminal_context_mismatch")
        broker = broker_info()
        selected = None
        bars = None
        if history is not None:
            digest = sha256(json.dumps(broker, sort_keys=True, separators=(",", ":"), allow_nan=False).encode()).hexdigest()
            if digest != history["broker_sha256"]:
                raise ValueError("mt5_broker_changed")
            selected = _symbol(mt5.symbol_info(request["symbol"]))
            if selected["name"] != request["symbol"]:
                raise ValueError("mt5_symbol_unavailable")
            symbols = [selected]
            raw = mt5.copy_rates_range(request["symbol"], getattr(mt5, "TIMEFRAME_" + history["timeframe"]), start, end - timedelta(seconds=1))
            if raw is None or not len(raw):
                raise ValueError("mt5_history_empty")
            if len(raw) > MAX_HISTORY_BARS:
                raise ValueError("mt5_history_limit")
            bars = []
            previous = None
            for item in raw:
                bar = {}
                for key in HISTORY_COLUMNS:
                    value = item[key]
                    if key in ("time", "tick_volume", "spread", "real_volume"):
                        if isinstance(value, bool) or not isinstance(value, Integral) or value < 0 or value > 2**53:
                            raise ValueError("mt5_history_invalid")
                        bar[key] = int(value)
                    else:
                        if isinstance(value, bool) or not isinstance(value, Real) or not math.isfinite(value):
                            raise ValueError("mt5_history_invalid")
                        bar[key] = float(value)
                if (not start.timestamp() <= bar["time"] < end.timestamp()
                        or previous is not None and bar["time"] <= previous
                        or not bar["low"] <= min(bar["open"], bar["close"]) <= max(bar["open"], bar["close"]) <= bar["high"]):
                    raise ValueError("mt5_history_invalid")
                if bar["time"] + HISTORY_PERIODS[history["timeframe"]] > datetime.now(timezone.utc).timestamp():
                    raise ValueError("mt5_history_open_bar")
                previous = bar["time"]
                bars.append(bar)
        else:
            raw = mt5.symbols_get(group="*" + symbol_filter + "*") if symbol_filter is not None else mt5.symbols_get()
            if raw is None or not len(raw):
                raise ValueError("mt5_symbols_unavailable")
            if len(raw) > MAX_SYMBOLS:
                raise ValueError("mt5_metadata_limit")
            symbols = sorted((_symbol(row) for row in raw), key=lambda row: row["name"])
            names = [row["name"] for row in symbols]
            if len(set(names)) != len(names):
                raise ValueError("mt5_metadata_invalid")
            if request.get("symbol") is not None:
                if request["symbol"] not in names:
                    raise ValueError("mt5_symbol_unavailable")
                selected = _symbol(mt5.symbol_info(request["symbol"]))
                if selected["name"] != request["symbol"]:
                    raise ValueError("mt5_metadata_invalid")
        if broker_info() != broker:
            raise ValueError("mt5_broker_changed")
        after = mt5.terminal_info()
        if (after is None or after.connected is not True or str(Path(after.path).resolve()) != terminal_path
                or str(Path(after.data_path).resolve()) != data_path):
            raise ValueError("mt5_terminal_context_mismatch")
        check_identity()
        result = {"schema": "tc.mt5-probe.v1", "process": expected, "data_path": data_path,
                "terminal": {"company": _text(info.company), "build": info.build, "connected": True},
                "broker": broker, "symbols": symbols, "selected_symbol": selected}
        if bars is not None:
            result["history"] = bars
        return result
    finally:
        mt5.shutdown()


def main():
    try:
        raw = sys.stdin.buffer.read(16385)
        if len(raw) > 16384:
            raise ValueError("mt5_request_invalid")
        request = json.loads(raw)
        import MetaTrader5 as mt5
        result = read_metadata(request, mt5)
        sys.stdout.write(json.dumps(result, allow_nan=False, separators=(",", ":")))
    except Exception as exc:
        # Never print terminal paths, account objects, or upstream exception text.
        code = str(exc) if isinstance(exc, ValueError) and str(exc) in {
            "mt5_terminal_changed", "mt5_account_unavailable", "mt5_connection_failed",
            "mt5_terminal_disconnected", "mt5_terminal_context_mismatch", "mt5_symbols_unavailable",
            "mt5_symbol_unavailable", "mt5_metadata_invalid", "mt5_broker_changed", "mt5_request_invalid", "mt5_metadata_limit",
            "mt5_history_request_invalid", "mt5_history_empty", "mt5_history_limit", "mt5_history_invalid",
            "mt5_history_open_bar",
        } else "mt5_probe_failed"
        result = {"error": code}
        if isinstance(exc, ProbeError) and exc.api_error_code is not None:
            result["api_error_code"] = exc.api_error_code
        sys.stdout.write(json.dumps(result))
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
