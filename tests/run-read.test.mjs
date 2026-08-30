import assert from "node:assert/strict";
import test from "node:test";

import { runReadRequestPath, runReadRows } from "../web/run-read.mjs";


test("exact run lookup preserves run and invocation input bytes through URL encoding", () => {
  const runRef = "tc:backtest-run:v1:sha256:" + "a".repeat(64);
  const invocationId = " initial-001 ";
  const path = runReadRequestPath(runRef, invocationId);
  const url = new URL(path, "http://localhost");

  assert.equal(url.pathname, "/api/run-read");
  assert.equal(url.searchParams.get("runRef"), runRef);
  assert.equal(url.searchParams.get("invocationId"), invocationId);
});


test("run read rows expose verified identity and lifecycle fields without inventing metrics", () => {
  const payload = {
    schema: "tc.initial-run-read.v1",
    run_ref: "run-ref",
    invocation_id: "initial-001",
    status: "passed",
    terminal: true,
    occurred_at: "2025-01-02T00:00:00Z",
    reason_code: null,
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
  };

  const rows = Object.fromEntries(runReadRows(payload));
  assert.equal(rows.Status, "passed");
  assert.equal(rows.Terminal, "Yes");
  assert.equal(rows["Run reference"], "run-ref");
  assert.equal(rows.Candidate, "candidate-ref");
  assert.equal(rows.Result, "result-ref");
  assert.equal(rows.Evidence, "evidence-ref");
  assert.equal(Object.hasOwn(rows, "Profit factor"), false);
  assert.equal(Object.hasOwn(rows, "Trades"), false);
});
