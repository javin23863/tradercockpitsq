import {
  fetchHistoricalResults,
  fetchRuntimeStatus,
  historicalResultFromPayload,
  retesterRuntimeReady,
} from "./research-backtest.mjs";

const HISTORICAL_RESULTS_API_PATH = "/api/research/historical-results";
const ROBUSTNESS_SCHEMA = "tc.research-native-robustness.v1";
const HIGHER_PRECISION_METHOD = "RetestWithHigherPrecision";
const SYSTEM_PARAMETER_METHOD = "OptProfileSysParamPermutation";
const OUTCOME_UNREAD = "producer_result_captured_outcome_unread";
const SUPPORTED_METHODS = new Set([HIGHER_PRECISION_METHOD, SYSTEM_PARAMETER_METHOD]);

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

function methodFromLocation() {
  const value = new URLSearchParams(globalThis.location?.search || "").get("robustnessMethod");
  return SUPPORTED_METHODS.has(value) ? value : HIGHER_PRECISION_METHOD;
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

function nativeSettingsValid(method, settings) {
  if (!settings || typeof settings !== "object") return false;
  if (method === HIGHER_PRECISION_METHOD) {
    return typeof settings.Precision === "string" && settings.Precision.length > 0
      && typeof settings.Spread === "string" && settings.Spread.length > 0;
  }
  if (method === SYSTEM_PARAMETER_METHOD) {
    const periods = typeof settings.OptimPeriods === "string" ? settings.OptimPeriods.toLowerCase() : "";
    const exits = typeof settings.OptimExitTypes === "string" ? settings.OptimExitTypes.toLowerCase() : "";
    const maxTests = typeof settings.MaxTests === "string" ? settings.MaxTests : "";
    return ["true", "false"].includes(periods)
      && ["true", "false"].includes(exits)
      && /^[0-9]+$/.test(maxTests)
      && !/^0+$/.test(maxTests);
  }
  return false;
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
    || !SUPPORTED_METHODS.has(payload.method)
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
    || payload.receipts[0]?.project !== payload.native_project_name
    || payload.receipts[0]?.project_sha256 !== payload.compiled_project_sha256
    || payload.receipts[0]?.engine_sha256 !== payload.engine_sha256
    || payload.receipts[0]?.launcher_sha256 !== payload.launcher_sha256
    || !nativeSettingsValid(payload.method, payload.native_settings)
  ) {
    throw new Error("Native robustness custody is inconsistent");
  }
  return payload;
}

function readActionForMethod(method) {
  if (method === HIGHER_PRECISION_METHOD) return "read-robustness";
  if (method === SYSTEM_PARAMETER_METHOD) return "read-system-parameter-permutation";
  throw new Error("Native robustness method is unsupported");
}

function startActionForMethod(method) {
  if (method === HIGHER_PRECISION_METHOD) return "start-higher-precision";
  if (method === SYSTEM_PARAMETER_METHOD) return "start-system-parameter-permutation";
  throw new Error("Native robustness method is unsupported");
}

export async function fetchRobustnessResult(
  validationRef,
  methodOrFetch = HIGHER_PRECISION_METHOD,
  maybeFetch = globalThis.fetch,
) {
  if (!/^tc-evidence:sha256:[0-9a-f]{64}$/.test(validationRef || "")) {
    throw new Error("Robustness validation reference is invalid");
  }
  const method = typeof methodOrFetch === "function" ? HIGHER_PRECISION_METHOD : methodOrFetch;
  const fetchImpl = typeof methodOrFetch === "function" ? methodOrFetch : maybeFetch;
  if (!SUPPORTED_METHODS.has(method) || typeof fetchImpl !== "function") {
    throw new Error("Native robustness method or fetch boundary is invalid");
  }
  const response = await fetchImpl(HISTORICAL_RESULTS_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({ action: readActionForMethod(method), validation_ref: validationRef }),
  });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Robustness result read failed");
  const result = robustnessResultFromPayload(payload);
  if (result.method !== method) throw new Error("Robustness result method does not match the requested native method");
  return result;
}

