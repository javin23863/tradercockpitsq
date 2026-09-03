import { researchPath } from "./model.mjs";
import {
  fetchCustomProjectResults,
  renderProjectDatabankList,
  renderProjectDatabankStats,
} from "./custom-project-results.mjs";
import {
  actionButton,
  chip,
  escapeHtml,
  icon,
  readable,
  statList,
  unavailable,
} from "./ui.mjs";

const SQX_PROJECTS_API_PATH = "/api/sqx-projects";
const SQX_PROJECT_TOPOLOGY_API_PATH = "/api/sqx-project-topology";
const SQX_PROJECT_CONTROL_API_PATH = "/api/sqx-project-control";
const SQX_PROJECT_SETTINGS_API_PATH = "/api/sqx-project-settings";
const PROJECTS_SCHEMA = "tc.sqx-custom-projects.v1";
const TOPOLOGY_SCHEMA = "tc.sqx-custom-project-topology.v1";
const SQX_BUILD = "144.2953";
const WORKFLOW_TABS = Object.freeze(["progress", "settings", "results"]);

function digest(value) {
  return typeof value === "string" && /^[0-9a-f]{64}$/.test(value) ? value : "";
}

function projectName(value) {
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

function object(value) {
  return value && typeof value === "object" && !Array.isArray(value) ? value : null;
}

function optionalCount(value) {
  return value === null || value === undefined || (Number.isInteger(value) && value >= 0);
}

export function humanizeNativeName(value) {
  return String(value || "")
    .replace(/_/g, " ")
    .replace(/([a-z0-9])([A-Z])/g, "$1 $2")
    .replace(/([A-Z]+)([A-Z][a-z])/g, "$1 $2")
    .replace(/\b\w/g, (letter) => letter.toUpperCase())
    .trim();
}

export function customProjectsCatalogFromPayload(payload) {
  const catalog = object(payload);
  if (
    !catalog
    || catalog.schema !== PROJECTS_SCHEMA
    || catalog.source_build !== SQX_BUILD
    || catalog.status !== "ready"
    || !Array.isArray(catalog.projects)
    || !object(catalog.control)
    || catalog.control.available !== false
    || typeof catalog.control.reason_code !== "string"
    || !catalog.control.reason_code
    || catalog.control.native_tools !== undefined
  ) {
    throw new Error("Native Custom Project catalog is invalid");
  }
  for (const project of catalog.projects) {
    const name = projectName(project?.name);
    if (
      !name
      || !["ready", "unresolved"].includes(project.status)
      || (project.status === "ready" && (!Number.isInteger(project.task_count) || project.task_count < 0 || !digest(project.archive_sha256)))
      || (project.status === "unresolved" && (typeof project.reason_code !== "string" || !project.reason_code))
      || !optionalCount(project.databank_count)
      || !optionalCount(project.strategy_count)
      || project.source_relative_path !== `user/projects/${name}/project.cfx`
    ) {
      throw new Error("Native Custom Project catalog item is invalid");
    }
  }
  return catalog;
}

function setupFromPayload(value) {
  if (value === null || value === undefined) return null;
  const setup = object(value);
  if (!setup) throw new Error("Native Custom Project setup is invalid");
  if (
    (setup.engine !== null && typeof setup.engine !== "string")
    || (setup.symbol !== null && typeof setup.symbol !== "string")
    || (setup.timeframe !== null && typeof setup.timeframe !== "string")
    || (setup.date_from !== null && typeof setup.date_from !== "string")
    || (setup.date_to !== null && typeof setup.date_to !== "string")
    || (setup.generation_type !== null && typeof setup.generation_type !== "string")
    || (setup.money_management_type !== null && typeof setup.money_management_type !== "string")
    || (setup.money_management_size !== null && typeof setup.money_management_size !== "string")
    || (setup.cross_checks_use !== null && typeof setup.cross_checks_use !== "boolean")
    || !Array.isArray(setup.cross_checks)
    || setup.cross_checks.some((item) => !item || typeof item.name !== "string" || !item.name || (item.use !== null && typeof item.use !== "boolean"))
  ) {
    throw new Error("Native Custom Project setup is invalid");
  }
  return setup;
}

function pathStepMatchesTag(step, tag) {
  return step === tag || new RegExp(`^${tag}:[1-9][0-9]*$`).test(step);
}

export function settingsNodeFromPayload(value) {
  const node = object(value);
  if (
    !node
    || typeof node.tag !== "string"
    || !/^[A-Za-z][A-Za-z0-9-]*$/.test(node.tag)
    || !object(node.attributes)
    || Object.entries(node.attributes).some(([key, item]) => !key || typeof item !== "string")
    || (node.text !== null && node.text !== undefined && typeof node.text !== "string")
    || !Array.isArray(node.path)
    || !node.path.length
    || node.path.some((part) => typeof part !== "string" || !/^[A-Za-z][A-Za-z0-9-]*(?::[1-9][0-9]*)?$/.test(part))
    || !pathStepMatchesTag(node.path[node.path.length - 1], node.tag)
    || !Array.isArray(node.children)
  ) {
    throw new Error("Native Custom Project settings node is invalid");
  }
  node.children.forEach(settingsNodeFromPayload);
  return node;
}

export function workflowTopologyFromPayload(payload) {
  const topology = object(payload);
  const project = projectName(topology?.project);
  if (
    !topology
    || topology.schema !== TOPOLOGY_SCHEMA
    || topology.source_build !== SQX_BUILD
    || !project
    || topology.source_relative_path !== `user/projects/${project}/project.cfx`
    || !digest(topology.archive_sha256)
    || !Array.isArray(topology.tasks)
    || topology.execution?.supported !== false
    || topology.execution?.reason !== "topology_custody_only"
  ) {
    throw new Error("Native Custom Project topology is invalid");
  }
  let previous = 0;
  for (const task of topology.tasks) {
    if (
      !task
      || !Number.isInteger(task.native_task_index)
      || task.native_task_index <= previous
      || typeof task.kind !== "string"
      || !/^[A-Za-z][A-Za-z0-9]*$/.test(task.kind)
      || task.entry_name !== `${task.kind}-Task${task.native_task_index}.xml`
      || (task.name !== null && task.name !== undefined && (typeof task.name !== "string" || !task.name))
      || (task.active !== null && task.active !== undefined && typeof task.active !== "boolean")
      || !Array.isArray(task.settings)
    ) {
      throw new Error("Native Custom Project task topology is invalid");
    }
    task.settings.forEach(settingsNodeFromPayload);
    setupFromPayload(task.setup);
    previous = task.native_task_index;
  }
  topology.native_setup = setupFromPayload(topology.native_setup);
  return topology;
}

async function readJson(response) {
  try { return await response.json(); } catch { return null; }
}

export async function fetchCustomProjectsCatalog(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Native project catalog fetch is unavailable");
  const response = await fetchImpl(SQX_PROJECTS_API_PATH, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || `Native project catalog request failed: ${response?.status ?? "unknown"}`);
  return customProjectsCatalogFromPayload(payload);
}

export async function fetchWorkflowTopology(project, fetchImpl = globalThis.fetch) {
  const exact = projectName(project);
  if (!exact) throw new Error("Exact native project name is required");
  if (typeof fetchImpl !== "function") throw new Error("Native project topology fetch is unavailable");
  const path = `${SQX_PROJECT_TOPOLOGY_API_PATH}?${new URLSearchParams({ project: exact }).toString()}`;
  const response = await fetchImpl(path, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || `Native project topology request failed: ${response?.status ?? "unknown"}`);
  return workflowTopologyFromPayload(payload);
}

export async function requestProjectControl(project, action, fetchImpl = globalThis.fetch) {
  const exact = projectName(project);
  if (!exact) throw new Error("Exact native project name is required");
  if (action !== "run_project" && action !== "stop_project") throw new Error("Native project action is invalid");
  if (typeof fetchImpl !== "function") throw new Error("Native project control is unavailable");
  const response = await fetchImpl(SQX_PROJECT_CONTROL_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({ project: exact, action }),
  });
  const payload = await readJson(response);
  if (response?.ok) return payload;
  throw new Error(payload?.detail || payload?.reason_code || `Native project control failed: ${response?.status ?? "unknown"}`);
}

