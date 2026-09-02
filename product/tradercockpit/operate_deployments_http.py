"""HTTP-neutral boundary for Operate deployment custody."""

from __future__ import annotations

from tradercockpit.operate_deployments import (
    OperateDeploymentError,
    create_deployment,
    list_current_deployments,
    read_current_deployment,
)
from tradercockpit.operate_exports import OperateExportError
from tradercockpit.research_custody import FileResearchCustodyStore, ResearchCustodyError


OPERATE_DEPLOYMENTS_API_PATH = "/api/operate/deployments"


def _error_response(exc: OperateDeploymentError | OperateExportError | ResearchCustodyError) -> tuple[int, dict[str, object]]:
    code = exc.code
    detail = exc.detail
    if code in {"current_pointer_missing", "operate_deployment_entity_invalid", "operate_export_entity_invalid"}:
        status, error = 404, "not_found"
    elif code in {
        "entity_id_invalid",
        "entity_kind_invalid",
        "revision_ref_invalid",
        "operate_deployment_store_invalid",
    }:
        status, error = 400, "invalid_request"
    else:
        status, error = 409, "invalid_state"
    return status, {"error": error, "reason_code": code, "detail": detail}


def operate_deployments_response(
    research_store: FileResearchCustodyStore | None,
    *,
    entity_id: str | None = None,
) -> tuple[int, dict[str, object]]:
    if research_store is None:
        return 503, {
            "error": "store_not_configured",
            "reason_code": "research_store_not_bound",
            "detail": "Canonical research custody store is not bound.",
        }
    try:
        if entity_id is None:
            return 200, list_current_deployments(research_store)
        return 200, read_current_deployment(research_store, entity_id)
    except (OperateDeploymentError, OperateExportError, ResearchCustodyError) as exc:
        return _error_response(exc)


def operate_deployment_write_response(
    research_store: FileResearchCustodyStore | None,
    payload: dict[str, object],
) -> tuple[int, dict[str, object]]:
    if research_store is None:
        return 503, {
            "error": "store_not_configured",
            "reason_code": "research_store_not_bound",
            "detail": "Canonical research custody store is not bound.",
        }
    required = {"action", "export_entity_id"}
    if set(payload) != required or payload.get("action") != "deploy":
        return 400, {
            "error": "invalid_request",
            "reason_code": "operate_deployment_action_invalid",
            "detail": "Deployment requires only action=deploy and the exact Export entity identity.",
        }
    export_entity_id = payload.get("export_entity_id")
    if not isinstance(export_entity_id, str) or not export_entity_id:
        return 400, {
            "error": "invalid_request",
            "reason_code": "operate_deployment_identity_invalid",
            "detail": "export_entity_id must be a non-empty string.",
        }
    try:
        record = create_deployment(research_store, export_entity_id=export_entity_id)
        return (200 if record.get("reused") else 201), record
    except (OperateDeploymentError, OperateExportError, ResearchCustodyError) as exc:
        return _error_response(exc)
