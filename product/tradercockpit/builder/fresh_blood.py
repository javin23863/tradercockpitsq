"""SQX 144.2953 Builder fresh-blood semantics.

This module reproduces the source-proven removal/refill mechanics behind the
Builder Fresh blood controls. It deliberately does not define fitness ordering,
candidate generation, fingerprints, or evaluation.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Callable, Generic, Sequence, TypeVar

from .evolution import SourceProvenance


CandidateT = TypeVar("CandidateT")
SQX_MAX_CANDIDATES_PER_FINGERPRINT = 2
SQX_WEAKEST_REPLACEMENT_MAX_PCT = 50


class FreshBloodError(ValueError):
    """Raised when fresh-blood inputs violate the bounded product contract."""


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
            "percentage and generation cadence."
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
            "After population evaluation/sort, similar replacement removes zero-fitness "
            "candidates and retains at most two per fingerprint. Weakest replacement "
            "runs on its generation cadence, clamps percentage to 50, targets at least "
            "one candidate, credits already-missing population slots, removes any "
            "remaining target from the sorted tail, then refills to population size."
        ),
    ),
)


def _exact_int(value: int, name: str) -> int:
    if type(value) is not int:
        raise FreshBloodError(f"{name} must be an integer")
    return value


def _fitness_value(value: float | int) -> float:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise FreshBloodError("fitness callback must return a number")
    return float(value)


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

    Source iteration order is preserved. Zero-fitness candidates are removed before
    fingerprint accounting. Among nonzero-fitness candidates, the first two
    occurrences of each integer fingerprint survive and later occurrences are
    removed.
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
        if _fitness_value(fitness(candidate)) == 0.0:
            removed_zero += 1
            continue

        value = fingerprint(candidate)
        if type(value) is not int:
            raise FreshBloodError("fingerprint callback must return an integer")

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
    refill_count: int


def plan_weakest_replacement(
    *,
    population_size: int,
    current_population_size: int,
    current_generation: int,
    replace_weakest_pct: int,
    replace_every_generations: int,
) -> WeakestReplacementPlan:
    """Plan native weakest-replacement counts after similarity pruning.

    TraderCockpit fails closed on negative percentages, invalid population counts,
    or a nonpositive cadence instead of reproducing native malformed-input crashes.
    For valid Builder states, the count mechanics match
    ``GPGenerationalEngine.removeWeakestStrategies`` followed by the refill branch
    in ``processFreshBloodSettings``.
    """

    population_size = _exact_int(population_size, "population_size")
    current_population_size = _exact_int(
        current_population_size, "current_population_size"
    )
    current_generation = _exact_int(current_generation, "current_generation")
    requested_pct = _exact_int(replace_weakest_pct, "replace_weakest_pct")
    cadence = _exact_int(
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
        raise FreshBloodError("replace_weakest_pct must not be negative")
    if cadence <= 0:
        raise FreshBloodError("replace_every_generations must be positive")

    effective_pct = min(requested_pct, SQX_WEAKEST_REPLACEMENT_MAX_PCT)
    scheduled = (
        current_generation != 0
        and current_generation % cadence == 0
    )
    already_missing = population_size - current_population_size

    target_fresh_count = 0
    weakest_to_remove = 0
    if scheduled:
        target_fresh_count = int((effective_pct / 100.0) * population_size)
        if target_fresh_count == 0:
            target_fresh_count = 1
        weakest_to_remove = max(target_fresh_count - already_missing, 0)

    refill_count = already_missing + weakest_to_remove

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
        refill_count=refill_count,
    )
