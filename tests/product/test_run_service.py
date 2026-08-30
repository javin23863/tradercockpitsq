from decimal import Decimal
import tempfile
import unittest

from tradercockpit.domain import (
    BacktestRunSpecV1,
    CandidateSpecV1,
    DataSpecV1,
    EngineBuildSpecV1,
    EvidenceManifestV1,
    ExecutionModelV1,
    ExecutionSpecV1,
    InitialValidationPlanV1,
    MetricGateV1,
    ResultArtifactV1,
    RunReceiptV1,
    StrategySpecV1,
    ValidationDecisionV1,
)
from tradercockpit.engine import (
    EngineContractError,
    EvaluatorDescriptorV1,
    execute_initial_backtest,
)
from tradercockpit.storage import FileObjectStore


class ServiceEvaluator:
    def __init__(self, descriptor, *, fail=False, result_factory=None):
        self._descriptor = descriptor
        self.fail = fail
        self.result_factory = result_factory
        self.calls = 0

    @property
    def descriptor(self):
        return self._descriptor

    def evaluate(self, inputs):
        self.calls += 1
        if self.fail:
            raise RuntimeError("producer failed")
        if self.result_factory is not None:
            return self.result_factory(inputs)
        return ResultArtifactV1(
            inputs.run.ref,
            inputs.engine_build.ref,
            self.descriptor.result_schema,
            {"metrics": {"profit_factor": Decimal("1.50"), "trades": 12}},
        )


class InitialRunServiceTests(unittest.TestCase):
    def setup_store(self, root):
        store = FileObjectStore(root)
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
        for value in (strategy, candidate, data, execution, build, run):
            store.put(value)
        return store, run, build

    def descriptor(self, build, *, result_schema="tc.backtest.result.v1"):
        return EvaluatorDescriptorV1(
            build.ref,
            ("tc.strategy.rules.v1",),
            result_schema,
            True,
        )

    def plan(self, *, result_schema="tc.backtest.result.v1"):
        return InitialValidationPlanV1(
            result_schema,
            (MetricGateV1("metrics.profit_factor", "gt", Decimal("1.30")),),
        )

    def test_success_persists_complete_initial_evidence_chain(self):
        with tempfile.TemporaryDirectory() as tmp:
            store, run, build = self.setup_store(tmp)
            evaluator = ServiceEvaluator(self.descriptor(build))
            plan = self.plan()
            execution = execute_initial_backtest(
                run.ref,
                store,
                evaluator,
                plan,
                invocation_id="initial-001",
                issued_at="2025-01-02T00:00:00Z",
            )

            self.assertEqual(evaluator.calls, 1)
            receipt = store.resolve(execution.receipt_ref)
            result = store.resolve(execution.result_ref)
            stored_plan = store.resolve(execution.plan_ref)
            decision = store.resolve(execution.decision_ref)
            evidence = store.resolve(execution.evidence_manifest_ref)

            self.assertIsInstance(receipt, RunReceiptV1)
            self.assertIsInstance(result, ResultArtifactV1)
            self.assertEqual(stored_plan.ref, plan.ref)
            self.assertIsInstance(decision, ValidationDecisionV1)
            self.assertTrue(decision.passed)
            self.assertIsInstance(evidence, EvidenceManifestV1)
            self.assertEqual(evidence.run_ref, run.ref)
            self.assertEqual(
                set(evidence.evidence_refs),
                {receipt.ref, result.ref, plan.ref, decision.ref},
            )

            reopened = FileObjectStore(tmp)
            self.assertEqual(
                reopened.resolve(execution.evidence_manifest_ref).ref,
                execution.evidence_manifest_ref,
            )

    def test_plan_schema_mismatch_refuses_before_launch_or_persistence(self):
        with tempfile.TemporaryDirectory() as tmp:
            store, run, build = self.setup_store(tmp)
            evaluator = ServiceEvaluator(self.descriptor(build))
            plan = self.plan(result_schema="tc.backtest.other.v1")
            with self.assertRaisesRegex(EngineContractError, "validation plan result schema"):
                execute_initial_backtest(
                    run.ref,
                    store,
                    evaluator,
                    plan,
                    invocation_id="initial-001",
                    issued_at="2025-01-02T00:00:00Z",
                )
            self.assertEqual(evaluator.calls, 0)
            self.assertFalse(store.contains(plan.ref))

    def test_producer_failure_leaves_receipt_but_no_false_result_or_evidence(self):
        with tempfile.TemporaryDirectory() as tmp:
            store, run, build = self.setup_store(tmp)
            evaluator = ServiceEvaluator(self.descriptor(build), fail=True)
            plan = self.plan()
            expected_receipt = RunReceiptV1(
                run.ref,
                build.ref,
                "initial-001",
                "2025-01-02T00:00:00Z",
            )
            with self.assertRaisesRegex(RuntimeError, "producer failed"):
                execute_initial_backtest(
                    run.ref,
                    store,
                    evaluator,
                    plan,
                    invocation_id="initial-001",
                    issued_at="2025-01-02T00:00:00Z",
                )
            self.assertEqual(evaluator.calls, 1)
            self.assertTrue(store.contains(plan.ref))
            self.assertTrue(store.contains(expected_receipt.ref))
            self.assertEqual(store.resolve(expected_receipt.ref).run_ref, run.ref)

    def test_invalid_result_is_not_persisted_as_success(self):
        with tempfile.TemporaryDirectory() as tmp:
            store, run, build = self.setup_store(tmp)
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
            evaluator = ServiceEvaluator(
                self.descriptor(build),
                result_factory=lambda _: ResultArtifactV1(
                    other_run.ref,
                    build.ref,
                    "tc.backtest.result.v1",
                    {"metrics": {"profit_factor": Decimal("9")}},
                ),
            )
            expected_bad_result = evaluator.result_factory(None)
            with self.assertRaisesRegex(EngineContractError, "result run_ref"):
                execute_initial_backtest(
                    run.ref,
                    store,
                    evaluator,
                    self.plan(),
                    invocation_id="initial-001",
                    issued_at="2025-01-02T00:00:00Z",
                )
            self.assertFalse(store.contains(expected_bad_result.ref))


if __name__ == "__main__":
    unittest.main()
