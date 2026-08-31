import assert from "node:assert/strict";
import test from "node:test";

import {
  candidateFromPayload,
  historicalResultCatalogFromPayload,
  historicalResultFromPayload,
  retesterRuntimeReady,
  startRetester,
} from "../web/research-backtest.mjs";

const candidateArchiveSha = "a".repeat(64);
const resultArchiveSha = "b".repeat(64);
const projectSha = "c".repeat(64);
const engineSha = "d".repeat(64);
const launcherSha = "e".repeat(64);
const resultStrategySha = "f".repeat(64);
const resultSettingsSha = "1".repeat(64);
const candidateEntity = "tc-research:candidate:v1:11111111-1111-4111-8111-111111111111";
const candidateRevision = `tc-research-revision:candidate:sha256:${"2".repeat(64)}`;
const projectName = "TraderCockpit-Retester-33333333333343338333333333333333";

function candidate(overrides = {}) {
  return {
    schema: "tc.research-candidate.v1",
    entity_id: candidateEntity,
    revision: candidateRevision,
    archive_name: "Survivor.sqx",
    archive_ref: `tc-evidence:sha256:${candidateArchiveSha}`,
    archive_sha256: candidateArchiveSha,
    sqx_build: "144.2953",
    ...overrides,
  };
}

function result(overrides = {}) {
  return {
    schema: "tc.research-historical-result.v1",
    entity_id: "tc-research:historical-result:v1:33333333-3333-4333-8333-333333333333",
    revision: `tc-research-revision:historical-result:sha256:${"3".repeat(64)}`,
    parent_revision: `tc-research-revision:historical-result:sha256:${"4".repeat(64)}`,
    state: "completed",
    candidate_entity_id: candidateEntity,
    candidate_revision: candidateRevision,
    candidate_archive_name: "Survivor.sqx",
    candidate_archive_ref: `tc-evidence:sha256:${candidateArchiveSha}`,
    candidate_archive_sha256: candidateArchiveSha,
    sqx_build: "144.2953",
    operation: "native_retester_task_1",
    retester_task: 1,
    native_project_name: projectName,
    native_project_relative_path: `user/projects/${projectName}/project.cfx`,
    source_project_ref: `tc-evidence:sha256:${projectSha}`,
    source_project_sha256: projectSha,
    engine_ref: `tc-evidence:sha256:${engineSha}`,
    engine_sha256: engineSha,
    launcher_sha256: launcherSha,
    receipts: [{ action: "startOnlyTask", state: "completed", task: 1, exit_code: 0 }],
    partial_side_effect: false,
    result_archive_name: "Survivor.sqx",
    result_archive_relative_path: `user/projects/${projectName}/databanks/Results/Survivor.sqx`,
    result_archive_ref: `tc-evidence:sha256:${resultArchiveSha}`,
    result_archive_sha256: resultArchiveSha,
    result_strategy_ref: `tc-evidence:sha256:${resultStrategySha}`,
    result_strategy_sha256: resultStrategySha,
    result_settings_ref: `tc-evidence:sha256:${resultSettingsSha}`,
    result_settings_sha256: resultSettingsSha,
    failure_reason_code: null,
    execution_completed: true,
    validation_state: "not_run",
    reused: false,
    ...overrides,
  };
}

function response(payload, { ok = true, status = 200 } = {}) {
  return { ok, status, async json() { return payload; } };
}

test("historical result cross-checks exact producer evidence and never infers validation", () => {
  const parsed = historicalResultFromPayload(result());
  assert.equal(parsed.execution_completed, true);
  assert.equal(parsed.validation_state, "not_run");
  assert.throws(
    () => historicalResultFromPayload(result({ validation_state: "passed" })),
    /receipt\/validation state is invalid/,
  );
  assert.throws(
    () => historicalResultFromPayload(result({ result_archive_ref: `tc-evidence:sha256:${"9".repeat(64)}` })),
    /Completed historical result is inconsistent/,
  );
  assert.throws(
    () => historicalResultFromPayload(result({ result_archive_sha256: candidateArchiveSha, result_archive_ref: `tc-evidence:sha256:${candidateArchiveSha}` })),
    /Completed historical result is inconsistent/,
  );
});

test("historical-result catalog validates each durable run", () => {
  const parsed = historicalResultCatalogFromPayload({
    schema: "tc.research-historical-result-catalog.v1",
    results: [result()],
  });
  assert.equal(parsed.length, 1);
  assert.throws(
    () => historicalResultCatalogFromPayload({ schema: "wrong", results: [] }),
    /schema mismatch/,
  );
});

test("Retester UI requires verified runtime plus trusted gateway readiness", () => {
  const ready = {
    schema: "tc.runtime-status.v1",
    research_backend: {
      verified: true,
      execution: { gateway_available: true, launcher_verified: true },
    },
  };
  assert.equal(retesterRuntimeReady(ready), true);
  assert.equal(retesterRuntimeReady({ ...ready, research_backend: { ...ready.research_backend, execution: { gateway_available: true, launcher_verified: false } } }), false);
  assert.equal(retesterRuntimeReady({ ...ready, research_backend: { ...ready.research_backend, verified: false } }), false);
});

test("Retester start sends only exact Candidate identity and validates returned binding", async () => {
  const selected = candidateFromPayload(candidate());
  let request;
  const historical = await startRetester(selected, async (url, options) => {
    request = { url, options };
    return response(result(), { status: 201 });
  });

  assert.equal(request.url, "/api/research/historical-results");
  assert.equal(request.options.method, "POST");
  assert.deepEqual(JSON.parse(request.options.body), {
    action: "start-retester",
    candidate_entity_id: candidateEntity,
    expected_candidate_revision: candidateRevision,
  });
  assert.equal(historical.candidate_revision, candidateRevision);

  await assert.rejects(
    startRetester(selected, async () => response(result({ candidate_revision: `tc-research-revision:candidate:sha256:${"8".repeat(64)}` }))),
    /does not bind the selected Candidate revision/,
  );
});
