"""Read native data profiles and inspect uploaded bars without importing or executing."""
from __future__ import annotations

from collections import Counter
from contextlib import closing
import csv
from datetime import datetime, timezone
from decimal import Decimal, InvalidOperation
from hashlib import sha256
from io import StringIO
import json
import math
from pathlib import Path
import re
import sqlite3

from tradercockpit.sqx_presets import SQX_BUILD, SqxPresetRuntimeError, verified_sqx_home
from tradercockpit.sqx_settings_lists import _installed_rows

MAX_DATA_FILE_BYTES = 16 * 1024 * 1024
MAX_DATA_FILE_ROWS = 500_000
_MT5_URL = "https://strategyquant.com/doc/quantdatamanager/metatrader5-data-import/"


class DataSetupError(ValueError):
    def __init__(self, code: str, detail: str):
        self.code, self.detail = code, detail
        super().__init__(f"{code}: {detail}")


def _inside(home: Path, relative: str) -> Path:
    path = home / relative
    for part in (path, *path.parents):
        if part == home:
            break
        if part.is_symlink() or part.resolve() != part:
            raise DataSetupError("data_catalog_path_escape", "Native data metadata path was redirected")
    return path


def _number(value):
    return value if type(value) in (int, float) and math.isfinite(value) else None


def _text(value):
    return value if isinstance(value, str) and value.strip() and not any(ord(c) < 32 for c in value) else None


