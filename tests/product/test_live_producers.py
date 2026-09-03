from __future__ import annotations

import json
from unittest.mock import patch
import unittest

from tradercockpit.live_producers import (
    LIVE_PRODUCERS_SCHEMA,
    METATRADER_MCP_TOKEN_ENV,
    METATRADER_MCP_URL_ENV,
    SQX_MCP_TOKEN_ENV,
    SQX_MCP_TOOLS,
    SQX_MCP_URL_ENV,
    TRADINGVIEW_MCP_TOKEN_ENV,
    TRADINGVIEW_MCP_URL_ENV,
    live_producers_record,
)
from tradercockpit.runtime_status import runtime_status_record


class LiveProducersTests(unittest.TestCase):
    def test_unconfigured_producers_are_secret_free_and_not_live(self) -> None:
        with patch.dict(
            "os.environ",
            {
                TRADINGVIEW_MCP_URL_ENV: "",
                TRADINGVIEW_MCP_TOKEN_ENV: "",
                METATRADER_MCP_URL_ENV: "",
                METATRADER_MCP_TOKEN_ENV: "",
                SQX_MCP_URL_ENV: "",
                SQX_MCP_TOKEN_ENV: "",
            },
            clear=False,
        ):
            payload = live_producers_record()

        self.assertEqual(payload["schema"], LIVE_PRODUCERS_SCHEMA)
        self.assertEqual(payload["status"], "unavailable")
        self.assertEqual(payload["tradingview"]["id"], "tradingview")
        self.assertEqual(payload["metatrader"]["id"], "metatrader")
        self.assertEqual(payload["tradingview"]["purpose"], "apollo_llm_tool")
        self.assertEqual(payload["metatrader"]["purpose"], "apollo_llm_tool")
        self.assertEqual(payload["strategyquant_mcp"]["purpose"], "native_custom_project_control")
        self.assertEqual(payload["strategyquant_mcp"]["native_tools"], list(SQX_MCP_TOOLS))
        self.assertIn("Apollo/LLM tool", payload["tradingview"]["job"])
        self.assertIn("Apollo/LLM tools", payload["detail"])
        self.assertIn("Custom Project control", payload["detail"])
        self.assertFalse(payload["tradingview"]["endpoint_configured"])
        self.assertFalse(payload["tradingview"]["live_quotes"])
        self.assertFalse(payload["metatrader"]["live_positions"])
        self.assertFalse(payload["metatrader"]["live_pnl"])
        self.assertEqual(payload["tradingview"]["reason_code"], "mcp_url_not_configured")
        encoded = json.dumps(payload)
        self.assertNotIn("secret", encoded.lower())
        self.assertNotIn("sk-", encoded)

    def test_configured_endpoint_does_not_claim_a_handshake_or_echo_url(self) -> None:
        with patch.dict(
            "os.environ",
            {
                TRADINGVIEW_MCP_URL_ENV: "https://tv.example.invalid/mcp",
                TRADINGVIEW_MCP_TOKEN_ENV: "tv-secret-token",
                METATRADER_MCP_URL_ENV: "https://mt5.example.invalid/mcp",
                METATRADER_MCP_TOKEN_ENV: "mt-secret-token",
                SQX_MCP_URL_ENV: "https://sqx.example.invalid/mcp",
                SQX_MCP_TOKEN_ENV: "sqx-secret-token",
            },
            clear=False,
        ):
            payload = live_producers_record()
            encoded = json.dumps(runtime_status_record(None))

        self.assertTrue(payload["tradingview"]["endpoint_configured"])
        self.assertTrue(payload["tradingview"]["credential_configured"])
        self.assertEqual(payload["tradingview"]["reason_code"], "mcp_transport_unverified")
        self.assertFalse(payload["tradingview"]["live_quotes"])
        self.assertEqual(payload["strategyquant_mcp"]["reason_code"], "mcp_transport_unverified")
        self.assertNotIn("tv.example.invalid", encoded)
        self.assertNotIn("tv-secret-token", encoded)
        self.assertNotIn("mt-secret-token", encoded)
        self.assertNotIn("sqx-secret-token", encoded)
        self.assertIn("live_producers", encoded)

    def test_invalid_url_fails_closed_without_echoing_the_value(self) -> None:
        with patch.dict("os.environ", {TRADINGVIEW_MCP_URL_ENV: "file:///etc/passwd"}, clear=False):
            payload = live_producers_record()
            encoded = json.dumps(payload)
        self.assertEqual(payload["tradingview"]["reason_code"], "mcp_url_invalid")
        self.assertFalse(payload["tradingview"]["endpoint_configured"])
        self.assertNotIn("passwd", encoded)
        self.assertNotIn("file://", encoded)


if __name__ == "__main__":
    unittest.main()
