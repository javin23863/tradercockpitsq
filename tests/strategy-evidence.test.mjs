import assert from "node:assert/strict";
import test from "node:test";

import {
  fetchStrategyEvidence,
  renderStrategyEvidenceAuthority,
  validateStrategyEvidencePayload,
} from "../web/strategy-evidence.mjs";

const digest = (char) => char.repeat(64);
const strategyRef = `tc:strategy:v1:sha256:${digest("a")}`;

function payload() {
  return {
    schema: "tc.initial-run-read.v1",
    run_ref: `tc:backtest-run:v1:sha256:${digest("b")}`,
    invocation_id: "initial-001",
    status: "passed",
    terminal: true,
    occurred_at: "2026-01-01T00:00:00.000000Z",
    reason_code: null,
    lifecycle_event_ref: `tc:run-lifecycle-event:v1:sha256:${digest("c")}`,
    inputs: {
      candidate_ref: `tc:candidate:v1:sha256:${digest("d")}`,
      strategy_ref: strategyRef,
      data_ref: `tc:data:v1:sha256:${digest("e")}`,
      execution_ref: `tc:execution:v1:sha256:${digest("f")}`,
      engine_build_ref: `tc:engine-build:v1:sha256:${digest("0")}`,
      random_seed: null,
    },
    input_detail: {
      candidate: { origin: "manual" },
      strategy: { semantic_schema: "tc.strategy.rules.v1" },
      data: { symbol: "ES", timeframe: "1m" },
      execution: { starting_cash: "100000", currency: "USD", models: [] },
      engine_build: { implementation: "tradercockpit", revision: "r1" },
    },
    artifacts: {
      receipt_ref: `tc:run-receipt:v1:sha256:${digest("1")}`,
      result_ref: `tc:result:v1:sha256:${digest("2")}`,
      plan_ref: `tc:validation-plan:v1:sha256:${digest("3")}`,
      decision_ref: `tc:validation-decision:v1:sha256:${digest("4")}`,
      evidence_manifest_ref: `tc:evidence-manifest:v1:sha256:${digest("5")}`,
    },
    result: {
      result_schema: "tc.backtest.result.v1",
      producer_build_ref: `tc:engine-build:v1:sha256:${digest("0")}`,
    },
    validation: {
      passed: true,
      source_result_schema: "tc.backtest.result.v1",
      outcomes: [
        {
          metric_path: "metrics.profit_factor",
          operator: "gt",
          threshold: "1.3",
          actual: "1.5",
          passed: true,
        },
      ],
    },
  };
}

test("evidence payload accepts one exact canonical proof chain", () => {
  const value = payload();
  assert.equal(validateStrategyEvidencePayload(value, strategyRef), value);
});

test("evidence payload refuses cross-strategy run substitution", () => {
  assert.throws(
    () => validateStrategyEvidencePayload(payload(), `tc:strategy:v1:sha256:${digest("9")}`),
    /different strategy reference/,
  );
});

test("evidence manifest cannot appear without receipt result plan and decision", () => {
  const value = payload();
  value.artifacts.receipt_ref = null;
  assert.throws(
    () => validateStrategyEvidencePayload(value, strategyRef),
    /complete evidence chain/,
  );
});

test("passed lifecycle cannot be displayed without a passing evidence decision", () => {
  const value = payload();
  value.validation.passed = false;
  assert.throws(
    () => validateStrategyEvidencePayload(value, strategyRef),
    /passing evidence chain/,
  );
});

test("evidence fetch uses only the canonical run-read endpoint", async () => {
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
  const value = await fetchStrategyEvidence(
    strategyRef,
    payload().run_ref,
    "initial-001",
    fetchImpl,
  );
  assert.equal(value.inputs.strategy_ref, strategyRef);
  assert.equal(calls.length, 1);
  assert.match(calls[0][0], /^\/api\/run-read\?/);
  assert.match(calls[0][0], /runRef=/);
  assert.match(calls[0][0], /invocationId=initial-001/);
  assert.equal(calls[0][1].method, "GET");
});

test("Evidence UI is read-only and does not claim champion or deployment state", () => {
  const html = renderStrategyEvidenceAuthority(strategyRef);
  assert.match(html, /Load exact run proof/);
  assert.match(html, /canonical run reader/);
  assert.match(html, /No run is inferred from the route/);
  assert.match(html, /does not imply champion, promotion, deployment, or live-trading state/);
  assert.doesNotMatch(html, /certified strategy/i);
  assert.doesNotMatch(html, /promote strategy/i);
});
