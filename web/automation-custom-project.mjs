const CONTROL_API_PATH = "/api/sqx-project-control";
const CONTROL_SCHEMA = "tc.sqx-custom-project-control.v1";
const NATIVE_CONTROL_SCHEMA = "tc.sqx-native-control.v1";

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
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

function automationRoute() {
  return globalThis.location?.pathname === "/automation";
}

async function readJson(response) {
  try { return await response.json(); } catch { return null; }
}

export function customProjectControlFromPayload(payload) {
  const project = projectName(payload?.project);
  if (
    !payload
    || payload.schema !== CONTROL_SCHEMA
    || !project
    || typeof payload.project_sha256 !== "string"
    || !/^[0-9a-f]{64}$/.test(payload.project_sha256)
    || payload.source_relative_path !== `user/projects/${project}/project.cfx`
    || !payload.execution
    || typeof payload.execution.available !== "boolean"
    || !payload.control
    || typeof payload.control.live !== "boolean"
    || typeof payload.control.run_enabled !== "boolean"
    || typeof payload.control.stop_enabled !== "boolean"
    || !payload.schedule
    || payload.schedule.enabled !== false
    || payload.schedule.reason_code !== "native_schedule_action_unavailable"
    || typeof payload.schedule.detail !== "string"
    || !payload.schedule.detail
  ) {
    throw new Error("Native Custom Project control state is invalid");
  }
  return payload;
}

export async function fetchCustomProjectControl(project, fetchImpl = globalThis.fetch) {
  const exactProject = projectName(project);
  if (!exactProject) throw new Error("Exact native project name is required");
  const path = `${CONTROL_API_PATH}?${new URLSearchParams({ project: exactProject }).toString()}`;
  const response = await fetchImpl(path, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || `Native project control request failed: ${response?.status ?? "unknown"}`);
  return customProjectControlFromPayload(payload);
}

export async function submitCustomProjectControl(project, action, fetchImpl = globalThis.fetch) {
  const exactProject = projectName(project);
  if (!exactProject) throw new Error("Exact native project name is required");
  if (action !== "run" && action !== "stop") throw new Error("Native project control action is invalid");
  const response = await fetchImpl(CONTROL_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({ project: exactProject, action }),
  });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || `Native project control ${action} failed: ${response?.status ?? "unknown"}`);
  if (payload?.schema !== NATIVE_CONTROL_SCHEMA) throw new Error("Native project control receipt is invalid");
  return payload;
}

function renderControlBody(control, executionNote) {
  const live = control.control.live;
  const pid = control.control.pid;
  const stats = [
    ["Execution gateway", control.execution.available ? "Verified" : "Unavailable"],
    ["Live native handle", live ? "Yes" : "No"],
    ["Process id", live && pid ? String(pid) : "—"],
    ["Last control detail", executionNote || control.execution.detail || "—"],
  ];
  const rows = stats.map(([label, value]) => `<div class="stat-row"><span>${escapeHtml(label)}</span><code>${escapeHtml(value)}</code></div>`).join("");
  return `<div data-automation-control-state="${live ? "live" : control.execution.available ? "ready" : "unavailable"}"><p class="field-help">Run submits native <code>sqcli.exe -project action=start</code> for one exact saved project. Stop submits native <code>action=stop</code>. TraderCockpit does not build a task-loop engine.</p>${rows}<p class="idea-save-status" data-automation-control-status></p></div>`;
}

function renderFooter(control) {
  const runDisabled = !control.control.run_enabled;
  const stopDisabled = !control.control.stop_enabled;
  const runTitle = runDisabled
    ? (control.control.live ? "Project is already running" : control.execution.detail || "Native execution is unavailable")
    : "Run the selected native Custom Project";
  const stopTitle = stopDisabled
    ? "No live native control handle for this project"
    : "Stop the live native Custom Project";
  const scheduleTitle = control.schedule?.detail || "Native schedule action is unavailable";
  return `<button class="button button-primary" type="button" data-automation-control-action="run" ${runDisabled ? "disabled" : ""} title="${escapeHtml(runTitle)}"><span>Run project</span></button><button class="button button-secondary" type="button" data-automation-control-action="stop" ${stopDisabled ? "disabled" : ""} title="${escapeHtml(stopTitle)}"><span>Stop project</span></button><button class="button button-secondary" type="button" disabled title="${escapeHtml(scheduleTitle)}"><span>Schedule</span></button>`;
}

