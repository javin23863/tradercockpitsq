import test from "node:test";
import assert from "node:assert/strict";

import {
  customProjectsCatalogFromPayload,
  fetchCustomProjectsCatalog,
  humanizeNativeName,
  nativeChoicesFor,
  renderCrossChecksPane,
  renderFullSettings,
  renderNativeSetup,
  renderRankingsPane,
  renderTaskPipeline,
  renderWorkflowDetail,
  renderWorkflowList,
  applyCatalogPatch,
  exclusiveUseUpdates,
  fetchBuildTypeFiles,
  fetchCommissionMethods,
  fetchInstalledDataSymbols,
  fetchRankingFitnessTypes,
  fetchSymbolData,
  loadOfficialSettingsLists,
  requestCalibrate,
  requestProjectControl,
  requestTemplateReload,
  saveEngineChartSelection,
  saveProjectSettings,
  progressLiveFragments,
  SQX_PROGRESS_POLL_MS,
  workflowTopologyFromPayload,
} from "../web/automation-workflows.mjs";
import {
  customProjectResultsFromPayload,
  renderNativeArchivesCard,
} from "../web/custom-project-results.mjs";
import {
  documentedSettingsTabs,
  isImproveExisting,
  renderBuildingBlocksPane,
  renderMoneyManagementPane,
} from "../web/automation-full-settings.mjs";
import {
  availableInstalledRows,
  officialSqxChoiceState,
  renderAttributeControl,
  resetOfficialSqxChoices,
  rewrittenSetupDates,
  setOfficialSqxChoices,
  symbolChangeUpdates,
} from "../web/automation-settings-controls.mjs";
import {
  projectStrategyFromPayload,
  renderResultsPanel,
  fetchProjectStrategy,
  fetchResultsChart,
} from "../web/automation-results.mjs";

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
      reason_code: "trusted_launcher_not_configured",
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
              attributes: {
                engine: "MetaTrader5",
                dateFrom: "2017.01.03",
                dateTo: "2023.01.01",
                session: "No Session",
                testPrecision: "1",
              },
              text: null,
              children: [
                {
                  tag: "Chart",
                  path: ["Data", "Setups", "Setup", "Chart"],
                  attributes: { symbol: "ES", timeframe: "H1" },
                  text: null,
                  children: [],
                },
                {
                  tag: "Commissions",
                  path: ["Data", "Setups", "Setup", "Commissions"],
                  attributes: {},
                  text: null,
                  children: [
                    {
                      tag: "Method",
                      path: ["Data", "Setups", "Setup", "Commissions", "Method"],
                      attributes: { type: "None", use: "true" },
                      text: null,
                      children: [],
                    },
                  ],
                },
                {
                  tag: "Swap",
                  path: ["Data", "Setups", "Setup", "Swap"],
                  attributes: { use: "false", type: "money", long: "0", short: "0", tripleSwapOn: "WEDNESDAY" },
                  text: null,
                  children: [],
                },
              ],
            },
          ],
        },
        {
          tag: "OutOfSample",
          path: ["Data", "OutOfSample"],
          attributes: { showGraph: "false" },
          text: null,
          children: [
            { tag: "Range", path: ["Data", "OutOfSample", "Range"], attributes: { dateFrom: "2018.01.01", dateTo: "2018.06.01", type: "oos" }, text: null, children: [] },
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
  const ready = catalog();
  ready.control.available = true;
  ready.control.reason_code = null;
  assert.equal(customProjectsCatalogFromPayload(ready).control.available, true);
  const inventedReason = catalog();
  inventedReason.control.available = true;
  inventedReason.control.reason_code = "trusted_launcher_not_configured";
  assert.throws(() => customProjectsCatalogFromPayload(inventedReason), /catalog is invalid/);
  const mcp = catalog();
  mcp.control.native_tools = ["run_project", "stop_project"];
  assert.throws(() => customProjectsCatalogFromPayload(mcp), /catalog is invalid/);
});

