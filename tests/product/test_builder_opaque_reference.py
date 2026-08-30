from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.builder.api import (
    builder_candidates_response,
    builder_search_read_response,
    builder_search_start_response,
)


class BuilderOpaqueReferenceTests(unittest.TestCase):
    def test_exact_surrounding_whitespace_and_unicode_survive_start_list_and_reopen(self):
        requested = "  opaque/percent%+query?#&= Khmer ខ្មែរ  "
        config = {
            "population_size_per_island": 2,
            "maximum_generations": 1,
            "crossover_probability_pct": 0,
            "mutation_probability_pct": 0,
            "island_count": 1,
            "migration_interval": 1,
            "migration_rate_pct": 0,
            "fresh_blood_replace_similar": False,
            "fresh_blood_replace_weakest": False,
            "random_seed": 41,
        }
        with TemporaryDirectory() as directory:
            root = Path(directory)
            status, created = builder_search_start_response(
                root,
                {"strategyRef": requested, "config": config},
            )
            self.assertEqual(status, 201)
            self.assertEqual(created["requested_strategy_ref"], requested)
            self.assertTrue(created["candidates"])

            status, catalog = builder_candidates_response(root, requested)
            self.assertEqual(status, 200)
            self.assertEqual(catalog["requested_strategy_ref"], requested)
            self.assertEqual(len(catalog["searches"]), 1)
            self.assertEqual(catalog["searches"][0]["requested_strategy_ref"], requested)

            status, reopened = builder_search_read_response(root, created["search_ref"])
            self.assertEqual(status, 200)
            self.assertEqual(reopened["requested_strategy_ref"], requested)
            self.assertEqual(reopened["search_ref"], created["search_ref"])
            self.assertEqual(reopened["candidates"], created["candidates"])

    def test_all_whitespace_reference_is_still_rejected(self):
        with TemporaryDirectory() as directory:
            status, payload = builder_search_start_response(
                Path(directory),
                {"strategyRef": "   ", "config": {}},
            )
            self.assertEqual(status, 400)
            self.assertEqual(payload["error"], "invalid_request")


if __name__ == "__main__":
    unittest.main()
