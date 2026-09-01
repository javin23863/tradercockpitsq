import {
  fetchHistoricalResults,
  fetchRuntimeStatus,
  historicalResultFromPayload,
  retesterRuntimeReady,
} from "./research-backtest.mjs";

const HISTORICAL_RESULTS_API_PATH = "/api/research/historical-results";
const ROBUSTNESS_SCHEMA = "tc.research-native-robustness.v1";
const ROBUSTNESS_ATTEMPT_SCHEMA = "tc.research-native-robustness-attempt.v1";
const ROBUSTNESS_CAPABILITIES_SCHEMA = "tc.research-native-robustness-capabilities.v1";
const ROBUSTNESS_CATALOG_SCHEMA = "tc.research-native-robustness-catalog.v1";
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
    "proof_entity_id",
    "proof_revision",
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
    !/^tc-research:proof:v1:[0-9a-f-]{36}$/.test(payload.proof_entity_id)
    || !/^tc-research-revision:proof:sha256:[0-9a-f]{64}$/.test(payload.proof_revision)
  ) {
    throw new Error("Native robustness proof custody is inconsistent");
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
    || payload.receipts[0]?.result_archive_sha256 !== payload.source_result_archive_sha256
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

export function robustnessAttemptFromPayload(payload) {
  const requiredStrings = [
    "attempt_ref", "proof_entity_id", "proof_revision",
    "source_historical_result_entity_id", "source_historical_result_revision",
    "source_result_archive_ref", "source_result_archive_sha256",
    "source_project_ref", "source_project_sha256",
    "compiled_project_ref", "compiled_project_sha256",
    "source_task_sha256", "compiled_task_sha256",
    "engine_ref", "engine_sha256", "native_project_name", "native_project_relative_path",
    "failure_reason_code",
  ];
  if (
    !payload
    || payload.schema !== ROBUSTNESS_ATTEMPT_SCHEMA
    || !["failed", "interrupted"].includes(payload.state)
    || payload.sqx_build !== "144.2953"
    || payload.operation !== "native_retester_cross_check"
    || payload.method !== HIGHER_PRECISION_METHOD
    || typeof payload.configuration_changed !== "boolean"
    || typeof payload.partial_side_effect !== "boolean"
    || requiredStrings.some((key) => typeof payload[key] !== "string" || !payload[key])
    || !Array.isArray(payload.receipts)
    || payload.receipts.length > 1
  ) {
    throw new Error("Native robustness failed-attempt identity is invalid");
  }
  const evidencePairs = [
    ["source_result_archive_ref", "source_result_archive_sha256"],
    ["source_project_ref", "source_project_sha256"],
    ["compiled_project_ref", "compiled_project_sha256"],
    ["engine_ref", "engine_sha256"],
  ];
  if (
    evidenceDigest(payload.attempt_ref) === ""
    || !/^tc-research:proof:v1:[0-9a-f-]{36}$/.test(payload.proof_entity_id)
    || !/^tc-research-revision:proof:sha256:[0-9a-f]{64}$/.test(payload.proof_revision)
    || !/^tc-research:historical-result:v1:[0-9a-f-]{36}$/.test(payload.source_historical_result_entity_id)
    || !/^tc-research-revision:historical-result:sha256:[0-9a-f]{64}$/.test(payload.source_historical_result_revision)
    || !/^TraderCockpit-Retester-[0-9a-f]{32}$/.test(payload.native_project_name)
    || payload.native_project_relative_path !== `user/projects/${payload.native_project_name}/project.cfx`
    || !digest(payload.source_task_sha256)
    || !digest(payload.compiled_task_sha256)
    || evidencePairs.some(([refKey, digestKey]) => !digest(payload[digestKey]) || evidenceDigest(payload[refKey]) !== payload[digestKey])
    || (payload.launcher_sha256 !== null && !digest(payload.launcher_sha256))
    || !payload.native_settings
    || typeof payload.native_settings !== "object"
    || typeof payload.native_settings.Precision !== "string"
    || !payload.native_settings.Precision
    || typeof payload.native_settings.Spread !== "string"
    || !payload.native_settings.Spread
  ) {
    throw new Error("Native robustness failed-attempt custody is inconsistent");
  }
  if (payload.state === "interrupted") {
    if (
      payload.failure_reason_code !== "robustness_attempt_interrupted"
      || payload.partial_side_effect !== true
      || payload.launcher_sha256 !== null
      || payload.receipts.length !== 0
    ) {
      throw new Error("Native robustness interrupted-attempt custody is inconsistent");
    }
    return payload;
  }
  const launchedStates = new Set(["completed", "timeout", "rejected", "invalid_receipt"]);
  const allowedStates = new Set([...launchedStates, "preflight_failed", "launch_failed"]);
  for (const receipt of payload.receipts) {
    if (
      !receipt || typeof receipt !== "object"
      || !allowedStates.has(receipt.state)
      || receipt.action !== "startOnlyTask"
      || receipt.task !== 1
      || receipt.project !== payload.native_project_name
      || (receipt.launcher_sha256 !== null && receipt.launcher_sha256 !== payload.launcher_sha256)
      || (receipt.project_sha256 !== null && receipt.project_sha256 !== payload.compiled_project_sha256)
      || (receipt.engine_sha256 !== null && receipt.engine_sha256 !== payload.engine_sha256)
      || (launchedStates.has(receipt.state) && receipt.result_archive_sha256 !== payload.source_result_archive_sha256)
    ) {
      throw new Error("Native robustness failed-attempt receipt is inconsistent");
    }
  }
  const launched = payload.receipts.some((item) => launchedStates.has(item.state));
  const invalidReceipt = payload.receipts.some((item) => item.state === "invalid_receipt");
  if (launched !== payload.partial_side_effect || (payload.partial_side_effect && !payload.launcher_sha256 && !invalidReceipt)) {
    throw new Error("Native robustness failed-attempt side-effect state is inconsistent");
  }
  return payload;
}


export function robustnessReadbackFromPayload(payload) {
  return payload?.schema === ROBUSTNESS_ATTEMPT_SCHEMA
    ? robustnessAttemptFromPayload(payload)
    : robustnessResultFromPayload(payload);
}

export async function fetchRobustnessResult(validationRef, fetchImpl = globalThis.fetch) {
  if (!/^tc-evidence:sha256:[0-9a-f]{64}$/.test(validationRef || "")) {
    throw new Error("Robustness validation reference is invalid");
  }
  const response = await fetchImpl(HISTORICAL_RESULTS_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({ action: "read-robustness", validation_ref: validationRef }),
  });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Robustness result read failed");
  return robustnessReadbackFromPayload(payload);
}

