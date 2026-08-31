"""Canonical runtime/status read model for the TraderCockpit desktop."""

from __future__ import annotations

from pathlib import Path
from typing import Any

from tradercockpit.research_custody import research_custody_capability_record
from tradercockpit.sqx_presets import SQX_BUILD, SqxPresetRuntimeError, verified_sqx_home


RUNTIME_STATUS_SCHEMA = "tc.runtime-status.v1"


def _unavailable(reason_code: str, detail: str) -> dict[str, object]:
    return {
        "status": "unavailable",
        "reason_code": reason_code,
        "detail": detail,
    }


def _research_backend_status(sqx_home: Path | str | None) -> dict[str, object]:
    configured = sqx_home is not None
    try:
        verified_sqx_home(sqx_home)
    except SqxPresetRuntimeError as exc:
        status = "invalid" if configured else "unavailable"
        return {
            "status": status,
            "configured": configured,
            "verified": False,
            "producer": "strategyquant-x",
            "build": None,
            "reason_code": exc.code,
            "detail": exc.detail,
            "inspection": {
                "available": False,
                "reason_code": exc.code,
            },
            "execution": {
                "available": False,
                "reason_code": "trusted_native_gateway_not_implemented",
                "launcher_sha256": None,
            },
        }

    return {
        "status": "ready",
        "configured": True,
        "verified": True,
        "producer": "strategyquant-x",
        "build": SQX_BUILD,
        "reason_code": None,
        "detail": f"Verified StrategyQuant X {SQX_BUILD} runtime for read-only research inspection.",
        "inspection": {
            "available": True,
            "reason_code": None,
        },
        "execution": {
            "available": False,
            "reason_code": "trusted_native_gateway_not_implemented",
            "launcher_sha256": None,
        },
    }


def _research_custody_status() -> dict[str, object]:
    return {
        "status": "unavailable",
        "reason_code": "store_not_bound",
        "detail": "Research custody primitives are implemented, but no canonical application data root/store is bound yet.",
        "contract": research_custody_capability_record(),
    }


def runtime_status_record(sqx_home: Path | str | None = None) -> dict[str, Any]:
    """Return the canonical, secret-free application readiness snapshot.

    This read model intentionally reports unavailable capabilities instead of
    inferring readiness from frontend state or from the mere presence of files.
    Native execution remains disabled until one trusted launcher/gateway contract
    exists and proves the exact launcher identity before process execution.
    """

    return {
        "schema": RUNTIME_STATUS_SCHEMA,
        "application": {
            "status": "ready",
            "server": "canonical",
            "desktop": "canonical-server-ui",
        },
        "research_backend": _research_backend_status(sqx_home),
        "research_custody": _research_custody_status(),
        "market_data": _unavailable(
            "producer_not_configured",
            "No live/current market-data producer is configured.",
        ),
        "account": _unavailable(
            "authority_not_implemented",
            "Consumer account authority is not implemented yet.",
        ),
        "model": _unavailable(
            "policy_not_implemented",
            "Consumer model policy is not implemented yet.",
        ),
        "provider": _unavailable(
            "provider_not_configured",
            "No consumer model-provider authority is configured.",
        ),
        "extensions": _unavailable(
            "manifest_not_implemented",
            "Capability/add-on manifest authority is not implemented yet.",
        ),
    }
