import { escapeHtml } from "./ui.mjs";

export function humanizeNativeName(value) {
  return String(value || "")
    .replace(/_/g, " ")
    .replace(/([a-z0-9])([A-Z])/g, "$1 $2")
    .replace(/([A-Z]+)([A-Z][a-z])/g, "$1 $2")
    .replace(/\b\w/g, (letter) => letter.toUpperCase())
    .trim();
}

export function projectName(value) {
  return typeof value === "string"
    && value
    && value === value.trim()
    && !value.includes("/")
    && !value.includes("\\")
    && !value.includes("\0")
    && ![".", ".."].includes(value)
    ? value
    : "";
}

export const RUN_MODULE_PATHS = Object.freeze({
  "/builder": "Builder",
  "/retester": "Retester",
  "/optimizer": "Optimizer",
});
export const CUSTOM_PROJECTS_PATH = "/custom-projects";

export function currentWorkflowPath() {
  const path = typeof globalThis.location !== "undefined" ? globalThis.location.pathname : "";
  if (path === "/automation") return CUSTOM_PROJECTS_PATH;
  if (path in RUN_MODULE_PATHS || path === CUSTOM_PROJECTS_PATH) return path;
  return CUSTOM_PROJECTS_PATH;
}

export function workflowHref({
  path = "",
  project = "",
  tab = "",
  task = "",
  section = "",
  method = "",
  methodPane = "",
  databank = "",
  archive = "",
  resultView = "",
  sample = "",
  direction = "",
  block = "",
} = {}) {
  const params = new URLSearchParams();
  const exact = projectName(project);
  const base = path || currentWorkflowPath();
  if (exact && !(base in RUN_MODULE_PATHS)) params.set("project", exact);
  if (tab && tab !== "progress") params.set("tab", tab);
  if (task) params.set("task", String(task));
  if (section) params.set("section", section);
  if (method) params.set("method", method);
  if (method && methodPane) params.set("methodPane", methodPane);
  if (block) params.set("block", block);
  if (databank) params.set("databank", databank);
  if (archive) params.set("archive", archive);
  if (resultView) params.set("resultView", resultView);
  if (sample === "is" || sample === "oos") params.set("sample", sample);
  if (direction === "long" || direction === "short") params.set("direction", direction);
  const query = params.toString();
  return query ? `${base}?${query}` : base;
}

export function findNodesByTag(nodes, tag) {
  const found = [];
  for (const node of nodes || []) {
    if (node.tag === tag) found.push(node);
    found.push(...findNodesByTag(node.children, tag));
  }
  return found;
}

export function firstChild(node, tag) {
  return (node?.children || []).find((child) => child.tag === tag) || null;
}

function pathTag(path) {
  return String(path?.[path.length - 1] || "").replace(/:\d+$/, "");
}

function includeCurrentChoice(choices, value) {
  if (value == null || value === "") return choices;
  if (choices.some((choice) => choice[0] === value)) return choices;
  return [[value, value], ...choices];
}

const ENGINE_CHOICES = Object.freeze([
  ["MetaTrader4", "MetaTrader4"],
  ["MetaTrader5 (netted)", "MetaTrader5 (netted)"],
  ["MetaTrader5 (hedged)", "MetaTrader5 (hedged)"],
  ["Tradestation", "Tradestation"],
  ["MultiCharts", "MultiCharts"],
  ["JForex", "JForex"],
  ["Stockpicker", "AlgoCloud Stockpicker"],
  ["Single-asset cloud strategy", "AlgoCloud Single-asset"],
]);

const TIMEFRAME_CHOICES = Object.freeze([
  ["TICK", "TICK"],
  ["M1", "M1"],
  ["M5", "M5"],
  ["M15", "M15"],
  ["M30", "M30"],
  ["H1", "H1"],
  ["H4", "H4"],
  ["D1", "D1"],
  ["Weekly", "Weekly"],
  ["Monthly", "Monthly"],
]);

const GENERATION_TASK_CHOICES = Object.freeze([
  ["random", "Random"],
  ["genetic", "Genetic"],
]);

const GENERATION_BUILDER_CHOICES = Object.freeze([
  ["random-generation", "Random Discovery"],
  ["genetic-evolution", "Genetic Evolution"],
]);

const STRATEGY_TYPE_CHOICES = Object.freeze([
  ["simple", "Simple strategy [default]"],
  ["multi-tf", "Multi-TF or multi-symbol strategy"],
  ["template", "Strategy from template"],
  ["improve", "Improve existing strategy"],
]);

const IMPROVE_TYPE_CHOICES = Object.freeze([
  ["strategy", "Select strategy file"],
  ["databank", "Improve all strategies in databank"],
]);