export function robustnessCapabilitiesFromPayload(payload) {
  if (
    !payload
    || payload.schema !== ROBUSTNESS_CAPABILITIES_SCHEMA
    || payload.sqx_build !== "144.2953"
    || !Array.isArray(payload.methods)
    || payload.methods.length !== 1
  ) {
    throw new Error("Native robustness capability schema is invalid");
  }
  const method = payload.methods[0];
  if (!method || method.method !== HIGHER_PRECISION_METHOD || !["ready", "unavailable"].includes(method.state)) {
    throw new Error("Native robustness capability identity is invalid");
  }
  if (method.state === "ready") {
    if (
      typeof method.detail !== "string" || !method.detail
      || method.reason_code !== null
      || typeof method.configuration_changed !== "boolean"
      || !method.native_settings || typeof method.native_settings !== "object"
      || typeof method.native_settings.Precision !== "string" || !method.native_settings.Precision
      || typeof method.native_settings.Spread !== "string" || !method.native_settings.Spread
      || !digest(method.source_project_sha256)
      || !digest(method.compiled_project_sha256)
      || !digest(method.engine_sha256)
    ) {
      throw new Error("Ready native robustness capability is inconsistent");
    }
  } else if (
    typeof method.reason_code !== "string" || !method.reason_code
    || typeof method.detail !== "string" || !method.detail
    || method.native_settings !== null
    || method.configuration_changed !== null
  ) {
    throw new Error("Unavailable native robustness capability is inconsistent");
  }
  return payload;
}

export function robustnessCatalogFromPayload(payload) {
  if (
    !payload
    || payload.schema !== ROBUSTNESS_CATALOG_SCHEMA
    || !Array.isArray(payload.results)
    || !Array.isArray(payload.failed_attempts)
  ) {
    throw new Error("Native robustness catalog schema is invalid");
  }
  return {
    results: payload.results.map(robustnessResultFromPayload),
    failedAttempts: payload.failed_attempts.map(robustnessAttemptFromPayload),
  };
}

