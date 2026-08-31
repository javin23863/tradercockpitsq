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
SQX_BUILDER_PROJECT_RELATIVE_PATH = "user/projects/Builder/project.cfx"
SQX_BUILDER_REQUIRED_ENTRIES = ("config.xml", "Build-Task1.xml")
SQX_BUILDER_PRESET_BINDING_STATUS = "market_proven_preset_unverified"


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
class SqxBuilderProjectConfig:
    archive_path: Path
    archive_sha256: str
    charts: tuple[SqxBuilderChart, ...]
    instruments: tuple[SqxBuilderInstrument, ...]
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


def read_sqx_builder_project(sqx_home: Path | str | None) -> SqxBuilderProjectConfig:
    """Read proven market/timeframe custody from one native Builder snapshot."""

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
    )


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
        "execution": {
            "available": False,
            "reason": "trusted_native_gateway_not_implemented",
        },
    }
