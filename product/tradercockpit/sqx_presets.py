"""Source-bound StrategyQuant X preset catalog and launch control for TraderCockpit.

Preset identity is pinned to reviewed SQX 144.2953 assets. Builder launch uses the
configured native ``sqcli.exe`` directly; TraderCockpit does not trust or reuse an
unidentified localhost command listener. Because the retained readable SQX archive
intentionally excludes runtime binaries, launcher trust is an explicit operator
boundary: launch remains unavailable until ``SQX_LAUNCHER_SHA256`` matches the
configured executable exactly.
"""

from __future__ import annotations

from dataclasses import dataclass
from hashlib import sha256
import os
from pathlib import Path
import re
import subprocess
from tempfile import TemporaryDirectory
from threading import Lock
from typing import Callable, Iterable, Sequence


SQX_BUILD = "144.2953"
SQX_REFERENCE_COMMIT = "958e2fe2910cbf71d51ae29e4951484a86fc4ab6"
SQX_PRESET_SCHEMA = "tc.sqx-preset-catalog.v1"
SQX_LAUNCHER_SHA256_ENV = "SQX_LAUNCHER_SHA256"
SQX_COMMAND_TIMEOUT_SECONDS = 60.0
_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")


class SqxPresetRuntimeError(RuntimeError):
    """Raised when the configured SQX runtime cannot execute a preset action."""

    def __init__(
        self,
        code: str,
        detail: str,
        *,
        receipts: Sequence[dict[str, object]] = (),
    ):
        super().__init__(detail)
        self.code = code
        self.detail = detail
        self.receipts = tuple(dict(item) for item in receipts)


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
_PRESET_BY_ID = {item.preset_id: item for item in _PRESETS}
_LAUNCH_LOCK = Lock()


def iter_sqx_presets() -> Iterable[SqxPresetDescriptor]:
    return _PRESETS


def get_sqx_preset(preset_id: str) -> SqxPresetDescriptor:
    if not isinstance(preset_id, str) or not preset_id:
        raise ValueError("presetId must be a non-empty string")
    try:
        return _PRESET_BY_ID[preset_id]
    except KeyError as exc:
        raise KeyError(preset_id) from exc


def _sha256_bytes(value: bytes) -> str:
    return sha256(value).hexdigest()


def _sha256_file(path: Path) -> str:
    digest = sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _resolved_home(value: Path | str | None) -> Path | None:
    if value is None:
        return None
    try:
        home = Path(value).expanduser().resolve()
    except (OSError, RuntimeError):
        return None
    return home if home.is_dir() else None


def _read_build(home: Path) -> str:
    build_path = home / "internal/web/SQUANT/build.dat"
    version_path = home / "internal/SQUANT.dat"
    if not build_path.is_file() or not version_path.is_file():
        raise SqxPresetRuntimeError(
            "sqx_build_markers_missing",
            "SQX 144.2953 build markers are missing",
        )
    try:
        build = build_path.read_text(encoding="utf-8").strip()
        version_bytes = version_path.read_bytes()
        if len(version_bytes) < 3:
            raise SqxPresetRuntimeError(
                "sqx_build_invalid",
                "SQX version marker is truncated",
            )
        major = version_bytes[:3].decode("ascii", errors="strict")
    except SqxPresetRuntimeError:
        raise
    except (OSError, UnicodeError) as exc:
        raise SqxPresetRuntimeError(
            "sqx_build_invalid",
            "SQX build markers are unreadable or malformed",
        ) from exc
    if not build.isdigit() or not major.isdigit():
        raise SqxPresetRuntimeError(
            "sqx_build_invalid",
            "SQX build markers are malformed",
        )
    observed = f"{major}.{build}"
    if observed != SQX_BUILD:
        raise SqxPresetRuntimeError(
            "sqx_build_mismatch",
            f"expected SQX {SQX_BUILD}, observed {observed}",
        )
    return observed


