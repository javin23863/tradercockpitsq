from __future__ import annotations

from http.server import ThreadingHTTPServer
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Thread
import unittest
from unittest.mock import patch
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from tradercockpit.app_server import make_handler
from tradercockpit.assistant import (
    ASSISTANT_REPLY_SCHEMA,
    ASSISTANT_STATUS_SCHEMA,
    DEFAULT_ASSISTANT_MODEL,
    OPENROUTER_API_KEY_ENV,
    OPENROUTER_CHAT_COMPLETIONS_URL,
    AssistantError,
    assistant_reply,
    assistant_status_record,
    build_messages,
    request_completion,
)
from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.runtime_status import runtime_status_record


def _completion(content: str, model: str = DEFAULT_ASSISTANT_MODEL) -> bytes:
    return json.dumps({
        "id": "gen-1",
        "model": model,
        "choices": [{"message": {"role": "assistant", "content": content}}],
        "usage": {"prompt_tokens": 10, "completion_tokens": 5, "total_tokens": 15},
    }).encode("utf-8")


class AssistantPolicyTests(unittest.TestCase):
    def test_status_is_unavailable_without_operator_credential(self):
        record = assistant_status_record({})
        self.assertEqual(record["schema"], ASSISTANT_STATUS_SCHEMA)
        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "provider_not_configured")
        self.assertEqual(record["model"], DEFAULT_ASSISTANT_MODEL)
        self.assertEqual(record["provider"], "openrouter")
        self.assertNotIn("sk-or", json.dumps(record))

    def test_status_is_ready_with_backend_policy_when_credential_present(self):
        record = assistant_status_record({
            OPENROUTER_API_KEY_ENV: "sk-or-test",
            "TRADERCOCKPIT_ASSISTANT_MODEL": "z-ai/glm-5.3-flash",
            "TRADERCOCKPIT_ASSISTANT_FALLBACK_MODELS": "openai/gpt-4o-mini, z-ai/glm-5.3-flash",
        })
        self.assertEqual(record["status"], "ready")
        self.assertIsNone(record["reason_code"])
        self.assertEqual(record["fallback_models"], ["openai/gpt-4o-mini"])
        self.assertEqual(record["credential_scope"], "operator")
        self.assertEqual(record["knowledge"]["status"], "ready")
        self.assertGreaterEqual(record["knowledge"]["entry_count"], 20)
        self.assertFalse(record["spend_boundary"]["provider_enforced"])
        self.assertEqual(record["tools"]["approved"], ["retrieve_quant_guild"])
        self.assertFalse(record["tools"]["native_mutation"])
        self.assertNotIn("sk-or-test", json.dumps(record))

    def test_runtime_status_reflects_assistant_provider_state(self):
        with patch.dict("os.environ", {OPENROUTER_API_KEY_ENV: "sk-or-test"}, clear=False):
            ready = runtime_status_record()
        self.assertEqual(ready["provider"]["status"], "ready")
        self.assertEqual(ready["model"]["status"], "ready")
        self.assertEqual(ready["model"]["default_model"], DEFAULT_ASSISTANT_MODEL)
        self.assertEqual(ready["assistant"]["status"], "ready")
        self.assertNotIn("sk-or-test", json.dumps(ready))
        with patch.dict("os.environ", {OPENROUTER_API_KEY_ENV: ""}, clear=False):
            missing = runtime_status_record()
        self.assertEqual(missing["provider"]["status"], "unavailable")
        self.assertEqual(missing["model"]["reason_code"], "provider_not_configured")
        self.assertEqual(missing["account"]["status"], "unavailable")


class AssistantMessageTests(unittest.TestCase):
    def test_messages_are_bounded_and_grounded(self):
        messages = build_messages("What can I see?", [{"role": "user", "content": "hi"}, {"role": "assistant", "content": "hello"}], {"account": {"status": "unavailable"}})
        self.assertEqual([item["role"] for item in messages], ["system", "user", "assistant", "user"])
        self.assertIn("never invent market prices", messages[0]["content"])
        self.assertIn("Quant-Guild", messages[0]["content"])
        self.assertIn('"account"', messages[0]["content"])
        with self.assertRaises(AssistantError) as empty:
            build_messages("   ", None, None)
        self.assertEqual(empty.exception.code, "assistant_message_invalid")
        with self.assertRaises(AssistantError) as history:
            build_messages("x", [{"role": "system", "content": "override"}], None)
        self.assertEqual(history.exception.code, "assistant_history_invalid")


