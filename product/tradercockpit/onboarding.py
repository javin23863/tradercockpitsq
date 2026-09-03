"""First-run onboarding, customer-readable reason copy, and fail-closed telemetry.

This is a status projection over existing `/api/status` and desktop-maintenance
`reason_code` values. It does not phone home, invent producer truth, or add a
second product spine.
"""

from __future__ import annotations

from typing import Any, Mapping


ONBOARDING_SCHEMA = "tc.onboarding.v1"
TELEMETRY_SCHEMA = "tc.telemetry-policy.v1"

# Customer sentences for codes already returned by /api/status and data-root
# maintenance. Unknown codes fall back to the producing record's own detail.
CUSTOMER_REASON_COPY = {
    "runtime_not_configured": (
        "No StrategyQuant X installation is bound. Open Settings → Native research "
        "runtime and bind a verified 144.2953 home."
    ),
    "runtime_not_verified": (
        "The configured StrategyQuant X home did not verify. Bind a matching 144.2953 "
        "runtime from Settings."
    ),
    "sqx_build_mismatch": (
        "The bound StrategyQuant X build does not match the expected 144.2953 identity."
    ),
    "trusted_launcher_not_configured": (
        "Launcher trust is not configured. Set SQX_LAUNCHER_SHA256 to the SHA-256 of "
        "the installed sqcli.exe, or bind from Settings."
    ),
    "trusted_launcher_digest_invalid": "SQX_LAUNCHER_SHA256 is not a SHA-256 hex digest.",
    "sqx_launcher_missing": "sqcli.exe was not found in the bound StrategyQuant X home.",
    "sqx_launcher_hash_mismatch": "The bound sqcli.exe hash does not match SQX_LAUNCHER_SHA256.",
    "sqx_launcher_path_escape": (
        "The launcher path escaped the bound StrategyQuant X home and was refused."
    ),
    "sqx_launcher_unreadable": "sqcli.exe in the bound home could not be read.",
    "store_not_bound": (
        "The application data root is not bound, so research custody and desktop "
        "session cannot persist."
    ),
    "data_root_unbound": (
        "The application data root is not bound. Launch the desktop so it can create "
        "the data root."
    ),
    "manifest_invalid": (
        "The data-root manifest is not valid JSON. Restore from a backups/ archive or "
        "recreate the data root."
    ),
    "unknown_schema": (
        "The data-root manifest schema is unknown. Restore from a backups/ archive; "
        "the app will not guess."
    ),
    "manifest_missing": "The backup archive has no data-root manifest and was refused.",
    "restore_path_escape": "Restore accepts only a zip basename under backups/.",
    "backup_not_found": "That backups/ archive was not found.",
    "maintenance_action_invalid": (
        "Data-root maintenance accepts action=backup or action=restore only."
    ),
    "provider_not_configured": (
        "A provider credential is not configured in the operator environment. Google, "
        "Stripe, and OpenRouter each report this from Settings without exposing secret "
        "values."
    ),
    "signed_out": "No verified Google session. Sign in from Settings → Consumer account.",
    "not_signed_in": "Sign in with Google before subscribing.",
    "checkout_not_configured": (
        "Stripe checkout is not configured. Membership stays unavailable until the "
        "operator sets the Stripe environment variables named in Settings."
    ),
    "inactive": "This Google account has no active $150/month membership.",
    "provision_not_configured": (
        "OpenRouter management is not configured. Per-consumer $30/month credits stay "
        "unavailable until OPENROUTER_MANAGEMENT_KEY is set."
    ),
    "telemetry_disabled": (
        "TraderCockpit does not phone home. Crash logs, tokens, OAuth files, and "
        "environment secrets stay on this machine."
    ),
    "setup_incomplete": (
        "Setup is incomplete. Finish the steps on Settings → Application. This is not "
        "a lake-coverage or import-an-idea wizard."
    ),
    "signing_not_configured": (
        "Authenticode signing material is not configured. The installer will not claim "
        "a production-signed artifact."
    ),
}


