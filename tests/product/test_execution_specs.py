from decimal import Decimal
import unittest

from tradercockpit.domain import (
    BacktestRunSpecV1,
    CandidateSpecV1,
    DataSpecV1,
    EngineBuildSpecV1,
    ExecutionModelV1,
    ExecutionSpecV1,
    SpecValidationError,
    StrategySpecV1,
)


class ExecutionSpecTests(unittest.TestCase):
    def strategy(self, period: int = 10) -> StrategySpecV1:
        return StrategySpecV1(
            semantic_schema="tc.strategy.rules.v1",
            semantics={
                "entry": {
                    "operator": "greater-than",
                    "left": "close",
                    "period": period,
                },
                "exit": {"kind": "bars", "value": 5},
            },
        )

    def execution(self) -> ExecutionSpecV1:
        return ExecutionSpecV1(
            starting_cash=Decimal("100000.00"),
            currency="USD",
            models=(
                ExecutionModelV1("fill", "bar-close", {}),
                ExecutionModelV1("fees", "none", {}),
                ExecutionModelV1("slippage", "none", {}),
            ),
        )

    def test_strategy_semantics_are_deep_frozen(self):
        raw = {"entry": {"period": 10}, "values": [1, 2]}
        strategy = StrategySpecV1("tc.strategy.rules.v1", raw)
        original = strategy.ref
        raw["entry"]["period"] = 99
        raw["values"].append(3)
        self.assertEqual(strategy.ref, original)
        self.assertEqual(strategy.semantics["entry"]["period"], 10)
        self.assertEqual(strategy.semantics["values"], (1, 2))
        with self.assertRaises(TypeError):
            strategy.semantics["entry"] = {}  # type: ignore[index]

    def test_strategy_identity_changes_with_semantics_or_schema(self):
        a = self.strategy(10)
        b = self.strategy(11)
        c = StrategySpecV1("tc.strategy.rules.v2", a.semantics)
        self.assertNotEqual(a.ref, b.ref)
        self.assertNotEqual(a.ref, c.ref)

    def test_strategy_rejects_unversioned_schema_and_float_semantics(self):
        with self.assertRaises(SpecValidationError):
            StrategySpecV1("rules", {"entry": {"period": 10}})
        with self.assertRaisesRegex(SpecValidationError, "float is not permitted"):
            StrategySpecV1("tc.strategy.rules.v1", {"threshold": 1.5})

    def test_candidate_keeps_lineage_separate_from_strategy_identity(self):
        base = self.strategy(10)
        resolved = self.strategy(12)
        manual = CandidateSpecV1(
            resolved.ref, "manual", parent_strategy_ref=base.ref
        )
        search_origin = CandidateSpecV1(
            resolved.ref,
            "search",
            parent_strategy_ref=base.ref,
            origin_ref=EngineBuildSpecV1("search-engine", "r1", "1" * 64).ref,
        )
        self.assertEqual(manual.strategy_ref, search_origin.strategy_ref)
        self.assertNotEqual(manual.ref, search_origin.ref)

    def test_candidate_rejects_wrong_reference_kind(self):
        data = DataSpecV1(
            "ES",
            "1m",
            "fixture",
            "rev-1",
            "America/Chicago",
            "CME",
            "2025-01-01T00:00:00Z",
            "2025-01-02T00:00:00Z",
            "none",
        )
        with self.assertRaisesRegex(SpecValidationError, "strategy_ref must reference"):
            CandidateSpecV1(data.ref, "manual")

    def test_data_timestamps_are_timezone_canonical_and_identity_equivalent(self):
        zulu = DataSpecV1(
            "ES",
            "7m",
            "fixture",
            "rev-1",
            "America/Chicago",
            "CME",
            "2025-01-01T00:00:00Z",
            "2025-01-02T00:00:00Z",
            "none",
        )
        offset = DataSpecV1(
            "ES",
            "7m",
            "fixture",
            "rev-1",
            "America/Chicago",
            "CME",
            "2024-12-31T19:00:00-05:00",
            "2025-01-01T19:00:00-05:00",
            "none",
        )
        self.assertEqual(zulu.start, "2025-01-01T00:00:00.000000Z")
        self.assertEqual(zulu.ref, offset.ref)

    def test_data_refuses_naive_or_reversed_window(self):
        args = ("ES", "1m", "fixture", "rev-1", "America/Chicago", "CME")
        with self.assertRaisesRegex(SpecValidationError, "explicit timezone"):
            DataSpecV1(
                *args,
                "2025-01-01T00:00:00",
                "2025-01-02T00:00:00Z",
                "none",
            )
        with self.assertRaisesRegex(SpecValidationError, "start must be before end"):
            DataSpecV1(
                *args,
                "2025-01-03T00:00:00Z",
                "2025-01-02T00:00:00Z",
                "none",
            )

    def test_data_window_orders_microseconds_correctly(self):
        args = ("ES", "1m", "fixture", "rev-1", "America/Chicago", "CME")
        valid = DataSpecV1(
            *args,
            "2025-01-01T00:00:00.100000Z",
            "2025-01-01T00:00:00.200000Z",
            "none",
        )
        self.assertLess(valid.start, valid.end)
        with self.assertRaisesRegex(SpecValidationError, "start must be before end"):
            DataSpecV1(
                *args,
                "2025-01-01T00:00:00.200000Z",
                "2025-01-01T00:00:00.100000Z",
                "none",
            )

    def test_execution_models_are_explicit_and_unique(self):
        execution = self.execution()
        self.assertEqual(execution.models[0].kind, "fill")
        with self.assertRaisesRegex(SpecValidationError, "duplicate execution model kind"):
            ExecutionSpecV1(
                Decimal("1000"),
                "USD",
                (
                    ExecutionModelV1("fees", "none", {}),
                    ExecutionModelV1(
                        "fees", "flat", {"amount": Decimal("1")}
                    ),
                ),
            )

    def test_execution_refuses_float_cash_and_float_model_parameters(self):
        with self.assertRaises(SpecValidationError):
            ExecutionSpecV1(  # type: ignore[arg-type]
                1000.0, "USD", (ExecutionModelV1("fill", "bar-close", {}),)
            )
        with self.assertRaisesRegex(SpecValidationError, "float is not permitted"):
            ExecutionModelV1("fees", "flat", {"amount": 1.25})

    def test_backtest_run_binds_all_custody_refs_and_seed(self):
        strategy = self.strategy()
        candidate = CandidateSpecV1(strategy.ref, "manual")
        data = DataSpecV1(
            "ES",
            "1m",
            "fixture",
            "rev-1",
            "America/Chicago",
            "CME",
            "2025-01-01T00:00:00Z",
            "2025-02-01T00:00:00Z",
            "none",
        )
        execution = self.execution()
        engine = EngineBuildSpecV1("tradercockpit", "abc123", "a" * 64)
        run = BacktestRunSpecV1(
            candidate.ref, data.ref, execution.ref, engine.ref, random_seed=7
        )
        same = BacktestRunSpecV1(
            candidate.ref, data.ref, execution.ref, engine.ref, random_seed=7
        )
        other_seed = BacktestRunSpecV1(
            candidate.ref, data.ref, execution.ref, engine.ref, random_seed=8
        )
        self.assertEqual(run.ref, same.ref)
        self.assertNotEqual(run.ref, other_seed.ref)

    def test_backtest_run_rejects_cross_kind_substitution(self):
        strategy = self.strategy()
        candidate = CandidateSpecV1(strategy.ref, "manual")
        data = DataSpecV1(
            "ES",
            "1m",
            "fixture",
            "rev-1",
            "America/Chicago",
            "CME",
            "2025-01-01T00:00:00Z",
            "2025-02-01T00:00:00Z",
            "none",
        )
        execution = self.execution()
        engine = EngineBuildSpecV1("tradercockpit", "abc123", "a" * 64)
        with self.assertRaisesRegex(SpecValidationError, "candidate_ref must reference"):
            BacktestRunSpecV1(data.ref, data.ref, execution.ref, engine.ref)
        with self.assertRaisesRegex(SpecValidationError, "random_seed"):
            BacktestRunSpecV1(
                candidate.ref,
                data.ref,
                execution.ref,
                engine.ref,
                random_seed=-1,
            )

    def test_engine_build_identity_includes_artifact_digest(self):
        a = EngineBuildSpecV1("tradercockpit", "rev", "a" * 64)
        b = EngineBuildSpecV1("tradercockpit", "rev", "b" * 64)
        self.assertNotEqual(a.ref, b.ref)


if __name__ == "__main__":
    unittest.main()