test("Catalog rows paint producer running percent without inventing list chrome", () => {
  const idle = renderWorkflowList(catalog());
  assert.doesNotMatch(idle, /data-project-running/);
  assert.match(idle, /data-automation-progress-pause/);
  assert.match(idle, /data-automation-project-list/);
  const liveCatalog = catalog();
  liveCatalog.projects[0].running = true;
  liveCatalog.projects[0].percent = 37;
  liveCatalog.projects[0].running_status = "Running";
  assert.equal(customProjectsCatalogFromPayload(liveCatalog).projects[0].percent, 37);
  const html = renderWorkflowList(liveCatalog);
  assert.match(html, /data-project-running="true"/);
  assert.match(html, /style="width:37%"/);
  assert.match(html, /data-automation-control="pause_project"/);
  assert.match(html, /disabled[^>]*data-automation-control="run_project"/);
  assert.doesNotMatch(html, /Running time|Top Strategy/);
  const bad = catalog();
  bad.projects[0].percent = 101;
  assert.throws(() => customProjectsCatalogFromPayload(bad), /invalid/);
  const list = { innerHTML: "old", querySelector() { return null; } };
  const root = {
    querySelector(sel) {
      return sel === "[data-automation-project-list]" ? list : null;
    },
  };
  assert.equal(applyCatalogPatch(root, liveCatalog), true);
  assert.match(list.innerHTML, /width:37%/);
  const focused = {
    innerHTML: "keep",
    querySelector() { return { matches: () => true }; },
  };
  assert.equal(applyCatalogPatch({ querySelector() { return focused; } }, liveCatalog), false);
  assert.equal(focused.innerHTML, "keep");
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

test("Native settings use documented choice lists instead of typing every value", () => {
  resetOfficialSqxChoices();
  assert.deepEqual(nativeChoicesFor("engine", "MetaTrader4").map((row) => row[0]), [
    "MetaTrader4",
    "MetaTrader5 (netted)",
    "MetaTrader5 (hedged)",
    "Tradestation",
    "MultiCharts",
    "JForex",
    "Stockpicker",
    "Single-asset cloud strategy",
  ]);
  assert.deepEqual(nativeChoicesFor("engine", "MetaTrader5").map((row) => row[0]), [
    "MetaTrader5",
    "MetaTrader4",
    "MetaTrader5 (netted)",
    "MetaTrader5 (hedged)",
    "Tradestation",
    "MultiCharts",
    "JForex",
    "Stockpicker",
    "Single-asset cloud strategy",
  ]);
  assert.deepEqual(nativeChoicesFor("generationType", "genetic").map((row) => row[0]), ["random", "genetic"]);
  assert.deepEqual(nativeChoicesFor("generationType", "genetic-evolution").map((row) => row[0]), ["random-generation", "genetic-evolution"]);
  assert.equal(nativeChoicesFor("generationType", "future-native-mode"), null);
  assert.deepEqual(nativeChoicesFor("timeframe", "H1").map((row) => row[0]), ["TICK", "M1", "M5", "M15", "M30", "H1", "H4", "D1", "Weekly", "Monthly"]);
  assert.ok(nativeChoicesFor("timeframe", "W1").map((row) => row[0]).includes("W1"));
  assert.deepEqual(nativeChoicesFor("type", "improve", { tag: "StrategyType" }).map((row) => row[0]), ["simple", "multi-tf", "template", "improve"]);
  assert.deepEqual(nativeChoicesFor("type", "improve-existing", { tag: "StrategyType" }).map((row) => row[0]), ["improve-existing", "simple", "multi-tf", "template", "improve"]);
  assert.deepEqual(nativeChoicesFor("improveType", "strategy", { tag: "StrategyType" }).map((row) => row[0]), ["strategy", "databank"]);
  assert.deepEqual(nativeChoicesFor("architecture", "sq4", { tag: "StrategyType" }).map((row) => row[0]), ["sq4", "sq4fuzzy", "sq3"]);
  assert.equal(nativeChoicesFor("type", "FixedSize", { tag: "MoneyManagement" }), null);
  assert.deepEqual(nativeChoicesFor("type", "both", { tag: "MarketSides" }).map((row) => row[0]), ["both", "long", "short"]);
  assert.deepEqual(nativeChoicesFor("type", "databank-full", { tag: "StopCondition" }).map((row) => row[0]), [
    "never",
    "passed-count",
    "databank-full",
    "time-limit",
  ]);
  assert.deepEqual(nativeChoicesFor("action", "replace", { tag: "LongImprovement" }).map((row) => row[0]), ["add-or-replace", "replace", "add"]);
  assert.equal(nativeChoicesFor("type", "RExpectancy", { tag: "Ranking" }), null);
  assert.equal(nativeChoicesFor("symbol", "ES", { tag: "Chart" }), null);
  setOfficialSqxChoices({
    rankingTypes: [
      { key: "NetProfit", name: "Net Profit (Return)" },
      { key: "ReturnDDRatio", name: "Return / Drawdown ratio" },
      { key: "RExpectancy", name: "R Expectancy (Van Tharp)" },
      { key: "AnnualPctReturnDDRatio", name: "Annual Return % / Max DD %" },
      { key: "Weighted", name: "Weighted Fitness (multiple goals)" },
    ],
    templateFiles: ["highest_breakout.sqx"],
    strategyFiles: ["Strategy 1.sqx"],
  });
  assert.deepEqual(nativeChoicesFor("type", "RExpectancy", { tag: "Ranking" }).map((row) => row[0]), [
    "NetProfit",
    "ReturnDDRatio",
    "RExpectancy",
    "AnnualPctReturnDDRatio",
    "Weighted",
  ]);
  assert.deepEqual(nativeChoicesFor("templateFile", "highest_breakout.sqx").map((row) => row[0]), ["highest_breakout.sqx"]);
  setOfficialSqxChoices({
    symbols: ["EURUSD", "DJ CFD"],
    dataRows: [
      { symbol: "EURUSD", dataType: "3", dateFrom: 1483228800000, dateTo: 1704067200000, rows: 10, show: true },
      { symbol: "DJ CFD", dataType: "4", dateFrom: 1483228800000, dateTo: 1704067200000, rows: 10, show: true },
    ],
    dataTypes: [{ key: "3", name: "Forex" }, { key: "4", name: "CFD" }],
    sessions: ["No Session", "London"],
    precisions: [{ key: "1", name: "Selected timeframe" }, { key: "2", name: "1 minute" }],
    swapTypes: ["money", "percent", "points"],
    tripleSwapOptions: ["WEDNESDAY", "FRIDAY"],
    symbolsReady: true,
    commissionMethods: [{ key: "None", name: "None" }, { key: "SizeCommission", name: "Size commission" }],
    commissionReady: true,
  });
  assert.deepEqual(nativeChoicesFor("symbol", "ES", { tag: "Chart" }).map((row) => row[0]), ["ES", "EURUSD", "DJ CFD"]);
  assert.equal(nativeChoicesFor("symbol", "ES", { tag: "Setup" }), null);
  assert.deepEqual(nativeChoicesFor("session", "No Session", { tag: "Setup" }).map((row) => row[0]), ["No Session", "London"]);
  assert.deepEqual(nativeChoicesFor("testPrecision", "1", { tag: "Setup" }).map((row) => row[0]), ["1", "2"]);
  assert.deepEqual(
    nativeChoicesFor("type", "None", { tag: "Method", path: ["Data", "Setups", "Setup", "Commissions", "Method"] }).map((row) => row[0]),
    ["None", "SizeCommission"],
  );
  assert.equal(nativeChoicesFor("type", "None", { tag: "Method", path: ["MoneyManagement", "Method"] }), null);
  const dataHtml = renderFullSettings(topology().tasks[0], "Data", "Example Workflow");
  assert.match(dataHtml, /<select[^>]*data-settings-attribute="symbol"/);
  assert.match(dataHtml, /<option value="ES" selected>/);
  assert.match(dataHtml, /<option value="DJ CFD"/);
  assert.match(dataHtml, /<select[^>]*data-settings-attribute="session"/);
  assert.match(dataHtml, /<option value="London"/);
  assert.match(dataHtml, /<select[^>]*data-settings-attribute="testPrecision"/);
  assert.match(dataHtml, /<option value="2"[^>]*>1 minute/);
  assert.match(dataHtml, /<select[^>]*data-settings-attribute="type"/);
  assert.match(dataHtml, /<option value="SizeCommission"/);
  assert.match(dataHtml, /data-sqx-data-box/);
  assert.match(dataHtml, /search by typing/);
  assert.match(dataHtml, /Recently used/);
  assert.match(dataHtml, /data-sqx-reset-dates/);
  assert.match(dataHtml, /data-settings-dialog="data-commission"/);
  assert.match(dataHtml, /data-settings-dialog="data-swap"/);
  assert.match(dataHtml, /data-sqx-oos-graph/);
  assert.match(dataHtml, /data-task-kind="Build"/);
  assert.match(dataHtml, /<option value="money"/);
  assert.match(dataHtml, /<option value="WEDNESDAY"/);
  assert.deepEqual(rewrittenSetupDates({ dateFrom: 1483228800000, dateTo: 1704067200000 }, "2010.01.01", "2011.01.01"), {
    dateFrom: "2017.01.01",
    dateTo: "2024.01.01",
  });
  assert.deepEqual(
    symbolChangeUpdates(
      {
        closest() {
          return {
            querySelectorAll(selector) {
              if (!String(selector).includes("dateFrom") && !String(selector).includes("dateTo")) return [];
              const attribute = String(selector).includes("dateFrom") ? "dateFrom" : "dateTo";
              return [{
                value: attribute === "dateFrom" ? "2010.01.01" : "2011.01.01",
                getAttribute(name) {
                  return name === "data-settings-path" ? JSON.stringify(["Data", "Setups", "Setup"]) : "";
                },
              }];
            },
          };
        },
      },
      [{ path: ["Data", "Setups", "Setup", "Chart"], attribute: "symbol", value: "EURUSD" }],
    ).slice(-2),
    [
      { path: ["Data", "Setups", "Setup"], attribute: "dateFrom", value: "2017.01.01" },
      { path: ["Data", "Setups", "Setup"], attribute: "dateTo", value: "2024.01.01" },
    ],
  );
  resetOfficialSqxChoices();
  const closed = renderFullSettings(topology().tasks[0], "Data", "Example Workflow");
  assert.match(closed, /constants\/getAll/);
  assert.match(closed, /data-settings-attribute="symbol"[^>]*disabled/);
  assert.match(closed, /data-settings-attribute="session"[^>]*disabled/);
  assert.match(closed, /data-settings-attribute="testPrecision"[^>]*disabled/);
  assert.match(closed, /listCommissionMethods/);
  assert.match(closed, /data-settings-attribute="type"[^>]*disabled/);
  assert.match(closed, /Swap type and triple-swap day come from StrategyQuant X constants\/getAll/);
});

test("Data pane fail-closes hidden rows, empty commission methods, and stale official lists", async () => {
  resetOfficialSqxChoices();
  const swapDown = renderAttributeControl(["Data", "Setups", "Setup", "Swap"], "type", "money", { tag: "Swap" });
  assert.match(swapDown, /disabled/);
  assert.doesNotMatch(swapDown, /<input(?![^>]*disabled)/);
  setOfficialSqxChoices({ commissionReady: true, commissionMethods: [] });
  const commissionEmpty = renderAttributeControl(
    ["Data", "Setups", "Setup", "Commissions", "Method"],
    "type",
    "None",
    { tag: "Method", path: ["Data", "Setups", "Setup", "Commissions", "Method"] },
  );
  assert.match(commissionEmpty, /listCommissionMethods/);
  assert.doesNotMatch(commissionEmpty, /<input/);
  resetOfficialSqxChoices();
  setOfficialSqxChoices({
    symbolsReady: true,
    symbols: ["EURUSD", "HIDDEN", "[SP500]"],
    dataRows: [
      { symbol: "EURUSD", dataType: "3", rows: 10, show: true },
      { symbol: "HIDDEN", dataType: "3", rows: 0, show: false },
      { symbol: "BARE", dataType: "3" },
      { symbol: "[SP500]", dataType: "1", rows: 10, show: true },
    ],
  });
  assert.deepEqual(nativeChoicesFor("symbol", "ES", { tag: "Chart" }).map((row) => row[0]), ["ES", "EURUSD"]);
  assert.deepEqual(availableInstalledRows("MetaTrader5 (netted)").map((row) => row.symbol), ["EURUSD"]);
  assert.deepEqual(availableInstalledRows("Single-asset cloud strategy", "Build").map((row) => row.symbol), []);
  assert.deepEqual(availableInstalledRows("Single-asset cloud strategy", "Retest").map((row) => row.symbol), ["[SP500]"]);
  resetOfficialSqxChoices();
  const staleFetch = async (path) => {
    if (path === "/api/sqx-build-type-files") return { ok: true, status: 200, json: async () => ({ templates: ["highest_breakout.sqx"], strategies: [] }) };
    if (path === "/api/sqx-ranking-fitness-types") return { ok: true, status: 200, json: async () => ({ types: [{ key: "NetProfit", name: "Net Profit" }] }) };
    if (path === "/api/sqx-installed-data") return { ok: true, status: 200, json: async () => ({ symbols: ["STALE"], sessions: ["No Session"], precisions: [{ key: "1", name: "Selected timeframe" }] }) };
    if (path === "/api/sqx-commission-methods") return { ok: true, status: 200, json: async () => ({ methods: [{ key: "None", name: "None" }] }) };
    throw new Error(path);
  };
  await loadOfficialSettingsLists(staleFetch, () => false);
  assert.equal(officialSqxChoiceState().symbolsReady, false);
  assert.equal(officialSqxChoiceState().symbols, null);
  await loadOfficialSettingsLists(staleFetch, () => true);
  assert.equal(officialSqxChoiceState().symbolsReady, true);
  assert.deepEqual(officialSqxChoiceState().symbols, ["STALE"]);
  resetOfficialSqxChoices();
});

test("Workflow list and pipeline render native names and adjustable settings in this desktop", () => {
  assert.equal(humanizeNativeName("WhatToBuild"), "What To Build");
  const list = renderWorkflowList(catalog(), "Example Workflow");
  assert.match(list, /Example Workflow/);
  assert.match(list, /\[ Tasks \(2\) \]/);
  assert.match(list, /\[ Engine \]/);
  assert.match(list, /\[ Results \]/);
  assert.match(list, /DATABANKS: 1/);
  assert.match(list, /STRATEGIES: 1/);
  assert.match(list, /Create new project/);
  assert.match(list, /Open existing project/);
  assert.match(list, /data-automation-open="Example Workflow"/);
  assert.match(list, /data-automation-open-tab="results"/);
  assert.match(list, /data-automation-control="run_project"/);
  assert.match(list, /data-automation-control="stop_project"/);
  assert.match(list, /data-automation-refresh/);
  assert.doesNotMatch(list, /workspace=validate/);
  assert.doesNotMatch(list, /DJ CFD|GOLD BREAKOUT|NQ_M1_dukas|GBPJPY/);
  assert.doesNotMatch(list, /SQX MCP|StrategyQuant X MCP/);
  const unresolved = catalog();
  unresolved.projects.push({
    name: "Broken Archive",
    status: "unresolved",
    reason_code: "project_resources_unresolved",
    detail: "Project has unresolved resources",
    task_count: null,
    databank_count: 0,
    strategy_count: 0,
    engine: null,
    symbol: null,
    timeframe: null,
    archive_sha256: null,
    source_relative_path: "user/projects/Broken Archive/project.cfx",
  });
  const broken = renderWorkflowList(unresolved);
  assert.match(broken, /Project has unresolved resources/);
  assert.doesNotMatch(broken, /data-automation-project="Broken Archive"[^>]*>[\s\S]*\[ Tasks/);
  const pipeline = renderTaskPipeline(topology(), 1);
  assert.match(pipeline, /Build strategies/);
  assert.match(pipeline, />OOS</);
  assert.match(pipeline, /task-connector/);
  assert.match(pipeline, /data-automation-task-settings="1"/);
  assert.match(pipeline, /data-automation-task-active="1"/);
  assert.match(pipeline, /class="task-add"[^>]*disabled/);
  assert.match(pipeline, /\+ Add new task/);
  const setup = renderNativeSetup(topology().tasks[0]);
  assert.match(setup, /Engine/);
  assert.match(setup, /<select[^>]*data-settings-attribute="engine"[^>]*>[\s\S]*<option value="MetaTrader5" selected>/);
  assert.match(setup, /<option value="MetaTrader5 \(netted\)"/);
  assert.match(setup, /<option value="Stockpicker"/);
  assert.match(setup, /<select[^>]*data-settings-attribute="timeframe"[^>]*>[\s\S]*<option value="H1" selected>/);
  assert.match(setup, /type="radio"[^>]*value="genetic" checked[^>]*data-settings-attribute="generationType"/);
  assert.match(setup, /<input[^>]*data-settings-attribute="dateFrom"[^>]*value="2017.01.03"/);
  assert.match(setup, /<input[^>]*data-settings-attribute="type"[^>]*value="FixedSize"/);
  assert.doesNotMatch(setup, /<select[^>]*disabled/);
  assert.match(setup, /What If/);
  assert.match(setup, /Monte Carlo/);
  assert.match(setup, /data-automation-save-settings/);
  const settingsHtml = renderFullSettings(topology().tasks[0], "WhatToBuild", "Example Workflow");
  assert.match(settingsHtml, /What to build/);
  assert.match(settingsHtml, /data-automation-section="Data"/);
  assert.match(settingsHtml, /tab=settings&amp;task=1&amp;section=WhatToBuild/);
  assert.match(settingsHtml, /data-automation-section="MoneyManagement"/);
  assert.match(settingsHtml, /data-automation-section="CrossChecks"/);
  assert.match(settingsHtml, /data-automation-section="GeneticOptions"/);
  assert.match(settingsHtml, />Genetic options</);
  assert.doesNotMatch(settingsHtml, /Parts to improve/);
  assert.doesNotMatch(settingsHtml, /Ranking|Building blocks|Trading options/);
  const detail = renderWorkflowDetail(topology(), catalog().control, customProjectResultsFromPayload(results()), { tab: "progress", task: 1 });
  assert.match(detail, /data-automation-tab="progress"/);
  assert.match(detail, /data-automation-tab="settings"/);
  assert.match(detail, />Full settings</);
  assert.match(detail, /data-automation-tab="results"/);
  assert.match(detail, />Start</);
  assert.match(detail, /data-automation-control="run_project"/);
  assert.match(detail, /data-automation-control="stop_project"/);
  assert.match(detail, /Pause is available while StrategyQuant X is running this project/);
  assert.match(detail, /data-automation-back/);
  assert.match(detail, />Custom projects</);
  assert.match(detail, /No producer log yet|Native project is running/);
  assert.match(detail, /Total tested/);
  assert.match(detail, /Rate/);
  assert.match(detail, /data-automation-progress-stats/);
  assert.match(detail, /data-automation-native-setup/);
  assert.match(detail, /MetaTrader5/);
  assert.match(detail, /sqx-progress-column-mid/);
  assert.match(detail, /sqx-progress-column-right/);
  assert.match(detail, /data-automation-settings-form/);
  assert.match(detail, /data-settings-kind="flag"/);
  assert.match(detail, /Average strategies per hour/);
  assert.match(detail, /Heap memory chart/);
  assert.doesNotMatch(detail, /Databank Fitness - IS Training/);
  assert.doesNotMatch(detail, /Running time/);
  assert.doesNotMatch(detail, /Fitness series/);
  assert.match(detail, /Example\.sqx/);
  assert.match(detail, /class="task-add"[^>]*disabled/);
  assert.doesNotMatch(detail, /Top Strategy|Top 10 Avg|All Avg/);
  assert.match(detail, /Trusted Launcher Not Configured|No producer log yet/);
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
        json: async () => ({ reason_code: "trusted_launcher_not_configured", detail: "Set SQX_LAUNCHER_SHA256 to the SHA-256 digest of the installed sqcli.exe." }),
      };
    }),
    /SQX_LAUNCHER_SHA256/,
  );
  assert.equal(JSON.parse(body).action, "stop_project");
});