def verified_sqx_home(value: Path | str | None) -> Path:
    """Return an SQX 144.2953 runtime root whose readable build markers match."""

    home = _resolved_home(value)
    if home is None:
        raise SqxPresetRuntimeError(
            "runtime_not_configured",
            "SQX_HOME is not configured or does not exist",
        )
    _read_build(home)
    return home


def _expected_launcher_sha256(value: str | None, *, required: bool) -> str | None:
    candidate = value if value is not None else os.environ.get(SQX_LAUNCHER_SHA256_ENV)
    if candidate is None or not candidate.strip():
        if required:
            raise SqxPresetRuntimeError(
                "launcher_identity_unconfigured",
                f"set {SQX_LAUNCHER_SHA256_ENV} to the trusted sqcli.exe SHA-256 before enabling native launch",
            )
        return None
    normalized = candidate.strip().lower()
    if not _DIGEST_RE.fullmatch(normalized):
        raise SqxPresetRuntimeError(
            "launcher_identity_invalid",
            f"{SQX_LAUNCHER_SHA256_ENV} must be exactly 64 hexadecimal characters",
        )
    return normalized


def _launch_runtime_status(
    home: Path | None,
    expected_launcher_sha256: str | None = None,
) -> dict[str, object]:
    base: dict[str, object] = {
        "launch_available": False,
        "launch_status": "runtime_not_configured",
        "launch_detail": "SQX_HOME is not configured",
        "observed_build": None,
        "launcher_sha256": None,
        "launcher_identity_source": None,
    }
    if home is None:
        return base
    try:
        observed_build = _read_build(home)
    except SqxPresetRuntimeError as exc:
        return {
            **base,
            "launch_status": exc.code,
            "launch_detail": exc.detail,
        }

    launcher = home / "sqcli.exe"
    if not launcher.is_file():
        return {
            **base,
            "launch_status": "sqx_launcher_missing",
            "launch_detail": f"SQX launcher is missing: {launcher}",
            "observed_build": observed_build,
        }
    try:
        launcher_hash = _sha256_file(launcher)
    except OSError:
        return {
            **base,
            "launch_status": "sqx_launcher_unreadable",
            "launch_detail": "configured SQX launcher cannot be read",
            "observed_build": observed_build,
        }
    try:
        expected = _expected_launcher_sha256(expected_launcher_sha256, required=False)
    except SqxPresetRuntimeError as exc:
        return {
            **base,
            "launch_status": exc.code,
            "launch_detail": exc.detail,
            "observed_build": observed_build,
            "launcher_sha256": launcher_hash,
            "launcher_identity_source": SQX_LAUNCHER_SHA256_ENV,
        }
    if expected is None:
        return {
            **base,
            "launch_status": "launcher_identity_unconfigured",
            "launch_detail": (
                f"launcher observed as {launcher_hash}; set {SQX_LAUNCHER_SHA256_ENV} "
                "to this separately trusted package identity before native launch"
            ),
            "observed_build": observed_build,
            "launcher_sha256": launcher_hash,
        }
    if launcher_hash != expected:
        return {
            **base,
            "launch_status": "launcher_hash_mismatch",
            "launch_detail": "configured sqcli.exe does not match the trusted launcher identity",
            "observed_build": observed_build,
            "launcher_sha256": launcher_hash,
            "launcher_identity_source": SQX_LAUNCHER_SHA256_ENV,
        }
    return {
        **base,
        "launch_available": True,
        "launch_status": "verified",
        "launch_detail": "Verified SQX build markers and trusted launcher identity.",
        "observed_build": observed_build,
        "launcher_sha256": launcher_hash,
        "launcher_identity_source": SQX_LAUNCHER_SHA256_ENV,
    }