const ARCHITECTURE_CHOICES = Object.freeze([
  ["sq4", "SQX Signals Style"],
  ["sq4fuzzy", "SQX Signals Style with Fuzzy Logic"],
  ["sq3", "Old SQ3 Style"],
]);

const COMPARATOR_CHOICES = Object.freeze([
  [">", ">"],
  [">=", ">="],
  ["<", "<"],
  ["<=", "<="],
  ["=", "="],
  ["==", "=="],
  ["!=", "!="],
  ["<>", "<>"],
]);

const MARKET_SIDE_CHOICES = Object.freeze([
  ["both", "Both"],
  ["long", "Long"],
  ["short", "Short"],
]);

const STOP_CONDITION_CHOICES = Object.freeze([
  ["never", "Never"],
  ["passed-count", "Totally N strategies (that passed filters) were generated"],
  ["databank-full", "Databank is full (reached maximum capacity)"],
  ["time-limit", "After a time limit"],
]);

const FITNESS_METHOD_CHOICES = Object.freeze([
  ["ComputeFromStrategyResult", "Compute from strategy result"],
]);

const IMPROVE_ACTION_CHOICES = Object.freeze([
  ["add-or-replace", "Add or replace"],
  ["replace", "Replace"],
  ["add", "Add"],
]);

const RADIO_CHOICE_LIMIT = 4;

const officialSqxChoices = {
  rankingTypes: null,
  rankingReady: false,
  templateFiles: null,
  strategyFiles: null,
  filesReady: false,
};

export function resetOfficialSqxChoices() {
  officialSqxChoices.rankingTypes = null;
  officialSqxChoices.rankingReady = false;
  officialSqxChoices.templateFiles = null;
  officialSqxChoices.strategyFiles = null;
  officialSqxChoices.filesReady = false;
}

export function setOfficialSqxChoices({
  rankingTypes,
  templateFiles,
  strategyFiles,
  rankingReady,
  filesReady,
} = {}) {
  if (rankingTypes !== undefined) {
    officialSqxChoices.rankingTypes = rankingTypes;
    if (rankingReady === undefined) officialSqxChoices.rankingReady = Array.isArray(rankingTypes);
  }
  if (rankingReady !== undefined) officialSqxChoices.rankingReady = rankingReady;
  if (templateFiles !== undefined) officialSqxChoices.templateFiles = templateFiles;
  if (strategyFiles !== undefined) officialSqxChoices.strategyFiles = strategyFiles;
  if (filesReady !== undefined) officialSqxChoices.filesReady = filesReady;
  else if (templateFiles !== undefined || strategyFiles !== undefined) {
    officialSqxChoices.filesReady = Array.isArray(templateFiles) || Array.isArray(strategyFiles);
  }
}

export function officialSqxChoiceState() {
  return officialSqxChoices;
}

function choicePairs(rows) {
  if (!Array.isArray(rows) || !rows.length) return null;
  return rows.map((row) => (Array.isArray(row) ? row : [String(row.key ?? row), String(row.name ?? row)]));
}

export function nativeChoicesFor(attribute, value, context = {}) {
  const tag = context.tag || pathTag(context.path);
  if (attribute === "engine") {
    return includeCurrentChoice(ENGINE_CHOICES.slice(), value);
  }
  if (attribute === "timeframe") {
    return includeCurrentChoice(TIMEFRAME_CHOICES.slice(), value);
  }
  if (attribute === "generationType") {
    if (GENERATION_TASK_CHOICES.some((choice) => choice[0] === value)) return GENERATION_TASK_CHOICES.slice();
    if (GENERATION_BUILDER_CHOICES.some((choice) => choice[0] === value)) return GENERATION_BUILDER_CHOICES.slice();
    return null;
  }
  if (attribute === "type" && tag === "StrategyType") {
    return includeCurrentChoice(STRATEGY_TYPE_CHOICES.slice(), value);
  }
  if (attribute === "improveType" && tag === "StrategyType") {
    return includeCurrentChoice(IMPROVE_TYPE_CHOICES.slice(), value);
  }
  if (attribute === "architecture" && tag === "StrategyType") {
    return includeCurrentChoice(ARCHITECTURE_CHOICES.slice(), value);
  }
  if (attribute === "type" && tag === "MarketSides") {
    return includeCurrentChoice(MARKET_SIDE_CHOICES.slice(), value);
  }
  if (attribute === "type" && tag === "StopCondition") {
    return includeCurrentChoice(STOP_CONDITION_CHOICES.slice(), value);
  }
  if (attribute === "type" && tag === "Ranking") {
    const types = choicePairs(officialSqxChoices.rankingTypes);
    return types ? includeCurrentChoice(types, value) : null;
  }
  if (attribute === "templateFile") {
    const files = choicePairs(officialSqxChoices.templateFiles);
    return files ? includeCurrentChoice(files, value) : null;
  }
  if (attribute === "strategyFile") {
    const files = choicePairs(officialSqxChoices.strategyFiles);
    return files ? includeCurrentChoice(files, value) : null;
  }
  if (attribute === "method" && tag === "FitnessCriteria") {
    return includeCurrentChoice(FITNESS_METHOD_CHOICES.slice(), value);
  }
  if (attribute === "action" && (tag === "LongImprovement" || tag === "ShortImprovement")) {
    return includeCurrentChoice(IMPROVE_ACTION_CHOICES.slice(), value);
  }
  if (attribute === "value" && tag === "Comparator") {
    return includeCurrentChoice(COMPARATOR_CHOICES.slice(), value);
  }
  return null;
}

