import test from "node:test";
import assert from "node:assert/strict";

import {
  fetchProductStatus,
  fetchProductStatusOrUnavailable,
  productStatusFromPayload,
  proofDetail,
} from "../web/research-proof.mjs";

function currentStatus(overrides = {}) {
  return {
    schema: "tc.runtime-status.v1",
    application: { status: "ready", server: "canonical", desktop: "canonical-server-ui" },
    research_backend: { status: "ready", producer: "strategyquant-x" },
    research_custody: { status: "ready" },
    market_data: { status: "unavailable" },
    account: { status: "unavailable" },
    model: { status: "unavailable" },
    provider: { status: "unavailable" },
    extensions: { status: "unavailable" },
    ...overrides,
  };
}

function proofRecord() {
  return {
    entity_id: "tc-research:proof:v1:77777777-7777-4777-8777-777777777777",
    revision: `tc-research-revision:proof:sha256:${"7".repeat(64)}`,
    idea: { revision: `tc-research-revision:idea:sha256:${"0".repeat(64)}` },
    configuration: { revision: `tc-research-revision:configuration:sha256:${"1".repeat(64)}` },
    native_job: { revision: `tc-research-revision:native-job:sha256:${"2".repeat(64)}` },
    candidate: { revision: `tc-research-revision:candidate:sha256:${"3".repeat(64)}` },
    historical_result: {
      revision: `tc-research-revision:historical-result:sha256:${"4".repeat(64)}`,
      result_archive_sha256: "e".repeat(64),
      engine_sha256: "f".repeat(64),
    },
    validation: {
      validation_ref: `tc-evidence:sha256:${"6".repeat(64)}`,
      result_archive_sha256: "8".repeat(64),
    },
  };
}

test("current product status parser requires the canonical runtime read model", () => {
  assert.equal(productStatusFromPayload(currentStatus()).schema, "tc.runtime-status.v1");
  assert.throws(
    () => productStatusFromPayload({ ...currentStatus(), schema: "tc.other-status.v1" }),
    /Current product status is invalid/,
  );
  assert.throws(
    () => productStatusFromPayload({ ...currentStatus(), research_custody: null }),
    /Current product status is invalid/,
  );
});

test("current product status is read from the canonical status API", async () => {
  let request = null;
  const expected = currentStatus();
  const actual = await fetchProductStatus(async (path, options) => {
    request = { path, options };
    return { ok: true, status: 200, json: async () => expected };
  });
  assert.equal(request.path, "/api/status");
  assert.equal(request.options.headers.accept, "application/json");
  assert.equal(actual, expected);
});

test("current product status failure degrades to unavailable instead of failing Proof", async () => {
  const httpFailure = await fetchProductStatusOrUnavailable(async () => ({
    ok: false,
    status: 503,
    json: async () => ({ detail: "status offline" }),
  }));
  assert.equal(httpFailure, null);

  const newerSchema = await fetchProductStatusOrUnavailable(async () => ({
    ok: true,
    status: 200,
    json: async () => ({ ...currentStatus(), schema: "tc.runtime-status.v2" }),
  }));
  assert.equal(newerSchema, null);
});

test("Proof rendering separates immutable historical evidence from current product status", () => {
  const html = proofDetail(proofRecord(), currentStatus());
  assert.match(html, /Exact historical Research chain recovered/);
  assert.match(html, /data-proof-current-product-status="tc\.runtime-status\.v1"/);
  assert.match(html, /Current product status/);
  assert.match(html, /This mutable status is not stored as immutable Proof evidence/);
  assert.doesNotMatch(html, /<strong>Exact Research chain recovered<\/strong>/);
});

test("Proof rendering never silently omits current product status", () => {
  const html = proofDetail(proofRecord(), null);
  assert.match(html, /data-proof-current-product-status="unavailable"/);
  assert.match(html, /Current product status could not be read/);
});
