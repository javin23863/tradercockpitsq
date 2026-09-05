from __future__ import annotations

import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

from tradercockpit.assistant import (
    APPROVED_TOOL_NAMES,
    DEFAULT_ASSISTANT_MODEL,
    OPENROUTER_API_KEY_ENV,
    assistant_reply,
    assistant_status_record,
)
from tradercockpit.assistant_product_tools import (
    DRAFT_IDEA_TOOL,
    NAVIGATE_TOOL,
    PROPOSE_SPEC_TOOL,
    REQUEST_COMPILE_TOOL,
    REQUEST_LAUNCH_TOOL,
    canonicalize_navigate_path,
    dispatch_product_tool,
)
from tradercockpit.research_clarifying_questions import load_answers
from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.research_ideas import create_idea, list_current_ideas


def _named_tool_completion(name: str, arguments: dict[str, object]) -> bytes:
    return json.dumps({
        "id": "gen-tool",
        "model": DEFAULT_ASSISTANT_MODEL,
        "choices": [{
            "finish_reason": "tool_calls",
            "message": {
                "role": "assistant",
                "content": None,
                "tool_calls": [{
                    "id": "call_1",
                    "type": "function",
                    "function": {"name": name, "arguments": json.dumps(arguments)},
                }],
            },
        }],
        "usage": {"prompt_tokens": 10, "completion_tokens": 5, "total_tokens": 15},
    }).encode("utf-8")


def _completion(content: str) -> bytes:
    return json.dumps({
        "id": "gen-1",
        "model": DEFAULT_ASSISTANT_MODEL,
        "choices": [{"message": {"role": "assistant", "content": content}}],
        "usage": {"prompt_tokens": 10, "completion_tokens": 5, "total_tokens": 15},
    }).encode("utf-8")


class NavigatePathTests(unittest.TestCase):
    def test_canonical_product_paths_are_allowlisted(self) -> None:
        self.assertEqual(canonicalize_navigate_path("/home"), "/home")
        self.assertEqual(canonicalize_navigate_path("/research"), "/builder")
        self.assertEqual(canonicalize_navigate_path("/explore"), "/home")
        self.assertEqual(canonicalize_navigate_path("/operate"), "/home")
        self.assertEqual(canonicalize_navigate_path("/data-manager"), "/data-manager")
        self.assertEqual(canonicalize_navigate_path("/automation"), "/custom-projects")
        self.assertEqual(canonicalize_navigate_path("/algowizard"), "/apollo")
        self.assertEqual(canonicalize_navigate_path("/retester"), "/builder")
        self.assertEqual(canonicalize_navigate_path("/optimizer"), "/builder")
        self.assertEqual(canonicalize_navigate_path("/apollo"), "/apollo")
        self.assertEqual(canonicalize_navigate_path("/builder"), "/builder")
        self.assertEqual(
            canonicalize_navigate_path("/research?workspace=signals&tab=signals"),
            "/research?workspace=signals&tab=signals",
        )
        self.assertEqual(canonicalize_navigate_path("/research?workspace=evolution"), "/research?workspace=evolution")
        self.assertEqual(
            canonicalize_navigate_path("/research?workspace=validate&tab=evidence"),
            "/research?workspace=validate&tab=evidence",
        )

    def test_executable_and_identity_paths_are_refused(self) -> None:
        self.assertIsNone(canonicalize_navigate_path(r"C:/StrategyQuantX/sqcli.exe"))
        self.assertIsNone(canonicalize_navigate_path("/research?workspace=signals&tab=overview&entityId=x"))
        self.assertIsNone(canonicalize_navigate_path("/research?workspace=not-a-workspace"))
        self.assertIsNone(canonicalize_navigate_path("/research?workspace=evolution&tab=overview"))
        self.assertIsNone(canonicalize_navigate_path("https://example.com/home"))
        self.assertIsNone(canonicalize_navigate_path("/home/../settings"))
        self.assertIsNone(canonicalize_navigate_path("/operate?account=old"))


