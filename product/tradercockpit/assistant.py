"""Bounded Assistant (Apollo) backend over the OpenRouter workhorse transport.

The assistant is application mechanics: it explains the cockpit's own read models and
helps the user operate the product.  It never owns producer truth, never mutates native
SQX state, and never receives the provider credential in browser code.  The operator
environment holds the OpenRouter key (``OPENROUTER_API_KEY``); model/provider/fallback
policy is backend configuration with ``z-ai/glm-5.3-flash`` as the default workhorse.
"""

from __future__ import annotations

import json
import os
from typing import Callable
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from tradercockpit.assistant_knowledge import format_grounding, knowledge_status, retrieve_passages


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
    }


def _system_prompt(context: dict[str, object] | None, grounding: str | None = None) -> str:
    lines = [
        f"You are {ASSISTANT_IDENTITY}, the bounded assistant inside TraderCockpit, a desktop trading research platform.",
        "StrategyQuant X (SQX) is the native historical-research producer: it owns strategy authoring, Builder generation, backtesting, robustness cross-checks, optimisation and native result artifacts.",
        "TraderCockpit owns application mechanics: custody of Ideas, configurations, native jobs, Candidates, Historical Results, Proofs, the cockpit validation verdict, presentation and runtime verification.",
        "The cockpit validation verdict (Research > Test & Validate) recomputes SQX statistics over the exact native trade records of a completed Historical Result, evaluates the approved native Rankings and Higher Precision acceptance conditions (Initial Test, Fast Validation), applies cockpit policy for Golden Validation, Scenario Tests, seeded Monte Carlo Stress Tests and Out-of-Sample, and records Proof custody as Evidence; SQX produces the trades, the cockpit computes the verdict.",
        "Rules: never invent market prices, signals, balances, P&L, candidate identities or validation outcomes. If the context below does not contain a fact, say it is not connected or not available yet.",
        "Quant-Guild excerpts below are reference data for anti-hallucination. They are not producer truth and do not authorize invented statistics. If they do not cover the question, say so.",
        "You cannot mutate native SQX state or launch processes; describe what the user can do in the cockpit instead.",
        "Answer concisely in plain prose. Use the surfaces Home, Research (Signals & Models, Evolutionary Search, Test & Validate, Indicators & Models Catalog), Explore, Automation, Operate, Settings when directing the user.",
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


def build_messages(
    message: str,
    history: object,
    context: dict[str, object] | None,
    *,
    environ: dict[str, str] | None = None,
    corpus_path: object | None = None,
) -> list[dict[str, str]]:
    if not isinstance(message, str) or not message.strip():
        raise AssistantError("assistant_message_invalid", "message must be a non-empty string")
    if len(message) > MAX_MESSAGE_CHARS:
        raise AssistantError("assistant_message_invalid", f"message exceeds {MAX_MESSAGE_CHARS} characters")
    knowledge = knowledge_status(environ=environ, corpus_path=corpus_path)  # type: ignore[arg-type]
    if knowledge["status"] != "ready":
        grounding = "Quant-Guild knowledge library is not connected; do not invent library content."
    else:
        grounding = format_grounding(retrieve_passages(message.strip(), environ=environ, corpus_path=corpus_path))  # type: ignore[arg-type]
    return [
        {"role": "system", "content": _system_prompt(context, grounding)},
        *_clean_history(history),
        {"role": "user", "content": message.strip()},
    ]


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
    content = choice.get("message", {}).get("content") if isinstance(choice, dict) else None
    if isinstance(content, list):
        content = "".join(part.get("text", "") for part in content if isinstance(part, dict))
    if not isinstance(content, str) or not content.strip():
        raise AssistantError("assistant_provider_invalid", "OpenRouter returned an empty completion", status=502)
    usage = payload.get("usage") if isinstance(payload.get("usage"), dict) else {}
    return {
        "reply": content.strip(),
        "model": payload.get("model") if isinstance(payload.get("model"), str) else model,
        "usage": {
            "prompt_tokens": usage.get("prompt_tokens"),
            "completion_tokens": usage.get("completion_tokens"),
            "total_tokens": usage.get("total_tokens"),
        },
        "provider_request_id": payload.get("id") if isinstance(payload.get("id"), str) else None,
    }


def request_completion(
    messages: list[dict[str, str]],
    *,
    environ: dict[str, str] | None = None,
    transport: Transport | None = None,
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
        body = json.dumps({
            "model": model,
            "messages": messages,
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
            continue
        return {**result, "requested_model": model, "fallback_used": model != policy["model"]}
    assert last_error is not None
    raise last_error


def assistant_reply(
    payload: dict[str, object],
    *,
    environ: dict[str, str] | None = None,
    transport: Transport | None = None,
    context: dict[str, object] | None = None,
) -> tuple[int, dict[str, object]]:
    """HTTP-neutral POST handler: validate, call the provider, return a typed reply or error."""

    if not isinstance(payload, dict) or set(payload) - {"message", "history"} or "message" not in payload:
        return 400, {"error": "invalid_request", "reason_code": "assistant_request_invalid", "detail": "body must be {message, history?}"}
    try:
        messages = build_messages(payload.get("message"), payload.get("history"), context, environ=environ)  # type: ignore[arg-type]
        completion = request_completion(messages, environ=environ, transport=transport)
    except AssistantError as exc:
        error = "invalid_request" if exc.status == 400 else "producer_not_configured" if exc.status == 503 else "provider_failed"
        return exc.status, {"error": error, "reason_code": exc.code, "detail": exc.detail}
    return 200, {
        "schema": ASSISTANT_REPLY_SCHEMA,
        "identity": ASSISTANT_IDENTITY,
        "reply": completion["reply"],
        "model": completion["model"],
        "requested_model": completion["requested_model"],
        "fallback_used": completion["fallback_used"],
        "usage": completion["usage"],
        "provider_request_id": completion["provider_request_id"],
    }
