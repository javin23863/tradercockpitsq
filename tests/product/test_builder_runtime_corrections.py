from dataclasses import asdict
from decimal import Decimal
from pathlib import Path
import random
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.builder.api import (
    builder_candidates_response,
    builder_search_read_response,
    builder_search_start_response,
)
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
    canonical_json_bytes,
    canonical_json_loads,
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

    def _tamper_search_state(self, directory, result, mutate):
        search_ref = ContentAddress.parse(result["search_ref"])
        path = (
            Path(directory)
            / "builder-search"
            / "searches"
            / f"{search_ref.sha256}.json"
        )
        state = canonical_json_loads(path.read_bytes())
        mutate(state)
        path.write_bytes(canonical_json_bytes(state))

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
            self.assertEqual(BUILDER_SEARCH_IMPLEMENTATION, "tradercockpit.builder-search.v3")
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
            for item in refilled:
                if item.source == "builder-fresh-blood":
                    lineage = service.objects.resolve(item.candidate.origin_ref)
                    self.assertIsInstance(lineage, BuilderLineageSpecV1)
                    self.assertEqual(lineage.generation_index, 1)
                    self.assertEqual(lineage.restart_index, 0)

            control = random.Random()
            control.setstate(before_refill)
            for _ in range(3):
                _random_genome(control)
            self.assertEqual(rng.random(), control.random())

    def test_island_zero_strategy_stream_is_independent_of_added_islands(self):
        common = {
            "population_size_per_island": 4,
            "maximum_generations": 2,
            "crossover_probability_pct": 70,
            "mutation_probability_pct": 40,
            "migration_interval": 1,
            "migration_rate_pct": 0,
            "fresh_blood_replace_similar": False,
            "fresh_blood_replace_weakest": False,
            "random_seed": 53,
        }
        with TemporaryDirectory() as one_dir, TemporaryDirectory() as two_dir:
            one_status, one = builder_search_start_response(
                Path(one_dir),
                {"strategyRef": "opaque/islands", "config": {**common, "island_count": 1}},
            )
            two_status, two = builder_search_start_response(
                Path(two_dir),
                {"strategyRef": "opaque/islands", "config": {**common, "island_count": 2}},
            )
        self.assertEqual(one_status, 201)
        self.assertEqual(two_status, 201)
        one_island_zero = [
            (row["strategy_ref"], row["objective_values"]["construction_fit"])
            for row in one["candidates"]
            if row["island_index"] == 0
        ]
        two_island_zero = [
            (row["strategy_ref"], row["objective_values"]["construction_fit"])
            for row in two["candidates"]
            if row["island_index"] == 0
        ]
        self.assertEqual(two_island_zero, one_island_zero)

    def test_restart_epochs_have_distinct_immutable_lineage_identity(self):
        with TemporaryDirectory() as directory:
            config = self._config(
                population_size_per_island=2,
                restart_on_finish=True,
                max_restarts=2,
                random_seed=61,
            )
            status, result = builder_search_start_response(
                Path(directory),
                {"strategyRef": "opaque/restart-lineage", "config": asdict(config)},
            )
            self.assertEqual(status, 201)
            self.assertEqual(result["restart_count"], 2)
            self.assertTrue(result["candidates"])
            self.assertEqual({row["restart_index"] for row in result["candidates"]}, {2})
            self.assertEqual(
                {row["generation_index"] for row in result["candidates"]},
                {config.maximum_generations},
            )

            store = FileObjectStore(directory)
            lineage_root = Path(directory) / "objects" / "builder-lineage" / "v1"
            matching = []
            for path in lineage_root.glob("*.json"):
                lineage = store.resolve(ContentAddress("builder-lineage", 1, path.stem))
                if (
                    isinstance(lineage, BuilderLineageSpecV1)
                    and lineage.source == "builder-restart"
                    and lineage.island_index == 0
                    and lineage.generation_index == 0
                    and lineage.node_index == 0
                ):
                    matching.append(lineage)
            by_restart = {lineage.restart_index: lineage.ref for lineage in matching}
            self.assertIn(1, by_restart)
            self.assertIn(2, by_restart)
            self.assertNotEqual(by_restart[1], by_restart[2])

    def test_final_fresh_blood_lineage_uses_current_generation(self):
        with TemporaryDirectory() as directory:
            config = self._config(
                maximum_generations=2,
                fresh_blood_replace_weakest=True,
                fresh_blood_weakest_pct=50,
                fresh_blood_every_generations=1,
                random_seed=67,
            )
            status, result = builder_search_start_response(
                Path(directory),
                {"strategyRef": "opaque/fresh-generation", "config": asdict(config)},
            )
            self.assertEqual(status, 201)
            fresh = [row for row in result["candidates"] if row["source"] == "builder-fresh-blood"]
            self.assertTrue(fresh)
            self.assertEqual({row["generation_index"] for row in fresh}, {2})
            self.assertEqual({row["restart_index"] for row in fresh}, {0})

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
            self.assertEqual(lineage.restart_index, row["restart_index"])
            self.assertEqual(
                tuple(str(ref) for ref in lineage.parent_candidate_refs),
                tuple(row["parent_candidate_refs"]),
            )
            self.assertEqual(len(lineage.parent_strategy_refs), 2)

    def test_reopen_rejects_tampered_candidate_objective_as_invalid_state(self):
        with TemporaryDirectory() as directory:
            config = self._config(crossover_probability_pct=100)
            status, result = builder_search_start_response(
                Path(directory),
                {"strategyRef": "opaque", "config": asdict(config)},
            )
            self.assertEqual(status, 201)
            self.assertTrue(result["candidates"])

            def mutate(state):
                state["candidates"][0]["objective_values"]["construction_fit"] += Decimal(1)

            self._tamper_search_state(directory, result, mutate)
            read_status, read_payload = builder_search_read_response(
                Path(directory), result["search_ref"]
            )
            self.assertEqual(read_status, 409)
            self.assertEqual(read_payload["error"], "invalid_state")
            self.assertIn("objective disagrees", read_payload["detail"])

            list_status, list_payload = builder_candidates_response(
                Path(directory), "opaque"
            )
            self.assertEqual(list_status, 409)
            self.assertEqual(list_payload["error"], "invalid_state")

    def test_reopen_rejects_config_payload_that_no_longer_matches_config_ref(self):
        with TemporaryDirectory() as directory:
            config = self._config()
            status, result = builder_search_start_response(
                Path(directory),
                {"strategyRef": "opaque", "config": asdict(config)},
            )
            self.assertEqual(status, 201)

            self._tamper_search_state(
                directory,
                result,
                lambda state: state["config"].__setitem__(
                    "random_seed", state["config"]["random_seed"] + 1
                ),
            )
            read_status, payload = builder_search_read_response(
                Path(directory), result["search_ref"]
            )
            self.assertEqual(read_status, 409)
            self.assertEqual(payload["error"], "invalid_state")
            self.assertIn("config payload does not match config_ref", payload["detail"])

    def test_reopen_rejects_progress_counters_outside_configured_bounds(self):
        with TemporaryDirectory() as directory:
            config = self._config(maximum_generations=1, max_restarts=0)
            status, result = builder_search_start_response(
                Path(directory),
                {"strategyRef": "opaque/counters", "config": asdict(config)},
            )
            self.assertEqual(status, 201)

            self._tamper_search_state(
                directory,
                result,
                lambda state: state.__setitem__("generation", config.maximum_generations + 1),
            )
            read_status, payload = builder_search_read_response(Path(directory), result["search_ref"])
            self.assertEqual(read_status, 409)
            self.assertIn("generation exceeds", payload["detail"])

        with TemporaryDirectory() as directory:
            config = self._config(max_restarts=0)
            status, result = builder_search_start_response(
                Path(directory),
                {"strategyRef": "opaque/restart-counter", "config": asdict(config)},
            )
            self.assertEqual(status, 201)
            self._tamper_search_state(
                directory,
                result,
                lambda state: state.__setitem__("restart_count", 1),
            )
            read_status, payload = builder_search_read_response(Path(directory), result["search_ref"])
            self.assertEqual(read_status, 409)
            self.assertIn("restart_count exceeds", payload["detail"])


if __name__ == "__main__":
    unittest.main()
