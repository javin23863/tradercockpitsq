import assert from "node:assert/strict";
import test from "node:test";

import {
  candidateChainFromPayload,
  configurationFromPayload,
  fetchExecutedConfigurationChain,
  fetchCompletedHistoricalResults,
  nativeJobFromPayload,
  verifyExecutedConfigurationChain,
} from "../web/research-backtest-configuration.mjs";

const hex = (value) => value.repeat(64).slice(0, 64);
const executionProof = { schema: "tc.sqx-retester-execution.v1", task_name: "Retest strategies", input_strategies: 1, tested_strategies: 1, passed_strategies: 0, failed_strategies: 1, stdout_sha256: "a".repeat(64), task_log_sha256: "b".repeat(64) };
const configEntity = "tc-research:configuration:v1:11111111-1111-4111-8111-111111111111";
const jobEntity = "tc-research:native-job:v1:22222222-2222-4222-8222-222222222222";
const candidateEntity = "tc-research:candidate:v1:33333333-3333-4333-8333-333333333333";
const resultEntity = "tc-research:historical-result:v1:44444444-4444-4444-8444-444444444444";
const configRevision = `tc-research-revision:configuration:sha256:${hex("a")}`;
const jobRevision = `tc-research-revision:native-job:sha256:${hex("b")}`;
const candidateRevision = `tc-research-revision:candidate:sha256:${hex("c")}`;
const resultRevision = `tc-research-revision:historical-result:sha256:${hex("d")}`;
const candidateArchiveSha = hex("1");
const resultArchiveSha = hex("2");
const xmlSha = hex("3");
const sourceProjectSha = hex("4");
const candidateStrategySha = hex("5");
const candidateSettingsSha = hex("6");
const resultStrategySha = hex("7");
const resultSettingsSha = hex("8");
const engineSha = hex("9");
const launcherSha = hex("e");
const retesterProjectSha = hex("f");
const evidence = (digest) => `tc-evidence:sha256:${digest}`;

function configuration(overrides = {}) {
  return {
    schema: "tc.research-configuration.v1",
    entity_id: configEntity,
    revision: configRevision,
    parent_revision: `tc-research-revision:configuration:sha256:${hex("0")}`,
    content_ref: `tc-research-content:configuration:sha256:${hex("a")}`,
    state: "approved",
    sqx_build: "144.2953",
    source_project_path: "user/projects/Builder/project.cfx",
    source_project_sha256: sourceProjectSha,
    source_project_ref: evidence(sourceProjectSha),
    source_entry: "Build-Task1.xml",
    source_entry_ref: evidence(xmlSha),
    executable_xml_ref: evidence(xmlSha),
    executable_xml_sha256: xmlSha,
    assembly_mode: "exact_native_builder_task_snapshot",
    approved_changes: [],
    review: { changed: false, summary: "exact" },
    approval: { approved: true, approved_from_revision: `tc-research-revision:configuration:sha256:${hex("0")}` },
    launch: { enabled: false, reason_code: "native_launch_not_in_this_slice" },
    ...overrides,
  };
}

function nativeJob(overrides = {}) {
  return {
    schema: "tc.research-native-job.v1",
    entity_id: jobEntity,
    revision: jobRevision,
    parent_revision: `tc-research-revision:native-job:sha256:${hex("0")}`,
    state: "submitted",
    configuration_entity_id: configEntity,
    configuration_revision: configRevision,
    executable_xml_ref: evidence(xmlSha),
    executable_xml_sha256: xmlSha,
    sqx_build: "144.2953",
    operation: "builder_loadconfig_start",
    staged_config_relative_path: `user/TraderCockpit/approved-configurations/${xmlSha.slice(0, 2)}/${xmlSha}.xml`,
    launcher_sha256: launcherSha,
    partial_side_effect: false,
    failure_reason_code: null,
    receipts: [
      { sequence: 1, action: "loadconfig", state: "completed" },
      { sequence: 2, action: "start", state: "completed" },
    ],
    ...overrides,
  };
}

function candidate(overrides = {}) {
  return {
    schema: "tc.research-candidate.v1",
    entity_id: candidateEntity,
    revision: candidateRevision,
    native_job_entity_id: jobEntity,
    native_job_revision: jobRevision,
    configuration_entity_id: configEntity,
    configuration_revision: configRevision,
    association_mode: "operator_selected_exact_native_output",
    archive_name: "Survivor.sqx",
    archive_relative_path: "user/projects/Builder/databanks/Results/Survivor.sqx",
    archive_ref: evidence(candidateArchiveSha),
    archive_sha256: candidateArchiveSha,
    strategy_ref: evidence(candidateStrategySha),
    strategy_sha256: candidateStrategySha,
    settings_ref: evidence(candidateSettingsSha),
    settings_sha256: candidateSettingsSha,
    sqx_build: "144.2953",
    ...overrides,
  };
}

