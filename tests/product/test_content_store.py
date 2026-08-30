from decimal import Decimal
from pathlib import Path
import tempfile
import unittest

from tradercockpit.domain import (
    BacktestRunSpecV1,
    CandidateSpecV1,
    DataSpecV1,
    EngineBuildSpecV1,
    ExecutionModelV1,
    ExecutionSpecV1,
    StrategySpecV1,
    canonical_json_bytes,
    canonical_json_loads,
)
from tradercockpit.engine import resolve_backtest_inputs
from tradercockpit.storage import (
    ContentStoreError,
    FileObjectStore,
    WireFormatError,
    decode_addressed_object,
    encode_addressed_object,
)


class ContentStoreTests(unittest.TestCase):
    def objects(self):
        strategy = StrategySpecV1(
            "tc.strategy.rules.v1",
            {
                "entry": {"kind": "always"},
                "exit": {"bars": 1},
                "threshold": Decimal("1.25"),
            },
        )
        candidate = CandidateSpecV1(strategy.ref, "manual")
        data = DataSpecV1(
            "ES",
            "7m",
            "fixture",
            "rev-1",
            "America/Chicago",
            "CME",
            "2025-01-01T00:00:00Z",
            "2025-02-01T00:00:00Z",
            "none",
        )
        execution = ExecutionSpecV1(
            Decimal("100000"),
            "USD",
            (
                ExecutionModelV1("slippage", "none", {}),
                ExecutionModelV1("fill", "bar-close", {}),
            ),
        )
        build = EngineBuildSpecV1("tradercockpit", "r1", "a" * 64)
        run = BacktestRunSpecV1(
            candidate.ref,
            data.ref,
            execution.ref,
            build.ref,
            random_seed=7,
        )
        return strategy, candidate, data, execution, build, run

    def test_supported_execution_objects_round_trip_exact_identity(self):
        for value in self.objects():
            with self.subTest(kind=value.KIND):
                encoded = encode_addressed_object(value)
                decoded = decode_addressed_object(encoded)
                self.assertEqual(decoded.ref, value.ref)
                self.assertEqual(encode_addressed_object(decoded), encoded)

    def test_wire_rejects_noncanonical_bytes(self):
        strategy = self.objects()[0]
        encoded = encode_addressed_object(strategy)
        with self.assertRaisesRegex(WireFormatError, "not canonical"):
            decode_addressed_object(encoded + b"\n")

    def test_wire_rejects_declared_ref_tamper(self):
        strategy = self.objects()[0]
        envelope = canonical_json_loads(encode_addressed_object(strategy))
        envelope["ref"] = "tc:strategy:v1:sha256:" + "0" * 64
        tampered = canonical_json_bytes(envelope)
        with self.assertRaisesRegex(WireFormatError, "declared ref"):
            decode_addressed_object(tampered)

    def test_store_resolves_real_backtest_input_chain(self):
        with tempfile.TemporaryDirectory() as tmp:
            store = FileObjectStore(tmp)
            strategy, candidate, data, execution, build, run = self.objects()
            for value in (strategy, candidate, data, execution, build, run):
                self.assertEqual(store.put(value), value.ref)
                self.assertTrue(store.contains(value.ref))

            resolved = resolve_backtest_inputs(run, store)
            self.assertEqual(resolved.strategy.ref, strategy.ref)
            self.assertEqual(resolved.candidate.ref, candidate.ref)
            self.assertEqual(resolved.data.ref, data.ref)
            self.assertEqual(resolved.execution.ref, execution.ref)
            self.assertEqual(resolved.engine_build.ref, build.ref)

    def test_put_is_idempotent_for_same_immutable_object(self):
        with tempfile.TemporaryDirectory() as tmp:
            store = FileObjectStore(tmp)
            strategy = self.objects()[0]
            first = store.put(strategy)
            target = (
                Path(tmp).resolve()
                / "objects"
                / strategy.ref.kind
                / f"v{strategy.ref.version}"
                / f"{strategy.ref.sha256}.json"
            )
            bytes_before = target.read_bytes()
            second = store.put(strategy)
            self.assertEqual(first, second)
            self.assertEqual(target.read_bytes(), bytes_before)

    def test_missing_ref_is_key_error(self):
        with tempfile.TemporaryDirectory() as tmp:
            store = FileObjectStore(tmp)
            strategy = self.objects()[0]
            with self.assertRaises(KeyError):
                store.resolve(strategy.ref)

    def test_wrong_object_bytes_under_ref_path_are_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            store = FileObjectStore(tmp)
            first = self.objects()[2]
            second = DataSpecV1(
                "NQ",
                "7m",
                "fixture",
                "rev-1",
                "America/Chicago",
                "CME",
                "2025-01-01T00:00:00Z",
                "2025-02-01T00:00:00Z",
                "none",
            )
            store.put(first)
            target = (
                Path(tmp).resolve()
                / "objects"
                / first.ref.kind
                / f"v{first.ref.version}"
                / f"{first.ref.sha256}.json"
            )
            target.write_bytes(encode_addressed_object(second))
            with self.assertRaisesRegex(ContentStoreError, "different ref"):
                store.resolve(first.ref)

    def test_corrupt_existing_bytes_are_not_silently_overwritten(self):
        with tempfile.TemporaryDirectory() as tmp:
            store = FileObjectStore(tmp)
            strategy = self.objects()[0]
            store.put(strategy)
            target = (
                Path(tmp).resolve()
                / "objects"
                / strategy.ref.kind
                / f"v{strategy.ref.version}"
                / f"{strategy.ref.sha256}.json"
            )
            target.write_bytes(b"{}")
            with self.assertRaisesRegex(ContentStoreError, "disagree"):
                store.put(strategy)
            self.assertEqual(target.read_bytes(), b"{}")


if __name__ == "__main__":
    unittest.main()
