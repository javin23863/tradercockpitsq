import { researchLocationMatches } from "./model.mjs";
import {
  historicalResultCatalogFromPayload,
  historicalResultFromPayload,
} from "./research-backtest.mjs";

const CONFIGURATIONS_API_PATH = "/api/research/configurations";
const NATIVE_JOBS_API_PATH = "/api/research/native-jobs";
const CANDIDATES_API_PATH = "/api/research/candidates";
const HISTORICAL_RESULTS_API_PATH = "/api/research/historical-results";
const SQX_BUILD = "144.2953";

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function configurationRoute() {
  return researchLocationMatches(globalThis.location, "validate", "configuration");
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
  const prefix = "tc-evidence:sha256:";
  return typeof value === "string" && value.startsWith(prefix) ? digest(value.slice(prefix.length)) : "";
}

function requireString(payload, key, label) {
  if (typeof payload?.[key] !== "string" || !payload[key]) throw new Error(`${label} is invalid`);
  return payload[key];
}

function exactReadPath(path, entityId) {
  return `${path}?entityId=${encodeURIComponent(entityId)}`;
}

export function configurationFromPayload(payload) {
  if (
    !payload
    || payload.schema !== "tc.research-configuration.v1"
    || payload.state !== "approved"
    || payload.sqx_build !== SQX_BUILD
    || payload.assembly_mode !== "exact_native_builder_task_snapshot"
    || payload.source_entry !== "Build-Task1.xml"
    || payload.approval?.approved !== true
    || !Array.isArray(payload.approved_changes)
    || payload.approved_changes.length !== 0
    || payload.review?.changed !== false
  ) {
    throw new Error("Approved configuration custody is invalid");
  }
  for (const key of ["entity_id", "revision", "source_project_path", "source_project_ref", "source_entry_ref", "executable_xml_ref"]) {
    requireString(payload, key, "Approved configuration identity");
  }
  if (
    !digest(payload.source_project_sha256)
    || evidenceDigest(payload.source_project_ref) !== payload.source_project_sha256
    || !digest(payload.executable_xml_sha256)
    || evidenceDigest(payload.executable_xml_ref) !== payload.executable_xml_sha256
    || payload.source_entry_ref !== payload.executable_xml_ref
  ) {
    throw new Error("Approved configuration evidence binding is invalid");
  }
  return payload;
}

export function nativeJobFromPayload(payload) {
  if (
    !payload
    || payload.schema !== "tc.research-native-job.v1"
    || payload.state !== "submitted"
    || payload.operation !== "builder_loadconfig_start"
    || payload.sqx_build !== SQX_BUILD
    || payload.partial_side_effect !== false
    || !Array.isArray(payload.receipts)
    || payload.receipts.length !== 2
    || payload.receipts.some((item) => item?.state !== "completed")
  ) {
    throw new Error("Submitted Builder job custody is invalid");
  }
  for (const key of ["entity_id", "revision", "configuration_entity_id", "configuration_revision", "executable_xml_ref", "staged_config_relative_path", "launcher_sha256"]) {
    requireString(payload, key, "Builder job identity");
  }
  if (
    !digest(payload.executable_xml_sha256)
    || evidenceDigest(payload.executable_xml_ref) !== payload.executable_xml_sha256
    || !digest(payload.launcher_sha256)
  ) {
    throw new Error("Builder job evidence binding is invalid");
  }
  return payload;
}

export function candidateChainFromPayload(payload) {
  if (!payload || payload.schema !== "tc.research-candidate.v1" || payload.sqx_build !== SQX_BUILD || payload.association_mode !== "operator_selected_exact_native_output") {
    throw new Error("Candidate custody is invalid");
  }
  for (const key of [
    "entity_id", "revision", "native_job_entity_id", "native_job_revision",
    "configuration_entity_id", "configuration_revision", "archive_name",
    "archive_relative_path", "archive_ref", "strategy_ref", "settings_ref",
  ]) requireString(payload, key, "Candidate identity");
  for (const [refKey, digestKey] of [["archive_ref", "archive_sha256"], ["strategy_ref", "strategy_sha256"], ["settings_ref", "settings_sha256"]]) {
    if (!digest(payload[digestKey]) || evidenceDigest(payload[refKey]) !== payload[digestKey]) {
      throw new Error("Candidate evidence binding is invalid");
    }
  }
  if (payload.archive_relative_path !== `user/projects/Builder/databanks/Results/${payload.archive_name}`) {
    throw new Error("Candidate native archive path is invalid");
  }
  return payload;
}