test("Pause posts native pause_project through the same control path", async () => {
  let body = "";
  await requestProjectControl("Example Workflow", "pause_project", async (path, options) => {
    assert.equal(path, "/api/sqx-project-control");
    assert.equal(options.method, "POST");
    body = options.body;
    return { ok: true, status: 200, json: async () => ({ action: "pause_project" }) };
  });
  assert.equal(JSON.parse(body).action, "pause_project");
  assert.equal(JSON.parse(body).project, "Example Workflow");
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
        status: 503,
        json: async () => ({ reason_code: "trusted_launcher_not_configured", detail: "Set SQX_LAUNCHER_SHA256 to the SHA-256 digest of the installed sqcli.exe." }),
      };
    }),
    /SQX_LAUNCHER_SHA256/,
  );
  assert.equal(JSON.parse(body).action, "run_project");
  assert.equal(JSON.parse(body).project, "Example Workflow");
});

test("Progress parser streams producer log lines and accepts engine-channel counts", async () => {
  const { fetchProjectProgress, projectProgressFromPayload } = await import("../web/automation-workflows.mjs");
  const payload = {
    schema: "tc.sqx-custom-project-progress.v1",
    source_build: "144.2953",
    project: "Example Workflow",
    source_relative_path: "user/projects/Example Workflow/project.cfx",
    archive_sha256: "a".repeat(64),
    running: true,
    worker_label: "sqx-project-start:Example Workflow",
    generated: null,
    rejected: null,
    accepted: null,
    rate: null,
    databank_count: 1,
    strategy_count: 0,
    log_lines: [{ relative_path: "log/sqcli.log", text: "Task 1 running" }],
    control: { available: true, reason_code: null },
    detail: "producer files",
  };
  const parsed = projectProgressFromPayload(payload);
  assert.equal(parsed.running, true);
  assert.equal(parsed.log_lines[0].text, "Task 1 running");
  const counted = projectProgressFromPayload({
    ...payload,
    generated: 12,
    rejected: 8,
    accepted: 4,
    rate: 30,
    percent: 25,
    running_status: "paused",
  });
  assert.equal(counted.generated, 12);
  assert.equal(counted.percent, 25);
  assert.throws(() => projectProgressFromPayload({ ...payload, generated: "12" }), /invalid/);
  assert.throws(() => projectProgressFromPayload({ ...payload, percent: 101 }), /invalid/);
  let requested = "";
  const fetched = await fetchProjectProgress("Example Workflow", async (path) => {
    requested = path;
    return { ok: true, status: 200, json: async () => payload };
  });
  assert.equal(requested, "/api/sqx-project-progress?project=Example+Workflow");
  assert.equal(fetched.running, true);
  const html = renderWorkflowDetail(topology(), catalog().control, null, { tab: "progress", task: 1 }, null, "", parsed);
  assert.match(html, /Task 1 running/);
  assert.match(html, /data-automation-progress-running="true"/);
  assert.match(html, /data-automation-control="pause_project"/);
  const pausedHtml = renderWorkflowDetail(topology(), catalog().control, null, { tab: "progress", task: 1 }, null, "", counted);
  assert.match(pausedHtml, /data-automation-control="resume_project"/);
  assert.match(pausedHtml, />12</);
  assert.match(html, /Average strategies per hour/);
  assert.match(html, /Heap memory chart/);
  assert.match(html, /data-chart-state="unavailable"/);
  const charted = projectProgressFromPayload({
    ...payload,
    charts: [{
      type: "SQ.EngineCharts.AverageStrategiesPerHourChart",
      title: "Average strategies per hour",
      series: [{ label: "Avg. strategies per hour", values: [10, 20, 30] }],
    }],
  });
  const chartHtml = renderWorkflowDetail(topology(), catalog().control, null, { tab: "progress", task: 1 }, null, "", charted);
  assert.match(chartHtml, /data-chart-state="current"/);
  assert.match(chartHtml, /Avg\. strategies per hour/);
  assert.throws(() => projectProgressFromPayload({ ...payload, charts: [{ type: "x", title: "x", series: [{ values: [1] }] }] }), /invalid/);
  const cataloged = projectProgressFromPayload({
    ...payload,
    chart_types: [
      { type: "AverageStrategiesPerHourChart", name: "Average strategies per hour" },
      { type: "GeneticEvolutionInfo", name: "Genetic Evolution info" },
    ],
    chart_settings: ["GeneticEvolutionInfo", "AverageStrategiesPerHourChart"],
    charts: [{
      type: "GeneticEvolutionInfo",
      title: "Genetic Evolution info",
      kind: "rows",
      items: [{ name: "No data", value: "No genetic evolution running" }],
    }],
  });
  const picked = renderWorkflowDetail(topology(), catalog().control, null, { tab: "progress", task: 1 }, null, "", cataloged);
  assert.match(picked, /data-engine-chart-slot="0"/);
  assert.match(picked, /Genetic Evolution info/);
  assert.match(picked, /No genetic evolution running/);
  let body = "";
  await saveEngineChartSelection("Builder", 1, "HeapMemoryChart", async (path, options) => {
    assert.equal(path, "/api/sqx-engine-chart-selection");
    body = options.body;
    return { ok: true, status: 200, json: async () => ({ type: "HeapMemoryChart" }) };
  });
  assert.equal(JSON.parse(body).number, 1);
  assert.equal(JSON.parse(body).type, "HeapMemoryChart");
  assert.equal(SQX_PROGRESS_POLL_MS, 2000);
  const live = progressLiveFragments(counted, "Example Workflow");
  assert.equal(live.running, "true");
  assert.match(live.stats, />12</);
  assert.match(live.pause, /resume_project/);
  assert.match(live.bar, /aria-valuenow="25"/);
  assert.match(html, /data-automation-progress-pause/);
  assert.throws(() => projectProgressFromPayload({
    ...payload,
    chart_types: [{ type: "AverageStrategiesPerHourChart", name: "Average strategies per hour" }],
    chart_settings: ["InventedChart", "AverageStrategiesPerHourChart"],
  }), /invalid/);
});

