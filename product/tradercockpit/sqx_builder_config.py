"""Read-only custody for the native SQX Builder project configuration.

Reads one physically bounded Builder project.cfx snapshot from exact SQX
144.2953, exposes only facts present in native XML, and resolves the native
requirements needed by Research → Specification. It never infers producer
semantics, preset binding, configuration writes, or native execution.
"""

from __future__ import annotations

from dataclasses import dataclass
from hashlib import sha256
from io import BytesIO
from pathlib import Path
from typing import Iterable
from xml.etree import ElementTree
from zipfile import BadZipFile, ZipFile
import zlib

from .sqx_presets import SQX_BUILD, verified_sqx_home


SQX_BUILDER_CONFIG_SCHEMA = "tc.sqx-builder-config.v1"
SQX_BUILDER_SEARCH_SCHEMA = "tc.sqx-builder-search.v1"
SQX_BUILDER_BLOCKS_SCHEMA = "tc.sqx-builder-blocks.v1"
SQX_BUILDER_RANKINGS_SCHEMA = "tc.sqx-builder-rankings.v1"
SQX_BUILDER_CROSS_CHECKS_SCHEMA = "tc.sqx-builder-cross-checks.v1"
SQX_BUILDER_MONEY_MANAGEMENT_SCHEMA = "tc.sqx-builder-money-management.v1"
SQX_RESEARCH_SPECIFICATION_SCHEMA = "tc.research-specification.v1"
SQX_BUILDER_PROJECT_RELATIVE_PATH = "user/projects/Builder/project.cfx"
SQX_BUILDER_REQUIRED_ENTRIES = ("config.xml", "Build-Task1.xml")
SQX_BUILDER_TASK_ENTRY = "Build-Task1.xml"
SQX_BUILDER_PRESET_BINDING_STATUS = "market_proven_preset_unverified"


class SqxBuilderConfigError(RuntimeError):
    def __init__(self, code: str, detail: str):
        super().__init__(detail)
        self.code = code
        self.detail = detail


@dataclass(frozen=True, slots=True)
class SqxBuilderChart:
    symbol: str
    timeframe: str


@dataclass(frozen=True, slots=True)
class SqxBuilderInstrument:
    instrument: str
    tick_size: str | None = None
    point_value: str | None = None
    data_type: str | None = None


@dataclass(frozen=True, slots=True)
class SqxBuilderDataSetup:
    symbol: str | None = None
    timeframe: str | None = None
    spread: str | None = None
    date_from: str | None = None
    date_to: str | None = None
    test_precision: str | None = None
    engine: str | None = None
    slippage: str | None = None
    min_distance: str | None = None
    has_commissions: bool = False


@dataclass(frozen=True, slots=True)
class SqxBuilderNativeNode:
    tag: str
    attributes: tuple[tuple[str, str], ...]
    text: str | None
    children: tuple["SqxBuilderNativeNode", ...]


@dataclass(frozen=True, slots=True)
class SqxBuilderNativeSelections:
    strategy_type: str | None = None
    market_sides: str | None = None
    generation_type: str | None = None
    build_mode: SqxBuilderNativeNode | None = None
    blocks: SqxBuilderNativeNode | None = None
    rankings: SqxBuilderNativeNode | None = None
    cross_checks: SqxBuilderNativeNode | None = None
    money_management: SqxBuilderNativeNode | None = None
    stop_condition_type: str | None = None
    max_strategies: str | None = None
    data_setup: SqxBuilderDataSetup | None = None
    data_setup_count: int = 0
    has_build_trading_options: bool = False
    has_blocks: bool = False
    has_money_management: bool = False
    has_cross_checks: bool = False
    cross_checks_enabled: bool = False


@dataclass(frozen=True, slots=True)
class SqxBuilderProjectConfig:
    archive_path: Path
    archive_sha256: str
    charts: tuple[SqxBuilderChart, ...]
    instruments: tuple[SqxBuilderInstrument, ...]
    native: SqxBuilderNativeSelections
    internal_entries: tuple[str, ...] = SQX_BUILDER_REQUIRED_ENTRIES
    source_build: str = SQX_BUILD


