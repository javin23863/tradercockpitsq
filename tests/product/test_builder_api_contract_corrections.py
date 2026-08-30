from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

from tradercockpit.builder.api import (
    builder_candidates_response,
    builder_search_start_response,
)


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


if __name__ == "__main__":
    unittest.main()