test("Build-type files, ranking fitness types, and template reload use official SQX servlets", async () => {
  const files = await fetchBuildTypeFiles(async (path) => {
    assert.equal(path, "/api/sqx-build-type-files");
    return { ok: true, status: 200, json: async () => ({ templates: ["highest_breakout.sqx"], strategies: ["Strategy 1.sqx"] }) };
  });
  assert.deepEqual(files.templates, ["highest_breakout.sqx"]);
  const ranking = await fetchRankingFitnessTypes(async (path) => {
    assert.equal(path, "/api/sqx-ranking-fitness-types");
    return { ok: true, status: 200, json: async () => ({ types: [{ key: "RExpectancy", name: "R Expectancy (Van Tharp)" }] }) };
  });
  assert.equal(ranking.types[0].key, "RExpectancy");
  const installed = await fetchInstalledDataSymbols(async (path) => {
    assert.equal(path, "/api/sqx-installed-data");
    return {
      ok: true,
      status: 200,
      json: async () => ({
        symbols: ["EURUSD", "DJ CFD"],
        sessions: ["No Session"],
        precisions: [{ key: "1", name: "Selected timeframe" }],
      }),
    };
  });
  assert.deepEqual(installed.symbols, ["EURUSD", "DJ CFD"]);
  assert.deepEqual(installed.sessions, ["No Session"]);
  const commissions = await fetchCommissionMethods(async (path) => {
    assert.equal(path, "/api/sqx-commission-methods");
    return { ok: true, status: 200, json: async () => ({ methods: [{ key: "None", name: "None" }] }) };
  });
  assert.equal(commissions.methods[0].key, "None");
  const series = await fetchSymbolData("2017.01.03", "2023.01.01", "EURUSD", "No Session", async (path, options) => {
    assert.equal(path, "/api/sqx-symbol-data");
    assert.equal(options.method, "POST");
    return { ok: true, status: 200, json: async () => ({ points: [[1, 0], [2, 3]] }) };
  });
  assert.deepEqual(series.points[1], [2, 3]);
  let body = "";
  const reloaded = await requestTemplateReload("Example Workflow", 1, "highest_breakout.sqx", true, async (path, options) => {
    assert.equal(path, "/api/sqx-build-type-template");
    body = options.body;
    return { ok: true, status: 200, json: async () => ({ updated_charts: 2 }) };
  });
  assert.equal(reloaded.updated_charts, 2);
  assert.deepEqual(JSON.parse(body), { project: "Example Workflow", task: 1, fileName: "highest_breakout.sqx", apply: true });
});

test("Calibrate now posts project and task to the native servlet wrapper", async () => {
  let body = "";
  const result = await requestCalibrate("Example Workflow", 1, true, async (path, options) => {
    assert.equal(path, "/api/sqx-calibrate");
    assert.equal(options.method, "POST");
    body = options.body;
    return { ok: true, status: 200, json: async () => ({ updated_blocks: 2, updated_params: 4 }) };
  });
  assert.equal(result.updated_blocks, 2);
  assert.deepEqual(JSON.parse(body), { project: "Example Workflow", task: 1, apply: true });
});

test("exclusive money-management use writes every sibling", () => {
  const radios = [
    { path: ["RiskMoneyManagement", "MoneyManagement", "Method"], checked: false },
    { path: ["RiskMoneyManagement", "MoneyManagement", "Method:2"], checked: true },
  ].map((row) => ({
    checked: row.checked,
    getAttribute(name) {
      if (name === "data-settings-path") return JSON.stringify(row.path);
      if (name === "data-settings-attribute") return "use";
      return "";
    },
  }));
  const group = { querySelectorAll: () => radios };
  assert.deepEqual(exclusiveUseUpdates({ closest: () => group }), [
    { path: ["RiskMoneyManagement", "MoneyManagement", "Method"], attribute: "use", value: "false" },
    { path: ["RiskMoneyManagement", "MoneyManagement", "Method:2"], attribute: "use", value: "true" },
  ]);
  assert.equal(exclusiveUseUpdates({ closest: () => null }), null);
});

test("Money management methods are exclusive existing use radios", () => {
  const html = renderMoneyManagementPane({
    tag: "RiskMoneyManagement",
    path: ["RiskMoneyManagement"],
    attributes: {},
    children: [
      {
        tag: "MoneyManagement",
        path: ["RiskMoneyManagement", "MoneyManagement"],
        attributes: {},
        children: [
          {
            tag: "Method",
            path: ["RiskMoneyManagement", "MoneyManagement", "Method"],
            attributes: { type: "FixedSize", use: "true" },
            children: [{ tag: "Params", path: ["RiskMoneyManagement", "MoneyManagement", "Method", "Params"], attributes: {}, children: [
              { tag: "Param", path: ["RiskMoneyManagement", "MoneyManagement", "Method", "Params", "Param"], attributes: { key: "Size" }, text: "1", children: [] },
            ] }],
          },
          {
            tag: "Method",
            path: ["RiskMoneyManagement", "MoneyManagement", "Method:2"],
            attributes: { type: "FixedAmount", use: "false" },
            children: [],
          },
        ],
      },
    ],
  });
  assert.match(html, /data-settings-exclusive-group/);
  assert.match(html, /data-settings-attribute="use"[^>]*data-settings-exclusive-use="1"/);
  assert.match(html, />Fixed Size</);
  assert.match(html, />Fixed Amount</);
  assert.doesNotMatch(html, /data-settings-kind="flag"/);
  assert.match(html, /data-settings-text="1" value="1"/);
});

test("Settings save posts existing native attributes or text", async () => {
  let body = "";
  const result = await saveProjectSettings("Example Workflow", 1, [
    { path: ["Data", "Setups", "Setup"], attribute: "engine", value: "MetaTrader4" },
    { path: ["Rankings", "MaxStrategies"], text: "500" },
  ], async (path, options) => {
    assert.equal(path, "/api/sqx-project-settings");
    assert.equal(options.method, "POST");
    body = options.body;
    return { ok: true, status: 200, json: async () => ({ updated: 2 }) };
  });
  assert.equal(result.updated, 2);
  assert.deepEqual(JSON.parse(body), {
    project: "Example Workflow",
    task: 1,
    updates: [
      { path: ["Data", "Setups", "Setup"], attribute: "engine", value: "MetaTrader4" },
      { path: ["Rankings", "MaxStrategies"], text: "500" },
    ],
  });
});

