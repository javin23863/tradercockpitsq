from __future__ import annotations

import json
import os
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

from tradercockpit.market_data import market_provider_from_env, market_quotes_record
from tradercockpit.runtime_status import runtime_status_record
from tradercockpit.secrets_store import (
    TRADERCOCKPIT_SECRETS_PATH_ENV,
    SecretsStoreError,
    apply_operator_secrets,
    load_secrets_file,
    parse_dotenv,
    secrets_status_record,
)


class SecretsStoreTests(unittest.TestCase):
    def test_parse_dotenv_ignores_comments_and_export_prefix(self) -> None:
        parsed = parse_dotenv(
            "# comment\n"
            "export OPENROUTER_API_KEY=sk-or-from-file\n"
            "GOOGLE_CLIENT_ID=google-id\n"
            'STRIPE_SECRET_KEY="sk_test_quoted"\n'
        )
        self.assertEqual(parsed["OPENROUTER_API_KEY"], "sk-or-from-file")
        self.assertEqual(parsed["GOOGLE_CLIENT_ID"], "google-id")
        self.assertEqual(parsed["STRIPE_SECRET_KEY"], "sk_test_quoted")

    def test_explicit_missing_store_fails_closed(self) -> None:
        with TemporaryDirectory() as tmp:
            missing = Path(tmp) / "missing.env"
            with self.assertRaises(SecretsStoreError) as ctx:
                load_secrets_file(missing)
            self.assertEqual(ctx.exception.code, "secrets_file_missing")

            with patch.dict(
                os.environ,
                {TRADERCOCKPIT_SECRETS_PATH_ENV: str(missing)},
                clear=False,
            ):
                with self.assertRaises(SecretsStoreError):
                    apply_operator_secrets()
                status = secrets_status_record()
            self.assertEqual(status["store"]["status"], "unavailable")
            self.assertEqual(status["store"]["reason_code"], "secrets_file_missing")

    def test_apply_does_not_override_existing_process_environment(self) -> None:
        with TemporaryDirectory() as tmp:
            secrets_path = Path(tmp) / "keys.env"
            secrets_path.write_text(
                "OPENROUTER_API_KEY=sk-or-from-file\n"
                "GOOGLE_CLIENT_ID=google-from-file\n",
                encoding="utf-8",
            )
            with patch.dict(
                os.environ,
                {
                    TRADERCOCKPIT_SECRETS_PATH_ENV: str(secrets_path),
                    "OPENROUTER_API_KEY": "sk-or-process",
                },
                clear=False,
            ):
                result = apply_operator_secrets()
                self.assertEqual(result["loaded"], 1)
                self.assertEqual(os.environ["OPENROUTER_API_KEY"], "sk-or-process")
                self.assertEqual(os.environ["GOOGLE_CLIENT_ID"], "google-from-file")

    def test_status_json_never_dumps_secret_values(self) -> None:
        secret = "sk-or-status-leak-test"
        with TemporaryDirectory() as tmp:
            secrets_path = Path(tmp) / "keys.env"
            secrets_path.write_text(f"OPENROUTER_API_KEY={secret}\n", encoding="utf-8")
            with patch.dict(
                os.environ,
                {TRADERCOCKPIT_SECRETS_PATH_ENV: str(secrets_path)},
                clear=False,
            ):
                apply_operator_secrets()
                status = secrets_status_record()
                runtime = runtime_status_record(research_store_bound=True, data_root=tmp)

        encoded = json.dumps({"secrets": status, "runtime": runtime}, sort_keys=True)
        self.assertNotIn(secret, encoded)
        self.assertNotIn("sk-or", encoded)
        openrouter = next(group for group in status["groups"] if group["id"] == "openrouter")
        self.assertEqual(openrouter["status"], "configured")

    def test_unconfigured_store_reports_not_configured_groups(self) -> None:
        with patch.dict(os.environ, {}, clear=True):
            status = secrets_status_record({})
        self.assertEqual(status["store"]["status"], "not_configured")
        self.assertTrue(all(group["status"] == "not_configured" for group in status["groups"]))

    def test_desktop_keys_env_enables_schwab_oauth_not_invented_quotes(self) -> None:
        with TemporaryDirectory() as tmp:
            secrets_path = Path(tmp) / "keys.env"
            secrets_path.write_text(
                "SCHWAB_CLIENT_ID=cid-from-file\n"
                "SCHWAB_CLIENT_SECRET=csecret-from-file\n"
                "SCHWAB_CALLBACK_URL=https://127.0.0.1:8182/callback\n",
                encoding="utf-8",
            )
            with patch.dict(os.environ, {TRADERCOCKPIT_SECRETS_PATH_ENV: str(secrets_path)}, clear=True):
                apply_operator_secrets()
                status = secrets_status_record()
                schwab = next(group for group in status["groups"] if group["id"] == "schwab")
                self.assertEqual(schwab["status"], "configured")
                self.assertIsNone(market_provider_from_env())
                quotes = market_quotes_record(None, ())
                self.assertEqual(quotes["reason_code"], "provider_not_configured")
                self.assertEqual(quotes["provider_hookup"]["authorize_path"], "/api/market/schwab/authorize")
                encoded = json.dumps(quotes)
                self.assertNotIn("csecret-from-file", encoded)
                self.assertIsNone(quotes.get("provider"))


if __name__ == "__main__":
    unittest.main()
