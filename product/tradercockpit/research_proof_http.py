"""HTTP-neutral boundary for immutable user-facing Research Proof custody."""

from __future__ import annotations

from tradercockpit.research_custody import FileResearchCustodyStore, ResearchCustodyError
from tradercockpit.research_proof import (
    ResearchProofError,
    create_research_proof,
    list_current_research_proofs,
    read_current_research_proof,
)


RESEARCH_PROOFS_API_PATH = "/api/research/proofs"


def _error_response(exc: ResearchProofError | ResearchCustodyError) -> tuple[int, dict[str, object]]:
    code = exc.code
    detail = exc.detail
    if code == "current_pointer_missing":
        status, error = 404, "not_found"
    elif code in {
        "entity_id_invalid",
        "entity_kind_invalid",
        "revision_ref_invalid",
        "research_proof_entity_invalid",
        "research_proof_idea_invalid",
        "research_proof_historical_result_invalid",
        "research_proof_validation_invalid",
    }:
        status, error = 400, "invalid_request"
    elif code == "research_proof_not_user_proof":
        status, error = 404, "not_found"
    else:
        status, error = 409, "invalid_state"
    return status, {"error": error, "reason_code": code, "detail": detail}


def research_proofs_response(
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
            return 200, list_current_research_proofs(research_store)
        return 200, read_current_research_proof(research_store, entity_id)
    except (ResearchProofError, ResearchCustodyError) as exc:
        return _error_response(exc)


def research_proof_write_response(
    research_store: FileResearchCustodyStore | None,
    payload: dict[str, object],
) -> tuple[int, dict[str, object]]:
    if research_store is None:
        return 503, {
            "error": "store_not_configured",
            "reason_code": "research_store_not_bound",
            "detail": "Canonical research custody store is not bound.",
        }
    required = {
        "action",
        "idea_entity_id",
        "idea_revision",
        "historical_result_entity_id",
        "historical_result_revision",
        "validation_ref",
    }
    if set(payload) != required or payload.get("action") != "create-proof":
        return 400, {
            "error": "invalid_request",
            "reason_code": "research_proof_action_invalid",
            "detail": "Proof creation requires only action=create-proof and exact Idea, Historical Result, and validation identities.",
        }
    if any(not isinstance(payload.get(key), str) or not payload[key] for key in required - {"action"}):
        return 400, {
            "error": "invalid_request",
            "reason_code": "research_proof_identity_invalid",
            "detail": "Proof source identities must be non-empty strings.",
        }
    try:
        record = create_research_proof(
            research_store,
            idea_entity_id=payload["idea_entity_id"],  # type: ignore[arg-type]
            idea_revision=payload["idea_revision"],  # type: ignore[arg-type]
            historical_result_entity_id=payload["historical_result_entity_id"],  # type: ignore[arg-type]
            historical_result_revision=payload["historical_result_revision"],  # type: ignore[arg-type]
            validation_ref=payload["validation_ref"],  # type: ignore[arg-type]
        )
        return (200 if record.get("reused") else 201), record
    except (ResearchProofError, ResearchCustodyError) as exc:
        return _error_response(exc)
