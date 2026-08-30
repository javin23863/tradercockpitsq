"""SQX 144.2953 initial-population generation/decimation mechanics.

This module reconstructs the source-proven count and control-flow semantics behind
Builder's ``Generated decimation coefficient``.  It deliberately distinguishes
SQX's acceptance threshold from the actual number of accepted generated
candidates: the native engine evaluates batches and can overshoot the threshold
before it sorts and decimates the generated population.
"""

from __future__ import annotations

from dataclasses import dataclass

from .evolution import SourceProvenance


JAVA_INT_MIN = -(2**31)
JAVA_INT_MAX = 2**31 - 1


class InitialPopulationDecimationError(ValueError):
    """Raised when initial-population generation/decimation input is malformed."""


SQX_DECIMATION_SOURCE_PROVENANCE: tuple[SourceProvenance, ...] = (
    SourceProvenance(
        class_name="BuildMode",
        method="decimationCoef field",
        path=(
            "sources/engine-core/com/strategyquant/tradinglib/task/settings/"
            "buildmode/BuildMode.java"
        ),
        blob_sha="92a1596c49a71a7444166cb1a30e9468cbf27b00",
        conclusion=(
            "Builder stores generated decimation as Java int field decimationCoef."
        ),
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
            "Nonpositive coefficients normalize to 1. The generated-count formula is "
            "an acceptance threshold, not an exact accepted count: evaluation batches "
            "can overshoot it. SQX then sorts and removes the actual excess before "
            "adding a shuffled supplied initial population. Normal threshold completion "
            "also performs one additional discarded generateRandomCandidate call."
        ),
    ),
)


@dataclass(frozen=True, slots=True)
class InitialPopulationDecimationPlan:
    """Source-level initial-generation/decimation plan for one SQX island.

    ``native_acceptance_threshold`` is the Java expression
    ``(populationSize - initialPopulationSize) * decimationCoefficient`` after
    native nonpositive-coefficient normalization.  It is *not* an exact accepted
    generated-candidate count.  The native engine evaluates batches until the
    accepted generated population reaches or exceeds the threshold.
    """

    population_size_per_island: int
    supplied_initial_count: int
    requested_decimation_coefficient: int
    effective_decimation_coefficient: int
    generated_survivor_capacity: int
    native_acceptance_threshold: int
    minimum_filter_passing_generated_count: int
    minimum_generated_candidates_removed_after_sort: int
    normal_completion_discarded_candidate_factory_calls: int
    supplied_initial_population_shuffle_invoked: bool


def _java_int(value: int, name: str) -> int:
    if type(value) is not int:
        raise InitialPopulationDecimationError(f"{name} must be an integer")
    if not JAVA_INT_MIN <= value <= JAVA_INT_MAX:
        raise InitialPopulationDecimationError(
            f"{name} must fit a signed Java int"
        )
    return value


def _checked_java_product(left: int, right: int, name: str) -> int:
    value = left * right
    if not JAVA_INT_MIN <= value <= JAVA_INT_MAX:
        raise InitialPopulationDecimationError(
            f"{name} would overflow SQX Java int arithmetic"
        )
    return value


def normalize_decimation_coefficient(value: int) -> int:
    """Match ``GPGenerationalEngine.generateInitialPopulation`` normalization."""

    value = _java_int(value, "decimation_coefficient")
    return 1 if value <= 0 else value


