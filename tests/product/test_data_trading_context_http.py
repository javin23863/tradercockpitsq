import tempfile
import unittest

from tradercockpit.data_context import (
    DATA_CONTEXTS_API_PATH,
    DATA_CONTEXT_READ_API_PATH,
    data_context_create_response,
    data_context_http_get_response,
    data_context_http_post_response,
    data_context_list_response,
)


class DataTradingContextHttpTests(unittest.TestCase):
    def request(self):
        return {
            "symbol": "ES",
            "timeframe": "1m",
            "source": "local-fixture",
            "datasetRevision": "rev-1",
            "timezone": "America/Chicago",
            "sessionCalendar": "CME",
            "start": "2026-01-01T00:00:00Z",
            "end": "2026-02-01T00:00:00Z",
            "startingCash": "100000",
            "currency": "USD",
            "fillModel": "bar-close",
        }

    def test_create_list_read_http_contract(self):
        with tempfile.TemporaryDirectory() as tmp:
            status, created = data_context_http_post_response(
                tmp, DATA_CONTEXTS_API_PATH, self.request()
            )
            self.assertEqual(status, 201)
            self.assertEqual(created["schema"], "tc.data-trading-context.v1")

            status, listed = data_context_http_get_response(
                tmp, DATA_CONTEXTS_API_PATH, {}
            )
            self.assertEqual(status, 200)
            self.assertEqual(
                [item["context_ref"] for item in listed["contexts"]],
                [created["context_ref"]],
            )

            status, reopened = data_context_http_get_response(
                tmp,
                DATA_CONTEXT_READ_API_PATH,
                {"contextRef": [created["context_ref"]]},
            )
            self.assertEqual(status, 200)
            self.assertEqual(reopened, created)

    def test_adapter_falls_through_for_other_product_authorities(self):
        with tempfile.TemporaryDirectory() as tmp:
            self.assertIsNone(data_context_http_get_response(tmp, "/api/run-read", {}))
            self.assertIsNone(
                data_context_http_post_response(tmp, "/api/builder-searches", {})
            )

    def test_query_cardinality_and_unknown_fields_fail_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            status, payload = data_context_http_get_response(
                tmp, DATA_CONTEXT_READ_API_PATH, {"contextRef": ["a", "b"]}
            )
            self.assertEqual(status, 400)
            self.assertEqual(payload["error"], "invalid_request")

            status, payload = data_context_http_get_response(
                tmp, DATA_CONTEXTS_API_PATH, {"unexpected": ["x"]}
            )
            self.assertEqual(status, 400)
            self.assertEqual(payload["error"], "invalid_request")

    def test_missing_state_root_and_missing_context_are_truthful(self):
        status, payload = data_context_list_response(None)
        self.assertEqual(status, 503)
        self.assertEqual(payload["error"], "producer_not_configured")

        with tempfile.TemporaryDirectory() as tmp:
            status, created = data_context_create_response(tmp, self.request())
            self.assertEqual(status, 201)
            fake = created["context_ref"][:-1] + (
                "0" if created["context_ref"][-1] != "0" else "1"
            )
            status, payload = data_context_http_get_response(
                tmp, DATA_CONTEXT_READ_API_PATH, {"contextRef": [fake]}
            )
            self.assertEqual(status, 404)
            self.assertEqual(payload["error"], "not_found")


if __name__ == "__main__":
    unittest.main()
