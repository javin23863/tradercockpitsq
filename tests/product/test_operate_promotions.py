from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

from tradercockpit.operate_promotions import (
    PROMOTION_CATALOG_SCHEMA,
    PROMOTION_READ_SCHEMA,
    OperatePromotionError,
    create_promotion,
    list_current_promotions,
    read_current_promotion,
)
from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.research_proof import ResearchProofError


class OperatePromotionTests(unittest.TestCase):
    PROOF_ENTITY = "tc-research:proof:v1:aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
    PROOF_REVISION = f"tc-research-revision:proof:sha256:{'a' * 64}"
    CANDIDATE_ENTITY = "tc-research:candidate:v1:bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
    CANDIDATE_REVISION = f"tc-research-revision:candidate:sha256:{'b' * 64}"
    HISTORICAL_ENTITY = "tc-research:historical-result:v1:cccccccc-cccc-4ccc-8ccc-cccccccccccc"
    HISTORICAL_REVISION = f"tc-research-revision:historical-result:sha256:{'c' * 64}"

    def setUp(self) -> None:
        self.tmp = TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.store = FileResearchCustodyStore(Path(self.tmp.name) / "data")

    def _proof(self, **overrides: object) -> dict[str, object]:
        candidate = {
            "entity_id": self.CANDIDATE_ENTITY,
            "revision": self.CANDIDATE_REVISION,
            "archive_name": "Survivor.sqx",
            "archive_sha256": "d" * 64,
        }
        historical = {
            "entity_id": self.HISTORICAL_ENTITY,
            "revision": self.HISTORICAL_REVISION,
        }
        record: dict[str, object] = {
            "schema": "tc.research-proof.v1",
            "entity_id": self.PROOF_ENTITY,
            "revision": self.PROOF_REVISION,
            "sqx_build": "144.2953",
            "candidate": candidate,
            "historical_result": historical,
        }
        record.update(overrides)
        return record

    def test_empty_catalog_is_current_zero(self) -> None:
        payload = list_current_promotions(self.store)
        self.assertEqual(payload["schema"], PROMOTION_CATALOG_SCHEMA)
        self.assertEqual(payload["promotions"], [])

    def test_promote_reuses_the_exact_proof_payload(self) -> None:
        proof = self._proof()
        with patch("tradercockpit.operate_promotions.read_current_research_proof", return_value=proof):
            first = create_promotion(self.store, proof_entity_id=self.PROOF_ENTITY)
            second = create_promotion(self.store, proof_entity_id=self.PROOF_ENTITY)
            catalog = list_current_promotions(self.store)
            exact = read_current_promotion(self.store, first["entity_id"])
        self.assertFalse(first["reused"])
        self.assertTrue(second["reused"])
        self.assertEqual(first["entity_id"], second["entity_id"])
        self.assertEqual(first["schema"], PROMOTION_READ_SCHEMA)
        self.assertEqual(first["proof_entity_id"], self.PROOF_ENTITY)
        self.assertEqual(first["candidate_archive_name"], "Survivor.sqx")
        self.assertNotIn("live", first)
        self.assertNotIn("deployed", first)
        self.assertEqual(len(catalog["promotions"]), 1)
        self.assertEqual(exact["revision"], first["revision"])

    def test_missing_proof_stays_fail_closed(self) -> None:
        with patch(
            "tradercockpit.operate_promotions.read_current_research_proof",
            side_effect=ResearchProofError("current_pointer_missing", "Proof is not current"),
        ):
            with self.assertRaises(OperatePromotionError) as raised:
                create_promotion(self.store, proof_entity_id=self.PROOF_ENTITY)
        self.assertEqual(raised.exception.code, "current_pointer_missing")

    def test_historical_result_is_never_promoted_as_live(self) -> None:
        with patch(
            "tradercockpit.operate_promotions.read_current_research_proof",
            return_value=self._proof(candidate={"entity_id": self.CANDIDATE_ENTITY}),
        ):
            with self.assertRaises(OperatePromotionError) as raised:
                create_promotion(self.store, proof_entity_id=self.PROOF_ENTITY)
        self.assertEqual(raised.exception.code, "operate_promotion_proof_invalid")


if __name__ == "__main__":
    unittest.main()
