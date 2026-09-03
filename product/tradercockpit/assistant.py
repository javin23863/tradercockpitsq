"""Bounded Assistant (Apollo) backend over the OpenRouter workhorse transport.

The assistant is application mechanics: it explains the cockpit's own read models and
helps the user operate the product.  It never owns producer truth, never mutates native
SQX state directly, and never receives the provider credential in browser code.  Approved
product tools propose the same custody APIs a human click would; mutations still require
owner confirmation.  The operator environment holds the OpenRouter key
(``OPENROUTER_API_KEY``); model/provider/fallback policy is backend configuration with
``z-ai/glm-5.3-flash`` as the default workhorse.
"""

from __future__ import annotations

import json
import os
from typing import Callable
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from tradercockpit.assistant_product_tools import PRODUCT_TOOL_NAMES, PRODUCT_TOOL_SPECS, dispatch_product_tool
from tradercockpit.knowledge import (
    format_grounding,
    knowledge_reply_record,
    knowledge_status,
    retrieve_knowledge,
)


ASSISTANT_API_PATH = "/api/assistant"
ASSISTANT_STATUS_SCHEMA = "tc.assistant-status.v1"
ASSISTANT_REPLY_SCHEMA = "tc.assistant-reply.v1"
ASSISTANT_IDENTITY = "Apollo"

OPENROUTER_API_KEY_ENV = "OPENROUTER_API_KEY"
ASSISTANT_MODEL_ENV = "TRADERCOCKPIT_ASSISTANT_MODEL"
ASSISTANT_FALLBACK_MODELS_ENV = "TRADERCOCKPIT_ASSISTANT_FALLBACK_MODELS"
ASSISTANT_MAX_OUTPUT_TOKENS_ENV = "TRADERCOCKPIT_ASSISTANT_MAX_OUTPUT_TOKENS"
OPENROUTER_CHAT_COMPLETIONS_URL = "https://openrouter.ai/api/v1/chat/completions"

DEFAULT_ASSISTANT_MODEL = "z-ai/glm-5.3-flash"
DEFAULT_MAX_OUTPUT_TOKENS = 700
MAX_MESSAGE_CHARS = 4000
MAX_HISTORY_MESSAGES = 12
MAX_HISTORY_CHARS = 16000
REQUEST_TIMEOUT_SECONDS = 45
MAX_TOOL_ROUNDS = 2
RETRIEVE_TOOL = "retrieve_quant_guild"
RETRIEVE_TOOL_SPEC = {
    "type": "function",
    "function": {
        "name": RETRIEVE_TOOL,
        "description": (
            "Retrieve Quant-Guild catalog notes (lecture titles, source URLs, and "
            "platform-authored cockpit notes) for a research question. Reference data "
            "only; not lecture transcripts, not producer truth, and not a reason to invent statistics."
        ),
        "parameters": {
            "type": "object",
            "properties": {"query": {"type": "string", "description": "Search query for catalog notes"}},
            "required": ["query"],
        },
    },
}
ASSISTANT_TOOLS = (RETRIEVE_TOOL_SPEC, *PRODUCT_TOOL_SPECS)
APPROVED_TOOL_NAMES = (RETRIEVE_TOOL, *PRODUCT_TOOL_NAMES)

Transport = Callable[[str, bytes, dict[str, str]], tuple[int, bytes]]


class AssistantError(ValueError):
    def __init__(self, code: str, detail: str, *, status: int = 400) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail
        self.status = status


def _environ(environ: dict[str, str] | None) -> dict[str, str]:
    return os.environ if environ is None else environ  # type: ignore[return-value]


def assistant_policy(environ: dict[str, str] | None = None) -> dict[str, object]:
    """Backend model-routing policy (secret-free)."""

    env = _environ(environ)
    model = (env.get(ASSISTANT_MODEL_ENV) or "").strip() or DEFAULT_ASSISTANT_MODEL
    fallbacks = [item.strip() for item in (env.get(ASSISTANT_FALLBACK_MODELS_ENV) or "").split(",") if item.strip() and item.strip() != model]
    try:
        max_tokens = int(env.get(ASSISTANT_MAX_OUTPUT_TOKENS_ENV) or DEFAULT_MAX_OUTPUT_TOKENS)
    except ValueError:
        max_tokens = DEFAULT_MAX_OUTPUT_TOKENS
    max_tokens = max(64, min(max_tokens, 4000))
    key = (env.get(OPENROUTER_API_KEY_ENV) or "").strip()
    return {
        "provider": "openrouter",
        "transport": "openai-compatible-chat",
        "model": model,
        "fallback_models": fallbacks,
        "max_output_tokens": max_tokens,
        "configured": bool(key),
        "credential_scope": "operator",
    }


