from decimal import Decimal
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
from tradercockpit.engine import (
    BacktestInputsV1,
    EngineContractError,
    EvaluatorDescriptorV1,
    evaluate_backtest,
)


class FakeEvaluator:
    def __init__(self, descriptor, *, result_factory=None):
        self._descriptor = descriptor
        self.calls = 0
        self.result_factory = result_factory

    @property
    def descriptor(self):
        return self._descriptor

    def evaluate(self, inputs):
        self.calls += 1
        if self.result_factory is not None:
            return self.result_factory(inputs)
        return ResultArtifactV1(
            inputs.run.ref,
            inputs.engine_build.ref,
            self.descriptor.result_schema,
            {"net_profit": Decimal("1250.50"), "trades": 12},
        )


class EvaluatorContractTests(unittest.TestCase):
    def inputs(self, *, schema="tc.strategy.rules.v1", build_digest="a" * 64):
        strategy = StrategySpecV1(
            schema,
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
        build = EngineBuildSpecV1("tradercockpit", "r1", build_digest)
        run = BacktestRunSpecV1(candidate.ref, data.ref, execution.ref, build.ref)
        return BacktestInputsV1(run, candidate, strategy, data, execution, build)

    def descriptor(
        self,
        inputs,
        *,
        schemas=("tc.strategy.rules.v1",),
        result_schema="tc.backtest.result.v1",
        deterministic=True,
    ):
        return EvaluatorDescriptorV1(
            inputs.engine_build.ref,
            schemas,
            result_schema,
            deterministic,
        )

    def test_validated_evaluator_returns_content_addressed_result(self):
        inputs = self.inputs()
        evaluator = FakeEvaluator(self.descriptor(inputs))
        result = evaluate_backtest(inputs, evaluator)
        self.assertEqual(result.run_ref, inputs.run.ref)
        self.assertEqual(result.producer_build_ref, inputs.engine_build.ref)
        self.assertEqual(result.result_schema, "tc.backtest.result.v1")
        self.assertEqual(evaluator.calls, 1)

    def test_unsupported_semantic_schema_is_refused_before_execution(self):
        inputs = self.inputs(schema="tc.strategy.other.v1")
        evaluator = FakeEvaluator(self.descriptor(inputs))
        with self.assertRaisesRegex(
            EngineContractError,
            "unsupported strategy semantic schema",
        ):
            evaluate_backtest(inputs, evaluator)
        self.assertEqual(evaluator.calls, 0)

    def test_wrong_evaluator_build_is_refused_before_execution(self):
        inputs = self.inputs()
        other_build = EngineBuildSpecV1("tradercockpit", "r2", "b" * 64)
        descriptor = EvaluatorDescriptorV1(
            other_build.ref,
            (inputs.strategy.semantic_schema,),
            "tc.backtest.result.v1",
            True,
        )
        evaluator = FakeEvaluator(descriptor)
        with self.assertRaisesRegex(
            EngineContractError,
            "evaluator build does not match",
        ):
            evaluate_backtest(inputs, evaluator)
        self.assertEqual(evaluator.calls, 0)

    def test_result_for_different_run_is_rejected(self):
        inputs = self.inputs()
        other = self.inputs(build_digest="c" * 64)
        evaluator = FakeEvaluator(
            self.descriptor(inputs),
            result_factory=lambda _: ResultArtifactV1(
                other.run.ref,
                inputs.engine_build.ref,
                "tc.backtest.result.v1",
                {"net_profit": Decimal("1")},
            ),
        )
        with self.assertRaisesRegex(EngineContractError, "result run_ref"):
            evaluate_backtest(inputs, evaluator)

    def test_result_schema_mismatch_is_rejected(self):
        inputs = self.inputs()
        evaluator = FakeEvaluator(
            self.descriptor(inputs),
            result_factory=lambda _: ResultArtifactV1(
                inputs.run.ref,
                inputs.engine_build.ref,
                "tc.backtest.other.v1",
                {"net_profit": Decimal("1")},
            ),
        )
        with self.assertRaisesRegex(EngineContractError, "result schema"):
            evaluate_backtest(inputs, evaluator)

    def test_deterministic_evaluator_reproduces_result_identity(self):
        inputs = self.inputs()
        evaluator = FakeEvaluator(self.descriptor(inputs, deterministic=True))
        first = evaluate_backtest(inputs, evaluator)
        second = evaluate_backtest(inputs, evaluator)
        self.assertEqual(first.ref, second.ref)

    def test_result_payload_is_frozen_against_caller_mutation(self):
        inputs = self.inputs()
        payload = {"metrics": {"profit": Decimal("10")}}
        result = ResultArtifactV1(
            inputs.run.ref,
            inputs.engine_build.ref,
            "tc.backtest.result.v1",
            payload,
        )
        before = result.ref
        payload["metrics"]["profit"] = Decimal("999")
        self.assertEqual(result.ref, before)
        self.assertEqual(result.payload["metrics"]["profit"], Decimal("10"))


if __name__ == "__main__":
    unittest.main()