function sampleTypeLabel(sampleType) {
  const value = Number(sampleType);
  if (value === 127) return "full sample";
  if (value >= 10 && value < 20) return "in-sample";
  if (value >= 20 && value <= 30) return "out-of-sample";
  return sampleType ? `sample ${sampleType}` : "";
}

function nestedTextAndAttrs(node, limit = 4) {
  const bits = [];
  const visit = (item) => {
    if (!item || bits.length >= limit) return;
    if (item.text) bits.push(`${humanizeNativeName(item.tag)} ${item.text}`);
    for (const [key, value] of Object.entries(item.attributes || {})) {
      if (key === "use" || bits.length >= limit) continue;
      bits.push(`${humanizeNativeName(key)} ${value}`);
    }
    for (const child of item.children || []) visit(child);
  };
  visit(node);
  return bits;
}

function conditionCount(node) {
  let count = 0;
  const visit = (item) => {
    if (!item) return;
    if (item.tag === "Condition") {
      count += 1;
      return;
    }
    for (const child of item.children || []) visit(child);
  };
  visit(node);
  return count;
}

function radioGroupName(path, attribute) {
  return `sqx-${JSON.stringify(path)}-${attribute}`.replace(/[^A-Za-z0-9_-]/g, "_");
}

function renderRadioControl(path, attribute, value, choices, name) {
  const encodedPath = escapeHtml(JSON.stringify(path));
  const group = escapeHtml(radioGroupName(path, attribute));
  const options = choices.map(([choice, label]) => (
    `<label class="settings-radio"><input type="radio" name="${group}" value="${escapeHtml(choice)}" ${choice === value ? "checked" : ""} data-settings-path="${encodedPath}" data-settings-attribute="${escapeHtml(attribute)}" data-settings-kind="choice">${escapeHtml(label)}</label>`
  )).join("");
  return `<fieldset class="settings-radio-group"><legend>${escapeHtml(name)}</legend>${options}</fieldset>`;
}

function renderSelectControl(path, attribute, value, choices, name) {
  const encodedPath = escapeHtml(JSON.stringify(path));
  const options = choices.map(([choice, label]) => (
    `<option value="${escapeHtml(choice)}" ${choice === value ? "selected" : ""}>${escapeHtml(label)}</option>`
  )).join("");
  return `<label class="field-label">${escapeHtml(name)}<select class="idea-editor workflow-input workflow-select" data-settings-path="${encodedPath}" data-settings-attribute="${escapeHtml(attribute)}" data-settings-kind="choice" aria-label="${escapeHtml(name)}">${options}</select></label>`;
}

export function renderAttributeControl(path, attribute, value, context = {}) {
  const encodedPath = escapeHtml(JSON.stringify(path));
  const name = humanizeNativeName(attribute);
  if (value === "true" || value === "false") {
    const on = value === "true";
    return `<div class="settings-row"><span>${escapeHtml(name)}</span><button type="button" class="toggle ${on ? "is-on" : ""}" role="switch" aria-checked="${on}" data-settings-path="${encodedPath}" data-settings-attribute="${escapeHtml(attribute)}" data-settings-kind="flag" title="${escapeHtml(name)}"></button></div>`;
  }
  const tag = context.tag || pathTag(path);
  if (tag === "Ranking" && attribute === "type" && !officialSqxChoices.rankingReady) {
    return `<p class="field-help">Ranking fitness types come from StrategyQuant X fitnessMethodStrategyResult/list. Keep StrategyQuant X open.</p>`;
  }
  if ((attribute === "templateFile" || attribute === "strategyFile") && !officialSqxChoices.filesReady) {
    return `<label class="field-label">${escapeHtml(name)}<input class="idea-editor workflow-input" data-settings-path="${encodedPath}" data-settings-attribute="${escapeHtml(attribute)}" value="${escapeHtml(value)}" disabled aria-label="${escapeHtml(name)}" /></label><p class="field-help">Official file list is unavailable. Keep StrategyQuant X open.</p>`;
  }
  const choices = nativeChoicesFor(attribute, value, { ...context, path, tag });
  if (choices?.length) {
    const radios = (tag === "Ranking" && attribute === "type") || choices.length <= RADIO_CHOICE_LIMIT;
    return radios
      ? renderRadioControl(path, attribute, value, choices, name)
      : renderSelectControl(path, attribute, value, choices, name);
  }
  return `<label class="field-label">${escapeHtml(name)}<input class="idea-editor workflow-input" data-settings-path="${encodedPath}" data-settings-attribute="${escapeHtml(attribute)}" value="${escapeHtml(value)}" aria-label="${escapeHtml(name)}" /></label>`;
}

