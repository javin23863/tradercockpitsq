import assert from "node:assert/strict";
import test from "node:test";

import {
  fetchImportedSqxCandidates,
  normalizeImportedSqxCandidates,
} from "../web/sqx-imported-candidates.mjs";

const candidateRef = `tc:candidate:v1:sha256:${"1".repeat(64)}`;
const strategyRef = `tc:strategy:v1:sha256:${"2".repeat(64)}`;

function fixture() {
  return {
    schema: "tc.sqx-imported-candidate-list.v1",
    candidates: [
      {
        candidate_ref: candidateRef,
        strategy_ref: strategyRef,
        candidate_origin: "sqx-builder",
        semantic_schema: "sqx.native-archive.v1",
        archive_sha256: "a".repeat(64),
        custody_relative_path: `native/sqx/archives/aa/${"a".repeat(64)}.sqx`,
        native_version: "144.2953",
        run_binding: {
          available: true,
          mode: "sqx-native-retester",
          request: { candidate_ref: candidateRef },
        },
      },
    ],
  };
}

test("normalizes durable imported candidate identities", () => {
  const catalog = normalizeImportedSqxCandidates(fixture());
  assert.equal(catalog.candidates.length, 1);
  assert.equal(catalog.candidates[0].candidate_ref, candidateRef);
  assert.equal(catalog.candidates[0].strategy_ref, strategyRef);
});

test("rejects duplicate durable candidate identities", () => {
  const payload = fixture();
  payload.candidates.push({ ...payload.candidates[0] });
  assert.throws(
    () => normalizeImportedSqxCandidates(payload),
    /Duplicate imported SQX candidate identity/,
  );
});

test("fetches only the durable candidate custody endpoint", async () => {
  const calls = [];
  const fetchImpl = async (url, options) => {
    calls.push({ url, options });
    return {
      ok: true,
      status: 200,
      async json() {
        return fixture();
      },
    };
  };
  const catalog = await fetchImportedSqxCandidates(fetchImpl);
  assert.equal(catalog.candidates[0].candidate_ref, candidateRef);
  assert.deepEqual(calls, [
    {
      url: "/api/sqx-imported-candidates",
      options: { headers: { accept: "application/json" } },
    },
  ]);
});