def runtime_preset_status(
    descriptor: SqxPresetDescriptor,
    sqx_home: Path | str | None,
    *,
    expected_launcher_sha256: str | None = None,
) -> dict[str, object]:
    home = _resolved_home(sqx_home)
    launch = _launch_runtime_status(home, expected_launcher_sha256)
    if home is None:
        return {
            "available": False,
            "status": "runtime_not_configured",
            "verified_sha256": None,
            **launch,
        }

    path = descriptor.runtime_path(home)
    if not path.is_file():
        return {
            "available": False,
            "status": "preset_missing",
            "verified_sha256": None,
            **launch,
        }
    try:
        preset_bytes = path.read_bytes()
    except OSError:
        return {
            "available": False,
            "status": "preset_unreadable",
            "verified_sha256": None,
            **launch,
        }
    digest = _sha256_bytes(preset_bytes)
    if digest != descriptor.sha256_hex:
        return {
            "available": False,
            "status": "hash_mismatch",
            "verified_sha256": digest,
            **launch,
        }

    return {
        "available": True,
        "status": "verified",
        "verified_sha256": digest,
        **launch,
    }


def preset_record(
    descriptor: SqxPresetDescriptor,
    sqx_home: Path | str | None,
    *,
    expected_launcher_sha256: str | None = None,
) -> dict[str, object]:
    runtime = runtime_preset_status(
        descriptor,
        sqx_home,
        expected_launcher_sha256=expected_launcher_sha256,
    )
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


def preset_catalog(
    sqx_home: Path | str | None = None,
    *,
    expected_launcher_sha256: str | None = None,
) -> dict[str, object]:
    return {
        "schema": SQX_PRESET_SCHEMA,
        "source_build": SQX_BUILD,
        "reference_commit": SQX_REFERENCE_COMMIT,
        "presets": [
            preset_record(
                item,
                sqx_home,
                expected_launcher_sha256=expected_launcher_sha256,
            )
            for item in _PRESETS
        ],
    }


def builder_commands(
    descriptor: SqxPresetDescriptor,
    sqx_home: Path,
    *,
    preset_path: Path | None = None,
) -> tuple[tuple[str, ...], tuple[str, ...]]:
    """Return direct native CLI argv for one immutable preset snapshot."""

    launcher = str((sqx_home / "sqcli.exe").resolve())
    config = (preset_path or descriptor.runtime_path(sqx_home)).resolve()
    return (
        (
            launcher,
            "-project",
            "action=loadconfig",
            "name=Builder",
            f"file={config}",
        ),
        (launcher, "-project", "action=start", "name=Builder"),
    )


def _command_action(command: Sequence[str]) -> str:
    for item in command:
        if item.startswith("action="):
            return item.split("=", 1)[1]
    return "unknown"


def _failed_command_receipt(
    sequence: int,
    command: Sequence[str],
    *,
    state: str,
    exit_code: int | None,
) -> dict[str, object]:
    return {
        "sequence": sequence,
        "action": _command_action(command),
        "state": state,
        "exit_code": exit_code,
    }


