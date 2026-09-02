"""Native Custom Project run/stop custody through the trusted SQX gateway."""

from __future__ import annotations

from pathlib import Path
from threading import Lock
from typing import Callable

from tradercockpit.desktop_lifecycle import OwnedProcess
from tradercockpit.sqx_custom_project import SqxCustomProjectTopologyError, read_sqx_custom_project_topology
from tradercockpit.sqx_gateway import SqxNativeControlGateway, SqxNativeGatewayError
from tradercockpit.sqx_presets import SQX_BUILD, SqxPresetRuntimeError
from tradercockpit.sqx_runtime import sqx_runtime_descriptor


SQX_CUSTOM_PROJECT_CONTROL_SCHEMA = "tc.sqx-custom-project-control.v1"
NATIVE_SCHEDULE_REASON = "native_schedule_action_unavailable"
NATIVE_SCHEDULE_DETAIL = (
    "Native sqcli -project exposes action=start and action=stop only. "
    "SQX has no schedule action at this trusted-launcher seam."
)
_CONTROL_LOCK = Lock()
_ACTIVE_HANDLES: dict[str, OwnedProcess] = {}
_WORKER_REGISTER: Callable[[OwnedProcess, str], None] | None = None


def bind_worker_register(registrar: Callable[[OwnedProcess, str], None] | None) -> None:
    """Optional desktop hook for long-lived native Custom Project workers."""

    global _WORKER_REGISTER
    if registrar is not None and not callable(registrar):
        raise TypeError("worker registrar must be callable")
    _WORKER_REGISTER = registrar


def _execution_available(sqx_home: Path | str | None, trusted_launcher_sha256: str | None) -> tuple[bool, str | None, str]:
    runtime = sqx_runtime_descriptor(sqx_home, trusted_launcher_sha256)
    execution = runtime.get("execution")
    if isinstance(execution, dict) and execution.get("available") is True:
        return True, None, "Verified native gateway and launcher are ready for Custom Project control."
    reason = str(execution.get("reason_code") if isinstance(execution, dict) else "runtime_invalid")
    return False, reason, "Native Custom Project control requires a verified runtime and trusted launcher."


def _sanitize_receipt(payload: dict[str, object]) -> dict[str, object]:
    cleaned = dict(payload)
    cleaned.pop("process", None)
    return cleaned


def _release_handle(project: str) -> None:
    with _CONTROL_LOCK:
        _ACTIVE_HANDLES.pop(project, None)


def _store_handle(project: str, process: OwnedProcess) -> None:
    with _CONTROL_LOCK:
        existing = _ACTIVE_HANDLES.get(project)
        if existing is not None and existing.poll() is None:
            raise SqxNativeGatewayError(
                "custom_project_already_running",
                "native Custom Project control handle is already live for this project",
            )
        _ACTIVE_HANDLES[project] = process


def _live_handle(project: str) -> OwnedProcess | None:
    with _CONTROL_LOCK:
        process = _ACTIVE_HANDLES.get(project)
        if process is None:
            return None
        if process.poll() is not None:
            _ACTIVE_HANDLES.pop(project, None)
            return None
        return process


def custom_project_control_record(
    sqx_home: Path | str | None,
    trusted_launcher_sha256: str | None,
    project: str,
) -> dict[str, object]:
    """Return run/stop availability and live handle state for one exact project."""

    try:
        topology = read_sqx_custom_project_topology(sqx_home, project)
    except SqxCustomProjectTopologyError as exc:
        raise exc

    available, reason_code, detail = _execution_available(sqx_home, trusted_launcher_sha256)
    live = _live_handle(project)
    pid = live.pid if live is not None and isinstance(getattr(live, "pid", None), int) else None
    return {
        "schema": SQX_CUSTOM_PROJECT_CONTROL_SCHEMA,
        "source_build": SQX_BUILD,
        "project": topology.project,
        "project_sha256": topology.archive_sha256,
        "source_relative_path": f"user/projects/{topology.project}/project.cfx",
        "execution": {
            "available": available,
            "reason_code": reason_code,
            "detail": detail,
        },
        "control": {
            "live": live is not None,
            "pid": pid,
            "run_enabled": available and live is None,
            "stop_enabled": live is not None,
        },
        "schedule": {
            "enabled": False,
            "reason_code": NATIVE_SCHEDULE_REASON,
            "detail": NATIVE_SCHEDULE_DETAIL,
        },
    }


def submit_custom_project_control(
    sqx_home: Path | str | None,
    trusted_launcher_sha256: str | None,
    project: str,
    action: str,
) -> dict[str, object]:
    """Run or stop one exact saved native Custom Project through the gateway."""

    available, reason_code, detail = _execution_available(sqx_home, trusted_launcher_sha256)
    if not available:
        raise SqxNativeGatewayError(
            reason_code or "runtime_not_configured",
            detail,
        )

    gateway = SqxNativeControlGateway(sqx_home, trusted_launcher_sha256)
    if action == "run":
        result = gateway.control_custom_project(project, action)
        process = result.pop("process", None)
        if process is None:
            raise SqxNativeGatewayError(
                "sqx_command_failed",
                "native Custom Project run did not return a process handle",
            )
        _store_handle(project, process)
        if _WORKER_REGISTER is not None:
            _WORKER_REGISTER(process, f"sqx-custom-project:{project}")
        cleaned = _sanitize_receipt(result)
        cleaned["control"] = {"live": True, "pid": process.pid, "action": action}
        return cleaned

    if action == "stop":
        live = _live_handle(project)
        try:
            result = gateway.control_custom_project(project, action)
        finally:
            if live is not None and live.poll() is None:
                try:
                    live.terminate()
                except OSError:
                    pass
            _release_handle(project)
        cleaned = _sanitize_receipt(result)
        cleaned["control"] = {"live": False, "pid": None, "action": action}
        return cleaned

    if action == "schedule":
        raise SqxNativeGatewayError(NATIVE_SCHEDULE_REASON, NATIVE_SCHEDULE_DETAIL)

    raise SqxNativeGatewayError(
        "custom_project_action_invalid",
        "native Custom Project control accepts only action=run or action=stop",
    )


def custom_project_control_error_record(exc: SqxNativeGatewayError) -> dict[str, object]:
    return exc.read_model()
