"""Official SQX What-to-build file lists and Ranking fitness types.

Calls the same local-web servlets the Electron UI uses. Does not invent
template names, fitness keys, or extra XML attributes.
"""

from __future__ import annotations

import json
from io import BytesIO
from pathlib import Path
from xml.etree import ElementTree
from zipfile import ZipFile

from .sqx_custom_project import (
    SqxCustomProjectTopologyError,
    _local_name,
    _project_relative_path,
    _read_archive_snapshot,
    _resolved_project_archive,
    _verified_home,
    read_sqx_custom_project_topology,
)
from .sqx_custom_project_settings import update_custom_project_settings
from .sqx_native_web import SqxNativeWebError, sqx_local_json
from .sqx_presets import SQX_BUILD

SQX_BUILD_TYPE_FILES_SCHEMA = "tc.sqx-build-type-files.v1"
SQX_BUILD_TYPE_TEMPLATE_SCHEMA = "tc.sqx-build-type-template.v1"
SQX_RANKING_FITNESS_SCHEMA = "tc.sqx-ranking-fitness-types.v1"
SQX_INSTALLED_DATA_SCHEMA = "tc.sqx-installed-data.v1"
SQX_COMMISSION_METHODS_SCHEMA = "tc.sqx-commission-methods.v1"
SQX_SYMBOL_DATA_SCHEMA = "tc.sqx-symbol-data.v1"
SQX_BUILD_TYPE_FILES_API_PATH = "/api/sqx-build-type-files"
SQX_BUILD_TYPE_TEMPLATE_API_PATH = "/api/sqx-build-type-template"
SQX_RANKING_FITNESS_API_PATH = "/api/sqx-ranking-fitness-types"
SQX_INSTALLED_DATA_API_PATH = "/api/sqx-installed-data"
SQX_COMMISSION_METHODS_API_PATH = "/api/sqx-commission-methods"
SQX_SYMBOL_DATA_API_PATH = "/api/sqx-symbol-data"
BUILD_TYPE_LIST_PATH = "/buildType/listFiles"
BUILD_TYPE_TEMPLATE_PATH = "/buildType/getTemplateConfig"
RANKING_FITNESS_LIST_PATH = "/fitnessMethodStrategyResult/list"
CONSTANTS_GET_ALL_PATH = "/constants/getAll"
COMMISSION_METHODS_LIST_PATH = "/constants/listCommissionMethods"
SYMBOL_DATA_PATH = "/data/getSymbolData"
_FILE_NAME_MAX = 512
_SYMBOL_NAME_MAX = 128
_INSTALLED_DATA_MAX = 512
_SESSION_MAX = 128
_PRECISION_MAX = 32
_COMMISSION_MAX = 64
_DATA_TYPE_MAX = 32
_SYMBOL_DATA_POINTS_MAX = 4096


def _named(root: ElementTree.Element | None, name: str) -> ElementTree.Element | None:
    if root is None:
        return None
    return next((child for child in root if _local_name(child.tag) == name), None)


def _file_name(value: object) -> str:
    if not isinstance(value, str):
        raise SqxNativeWebError(
            "build_type_files_invalid",
            "StrategyQuant X listFiles omitted a usable file name.",
        )
    name = value.strip()
    if not name or "\0" in name or len(name) > _FILE_NAME_MAX:
        raise SqxNativeWebError(
            "build_type_files_invalid",
            "StrategyQuant X listFiles omitted a usable file name.",
        )
    return name


def _file_names(values: object) -> list[str]:
    if values is None:
        raise SqxNativeWebError(
            "build_type_files_invalid",
            "StrategyQuant X listFiles omitted a file list.",
        )
    if not isinstance(values, list):
        raise SqxNativeWebError(
            "build_type_files_invalid",
            "StrategyQuant X listFiles did not return a file list.",
        )
    names: list[str] = []
    seen: set[str] = set()
    for item in values:
        name = _file_name(item)
        if name in seen:
            continue
        seen.add(name)
        names.append(name)
    return names