function historicalResult(overrides = {}) {
  const nativeProject = "TraderCockpit-Retester-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
  return {
    schema: "tc.research-historical-result.v1",
    entity_id: resultEntity,
    revision: resultRevision,
    parent_revision: `tc-research-revision:historical-result:sha256:${hex("0")}`,
    state: "completed",
    candidate_entity_id: candidateEntity,
    candidate_revision: candidateRevision,
    candidate_archive_name: "Survivor.sqx",
    candidate_archive_ref: evidence(candidateArchiveSha),
    candidate_archive_sha256: candidateArchiveSha,
    sqx_build: "144.2953",
    operation: "native_retester_task_1",
    retester_task: 1,
    native_project_name: nativeProject,
    native_project_relative_path: `user/projects/${nativeProject}/project.cfx`,
    source_project_ref: evidence(retesterProjectSha),
    source_project_sha256: retesterProjectSha,
    engine_ref: evidence(engineSha),
    engine_sha256: engineSha,
    launcher_sha256: launcherSha,
    receipts: [{ sequence: 1, action: "start", state: "completed", task: 1, execution_proof: executionProof }],
    partial_side_effect: false,
    result_archive_name: "Retested.sqx",
    result_archive_relative_path: `user/projects/${nativeProject}/databanks/Results/Retested.sqx`,
    result_archive_ref: evidence(resultArchiveSha),
    result_archive_sha256: resultArchiveSha,
    result_strategy_ref: evidence(resultStrategySha),
    result_strategy_sha256: resultStrategySha,
    result_settings_ref: evidence(resultSettingsSha),
    result_settings_sha256: resultSettingsSha,
    failure_reason_code: null,
    execution_completed: true,
    execution_verification: "verified",
    validation_state: "not_run",
    ...overrides,
  };
}

function response(payload, status = 200) {
  return { ok: status >= 200 && status < 300, status, async json() { return payload; } };
}

test("executed configuration picker excludes unverified legacy results", async () => {
  const verified = historicalResult();
  const legacy = historicalResult({ execution_completed: false, execution_verification: "unverified", receipts: [{ action: "startOnlyTask", task: 1, state: "completed" }] });
  const results = await fetchCompletedHistoricalResults(async () => response({ schema: "tc.research-historical-result-catalog.v1", results: [legacy, verified] }));
  assert.deepEqual(results, [verified]);
});

test("exact approved configuration -> Builder job -> Candidate -> Retester result chain verifies", () => {
  const chain = verifyExecutedConfigurationChain({
    historicalResult: historicalResult(),
    candidate: candidate(),
    nativeJob: nativeJob(),
    configuration: configuration(),
  });
  assert.equal(chain.configuration.revision, configRevision);
  assert.equal(chain.nativeJob.executable_xml_sha256, xmlSha);
  assert.equal(chain.candidate.archive_sha256, candidateArchiveSha);
  assert.equal(chain.historicalResult.result_archive_sha256, resultArchiveSha);
  assert.equal(chain.historicalResult.validation_state, "not_run");
});

test("chain fails closed when Retester result points at a different Candidate revision", () => {
  assert.throws(
    () => verifyExecutedConfigurationChain({
      historicalResult: historicalResult({ candidate_revision: `tc-research-revision:candidate:sha256:${hex("0")}` }),
      candidate: candidate(), nativeJob: nativeJob(), configuration: configuration(),
    }),
    /does not preserve exact Candidate custody/,
  );
});

test("chain fails closed when Candidate points at a different Builder job", () => {
  assert.throws(
    () => verifyExecutedConfigurationChain({
      historicalResult: historicalResult(),
      candidate: candidate({ native_job_revision: `tc-research-revision:native-job:sha256:${hex("0")}` }),
      nativeJob: nativeJob(), configuration: configuration(),
    }),
    /does not preserve exact Builder job custody/,
  );
});

test("chain fails closed when submitted job executable bytes differ from approved configuration", () => {
  const other = hex("0");
  assert.throws(
    () => verifyExecutedConfigurationChain({
      historicalResult: historicalResult(), candidate: candidate(),
      nativeJob: nativeJob({ executable_xml_ref: evidence(other), executable_xml_sha256: other }),
      configuration: configuration(),
    }),
    /did not execute the approved configuration bytes/,
  );
});

test("parsers reject non-approved configuration and arbitrary Candidate archive paths", () => {
  assert.throws(() => configurationFromPayload(configuration({ state: "compiled", approval: { approved: false } })), /custody is invalid/);
  assert.throws(() => candidateChainFromPayload(candidate({ archive_relative_path: "C:/arbitrary/Survivor.sqx" })), /native archive path is invalid/);
  assert.throws(() => nativeJobFromPayload(nativeJob({ state: "failed" })), /custody is invalid/);
});

test("executed-chain read uses only exact canonical entity selectors", async () => {
  const requests = [];
  const byPath = new Map([
    [`/api/research/historical-results?entityId=${encodeURIComponent(resultEntity)}`, historicalResult()],
    [`/api/research/candidates?entityId=${encodeURIComponent(candidateEntity)}`, candidate()],
    [`/api/research/native-jobs?entityId=${encodeURIComponent(jobEntity)}`, nativeJob()],
    [`/api/research/configurations?entityId=${encodeURIComponent(configEntity)}`, configuration()],
  ]);
  const chain = await fetchExecutedConfigurationChain(resultEntity, async (url, options) => {
    requests.push({ url, options });
    const payload = byPath.get(url);
    return payload ? response(payload) : response({ error: "not_found" }, 404);
  });
  assert.equal(chain.historicalResult.entity_id, resultEntity);
  assert.deepEqual(new Set(requests.map((item) => item.url)), new Set(byPath.keys()));
  assert.equal(requests.every((item) => item.options.headers.accept === "application/json"), true);
  assert.equal(requests.some((item) => /latest|best|path=|file=/.test(item.url)), false);
});
