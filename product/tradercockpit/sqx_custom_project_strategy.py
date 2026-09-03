"""Inspect one native Custom Project databank ``.sqx`` for Automation Results.

Trades and equity come from producer ``orders.bin``. Strategy config is the
archive ``settings.xml`` compared with the current task XML. This module does
not run a backtester or invent Net Profit.
"""

from __future__ import annotations

from xml.etree import ElementTree
from zipfile import ZipFile, BadZipFile
from io import BytesIO
from pathlib import Path

from .research_verdicts import equity_points
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


SQX_CUSTOM_PROJECT_STRATEGY_SCHEMA = "tc.sqx-custom-project-strategy.v1"
SQX_CUSTOM_PROJECT_STRATEGY_API_PATH = "/api/sqx-project-strategy"
_CHART_ENTRY_HINTS = ("chart", "bars", "candle", "ohlc")
_RESERVED_ARCHIVE_ENTRIES = frozenset(
    {"settings.xml", "strategy_Portfolio.xml", "version.txt", "orders.bin"}
)


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


def inspect_custom_project_strategy(
    sqx_home: Path | str | None,
    project: str,
    databank: str,
    archive: str,
    *,
    task: int | None = None,
) -> dict[str, object]:
    """Return trades, equity, and strategy config from one inspectable databank archive."""

    home = _verified_home(sqx_home)
    read_sqx_custom_project_topology(home, project)
    path = _resolved_strategy_archive(home, project, databank, archive)
    snapshot = path.read_bytes()
    relative = f"user/projects/{project}/databanks/{_databank_name(databank)}/{path.name}"
    try:
        identity = inspect_sqx_output_bytes(snapshot, archive_name=path.name)
    except SqxOutputError as exc:
        raise SqxCustomProjectTopologyError(exc.code, exc.detail) from exc

    topology = custom_project_topology_record(home, project)
    selected = _selected_task(topology, task)
    task_settings = list(selected.get("settings") or []) if selected else []

    archive_settings: list[dict[str, object]] = []
    settings_root: ElementTree.Element | None = None
    try:
        with ZipFile(BytesIO(snapshot)) as handle:
            settings_bytes = handle.read("settings.xml")
        settings_root = ElementTree.fromstring(settings_bytes)
        archive_settings = list(settings_sections(settings_root))
    except (BadZipFile, KeyError, ElementTree.ParseError):
        archive_settings = []
        settings_root = None

    orders_state: dict[str, object]
    trades: list[dict[str, object]] = []
    try:
        orders = inspect_sqx_orders_bytes(snapshot)
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

    equity: list[dict[str, float | int]] = []
    equity_basis = None
    if orders_state["state"] == "available":
        if capital is None:
            equity_basis = "cumulative_pl"
            equity = equity_points(trades, initial_capital=0.0)
        else:
            equity_basis = "archive_initial_capital"
            equity = equity_points(trades, initial_capital=capital)

    entries = list(identity.get("archive_entries") or [])
    stored_chart_entries = _chart_entries(entries)
    store_flag = _store_chart_data(settings_root)
    chart_stored = bool(stored_chart_entries)
    if chart_stored:
        chart = {
            "stored": True,
            "entries": stored_chart_entries,
            "store_chart_data": store_flag,
            "reason_code": None,
            "detail": None,
        }
    else:
        chart = {
            "stored": False,
            "entries": [],
            "store_chart_data": store_flag,
            "reason_code": "chart_data_not_stored",
            "detail": (
                "This archive did not store chart data. Trades on chart stay unavailable "
                "until Trading options Store chart data is on and the producer writes chart members."
            ),
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
        "archive_entries": entries,
        "task_index": selected.get("native_task_index") if selected else None,
        "orders": orders_state,
        "equity": equity,
        "equity_basis": equity_basis,
        "initial_capital": capital,
        "settings": archive_settings,
        "config_diff": _config_diff(task_settings, archive_settings),
        "chart": chart,
        "detail": (
            "List of trades and equity are producer orders.bin records from this databank archive. "
            "This desktop does not invent Net Profit."
        ),
    }
