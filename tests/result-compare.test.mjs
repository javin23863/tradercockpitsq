import assert from "node:assert/strict";
import test from "node:test";

import {
  compareCanonicalRuns,
  fetchComparableRun,
  renderResultCompareAuthority,
  resultCompareContext,
  resultCompareContextPath,
  validateComparableRun,
} from "../web/result-compare.mjs";

const digest = (char) => char.repeat(64);
const strategyA = `tc:strategy:v1:sha256:${digest("a")}`;
const strategyB = `tc:strategy:v1:sha256:${digest("b")}`;

function payload({
  run = "1",
  candidate = "2",
  strategy = strategyA,
  data = "3",
  execution = "4",
  engine = "5",
  result = "6",
  plan = "7",
  decision = "8",
  evidence = "9",
  actual = "1.5",
  passed = true,
  resultSchema = "tc.backtest.result.v1",
  symbol = "ES",
  timeframe = "1m",
  status = passed ? "passed" : "failed",
  reasonCode = passed ? null : "validation_rejected",
} = {}) {
  return {
    schema: "tc.initial-run-read.v1",
    run_ref: `tc:backtest-run:v1:sha256:${digest(run)}`,
    invocation_id: `initial-${run}`,
    status,
    terminal: true,
    occurred_at: "2026-01-01T00:00:00.000000Z",
    reason_code: reasonCode,
    lifecycle_event_ref: `tc:run-lifecycle-event:v1:sha256:${digest(run === "1" ? "c" : "d")}`,
    inputs: {
      candidate_ref: `tc:candidate:v1:sha256:${digest(candidate)}`,
      strategy_ref: strategy,
      data_ref: `tc:data:v1:sha256:${digest(data)}`,
      execution_ref: `tc:execution:v1:sha256:${digest(execution)}`,
      engine_build_ref: `tc:engine-build:v1:sha256:${digest(engine)}`,
      random_seed: null,
    },
    input_detail: {
      candidate: { origin: "manual" },
      strategy: { semantic_schema: "tc.strategy.rules.v1" },
      data: {
        symbol,
        timeframe,
        source: "fixture",
        dataset_revision: `rev-${data}`,
      },
      execution: { starting_cash: "100000", currency: "USD", models: [] },
      engine_build: { implementation: "tradercockpit", revision: `r-${engine}` },
    },
    artifacts: {
      receipt_ref: `tc:run-receipt:v1:sha256:${digest("0")}`,
      result_ref: `tc:result:v1:sha256:${digest(result)}`,
      plan_ref: `tc:validation-plan:v1:sha256:${digest(plan)}`,
      decision_ref: `tc:validation-decision:v1:sha256:${digest(decision)}`,
      evidence_manifest_ref: `tc:evidence-manifest:v1:sha256:${digest(evidence)}`,
    },
    result: {
      result_schema: resultSchema,
      producer_build_ref: `tc:engine-build:v1:sha256:${digest(engine)}`,
    },
    validation: {
      passed,
      source_result_schema: resultSchema,
      outcomes: [
        {
          metric_path: "metrics.profit_factor",
          operator: "gt",
          threshold: "1.3",
          actual,
          passed,
        },
      ],
    },
  };
}

test("valid comparable run requires terminal durable result custody", () => {
  const value = payload();
  assert.equal(validateComparableRun(value, strategyA).run_ref, value.run_ref);

  const running = payload();
  running.status = "running";
  running.terminal = false;
  assert.throws(() => validateComparableRun(running), /terminal with a durable result/);

  const missing = payload();
  missing.artifacts.result_ref = null;
  assert.throws(() => validateComparableRun(missing), /result_ref must be a content address/);
});

test("contextual compare refuses a run from another requested strategy", () => {
  assert.throws(
    () => validateComparableRun(payload({ strategy: strategyB }), strategyA),
    /different requested strategy/,
  );
});

test("comparison accepts matching result schema without inventing a winner", () => {
  const left = payload({ run: "1", actual: "1.5" });
  const right = payload({
    run: "2",
    candidate: "a",
    data: "b",
    execution: "c",
    engine: "d",
    result: "e",
    plan: "f",
    decision: "1",
    evidence: "2",
    actual: "1.8",
  });
  const comparison = compareCanonicalRuns(left, right);
  assert.equal(comparison.schema, "tc.result-comparison-read.v1");
  assert.equal(comparison.result_schema, "tc.backtest.result.v1");
  assert.equal(comparison.same_strategy, true);
  assert.equal(comparison.same_candidate, false);
  assert.equal(comparison.same_data, false);
  assert.equal(comparison.same_engine_build, false);
  assert.equal(comparison.gates.length, 1);
  assert.equal(comparison.gates[0].left.actual, "1.5");
  assert.equal(comparison.gates[0].right.actual, "1.8");
  assert.equal("winner" in comparison, false);
  assert.equal("score" in comparison, false);
});

test("different result schemas fail closed", () => {
  assert.throws(
    () => compareCanonicalRuns(payload(), payload({ run: "2", resultSchema: "tc.native.result.v1" })),
    /result schemas differ/,
  );
});

test("result producer build must match exact run engine identity", () => {
  const value = payload();
  value.result.producer_build_ref = `tc:engine-build:v1:sha256:${digest("f")}`;
  assert.throws(() => validateComparableRun(value), /producer build does not match/);
});

test("comparison query context round-trips without replacing other route context", () => {
  const context = {
    leftRunRef: payload().run_ref,
    leftInvocationId: "initial-1",
    rightRunRef: payload({ run: "2" }).run_ref,
    rightInvocationId: "initial-2",
  };
  const path = resultCompareContextPath("/validate/compare", context, "?strategyRef=opaque%2Fref");
  assert.match(path, /^\/validate\/compare\?/);
  assert.match(path, /strategyRef=opaque%2Fref/);
  assert.deepEqual(resultCompareContext(path.slice(path.indexOf("?"))), context);
});

test("fetch uses canonical run-read endpoint only", async () => {
  const calls = [];
  const fetchImpl = async (path, options) => {
    calls.push([path, options]);
    return {
      ok: true,
      status: 200,
      async json() {
        return payload();
      },
    };
  };
  await fetchComparableRun(payload().run_ref, "initial-1", strategyA, fetchImpl);
  assert.equal(calls.length, 1);
  assert.match(calls[0][0], /^\/api\/run-read\?/);
  assert.match(calls[0][0], /invocationId=initial-1/);
  assert.equal(calls[0][1].method, "GET");
});

test("Compare UI states the truth boundary and does not rank results", () => {
  const html = renderResultCompareAuthority(strategyA);
  assert.match(html, /Compare two exact results/);
  assert.match(html, /canonical run reader/);
  assert.match(html, /never reads hidden result payloads/);
  assert.match(html, /never.*declares a superior strategy/i);
  assert.match(html, /Different actual values are not converted into a ranking or recommendation/);
  assert.doesNotMatch(html, /winner/i);
  assert.doesNotMatch(html, /best strategy/i);
});
