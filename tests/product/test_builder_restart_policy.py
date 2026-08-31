from decimal import Decimal
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.builder.runtime import (
    BUILDER_SEARCH_IMPLEMENTATION,
    BuilderRuntimeSearchService,
)
from tradercockpit.builder.search import BuilderSearchConfigV1, _Individual


class _RestartProbeService(BuilderRuntimeSearchService):
    def __init__(self, state_root):
        super().__init__(state_root)
        self.initial_calls = 0

    @staticmethod
    def _population(score):
        return [
            _Individual(
                strategy=None,
                candidate=None,
                objective=Decimal(score),
                island_index=0,
                generation_index=0,
                node_index=index,
                source="probe",
            )
            for index in range(2)
        ]

    def _initial_population(self, search_ref, config, rng, island_index, *, restart_index=0):
        del search_ref, config, rng, island_index, restart_index
        self.initial_calls += 1
        score = 100 if self.initial_calls == 1 else 50
        return self._population(score), 2

    def _evolve_island(
        self,
        search_ref,
        config,
        rng,
        island_index,
        generation,
        population,
        *,
        restart_index=0,
    ):
        del search_ref, config, rng, island_index, generation, population, restart_index
        score = 100 if self.initial_calls == 1 else 60
        return self._population(score), 2

    def _migrate(self, search_ref, config, generation, populations, *, restart_index=0):
        del search_ref, config, generation, restart_index
        return populations

    def _record_population(self, state, populations, config):
        del config
        state["candidates"] = []
        state["candidate_count"] = 0
        state["population_count"] = sum(len(population) for population in populations)

    def read(self, search_ref):
        state = self.searches.read(search_ref)
        return self._read_model(state)


class BuilderRestartPolicyTests(unittest.TestCase):
    def test_stagnation_baseline_resets_to_restarted_population(self):
        self.assertEqual(BUILDER_SEARCH_IMPLEMENTATION, "tradercockpit.builder-search.v3")
        config = BuilderSearchConfigV1(
            population_size_per_island=2,
            maximum_generations=1,
            crossover_probability_pct=0,
            mutation_probability_pct=0,
            island_count=1,
            migration_interval=1,
            migration_rate_pct=0,
            fresh_blood_replace_similar=False,
            fresh_blood_replace_weakest=False,
            restart_on_stagnation=True,
            stagnation_generations=1,
            max_restarts=2,
            random_seed=1,
        )
        with TemporaryDirectory() as directory:
            result = _RestartProbeService(Path(directory)).run("opaque", config)

        self.assertEqual(result["restart_count"], 1)
        self.assertEqual(result["generation"], 1)
        self.assertEqual(result["status"], "complete")
        self.assertEqual(result["implementation"], BUILDER_SEARCH_IMPLEMENTATION)


if __name__ == "__main__":
    unittest.main()
