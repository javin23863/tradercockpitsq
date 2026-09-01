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
    ROBUSTNESS_ATTEMPT_SCHEMA,
    ROBUSTNESS_METHOD_HIGHER_PRECISION,
    ROBUSTNESS_OPERATION,
    ROBUSTNESS_OUTCOME_UNREAD,
    ROBUSTNESS_RECORD_SCHEMA,
    ResearchRobustnessError,
    list_native_robustness_results,
    read_native_robustness_capabilities,
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
    not_found = {"robustness_record_ref_invalid", "robustness_proof_required"}
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


def _verified_robustness_public_record(record: dict[str, object]) -> dict[str, object]:
    """Fail closed if the public robustness receipt is detached from its custody.

    ``research_robustness`` already re-hashes every evidence object and native
    project/result member. This adapter additionally binds the native control
    receipt back to the exact compiled project and installed engine identities
    before the record is exposed through the canonical HTTP command boundary.
    """

    receipts = record.get("receipts")
    proof_entity_id = record.get("proof_entity_id")
    proof_revision = record.get("proof_revision")
    if record.get("schema") == ROBUSTNESS_ATTEMPT_SCHEMA and record.get("state") == "failed":
        launched_states = {"completed", "timeout", "rejected", "invalid_receipt"}
        if (
            not isinstance(record.get("attempt_ref"), str)
            or not record["attempt_ref"].startswith("tc-evidence:sha256:")
            or not isinstance(proof_entity_id, str)
            or not proof_entity_id.startswith("tc-research:proof:v1:")
            or not isinstance(proof_revision, str)
            or not proof_revision.startswith("tc-research-revision:proof:sha256:")
            or not isinstance(record.get("failure_reason_code"), str)
            or not record["failure_reason_code"]
            or type(record.get("partial_side_effect")) is not bool
            or not isinstance(receipts, list)
            or len(receipts) > 1
            or any(not isinstance(item, dict) for item in receipts)
            or any(item.get("action") != "startOnlyTask" or item.get("task") != 1 or item.get("project") != record.get("native_project_name") for item in receipts)
            or any(item.get("project_sha256") is not None and item.get("project_sha256") != record.get("compiled_project_sha256") for item in receipts)
            or any(item.get("engine_sha256") is not None and item.get("engine_sha256") != record.get("engine_sha256") for item in receipts)
            or any(item.get("state") in launched_states and item.get("result_archive_sha256") != record.get("source_result_archive_sha256") for item in receipts)
            or (record["partial_side_effect"] != any(item.get("state") in launched_states for item in receipts))
        ):
            raise ResearchRobustnessError(
                "robustness_record_corrupt",
                "failed native robustness attempt is not bound to durable Proof custody",
            )
        return record

    receipt = receipts[0] if isinstance(receipts, list) and len(receipts) == 1 and isinstance(receipts[0], dict) else None
    if (
        record.get("schema") != ROBUSTNESS_RECORD_SCHEMA
        or record.get("operation") != ROBUSTNESS_OPERATION
        or record.get("method") != ROBUSTNESS_METHOD_HIGHER_PRECISION
        or record.get("execution_state") != "completed"
        or record.get("producer_outcome_state") != ROBUSTNESS_OUTCOME_UNREAD
        or not isinstance(proof_entity_id, str)
        or not proof_entity_id.startswith("tc-research:proof:v1:")
        or not isinstance(proof_revision, str)
        or not proof_revision.startswith("tc-research-revision:proof:sha256:")
        or receipt is None
        or receipt.get("action") != "startOnlyTask"
        or receipt.get("task") != 1
        or receipt.get("state") != "completed"
        or receipt.get("project") != record.get("native_project_name")
        or receipt.get("project_sha256") != record.get("compiled_project_sha256")
        or receipt.get("engine_sha256") != record.get("engine_sha256")
        or receipt.get("launcher_sha256") != record.get("launcher_sha256")
        or receipt.get("result_archive_sha256") != record.get("source_result_archive_sha256")
    ):
        raise ResearchRobustnessError(
            "robustness_record_corrupt",
            "native robustness receipt is not bound to the exact compiled project, engine, launcher, and method custody",
        )
    return record


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
    if action == "read-robustness-capabilities":
        if set(payload) != {"action"}:
            return 400, {
                "error": "invalid_request",
                "reason_code": "robustness_capabilities_invalid",
                "detail": "Robustness capabilities read accepts only action=read-robustness-capabilities.",
            }
        return 200, read_native_robustness_capabilities(sqx_home)

    if action == "list-robustness":
        if set(payload) != {"action"}:
            return 400, {
                "error": "invalid_request",
                "reason_code": "robustness_catalog_invalid",
                "detail": "Robustness catalog read accepts only action=list-robustness.",
            }
        try:
            catalog = list_native_robustness_results(research_store)
            catalog["results"] = [_verified_robustness_public_record(item) for item in catalog["results"]]
            catalog["failed_attempts"] = [_verified_robustness_public_record(item) for item in catalog.get("failed_attempts", [])]
            return 200, catalog
        except ResearchRobustnessError as exc:
            return _robustness_error_response(exc)
        except ResearchCustodyError as exc:
            return 409, {"error": "invalid_state", "reason_code": exc.code, "detail": exc.detail}

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
            record = read_native_robustness_result(research_store, validation_ref)
            return 200, _verified_robustness_public_record(record)
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
            record = start_native_higher_precision(
                research_store,
                sqx_home,
                trusted_launcher_sha256,
                historical_result_entity_id=payload["historical_result_entity_id"],  # type: ignore[arg-type]
                expected_historical_result_revision=payload["expected_historical_result_revision"],  # type: ignore[arg-type]
            )
            return 201, _verified_robustness_public_record(record)
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
            "detail": "Historical Result action must be start-retester or one of the registered robustness read/start actions with its exact identity fields.",
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
