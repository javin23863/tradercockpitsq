import assert from "node:assert/strict";
import test from "node:test";

import {
  strategyOverviewActionSpecs,
  strategyOverviewRows,
  validateStrategyOverviewPayload,
} from "../web/strategy-overview.mjs";

const digest = (char) => char.repeat(64);
const strategyRef = `tc:strategy:v1:sha256:${digest("a")}`;
const runRef = `tc:backtest-run:v1:sha256:${digest("1")}`;
const invocationId = "overview-001";

function payload(overrides = {}) {
  const engineBuildRef = `tc:engine-build:v1:sha256:${digest("9")}`;
  const base = {
    schema: "tc.initial-run-read.v1",
    run_ref: runRef,
    invocation_id: invocationId,
    status: "passed",
    terminal: true,
    occurred_at: "2026-08-31T00:00:00.000000Z",
    reason_code: null,
    lifecycle_event_ref: `tc:run-lifecycle-event:v1:sha256:${digest("5")}`,
    inputs: {
      candidate_ref: `tc:candidate:v1:sha256:${digest("6")}`,
      strategy_ref: strategyRef,
      data_ref: `tc:data:v1:sha256:${digest("7")}`,
      execution_ref: `tc:execution:v1:sha256:${digest("8")}`,
      engine_build_ref: engineBuildRef,
      random_seed: null,
    },
    input_detail: {
      candidate: { origin: "sqx-builder" },
      strategy: { semantic_schema: "tc.strategy.rules.v1" },
      data: {
        symbol: "ES",
        timeframe: "1m",
        source: "fixture",
        dataset_revision: "rev-a",
      },
      execution: {
        starting_cash: "100000",
        currency: "USD",
        models: [],
      },
      engine_build: {
        implementation: "tradercockpit",
        revision: "rev-engine",
      },
    },
    artifacts: {
      receipt_ref: `tc:run-receipt:v1:sha256:${digest("0")}`,
      result_ref: `tc:result:v1:sha256:${digest("2")}`,
      plan_ref: `tc:validation-plan:v1:sha256:${digest("3")}`,
      decision_ref: `tc:validation-decision:v1:sha256:${digest("4")}`,
      evidence_manifest_ref: `tc:evidence-manifest:v1:sha256:${digest("b")}`,
    },
    result: {
      result_schema: "tc.backtest.result.v1",
      producer_build_ref: engineBuildRef,
    },
    validation: {
      passed: true,
      source_result_schema: "tc.backtest.result.v1",
      outcomes: [],
    },
  };
  return { ...base, ...overrides };
}

test("Overview accepts one exact canonical linked invocation for the requested strategy", () => {
  const accepted = validateStrategyOverviewPayload(payload(), {
    requestedStrategyRef: strategyRef,
    runRef,
    invocationId,
  });
  assert.equal(accepted.inputs.strategy_ref, strategyRef);
  const rows = new Map(strategyOverviewRows(accepted));
  assert.equal(rows.get("Run reference"), runRef);
  assert.equal(rows.get("Invocation"), invocationId);
  assert.equal(rows.get("Candidate origin"), "sqx-builder");
  assert.equal(rows.get("Market"), "ES · 1m");
  assert.equal(rows.get("Validation"), "Passed");
});

test("Overview refuses cross-strategy run substitution", () => {
  const otherStrategy = `tc:strategy:v1:sha256:${digest("c")}`;
  assert.throws(
    () =>
      validateStrategyOverviewPayload(
        payload({
          inputs: { ...payload().inputs, strategy_ref: otherStrategy },
        }),
        { requestedStrategyRef: strategyRef, runRef, invocationId },
      ),
    /different strategy than this Overview route/,
  );
});

test("Overview refuses result custody produced by a different engine build", () => {
  assert.throws(
    () =>
      validateStrategyOverviewPayload(
        payload({
          result: {
            result_schema: "tc.backtest.result.v1",
            producer_build_ref: `tc:engine-build:v1:sha256:${digest("d")}`,
          },
        }),
        { requestedStrategyRef: strategyRef, runRef, invocationId },
      ),
    /result producer build does not match/,
  );
});

test("Overview refuses non-canonical content-address spellings", () => {
  assert.throws(
    () =>
      validateStrategyOverviewPayload(
        payload({ run_ref: `tc:backtest-run:v1:sha256:${"A".repeat(64)}` }),
        { requestedStrategyRef: strategyRef, runRef, invocationId },
      ),
    /canonical backtest-run content address/,
  );
});

test("Overview next actions preserve exact run context without inventing controls", () => {
  const specs = strategyOverviewActionSpecs(payload(), "?unrelated=keep");
  assert.equal(specs.length, 2);
  assert.equal(specs[0].label, "Open exact invocation in Operate");
  const operate = new URL(`https://example.test${specs[0].path}`);
  assert.equal(operate.pathname, "/operate/runs");
  assert.equal(operate.searchParams.get("runRef"), runRef);
  assert.equal(operate.searchParams.get("invocationId"), invocationId);
  assert.equal(operate.searchParams.get("unrelated"), "keep");
  assert.equal(specs[1].label, "Open verified results");
});

test("Overview does not manufacture a results action when the exact run has no result evidence", () => {
  const incomplete = payload({
    artifacts: {
      receipt_ref: `tc:run-receipt:v1:sha256:${digest("0")}`,
      result_ref: null,
      plan_ref: null,
      decision_ref: null,
      evidence_manifest_ref: null,
    },
    result: null,
    validation: null,
    status: "running",
    terminal: false,
  });
  const specs = strategyOverviewActionSpecs(incomplete);
  assert.deepEqual(specs.map((item) => item.label), ["Open exact invocation in Operate"]);
});
