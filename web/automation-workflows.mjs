import {
  fetchCustomProjectResults,
  projectResultsOf,
  renderProjectDatabankList,
} from "./custom-project-results.mjs";
import {
  documentedSettingsTabs,
  humanizeNativeName,
  renderCrossChecksPane,
  renderFullSettings,
  renderRankingsPane,
  renderSettingsNode,
  workflowHref,
} from "./automation-full-settings.mjs";
import { CUSTOM_PROJECTS_PATH, RUN_MODULE_PATHS, currentWorkflowPath, filterSqxDataBox, findNodesByTag, nativeChoicesFor, resetDateUpdates, setOfficialSqxChoices, symbolChangeUpdates } from "./automation-settings-controls.mjs";
import { fetchSqxModule } from "./sqx-modules.mjs";
import {
  bindDatabankGrid,
  bindResultsChrome,
  createResultsPluginTab,
  fetchProjectStrategy,
  renderResultsPanel,
} from "./automation-results.mjs";
import {
  actionButton,
  chartFrame,
  escapeHtml,
  icon,
  readable,
  unavailable,
} from "./ui.mjs";

export {
  humanizeNativeName,
  nativeChoicesFor,
  renderCrossChecksPane,
  renderFullSettings,
  renderRankingsPane,
};

const SQX_PROJECTS_API_PATH = "/api/sqx-projects";
const SQX_PROJECT_TOPOLOGY_API_PATH = "/api/sqx-project-topology";
const SQX_PROJECT_CONTROL_API_PATH = "/api/sqx-project-control";
const SQX_PROJECT_SETTINGS_API_PATH = "/api/sqx-project-settings";
const SQX_CALIBRATE_API_PATH = "/api/sqx-calibrate";
const SQX_BUILD_TYPE_FILES_API_PATH = "/api/sqx-build-type-files";
const SQX_BUILD_TYPE_TEMPLATE_API_PATH = "/api/sqx-build-type-template";
const SQX_RANKING_FITNESS_API_PATH = "/api/sqx-ranking-fitness-types";
const SQX_INSTALLED_DATA_API_PATH = "/api/sqx-installed-data";
const SQX_COMMISSION_METHODS_API_PATH = "/api/sqx-commission-methods";
const SQX_SYMBOL_DATA_API_PATH = "/api/sqx-symbol-data";
const SQX_PROJECT_PROGRESS_API_PATH = "/api/sqx-project-progress";
const SQX_ENGINE_CHART_SELECTION_API_PATH = "/api/sqx-engine-chart-selection";
export const SQX_PROGRESS_POLL_MS = 2000;
const startInFlight = new Set();
const PROJECTS_SCHEMA = "tc.sqx-custom-projects.v1";
const TOPOLOGY_SCHEMA = "tc.sqx-custom-project-topology.v1";
const PROGRESS_SCHEMA = "tc.sqx-custom-project-progress.v1";
const SQX_BUILD = "144.2953";
const WORKFLOW_TABS = Object.freeze(["progress", "settings", "results"]);
const PROJECT_DISPLAY_NAMES = Object.freeze({
  "DJ CFD - Dukascopy": "Indices Template",
  "EW FUTURES BREAKOUT H1 - Tradestation": "Futures Template H1 Breakout",
  "GBPJPY BREAKOUT H1 - Dukascopy": "Forex Template H1 Breakout",
  "GBPJPY BREAKOUT H4 - Dukascopy": "Forex Template H4 Breakout",
  "GBPUSD H1 - Dukascopy": "Forex Template H1",
  "GOLD BREAKOUT M30 - Dukascopy": "Gold Template H1 Breakout",
  "GOLD H1 CFD - Dukascopy": "Gold indices Template  H1",
  "NQ BREAKOUT FUTURES  H1 - Tradestation": "Futures Template H1 Breakout",
  "NQ CFD H1 - Dukascopy": "Indices Template Futures H1",
  "NQ CFD H1 D1 MULTI-TIMEFRAME  - Dukascopy": "Indices Futures H1 D1 Multi TimeFrame",
});

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

function projectDisplayName(name) {
  return PROJECT_DISPLAY_NAMES[name] || name;
}

function object(value) {
  return value && typeof value === "object" && !Array.isArray(value) ? value : null;
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
    || typeof catalog.control.available !== "boolean"
    || (catalog.control.available
      ? catalog.control.reason_code != null && catalog.control.reason_code !== ""
      : typeof catalog.control.reason_code !== "string" || !catalog.control.reason_code)
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
      || (project.running !== undefined && typeof project.running !== "boolean")
      || !optionalPercent(project.percent)
      || !optionalRunningStatus(project.running_status)
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
    || typeof topology.execution?.supported !== "boolean"
    || (topology.execution.supported
      ? topology.execution.reason !== "native_cli"
      : topology.execution.reason !== "topology_custody_only")
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
      || (task.title !== null && task.title !== undefined && (typeof task.title !== "string" || !task.title))
      || (task.active !== null && task.active !== undefined && typeof task.active !== "boolean")
      || (task.input_databanks != null && (!Array.isArray(task.input_databanks) || task.input_databanks.some((value) => typeof value !== "string" || !value)))
      || (task.output_databanks != null && (!Array.isArray(task.output_databanks) || task.output_databanks.some((value) => typeof value !== "string" || !value)))
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
  const params = new URLSearchParams({ project: exact });
  const section = searchParams().get("section") || "";
  const block = searchParams().get("block") || "";
  if (section === "Blocks" || block) params.set("blocks", "1");
  if (block) params.set("block", block);
  const path = `${SQX_PROJECT_TOPOLOGY_API_PATH}?${params.toString()}`;
  const response = await fetchImpl(path, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || `Native project topology request failed: ${response?.status ?? "unknown"}`);
  return workflowTopologyFromPayload(payload);
}

function optionalPercent(value) {
  return value === undefined || (Number.isInteger(value) && value >= 0 && value <= 100);
}

function optionalRunningStatus(value) {
  return value === undefined || value === null || (typeof value === "string" && Boolean(value.trim()));
}

function optionalChartValues(values) {
  return Array.isArray(values)
    && values.length > 1
    && values.length <= 256
    && values.every((point) => typeof point === "number" && Number.isFinite(point));
}

function optionalChartItems(value) {
  return Array.isArray(value)
    && value.length <= 32
    && value.every((row) => object(row)
      && typeof row.name === "string"
      && row.name
      && typeof row.value === "string");
}

function optionalCharts(value) {
  if (value === undefined) return true;
  if (!Array.isArray(value) || value.length > 2) return false;
  return value.every((frame) => {
    if (
      !object(frame)
      || typeof frame.type !== "string"
      || !frame.type
      || typeof frame.title !== "string"
      || !frame.title
    ) return false;
    if (frame.kind === "grid" || frame.kind === "rows") return optionalChartItems(frame.items);
    const series = frame.series;
    return Array.isArray(series)
      && series.length <= 8
      && series.every((line) => object(line)
        && optionalChartValues(line.values)
        && (line.label === undefined || (typeof line.label === "string" && Boolean(line.label))));
  });
}

function optionalChartCatalog(progress) {
  const types = progress?.chart_types;
  const settings = progress?.chart_settings;
  if (types === undefined && settings === undefined) return true;
  if (!Array.isArray(types) || !types.length || types.length > 32) return false;
  if (!types.every((item) => object(item) && typeof item.type === "string" && item.type && typeof item.name === "string" && item.name)) {
    return false;
  }
  if (!Array.isArray(settings) || settings.length !== 2) return false;
  const allowed = new Set(types.map((item) => item.type));
  return settings.every((type) => typeof type === "string" && allowed.has(type));
}

export function projectProgressFromPayload(payload) {
  const progress = object(payload);
  const project = projectName(progress?.project);
  if (
    !progress
    || progress.schema !== PROGRESS_SCHEMA
    || progress.source_build !== SQX_BUILD
    || !project
    || progress.source_relative_path !== `user/projects/${project}/project.cfx`
    || typeof progress.running !== "boolean"
    || typeof progress.worker_label !== "string"
    || !progress.worker_label
    || !optionalCount(progress.generated)
    || !optionalCount(progress.rejected)
    || !optionalCount(progress.accepted)
    || !optionalCount(progress.rate)
    || !optionalPercent(progress.percent)
    || !optionalRunningStatus(progress.running_status)
    || !optionalCount(progress.databank_count)
    || !optionalCount(progress.strategy_count)
    || !optionalCharts(progress.charts)
    || !optionalChartCatalog(progress)
    || !Array.isArray(progress.log_lines)
  ) {
    throw new Error("Native Custom Project progress is invalid");
  }
  for (const line of progress.log_lines) {
    if (
      !object(line)
      || typeof line.relative_path !== "string"
      || !line.relative_path
      || line.relative_path.includes("\\")
      || line.relative_path.includes("\0")
      || typeof line.text !== "string"
    ) {
      throw new Error("Native Custom Project progress log is invalid");
    }
  }
  return progress;
}

export async function fetchProjectProgress(project, fetchImpl = globalThis.fetch) {
  const exact = projectName(project);
  if (!exact) throw new Error("Exact native project name is required");
  if (typeof fetchImpl !== "function") throw new Error("Native project progress fetch is unavailable");
  const path = `${SQX_PROJECT_PROGRESS_API_PATH}?${new URLSearchParams({ project: exact }).toString()}`;
  const response = await fetchImpl(path, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || `Native project progress request failed: ${response?.status ?? "unknown"}`);
  return projectProgressFromPayload(payload);
}