class ProductToolDispatchTests(unittest.TestCase):
    def test_navigate_proposes_without_confirmation(self) -> None:
        result = dispatch_product_tool(NAVIGATE_TOOL, {"path": "/settings"})
        self.assertIsNotNone(result.proposed_action)
        assert result.proposed_action is not None
        self.assertFalse(result.proposed_action["confirmation_required"])
        self.assertFalse(result.proposed_action["native_mutation"])
        self.assertEqual(result.proposed_action["path"], "/settings")
        self.assertEqual(result.proposed_action["method"], "GET")

    def test_draft_idea_does_not_write_until_confirm(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            result = dispatch_product_tool(DRAFT_IDEA_TOOL, {"text": "Mean reversion around RSI.", "source": "typed"}, store=store)
            self.assertIsNotNone(result.proposed_action)
            assert result.proposed_action is not None
            self.assertTrue(result.proposed_action["confirmation_required"])
            self.assertEqual(result.proposed_action["path"], "/api/research/ideas")
            self.assertEqual(result.proposed_action["body"], {"text": "Mean reversion around RSI.", "source": "typed"})
            self.assertEqual(list_current_ideas(store)["ideas"], [])

    def test_draft_idea_refuses_invented_object_kind_key(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            result = dispatch_product_tool(
                DRAFT_IDEA_TOOL,
                {"text": "RSI", "object_kind": "strategy", "path": "C:/sqcli.exe"},
                store=store,
            )
            payload = json.loads(result.content)
            self.assertEqual(payload["error"], "invalid_arguments")
            self.assertIsNone(result.proposed_action)
            self.assertEqual(list_current_ideas(store)["ideas"], [])

    def test_specification_proposal_is_allowlisted_and_does_not_write(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            idea = create_idea(store, text="Mean reversion around RSI.", source="notes")
            environ = {"TRADERCOCKPIT_WATCHLIST": "ESM5"}
            result = dispatch_product_tool(
                PROPOSE_SPEC_TOOL,
                {"field_id": "object_kind", "answer_id": "strategy"},
                store=store,
                environ=environ,
            )
            self.assertIsNotNone(result.proposed_action)
            assert result.proposed_action is not None
            self.assertEqual(result.proposed_action["path"], "/api/research/clarifying-questions")
            self.assertEqual(result.proposed_action["body"]["answer_id"], "strategy")
            self.assertEqual(result.proposed_action["body"]["entity_id"], idea["entity_id"])
            self.assertEqual(load_answers(store).get("ideas"), {})

            invented = dispatch_product_tool(
                PROPOSE_SPEC_TOOL,
                {"field_id": "object_kind", "answer_id": "ES"},
                store=store,
                environ=environ,
            )
            self.assertEqual(json.loads(invented.content)["error"], "answer_not_allowed")
            self.assertIsNone(invented.proposed_action)

    def test_compile_fails_closed_while_specification_is_locked(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            create_idea(store, text="Mean reversion around RSI.", source="notes")
            result = dispatch_product_tool(REQUEST_COMPILE_TOOL, {}, store=store)
            self.assertEqual(json.loads(result.content)["error"], "specification_locked")
            self.assertIsNone(result.proposed_action)

    def test_compile_proposes_action_compile_only_when_unlocked(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            with patch(
                "tradercockpit.assistant_product_tools.clarifying_questions_record",
                return_value={"reason_code": None, "build_gate": {"locked": False}},
            ):
                result = dispatch_product_tool(REQUEST_COMPILE_TOOL, {}, store=store)
            assert result.proposed_action is not None
            self.assertEqual(result.proposed_action["body"], {"action": "compile"})
            extra = dispatch_product_tool(REQUEST_COMPILE_TOOL, {"xml": "<Task/>"}, store=store)
            self.assertEqual(json.loads(extra.content)["error"], "invalid_arguments")

    def test_launch_fails_closed_without_approved_configuration(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            result = dispatch_product_tool(REQUEST_LAUNCH_TOOL, {}, store=store)
            self.assertEqual(json.loads(result.content)["error"], "native_job_configuration_unapproved")
            path_args = dispatch_product_tool(
                REQUEST_LAUNCH_TOOL,
                {"path": "C:/StrategyQuantX/sqcli.exe"},
                store=store,
            )
            self.assertEqual(json.loads(path_args.content)["error"], "invalid_arguments")

    def test_launch_proposes_exact_approved_identity(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            with patch(
                "tradercockpit.assistant_product_tools.list_current_configurations",
                return_value={
                    "configurations": [{
                        "entity_id": "tc-research:configuration:v1:00000000-0000-0000-0000-000000000001",
                        "revision": "tc-research-revision:configuration:sha256:" + ("a" * 64),
                        "state": "approved",
                    }],
                },
            ):
                result = dispatch_product_tool(REQUEST_LAUNCH_TOOL, {}, store=store)
            assert result.proposed_action is not None
            self.assertEqual(result.proposed_action["path"], "/api/research/native-jobs")
            self.assertEqual(result.proposed_action["body"]["action"], "launch-builder")
            self.assertEqual(
                result.proposed_action["body"]["configuration_entity_id"],
                "tc-research:configuration:v1:00000000-0000-0000-0000-000000000001",
            )
            self.assertFalse(result.proposed_action["native_mutation"])


class ProductToolAssistantRoundTripTests(unittest.TestCase):
    ENV = {OPENROUTER_API_KEY_ENV: "sk-or-test"}

    def test_status_advertises_product_tools_without_native_mutation(self) -> None:
        record = assistant_status_record({OPENROUTER_API_KEY_ENV: "sk-or-test"})
        self.assertEqual(record["tools"]["approved"], list(APPROVED_TOOL_NAMES))
        self.assertFalse(record["tools"]["native_mutation"])
        self.assertIn(REQUEST_LAUNCH_TOOL, record["tools"]["approved"])
        self.assertNotIn("launch_builder", record["tools"]["approved"])

    def test_navigate_round_trip_returns_proposed_action(self) -> None:
        calls = []

        def transport(_url, body, _headers):
            payload = json.loads(body)
            calls.append(payload)
            if payload["messages"][-1]["role"] != "tool":
                return 200, _named_tool_completion(NAVIGATE_TOOL, {"path": "/research?workspace=evolution"})
            return 200, _completion("Opening Evolutionary Search.")

        status, payload = assistant_reply({"message": "open evolutionary search"}, environ=self.ENV, transport=transport)
        self.assertEqual(status, 200)
        self.assertEqual(payload["tools_used"][0]["name"], NAVIGATE_TOOL)
        self.assertEqual(len(payload["proposed_actions"]), 1)
        self.assertEqual(payload["proposed_actions"][0]["path"], "/research?workspace=evolution")
        self.assertFalse(payload["proposed_actions"][0]["confirmation_required"])
        advertised = [item["function"]["name"] for item in calls[0]["tools"]]
        self.assertEqual(advertised[0], "retrieve_quant_guild")
        self.assertEqual(advertised[1:], list(APPROVED_TOOL_NAMES[1:]))

    def test_launch_without_approval_is_refused_not_executed(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            captured = []

            def transport(_url, body, _headers):
                payload = json.loads(body)
                captured.append(payload)
                if payload["messages"][-1]["role"] != "tool":
                    return 200, _named_tool_completion(REQUEST_LAUNCH_TOOL, {})
                return 200, _completion("Launch stays locked until an approved configuration exists.")

            with patch("tradercockpit.sqx_gateway.SqxNativeControlGateway.launch_builder", side_effect=AssertionError("assistant launched SQX")):
                status, payload = assistant_reply(
                    {"message": "launch builder"},
                    environ=self.ENV,
                    transport=transport,
                    research_store=store,
                )
            self.assertEqual(status, 200)
            self.assertEqual(payload["proposed_actions"], [])
            tool_result = json.loads(captured[1]["messages"][-1]["content"])
            self.assertEqual(tool_result["error"], "native_job_configuration_unapproved")


if __name__ == "__main__":
    unittest.main()
