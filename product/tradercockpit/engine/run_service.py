"""Durable orchestration for one exact initial TraderCockpit backtest run.

This service owns custody and persistence only. Trading behavior remains entirely
inside an injected ``BacktestEvaluatorV1`` implementation.
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
    RunReceiptV1,
    ValidationDecisionV1,
    build_initial_evidence_manifest,
    evaluate_initial_validation,
)

from .contracts import EngineContractError, resolve_backtest_inputs
from .evaluator import BacktestEvaluatorV1, evaluate_backtest, preflight_backtest


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

    def __post_init__(self) -> None:
        expected = {
            "run_ref": "backtest-run",
            "receipt_ref": "run-receipt",
            "result_ref": "result",
            "plan_ref": "validation-plan",
            "decision_ref": "validation-decision",
            "evidence_manifest_ref": "evidence-manifest",
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


def execute_initial_backtest(
    run_ref: ContentAddress,
    store: ObjectStoreV1,
    evaluator: BacktestEvaluatorV1,
    plan: InitialValidationPlanV1,
    *,
    invocation_id: str,
    issued_at: str,
) -> InitialBacktestExecutionV1:
    """Resolve, preflight, launch, validate, and durably persist one exact run.

    The receipt is persisted immediately before evaluator execution. Therefore a
    producer failure can leave a durable launch receipt without falsely creating
    a result, validation decision, or evidence manifest.
    """

    if not isinstance(store, ObjectStoreV1):
        raise EngineContractError("store must implement ObjectStoreV1")
    if not isinstance(plan, InitialValidationPlanV1):
        raise EngineContractError("plan must be InitialValidationPlanV1")

    run = _resolve_run(store, run_ref)
    inputs = resolve_backtest_inputs(run, store)
    descriptor = preflight_backtest(inputs, evaluator)
    if plan.source_result_schema != descriptor.result_schema:
        raise EngineContractError(
            "validation plan result schema does not match evaluator result schema"
        )

    # Persist policy before launch so any later receipt always points to a durable
    # validation policy that existed at invocation time.
    if store.put(plan) != plan.ref:
        raise EngineContractError("store returned the wrong validation-plan ref")

    receipt = RunReceiptV1(
        run_ref=run.ref,
        producer_build_ref=descriptor.engine_build_ref,
        invocation_id=invocation_id,
        issued_at=issued_at,
    )
    if store.put(receipt) != receipt.ref:
        raise EngineContractError("store returned the wrong run-receipt ref")

    result = evaluate_backtest(inputs, evaluator)
    if store.put(result) != result.ref:
        raise EngineContractError("store returned the wrong result ref")

    decision = evaluate_initial_validation(plan, result)
    if store.put(decision) != decision.ref:
        raise EngineContractError("store returned the wrong validation-decision ref")

    evidence = build_initial_evidence_manifest(
        run.ref,
        receipt,
        result,
        plan,
        decision,
    )
    if store.put(evidence) != evidence.ref:
        raise EngineContractError("store returned the wrong evidence-manifest ref")

    return InitialBacktestExecutionV1(
        run_ref=run.ref,
        receipt_ref=receipt.ref,
        result_ref=result.ref,
        plan_ref=plan.ref,
        decision_ref=decision.ref,
        evidence_manifest_ref=evidence.ref,
    )
