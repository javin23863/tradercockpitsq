import { actionButton, escapeHtml, unavailable } from "./ui.mjs";
import {
  firstChild,
  findNodesByTag,
  humanizeNativeName,
  renderAttributeControl,
  renderConditionTable,
  renderCrossChecksPane,
  renderFieldGroup,
  renderNodeAttributes,
  renderRankingsPane,
  renderSettingsNode,
  renderTextControl,
  workflowHref,
} from "./automation-settings-controls.mjs";

export {
  humanizeNativeName,
  renderAttributeControl,
  renderConditionTable,
  renderCrossChecksPane,
  renderRankingsPane,
  renderSettingsNode,
  renderTextControl,
  workflowHref,
};

const DOCUMENTED_LABELS = Object.freeze({
  WhatToBuild: "What to build",
  Data: "Data",
  Options: "Trading options",
  Blocks: "Building blocks",
  ATMs: "ATM",
  RiskMoneyManagement: "Money management",
  Rankings: "Ranking",
  CrossChecks: "Cross checks",
  PartsToImprove: "Parts to improve",
  GeneticOptions: "Genetic options",
  Notes: "Notes",
  Databanks: "Databanks",
  Resources: "Resources",
});

const BLOCK_PANELS = Object.freeze([
  ["signals", "Signals"],
  ["indicators", "Indicators"],
  ["stopLimitBlocks", "Stop-Limit"],
]);

const TRADING_OPTION_GROUPS = Object.freeze([
  ["Weekends", ["DontTradeOnWeekends", "FridayCloseTime", "SundayOpenTime"]],
  ["End of day / Friday", ["ExitAtEndOfDay", "EODExitTime", "ExitOnFriday", "FridayExitTime"]],
  ["Time range", ["LimitTimeRange", "SignalTimeRangeFrom", "SignalTimeRangeTo", "ExitAtEndOfRange", "OrderTypeToExit"]],
  ["Max trades", ["MaxTradesPerDay"]],
  ["Store chart data", ["StoreChartData"]],
]);

export function documentedSettingsLabel(tag) {
  return DOCUMENTED_LABELS[tag] || humanizeNativeName(tag);
}

export function isGeneticGeneration(value) {
  return typeof value === "string" && /genetic/i.test(value);
}

export function isImproveExisting(task) {
  const what = (task?.settings || []).find((node) => node.tag === "WhatToBuild");
  const strategy = firstChild(what, "StrategyType");
  const type = strategy?.attributes?.type || "";
  return type === "improve" || type === "improve-existing";
}

export function geneticBuildMode(task) {
  const what = (task?.settings || []).find((node) => node.tag === "WhatToBuild");
  const mode = firstChild(what, "BuildMode") || findNodesByTag(task?.settings || [], "BuildMode")[0] || null;
  if (!mode || !isGeneticGeneration(mode.attributes?.generationType || "")) return null;
  return mode;
}

export function documentedSettingsTabs(task) {
  const tabs = [];
  for (const node of task?.settings || []) {
    if (node.tag === "PartsToImprove" && !isImproveExisting(task)) continue;
    tabs.push({ id: node.tag, label: documentedSettingsLabel(node.tag), node, kind: "section" });
    if (node.tag === "WhatToBuild") {
      const mode = geneticBuildMode(task);
      if (mode) {
        tabs.push({ id: "GeneticOptions", label: documentedSettingsLabel("GeneticOptions"), node: mode, kind: "genetic" });
      }
    }
  }
  return tabs;
}

function blockPathKey(node) {
  return (node?.path || []).join("/");
}

function renderParam(node) {
  const key = node.attributes?.key || node.tag;
  const label = humanizeNativeName(key);
  if (node.text != null && node.text !== "") {
    return renderTextControl(node.path, node.text, label);
  }
  return `<div class="settings-node" data-settings-tag="${escapeHtml(node.tag)}">${renderNodeAttributes(node)}${(node.children || []).map((child) => renderSettingsNode(child, { heading: true })).join("")}</div>`;
}

function paramsOf(node) {
  const params = firstChild(node, "Params");
  if (params) return (params.children || []).filter((child) => child.tag === "Param");
  return (node?.children || []).filter((child) => child.tag === "Param" || child.tag === "Parameter");
}