export function renderTextControl(path, value, label = "") {
  const encodedPath = escapeHtml(JSON.stringify(path));
  const name = label || humanizeNativeName(String(path[path.length - 1] || "").replace(/:\d+$/, ""));
  if (value === "true" || value === "false") {
    const on = value === "true";
    return `<div class="settings-row"><span>${escapeHtml(name)}</span><button type="button" class="toggle ${on ? "is-on" : ""}" role="switch" aria-checked="${on}" data-settings-path="${encodedPath}" data-settings-text="1" data-settings-kind="flag" title="${escapeHtml(name)}"></button></div>`;
  }
  const long = String(value || "").length > 80 || String(value || "").includes("<");
  if (long) {
    return `<label class="field-label">${escapeHtml(name)}<textarea class="idea-editor workflow-input" data-settings-path="${encodedPath}" data-settings-text="1" aria-label="${escapeHtml(name)}">${escapeHtml(value)}</textarea></label>`;
  }
  return `<label class="field-label">${escapeHtml(name)}<input class="idea-editor workflow-input" data-settings-path="${encodedPath}" data-settings-text="1" value="${escapeHtml(value)}" aria-label="${escapeHtml(name)}" /></label>`;
}

export function renderExclusiveUseChoices(nodes, legend = "Type") {
  const choosable = (nodes || []).filter((node) => node.attributes?.use === "true" || node.attributes?.use === "false");
  if (choosable.length < 2) return "";
  const group = escapeHtml(radioGroupName(choosable[0].path.slice(0, -1), "use"));
  const options = choosable.map((node) => {
    const encodedPath = escapeHtml(JSON.stringify(node.path));
    const label = humanizeNativeName(node.attributes?.type || node.tag);
    return `<label class="settings-radio"><input type="radio" name="${group}" ${node.attributes.use === "true" ? "checked" : ""} data-settings-path="${encodedPath}" data-settings-attribute="use" data-settings-kind="choice" data-settings-exclusive-use="1">${escapeHtml(label)}</label>`;
  }).join("");
  return `<fieldset class="settings-radio-group" data-settings-exclusive-group><legend>${escapeHtml(legend)}</legend>${options}</fieldset>`;
}

export function renderNodeAttributes(node, skip = []) {
  return Object.entries(node?.attributes || {})
    .filter(([attribute]) => !skip.includes(attribute))
    .map(([attribute, value]) => renderAttributeControl(node.path, attribute, value, { tag: node.tag }))
    .join("");
}

function renderComparatorCell(comparator) {
  const value = comparator?.attributes?.value ?? "";
  const encodedPath = escapeHtml(JSON.stringify(comparator.path));
  return `<input class="idea-editor workflow-input" data-settings-path="${encodedPath}" data-settings-attribute="value" value="${escapeHtml(value)}" aria-label="${escapeHtml(humanizeNativeName("comparator"))}" />`;
}

function renderThresholdCell(rightSide) {
  const numeric = firstChild(rightSide, "Numeric-Value");
  const column = firstChild(rightSide, "Column-Value");
  if (numeric) {
    return renderAttributeControl(numeric.path, "value", numeric.attributes?.value ?? "", { tag: numeric.tag });
  }
  if (column) {
    return renderAttributeControl(column.path, "column", column.attributes?.column ?? "", { tag: column.tag });
  }
  return "";
}

function renderConditionRow(node) {
  const left = firstChild(firstChild(node, "Left-Side"), "Column-Value");
  const comparator = firstChild(node, "Comparator");
  const rightSide = firstChild(node, "Right-Side");
  const sample = sampleTypeLabel(left?.attributes?.sampleType);
  const use = node.attributes?.use;
  return `<tr data-settings-tag="Condition">
    <td>${use === "true" || use === "false" ? renderAttributeControl(node.path, "use", use) : ""}</td>
    <td>${left ? renderAttributeControl(left.path, "column", left.attributes?.column ?? "", { tag: left.tag }) : ""}</td>
    <td>${escapeHtml(sample)}</td>
    <td>${comparator ? renderComparatorCell(comparator) : ""}</td>
    <td>${renderThresholdCell(rightSide)}</td>
  </tr>`;
}

