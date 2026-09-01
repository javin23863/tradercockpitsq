from __future__ import annotations

import re
import unittest
from pathlib import Path

from tradercockpit import app_server
from tradercockpit.research_capability_inventory import (
    RESEARCH_CAPABILITY_INVENTORY_SCHEMA,
    research_capability_inventory_record,
)


ROOT = Path(__file__).resolve().parents[2]
FRONTEND_CAPABILITIES = ROOT / "web" / "research-capabilities.mjs"


class ResearchCapabilityInventoryTests(unittest.TestCase):
    def test_backend_inventory_matches_frontend_capability_ids_and_exposure(self) -> None:
        inventory = research_capability_inventory_record()
        self.assertEqual(inventory["schema"], RESEARCH_CAPABILITY_INVENTORY_SCHEMA)
        self.assertEqual(inventory["schema"], "tc.research-capability-inventory.v1")
        capabilities = inventory["capabilities"]
        self.assertIsInstance(capabilities, list)
        self.assertEqual(len(capabilities), 20)

        backend = {item["id"]: item["producer_exposure"] for item in capabilities}
        self.assertEqual(len(backend), len(capabilities))

        source = FRONTEND_CAPABILITIES.read_text(encoding="utf-8")
        frontend_pairs = re.findall(
            r'id:\s*"([^"]+)"[\s\S]*?producer_exposure:\s*"([^"]+)"',
            source,
        )
        frontend = dict(frontend_pairs)
        self.assertEqual(len(frontend_pairs), 20)
        self.assertEqual(frontend, backend)

    def test_every_canonical_api_path_is_accounted_for_by_backend_research_inventory(self) -> None:
        inventory = research_capability_inventory_record()
        inventoried_paths = {
            path
            for item in inventory["capabilities"]
            for path in item["api_paths"]
        }
        canonical_paths = {
            value
            for name, value in vars(app_server).items()
            if name.endswith("_API_PATH") and isinstance(value, str) and value.startswith("/api/")
        }
        self.assertTrue(canonical_paths)
        self.assertEqual(canonical_paths - inventoried_paths, set())

    def test_not_exposed_backend_families_have_no_api_path(self) -> None:
        inventory = research_capability_inventory_record()
        for item in inventory["capabilities"]:
            if item["producer_exposure"] == "not_exposed":
                self.assertEqual(item["api_paths"], [])
            else:
                self.assertEqual(item["producer_exposure"], "canonical_read_model")
                self.assertGreater(len(item["api_paths"]), 0)


if __name__ == "__main__":
    unittest.main()