def launch_sqx_preset(
    preset_id: str,
    sqx_home: Path | str | None,
    *,
    expected_launcher_sha256: str | None = None,
    runner: Callable[..., subprocess.CompletedProcess[str]] = subprocess.run,
    timeout_seconds: float = SQX_COMMAND_TIMEOUT_SECONDS,
) -> dict[str, object]:
    """Load one pinned preset and invoke native Builder through direct SQX CLI.

    A successful receipt proves only that the two documented CLI processes exited
    successfully. It does not claim strategy generation, backtest, validation, or
    any later producer result.
    """

    try:
        descriptor = get_sqx_preset(preset_id)
    except KeyError as exc:
        raise SqxPresetRuntimeError(
            "unknown_preset",
            f"unknown SQX preset: {preset_id!r}",
        ) from exc

    home = verified_sqx_home(sqx_home)
    expected = _expected_launcher_sha256(expected_launcher_sha256, required=True)
    launch_status = _launch_runtime_status(home, expected)
    if not launch_status["launch_available"]:
        raise SqxPresetRuntimeError(
            str(launch_status["launch_status"]),
            str(launch_status["launch_detail"]),
        )

    source = descriptor.runtime_path(home)
    if not source.is_file():
        raise SqxPresetRuntimeError("preset_missing", f"SQX preset is missing: {source}")
    try:
        preset_snapshot = source.read_bytes()
    except OSError as exc:
        raise SqxPresetRuntimeError("preset_unreadable", "SQX preset cannot be read") from exc
    snapshot_hash = _sha256_bytes(preset_snapshot)
    if snapshot_hash != descriptor.sha256_hex:
        raise SqxPresetRuntimeError(
            "hash_mismatch",
            f"SQX preset hash mismatch: {snapshot_hash}",
        )

    if not isinstance(timeout_seconds, (int, float)) or isinstance(timeout_seconds, bool) or timeout_seconds <= 0:
        raise ValueError("timeout_seconds must be positive")

    receipts: list[dict[str, object]] = []
    with TemporaryDirectory(prefix="tradercockpit-sqx-preset-") as tmp:
        staged = Path(tmp) / source.name
        try:
            staged.write_bytes(preset_snapshot)
        except OSError as exc:
            raise SqxPresetRuntimeError(
                "preset_staging_failed",
                "verified SQX preset snapshot could not be staged for native load",
            ) from exc
        if _sha256_file(staged) != descriptor.sha256_hex:
            raise SqxPresetRuntimeError(
                "preset_staging_failed",
                "staged SQX preset snapshot changed before native load",
            )
        commands = builder_commands(descriptor, home, preset_path=staged)

        with _LAUNCH_LOCK:
            for sequence, command in enumerate(commands, start=1):
                try:
                    completed = runner(
                        list(command),
                        cwd=str(home),
                        capture_output=True,
                        text=True,
                        timeout=float(timeout_seconds),
                        check=False,
                    )
                except subprocess.TimeoutExpired as exc:
                    failed = _failed_command_receipt(
                        sequence,
                        command,
                        state="timeout",
                        exit_code=None,
                    )
                    raise SqxPresetRuntimeError(
                        "sqx_command_timeout",
                        f"SQX {_command_action(command)} command timed out",
                        receipts=(*receipts, failed),
                    ) from exc
                except OSError as exc:
                    failed = _failed_command_receipt(
                        sequence,
                        command,
                        state="launch_failed",
                        exit_code=None,
                    )
                    raise SqxPresetRuntimeError(
                        "sqx_command_failed",
                        f"SQX {_command_action(command)} command could not be executed",
                        receipts=(*receipts, failed),
                    ) from exc

                if type(completed.returncode) is not int:
                    failed = _failed_command_receipt(
                        sequence,
                        command,
                        state="invalid_receipt",
                        exit_code=None,
                    )
                    raise SqxPresetRuntimeError(
                        "sqx_command_failed",
                        "SQX command runner returned an invalid exit code",
                        receipts=(*receipts, failed),
                    )
                if completed.returncode != 0:
                    failed = _failed_command_receipt(
                        sequence,
                        command,
                        state="rejected",
                        exit_code=int(completed.returncode),
                    )
                    raise SqxPresetRuntimeError(
                        "sqx_command_rejected",
                        f"SQX {_command_action(command)} command exited with code {completed.returncode}",
                        receipts=(*receipts, failed),
                    )
                receipts.append(
                    {
                        "sequence": sequence,
                        "action": _command_action(command),
                        "state": "completed",
                        "exit_code": int(completed.returncode),
                    }
                )

    return {
        "schema": "tc.sqx-preset-launch.v1",
        "preset_id": descriptor.preset_id,
        "market": descriptor.market,
        "sqx_build": SQX_BUILD,
        "source_sha256": descriptor.sha256_hex,
        "launcher_sha256": launch_status["launcher_sha256"],
        "project": "Builder",
        "state": "submitted",
        "control_requests_submitted": len(receipts),
        "receipts": receipts,
    }
