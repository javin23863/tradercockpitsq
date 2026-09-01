import assert from "node:assert/strict";
import test from "node:test";

import {
  fetchRobustnessResult,
  robustnessResultFromPayload,
  startHigherPrecision,
} from "../web/research-backtest-robustness.mjs";

const sourceArchiveSha = "a".repeat(64);
const sourceProjectSha = "b".repeat(64);
const compiledProjectSha = "c".repeat(64);
const sourceTaskSha = "d".repeat(64);
const compiledTaskSha = "e".repeat(64);
const engineSha = "f".repeat(64);
const launcherSha = "1".repeat(64);
const resultArchiveSha = "2".repeat(64);
const resultStrategySha = "3".repeat(64);
const resultSettingsSha = "4".repeat(64);
const validationSha = "5".repeat(64);
const historicalEntity = "tc-research:historical-result:v1:11111111-1111-4111-8111-111111111111";
const historicalRevision = `tc-research-revision:historical-result:sha256:${"6".repeat(64)}`;
const projectName = "TraderCockpit-Retester-77777777777747778777777777777777";

function historical(overrides = {}) {
  return {
    schema: "tc.research-historical-result.v1",
    entity_id: historicalEntity,
    revision: historicalRevision,
    parent_revision: `tc-research-revision:historical-result:sha256:${"7".repeat(64)}`,
    state: "completed",
    candidate_entity_id: "tc-research:candidate:v1:22222222-2222-4222-8222-222222222222",
    candidate_revision: `tc-research-revision:candidate:sha256:${"8".repeat(64)}`,
    candidate_archive_name: "Candidate.sqx",
    candidate_archive_ref: `tc-evidence:sha256:${"9".repeat(64)}`,
    candidate_archive_sha256: "9".repeat(64),
    sqx_build: "144.2953",
    operation: "native_retester_task_1",
    retester_task: 1,
    native_project_name: "TraderCockpit-Retester-aaaaaaaaaaaa4aaa8aaaaaaaaaaaaaaa",
    native_project_relative_path: "user/projects/TraderCockpit-Retester-aaaaaaaaaaaa4aaa8aaaaaaaaaaaaaaa/project.cfx",
    source_project_ref: `tc-evidence:sha256:${"b".repeat(64)}`,
    source_project_sha256: "b".repeat(64),
    engine_ref: `tc-evidence:sha256:${"c".repeat(64)}`,
    engine_sha256: "c".repeat(64),
    launcher_sha256: "d".repeat(64),
    receipts: [{ action: "startOnlyTask", state: "completed", task: 1 }],
    partial_side_effect: false,
    result_archive_name: "Baseline.sqx",
    result_archive_relative_path: "user/projects/TraderCockpit-Retester-aaaaaaaaaaaa4aaa8aaaaaaaaaaaaaaa/databanks/Results/Baseline.sqx",
    result_archive_ref: `tc-evidence:sha256:${sourceArchiveSha}`,
    result_archive_sha256: sourceArchiveSha,
    result_strategy_ref: `tc-evidence:sha256:${"e".repeat(64)}`,
    result_strategy_sha256: "e".repeat(64),
    result_settings_ref: `tc-evidence:sha256:${"f".repeat(64)}`,
    result_settings_sha256: "f".repeat(64),
    failure_reason_code: null,
    execution_completed: true,
    validation_state: "not_run",
    reused: false,
    ...overrides,
  };
}

