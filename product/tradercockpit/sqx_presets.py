"""Read-only native SQX preset/runtime inspection.

This module never launches SQX. It verifies only the configured runtime build and
source-bound preset bytes. Native execution is intentionally unavailable until a
trusted launcher gateway is implemented on the clean product contracts.
"""

from __future__ import annotations

from dataclasses import dataclass
from hashlib import sha256
from pathlib import Path
import re
from typing import Iterable


SQX_BUILD = "144.2953"
SQX_PRESET_SCHEMA = "tc.sqx-preset-catalog.v1"
# Observed on the installed 144.2953 producer: the result archive's ``version.txt`` is the
# archive format version (``1``); the build identity is the ``StrategyFile`` root attribute
# ``AppVersion="SQX Build 144.2953"`` inside ``strategy_Portfolio.xml``.
SQX_RESULT_ARCHIVE_FORMAT_VERSION = "1"
_STRATEGY_FILE_ROOT_RE = re.compile(rb"<StrategyFile\b[^>]*>")
_APP_VERSION_RE = re.compile(rb'\bAppVersion="SQX Build ([0-9][0-9.]*)"')


def sqx_result_archive_build(strategy_xml: bytes) -> str | None:
    """Return the producer build stamped on ``strategy_Portfolio.xml``, or None if absent."""

    root = _STRATEGY_FILE_ROOT_RE.search(strategy_xml)
    if root is None:
        return None
    stamp = _APP_VERSION_RE.search(root.group(0))
    return stamp.group(1).decode("ascii") if stamp else None


class SqxPresetRuntimeError(RuntimeError):
    """Raised when configured SQX runtime evidence cannot be verified exactly."""

    def __init__(self, code: str, detail: str):
        super().__init__(detail)
        self.code = code
        self.detail = detail


@dataclass(frozen=True, slots=True)
class SqxPresetDescriptor:
    preset_id: str
    label: str
    market: str
    source_relative_path: str
    sha256_hex: str
    source_build: str = SQX_BUILD

    def runtime_path(self, sqx_home: Path) -> Path:
        return sqx_home / Path(self.source_relative_path)


_PRESETS = (
    SqxPresetDescriptor(
        preset_id="sqx-default-forex",
        label="Forex",
        market="forex",
        source_relative_path="internal/web/BUILDER/simpleTemplates/DefaultForex.xml",
        sha256_hex="92a7b7cdd6065e0f0f50aa5a5c01a6e4d5123cbe77fbc94fd8083dd9d1007f31",
    ),
    SqxPresetDescriptor(
        preset_id="sqx-default-futures",
        label="Futures",
        market="futures",
        source_relative_path="internal/web/BUILDER/simpleTemplates/DefaultFutures.xml",
        sha256_hex="a792e499205470c832e079647f33e52ce11e3a119a28889819b35e84b93b813b",
    ),
    SqxPresetDescriptor(
        preset_id="sqx-default-stockpicker",
        label="Stocks",
        market="stocks",
        source_relative_path="internal/web/BUILDER/simpleTemplates/DefaultStockpicker.xml",
        sha256_hex="4705d1ec2db13f364803f2ec13e54c6b69cbc55fb3daebd3d882523d97d44268",
    ),
)
_PRESET_BY_ID = {item.preset_id: item for item in _PRESETS}


def iter_sqx_presets() -> Iterable[SqxPresetDescriptor]:
    return _PRESETS


def get_sqx_preset(preset_id: str) -> SqxPresetDescriptor:
    if not isinstance(preset_id, str) or not preset_id:
        raise ValueError("presetId must be a non-empty string")
    try:
        return _PRESET_BY_ID[preset_id]
    except KeyError as exc:
        raise KeyError(preset_id) from exc


def _resolved_home(value: Path | str | None) -> Path | None:
    if value is None:
        return None
    home = Path(value).expanduser().resolve()
    return home if home.is_dir() else None


def _resolved_build_marker(home: Path, relative_path: str) -> Path:
    try:
        path = (home / relative_path).resolve()
        path.relative_to(home)
    except (OSError, RuntimeError, ValueError) as exc:
        raise SqxPresetRuntimeError(
            "sqx_build_marker_path_escape",
            "SQX build marker resolves outside the configured runtime",
        ) from exc
    return path


