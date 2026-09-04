"""Typed clarifying questions bound to unresolved Specification fields.

Unresolved native or Models requirements become Apollo questions with an
allowlist of answers. Free-form invention is refused. Answering records
user-selected product custody; it does not write executable XML, launch SQX,
or unlock Build while a required field is still open or blocked.
"""

from __future__ import annotations

from datetime import datetime, timezone
import json
from typing import Mapping, Sequence

from tradercockpit.atomic_io import atomic_write_json
from tradercockpit.market_data import ALLOWED_BAR_TIMEFRAMES, watchlist_from_env
from tradercockpit.research_models import FAMILIES


QUESTIONS_SCHEMA = "tc.research-clarifying-questions.v1"
ANSWERS_SCHEMA = "tc.research-specification-answers.v1"
RESEARCH_CLARIFYING_QUESTIONS_API_PATH = "/api/research/clarifying-questions"
_ANSWERS_NAME = "clarifying-answers.json"

_RESOLVED_NATIVE_STATES = frozenset(
    {"producer_configured", "user_selected", "proven_default", "native_validated", "not_applicable"}
)
_OBJECT_KINDS = frozenset({"indicator", "strategy", "model"})

_STATIC: dict[str, tuple[dict[str, str], ...]] = {
    "object_kind": (
        {"id": "indicator", "label": "Indicator"},
        {"id": "strategy", "label": "Strategy"},
        {"id": "model", "label": "Model"},
    ),
    "session": (
        {"id": "exchange_default", "label": "Exchange default"},
        {"id": "rth", "label": "Regular trading hours"},
        {"id": "eth", "label": "Extended hours"},
    ),
    "cost_assumptions": (
        {"id": "include_spread_and_commission", "label": "Include spread and commission"},
        {"id": "spread_only", "label": "Spread only"},
    ),
    "sample_split": (
        {"id": "in_sample_out_of_sample", "label": "In-sample and out-of-sample"},
        {"id": "full_sample_only", "label": "Full sample only"},
    ),
    "exits": (
        {"id": "stop_and_target", "label": "Stop and profit target"},
        {"id": "stop_only", "label": "Stop only"},
        {"id": "native_default", "label": "Keep native Builder exits"},
    ),
    "search_build_mode": (
        {"id": "random-generation", "label": "Random Discovery"},
        {"id": "genetic-evolution", "label": "Genetic / Evolutionary search"},
    ),
    "ranking_preference": (
        {"id": "net_profit", "label": "Net profit (producer column)"},
        {"id": "return_dd", "label": "Return / drawdown (producer column)"},
        {"id": "expectancy", "label": "Expectancy (producer column)"},
    ),
    "robustness_intent": (
        {"id": "none_until_native", "label": "None until native CrossChecks exist"},
        {"id": "use_native_cross_checks", "label": "Use native CrossChecks when enabled"},
    ),
    "leakage_split": (
        {"id": "purged_embargoed", "label": "Purged / embargoed split"},
        {"id": "refuse_fit", "label": "Refuse to fit until a leakage-safe split exists"},
    ),
    "feature_source": (
        {"id": "native_trade_fields", "label": "Native trade fields (Duration, MAE, MFE, PipsPL)"},
    ),
}

