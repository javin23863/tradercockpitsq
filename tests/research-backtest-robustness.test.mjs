import assert from "node:assert/strict";
import test from "node:test";

import {
  fetchRobustnessCapabilities,
  fetchRobustnessCatalog,
  fetchRobustnessResult,
  robustnessAttemptFromPayload,
  robustnessAttemptsForHistorical,
  robustnessAttemptRefFromStartError,
  fetchRobustnessAttemptForStartError,
  robustnessExecutionAvailable,
  robustnessOperationIsCurrent,
  robustnessCompletedHistoricalResults,
  robustnessCurrentSourceIndex,
  robustnessUnboundSourceSelection,
  robustnessKeepAttemptRef,
  robustnessStartErrorDetail,
  robustnessStartFailureState,
  robustnessCapabilitiesFromPayload,
  robustnessCatalogFromPayload,
  robustnessResultForHistorical,
  robustnessResultsForHistorical,
  robustnessResultFromPayload,
  startHigherPrecision,
  startAdditionalMarkets,
  startWalkForward,
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
      project: projectName,
      project_sha256: compiledProjectSha,
      engine_sha256: engineSha,
      result_archive_sha256: sourceArchiveSha,
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
    proof_entity_id: "tc-research:proof:v1:33333333-3333-4333-8333-333333333333",
    proof_revision: `tc-research-revision:proof:sha256:${"a".repeat(64)}`,
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
  for (const [field, value] of [["project_sha256", "0".repeat(64)], ["engine_sha256", "0".repeat(64)], ["result_archive_sha256", "0".repeat(64)]]) {
    const valuePayload = robustness();
    valuePayload.receipts = [{ ...valuePayload.receipts[0], [field]: value }];
    assert.throws(() => robustnessResultFromPayload(valuePayload), /custody is inconsistent/);
  }
});

