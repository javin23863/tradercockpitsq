"""Consumer account and bounded external-LLM contracts.

This module deliberately contains no live Google OAuth or OpenRouter network client.
Those integrations are injected behind narrow protocols so the product fails closed
until real provider adapters are configured.
"""

from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal, InvalidOperation
from hashlib import sha256
import json
from typing import Protocol


_ACCOUNT_SUBJECT_PREFIX = "tc-account:google:v1:sha256:"
_GOOGLE_CANONICAL_ISSUER = "https://accounts.google.com"
_GOOGLE_ACCEPTED_ISSUERS = frozenset({_GOOGLE_CANONICAL_ISSUER, "accounts.google.com"})
_SPEND_STATUSES = frozenset({"unconfigured", "active", "exhausted", "revoked", "expired"})
_ACCOUNT_EVENT_KINDS = frozenset(
    {
        "account_created",
        "allowance_changed",
        "spend_authority_bound",
        "usage_reconciled",
        "signed_in",
        "signed_out",
        "entitlement_revoked",
    }
)


class AccountContractError(ValueError):
    """Raised when account, model-policy, or provider contract data is invalid."""


class AccountIntegrationUnavailable(RuntimeError):
    """Raised when a live external identity/provider collaborator is not configured."""


def _text(value: object, name: str) -> str:
    if not isinstance(value, str) or not value:
        raise AccountContractError(f"{name} must be a non-empty string")
    if value != value.strip():
        raise AccountContractError(f"{name} must not contain surrounding whitespace")
    return value


def _optional_text(value: object, name: str) -> str | None:
    if value is None:
        return None
    return _text(value, name)


def _decimal(value: object, name: str) -> Decimal:
    if isinstance(value, bool):
        raise AccountContractError(f"{name} must be a decimal amount")
    try:
        amount = value if isinstance(value, Decimal) else Decimal(str(value))
    except (InvalidOperation, ValueError) as exc:
        raise AccountContractError(f"{name} must be a decimal amount") from exc
    if not amount.is_finite():
        raise AccountContractError(f"{name} must be finite")
    if amount < 0:
        raise AccountContractError(f"{name} must not be negative")
    return amount


def decimal_text(value: Decimal) -> str:
    """Return a stable non-exponent decimal string."""
    if not isinstance(value, Decimal) or not value.is_finite():
        raise AccountContractError("amount must be a finite Decimal")
    text = format(value, "f")
    if "." in text:
        text = text.rstrip("0").rstrip(".")
    return text or "0"


