"""Initial validation policy, launch receipts, and evidence custody."""

from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal
import re
from typing import Any, ClassVar, Mapping

from .artifacts import ResultArtifactV1
from .canonical import ContentAddress, canonical_json_bytes
from .specs import (
    SpecValidationError,
    _AddressedSpec,
    _require_ref,
    _require_schema,
    _require_text,
    _utc_timestamp,
)


_METRIC_RE = re.compile(r"^[a-z][a-z0-9_.-]*$")
_OPERATORS = frozenset({"gt", "gte", "lt", "lte", "eq"})


def _metric_path(value: str) -> str:
    value = _require_text(value, "metric_path")
    if not _METRIC_RE.fullmatch(value) or ".." in value or value.endswith("."):
        raise SpecValidationError("metric_path must be a dotted lowercase metric path")
    return value


def _exact_decimal(value: Decimal, name: str) -> Decimal:
    if not isinstance(value, Decimal) or not value.is_finite():
        raise SpecValidationError(f"{name} must be a finite Decimal")
    return value


def _compare(actual: Decimal, operator: str, threshold: Decimal) -> bool:
    if operator == "gt":
        return actual > threshold
    if operator == "gte":
        return actual >= threshold
    if operator == "lt":
        return actual < threshold
    if operator == "lte":
        return actual <= threshold
    if operator == "eq":
        return actual == threshold
    raise AssertionError(f"unreachable operator: {operator}")


@dataclass(frozen=True, slots=True)
class RunReceiptV1(_AddressedSpec):
    """Immutable acknowledgement that one exact run invocation was launched."""

    KIND: ClassVar[str] = "run-receipt"

    run_ref: ContentAddress
    producer_build_ref: ContentAddress
    invocation_id: str
    issued_at: str

    def __post_init__(self) -> None:
        _require_ref(self.run_ref, "backtest-run", "run_ref")
        _require_ref(self.producer_build_ref, "engine-build", "producer_build_ref")
        object.__setattr__(
            self,
            "invocation_id",
            _require_text(self.invocation_id, "invocation_id"),
        )
        object.__setattr__(self, "issued_at", _utc_timestamp(self.issued_at, "issued_at"))

    def identity_payload(self) -> Mapping[str, Any]:
        return {
            "run_ref": str(self.run_ref),
            "producer_build_ref": str(self.producer_build_ref),
            "invocation_id": self.invocation_id,
            "issued_at": self.issued_at,
        }


@dataclass(frozen=True, slots=True)
class MetricGateV1:
    """One exact comparison over a numeric result metric."""

    metric_path: str
    operator: str
    threshold: Decimal

    def __post_init__(self) -> None:
        object.__setattr__(self, "metric_path", _metric_path(self.metric_path))
        if self.operator not in _OPERATORS:
            raise SpecValidationError(
                f"operator must be one of {sorted(_OPERATORS)}, got {self.operator!r}"
            )
        object.__setattr__(
            self,
            "threshold",
            _exact_decimal(self.threshold, "threshold"),
        )

    def identity_payload(self) -> Mapping[str, Any]:
        return {
            "metric_path": self.metric_path,
            "operator": self.operator,
            "threshold": self.threshold,
        }


@dataclass(frozen=True, slots=True)
class InitialValidationPlanV1(_AddressedSpec):
    """Backend-owned initial gate policy. Every gate must pass."""

    KIND: ClassVar[str] = "validation-plan"

    source_result_schema: str
    gates: tuple[MetricGateV1, ...]

    def __post_init__(self) -> None:
        object.__setattr__(
            self,
            "source_result_schema",
            _require_schema(self.source_result_schema, "source_result_schema"),
        )
        if not isinstance(self.gates, tuple):
            object.__setattr__(self, "gates", tuple(self.gates))
        if not self.gates:
            raise SpecValidationError("gates must not be empty")
        for gate in self.gates:
            if not isinstance(gate, MetricGateV1):
                raise SpecValidationError("gates must contain only MetricGateV1 values")
        ordered = tuple(
            sorted(
                self.gates,
                key=lambda gate: canonical_json_bytes(gate.identity_payload()),
            )
        )
        fingerprints = [
            canonical_json_bytes(gate.identity_payload()) for gate in ordered
        ]
        if len(set(fingerprints)) != len(fingerprints):
            raise SpecValidationError("gates must not contain duplicates")
        object.__setattr__(self, "gates", ordered)

    def identity_payload(self) -> Mapping[str, Any]:
        return {
            "source_result_schema": self.source_result_schema,
            "gates": tuple(gate.identity_payload() for gate in self.gates),
        }


@dataclass(frozen=True, slots=True)
class GateOutcomeV1:
    metric_path: str
    operator: str
    threshold: Decimal
    actual: Decimal
    passed: bool

    def __post_init__(self) -> None:
        object.__setattr__(self, "metric_path", _metric_path(self.metric_path))
        if self.operator not in _OPERATORS:
            raise SpecValidationError("invalid gate outcome operator")
        object.__setattr__(self, "threshold", _exact_decimal(self.threshold, "threshold"))
        object.__setattr__(self, "actual", _exact_decimal(self.actual, "actual"))
        if not isinstance(self.passed, bool):
            raise SpecValidationError("passed must be bool")
        if self.passed != _compare(self.actual, self.operator, self.threshold):
            raise SpecValidationError(
                "gate outcome passed value does not match comparison"
            )

    def gate_identity_payload(self) -> Mapping[str, Any]:
        return {
            "metric_path": self.metric_path,
            "operator": self.operator,
            "threshold": self.threshold,
        }

    def identity_payload(self) -> Mapping[str, Any]:
        return {
            **self.gate_identity_payload(),
            "actual": self.actual,
            "passed": self.passed,
        }


