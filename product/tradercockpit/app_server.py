"""Canonical application server for the TraderCockpit desktop."""

from __future__ import annotations

import argparse
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from ipaddress import ip_address
import json
import os
from pathlib import Path
import socket
from urllib.parse import parse_qs, urlsplit

from tradercockpit.app_data import resolve_application_data_root
from tradercockpit.desktop_lifecycle import DEFAULT_WORKER_STOP_TIMEOUT_SECONDS, DesktopWorkerSupervisor
from tradercockpit.assistant import ASSISTANT_API_PATH, assistant_reply, assistant_status_record
from tradercockpit.assistant_voice import (
    ASSISTANT_TRANSCRIBE_API_PATH,
    MAX_AUDIO_BYTES,
    transcribe_response,
)
from tradercockpit.capability_registry import (
    REGISTRY_API_PATH,
    CapabilityRegistryError,
    capability_registry_record,
    stage_addon,
)
from tradercockpit.desktop_session import (
    DESKTOP_SESSION_API_PATH,
    DesktopSessionError,
    read_desktop_session,
    write_desktop_session,
)
from tradercockpit.research_candidates import (
    ResearchCandidateError,
    bind_ml_model,
    import_native_candidate,
    list_current_candidates,
    read_current_candidate,
)
from tradercockpit.research_configurations import (
    ResearchConfigurationError,
    approve_configuration,
    compile_current_builder_configuration,
    list_current_configurations,
    read_current_configuration,
)
from tradercockpit.research_custody import FileResearchCustodyStore, ResearchCustodyError
from tradercockpit.research_ideas import (
    ResearchIdeaError,
    create_idea,
    list_current_ideas,
    read_current_idea,
    revise_idea,
)
from tradercockpit.research_native_jobs import (
    ResearchNativeJobError,
    launch_approved_builder_configuration,
    list_current_native_jobs,
    read_current_native_job,
)
from tradercockpit.market_data import (
    market_bars_record,
    market_quotes_record,
    watchlist_from_env,
)
from tradercockpit.research_clarifying_questions import (
    RESEARCH_CLARIFYING_QUESTIONS_API_PATH,
    clarifying_questions_response,
    clarifying_questions_write,
)
from tradercockpit.research_next_action import (
    RESEARCH_NEXT_ACTION_API_PATH,
    research_next_action_record,
)
from tradercockpit.research_source_ingest import (
    RESEARCH_IDEA_INGEST_API_PATH,
    research_idea_ingest_write,
)
from tradercockpit.research_proof_http import (
    RESEARCH_PROOFS_API_PATH,
    research_proof_write_response,
    research_proofs_response,
)
from tradercockpit.research_models import (
    RESEARCH_MODELS_API_PATH,
    models_catalog,
    models_write,
)
from tradercockpit.research_retester_http import (
    RESEARCH_HISTORICAL_RESULTS_API_PATH,
    historical_result_write_response,
    historical_results_response,
)
from tradercockpit.live_producers import LIVE_PRODUCERS_API_PATH, live_producers_record
from tradercockpit.runtime_status import runtime_status_record
from tradercockpit.sqx_builder_config import (
    SqxBuilderConfigError,
    builder_project_config_record,
)
from tradercockpit.sqx_custom_project import (
    SQX_CUSTOM_PROJECT_CONTROL_API_PATH,
    SQX_CUSTOM_PROJECT_RESULTS_API_PATH,
    SQX_CUSTOM_PROJECTS_API_PATH,
    SqxCustomProjectControlError,
    SqxCustomProjectTopologyError,
    custom_project_control,
    custom_project_progress_record,
    custom_project_topology_record,
    list_custom_project_results,
    list_custom_projects,
)
from tradercockpit.sqx_custom_project_launch import SQX_CUSTOM_PROJECT_PROGRESS_API_PATH
from tradercockpit.sqx_calibrate import (
    SQX_CALIBRATE_API_PATH,
    calibrate_indicators,
)
from tradercockpit.sqx_custom_project_settings import (
    SQX_CUSTOM_PROJECT_SETTINGS_API_PATH,
    update_custom_project_settings,
)
from tradercockpit.sqx_custom_project_strategy import (
    SQX_CUSTOM_PROJECT_STRATEGY_API_PATH,
    inspect_custom_project_strategy,
)
from tradercockpit.sqx_results_plugins import (
    SQX_RESULTS_PLUGIN_API_PATH,
    SQX_RESULTS_PLUGIN_CREATE_API_PATH,
    create_results_plugin,
    read_results_plugin_file,
)
from tradercockpit.sqx_results_overview import (
    OVERVIEW_QUERY,
    SQX_OVERVIEW_API_PATH,
    overview_html,
)
from tradercockpit.sqx_results_chart import (
    CHART_QUERY,
    SQX_RESULTS_CHART_API_PATH,
    results_chart,
)
from tradercockpit.sqx_sourcecode import (
    SQX_SOURCECODE_API_PATH,
    print_sourcecode,
    save_ea,
    save_mt_paths,
    sourcecode_catalog_record,
    sourcecode_data_path,
)
from tradercockpit.sqx_native_web import SqxNativeWebError
from tradercockpit.sqx_run_module import (
    SQX_RUN_MODULE_API_PATH,
    SqxRunModuleError,
    read_sqx_run_module,
)
from tradercockpit.sqx_outputs import discover_sqx_outputs
from tradercockpit.sqx_presets import (
    SqxPresetRuntimeError,
    get_sqx_preset,
    preset_catalog,
    preset_record,
)
from tradercockpit.sqx_runtime import SQX_LAUNCHER_SHA256_ENV


STATUS_API_PATH = "/api/status"
MARKET_QUOTES_API_PATH = "/api/market/quotes"
MARKET_BARS_API_PATH = "/api/market/bars"
CAPABILITIES_API_PATH = REGISTRY_API_PATH
RESEARCH_IDEAS_API_PATH = "/api/research/ideas"
RESEARCH_CONFIGURATIONS_API_PATH = "/api/research/configurations"
RESEARCH_NATIVE_JOBS_API_PATH = "/api/research/native-jobs"
RESEARCH_CANDIDATES_API_PATH = "/api/research/candidates"
SQX_PRESETS_API_PATH = "/api/sqx-presets"
SQX_OUTPUTS_API_PATH = "/api/sqx-outputs"
SQX_BUILDER_CONFIG_API_PATH = "/api/sqx-builder-config"
SQX_PROJECT_TOPOLOGY_API_PATH = "/api/sqx-project-topology"
MAX_JSON_BODY_BYTES = 256_000
_DEFAULT_WEB_ROOT = Path(__file__).resolve().parents[2] / "web"


def _is_loopback_address(value: str) -> bool:
    try:
        return ip_address(value.split("%", 1)[0]).is_loopback
    except (AttributeError, ValueError):
        return False


def status_response(
    sqx_home: Path | str | None,
    trusted_launcher_sha256: str | None = None,
    research_store: FileResearchCustodyStore | None = None,
) -> tuple[int, dict[str, object]]:
    return 200, runtime_status_record(
        sqx_home,
        trusted_launcher_sha256,
        research_store_bound=research_store is not None,
        data_root=research_store.root if research_store is not None else None,
    )


def capabilities_response(
    research_store: FileResearchCustodyStore | None,
    sqx_home: Path | str | None = None,
) -> tuple[int, dict[str, object]]:
    root = research_store.root if research_store is not None else None
    return 200, capability_registry_record(root, sqx_home)


_RUNTIME_STAGE_CODES = frozenset(
    {
        "runtime_not_configured",
        "sqx_build_mismatch",
        "sqx_build_markers_missing",
        "sqx_build_unreadable",
        "sqx_build_invalid",
        "sqx_build_marker_path_escape",
        "native_plugin_catalog_invalid",
    }
)


def capabilities_stage_response(
    sqx_home: Path | str | None,
    payload: dict[str, object],
) -> tuple[int, dict[str, object]]:
    if set(payload) != {"action", "id"} or payload.get("action") != "stage":
        return 400, {
            "error": "invalid_request",
            "reason_code": "capability_action_invalid",
            "detail": "Install accepts only action stage and one packaged plugin id. Plugin settings stay in StrategyQuant X.",
        }
    plugin_id = payload.get("id")
    if not isinstance(plugin_id, str) or not plugin_id:
        return 400, {
            "error": "invalid_request",
            "reason_code": "capability_action_invalid",
            "detail": "Install requires one packaged plugin id.",
        }
    try:
        result = stage_addon(plugin_id, sqx_home)
    except CapabilityRegistryError as exc:
        if exc.code in _RUNTIME_STAGE_CODES:
            return 503, {
                "error": "producer_not_configured",
                "reason_code": exc.code,
                "detail": exc.detail,
            }
        if exc.code == "native_plugin_unknown":
            return 404, {"error": "not_found", "reason_code": exc.code, "detail": exc.detail}
        return 409, {"error": "invalid_state", "reason_code": exc.code, "detail": exc.detail}
    return 200, {
        "schema": "tc.capability-addon-stage.v1",
        "id": result["id"],
        "installed": result["installed"],
        "native_placement": result["native_placement"],
        "detail": result["detail"],
    }


def desktop_session_response(research_store: FileResearchCustodyStore | None) -> tuple[int, dict[str, object]]:
    root = research_store.root if research_store is not None else None
    return 200, read_desktop_session(root)


