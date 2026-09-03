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

export function renderAttributeControl(path, attribute, value) {
  const encodedPath = escapeHtml(JSON.stringify(path));
  const name = humanizeNativeName(attribute);
  if (value === "true" || value === "false") {
    const on = value === "true";
    return `<div class="settings-row"><span>${escapeHtml(name)}</span><button type="button" class="toggle ${on ? "is-on" : ""}" role="switch" aria-checked="${on}" data-settings-path="${encodedPath}" data-settings-attribute="${escapeHtml(attribute)}" data-settings-kind="flag" title="${escapeHtml(name)}"></button></div>`;
  }
  return `<label class="field-label">${escapeHtml(name)}<input class="idea-editor workflow-input" data-settings-path="${encodedPath}" data-settings-attribute="${escapeHtml(attribute)}" value="${escapeHtml(value)}" aria-label="${escapeHtml(name)}" /></label>`;
}

export function renderTextControl(path, value, label = "") {
  const encodedPath = escapeHtml(JSON.stringify(path));
  const name = label || humanizeNativeName(String(path[path.length - 1] || "").replace(/:\d+$/, ""));
  const long = String(value || "").length > 80 || String(value || "").includes("<");
  if (long) {
    return `<label class="field-label">${escapeHtml(name)}<textarea class="idea-editor workflow-input" data-settings-path="${encodedPath}" data-settings-text="1" aria-label="${escapeHtml(name)}">${escapeHtml(value)}</textarea></label>`;
  }
  return `<label class="field-label">${escapeHtml(name)}<input class="idea-editor workflow-input" data-settings-path="${encodedPath}" data-settings-text="1" value="${escapeHtml(value)}" aria-label="${escapeHtml(name)}" /></label>`;
}

export function renderNodeAttributes(node, skip = []) {
  return Object.entries(node?.attributes || {})
    .filter(([attribute]) => !skip.includes(attribute))
    .map(([attribute, value]) => renderAttributeControl(node.path, attribute, value))
    .join("");
}

function renderConditionRow(node) {
  const left = firstChild(firstChild(node, "Left-Side"), "Column-Value");
  const comparator = firstChild(node, "Comparator");
  const numeric = firstChild(firstChild(node, "Right-Side"), "Numeric-Value");
  const display = node.display || {};
  const column = left?.attributes?.column || display.column || "";
  const sample = sampleTypeLabel(left?.attributes?.sampleType) || display.sample || "";
  const comparatorValue = comparator?.attributes?.value || display.comparator || "";
  const rightValue = numeric?.attributes?.value ?? (display.threshold == null ? "" : String(display.threshold));
  const use = node.attributes?.use;
  return `<tr data-settings-tag="Condition">
    <td>${use === "true" || use === "false" ? renderAttributeControl(node.path, "use", use) : ""}</td>
    <td>${left ? renderAttributeControl(left.path, "column", column) : escapeHtml(column)}<span class="field-help">${escapeHtml(sample)}</span></td>
    <td>${comparator ? renderAttributeControl(comparator.path, "value", comparatorValue) : escapeHtml(comparatorValue)}</td>
    <td>${numeric ? renderAttributeControl(numeric.path, "value", rightValue) : escapeHtml(rightValue)}</td>
  </tr>`;
}

export function renderConditionTable(conditionsNode) {
  const rows = (conditionsNode?.children || []).filter((child) => child.tag === "Condition");
  if (!rows.length) {
    return `<p class="field-help">This saved task has no Ranking or filter Condition rows.</p>`;
  }
  return `<table class="settings-condition-table">
    <thead><tr><th>Use</th><th>Column</th><th>Comparator</th><th>Value</th></tr></thead>
    <tbody>${rows.map(renderConditionRow).join("")}</tbody>
  </table>`;
}

export function renderFieldGroup(title, body) {
  if (!body) return "";
  return `<section class="settings-group" data-settings-group="${escapeHtml(title)}"><h4>${escapeHtml(title)}</h4>${body}</section>`;
}