export function renderWhatToBuildPane(node) {
  const strategy = firstChild(node, "StrategyType");
  const sides = firstChild(node, "MarketSides");
  const mode = firstChild(node, "BuildMode");
  const rules = firstChild(node, "RulesComplexity");
  const slpt = firstChild(node, "SLPTOptions");
  const shown = new Set(["StrategyType", "MarketSides", "BuildMode", "RulesComplexity", "SLPTOptions"]);
  const rest = (node.children || []).filter((child) => !shown.has(child.tag));
  const slChildren = (slpt?.children || []).filter((child) => /^SL|LimitSL|Separated/.test(child.tag));
  const ptChildren = (slpt?.children || []).filter((child) => /^PT|LimitPT/.test(child.tag) && !slChildren.includes(child));
  const otherSlpt = (slpt?.children || []).filter((child) => !slChildren.includes(child) && !ptChildren.includes(child));
  return `<div class="settings-node" data-settings-tag="WhatToBuild">
    ${renderFieldGroup("Strategy type", strategy ? `<div class="settings-node" data-settings-tag="StrategyType">${renderNodeAttributes(strategy)}${strategy.text ? renderTextControl(strategy.path, strategy.text, "Strategy type") : ""}</div>` : "")}
    ${renderFieldGroup("Trading direction / symmetry", sides ? renderSettingsNode(sides, { heading: false }) : "")}
    ${renderFieldGroup("Build mode", mode ? `<div class="settings-node" data-settings-tag="BuildMode">${renderNodeAttributes(mode)}</div>` : "")}
    ${renderFieldGroup("Condition / shift / period ranges", rules ? renderSettingsNode(rules, { heading: false }) : "")}
    ${renderFieldGroup("Stop loss", slChildren.map((child) => renderSettingsNode(child, { heading: false })).join(""))}
    ${renderFieldGroup("Profit target", ptChildren.map((child) => renderSettingsNode(child, { heading: false })).join(""))}
    ${otherSlpt.length ? renderFieldGroup("SL / PT", otherSlpt.map((child) => renderSettingsNode(child, { heading: false })).join("")) : ""}
    ${rest.map((child) => renderSettingsNode(child, { heading: true })).join("")}
  </div>`;
}

export function renderGeneticOptionsPane(node) {
  const fields = renderNodeAttributes(node, ["generationType"]);
  const body = (node.children || []).map((child) => (
    child.tag === "Conditions"
      ? renderFieldGroup("Genetic conditions", renderConditionTable(child))
      : renderSettingsNode(child, { heading: true })
  )).join("");
  return `<div class="settings-node" data-settings-tag="BuildMode" data-genetic-options="1">${fields}${body || `<p class="field-help">This saved BuildMode has no nested Genetic options fields.</p>`}</div>`;
}

export function renderPartsToImprovePane(node) {
  const groups = [
    ["Entry rules", firstChild(node, "EntryRules")],
    ["Order types", firstChild(node, "OrderTypes")],
    ["Exit rules", firstChild(node, "ExitRules")],
  ];
  const shown = new Set(["EntryRules", "OrderTypes", "ExitRules"]);
  const rest = (node.children || []).filter((child) => !shown.has(child.tag));
  return `<div class="settings-node" data-settings-tag="PartsToImprove">
    ${renderNodeAttributes(node)}
    ${node.text ? renderTextControl(node.path, node.text, "Parts to improve") : ""}
    ${groups.map(([title, child]) => renderFieldGroup(title, child ? renderSettingsNode(child, { heading: false }) : "")).join("")}
    ${rest.map((child) => renderSettingsNode(child, { heading: true })).join("")}
  </div>`;
}

