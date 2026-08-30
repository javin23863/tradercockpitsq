from decimal import Decimal
import json
from pathlib import Path
import tempfile
import unittest

from tradercockpit.data_context import (
    DataTradingContextConfigV1,
    DataTradingContextServiceV1,
    DataTradingContextStateError,
)
from tradercockpit.domain import DataSpecV1, ExecutionSpecV1


class DataTradingContextTests(unittest.TestCase):
    def config(self, **overrides):
        values = {
            "symbol": "ES",
            "timeframe": "1m",
            "source": "local-fixture",
            "dataset_revision": "rev-2026-08-31",
            "timezone_name": "America/Chicago",
            "session_calendar": "CME",
            "start": "2026-01-01T00:00:00Z",
            "end": "2026-02-01T00:00:00Z",
            "adjustment_policy": "none",
            "starting_cash": Decimal("100000"),
            "currency": "USD",
            "fill_model": "bar-close",
        }
        values.update(overrides)
        return DataTradingContextConfigV1(**values)

    def test_create_persists_existing_canonical_specs_and_reopens_exact_identity(self):
        with tempfile.TemporaryDirectory() as tmp:
            service = DataTradingContextServiceV1(tmp)
            created = service.create(self.config())
            self.assertEqual(created.ref.kind, "data-trading-context")
            self.assertIsInstance(service.store.resolve(created.data.ref), DataSpecV1)
            self.assertIsInstance(service.store.resolve(created.execution.ref), ExecutionSpecV1)

            reopened = DataTradingContextServiceV1(tmp).read(created.ref)
            self.assertEqual(reopened.ref, created.ref)
            self.assertEqual(reopened.data.ref, created.data.ref)
            self.assertEqual(reopened.execution.ref, created.execution.ref)
            record = reopened.record()
            self.assertEqual(record["authority"]["market_and_dataset_identity"], "user-supplied")
            self.assertEqual(record["authority"]["execution_assumptions"], "tradercockpit-owned")
            self.assertFalse(record["authority"]["native_sqx_binding"])

    def test_same_context_is_idempotent_and_listed_once(self):
        with tempfile.TemporaryDirectory() as tmp:
            service = DataTradingContextServiceV1(tmp)
            first = service.create(self.config())
            second = service.create(self.config())
            self.assertEqual(first.ref, second.ref)
            self.assertEqual([item.ref for item in service.list()], [first.ref])

    def test_dataset_or_execution_assumption_changes_context_identity(self):
        with tempfile.TemporaryDirectory() as tmp:
            service = DataTradingContextServiceV1(tmp)
            baseline = service.create(self.config())
            different_dataset = service.create(self.config(dataset_revision="rev-2"))
            different_cash = service.create(self.config(starting_cash=Decimal("50000")))
            self.assertNotEqual(baseline.data.ref, different_dataset.data.ref)
            self.assertNotEqual(baseline.ref, different_dataset.ref)
            self.assertEqual(baseline.data.ref, different_cash.data.ref)
            self.assertNotEqual(baseline.execution.ref, different_cash.execution.ref)
            self.assertNotEqual(baseline.ref, different_cash.ref)

    def test_request_parser_rejects_binary_float_and_missing_producer_identity(self):
        base = {
            "symbol": "ES",
            "timeframe": "1m",
            "source": "local-fixture",
            "datasetRevision": "rev-1",
            "timezone": "America/Chicago",
            "sessionCalendar": "CME",
            "start": "2026-01-01T00:00:00Z",
            "end": "2026-02-01T00:00:00Z",
        }
        with self.assertRaisesRegex(ValueError, "startingCash"):
            DataTradingContextConfigV1.from_request({**base, "startingCash": 100000.0})
        missing = dict(base)
        del missing["datasetRevision"]
        with self.assertRaisesRegex(ValueError, "datasetRevision"):
            DataTradingContextConfigV1.from_request(missing)

    def test_tampered_composite_catalog_fails_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            service = DataTradingContextServiceV1(tmp)
            created = service.create(self.config())
            path = Path(tmp) / "data-trading-contexts" / f"{created.ref.sha256}.json"
            payload = json.loads(path.read_text())
            payload["data_ref"] = str(service.create(self.config(dataset_revision="other")).data.ref)
            path.write_text(json.dumps(payload, sort_keys=True, separators=(",", ":")))
            with self.assertRaises(DataTradingContextStateError):
                DataTradingContextServiceV1(tmp).read(created.ref)


if __name__ == "__main__":
    unittest.main()