def _native_catalog(sqx_home) -> dict:
    try:
        home = verified_sqx_home(sqx_home)
    except SqxPresetRuntimeError as exc:
        raise DataSetupError(exc.code, exc.detail) from exc
    path = _inside(home, "user/data/data.db")
    if not path.is_file():
        raise DataSetupError("native_data_catalog_missing", "The installed runtime has no native data catalog")
    queries = {
        "data": "SELECT ID,SOURCEDATA_ID,SYMBOL,INSTRUMENT,TIMEFRAME,TIMEZONE,DATEFROM,DATETO,ROWS,DATATYPE,DECIMALS,SOURCE,BROKER_ID FROM DATA ORDER BY ID",
        "instruments": "SELECT INSTRUMENT,POINTVALUE,TICKSIZE,TICKSTEP,DEFAULTSPREAD,DEFAULTSLIPPAGE,COMMISSIONS,DATATYPE,BROKER_ID FROM INSTRUMENTS ORDER BY INSTRUMENT",
        "brokers": "SELECT ID,NAME,MT_USE,MT_TIMEZONE,POSTFIX FROM BROKER ORDER BY ID",
        "sessions": "SELECT SESSION,BROKER_ID FROM SESSIONS ORDER BY SESSION",
        "periods": "SELECT SESSION,DAYFROM,TIMEFROM,DAYTO,TIMETO,EOD FROM ELEMENTS ORDER BY ID",
    }
    tables = {}
    try:
        # URI read-only + one read transaction: include WAL-visible native rows, never copy/edit SQLite.
        with closing(sqlite3.connect(path.as_uri() + "?mode=ro", uri=True, timeout=2)) as connection:
            connection.row_factory = sqlite3.Row
            connection.execute("PRAGMA query_only=ON")
            connection.execute("BEGIN")
            for key, query in queries.items():
                limit = 16384 if key == "periods" else 4096
                rows = [dict(row) for row in connection.execute(query + f" LIMIT {limit + 1}")]
                if len(rows) > limit:
                    raise DataSetupError("native_data_catalog_limit", "Native data metadata exceeds the bounded catalog size")
                tables[key] = rows
    except sqlite3.Error as exc:
        raise DataSetupError("native_data_catalog_unavailable", "Native data catalog is unreadable or has an unsupported schema") from exc
    try:
        digest = sha256(json.dumps(tables, sort_keys=True, separators=(",", ":"), allow_nan=False).encode()).hexdigest()
    except (ValueError, TypeError) as exc:
        raise DataSetupError("native_data_catalog_invalid", "Native data metadata contains invalid values") from exc
    instruments = {row["INSTRUMENT"]: row for row in tables["instruments"]}
    brokers = {row["ID"]: row for row in tables["brokers"]}
    if len(instruments) != len(tables["instruments"]) or len(brokers) != len(tables["brokers"]):
        raise DataSetupError("native_data_catalog_invalid", "Native profile identities are ambiguous")
    # Native session definitions are options, not evidence of a dataset's active session.
    periods_by_session = {}
    for period in tables["periods"]:
        periods_by_session.setdefault(period["SESSION"], []).append({
            "day_from": period["DAYFROM"], "time_from": period["TIMEFROM"],
            "day_to": period["DAYTO"], "time_to": period["TIMETO"], "end_of_day": bool(period["EOD"])})
    session_definitions = [{"name": session["SESSION"], "broker_id": session["BROKER_ID"],
        "periods": periods_by_session.get(session["SESSION"], [])} for session in tables["sessions"]]
    sessions_by_broker = {}
    for session in session_definitions:
        sessions_by_broker.setdefault(session["broker_id"], []).append(session["name"])
    datasets = []
    for row in tables["data"]:
        if type(row["ID"]) is not int or row["ID"] < 1 or not _text(row["SYMBOL"]):
            raise DataSetupError("native_data_catalog_invalid", "Native dataset identity is invalid")
        # Retain existing installed-data normalization, then extend its limited metadata projection.
        normalized = _installed_rows([{ "symbol": row["SYMBOL"], "timeframe": row["TIMEFRAME"],
            "dateFrom": row["DATEFROM"], "dateTo": row["DATETO"], "rows": row["ROWS"] }])
        instrument = instruments.get(row["INSTRUMENT"], {})
        broker = brokers.get(row["BROKER_ID"])
        sessions = sessions_by_broker.get(row["BROKER_ID"], [])
        datasets.append({**(normalized[0] if normalized else {"symbol": row["SYMBOL"]}),
            "dataset_id": f"sqx-data-{row['ID']}", "native_source_id": row["SOURCE"],
            "source_dataset_id": f"sqx-data-{row['SOURCEDATA_ID']}" if row["SOURCEDATA_ID"] else None,
            "timeframe": _text(row["TIMEFRAME"]), "timezone": _text(row["TIMEZONE"]),
            # Installed DataManager reads DATA.DATATYPE into DataInfo.barTimeType; it is not asset type.
            "bar_timestamp_convention": {1: "start_of_bar", 2: "end_of_bar"}.get(row["DATATYPE"]),
            "instrument": {"name": _text(row["INSTRUMENT"]),
                **{out: _number(instrument.get(key)) for out, key in (("point_value", "POINTVALUE"),
                    ("tick_size", "TICKSIZE"), ("tick_step", "TICKSTEP"), ("default_spread", "DEFAULTSPREAD"),
                    ("default_slippage", "DEFAULTSLIPPAGE"))}, "commissions": _text(instrument.get("COMMISSIONS"))},
            "broker": {"profile_id": f"sqx-broker-{broker['ID']}", "name": _text(broker["NAME"]),
                "timezone": _text(broker["MT_TIMEZONE"])} if broker else None, "sessions": sessions})
    component = _inside(home, "internal/plugins/DataSourceMt5Api/DataSourceMt5Api.jar")
    return {"schema": "tc.data-setup.v1", "status": "available", "source_build": SQX_BUILD,
        "source": {"kind": "native_sqlite", "relative_path": "user/data/data.db", "snapshot_sha256": digest},
        "datasets": datasets, "session_definitions": session_definitions, "native_mt5_import": {"native_component_present": component.is_file(),
            "product_import_wired": False, "official_url": _MT5_URL}, "reason_code": None}


def read_native_data_setup(sqx_home) -> dict:
    try:
        return _native_catalog(sqx_home)
    except DataSetupError as exc:
        return {"schema": "tc.data-setup.v1", "status": "unavailable", "source_build": SQX_BUILD,
            "source": None, "datasets": [], "session_definitions": [], "native_mt5_import": {"native_component_present": False,
                "product_import_wired": False, "official_url": _MT5_URL}, "reason_code": exc.code, "detail": exc.detail}