export function renderDataPane(node) {
  const setups = firstChild(node, "Setups");
  const setupNodes = (setups?.children || []).filter((child) => child.tag === "Setup");
  const oos = firstChild(node, "OutOfSample");
  const shown = new Set(["Setups", "OutOfSample"]);
  const rest = (node.children || []).filter((child) => !shown.has(child.tag));
  const setupHtml = setupNodes.map((setup) => {
    const chart = firstChild(setup, "Chart");
    const commissions = firstChild(setup, "Commissions");
    const swap = firstChild(setup, "Swap");
    const extra = (setup.children || []).filter((child) => !["Chart", "Commissions", "Swap"].includes(child.tag));
    return `<div class="settings-node" data-settings-tag="Setup">
      ${renderFieldGroup("Engine / dates / precision", renderNodeAttributes(setup))}
      ${renderFieldGroup("Chart", chart ? renderNodeAttributes(chart) : "")}
      ${renderFieldGroup("Commissions", commissions ? renderSettingsNode(commissions, { heading: false }) : "")}
      ${renderFieldGroup("Swap", swap ? renderSettingsNode(swap, { heading: false }) : "")}
      ${extra.map((child) => renderSettingsNode(child, { heading: true })).join("")}
    </div>`;
  }).join("");
  return `<div class="settings-node" data-settings-tag="Data">
    ${setupHtml || `<p class="field-help">This saved task has no Data Setup rows.</p>`}
    ${renderFieldGroup("Data parts / out of sample", oos ? renderSettingsNode(oos, { heading: false }) : "")}
    ${rest.map((child) => renderSettingsNode(child, { heading: true })).join("")}
  </div>`;
}

export function renderTradingOptionsPane(node) {
  const paramsRoot = findNodesByTag([node], "Params")[0];
  const params = (paramsRoot?.children || []).filter((child) => child.tag === "Param");
  const used = new Set();
  const groups = TRADING_OPTION_GROUPS.map(([title, keys]) => {
    const rows = params.filter((param) => keys.includes(param.attributes?.key));
    rows.forEach((param) => used.add(param));
    return renderFieldGroup(title, rows.map(renderParam).join(""));
  }).join("");
  const rest = params.filter((param) => !used.has(param));
  const extra = (node.children || []).filter((child) => child.tag !== "BuildTradingOptions");
  return `<div class="settings-node" data-settings-tag="Options">
    ${renderNodeAttributes(node)}
    ${groups}
    ${renderFieldGroup("Other existing options", rest.map(renderParam).join(""))}
    ${extra.map((child) => renderSettingsNode(child, { heading: true })).join("")}
  </div>`;
}

function renderBlockRow(node, { project, taskIndex }) {
  const key = node.attributes?.key || node.tag;
  const use = node.attributes?.use;
  const weight = node.attributes?.weight;
  const probability = node.attributes?.probability;
  const nested = (node.children || []).length > 0;
  const href = workflowHref({
    project,
    tab: "settings",
    task: taskIndex,
    section: "Blocks",
    block: blockPathKey(node),
  });
  return `<div class="settings-block-row" data-settings-tag="Block" data-block-key="${escapeHtml(key)}">
    <div class="settings-block-main">
      ${use === "true" || use === "false" ? renderAttributeControl(node.path, "use", use) : ""}
      <strong>${escapeHtml(key)}</strong>
    </div>
    <div class="settings-block-tools">
      ${weight != null ? renderAttributeControl(node.path, "weight", weight) : ""}
      ${probability != null ? renderAttributeControl(node.path, "probability", probability) : ""}
      ${nested ? `<a class="button-small" href="${escapeHtml(href)}" data-route="${escapeHtml(href)}" data-automation-block="${escapeHtml(blockPathKey(node))}">Parameters</a>` : ""}
    </div>
  </div>`;
}

function findBlockByPath(node, pathKey) {
  if (blockPathKey(node) === pathKey) return node;
  for (const child of node.children || []) {
    const found = findBlockByPath(child, pathKey);
    if (found) return found;
  }
  return null;
}

