"""Verified read model for one initial TraderCockpit run invocation.

The UI/API must read explicit lifecycle state and durable typed artifacts. This
module deliberately refuses to infer status from whichever files happen to
exist.
"""

from __future__ import annotations

from dataclasses import dataclass

from tradercockpit.domain import (
    BacktestRunSpecV1,
    ContentAddress,
    EvidenceManifestV1,
    InitialValidationPlanV1,
    ResultArtifactV1,
    RunLifecycleEventV1,
    RunReceiptV1,
    ValidationDecisionV1,
)

from .contracts import (
    BacktestInputsV1,
    EngineContractError,
    SpecResolver,
    resolve_backtest_inputs,
)
from .lifecycle import RunLifecycleStoreV1


def _resolve_exact(
    resolver: SpecResolver,
    ref: ContentAddress,
    expected_type: type,
    name: str,
):
    try:
        value = resolver.resolve(ref)
    except KeyError as exc:
        raise EngineContractError(f"missing {name} for ref {ref}") from exc
    if not isinstance(value, expected_type):
        raise EngineContractError(
            f"{name} resolved to {type(value).__name__}, expected {expected_type.__name__}"
        )
    if getattr(value, "ref", None) != ref:
        raise EngineContractError(f"{name} identity does not match requested ref")
    return value


def _resolve_optional(
    resolver: SpecResolver,
    ref: ContentAddress | None,
    expected_type: type,
    name: str,
):
    if ref is None:
        return None
    return _resolve_exact(resolver, ref, expected_type, name)


@dataclass(frozen=True, slots=True)
class InitialRunReadModelV1:
    """Cross-checked current state, exact inputs, and durable owned artifacts."""

    run: BacktestRunSpecV1
    inputs: BacktestInputsV1
    lifecycle_event: RunLifecycleEventV1
    receipt: RunReceiptV1 | None
    result: ResultArtifactV1 | None
    plan: InitialValidationPlanV1 | None
    decision: ValidationDecisionV1 | None
    evidence_manifest: EvidenceManifestV1 | None

    @property
    def status(self) -> str:
        return self.lifecycle_event.status

    @property
    def terminal(self) -> bool:
        return self.lifecycle_event.terminal


def load_initial_run_read_model(
    run_ref: ContentAddress,
    invocation_id: str,
    resolver: SpecResolver,
    lifecycle: RunLifecycleStoreV1,
) -> InitialRunReadModelV1:
    """Load current state and reject stale/cross-run/forged input or artifact custody."""

    if not isinstance(run_ref, ContentAddress) or run_ref.kind != "backtest-run":
        raise EngineContractError("run_ref must reference 'backtest-run'")
    if not isinstance(invocation_id, str) or not invocation_id.strip():
        raise EngineContractError("invocation_id must be a non-empty string")
    if invocation_id != invocation_id.strip():
        raise EngineContractError("invocation_id must not contain surrounding whitespace")
    if not isinstance(resolver, SpecResolver):
        raise EngineContractError("resolver must implement SpecResolver")
    if not isinstance(lifecycle, RunLifecycleStoreV1):
        raise EngineContractError("lifecycle must implement RunLifecycleStoreV1")

    run = _resolve_exact(resolver, run_ref, BacktestRunSpecV1, "run")
    inputs = resolve_backtest_inputs(run, resolver)
    try:
        event = lifecycle.current(run_ref, invocation_id)
    except KeyError as exc:
        raise EngineContractError("run invocation has no lifecycle state") from exc
    if not isinstance(event, RunLifecycleEventV1):
        raise EngineContractError("lifecycle store returned a non-lifecycle event")
    if event.run_ref != run_ref or event.invocation_id != invocation_id:
        raise EngineContractError("lifecycle event belongs to another invocation")

    receipt = _resolve_optional(resolver, event.receipt_ref, RunReceiptV1, "receipt")
    result = _resolve_optional(resolver, event.result_ref, ResultArtifactV1, "result")
    decision = _resolve_optional(
        resolver,
        event.decision_ref,
        ValidationDecisionV1,
        "validation decision",
    )
    evidence = _resolve_optional(
        resolver,
        event.evidence_manifest_ref,
        EvidenceManifestV1,
        "evidence manifest",
    )
    plan = None
    if decision is not None:
        plan = _resolve_exact(
            resolver,
            decision.plan_ref,
            InitialValidationPlanV1,
            "validation plan",
        )

    if receipt is not None:
        if receipt.run_ref != run_ref:
            raise EngineContractError("receipt belongs to another run")
        if receipt.producer_build_ref != run.engine_build_ref:
            raise EngineContractError("receipt producer build does not match run")
        if receipt.invocation_id != invocation_id:
            raise EngineContractError("receipt invocation_id does not match lifecycle")

    if result is not None:
        if result.run_ref != run_ref:
            raise EngineContractError("result belongs to another run")
        if result.producer_build_ref != run.engine_build_ref:
            raise EngineContractError("result producer build does not match run")
        if receipt is None:
            raise EngineContractError("durable result exists without launch receipt")
        if result.producer_build_ref != receipt.producer_build_ref:
            raise EngineContractError("receipt/result producer build mismatch")

    if decision is not None:
        if result is None:
            raise EngineContractError("validation decision exists without result")
        if decision.result_ref != result.ref:
            raise EngineContractError("validation decision points to another result")
        if plan is None:
            raise AssertionError("decision plan resolution unexpectedly missing")
        if plan.source_result_schema != result.result_schema:
            raise EngineContractError("validation plan/result schema mismatch")

    if evidence is not None:
        if evidence.run_ref != run_ref:
            raise EngineContractError("evidence manifest belongs to another run")
        if receipt is None or result is None or decision is None or plan is None:
            raise EngineContractError("evidence manifest exists without complete evidence chain")
        expected_refs = {receipt.ref, result.ref, plan.ref, decision.ref}
        if set(evidence.evidence_refs) != expected_refs:
            raise EngineContractError("evidence manifest does not match the current run chain")

    if event.status == "passed":
        if decision is None or evidence is None or not decision.passed:
            raise EngineContractError("passed lifecycle state lacks a passing evidence chain")
    if event.status == "failed" and event.reason_code == "validation_rejected":
        if decision is None or evidence is None or decision.passed:
            raise EngineContractError(
                "validation_rejected lifecycle state lacks a failing evidence chain"
            )

    return InitialRunReadModelV1(
        run=run,
        inputs=inputs,
        lifecycle_event=event,
        receipt=receipt,
        result=result,
        plan=plan,
        decision=decision,
        evidence_manifest=evidence,
    )
