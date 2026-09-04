import { actionButton, chartFrame, escapeHtml, unavailable } from "./ui.mjs";
import {
  firstChild,
  findNodesByTag,
  humanizeNativeName,
  choiceLabel,
  nativeChoicesFor,
  nodeSettingSummary,
  renderAttributeControl,
  renderConditionTable,
  renderConfigCard,
  renderConfigRow,
  renderCrossChecksPane,
  renderExclusiveUseChoices,
  renderFieldGroup,
  renderNodeAttributes,
  renderRankingsPane,
  renderSettingsNode,
  renderSqxDataBox,
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
  MoneyManagement: "Money management",
  Rankings: "Ranking",
  CrossChecks: "Cross checks (robustness)",
  PartsToImprove: "Parts to improve",
  GeneticOptions: "Genetic options",
  Notes: "Notes",
  Databanks: "Databanks",
  Resources: "Resources",
});

// SQX Advanced settings tab strip — not XML child order.
const SQX_SETTINGS_TAB_ORDER = Object.freeze([
  "WhatToBuild",
  "PartsToImprove",
  "GeneticOptions",
  "Data",
  "Options",
  "Blocks",
  "ATMs",
  "RiskMoneyManagement",
  "MoneyManagement",
  "CrossChecks",
  "Rankings",
  "Notes",
  "Databanks",
  "Resources",
]);

const BLOCK_PANELS = Object.freeze([
  ["signals", "Signals"],
  ["indicators", "Indicators"],
  ["stopLimitBlocks", "Stop-Limit"],
]);

const BLOCK_PANEL_TITLES = Object.freeze({
  signals: "Signals",
  indicators: "Indicators",
  stopLimitBlocks: "Stop/Limit entry blocks",
});

function blockPanelTitle(id, group) {
  return BLOCK_PANEL_TITLES[id] || group;
}