def _fitness_types(values: object) -> list[dict[str, str]]:
    if not isinstance(values, list) or not values:
        raise SqxNativeWebError(
            "ranking_fitness_invalid",
            "StrategyQuant X fitnessMethodStrategyResult/list omitted types.",
        )
    types: list[dict[str, str]] = []
    seen: set[str] = set()
    for item in values:
        if not isinstance(item, dict):
            raise SqxNativeWebError(
                "ranking_fitness_invalid",
                "StrategyQuant X fitness type is not an object.",
            )
        key = item.get("key")
        name = item.get("name")
        if not isinstance(key, str) or not key.strip() or "\0" in key or len(key) > 128:
            raise SqxNativeWebError(
                "ranking_fitness_invalid",
                "StrategyQuant X fitness type omitted key.",
            )
        if not isinstance(name, str) or not name.strip() or "\0" in name or len(name) > 256:
            raise SqxNativeWebError(
                "ranking_fitness_invalid",
                "StrategyQuant X fitness type omitted name.",
            )
        exact = key.strip()
        if exact in seen:
            continue
        seen.add(exact)
        types.append({"key": exact, "name": name.strip()})
    if not types:
        raise SqxNativeWebError(
            "ranking_fitness_invalid",
            "StrategyQuant X fitnessMethodStrategyResult/list omitted types.",
        )
    return types


def list_build_type_files(sqx_home: Path | str | None, *, opener=None) -> dict[str, object]:
    kwargs = {"method": "GET", "timeout": 5.0}
    if opener is not None:
        kwargs["opener"] = opener
    producer = sqx_local_json(sqx_home, BUILD_TYPE_LIST_PATH, **kwargs)
    templates = _file_names(producer.get("strategyTemplateFiles"))
    strategies = _file_names(producer.get("strategyFiles"))
    return {
        "schema": SQX_BUILD_TYPE_FILES_SCHEMA,
        "source_build": SQX_BUILD,
        "templates": templates,
        "strategies": strategies,
        "detail": "Official StrategyQuant X buildType/listFiles names.",
    }


def _symbol_name(value: object) -> str:
    if not isinstance(value, str):
        raise SqxNativeWebError(
            "installed_data_invalid",
            "StrategyQuant X constants/getAll omitted a usable symbol.",
        )
    name = value.strip()
    if (
        not name
        or "\0" in name
        or "/" in name
        or "\\" in name
        or len(name) > _SYMBOL_NAME_MAX
    ):
        raise SqxNativeWebError(
            "installed_data_invalid",
            "StrategyQuant X constants/getAll omitted a usable symbol.",
        )
    return name


def _installed_symbols(values: object) -> list[str]:
    if not isinstance(values, list):
        raise SqxNativeWebError(
            "installed_data_invalid",
            "StrategyQuant X constants/getAll omitted constants.data.",
        )
    names: list[str] = []
    seen: set[str] = set()
    for item in values[:_INSTALLED_DATA_MAX]:
        if not isinstance(item, dict):
            raise SqxNativeWebError(
                "installed_data_invalid",
                "StrategyQuant X installed-data row is not an object.",
            )
        name = _symbol_name(item.get("symbol"))
        if name in seen:
            continue
        seen.add(name)
        names.append(name)
    return names


def _optional_named_list(values: object, field: str, *, limit: int, max_len: int) -> list[str]:
    if values is None:
        return []
    if not isinstance(values, list):
        raise SqxNativeWebError(
            "installed_data_invalid",
            f"StrategyQuant X constants/getAll omitted constants.{field}.",
        )
    names: list[str] = []
    seen: set[str] = set()
    for item in values[:limit]:
        if not isinstance(item, dict):
            raise SqxNativeWebError(
                "installed_data_invalid",
                f"StrategyQuant X {field} row is not an object.",
            )
        name = _symbol_name(item.get("name") if field == "sessions" else item.get(field, item.get("name")))
        if len(name) > max_len:
            raise SqxNativeWebError(
                "installed_data_invalid",
                f"StrategyQuant X {field} name is too long.",
            )
        if name in seen:
            continue
        seen.add(name)
        names.append(name)
    return names