export async function saveProjectSettings(project, task, updates, fetchImpl = globalThis.fetch) {
  const exact = projectName(project);
  if (!exact) throw new Error("Exact native project name is required");
  if (!Number.isInteger(task) || task < 1) throw new Error("Exact native task index is required");
  if (!Array.isArray(updates) || !updates.length) throw new Error("Settings updates must be a non-empty list");
  if (typeof fetchImpl !== "function") throw new Error("Native project settings write is unavailable");
  const response = await fetchImpl(SQX_PROJECT_SETTINGS_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({ project: exact, task, updates }),
  });
  const payload = await readJson(response);
  if (response?.ok) return payload;
  throw new Error(payload?.detail || payload?.reason_code || `Native project settings write failed: ${response?.status ?? "unknown"}`);
}

function searchParams() {
  if (typeof globalThis.location === "undefined") return new URLSearchParams();
  return new URLSearchParams(globalThis.location.search || "");
}

function selectedProjectName() {
  return projectName(searchParams().get("project") || "");
}

function selectedWorkflowTab() {
  const tab = searchParams().get("tab") || "progress";
  return WORKFLOW_TABS.includes(tab) ? tab : "progress";
}

function selectedTaskIndex(topology) {
  const raw = searchParams().get("task");
  const index = raw ? Number(raw) : NaN;
  if (Number.isInteger(index) && topology.tasks.some((task) => task.native_task_index === index)) {
    return index;
  }
  return topology.tasks[0]?.native_task_index ?? null;
}

