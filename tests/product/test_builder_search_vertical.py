from decimal import Decimal
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.builder.api import (
    builder_candidates_response,
    builder_search_read_response,
    builder_search_start_response,
)
from tradercockpit.builder.search import (
    BUILDER_STRATEGY_SEMANTIC_SCHEMA,
    BuilderSearchConfigV1,
    evaluate_construction_objective,
)
from tradercockpit.domain import ContentAddress
from tradercockpit.domain.specs import CandidateSpecV1, StrategySpecV1
from tradercockpit.storage import FileObjectStore


class BuilderSearchVerticalTests(unittest.TestCase):
    def _request(self, **overrides):
        config = {
            "population_size_per_island": 4,
            "maximum_generations": 1,
            "crossover_probability_pct": 100,
            "mutation_probability_pct": 100,
            "island_count": 2,
            "migration_interval": 1,
            "migration_rate_pct": 25,
            "decimation_coefficient": 2,
            "fresh_blood_replace_similar": True,
            "fresh_blood_replace_weakest": True,
            "fresh_blood_weakest_pct": 25,
            "fresh_blood_every_generations": 1,
            "random_seed": 73,
        }
        config.update(overrides)
        return {"strategyRef": "opaque/strategy+context", "config": config}

    def test_real_product_contract_generates_persists_ranks_and_reopens_candidates(self):
        with TemporaryDirectory() as directory:
            root = Path(directory)
            status, created = builder_search_start_response(root, self._request())
            self.assertEqual(status, 201)
            self.assertEqual(created["schema"], "tc.builder-search.v1")
            self.assertEqual(created["status"], "complete")
            self.assertEqual(created["generation"], 1)
            self.assertEqual(created["population_count"], 8)
            self.assertGreaterEqual(created["evaluations"], 24)
            self.assertGreater(created["candidate_count"], 0)

            search_ref = ContentAddress.parse(created["search_ref"])
            self.assertEqual(search_ref.kind, "builder-search")
            self.assertEqual(ContentAddress.parse(created["config_ref"]).kind, "builder-config")

            store = FileObjectStore(root)
            scores = []
            sources = set()
            config = BuilderSearchConfigV1.from_request(self._request()["config"])
            for row in created["candidates"]:
                candidate_ref = ContentAddress.parse(row["candidate_ref"])
                self.assertEqual(candidate_ref.kind, "candidate")
                candidate = store.resolve(candidate_ref)
                self.assertIsInstance(candidate, CandidateSpecV1)
                self.assertEqual(candidate.ref, candidate_ref)
                strategy = store.resolve(candidate.strategy_ref)
                self.assertIsInstance(strategy, StrategySpecV1)
                self.assertEqual(strategy.semantic_schema, BUILDER_STRATEGY_SEMANTIC_SCHEMA)
                actual = evaluate_construction_objective(strategy, config)
                self.assertEqual(Decimal(row["objective_values"]["construction_fit"]), actual)
                scores.append(actual)
                sources.add(row["source"])

            self.assertEqual(scores, sorted(scores, reverse=True))
            self.assertTrue(sources & {"builder-crossover", "builder-mutation", "builder-fresh-blood", "builder-migration"})

            read_status, reopened = builder_search_read_response(root, created["search_ref"])
            self.assertEqual(read_status, 200)
            self.assertEqual(reopened, created)

            list_status, catalog = builder_candidates_response(root, "opaque/strategy+context")
            self.assertEqual(list_status, 200)
            self.assertEqual(catalog["schema"], "tc.builder-candidates.v1")
            self.assertEqual(
                {row["candidate_ref"] for row in catalog["candidates"]},
                {row["candidate_ref"] for row in created["candidates"]},
            )

    def test_same_config_and_seed_reproduce_candidate_identities_in_a_fresh_store(self):
        request = self._request()
        with TemporaryDirectory() as first, TemporaryDirectory() as second:
            _, left = builder_search_start_response(first, request)
            _, right = builder_search_start_response(second, request)
            self.assertEqual(left["search_ref"], right["search_ref"])
            self.assertEqual(left["config_ref"], right["config_ref"])
            self.assertEqual(
                [row["candidate_ref"] for row in left["candidates"]],
                [row["candidate_ref"] for row in right["candidates"]],
            )
            self.assertEqual(
                [row["objective_values"] for row in left["candidates"]],
                [row["objective_values"] for row in right["candidates"]],
            )

    def test_restart_on_finish_is_bounded_product_behavior_not_refused_sqx_behavior(self):
        with TemporaryDirectory() as directory:
            request = self._request(
                island_count=1,
                migration_rate_pct=0,
                decimation_coefficient=1,
                fresh_blood_replace_weakest=False,
                restart_on_finish=True,
                max_restarts=1,
            )
            status, result = builder_search_start_response(directory, request)
            self.assertEqual(status, 201)
            self.assertEqual(result["status"], "complete")
            self.assertEqual(result["restart_count"], 1)
            self.assertEqual(result["generation"], 1)

    def test_sqx_import_provenance_is_separate_from_product_defaults(self):
        native = {
            "population": 4,
            "max_generations": 2,
            "crossover_probability": 93,
            "mutation_probability": 30,
            "islands": 1,
            "migration_modulo": 1,
            "migration_rate": 0,
            "fresh_blood_replace_similar": True,
            "fresh_blood_replace_weakest": False,
            "filter_initial_population": False,
            "restart_on_finish": True,
            "restart_on_stagnation": False,
        }
        imported = BuilderSearchConfigV1.from_sqx_settings(
            native,
            native_source_ref="tc-sqx-project:sha256:abc",
            max_restarts=1,
        )
        direct = BuilderSearchConfigV1(
            population_size_per_island=4,
            maximum_generations=2,
            crossover_probability_pct=93,
            mutation_probability_pct=30,
            restart_on_finish=True,
            max_restarts=1,
        )
        self.assertEqual(imported.source, "sqx-import")
        self.assertEqual(imported.native_source_ref, "tc-sqx-project:sha256:abc")
        self.assertEqual(direct.source, "tradercockpit")
        self.assertIsNone(direct.native_source_ref)
        self.assertNotEqual(imported.ref, direct.ref)

    def test_api_rejects_unknown_fields_and_missing_state_root(self):
        status, payload = builder_search_start_response(None, self._request())
        self.assertEqual(status, 503)
        self.assertEqual(payload["error"], "producer_not_configured")

        with TemporaryDirectory() as directory:
            status, payload = builder_search_start_response(
                directory,
                {"strategyRef": "opaque", "config": {}, "fake": True},
            )
            self.assertEqual(status, 400)
            self.assertEqual(payload["error"], "invalid_request")


if __name__ == "__main__":
    unittest.main()