def _precisions(values: object) -> list[dict[str, str]]:
    if values is None:
        return []
    if not isinstance(values, list):
        raise SqxNativeWebError(
            "installed_data_invalid",
            "StrategyQuant X constants/getAll omitted constants.precisions.",
        )
    rows: list[dict[str, str]] = []
    seen: set[str] = set()
    for item in values[:_PRECISION_MAX]:
        if not isinstance(item, dict):
            raise SqxNativeWebError(
                "installed_data_invalid",
                "StrategyQuant X precision row is not an object.",
            )
        raw = item.get("value")
        if isinstance(raw, bool) or not isinstance(raw, (str, int)):
            raise SqxNativeWebError(
                "installed_data_invalid",
                "StrategyQuant X precision omitted value.",
            )
        key = str(raw).strip()
        name = item.get("name")
        if not key or "\0" in key or "/" in key or "\\" in key:
            raise SqxNativeWebError(
                "installed_data_invalid",
                "StrategyQuant X precision omitted value.",
            )
        if not isinstance(name, str) or not name.strip() or "\0" in name:
            raise SqxNativeWebError(
                "installed_data_invalid",
                "StrategyQuant X precision omitted name.",
            )
        if key in seen:
            continue
        seen.add(key)
        rows.append({"key": key, "name": name.strip()})
    return rows


def _millis(value: object) -> int | None:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        return None
    number = int(value)
    return number if number > 0 else None


def _installed_rows(values: object) -> list[dict[str, object]]:
    if not isinstance(values, list):
        return []
    rows: list[dict[str, object]] = []
    seen: set[str] = set()
    for item in values[:_INSTALLED_DATA_MAX]:
        if not isinstance(item, dict):
            continue
        try:
            name = _symbol_name(item.get("symbol"))
        except SqxNativeWebError:
            continue
        if name in seen:
            continue
        seen.add(name)
        data_type = item.get("dataType")
        row: dict[str, object] = {"symbol": name}
        if isinstance(data_type, bool):
            pass
        elif isinstance(data_type, (int, str)) and str(data_type).strip():
            row["dataType"] = str(data_type).strip()
        date_from = _millis(item.get("dateFrom"))
        date_to = _millis(item.get("dateTo"))
        if date_from is not None:
            row["dateFrom"] = date_from
        if date_to is not None:
            row["dateTo"] = date_to
        raw_rows = item.get("rows")
        if not isinstance(raw_rows, bool) and isinstance(raw_rows, int):
            row["rows"] = raw_rows
        if isinstance(item.get("show"), bool):
            row["show"] = item["show"]
        timeframe = item.get("timeframe")
        if isinstance(timeframe, str) and timeframe.strip() and "\0" not in timeframe:
            row["timeframe"] = timeframe.strip()
        rows.append(row)
    return rows


def _value_name_list(values: object, field: str, *, limit: int) -> list[dict[str, str]]:
    if values is None:
        return []
    if not isinstance(values, list):
        raise SqxNativeWebError(
            "installed_data_invalid",
            f"StrategyQuant X constants/getAll omitted constants.{field}.",
        )
    return _precisions(values) if field == "precisions" else _named_values(values, limit)


def _named_values(values: list[object], limit: int) -> list[dict[str, str]]:
    rows: list[dict[str, str]] = []
    seen: set[str] = set()
    for item in values[:limit]:
        if not isinstance(item, dict):
            raise SqxNativeWebError(
                "installed_data_invalid",
                "StrategyQuant X constants/getAll row is not an object.",
            )
        raw = item.get("value")
        if isinstance(raw, bool) or not isinstance(raw, (str, int)):
            raise SqxNativeWebError(
                "installed_data_invalid",
                "StrategyQuant X constants/getAll omitted value.",
            )
        key = str(raw).strip()
        name = item.get("name")
        if not key or "\0" in key or "/" in key or "\\" in key:
            raise SqxNativeWebError(
                "installed_data_invalid",
                "StrategyQuant X constants/getAll omitted value.",
            )
        if not isinstance(name, str) or not name.strip() or "\0" in name:
            raise SqxNativeWebError(
                "installed_data_invalid",
                "StrategyQuant X constants/getAll omitted name.",
            )
        if key in seen:
            continue
        seen.add(key)
        rows.append({"key": key, "name": name.strip()})
    return rows