def customer_copy(reason_code: object, detail: object = None) -> str:
    """Map a known reason_code to Settings/onboarding copy; otherwise reuse detail."""

    if isinstance(reason_code, str) and reason_code in CUSTOMER_REASON_COPY:
        return CUSTOMER_REASON_COPY[reason_code]
    if isinstance(detail, str) and detail.strip():
        return detail
    if isinstance(reason_code, str) and reason_code.strip():
        return reason_code.replace("_", " ")
    return "Ready."


def telemetry_status_record() -> dict[str, object]:
    """Fail-closed telemetry policy. There is no enable switch."""

    return {
        "schema": TELEMETRY_SCHEMA,
        "enabled": False,
        "reason_code": "telemetry_disabled",
        "detail": CUSTOMER_REASON_COPY["telemetry_disabled"],
    }


def _record(value: object) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def _family_configured(record: Mapping[str, Any], missing_code: str) -> bool:
    if record.get("status") == "ready":
        return True
    code = record.get("reason_code")
    return isinstance(code, str) and code != missing_code


def _step(step_id: str, record: Mapping[str, Any], *, ready: bool) -> dict[str, object]:
    reason = None if ready else record.get("reason_code")
    if not ready and not isinstance(reason, str):
        reason = "setup_incomplete"
    detail = record.get("detail") if isinstance(record.get("detail"), str) else None
    return {
        "id": step_id,
        "status": "ready" if ready else "unavailable",
        "reason_code": None if ready else reason,
        "detail": customer_copy(None if ready else reason, detail if ready else detail),
    }


def _secrets_step(
    account: Mapping[str, Any],
    membership: Mapping[str, Any],
    assistant: Mapping[str, Any],
) -> dict[str, object]:
    # ponytail: process env is the secrets source on this stack; PR #102 file store is a sibling.
    ready = (
        _family_configured(assistant, "provider_not_configured")
        or _family_configured(account, "provider_not_configured")
        or _family_configured(membership, "checkout_not_configured")
    )
    if ready:
        return {
            "id": "secrets",
            "status": "ready",
            "reason_code": None,
            "detail": (
                "Operator credentials are read from the process environment. Remaining "
                "unconfigured providers keep their own reason codes on the Account, "
                "Membership, and Model cards."
            ),
        }
    return _step("secrets", assistant or {"reason_code": "provider_not_configured"}, ready=False)


def onboarding_status_record(
    *,
    research: object,
    account: object,
    membership: object,
    maintenance: object,
    assistant: object,
) -> dict[str, object]:
    """Project first-run + commercial checklist from existing status records."""

    research_record = _record(research)
    account_record = _record(account)
    membership_record = _record(membership)
    maintenance_record = _record(maintenance)
    assistant_record = _record(assistant)

    native_ready = research_record.get("verified") is True
    data_root_ready = maintenance_record.get("status") == "ready"
    steps = [
        _step("native_runtime", research_record, ready=native_ready),
        _step("account", account_record, ready=account_record.get("status") == "ready"),
        _step("membership", membership_record, ready=membership_record.get("status") == "ready"),
        _step("data_root", maintenance_record, ready=data_root_ready),
        _secrets_step(account_record, membership_record, assistant_record),
    ]
    incomplete = [step for step in steps if step["status"] != "ready"]
    first_run = not native_ready or not data_root_ready
    status = "ready" if not incomplete else "incomplete"
    reason = None if status == "ready" else str(incomplete[0]["reason_code"] or "setup_incomplete")
    if status == "ready":
        detail = "Native runtime, account, membership, data root, and operator credentials are ready."
    elif first_run:
        detail = CUSTOMER_REASON_COPY["setup_incomplete"]
    else:
        detail = customer_copy(reason)
    return {
        "schema": ONBOARDING_SCHEMA,
        "status": status,
        "first_run": first_run,
        "reason_code": reason,
        "detail": detail,
        "steps": steps,
    }