export function verifyExecutedConfigurationChain({ historicalResult, candidate, nativeJob, configuration }) {
  const result = historicalResultFromPayload(historicalResult);
  const candidateRecord = candidateChainFromPayload(candidate);
  const job = nativeJobFromPayload(nativeJob);
  const config = configurationFromPayload(configuration);

  if (result.state !== "completed" || result.execution_completed !== true || result.validation_state !== "not_run") {
    throw new Error("Executed chain requires one completed native historical result");
  }
  if (
    result.candidate_entity_id !== candidateRecord.entity_id
    || result.candidate_revision !== candidateRecord.revision
    || result.candidate_archive_name !== candidateRecord.archive_name
    || result.candidate_archive_ref !== candidateRecord.archive_ref
    || result.candidate_archive_sha256 !== candidateRecord.archive_sha256
  ) {
    throw new Error("Historical result does not preserve exact Candidate custody");
  }
  if (
    candidateRecord.native_job_entity_id !== job.entity_id
    || candidateRecord.native_job_revision !== job.revision
  ) {
    throw new Error("Candidate does not preserve exact Builder job custody");
  }
  if (
    candidateRecord.configuration_entity_id !== config.entity_id
    || candidateRecord.configuration_revision !== config.revision
    || job.configuration_entity_id !== config.entity_id
    || job.configuration_revision !== config.revision
  ) {
    throw new Error("Builder/Candidate configuration revision binding is inconsistent");
  }
  if (
    job.executable_xml_ref !== config.executable_xml_ref
    || job.executable_xml_sha256 !== config.executable_xml_sha256
  ) {
    throw new Error("Builder job did not execute the approved configuration bytes");
  }
  if ([result.sqx_build, candidateRecord.sqx_build, job.sqx_build, config.sqx_build].some((value) => value !== SQX_BUILD)) {
    throw new Error("Executed chain producer build identity is inconsistent");
  }
  if (
    !digest(result.result_archive_sha256)
    || evidenceDigest(result.result_archive_ref) !== result.result_archive_sha256
    || result.result_archive_sha256 === candidateRecord.archive_sha256
  ) {
    throw new Error("Executed chain result archive identity is invalid");
  }

  return Object.freeze({ historicalResult: result, candidate: candidateRecord, nativeJob: job, configuration: config });
}

