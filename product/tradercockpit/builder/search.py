"""TraderCockpit-owned Builder/evolution candidate-production service.

The search engine combines the retained SQX population mechanics with a small,
versioned TraderCockpit strategy language. SQX-proven selection/count/topology
semantics are preserved by the adjacent ingredient modules. Tree construction,
crossover, mutation, repair, restart policy, and the construction objective are
explicit TraderCockpit behavior; they are not presented as recovered SQX internals.

The objective produced here is a construction-fit objective over the candidate's
actual semantic fields. It is discovery evidence only. It is not P&L, a backtest,
validation, robustness, Retester output, or a champion decision.
"""

from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal
import os
from pathlib import Path
import random
import tempfile
from typing import Any, Mapping, Sequence

from tradercockpit.domain import ContentAddress
from tradercockpit.domain.canonical import canonical_json_bytes, canonical_json_loads, content_address
from tradercockpit.domain.specs import CandidateSpecV1, StrategySpecV1
from tradercockpit.storage import FileObjectStore

from .decimation import plan_initial_population_decimation
from .evolution import EvolutionConfig, TournamentSelection, sqx_probability_gate
from .fresh_blood import plan_weakest_replacement, prune_similar_population
from .migration import migration_inbox_capacity, plan_migration_receive, plan_migration_send
from .ranking import CandidateFitnessV1, RankingObjectiveV1, order_candidates


BUILDER_STRATEGY_SEMANTIC_SCHEMA = "tradercockpit.builder-strategy.v1"
BUILDER_SEARCH_STATE_SCHEMA = "tc.builder-search-state.v1"
BUILDER_SEARCH_READ_SCHEMA = "tc.builder-search.v1"
BUILDER_OBJECTIVE = "construction_fit"
BUILDER_OBJECTIVE_ROLE = "discovery"

_DIRECTIONS = ("long", "short")
_ENTRY_KINDS = ("ema", "momentum", "rsi", "sma")
_GENE_NAMES = (
    "direction",
    "entry_kind",
    "entry_period",
    "entry_threshold",
    "exit_bars",
    "position_bps",
)


class BuilderSearchError(ValueError):
    """Raised when product-owned Builder search input/state is invalid."""


def _exact_int(value: Any, name: str, *, minimum: int | None = None, maximum: int | None = None) -> int:
    if type(value) is not int:
        raise BuilderSearchError(f"{name} must be an integer")
    if minimum is not None and value < minimum:
        raise BuilderSearchError(f"{name} must be >= {minimum}")
    if maximum is not None and value > maximum:
        raise BuilderSearchError(f"{name} must be <= {maximum}")
    return value


def _exact_bool(value: Any, name: str) -> bool:
    if type(value) is not bool:
        raise BuilderSearchError(f"{name} must be a boolean")
    return value


def _text(value: Any, name: str) -> str:
    if not isinstance(value, str) or not value.strip() or value != value.strip():
        raise BuilderSearchError(f"{name} must be a non-empty trimmed string")
    return value


def _clamp(value: int, low: int, high: int) -> int:
    return min(max(value, low), high)


