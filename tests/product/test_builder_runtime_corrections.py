from dataclasses import asdict
from pathlib import Path
import random
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.builder.api import builder_search_start_response
from tradercockpit.builder.runtime import (
    BUILDER_SEARCH_IMPLEMENTATION,
    BuilderRuntimeSearchService,
    java_signed_strategy_fingerprint,
)
from tradercockpit.builder.search import (
    BuilderSearchConfigV1,
    _random_genome,
)
from tradercockpit.domain import (
    BuilderLineageSpecV1,
    CandidateSpecV1,
    ContentAddress,
    content_address,
)
from tradercockpit.storage import FileObjectStore


class BuilderRuntimeCorrectionTests(unittest.TestCase):
    def _config(self, **overrides):
        values = {
            "population_size_per_island": 4,
            "maximum_generations": 1,
            "crossover_probability_pct": 0,
            "mutation_probability_pct": 0,
            "island_count": 1,
            "migration_interval": 1,
            "migration_rate_pct": 0,
            "decimation_coefficient": 1,
            "fresh_blood_replace_similar": False,
            "fresh_blood_replace_weakest": False,
            "random_seed": 17,
        }
        values.update(overrides)
        return BuilderSearchConfigV1(**values)

    def test_api_search_identity_is_bound_to_runtime_implementation(self):
        with TemporaryDirectory() as directory:
            config = self._config(population_size_per_island=2)
            legacy_ref = content_address(
                "builder-search",
                1,
                {
                    "requested_strategy_ref": "opaque",
                    "config_ref": str(config.ref),
                },
            )
            status, result = builder_search_start_response(
                Path(directory),
                {"strategyRef": "opaque", "config": asdict(config)},
            )
            self.assertEqual(status, 201)
            self.assertEqual(result["implementation"], BUILDER_SEARCH_IMPLEMENTATION)
            self.assertNotEqual(result["search_ref"], str(legacy_ref))

    def test_similarity_fingerprint_uses_signed_java_int_without_high_bit_alias(self):
        low = ContentAddress("strategy", 1, "00000000" + "0" * 56)
        high = ContentAddress("strategy", 1, "80000000" + "0" * 56)
        self.assertEqual(java_signed_strategy_fingerprint(low), 0)
        self.assertEqual(java_signed_strategy_fingerprint(high), -(2**31))
        self.assertNotEqual(
            java_signed_strategy_fingerprint(low),
            java_signed_strategy_fingerprint(high),
        )

    def test_initial_population_honors_batch_overshoot_and_discarded_factory_call(self):
        with TemporaryDirectory() as directory:
            service = BuilderRuntimeSearchService(directory)
            config = self._config(
                population_size_per_island=3,
                decimation_coefficient=3,
            )
            search_ref = content_address("builder-search", 1, {"test": "initial"})
            rng = random.Random(11)
            population, evaluations = service._initial_population(
                search_ref,
                config,
                rng,
                0,
            )
            self.assertEqual(evaluations, 10)
            self.assertEqual(len(population), 3)

            control = random.Random(11)
            for _ in range(11):
                _random_genome(control)
            self.assertEqual(rng.random(), control.random())

    def test_fresh_blood_refill_uses_post_removal_indices_and_discarded_call(self):
        with TemporaryDirectory() as directory:
            service = BuilderRuntimeSearchService(directory)
            config = self._config(
                fresh_blood_replace_weakest=True,
                fresh_blood_weakest_pct=50,
                fresh_blood_every_generations=1,
            )
            search_ref = content_address("builder-search", 1, {"test": "fresh"})
            rng = random.Random(19)
            population, _ = service._initial_population(search_ref, config, rng, 0)

            before_refill = rng.getstate()
            refilled, generated = service._apply_fresh_blood(
                search_ref,
                config,
                rng,
                0,
                1,
                population,
                node_index_start=999,
            )
            self.assertEqual(generated, 2)
            self.assertEqual(len(refilled), 4)
            self.assertEqual(
                sorted(
                    item.node_index
                    for item in refilled
                    if item.source == "builder-fresh-blood"
                ),
                [2, 3],
            )

            control = random.Random()
            control.setstate(before_refill)
            for _ in range(3):
                _random_genome(control)
            self.assertEqual(rng.random(), control.random())

    def test_crossover_candidate_binds_full_parent_set_through_lineage_custody(self):
        with TemporaryDirectory() as directory:
            config = self._config(
                crossover_probability_pct=100,
                mutation_probability_pct=0,
                population_size_per_island=4,
            )
            status, result = builder_search_start_response(
                Path(directory),
                {"strategyRef": "opaque", "config": asdict(config)},
            )
            self.assertEqual(status, 201)
            rows = [
                row
                for row in result["candidates"]
                if row["source"] == "builder-crossover"
                and len(row["parent_candidate_refs"]) == 2
            ]
            self.assertTrue(rows)
            row = rows[0]

            store = FileObjectStore(directory)
            candidate = store.resolve(ContentAddress.parse(row["candidate_ref"]))
            self.assertIsInstance(candidate, CandidateSpecV1)
            self.assertIsNotNone(candidate.origin_ref)
            self.assertEqual(candidate.origin_ref.kind, "builder-lineage")
            self.assertEqual(str(candidate.origin_ref), row["lineage_ref"])

            lineage = store.resolve(candidate.origin_ref)
            self.assertIsInstance(lineage, BuilderLineageSpecV1)
            self.assertEqual(str(lineage.search_ref), result["search_ref"])
            self.assertEqual(
                tuple(str(ref) for ref in lineage.parent_candidate_refs),
                tuple(row["parent_candidate_refs"]),
            )
            self.assertEqual(len(lineage.parent_strategy_refs), 2)


if __name__ == "__main__":
    unittest.main()