def _choice_values(values: object) -> list[str]:
    if values is None:
        return []
    names: list[str] = []
    seen: set[str] = set()
    items: list[object]
    if isinstance(values, dict):
        items = list(values.values())
    elif isinstance(values, list):
        items = values
    else:
        raise SqxNativeWebError(
            "installed_data_invalid",
            "StrategyQuant X constants/getAll omitted a choice list.",
        )
    for item in items[:_DATA_TYPE_MAX]:
        name = item if isinstance(item, str) else (item.get("name") if isinstance(item, dict) else None)
        if not isinstance(name, str) or not name.strip() or "\0" in name:
            raise SqxNativeWebError(
                "installed_data_invalid",
                "StrategyQuant X constants/getAll omitted a usable choice name.",
            )
        exact = name.strip()
        if exact in seen:
            continue
        seen.add(exact)
        names.append(exact)
    return names


def list_installed_data_symbols(sqx_home: Path | str | None, *, opener=None) -> dict[str, object]:
    kwargs = {"method": "GET", "timeout": 5.0}
    if opener is not None:
        kwargs["opener"] = opener
    producer = sqx_local_json(sqx_home, CONSTANTS_GET_ALL_PATH, **kwargs)
    constants = producer.get("constants")
    if not isinstance(constants, dict):
        raise SqxNativeWebError(
            "installed_data_invalid",
            "StrategyQuant X constants/getAll omitted constants.",
        )
    data = constants.get("data")
    return {
        "schema": SQX_INSTALLED_DATA_SCHEMA,
        "source_build": SQX_BUILD,
        "symbols": _installed_symbols(data),
        "rows": _installed_rows(data if isinstance(data, list) else []),
        "dataTypes": _value_name_list(constants.get("dataTypes"), "dataTypes", limit=_DATA_TYPE_MAX),
        "sessions": _optional_named_list(constants.get("sessions"), "sessions", limit=_SESSION_MAX, max_len=_SYMBOL_NAME_MAX),
        "precisions": _precisions(constants.get("precisions")),
        "swapTypes": _choice_values(constants.get("swapTypes")),
        "tripleSwapOptions": _choice_values(constants.get("tripleSwapOptions")),
        "detail": "Official StrategyQuant X constants/getAll installed-data symbols, sessions, and precisions.",
    }


def _symbol_data_points(values: object) -> list[list[float]]:
    if not isinstance(values, list):
        raise SqxNativeWebError(
            "symbol_data_invalid",
            "StrategyQuant X data/getSymbolData omitted data.",
        )
    points: list[list[float]] = []
    for item in values[:_SYMBOL_DATA_POINTS_MAX]:
        if not isinstance(item, (list, tuple)) or len(item) < 2:
            raise SqxNativeWebError(
                "symbol_data_invalid",
                "StrategyQuant X data/getSymbolData omitted a point.",
            )
        stamp, value = item[0], item[1]
        if isinstance(stamp, bool) or isinstance(value, bool) or not isinstance(stamp, (int, float)) or not isinstance(value, (int, float)):
            raise SqxNativeWebError(
                "symbol_data_invalid",
                "StrategyQuant X data/getSymbolData omitted a point.",
            )
        points.append([float(stamp), float(value)])
    if not points:
        return []
    minimum = min(row[1] for row in points)
    return [[row[0], row[1] - minimum] for row in points]


def _symbol_data_field(value: object, name: str) -> str:
    if not isinstance(value, str) or not value.strip() or "\0" in value or "/" in value or "\\" in value or len(value) > _SYMBOL_NAME_MAX:
        raise SqxNativeWebError(
            "symbol_data_invalid",
            f"StrategyQuant X data/getSymbolData omitted {name}.",
        )
    return value.strip()


def fetch_symbol_data(
    sqx_home: Path | str | None,
    date_from: object,
    date_to: object,
    symbol: object,
    session: object,
    *,
    opener=None,
) -> dict[str, object]:
    exact_from = _symbol_data_field(date_from, "dateFrom")
    exact_to = _symbol_data_field(date_to, "dateTo")
    exact_symbol = _symbol_name(symbol)
    exact_session = _symbol_data_field(session, "session")
    kwargs: dict[str, object] = {
        "method": "POST",
        "timeout": 15.0,
        "fields": {
            "dateFrom": exact_from,
            "dateTo": exact_to,
            "symbol": exact_symbol,
            "session": exact_session,
        },
    }
    if opener is not None:
        kwargs["opener"] = opener
    producer = sqx_local_json(sqx_home, SYMBOL_DATA_PATH, **kwargs)
    if producer.get("success") is not True:
        raise SqxNativeWebError(
            "symbol_data_unavailable",
            "StrategyQuant X data/getSymbolData failed.",
        )
    return {
        "schema": SQX_SYMBOL_DATA_SCHEMA,
        "source_build": SQX_BUILD,
        "symbol": exact_symbol,
        "dateFrom": exact_from,
        "dateTo": exact_to,
        "session": exact_session,
        "points": _symbol_data_points(producer.get("data")),
        "detail": "Official StrategyQuant X data/getSymbolData series with offset removed.",
    }