@dataclass(frozen=True, slots=True)
class BuilderGenomeV1:
    """Concrete strategy representation owned by Builder semantic schema v1."""

    direction: str
    entry_kind: str
    entry_period: int
    entry_threshold: int
    exit_bars: int
    position_bps: int

    def __post_init__(self) -> None:
        if self.direction not in _DIRECTIONS:
            raise BuilderSearchError("direction is not supported by builder-strategy.v1")
        if self.entry_kind not in _ENTRY_KINDS:
            raise BuilderSearchError("entry_kind is not supported by builder-strategy.v1")
        _exact_int(self.entry_period, "entry_period", minimum=2, maximum=200)
        _exact_int(self.entry_threshold, "entry_threshold", minimum=-100, maximum=100)
        _exact_int(self.exit_bars, "exit_bars", minimum=1, maximum=200)
        _exact_int(self.position_bps, "position_bps", minimum=10, maximum=1000)

    def semantics(self) -> Mapping[str, Any]:
        return {
            "direction": self.direction,
            "entry": {
                "kind": self.entry_kind,
                "period": self.entry_period,
                "threshold": self.entry_threshold,
            },
            "exit": {"bars": self.exit_bars},
            "risk": {"position_bps": self.position_bps},
        }

    def strategy(self) -> StrategySpecV1:
        return StrategySpecV1(
            semantic_schema=BUILDER_STRATEGY_SEMANTIC_SCHEMA,
            semantics=self.semantics(),
        )

    def genes(self) -> tuple[Any, ...]:
        return (
            self.direction,
            self.entry_kind,
            self.entry_period,
            self.entry_threshold,
            self.exit_bars,
            self.position_bps,
        )

    @classmethod
    def from_genes(cls, genes: Sequence[Any]) -> "BuilderGenomeV1":
        if len(genes) != len(_GENE_NAMES):
            raise BuilderSearchError("builder genome must contain six genes")
        return repair_builder_genome(
            direction=genes[0],
            entry_kind=genes[1],
            entry_period=genes[2],
            entry_threshold=genes[3],
            exit_bars=genes[4],
            position_bps=genes[5],
        )

    @classmethod
    def from_strategy(cls, strategy: StrategySpecV1) -> "BuilderGenomeV1":
        if not isinstance(strategy, StrategySpecV1):
            raise BuilderSearchError("strategy must be StrategySpecV1")
        if strategy.semantic_schema != BUILDER_STRATEGY_SEMANTIC_SCHEMA:
            raise BuilderSearchError("strategy semantic schema is not builder-strategy.v1")
        try:
            entry = strategy.semantics["entry"]
            exit_spec = strategy.semantics["exit"]
            risk = strategy.semantics["risk"]
            return cls(
                direction=strategy.semantics["direction"],
                entry_kind=entry["kind"],
                entry_period=entry["period"],
                entry_threshold=entry["threshold"],
                exit_bars=exit_spec["bars"],
                position_bps=risk["position_bps"],
            )
        except (KeyError, TypeError) as exc:
            raise BuilderSearchError("strategy does not match builder-strategy.v1 shape") from exc


def repair_builder_genome(
    *,
    direction: Any,
    entry_kind: Any,
    entry_period: Any,
    entry_threshold: Any,
    exit_bars: Any,
    position_bps: Any,
) -> BuilderGenomeV1:
    """Deterministic Class B/C structural repair for TraderCockpit genomes.

    Numeric outliers are clamped to the versioned schema bounds. Invalid enum
    values are repaired to stable product defaults. This is TraderCockpit-owned
    behavior, not a claim about SQX's five hidden tree-fix implementations.
    """

    repaired_direction = direction if direction in _DIRECTIONS else _DIRECTIONS[0]
    repaired_kind = entry_kind if entry_kind in _ENTRY_KINDS else _ENTRY_KINDS[0]
    numeric = (
        (entry_period, "entry_period", 2, 200),
        (entry_threshold, "entry_threshold", -100, 100),
        (exit_bars, "exit_bars", 1, 200),
        (position_bps, "position_bps", 10, 1000),
    )
    values: list[int] = []
    for value, name, low, high in numeric:
        if type(value) is not int:
            raise BuilderSearchError(f"{name} must be an integer before repair")
        values.append(_clamp(value, low, high))
    return BuilderGenomeV1(
        direction=repaired_direction,
        entry_kind=repaired_kind,
        entry_period=values[0],
        entry_threshold=values[1],
        exit_bars=values[2],
        position_bps=values[3],
    )


