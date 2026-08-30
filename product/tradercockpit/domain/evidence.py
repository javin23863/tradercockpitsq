"""Cross-object evidence custody for immutable TraderCockpit runs."""

from __future__ import annotations

from .artifacts import ResultArtifactV1
from .canonical import ContentAddress
from .specs import SpecValidationError, _require_ref
from .validation import (
    EvidenceManifestV1,
    InitialValidationPlanV1,
    RunReceiptV1,
    ValidationDecisionV1,
    evaluate_initial_validation,
)


def build_initial_evidence_manifest(
    run_ref: ContentAddress,
    receipt: RunReceiptV1,
    result: ResultArtifactV1,
    plan: InitialValidationPlanV1,
    decision: ValidationDecisionV1,
) -> EvidenceManifestV1:
    """Build initial evidence only after every custody link matches exactly."""

    _require_ref(run_ref, "backtest-run", "run_ref")
    if not isinstance(receipt, RunReceiptV1):
        raise SpecValidationError("receipt must be RunReceiptV1")
    if not isinstance(result, ResultArtifactV1):
        raise SpecValidationError("result must be ResultArtifactV1")
    if not isinstance(plan, InitialValidationPlanV1):
        raise SpecValidationError("plan must be InitialValidationPlanV1")
    if not isinstance(decision, ValidationDecisionV1):
        raise SpecValidationError("decision must be ValidationDecisionV1")
    if receipt.run_ref != run_ref:
        raise SpecValidationError("receipt belongs to a different run")
    if result.run_ref != run_ref:
        raise SpecValidationError("result belongs to a different run")
    if receipt.producer_build_ref != result.producer_build_ref:
        raise SpecValidationError("receipt/result producer build mismatch")
    if decision.plan_ref != plan.ref:
        raise SpecValidationError("decision belongs to a different validation plan")
    if decision.result_ref != result.ref:
        raise SpecValidationError("decision belongs to a different result")

    expected = evaluate_initial_validation(plan, result)
    if expected.ref != decision.ref:
        raise SpecValidationError(
            "decision content does not match plan/result evaluation"
        )

    return EvidenceManifestV1(
        run_ref,
        (receipt.ref, result.ref, plan.ref, decision.ref),
    )
