"""TraderCockpit-owned immutable domain primitives."""

from .canonical import (
    CanonicalizationError,
    ContentAddress,
    canonical_json_bytes,
    canonical_json_loads,
    canonical_sha256,
    content_address,
)
from .artifacts import ResultArtifactV1
from .evidence import build_initial_evidence_manifest
from .lifecycle import (
    RUN_LIFECYCLE_STATUSES,
    RUN_LIFECYCLE_TERMINAL_STATUSES,
    RunLifecycleEventV1,
)
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
    BuilderLineageSpecV1,
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
    "BuilderLineageSpecV1",
    "CandidateSpecV1",
    "CanonicalizationError",
    "ContentAddress",
    "DataSpecV1",
    "EngineBuildSpecV1",
    "ExecutionModelV1",
    "ExecutionSpecV1",
    "EvidenceManifestV1",
    "GateOutcomeV1",
    "InitialValidationPlanV1",
    "MetricGateV1",
    "RUN_LIFECYCLE_STATUSES",
    "RUN_LIFECYCLE_TERMINAL_STATUSES",
    "ResultArtifactV1",
    "RunLifecycleEventV1",
    "RunReceiptV1",
    "SpecValidationError",
    "StrategySpecV1",
    "ValidationDecisionV1",
    "build_initial_evidence_manifest",
    "canonical_json_bytes",
    "canonical_json_loads",
    "canonical_sha256",
    "content_address",
    "evaluate_initial_validation",
]
