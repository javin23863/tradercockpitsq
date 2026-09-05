"""Inspect one native Custom Project databank ``.sqx`` for Automation Results.

Trades and equity come from producer ``orders.bin``. Strategy config is the
archive ``settings.xml`` compared with the current task XML. This module does
not run a backtester or invent Net Profit.
"""

from __future__ import annotations

from csv import reader as csv_reader
from datetime import datetime, timezone
from xml.etree import ElementTree
from zipfile import ZipFile, BadZipFile
from io import BytesIO, StringIO
from pathlib import Path

from .research_verdicts import (
    SAMPLE_FULL,
    SAMPLE_IN_SAMPLE,
    SAMPLE_OUT_OF_SAMPLE,
    equity_points,
    parse_chart_history_range,
    period_profits,
    round2,
    select_sample,
    sqx_statistics,
)
from .sqx_custom_project import (
    SQX_BUILD,
    SqxCustomProjectTopologyError,
    _databank_name,
    _local_name,
    _project_databanks_root,
    _verified_home,
    custom_project_topology_record,
    read_sqx_custom_project_topology,
    settings_sections,
    xml_node,
)
from .sqx_orders import SqxOrdersError, inspect_sqx_orders_bytes
from .sqx_outputs import SqxOutputError, inspect_sqx_output_bytes
from .sqx_results_plugins import list_results_plugin_tabs, results_plugin_create_state
from .results_analytics import analytics, FILTERS


SQX_CUSTOM_PROJECT_STRATEGY_SCHEMA = "tc.sqx-custom-project-strategy.v1"
SQX_CUSTOM_PROJECT_STRATEGY_API_PATH = "/api/sqx-project-strategy"
_CHART_ENTRY_HINTS = ("chart", "bars", "candle", "ohlc")
_RESERVED_ARCHIVE_ENTRIES = frozenset(
    {"settings.xml", "strategy_Portfolio.xml", "version.txt", "orders.bin"}
)
_LONG_ORDER_TYPES = frozenset({1, 9})
_SHORT_ORDER_TYPES = frozenset({2, 11})
_STATS_FIELDS = (
    "NumberOfTrades",
    "NumberOfProfits",
    "NumberOfLosses",
    "NetProfit",
    "GrossProfit",
    "GrossLoss",
    "WinningPct",
    "ProfitFactor",
    "Drawdown",
    "DrawdownPct",
    "ReturnDDRatio",
    "Expectancy",
    "AvgTradesPerMonth",
    "MaxConsecLosses",
    "final_equity",
    "months_basis",
    "AvgWin",
    "AvgLoss",
    "PayoutRatio",
    "MaxConsecLoss",
    "PctDrawdown",
)
_PROFILE_POINT_LIMIT = 400
_MAX_CHART_BARS = 500
_SIDECAR_SUFFIXES = (".txt", ".csv")


def _archive_filename(value: str) -> str:
    if (
        not isinstance(value, str)
        or not value
        or value != value.strip()
        or "/" in value
        or "\\" in value
        or "\0" in value
        or value in {".", ".."}
        or Path(value).name != value
        or not value.lower().endswith(".sqx")
    ):
        raise SqxCustomProjectTopologyError(
            "custom_project_archive_name_invalid",
            "SQX strategy archive name must be one exact .sqx filename",
        )
    return value


def _resolved_strategy_archive(home: Path, project: str, databank: str, archive: str) -> Path:
    bank = _databank_name(databank)
    name = _archive_filename(archive)
    root = _project_databanks_root(home, project)
    candidate = root / bank / name
    try:
        resolved = candidate.resolve()
        resolved.relative_to(home.resolve())
    except (OSError, RuntimeError, ValueError) as exc:
        raise SqxCustomProjectTopologyError(
            "custom_project_path_escape",
            "SQX strategy archive resolves outside the verified runtime",
        ) from exc
    if resolved.parent.parent != root or resolved.parent.name != bank or resolved.name != name:
        raise SqxCustomProjectTopologyError(
            "custom_project_path_escape",
            "SQX strategy archive resolves outside the exact project databank",
        )
    if not resolved.is_file():
        raise SqxCustomProjectTopologyError(
            "custom_project_strategy_missing",
            f"SQX strategy archive is missing: {name}",
        )
    return resolved


