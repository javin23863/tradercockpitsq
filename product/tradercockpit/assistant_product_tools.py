"""Approved Apollo product tools: propose the same custody APIs a human click would.

These tools never mutate native SQX, never write executable XML, never invoke ``sqcli``,
and never skip gateway verification. Mutating proposals require owner confirmation in
the widget; ``native_mutation`` stays false.
"""

from __future__ import annotations

from dataclasses import dataclass
from hashlib import sha256
import json
from typing import Mapping
from urllib.parse import parse_qs, urlsplit

from tradercockpit.research_clarifying_questions import clarifying_questions_record
from tradercockpit.research_configurations import list_current_configurations
from tradercockpit.research_ideas import MAX_IDEA_SOURCE_CHARS, MAX_IDEA_TEXT_CHARS, list_current_ideas


NAVIGATE_TOOL = "navigate_surface"
DRAFT_IDEA_TOOL = "draft_idea_revision"
PROPOSE_SPEC_TOOL = "propose_specification_fields"
REQUEST_COMPILE_TOOL = "request_compile"
REQUEST_LAUNCH_TOOL = "request_launch"

PRODUCT_TOOL_NAMES = (
    NAVIGATE_TOOL,
    DRAFT_IDEA_TOOL,
    PROPOSE_SPEC_TOOL,
    REQUEST_COMPILE_TOOL,
    REQUEST_LAUNCH_TOOL,
)

RESEARCH_IDEAS_API_PATH = "/api/research/ideas"
RESEARCH_CLARIFYING_QUESTIONS_API_PATH = "/api/research/clarifying-questions"
RESEARCH_CONFIGURATIONS_API_PATH = "/api/research/configurations"
RESEARCH_NATIVE_JOBS_API_PATH = "/api/research/native-jobs"

_SURFACE_PATHS = frozenset({
    "/home",
    "/builder",
    "/data-manager",
    "/custom-projects",
    "/apollo",
    "/settings",
})
# Same redirects the browser router applies; the tool never names a surface the rail lacks.
_LEGACY_SURFACE_PATHS = {
    "/explore": "/home",
    "/operate": "/home",
    "/automation": "/custom-projects",
    "/algowizard": "/apollo",
    "/retester": "/builder",
    "/optimizer": "/builder",
}
_RESEARCH_TABS = {
    "signals": (
        "overview",
        "signals",
        "order-flow",
        "footprint",
        "volume-profile",
        "liquidity-map",
        "replays",
        "alerts",
        "reports",
    ),
    "evolution": (),
    "validate": (
        "overview",
        "initial-test",
        "trades",
        "robustness",
        "configuration",
        "evidence",
    ),
    "catalog": ("all", "indicators", "models", "strategies", "utilities", "mine"),
}
_PATH_CHARS = frozenset("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789/?=&_-")


def _spec(name: str, description: str, properties: dict[str, object], required: tuple[str, ...] = ()) -> dict[str, object]:
    return {
        "type": "function",
        "function": {
            "name": name,
            "description": description,
            "parameters": {
                "type": "object",
                "properties": properties,
                "required": list(required),
            },
        },
    }


PRODUCT_TOOL_SPECS: tuple[dict[str, object], ...] = (
    _spec(
        NAVIGATE_TOOL,
        "Navigate to a canonical TraderCockpit surface or Research workspace/tab. Path only; no executable, file, or identity query.",
        {"path": {"type": "string", "description": "Canonical path such as /home or /research?workspace=signals&tab=overview"}},
        ("path",),
    ),
    _spec(
        DRAFT_IDEA_TOOL,
        "Propose an Idea revision using the same POST /api/research/ideas contract a human save uses. Text only; do not invent object_kind, ingest spans, or executable XML.",
        {
            "text": {"type": "string", "description": "Idea source/provenance text"},
            "source": {"type": "string", "description": "Optional Idea source label"},
            "entity_id": {"type": "string", "description": "Existing Idea entity id when revising"},
            "expected_revision": {"type": "string", "description": "Current Idea revision when revising"},
        },
        ("text",),
    ),
    _spec(
        PROPOSE_SPEC_TOOL,
        "Propose one typed Specification answer using a currently open field_id and one of its allowed_answers ids. Invented answers are refused.",
        {
            "field_id": {"type": "string", "description": "Open clarifying-question field id"},
            "answer_id": {"type": "string", "description": "Allowed answer id for that field"},
        },
        ("field_id", "answer_id"),
    ),
    _spec(
        REQUEST_COMPILE_TOOL,
        "Propose compiling the exact current native Builder task (POST /api/research/configurations action=compile). No XML body. Fails closed while Specification is locked.",
        {},
    ),
    _spec(
        REQUEST_LAUNCH_TOOL,
        "Propose launching the exact current approved Builder configuration through the trusted gateway. Fails closed when no approved configuration exists. No executable path arguments.",
        {},
    ),
)


