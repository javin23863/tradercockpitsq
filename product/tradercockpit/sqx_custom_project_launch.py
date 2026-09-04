"""Native Custom Project start/stop through the trusted StrategyQuant X launcher.

Official SQX CLI (StrategyQuant X 144 command-line ``-project``):

    sqcli.exe -project action=start name=<Project>
    sqcli.exe -project action=stop name=<Project>

Desktop action ids stay ``run_project`` / ``stop_project``. This module does not
invent MCP, JSON-RPC, loadconfig, or a platform task-loop executor. Builder
loadconfig remains a separate Research custody seam.
"""

from __future__ import annotations

from dataclasses import dataclass
from hashlib import sha256
from pathlib import Path
import re
import subprocess
import time
from threading import Lock
from typing import Callable, Sequence

from tradercockpit.sqx_presets import SQX_BUILD, SqxPresetRuntimeError, verified_sqx_home
from tradercockpit.sqx_runtime import SQX_LAUNCHER_RELATIVE_PATH


SQX_CUSTOM_PROJECT_PROGRESS_SCHEMA = "tc.sqx-custom-project-progress.v1"
SQX_CUSTOM_PROJECT_PROGRESS_API_PATH = "/api/sqx-project-progress"
SQX_CUSTOM_PROJECT_LAUNCH_TIMEOUT_SECONDS = 60.0
SQX_CLI_START_SETTLE_SECONDS = 1.0
SQX_CUSTOM_PROJECT_LOG_SUFFIXES = (".log", ".txt")
SQX_CUSTOM_PROJECT_LOG_MAX_BYTES = 65_536
SQX_CUSTOM_PROJECT_LOG_MAX_LINES_PER_FILE = 80
SQX_CUSTOM_PROJECT_LOG_MAX_LINES = 200
_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")
_CONTROL_LOCK = Lock()
_DESKTOP_TO_NATIVE = {"run_project": "start", "stop_project": "stop"}


class SqxCustomProjectLaunchError(RuntimeError):
    """Structured fail-closed Custom Project launch refusal."""

    def __init__(self, code: str, detail: str) -> None:
        super().__init__(detail)
        self.code = code
        self.detail = detail


@dataclass(frozen=True, slots=True)
class _VerifiedLaunchContext:
    home: Path
    launcher: Path
    launcher_sha256: str
    project: str
    project_file: Path
    project_relative_path: str
    project_sha256: str


def native_project_action(desktop_action: str) -> str:
    native = _DESKTOP_TO_NATIVE.get(desktop_action)
    if native is None:
        raise SqxCustomProjectLaunchError(
            "custom_project_action_invalid",
            "Custom Project control accepts only native start (run_project) or stop (stop_project).",
        )
    return native


def custom_project_worker_label(project: str) -> str:
    return f"sqx-project-start:{project}"


def _trusted_digest(value: str | None) -> str:
    if value is None or not str(value).strip():
        raise SqxCustomProjectLaunchError(
            "trusted_launcher_not_configured",
            "Set SQX_LAUNCHER_SHA256 to the SHA-256 digest of the installed sqcli.exe.",
        )
    normalized = str(value).strip().lower()
    if not _DIGEST_RE.fullmatch(normalized):
        raise SqxCustomProjectLaunchError(
            "trusted_launcher_digest_invalid",
            "SQX_LAUNCHER_SHA256 must be 64 hexadecimal characters.",
        )
    return normalized


def _sha256_file(path: Path) -> str:
    digest = sha256()
    try:
        with path.open("rb") as handle:
            for chunk in iter(lambda: handle.read(1024 * 1024), b""):
                digest.update(chunk)
    except OSError as exc:
        raise SqxCustomProjectLaunchError(
            "sqx_launcher_unreadable",
            "trusted SQX launcher could not be read",
        ) from exc
    return digest.hexdigest()


def _resolve_inside(home: Path, value: Path | str, *, escape_code: str) -> Path:
    candidate = Path(value)
    if not candidate.is_absolute():
        candidate = home / candidate
    try:
        resolved = candidate.resolve()
        resolved.relative_to(home)
    except (OSError, RuntimeError, ValueError) as exc:
        raise SqxCustomProjectLaunchError(
            escape_code,
            "native Custom Project path resolves outside the verified runtime",
        ) from exc
    return resolved