export function robustnessResultsForHistorical(catalog, historicalResult) {
  if (!Array.isArray(catalog)) throw new Error("Native robustness catalog is invalid");
  const source = historicalResultFromPayload(historicalResult);
  return catalog.filter((item) => (
    item.source_historical_result_entity_id === source.entity_id
    && item.source_historical_result_revision === source.revision
  ));
}

export function robustnessAttemptsForHistorical(attempts, historicalResult) {
  if (!Array.isArray(attempts)) throw new Error("Native robustness attempt catalog is invalid");
  const source = historicalResultFromPayload(historicalResult);
  return attempts.filter((item) => (
    item.source_historical_result_entity_id === source.entity_id
    && item.source_historical_result_revision === source.revision
  ));
}

export function robustnessAttemptRefFromStartError(error) {
  const value = error?.payload?.attempt_ref;
  return typeof value === "string" && /^tc-evidence:sha256:[0-9a-f]{64}$/.test(value) ? value : "";
}

export async function fetchRobustnessAttemptForStartError(error, historicalResult, fetchImpl = globalThis.fetch) {
  const attemptRef = robustnessAttemptRefFromStartError(error);
  if (!attemptRef) return null;
  const attempt = await fetchRobustnessResult(attemptRef, fetchImpl);
  if (attempt.schema !== ROBUSTNESS_ATTEMPT_SCHEMA) {
    throw new Error("Native robustness failure did not return an attempt record");
  }
  const matches = robustnessAttemptsForHistorical([attempt], historicalResult);
  if (matches.length !== 1) {
    throw new Error("Native robustness failed attempt does not bind the originating Historical Result");
  }
  return attempt;
}

export function robustnessStartFailureState(current, failedAttempt, failedAttempts, detail) {
  return {
    ...current,
    phase: "failed",
    runtimeReady: false,
    capabilities: null,
    validation: failedAttempt,
    suppressCompletedPicker: !failedAttempt,
    inFlightSource: null,
    failedAttempts,
    detail,
  };
}

export function robustnessExecutionAvailable(phase, runtimeReady, higherCapability, selected) {
  return phase === "loaded" && runtimeReady === true && higherCapability?.state === "ready" && Boolean(selected);
}

export function robustnessResultForHistorical(catalog, historicalResult, validationRef = "") {
  const matches = robustnessResultsForHistorical(catalog, historicalResult);
  if (validationRef) return matches.find((item) => item.validation_ref === validationRef) || null;
  return matches.length === 1 ? matches[0] : null;
}

export async function fetchRobustnessCapabilities(fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(HISTORICAL_RESULTS_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({ action: "read-robustness-capabilities" }),
  });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Robustness capability read failed");
  return robustnessCapabilitiesFromPayload(payload);
}

export async function fetchRobustnessCatalog(fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(HISTORICAL_RESULTS_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({ action: "list-robustness" }),
  });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Robustness catalog read failed");
  return robustnessCatalogFromPayload(payload);
}

