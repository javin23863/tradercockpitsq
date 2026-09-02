from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

from tradercockpit.operate_exports import OperateExportError
from tradercockpit.operate_exports_http import (
    operate_export_write_response,
    operate_exports_response,
)
from tradercockpit.research_custody import FileResearchCustodyStore


class OperateExportsHttpTests(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.store = FileResearchCustodyStore(Path(self.tmp.name) / "data")

    def test_unbound_store_is_503(self) -> None:
        status, payload = operate_exports_response(None)
        self.assertEqual(status, 503)
        self.assertEqual(payload["reason_code"], "research_store_not_bound")

    def test_catalog_and_exact_read_delegate_to_export_custody(self) -> None:
        with patch(
            "tradercockpit.operate_exports_http.list_current_exports",
            return_value={"schema": "tc.operate-export-catalog.v1", "exports": []},
        ):
            status, payload = operate_exports_response(self.store)
        self.assertEqual(status, 200)
        self.assertEqual(payload["exports"], [])

        with patch(
            "tradercockpit.operate_exports_http.read_current_export",
            return_value={"schema": "tc.operate-export.v1", "entity_id": "export"},
        ):
            status, payload = operate_exports_response(self.store, entity_id="export")
        self.assertEqual(status, 200)
        self.assertEqual(payload["schema"], "tc.operate-export.v1")

    def test_export_write_requires_exact_action_and_identity(self) -> None:
        status, payload = operate_export_write_response(self.store, {"action": "promote"})
        self.assertEqual(status, 400)
        self.assertEqual(payload["reason_code"], "operate_export_action_invalid")

        request = {"action": "export", "promotion_entity_id": "tc-research:promotion:v1:aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"}
        with patch(
            "tradercockpit.operate_exports_http.create_export",
            return_value={"schema": "tc.operate-export.v1", "reused": False},
        ):
            status, _ = operate_export_write_response(self.store, request)
        self.assertEqual(status, 201)

        with patch(
            "tradercockpit.operate_exports_http.create_export",
            return_value={"schema": "tc.operate-export.v1", "reused": True},
        ):
            status, _ = operate_export_write_response(self.store, request)
        self.assertEqual(status, 200)

    def test_export_failures_are_typed_http_state(self) -> None:
        with patch(
            "tradercockpit.operate_exports_http.create_export",
            side_effect=OperateExportError("current_pointer_missing", "no promotion"),
        ):
            status, payload = operate_export_write_response(
                self.store,
                {"action": "export", "promotion_entity_id": "tc-research:promotion:v1:aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"},
            )
        self.assertEqual(status, 404)
        self.assertEqual(payload["reason_code"], "current_pointer_missing")


if __name__ == "__main__":
    unittest.main()
