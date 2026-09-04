"""Canonical runtime/status read model for the TraderCockpit desktop."""

from __future__ import annotations

from pathlib import Path
from typing import Any

from tradercockpit.assistant import assistant_status_record
from tradercockpit.capability_registry import extensions_status_record
from tradercockpit.home_market import market_overview_record
from tradercockpit.live_producers import live_producers_record
from tradercockpit.research_custody import research_custody_capability_record
from tradercockpit.sqx_runtime import sqx_runtime_descriptor


RUNTIME_STATUS_SCHEMA = "tc.runtime-status.v1"

# Operator recovery copy for fail-closed native runtime states. Process-side knobs
# only; never a filesystem path and never a browser-chosen sqx_home.
_RESEARCH_BACKEND_RECOVERY = {
    "runtime_not_configured": (
        "Set SQX_HOME or pass --sqx-home to the installed StrategyQuant X 144.2953 "
        "runtime. A unique 144.2953 install in the usual Windows locations can be "
        "remembered for this machine. The browser cannot choose this path."
    ),
    "sqx_install_ambiguous": (
        "More than one StrategyQuant X 144.2953 install was found. Set SQX_HOME or "
        "pass --sqx-home to the authorized one. The browser cannot choose this path."
    ),
    "sqx_build_mismatch": (
        "The configured runtime is not StrategyQuant X 144.2953. Point SQX_HOME or "
        "--sqx-home at the authorized 144.2953 install. The browser cannot choose this path."
    ),
    "sqx_build_markers_missing": (
        "The configured runtime is missing StrategyQuant X 144.2953 build markers. "
        "Restore the authorized install or point SQX_HOME at it. The browser cannot choose this path."
    ),
    "sqx_build_unreadable": (
        "The configured runtime's build markers could not be read. Restore the "
        "authorized StrategyQuant X 144.2953 install."
    ),
    "sqx_build_invalid": (
        "The configured runtime's version marker is invalid. Restore the authorized "
        "StrategyQuant X 144.2953 install."
    ),
    "sqx_build_marker_path_escape": (
        "Runtime verification failed closed because a build marker resolved outside "
        "the authorized install. Reset SQX_HOME or --sqx-home to the real StrategyQuant X "
        "144.2953 root. The browser cannot choose this path."
    ),
    "trusted_launcher_not_configured": (
        "Set SQX_LAUNCHER_SHA256 to the SHA-256 digest of the installed sqcli.exe. "
        "The browser cannot choose this value."
    ),
    "trusted_launcher_digest_invalid": (
        "SQX_LAUNCHER_SHA256 is not a 64-character hex SHA-256 digest. Restore the "
        "authorized launcher digest."
    ),
    "sqx_launcher_hash_mismatch": (
        "The installed sqcli.exe does not match the trusted launcher digest. Restore "
        "the authorized launcher or SQX_LAUNCHER_SHA256."
    ),
    "sqx_launcher_missing": (
        "The trusted launcher is missing from the authorized runtime. Restore sqcli.exe "
        "inside the StrategyQuant X 144.2953 install."
    ),
    "sqx_launcher_unreadable": (
        "The trusted launcher could not be read. Restore sqcli.exe inside the authorized "
        "StrategyQuant X 144.2953 install."
    ),
    "sqx_launcher_path_escape": (
        "Launcher verification failed closed because the path escaped the authorized "
        "runtime. Reset SQX_HOME to the real StrategyQuant X 144.2953 root. The browser "
        "cannot choose this path."
    ),
}

_UNKNOWN_RUNTIME_RECOVERY = (
    "Native research runtime verification failed closed. Set SQX_HOME or --sqx-home "
    "to the authorized StrategyQuant X 144.2953 install. The browser cannot choose this path."
)


def research_backend_recovery_detail(reason_code: str | None) -> str:
    """Return operator recovery copy for a fail-closed native runtime reason."""

    if not isinstance(reason_code, str) or not reason_code:
        return _UNKNOWN_RUNTIME_RECOVERY
    return _RESEARCH_BACKEND_RECOVERY.get(reason_code, _UNKNOWN_RUNTIME_RECOVERY)


def _unavailable(reason_code: str, detail: str) -> dict[str, object]:
    return {
        "status": "unavailable",
        "reason_code": reason_code,
        "detail": detail,
    }


