import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import {
  ensureHomePipelineBody,
  fetchHomeCandidates,
  fetchHomeNativeJobs,
  fetchHomePipelineSnapshot,
  renderHomePipelineOverview,
  summarizeHomePipelineCatalogs,
} from "../web/home-pipeline-overview.mjs";

const emptyPayloads = {
  "/api/research/ideas": { schema: "tc.research-idea-catalog.v1", ideas: [] },
  "/api/research/configurations": { schema: "tc.research-configuration-catalog.v1", configurations: [] },
  "/api/research/native-jobs": { schema: "tc.research-native-job-catalog.v1", jobs: [] },
  "/api/research/candidates": { schema: "tc.research-candidate-catalog.v1", candidates: [] },
  "/api/research/historical-results": { schema: "tc.research-historical-result-catalog.v1", results: [] },
  "/api/research/proofs": { schema: "tc.research-proof-catalog.v1", proofs: [] },
};

function response(payload, status = 200) {
  return { ok: status >= 200 && status < 300, status, json: async () => payload };
}

test("Pipeline Overview reads only canonical Research lifecycle catalogs", async () => {
  const calls = [];
  const snapshot = await fetchHomePipelineSnapshot(async (path, options) => {
    calls.push(path);
    assert.equal(options.headers.accept, "application/json");
    assert.equal(options.method, undefined);
    return response(emptyPayloads[path]);
  });

  assert.equal(snapshot.phase, "loaded");
  assert.deepEqual([...calls].sort(), Object.keys(emptyPayloads).sort());
  assert.deepEqual(snapshot.summary, {
    idea: { count: 0 },
    configuration: { count: 0, compiled: 0, approved: 0 },
    native_job: { count: 0, prepared: 0, submitted: 0, failed: 0 },
    candidate: { count: 0 },
    historical_result: { count: 0, prepared: 0, completed: 0, failed: 0, validation_not_run: 0 },
    proof: { count: 0, outcome_unread: 0 },
  });
});

test("Pipeline Overview preserves canonical parser refusal for Candidate and native-job catalogs", async () => {
  await assert.rejects(
    () => fetchHomeCandidates(async () => response({ schema: "wrong", candidates: [] })),
    /Candidate catalog schema mismatch/,
  );
  await assert.rejects(
    () => fetchHomeNativeJobs(async () => response({ schema: "wrong", jobs: [] })),
    /Native job catalog schema mismatch/,
  );
});

test("Pipeline summary keeps submitted, completed, failed, not-run, and outcome-unread distinct", () => {
  const summary = summarizeHomePipelineCatalogs({
    ideas: { ideas: [{ entity_id: "idea" }] },
    configurations: { configurations: [{ state: "compiled" }, { state: "approved" }] },
    jobs: [{ state: "submitted" }, { state: "failed" }],
    candidates: [{ entity_id: "candidate" }],
    results: [
      { state: "completed", validation_state: "not_run" },
      { state: "failed", validation_state: "not_run" },
    ],
    proofs: [{ producer_validation_outcome: "producer_result_captured_outcome_unread" }],
  });

  assert.equal(summary.native_job.submitted, 1);
  assert.equal(summary.native_job.failed, 1);
  assert.equal(summary.historical_result.completed, 1);
  assert.equal(summary.historical_result.failed, 1);
  assert.equal(summary.historical_result.validation_not_run, 2);
  assert.equal(summary.proof.outcome_unread, 1);

  const html = renderHomePipelineOverview({ phase: "loaded", summary });
  assert.match(html, /Submitted/);
  assert.match(html, /Execution completed/);
  assert.match(html, /Execution failed/);
  assert.match(html, /Validation not run/);
  assert.match(html, /Producer validation outcome unread/);
  assert.match(html, /Promotion \/ Deployment/);
  assert.match(html, /Promotion\/deployment pipeline authority not connected/);
  assert.doesNotMatch(html, />Passed?</i);
  assert.doesNotMatch(html, /Deployed ·/);
});

test("one failed lifecycle authority remains visible without fabricating the missing stage", async () => {
  const snapshot = await fetchHomePipelineSnapshot(async (path) => {
    if (path === "/api/research/proofs") return response({ detail: "Proof custody unavailable" }, 503);
    return response(emptyPayloads[path]);
  });
  assert.equal(snapshot.phase, "partial");
  assert.equal(snapshot.failures.proofs, "Proof custody unavailable");

  const html = renderHomePipelineOverview(snapshot);
  assert.match(html, /Research Proof/);
  assert.match(html, /Read failed/);
  assert.match(html, /Proof custody unavailable/);
  assert.match(html, /Read available/);
  assert.match(html, /Promotion\/deployment pipeline authority not connected/);
});

test("Pipeline binder replaces only the body placeholder and preserves the panel shell", () => {
  const originalDocument = globalThis.document;
  let replacement = null;
  const placeholder = { replaceWith(node) { replacement = node; } };
  const body = {
    attributes: new Map(),
    setAttribute(name, value) { this.attributes.set(name, value); },
  };
  const zone = {
    querySelector(selector) {
      if (selector === "[data-home-pipeline-body]") return null;
      if (selector === ".empty-state") return placeholder;
      throw new Error(`Unexpected selector: ${selector}`);
    },
  };
  globalThis.document = {
    createElement(tagName) {
      assert.equal(tagName, "div");
      return body;
    },
  };
  try {
    assert.equal(ensureHomePipelineBody(zone), body);
    assert.equal(replacement, body);
    assert.equal(body.attributes.has("data-home-pipeline-body"), true);
  } finally {
    if (originalDocument === undefined) delete globalThis.document;
    else globalThis.document = originalDocument;
  }
});

test("desktop loads the Home Pipeline Overview binder", async () => {
  const source = await readFile(new URL("../web/index.html", import.meta.url), "utf8");
  assert.match(source, /src="\/home-pipeline-overview\.mjs"/);

  const binder = await readFile(new URL("../web/home-pipeline-overview.mjs", import.meta.url), "utf8");
  assert.doesNotMatch(binder, /zone\.innerHTML\s*=/);
  assert.match(binder, /body\.innerHTML\s*=\s*renderHomePipelineOverview/);
});