def select_native_data_setup(sqx_home, payload: dict) -> dict:
    if (not isinstance(payload, dict) or set(payload) != {"dataset_id", "snapshot_sha256"}
            or not isinstance(payload["dataset_id"], str) or not re.fullmatch(r"sqx-data-[1-9][0-9]*", payload["dataset_id"])
            or not isinstance(payload["snapshot_sha256"], str) or not re.fullmatch(r"[0-9a-f]{64}", payload["snapshot_sha256"])):
        raise DataSetupError("data_setup_selection_invalid", "Choose one exact dataset from the native catalog")
    catalog = _native_catalog(sqx_home)
    if catalog["source"]["snapshot_sha256"] != payload["snapshot_sha256"]:
        raise DataSetupError("native_data_catalog_changed", "Native data metadata changed; refresh the selected dataset")
    dataset = next((item for item in catalog["datasets"] if item["dataset_id"] == payload["dataset_id"]), None)
    if dataset is None:
        raise DataSetupError("native_dataset_missing", "The selected native dataset is no longer available")
    fields = {}
    for field in ("symbol", "timeframe", "timezone", "bar_timestamp_convention"):
        fields[field] = {"value": dataset[field], "source": "native_dataset"}
    for field, value in dataset["instrument"].items():
        fields["instrument_name" if field == "name" else field] = {"value": value, "source": "native_instrument"}
    fields["broker_timezone"] = {"value": (dataset["broker"] or {}).get("timezone"), "source": "native_broker_profile"}
    unresolved, conflicts = [], []
    for key, item in fields.items():
        item["state"] = "observed_native" if item["value"] is not None else "unresolved"
        if item["value"] is None:
            unresolved.append({"field": key, "reason_code": "native_metadata_missing"})
    zone, broker_zone = dataset["timezone"], fields["broker_timezone"]["value"]
    if zone and broker_zone and zone != broker_zone:
        fields["timezone"]["state"] = "conflict"
        conflicts.append({"field": "timezone", "reason_code": "dataset_broker_timezone_difference",
            "dataset_value": zone, "broker_value": broker_zone})
    for key in ("point_value", "tick_size", "tick_step"):
        if fields[key]["value"] is not None and fields[key]["value"] <= 0:
            fields[key]["state"] = "conflict"
            conflicts.append({"field": key, "reason_code": "native_instrument_value_invalid",
                "dataset_value": fields[key]["value"], "broker_value": None})
    return {"schema": "tc.data-setup-selection.v1", "dataset": dataset, "source": catalog["source"],
        "fields": fields, "unresolved": unresolved, "conflicts": conflicts,
        "status": "needs_review" if unresolved or conflicts else "resolved",
        "native_import_performed": False, "backtest_ready": False}


_COLUMN_ALIASES = {"date": "date", "time": "time", "timestamp": "timestamp", "datetime": "timestamp",
    "open": "open", "high": "high", "low": "low", "close": "close", "volume": "volume",
    "up": "up", "down": "down", "tickvol": "tick_volume", "tickvolume": "tick_volume", "vol": "volume", "spread": "spread"}


