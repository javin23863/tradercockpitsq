import {
  fetchHistoricalResults,
  fetchRuntimeStatus,
  historicalResultFromPayload,
  retesterRuntimeReady,
} from "./research-backtest.mjs";

const ROBUSTNESS_API_PATH = "/api/research/robustness";
const ROBUSTNESS_SCHEMA = "tc.research-native-robustness.v1";
const HIGHER_PRECISION_METHOD = "RetestWithHigherPrecision";
const OUTCOME_UNREAD = "producer_result_captured_outcome_unread";

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function robustnessRoute() {
  if (globalThis.location?.pathname !== "/research") return false;
  const params = new URLSearchParams(globalThis.location.search || "");
  return params.get("stage") === "backtest" && params.get("tab") === "robustness";
}

function validationRefFromLocation() {
  const value = new URLSearchParams(globalThis.location?.search || "").get("validationRef");
  return typeof value === "string" && /^tc-evidence:sha256:[0-9a-f]{64}$/.test(value) ? value : "";
}

function digest(value) {
  return typeof value === "string" && /^[0-9a-f]{64}$/.test(value) ? value : "";
}

function evidenceDigest(value) {
  const prefix = "tc-evidence:sha256:";
  return typeof value === "string" && value.startsWith(prefix) && digest(value.slice(prefix.length))
    ? value.slice(prefix.length)
    : "";
}

async function readJson(response) {
  try { return await response.json(); } catch { return null; }
}

function apiError(response, payload, fallback) {
  const error = new Error(payload?.detail || fallback);
  error.status = response?.status || 0;
  error.payload = payload;
  return error;
}

export function robustnessResultFromPayload(payload) {
  const requiredStrings = [
    "validation_ref",
    "source_historical_result_entity_id",
    "source_historical_result_revision",
    "source_result_archive_ref",
    "source_result_archive_sha256",
    "source_project_ref",
    "source_project_sha256",
    "compiled_project_ref",
    "compiled_project_sha256",
    "source_task_sha256",
    "compiled_task_sha256",
    "engine_ref",
    "engine_sha256",
    "launcher_sha256",
    "native_project_name",
    "native_project_relative_path",
    "result_archive_name",
    "result_archive_ref",
    "result_archive_sha256",
    "result_strategy_ref",
    "result_strategy_sha256",
    "result_settings_ref",
    "result_settings_sha256",
  ];
  if (
    !payload
    || payload.schema !== ROBUSTNESS_SCHEMA
    || payload.sqx_build !== "144.2953"
    || payload.operation !== "native_retester_cross_check"
    || payload.method !== HIGHER_PRECISION_METHOD
    || payload.execution_state !== "completed"
    || payload.producer_outcome_state !== OUTCOME_UNREAD
    || typeof payload.configuration_changed !== "boolean"
    || requiredStrings.some((key) => typeof payload[key] !== "string" || !payload[key])
  ) {
    throw new Error("Native robustness result identity is invalid");
  }
  const evidenceBindings = [
    ["source_result_archive_ref", "source_result_archive_sha256"],
    ["source_project_ref", "source_project_sha256"],
    ["compiled_project_ref", "compiled_project_sha256"],
    ["engine_ref", "engine_sha256"],
    ["result_archive_ref", "result_archive_sha256"],
    ["result_strategy_ref", "result_strategy_sha256"],
    ["result_settings_ref", "result_settings_sha256"],
  ];
  for (const [refKey, digestKey] of evidenceBindings) {
    if (!digest(payload[digestKey]) || evidenceDigest(payload[refKey]) !== payload[digestKey]) {
      throw new Error("Native robustness evidence binding is invalid");
    }
  }
  if (
    evidenceDigest(payload.validation_ref) === ""
    || !digest(payload.source_task_sha256)
    || !digest(payload.compiled_task_sha256)
    || !digest(payload.launcher_sha256)
    || !/^tc-research:historical-result:v1:[0-9a-f-]{36}$/.test(payload.source_historical_result_entity_id)
    || !/^tc-research-revision:historical-result:sha256:[0-9a-f]{64}$/.test(payload.source_historical_result_revision)
    || !/^TraderCockpit-Retester-[0-9a-f]{32}$/.test(payload.native_project_name)
    || payload.native_project_relative_path !== `user/projects/${payload.native_project_name}/project.cfx`
    || payload.result_archive_sha256 === payload.source_result_archive_sha256
    || !Array.isArray(payload.receipts)
    || payload.receipts.length !== 1
    || payload.receipts[0]?.action !== "startOnlyTask"
    || payload.receipts[0]?.task !== 1
    || payload.receipts[0]?.state !== "completed"
    || payload.receipts[0]?.launcher_sha256 !== payload.launcher_sha256
    || !payload.native_settings
    || typeof payload.native_settings !== "object"
    || typeof payload.native_settings.Precision !== "string"
    || !payload.native_settings.Precision
    || typeof payload.native_settings.Spread !== "string"
    || !payload.native_settings.Spread
  ) {
    throw new Error("Native robustness custody is inconsistent");
  }
  return payload;
}

