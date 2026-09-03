import test from "node:test";
import assert from "node:assert/strict";

import {
  customProjectsCatalogFromPayload,
  fetchCustomProjectsCatalog,
  humanizeNativeName,
  renderFullSettings,
  renderNativeSetup,
  renderTaskPipeline,
  renderWorkflowDetail,
  renderWorkflowList,
  requestProjectControl,
  saveProjectSettings,
  workflowTopologyFromPayload,
} from "../web/automation-workflows.mjs";
import {
  customProjectResultsFromPayload,
  renderNativeArchivesCard,
} from "../web/custom-project-results.mjs";

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
      reason_code: "native_custom_project_launch_unwired",
      detail: "Custom Project start uses the verified StrategyQuant X runtime.",
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
        databank_count: 1,
        strategy_count: 1,
        engine: "MetaTrader5",
        symbol: "ES",
        timeframe: "H1",
        archive_sha256: "a".repeat(64),
        source_relative_path: "user/projects/Example Workflow/project.cfx",
      },
    ],
  };
}

function settings() {
  return [
    {
      tag: "Data",
      path: ["Data"],
      attributes: {},
      text: null,
      children: [
        {
          tag: "Setups",
          path: ["Data", "Setups"],
          attributes: {},
          text: null,
          children: [
            {
              tag: "Setup",
              path: ["Data", "Setups", "Setup"],
              attributes: { engine: "MetaTrader5", dateFrom: "2017.01.03", dateTo: "2023.01.01" },
              text: null,
              children: [
                {
                  tag: "Chart",
                  path: ["Data", "Setups", "Setup", "Chart"],
                  attributes: { symbol: "ES", timeframe: "H1" },
                  text: null,
                  children: [],
                },
              ],
            },
          ],
        },
      ],
    },
    {
      tag: "WhatToBuild",
      path: ["WhatToBuild"],
      attributes: {},
      text: null,
      children: [
        {
          tag: "BuildMode",
          path: ["WhatToBuild", "BuildMode"],
          attributes: { generationType: "genetic" },
          text: null,
          children: [],
        },
      ],
    },
    {
      tag: "MoneyManagement",
      path: ["MoneyManagement"],
      attributes: { type: "FixedSize", size: "0.1" },
      text: null,
      children: [],
    },
    {
      tag: "CrossChecks",
      path: ["CrossChecks"],
      attributes: { use: "true" },
      text: null,
      children: [
        { tag: "WhatIf", path: ["CrossChecks", "WhatIf"], attributes: { use: "false" }, text: null, children: [] },
        { tag: "MonteCarlo", path: ["CrossChecks", "MonteCarlo"], attributes: { use: "true" }, text: null, children: [] },
      ],
    },
  ];
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
        settings: settings(),
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
        settings: [{ tag: "Retest", path: ["Retest"], attributes: {}, text: null, children: [] }],
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
  const mcp = catalog();
  mcp.control.native_tools = ["run_project", "stop_project"];
  assert.throws(() => customProjectsCatalogFromPayload(mcp), /catalog is invalid/);
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

test("Workflow topology parser keeps native setup, task names, and settings panes", () => {
  const parsed = workflowTopologyFromPayload(topology());
  assert.equal(parsed.tasks[0].name, "Build strategies");
  assert.equal(parsed.native_setup.engine, "MetaTrader5");
  assert.equal(parsed.native_setup.cross_checks[1].name, "MonteCarlo");
  assert.deepEqual(parsed.tasks[0].settings.map((node) => node.tag), ["Data", "WhatToBuild", "MoneyManagement", "CrossChecks"]);
});

function results() {
  return {
    schema: "tc.sqx-custom-project-results.v1",
    source_build: "144.2953",
    status: "ready",
    reason_code: null,
    detail: "Native Custom Project databanks",
    project: "Example Workflow",
    databank_count: 1,
    strategy_count: 1,
    projects: [
      {
        name: "Example Workflow",
        source_relative_path: "user/projects/Example Workflow/project.cfx",
        databank_count: 1,
        strategy_count: 1,
        databanks: [
          {
            name: "Results",
            strategy_count: 1,
            strategies: [
              {
                archive: "Example.sqx",
                relative_path: "user/projects/Example Workflow/databanks/Results/Example.sqx",
                inspectable: true,
                native_version: "144.2953",
                archive_sha256: "b".repeat(64),
              },
            ],
          },
        ],
      },
    ],
  };
}

test("Workflow list and pipeline render native names and adjustable settings in this desktop", () => {
  assert.equal(humanizeNativeName("WhatToBuild"), "What To Build");
  const list = renderWorkflowList(catalog(), "Example Workflow");
  assert.match(list, /Example Workflow/);
  assert.match(list, /Tasks \(2\)/);
  assert.match(list, /Databanks \(1\)/);
  assert.match(list, /Strategies \(1\)/);
  assert.match(list, /MetaTrader5/);
  assert.match(list, /data-automation-open="Example Workflow"/);
  assert.match(list, /data-automation-control="run_project"/);
  assert.match(list, /data-automation-control="stop_project"/);
  assert.match(list, /workspace=validate/);
  assert.doesNotMatch(list, /DJ CFD|GOLD BREAKOUT|NQ_M1_dukas|GBPJPY/);
  assert.doesNotMatch(list, /SQX MCP|StrategyQuant X MCP/);
  const pipeline = renderTaskPipeline(topology(), 1);
  assert.match(pipeline, /Build strategies/);
  assert.match(pipeline, />OOS</);
  assert.match(pipeline, /task-connector/);
  assert.match(pipeline, /data-automation-task-settings="1"/);
  assert.match(pipeline, /data-automation-task-active="1"/);
  const setup = renderNativeSetup(topology().tasks[0]);
  assert.match(setup, /Engine/);
  assert.match(setup, /<input[^>]*workflow-input[^>]*value="MetaTrader5"/);
  assert.doesNotMatch(setup, /<select[^>]*disabled/);
  assert.match(setup, /What If/);
  assert.match(setup, /Monte Carlo/);
  assert.match(setup, /data-automation-save-settings/);
  const settingsHtml = renderFullSettings(topology().tasks[0], "WhatToBuild");
  assert.match(settingsHtml, /What To Build/);
  assert.match(settingsHtml, /data-automation-section="Data"/);
  assert.match(settingsHtml, /data-automation-section="MoneyManagement"/);
  assert.match(settingsHtml, /data-automation-section="CrossChecks"/);
  assert.doesNotMatch(settingsHtml, /Ranking|Building Blocks|Trading Options/);
  const detail = renderWorkflowDetail(topology(), catalog().control, customProjectResultsFromPayload(results()), { tab: "progress", task: 1 });
  assert.match(detail, /data-automation-tab="progress"/);
  assert.match(detail, /data-automation-tab="settings"/);
  assert.match(detail, />Full settings</);
  assert.match(detail, /data-automation-tab="results"/);
  assert.match(detail, /Start project/);
  assert.match(detail, /data-automation-control="run_project"/);
  assert.match(detail, /data-automation-control="stop_project"/);
  assert.match(detail, /data-automation-back/);
  assert.match(detail, /Live task logs are not streaming/);
  assert.match(detail, /Example\.sqx/);
  assert.match(detail, /Launch unwired|native custom project launch/i);
  assert.doesNotMatch(detail, /StrategyQuant X MCP|Native MCP/);
  const full = renderWorkflowDetail(topology(), catalog().control, customProjectResultsFromPayload(results()), { tab: "settings", task: 1, section: "CrossChecks" });
  assert.match(full, /data-automation-section="CrossChecks"/);
  assert.match(full, /What If/);
  assert.match(full, /Save settings/);
});

test("Test & Validate lists native Custom Project archives without inventing funnel counts", () => {
  const parsed = customProjectResultsFromPayload(results());
  const html = renderNativeArchivesCard(parsed);
  assert.match(html, /data-validate-native-archives="loaded"/);
  assert.match(html, /Example Workflow/);
  assert.match(html, /Example\.sqx/);
  assert.match(html, /Inspectable/);
  assert.doesNotMatch(html, /Profit Factor|\$\s?\d/);
  const invented = results();
  invented.projects[0].strategy_count = 9;
  assert.throws(() => customProjectResultsFromPayload(invented), /invalid/);
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
        json: async () => ({ reason_code: "native_custom_project_launch_unwired", detail: "Custom Project start uses the verified StrategyQuant X runtime." }),
      };
    }),
    /verified StrategyQuant X runtime/,
  );
  assert.equal(JSON.parse(body).action, "stop_project");
});