const TRADING_OPTION_GROUPS = Object.freeze([
  ["Weekends", ["DontTradeOnWeekends", "FridayCloseTime", "SundayOpenTime"]],
  ["End of day / Friday", ["ExitAtEndOfDay", "EODExitTime", "ExitOnFriday", "FridayExitTime"]],
  ["Time range", ["LimitTimeRange", "SignalTimeRangeFrom", "SignalTimeRangeTo", "ExitAtEndOfRange", "OrderTypeToExit"]],
  ["Max trades", ["MaxTradesPerDay"]],
  ["Max distance from market", ["MaxDistanceFromMarket", "MaxDistancePct"]],
  ["Min / max SL and PT", ["MinimumSL", "MaximumSL", "MinimumPT", "MaximumPT", "UseInitialSLPT"]],
  ["Session / bars", ["Session", "ReservedBars", "MarketOpenSession"]],
  ["Store chart data", ["StoreChartData"]],
  ["Realistic gaps", ["RealisticGapsHandling"]],
  ["Stockpicker", [
    "PickerEntryType", "PickerExitType", "PickerEndOfDayLong", "PickerEndOfDayShort",
    "PickerMaxOpenPositionsLong", "PickerMaxOpenPositionsShort", "PickerStoreLogs",
    "PickerAllowBetterLimitFill", "PickerBroker",
  ]],
  ["Broker / limit", ["LimitOver"]],
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
  tabs.sort((left, right) => {
    const a = SQX_SETTINGS_TAB_ORDER.indexOf(left.id);
    const b = SQX_SETTINGS_TAB_ORDER.indexOf(right.id);
    return (a === -1 ? 999 : a) - (b === -1 ? 999 : b);
  });
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

const GENETIC_CARD_BUCKETS = Object.freeze([
  ["Genetic options", new Set(["PopulationSize", "MaxGenerations", "InitGenerationType", "DecimationCoef", "CrossoverProbability", "MutationProbability", "EvoInSamplePeriod", "ShowAdvancedGeneticSettings"])],
  ["Islands options", new Set(["Islands", "MigrationModulo", "MigrationRate"])],
  ["Initial population generation", new Set(["InitialPopulation", "InitPopulationType"])],
  ["Filter generated initial population", new Set(["Conditions", "FilterConditions"])],
  ["\"Fresh blood\"", new Set(["FreshBloodReplaceSimilar", "FreshBloodReplaceWeakest", "FreshBloodWeakestPct", "FreshBloodWeakestGenerations", "ShowLastGenerationDatabank"])],
  ["Evolution management", new Set(["EvoRestartOnFinish", "EvoRestartOnStagnation"])],
]);

function bucketGeneticChildren(children) {
  const buckets = Object.fromEntries(GENETIC_CARD_BUCKETS.map(([title]) => [title, []]));
  const other = [];
  for (const child of children || []) {
    let placed = false;
    for (const [title, tags] of GENETIC_CARD_BUCKETS) {
      if (tags.has(child.tag)) {
        buckets[title].push(child);
        placed = true;
        break;
      }
    }
    if (!placed) other.push(child);
  }
  return { buckets, other };
}

function renderGeneticChild(child) {
  if (child.tag === "Conditions") return renderConditionTable(child);
  return renderSettingsNode(child, { heading: false });
}

function renderTradingSymmetryChildren(children) {
  return (children || [])
    .filter((child) => child.tag === "EntrySymmetry" || child.tag === "ExitSymmetry")
    .map((child) => renderSettingsNode(child, { heading: false }))
    .join("");
}

function renderOosRanges(node) {
  if (!node) return "";
  const ranges = (node.children || []).filter((child) => child.tag === "Range");
  const attrs = renderNodeAttributes(node);
  const rows = ranges.map((range) => (
    `<div class="settings-oos-range" data-settings-tag="Range">${renderNodeAttributes(range)}</div>`
  )).join("");
  const rest = (node.children || [])
    .filter((child) => child.tag !== "Range")
    .map((child) => renderSettingsNode(child, { heading: false }))
    .join("");
  return `${attrs}${rows}${rest}`;
}

function renderOosGraph(dataNode, oos) {
  const setup = firstChild(firstChild(dataNode, "Setups"), "Setup");
  const chart = firstChild(setup, "Chart");
  const show = oos?.attributes?.showGraph === "true";
  const symbol = chart?.attributes?.symbol || "";
  const basket = symbol.startsWith("[");
  return `<div class="sqx-oos-graph" data-sqx-oos-graph data-show-graph="${show ? "true" : "false"}" data-symbol="${escapeHtml(symbol)}" data-session="${escapeHtml(setup?.attributes?.session || "No Session")}" data-date-from="${escapeHtml(setup?.attributes?.dateFrom || "")}" data-date-to="${escapeHtml(setup?.attributes?.dateTo || "")}">${
    !show
      ? `<p class="field-help">Show chart</p>`
      : basket
        ? unavailable("OOS graph", "Basket aliases have no data/getSymbolData series.", { compact: true })
        : unavailable("Loading OOS graph…", "Calling StrategyQuant X data/getSymbolData.", { compact: true, tone: "pending" })
  }</div>`;
}

function renderDatabankRows(rows) {
  return rows.map((row) => (
    `<div class="settings-databank-row" data-settings-tag="Databank">${renderNodeAttributes(row)}</div>`
  )).join("");
}

function whatConfigSection(group, body) {
  if (!body) return "";
  return `<section class="settings-group sqx-what-config-row" data-settings-group="${escapeHtml(group)}">${body}</section>`;
}

const STRATEGY_TYPE_HELP = Object.freeze({
  simple: "Simple strategy running on one symbol and timeframe",
  "multi-tf": "Strategy looking at main chart and additional charts. These can be just another timeframes of original chart or totally different symbols/TFs. It trades only on the main chart.",
  template: "Strategy created from the template",
  improve: "Improve some parts of existing strategy / strategies",
  "improve-existing": "Improve some parts of existing strategy / strategies",
});

const STRATEGY_TYPE_OWNED_ATTRS = Object.freeze([
  "type",
  "additionalCharts",
  "templateFile",
  "improveType",
  "strategyFile",
  "improveDatabank",
]);

function hasOwnAttr(node, name) {
  return Boolean(node?.attributes) && Object.prototype.hasOwnProperty.call(node.attributes, name);
}

function isImproveStrategyType(type) {
  return type === "improve" || type === "improve-existing";
}

function strategyTypeRadioName(path) {
  return `sqx-${JSON.stringify(path)}-type`.replace(/[^A-Za-z0-9_-]/g, "_");
}

function renderStrategyTypeExtra(strategy, boxType) {
  const selected = strategy.attributes?.type || "";
  const extras = [];
  if (boxType === "multi-tf" && hasOwnAttr(strategy, "additionalCharts")) {
    extras.push(renderAttributeControl(strategy.path, "additionalCharts", strategy.attributes.additionalCharts, { tag: "StrategyType" }));
  }
  if (boxType === "template" && hasOwnAttr(strategy, "templateFile")) {
    extras.push(renderAttributeControl(strategy.path, "templateFile", strategy.attributes.templateFile, { tag: "StrategyType" }));
    extras.push(`<div class="sqx-file-actions">
      <button type="button" class="button button-secondary" data-settings-browse-files="templates"><span>Browse</span></button>
      <button type="button" class="button button-secondary" data-settings-reload-template><span>Reload</span></button>
      <p class="idea-save-status" data-settings-template-status></p>
      <dialog class="sqx-settings-dialog" data-settings-file-browse="templates">
        <p class="sqx-advanced-head">Official StrategyQuant X templates</p>
        <div class="sqx-file-browse-list" data-settings-file-browse-list></div>
        <div class="sqx-settings-dialog-actions">
          <button type="button" class="button button-secondary" data-settings-dialog-close><span>Close</span></button>
        </div>
      </dialog>
    </div>`);
  }
  if (isImproveStrategyType(boxType) && boxType === selected) {
    if (hasOwnAttr(strategy, "improveType")) {
      extras.push(renderAttributeControl(strategy.path, "improveType", strategy.attributes.improveType, { tag: "StrategyType" }));
    }
    if (hasOwnAttr(strategy, "strategyFile")) {
      extras.push(renderAttributeControl(strategy.path, "strategyFile", strategy.attributes.strategyFile, { tag: "StrategyType" }));
      extras.push(`<div class="sqx-file-actions">
        <button type="button" class="button button-secondary" data-settings-browse-files="strategies"><span>Browse</span></button>
        <dialog class="sqx-settings-dialog" data-settings-file-browse="strategies">
          <p class="sqx-advanced-head">Official StrategyQuant X strategies</p>
          <div class="sqx-file-browse-list" data-settings-file-browse-list></div>
          <div class="sqx-settings-dialog-actions">
            <button type="button" class="button button-secondary" data-settings-dialog-close><span>Close</span></button>
          </div>
        </dialog>
      </div>`);
    }
    if (hasOwnAttr(strategy, "improveDatabank")) {
      extras.push(renderAttributeControl(strategy.path, "improveDatabank", strategy.attributes.improveDatabank, { tag: "StrategyType" }));
    }
  }
  return extras.length ? `<div class="sqx-what-type-extra">${extras.join("")}</div>` : "";
}

function renderStrategyTypeBoxes(strategy) {
  const selected = strategy.attributes?.type || "";
  const choices = nativeChoicesFor("type", selected, { tag: "StrategyType", path: strategy.path }) || [];
  const encodedPath = escapeHtml(JSON.stringify(strategy.path));
  const group = escapeHtml(strategyTypeRadioName(strategy.path));
  const options = choices.map(([choice, label]) => {
    const help = STRATEGY_TYPE_HELP[choice] || "";
    return `<div class="settings-radio sqx-what-type" data-strategy-type="${escapeHtml(choice)}">
      <label class="sqx-what-type-pick">
        <input type="radio" name="${group}" value="${escapeHtml(choice)}" ${choice === selected ? "checked" : ""} data-settings-path="${encodedPath}" data-settings-attribute="type" data-settings-kind="choice">
        <span class="sqx-what-type-body">
          <span class="sqx-what-type-title">${escapeHtml(label)}</span>
          ${help ? `<p class="field-help sqx-what-type-help">${escapeHtml(help)}</p>` : ""}
        </span>
      </label>
      ${renderStrategyTypeExtra(strategy, choice)}
    </div>`;
  }).join("");
  return `<fieldset class="settings-radio-group"><legend>Type</legend>${options}</fieldset>`;
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
  const slRanges = otherSlpt.filter((child) => /SL/i.test(child.tag));
  const ptRanges = otherSlpt.filter((child) => /PT/i.test(child.tag));
  const neutralSlpt = otherSlpt.filter((child) => !slRanges.includes(child) && !ptRanges.includes(child));
  const genetic = isGeneticGeneration(mode?.attributes?.generationType || "");
  const leftoverMode = mode && !genetic
    ? (mode.children || []).map((child) => renderSettingsNode(child, { heading: true })).join("")
    : "";
  const strategyLeftover = strategy
    ? `${renderNodeAttributes(strategy, STRATEGY_TYPE_OWNED_ATTRS)}${strategy.text ? renderTextControl(strategy.path, strategy.text, "Strategy type") : ""}`
    : "";
  const symmetryChildren = (sides?.children || []).filter((child) => child.tag === "EntrySymmetry" || child.tag === "ExitSymmetry");
  const otherSideChildren = (sides?.children || []).filter((child) => child.tag !== "EntrySymmetry" && child.tag !== "ExitSymmetry");
  const symmetryBody = renderTradingSymmetryChildren(sides?.children || []);
  const modeRadios = mode
    ? renderAttributeControl(mode.path, "generationType", mode.attributes?.generationType || "", { tag: "BuildMode" })
    : "";
  const modeExtra = mode ? renderNodeAttributes(mode, ["generationType"]) : "";
  const modeDialog = `${modeRadios}${leftoverMode}${modeExtra || (genetic
    ? `<p class="field-help">Population, islands, and evolution fields are on Genetic options.</p>`
    : leftoverMode || `<p class="field-help">This saved BuildMode has no extra attributes.</p>`)}`;
  const directionBody = sides
    ? `${renderAttributeControl(sides.path, "type", sides.attributes?.type || "", { tag: "MarketSides" })}${symmetryBody}${otherSideChildren.map((child) => renderSettingsNode(child, { heading: false })).join("")}`
    : "";
  const directionSummary = [
    choiceLabel("type", sides?.attributes?.type || "", { tag: "MarketSides", path: sides?.path }),
    nodeSettingSummary({ children: symmetryChildren }),
  ].filter(Boolean).join(", ");
  const configRows = [
    sides ? whatConfigSection(
      "Trading direction / symmetry",
      `<div class="settings-node" data-settings-tag="MarketSides">${renderConfigRow("Trading directions", directionSummary, directionBody, "what-trading-symmetry")}</div>`,
    ) : "",
    strategyLeftover ? whatConfigSection(
      "Strategy style",
      renderConfigRow("Strategy style", choiceLabel("architecture", strategy.attributes?.architecture || "", { tag: "StrategyType", path: strategy.path }), strategyLeftover, "what-strategy-style"),
    ) : "",
    mode ? whatConfigSection(
      "Build mode",
      `<div class="settings-node" data-settings-tag="BuildMode">${renderConfigRow("Build mode", choiceLabel("generationType", mode.attributes?.generationType, { tag: "BuildMode", path: mode.path }), modeDialog, "what-build-mode")}</div>`,
    ) : "",
    rules ? renderConfigCard("Condition / shift / period ranges", nodeSettingSummary(rules), renderSettingsNode(rules, { heading: false }), "what-rules-complexity") : "",
    slpt && (slChildren.length || slRanges.length)
      ? renderConfigCard(
        "Stop loss",
        nodeSettingSummary({ ...slpt, children: [...slChildren, ...slRanges] }),
        [...slChildren, ...slRanges].map((child) => renderSettingsNode(child, { heading: false })).join(""),
        "what-stop-loss",
      )
      : "",
    slpt && (ptChildren.length || ptRanges.length)
      ? renderConfigCard(
        "Profit target",
        nodeSettingSummary({ ...slpt, children: [...ptChildren, ...ptRanges] }),
        [...ptChildren, ...ptRanges].map((child) => renderSettingsNode(child, { heading: false })).join(""),
        "what-profit-target",
      )
      : "",
    neutralSlpt.length ? renderFieldGroup("SL / PT", neutralSlpt.map((child) => renderSettingsNode(child, { heading: false })).join("")) : "",
    rest.map((child) => renderSettingsNode(child, { heading: true })).join(""),
  ].join("");
  return `<div class="settings-node sqx-settings-stack sqx-what-build" data-settings-tag="WhatToBuild">
    ${strategy ? `<section class="settings-group sqx-settings-card sqx-what-types" data-settings-group="Strategy type">
      <h4>Strategy type</h4>
      <div class="settings-node" data-settings-tag="StrategyType">
        ${renderStrategyTypeBoxes(strategy)}
      </div>
    </section>` : ""}
    ${configRows ? `<section class="settings-group sqx-settings-card sqx-what-config"><h4>Additional build config</h4><div class="sqx-what-table">${configRows}</div></section>` : ""}
  </div>`;
}

export function renderGeneticOptionsPane(node) {
  const { buckets, other } = bucketGeneticChildren(node.children || []);
  const cards = GENETIC_CARD_BUCKETS.map(([title]) => {
    const items = buckets[title];
    if (!items.length) return "";
    return renderFieldGroup(title, items.map(renderGeneticChild).join(""));
  }).join("");
  const extra = other.length ? renderFieldGroup("Other settings", other.map(renderGeneticChild).join("")) : "";
  const fields = renderNodeAttributes(node, ["generationType"]);
  const empty = !cards && !extra;
  return `<div class="settings-node sqx-settings-grid" data-settings-tag="BuildMode" data-genetic-options="1">${fields}${cards}${extra}${empty ? `<p class="field-help">This saved BuildMode has no nested Genetic options fields.</p>` : ""}</div>`;
}

export function renderPartsToImprovePane(node) {
  const groups = [
    ["Entry rules", firstChild(node, "EntryRules")],
    ["Order types", firstChild(node, "OrderTypes")],
    ["Exit rules", firstChild(node, "ExitRules")],
    ["ATM", firstChild(node, "ATMImprovement") || firstChild(node, "ATM")],
  ];
  const shown = new Set(["EntryRules", "OrderTypes", "ExitRules", "ATMImprovement", "ATM"]);
  const rest = (node.children || []).filter((child) => !shown.has(child.tag));
  return `<div class="settings-node sqx-settings-stack" data-settings-tag="PartsToImprove">
    ${renderNodeAttributes(node) ? renderFieldGroup("Improve scope", renderNodeAttributes(node)) : ""}
    ${node.text ? renderFieldGroup("Notes", renderTextControl(node.path, node.text, "Parts to improve")) : ""}
    ${groups.map(([title, child]) => renderFieldGroup(title, child ? renderSettingsNode(child, { heading: false }) : "")).join("")}
    ${rest.map((child) => renderSettingsNode(child, { heading: true })).join("")}
  </div>`;
}

export function renderDataPane(node, taskKind = "") {
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
    const commissionBody = commissions ? renderSettingsNode(commissions, { heading: false }) : "";
    const swapBody = swap ? renderNodeAttributes(swap) : "";
    return `<div class="settings-node" data-settings-tag="Setup">
      ${renderFieldGroup("Trading engine", renderNodeAttributes(setup))}
      ${renderFieldGroup("Backtest data", chart ? `${renderSqxDataBox(chart, setup, taskKind)}${renderNodeAttributes(chart, ["symbol"])}` : "")}
      ${renderFieldGroup("Test parameters", `${commissionBody ? renderConfigRow("Commission", nodeSettingSummary(firstChild(commissions, "Method") || commissions), commissionBody, "data-commission") : ""}${swapBody ? renderConfigRow("Swap", nodeSettingSummary(swap), swapBody, "data-swap") : ""}`)}
      ${extra.map((child) => renderSettingsNode(child, { heading: true })).join("")}
    </div>`;
  }).join("");
  return `<div class="settings-node sqx-settings-stack" data-settings-tag="Data">
    ${setupHtml || `<p class="field-help">This saved task has no Data Setup rows.</p>`}
    ${renderFieldGroup("Data range / OOS", oos ? `${renderOosRanges(oos)}${renderOosGraph(node, oos)}` : "")}
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
  return `<div class="settings-node sqx-settings-stack" data-settings-tag="Options">
    ${renderNodeAttributes(node)}
    ${groups}
    ${renderFieldGroup("Other existing options", rest.map(renderParam).join(""))}
    ${extra.map((child) => renderSettingsNode(child, { heading: true })).join("")}
  </div>`;
}

function sortBlockRows(rows) {
  return rows.slice().sort((a, b) => String(a.attributes?.key || a.tag).localeCompare(String(b.attributes?.key || b.tag), undefined, { numeric: true, sensitivity: "base" }));
}

function blockFamily(key) {
  const text = String(key || "");
  if (text.includes(".")) return text.slice(0, text.indexOf("."));
  const camel = text.match(/^([A-Z]+(?=[A-Z][a-z])|[A-Z][a-z]+)/);
  return camel ? camel[1] : text;
}

function renderFlagToggle(path, attribute, value, title) {
  const encodedPath = escapeHtml(JSON.stringify(path));
  const on = value === "true";
  return `<button type="button" class="toggle ${on ? "is-on" : ""}" role="switch" aria-checked="${on}" data-settings-path="${encodedPath}" data-settings-attribute="${escapeHtml(attribute)}" data-settings-kind="flag" title="${escapeHtml(title)}"></button>`;
}

function renderUseToggle(path, value) {
  const encodedPath = escapeHtml(JSON.stringify(path));
  const on = value === "true";
  return `<button type="button" class="toggle settings-use ${on ? "is-on" : ""}" role="checkbox" aria-checked="${on}" data-settings-path="${encodedPath}" data-settings-attribute="use" data-settings-kind="flag" title="Use"></button>`;
}

function blockDisplayName(key) {
  return humanizeNativeName(String(key || "").replace(/\./g, " "));
}

function renderWeightInput(path, attribute, value) {
  const encodedPath = escapeHtml(JSON.stringify(path));
  return `<input class="workflow-input settings-block-weight" data-settings-path="${encodedPath}" data-settings-attribute="${escapeHtml(attribute)}" value="${escapeHtml(value)}" aria-label="${escapeHtml(humanizeNativeName(attribute))}">`;
}

function renderAzBar(panelId) {
  const letters = [..."ABCDEFGHIJKLMNOPQRSTUVWXYZ"].map((letter) => (
    `<button type="button" data-settings-az="${letter}">${letter}</button>`
  )).join("");
  return `<div class="settings-az-roll"><div class="settings-az" data-settings-az-panel="${escapeHtml(panelId)}"><span>Filter</span><button type="button" class="is-current" data-settings-az>Reset</button>${letters}</div></div>`;
}

function renderBlockRow(node, { project, taskIndex }) {
  const key = node.attributes?.key || node.tag;
  const use = node.attributes?.use;
  const weight = node.attributes?.weight;
  const required = node.attributes?.required;
  const nested = Number(node.child_count) > 0 || (node.children || []).length > 0;
  const href = workflowHref({
    project,
    tab: "settings",
    task: taskIndex,
    section: "Blocks",
    block: blockPathKey(node),
  });
  const params = nested
    ? `<a class="settings-block-param is-custom" href="${escapeHtml(href)}" data-route="${escapeHtml(href)}" data-automation-block="${escapeHtml(blockPathKey(node))}">Custom</a>`
    : `<span class="settings-block-param">Default</span>`;
  return `<div class="settings-block-row" data-settings-tag="Block" data-block-key="${escapeHtml(key)}" data-block-family="${escapeHtml(blockFamily(key))}">
    ${use === "true" || use === "false" ? renderUseToggle(node.path, use) : "<span></span>"}
    <strong>${escapeHtml(blockDisplayName(key))}</strong>
    ${weight != null ? renderWeightInput(node.path, "weight", weight) : (required === "true" || required === "false" ? renderFlagToggle(node.path, "required", required, "Required") : "<span></span>")}
    ${params}
  </div>`;
}

function renderGroupedBlockRows(rows, options) {
  const sorted = sortBlockRows(rows);
  const parts = [];
  let family = "";
  for (const row of sorted) {
    const next = blockFamily(row.attributes?.key || row.tag);
    if (next !== family) {
      family = next;
      parts.push(`<div class="settings-block-family" data-block-family="${escapeHtml(family)}">${escapeHtml(family)}</div>`);
    }
    parts.push(renderBlockRow(row, options));
  }
  return parts.join("");
}

function renderBlockAccordion(panel, options, open) {
  const used = panel.rows.filter((row) => row.attributes?.use === "true").length;
  const title = blockPanelTitle(panel.id, panel.title);
  return `<details class="settings-block-accordion" data-settings-group="${escapeHtml(title)}" data-settings-block-panel="${escapeHtml(panel.id)}"${open ? " open" : ""}>
    <summary aria-label="${escapeHtml(title)}"><span class="settings-block-title">${escapeHtml(title)}</span> <span class="settings-block-count">${used} selected</span></summary>
    <div class="settings-block-body settings-block-body-fill">
      ${renderAzBar(panel.id)}
      <div class="settings-block-scroll settings-block-scroll-fill">
        <div class="settings-block-head"><span>Use</span><span>Name</span><span>Weight</span><span>Parameters</span></div>
        ${panel.rows.length ? renderGroupedBlockRows(panel.rows, options) : `<p class="field-help">No ${escapeHtml(panel.group.toLowerCase())} blocks in this saved XML.</p>`}
      </div>
    </div>
  </details>`;
}

function renderSideBlockPanel(title, rows, options, weightLabel = "Weight") {
  return `<section class="settings-group settings-block-side" data-settings-group="${escapeHtml(title)}">
    <h4>${escapeHtml(title)}</h4>
    <div class="settings-block-head"><span>Use</span><span>Name</span><span>${escapeHtml(weightLabel)}</span><span>Parameters</span></div>
    <div class="settings-block-scroll">${rows.length ? sortBlockRows(rows).map((row) => renderBlockRow(row, options)).join("") : `<p class="field-help">No ${escapeHtml(title.toLowerCase())} in this saved XML.</p>`}</div>
  </section>`;
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
  const mainPanels = [
    ...BLOCK_PANELS.map(([id, group]) => ({
      id,
      group,
      title: blockPanelTitle(id, group),
      rows: blocks.filter((item) => item.attributes?.category === id),
    })),
    ...extraCategories.map((id) => ({ id, group: humanizeNativeName(id), title: humanizeNativeName(id), rows: blocks.filter((item) => item.attributes?.category === id) })),
  ];
  const calibrateToggle = calibration?.attributes?.calibrateBeforeStart === "true" || calibration?.attributes?.calibrateBeforeStart === "false"
    ? renderFlagToggle(calibration.path, "calibrateBeforeStart", calibration.attributes.calibrateBeforeStart, "calibrate before start")
    : "";
  return `<div class="settings-node" data-settings-tag="Blocks">
    <div class="settings-blocks-shell">
      <div class="settings-blocks-main settings-blocks-main-fill">${mainPanels.map((panel, index) => renderBlockAccordion(panel, options, index === 0)).join("")}</div>
      <div class="settings-blocks-side">
        ${renderSideBlockPanel("Order types", (orderTypes?.children || []).filter((child) => child.tag === "Block"), options)}
        ${renderSideBlockPanel("Exit types", (exitTypes?.children || []).filter((child) => child.tag === "Block"), options, "Required")}
        <section class="settings-group settings-block-side" data-settings-group="Custom data">
          <h4>External indicators</h4>
          ${custom ? renderSettingsNode(custom, { heading: false }) : `<p class="field-help">No custom data defined.</p>`}
        </section>
      </div>
    </div>
    <div class="settings-block-foot">
      ${calibration ? `<button type="button" class="button button-secondary" data-settings-calibrate-open><span>Calibrate indicators</span></button>${calibrateToggle}` : ""}
    </div>
    ${calibration ? `<dialog class="settings-calibrate-dialog" data-settings-calibrate>
      <p class="sqx-advanced-head">Calibrate indicators</p>
      ${renderSettingsNode(calibration, { heading: false })}
      <p class="field-help">Calibrate now posts the saved Data symbol/timeframe/engine and these Calibration fields to installed StrategyQuant X. Returned min/max/step are written onto existing blocks. Keep StrategyQuant X open.</p>
      <p class="idea-save-status" data-settings-calibrate-status></p>
      <div class="sqx-settings-dialog-actions">
        <button type="button" class="button button-primary" data-settings-calibrate-now><span>Calibrate now</span></button>
        <button type="button" class="button button-secondary" data-settings-dialog-save><span>Save</span></button>
        <button type="button" class="button button-secondary" data-settings-calibrate-close><span>Close</span></button>
      </div>
    </dialog>` : ""}
  </div>`;
}

function renderMethodCards(methods, exclusive) {
  const used = methods.find((method) => method.attributes?.use === "true") || methods[0] || null;
  if (exclusive) {
    if (!used) return "";
    return `<div class="settings-mm-method is-selected" data-settings-tag="Method">
      ${paramsOf(used).map(renderParam).join("")}
      ${(used.children || []).filter((child) => child.tag !== "Params" && child.tag !== "Param" && child.tag !== "Parameter").map((child) => renderSettingsNode(child, { heading: true })).join("")}
    </div>`;
  }
  return methods.map((method) => `<div class="settings-mm-method" data-settings-tag="Method">
    ${renderNodeAttributes(method)}
    ${paramsOf(method).map(renderParam).join("")}
    ${(method.children || []).filter((child) => child.tag !== "Params" && child.tag !== "Param" && child.tag !== "Parameter").map((child) => renderSettingsNode(child, { heading: true })).join("")}
  </div>`).join("");
}

export function renderMoneyManagementPane(node) {
  const mm = firstChild(node, "MoneyManagement") || node;
  const risk = firstChild(node, "RiskManagement");
  const methods = (mm.children || []).filter((child) => child.tag === "Method");
  const riskMethods = (risk?.children || []).filter((child) => child.tag === "Method");
  const capital = firstChild(mm, "InitialCapital");
  const extra = (mm.children || []).filter((child) => child.tag !== "Method" && child.tag !== "InitialCapital");
  const exclusive = renderExclusiveUseChoices(methods, "Type");
  const riskExclusive = renderExclusiveUseChoices(riskMethods, "Risk method");
  const riskRest = (risk?.children || []).filter((child) => child.tag !== "Method");
  return `<div class="settings-node sqx-settings-stack" data-settings-tag="${escapeHtml(node.tag)}">
    ${renderNodeAttributes(node) ? renderFieldGroup("Money management", renderNodeAttributes(node)) : ""}
    ${renderFieldGroup("Type", `${exclusive}${renderMethodCards(methods, Boolean(exclusive))}`)}
    ${renderFieldGroup("Size / capital", capital ? renderTextControl(capital.path, capital.text || "", "Initial capital") : "")}
    ${extra.map((child) => renderSettingsNode(child, { heading: true })).join("")}
    ${renderFieldGroup("Risk management", risk
      ? `${renderNodeAttributes(risk)}${riskExclusive}${riskExclusive ? renderMethodCards(riskMethods, true) : riskMethods.map((method) => renderSettingsNode(method, { heading: true })).join("")}${riskRest.map((child) => renderSettingsNode(child, { heading: true })).join("")}`
      : "")}
  </div>`;
}

export function renderAtmPane(node) {
  const atm = firstChild(node, "ATM");
  const exits = firstChild(atm, "Exits");
  const generate = firstChild(node, "GenerateConfig");
  const types = firstChild(generate, "Types");
  const scenarios = firstChild(generate, "Scenarios");
  const extra = (node.children || []).filter((child) => child.tag !== "ATM" && child.tag !== "GenerateConfig");
  const exitRows = exits?.children || [];
  const typeRows = types?.children || [];
  const scenarioRows = scenarios?.children || [];
  return `<div class="settings-node sqx-settings-stack" data-settings-tag="ATMs">
    ${renderFieldGroup("Enable / size constraints", `${renderNodeAttributes(node)}${atm ? renderNodeAttributes(atm) : ""}`)}
    ${renderFieldGroup("Exits", exitRows.length ? exitRows.map((child) => renderSettingsNode(child, { heading: false })).join("") : `<p class="field-help">This saved ATM has no exit rows. This desktop does not add ATM exits.</p>`)}
    ${typeRows.length ? renderFieldGroup("Generate exit types", typeRows.map((child) => renderSettingsNode(child, { heading: false })).join("")) : ""}
    ${scenarioRows.length ? renderFieldGroup("Generate scenarios", scenarioRows.map((child) => renderSettingsNode(child, { heading: false })).join("")) : ""}
    ${extra.map((child) => renderSettingsNode(child, { heading: true })).join("")}
  </div>`;
}

export function renderDatabanksPane(node) {
  const rows = (node.children || []).filter((child) => child.tag === "Databank");
  const outputs = rows.filter((row) => String(row.attributes?.name || "").toLowerCase() === "output");
  const inputs = rows.filter((row) => String(row.attributes?.name || "").toLowerCase() === "input");
  const other = rows.filter((row) => !outputs.includes(row) && !inputs.includes(row));
  return `<div class="settings-node sqx-settings-stack" data-settings-tag="Databanks">
    ${renderFieldGroup("Output databanks", outputs.length ? renderDatabankRows(outputs) : "")}
    ${renderFieldGroup("Input databanks", inputs.length ? renderDatabankRows(inputs) : "")}
    ${other.length ? renderFieldGroup("Other databanks", renderDatabankRows(other)) : ""}
    ${!rows.length ? `<p class="field-help">This saved task has no Databank rows.</p>` : ""}
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
      return renderDataPane(node, options.taskKind || "");
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
      return renderFieldGroup(documentedSettingsLabel(node.tag), renderSettingsNode(node, { ...options, heading: false }));
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
  const tablist = `<div class="settings-section-roll-wrap">
    <button type="button" class="settings-roll-btn" data-settings-tab-roll="-1" aria-label="Scroll settings tabs left">‹</button>
    <div class="settings-section-roll"><div class="settings-section-tabs" role="tablist">${tabs.map((tab) => {
    const currentTab = tab.id === current.id;
    const href = workflowHref({
      project,
      tab: "settings",
      task: task.native_task_index,
      section: tab.id,
    });
    return `<a class="workflow-tab ${currentTab ? "is-current" : ""}" role="tab" aria-selected="${currentTab}" href="${escapeHtml(href)}" data-route="${escapeHtml(href)}" data-automation-section="${escapeHtml(tab.id)}">${escapeHtml(tab.label)}</a>`;
  }).join("")}</div></div>
    <button type="button" class="settings-roll-btn" data-settings-tab-roll="1" aria-label="Scroll settings tabs right">›</button>
  </div>`;
  const body = renderDocumentedPane(current, {
    project,
    taskIndex: task.native_task_index,
    taskKind: task.kind || "",
    method,
    methodPane,
    block,
  });
  return `<form class="full-settings" data-automation-settings-form data-settings-task="${task.native_task_index}">
    <div class="settings-toolbar">
      <p class="sqx-advanced-head">Advanced settings for '${escapeHtml(task.name || task.kind)}'</p>
      ${actionButton("Save settings", { primary: true, attrs: `data-automation-save-settings data-project-task="${task.native_task_index}"` })}
    </div>
    ${tablist}
    <div class="settings-pane">${body}</div>
    <p class="idea-save-status" data-automation-settings-status></p>
    <p class="field-help">Only attributes or existing text on this native element can be written. This desktop does not invent SQX parameters, Condition rows, or What-If scenarios.</p>
  </form>`;
}