def _local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def _parse_xml(payload: bytes, entry_name: str) -> ElementTree.Element:
    try:
        return ElementTree.fromstring(payload)
    except (ElementTree.ParseError, LookupError, ValueError) as exc:
        raise SqxBuilderConfigError(
            "builder_project_xml_invalid",
            f"SQX Builder project entry {entry_name!r} is not valid XML",
        ) from exc


def _iter_named(root: ElementTree.Element, name: str) -> Iterable[ElementTree.Element]:
    for element in root.iter():
        if _local_name(element.tag) == name:
            yield element


def _first_named(root: ElementTree.Element | None, name: str) -> ElementTree.Element | None:
    return next(iter(_iter_named(root, name)), None) if root is not None else None


def _child_named(root: ElementTree.Element | None, name: str) -> ElementTree.Element | None:
    if root is None:
        return None
    return next((child for child in root if _local_name(child.tag) == name), None)


def _native_node(element: ElementTree.Element) -> SqxBuilderNativeNode:
    text = (element.text or "").strip() or None
    return SqxBuilderNativeNode(
        tag=_local_name(element.tag),
        attributes=tuple((str(key), str(value)) for key, value in element.attrib.items()),
        text=text,
        children=tuple(_native_node(child) for child in list(element)),
    )


def _native_node_record(node: SqxBuilderNativeNode | None) -> dict[str, object] | None:
    if node is None:
        return None
    return {
        "tag": node.tag,
        "attributes": {key: value for key, value in node.attributes},
        "text": node.text,
        "children": [_native_node_record(child) for child in node.children],
    }


def _dedupe(items: Iterable[object]) -> tuple[object, ...]:
    seen: set[object] = set()
    ordered: list[object] = []
    for item in items:
        if item not in seen:
            seen.add(item)
            ordered.append(item)
    return tuple(ordered)


def _resolved_builder_archive(home: Path) -> Path:
    candidate = home / SQX_BUILDER_PROJECT_RELATIVE_PATH
    try:
        resolved = candidate.resolve()
        resolved.relative_to(home)
    except (OSError, RuntimeError, ValueError) as exc:
        raise SqxBuilderConfigError(
            "builder_project_path_escape",
            "SQX Builder project resolves outside the verified runtime",
        ) from exc
    expected_parent = (home / "user/projects/Builder").resolve()
    if resolved.name != "project.cfx" or resolved.parent != expected_parent:
        raise SqxBuilderConfigError(
            "builder_project_path_escape",
            "SQX Builder project is not the exact verified user/projects/Builder/project.cfx path",
        )
    return resolved


def _read_project_entries(archive_snapshot: bytes) -> tuple[bytes, ...]:
    try:
        with ZipFile(BytesIO(archive_snapshot)) as archive:
            names = archive.namelist()
            if len(names) != len(set(names)):
                raise SqxBuilderConfigError(
                    "builder_project_duplicate_entries",
                    "SQX Builder project contains duplicate archive members",
                )
            missing = [name for name in SQX_BUILDER_REQUIRED_ENTRIES if name not in names]
            if missing:
                raise SqxBuilderConfigError(
                    "builder_project_entries_missing",
                    "SQX Builder project is missing required entries: " + ", ".join(missing),
                )
            return tuple(archive.read(name) for name in SQX_BUILDER_REQUIRED_ENTRIES)
    except SqxBuilderConfigError:
        raise
    except (BadZipFile, RuntimeError, NotImplementedError, OSError, EOFError, zlib.error) as exc:
        raise SqxBuilderConfigError(
            "builder_project_archive_invalid",
            "SQX Builder project.cfx is not a readable native project archive",
        ) from exc


