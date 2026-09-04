"""Call installed SQX indicator calibration and apply its ranges.

Posts the same ``indyTester/calibrate`` fields the Electron UI sends. Apply
writes producer min/max/step onto existing Block / #Level# Param nodes. It does
not invent blocks, params, or ranges.
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
    settings_sections,
)
from .sqx_native_web import SqxNativeWebError, sqx_local_json
from .sqx_presets import SQX_BUILD


SQX_CALIBRATE_SCHEMA = "tc.sqx-calibrate.v1"
SQX_CALIBRATE_API_PATH = "/api/sqx-calibrate"
CALIBRATE_SERVLET_PATH = "/indyTester/calibrate"
CALIBRATE_TIMEOUT_SECONDS = 300.0
_SAME_AS_MAIN = "Same as main chart"
_RANGE_ATTRS = ("indicatorMin", "indicatorMax", "indicatorStep")


def _named(root: ElementTree.Element | None, name: str) -> ElementTree.Element | None:
    if root is None:
        return None
    return next((child for child in root if _local_name(child.tag) == name), None)


def _range_text(value: object) -> str:
    if isinstance(value, bool) or value is None:
        raise SqxNativeWebError(
            "calibrate_results_invalid",
            "StrategyQuant X calibration omitted a numeric min, max, or step.",
        )
    if isinstance(value, int):
        return str(value)
    if isinstance(value, float):
        if value.is_integer():
            return str(int(value))
        text = format(value, ".12g")
        if not text or text in {"nan", "inf", "-inf"}:
            raise SqxNativeWebError(
                "calibrate_results_invalid",
                "StrategyQuant X calibration returned a non-finite range.",
            )
        return text
    if isinstance(value, str) and value.strip():
        try:
            number = float(value.strip())
        except ValueError as exc:
            raise SqxNativeWebError(
                "calibrate_results_invalid",
                "StrategyQuant X calibration returned a non-numeric range.",
            ) from exc
        if number != number or number in {float("inf"), float("-inf")}:
            raise SqxNativeWebError(
                "calibrate_results_invalid",
                "StrategyQuant X calibration returned a non-finite range.",
            )
        return value.strip()
    raise SqxNativeWebError(
        "calibrate_results_invalid",
        "StrategyQuant X calibration omitted a numeric min, max, or step.",
    )


def _chart_symbol(chart: ElementTree.Element, main_symbol: str) -> str:
    symbol = (chart.attrib.get("symbol") or "").strip()
    if symbol == _SAME_AS_MAIN:
        return main_symbol
    return symbol


def calibrate_request_fields(root: ElementTree.Element, project: str, task_name: str) -> dict[str, str]:
    data = next((item for item in root.iter() if _local_name(item.tag) == "Data"), None)
    setups = _named(data, "Setups")
    setup = _named(setups, "Setup")
    charts = [child for child in (list(setup) if setup is not None else []) if _local_name(child.tag) == "Chart"]
    if setup is None or not charts:
        raise SqxCustomProjectTopologyError(
            "calibrate_data_missing",
            "This task has no Data setup Chart. Indicator calibration needs that native setup.",
        )
    engine = (setup.attrib.get("engine") or "").strip()
    main_symbol = _chart_symbol(charts[0], "")
    symbols = [_chart_symbol(chart, main_symbol) for chart in charts]
    timeframes = [(chart.attrib.get("timeframe") or "").strip() for chart in charts]
    if not engine or not all(symbols) or not all(timeframes):
        raise SqxCustomProjectTopologyError(
            "calibrate_data_missing",
            "Main Data setup is missing engine, symbol, or timeframe.",
        )
    calibration = next((item for item in root.iter() if _local_name(item.tag) == "Calibration"), None)
    max_steps = "-1"
    if calibration is not None and calibration.attrib.get("useMaxSteps") == "true":
        steps = (calibration.attrib.get("maxSteps") or "").strip()
        if not steps:
            raise SqxCustomProjectTopologyError(
                "calibrate_data_missing",
                "Calibration useMaxSteps is true but maxSteps is missing.",
            )
        max_steps = steps
    return {
        "projectName": project,
        "taskName": task_name,
        "symbols": ",".join(symbols),
        "timeframes": ",".join(timeframes),
        "maxSteps": max_steps,
        "engine": engine,
    }


def public_calibration_results(payload: dict[str, object]) -> list[dict[str, object]]:
    raw = payload.get("calibrationResults")
    if not isinstance(raw, list) or not raw:
        raise SqxNativeWebError(
            "calibrate_results_invalid",
            "StrategyQuant X local web did not return calibrationResults.",
        )
    results: list[dict[str, object]] = []
    for item in raw:
        if not isinstance(item, dict):
            raise SqxNativeWebError(
                "calibrate_results_invalid",
                "StrategyQuant X calibrationResults must be objects.",
            )
        key = item.get("key")
        ranges = item.get("ranges")
        if not isinstance(key, str) or not key.strip():
            raise SqxNativeWebError(
                "calibrate_results_invalid",
                "StrategyQuant X calibrationResults omitted a block key.",
            )
        if not isinstance(ranges, list) or not ranges or not isinstance(ranges[0], dict):
            raise SqxNativeWebError(
                "calibrate_results_invalid",
                "StrategyQuant X calibrationResults omitted ranges.",
            )
        first = ranges[0]
        results.append(
            {
                "key": key.strip(),
                "ranges": [
                    {
                        "minValue": _range_text(first.get("minValue")),
                        "maxValue": _range_text(first.get("maxValue")),
                        "step": _range_text(first.get("step")),
                    }
                ],
            }
        )
    return results


def _indicator_last_segments(root: ElementTree.Element) -> tuple[str, ...]:
    names: list[str] = []
    for block in root.iter():
        if _local_name(block.tag) != "Block":
            continue
        category = block.attrib.get("category")
        key = block.attrib.get("key") or ""
        if category == "indicators" or (
            category == "stopLimitBlocks" and "Price Ranges" in key
        ):
            last = key.split(".")[-1]
            if last and last not in names:
                names.append(last)
    return tuple(names)


def _signal_matches(signal_key: str, result_key: str, families: tuple[str, ...]) -> bool:
    if signal_key != result_key and not (
        signal_key.startswith(result_key)
        and len(signal_key) > len(result_key)
        and signal_key[len(result_key)].isupper()
    ):
        return False
    return not any(
        family != result_key and family.startswith(result_key) and signal_key.startswith(family)
        for family in families
    )


def _block_takes_indicator_range(block: ElementTree.Element, result_key: str) -> bool:
    key = block.attrib.get("key") or ""
    category = block.attrib.get("category")
    last = key.split(".")[-1]
    if last != result_key:
        return False
    if category == "indicators":
        return True
    return category == "stopLimitBlocks" and "Price Ranges" in key


def _set_level_params(block: ElementTree.Element, minimum: str, maximum: str, step: str) -> int:
    updated = 0
    values = {"minValue": minimum, "maxValue": maximum, "step": step}
    for param in block.iter():
        if _local_name(param.tag) != "Param" or param.attrib.get("key") != "#Level#":
            continue
        if "minValue" not in param.attrib:
            continue
        changed = False
        for attribute, value in values.items():
            if attribute in param.attrib and param.attrib[attribute] != value:
                param.attrib[attribute] = value
                changed = True
        if changed:
            updated += 1
    return updated


def apply_calibration_results(root: ElementTree.Element, results: list[dict[str, object]]) -> tuple[int, int]:
    families = _indicator_last_segments(root)
    blocks_updated = 0
    params_updated = 0
    for item in results:
        key = str(item["key"])
        first = item["ranges"][0]
        minimum = str(first["minValue"])
        maximum = str(first["maxValue"])
        step = str(first["step"])
        for block in root.iter():
            if _local_name(block.tag) != "Block":
                continue
            if _block_takes_indicator_range(block, key):
                before = tuple(block.attrib.get(name) for name in _RANGE_ATTRS)
                block.set("indicatorMin", minimum)
                block.set("indicatorMax", maximum)
                block.set("indicatorStep", step)
                if before != (minimum, maximum, step):
                    blocks_updated += 1
                continue
            if block.attrib.get("category") == "signals" and _signal_matches(
                block.attrib.get("key") or "",
                key,
                families,
            ):
                params_updated += _set_level_params(block, minimum, maximum, step)
    return blocks_updated, params_updated


def _task_name(task) -> str:
    if task.name:
        return task.name
    entry = task.entry_name
    return entry[:-4] if entry.lower().endswith(".xml") else entry


def calibrate_indicators(
    sqx_home: Path | str | None,
    project: str,
    task_index: int,
    *,
    apply: bool = True,
    opener=None,
) -> dict[str, object]:
    if not isinstance(project, str) or not project.strip():
        raise SqxCustomProjectTopologyError(
            "custom_project_name_invalid",
            "Calibrate requires one exact native project name.",
        )
    if not isinstance(task_index, int) or isinstance(task_index, bool) or task_index < 1:
        raise SqxCustomProjectTopologyError(
            "custom_project_task_index_invalid",
            "Calibrate requires one exact native task index.",
        )

    topology = read_sqx_custom_project_topology(sqx_home, project)
    task = next((item for item in topology.tasks if item.native_task_index == task_index), None)
    if task is None:
        raise SqxCustomProjectTopologyError(
            "custom_project_task_missing",
            f"This project has no native task {task_index}.",
        )

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
        members = {info.filename: archive.read(info.filename) for info in archive.infolist()}

    root = ElementTree.fromstring(payload)
    fields = calibrate_request_fields(root, project, _task_name(task))
    kwargs = {
        "method": "POST",
        "fields": fields,
        "timeout": CALIBRATE_TIMEOUT_SECONDS,
    }
    if opener is not None:
        kwargs["opener"] = opener
    producer = sqx_local_json(sqx_home, CALIBRATE_SERVLET_PATH, **kwargs)
    results = public_calibration_results(producer)

    blocks_updated = 0
    params_updated = 0
    written = snapshot
    if apply:
        blocks_updated, params_updated = apply_calibration_results(root, results)
        members[task.entry_name] = ElementTree.tostring(root, encoding="utf-8")
        buffer = BytesIO()
        with ZipFile(buffer, "w", compression=ZIP_DEFLATED) as archive:
            for name, body in members.items():
                archive.writestr(name, body)
        written = buffer.getvalue()
        archive_path.write_bytes(written)

    return {
        "schema": SQX_CALIBRATE_SCHEMA,
        "source_build": SQX_BUILD,
        "project": project,
        "task_index": task_index,
        "entry_name": task.entry_name,
        "source_relative_path": _project_relative_path(project),
        "request": fields,
        "calibration_results": results,
        "applied": apply,
        "updated_blocks": blocks_updated,
        "updated_params": params_updated,
        "archive_sha256": sha256(written).hexdigest() if apply else topology.archive_sha256,
        "previous_archive_sha256": topology.archive_sha256,
        "settings": list(settings_sections(root)) if apply else [],
        "detail": (
            "Applied StrategyQuant X calibrationResults onto existing blocks."
            if apply
            else "StrategyQuant X returned calibrationResults. Nothing was written."
        ),
    }
