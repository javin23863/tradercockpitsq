"""HTTP-neutral adapter for canonical Research historical-result custody."""

from __future__ import annotations

from pathlib import Path

from tradercockpit.research_custody import FileResearchCustodyStore, ResearchCustodyError
from tradercockpit.research_retester import (
    ResearchRetesterError,
    list_current_historical_results,
    read_current_historical_result,
    start_native_retester,
)
from tradercockpit.research_robustness import (
    ResearchRobustnessError,
    read_native_robustness_result,
    start_native_higher_precision,
)
from tradercockpit.research_trades import ResearchTradesError, read_historical_trades


RESEARCH_HISTORICAL_RESULTS_API_PATH = "/api/research/historical-results"


def _trades_readback(
    research_store: FileResearchCustodyStore,
    historical_result: dict[str, object],
) -> dict[str, object]:
    """Attach optional Trades readback without changing Historical Result validity."""

    if historical_result.get("state") != "completed" or historical_result.get("execution_completed") is not True:
        return {
            "state": "unavailable",
            "reason_code": "historical_trades_result_incomplete",
            "detail": "Trades require one completed native Retester Historical Result.",
        }
    try:
        payload = read_historical_trades(
            research_store,
            historical_result_entity_id=historical_result["entity_id"],  # type: ignore[arg-type]
            expected_historical_result_revision=historical_result["revision"],  # type: ignore[arg-type]
        )
        return {"state": "available", "payload": payload}
    except (ResearchTradesError, ResearchRetesterError, ResearchCustodyError) as exc:
        return {
            "state": "unavailable",
            "reason_code": exc.code,
            "detail": exc.detail,
        }


def historical_results_response(
    research_store: FileResearchCustodyStore | None,
    *,
    entity_id: str | None = None,
    candidate_revision: str | None = None,
) -> tuple[int, dict[str, object]]:
    if research_store is None:
        return 503, {
            "error": "store_not_configured",
            "reason_code": "research_store_not_bound",
            "detail": "Canonical research custody store is not bound.",
        }
    try:
        if entity_id is not None:
            result = read_current_historical_result(research_store, entity_id)
            return 200, {**result, "trades_readback": _trades_readback(research_store, result)}
        return 200, list_current_historical_results(research_store, candidate_revision)
    except ResearchRetesterError as exc:
        status = 409 if exc.code in {"historical_result_content_corrupt", "historical_result_duplicate"} else 400
        return status, {
            "error": "invalid_state" if status == 409 else "invalid_request",
            "reason_code": exc.code,
            "detail": exc.detail,
        }
    except ResearchCustodyError as exc:
        if exc.code == "current_pointer_missing":
            status, error = 404, "not_found"
        elif exc.code in {"entity_id_invalid", "entity_kind_invalid", "revision_ref_invalid"}:
            status, error = 400, "invalid_request"
        else:
            status, error = 409, "invalid_state"
        return status, {"error": error, "reason_code": exc.code, "detail": exc.detail}


def _robustness_error_response(exc: ResearchRobustnessError) -> tuple[int, dict[str, object]]:
    unavailable = {
        "runtime_not_configured",
        "runtime_build_mismatch",
        "runtime_identity_missing",
        "trusted_launcher_not_configured",
        "sqx_launcher_missing",
        "retester_source_project_missing",
        "retester_engine_missing",
        "retester_projects_missing",
    }
    not_found = {"robustness_record_ref_invalid"}
    if exc.code in unavailable:
        status, error = 503, "producer_not_configured"
    elif exc.code in not_found:
        status, error = 404, "not_found"
    else:
        status, error = 409, "invalid_state"
    return status, {
        "error": error,
        "reason_code": exc.code,
        "detail": exc.detail,
    }