export function renderConditionTable(conditionsNode) {
  const rows = (conditionsNode?.children || []).filter((child) => child.tag === "Condition");
  if (!rows.length) {
    return `<p class="field-help">This saved task has no Ranking or filter Condition rows.</p>`;
  }
  return `<table class="settings-condition-table">
    <thead><tr><th>${escapeHtml(humanizeNativeName("use"))}</th><th>${escapeHtml(humanizeNativeName("column"))}</th><th>${escapeHtml(humanizeNativeName("sampleType"))}</th><th>${escapeHtml(humanizeNativeName("comparator"))}</th><th>${escapeHtml(humanizeNativeName("threshold"))}</th></tr></thead>
    <tbody>${rows.map(renderConditionRow).join("")}</tbody>
  </table>`;
}

export function choiceLabel(attribute, value, context = {}) {
  const choices = nativeChoicesFor(attribute, value, context);
  if (choices) {
    const row = choices.find(([choice]) => choice === value);
    if (row) return row[1];
  }
  return value ?? "";
}

export function nodeSettingSummary(node) {
  if (!node) return "";
  const bits = [];
  for (const [attribute, value] of Object.entries(node.attributes || {})) {
    bits.push(choiceLabel(attribute, value, { tag: node.tag, path: node.path }));
  }
  if (node.text) bits.push(String(node.text));
  return bits.slice(0, 3).join(" · ") || humanizeNativeName(node.tag);
}

export function renderConfigRow(label, summary, dialogBody, dialogKey) {
  const key = escapeHtml(dialogKey);
  return `<div class="sqx-config-row">
    <span class="sqx-config-label">${escapeHtml(label)}</span>
    <button type="button" class="sqx-config-gear" data-settings-dialog-open="${key}" aria-label="Configure ${escapeHtml(label)}">⚙</button>
    <span class="sqx-config-summary">${escapeHtml(summary)}</span>
    <dialog class="sqx-settings-dialog" data-settings-dialog="${key}">
      <p class="sqx-advanced-head">${escapeHtml(label)}</p>
      <div class="sqx-settings-dialog-body settings-node">${dialogBody}</div>
      <div class="sqx-settings-dialog-actions">
        <button type="button" class="button button-secondary" data-settings-dialog-save><span>Save</span></button>
        <button type="button" class="button button-secondary" data-settings-dialog-close><span>Close</span></button>
      </div>
    </dialog>
  </div>`;
}

export function renderConfigCard(title, summary, dialogBody, dialogKey) {
  if (!dialogBody) return "";
  return `<section class="settings-group sqx-settings-card" data-settings-group="${escapeHtml(title)}">${renderConfigRow(title, summary, dialogBody, dialogKey)}</section>`;
}

export function renderFieldGroup(title, body) {
  if (!body) return "";
  return `<section class="settings-group sqx-settings-card" data-settings-group="${escapeHtml(title)}"><h4>${escapeHtml(title)}</h4>${body}</section>`;
}

function renderRankingsPaneSection(title, body, emptyHelp = "") {
  const content = body || (emptyHelp ? `<p class="field-help">${escapeHtml(emptyHelp)}</p>` : "");
  return renderFieldGroup(title, content);
}

function renderRankingSubgroup(title, body) {
  if (!body) return "";
  return `<div class="settings-subgroup" data-settings-subgroup="${escapeHtml(title)}"><h5>${escapeHtml(title)}</h5>${body}</div>`;
}

function renderRankingMaxStrategies(node) {
  if (!node) return "";
  return renderTextControl(node.path, node.text || "", "Maximum strategies to store in databank");
}

function renderRankingStopCondition(node) {
  if (!node) return "";
  const type = node.attributes?.type != null
    ? renderAttributeControl(node.path, "type", node.attributes.type, { tag: node.tag })
    : "";
  const rest = Object.entries(node.attributes || {})
    .filter(([attribute]) => attribute !== "type")
    .map(([attribute, value]) => renderAttributeControl(node.path, attribute, value, { tag: node.tag }))
    .join("");
  return renderRankingSubgroup("Stop generation when", `${type}${rest}`);
}

function renderRankingFitness(node) {
  if (!node) return "";
  const settings = firstChild(node, "Settings");
  const ranking = settings ? firstChild(settings, "Ranking") : null;
  const fields = Object.entries(node.attributes || {})
    .map(([attribute, value]) => renderAttributeControl(node.path, attribute, value, { tag: node.tag }))
    .join("");
  const computeFrom = ranking
    ? renderRankingSubgroup("Compute from", renderAttributeControl(ranking.path, "type", ranking.attributes?.type || "", { tag: ranking.tag }))
    : (settings ? renderSettingsNode(settings, { heading: false }) : "");
  const extra = (node.children || [])
    .filter((child) => child.tag !== "Settings")
    .map((child) => renderSettingsNode(child, { heading: false }))
    .join("");
  return `${fields}${computeFrom}${extra}`;
}