def _execution_readback(
    execution: dict[str, object],
    *,
    available: bool,
    launcher_verified: bool,
    launcher_sha256: str | None,
) -> dict[str, object]:
    reason_code = execution.get("reason_code")
    return {
        "available": available,
        "reason_code": reason_code,
        "detail": None if available else research_backend_recovery_detail(
            reason_code if isinstance(reason_code, str) else None
        ),
        "launcher_verified": launcher_verified,
        "launcher_sha256": launcher_sha256,
        "gateway_implemented": execution["gateway_implemented"],
        "gateway_available": execution["gateway_available"] if available else False,
        "requires_approved_configuration": True,
    }


_BINDING_SOURCES = frozenset({"environment", "remembered", "discovered", "none"})


def _binding_record(sqx_home: Path | str | None, runtime_binding: str | None) -> dict[str, str]:
    if runtime_binding in _BINDING_SOURCES:
        source = runtime_binding
    else:
        source = "none" if sqx_home is None else "environment"
    return {"source": source}


def _research_backend_status(
    sqx_home: Path | str | None,
    trusted_launcher_sha256: str | None,
    *,
    runtime_binding: str | None = None,
    runtime_unavailable_reason: str | None = None,
) -> dict[str, object]:
    runtime = sqx_runtime_descriptor(sqx_home, trusted_launcher_sha256)
    build = runtime["build"]
    launcher = runtime["launcher"]
    execution = runtime["execution"]
    build_verified = isinstance(build, dict) and build.get("verified") is True
    binding = _binding_record(sqx_home, runtime_binding)
    if not build_verified:
        reason_code = build.get("reason_code") if isinstance(build, dict) else "runtime_invalid"
        if sqx_home is None and runtime_unavailable_reason == "sqx_install_ambiguous":
            reason_code = "sqx_install_ambiguous"
        detail = research_backend_recovery_detail(
            reason_code if isinstance(reason_code, str) else None
        )
        execution_readback = _execution_readback(
            execution,
            available=False,
            launcher_verified=False,
            launcher_sha256=None,
        )
        if reason_code == "sqx_install_ambiguous":
            execution_readback["reason_code"] = reason_code
            execution_readback["detail"] = detail
        return {
            "status": runtime["status"],
            "configured": sqx_home is not None,
            "verified": False,
            "producer": "strategyquant-x",
            "build": None,
            "reason_code": reason_code,
            "detail": detail,
            "binding": binding,
            "runtime": runtime,
            "inspection": runtime["inspection"],
            "execution": execution_readback,
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
        "binding": binding,
        "runtime": runtime,
        "inspection": runtime["inspection"],
        "execution": _execution_readback(
            execution,
            available=execution_available,
            launcher_verified=launcher_verified,
            launcher_sha256=launcher.get("observed_sha256") if isinstance(launcher, dict) else None,
        ),
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


def runtime_status_record(
    sqx_home: Path | str | None = None,
    trusted_launcher_sha256: str | None = None,
    *,
    research_store_bound: bool = False,
    data_root: Path | str | None = None,
    runtime_binding: str | None = None,
    runtime_unavailable_reason: str | None = None,
) -> dict[str, Any]:
    """Return the canonical, secret-free application readiness snapshot.

    This read model reports whether the trusted native control boundary is available,
    but it is never authorization for a particular launch. Every Builder launch still
    requires one exact current approved configuration and fresh launcher/config
    verification inside the native gateway immediately before process creation.
    """

    if not isinstance(research_store_bound, bool):
        raise ValueError("research_store_bound must be boolean")

    assistant = assistant_status_record()
    provider_ready = assistant["status"] == "ready"
    return {
        "schema": RUNTIME_STATUS_SCHEMA,
        "application": {
            "status": "ready",
            "server": "canonical",
            "desktop": "canonical-server-ui",
        },
        "research_backend": _research_backend_status(
            sqx_home,
            trusted_launcher_sha256,
            runtime_binding=runtime_binding,
            runtime_unavailable_reason=runtime_unavailable_reason,
        ),
        "research_custody": _research_custody_status(research_store_bound),
        "market_data": market_overview_record(),
        "account": _unavailable(
            "authority_not_implemented",
            "Consumer account authority is not implemented yet; the assistant runs under the operator credential on this desktop.",
        ),
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
        "extensions": extensions_status_record(data_root, sqx_home=sqx_home),
        "live_producers": live_producers_record(),
    }
