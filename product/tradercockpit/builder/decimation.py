"""SQX 144.2953 initial-population decimation count semantics.

This module reproduces only the source-proven count/custody behavior behind the
Builder "Generated decimation coefficient" control. Candidate fitness ordering
and candidate selection remain outside this bounded slice.
"""

from __future__ import annotations

from dataclasses import dataclass

from .evolution import SourceProvenance


class InitialPopulationDecimationError(ValueError):
    """Raised when initial-population decimation inputs are malformed."""


SQX_DECIMATION_SOURCE_PROVENANCE: tuple[SourceProvenance, ...] = (
    SourceProvenance(
        class_name="BuildMode",
        method="decimationCoef field",
        path=(
            "sources/engine-core/com/strategyquant/tradinglib/task/settings/"
            "buildmode/BuildMode.java"
        ),
        blob_sha="92a1596c49a71a7444166cb1a30e9468cbf27b00",
        conclusion="Builder stores the generated decimation coefficient as integer decimationCoef.",
    ),
    SourceProvenance(
        class_name="GeneticBuildEngine",
        method="getGPSettings",
        path=(
            "sources/plugins/TaskBuild/com/strategyquant/plugin/Task/impl/Build/"
            "GeneticBuildEngine.java"
        ),
        blob_sha="bfb72b3e0d9b72c4a989ae32bb62f33cacce1d61",
        conclusion=(
            "Builder copies BuildMode.decimationCoef into GPSettings.decimationCoefficient."
        ),
    ),
    SourceProvenance(
        class_name="GPGenerationalEngine",
        method=(
            "gpEvolution/generateInitialPopulation/decimateInitialPopulation/"
            "addExistingInitialPopulation"
        ),
        path=(
            "sources/engine-core/com/strategyquant/tradinglib/gp/"
            "GPGenerationalEngine.java"
        ),
        blob_sha="c5ff3193354a7168c5b5da11428a552cb1bbdc45",
        conclusion=(
            "Nonpositive coefficients normalize to 1. Initial generation requires "
            "(populationSize - initialPopulationSize) * coefficient filter-passing "
            "generated candidates; after sorting, excess generated candidates are "
            "removed before supplied initial-population candidates are added."
        ),
    ),
)


@dataclass(frozen=True, slots=True)
class InitialPopulationDecimationPlan:
    """Count-level SQX initial-population decimation plan for one island."""

    population_size_per_island: int
    supplied_initial_count: int
    requested_decimation_coefficient: int
    effective_decimation_coefficient: int
    generated_survivor_capacity: int
    filter_passing_generated_target: int
    generated_candidates_removed_after_sort: int


def _exact_int(value: int, name: str) -> int:
    if type(value) is not int:
        raise InitialPopulationDecimationError(f"{name} must be an integer")
    return value


def normalize_decimation_coefficient(value: int) -> int:
    """Match ``GPGenerationalEngine.generateInitialPopulation`` normalization."""

    value = _exact_int(value, "decimation_coefficient")
    return 1 if value <= 0 else value


def plan_initial_population_decimation(
    *,
    population_size_per_island: int,
    supplied_initial_count: int,
    decimation_coefficient: int,
) -> InitialPopulationDecimationPlan:
    """Plan SQX's count-level initial-population generation and decimation.

    ``supplied_initial_count`` is the number of actual candidates supplied to the
    native island engine. SQX reserves up to that many final population slots,
    generates ``coefficient`` times the remaining slot count using only generated
    candidates that pass initial evaluation/filtering, sorts those generated
    candidates, removes the excess, then adds supplied initial candidates until
    the configured per-island population is full.

    This function intentionally does not choose survivors or model fitness
    ordering. It only reproduces the source-proven counts.
    """

    population_size_per_island = _exact_int(
        population_size_per_island,
        "population_size_per_island",
    )
    supplied_initial_count = _exact_int(
        supplied_initial_count,
        "supplied_initial_count",
    )
    requested = _exact_int(
        decimation_coefficient,
        "decimation_coefficient",
    )

    if population_size_per_island <= 0:
        raise InitialPopulationDecimationError(
            "population_size_per_island must be positive"
        )
    if supplied_initial_count < 0:
        raise InitialPopulationDecimationError(
            "supplied_initial_count must not be negative"
        )

    effective = normalize_decimation_coefficient(requested)
    generated_survivor_capacity = max(
        population_size_per_island - supplied_initial_count,
        0,
    )
    filter_passing_generated_target = (
        generated_survivor_capacity * effective
    )
    generated_candidates_removed_after_sort = max(
        filter_passing_generated_target - generated_survivor_capacity,
        0,
    )

    return InitialPopulationDecimationPlan(
        population_size_per_island=population_size_per_island,
        supplied_initial_count=supplied_initial_count,
        requested_decimation_coefficient=requested,
        effective_decimation_coefficient=effective,
        generated_survivor_capacity=generated_survivor_capacity,
        filter_passing_generated_target=filter_passing_generated_target,
        generated_candidates_removed_after_sort=generated_candidates_removed_after_sort,
    )