function replaceHost(host, html) {
  host.innerHTML = html;
}

async function refreshControl(host) {
  const projectInput = document.querySelector("#native-project-name");
  const project = projectInput?.value ?? "";
  const bodyHost = host.querySelector("[data-automation-control-body]") || host;
  const footerHost = host.closest(".card")?.querySelector("[data-automation-control-footer]");
  const status = host.querySelector("[data-automation-control-status]");
  if (!projectName(project)) {
    replaceHost(bodyHost, `<div data-automation-control-state="idle"><p class="field-help">Enter one exact saved SQX project name in the topology card, then Run becomes available when the verified native gateway is ready.</p></div>`);
    if (footerHost) footerHost.innerHTML = renderFooter({ execution: { available: false, detail: "Select a project first" }, control: { live: false, run_enabled: false, stop_enabled: false }, schedule: { enabled: false, reason_code: "native_schedule_action_unavailable", detail: "Select a project first" } });
    return;
  }
  try {
    const control = await fetchCustomProjectControl(project);
    replaceHost(bodyHost, renderControlBody(control));
    if (footerHost) footerHost.innerHTML = renderFooter(control);
    if (status) status.textContent = control.control.live ? "Native Custom Project control handle is live." : "Native Custom Project control is ready.";
  } catch (error) {
    replaceHost(bodyHost, `<div data-automation-control-state="failed"><p class="field-help">${escapeHtml(error instanceof Error ? error.message : "Native project control unavailable")}</p></div>`);
    if (footerHost) footerHost.innerHTML = renderFooter({ execution: { available: false, detail: "Control read failed" }, control: { live: false, run_enabled: false, stop_enabled: false }, schedule: { enabled: false, reason_code: "native_schedule_action_unavailable", detail: "Control read failed" } });
  }
}

async function submitControl(action, button) {
  const host = document.querySelector("[data-automation-control-host]");
  if (!host) return;
  const project = document.querySelector("#native-project-name")?.value ?? "";
  const status = host.querySelector("[data-automation-control-status]");
  button.disabled = true;
  if (status) status.textContent = action === "run" ? "Submitting native project run…" : "Submitting native project stop…";
  try {
    await submitCustomProjectControl(project, action);
    if (status) status.textContent = action === "run" ? "Native project run submitted." : "Native project stop submitted.";
    await refreshControl(host);
  } catch (error) {
    if (status) status.textContent = error instanceof Error ? error.message : "Native project control failed";
    await refreshControl(host);
  } finally {
    if (button.isConnected) button.disabled = false;
  }
}

let generation = 0;

function bindAutomationControl() {
  if (!automationRoute()) return;
  const host = document.querySelector("[data-automation-control-host]");
  if (!host || host.dataset.automationControlBound === "true") return;
  host.dataset.automationControlBound = "true";
  const myGeneration = ++generation;
  void refreshControl(host);
  const projectInput = document.querySelector("#native-project-name");
  projectInput?.addEventListener("input", () => {
    if (myGeneration !== generation || !automationRoute()) return;
    void refreshControl(host);
  });
}

if (typeof document !== "undefined") {
  document.addEventListener("click", (event) => {
    if (!automationRoute()) return;
    const button = event.target.closest?.("[data-automation-control-action]");
    if (!button || button.disabled) return;
    const action = button.getAttribute("data-automation-control-action");
    if (action !== "run" && action !== "stop") return;
    event.preventDefault();
    void submitControl(action, button);
  });
  const observer = new MutationObserver(() => {
    if (!automationRoute()) {
      generation += 1;
      return;
    }
    bindAutomationControl();
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  bindAutomationControl();
}
