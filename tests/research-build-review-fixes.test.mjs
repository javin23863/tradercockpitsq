import assert from "node:assert/strict";
import test from "node:test";

import {
  ResearchBuildApiError,
  configurationFromPayload,
  configurationSelectionTarget,
  preserveCompiledStateAfterRefreshFailure,
  refreshConfigurationAfterConflict,
  renderBuildWorkspace,
} from "../web/research-build.mjs";

function record(overrides = {}) {
  return {
    schema: "tc.research-configuration.v1",
    entity_id: "tc-research:configuration:v1:11111111-1111-1111-1111-111111111111",
    revision: "tc-research-revision:configuration:sha256:" + "a".repeat(64),
    parent_revision: null,
    content_ref: "tc-evidence:sha256:" + "b".repeat(64),
    state: "compiled",
    sqx_build: "144.2953",
    source_project_path: "user/projects/Builder/project.cfx",
    source_project_sha256: "c".repeat(64),
    source_project_ref: "tc-evidence:sha256:" + "c".repeat(64),
    source_entry: "Build-Task1.xml",
    source_entry_ref: "tc-evidence:sha256:" + "d".repeat(64),
    executable_xml_ref: "tc-evidence:sha256:" + "d".repeat(64),
    executable_xml_sha256: "d".repeat(64),
    assembly_mode: "exact_native_builder_task_snapshot",
    approved_changes: [],
    review: { changed: false, summary: "Byte-identical native task snapshot." },
    approval: { approved: false, approved_from_revision: null },
    launch: { enabled: false, reason_code: "native_launch_not_in_this_slice" },
    ...overrides,
  };
}

function approvedRecord(overrides = {}) {
  const parent = "tc-research-revision:configuration:sha256:" + "a".repeat(64);
  return record({
    state: "approved",
    revision: "tc-research-revision:configuration:sha256:" + "e".repeat(64),
    parent_revision: parent,
    approval: { approved: true, approved_from_revision: parent },
    ...overrides,
  });
}

function response(payload, { status = 200, ok = true } = {}) {
  return { status, ok, json: async () => payload };
}

test("configuration response states are exact discriminated shapes", () => {
  assert.equal(configurationFromPayload(record()).state, "compiled");
  assert.equal(configurationFromPayload(approvedRecord()).state, "approved");

  assert.throws(
    () => configurationFromPayload(record({ state: "unknown" })),
    /state is invalid/,
  );
  assert.throws(
    () => configurationFromPayload(record({ parent_revision: "unexpected" })),
    /Compiled configuration approval shape is invalid/,
  );
  assert.throws(
    () => configurationFromPayload(record({ approval: { approved: true } })),
    /Compiled configuration approval shape is invalid/,
  );
  assert.throws(
    () => configurationFromPayload(approvedRecord({ approval: { approved: true, approved_from_revision: null } })),
    /Approved configuration approval shape is invalid/,
  );
  assert.throws(
    () => configurationFromPayload(approvedRecord({ parent_revision: null })),
    /Approved configuration approval shape is invalid/,
  );
});

test("configuration response cross-checks fixed paths and evidence digests", () => {
  assert.throws(
    () => configurationFromPayload(record({ source_project_path: "user/projects/Other/project.cfx" })),
    /identity is inconsistent/,
  );
  assert.throws(
    () => configurationFromPayload(record({ source_project_ref: "tc-evidence:sha256:" + "f".repeat(64) })),
    /identity is inconsistent/,
  );
  assert.throws(
    () => configurationFromPayload(record({ executable_xml_sha256: "e".repeat(64) })),
    /identity is inconsistent/,
  );
  assert.throws(
    () => configurationFromPayload(record({ source_entry_ref: "tc-evidence:sha256:" + "e".repeat(64) })),
    /identity is inconsistent/,
  );
  assert.throws(
    () => configurationFromPayload(record({ assembly_mode: "translated" })),
    /identity is inconsistent/,
  );
});

test("reload does not guess among multiple configuration identities", () => {
  const first = { entity_id: "first" };
  const second = { entity_id: "second" };
  assert.equal(configurationSelectionTarget([first], "", ""), "first");
  assert.equal(configurationSelectionTarget([first, second], "", ""), "");
  assert.equal(configurationSelectionTarget([first, second], "second", ""), "second");
  assert.equal(configurationSelectionTarget([first, second], "", "first"), "first");
});

test("catalog reselection controls are disabled while a read is pending", () => {
  const html = renderBuildWorkspace({
    phase: "loading",
    catalog: [{ entity_id: "first", revision: "revision-first", state: "compiled" }],
  });
  assert.match(html, /data-configuration-entity-id="first" disabled/);
});

test("successful compile remains selected when follow-up catalog refresh fails", () => {
  const compiled = record();
  const state = preserveCompiledStateAfterRefreshFailure(
    [{
      entity_id: "older",
      revision: "old-revision",
      state: "compiled",
      source_project_sha256: "1".repeat(64),
      executable_xml_sha256: "2".repeat(64),
    }],
    compiled,
    "Compiled revision is durable; catalog refresh failed: offline",
  );
  assert.equal(state.phase, "loaded");
  assert.equal(state.selected, compiled);
  assert.equal(state.catalog.at(-1).entity_id, compiled.entity_id);
  assert.equal(state.catalog.at(-1).revision, compiled.revision);
  assert.match(state.detail, /durable/);
});

test("approval conflict refresh reads authoritative catalog and current entity", async () => {
  const current = approvedRecord();
  const calls = [];
  const fakeFetch = async (url) => {
    calls.push(url);
    if (url === "/api/research/configurations") {
      return response({
        schema: "tc.research-configuration-catalog.v1",
        configurations: [{
          entity_id: current.entity_id,
          revision: current.revision,
          state: current.state,
          source_project_sha256: current.source_project_sha256,
          executable_xml_sha256: current.executable_xml_sha256,
        }],
      });
    }
    return response(current);
  };

  const state = await refreshConfigurationAfterConflict(
    current.entity_id,
    "configuration revision changed before approval",
    fakeFetch,
  );
  assert.equal(state.phase, "loaded");
  assert.equal(state.selected.revision, current.revision);
  assert.equal(state.selected.state, "approved");
  assert.match(state.detail, /changed before approval/);
  assert.deepEqual(calls, [
    "/api/research/configurations",
    `/api/research/configurations?entityId=${encodeURIComponent(current.entity_id)}`,
  ]);
});

test("conflict identity remains distinguishable from generic failures", () => {
  const error = new ResearchBuildApiError("stale", { status: 409, payload: { reason_code: "current_conflict" } });
  assert.equal(error.status, 409);
  assert.equal(error.payload.reason_code, "current_conflict");
});
