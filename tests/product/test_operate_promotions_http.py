from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

from tradercockpit.operate_promotions import OperatePromotionError
from tradercockpit.operate_promotions_http import (
    operate_promotion_write_response,
    operate_promotions_response,
)
from tradercockpit.research_custody import FileResearchCustodyStore


class OperatePromotionHttpTests(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.store = FileResearchCustodyStore(Path(self.tmp.name) / "data")

    def test_store_is_required(self) -> None:
        status, payload = operate_promotions_response(None)
        self.assertEqual(status, 503)
        self.assertEqual(payload["reason_code"], "research_store_not_bound")

    def test_catalog_and_exact_read_delegate_to_promotion_custody(self) -> None:
        with patch(
            "tradercockpit.operate_promotions_http.list_current_promotions",
            return_value={"schema": "tc.operate-promotion-catalog.v1", "promotions": []},
        ) as listed:
            status, payload = operate_promotions_response(self.store)
        self.assertEqual(status, 200)
        self.assertEqual(payload["promotions"], [])
        listed.assert_called_once_with(self.store)

        with patch(
            "tradercockpit.operate_promotions_http.read_current_promotion",
            return_value={"schema": "tc.operate-promotion.v1", "entity_id": "promotion"},
        ) as read:
            status, payload = operate_promotions_response(self.store, entity_id="promotion")
        self.assertEqual(status, 200)
        self.assertEqual(payload["schema"], "tc.operate-promotion.v1")
        read.assert_called_once_with(self.store, "promotion")

    def test_promote_requires_exact_narrow_identity_contract(self) -> None:
        status, payload = operate_promotion_write_response(
            self.store,
            {"action": "promote", "proof_entity_id": "x", "candidate_entity_id": "nope"},
        )
        self.assertEqual(status, 400)
        self.assertEqual(payload["reason_code"], "operate_promotion_action_invalid")

    def test_create_returns_201_then_200_for_reused_record(self) -> None:
        request = {"action": "promote", "proof_entity_id": "proof"}
        with patch(
            "tradercockpit.operate_promotions_http.create_promotion",
            return_value={"schema": "tc.operate-promotion.v1", "reused": False},
        ):
            status, _ = operate_promotion_write_response(self.store, request)
        self.assertEqual(status, 201)

        with patch(
            "tradercockpit.operate_promotions_http.create_promotion",
            return_value={"schema": "tc.operate-promotion.v1", "reused": True},
        ):
            status, _ = operate_promotion_write_response(self.store, request)
        self.assertEqual(status, 200)

    def test_promotion_failures_are_typed_http_state(self) -> None:
        with patch(
            "tradercockpit.operate_promotions_http.create_promotion",
            side_effect=OperatePromotionError("operate_promotion_proof_invalid", "no proof"),
        ):
            status, payload = operate_promotion_write_response(
                self.store,
                {"action": "promote", "proof_entity_id": "proof"},
            )
        self.assertEqual(status, 409)
        self.assertEqual(payload["reason_code"], "operate_promotion_proof_invalid")


if __name__ == "__main__":
    unittest.main()
