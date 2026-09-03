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
const PROJECTS_SCHEMA = "tc.sqx-custom-projects.v1";
const TOPOLOGY_SCHEMA = "tc.sqx-custom-project-topology.v1";
const SQX_BUILD = "144.2953";

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

function optionalString(value) {
  return typeof value === "string" && value ? value : null;
}

function optionalBool(value) {
  return typeof value === "boolean" ? value : null;
}

function optionalCount(value) {
  return value === null || value === undefined || (Number.isInteger(value) && value >= 0);
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
    || !Array.isArray(catalog.control.native_tools)
    || catalog.control.native_tools.some((tool) => tool !== "run_project" && tool !== "stop_project")
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
    ) {
      throw new Error("Native Custom Project task topology is invalid");
    }
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
  if (action !== "run_project" && action !== "stop_project") throw new Error("Native MCP action is invalid");
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

function selectedProjectName() {
  if (typeof globalThis.location === "undefined") return "";
  return projectName(new URLSearchParams(globalThis.location.search || "").get("project") || "");
}

function automationRoute() {
  return typeof globalThis.location !== "undefined" && globalThis.location.pathname === "/automation";
}

function field(label, value) {
  const shown = value || "";
  return `<label class="field-label">${escapeHtml(label)}<select class="idea-editor workflow-select" disabled aria-label="${escapeHtml(label)}"><option value="${escapeHtml(shown)}" selected>${escapeHtml(shown || "Unread in this archive")}</option></select></label>`;
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
        ${actionButton("Pause", { iconName: "pause", className: "button-icon", disabled: true, title: "Pause is not a retained SQX MCP tool" })}
        ${actionButton("Start", { primary: true, iconName: "play", className: "button-icon", attrs: `data-automation-control="run_project" data-project="${escapeHtml(project.name)}"`, title: catalog.control.detail, disabled: !canStart })}
      </div>
      <div class="workflow-meta"><span>${escapeHtml(market)}</span></div>
    </article>`;
  }).join("")}</div>`;
}

function taskLabel(task) {
  return task.name || task.kind;
}

export function renderTaskPipeline(topology) {
  if (!topology.tasks.length) {
    return '<p class="field-help">This saved native project contains no numbered tasks.</p>';
  }
  return `<ol class="task-pipeline" data-automation-task-pipeline>${topology.tasks.map((task, index) => {
    const active = task.active === false ? "is-off" : "is-on";
    const details = [
      task.kind,
      task.setup?.symbol && task.setup?.timeframe ? `${task.setup.symbol} ${task.setup.timeframe}` : "",
      task.clear_databanks?.length ? `Databanks: ${task.clear_databanks.join(", ")}` : "",
      task.goto_target_label ? `Go to ${task.goto_target_label}` : "",
    ].filter(Boolean).join(" · ");
    const connector = index < topology.tasks.length - 1
      ? `<li class="task-connector" aria-hidden="true"><span class="task-plus">${icon("plus", { size: 10 })}</span></li>`
      : "";
    return `<li class="task-step ${active}" data-native-project-task="${task.native_task_index}">
      <span class="task-index">${task.native_task_index}</span>
      <div><strong>${escapeHtml(taskLabel(task))}</strong><span>${escapeHtml(details)}</span></div>
      <span class="toggle ${task.active === false ? "" : "is-on"}" title="Native active flag" aria-hidden="true"></span>
    </li>${connector}`;
  }).join("")}</ol>`;
}

