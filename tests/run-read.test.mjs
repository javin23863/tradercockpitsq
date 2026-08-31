import assert from "node:assert/strict";
import test from "node:test";

import {
  runReadContext,
  runReadContextPath,
  runReadRequestPath,
  runReadRows,
  validationGateRows,
  validationResultIdentityRows,
  validationResultRows,
} from "../web/run-read.mjs";


test("exact run lookup preserves run and invocation bytes through URL encoding", () => {
  const runRef = "tc:backtest-run:v1:sha256:" + "a".repeat(64);
  const invocationId = " initial-001 ";
  const url = new URL(runReadRequestPath(runRef, invocationId), "http://localhost");
  assert.equal(url.pathname, "/api/run-read");
  assert.equal(url.searchParams.get("runRef"), runRef);
  assert.equal(url.searchParams.get("invocationId"), invocationId);
});


test("exact run context can be carried inside the StrategyQuant X screen", () => {
  const runRef = "tc:backtest-run:v1:sha256:" + "b".repeat(64);
  const invocationId = "retest + Khmer ខ្មែរ / 001";
  const path = runReadContextPath(
    "/strategyquant",
    runRef,
    invocationId,
    "?stage=backtest&tab=overview&other=keep",
  );
  const url = new URL(path, "http://localhost");
  assert.equal(url.pathname, "/strategyquant");
  assert.equal(url.searchParams.get("stage"), "backtest");
  assert.equal(url.searchParams.get("tab"), "overview");
  assert.equal(url.searchParams.get("other"), "keep");
  assert.deepEqual(runReadContext(url.search), { runRef, invocationId });
});


test("exact run context rejects partial or ambiguous identity", () => {
  const runRef = "tc:backtest-run:v1:sha256:" + "c".repeat(64);
  assert.equal(runReadContext(`?runRef=${encodeURIComponent(runRef)}`), null);
  assert.equal(runReadContext("?invocationId=initial-001"), null);
  assert.equal(
    runReadContext(`?runRef=${encodeURIComponent(runRef)}&runRef=${encodeURIComponent(runRef)}&invocationId=initial-001`),
    null,
  );
});


const verifiedPayload = {
  schema: "tc.initial-run-read.v1",
  run_ref: "run-ref",
  invocation_id: "initial-001",
  status: "passed",
  terminal: true,
  occurred_at: "2025-01-02T00:00:00Z",
  reason_code: null,
  lifecycle_event_ref: "lifecycle-ref",
  inputs: {
    candidate_ref: "candidate-ref",
    strategy_ref: "strategy-ref",
    data_ref: "data-ref",
    execution_ref: "execution-ref",
    engine_build_ref: "build-ref",
    random_seed: null,
  },
  input_detail: {
    candidate: { origin: "sqx-builder", parent_strategy_ref: null, origin_ref: null },
    strategy: { semantic_schema: "sqx.native-archive.v1" },
    data: {
      symbol: "ES",
      timeframe: "1m",
      source: "historical",
      dataset_revision: "rev-1",
    },
    execution: {
      starting_cash: "100000",
      currency: "USD",
      models: [{ kind: "fill", model: "native-sqx" }],
    },
    engine_build: {
      implementation: "strategyquant-x",
      revision: "144.2953",
      artifact_sha256: "a".repeat(64),
    },
  },
  artifacts: {
    receipt_ref: "receipt-ref",
    result_ref: "result-ref",
    plan_ref: "plan-ref",
    decision_ref: "decision-ref",
    evidence_manifest_ref: "evidence-ref",
  },
  result: {
    result_schema: "tc.backtest.result.v1",
    producer_build_ref: "build-ref",
  },
  validation: {
    passed: true,
    source_result_schema: "tc.backtest.result.v1",
    outcomes: [{
      metric_path: "metrics.profit_factor",
      operator: "gt",
      threshold: "1.3",
      actual: "1.5",
      passed: true,
    }],
  },
};


test("run read rows expose custody without inventing extra result metrics", () => {
  const rows = Object.fromEntries(runReadRows(verifiedPayload));
  assert.equal(rows.Status, "passed");
  assert.equal(rows.Terminal, "Yes");
  assert.equal(rows.Candidate, "candidate-ref");
  assert.equal(rows["Candidate origin"], "sqx-builder");
  assert.equal(rows.Strategy, "strategy-ref");
  assert.equal(rows.Market, "ES · 1m");
  assert.equal(rows.Engine, "strategyquant-x · 144.2953");
  assert.equal(rows.Result, "result-ref");
  assert.equal(rows.Evidence, "evidence-ref");
  assert.equal(Object.hasOwn(rows, "Profit factor"), false);
  assert.equal(Object.hasOwn(rows, "Trades"), false);
});


test("validation read helpers expose only backend-owned decision/evidence", () => {
  const resultRows = Object.fromEntries(validationResultRows(verifiedPayload));
  const identityRows = Object.fromEntries(validationResultIdentityRows(verifiedPayload));
  const gates = validationGateRows(verifiedPayload);

  assert.equal(resultRows["Validation decision"], "Passed");
  assert.equal(resultRows["Validated gates"], "1");
  assert.equal(resultRows.Evidence, "evidence-ref");
  assert.equal(identityRows["Run reference"], "run-ref");
  assert.equal(identityRows["Lifecycle event"], "lifecycle-ref");
  assert.deepEqual(gates, [["Gate · metrics.profit_factor", "1.5 gt 1.3 · Passed"]]);
});


test("running lifecycle never implies historical result completion", () => {
  const payload = {
    ...verifiedPayload,
    status: "running",
    terminal: false,
    artifacts: {
      receipt_ref: "receipt-ref",
      result_ref: null,
      plan_ref: null,
      decision_ref: null,
      evidence_manifest_ref: null,
    },
    result: null,
    validation: null,
  };
  const rows = Object.fromEntries(validationResultRows(payload));
  assert.equal(rows["Lifecycle status"], "running");
  assert.equal(rows.Terminal, "No");
  assert.equal(rows.Result, "None");
  assert.equal(rows["Validation decision"], "None");
});