function robustness(overrides = {}) {
  return {
    schema: "tc.research-native-robustness.v1",
    validation_ref: `tc-evidence:sha256:${validationSha}`,
    sqx_build: "144.2953",
    operation: "native_retester_cross_check",
    method: "RetestWithHigherPrecision",
    source_historical_result_entity_id: historicalEntity,
    source_historical_result_revision: historicalRevision,
    source_result_archive_ref: `tc-evidence:sha256:${sourceArchiveSha}`,
    source_result_archive_sha256: sourceArchiveSha,
    source_project_ref: `tc-evidence:sha256:${sourceProjectSha}`,
    source_project_sha256: sourceProjectSha,
    compiled_project_ref: `tc-evidence:sha256:${compiledProjectSha}`,
    compiled_project_sha256: compiledProjectSha,
    configuration_changed: true,
    source_task_sha256: sourceTaskSha,
    compiled_task_sha256: compiledTaskSha,
    native_settings: { Precision: "2", Spread: "3" },
    engine_ref: `tc-evidence:sha256:${engineSha}`,
    engine_sha256: engineSha,
    launcher_sha256: launcherSha,
    native_project_name: projectName,
    native_project_relative_path: `user/projects/${projectName}/project.cfx`,
    receipts: [{
      action: "startOnlyTask",
      task: 1,
      state: "completed",
      sqx_build: "144.2953",
      launcher_sha256: launcherSha,
      project_sha256: compiledProjectSha,
      engine_sha256: engineSha,
    }],
    result_archive_name: "HigherPrecision.sqx",
    result_archive_ref: `tc-evidence:sha256:${resultArchiveSha}`,
    result_archive_sha256: resultArchiveSha,
    result_strategy_ref: `tc-evidence:sha256:${resultStrategySha}`,
    result_strategy_sha256: resultStrategySha,
    result_settings_ref: `tc-evidence:sha256:${resultSettingsSha}`,
    result_settings_sha256: resultSettingsSha,
    execution_state: "completed",
    producer_outcome_state: "producer_result_captured_outcome_unread",
    ...overrides,
  };
}

function response(payload, { ok = true, status = 200 } = {}) {
  return { ok, status, async json() { return payload; } };
}

test("robustness result accepts exact native custody but refuses fabricated pass state", () => {
  const parsed = robustnessResultFromPayload(robustness());
  assert.equal(parsed.execution_state, "completed");
  assert.equal(parsed.producer_outcome_state, "producer_result_captured_outcome_unread");
  assert.equal(parsed.native_settings.Precision, "2");

  assert.throws(
    () => robustnessResultFromPayload(robustness({ producer_outcome_state: "passed" })),
    /identity is invalid/,
  );
  assert.throws(
    () => robustnessResultFromPayload(robustness({ result_archive_sha256: sourceArchiveSha, result_archive_ref: `tc-evidence:sha256:${sourceArchiveSha}` })),
    /custody is inconsistent/,
  );
  assert.throws(
    () => robustnessResultFromPayload(robustness({ native_settings: { Precision: "", Spread: "3" } })),
    /custody is inconsistent/,
  );
});

test("Higher Precision start sends only exact Historical Result identity and verifies returned binding", async () => {
  let request;
  const result = await startHigherPrecision(historical(), async (url, options) => {
    request = { url, options };
    return response(robustness(), { status: 201 });
  });

  assert.equal(request.url, "/api/research/robustness");
  assert.equal(request.options.method, "POST");
  assert.deepEqual(JSON.parse(request.options.body), {
    action: "start-higher-precision",
    historical_result_entity_id: historicalEntity,
    expected_historical_result_revision: historicalRevision,
  });
  assert.equal(result.validation_ref, `tc-evidence:sha256:${validationSha}`);

  await assert.rejects(
    startHigherPrecision(historical(), async () => response(robustness({ source_historical_result_revision: `tc-research-revision:historical-result:sha256:${"0".repeat(64)}` }), { status: 201 })),
    /does not bind the selected Historical Result revision/,
  );
});

test("robustness reopen uses exact validation evidence reference", async () => {
  let requested;
  const validationRef = `tc-evidence:sha256:${validationSha}`;
  const result = await fetchRobustnessResult(validationRef, async (url, options) => {
    requested = { url, options };
    return response(robustness());
  });
  assert.equal(requested.url, `/api/research/robustness?validationRef=${encodeURIComponent(validationRef)}`);
  assert.equal(requested.options.headers.accept, "application/json");
  assert.equal(result.validation_ref, validationRef);

  await assert.rejects(
    fetchRobustnessResult("not-an-evidence-ref", async () => { throw new Error("must not fetch"); }),
    /validation reference is invalid/,
  );
});
