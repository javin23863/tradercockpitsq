const SQX_PROJECT_TOPOLOGY_API_PATH = "/api/sqx-project-topology";
const CUSTOM_PROJECT_TOPOLOGY_SCHEMA = "tc.sqx-custom-project-topology.v1";
const SQX_BUILD = "144.2953";

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

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

export function customProjectTopologyFromPayload(payload) {
  const project = projectName(payload?.project);
  if (
    !payload
    || payload.schema !== CUSTOM_PROJECT_TOPOLOGY_SCHEMA
    || payload.source_build !== SQX_BUILD
    || !project
    || payload.source_relative_path !== `user/projects/${project}/project.cfx`
    || !digest(payload.archive_sha256)
    || !Array.isArray(payload.internal_entries)
    || payload.internal_entries.some((value) => typeof value !== "string" || !value)
    || new Set(payload.internal_entries).size !== payload.internal_entries.length
    || !payload.internal_entries.includes("config.xml")
    || !Array.isArray(payload.tasks)
    || payload.execution?.supported !== false
    || payload.execution?.reason !== "topology_custody_only"
  ) {
    throw new Error("Native Custom Project topology is invalid");
  }

  const archiveEntries = new Set(payload.internal_entries);
  const indexes = new Set();
  let previousIndex = 0;
  for (const task of payload.tasks) {
    if (
      !task
      || !Number.isInteger(task.native_task_index)
      || task.native_task_index < 1
      || task.native_task_index <= previousIndex
      || indexes.has(task.native_task_index)
      || typeof task.kind !== "string" || !/^[A-Za-z][A-Za-z0-9]*$/.test(task.kind)
      || task.entry_name !== `${task.kind}-Task${task.native_task_index}.xml`
      || !archiveEntries.has(task.entry_name)
      || !Array.isArray(task.clear_databanks)
      || task.clear_databanks.some((value) => typeof value !== "string" || !value)
      || new Set(task.clear_databanks).size !== task.clear_databanks.length
      || (task.goto_target_label !== null && (typeof task.goto_target_label !== "string" || !task.goto_target_label))
    ) {
      throw new Error("Native Custom Project task topology is invalid");
    }
    indexes.add(task.native_task_index);
    previousIndex = task.native_task_index;
  }
  return payload;
}

async function readJson(response) {
  try { return await response.json(); } catch { return null; }
}

export async function fetchCustomProjectTopology(project, fetchImpl = globalThis.fetch) {
  const exactProject = projectName(project);
  if (!exactProject) throw new Error("Exact native project name is required");
  if (typeof fetchImpl !== "function") throw new Error("Native project topology fetch is unavailable");
  const path = `${SQX_PROJECT_TOPOLOGY_API_PATH}?${new URLSearchParams({ project: exactProject }).toString()}`;
  const response = await fetchImpl(path, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || `Native project topology request failed: ${response?.status ?? "unknown"}`);
  return customProjectTopologyFromPayload(payload);
}

function taskDetail(task) {
  const details = [];
  if (task.clear_databanks.length) details.push(`Databanks: ${task.clear_databanks.join(", ")}`);
  if (task.goto_target_label) details.push(`Target: ${task.goto_target_label}`);
  return details.length ? details.join(" · ") : "Producer semantics preserved opaquely";
}

export function renderCustomProjectTopologyResult(payload) {
  const topology = customProjectTopologyFromPayload(payload);
  const tasks = topology.tasks.length
    ? topology.tasks.map((task) => `<div class="stat-row" data-native-project-task="${task.native_task_index}"><span>${task.native_task_index} · ${escapeHtml(task.kind)}</span><code>${escapeHtml(taskDetail(task))}</code></div>`).join("")
    : '<p class="field-help">This saved native project contains no numbered task entries.</p>';
  return `<div data-native-project-topology-result><div class="context-callout"><span class="callout-icon">↳</span><div><span class="eyebrow">Exact native project snapshot</span><strong>${escapeHtml(topology.project)}</strong><span>Read-only topology custody. TraderCockpit does not execute or reconstruct the native task loop from this record.</span></div></div><div class="idea-identity"><div class="stat-row"><span>Project archive SHA-256</span><code>${escapeHtml(topology.archive_sha256)}</code></div><div class="stat-row"><span>Source path</span><code>${escapeHtml(topology.source_relative_path)}</code></div><div class="stat-row"><span>Native build</span><code>${escapeHtml(topology.source_build)}</code></div></div><div class="requirement-list">${tasks}</div></div>`;
}

function specificationRoute() {
  if (globalThis.location?.pathname !== "/research") return false;
  const params = new URLSearchParams(globalThis.location.search || "");
  return params.get("stage") === "construct" && params.get("tab") === "specification";
}

let generation = 0;
let boundHost = null;

function capabilityHost() {
  if (!specificationRoute()) return null;
  return document.querySelector('[data-research-capability="native_custom_project_topology"]');
}

function bindInspector() {
  const host = capabilityHost();
  if (!host || host === boundHost) return;
  boundHost = host;
  const workspace = document.createElement("div");
  workspace.dataset.nativeProjectTopologyWorkspace = "idle";
  workspace.innerHTML = `<label class="field-label" for="native-project-name">Exact saved SQX project name</label><input id="native-project-name" class="idea-editor" type="text" autocomplete="off" placeholder="Project folder name under user/projects" /><p class="field-help">Enter one exact direct user/projects child. The backend resolves and physically contains project.cfx before reading it.</p><div class="idea-actions"><button class="button button-secondary" type="button" data-native-project-action="inspect">Inspect topology</button></div><p class="idea-save-status" data-native-project-status></p><div data-native-project-result></div>`;
  host.append(workspace);
}

async function inspectProject(button) {
  const workspace = button.closest?.("[data-native-project-topology-workspace]");
  if (!workspace) return;
  const project = workspace.querySelector("#native-project-name")?.value ?? "";
  const status = workspace.querySelector("[data-native-project-status]");
  const result = workspace.querySelector("[data-native-project-result]");
  const myGeneration = ++generation;
  workspace.dataset.nativeProjectTopologyWorkspace = "loading";
  button.disabled = true;
  if (status) status.textContent = "Reading exact native project topology…";
  if (result) result.innerHTML = "";
  try {
    const topology = await fetchCustomProjectTopology(project);
    if (myGeneration !== generation || !specificationRoute() || !workspace.isConnected) return;
    workspace.dataset.nativeProjectTopologyWorkspace = "loaded";
    if (status) status.textContent = "Exact native project topology loaded.";
    if (result) result.innerHTML = renderCustomProjectTopologyResult(topology);
  } catch (error) {
    if (myGeneration !== generation || !specificationRoute() || !workspace.isConnected) return;
    workspace.dataset.nativeProjectTopologyWorkspace = "failed";
    if (status) status.textContent = error instanceof Error ? error.message : "Native project topology unavailable";
  } finally {
    if (myGeneration === generation && workspace.isConnected) button.disabled = false;
  }
}

if (typeof document !== "undefined") {
  document.addEventListener("click", (event) => {
    if (!specificationRoute()) return;
    const button = event.target.closest?.('[data-native-project-action="inspect"]');
    if (!button) return;
    event.preventDefault();
    void inspectProject(button);
  });
  const observer = new MutationObserver(() => {
    if (!specificationRoute()) {
      generation += 1;
      boundHost = null;
      return;
    }
    bindInspector();
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  bindInspector();
}
