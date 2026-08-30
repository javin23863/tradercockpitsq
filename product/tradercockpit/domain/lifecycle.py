"""Immutable run-lifecycle events for operational TraderCockpit status.

Lifecycle is intentionally separate from validation evidence. Events are immutable
and content-addressed; a separate operational store owns the mutable "current"
pointer for one invocation.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any, ClassVar, Mapping

from .canonical import ContentAddress
from .specs import (
    SpecValidationError,
    _AddressedSpec,
    _require_ref,
    _require_text,
    _require_token,
    _utc_timestamp,
)


RUN_LIFECYCLE_STATUSES = frozenset({"ready", "running", "passed", "failed", "refused"})
RUN_LIFECYCLE_TERMINAL_STATUSES = frozenset({"passed", "failed", "refused"})


def _optional_ref(
    value: ContentAddress | None,
    kind: str,
    name: str,
) -> ContentAddress | None:
    if value is None:
        return None
    return _require_ref(value, kind, name)


@dataclass(frozen=True, slots=True)
class RunLifecycleEventV1(_AddressedSpec):
    """One immutable, non-inferred state transition for a run invocation."""

    KIND: ClassVar[str] = "run-lifecycle-event"

    run_ref: ContentAddress
    invocation_id: str
    status: str
    occurred_at: str
    previous_event_ref: ContentAddress | None = None
    receipt_ref: ContentAddress | None = None
    result_ref: ContentAddress | None = None
    decision_ref: ContentAddress | None = None
    evidence_manifest_ref: ContentAddress | None = None
    reason_code: str | None = None

    def __post_init__(self) -> None:
        _require_ref(self.run_ref, "backtest-run", "run_ref")
        object.__setattr__(self, "invocation_id", _require_text(self.invocation_id, "invocation_id"))
        if self.status not in RUN_LIFECYCLE_STATUSES:
            raise SpecValidationError(
                f"status must be one of {sorted(RUN_LIFECYCLE_STATUSES)}, got {self.status!r}"
            )
        object.__setattr__(self, "occurred_at", _utc_timestamp(self.occurred_at, "occurred_at"))
        object.__setattr__(
            self,
            "previous_event_ref",
            _optional_ref(self.previous_event_ref, self.KIND, "previous_event_ref"),
        )
        object.__setattr__(
            self,
            "receipt_ref",
            _optional_ref(self.receipt_ref, "run-receipt", "receipt_ref"),
        )
        object.__setattr__(
            self,
            "result_ref",
            _optional_ref(self.result_ref, "result", "result_ref"),
        )
        object.__setattr__(
            self,
            "decision_ref",
            _optional_ref(self.decision_ref, "validation-decision", "decision_ref"),
        )
        object.__setattr__(
            self,
            "evidence_manifest_ref",
            _optional_ref(
                self.evidence_manifest_ref,
                "evidence-manifest",
                "evidence_manifest_ref",
            ),
        )
        if self.reason_code is not None:
            object.__setattr__(
                self,
                "reason_code",
                _require_token(self.reason_code, "reason_code"),
            )

        self._validate_shape()

    def _validate_shape(self) -> None:
        refs = (
            self.receipt_ref,
            self.result_ref,
            self.decision_ref,
            self.evidence_manifest_ref,
        )

        # Artifact refs form a strict durable prefix. A decision cannot exist
        # without a result, and evidence cannot exist without a decision.
        seen_none = False
        for ref in refs:
            if ref is None:
                seen_none = True
            elif seen_none:
                raise SpecValidationError(
                    "lifecycle artifact refs must form receipt -> result -> decision -> evidence prefix"
                )

        if self.status == "ready":
            if self.previous_event_ref is not None or any(refs) or self.reason_code is not None:
                raise SpecValidationError(
                    "ready event must be the first event and contain no artifacts or reason"
                )
            return

        if self.previous_event_ref is None:
            raise SpecValidationError(f"{self.status} event must reference a previous event")

        if self.status == "running":
            if self.receipt_ref is None or any(ref is not None for ref in refs[1:]):
                raise SpecValidationError("running event requires only a durable receipt_ref")
            if self.reason_code is not None:
                raise SpecValidationError("running event must not contain a reason_code")
            return

        if self.status == "refused":
            if any(refs):
                raise SpecValidationError("refused event must not claim launch/result artifacts")
            if self.reason_code is None:
                raise SpecValidationError("refused event requires a reason_code")
            return

        if self.status == "passed":
            if any(ref is None for ref in refs):
                raise SpecValidationError(
                    "passed event requires receipt, result, decision, and evidence refs"
                )
            if self.reason_code is not None:
                raise SpecValidationError("passed event must not contain a reason_code")
            return

        if self.status == "failed":
            if self.receipt_ref is None:
                raise SpecValidationError("failed event requires proof that launch occurred")
            if self.reason_code is None:
                raise SpecValidationError("failed event requires a reason_code")
            return

        raise AssertionError(f"unreachable lifecycle status: {self.status}")

    @property
    def terminal(self) -> bool:
        return self.status in RUN_LIFECYCLE_TERMINAL_STATUSES

    def identity_payload(self) -> Mapping[str, Any]:
        return {
            "run_ref": str(self.run_ref),
            "invocation_id": self.invocation_id,
            "status": self.status,
            "occurred_at": self.occurred_at,
            "previous_event_ref": (
                None if self.previous_event_ref is None else str(self.previous_event_ref)
            ),
            "receipt_ref": None if self.receipt_ref is None else str(self.receipt_ref),
            "result_ref": None if self.result_ref is None else str(self.result_ref),
            "decision_ref": None if self.decision_ref is None else str(self.decision_ref),
            "evidence_manifest_ref": (
                None
                if self.evidence_manifest_ref is None
                else str(self.evidence_manifest_ref)
            ),
            "reason_code": self.reason_code,
        }
