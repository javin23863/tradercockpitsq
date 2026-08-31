"""Filesystem custody for consumer account state.

Account state mirrors the run lifecycle pattern: immutable events plus one atomic head
pointer per stable internal subject. Provider secrets are never stored here.
"""

from __future__ import annotations

from decimal import Decimal
from hashlib import sha256
import json
import os
from pathlib import Path
import tempfile
from typing import Any, Mapping

from tradercockpit.account import (
    AccountContractError,
    AccountStateEventV1,
    AccountStateV1,
    SpendAuthorityMetadataV1,
)


_EVENT_SCHEMA = "tc.account-state-event-wire.v1"
_HEAD_SCHEMA = "tc.account-state-head.v1"


class AccountStateStoreError(RuntimeError):
    """Raised when durable account-state custody is missing, corrupt, or stale."""


def _canonical_bytes(value: object) -> bytes:
    return json.dumps(
        value,
        ensure_ascii=False,
        allow_nan=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")


def _object(value: Any, name: str) -> Mapping[str, Any]:
    if not isinstance(value, Mapping):
        raise AccountStateStoreError(f"{name} must be an object")
    return value


def _text(value: Any, name: str) -> str:
    if not isinstance(value, str) or not value:
        raise AccountStateStoreError(f"{name} must be a non-empty string")
    return value


def _exact_keys(value: Mapping[str, Any], expected: set[str], name: str) -> None:
    if set(value) != expected:
        raise AccountStateStoreError(f"{name} fields do not match the account-state schema")


def _optional_text(value: Any, name: str) -> str | None:
    return None if value is None else _text(value, name)


def _decode_decimal(value: Any, name: str) -> Decimal:
    if not isinstance(value, str) or not value:
        raise AccountStateStoreError(f"{name} must be a decimal string")
    try:
        amount = Decimal(value)
    except Exception as exc:
        raise AccountStateStoreError(f"{name} is not a valid decimal string") from exc
    if not amount.is_finite():
        raise AccountStateStoreError(f"{name} must be finite")
    return amount


def _decode_spend_authority(value: Any) -> SpendAuthorityMetadataV1 | None:
    if value is None:
        return None
    raw = _object(value, "spend_authority")
    _exact_keys(
        raw,
        {"authority_id", "status", "hard_limit", "limit_reset", "expires_at"},
        "spend_authority",
    )
    try:
        return SpendAuthorityMetadataV1(
            authority_id=_text(raw["authority_id"], "authority_id"),
            status=_text(raw["status"], "status"),
            hard_limit=(
                None
                if raw["hard_limit"] is None
                else _decode_decimal(raw["hard_limit"], "hard_limit")
            ),
            limit_reset=_optional_text(raw["limit_reset"], "limit_reset"),
            expires_at=_optional_text(raw["expires_at"], "expires_at"),
        )
    except AccountContractError as exc:
        raise AccountStateStoreError(f"invalid spend authority: {exc}") from exc


def _decode_state(value: Any) -> AccountStateV1:
    raw = _object(value, "state")
    _exact_keys(
        raw,
        {
            "subject",
            "signed_in",
            "entitlement_id",
            "allowance_limit",
            "allowance_used",
            "email",
            "spend_authority",
        },
        "state",
    )
    if not isinstance(raw["signed_in"], bool):
        raise AccountStateStoreError("signed_in must be boolean")
    try:
        return AccountStateV1(
            subject=_text(raw["subject"], "subject"),
            signed_in=raw["signed_in"],
            entitlement_id=_text(raw["entitlement_id"], "entitlement_id"),
            allowance_limit=_decode_decimal(raw["allowance_limit"], "allowance_limit"),
            allowance_used=_decode_decimal(raw["allowance_used"], "allowance_used"),
            email=_optional_text(raw["email"], "email"),
            spend_authority=_decode_spend_authority(raw["spend_authority"]),
        )
    except AccountContractError as exc:
        raise AccountStateStoreError(f"invalid account state: {exc}") from exc


def _encode_event(event: AccountStateEventV1) -> bytes:
    if not isinstance(event, AccountStateEventV1):
        raise AccountStateStoreError("event must be AccountStateEventV1")
    return _canonical_bytes(
        {
            "wire_schema": _EVENT_SCHEMA,
            "event_id": event.event_id,
            "payload": event.identity_payload(),
        }
    )


def _decode_event(data: bytes) -> AccountStateEventV1:
    try:
        raw = json.loads(data.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise AccountStateStoreError(f"invalid account event bytes: {exc}") from exc
    envelope = _object(raw, "account event envelope")
    _exact_keys(envelope, {"wire_schema", "event_id", "payload"}, "account event envelope")
    if envelope["wire_schema"] != _EVENT_SCHEMA:
        raise AccountStateStoreError("unsupported account event wire schema")
    declared_id = _text(envelope["event_id"], "event_id")
    payload = _object(envelope["payload"], "payload")
    _exact_keys(
        payload,
        {"event_kind", "occurred_at", "state", "previous_event_id"},
        "account event payload",
    )
    try:
        event = AccountStateEventV1(
            event_kind=_text(payload["event_kind"], "event_kind"),
            occurred_at=_text(payload["occurred_at"], "occurred_at"),
            state=_decode_state(payload["state"]),
            previous_event_id=_optional_text(payload["previous_event_id"], "previous_event_id"),
        )
    except AccountContractError as exc:
        raise AccountStateStoreError(f"invalid account event payload: {exc}") from exc
    if event.event_id != declared_id:
        raise AccountStateStoreError("account event payload does not match declared event_id")
    return event


class FileAccountStateStore:
    """Atomic current-state index plus immutable account event archive."""

    def __init__(self, root: Path | str):
        self.root = Path(root).expanduser().resolve()
        self.account_root = self.root / "accounts"
        self.events_root = self.account_root / "events" / "v1"
        self.heads_root = self.account_root / "heads"
        self.events_root.mkdir(parents=True, exist_ok=True)
        self.heads_root.mkdir(parents=True, exist_ok=True)

    @staticmethod
    def _validate_subject(subject: str) -> None:
        if not isinstance(subject, str) or not subject:
            raise AccountStateStoreError("subject must be a non-empty string")
        if subject != subject.strip():
            raise AccountStateStoreError("subject must not contain surrounding whitespace")
        if not subject.startswith("tc-account:google:v1:sha256:"):
            raise AccountStateStoreError("subject is not a TraderCockpit Google account subject")

    def _event_path(self, event_id: str) -> Path:
        if (
            not isinstance(event_id, str)
            or len(event_id) != 64
            or any(ch not in "0123456789abcdef" for ch in event_id)
        ):
            raise AccountStateStoreError("event_id must be a lowercase sha256 hex digest")
        return self.events_root / f"{event_id}.json"

    def _head_path(self, subject: str) -> Path:
        self._validate_subject(subject)
        digest = sha256(subject.encode("utf-8")).hexdigest()
        return self.heads_root / f"{digest}.json"

    @staticmethod
    def _atomic_write(target: Path, data: bytes) -> None:
        target.parent.mkdir(parents=True, exist_ok=True)
        fd, temporary_name = tempfile.mkstemp(
            prefix=f".{target.name}.",
            suffix=".tmp",
            dir=target.parent,
        )
        temporary = Path(temporary_name)
        try:
            with os.fdopen(fd, "wb") as handle:
                handle.write(data)
                handle.flush()
                os.fsync(handle.fileno())
            os.replace(temporary, target)
        finally:
            if temporary.exists():
                temporary.unlink()

    def _write_event(self, event: AccountStateEventV1) -> None:
        encoded = _encode_event(event)
        target = self._event_path(event.event_id)
        if target.exists():
            existing = target.read_bytes()
            if existing != encoded:
                raise AccountStateStoreError(
                    f"existing account event bytes disagree with immutable id {event.event_id}"
                )
            if _decode_event(existing).event_id != event.event_id:
                raise AccountStateStoreError("existing account event cannot be reverified")
            return
        self._atomic_write(target, encoded)
        stored = target.read_bytes()
        if stored != encoded or _decode_event(stored).event_id != event.event_id:
            raise AccountStateStoreError("stored account event failed verification")

    def _encode_head(self, event: AccountStateEventV1) -> bytes:
        return _canonical_bytes(
            {
                "wire_schema": _HEAD_SCHEMA,
                "subject": event.state.subject,
                "event_id": event.event_id,
            }
        )

    def _read_head(self, subject: str) -> AccountStateEventV1:
        target = self._head_path(subject)
        try:
            encoded = target.read_bytes()
        except FileNotFoundError as exc:
            raise KeyError(subject) from exc
        try:
            raw = json.loads(encoded.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise AccountStateStoreError(f"invalid account head bytes: {exc}") from exc
        head = _object(raw, "account head")
        _exact_keys(head, {"wire_schema", "subject", "event_id"}, "account head")
        if head["wire_schema"] != _HEAD_SCHEMA:
            raise AccountStateStoreError("unsupported account head schema")
        stored_subject = _text(head["subject"], "subject")
        event_id = _text(head["event_id"], "event_id")
        if stored_subject != subject:
            raise AccountStateStoreError("account head lookup identity mismatch")
        try:
            event = _decode_event(self._event_path(event_id).read_bytes())
        except FileNotFoundError as exc:
            raise AccountStateStoreError("account head points to a missing event") from exc
        if event.event_id != event_id:
            raise AccountStateStoreError("account head event id mismatch")
        if event.state.subject != subject:
            raise AccountStateStoreError("account event belongs to another subject")
        return event

    def publish(self, event: AccountStateEventV1) -> str:
        if not isinstance(event, AccountStateEventV1):
            raise AccountStateStoreError("event must be AccountStateEventV1")
        subject = event.state.subject
        try:
            current = self._read_head(subject)
        except KeyError:
            current = None

        if current is None:
            if event.event_kind != "account_created":
                raise AccountStateStoreError("first account event must be account_created")
            if event.previous_event_id is not None:
                raise AccountStateStoreError("first account event cannot reference a predecessor")
        else:
            if event.previous_event_id != current.event_id:
                raise AccountStateStoreError("account event does not extend the current head")
            if event.state.subject != current.state.subject:
                raise AccountStateStoreError("account event cannot change account subject")

        self._write_event(event)
        self._atomic_write(self._head_path(subject), self._encode_head(event))
        verified = self._read_head(subject)
        if verified.event_id != event.event_id:
            raise AccountStateStoreError("account head failed post-write verification")
        return event.event_id

    def current(self, subject: str) -> AccountStateEventV1:
        return self._read_head(subject)
