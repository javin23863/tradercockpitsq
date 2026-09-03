"""Process-side MCP identities.

TradingView and MetaTrader 5 MCP are Apollo/LLM tools so the assistant can talk to
those platforms. They are not the robustness pipeline, not Automation, and not a
substitute StrategyQuant X engine. Endpoints and tokens stay in the operator
environment. Tokens never appear in the read model.

StrategyQuant X MCP is the retained Custom Project control transport
(list_projects, list_databanks, list_strategies, get_strategy_stats, run_project,
stop_project). This module does not invent extra SQX MCP methods and does not
perform JSON-RPC calls.
"""

from __future__ import annotations

import os
from urllib.parse import urlsplit


LIVE_PRODUCERS_SCHEMA = "tc.live-producers.v1"
LIVE_PRODUCERS_API_PATH = "/api/live-producers"

TRADINGVIEW_MCP_URL_ENV = "TRADERCOCKPIT_TRADINGVIEW_MCP_URL"
TRADINGVIEW_MCP_TOKEN_ENV = "TRADERCOCKPIT_TRADINGVIEW_MCP_TOKEN"
METATRADER_MCP_URL_ENV = "TRADERCOCKPIT_METATRADER_MCP_URL"
METATRADER_MCP_TOKEN_ENV = "TRADERCOCKPIT_METATRADER_MCP_TOKEN"
SQX_MCP_URL_ENV = "TRADERCOCKPIT_SQX_MCP_URL"
SQX_MCP_TOKEN_ENV = "TRADERCOCKPIT_SQX_MCP_TOKEN"

SQX_MCP_TOOLS = (
    "list_projects",
    "list_databanks",
    "list_strategies",
    "get_strategy_stats",
    "run_project",
    "stop_project",
)


def _endpoint_state(url_env: str) -> tuple[bool, str | None]:
    raw = os.environ.get(url_env, "")
    if not isinstance(raw, str) or not raw.strip():
        return False, None
    parsed = urlsplit(raw.strip())
    if parsed.scheme not in {"http", "https"} or not parsed.netloc:
        return False, "mcp_url_invalid"
    return True, None


def _credential_configured(token_env: str) -> bool:
    raw = os.environ.get(token_env, "")
    return isinstance(raw, str) and bool(raw.strip())


def _producer_record(
    *,
    producer_id: str,
    label: str,
    kind: str,
    job: str,
    url_env: str,
    token_env: str,
    purpose: str,
    native_tools: tuple[str, ...] = (),
) -> dict[str, object]:
    endpoint_configured, url_reason = _endpoint_state(url_env)
    credential_configured = _credential_configured(token_env)
    if url_reason == "mcp_url_invalid":
        reason_code = "mcp_url_invalid"
        detail = (
            f"{label} MCP URL is set but is not an http(s) endpoint. "
            f"Set {url_env} on the desktop process. The browser cannot choose this URL."
        )
    elif not endpoint_configured:
        reason_code = "mcp_url_not_configured"
        detail = (
            f"{label} MCP is not configured. Set {url_env} on the desktop process. "
            "The browser cannot choose this URL."
        )
    else:
        reason_code = "mcp_transport_unverified"
        detail = (
            f"{label} MCP endpoint is configured on the desktop process. "
            "Handshake is not claimed until the trusted transport is verified."
        )
    return {
        "id": producer_id,
        "label": label,
        "kind": kind,
        "purpose": purpose,
        "job": job,
        "transport": "mcp",
        "status": "unavailable",
        "reason_code": reason_code,
        "detail": detail,
        "endpoint_configured": endpoint_configured,
        "credential_configured": credential_configured,
        "live_quotes": False,
        "live_bars": False,
        "live_positions": False,
        "live_pnl": False,
        "native_tools": list(native_tools),
        "url_env": url_env,
        "token_env": token_env,
    }