function automaticDismissalSummary(node) {
  const problems = (node?.children || []).filter((child) => child.tag === "Problem");
  if (!problems.length) return nodeSettingSummary(node);
  const active = problems.filter((problem) => problem.attributes?.dismiss === "true").length;
  return active
    ? `${active} ${humanizeNativeName("Problem")} ${humanizeNativeName("dismiss")}`
    : `${problems.length} ${humanizeNativeName("Problem")}`;
}

function renderProblemCell(path, attribute, value, tag) {
  const encodedPath = escapeHtml(JSON.stringify(path));
  if (value === "true" || value === "false") {
    const on = value === "true";
    return `<button type="button" class="toggle ${on ? "is-on" : ""}" role="switch" aria-checked="${on}" aria-label="${escapeHtml(humanizeNativeName(attribute))}" data-settings-path="${encodedPath}" data-settings-attribute="${escapeHtml(attribute)}" data-settings-kind="flag" title="${escapeHtml(humanizeNativeName(attribute))}"></button>`;
  }
  return `<input class="idea-editor workflow-input" data-settings-path="${encodedPath}" data-settings-attribute="${escapeHtml(attribute)}" value="${escapeHtml(value)}" aria-label="${escapeHtml(humanizeNativeName(attribute))}" />`;
}

function renderProblemRow(problem) {
  const attrs = problem.attributes || {};
  const useCell = attrs.use === "true" || attrs.use === "false"
    ? renderProblemCell(problem.path, "use", attrs.use, problem.tag)
    : "";
  const codeCell = attrs.code != null
    ? renderProblemCell(problem.path, "code", attrs.code, problem.tag)
    : "";
  const dismissCell = attrs.dismiss === "true" || attrs.dismiss === "false"
    ? renderProblemCell(problem.path, "dismiss", attrs.dismiss, problem.tag)
    : "";
  return `<tr data-settings-tag="Problem"><td>${useCell}</td><td>${codeCell}</td><td>${dismissCell}</td></tr>`;
}

function renderAutomaticDismissal(automatic, options = {}) {
  if (!automatic) return "";
  const problems = (automatic.children || []).filter((child) => child.tag === "Problem");
  const otherChildren = (automatic.children || []).filter((child) => child.tag !== "Problem");
  const parentFields = renderNodeAttributes(automatic);
  const problemTable = problems.length
    ? `<table class="settings-problem-table">
      <thead><tr><th>${escapeHtml(humanizeNativeName("use"))}</th><th>${escapeHtml(humanizeNativeName("code"))}</th><th>${escapeHtml(humanizeNativeName("dismiss"))}</th></tr></thead>
      <tbody>${problems.map(renderProblemRow).join("")}</tbody>
    </table>`
    : "";
  const rest = otherChildren.map((child) => renderSettingsNode(child, { ...options, heading: false })).join("");
  return `${parentFields}${problemTable}${rest}`;
}

function renderRankingFilterCard({ automatic, dismissSimilar, conditionsType, conditions }, options) {
  const automaticBody = automatic
    ? renderAutomaticDismissal(automatic, options)
    : `<p class="field-help">This saved task has no AutomaticDismissal subtree.</p>`;
  const customBody = `${dismissSimilar ? renderSettingsNode(dismissSimilar, { ...options, heading: false }) : ""}${conditionsType ? renderSettingsNode(conditionsType, { ...options, heading: false }) : ""}${conditions ? renderConditionTable(conditions) : ""}`;
  return `${automatic
    ? renderConfigRow("Automatic filters", automaticDismissalSummary(automatic), automaticBody, "ranking-automatic-filters")
    : ""}${renderRankingSubgroup("Custom filters", customBody)}`;
}