export async function saveEngineChartSelection(project, number, type, fetchImpl = globalThis.fetch) {
  const exact = projectName(project);
  if (!exact) throw new Error("Exact native project name is required");
  if (number !== 0 && number !== 1) throw new Error("Chart slot number must be 0 or 1");
  if (typeof type !== "string" || !type.trim()) throw new Error("Official engine chart type is required");
  if (typeof fetchImpl !== "function") throw new Error("Native engine chart selection is unavailable");
  const response = await fetchImpl(SQX_ENGINE_CHART_SELECTION_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({ project: exact, number, type: type.trim() }),
  });
  const payload = await readJson(response);
  if (response?.ok) return payload;
  throw new Error(payload?.detail || payload?.reason_code || `Native engine chart selection failed: ${response?.status ?? "unknown"}`);
}

export async function requestProjectControl(project, action, fetchImpl = globalThis.fetch) {
  const exact = projectName(project);
  if (!exact) throw new Error("Exact native project name is required");
  if (
    action !== "run_project"
    && action !== "stop_project"
    && action !== "pause_project"
    && action !== "resume_project"
  ) throw new Error("Native project action is invalid");
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

export async function fetchBuildTypeFiles(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Native build-type file list is unavailable");
  const response = await fetchImpl(SQX_BUILD_TYPE_FILES_API_PATH, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || payload?.reason_code || `Native build-type files failed: ${response?.status ?? "unknown"}`);
  if (!Array.isArray(payload?.templates) || !Array.isArray(payload?.strategies)) {
    throw new Error("Native build-type file list is invalid");
  }
  return payload;
}

export async function fetchInstalledDataSymbols(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Native installed-data list is unavailable");
  const response = await fetchImpl(SQX_INSTALLED_DATA_API_PATH, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || payload?.reason_code || `Native installed-data list failed: ${response?.status ?? "unknown"}`);
  if (!Array.isArray(payload?.symbols) || payload.symbols.some((name) => typeof name !== "string" || !name)) {
    throw new Error("Native installed-data list is invalid");
  }
  if (payload.sessions !== undefined && (!Array.isArray(payload.sessions) || payload.sessions.some((name) => typeof name !== "string" || !name))) {
    throw new Error("Native installed-data list is invalid");
  }
  if (payload.precisions !== undefined && (!Array.isArray(payload.precisions) || payload.precisions.some((row) => !row || typeof row.key !== "string" || typeof row.name !== "string"))) {
    throw new Error("Native installed-data list is invalid");
  }
  if (payload.rows !== undefined && (!Array.isArray(payload.rows) || payload.rows.some((row) => !row || typeof row.symbol !== "string"))) {
    throw new Error("Native installed-data list is invalid");
  }
  if (payload.dataTypes !== undefined && (!Array.isArray(payload.dataTypes) || payload.dataTypes.some((row) => !row || typeof row.key !== "string" || typeof row.name !== "string"))) {
    throw new Error("Native installed-data list is invalid");
  }
  if (payload.swapTypes !== undefined && (!Array.isArray(payload.swapTypes) || payload.swapTypes.some((name) => typeof name !== "string" || !name))) {
    throw new Error("Native installed-data list is invalid");
  }
  if (payload.tripleSwapOptions !== undefined && (!Array.isArray(payload.tripleSwapOptions) || payload.tripleSwapOptions.some((name) => typeof name !== "string" || !name))) {
    throw new Error("Native installed-data list is invalid");
  }
  return payload;
}

export async function fetchSymbolData(dateFrom, dateTo, symbol, session, fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Native symbol data is unavailable");
  const response = await fetchImpl(SQX_SYMBOL_DATA_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({ dateFrom, dateTo, symbol, session }),
  });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || payload?.reason_code || `Native symbol data failed: ${response?.status ?? "unknown"}`);
  if (!Array.isArray(payload?.points) || payload.points.some((row) => !Array.isArray(row) || row.length < 2 || !Number.isFinite(Number(row[1])))) {
    throw new Error("Native symbol data is invalid");
  }
  return payload;
}

export async function fetchCommissionMethods(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Native commission-method list is unavailable");
  const response = await fetchImpl(SQX_COMMISSION_METHODS_API_PATH, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || payload?.reason_code || `Native commission-method list failed: ${response?.status ?? "unknown"}`);
  if (!Array.isArray(payload?.methods) || !payload.methods.every((row) => row && typeof row.key === "string" && typeof row.name === "string")) {
    throw new Error("Native commission-method list is invalid");
  }
  return payload;
}

export async function fetchRankingFitnessTypes(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Native ranking fitness list is unavailable");
  const response = await fetchImpl(SQX_RANKING_FITNESS_API_PATH, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || payload?.reason_code || `Native ranking fitness types failed: ${response?.status ?? "unknown"}`);
  if (!Array.isArray(payload?.types) || !payload.types.every((row) => row && typeof row.key === "string" && typeof row.name === "string")) {
    throw new Error("Native ranking fitness list is invalid");
  }
  return payload;
}

export async function requestTemplateReload(project, task, fileName, apply = true, fetchImpl = globalThis.fetch) {
  const exact = projectName(project);
  if (!exact) throw new Error("Exact native project name is required");
  if (!Number.isInteger(task) || task < 1) throw new Error("Exact native task index is required");
  if (!fileName || typeof fileName !== "string") throw new Error("Official template file name is required");
  if (typeof apply !== "boolean") throw new Error("Template reload apply must be true or false");
  if (typeof fetchImpl !== "function") throw new Error("Native template reload is unavailable");
  const response = await fetchImpl(SQX_BUILD_TYPE_TEMPLATE_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({ project: exact, task, fileName, apply }),
  });
  const payload = await readJson(response);
  if (response?.ok) return payload;
  throw new Error(payload?.detail || payload?.reason_code || `Native template reload failed: ${response?.status ?? "unknown"}`);
}

export async function loadOfficialSettingsLists(fetchImpl = globalThis.fetch, isCurrent = () => true) {
  const [files, ranking, installed, commissions] = await Promise.allSettled([
    fetchBuildTypeFiles(fetchImpl),
    fetchRankingFitnessTypes(fetchImpl),
    fetchInstalledDataSymbols(fetchImpl),
    fetchCommissionMethods(fetchImpl),
  ]);
  if (!isCurrent()) return;
  const fileValue = files.status === "fulfilled" ? files.value : null;
  const rankingValue = ranking.status === "fulfilled" ? ranking.value : null;
  const installedValue = installed.status === "fulfilled" ? installed.value : null;
  const commissionValue = commissions.status === "fulfilled" ? commissions.value : null;
  setOfficialSqxChoices({
    templateFiles: fileValue?.templates ?? null,
    strategyFiles: fileValue?.strategies ?? null,
    filesReady: files.status === "fulfilled",
    rankingTypes: rankingValue?.types ?? null,
    rankingReady: ranking.status === "fulfilled",
    symbols: installedValue?.symbols ?? null,
    sessions: installedValue?.sessions ?? null,
    precisions: installedValue?.precisions ?? null,
    dataRows: installedValue?.rows ?? null,
    dataTypes: installedValue?.dataTypes ?? null,
    swapTypes: installedValue?.swapTypes ?? null,
    tripleSwapOptions: installedValue?.tripleSwapOptions ?? null,
    symbolsReady: installed.status === "fulfilled",
    commissionMethods: commissionValue?.methods ?? null,
    commissionReady: commissions.status === "fulfilled",
  });
}

export async function requestCalibrate(project, task, apply = true, fetchImpl = globalThis.fetch) {
  const exact = projectName(project);
  if (!exact) throw new Error("Exact native project name is required");
  if (!Number.isInteger(task) || task < 1) throw new Error("Exact native task index is required");
  if (typeof apply !== "boolean") throw new Error("Calibrate apply must be true or false");
  if (typeof fetchImpl !== "function") throw new Error("Native calibrate is unavailable");
  const response = await fetchImpl(SQX_CALIBRATE_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({ project: exact, task, apply }),
  });
  const payload = await readJson(response);
  if (response?.ok) return payload;
  throw new Error(payload?.detail || payload?.reason_code || `Native calibrate failed: ${response?.status ?? "unknown"}`);
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
  const fromQuery = projectName(searchParams().get("project") || "");
  if (fromQuery) return fromQuery;
  const path = typeof globalThis.location !== "undefined" ? globalThis.location.pathname : "";
  return RUN_MODULE_PATHS[path] || "";
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
  const tabs = documentedSettingsTabs(task);
  const ids = tabs.map((tab) => tab.id);
  return ids.includes(requested) ? requested : (ids[0] || "");
}

function workflowRoute() {
  const path = typeof globalThis.location !== "undefined" ? globalThis.location.pathname : "";
  return path === CUSTOM_PROJECTS_PATH || path === "/automation" || path in RUN_MODULE_PATHS;
}

function isRunModuleSurface() {
  const path = typeof globalThis.location !== "undefined" ? globalThis.location.pathname : "";
  return path in RUN_MODULE_PATHS;
}

function workflowLink(label, attrs) {
  return `<button type="button" class="sqx-project-link" ${attrs}>${escapeHtml(label)}</button>`;
}

function countLabel(value) {
  return Number.isInteger(value) ? String(value) : "—";
}

function progressPercent(progress) {
  const value = progress?.percent ?? progress?.progress_percent;
  return Number.isInteger(value) && value >= 0 && value <= 100 ? value : null;
}

function isPausedStatus(progress) {
  return typeof progress?.running_status === "string" && /pause/i.test(progress.running_status);
}

