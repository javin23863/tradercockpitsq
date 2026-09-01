"""HTTP-neutral adapter for native SQX robustness execution/readback."""

from __future__ import annotations

from pathlib import Path

from tradercockpit.research_custody import FileResearchCustodyStore, ResearchCustodyError
from tradercockpit.research_robustness import (
    ResearchRobustnessError,
    read_native_robustness_result,
    start_native_higher_precision,
)


RESEARCH_ROBUSTNESS_API_PATH = "/api/research/robustness"


def robustness_response(
    research_store: FileResearchCustodyStore | None,
    *,
    validation_ref: str | None,
) -> tuple[int, dict[str, object]]:
    if research_store is None:
        return 503, {
            "error": "store_not_configured",
            "reason_code": "research_store_not_bound",
            "detail": "Canonical research custody store is not bound.",
        }
    if not isinstance(validation_ref, str) or not validation_ref:
        return 400, {
            "error": "invalid_request",
            "reason_code": "robustness_record_ref_invalid",
            "detail": "Robustness read requires one non-empty validationRef.",
        }
    try:
        return 200, read_native_robustness_result(research_store, validation_ref)
    except ResearchRobustnessError as exc:
        status = 404 if exc.code in {"robustness_record_ref_invalid"} else 409
        return status, {
            "error": "not_found" if status == 404 else "invalid_state",
            "reason_code": exc.code,
            "detail": exc.detail,
        }
    except ResearchCustodyError as exc:
        status = 404 if exc.code in {"evidence_missing", "current_pointer_missing"} else 409
        return status, {
            "error": "not_found" if status == 404 else "invalid_state",
            "reason_code": exc.code,
            "detail": exc.detail,
        }


def robustness_write_response(
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
    required = {
        "action",
        "historical_result_entity_id",
        "expected_historical_result_revision",
    }
    if set(payload) != required or payload.get("action") != "start-higher-precision":
        return 400, {
            "error": "invalid_request",
            "reason_code": "robustness_action_invalid",
            "detail": "Higher Precision start requires only action=start-higher-precision and exact Historical Result entity/revision identity.",
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
        if exc.code == "current_pointer_missing":
            status, error = 404, "not_found"
        elif exc.code in {"entity_id_invalid", "entity_kind_invalid", "revision_ref_invalid"}:
            status, error = 400, "invalid_request"
        else:
            status, error = 409, "invalid_state"
        return status, {
            "error": error,
            "reason_code": exc.code,
            "detail": exc.detail,
        }