def assistant_status_record(environ: dict[str, str] | None = None) -> dict[str, object]:
    """Secret-free readiness record for `/api/status` and `/api/assistant` GET."""

    policy = assistant_policy(environ)
    configured = bool(policy["configured"])
    return {
        "schema": ASSISTANT_STATUS_SCHEMA,
        "identity": ASSISTANT_IDENTITY,
        "status": "ready" if configured else "unavailable",
        "reason_code": None if configured else "provider_not_configured",
        "detail": (
            f"Assistant ready on OpenRouter with backend model policy ({policy['model']})."
            if configured
            else f"Set {OPENROUTER_API_KEY_ENV} in the operator environment to enable the assistant transport."
        ),
        "provider": policy["provider"],
        "transport": policy["transport"],
        "model": policy["model"],
        "fallback_models": policy["fallback_models"],
        "max_output_tokens": policy["max_output_tokens"],
        "credential_scope": policy["credential_scope"],
        "spend_boundary": {
            "provider_enforced": False,
            "detail": "Operator credential on the development desktop; per-consumer provider-enforced limits arrive with consumer account authority.",
        },
        "knowledge": knowledge_status(environ=environ),
        "tools": {
            "approved": list(APPROVED_TOOL_NAMES),
            "native_mutation": False,
            "detail": (
                "Backend-only approved tools. Product tools propose the same custody APIs a human "
                "click would; mutations require confirmation. The assistant cannot invoke sqcli or write executable XML."
            ),
        },
    }


def _system_prompt(context: dict[str, object] | None, grounding: str | None = None) -> str:
    lines = [
        f"You are {ASSISTANT_IDENTITY}, the bounded assistant inside TraderCockpit, a desktop trading research platform.",
        "StrategyQuant X (SQX) is the native historical-research producer: it owns strategy authoring, Builder generation, backtesting, robustness cross-checks, optimisation and native result artifacts.",
        "TraderCockpit owns application mechanics: custody of Ideas, configurations, native jobs, Candidates, Historical Results, Proofs, the cockpit validation verdict, presentation and runtime verification.",
        "The cockpit validation verdict (Research > Test & Validate) recomputes SQX statistics over the exact native trade records of a completed Historical Result, evaluates the approved native Rankings and Higher Precision acceptance conditions (Initial Test, Fast Validation), applies cockpit policy for Golden Validation, Scenario Tests, seeded Monte Carlo Stress Tests and Out-of-Sample, and records Proof custody as Evidence; SQX produces the trades, the cockpit computes the verdict.",
        "Rules: never invent market prices, signals, balances, P&L, candidate identities or validation outcomes. If the context below does not contain a fact, say it is not connected or not available yet.",
        "You cannot mutate native SQX state directly, write executable XML, invoke sqcli, or skip runtime verification.",
        "Approved product tools propose the same custody APIs a human click would. Mutating proposals still require owner confirmation in the widget.",
        "If clarifying_questions.current_question is present, ask that exact prompt and only name its allowed_answers. Do not invent other answers, symbols, timeframes, or unlock Build while required fields remain unresolved.",
        f"You may call {RETRIEVE_TOOL} for extra Quant-Guild catalog notes.",
        "You may call navigate_surface with a canonical product path, draft_idea_revision with Idea text, propose_specification_fields with an open field_id and one allowed answer_id, request_compile, and request_launch only after an approved configuration exists.",
        "Do not invent object_kind, ingest spans, executable XML, or executable paths.",
        "Answer concisely in plain prose. Use the surfaces Home, Research (Signals & Models, Evolutionary Search, Test & Validate, Indicators & Models Catalog), Explore, Automation, Operate, Settings when directing the user.",
        "When Quant-Guild catalog notes are present, cite the lecture title if you use them. Do not reproduce lecture mathematics or invent formulas from the notes.",
    ]
    if grounding:
        lines.append(grounding)
    if context:
        lines.append("Current cockpit read-model context (JSON, truthful, may contain unavailable states):")
        lines.append(json.dumps(context, ensure_ascii=False, sort_keys=True, separators=(",", ":"))[:12000])
    return "\n".join(lines)


