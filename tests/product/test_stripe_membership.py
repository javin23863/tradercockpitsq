from __future__ import annotations

from http.client import HTTPConnection
from http.server import ThreadingHTTPServer
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Thread
import unittest
from unittest.mock import patch

from tradercockpit.app_server import make_handler
from tradercockpit.consumer_account import begin_google_oauth, complete_google_oauth
from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.runtime_status import runtime_status_record
from tradercockpit.stripe_membership import (
    BILLING_CHECKOUT_PATH,
    begin_stripe_checkout,
    complete_stripe_checkout,
    membership_status_record,
    stripe_membership_path,
)


class StripeMembershipTests(unittest.TestCase):
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

    def test_missing_stripe_env_reports_checkout_not_configured(self) -> None:
        with TemporaryDirectory() as tmp:
            record = membership_status_record(Path(tmp), self._google_environ())
        self.assertEqual(record["reason_code"], "checkout_not_configured")
        self.assertEqual(record["price_amount_cents"], 15000)

    def test_signed_out_reports_not_signed_in_when_stripe_configured(self) -> None:
        with TemporaryDirectory() as tmp:
            record = membership_status_record(Path(tmp), self._stripe_environ())
        self.assertEqual(record["reason_code"], "not_signed_in")
        self.assertEqual(record["checkout_path"], BILLING_CHECKOUT_PATH)

    def test_checkout_create_uses_fake_price_and_never_leaks_secret(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            environ = self._stripe_environ()
            account_id = self._sign_in(root, environ)
            captured: dict[str, object] = {}

            def transport(method, path, headers, data):
                captured["method"] = method
                captured["path"] = path
                captured["headers"] = headers
                captured["data"] = data
                return 200, json.dumps({"url": "https://checkout.stripe.com/c/pay/cs_test_123"}).encode()

            with patch.dict("os.environ", environ, clear=False):
                url = begin_stripe_checkout(root, environ, transport=transport)
            self.assertEqual(url, "https://checkout.stripe.com/c/pay/cs_test_123")
            self.assertEqual(captured["method"], "POST")
            self.assertEqual(captured["path"], "/checkout/sessions")
            headers = captured["headers"]
            self.assertTrue(str(headers.get("Authorization", "")).startswith("Bearer sk_test_fake"))
            body = captured["data"]
            self.assertIsInstance(body, bytes)
            encoded = body.decode("utf-8")
            self.assertIn("price_fake_150", encoded)
            self.assertIn(account_id.replace(":", "%3A"), encoded)
            self.assertNotIn("sk_test_fake", encoded)
            record = membership_status_record(root, environ)
            dumped = json.dumps(record)
            self.assertNotIn("sk_test_fake", dumped)

    def test_complete_checkout_persists_secret_free_membership(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            environ = self._stripe_environ()
            account_id = self._sign_in(root, environ)

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
                            "subscription": {
                                "id": "sub_test",
                                "status": "active",
                                "current_period_end": 1893456000,
                            },
                        }
                    ).encode()
                raise AssertionError(f"unexpected stripe call {method} {path}")

            with patch.dict("os.environ", environ, clear=False):
                record = complete_stripe_checkout(root, "cs_test_123", environ, transport=transport)
            stored = json.loads(stripe_membership_path(root).read_text(encoding="utf-8"))
            self.assertEqual(stored["account_id"], account_id)
            self.assertEqual(stored["stripe_customer_id"], "cus_test")
            self.assertEqual(stored["stripe_subscription_id"], "sub_test")
            self.assertEqual(stored["status"], "active")
            self.assertIn("current_period_end", stored)
            self.assertNotIn("sk_test_fake", json.dumps(stored))
            self.assertEqual(record["status"], "active")
            status = membership_status_record(root, environ)
            self.assertEqual(status["status"], "ready")
            self.assertEqual(status["membership_status"], "active")

    def test_runtime_status_includes_membership(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            environ = self._stripe_environ()
            self._sign_in(root, environ)
            with patch.dict("os.environ", environ, clear=False):
                payload = runtime_status_record(data_root=root)
            self.assertIn("membership", payload)
            self.assertEqual(payload["membership"]["reason_code"], "inactive")

    def test_unsigned_checkout_post_returns_account_not_signed_in(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            web = root / "web"
            web.mkdir()
            (web / "index.html").write_text("<main>TraderCockpit</main>", encoding="utf-8")
            store = FileResearchCustodyStore(root / "data")
            server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(web, None, None, store))
            thread = Thread(target=server.serve_forever, daemon=True)
            thread.start()
            try:
                with patch.dict("os.environ", self._stripe_environ(), clear=False):
                    conn = HTTPConnection("127.0.0.1", server.server_port, timeout=2)
                    conn.request("POST", "/api/account/billing")
                    response = conn.getresponse()
                    body = json.loads(response.read().decode("utf-8"))
                    self.assertEqual(response.status, 401)
                    self.assertEqual(body["reason_code"], "account_not_signed_in")
                    self.assertNotIn("sk_test_fake", json.dumps(body))
                    conn.close()
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

    def test_checkout_get_redirects_to_stripe_hosted_checkout(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            web = root / "web"
            web.mkdir()
            (web / "index.html").write_text("<main>TraderCockpit</main>", encoding="utf-8")
            store = FileResearchCustodyStore(root / "data")
            begin_google_oauth(store.root, self._stripe_environ())
            state = json.loads((store.root / "google-oauth.json").read_text(encoding="utf-8"))["oauth_state"]
            complete_google_oauth(
                store.root,
                "auth-code",
                state,
                self._stripe_environ(),
                transport=lambda *_args: (200, json.dumps({"access_token": "access-token"}).encode()),
                userinfo_transport=lambda *_args: (200, json.dumps({"sub": "subject-1", "email": "user@example.com"}).encode()),
            )
            server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(web, None, None, store))
            thread = Thread(target=server.serve_forever, daemon=True)
            thread.start()
            try:
                with patch.dict("os.environ", self._stripe_environ(), clear=False):
                    with patch(
                        "tradercockpit.stripe_membership.begin_stripe_checkout",
                        return_value="https://checkout.stripe.com/c/pay/cs_test_abc",
                    ):
                        conn = HTTPConnection("127.0.0.1", server.server_port, timeout=2)
                        conn.request("GET", BILLING_CHECKOUT_PATH)
                        response = conn.getresponse()
                        location = response.getheader("location") or ""
                        self.assertEqual(response.status, 302)
                        self.assertEqual(location, "https://checkout.stripe.com/c/pay/cs_test_abc")
                        conn.close()
            finally:
                server.shutdown()
                server.server_close()
                thread.join()


if __name__ == "__main__":
    unittest.main()
