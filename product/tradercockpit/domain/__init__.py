"""TraderCockpit-owned immutable domain primitives."""

from .canonical import (
    CanonicalizationError,
    ContentAddress,
    canonical_json_bytes,
    canonical_sha256,
    content_address,
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
    "ExecutionModelV1",
    "ExecutionSpecV1",
    "SpecValidationError",
    "StrategySpecV1",
    "canonical_json_bytes",
    "canonical_sha256",
    "content_address",
]