@dataclass(frozen=True, slots=True)
class ToolResult:
    content: str
    used: dict[str, str] | None = None
    proposed_action: dict[str, object] | None = None


def _error(code: str, detail: str) -> ToolResult:
    return ToolResult(content=json.dumps({"error": code, "detail": detail}, sort_keys=True))


def _action_id(tool: str, payload: Mapping[str, object]) -> str:
    digest = sha256(
        json.dumps({"tool": tool, **payload}, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()
    ).hexdigest()[:16]
    return f"tc-assistant-action:{tool}:{digest}"


def _proposed(
    tool: str,
    *,
    label: str,
    path: str,
    confirmation_required: bool,
    method: str,
    body: dict[str, object] | None = None,
    used: dict[str, str] | None = None,
) -> ToolResult:
    action = {
        "id": _action_id(tool, {"method": method, "path": path, "body": body or {}}),
        "tool": tool,
        "label": label,
        "confirmation_required": confirmation_required,
        "native_mutation": False,
        "method": method,
        "path": path,
        "body": body,
    }
    return ToolResult(
        content=json.dumps({"ok": True, "proposed_action": action}, sort_keys=True),
        used=used or {"name": tool},
        proposed_action=action,
    )


def canonicalize_navigate_path(path: object) -> str | None:
    """Return a canonical allowlisted product path, or None if the path is not legal."""

    if not isinstance(path, str) or not path.startswith("/") or any(char not in _PATH_CHARS for char in path):
        return None
    split = urlsplit(path)
    if split.scheme or split.netloc or split.fragment or split.username or split.password:
        return None
    pathname = split.path.rstrip("/") or "/home"
    pathname = _LEGACY_SURFACE_PATHS.get(pathname, pathname)
    if pathname in _SURFACE_PATHS:
        return pathname if not split.query else None
    if pathname != "/research":
        return None
    if not split.query:
        return "/builder"
    query = parse_qs(split.query, keep_blank_values=True)
    if any(len(values) != 1 for values in query.values()) or set(query) - {"workspace", "tab"}:
        return None
    workspace = query.get("workspace", ["signals"])[0]
    tabs = _RESEARCH_TABS.get(workspace)
    if tabs is None:
        return None
    if not tabs:
        return f"/research?workspace={workspace}" if "tab" not in query else None
    tab = query.get("tab", [tabs[0]])[0]
    if tab not in tabs:
        return None
    return f"/research?workspace={workspace}&tab={tab}"


def _require_object(arguments: object, allowed: set[str], *, required: set[str] | None = None) -> dict[str, object] | ToolResult:
    if not isinstance(arguments, dict):
        return _error("invalid_arguments", "tool arguments must be an object")
    keys = set(arguments)
    if keys - allowed:
        return _error("invalid_arguments", "unknown argument keys are not approved")
    missing = (required or set()) - keys
    if missing:
        return _error("invalid_arguments", "required argument keys are missing")
    return arguments


def _navigate(arguments: object) -> ToolResult:
    parsed = _require_object(arguments, {"path"}, required={"path"})
    if isinstance(parsed, ToolResult):
        return parsed
    path = canonicalize_navigate_path(parsed.get("path"))
    if path is None:
        return _error("path_not_allowed", "navigate_surface accepts only canonical product paths")
    return _proposed(
        NAVIGATE_TOOL,
        label=f"Open {path}",
        path=path,
        confirmation_required=False,
        method="GET",
        body=None,
        used={"name": NAVIGATE_TOOL, "path": path},
    )


def _current_ideas(store) -> list[dict[str, object]]:
    if store is None:
        return []
    catalog = list_current_ideas(store)
    ideas = catalog.get("ideas") if isinstance(catalog, dict) else None
    return [item for item in ideas if isinstance(item, dict)] if isinstance(ideas, list) else []


def _draft_idea(arguments: object, *, store) -> ToolResult:
    if store is None:
        return _error("research_store_not_bound", "Idea drafts require the research custody store")
    parsed = _require_object(arguments, {"text", "source", "entity_id", "expected_revision"}, required={"text"})
    if isinstance(parsed, ToolResult):
        return parsed
    text = parsed.get("text")
    if not isinstance(text, str) or not text.strip():
        return _error("invalid_arguments", "draft_idea_revision requires non-empty text")
    if len(text) > MAX_IDEA_TEXT_CHARS:
        return _error("invalid_arguments", "idea text exceeds the supported size")
    source = parsed.get("source", "typed")
    if not isinstance(source, str):
        return _error("invalid_arguments", "source must be a string")
    if len(source) > MAX_IDEA_SOURCE_CHARS:
        return _error("invalid_arguments", "idea source exceeds the supported size")
    source = source.strip() or "typed"
    entity_id = parsed.get("entity_id")
    expected_revision = parsed.get("expected_revision")
    if entity_id is None and expected_revision is None:
        ideas = _current_ideas(store)
        if len(ideas) > 1:
            return _error("idea_entity_required", "multiple Ideas exist; name entity_id and expected_revision")
        if len(ideas) == 1:
            entity_id = ideas[0].get("entity_id")
            expected_revision = ideas[0].get("revision")
    if (entity_id is None) != (expected_revision is None):
        return _error("invalid_arguments", "entity_id and expected_revision must be supplied together")
    if entity_id is not None:
        if not isinstance(entity_id, str) or not entity_id.strip():
            return _error("invalid_arguments", "entity_id must be a non-empty string")
        if not isinstance(expected_revision, str) or not expected_revision.strip():
            return _error("invalid_arguments", "expected_revision must be a non-empty string")
        match = next((item for item in _current_ideas(store) if item.get("entity_id") == entity_id), None)
        if match is None:
            return _error("idea_not_found", "that Idea is not in current custody")
        if match.get("revision") != expected_revision:
            return _error("idea_revision_conflict", "expected_revision is not the current Idea revision")
        body = {
            "entity_id": entity_id,
            "expected_revision": expected_revision,
            "text": text,
            "source": source,
        }
        label = "Save this Idea revision"
    else:
        body = {"text": text, "source": source}
        label = "Create this Idea"
    return _proposed(
        DRAFT_IDEA_TOOL,
        label=label,
        path=RESEARCH_IDEAS_API_PATH,
        confirmation_required=True,
        method="POST",
        body=body,
        used={"name": DRAFT_IDEA_TOOL},
    )


def _propose_specification(arguments: object, *, store, sqx_home, environ) -> ToolResult:
    if store is None:
        return _error("research_store_not_bound", "Specification proposals require the research custody store")
    parsed = _require_object(arguments, {"field_id", "answer_id"}, required={"field_id", "answer_id"})
    if isinstance(parsed, ToolResult):
        return parsed
    field_id = parsed.get("field_id")
    answer_id = parsed.get("answer_id")
    if not isinstance(field_id, str) or not field_id.strip() or not isinstance(answer_id, str) or not answer_id.strip():
        return _error("invalid_arguments", "field_id and answer_id must be non-empty strings")
    record = clarifying_questions_record(store, sqx_home=sqx_home, environ=environ)
    if record.get("reason_code") == "idea_required":
        return _error("idea_required", "clarifying answers require an Idea")
    target = next(
        (item for item in (record.get("questions") or []) if isinstance(item, dict) and item.get("id") == field_id),
        None,
    )
    if target is None or target.get("status") != "open":
        return _error("question_not_open", "that field is not an open clarifying question for this Idea")
    allowed = target.get("allowed_answers") or []
    match = next((item for item in allowed if isinstance(item, dict) and item.get("id") == answer_id), None)
    if match is None:
        return _error("answer_not_allowed", "answer is not in the allowed set for this field")
    body: dict[str, object] = {"field_id": field_id, "answer_id": answer_id}
    entity_id = record.get("idea_entity_id")
    if isinstance(entity_id, str) and entity_id:
        body["entity_id"] = entity_id
    return _proposed(
        PROPOSE_SPEC_TOOL,
        label=f"Answer {target.get('label') or field_id}: {match.get('label') or answer_id}",
        path=RESEARCH_CLARIFYING_QUESTIONS_API_PATH,
        confirmation_required=True,
        method="POST",
        body=body,
        used={"name": PROPOSE_SPEC_TOOL, "field_id": field_id},
    )


def _request_compile(arguments: object, *, store, sqx_home, environ) -> ToolResult:
    parsed = _require_object(arguments if arguments is not None else {}, set())
    if isinstance(parsed, ToolResult):
        return parsed
    if store is None:
        return _error("research_store_not_bound", "Compile proposals require the research custody store")
    record = clarifying_questions_record(store, sqx_home=sqx_home, environ=environ)
    if record.get("reason_code") == "idea_required":
        return _error("idea_required", "compile requires an Idea and a resolved Specification")
    gate = record.get("build_gate") if isinstance(record.get("build_gate"), dict) else {}
    if gate.get("locked") is not False:
        return _error("specification_locked", "Builder configuration cannot compile while Specification is locked")
    return _proposed(
        REQUEST_COMPILE_TOOL,
        label="Compile the exact current native Builder configuration",
        path=RESEARCH_CONFIGURATIONS_API_PATH,
        confirmation_required=True,
        method="POST",
        body={"action": "compile"},
        used={"name": REQUEST_COMPILE_TOOL},
    )


def _request_launch(arguments: object, *, store) -> ToolResult:
    parsed = _require_object(arguments if arguments is not None else {}, set())
    if isinstance(parsed, ToolResult):
        return parsed
    if store is None:
        return _error("research_store_not_bound", "Launch proposals require the research custody store")
    catalog = list_current_configurations(store)
    rows = catalog.get("configurations") if isinstance(catalog, dict) else None
    approved = [
        item
        for item in (rows or [])
        if isinstance(item, dict) and item.get("state") == "approved"
    ]
    if not approved:
        return _error("native_job_configuration_unapproved", "native launch requires the exact current approved configuration")
    if len(approved) > 1:
        return _error("multiple_approved_configurations", "launch from Evolutionary Search when more than one approved configuration exists")
    configuration = approved[0]
    entity_id = configuration.get("entity_id")
    revision = configuration.get("revision")
    if not isinstance(entity_id, str) or not entity_id or not isinstance(revision, str) or not revision:
        return _error("native_job_configuration_unapproved", "approved configuration identity is incomplete")
    return _proposed(
        REQUEST_LAUNCH_TOOL,
        label="Launch the approved Builder job",
        path=RESEARCH_NATIVE_JOBS_API_PATH,
        confirmation_required=True,
        method="POST",
        body={
            "action": "launch-builder",
            "configuration_entity_id": entity_id,
            "expected_configuration_revision": revision,
        },
        used={"name": REQUEST_LAUNCH_TOOL},
    )


def dispatch_product_tool(
    name: str,
    arguments: object,
    *,
    store=None,
    sqx_home=None,
    environ: Mapping[str, str] | None = None,
) -> ToolResult:
    if name == NAVIGATE_TOOL:
        return _navigate(arguments)
    if name == DRAFT_IDEA_TOOL:
        return _draft_idea(arguments, store=store)
    if name == PROPOSE_SPEC_TOOL:
        return _propose_specification(arguments, store=store, sqx_home=sqx_home, environ=environ)
    if name == REQUEST_COMPILE_TOOL:
        return _request_compile(arguments, store=store, sqx_home=sqx_home, environ=environ)
    if name == REQUEST_LAUNCH_TOOL:
        return _request_launch(arguments, store=store)
    return _error("unknown_tool", "that tool is not approved")