def _commission_methods(values: object) -> list[dict[str, str]]:
    if not isinstance(values, list) or not values:
        raise SqxNativeWebError(
            "commission_methods_invalid",
            "StrategyQuant X constants/listCommissionMethods omitted methods.",
        )
    rows: list[dict[str, str]] = []
    seen: set[str] = set()
    for item in values[:_COMMISSION_MAX]:
        if not isinstance(item, dict):
            raise SqxNativeWebError(
                "commission_methods_invalid",
                "StrategyQuant X commission method is not an object.",
            )
        key = item.get("class")
        name = item.get("display")
        if not isinstance(key, str) or not key.strip() or "\0" in key or "/" in key or "\\" in key or len(key) > 128:
            raise SqxNativeWebError(
                "commission_methods_invalid",
                "StrategyQuant X commission method omitted class.",
            )
        if not isinstance(name, str) or not name.strip() or "\0" in name or len(name) > 256:
            raise SqxNativeWebError(
                "commission_methods_invalid",
                "StrategyQuant X commission method omitted display.",
            )
        exact = key.strip()
        if exact in seen:
            continue
        seen.add(exact)
        rows.append({"key": exact, "name": name.strip()})
    if not rows:
        raise SqxNativeWebError(
            "commission_methods_invalid",
            "StrategyQuant X constants/listCommissionMethods omitted methods.",
        )
    return rows


def list_commission_methods(sqx_home: Path | str | None, *, opener=None) -> dict[str, object]:
    kwargs = {"method": "GET", "timeout": 5.0}
    if opener is not None:
        kwargs["opener"] = opener
    producer = sqx_local_json(sqx_home, COMMISSION_METHODS_LIST_PATH, **kwargs)
    return {
        "schema": SQX_COMMISSION_METHODS_SCHEMA,
        "source_build": SQX_BUILD,
        "methods": _commission_methods(producer.get("methods")),
        "detail": "Official StrategyQuant X constants/listCommissionMethods classes.",
    }


def list_ranking_fitness_types(sqx_home: Path | str | None, *, opener=None) -> dict[str, object]:
    kwargs = {"method": "GET", "timeout": 5.0}
    if opener is not None:
        kwargs["opener"] = opener
    producer = sqx_local_json(sqx_home, RANKING_FITNESS_LIST_PATH, **kwargs)
    return {
        "schema": SQX_RANKING_FITNESS_SCHEMA,
        "source_build": SQX_BUILD,
        "types": _fitness_types(producer.get("types")),
        "detail": "Official StrategyQuant X fitnessMethodStrategyResult/list types.",
    }


def template_chart_updates(root: ElementTree.Element, chart_settings: object) -> list[dict[str, object]]:
    if not isinstance(chart_settings, list) or not chart_settings:
        return []
    data = _named(root, "Data")
    setups = _named(data, "Setups")
    setup = _named(setups, "Setup")
    if setup is None:
        return []
    charts = [child for child in setup if _local_name(child.tag) == "Chart"]
    updates: list[dict[str, object]] = []
    for index, chart in enumerate(charts):
        if index == 0 or index >= len(chart_settings):
            continue
        item = chart_settings[index]
        if not isinstance(item, dict):
            continue
        step = "Chart" if len(charts) == 1 else f"Chart:{index + 1}"
        path = ["Data", "Setups", "Setup", step]
        symbol = item.get("symbol")
        timeframe = item.get("timeframe")
        if isinstance(symbol, str) and symbol.strip() and "symbol" in chart.attrib:
            updates.append({"path": path, "attribute": "symbol", "value": symbol.strip()})
        if isinstance(timeframe, str) and timeframe.strip() and "timeframe" in chart.attrib:
            updates.append({"path": path, "attribute": "timeframe", "value": timeframe.strip()})
    return updates


