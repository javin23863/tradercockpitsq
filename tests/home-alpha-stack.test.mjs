import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import {
  fetchHomeAlphaCandidates,
  parseHomeAlphaCandidates,
  renderHomeAlphaStack,
} from "../web/home-alpha-stack.mjs";

const archiveSha = "a".repeat(64);
const strategySha = "b".repeat(64);
const settingsSha = "c".repeat(64);

function candidate(overrides = {}) {
  return {
    schema: "tc.research-candidate.v1",
    entity_id: "tc-research:candidate:v1:22222222-2222-4222-8222-222222222222",
    revision: `tc-research-revision:candidate:sha256:${"f".repeat(64)}`,
    native_job_entity_id: "tc-research:native-job:v1:11111111-1111-4111-8111-111111111111",
    native_job_revision: `tc-research-revision:native-job:sha256:${"d".repeat(64)}`,
    configuration_entity_id: "tc-research:configuration:v1:33333333-3333-4333-8333-333333333333",
    configuration_revision: `tc-research-revision:configuration:sha256:${"e".repeat(64)}`,
    association_mode: "operator_selected_exact_native_output",
    archive_name: "Survivor.sqx",
    archive_relative_path: "user/projects/Builder/databanks/Results/Survivor.sqx",
    archive_ref: `tc-evidence:sha256:${archiveSha}`,
    archive_sha256: archiveSha,
    strategy_ref: `tc-evidence:sha256:${strategySha}`,
    strategy_sha256: strategySha,
    settings_ref: `tc-evidence:sha256:${settingsSha}`,
    settings_sha256: settingsSha,
    sqx_build: "144.2953",
    ...overrides,
  };
}

function catalog(candidates = []) {
  return { schema: "tc.research-candidate-catalog.v1", candidates };
}

test("Alpha Stack reuses exact current Candidate custody without inventing rank", () => {
  const parsed = parseHomeAlphaCandidates(catalog([candidate()]));
  assert.equal(parsed.candidates.length, 1);
  assert.deepEqual(parsed.candidates[0], {
    entity_id: candidate().entity_id,
    revision: candidate().revision,
    archive_name: "Survivor.sqx",
    archive_sha256: archiveSha,
    strategy_sha256: strategySha,
    sqx_build: "144.2953",
  });
  assert.equal("rank" in parsed.candidates[0], false);
  assert.equal("score" in parsed.candidates[0], false);
  assert.equal("deployed" in parsed.candidates[0], false);
});

test("Alpha Stack candidate parser preserves canonical Candidate integrity checks", () => {
  assert.throws(
    () => parseHomeAlphaCandidates(catalog([candidate({ association_mode: "inferred" })])),
    /Candidate provenance is invalid/,
  );
  assert.throws(
    () => parseHomeAlphaCandidates(catalog([candidate({ archive_relative_path: "C:/elsewhere/Survivor.sqx" })])),
    /Candidate archive path is inconsistent/,
  );
  assert.throws(
    () => parseHomeAlphaCandidates(catalog([candidate({ strategy_ref: `tc-evidence:sha256:${"0".repeat(64)}` })])),
    /Candidate evidence binding is inconsistent/,
  );
});

test("Alpha Stack refuses duplicate current Candidate identities instead of choosing one", () => {
  assert.throws(
    () => parseHomeAlphaCandidates(catalog([candidate(), candidate()])),
    /duplicate entity identity/,
  );
  assert.throws(
    () => parseHomeAlphaCandidates(catalog([
      candidate(),
      candidate({
        entity_id: "tc-research:candidate:v1:44444444-4444-4444-8444-444444444444",
      }),
    ])),
    /duplicate revision identity/,
  );
});

test("Alpha Stack fetch uses only the canonical Research Candidate catalog", async () => {
  const expected = catalog([candidate()]);
  const parsed = await fetchHomeAlphaCandidates(async (path, options) => {
    assert.equal(path, "/api/research/candidates");
    assert.equal(options.headers.accept, "application/json");
    assert.equal(options.method, undefined);
    return { ok: true, status: 200, json: async () => expected };
  });
  assert.equal(parsed.candidates.length, 1);

  await assert.rejects(
    () => fetchHomeAlphaCandidates(async () => ({ ok: false, status: 503, json: async () => ({}) })),
    /request failed: 503/,
  );
});

test("Alpha Stack keeps Candidate, promotion, export, and deployment visibly distinct", () => {
  const html = renderHomeAlphaStack(parseHomeAlphaCandidates(catalog([candidate()])));
  assert.match(html, /Research Candidates/);
  assert.match(html, /Current catalog · 1/);
  assert.match(html, /Survivor\.sqx/);
  assert.match(html, /Promoted Research Strategy/);
  assert.match(html, /Current catalog · 0/);
  assert.match(html, /No operator promotion after Proof yet/);
  assert.match(html, /Exported Strategy/);
  assert.match(html, /Unavailable · Export authority not connected/);
  assert.match(html, /Deployed \/ Live Strategy/);
  assert.match(html, /Unavailable · Deployment authority not connected/);
  assert.match(html, /historical\/research evidence only/);
  assert.doesNotMatch(html, /Champion/);
  assert.doesNotMatch(html, /Deployed · Survivor/);
  assert.doesNotMatch(html, /Live · Survivor/);
});

test("empty Candidate custody is current zero, not fabricated unavailability or deployment", () => {
  const html = renderHomeAlphaStack(parseHomeAlphaCandidates(catalog([])));
  assert.match(html, /Current catalog · 0/);
  assert.match(html, /No current native Research Candidate custody exists/);
  assert.match(html, /Deployment authority not connected/);
});

test("Candidate read failure remains distinct from downstream unconnected authorities", () => {
  const html = renderHomeAlphaStack(null, "Candidate catalog request failed: 409");
  assert.match(html, /Candidate custody read failed/);
  assert.match(html, /Candidate catalog request failed: 409/);
  assert.match(html, /No operator promotion after Proof yet/);
  assert.match(html, /Deployment authority not connected/);
});

test("Alpha Stack renders operator promotion identities without claiming live deployment", () => {
  const promotions = {
    promotions: [{
      entity_id: "tc-research:promotion:v1:aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
      proof_entity_id: "tc-research:proof:v1:bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
      candidate_entity_id: "tc-research:candidate:v1:22222222-2222-4222-8222-222222222222",
      candidate_archive_name: "Survivor.sqx",
    }],
  };
  const html = renderHomeAlphaStack(parseHomeAlphaCandidates(catalog([candidate()])), "", promotions);
  assert.match(html, /data-alpha-stage="promoted-research-strategy" data-alpha-stage-state="current"/);
  assert.match(html, /Current catalog · 1/);
  assert.match(html, /data-alpha-promotion/);
  assert.doesNotMatch(html, /Live · Survivor/);
  assert.match(html, /Unavailable · Deployment authority not connected/);
});

test("desktop loads the Home Alpha Stack binder", async () => {
  const source = await readFile(new URL("../web/index.html", import.meta.url), "utf8");
  assert.match(source, /src="\/home-alpha-stack\.mjs"/);
});
