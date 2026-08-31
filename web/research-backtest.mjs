const CANDIDATES_API_PATH = "/api/research/candidates";
const HISTORICAL_RESULTS_API_PATH = "/api/research/historical-results";
const STATUS_API_PATH = "/api/status";
const CANDIDATE_SCHEMA = "tc.research-candidate.v1";
const CANDIDATE_CATALOG_SCHEMA = "tc.research-candidate-catalog.v1";
const HISTORICAL_RESULT_SCHEMA = "tc.research-historical-result.v1";
const HISTORICAL_RESULT_CATALOG_SCHEMA = "tc.research-historical-result-catalog.v1";

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function overviewRoute() {
  if (globalThis.location?.pathname !== "/research") return false;
  const params = new URLSearchParams(globalThis.location.search || "");
  return params.get("stage") === "backtest" && params.get("tab") === "overview";
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

function digest(value) {
  return typeof value === "string" && /^[0-9a-f]{64}$/.test(value) ? value : "";
}

function evidenceDigest(value) {
  if (typeof value !== "string" || !value.startsWith("tc-evidence:sha256:")) return "";
  return digest(value.slice("tc-evidence:sha256:".length));
}

export function candidateFromPayload(payload) {
  if (
    !payload
    || payload.schema !== CANDIDATE_SCHEMA
    || typeof payload.entity_id !== "string"
    || typeof payload.revision !== "string"
    || typeof payload.archive_name !== "string"
    || !payload.archive_name
    || !digest(payload.archive_sha256)
    || evidenceDigest(payload.archive_ref) !== payload.archive_sha256
    || payload.sqx_build !== "144.2953"
  ) {
    throw new Error("Candidate custody is invalid");
  }
  return payload;
}

export function candidateCatalogFromPayload(payload) {
  if (!payload || payload.schema !== CANDIDATE_CATALOG_SCHEMA || !Array.isArray(payload.candidates)) {
    throw new Error("Candidate catalog schema mismatch");
  }
  return payload.candidates.map(candidateFromPayload);
}

export function historicalResultFromPayload(payload) {
  const requiredStrings = [
    "entity_id", "revision", "candidate_entity_id", "candidate_revision",
    "candidate_archive_name", "candidate_archive_ref", "candidate_archive_sha256",
    "sqx_build", "operation", "native_project_name", "native_project_relative_path",
    "source_project_ref", "source_project_sha256", "engine_ref", "engine_sha256",
  ];
  if (!payload || payload.schema !== HISTORICAL_RESULT_SCHEMA || requiredStrings.some((key) => typeof payload[key] !== "string" || !payload[key])) {
    throw new Error("Historical result identity is invalid");
  }
  if (!['prepared', 'completed', 'failed'].includes(payload.state) || payload.sqx_build !== "144.2953" || payload.operation !== "native_retester_task_1" || payload.retester_task !== 1) {
    throw new Error("Historical result control state is invalid");
  }
  if (
    !digest(payload.candidate_archive_sha256)
    || evidenceDigest(payload.candidate_archive_ref) !== payload.candidate_archive_sha256
    || !digest(payload.source_project_sha256)
    || evidenceDigest(payload.source_project_ref) !== payload.source_project_sha256
    || !digest(payload.engine_sha256)
    || evidenceDigest(payload.engine_ref) !== payload.engine_sha256
  ) {
    throw new Error("Historical result evidence binding is invalid");
  }
  if (!/^TraderCockpit-Retester-[0-9a-f]{32}$/.test(payload.native_project_name) || payload.native_project_relative_path !== `user/projects/${payload.native_project_name}/project.cfx`) {
    throw new Error("Historical result native project binding is invalid");
  }
  if (!Array.isArray(payload.receipts) || typeof payload.partial_side_effect !== "boolean" || payload.validation_state !== "not_run") {
    throw new Error("Historical result receipt/validation state is invalid");
  }
  if (payload.state === "completed") {
    if (
      payload.execution_completed !== true
      || typeof payload.launcher_sha256 !== "string"
      || !digest(payload.launcher_sha256)
      || typeof payload.result_archive_name !== "string"
      || !payload.result_archive_name
      || typeof payload.result_archive_relative_path !== "string"
      || !payload.result_archive_relative_path
      || !digest(payload.result_archive_sha256)
      || evidenceDigest(payload.result_archive_ref) !== payload.result_archive_sha256
      || !digest(payload.result_strategy_sha256)
      || evidenceDigest(payload.result_strategy_ref) !== payload.result_strategy_sha256
      || !digest(payload.result_settings_sha256)
      || evidenceDigest(payload.result_settings_ref) !== payload.result_settings_sha256
      || payload.result_archive_sha256 === payload.candidate_archive_sha256
      || payload.receipts.length !== 1
      || payload.receipts[0]?.state !== "completed"
    ) {
      throw new Error("Completed historical result is inconsistent");
    }
  } else if (payload.execution_completed !== false) {
    throw new Error("Non-completed historical result cannot claim execution completion");
  }
  if (payload.state === "failed" && (typeof payload.failure_reason_code !== "string" || !payload.failure_reason_code)) {
    throw new Error("Failed historical result refusal is incomplete");
  }
  return payload;
}

export function historicalResultCatalogFromPayload(payload) {
  if (!payload || payload.schema !== HISTORICAL_RESULT_CATALOG_SCHEMA || !Array.isArray(payload.results)) {
    throw new Error("Historical result catalog schema mismatch");
  }
  return payload.results.map(historicalResultFromPayload);
}

export function retesterRuntimeReady(payload) {
  return Boolean(
    payload
    && payload.schema === "tc.runtime-status.v1"
    && payload.research_backend?.verified === true
    && payload.research_backend?.execution?.gateway_available === true
    && payload.research_backend?.execution?.launcher_verified === true,
  );
}

export async function fetchCandidates(fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(CANDIDATES_API_PATH, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Candidate catalog read failed");
  return candidateCatalogFromPayload(payload);
}

export async function fetchHistoricalResults(fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(HISTORICAL_RESULTS_API_PATH, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Historical result catalog read failed");
  return historicalResultCatalogFromPayload(payload);
}

export async function fetchRuntimeStatus(fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(STATUS_API_PATH, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok || !payload || payload.schema !== "tc.runtime-status.v1") {
    throw apiError(response, payload, "Runtime status read failed");
  }
  return payload;
}

export async function startRetester(candidate, fetchImpl = globalThis.fetch) {
  candidateFromPayload(candidate);
  const response = await fetchImpl(HISTORICAL_RESULTS_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({
      action: "start-retester",
      candidate_entity_id: candidate.entity_id,
      expected_candidate_revision: candidate.revision,
    }),
  });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Native Retester execution failed");
  const result = historicalResultFromPayload(payload);
  if (result.candidate_entity_id !== candidate.entity_id || result.candidate_revision !== candidate.revision || result.candidate_archive_sha256 !== candidate.archive_sha256) {
    throw new Error("Historical result does not bind the selected Candidate revision");
  }
  return result;
}

function short(value) {
  const text = String(value || "");
  return text.length > 20 ? `…${text.slice(-18)}` : text;
}

function resultDetail(result) {
  if (!result) {
    return `<div class="empty-state"><div class="empty-icon">—</div><div><strong>No native historical result</strong><p>Select an exact imported Candidate and run the baseline native Retester task.</p></div></div>`;
  }
  const completed = result.state === "completed";
  const label = completed ? "Execution completed" : result.state === "failed" ? "Execution failed" : "Prepared";
  const tone = completed ? "ready" : "unavailable";
  return `<div data-historical-result-entity-id="${escapeHtml(result.entity_id)}">
    <div class="context-callout"><span class="callout-icon">↳</span><div><span class="eyebrow">Native Retester</span><strong>${escapeHtml(label)}</strong><span>${completed ? "One changed native SQX result archive was captured into immutable custody." : escapeHtml(result.failure_reason_code || "Native execution has not completed.")}</span></div></div>
    <div class="idea-identity">
      <div class="stat-row"><span>Historical result</span><code>${escapeHtml(result.entity_id)}</code></div>
      <div class="stat-row"><span>Result revision</span><code>${escapeHtml(result.revision)}</code></div>
      <div class="stat-row"><span>Candidate revision</span><code>${escapeHtml(result.candidate_revision)}</code></div>
      <div class="stat-row"><span>Native project</span><code>${escapeHtml(result.native_project_name)}</code></div>
      <div class="stat-row"><span>Retester task</span><code>${escapeHtml(result.retester_task)}</code></div>
      <div class="stat-row"><span>Engine SHA-256</span><code>${escapeHtml(result.engine_sha256)}</code></div>
      ${result.launcher_sha256 ? `<div class="stat-row"><span>Launcher SHA-256</span><code>${escapeHtml(result.launcher_sha256)}</code></div>` : ""}
      <div class="stat-row"><span>Candidate archive</span><code>${escapeHtml(result.candidate_archive_sha256)}</code></div>
      ${completed ? `<div class="stat-row"><span>Result archive</span><code>${escapeHtml(result.result_archive_sha256)}</code></div>` : ""}
    </div>
    <div class="requirement-item"><div><strong>Execution state</strong><span class="status-badge status-${tone}"><span class="status-dot"></span>${escapeHtml(label)}</span></div><p>Execution completion is native producer evidence only. Validation has not run and no promotion/champion claim is inferred.</p></div>
  </div>`;
}

function render(panel, state) {
  if (!panel?.isConnected) return;
  const { phase, candidates, results, runtimeReady, selectedIndex, detail } = state;
  const candidate = candidates[selectedIndex] || null;
  const bound = candidate ? results.find((item) => item.candidate_revision === candidate.revision) || null : null;
  const canStart = phase !== "loading" && runtimeReady && candidate && !bound;
  const host = panel.querySelector(".empty-state")?.parentElement || panel;
  panel.querySelector(".empty-state")?.remove();
  let workspace = panel.querySelector("[data-retester-overview]");
  if (!workspace) {
    workspace = document.createElement("div");
    workspace.dataset.retesterOverview = "";
    host.append(workspace);
  }
  workspace.innerHTML = `
    <div class="dashboard-grid">
      <div class="panel" data-accent="cyan">
        <div class="panel-heading"><div><p class="eyebrow">Exact input</p><h2>Candidate</h2></div></div>
        <label class="field-label" for="retester-candidate">Imported native Candidate</label>
        <select id="retester-candidate" class="idea-editor" ${candidates.length ? "" : "disabled"}>${candidates.length ? candidates.map((item, index) => `<option value="${index}" ${index === selectedIndex ? "selected" : ""}>${escapeHtml(item.archive_name)} · ${escapeHtml(short(item.revision))}</option>`).join("") : '<option>No imported Candidates</option>'}</select>
        ${candidate ? `<div class="idea-identity"><div class="stat-row"><span>Candidate entity</span><code>${escapeHtml(candidate.entity_id)}</code></div><div class="stat-row"><span>Archive SHA-256</span><code>${escapeHtml(candidate.archive_sha256)}</code></div></div>` : ""}
        <p class="field-help">The server reads the Candidate's immutable archive evidence. The browser does not choose Retester project, task, executable, runtime, or filesystem paths.</p>
        <button class="button button-primary" type="button" data-retester-action="start" ${canStart ? "" : "disabled"}>${bound ? "Retester result already bound" : runtimeReady ? "Run native Retester" : "Native Retester unavailable"}</button>
        <p class="idea-save-status" data-retester-status>${escapeHtml(detail || "")}</p>
      </div>
      <div class="panel" data-accent="purple"><div class="panel-heading"><div><p class="eyebrow">Producer readback</p><h2>Historical result custody</h2></div></div>${resultDetail(bound)}</div>
    </div>`;
}

let generation = 0;
let state = { phase: "idle", candidates: [], results: [], runtimeReady: false, selectedIndex: 0, detail: "" };

function overviewPanel() {
  if (!overviewRoute()) return null;
  return document.querySelector('.content-inner .panel.wide-panel[data-accent="cyan"]');
}

async function load() {
  const current = ++generation;
  const panel = overviewPanel();
  if (!panel) return;
  state = { ...state, phase: "loading", detail: "Loading native Retester custody…" };
  render(panel, state);
  try {
    const [candidates, results, runtime] = await Promise.all([
      fetchCandidates(),
      fetchHistoricalResults(),
      fetchRuntimeStatus(),
    ]);
    if (current !== generation || !overviewRoute()) return;
    state = { phase: "loaded", candidates, results, runtimeReady: retesterRuntimeReady(runtime), selectedIndex: Math.min(state.selectedIndex, Math.max(0, candidates.length - 1)), detail: "" };
  } catch (error) {
    if (current !== generation || !overviewRoute()) return;
    state = { ...state, phase: "failed", detail: error instanceof Error ? error.message : "Native Retester workspace unavailable" };
  }
  render(overviewPanel(), state);
}

async function start(button) {
  if (state.phase === "loading" || !state.runtimeReady) return;
  const candidate = state.candidates[state.selectedIndex];
  if (!candidate) return;
  button.disabled = true;
  button.textContent = "Running native Retester…";
  try {
    const result = await startRetester(candidate);
    if (!overviewRoute()) return;
    const results = [...state.results.filter((item) => item.entity_id !== result.entity_id), result];
    state = { ...state, phase: "loaded", results, detail: result.reused ? "Existing exact Retester result reused." : "Native Retester execution captured." };
  } catch (error) {
    state = { ...state, phase: "failed", detail: error instanceof Error ? error.message : "Native Retester execution failed" };
  }
  render(overviewPanel(), state);
}

if (typeof document !== "undefined") {
  document.addEventListener("change", (event) => {
    if (!overviewRoute() || event.target?.id !== "retester-candidate") return;
    const selectedIndex = Number(event.target.value);
    if (!Number.isInteger(selectedIndex) || selectedIndex < 0 || selectedIndex >= state.candidates.length) return;
    state = { ...state, selectedIndex, detail: "" };
    render(overviewPanel(), state);
  });
  document.addEventListener("click", (event) => {
    const button = event.target?.closest?.('[data-retester-action="start"]');
    if (button && overviewRoute()) void start(button);
  });
  const observer = new MutationObserver(() => {
    if (overviewRoute() && overviewPanel() && !overviewPanel().querySelector("[data-retester-overview]")) void load();
    if (!overviewRoute()) generation += 1;
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void load();
}
