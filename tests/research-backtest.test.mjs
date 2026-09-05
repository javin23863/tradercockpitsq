import assert from "node:assert/strict";
import test from "node:test";

import {
  candidateFromPayload,
  fetchRetesterProfile,
  historicalResultCatalogFromPayload,
  historicalResultFromPayload,
  retesterRuntimeReady,
  startRetester,
} from "../web/research-backtest.mjs";
import { workflowTopologyFromPayload } from "../web/automation-workflows.mjs";

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
const executionProof = { schema: "tc.sqx-retester-execution.v1", task_name: "Retest strategies", input_strategies: 1, tested_strategies: 1, passed_strategies: 0, failed_strategies: 1, stdout_sha256: "a".repeat(64), task_log_sha256: "b".repeat(64) };

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
    receipts: [{ action: "start", state: "completed", task: 1, exit_code: 0, execution_proof: executionProof }],
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
    execution_verification: "verified",
    validation_state: "not_run",
    reused: false,
    ...overrides,
  };
}

function response(payload, { ok = true, status = 200 } = {}) {
  return { ok, status, async json() { return payload; } };
}

function profileTopology() {
  const setup = { engine: "MetaTrader4", symbol: "GBPUSD", timeframe: "H1", date_from: "2003.01.01", date_to: "2018.12.31", generation_type: null, money_management_type: null, money_management_size: null, cross_checks_use: false, cross_checks: [] };
  return workflowTopologyFromPayload({ schema: "tc.sqx-custom-project-topology.v1", source_build: "144.2953", project: "Retester", source_relative_path: "user/projects/Retester/project.cfx", archive_sha256: "7".repeat(64), native_setup: setup, execution: { supported: false, reason: "topology_custody_only" }, tasks: [{ native_task_index: 1, kind: "Retest", entry_name: "Retest-Task1.xml", name: "Retest", active: true, settings: [], setup }] });
}

test("saved Retester profile uses the existing validated topology and refuses a substituted module", async () => {
  let request;
  const profile = await fetchRetesterProfile(async (path, options) => { request = { path, options }; return response(profileTopology()); });
  assert.equal(request.path, "/api/sqx-project-topology?project=Retester");
  assert.equal(request.options.method, undefined);
  assert.equal(profile.setup.symbol, "GBPUSD");
  assert.equal(profile.archive_sha256, "7".repeat(64));
  await assert.rejects(fetchRetesterProfile(async () => response({ ...profileTopology(), project: "Builder", source_relative_path: "user/projects/Builder/project.cfx" })), /Saved Retester task 1 profile is unavailable/);
  await assert.rejects(fetchRetesterProfile(async () => response({ detail: "Runtime not configured" }, { ok: false, status: 503 })), /Runtime not configured/);
});

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

test("Retester start sends only exact Candidate identity and refreshes only bound custody", async (t) => {
  const previousWindow = globalThis.window;
  t.after(() => { if (previousWindow === undefined) delete globalThis.window; else globalThis.window = previousWindow; });
  const events = [];
  globalThis.window = { dispatchEvent: (event) => events.push(event.type) };
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
  assert.deepEqual(events, ["tradercockpit:custody-changed"]);

  await assert.rejects(
    startRetester(selected, async () => response(result({ candidate_revision: `tc-research-revision:candidate:sha256:${"8".repeat(64)}` }))),
    /does not bind the selected Candidate revision/,
  );
  await assert.rejects(startRetester(selected, async () => response({ detail: "refused" }, { ok: false, status: 409 })), /refused/);
  await assert.rejects(startRetester({}), /Candidate custody is invalid/);
  assert.equal(events.length, 1, "invalid or refused runs must not announce custody changes");
});

test("legacy completion stays readable without execution authority; native filter failure still proves execution", async () => {
  const legacy = result({ execution_completed: false, execution_verification: "unverified", receipts: [{ action: "startOnlyTask", task: 1, state: "completed" }] });
  const catalog = historicalResultCatalogFromPayload({ schema: "tc.research-historical-result-catalog.v1", results: [legacy, result()] });
  assert.equal(catalog[0].execution_completed, false);
  assert.equal(catalog[1].execution_completed, true, "tested 1 / passed 0 / failed 1 is executed, not profitable");
  await assert.rejects(startRetester(candidate(), async () => response(legacy)), /did not return verified execution/);
  for (const patch of [{ input_strategies: 0 }, { tested_strategies: 0 }, { tested_strategies: 2 }, { failed_strategies: -1 }, { task_log_sha256: "" }, { task_name: "" }]) {
    assert.throws(() => historicalResultFromPayload(result({ receipts: [{ action: "start", task: 1, state: "completed", execution_proof: { ...executionProof, ...patch } }] })), /Completed historical result is inconsistent/);
  }
  assert.throws(() => historicalResultFromPayload({ ...legacy, execution_completed: true, execution_verification: "verified" }), /Completed historical result is inconsistent/);
});