function renderPauseButton(project, progress) {
  if (isPausedStatus(progress)) {
    return actionButton("Resume", {
      iconName: "play",
      className: "button-icon",
      attrs: `data-automation-control="resume_project" data-project="${escapeHtml(project)}"`,
      title: "Resume the native StrategyQuant X project",
    });
  }
  if (progress?.running === true) {
    return actionButton("Pause", {
      iconName: "pause",
      className: "button-icon",
      attrs: `data-automation-control="pause_project" data-project="${escapeHtml(project)}"`,
      title: "Pause the native StrategyQuant X project",
    });
  }
  return actionButton("Pause", {
    iconName: "pause",
    className: "button-icon",
    disabled: true,
    title: "Pause is available while StrategyQuant X is running this project",
  });
}

function rowProgress(project, progress = null) {
  if (progress) return progress;
  if (project?.running === true || project?.percent != null || project?.running_status) {
    return {
      running: project.running === true,
      percent: project.percent,
      running_status: project.running_status,
    };
  }
  return null;
}

function renderProjectRow(project, catalog, selected = "", progress = null) {
  const current = project.name === selected;
  const unresolved = project.status === "unresolved";
  const live = rowProgress(project, progress);
  const warning = unresolved
    ? `<span class="sqx-project-warning">${icon("warn", { size: 14 })}<span>${escapeHtml(project.detail || "Project has unresolved resources")}</span></span>`
    : "";
  const links = unresolved
    ? warning
    : `<div class="sqx-project-links">
        ${workflowLink(`[ Tasks (${countLabel(project.task_count)}) ]`, `data-automation-open="${escapeHtml(project.name)}"`)}
        ${workflowLink("[ Engine ]", `data-automation-open="${escapeHtml(project.name)}" data-automation-open-tab="settings" data-automation-open-section="Data"`)}
        ${workflowLink("[ Results ]", `data-automation-open="${escapeHtml(project.name)}" data-automation-open-tab="results"`)}
      </div>`;
  const pct = progressPercent(live);
  const progressSpan = pct == null ? "<span></span>" : `<span style="width:${pct}%"></span>`;
  const running = live?.running === true ? ' data-project-running="true"' : "";
  return `<article class="sqx-project-row ${current ? "is-selected" : ""}" data-automation-project="${escapeHtml(project.name)}" data-project-status="${escapeHtml(project.status)}"${running}>
    <strong class="sqx-project-name">${escapeHtml(projectDisplayName(project.name))}</strong>
    ${links}
    <div class="sqx-project-progress" aria-hidden="true">${progressSpan}</div>
    <div class="sqx-project-transport">
      ${actionButton("Stop", { iconName: "stop", className: "button-icon", attrs: `data-automation-control="stop_project" data-project="${escapeHtml(project.name)}"`, title: catalog.control.detail })}
      <span data-automation-progress-pause>${renderPauseButton(project.name, live)}</span>
      ${actionButton("Start", { iconName: "play", className: "button-icon button-sqx-start", attrs: `data-automation-control="run_project" data-project="${escapeHtml(project.name)}"`, title: catalog.control.detail, disabled: project.status !== "ready" || live?.running === true || startInFlight.has(project.name) })}
    </div>
    <div class="sqx-project-counts">
      <span>DATABANKS: ${escapeHtml(countLabel(project.databank_count))}</span>
      <span>STRATEGIES: ${escapeHtml(countLabel(project.strategy_count))}</span>
    </div>
    <button type="button" class="sqx-project-gear" data-automation-open="${escapeHtml(project.name)}" data-automation-open-tab="settings" title="Full settings" aria-label="Full settings">${icon("settings", { size: 16 })}</button>
  </article>`;
}

function startConfirmDialogHtml() {
  return `<dialog class="sqx-results-dialog" data-automation-start-confirm>
    <form method="dialog">
      <h3>Start this Custom Project?</h3>
      <p>This runs the saved native task order for <strong data-automation-start-confirm-name></strong> on StrategyQuant X. Databanks the project clears stay cleared. This desktop does not invent a second engine.</p>
      <div class="sqx-results-dialog-actions">
        <button type="submit" value="cancel" class="button button-small">Cancel</button>
        <button type="submit" value="start" class="button button-small">Start</button>
      </div>
    </form>
  </dialog>`;
}

export function confirmStartProject(root, project) {
  return new Promise((resolve) => {
    const dialog = root?.querySelector?.("[data-automation-start-confirm]");
    if (!dialog || typeof dialog.showModal !== "function") {
      resolve(false);
      return;
    }
    const name = dialog.querySelector("[data-automation-start-confirm-name]");
    if (name) name.textContent = project;
    const onClose = () => {
      dialog.removeEventListener("close", onClose);
      resolve(dialog.returnValue === "start");
    };
    dialog.addEventListener("close", onClose);
    dialog.returnValue = "";
    dialog.showModal();
  });
}

function catalogRowsHtml(catalog, selected = "", progressByProject = null) {
  return catalog.projects.length
    ? catalog.projects.map((project) => renderProjectRow(
      project,
      catalog,
      selected,
      progressByProject?.[project.name] ?? null,
    )).join("")
    : unavailable(
      "No saved Custom Projects",
      "Verified StrategyQuant X has no Custom Project archives under user/projects yet. This desktop lists real native workflows; it does not invent asset-class rows.",
      { compact: true },
    );
}

export function applyCatalogPatch(root, catalog, selected = "") {
  const list = root?.querySelector?.("[data-automation-project-list]");
  if (!list) return false;
  if (list.querySelector?.("[data-automation-control]:focus, [data-automation-open]:focus")) return false;
  list.innerHTML = catalogRowsHtml(catalog, selected);
  return true;
}

export function renderWorkflowList(catalog, selected = "", progressByProject = null) {
  const rows = catalogRowsHtml(catalog, selected, progressByProject);
  return `<div class="sqx-projects-board" data-automation-project-board>
    <header class="sqx-projects-head">
      <h2>Custom projects</h2>
      <button type="button" class="sqx-projects-refresh" data-automation-refresh title="Refresh native project list" aria-label="Refresh">${icon("refresh", { size: 16 })}</button>
    </header>
    <p class="idea-save-status" data-automation-control-status></p>
    <div class="sqx-project-list" data-automation-project-list>${rows}</div>
    ${startConfirmDialogHtml()}
    <footer class="sqx-projects-foot">
      ${actionButton("Create new project", { className: "button-sqx-create", disabled: true, title: "Native Custom Project create is not wired. This desktop does not invent a project factory." })}
      ${actionButton("Open existing project", { className: "button-sqx-open", attrs: "data-automation-open-existing", title: "Open a saved archive from the verified user/projects list." })}
    </footer>
  </div>`;
}

function taskLabel(task) {
  return task.title || task.name || task.kind;
}

function taskSetupLine(task) {
  const setup = task?.setup;
  if (!setup) return "";
  const range = setup.date_from && setup.date_to ? `${setup.date_from}–${setup.date_to}` : (setup.date_from || setup.date_to || "");
  return [setup.symbol, setup.timeframe, range, setup.engine].filter(Boolean).join(" · ");
}

