"""HTTP-neutral adapter for canonical Research historical-result custody."""

from __future__ import annotations

from io import BytesIO
from pathlib import Path
from zipfile import BadZipFile, ZipFile

from tradercockpit.research_custody import FileResearchCustodyStore, ResearchCustodyError
from tradercockpit.research_retester import (
    RETESTER_PROJECT_TASK_ENTRY,
    ResearchRetesterError,
    list_current_historical_results,
    read_current_historical_result,
    start_native_retester,
)
from tradercockpit.research_robustness import (
    ROBUSTNESS_ATTEMPT_SCHEMA,
    ROBUSTNESS_METHOD_ADDITIONAL_MARKETS,
    ROBUSTNESS_METHOD_HIGHER_PRECISION,
    ROBUSTNESS_METHOD_ORDER,
    ROBUSTNESS_METHODS,
    ROBUSTNESS_OPERATION,
    ROBUSTNESS_OUTCOME_UNREAD,
    ROBUSTNESS_RECORD_SCHEMA,
    ROBUSTNESS_START_ACTIONS,
    ResearchRobustnessError,
    list_native_robustness_results,
    read_native_robustness_capabilities,
    read_native_robustness_result,
    start_native_additional_markets,
    start_native_higher_precision,
)
from tradercockpit.sqx_presets import SQX_BUILD
from tradercockpit.research_candidates import ResearchCandidateError, read_candidate_revision
from tradercockpit.research_configurations import ResearchConfigurationError, read_configuration_revision
from tradercockpit.research_custody import EvidenceRef
from tradercockpit.research_proof import ResearchProofError, list_current_research_proofs
from tradercockpit.research_trades import ResearchTradesError, read_historical_trades
from tradercockpit.research_verdicts import (
    ResearchVerdictError,
    cockpit_verdict,
    native_chart_history_ms,
    native_task_sections,
    select_additional_market_trades,
)
from tradercockpit.sqx_databank import parse_sqx_databank
from tradercockpit.sqx_orders import SqxOrdersError, inspect_sqx_orders_bytes


RESEARCH_HISTORICAL_RESULTS_API_PATH = "/api/research/historical-results"


def _native_task_sections_for_result(
    research_store: FileResearchCustodyStore,
    historical_result: dict[str, object],
) -> tuple[dict[str, dict[str, object] | None] | None, str | None]:
    """Read the exact approved Builder task behind this result through custody (candidate → configuration)."""

    try:
        candidate = read_candidate_revision(
            research_store,
            historical_result["candidate_entity_id"],  # type: ignore[arg-type]
            historical_result["candidate_revision"],  # type: ignore[arg-type]
        )
        configuration = read_configuration_revision(
            research_store,
            candidate["configuration_entity_id"],  # type: ignore[arg-type]
            candidate["configuration_revision"],  # type: ignore[arg-type]
        )
        xml_ref = EvidenceRef.parse(configuration["executable_xml_ref"])  # type: ignore[arg-type]
        task_xml = research_store.read_evidence(xml_ref)
        if EvidenceRef.from_bytes(task_xml) != xml_ref:
            return None, "approved native task bytes changed in custody"
        return native_task_sections(task_xml), None
    except (ResearchCandidateError, ResearchConfigurationError, ResearchCustodyError, ResearchVerdictError) as exc:
        return None, f"{exc.code}: {exc.detail}"
    except (KeyError, TypeError):
        return None, "historical result is not bound to an approved configuration"


def _robustness_catalog(
    research_store: FileResearchCustodyStore,
) -> tuple[dict[str, object] | None, str | None]:
    try:
        return list_native_robustness_results(research_store), None
    except (ResearchRobustnessError, ResearchCustodyError) as exc:
        return None, f"{exc.code}: {exc.detail}"


def _robustness_matches(
    catalog: dict[str, object],
    historical_result: dict[str, object],
    method: str,
) -> list[dict[str, object]]:
    return [
        record for record in catalog.get("results", [])  # type: ignore[union-attr]
        if isinstance(record, dict)
        and record.get("method") == method
        and record.get("source_historical_result_revision") == historical_result.get("revision")
    ]


def _pick_robustness_record(matches: list[dict[str, object]]) -> dict[str, object] | None:
    # ponytail: catalog is UUID-filename order, not recency; no recorded timestamp
    return matches[-1] if matches else None


