"""SQX 144.2953 Builder fresh-blood mechanics.

This module reproduces the source-visible pruning, weakest-replacement, and
refill-count/control-flow behavior behind Builder's Fresh blood controls.
Candidate fitness ordering and the strategy factory itself remain separate GA
concerns, but the refill generator's batch and lineage-context effects are part
of this contract because they affect downstream candidate identity/randomness.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Callable, Generic, Sequence, TypeVar

from .evolution import SourceProvenance


CandidateT = TypeVar("CandidateT")
JAVA_INT_MIN = -(2**31)
JAVA_INT_MAX = 2**31 - 1
SQX_MAX_CANDIDATES_PER_FINGERPRINT = 2
SQX_WEAKEST_REPLACEMENT_MAX_PCT = 50
SQX_FRESH_BLOOD_REFILL_GENERATION_TYPE = "Initial"
SQX_FRESH_BLOOD_REFILL_GENERATION_INDEX = 0


class FreshBloodError(ValueError):
    """Raised when fresh-blood inputs violate the TraderCockpit contract."""


SQX_FRESH_BLOOD_SOURCE_PROVENANCE: tuple[SourceProvenance, ...] = (
    SourceProvenance(
        class_name="BuildMode",
        method=(
            "freshBloodReplaceSimilar/freshBloodReplaceWeakest/"
            "freshBloodWeakestPct/freshBloodWeakestGenerations fields"
        ),
        path=(
            "sources/engine-core/com/strategyquant/tradinglib/task/settings/"
            "buildmode/BuildMode.java"
        ),
        blob_sha="92a1596c49a71a7444166cb1a30e9468cbf27b00",
        conclusion=(
            "Builder stores the two fresh-blood switches plus weakest-replacement "
            "percentage and generation cadence as Java primitive fields."
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
            "Builder maps fresh-blood switches, weakest percentage, and cadence "
            "directly into GPSettings."
        ),
    ),
    SourceProvenance(
        class_name="GPGenerationalEngine",
        method=(
            "processFreshBloodSettings/removeTooSimilarStrategies/"
            "removeWeakestStrategies/generateAdditionalCandidates"
        ),
        path=(
            "sources/engine-core/com/strategyquant/tradinglib/gp/"
            "GPGenerationalEngine.java"
        ),
        blob_sha="c5ff3193354a7168c5b5da11428a552cb1bbdc45",
        conclusion=(
            "After evaluation/sort, similar replacement removes exactly zero-fitness "
            "candidates and retains at most two per fingerprint. Weakest replacement "
            "runs on cadence, upper-clamps percentage to 50, targets at least one, "
            "credits missing slots, removes from the sorted tail, then refills. Refill "
            "candidates use Initial generation lineage (generation 0), batches capped "
            "at twice used compute threads, and normal completion performs one extra "
            "discarded generateRandomCandidate call."
        ),
    ),
)


def _java_int(value: int, name: str) -> int:
    if type(value) is not int:
        raise FreshBloodError(f"{name} must be an integer")
    if not JAVA_INT_MIN <= value <= JAVA_INT_MAX:
        raise FreshBloodError(f"{name} must fit a signed Java int")
    return value


def _fitness_is_zero(value: float | int) -> bool:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise FreshBloodError("fitness callback must return an int or float")
    # Java double equality used by SQX removes both +0.0 and -0.0; NaN and
    # infinities compare nonzero and are therefore not silently reclassified here.
    return value == 0.0


@dataclass(frozen=True, slots=True)
class SimilarityPruneResult(Generic[CandidateT]):
    retained: tuple[CandidateT, ...]
    removed_zero_fitness: int
    removed_excess_fingerprint: int

    @property
    def removed_count(self) -> int:
        return self.removed_zero_fitness + self.removed_excess_fingerprint


def prune_similar_population(
    population: Sequence[CandidateT],
    *,
    fitness: Callable[[CandidateT], float | int],
    fingerprint: Callable[[CandidateT], int],
) -> SimilarityPruneResult[CandidateT]:
    """Reproduce native ``removeTooSimilarStrategies`` without destructive mutation.

    Native iteration order is preserved. Zero-fitness candidates are removed
    before fingerprint accounting. Among nonzero-fitness candidates, the first
    two occurrences of each signed-Java-int fingerprint survive and later
    occurrences are removed.
    """

    if not isinstance(population, Sequence) or isinstance(
        population, (str, bytes, bytearray)
    ):
        raise FreshBloodError("population must be an ordered candidate sequence")

    counts: dict[int, int] = {}
    retained: list[CandidateT] = []
    removed_zero = 0
    removed_duplicate = 0

    for candidate in population:
        if _fitness_is_zero(fitness(candidate)):
            removed_zero += 1
            continue

        value = _java_int(fingerprint(candidate), "fingerprint callback value")
        seen = counts.get(value, 0)
        if seen >= SQX_MAX_CANDIDATES_PER_FINGERPRINT:
            removed_duplicate += 1
            continue

        counts[value] = seen + 1
        retained.append(candidate)

    return SimilarityPruneResult(
        retained=tuple(retained),
        removed_zero_fitness=removed_zero,
        removed_excess_fingerprint=removed_duplicate,
    )


@dataclass(frozen=True, slots=True)
class WeakestReplacementPlan:
    population_size: int
    current_population_size: int
    current_generation: int
    requested_replace_pct: int
    effective_replace_pct: int
    replace_every_generations: int
    scheduled: bool
    target_fresh_count: int
    already_missing_count: int
    weakest_to_remove: int
    refill_start_population_size: int
    refill_count: int
    refill_generation_type: str
    refill_generation_index: int
    normal_refill_discarded_candidate_factory_calls: int


def plan_weakest_replacement(
    *,
    population_size: int,
    current_population_size: int,
    current_generation: int,
    replace_weakest_pct: int,
    replace_every_generations: int,
) -> WeakestReplacementPlan:
    """Plan weakest replacement after any similarity pruning.

    For valid Builder settings, count mechanics reproduce
    ``GPGenerationalEngine.removeWeakestStrategies`` and its subsequent refill.

    Two validations are deliberate TraderCockpit-owned safety boundaries rather
    than claimed SQX behavior: negative percentages and nonpositive cadences are
    rejected. Native Java would turn a scheduled negative percentage into the
    minimum-one target, a negative cadence can still match modulo generations,
    and a zero cadence can raise arithmetic failure. Those malformed states are
    not useful product semantics, so TraderCockpit refuses them explicitly.
    """

    population_size = _java_int(population_size, "population_size")
    current_population_size = _java_int(
        current_population_size, "current_population_size"
    )
    current_generation = _java_int(current_generation, "current_generation")
    requested_pct = _java_int(replace_weakest_pct, "replace_weakest_pct")
    cadence = _java_int(
        replace_every_generations, "replace_every_generations"
    )

    if population_size <= 0:
        raise FreshBloodError("population_size must be positive")
    if not 0 <= current_population_size <= population_size:
        raise FreshBloodError(
            "current_population_size must be between 0 and population_size"
        )
    if current_generation < 0:
        raise FreshBloodError("current_generation must not be negative")
    if requested_pct < 0:
        raise FreshBloodError(
            "replace_weakest_pct must not be negative (TraderCockpit safety boundary)"
        )
    if cadence <= 0:
        raise FreshBloodError(
            "replace_every_generations must be positive (TraderCockpit safety boundary)"
        )

    effective_pct = min(requested_pct, SQX_WEAKEST_REPLACEMENT_MAX_PCT)
    scheduled = current_generation != 0 and current_generation % cadence == 0
    already_missing = population_size - current_population_size

    target_fresh_count = 0
    weakest_to_remove = 0
    if scheduled:
        target_fresh_count = int((effective_pct / 100.0) * population_size)
        if target_fresh_count == 0:
            target_fresh_count = 1
        weakest_to_remove = max(target_fresh_count - already_missing, 0)
        weakest_to_remove = min(weakest_to_remove, current_population_size)

    refill_start_population_size = current_population_size - weakest_to_remove
    refill_count = population_size - refill_start_population_size

    return WeakestReplacementPlan(
        population_size=population_size,
        current_population_size=current_population_size,
        current_generation=current_generation,
        requested_replace_pct=requested_pct,
        effective_replace_pct=effective_pct,
        replace_every_generations=cadence,
        scheduled=scheduled,
        target_fresh_count=target_fresh_count,
        already_missing_count=already_missing,
        weakest_to_remove=weakest_to_remove,
        refill_start_population_size=refill_start_population_size,
        refill_count=refill_count,
        refill_generation_type=SQX_FRESH_BLOOD_REFILL_GENERATION_TYPE,
        refill_generation_index=SQX_FRESH_BLOOD_REFILL_GENERATION_INDEX,
        normal_refill_discarded_candidate_factory_calls=1 if refill_count > 0 else 0,
    )


def additional_generation_batch_size(
    *,
    population_size: int,
    current_population_size: int,
    computed_threads: int,
) -> int:
    """Reproduce ``generateAdditionalCandidates`` batch sizing.

    Unlike initial-population decimation, fresh-blood refill does not double small
    final batches. It generates at most the remaining population slots and caps
    each batch at twice the currently used compute-thread count. A return of zero
    means the target population has been reached; on normal completion the native
    loop then makes one discarded candidate-factory call.
    """

    population_size = _java_int(population_size, "population_size")
    current_population_size = _java_int(
        current_population_size, "current_population_size"
    )
    computed_threads = _java_int(computed_threads, "computed_threads")
    if population_size <= 0:
        raise FreshBloodError("population_size must be positive")
    if not 0 <= current_population_size <= population_size:
        raise FreshBloodError(
            "current_population_size must be between 0 and population_size"
        )
    if computed_threads <= 0:
        raise FreshBloodError("computed_threads must be positive")

    remaining = population_size - current_population_size
    if remaining == 0:
        return 0
    thread_cap = computed_threads * 2
    if thread_cap > JAVA_INT_MAX:
        raise FreshBloodError(
            "computed-thread batch cap would overflow SQX Java int arithmetic"
        )
    return min(remaining, thread_cap)
