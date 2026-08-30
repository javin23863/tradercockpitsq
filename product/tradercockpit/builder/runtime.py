"""Corrected product runtime for Builder/evolution search execution.

This layer keeps the stable search contract while closing product-correctness gaps
found during the Issue #24 recovery review. It intentionally does not register
HTTP routes or touch the Retester/server vertical owned by PR #23.
"""

from __future__ import annotations

import random
from typing import Any, Mapping, Sequence

from tradercockpit.domain import ContentAddress, content_address

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
    BuilderSearchConfigV1,
    BuilderSearchError,
    BuilderSearchService as _BaseBuilderSearchService,
    _Individual,
    _random_genome,
    _sort_population,
)


BUILDER_SEARCH_IMPLEMENTATION = "tradercockpit.builder-search.v1"
_RUNTIME_COMPUTED_THREADS = 1


def java_signed_strategy_fingerprint(strategy_ref: ContentAddress) -> int:
    """Map the first 32 digest bits to the signed Java-int domain without aliasing."""

    if not isinstance(strategy_ref, ContentAddress) or strategy_ref.kind != "strategy":
        raise BuilderSearchError("strategy_ref must reference strategy")
    raw = int(strategy_ref.sha256[:8], 16)
    return raw if raw <= 0x7FFFFFFF else raw - 0x100000000


class BuilderRuntimeSearchService(_BaseBuilderSearchService):
    """Product search service with versioned identity and corrected RNG semantics."""

    def run(
        self,
        requested_strategy_ref: str,
        config: BuilderSearchConfigV1,
    ) -> dict[str, Any]:
        if not isinstance(requested_strategy_ref, str) or not requested_strategy_ref.strip():
            raise BuilderSearchError("requested_strategy_ref must be a non-empty string")
        if requested_strategy_ref != requested_strategy_ref.strip():
            raise BuilderSearchError("requested_strategy_ref must not have surrounding whitespace")
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