def _robustness_trades_for_result(
    research_store: FileResearchCustodyStore,
    historical_result: dict[str, object],
    method: str,
    catalog: dict[str, object] | None = None,
) -> tuple[list[dict[str, object]] | None, str | None]:
    """Return trade rows of one completed native CrossChecks run of one method."""

    if catalog is None:
        catalog, catalog_error = _robustness_catalog(research_store)
        if catalog is None:
            return None, catalog_error
    record = _pick_robustness_record(_robustness_matches(catalog, historical_result, method))
    if record is None:
        return None, None
    try:
        archive_ref = EvidenceRef.parse(record["result_archive_ref"])  # type: ignore[arg-type]
        snapshot = research_store.read_evidence(archive_ref)
        if archive_ref.digest != record.get("result_archive_sha256") or EvidenceRef.from_bytes(snapshot) != archive_ref:
            return None, f"{method} result archive bytes changed in custody"
        orders = inspect_sqx_orders_bytes(snapshot)
    except (KeyError, TypeError, ResearchCustodyError, SqxOrdersError) as exc:
        code = getattr(exc, "code", "robustness_archive_invalid")
        detail = getattr(exc, "detail", f"{method} result archive is not readable")
        return None, f"{code}: {detail}"
    trades = list(orders["trades"])  # type: ignore[index]
    if method == ROBUSTNESS_METHOD_ADDITIONAL_MARKETS:
        trades = select_additional_market_trades(trades)
    return trades, None


def _settings_xml_from_ref(
    research_store: FileResearchCustodyStore,
    ref: object,
    digest: object,
) -> bytes | None:
    if not isinstance(ref, str) or not ref:
        return None
    try:
        evidence = EvidenceRef.parse(ref)
        payload = research_store.read_evidence(evidence)
    except (ResearchCustodyError, TypeError, ValueError):
        return None
    if isinstance(digest, str) and digest and evidence.digest != digest:
        return None
    if EvidenceRef.from_bytes(payload) != evidence:
        return None
    return payload


def _databank_for_settings(settings_xml: bytes | None) -> list[dict[str, object]] | None:
    rows = parse_sqx_databank(settings_xml)
    return rows or None


def _robustness_databank_for_result(
    research_store: FileResearchCustodyStore,
    historical_result: dict[str, object],
    method: str,
    catalog: dict[str, object] | None = None,
) -> list[dict[str, object]] | None:
    record = _latest_robustness_record(research_store, historical_result, method, catalog=catalog)
    if record is None:
        return None
    settings_xml = _settings_xml_from_ref(
        research_store, record.get("result_settings_ref"), record.get("result_settings_sha256"),
    )
    if settings_xml is None:
        try:
            archive_ref = EvidenceRef.parse(record["result_archive_ref"])  # type: ignore[arg-type]
            snapshot = research_store.read_evidence(archive_ref)
            with ZipFile(BytesIO(snapshot)) as archive:
                settings_xml = archive.read("settings.xml")
        except (KeyError, TypeError, ResearchCustodyError, BadZipFile, OSError, RuntimeError):
            return None
    return _databank_for_settings(settings_xml)


def _higher_precision_trades_for_result(
    research_store: FileResearchCustodyStore,
    historical_result: dict[str, object],
    catalog: dict[str, object] | None = None,
) -> tuple[list[dict[str, object]] | None, str | None]:
    return _robustness_trades_for_result(
        research_store, historical_result, ROBUSTNESS_METHOD_HIGHER_PRECISION, catalog=catalog,
    )


def _latest_robustness_record(
    research_store: FileResearchCustodyStore,
    historical_result: dict[str, object],
    method: str,
    catalog: dict[str, object] | None = None,
) -> dict[str, object] | None:
    if catalog is None:
        catalog, _error = _robustness_catalog(research_store)
        if catalog is None:
            return None
    return _pick_robustness_record(_robustness_matches(catalog, historical_result, method))


def _compiled_cross_checks_for_result(
    research_store: FileResearchCustodyStore,
    historical_result: dict[str, object],
    method: str,
    catalog: dict[str, object] | None = None,
) -> dict[str, object] | None:
    """AcceptanceSettings of the executed isolated snapshot, not the Builder task."""

    record = _latest_robustness_record(research_store, historical_result, method, catalog=catalog)
    if record is None:
        return None
    try:
        compiled_ref = EvidenceRef.parse(record["compiled_project_ref"])  # type: ignore[arg-type]
        compiled = research_store.read_evidence(compiled_ref)
        if compiled_ref.digest != record.get("compiled_project_sha256") or EvidenceRef.from_bytes(compiled) != compiled_ref:
            return None
        with ZipFile(BytesIO(compiled)) as archive:
            task_xml = archive.read(RETESTER_PROJECT_TASK_ENTRY)
        return native_task_sections(task_xml).get("cross_checks")
    except (KeyError, TypeError, ResearchCustodyError, ResearchVerdictError, BadZipFile, OSError, RuntimeError):
        return None