def _commission_config_present(setup: ElementTree.Element | None) -> bool:
    if setup is None:
        return False
    legacy = setup.attrib.get("commissions")
    if legacy not in {None, "", "undefined"}:
        return True
    commissions = _child_named(setup, "Commissions")
    if commissions is None:
        return False
    methods = [child for child in commissions if _local_name(child.tag) == "Method"]
    if len(methods) == 1:
        return True
    return any(method.attrib.get("use", "").lower() == "true" for method in methods)


def _native_selections(task_root: ElementTree.Element) -> SqxBuilderNativeSelections:
    what_to_build = _first_named(task_root, "WhatToBuild")
    strategy_type = _child_named(what_to_build, "StrategyType")
    market_sides = _child_named(what_to_build, "MarketSides")
    build_mode = _child_named(what_to_build, "BuildMode")
    blocks = _first_named(task_root, "Blocks")

    data = _first_named(task_root, "Data")
    setups = _child_named(data, "Setups")
    setup_elements = (
        [child for child in setups if _local_name(child.tag) == "Setup"]
        if setups is not None
        else []
    )
    setup = setup_elements[0] if len(setup_elements) == 1 else None
    chart = _child_named(setup, "Chart")
    data_setup = None
    if setup is not None:
        data_setup = SqxBuilderDataSetup(
            symbol=chart.attrib.get("symbol") if chart is not None else None,
            timeframe=chart.attrib.get("timeframe") if chart is not None else None,
            spread=chart.attrib.get("spread") if chart is not None else None,
            date_from=setup.attrib.get("dateFrom"),
            date_to=setup.attrib.get("dateTo"),
            test_precision=setup.attrib.get("testPrecision"),
            engine=setup.attrib.get("engine"),
            slippage=setup.attrib.get("slippage"),
            min_distance=setup.attrib.get("minDist"),
            has_commissions=_commission_config_present(setup),
        )

    rankings = _first_named(task_root, "Rankings")
    max_strategies = _child_named(rankings, "MaxStrategies")
    stop_condition = _child_named(rankings, "StopCondition")
    options = _first_named(task_root, "Options")
    cross_checks = _first_named(task_root, "CrossChecks")
    money_management = _first_named(task_root, "MoneyManagement")
    return SqxBuilderNativeSelections(
        strategy_type=strategy_type.attrib.get("type") if strategy_type is not None else None,
        market_sides=market_sides.attrib.get("type") if market_sides is not None else None,
        generation_type=build_mode.attrib.get("generationType") if build_mode is not None else None,
        build_mode=_native_node(build_mode) if build_mode is not None else None,
        blocks=_native_node(blocks) if blocks is not None else None,
        rankings=_native_node(rankings) if rankings is not None else None,
        cross_checks=_native_node(cross_checks) if cross_checks is not None else None,
        money_management=_native_node(money_management) if money_management is not None else None,
        stop_condition_type=stop_condition.attrib.get("type") if stop_condition is not None else None,
        max_strategies=(max_strategies.text or "").strip() if max_strategies is not None else None,
        data_setup=data_setup,
        data_setup_count=len(setup_elements),
        has_build_trading_options=_child_named(options, "BuildTradingOptions") is not None,
        has_blocks=blocks is not None,
        has_money_management=money_management is not None,
        has_cross_checks=cross_checks is not None,
        cross_checks_enabled=(
            cross_checks is not None and cross_checks.attrib.get("use", "").lower() == "true"
        ),
    )