def _canonical_bytes(value: object) -> bytes:
    return json.dumps(
        value,
        ensure_ascii=False,
        allow_nan=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")


@dataclass(frozen=True)
class VerifiedGoogleIdentity:
    """Identity claims returned only by a trusted Google-verification collaborator."""

    issuer: str
    subject: str
    email: str | None = None

    def __post_init__(self) -> None:
        issuer = _text(self.issuer, "issuer")
        if issuer not in _GOOGLE_ACCEPTED_ISSUERS:
            raise AccountContractError("issuer is not an accepted Google identity-token issuer")
        object.__setattr__(self, "issuer", _GOOGLE_CANONICAL_ISSUER)
        object.__setattr__(self, "subject", _text(self.subject, "subject"))
        object.__setattr__(self, "email", _optional_text(self.email, "email"))

    @property
    def account_subject(self) -> str:
        digest = sha256(f"{self.issuer}\x00{self.subject}".encode("utf-8")).hexdigest()
        return _ACCOUNT_SUBJECT_PREFIX + digest


@dataclass(frozen=True)
class ModelPolicyV1:
    """Backend-owned external model routing policy."""

    policy_id: str
    default_model: str
    fallback_models: tuple[str, ...] = ()

    def __post_init__(self) -> None:
        object.__setattr__(self, "policy_id", _text(self.policy_id, "policy_id"))
        object.__setattr__(self, "default_model", _text(self.default_model, "default_model"))
        fallback = tuple(_text(item, "fallback_model") for item in self.fallback_models)
        if self.default_model in fallback:
            raise AccountContractError("fallback_models must not repeat default_model")
        if len(set(fallback)) != len(fallback):
            raise AccountContractError("fallback_models must not contain duplicates")
        object.__setattr__(self, "fallback_models", fallback)

    def read_model(self) -> dict[str, object]:
        return {
            "policy_id": self.policy_id,
            "default_model": self.default_model,
            "fallback_models": list(self.fallback_models),
        }


@dataclass(frozen=True)
class SpendAuthorityMetadataV1:
    """Secret-free metadata describing provider-enforced consumer spend authority."""

    authority_id: str
    status: str
    hard_limit: Decimal | None = None
    limit_reset: str | None = None
    expires_at: str | None = None

    def __post_init__(self) -> None:
        authority_id = _text(self.authority_id, "authority_id")
        if authority_id.startswith("sk-") or "bearer " in authority_id.lower():
            raise AccountContractError("authority_id must be provider metadata, not a credential")
        object.__setattr__(self, "authority_id", authority_id)
        status = _text(self.status, "status")
        if status not in _SPEND_STATUSES:
            raise AccountContractError(f"unsupported spend authority status: {status}")
        object.__setattr__(self, "status", status)
        if self.hard_limit is not None:
            object.__setattr__(self, "hard_limit", _decimal(self.hard_limit, "hard_limit"))
        if status == "active" and self.hard_limit is None:
            raise AccountContractError("active spend authority requires an explicit hard_limit")
        object.__setattr__(self, "limit_reset", _optional_text(self.limit_reset, "limit_reset"))
        object.__setattr__(self, "expires_at", _optional_text(self.expires_at, "expires_at"))

    def read_model(self) -> dict[str, object]:
        return {
            "authority_id": self.authority_id,
            "status": self.status,
            "hard_limit": decimal_text(self.hard_limit) if self.hard_limit is not None else None,
            "limit_reset": self.limit_reset,
            "expires_at": self.expires_at,
        }


@dataclass(frozen=True)
class AccountStateV1:
    """Current consumer account snapshot carried by immutable state events."""

    subject: str
    signed_in: bool
    entitlement_id: str
    allowance_limit: Decimal
    allowance_used: Decimal
    email: str | None = None
    spend_authority: SpendAuthorityMetadataV1 | None = None

    def __post_init__(self) -> None:
        subject = _text(self.subject, "subject")
        if not subject.startswith(_ACCOUNT_SUBJECT_PREFIX):
            raise AccountContractError("subject must be a TraderCockpit Google account subject")
        object.__setattr__(self, "subject", subject)
        if not isinstance(self.signed_in, bool):
            raise AccountContractError("signed_in must be boolean")
        object.__setattr__(self, "entitlement_id", _text(self.entitlement_id, "entitlement_id"))
        limit = _decimal(self.allowance_limit, "allowance_limit")
        used = _decimal(self.allowance_used, "allowance_used")
        if used > limit:
            raise AccountContractError("allowance_used must not exceed allowance_limit")
        object.__setattr__(self, "allowance_limit", limit)
        object.__setattr__(self, "allowance_used", used)
        object.__setattr__(self, "email", _optional_text(self.email, "email"))
        if self.spend_authority is not None:
            if not isinstance(self.spend_authority, SpendAuthorityMetadataV1):
                raise AccountContractError("spend_authority must be SpendAuthorityMetadataV1")
            if (
                self.spend_authority.hard_limit is not None
                and self.spend_authority.hard_limit > self.allowance_limit
            ):
                raise AccountContractError(
                    "provider hard_limit must not exceed the product allowance_limit"
                )

    @property
    def allowance_remaining(self) -> Decimal:
        return self.allowance_limit - self.allowance_used

    def identity_payload(self) -> dict[str, object]:
        return {
            "subject": self.subject,
            "signed_in": self.signed_in,
            "entitlement_id": self.entitlement_id,
            "allowance_limit": decimal_text(self.allowance_limit),
            "allowance_used": decimal_text(self.allowance_used),
            "email": self.email,
            "spend_authority": (
                self.spend_authority.read_model() if self.spend_authority is not None else None
            ),
        }

    def read_model(self, model_policy: ModelPolicyV1) -> dict[str, object]:
        if not isinstance(model_policy, ModelPolicyV1):
            raise AccountContractError("model_policy must be ModelPolicyV1")
        return {
            "schema": "tc.account-state-read.v1",
            "subject": self.subject,
            "signed_in": self.signed_in,
            "email": self.email,
            "entitlement_id": self.entitlement_id,
            "allowance": {
                "limit": decimal_text(self.allowance_limit),
                "used": decimal_text(self.allowance_used),
                "remaining": decimal_text(self.allowance_remaining),
            },
            "spend_authority": (
                self.spend_authority.read_model() if self.spend_authority is not None else None
            ),
            "model_policy": model_policy.read_model(),
        }


@dataclass(frozen=True)
class AccountStateEventV1:
    """Immutable event containing the full account snapshot after one state change."""

    event_kind: str
    occurred_at: str
    state: AccountStateV1
    previous_event_id: str | None = None

    def __post_init__(self) -> None:
        event_kind = _text(self.event_kind, "event_kind")
        if event_kind not in _ACCOUNT_EVENT_KINDS:
            raise AccountContractError(f"unsupported account event kind: {event_kind}")
        object.__setattr__(self, "event_kind", event_kind)
        object.__setattr__(self, "occurred_at", _text(self.occurred_at, "occurred_at"))
        if not isinstance(self.state, AccountStateV1):
            raise AccountContractError("state must be AccountStateV1")
        if self.previous_event_id is not None:
            previous = _text(self.previous_event_id, "previous_event_id")
            if len(previous) != 64 or any(ch not in "0123456789abcdef" for ch in previous):
                raise AccountContractError("previous_event_id must be a lowercase sha256 hex digest")
            object.__setattr__(self, "previous_event_id", previous)

    def identity_payload(self) -> dict[str, object]:
        return {
            "event_kind": self.event_kind,
            "occurred_at": self.occurred_at,
            "state": self.state.identity_payload(),
            "previous_event_id": self.previous_event_id,
        }

    @property
    def event_id(self) -> str:
        return sha256(_canonical_bytes(self.identity_payload())).hexdigest()


class GoogleIdentityVerifier(Protocol):
    """Trusted verifier boundary; production adapter is intentionally not implemented here."""

    def verify(self, credential: str) -> VerifiedGoogleIdentity:
        ...


class OpenRouterSpendProvisioner(Protocol):
    """Trusted provider-provisioning boundary; management credentials stay behind it."""

    def provision(
        self,
        *,
        account_subject: str,
        hard_limit: Decimal,
        limit_reset: str | None,
        expires_at: str | None,
    ) -> SpendAuthorityMetadataV1:
        ...


def verify_google_identity(
    credential: str,
    verifier: GoogleIdentityVerifier | None,
) -> VerifiedGoogleIdentity:
    """Fail closed unless a trusted external identity verifier is injected."""
    _text(credential, "credential")
    if verifier is None:
        raise AccountIntegrationUnavailable("Google identity verifier is not configured")
    identity = verifier.verify(credential)
    if not isinstance(identity, VerifiedGoogleIdentity):
        raise AccountContractError("Google verifier returned an invalid identity object")
    return identity


def provision_openrouter_spend_authority(
    *,
    account_subject: str,
    hard_limit: Decimal,
    limit_reset: str | None,
    expires_at: str | None,
    provisioner: OpenRouterSpendProvisioner | None,
) -> SpendAuthorityMetadataV1:
    """Fail closed unless the trusted OpenRouter provisioning boundary is injected."""
    _text(account_subject, "account_subject")
    limit = _decimal(hard_limit, "hard_limit")
    if provisioner is None:
        raise AccountIntegrationUnavailable("OpenRouter provisioner is not configured")
    authority = provisioner.provision(
        account_subject=account_subject,
        hard_limit=limit,
        limit_reset=limit_reset,
        expires_at=expires_at,
    )
    if not isinstance(authority, SpendAuthorityMetadataV1):
        raise AccountContractError("OpenRouter provisioner returned invalid authority metadata")
    if authority.hard_limit is not None and authority.hard_limit != limit:
        raise AccountContractError("OpenRouter authority limit does not match requested hard limit")
    return authority
