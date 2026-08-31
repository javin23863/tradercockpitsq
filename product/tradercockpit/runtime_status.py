"""Canonical runtime/status read model for the TraderCockpit desktop."""

from __future__ import annotations

from pathlib import Path
from typing import Any

from tradercockpit.research_custody import research_custody_capability_record
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
            },
        }

    launcher_verified = isinstance(launcher, dict) and launcher.get("verified") is True
    return {
        "status": "ready",
        "configured": True,
        "verified": True,
        "producer": "strategyquant-x",
        "build": build["observed"],
        "reason_code": None,
        "detail": f"Verified StrategyQuant X {build['observed']} runtime for read-only research inspection.",
        "runtime": runtime,
        "inspection": runtime["inspection"],
        "execution": {
            "available": False,
            "reason_code": execution["reason_code"],
            "launcher_verified": launcher_verified,
            "launcher_sha256": launcher.get("observed_sha256") if isinstance(launcher, dict) else None,
            "gateway_implemented": execution["gateway_implemented"],
            "gateway_available": execution["gateway_available"],
        },
    }


def _research_custody_status() -> dict[str, object]:
    return {
        "status": "unavailable",
        "reason_code": "store_not_bound",
        "detail": "Research custody primitives are implemented, but no canonical application data root/store is bound yet.",
        "contract": research_custody_capability_record(),
    }


def runtime_status_record(
    sqx_home: Path | str | None = None,
    trusted_launcher_sha256: str | None = None,
) -> dict[str, Any]:
    """Return the canonical, secret-free application readiness snapshot.

    This read model intentionally reports unavailable capabilities instead of
    inferring readiness from frontend state or from the mere presence of files.
    The trusted native gateway is implemented, but execution remains disabled until
    a product feature binds one exact approved native control request. Every future
    control still performs fresh launcher/config verification before process spawn.
    """

    return {
        "schema": RUNTIME_STATUS_SCHEMA,
        "application": {
            "status": "ready",
            "server": "canonical",
            "desktop": "canonical-server-ui",
        },
        "research_backend": _research_backend_status(sqx_home, trusted_launcher_sha256),
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
