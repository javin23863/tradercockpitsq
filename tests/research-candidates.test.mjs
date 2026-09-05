import assert from "node:assert/strict";
import test from "node:test";
import { startRetester } from "../web/research-backtest.mjs";

import {
  candidateCatalogFromPayload,
  candidateFromPayload,
  importNativeCandidate,
  nativeJobsFromPayload,
  outputsFromPayload,
} from "../web/research-candidates.mjs";

const archiveSha = "a".repeat(64);
const strategySha = "b".repeat(64);
const settingsSha = "c".repeat(64);
const nativeJobRevision = `tc-research-revision:native-job:sha256:${"d".repeat(64)}`;
const configurationRevision = `tc-research-revision:configuration:sha256:${"e".repeat(64)}`;
const nativeJobEntity = "tc-research:native-job:v1:11111111-1111-4111-8111-111111111111";

function candidate(overrides = {}) {
  return {
    schema: "tc.research-candidate.v1",
    entity_id: "tc-research:candidate:v1:22222222-2222-4222-8222-222222222222",
    revision: `tc-research-revision:candidate:sha256:${"f".repeat(64)}`,
    native_job_entity_id: nativeJobEntity,
    native_job_revision: nativeJobRevision,
    configuration_entity_id: "tc-research:configuration:v1:33333333-3333-4333-8333-333333333333",
    configuration_revision: configurationRevision,
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
    reused: false,
    ...overrides,
  };
}

function response(payload, { ok = true, status = 200 } = {}) {
  return { ok, status, async json() { return payload; } };
}

test("candidate payload cross-checks exact native evidence bindings", () => {
  assert.equal(candidateFromPayload(candidate()).archive_sha256, archiveSha);
  assert.throws(
    () => candidateFromPayload(candidate({ archive_ref: `tc-evidence:sha256:${"0".repeat(64)}` })),
    /Candidate evidence binding is inconsistent/,
  );
  assert.throws(
    () => candidateFromPayload(candidate({ association_mode: "inferred" })),
    /Candidate provenance is invalid/,
  );
  assert.throws(
    () => candidateFromPayload(candidate({ archive_relative_path: "C:/arbitrary/Survivor.sqx" })),
    /Candidate archive path is inconsistent/,
  );
  assert.equal(candidateFromPayload(candidate({ ml_model_artifact_sha256: "9".repeat(64) })).ml_model_artifact_sha256, "9".repeat(64));
  assert.equal(candidateFromPayload(candidate({ ml_model_artifact_sha256: null })).ml_model_artifact_sha256, null);
  assert.throws(
    () => candidateFromPayload(candidate({ ml_model_artifact_sha256: "C:/models/tree.pkl" })),
    /Candidate ML pointer is invalid/,
  );
});

test("candidate catalog validates every current candidate", () => {
  const parsed = candidateCatalogFromPayload({ schema: "tc.research-candidate-catalog.v1", candidates: [candidate()] });
  assert.equal(parsed.length, 1);
  assert.throws(() => candidateCatalogFromPayload({ schema: "wrong", candidates: [] }), /schema mismatch/);
});

test("only submitted native jobs are offered for candidate binding", () => {
  const jobs = nativeJobsFromPayload({
    schema: "tc.research-native-job-catalog.v1",
    jobs: [
      { schema: "tc.research-native-job.v1", state: "submitted", entity_id: nativeJobEntity, revision: nativeJobRevision },
      { schema: "tc.research-native-job.v1", state: "failed", entity_id: "failed", revision: "failed" },
    ],
  });
  assert.equal(jobs.length, 1);
  assert.equal(jobs[0].revision, nativeJobRevision);
});

test("only exact inspectable SQX outputs are offered", () => {
  const parsed = outputsFromPayload({
    schema: "tc.sqx-builder-output-list.v1",
    runtime: { ready: true, status: "verified" },
    import_available: true,
    import_reason: null,
    outputs: [
      { archive: "Survivor.sqx", archive_sha256: archiveSha, inspectable: true },
      { archive: "Broken.sqx", inspectable: false, reason_code: "invalid_sqx_archive" },
    ],
  });
  assert.equal(parsed.ready, true);
  assert.equal(parsed.importAvailable, true);
  assert.equal(parsed.outputs.length, 1);
  assert.equal(parsed.outputs[0].archive, "Survivor.sqx");
});

test("candidate import sends only job and archive identities and refreshes only bound custody", async (t) => {
  const previousWindow = globalThis.window;
  t.after(() => { if (previousWindow === undefined) delete globalThis.window; else globalThis.window = previousWindow; });
  const events = [];
  globalThis.window = { dispatchEvent: (event) => events.push(event.type) };
  const job = {
    schema: "tc.research-native-job.v1",
    state: "submitted",
    entity_id: nativeJobEntity,
    revision: nativeJobRevision,
    configuration_revision: configurationRevision,
  };
  const output = { archive: "Survivor.sqx", archive_sha256: archiveSha, inspectable: true };
  let request;
  const imported = await importNativeCandidate(job, output, async (url, options) => {
    request = { url, options };
    return response(candidate(), { status: 201 });
  });
  assert.equal(request.url, "/api/research/candidates");
  assert.equal(request.options.method, "POST");
  assert.deepEqual(JSON.parse(request.options.body), {
    action: "import-native-output",
    native_job_entity_id: nativeJobEntity,
    expected_native_job_revision: nativeJobRevision,
    archive: "Survivor.sqx",
    expected_archive_sha256: archiveSha,
  });
  assert.equal(imported.entity_id, candidate().entity_id);
  assert.deepEqual(events, ["tradercockpit:custody-changed"]);

  await assert.rejects(
    importNativeCandidate(job, output, async () => response(candidate({ archive_sha256: "9".repeat(64), archive_ref: `tc-evidence:sha256:${"9".repeat(64)}` }))),
    /does not bind the selected native identities/,
  );
  await assert.rejects(importNativeCandidate(job, output, async () => response({ detail: "refused" }, { ok: false, status: 409 })), /refused/);
  await assert.rejects(importNativeCandidate(null, output), /requires one submitted native job/);
  assert.equal(events.length, 1, "invalid or refused imports must not announce custody changes");
});

test("imported candidates coexist with Builder custody without invented run provenance", async () => {
  const imported = candidate({
    native_job_entity_id: null, native_job_revision: null,
    configuration_entity_id: null, configuration_revision: null,
    association_mode: "operator_selected_exact_native_archive", sqx_build: null,
    archive_relative_path: "user/projects/Retester/databanks/Results/Survivor.sqx",
    history_status: "unknown",
    origin: { kind: "user_import", project: "Retester", databank: "Results", history_status: "unknown",
      original_archive_sha256: archiveSha, original_archive_ref: `tc-evidence:sha256:${archiveSha}` },
  });
  assert.equal(candidateCatalogFromPayload({ schema: "tc.research-candidate-catalog.v1", candidates: [candidate(), imported] }).length, 2);
  assert.throws(() => candidateFromPayload({ ...imported, native_job_revision: nativeJobRevision }), /provenance/);
  assert.throws(() => candidateFromPayload({ ...imported, origin: { ...imported.origin, history_status: "passed" } }), /provenance/);
  assert.throws(() => candidateFromPayload({ ...imported, origin: { ...imported.origin, original_archive_sha256: "0".repeat(64) } }), /provenance/);
  let calls = 0;
  await assert.rejects(startRetester(imported, async () => { calls++; }), /approved native run configuration/);
  assert.equal(calls, 0);
});