# id, label, prompt, applies_to, required, native_requirement_id, answer_source
_FIELDS: tuple[tuple[str, str, str, tuple[str, ...], bool, str | None, str], ...] = (
    (
        "object_kind",
        "Object kind",
        "Is this Idea an indicator, a strategy, or a model?",
        ("unresolved", "indicator", "strategy", "model"),
        True,
        None,
        "object_kind",
    ),
    (
        "market_identity",
        "Market identity",
        "Which configured watchlist symbol should this plan use?",
        ("indicator", "strategy", "model"),
        True,
        "market_identity",
        "watchlist",
    ),
    (
        "timeframe",
        "Timeframe",
        "Which bar timeframe should this plan use?",
        ("indicator", "strategy", "model"),
        True,
        "market_identity",
        "timeframe",
    ),
    (
        "session",
        "Session",
        "Which session assumption should this plan use?",
        ("indicator", "strategy"),
        True,
        "trading_options",
        "session",
    ),
    (
        "cost_assumptions",
        "Cost assumptions",
        "Which cost assumption should this plan use?",
        ("indicator", "strategy"),
        True,
        "historical_backtest",
        "cost_assumptions",
    ),
    (
        "sample_split",
        "Sample split",
        "How should in-sample and out-of-sample history be treated?",
        ("indicator", "strategy", "model"),
        True,
        "historical_backtest",
        "sample_split",
    ),
    (
        "exits",
        "Exits",
        "Which exit shape should this strategy plan use?",
        ("strategy",),
        True,
        None,
        "exits",
    ),
    (
        "search_build_mode",
        "Search / build mode",
        "Which Construct modality should this strategy plan use?",
        ("strategy",),
        True,
        "search_build_mode",
        "search_build_mode",
    ),
    (
        "ranking_preference",
        "Fitness / ranking preference",
        "Which native ranking column should this plan prefer?",
        ("strategy",),
        True,
        "ranking_filters",
        "ranking_preference",
    ),
    (
        "robustness_intent",
        "Robustness",
        "How should robustness be treated until native CrossChecks exist?",
        ("strategy",),
        True,
        "validation_profile",
        "robustness_intent",
    ),
    (
        "estimator_family",
        "Model family",
        "Which allowlisted estimator family should this model use?",
        ("model",),
        True,
        None,
        "estimator_family",
    ),
    (
        "leakage_split",
        "Leakage-safe split",
        "Which leakage control should lock or allow Models fit?",
        ("model",),
        True,
        None,
        "leakage_split",
    ),
    (
        "feature_source",
        "Feature source",
        "Which feature source should this model use?",
        ("model",),
        True,
        None,
        "feature_source",
    ),
)


class ClarifyingQuestionError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


def _answers_path(store) -> object:
    return store.base / _ANSWERS_NAME


def _empty_answers() -> dict[str, object]:
    return {"schema": ANSWERS_SCHEMA, "ideas": {}}


def load_answers(store) -> dict[str, object]:
    path = _answers_path(store)
    if not path.is_file():
        return _empty_answers()
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ClarifyingQuestionError("answers_corrupt", "clarifying-answer custody is not valid JSON") from exc
    if not isinstance(payload, dict) or payload.get("schema") != ANSWERS_SCHEMA or not isinstance(payload.get("ideas"), dict):
        raise ClarifyingQuestionError("answers_corrupt", "clarifying-answer custody schema is invalid")
    return payload


def save_answers(store, payload: dict[str, object]) -> None:
    atomic_write_json(_answers_path(store), payload)


def _watchlist_answers(environ: Mapping[str, str] | None) -> tuple[dict[str, str], ...]:
    return tuple({"id": symbol, "label": symbol} for symbol in watchlist_from_env(environ))


def _timeframe_answers() -> tuple[dict[str, str], ...]:
    return tuple({"id": item, "label": item} for item in sorted(ALLOWED_BAR_TIMEFRAMES))


def _estimator_answers() -> tuple[dict[str, str], ...]:
    return tuple(
        {"id": str(item["family_id"]), "label": str(item["label"])}
        for item in FAMILIES
        if isinstance(item, dict) and item.get("family_id") and item.get("label")
    )


def allowed_answers_for(field_id: str, *, environ: Mapping[str, str] | None = None) -> tuple[dict[str, str], ...]:
    if field_id == "watchlist" or field_id == "market_identity":
        return _watchlist_answers(environ)
    if field_id == "timeframe":
        return _timeframe_answers()
    if field_id == "estimator_family":
        return _estimator_answers()
    return _STATIC.get(field_id, ())


def _native_requirement_states(sqx_home) -> tuple[dict[str, str], str | None]:
    if sqx_home is None:
        return {}, "native_runtime_unavailable"
    try:
        from tradercockpit.sqx_builder_config import (
            builder_project_specification_record,
            read_sqx_builder_project,
        )

        specification = builder_project_specification_record(read_sqx_builder_project(sqx_home))
    except Exception:  # noqa: BLE001 - native absence is a typed blocked state, not an HTTP 500
        return {}, "native_runtime_unavailable"
    states: dict[str, str] = {}
    requirements = specification.get("requirements") if isinstance(specification, dict) else None
    if not isinstance(requirements, list):
        return {}, "native_runtime_unavailable"
    for item in requirements:
        if isinstance(item, dict) and isinstance(item.get("id"), str) and isinstance(item.get("state"), str):
            states[item["id"]] = item["state"]
    return states, None


