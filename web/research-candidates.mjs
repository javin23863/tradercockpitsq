const CANDIDATES_API_PATH = "/api/research/candidates";
const NATIVE_JOBS_API_PATH = "/api/research/native-jobs";
const SQX_OUTPUTS_API_PATH = "/api/sqx-outputs";
const CANDIDATE_SCHEMA = "tc.research-candidate.v1";
const CANDIDATE_CATALOG_SCHEMA = "tc.research-candidate-catalog.v1";
const NATIVE_JOB_CATALOG_SCHEMA = "tc.research-native-job-catalog.v1";
const OUTPUT_LIST_SCHEMA = "tc.sqx-builder-output-list.v1";
const ASSOCIATION_MODE = "operator_selected_exact_native_output";

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function candidatesRoute() {
  if (globalThis.location?.pathname !== "/research") return false;
  const params = new URLSearchParams(globalThis.location.search || "");
  return params.get("stage") === "construct" && params.get("tab") === "candidates";
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

export function candidateFromPayload(payload) {
  const requiredStrings = [
    "entity_id", "revision", "native_job_entity_id", "native_job_revision",
    "configuration_entity_id", "configuration_revision", "archive_name",
    "archive_relative_path", "archive_ref", "archive_sha256", "strategy_ref",
    "strategy_sha256", "settings_ref", "settings_sha256", "sqx_build", "association_mode",
  ];
  if (!payload || payload.schema !== CANDIDATE_SCHEMA || requiredStrings.some((key) => typeof payload[key] !== "string" || !payload[key])) {
    throw new Error("Candidate identity is invalid");
  }
  if (payload.association_mode !== ASSOCIATION_MODE || payload.sqx_build !== "144.2953") {
    throw new Error("Candidate provenance is invalid");
  }
  for (const key of ["archive_sha256", "strategy_sha256", "settings_sha256"]) {
    if (!/^[0-9a-f]{64}$/.test(payload[key])) throw new Error("Candidate evidence digest is invalid");
  }
  if (!payload.archive_ref.endsWith(payload.archive_sha256) || !payload.strategy_ref.endsWith(payload.strategy_sha256) || !payload.settings_ref.endsWith(payload.settings_sha256)) {
    throw new Error("Candidate evidence binding is inconsistent");
  }
  if (payload.archive_relative_path !== `user/projects/Builder/databanks/Results/${payload.archive_name}`) {
    throw new Error("Candidate archive path is inconsistent");
  }
  return payload;
}

export function candidateCatalogFromPayload(payload) {
  if (!payload || payload.schema !== CANDIDATE_CATALOG_SCHEMA || !Array.isArray(payload.candidates)) {
    throw new Error("Candidate catalog schema mismatch");
  }
  return payload.candidates.map(candidateFromPayload);
}

export function nativeJobsFromPayload(payload) {
  if (!payload || payload.schema !== NATIVE_JOB_CATALOG_SCHEMA || !Array.isArray(payload.jobs)) {
    throw new Error("Native job catalog schema mismatch");
  }
  return payload.jobs.filter((job) => job?.schema === "tc.research-native-job.v1" && job?.state === "submitted" && typeof job.entity_id === "string" && typeof job.revision === "string");
}

export function outputsFromPayload(payload) {
  if (!payload || payload.schema !== OUTPUT_LIST_SCHEMA || !Array.isArray(payload.outputs) || !payload.runtime || typeof payload.runtime.ready !== "boolean") {
    throw new Error("Native output catalog schema mismatch");
  }
  return {
    ready: payload.runtime.ready === true,
    importAvailable: payload.import_available === true,
    reason: payload.import_reason || payload.runtime.status || null,
    outputs: payload.outputs.filter((output) => output?.inspectable === true && typeof output.archive === "string" && /^[0-9a-f]{64}$/.test(output.archive_sha256 || "")),
  };
}

export async function fetchCandidates(fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(CANDIDATES_API_PATH, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Candidate catalog read failed");
  return candidateCatalogFromPayload(payload);
}

export async function fetchSubmittedNativeJobs(fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(NATIVE_JOBS_API_PATH, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Native job catalog read failed");
  return nativeJobsFromPayload(payload);
}

export async function fetchNativeOutputs(fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(SQX_OUTPUTS_API_PATH, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Native output catalog read failed");
  return outputsFromPayload(payload);
}

export async function importNativeCandidate(job, output, fetchImpl = globalThis.fetch) {
  if (!job || job.state !== "submitted" || !output || output.inspectable !== true) {
    throw new Error("Candidate import requires one submitted native job and one inspectable native output");
  }
  const response = await fetchImpl(CANDIDATES_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({
      action: "import-native-output",
      native_job_entity_id: job.entity_id,
      expected_native_job_revision: job.revision,
      archive: output.archive,
      expected_archive_sha256: output.archive_sha256,
    }),
  });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Candidate import failed");
  const candidate = candidateFromPayload(payload);
  if (candidate.native_job_entity_id !== job.entity_id || candidate.native_job_revision !== job.revision || candidate.archive_name !== output.archive || candidate.archive_sha256 !== output.archive_sha256) {
    throw new Error("Imported Candidate does not bind the selected native identities");
  }
  return candidate;
}

function short(value) {
  const text = String(value || "");
  return text.length > 18 ? `…${text.slice(-16)}` : text;
}

function optionMarkup(items, valueKey, label) {
  return items.map((item, index) => `<option value="${index}">${escapeHtml(label(item, valueKey))}</option>`).join("");
}

function candidateRows(candidates) {
  if (!candidates.length) return `<div class="empty-state"><div class="empty-icon">—</div><div><strong>No imported native candidates</strong><p>Run Builder, then explicitly bind an exact submitted native job to an exact inspectable Results archive.</p></div></div>`;
  return `<div class="idea-catalog-list">${candidates.map((candidate) => `<div class="idea-catalog-item" data-candidate-entity-id="${escapeHtml(candidate.entity_id)}"><strong>${escapeHtml(candidate.archive_name)}</strong><span>${escapeHtml(short(candidate.revision))}</span></div>`).join("")}</div>`;
}

function renderWorkspace(panel, state) {
  if (!panel?.isConnected) return;
  const { candidates, jobs, outputs, phase, detail, outputState } = state;
  const canImport = phase !== "loading" && jobs.length > 0 && outputs.length > 0 && outputState.importAvailable;
  panel.dataset.researchCandidatesWorkspace = phase;
  const body = panel.querySelector(".empty-state");
  const host = body?.parentElement || panel;
  if (body) body.remove();
  let workspace = panel.querySelector("[data-candidate-workspace-body]");
  if (!workspace) {
    workspace = document.createElement("div");
    workspace.dataset.candidateWorkspaceBody = "";
    host.append(workspace);
  }
  workspace.innerHTML = `
    <div class="context-callout"><span class="callout-icon">↳</span><div><span class="eyebrow">Provenance boundary</span><strong>Explicit native output association</strong><span>SQX output archives do not expose a TraderCockpit job id. Candidate custody records the exact submitted job revision and exact selected archive bytes without pretending SQX supplied a hidden link.</span></div></div>
    <div class="dashboard-grid">
      <div class="panel" data-accent="cyan"><div class="panel-heading"><div><p class="eyebrow">Imported custody</p><h2>Candidate identities</h2></div></div>${candidateRows(candidates)}</div>
      <div class="panel" data-accent="orange"><div class="panel-heading"><div><p class="eyebrow">Native import</p><h2>Bind survivor</h2></div></div>
        <label class="field-label" for="candidate-native-job">Submitted native job</label>
        <select id="candidate-native-job" class="idea-editor" ${jobs.length ? "" : "disabled"}>${jobs.length ? optionMarkup(jobs, "revision", (job) => `${short(job.revision)} · ${short(job.configuration_revision)}`) : '<option>No submitted native jobs</option>'}</select>
        <label class="field-label" for="candidate-native-output">SQX Results archive</label>
        <select id="candidate-native-output" class="idea-editor" ${outputs.length ? "" : "disabled"}>${outputs.length ? optionMarkup(outputs, "archive", (output) => `${output.archive} · ${short(output.archive_sha256)}`) : '<option>No inspectable native outputs</option>'}</select>
        <p class="field-help">Only the selected archive name and SHA-256 plus exact native-job custody identity are submitted. The server resolves the bounded Results path itself and captures the archive bytes atomically.</p>
        <p class="idea-save-status" data-candidate-status>${escapeHtml(detail || outputState.reason || "")}</p>
        <button class="button button-primary" type="button" data-candidate-action="import" ${canImport ? "" : "disabled"}>Import exact native survivor</button>
      </div>
    </div>`;
}

let generation = 0;
let state = { phase: "idle", candidates: [], jobs: [], outputs: [], outputState: { importAvailable: false, reason: null }, detail: "" };

function candidatePanel() {
  if (!candidatesRoute()) return null;
  return document.querySelector('.content-inner .panel.wide-panel[data-accent="purple"]');
}

async function loadWorkspace() {
  const current = ++generation;
  const panel = candidatePanel();
  if (!panel) return;
  state = { ...state, phase: "loading", detail: "Loading native Candidate custody…" };
  renderWorkspace(panel, state);
  try {
    const [candidates, jobs, outputState] = await Promise.all([
      fetchCandidates(),
      fetchSubmittedNativeJobs(),
      fetchNativeOutputs(),
    ]);
    if (current !== generation || !candidatesRoute()) return;
    state = { phase: "loaded", candidates, jobs, outputs: outputState.outputs, outputState, detail: "" };
  } catch (error) {
    if (current !== generation || !candidatesRoute()) return;
    state = { ...state, phase: "failed", detail: error instanceof Error ? error.message : "Candidate workspace unavailable" };
  }
  renderWorkspace(candidatePanel(), state);
}

async function importSelection(button) {
  if (state.phase === "loading") return;
  const jobIndex = Number(document.querySelector("#candidate-native-job")?.value ?? -1);
  const outputIndex = Number(document.querySelector("#candidate-native-output")?.value ?? -1);
  const job = state.jobs[jobIndex];
  const output = state.outputs[outputIndex];
  if (!job || !output) return;
  button.disabled = true;
  button.textContent = "Importing exact archive…";
  try {
    const candidate = await importNativeCandidate(job, output);
    if (!candidatesRoute()) return;
    const candidates = state.candidates.some((item) => item.entity_id === candidate.entity_id)
      ? state.candidates
      : [...state.candidates, candidate];
    state = { ...state, phase: "loaded", candidates, detail: candidate.reused ? "Existing exact Candidate custody reused." : "Exact native Candidate imported." };
  } catch (error) {
    state = { ...state, phase: "failed", detail: error instanceof Error ? error.message : "Candidate import failed" };
  }
  renderWorkspace(candidatePanel(), state);
}

if (typeof document !== "undefined") {
  document.addEventListener("click", (event) => {
    const button = event.target?.closest?.('[data-candidate-action="import"]');
    if (button && candidatesRoute()) void importSelection(button);
  });
  const observer = new MutationObserver(() => {
    if (candidatesRoute() && candidatePanel() && !candidatePanel().querySelector("[data-candidate-workspace-body]")) void loadWorkspace();
    if (!candidatesRoute()) generation += 1;
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void loadWorkspace();
}