export async function fetchRobustnessResult(validationRef, fetchImpl = globalThis.fetch) {
  if (!/^tc-evidence:sha256:[0-9a-f]{64}$/.test(validationRef || "")) {
    throw new Error("Robustness validation reference is invalid");
  }
  const response = await fetchImpl(`${ROBUSTNESS_API_PATH}?validationRef=${encodeURIComponent(validationRef)}`, {
    headers: { accept: "application/json" },
  });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Robustness result read failed");
  return robustnessResultFromPayload(payload);
}

export async function startHigherPrecision(historicalResult, fetchImpl = globalThis.fetch) {
  const source = historicalResultFromPayload(historicalResult);
  if (source.state !== "completed" || source.execution_completed !== true) {
    throw new Error("Higher Precision requires a completed Historical Result");
  }
  const response = await fetchImpl(ROBUSTNESS_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({
      action: "start-higher-precision",
      historical_result_entity_id: source.entity_id,
      expected_historical_result_revision: source.revision,
    }),
  });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Native Higher Precision execution failed");
  const result = robustnessResultFromPayload(payload);
  if (
    result.source_historical_result_entity_id !== source.entity_id
    || result.source_historical_result_revision !== source.revision
    || result.source_result_archive_sha256 !== source.result_archive_sha256
  ) {
    throw new Error("Robustness result does not bind the selected Historical Result revision");
  }
  return result;
}

function short(value) {
  const text = String(value || "");
  return text.length > 24 ? `…${text.slice(-22)}` : text;
}

function resultPanel(result) {
  if (!result) {
    return `<div class="empty-state"><div class="empty-icon">—</div><div><strong>No native robustness run selected</strong><p>Choose one completed baseline Historical Result and run Higher Precision through installed SQX.</p></div></div>`;
  }
  return `<div data-robustness-result="${escapeHtml(result.validation_ref)}">
    <div class="context-callout"><span class="callout-icon">↳</span><div><span class="eyebrow">Native SQX output captured</span><strong>Higher Precision execution completed</strong><span>Producer output is in immutable custody. No robustness pass/fail is inferred until an authoritative SQX outcome readback seam is connected.</span></div></div>
    <div class="idea-identity">
      <div class="stat-row"><span>Validation evidence</span><code>${escapeHtml(result.validation_ref)}</code></div>
      <div class="stat-row"><span>Source Historical Result</span><code>${escapeHtml(result.source_historical_result_revision)}</code></div>
      <div class="stat-row"><span>Native method</span><code>${escapeHtml(result.method)}</code></div>
      <div class="stat-row"><span>Precision</span><code>${escapeHtml(result.native_settings.Precision)}</code></div>
      <div class="stat-row"><span>Spread</span><code>${escapeHtml(result.native_settings.Spread)}</code></div>
      <div class="stat-row"><span>Config mutation</span><code>${result.configuration_changed ? "Existing profile enabled in isolated snapshot" : "Exact installed project already enabled"}</code></div>
      <div class="stat-row"><span>Engine SHA-256</span><code>${escapeHtml(result.engine_sha256)}</code></div>
      <div class="stat-row"><span>Launcher SHA-256</span><code>${escapeHtml(result.launcher_sha256)}</code></div>
      <div class="stat-row"><span>Native result archive</span><code>${escapeHtml(result.result_archive_sha256)}</code></div>
    </div>
    <div class="requirement-item"><div><strong>Producer outcome</strong><span class="status-badge status-unavailable"><span class="status-dot"></span>Outcome unread</span></div><p>The exact native result exists. TraderCockpit has not yet claimed an SQX robustness verdict or reconstructed one from process completion.</p></div>
  </div>`;
}

