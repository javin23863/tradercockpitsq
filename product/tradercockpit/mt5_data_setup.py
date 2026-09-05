"""Passive MT5 discovery and an explicit, bounded read of an existing terminal."""
from __future__ import annotations

from datetime import datetime, timezone
import csv
from hashlib import sha256
from io import StringIO
import json
import math
import os
from pathlib import Path
import re
import subprocess
from threading import Lock, Thread

from .data_setup import DataSetupError, _inside
from .mt5_metadata_probe import (TEXT_FIELDS, INT_FIELDS, NUMBER_FIELDS, SYMBOL_FIELDS, MAX_SYMBOLS,
                                 HISTORY_PERIODS as _HISTORY_PERIODS, HISTORY_COLUMNS as _HISTORY_COLUMNS)
from .sqx_presets import SQX_BUILD, SqxPresetRuntimeError, verified_sqx_home
from .research_custody import EvidenceRef, ResearchCustodyError

MAX_OUTPUT_BYTES = 4 * 1024 * 1024
_READ_LOCK = Lock()  # ponytail: one explicit metadata read at a time on this desktop.
_HISTORY_UNRESOLVED = ["broker_timezone", "trading_sessions", "commission", "slippage", "native_broker_profile"]
_DETAIL = "Open MetaTrader 5 and log in to your broker, then explicitly read terminal settings. The API can reopen a terminal if it closes during connection."


class Mt5DataSetupError(ValueError):
    def __init__(self, code, detail, native_error_code=None):
        self.code, self.detail = code, detail
        self.native_error_code = native_error_code
        super().__init__(f"{code}: {detail}")


def _connection_error_detail(number):
    # MetaQuotes last_error contract and the installed MetaTrader5 constants.
    # https://www.mql5.com/en/docs/python_metatrader5/mt5lasterror_py
    meanings = {-1: "general failure", -2: "invalid parameters", -3: "insufficient memory",
                -4: "requested history unavailable", -5: "incompatible API version",
                -6: "authorization failed; check the terminal's broker login",
                -7: "unsupported API method", -8: "automatic trading disabled",
                -10000: "internal terminal communication failure", -10001: "terminal communication send failed",
                -10002: "terminal communication receive failed", -10003: "terminal communication initialization failed",
                -10004: "terminal communication connection unavailable", -10005: "terminal communication timed out"}
    return f"MetaTrader5 API error {number}: {meanings.get(number, 'unrecognized native failure')}."


def _digest(value):
    return sha256(json.dumps(value, sort_keys=True, separators=(",", ":"), allow_nan=False).encode()).hexdigest()


def _runtime(sqx_home):
    try:
        home = verified_sqx_home(sqx_home)
        python = _inside(home, "internal/python/runtime/python.exe")
        dependency = _inside(home, "internal/python/runtime/Lib/site-packages/MetaTrader5/__init__.py")
        if not python.is_file() or not dependency.is_file():
            raise Mt5DataSetupError("mt5_dependency_unavailable", "The verified SQX runtime has no bundled MT5 metadata dependency.")
        return python
    except (SqxPresetRuntimeError, DataSetupError) as exc:
        raise Mt5DataSetupError(exc.code, exc.detail) from exc