test("pending Retester locks selection and ignores completion after navigation and reselection", async () => {
  const keys = ["document", "location", "fetch", "MutationObserver", "window"];
  const original = Object.fromEntries(keys.map((key) => [key, globalThis[key]]));
  const listeners = {};
  let observe;
  let workspace;
  let requests = 0;
  let release;
  let holdNextProfile = false;
  let releaseProfile;
  const events = [];
  const second = candidate({ entity_id: "candidate-second", revision: "revision-second", archive_name: "Second.sqx" });
  const tick = () => new Promise((resolve) => setImmediate(resolve));
  const panel = {
    isConnected: true,
    querySelector: (selector) => selector === "[data-retester-overview]" ? workspace : null,
    append(node) { workspace = node; },
  };
  const click = () => listeners.click({ target: { closest: () => ({ disabled: false, textContent: "" }) } });
  const selectSecond = () => listeners.change({ target: { id: "retester-candidate", value: "1" } });
  try {
    globalThis.location = new URL("http://localhost/research?workspace=validate&tab=initial-test");
    globalThis.document = {
      documentElement: {}, querySelector: () => panel,
      createElement: () => ({ dataset: {}, innerHTML: "" }),
      addEventListener(name, callback) { listeners[name] = callback; },
    };
    globalThis.MutationObserver = class { constructor(callback) { observe = callback; } observe() {} };
    globalThis.window = { dispatchEvent: (event) => events.push(event.type) };
    globalThis.fetch = async (path, options = {}) => {
      if (options.method === "POST") {
        requests += 1;
        await new Promise((resolve) => { release = resolve; });
        return response(result(), { status: 201 });
      }
      if (path.endsWith("candidates")) return response({ schema: "tc.research-candidate-catalog.v1", candidates: [candidate(), second] });
      if (path.endsWith("historical-results")) return response({ schema: "tc.research-historical-result-catalog.v1", results: [] });
      if (path.startsWith("/api/sqx-project-topology")) {
        if (holdNextProfile) {
          holdNextProfile = false;
          await new Promise((resolve) => { releaseProfile = resolve; });
          const old = profileTopology();
          old.tasks[0].setup.symbol = "STALE";
          return response(old);
        }
        return response(profileTopology());
      }
      return response({ schema: "tc.runtime-status.v1", research_backend: { verified: true, execution: { gateway_available: true, launcher_verified: true } } });
    };
    await import(`../web/research-backtest.mjs?lifecycle=${Date.now()}`);
    await tick();
    assert.match(workspace.innerHTML, /data-retester-profile="available"/);
    assert.match(workspace.innerHTML, /MetaTrader4|GBPUSD/);
    assert.match(workspace.innerHTML, /2003\.01\.01 – 2018\.12\.31/);
    assert.match(workspace.innerHTML, /may differ from this Candidate/);
    click();
    assert.match(workspace.innerHTML, /id="retester-candidate"[^>]*disabled/);
    assert.match(workspace.innerHTML, /Running native Retester/);
    const pendingMarkup = workspace.innerHTML;
    selectSecond();
    observe();
    assert.equal(workspace.innerHTML, pendingMarkup, "selection and unrelated DOM mutations cannot unlock a pending run");
    click();
    assert.equal(requests, 1, "a second click must not submit while the first request is pending");
    globalThis.location = new URL("http://localhost/home");
    observe();
    workspace = null;
    holdNextProfile = true;
    globalThis.location = new URL("http://localhost/research?workspace=validate&tab=initial-test");
    observe();
    await tick();
    assert.equal(typeof releaseProfile, "function");
    globalThis.location = new URL("http://localhost/home");
    observe();
    workspace = null;
    globalThis.location = new URL("http://localhost/research?workspace=validate&tab=initial-test");
    observe();
    await tick();
    selectSecond();
    assert.match(workspace.innerHTML, /value="1" selected/);
    const currentMarkup = workspace.innerHTML;
    releaseProfile();
    await tick();
    assert.equal(workspace.innerHTML, currentMarkup, "a late saved-profile read cannot replace the current visit");
    release();
    await tick();
    assert.deepEqual(events, ["tradercockpit:custody-changed"], "stale response is valid terminal custody and still refreshes global state");
    assert.equal(workspace.innerHTML, currentMarkup, "an earlier visit's terminal response cannot repaint the current Candidate");
    assert.doesNotMatch(workspace.innerHTML, /Execution completed|Native Retester execution captured/);
  } finally {
    for (const [key, value] of Object.entries(original)) {
      if (value === undefined) delete globalThis[key];
      else globalThis[key] = value;
    }
  }
});