def apply_template_chart_settings(root: ElementTree.Element, chart_settings: object) -> int:
    updates = template_chart_updates(root, chart_settings)
    charts = []
    data = _named(root, "Data")
    setups = _named(data, "Setups")
    setup = _named(setups, "Setup")
    if setup is not None:
        charts = [child for child in setup if _local_name(child.tag) == "Chart"]
    for item in updates:
        path = item["path"]
        if not isinstance(path, list) or not path:
            continue
        step = str(path[-1])
        index = 0
        if ":" in step:
            index = int(step.split(":", 1)[1]) - 1
        if index < 0 or index >= len(charts):
            continue
        charts[index].set(str(item["attribute"]), str(item["value"]))
    return len(updates)


def _authorize_template_file(file_name: str, listed: object) -> str:
    if isinstance(listed, list) and file_name in listed:
        return file_name
    raise SqxNativeWebError(
        "build_type_template_invalid",
        "Reload accepts an official listFiles template name.",
    )


def reload_build_template(
    sqx_home: Path | str | None,
    project: str,
    task_index: int,
    file_name: str,
    *,
    apply: bool = True,
    opener=None,
) -> dict[str, object]:
    if not isinstance(project, str) or not project.strip():
        raise SqxCustomProjectTopologyError(
            "custom_project_name_invalid",
            "Template reload requires one exact native project name.",
        )
    if not isinstance(task_index, int) or isinstance(task_index, bool) or task_index < 1:
        raise SqxCustomProjectTopologyError(
            "custom_project_task_index_invalid",
            "Template reload requires one exact native task index.",
        )
    if not isinstance(file_name, str) or not file_name.strip() or "\0" in file_name or len(file_name) > _FILE_NAME_MAX:
        raise SqxNativeWebError(
            "build_type_template_invalid",
            "Template reload requires the official template file name.",
        )

    listed = list_build_type_files(sqx_home, opener=opener)
    authorized = _authorize_template_file(file_name.strip(), listed["templates"])
    topology = read_sqx_custom_project_topology(sqx_home, project, omit_building_block_rows=True)
    task = next((item for item in topology.tasks if item.native_task_index == task_index), None)
    if task is None:
        raise SqxCustomProjectTopologyError(
            "custom_project_task_missing",
            f"This project has no native task {task_index}.",
        )

    kwargs = {
        "method": "POST",
        "fields": {"fileName": authorized, "reload": "true"},
    }
    if opener is not None:
        kwargs["opener"] = opener
    producer = sqx_local_json(sqx_home, BUILD_TYPE_TEMPLATE_PATH, **kwargs)
    chart_settings = producer.get("chartSettings")
    refreshed = _file_names(producer["strategyTemplateFiles"]) if "strategyTemplateFiles" in producer else listed["templates"]

    home = _verified_home(sqx_home)
    archive_path = _resolved_project_archive(home, project)
    snapshot = _read_archive_snapshot(archive_path)
    with ZipFile(BytesIO(snapshot)) as archive:
        try:
            payload = archive.read(task.entry_name)
        except KeyError as exc:
            raise SqxCustomProjectTopologyError(
                "custom_project_task_missing",
                f"SQX project is missing {task.entry_name}",
            ) from exc
    root = ElementTree.fromstring(payload)
    updates = template_chart_updates(root, chart_settings)
    persisted = None
    if apply and updates:
        persisted = update_custom_project_settings(sqx_home, project, task_index, updates)

    return {
        "schema": SQX_BUILD_TYPE_TEMPLATE_SCHEMA,
        "source_build": SQX_BUILD,
        "project": project,
        "task_index": task_index,
        "file_name": authorized,
        "templates": refreshed,
        "applied": bool(persisted),
        "updated_charts": len(updates),
        "source_relative_path": _project_relative_path(project),
        "archive_sha256": persisted["archive_sha256"] if persisted else topology.archive_sha256,
        "previous_archive_sha256": topology.archive_sha256,
        "detail": (
            "Reloaded the official StrategyQuant X template and wrote existing additional-chart attributes."
            if persisted
            else "StrategyQuant X returned getTemplateConfig. Nothing was written."
        ),
    }