def historical_result_write_response(
    research_store: FileResearchCustodyStore | None,
    sqx_home: Path | str | None,
    trusted_launcher_sha256: str | None,
    payload: dict[str, object],
) -> tuple[int, dict[str, object]]:
    if research_store is None:
        return 503, {
            "error": "store_not_configured",
            "reason_code": "research_store_not_bound",
            "detail": "Canonical research custody store is not bound.",
        }

    action = payload.get("action")
    if action == "read-robustness":
        if set(payload) != {"action", "validation_ref"}:
            return 400, {
                "error": "invalid_request",
                "reason_code": "robustness_read_invalid",
                "detail": "Robustness read requires only action=read-robustness and validation_ref.",
            }
        validation_ref = payload.get("validation_ref")
        if not isinstance(validation_ref, str) or not validation_ref:
            return 400, {
                "error": "invalid_request",
                "reason_code": "robustness_record_ref_invalid",
                "detail": "validation_ref must be a non-empty evidence reference.",
            }
        try:
            return 200, read_native_robustness_result(research_store, validation_ref)
        except ResearchRobustnessError as exc:
            return _robustness_error_response(exc)
        except ResearchCustodyError as exc:
            status = 404 if exc.code in {"evidence_missing", "current_pointer_missing"} else 409
            return status, {
                "error": "not_found" if status == 404 else "invalid_state",
                "reason_code": exc.code,
                "detail": exc.detail,
            }

    if action == "start-higher-precision":
        required = {
            "action",
            "historical_result_entity_id",
            "expected_historical_result_revision",
        }
        if set(payload) != required:
            return 400, {
                "error": "invalid_request",
                "reason_code": "robustness_action_invalid",
                "detail": "Higher Precision requires only action=start-higher-precision and exact Historical Result entity/revision identity.",
            }
        if any(
            not isinstance(payload.get(key), str) or not payload[key]
            for key in required - {"action"}
        ):
            return 400, {
                "error": "invalid_request",
                "reason_code": "robustness_source_result_invalid",
                "detail": "Historical Result entity/revision identities must be non-empty strings.",
            }
        try:
            return 201, start_native_higher_precision(
                research_store,
                sqx_home,
                trusted_launcher_sha256,
                historical_result_entity_id=payload["historical_result_entity_id"],  # type: ignore[arg-type]
                expected_historical_result_revision=payload["expected_historical_result_revision"],  # type: ignore[arg-type]
            )
        except ResearchRobustnessError as exc:
            return _robustness_error_response(exc)
        except ResearchCustodyError as exc:
            if exc.code == "current_pointer_missing":
                status, error = 404, "not_found"
            elif exc.code in {"entity_id_invalid", "entity_kind_invalid", "revision_ref_invalid"}:
                status, error = 400, "invalid_request"
            else:
                status, error = 409, "invalid_state"
            return status, {"error": error, "reason_code": exc.code, "detail": exc.detail}

    required = {"action", "candidate_entity_id", "expected_candidate_revision"}
    if set(payload) != required or action != "start-retester":
        return 400, {
            "error": "invalid_request",
            "reason_code": "historical_result_action_invalid",
            "detail": "Historical Result action must be start-retester, start-higher-precision, or read-robustness with its exact identity fields.",
        }
    if any(not isinstance(payload.get(key), str) or not payload[key] for key in required - {"action"}):
        return 400, {
            "error": "invalid_request",
            "reason_code": "historical_result_candidate_invalid",
            "detail": "Candidate entity/revision identities must be non-empty strings.",
        }
    try:
        result = start_native_retester(
            research_store,
            sqx_home,
            trusted_launcher_sha256,
            candidate_entity_id=payload["candidate_entity_id"],
            expected_candidate_revision=payload["expected_candidate_revision"],
        )
        return (200 if result.get("reused") else 201), result
    except ResearchRetesterError as exc:
        unavailable = {
            "runtime_not_configured",
            "runtime_build_mismatch",
            "runtime_identity_missing",
            "trusted_launcher_not_configured",
            "sqx_launcher_missing",
            "retester_source_project_missing",
            "retester_engine_missing",
            "retester_projects_missing",
        }
        status = 503 if exc.code in unavailable else 409
        return status, {
            "error": "producer_not_configured" if status == 503 else "invalid_state",
            "reason_code": exc.code,
            "detail": exc.detail,
        }
    except ResearchCustodyError as exc:
        if exc.code == "current_conflict":
            status, error = 409, "conflict"
        elif exc.code == "current_pointer_missing":
            status, error = 404, "not_found"
        elif exc.code in {"entity_id_invalid", "entity_kind_invalid", "revision_ref_invalid"}:
            status, error = 400, "invalid_request"
        else:
            status, error = 409, "invalid_state"
        return status, {"error": error, "reason_code": exc.code, "detail": exc.detail}