def _running_terminals():
    if os.name != "nt":
        raise Mt5DataSetupError("mt5_discovery_unavailable", "MT5 terminal discovery requires Windows.")
    # Constant, read-only query: no request values enter PowerShell.
    script = (
        "$ErrorActionPreference='Stop'; "
        "@(Get-Process -Name terminal64 -ErrorAction SilentlyContinue | ForEach-Object { "
        "[pscustomobject]@{pid=$_.Id;path=$_.Path;started=$_.StartTime.ToFileTimeUtc().ToString()} "
        "}) | ConvertTo-Json -Compress"
    )
    try:
        result = subprocess.run(
            [str(Path(os.environ["SystemRoot"]) / "System32/WindowsPowerShell/v1.0/powershell.exe"),
             "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-Command", script],
            stdin=subprocess.DEVNULL, capture_output=True, text=True, timeout=5, check=False,
            shell=False, creationflags=subprocess.CREATE_NO_WINDOW,
        )
        if result.returncode or len(result.stdout) > 32768:
            raise ValueError()
        rows = json.loads(result.stdout) if result.stdout.strip() else []
        rows = [rows] if isinstance(rows, dict) else rows
        if not isinstance(rows, list) or len(rows) > 32:
            raise ValueError()
        found = []
        for row in rows:
            if (set(row) != {"pid", "path", "started"} or type(row["pid"]) is not int or row["pid"] <= 0
                    or not isinstance(row["started"], str) or not re.fullmatch(r"[0-9]{1,20}", row["started"])
                    or not isinstance(row["path"], str)):
                raise ValueError()
            path = Path(row["path"])
            if not path.is_absolute() or path.name.lower() != "terminal64.exe" or not path.is_file():
                raise ValueError()
            if path.resolve() != path or any(p.is_symlink() for p in (path, *path.parents)):
                raise ValueError()
            found.append({**row, "path": str(path.resolve())})
        if len({r["path"].casefold() for r in found}) != len(found):
            raise Mt5DataSetupError("mt5_terminal_ambiguous", "More than one running terminal uses the same installation.")
        return sorted(found, key=lambda row: row["pid"])
    except Mt5DataSetupError:
        raise
    except (OSError, ValueError, KeyError, TypeError, subprocess.SubprocessError) as exc:
        raise Mt5DataSetupError("mt5_discovery_unavailable", "Running MT5 terminal identity could not be verified.") from exc


def _public_terminal(row):
    return {"terminal_id": f"mt5-{row['pid']}", "identity_sha256": _digest(row),
            "label": f"{Path(row['path']).parent.name} · {row['pid']}", "running": True}


def read_mt5_terminal_catalog(sqx_home):
    base = {"schema": "tc.mt5-terminals.v1", "terminals": [], "detail": _DETAIL,
            "native_import_performed": False, "backtest_ready": False}
    try:
        _runtime(sqx_home)
        rows = _running_terminals()
        return {**base, "status": "available" if rows else "no_running_terminal",
                "terminals": [_public_terminal(row) for row in rows],
                "reason_code": None if rows else "mt5_terminal_not_running"}
    except Mt5DataSetupError as exc:
        return {**base, "status": "unavailable", "reason_code": exc.code, "detail": exc.detail}


def _data_paths(terminal):
    """Recognize existing portable or MT5 origin-linked standard data directories."""
    install = Path(terminal).parent.resolve()
    result = [str(install)]
    appdata = os.environ.get("APPDATA")
    if not appdata:
        return result
    root = Path(appdata) / "MetaQuotes/Terminal"
    if not root.is_dir() or root.resolve() != root:
        return result
    for folder in list(root.iterdir())[:128]:
        origin = folder / "origin.txt"
        if folder.resolve() != folder or not origin.is_file() or origin.resolve() != origin or origin.stat().st_size > 4096:
            continue
        try:
            raw = origin.read_bytes()
            text = raw.decode("utf-16" if raw.startswith((b"\xff\xfe", b"\xfe\xff")) else "utf-8-sig").strip()
            if Path(text).resolve() == install:
                result.append(str(folder))
        except (OSError, UnicodeError, ValueError):
            continue
    return sorted(set(result))


def _stop(process):
    if process.poll() is None:
        try:
            process.terminate()
            process.wait(timeout=1)
        except (OSError, subprocess.TimeoutExpired):
            try:
                process.kill()
                process.wait(timeout=1)
            except (OSError, subprocess.TimeoutExpired) as exc:
                raise Mt5DataSetupError("mt5_worker_cleanup_failed", "The metadata worker did not stop within its cleanup deadline.") from exc


