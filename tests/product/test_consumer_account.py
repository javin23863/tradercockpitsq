from __future__ import annotations

from http.client import HTTPConnection
from http.server import ThreadingHTTPServer
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Thread
import unittest
from unittest.mock import patch

from tradercockpit.app_server import make_handler, status_response
from tradercockpit.consumer_account import (
    GOOGLE_AUTHORIZE_PATH,
    account_status_record,
    begin_google_oauth,
    clear_google_session,
    complete_google_oauth,
    google_callback_uri,
    google_session_path,
)
from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.runtime_status import runtime_status_record


class ConsumerAccountTests(unittest.TestCase):
    def _environ(self) -> dict[str, str]:
        return {
            "GOOGLE_CLIENT_ID": "google-client",
            "GOOGLE_CLIENT_SECRET": "google-secret",
            "GOOGLE_REDIRECT_URI": "http://127.0.0.1:4173/api/account/google/callback",
        }

    def test_non_loopback_redirect_uri_fails_closed(self) -> None:
        with self.assertRaises(ValueError):
            google_callback_uri({"GOOGLE_REDIRECT_URI": "https://example.com/callback"})

    def test_missing_client_credentials_report_provider_not_configured(self) -> None:
        record = account_status_record(Path("/tmp/unused"), {})
        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "provider_not_configured")

    def test_signed_out_includes_authorize_path_when_configured(self) -> None:
        with TemporaryDirectory() as tmp:
            record = account_status_record(Path(tmp), self._environ())
        self.assertEqual(record["reason_code"], "signed_out")
        self.assertEqual(record["authorize_path"], GOOGLE_AUTHORIZE_PATH)

    def test_oauth_round_trip_persists_secret_free_session(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            environ = self._environ()
            location = begin_google_oauth(root, environ)
            self.assertIn("https://accounts.google.com/o/oauth2/v2/auth", location)
            self.assertIn("client_id=google-client", location)
            self.assertNotIn("google-secret", location)
            state = json.loads((root / "google-oauth.json").read_text(encoding="utf-8"))["oauth_state"]

            def token_transport(_url, _headers, data):
                self.assertIn(b"grant_type=authorization_code", data or b"")
                return 200, json.dumps({"access_token": "access-token", "refresh_token": "refresh-token"}).encode()

            def userinfo_transport(_url, headers):
                self.assertEqual(headers["Authorization"], "Bearer access-token")
                return 200, json.dumps({"sub": "google-subject", "email": "consumer@example.com"}).encode()

            complete_google_oauth(
                root,
                "auth-code",
                state,
                environ,
                transport=token_transport,
                userinfo_transport=userinfo_transport,
            )
            session = json.loads(google_session_path(root).read_text(encoding="utf-8"))
            self.assertEqual(session["email"], "consumer@example.com")
            self.assertEqual(session["subject"], "google-subject")
            self.assertTrue(str(session["account_id"]).startswith("sha256:"))
            oauth = json.loads((root / "google-oauth.json").read_text(encoding="utf-8"))
            self.assertEqual(oauth["refresh_token"], "refresh-token")
            self.assertNotIn("oauth_state", oauth)

            record = account_status_record(root, environ)
            self.assertEqual(record["status"], "ready")
            self.assertEqual(record["email"], "consumer@example.com")
            dumped = json.dumps(record)
            self.assertNotIn("access-token", dumped)
            self.assertNotIn("refresh-token", dumped)
            self.assertNotIn("google-secret", dumped)

    def test_runtime_status_account_becomes_ready_with_verified_session(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            environ = self._environ()
            begin_google_oauth(root, environ)
            state = json.loads((root / "google-oauth.json").read_text(encoding="utf-8"))["oauth_state"]

            def token_transport(_url, _headers, _data):
                return 200, json.dumps({"access_token": "access-token"}).encode()

            def userinfo_transport(_url, _headers):
                return 200, json.dumps({"sub": "subject-1", "email": "user@example.com"}).encode()

            complete_google_oauth(
                root,
                "auth-code",
                state,
                environ,
                transport=token_transport,
                userinfo_transport=userinfo_transport,
            )
            with patch.dict("os.environ", environ, clear=False):
                payload = runtime_status_record(data_root=root)
            self.assertEqual(payload["account"]["status"], "ready")
            self.assertEqual(payload["account"]["email"], "user@example.com")

    def test_sign_out_clears_session(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            environ = self._environ()
            begin_google_oauth(root, environ)
            state = json.loads((root / "google-oauth.json").read_text(encoding="utf-8"))["oauth_state"]
            complete_google_oauth(
                root,
                "auth-code",
                state,
                environ,
                transport=lambda *_args: (200, json.dumps({"access_token": "access-token"}).encode()),
                userinfo_transport=lambda *_args: (200, json.dumps({"sub": "subject-1", "email": "user@example.com"}).encode()),
            )
            clear_google_session(root)
            record = account_status_record(root, environ)
            self.assertEqual(record["reason_code"], "signed_out")
            self.assertFalse(google_session_path(root).is_file())

    def test_google_authorize_route_is_loopback_only(self) -> None:
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
                with patch.dict("os.environ", self._environ(), clear=False):
                    conn = HTTPConnection("127.0.0.1", server.server_port, timeout=2)
                    conn.request("GET", GOOGLE_AUTHORIZE_PATH)
                    response = conn.getresponse()
                    location = response.getheader("location") or ""
                    body = response.read()
                    self.assertEqual(response.status, 302)
                    self.assertIn("https://accounts.google.com/o/oauth2/v2/auth", location)
                    self.assertIn("client_id=google-client", location)
                    self.assertNotIn("google-secret", location)
                    self.assertNotIn(b"google-secret", body)
                    conn.close()

                    status, payload = status_response(None, None, store)
                    self.assertEqual(status, 200)
                    self.assertEqual(payload["account"]["reason_code"], "signed_out")
                    self.assertNotIn("refresh-token", json.dumps(payload))
            finally:
                server.shutdown()
                server.server_close()
                thread.join()


if __name__ == "__main__":
    unittest.main()