@dataclass(frozen=True, slots=True)
class BuilderSearchConfigV1:
    """Directly constructible TraderCockpit Builder/evolution configuration."""

    population_size_per_island: int = 8
    maximum_generations: int = 3
    crossover_probability_pct: int = 80
    mutation_probability_pct: int = 25
    island_count: int = 1
    migration_interval: int = 2
    migration_rate_pct: int = 10
    decimation_coefficient: int = 1
    fresh_blood_replace_similar: bool = True
    fresh_blood_replace_weakest: bool = False
    fresh_blood_weakest_pct: int = 10
    fresh_blood_every_generations: int = 2
    restart_on_finish: bool = False
    restart_on_stagnation: bool = False
    stagnation_generations: int = 2
    max_restarts: int = 0
    minimum_objective_score: int = 0
    random_seed: int = 1
    target_direction: str = "long"
    target_entry_kind: str = "ema"
    target_entry_period: int = 20
    target_entry_threshold: int = 0
    target_exit_bars: int = 20
    target_position_bps: int = 100
    source: str = "tradercockpit"
    native_source_ref: str | None = None

    def __post_init__(self) -> None:
        _exact_int(self.population_size_per_island, "population_size_per_island", minimum=2, maximum=10000)
        _exact_int(self.maximum_generations, "maximum_generations", minimum=1, maximum=100000)
        _exact_int(self.crossover_probability_pct, "crossover_probability_pct", minimum=0, maximum=100)
        _exact_int(self.mutation_probability_pct, "mutation_probability_pct", minimum=0, maximum=100)
        _exact_int(self.island_count, "island_count", minimum=1, maximum=128)
        _exact_int(self.migration_interval, "migration_interval", minimum=1)
        _exact_int(self.migration_rate_pct, "migration_rate_pct", minimum=0, maximum=100)
        _exact_int(self.decimation_coefficient, "decimation_coefficient", minimum=1, maximum=100)
        _exact_bool(self.fresh_blood_replace_similar, "fresh_blood_replace_similar")
        _exact_bool(self.fresh_blood_replace_weakest, "fresh_blood_replace_weakest")
        _exact_int(self.fresh_blood_weakest_pct, "fresh_blood_weakest_pct", minimum=0, maximum=100)
        _exact_int(self.fresh_blood_every_generations, "fresh_blood_every_generations", minimum=1)
        _exact_bool(self.restart_on_finish, "restart_on_finish")
        _exact_bool(self.restart_on_stagnation, "restart_on_stagnation")
        _exact_int(self.stagnation_generations, "stagnation_generations", minimum=1)
        _exact_int(self.max_restarts, "max_restarts", minimum=0, maximum=1000)
        _exact_int(self.minimum_objective_score, "minimum_objective_score")
        _exact_int(self.random_seed, "random_seed", minimum=0)
        BuilderGenomeV1(
            direction=self.target_direction,
            entry_kind=self.target_entry_kind,
            entry_period=self.target_entry_period,
            entry_threshold=self.target_entry_threshold,
            exit_bars=self.target_exit_bars,
            position_bps=self.target_position_bps,
        )
        if self.source not in {"tradercockpit", "sqx-import"}:
            raise BuilderSearchError("source must be 'tradercockpit' or 'sqx-import'")
        if self.source == "sqx-import":
            _text(self.native_source_ref, "native_source_ref")
        elif self.native_source_ref is not None:
            raise BuilderSearchError("native_source_ref is only valid for sqx-import provenance")

    @property
    def evolution_config(self) -> EvolutionConfig:
        return EvolutionConfig(
            population_size_per_island=self.population_size_per_island,
            maximum_generations=self.maximum_generations,
            crossover_probability_pct=self.crossover_probability_pct,
            mutation_probability_pct=self.mutation_probability_pct,
            island_count=self.island_count,
            migration_interval=self.migration_interval,
            migration_rate_pct=self.migration_rate_pct,
            fresh_blood_replace_similar=self.fresh_blood_replace_similar,
            fresh_blood_replace_weakest=self.fresh_blood_replace_weakest,
            filter_initial_population=self.decimation_coefficient > 1,
            restart_on_finish=self.restart_on_finish,
            restart_on_stagnation=self.restart_on_stagnation,
        )

    def identity_payload(self) -> Mapping[str, Any]:
        return {
            "population_size_per_island": self.population_size_per_island,
            "maximum_generations": self.maximum_generations,
            "crossover_probability_pct": self.crossover_probability_pct,
            "mutation_probability_pct": self.mutation_probability_pct,
            "island_count": self.island_count,
            "migration_interval": self.migration_interval,
            "migration_rate_pct": self.migration_rate_pct,
            "decimation_coefficient": self.decimation_coefficient,
            "fresh_blood_replace_similar": self.fresh_blood_replace_similar,
            "fresh_blood_replace_weakest": self.fresh_blood_replace_weakest,
            "fresh_blood_weakest_pct": self.fresh_blood_weakest_pct,
            "fresh_blood_every_generations": self.fresh_blood_every_generations,
            "restart_on_finish": self.restart_on_finish,
            "restart_on_stagnation": self.restart_on_stagnation,
            "stagnation_generations": self.stagnation_generations,
            "max_restarts": self.max_restarts,
            "minimum_objective_score": self.minimum_objective_score,
            "random_seed": self.random_seed,
            "target": {
                "direction": self.target_direction,
                "entry_kind": self.target_entry_kind,
                "entry_period": self.target_entry_period,
                "entry_threshold": self.target_entry_threshold,
                "exit_bars": self.target_exit_bars,
                "position_bps": self.target_position_bps,
            },
            "source": self.source,
            "native_source_ref": self.native_source_ref,
        }

    @property
    def ref(self) -> ContentAddress:
        return content_address("builder-config", 1, self.identity_payload())

    @classmethod
    def from_request(cls, value: Mapping[str, Any] | None) -> "BuilderSearchConfigV1":
        if value is None:
            return cls()
        if not isinstance(value, Mapping):
            raise BuilderSearchError("config must be an object")
        allowed = set(cls.__dataclass_fields__)
        unknown = sorted(set(value) - allowed)
        if unknown:
            raise BuilderSearchError("unknown config fields: " + ", ".join(unknown))
        return cls(**dict(value))

    @classmethod
    def from_sqx_settings(
        cls,
        settings: Mapping[str, Any],
        *,
        native_source_ref: str,
        **product_overrides: Any,
    ) -> "BuilderSearchConfigV1":
        """Import proven GA values while keeping provenance distinct from defaults."""

        native = EvolutionConfig.from_native_settings(settings)
        values = {
            "population_size_per_island": native.population_size_per_island,
            "maximum_generations": native.maximum_generations,
            "crossover_probability_pct": native.crossover_probability_pct,
            "mutation_probability_pct": native.mutation_probability_pct,
            "island_count": native.island_count,
            "migration_interval": native.migration_interval,
            "migration_rate_pct": native.migration_rate_pct,
            "fresh_blood_replace_similar": native.fresh_blood_replace_similar,
            "fresh_blood_replace_weakest": native.fresh_blood_replace_weakest,
            "restart_on_finish": native.restart_on_finish,
            "restart_on_stagnation": native.restart_on_stagnation,
            "source": "sqx-import",
            "native_source_ref": _text(native_source_ref, "native_source_ref"),
        }
        values.update(product_overrides)
        return cls(**values)


