from decimal import Decimal
from http.server import ThreadingHTTPServer
import json
from pathlib import Path
import tempfile
from threading import Thread
import unittest
from urllib.error import HTTPError
from urllib.request import urlopen

from tradercockpit.account import (
    AccountStateEventV1,
    AccountStateV1,
    ModelPolicyV1,
    VerifiedGoogleIdentity,
)
from tradercockpit.app_server import account_state_response, make_handler
from tradercockpit.storage import FileAccountStateStore


class AccountServerTests(unittest.TestCase):
    def setup_account(self, root):
        identity = VerifiedGoogleIdentity(
            "https://accounts.google.com",
            "stable-subject",
            "consumer@example.test",
        )
        policy = ModelPolicyV1("workhorse-v1", "z-ai/glm-5.3-flash")
        state = AccountStateV1(
            identity.account_subject,
            True,
            "starter",
            Decimal("2"),
            Decimal("0.25"),
            policy.policy_id,
            identity.email,
        )
        event = AccountStateEventV1(
            "account_created",
            "2026-08-31T03:00:00Z",
            state,
        )
        store = FileAccountStateStore(root)
        store.publish(event)
        return identity, policy, store, event

    def test_account_read_fails_closed_without_session_or_dependencies(self):
        status, payload = account_state_response(None, None, None)
        self.assertEqual(status, 401)
        self.assertEqual(payload["error"], "not_authenticated")

        identity = VerifiedGoogleIdentity("https://accounts.google.com", "stable-subject")
        status, payload = account_state_response(None, identity.account_subject, None)
        self.assertEqual(status, 503)
        self.assertEqual(payload["error"], "account_state_not_configured")

        with tempfile.TemporaryDirectory() as tmp:
            store = FileAccountStateStore(tmp)
            status, payload = account_state_response(store, identity.account_subject, None)
            self.assertEqual(status, 503)
            self.assertEqual(payload["error"], "model_policy_not_configured")

    def test_account_read_returns_backend_owned_policy_and_allowance(self):
        with tempfile.TemporaryDirectory() as tmp:
            identity, policy, store, event = self.setup_account(tmp)
            status, payload = account_state_response(store, identity.account_subject, policy)

            self.assertEqual(status, 200)
            self.assertEqual(payload["subject"], identity.account_subject)
            self.assertEqual(payload["allowance"]["limit"], "2")
            self.assertEqual(payload["allowance"]["used"], "0.25")
            self.assertEqual(payload["allowance"]["remaining"], "1.75")
            self.assertEqual(payload["model_policy"]["default_model"], "z-ai/glm-5.3-flash")
            self.assertEqual(payload["state_event"]["event_id"], event.event_id)

    def test_http_account_read_does_not_accept_browser_supplied_subject(self):
        with tempfile.TemporaryDirectory() as state_tmp, tempfile.TemporaryDirectory() as web_tmp:
            identity, policy, store, _ = self.setup_account(state_tmp)
            Path(web_tmp, "index.html").write_text("<main>TraderCockpit</main>", encoding="utf-8")
            server = ThreadingHTTPServer(
                ("127.0.0.1", 0),
                make_handler(
                    Path(web_tmp),
                    None,
                    account_store=store,
                    active_account_subject=identity.account_subject,
                    model_policy=policy,
                ),
            )
            thread = Thread(target=server.serve_forever, daemon=True)
            thread.start()
            base = f"http://127.0.0.1:{server.server_port}"
            try:
                with urlopen(f"{base}/api/account-state") as response:
                    payload = json.loads(response.read())
                self.assertEqual(payload["subject"], identity.account_subject)

                with self.assertRaises(HTTPError) as raised:
                    urlopen(f"{base}/api/account-state?subject=attacker-controlled")
                self.assertEqual(raised.exception.code, 400)
            finally:
                server.shutdown()
                server.server_close()
                thread.join()


if __name__ == "__main__":
    unittest.main()