function selectedSettingsSection(task) {
  const requested = searchParams().get("section") || "";
  const sections = (task?.settings || []).map((node) => node.tag);
  return sections.includes(requested) ? requested : (sections[0] || "");
}

function automationRoute() {
  return typeof globalThis.location !== "undefined" && globalThis.location.pathname === "/automation";
}

function workflowHref({ project = "", tab = "", task = "", section = "", method = "", methodPane = "" } = {}) {
  const params = new URLSearchParams();
  const exact = projectName(project);
  if (exact) params.set("project", exact);
  if (tab && tab !== "progress") params.set("tab", tab);
  if (task) params.set("task", String(task));
  if (section) params.set("section", section);
  if (method) params.set("method", method);
  if (method && methodPane) params.set("methodPane", methodPane);
  const query = params.toString();
  return query ? `/automation?${query}` : "/automation";
}

function workflowLink(label, attrs) {
  return `<button type="button" class="workflow-link" ${attrs}>${escapeHtml(label)}</button>`;
}

export function renderWorkflowList(catalog, selected = "") {
  if (!catalog.projects.length) {
    return unavailable(
      "No saved Custom Projects",
      "Verified StrategyQuant X has no Custom Project archives under user/projects yet. This desktop lists real native workflows; it does not invent asset-class rows.",
      { compact: true },
    );
  }
  return `<div class="workflow-list" data-automation-project-list>${catalog.projects.map((project) => {
    const current = project.name === selected;
    const taskLabel = Number.isInteger(project.task_count) ? `Tasks (${project.task_count})` : "Tasks (—)";
    const engineLabel = project.engine || "Engine unread";
    const databankLabel = Number.isInteger(project.databank_count) ? `Databanks (${project.databank_count})` : "Databanks (—)";
    const strategyLabel = Number.isInteger(project.strategy_count) ? `Strategies (${project.strategy_count})` : "Strategies (—)";
    const market = [project.symbol, project.timeframe].filter(Boolean).join(" ") || "Symbol unread";
    const unresolved = project.status === "unresolved"
      ? `<span class="workflow-warning">${icon("warn", { size: 14 })}<span>${escapeHtml(project.detail || readable(project.reason_code))}</span></span>`
      : "";
    const canStart = project.status === "ready";
    const resultsHref = researchPath("validate", "overview");
    return `<article class="workflow-row ${current ? "is-selected" : ""}" data-automation-project="${escapeHtml(project.name)}" data-project-status="${escapeHtml(project.status)}">
      <div class="workflow-copy">
        <strong>${escapeHtml(project.name)}</strong>
        <div class="workflow-nav">
          ${workflowLink(taskLabel, `data-automation-open="${escapeHtml(project.name)}"`)}
          ${workflowLink(engineLabel, `data-automation-open="${escapeHtml(project.name)}"`)}
          ${workflowLink(databankLabel, `data-automation-open="${escapeHtml(project.name)}"`)}
          <a class="workflow-link" href="${escapeHtml(resultsHref)}" data-route="${escapeHtml(resultsHref)}">${escapeHtml(strategyLabel)}</a>
        </div>
        ${unresolved}
      </div>
      <div class="workflow-progress" aria-hidden="true"><span></span></div>
      <div class="workflow-transport">
        ${actionButton("Stop", { iconName: "stop", className: "button-icon", attrs: `data-automation-control="stop_project" data-project="${escapeHtml(project.name)}"`, title: catalog.control.detail })}
        ${actionButton("Pause", { iconName: "pause", className: "button-icon", disabled: true, title: "Pause is not a native Custom Project control action" })}
        ${actionButton("Start", { primary: true, iconName: "play", className: "button-icon", attrs: `data-automation-control="run_project" data-project="${escapeHtml(project.name)}"`, title: catalog.control.detail, disabled: !canStart })}
      </div>
      <div class="workflow-meta"><span>${escapeHtml(market)}</span></div>
    </article>`;
  }).join("")}</div>`;
}