def _probe(python, request, register_worker):
    worker = Path(__file__).with_name("mt5_metadata_probe.py").resolve()
    if not worker.is_file():
        raise Mt5DataSetupError("mt5_worker_unavailable", "The packaged metadata reader is missing.")
    process = None
    output = []
    reader = None
    try:
        process = subprocess.Popen(
            [str(python), "-I", "-B", str(worker)], stdin=subprocess.PIPE, stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL, shell=False, creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
        )
        register_worker(process, label="mt5-metadata", timeout_seconds=1)
        # ponytail: one bounded reader for this synchronous desktop action; no job framework.
        def capture():
            try:
                data = process.stdout.read(MAX_OUTPUT_BYTES + 1)
                output.append(data)
                if len(data) > MAX_OUTPUT_BYTES and process.poll() is None:
                    process.kill()
            except OSError:
                pass
        reader = Thread(target=capture, daemon=True)
        reader.start()
        process.stdin.write(json.dumps(request, separators=(",", ":")).encode())
        process.stdin.close()
        process.wait(timeout=30)
        reader.join(timeout=1)
        if reader.is_alive() or len(output) != 1 or not output[0] or len(output[0]) > MAX_OUTPUT_BYTES:
            raise Mt5DataSetupError("mt5_probe_invalid", "The terminal metadata response exceeded its bounded contract.")
        result = json.loads(output[0])
        if process.returncode != 0 or not isinstance(result, dict) or "error" in result:
            code = result.get("error") if isinstance(result, dict) else None
            allowed = {"mt5_terminal_changed", "mt5_account_unavailable", "mt5_connection_failed",
                       "mt5_terminal_disconnected", "mt5_terminal_context_mismatch", "mt5_symbols_unavailable",
                       "mt5_symbol_unavailable", "mt5_metadata_invalid", "mt5_broker_changed", "mt5_metadata_limit",
                       "mt5_history_request_invalid", "mt5_history_empty", "mt5_history_limit", "mt5_history_invalid", "mt5_history_open_bar"}
            number = result.get("api_error_code") if isinstance(result, dict) else None
            if isinstance(result, dict) and (set(result) - {"error", "api_error_code"}
                    or (number is not None and (type(number) is not int or not -(2**31) <= number < 2**31))):
                raise Mt5DataSetupError("mt5_probe_invalid", "The terminal error response is outside the allowed contract.")
            detail = _connection_error_detail(number) if code == "mt5_connection_failed" and number is not None else "MT5 metadata could not be read from the selected connected terminal."
            if code == "mt5_metadata_limit":
                detail = "Enter part of a symbol name to narrow the broker catalog; the current selection exceeds the metadata limit."
            elif code == "mt5_history_empty":
                detail = "The terminal returned no bars for this symbol and UTC date range. No history was captured. History may still be loading; retry after it is available in MT5, or choose another range."
            elif code in {"mt5_history_limit", "mt5_history_request_invalid"}:
                detail = "Choose a supported timeframe and a smaller range of closed UTC days (at most 10,000 possible bars)."
            elif code == "mt5_history_invalid":
                detail = "The terminal returned invalid or inconsistent bars. No history was captured."
            elif code == "mt5_history_open_bar":
                detail = "The returned range includes a bar whose timeframe has not elapsed. Choose an earlier end date."
            raise Mt5DataSetupError(code if code in allowed else "mt5_probe_failed", detail, number)
        return result
    except subprocess.TimeoutExpired as exc:
        raise Mt5DataSetupError("mt5_probe_timeout", "MT5 metadata read exceeded 30 seconds.") from exc
    except Mt5DataSetupError:
        raise
    except Exception as exc:
        raise Mt5DataSetupError("mt5_probe_failed", "The bounded MT5 metadata reader failed.") from exc
    finally:
        if process is not None:
            _stop(process)
            if reader is not None:
                reader.join(timeout=1)
            for stream in (process.stdin, process.stdout):
                if stream is not None:
                    stream.close()


def _valid_text(value):
    return isinstance(value, str) and 0 < len(value) <= 512 and bool(value.strip()) and all(ord(c) >= 32 for c in value)