def _flatten_fields(node: dict[str, object]) -> list[dict[str, object]]:
    fields: list[dict[str, object]] = []
    path = list(node.get("path") or [])
    attributes = node.get("attributes") if isinstance(node.get("attributes"), dict) else {}
    for key, value in attributes.items():
        fields.append({"path": path, "attribute": str(key), "value": str(value)})
    text = node.get("text")
    if isinstance(text, str) and text:
        fields.append({"path": path, "text": text, "value": text})
    for child in node.get("children") or []:
        if isinstance(child, dict):
            fields.extend(_flatten_fields(child))
    return fields


def _field_key(item: dict[str, object]) -> tuple[object, ...]:
    path = tuple(item.get("path") or [])
    if "attribute" in item:
        return (*path, "attribute", item["attribute"])
    return (*path, "text")


def _config_diff(task_settings: list[dict[str, object]], archive_settings: list[dict[str, object]]) -> list[dict[str, object]]:
    task_fields = { _field_key(item): item for node in task_settings for item in _flatten_fields(node) }
    archive_fields = { _field_key(item): item for node in archive_settings for item in _flatten_fields(node) }
    diff: list[dict[str, object]] = []
    for key, task_item in task_fields.items():
        archive_item = archive_fields.get(key)
        if archive_item is None:
            continue
        task_value = str(task_item.get("value") or "")
        archive_value = str(archive_item.get("value") or "")
        if task_value == archive_value:
            continue
        record = {
            "path": list(task_item["path"]),
            "task_value": task_value,
            "archive_value": archive_value,
        }
        if "attribute" in task_item:
            record["attribute"] = task_item["attribute"]
        else:
            record["text"] = True
        diff.append(record)
    return diff


def _initial_capital(root: ElementTree.Element | None) -> float | None:
    if root is None:
        return None
    for element in root.iter():
        if _local_name(element.tag) != "InitialCapital":
            continue
        text = (element.text or "").strip()
        if not text:
            continue
        try:
            return float(text)
        except ValueError:
            continue
    return None


def _store_chart_data(root: ElementTree.Element | None) -> bool | None:
    if root is None:
        return None
    for element in root.iter():
        if _local_name(element.tag) != "Param":
            continue
        if element.attrib.get("key") != "StoreChartData":
            continue
        text = (element.text or "").strip().lower()
        if text == "true":
            return True
        if text == "false":
            return False
    return None


def _chart_entries(entries: list[str]) -> list[str]:
    found: list[str] = []
    for name in entries:
        if name in _RESERVED_ARCHIVE_ENTRIES:
            continue
        lower = name.lower()
        if any(hint in lower for hint in _CHART_ENTRY_HINTS):
            found.append(name)
    return found


def _selected_task(record: dict[str, object], task: int | None) -> dict[str, object] | None:
    tasks = [item for item in record.get("tasks") or [] if isinstance(item, dict)]
    if task is not None:
        for item in tasks:
            if item.get("native_task_index") == task:
                return item
        raise SqxCustomProjectTopologyError(
            "custom_project_task_missing",
            "Exact native task index is not in this saved Custom Project",
        )
    for item in tasks:
        if item.get("kind") == "Build" and item.get("settings"):
            return item
    for item in tasks:
        if item.get("settings"):
            return item
    return tasks[0] if tasks else None


