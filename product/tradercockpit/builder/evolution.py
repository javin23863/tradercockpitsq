"""SQX-aligned population mechanics used by TraderCockpit Builder search.

Selection, probability-gate, operator ordering, island sizing, and node-index
finalization preserve the retained SQX 144.2953 evidence. Restart flags are
configuration values only; the concrete restart/stagnation policy lives in the
TraderCockpit-owned search service and is not claimed as hidden SQX behavior.
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
    """Raised when Builder genetic input is malformed."""


@dataclass(frozen=True, slots=True)
class SourceProvenance:
    class_name: str
    method: str
    path: str
    blob_sha: str
    conclusion: str


SQX_GA_SOURCE_PROVENANCE: tuple[SourceProvenance, ...] = (
    SourceProvenance(
        "GeneticBuildEngine",
        "getGPSettings",
        "sources/plugins/TaskBuild/com/strategyquant/plugin/Task/impl/Build/GeneticBuildEngine.java",
        "bfb72b3e0d9b72c4a989ae32bb62f33cacce1d61",
        "Maps Builder genetic settings; constructs crossover, mutation, fixes and tournament selection.",
    ),
    SourceProvenance(
        "TournamentSelection",
        "select/select2/private select",
        "sources/engine-core/com/strategyquant/tradinglib/gp/TournamentSelection.java",
        "dd6491bd44a467985a3eca4ab8ab96b064b23d9f",
        "Natural-fitness selection samples three with replacement and uses 0.8 probabilistic rank choice.",
    ),
    SourceProvenance(
        "EvolutionPipeline",
        "apply",
        "sources/engine-core/com/strategyquant/tradinglib/gp/EvolutionPipeline.java",
        "ed5cc26702e1a31841e6f746839259ee4ee40267",
        "Applies population operators sequentially and then assigns node indices to negative outputs.",
    ),
    SourceProvenance(
        "NodeCrossover",
        "apply/_apply/mate/makeCrossover",
        "sources/engine-core/com/strategyquant/tradinglib/gp/strategies/NodeCrossover.java",
        "68a928465858f1fd211b431d6a9da919b5f9244a",
        "Gates crossover per shuffled pair and chooses one or two crossover points.",
    ),
    SourceProvenance(
        "NodeMutation",
        "apply/mutate",
        "sources/engine-core/com/strategyquant/tradinglib/gp/strategies/NodeMutation.java",
        "ff36748ba1baa17d00a104f768a0d0e4d95d772a",
        "Clones and gates mutation independently for each generated object.",
    ),
    SourceProvenance(
        "GPEngine",
        "start/getIslandParams",
        "sources/engine-core/com/strategyquant/tradinglib/gp/GPEngine.java",
        "7f4291509fd6a9c20472d3a5ce9294e48d064866",
        "Starts one island job per configured island with cloned settings and an island RNG seed.",
    ),
    SourceProvenance(
        "GPGenerationalEngine",
        "nextEvolutionStep",
        "sources/engine-core/com/strategyquant/tradinglib/gp/GPGenerationalEngine.java",
        "c5ff3193354a7168c5b5da11428a552cb1bbdc45",
        "Selects before the evolution pipeline and forwards island/generation context.",
    ),
    SourceProvenance(
        "MersenneTwisterRng",
        "probability",
        "sources/platform-runtime/com/strategyquant/lib/random/MersenneTwisterRng.java",
        "9bfbbcb583e0aa279cdec55d32eba47b32808a2c",
        "Probability 1.0 succeeds without consuming nextDouble; other values consume a draw and compare draw < probability.",
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
    """SQX-visible GA controls plus restart intent retained for product policy."""

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
        for name, value in (
            ("population_size_per_island", self.population_size_per_island),
            ("maximum_generations", self.maximum_generations),
            ("crossover_probability_pct", self.crossover_probability_pct),
            ("mutation_probability_pct", self.mutation_probability_pct),
            ("island_count", self.island_count),
            ("migration_interval", self.migration_interval),
            ("migration_rate_pct", self.migration_rate_pct),
        ):
            if type(value) is not int:
                raise EvolutionConfigError(f"{name} must be an integer")
        for name, value in (
            ("fresh_blood_replace_similar", self.fresh_blood_replace_similar),
            ("fresh_blood_replace_weakest", self.fresh_blood_replace_weakest),
            ("filter_initial_population", self.filter_initial_population),
            ("restart_on_finish", self.restart_on_finish),
            ("restart_on_stagnation", self.restart_on_stagnation),
        ):
            if type(value) is not bool:
                raise EvolutionConfigError(f"{name} must be a boolean")
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

    @property
    def planned_population_capacity(self) -> int:
        return self.population_size_per_island * self.island_count

    @classmethod
    def from_native_settings(cls, settings: Mapping[str, Any]) -> "EvolutionConfig":
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
            raise EvolutionConfigError("missing native SQX GA settings: " + ", ".join(sorted(missing)))
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
    return tuple(
        IslandPlan(island_index=index, population_size=config.population_size_per_island)
        for index in range(config.island_count)
    )


@dataclass(frozen=True, slots=True)
class EvolutionExecutionContext:
    island_index: int
    generation_index: int

    def __post_init__(self) -> None:
        if type(self.island_index) is not int:
            raise EvolutionConfigError("island_index must be an integer")
        if type(self.generation_index) is not int:
            raise EvolutionConfigError("generation_index must be an integer")
        if self.island_index < 0:
            raise EvolutionConfigError("island_index must not be negative")
        if self.generation_index <= 0:
            raise EvolutionConfigError("generation_index must be positive")


class RandomSource(Protocol):
    def random(self) -> float: ...
    def randrange(self, stop: int) -> int: ...


def sqx_probability_gate(probability: float, rng: RandomSource) -> bool:
    if not 0.0 <= probability <= 1.0:
        raise EvolutionConfigError("normalized probability must be between 0 and 1")
    return probability == 1.0 or rng.random() < probability


class TournamentSelection(Generic[CandidateT]):
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
            repeated = sum(
                1 for candidate in selected
                if self._identity(candidate) == self._identity(winner)
            )
            if repeated * 2 > duplicate_limit:
                self._remove_same_identity(working, winner)
        return tuple(selected)

    def _select_one(self, population: Sequence[CandidateT], rng: RandomSource) -> CandidateT:
        sampled = [population[rng.randrange(len(population))] for _ in range(SQX_TOURNAMENT_SIZE)]
        sampled.sort(key=self._fitness)
        for rank in range(1, SQX_TOURNAMENT_SIZE + 1):
            if rng.random() < SQX_TOURNAMENT_RANK_PROBABILITY:
                return sampled[SQX_TOURNAMENT_SIZE - rank]
        return sampled[SQX_TOURNAMENT_SIZE - 1]

    def _remove_same_identity(self, population: list[CandidateT], selected: CandidateT) -> None:
        identity = self._identity(selected)
        index = 0
        while index < len(population):
            if self._identity(population[index]) == identity and len(population) > 2:
                population.pop(index)
                continue
            index += 1


PopulationOperator = Callable[
    [Sequence[CandidateT], EvolutionConfig, RandomSource, EvolutionExecutionContext],
    Sequence[CandidateT],
]
NodeIndexReader = Callable[[CandidateT], int]
NodeIndexWriter = Callable[[CandidateT, int], CandidateT]


@dataclass(frozen=True, slots=True)
class EvolutionStepResult(Generic[CandidateT]):
    population: tuple[CandidateT, ...]
    selected_count: int
    context: EvolutionExecutionContext


class EvolutionKernel(Generic[CandidateT]):
    """Source-ordered population kernel retained for SQX parity-focused tests."""

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
        node_index: NodeIndexReader[CandidateT],
        with_node_index: NodeIndexWriter[CandidateT],
    ) -> None:
        self._selector = selector
        self._operators = (
            crossover,
            mutate,
            fix_non_random_blocks,
            fix_unused_dependent_formulas,
            fix_custom_blocks,
            fix_stockpicker_blocks,
            fix_number_of_exit_types,
        )
        self._node_index = node_index
        self._with_node_index = with_node_index

    def evolve_selected_population(
        self,
        population: Sequence[CandidateT],
        config: EvolutionConfig,
        rng: RandomSource,
        *,
        selection_count: int,
        context: EvolutionExecutionContext,
    ) -> EvolutionStepResult[CandidateT]:
        if context.island_index >= config.island_count:
            raise EvolutionConfigError("execution island_index exceeds configured islands")
        if context.generation_index > config.maximum_generations:
            raise EvolutionConfigError("execution generation_index exceeds configured maximum generations")
        candidates: Sequence[CandidateT] = list(self._selector.select(population, selection_count, rng))
        selected_count = len(candidates)
        for operator in self._operators:
            candidates = tuple(operator(candidates, config, rng, context))
        return EvolutionStepResult(
            population=self._finalize_node_indices(candidates),
            selected_count=selected_count,
            context=context,
        )

    def _finalize_node_indices(self, candidates: Sequence[CandidateT]) -> tuple[CandidateT, ...]:
        finalized = list(candidates)
        next_index = len(finalized)
        for position, candidate in enumerate(finalized):
            current_index = self._node_index(candidate)
            if type(current_index) is not int:
                raise EvolutionConfigError("candidate node index must be an integer")
            if current_index < 0:
                candidate = self._with_node_index(candidate, next_index)
                assigned = self._node_index(candidate)
                if type(assigned) is not int or assigned != next_index:
                    raise EvolutionConfigError("node index writer did not assign the requested integer index")
                finalized[position] = candidate
                next_index += 1
        return tuple(finalized)