def _clean_history(history: object) -> list[dict[str, str]]:
    if history is None:
        return []
    if not isinstance(history, list):
        raise AssistantError("assistant_history_invalid", "history must be a list of {role, content} messages")
    cleaned: list[dict[str, str]] = []
    for item in history[-MAX_HISTORY_MESSAGES:]:
        if not isinstance(item, dict):
            raise AssistantError("assistant_history_invalid", "history entries must be objects")
        role = item.get("role")
        content = item.get("content")
        if role not in {"user", "assistant"} or not isinstance(content, str) or not content.strip():
            raise AssistantError("assistant_history_invalid", "history entries need role user|assistant and non-empty content")
        cleaned.append({"role": role, "content": content.strip()[:MAX_MESSAGE_CHARS]})
    total = sum(len(item["content"]) for item in cleaned)
    while cleaned and total > MAX_HISTORY_CHARS:
        total -= len(cleaned.pop(0)["content"])
    return cleaned


def build_grounded_messages(
    message: str,
    history: object,
    context: dict[str, object] | None,
    *,
    environ: dict[str, str] | None = None,
    catalog_path: object = None,
) -> tuple[list[dict[str, str]], dict[str, object]]:
    if not isinstance(message, str) or not message.strip():
        raise AssistantError("assistant_message_invalid", "message must be a non-empty string")
    if len(message) > MAX_MESSAGE_CHARS:
        raise AssistantError("assistant_message_invalid", f"message exceeds {MAX_MESSAGE_CHARS} characters")
    retrieval = retrieve_knowledge(message.strip(), history=history, environ=environ, catalog_path=catalog_path)  # type: ignore[arg-type]
    return [
        {"role": "system", "content": _system_prompt(context, format_grounding(retrieval))},
        *_clean_history(history),
        {"role": "user", "content": message.strip()},
    ], retrieval


def build_messages(
    message: str,
    history: object,
    context: dict[str, object] | None,
    *,
    environ: dict[str, str] | None = None,
    catalog_path: object = None,
) -> list[dict[str, str]]:
    messages, _retrieval = build_grounded_messages(
        message,
        history,
        context,
        environ=environ,
        catalog_path=catalog_path,
    )
    return messages


def _urllib_transport(url: str, body: bytes, headers: dict[str, str]) -> tuple[int, bytes]:
    request = Request(url, data=body, headers=headers, method="POST")
    try:
        with urlopen(request, timeout=REQUEST_TIMEOUT_SECONDS) as response:  # noqa: S310 - fixed provider URL
            return int(response.status), response.read()
    except HTTPError as exc:
        return int(exc.code), exc.read()
    except URLError as exc:
        raise AssistantError("assistant_provider_unreachable", f"OpenRouter is unreachable: {exc.reason}", status=502) from exc
    except TimeoutError as exc:
        raise AssistantError("assistant_provider_timeout", "OpenRouter did not answer in time", status=504) from exc