def _snapshot_components(
    archive_snapshot: bytes,
) -> tuple[tuple[SqxBuilderChart, ...], tuple[SqxBuilderInstrument, ...], SqxBuilderNativeSelections]:
    payloads = _read_project_entries(archive_snapshot)
    roots = tuple(
        _parse_xml(payload, entry_name)
        for entry_name, payload in zip(SQX_BUILDER_REQUIRED_ENTRIES, payloads, strict=True)
    )
    charts = _dedupe(
        SqxBuilderChart(element.attrib["symbol"], element.attrib["timeframe"])
        for root in roots
        for element in _iter_named(root, "Chart")
        if element.attrib.get("symbol") and element.attrib.get("timeframe")
    )
    instruments = _dedupe(
        SqxBuilderInstrument(
            element.attrib["instrument"],
            element.attrib.get("tickSize"),
            element.attrib.get("pointValue"),
            element.attrib.get("dataType"),
        )
        for root in roots
        for element in _iter_named(root, "InstrumentInfo")
        if element.attrib.get("instrument")
    )
    if not charts or not instruments:
        raise SqxBuilderConfigError(
            "builder_market_configuration_missing",
            "SQX Builder project does not contain proven Chart and InstrumentInfo market configuration",
        )
    return (
        charts,  # type: ignore[return-value]
        instruments,  # type: ignore[return-value]
        _native_selections(roots[1]),
    )


def validate_sqx_builder_project_snapshot(archive_snapshot: bytes) -> None:
    """Reapply the native archive/XML/market invariants to preserved project bytes."""

    _snapshot_components(archive_snapshot)


def read_sqx_builder_project(sqx_home: Path | str | None) -> SqxBuilderProjectConfig:
    home = verified_sqx_home(sqx_home)
    archive_path = _resolved_builder_archive(home)
    if not archive_path.is_file():
        raise SqxBuilderConfigError("builder_project_missing", f"SQX Builder project is missing: {archive_path}")
    try:
        archive_snapshot = archive_path.read_bytes()
    except OSError as exc:
        raise SqxBuilderConfigError(
            "builder_project_unreadable",
            f"SQX Builder project could not be read: {archive_path}",
        ) from exc

    charts, instruments, native = _snapshot_components(archive_snapshot)
    return SqxBuilderProjectConfig(
        archive_path=archive_path,
        archive_sha256=sha256(archive_snapshot).hexdigest(),
        charts=charts,
        instruments=instruments,
        native=native,
    )


def _state(configured: bool) -> str:
    return "producer_configured" if configured else "unresolved"


def _present(value: str | None) -> bool:
    return isinstance(value, str) and bool(value.strip())


def _data_setup_configured(native: SqxBuilderNativeSelections) -> bool:
    data = native.data_setup
    return bool(
        native.data_setup_count == 1
        and data is not None
        and all(
            _present(value)
            for value in (
                data.symbol,
                data.timeframe,
                data.spread,
                data.date_from,
                data.date_to,
                data.test_precision,
                data.engine,
                data.slippage,
                data.min_distance,
            )
        )
    )


def _requirement(
    requirement_id: str,
    label: str,
    state: str,
    *,
    required: bool,
    detail: str,
    evidence_path: str,
    values: dict[str, object] | None = None,
) -> dict[str, object]:
    return {
        "id": requirement_id,
        "label": label,
        "state": state,
        "required": required,
        "detail": detail,
        "evidence": {"source_build": SQX_BUILD, "native_source_path": evidence_path},
        "values": values or {},
    }


def _search_display_mode(generation_type: str | None) -> dict[str, object]:
    selector = generation_type.strip() if _present(generation_type) else None
    normalized = selector.casefold() if selector else None
    if normalized == "genetic-evolution":
        return {"kind": "genetic_evolution", "label": "Genetic Evolution", "recognized": True}
    if normalized == "random-generation":
        return {"kind": "random_discovery", "label": "Random Discovery", "recognized": True}
    if selector is None:
        return {
            "kind": "unresolved",
            "label": "Unresolved native search mode",
            "recognized": False,
        }
    return {"kind": "native_other", "label": "Other native search mode", "recognized": False}