@dataclass(frozen=True, slots=True)
class _Individual:
    strategy: StrategySpecV1
    candidate: CandidateSpecV1
    objective: Decimal
    island_index: int
    generation_index: int
    node_index: int
    source: str
    parent_candidate_refs: tuple[ContentAddress, ...] = ()


class FileBuilderSearchStore:
    """Minimum durable search catalog around canonical immutable candidate objects."""

    def __init__(self, root: Path | str):
        self.root = Path(root).expanduser().resolve()
        self.searches_root = self.root / "builder-search" / "searches"
        self.searches_root.mkdir(parents=True, exist_ok=True)

    def _path(self, search_ref: ContentAddress) -> Path:
        if not isinstance(search_ref, ContentAddress) or search_ref.kind != "builder-search":
            raise BuilderSearchError("search_ref must reference builder-search")
        return self.searches_root / f"{search_ref.sha256}.json"

    def write(self, search_ref: ContentAddress, state: Mapping[str, Any]) -> None:
        payload = canonical_json_bytes(state)
        target = self._path(search_ref)
        fd, temp_name = tempfile.mkstemp(prefix=f".{search_ref.sha256}.", suffix=".tmp", dir=target.parent)
        temp = Path(temp_name)
        try:
            with os.fdopen(fd, "wb") as handle:
                handle.write(payload)
                handle.flush()
                os.fsync(handle.fileno())
            os.replace(temp, target)
        finally:
            if temp.exists():
                temp.unlink()
        if target.read_bytes() != payload:
            raise BuilderSearchError("durable Builder search state changed after write")

    def read(self, search_ref: ContentAddress) -> dict[str, Any]:
        try:
            value = canonical_json_loads(self._path(search_ref).read_bytes())
        except FileNotFoundError as exc:
            raise KeyError(search_ref) from exc
        if not isinstance(value, dict):
            raise BuilderSearchError("Builder search state must be an object")
        if value.get("search_ref") != str(search_ref):
            raise BuilderSearchError("Builder search state ref does not match path")
        return value

    def list_for_strategy(self, requested_strategy_ref: str) -> tuple[dict[str, Any], ...]:
        requested_strategy_ref = _text(requested_strategy_ref, "requested_strategy_ref")
        found: list[dict[str, Any]] = []
        for path in sorted(self.searches_root.glob("*.json")):
            value = canonical_json_loads(path.read_bytes())
            if isinstance(value, dict) and value.get("requested_strategy_ref") == requested_strategy_ref:
                found.append(value)
        return tuple(found)


def evaluate_construction_objective(strategy: StrategySpecV1, config: BuilderSearchConfigV1) -> Decimal:
    """Evaluate the real candidate semantics against explicit construction targets."""

    genome = BuilderGenomeV1.from_strategy(strategy)
    penalty = 0
    penalty += 1000 if genome.direction != config.target_direction else 0
    penalty += 600 if genome.entry_kind != config.target_entry_kind else 0
    penalty += abs(genome.entry_period - config.target_entry_period) * 10
    penalty += abs(genome.entry_threshold - config.target_entry_threshold) * 5
    penalty += abs(genome.exit_bars - config.target_exit_bars) * 8
    penalty += abs(genome.position_bps - config.target_position_bps) * 2
    return Decimal(10000 - penalty)