function taskLabel(task) {
  return task.name || task.kind;
}

function findNodesByTag(nodes, tag) {
  const found = [];
  for (const node of nodes || []) {
    if (node.tag === tag) found.push(node);
    found.push(...findNodesByTag(node.children, tag));
  }
  return found;
}

function sampleTypeLabel(sampleType) {
  const value = Number(sampleType);
  if (value === 127) return "full sample";
  if (value >= 10 && value < 20) return "in-sample";
  if (value >= 20 && value <= 30) return "out-of-sample";
  return sampleType ? `sample ${sampleType}` : "";
}

function firstChild(node, tag) {
  return (node?.children || []).find((child) => child.tag === tag) || null;
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
  return `<label class="field-label">${escapeHtml(name)}<input class="idea-editor workflow-input" data-settings-path="${encodedPath}" data-settings-text="1" value="${escapeHtml(value)}" aria-label="${escapeHtml(name)}" /></label>`;
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

export function renderRankingsPane(node, options = {}) {
  const conditions = firstChild(node, "Conditions");
  const others = (node.children || []).filter((child) => child.tag !== "Conditions");
  const fields = Object.entries(node.attributes || {}).map(([attribute, value]) => renderAttributeControl(node.path, attribute, value)).join("");
  const text = node.text ? renderTextControl(node.path, node.text, humanizeNativeName(node.tag)) : "";
  const otherHtml = others.map((child) => renderSettingsNode(child, { ...options, heading: true })).join("");
  return `<div class="settings-node" data-settings-tag="Rankings">${fields}${text}${otherHtml}${renderConditionTable(conditions)}</div>`;
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
    ${Object.entries(node.attributes || {}).map(([attribute, value]) => renderAttributeControl(node.path, attribute, value)).join("")}
    ${tabs}
    ${body}
  </div>`;
}

export function renderCrossChecksPane(node, { project = "", taskIndex = "", method = "", methodPane = "" } = {}) {
  if (method) {
    const methodNode = (node.children || []).find((child) => child.tag === method);
    if (methodNode) return renderCrossCheckMethodView(methodNode, { project, taskIndex, methodPane });
  }
  const fields = Object.entries(node.attributes || {}).map(([attribute, value]) => renderAttributeControl(node.path, attribute, value)).join("");
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
        ${Object.entries(child.attributes || {}).map(([attribute, value]) => renderAttributeControl(child.path, attribute, value)).join("")}
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

export function renderFullSettings(task, sectionTag = "", project = "", method = "", methodPane = "") {
  const sections = task?.settings || [];
  if (!sections.length) {
    return unavailable(
      "This task has no Full settings panes",
      "Full settings tabs are the Settings children in this task XML. This desktop does not invent Data, Ranking, or Building blocks panes.",
      { compact: true },
    );
  }
  const current = sections.find((node) => node.tag === sectionTag) || sections[0];
  const tabs = `<div class="settings-section-tabs" role="tablist">${sections.map((node) => {
    const currentTab = node.tag === current.tag;
    const href = workflowHref({
      project,
      tab: "settings",
      task: task.native_task_index,
      section: node.tag,
    });
    return `<a class="workflow-tab ${currentTab ? "is-current" : ""}" role="tab" aria-selected="${currentTab}" href="${escapeHtml(href)}" data-route="${escapeHtml(href)}" data-automation-section="${escapeHtml(node.tag)}">${escapeHtml(humanizeNativeName(node.tag))}</a>`;
  }).join("")}</div>`;
  const body = current.tag === "Rankings"
    ? renderRankingsPane(current, { project, taskIndex: task.native_task_index })
    : current.tag === "CrossChecks"
      ? renderCrossChecksPane(current, { project, taskIndex: task.native_task_index, method, methodPane })
      : renderSettingsNode(current, { heading: false, project, taskIndex: task.native_task_index, section: current.tag });
  return `<form class="full-settings" data-automation-settings-form data-settings-task="${task.native_task_index}">
    ${tabs}
    ${body}
    <div class="idea-actions">
      ${actionButton("Save settings", { primary: true, attrs: `data-automation-save-settings data-project-task="${task.native_task_index}"` })}
    </div>
    <p class="idea-save-status" data-automation-settings-status></p>
    <p class="field-help">Only attributes or existing text on this native element can be written. This desktop does not invent SQX parameters, Condition rows, or What-If scenarios.</p>
  </form>`;
}

export function renderProgressSummary(task, project = "") {
  const settings = task?.settings || [];
  if (!settings.length) {
    return unavailable(
      "No adjustable settings on this task",
      "Progress summary shows existing native attributes from the selected task. Choose a Build or Retest task, or open Full settings.",
      { compact: true },
    );
  }
  const preferred = ["Setup", "Chart", "BuildMode", "MoneyManagement", "CrossChecks"];
  const blocks = [];
  const options = { project, taskIndex: task.native_task_index };
  for (const tag of preferred) {
    for (const node of findNodesByTag(settings, tag)) {
      blocks.push(renderSettingsNode(node, options));
    }
  }
  if (!blocks.length) {
    blocks.push(settings.map((node) => renderSettingsNode(node, { ...options, heading: true })).join(""));
  }
  return `<form class="native-setup" data-automation-settings-form data-settings-task="${task.native_task_index}">
    ${blocks.join("")}
    <div class="idea-actions">
      ${actionButton("Save settings", { primary: true, attrs: `data-automation-save-settings data-project-task="${task.native_task_index}"` })}
    </div>
    <p class="idea-save-status" data-automation-settings-status></p>
    <p class="note">These are live native values from the saved task XML. Change them here; this desktop writes existing attributes or text back into the project.</p>
  </form>`;
}

export function renderTaskPipeline(topology, selectedTask = null) {
  if (!topology.tasks.length) {
    return '<p class="field-help">This saved native project contains no numbered tasks.</p>';
  }
  return `<ol class="task-pipeline" data-automation-task-pipeline>${topology.tasks.map((task, index) => {
    const active = task.active === false ? "is-off" : "is-on";
    const selected = task.native_task_index === selectedTask ? "is-selected" : "";
    const details = [
      task.kind,
      task.setup?.symbol && task.setup?.timeframe ? `${task.setup.symbol} ${task.setup.timeframe}` : "",
      task.clear_databanks?.length ? `Databanks: ${task.clear_databanks.join(", ")}` : "",
      task.goto_target_label ? `Go to ${task.goto_target_label}` : "",
      task.settings?.length ? `Settings (${task.settings.length})` : "",
    ].filter(Boolean).join(" · ");
    const connector = index < topology.tasks.length - 1
      ? `<li class="task-connector" aria-hidden="true"><span class="task-plus">${icon("plus", { size: 10 })}</span></li>`
      : "";
    const canToggle = task.active === true || task.active === false;
    return `<li class="task-step ${active} ${selected}" data-native-project-task="${task.native_task_index}" data-automation-select-task="${task.native_task_index}">
      <span class="task-index">${task.native_task_index}</span>
      <div><strong>${escapeHtml(taskLabel(task))}</strong><span>${escapeHtml(details)}</span></div>
      <div class="task-tools">
        <button type="button" class="task-gear" data-automation-task-settings="${task.native_task_index}" title="Full settings for this task" aria-label="Full settings">${icon("settings", { size: 14 })}</button>
        <button type="button" class="toggle ${task.active === false ? "" : "is-on"}" data-automation-task-active="${task.native_task_index}" ${canToggle ? "" : "disabled"} title="Native active flag" aria-pressed="${task.active !== false}"></button>
      </div>
    </li>${connector}`;
  }).join("")}</ol>`;
}

export function renderNativeSetup(task) {
  return renderProgressSummary(task);
}

function renderProgressPanel(topology, control, results) {
  const reason = control?.detail || readable(control?.reason_code, "Native Custom Project launch is not wired");
  const stats = renderProjectDatabankStats(results, topology.project);
  const streaming = unavailable(
    "Live task logs are not streaming",
    "Generated, rejected, accepted, and rate counts stay dashes until a native Custom Project run writes them. Databank archives below are producer files from this saved project.",
    { compact: true },
  );
  return `<div class="workflow-progress-panel">
    ${streaming}
    ${statList(stats)}
    ${renderProjectDatabankList(results, topology.project)}
    <div class="idea-actions">
      ${actionButton("Stop", { iconName: "stop", attrs: `data-automation-control="stop_project" data-project="${escapeHtml(topology.project)}"`, title: reason })}
      ${actionButton("Start project", { primary: true, iconName: "play", attrs: `data-automation-control="run_project" data-project="${escapeHtml(topology.project)}"`, title: reason })}
    </div>
    <p class="idea-save-status" data-automation-control-status></p>
    <p class="field-help">Archive ${escapeHtml(topology.archive_sha256.slice(0, 12))}… · ${escapeHtml(topology.source_relative_path)}</p>
  </div>`;
}

function renderResultsPanel(topology, results) {
  return `<div class="workflow-progress-panel">
    ${statList(renderProjectDatabankStats(results, topology.project))}
    ${renderProjectDatabankList(results, topology.project)}
    <p class="field-help">Results are producer databank archives from this saved Custom Project. This desktop does not invent P&amp;L.</p>
  </div>`;
}

function renderWorkflowTabs(topology, tab, taskIndex, section) {
  const items = [
    ["progress", "Progress"],
    ["settings", "Full settings"],
    ["results", "Results"],
  ];
  return `<div class="workflow-tabs" role="tablist">${items.map(([id, label]) => {
    const current = id === tab;
    const href = workflowHref({ project: topology.project, tab: id, task: taskIndex, section: id === "settings" ? section : "" });
    return `<a class="workflow-tab ${current ? "is-current" : ""}" role="tab" aria-selected="${current}" href="${escapeHtml(href)}" data-automation-tab="${id}">${escapeHtml(label)}</a>`;
  }).join("")}</div>`;
}

export function renderWorkflowDetail(topology, control, results = null, view = {}) {
  const tab = WORKFLOW_TABS.includes(view.tab) ? view.tab : "progress";
  const taskIndex = Number.isInteger(view.task) ? view.task : (topology.tasks[0]?.native_task_index ?? null);
  const task = topology.tasks.find((item) => item.native_task_index === taskIndex) || topology.tasks[0] || null;
  const section = view.section || selectedSettingsSection(task);
  const method = view.method || "";
  const methodPane = view.methodPane || "";
  const reason = control?.detail || readable(control?.reason_code, "Native Custom Project launch is not wired");
  let main = "";
  let side = "";
  if (tab === "settings") {
    main = renderFullSettings(task, section, topology.project, method, methodPane);
    side = `<p class="field-help">Select a task on the left. Full settings panes come from that task’s Settings children, including nested Ranking conditions and Cross-check Settings/Filtering when those subtrees exist in the saved XML.</p>`;
  } else if (tab === "results") {
    main = renderResultsPanel(topology, results);
  } else {
    main = renderProgressPanel(topology, control, results);
    side = renderProgressSummary(task, topology.project);
  }
  return `<div class="automation-detail" data-automation-project-detail="${escapeHtml(topology.project)}" data-automation-tab="${escapeHtml(tab)}">
    <nav class="workflow-crumb">
      ${actionButton("All workflows", { iconName: "list", className: "button-small", attrs: "data-automation-back" })}
      <span>/</span>
      <strong>${escapeHtml(topology.project)}</strong>
    </nav>
    ${renderWorkflowTabs(topology, tab, taskIndex, section)}
    <div class="automation-detail-grid">
      <section class="card accent-purple"><header class="card-head"><span class="card-icon tone-purple">${icon("list", { size: 15 })}</span><div class="card-titles"><h2>Task pipeline</h2><p>Native order from the saved Custom Project</p></div></header><div class="card-body">${renderTaskPipeline(topology, taskIndex)}</div></section>
      <section class="card accent-orange"><header class="card-head"><span class="card-icon tone-orange">${icon(tab === "settings" ? "settings" : "play", { size: 15 })}</span><div class="card-titles"><h2>${escapeHtml(tab === "settings" ? "Full settings" : tab === "results" ? "Results" : "Progress")}</h2><p>${escapeHtml(tab === "settings" ? "Exact native attributes and text from this task XML" : tab === "results" ? "Producer databank archives" : "Native run log and databanks")}</p></div>${tab === "progress" ? chip(readable(control?.reason_code, "Launch unwired"), "unavailable") : ""}</header><div class="card-body">${main}</div></section>
      <section class="card accent-blue"><header class="card-head"><span class="card-icon tone-blue">${icon("settings", { size: 15 })}</span><div class="card-titles"><h2>${escapeHtml(tab === "settings" ? "Task" : "Settings")}</h2><p>${escapeHtml(task ? taskLabel(task) : "No task selected")}</p></div></header><div class="card-body">${side || `<p class="note">${escapeHtml(reason)}</p>`}</div></section>
    </div>
  </div>`;
}

function host() {
  if (!automationRoute()) return null;
  return document.querySelector("[data-automation-workflows]");
}

let generation = 0;
let boundHost = null;

function renderShell(inner) {
  return inner;
}

function navigate(url) {
  if (typeof globalThis.history === "undefined") return;
  globalThis.history.pushState({}, "", url);
  boundHost = null;
  bindWorkspace();
}

async function loadWorkspace(root) {
  const myGeneration = ++generation;
  const selected = selectedProjectName();
  root.dataset.automationWorkflows = "loading";
  root.innerHTML = unavailable("Loading native workflows…", "Reading saved Custom Projects from the verified StrategyQuant X runtime.", { tone: "pending", compact: true });
  try {
    const catalog = await fetchCustomProjectsCatalog();
    if (myGeneration !== generation || !root.isConnected) return;
    const list = renderWorkflowList(catalog, selected);
    let detail = "";
    if (selected) {
      try {
        const topology = await fetchWorkflowTopology(selected);
        if (myGeneration !== generation || !root.isConnected) return;
        let results = null;
        try {
          results = await fetchCustomProjectResults(selected);
        } catch {
          results = null;
        }
        if (myGeneration !== generation || !root.isConnected) return;
        detail = renderWorkflowDetail(topology, catalog.control, results, {
          tab: selectedWorkflowTab(),
          task: selectedTaskIndex(topology),
          section: searchParams().get("section") || "",
          method: searchParams().get("method") || "",
          methodPane: searchParams().get("methodPane") || "",
        });
      } catch (error) {
        detail = `<nav class="workflow-crumb">${actionButton("All workflows", { iconName: "list", className: "button-small", attrs: "data-automation-back" })}</nav>${unavailable("Could not open this project", error instanceof Error ? error.message : "Native topology unavailable", { compact: true, tone: "error" })}`;
      }
    }
    root.dataset.automationWorkflows = "loaded";
    root.innerHTML = renderShell(selected
      ? detail
      : `<p class="idea-save-status" data-automation-control-status></p>${list}`);
  } catch (error) {
    if (myGeneration !== generation || !root.isConnected) return;
    root.dataset.automationWorkflows = "failed";
    root.innerHTML = unavailable(
      "Native workflows unavailable",
      error instanceof Error ? error.message : "Custom Project catalog could not be read.",
      { compact: true, tone: "error" },
    );
  }
}

function bindWorkspace() {
  const root = host();
  if (!root || root === boundHost) return;
  boundHost = root;
  void loadWorkspace(root);
}

function showList() {
  navigate("/automation");
}

function openProject(name, extras = {}) {
  const exact = projectName(name);
  if (!exact) return;
  navigate(workflowHref({ project: exact, ...extras }));
}

function collectSettingsUpdates(root) {
  return [...root.querySelectorAll("[data-settings-attribute], [data-settings-text]")].map((element) => {
    const path = JSON.parse(element.getAttribute("data-settings-path") || "[]");
    if (element.hasAttribute("data-settings-text")) {
      return { path, text: String(element.value ?? "") };
    }
    const attribute = element.getAttribute("data-settings-attribute") || "";
    const value = element.matches("[data-settings-kind='flag']")
      ? (element.classList.contains("is-on") ? "true" : "false")
      : String(element.value ?? "");
    return { path, attribute, value };
  }).filter((item) => item.path.length && (item.attribute || item.text !== undefined));
}

async function controlProject(button, action) {
  const project = projectName(button.getAttribute("data-project") || "");
  const status = document.querySelector("[data-automation-control-status]");
  button.disabled = true;
  if (status) status.textContent = `Requesting native ${action}…`;
  try {
    await requestProjectControl(project, action);
    if (status) status.textContent = action === "stop_project" ? "Native project stop requested." : "Native project is running.";
  } catch (error) {
    if (status) status.textContent = error instanceof Error ? error.message : "Native launch refused the request.";
  } finally {
    button.disabled = false;
  }
}

async function writeSettings(project, task, updates, statusNode) {
  if (statusNode) statusNode.textContent = "Writing native settings…";
  try {
    await saveProjectSettings(project, task, updates);
    if (statusNode) statusNode.textContent = "Saved existing native attributes or text.";
    boundHost = null;
    bindWorkspace();
  } catch (error) {
    if (statusNode) statusNode.textContent = error instanceof Error ? error.message : "Native settings write refused.";
  }
}

if (typeof document !== "undefined") {
  document.addEventListener("click", (event) => {
    if (!automationRoute()) return;
    const back = event.target.closest?.("[data-automation-back]");
    if (back) {
      event.preventDefault();
      showList();
      return;
    }
    const open = event.target.closest?.("[data-automation-open]");
    if (open) {
      event.preventDefault();
      openProject(open.getAttribute("data-automation-open") || "");
      return;
    }
    const tab = event.target.closest?.("a.workflow-tab[data-automation-tab], button.workflow-tab[data-automation-tab]");
    if (tab) {
      event.preventDefault();
      const project = selectedProjectName();
      openProject(project, {
        tab: tab.getAttribute("data-automation-tab") || "progress",
        task: searchParams().get("task") || "",
        section: tab.getAttribute("data-automation-tab") === "settings" ? (searchParams().get("section") || "") : "",
      });
      return;
    }
    const method = event.target.closest?.("[data-automation-method]");
    if (method) {
      event.preventDefault();
      openProject(selectedProjectName(), {
        tab: "settings",
        task: searchParams().get("task") || "",
        section: "CrossChecks",
        method: method.getAttribute("data-automation-method") || "",
        methodPane: method.getAttribute("data-automation-method-pane") || "",
      });
      return;
    }
    const section = event.target.closest?.("[data-automation-section]");
    if (section) {
      event.preventDefault();
      openProject(selectedProjectName(), {
        tab: "settings",
        task: searchParams().get("task") || "",
        section: section.getAttribute("data-automation-section") || "",
      });
      return;
    }
    const taskSettings = event.target.closest?.("[data-automation-task-settings]");
    if (taskSettings) {
      event.preventDefault();
      event.stopPropagation();
      openProject(selectedProjectName(), {
        tab: "settings",
        task: taskSettings.getAttribute("data-automation-task-settings") || "",
      });
      return;
    }
    const taskActive = event.target.closest?.("[data-automation-task-active]");
    if (taskActive && !taskActive.disabled) {
      event.preventDefault();
      event.stopPropagation();
      const task = Number(taskActive.getAttribute("data-automation-task-active"));
      const next = taskActive.classList.contains("is-on") ? "false" : "true";
      const status = document.querySelector("[data-automation-settings-status]") || document.querySelector("[data-automation-control-status]");
      void writeSettings(selectedProjectName(), task, [{ target: "config", attribute: "active", value: next }], status);
      return;
    }
    const selectTask = event.target.closest?.("[data-automation-select-task]");
    if (selectTask) {
      event.preventDefault();
      openProject(selectedProjectName(), {
        tab: selectedWorkflowTab(),
        task: selectTask.getAttribute("data-automation-select-task") || "",
        section: searchParams().get("section") || "",
      });
      return;
    }
    const save = event.target.closest?.("[data-automation-save-settings]");
    if (save) {
      event.preventDefault();
      const form = save.closest("[data-automation-settings-form]");
      const task = Number(save.getAttribute("data-project-task"));
      const updates = form ? collectSettingsUpdates(form) : [];
      const status = form?.querySelector("[data-automation-settings-status]") || document.querySelector("[data-automation-settings-status]");
      if (!updates.length) {
        if (status) status.textContent = "No existing attributes or text to write on this pane.";
        return;
      }
      void writeSettings(selectedProjectName(), task, updates, status);
      return;
    }
    const flag = event.target.closest?.("[data-settings-kind='flag']");
    if (flag) {
      event.preventDefault();
      flag.classList.toggle("is-on");
      flag.setAttribute("aria-checked", flag.classList.contains("is-on") ? "true" : "false");
      return;
    }
    const control = event.target.closest?.("[data-automation-control]");
    if (control) {
      event.preventDefault();
      const action = control.getAttribute("data-automation-control") || "";
      if (action === "run_project" || action === "stop_project") {
        void controlProject(control, action);
      }
    }
  });
  const observer = new MutationObserver(() => {
    if (!automationRoute()) {
      generation += 1;
      boundHost = null;
      return;
    }
    bindWorkspace();
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  bindWorkspace();
}