def _public_stats(stats: dict[str, object]) -> dict[str, object]:
    out = {key: stats[key] for key in _STATS_FIELDS if key in stats}
    profits = int(stats.get("NumberOfProfits") or 0)
    losses = int(stats.get("NumberOfLosses") or 0)
    gross_profit = float(stats.get("GrossProfit") or 0)
    gross_loss = float(stats.get("GrossLoss") or 0)
    avg_win = round2(gross_profit / profits) if profits else 0.0
    avg_loss = round2(gross_loss / losses) if losses else 0.0
    out["AvgWin"] = avg_win
    out["AvgLoss"] = avg_loss
    out["PayoutRatio"] = round2(avg_win / avg_loss) if avg_loss else 0.0
    out["MaxConsecLoss"] = stats.get("MaxConsecLosses")
    out["PctDrawdown"] = stats.get("DrawdownPct")
    return out


def _direction_trades(trades: list[dict[str, object]], types: frozenset[int] | None) -> list[dict[str, object]]:
    if types is None:
        return list(trades)
    return [trade for trade in trades if int(trade["Type"]) in types]


def _stats_sides(trades: list[dict[str, object]], *, initial_capital: float, chart_history: dict[str, object] | None) -> dict[str, object | None]:
    long_trades = _direction_trades(trades, _LONG_ORDER_TYPES)
    short_trades = _direction_trades(trades, _SHORT_ORDER_TYPES)
    return {
        "all": _public_stats(sqx_statistics(trades, initial_capital=initial_capital, chart_history=chart_history)) if trades else None,
        "long": _public_stats(sqx_statistics(long_trades, initial_capital=initial_capital, chart_history=chart_history)) if long_trades else None,
        "short": _public_stats(sqx_statistics(short_trades, initial_capital=initial_capital, chart_history=chart_history)) if short_trades else None,
    }


def _statistics_record(trades: list[dict[str, object]], *, initial_capital: float, chart_history: dict[str, object] | None) -> dict[str, object]:
    return {
        "basis": "sqx_column_formulas_over_orders.bin",
        "full": _stats_sides(select_sample(trades, SAMPLE_FULL), initial_capital=initial_capital, chart_history=chart_history),
        "is": _stats_sides(select_sample(trades, SAMPLE_IN_SAMPLE), initial_capital=initial_capital, chart_history=chart_history),
        "oos": _stats_sides(select_sample(trades, SAMPLE_OUT_OF_SAMPLE), initial_capital=initial_capital, chart_history=chart_history),
    }


def _fitnesses(root: ElementTree.Element | None) -> dict[str, float]:
    if root is None:
        return {}
    for element in root.iter():
        if _local_name(element.tag) != "Fitnesses":
            continue
        found: dict[str, float] = {}
        for key, raw in element.attrib.items():
            try:
                value = float(raw)
            except (TypeError, ValueError):
                continue
            found[str(key)] = value
        return found
    return {}


def _result_identity(root: ElementTree.Element | None) -> tuple[str, str]:
    if root is None:
        return "", ""
    name = root.attrib.get("ResultName") if _local_name(root.tag) == "ResultsGroup" else ""
    key = ""
    for element in root.iter():
        if _local_name(element.tag) != "Result":
            continue
        key = str(element.attrib.get("resultKey") or "")
        if key:
            break
    return str(name or ""), key


def _source_record(snapshot: bytes) -> dict[str, object]:
    try:
        with ZipFile(BytesIO(snapshot)) as handle:
            payload = handle.read("strategy_Portfolio.xml")
        text = payload.decode("utf-8-sig")
    except (BadZipFile, KeyError, UnicodeDecodeError):
        return {
            "state": "unavailable",
            "reason_code": "strategy_xml_unreadable",
            "detail": "Archive strategy_Portfolio.xml could not be read as UTF-8.",
            "member": "strategy_Portfolio.xml",
            "language": "Strategy XML",
            "text": "",
        }
    return {
        "state": "available",
        "reason_code": None,
            "detail": "Native strategy XML from strategy_Portfolio.xml. EasyLanguage / MQL / Pseudo Code print through the running StrategyQuant X local web.",
        "member": "strategy_Portfolio.xml",
        "language": "Strategy XML",
        "text": text,
    }