def _random_genome(rng: random.Random) -> BuilderGenomeV1:
    return BuilderGenomeV1(
        direction=_DIRECTIONS[rng.randrange(len(_DIRECTIONS))],
        entry_kind=_ENTRY_KINDS[rng.randrange(len(_ENTRY_KINDS))],
        entry_period=rng.randrange(2, 201),
        entry_threshold=rng.randrange(-100, 101),
        exit_bars=rng.randrange(1, 201),
        position_bps=rng.randrange(10, 1001),
    )


def _crossover_pair(
    left: BuilderGenomeV1,
    right: BuilderGenomeV1,
    rng: random.Random,
) -> tuple[BuilderGenomeV1, BuilderGenomeV1]:
    genes_left = left.genes()
    genes_right = right.genes()
    point_count = 1 + rng.randrange(2)
    available = list(range(1, len(_GENE_NAMES)))
    rng.shuffle(available)
    points = sorted(available[:point_count])
    boundaries = [0, *points, len(_GENE_NAMES)]
    child_a: list[Any] = []
    child_b: list[Any] = []
    use_left = True
    for start, end in zip(boundaries, boundaries[1:]):
        if use_left:
            child_a.extend(genes_left[start:end])
            child_b.extend(genes_right[start:end])
        else:
            child_a.extend(genes_right[start:end])
            child_b.extend(genes_left[start:end])
        use_left = not use_left
    return BuilderGenomeV1.from_genes(child_a), BuilderGenomeV1.from_genes(child_b)


def _mutate(genome: BuilderGenomeV1, rng: random.Random) -> BuilderGenomeV1:
    values = list(genome.genes())
    position = rng.randrange(len(values))
    if position == 0:
        values[position] = "short" if genome.direction == "long" else "long"
    elif position == 1:
        options = [item for item in _ENTRY_KINDS if item != genome.entry_kind]
        values[position] = options[rng.randrange(len(options))]
    elif position == 2:
        values[position] = genome.entry_period + rng.randrange(-20, 21)
    elif position == 3:
        values[position] = genome.entry_threshold + rng.randrange(-25, 26)
    elif position == 4:
        values[position] = genome.exit_bars + rng.randrange(-20, 21)
    else:
        values[position] = genome.position_bps + rng.randrange(-100, 101)
    return BuilderGenomeV1.from_genes(values)


def _fingerprint(individual: _Individual) -> int:
    return int(individual.strategy.ref.sha256[:8], 16) % (2**31)


def _sort_population(population: Sequence[_Individual]) -> list[_Individual]:
    return sorted(population, key=lambda item: (-item.objective, str(item.candidate.ref)))


