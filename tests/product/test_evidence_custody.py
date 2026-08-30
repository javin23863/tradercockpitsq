from decimal import Decimal
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
    RunReceiptV1,
    SpecValidationError,
    StrategySpecV1,
    ValidationDecisionV1,
    build_initial_evidence_manifest,
    evaluate_initial_validation,
)


class EvidenceCustodyTests(unittest.TestCase):
    def context(self, *, seed=None):
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
        run = BacktestRunSpecV1(
            candidate.ref,
            data.ref,
            execution.ref,
            build.ref,
            random_seed=seed,
        )
        plan = InitialValidationPlanV1(
            "tc.backtest.result.v1",
            (MetricGateV1("metrics.profit_factor", "gt", Decimal("1.3")),),
        )
        result = ResultArtifactV1(
            run.ref,
            build.ref,
            "tc.backtest.result.v1",
            {"metrics": {"profit_factor": Decimal("1.5")}},
        )
        decision = evaluate_initial_validation(plan, result)
        receipt = RunReceiptV1(
            run.ref,
            build.ref,
            f"run-{seed if seed is not None else 0}",
            "2025-01-01T00:00:00Z",
        )
        return run, build, plan, result, decision, receipt

    def test_builds_only_exact_initial_evidence_chain(self):
        run, build, plan, result, decision, receipt = self.context()
        manifest = build_initial_evidence_manifest(
            run.ref,
            receipt,
            result,
            plan,
            decision,
        )
        self.assertEqual(manifest.run_ref, run.ref)
        self.assertIn(receipt.ref, manifest.evidence_refs)
        self.assertIn(result.ref, manifest.evidence_refs)
        self.assertIn(plan.ref, manifest.evidence_refs)
        self.assertIn(decision.ref, manifest.evidence_refs)

    def test_cross_run_receipt_substitution_is_rejected(self):
        run, build, plan, result, decision, receipt = self.context(seed=1)
        other_run, other_build, _, _, _, other_receipt = self.context(seed=2)
        self.assertNotEqual(run.ref, other_run.ref)
        self.assertEqual(build.ref, other_build.ref)
        with self.assertRaisesRegex(
            SpecValidationError,
            "receipt belongs to a different run",
        ):
            build_initial_evidence_manifest(
                run.ref,
                other_receipt,
                result,
                plan,
                decision,
            )

    def test_forged_decision_content_is_rejected(self):
        run, build, plan, result, decision, receipt = self.context()
        forged_outcomes = tuple(
            type(outcome)(
                outcome.metric_path,
                outcome.operator,
                outcome.threshold,
                outcome.actual + Decimal("1"),
                outcome.passed,
            )
            for outcome in decision.outcomes
        )
        forged = ValidationDecisionV1(
            decision.plan_ref,
            decision.result_ref,
            decision.passed,
            forged_outcomes,
        )
        with self.assertRaisesRegex(
            SpecValidationError,
            "decision content does not match",
        ):
            build_initial_evidence_manifest(
                run.ref,
                receipt,
                result,
                plan,
                forged,
            )


if __name__ == "__main__":
    unittest.main()