def _validate_result(result, request):
    try:
        if (set(result) != {"schema", "process", "data_path", "terminal", "broker", "symbols", "selected_symbol"}
                or result["schema"] != "tc.mt5-probe.v1" or result["process"] != request["process"]
                or result["data_path"] not in request["data_paths"]):
            raise ValueError()
        terminal, broker = result["terminal"], result["broker"]
        if (set(terminal) != {"company", "build", "connected"} or terminal["connected"] is not True
                or type(terminal["build"]) is not int or not 0 < terminal["build"] < 100000
                or (terminal["company"] is not None and not _valid_text(terminal["company"]))
                or set(broker) != {"company", "server", "currency"} or not all(_valid_text(v) for v in broker.values())):
            raise ValueError()
        rows = result["symbols"]
        if not isinstance(rows, list) or not 0 < len(rows) <= MAX_SYMBOLS:
            raise ValueError()
        names = set()
        for row in rows + ([result["selected_symbol"]] if result["selected_symbol"] is not None else []):
            if set(row) != set(SYMBOL_FIELDS) or not _valid_text(row["name"]):
                raise ValueError()
            for key, value in row.items():
                if value is None:
                    continue
                if key in TEXT_FIELDS and not _valid_text(value):
                    raise ValueError()
                if key in INT_FIELDS and (type(value) is not int or abs(value) > 2**53):
                    raise ValueError()
                if key in NUMBER_FIELDS and (type(value) not in (int, float) or not math.isfinite(value) or abs(value) > 1e100):
                    raise ValueError()
                if key == "spread_float" and type(value) is not bool:
                    raise ValueError()
        for row in rows:
            if row["name"] in names:
                raise ValueError()
            names.add(row["name"])
        selected = result["selected_symbol"]
        if request.get("symbol") is None:
            if selected is not None:
                raise ValueError()
        elif selected is None or selected["name"] != request["symbol"] or selected["name"] not in names:
            raise ValueError()
    except (ValueError, TypeError, KeyError, AttributeError, OverflowError) as exc:
        raise Mt5DataSetupError("mt5_metadata_invalid", "MT5 returned metadata outside the allowed response contract.") from exc


def _read_metadata(sqx_home, payload, register_worker):
    if (not isinstance(payload, dict) or set(payload) - {"terminal_id", "identity_sha256", "symbol", "symbol_filter"}
            or not isinstance(payload.get("terminal_id"), str) or not re.fullmatch(r"mt5-[1-9][0-9]*", payload["terminal_id"])
            or not isinstance(payload.get("identity_sha256"), str) or not re.fullmatch(r"[a-f0-9]{64}", payload["identity_sha256"])
            or ("symbol" in payload and not _valid_text(payload["symbol"]))
            or ("symbol_filter" in payload and (not isinstance(payload["symbol_filter"], str)
                or not payload["symbol_filter"].strip() or not re.fullmatch(r"[A-Za-z0-9._# /-]{2,64}", payload["symbol_filter"])))):
        raise Mt5DataSetupError("mt5_request_invalid", "Choose an exact discovered terminal and optional native symbol.")
    python, row = _selected_process(sqx_home, payload, register_worker)
    request = {"process": row, "data_paths": _data_paths(row["path"]), "symbol": payload.get("symbol"),
               "symbol_filter": payload.get("symbol_filter")}
    result = _probe(python, request, register_worker)
    _validate_result(result, request)
    if row not in _running_terminals():
        raise Mt5DataSetupError("mt5_terminal_changed", "The selected terminal changed during metadata read.")
    return {"schema": "tc.mt5-metadata.v1", "status": "observed", "terminal_id": payload["terminal_id"],
            "identity_sha256": payload["identity_sha256"], "terminal": result["terminal"], "broker": result["broker"],
            "symbols": result["symbols"], "selected_symbol": result["selected_symbol"],
            "unresolved": ["timezone", "bar_timestamp_convention", "commission", "slippage", "trading_sessions"],
            "source": {"kind": "mt5_terminal_api", "producer": "MetaTrader5", "runtime_build": SQX_BUILD,
                       "observed_at_utc": datetime.now(timezone.utc).isoformat(), "symbol_filter": payload.get("symbol_filter"),
                       "broker_sha256": _digest(result["broker"])},
            "native_import_performed": False, "backtest_ready": False}