class BuilderSearchService:
    """Execute and durably catalog one bounded synchronous Builder search."""

    def __init__(self, state_root: Path | str):
        self.root = Path(state_root).expanduser().resolve()
        self.root.mkdir(parents=True, exist_ok=True)
        self.objects = FileObjectStore(self.root)
        self.searches = FileBuilderSearchStore(self.root)

    def run(self, requested_strategy_ref: str, config: BuilderSearchConfigV1) -> dict[str, Any]:
        requested_strategy_ref = _text(requested_strategy_ref, "requested_strategy_ref")
        if not isinstance(config, BuilderSearchConfigV1):
            raise BuilderSearchError("config must be BuilderSearchConfigV1")
        search_ref = content_address(
            "builder-search",
            1,
            {"requested_strategy_ref": requested_strategy_ref, "config_ref": str(config.ref)},
        )
        rng = random.Random(config.random_seed)
        state = self._base_state(search_ref, requested_strategy_ref, config)
        self.searches.write(search_ref, state)

        populations: list[list[_Individual]] = []
        evaluations = 0
        for island_index in range(config.island_count):
            population, count = self._initial_population(search_ref, config, rng, island_index)
            populations.append(population)
            evaluations += count
        state["evaluations"] = evaluations
        state["status"] = "running"
        state["stage"] = "initial-population"
        self._record_population(state, populations, config)
        self.searches.write(search_ref, state)

        global_best = max(item.objective for population in populations for item in population)
        stagnant = 0
        restart_count = 0
        generation = 0

        while True:
            restart_requested = False
            for generation in range(1, config.maximum_generations + 1):
                next_populations: list[list[_Individual]] = []
                for island_index, population in enumerate(populations):
                    evolved, count = self._evolve_island(
                        search_ref,
                        config,
                        rng,
                        island_index,
                        generation,
                        population,
                    )
                    next_populations.append(evolved)
                    evaluations += count
                populations = self._migrate(search_ref, config, generation, next_populations)
                current_best = max(item.objective for population in populations for item in population)
                if current_best > global_best:
                    global_best = current_best
                    stagnant = 0
                else:
                    stagnant += 1
                state["status"] = "running"
                state["stage"] = "generation"
                state["generation"] = generation
                state["restart_count"] = restart_count
                state["evaluations"] = evaluations
                self._record_population(state, populations, config)
                self.searches.write(search_ref, state)

                if (
                    config.restart_on_stagnation
                    and stagnant >= config.stagnation_generations
                    and restart_count < config.max_restarts
                ):
                    restart_requested = True
                    break

            if restart_requested or (
                config.restart_on_finish and restart_count < config.max_restarts
            ):
                restart_count += 1
                stagnant = 0
                populations = []
                for island_index in range(config.island_count):
                    population, count = self._initial_population(
                        search_ref,
                        config,
                        rng,
                        island_index,
                        restart_index=restart_count,
                    )
                    populations.append(population)
                    evaluations += count
                state["status"] = "running"
                state["stage"] = "restart"
                state["generation"] = 0
                state["restart_count"] = restart_count
                state["evaluations"] = evaluations
                self._record_population(state, populations, config)
                self.searches.write(search_ref, state)
                continue
            break

        state["status"] = "complete"
        state["stage"] = "complete"
        state["generation"] = generation
        state["restart_count"] = restart_count
        state["evaluations"] = evaluations
        self._record_population(state, populations, config)
        self.searches.write(search_ref, state)
        return self.read(search_ref)

    def read(self, search_ref: ContentAddress | str) -> dict[str, Any]:
        ref = ContentAddress.parse(search_ref) if isinstance(search_ref, str) else search_ref
        if not isinstance(ref, ContentAddress) or ref.kind != "builder-search":
            raise BuilderSearchError("search_ref must reference builder-search")
        state = self.searches.read(ref)
        return self._read_model(state)

    def list_for_strategy(self, requested_strategy_ref: str) -> tuple[dict[str, Any], ...]:
        return tuple(self._read_model(state) for state in self.searches.list_for_strategy(requested_strategy_ref))

    def _base_state(
        self,
        search_ref: ContentAddress,
        requested_strategy_ref: str,
        config: BuilderSearchConfigV1,
    ) -> dict[str, Any]:
        return {
            "schema": BUILDER_SEARCH_STATE_SCHEMA,
            "search_ref": str(search_ref),
            "requested_strategy_ref": requested_strategy_ref,
            "config_ref": str(config.ref),
            "config": config.identity_payload(),
            "status": "created",
            "stage": "created",
            "generation": 0,
            "restart_count": 0,
            "evaluations": 0,
            "objective": {
                "name": BUILDER_OBJECTIVE,
                "direction": "maximize",
                "evidence_role": BUILDER_OBJECTIVE_ROLE,
                "meaning": "distance-weighted fit to explicit TraderCockpit construction targets",
            },
            "candidates": [],
        }

    def _make_individual(
        self,
        search_ref: ContentAddress,
        config: BuilderSearchConfigV1,
        genome: BuilderGenomeV1,
        *,
        island_index: int,
        generation_index: int,
        node_index: int,
        source: str,
        parents: Sequence[_Individual] = (),
    ) -> _Individual:
        strategy = genome.strategy()
        parent_strategy_ref = parents[0].strategy.ref if parents else None
        candidate = CandidateSpecV1(
            strategy_ref=strategy.ref,
            origin=source,
            parent_strategy_ref=parent_strategy_ref,
            origin_ref=search_ref,
        )
        self.objects.put(strategy)
        self.objects.put(candidate)
        return _Individual(
            strategy=strategy,
            candidate=candidate,
            objective=evaluate_construction_objective(strategy, config),
            island_index=island_index,
            generation_index=generation_index,
            node_index=node_index,
            source=source,
            parent_candidate_refs=tuple(parent.candidate.ref for parent in parents),
        )

    def _initial_population(
        self,
        search_ref: ContentAddress,
        config: BuilderSearchConfigV1,
        rng: random.Random,
        island_index: int,
        *,
        restart_index: int = 0,
    ) -> tuple[list[_Individual], int]:
        plan = plan_initial_population_decimation(
            population_size_per_island=config.population_size_per_island,
            supplied_initial_count=0,
            decimation_coefficient=config.decimation_coefficient,
        )
        generated: list[_Individual] = []
        source = "builder-initial" if restart_index == 0 else "builder-restart"
        for node_index in range(plan.minimum_filter_passing_generated_count):
            generated.append(
                self._make_individual(
                    search_ref,
                    config,
                    _random_genome(rng),
                    island_index=island_index,
                    generation_index=0,
                    node_index=node_index,
                    source=source,
                )
            )
        survivors = _sort_population(generated)[: config.population_size_per_island]
        return survivors, len(generated)

    def _evolve_island(
        self,
        search_ref: ContentAddress,
        config: BuilderSearchConfigV1,
        rng: random.Random,
        island_index: int,
        generation: int,
        population: Sequence[_Individual],
    ) -> tuple[list[_Individual], int]:
        selector = TournamentSelection(
            fitness=lambda item: float(item.objective),
            identity=lambda item: str(item.strategy.ref),
        )
        selected = list(selector.select(population, config.population_size_per_island, rng))
        rng.shuffle(selected)
        children: list[_Individual] = []
        evaluations = 0
        node_index = 0
        cross_probability = config.crossover_probability_pct / 100.0
        mutation_probability = config.mutation_probability_pct / 100.0

        for offset in range(0, len(selected), 2):
            left = selected[offset]
            right = selected[offset + 1] if offset + 1 < len(selected) else selected[offset]
            left_genome = BuilderGenomeV1.from_strategy(left.strategy)
            right_genome = BuilderGenomeV1.from_strategy(right.strategy)
            crossed = sqx_probability_gate(cross_probability, rng)
            if crossed:
                produced = _crossover_pair(left_genome, right_genome, rng)
                parent_pairs = ((left, right), (right, left))
                source = "builder-crossover"
            else:
                produced = (left_genome, right_genome)
                parent_pairs = ((left,), (right,))
                source = "builder-selection"

            for genome, parents in zip(produced, parent_pairs):
                if len(children) >= config.population_size_per_island:
                    break
                child_source = source
                if sqx_probability_gate(mutation_probability, rng):
                    genome = _mutate(genome, rng)
                    child_source = "builder-mutation"
                individual = self._make_individual(
                    search_ref,
                    config,
                    genome,
                    island_index=island_index,
                    generation_index=generation,
                    node_index=node_index,
                    source=child_source,
                    parents=parents,
                )
                node_index += 1
                evaluations += 1
                children.append(individual)

        population_after_fresh, fresh_evaluations = self._apply_fresh_blood(
            search_ref,
            config,
            rng,
            island_index,
            generation,
            children,
            node_index_start=node_index,
        )
        evaluations += fresh_evaluations
        return _sort_population(population_after_fresh)[: config.population_size_per_island], evaluations

    def _apply_fresh_blood(
        self,
        search_ref: ContentAddress,
        config: BuilderSearchConfigV1,
        rng: random.Random,
        island_index: int,
        generation: int,
        population: Sequence[_Individual],
        *,
        node_index_start: int,
    ) -> tuple[list[_Individual], int]:
        working = _sort_population(population)
        if config.fresh_blood_replace_similar:
            working = list(
                prune_similar_population(
                    working,
                    fitness=lambda item: float(item.objective),
                    fingerprint=_fingerprint,
                ).retained
            )

        weakest_to_remove = 0
        if config.fresh_blood_replace_weakest:
            plan = plan_weakest_replacement(
                population_size=config.population_size_per_island,
                current_population_size=len(working),
                current_generation=generation,
                replace_weakest_pct=config.fresh_blood_weakest_pct,
                replace_every_generations=config.fresh_blood_every_generations,
            )
            weakest_to_remove = plan.weakest_to_remove
        if weakest_to_remove:
            working = working[:-weakest_to_remove]

        missing = config.population_size_per_island - len(working)
        for index in range(missing):
            working.append(
                self._make_individual(
                    search_ref,
                    config,
                    _random_genome(rng),
                    island_index=island_index,
                    generation_index=0,
                    node_index=node_index_start + index,
                    source="builder-fresh-blood",
                )
            )
        return _sort_population(working), missing

    def _migrate(
        self,
        search_ref: ContentAddress,
        config: BuilderSearchConfigV1,
        generation: int,
        populations: list[list[_Individual]],
    ) -> list[list[_Individual]]:
        if config.island_count == 1 or config.migration_rate_pct == 0:
            return populations
        evolution = config.evolution_config
        inboxes: list[list[_Individual]] = [[] for _ in populations]
        capacity = migration_inbox_capacity(config.population_size_per_island)
        for island_index, population in enumerate(populations):
            plan = plan_migration_send(
                evolution,
                source_island_index=island_index,
                current_generation=generation,
                current_population_size=len(population),
            )
            if not plan.scheduled or plan.destination_island_index is None:
                continue
            destination = plan.destination_island_index
            incoming = [population[position] for position in plan.source_positions]
            combined = inboxes[destination] + incoming
            # SQX default-shuffle overflow identity is not reconstructed. The product
            # uses a stable best-first cap while preserving the proven 20% capacity.
            inboxes[destination] = _sort_population(combined)[:capacity]

        migrated: list[list[_Individual]] = []
        for island_index, population in enumerate(populations):
            inbox = inboxes[island_index]
            receive = plan_migration_receive(
                evolution,
                current_generation=generation,
                current_population_size=len(population),
                inbox_count=len(inbox),
            )
            if not receive.eligible or receive.immigrants_applied == 0:
                migrated.append(population)
                continue
            keep = len(population) - receive.immigrants_applied
            target = _sort_population(population)[:keep]
            for position, source in enumerate(inbox[: receive.immigrants_applied]):
                target.append(
                    self._make_individual(
                        search_ref,
                        config,
                        BuilderGenomeV1.from_strategy(source.strategy),
                        island_index=island_index,
                        generation_index=generation,
                        node_index=keep + position,
                        source="builder-migration",
                        parents=(source,),
                    )
                )
            migrated.append(_sort_population(target))
        return migrated

    def _record_population(
        self,
        state: dict[str, Any],
        populations: Sequence[Sequence[_Individual]],
        config: BuilderSearchConfigV1,
    ) -> None:
        flattened = [item for population in populations for item in population]
        unique: dict[ContentAddress, _Individual] = {}
        for item in flattened:
            existing = unique.get(item.candidate.ref)
            if existing is None or item.objective > existing.objective:
                unique[item.candidate.ref] = item
        fitness = tuple(
            CandidateFitnessV1(candidate_ref=item.candidate.ref, score=item.objective)
            for item in unique.values()
        )
        ordered = order_candidates(RankingObjectiveV1(BUILDER_OBJECTIVE), fitness) if fitness else ()
        by_ref = {item.candidate.ref: item for item in unique.values()}
        candidates: list[dict[str, Any]] = []
        rank = 0
        for fitness_item in ordered:
            if fitness_item.score < Decimal(config.minimum_objective_score):
                continue
            rank += 1
            item = by_ref[fitness_item.candidate_ref]
            candidates.append(
                {
                    "candidate_ref": str(item.candidate.ref),
                    "strategy_ref": str(item.strategy.ref),
                    "objective_values": {BUILDER_OBJECTIVE: item.objective},
                    "rank": rank,
                    "island_index": item.island_index,
                    "generation_index": item.generation_index,
                    "node_index": item.node_index,
                    "source": item.source,
                    "parent_candidate_refs": tuple(str(ref) for ref in item.parent_candidate_refs),
                    "parent_strategy_ref": (
                        str(item.candidate.parent_strategy_ref)
                        if item.candidate.parent_strategy_ref is not None
                        else None
                    ),
                }
            )
        state["candidates"] = candidates
        state["candidate_count"] = len(candidates)
        state["population_count"] = len(flattened)

    def _read_model(self, state: Mapping[str, Any]) -> dict[str, Any]:
        candidates: list[dict[str, Any]] = []
        for stored in state.get("candidates", ()):
            row = dict(stored)
            row["parent_candidate_refs"] = list(stored.get("parent_candidate_refs", ()))
            row["objective_values"] = {
                key: str(value) for key, value in stored.get("objective_values", {}).items()
            }
            candidates.append(row)
        return {
            "schema": BUILDER_SEARCH_READ_SCHEMA,
            "search_ref": state["search_ref"],
            "requested_strategy_ref": state["requested_strategy_ref"],
            "config_ref": state["config_ref"],
            "config": state["config"],
            "status": state["status"],
            "stage": state["stage"],
            "generation": state["generation"],
            "restart_count": state["restart_count"],
            "evaluations": state["evaluations"],
            "objective": state["objective"],
            "candidate_count": state.get("candidate_count", 0),
            "population_count": state.get("population_count", 0),
            "candidates": candidates,
        }
