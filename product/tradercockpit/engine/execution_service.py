"""Durable execution-only orchestration for an exact TraderCockpit backtest run."""

from __future__ import annotations

from dataclasses import dataclass

from tradercockpit.domain import (
    BacktestRunSpecV1,
    ContentAddress,
    RunLifecycleEventV1,
    RunReceiptV1,
)

from .contracts import EngineContractError, resolve_backtest_inputs
from .evaluator import BacktestEvaluatorV1, _evaluate_preflighted_backtest, preflight_backtest
from .lifecycle import RunLifecycleStoreV1
from .run_service import ObjectStoreV1


@dataclass(frozen=True, slots=True)
class BacktestExecutionV1:
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


def _publish(lifecycle: RunLifecycleStoreV1, event: RunLifecycleEventV1) -> ContentAddress:
    ref = lifecycle.publish(event)
    if ref != event.ref:
        raise EngineContractError("lifecycle store returned the wrong event ref")
    return ref


def _resolve_run(store: ObjectStoreV1, run_ref: ContentAddress) -> BacktestRunSpecV1:
    if not isinstance(run_ref, ContentAddress) or run_ref.kind != "backtest-run":
        raise EngineContractError("run_ref must reference 'backtest-run'")
    try:
        value = store.resolve(run_ref)
    except KeyError as exc:
        raise EngineContractError(f"missing immutable run for ref {run_ref}") from exc
    if not isinstance(value, BacktestRunSpecV1) or value.ref != run_ref:
        raise EngineContractError("run_ref did not resolve to the exact BacktestRunSpecV1")
    return value


def execute_backtest(
    run_ref: ContentAddress,
    store: ObjectStoreV1,
    lifecycle: RunLifecycleStoreV1,
    evaluator: BacktestEvaluatorV1,
    *,
    invocation_id: str,
    issued_at: str,
) -> BacktestExecutionV1:
    """Execute and persist one run without claiming strategy-quality validation."""

    ready = RunLifecycleEventV1(
        run_ref=run_ref,
        invocation_id=invocation_id,
        status="ready",
        occurred_at=issued_at,
    )
    _publish(lifecycle, ready)

    try:
        run = _resolve_run(store, run_ref)
        inputs = resolve_backtest_inputs(run, store)
        descriptor = preflight_backtest(inputs, evaluator)
    except Exception:
        refused = RunLifecycleEventV1(
            run_ref=run_ref,
            invocation_id=invocation_id,
            status="refused",
            occurred_at=issued_at,
            previous_event_ref=ready.ref,
            reason_code="prelaunch_refused",
        )
        _publish(lifecycle, refused)
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
        _publish(lifecycle, refused)
        raise

    running = RunLifecycleEventV1(
        run_ref=run.ref,
        invocation_id=invocation_id,
        status="running",
        occurred_at=issued_at,
        previous_event_ref=ready.ref,
        receipt_ref=receipt.ref,
    )
    _publish(lifecycle, running)

    try:
        result = _evaluate_preflighted_backtest(inputs, evaluator, descriptor)
    except Exception:
        failed = RunLifecycleEventV1(
            run_ref=run.ref,
            invocation_id=invocation_id,
            status="failed",
            occurred_at=issued_at,
            previous_event_ref=running.ref,
            receipt_ref=receipt.ref,
            reason_code="evaluation_failed",
        )
        _publish(lifecycle, failed)
        raise

    try:
        if store.put(result) != result.ref:
            raise EngineContractError("store returned the wrong result ref")
    except Exception:
        failed = RunLifecycleEventV1(
            run_ref=run.ref,
            invocation_id=invocation_id,
            status="failed",
            occurred_at=issued_at,
            previous_event_ref=running.ref,
            receipt_ref=receipt.ref,
            reason_code="result_persistence_failed",
        )
        _publish(lifecycle, failed)
        raise

    completed = RunLifecycleEventV1(
        run_ref=run.ref,
        invocation_id=invocation_id,
        status="completed",
        occurred_at=issued_at,
        previous_event_ref=running.ref,
        receipt_ref=receipt.ref,
        result_ref=result.ref,
    )
    completed_ref = _publish(lifecycle, completed)
    return BacktestExecutionV1(
        run_ref=run.ref,
        receipt_ref=receipt.ref,
        result_ref=result.ref,
        lifecycle_event_ref=completed_ref,
    )