test("Higher Precision start sends only exact Historical Result identity and verifies returned binding", async () => {
  let request;
  const result = await startHigherPrecision(historical(), async (url, options) => {
    request = { url, options };
    return response(robustness(), { status: 201 });
  });

  assert.equal(request.url, "/api/research/historical-results");
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

test("Additional Markets start sends only exact Historical Result identity and verifies method binding", async () => {
  const amResult = robustness({
    method: "RetestOnAdditionalMarkets",
    native_settings: { markets: [{ symbol: "EURUSD", timeframe: "H1", dateFrom: "2003.5.5", dateTo: "2018.08.30" }] },
  });
  let request;
  const result = await startAdditionalMarkets(historical(), async (url, options) => {
    request = { url, options };
    return response(amResult, { status: 201 });
  });
  assert.deepEqual(JSON.parse(request.options.body), {
    action: "start-additional-markets",
    historical_result_entity_id: historicalEntity,
    expected_historical_result_revision: historicalRevision,
  });
  assert.equal(result.method, "RetestOnAdditionalMarkets");
  await assert.rejects(
    startAdditionalMarkets(historical(), async () => response(robustness(), { status: 201 })),
    /does not bind the selected Historical Result revision/,
  );
});

test("Walk-Forward start sends only exact Historical Result identity and verifies method binding", async () => {
  const wfResult = robustness({
    method: "WalkForwardOptimization",
    native_settings: { type: "1", period: "10", optimization: "15", MaxTests: "100" },
  });
  let request;
  const result = await startWalkForward(historical(), async (url, options) => {
    request = { url, options };
    return response(wfResult, { status: 201 });
  });
  assert.deepEqual(JSON.parse(request.options.body), {
    action: "start-walk-forward",
    historical_result_entity_id: historicalEntity,
    expected_historical_result_revision: historicalRevision,
  });
  assert.equal(result.method, "WalkForwardOptimization");
});

test("robustness reopen sends exact validation evidence through historical-result command boundary", async () => {
  let requested;
  const validationRef = `tc-evidence:sha256:${validationSha}`;
  const result = await fetchRobustnessResult(validationRef, async (url, options) => {
    requested = { url, options };
    return response(robustness());
  });
  assert.equal(requested.url, "/api/research/historical-results");
  assert.equal(requested.options.method, "POST");
  assert.deepEqual(JSON.parse(requested.options.body), {
    action: "read-robustness",
    validation_ref: validationRef,
  });
  assert.equal(result.validation_ref, validationRef);

  await assert.rejects(
    fetchRobustnessResult("not-an-evidence-ref", async () => { throw new Error("must not fetch"); }),
    /validation reference is invalid/,
  );
});

test("robustness capabilities and catalog are backend read models", async () => {
  const unavailable = (method, reason) => ({
    method,
    state: "unavailable",
    reason_code: reason,
    detail: "installed Retester project does not contain one profile; configure/save it in SQX first",
    native_settings: null,
    configuration_changed: null,
    source_project_sha256: null,
    compiled_project_sha256: null,
    engine_sha256: null,
  });
  const capabilityPayload = {
    schema: "tc.research-native-robustness-capabilities.v1",
    sqx_build: "144.2953",
    methods: [{
      method: "RetestWithHigherPrecision",
      state: "ready",
      reason_code: null,
      detail: "installed producer profile is usable",
      native_settings: { Precision: "4", Spread: "7" },
      configuration_changed: false,
      source_project_sha256: "b".repeat(64),
      compiled_project_sha256: "b".repeat(64),
      engine_sha256: "c".repeat(64),
    }, unavailable("RetestOnAdditionalMarkets", "robustness_additional_markets_missing"),
    unavailable("MonteCarloRetest", "robustness_monte_carlo_retest_missing"),
    unavailable("WalkForwardOptimization", "robustness_walk_forward_missing"),
    unavailable("WalkForwardMatrix", "robustness_walk_forward_matrix_missing"),
    unavailable("WhatIf", "robustness_what_if_missing"),
    unavailable("OptProfileSysParamPermutation", "robustness_permutation_missing"),
    unavailable("MonteCarloManipulation", "robustness_monte_carlo_manipulation_invalid"),
    unavailable("SequentialOptimization", "robustness_sequential_optimization_invalid")],
  };
  assert.equal(robustnessCapabilitiesFromPayload(capabilityPayload).methods[0].native_settings.Precision, "4");
  assert.throws(
    () => robustnessCapabilitiesFromPayload({
      ...capabilityPayload,
      methods: [{ ...capabilityPayload.methods[0], native_settings: null }, ...capabilityPayload.methods.slice(1)],
    }),
    /inconsistent/,
  );

  let capabilityRequest;
  await fetchRobustnessCapabilities(async (url, options) => {
    capabilityRequest = { url, options };
    return response(capabilityPayload);
  });
  assert.deepEqual(JSON.parse(capabilityRequest.options.body), { action: "read-robustness-capabilities" });

  const failedAttempt = {
    schema: "tc.research-native-robustness-attempt.v1", state: "failed", sqx_build: "144.2953", operation: "native_retester_cross_check", method: "RetestWithHigherPrecision",
    attempt_ref: `tc-evidence:sha256:${"0".repeat(64)}`, proof_entity_id: "tc-research:proof:v1:55555555-5555-4555-8555-555555555555", proof_revision: `tc-research-revision:proof:sha256:${"1".repeat(64)}`,
    source_historical_result_entity_id: historicalEntity, source_historical_result_revision: historicalRevision,
    source_result_archive_ref: `tc-evidence:sha256:${sourceArchiveSha}`, source_result_archive_sha256: sourceArchiveSha,
    source_project_ref: `tc-evidence:sha256:${sourceProjectSha}`, source_project_sha256: sourceProjectSha,
    compiled_project_ref: `tc-evidence:sha256:${compiledProjectSha}`, compiled_project_sha256: compiledProjectSha,
    configuration_changed: true, source_task_sha256: sourceTaskSha, compiled_task_sha256: compiledTaskSha, native_settings: { Precision: "2", Spread: "3" },
    engine_ref: `tc-evidence:sha256:${engineSha}`, engine_sha256: engineSha, launcher_sha256: launcherSha,
    native_project_name: projectName, native_project_relative_path: `user/projects/${projectName}/project.cfx`,
    failure_reason_code: "sqx_command_timeout", partial_side_effect: true, receipts: [{
      action: "startOnlyTask", task: 1, project: projectName, state: "timeout", launcher_sha256: launcherSha,
      project_sha256: compiledProjectSha, engine_sha256: engineSha, result_archive_sha256: sourceArchiveSha,
    }],
  };
  assert.equal(robustnessAttemptFromPayload(failedAttempt).failure_reason_code, "sqx_command_timeout");
  assert.throws(
    () => robustnessAttemptFromPayload({ ...failedAttempt, receipts: [{ ...failedAttempt.receipts[0], result_archive_sha256: "0".repeat(64) }] }),
    /receipt is inconsistent/,
  );
  const catalogPayload = { schema: "tc.research-native-robustness-catalog.v1", results: [robustness()], failed_attempts: [failedAttempt] };
  const parsedCatalog = robustnessCatalogFromPayload(catalogPayload);
  assert.equal(parsedCatalog.results[0].validation_ref, `tc-evidence:sha256:${validationSha}`);
  assert.equal(parsedCatalog.failedAttempts[0].attempt_ref, failedAttempt.attempt_ref);
  let catalogRequest;
  await fetchRobustnessCatalog(async (url, options) => {
    catalogRequest = { url, options };
    return response(catalogPayload);
  });
  assert.deepEqual(JSON.parse(catalogRequest.options.body), { action: "list-robustness" });
});



test("robustness catalog selection binds to the selected Historical Result revision", () => {
  const first = historical();
  const second = {
    ...historical(),
    entity_id: "tc-research:historical-result:v1:44444444-4444-4444-8444-444444444444",
    revision: `tc-research-revision:historical-result:sha256:${"4".repeat(64)}`,
    result_archive_ref: `tc-evidence:sha256:${"5".repeat(64)}`,
    result_archive_sha256: "5".repeat(64),
  };
  const firstRun = robustness();
  const secondRun = {
    ...robustness(),
    validation_ref: `tc-evidence:sha256:${"6".repeat(64)}`,
    source_historical_result_entity_id: second.entity_id,
    source_historical_result_revision: second.revision,
    source_result_archive_ref: second.result_archive_ref,
    source_result_archive_sha256: second.result_archive_sha256,
    receipts: [{ ...robustness().receipts[0], result_archive_sha256: second.result_archive_sha256 }],
    result_archive_ref: `tc-evidence:sha256:${"7".repeat(64)}`,
    result_archive_sha256: "7".repeat(64),
  };
  const catalog = [secondRun, firstRun].map(robustnessResultFromPayload);
  assert.equal(robustnessResultForHistorical(catalog, first)?.validation_ref, firstRun.validation_ref);
  assert.equal(robustnessResultForHistorical(catalog, second)?.validation_ref, secondRun.validation_ref);
});


test("native start responses are generation-bound and source-currentness uses fresh results", () => {
  assert.equal(robustnessOperationIsCurrent(7, 7, true), true);
  assert.equal(robustnessOperationIsCurrent(7, 8, true), false);
  assert.equal(robustnessOperationIsCurrent(7, 7, false), false);

  const current = historical();
  assert.equal(robustnessCurrentSourceIndex([current], { entity_id: current.entity_id, revision: current.revision }), 0);
  const advanced = { ...current, revision: `tc-research-revision:historical-result:sha256:${"0".repeat(64)}` };
  assert.equal(robustnessCurrentSourceIndex([advanced], { entity_id: current.entity_id, revision: current.revision }), -1);

  const preceding = historical({
    entity_id: "tc-research:historical-result:v1:aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
    revision: `tc-research-revision:historical-result:sha256:${"a".repeat(64)}`,
    state: "failed",
    execution_completed: false,
  });
  const later = historical({
    entity_id: "tc-research:historical-result:v1:bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
    revision: `tc-research-revision:historical-result:sha256:${"b".repeat(64)}`,
  });
  const mixed = [preceding, current, later];
  assert.equal(robustnessCompletedHistoricalResults(mixed).length, 2);
  assert.equal(robustnessCurrentSourceIndex(mixed, { entity_id: current.entity_id, revision: current.revision }), 0);
  assert.equal(robustnessCurrentSourceIndex(mixed, { entity_id: later.entity_id, revision: later.revision }), 1);
  assert.equal(robustnessCurrentSourceIndex(mixed, { entity_id: preceding.entity_id, revision: preceding.revision }), -1);
});


test("robustness result requires durable proof identity", () => {
  assert.throws(
    () => robustnessResultFromPayload(robustness({ proof_entity_id: undefined, proof_revision: undefined })),
    /identity is invalid|proof custody is inconsistent/,
  );
});

test("multiple robustness runs for one baseline require exact validation selection", () => {
  const source = historical();
  const first = robustness();
  const second = robustness({
    validation_ref: `tc-evidence:sha256:${"8".repeat(64)}`,
    proof_entity_id: "tc-research:proof:v1:44444444-4444-4444-8444-444444444444",
    proof_revision: `tc-research-revision:proof:sha256:${"b".repeat(64)}`,
    result_archive_ref: `tc-evidence:sha256:${"9".repeat(64)}`,
    result_archive_sha256: "9".repeat(64),
  });
  const catalog = [first, second].map(robustnessResultFromPayload);
  assert.equal(robustnessResultsForHistorical(catalog, source).length, 2);
  assert.equal(robustnessResultForHistorical(catalog, source), null);
  assert.equal(
    robustnessResultForHistorical(catalog, source, second.validation_ref)?.validation_ref,
    second.validation_ref,
  );
});


test("interrupted attempts remain readable and attempt lists are exact-baseline scoped", () => {
  const interrupted = {
    schema: "tc.research-native-robustness-attempt.v1", state: "interrupted", sqx_build: "144.2953", operation: "native_retester_cross_check", method: "RetestWithHigherPrecision",
    attempt_ref: `tc-evidence:sha256:${"0".repeat(64)}`, proof_entity_id: "tc-research:proof:v1:55555555-5555-4555-8555-555555555555", proof_revision: `tc-research-revision:proof:sha256:${"1".repeat(64)}`,
    source_historical_result_entity_id: historicalEntity, source_historical_result_revision: historicalRevision,
    source_result_archive_ref: `tc-evidence:sha256:${sourceArchiveSha}`, source_result_archive_sha256: sourceArchiveSha,
    source_project_ref: `tc-evidence:sha256:${sourceProjectSha}`, source_project_sha256: sourceProjectSha,
    compiled_project_ref: `tc-evidence:sha256:${compiledProjectSha}`, compiled_project_sha256: compiledProjectSha,
    configuration_changed: true, source_task_sha256: sourceTaskSha, compiled_task_sha256: compiledTaskSha, native_settings: { Precision: "2", Spread: "3" },
    engine_ref: `tc-evidence:sha256:${engineSha}`, engine_sha256: engineSha, launcher_sha256: null,
    native_project_name: projectName, native_project_relative_path: `user/projects/${projectName}/project.cfx`,
    failure_reason_code: "robustness_attempt_interrupted", partial_side_effect: true, receipts: [],
  };
  const parsed = robustnessAttemptFromPayload(interrupted);
  assert.equal(parsed.state, "interrupted");

  const other = {
    ...interrupted,
    attempt_ref: `tc-evidence:sha256:${"9".repeat(64)}`,
    source_historical_result_entity_id: "tc-research:historical-result:v1:44444444-4444-4444-8444-444444444444",
    source_historical_result_revision: `tc-research-revision:historical-result:sha256:${"8".repeat(64)}`,
  };
  assert.deepEqual(robustnessAttemptsForHistorical([parsed, other], historical()).map((item) => item.attempt_ref), [parsed.attempt_ref]);
});


test("failed start uses only backend originating attempt identity and revokes launch authority", async () => {
  const source = historical();
  const exactAttempt = {
    schema: "tc.research-native-robustness-attempt.v1", state: "failed", sqx_build: "144.2953", operation: "native_retester_cross_check", method: "RetestWithHigherPrecision",
    attempt_ref: `tc-evidence:sha256:${"1".repeat(64)}`, proof_entity_id: "tc-research:proof:v1:55555555-5555-4555-8555-555555555555", proof_revision: `tc-research-revision:proof:sha256:${"2".repeat(64)}`,
    source_historical_result_entity_id: source.entity_id, source_historical_result_revision: source.revision,
    source_result_archive_ref: `tc-evidence:sha256:${sourceArchiveSha}`, source_result_archive_sha256: sourceArchiveSha,
    source_project_ref: `tc-evidence:sha256:${sourceProjectSha}`, source_project_sha256: sourceProjectSha,
    compiled_project_ref: `tc-evidence:sha256:${compiledProjectSha}`, compiled_project_sha256: compiledProjectSha,
    configuration_changed: true, source_task_sha256: sourceTaskSha, compiled_task_sha256: compiledTaskSha, native_settings: { Precision: "2", Spread: "3" },
    engine_ref: `tc-evidence:sha256:${engineSha}`, engine_sha256: engineSha, launcher_sha256: launcherSha,
    native_project_name: projectName, native_project_relative_path: `user/projects/${projectName}/project.cfx`,
    failure_reason_code: "sqx_command_timeout", partial_side_effect: true, receipts: [{
      action: "startOnlyTask", task: 1, project: projectName, state: "timeout", launcher_sha256: launcherSha,
      project_sha256: compiledProjectSha, engine_sha256: engineSha, result_archive_sha256: sourceArchiveSha,
    }],
  };
  const error = new Error("failed");
  error.payload = { attempt_ref: exactAttempt.attempt_ref };
  assert.equal(robustnessAttemptRefFromStartError(error), exactAttempt.attempt_ref);
  assert.equal(robustnessKeepAttemptRef(error, null), exactAttempt.attempt_ref);
  assert.equal(
    robustnessStartErrorDetail(error, ["failed-attempt readback failed"]),
    "failed Durable failed-Proof recovery failed: failed-attempt readback failed.",
  );
  let requestedBody = null;
  const fetched = await fetchRobustnessAttemptForStartError(error, source, async (_url, options) => {
    requestedBody = JSON.parse(options.body);
    return response(exactAttempt);
  });
  assert.deepEqual(requestedBody, { action: "read-robustness", validation_ref: exactAttempt.attempt_ref });
  assert.equal(fetched.attempt_ref, exactAttempt.attempt_ref);
  assert.equal(robustnessAttemptRefFromStartError({ payload: { attempt_ref: "not-an-evidence-ref" } }), "");

  const failedState = robustnessStartFailureState(
    { phase: "running", runtimeReady: true, capabilities: { methods: [{ state: "ready" }] }, validation: null, suppressCompletedPicker: true, inFlightSource: source, failedAttempts: [] },
    fetched,
    [fetched],
    "native start failed",
  );
  assert.equal(failedState.phase, "failed");
  assert.equal(failedState.runtimeReady, false);
  assert.equal(failedState.capabilities, null);
  assert.equal(failedState.validation.attempt_ref, exactAttempt.attempt_ref);
  assert.equal(robustnessExecutionAvailable(failedState.phase, true, { state: "ready" }, source), false);
  assert.equal(robustnessExecutionAvailable("loaded", true, { state: "ready" }, source), true);
});

test("unbound source selection does not substitute another completed Historical Result", () => {
  const missing = historical({
    entity_id: "tc-research:historical-result:v1:aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
    revision: `tc-research-revision:historical-result:sha256:${"a".repeat(64)}`,
  });
  const other = historical();
  const unbound = robustnessUnboundSourceSelection([other]);
  assert.equal(robustnessCurrentSourceIndex([other], { entity_id: missing.entity_id, revision: missing.revision }), -1);
  assert.equal(unbound.selectedIndex, -1);
  assert.equal(unbound.selected, null);
  assert.equal(unbound.canRun, false);
});