def _verified_home(sqx_home: Path | str | None) -> Path:
    try:
        return verified_sqx_home(sqx_home)
    except SqxPresetRuntimeError as exc:
        raise SqxCustomProjectLaunchError(exc.code, exc.detail) from exc


def _preflight_launcher(
    sqx_home: Path | str | None,
    trusted_launcher_sha256: str | None,
) -> tuple[Path, Path, str]:
    home = _verified_home(sqx_home)
    expected = _trusted_digest(trusted_launcher_sha256)
    launcher = _resolve_inside(home, SQX_LAUNCHER_RELATIVE_PATH, escape_code="sqx_launcher_path_escape")
    if not launcher.is_file():
        raise SqxCustomProjectLaunchError("sqx_launcher_missing", "trusted SQX launcher is missing")
    observed = _sha256_file(launcher)
    if observed != expected:
        raise SqxCustomProjectLaunchError(
            "sqx_launcher_hash_mismatch",
            "configured SQX launcher does not match the trusted identity",
        )
    return home, launcher, observed


def launch_readiness(
    sqx_home: Path | str | None,
    trusted_launcher_sha256: str | None,
    register_worker: Callable[..., None] | None,
) -> dict[str, object]:
    """Read-only launch-path readiness. This is not launch authorization."""

    if register_worker is None or not callable(register_worker):
        try:
            _preflight_launcher(sqx_home, trusted_launcher_sha256)
        except SqxCustomProjectLaunchError as exc:
            return {
                "available": False,
                "reason_code": exc.code,
                "detail": exc.detail,
                "launcher_sha256": None,
            }
        return {
            "available": False,
            "reason_code": "desktop_worker_unregistered",
            "detail": (
                "Custom Project start is a long-lived native process and must register "
                "with the desktop worker supervisor before control returns."
            ),
            "launcher_sha256": None,
        }
    try:
        _home, _launcher, digest = _preflight_launcher(sqx_home, trusted_launcher_sha256)
    except SqxCustomProjectLaunchError as exc:
        return {
            "available": False,
            "reason_code": exc.code,
            "detail": exc.detail,
            "launcher_sha256": None,
        }
    return {
        "available": True,
        "reason_code": None,
        "detail": (
            "Start and stop call the verified StrategyQuant X launcher with the official "
            "sqcli -project action=start|stop name=<project> command. There is no "
            "StrategyQuant X MCP."
        ),
        "launcher_sha256": digest,
    }


def _preflight_project(
    sqx_home: Path | str | None,
    trusted_launcher_sha256: str | None,
    project: str,
    project_relative_path: str,
    expected_project_sha256: str,
) -> _VerifiedLaunchContext:
    home, launcher, launcher_digest = _preflight_launcher(sqx_home, trusted_launcher_sha256)
    archive = _resolve_inside(home, project_relative_path, escape_code="custom_project_path_escape")
    expected = home / project_relative_path
    if archive != expected.resolve() or archive.name != "project.cfx" or archive.parent.name != project:
        raise SqxCustomProjectLaunchError(
            "custom_project_path_escape",
            "SQX Custom Project resolves outside its exact direct user/projects child",
        )
    if not archive.is_file():
        raise SqxCustomProjectLaunchError("custom_project_missing", "SQX Custom Project is missing")
    try:
        observed_project = sha256(archive.read_bytes()).hexdigest()
    except OSError as exc:
        raise SqxCustomProjectLaunchError(
            "custom_project_unreadable",
            "SQX Custom Project could not be read before native launch",
        ) from exc
    if observed_project != expected_project_sha256:
        raise SqxCustomProjectLaunchError(
            "custom_project_hash_mismatch",
            "saved Custom Project archive changed before native launch",
        )
    return _VerifiedLaunchContext(
        home=home,
        launcher=launcher,
        launcher_sha256=launcher_digest,
        project=project,
        project_file=archive,
        project_relative_path=project_relative_path,
        project_sha256=observed_project,
    )


