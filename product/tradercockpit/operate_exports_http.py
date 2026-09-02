"""HTTP-neutral boundary for Operate export custody."""

from __future__ import annotations

from tradercockpit.operate_exports import (
    OperateExportError,
    create_export,
    list_current_exports,
    read_current_export,
)
from tradercockpit.operate_promotions import OperatePromotionError
from tradercockpit.research_custody import FileResearchCustodyStore, ResearchCustodyError


OPERATE_EXPORTS_API_PATH = "/api/operate/exports"


def _error_response(exc: OperateExportError | OperatePromotionError | ResearchCustodyError) -> tuple[int, dict[str, object]]:
    code = exc.code
    detail = exc.detail
    if code in {"current_pointer_missing", "operate_export_entity_invalid", "operate_promotion_entity_invalid"}:
        status, error = 404, "not_found"
    elif code in {
        "entity_id_invalid",
        "entity_kind_invalid",
        "revision_ref_invalid",
        "operate_export_store_invalid",
    }:
        status, error = 400, "invalid_request"
    else:
        status, error = 409, "invalid_state"
    return status, {"error": error, "reason_code": code, "detail": detail}


def operate_exports_response(
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
            return 200, list_current_exports(research_store)
        return 200, read_current_export(research_store, entity_id)
    except (OperateExportError, OperatePromotionError, ResearchCustodyError) as exc:
        return _error_response(exc)


def operate_export_write_response(
    research_store: FileResearchCustodyStore | None,
    payload: dict[str, object],
) -> tuple[int, dict[str, object]]:
    if research_store is None:
        return 503, {
            "error": "store_not_configured",
            "reason_code": "research_store_not_bound",
            "detail": "Canonical research custody store is not bound.",
        }
    required = {"action", "promotion_entity_id"}
    if set(payload) != required or payload.get("action") != "export":
        return 400, {
            "error": "invalid_request",
            "reason_code": "operate_export_action_invalid",
            "detail": "Export requires only action=export and the exact Promotion entity identity.",
        }
    promotion_entity_id = payload.get("promotion_entity_id")
    if not isinstance(promotion_entity_id, str) or not promotion_entity_id:
        return 400, {
            "error": "invalid_request",
            "reason_code": "operate_export_identity_invalid",
            "detail": "promotion_entity_id must be a non-empty string.",
        }
    try:
        record = create_export(research_store, promotion_entity_id=promotion_entity_id)
        return (200 if record.get("reused") else 201), record
    except (OperateExportError, OperatePromotionError, ResearchCustodyError) as exc:
        return _error_response(exc)