def _selected_process(sqx_home, payload, register_worker):
    python = _runtime(sqx_home)
    matches = [row for row in _running_terminals() if _public_terminal(row)["terminal_id"] == payload["terminal_id"]]
    if len(matches) != 1 or _digest(matches[0]) != payload["identity_sha256"]:
        raise Mt5DataSetupError("mt5_terminal_changed", "The selected terminal is closed or its identity changed. Refresh terminals.")
    if not callable(register_worker):
        raise Mt5DataSetupError("mt5_supervisor_unavailable", "The desktop metadata worker supervisor is unavailable.")
    return python, matches[0]


def read_mt5_metadata(sqx_home, payload, *, register_worker=None):
    if not _READ_LOCK.acquire(blocking=False):
        raise Mt5DataSetupError("mt5_read_busy", "A terminal metadata read is already running.")
    try:
        return _read_metadata(sqx_home, payload, register_worker)
    finally:
        _READ_LOCK.release()


def _history_request(payload):
    try:
        if (not isinstance(payload, dict) or set(payload) != {"terminal_id", "identity_sha256", "broker_sha256", "symbol", "timeframe", "date_from", "date_to"}
                or not isinstance(payload["terminal_id"], str) or not re.fullmatch(r"mt5-[1-9][0-9]*", payload["terminal_id"])
                or not all(isinstance(payload[k], str) and re.fullmatch(r"[a-f0-9]{64}", payload[k]) for k in ("identity_sha256", "broker_sha256"))
                or not _valid_text(payload["symbol"]) or payload["timeframe"] not in _HISTORY_PERIODS
                or not all(isinstance(payload[k], str) and re.fullmatch(r"[0-9]{4}-[0-9]{2}-[0-9]{2}", payload[k]) for k in ("date_from", "date_to"))):
            raise ValueError()
        start, end = (datetime.strptime(payload[k], "%Y-%m-%d").replace(tzinfo=timezone.utc) for k in ("date_from", "date_to"))
        if not datetime(1970, 1, 1, tzinfo=timezone.utc) <= start < end or end.date() > datetime.now(timezone.utc).date():
            raise ValueError()
        if (end - start).total_seconds() / _HISTORY_PERIODS[payload["timeframe"]] > 10000:
            raise Mt5DataSetupError("mt5_history_limit", "Choose a smaller UTC date range (at most 10,000 possible bars).")
        return int(start.timestamp()), int(end.timestamp()), _HISTORY_PERIODS[payload["timeframe"]]
    except (KeyError, TypeError, ValueError) as exc:
        if isinstance(exc, Mt5DataSetupError):
            raise
        raise Mt5DataSetupError("mt5_history_request_invalid", "Select the observed broker and symbol, supported timeframe, and an increasing range of closed UTC days.") from exc


def _validated_bars(rows, payload, *, now=None):
    start, end, period = _history_request(payload)
    observed_now = (now or datetime.now(timezone.utc)).timestamp()
    if not isinstance(rows, list) or not rows:
        raise Mt5DataSetupError("mt5_history_empty", "No history bars were returned.")
    if len(rows) > 10000:
        raise Mt5DataSetupError("mt5_history_limit", "History exceeds the 10,000-bar capture limit.")
    previous, gaps = None, 0
    for row in rows:
        if not isinstance(row, dict) or set(row) != set(_HISTORY_COLUMNS):
            raise Mt5DataSetupError("mt5_history_invalid", "History columns do not match the native bar contract.")
        for key in _HISTORY_COLUMNS:
            value = row[key]
            if key in ("time", "tick_volume", "spread", "real_volume"):
                valid = type(value) is int and 0 <= value <= 2**53
            else:
                valid = type(value) in (int, float) and math.isfinite(value)
            if not valid:
                raise Mt5DataSetupError("mt5_history_invalid", "History contains invalid numeric values.")
        if (not start <= row["time"] < end or previous is not None and row["time"] <= previous
                or not row["low"] <= min(row["open"], row["close"]) <= max(row["open"], row["close"]) <= row["high"]):
            raise Mt5DataSetupError("mt5_history_invalid", "History has inconsistent OHLC, ordering, or requested UTC range.")
        if row["time"] + period > observed_now:
            raise Mt5DataSetupError("mt5_history_open_bar", "A returned bar's timeframe has not elapsed. Choose an earlier end date.")
        if previous is not None and row["time"] - previous > period:
            gaps += 1
        previous = row["time"]
    return {"row_count": len(rows), "date_from": datetime.fromtimestamp(rows[0]["time"], timezone.utc).isoformat(),
            "date_to": datetime.fromtimestamp(rows[-1]["time"], timezone.utc).isoformat(), "gap_count": gaps}