@dataclass(frozen=True, slots=True)
class ValidationDecisionV1(_AddressedSpec):
    """Immutable outcome of applying one validation plan to one result artifact."""

    KIND: ClassVar[str] = "validation-decision"

    plan_ref: ContentAddress
    result_ref: ContentAddress
    passed: bool
    outcomes: tuple[GateOutcomeV1, ...]

    def __post_init__(self) -> None:
        _require_ref(self.plan_ref, "validation-plan", "plan_ref")
        _require_ref(self.result_ref, "result", "result_ref")
        if not isinstance(self.passed, bool):
            raise SpecValidationError("passed must be bool")
        if not isinstance(self.outcomes, tuple):
            object.__setattr__(self, "outcomes", tuple(self.outcomes))
        if not self.outcomes:
            raise SpecValidationError("outcomes must not be empty")
        if any(not isinstance(outcome, GateOutcomeV1) for outcome in self.outcomes):
            raise SpecValidationError("outcomes must contain only GateOutcomeV1 values")

        keyed = tuple(
            sorted(
                (
                    canonical_json_bytes(outcome.gate_identity_payload()),
                    outcome,
                )
                for outcome in self.outcomes
            )
        )
        gate_keys = [key for key, _ in keyed]
        if len(set(gate_keys)) != len(gate_keys):
            raise SpecValidationError("outcomes must not contain duplicate gates")
        object.__setattr__(self, "outcomes", tuple(outcome for _, outcome in keyed))

        if self.passed != all(outcome.passed for outcome in self.outcomes):
            raise SpecValidationError("passed must equal conjunction of gate outcomes")

    def identity_payload(self) -> Mapping[str, Any]:
        return {
            "plan_ref": str(self.plan_ref),
            "result_ref": str(self.result_ref),
            "passed": self.passed,
            "outcomes": tuple(
                outcome.identity_payload() for outcome in self.outcomes
            ),
        }


@dataclass(frozen=True, slots=True)
class EvidenceManifestV1(_AddressedSpec):
    """Immutable set of evidence refs associated with one exact run."""

    KIND: ClassVar[str] = "evidence-manifest"

    run_ref: ContentAddress
    evidence_refs: tuple[ContentAddress, ...]

    def __post_init__(self) -> None:
        _require_ref(self.run_ref, "backtest-run", "run_ref")
        if not isinstance(self.evidence_refs, tuple):
            object.__setattr__(self, "evidence_refs", tuple(self.evidence_refs))
        if not self.evidence_refs:
            raise SpecValidationError("evidence_refs must not be empty")
        if any(not isinstance(ref, ContentAddress) for ref in self.evidence_refs):
            raise SpecValidationError(
                "evidence_refs must contain only ContentAddress values"
            )
        ordered = tuple(sorted(self.evidence_refs, key=str))
        if len(set(ordered)) != len(ordered):
            raise SpecValidationError("evidence_refs must not contain duplicates")
        object.__setattr__(self, "evidence_refs", ordered)

    def identity_payload(self) -> Mapping[str, Any]:
        return {
            "run_ref": str(self.run_ref),
            "evidence_refs": tuple(str(ref) for ref in self.evidence_refs),
        }


def _lookup_metric(payload: Mapping[str, Any], path: str) -> Decimal:
    current: Any = payload
    for component in path.split("."):
        if not isinstance(current, Mapping) or component not in current:
            raise SpecValidationError(f"missing validation metric: {path}")
        current = current[component]
    if isinstance(current, bool):
        raise SpecValidationError(
            f"validation metric {path} must be numeric, not bool"
        )
    if isinstance(current, int):
        return Decimal(current)
    if isinstance(current, Decimal) and current.is_finite():
        return current
    raise SpecValidationError(
        f"validation metric {path} must be int or finite Decimal"
    )


def evaluate_initial_validation(
    plan: InitialValidationPlanV1,
    result: ResultArtifactV1,
) -> ValidationDecisionV1:
    """Apply an exact initial policy to one compatible immutable result artifact."""

    if not isinstance(plan, InitialValidationPlanV1):
        raise SpecValidationError("plan must be InitialValidationPlanV1")
    if not isinstance(result, ResultArtifactV1):
        raise SpecValidationError("result must be ResultArtifactV1")
    if result.result_schema != plan.source_result_schema:
        raise SpecValidationError("result schema does not match validation plan")

    outcomes = tuple(
        GateOutcomeV1(
            gate.metric_path,
            gate.operator,
            gate.threshold,
            actual := _lookup_metric(result.payload, gate.metric_path),
            _compare(actual, gate.operator, gate.threshold),
        )
        for gate in plan.gates
    )
    return ValidationDecisionV1(
        plan_ref=plan.ref,
        result_ref=result.ref,
        passed=all(outcome.passed for outcome in outcomes),
        outcomes=outcomes,
    )
