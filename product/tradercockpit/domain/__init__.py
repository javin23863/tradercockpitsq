"""TraderCockpit-owned immutable domain primitives."""

from .canonical import (
    CanonicalizationError,
    ContentAddress,
    canonical_json_bytes,
    canonical_sha256,
    content_address,
)
from .artifacts import ResultArtifactV1
from .validation import (
    EvidenceManifestV1,
    GateOutcomeV1,
    InitialValidationPlanV1,
    MetricGateV1,
    RunReceiptV1,
    ValidationDecisionV1,
    evaluate_initial_validation,
)
from .specs import (
    BacktestRunSpecV1,
    CandidateSpecV1,
    DataSpecV1,
    EngineBuildSpecV1,
    ExecutionModelV1,
    ExecutionSpecV1,
    SpecValidationError,
    StrategySpecV1,
)

__all__ = [
    "BacktestRunSpecV1",
    "CandidateSpecV1",
    "CanonicalizationError",
    "ContentAddress",
    "DataSpecV1",
    "EngineBuildSpecV1",
    "EvidenceManifestV1",
    "ExecutionModelV1",
    "ExecutionSpecV1",
    "GateOutcomeV1",
    "InitialValidationPlanV1",
    "MetricGateV1",
    "ResultArtifactV1",
    "RunReceiptV1",
    "SpecValidationError",
    "StrategySpecV1",
    "ValidationDecisionV1",
    "canonical_json_bytes",
    "canonical_sha256",
    "content_address",
    "evaluate_initial_validation",
]
