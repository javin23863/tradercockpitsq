"""Validated evaluator protocol for the first real TraderCockpit backtest."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Protocol, runtime_checkable

from tradercockpit.domain import ContentAddress
from tradercockpit.domain.artifacts import ResultArtifactV1
from tradercockpit.domain.specs import SpecValidationError, _require_ref, _require_schema

from .contracts import BacktestInputsV1, EngineContractError


@dataclass(frozen=True, slots=True)
class EvaluatorDescriptorV1:
    """Producer-owned declaration of one evaluator's executable contract."""

    engine_build_ref: ContentAddress
    semantic_schemas: tuple[str, ...]
    result_schema: str
    deterministic: bool

    def __post_init__(self) -> None:
        _require_ref(self.engine_build_ref, "engine-build", "engine_build_ref")
        schemas = tuple(
            _require_schema(item, "semantic_schema") for item in self.semantic_schemas
        )
        if not schemas:
            raise SpecValidationError("semantic_schemas must not be empty")
        if len(set(schemas)) != len(schemas):
            raise SpecValidationError("semantic_schemas must not contain duplicates")
        object.__setattr__(self, "semantic_schemas", tuple(sorted(schemas)))
        object.__setattr__(
            self,
            "result_schema",
            _require_schema(self.result_schema, "result_schema"),
        )
        if not isinstance(self.deterministic, bool):
            raise SpecValidationError("deterministic must be bool")


@runtime_checkable
class BacktestEvaluatorV1(Protocol):
    @property
    def descriptor(self) -> EvaluatorDescriptorV1:
        ...

    def evaluate(self, inputs: BacktestInputsV1) -> ResultArtifactV1:
        ...


def preflight_backtest(
    inputs: BacktestInputsV1,
    evaluator: BacktestEvaluatorV1,
) -> EvaluatorDescriptorV1:
    """Validate evaluator/run compatibility without launching computation."""

    if not isinstance(inputs, BacktestInputsV1):
        raise EngineContractError("inputs must be BacktestInputsV1")
    if not isinstance(evaluator, BacktestEvaluatorV1):
        raise EngineContractError("evaluator must implement BacktestEvaluatorV1")

    descriptor = evaluator.descriptor
    if not isinstance(descriptor, EvaluatorDescriptorV1):
        raise EngineContractError("evaluator.descriptor must be EvaluatorDescriptorV1")
    if descriptor.engine_build_ref != inputs.engine_build.ref:
        raise EngineContractError("evaluator build does not match run engine build")
    if inputs.strategy.semantic_schema not in descriptor.semantic_schemas:
        raise EngineContractError(
            f"unsupported strategy semantic schema: {inputs.strategy.semantic_schema}"
        )
    return descriptor


def evaluate_backtest(
    inputs: BacktestInputsV1,
    evaluator: BacktestEvaluatorV1,
) -> ResultArtifactV1:
    """Execute only after producer/build/schema custody has been proven exact."""

    descriptor = preflight_backtest(inputs, evaluator)
    result = evaluator.evaluate(inputs)
    if not isinstance(result, ResultArtifactV1):
        raise EngineContractError("evaluator returned non-ResultArtifactV1")
    if result.run_ref != inputs.run.ref:
        raise EngineContractError("result run_ref does not match evaluated run")
    if result.producer_build_ref != inputs.engine_build.ref:
        raise EngineContractError(
            "result producer build does not match evaluated engine build"
        )
    if result.result_schema != descriptor.result_schema:
        raise EngineContractError("result schema does not match evaluator descriptor")
    return result
