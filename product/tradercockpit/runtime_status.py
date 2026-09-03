"""Canonical runtime/status read model for the TraderCockpit desktop."""

from __future__ import annotations

from datetime import datetime
from pathlib import Path
from typing import Any

from tradercockpit.assistant import assistant_status_record
from tradercockpit.consumer_account import account_status_record
from tradercockpit.data_maintenance import data_maintenance_status
from tradercockpit.openrouter_credits import credits_status_record
from tradercockpit.stripe_membership import membership_status_record
from tradercockpit.home_market import (
    MarketOverviewObservation,
    error_market_overview_record,
    market_overview_record,
)
from tradercockpit.macro_series import macro_series_record
from tradercockpit.market_data import market_quotes_record, watchlist_from_env
from tradercockpit.operate_live_state import (
    live_deployment_record,
    live_risk_record,
    live_signals_record,
    scoped_performance_record,
)
from tradercockpit.operate_prop_simulation import prop_simulation_record
from tradercockpit.research_custody import research_custody_capability_record
from tradercockpit.extensions import extensions_status_record
from tradercockpit.secrets_store import secrets_status_record
from tradercockpit.sqx_runtime import sqx_runtime_descriptor


RUNTIME_STATUS_SCHEMA = "tc.runtime-status.v1"


def _unavailable(reason_code: str, detail: str) -> dict[str, object]:
    return {
        "status": "unavailable",
        "reason_code": reason_code,
        "detail": detail,
    }


def _research_backend_status(
    sqx_home: Path | str | None,
    trusted_launcher_sha256: str | None,
) -> dict[str, object]:
    runtime = sqx_runtime_descriptor(sqx_home, trusted_launcher_sha256)
    build = runtime["build"]
    launcher = runtime["launcher"]
    execution = runtime["execution"]
    build_verified = isinstance(build, dict) and build.get("verified") is True
    if not build_verified:
        reason_code = build.get("reason_code") if isinstance(build, dict) else "runtime_invalid"
        return {
            "status": runtime["status"],
            "configured": sqx_home is not None,
            "verified": False,
            "producer": "strategyquant-x",
            "build": None,
            "reason_code": reason_code,
            "detail": "Native research runtime build is not verified.",
            "runtime": runtime,
            "inspection": runtime["inspection"],
            "execution": {
                "available": False,
                "reason_code": execution["reason_code"],
                "launcher_verified": False,
                "launcher_sha256": None,
                "gateway_implemented": execution["gateway_implemented"],
                "gateway_available": False,
                "requires_approved_configuration": True,
            },
        }

    launcher_verified = isinstance(launcher, dict) and launcher.get("verified") is True
    execution_available = isinstance(execution, dict) and execution.get("available") is True
    return {
        "status": "ready",
        "configured": True,
        "verified": True,
        "producer": "strategyquant-x",
        "build": build["observed"],
        "reason_code": None,
        "detail": f"Verified StrategyQuant X {build['observed']} runtime for native research inspection and approval-gated Builder control.",
        "runtime": runtime,
        "inspection": runtime["inspection"],
        "execution": {
            "available": execution_available,
            "reason_code": execution["reason_code"],
            "launcher_verified": launcher_verified,
            "launcher_sha256": launcher.get("observed_sha256") if isinstance(launcher, dict) else None,
            "gateway_implemented": execution["gateway_implemented"],
            "gateway_available": execution["gateway_available"],
            "requires_approved_configuration": True,
        },
    }


def _research_custody_status(bound: bool) -> dict[str, object]:
    contract = research_custody_capability_record()
    if bound:
        return {
            "status": "ready",
            "reason_code": None,
            "detail": "Canonical local research custody store is bound.",
            "contract": contract,
        }
    return {
        "status": "unavailable",
        "reason_code": "store_not_bound",
        "detail": "Research custody primitives are implemented, but no canonical application data root/store is bound yet.",
        "contract": contract,
    }


def _market_data_status(market_provider: object | None) -> dict[str, object]:
    if market_provider is None:
        return market_overview_record()
    provider_id = getattr(market_provider, "provider_id", "connected")
    quotes = market_quotes_record(market_provider, watchlist_from_env(), provider_id=str(provider_id))
    if quotes.get("reason_code") == "provider_read_failed":
        return error_market_overview_record()
    rows = quotes.get("quotes") if isinstance(quotes.get("quotes"), list) else []
    first = rows[0] if rows and isinstance(rows[0], dict) else None
    observed = first.get("observed_at") if first else None
    symbol = first.get("symbol") if first else None
    if not isinstance(observed, str) or not isinstance(symbol, str):
        return market_overview_record()
    return market_overview_record(
        MarketOverviewObservation(
            producer=str(provider_id),
            observed_at=datetime.fromisoformat(observed.replace("Z", "+00:00")),
            instrument=symbol,
        )
    )


def runtime_status_record(
    sqx_home: Path | str | None = None,
    trusted_launcher_sha256: str | None = None,
    *,
    research_store_bound: bool = False,
    market_provider: object | None = None,
    macro_provider: object | None = None,
    data_root: Path | str | None = None,
) -> dict[str, Any]:
    """Return the canonical, secret-free application readiness snapshot.

    This read model reports whether the trusted native control boundary is available,
    but it is never authorization for a particular launch. Every Builder launch still
    requires one exact current approved configuration and fresh launcher/config
    verification inside the native gateway immediately before process creation.
    """

    if not isinstance(research_store_bound, bool):
        raise ValueError("research_store_bound must be boolean")

    assistant = assistant_status_record(data_root=data_root)
    credits = credits_status_record(data_root)
    provider_ready = assistant["status"] == "ready"
    return {
        "schema": RUNTIME_STATUS_SCHEMA,
        "application": {
            "status": "ready",
            "server": "canonical",
            "desktop": "canonical-server-ui",
        },
        "research_backend": _research_backend_status(sqx_home, trusted_launcher_sha256),
        "research_custody": _research_custody_status(research_store_bound),
        "market_data": _market_data_status(market_provider),
        "macro_series": macro_series_record(macro_provider),
        "account": account_status_record(data_root),
        "membership": membership_status_record(data_root),
        "model_credits": credits,
        "model": {
            "status": "ready" if provider_ready else "unavailable",
            "reason_code": None if provider_ready else "provider_not_configured",
            "detail": (
                f"Backend model policy: {assistant['model']} on {assistant['provider']}."
                if provider_ready
                else f"Backend model policy is {assistant['model']}, but the provider credential is not configured."
            ),
            "default_model": assistant["model"],
            "fallback_models": assistant["fallback_models"],
            "policy_source": "backend",
        },
        "provider": {
            "status": assistant["status"],
            "reason_code": assistant["reason_code"],
            "detail": assistant["detail"],
            "provider": assistant["provider"],
            "transport": assistant["transport"],
            "credential_scope": assistant["credential_scope"],
            "spend_boundary": assistant["spend_boundary"],
        },
        "assistant": assistant,
        "secrets": secrets_status_record(),
        "extensions": extensions_status_record(data_root),
        "data_maintenance": data_maintenance_status(data_root),
        "live_signals": live_signals_record(),
        "live_risk": live_risk_record(),
        "scoped_performance": scoped_performance_record(),
        "live_deployment": live_deployment_record(),
        "prop_simulation": prop_simulation_record(),
    }
