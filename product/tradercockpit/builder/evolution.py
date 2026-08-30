"""StrategyQuant X 144.2953-aligned Builder evolution contracts.

This module contains only TraderCockpit-owned behavior that is established by
the retained SQX genetic-options screen, native six-run evidence, or recovered
144.2953 implementation. SQX tree/XML operators remain injected boundaries.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Callable, Generic, Mapping, Protocol, Sequence, TypeVar

CandidateT = TypeVar("CandidateT")

SQX_TOURNAMENT_SIZE = 3
SQX_TOURNAMENT_RANK_PROBABILITY = 0.8
SQX_CROSSOVER_MAX_POINTS = 2
SQX_CROSSOVER_PROBABILITY_SCOPE = "shuffled-pair"
SQX_MUTATION_PROBABILITY_SCOPE = "generated-object"

SQX_NATIVE_OPERATOR_PIPELINE: tuple[str, ...] = (
    "TournamentSelection",
    "NodeCrossover(max-points=2)",
    "NodeMutation",
    "FixNonRandomBlocks",
    "FixUnusedDependentFormulas",
    "FixCustomBlocks",
    "FixStockpickerBlocks",
    "FixNumberOfExitTypes",
)


class EvolutionConfigError(ValueError):
    """Raised when Builder genetic input is malformed or not yet supported."""


@dataclass(frozen=True, slots=True)
class SourceProvenance:
    class_name: str
    method: str
    path: str
    blob_sha: str
    conclusion: str


SQX_GA_SOURCE_PROVENANCE: tuple[SourceProvenance, ...] = (
    SourceProvenance(
        class_name="GeneticBuildEngine",
        method="getGPSettings",
        path="sources/plugins/TaskBuild/com/strategyquant/plugin/Task/impl/Build/GeneticBuildEngine.java",
        blob_sha="bfb72b3e0d9b72c4a989ae32bb62f33cacce1d61",
        conclusion=(
            "Maps Builder genetic settings into GPSettings; constructs "
            "NodeCrossover(max 2), NodeMutation, five fix operators, and TournamentSelection."
        ),
    ),
    SourceProvenance(
        class_name="TournamentSelection",
        method="select/select2/private select",
        path="sources/engine-core/com/strategyquant/tradinglib/gp/TournamentSelection.java",
        blob_sha="dd6491bd44a467985a3eca4ab8ab96b064b23d9f",
        conclusion=(
            "Natural-fitness selection uses size-3 sampling with replacement and "
            "0.8 probabilistic rank choice, with source-population duplicate culling."
        ),
    ),
    SourceProvenance(
        class_name="EvolutionPipeline",
        method="apply",
        path="sources/engine-core/com/strategyquant/tradinglib/gp/EvolutionPipeline.java",
        blob_sha="ed5cc26702e1a31841e6f746839259ee4ee40267",
        conclusion="Applies each evolutionary operator sequentially to the whole selected population.",
    ),
    SourceProvenance(
        class_name="NodeCrossover",
        method="apply/_apply/mate/makeCrossover",
        path="sources/engine-core/com/strategyquant/tradinglib/gp/strategies/NodeCrossover.java",
        blob_sha="68a928465858f1fd211b431d6a9da919b5f9244a",
        conclusion=(
            "Gates crossover per shuffled pair; when enabled chooses 1..2 crossover "
            "points, swaps compatible generated elements, validates, and falls back on failure."
        ),
    ),
    SourceProvenance(
        class_name="NodeMutation",
        method="apply/mutate",
        path="sources/engine-core/com/strategyquant/tradinglib/gp/strategies/NodeMutation.java",
        blob_sha="ff36748ba1baa17d00a104f768a0d0e4d95d772a",
        conclusion=(
            "Clones each candidate and gates mutation independently for each generated "
            "object; failed/no mutation falls back to an unmodified clone."
        ),
    ),
    SourceProvenance(
        class_name="GPEngine",
        method="start/getIslandParams",
        path="sources/engine-core/com/strategyquant/tradinglib/gp/GPEngine.java",
        blob_sha="7f4291509fd6a9c20472d3a5ce9294e48d064866",
        conclusion=(
            "Starts one GPIslandJob per configured island and supplies each island "
            "a cloned GPSettings instance and island-specific RNG seed."
        ),
    ),
    SourceProvenance(
        class_name="GPGenerationalEngine",
        method="nextEvolutionStep",
        path="sources/engine-core/com/strategyquant/tradinglib/gp/GPGenerationalEngine.java",
        blob_sha="c5ff3193354a7168c5b5da11428a552cb1bbdc45",
        conclusion=(
            "Selects a non-elite population first, applies the population pipeline, "
            "adds elites back, evaluates, processes fresh blood, then migrates."
        ),
    ),
    SourceProvenance(
        class_name="MersenneTwisterRng",
        method="probability",
        path="sources/platform-runtime/com/strategyquant/lib/random/MersenneTwisterRng.java",
        blob_sha="9bfbbcb583e0aa279cdec55d32eba47b32808a2c",
        conclusion=(
            "Normalized probability 1.0 succeeds without consuming nextDouble; "
            "other values consume nextDouble and compare draw < probability."
        ),
    ),
)


def _native_int(settings: Mapping[str, Any], key: str) -> int:
    value = settings[key]
    if type(value) is not int:
        raise EvolutionConfigError(f"{key} must be an integer")
    return value


def _native_bool(settings: Mapping[str, Any], key: str) -> bool:
    value = settings[key]
    if type(value) is not bool:
        raise EvolutionConfigError(f"{key} must be a boolean")
    return value


@dataclass(frozen=True, slots=True)
class EvolutionConfig:
    """Source- and runtime-proven SQX Builder genetic controls."""

    population_size_per_island: int
    maximum_generations: int
    crossover_probability_pct: int
    mutation_probability_pct: int
    island_count: int = 1
    migration_interval: int = 1
    migration_rate_pct: int = 0
    fresh_blood_replace_similar: bool = True
    fresh_blood_replace_weakest: bool = False
    filter_initial_population: bool = False
    restart_on_finish: bool = False
    restart_on_stagnation: bool = False

    def __post_init__(self) -> None:
        if self.population_size_per_island <= 0:
            raise EvolutionConfigError("population size per island must be positive")
        if self.maximum_generations <= 0:
            raise EvolutionConfigError("maximum generations must be positive")
        if self.island_count <= 0:
            raise EvolutionConfigError("island count must be positive")
        if self.migration_interval <= 0:
            raise EvolutionConfigError("migration interval must be positive")
        for name, value in (
            ("crossover probability", self.crossover_probability_pct),
            ("mutation probability", self.mutation_probability_pct),
            ("migration rate", self.migration_rate_pct),
        ):
            if not 0 <= value <= 100:
                raise EvolutionConfigError(f"{name} must be between 0 and 100")
        if self.restart_on_finish or self.restart_on_stagnation:
            raise EvolutionConfigError(
                "SQX restart behavior is not yet supported: native bounded GA evidence disabled restarts"
            )

    @property
    def planned_population_capacity(self) -> int:
        """Configured slots across source-proven per-island populations."""

        return self.population_size_per_island * self.island_count

    @classmethod
    def from_native_settings(cls, settings: Mapping[str, Any]) -> "EvolutionConfig":
        """Translate the retained native GA setting shape, failing closed on type drift."""

        required = (
            "population",
            "max_generations",
            "crossover_probability",
            "mutation_probability",
            "islands",
            "migration_modulo",
            "migration_rate",
            "fresh_blood_replace_similar",
            "fresh_blood_replace_weakest",
            "filter_initial_population",
            "restart_on_finish",
            "restart_on_stagnation",
        )
        missing = [key for key in required if key not in settings]
        if missing:
            raise EvolutionConfigError(
                "missing native SQX GA settings: " + ", ".join(sorted(missing))
            )

        return cls(
            population_size_per_island=_native_int(settings, "population"),
            maximum_generations=_native_int(settings, "max_generations"),
            crossover_probability_pct=_native_int(settings, "crossover_probability"),
            mutation_probability_pct=_native_int(settings, "mutation_probability"),
            island_count=_native_int(settings, "islands"),
            migration_interval=_native_int(settings, "migration_modulo"),
            migration_rate_pct=_native_int(settings, "migration_rate"),
            fresh_blood_replace_similar=_native_bool(settings, "fresh_blood_replace_similar"),
            fresh_blood_replace_weakest=_native_bool(settings, "fresh_blood_replace_weakest"),
            filter_initial_population=_native_bool(settings, "filter_initial_population"),
            restart_on_finish=_native_bool(settings, "restart_on_finish"),
            restart_on_stagnation=_native_bool(settings, "restart_on_stagnation"),
        )


@dataclass(frozen=True, slots=True)
class IslandPlan:
    island_index: int
    population_size: int


def plan_islands(config: EvolutionConfig) -> tuple[IslandPlan, ...]:
    """Return the configured island-local population targets.

    GPEngine starts one GPIslandJob per configured island and each island's
    GPGenerationalEngine refills toward the same GPSettings.populationSize.
    Migration execution is intentionally outside this bounded slice.
    """

    return tuple(
        IslandPlan(island_index=index, population_size=config.population_size_per_island)
        for index in range(config.island_count)
    )


class RandomSource(Protocol):
    def random(self) -> float: ...
    def randrange(self, stop: int) -> int: ...


def sqx_probability_gate(probability: float, rng: RandomSource) -> bool:
    """Reproduce SQX's normalized MersenneTwisterRng.probability gate semantics.

    This models gate behavior and RNG consumption, not the Mersenne Twister
    sequence itself. GA probabilities must already be normalized to [0, 1].
    """

    if not 0.0 <= probability <= 1.0:
        raise EvolutionConfigError("normalized probability must be between 0 and 1")
    return probability == 1.0 or rng.random() < probability


class TournamentSelection(Generic[CandidateT]):
    """TraderCockpit-owned reproduction of SQX 144.2953 tournament selection.

    Fitness and GP identity extraction remain product-domain callbacks. Selection
    mechanics match the recovered class: size-3 sampling with replacement,
    ascending fitness sort, 0.8 rank checks from best to worst, best fallback,
    and the recovered duplicate-identity culling used while filling the output.
    """

    def __init__(
        self,
        *,
        fitness: Callable[[CandidateT], float],
        identity: Callable[[CandidateT], Any],
    ) -> None:
        self._fitness = fitness
        self._identity = identity

    def select(
        self,
        population: Sequence[CandidateT],
        count: int,
        rng: RandomSource,
    ) -> tuple[CandidateT, ...]:
        if count < 0:
            raise EvolutionConfigError("selection count must not be negative")
        if count == 0:
            return ()
        if not population:
            raise EvolutionConfigError("population must not be empty")

        working = list(population)
        selected: list[CandidateT] = []
        duplicate_limit = int(count * 0.2)
        if duplicate_limit > 15:
            duplicate_limit = 10
        if duplicate_limit > 20:
            duplicate_limit //= 2

        for _ in range(count):
            winner = self._select_one(working, rng)
            selected.append(winner)

            # The retained 144.2953 decompilation performs these equivalent
            # counts twice before its duplicate threshold comparison.
            repeated = sum(
                1 for candidate in selected
                if self._identity(candidate) == self._identity(winner)
            )
            if repeated * 2 > duplicate_limit:
                self._remove_same_identity(working, winner)

        return tuple(selected)

    def _select_one(
        self,
        population: Sequence[CandidateT],
        rng: RandomSource,
    ) -> CandidateT:
        sampled = [
            population[rng.randrange(len(population))]
            for _ in range(SQX_TOURNAMENT_SIZE)
        ]
        sampled.sort(key=self._fitness)

        for rank in range(1, SQX_TOURNAMENT_SIZE + 1):
            if rng.random() < SQX_TOURNAMENT_RANK_PROBABILITY:
                return sampled[SQX_TOURNAMENT_SIZE - rank]

        return sampled[SQX_TOURNAMENT_SIZE - 1]

    def _remove_same_identity(
        self,
        population: list[CandidateT],
        selected: CandidateT,
    ) -> None:
        identity = self._identity(selected)
        index = 0
        while index < len(population):
            if (
                self._identity(population[index]) == identity
                and len(population) > 2
            ):
                population.pop(index)
                continue
            index += 1


PopulationOperator = Callable[
    [Sequence[CandidateT], EvolutionConfig, RandomSource],
    Sequence[CandidateT],
]


@dataclass(frozen=True, slots=True)
class EvolutionStepResult(Generic[CandidateT]):
    population: tuple[CandidateT, ...]
    selected_count: int
    operator_pipeline: tuple[str, ...] = SQX_NATIVE_OPERATOR_PIPELINE


class EvolutionKernel(Generic[CandidateT]):
    """Population-level SQX evolution pipeline with tree-specific operators injected.

    Tournament selection is source-reproduced above. NodeCrossover, NodeMutation,
    and the five fix operators depend on SQX's generated XML/tree model, so this
    kernel preserves them as explicit population-operator boundaries rather than
    substituting generic genetic operators.
    """

    def __init__(
        self,
        *,
        selector: TournamentSelection[CandidateT],
        crossover: PopulationOperator[CandidateT],
        mutate: PopulationOperator[CandidateT],
        fix_non_random_blocks: PopulationOperator[CandidateT],
        fix_unused_dependent_formulas: PopulationOperator[CandidateT],
        fix_custom_blocks: PopulationOperator[CandidateT],
        fix_stockpicker_blocks: PopulationOperator[CandidateT],
        fix_number_of_exit_types: PopulationOperator[CandidateT],
    ) -> None:
        self._selector = selector
        self._operators: tuple[PopulationOperator[CandidateT], ...] = (
            crossover,
            mutate,
            fix_non_random_blocks,
            fix_unused_dependent_formulas,
            fix_custom_blocks,
            fix_stockpicker_blocks,
            fix_number_of_exit_types,
        )

    def evolve_selected_population(
        self,
        population: Sequence[CandidateT],
        config: EvolutionConfig,
        rng: RandomSource,
        *,
        selection_count: int,
    ) -> EvolutionStepResult[CandidateT]:
        """Select, then apply SQX population operators in recovered source order.

        `selection_count` is explicit because SQX computes it as current
        population size minus elitism size. Elitism derivation is not part of
        this bounded slice and is therefore not guessed here.

        Crossover/mutation probability gates are deliberately not applied here:
        NodeCrossover owns a per-pair gate and NodeMutation owns per-generated-
        object gates.
        """

        selected = list(self._selector.select(population, selection_count, rng))
        candidates: Sequence[CandidateT] = selected
        for operator in self._operators:
            candidates = tuple(operator(candidates, config, rng))

        return EvolutionStepResult(
            population=tuple(candidates),
            selected_count=len(selected),
        )
