from dataclasses import asdict
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.builder.http import (
    builder_http_get_response,
    builder_http_post_response,
)
from tradercockpit.builder.search import BuilderSearchConfigV1


class BuilderHttpAdapterTests(unittest.TestCase):
    def _config(self):
        return BuilderSearchConfigV1(
            population_size_per_island=2,
            maximum_generations=1,
            crossover_probability_pct=0,
            mutation_probability_pct=0,
            island_count=1,
            migration_interval=1,
            migration_rate_pct=0,
            fresh_blood_replace_similar=False,
            fresh_blood_replace_weakest=False,
            random_seed=29,
        )

    def test_post_then_get_candidate_catalog_uses_same_durable_state(self):
        with TemporaryDirectory() as directory:
            state_root = Path(directory)
            strategy_ref = "opaque/requested strategy + Khmer ខ្មែរ"
            started = builder_http_post_response(
                state_root,
                "/api/builder-searches",
                {"strategyRef": strategy_ref, "config": asdict(self._config())},
            )
            self.assertIsNotNone(started)
            status, search = started
            self.assertEqual(status, 201)
            self.assertEqual(search["requested_strategy_ref"], strategy_ref)
            self.assertTrue(search["candidates"])

            listed = builder_http_get_response(
                state_root,
                "/api/builder-candidates",
                {"strategyRef": [strategy_ref]},
            )
            self.assertIsNotNone(listed)
            status, catalog = listed
            self.assertEqual(status, 200)
            self.assertEqual(catalog["requested_strategy_ref"], strategy_ref)
            self.assertEqual(
                [row["candidate_ref"] for row in catalog["candidates"]],
                [row["candidate_ref"] for row in search["candidates"]],
            )

            reopened = builder_http_get_response(
                state_root,
                "/api/builder-searches/read",
                {"searchRef": [search["search_ref"]]},
            )
            self.assertIsNotNone(reopened)
            status, readback = reopened
            self.assertEqual(status, 200)
            self.assertEqual(readback["search_ref"], search["search_ref"])
            self.assertEqual(readback["candidates"], search["candidates"])

    def test_get_contract_rejects_duplicate_missing_and_unknown_query_fields(self):
        with TemporaryDirectory() as directory:
            state_root = Path(directory)
            cases = [
                ({}, "strategyRef"),
                ({"strategyRef": ["a", "b"]}, "strategyRef"),
                ({"strategyRef": [""]}, "strategyRef"),
                ({"strategyRef": ["a"], "extra": ["b"]}, "unknown query fields"),
            ]
            for query, expected in cases:
                response = builder_http_get_response(
                    state_root,
                    "/api/builder-candidates",
                    query,
                )
                self.assertIsNotNone(response)
                status, payload = response
                self.assertEqual(status, 400)
                self.assertEqual(payload["error"], "invalid_request")
                self.assertIn(expected, payload["detail"])

    def test_adapter_returns_none_for_paths_owned_by_other_product_authorities(self):
        with TemporaryDirectory() as directory:
            state_root = Path(directory)
            for path in (
                "/api/run-read",
                "/api/sqx-imported-candidates",
            ):
                self.assertIsNone(builder_http_get_response(state_root, path, {}))
            self.assertIsNone(
                builder_http_post_response(state_root, "/api/sqx-runs/start", {})
            )


if __name__ == "__main__":
    unittest.main()