def project_command(context: _VerifiedLaunchContext, native_action: str) -> tuple[str, ...]:
    return (
        str(context.launcher),
        "-project",
        f"action={native_action}",
        f"name={context.project}",
    )


def _receipt(
    native_action: str,
    state: str,
    context: _VerifiedLaunchContext | None,
    project: str,
    *,
    exit_code: int | None,
    reason_code: str | None = None,
) -> dict[str, object]:
    return {
        "sequence": 1,
        "action": native_action,
        "project": project,
        "state": state,
        "exit_code": exit_code,
        "sqx_build": SQX_BUILD,
        "launcher_sha256": context.launcher_sha256 if context else None,
        "project_sha256": context.project_sha256 if context else None,
        "reason_code": reason_code,
    }


def _start_process(
    context: _VerifiedLaunchContext,
    command: tuple[str, ...],
    register_worker: Callable[..., None],
    process_factory: Callable[..., object],
) -> object:
    try:
        process = process_factory(
            list(command),
            cwd=str(context.home),
            stdin=subprocess.DEVNULL,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            shell=False,
        )
    except OSError as exc:
        raise SqxCustomProjectLaunchError(
            "sqx_command_failed",
            "SQX start command could not be executed",
        ) from exc
    try:
        register_worker(process, label=custom_project_worker_label(context.project))
    except Exception as exc:
        terminate = getattr(process, "terminate", None)
        if callable(terminate):
            try:
                terminate()
            except Exception:
                pass
        raise SqxCustomProjectLaunchError(
            "desktop_worker_unregistered",
            "SQX start process could not be registered with the desktop worker supervisor",
        ) from exc
    poll = getattr(process, "poll", None)
    if not callable(poll):
        raise SqxCustomProjectLaunchError(
            "sqx_command_failed",
            "SQX start process does not expose poll()",
        )
    deadline = time.monotonic() + SQX_CLI_START_SETTLE_SECONDS
    exit_code = poll()
    while exit_code is None and time.monotonic() < deadline:
        time.sleep(0.05)
        exit_code = poll()
    if exit_code is not None:
        raise SqxCustomProjectLaunchError(
            "sqx_command_rejected",
            "SQX start command exited before the project stayed running",
        )
    return process


def _run_stop(
    context: _VerifiedLaunchContext,
    command: tuple[str, ...],
    runner: Callable[..., subprocess.CompletedProcess[str]],
    timeout_seconds: float,
) -> int:
    try:
        completed = runner(
            list(command),
            cwd=str(context.home),
            stdin=subprocess.DEVNULL,
            capture_output=True,
            text=True,
            timeout=float(timeout_seconds),
            check=False,
            shell=False,
        )
    except subprocess.TimeoutExpired as exc:
        raise SqxCustomProjectLaunchError("sqx_command_timeout", "SQX stop command timed out") from exc
    except OSError as exc:
        raise SqxCustomProjectLaunchError(
            "sqx_command_failed",
            "SQX stop command could not be executed",
        ) from exc
    if type(completed.returncode) is not int:
        raise SqxCustomProjectLaunchError(
            "sqx_command_failed",
            "SQX command runner returned an invalid exit code",
        )
    if completed.returncode != 0:
        raise SqxCustomProjectLaunchError(
            "sqx_command_rejected",
            "SQX stop command exited nonzero",
        )
    return int(completed.returncode)