export function renderRankingsPane(node, options = {}) {
  const byTag = new Map((node.children || []).map((child) => [child.tag, child]));
  const databank = byTag.get("MaxStrategies");
  const stop = byTag.get("StopCondition");
  const fitness = byTag.get("FitnessCriteria");
  const dismissSimilar = byTag.get("DismissTooSimilarStrategies");
  const automatic = byTag.get("AutomaticDismissal");
  const conditionsType = byTag.get("ConditionsType");
  const conditions = byTag.get("Conditions");
  const shown = new Set(["MaxStrategies", "StopCondition", "FitnessCriteria", "DismissTooSimilarStrategies", "AutomaticDismissal", "ConditionsType", "Conditions"]);
  const rest = (node.children || []).filter((child) => !shown.has(child.tag));
  const maxBody = `${renderRankingMaxStrategies(databank)}${renderRankingStopCondition(stop)}`;
  const fitnessBody = renderRankingFitness(fitness);
  const filterBody = renderRankingFilterCard({ automatic, dismissSimilar, conditionsType, conditions }, options);
  const left = [
    renderRankingsPaneSection("Maximum top strategies to store", maxBody, "This saved task has no MaxStrategies or StopCondition fields."),
    renderRankingsPaneSection("Strategy Quality ranking (fitness)", fitnessBody, "This saved task has no FitnessCriteria subtree."),
  ].join("");
  const right = [
    renderRankingsPaneSection("Strategy filtering conditions", `<p class="field-help">Configure which strategies will be saved into Results databank</p>${filterBody}`, "This saved task has no custom filter conditions."),
    ...rest.map((child) => renderFieldGroup(humanizeNativeName(child.tag), renderSettingsNode(child, { ...options, heading: false }))),
  ].join("");
  return `<div class="settings-node sqx-settings-grid sqx-settings-grid-ranking" data-settings-tag="Rankings">
    <div class="sqx-settings-grid-col sqx-settings-grid-col-left">${left}</div>
    <div class="sqx-settings-grid-col sqx-settings-grid-col-right">${right}</div>
  </div>`;
}

export function renderCrossCheckMethodView(node, { project = "", taskIndex = "", methodPane = "" } = {}) {
  const settings = firstChild(node, "Settings");
  const acceptance = firstChild(node, "AcceptanceSettings");
  const panes = [
    settings ? ["settings", "Settings"] : null,
    acceptance ? ["filtering", "Filtering"] : null,
  ].filter(Boolean);
  const currentPane = panes.some(([id]) => id === methodPane) ? methodPane : (panes[0]?.[0] || "");
  const tabs = panes.length
    ? `<div class="settings-nested-tabs" role="tablist">${panes.map(([id, label]) => {
      const href = workflowHref({
        project,
        tab: "settings",
        task: taskIndex,
        section: "CrossChecks",
        method: node.tag,
        methodPane: id,
      });
      return `<a class="workflow-tab ${id === currentPane ? "is-current" : ""}" role="tab" aria-selected="${id === currentPane}" href="${escapeHtml(href)}" data-route="${escapeHtml(href)}" data-automation-method="${escapeHtml(node.tag)}" data-automation-method-pane="${escapeHtml(id)}">${escapeHtml(label)}</a>`;
    }).join("")}</div>`
    : "";
  const body = currentPane === "filtering"
    ? renderSettingsNode(acceptance, { heading: false, project, taskIndex })
    : currentPane === "settings"
      ? renderSettingsNode(settings, { heading: false, project, taskIndex })
      : `<p class="field-help">This saved method has no Settings or Filtering subtree.</p>`;
  const backHref = workflowHref({ project, tab: "settings", task: taskIndex, section: "CrossChecks" });
  return `<div class="settings-node" data-settings-tag="${escapeHtml(node.tag)}" data-cross-check-method="${escapeHtml(node.tag)}">
    <p class="workflow-crumb"><a class="workflow-link" href="${escapeHtml(backHref)}" data-route="${escapeHtml(backHref)}" data-automation-section="CrossChecks">Cross checks</a><span>/</span><strong>${escapeHtml(humanizeNativeName(node.tag))}</strong></p>
    ${renderNodeAttributes(node)}
    ${tabs}
    ${body}
  </div>`;
}

const CROSS_CHECK_TIERS = Object.freeze([
  ["tier-basic", new Set(["WhatIf", "MonteCarloManipulation", "MonteCarlo", "RetestWithHigherPrecision"])],
  ["tier-standard", new Set(["RetestOnAdditionalMarkets", "MonteCarloRetest"])],
  ["tier-extensive", new Set(["SequentialOptimization", "OptProfileSysParamPermutation", "WalkForwardOptimization", "WalkForwardMatrix"])],
]);

function crossCheckTier(tag) {
  for (const [tier, tags] of CROSS_CHECK_TIERS) {
    if (tags.has(tag)) return tier;
  }
  return "tier-other";
}

function settingsSummary(node) {
  const bits = nestedTextAndAttrs(node, 3);
  return bits.length ? bits.join(" · ") : "Nested settings present in this saved task";
}

function filterSummary(node) {
  const filters = conditionCount(node);
  return filters ? `${filters} condition${filters === 1 ? "" : "s"}` : "No filter conditions in this saved task";
}