def inspect_csv_data(raw: bytes) -> dict:
    """Inspect file structure; native profile selection never supplies missing file provenance."""
    if not isinstance(raw, bytes) or not raw or len(raw) > MAX_DATA_FILE_BYTES:
        raise DataSetupError("data_file_size_invalid", "Choose a nonempty data file up to 16 MiB")
    try:
        encoding = "utf-16" if raw.startswith((b"\xff\xfe", b"\xfe\xff")) else "utf-8-sig"
        text = raw.decode(encoding)
    except UnicodeError as exc:
        raise DataSetupError("data_file_encoding_invalid", "Data must be UTF-8 or BOM-marked UTF-16 text") from exc
    if "\0" in text:
        raise DataSetupError("data_file_encoding_invalid", "Data contains unsupported binary characters")
    try:
        header_line = text.splitlines()[0]
        delimiter = csv.Sniffer().sniff(header_line, delimiters=",;\t|").delimiter
        reader = csv.reader(StringIO(text), delimiter=delimiter, strict=True)
        header = next(reader)
        columns = dict.fromkeys(("date", "time", "timestamp", "open", "high", "low", "close", "volume", "up", "down", "tick_volume", "spread"))
        indexes = {}
        for index, name in enumerate(header):
            key = _COLUMN_ALIASES.get(re.sub(r"[<> _]", "", name.strip().lower()))
            if key:
                if key in indexes:
                    raise DataSetupError("data_file_columns_invalid", "More than one column maps to the same field")
                indexes[key], columns[key] = index, name
        if "timestamp" in indexes and ("date" in indexes or "time" in indexes):
            raise DataSetupError("data_file_columns_invalid", "Competing timestamp and date/time columns need one explicit clock representation")
        if "date" not in indexes and "timestamp" not in indexes and "time" in indexes:
            indexes["timestamp"] = indexes.pop("time")
            columns["timestamp"], columns["time"] = columns["time"], None
        if not all(key in indexes for key in ("open", "high", "low", "close")) or not ("date" in indexes or "timestamp" in indexes):
            raise DataSetupError("data_file_columns_invalid", "Data needs named timestamp/date and Open, High, Low, Close columns")
        slash_formats = {"%m/%d/%Y", "%d/%m/%Y"}
        row_count = 0
        for values in reader:
            if not values or not any(value.strip() for value in values):
                continue
            row_count += 1
            if row_count > MAX_DATA_FILE_ROWS:
                raise DataSetupError("data_file_row_limit", "Data inspection supports up to 500,000 rows")
            if len(values) == len(header) and "date" in indexes:
                date = values[indexes["date"]].strip()
                if re.fullmatch(r"[0-9]{1,2}/[0-9]{1,2}/[0-9]{4}", date):
                    left, right, _ = map(int, date.split("/"))
                    if left > 12:
                        slash_formats.discard("%m/%d/%Y")
                    if right > 12:
                        slash_formats.discard("%d/%m/%Y")
        if not row_count:
            raise DataSetupError("data_file_empty", "Data contains no rows")
    except (csv.Error, StopIteration, IndexError) as exc:
        raise DataSetupError("data_file_format_invalid", "Data has an unreadable delimited header or malformed quoting") from exc

    issues = Counter()
    intervals = Counter()
    first = last = previous = None
    aware_states = set()
    offsets = set()
    naive_rows = numeric_rows = 0
    timestamps_seen = set()
    reader = csv.reader(StringIO(text), delimiter=delimiter, strict=True)
    next(reader)
    try:
        for values in reader:
            if not values or not any(value.strip() for value in values):
                continue
            if len(values) != len(header):
                issues["column_count_mismatch"] += 1
                continue
            try:
                prices = [Decimal(values[indexes[key]]) for key in ("open", "high", "low", "close")]
                if not all(value.is_finite() for value in prices):
                    raise ValueError("nonfinite")
                o, h, l, c = prices
                if h < max(o, l, c) or l > min(o, h, c):
                    raise ValueError("OHLC bounds")
            except (InvalidOperation, ValueError):
                issues["invalid_ohlc"] += 1
            for key in ("volume", "up", "down", "tick_volume", "spread"):
                if key in indexes:
                    try:
                        value = Decimal(values[indexes[key]])
                        if not value.is_finite() or value < 0:
                            raise ValueError("invalid volume/spread")
                    except (InvalidOperation, ValueError):
                        issues["invalid_volume_or_spread"] += 1
            try:
                if "timestamp" in indexes:
                    timestamp = values[indexes["timestamp"]].strip()
                    if re.fullmatch(r"[0-9]{10}|[0-9]{13}", timestamp):
                        numeric_rows += 1
                        at = datetime.fromtimestamp(int(timestamp) / (1000 if len(timestamp) == 13 else 1), timezone.utc)
                    else:
                        at = datetime.fromisoformat(timestamp.replace("Z", "+00:00"))
                else:
                    date = values[indexes["date"]].strip()
                    clock = values[indexes["time"]].strip() if "time" in indexes else "00:00"
                    if "/" in date:
                        if len(slash_formats) != 1:
                            issues["ambiguous_date_format"] += 1
                            continue
                        date = datetime.strptime(date, next(iter(slash_formats))).date().isoformat()
                    elif re.fullmatch(r"[0-9]{4}\.[0-9]{2}\.[0-9]{2}", date):
                        date = date.replace(".", "-")
                    at = datetime.fromisoformat(date + "T" + clock)
                aware = at.utcoffset() is not None
                aware_states.add(aware)
                if aware:
                    offsets.add(at.utcoffset().total_seconds())
                    at = at.astimezone(timezone.utc)
                else:
                    naive_rows += 1
                normalized = at.replace(tzinfo=None)
                first = normalized if first is None else min(first, normalized)
                last = normalized if last is None else max(last, normalized)
                timestamp_key = (aware, normalized)
                if timestamp_key in timestamps_seen:
                    issues["duplicate_timestamps"] += 1
                timestamps_seen.add(timestamp_key)
                if previous is not None:
                    delta = (normalized - previous).total_seconds()
                    if delta < 0:
                        issues["out_of_order_timestamps"] += 1
                    elif delta > 0:
                        intervals[delta] += 1
                previous = normalized
            except (ValueError, OverflowError, OSError):
                issues["invalid_timestamp"] += 1
    except csv.Error as exc:
        raise DataSetupError("data_file_format_invalid", "Data contains malformed quoting") from exc
    if numeric_rows:
        issues["numeric_timestamp_unit_unconfirmed"] = numeric_rows
    if naive_rows:
        issues["timestamp_timezone_unresolved"] = naive_rows
    if len(aware_states) > 1:
        issues["mixed_timestamp_timezones"] = row_count
        first = last = None
    if len(offsets) > 1:
        issues["explicit_timezone_offset_changes"] = len(offsets)
    interval = None
    if intervals and len(aware_states) == 1:
        candidate, count = intervals.most_common(1)[0]
        if count / sum(intervals.values()) >= 0.8:
            interval = candidate
        variable = sum(value for delta, value in intervals.items() if delta != candidate)
        if variable:
            issues["gaps_or_variable_intervals"] = variable
    if interval is None:
        issues["interval_unresolved"] = 1
    details = {
        "numeric_timestamp_unit_unconfirmed": "Numeric time values were provisionally interpreted as epoch seconds/milliseconds; the header does not declare units or a clock reference.",
        "timestamp_timezone_unresolved": "Naive timestamps contain no timezone; a selected native dataset is only a reference.",
        "ambiguous_date_format": "Day/month order is ambiguous or inconsistent; it was not guessed.",
        "gaps_or_variable_intervals": "Observed spacing varies; sessions, missing bars or DST may explain gaps. Interval is a suggestion only.",
        "explicit_timezone_offset_changes": "Explicit offsets change within the file; displayed instants are normalized to UTC.",
        "mixed_timestamp_timezones": "The file mixes explicit and missing timezone information.",
        "interval_unresolved": "No consistent interval can be suggested from these timestamps.",
    }
    explicit = aware_states == {True} and not numeric_rows
    def stamp(value):
        return value.isoformat() + ("Z" if explicit else "") if value is not None else None
    return {"schema": "tc.data-file-inspection.v1", "source_sha256": sha256(raw).hexdigest(), "bytes": len(raw),
        "columns": columns, "row_count": row_count, "date_from": stamp(first), "date_to": stamp(last),
        "timestamp_timezone": "UTC" if explicit else None, "observed_interval_seconds": interval,
        "issues": [{"code": code, "count": count, "detail": details.get(code, code.replace("_", " "))} for code, count in sorted(issues.items())],
        "status": "needs_review" if issues else "inspected", "native_import_performed": False, "backtest_ready": False}