def _parse_completion(status: int, body: bytes, model: str) -> dict[str, object]:
    try:
        payload = json.loads(body.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise AssistantError("assistant_provider_invalid", f"OpenRouter returned a non-JSON response ({status})", status=502) from exc
    if status >= 400 or not isinstance(payload, dict) or not payload.get("choices"):
        error = payload.get("error") if isinstance(payload, dict) else None
        detail = error.get("message") if isinstance(error, dict) and isinstance(error.get("message"), str) else f"OpenRouter request failed ({status})"
        code = "assistant_provider_rejected" if status in {401, 402, 403} else "assistant_provider_error"
        raise AssistantError(code, detail, status=502 if status >= 500 else 503)
    choice = payload["choices"][0] if isinstance(payload["choices"], list) and payload["choices"] else None
    message = choice.get("message") if isinstance(choice, dict) else None
    if not isinstance(message, dict):
        raise AssistantError("assistant_provider_invalid", "OpenRouter returned an empty completion", status=502)
    usage = payload.get("usage") if isinstance(payload.get("usage"), dict) else {}
    meta = {
        "model": payload.get("model") if isinstance(payload.get("model"), str) else model,
        "usage": {
            "prompt_tokens": usage.get("prompt_tokens"),
            "completion_tokens": usage.get("completion_tokens"),
            "total_tokens": usage.get("total_tokens"),
        },
        "provider_request_id": payload.get("id") if isinstance(payload.get("id"), str) else None,
    }
    tool_calls = message.get("tool_calls")
    if isinstance(tool_calls, list) and tool_calls:
        return {**meta, "tool_calls": tool_calls, "reply": None}
    content = message.get("content")
    if isinstance(content, list):
        content = "".join(part.get("text", "") for part in content if isinstance(part, dict))
    if not isinstance(content, str) or not content.strip():
        raise AssistantError("assistant_provider_invalid", "OpenRouter returned an empty completion", status=502)
    return {**meta, "reply": content.strip(), "tool_calls": None}


def _merge_retrieval(base: dict[str, object], extra: dict[str, object]) -> dict[str, object]:
    if extra.get("state") != "grounded":
        return base
    citations = [item for item in (base.get("citations") or []) if isinstance(item, dict)]
    seen = {item.get("id") for item in citations}
    for item in extra.get("citations") or []:
        if isinstance(item, dict) and item.get("id") not in seen:
            citations.append(item)
            seen.add(item.get("id"))
    return {
        **base,
        "state": "grounded",
        "reason_code": None,
        "citations": citations,
        "library": extra.get("library") or base.get("library"),
    }


def _parse_tool_arguments(raw_arguments: object) -> dict[str, object] | None:
    if raw_arguments in (None, ""):
        return {}
    try:
        arguments = json.loads(raw_arguments) if isinstance(raw_arguments, str) else raw_arguments
    except json.JSONDecodeError:
        return None
    return arguments if isinstance(arguments, dict) else None


def _tool_result(
    name: str,
    raw_arguments: object,
    *,
    environ: dict[str, str] | None,
    catalog_path: object | None,
    research_store=None,
    sqx_home=None,
) -> tuple[str, dict[str, str] | None, dict[str, object] | None, dict[str, object] | None]:
    if name == RETRIEVE_TOOL:
        arguments = _parse_tool_arguments(raw_arguments)
        if arguments is None:
            return json.dumps({"error": "invalid_arguments", "detail": "tool arguments must be JSON"}, sort_keys=True), None, None, None
        if set(arguments) != {"query"} or not isinstance(arguments.get("query"), str) or not arguments["query"].strip():
            return json.dumps({"error": "invalid_arguments", "detail": "retrieve_quant_guild accepts only query"}, sort_keys=True), None, None, None
        retrieval = retrieve_knowledge(
            arguments["query"].strip(),
            environ=environ,
            catalog_path=catalog_path,  # type: ignore[arg-type]
        )
        return format_grounding(retrieval), {"name": RETRIEVE_TOOL, "query": arguments["query"].strip()[:MAX_MESSAGE_CHARS]}, retrieval, None
    if name in PRODUCT_TOOL_NAMES:
        arguments = _parse_tool_arguments(raw_arguments)
        if arguments is None:
            return json.dumps({"error": "invalid_arguments", "detail": "tool arguments must be JSON"}, sort_keys=True), None, None, None
        result = dispatch_product_tool(
            name,
            arguments,
            store=research_store,
            sqx_home=sqx_home,
            environ=environ,
        )
        return result.content, result.used, None, result.proposed_action
    return json.dumps({"error": "unknown_tool", "detail": "that tool is not approved"}, sort_keys=True), None, None, None


def request_completion(
    messages: list[dict[str, object]],
    *,
    environ: dict[str, str] | None = None,
    transport: Transport | None = None,
    catalog_path: object | None = None,
    research_store=None,
    sqx_home=None,
) -> dict[str, object]:
    """Call the configured workhorse model, falling back through the backend fallback list."""

    env = _environ(environ)
    policy = assistant_policy(env)
    key = (env.get(OPENROUTER_API_KEY_ENV) or "").strip()
    if not key:
        raise AssistantError("provider_not_configured", f"Set {OPENROUTER_API_KEY_ENV} in the operator environment.", status=503)
    send = transport or _urllib_transport
    headers = {
        "Authorization": f"Bearer {key}",
        "Content-Type": "application/json",
        "Accept": "application/json",
        "HTTP-Referer": "https://tradercockpit.local/",
        "X-Title": "TraderCockpit",
    }
    last_error: AssistantError | None = None
    for model in [policy["model"], *policy["fallback_models"]]:  # type: ignore[list-item]
        pending: list[dict[str, object]] = [dict(item) for item in messages]
        tools_used: list[dict[str, str]] = []
        tool_retrievals: list[dict[str, object]] = []
        proposed_actions: list[dict[str, object]] = []
        for round_index in range(MAX_TOOL_ROUNDS + 1):
            body = json.dumps({
                "model": model,
                "messages": pending,
                "tools": list(ASSISTANT_TOOLS),
                "max_tokens": policy["max_output_tokens"],
                "temperature": 0.3,
            }).encode("utf-8")
            try:
                status, raw = send(OPENROUTER_CHAT_COMPLETIONS_URL, body, headers)
                result = _parse_completion(status, raw, str(model))
            except AssistantError as exc:
                last_error = exc
                if exc.code in {"assistant_provider_rejected", "assistant_provider_unreachable", "assistant_provider_timeout"}:
                    raise
                break
            tool_calls = result.get("tool_calls")
            if isinstance(tool_calls, list) and tool_calls:
                if round_index >= MAX_TOOL_ROUNDS:
                    last_error = AssistantError(
                        "assistant_tool_loop_exhausted",
                        "the assistant exceeded approved tool rounds",
                        status=502,
                    )
                    break
                pending.append({"role": "assistant", "content": None, "tool_calls": tool_calls})
                malformed = False
                for call in tool_calls:
                    if not isinstance(call, dict):
                        last_error = AssistantError("assistant_provider_invalid", "OpenRouter returned a malformed tool call", status=502)
                        malformed = True
                        break
                    function = call.get("function") if isinstance(call.get("function"), dict) else {}
                    content, used, retrieval, proposed = _tool_result(
                        str(function.get("name") or ""),
                        function.get("arguments"),
                        environ=env,
                        catalog_path=catalog_path,
                        research_store=research_store,
                        sqx_home=sqx_home,
                    )
                    if used:
                        tools_used.append(used)
                    if retrieval is not None:
                        tool_retrievals.append(retrieval)
                    if proposed is not None:
                        proposed_actions.append(proposed)
                    pending.append({"role": "tool", "tool_call_id": call.get("id"), "content": content})
                if malformed:
                    break
                continue
            return {
                **result,
                "requested_model": model,
                "fallback_used": model != policy["model"],
                "tools_used": tools_used,
                "tool_retrievals": tool_retrievals,
                "proposed_actions": proposed_actions,
            }
        if last_error is not None and last_error.code in {
            "assistant_provider_rejected",
            "assistant_provider_unreachable",
            "assistant_provider_timeout",
        }:
            raise last_error
    if last_error is not None:
        raise last_error
    raise AssistantError("assistant_provider_invalid", "OpenRouter returned an empty completion", status=502)


def assistant_reply(
    payload: dict[str, object],
    *,
    environ: dict[str, str] | None = None,
    transport: Transport | None = None,
    context: dict[str, object] | None = None,
    catalog_path: object | None = None,
    research_store=None,
    sqx_home=None,
) -> tuple[int, dict[str, object]]:
    """HTTP-neutral POST handler: validate, call the provider, return a typed reply or error."""

    if not isinstance(payload, dict) or set(payload) - {"message", "history"} or "message" not in payload:
        return 400, {"error": "invalid_request", "reason_code": "assistant_request_invalid", "detail": "body must be {message, history?}"}
    try:
        messages, retrieval = build_grounded_messages(
            payload.get("message"),
            payload.get("history"),
            context,
            environ=environ,
            catalog_path=catalog_path,
        )  # type: ignore[arg-type]
        completion = request_completion(
            messages,
            environ=environ,
            transport=transport,
            catalog_path=catalog_path,
            research_store=research_store,
            sqx_home=sqx_home,
        )
    except AssistantError as exc:
        error = "invalid_request" if exc.status == 400 else "producer_not_configured" if exc.status == 503 else "provider_failed"
        return exc.status, {"error": error, "reason_code": exc.code, "detail": exc.detail}
    for extra in completion.get("tool_retrievals") or []:
        if isinstance(extra, dict):
            retrieval = _merge_retrieval(retrieval, extra)
    return 200, {
        "schema": ASSISTANT_REPLY_SCHEMA,
        "identity": ASSISTANT_IDENTITY,
        "reply": completion["reply"],
        "model": completion["model"],
        "requested_model": completion["requested_model"],
        "fallback_used": completion["fallback_used"],
        "usage": completion["usage"],
        "provider_request_id": completion["provider_request_id"],
        "knowledge": knowledge_reply_record(retrieval),
        "tools_used": completion.get("tools_used") or [],
        "proposed_actions": completion.get("proposed_actions") or [],
    }
