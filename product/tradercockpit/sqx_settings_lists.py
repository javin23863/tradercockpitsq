"""Official SQX What-to-build file lists and Ranking fitness types.

Calls the same local-web servlets the Electron UI uses. Does not invent
template names, fitness keys, or extra XML attributes.
"""

from __future__ import annotations

from hashlib import sha256
from io import BytesIO
from pathlib import Path
from xml.etree import ElementTree
from zipfile import ZipFile, ZIP_DEFLATED

from .sqx_custom_project import (
    SqxCustomProjectTopologyError,
    _local_name,
    _project_relative_path,
    _read_archive_snapshot,
    _resolved_project_archive,
    _verified_home,
    read_sqx_custom_project_topology,
)
from .sqx_native_web import SqxNativeWebError, sqx_local_json
from .sqx_presets import SQX_BUILD


SQX_BUILD_TYPE_FILES_SCHEMA = "tc.sqx-build-type-files.v1"
SQX_BUILD_TYPE_TEMPLATE_SCHEMA = "tc.sqx-build-type-template.v1"
SQX_RANKING_FITNESS_SCHEMA = "tc.sqx-ranking-fitness-types.v1"
SQX_BUILD_TYPE_FILES_API_PATH = "/api/sqx-build-type-files"
SQX_BUILD_TYPE_TEMPLATE_API_PATH = "/api/sqx-build-type-template"
SQX_RANKING_FITNESS_API_PATH = "/api/sqx-ranking-fitness-types"
BUILD_TYPE_LIST_PATH = "/buildType/listFiles"
BUILD_TYPE_TEMPLATE_PATH = "/buildType/getTemplateConfig"
RANKING_FITNESS_LIST_PATH = "/fitnessMethodStrategyResult/list"
_FILE_NAME_MAX = 512


def _named(root: ElementTree.Element | None, name: str) -> ElementTree.Element | None:
    if root is None:
        return None
    return next((child for child in root if _local_name(child.tag) == name), None)


def _file_name(value: object) -> str:
    if isinstance(value, str):
        name = value.strip()
    elif isinstance(value, dict):
        raw = value.get("name") or value.get("fileName") or value.get("file") or value.get("path")
        name = raw.strip() if isinstance(raw, str) else ""
    else:
        name = ""
    if not name or "\0" in name or len(name) > _FILE_NAME_MAX:
        raise SqxNativeWebError(
            "build_type_files_invalid",
            "StrategyQuant X listFiles omitted a usable file name.",
        )
    return name


def _file_names(values: object) -> list[str]:
    if values is None:
        return []
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
    kwargs = {"method": "GET"}
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


def list_ranking_fitness_types(sqx_home: Path | str | None, *, opener=None) -> dict[str, object]:
    kwargs = {"method": "GET"}
    if opener is not None:
        kwargs["opener"] = opener
    producer = sqx_local_json(sqx_home, RANKING_FITNESS_LIST_PATH, **kwargs)
    return {
        "schema": SQX_RANKING_FITNESS_SCHEMA,
        "source_build": SQX_BUILD,
        "types": _fitness_types(producer.get("types")),
        "detail": "Official StrategyQuant X fitnessMethodStrategyResult/list types.",
    }


def apply_template_chart_settings(root: ElementTree.Element, chart_settings: object) -> int:
    if not isinstance(chart_settings, list) or not chart_settings:
        return 0
    data = _named(root, "Data")
    setups = _named(data, "Setups")
    setup = _named(setups, "Setup")
    if setup is None:
        return 0
    charts = [child for child in setup if _local_name(child.tag) == "Chart"]
    updated = 0
    for index, chart in enumerate(charts):
        if index == 0 or index >= len(chart_settings):
            continue
        item = chart_settings[index]
        if not isinstance(item, dict):
            continue
        symbol = item.get("symbol")
        timeframe = item.get("timeframe")
        if isinstance(symbol, str) and symbol.strip() and "symbol" in chart.attrib:
            chart.set("symbol", symbol.strip())
            updated += 1
        if isinstance(timeframe, str) and timeframe.strip() and "timeframe" in chart.attrib:
            chart.set("timeframe", timeframe.strip())
            updated += 1
    return updated


def _authorize_template_file(home: Path, file_name: str, listed: list[str]) -> str:
    if file_name in listed:
        return file_name
    path = Path(file_name)
    if not path.is_absolute():
        raise SqxNativeWebError(
            "build_type_template_invalid",
            "Reload accepts an official listFiles name or a path inside the verified runtime.",
        )
    try:
        resolved = path.resolve()
        resolved.relative_to(home.resolve())
    except (OSError, RuntimeError, ValueError) as exc:
        raise SqxCustomProjectTopologyError(
            "custom_project_path_escape",
            "Template path resolves outside the verified runtime",
        ) from exc
    if resolved.is_symlink() or not resolved.is_file():
        raise SqxNativeWebError(
            "build_type_template_invalid",
            "Template file is not inside the verified StrategyQuant X runtime.",
        )
    return str(resolved)


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
    home = _verified_home(sqx_home)
    authorized = _authorize_template_file(home, file_name.strip(), listed["templates"])
    topology = read_sqx_custom_project_topology(sqx_home, project)
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
    refreshed = _file_names(producer.get("strategyTemplateFiles")) if "strategyTemplateFiles" in producer else listed["templates"]

    archive_path = _resolved_project_archive(home, project)
    snapshot = _read_archive_snapshot(archive_path)
    updated_charts = 0
    written = snapshot
    if apply:
        with ZipFile(BytesIO(snapshot)) as archive:
            try:
                payload = archive.read(task.entry_name)
            except KeyError as exc:
                raise SqxCustomProjectTopologyError(
                    "custom_project_task_missing",
                    f"SQX project is missing {task.entry_name}",
                ) from exc
            members = {info.filename: archive.read(info.filename) for info in archive.infolist()}
        root = ElementTree.fromstring(payload)
        updated_charts = apply_template_chart_settings(root, chart_settings)
        members[task.entry_name] = ElementTree.tostring(root, encoding="utf-8")
        buffer = BytesIO()
        with ZipFile(buffer, "w", compression=ZIP_DEFLATED) as archive:
            for name, body in members.items():
                archive.writestr(name, body)
        written = buffer.getvalue()
        archive_path.write_bytes(written)

    return {
        "schema": SQX_BUILD_TYPE_TEMPLATE_SCHEMA,
        "source_build": SQX_BUILD,
        "project": project,
        "task_index": task_index,
        "file_name": authorized,
        "templates": refreshed,
        "applied": apply,
        "updated_charts": updated_charts,
        "source_relative_path": _project_relative_path(project),
        "archive_sha256": sha256(written).hexdigest() if apply else topology.archive_sha256,
        "previous_archive_sha256": topology.archive_sha256,
        "detail": (
            "Reloaded the official StrategyQuant X template and applied returned additional-chart settings."
            if apply
            else "StrategyQuant X returned getTemplateConfig. Nothing was written."
        ),
    }