def tradingview_producer_record() -> dict[str, object]:
    record = _producer_record(
        producer_id="tradingview",
        label="TradingView",
        kind="apollo_llm_tool",
        purpose="apollo_llm_tool",
        job="Apollo/LLM tool so the assistant can interact with TradingView. Not Automation and not the robustness pipeline.",
        url_env=TRADINGVIEW_MCP_URL_ENV,
        token_env=TRADINGVIEW_MCP_TOKEN_ENV,
    )
    if record["reason_code"] == "mcp_url_not_configured":
        record["detail"] = (
            "Apollo TradingView MCP is not configured. Set TRADERCOCKPIT_TRADINGVIEW_MCP_URL "
            "on the desktop process so the assistant can talk to TradingView. "
            "This is not Custom Project control and not a market-data producer for Home/Operate."
        )
    elif record["reason_code"] == "mcp_transport_unverified":
        record["detail"] = (
            "Apollo TradingView MCP endpoint is configured. Handshake is not claimed. "
            "This slot is an LLM tool, not the robustness pipeline."
        )
    return record


def metatrader_producer_record() -> dict[str, object]:
    record = _producer_record(
        producer_id="metatrader",
        label="MetaTrader 5",
        kind="apollo_llm_tool",
        purpose="apollo_llm_tool",
        job="Apollo/LLM tool so the assistant can interact with MetaTrader 5. Not Automation and not live Operate P&L.",
        url_env=METATRADER_MCP_URL_ENV,
        token_env=METATRADER_MCP_TOKEN_ENV,
    )
    if record["reason_code"] == "mcp_url_not_configured":
        record["detail"] = (
            "Apollo MetaTrader MCP is not configured. Set TRADERCOCKPIT_METATRADER_MCP_URL "
            "on the desktop process so the assistant can talk to MetaTrader 5. "
            "This is not Custom Project control and not the Operate broker producer."
        )
    elif record["reason_code"] == "mcp_transport_unverified":
        record["detail"] = (
            "Apollo MetaTrader MCP endpoint is configured. Handshake is not claimed. "
            "This slot is an LLM tool, not the robustness pipeline."
        )
    return record


def strategyquant_mcp_record() -> dict[str, object]:
    record = _producer_record(
        producer_id="strategyquant_mcp",
        label="StrategyQuant X MCP",
        kind="native_workflow_control",
        purpose="native_custom_project_control",
        job="Retained SQX 144.2953 MCP for Custom Project list/run/stop. Task execution stays native.",
        url_env=SQX_MCP_URL_ENV,
        token_env=SQX_MCP_TOKEN_ENV,
        native_tools=SQX_MCP_TOOLS,
    )
    if record["reason_code"] == "mcp_url_not_configured":
        record["detail"] = (
            "StrategyQuant X MCP is not connected. Set TRADERCOCKPIT_SQX_MCP_URL on the desktop "
            "process for the retained tools list_projects, run_project, and stop_project. "
            "The browser cannot choose this URL. Custom Project start stays fail-closed."
        )
    elif record["reason_code"] == "mcp_transport_unverified":
        record["detail"] = (
            "StrategyQuant X MCP endpoint is configured. run_project and stop_project stay "
            "fail-closed until the trusted native MCP transport is verified. Extra SQX MCP "
            "methods are not invented."
        )
    return record


def live_producers_record() -> dict[str, object]:
    """Secret-free live producer identities for /api/status and Settings/Operate."""

    tradingview = tradingview_producer_record()
    metatrader = metatrader_producer_record()
    strategyquant = strategyquant_mcp_record()
    return {
        "schema": LIVE_PRODUCERS_SCHEMA,
        "status": "unavailable",
        "reason_code": "live_producers_not_connected",
        "detail": (
            "TradingView and MetaTrader MCP are Apollo/LLM tools. StrategyQuant X MCP is the "
            "Custom Project control transport. This desktop does not mix those slots and does "
            "not fabricate quotes, positions, or Custom Project runs."
        ),
        "tradingview": tradingview,
        "metatrader": metatrader,
        "strategyquant_mcp": strategyquant,
        "producers": [tradingview, metatrader, strategyquant],
    }
