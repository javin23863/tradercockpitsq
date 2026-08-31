"""Read-only custody for the native SQX Builder project configuration.

The module reads only one physically bounded project.cfx snapshot from an exact
SQX 144.2953 runtime and exposes configuration facts present in native XML. It
never infers a project-to-preset relationship or native execution semantics.
"""

from __future__ import annotations

from dataclasses import dataclass
from hashlib import sha256
from io import BytesIO
from pathlib import Path
from typing import Iterable
from xml.etree import ElementTree
from zipfile import BadZipFile, ZipFile

from .sqx_presets import SQX_BUILD, verified_sqx_home


SQX_BUILDER_CONFIG_SCHEMA = "tc.sqx-builder-config.v1"
SQX_RESEARCH_SPECIFICATION_SCHEMA = "tc.research-specification.v1"
SQX_BUILDER_PROJECT_RELATIVE_PATH = "user/projects/Builder/project.cfx"
SQX_BUILDER_REQUIRED_ENTRIES = ("config.xml", "Build-Task1.xml")
SQX_BUILDER_PRESET_BINDING_STATUS = "market_proven_preset_unverified"
_NATIVE_SOURCE_ROOT = "sources/plugins"


class SqxBuilderConfigError(RuntimeError):
    """Raised when native Builder configuration evidence cannot be read exactly."""

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
class SqxBuilderNativeSelections:
    strategy_type: str | None = None
    market_sides: str | None = None
    generation_type: str | None = None
    stop_condition_type: str | None = None
    max_strategies: str | None = None
    data_setup: SqxBuilderDataSetup | None = None
    has_build_trading_options: bool = False
    has_blocks: bool = False
    has_money_management: bool = False
    has_cross_checks: bool = False


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
    except ElementTree.ParseError as exc:
        raise SqxBuilderConfigError(
            "builder_project_xml_invalid",
            f"SQX Builder project entry {entry_name!r} is not valid XML",
        ) from exc


def _iter_named(root: ElementTree.Element, name: str) -> Iterable[ElementTree.Element]:
    for element in root.iter():
        if _local_name(element.tag) == name:
            yield element


def _first_named(root: ElementTree.Element | None, name: str) -> ElementTree.Element | None:
    if root is None:
        return None
    return next(iter(_iter_named(root, name)), None)


def _child_named(root: ElementTree.Element | None, name: str) -> ElementTree.Element | None:
    if root is None:
        return None
    for child in root:
        if _local_name(child.tag) == name:
            return child
    return None


def _dedupe(items: Iterable[object]) -> tuple[object, ...]:
    seen: set[object] = set()
    ordered: list[object] = []
    for item in items:
        if item in seen:
            continue
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
    except BadZipFile as exc:
        raise SqxBuilderConfigError(
            "builder_project_archive_invalid",
            "SQX Builder project.cfx is not a readable native project archive",
        ) from exc


def _native_selections(task_root: ElementTree.Element) -> SqxBuilderNativeSelections:
    what_to_build = _first_named(task_root, "WhatToBuild")
    strategy_type = _child_named(what_to_build, "StrategyType")
    market_sides = _child_named(what_to_build, "MarketSides")
    build_mode = _child_named(what_to_build, "BuildMode")

    data = _first_named(task_root, "Data")
    setups = _child_named(data, "Setups")
    setup = _child_named(setups, "Setup")
    chart = _child_named(setup, "Chart")
    commissions = _child_named(setup, "Commissions")
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
            has_commissions=commissions is not None or bool(setup.attrib.get("commissions")),
        )

    rankings = _first_named(task_root, "Rankings")
    max_strategies = _child_named(rankings, "MaxStrategies")
    stop_condition = _child_named(rankings, "StopCondition")

    options = _first_named(task_root, "Options")
    return SqxBuilderNativeSelections(
        strategy_type=strategy_type.attrib.get("type") if strategy_type is not None else None,
        market_sides=market_sides.attrib.get("type") if market_sides is not None else None,
        generation_type=build_mode.attrib.get("generationType") if build_mode is not None else None,
        stop_condition_type=stop_condition.attrib.get("type") if stop_condition is not None else None,
        max_strategies=(max_strategies.text or "").strip() if max_strategies is not None else None,
        data_setup=data_setup,
        has_build_trading_options=_child_named(options, "BuildTradingOptions") is not None,
        has_blocks=_first_named(task_root, "Blocks") is not None,
        has_money_management=_first_named(task_root, "MoneyManagement") is not None,
        has_cross_checks=_first_named(task_root, "CrossChecks") is not None,
    )


