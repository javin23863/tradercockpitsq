from decimal import Decimal
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
    SpecValidationError,
    StrategySpecV1,
    ValidationDecisionV1,
    evaluate_initial_validation,
)


class InitialValidationTests(unittest.TestCase):
    def run_context(self):
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
            "2025-02-01T00:00:00Z",
            "none",
        )
        execution = ExecutionSpecV1(
            Decimal("100000"),
            "USD",
            (ExecutionModelV1("fill", "bar-close", {}),),
        )
        build = EngineBuildSpecV1("tradercockpit", "r1", "a" * 64)
        run = BacktestRunSpecV1(candidate.ref, data.ref, execution.ref, build.ref)
        return run, build

    def plan(self):
        return InitialValidationPlanV1(
            "tc.backtest.result.v1",
            (
                MetricGateV1(
                    "metrics.profit_factor",
                    "gt",
                    Decimal("1.3"),
                ),
                MetricGateV1("metrics.ret_dd", "gt", Decimal("4")),
                MetricGateV1(
                    "metrics.trades_per_month",
                    "gt",
                    Decimal("2"),
                ),
            ),
        )

    def result(self, *, pf="1.5", ret_dd="5", trades=3):
        run, build = self.run_context()
        return ResultArtifactV1(
            run.ref,
            build.ref,
            "tc.backtest.result.v1",
            {
                "metrics": {
                    "profit_factor": Decimal(pf),
                    "ret_dd": Decimal(ret_dd),
                    "trades_per_month": trades,
                }
            },
        )

    def test_known_good_metrics_pass_all_gates(self):
        decision = evaluate_initial_validation(self.plan(), self.result())
        self.assertTrue(decision.passed)
        self.assertTrue(all(item.passed for item in decision.outcomes))

    def test_one_failed_gate_produces_real_failure(self):
        decision = evaluate_initial_validation(self.plan(), self.result(pf="1.2"))
        self.assertFalse(decision.passed)
        failed = [item.metric_path for item in decision.outcomes if not item.passed]
        self.assertEqual(failed, ["metrics.profit_factor"])

    def test_gate_order_does_not_change_plan_identity(self):
        gates = self.plan().gates
        reversed_plan = InitialValidationPlanV1(
            "tc.backtest.result.v1",
            tuple(reversed(gates)),
        )
        self.assertEqual(self.plan().ref, reversed_plan.ref)

    def test_duplicate_gate_is_rejected(self):
        gate = MetricGateV1(
            "metrics.profit_factor",
            "gt",
            Decimal("1.3"),
        )
        with self.assertRaisesRegex(SpecValidationError, "duplicates"):
            InitialValidationPlanV1(
                "tc.backtest.result.v1",
                (gate, gate),
            )

    def test_result_schema_mismatch_fails_closed(self):
        result = self.result()
        other_plan = InitialValidationPlanV1(
            "tc.backtest.other.v1",
            (
                MetricGateV1(
                    "metrics.profit_factor",
                    "gt",
                    Decimal("1"),
                ),
            ),
        )
        with self.assertRaisesRegex(SpecValidationError, "result schema"):
            evaluate_initial_validation(other_plan, result)

    def test_missing_or_non_numeric_metric_fails_closed(self):
        run, build = self.run_context()
        missing = ResultArtifactV1(
            run.ref,
            build.ref,
            "tc.backtest.result.v1",
            {"metrics": {"ret_dd": Decimal("5")}},
        )
        with self.assertRaisesRegex(
            SpecValidationError,
            "missing validation metric",
        ):
            evaluate_initial_validation(self.plan(), missing)
        bad = ResultArtifactV1(
            run.ref,
            build.ref,
            "tc.backtest.result.v1",
            {
                "metrics": {
                    "profit_factor": "1.5",
                    "ret_dd": Decimal("5"),
                    "trades_per_month": 3,
                }
            },
        )
        with self.assertRaisesRegex(
            SpecValidationError,
            "must be int or finite Decimal",
        ):
            evaluate_initial_validation(self.plan(), bad)

    def test_decision_cannot_claim_pass_when_an_outcome_failed(self):
        decision = evaluate_initial_validation(
            self.plan(),
            self.result(pf="1.2"),
        )
        with self.assertRaisesRegex(SpecValidationError, "conjunction"):
            ValidationDecisionV1(
                decision.plan_ref,
                decision.result_ref,
                True,
                decision.outcomes,
            )

    def test_receipt_freezes_run_build_invocation_and_canonical_time(self):
        run, build = self.run_context()
        receipt = RunReceiptV1(
            run.ref,
            build.ref,
            "run-001",
            "2025-01-01T19:00:00-05:00",
        )
        self.assertEqual(receipt.issued_at, "2025-01-02T00:00:00.000000Z")
        self.assertNotEqual(
            receipt.ref,
            RunReceiptV1(
                run.ref,
                build.ref,
                "run-002",
                receipt.issued_at,
            ).ref,
        )

    def test_evidence_manifest_is_order_invariant_and_duplicate_safe(self):
        result = self.result()
        decision = evaluate_initial_validation(self.plan(), result)
        run, build = self.run_context()
        receipt = RunReceiptV1(
            run.ref,
            build.ref,
            "run-001",
            "2025-01-01T00:00:00Z",
        )
        left = EvidenceManifestV1(
            run.ref,
            (receipt.ref, result.ref, decision.ref),
        )
        right = EvidenceManifestV1(
            run.ref,
            (decision.ref, receipt.ref, result.ref),
        )
        self.assertEqual(left.ref, right.ref)
        with self.assertRaisesRegex(SpecValidationError, "duplicates"):
            EvidenceManifestV1(run.ref, (result.ref, result.ref))


if __name__ == "__main__":
    unittest.main()