function nestedSettings() {
  return [
    {
      tag: "Rankings",
      path: ["Rankings"],
      attributes: {},
      text: null,
      children: [
        {
          tag: "MaxStrategies",
          path: ["Rankings", "MaxStrategies"],
          attributes: {},
          text: "1000",
          children: [],
        },
        {
          tag: "StopCondition",
          path: ["Rankings", "StopCondition"],
          attributes: { type: "databank-full" },
          text: null,
          children: [],
        },
        {
          tag: "FitnessCriteria",
          path: ["Rankings", "FitnessCriteria"],
          attributes: { method: "ComputeFromStrategyResult" },
          text: null,
          children: [
            {
              tag: "Settings",
              path: ["Rankings", "FitnessCriteria", "Settings"],
              attributes: {},
              text: null,
              children: [
                {
                  tag: "Ranking",
                  path: ["Rankings", "FitnessCriteria", "Settings", "Ranking"],
                  attributes: { type: "RExpectancy" },
                  text: null,
                  children: [],
                },
              ],
            },
          ],
        },
        {
          tag: "AutomaticDismissal",
          path: ["Rankings", "AutomaticDismissal"],
          attributes: { warnings: "false" },
          text: null,
          children: [
            { tag: "Problem", path: ["Rankings", "AutomaticDismissal", "Problem:1"], attributes: { code: "1", dismiss: "true" }, text: null, children: [] },
            { tag: "Problem", path: ["Rankings", "AutomaticDismissal", "Problem:2"], attributes: { code: "4", dismiss: "false" }, text: null, children: [] },
          ],
        },
        {
          tag: "Conditions",
          path: ["Rankings", "Conditions"],
          attributes: {},
          text: null,
          children: [
            {
              tag: "Condition",
              path: ["Rankings", "Conditions", "Condition:1"],
              attributes: { use: "true" },
              text: null,
              display: { column: "ProfitFactor", sample: "in-sample", comparator: ">", threshold: 1.3, label: "ProfitFactor (in-sample) > 1.3" },
              children: [
                {
                  tag: "Left-Side",
                  path: ["Rankings", "Conditions", "Condition:1", "Left-Side"],
                  attributes: {},
                  text: null,
                  children: [
                    {
                      tag: "Column-Value",
                      path: ["Rankings", "Conditions", "Condition:1", "Left-Side", "Column-Value"],
                      attributes: { column: "ProfitFactor", sampleType: "10" },
                      text: null,
                      children: [],
                    },
                  ],
                },
                {
                  tag: "Comparator",
                  path: ["Rankings", "Conditions", "Condition:1", "Comparator"],
                  attributes: { value: ">" },
                  text: null,
                  children: [],
                },
                {
                  tag: "Right-Side",
                  path: ["Rankings", "Conditions", "Condition:1", "Right-Side"],
                  attributes: {},
                  text: null,
                  children: [
                    {
                      tag: "Numeric-Value",
                      path: ["Rankings", "Conditions", "Condition:1", "Right-Side", "Numeric-Value"],
                      attributes: { value: "1.3" },
                      text: null,
                      children: [],
                    },
                  ],
                },
              ],
            },
          ],
        },
      ],
    },
    {
      tag: "CrossChecks",
      path: ["CrossChecks"],
      attributes: { use: "true" },
      text: null,
      children: [
        {
          tag: "WalkForwardOptimization",
          path: ["CrossChecks", "WalkForwardOptimization"],
          attributes: { use: "false" },
          text: null,
          children: [
            {
              tag: "Settings",
              path: ["CrossChecks", "WalkForwardOptimization", "Settings"],
              attributes: {},
              text: null,
              children: [
                {
                  tag: "WalkForward",
                  path: ["CrossChecks", "WalkForwardOptimization", "Settings", "WalkForward"],
                  attributes: { optimization: "15", period: "10", type: "0" },
                  text: null,
                  children: [
                    {
                      tag: "Param1",
                      path: ["CrossChecks", "WalkForwardOptimization", "Settings", "WalkForward", "Param1"],
                      attributes: { value: "20" },
                      text: null,
                      children: [],
                    },
                    {
                      tag: "Param2",
                      path: ["CrossChecks", "WalkForwardOptimization", "Settings", "WalkForward", "Param2"],
                      attributes: { value: "10" },
                      text: null,
                      children: [],
                    },
                  ],
                },
                {
                  tag: "MaxTests",
                  path: ["CrossChecks", "WalkForwardOptimization", "Settings", "MaxTests"],
                  attributes: {},
                  text: "1000",
                  children: [],
                },
              ],
            },
            {
              tag: "AcceptanceSettings",
              path: ["CrossChecks", "WalkForwardOptimization", "AcceptanceSettings"],
              attributes: {},
              text: null,
              children: [{ tag: "Conditions", path: ["CrossChecks", "WalkForwardOptimization", "AcceptanceSettings", "Conditions"], attributes: {}, text: null, children: [] }],
            },
          ],
        },
        {
          tag: "WhatIf",
          path: ["CrossChecks", "WhatIf"],
          attributes: { use: "false" },
          text: null,
          children: [],
        },
        {
          tag: "RetestWithHigherPrecision",
          path: ["CrossChecks", "RetestWithHigherPrecision"],
          attributes: { use: "true" },
          text: null,
          children: [
            {
              tag: "Settings",
              path: ["CrossChecks", "RetestWithHigherPrecision", "Settings"],
              attributes: {},
              text: null,
              children: [
                { tag: "Precision", path: ["CrossChecks", "RetestWithHigherPrecision", "Settings", "Precision"], attributes: {}, text: "2", children: [] },
                { tag: "Spread", path: ["CrossChecks", "RetestWithHigherPrecision", "Settings", "Spread"], attributes: {}, text: "2", children: [] },
              ],
            },
            {
              tag: "AcceptanceSettings",
              path: ["CrossChecks", "RetestWithHigherPrecision", "AcceptanceSettings"],
              attributes: {},
              text: null,
              children: [
                {
                  tag: "Conditions",
                  path: ["CrossChecks", "RetestWithHigherPrecision", "AcceptanceSettings", "Conditions"],
                  attributes: {},
                  text: null,
                  children: [
                    {
                      tag: "Condition",
                      path: ["CrossChecks", "RetestWithHigherPrecision", "AcceptanceSettings", "Conditions", "Condition:1"],
                      attributes: { use: "true" },
                      text: null,
                      children: [
                        {
                          tag: "Left-Side",
                          path: ["CrossChecks", "RetestWithHigherPrecision", "AcceptanceSettings", "Conditions", "Condition:1", "Left-Side"],
                          attributes: {},
                          text: null,
                          children: [
                            {
                              tag: "Column-Value",
                              path: ["CrossChecks", "RetestWithHigherPrecision", "AcceptanceSettings", "Conditions", "Condition:1", "Left-Side", "Column-Value"],
                              attributes: { column: "ProfitFactor", sampleType: "127" },
                              text: null,
                              children: [],
                            },
                          ],
                        },
                        {
                          tag: "Comparator",
                          path: ["CrossChecks", "RetestWithHigherPrecision", "AcceptanceSettings", "Conditions", "Condition:1", "Comparator"],
                          attributes: { value: ">" },
                          text: null,
                          children: [],
                        },
                        {
                          tag: "Right-Side",
                          path: ["CrossChecks", "RetestWithHigherPrecision", "AcceptanceSettings", "Conditions", "Condition:1", "Right-Side"],
                          attributes: {},
                          text: null,
                          children: [
                            {
                              tag: "Numeric-Value",
                              path: ["CrossChecks", "RetestWithHigherPrecision", "AcceptanceSettings", "Conditions", "Condition:1", "Right-Side", "Numeric-Value"],
                              attributes: { value: "1.3" },
                              text: null,
                              children: [],
                            },
                          ],
                        },
                      ],
                    },
                  ],
                },
              ],
            },
          ],
        },
      ],
    },
  ];
}

test("Ranking table and Cross-check Open view render from the saved XML tree", () => {
  resetOfficialSqxChoices();
  const task = { native_task_index: 1, kind: "Build", settings: nestedSettings() };
  const rankings = renderRankingsPane(nestedSettings()[0]);
  assert.match(rankings, /settings-condition-table/);
  assert.match(rankings, /ProfitFactor/);
  assert.match(rankings, /in-sample/);
  assert.match(rankings, /Sample Type/);
  assert.match(rankings, /Threshold/);
  assert.match(rankings, /data-settings-text="1"/);
  assert.match(rankings, /value="1000"/);
  assert.match(rankings, /value="1.3"/);
  assert.match(rankings, /<input[^>]*data-settings-attribute="value"[^>]*value="&gt;"/);
  assert.match(rankings, /<input[^>]*data-settings-attribute="value"[^>]*value="1.3"/);
  assert.doesNotMatch(rankings, /disabled/);
  assert.doesNotMatch(rankings, /Net profit|Return \/ Drawdown|Van Tharp/);
  assert.match(rankings, /fitnessMethodStrategyResult\/list/);
  assert.doesNotMatch(rankings, /data-settings-attribute="type"[^>]*value="RExpectancy"/);
  resetOfficialSqxChoices();
  setOfficialSqxChoices({
    rankingTypes: [
      { key: "NetProfit", name: "Net Profit (Return)" },
      { key: "ReturnDDRatio", name: "Return / Drawdown ratio" },
      { key: "RExpectancy", name: "R Expectancy (Van Tharp)" },
    ],
  });
  const ranked = renderRankingsPane(nestedSettings()[0]);
  assert.match(ranked, /type="radio"[^>]*value="RExpectancy" checked[^>]*data-settings-attribute="type"/);
  assert.match(ranked, /type="radio"[^>]*value="NetProfit"[^>]*data-settings-attribute="type"/);
  resetOfficialSqxChoices();
  assert.match(rankings, /Maximum top strategies to store/);
  assert.match(rankings, /Strategy Quality ranking \(fitness\)/);
  assert.match(rankings, /Strategy filtering conditions/);
  assert.match(rankings, /data-settings-subgroup="Stop generation when"/);
  assert.match(rankings, /data-settings-subgroup="Compute from"/);
  assert.match(rankings, /data-settings-subgroup="Custom filters"/);
  assert.match(rankings, /sqx-settings-grid-ranking/);
  assert.match(rankings, /sqx-settings-grid-col-left/);
  assert.match(rankings, /sqx-settings-grid-col-right/);
  assert.match(rankings, /Maximum strategies to store in databank/);
  assert.match(rankings, /data-settings-dialog="ranking-automatic-filters"/);
  assert.match(rankings, /data-settings-tag="Problem"/);
  assert.match(rankings, /data-settings-attribute="code"[^>]*value="1"/);
  assert.match(rankings, /Problem Dismiss/);
  const cross = renderCrossChecksPane(nestedSettings()[1], { project: "RetainedBuildTask", taskIndex: 1 });
  assert.match(cross, /Walk Forward Optimization/);
  assert.match(cross, /data-cross-check-tier="tier-extensive"/);
  assert.match(cross, /data-cross-check-tier="tier-basic"/);
  assert.match(cross, /sqx-settings-card/);
  assert.match(cross, /BASIC \(FAST\)/);
  assert.match(cross, /data-settings-dialog="cross-WalkForwardOptimization-settings"/);
  assert.match(cross, /data-settings-dialog="cross-WalkForwardOptimization-filtering"/);
  assert.match(cross, /data-settings-dialog="cross-RetestWithHigherPrecision-settings"/);
  assert.match(cross, /data-settings-dialog="cross-RetestWithHigherPrecision-filtering"/);
  assert.match(cross, /data-settings-tag="Precision"/);
  assert.match(cross, /data-settings-tag="WalkForward"/);
  assert.match(cross, /data-automation-method="WalkForwardOptimization"/);
  assert.match(cross, />Open</);
  assert.match(cross, /What If/);
  assert.match(cross, /Higher Precision|Retest With Higher Precision/);
  assert.doesNotMatch(cross, /data-automation-method="WhatIf"/);
  assert.doesNotMatch(cross, /EURUSD|GBPUSD|DJ CFD/);
  assert.doesNotMatch(cross, /data-settings-exclusive-group/);
  const opened = renderFullSettings(task, "CrossChecks", "RetainedBuildTask", "WalkForwardOptimization", "settings");
  assert.match(opened, /data-cross-check-method="WalkForwardOptimization"/);
  assert.match(opened, /data-automation-method-pane="settings"/);
  assert.match(opened, /data-automation-method-pane="filtering"/);
  assert.match(opened, /value="20"/);
  assert.match(opened, /data-settings-text="1"/);
  assert.match(opened, /value="1000"/);
  assert.doesNotMatch(opened, /<select[^>]*disabled/);
});

