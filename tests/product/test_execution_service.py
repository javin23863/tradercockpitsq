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
from tradercockpit.engine.evaluator import EvaluatorDescriptorV1
from tradercockpit.engine.execution_service import execute_backtest
from tradercockpit.storage import FileObjectStore, FileRunLifecycleStore


class ExecutionEvaluator:
    def __init__(self, build, *, fail=False):
        self.fail = fail
        self._descriptor = EvaluatorDescriptorV1(
            build.ref,
            ("tc.strategy.rules.v1",),
            "tc.execution-result.v1",
            True,
        )

    @property
    def descriptor(self):
        return self._descriptor

    def validate_strategy(self, strategy):
        return None

    def evaluate(self, inputs):
        if self.fail:
            raise RuntimeError("producer failed")
        return ResultArtifactV1(
            inputs.run.ref,
            inputs.engine_build.ref,
            self.descriptor.result_schema,
            {"producer": {"exit_code": 0}, "result": {"archive_bytes": 123}},
        )


class ExecutionServiceTests(unittest.TestCase):
    def setup_run(self, root):
        store = FileObjectStore(root)
        lifecycle = FileRunLifecycleStore(root)
        strategy = StrategySpecV1("tc.strategy.rules.v1", {"entry": "fixture"})
        candidate = CandidateSpecV1(strategy.ref, "manual")
        data = DataSpecV1(
            "ES", "H1", "fixture", "rev-1", "UTC", "CME",
            "2025-01-01T00:00:00Z", "2025-02-01T00:00:00Z", "none",
        )
        execution = ExecutionSpecV1(
            Decimal("100000"), "USD",
            (ExecutionModelV1("fills", "fixture", {}),),
        )
        build = EngineBuildSpecV1("fixture-engine", "r1", "a" * 64)
        run = BacktestRunSpecV1(candidate.ref, data.ref, execution.ref, build.ref)
        for item in (strategy, candidate, data, execution, build, run):
            store.put(item)
        return store, lifecycle, run, build

    def test_success_ends_completed_without_validation_artifacts(self):
        with tempfile.TemporaryDirectory() as tmp:
            store, lifecycle, run, build = self.setup_run(tmp)
            result = execute_backtest(
                run.ref,
                store,
                lifecycle,
                ExecutionEvaluator(build),
                invocation_id="exec-001",
                issued_at="2025-02-01T00:00:00Z",
            )
            event = lifecycle.current(run.ref, "exec-001")
            self.assertEqual(event.status, "completed")
            self.assertTrue(event.terminal)
            self.assertEqual(event.receipt_ref, result.receipt_ref)
            self.assertEqual(event.result_ref, result.result_ref)
            self.assertIsNone(event.decision_ref)
            self.assertIsNone(event.evidence_manifest_ref)
            self.assertEqual(store.resolve(result.result_ref).payload["producer"]["exit_code"], 0)

    def test_producer_failure_is_failed_after_durable_receipt(self):
        with tempfile.TemporaryDirectory() as tmp:
            store, lifecycle, run, build = self.setup_run(tmp)
            with self.assertRaisesRegex(RuntimeError, "producer failed"):
                execute_backtest(
                    run.ref,
                    store,
                    lifecycle,
                    ExecutionEvaluator(build, fail=True),
                    invocation_id="exec-002",
                    issued_at="2025-02-01T00:00:00Z",
                )
            event = lifecycle.current(run.ref, "exec-002")
            self.assertEqual(event.status, "failed")
            self.assertEqual(event.reason_code, "evaluation_failed")
            self.assertIsNotNone(event.receipt_ref)
            self.assertIsNone(event.result_ref)


if __name__ == "__main__":
    unittest.main()