def desktop_session_write_response(
    research_store: FileResearchCustodyStore | None,
    payload: dict[str, object],
) -> tuple[int, dict[str, object]]:
    if research_store is None:
        return 503, {
            "error": "unavailable",
            "reason_code": "session_store_unbound",
            "detail": "Desktop session persistence requires the application data root.",
        }
    if not isinstance(payload, dict) or set(payload) != {"path"} or not isinstance(payload.get("path"), str):
        return 400, {
            "error": "invalid_request",
            "reason_code": "desktop_path_invalid",
            "detail": "session write accepts only a registered path",
        }
    try:
        return 200, write_desktop_session(research_store.root, payload["path"])
    except DesktopSessionError as exc:
        return 400, {
            "error": "invalid_request",
            "reason_code": exc.code,
            "detail": exc.detail,
        }


def _catalog_count(reader, store: FileResearchCustodyStore, key: str) -> int | None:
    try:
        payload = reader(store)
    except Exception:  # noqa: BLE001 - context is best-effort and must never block the assistant
        return None
    items = payload.get(key) if isinstance(payload, dict) else None
    return len(items) if isinstance(items, list) else None


def assistant_context(
    sqx_home: Path | str | None,
    trusted_launcher_sha256: str | None,
    research_store: FileResearchCustodyStore | None,
) -> dict[str, object]:
    """Secret-free, bounded read-model context handed to the assistant prompt."""

    status = runtime_status_record(
        sqx_home,
        trusted_launcher_sha256,
        research_store_bound=research_store is not None,
        data_root=research_store.root if research_store is not None else None,
    )
    backend = status["research_backend"]
    context: dict[str, object] = {
        "research_backend": {
            "status": backend.get("status"),
            "producer": backend.get("producer"),
            "build": backend.get("build"),
            "execution_available": backend.get("execution", {}).get("available") if isinstance(backend.get("execution"), dict) else None,
            "reason_code": backend.get("reason_code"),
        },
        "research_custody": {"status": status["research_custody"]["status"]},
        "market_data": {"status": status["market_data"].get("status"), "reason_code": status["market_data"].get("reason_code")},
        "account": {"status": status["account"]["status"], "reason_code": status["account"]["reason_code"]},
        "surfaces": [
            "Getting started",
            "Builder",
            "Retester",
            "Optimizer",
            "Data manager",
            "Custom projects",
            "Apollo",
            "Operate",
            "Settings",
        ],
        "live_producers": {
            "status": status["live_producers"]["status"],
            "reason_code": status["live_producers"]["reason_code"],
            "tradingview": status["live_producers"]["tradingview"]["reason_code"],
            "metatrader": status["live_producers"]["metatrader"]["reason_code"],
            "custom_project_control": status.get("research_backend", {}).get("execution", {}).get("reason_code") if isinstance(status.get("research_backend"), dict) else None,
        },
    }
    if research_store is not None:
        context["research_catalog_counts"] = {
            "ideas": _catalog_count(list_current_ideas, research_store, "ideas"),
            "configurations": _catalog_count(list_current_configurations, research_store, "configurations"),
            "native_jobs": _catalog_count(list_current_native_jobs, research_store, "jobs"),
            "candidates": _catalog_count(list_current_candidates, research_store, "candidates"),
        }
        try:
            from tradercockpit.research_clarifying_questions import clarifying_questions_record

            questions = clarifying_questions_record(research_store, sqx_home=sqx_home)
            current = questions.get("current_question") if isinstance(questions, dict) else None
            context["clarifying_questions"] = {
                "open_count": questions.get("open_count"),
                "blocked_count": questions.get("blocked_count"),
                "object_kind": questions.get("object_kind"),
                "build_gate_locked": (questions.get("build_gate") or {}).get("locked") if isinstance(questions.get("build_gate"), dict) else True,
                "current_question": (
                    {
                        "id": current.get("id"),
                        "prompt": current.get("prompt"),
                        "status": current.get("status"),
                        "allowed_answers": [
                            item.get("id")
                            for item in (current.get("allowed_answers") or [])
                            if isinstance(item, dict)
                        ],
                    }
                    if isinstance(current, dict)
                    else None
                ),
            }
        except Exception:  # noqa: BLE001 - context is best-effort and must never block the assistant
            context["clarifying_questions"] = {"open_count": None, "reason_code": "questions_unavailable"}
    return context


def assistant_status_response() -> tuple[int, dict[str, object]]:
    return 200, assistant_status_record()


def assistant_reply_response(
    payload: dict[str, object],
    sqx_home: Path | str | None,
    trusted_launcher_sha256: str | None,
    research_store: FileResearchCustodyStore | None,
) -> tuple[int, dict[str, object]]:
    context = assistant_context(sqx_home, trusted_launcher_sha256, research_store)
    return assistant_reply(payload, context=context, research_store=research_store, sqx_home=sqx_home)


def market_quotes_response(
    market_provider: object | None = None,
) -> tuple[int, dict[str, object]]:
    """Return the live/current watchlist quotes read model.

    The watchlist is operator configuration (``TRADERCOCKPIT_WATCHLIST``); quote values
    exist only when a market-data provider is connected. With no provider, this is an
    explicit ``provider_not_configured`` record carrying the configured symbols as
    placeholders. No prices, changes, or symbols are hard-coded.
    """

    return 200, market_quotes_record(market_provider, watchlist_from_env())


def market_bars_response(
    market_provider: object | None = None,
    *,
    symbol: str | None = None,
    timeframe: str | None = None,
) -> tuple[int, dict[str, object]]:
    """Return the live/current OHLC bar-series read model.

    Symbol and timeframe are the requested instrument. Missing values fail closed
    (``instrument_unspecified`` / ``timeframe_unspecified``). Quotes are never
    used as a candle substitute.
    """

    return 200, market_bars_record(
        market_provider,
        symbol=symbol,
        timeframe=timeframe,
        watchlist=watchlist_from_env(),
    )


def research_next_action_response(
    research_store: FileResearchCustodyStore | None,
    sqx_home: Path | str | None = None,
) -> tuple[int, dict[str, object]]:
    return 200, research_next_action_record(research_store, sqx_home=sqx_home)


