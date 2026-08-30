from decimal import Decimal
import unittest

from tradercockpit.domain import (
    BacktestRunSpecV1,
    CandidateSpecV1,
    DataSpecV1,
    EngineBuildSpecV1,
    ExecutionModelV1,
    ExecutionSpecV1,
    StrategySpecV1,
)
from tradercockpit.engine import (
    BacktestInputsV1,
    EngineContractError,
    resolve_backtest_inputs,
)


class DictResolver:
    def __init__(self, *objects):
        self.objects = {obj.ref: obj for obj in objects}

    def resolve(self, ref):
        return self.objects[ref]


class LyingResolver:
    def __init__(self, wrong):
        self.wrong = wrong

    def resolve(self, ref):
        return self.wrong


class EngineContractTests(unittest.TestCase):
    def fixture(self):
        strategy = StrategySpecV1(
            "tc.strategy.rules.v1",
            {"entry": {"kind": "always"}, "exit": {"bars": 1}},
        )
        candidate = CandidateSpecV1(strategy.ref, "manual")
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
        execution = ExecutionSpecV1(
            Decimal("100000"),
            "USD",
            (ExecutionModelV1("fill", "bar-close", {}),),
        )
        build = EngineBuildSpecV1("tradercockpit", "r1", "a" * 64)
        run = BacktestRunSpecV1(candidate.ref, data.ref, execution.ref, build.ref)
        return strategy, candidate, data, execution, build, run

    def test_resolves_exact_run_custody(self):
        strategy, candidate, data, execution, build, run = self.fixture()
        resolved = resolve_backtest_inputs(
            run, DictResolver(strategy, candidate, data, execution, build)
        )
        self.assertEqual(resolved.run.ref, run.ref)
        self.assertEqual(resolved.strategy.ref, strategy.ref)

    def test_missing_object_fails_closed(self):
        strategy, candidate, data, execution, build, run = self.fixture()
        with self.assertRaisesRegex(EngineContractError, "missing immutable spec"):
            resolve_backtest_inputs(run, DictResolver(strategy, candidate, data, build))

    def test_type_confused_resolution_fails_closed(self):
        strategy, candidate, data, execution, build, run = self.fixture()
        with self.assertRaisesRegex(
            EngineContractError, "candidate resolved to DataSpecV1"
        ):
            resolve_backtest_inputs(run, LyingResolver(data))

    def test_stale_strategy_substitution_fails_closed(self):
        strategy, candidate, data, execution, build, run = self.fixture()
        stale = StrategySpecV1(
            "tc.strategy.rules.v1",
            {"entry": {"kind": "never"}, "exit": {"bars": 1}},
        )
        resolver = DictResolver(candidate, data, execution, build)
        resolver.objects[candidate.strategy_ref] = stale
        with self.assertRaisesRegex(
            EngineContractError, "strategy content address mismatch"
        ):
            resolve_backtest_inputs(run, resolver)

    def test_bundle_constructor_rejects_cross_run_data(self):
        strategy, candidate, data, execution, build, run = self.fixture()
        other_data = DataSpecV1(
            "NQ",
            "1m",
            "fixture",
            "rev-1",
            "America/Chicago",
            "CME",
            "2025-01-01T00:00:00Z",
            "2025-01-02T00:00:00Z",
            "none",
        )
        with self.assertRaisesRegex(EngineContractError, "data does not match"):
            BacktestInputsV1(run, candidate, strategy, other_data, execution, build)

    def test_bundle_constructor_rejects_candidate_strategy_swap(self):
        strategy, candidate, data, execution, build, run = self.fixture()
        other_strategy = StrategySpecV1(
            "tc.strategy.rules.v1",
            {"entry": {"kind": "never"}, "exit": {"bars": 1}},
        )
        with self.assertRaisesRegex(EngineContractError, "strategy does not match"):
            BacktestInputsV1(
                run, candidate, other_strategy, data, execution, build
            )


if __name__ == "__main__":
    unittest.main()
