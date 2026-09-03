import test from "node:test";
import assert from "node:assert/strict";

import {
  customProjectsCatalogFromPayload,
  fetchCustomProjectsCatalog,
  renderNativeSetup,
  renderTaskPipeline,
  renderWorkflowDetail,
  renderWorkflowList,
  requestProjectControl,
  workflowTopologyFromPayload,
} from "../web/automation-workflows.mjs";

function catalog() {
  return {
    schema: "tc.sqx-custom-projects.v1",
    source_build: "144.2953",
    status: "ready",
    reason_code: null,
    detail: "Native Custom Project workflows",
    control: {
      schema: "tc.sqx-custom-project-control.v1",
      available: false,
      reason_code: "mcp_url_not_configured",
      detail: "StrategyQuant X MCP is not connected.",
      native_tools: ["run_project", "stop_project"],
      endpoint_configured: false,
      credential_configured: false,
    },
    projects: [
      {
        name: "Example Workflow",
        status: "ready",
        reason_code: null,
        detail: null,
        task_count: 2,
        engine: "MetaTrader5",
        symbol: "ES",
        timeframe: "H1",
        archive_sha256: "a".repeat(64),
        source_relative_path: "user/projects/Example Workflow/project.cfx",
      },
    ],
  };
}

function topology() {
  return {
    schema: "tc.sqx-custom-project-topology.v1",
    source_build: "144.2953",
    project: "Example Workflow",
    source_relative_path: "user/projects/Example Workflow/project.cfx",
    archive_sha256: "a".repeat(64),
    internal_entries: ["config.xml", "Build-Task1.xml", "Retest-Task2.xml"],
    tasks: [
      {
        native_task_index: 1,
        kind: "Build",
        entry_name: "Build-Task1.xml",
        name: "Build strategies",
        active: true,
        clear_databanks: [],
        goto_target_label: null,
        setup: {
          engine: "MetaTrader5",
          symbol: "ES",
          timeframe: "H1",
          date_from: "2017.01.03",
          date_to: "2023.01.01",
          generation_type: "genetic",
          money_management_type: "FixedSize",
          money_management_size: "0.1",
          cross_checks_use: true,
          cross_checks: [{ name: "WhatIf", use: false }, { name: "MonteCarlo", use: true }],
          source_member: "Build-Task1.xml",
        },
      },
      {
        native_task_index: 2,
        kind: "Retest",
        entry_name: "Retest-Task2.xml",
        name: "OOS",
        active: true,
        clear_databanks: [],
        goto_target_label: null,
        setup: null,
      },
    ],
    native_setup: {
      engine: "MetaTrader5",
      symbol: "ES",
      timeframe: "H1",
      date_from: "2017.01.03",
      date_to: "2023.01.01",
      generation_type: "genetic",
      money_management_type: "FixedSize",
      money_management_size: "0.1",
      cross_checks_use: true,
      cross_checks: [{ name: "WhatIf", use: false }, { name: "MonteCarlo", use: true }],
      source_member: "Build-Task1.xml",
    },
    execution: { supported: false, reason: "topology_custody_only" },
  };
}

test("Automation catalog parser lists native workflows without inventing execution", () => {
  const parsed = customProjectsCatalogFromPayload(catalog());
  assert.equal(parsed.projects[0].name, "Example Workflow");
  assert.equal(parsed.control.available, false);
  const invented = catalog();
  invented.control.available = true;
  assert.throws(() => customProjectsCatalogFromPayload(invented), /catalog is invalid/);
});

test("Automation catalog fetch uses the canonical list endpoint", async () => {
  let requested = "";
  const result = await fetchCustomProjectsCatalog(async (path) => {
    requested = path;
    return { ok: true, status: 200, json: async () => catalog() };
  });
  assert.equal(requested, "/api/sqx-projects");
  assert.equal(result.projects.length, 1);
});

test("Workflow topology parser keeps native setup and task names", () => {
  const parsed = workflowTopologyFromPayload(topology());
  assert.equal(parsed.tasks[0].name, "Build strategies");
  assert.equal(parsed.native_setup.engine, "MetaTrader5");
  assert.equal(parsed.native_setup.cross_checks[1].name, "MonteCarlo");
});

test("Workflow list and pipeline render native names and setup in this desktop", () => {
  const list = renderWorkflowList(catalog(), "Example Workflow");
  assert.match(list, /Example Workflow/);
  assert.match(list, /Tasks \(2\)/);
  assert.match(list, /MetaTrader5/);
  assert.match(list, /data-automation-open="Example Workflow"/);
  assert.match(list, /data-automation-control="run_project"/);
  assert.match(list, /data-automation-control="stop_project"/);
  assert.match(list, />Results</);
  assert.doesNotMatch(list, /DJ CFD|GOLD BREAKOUT|NQ_M1_dukas|GBPJPY/);
  const pipeline = renderTaskPipeline(topology());
  assert.match(pipeline, /Build strategies/);
  assert.match(pipeline, />OOS</);
  assert.match(pipeline, /task-connector/);
  const setup = renderNativeSetup(topology().native_setup);
  assert.match(setup, /Engine/);
  assert.match(setup, /<select[^>]*disabled/);
  assert.match(setup, /WhatIf/);
  assert.match(setup, /MonteCarlo/);
  const detail = renderWorkflowDetail(topology(), catalog().control);
  assert.match(detail, /Start project/);
  assert.match(detail, /data-automation-control="run_project"/);
  assert.match(detail, /data-automation-control="stop_project"/);
  assert.match(detail, /data-automation-back/);
  assert.match(detail, /Progress is not streaming/);
  assert.match(detail, /Native MCP is not connected|mcp url not configured|Not connected/i);
});

test("Stop posts native stop_project through the same fail-closed control path", async () => {
  let body = "";
  await assert.rejects(
    () => requestProjectControl("Example Workflow", "stop_project", async (path, options) => {
      assert.equal(path, "/api/sqx-project-control");
      assert.equal(options.method, "POST");
      body = options.body;
      return {
        ok: false,
        status: 409,
        json: async () => ({ reason_code: "mcp_url_not_configured", detail: "StrategyQuant X MCP is not connected." }),
      };
    }),
    /StrategyQuant X MCP is not connected/,
  );
  assert.equal(JSON.parse(body).action, "stop_project");
});

test("Start posts native run_project and surfaces the fail-closed MCP refusal", async () => {
  let body = "";
  await assert.rejects(
    () => requestProjectControl("Example Workflow", "run_project", async (path, options) => {
      assert.equal(path, "/api/sqx-project-control");
      assert.equal(options.method, "POST");
      body = options.body;
      return {
        ok: false,
        status: 409,
        json: async () => ({ reason_code: "mcp_url_not_configured", detail: "StrategyQuant X MCP is not connected." }),
      };
    }),
    /StrategyQuant X MCP is not connected/,
  );
  assert.equal(JSON.parse(body).action, "run_project");
  assert.equal(JSON.parse(body).project, "Example Workflow");
});
