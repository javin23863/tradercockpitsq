import assert from "node:assert/strict";
import test from "node:test";

import {
  fetchNativeExecution,
  fetchNativeJobsForConfiguration,
  launchApprovedBuilder,
  nativeExecutionFromStatus,
  nativeJobCatalogFromPayload,
  nativeJobFromPayload,
} from "../web/research-build-launch.mjs";

const digest = "a".repeat(64);
const launcher = "b".repeat(64);
const configurationRevision = `tc-research-revision:configuration:sha256:${"c".repeat(64)}`;
const configurationEntity = "tc-research:configuration:v1:11111111-1111-4111-8111-111111111111";

function response(payload, { ok = true, status = 200 } = {}) {
  return {
    ok,
    status,
    async json() { return payload; },
  };
}

function job(overrides = {}) {
  return {
    schema: "tc.research-native-job.v1",
    entity_id: "tc-research:native-job:v1:22222222-2222-4222-8222-222222222222",
    revision: `tc-research-revision:native-job:sha256:${"d".repeat(64)}`,
    parent_revision: `tc-research-revision:native-job:sha256:${"e".repeat(64)}`,
    state: "submitted",
    configuration_entity_id: configurationEntity,
    configuration_revision: configurationRevision,
    executable_xml_ref: `tc-evidence:sha256:${digest}`,
    executable_xml_sha256: digest,
    sqx_build: "144.2953",
    operation: "builder_loadconfig_start",
    staged_config_relative_path: `user/TraderCockpit/approved-configurations/${digest.slice(0, 2)}/${digest}.xml`,
    launcher_sha256: launcher,
    partial_side_effect: false,
    failure_reason_code: null,
    receipts: [
      { sequence: 1, action: "loadconfig", project: "Builder", state: "completed", exit_code: 0, sqx_build: "144.2953", launcher_sha256: launcher, config_sha256: digest, reason_code: null },
      { sequence: 2, action: "start", project: "Builder", state: "completed", exit_code: 0, sqx_build: "144.2953", launcher_sha256: launcher, config_sha256: digest, reason_code: null },
    ],
    reused: false,
    ...overrides,
  };
}

test("runtime status enables native control only with verified bound gateway", () => {
  const ready = nativeExecutionFromStatus({
    schema: "tc.runtime-status.v1",
    research_backend: {
      execution: {
        available: true,
        gateway_available: true,
        launcher_verified: true,
        launcher_sha256: launcher,
        reason_code: null,
      },
    },
  });
  assert.equal(ready.available, true);
  assert.equal(ready.launcher_sha256, launcher);

  const unavailable = nativeExecutionFromStatus({
    schema: "tc.runtime-status.v1",
    research_backend: {
      execution: {
        available: false,
        gateway_available: true,
        launcher_verified: true,
        launcher_sha256: launcher,
        reason_code: "native_execution_unavailable",
      },
    },
  });
  assert.equal(unavailable.available, false);
  assert.equal(unavailable.reason_code, "native_execution_unavailable");
  assert.equal(unavailable.launcher_sha256, null);
});

test("submitted native job requires exact control receipts", () => {
  assert.equal(nativeJobFromPayload(job()).state, "submitted");
  assert.throws(
    () => nativeJobFromPayload(job({ receipts: [job().receipts[0]] })),
    /Submitted native job receipt is inconsistent/,
  );
  assert.throws(
    () => nativeJobFromPayload(job({ executable_xml_ref: `tc-evidence:sha256:${"f".repeat(64)}` })),
    /Native job executable evidence is inconsistent/,
  );
});

test("supervised CFX submission binds archive receipts separately from approved settings", () => {
  const archive = "f".repeat(64);
  const packed = job({
    staged_config_ref: `tc-evidence:sha256:${archive}`,
    staged_config_sha256: archive,
    staged_config_relative_path: `user/TraderCockpit/approved-configurations/ff/${archive}.cfx`,
    receipts: job().receipts.map((receipt, index) => ({ ...receipt, config_sha256: archive, exit_code: index === 0 ? 0 : null })),
  });
  assert.equal(nativeJobFromPayload(packed).executable_xml_sha256, digest);
  for (const invalid of [
    { staged_config_ref: `tc-evidence:sha256:${digest}` },
    { receipts: job().receipts },
    { receipts: [...packed.receipts].reverse() },
    { receipts: packed.receipts.map((receipt) => ({ ...receipt, launcher_sha256: digest })) },
  ]) assert.throws(() => nativeJobFromPayload({ ...packed, ...invalid }), /inconsistent/);
});

test("native job catalog validates each job", () => {
  const jobs = nativeJobCatalogFromPayload({ schema: "tc.research-native-job-catalog.v1", jobs: [job()] });
  assert.equal(jobs.length, 1);
  assert.equal(jobs[0].configuration_revision, configurationRevision);
  assert.throws(
    () => nativeJobCatalogFromPayload({ schema: "wrong", jobs: [] }),
    /Native job catalog schema mismatch/,
  );
});

test("launch sends only exact approved configuration identity and validates returned binding", async () => {
  const configuration = {
    schema: "tc.research-configuration.v1",
    entity_id: configurationEntity,
    revision: configurationRevision,
    state: "approved",
    approval: { approved: true },
    executable_xml_sha256: digest,
  };
  let request;
  const launched = await launchApprovedBuilder(configuration, async (url, options) => {
    request = { url, options };
    return response(job());
  });
  assert.equal(request.url, "/api/research/native-jobs");
  assert.equal(request.options.method, "POST");
  assert.deepEqual(JSON.parse(request.options.body), {
    action: "launch-builder",
    configuration_entity_id: configurationEntity,
    expected_configuration_revision: configurationRevision,
  });
  assert.equal(launched.state, "submitted");

  await assert.rejects(
    launchApprovedBuilder(configuration, async () => response(job({ configuration_revision: `tc-research-revision:configuration:sha256:${"9".repeat(64)}` }))),
    /Native job does not bind the selected approved configuration/,
  );
});

test("native read helpers use canonical status and configuration-revision selectors", async () => {
  const calls = [];
  const fetchImpl = async (url) => {
    calls.push(url);
    if (url === "/api/status") {
      return response({
        schema: "tc.runtime-status.v1",
        research_backend: {
          execution: {
            available: true,
            gateway_available: true,
            launcher_verified: true,
            launcher_sha256: launcher,
            reason_code: null,
          },
        },
      });
    }
    return response({ schema: "tc.research-native-job-catalog.v1", jobs: [job()] });
  };

  const execution = await fetchNativeExecution(fetchImpl);
  const jobs = await fetchNativeJobsForConfiguration(configurationRevision, fetchImpl);
  assert.equal(execution.available, true);
  assert.equal(jobs.length, 1);
  assert.deepEqual(calls, [
    "/api/status",
    `/api/research/native-jobs?configurationRevision=${encodeURIComponent(configurationRevision)}`,
  ]);
});
