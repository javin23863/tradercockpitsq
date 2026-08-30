"""Durable orchestration for exact TraderCockpit backtest runs.

This module is the single run authority for both execution-only runs and runs
that continue into TraderCockpit validation. Trading behavior remains inside an
injected ``BacktestEvaluatorV1`` implementation.
"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Callable, Protocol, runtime_checkable

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

from .contracts import BacktestInputsV1, EngineContractError, resolve_backtest_inputs
from .evaluator import (
    BacktestEvaluatorV1,
    EvaluatorDescriptorV1,
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
class BacktestExecutionV1:
    """Refs for one durable execution that makes no validation claim."""

    run_ref: ContentAddress
    receipt_ref: ContentAddress
    result_ref: ContentAddress
    lifecycle_event_ref: ContentAddress

    def __post_init__(self) -> None:
        expected = {
            "run_ref": "backtest-run",
            "receipt_ref": "run-receipt",
            "result_ref": "result",
            "lifecycle_event_ref": "run-lifecycle-event",
        }
        for name, kind in expected.items():
            ref = getattr(self, name)
            if not isinstance(ref, ContentAddress) or ref.kind != kind:
                raise EngineContractError(f"{name} must reference {kind!r}")


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


@dataclass(frozen=True, slots=True)
class _PreparedRunV1:
    run: BacktestRunSpecV1
    inputs: BacktestInputsV1
    descriptor: EvaluatorDescriptorV1
    ready: RunLifecycleEventV1


@dataclass(frozen=True, slots=True)
class _EvaluatedRunV1:
    run: BacktestRunSpecV1
    receipt: RunReceiptV1
    result: ResultArtifactV1
    running: RunLifecycleEventV1


def _utc_now_text() -> str:
    return (
        datetime.now(timezone.utc)
        .isoformat(timespec="microseconds")
        .replace("+00:00", "Z")
    )


def _validate_service_inputs(
    run_ref: ContentAddress,
    store: ObjectStoreV1,
    lifecycle: RunLifecycleStoreV1,
) -> None:
    if not isinstance(store, ObjectStoreV1):
        raise EngineContractError("store must implement ObjectStoreV1")
    if not isinstance(lifecycle, RunLifecycleStoreV1):
        raise EngineContractError("lifecycle must implement RunLifecycleStoreV1")
    if not isinstance(run_ref, ContentAddress) or run_ref.kind != "backtest-run":
        raise EngineContractError("run_ref must reference 'backtest-run'")


def _resolve_run(store: ObjectStoreV1, run_ref: ContentAddress) -> BacktestRunSpecV1:
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


def _refused_event(
    run_ref: ContentAddress,
    invocation_id: str,
    issued_at: str,
    previous_event_ref: ContentAddress,
    reason_code: str,
) -> RunLifecycleEventV1:
    return RunLifecycleEventV1(
        run_ref=run_ref,
        invocation_id=invocation_id,
        status="refused",
        occurred_at=issued_at,
        previous_event_ref=previous_event_ref,
        reason_code=reason_code,
    )


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


def _prepare_run(
    run_ref: ContentAddress,
    store: ObjectStoreV1,
    lifecycle: RunLifecycleStoreV1,
    evaluator: BacktestEvaluatorV1,
    *,
    invocation_id: str,
    issued_at: str,
) -> _PreparedRunV1:
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
    except Exception:
        _publish_lifecycle(
            lifecycle,
            _refused_event(
                run_ref,
                invocation_id,
                issued_at,
                ready.ref,
                "prelaunch_refused",
            ),
        )
        raise
    return _PreparedRunV1(run, inputs, descriptor, ready)


def _evaluate_prepared_run(
    prepared: _PreparedRunV1,
    store: ObjectStoreV1,
    lifecycle: RunLifecycleStoreV1,
    evaluator: BacktestEvaluatorV1,
    *,
    invocation_id: str,
    issued_at: str,
) -> _EvaluatedRunV1:
    run = prepared.run
    receipt = RunReceiptV1(
        run_ref=run.ref,
        producer_build_ref=prepared.descriptor.engine_build_ref,
        invocation_id=invocation_id,
        issued_at=issued_at,
    )
    try:
        if store.put(receipt) != receipt.ref:
            raise EngineContractError("store returned the wrong run-receipt ref")
    except Exception:
        _publish_lifecycle(
            lifecycle,
            _refused_event(
                run.ref,
                invocation_id,
                issued_at,
                prepared.ready.ref,
                "receipt_persistence_refused",
            ),
        )
        raise

    running = RunLifecycleEventV1(
        run_ref=run.ref,
        invocation_id=invocation_id,
        status="running",
        occurred_at=issued_at,
        previous_event_ref=prepared.ready.ref,
        receipt_ref=receipt.ref,
    )
    _publish_lifecycle(lifecycle, running)

    try:
        result = _evaluate_preflighted_backtest(
            prepared.inputs,
            evaluator,
            prepared.descriptor,
        )
    except Exception:
        _publish_lifecycle(
            lifecycle,
            _failed_event(
                run.ref,
                invocation_id,
                issued_at,
                running.ref,
                receipt.ref,
                "evaluation_failed",
            ),
        )
        raise

    try:
        if store.put(result) != result.ref:
            raise EngineContractError("store returned the wrong result ref")
    except Exception:
        _publish_lifecycle(
            lifecycle,
            _failed_event(
                run.ref,
                invocation_id,
                issued_at,
                running.ref,
                receipt.ref,
                "result_persistence_failed",
            ),
        )
        raise

    return _EvaluatedRunV1(run, receipt, result, running)


def execute_backtest(
    run_ref: ContentAddress,
    store: ObjectStoreV1,
    lifecycle: RunLifecycleStoreV1,
    evaluator: BacktestEvaluatorV1,
    *,
    invocation_id: str,
    issued_at: str,
    completion_clock: Callable[[], str] = _utc_now_text,
) -> BacktestExecutionV1:
    """Execute and persist one run without claiming validation or promotion."""

    _validate_service_inputs(run_ref, store, lifecycle)
    if not callable(completion_clock):
        raise EngineContractError("completion_clock must be callable")
    prepared = _prepare_run(
        run_ref,
        store,
        lifecycle,
        evaluator,
        invocation_id=invocation_id,
        issued_at=issued_at,
    )
    evaluated = _evaluate_prepared_run(
        prepared,
        store,
        lifecycle,
        evaluator,
        invocation_id=invocation_id,
        issued_at=issued_at,
    )
    completed_at = completion_clock()
    if not isinstance(completed_at, str) or not completed_at:
        raise EngineContractError("completion_clock must return a timestamp string")
    completed = RunLifecycleEventV1(
        run_ref=evaluated.run.ref,
        invocation_id=invocation_id,
        status="completed",
        occurred_at=completed_at,
        previous_event_ref=evaluated.running.ref,
        receipt_ref=evaluated.receipt.ref,
        result_ref=evaluated.result.ref,
    )
    completed_ref = _publish_lifecycle(lifecycle, completed)
    return BacktestExecutionV1(
        run_ref=evaluated.run.ref,
        receipt_ref=evaluated.receipt.ref,
        result_ref=evaluated.result.ref,
        lifecycle_event_ref=completed_ref,
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
    """Execute one run and continue into the governed initial validation chain."""

    _validate_service_inputs(run_ref, store, lifecycle)
    if not isinstance(plan, InitialValidationPlanV1):
        raise EngineContractError("plan must be InitialValidationPlanV1")

    prepared = _prepare_run(
        run_ref,
        store,
        lifecycle,
        evaluator,
        invocation_id=invocation_id,
        issued_at=issued_at,
    )
    if plan.source_result_schema != prepared.descriptor.result_schema:
        _publish_lifecycle(
            lifecycle,
            _refused_event(
                run_ref,
                invocation_id,
                issued_at,
                prepared.ready.ref,
                "prelaunch_refused",
            ),
        )
        raise EngineContractError(
            "validation plan result schema does not match evaluator result schema"
        )

    try:
        if store.put(plan) != plan.ref:
            raise EngineContractError("store returned the wrong validation-plan ref")
    except Exception:
        _publish_lifecycle(
            lifecycle,
            _refused_event(
                prepared.run.ref,
                invocation_id,
                issued_at,
                prepared.ready.ref,
                "policy_persistence_refused",
            ),
        )
        raise

    evaluated = _evaluate_prepared_run(
        prepared,
        store,
        lifecycle,
        evaluator,
        invocation_id=invocation_id,
        issued_at=issued_at,
    )
    run = evaluated.run
    receipt = evaluated.receipt
    result = evaluated.result
    running = evaluated.running

    try:
        decision = evaluate_initial_validation(plan, result)
    except Exception:
        _publish_lifecycle(
            lifecycle,
            _failed_event(
                run.ref,
                invocation_id,
                issued_at,
                running.ref,
                receipt.ref,
                "validation_error",
                result_ref=result.ref,
            ),
        )
        raise

    try:
        if store.put(decision) != decision.ref:
            raise EngineContractError("store returned the wrong validation-decision ref")
    except Exception:
        _publish_lifecycle(
            lifecycle,
            _failed_event(
                run.ref,
                invocation_id,
                issued_at,
                running.ref,
                receipt.ref,
                "decision_persistence_failed",
                result_ref=result.ref,
            ),
        )
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
        _publish_lifecycle(
            lifecycle,
            _failed_event(
                run.ref,
                invocation_id,
                issued_at,
                running.ref,
                receipt.ref,
                "evidence_build_failed",
                result_ref=result.ref,
                decision_ref=decision.ref,
            ),
        )
        raise

    try:
        if store.put(evidence) != evidence.ref:
            raise EngineContractError("store returned the wrong evidence-manifest ref")
    except Exception:
        _publish_lifecycle(
            lifecycle,
            _failed_event(
                run.ref,
                invocation_id,
                issued_at,
                running.ref,
                receipt.ref,
                "evidence_persistence_failed",
                result_ref=result.ref,
                decision_ref=decision.ref,
            ),
        )
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