function taskFlowLine(task) {
  if (task.goto_target_label) return `Go to ${task.goto_target_label}`;
  if (Array.isArray(task.clear_databanks) && task.clear_databanks.length) {
    return `Clear ${task.clear_databanks.join(", ")}`;
  }
  const inputs = Array.isArray(task.input_databanks) ? task.input_databanks : [];
  const outputs = Array.isArray(task.output_databanks) ? task.output_databanks : [];
  if (!inputs.length && !outputs.length) return "";
  return `${inputs.join(", ") || "—"} → ${outputs.join(", ") || "—"}`;
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
    <p class="note">Live native values from the saved task XML. Writes existing attributes or text only.</p>
  </form>`;
}

export function renderTaskPipeline(topology, selectedTask = null) {
  const add = `<button type="button" class="task-add" disabled aria-disabled="true" title="Native add-task is not wired. This desktop does not invent Custom Project tasks.">+ Add new task</button>`;
  if (!topology.tasks.length) {
    return `${add}<p class="field-help">This saved native project contains no numbered tasks.</p>`;
  }
  return `${add}<ol class="task-pipeline" data-automation-task-pipeline>${topology.tasks.map((task, index) => {
    const active = task.active === false ? "is-off" : "is-on";
    const selected = task.native_task_index === selectedTask ? "is-selected" : "";
    const connector = index < topology.tasks.length - 1
      ? `<li class="task-connector" aria-hidden="true">${icon("down", { size: 12 })}<span class="task-plus">${icon("plus", { size: 10 })}</span></li>`
      : "";
    const canToggle = task.active === true || task.active === false;
    const setupLine = taskSetupLine(task);
    const flowLine = taskFlowLine(task);
    return `<li class="task-step ${active} ${selected}" data-native-project-task="${task.native_task_index}" data-automation-select-task="${task.native_task_index}">
      <span class="task-index">${task.native_task_index}</span>
      <div class="task-copy">
        <strong>${escapeHtml(taskLabel(task))}</strong>
        <span class="task-kind">${escapeHtml(task.kind)}</span>
        ${setupLine ? `<span class="task-setup">${escapeHtml(setupLine)}</span>` : ""}
        ${flowLine ? `<span class="task-io">${escapeHtml(flowLine)}</span>` : ""}
      </div>
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

function progressDash(value) {
  return value === null || value === undefined ? "—" : String(value);
}

function renderProgressLogs(progress) {
  const lines = Array.isArray(progress?.log_lines) ? progress.log_lines : [];
  const worker = typeof progress?.worker_label === "string" && progress.worker_label ? progress.worker_label : "";
  const body = lines.length
    ? `<ol class="workflow-log" data-automation-progress-log>${lines.map((line) => (
      `<li><code>${escapeHtml(line.relative_path)}</code><span>${escapeHtml(line.text)}</span></li>`
    )).join("")}</ol>`
    : unavailable(
      progress?.running ? "Native project is running" : "No producer log yet",
      "Generated, rejected, accepted, and rate stay dashes until the producer writes them.",
      { compact: true, tone: progress?.running ? "pending" : "unavailable" },
    );
  const label = worker
    ? `<span class="sqx-task-log-label"><code data-progress-worker>${escapeHtml(worker)}</code></span>`
    : `<span class="sqx-task-log-label">Task:</span>`;
  return `<div class="sqx-task-log">${label}<div class="sqx-task-log-body">${body}</div></div>`;
}

function renderProgressStats(progress) {
  const rows = [
    ["Total tested", progressDash(progress?.generated)],
    ["Failed", progressDash(progress?.rejected)],
    ["Passed", progressDash(progress?.accepted)],
    ["Rate", progressDash(progress?.rate)],
  ];
  return `<dl class="sqx-progress-stats" data-automation-progress-stats>${rows.map(([label, value]) => (
    `<div><dt>${escapeHtml(label)}</dt><dd data-progress-stat="${escapeHtml(label.toLowerCase().replace(/\s+/g, "-"))}">${escapeHtml(value)}</dd></div>`
  )).join("")}</dl>`;
}

function renderProgressBar(progress) {
  const running = progress?.running === true;
  const pct = progressPercent(progress);
  const indeterminate = running && pct == null ? " is-indeterminate" : "";
  const span = pct == null ? "<span></span>" : `<span style="width:${pct}%"></span>`;
  const aria = pct == null
    ? (running ? ' aria-busy="true"' : "")
    : ` aria-valuemin="0" aria-valuemax="100" aria-valuenow="${pct}"`;
  return `<div class="workflow-progress sqx-progress-bar${indeterminate}" role="progressbar"${aria} aria-label="Native project progress">${span}</div>`;
}

function renderNativeSetupStrip(setup) {
  if (!setup) return "";
  const rows = [
    ["Engine", setup.engine],
    ["Symbol", setup.symbol],
    ["Timeframe", setup.timeframe],
    ["From", setup.date_from],
    ["To", setup.date_to],
    ["Build mode", setup.generation_type],
    ["MM type", setup.money_management_type],
    ["MM size", setup.money_management_size],
  ].filter(([, value]) => value != null && value !== "");
  if (!rows.length) return "";
  return `<dl class="sqx-progress-stats" data-automation-native-setup>${rows.map(([label, value]) => (
    `<div><dt>${escapeHtml(label)}</dt><dd>${escapeHtml(String(value))}</dd></div>`
  )).join("")}</dl>`;
}

function renderProgressResultsColumn(results, topology, task, progress = null) {
  const item = projectResultsOf(results, topology.project);
  const archives = Number.isInteger(progress?.strategy_count)
    ? progress.strategy_count
    : (Number.isInteger(item?.strategy_count) ? item.strategy_count : 0);
  const databanks = Number.isInteger(progress?.databank_count)
    ? progress.databank_count
    : (Number.isInteger(item?.databank_count) ? item.databank_count : null);
  if (!item?.databanks?.length && !archives) {
    return `<aside class="sqx-progress-column-right">${unavailable(
      "No results so far",
      "Databank archives appear here when StrategyQuant X writes them during a run.",
      { compact: true },
    )}</aside>`;
  }
  const strip = item?.databanks?.length
    ? renderProjectDatabankList(results, topology.project, {
      archiveHref: (bank, archive) => workflowHref({
        project: topology.project,
        tab: "results",
        task: task?.native_task_index,
        databank: bank,
        archive,
        resultView: "overview",
      }),
    })
    : "";
  const databankMeta = databanks == null ? "" : `<span>${escapeHtml(String(databanks))} databank${databanks === 1 ? "" : "s"}</span>`;
  return `<aside class="sqx-progress-column-right">
    <header class="sqx-progress-results-head"><strong>Live results</strong>${databankMeta}<span>${escapeHtml(String(archives))} archive${archives === 1 ? "" : "s"}</span></header>
    <div class="sqx-progress-databank-strip">${strip || unavailable("Databanks unread", "Producer databank files are not readable yet.", { compact: true })}</div>
  </aside>`;
}

function renderProgressChartFrame(frame) {
  const tones = ["cyan", "purple", "orange", "green"];
  const series = Array.isArray(frame?.series) ? frame.series : [];
  const drawn = series.filter((line) => optionalChartValues(line.values));
  if (!drawn.length) {
    return chartFrame({
      title: frame?.title || "Engine charts",
      height: 120,
      state: "unavailable",
      detail: "Series appear when StrategyQuant X publishes engineCharts.",
    });
  }
  const all = drawn.flatMap((line) => line.values);
  const max = Math.max(...all);
  const min = Math.min(...all);
  const mid = (max + min) / 2;
  return chartFrame({
    title: frame.title,
    height: 120,
    state: "current",
    detail: "",
    legend: drawn.filter((line) => line.label).map((line, index) => [line.label, tones[index % tones.length]]),
    yLabels: [String(max), String(mid), String(min)],
    series: drawn.map((line, index) => ({ values: line.values, tone: tones[index % tones.length] })),
  });
}

function renderProgressChartRows(frame) {
  const items = Array.isArray(frame?.items) ? frame.items : [];
  if (!items.length) {
    return chartFrame({
      title: frame?.title || "Engine charts",
      height: 120,
      state: "unavailable",
      detail: "Series appear when StrategyQuant X publishes engineCharts.",
    });
  }
  return `<dl class="sqx-engine-chart-rows" data-chart-kind="${escapeHtml(frame.kind || "rows")}">${items.map((row) => (
    `<div><dt>${escapeHtml(row.name)}</dt><dd>${escapeHtml(row.value)}</dd></div>`
  )).join("")}</dl>`;
}

function renderProgressChartPicker(frame, slot, types, settings, project) {
  if (!types.length || !project) return "";
  const selected = settings[slot] || frame?.type || types[0].type;
  return `<label class="sqx-engine-chart-type"><select data-engine-chart-slot="${slot}" data-project="${escapeHtml(project)}" aria-label="Progress chart ${slot + 1}">${types.map((item) => (
    `<option value="${escapeHtml(item.type)}"${item.type === selected ? " selected" : ""}>${escapeHtml(item.name)}</option>`
  )).join("")}</select></label>`;
}

function renderProgressChartSlot(frame, slot, types, settings, project) {
  const body = frame?.kind === "grid" || frame?.kind === "rows"
    ? renderProgressChartRows(frame)
    : renderProgressChartFrame(frame);
  return `<div class="sqx-engine-chart-slot">${renderProgressChartPicker(frame, slot, types, settings, project)}${body}</div>`;
}

export function progressLiveFragments(progress, project) {
  return {
    running: progress?.running === true ? "true" : "false",
    stats: renderProgressStats(progress),
    bar: renderProgressBar(progress),
    logs: renderProgressLogs(progress),
    charts: renderProgressCharts(progress, project),
    pause: renderPauseButton(project, progress),
  };
}

export function applyProgressPatch(root, progress, project) {
  const shell = root?.querySelector?.("[data-automation-progress-running]");
  if (!shell) return false;
  const next = progressLiveFragments(progress, project);
  shell.dataset.automationProgressRunning = next.running;
  const stats = root.querySelector("[data-automation-progress-stats]");
  if (stats) stats.outerHTML = next.stats;
  const bar = root.querySelector(".sqx-progress-bar");
  if (bar) bar.outerHTML = next.bar;
  const logs = root.querySelector(".sqx-task-log");
  if (logs) logs.outerHTML = next.logs;
  const pause = root.querySelector("[data-automation-progress-pause]");
  if (pause) pause.innerHTML = next.pause;
  const start = root.querySelector('[data-automation-control="run_project"]');
  if (start) start.disabled = progress?.running === true || startInFlight.has(project);
  const charts = root.querySelector(".sqx-progress-charts");
  // ponytail: keep an open type picker; next tick paints charts
  if (charts && !root.querySelector("[data-engine-chart-slot]:focus")) {
    charts.outerHTML = next.charts;
  }
  return true;
}

function renderProgressCharts(progress, project = "") {
  const types = Array.isArray(progress?.chart_types) ? progress.chart_types : [];
  const settings = Array.isArray(progress?.chart_settings) ? progress.chart_settings : [];
  const frames = Array.isArray(progress?.charts) && progress.charts.length
    ? progress.charts.slice(0, 2)
    : [
      { title: "Average strategies per hour", type: settings[0] || "AverageStrategiesPerHourChart", series: [] },
      { title: "Heap memory chart", type: settings[1] || "HeapMemoryChart", series: [] },
    ];
  if (types.length) {
    while (frames.length < 2) {
      const type = settings[frames.length] || types[0].type;
      const name = types.find((item) => item.type === type)?.name || type;
      frames.push({ title: name, type, series: [] });
    }
  }
  return `<div class="sqx-progress-charts${frames.length > 1 ? " is-pair" : ""}">${frames.map((frame, slot) => renderProgressChartSlot(frame, slot, types, settings, project)).join("")}</div>`;
}

function renderProgressPanel(topology, control, task, progress = null, results = null) {
  const reason = control?.detail || readable(control?.reason_code, "Native Custom Project launch is not ready");
  const running = progress?.running === true;
  return `<div class="sqx-progress-shell" data-automation-progress-running="${running ? "true" : "false"}">
    <div class="sqx-progress-column-left">
      <div class="sqx-progress-transport">
        ${actionButton("Stop", { iconName: "stop", className: "button-icon", attrs: `data-automation-control="stop_project" data-project="${escapeHtml(topology.project)}"`, title: reason })}
        <span data-automation-progress-pause>${renderPauseButton(topology.project, progress)}</span>
        ${actionButton("Start", { iconName: "play", className: "button-icon button-sqx-start", attrs: `data-automation-control="run_project" data-project="${escapeHtml(topology.project)}"`, title: reason, disabled: running || startInFlight.has(topology.project) })}
      </div>
      ${startConfirmDialogHtml()}
      ${renderProgressBar(progress)}
      ${renderProgressLogs(progress)}
      ${renderProgressStats(progress)}
      ${renderProgressCharts(progress, topology.project)}
      <p class="idea-save-status" data-automation-control-status></p>
    </div>
    <aside class="sqx-progress-column-mid">${renderNativeSetupStrip(topology.native_setup)}${renderProgressSummary(task, topology.project)}</aside>
    ${renderProgressResultsColumn(results, topology, task, progress)}
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

export function renderWorkflowDetail(topology, control, results = null, view = {}, strategy = null, strategyError = "", progress = null) {
  const tab = WORKFLOW_TABS.includes(view.tab) ? view.tab : "progress";
  const taskIndex = Number.isInteger(view.task) ? view.task : (topology.tasks[0]?.native_task_index ?? null);
  const task = topology.tasks.find((item) => item.native_task_index === taskIndex) || topology.tasks[0] || null;
  const section = view.section || selectedSettingsSection(task);
  const method = view.method || "";
  const methodPane = view.methodPane || "";
  const block = view.block || "";
  let main = "";
  if (tab === "settings") {
    main = renderFullSettings(task, section, topology.project, method, methodPane, block);
  } else if (tab === "results") {
    main = renderResultsPanel(topology, results, {
      task: taskIndex,
      databank: view.databank || "",
      archive: view.archive || "",
      resultView: view.resultView || "overview",
      sample: view.sample || "",
      direction: view.direction || "",
    }, strategy, strategyError);
  } else {
    main = renderProgressPanel(topology, control, task, progress, results);
  }
  const moduleMode = Boolean(view.module);
  const crumb = moduleMode
    ? `<nav class="workflow-crumb"><strong>${escapeHtml(topology.project)}</strong><span>Native module archive</span></nav>`
    : `<nav class="workflow-crumb">
      ${actionButton("Custom projects", { iconName: "list", className: "button-small", attrs: "data-automation-back" })}
      <span>/</span>
      <strong>${escapeHtml(topology.project)}</strong>
    </nav>`;
  return `<div class="automation-detail" data-automation-project-detail="${escapeHtml(topology.project)}" data-automation-tab="${escapeHtml(tab)}" data-sqx-module-mode="${moduleMode ? "run" : "custom"}">
    ${crumb}
    ${renderWorkflowTabs(topology, tab, taskIndex, section)}
    <div class="automation-detail-grid">
      <section class="sqx-task-column">${renderTaskPipeline(topology, taskIndex)}</section>
      <section class="sqx-main-column" data-automation-main-tab="${escapeHtml(tab)}">${main}</section>
    </div>
  </div>`;
}

function host() {
  if (!workflowRoute()) return null;
  return document.querySelector("[data-automation-workflows]");
}

function workflowLocationKey() {
  if (typeof globalThis.location === "undefined") return "";
  return `${globalThis.location.pathname}${globalThis.location.search || ""}`;
}

function liveWorkflowHost(root, myGeneration) {
  if (myGeneration !== generation) return null;
  const live = host();
  if (live) return live;
  return root?.isConnected ? root : null;
}

function paintWorkflowState(root, myGeneration, state, html) {
  const live = liveWorkflowHost(root, myGeneration);
  if (!live) return false;
  live.dataset.automationWorkflows = state;
  live.innerHTML = html;
  boundHost = live;
  return true;
}

function commitWorkflowHtml(root, myGeneration, html, strategy = null) {
  const live = liveWorkflowHost(root, myGeneration);
  if (!live) return false;
  live.dataset.automationWorkflows = "loaded";
  live.dataset.workflowLoadKey = workflowLocationKey();
  live.innerHTML = html;
  boundHost = live;
  try {
    bindResultsChrome(live, strategy);
    bindDatabankGrid(live);
    bindSettingsScroll(live);
    void loadOosGraph(live);
  } catch {
    // Results/settings binders must not block native read-model paint.
  }
  return true;
}

async function loadOosGraph(root) {
  const host = root?.querySelector?.("[data-sqx-oos-graph]");
  if (!host || host.getAttribute("data-show-graph") !== "true") return;
  const symbol = host.getAttribute("data-symbol") || "";
  if (!symbol || symbol.startsWith("[")) return;
  try {
    const payload = await fetchSymbolData(
      host.getAttribute("data-date-from") || "",
      host.getAttribute("data-date-to") || "",
      symbol,
      host.getAttribute("data-session") || "No Session",
    );
    const values = payload.points.map((row) => Number(row[1])).filter(Number.isFinite);
    host.innerHTML = chartFrame({
      title: "OOS graph",
      height: 120,
      state: values.length > 1 ? "current" : "unavailable",
      detail: values.length > 1 ? "" : "StrategyQuant X data/getSymbolData returned no series.",
      series: values.length > 1 ? [{ values, tone: "cyan" }] : [],
    });
  } catch (error) {
    host.innerHTML = unavailable(
      "OOS graph unavailable",
      error instanceof Error ? error.message : "data/getSymbolData failed.",
      { compact: true, tone: "error" },
    );
  }
}

let generation = 0;
let boundHost = null;
let bindScheduled = false;
let progressPollTimer = 0;
let progressPollBusy = false;

function stopProgressPoll() {
  if (progressPollTimer) {
    globalThis.clearInterval(progressPollTimer);
    progressPollTimer = 0;
  }
  progressPollBusy = false;
}

async function refreshProgressLive(project, myGeneration) {
  if (progressPollBusy) return;
  const root = host();
  if (!liveWorkflowHost(root, myGeneration) || root.dataset.automationWorkflows !== "loaded") {
    if (!liveWorkflowHost(root, myGeneration)) stopProgressPoll();
    return;
  }
  if (selectedWorkflowTab() !== "progress") {
    stopProgressPoll();
    return;
  }
  progressPollBusy = true;
  try {
    const progress = await fetchProjectProgress(project);
    if (!liveWorkflowHost(root, myGeneration) || root.dataset.automationWorkflows !== "loaded") return;
    applyProgressPatch(root, progress, project);
  } catch {
    // Keep the last painted producer values; do not invent a refresh.
  } finally {
    progressPollBusy = false;
  }
}

function armProgressPoll(root, myGeneration, project, tab) {
  stopProgressPoll();
  if (tab !== "progress" || !projectName(project)) return;
  if (!root?.querySelector?.("[data-automation-progress-running]")) return;
  if (typeof globalThis.setInterval !== "function") return;
  progressPollTimer = globalThis.setInterval(() => {
    void refreshProgressLive(project, myGeneration);
  }, SQX_PROGRESS_POLL_MS);
}

async function refreshCatalogLive(myGeneration) {
  if (progressPollBusy) return;
  const root = host();
  if (!liveWorkflowHost(root, myGeneration) || root.dataset.automationWorkflows !== "loaded") {
    if (!liveWorkflowHost(root, myGeneration)) stopProgressPoll();
    return;
  }
  if (selectedProjectName() || isRunModuleSurface()) {
    stopProgressPoll();
    return;
  }
  if (!root.querySelector("[data-automation-project-list]")) {
    stopProgressPoll();
    return;
  }
  progressPollBusy = true;
  try {
    const catalog = await fetchCustomProjectsCatalog();
    if (!liveWorkflowHost(root, myGeneration) || root.dataset.automationWorkflows !== "loaded") return;
    if (selectedProjectName()) return;
    applyCatalogPatch(root, catalog, "");
  } catch {
    // Keep the last painted catalog rows; do not invent a refresh.
  } finally {
    progressPollBusy = false;
  }
}

function armCatalogPoll(root, myGeneration) {
  stopProgressPoll();
  if (selectedProjectName() || isRunModuleSurface()) return;
  if (!root?.querySelector?.("[data-automation-project-list]")) return;
  if (typeof globalThis.setInterval !== "function") return;
  progressPollTimer = globalThis.setInterval(() => {
    void refreshCatalogLive(myGeneration);
  }, SQX_PROGRESS_POLL_MS);
}

function scheduleBindWorkspace() {
  if (bindScheduled) return;
  bindScheduled = true;
  queueMicrotask(() => {
    bindScheduled = false;
    bindWorkspace();
  });
}

function reloadWorkspace() {
  stopProgressPoll();
  const root = host();
  if (root) delete root.dataset.workflowLoadKey;
  boundHost = null;
  bindWorkspace();
}

function renderShell(inner) {
  return inner;
}

function navigate(url) {
  if (typeof globalThis.history === "undefined") return;
  globalThis.history.pushState({}, "", url);
  reloadWorkspace();
}

async function loadModuleWorkspace(root, moduleName, myGeneration) {
  const moduleRecord = await fetchSqxModule(moduleName);
  if (!liveWorkflowHost(root, myGeneration)) return;
  if (moduleRecord.status !== "ready" || !moduleRecord.project) {
    paintWorkflowState(root, myGeneration, "unavailable", unavailable(
      `${moduleName} unavailable`,
      moduleRecord.detail || "This module archive is not present on the verified runtime. This desktop does not invent tasks.",
      { compact: true, tone: "unavailable" },
    ));
    return;
  }
  const tab = searchParams().get("tab") || "progress";
  const listsPromise = tab === "settings" ? loadOfficialSettingsLists(globalThis.fetch, () => Boolean(liveWorkflowHost(root, myGeneration))) : Promise.resolve();
  const topology = await fetchWorkflowTopology(moduleRecord.project);
  await listsPromise;
  if (!liveWorkflowHost(root, myGeneration)) return;
  let results = null;
  if (tab !== "settings") {
    try {
      results = await fetchCustomProjectResults(moduleRecord.project);
    } catch {
      results = null;
    }
  }
  if (!liveWorkflowHost(root, myGeneration)) return;
  const view = {
    tab: selectedWorkflowTab(),
    task: selectedTaskIndex(topology),
    section: searchParams().get("section") || "",
    method: searchParams().get("method") || "",
    methodPane: searchParams().get("methodPane") || "",
    block: searchParams().get("block") || "",
    databank: searchParams().get("databank") || "",
    archive: searchParams().get("archive") || "",
    resultView: searchParams().get("resultView") || "",
    sample: searchParams().get("sample") || "",
    direction: searchParams().get("direction") || "",
    module: moduleName,
  };
  let strategy = null;
  let strategyError = "";
  if (view.tab === "results" && view.databank && view.archive) {
    try {
      strategy = await fetchProjectStrategy(moduleRecord.project, view.databank, view.archive, view.task);
    } catch (error) {
      strategyError = error instanceof Error ? error.message : "Native strategy inspect unavailable";
    }
  }
  let progress = null;
  if (view.tab === "progress") {
    try {
      progress = await fetchProjectProgress(moduleRecord.project);
    } catch {
      progress = null;
    }
  }
  if (!liveWorkflowHost(root, myGeneration)) return;
  commitWorkflowHtml(
    root,
    myGeneration,
    renderWorkflowDetail(topology, moduleRecord.control, results, view, strategy, strategyError, progress),
    strategy,
  );
  armProgressPoll(root, myGeneration, moduleRecord.project, view.tab);
}

async function loadWorkspace(root) {
  stopProgressPoll();
  const myGeneration = ++generation;
  const moduleName = isRunModuleSurface() ? (RUN_MODULE_PATHS[currentWorkflowPath()] || selectedProjectName()) : "";
  const selected = selectedProjectName();
  if (moduleName) {
    paintWorkflowState(
      root,
      myGeneration,
      "loading",
      unavailable(`Loading ${moduleName}…`, `Reading user/projects/${moduleName}/project.cfx from the verified runtime.`, { tone: "pending", compact: true }),
    );
    try {
      await loadModuleWorkspace(root, moduleName, myGeneration);
    } catch (error) {
      if (!liveWorkflowHost(root, myGeneration)) return;
      paintWorkflowState(root, myGeneration, "failed", unavailable(
        `${moduleName} unavailable`,
        error instanceof Error ? error.message : "Native module archive could not be read.",
        { compact: true, tone: "error" },
      ));
    }
    return;
  }
  paintWorkflowState(
    root,
    myGeneration,
    "loading",
    unavailable("Loading native workflows…", "Reading saved Custom Projects from the verified StrategyQuant X runtime.", { tone: "pending", compact: true }),
  );
  try {
    const catalog = await fetchCustomProjectsCatalog();
    if (!liveWorkflowHost(root, myGeneration)) return;
    const list = renderWorkflowList(catalog, selected);
    let detail = "";
    let strategy = null;
    if (selected) {
      try {
        const tab = searchParams().get("tab") || "progress";
        const listsPromise = tab === "settings" ? loadOfficialSettingsLists(globalThis.fetch, () => Boolean(liveWorkflowHost(root, myGeneration))) : Promise.resolve();
        const topology = await fetchWorkflowTopology(selected);
        await listsPromise;
        if (!liveWorkflowHost(root, myGeneration)) return;
        let results = null;
        if (tab !== "settings") {
          try {
            results = await fetchCustomProjectResults(selected);
          } catch {
            results = null;
          }
        }
        if (!liveWorkflowHost(root, myGeneration)) return;
        const view = {
          tab: selectedWorkflowTab(),
          task: selectedTaskIndex(topology),
          section: searchParams().get("section") || "",
          method: searchParams().get("method") || "",
          methodPane: searchParams().get("methodPane") || "",
          block: searchParams().get("block") || "",
          databank: searchParams().get("databank") || "",
          archive: searchParams().get("archive") || "",
          resultView: searchParams().get("resultView") || "",
          sample: searchParams().get("sample") || "",
          direction: searchParams().get("direction") || "",
        };
        let strategyError = "";
        if (view.tab === "results" && view.databank && view.archive) {
          try {
            strategy = await fetchProjectStrategy(selected, view.databank, view.archive, view.task);
          } catch (error) {
            strategyError = error instanceof Error ? error.message : "Native strategy inspect unavailable";
          }
        }
        let progress = null;
        if (view.tab === "progress") {
          try {
            progress = await fetchProjectProgress(selected);
          } catch {
            progress = null;
          }
        }
        if (!liveWorkflowHost(root, myGeneration)) return;
        detail = renderWorkflowDetail(topology, catalog.control, results, view, strategy, strategyError, progress);
      } catch (error) {
        detail = `<nav class="workflow-crumb">${actionButton("Custom projects", { iconName: "list", className: "button-small", attrs: "data-automation-back" })}</nav>${unavailable("Could not open this project", error instanceof Error ? error.message : "Native topology unavailable", { compact: true, tone: "error" })}`;
      }
    }
    commitWorkflowHtml(root, myGeneration, renderShell(selected ? detail : list), strategy);
    if (selected) armProgressPoll(root, myGeneration, selected, selectedWorkflowTab());
    else armCatalogPoll(root, myGeneration);
  } catch (error) {
    if (!liveWorkflowHost(root, myGeneration)) return;
    paintWorkflowState(root, myGeneration, "failed", unavailable(
      "Native workflows unavailable",
      error instanceof Error ? error.message : "Custom Project catalog could not be read.",
      { compact: true, tone: "error" },
    ));
  }
}

function bindWorkspace() {
  const root = host();
  if (!root) return;
  const key = workflowLocationKey();
  const state = root.dataset.automationWorkflows || "";
  if (root.dataset.workflowLoadKey === key && state) return;
  root.dataset.workflowLoadKey = key;
  root.dataset.automationWorkflows = "loading";
  boundHost = root;
  void loadWorkspace(root);
}

function showList() {
  navigate(currentWorkflowPath());
}

function openProject(name, extras = {}) {
  const exact = projectName(name);
  if (!exact) return;
  navigate(workflowHref({ project: exact, ...extras }));
}

function controlValue(element) {
  if (element.matches("[data-settings-kind='flag']")) {
    return element.classList.contains("is-on") ? "true" : "false";
  }
  return String(element.value ?? "");
}

export function exclusiveUseUpdates(element) {
  const group = element?.closest?.("[data-settings-exclusive-group]");
  if (!group) return null;
  const updates = [...group.querySelectorAll("input[data-settings-exclusive-use][data-settings-attribute='use']")].flatMap((input) => {
    const path = JSON.parse(input.getAttribute("data-settings-path") || "[]");
    if (!path.length) return [];
    return [{ path, attribute: "use", value: input.checked ? "true" : "false" }];
  });
  return updates.length ? updates : null;
}

export function collectSettingsUpdates(root) {
  const seen = new Set();
  return [...root.querySelectorAll("[data-settings-attribute], [data-settings-text]")].flatMap((element) => {
    if (element.hasAttribute("data-settings-exclusive-use")) {
      const group = element.closest("[data-settings-exclusive-group]");
      if (!group || seen.has(group)) return [];
      seen.add(group);
      return exclusiveUseUpdates(element) || [];
    }
    if (element.matches?.("input[type='radio']") && !element.checked) return [];
    const path = JSON.parse(element.getAttribute("data-settings-path") || "[]");
    if (!path.length) return [];
    if (element.hasAttribute("data-settings-text")) {
      return [{ path, text: controlValue(element) }];
    }
    const attribute = element.getAttribute("data-settings-attribute") || "";
    if (!attribute) return [];
    return [{ path, attribute, value: controlValue(element) }];
  });
}

function updateFromControl(element) {
  const path = JSON.parse(element.getAttribute("data-settings-path") || "[]");
  if (!path.length) return null;
  if (element.hasAttribute("data-settings-text")) {
    return { path, text: controlValue(element) };
  }
  const attribute = element.getAttribute("data-settings-attribute") || "";
  if (!attribute) return null;
  return { path, attribute, value: controlValue(element) };
}

function persistControl(element, updates) {
  const form = element.closest("[data-automation-settings-form]");
  const task = Number(form?.getAttribute("data-settings-task") || searchParams().get("task"));
  const status = form?.querySelector("[data-automation-settings-status]") || document.querySelector("[data-automation-settings-status]");
  const payload = symbolChangeUpdates(element, updates || [updateFromControl(element)].filter(Boolean));
  if (!payload.length) {
    if (status) status.textContent = "No existing attributes or text to write on this control.";
    return;
  }
  void writeSettings(selectedProjectName(), task, payload, status);
}

async function controlProject(button, action) {
  const project = projectName(button.getAttribute("data-project") || "");
  const status = document.querySelector("[data-automation-control-status]");
  if (action === "run_project") {
    if (startInFlight.has(project)) return;
    const host = button.closest("[data-automation-project-board]")
      || button.closest("[data-automation-progress-running]")
      || document;
    if (!await confirmStartProject(host, project)) return;
    startInFlight.add(project);
  }
  button.disabled = true;
  if (status) status.textContent = `Requesting native ${action}…`;
  try {
    await requestProjectControl(project, action);
    if (action === "stop_project") startInFlight.delete(project);
    const messages = {
      run_project: "Native project is running.",
      stop_project: "Native project stop requested.",
      pause_project: "Native project pause requested.",
      resume_project: "Native project resume requested.",
    };
    if (status) status.textContent = messages[action] || "Native project control requested.";
    reloadWorkspace();
  } catch (error) {
    if (action === "run_project") startInFlight.delete(project);
    if (status) status.textContent = error instanceof Error ? error.message : "Native launch refused the request.";
  } finally {
    if (action !== "run_project" || !startInFlight.has(project)) button.disabled = false;
  }
}

async function writeSettings(project, task, updates, statusNode) {
  if (statusNode) statusNode.textContent = "Writing native settings…";
  try {
    await saveProjectSettings(project, task, updates);
    if (statusNode) statusNode.textContent = "Saved existing native attributes or text.";
    reloadWorkspace();
  } catch (error) {
    if (statusNode) statusNode.textContent = error instanceof Error ? error.message : "Native settings write refused.";
  }
}

function applyAzFilter(button) {
  const letter = button.getAttribute("data-settings-az") || "";
  const panel = button.closest("[data-settings-block-panel]");
  if (!panel) return;
  for (const item of panel.querySelectorAll("[data-settings-az]")) {
    item.classList.toggle("is-current", item === button);
  }
  for (const row of panel.querySelectorAll(".settings-block-row")) {
    const key = row.getAttribute("data-block-key") || "";
    const start = (key.replace(/^.*\./, "").match(/[A-Za-z]/) || [""])[0].toUpperCase();
    row.hidden = Boolean(letter) && start !== letter;
  }
  for (const family of panel.querySelectorAll(".settings-block-family")) {
    const name = family.getAttribute("data-block-family") || "";
    family.hidden = ![...panel.querySelectorAll(".settings-block-row")].some((row) => (
      !row.hidden && row.getAttribute("data-block-family") === name
    ));
  }
}

function sizeSettingsBlockList(root) {
  const page = document.querySelector(".content-scroll");
  if (!page) return null;
  const pageBottom = page.getBoundingClientRect().bottom;
  const foot = root.querySelector(".settings-block-foot");
  const reserve = (foot ? Math.ceil(foot.getBoundingClientRect().height) : 0) + 10;
  let sizedH = null;
  const fit = (el) => {
    const top = el.getBoundingClientRect().top;
    sizedH = Math.max(160, Math.floor(pageBottom - top - reserve));
    el.style.maxHeight = `${sizedH}px`;
  };
  root.querySelectorAll(".settings-blocks-main details[open] .settings-block-scroll").forEach(fit);
  root.querySelectorAll(".settings-blocks-side").forEach((side) => {
    side.style.overflowY = "auto";
    fit(side);
  });
  return sizedH;
}

function layoutSettingsScroll(root) {
  sizeSettingsBlockList(root);
  requestAnimationFrame(() => sizeSettingsBlockList(root));
}

function bindSettingsScroll(root) {
  const roll = root.querySelector(".settings-section-roll");
  if (roll && !roll.dataset.scrollBound) {
    roll.dataset.scrollBound = "1";
    roll.addEventListener("wheel", (event) => {
      if (roll.scrollWidth <= roll.clientWidth + 2) return;
      event.preventDefault();
      roll.scrollLeft += event.deltaY + event.deltaX;
    }, { passive: false });
  }
  if (!root.dataset.accordionSizeBound) {
    root.dataset.accordionSizeBound = "1";
    root.addEventListener("toggle", (event) => {
      if (!event.target?.classList?.contains("settings-block-accordion")) return;
      requestAnimationFrame(() => bindSettingsScroll(root));
    }, true);
  }
  if (!globalThis.__settingsScrollResize) {
    globalThis.__settingsScrollResize = true;
    globalThis.addEventListener("resize", () => {
      const host = document.querySelector("[data-automation-workflows='loaded']") || root;
      layoutSettingsScroll(host);
    });
  }
  requestAnimationFrame(() => layoutSettingsScroll(root));
}

if (typeof document !== "undefined") {
  document.addEventListener("input", (event) => {
    const search = event.target.closest?.("[data-sqx-data-search]");
    if (search) filterSqxDataBox(search.closest("[data-sqx-data-box]"), { search: search.value });
  });
  document.addEventListener("change", (event) => {
    if (!workflowRoute()) return;
    const chartSelect = event.target.closest?.("[data-engine-chart-slot]");
    if (chartSelect) {
      const project = projectName(chartSelect.getAttribute("data-project") || "");
      const number = Number(chartSelect.getAttribute("data-engine-chart-slot"));
      const type = String(chartSelect.value || "");
      const status = document.querySelector("[data-automation-control-status]");
      void (async () => {
        if (status) status.textContent = "Saving official Progress chart type…";
        try {
          await saveEngineChartSelection(project, number, type);
          if (status) status.textContent = "Saved official Progress chart type.";
          reloadWorkspace();
        } catch (error) {
          if (status) status.textContent = error instanceof Error ? error.message : "Official chart type was refused.";
        }
      })();
      return;
    }
    const sampleSelect = event.target.closest?.("[data-results-sample], [data-results-direction]");
    if (sampleSelect) {
      const option = sampleSelect.selectedOptions?.[0];
      const href = option?.getAttribute("data-route");
      if (href) navigate(href);
      return;
    }
    const choice = event.target.closest?.("[data-settings-kind='choice']");
    if (choice) {
      persistControl(choice, exclusiveUseUpdates(choice) || undefined);
      return;
    }
    const field = event.target.closest?.("[data-settings-path]");
    if (field && !field.matches("[data-settings-kind='flag']")) persistControl(field);
  });
  document.addEventListener("submit", async (event) => {
    const form = event.target.closest?.("[data-results-new-analysis-form]");
    if (!form || !workflowRoute()) return;
    if (event.submitter?.value !== "create") return;
    event.preventDefault();
    const name = String(form.querySelector("input[name=name]")?.value || "").trim();
    const status = form.querySelector("[data-results-new-analysis-status]");
    if (!name) {
      if (status) status.textContent = "Custom plugin name is required.";
      return;
    }
    if (status) status.textContent = "Copying CustomPlugin…";
    try {
      const created = await createResultsPluginTab(name);
      form.closest("dialog")?.close?.();
      openProject(selectedProjectName(), {
        tab: "results",
        task: searchParams().get("task") || "",
        databank: searchParams().get("databank") || "",
        archive: searchParams().get("archive") || "",
        resultView: created.id,
        sample: searchParams().get("sample") || "",
        direction: searchParams().get("direction") || "",
      });
    } catch (error) {
      if (status) status.textContent = error instanceof Error ? error.message : "Failed to create custom analysis.";
    }
  });
  document.addEventListener("click", (event) => {
    if (!workflowRoute()) return;
    const typeFilter = event.target.closest?.("[data-sqx-data-type]");
    if (typeFilter) {
      event.preventDefault();
      filterSqxDataBox(typeFilter.closest("[data-sqx-data-box]"), { type: typeFilter.getAttribute("data-sqx-data-type") });
      return;
    }
    const cloudWord = event.target.closest?.("[data-sqx-data-cloud-word]");
    if (cloudWord) {
      event.preventDefault();
      const select = cloudWord.closest("[data-sqx-data-box]")?.querySelector("[data-settings-attribute='symbol']");
      if (select) {
        select.value = cloudWord.getAttribute("data-sqx-data-cloud-word") || "";
        persistControl(select);
      }
      return;
    }
    const resetDates = event.target.closest?.("[data-sqx-reset-dates]");
    if (resetDates) {
      event.preventDefault();
      persistControl(resetDates, resetDateUpdates(resetDates));
      return;
    }
    const newAnalysis = event.target.closest?.("[data-results-new-analysis]");
    if (newAnalysis && !newAnalysis.disabled) {
      event.preventDefault();
      document.querySelector("[data-results-new-analysis-modal]")?.showModal?.();
      return;
    }
    const closeAnalysis = event.target.closest?.("[data-results-new-analysis-close]");
    if (closeAnalysis) {
      event.preventDefault();
      closeAnalysis.closest("dialog")?.close?.();
      return;
    }
    const back = event.target.closest?.("[data-automation-back]");
    if (back) {
      event.preventDefault();
      showList();
      return;
    }
    const refresh = event.target.closest?.("[data-automation-refresh]");
    if (refresh) {
      event.preventDefault();
      reloadWorkspace();
      return;
    }
    const openExisting = event.target.closest?.("[data-automation-open-existing]");
    if (openExisting) {
      event.preventDefault();
      const list = document.querySelector("[data-automation-project-list]");
      const status = document.querySelector("[data-automation-control-status]");
      list?.scrollIntoView({ block: "start" });
      if (status) status.textContent = "Saved archives under verified user/projects are listed above.";
      return;
    }
    const open = event.target.closest?.("[data-automation-open]");
    if (open) {
      event.preventDefault();
      openProject(open.getAttribute("data-automation-open") || "", {
        tab: open.getAttribute("data-automation-open-tab") || "progress",
        section: open.getAttribute("data-automation-open-section") || "",
      });
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
        databank: tab.getAttribute("data-automation-tab") === "results" ? (searchParams().get("databank") || "") : "",
        archive: tab.getAttribute("data-automation-tab") === "results" ? (searchParams().get("archive") || "") : "",
        resultView: tab.getAttribute("data-automation-tab") === "results" ? (searchParams().get("resultView") || "overview") : "",
      });
      return;
    }
    const archiveLink = event.target.closest?.("[data-automation-archive]");
    if (archiveLink) {
      event.preventDefault();
      const row = archiveLink.closest("tr");
      row?.closest("tbody")?.querySelectorAll("tr.is-selected").forEach((node) => node.classList.remove("is-selected"));
      row?.classList.add("is-selected");
      return;
    }
    const resultView = event.target.closest?.("[data-automation-result-view]");
    if (resultView) {
      event.preventDefault();
      openProject(selectedProjectName(), {
        tab: "results",
        task: searchParams().get("task") || "",
        databank: searchParams().get("databank") || "",
        archive: searchParams().get("archive") || "",
        resultView: resultView.getAttribute("data-automation-result-view") || "trades",
        sample: searchParams().get("sample") || "",
        direction: searchParams().get("direction") || "",
      });
      return;
    }
    const az = event.target.closest?.("[data-settings-az]");
    if (az) {
      event.preventDefault();
      applyAzFilter(az);
      return;
    }
    const tabRoll = event.target.closest?.("[data-settings-tab-roll]");
    if (tabRoll) {
      event.preventDefault();
      const roll = tabRoll.closest(".settings-section-roll-wrap")?.querySelector(".settings-section-roll");
      const dir = Number(tabRoll.getAttribute("data-settings-tab-roll")) || 0;
      if (roll) roll.scrollBy({ left: dir * Math.max(180, roll.clientWidth * 0.7), behavior: "smooth" });
      return;
    }
    const dialogOpen = event.target.closest?.("[data-settings-dialog-open], [data-settings-calibrate-open]");
    if (dialogOpen) {
      event.preventDefault();
      const genericId = dialogOpen.getAttribute("data-settings-dialog-open") || "";
      const dialog = genericId
        ? document.querySelector(`[data-settings-dialog="${genericId}"]`)
        : dialogOpen.closest("[data-settings-tag='Blocks']")?.querySelector("[data-settings-calibrate]");
      dialog?.showModal?.();
      return;
    }
    const calibrateNow = event.target.closest?.("[data-settings-calibrate-now]");
    if (calibrateNow) {
      event.preventDefault();
      const dialog = calibrateNow.closest("dialog");
      const form = calibrateNow.closest("[data-automation-settings-form]");
      const task = Number(form?.getAttribute("data-settings-task") || searchParams().get("task"));
      const status = dialog?.querySelector("[data-settings-calibrate-status]")
        || form?.querySelector("[data-automation-settings-status]")
        || document.querySelector("[data-automation-settings-status]");
      const pending = dialog ? collectSettingsUpdates(dialog) : [];
      calibrateNow.disabled = true;
      void (async () => {
        try {
          if (pending.length) await saveProjectSettings(selectedProjectName(), task, pending);
          if (status) status.textContent = "Calibrating through installed StrategyQuant X…";
          const result = await requestCalibrate(selectedProjectName(), task, true);
          if (status) {
            status.textContent = `Applied ${result.updated_blocks || 0} block ranges and ${result.updated_params || 0} Level params.`;
          }
          dialog?.close?.();
          reloadWorkspace();
        } catch (error) {
          if (status) status.textContent = error instanceof Error ? error.message : "Native calibrate refused.";
        } finally {
          calibrateNow.disabled = false;
        }
      })();
      return;
    }
    const browseFiles = event.target.closest?.("[data-settings-browse-files]");
    if (browseFiles) {
      event.preventDefault();
      const kind = browseFiles.getAttribute("data-settings-browse-files") || "templates";
      const host = browseFiles.closest(".sqx-file-actions");
      const dialog = host?.querySelector("[data-settings-file-browse]");
      const list = dialog?.querySelector("[data-settings-file-browse-list]");
      const status = host?.querySelector("[data-settings-template-status]")
        || browseFiles.closest("[data-automation-settings-form]")?.querySelector("[data-automation-settings-status]");
      browseFiles.disabled = true;
      if (status) status.textContent = "Reading official StrategyQuant X files…";
      void fetchBuildTypeFiles()
        .then((files) => {
          setOfficialSqxChoices({ templateFiles: files.templates, strategyFiles: files.strategies });
          const names = kind === "strategies" ? files.strategies : files.templates;
          if (list) {
            list.innerHTML = names.length
              ? names.map((name) => `<button type="button" class="button button-secondary" data-settings-pick-file="${escapeHtml(kind)}" data-file-name="${escapeHtml(name)}"><span>${escapeHtml(name)}</span></button>`).join("")
              : `<p class="field-help">StrategyQuant X listFiles returned no ${escapeHtml(kind)}.</p>`;
          }
          if (status) status.textContent = "";
          dialog?.showModal?.();
        })
        .catch((error) => {
          if (status) status.textContent = error instanceof Error ? error.message : "Official file list unavailable. Keep StrategyQuant X open.";
        })
        .finally(() => {
          browseFiles.disabled = false;
        });
      return;
    }
    const pickFile = event.target.closest?.("[data-settings-pick-file]");
    if (pickFile) {
      event.preventDefault();
      const kind = pickFile.getAttribute("data-settings-pick-file") || "templates";
      const fileName = pickFile.getAttribute("data-file-name") || "";
      const form = pickFile.closest("[data-automation-settings-form]");
      const attribute = kind === "strategies" ? "strategyFile" : "templateFile";
      const field = form?.querySelector(`[data-settings-attribute="${attribute}"]`);
      if (field && fileName) persistControl(field, [{ path: JSON.parse(field.getAttribute("data-settings-path") || "null"), attribute, value: fileName }]);
      pickFile.closest("dialog")?.close?.();
      return;
    }
    const reloadTemplate = event.target.closest?.("[data-settings-reload-template]");
    if (reloadTemplate) {
      event.preventDefault();
      const form = reloadTemplate.closest("[data-automation-settings-form]");
      const field = form?.querySelector("[data-settings-attribute='templateFile']");
      const fileName = field ? controlValue(field) : "";
      const task = Number(form?.getAttribute("data-settings-task") || searchParams().get("task"));
      const status = form?.querySelector("[data-settings-template-status]")
        || form?.querySelector("[data-automation-settings-status]");
      reloadTemplate.disabled = true;
      if (status) status.textContent = "Reloading official template…";
      void requestTemplateReload(selectedProjectName(), task, fileName, true)
        .then(() => {
          if (status) status.textContent = "Template reloaded from StrategyQuant X.";
          reloadWorkspace();
        })
        .catch((error) => {
          if (status) status.textContent = error instanceof Error ? error.message : "Template reload refused.";
        })
        .finally(() => {
          reloadTemplate.disabled = false;
        });
      return;
    }
    const dialogSave = event.target.closest?.("[data-settings-dialog-save]");
    if (dialogSave) {
      event.preventDefault();
      const dialog = dialogSave.closest("dialog");
      persistControl(dialogSave, dialog ? collectSettingsUpdates(dialog) : []);
      dialog?.close?.();
      return;
    }
    const dialogClose = event.target.closest?.("[data-settings-dialog-close], [data-settings-calibrate-close]");
    if (dialogClose) {
      event.preventDefault();
      dialogClose.closest("dialog")?.close?.();
      return;
    }
    const block = event.target.closest?.("[data-automation-block]");
    if (block) {
      event.preventDefault();
      openProject(selectedProjectName(), {
        tab: "settings",
        task: searchParams().get("task") || "",
        section: "Blocks",
        block: block.getAttribute("data-automation-block") || "",
      });
      return;
    }
    const applyConfig = event.target.closest?.("[data-automation-apply-config]");
    if (applyConfig) {
      event.preventDefault();
      let updates = [];
      try {
        updates = JSON.parse(applyConfig.getAttribute("data-config-updates") || "[]");
      } catch {
        updates = [];
      }
      const status = document.querySelector("[data-automation-settings-status]");
      const task = Number(searchParams().get("task"));
      if (!updates.length) {
        if (status) status.textContent = "No overlapping existing fields to apply from this archive.";
        return;
      }
      void writeSettings(selectedProjectName(), task, updates, status);
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
      persistControl(flag);
      return;
    }
    const control = event.target.closest?.("[data-automation-control]");
    if (control) {
      event.preventDefault();
      const action = control.getAttribute("data-automation-control") || "";
      if (
        action === "run_project"
        || action === "stop_project"
        || action === "pause_project"
        || action === "resume_project"
      ) {
        void controlProject(control, action);
      }
    }
  });
  document.addEventListener("dblclick", (event) => {
    if (!workflowRoute()) return;
    const archiveLink = event.target.closest?.("[data-automation-archive]");
    if (!archiveLink) return;
    event.preventDefault();
    openProject(selectedProjectName(), {
      tab: "results",
      task: searchParams().get("task") || "",
      databank: archiveLink.getAttribute("data-automation-databank") || "",
      archive: archiveLink.getAttribute("data-automation-archive") || "",
      resultView: "overview",
    });
  });
  document.addEventListener("tradercockpit:shell-painted", () => {
    if (!workflowRoute()) {
      generation += 1;
      boundHost = null;
      stopProgressPoll();
      return;
    }
    scheduleBindWorkspace();
  });
  bindWorkspace();
}