def _proof_count_for_result(research_store: FileResearchCustodyStore, historical_result: dict[str, object]) -> int:
    try:
        catalog = list_current_research_proofs(research_store)
    except (ResearchProofError, ResearchCustodyError):
        return 0
    return sum(
        1 for proof in catalog.get("proofs", [])  # type: ignore[union-attr]
        if isinstance(proof, dict) and proof.get("historical_result_revision") == historical_result.get("revision")
    )


def _cockpit_verdict_readback(
    research_store: FileResearchCustodyStore,
    historical_result: dict[str, object],
    trades_readback: dict[str, object],
) -> dict[str, object]:
    """Attach the cockpit verdict without changing Historical Result validity."""

    if trades_readback.get("state") != "available":
        return {
            "state": "unavailable",
            "reason_code": trades_readback.get("reason_code"),
            "detail": trades_readback.get("detail"),
        }
    trades_payload = trades_readback.get("payload")
    trades = list(trades_payload.get("trades", [])) if isinstance(trades_payload, dict) else []
    sections, conditions_detail = _native_task_sections_for_result(research_store, historical_result)
    catalog, catalog_error = _robustness_catalog(research_store)
    if catalog is None:
        higher_precision, higher_precision_detail = None, catalog_error
        additional_markets, additional_markets_detail = None, catalog_error
        cross_check_runs: dict[str, dict[str, object]] = {}
    else:
        higher_precision, higher_precision_detail = _higher_precision_trades_for_result(
            research_store, historical_result, catalog=catalog,
        )
        additional_markets, additional_markets_detail = _robustness_trades_for_result(
            research_store, historical_result, ROBUSTNESS_METHOD_ADDITIONAL_MARKETS, catalog=catalog,
        )
        cross_check_runs = {}
        for method in ROBUSTNESS_METHOD_ORDER:
            if method in {ROBUSTNESS_METHOD_HIGHER_PRECISION, ROBUSTNESS_METHOD_ADDITIONAL_MARKETS}:
                continue
            trades_for_method, method_detail = _robustness_trades_for_result(
                research_store, historical_result, method, catalog=catalog,
            )
            if trades_for_method is None:
                continue
            cross_check_runs[method] = {
                "trades": trades_for_method,
                "detail": method_detail,
                "cross_checks": _compiled_cross_checks_for_result(
                    research_store, historical_result, method, catalog=catalog,
                ),
                "native_columns": _robustness_databank_for_result(
                    research_store, historical_result, method, catalog=catalog,
                ),
            }
    chart_from_ms, chart_to_ms = None, None
    settings_ref = historical_result.get("result_settings_ref")
    settings_xml = None
    if isinstance(settings_ref, str) and settings_ref:
        try:
            settings_xml = research_store.read_evidence(EvidenceRef.parse(settings_ref))
            chart_from_ms, chart_to_ms = native_chart_history_ms(settings_xml)
        except (ResearchCustodyError, TypeError, ValueError):
            chart_from_ms, chart_to_ms = None, None
            settings_xml = None
    try:
        payload = cockpit_verdict(
            historical_trades=trades,
            higher_precision_trades=higher_precision,
            rankings=sections["rankings"] if sections else None,
            cross_checks=sections["cross_checks"] if sections else None,
            money_management=sections["money_management"] if sections else None,
            proof_count=_proof_count_for_result(research_store, historical_result),
            seed_digest=str(historical_result.get("result_archive_sha256") or historical_result.get("revision") or ""),
            native_conditions_state="available" if sections else "unavailable",
            native_conditions_detail=conditions_detail,
            higher_precision_detail=higher_precision_detail,
            additional_market_trades=additional_markets,
            additional_market_detail=additional_markets_detail,
            additional_market_cross_checks=_compiled_cross_checks_for_result(
                research_store, historical_result, ROBUSTNESS_METHOD_ADDITIONAL_MARKETS, catalog=catalog,
            ) if additional_markets is not None and catalog is not None else None,
            additional_market_native_columns=_robustness_databank_for_result(
                research_store, historical_result, ROBUSTNESS_METHOD_ADDITIONAL_MARKETS, catalog=catalog,
            ) if additional_markets is not None and catalog is not None else None,
            cross_check_runs=cross_check_runs or None,
            native_columns=_databank_for_settings(settings_xml),
            higher_precision_native_columns=_robustness_databank_for_result(
                research_store, historical_result, ROBUSTNESS_METHOD_HIGHER_PRECISION, catalog=catalog,
            ) if higher_precision is not None and catalog is not None else None,
            chart_from_ms=chart_from_ms,
            chart_to_ms=chart_to_ms,
        )
    except ResearchVerdictError as exc:
        return {"state": "unavailable", "reason_code": exc.code, "detail": exc.detail}
    return {
        "state": "available",
        "payload": {
            **payload,
            "historical_result_entity_id": historical_result.get("entity_id"),
            "historical_result_revision": historical_result.get("revision"),
            "result_archive_sha256": historical_result.get("result_archive_sha256"),
        },
    }


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
            trades_readback = _trades_readback(research_store, result)
            return 200, {
                **result,
                "trades_readback": trades_readback,
                "cockpit_verdict": _cockpit_verdict_readback(research_store, result, trades_readback),
            }
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
    payload: dict[str, object] = {
        "error": error,
        "reason_code": exc.code,
        "detail": exc.detail,
    }
    if isinstance(exc.attempt_ref, str) and exc.attempt_ref.startswith("tc-evidence:sha256:"):
        payload["attempt_ref"] = exc.attempt_ref
    return status, payload


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
    if record.get("schema") == ROBUSTNESS_ATTEMPT_SCHEMA and record.get("state") in {"failed", "interrupted"}:
        launched_states = {"completed", "timeout", "rejected", "invalid_receipt"}
        if record.get("state") == "interrupted":
            if (
                not isinstance(record.get("attempt_ref"), str)
                or not record["attempt_ref"].startswith("tc-evidence:sha256:")
                or not isinstance(proof_entity_id, str)
                or not proof_entity_id.startswith("tc-research:proof:v1:")
                or not isinstance(proof_revision, str)
                or not proof_revision.startswith("tc-research-revision:proof:sha256:")
                or record.get("failure_reason_code") != "robustness_attempt_interrupted"
                or record.get("partial_side_effect") is not True
                or record.get("launcher_sha256") is not None
                or receipts != []
                or record.get("method") not in ROBUSTNESS_METHODS
                or record.get("operation") != ROBUSTNESS_OPERATION
                or record.get("sqx_build") != SQX_BUILD
            ):
                raise ResearchRobustnessError(
                    "robustness_record_corrupt",
                    "interrupted native robustness attempt is not bound to durable prepared Proof custody",
                )
            return record
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
            or record.get("method") not in ROBUSTNESS_METHODS
            or record.get("operation") != ROBUSTNESS_OPERATION
            or record.get("sqx_build") != SQX_BUILD
            or any(item.get("state") not in (launched_states | {"preflight_failed", "launch_failed"}) for item in receipts)
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
        or record.get("method") not in ROBUSTNESS_METHODS
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
            failed_attempts = catalog.get("failed_attempts")
            if not isinstance(failed_attempts, list):
                return 409, {
                    "error": "invalid_state",
                    "reason_code": "robustness_catalog_corrupt",
                    "detail": "robustness catalog omitted failed attempts",
                }
            catalog["failed_attempts"] = [_verified_robustness_public_record(item) for item in failed_attempts]
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

    if action in ROBUSTNESS_START_ACTIONS:
        method = ROBUSTNESS_START_ACTIONS[action]
        label = {
            "start-higher-precision": "Higher Precision",
            "start-additional-markets": "Additional Markets",
            "start-monte-carlo-retest": "Monte Carlo retest",
            "start-walk-forward": "Walk-Forward",
            "start-walk-forward-matrix": "Walk-Forward Matrix",
            "start-what-if": "What-If",
            "start-permutation": "System Parameter Permutation",
            "start-monte-carlo-manipulation": "Monte Carlo manipulation",
            "start-sequential-optimization": "Sequential Optimization",
        }[action]
        required = {
            "action",
            "historical_result_entity_id",
            "expected_historical_result_revision",
        }
        if set(payload) != required:
            return 400, {
                "error": "invalid_request",
                "reason_code": "robustness_action_invalid",
                "detail": f"{label} requires only action={action} and exact Historical Result entity/revision identity.",
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
            starter_kwargs = {
                "historical_result_entity_id": payload["historical_result_entity_id"],
                "expected_historical_result_revision": payload["expected_historical_result_revision"],
            }
            if action == "start-additional-markets":
                record = start_native_additional_markets(
                    research_store, sqx_home, trusted_launcher_sha256, **starter_kwargs,  # type: ignore[arg-type]
                )
            elif action == "start-higher-precision":
                record = start_native_higher_precision(
                    research_store, sqx_home, trusted_launcher_sha256, **starter_kwargs,  # type: ignore[arg-type]
                )
            else:
                record = start_native_higher_precision(
                    research_store, sqx_home, trusted_launcher_sha256, method=method, **starter_kwargs,  # type: ignore[arg-type]
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