def read_sqx_builder_project(sqx_home: Path | str | None) -> SqxBuilderProjectConfig:
    """Read proven native selections from one exact Builder project snapshot."""

    home = verified_sqx_home(sqx_home)
    archive_path = _resolved_builder_archive(home)
    if not archive_path.is_file():
        raise SqxBuilderConfigError(
            "builder_project_missing",
            f"SQX Builder project is missing: {archive_path}",
        )
    try:
        archive_snapshot = archive_path.read_bytes()
    except OSError as exc:
        raise SqxBuilderConfigError(
            "builder_project_unreadable",
            f"SQX Builder project could not be read: {archive_path}",
        ) from exc

    payloads = _read_project_entries(archive_snapshot)
    roots = tuple(
        _parse_xml(payload, entry_name)
        for entry_name, payload in zip(SQX_BUILDER_REQUIRED_ENTRIES, payloads, strict=True)
    )
    charts = _dedupe(
        SqxBuilderChart(
            symbol=element.attrib.get("symbol", ""),
            timeframe=element.attrib.get("timeframe", ""),
        )
        for root in roots
        for element in _iter_named(root, "Chart")
        if element.attrib.get("symbol") and element.attrib.get("timeframe")
    )
    instruments = _dedupe(
        SqxBuilderInstrument(
            instrument=element.attrib.get("instrument", ""),
            tick_size=element.attrib.get("tickSize"),
            point_value=element.attrib.get("pointValue"),
            data_type=element.attrib.get("dataType"),
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

    return SqxBuilderProjectConfig(
        archive_path=archive_path,
        archive_sha256=sha256(archive_snapshot).hexdigest(),
        charts=charts,  # type: ignore[arg-type]
        instruments=instruments,  # type: ignore[arg-type]
        native=_native_selections(roots[1]),
    )


def _state(selected: bool) -> str:
    return "user_selected" if selected else "unresolved"


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
        "evidence": {
            "source_build": SQX_BUILD,
            "native_source_path": evidence_path,
        },
        "values": values or {},
    }


def _specification_record(config: SqxBuilderProjectConfig) -> dict[str, object]:
    native = config.native
    data = native.data_setup
    data_complete = bool(
        data
        and data.symbol
        and data.timeframe
        and data.spread
        and data.date_from
        and data.date_to
        and data.test_precision
        and data.engine
        and data.slippage
        and data.min_distance
        and data.has_commissions
    )

    requirements = [
        _requirement(
            "strategy_shape",
            "Strategy shape",
            _state(bool(native.strategy_type)),
            required=True,
            detail="Native WhatToBuild requires a recognized StrategyType; template and improve modes add conditional source requirements.",
            evidence_path=f"{_NATIVE_SOURCE_ROOT}/SettingsWhatToBuild/.../WhatToBuildSettingsPlugin.java",
            values={"strategy_type": native.strategy_type, "market_sides": native.market_sides},
        ),
        _requirement(
            "market_identity",
            "Market identity",
            "user_selected",
            required=True,
            detail="Chart symbol/timeframe and InstrumentInfo are present in the exact Builder archive.",
            evidence_path="user/projects/Builder/project.cfx::{config.xml,Build-Task1.xml}",
            values={
                "charts": [{"symbol": item.symbol, "timeframe": item.timeframe} for item in config.charts],
                "instruments": [item.instrument for item in config.instruments],
            },
        ),
        _requirement(
            "historical_backtest",
            "Historical backtest setup",
            _state(data_complete),
            required=True,
            detail="Native Data requires setup dates, precision, engine, slippage, minimum distance, commission configuration, and chart spread/symbol/timeframe.",
            evidence_path=f"{_NATIVE_SOURCE_ROOT}/SettingsData/.../DataSettingsPlugin.java",
            values={
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
            "trading_options",
            "Trading assumptions",
            _state(native.has_build_trading_options),
            required=True,
            detail="SQX parses BuildTradingOptions through its native TradingOptionsList; TraderCockpit does not substitute generic option semantics.",
            evidence_path=f"{_NATIVE_SOURCE_ROOT}/SettingsOptions/.../SettingsOptionsPlugin.java",
        ),
        _requirement(
            "building_blocks",
            "Building blocks",
            _state(native.has_blocks),
            required=True,
            detail="Generation building blocks remain native SQX configuration and must be preserved or explicitly resolved before compilation.",
            evidence_path=f"{_NATIVE_SOURCE_ROOT}/SettingsBlocks/",
        ),
        _requirement(
            "money_management",
            "Sizing / money management",
            _state(native.has_money_management),
            required=True,
            detail="Sizing and money-management configuration remains native SQX state; no TraderCockpit sizing algorithm is inferred.",
            evidence_path=f"{_NATIVE_SOURCE_ROOT}/SettingsMoneyManagement/",
        ),
        _requirement(
            "search_build_mode",
            "Search / build mode",
            _state(bool(native.generation_type)),
            required=True,
            detail="Native WhatToBuild recognizes random-generation or genetic-evolution and rejects unknown generation types.",
            evidence_path=f"{_NATIVE_SOURCE_ROOT}/SettingsWhatToBuild/.../WhatToBuildSettingsPlugin.java",
            values={"generation_type": native.generation_type},
        ),
        _requirement(
            "ranking_filters",
            "Ranking & filters",
            _state(bool(native.max_strategies and native.stop_condition_type)),
            required=True,
            detail="Native Rankings supplies fitness/filter configuration and Build stop conditions; required conditional stop values are interpreted by SQX.",
            evidence_path=f"{_NATIVE_SOURCE_ROOT}/SettingsRankings/.../SettingsRankingsPlugin.java",
            values={
                "max_strategies": native.max_strategies,
                "stop_condition_type": native.stop_condition_type,
            },
        ),
        _requirement(
            "validation_profile",
            "Validation profile",
            "user_selected" if native.has_cross_checks else "not_applicable",
            required=False,
            detail="Cross-check configuration is conditional. Its absence does not cause TraderCockpit to invent a validation profile.",
            evidence_path=f"{_NATIVE_SOURCE_ROOT}/SettingsCrossChecks/",
        ),
        _requirement(
            "source_provenance",
            "Source provenance",
            "user_selected",
            required=True,
            detail="The exact native Builder archive, internal entry names, build identity, and archive hash are retained as source custody.",
            evidence_path=SQX_BUILDER_PROJECT_RELATIVE_PATH,
            values={
                "archive_sha256": config.archive_sha256,
                "internal_entries": list(config.internal_entries),
            },
        ),
    ]

    unresolved = [
        item["id"]
        for item in requirements
        if item["required"] and item["state"] in {"unresolved", "unsupported"}
    ]
    reason_codes = [f"unresolved:{item}" for item in unresolved]
    reason_codes.append("exact_native_configuration_not_compiled")
    return {
        "schema": SQX_RESEARCH_SPECIFICATION_SCHEMA,
        "authority": "native_sqx_read_only",
        "requirements": requirements,
        "build_gate": {
            "locked": True,
            "reason_codes": reason_codes,
            "next_authority": "compile_review_approve_exact_native_configuration",
        },
        "native_defaults": [
            {
                "scope": "WhatToBuild",
                "values": {
                    "market_sides": "both",
                    "entry_symmetry": True,
                    "exit_symmetry": True,
                    "generation_type": "random-generation",
                },
                "status": "proven_default",
                "evidence": f"{_NATIVE_SOURCE_ROOT}/SettingsWhatToBuild/.../WhatToBuildSettingsPlugin.java",
            },
            {
                "scope": "Rankings",
                "values": {"max_strategies": 500, "fitness": "NetProfit", "stop_condition_type": "never"},
                "status": "proven_default",
                "evidence": f"{_NATIVE_SOURCE_ROOT}/SettingsRankings/.../SettingsRankingsPlugin.java",
            },
        ],
    }


def builder_project_config_record(sqx_home: Path | str | None) -> dict[str, object]:
    config = read_sqx_builder_project(sqx_home)
    return {
        "schema": SQX_BUILDER_CONFIG_SCHEMA,
        "source_build": config.source_build,
        "project": "Builder",
        "source_relative_path": SQX_BUILDER_PROJECT_RELATIVE_PATH,
        "archive_sha256": config.archive_sha256,
        "internal_entries": list(config.internal_entries),
        "charts": [
            {"symbol": item.symbol, "timeframe": item.timeframe}
            for item in config.charts
        ],
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
        "specification": _specification_record(config),
        "execution": {
            "available": False,
            "reason": "specification_read_only_no_native_launch",
        },
    }
