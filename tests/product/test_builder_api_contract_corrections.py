from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

from tradercockpit.builder.api import (
    builder_candidates_response,
    builder_search_start_response,
)
from tradercockpit.domain import ContentAddress, canonical_json_bytes, canonical_json_loads


class _CatalogService:
    def list_for_strategy(self, requested_strategy_ref):
        return (
            {
                "search_ref": "tc:builder-search:v1:sha256:" + "1" * 64,
                "status": "complete",
                "config_ref": "tc:builder-config:v1:sha256:" + "2" * 64,
                "candidates": [
                    {
                        "candidate_ref": "z-higher-score",
                        "objective_values": {"construction_fit": "9.9"},
                    },
                    {
                        "candidate_ref": "a-lower-score",
                        "objective_values": {"construction_fit": "9.1"},
                    },
                ],
            },
        )


class BuilderApiContractCorrectionTests(unittest.TestCase):
    def test_missing_state_root_is_not_created_by_builder_api(self):
        with TemporaryDirectory() as directory:
            missing = Path(directory) / "mistyped-state-root"
            status, payload = builder_search_start_response(
                missing,
                {"strategyRef": "opaque", "config": {}},
            )
            self.assertEqual(status, 503)
            self.assertEqual(payload["error"], "producer_not_configured")
            self.assertFalse(missing.exists())

    def test_candidate_catalog_orders_exact_decimal_scores_without_integer_truncation(self):
        with patch("tradercockpit.builder.api._service", return_value=_CatalogService()):
            status, payload = builder_candidates_response(Path("unused"), "opaque")
        self.assertEqual(status, 200)
        self.assertEqual(
            [item["candidate_ref"] for item in payload["candidates"]],
            ["z-higher-score", "a-lower-score"],
        )

    def test_repeated_identical_start_reuses_completed_durable_search(self):
        with TemporaryDirectory() as directory:
            root = Path(directory)
            request = {
                "strategyRef": "opaque/idempotent",
                "config": {
                    "population_size_per_island": 2,
                    "maximum_generations": 1,
                    "crossover_probability_pct": 0,
                    "mutation_probability_pct": 0,
                    "island_count": 1,
                    "migration_rate_pct": 0,
                    "fresh_blood_replace_similar": False,
                    "fresh_blood_replace_weakest": False,
                    "random_seed": 31,
                },
            }
            status, first = builder_search_start_response(root, request)
            self.assertEqual(status, 201)
            self.assertEqual(first["status"], "complete")

            with patch(
                "tradercockpit.builder.api.BuilderRuntimeSearchService.run",
                side_effect=AssertionError("completed search must be reused, not rerun"),
            ):
                status, second = builder_search_start_response(root, request)

            self.assertEqual(status, 201)
            self.assertEqual(second, first)

    def test_start_refuses_to_overwrite_valid_incomplete_durable_search(self):
        with TemporaryDirectory() as directory:
            root = Path(directory)
            request = {
                "strategyRef": "opaque/incomplete",
                "config": {
                    "population_size_per_island": 2,
                    "maximum_generations": 1,
                    "crossover_probability_pct": 0,
                    "mutation_probability_pct": 0,
                    "island_count": 1,
                    "migration_rate_pct": 0,
                    "fresh_blood_replace_similar": False,
                    "fresh_blood_replace_weakest": False,
                    "random_seed": 37,
                },
            }
            status, completed = builder_search_start_response(root, request)
            self.assertEqual(status, 201)

            search_ref = ContentAddress.parse(completed["search_ref"])
            state_path = root / "builder-search" / "searches" / f"{search_ref.sha256}.json"
            state = canonical_json_loads(state_path.read_bytes())
            state["status"] = "running"
            state["stage"] = "generation"
            state_path.write_bytes(canonical_json_bytes(state))

            with patch(
                "tradercockpit.builder.api.BuilderRuntimeSearchService.run",
                side_effect=AssertionError("incomplete durable search must not be overwritten"),
            ):
                status, payload = builder_search_start_response(root, request)

            self.assertEqual(status, 409)
            self.assertEqual(payload["error"], "invalid_state")
            self.assertIn("durable incomplete state", payload["detail"])


if __name__ == "__main__":
    unittest.main()
