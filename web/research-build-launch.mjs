import { researchLocationMatches } from "./model.mjs";
const CONFIGURATION_API_PATH = "/api/research/configurations";
const NATIVE_JOBS_API_PATH = "/api/research/native-jobs";
const STATUS_API_PATH = "/api/status";
const NATIVE_JOB_SCHEMA = "tc.research-native-job.v1";
const NATIVE_JOB_CATALOG_SCHEMA = "tc.research-native-job-catalog.v1";
const NATIVE_JOB_OPERATION = "builder_loadconfig_start";

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function buildRoute() {
  return researchLocationMatches(globalThis.location, "evolution");
}

function selectedConfigurationEntity() {
  const params = new URLSearchParams(globalThis.location?.search || "");
  return params.get("configuration") || "";
}

async function readJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

function apiError(response, payload, fallback) {
  const error = new Error(payload?.detail || fallback);
  error.status = response?.status || 0;
  error.payload = payload;
  return error;
}

function configurationFromPayload(payload) {
  if (
    !payload
    || payload.schema !== "tc.research-configuration.v1"
    || typeof payload.entity_id !== "string"
    || typeof payload.revision !== "string"
    || !["compiled", "approved"].includes(payload.state)
    || !payload.approval
    || typeof payload.approval.approved !== "boolean"
    || typeof payload.executable_xml_sha256 !== "string"
  ) {
    throw new Error("Research configuration identity is invalid");
  }
  if (payload.state === "approved" && payload.approval.approved !== true) {
    throw new Error("Approved configuration state is inconsistent");
  }
  return payload;
}

export function nativeExecutionFromStatus(payload) {
  const execution = payload?.research_backend?.execution;
  if (payload?.schema !== "tc.runtime-status.v1" || !execution || typeof execution !== "object") {
    throw new Error("Native execution status is invalid");
  }
  const available = execution.available === true
    && execution.gateway_available === true
    && execution.launcher_verified === true;
  return Object.freeze({
    available,
    reason_code: available ? null : String(execution.reason_code || "native_execution_unavailable"),
    launcher_sha256: available && typeof execution.launcher_sha256 === "string" ? execution.launcher_sha256 : null,
  });
}

export function nativeJobFromPayload(payload) {
  const strings = [
    "entity_id",
    "revision",
    "configuration_entity_id",
    "configuration_revision",
    "executable_xml_ref",
    "executable_xml_sha256",
    "sqx_build",
    "operation",
    "staged_config_relative_path",
  ];
  if (!payload || payload.schema !== NATIVE_JOB_SCHEMA || strings.some((key) => typeof payload[key] !== "string" || !payload[key])) {
    throw new Error("Native job identity is invalid");
  }
  if (!/^[0-9a-f]{64}$/.test(payload.executable_xml_sha256) || payload.operation !== NATIVE_JOB_OPERATION) {
    throw new Error("Native job executable/control identity is invalid");
  }
  if (!payload.executable_xml_ref.endsWith(payload.executable_xml_sha256)) {
    throw new Error("Native job executable evidence is inconsistent");
  }
  if (!["prepared", "submitted", "failed"].includes(payload.state) || !Array.isArray(payload.receipts)) {
    throw new Error("Native job state is invalid");
  }
  if (payload.state === "submitted") {
    if (
      payload.partial_side_effect !== false
      || payload.failure_reason_code !== null
      || typeof payload.launcher_sha256 !== "string"
      || !/^[0-9a-f]{64}$/.test(payload.launcher_sha256)
      || payload.receipts.length !== 2
      || payload.receipts.some((receipt) => receipt?.state !== "completed")
    ) {
      throw new Error("Submitted native job receipt is inconsistent");
    }
  }
  if (payload.state === "failed" && (typeof payload.failure_reason_code !== "string" || !payload.failure_reason_code)) {
    throw new Error("Failed native job refusal is incomplete");
  }
  return payload;
}