function documentedTask() {
  return {
    native_task_index: 1,
    kind: "Build",
    settings: [
      {
        tag: "WhatToBuild",
        path: ["WhatToBuild"],
        attributes: {},
        text: null,
        children: [
          {
            tag: "StrategyType",
            path: ["WhatToBuild", "StrategyType"],
            attributes: {
              type: "improve",
              additionalCharts: "2",
              templateFile: "not set",
              improveType: "strategy",
              strategyFile: "not set",
              improveDatabank: "Strategies to improve",
              architecture: "sq4",
            },
            text: null,
            children: [],
          },
          {
            tag: "MarketSides",
            path: ["WhatToBuild", "MarketSides"],
            attributes: { type: "both" },
            text: null,
            children: [
              { tag: "EntrySymmetry", path: ["WhatToBuild", "MarketSides", "EntrySymmetry"], attributes: {}, text: "false", children: [] },
            ],
          },
          {
            tag: "BuildMode",
            path: ["WhatToBuild", "BuildMode"],
            attributes: { generationType: "genetic-evolution" },
            text: null,
            children: [
              { tag: "PopulationSize", path: ["WhatToBuild", "BuildMode", "PopulationSize"], attributes: {}, text: "100", children: [] },
              { tag: "Islands", path: ["WhatToBuild", "BuildMode", "Islands"], attributes: {}, text: "4", children: [] },
            ],
          },
          {
            tag: "SLPTOptions",
            path: ["WhatToBuild", "SLPTOptions"],
            attributes: {},
            text: null,
            children: [
              { tag: "SLRequired", path: ["WhatToBuild", "SLPTOptions", "SLRequired"], attributes: {}, text: "true", children: [] },
              { tag: "PTRequired", path: ["WhatToBuild", "SLPTOptions", "PTRequired"], attributes: {}, text: "true", children: [] },
            ],
          },
        ],
      },
      {
        tag: "PartsToImprove",
        path: ["PartsToImprove"],
        attributes: { improveATM: "false" },
        text: null,
        children: [
          {
            tag: "ExitRules",
            path: ["PartsToImprove", "ExitRules"],
            attributes: {},
            text: null,
            children: [
              { tag: "LongImprovement", path: ["PartsToImprove", "ExitRules", "LongImprovement"], attributes: { use: "true", action: "add-or-replace" }, text: null, children: [] },
            ],
          },
        ],
      },
      {
        tag: "Blocks",
        path: ["Blocks"],
        attributes: { type: "simple" },
        text: null,
        children: [
          { tag: "Calibration", path: ["Blocks", "Calibration"], attributes: { maxSteps: "50" }, text: null, children: [] },
          {
            tag: "BuildingBlocks",
            path: ["Blocks", "BuildingBlocks"],
            attributes: {},
            text: null,
            children: [
              {
                tag: "Block",
                path: ["Blocks", "BuildingBlocks", "Block:3"],
                attributes: { key: "AND", use: "true", weight: "1", category: "signals" },
                text: null,
                children: [],
              },
              {
                tag: "Block",
                path: ["Blocks", "BuildingBlocks", "Block:1"],
                attributes: { key: "Price.Close", use: "true", weight: "1", category: "signals" },
                text: null,
                children: [
                  { tag: "Generated", path: ["Blocks", "BuildingBlocks", "Block:1", "Generated"], attributes: { weight: "1" }, text: null, children: [] },
                ],
              },
              {
                tag: "Block",
                path: ["Blocks", "BuildingBlocks", "Block:2"],
                attributes: { key: "Indicators.RSI", use: "false", weight: "1", category: "indicators" },
                text: null,
                children: [],
              },
            ],
          },
        ],
      },
      {
        tag: "Options",
        path: ["Options"],
        attributes: {},
        text: null,
        children: [
          {
            tag: "BuildTradingOptions",
            path: ["Options", "BuildTradingOptions"],
            attributes: {},
            text: null,
            children: [
              {
                tag: "Params",
                path: ["Options", "BuildTradingOptions", "Params"],
                attributes: {},
                text: null,
                children: [
                  { tag: "Param", path: ["Options", "BuildTradingOptions", "Params", "Param:1"], attributes: { key: "ExitAtEndOfDay" }, text: "false", children: [] },
                  { tag: "Param", path: ["Options", "BuildTradingOptions", "Params", "Param:2"], attributes: { key: "StoreChartData" }, text: "false", children: [] },
                  { tag: "Param", path: ["Options", "BuildTradingOptions", "Params", "Param:3"], attributes: { key: "MaxTradesPerDay" }, text: "0", children: [] },
                ],
              },
            ],
          },
        ],
      },
    ],
  };
}

test("Documented Full settings groups follow SQX panes and existing XML only", () => {
  resetOfficialSqxChoices();
  const task = documentedTask();
  assert.equal(isImproveExisting(task), true);
  assert.deepEqual(documentedSettingsTabs(task).map((tab) => tab.id), [
    "WhatToBuild",
    "PartsToImprove",
    "GeneticOptions",
    "Options",
    "Blocks",
  ]);
  const what = renderFullSettings(task, "WhatToBuild", "RetainedBuildTask");
  assert.match(what, /data-settings-group="Strategy type"/);
  assert.match(what, /sqx-what-types/);
  assert.match(what, /Additional build config/);
  assert.match(what, /data-settings-group="Trading direction \/ symmetry"/);
  assert.match(what, /data-settings-group="Build mode"/);
  assert.match(what, /type="radio"[^>]*value="genetic-evolution" checked[^>]*data-settings-attribute="generationType"/);
  assert.match(what, /type="radio"[^>]*value="simple"[^>]*data-settings-attribute="type"/);
  assert.match(what, /type="radio"[^>]*value="multi-tf"[^>]*data-settings-attribute="type"/);
  assert.match(what, /type="radio"[^>]*value="template"[^>]*data-settings-attribute="type"/);
  assert.match(what, /type="radio"[^>]*value="improve" checked[^>]*data-settings-attribute="type"/);
  assert.match(what, /data-strategy-type="multi-tf"/);
  assert.match(what, /data-strategy-type="template"/);
  assert.match(what, /data-settings-attribute="additionalCharts"[^>]*value="2"/);
  assert.match(what, /data-settings-attribute="templateFile"[^>]*value="not set"/);
  assert.match(what, /data-settings-browse-files="templates"/);
  assert.match(what, /data-settings-reload-template/);
  assert.match(what, /data-settings-browse-files="strategies"/);
  assert.match(what, /type="radio"[^>]*value="strategy" checked[^>]*data-settings-attribute="improveType"/);
  assert.match(what, /data-settings-attribute="strategyFile"[^>]*value="not set"/);
  assert.match(what, /data-settings-group="Strategy style"/);
  assert.match(what, /type="radio"[^>]*value="sq4" checked[^>]*data-settings-attribute="architecture"/);
  assert.match(what, /type="radio"[^>]*value="both" checked[^>]*data-settings-attribute="type"/);
  assert.match(what, /data-settings-dialog-save/);
  assert.match(what, /data-settings-group="Stop loss"/);
  assert.match(what, /data-settings-group="Profit target"/);
  assert.match(what, /data-settings-dialog="what-stop-loss"/);
  assert.match(what, /data-settings-dialog="what-profit-target"/);
  assert.match(what, /data-settings-tag="SLRequired"[\s\S]*data-settings-kind="flag"/);
  assert.match(what, /data-settings-tag="EntrySymmetry"[\s\S]*data-settings-kind="flag"/);
  assert.match(what, /data-settings-dialog="what-trading-symmetry"/);
  assert.match(what, /sqx-settings-card/);
  assert.match(what, /data-settings-dialog="what-build-mode"/);
  assert.match(what, />Genetic options</);
  assert.match(what, />Parts to improve</);
  assert.match(what, />Building blocks</);
  assert.match(what, />Trading options</);
  assert.match(what, /settings-section-roll/);
  assert.match(what, /data-settings-tab-roll/);
  assert.deepEqual([...what.matchAll(/data-automation-section="([^"]+)"/g)].map((match) => match[1]), [
    "WhatToBuild",
    "PartsToImprove",
    "GeneticOptions",
    "Options",
    "Blocks",
  ]);
  assert.doesNotMatch(what, /PopulationSize|Islands/);
  const genetic = renderFullSettings(task, "GeneticOptions", "RetainedBuildTask");
  assert.match(genetic, /data-genetic-options="1"/);
  assert.match(genetic, /sqx-settings-grid/);
  assert.match(genetic, /sqx-settings-card/);
  assert.match(genetic, /value="100"/);
  assert.match(genetic, /value="4"/);
  assert.doesNotMatch(genetic, /Other settings/);
  const parts = renderFullSettings(task, "PartsToImprove", "RetainedBuildTask");
  assert.match(parts, /data-settings-group="Exit rules"/);
  assert.match(parts, /add-or-replace/);
  assert.match(parts, /data-settings-attribute="action"/);
  assert.match(parts, /type="radio"[^>]*value="add-or-replace"/);
  assert.match(parts, /type="radio"[^>]*value="replace"/);
  assert.match(parts, /type="radio"[^>]*value="add"/);
  const blocks = renderBuildingBlocksPane(task.settings[2], { project: "RetainedBuildTask", taskIndex: 1 });
  assert.match(blocks, /data-settings-group="Signals"/);
  assert.match(blocks, /settings-block-title">Signals</);
  assert.match(blocks, /data-settings-block-panel="signals"/);
  assert.doesNotMatch(blocks, /Predefined conditions/);
  assert.doesNotMatch(blocks, /Signals \(Predefined conditions\)/);
  assert.match(blocks, /data-settings-group="Indicators"/);
  assert.match(blocks, /data-settings-az="A"/);
  assert.match(blocks, /data-settings-block-panel="signals"/);
  assert.match(blocks, /data-block-key="Price.Close"/);
  assert.match(blocks, />Price Close</);
  assert.match(blocks, /settings-use/);
  assert.match(blocks, /data-automation-block="Blocks\/BuildingBlocks\/Block:1"/);
  assert.match(blocks, />Custom</);
  const signals = blocks.split('data-settings-group="Signals"')[1].split("data-settings-group=")[0];
  assert.ok(signals.indexOf('data-block-key="AND"') < signals.indexOf('data-block-key="Price.Close"'));
  assert.match(blocks, /data-settings-block-panel="indicators"/);
  assert.doesNotMatch(blocks, /data-settings-block-panel="indicators" hidden/);
  assert.match(blocks, /data-settings-calibrate-open/);
  assert.match(blocks, /data-settings-calibrate-now/);
  assert.match(blocks, />Calibrate now</);
  assert.doesNotMatch(blocks, /data-settings-tag="Generated"/);
  assert.doesNotMatch(blocks, /BASIC|STANDARD|EXTENSIVE/);
  const options = renderFullSettings(task, "Options", "RetainedBuildTask");
  assert.match(options, /data-settings-group="End of day \/ Friday"/);
  assert.match(options, /data-settings-group="Store chart data"/);
  assert.match(options, /data-settings-group="Max trades"/);
  const randomTask = structuredClone(task);
  randomTask.settings[0].children[2].attributes.generationType = "random";
  randomTask.settings[0].children[0].attributes.type = "new";
  assert.equal(isImproveExisting(randomTask), false);
  assert.deepEqual(documentedSettingsTabs(randomTask).map((tab) => tab.id), ["WhatToBuild", "Options", "Blocks"]);
  const randomWhat = renderFullSettings(randomTask, "WhatToBuild", "RetainedBuildTask");
  assert.match(randomWhat, /value="100"/);
  assert.match(randomWhat, /value="4"/);
  assert.doesNotMatch(randomWhat, /data-automation-section="GeneticOptions"/);
});

