"""Durable orchestration for one exact initial TraderCockpit backtest run.

This service owns custody, lifecycle, and persistence only. Trading behavior
remains entirely inside an injected ``BacktestEvaluatorV1`` implementation.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Protocol, runtime_checkable

from tradercockpit.domain import (
    BacktestRunSpecV1,
    ContentAddress,
    EvidenceManifestV1,
    InitialValidationPlanV1,
    ResultArtifactV1,
    RunLifecycleEventV1,
    RunReceiptV1,
    ValidationDecisionV1,
    build_initial_evidence_manifest,
    evaluate_initial_validation,
)

from .contracts import EngineContractError, resolve_backtest_inputs
from .evaluator import (
    BacktestEvaluatorV1,
    _evaluate_preflighted_backtest,
    preflight_backtest,
)
from .lifecycle import RunLifecycleStoreV1


@runtime_checkable
class ObjectStoreV1(Protocol):
    """Minimal immutable-object store required by the run service."""

    def put(self, value: object) -> ContentAddress:
        ...

    def resolve(self, ref: ContentAddress) -> object:
        ...


@dataclass(frozen=True, slots=True)
class InitialBacktestExecutionV1:
    """Refs for the complete durable initial execution/evidence chain."""

    run_ref: ContentAddress
    receipt_ref: ContentAddress
    result_ref: ContentAddress
    plan_ref: ContentAddress
    decision_ref: ContentAddress
    evidence_manifest_ref: ContentAddress
    lifecycle_event_ref: ContentAddress

    def __post_init__(self) -> None:
        expected = {
            "run_ref": "backtest-run",
            "receipt_ref": "run-receipt",
            "result_ref": "result",
            "plan_ref": "validation-plan",
            "decision_ref": "validation-decision",
            "evidence_manifest_ref": "evidence-manifest",
            "lifecycle_event_ref": "run-lifecycle-event",
        }
        for name, kind in expected.items():
            ref = getattr(self, name)
            if not isinstance(ref, ContentAddress) or ref.kind != kind:
                raise EngineContractError(f"{name} must reference {kind!r}")


def _resolve_run(store: ObjectStoreV1, run_ref: ContentAddress) -> BacktestRunSpecV1:
    if not isinstance(run_ref, ContentAddress) or run_ref.kind != "backtest-run":
        raise EngineContractError("run_ref must reference 'backtest-run'")
    try:
        value = store.resolve(run_ref)
    except KeyError as exc:
        raise EngineContractError(f"missing immutable run for ref {run_ref}") from exc
    if not isinstance(value, BacktestRunSpecV1):
        raise EngineContractError(
            f"run_ref resolved to {type(value).__name__}, expected BacktestRunSpecV1"
        )
    if value.ref != run_ref:
        raise EngineContractError("resolved run identity does not match run_ref")
    return value


def _publish_lifecycle(
    lifecycle: RunLifecycleStoreV1,
    event: RunLifecycleEventV1,
) -> ContentAddress:
    ref = lifecycle.publish(event)
    if ref != event.ref:
        raise EngineContractError("lifecycle store returned the wrong event ref")
    return ref


def _failed_event(
    run_ref: ContentAddress,
    invocation_id: str,
    issued_at: str,
    previous_event_ref: ContentAddress,
    receipt_ref: ContentAddress,
    reason_code: str,
    *,
    result_ref: ContentAddress | None = None,
    decision_ref: ContentAddress | None = None,
    evidence_manifest_ref: ContentAddress | None = None,
) -> RunLifecycleEventV1:
    return RunLifecycleEventV1(
        run_ref=run_ref,
        invocation_id=invocation_id,
        status="failed",
        occurred_at=issued_at,
        previous_event_ref=previous_event_ref,
        receipt_ref=receipt_ref,
        result_ref=result_ref,
        decision_ref=decision_ref,
        evidence_manifest_ref=evidence_manifest_ref,
        reason_code=reason_code,
    )


def execute_initial_backtest(
    run_ref: ContentAddress,
    store: ObjectStoreV1,
    lifecycle: RunLifecycleStoreV1,
    evaluator: BacktestEvaluatorV1,
    plan: InitialValidationPlanV1,
    *,
    invocation_id: str,
    issued_at: str,
) -> InitialBacktestExecutionV1:
    """Resolve, preflight, launch, validate, and persist one exact run.

    Status is producer-owned rather than inferred from artifact presence:
    ``ready`` is persisted before preflight, ``running`` only after the durable
    launch receipt exists, and a terminal ``passed``/``failed``/``refused`` event
    closes the invocation when the service reaches a governed outcome.

    The receipt is persisted immediately before evaluator execution. Therefore a
    producer failure leaves durable proof that launch occurred without falsely
    creating a result, validation decision, or evidence manifest.
    """

    if not isinstance(store, ObjectStoreV1):
        raise EngineContractError("store must implement ObjectStoreV1")
    if not isinstance(lifecycle, RunLifecycleStoreV1):
        raise EngineContractError("lifecycle must implement RunLifecycleStoreV1")
    if not isinstance(plan, InitialValidationPlanV1):
        raise EngineContractError("plan must be InitialValidationPlanV1")
    if not isinstance(run_ref, ContentAddress) or run_ref.kind != "backtest-run":
        raise EngineContractError("run_ref must reference 'backtest-run'")

    ready = RunLifecycleEventV1(
        run_ref=run_ref,
        invocation_id=invocation_id,
        status="ready",
        occurred_at=issued_at,
    )
    _publish_lifecycle(lifecycle, ready)

    try:
        run = _resolve_run(store, run_ref)
        inputs = resolve_backtest_inputs(run, store)
        descriptor = preflight_backtest(inputs, evaluator)
        if plan.source_result_schema != descriptor.result_schema:
            raise EngineContractError(
                "validation plan result schema does not match evaluator result schema"
            )
    except Exception:
        refused = RunLifecycleEventV1(
            run_ref=run_ref,
            invocation_id=invocation_id,
            status="refused",
            occurred_at=issued_at,
            previous_event_ref=ready.ref,
            reason_code="prelaunch_refused",
        )
        _publish_lifecycle(lifecycle, refused)
        raise

    try:
        if store.put(plan) != plan.ref:
            raise EngineContractError("store returned the wrong validation-plan ref")
    except Exception:
        refused = RunLifecycleEventV1(
            run_ref=run.ref,
            invocation_id=invocation_id,
            status="refused",
            occurred_at=issued_at,
            previous_event_ref=ready.ref,
            reason_code="policy_persistence_refused",
        )
        _publish_lifecycle(lifecycle, refused)
        raise

    receipt = RunReceiptV1(
        run_ref=run.ref,
        producer_build_ref=descriptor.engine_build_ref,
        invocation_id=invocation_id,
        issued_at=issued_at,
    )
    try:
        if store.put(receipt) != receipt.ref:
            raise EngineContractError("store returned the wrong run-receipt ref")
    except Exception:
        refused = RunLifecycleEventV1(
            run_ref=run.ref,
            invocation_id=invocation_id,
            status="refused",
            occurred_at=issued_at,
            previous_event_ref=ready.ref,
            reason_code="receipt_persistence_refused",
        )
        _publish_lifecycle(lifecycle, refused)
        raise

    running = RunLifecycleEventV1(
        run_ref=run.ref,
        invocation_id=invocation_id,
        status="running",
        occurred_at=issued_at,
        previous_event_ref=ready.ref,
        receipt_ref=receipt.ref,
    )
    _publish_lifecycle(lifecycle, running)

    try:
        result = _evaluate_preflighted_backtest(inputs, evaluator, descriptor)
    except Exception:
        failed = _failed_event(
            run.ref,
            invocation_id,
            issued_at,
            running.ref,
            receipt.ref,
            "evaluation_failed",
        )
        _publish_lifecycle(lifecycle, failed)
        raise

    try:
        if store.put(result) != result.ref:
            raise EngineContractError("store returned the wrong result ref")
    except Exception:
        failed = _failed_event(
            run.ref,
            invocation_id,
            issued_at,
            running.ref,
            receipt.ref,
            "result_persistence_failed",
        )
        _publish_lifecycle(lifecycle, failed)
        raise

    try:
        decision = evaluate_initial_validation(plan, result)
    except Exception:
        failed = _failed_event(
            run.ref,
            invocation_id,
            issued_at,
            running.ref,
            receipt.ref,
            "validation_error",
            result_ref=result.ref,
        )
        _publish_lifecycle(lifecycle, failed)
        raise

    try:
        if store.put(decision) != decision.ref:
            raise EngineContractError("store returned the wrong validation-decision ref")
    except Exception:
        failed = _failed_event(
            run.ref,
            invocation_id,
            issued_at,
            running.ref,
            receipt.ref,
            "decision_persistence_failed",
            result_ref=result.ref,
        )
        _publish_lifecycle(lifecycle, failed)
        raise

    try:
        evidence = build_initial_evidence_manifest(
            run.ref,
            receipt,
            result,
            plan,
            decision,
        )
    except Exception:
        failed = _failed_event(
            run.ref,
            invocation_id,
            issued_at,
            running.ref,
            receipt.ref,
            "evidence_build_failed",
            result_ref=result.ref,
            decision_ref=decision.ref,
        )
        _publish_lifecycle(lifecycle, failed)
        raise

    try:
        if store.put(evidence) != evidence.ref:
            raise EngineContractError("store returned the wrong evidence-manifest ref")
    except Exception:
        failed = _failed_event(
            run.ref,
            invocation_id,
            issued_at,
            running.ref,
            receipt.ref,
            "evidence_persistence_failed",
            result_ref=result.ref,
            decision_ref=decision.ref,
        )
        _publish_lifecycle(lifecycle, failed)
        raise

    if decision.passed:
        terminal = RunLifecycleEventV1(
            run_ref=run.ref,
            invocation_id=invocation_id,
            status="passed",
            occurred_at=issued_at,
            previous_event_ref=running.ref,
            receipt_ref=receipt.ref,
            result_ref=result.ref,
            decision_ref=decision.ref,
            evidence_manifest_ref=evidence.ref,
        )
    else:
        terminal = _failed_event(
            run.ref,
            invocation_id,
            issued_at,
            running.ref,
            receipt.ref,
            "validation_rejected",
            result_ref=result.ref,
            decision_ref=decision.ref,
            evidence_manifest_ref=evidence.ref,
        )
    terminal_ref = _publish_lifecycle(lifecycle, terminal)

    return InitialBacktestExecutionV1(
        run_ref=run.ref,
        receipt_ref=receipt.ref,
        result_ref=result.ref,
        plan_ref=plan.ref,
        decision_ref=decision.ref,
        evidence_manifest_ref=evidence.ref,
        lifecycle_event_ref=terminal_ref,
    )