export function renderBuildingBlocksPane(node, { project = "", taskIndex = "", block = "" } = {}) {
  if (block) {
    const selected = findBlockByPath(node, block);
    if (selected) {
      const backHref = workflowHref({ project, tab: "settings", task: taskIndex, section: "Blocks" });
      const attrs = Object.entries(selected.attributes || {})
        .filter(([name]) => !["use", "weight", "probability", "key", "category"].includes(name));
      return `<div class="settings-node" data-settings-tag="Block" data-block-key="${escapeHtml(selected.attributes?.key || "")}">
        <p class="workflow-crumb"><a class="workflow-link" href="${escapeHtml(backHref)}" data-route="${escapeHtml(backHref)}" data-automation-section="Blocks">Building blocks</a><span>/</span><strong>${escapeHtml(selected.attributes?.key || selected.tag)}</strong></p>
        ${selected.attributes?.use != null ? renderAttributeControl(selected.path, "use", selected.attributes.use) : ""}
        ${selected.attributes?.weight != null ? renderAttributeControl(selected.path, "weight", selected.attributes.weight) : ""}
        ${selected.attributes?.probability != null ? renderAttributeControl(selected.path, "probability", selected.attributes.probability) : ""}
        ${attrs.map(([attribute, value]) => renderAttributeControl(selected.path, attribute, value)).join("")}
        ${(selected.children || []).map((child) => renderSettingsNode(child, { heading: true })).join("")}
      </div>`;
    }
  }
  const calibration = firstChild(node, "Calibration");
  const building = firstChild(node, "BuildingBlocks");
  const orderTypes = firstChild(node, "OrderTypes");
  const exitTypes = firstChild(node, "ExitTypes");
  const custom = firstChild(node, "CustomData");
  const blocks = (building?.children || []).filter((child) => child.tag === "Block");
  const options = { project, taskIndex };
  const known = new Set(BLOCK_PANELS.map(([id]) => id));
  const extraCategories = [...new Set(blocks.map((item) => item.attributes?.category || "").filter((item) => item && !known.has(item)))];
  const panels = [
    ...BLOCK_PANELS.map(([id, title]) => [title, blocks.filter((item) => item.attributes?.category === id)]),
    ...extraCategories.map((id) => [humanizeNativeName(id), blocks.filter((item) => item.attributes?.category === id)]),
  ];
  return `<div class="settings-node" data-settings-tag="Blocks">
    ${renderNodeAttributes(node)}
    ${renderFieldGroup("Indicators calibration", calibration ? renderSettingsNode(calibration, { heading: false }) : "")}
    <div class="settings-block-panels">
      ${panels.map(([title, rows]) => `<section class="settings-group" data-settings-group="${escapeHtml(title)}"><h4>${escapeHtml(title)}</h4>${rows.length ? rows.map((row) => renderBlockRow(row, options)).join("") : `<p class="field-help">No ${escapeHtml(title.toLowerCase())} blocks in this saved XML.</p>`}</section>`).join("")}
    </div>
    ${renderFieldGroup("Order types", (orderTypes?.children || []).filter((child) => child.tag === "Block").map((row) => renderBlockRow(row, options)).join(""))}
    ${renderFieldGroup("Exit types", (exitTypes?.children || []).filter((child) => child.tag === "Block").map((row) => renderBlockRow(row, options)).join(""))}
    ${renderFieldGroup("Custom data", custom ? renderSettingsNode(custom, { heading: false }) : "")}
  </div>`;
}

export function renderMoneyManagementPane(node) {
  const mm = firstChild(node, "MoneyManagement") || node;
  const risk = firstChild(node, "RiskManagement");
  const methods = (mm.children || []).filter((child) => child.tag === "Method");
  const capital = firstChild(mm, "InitialCapital");
  const extra = (mm.children || []).filter((child) => child.tag !== "Method" && child.tag !== "InitialCapital");
  const used = methods.find((method) => method.attributes?.use === "true") || methods[0] || null;
  return `<div class="settings-node" data-settings-tag="${escapeHtml(node.tag)}">
    ${renderNodeAttributes(node)}
    ${renderFieldGroup("Type", methods.map((method) => {
      const selected = method === used;
      return `<div class="settings-mm-method ${selected ? "is-selected" : ""}" data-settings-tag="Method">
        ${renderNodeAttributes(method)}
        ${selected ? paramsOf(method).map(renderParam).join("") : ""}
        ${selected ? (method.children || []).filter((child) => child.tag !== "Params" && child.tag !== "Param" && child.tag !== "Parameter").map((child) => renderSettingsNode(child, { heading: true })).join("") : ""}
      </div>`;
    }).join(""))}
    ${renderFieldGroup("Size / capital", capital ? renderTextControl(capital.path, capital.text || "", "Initial capital") : "")}
    ${extra.map((child) => renderSettingsNode(child, { heading: true })).join("")}
    ${renderFieldGroup("Risk management", risk ? renderSettingsNode(risk, { heading: false }) : "")}
  </div>`;
}