def _search_configuration_record(config: SqxBuilderProjectConfig) -> dict[str, object]:
    return {
        "schema": SQX_BUILDER_SEARCH_SCHEMA,
        "authority": "native_sqx_read_only",
        "source": {
            "source_build": config.source_build,
            "project": "Builder",
            "relative_path": SQX_BUILDER_PROJECT_RELATIVE_PATH,
            "archive_sha256": config.archive_sha256,
            "member": SQX_BUILDER_TASK_ENTRY,
        },
        "selector": config.native.generation_type,
        "display_mode": _search_display_mode(config.native.generation_type),
        "producer_configuration": _native_node_record(config.native.build_mode),
        "semantics": {
            "interpreted_by_tradercockpit": False,
            "owner": "StrategyQuant X",
            "description": (
                "The producer-owned BuildMode structure is reflected read-only. "
                "StrategyQuant X owns Random Discovery, Genetic Evolution, ranking, "
                "selection, mutation, crossover, and search semantics."
            ),
        },
        "execution": {
            "available": False,
            "reason": "native_sqx_builder_owns_search_execution",
        },
    }


def _blocks_configuration_record(config: SqxBuilderProjectConfig) -> dict[str, object]:
    return {
        "schema": SQX_BUILDER_BLOCKS_SCHEMA,
        "authority": "native_sqx_read_only",
        "source": {
            "source_build": config.source_build,
            "project": "Builder",
            "relative_path": SQX_BUILDER_PROJECT_RELATIVE_PATH,
            "archive_sha256": config.archive_sha256,
            "member": SQX_BUILDER_TASK_ENTRY,
        },
        "producer_configuration": _native_node_record(config.native.blocks),
        "semantics": {
            "interpreted_by_tradercockpit": False,
            "owner": "StrategyQuant X",
            "description": (
                "The exact producer-owned Blocks subtree is reflected read-only. "
                "Native tag names, attributes, text, ordering, nesting, block families, "
                "parameter representations, and selection semantics remain StrategyQuant X authority."
            ),
        },
        "execution": {
            "available": False,
            "reason": "native_sqx_builder_owns_block_configuration",
        },
    }


def _rankings_configuration_record(config: SqxBuilderProjectConfig) -> dict[str, object]:
    return {
        "schema": SQX_BUILDER_RANKINGS_SCHEMA,
        "authority": "native_sqx_read_only",
        "source": {
            "source_build": config.source_build,
            "project": "Builder",
            "relative_path": SQX_BUILDER_PROJECT_RELATIVE_PATH,
            "archive_sha256": config.archive_sha256,
            "member": SQX_BUILDER_TASK_ENTRY,
        },
        "producer_configuration": _native_node_record(config.native.rankings),
        "semantics": {
            "interpreted_by_tradercockpit": False,
            "owner": "StrategyQuant X",
            "description": (
                "The exact producer-owned Rankings subtree is reflected read-only. "
                "Native tag names, attributes, text, ordering, nesting, objectives, directions, "
                "thresholds, selection rules, and stop behavior remain StrategyQuant X authority."
            ),
        },
        "execution": {
            "available": False,
            "reason": "native_sqx_builder_owns_ranking_configuration",
        },
    }


def _cross_checks_configuration_record(config: SqxBuilderProjectConfig) -> dict[str, object]:
    return {
        "schema": SQX_BUILDER_CROSS_CHECKS_SCHEMA,
        "authority": "native_sqx_read_only",
        "source": {
            "source_build": config.source_build,
            "project": "Builder",
            "relative_path": SQX_BUILDER_PROJECT_RELATIVE_PATH,
            "archive_sha256": config.archive_sha256,
            "member": SQX_BUILDER_TASK_ENTRY,
        },
        "enabled": config.native.cross_checks_enabled,
        "producer_configuration": _native_node_record(config.native.cross_checks),
        "semantics": {
            "interpreted_by_tradercockpit": False,
            "owner": "StrategyQuant X",
            "description": (
                "The exact producer-owned CrossChecks subtree is reflected read-only. "
                "Native tag names, attributes, text, ordering, nesting, validation methods, "
                "profiles, thresholds, result interpretation, and execution remain StrategyQuant X authority."
            ),
        },
        "execution": {
            "available": False,
            "reason": "native_sqx_builder_owns_cross_check_configuration",
        },
    }


