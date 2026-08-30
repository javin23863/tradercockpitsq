"""Strict custody boundary between immutable specs and a backtest engine."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Protocol, runtime_checkable

from tradercockpit.domain import (
    BacktestRunSpecV1,
    CandidateSpecV1,
    ContentAddress,
    DataSpecV1,
    EngineBuildSpecV1,
    ExecutionSpecV1,
    StrategySpecV1,
)


class EngineContractError(ValueError):
    """Raised when run custody cannot be resolved exactly."""


@runtime_checkable
class SpecResolver(Protocol):
    """Resolve one immutable production object by its exact content address."""

    def resolve(self, ref: ContentAddress) -> object:
        ...


def _require_exact(
    value: object,
    expected_type: type,
    expected_ref: ContentAddress,
    name: str,
):
    if not isinstance(value, expected_type):
        raise EngineContractError(
            f"{name} resolved to {type(value).__name__}, expected {expected_type.__name__}"
        )
    actual_ref = value.ref
    if actual_ref != expected_ref:
        raise EngineContractError(
            f"{name} content address mismatch: expected {expected_ref}, got {actual_ref}"
        )
    return value


@dataclass(frozen=True, slots=True)
class BacktestInputsV1:
    """Fully resolved, cross-checked inputs for one immutable BacktestRunSpec."""

    run: BacktestRunSpecV1
    candidate: CandidateSpecV1
    strategy: StrategySpecV1
    data: DataSpecV1
    execution: ExecutionSpecV1
    engine_build: EngineBuildSpecV1

    def __post_init__(self) -> None:
        if self.candidate.ref != self.run.candidate_ref:
            raise EngineContractError("candidate does not match run.candidate_ref")
        if self.strategy.ref != self.candidate.strategy_ref:
            raise EngineContractError("strategy does not match candidate.strategy_ref")
        if self.data.ref != self.run.data_ref:
            raise EngineContractError("data does not match run.data_ref")
        if self.execution.ref != self.run.execution_ref:
            raise EngineContractError("execution does not match run.execution_ref")
        if self.engine_build.ref != self.run.engine_build_ref:
            raise EngineContractError("engine build does not match run.engine_build_ref")


def resolve_backtest_inputs(
    run: BacktestRunSpecV1, resolver: SpecResolver
) -> BacktestInputsV1:
    """Resolve every input by exact ref and reject missing/stale/type-confused objects."""

    if not isinstance(run, BacktestRunSpecV1):
        raise EngineContractError("run must be BacktestRunSpecV1")
    if not isinstance(resolver, SpecResolver):
        raise EngineContractError("resolver must implement SpecResolver")

    try:
        candidate = _require_exact(
            resolver.resolve(run.candidate_ref),
            CandidateSpecV1,
            run.candidate_ref,
            "candidate",
        )
        strategy = _require_exact(
            resolver.resolve(candidate.strategy_ref),
            StrategySpecV1,
            candidate.strategy_ref,
            "strategy",
        )
        data = _require_exact(
            resolver.resolve(run.data_ref),
            DataSpecV1,
            run.data_ref,
            "data",
        )
        execution = _require_exact(
            resolver.resolve(run.execution_ref),
            ExecutionSpecV1,
            run.execution_ref,
            "execution",
        )
        engine_build = _require_exact(
            resolver.resolve(run.engine_build_ref),
            EngineBuildSpecV1,
            run.engine_build_ref,
            "engine_build",
        )
    except KeyError as exc:
        raise EngineContractError(f"missing immutable spec for ref {exc.args[0]}") from exc

    return BacktestInputsV1(
        run=run,
        candidate=candidate,
        strategy=strategy,
        data=data,
        execution=execution,
        engine_build=engine_build,
    )