function renderCrossCheckRow(child, { project = "", taskIndex = "" } = {}) {
  const settings = firstChild(child, "Settings");
  const acceptance = firstChild(child, "AcceptanceSettings");
  const href = workflowHref({
    project,
    tab: "settings",
    task: taskIndex,
    section: "CrossChecks",
    method: child.tag,
  });
  const useControl = child.attributes?.use === "true" || child.attributes?.use === "false"
    ? renderAttributeControl(child.path, "use", child.attributes.use, { tag: child.tag })
    : "";
  const settingsGear = settings
    ? renderConfigRow("Cross check settings", settingsSummary(settings), renderSettingsNode(settings, { heading: false }), `cross-${child.tag}-settings`)
    : `<span class="cross-check-method-empty">—</span>`;
  const filterGear = acceptance
    ? renderConfigRow("Filters", filterSummary(acceptance), renderSettingsNode(acceptance, { heading: false }), `cross-${child.tag}-filtering`)
    : `<span class="cross-check-method-empty">—</span>`;
  const open = settings || acceptance
    ? `<a class="button-small cross-check-method-open" href="${escapeHtml(href)}" data-route="${escapeHtml(href)}" data-automation-method="${escapeHtml(child.tag)}">Open</a>`
    : "";
  return `<div class="cross-check-method" data-settings-tag="${escapeHtml(child.tag)}">
    <div class="cross-check-method-name">
      ${useControl}
      <strong>${escapeHtml(humanizeNativeName(child.tag))}</strong>
      ${open}
    </div>
    <div class="cross-check-method-columns">
      ${settingsGear}
      ${filterGear}
    </div>
  </div>`;
}

const CROSS_CHECK_TIER_META = Object.freeze({
  "tier-basic": { title: "BASIC (FAST)", help: "These cross checks require none or only one additional backtest, so they are fast." },
  "tier-standard": { title: "STANDARD (SLOW)", help: "Cross checks that require multiple additional backtests, thus multiplying the time of processing the whole strategy." },
  "tier-extensive": { title: "EXTENSIVE", help: "Advanced optimization and walk-forward cross checks." },
  "tier-other": { title: "Other", help: "" },
});

export function renderCrossChecksPane(node, { project = "", taskIndex = "", method = "", methodPane = "" } = {}) {
  if (method) {
    const methodNode = (node.children || []).find((child) => child.tag === method);
    if (methodNode) return renderCrossCheckMethodView(methodNode, { project, taskIndex, methodPane });
  }
  const fields = renderNodeAttributes(node);
  const tiers = new Map([...CROSS_CHECK_TIERS.map(([tier]) => [tier, []]), ["tier-other", []]]);
  for (const child of node.children || []) {
    tiers.get(crossCheckTier(child.tag)).push(child);
  }
  const sections = [...CROSS_CHECK_TIERS, ["tier-other", null]].map(([tier]) => {
    const rows = tiers.get(tier);
    if (!rows?.length) return "";
    const meta = CROSS_CHECK_TIER_META[tier] || CROSS_CHECK_TIER_META["tier-other"];
    return `<section class="sqx-cross-check-tier sqx-settings-card" data-cross-check-tier="${tier}">
      <h4 class="sqx-cross-check-tier-title">${escapeHtml(meta.title)}</h4>
      ${meta.help ? `<p class="field-help sqx-cross-check-tier-help">${escapeHtml(meta.help)}</p>` : ""}
      <div class="cross-check-tier-head"><span></span><span>Cross check settings</span><span>Filters</span></div>
      <div class="cross-check-list">${rows.map((row) => renderCrossCheckRow(row, { project, taskIndex })).join("")}</div>
    </section>`;
  }).join("");
  return `<div class="settings-node sqx-settings-stack" data-settings-tag="CrossChecks">${fields}${sections}</div>`;
}

export function renderSettingsNode(node, options = {}) {
  const heading = options.heading !== false;
  if (node.tag === "Rankings") return renderRankingsPane(node, options);
  if (node.tag === "CrossChecks") return renderCrossChecksPane(node, options);
  if (node.tag === "Conditions") {
    return `<div class="settings-node" data-settings-tag="Conditions">${heading ? `<h4>Conditions</h4>` : ""}${renderConditionTable(node)}</div>`;
  }
  const attributes = Object.entries(node.attributes || {});
  const fields = attributes.map(([attribute, value]) => renderAttributeControl(node.path, attribute, value, { tag: node.tag })).join("");
  const children = (node.children || []).map((child) => renderSettingsNode(child, { ...options, heading: true })).join("");
  const text = node.text ? renderTextControl(node.path, node.text, humanizeNativeName(node.tag)) : "";
  if (!fields && !children && !text) {
    return `<div class="settings-node" data-settings-tag="${escapeHtml(node.tag)}"><p class="field-help">${escapeHtml(humanizeNativeName(node.tag))} has no attributes or text in this task XML.</p></div>`;
  }
  return `<div class="settings-node" data-settings-tag="${escapeHtml(node.tag)}">${heading ? `<h4>${escapeHtml(humanizeNativeName(node.tag))}</h4>` : ""}${fields}${text}${children}</div>`;
}