def _money_management_configuration_record(config: SqxBuilderProjectConfig) -> dict[str, object]:
    return {
        "schema": SQX_BUILDER_MONEY_MANAGEMENT_SCHEMA,
        "authority": "native_sqx_read_only",
        "source": {
            "source_build": config.source_build,
            "project": "Builder",
            "relative_path": SQX_BUILDER_PROJECT_RELATIVE_PATH,
            "archive_sha256": config.archive_sha256,
            "member": SQX_BUILDER_TASK_ENTRY,
        },
        "producer_configuration": _native_node_record(config.native.money_management),
        "semantics": {
            "interpreted_by_tradercockpit": False,
            "owner": "StrategyQuant X",
            "description": (
                "The exact producer-owned MoneyManagement subtree is reflected read-only. "
                "Native tag names, attributes, text, ordering, nesting, sizing models, risk and lot semantics, "
                "stop-loss dependencies, compounding behavior, and parameter representations remain StrategyQuant X authority."
            ),
        },
        "execution": {
            "available": False,
            "reason": "native_sqx_builder_owns_money_management_configuration",
        },
    }


def _specification_record(config: SqxBuilderProjectConfig) -> dict[str, object]:
    native = config.native
    data = native.data_setup
    task_source = f"{SQX_BUILDER_PROJECT_RELATIVE_PATH}::{SQX_BUILDER_TASK_ENTRY}"
    requirements = [
        _requirement(
            "strategy_shape", "Strategy shape",
            _state(_present(native.strategy_type) and _present(native.market_sides)),
            required=True,
            detail="StrategyType and MarketSides are present in the exact native Builder task. SQX retains semantic authority and revalidates the task during loadconfig.",
            evidence_path=task_source,
            values={"strategy_type": native.strategy_type, "market_sides": native.market_sides},
        ),
        _requirement(
            "market_identity", "Market identity", "producer_configured", required=True,
            detail="Chart symbol/timeframe and InstrumentInfo are present in the exact Builder archive.",
            evidence_path="user/projects/Builder/project.cfx::{config.xml,Build-Task1.xml}",
            values={
                "charts": [{"symbol": item.symbol, "timeframe": item.timeframe} for item in config.charts],
                "instruments": [item.instrument for item in config.instruments],
            },
        ),
        _requirement(
            "historical_backtest", "Historical backtest setup", _state(_data_setup_configured(native)), required=True,
            detail="One complete native Data setup is present. Values remain opaque exact SQX strings; TraderCockpit does not reinterpret them, and SQX revalidates them during loadconfig.",
            evidence_path=task_source,
            values={
                "setup_count": native.data_setup_count,
                "date_from": data.date_from if data else None,
                "date_to": data.date_to if data else None,
                "test_precision": data.test_precision if data else None,
                "engine": data.engine if data else None,
                "slippage": data.slippage if data else None,
                "min_distance": data.min_distance if data else None,
                "spread": data.spread if data else None,
                "has_commissions": data.has_commissions if data else False,
            },
        ),
        _requirement(
            "trading_options", "Trading assumptions", _state(native.has_build_trading_options), required=True,
            detail="BuildTradingOptions is present in the exact native task. Its producer-owned values are preserved without reinterpretation.",
            evidence_path=task_source,
            values={"section_present": native.has_build_trading_options},
        ),
        _requirement(
            "building_blocks", "Building blocks", _state(native.has_blocks), required=True,
            detail="The native Blocks section is present and is passed to SQX unchanged.",
            evidence_path=task_source,
            values={"section_present": native.has_blocks},
        ),
        _requirement(
            "money_management", "Sizing / money management", _state(native.has_money_management), required=True,
            detail="The native MoneyManagement section is present and is passed to SQX unchanged.",
            evidence_path=task_source,
            values={"section_present": native.has_money_management},
        ),
        _requirement(
            "search_build_mode", "Search / build mode",
            _state(_present(native.generation_type)), required=True,
            detail="A native generationType selection is present in the exact current Builder task. TraderCockpit preserves the opaque producer value and SQX validates its semantics during loadconfig.",
            evidence_path=task_source,
            values={"generation_type": native.generation_type},
        ),
        _requirement(
            "ranking_filters", "Ranking & filters",
            _state(_present(native.max_strategies) and _present(native.stop_condition_type)),
            required=True,
            detail="MaxStrategies and StopCondition are present in the exact native task; SQX owns their quantitative interpretation.",
            evidence_path=task_source,
            values={"max_strategies": native.max_strategies, "stop_condition_type": native.stop_condition_type},
        ),
        _requirement(
            "validation_profile", "Validation profile",
            "producer_configured" if native.cross_checks_enabled else "not_applicable",
            required=native.cross_checks_enabled,
            detail="CrossChecks is conditional. When enabled, the exact native section is preserved for SQX; when disabled or absent, it is not applicable to this Build.",
            evidence_path=task_source,
            values={
                "section_present": native.has_cross_checks,
                "enabled": native.cross_checks_enabled,
            },
        ),
        _requirement(
            "source_provenance", "Source provenance", "producer_configured", required=True,
            detail="Exact native archive identity is preserved as artifact custody, not used as a validity allowlist.",
            evidence_path=SQX_BUILDER_PROJECT_RELATIVE_PATH,
            values={
                "archive_sha256": config.archive_sha256,
                "internal_entries": list(config.internal_entries),
            },
        ),
    ]
    unresolved = [
        item["id"] for item in requirements
        if item["required"] and item["state"] in {"unresolved", "unsupported"}
    ]
    return {
        "schema": SQX_RESEARCH_SPECIFICATION_SCHEMA,
        "authority": "native_sqx_read_only",
        "runtime_trust": {
            "state": "build_verified",
            "source_build": SQX_BUILD,
            "launch_authorization": False,
        },
        "artifact_custody": {
            "state": "exact_snapshot",
            "archive_sha256": config.archive_sha256,
            "internal_entries": list(config.internal_entries),
        },
        "producer_validity": {
            "state": "structurally_valid" if not unresolved else "incomplete",
            "method": "authorized_runtime_native_project_structure",
            "native_execution_check": "loadconfig_before_start",
        },
        "requirements": requirements,
        "build_gate": {
            "locked": bool(unresolved),
            "reason_codes": [f"unresolved:{item}" for item in unresolved],
            "next_authority": (
                "complete_native_builder_configuration"
                if unresolved
                else "compile_review_approve_exact_native_configuration"
            ),
        },
    }