function builderPaneTask() {
  return {
    native_task_index: 1,
    kind: "Build",
    name: "Build",
    settings: [
      {
        tag: "WhatToBuild",
        path: ["WhatToBuild"],
        attributes: {},
        text: null,
        children: [
          {
            tag: "BuildMode",
            path: ["WhatToBuild", "BuildMode"],
            attributes: { generationType: "genetic-evolution" },
            text: null,
            children: [
              { tag: "PopulationSize", path: ["WhatToBuild", "BuildMode", "PopulationSize"], attributes: {}, text: "50", children: [] },
              { tag: "ShowAdvancedGeneticSettings", path: ["WhatToBuild", "BuildMode", "ShowAdvancedGeneticSettings"], attributes: {}, text: "false", children: [] },
              { tag: "ShowLastGenerationDatabank", path: ["WhatToBuild", "BuildMode", "ShowLastGenerationDatabank"], attributes: {}, text: "true", children: [] },
            ],
          },
        ],
      },
      {
        tag: "Data",
        path: ["Data"],
        attributes: {},
        text: null,
        children: [
          {
            tag: "OutOfSample",
            path: ["Data", "OutOfSample"],
            attributes: { showGraph: "false" },
            text: null,
            children: [
              { tag: "Range", path: ["Data", "OutOfSample", "Range:1"], attributes: { dateFrom: "2017.03.20", dateTo: "2017.06.04", type: "isv" }, text: null, children: [] },
              { tag: "Range", path: ["Data", "OutOfSample", "Range:2"], attributes: { dateFrom: "2018.01.18", dateTo: "2018.04.04", type: "isv" }, text: null, children: [] },
            ],
          },
        ],
      },
      {
        tag: "Options",
        path: ["Options"],
        attributes: {},
        text: null,
        children: [
          {
            tag: "BuildTradingOptions",
            path: ["Options", "BuildTradingOptions"],
            attributes: {},
            text: null,
            children: [
              {
                tag: "Params",
                path: ["Options", "BuildTradingOptions", "Params"],
                attributes: {},
                text: null,
                children: [
                  { tag: "Param", path: ["Options", "BuildTradingOptions", "Params", "Param:1"], attributes: { key: "MinimumSL" }, text: "0", children: [] },
                  { tag: "Param", path: ["Options", "BuildTradingOptions", "Params", "Param:2"], attributes: { key: "MaximumSL" }, text: "0", children: [] },
                  { tag: "Param", path: ["Options", "BuildTradingOptions", "Params", "Param:3"], attributes: { key: "StoreChartData" }, text: "false", children: [] },
                ],
              },
            ],
          },
        ],
      },
      {
        tag: "ATMs",
        path: ["ATMs"],
        attributes: { enable: "false", minSize: "0.1" },
        text: null,
        children: [
          { tag: "ATM", path: ["ATMs", "ATM"], attributes: { id: "global" }, text: null, children: [{ tag: "Exits", path: ["ATMs", "ATM", "Exits"], attributes: {}, text: null, children: [] }] },
          {
            tag: "GenerateConfig",
            path: ["ATMs", "GenerateConfig"],
            attributes: {},
            text: null,
            children: [
              {
                tag: "Types",
                path: ["ATMs", "GenerateConfig", "Types"],
                attributes: {},
                text: null,
                children: [{ tag: "SLMultiple", path: ["ATMs", "GenerateConfig", "Types", "SLMultiple"], attributes: { use: "false", minMultiplier: "0.5", maxMultiplier: "2" }, text: null, children: [] }],
              },
              {
                tag: "Scenarios",
                path: ["ATMs", "GenerateConfig", "Scenarios"],
                attributes: {},
                text: null,
                children: [{ tag: "TwoExits", path: ["ATMs", "GenerateConfig", "Scenarios", "TwoExits"], attributes: { exit5050: "false" }, text: null, children: [] }],
              },
            ],
          },
        ],
      },
      {
        tag: "Databanks",
        path: ["Databanks"],
        attributes: {},
        text: null,
        children: [
          { tag: "Databank", path: ["Databanks", "Databank:1"], attributes: { label: "Output databank", name: "Output", value: "Results" }, text: null, children: [] },
          { tag: "Databank", path: ["Databanks", "Databank:2"], attributes: { label: "Input databank", name: "Input", value: "Initial population" }, text: null, children: [] },
        ],
      },
      {
        tag: "RiskMoneyManagement",
        path: ["RiskMoneyManagement"],
        attributes: {},
        text: null,
        children: [
          {
            tag: "MoneyManagement",
            path: ["RiskMoneyManagement", "MoneyManagement"],
            attributes: {},
            text: null,
            children: [
              { tag: "Method", path: ["RiskMoneyManagement", "MoneyManagement", "Method"], attributes: { type: "FixedSize", use: "true" }, text: null, children: [] },
              { tag: "Method", path: ["RiskMoneyManagement", "MoneyManagement", "Method:2"], attributes: { type: "FixedAmount", use: "false" }, text: null, children: [] },
            ],
          },
        ],
      },
    ],
  };
}

test("Builder-like Full settings panes bucket existing XML without Other dumps", () => {
  const task = builderPaneTask();
  const genetic = renderFullSettings(task, "GeneticOptions", "Builder");
  assert.match(genetic, /data-settings-group="Genetic options"[\s\S]*ShowAdvancedGeneticSettings/);
  assert.match(genetic, /data-settings-group="&quot;Fresh blood&quot;"[\s\S]*ShowLastGenerationDatabank/);
  assert.doesNotMatch(genetic, /Other settings/);
  const data = renderFullSettings(task, "Data", "Builder");
  assert.match(data, /data-settings-group="Data range \/ OOS"/);
  assert.match(data, /settings-oos-range/);
  assert.match(data, /value="2017.03.20"/);
  assert.doesNotMatch(data, /<h4>Range<\/h4>/);
  const options = renderFullSettings(task, "Options", "Builder");
  assert.match(options, /data-settings-group="Min \/ max SL and PT"/);
  assert.match(options, /data-settings-group="Store chart data"/);
  const atm = renderFullSettings(task, "ATMs", "Builder");
  assert.match(atm, /data-settings-group="Generate exit types"/);
  assert.match(atm, /data-settings-group="Generate scenarios"/);
  assert.match(atm, /data-settings-tag="SLMultiple"/);
  assert.match(atm, /data-settings-tag="TwoExits"/);
  assert.doesNotMatch(atm, /data-settings-group="Generate config"/);
  const databanks = renderFullSettings(task, "Databanks", "Builder");
  assert.match(databanks, /data-settings-group="Output databanks"/);
  assert.match(databanks, /data-settings-group="Input databanks"/);
  assert.match(databanks, /value="Results"/);
  assert.match(databanks, /value="Initial population"/);
  const mm = renderFullSettings(task, "RiskMoneyManagement", "Builder");
  assert.match(mm, /data-settings-exclusive-group/);
  assert.match(mm, /data-settings-exclusive-use="1"[^>]*>Fixed Size/);
  assert.match(mm, /settings-mm-method is-selected/);
});