def plan_initial_population_decimation(
    *,
    population_size_per_island: int,
    supplied_initial_count: int,
    decimation_coefficient: int,
) -> InitialPopulationDecimationPlan:
    """Plan the source-proven initial-generation threshold and decimation floor.

    The result intentionally does not predict the exact number of accepted
    generated candidates because SQX's final evaluation batch may overshoot the
    threshold.  Use :func:`initial_generation_batch_size` to reproduce native
    batch sizing and :func:`generated_candidates_removed_after_sort` once the
    actual accepted-generated count is known.

    Java integer overflow is rejected instead of silently inheriting native
    wraparound.  That is a TraderCockpit fail-closed boundary around malformed
    configuration, not a claim that SQX itself raises on overflow.
    """

    population_size_per_island = _java_int(
        population_size_per_island,
        "population_size_per_island",
    )
    supplied_initial_count = _java_int(
        supplied_initial_count,
        "supplied_initial_count",
    )
    requested = _java_int(
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
    remaining_slots = population_size_per_island - supplied_initial_count
    native_threshold = _checked_java_product(
        remaining_slots,
        effective,
        "initial-population acceptance threshold",
    )
    generated_survivor_capacity = max(remaining_slots, 0)
    minimum_filter_passing_generated_count = max(native_threshold, 0)
    minimum_generated_candidates_removed_after_sort = (
        generated_candidates_removed_after_sort_from_counts(
            population_size_per_island=population_size_per_island,
            supplied_initial_count=supplied_initial_count,
            accepted_generated_count=minimum_filter_passing_generated_count,
        )
    )

    return InitialPopulationDecimationPlan(
        population_size_per_island=population_size_per_island,
        supplied_initial_count=supplied_initial_count,
        requested_decimation_coefficient=requested,
        effective_decimation_coefficient=effective,
        generated_survivor_capacity=generated_survivor_capacity,
        native_acceptance_threshold=native_threshold,
        minimum_filter_passing_generated_count=minimum_filter_passing_generated_count,
        minimum_generated_candidates_removed_after_sort=(
            minimum_generated_candidates_removed_after_sort
        ),
        normal_completion_discarded_candidate_factory_calls=1,
        supplied_initial_population_shuffle_invoked=supplied_initial_count > 0,
    )


def initial_generation_batch_size(
    plan: InitialPopulationDecimationPlan,
    *,
    accepted_generated_count: int,
    computed_threads: int,
) -> int:
    """Return SQX's next generated-candidate batch size before evaluation.

    A return value of ``0`` means the acceptance threshold is already satisfied.
    On normal native completion SQX then performs the one discarded candidate
    factory call recorded by ``normal_completion_discarded_candidate_factory_calls``.
    """

    if not isinstance(plan, InitialPopulationDecimationPlan):
        raise InitialPopulationDecimationError(
            "plan must be InitialPopulationDecimationPlan"
        )
    accepted_generated_count = _java_int(
        accepted_generated_count,
        "accepted_generated_count",
    )
    computed_threads = _java_int(computed_threads, "computed_threads")
    if accepted_generated_count < 0:
        raise InitialPopulationDecimationError(
            "accepted_generated_count must not be negative"
        )
    if computed_threads <= 0:
        raise InitialPopulationDecimationError("computed_threads must be positive")

    if accepted_generated_count >= plan.native_acceptance_threshold:
        return 0

    remaining = plan.native_acceptance_threshold - accepted_generated_count
    batch_size = remaining
    if batch_size < 5:
        batch_size *= 2

    thread_cap = _checked_java_product(
        computed_threads,
        2,
        "computed-thread batch cap",
    )
    if batch_size > thread_cap:
        batch_size = thread_cap
    return batch_size


def generated_candidates_removed_after_sort(
    plan: InitialPopulationDecimationPlan,
    *,
    accepted_generated_count: int,
) -> int:
    """Return native tail removals for the actual accepted generated population."""

    if not isinstance(plan, InitialPopulationDecimationPlan):
        raise InitialPopulationDecimationError(
            "plan must be InitialPopulationDecimationPlan"
        )
    return generated_candidates_removed_after_sort_from_counts(
        population_size_per_island=plan.population_size_per_island,
        supplied_initial_count=plan.supplied_initial_count,
        accepted_generated_count=accepted_generated_count,
    )


def generated_candidates_removed_after_sort_from_counts(
    *,
    population_size_per_island: int,
    supplied_initial_count: int,
    accepted_generated_count: int,
) -> int:
    """Reproduce ``decimateInitialPopulation`` removal count from actual counts."""

    population_size_per_island = _java_int(
        population_size_per_island,
        "population_size_per_island",
    )
    supplied_initial_count = _java_int(
        supplied_initial_count,
        "supplied_initial_count",
    )
    accepted_generated_count = _java_int(
        accepted_generated_count,
        "accepted_generated_count",
    )
    if population_size_per_island <= 0:
        raise InitialPopulationDecimationError(
            "population_size_per_island must be positive"
        )
    if supplied_initial_count < 0:
        raise InitialPopulationDecimationError(
            "supplied_initial_count must not be negative"
        )
    if accepted_generated_count < 0:
        raise InitialPopulationDecimationError(
            "accepted_generated_count must not be negative"
        )

    requested_removals = (
        accepted_generated_count
        - population_size_per_island
        + supplied_initial_count
    )
    if not JAVA_INT_MIN <= requested_removals <= JAVA_INT_MAX:
        raise InitialPopulationDecimationError(
            "decimation removal count would overflow SQX Java int arithmetic"
        )
    if requested_removals <= 0:
        return 0
    return min(requested_removals, accepted_generated_count)