test("Start posts native run_project and surfaces the fail-closed launch refusal", async () => {
  let body = "";
  await assert.rejects(
    () => requestProjectControl("Example Workflow", "run_project", async (path, options) => {
      assert.equal(path, "/api/sqx-project-control");
      assert.equal(options.method, "POST");
      body = options.body;
      return {
        ok: false,
        status: 409,
        json: async () => ({ reason_code: "native_custom_project_launch_unwired", detail: "Custom Project start uses the verified StrategyQuant X runtime." }),
      };
    }),
    /verified StrategyQuant X runtime/,
  );
  assert.equal(JSON.parse(body).action, "run_project");
  assert.equal(JSON.parse(body).project, "Example Workflow");
});

test("Settings save posts only path, attribute, and value for existing native attributes", async () => {
  let body = "";
  const result = await saveProjectSettings("Example Workflow", 1, [
    { path: ["Data", "Setups", "Setup"], attribute: "engine", value: "MetaTrader4" },
  ], async (path, options) => {
    assert.equal(path, "/api/sqx-project-settings");
    assert.equal(options.method, "POST");
    body = options.body;
    return { ok: true, status: 200, json: async () => ({ updated: 1 }) };
  });
  assert.equal(result.updated, 1);
  assert.deepEqual(JSON.parse(body), {
    project: "Example Workflow",
    task: 1,
    updates: [{ path: ["Data", "Setups", "Setup"], attribute: "engine", value: "MetaTrader4" }],
  });
});
