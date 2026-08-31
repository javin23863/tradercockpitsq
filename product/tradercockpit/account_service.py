"""Application service for stable consumer-account resolution.

Live OAuth and OpenRouter clients are intentionally outside this module. The service
accepts only a trusted verifier collaborator and durable account-state store.
"""

from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal

from tradercockpit.account import (
    AccountStateEventV1,
    AccountStateV1,
    GoogleIdentityVerifier,
    verify_google_identity,
)
from tradercockpit.storage.account_store import AccountStateStoreError, FileAccountStateStore


@dataclass(frozen=True)
class AccountResolution:
    event: AccountStateEventV1
    created: bool


def resolve_google_account(
    *,
    credential: str,
    verifier: GoogleIdentityVerifier | None,
    store: FileAccountStateStore,
    entitlement_id: str,
    starter_grant_policy_id: str,
    starter_allowance: Decimal,
    occurred_at: str,
) -> AccountResolution:
    """Resolve one stable account and admit the configured starter grant only once."""

    identity = verify_google_identity(credential, verifier)
    proposed = AccountStateEventV1(
        event_kind="account_created",
        occurred_at=occurred_at,
        state=AccountStateV1(
            subject=identity.account_subject,
            signed_in=True,
            entitlement_id=entitlement_id,
            starter_grant_policy_id=starter_grant_policy_id,
            allowance_limit=starter_allowance,
            allowance_used=Decimal("0"),
            email=identity.email,
        ),
    )
    current, created = store.create_if_absent(proposed)
    if created:
        return AccountResolution(current, True)

    if current.state.signed_in and (
        identity.email is None or identity.email == current.state.email
    ):
        return AccountResolution(current, False)

    signed_in = AccountStateEventV1(
        event_kind="signed_in",
        occurred_at=occurred_at,
        state=AccountStateV1(
            subject=current.state.subject,
            signed_in=True,
            entitlement_id=current.state.entitlement_id,
            starter_grant_policy_id=current.state.starter_grant_policy_id,
            allowance_limit=current.state.allowance_limit,
            allowance_used=current.state.allowance_used,
            email=identity.email if identity.email is not None else current.state.email,
            spend_authority=current.state.spend_authority,
        ),
        previous_event_id=current.event_id,
    )
    try:
        store.publish(signed_in)
    except AccountStateStoreError:
        # Another same-subject sign-in may have advanced the head after our read.
        # Re-read rather than creating a parallel grant or overwriting custody.
        return AccountResolution(store.current(current.state.subject), False)
    return AccountResolution(signed_in, False)