export function nativeJobCatalogFromPayload(payload) {
  if (!payload || payload.schema !== NATIVE_JOB_CATALOG_SCHEMA || !Array.isArray(payload.jobs)) {
    throw new Error("Native job catalog schema mismatch");
  }
  return payload.jobs.map(nativeJobFromPayload);
}

async function fetchConfiguration(entityId, fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(`${CONFIGURATION_API_PATH}?entityId=${encodeURIComponent(entityId)}`, {
    headers: { accept: "application/json" },
  });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Configuration read failed");
  return configurationFromPayload(payload);
}

export async function fetchNativeExecution(fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(STATUS_API_PATH, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Runtime status request failed");
  return nativeExecutionFromStatus(payload);
}

export async function fetchNativeJobsForConfiguration(configurationRevision, fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(
    `${NATIVE_JOBS_API_PATH}?configurationRevision=${encodeURIComponent(configurationRevision)}`,
    { headers: { accept: "application/json" } },
  );
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Native job read failed");
  return nativeJobCatalogFromPayload(payload);
}

export async function launchApprovedBuilder(configuration, fetchImpl = globalThis.fetch) {
  if (!configuration || configuration.state !== "approved" || configuration.approval?.approved !== true) {
    throw new Error("Native Builder launch requires the exact approved configuration");
  }
  const response = await fetchImpl(NATIVE_JOBS_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({
      action: "launch-builder",
      configuration_entity_id: configuration.entity_id,
      expected_configuration_revision: configuration.revision,
    }),
  });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Native Builder launch failed");
  const job = nativeJobFromPayload(payload);
  if (
    job.configuration_entity_id !== configuration.entity_id
    || job.configuration_revision !== configuration.revision
    || job.executable_xml_sha256 !== configuration.executable_xml_sha256
  ) {
    throw new Error("Native job does not bind the selected approved configuration");
  }
  return job;
}

function receiptMarkup(receipts) {
  return receipts.map((receipt) => `<div class="stat-row"><span>${escapeHtml(receipt.action)}</span><code>${escapeHtml(receipt.state)}${receipt.exit_code === null ? "" : ` / ${escapeHtml(receipt.exit_code)}`}</code></div>`).join("");
}

function renderLaunchGate(gate, configuration, execution, job, detail = "") {
  if (!gate?.isConnected) return;
  if (job) {
    const submitted = job.state === "submitted";
    gate.dataset.buildLaunchGate = submitted ? "submitted" : job.state;
    gate.innerHTML = `
      <div><strong>Native Builder job</strong><span class="status-badge status-${submitted ? "ready" : "unavailable"}"><span class="status-dot"></span>${escapeHtml(submitted ? "Submitted" : job.state)}</span></div>
      <p>${submitted ? "The exact approved configuration was loaded and Builder start was submitted through the trusted native gateway." : escapeHtml(job.failure_reason_code || detail || "Native control did not complete.")}</p>
      <div class="idea-identity">
        <div class="stat-row"><span>Native job</span><code>${escapeHtml(job.entity_id)}</code></div>
        <div class="stat-row"><span>Job revision</span><code>${escapeHtml(job.revision)}</code></div>
        <div class="stat-row"><span>Approved config</span><code>${escapeHtml(job.configuration_revision)}</code></div>
        <div class="stat-row"><span>Staged config</span><code>${escapeHtml(job.staged_config_relative_path)}</code></div>
        ${job.launcher_sha256 ? `<div class="stat-row"><span>Launcher SHA-256</span><code>${escapeHtml(job.launcher_sha256)}</code></div>` : ""}
        ${receiptMarkup(job.receipts)}
      </div>`;
    return;
  }

  if (configuration.state !== "approved" || configuration.approval?.approved !== true) {
    gate.dataset.buildLaunchGate = "approval-required";
    gate.innerHTML = `
      <div><strong>Native launch</strong><span class="status-badge status-unavailable"><span class="status-dot"></span>Approval required</span></div>
      <p>Approve the exact configuration revision before native control is available.</p>
      <button class="button button-secondary" type="button" disabled>Launch Builder</button>`;
    return;
  }

  if (!execution?.available) {
    gate.dataset.buildLaunchGate = "runtime-unavailable";
    gate.innerHTML = `
      <div><strong>Native launch</strong><span class="status-badge status-unavailable"><span class="status-dot"></span>Runtime unavailable</span></div>
      <p>${escapeHtml(detail || execution?.reason_code || "Native execution trust is unavailable.")}</p>
      <button class="button button-secondary" type="button" disabled>Launch Builder</button>`;
    return;
  }

  gate.dataset.buildLaunchGate = "ready";
  gate.innerHTML = `
    <div><strong>Native launch</strong><span class="status-badge status-ready"><span class="status-dot"></span>Ready</span></div>
    <p>The server will stage the exact approved Builder task as a Task-rooted <code>.cfx</code> inside the verified SQX runtime, freshly reverify launcher/config identity, then submit only <code>loadconfig → start</code>.</p>
    <div class="stat-row"><span>Verified launcher</span><code>${escapeHtml(execution.launcher_sha256 || "")}</code></div>
    ${detail ? `<p class="field-help">${escapeHtml(detail)}</p>` : ""}
    <button class="button button-secondary" type="button" data-native-builder-launch>Launch Builder</button>`;
}