def _symbol_rows(trades: list[dict[str, object]], *, initial_capital: float, chart_history: dict[str, object] | None) -> list[dict[str, object]]:
    grouped: dict[str, list[dict[str, object]]] = {}
    for trade in trades:
        symbol = str(trade.get("Symbol") or "—")
        grouped.setdefault(symbol, []).append(trade)
    rows: list[dict[str, object]] = []
    for symbol in sorted(grouped):
        stats = _public_stats(sqx_statistics(grouped[symbol], initial_capital=initial_capital, chart_history=chart_history))
        rows.append({"symbol": symbol, **stats})
    return rows


def _trade_analysis(trades: list[dict[str, object]]) -> dict[str, object]:
    yearly = period_profits(trades, "year")
    years = [{"period": period, "net_profit": round(value, 2)} for period, value in yearly.items()]
    mae = [float(trade["MAE"]) for trade in trades if isinstance(trade.get("MAE"), (int, float))]
    mfe = [float(trade["MFE"]) for trade in trades if isinstance(trade.get("MFE"), (int, float))]
    return {
        "period_by": "close_time",
        "years": years,
        "mae_avg": round(sum(mae) / len(mae), 2) if mae else None,
        "mfe_avg": round(sum(mfe) / len(mfe), 2) if mfe else None,
    }


def _profile_points(trades: list[dict[str, object]]) -> list[dict[str, float]]:
    points = [
        {"mae": float(trade["MAE"]), "mfe": float(trade["MFE"]), "pl": float(trade["PL"])}
        for trade in trades
        if isinstance(trade.get("MAE"), (int, float)) and isinstance(trade.get("MFE"), (int, float))
    ]
    if len(points) <= _PROFILE_POINT_LIMIT:
        return points
    step = len(points) / _PROFILE_POINT_LIMIT
    sampled = [points[int(index * step)] for index in range(_PROFILE_POINT_LIMIT - 1)]
    sampled.append(points[-1])
    return sampled


def _csv_time_ms(date_text: str, time_text: str) -> int | None:
    raw = f"{date_text.strip()} {time_text.strip()}"
    parsed = None
    for fmt in ("%m/%d/%Y %H:%M:%S", "%m/%d/%Y %H:%M"):
        try:
            parsed = datetime.strptime(raw, fmt)
            break
        except ValueError:
            continue
    if parsed is None:
        return None
    return int(parsed.replace(tzinfo=timezone.utc).timestamp() * 1000)