async function startNativeRobustness(historicalResult, method, fetchImpl = globalThis.fetch) {
  const source = historicalResultFromPayload(historicalResult);
  if (source.state !== "completed" || source.execution_completed !== true) {
    throw new Error("Native robustness requires a completed Historical Result");
  }
  const response = await fetchImpl(HISTORICAL_RESULTS_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({
      action: startActionForMethod(method),
      historical_result_entity_id: source.entity_id,
      expected_historical_result_revision: source.revision,
    }),
  });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Native robustness execution failed");
  const result = robustnessResultFromPayload(payload);
  if (
    result.method !== method
    || result.source_historical_result_entity_id !== source.entity_id
    || result.source_historical_result_revision !== source.revision
    || result.source_result_archive_sha256 !== source.result_archive_sha256
  ) {
    throw new Error("Robustness result does not bind the selected Historical Result revision and native method");
  }
  return result;
}

export function startHigherPrecision(historicalResult, fetchImpl = globalThis.fetch) {
  return startNativeRobustness(historicalResult, HIGHER_PRECISION_METHOD, fetchImpl);
}

export function startSystemParameterPermutation(historicalResult, fetchImpl = globalThis.fetch) {
  return startNativeRobustness(historicalResult, SYSTEM_PARAMETER_METHOD, fetchImpl);
}

function short(value) {
  const text = String(value || "");
  return text.length > 24 ? `…${text.slice(-22)}` : text;
}

function methodLabel(method) {
  return method === SYSTEM_PARAMETER_METHOD ? "System Parameter Permutation" : "Higher Precision";
}

function settingsRows(result) {
  if (result.method === SYSTEM_PARAMETER_METHOD) {
    return `<div class="stat-row"><span>Optimize periods</span><code>${escapeHtml(result.native_settings.OptimPeriods)}</code></div>
      <div class="stat-row"><span>Optimize exit types</span><code>${escapeHtml(result.native_settings.OptimExitTypes)}</code></div>
      <div class="stat-row"><span>Max tests</span><code>${escapeHtml(result.native_settings.MaxTests)}</code></div>`;
  }
  return `<div class="stat-row"><span>Precision</span><code>${escapeHtml(result.native_settings.Precision)}</code></div>
      <div class="stat-row"><span>Spread</span><code>${escapeHtml(result.native_settings.Spread)}</code></div>`;
}