function methodRows() {
  const nativeLater = [
    ["Additional Markets", "Native cross-market retest — producer path not connected in this slice."],
    ["Monte Carlo · trade manipulation", "Native trade-manipulation family — not executed by TraderCockpit locally."],
    ["Monte Carlo · full retest", "Native full-retest family — producer path not connected in this slice."],
    ["System Parameter Permutation", "Native optimization profile — producer path not connected in this slice."],
    ["Walk-Forward / Matrix", "Native optimization/validation family — producer path not connected in this slice."],
  ];
  return `<div class="requirement-list" data-robustness-methods>
    <div class="requirement-item"><div><strong>Higher Precision</strong><span class="status-badge status-ready"><span class="status-dot"></span>Native execution wired</span></div><p>Uses the current installed Retester profile and its existing Precision/Spread settings.</p></div>
    ${nativeLater.map(([name, detail]) => `<div class="requirement-item"><div><strong>${escapeHtml(name)}</strong><span class="status-badge status-unavailable"><span class="status-dot"></span>Not connected</span></div><p>${escapeHtml(detail)}</p></div>`).join("")}
  </div>`;
}

let generation = 0;
let state = { phase: "idle", results: [], selectedIndex: 0, runtimeReady: false, validation: null, detail: "" };

function panel() {
  if (!robustnessRoute()) return null;
  return document.querySelector('.content-inner .panel.wide-panel[data-accent="orange"]');
}

function render(host, current) {
  if (!host?.isConnected) return;
  const completed = current.results.filter((item) => item.state === "completed" && item.execution_completed === true);
  const selected = completed[current.selectedIndex] || null;
  const canRun = current.phase !== "loading" && current.runtimeReady && selected;
  host.querySelector(".empty-state")?.remove();
  let workspace = host.querySelector("[data-robustness-workspace]");
  if (!workspace) {
    workspace = document.createElement("div");
    workspace.dataset.robustnessWorkspace = "";
    host.append(workspace);
  }
  workspace.innerHTML = `<div class="dashboard-grid">
    <section class="panel" data-accent="orange">
      <div class="panel-heading"><div><p class="eyebrow">Producer-backed plan</p><h2>Native robustness methods</h2></div></div>
      ${methodRows()}
    </section>
    <section class="panel" data-accent="cyan">
      <div class="panel-heading"><div><p class="eyebrow">Exact input</p><h2>Higher Precision</h2></div></div>
      <label class="field-label" for="robustness-source-result">Completed baseline Historical Result</label>
      <select id="robustness-source-result" class="idea-editor" ${completed.length ? "" : "disabled"}>${completed.length ? completed.map((item, index) => `<option value="${index}" ${index === current.selectedIndex ? "selected" : ""}>${escapeHtml(item.result_archive_name)} · ${escapeHtml(short(item.revision))}</option>`).join("") : '<option>No completed Historical Results</option>'}</select>
      ${selected ? `<div class="idea-identity"><div class="stat-row"><span>Historical Result</span><code>${escapeHtml(selected.entity_id)}</code></div><div class="stat-row"><span>Revision</span><code>${escapeHtml(selected.revision)}</code></div><div class="stat-row"><span>Source archive</span><code>${escapeHtml(selected.result_archive_sha256)}</code></div></div>` : ""}
      <p class="field-help">TraderCockpit supplies only exact Historical Result identity. The installed Retester project owns the Higher Precision profile and native settings.</p>
      <button class="button button-primary" type="button" data-robustness-action="start" ${canRun ? "" : "disabled"}>${current.runtimeReady ? "Run native Higher Precision" : "Native Retester unavailable"}</button>
      <p class="idea-save-status" data-robustness-status>${escapeHtml(current.detail || "")}</p>
    </section>
    <section class="panel" data-accent="purple"><div class="panel-heading"><div><p class="eyebrow">Immutable readback</p><h2>Robustness result custody</h2></div></div>${resultPanel(current.validation)}</section>
  </div>`;
}