def _history_csv(rows):
    stream = StringIO(newline="")
    writer = csv.writer(stream, lineterminator="\n")
    writer.writerow(_HISTORY_COLUMNS)
    for row in rows:
        writer.writerow([datetime.fromtimestamp(row["time"], timezone.utc).isoformat(),
                         *[float(row[k]) if k in ("open", "high", "low", "close") else row[k] for k in _HISTORY_COLUMNS[1:]]])
    return stream.getvalue().encode("utf-8")


def read_mt5_history(sqx_home, payload, *, store, register_worker=None):
    _history_request(payload)
    if not callable(getattr(store, "put_evidence", None)) or not callable(getattr(store, "read_evidence", None)):
        raise Mt5DataSetupError("mt5_history_custody_unavailable", "History capture requires the existing immutable evidence store.")
    if not _READ_LOCK.acquire(blocking=False):
        raise Mt5DataSetupError("mt5_read_busy", "A terminal metadata or history read is already running.")
    try:
        python, process = _selected_process(sqx_home, payload, register_worker)
        request = {"process": process, "data_paths": _data_paths(process["path"]), "symbol": payload["symbol"],
                   "history": {k: payload[k] for k in ("broker_sha256", "timeframe", "date_from", "date_to")}}
        result = _probe(python, request, register_worker)
        if not isinstance(result, dict) or "history" not in result:
            raise Mt5DataSetupError("mt5_history_invalid", "The terminal did not return history bars.")
        _validate_result({k: v for k, v in result.items() if k != "history"}, request)
        if _digest(result["broker"]) != payload["broker_sha256"]:
            raise Mt5DataSetupError("mt5_broker_changed", "The terminal broker changed. Read terminal settings again.")
        stats = _validated_bars(result["history"], payload)
        if process not in _running_terminals():
            raise Mt5DataSetupError("mt5_terminal_changed", "The selected terminal changed during history capture.")
        raw = _history_csv(result["history"])
        if len(raw) > MAX_OUTPUT_BYTES:
            raise Mt5DataSetupError("mt5_history_limit", "The captured CSV exceeds the bounded output size.")
        csv_ref = store.put_evidence(raw)
        manifest = {"schema": "tc.mt5-history-manifest.v1", "status": "captured", "request": dict(payload),
                    "csv_ref": str(csv_ref), "source_sha256": sha256(raw).hexdigest(), "bytes": len(raw), **stats,
                    "timezone": "UTC", "bar_timestamp_convention": "start_of_bar", "coverage_complete": None,
                    "broker": result["broker"], "symbol_metadata": result["selected_symbol"],
                    "source": {"kind": "mt5_terminal_api", "producer": "MetaTrader5", "runtime_build": SQX_BUILD,
                               "broker_sha256": payload["broker_sha256"], "observed_at_utc": datetime.now(timezone.utc).isoformat()},
                    "unresolved": list(_HISTORY_UNRESOLVED), "native_import_performed": False, "backtest_ready": False}
        history_ref = store.put_evidence(json.dumps(manifest, sort_keys=True, separators=(",", ":"), allow_nan=False).encode())
        return {**manifest, "schema": "tc.mt5-history.v1", "history_ref": str(history_ref)}
    except (ResearchCustodyError, OSError) as exc:
        raise Mt5DataSetupError("mt5_history_custody_failed", "History could not be retained with verified immutable custody.") from exc
    finally:
        _READ_LOCK.release()


