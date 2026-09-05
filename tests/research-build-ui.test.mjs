import assert from "node:assert/strict";
import test from "node:test";

import {
  approveConfiguration,
  compileConfiguration,
  configurationCatalogFromPayload,
  configurationFromPayload,
  fetchConfiguration,
  fetchConfigurationCatalog,
  isBuildRoute,
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

function response(payload, { status = 200, ok = true } = {}) {
  return { status, ok, json: async () => payload };
}

test("Build route is canonical query-based Construct/Build", () => {
  assert.equal(isBuildRoute({ pathname: "/research", search: "?stage=construct&tab=build" }), true);
  assert.equal(isBuildRoute({ pathname: "/research", search: "?stage=construct&tab=specification" }), false);
  assert.equal(isBuildRoute({ pathname: "/build", search: "" }), false);
});

test("configuration schemas fail closed", () => {
  assert.equal(configurationFromPayload(record()).state, "compiled");
  assert.throws(() => configurationFromPayload({ schema: "wrong" }), /schema mismatch/);
  assert.throws(() => configurationFromPayload({ ...record(), launch: { enabled: true, reason_code: "wrong" } }), /launch gate/);

  const catalog = configurationCatalogFromPayload({
    schema: "tc.research-configuration-catalog.v1",
    configurations: [{
      entity_id: record().entity_id,
      revision: record().revision,
      state: "compiled",
      source_project_sha256: record().source_project_sha256,
      executable_xml_sha256: record().executable_xml_sha256,
    }],
  });
  assert.equal(catalog.configurations.length, 1);
  assert.throws(() => configurationCatalogFromPayload({ schema: "wrong", configurations: [] }), /catalog schema mismatch/);
});

test("compile and approve use exact bounded API request shapes and refresh custody", async (t) => {
  const previousWindow = globalThis.window;
  t.after(() => { if (previousWindow === undefined) delete globalThis.window; else globalThis.window = previousWindow; });
  const events = [];
  globalThis.window = { dispatchEvent: (event) => events.push(event.type) };
  const calls = [];
  const fakeFetch = async (url, options = {}) => {
    calls.push([url, options]);
    const body = JSON.parse(options.body);
    if (body.action === "compile") return response(record(), { status: 201 });
    return response(record({
      state: "approved",
      parent_revision: record().revision,
      revision: "tc-research-revision:configuration:sha256:" + "e".repeat(64),
      approval: { approved: true, approved_from_revision: record().revision },
    }));
  };

  const compiled = await compileConfiguration(fakeFetch);
  assert.equal(compiled.state, "compiled");
  assert.deepEqual(events, ["tradercockpit:custody-changed"]);
  const approved = await approveConfiguration(compiled.entity_id, compiled.revision, fakeFetch);
  assert.equal(approved.state, "approved");
  assert.deepEqual(events, ["tradercockpit:custody-changed", "tradercockpit:custody-changed"]);

  assert.equal(calls.length, 2);
  assert.equal(calls[0][0], "/api/research/configurations");
  assert.deepEqual(JSON.parse(calls[0][1].body), { action: "compile" });
  assert.deepEqual(JSON.parse(calls[1][1].body), {
    action: "approve",
    entity_id: compiled.entity_id,
    expected_revision: compiled.revision,
  });
  assert.equal(calls[0][1].method, "POST");
  assert.equal(calls[0][1].headers["content-type"], "application/json");
  await assert.rejects(compileConfiguration(async () => response({ schema: "wrong" })), /schema mismatch/);
  await assert.rejects(approveConfiguration(compiled.entity_id, compiled.revision, async () => response({ detail: "refused" }, { ok: false, status: 409 })), /refused/);
  assert.equal(events.length, 2, "malformed or refused writes must not announce custody changes");
});

test("catalog and entity reads come from backend custody", async () => {
  const calls = [];
  const fakeFetch = async (url) => {
    calls.push(url);
    if (url === "/api/research/configurations") {
      return response({
        schema: "tc.research-configuration-catalog.v1",
        configurations: [{
          entity_id: record().entity_id,
          revision: record().revision,
          state: "compiled",
          source_project_sha256: record().source_project_sha256,
          executable_xml_sha256: record().executable_xml_sha256,
        }],
      });
    }
    return response(record());
  };

  const catalog = await fetchConfigurationCatalog(fakeFetch);
  const selected = await fetchConfiguration(catalog.configurations[0].entity_id, fakeFetch);
  assert.equal(selected.entity_id, record().entity_id);
  assert.equal(calls[0], "/api/research/configurations");
  assert.match(calls[1], /^\/api\/research\/configurations\?entityId=/);
});

test("Build workspace exposes custody, exact-byte diff, approval, and disabled launch", () => {
  const compiled = record();
  const html = renderBuildWorkspace({
    phase: "loaded",
    catalog: [{ entity_id: compiled.entity_id, revision: compiled.revision, state: compiled.state }],
    selected: compiled,
  });
  assert.match(html, /data-research-build-workspace/);
  assert.match(html, new RegExp(compiled.source_project_sha256));
  assert.match(html, new RegExp(compiled.executable_xml_sha256));
  assert.match(html, /Byte identical/);
  assert.match(html, /Approve exact revision/);
  assert.match(html, /data-build-launch-gate="disabled"/);
  assert.match(html, /native_launch_not_in_this_slice/);
  assert.match(html, /data-build-launch-disabled/);
  assert.doesNotMatch(html, /data-build-action="launch"/);
});

test("approved workspace preserves compiled parent and cannot reapprove", () => {
  const compiled = record();
  const approved = record({
    state: "approved",
    revision: "tc-research-revision:configuration:sha256:" + "e".repeat(64),
    parent_revision: compiled.revision,
    approval: { approved: true, approved_from_revision: compiled.revision },
  });
  const html = renderBuildWorkspace({ phase: "loaded", catalog: [], selected: approved });
  assert.match(html, /Approved exact revision/);
  assert.match(html, /data-build-approval-complete/);
  assert.doesNotMatch(html, /data-build-action="approve"/);
  assert.match(html, /Launch Builder/);
  assert.match(html, /disabled data-build-launch-disabled/);
});
