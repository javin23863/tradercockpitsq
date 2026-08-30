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


test("exact run lookup preserves run and invocation input bytes through URL encoding", () => {
  const runRef = "tc:backtest-run:v1:sha256:" + "a".repeat(64);
  const invocationId = " initial-001 ";
  const path = runReadRequestPath(runRef, invocationId);
  const url = new URL(path, "http://localhost");

  assert.equal(url.pathname, "/api/run-read");
  assert.equal(url.searchParams.get("runRef"), runRef);
  assert.equal(url.searchParams.get("invocationId"), invocationId);
});


test("exact run context round-trips across shared run surfaces without dropping other context", () => {
  const runRef = "tc:backtest-run:v1:sha256:" + "b".repeat(64);
  const invocationId = "initial + Khmer ខ្មែរ / 001";
  const strategyRef = "opaque/strategy+42";
  const path = runReadContextPath(
    "/operate/runs",
    runRef,
    invocationId,
    `?strategyRef=${encodeURIComponent(strategyRef)}&unrelated=keep`,
  );
  const url = new URL(path, "http://localhost");

  assert.equal(url.pathname, "/operate/runs");
  assert.equal(url.searchParams.get("strategyRef"), strategyRef);
  assert.equal(url.searchParams.get("unrelated"), "keep");
  assert.deepEqual(runReadContext(url.search), { runRef, invocationId });
});


test("verified result route keeps the same exact run context", () => {
  const runRef = "tc:backtest-run:v1:sha256:" + "d".repeat(64);
  const invocationId = "initial-004";
  const path = runReadContextPath(
    "/validate/results",
    runRef,
    invocationId,
    "?strategyRef=opaque%2Fstrategy%2B42",
  );
  const url = new URL(path, "http://localhost");

  assert.equal(url.pathname, "/validate/results");
  assert.equal(url.searchParams.get("strategyRef"), "opaque/strategy+42");
  assert.deepEqual(runReadContext(url.search), { runRef, invocationId });
});


test("exact run context rejects partial or ambiguous query identity", () => {
  const runRef = "tc:backtest-run:v1:sha256:" + "c".repeat(64);
  assert.equal(runReadContext(`?runRef=${encodeURIComponent(runRef)}`), null);
  assert.equal(runReadContext("?invocationId=initial-001"), null);
  assert.equal(
    runReadContext(`?runRef=${encodeURIComponent(runRef)}&runRef=${encodeURIComponent(runRef)}&invocationId=initial-001`),
    null,
  );
  assert.equal(
    runReadContext(`?runRef=${encodeURIComponent(runRef)}&invocationId=initial-001&invocationId=initial-002`),
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
    data_ref: "data-ref",
    execution_ref: "execution-ref",
    engine_build_ref: "build-ref",
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


test("run read rows expose verified identity and lifecycle fields without inventing metrics", () => {
  const rows = Object.fromEntries(runReadRows(verifiedPayload));
  assert.equal(rows.Status, "passed");
  assert.equal(rows.Terminal, "Yes");
  assert.equal(rows["Run reference"], "run-ref");
  assert.equal(rows.Candidate, "candidate-ref");
  assert.equal(rows.Result, "result-ref");
  assert.equal(rows.Evidence, "evidence-ref");
  assert.equal(Object.hasOwn(rows, "Profit factor"), false);
  assert.equal(Object.hasOwn(rows, "Trades"), false);
});


test("validation results expose verified schema decision and evidence chain", () => {
  const resultRows = Object.fromEntries(validationResultRows(verifiedPayload));
  const identityRows = Object.fromEntries(validationResultIdentityRows(verifiedPayload));

  assert.equal(resultRows["Lifecycle status"], "passed");
  assert.equal(resultRows["Result schema"], "tc.backtest.result.v1");
  assert.equal(resultRows["Validation decision"], "Passed");
  assert.equal(resultRows["Validated gates"], "1");
  assert.equal(resultRows.Result, "result-ref");
  assert.equal(resultRows.Decision, "decision-ref");
  assert.equal(resultRows.Evidence, "evidence-ref");
  assert.equal(resultRows["Validation plan"], "plan-ref");
  assert.equal(identityRows["Run reference"], "run-ref");
  assert.equal(identityRows.Invocation, "initial-001");
  assert.equal(identityRows.Receipt, "receipt-ref");
  assert.equal(identityRows["Lifecycle event"], "lifecycle-ref");
});


test("validation gate rows render only backend-owned gate outcomes", () => {
  const rows = validationGateRows(verifiedPayload);
  assert.deepEqual(rows, [
    ["Gate · metrics.profit_factor", "1.5 gt 1.3 · Passed"],
  ]);
  assert.equal(rows.some(([label]) => label === "Trades"), false);
  assert.equal(rows.some(([label]) => label === "Net profit"), false);
});


test("validation results do not imply a completed result when the verified chain has none", () => {
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
  const gates = Object.fromEntries(validationGateRows(payload));

  assert.equal(rows["Lifecycle status"], "running");
  assert.equal(rows.Terminal, "No");
  assert.equal(rows["Result schema"], "None");
  assert.equal(rows["Validation decision"], "None");
  assert.equal(rows["Validated gates"], "0");
  assert.equal(rows.Result, "None");
  assert.equal(rows.Decision, "None");
  assert.equal(rows.Evidence, "None");
  assert.equal(gates["Gate outcomes"], "None");
});