def _latest_idea(store) -> dict[str, object] | None:
    from tradercockpit.research_ideas import list_current_ideas, read_current_idea

    catalog = list_current_ideas(store).get("ideas") or []
    if not isinstance(catalog, list) or not catalog:
        return None
    first = catalog[0]
    if not isinstance(first, dict) or not isinstance(first.get("entity_id"), str):
        return None
    return read_current_idea(store, first["entity_id"])


def _idea_record(store, entity_id: str | None) -> dict[str, object] | None:
    from tradercockpit.research_custody import ResearchCustodyError
    from tradercockpit.research_ideas import ResearchIdeaError, read_current_idea

    try:
        if entity_id:
            return read_current_idea(store, entity_id)
        return _latest_idea(store)
    except (ResearchIdeaError, ResearchCustodyError) as exc:
        raise ClarifyingQuestionError(getattr(exc, "code", "idea_invalid"), str(getattr(exc, "detail", exc))) from exc


def _draft_object_kind(idea: Mapping[str, object] | None) -> str:
    draft = idea.get("draft") if isinstance(idea, Mapping) else None
    if not isinstance(draft, dict):
        return "unresolved"
    kind = draft.get("object_kind")
    return kind if kind in _OBJECT_KINDS else "unresolved"


def _stored_answer(answers: Mapping[str, object], entity_id: str, field_id: str) -> dict[str, object] | None:
    ideas = answers.get("ideas")
    if not isinstance(ideas, dict):
        return None
    bucket = ideas.get(entity_id)
    if not isinstance(bucket, dict):
        return None
    stored = bucket.get("answers")
    if not isinstance(stored, dict):
        return None
    item = stored.get(field_id)
    if not isinstance(item, dict) or not isinstance(item.get("id"), str) or not isinstance(item.get("label"), str):
        return None
    return {"id": item["id"], "label": item["label"]}


def _question(
    field_id: str,
    label: str,
    prompt: str,
    *,
    status: str,
    required: bool,
    allowed: Sequence[Mapping[str, str]] = (),
    answer: Mapping[str, object] | None = None,
    source: str | None = None,
    reason_code: str | None = None,
    native_requirement_id: str | None = None,
) -> dict[str, object]:
    record: dict[str, object] = {
        "id": field_id,
        "label": label,
        "prompt": prompt,
        "status": status,
        "required": required,
        "allowed_answers": [{"id": item["id"], "label": item["label"]} for item in allowed],
        "native_requirement_id": native_requirement_id,
    }
    if answer is not None:
        record["answer"] = {"id": answer["id"], "label": answer["label"]}
    if source:
        record["source"] = source
    if reason_code:
        record["reason_code"] = reason_code
    return record