function resultPanel(result) {
  if (!result) {
    return `<div class="empty-state"><div class="empty-icon">—</div><div><strong>No native robustness run selected</strong><p>Choose one completed baseline Historical Result and run a producer-backed SQX robustness method.</p></div></div>`;
  }
  const label = methodLabel(result.method);
  return `<div data-robustness-result="${escapeHtml(result.validation_ref)}" data-robustness-method="${escapeHtml(result.method)}">
    <div class="context-callout"><span class="callout-icon">↳</span><div><span class="eyebrow">Native SQX output captured</span><strong>${escapeHtml(label)} execution completed</strong><span>Producer output is in immutable custody. No robustness pass/fail is inferred until an authoritative SQX outcome readback seam is connected.</span></div></div>
    <div class="idea-identity">
      <div class="stat-row"><span>Validation evidence</span><code>${escapeHtml(result.validation_ref)}</code></div>
      <div class="stat-row"><span>Source Historical Result</span><code>${escapeHtml(result.source_historical_result_revision)}</code></div>
      <div class="stat-row"><span>Native method</span><code>${escapeHtml(result.method)}</code></div>
      ${settingsRows(result)}
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
    ["Walk-Forward / Matrix", "Native optimization/validation family — producer path not connected in this slice."],
  ];
  return `<div class="requirement-list" data-robustness-methods>
    <div class="requirement-item"><div><strong>Higher Precision</strong><span class="status-badge status-ready"><span class="status-dot"></span>Native execution wired</span></div><p>Uses the current installed Retester profile and its existing Precision/Spread settings.</p></div>
    <div class="requirement-item"><div><strong>System Parameter Permutation</strong><span class="status-badge status-ready"><span class="status-dot"></span>Native execution wired</span></div><p>Uses the installed OptProfileSysParamPermutation profile and preserves OptimPeriods, OptimExitTypes, and MaxTests.</p></div>
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
      <div class="panel-heading"><div><p class="eyebrow">Exact input</p><h2>Native SQX execution</h2></div></div>
      <label class="field-label" for="robustness-source-result">Completed baseline Historical Result</label>
      <select id="robustness-source-result" class="idea-editor" ${completed.length ? "" : "disabled"}>${completed.length ? completed.map((item, index) => `<option value="${index}" ${index === current.selectedIndex ? "selected" : ""}>${escapeHtml(item.result_archive_name)} · ${escapeHtml(short(item.revision))}</option>`).join("") : '<option>No completed Historical Results</option>'}</select>
      ${selected ? `<div class="idea-identity"><div class="stat-row"><span>Historical Result</span><code>${escapeHtml(selected.entity_id)}</code></div><div class="stat-row"><span>Revision</span><code>${escapeHtml(selected.revision)}</code></div><div class="stat-row"><span>Source archive</span><code>${escapeHtml(selected.result_archive_sha256)}</code></div></div>` : ""}
      <p class="field-help">TraderCockpit supplies only exact Historical Result identity. The installed Retester project owns each native robustness profile and its settings.</p>
      <div class="button-row">
        <button class="button button-primary" type="button" data-robustness-action="higher-precision" ${canRun ? "" : "disabled"}>${current.runtimeReady ? "Run native Higher Precision" : "Native Retester unavailable"}</button>
        <button class="button" type="button" data-robustness-action="system-parameter-permutation" ${canRun ? "" : "disabled"}>${current.runtimeReady ? "Run native System Parameter Permutation" : "Native Retester unavailable"}</button>
      </div>
      <p class="idea-save-status" data-robustness-status>${escapeHtml(current.detail || "")}</p>
    </section>
    <section class="panel" data-accent="purple"><div class="panel-heading"><div><p class="eyebrow">Immutable readback</p><h2>Robustness result custody</h2></div></div>${resultPanel(current.validation)}</section>
  </div>`;
}

function persistValidationRef(validationRef, method) {
  if (!globalThis.history?.replaceState || !globalThis.location) return;
  const url = new URL(globalThis.location.href);
  url.searchParams.set("stage", "backtest");
  url.searchParams.set("tab", "robustness");
  url.searchParams.set("validationRef", validationRef);
  url.searchParams.set("robustnessMethod", method);
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
    const requestedMethod = methodFromLocation();
    const [results, runtime, validation] = await Promise.all([
      fetchHistoricalResults(),
      fetchRuntimeStatus(),
      requestedRef ? fetchRobustnessResult(requestedRef, requestedMethod) : Promise.resolve(null),
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

async function start(button, method) {
  if (state.phase === "loading" || !state.runtimeReady) return;
  const completed = state.results.filter((item) => item.state === "completed" && item.execution_completed === true);
  const selected = completed[state.selectedIndex];
  if (!selected) return;
  const label = methodLabel(method);
  button.disabled = true;
  button.textContent = `Running ${label} in SQX…`;
  try {
    const validation = method === SYSTEM_PARAMETER_METHOD
      ? await startSystemParameterPermutation(selected)
      : await startHigherPrecision(selected);
    if (!robustnessRoute()) return;
    persistValidationRef(validation.validation_ref, validation.method);
    state = { ...state, phase: "loaded", validation, detail: `Native ${label} result captured. Producer verdict remains unread.` };
  } catch (error) {
    state = { ...state, phase: "failed", detail: error instanceof Error ? error.message : `Native ${label} execution failed` };
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
    const button = event.target?.closest?.("[data-robustness-action]");
    if (!button || !robustnessRoute()) return;
    const action = button.getAttribute("data-robustness-action");
    const method = action === "system-parameter-permutation" ? SYSTEM_PARAMETER_METHOD : action === "higher-precision" ? HIGHER_PRECISION_METHOD : "";
    if (method) void start(button, method);
  });
  document.addEventListener("locationchange", () => { if (robustnessRoute()) void load(); });
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => { if (robustnessRoute()) void load(); }, { once: true });
  } else if (robustnessRoute()) {
    queueMicrotask(load);
  }
}
