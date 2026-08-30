"""StrategyQuant X-aligned Builder genetic evolution controls.

This module deliberately models only the GA behavior proved by retained SQX
144.2953 evidence. It preserves the native operator order and bounded settings
without claiming unobserved restart, migration, tree-repair, or population
replacement semantics.
"""

from __future__ import annotations

from dataclasses import dataclass
from random import Random
from typing import Any, Callable, Generic, Mapping, Sequence, TypeVar

CandidateT = TypeVar("CandidateT")

SQX_NATIVE_OPERATOR_PIPELINE: tuple[str, ...] = (
    "TournamentSelection",
    "NodeCrossover(two-point)",
    "NodeMutation",
    "fix/custom/stock-picker/exit-type operators",
)


class EvolutionConfigError(ValueError):
    """Raised when a Builder genetic setting is malformed or not yet proved."""


@dataclass(frozen=True, slots=True)
class EvolutionConfig:
    """Proved SQX Builder genetic controls for one evolution run."""

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
        """Population slots implied by SQX's per-island population control."""

        return self.population_size_per_island * self.island_count

    @classmethod
    def from_native_settings(cls, settings: Mapping[str, Any]) -> "EvolutionConfig":
        """Translate retained SQX GA setting names into the product contract.

        Only settings established by the six-run native evidence are accepted.
        Unknown extra keys are ignored because project archives contain many
        unrelated Builder settings; missing required GA keys fail closed.
        """

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
        try:
            return cls(
                population_size_per_island=int(settings["population"]),
                maximum_generations=int(settings["max_generations"]),
                crossover_probability_pct=int(settings["crossover_probability"]),
                mutation_probability_pct=int(settings["mutation_probability"]),
                island_count=int(settings["islands"]),
                migration_interval=int(settings["migration_modulo"]),
                migration_rate_pct=int(settings["migration_rate"]),
                fresh_blood_replace_similar=bool(settings["fresh_blood_replace_similar"]),
                fresh_blood_replace_weakest=bool(settings["fresh_blood_replace_weakest"]),
                filter_initial_population=bool(settings["filter_initial_population"]),
                restart_on_finish=bool(settings["restart_on_finish"]),
                restart_on_stagnation=bool(settings["restart_on_stagnation"]),
            )
        except EvolutionConfigError:
            raise
        except (TypeError, ValueError) as exc:
            raise EvolutionConfigError("native SQX GA settings contain invalid values") from exc


@dataclass(frozen=True, slots=True)
class IslandPlan:
    island_index: int
    population_size: int


def plan_islands(config: EvolutionConfig) -> tuple[IslandPlan, ...]:
    """Return SQX's configured island topology without inventing migration semantics."""

    return tuple(
        IslandPlan(island_index=index, population_size=config.population_size_per_island)
        for index in range(config.island_count)
    )


@dataclass(frozen=True, slots=True)
class VariationDecision:
    crossover_applied: bool
    mutation_applied: bool
    operator_pipeline: tuple[str, ...] = SQX_NATIVE_OPERATOR_PIPELINE


def decide_variation(
    config: EvolutionConfig,
    *,
    crossover_draw: float,
    mutation_draw: float,
) -> VariationDecision:
    """Apply SQX's independent crossover then mutation probability gates.

    Draws use [0.0, 1.0). This models only whether each native operator is
    invoked, not the private implementation of NodeCrossover/NodeMutation.
    """

    for name, draw in (("crossover", crossover_draw), ("mutation", mutation_draw)):
        if not 0.0 <= draw < 1.0:
            raise EvolutionConfigError(f"{name} draw must be in [0.0, 1.0)")
    return VariationDecision(
        crossover_applied=crossover_draw < config.crossover_probability_pct / 100.0,
        mutation_applied=mutation_draw < config.mutation_probability_pct / 100.0,
    )


@dataclass(frozen=True, slots=True)
class SelectedParents(Generic[CandidateT]):
    primary: CandidateT
    secondary: CandidateT


@dataclass(frozen=True, slots=True)
class VariationResult(Generic[CandidateT]):
    candidate: CandidateT
    decision: VariationDecision


class EvolutionKernel(Generic[CandidateT]):
    """Small, injected operator kernel matching the proved SQX operator order.

    Selection/crossover/mutation implementations remain explicit dependencies;
    this layer must not silently substitute generic genetic operators for SQX.
    """

    def __init__(
        self,
        *,
        select_parents: Callable[[Sequence[CandidateT], Random], SelectedParents[CandidateT]],
        crossover: Callable[[CandidateT, CandidateT, Random], CandidateT],
        mutate: Callable[[CandidateT, Random], CandidateT],
        postprocess: Callable[[CandidateT, Random], CandidateT],
    ) -> None:
        self._select_parents = select_parents
        self._crossover = crossover
        self._mutate = mutate
        self._postprocess = postprocess

    def vary_one(
        self,
        population: Sequence[CandidateT],
        config: EvolutionConfig,
        rng: Random,
    ) -> VariationResult[CandidateT]:
        if not population:
            raise EvolutionConfigError("population must not be empty")

        parents = self._select_parents(population, rng)
        crossover_draw = rng.random()
        mutation_draw = rng.random()
        decision = decide_variation(
            config,
            crossover_draw=crossover_draw,
            mutation_draw=mutation_draw,
        )

        candidate = parents.primary
        if decision.crossover_applied:
            candidate = self._crossover(parents.primary, parents.secondary, rng)
        if decision.mutation_applied:
            candidate = self._mutate(candidate, rng)
        candidate = self._postprocess(candidate, rng)
        return VariationResult(candidate=candidate, decision=decision)
