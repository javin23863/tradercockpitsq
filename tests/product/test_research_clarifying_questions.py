from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory
from http.server import ThreadingHTTPServer
from threading import Thread
import json
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from unittest.mock import patch
import unittest

from tradercockpit.app_server import make_handler

from tradercockpit.research_clarifying_questions import (
    QUESTIONS_SCHEMA,
    ClarifyingQuestionError,
    clarifying_questions_record,
    clarifying_questions_write,
    open_question_count,
    questions_from_idea,
    record_answer,
)
from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.research_ideas import create_idea
from tradercockpit.research_next_action import next_action_from_catalogs


class ClarifyingQuestionTests(unittest.TestCase):
    def test_without_an_idea_questions_are_locked_and_unanswerable(self) -> None:
        record = questions_from_idea(None)
        self.assertEqual(record["schema"], QUESTIONS_SCHEMA)
        self.assertEqual(record["reason_code"], "idea_required")
        self.assertTrue(record["build_gate"]["locked"])
        self.assertEqual(record["questions"], [])
        self.assertIsNone(record["current_question"])

    def test_idea_without_draft_starts_at_object_kind(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            idea = create_idea(store, text="Mean reversion around RSI.", source="notebook")
            record = clarifying_questions_record(store, environ={"TRADERCOCKPIT_WATCHLIST": "ESM5"})
            self.assertEqual(record["idea_entity_id"], idea["entity_id"])
            self.assertEqual(record["object_kind"], "unresolved")
            self.assertEqual(record["open_count"], 1)
            current = record["current_question"]
            self.assertEqual(current["id"], "object_kind")
            self.assertEqual({item["id"] for item in current["allowed_answers"]}, {"indicator", "strategy", "model"})
            self.assertTrue(record["build_gate"]["locked"])
            self.assertIn("unresolved:object_kind", record["build_gate"]["reason_codes"])

    def test_bound_strategy_draft_skips_object_kind_and_asks_watchlist_symbol(self) -> None:
        idea = {
            "entity_id": "tc-research:idea:v1:00000000-0000-0000-0000-000000000001",
            "revision": "rev-1",
            "draft": {"object_kind": "strategy", "status": "bound"},
        }
        record = questions_from_idea(
            idea,
            environ={"TRADERCOCKPIT_WATCHLIST": "ESM5,NQ"},
        )
        by_id = {item["id"]: item for item in record["questions"]}
        self.assertEqual(by_id["object_kind"]["status"], "resolved")
        self.assertEqual(by_id["object_kind"]["source"], "idea_draft")
        self.assertEqual(record["object_kind"], "strategy")
        self.assertEqual(record["current_question"]["id"], "market_identity")
        self.assertEqual([item["id"] for item in record["current_question"]["allowed_answers"]], ["ESM5", "NQ"])
        self.assertNotIn("estimator_family", by_id)

    def test_empty_watchlist_blocks_symbol_without_inventing_one(self) -> None:
        idea = {
            "entity_id": "tc-research:idea:v1:00000000-0000-0000-0000-000000000002",
            "revision": "rev-1",
            "draft": {"object_kind": "strategy"},
        }
        record = questions_from_idea(idea, environ={})
        market = next(item for item in record["questions"] if item["id"] == "market_identity")
        self.assertEqual(market["status"], "blocked")
        self.assertEqual(market["reason_code"], "watchlist_empty")
        self.assertEqual(market["allowed_answers"], [])
        self.assertTrue(record["build_gate"]["locked"])

    def test_answers_must_be_in_the_allowed_set(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            create_idea(store, text="RSI mean reversion.", source="notebook")
            env = {"TRADERCOCKPIT_WATCHLIST": "NQ"}
            with self.assertRaises(ClarifyingQuestionError) as invented:
                record_answer(store, field_id="object_kind", answer_id="alpha_signal", environ=env)
            self.assertEqual(invented.exception.code, "answer_not_allowed")

            updated = record_answer(store, field_id="object_kind", answer_id="strategy", environ=env)
            self.assertEqual(updated["object_kind"], "strategy")
            self.assertEqual(updated["open_count"] > 0, True)
            market = updated["current_question"]
            self.assertEqual(market["id"], "market_identity")
            with self.assertRaises(ClarifyingQuestionError) as fake_symbol:
                record_answer(store, field_id="market_identity", answer_id="ES", environ=env)
            self.assertEqual(fake_symbol.exception.code, "answer_not_allowed")
            chosen = record_answer(store, field_id="market_identity", answer_id="NQ", environ=env)
            resolved = next(item for item in chosen["questions"] if item["id"] == "market_identity")
            self.assertEqual(resolved["status"], "resolved")
            self.assertEqual(resolved["source"], "user_selected")
            self.assertEqual(resolved["answer"]["id"], "NQ")

            status, refused = clarifying_questions_write(
                store,
                {"field_id": "timeframe", "answer_id": "H7", "extra": True},
                environ=env,
            )
            self.assertEqual(status, 400)

    def test_model_kind_asks_allowlisted_family_and_leakage_controls(self) -> None:
        idea = {
            "entity_id": "tc-research:idea:v1:00000000-0000-0000-0000-000000000003",
            "revision": "rev-1",
            "draft": {"object_kind": "model"},
        }
        record = questions_from_idea(idea, environ={"TRADERCOCKPIT_WATCHLIST": "ESM5"})
        ids = [item["id"] for item in record["questions"]]
        self.assertIn("estimator_family", ids)
        self.assertIn("leakage_split", ids)
        self.assertNotIn("search_build_mode", ids)
        family = next(item for item in record["questions"] if item["id"] == "estimator_family")
        self.assertTrue(any(item["id"].startswith("sklearn.") for item in family["allowed_answers"]))

    def test_native_producer_configured_fields_are_not_reasked(self) -> None:
        idea = {
            "entity_id": "tc-research:idea:v1:00000000-0000-0000-0000-000000000004",
            "revision": "rev-1",
            "draft": {"object_kind": "strategy"},
        }
        record = questions_from_idea(
            idea,
            native_states={"market_identity": "producer_configured", "search_build_mode": "producer_configured"},
            environ={"TRADERCOCKPIT_WATCHLIST": "ESM5"},
        )
        by_id = {item["id"]: item for item in record["questions"]}
        self.assertEqual(by_id["market_identity"]["status"], "resolved")
        self.assertEqual(by_id["market_identity"]["source"], "producer_configured")
        self.assertEqual(by_id["search_build_mode"]["source"], "producer_configured")
        self.assertEqual(record["current_question"]["id"], "session")

    def test_next_action_prefers_open_questions_before_compile(self) -> None:
        record = next_action_from_catalogs(ideas=[{"entity_id": "idea-1"}], open_questions=2)
        self.assertEqual(record["next_action"]["id"], "answer_clarifying_questions")
        self.assertEqual(record["current_stage"], "specification")
        self.assertEqual(record["next_action"]["path"], "/research?workspace=signals&tab=signals")
        compiled = next_action_from_catalogs(ideas=[{"entity_id": "idea-1"}], open_questions=0)
        self.assertEqual(compiled["next_action"]["id"], "specify_and_compile")

    def test_open_question_count_is_zero_without_an_idea(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            self.assertEqual(open_question_count(store), 0)
            self.assertEqual(open_question_count(None), 0)

    def test_http_loopback_answers_and_refuses_invention(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            web = root / "web"
            web.mkdir()
            (web / "index.html").write_text("<main>TraderCockpit</main>", encoding="utf-8")
            store = FileResearchCustodyStore(root / "data")
            create_idea(store, text="RSI mean reversion.", source="notebook")
            server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(web, None, None, store))
            thread = Thread(target=server.serve_forever, daemon=True)
            thread.start()
            base = f"http://127.0.0.1:{server.server_port}"
            env = {"TRADERCOCKPIT_WATCHLIST": "NQ"}
            try:
                with patch.dict("os.environ", env, clear=False):
                    with urlopen(base + "/api/research/clarifying-questions", timeout=2) as response:
                        payload = json.loads(response.read().decode("utf-8"))
                    self.assertEqual(payload["schema"], QUESTIONS_SCHEMA)
                    self.assertEqual(payload["current_question"]["id"], "object_kind")

                    bad = Request(
                        base + "/api/research/clarifying-questions",
                        data=json.dumps({"field_id": "object_kind", "answer_id": "alpha"}).encode(),
                        headers={"content-type": "application/json"},
                        method="POST",
                    )
                    with self.assertRaises(HTTPError) as raised:
                        urlopen(bad, timeout=2)
                    self.assertEqual(raised.exception.code, 400)
                    refused = json.loads(raised.exception.read().decode("utf-8"))
                    self.assertEqual(refused["reason_code"], "answer_not_allowed")

                    ok = Request(
                        base + "/api/research/clarifying-questions",
                        data=json.dumps({"field_id": "object_kind", "answer_id": "strategy"}).encode(),
                        headers={"content-type": "application/json"},
                        method="POST",
                    )
                    with urlopen(ok, timeout=2) as response:
                        answered = json.loads(response.read().decode("utf-8"))
                    self.assertEqual(answered["object_kind"], "strategy")
                    self.assertEqual(answered["current_question"]["id"], "market_identity")

                    with urlopen(base + "/api/research/next-action", timeout=2) as response:
                        nxt = json.loads(response.read().decode("utf-8"))
                    self.assertEqual(nxt["next_action"]["id"], "answer_clarifying_questions")
            finally:
                server.shutdown()
                server.server_close()
                thread.join()


if __name__ == "__main__":
    unittest.main()
