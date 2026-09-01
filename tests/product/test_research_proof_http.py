from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.research_proof import ResearchProofError
from tradercockpit.research_proof_http import (
    research_proof_write_response,
    research_proofs_response,
)


class ResearchProofHttpTests(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.store = FileResearchCustodyStore(Path(self.tmp.name) / "data")

    def test_store_is_required(self):
        status, payload = research_proofs_response(None)
        self.assertEqual(status, 503)
        self.assertEqual(payload["reason_code"], "research_store_not_bound")

    def test_catalog_and_exact_read_delegate_to_canonical_proof_custody(self):
        with patch(
            "tradercockpit.research_proof_http.list_current_research_proofs",
            return_value={"schema": "tc.research-proof-catalog.v1", "proofs": []},
        ) as listed:
            status, payload = research_proofs_response(self.store)
        self.assertEqual(status, 200)
        self.assertEqual(payload["proofs"], [])
        listed.assert_called_once_with(self.store)

        with patch(
            "tradercockpit.research_proof_http.read_current_research_proof",
            return_value={"schema": "tc.research-proof.v1", "entity_id": "proof"},
        ) as read:
            status, payload = research_proofs_response(self.store, entity_id="proof")
        self.assertEqual(status, 200)
        self.assertEqual(payload["schema"], "tc.research-proof.v1")
        read.assert_called_once_with(self.store, "proof")

    def test_create_requires_exact_narrow_identity_contract(self):
        status, payload = research_proof_write_response(
            self.store,
            {"action": "create-proof", "idea_entity_id": "x"},
        )
        self.assertEqual(status, 400)
        self.assertEqual(payload["reason_code"], "research_proof_action_invalid")

    def test_create_returns_201_then_200_for_reused_record(self):
        request = {
            "action": "create-proof",
            "idea_entity_id": "idea",
            "idea_revision": "idea-revision",
            "historical_result_entity_id": "historical",
            "historical_result_revision": "historical-revision",
            "validation_ref": "validation",
        }
        with patch(
            "tradercockpit.research_proof_http.create_research_proof",
            return_value={"schema": "tc.research-proof.v1", "reused": False},
        ):
            status, _ = research_proof_write_response(self.store, request)
        self.assertEqual(status, 201)

        with patch(
            "tradercockpit.research_proof_http.create_research_proof",
            return_value={"schema": "tc.research-proof.v1", "reused": True},
        ):
            status, _ = research_proof_write_response(self.store, request)
        self.assertEqual(status, 200)

    def test_proof_failures_are_typed_http_state(self):
        with patch(
            "tradercockpit.research_proof_http.create_research_proof",
            side_effect=ResearchProofError("research_proof_chain_invalid", "substitution"),
        ):
            status, payload = research_proof_write_response(
                self.store,
                {
                    "action": "create-proof",
                    "idea_entity_id": "idea",
                    "idea_revision": "idea-revision",
                    "historical_result_entity_id": "historical",
                    "historical_result_revision": "historical-revision",
                    "validation_ref": "validation",
                },
            )
        self.assertEqual(status, 409)
        self.assertEqual(payload["reason_code"], "research_proof_chain_invalid")


if __name__ == "__main__":
    unittest.main()