export function renderRankingsPane(node, options = {}) {
  const fields = renderNodeAttributes(node);
  const text = node.text ? renderTextControl(node.path, node.text, humanizeNativeName(node.tag)) : "";
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
  return `<div class="settings-node" data-settings-tag="Rankings">${fields}${text}
    ${renderFieldGroup("Databank max", databank ? renderSettingsNode(databank, { ...options, heading: false }) : "")}
    ${renderFieldGroup("Stop condition", stop ? renderSettingsNode(stop, { ...options, heading: false }) : "")}
    ${renderFieldGroup("Fitness", fitness ? renderSettingsNode(fitness, { ...options, heading: false }) : "")}
    ${renderFieldGroup("Automatic dismissal", `${dismissSimilar ? renderSettingsNode(dismissSimilar, { ...options, heading: false }) : ""}${automatic ? renderSettingsNode(automatic, { ...options, heading: false }) : ""}`)}
    ${renderFieldGroup("Custom filter", `${conditionsType ? renderSettingsNode(conditionsType, { ...options, heading: false }) : ""}${conditions ? renderConditionTable(conditions) : ""}`)}
    ${rest.map((child) => renderSettingsNode(child, { ...options, heading: true })).join("")}
  </div>`;
}

function methodSummary(node) {
  const settings = firstChild(node, "Settings");
  const acceptance = firstChild(node, "AcceptanceSettings");
  const bits = nestedTextAndAttrs(settings);
  const filters = conditionCount(acceptance);
  if (acceptance) bits.push(`${filters} filter${filters === 1 ? "" : "s"}`);
  if (!bits.length) {
    return settings || acceptance
      ? "Nested settings present in this saved task"
      : "No nested settings in this saved task";
  }
  return bits.join(" · ");
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

export function renderCrossChecksPane(node, { project = "", taskIndex = "", method = "", methodPane = "" } = {}) {
  if (method) {
    const methodNode = (node.children || []).find((child) => child.tag === method);
    if (methodNode) return renderCrossCheckMethodView(methodNode, { project, taskIndex, methodPane });
  }
  const fields = renderNodeAttributes(node);
  const rows = (node.children || []).map((child) => {
    const nested = firstChild(child, "Settings") || firstChild(child, "AcceptanceSettings");
    const href = workflowHref({
      project,
      tab: "settings",
      task: taskIndex,
      section: "CrossChecks",
      method: child.tag,
    });
    const open = nested
      ? `<a class="button-small" href="${escapeHtml(href)}" data-route="${escapeHtml(href)}" data-automation-method="${escapeHtml(child.tag)}">Open</a>`
      : "";
    return `<div class="cross-check-method" data-settings-tag="${escapeHtml(child.tag)}">
      <div>
        <strong>${escapeHtml(humanizeNativeName(child.tag))}</strong>
        <p class="cross-check-method-summary">${escapeHtml(methodSummary(child))}</p>
      </div>
      <div class="cross-check-method-tools">
        ${renderNodeAttributes(child)}
        ${open}
      </div>
    </div>`;
  }).join("");
  return `<div class="settings-node" data-settings-tag="CrossChecks">${fields}<div class="cross-check-list">${rows}</div></div>`;
}

export function renderSettingsNode(node, options = {}) {
  const heading = options.heading !== false;
  if (node.tag === "Rankings") return renderRankingsPane(node, options);
  if (node.tag === "CrossChecks") return renderCrossChecksPane(node, options);
  if (node.tag === "Conditions") {
    return `<div class="settings-node" data-settings-tag="Conditions">${heading ? `<h4>Conditions</h4>` : ""}${renderConditionTable(node)}</div>`;
  }
  const attributes = Object.entries(node.attributes || {});
  const fields = attributes.map(([attribute, value]) => renderAttributeControl(node.path, attribute, value)).join("");
  const children = (node.children || []).map((child) => renderSettingsNode(child, { ...options, heading: true })).join("");
  const text = node.text ? renderTextControl(node.path, node.text, humanizeNativeName(node.tag)) : "";
  if (!fields && !children && !text) {
    return `<div class="settings-node" data-settings-tag="${escapeHtml(node.tag)}"><p class="field-help">${escapeHtml(humanizeNativeName(node.tag))} has no attributes or text in this task XML.</p></div>`;
  }
  return `<div class="settings-node" data-settings-tag="${escapeHtml(node.tag)}">${heading ? `<h4>${escapeHtml(humanizeNativeName(node.tag))}</h4>` : ""}${fields}${text}${children}</div>`;
}