export async function startHigherPrecision(historicalResult, fetchImpl = globalThis.fetch) {
  const source = historicalResultFromPayload(historicalResult);
  if (source.state !== "completed" || source.execution_completed !== true) {
    throw new Error("Higher Precision requires a completed Historical Result");
  }
  const response = await fetchImpl(HISTORICAL_RESULTS_API_PATH, {
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
  if (result?.schema === ROBUSTNESS_ATTEMPT_SCHEMA) {
    const receiptState = result.receipts.map((item) => item.state).filter(Boolean).join(", ") || "no native receipt";
    return `<div data-robustness-attempt="${escapeHtml(result.attempt_ref)}"><div class="context-callout"><span class="callout-icon">!</span><div><span class="eyebrow">Native SQX attempt custody</span><strong>Higher Precision attempt did not complete cleanly</strong><span>This is durable execution-state evidence, not a producer robustness verdict.</span></div></div><div class="idea-identity"><div class="stat-row"><span>Attempt evidence</span><code>${escapeHtml(result.attempt_ref)}</code></div><div class="stat-row"><span>Source Historical Result</span><code>${escapeHtml(result.source_historical_result_revision)}</code></div><div class="stat-row"><span>Failure reason</span><code>${escapeHtml(result.failure_reason_code)}</code></div><div class="stat-row"><span>Possible native side effect</span><code>${result.partial_side_effect ? "yes" : "no"}</code></div><div class="stat-row"><span>Receipt state</span><code>${escapeHtml(receiptState)}</code></div></div></div>`;
  }
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

function methodRows(capabilities) {
  const nativeLater = [
    ["Additional Markets", "Native cross-market retest — producer path not connected in this slice."],
    ["Monte Carlo · trade manipulation", "Native trade-manipulation family — not executed by TraderCockpit locally."],
    ["Monte Carlo · full retest", "Native full-retest family — producer path not connected in this slice."],
    ["System Parameter Permutation", "Native optimization profile — producer path not connected in this slice."],
    ["Walk-Forward / Matrix", "Native optimization/validation family — producer path not connected in this slice."],
  ];
  const higher = capabilities?.methods?.find((item) => item.method === HIGHER_PRECISION_METHOD) || null;
  const ready = higher?.state === "ready";
  const label = ready ? "Producer capability available" : higher ? "Producer unavailable" : "Checking producer";
  const detail = ready
    ? `Installed SQX owns this profile. Precision ${higher.native_settings.Precision}; Spread ${higher.native_settings.Spread}.`
    : higher?.detail || "Waiting for the backend to inspect the installed SQX Retester project.";
  return `<div class="requirement-list" data-robustness-methods>
    <div class="requirement-item"><div><strong>Higher Precision</strong><span class="status-badge status-${ready ? "ready" : "unavailable"}"><span class="status-dot"></span>${escapeHtml(label)}</span></div><p>${escapeHtml(detail)}</p></div>
    ${nativeLater.map(([name, itemDetail]) => `<div class="requirement-item"><div><strong>${escapeHtml(name)}</strong><span class="status-badge status-unavailable"><span class="status-dot"></span>Not connected</span></div><p>${escapeHtml(itemDetail)}</p></div>`).join("")}
  </div>`;
}

let generation = 0;
let state = { phase: "idle", results: [], selectedIndex: 0, runtimeReady: false, capabilities: null, catalog: [], failedAttempts: [], validation: null, suppressCompletedPicker: false, inFlightSource: null, detail: "" };

function panel() {
  if (!robustnessRoute()) return null;
  return document.querySelector('.content-inner .panel.wide-panel[data-accent="orange"]');
}

function render(host, current) {
  if (!host?.isConnected) return;
  const completed = current.results.filter((item) => item.state === "completed" && item.execution_completed === true);
  const selected = completed[current.selectedIndex] || null;
  const matchingValidations = selected ? robustnessResultsForHistorical(current.catalog, selected) : [];
  const matchingAttempts = selected ? robustnessAttemptsForHistorical(current.failedAttempts, selected) : [];
  const higherCapability = current.capabilities?.methods?.find((item) => item.method === HIGHER_PRECISION_METHOD) || null;
  const locked = current.phase !== "loaded";
  const canRun = robustnessExecutionAvailable(current.phase, current.runtimeReady, higherCapability, selected);
  host.querySelector(".empty-state")?.remove();
  let workspace = host.querySelector("[data-robustness-workspace]");
  if (!workspace) {
    workspace = document.createElement("div");
    workspace.dataset.robustnessWorkspace = "";
    host.append(workspace);
  }
  const validationPicker = current.validation?.schema === ROBUSTNESS_ATTEMPT_SCHEMA
    ? `<p class="field-help" data-robustness-attempt-selection>Exact native attempt selected: <code>${escapeHtml(short(current.validation.attempt_ref))}</code>. Completed-run selection is hidden so custody identities cannot be cross-displayed.</p>`
    : current.suppressCompletedPicker
      ? '<p class="field-help" data-robustness-unbound-attempt>No exact completed or failed attempt is selected for the most recent execution.</p>'
    : matchingValidations.length
      ? `<label class="field-label" for="robustness-validation-result">Captured robustness run</label><select id="robustness-validation-result" class="idea-editor" ${locked ? "disabled" : ""}>${matchingValidations.length > 1 && !current.validation ? '<option value="" selected>Choose exact robustness run</option>' : ""}${matchingValidations.map((item) => `<option value="${escapeHtml(item.validation_ref)}" ${current.validation?.validation_ref === item.validation_ref ? "selected" : ""}>${escapeHtml(short(item.validation_ref))} · ${escapeHtml(short(item.proof_revision))}</option>`).join("")}</select>`
      : '<p class="field-help">No completed robustness run is registered for the selected Historical Result.</p>';
  workspace.innerHTML = `<div class="dashboard-grid">
    <section class="panel" data-accent="orange">
      <div class="panel-heading"><div><p class="eyebrow">Producer-backed plan</p><h2>Native robustness methods</h2></div></div>
      ${methodRows(current.capabilities)}
    </section>
    <section class="panel" data-accent="cyan">
      <div class="panel-heading"><div><p class="eyebrow">Exact input</p><h2>Higher Precision</h2></div></div>
      <label class="field-label" for="robustness-source-result">Completed baseline Historical Result</label>
      <select id="robustness-source-result" class="idea-editor" ${completed.length && !locked ? "" : "disabled"}>${completed.length ? completed.map((item, index) => `<option value="${index}" ${index === current.selectedIndex ? "selected" : ""}>${escapeHtml(item.result_archive_name)} · ${escapeHtml(short(item.revision))}</option>`).join("") : '<option>No completed Historical Results</option>'}</select>
      ${selected ? `<div class="idea-identity"><div class="stat-row"><span>Historical Result</span><code>${escapeHtml(selected.entity_id)}</code></div><div class="stat-row"><span>Revision</span><code>${escapeHtml(selected.revision)}</code></div><div class="stat-row"><span>Source archive</span><code>${escapeHtml(selected.result_archive_sha256)}</code></div></div>` : ""}
      <p class="field-help">TraderCockpit supplies only exact Historical Result identity. The installed Retester project owns the Higher Precision profile and native settings.</p>
      <button class="button button-primary" type="button" data-robustness-action="start" ${canRun ? "" : "disabled"}>${canRun ? "Run native Higher Precision" : "Native Higher Precision unavailable"}</button>
      <p class="idea-save-status" data-robustness-status>${escapeHtml(current.detail || "")}</p>
    </section>
    <section class="panel" data-accent="purple"><div class="panel-heading"><div><p class="eyebrow">Immutable readback</p><h2>Robustness result custody</h2></div></div>${validationPicker}${resultPanel(current.validation)}${matchingAttempts.length ? `<div class="requirement-list" data-robustness-failed-attempts>${matchingAttempts.map((item) => `<div class="requirement-item"><div><strong>${item.state === "interrupted" ? "Interrupted native attempt" : "Failed native attempt"}</strong><span class="status-badge status-unavailable"><span class="status-dot"></span>${escapeHtml(item.failure_reason_code)}</span></div><p><code>${escapeHtml(short(item.attempt_ref))}</code> · partial side effect ${item.partial_side_effect ? "possible" : "not observed"}</p></div>`).join("")}</div>` : ""}</section>
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

function clearValidationRef() {
  if (!globalThis.history?.replaceState || !globalThis.location) return;
  const url = new URL(globalThis.location.href);
  url.searchParams.delete("validationRef");
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
    const [results, runtime, capabilities, catalogRead] = await Promise.all([
      fetchHistoricalResults(),
      fetchRuntimeStatus(),
      fetchRobustnessCapabilities(),
      fetchRobustnessCatalog(),
    ]);
    const catalog = catalogRead.results;
    const failedAttempts = catalogRead.failedAttempts;
    const completed = results.filter((item) => item.state === "completed" && item.execution_completed === true);
    let selectedIndex = 0;
    let validation = completed[0] ? robustnessResultForHistorical(catalog, completed[0]) : null;
    let suppressCompletedPicker = false;
    let detail = "";
    if (requestedRef) {
      try {
        const requestedValidation = await fetchRobustnessResult(requestedRef);
        const sourceIndex = completed.findIndex((item) => (
          item.entity_id === requestedValidation.source_historical_result_entity_id
          && item.revision === requestedValidation.source_historical_result_revision
        ));
        if (sourceIndex >= 0) {
          selectedIndex = sourceIndex;
          validation = requestedValidation;
        } else {
          validation = null;
          suppressCompletedPicker = true;
          clearValidationRef();
          detail = "Saved robustness result source Historical Result is no longer current; receipt was not displayed.";
        }
      } catch (error) {
        validation = null;
        suppressCompletedPicker = true;
        clearValidationRef();
        detail = `Saved robustness result unavailable: ${error instanceof Error ? error.message : "readback failed"}`;
      }
    }
    if (currentGeneration !== generation || !robustnessRoute()) return;
    state = {
      phase: "loaded",
      results,
      selectedIndex,
      runtimeReady: retesterRuntimeReady(runtime),
      capabilities,
      catalog,
      failedAttempts,
      validation,
      suppressCompletedPicker,
      inFlightSource: null,
      detail,
    };
  } catch (error) {
    if (currentGeneration !== generation || !robustnessRoute()) return;
    state = { ...state, phase: "failed", runtimeReady: false, capabilities: null, suppressCompletedPicker: true, inFlightSource: null, detail: error instanceof Error ? error.message : "Native robustness workspace unavailable" };
  }
  render(panel(), state);
}

async function start(button) {
  const higherCapability = state.capabilities?.methods?.find((item) => item.method === HIGHER_PRECISION_METHOD) || null;
  if (["loading", "running"].includes(state.phase) || !state.runtimeReady || higherCapability?.state !== "ready") return;
  const completed = state.results.filter((item) => item.state === "completed" && item.execution_completed === true);
  const selected = completed[state.selectedIndex];
  if (!selected) return;
  const inFlightSource = { entity_id: selected.entity_id, revision: selected.revision };
  clearValidationRef();
  state = { ...state, phase: "running", inFlightSource, validation: null, suppressCompletedPicker: true, detail: "Running Higher Precision in SQX…" };
  render(panel(), state);
  try {
    const validation = await startHigherPrecision(selected);
    if (!robustnessRoute()) return;
    const completedNow = state.results.filter((item) => item.state === "completed" && item.execution_completed === true);
    const sourceIndex = completedNow.findIndex((item) => item.entity_id === inFlightSource.entity_id && item.revision === inFlightSource.revision);
    if (sourceIndex < 0) {
      clearValidationRef();
      state = { ...state, phase: "loaded", validation: null, suppressCompletedPicker: true, inFlightSource: null, detail: "Native result captured, but its source Historical Result is no longer current; receipt was not cross-displayed." };
    } else {
      persistValidationRef(validation.validation_ref);
      state = { ...state, phase: "loaded", selectedIndex: sourceIndex, validation, suppressCompletedPicker: false, inFlightSource: null, catalog: [validation, ...state.catalog.filter((item) => item.validation_ref !== validation.validation_ref)], detail: "Native Higher Precision result captured. Producer verdict remains unread." };
    }
  } catch (error) {
    let failedAttempt = null;
    try { failedAttempt = await fetchRobustnessAttemptForStartError(error, selected); } catch {}
    let failedAttempts = state.failedAttempts;
    try { failedAttempts = (await fetchRobustnessCatalog()).failedAttempts; } catch {}
    if (failedAttempt && !failedAttempts.some((item) => item.attempt_ref === failedAttempt.attempt_ref)) {
      failedAttempts = [failedAttempt, ...failedAttempts];
    }
    if (failedAttempt) persistValidationRef(failedAttempt.attempt_ref);
    else clearValidationRef();
    state = robustnessStartFailureState(
      state,
      failedAttempt,
      failedAttempts,
      error instanceof Error ? error.message : "Native Higher Precision execution failed",
    );
  }
  render(panel(), state);
}

if (typeof document !== "undefined") {
  document.addEventListener("change", (event) => {
    if (!robustnessRoute() || state.phase === "running") return;
    const completed = state.results.filter((item) => item.state === "completed" && item.execution_completed === true);
    if (event.target?.id === "robustness-source-result") {
      const selectedIndex = Number(event.target.value);
      if (!Number.isInteger(selectedIndex) || selectedIndex < 0 || selectedIndex >= completed.length) return;
      const selected = completed[selectedIndex];
      const validation = robustnessResultForHistorical(state.catalog, selected);
      if (validation) persistValidationRef(validation.validation_ref);
      else clearValidationRef();
      state = { ...state, selectedIndex, validation, suppressCompletedPicker: false, detail: "" };
      render(panel(), state);
      return;
    }
    if (event.target?.id === "robustness-validation-result") {
      const selected = completed[state.selectedIndex];
      if (!selected) return;
      const validationRef = typeof event.target.value === "string" ? event.target.value : "";
      const validation = validationRef
        ? robustnessResultForHistorical(state.catalog, selected, validationRef)
        : null;
      if (validationRef && !validation) return;
      if (validation) persistValidationRef(validation.validation_ref);
      else clearValidationRef();
      state = { ...state, validation, suppressCompletedPicker: false, detail: "" };
      render(panel(), state);
    }
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