async function fetchExact(path, entityId, parser, fallback, fetchImpl) {
  const response = await fetchImpl(exactReadPath(path, entityId), { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, fallback);
  return parser(payload);
}

export async function fetchCompletedHistoricalResults(fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(HISTORICAL_RESULTS_API_PATH, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Historical result catalog read failed");
  return historicalResultCatalogFromPayload(payload).filter((item) => item.state === "completed");
}

export async function fetchExecutedConfigurationChain(historicalResultEntityId, fetchImpl = globalThis.fetch) {
  if (typeof historicalResultEntityId !== "string" || !historicalResultEntityId) throw new Error("Historical result selection is invalid");
  const result = await fetchExact(HISTORICAL_RESULTS_API_PATH, historicalResultEntityId, historicalResultFromPayload, "Historical result read failed", fetchImpl);
  const candidate = await fetchExact(CANDIDATES_API_PATH, result.candidate_entity_id, candidateChainFromPayload, "Candidate read failed", fetchImpl);
  const [nativeJob, configuration] = await Promise.all([
    fetchExact(NATIVE_JOBS_API_PATH, candidate.native_job_entity_id, nativeJobFromPayload, "Builder job read failed", fetchImpl),
    fetchExact(CONFIGURATIONS_API_PATH, candidate.configuration_entity_id, configurationFromPayload, "Configuration read failed", fetchImpl),
  ]);
  return verifyExecutedConfigurationChain({ historicalResult: result, candidate, nativeJob, configuration });
}

function short(value) {
  const text = String(value || "");
  return text.length > 22 ? `…${text.slice(-20)}` : text;
}

function identityRow(label, value) {
  return `<div class="stat-row"><span>${escapeHtml(label)}</span><code>${escapeHtml(value)}</code></div>`;
}

function chainMarkup(chain) {
  if (!chain) return `<div class="empty-state"><div class="empty-icon">—</div><div><strong>No executed chain selected</strong><p>Choose one completed native Retester result and inspect its exact custody chain. Catalog order is not treated as recency or quality.</p></div></div>`;
  const { configuration, nativeJob, candidate, historicalResult } = chain;
  return `<div data-backtest-configuration-chain="verified">
    <div class="context-callout"><span class="callout-icon">↳</span><div><span class="eyebrow">Exact executed custody</span><strong>Configuration → Builder job → Candidate → Retester result</strong><span>Every revision and byte identity below was cross-checked against the canonical backend reads before rendering.</span></div></div>
    <div class="dashboard-grid">
      <div class="panel" data-accent="orange"><div class="panel-heading"><div><p class="eyebrow">1 · Approved configuration</p><h2>Executable bytes</h2></div></div><div class="idea-identity">
        ${identityRow("Configuration entity", configuration.entity_id)}${identityRow("Approved revision", configuration.revision)}${identityRow("Source project SHA-256", configuration.source_project_sha256)}${identityRow("Executable XML SHA-256", configuration.executable_xml_sha256)}${identityRow("Source path", configuration.source_project_path)}
      </div></div>
      <div class="panel" data-accent="red"><div class="panel-heading"><div><p class="eyebrow">2 · Native Builder job</p><h2>Submitted control</h2></div></div><div class="idea-identity">
        ${identityRow("Native job entity", nativeJob.entity_id)}${identityRow("Submitted revision", nativeJob.revision)}${identityRow("Operation", nativeJob.operation)}${identityRow("Launcher SHA-256", nativeJob.launcher_sha256)}${identityRow("Executed XML SHA-256", nativeJob.executable_xml_sha256)}
      </div></div>
      <div class="panel" data-accent="purple"><div class="panel-heading"><div><p class="eyebrow">3 · Candidate</p><h2>Native survivor</h2></div></div><div class="idea-identity">
        ${identityRow("Candidate entity", candidate.entity_id)}${identityRow("Candidate revision", candidate.revision)}${identityRow("Archive", candidate.archive_name)}${identityRow("Archive SHA-256", candidate.archive_sha256)}${identityRow("Association", candidate.association_mode)}
      </div></div>
      <div class="panel" data-accent="cyan"><div class="panel-heading"><div><p class="eyebrow">4 · Retester result</p><h2>Producer readback</h2></div></div><div class="idea-identity">
        ${identityRow("Historical result", historicalResult.entity_id)}${identityRow("Completed revision", historicalResult.revision)}${identityRow("Result archive", historicalResult.result_archive_name)}${identityRow("Result SHA-256", historicalResult.result_archive_sha256)}${identityRow("Engine SHA-256", historicalResult.engine_sha256)}${identityRow("Retester launcher SHA-256", historicalResult.launcher_sha256)}
      </div><div class="requirement-item"><div><strong>Validation state</strong><span class="status-badge status-unavailable"><span class="status-dot"></span>Not run</span></div><p>This chain proves native execution custody only. It does not claim validation, robustness, promotion, champion, or deployment status.</p></div></div>
    </div>
  </div>`;
}

function render(panel, state) {
  if (!panel?.isConnected) return;
  const host = panel.querySelector(".empty-state")?.parentElement || panel;
  panel.querySelector(".empty-state")?.remove();
  let workspace = panel.querySelector("[data-backtest-configuration-workspace]");
  if (!workspace) {
    workspace = document.createElement("div");
    workspace.dataset.backtestConfigurationWorkspace = "";
    host.append(workspace);
  }
  const disabled = state.phase === "loading" || !state.results.length;
  workspace.innerHTML = `
    <div class="panel" data-accent="cyan">
      <div class="panel-heading"><div><p class="eyebrow">Historical result selector</p><h2>Choose exact execution</h2></div></div>
      <label class="field-label" for="backtest-configuration-result">Completed native Retester result</label>
      <select id="backtest-configuration-result" class="idea-editor" ${disabled ? "disabled" : ""}>${state.results.length ? state.results.map((item, index) => `<option value="${index}" ${index === state.selectedIndex ? "selected" : ""}>${escapeHtml(short(item.revision))} · ${escapeHtml(item.result_archive_name)}</option>`).join("") : '<option>No completed native Retester results</option>'}</select>
      <p class="field-help">Selection is explicit. TraderCockpit does not infer a latest, best, promoted, or current-quality result from catalog order.</p>
      <button class="button button-primary" type="button" data-backtest-configuration-action="inspect" ${disabled ? "disabled" : ""}>Verify executed custody chain</button>
      <p class="idea-save-status" data-backtest-configuration-status>${escapeHtml(state.detail || "")}</p>
    </div>
    ${chainMarkup(state.chain)}`;
}

let generation = 0;
let state = { phase: "idle", results: [], selectedIndex: 0, chain: null, detail: "" };

function configurationPanel() {
  if (!configurationRoute()) return null;
  return document.querySelector('[data-research-host="configuration"]');
}

async function loadCatalog() {
  const current = ++generation;
  const panel = configurationPanel();
  if (!panel) return;
  state = { ...state, phase: "loading", chain: null, detail: "Loading completed native Retester custody…" };
  render(panel, state);
  try {
    const results = await fetchCompletedHistoricalResults();
    if (current !== generation || !configurationRoute()) return;
    state = { phase: "loaded", results, selectedIndex: 0, chain: null, detail: "" };
  } catch (error) {
    if (current !== generation || !configurationRoute()) return;
    state = { phase: "failed", results: [], selectedIndex: 0, chain: null, detail: error instanceof Error ? error.message : "Backtest Configuration catalog unavailable" };
  }
  render(configurationPanel(), state);
}

async function inspectSelected(button) {
  if (state.phase === "loading") return;
  const selected = state.results[state.selectedIndex];
  if (!selected) return;
  button.disabled = true;
  button.textContent = "Verifying exact custody…";
  try {
    const chain = await fetchExecutedConfigurationChain(selected.entity_id);
    if (!configurationRoute()) return;
    state = { ...state, phase: "loaded", chain, detail: "Exact executed custody chain verified." };
  } catch (error) {
    if (!configurationRoute()) return;
    state = { ...state, phase: "failed", chain: null, detail: `Chain refused: ${error instanceof Error ? error.message : "custody verification failed"}` };
  }
  render(configurationPanel(), state);
}

if (typeof document !== "undefined") {
  document.addEventListener("change", (event) => {
    if (!configurationRoute() || event.target?.id !== "backtest-configuration-result") return;
    const selectedIndex = Number(event.target.value);
    if (!Number.isInteger(selectedIndex) || selectedIndex < 0 || selectedIndex >= state.results.length) return;
    state = { ...state, selectedIndex, chain: null, detail: "" };
    render(configurationPanel(), state);
  });
  document.addEventListener("click", (event) => {
    const button = event.target?.closest?.('[data-backtest-configuration-action="inspect"]');
    if (button && configurationRoute()) void inspectSelected(button);
  });
  const observer = new MutationObserver(() => {
    if (configurationRoute() && configurationPanel() && !configurationPanel().querySelector("[data-backtest-configuration-workspace]")) void loadCatalog();
    if (!configurationRoute()) generation += 1;
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void loadCatalog();
}
