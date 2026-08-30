"""Corrected product runtime for Builder/evolution search execution.

This layer keeps the stable search contract while closing product-correctness gaps
found during the Issue #24 recovery review. It intentionally does not register
HTTP routes or touch the Retester/server vertical owned by PR #23.
"""

from __future__ import annotations

from decimal import Decimal
import random
from typing import Any, Mapping, Sequence

from tradercockpit.domain import (
    BuilderLineageSpecV1,
    CandidateSpecV1,
    ContentAddress,
    StrategySpecV1,
    content_address,
)
from tradercockpit.domain.canonical import canonical_json_loads
from tradercockpit.storage import ContentStoreError

from .decimation import (
    generated_candidates_removed_after_sort,
    initial_generation_batch_size,
    plan_initial_population_decimation,
)
from .fresh_blood import (
    additional_generation_batch_size,
    plan_weakest_replacement,
    prune_similar_population,
)
from .search import (
    BUILDER_OBJECTIVE,
    BUILDER_OBJECTIVE_ROLE,
    BUILDER_SEARCH_STATE_SCHEMA,
    BuilderGenomeV1,
    BuilderSearchConfigV1,
    BuilderSearchError,
    BuilderSearchService as _BaseBuilderSearchService,
    _Individual,
    _random_genome,
    _sort_population,
    evaluate_construction_objective,
)


BUILDER_SEARCH_IMPLEMENTATION = "tradercockpit.builder-search.v2"
_RUNTIME_COMPUTED_THREADS = 1
_CONFIG_IDENTITY_KEYS = frozenset(
    {
        "population_size_per_island",
        "maximum_generations",
        "crossover_probability_pct",
        "mutation_probability_pct",
        "island_count",
        "migration_interval",
        "migration_rate_pct",
        "decimation_coefficient",
        "fresh_blood_replace_similar",
        "fresh_blood_replace_weakest",
        "fresh_blood_weakest_pct",
        "fresh_blood_every_generations",
        "restart_on_finish",
        "restart_on_stagnation",
        "stagnation_generations",
        "max_restarts",
        "minimum_objective_score",
        "random_seed",
        "target",
        "source",
        "native_source_ref",
    }
)
_TARGET_IDENTITY_KEYS = frozenset(
    {
        "direction",
        "entry_kind",
        "entry_period",
        "entry_threshold",
        "exit_bars",
        "position_bps",
    }
)


def java_signed_strategy_fingerprint(strategy_ref: ContentAddress) -> int:
    """Map the first 32 digest bits to the signed Java-int domain without aliasing."""

    if not isinstance(strategy_ref, ContentAddress) or strategy_ref.kind != "strategy":
        raise BuilderSearchError("strategy_ref must reference strategy")
    raw = int(strategy_ref.sha256[:8], 16)
    return raw if raw <= 0x7FFFFFFF else raw - 0x100000000


def _state_error(detail: str) -> ContentStoreError:
    return ContentStoreError(f"corrupt Builder search state: {detail}")


def _state_ref(value: object, kind: str, name: str) -> ContentAddress:
    if not isinstance(value, str):
        raise _state_error(f"{name} must be a content-address string")
    try:
        ref = ContentAddress.parse(value)
    except ValueError as exc:
        raise _state_error(f"{name} is not a valid content address") from exc
    if ref.kind != kind:
        raise _state_error(f"{name} must reference {kind}")
    return ref


def _state_int(value: object, name: str) -> int:
    if not isinstance(value, int) or isinstance(value, bool) or value < 0:
        raise _state_error(f"{name} must be a non-negative integer")
    return value


def _config_from_identity_payload(value: object) -> BuilderSearchConfigV1:
    if not isinstance(value, Mapping):
        raise _state_error("config must be an object")
    if set(value) != _CONFIG_IDENTITY_KEYS:
        raise _state_error("config identity fields do not match BuilderSearchConfigV1")
    target = value.get("target")
    if not isinstance(target, Mapping) or set(target) != _TARGET_IDENTITY_KEYS:
        raise _state_error("config target identity fields are invalid")

    kwargs = {key: item for key, item in value.items() if key != "target"}
    kwargs.update(
        {
            "target_direction": target["direction"],
            "target_entry_kind": target["entry_kind"],
            "target_entry_period": target["entry_period"],
            "target_entry_threshold": target["entry_threshold"],
            "target_exit_bars": target["exit_bars"],
            "target_position_bps": target["position_bps"],
        }
    )
    try:
        return BuilderSearchConfigV1(**kwargs)
    except (BuilderSearchError, TypeError, ValueError) as exc:
        raise _state_error(f"config identity payload is invalid: {exc}") from exc