export function renderAtmPane(node) {
  const atm = firstChild(node, "ATM");
  const exits = firstChild(atm, "Exits");
  const generate = firstChild(node, "GenerateConfig");
  const extra = (node.children || []).filter((child) => child.tag !== "ATM" && child.tag !== "GenerateConfig");
  const exitRows = exits?.children || [];
  return `<div class="settings-node" data-settings-tag="ATMs">
    ${renderFieldGroup("Enable / size constraints", renderNodeAttributes(node))}
    ${atm ? renderNodeAttributes(atm) : ""}
    ${renderFieldGroup("Exits", exitRows.length ? exitRows.map((child) => renderSettingsNode(child, { heading: true })).join("") : `<p class="field-help">This saved ATM has no exit rows. This desktop does not add ATM exits.</p>`)}
    ${renderFieldGroup("Generate config", generate ? renderSettingsNode(generate, { heading: false }) : "")}
    ${extra.map((child) => renderSettingsNode(child, { heading: true })).join("")}
  </div>`;
}

export function renderDatabanksPane(node) {
  const rows = (node.children || []).filter((child) => child.tag === "Databank");
  return `<div class="settings-node" data-settings-tag="Databanks">
    ${renderNodeAttributes(node)}
    ${rows.map((row) => `<div class="settings-databank-row" data-settings-tag="Databank">${renderNodeAttributes(row)}</div>`).join("")}
  </div>`;
}

export function renderDocumentedPane(tab, options = {}) {
  const node = tab?.node;
  if (!node) return "";
  if (tab.kind === "genetic" || tab.id === "GeneticOptions") return renderGeneticOptionsPane(node);
  switch (node.tag) {
    case "WhatToBuild":
      return renderWhatToBuildPane(node);
    case "PartsToImprove":
      return renderPartsToImprovePane(node);
    case "Data":
      return renderDataPane(node);
    case "Options":
      return renderTradingOptionsPane(node);
    case "Blocks":
      return renderBuildingBlocksPane(node, options);
    case "RiskMoneyManagement":
    case "MoneyManagement":
      return renderMoneyManagementPane(node);
    case "ATMs":
      return renderAtmPane(node);
    case "Rankings":
      return renderRankingsPane(node, options);
    case "CrossChecks":
      return renderCrossChecksPane(node, options);
    case "Databanks":
      return renderDatabanksPane(node);
    default:
      return renderSettingsNode(node, { ...options, heading: false });
  }
}

export function renderFullSettings(task, sectionTag = "", project = "", method = "", methodPane = "", block = "") {
  const tabs = documentedSettingsTabs(task);
  if (!tabs.length) {
    return unavailable(
      "This task has no Full settings panes",
      "Full settings tabs are the Settings children in this task XML. This desktop does not invent Data, Ranking, or Building blocks panes.",
      { compact: true },
    );
  }
  const current = tabs.find((tab) => tab.id === sectionTag) || tabs[0];
  const tablist = `<div class="settings-section-tabs" role="tablist">${tabs.map((tab) => {
    const currentTab = tab.id === current.id;
    const href = workflowHref({
      project,
      tab: "settings",
      task: task.native_task_index,
      section: tab.id,
    });
    return `<a class="workflow-tab ${currentTab ? "is-current" : ""}" role="tab" aria-selected="${currentTab}" href="${escapeHtml(href)}" data-route="${escapeHtml(href)}" data-automation-section="${escapeHtml(tab.id)}">${escapeHtml(tab.label)}</a>`;
  }).join("")}</div>`;
  const body = renderDocumentedPane(current, {
    project,
    taskIndex: task.native_task_index,
    method,
    methodPane,
    block,
  });
  return `<form class="full-settings" data-automation-settings-form data-settings-task="${task.native_task_index}">
    ${tablist}
    ${body}
    <div class="idea-actions">
      ${actionButton("Save settings", { primary: true, attrs: `data-automation-save-settings data-project-task="${task.native_task_index}"` })}
    </div>
    <p class="idea-save-status" data-automation-settings-status></p>
    <p class="field-help">Only attributes or existing text on this native element can be written. This desktop does not invent SQX parameters, Condition rows, or What-If scenarios.</p>
  </form>`;
}