test("failed Retester refreshes durable failure without letting a stale catalog repaint another visit", async () => {
  const original = Object.fromEntries(["document", "location", "fetch", "MutationObserver", "window"].map((key) => [key, globalThis[key]]));
  try {
    for (const stale of [false, true]) {
      const listeners = {};
      const events = [];
      let observe;
      let workspace;
      let refreshPending = false;
      let releaseCatalog;
      let posts = 0;
      const tick = () => new Promise((resolve) => setImmediate(resolve));
      const panel = {
        isConnected: true,
        querySelector: (selector) => selector === "[data-retester-overview]" ? workspace : null,
        append(node) { workspace = node; },
      };
      globalThis.location = new URL("http://localhost/research?workspace=validate&tab=initial-test");
      globalThis.document = {
        documentElement: {}, querySelector: () => panel,
        createElement: () => ({ dataset: {}, innerHTML: "" }),
        addEventListener(name, callback) { listeners[name] = callback; },
      };
      globalThis.MutationObserver = class { constructor(callback) { observe = callback; } observe() {} };
      globalThis.window = { dispatchEvent: (event) => events.push(event.type) };
      globalThis.fetch = async (path, options = {}) => {
        if (options.method === "POST") {
          posts += 1;
          refreshPending = true;
          return response({ detail: "No native task executed" }, { ok: false, status: 409 });
        }
        if (path.endsWith("candidates")) return response({ schema: "tc.research-candidate-catalog.v1", candidates: [candidate(), candidate({ entity_id: "candidate-second", revision: "revision-second", archive_name: "Second.sqx" })] });
        if (path.endsWith("historical-results")) {
          if (refreshPending) {
            refreshPending = false;
            await new Promise((resolve) => { releaseCatalog = resolve; });
            return response({ schema: "tc.research-historical-result-catalog.v1", results: [result({ state: "failed", execution_completed: false, failure_reason_code: "sqx_task_not_started", receipts: [] })] });
          }
          return response({ schema: "tc.research-historical-result-catalog.v1", results: [] });
        }
        return response({ schema: "tc.runtime-status.v1", research_backend: { verified: true, execution: { gateway_available: true, launcher_verified: true } } });
      };
      await import(`../web/research-backtest.mjs?failure=${stale}-${Date.now()}`);
      await tick();
      assert.match(workspace.innerHTML, /data-retester-profile="unavailable"/);
      const click = () => listeners.click({ target: { closest: () => ({ disabled: false }) } });
      click();
      await tick();
      assert.equal(typeof releaseCatalog, "function", "a refused POST must refresh durable historical-result custody");
      let currentMarkup;
      if (stale) {
        globalThis.location = new URL("http://localhost/home");
        observe();
        workspace = null;
        globalThis.location = new URL("http://localhost/research?workspace=validate&tab=initial-test");
        observe();
        await tick();
        listeners.change({ target: { id: "retester-candidate", value: "1" } });
        currentMarkup = workspace.innerHTML;
      }
      releaseCatalog();
      await tick();
      if (stale) {
        assert.equal(workspace.innerHTML, currentMarkup);
        assert.doesNotMatch(workspace.innerHTML, /Execution failed|No native task executed/);
      } else {
        assert.match(workspace.innerHTML, /Execution failed/);
        assert.match(workspace.innerHTML, /sqx_task_not_started/);
        assert.match(workspace.innerHTML, /No native task executed/);
        assert.match(workspace.innerHTML, /data-retester-action="start" disabled/);
        click();
        assert.equal(posts, 1, "a durable failed binding cannot be launched again");
        assert.deepEqual(events, ["tradercockpit:custody-changed"]);
      }
    }
  } finally {
    for (const [key, value] of Object.entries(original)) {
      if (value === undefined) delete globalThis[key];
      else globalThis[key] = value;
    }
  }
});