def read_mt5_history_csv(store, payload):
    if not isinstance(payload, dict) or set(payload) != {"history_ref"}:
        raise Mt5DataSetupError("mt5_history_request_invalid", "Choose an exact captured history reference.")
    try:
        encoded = store.read_evidence(EvidenceRef.parse(payload["history_ref"]))
        if len(encoded) > 65536:
            raise ValueError()
        manifest = json.loads(encoded)
        if (set(manifest) != {"schema", "status", "request", "csv_ref", "source_sha256", "bytes", "row_count", "date_from", "date_to", "gap_count",
                             "timezone", "bar_timestamp_convention", "coverage_complete", "broker", "symbol_metadata", "source", "unresolved", "native_import_performed", "backtest_ready"}
                or manifest["schema"] != "tc.mt5-history-manifest.v1" or manifest["status"] != "captured"
                or manifest["timezone"] != "UTC" or manifest["bar_timestamp_convention"] != "start_of_bar"
                or manifest["coverage_complete"] is not None or manifest["native_import_performed"] is not False
                or manifest["backtest_ready"] is not False or manifest["unresolved"] != _HISTORY_UNRESOLVED):
            raise ValueError()
        request, source = manifest["request"], manifest["source"]
        _history_request(request)
        observed = datetime.fromisoformat(source["observed_at_utc"])
        if (set(source) != {"kind", "producer", "runtime_build", "broker_sha256", "observed_at_utc"}
                or source["kind"] != "mt5_terminal_api" or source["producer"] != "MetaTrader5" or source["runtime_build"] != SQX_BUILD
                or source["broker_sha256"] != request["broker_sha256"] or _digest(manifest["broker"]) != request["broker_sha256"]
                or observed.utcoffset() != timezone.utc.utcoffset(None) or observed > datetime.now(timezone.utc)):
            raise ValueError()
        # Reuse the independent metadata validator for the retained symbol/broker projection.
        marker = {"pid": 1, "path": "retained", "started": "1"}
        _validate_result({"schema": "tc.mt5-probe.v1", "process": marker, "data_path": "retained",
                          "terminal": {"company": None, "build": 1, "connected": True}, "broker": manifest["broker"],
                          "symbols": [manifest["symbol_metadata"]], "selected_symbol": manifest["symbol_metadata"]},
                         {"process": marker, "data_paths": ["retained"], "symbol": request["symbol"]})
        raw = store.read_evidence(EvidenceRef.parse(manifest["csv_ref"]))
        if (not 0 < len(raw) <= MAX_OUTPUT_BYTES or type(manifest["bytes"]) is not int or len(raw) != manifest["bytes"]
                or sha256(raw).hexdigest() != manifest["source_sha256"]):
            raise ValueError()
        reader = csv.DictReader(StringIO(raw.decode("utf-8"), newline=""), strict=True)
        if reader.fieldnames != list(_HISTORY_COLUMNS):
            raise ValueError()
        rows = []
        for row in reader:
            if len(rows) >= 10000 or set(row) != set(_HISTORY_COLUMNS):
                raise ValueError()
            when = datetime.fromisoformat(row["time"])
            if when.utcoffset() != timezone.utc.utcoffset(None) or when.microsecond:
                raise ValueError()
            rows.append({"time": int(when.timestamp()), **{key: int(row[key]) if key in ("tick_volume", "spread", "real_volume") else float(row[key]) for key in _HISTORY_COLUMNS[1:]}})
        stats = _validated_bars(rows, request, now=observed)
        if (any(manifest[k] != value for k, value in stats.items()) or type(manifest["row_count"]) is not int
                or type(manifest["gap_count"]) is not int or _history_csv(rows) != raw):
            raise ValueError()
        return raw
    except (ResearchCustodyError, ValueError, TypeError, KeyError, AttributeError, OSError, OverflowError, csv.Error) as exc:
        raise Mt5DataSetupError("mt5_history_custody_invalid", "The captured history manifest or CSV failed custody verification.") from exc