def builder_project_specification_record(config: SqxBuilderProjectConfig) -> dict[str, object]:
    return _specification_record(config)


def builder_project_config_record(sqx_home: Path | str | None) -> dict[str, object]:
    config = read_sqx_builder_project(sqx_home)
    return {
        "schema": SQX_BUILDER_CONFIG_SCHEMA,
        "source_build": config.source_build,
        "project": "Builder",
        "source_relative_path": SQX_BUILDER_PROJECT_RELATIVE_PATH,
        "archive_sha256": config.archive_sha256,
        "internal_entries": list(config.internal_entries),
        "charts": [{"symbol": item.symbol, "timeframe": item.timeframe} for item in config.charts],
        "instruments": [
            {
                "instrument": item.instrument,
                "tick_size": item.tick_size,
                "point_value": item.point_value,
                "data_type": item.data_type,
            }
            for item in config.instruments
        ],
        "preset_binding": {
            "status": SQX_BUILDER_PRESET_BINDING_STATUS,
            "preset_id": None,
            "wiring_allowed": False,
        },
        "search": _search_configuration_record(config),
        "blocks": _blocks_configuration_record(config),
        "rankings": _rankings_configuration_record(config),
        "cross_checks": _cross_checks_configuration_record(config),
        "money_management": _money_management_configuration_record(config),
        "specification": _specification_record(config),
        "execution": {"available": False, "reason": "specification_read_only_no_native_launch"},
    }
