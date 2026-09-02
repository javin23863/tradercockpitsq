"""Deployment mode: separate the personal single-operator setup from the commercial
multi-tenant product so one can never silently masquerade as the other.

- ``personal`` (default): one owner, one local data root, operator-held provider
  credentials, loopback desktop. This is today's shipping behavior and is ``ready``.
- ``commercial``: many isolated consumers (scaling to thousands). It additionally
  requires hosted consumer authentication, provider-enforced per-consumer spend, and —
  critically — per-tenant data isolation. A single local data root is not a tenant
  boundary, so per-tenant isolation is not satisfied in this build. Commercial mode
  therefore reports ``not-ready`` (fail closed) with the exact unmet prerequisites
  instead of pretending a personal desktop is a multi-tenant server.

The storage seam that unlocks commercial mode (a per-tenant isolated store behind the
same read-model contracts) is described in ``docs/product-architecture-v1.md``.
"""

from __future__ import annotations

import os
from typing import Mapping

DEPLOYMENT_MODE_ENV = "TRADERCOCKPIT_DEPLOYMENT_MODE"
DEPLOYMENT_STATUS_SCHEMA = "tc.deployment-mode.v1"

PERSONAL = "personal"
COMMERCIAL = "commercial"
_VALID_MODES = (PERSONAL, COMMERCIAL)


def deployment_mode(environ: Mapping[str, str] | None = None) -> str:
    source = environ if environ is not None else os.environ
    raw = (source.get(DEPLOYMENT_MODE_ENV) or "").strip().lower()
    if not raw:
        return PERSONAL
    if raw not in _VALID_MODES:
        raise ValueError(f"{DEPLOYMENT_MODE_ENV} must be one of {_VALID_MODES}")
    return raw


def deployment_status_record(
    environ: Mapping[str, str] | None = None,
    *,
    account: dict[str, object] | None = None,
    membership: dict[str, object] | None = None,
    provider: dict[str, object] | None = None,
) -> dict[str, object]:
    """Secret-free deployment-mode readiness for /api/status and Settings."""

    try:
        mode = deployment_mode(environ)
    except ValueError as exc:
        return {
            "schema": DEPLOYMENT_STATUS_SCHEMA,
            "mode": None,
            "status": "unavailable",
            "reason_code": "deployment_mode_invalid",
            "detail": str(exc),
        }

    base = {"schema": DEPLOYMENT_STATUS_SCHEMA, "mode": mode}
    if mode == PERSONAL:
        return {
            **base,
            "status": "ready",
            "reason_code": None,
            "detail": "Single-operator desktop: one owner, one local data root, operator-held credentials.",
        }

    account_ready = isinstance(account, dict) and account.get("status") == "ready"
    membership_active = isinstance(membership, dict) and membership.get("membership_status") == "active"
    spend_boundary = provider.get("spend_boundary") if isinstance(provider, dict) else None
    provider_enforced_spend = isinstance(spend_boundary, dict) and bool(spend_boundary.get("provider_enforced"))
    requirements = {
        "hosted_consumer_auth": bool(account_ready),
        "membership_billing": bool(membership_active),
        "provider_enforced_spend": bool(provider_enforced_spend),
        # A single local data root is not a tenant boundary; per-tenant isolation is the
        # architectural blocker that the commercial storage seam must satisfy.
        "per_tenant_isolation": False,
    }
    unmet = sorted(name for name, satisfied in requirements.items() if not satisfied)
    if unmet:
        return {
            **base,
            "status": "unavailable",
            "reason_code": "commercial_not_ready",
            "detail": (
                "Commercial multi-tenant mode requires: "
                + ", ".join(unmet)
                + ". Per-tenant data isolation is provided by the commercial storage seam, "
                "not by the personal single-data-root desktop."
            ),
            "requirements": requirements,
            "unmet": unmet,
        }
    return {
        **base,
        "status": "ready",
        "reason_code": None,
        "detail": "Commercial multi-tenant prerequisites satisfied.",
        "requirements": requirements,
        "unmet": [],
    }
