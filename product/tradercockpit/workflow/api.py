"""HTTP-neutral API contract for TraderCockpit workflow automation."""

from __future__ import annotations

from pathlib import Path
from typing import Any, Mapping

from tradercockpit.domain import ContentAddress
from tradercockpit.sqx_custom_project import (
    SqxCustomProjectTopologyError,
    read_sqx_custom_project_topology,
)
from tradercockpit.storage import ContentStoreError

from .model import WorkflowError, WorkflowPlanV1
from .service import WorkflowActionHandler, WorkflowRunService
from .sqx_import import workflow_plan_from_sqx_topology


WORKFLOW_START_API_PATH = "/api/workflows/runs"
WORKFLOW_READ_API_PATH = "/api/workflows/runs/read"
WORKFLOW_LIST_API_PATH = "/api/workflows/runs/list"
WORKFLOW_IMPORT_SQX_API_PATH = "/api/workflows/import-sqx"


def _service(
    state_root: Path | str | None,
    handlers: Mapping[str, WorkflowActionHandler] | None,
) -> WorkflowRunService:
    if state_root is None:
        raise FileNotFoundError("TraderCockpit state root is not configured")
    return WorkflowRunService(state_root, handlers=handlers)


def workflow_start_response(
    state_root: Path | str | None,
    request: object,
    *,
    handlers: Mapping[str, WorkflowActionHandler] | None = None,
) -> tuple[int, dict[str, Any]]:
    try:
        if not isinstance(request, Mapping):
            raise WorkflowError("request body must be an object")
        allowed = {"plan", "runKey", "inputs"}
        unknown = sorted(set(request) - allowed)
        if unknown:
            raise WorkflowError("unknown request fields: " + ", ".join(unknown))
        plan = WorkflowPlanV1.from_payload(request.get("plan", {}))
        run_key = request.get("runKey")
        inputs = request.get("inputs", {})
        if not isinstance(inputs, Mapping):
            raise WorkflowError("inputs must be an object")
        result = _service(state_root, handlers).start(
            plan,
            run_key=run_key,
            inputs=dict(inputs),
        )
        return 201, result
    except FileNotFoundError as exc:
        return 503, {"error": "producer_not_configured", "detail": str(exc)}
    except (WorkflowError, ValueError, TypeError) as exc:
        return 400, {"error": "invalid_request", "detail": str(exc)}
    except ContentStoreError as exc:
        return 409, {"error": "invalid_state", "detail": str(exc)}


def workflow_read_response(
    state_root: Path | str | None,
    run_ref_text: str,
    *,
    handlers: Mapping[str, WorkflowActionHandler] | None = None,
) -> tuple[int, dict[str, Any]]:
    try:
        ref = ContentAddress.parse(run_ref_text)
        if ref.kind != "workflow-run":
            raise WorkflowError("runRef must reference workflow-run")
        return 200, _service(state_root, handlers).read(ref)
    except FileNotFoundError as exc:
        return 503, {"error": "producer_not_configured", "detail": str(exc)}
    except KeyError:
        return 404, {"error": "not_found", "detail": "Workflow run was not found"}
    except (WorkflowError, ValueError, TypeError) as exc:
        return 400, {"error": "invalid_request", "detail": str(exc)}
    except ContentStoreError as exc:
        return 409, {"error": "invalid_state", "detail": str(exc)}


def workflow_list_response(
    state_root: Path | str | None,
    *,
    handlers: Mapping[str, WorkflowActionHandler] | None = None,
) -> tuple[int, dict[str, Any]]:
    try:
        runs = _service(state_root, handlers).list_runs()
        return 200, {"schema": "tc.workflow-run-list.v1", "runs": list(runs)}
    except FileNotFoundError as exc:
        return 503, {"error": "producer_not_configured", "detail": str(exc)}
    except (WorkflowError, ContentStoreError, ValueError, TypeError) as exc:
        return 409, {"error": "invalid_state", "detail": str(exc)}


def workflow_import_sqx_response(
    sqx_home: Path | str | None,
    request: object,
) -> tuple[int, dict[str, Any]]:
    try:
        if not isinstance(request, Mapping):
            raise WorkflowError("request body must be an object")
        allowed = {"project", "gotoTargets", "maxSteps"}
        unknown = sorted(set(request) - allowed)
        if unknown:
            raise WorkflowError("unknown request fields: " + ", ".join(unknown))
        project = request.get("project")
        if not isinstance(project, str) or not project.strip():
            raise WorkflowError("project must be a non-empty string")
        goto_targets = request.get("gotoTargets", {})
        if not isinstance(goto_targets, Mapping):
            raise WorkflowError("gotoTargets must be an object")
        topology = read_sqx_custom_project_topology(sqx_home, project)
        plan = workflow_plan_from_sqx_topology(
            topology,
            goto_targets={str(key): str(value) for key, value in goto_targets.items()},
            max_steps=request.get("maxSteps", 1000),
        )
        return 200, {
            "schema": "tc.workflow-import.v1",
            "plan_ref": str(plan.ref),
            "plan": plan.identity_payload(),
            "source": {
                "kind": "sqx-custom-project",
                "project": topology.project,
                "archive_sha256": topology.archive_sha256,
                "source_build": topology.source_build,
            },
        }
    except SqxCustomProjectTopologyError as exc:
        return 409, {"error": exc.code, "detail": exc.detail}
    except (WorkflowError, ValueError, TypeError) as exc:
        return 400, {"error": "invalid_request", "detail": str(exc)}