export function renderNativeSetup(setup, controlDetail) {
  if (!setup) {
    return unavailable("Native setup not present in this archive", "Engine, symbol, dates, money management, and cross-checks appear here when the saved task XML contains them.", { compact: true });
  }
  const checks = setup.cross_checks.length
    ? `<div class="cross-check-list">${setup.cross_checks.map((item) => `<div class="cross-check-row"><span>${escapeHtml(item.name)}</span><span class="toggle ${item.use === true ? "is-on" : ""}" title="Native use flag"></span></div>`).join("")}</div>`
    : '<p class="field-help">No named CrossChecks children in this task XML.</p>';
  const money = [setup.money_management_type, setup.money_management_size].filter(Boolean).join(", ") || "";
  return `<form class="native-setup" data-automation-native-setup>
    ${field("Engine", setup.engine)}
    ${field("Symbol", setup.symbol)}
    ${field("Timeframe", setup.timeframe)}
    ${field("Date from", setup.date_from)}
    ${field("Date to", setup.date_to)}
    ${field("Search", setup.generation_type)}
    ${field("Money management", money)}
    <div class="field-label">Cross checks</div>
    ${checks}
    <p class="note">${escapeHtml(controlDetail || "These are the live native values from the saved project. They belong in this desktop, not a second StrategyQuant X window.")}</p>
  </form>`;
}

function renderProgressPanel(topology, control, results) {
  const reason = control?.detail || readable(control?.reason_code, "Native MCP is not connected");
  const stats = renderProjectDatabankStats(results, topology.project);
  const streaming = unavailable(
    "Live task logs are not streaming",
    "Generated, rejected, accepted, and rate counts stay dashes until StrategyQuant X MCP streams them. Databank archives below are producer files from this saved project.",
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

export function renderWorkflowDetail(topology, control, results = null) {
  const reason = control?.detail || readable(control?.reason_code, "Native MCP is not connected");
  return `<div class="automation-detail" data-automation-project-detail="${escapeHtml(topology.project)}">
    <nav class="workflow-crumb">
      ${actionButton("All workflows", { iconName: "list", className: "button-small", attrs: "data-automation-back" })}
      <span>/</span>
      <strong>${escapeHtml(topology.project)}</strong>
    </nav>
    <div class="workflow-tabs" role="tablist">
      <span class="workflow-tab is-current">Progress</span>
      <span class="workflow-tab">Native setup</span>
      <a class="workflow-tab" href="/research?stage=backtest&amp;tab=overview" data-route="/research?stage=backtest&amp;tab=overview">Results</a>
    </div>
    <div class="automation-detail-grid">
      <section class="card accent-purple"><header class="card-head"><span class="card-icon tone-purple">${icon("list", { size: 15 })}</span><div class="card-titles"><h2>Task pipeline</h2><p>Native order from the saved Custom Project</p></div></header><div class="card-body">${renderTaskPipeline(topology)}</div></section>
      <section class="card accent-orange"><header class="card-head"><span class="card-icon tone-orange">${icon("play", { size: 15 })}</span><div class="card-titles"><h2>Progress</h2><p>One confirmed native MCP start or stop</p></div>${chip(readable(control?.reason_code, "Not connected"), "unavailable")}</header><div class="card-body">${renderProgressPanel(topology, control, results)}</div></section>
      <section class="card accent-blue"><header class="card-head"><span class="card-icon tone-blue">${icon("settings", { size: 15 })}</span><div class="card-titles"><h2>Native setup</h2><p>Engine, market, dates, and robustness flags from this project</p></div></header><div class="card-body">${renderNativeSetup(topology.native_setup, reason)}</div></section>
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
        detail = renderWorkflowDetail(topology, catalog.control, results);
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
  if (typeof globalThis.history === "undefined") return;
  globalThis.history.pushState({}, "", "/automation");
  boundHost = null;
  bindWorkspace();
}

function openProject(name) {
  const exact = projectName(name);
  if (!exact || typeof globalThis.history === "undefined") return;
  const url = `/automation?project=${encodeURIComponent(exact)}`;
  globalThis.history.pushState({}, "", url);
  boundHost = null;
  bindWorkspace();
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
    if (status) status.textContent = error instanceof Error ? error.message : "Native MCP refused the request.";
  } finally {
    button.disabled = false;
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
