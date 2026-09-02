from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

from tradercockpit.operate_deployments import (
    DEPLOYMENT_CATALOG_SCHEMA,
    DEPLOYMENT_READ_SCHEMA,
    OperateDeploymentError,
    create_deployment,
    list_current_deployments,
    read_current_deployment,
)
from tradercockpit.operate_exports import OperateExportError
from tradercockpit.research_custody import FileResearchCustodyStore


class OperateDeploymentTests(unittest.TestCase):
    EXPORT_ENTITY = "tc-research:export:v1:eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee"
    EXPORT_REVISION = f"tc-research-revision:export:sha256:{'e' * 64}"
    PROMOTION_ENTITY = "tc-research:promotion:v1:aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
    PROMOTION_REVISION = f"tc-research-revision:promotion:sha256:{'a' * 64}"
    PROOF_ENTITY = "tc-research:proof:v1:bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
    PROOF_REVISION = f"tc-research-revision:proof:sha256:{'b' * 64}"
    CANDIDATE_ENTITY = "tc-research:candidate:v1:cccccccc-cccc-4ccc-8ccc-cccccccccccc"
    CANDIDATE_REVISION = f"tc-research-revision:candidate:sha256:{'c' * 64}"
    HISTORICAL_ENTITY = "tc-research:historical-result:v1:dddddddd-dddd-4ddd-8ddd-dddddddddddd"
    HISTORICAL_REVISION = f"tc-research-revision:historical-result:sha256:{'d' * 64}"

    def setUp(self) -> None:
        self.tmp = TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.store = FileResearchCustodyStore(Path(self.tmp.name) / "data")

    def _export(self, **overrides: object) -> dict[str, object]:
        record: dict[str, object] = {
            "schema": "tc.operate-export.v1",
            "entity_id": self.EXPORT_ENTITY,
            "revision": self.EXPORT_REVISION,
            "content_ref": f"tc-evidence:sha256:{'f' * 64}",
            "promotion_entity_id": self.PROMOTION_ENTITY,
            "promotion_revision": self.PROMOTION_REVISION,
            "proof_entity_id": self.PROOF_ENTITY,
            "proof_revision": self.PROOF_REVISION,
            "candidate_entity_id": self.CANDIDATE_ENTITY,
            "candidate_revision": self.CANDIDATE_REVISION,
            "candidate_archive_name": "Survivor.sqx",
            "candidate_archive_sha256": "1" * 64,
            "historical_result_entity_id": self.HISTORICAL_ENTITY,
            "historical_result_revision": self.HISTORICAL_REVISION,
            "sqx_build": "144.2953",
        }
        record.update(overrides)
        return record

    def test_empty_catalog_is_current_zero(self) -> None:
        payload = list_current_deployments(self.store)
        self.assertEqual(payload["schema"], DEPLOYMENT_CATALOG_SCHEMA)
        self.assertEqual(payload["deployments"], [])

    def test_deployment_reuses_the_exact_export_payload(self) -> None:
        export = self._export()
        with patch("tradercockpit.operate_deployments.read_current_export", return_value=export):
            first = create_deployment(self.store, export_entity_id=self.EXPORT_ENTITY)
            second = create_deployment(self.store, export_entity_id=self.EXPORT_ENTITY)
            catalog = list_current_deployments(self.store)
            exact = read_current_deployment(self.store, first["entity_id"])
        self.assertFalse(first["reused"])
        self.assertTrue(second["reused"])
        self.assertEqual(first["entity_id"], second["entity_id"])
        self.assertEqual(first["schema"], DEPLOYMENT_READ_SCHEMA)
        self.assertEqual(first["export_entity_id"], self.EXPORT_ENTITY)
        self.assertEqual(first["candidate_archive_name"], "Survivor.sqx")
        self.assertEqual(first["mode"], "identity_only")
        self.assertEqual(first["status"], "execution_not_connected")
        self.assertNotIn("live", first)
        self.assertNotIn("broker", first)
        self.assertNotIn("positions", first)
        self.assertEqual(len(catalog["deployments"]), 1)
        self.assertEqual(exact["revision"], first["revision"])

    def test_missing_export_stays_fail_closed(self) -> None:
        with patch(
            "tradercockpit.operate_deployments.read_current_export",
            side_effect=OperateExportError("current_pointer_missing", "Export is not current"),
        ):
            with self.assertRaises(OperateDeploymentError) as raised:
                create_deployment(self.store, export_entity_id=self.EXPORT_ENTITY)
        self.assertEqual(raised.exception.code, "current_pointer_missing")

    def test_invalid_export_schema_is_rejected(self) -> None:
        with patch(
            "tradercockpit.operate_deployments.read_current_export",
            return_value=self._export(schema="invalid"),
        ):
            with self.assertRaises(OperateDeploymentError) as raised:
                create_deployment(self.store, export_entity_id=self.EXPORT_ENTITY)
        self.assertEqual(raised.exception.code, "operate_deployment_export_invalid")


if __name__ == "__main__":
    unittest.main()