let bindGeneration = 0;
let activeRevision = "";
let activeConfiguration = null;
let activeExecution = null;

async function bindLaunchGate() {
  const generation = ++bindGeneration;
  if (!buildRoute()) {
    activeRevision = "";
    activeConfiguration = null;
    activeExecution = null;
    return;
  }
  const gate = globalThis.document?.querySelector?.("[data-build-launch-gate]");
  const entityId = selectedConfigurationEntity();
  if (!gate || !entityId) return;
  try {
    const configuration = await fetchConfiguration(entityId);
    if (generation !== bindGeneration || !buildRoute() || selectedConfigurationEntity() !== entityId) return;
    activeRevision = configuration.revision;
    activeConfiguration = configuration;
    const [jobs, execution] = await Promise.all([
      fetchNativeJobsForConfiguration(configuration.revision),
      fetchNativeExecution(),
    ]);
    if (generation !== bindGeneration || !buildRoute() || selectedConfigurationEntity() !== entityId) return;
    if (jobs.length > 1) throw new Error("Multiple native jobs bind the same approved configuration");
    activeExecution = execution;
    renderLaunchGate(gate, configuration, execution, jobs[0] || null);
  } catch (error) {
    if (generation !== bindGeneration || !gate?.isConnected) return;
    const detail = error instanceof Error ? error.message : "Native launch state unavailable";
    if (activeConfiguration) renderLaunchGate(gate, activeConfiguration, activeExecution, null, detail);
  }
}

async function handleLaunch(button) {
  if (
    !activeConfiguration
    || !activeRevision
    || activeConfiguration.revision !== activeRevision
    || activeExecution?.available !== true
  ) return;
  button.disabled = true;
  button.textContent = "Submitting native Builder…";
  try {
    const job = await launchApprovedBuilder(activeConfiguration);
    if (!buildRoute() || selectedConfigurationEntity() !== activeConfiguration.entity_id) return;
    const gate = globalThis.document?.querySelector?.("[data-build-launch-gate]");
    renderLaunchGate(gate, activeConfiguration, activeExecution, job);
  } catch (error) {
    if (!buildRoute()) return;
    const gate = globalThis.document?.querySelector?.("[data-build-launch-gate]");
    renderLaunchGate(gate, activeConfiguration, activeExecution, null, error instanceof Error ? error.message : "Native Builder launch failed");
  }
}

if (typeof document !== "undefined") {
  document.addEventListener("click", (event) => {
    const button = event.target?.closest?.("[data-native-builder-launch]");
    if (button && buildRoute()) void handleLaunch(button);
  });
  const observer = new MutationObserver(() => { void bindLaunchGate(); });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void bindLaunchGate();
}