def _parse_tradestation_bars(text: str) -> list[dict[str, object]]:
    rows = csv_reader(StringIO(text))
    header = next(rows, None)
    if not header:
        return []
    labels = [item.strip().strip('"') for item in header]
    if len(labels) < 6 or labels[0] != "Date" or labels[1] != "Time":
        return []
    bars: list[dict[str, object]] = []
    for row in rows:
        if len(row) < 6:
            continue
        open_ms = _csv_time_ms(row[0], row[1])
        if open_ms is None:
            continue
        try:
            open_px = float(row[2])
            high_px = float(row[3])
            low_px = float(row[4])
            close_px = float(row[5])
        except (TypeError, ValueError):
            continue
        volume = None
        if len(row) >= 8:
            try:
                volume = float(row[6]) + float(row[7])
            except (TypeError, ValueError):
                volume = None
        open_time = datetime.fromtimestamp(open_ms / 1000, tz=timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
        bar = {
            "open_time": open_time,
            "open": open_px,
            "high": high_px,
            "low": low_px,
            "close": close_px,
            "time_ms": open_ms,
        }
        if volume is not None:
            bar["volume"] = volume
        bars.append(bar)
    return bars


def _sidecar_bar_path(archive_path: Path) -> Path | None:
    stem = archive_path.with_suffix("")
    for suffix in _SIDECAR_SUFFIXES:
        candidate = Path(str(stem) + suffix)
        if candidate.is_file() and not candidate.is_symlink():
            return candidate
    return None


def _timeframe_from_bars(bars: list[dict[str, object]]) -> str:
    if len(bars) < 2:
        return "H1"
    delta = int(bars[1]["time_ms"]) - int(bars[0]["time_ms"])
    mapping = {
        60_000: "M1",
        300_000: "M5",
        900_000: "M15",
        1_800_000: "M30",
        3_600_000: "H1",
        14_400_000: "H4",
        86_400_000: "D1",
    }
    return mapping.get(delta, "H1")


def _focus_trade_ms(trades: list[dict[str, object]], focus_ticket: int | None) -> int | None:
    chosen: dict[str, object] | None = None
    if focus_ticket is not None:
        for trade in trades:
            if trade.get("Ticket") == focus_ticket:
                chosen = trade
                break
    if chosen is None and trades:
        chosen = max(
            trades,
            key=lambda trade: int(trade.get("CloseTime") or trade.get("OpenTime") or 0),
        )
    if chosen is None:
        return None
    try:
        stamp = int(chosen.get("CloseTime") or chosen.get("OpenTime") or 0)
    except (TypeError, ValueError):
        return None
    return stamp or None


def _window_sidecar_bars(
    parsed: list[dict[str, object]],
    *,
    focus_ms: int | None,
    limit: int,
) -> list[dict[str, object]]:
    if len(parsed) <= limit:
        return parsed
    if focus_ms is None:
        return parsed[-limit:]
    idx = 0
    for index, bar in enumerate(parsed):
        try:
            stamp = int(bar["time_ms"])
        except (KeyError, TypeError, ValueError):
            continue
        if stamp <= focus_ms:
            idx = index
        else:
            break
    half = limit // 2
    start = max(0, idx - half)
    end = min(len(parsed), start + limit)
    start = max(0, end - limit)
    return parsed[start:end]


def _chart_bars_record(
    archive_path: Path,
    home: Path,
    trades: list[dict[str, object]],
    *,
    stored: bool,
    store_flag: bool | None,
    focus_ticket: int | None = None,
) -> dict[str, object]:
    if stored:
        return {
            "state": "unavailable",
            "reason_code": None,
            "detail": None,
            "basis": None,
            "symbol": "",
            "timeframe": "",
            "bars": [],
            "source_count": 0,
        }
    sidecar = _sidecar_bar_path(archive_path)
    if sidecar is None:
        return {
            "state": "unavailable",
            "reason_code": "chart_data_not_stored",
            "detail": (
                "This archive did not store chart data and there is no Tradestation "
                "OHLCV sidecar next to the .sqx. Trades on chart stay unavailable "
                "until Store chart data is on or a Date,Time,Open,High,Low,Close sidecar is present."
            ),
            "basis": None,
            "symbol": "",
            "timeframe": "",
            "bars": [],
            "source_count": 0,
        }
    try:
        text = sidecar.read_text(encoding="utf-8-sig")
    except OSError:
        return {
            "state": "unavailable",
            "reason_code": "chart_sidecar_unreadable",
            "detail": "The Tradestation sidecar next to this archive could not be read.",
            "basis": None,
            "symbol": "",
            "timeframe": "",
            "bars": [],
            "source_count": 0,
        }
    parsed = _parse_tradestation_bars(text)
    if not parsed:
        return {
            "state": "unavailable",
            "reason_code": "chart_sidecar_invalid",
            "detail": "The sidecar is not Tradestation Date,Time,Open,High,Low,Close OHLCV.",
            "basis": None,
            "symbol": "",
            "timeframe": "",
            "bars": [],
            "source_count": 0,
        }
    source_count = len(parsed)
    if trades:
        first_ms = min(int(trade["OpenTime"]) for trade in trades)
        last_ms = max(int(trade["CloseTime"]) for trade in trades)
        window = [bar for bar in parsed if first_ms <= int(bar["time_ms"]) <= last_ms]
        if window:
            parsed = window
    parsed = _window_sidecar_bars(parsed, focus_ms=_focus_trade_ms(trades, focus_ticket), limit=_MAX_CHART_BARS)
    symbol = ""
    if trades:
        symbol = str(trades[0].get("Symbol") or "")
    try:
        relative = sidecar.resolve().relative_to(home.resolve()).as_posix()
    except ValueError:
        relative = sidecar.name
    timeframe = _timeframe_from_bars(parsed)
    public_bars = [{key: value for key, value in bar.items() if key != "time_ms"} for bar in parsed]
    return {
        "state": "available",
        "reason_code": None,
        "detail": (
            f"{len(public_bars)} {timeframe} bars from sidecar {sidecar.name} "
            f"({source_count} source rows). Native fills overlay only when their timestamp lands on a bar."
        ),
        "basis": "databank_sidecar_tradestation_csv",
        "symbol": symbol,
        "timeframe": timeframe,
        "relative_path": relative,
        "bars": public_bars,
        "source_count": source_count,
        "store_chart_data": store_flag,
    }


def inspect_custom_project_strategy(
    sqx_home: Path | str | None,
    project: str,
    databank: str,
    archive: str,
    *,
    task: int | None = None,
    focus_ticket: int | None = None,
    sample: str = "full",
    direction: str = "both",
    period_by: str = "close_time",
) -> dict[str, object]:
    """Return trades, equity, and strategy config from one inspectable databank archive."""

    for key, value in (("sample", sample), ("direction", direction), ("period_by", period_by)):
        if value not in FILTERS[key]:
            raise SqxCustomProjectTopologyError("results_filter_invalid", f"Invalid Results {key}")
    home = _verified_home(sqx_home)
    read_sqx_custom_project_topology(home, project)
    path = _resolved_strategy_archive(home, project, databank, archive)
    snapshot = path.read_bytes()
    relative = f"user/projects/{project}/databanks/{_databank_name(databank)}/{path.name}"
    try:
        identity = inspect_sqx_output_bytes(snapshot, archive_name=path.name, require_runtime_build=False)
    except SqxOutputError as exc:
        raise SqxCustomProjectTopologyError(exc.code, exc.detail) from exc

    topology = custom_project_topology_record(home, project)
    selected = _selected_task(topology, task)
    task_settings = list(selected.get("settings") or []) if selected else []

    archive_settings: list[dict[str, object]] = []
    settings_root: ElementTree.Element | None = None
    settings_bytes: bytes | None = None
    try:
        with ZipFile(BytesIO(snapshot)) as handle:
            settings_bytes = handle.read("settings.xml")
        settings_root = ElementTree.fromstring(settings_bytes)
        archive_settings = list(settings_sections(settings_root))
    except (BadZipFile, KeyError, ElementTree.ParseError):
        archive_settings = []
        settings_root = None
        settings_bytes = None

    orders_state: dict[str, object]
    trades: list[dict[str, object]] = []
    try:
        orders = inspect_sqx_orders_bytes(snapshot, require_runtime_build=False)
        trades = list(orders.get("trades") or [])
        orders_state = {"state": "available", "payload": orders}
    except SqxOrdersError as exc:
        orders_state = {
            "state": "unavailable",
            "reason_code": exc.code,
            "detail": exc.detail,
            "payload": None,
        }

    capital = _initial_capital(settings_root)
    archive_capital = capital
    if archive_capital is None:
        try:
            with ZipFile(BytesIO(snapshot)) as handle:
                archive_capital = _initial_capital(ElementTree.fromstring(handle.read("lastSettings.xml")))
        except (BadZipFile, KeyError, ElementTree.ParseError):
            pass
    if capital is None and selected is not None:
        for node in task_settings:
            for item in _flatten_fields(node):
                if item.get("path") == ["RiskMoneyManagement", "MoneyManagement", "InitialCapital"] and "text" in item:
                    try:
                        capital = float(item["value"])
                    except (TypeError, ValueError):
                        capital = None
                    break
                if item.get("path") == ["InitialCapital"] and "text" in item:
                    try:
                        capital = float(item["value"])
                    except (TypeError, ValueError):
                        capital = None
                    break

    if capital is None:
        try:
            with ZipFile(BytesIO(snapshot)) as handle:
                last_root = ElementTree.fromstring(handle.read("lastSettings.xml"))
            capital = _initial_capital(last_root)
        except (BadZipFile, KeyError, ElementTree.ParseError):
            pass

    chart_history = parse_chart_history_range(settings_bytes if settings_root is not None else None)
    stats_capital = float(capital) if capital is not None else 0.0
    statistics = None
    symbols: list[dict[str, object]] = []
    trade_analysis = None
    profile: list[dict[str, float]] = []
    if orders_state["state"] == "available":
        statistics = _statistics_record(trades, initial_capital=stats_capital, chart_history=chart_history)
        symbols = _symbol_rows(trades, initial_capital=stats_capital, chart_history=chart_history)
        trade_analysis = _trade_analysis(trades)
        profile = _profile_points(trades)

    equity: list[dict[str, float | int]] = []
    equity_basis = None
    if orders_state["state"] == "available":
        if capital is None:
            equity_basis = "cumulative_pl"
            equity = equity_points(trades, initial_capital=0.0)
        else:
            equity_basis = "archive_initial_capital"
            equity = equity_points(trades, initial_capital=capital)

    result_name, result_key = _result_identity(settings_root)
    source = _source_record(snapshot)

    entries = list(identity.get("archive_entries") or [])
    stored_chart_entries = _chart_entries(entries)
    store_flag = _store_chart_data(settings_root)
    chart_stored = bool(stored_chart_entries)
    bars = _chart_bars_record(
        path,
        home,
        trades,
        stored=chart_stored,
        store_flag=store_flag,
        focus_ticket=focus_ticket,
    )
    if chart_stored:
        chart = {
            "stored": True,
            "entries": stored_chart_entries,
            "store_chart_data": store_flag,
            "reason_code": None,
            "detail": None,
            "bars": bars,
        }
    else:
        chart = {
            "stored": False,
            "entries": [],
            "store_chart_data": store_flag,
            "reason_code": bars.get("reason_code") or "chart_data_not_stored",
            "detail": bars.get("detail"),
            "bars": bars,
        }

    return {
        "schema": SQX_CUSTOM_PROJECT_STRATEGY_SCHEMA,
        "source_build": SQX_BUILD,
        "project": project,
        "databank": _databank_name(databank),
        "archive": path.name,
        "relative_path": relative,
        "archive_sha256": identity["archive_sha256"],
        "native_version": identity["native_version"],
        "archive_format_version": identity["archive_format_version"],
        "sqx_build": identity["sqx_build"],
        "archive_entries": entries,
        "task_index": selected.get("native_task_index") if selected else None,
        "orders": orders_state,
        "equity": equity,
        "equity_basis": equity_basis,
        "initial_capital": capital,
        "settings": archive_settings,
        "config_diff": _config_diff(task_settings, archive_settings),
        "chart": chart,
        "result_name": result_name,
        "result_key": result_key,
        "timeframes": sorted({(node.text or "").strip() for node in settings_root.iter()
                               if _local_name(node.tag).lower() == "timeframe" and (node.text or "").strip()}) if settings_root is not None else [],
        "fitnesses": _fitnesses(settings_root),
        "statistics": statistics,
        "symbols": symbols,
        "trade_analysis": trade_analysis,
        "analytics": analytics(trades, archive_capital, sample=sample, direction=direction, period_by=period_by) if orders_state["state"] == "available" else None,
        "profile": profile,
        "source": source,
        "results_plugins": list_results_plugin_tabs(home),
        "results_plugin_create": results_plugin_create_state(home),
        "detail": (
            "List of trades and equity are producer orders.bin records from this databank archive. "
            "Overview numbers are the published SQX column formulas over those trades, not an invented Net Profit."
        ),
    }