def research_ideas_response(
    research_store: FileResearchCustodyStore | None,
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
            return 200, list_current_ideas(research_store)
        return 200, read_current_idea(research_store, entity_id)
    except ResearchIdeaError as exc:
        status = 409 if exc.code == "idea_content_corrupt" else 400
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


def research_idea_write_response(
    research_store: FileResearchCustodyStore | None,
    payload: dict[str, object],
) -> tuple[int, dict[str, object]]:
    if research_store is None:
        return 503, {
            "error": "store_not_configured",
            "reason_code": "research_store_not_bound",
            "detail": "Canonical research custody store is not bound.",
        }

    keys = set(payload)
    is_create = "entity_id" not in payload and "expected_revision" not in payload
    if is_create:
        if not {"text"} <= keys or keys - {"text", "source"}:
            return 400, {
                "error": "invalid_request",
                "detail": "Idea creation accepts only text and optional source.",
            }
    else:
        required = {"entity_id", "expected_revision", "text"}
        if not required <= keys or keys - (required | {"source"}):
            return 400, {
                "error": "invalid_request",
                "detail": "Idea revision requires entity_id, expected_revision, text, and optional source.",
            }

    source = payload.get("source", "")
    try:
        if is_create:
            return 201, create_idea(
                research_store,
                text=payload["text"],  # type: ignore[arg-type]
                source=source,  # type: ignore[arg-type]
            )
        return 200, revise_idea(
            research_store,
            entity_id=payload["entity_id"],  # type: ignore[arg-type]
            expected_revision=payload["expected_revision"],  # type: ignore[arg-type]
            text=payload["text"],  # type: ignore[arg-type]
            source=source,  # type: ignore[arg-type]
        )
    except ResearchIdeaError as exc:
        return 400, {
            "error": "invalid_request",
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


def research_configurations_response(
    research_store: FileResearchCustodyStore | None,
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
            return 200, list_current_configurations(research_store)
        return 200, read_current_configuration(research_store, entity_id)
    except ResearchConfigurationError as exc:
        status = 409 if exc.code == "configuration_content_corrupt" else 400
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


def research_configuration_write_response(
    research_store: FileResearchCustodyStore | None,
    sqx_home: Path | str | None,
    payload: dict[str, object],
) -> tuple[int, dict[str, object]]:
    if research_store is None:
        return 503, {
            "error": "store_not_configured",
            "reason_code": "research_store_not_bound",
            "detail": "Canonical research custody store is not bound.",
        }

    action = payload.get("action")
    if action == "compile":
        if set(payload) != {"action"}:
            return 400, {
                "error": "invalid_request",
                "detail": "Configuration compile accepts only action=compile.",
            }
        try:
            return 201, compile_current_builder_configuration(research_store, sqx_home)
        except (SqxBuilderConfigError, SqxPresetRuntimeError) as exc:
            status = 503 if exc.code in {
                "runtime_not_configured",
                "builder_project_missing",
                "runtime_build_mismatch",
                "runtime_identity_missing",
            } else 409
            return status, {
                "error": "producer_not_configured" if status == 503 else "invalid_state",
                "reason_code": exc.code,
                "detail": exc.detail,
            }
        except ResearchConfigurationError as exc:
            return 409, {
                "error": "invalid_state",
                "reason_code": exc.code,
                "detail": exc.detail,
            }
        except ResearchCustodyError as exc:
            return 409, {
                "error": "invalid_state",
                "reason_code": exc.code,
                "detail": exc.detail,
            }

    if action == "approve":
        required = {"action", "entity_id", "expected_revision"}
        if set(payload) != required:
            return 400, {
                "error": "invalid_request",
                "detail": "Configuration approval requires action, entity_id, and expected_revision only.",
            }
        try:
            return 200, approve_configuration(
                research_store,
                entity_id=payload["entity_id"],  # type: ignore[arg-type]
                expected_revision=payload["expected_revision"],  # type: ignore[arg-type]
            )
        except ResearchConfigurationError as exc:
            status = 409 if exc.code in {
                "configuration_already_approved",
                "configuration_content_corrupt",
            } else 400
            return status, {
                "error": "invalid_state" if status == 409 else "invalid_request",
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

    return 400, {
        "error": "invalid_request",
        "reason_code": "configuration_action_invalid",
        "detail": "Configuration action must be compile or approve.",
    }


def research_native_jobs_response(
    research_store: FileResearchCustodyStore | None,
    *,
    entity_id: str | None = None,
    configuration_revision: str | None = None,
) -> tuple[int, dict[str, object]]:
    if research_store is None:
        return 503, {
            "error": "store_not_configured",
            "reason_code": "research_store_not_bound",
            "detail": "Canonical research custody store is not bound.",
        }
    try:
        if entity_id is not None:
            return 200, read_current_native_job(research_store, entity_id)
        return 200, list_current_native_jobs(research_store, configuration_revision)
    except ResearchNativeJobError as exc:
        status = 409 if exc.code in {"native_job_content_corrupt", "native_job_duplicate"} else 400
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


def research_native_job_write_response(
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
    required = {"action", "configuration_entity_id", "expected_configuration_revision"}
    if set(payload) != required or payload.get("action") != "launch-builder":
        return 400, {
            "error": "invalid_request",
            "reason_code": "native_job_action_invalid",
            "detail": "Native job launch requires action=launch-builder and exact configuration entity/revision identity.",
        }
    if not isinstance(payload.get("configuration_entity_id"), str) or not payload["configuration_entity_id"]:
        return 400, {"error": "invalid_request", "detail": "configuration_entity_id must be a non-empty string"}
    if not isinstance(payload.get("expected_configuration_revision"), str) or not payload["expected_configuration_revision"]:
        return 400, {"error": "invalid_request", "detail": "expected_configuration_revision must be a non-empty string"}
    try:
        result = launch_approved_builder_configuration(
            research_store,
            sqx_home,
            trusted_launcher_sha256,
            configuration_entity_id=payload["configuration_entity_id"],
            expected_configuration_revision=payload["expected_configuration_revision"],
        )
        return (200 if result.get("reused") else 201), result
    except ResearchNativeJobError as exc:
        unavailable = {
            "runtime_not_configured",
            "runtime_build_mismatch",
            "runtime_identity_missing",
            "trusted_launcher_not_configured",
            "sqx_launcher_missing",
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
        else:
            status, error = 409, "invalid_state"
        return status, {"error": error, "reason_code": exc.code, "detail": exc.detail}


def research_candidates_response(
    research_store: FileResearchCustodyStore | None,
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
            return 200, list_current_candidates(research_store)
        return 200, read_current_candidate(research_store, entity_id)
    except ResearchCandidateError as exc:
        status = 409 if exc.code in {"candidate_content_corrupt", "candidate_duplicate"} else 400
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


def _candidate_write_store_error(exc: ResearchCustodyError) -> tuple[int, dict[str, object]]:
    if exc.code == "current_conflict":
        status, error = 409, "conflict"
    elif exc.code == "current_pointer_missing":
        status, error = 404, "not_found"
    elif exc.code in {"entity_id_invalid", "entity_kind_invalid", "revision_ref_invalid"}:
        status, error = 400, "invalid_request"
    else:
        status, error = 409, "invalid_state"
    return status, {"error": error, "reason_code": exc.code, "detail": exc.detail}


def _candidate_import_response(
    research_store: FileResearchCustodyStore,
    sqx_home: Path | str | None,
    payload: dict[str, object],
) -> tuple[int, dict[str, object]]:
    required = {
        "action",
        "native_job_entity_id",
        "expected_native_job_revision",
        "archive",
        "expected_archive_sha256",
    }
    if set(payload) != required:
        return 400, {
            "error": "invalid_request",
            "reason_code": "candidate_action_invalid",
            "detail": "Candidate import requires one exact submitted native job revision and one exact native output archive identity.",
        }
    if any(not isinstance(payload.get(key), str) or not payload[key] for key in required - {"action"}):
        return 400, {"error": "invalid_request", "detail": "candidate import identities must be non-empty strings"}
    try:
        result = import_native_candidate(
            research_store,
            sqx_home,
            native_job_entity_id=payload["native_job_entity_id"],
            expected_native_job_revision=payload["expected_native_job_revision"],
            archive_name=payload["archive"],
            expected_archive_sha256=payload["expected_archive_sha256"],
        )
        return (200 if result.get("reused") else 201), result
    except ResearchCandidateError as exc:
        unavailable = {"runtime_not_configured", "runtime_build_mismatch", "results_databank_missing"}
        status = 503 if exc.code in unavailable else 409
        return status, {
            "error": "producer_not_configured" if status == 503 else "invalid_state",
            "reason_code": exc.code,
            "detail": exc.detail,
        }
    except ResearchCustodyError as exc:
        return _candidate_write_store_error(exc)


def _candidate_bind_ml_response(
    research_store: FileResearchCustodyStore,
    payload: dict[str, object],
) -> tuple[int, dict[str, object]]:
    required = {
        "action",
        "candidate_entity_id",
        "expected_candidate_revision",
        "artifact_sha256",
    }
    if set(payload) != required:
        return 400, {
            "error": "invalid_request",
            "reason_code": "candidate_action_invalid",
            "detail": "Candidate ML bind requires one exact candidate revision and one Models catalog digest.",
        }
    if any(not isinstance(payload.get(key), str) or not payload[key] for key in required - {"action"}):
        return 400, {"error": "invalid_request", "detail": "candidate ML bind identities must be non-empty strings"}
    try:
        result = bind_ml_model(
            research_store,
            candidate_entity_id=payload["candidate_entity_id"],
            expected_candidate_revision=payload["expected_candidate_revision"],
            artifact_sha256=payload["artifact_sha256"],
        )
        return (200 if result.get("reused") else 201), result
    except ResearchCandidateError as exc:
        if exc.code == "candidate_ml_model_missing":
            status, error = 404, "not_found"
        elif exc.code in {"candidate_ml_model_invalid", "candidate_entity_invalid", "candidate_revision_invalid"}:
            status, error = 400, "invalid_request"
        else:
            status, error = 409, "invalid_state"
        return status, {"error": error, "reason_code": exc.code, "detail": exc.detail}
    except ResearchCustodyError as exc:
        return _candidate_write_store_error(exc)


def research_candidate_write_response(
    research_store: FileResearchCustodyStore | None,
    sqx_home: Path | str | None,
    payload: dict[str, object],
) -> tuple[int, dict[str, object]]:
    if research_store is None:
        return 503, {
            "error": "store_not_configured",
            "reason_code": "research_store_not_bound",
            "detail": "Canonical research custody store is not bound.",
        }
    action = payload.get("action")
    if action == "import-native-output":
        return _candidate_import_response(research_store, sqx_home, payload)
    if action == "bind-ml-model":
        return _candidate_bind_ml_response(research_store, payload)
    return 400, {
        "error": "invalid_request",
        "reason_code": "candidate_action_invalid",
        "detail": "Candidate writes require one exact import-native-output identity tuple or one exact bind-ml-model identity tuple.",
    }


def sqx_preset_response(
    sqx_home: Path | str | None,
    preset_id: str | None = None,
) -> tuple[int, dict[str, object]]:
    if preset_id is None:
        return 200, preset_catalog(sqx_home)
    if not isinstance(preset_id, str) or not preset_id:
        return 400, {"error": "invalid_request", "detail": "presetId must be a non-empty string"}
    try:
        descriptor = get_sqx_preset(preset_id)
    except KeyError:
        return 404, {"error": "not_found", "detail": "unknown SQX preset"}
    return 200, {
        "schema": "tc.sqx-preset.v1",
        "preset": preset_record(descriptor, sqx_home),
    }


def sqx_builder_config_response(
    sqx_home: Path | str | None,
) -> tuple[int, dict[str, object]]:
    try:
        return 200, builder_project_config_record(sqx_home)
    except SqxBuilderConfigError as exc:
        status = 503 if exc.code in {"runtime_not_configured", "builder_project_missing"} else 409
        return status, {
            "error": "producer_not_configured" if status == 503 else "invalid_state",
            "reason_code": exc.code,
            "detail": exc.detail,
        }
    except Exception as exc:
        code = getattr(exc, "code", "runtime_invalid")
        detail = getattr(exc, "detail", str(exc))
        return 503, {
            "error": "producer_not_configured",
            "reason_code": str(code),
            "detail": str(detail),
        }


def sqx_module_response(
    sqx_home: Path | str | None,
    module: str,
    *,
    trusted_launcher_sha256: str | None = None,
    register_worker: object | None = None,
) -> tuple[int, dict[str, object]]:
    try:
        return 200, read_sqx_run_module(
            sqx_home,
            module,
            trusted_launcher_sha256=trusted_launcher_sha256,
            register_worker=register_worker,
        )
    except SqxRunModuleError as exc:
        if exc.code == "sqx_module_name_invalid":
            status, error = 400, "invalid_request"
        elif exc.code in {"runtime_not_configured"}:
            status, error = 503, "producer_not_configured"
        else:
            status, error = 409, "invalid_state"
        return status, {
            "error": error,
            "reason_code": exc.code,
            "detail": exc.detail,
        }
    except Exception as exc:
        code = getattr(exc, "code", "runtime_invalid")
        detail = getattr(exc, "detail", str(exc))
        return 503, {
            "error": "producer_not_configured",
            "reason_code": str(code),
            "detail": str(detail),
        }


def sqx_project_topology_response(
    sqx_home: Path | str | None,
    project: str,
    *,
    trusted_launcher_sha256: str | None = None,
    register_worker: object | None = None,
) -> tuple[int, dict[str, object]]:
    if not isinstance(project, str) or not project:
        return 400, {"error": "invalid_request", "detail": "project must be a non-empty string"}
    try:
        return 200, custom_project_topology_record(
            sqx_home,
            project,
            trusted_launcher_sha256=trusted_launcher_sha256,
            register_worker=register_worker,
        )
    except SqxCustomProjectTopologyError as exc:
        if exc.code == "custom_project_missing":
            status, error = 404, "not_found"
        elif exc.code in {"custom_project_name_invalid"}:
            status, error = 400, "invalid_request"
        elif exc.code in {"runtime_not_configured"}:
            status, error = 503, "producer_not_configured"
        else:
            status, error = 409, "invalid_state"
        return status, {
            "error": error,
            "reason_code": exc.code,
            "detail": exc.detail,
        }
    except Exception as exc:
        code = getattr(exc, "code", "runtime_invalid")
        detail = getattr(exc, "detail", str(exc))
        return 503, {
            "error": "producer_not_configured",
            "reason_code": str(code),
            "detail": str(detail),
        }


def sqx_projects_response(
    sqx_home: Path | str | None,
    *,
    trusted_launcher_sha256: str | None = None,
    register_worker: object | None = None,
) -> tuple[int, dict[str, object]]:
    try:
        return 200, list_custom_projects(
            sqx_home,
            trusted_launcher_sha256=trusted_launcher_sha256,
            register_worker=register_worker,
        )
    except SqxCustomProjectTopologyError as exc:
        status, error = (503, "producer_not_configured") if exc.code in {"runtime_not_configured"} else (409, "invalid_state")
        return status, {
            "error": error,
            "reason_code": exc.code,
            "detail": exc.detail,
        }
    except Exception as exc:
        code = getattr(exc, "code", "runtime_invalid")
        detail = getattr(exc, "detail", str(exc))
        return 503, {
            "error": "producer_not_configured",
            "reason_code": str(code),
            "detail": str(detail),
        }


def sqx_project_results_response(
    sqx_home: Path | str | None,
    project: str | None,
) -> tuple[int, dict[str, object]]:
    try:
        return 200, list_custom_project_results(sqx_home, project)
    except SqxCustomProjectTopologyError as exc:
        if exc.code == "custom_project_missing":
            status, error = 404, "not_found"
        elif exc.code in {"custom_project_name_invalid"}:
            status, error = 400, "invalid_request"
        elif exc.code in {"runtime_not_configured"}:
            status, error = 503, "producer_not_configured"
        else:
            status, error = 409, "invalid_state"
        return status, {
            "error": error,
            "reason_code": exc.code,
            "detail": exc.detail,
        }
    except Exception as exc:
        code = getattr(exc, "code", "runtime_invalid")
        detail = getattr(exc, "detail", str(exc))
        return 503, {
            "error": "producer_not_configured",
            "reason_code": str(code),
            "detail": str(detail),
        }


def sqx_project_strategy_response(
    sqx_home: Path | str | None,
    project: str,
    databank: str,
    archive: str,
    task: int | None,
    focus_ticket: int | None = None,
) -> tuple[int, dict[str, object]]:
    try:
        return 200, inspect_custom_project_strategy(
            sqx_home, project, databank, archive, task=task, focus_ticket=focus_ticket
        )
    except SqxCustomProjectTopologyError as exc:
        if exc.code in {"custom_project_missing", "custom_project_strategy_missing", "custom_project_task_missing"}:
            status, error = 404, "not_found"
        elif exc.code in {
            "custom_project_name_invalid",
            "custom_project_databank_name_invalid",
            "custom_project_archive_name_invalid",
        }:
            status, error = 400, "invalid_request"
        elif exc.code in {"runtime_not_configured"}:
            status, error = 503, "producer_not_configured"
        else:
            status, error = 409, "invalid_state"
        return status, {
            "error": error,
            "reason_code": exc.code,
            "detail": exc.detail,
        }
    except Exception as exc:
        code = getattr(exc, "code", "runtime_invalid")
        detail = getattr(exc, "detail", str(exc))
        return 503, {
            "error": "producer_not_configured",
            "reason_code": str(code),
            "detail": str(detail),
        }


def sqx_results_plugin_file_response(
    sqx_home: Path | str | None,
    path: str,
) -> tuple[int, bytes | None, str | None, dict[str, object] | None]:
    try:
        body, content_type = read_results_plugin_file(sqx_home, path)
        return 200, body, content_type, None
    except SqxCustomProjectTopologyError as exc:
        if exc.code in {"results_plugin_missing"}:
            status, error = 404, "not_found"
        elif exc.code in {
            "results_plugin_name_invalid",
            "results_plugin_type_unsupported",
            "results_plugin_too_large",
        }:
            status, error = 400, "invalid_request"
        elif exc.code in {"runtime_not_configured"}:
            status, error = 503, "producer_not_configured"
        else:
            status, error = 409, "invalid_state"
        return status, None, None, {
            "error": error,
            "reason_code": exc.code,
            "detail": exc.detail,
        }
    except Exception as exc:
        code = getattr(exc, "code", "runtime_invalid")
        detail = getattr(exc, "detail", str(exc))
        return 503, None, None, {
            "error": "producer_not_configured",
            "reason_code": str(code),
            "detail": str(detail),
        }


def _sqx_web_http_status(exc: SqxNativeWebError | SqxCustomProjectTopologyError) -> tuple[int, str]:
    code = exc.code
    if code in {
        "custom_project_missing",
        "custom_project_strategy_missing",
        "results_plugin_missing",
        "results_plugin_template_missing",
        "sqx_web_settings_missing",
    }:
        return 404, "not_found"
    if code in {
        "custom_project_name_invalid",
        "custom_project_databank_name_invalid",
        "custom_project_archive_name_invalid",
        "sourcecode_fields_invalid",
        "overview_fields_invalid",
        "chart_fields_invalid",
        "results_plugin_name_invalid",
        "results_plugin_exists",
        "sqx_web_port_invalid",
        "sqx_web_token_invalid",
        "sqx_web_path_invalid",
        "sqx_web_settings_invalid",
        "calibrate_fields_invalid",
    }:
        return 400, "invalid_request"
    if code in {"runtime_not_configured"}:
        return 503, "producer_not_configured"
    if code in {"sqx_web_unavailable", "sqx_web_unauthorized"}:
        return 503, "producer_not_configured"
    if code in {"strategy_xml_unreadable", "calibrate_data_missing", "calibrate_results_invalid"}:
        return 409, "invalid_state"
    return 409, "invalid_state"


def sqx_sourcecode_catalog_response(sqx_home: Path | str | None) -> tuple[int, dict[str, object]]:
    try:
        return 200, sourcecode_catalog_record(sqx_home)
    except SqxCustomProjectTopologyError as exc:
        status, error = _sqx_web_http_status(exc)
        return status, {"error": error, "reason_code": exc.code, "detail": exc.detail}


def sqx_sourcecode_print_response(
    sqx_home: Path | str | None,
    payload: dict[str, object],
) -> tuple[int, dict[str, object]]:
    try:
        return 200, print_sourcecode(sqx_home, payload)
    except (SqxNativeWebError, SqxCustomProjectTopologyError) as exc:
        status, error = _sqx_web_http_status(exc)
        return status, {"error": error, "reason_code": exc.code, "detail": exc.detail}


def sqx_overview_response(
    sqx_home: Path | str | None,
    project: str,
    databank: str,
    archive: str,
    *,
    template: str = "SQDefault",
    sample: str = "full",
    direction: str = "both",
) -> tuple[int, dict[str, object]]:
    try:
        return 200, overview_html(
            sqx_home,
            project=project,
            databank=databank,
            archive=archive,
            template=template,
            sample=sample,
            direction=direction,
        )
    except (SqxNativeWebError, SqxCustomProjectTopologyError) as exc:
        status, error = _sqx_web_http_status(exc)
        return status, {"error": error, "reason_code": exc.code, "detail": exc.detail}


def sqx_results_chart_response(
    sqx_home: Path | str | None,
    project: str,
    databank: str,
    archive: str,
    *,
    stock: str = "",
) -> tuple[int, dict[str, object]]:
    try:
        return 200, results_chart(
            sqx_home,
            project=project,
            databank=databank,
            archive=archive,
            stock=stock,
        )
    except (SqxNativeWebError, SqxCustomProjectTopologyError) as exc:
        status, error = _sqx_web_http_status(exc)
        return status, {"error": error, "reason_code": exc.code, "detail": exc.detail}


def sqx_sourcecode_action_response(
    sqx_home: Path | str | None,
    payload: dict[str, object],
) -> tuple[int, dict[str, object]]:
    action = payload.get("action")
    if action is None:
        action = "print"
    if not isinstance(action, str):
        return 400, {"error": "invalid_request", "detail": "Source Code action must be a string."}
    body = {key: value for key, value in payload.items() if key != "action"}
    try:
        if action == "print":
            return 200, print_sourcecode(sqx_home, body)
        if action == "saveEA":
            return 200, save_ea(sqx_home, body)
        if action == "getDataPath":
            return 200, sourcecode_data_path(sqx_home, body)
        if action == "saveMTPaths":
            return 200, save_mt_paths(sqx_home, body)
    except (SqxNativeWebError, SqxCustomProjectTopologyError) as exc:
        status, error = _sqx_web_http_status(exc)
        return status, {"error": error, "reason_code": exc.code, "detail": exc.detail}
    return 400, {"error": "invalid_request", "detail": "Source Code action must be print, saveEA, getDataPath, or saveMTPaths."}


def sqx_results_plugin_create_response(
    sqx_home: Path | str | None,
    payload: dict[str, object],
) -> tuple[int, dict[str, object]]:
    if set(payload) - {"name"}:
        return 400, {
            "error": "invalid_request",
            "reason_code": "results_plugin_name_invalid",
            "detail": "Results plugin create accepts only name.",
        }
    name = payload.get("name")
    if not isinstance(name, str) or not name:
        return 400, {"error": "invalid_request", "detail": "name must be a non-empty string"}
    try:
        return 200, create_results_plugin(sqx_home, name)
    except SqxCustomProjectTopologyError as exc:
        status, error = _sqx_web_http_status(exc)
        return status, {"error": error, "reason_code": exc.code, "detail": exc.detail}


_LAUNCH_UNAVAILABLE_CODES = frozenset(
    {
        "runtime_not_configured",
        "trusted_launcher_not_configured",
        "trusted_launcher_digest_invalid",
        "sqx_launcher_missing",
        "desktop_worker_unregistered",
    }
)


def sqx_project_progress_response(
    sqx_home: Path | str | None,
    project: str,
    *,
    trusted_launcher_sha256: str | None = None,
    register_worker: object | None = None,
    worker_is_active: object | None = None,
) -> tuple[int, dict[str, object]]:
    if not isinstance(project, str) or not project:
        return 400, {"error": "invalid_request", "detail": "project must be a non-empty string"}
    try:
        return 200, custom_project_progress_record(
            sqx_home,
            project,
            trusted_launcher_sha256=trusted_launcher_sha256,
            register_worker=register_worker,
            worker_is_active=worker_is_active,
        )
    except SqxCustomProjectTopologyError as exc:
        if exc.code == "custom_project_missing":
            status, error = 404, "not_found"
        elif exc.code in {"custom_project_name_invalid"}:
            status, error = 400, "invalid_request"
        elif exc.code in {"runtime_not_configured"}:
            status, error = 503, "producer_not_configured"
        else:
            status, error = 409, "invalid_state"
        return status, {
            "error": error,
            "reason_code": exc.code,
            "detail": exc.detail,
        }


def sqx_project_control_response(
    sqx_home: Path | str | None,
    payload: dict[str, object],
    *,
    trusted_launcher_sha256: str | None = None,
    register_worker: object | None = None,
    worker_is_active: object | None = None,
) -> tuple[int, dict[str, object]]:
    project = payload.get("project")
    action = payload.get("action")
    if set(payload) - {"project", "action"}:
        return 400, {
            "error": "invalid_request",
            "reason_code": "custom_project_control_fields_invalid",
            "detail": "Custom Project control accepts only project and action.",
        }
    if not isinstance(project, str) or not project:
        return 400, {"error": "invalid_request", "detail": "project must be a non-empty string"}
    if not isinstance(action, str) or not action:
        return 400, {"error": "invalid_request", "detail": "action must be a non-empty string"}
    try:
        return 200, custom_project_control(
            sqx_home,
            project,
            action,
            trusted_launcher_sha256=trusted_launcher_sha256,
            register_worker=register_worker,
            worker_is_active=worker_is_active,
        )
    except SqxCustomProjectControlError as exc:
        if exc.code == "custom_project_action_invalid":
            status, error = 400, "invalid_request"
        elif exc.code in {"custom_project_missing"}:
            status, error = 404, "not_found"
        elif exc.code in {"custom_project_name_invalid"}:
            status, error = 400, "invalid_request"
        elif exc.code in _LAUNCH_UNAVAILABLE_CODES:
            status, error = 503, "producer_not_configured"
        else:
            status, error = 409, "invalid_state"
        return status, {
            "error": error,
            "reason_code": exc.code,
            "detail": exc.detail,
        }
    except SqxCustomProjectTopologyError as exc:
        if exc.code == "custom_project_missing":
            status, error = 404, "not_found"
        elif exc.code in {"custom_project_name_invalid"}:
            status, error = 400, "invalid_request"
        elif exc.code in {"runtime_not_configured"}:
            status, error = 503, "producer_not_configured"
        else:
            status, error = 409, "invalid_state"
        return status, {
            "error": error,
            "reason_code": exc.code,
            "detail": exc.detail,
        }


def sqx_project_settings_response(
    sqx_home: Path | str | None,
    payload: dict[str, object],
) -> tuple[int, dict[str, object]]:
    project = payload.get("project")
    task = payload.get("task")
    updates = payload.get("updates")
    if set(payload) - {"project", "task", "updates"}:
        return 400, {
            "error": "invalid_request",
            "reason_code": "custom_project_settings_fields_invalid",
            "detail": "Custom Project settings accept only project, task, and updates.",
        }
    if not isinstance(project, str) or not project:
        return 400, {"error": "invalid_request", "detail": "project must be a non-empty string"}
    if isinstance(task, bool) or not isinstance(task, int):
        return 400, {"error": "invalid_request", "detail": "task must be the exact native task index"}
    if not isinstance(updates, list):
        return 400, {"error": "invalid_request", "detail": "updates must be a list of existing attribute or text writes"}
    try:
        return 200, update_custom_project_settings(sqx_home, project, task, updates)
    except SqxCustomProjectTopologyError as exc:
        if exc.code in {
            "custom_project_settings_updates_invalid",
            "custom_project_settings_path_invalid",
            "custom_project_settings_attribute_invalid",
            "custom_project_settings_value_invalid",
            "custom_project_settings_text_missing",
            "custom_project_task_index_invalid",
            "custom_project_name_invalid",
        }:
            status, error = 400, "invalid_request"
        elif exc.code in {"custom_project_missing", "custom_project_task_missing"}:
            status, error = 404, "not_found"
        elif exc.code in {"runtime_not_configured"}:
            status, error = 503, "producer_not_configured"
        else:
            status, error = 409, "invalid_state"
        return status, {
            "error": error,
            "reason_code": exc.code,
            "detail": exc.detail,
        }


def sqx_calibrate_response(
    sqx_home: Path | str | None,
    payload: dict[str, object],
) -> tuple[int, dict[str, object]]:
    extra = set(payload) - {"project", "task", "apply"}
    if extra:
        return 400, {
            "error": "invalid_request",
            "reason_code": "calibrate_fields_invalid",
            "detail": "Calibrate accepts only project, task, and optional apply.",
        }
    project = payload.get("project")
    task = payload.get("task")
    apply = payload.get("apply", True)
    if not isinstance(project, str) or not project:
        return 400, {"error": "invalid_request", "detail": "project must be a non-empty string"}
    if isinstance(task, bool) or not isinstance(task, int):
        return 400, {"error": "invalid_request", "detail": "task must be the exact native task index"}
    if not isinstance(apply, bool):
        return 400, {"error": "invalid_request", "detail": "apply must be true or false"}
    try:
        return 200, calibrate_indicators(sqx_home, project, task, apply=apply)
    except (SqxNativeWebError, SqxCustomProjectTopologyError) as exc:
        status, error = _sqx_web_http_status(exc)
        return status, {"error": error, "reason_code": exc.code, "detail": exc.detail}


def live_producers_response() -> tuple[int, dict[str, object]]:
    return 200, live_producers_record()


class TraderCockpitHTTPServer(ThreadingHTTPServer):
    """Refuse a second listener on the same Windows loopback port."""

    allow_reuse_address = False

    def server_bind(self) -> None:
        if os.name == "nt":
            self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_EXCLUSIVEADDRUSE, 1)
        super().server_bind()


def make_handler(
    web_root: Path,
    sqx_home: Path | str | None = None,
    trusted_launcher_sha256: str | None = None,
    research_store: FileResearchCustodyStore | None = None,
    market_provider: object | None = None,
    register_worker: object | None = None,
    worker_is_active: object | None = None,
):
    """Create the one canonical HTTP handler used by server and desktop."""

    directory = str(web_root.resolve())

    class Handler(SimpleHTTPRequestHandler):
        extensions_map = {
            **SimpleHTTPRequestHandler.extensions_map,
            ".js": "text/javascript; charset=utf-8",
            ".mjs": "text/javascript; charset=utf-8",
        }

        def __init__(self, *args, **kwargs):
            super().__init__(*args, directory=directory, **kwargs)

        def end_headers(self) -> None:
            self.send_header("cache-control", "no-store")
            super().end_headers()

        def _json(self, status: int, payload: dict[str, object]) -> None:
            body = json.dumps(
                payload,
                ensure_ascii=False,
                sort_keys=True,
                separators=(",", ":"),
            ).encode("utf-8")
            self.send_response(status)
            self.send_header("content-type", "application/json; charset=utf-8")
            self.send_header("content-length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def _bytes(self, status: int, body: bytes, content_type: str) -> None:
            self.send_response(status)
            self.send_header("content-type", content_type)
            self.send_header("content-length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def _research_client_is_loopback(self) -> bool:
            return _is_loopback_address(str(self.client_address[0]))

        def _reject_non_loopback_research_request(self) -> None:
            self._json(
                403,
                {
                    "error": "forbidden",
                    "reason_code": "local_custody_only",
                    "detail": "Research custody is available only to loopback clients.",
                },
            )

        def _request_json(self) -> dict[str, object] | None:
            content_type = (self.headers.get("Content-Type") or "").split(";", 1)[0].strip().lower()
            if content_type != "application/json":
                self._json(415, {"error": "unsupported_media_type", "detail": "application/json is required"})
                return None
            raw_length = self.headers.get("Content-Length")
            try:
                length = int(raw_length) if raw_length is not None else -1
            except ValueError:
                length = -1
            if length <= 0 or length > MAX_JSON_BODY_BYTES:
                self._json(400, {"error": "invalid_request", "detail": "JSON body length is missing, empty, or too large"})
                return None
            raw = self.rfile.read(length)
            if len(raw) != length:
                self._json(400, {"error": "invalid_request", "detail": "JSON request body is incomplete"})
                return None
            try:
                payload = json.loads(raw)
            except (UnicodeDecodeError, json.JSONDecodeError):
                self._json(400, {"error": "invalid_request", "detail": "request body must be valid JSON"})
                return None
            if not isinstance(payload, dict):
                self._json(400, {"error": "invalid_request", "detail": "request body must be a JSON object"})
                return None
            return payload

        def _request_audio(self) -> bytes | None:
            raw_length = self.headers.get("Content-Length")
            try:
                length = int(raw_length) if raw_length is not None else -1
            except ValueError:
                length = -1
            if length <= 0 or length > MAX_AUDIO_BYTES:
                self._json(400, {"error": "invalid_request", "reason_code": "audio_too_large" if length > MAX_AUDIO_BYTES else "audio_invalid", "detail": "audio body length is missing, empty, or too large"})
                return None
            raw = self.rfile.read(length)
            if len(raw) != length:
                self._json(400, {"error": "invalid_request", "reason_code": "audio_invalid", "detail": "audio request body is incomplete"})
                return None
            return raw

        def do_GET(self) -> None:  # noqa: N802 - stdlib handler API
            parsed = urlsplit(self.path)
            if parsed.path == STATUS_API_PATH:
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "runtime status accepts no query parameters"})
                    return
                status, payload = status_response(sqx_home, trusted_launcher_sha256, research_store)
                self._json(status, payload)
                return

            if parsed.path == CAPABILITIES_API_PATH:
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "capability registry accepts no query parameters"})
                    return
                status, payload = capabilities_response(research_store, sqx_home)
                self._json(status, payload)
                return

            if parsed.path == DESKTOP_SESSION_API_PATH:
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "desktop session accepts no query parameters"})
                    return
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                status, payload = desktop_session_response(research_store)
                self._json(status, payload)
                return

            if parsed.path == MARKET_QUOTES_API_PATH:
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "market quotes accepts no query parameters"})
                    return
                status, payload = market_quotes_response(market_provider)
                self._json(status, payload)
                return

            if parsed.path == MARKET_BARS_API_PATH:
                query = parse_qs(parsed.query, keep_blank_values=True)
                extra = set(query) - {"symbol", "timeframe"}
                if extra:
                    self._json(400, {"error": "invalid_request", "detail": "market bars accepts only symbol and timeframe"})
                    return
                if any(len(values) != 1 for values in query.values()):
                    self._json(400, {"error": "invalid_request", "detail": "market bars query values must be singular"})
                    return
                status, payload = market_bars_response(
                    market_provider,
                    symbol=query["symbol"][0] if "symbol" in query else None,
                    timeframe=query["timeframe"][0] if "timeframe" in query else None,
                )
                self._json(status, payload)
                return

            if parsed.path == RESEARCH_NEXT_ACTION_API_PATH:
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "next action accepts no query parameters"})
                    return
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                status, payload = research_next_action_response(research_store, sqx_home)
                self._json(status, payload)
                return

            if parsed.path == RESEARCH_CLARIFYING_QUESTIONS_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                query = parse_qs(parsed.query, keep_blank_values=True)
                if set(query) - {"entityId"}:
                    self._json(400, {"error": "invalid_request", "detail": "clarifying questions accept only entityId"})
                    return
                entity_ids = query.get("entityId", [])
                if len(entity_ids) > 1 or (entity_ids and not entity_ids[0]):
                    self._json(400, {"error": "invalid_request", "detail": "at most one non-empty entityId is allowed"})
                    return
                status, payload = clarifying_questions_response(
                    research_store,
                    sqx_home=sqx_home,
                    entity_id=entity_ids[0] if entity_ids else None,
                )
                self._json(status, payload)
                return

            if parsed.path == ASSISTANT_API_PATH:
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "assistant status accepts no query parameters"})
                    return
                status, payload = assistant_status_response()
                self._json(status, payload)
                return

            if parsed.path == ASSISTANT_TRANSCRIBE_API_PATH:
                self._json(405, {"error": "method_not_allowed", "detail": "Speech-to-text is POST only."})
                return

            if parsed.path == RESEARCH_IDEA_INGEST_API_PATH:
                self._json(405, {"error": "method_not_allowed", "reason_code": "read_only_baseline", "detail": "Idea ingest is POST only."})
                return

            if parsed.path == RESEARCH_IDEAS_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                query = parse_qs(parsed.query, keep_blank_values=True)
                if set(query) - {"entityId"}:
                    self._json(400, {"error": "invalid_request", "detail": "unsupported query parameter"})
                    return
                entity_ids = query.get("entityId", [])
                if len(entity_ids) > 1 or (entity_ids and not entity_ids[0]):
                    self._json(400, {"error": "invalid_request", "detail": "at most one non-empty entityId is allowed"})
                    return
                status, payload = research_ideas_response(research_store, entity_ids[0] if entity_ids else None)
                self._json(status, payload)
                return

            if parsed.path == RESEARCH_CONFIGURATIONS_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                query = parse_qs(parsed.query, keep_blank_values=True)
                if set(query) - {"entityId"}:
                    self._json(400, {"error": "invalid_request", "detail": "unsupported query parameter"})
                    return
                entity_ids = query.get("entityId", [])
                if len(entity_ids) > 1 or (entity_ids and not entity_ids[0]):
                    self._json(400, {"error": "invalid_request", "detail": "at most one non-empty entityId is allowed"})
                    return
                status, payload = research_configurations_response(
                    research_store,
                    entity_ids[0] if entity_ids else None,
                )
                self._json(status, payload)
                return

            if parsed.path == RESEARCH_NATIVE_JOBS_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                query = parse_qs(parsed.query, keep_blank_values=True)
                if set(query) - {"entityId", "configurationRevision"}:
                    self._json(400, {"error": "invalid_request", "detail": "unsupported query parameter"})
                    return
                entity_ids = query.get("entityId", [])
                revisions = query.get("configurationRevision", [])
                if len(entity_ids) > 1 or len(revisions) > 1 or (entity_ids and revisions):
                    self._json(400, {"error": "invalid_request", "detail": "use at most one native-job selector"})
                    return
                if (entity_ids and not entity_ids[0]) or (revisions and not revisions[0]):
                    self._json(400, {"error": "invalid_request", "detail": "native-job selector cannot be empty"})
                    return
                status, payload = research_native_jobs_response(
                    research_store,
                    entity_id=entity_ids[0] if entity_ids else None,
                    configuration_revision=revisions[0] if revisions else None,
                )
                self._json(status, payload)
                return

            if parsed.path == RESEARCH_CANDIDATES_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                query = parse_qs(parsed.query, keep_blank_values=True)
                if set(query) - {"entityId"}:
                    self._json(400, {"error": "invalid_request", "detail": "unsupported query parameter"})
                    return
                entity_ids = query.get("entityId", [])
                if len(entity_ids) > 1 or (entity_ids and not entity_ids[0]):
                    self._json(400, {"error": "invalid_request", "detail": "at most one non-empty entityId is allowed"})
                    return
                status, payload = research_candidates_response(research_store, entity_ids[0] if entity_ids else None)
                self._json(status, payload)
                return

            if parsed.path == RESEARCH_HISTORICAL_RESULTS_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                query = parse_qs(parsed.query, keep_blank_values=True)
                if set(query) - {"entityId", "candidateRevision"}:
                    self._json(400, {"error": "invalid_request", "detail": "unsupported query parameter"})
                    return
                entity_ids = query.get("entityId", [])
                candidate_revisions = query.get("candidateRevision", [])
                if len(entity_ids) > 1 or len(candidate_revisions) > 1 or (entity_ids and candidate_revisions):
                    self._json(400, {"error": "invalid_request", "detail": "use at most one historical-result selector"})
                    return
                if (entity_ids and not entity_ids[0]) or (candidate_revisions and not candidate_revisions[0]):
                    self._json(400, {"error": "invalid_request", "detail": "historical-result selector cannot be empty"})
                    return
                status, payload = historical_results_response(
                    research_store,
                    entity_id=entity_ids[0] if entity_ids else None,
                    candidate_revision=candidate_revisions[0] if candidate_revisions else None,
                )
                self._json(status, payload)
                return

            if parsed.path == RESEARCH_MODELS_API_PATH:
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "models catalog accepts no query parameters"})
                    return
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                root = research_store.root if research_store is not None else None
                self._json(200, models_catalog(root))
                return

            if parsed.path == RESEARCH_PROOFS_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                query = parse_qs(parsed.query, keep_blank_values=True)
                if set(query) - {"entityId"}:
                    self._json(400, {"error": "invalid_request", "detail": "unsupported query parameter"})
                    return
                entity_ids = query.get("entityId", [])
                if len(entity_ids) > 1 or (entity_ids and not entity_ids[0]):
                    self._json(400, {"error": "invalid_request", "detail": "at most one non-empty entityId is allowed"})
                    return
                status, payload = research_proofs_response(
                    research_store,
                    entity_id=entity_ids[0] if entity_ids else None,
                )
                self._json(status, payload)
                return

            if parsed.path == SQX_PRESETS_API_PATH:
                query = parse_qs(parsed.query, keep_blank_values=True)
                if set(query) - {"presetId"}:
                    self._json(400, {"error": "invalid_request", "detail": "unsupported query parameter"})
                    return
                preset_ids = query.get("presetId", [])
                if len(preset_ids) > 1:
                    self._json(400, {"error": "invalid_request", "detail": "at most one presetId is allowed"})
                    return
                status, payload = sqx_preset_response(sqx_home, preset_ids[0] if preset_ids else None)
                self._json(status, payload)
                return

            if parsed.path == SQX_OUTPUTS_API_PATH:
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "SQX output discovery accepts no query parameters"})
                    return
                self._json(200, discover_sqx_outputs(sqx_home))
                return

            if parsed.path == SQX_BUILDER_CONFIG_API_PATH:
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "Builder config read accepts no query parameters"})
                    return
                status, payload = sqx_builder_config_response(sqx_home)
                self._json(status, payload)
                return

            if parsed.path == SQX_RUN_MODULE_API_PATH:
                query = parse_qs(parsed.query, keep_blank_values=True)
                if set(query) != {"module"} or len(query.get("module", [])) != 1 or not query["module"][0]:
                    self._json(400, {"error": "invalid_request", "detail": "exactly one non-empty module parameter is required"})
                    return
                status, payload = sqx_module_response(
                    sqx_home,
                    query["module"][0],
                    trusted_launcher_sha256=trusted_launcher_sha256,
                    register_worker=register_worker,
                )
                self._json(status, payload)
                return

            if parsed.path == SQX_PROJECT_TOPOLOGY_API_PATH:
                query = parse_qs(parsed.query, keep_blank_values=True)
                if set(query) != {"project"} or len(query.get("project", [])) != 1 or not query["project"][0]:
                    self._json(400, {"error": "invalid_request", "detail": "exactly one non-empty project parameter is required"})
                    return
                status, payload = sqx_project_topology_response(
                    sqx_home,
                    query["project"][0],
                    trusted_launcher_sha256=trusted_launcher_sha256,
                    register_worker=register_worker,
                )
                self._json(status, payload)
                return

            if parsed.path == SQX_CUSTOM_PROJECTS_API_PATH:
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "Custom Project catalog accepts no query parameters"})
                    return
                status, payload = sqx_projects_response(
                    sqx_home,
                    trusted_launcher_sha256=trusted_launcher_sha256,
                    register_worker=register_worker,
                )
                self._json(status, payload)
                return

            if parsed.path == SQX_CUSTOM_PROJECT_RESULTS_API_PATH:
                query = parse_qs(parsed.query, keep_blank_values=True)
                if set(query) - {"project"}:
                    self._json(400, {"error": "invalid_request", "detail": "Custom Project results accept only an optional project parameter"})
                    return
                selected = query.get("project", [None])[0]
                if "project" in query and (len(query["project"]) != 1 or not selected):
                    self._json(400, {"error": "invalid_request", "detail": "project must be one non-empty name when provided"})
                    return
                status, payload = sqx_project_results_response(sqx_home, selected)
                self._json(status, payload)
                return

            if parsed.path == SQX_CUSTOM_PROJECT_STRATEGY_API_PATH:
                query = parse_qs(parsed.query, keep_blank_values=True)
                allowed = {"project", "databank", "archive", "task", "focusTicket"}
                if set(query) - allowed or not {"project", "databank", "archive"}.issubset(set(query)):
                    self._json(400, {"error": "invalid_request", "detail": "Custom Project strategy inspect requires project, databank, and archive"})
                    return
                if any(len(query[key]) != 1 or not query[key][0] for key in ("project", "databank", "archive")):
                    self._json(400, {"error": "invalid_request", "detail": "project, databank, and archive must each be one non-empty value"})
                    return
                task_value = None
                if "task" in query:
                    if len(query["task"]) != 1 or not query["task"][0]:
                        self._json(400, {"error": "invalid_request", "detail": "task must be one native task index when provided"})
                        return
                    try:
                        task_value = int(query["task"][0])
                    except ValueError:
                        self._json(400, {"error": "invalid_request", "detail": "task must be one native task index when provided"})
                        return
                focus_ticket = None
                if "focusTicket" in query:
                    if len(query["focusTicket"]) != 1 or not query["focusTicket"][0]:
                        self._json(400, {"error": "invalid_request", "detail": "focusTicket must be one native ticket when provided"})
                        return
                    try:
                        focus_ticket = int(query["focusTicket"][0])
                    except ValueError:
                        self._json(400, {"error": "invalid_request", "detail": "focusTicket must be one native ticket when provided"})
                        return
                status, payload = sqx_project_strategy_response(
                    sqx_home,
                    query["project"][0],
                    query["databank"][0],
                    query["archive"][0],
                    task_value,
                    focus_ticket,
                )
                self._json(status, payload)
                return

            if parsed.path == SQX_OVERVIEW_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                query = parse_qs(parsed.query, keep_blank_values=True)
                required = {"project", "databank", "archive"}
                if set(query) - OVERVIEW_QUERY or not required.issubset(set(query)):
                    self._json(400, {"error": "invalid_request", "detail": "Overview requires project, databank, and archive"})
                    return
                if any(len(query[key]) != 1 or not query[key][0] for key in query):
                    self._json(400, {"error": "invalid_request", "detail": "Overview query values must each be one non-empty string"})
                    return
                status, payload = sqx_overview_response(
                    sqx_home,
                    query["project"][0],
                    query["databank"][0],
                    query["archive"][0],
                    template=query.get("template", ["SQDefault"])[0],
                    sample=query.get("sample", ["full"])[0],
                    direction=query.get("direction", ["both"])[0],
                )
                self._json(status, payload)
                return

            if parsed.path == SQX_RESULTS_CHART_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                query = parse_qs(parsed.query, keep_blank_values=True)
                required = {"project", "databank", "archive"}
                if set(query) - CHART_QUERY or not required.issubset(set(query)):
                    self._json(400, {"error": "invalid_request", "detail": "Results chart requires project, databank, and archive"})
                    return
                if any(len(query[key]) != 1 or not query[key][0] for key in ("project", "databank", "archive")):
                    self._json(400, {"error": "invalid_request", "detail": "Results chart query values must each be one non-empty string"})
                    return
                if "stock" in query and (len(query["stock"]) != 1):
                    self._json(400, {"error": "invalid_request", "detail": "Results chart stock must be one native symbol string"})
                    return
                status, payload = sqx_results_chart_response(
                    sqx_home,
                    query["project"][0],
                    query["databank"][0],
                    query["archive"][0],
                    stock=query.get("stock", [""])[0],
                )
                self._json(status, payload)
                return

            if parsed.path == SQX_SOURCECODE_API_PATH:
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "Source Code catalog accepts no query parameters"})
                    return
                status, payload = sqx_sourcecode_catalog_response(sqx_home)
                self._json(status, payload)
                return

            if parsed.path == SQX_RESULTS_PLUGIN_API_PATH or parsed.path.startswith(SQX_RESULTS_PLUGIN_API_PATH + "/"):
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "Results plugin files accept no query parameters"})
                    return
                status, body, content_type, error = sqx_results_plugin_file_response(sqx_home, parsed.path)
                if error is not None:
                    self._json(status, error)
                    return
                self._bytes(status, body or b"", content_type or "application/octet-stream")
                return

            if parsed.path == SQX_CUSTOM_PROJECT_PROGRESS_API_PATH:
                query = parse_qs(parsed.query, keep_blank_values=True)
                if set(query) != {"project"} or len(query.get("project", [])) != 1 or not query["project"][0]:
                    self._json(400, {"error": "invalid_request", "detail": "exactly one non-empty project parameter is required"})
                    return
                status, payload = sqx_project_progress_response(
                    sqx_home,
                    query["project"][0],
                    trusted_launcher_sha256=trusted_launcher_sha256,
                    register_worker=register_worker,
                    worker_is_active=worker_is_active,
                )
                self._json(status, payload)
                return

            if parsed.path == LIVE_PRODUCERS_API_PATH:
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "live producers accept no query parameters"})
                    return
                status, payload = live_producers_response()
                self._json(status, payload)
                return

            if parsed.path.startswith("/api/"):
                self._json(404, {"error": "not_found", "detail": "unknown API path"})
                return

            if parsed.path == "/" or not Path(parsed.path).suffix:
                self.path = "/index.html"
            super().do_GET()

        def do_POST(self) -> None:  # noqa: N802 - stdlib handler API
            parsed = urlsplit(self.path)
            if parsed.path == CAPABILITIES_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "capability registry writes accept no query parameters"})
                    return
                payload = self._request_json()
                if payload is None:
                    return
                status, response = capabilities_stage_response(sqx_home, payload)
                self._json(status, response)
                return

            if parsed.path == DESKTOP_SESSION_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "desktop session writes accept no query parameters"})
                    return
                payload = self._request_json()
                if payload is None:
                    return
                status, response = desktop_session_write_response(research_store, payload)
                self._json(status, response)
                return

            if parsed.path == ASSISTANT_TRANSCRIBE_API_PATH:
                if not self._research_client_is_loopback():
                    self._json(403, {"error": "forbidden", "reason_code": "local_assistant_only", "detail": "The assistant is available only to loopback clients."})
                    return
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "speech-to-text accepts no query parameters"})
                    return
                audio = self._request_audio()
                if audio is None:
                    return
                status, response = transcribe_response(audio, content_type=self.headers.get("Content-Type"))
                self._json(status, response)
                return

            if parsed.path == ASSISTANT_API_PATH:
                if not self._research_client_is_loopback():
                    self._json(403, {"error": "forbidden", "reason_code": "local_assistant_only", "detail": "The assistant is available only to loopback clients."})
                    return
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "assistant messages accept no query parameters"})
                    return
                payload = self._request_json()
                if payload is None:
                    return
                status, response = assistant_reply_response(payload, sqx_home, trusted_launcher_sha256, research_store)
                self._json(status, response)
                return

            if parsed.path == RESEARCH_CLARIFYING_QUESTIONS_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "clarifying answers accept no query parameters"})
                    return
                payload = self._request_json()
                if payload is None:
                    return
                status, response = clarifying_questions_write(research_store, payload, sqx_home=sqx_home)
                self._json(status, response)
                return

            if parsed.path == RESEARCH_IDEA_INGEST_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "Idea ingest accepts no query parameters"})
                    return
                payload = self._request_json()
                if payload is None:
                    return
                status, response = research_idea_ingest_write(research_store, payload)
                self._json(status, response)
                return

            if parsed.path == RESEARCH_IDEAS_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "Idea writes accept no query parameters"})
                    return
                payload = self._request_json()
                if payload is None:
                    return
                status, response = research_idea_write_response(research_store, payload)
                self._json(status, response)
                return

            if parsed.path == RESEARCH_CONFIGURATIONS_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "Configuration writes accept no query parameters"})
                    return
                payload = self._request_json()
                if payload is None:
                    return
                status, response = research_configuration_write_response(research_store, sqx_home, payload)
                self._json(status, response)
                return

            if parsed.path == RESEARCH_NATIVE_JOBS_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "Native job writes accept no query parameters"})
                    return
                payload = self._request_json()
                if payload is None:
                    return
                status, response = research_native_job_write_response(
                    research_store,
                    sqx_home,
                    trusted_launcher_sha256,
                    payload,
                )
                self._json(status, response)
                return

            if parsed.path == RESEARCH_CANDIDATES_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "Candidate writes accept no query parameters"})
                    return
                payload = self._request_json()
                if payload is None:
                    return
                status, response = research_candidate_write_response(research_store, sqx_home, payload)
                self._json(status, response)
                return

            if parsed.path == RESEARCH_HISTORICAL_RESULTS_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "Historical-result writes accept no query parameters"})
                    return
                payload = self._request_json()
                if payload is None:
                    return
                status, response = historical_result_write_response(
                    research_store,
                    sqx_home,
                    trusted_launcher_sha256,
                    payload,
                )
                self._json(status, response)
                return

            if parsed.path == RESEARCH_MODELS_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "models writes accept no query parameters"})
                    return
                payload = self._request_json()
                if payload is None:
                    return
                status, response = models_write(research_store, payload)
                self._json(status, response)
                return

            if parsed.path == RESEARCH_PROOFS_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "Proof writes accept no query parameters"})
                    return
                payload = self._request_json()
                if payload is None:
                    return
                status, response = research_proof_write_response(research_store, payload)
                self._json(status, response)
                return

            if parsed.path == SQX_CUSTOM_PROJECT_CONTROL_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "Custom Project control accepts no query parameters"})
                    return
                payload = self._request_json()
                if payload is None:
                    return
                status, response = sqx_project_control_response(
                    sqx_home,
                    payload,
                    trusted_launcher_sha256=trusted_launcher_sha256,
                    register_worker=register_worker,
                    worker_is_active=worker_is_active,
                )
                self._json(status, response)
                return

            if parsed.path == SQX_SOURCECODE_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "Source Code print accepts no query parameters"})
                    return
                payload = self._request_json()
                if payload is None:
                    return
                status, response = sqx_sourcecode_action_response(sqx_home, payload)
                self._json(status, response)
                return

            if parsed.path == SQX_RESULTS_PLUGIN_CREATE_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "Results plugin create accepts no query parameters"})
                    return
                payload = self._request_json()
                if payload is None:
                    return
                status, response = sqx_results_plugin_create_response(sqx_home, payload)
                self._json(status, response)
                return

            if parsed.path == SQX_CUSTOM_PROJECT_SETTINGS_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "Custom Project settings accept no query parameters"})
                    return
                payload = self._request_json()
                if payload is None:
                    return
                status, response = sqx_project_settings_response(sqx_home, payload)
                self._json(status, response)
                return

            if parsed.path == SQX_CALIBRATE_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "Calibrate accepts no query parameters"})
                    return
                payload = self._request_json()
                if payload is None:
                    return
                status, response = sqx_calibrate_response(sqx_home, payload)
                self._json(status, response)
                return

            if parsed.path.startswith("/api/"):
                self._json(
                    405,
                    {
                        "error": "method_not_allowed",
                        "reason_code": "read_only_baseline",
                        "detail": "This API route has no approved mutation contract.",
                    },
                )
                return
            self._json(405, {"error": "method_not_allowed", "detail": "POST is not supported"})

    return Handler


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Serve TraderCockpit")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=int(os.environ.get("PORT", "4173")))
    parser.add_argument("--web-root", type=Path, default=_DEFAULT_WEB_ROOT)
    parser.add_argument(
        "--data-root",
        type=Path,
        default=None,
        help="Trusted process-side application data-root override.",
    )
    parser.add_argument(
        "--sqx-home",
        type=Path,
        default=Path(os.environ["SQX_HOME"]) if os.environ.get("SQX_HOME") else None,
        help="Authorized SQX installation used for read-only native inspection.",
    )
    parser.add_argument(
        "--sqx-launcher-sha256",
        default=os.environ.get(SQX_LAUNCHER_SHA256_ENV),
        help="Server-side trusted SHA-256 for the installed sqcli.exe launcher.",
    )
    args = parser.parse_args(argv)
    if not args.web_root.is_dir():
        parser.error(f"web root does not exist: {args.web_root}")

    data_root = resolve_application_data_root(args.data_root)
    research_store = FileResearchCustodyStore(data_root)
    workers = DesktopWorkerSupervisor()

    def register_worker(
        process,
        *,
        label: str,
        timeout_seconds: float = DEFAULT_WORKER_STOP_TIMEOUT_SECONDS,
    ) -> None:
        workers.register(process, label=label, timeout_seconds=timeout_seconds)

    def worker_is_active(label: str) -> bool:
        return workers.is_active(label)

    server = TraderCockpitHTTPServer(
        (args.host, args.port),
        make_handler(
            args.web_root,
            args.sqx_home,
            args.sqx_launcher_sha256,
            research_store,
            register_worker=register_worker,
            worker_is_active=worker_is_active,
        ),
    )
    print(f"TraderCockpit listening on http://{args.host}:{args.port}")
    print("Research custody ready: canonical local application store is bound")
    if args.sqx_home is None:
        print("Native SQX inspection unavailable: set SQX_HOME or --sqx-home")
    if args.sqx_launcher_sha256 is None:
        print(f"Native SQX launcher trust unavailable: set {SQX_LAUNCHER_SHA256_ENV}")
    else:
        print("Native SQX Builder/Retester controls are bound and remain exact-custody/trust gated")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())