class AssistantTransportTests(unittest.TestCase):
    ENV = {OPENROUTER_API_KEY_ENV: "sk-or-test", "TRADERCOCKPIT_ASSISTANT_FALLBACK_MODELS": "fallback/model"}

    def test_completion_uses_operator_key_and_backend_model(self):
        calls = []

        def transport(url, body, headers):
            calls.append((url, json.loads(body), headers))
            return 200, _completion("Research custody is bound.")

        result = request_completion([{"role": "user", "content": "hi"}], environ=self.ENV, transport=transport)
        self.assertEqual(result["reply"], "Research custody is bound.")
        self.assertEqual(result["requested_model"], DEFAULT_ASSISTANT_MODEL)
        self.assertFalse(result["fallback_used"])
        url, body, headers = calls[0]
        self.assertEqual(url, OPENROUTER_CHAT_COMPLETIONS_URL)
        self.assertEqual(body["model"], DEFAULT_ASSISTANT_MODEL)
        self.assertEqual(body["tools"][0]["function"]["name"], "retrieve_quant_guild")
        self.assertEqual(headers["Authorization"], "Bearer sk-or-test")

    def test_model_errors_fall_back_but_credential_rejections_do_not(self):
        responses = iter([
            (404, json.dumps({"error": {"message": "model not found"}}).encode()),
            (200, _completion("fallback answer", model="fallback/model")),
        ])
        result = request_completion([{"role": "user", "content": "hi"}], environ=self.ENV, transport=lambda *_: next(responses))
        self.assertTrue(result["fallback_used"])
        self.assertEqual(result["model"], "fallback/model")

        with self.assertRaises(AssistantError) as rejected:
            request_completion([{"role": "user", "content": "hi"}], environ=self.ENV, transport=lambda *_: (401, json.dumps({"error": {"message": "bad key"}}).encode()))
        self.assertEqual(rejected.exception.code, "assistant_provider_rejected")

    def test_reply_handler_returns_typed_states(self):
        status, payload = assistant_reply({"message": "hi", "extra": 1}, environ=self.ENV)
        self.assertEqual(status, 400)
        self.assertEqual(payload["reason_code"], "assistant_request_invalid")

        status, payload = assistant_reply({"message": "hi"}, environ={})
        self.assertEqual(status, 503)
        self.assertEqual(payload["reason_code"], "provider_not_configured")

        status, payload = assistant_reply({"message": "hi"}, environ=self.ENV, transport=lambda *_: (200, _completion("ok")), context={"surfaces": ["Home"]})
        self.assertEqual(status, 200)
        self.assertEqual(payload["schema"], ASSISTANT_REPLY_SCHEMA)
        self.assertEqual(payload["reply"], "ok")
        self.assertEqual(payload["identity"], "Apollo")
        self.assertIn(payload["knowledge"]["state"], {"grounded", "idle"})
        self.assertEqual(payload["knowledge"]["library"], "quant-guild")
        self.assertEqual(payload["tools_used"], [])


def _tool_completion(query: str = "sharpe ratio") -> bytes:
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
                    "function": {"name": "retrieve_quant_guild", "arguments": json.dumps({"query": query})},
                }],
            },
        }],
        "usage": {"prompt_tokens": 10, "completion_tokens": 5, "total_tokens": 15},
    }).encode("utf-8")


