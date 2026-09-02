from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.extensions import DESCRIPTOR_VERSION
from tradercockpit.extensions_http import EXTENSIONS_API_PATH, extensions_register_response
from tradercockpit.research_custody import FileResearchCustodyStore


class ExtensionsHttpTests(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.store = FileResearchCustodyStore(Path(self.tmp.name) / "data")

    def test_store_is_required(self) -> None:
        status, payload = extensions_register_response(None, {"action": "register", "addon": {}})
        self.assertEqual(status, 503)
        self.assertEqual(payload["reason_code"], "research_store_not_bound")

    def test_forbidden_html_registration_returns_400(self) -> None:
        status, payload = extensions_register_response(
            self.store,
            {
                "action": "register",
                "addon": {
                    "id": "inject",
                    "descriptor_version": DESCRIPTOR_VERSION,
                    "owner": "operator",
                    "placement": ["explore"],
                    "read_schema": "tc.inject.v1",
                    "html": "<script>",
                },
            },
        )
        self.assertEqual(status, 400)
        self.assertEqual(payload["reason_code"], "extension_registration_forbidden")

    def test_valid_registration_returns_201(self) -> None:
        status, payload = extensions_register_response(
            self.store,
            {
                "action": "register",
                "addon": {
                    "id": "typed-slot",
                    "descriptor_version": DESCRIPTOR_VERSION,
                    "owner": "operator",
                    "placement": ["settings"],
                    "read_schema": "tc.typed-slot-read.v1",
                },
            },
        )
        self.assertEqual(status, 201)
        self.assertEqual(payload["registered"]["id"], "typed-slot")
        self.assertEqual(EXTENSIONS_API_PATH, "/api/extensions")


if __name__ == "__main__":
    unittest.main()
