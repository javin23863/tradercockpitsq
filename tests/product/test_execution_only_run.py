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
from tradercockpit.engine import EvaluatorDescriptorV1, execute_backtest
from tradercockpit.storage import FileObjectStore, FileRunLifecycleStore


class ExecutionOnlyEvaluator:
    def __init__(self, build):
        self._descriptor = EvaluatorDescriptorV1(
            build.ref,
            ("tc.strategy.rules.v1",),
            "tc.backtest.result.v1",
            True,
        )

    @property
    def descriptor(self):
        return self._descriptor

    def validate_strategy(self, strategy):
        return None

    def evaluate(self, inputs):
        return ResultArtifactV1(
            inputs.run.ref,
            inputs.engine_build.ref,
            self._descriptor.result_schema,
            {"metrics": {"profit_factor": Decimal("1.25")}},
        )


class ExecutionOnlyRunTests(unittest.TestCase):
    def test_execution_completes_without_claiming_validation(self):
        with tempfile.TemporaryDirectory() as tmp:
            store = FileObjectStore(tmp)
            lifecycle = FileRunLifecycleStore(tmp)
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
                "UTC",
                "fixture",
                "2026-01-01T00:00:00Z",
                "2026-01-02T00:00:00Z",
                "none",
            )
            execution = ExecutionSpecV1(
                Decimal("100000"),
                "USD",
                (ExecutionModelV1("fill", "bar-close", {}),),
            )
            build = EngineBuildSpecV1("tradercockpit", "r1", "a" * 64)
            run = BacktestRunSpecV1(candidate.ref, data.ref, execution.ref, build.ref)
            for item in (strategy, candidate, data, execution, build, run):
                store.put(item)

            result = execute_backtest(
                run.ref,
                store,
                lifecycle,
                ExecutionOnlyEvaluator(build),
                invocation_id="execution-001",
                issued_at="2026-01-02T00:00:00Z",
            )

            event = lifecycle.current(run.ref, "execution-001")
            self.assertEqual(event.status, "completed")
            self.assertTrue(event.terminal)
            self.assertEqual(event.receipt_ref, result.receipt_ref)
            self.assertEqual(event.result_ref, result.result_ref)
            self.assertIsNone(event.decision_ref)
            self.assertIsNone(event.evidence_manifest_ref)
            self.assertIsNone(event.reason_code)
            self.assertEqual(store.resolve(result.result_ref).run_ref, run.ref)


if __name__ == "__main__":
    unittest.main()
