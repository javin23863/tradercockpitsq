from __future__ import annotations

import json
import unittest

from tradercockpit.macro_series import (
    FRED_API_KEY_ENV,
    FRED_PROVIDER_ID,
    MACRO_SERIES_SCHEMA,
    FredSeriesProvider,
    macro_provider_from_env,
    macro_series_record,
    series_ids_from_env,
)


class MacroSeriesTests(unittest.TestCase):
    def test_missing_key_keeps_provider_unconfigured(self) -> None:
        self.assertIsNone(macro_provider_from_env({}))
        self.assertIsNone(macro_provider_from_env({FRED_API_KEY_ENV: "  "}))
        record = macro_series_record(None, ("dgs10",), environ={})
        self.assertEqual(record["schema"], MACRO_SERIES_SCHEMA)
        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "provider_not_configured")
        self.assertEqual(record["series"][0]["id"], "DGS10")
        self.assertIsNone(record["series"][0]["value"])
        self.assertEqual(record["provider_hookup"]["credential_scope"], "operator")

    def test_series_env_deduplicates(self) -> None:
        self.assertEqual(series_ids_from_env({}), ())
        self.assertEqual(
            series_ids_from_env({"TRADERCOCKPIT_FRED_SERIES": " dgs10 , UNRATE ,DGS10,, "}),
            ("DGS10", "UNRATE"),
        )

    def test_observations_come_from_fred_json_without_leaking_the_key(self) -> None:
        calls = []

        def transport(url, headers):
            calls.append(url)
            if "DGS10" in url:
                return 200, json.dumps({"observations": [{"date": "2026-09-01", "value": "4.12"}]}).encode()
            return 200, json.dumps({"observations": [{"date": "2026-08-01", "value": "."}]}).encode()

        provider = FredSeriesProvider("fred-test-key", transport=transport)
        record = macro_series_record(provider, ("DGS10", "UNRATE"), provider_id=provider.provider_id)
        self.assertEqual(record["status"], "current")
        self.assertEqual(record["provider"], {"id": FRED_PROVIDER_ID, "credential_scope": "operator"})
        by_id = {row["id"]: row for row in record["series"]}
        self.assertEqual(by_id["DGS10"]["value"], 4.12)
        self.assertEqual(by_id["DGS10"]["date"], "2026-09-01")
        self.assertEqual(by_id["UNRATE"]["status"], "unavailable")
        dumped = json.dumps(record)
        self.assertNotIn("fred-test-key", dumped)
        self.assertIn("api_key=fred-test-key", calls[0])
        self.assertIn("series_id=DGS10", calls[0])

    def test_rejected_credential_fails_closed(self) -> None:
        def transport(_url, _headers):
            return 403, b'{"error_code": 400}'

        record = macro_series_record(FredSeriesProvider("fred-bad", transport=transport), ("DGS10",))
        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "provider_read_failed")
        self.assertNotIn("fred-bad", json.dumps(record))

    def test_provider_from_env_uses_fred_key(self) -> None:
        provider = macro_provider_from_env({FRED_API_KEY_ENV: "fred-env"})
        self.assertIsInstance(provider, FredSeriesProvider)
        self.assertEqual(provider.provider_id, FRED_PROVIDER_ID)


if __name__ == "__main__":
    unittest.main()
