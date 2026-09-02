from __future__ import annotations

import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

from tradercockpit.assistant import OPENROUTER_API_KEY_ENV, assistant_status_record, request_completion
from tradercockpit.consumer_account import begin_google_oauth, complete_google_oauth
from tradercockpit.openrouter_credits import (
    OPENROUTER_MANAGEMENT_KEY_ENV,
    credits_status_record,
    openrouter_credits_path,
    provision_consumer_key,
    refresh_consumer_key_usage,
)
from tradercockpit.runtime_status import runtime_status_record
from tradercockpit.stripe_membership import complete_stripe_checkout


class OpenRouterCreditsTests(unittest.TestCase):
    def _google_environ(self) -> dict[str, str]:
        return {
            "GOOGLE_CLIENT_ID": "google-client",
            "GOOGLE_CLIENT_SECRET": "google-secret",
            "GOOGLE_REDIRECT_URI": "http://127.0.0.1:4173/api/account/google/callback",
        }

    def _stripe_environ(self) -> dict[str, str]:
        return {
            **self._google_environ(),
            "STRIPE_SECRET_KEY": "sk_test_fake",
            "STRIPE_PRICE_ID": "price_fake_150",
            "STRIPE_SUCCESS_URL": "http://127.0.0.1:4173/api/account/billing/success?session_id={CHECKOUT_SESSION_ID}",
            "STRIPE_CANCEL_URL": "http://127.0.0.1:4173/api/account/billing/cancel",
        }

    def _credits_environ(self) -> dict[str, str]:
        return {
            **self._stripe_environ(),
            OPENROUTER_MANAGEMENT_KEY_ENV: "sk-or-mgmt-test",
        }

    def _sign_in(self, root: Path, environ: dict[str, str]) -> str:
        begin_google_oauth(root, environ)
        state = json.loads((root / "google-oauth.json").read_text(encoding="utf-8"))["oauth_state"]
        complete_google_oauth(
            root,
            "auth-code",
            state,
            environ,
            transport=lambda *_args: (200, json.dumps({"access_token": "access-token"}).encode()),
            userinfo_transport=lambda *_args: (200, json.dumps({"sub": "google-subject", "email": "consumer@example.com"}).encode()),
        )
        session = json.loads((root / "google-session.json").read_text(encoding="utf-8"))
        return str(session["account_id"])

    def _activate_membership(self, root: Path, account_id: str, environ: dict[str, str]) -> None:
        def transport(method, path, _headers, _data):
            if method == "GET" and path.startswith("/checkout/sessions/"):
                return 200, json.dumps(
                    {
                        "id": "cs_test_123",
                        "status": "complete",
                        "payment_status": "paid",
                        "customer": "cus_test",
                        "client_reference_id": account_id,
                        "metadata": {"account_id": account_id},
                        "subscription": {"id": "sub_test", "status": "active", "current_period_end": 1893456000},
                    }
                ).encode()
            raise AssertionError(f"unexpected stripe call {method} {path}")

        complete_stripe_checkout(root, "cs_test_123", environ, transport=transport)

    def test_missing_management_key_reports_provision_not_configured(self) -> None:
        with TemporaryDirectory() as tmp:
            record = credits_status_record(Path(tmp), self._stripe_environ())
        self.assertEqual(record["reason_code"], "provision_not_configured")
        self.assertFalse(record["provider_enforced"])

    def test_signed_out_reports_not_signed_in(self) -> None:
        with TemporaryDirectory() as tmp:
            record = credits_status_record(Path(tmp), self._credits_environ())
        self.assertEqual(record["reason_code"], "not_signed_in")

    def test_inactive_membership_reports_membership_inactive(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            environ = self._credits_environ()
            self._sign_in(root, environ)
            record = credits_status_record(root, environ)
        self.assertEqual(record["reason_code"], "membership_inactive")

    def test_provision_creates_secret_free_status_and_stores_key_server_side(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            environ = self._credits_environ()
            account_id = self._sign_in(root, environ)
            self._activate_membership(root, account_id, environ)

            def transport(method, path, headers, body):
                if method == "POST":
                    self.assertEqual(path, "")
                    self.assertTrue(str(headers.get("Authorization", "")).startswith("Bearer sk-or-mgmt-test"))
                    payload = json.loads(body.decode("utf-8"))
                    self.assertEqual(payload["limit"], 30)
                    self.assertEqual(payload["limit_reset"], "monthly")
                    return 200, json.dumps(
                        {
                            "key": "sk-or-consumer-test",
                            "data": {"hash": "abc123hash", "limit": 30, "limit_remaining": 30, "usage": 0},
                        }
                    ).encode()
                if method == "GET" and path == "/abc123hash":
                    return 200, json.dumps({"data": {"hash": "abc123hash", "limit_remaining": 30, "usage": 0}}).encode()
                raise AssertionError(f"unexpected {method} {path}")

            record = provision_consumer_key(root, environ, transport=transport)
            stored = json.loads(openrouter_credits_path(root).read_text(encoding="utf-8"))
            self.assertEqual(stored["account_id"], account_id)
            self.assertEqual(stored["key_hash"], "abc123hash")
            self.assertEqual(stored["api_key"], "sk-or-consumer-test")
            status = credits_status_record(root, environ, transport=transport)
            dumped = json.dumps(status)
            self.assertNotIn("sk-or-consumer-test", dumped)
            self.assertNotIn("sk-or-mgmt-test", dumped)
            self.assertTrue(status["provider_enforced"])
            self.assertEqual(status["status"], "ready")
            self.assertEqual(record["key_hash"], "abc123hash")

    def test_refresh_reads_provider_usage_without_leaking_keys(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            environ = self._credits_environ()
            account_id = self._sign_in(root, environ)
            self._activate_membership(root, account_id, environ)

            def transport(method, path, _headers, body):
                if method == "POST":
                    return 200, json.dumps({"key": "sk-or-consumer-test", "data": {"hash": "abc123hash", "limit_remaining": 30, "usage": 0}}).encode()
                if method == "GET" and path == "/abc123hash":
                    return 200, json.dumps({"data": {"hash": "abc123hash", "limit_remaining": 22.5, "usage": 7.5}}).encode()
                raise AssertionError(f"unexpected {method} {path}")

            provision_consumer_key(root, environ, transport=transport)
            refreshed = refresh_consumer_key_usage(root, environ, transport=transport)
            self.assertIsNotNone(refreshed)
            assert refreshed is not None
            self.assertEqual(refreshed["limit_remaining"], 22.5)
            self.assertEqual(refreshed["usage"], 7.5)

    def test_assistant_uses_consumer_key_with_provider_enforced_spend(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            environ = self._credits_environ()
            account_id = self._sign_in(root, environ)
            self._activate_membership(root, account_id, environ)
            openrouter_credits_path(root).write_text(
                json.dumps(
                    {
                        "schema": "tc.openrouter-credits.v1",
                        "account_id": account_id,
                        "key_hash": "abc123hash",
                        "api_key": "sk-or-consumer-test",
                        "limit_usd": 30,
                        "limit_reset": "monthly",
                    }
                ),
                encoding="utf-8",
            )
            status = assistant_status_record(environ, data_root=root)
            self.assertEqual(status["credential_scope"], "consumer")
            self.assertTrue(status["spend_boundary"]["provider_enforced"])
            calls = []

            def transport(url, body, headers):
                calls.append(headers["Authorization"])
                return 200, json.dumps(
                    {
                        "id": "gen-1",
                        "model": "z-ai/glm-5.3-flash",
                        "choices": [{"message": {"role": "assistant", "content": "ok"}}],
                        "usage": {"prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2},
                    }
                ).encode()

            result = request_completion([{"role": "user", "content": "hi"}], environ=environ, transport=transport, data_root=root)
            self.assertEqual(result["reply"], "ok")
            self.assertEqual(calls[0], "Bearer sk-or-consumer-test")

    def test_operator_key_remains_fallback_without_consumer_path(self) -> None:
        with patch.dict("os.environ", {OPENROUTER_API_KEY_ENV: "sk-or-operator"}, clear=False):
            status = assistant_status_record({})
        self.assertEqual(status["credential_scope"], "operator")
        self.assertFalse(status["spend_boundary"]["provider_enforced"])

    def test_runtime_status_includes_model_credits(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            environ = self._credits_environ()
            self._sign_in(root, environ)
            with patch.dict("os.environ", environ, clear=False):
                payload = runtime_status_record(data_root=root)
            self.assertIn("model_credits", payload)
            self.assertEqual(payload["model_credits"]["reason_code"], "membership_inactive")


if __name__ == "__main__":
    unittest.main()
