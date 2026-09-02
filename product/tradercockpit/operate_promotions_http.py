"""HTTP-neutral boundary for Operate promotion custody."""

from __future__ import annotations

from tradercockpit.operate_promotions import (
    OperatePromotionError,
    create_promotion,
    list_current_promotions,
    read_current_promotion,
)
from tradercockpit.research_custody import FileResearchCustodyStore, ResearchCustodyError


OPERATE_PROMOTIONS_API_PATH = "/api/operate/promotions"


def _error_response(exc: OperatePromotionError | ResearchCustodyError) -> tuple[int, dict[str, object]]:
    code = exc.code
    detail = exc.detail
    if code in {"current_pointer_missing", "operate_promotion_entity_invalid"}:
        status, error = 404, "not_found"
    elif code in {
        "entity_id_invalid",
        "entity_kind_invalid",
        "revision_ref_invalid",
        "operate_promotion_store_invalid",
        "research_proof_entity_invalid",
    }:
        status, error = 400, "invalid_request"
    else:
        status, error = 409, "invalid_state"
    return status, {"error": error, "reason_code": code, "detail": detail}


def operate_promotions_response(
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
            return 200, list_current_promotions(research_store)
        return 200, read_current_promotion(research_store, entity_id)
    except (OperatePromotionError, ResearchCustodyError) as exc:
        return _error_response(exc)


def operate_promotion_write_response(
    research_store: FileResearchCustodyStore | None,
    payload: dict[str, object],
) -> tuple[int, dict[str, object]]:
    if research_store is None:
        return 503, {
            "error": "store_not_configured",
            "reason_code": "research_store_not_bound",
            "detail": "Canonical research custody store is not bound.",
        }
    required = {"action", "proof_entity_id"}
    if set(payload) != required or payload.get("action") != "promote":
        return 400, {
            "error": "invalid_request",
            "reason_code": "operate_promotion_action_invalid",
            "detail": "Promotion requires only action=promote and the exact Proof entity identity.",
        }
    proof_entity_id = payload.get("proof_entity_id")
    if not isinstance(proof_entity_id, str) or not proof_entity_id:
        return 400, {
            "error": "invalid_request",
            "reason_code": "operate_promotion_identity_invalid",
            "detail": "proof_entity_id must be a non-empty string.",
        }
    try:
        record = create_promotion(research_store, proof_entity_id=proof_entity_id)
        return (200 if record.get("reused") else 201), record
    except (OperatePromotionError, ResearchCustodyError) as exc:
        return _error_response(exc)
