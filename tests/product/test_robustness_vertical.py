from decimal import Decimal
import tempfile
import unittest

from tradercockpit.domain import (
    BacktestRunSpecV1,
    CandidateSpecV1,
    DataSpecV1,
    EngineBuildSpecV1,
    ExecutionModelV1,
    ExecutionSpecV1,
    ResultArtifactV1,
    StrategySpecV1,
)
from tradercockpit.robustness import (
    ROBUSTNESS_RESULT_SCHEMA,
    RandomlySkipTradesConfig,
    RobustnessError,
    RobustnessExecutionUnavailable,
    RobustnessMetricGateV1,
    RobustnessPlanV1,
    RobustnessServiceV1,
    SystemParameterPermutationSettings,
)
from tradercockpit.robustness.api import robustness_start_response
from tradercockpit.storage import FileObjectStore


class RobustnessVerticalTests(unittest.TestCase):
    def source_result(self, root, *, include_trades=True):
        store = FileObjectStore(root)
        strategy = StrategySpecV1(
            "tc.strategy.rules.v1",
            {"entry": {"kind": "always"}, "exit": {"bars": 1}},
        )
        candidate = CandidateSpecV1(strategy.ref, "manual")
        data = DataSpecV1(
            "ES", "1m", "fixture", "rev-1", "America/Chicago", "CME",
            "2025-01-01T00:00:00Z", "2025-01-02T00:00:00Z", "none",
        )
        execution = ExecutionSpecV1(
            Decimal("100000"), "USD", (ExecutionModelV1("fill", "bar-close", {}),),
        )
        build = EngineBuildSpecV1("fixture-engine", "r1", "a" * 64)
        run = BacktestRunSpecV1(candidate.ref, data.ref, execution.ref, build.ref)
        payload = {"metrics": {"profit_factor": Decimal("1.4")}}
        if include_trades:
            payload["trades"] = (
                {"id": "t1", "pnl": Decimal("10")},
                {"id": "t2", "pnl": Decimal("-4")},
                {"id": "t3", "pnl": Decimal("7")},
                {"id": "t4", "pnl": Decimal("-2")},
            )
        result = ResultArtifactV1(run.ref, build.ref, "tc.backtest.result.v1", payload)
        for value in (strategy, candidate, data, execution, build, run, result):
            store.put(value)
        return result

    def plan(self, source_ref):
        return RobustnessPlanV1(
            source_ref,
            trials=8,
            random_seed=17,
            randomize_trades_order=True,
            randomly_skip_trades=RandomlySkipTradesConfig(25),
            gates=(RobustnessMetricGateV1("net_pnl", "gte", Decimal("0")),),
        )

    def test_trade_monte_carlo_uses_real_source_trades_and_reopens_exact_result(self):
        with tempfile.TemporaryDirectory() as tmp:
            source = self.source_result(tmp)
            service = RobustnessServiceV1(tmp)
            result = service.run(self.plan(source.ref))

            self.assertEqual(result.result_schema, ROBUSTNESS_RESULT_SCHEMA)
            self.assertEqual(result.payload["source_result_ref"], str(source.ref))
            self.assertEqual(result.payload["summary"]["source_trade_count"], 4)
            self.assertEqual(result.payload["summary"]["trial_count"], 8)
            self.assertEqual(len(result.payload["trials"]), 8)
            source_ids = {"t1", "t2", "t3", "t4"}
            for trial in result.payload["trials"]:
                self.assertTrue(set(trial["trade_ids"]).issubset(source_ids))
                self.assertEqual(trial["trade_count"], 3)
                self.assertIsInstance(trial["net_pnl"], Decimal)
                self.assertIsInstance(trial["max_drawdown"], Decimal)

            reopened = RobustnessServiceV1(tmp)
            self.assertEqual(reopened.read(result.ref).ref, result.ref)
            self.assertEqual(
                tuple(item.ref for item in reopened.list_for_source(source.ref)),
                (result.ref,),
            )

    def test_same_source_plan_and_code_reproduce_result_identity(self):
        refs = []
        payloads = []
        for _ in range(2):
            with tempfile.TemporaryDirectory() as tmp:
                source = self.source_result(tmp)
                result = RobustnessServiceV1(tmp).run(self.plan(source.ref))
                refs.append(result.ref)
                payloads.append(result.payload)
        self.assertEqual(refs[0], refs[1])
        self.assertEqual(payloads[0], payloads[1])

    def test_missing_trade_evidence_fails_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            source = self.source_result(tmp, include_trades=False)
            with self.assertRaisesRegex(RobustnessError, "trades"):
                RobustnessServiceV1(tmp).run(self.plan(source.ref))

    def test_system_parameter_permutation_never_creates_second_execution_pipeline(self):
        with tempfile.TemporaryDirectory() as tmp:
            source = self.source_result(tmp)
            plan = RobustnessPlanV1(
                source.ref,
                trials=1,
                randomize_trades_order=False,
                system_parameter_permutation=SystemParameterPermutationSettings(max_tests=2),
            )
            service = RobustnessServiceV1(tmp)
            with self.assertRaisesRegex(RobustnessExecutionUnavailable, "canonical execution-only"):
                service.run(plan)
            self.assertEqual(service.catalog.list(source.ref), ())

    def test_api_returns_real_result_and_explicit_unavailable_parameter_execution(self):
        with tempfile.TemporaryDirectory() as tmp:
            source = self.source_result(tmp)
            status, body = robustness_start_response(
                tmp,
                {
                    "sourceResultRef": str(source.ref),
                    "config": {
                        "trials": 3,
                        "randomSeed": 5,
                        "randomizeTradesOrder": True,
                        "randomlySkipTradesProbabilityPct": 25,
                        "filters": [{"metric": "net_pnl", "operator": "gte", "threshold": "0"}],
                    },
                },
            )
            self.assertEqual(status, 201)
            self.assertEqual(body["resultSchema"], ROBUSTNESS_RESULT_SCHEMA)
            self.assertEqual(body["payload"]["summary"]["trial_count"], 3)

            status, body = robustness_start_response(
                tmp,
                {
                    "sourceResultRef": str(source.ref),
                    "config": {
                        "trials": 1,
                        "randomizeTradesOrder": False,
                        "systemParameterPermutation": {"enabled": True, "maxTests": 2},
                    },
                },
            )
            self.assertEqual(status, 409)
            self.assertEqual(body["error"], "execution_unavailable")


if __name__ == "__main__":
    unittest.main()
