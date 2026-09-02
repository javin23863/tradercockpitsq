from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

from tradercockpit.operate_exports import (
    EXPORT_CATALOG_SCHEMA,
    EXPORT_READ_SCHEMA,
    OperateExportError,
    create_export,
    list_current_exports,
    read_current_export,
)
from tradercockpit.operate_promotions import OperatePromotionError
from tradercockpit.research_custody import FileResearchCustodyStore


class OperateExportTests(unittest.TestCase):
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

    def _promotion(self, **overrides: object) -> dict[str, object]:
        record: dict[str, object] = {
            "schema": "tc.operate-promotion.v1",
            "entity_id": self.PROMOTION_ENTITY,
            "revision": self.PROMOTION_REVISION,
            "content_ref": f"tc-evidence:sha256:{'e' * 64}",
            "proof_entity_id": self.PROOF_ENTITY,
            "proof_revision": self.PROOF_REVISION,
            "candidate_entity_id": self.CANDIDATE_ENTITY,
            "candidate_revision": self.CANDIDATE_REVISION,
            "candidate_archive_name": "Survivor.sqx",
            "candidate_archive_sha256": "f" * 64,
            "historical_result_entity_id": self.HISTORICAL_ENTITY,
            "historical_result_revision": self.HISTORICAL_REVISION,
            "sqx_build": "144.2953",
        }
        record.update(overrides)
        return record

    def test_empty_catalog_is_current_zero(self) -> None:
        payload = list_current_exports(self.store)
        self.assertEqual(payload["schema"], EXPORT_CATALOG_SCHEMA)
        self.assertEqual(payload["exports"], [])

    def test_export_reuses_the_exact_promotion_payload(self) -> None:
        promotion = self._promotion()
        with patch("tradercockpit.operate_exports.read_current_promotion", return_value=promotion):
            first = create_export(self.store, promotion_entity_id=self.PROMOTION_ENTITY)
            second = create_export(self.store, promotion_entity_id=self.PROMOTION_ENTITY)
            catalog = list_current_exports(self.store)
            exact = read_current_export(self.store, first["entity_id"])
        self.assertFalse(first["reused"])
        self.assertTrue(second["reused"])
        self.assertEqual(first["entity_id"], second["entity_id"])
        self.assertEqual(first["schema"], EXPORT_READ_SCHEMA)
        self.assertEqual(first["promotion_entity_id"], self.PROMOTION_ENTITY)
        self.assertEqual(first["candidate_archive_name"], "Survivor.sqx")
        self.assertNotIn("live", first)
        self.assertNotIn("deployed", first)
        self.assertNotIn("broker", first)
        self.assertEqual(len(catalog["exports"]), 1)
        self.assertEqual(exact["revision"], first["revision"])

    def test_missing_promotion_stays_fail_closed(self) -> None:
        with patch(
            "tradercockpit.operate_exports.read_current_promotion",
            side_effect=OperatePromotionError("current_pointer_missing", "Promotion is not current"),
        ):
            with self.assertRaises(OperateExportError) as raised:
                create_export(self.store, promotion_entity_id=self.PROMOTION_ENTITY)
        self.assertEqual(raised.exception.code, "current_pointer_missing")

    def test_invalid_promotion_schema_is_rejected(self) -> None:
        with patch(
            "tradercockpit.operate_exports.read_current_promotion",
            return_value=self._promotion(schema="invalid"),
        ):
            with self.assertRaises(OperateExportError) as raised:
                create_export(self.store, promotion_entity_id=self.PROMOTION_ENTITY)
        self.assertEqual(raised.exception.code, "operate_export_promotion_invalid")


if __name__ == "__main__":
    unittest.main()
