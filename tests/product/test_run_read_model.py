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
    InitialValidationPlanV1,
    MetricGateV1,
    ResultArtifactV1,
    RunLifecycleEventV1,
    StrategySpecV1,
)
from tradercockpit.engine import (
    EngineContractError,
    EvaluatorDescriptorV1,
    execute_initial_backtest,
    load_initial_run_read_model,
)
from tradercockpit.storage import FileObjectStore, FileRunLifecycleStore


class ReadEvaluator:
    def __init__(self, build_ref, profit_factor):
        self._descriptor = EvaluatorDescriptorV1(
            build_ref,
            ("tc.strategy.rules.v1",),
            "tc.backtest.result.v1",
            True,
        )
        self.profit_factor = profit_factor

    @property
    def descriptor(self):
        return self._descriptor

    def validate_strategy(self, strategy):
        return None

    def evaluate(self, inputs):
        return ResultArtifactV1(
            inputs.run.ref,
            inputs.engine_build.ref,
            "tc.backtest.result.v1",
            {"metrics": {"profit_factor": self.profit_factor, "trades": 12}},
        )


class FakeLifecycle:
    def __init__(self, event):
        self.event = event

    def publish(self, event):
        self.event = event
        return event.ref

    def current(self, run_ref, invocation_id):
        return self.event


class InitialRunReadModelTests(unittest.TestCase):
    def setup_run(self, root, *, exit_bars=1):
        store = FileObjectStore(root)
        lifecycle = FileRunLifecycleStore(root)
        strategy = StrategySpecV1(
            "tc.strategy.rules.v1",
            {"entry": {"kind": "always"}, "exit": {"bars": exit_bars}},
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
        for value in (strategy, candidate, data, execution, build, run):
            store.put(value)
        return store, lifecycle, run, build

    def plan(self):
        return InitialValidationPlanV1(
            "tc.backtest.result.v1",
            (MetricGateV1("metrics.profit_factor", "gt", Decimal("1.30")),),
        )

    def execute(self, store, lifecycle, run, build, profit_factor):
        return execute_initial_backtest(
            run.ref,
            store,
            lifecycle,
            ReadEvaluator(build.ref, profit_factor),
            self.plan(),
            invocation_id="initial-001",
            issued_at="2025-01-02T00:00:00Z",
        )

    def test_passed_read_model_resolves_exact_chain(self):
        with tempfile.TemporaryDirectory() as tmp:
            store, lifecycle, run, build = self.setup_run(tmp)
            execution = self.execute(store, lifecycle, run, build, Decimal("1.50"))
            model = load_initial_run_read_model(
                run.ref,
                "initial-001",
                store,
                lifecycle,
            )
            self.assertEqual(model.status, "passed")
            self.assertTrue(model.terminal)
            self.assertEqual(model.inputs.run.ref, run.ref)
            self.assertEqual(model.inputs.candidate.ref, run.candidate_ref)
            self.assertEqual(model.inputs.strategy.ref, model.inputs.candidate.strategy_ref)
            self.assertEqual(model.inputs.data.ref, run.data_ref)
            self.assertEqual(model.inputs.execution.ref, run.execution_ref)
            self.assertEqual(model.inputs.engine_build.ref, run.engine_build_ref)
            self.assertEqual(model.result.ref, execution.result_ref)
            self.assertEqual(model.decision.ref, execution.decision_ref)
            self.assertTrue(model.decision.passed)
            self.assertEqual(
                model.evidence_manifest.ref,
                execution.evidence_manifest_ref,
            )

    def test_validation_rejection_remains_failed_with_real_evidence(self):
        with tempfile.TemporaryDirectory() as tmp:
            store, lifecycle, run, build = self.setup_run(tmp)
            execution = self.execute(store, lifecycle, run, build, Decimal("1.00"))
            model = load_initial_run_read_model(
                run.ref,
                "initial-001",
                store,
                lifecycle,
            )
            self.assertEqual(model.status, "failed")
            self.assertEqual(model.lifecycle_event.reason_code, "validation_rejected")
            self.assertFalse(model.decision.passed)
            self.assertEqual(model.evidence_manifest.ref, execution.evidence_manifest_ref)

    def test_missing_lifecycle_state_is_not_inferred_from_run_artifacts(self):
        with tempfile.TemporaryDirectory() as tmp:
            store, lifecycle, run, _ = self.setup_run(tmp)
            with self.assertRaisesRegex(EngineContractError, "no lifecycle state"):
                load_initial_run_read_model(
                    run.ref,
                    "initial-001",
                    store,
                    lifecycle,
                )

    def test_forged_passed_status_over_failing_decision_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            store, lifecycle, run, build = self.setup_run(tmp)
            execution = self.execute(store, lifecycle, run, build, Decimal("1.00"))
            failed = lifecycle.current(run.ref, "initial-001")
            forged = RunLifecycleEventV1(
                run.ref,
                "initial-001",
                "passed",
                "2025-01-02T00:00:00Z",
                previous_event_ref=failed.previous_event_ref,
                receipt_ref=execution.receipt_ref,
                result_ref=execution.result_ref,
                decision_ref=execution.decision_ref,
                evidence_manifest_ref=execution.evidence_manifest_ref,
            )
            with self.assertRaisesRegex(EngineContractError, "passing evidence chain"):
                load_initial_run_read_model(
                    run.ref,
                    "initial-001",
                    store,
                    FakeLifecycle(forged),
                )

    def test_cross_run_result_substitution_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            store, lifecycle, run, build = self.setup_run(tmp, exit_bars=1)
            execution = self.execute(store, lifecycle, run, build, Decimal("1.50"))
            current = lifecycle.current(run.ref, "initial-001")

            other_strategy = StrategySpecV1(
                "tc.strategy.rules.v1",
                {"entry": {"kind": "always"}, "exit": {"bars": 2}},
            )
            other_candidate = CandidateSpecV1(other_strategy.ref, "manual")
            other_run = BacktestRunSpecV1(
                other_candidate.ref,
                run.data_ref,
                run.execution_ref,
                run.engine_build_ref,
            )
            other_result = ResultArtifactV1(
                other_run.ref,
                build.ref,
                "tc.backtest.result.v1",
                {"metrics": {"profit_factor": Decimal("9")}},
            )
            for value in (other_strategy, other_candidate, other_run, other_result):
                store.put(value)

            forged = RunLifecycleEventV1(
                run.ref,
                "initial-001",
                "failed",
                "2025-01-02T00:00:00Z",
                previous_event_ref=current.previous_event_ref,
                receipt_ref=execution.receipt_ref,
                result_ref=other_result.ref,
                reason_code="validation_error",
            )
            with self.assertRaisesRegex(EngineContractError, "result belongs to another run"):
                load_initial_run_read_model(
                    run.ref,
                    "initial-001",
                    store,
                    FakeLifecycle(forged),
                )


if __name__ == "__main__":
    unittest.main()
