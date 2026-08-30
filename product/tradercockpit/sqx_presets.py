"""Source-bound StrategyQuant X preset catalog and launch control for TraderCockpit.

This module intentionally does not import or read repository reference trees.
The checked-in descriptors record reviewed source identities; runtime availability
is established only by validating files in an explicitly configured SQX_HOME.
"""

from __future__ import annotations

from dataclasses import dataclass
from hashlib import sha256
import http.client
from pathlib import Path
import socket
import subprocess
from threading import Lock
import time
from typing import Callable, Iterable


SQX_BUILD = "144.2953"
SQX_REFERENCE_COMMIT = "958e2fe2910cbf71d51ae29e4951484a86fc4ab6"
SQX_PRESET_SCHEMA = "tc.sqx-preset-catalog.v1"
SQX_COMMAND_HOST = "127.0.0.1"
SQX_COMMAND_PORT = 5050


class SqxPresetRuntimeError(RuntimeError):
    """Raised when the configured SQX runtime cannot execute a preset action."""

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


def _resolved_home(value: Path | str | None) -> Path | None:
    if value is None:
        return None
    home = Path(value).expanduser().resolve()
    return home if home.is_dir() else None


def _read_build(home: Path) -> str:
    build_path = home / "internal/web/SQUANT/build.dat"
    version_path = home / "internal/SQUANT.dat"
    if not build_path.is_file() or not version_path.is_file():
        raise SqxPresetRuntimeError(
            "sqx_build_markers_missing",
            "SQX 144.2953 build markers are missing",
        )
    build = build_path.read_text(encoding="utf-8").strip()
    version_bytes = version_path.read_bytes()
    if len(version_bytes) < 3:
        raise SqxPresetRuntimeError("sqx_build_invalid", "SQX version marker is truncated")
    major = version_bytes[:3].decode("ascii", errors="strict")
    observed = f"{major}.{build}"
    if observed != SQX_BUILD:
        raise SqxPresetRuntimeError(
            "sqx_build_mismatch",
            f"expected SQX {SQX_BUILD}, observed {observed}",
        )
    return observed


def _launch_runtime_status(home: Path | None) -> dict[str, object]:
    if home is None:
        return {
            "launch_available": False,
            "launch_status": "runtime_not_configured",
            "launch_detail": "SQX_HOME is not configured",
            "observed_build": None,
        }
    launcher = home / "sqcli.exe"
    if not launcher.is_file():
        return {
            "launch_available": False,
            "launch_status": "sqx_launcher_missing",
            "launch_detail": f"SQX launcher is missing: {launcher}",
            "observed_build": None,
        }
    try:
        observed = _read_build(home)
    except SqxPresetRuntimeError as exc:
        return {
            "launch_available": False,
            "launch_status": exc.code,
            "launch_detail": exc.detail,
            "observed_build": None,
        }
    return {
        "launch_available": True,
        "launch_status": "verified",
        "launch_detail": "Verified SQX launcher and exact build.",
        "observed_build": observed,
    }


def runtime_preset_status(
    descriptor: SqxPresetDescriptor,
    sqx_home: Path | str | None,
) -> dict[str, object]:
    home = _resolved_home(sqx_home)
    launch = _launch_runtime_status(home)
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

    digest = sha256(path.read_bytes()).hexdigest()
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


def _quote_cli_value(value: str) -> str:
    if any(token in value for token in ('"', "\r", "\n")):
        raise SqxPresetRuntimeError(
            "invalid_runtime_path",
            "SQX runtime path contains unsupported command characters",
        )
    return f'"{value}"'


def builder_commands(descriptor: SqxPresetDescriptor, sqx_home: Path) -> tuple[str, str]:
    preset_path = str(descriptor.runtime_path(sqx_home).resolve())
    return (
        f"-project action=loadconfig name=Builder file={_quote_cli_value(preset_path)}",
        "-project action=start name=Builder",
    )


def _command_channel_ready(
    host: str = SQX_COMMAND_HOST,
    port: int = SQX_COMMAND_PORT,
) -> bool:
    try:
        with socket.create_connection((host, port), timeout=0.25):
            return True
    except OSError:
        return False


def _start_sqx(home: Path) -> subprocess.Popen[bytes]:
    creationflags = getattr(subprocess, "CREATE_NEW_PROCESS_GROUP", 0)
    return subprocess.Popen(
        [str(home / "sqcli.exe"), "-gui"],
        cwd=str(home),
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        creationflags=creationflags,
    )


def _ensure_command_channel(home: Path, *, timeout_seconds: float = 30.0) -> None:
    if _command_channel_ready():
        return
    process = _start_sqx(home)
    deadline = time.monotonic() + timeout_seconds
    while time.monotonic() < deadline:
        if _command_channel_ready():
            return
        if process.poll() is not None:
            raise SqxPresetRuntimeError(
                "sqx_start_failed",
                f"sqcli.exe exited with code {process.returncode}",
            )
        time.sleep(0.25)
    raise SqxPresetRuntimeError(
        "sqx_command_channel_timeout",
        "SQX command channel did not open on 127.0.0.1:5050",
    )


def _post_sqx_command(command: str) -> tuple[int, str]:
    connection = http.client.HTTPConnection(SQX_COMMAND_HOST, SQX_COMMAND_PORT, timeout=15)
    try:
        connection.request(
            "POST",
            "/",
            body=command.encode("utf-8"),
            headers={"Content-Type": "text/plain; charset=utf-8"},
        )
        response = connection.getresponse()
        body = response.read().decode("utf-8", errors="replace")
    except OSError as exc:
        raise SqxPresetRuntimeError(
            "sqx_command_failed",
            f"SQX command channel failed: {exc}",
        ) from exc
    finally:
        connection.close()
    if not 200 <= response.status < 300:
        raise SqxPresetRuntimeError(
            "sqx_command_rejected",
            f"SQX command returned HTTP {response.status}: {body[:240]}",
        )
    return response.status, body


def launch_sqx_preset(
    preset_id: str,
    sqx_home: Path | str | None,
    *,
    ensure_channel: Callable[[Path], None] = _ensure_command_channel,
    post_command: Callable[[str], tuple[int, str]] = _post_sqx_command,
) -> dict[str, object]:
    """Load one exact source-bound preset and submit native Builder start control."""

    try:
        descriptor = get_sqx_preset(preset_id)
    except KeyError as exc:
        raise SqxPresetRuntimeError(
            "unknown_preset",
            f"unknown SQX preset: {preset_id!r}",
        ) from exc

    home = _resolved_home(sqx_home)
    status = runtime_preset_status(descriptor, home)
    if not status["available"]:
        raise SqxPresetRuntimeError(str(status["status"]), "SQX preset is not verified in the configured runtime")
    if not status["launch_available"]:
        raise SqxPresetRuntimeError(
            str(status["launch_status"]),
            str(status["launch_detail"]),
        )
    assert home is not None

    commands = builder_commands(descriptor, home)
    with _LAUNCH_LOCK:
        ensure_channel(home)
        receipts: list[dict[str, object]] = []
        for sequence, command in enumerate(commands, start=1):
            http_status, _ = post_command(command)
            receipts.append({"sequence": sequence, "http_status": http_status})

    return {
        "schema": "tc.sqx-preset-launch.v1",
        "preset_id": descriptor.preset_id,
        "market": descriptor.market,
        "sqx_build": SQX_BUILD,
        "source_sha256": descriptor.sha256_hex,
        "project": "Builder",
        "state": "submitted",
        "control_requests_submitted": len(receipts),
        "receipts": receipts,
    }