def questions_from_idea(
    idea: Mapping[str, object] | None,
    *,
    answers: Mapping[str, object] | None = None,
    native_states: Mapping[str, str] | None = None,
    native_reason: str | None = None,
    environ: Mapping[str, str] | None = None,
) -> dict[str, object]:
    """Build the clarifying-question read model for one Idea (or the empty idea gate)."""

    if idea is None:
        return {
            "schema": QUESTIONS_SCHEMA,
            "idea_entity_id": None,
            "idea_revision": None,
            "object_kind": "unresolved",
            "questions": [],
            "current_question": None,
            "open_count": 0,
            "blocked_count": 0,
            "build_gate": {
                "locked": True,
                "reason_codes": ["idea_required"],
                "next_authority": "create_idea",
            },
            "reason_code": "idea_required",
            "detail": "Clarifying questions bind to an Idea. Create or ingest an Idea first.",
        }

    entity_id = str(idea["entity_id"])
    stored = answers or _empty_answers()
    native = dict(native_states or {})
    draft_kind = _draft_object_kind(idea)
    stored_kind = _stored_answer(stored, entity_id, "object_kind")
    object_kind = draft_kind if draft_kind in _OBJECT_KINDS else (
        stored_kind["id"] if stored_kind and stored_kind["id"] in _OBJECT_KINDS else "unresolved"
    )

    questions: list[dict[str, object]] = []
    for field_id, label, prompt, applies, required, native_id, answer_source in _FIELDS:
        if object_kind == "unresolved" and field_id != "object_kind":
            continue
        if object_kind not in applies:
            continue
        if native_id and native.get(native_id) in _RESOLVED_NATIVE_STATES:
            questions.append(
                _question(
                    field_id,
                    label,
                    prompt,
                    status="resolved",
                    required=required,
                    answer={"id": native[native_id], "label": native[native_id].replace("_", " ")},
                    source="producer_configured" if native[native_id] != "not_applicable" else "not_applicable",
                    native_requirement_id=native_id,
                )
            )
            continue
        if field_id == "object_kind" and draft_kind in _OBJECT_KINDS:
            questions.append(
                _question(
                    field_id,
                    label,
                    prompt,
                    status="resolved",
                    required=required,
                    allowed=allowed_answers_for(answer_source, environ=environ),
                    answer={"id": draft_kind, "label": draft_kind.replace("_", " ").title()},
                    source="idea_draft",
                )
            )
            continue
        allowed = allowed_answers_for(answer_source, environ=environ)
        chosen = _stored_answer(stored, entity_id, field_id)
        if chosen and any(item["id"] == chosen["id"] for item in allowed):
            questions.append(
                _question(
                    field_id,
                    label,
                    prompt,
                    status="resolved",
                    required=required,
                    allowed=allowed,
                    answer=chosen,
                    source="user_selected",
                    native_requirement_id=native_id,
                )
            )
            continue
        if not allowed:
            reason = "watchlist_empty" if answer_source == "watchlist" else (native_reason or "allowed_answers_unavailable")
            questions.append(
                _question(
                    field_id,
                    label,
                    prompt,
                    status="blocked",
                    required=required,
                    reason_code=reason,
                    native_requirement_id=native_id,
                )
            )
            continue
        questions.append(
            _question(
                field_id,
                label,
                prompt,
                status="open",
                required=required,
                allowed=allowed,
                native_requirement_id=native_id,
            )
        )

    open_questions = [item for item in questions if item["status"] == "open"]
    blocked_questions = [item for item in questions if item["status"] == "blocked" and item["required"]]
    current = open_questions[0] if open_questions else (blocked_questions[0] if blocked_questions else None)
    reasons = [f"unresolved:{item['id']}" for item in questions if item["required"] and item["status"] in {"open", "blocked"}]
    for item in blocked_questions:
        code = item.get("reason_code")
        if isinstance(code, str) and code not in reasons:
            reasons.append(code)
    locked = bool(reasons)
    return {
        "schema": QUESTIONS_SCHEMA,
        "idea_entity_id": entity_id,
        "idea_revision": idea.get("revision"),
        "object_kind": object_kind,
        "questions": questions,
        "current_question": current,
        "open_count": len(open_questions),
        "blocked_count": len(blocked_questions),
        "build_gate": {
            "locked": locked,
            "reason_codes": reasons,
            "next_authority": "answer_clarifying_questions" if open_questions else (
                "complete_native_builder_configuration" if locked else "compile_review_approve_exact_native_configuration"
            ),
        },
        "reason_code": None if not locked else (str(current["reason_code"]) if current and current.get("reason_code") else "unresolved_specification_fields"),
        "detail": (
            "Typed answers only. Invented values are refused. Build stays locked while a required field is open or blocked."
        ),
    }


def clarifying_questions_record(
    research_store,
    *,
    sqx_home=None,
    entity_id: str | None = None,
    environ: Mapping[str, str] | None = None,
) -> dict[str, object]:
    if research_store is None:
        return {
            "schema": QUESTIONS_SCHEMA,
            "idea_entity_id": None,
            "idea_revision": None,
            "object_kind": "unresolved",
            "questions": [],
            "current_question": None,
            "open_count": 0,
            "blocked_count": 0,
            "build_gate": {
                "locked": True,
                "reason_codes": ["research_store_not_bound"],
                "next_authority": "bind_research_store",
            },
            "reason_code": "research_store_not_bound",
            "detail": "Research custody is not connected.",
        }
    idea = _idea_record(research_store, entity_id)
    if idea is None:
        return questions_from_idea(None)
    native_states, native_reason = _native_requirement_states(sqx_home)
    return questions_from_idea(
        idea,
        answers=load_answers(research_store),
        native_states=native_states,
        native_reason=native_reason,
        environ=environ,
    )