class BuilderRuntimeSearchService(_BaseBuilderSearchService):
    """Product search service with versioned identity and corrected RNG semantics."""

    def run(
        self,
        requested_strategy_ref: str,
        config: BuilderSearchConfigV1,
    ) -> dict[str, Any]:
        if not isinstance(requested_strategy_ref, str) or not requested_strategy_ref.strip():
            raise BuilderSearchError("requested_strategy_ref must be a non-empty string")
        if not isinstance(config, BuilderSearchConfigV1):
            raise BuilderSearchError("config must be BuilderSearchConfigV1")

        search_ref = content_address(
            "builder-search",
            1,
            {
                "implementation": BUILDER_SEARCH_IMPLEMENTATION,
                "requested_strategy_ref": requested_strategy_ref,
                "config_ref": str(config.ref),
            },
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
                # A restart is a fresh stagnation epoch. Compare subsequent progress
                # to the restarted population, not to an unreachable pre-restart best.
                global_best = max(
                    item.objective for population in populations for item in population
                )
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
        try:
            ref = ContentAddress.parse(search_ref) if isinstance(search_ref, str) else search_ref
        except ValueError as exc:
            raise BuilderSearchError("search_ref must be a valid content address") from exc
        if not isinstance(ref, ContentAddress) or ref.kind != "builder-search":
            raise BuilderSearchError("search_ref must reference builder-search")
        try:
            state = self.searches.read(ref)
        except KeyError:
            raise
        except (BuilderSearchError, ValueError, TypeError, OSError) as exc:
            raise _state_error(str(exc)) from exc
        self._validate_state(ref, state)
        return self._read_model(state)

    def list_for_strategy(self, requested_strategy_ref: str) -> tuple[dict[str, Any], ...]:
        if not isinstance(requested_strategy_ref, str) or not requested_strategy_ref.strip():
            raise BuilderSearchError("requested_strategy_ref must be a non-empty string")
        try:
            states: list[dict[str, Any]] = []
            for path in sorted(self.searches.searches_root.glob("*.json")):
                value = canonical_json_loads(path.read_bytes())
                if isinstance(value, dict) and value.get("requested_strategy_ref") == requested_strategy_ref:
                    states.append(value)
        except (BuilderSearchError, ValueError, TypeError, OSError) as exc:
            raise _state_error(str(exc)) from exc

        models: list[dict[str, Any]] = []
        for state in states:
            ref = _state_ref(state.get("search_ref"), "builder-search", "search_ref")
            self._validate_state(ref, state)
            models.append(self._read_model(state))
        return tuple(models)

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
        lineage = BuilderLineageSpecV1(
            search_ref=search_ref,
            source=source,
            island_index=island_index,
            generation_index=generation_index,
            node_index=node_index,
            parent_candidate_refs=tuple(parent.candidate.ref for parent in parents),
            parent_strategy_refs=tuple(parent.strategy.ref for parent in parents),
        )
        self.objects.put(strategy)
        self.objects.put(lineage)
        candidate = CandidateSpecV1(
            strategy_ref=strategy.ref,
            origin=source,
            parent_strategy_ref=parents[0].strategy.ref if parents else None,
            origin_ref=lineage.ref,
        )
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

    def _record_population(
        self,
        state: dict[str, Any],
        populations: Sequence[Sequence[_Individual]],
        config: BuilderSearchConfigV1,
    ) -> None:
        super()._record_population(state, populations, config)
        for row in state.get("candidates", []):
            candidate = self.objects.resolve(ContentAddress.parse(row["candidate_ref"]))
            if not isinstance(candidate, CandidateSpecV1):
                raise BuilderSearchError("candidate custody resolved to the wrong object type")
            lineage_ref = candidate.origin_ref
            if lineage_ref is None or lineage_ref.kind != "builder-lineage":
                raise BuilderSearchError("generated candidate is missing immutable lineage custody")
            lineage = self.objects.resolve(lineage_ref)
            if not isinstance(lineage, BuilderLineageSpecV1):
                raise BuilderSearchError("lineage custody resolved to the wrong object type")
            if lineage.search_ref != ContentAddress.parse(state["search_ref"]):
                raise BuilderSearchError("candidate lineage belongs to another Builder search")
            if tuple(str(ref) for ref in lineage.parent_candidate_refs) != tuple(
                row["parent_candidate_refs"]
            ):
                raise BuilderSearchError("candidate lineage parents disagree with search catalog")
            row["lineage_ref"] = str(lineage_ref)

    def _base_state(
        self,
        search_ref: ContentAddress,
        requested_strategy_ref: str,
        config: BuilderSearchConfigV1,
    ) -> dict[str, Any]:
        state = super()._base_state(search_ref, requested_strategy_ref, config)
        state["implementation"] = BUILDER_SEARCH_IMPLEMENTATION
        return state

    def _read_model(self, state: Mapping[str, Any]) -> dict[str, Any]:
        model = super()._read_model(state)
        model["implementation"] = state.get("implementation", "legacy-unversioned")
        return model

    def _validate_state(
        self,
        search_ref: ContentAddress,
        state: Mapping[str, Any],
    ) -> None:
        if state.get("schema") != BUILDER_SEARCH_STATE_SCHEMA:
            raise _state_error("unsupported search-state schema")
        if state.get("implementation") != BUILDER_SEARCH_IMPLEMENTATION:
            raise _state_error("search-state implementation revision is missing or unsupported")
        if state.get("search_ref") != str(search_ref):
            raise _state_error("search_ref does not match requested durable record")

        requested_strategy_ref = state.get("requested_strategy_ref")
        if not isinstance(requested_strategy_ref, str) or not requested_strategy_ref.strip():
            raise _state_error("requested_strategy_ref is invalid")

        config_ref = _state_ref(state.get("config_ref"), "builder-config", "config_ref")
        config = _config_from_identity_payload(state.get("config"))
        if config.ref != config_ref:
            raise _state_error("config payload does not match config_ref")
        expected_search_ref = content_address(
            "builder-search",
            1,
            {
                "implementation": BUILDER_SEARCH_IMPLEMENTATION,
                "requested_strategy_ref": requested_strategy_ref,
                "config_ref": str(config_ref),
            },
        )
        if expected_search_ref != search_ref:
            raise _state_error("search identity does not match implementation/request/config custody")

        objective = state.get("objective")
        if not isinstance(objective, Mapping):
            raise _state_error("objective metadata must be an object")
        if objective.get("name") != BUILDER_OBJECTIVE:
            raise _state_error("objective name does not match Builder runtime")
        if objective.get("direction") != "maximize":
            raise _state_error("objective direction does not match Builder runtime")
        if objective.get("evidence_role") != BUILDER_OBJECTIVE_ROLE:
            raise _state_error("objective evidence role does not match Builder runtime")

        _state_int(state.get("generation"), "generation")
        _state_int(state.get("restart_count"), "restart_count")
        _state_int(state.get("evaluations"), "evaluations")
        status = state.get("status")
        stage = state.get("stage")
        if status not in {"created", "running", "complete"}:
            raise _state_error("status is invalid")
        if not isinstance(stage, str) or not stage:
            raise _state_error("stage is invalid")

        raw_rows = state.get("candidates", [])
        if not isinstance(raw_rows, (list, tuple)):
            raise _state_error("candidates must be an ordered array")

        seen: set[ContentAddress] = set()
        validated: list[tuple[ContentAddress, Decimal, int]] = []
        for position, raw_row in enumerate(raw_rows):
            candidate_ref, score, rank = self._validate_candidate_row(
                search_ref,
                config,
                raw_row,
                position,
            )
            if candidate_ref in seen:
                raise _state_error("candidate catalog contains duplicate candidate_ref values")
            seen.add(candidate_ref)
            validated.append((candidate_ref, score, rank))

        candidate_count = state.get("candidate_count", 0)
        if (
            not isinstance(candidate_count, int)
            or isinstance(candidate_count, bool)
            or candidate_count != len(validated)
        ):
            raise _state_error("candidate_count does not match candidate catalog")

        population_count = state.get("population_count", 0)
        if not isinstance(population_count, int) or isinstance(population_count, bool) or population_count < 0:
            raise _state_error("population_count must be a non-negative integer")
        if stage != "created":
            expected_population = config.population_size_per_island * config.island_count
            if population_count != expected_population:
                raise _state_error("population_count does not match configured island capacity")
        if candidate_count > population_count:
            raise _state_error("candidate_count exceeds current population_count")

        expected_order = sorted(validated, key=lambda item: (-item[1], str(item[0])))
        if [item[0] for item in validated] != [item[0] for item in expected_order]:
            raise _state_error("candidate catalog is not ordered by canonical objective/rank policy")
        if [item[2] for item in validated] != list(range(1, len(validated) + 1)):
            raise _state_error("candidate ranks are not contiguous in canonical order")

    def _validate_candidate_row(
        self,
        search_ref: ContentAddress,
        config: BuilderSearchConfigV1,
        raw_row: object,
        position: int,
    ) -> tuple[ContentAddress, Decimal, int]:
        if not isinstance(raw_row, Mapping):
            raise _state_error(f"candidates[{position}] must be an object")
        candidate_ref = _state_ref(
            raw_row.get("candidate_ref"),
            "candidate",
            f"candidates[{position}].candidate_ref",
        )
        try:
            candidate = self.objects.resolve(candidate_ref)
        except KeyError as exc:
            raise _state_error(f"candidate object is missing: {candidate_ref}") from exc
        if not isinstance(candidate, CandidateSpecV1):
            raise _state_error("candidate_ref resolved to the wrong object type")

        strategy_ref = _state_ref(
            raw_row.get("strategy_ref"),
            "strategy",
            f"candidates[{position}].strategy_ref",
        )
        if strategy_ref != candidate.strategy_ref:
            raise _state_error("candidate strategy_ref disagrees with immutable candidate custody")
        try:
            strategy = self.objects.resolve(strategy_ref)
        except KeyError as exc:
            raise _state_error(f"strategy object is missing: {strategy_ref}") from exc
        if not isinstance(strategy, StrategySpecV1):
            raise _state_error("strategy_ref resolved to the wrong object type")

        source = raw_row.get("source")
        if not isinstance(source, str) or source != candidate.origin:
            raise _state_error("candidate source disagrees with immutable candidate custody")

        lineage_ref = _state_ref(
            raw_row.get("lineage_ref"),
            "builder-lineage",
            f"candidates[{position}].lineage_ref",
        )
        if candidate.origin_ref != lineage_ref:
            raise _state_error("candidate origin_ref disagrees with catalog lineage_ref")
        try:
            lineage = self.objects.resolve(lineage_ref)
        except KeyError as exc:
            raise _state_error(f"lineage object is missing: {lineage_ref}") from exc
        if not isinstance(lineage, BuilderLineageSpecV1):
            raise _state_error("lineage_ref resolved to the wrong object type")
        if lineage.search_ref != search_ref or lineage.source != source:
            raise _state_error("lineage search/source custody disagrees with candidate catalog")

        island_index = _state_int(raw_row.get("island_index"), f"candidates[{position}].island_index")
        generation_index = _state_int(
            raw_row.get("generation_index"),
            f"candidates[{position}].generation_index",
        )
        node_index = _state_int(raw_row.get("node_index"), f"candidates[{position}].node_index")
        if (
            lineage.island_index != island_index
            or lineage.generation_index != generation_index
            or lineage.node_index != node_index
        ):
            raise _state_error("lineage coordinates disagree with candidate catalog")

        raw_parents = raw_row.get("parent_candidate_refs", ())
        if not isinstance(raw_parents, (list, tuple)):
            raise _state_error("parent_candidate_refs must be an ordered array")
        parent_candidate_refs = tuple(
            _state_ref(value, "candidate", f"candidates[{position}].parent_candidate_refs[{index}]")
            for index, value in enumerate(raw_parents)
        )
        if parent_candidate_refs != lineage.parent_candidate_refs:
            raise _state_error("candidate parent refs disagree with immutable lineage custody")

        expected_parent_strategy_ref = (
            lineage.parent_strategy_refs[0] if lineage.parent_strategy_refs else None
        )
        if candidate.parent_strategy_ref != expected_parent_strategy_ref:
            raise _state_error("candidate primary parent strategy disagrees with immutable lineage")
        raw_parent_strategy = raw_row.get("parent_strategy_ref")
        if expected_parent_strategy_ref is None:
            if raw_parent_strategy is not None:
                raise _state_error("catalog primary parent strategy is unexpected")
        else:
            catalog_parent_strategy = _state_ref(
                raw_parent_strategy,
                "strategy",
                f"candidates[{position}].parent_strategy_ref",
            )
            if catalog_parent_strategy != expected_parent_strategy_ref:
                raise _state_error("catalog primary parent strategy disagrees with immutable lineage")

        for index, (parent_candidate_ref, parent_strategy_ref) in enumerate(
            zip(lineage.parent_candidate_refs, lineage.parent_strategy_refs)
        ):
            try:
                parent_candidate = self.objects.resolve(parent_candidate_ref)
            except KeyError as exc:
                raise _state_error(
                    f"parent candidate object is missing at parent index {index}: {parent_candidate_ref}"
                ) from exc
            if not isinstance(parent_candidate, CandidateSpecV1):
                raise _state_error("parent candidate ref resolved to the wrong object type")
            if parent_candidate.strategy_ref != parent_strategy_ref:
                raise _state_error("lineage parent candidate/strategy pair is inconsistent")

        objective_values = raw_row.get("objective_values")
        if not isinstance(objective_values, Mapping) or set(objective_values) != {BUILDER_OBJECTIVE}:
            raise _state_error("candidate objective values do not match Builder objective schema")
        stored_score = objective_values[BUILDER_OBJECTIVE]
        if isinstance(stored_score, bool) or not isinstance(stored_score, (int, Decimal)):
            raise _state_error("candidate construction objective must be exact numeric custody")
        stored_score = Decimal(stored_score)
        actual_score = evaluate_construction_objective(strategy, config)
        if stored_score != actual_score:
            raise _state_error("candidate construction objective disagrees with canonical strategy/config")
        if actual_score < Decimal(config.minimum_objective_score):
            raise _state_error("candidate catalog contains a candidate below configured filter threshold")

        rank = _state_int(raw_row.get("rank"), f"candidates[{position}].rank")
        if rank == 0:
            raise _state_error("candidate rank must be one-based")
        return candidate_ref, actual_score, rank

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

        while True:
            batch_size = initial_generation_batch_size(
                plan,
                accepted_generated_count=len(generated),
                computed_threads=_RUNTIME_COMPUTED_THREADS,
            )
            if batch_size == 0:
                break
            for _ in range(batch_size):
                generated.append(
                    self._make_individual(
                        search_ref,
                        config,
                        _random_genome(rng),
                        island_index=island_index,
                        generation_index=0,
                        node_index=len(generated),
                        source=source,
                    )
                )

        for _ in range(plan.normal_completion_discarded_candidate_factory_calls):
            _random_genome(rng)

        removal_count = generated_candidates_removed_after_sort(
            plan,
            accepted_generated_count=len(generated),
        )
        ordered = _sort_population(generated)
        if removal_count:
            ordered = ordered[:-removal_count]
        return ordered, len(generated)

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
        del node_index_start  # native refill indices start at post-removal population size
        working = _sort_population(population)
        if config.fresh_blood_replace_similar:
            working = list(
                prune_similar_population(
                    working,
                    fitness=lambda item: float(item.objective),
                    fingerprint=lambda item: java_signed_strategy_fingerprint(item.strategy.ref),
                ).retained
            )

        if config.fresh_blood_replace_weakest:
            plan = plan_weakest_replacement(
                population_size=config.population_size_per_island,
                current_population_size=len(working),
                current_generation=generation,
                replace_weakest_pct=config.fresh_blood_weakest_pct,
                replace_every_generations=config.fresh_blood_every_generations,
            )
            if plan.weakest_to_remove:
                working = working[:-plan.weakest_to_remove]

        generated_count = 0
        while len(working) < config.population_size_per_island:
            batch_size = additional_generation_batch_size(
                population_size=config.population_size_per_island,
                current_population_size=len(working),
                computed_threads=_RUNTIME_COMPUTED_THREADS,
            )
            if batch_size <= 0:
                raise BuilderSearchError("fresh-blood refill stalled before population target")
            for _ in range(batch_size):
                node_index = len(working)
                working.append(
                    self._make_individual(
                        search_ref,
                        config,
                        _random_genome(rng),
                        island_index=island_index,
                        generation_index=0,
                        node_index=node_index,
                        source="builder-fresh-blood",
                    )
                )
                generated_count += 1

        if generated_count:
            _random_genome(rng)

        return _sort_population(working), generated_count