test("Results open inspectable archives without inventing Net Profit", () => {
  const parsed = customProjectResultsFromPayload(results());
  const html = renderResultsPanel(topology(), parsed, { task: 1 });
  assert.match(html, /data-automation-archive="Example.sqx"/);
  assert.match(html, /data-automation-databank="Results"/);
  assert.match(html, /archive=Example.sqx&amp;resultView=overview/);
  assert.match(html, /No result chosen - Double-click on result on databank to see the details/);
  assert.match(html, /data-automation-result-view="overview"/);
  assert.match(html, /data-automation-result-view="sp-overview"/);
  assert.doesNotMatch(html, /data-automation-result-view="prop-mc"/);
  assert.match(html, /data-results-databank-toolbar/);
  assert.doesNotMatch(html, />Load</);
  assert.doesNotMatch(html, />FAILED:/);
  assert.doesNotMatch(html, />PASSED:/);
  assert.match(html, /Records:/);
  assert.match(html, /data-results-toolbar/);
  assert.match(html, /\+ New analysis/);
  assert.match(html, /data-results-new-analysis/);
  assert.doesNotMatch(html, /Net Profit|\$\s?\d/);
  const failed = renderResultsPanel(topology(), parsed, {
    task: 1,
    databank: "Results",
    archive: "Example.sqx",
    resultView: "trades",
  }, null, "archive unreadable");
  assert.match(failed, /data-automation-result-view="trades"/);
  assert.match(failed, /data-results-toolbar/);
  assert.match(failed, /Could not inspect this archive/);
  const loading = renderResultsPanel(topology(), parsed, {
    task: 1,
    databank: "Results",
    archive: "Example.sqx",
    resultView: "equity",
  }, null, "");
  assert.match(loading, /data-automation-result-view="equity"/);
  assert.match(loading, /Reading archive/);
  const strategy = projectStrategyFromPayload({
    schema: "tc.sqx-custom-project-strategy.v1",
    source_build: "144.2953",
    project: "Example Workflow",
    databank: "Results",
    archive: "Example.sqx",
    relative_path: "user/projects/Example Workflow/databanks/Results/Example.sqx",
    archive_sha256: "b".repeat(64),
    native_version: "144.2953",
    archive_entries: ["settings.xml", "strategy_Portfolio.xml", "version.txt", "orders.bin"],
    task_index: 1,
    orders: {
      state: "available",
      payload: {
        trades: [{ Ticket: 1, Type: 1, PL: 100, Symbol: "ES", Size: 1, OpenTime: 1, CloseTime: 2, PipsPL: 1 }],
      },
    },
    equity: [{ time: 2, balance: 10100 }],
    equity_basis: "archive_initial_capital",
    initial_capital: 10000,
    settings: [],
    config_diff: [],
    chart: {
      stored: false,
      entries: [],
      store_chart_data: false,
      reason_code: "chart_data_not_stored",
      detail: "not stored",
      bars: {
        state: "available",
        basis: "databank_sidecar_tradestation_csv",
        timeframe: "H1",
        symbol: "ES",
        bars: [
          { open_time: "1970-01-01T00:00:00Z", open: 1, high: 2, low: 0.5, close: 1.5 },
        ],
      },
    },
    detail: "producer",
    result_name: "Example",
    result_key: "Main: ES/H1",
    fitnesses: { IS: 0.91 },
    statistics: {
      basis: "sqx_column_formulas_over_orders.bin",
      full: {
        all: { NumberOfTrades: 1, NumberOfProfits: 1, NumberOfLosses: 0, NetProfit: 100, GrossProfit: 100, GrossLoss: 0, WinningPct: 100, ProfitFactor: 5, Drawdown: 0, DrawdownPct: 0, ReturnDDRatio: 10, Expectancy: 100, AvgTradesPerMonth: 1, MaxConsecLosses: 0, final_equity: 10100, months_basis: "traded_span" },
        long: { NumberOfTrades: 1, NumberOfProfits: 1, NumberOfLosses: 0, NetProfit: 100, GrossProfit: 100, GrossLoss: 0, WinningPct: 100, ProfitFactor: 5, Drawdown: 0, DrawdownPct: 0, ReturnDDRatio: 10, Expectancy: 100, AvgTradesPerMonth: 1, MaxConsecLosses: 0, final_equity: 10100, months_basis: "traded_span" },
        short: null,
      },
      is: { all: null, long: null, short: null },
      oos: { all: null, long: null, short: null },
    },
    symbols: [{ symbol: "ES", NumberOfTrades: 1, NetProfit: 100, ProfitFactor: 5, WinningPct: 100 }],
    trade_analysis: { period_by: "close_time", years: [{ period: "2020", net_profit: 100 }], mae_avg: 10, mfe_avg: 20 },
    profile: [{ mae: 10, mfe: 20, pl: 100 }, { mae: 12, mfe: 8, pl: -5 }],
    source: { state: "available", language: "Strategy XML", member: "strategy_Portfolio.xml", text: "<Strategy/>", reason_code: null, detail: "xml" },
    results_plugins: [
      { id: "prop-mc", folder: "Prop Monte Carlo", title: "Prop Monte Carlo", installed: true },
      { id: "prop-analytics", folder: "Prop analytics", title: "Prop analytics", installed: true },
    ],
    results_plugin_create: { available: true, template: "CustomPlugin" },
  });
  const trades = renderResultsPanel(topology(), parsed, {
    task: 1,
    databank: "Results",
    archive: "Example.sqx",
    resultView: "trades",
  }, strategy);
  assert.match(trades, /data-native-trade-ticket="1"/);
  assert.match(trades, /OpenPrice|Open<\/th>/);
  assert.match(trades, /data-results-trades-state="ready"/);
  assert.match(trades, /List of trades/);
  assert.match(trades, /data-results-toolbar/);
  assert.doesNotMatch(trades, /Net Profit/);
  const overview = renderResultsPanel(topology(), parsed, {
    task: 1,
    databank: "Results",
    archive: "Example.sqx",
    resultView: "overview",
  }, strategy);
  assert.match(overview, /data-results-overview/);
  assert.match(overview, /data-results-overview-state="ready"/);
  assert.match(overview, /data-results-overview-stats="1"/);
  assert.match(overview, /Total Net Profit/);
  assert.match(overview, /TS Overview/);
  assert.match(overview, /data-overview-frame/);
  const source = renderResultsPanel(topology(), parsed, {
    task: 1,
    databank: "Results",
    archive: "Example.sqx",
    resultView: "source",
  }, strategy);
  assert.match(source, /data-results-source/);
  assert.match(source, /data-source-type/);
  assert.match(source, /data-source-mm/);
  assert.match(source, /data-source-save-ea="mt4"/);
  assert.match(source, /data-source-configure/);
  assert.match(source, /&lt;Strategy\/&gt;/);
  assert.match(overview, /data-results-new-analysis/);
  assert.doesNotMatch(overview, /data-results-new-analysis disabled/);
  const sp = renderResultsPanel(topology(), parsed, {
    task: 1,
    databank: "Results",
    archive: "Example.sqx",
    resultView: "sp-overview",
  }, strategy);
  assert.match(sp, /data-results-sp-overview/);
  assert.match(sp, />ES</);
  const analysis = renderResultsPanel(topology(), parsed, {
    task: 1,
    databank: "Results",
    archive: "Example.sqx",
    resultView: "trade-analysis",
  }, strategy);
  assert.match(analysis, /data-results-trade-analysis/);
  assert.match(analysis, />2020</);
  const profile = renderResultsPanel(topology(), parsed, {
    task: 1,
    databank: "Results",
    archive: "Example.sqx",
    resultView: "profile",
  }, strategy);
  assert.match(profile, /data-results-profile/);
  const chart = renderResultsPanel(topology(), parsed, {
    task: 1,
    databank: "Results",
    archive: "Example.sqx",
    resultView: "chart",
  }, strategy);
  assert.match(chart, /data-results-chart="sidecar"/);
  assert.match(chart, /data-chart-basis="databank_sidecar_tradestation_csv"/);
  assert.match(chart, /data-chart-toolbar/);
  assert.match(chart, /data-chart-indicators-toggle/);
  assert.match(chart, /data-chart-indicator-list/);
  assert.match(chart, /data-chart-body/);
  assert.match(chart, /Store Chart Data/);
  assert.match(chart, /section=Options/);
  assert.match(chart, /Previous trade/);
  const propMc = renderResultsPanel(topology(), parsed, {
    task: 1,
    databank: "Results",
    archive: "Example.sqx",
    resultView: "prop-mc",
  }, strategy);
  assert.match(propMc, /data-results-plugin="Prop Monte Carlo"/);
  assert.match(propMc, /\/api\/sqx-results-plugin\/Prop%20Monte%20Carlo\/index.html/);
  const direction = renderResultsPanel(topology(), parsed, {
    task: 1,
    databank: "Results",
    archive: "Example.sqx",
    resultView: "overview",
  }, strategy);
  assert.match(direction, /data-results-direction/);
  assert.doesNotMatch(direction, /Direction <select disabled/);
});

test("results chart fetch uses the native loadChartData read model", async () => {
  const fetched = await fetchResultsChart("Example", "Results", "Native.sqx", {}, async (url) => {
    assert.match(url, /\/api\/sqx-results-chart\?/);
    assert.match(url, /project=Example/);
    assert.match(url, /archive=Native.sqx/);
    return {
      ok: true,
      json: async () => ({
        schema: "tc.sqx-results-chart.v1",
        stored: false,
        indicators: [],
        bars: { state: "unavailable", bars: [] },
      }),
    };
  });
  assert.equal(fetched.schema, "tc.sqx-results-chart.v1");
  assert.equal(fetched.stored, false);
});

test("strategy inspect can request a sidecar window around one ticket", async () => {
  let url = "";
  await fetchProjectStrategy("Example Workflow", "Results", "Example.sqx", 1, async (href) => {
    url = href;
    return { ok: false, json: async () => ({ detail: "skip" }) };
  }, { focusTicket: 92229 }).catch(() => {});
  assert.match(url, /focusTicket=92229/);
});
