"""Durable operational status store for immutable run-lifecycle events.

Lifecycle status is intentionally stored separately from result/evidence objects:
immutable events are content addressed, while a small atomic head pointer answers
"what is the current state of this invocation?" without inferring from missing
artifacts.
"""

from __future__ import annotations

import hashlib
import os
from pathlib import Path
import tempfile
from typing import Any, Mapping

from tradercockpit.domain import (
    ContentAddress,
    RunLifecycleEventV1,
    canonical_json_bytes,
    canonical_json_loads,
    content_address,
)


_EVENT_SCHEMA = "tc.run-lifecycle-event-wire.v1"
_HEAD_SCHEMA = "tc.run-lifecycle-head.v1"
_TRANSITIONS = {
    "ready": frozenset({"running", "refused"}),
    "running": frozenset({"completed", "passed", "failed"}),
    "completed": frozenset(),
    "passed": frozenset(),
    "failed": frozenset(),
    "refused": frozenset(),
}


class LifecycleStoreError(RuntimeError):
    """Raised when lifecycle custody, transition, or persistence is invalid."""


def _object(value: Any, name: str) -> Mapping[str, Any]:
    if not isinstance(value, Mapping):
        raise LifecycleStoreError(f"{name} must be an object")
    return value


def _text(value: Any, name: str) -> str:
    if not isinstance(value, str) or not value:
        raise LifecycleStoreError(f"{name} must be a non-empty string")
    return value


def _exact_keys(value: Mapping[str, Any], expected: set[str], name: str) -> None:
    if set(value) != expected:
        raise LifecycleStoreError(f"{name} fields do not match the lifecycle schema")


def _ref(value: Any, name: str) -> ContentAddress:
    try:
        return ContentAddress.parse(_text(value, name))
    except ValueError as exc:
        raise LifecycleStoreError(f"{name} is not a valid content address") from exc


def _nullable_ref(value: Any, name: str) -> ContentAddress | None:
    return None if value is None else _ref(value, name)


def _encode_event(event: RunLifecycleEventV1) -> bytes:
    if not isinstance(event, RunLifecycleEventV1):
        raise LifecycleStoreError("event must be RunLifecycleEventV1")
    payload = event.identity_payload()
    expected = content_address(event.KIND, event.VERSION, payload)
    if expected != event.ref:
        raise LifecycleStoreError("event ref does not match event payload")
    return canonical_json_bytes(
        {
            "wire_schema": _EVENT_SCHEMA,
            "ref": str(event.ref),
            "payload": payload,
        }
    )


def _decode_event(data: bytes) -> RunLifecycleEventV1:
    try:
        raw = canonical_json_loads(data)
    except ValueError as exc:
        raise LifecycleStoreError(f"invalid lifecycle event bytes: {exc}") from exc
    envelope = _object(raw, "lifecycle event envelope")
    _exact_keys(envelope, {"wire_schema", "ref", "payload"}, "lifecycle event envelope")
    if envelope["wire_schema"] != _EVENT_SCHEMA:
        raise LifecycleStoreError("unsupported lifecycle event wire schema")
    declared_ref = _ref(envelope["ref"], "ref")
    if declared_ref.kind != RunLifecycleEventV1.KIND or declared_ref.version != 1:
        raise LifecycleStoreError("lifecycle event ref kind/version mismatch")
    payload = _object(envelope["payload"], "payload")
    _exact_keys(
        payload,
        {
            "run_ref",
            "invocation_id",
            "status",
            "occurred_at",
            "previous_event_ref",
            "receipt_ref",
            "result_ref",
            "decision_ref",
            "evidence_manifest_ref",
            "reason_code",
        },
        "lifecycle event payload",
    )
    expected_ref = content_address(RunLifecycleEventV1.KIND, 1, payload)
    if expected_ref != declared_ref:
        raise LifecycleStoreError("lifecycle event payload does not match declared ref")
    try:
        event = RunLifecycleEventV1(
            run_ref=_ref(payload["run_ref"], "run_ref"),
            invocation_id=_text(payload["invocation_id"], "invocation_id"),
            status=_text(payload["status"], "status"),
            occurred_at=_text(payload["occurred_at"], "occurred_at"),
            previous_event_ref=_nullable_ref(payload["previous_event_ref"], "previous_event_ref"),
            receipt_ref=_nullable_ref(payload["receipt_ref"], "receipt_ref"),
            result_ref=_nullable_ref(payload["result_ref"], "result_ref"),
            decision_ref=_nullable_ref(payload["decision_ref"], "decision_ref"),
            evidence_manifest_ref=_nullable_ref(
                payload["evidence_manifest_ref"], "evidence_manifest_ref"
            ),
            reason_code=(
                None
                if payload["reason_code"] is None
                else _text(payload["reason_code"], "reason_code")
            ),
        )
    except (TypeError, ValueError) as exc:
        raise LifecycleStoreError(f"invalid lifecycle event payload: {exc}") from exc
    if event.ref != declared_ref:
        raise LifecycleStoreError("reconstructed lifecycle event identity changed")
    return event