function persistValidationRef(validationRef) {
  if (!globalThis.history?.replaceState || !globalThis.location) return;
  const url = new URL(globalThis.location.href);
  url.searchParams.set("stage", "backtest");
  url.searchParams.set("tab", "robustness");
  url.searchParams.set("validationRef", validationRef);
  globalThis.history.replaceState({}, "", `${url.pathname}${url.search}`);
}

async function load() {
  const currentGeneration = ++generation;
  const host = panel();
  if (!host) return;
  state = { ...state, phase: "loading", detail: "Loading native robustness custody…" };
  render(host, state);
  try {
    const requestedRef = validationRefFromLocation();
    const [results, runtime, validation] = await Promise.all([
      fetchHistoricalResults(),
      fetchRuntimeStatus(),
      requestedRef ? fetchRobustnessResult(requestedRef) : Promise.resolve(null),
    ]);
    if (currentGeneration !== generation || !robustnessRoute()) return;
    state = {
      phase: "loaded",
      results,
      selectedIndex: 0,
      runtimeReady: retesterRuntimeReady(runtime),
      validation,
      detail: "",
    };
  } catch (error) {
    if (currentGeneration !== generation || !robustnessRoute()) return;
    state = { ...state, phase: "failed", detail: error instanceof Error ? error.message : "Native robustness workspace unavailable" };
  }
  render(panel(), state);
}

async function start(button) {
  if (state.phase === "loading" || !state.runtimeReady) return;
  const completed = state.results.filter((item) => item.state === "completed" && item.execution_completed === true);
  const selected = completed[state.selectedIndex];
  if (!selected) return;
  button.disabled = true;
  button.textContent = "Running Higher Precision in SQX…";
  try {
    const validation = await startHigherPrecision(selected);
    if (!robustnessRoute()) return;
    persistValidationRef(validation.validation_ref);
    state = { ...state, phase: "loaded", validation, detail: "Native Higher Precision result captured. Producer verdict remains unread." };
  } catch (error) {
    state = { ...state, phase: "failed", detail: error instanceof Error ? error.message : "Native Higher Precision execution failed" };
  }
  render(panel(), state);
}

if (typeof document !== "undefined") {
  document.addEventListener("change", (event) => {
    if (!robustnessRoute() || event.target?.id !== "robustness-source-result") return;
    const selectedIndex = Number(event.target.value);
    const completed = state.results.filter((item) => item.state === "completed" && item.execution_completed === true);
    if (!Number.isInteger(selectedIndex) || selectedIndex < 0 || selectedIndex >= completed.length) return;
    state = { ...state, selectedIndex, validation: null, detail: "" };
    render(panel(), state);
  });
  document.addEventListener("click", (event) => {
    const button = event.target?.closest?.('[data-robustness-action="start"]');
    if (button && robustnessRoute()) void start(button);
  });
  document.addEventListener("locationchange", () => { if (robustnessRoute()) void load(); });
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => { if (robustnessRoute()) void load(); }, { once: true });
  } else if (robustnessRoute()) {
    queueMicrotask(load);
  }
}