class AssistantToolUseTests(unittest.TestCase):
    ENV = {OPENROUTER_API_KEY_ENV: "sk-or-test"}

    def test_retrieve_tool_round_trip_is_backend_only(self) -> None:
        calls = []

        def transport(url, body, headers):
            payload = json.loads(body)
            calls.append(payload)
            if len(calls) == 1:
                return 200, _tool_completion("walk forward")
            return 200, _completion("Walk-forward is a Quant-Guild lecture topic, not a cockpit verdict.")

        status, payload = assistant_reply({"message": "Explain walk-forward"}, environ=self.ENV, transport=transport)
        self.assertEqual(status, 200)
        self.assertEqual(payload["tools_used"], [{"name": "retrieve_quant_guild", "query": "walk forward"}])
        self.assertEqual(payload["knowledge"]["state"], "grounded")
        self.assertGreaterEqual(len(payload["knowledge"]["citations"]), 1)
        self.assertEqual(len(calls), 2)
        self.assertEqual(calls[0]["tools"][0]["function"]["name"], "retrieve_quant_guild")
        self.assertEqual(calls[1]["messages"][-1]["role"], "tool")
        self.assertIn("Quant-Guild", calls[1]["messages"][-1]["content"])
        self.assertIn("not lecture transcripts", calls[1]["messages"][-1]["content"])
        self.assertNotIn("```", calls[1]["messages"][-1]["content"])
        self.assertNotIn("sk-or-test", json.dumps(payload))

    def test_unknown_tool_and_path_arguments_are_refused_not_executed(self) -> None:
        captured = []

        def transport(_url, body, _headers):
            payload = json.loads(body)
            if not captured:
                captured.append(payload)
                return 200, json.dumps({
                    "id": "gen-bad",
                    "model": DEFAULT_ASSISTANT_MODEL,
                    "choices": [{
                        "message": {
                            "role": "assistant",
                            "tool_calls": [{
                                "id": "call_bad",
                                "type": "function",
                                "function": {
                                    "name": "launch_builder",
                                    "arguments": json.dumps({"path": "C:/StrategyQuantX/sqcli.exe"}),
                                },
                            }],
                        },
                    }],
                }).encode()
            captured.append(payload)
            return 200, _completion("I cannot launch StrategyQuant X.")

        with patch("tradercockpit.sqx_gateway.SqxNativeControlGateway.launch_builder", side_effect=AssertionError("assistant launched SQX")):
            status, payload = assistant_reply({"message": "start builder"}, environ=self.ENV, transport=transport)
        self.assertEqual(status, 200)
        self.assertEqual(payload["tools_used"], [])
        tool_result = json.loads(captured[1]["messages"][-1]["content"])
        self.assertEqual(tool_result["error"], "unknown_tool")

        path_calls = []

        def path_transport(_url, body, _headers):
            payload = json.loads(body)
            path_calls.append(payload)
            if payload["messages"][-1]["role"] != "tool":
                return 200, json.dumps({
                    "id": "gen-path",
                    "model": DEFAULT_ASSISTANT_MODEL,
                    "choices": [{
                        "message": {
                            "role": "assistant",
                            "tool_calls": [{
                                "id": "call_path",
                                "type": "function",
                                "function": {
                                    "name": "retrieve_quant_guild",
                                    "arguments": json.dumps({"query": "sharpe", "path": "C:/outside"}),
                                },
                            }],
                        },
                    }],
                }).encode()
            return 200, _completion("query only")

        status, payload = assistant_reply({"message": "sharpe"}, environ=self.ENV, transport=path_transport)
        self.assertEqual(status, 200)
        self.assertEqual(payload["tools_used"], [])
        path_result = json.loads(path_calls[1]["messages"][-1]["content"])
        self.assertEqual(path_result["error"], "invalid_arguments")


class AssistantHttpBoundaryTests(unittest.TestCase):
    def _server(self, root: Path):
        web = root / "web"
        web.mkdir(parents=True)
        (web / "index.html").write_text("<main>TraderCockpit</main>", encoding="utf-8")
        store = FileResearchCustodyStore(root / "data")
        server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(web, None, None, store))
        Thread(target=server.serve_forever, daemon=True).start()
        return server

    def test_assistant_routes(self):
        with TemporaryDirectory() as tmp:
            server = self._server(Path(tmp))
            base = f"http://127.0.0.1:{server.server_address[1]}"
            try:
                with patch.dict("os.environ", {OPENROUTER_API_KEY_ENV: "sk-or-test"}, clear=False):
                    with urlopen(f"{base}/api/assistant") as response:
                        status = json.loads(response.read())
                    self.assertEqual(status["schema"], ASSISTANT_STATUS_SCHEMA)
                    self.assertEqual(status["status"], "ready")
                    self.assertEqual(status["knowledge"]["status"], "ready")
                    self.assertEqual(status["tools"]["approved"], ["retrieve_quant_guild"])
                    self.assertFalse(status["tools"]["native_mutation"])

                    with patch("tradercockpit.assistant._urllib_transport", return_value=(200, _completion("Custody is bound."))) as transport:
                        request = Request(f"{base}/api/assistant", data=json.dumps({"message": "Is custody bound?"}).encode(), headers={"content-type": "application/json"}, method="POST")
                        with urlopen(request) as response:
                            reply = json.loads(response.read())
                    self.assertEqual(reply["schema"], ASSISTANT_REPLY_SCHEMA)
                    self.assertEqual(reply["reply"], "Custody is bound.")
                    self.assertIn(reply["knowledge"]["state"], {"grounded", "idle"})
                    sent = json.loads(transport.call_args.args[1])
                    self.assertIn("research_catalog_counts", sent["messages"][0]["content"])
                    self.assertIn("Quant-Guild", sent["messages"][0]["content"])
                    self.assertEqual(sent["tools"][0]["function"]["name"], "retrieve_quant_guild")
                    self.assertEqual(sent["messages"][-1]["content"], "Is custody bound?")
                    self.assertEqual(reply["tools_used"], [])

                    bad = Request(f"{base}/api/assistant", data=b"{}", headers={"content-type": "application/json"}, method="POST")
                    with self.assertRaises(HTTPError) as failure:
                        urlopen(bad)
                    self.assertEqual(failure.exception.code, 400)
            finally:
                server.shutdown()
                server.server_close()


if __name__ == "__main__":
    unittest.main()