class FileRunLifecycleStore:
    """Atomic file-backed current-state index plus immutable event archive."""

    def __init__(self, root: Path | str):
        self.root = Path(root).resolve()
        self.lifecycle_root = self.root / "lifecycle"
        self.events_root = self.lifecycle_root / "events" / "v1"
        self.heads_root = self.lifecycle_root / "heads"
        self.events_root.mkdir(parents=True, exist_ok=True)
        self.heads_root.mkdir(parents=True, exist_ok=True)

    @staticmethod
    def _validate_lookup(run_ref: ContentAddress, invocation_id: str) -> None:
        if not isinstance(run_ref, ContentAddress) or run_ref.kind != "backtest-run":
            raise LifecycleStoreError("run_ref must reference 'backtest-run'")
        if not isinstance(invocation_id, str) or not invocation_id.strip():
            raise LifecycleStoreError("invocation_id must be a non-empty string")
        if invocation_id != invocation_id.strip():
            raise LifecycleStoreError("invocation_id must not contain surrounding whitespace")

    def _event_path(self, event_ref: ContentAddress) -> Path:
        if (
            not isinstance(event_ref, ContentAddress)
            or event_ref.kind != RunLifecycleEventV1.KIND
            or event_ref.version != 1
        ):
            raise LifecycleStoreError("event_ref must reference run-lifecycle-event v1")
        return self.events_root / f"{event_ref.sha256}.json"

    def _head_path(self, run_ref: ContentAddress, invocation_id: str) -> Path:
        self._validate_lookup(run_ref, invocation_id)
        invocation_digest = hashlib.sha256(invocation_id.encode("utf-8")).hexdigest()
        directory = self.heads_root / run_ref.sha256
        directory.mkdir(parents=True, exist_ok=True)
        return directory / f"{invocation_digest}.json"

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

    def _write_event(self, event: RunLifecycleEventV1) -> None:
        encoded = _encode_event(event)
        target = self._event_path(event.ref)
        if target.exists():
            if target.read_bytes() != encoded:
                raise LifecycleStoreError(
                    f"existing lifecycle event bytes disagree with immutable ref {event.ref}"
                )
            if _decode_event(encoded).ref != event.ref:
                raise LifecycleStoreError("existing lifecycle event cannot be reverified")
            return
        self._atomic_write(target, encoded)
        stored = target.read_bytes()
        if stored != encoded or _decode_event(stored).ref != event.ref:
            raise LifecycleStoreError("stored lifecycle event failed verification")

    def _encode_head(self, event: RunLifecycleEventV1) -> bytes:
        return canonical_json_bytes(
            {
                "wire_schema": _HEAD_SCHEMA,
                "run_ref": str(event.run_ref),
                "invocation_id": event.invocation_id,
                "event_ref": str(event.ref),
            }
        )

    def _read_head(
        self,
        run_ref: ContentAddress,
        invocation_id: str,
    ) -> RunLifecycleEventV1:
        target = self._head_path(run_ref, invocation_id)
        try:
            encoded = target.read_bytes()
        except FileNotFoundError as exc:
            raise KeyError((run_ref, invocation_id)) from exc
        try:
            raw = canonical_json_loads(encoded)
        except ValueError as exc:
            raise LifecycleStoreError(f"invalid lifecycle head bytes: {exc}") from exc
        head = _object(raw, "lifecycle head")
        _exact_keys(head, {"wire_schema", "run_ref", "invocation_id", "event_ref"}, "lifecycle head")
        if head["wire_schema"] != _HEAD_SCHEMA:
            raise LifecycleStoreError("unsupported lifecycle head schema")
        stored_run_ref = _ref(head["run_ref"], "run_ref")
        stored_invocation = _text(head["invocation_id"], "invocation_id")
        event_ref = _ref(head["event_ref"], "event_ref")
        if stored_run_ref != run_ref or stored_invocation != invocation_id:
            raise LifecycleStoreError("lifecycle head lookup identity mismatch")
        event_path = self._event_path(event_ref)
        try:
            event = _decode_event(event_path.read_bytes())
        except FileNotFoundError as exc:
            raise LifecycleStoreError("lifecycle head points to a missing event") from exc
        if event.ref != event_ref:
            raise LifecycleStoreError("lifecycle head event ref mismatch")
        if event.run_ref != run_ref or event.invocation_id != invocation_id:
            raise LifecycleStoreError("lifecycle event belongs to another invocation")
        return event

    def publish(self, event: RunLifecycleEventV1) -> ContentAddress:
        if not isinstance(event, RunLifecycleEventV1):
            raise LifecycleStoreError("event must be RunLifecycleEventV1")
        self._validate_lookup(event.run_ref, event.invocation_id)
        try:
            current = self._read_head(event.run_ref, event.invocation_id)
        except KeyError:
            current = None

        if current is None:
            if event.status != "ready" or event.previous_event_ref is not None:
                raise LifecycleStoreError("first lifecycle event must be ready")
        else:
            if current.terminal:
                raise LifecycleStoreError("terminal lifecycle event cannot transition")
            if event.previous_event_ref != current.ref:
                raise LifecycleStoreError("lifecycle event does not extend the current head")
            allowed = _TRANSITIONS[current.status]
            if event.status not in allowed:
                raise LifecycleStoreError(
                    f"invalid lifecycle transition {current.status!r} -> {event.status!r}"
                )
            if event.run_ref != current.run_ref or event.invocation_id != current.invocation_id:
                raise LifecycleStoreError("lifecycle transition changed invocation identity")

        self._write_event(event)
        head_path = self._head_path(event.run_ref, event.invocation_id)
        self._atomic_write(head_path, self._encode_head(event))
        resolved = self._read_head(event.run_ref, event.invocation_id)
        if resolved.ref != event.ref:
            raise LifecycleStoreError("lifecycle head did not advance to published event")
        return event.ref

    def current(
        self,
        run_ref: ContentAddress,
        invocation_id: str,
    ) -> RunLifecycleEventV1:
        self._validate_lookup(run_ref, invocation_id)
        return self._read_head(run_ref, invocation_id)