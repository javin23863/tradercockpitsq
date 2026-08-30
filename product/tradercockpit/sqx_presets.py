"""Source-bound StrategyQuant X preset catalog for TraderCockpit.

This module intentionally does not import or read repository reference trees.
The checked-in descriptors record reviewed source identities; runtime availability
is established only by validating files in an explicitly configured SQX_HOME.
"""

from __future__ import annotations

from dataclasses import dataclass
from hashlib import sha256
from pathlib import Path
from typing import Iterable


SQX_BUILD = "144.2953"
SQX_REFERENCE_COMMIT = "958e2fe2910cbf71d51ae29e4951484a86fc4ab6"
SQX_PRESET_SCHEMA = "tc.sqx-preset-catalog.v1"


@dataclass(frozen=True, slots=True)
class SqxPresetDescriptor:
    preset_id: str
    label: str
    market: str
    source_relative_path: str
    sha256_hex: str
    source_build: str = SQX_BUILD
    reference_commit: str = SQX_REFERENCE_COMMIT

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


def iter_sqx_presets() -> Iterable[SqxPresetDescriptor]:
    return _PRESETS


def get_sqx_preset(preset_id: str) -> SqxPresetDescriptor:
    if not isinstance(preset_id, str) or not preset_id:
        raise ValueError("presetId must be a non-empty string")
    for descriptor in _PRESETS:
        if descriptor.preset_id == preset_id:
            return descriptor
    raise KeyError(preset_id)


def _resolved_home(value: Path | str | None) -> Path | None:
    if value is None:
        return None
    home = Path(value).expanduser().resolve()
    return home if home.is_dir() else None


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
        }

    path = descriptor.runtime_path(home)
    if not path.is_file():
        return {
            "available": False,
            "status": "preset_missing",
            "verified_sha256": None,
        }

    digest = sha256(path.read_bytes()).hexdigest()
    if digest != descriptor.sha256_hex:
        return {
            "available": False,
            "status": "hash_mismatch",
            "verified_sha256": digest,
        }

    return {
        "available": True,
        "status": "verified",
        "verified_sha256": digest,
    }


def preset_record(
    descriptor: SqxPresetDescriptor,
    sqx_home: Path | str | None,
) -> dict[str, object]:
    runtime = runtime_preset_status(descriptor, sqx_home)
    return {
        "preset_id": descriptor.preset_id,
        "label": descriptor.label,
        "market": descriptor.market,
        "source_build": descriptor.source_build,
        "source_relative_path": descriptor.source_relative_path,
        "source_sha256": descriptor.sha256_hex,
        "reference_commit": descriptor.reference_commit,
        "runtime": runtime,
    }


def preset_catalog(sqx_home: Path | str | None = None) -> dict[str, object]:
    return {
        "schema": SQX_PRESET_SCHEMA,
        "source_build": SQX_BUILD,
        "reference_commit": SQX_REFERENCE_COMMIT,
        "presets": [preset_record(item, sqx_home) for item in _PRESETS],
    }