def launch_custom_project(
    sqx_home: Path | str | None,
    project: str,
    desktop_action: str,
    *,
    trusted_launcher_sha256: str | None,
    project_relative_path: str,
    expected_project_sha256: str,
    register_worker: Callable[..., None] | None,
    worker_is_active: Callable[[str], bool] | None = None,
    process_factory: Callable[..., object] = subprocess.Popen,
    runner: Callable[..., subprocess.CompletedProcess[str]] = subprocess.run,
    timeout_seconds: float = SQX_CUSTOM_PROJECT_LAUNCH_TIMEOUT_SECONDS,
) -> dict[str, object]:
    """Start or stop one saved Custom Project through the trusted launcher."""

    native_action = native_project_action(desktop_action)
    label = custom_project_worker_label(project)
    with _CONTROL_LOCK:
        if native_action == "start" and callable(worker_is_active) and worker_is_active(label):
            raise SqxCustomProjectLaunchError(
                "native_project_already_running",
                "This Custom Project already has a registered native start process.",
            )
        context = _preflight_project(
            sqx_home,
            trusted_launcher_sha256,
            project,
            project_relative_path,
            expected_project_sha256,
        )
        if native_action == "start":
            if register_worker is None or not callable(register_worker):
                raise SqxCustomProjectLaunchError(
                    "desktop_worker_unregistered",
                    "Custom Project start must register with the desktop worker supervisor.",
                )
            if not callable(process_factory):
                raise SqxCustomProjectLaunchError(
                    "sqx_command_failed",
                    "SQX start process factory is not callable",
                )
        command = project_command(context, native_action)
        if native_action == "start":
            _start_process(context, command, register_worker, process_factory)
            exit_code = None
        else:
            exit_code = _run_stop(context, command, runner, timeout_seconds)

    return {
        "schema": "tc.sqx-custom-project-control.v1",
        "available": True,
        "reason_code": None,
        "detail": (
            "Native StrategyQuant X launcher accepted the official "
            f"action={native_action} command for this saved project."
        ),
        "endpoint_configured": True,
        "credential_configured": False,
        "project": context.project,
        "action": desktop_action,
        "native_action": native_action,
        "state": "submitted",
        "sqx_build": SQX_BUILD,
        "launcher_sha256": context.launcher_sha256,
        "project_relative_path": context.project_relative_path,
        "project_sha256": context.project_sha256,
        "worker_label": label,
        "receipts": [
            _receipt(native_action, "completed", context, context.project, exit_code=exit_code)
        ],
    }


def _iter_log_files(home: Path, project: str) -> Sequence[Path]:
    roots = (
        home / "log",
        home / "user" / "log",
        home / "user" / "projects" / project,
    )
    found: list[Path] = []
    for root in roots:
        try:
            resolved = root.resolve()
            resolved.relative_to(home)
        except (OSError, RuntimeError, ValueError):
            continue
        if not resolved.is_dir():
            continue
        try:
            children = list(resolved.iterdir())
        except OSError:
            continue
        for child in children:
            if child.suffix.lower() not in SQX_CUSTOM_PROJECT_LOG_SUFFIXES:
                continue
            if child.is_symlink():
                continue
            try:
                path = child.resolve()
                path.relative_to(home)
            except (OSError, RuntimeError, ValueError):
                continue
            if not path.is_file() or path.is_symlink():
                continue
            found.append(path)
    return tuple(sorted(found, key=lambda item: item.stat().st_mtime if item.exists() else 0, reverse=True))


def _read_log_lines(path: Path, home: Path) -> list[dict[str, str]]:
    try:
        relative = path.relative_to(home).as_posix()
        size = path.stat().st_size
        with path.open("rb") as handle:
            if size > SQX_CUSTOM_PROJECT_LOG_MAX_BYTES:
                handle.seek(-SQX_CUSTOM_PROJECT_LOG_MAX_BYTES, 2)
            payload = handle.read(SQX_CUSTOM_PROJECT_LOG_MAX_BYTES)
    except OSError:
        return []
    text = payload.decode("utf-8", errors="replace")
    lines = [line.rstrip("\r") for line in text.splitlines() if line.strip()]
    return [
        {"relative_path": relative, "text": line}
        for line in lines[-SQX_CUSTOM_PROJECT_LOG_MAX_LINES_PER_FILE:]
    ]


def read_producer_log_lines(sqx_home: Path | str | None, project: str) -> list[dict[str, str]]:
    """Return recent producer log lines. Missing logs stay empty; counts stay unknown."""

    home = _verified_home(sqx_home)
    lines: list[dict[str, str]] = []
    for path in _iter_log_files(home, project):
        lines.extend(_read_log_lines(path, home))
        if len(lines) >= SQX_CUSTOM_PROJECT_LOG_MAX_LINES:
            return lines[:SQX_CUSTOM_PROJECT_LOG_MAX_LINES]
    return lines