def _read_build(home: Path) -> str:
    build_path = _resolved_build_marker(home, "internal/web/SQUANT/build.dat")
    version_path = _resolved_build_marker(home, "internal/SQUANT.dat")
    if not build_path.is_file() or not version_path.is_file():
        raise SqxPresetRuntimeError(
            "sqx_build_markers_missing",
            "SQX 144.2953 build markers are missing",
        )
    try:
        build = build_path.read_text(encoding="utf-8").strip()
        version_bytes = version_path.read_bytes()
    except OSError as exc:
        raise SqxPresetRuntimeError(
            "sqx_build_unreadable",
            "SQX build markers could not be read",
        ) from exc
    if len(version_bytes) < 3:
        raise SqxPresetRuntimeError("sqx_build_invalid", "SQX version marker is truncated")
    try:
        major = version_bytes[:3].decode("ascii", errors="strict")
    except UnicodeDecodeError as exc:
        raise SqxPresetRuntimeError("sqx_build_invalid", "SQX version marker is invalid") from exc
    observed = f"{major}.{build}"
    if observed != SQX_BUILD:
        raise SqxPresetRuntimeError(
            "sqx_build_mismatch",
            f"expected SQX {SQX_BUILD}, observed {observed}",
        )
    return observed


def verified_sqx_home(value: Path | str | None) -> Path:
    """Return an exact SQX 144.2953 runtime root or fail closed."""

    home = _resolved_home(value)
    if home is None:
        raise SqxPresetRuntimeError(
            "runtime_not_configured",
            "SQX_HOME is not configured or does not exist",
        )
    _read_build(home)
    return home


def _resolved_preset_path(home: Path, descriptor: SqxPresetDescriptor) -> Path:
    try:
        path = descriptor.runtime_path(home).resolve()
        path.relative_to(home)
    except (OSError, RuntimeError, ValueError) as exc:
        raise SqxPresetRuntimeError(
            "preset_path_escape",
            "SQX preset path resolves outside the verified runtime",
        ) from exc
    return path


def runtime_preset_status(
    descriptor: SqxPresetDescriptor,
    sqx_home: Path | str | None,
) -> dict[str, object]:
    home = _resolved_home(sqx_home)
    if home is None:
        return {
            "available": False,
            "status": "runtime_not_configured",
            "verified_sha256": None,
            "observed_build": None,
        }
    try:
        observed = _read_build(home)
        path = _resolved_preset_path(home, descriptor)
    except SqxPresetRuntimeError as exc:
        return {
            "available": False,
            "status": exc.code,
            "verified_sha256": None,
            "observed_build": None,
        }
    if not path.is_file():
        return {
            "available": False,
            "status": "preset_missing",
            "verified_sha256": None,
            "observed_build": observed,
        }
    try:
        digest = sha256(path.read_bytes()).hexdigest()
    except OSError:
        return {
            "available": False,
            "status": "preset_unreadable",
            "verified_sha256": None,
            "observed_build": observed,
        }
    if digest != descriptor.sha256_hex:
        return {
            "available": False,
            "status": "hash_mismatch",
            "verified_sha256": digest,
            "observed_build": observed,
        }
    return {
        "available": True,
        "status": "verified",
        "verified_sha256": digest,
        "observed_build": observed,
    }


def preset_record(
    descriptor: SqxPresetDescriptor,
    sqx_home: Path | str | None,
) -> dict[str, object]:
    return {
        "preset_id": descriptor.preset_id,
        "label": descriptor.label,
        "market": descriptor.market,
        "source_build": descriptor.source_build,
        "source_relative_path": descriptor.source_relative_path,
        "source_sha256": descriptor.sha256_hex,
        "runtime": runtime_preset_status(descriptor, sqx_home),
    }


def preset_catalog(sqx_home: Path | str | None = None) -> dict[str, object]:
    return {
        "schema": SQX_PRESET_SCHEMA,
        "source_build": SQX_BUILD,
        "execution_available": False,
        "execution_reason": "trusted_native_gateway_not_implemented",
        "presets": [preset_record(item, sqx_home) for item in _PRESETS],
    }
