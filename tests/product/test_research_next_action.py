from __future__ import annotations

from tempfile import TemporaryDirectory
from unittest.mock import patch
import unittest

from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.research_ideas import create_idea
from tradercockpit.research_next_action import (
    RESEARCH_NEXT_ACTION_SCHEMA,
    next_action_from_catalogs,
    research_next_action_record,
)


class ResearchNextActionTests(unittest.TestCase):
    def test_empty_custody_asks_for_an_idea(self) -> None:
        record = next_action_from_catalogs()
        self.assertEqual(record["schema"], RESEARCH_NEXT_ACTION_SCHEMA)
        self.assertEqual(record["current_stage"], "idea")
        self.assertEqual(record["next_action"]["id"], "create_idea")
        self.assertEqual(record["next_action"]["path"], "/research?workspace=signals&tab=overview")
        self.assertIn("specification", record["locked_stages"])
        self.assertIn("build", record["locked_stages"])

    def test_idea_without_configuration_asks_to_specify(self) -> None:
        record = next_action_from_catalogs(ideas=[{"entity_id": "idea-1"}])
        self.assertEqual(record["next_action"]["id"], "specify_and_compile")
        self.assertEqual(record["current_stage"], "specification")
        questions = next_action_from_catalogs(ideas=[{"entity_id": "idea-1"}], open_questions=1)
        self.assertEqual(questions["next_action"]["id"], "answer_clarifying_questions")

    def test_unapproved_configuration_asks_for_review(self) -> None:
        record = next_action_from_catalogs(
            ideas=[{"entity_id": "idea-1"}],
            configurations=[{"state": "compiled"}],
        )
        self.assertEqual(record["next_action"]["id"], "approve_configuration")
        self.assertEqual(record["current_stage"], "build")

    def test_approved_configuration_without_job_asks_to_launch(self) -> None:
        record = next_action_from_catalogs(
            ideas=[{"entity_id": "idea-1"}],
            configurations=[{"state": "approved"}],
            jobs=[{"state": "failed"}],
        )
        self.assertEqual(record["next_action"]["id"], "launch_builder")

    def test_submitted_job_without_candidates_asks_to_import(self) -> None:
        record = next_action_from_catalogs(
            ideas=[{"entity_id": "idea-1"}],
            configurations=[{"state": "approved"}],
            jobs=[{"state": "submitted"}],
        )
        self.assertEqual(record["next_action"]["id"], "import_candidates")
        self.assertEqual(record["current_stage"], "candidates")

    def test_candidates_without_completed_result_ask_for_historical_test(self) -> None:
        record = next_action_from_catalogs(
            ideas=[{"entity_id": "idea-1"}],
            configurations=[{"state": "approved"}],
            jobs=[{"state": "completed"}],
            candidates=[{"entity_id": "cand-1"}],
            results=[{"state": "failed"}],
        )
        self.assertEqual(record["next_action"]["id"], "run_historical_test")
        self.assertEqual(record["current_stage"], "backtest")

    def test_completed_result_without_proof_asks_to_bind_proof(self) -> None:
        record = next_action_from_catalogs(
            ideas=[{"entity_id": "idea-1"}],
            configurations=[{"state": "approved"}],
            jobs=[{"state": "completed"}],
            candidates=[{"entity_id": "cand-1"}],
            results=[{"state": "completed"}],
        )
        self.assertEqual(record["next_action"]["id"], "create_proof")
        self.assertEqual(record["next_action"]["path"], "/research?workspace=validate&tab=evidence")

    def test_bound_chain_moves_to_maintain(self) -> None:
        record = next_action_from_catalogs(
            ideas=[{"entity_id": "idea-1"}],
            configurations=[{"state": "approved"}],
            jobs=[{"state": "completed"}],
            candidates=[{"entity_id": "cand-1"}],
            results=[{"state": "completed"}],
            proofs=[{"entity_id": "proof-1"}],
        )
        self.assertEqual(record["next_action"]["id"], "maintain")
        self.assertEqual(record["locked_stages"], [])
        self.assertEqual(record["current_stage"], "proof")

    def test_missing_store_fails_closed(self) -> None:
        record = research_next_action_record(None)
        self.assertEqual(record["schema"], RESEARCH_NEXT_ACTION_SCHEMA)
        self.assertIsNone(record["next_action"])
        self.assertEqual(record["reason_code"], "custody_unavailable")
        self.assertEqual(record["locked_stages"], ["idea", "specification", "build", "candidates", "backtest", "proof"])

    def test_empty_store_does_not_open_the_question_catalog(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            with patch("tradercockpit.research_clarifying_questions.open_question_count") as count:
                record = research_next_action_record(store, sqx_home="/not-a-runtime")
            count.assert_not_called()
            self.assertEqual(record["next_action"]["id"], "create_idea")

    def test_idea_store_reads_open_question_count(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            create_idea(store, text="Mean reversion around RSI.", source="notebook")
            with patch("tradercockpit.research_clarifying_questions.open_question_count", return_value=1) as count:
                record = research_next_action_record(store, sqx_home=None)
            count.assert_called_once()
            self.assertEqual(record["next_action"]["id"], "answer_clarifying_questions")


if __name__ == "__main__":
    unittest.main()