def record_answer(
    research_store,
    *,
    field_id: str,
    answer_id: str,
    entity_id: str | None = None,
    sqx_home=None,
    environ: Mapping[str, str] | None = None,
) -> dict[str, object]:
    if not isinstance(field_id, str) or not field_id.strip():
        raise ClarifyingQuestionError("field_id_invalid", "field_id must be a non-empty string")
    if not isinstance(answer_id, str) or not answer_id.strip():
        raise ClarifyingQuestionError("answer_id_invalid", "answer_id must be a non-empty string")
    record = clarifying_questions_record(
        research_store,
        sqx_home=sqx_home,
        entity_id=entity_id,
        environ=environ,
    )
    if record.get("reason_code") == "idea_required":
        raise ClarifyingQuestionError("idea_required", "clarifying answers require an Idea")
    target = None
    for item in record.get("questions") or []:
        if isinstance(item, dict) and item.get("id") == field_id:
            target = item
            break
    if target is None:
        raise ClarifyingQuestionError("question_not_open", "that field is not an open clarifying question for this Idea")
    if target.get("status") != "open":
        raise ClarifyingQuestionError("question_not_open", "that field is already resolved or blocked")
    allowed = target.get("allowed_answers") or []
    match = next((item for item in allowed if isinstance(item, dict) and item.get("id") == answer_id), None)
    if match is None:
        raise ClarifyingQuestionError("answer_not_allowed", "answer is not in the allowed set for this field")
    payload = load_answers(research_store)
    ideas = payload.setdefault("ideas", {})
    if not isinstance(ideas, dict):
        raise ClarifyingQuestionError("answers_corrupt", "clarifying-answer custody schema is invalid")
    idea_key = str(record["idea_entity_id"])
    bucket = ideas.get(idea_key)
    if not isinstance(bucket, dict):
        bucket = {"idea_revision": record.get("idea_revision"), "answers": {}}
        ideas[idea_key] = bucket
    stored_answers = bucket.setdefault("answers", {})
    if not isinstance(stored_answers, dict):
        stored_answers = {}
        bucket["answers"] = stored_answers
    stored_answers[field_id] = {
        "id": match["id"],
        "label": match["label"],
        "recorded_at": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
    }
    bucket["idea_revision"] = record.get("idea_revision")
    save_answers(research_store, payload)
    return clarifying_questions_record(
        research_store,
        sqx_home=sqx_home,
        entity_id=idea_key,
        environ=environ,
    )


def open_question_count(research_store, *, sqx_home=None, environ: Mapping[str, str] | None = None) -> int:
    if research_store is None:
        return 0
    try:
        record = clarifying_questions_record(research_store, sqx_home=sqx_home, environ=environ)
    except (ClarifyingQuestionError, Exception):  # noqa: BLE001 - next-action must not fail closed on a missing questionnaire
        return 0
    count = record.get("open_count")
    return int(count) if isinstance(count, int) else 0


def clarifying_questions_response(
    research_store,
    *,
    sqx_home=None,
    entity_id: str | None = None,
    environ: Mapping[str, str] | None = None,
) -> tuple[int, dict[str, object]]:
    try:
        return 200, clarifying_questions_record(
            research_store,
            sqx_home=sqx_home,
            entity_id=entity_id,
            environ=environ,
        )
    except ClarifyingQuestionError as exc:
        status = 400 if exc.code not in {"answers_corrupt"} else 409
        return status, {"error": "invalid_request" if status == 400 else "invalid_state", "reason_code": exc.code, "detail": exc.detail}


def clarifying_questions_write(
    research_store,
    payload: dict[str, object],
    *,
    sqx_home=None,
    environ: Mapping[str, str] | None = None,
) -> tuple[int, dict[str, object]]:
    if research_store is None:
        return 503, {
            "error": "store_not_configured",
            "reason_code": "research_store_not_bound",
            "detail": "Canonical research custody store is not bound.",
        }
    keys = set(payload)
    if keys - {"field_id", "answer_id", "entity_id"} or "field_id" not in payload or "answer_id" not in payload:
        return 400, {
            "error": "invalid_request",
            "detail": "Clarifying answers accept field_id, answer_id, and optional entity_id.",
        }
    entity_id = payload.get("entity_id")
    if entity_id is not None and (not isinstance(entity_id, str) or not entity_id.strip()):
        return 400, {"error": "invalid_request", "detail": "entity_id must be a non-empty string when provided"}
    try:
        record = record_answer(
            research_store,
            field_id=str(payload["field_id"]),
            answer_id=str(payload["answer_id"]),
            entity_id=entity_id if isinstance(entity_id, str) else None,
            sqx_home=sqx_home,
            environ=environ,
        )
    except ClarifyingQuestionError as exc:
        status = 409 if exc.code == "answers_corrupt" else 400
        return status, {
            "error": "invalid_state" if status == 409 else "invalid_request",
            "reason_code": exc.code,
            "detail": exc.detail,
        }
    return 200, record
