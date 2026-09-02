import { researchLocationMatches } from "./model.mjs";
import { fetchIdeaCatalog } from "./research-ideas.mjs";
import { fetchHistoricalResults, historicalResultFromPayload } from "./research-backtest.mjs";
import {
  fetchRobustnessCatalog,
  robustnessResultFromPayload,
  robustnessResultsForHistorical,
} from "./research-backtest-robustness.mjs";

const PROOFS_API_PATH = "/api/research/proofs";
const STATUS_API_PATH = "/api/status";
const PROOF_SCHEMA = "tc.research-proof.v1";
const PROOF_CATALOG_SCHEMA = "tc.research-proof-catalog.v1";
const STATUS_SCHEMA = "tc.runtime-status.v1";
const IDEA_SCHEMA = "tc.research-idea.v1";
const CONFIGURATION_SCHEMA = "tc.research-configuration.v1";
const NATIVE_JOB_SCHEMA = "tc.research-native-job.v1";
const CANDIDATE_SCHEMA = "tc.research-candidate.v1";
const HISTORICAL_RESULT_SCHEMA = "tc.research-historical-result.v1";
const TRADES_SCHEMA = "tc.research-historical-trades.v1";
const ROBUSTNESS_SCHEMA = "tc.research-native-robustness.v1";
const OUTCOME_UNREAD = "producer_result_captured_outcome_unread";
const ASSOCIATION_MODE = "operator_selected_exact_idea_revision";

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function proofRoute() {
  return researchLocationMatches(globalThis.location, "validate", "evidence");
}

export function proofEntityFromLocation(search = globalThis.location?.search || "") {
  const params = new URLSearchParams(search);
  if (!params.has("proofEntity")) return { present: false, entityId: "" };
  const value = params.get("proofEntity");
  return {
    present: true,
    entityId: typeof value === "string" && /^tc-research:proof:v1:[0-9a-f-]{36}$/.test(value) ? value : "",
  };
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

function revision(value, kind) {
  const pattern = new RegExp(`^tc-research-revision:${kind}:sha256:[0-9a-f]{64}$`);
  return typeof value === "string" && pattern.test(value) ? value : "";
}

function entity(value, kind) {
  const pattern = new RegExp(`^tc-research:${kind}:v1:[0-9a-f-]{36}$`);
  return typeof value === "string" && pattern.test(value) ? value : "";
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

function exactEvidence(ref, sha) {
  return Boolean(digest(sha) && evidenceDigest(ref) === sha);
}

export function productStatusFromPayload(payload) {
  const required = [payload?.application, payload?.research_backend, payload?.research_custody];
  if (
    !payload
    || payload.schema !== STATUS_SCHEMA
    || required.some((item) => !item || typeof item !== "object" || Array.isArray(item) || typeof item.status !== "string" || !item.status)
  ) {
    throw new Error("Current product status is invalid");
  }
  return payload;
}

export async function fetchProductStatus(fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(STATUS_API_PATH, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Current product status read failed");
  return productStatusFromPayload(payload);
}

export async function fetchProductStatusOrUnavailable(fetchImpl = globalThis.fetch) {
  try {
    return await fetchProductStatus(fetchImpl);
  } catch {
    return null;
  }
}

export function proofFromPayload(payload) {
  if (
    !payload
    || payload.schema !== PROOF_SCHEMA
    || payload.association_mode !== ASSOCIATION_MODE
    || payload.sqx_build !== "144.2953"
    || !entity(payload.entity_id, "proof")
    || !revision(payload.revision, "proof")
    || evidenceDigest(payload.content_ref) === ""
    || payload.truth?.validation_execution_completed !== true
    || payload.truth?.producer_validation_outcome !== OUTCOME_UNREAD
    || payload.truth?.producer_verdict_available !== false
  ) {
    throw new Error("Research Proof identity is invalid");
  }
  const idea = payload.idea;
  const configuration = payload.configuration;
  const nativeJob = payload.native_job;
  const candidate = payload.candidate;
  const historical = historicalResultFromPayload(payload.historical_result);
  const trades = payload.trades;
  const validation = robustnessResultFromPayload(payload.validation);
  if (
    idea?.schema !== IDEA_SCHEMA
    || !entity(idea.entity_id, "idea")
    || !revision(idea.revision, "idea")
    || evidenceDigest(idea.content_ref) === ""
    || configuration?.schema !== CONFIGURATION_SCHEMA
    || configuration.state !== "approved"
    || !entity(configuration.entity_id, "configuration")
    || !revision(configuration.revision, "configuration")
    || !exactEvidence(configuration.source_project_ref, configuration.source_project_sha256)
    || !exactEvidence(configuration.executable_xml_ref, configuration.executable_xml_sha256)
    || nativeJob?.schema !== NATIVE_JOB_SCHEMA
    || nativeJob.state !== "submitted"
    || !entity(nativeJob.entity_id, "native-job")
    || !revision(nativeJob.revision, "native-job")
    || nativeJob.configuration_entity_id !== configuration.entity_id
    || nativeJob.configuration_revision !== configuration.revision
    || !digest(nativeJob.launcher_sha256)
    || candidate?.schema !== CANDIDATE_SCHEMA
    || !entity(candidate.entity_id, "candidate")
    || !revision(candidate.revision, "candidate")
    || candidate.native_job_entity_id !== nativeJob.entity_id
    || candidate.native_job_revision !== nativeJob.revision
    || candidate.configuration_entity_id !== configuration.entity_id
    || candidate.configuration_revision !== configuration.revision
    || !exactEvidence(candidate.archive_ref, candidate.archive_sha256)
    || historical.state !== "completed"
    || historical.execution_completed !== true
    || historical.candidate_entity_id !== candidate.entity_id
    || historical.candidate_revision !== candidate.revision
    || trades?.schema !== TRADES_SCHEMA
    || trades.historical_result_entity_id !== historical.entity_id
    || trades.historical_result_revision !== historical.revision
    || trades.candidate_entity_id !== candidate.entity_id
    || trades.candidate_revision !== candidate.revision
    || trades.result_archive_ref !== historical.result_archive_ref
    || trades.result_archive_sha256 !== historical.result_archive_sha256
    || validation.source_historical_result_entity_id !== historical.entity_id
    || validation.source_historical_result_revision !== historical.revision
    || validation.source_result_archive_ref !== historical.result_archive_ref
    || validation.source_result_archive_sha256 !== historical.result_archive_sha256
    || validation.producer_outcome_state !== OUTCOME_UNREAD
    || validation.schema !== ROBUSTNESS_SCHEMA
  ) {
    throw new Error("Research Proof chain is inconsistent");
  }
  return payload;
}

export function proofCatalogFromPayload(payload) {
  if (!payload || payload.schema !== PROOF_CATALOG_SCHEMA || !Array.isArray(payload.proofs)) {
    throw new Error("Research Proof catalog schema mismatch");
  }
  for (const item of payload.proofs) {
    if (
      !entity(item?.entity_id, "proof")
      || !revision(item?.revision, "proof")
      || !entity(item?.idea_entity_id, "idea")
      || !revision(item?.idea_revision, "idea")
      || !entity(item?.historical_result_entity_id, "historical-result")
      || !revision(item?.historical_result_revision, "historical-result")
      || evidenceDigest(item?.validation_ref) === ""
      || item?.producer_validation_outcome !== OUTCOME_UNREAD
    ) {
      throw new Error("Research Proof catalog identity is invalid");
    }
  }
  return payload.proofs;
}

export async function fetchProofCatalog(fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(PROOFS_API_PATH, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Research Proof catalog read failed");
  return proofCatalogFromPayload(payload);
}

export async function fetchProof(entityId, fetchImpl = globalThis.fetch) {
  if (!entity(entityId, "proof")) throw new Error("Research Proof entity is invalid");
  const path = `${PROOFS_API_PATH}?${new URLSearchParams({ entityId }).toString()}`;
  const response = await fetchImpl(path, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Research Proof read failed");
  return proofFromPayload(payload);
}

export async function createProof({ idea, historical, validation }, fetchImpl = globalThis.fetch) {
  if (!idea || !entity(idea.entity_id, "idea") || !revision(idea.revision, "idea")) {
    throw new Error("Exact Idea revision is required");
  }
  const source = historicalResultFromPayload(historical);
  const result = robustnessResultFromPayload(validation);
  if (
    source.state !== "completed"
    || source.execution_completed !== true
    || result.source_historical_result_entity_id !== source.entity_id
    || result.source_historical_result_revision !== source.revision
  ) {
    throw new Error("Exact completed Historical Result and matching Higher Precision validation are required");
  }
  const response = await fetchImpl(PROOFS_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({
      action: "create-proof",
      idea_entity_id: idea.entity_id,
      idea_revision: idea.revision,
      historical_result_entity_id: source.entity_id,
      historical_result_revision: source.revision,
      validation_ref: result.validation_ref,
    }),
  });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Research Proof creation failed");
  return proofFromPayload(payload);
}

export function proofSelections(ideas, historicalResults, robustnessResults, selectedHistoricalIndex = 0) {
  const completed = Array.isArray(historicalResults)
    ? historicalResults.filter((item) => item?.state === "completed" && item?.execution_completed === true)
    : [];
  const historical = completed[selectedHistoricalIndex] || null;
  const validations = historical ? robustnessResultsForHistorical(robustnessResults || [], historical) : [];
  return { ideas: Array.isArray(ideas) ? ideas : [], completed, historical, validations };
}

function short(value) {
  const text = String(value || "");
  return text.length > 28 ? `…${text.slice(-26)}` : text;
}

function identityRows(rows) {
  return `<div class="idea-identity">${rows.map(([label, value]) => `<div class="stat-row"><span>${escapeHtml(label)}</span><code>${escapeHtml(value)}</code></div>`).join("")}</div>`;
}

function productStatusDetail(productStatus) {
  if (!productStatus) {
    return `<div class="requirement-item" data-proof-current-product-status="unavailable"><div><strong>Current product status</strong><span class="status-badge status-unavailable"><span class="status-dot"></span>Unavailable</span></div><p>Current product status could not be read. Immutable Proof evidence remains historical and does not substitute for live product state.</p></div>`;
  }
  const application = productStatus.application.status;
  const researchBackend = productStatus.research_backend.status;
  const custody = productStatus.research_custody.status;
  const ready = application === "ready" && researchBackend === "ready" && custody === "ready";
  const badgeClass = ready ? "status-ready" : "status-unavailable";
  const label = ready ? "Ready now" : "Current state";
  return `<div class="requirement-item" data-proof-current-product-status="${escapeHtml(productStatus.schema)}"><div><strong>Current product status</strong><span class="status-badge ${badgeClass}"><span class="status-dot"></span>${label}</span></div><p>Live ${escapeHtml(productStatus.schema)} read at this Proof view: application ${escapeHtml(application)}, native research ${escapeHtml(researchBackend)}, custody ${escapeHtml(custody)}. This mutable status is not stored as immutable Proof evidence.</p></div>`;
}

export function proofDetail(record, productStatus = null) {
  if (!record) return `<div class="empty-state"><div class="empty-icon">—</div><div><strong>No Proof selected</strong><p>Create or reopen one exact immutable Research Proof.</p></div></div>`;
  return `<div data-research-proof-entity="${escapeHtml(record.entity_id)}">
    <div class="context-callout"><span class="callout-icon">✓</span><div><span class="eyebrow">Immutable Research Proof</span><strong>Exact historical Research chain recovered</strong><span>The immutable record revalidates the same Idea, native configuration/job, Candidate, Historical Result, Trades, and Higher Precision custody every time it is reopened. Current product status is read separately below.</span></div></div>
    ${identityRows([
      ["Proof revision", record.revision],
      ["Idea revision", record.idea.revision],
      ["Approved configuration", record.configuration.revision],
      ["Builder job", record.native_job.revision],
      ["Candidate", record.candidate.revision],
      ["Historical Result", record.historical_result.revision],
      ["Historical result archive", record.historical_result.result_archive_sha256],
      ["Retester engine", record.historical_result.engine_sha256],
      ["Higher Precision validation", record.validation.validation_ref],
      ["Higher Precision result", record.validation.result_archive_sha256],
    ])}
    <div class="requirement-item"><div><strong>Idea association</strong><span class="status-badge status-ready"><span class="status-dot"></span>Explicit</span></div><p>Operator-selected exact Idea revision. TraderCockpit does not claim SQX generated the native configuration from this Idea.</p></div>
    <div class="requirement-item"><div><strong>Producer validation outcome</strong><span class="status-badge status-unavailable"><span class="status-dot"></span>Outcome unread</span></div><p>Higher Precision execution and exact native result custody are proven. No SQX pass/fail verdict is reconstructed from process completion.</p></div>
    ${productStatusDetail(productStatus)}
  </div>`;
}

let generation = 0;
let state = {
  phase: "idle",
  ideas: [],
  historical: [],
  robustness: [],
  proofs: [],
  selectedIdeaIndex: 0,
  selectedHistoricalIndex: 0,
  selectedValidationIndex: 0,
  proof: null,
  productStatus: null,
  detail: "",
};

function hostPanel() {
  if (!proofRoute()) return null;
  return document.querySelector('[data-research-host="proof"]');
}

function setProofEntityInLocation(entityId) {
  if (!globalThis.history?.replaceState || !globalThis.location) return;
  const params = new URLSearchParams(globalThis.location.search || "");
  params.delete("stage");
  params.set("workspace", "validate");
  params.set("tab", "evidence");
  if (entityId) params.set("proofEntity", entityId); else params.delete("proofEntity");
  globalThis.history.replaceState({}, "", `/research?${params.toString()}`);
}

function render(host, current) {
  if (!host?.isConnected) return;
  host.querySelector(".proof-chain")?.remove();
  host.querySelector(":scope > .empty-state")?.remove();
  let workspace = host.querySelector("[data-research-proof-workspace]");
  if (!workspace) {
    workspace = document.createElement("div");
    workspace.dataset.researchProofWorkspace = "";
    host.append(workspace);
  }
  if (current.phase === "loading") {
    workspace.innerHTML = `<div class="idea-catalog-state">Loading exact Research custody…</div>`;
    return;
  }
  if (current.phase === "failed") {
    workspace.innerHTML = `<div class="idea-catalog-state idea-error">${escapeHtml(current.detail || "Research Proof unavailable")}</div>${proofDetail(current.proof, current.productStatus)}`;
    return;
  }
  if (current.proof) {
    workspace.innerHTML = `${proofDetail(current.proof, current.productStatus)}<div class="idea-actions"><button type="button" class="button button-secondary" data-proof-action="new">Create / select another Proof</button></div>`;
    return;
  }

  const selections = proofSelections(current.ideas, current.historical, current.robustness, current.selectedHistoricalIndex);
  const idea = selections.ideas[current.selectedIdeaIndex] || null;
  const historical = selections.historical;
  const validation = selections.validations[current.selectedValidationIndex] || null;
  const canCreate = Boolean(idea && historical && validation);
  const proofOptions = current.proofs.length
    ? `<label class="field-label" for="proof-existing">Reopen immutable Proof</label><select id="proof-existing" class="idea-editor"><option value="">Choose saved Proof</option>${current.proofs.map((item) => `<option value="${escapeHtml(item.entity_id)}">${escapeHtml(short(item.revision))} · ${escapeHtml(short(item.historical_result_revision))}</option>`).join("")}</select>`
    : `<p class="field-help">No saved user-facing Proof records yet.</p>`;
  workspace.innerHTML = `<div data-proof-create-workspace>
    ${proofOptions}
    <label class="field-label" for="proof-idea">Exact Idea revision</label>
    <select id="proof-idea" class="idea-editor">${selections.ideas.length ? selections.ideas.map((item, index) => `<option value="${index}" ${index === current.selectedIdeaIndex ? "selected" : ""}>${escapeHtml(item.summary || short(item.revision))} · ${escapeHtml(short(item.revision))}</option>`).join("") : '<option value="">No saved Ideas</option>'}</select>
    <p class="field-help">The currently selected exact Idea revision is preserved even if that Idea is revised later.</p>
    <label class="field-label" for="proof-historical">Completed Historical Result</label>
    <select id="proof-historical" class="idea-editor">${selections.completed.length ? selections.completed.map((item, index) => `<option value="${index}" ${index === current.selectedHistoricalIndex ? "selected" : ""}>${escapeHtml(short(item.revision))} · ${escapeHtml(short(item.result_archive_sha256))}</option>`).join("") : '<option value="">No completed Historical Results</option>'}</select>
    <label class="field-label" for="proof-validation">Matching Higher Precision validation</label>
    <select id="proof-validation" class="idea-editor">${selections.validations.length ? selections.validations.map((item, index) => `<option value="${index}" ${index === current.selectedValidationIndex ? "selected" : ""}>${escapeHtml(short(item.validation_ref))} · outcome unread</option>`).join("") : '<option value="">No matching completed Higher Precision run</option>'}</select>
    <p class="field-help">Proof requires exact completed producer custody; it does not turn Higher Precision completion into a pass/fail verdict.</p>
    <div class="idea-actions"><button type="button" class="button button-primary" data-proof-action="create" ${canCreate ? "" : "disabled"}>Create immutable Proof</button></div>
    ${current.detail ? `<p class="idea-save-status">${escapeHtml(current.detail)}</p>` : ""}
  </div>`;
}

async function load() {
  if (!proofRoute()) return;
  const myGeneration = ++generation;
  const host = hostPanel();
  if (!host) return;
  const bookmarked = proofEntityFromLocation();
  state = { ...state, phase: "loading", proof: null, productStatus: null, detail: "" };
  render(host, state);
  try {
    if (bookmarked.present) {
      if (!bookmarked.entityId) throw new Error("Bookmarked Research Proof identity is invalid");
      const [proof, productStatus] = await Promise.all([
        fetchProof(bookmarked.entityId),
        fetchProductStatusOrUnavailable(),
      ]);
      if (myGeneration !== generation || !proofRoute()) return;
      state = { ...state, phase: "loaded", proof, productStatus, detail: "" };
      render(hostPanel(), state);
      return;
    }
    const [ideaCatalog, historical, robustnessCatalog, proofs] = await Promise.all([
      fetchIdeaCatalog(),
      fetchHistoricalResults(),
      fetchRobustnessCatalog(),
      fetchProofCatalog(),
    ]);
    if (myGeneration !== generation || !proofRoute()) return;
    state = {
      phase: "loaded",
      ideas: ideaCatalog.ideas,
      historical,
      robustness: robustnessCatalog.results,
      proofs,
      selectedIdeaIndex: 0,
      selectedHistoricalIndex: 0,
      selectedValidationIndex: 0,
      proof: null,
      productStatus: null,
      detail: "",
    };
    render(hostPanel(), state);
  } catch (error) {
    if (myGeneration !== generation || !proofRoute()) return;
    state = { ...state, phase: "failed", proof: null, productStatus: null, detail: error instanceof Error ? error.message : "Research Proof load failed" };
    render(hostPanel(), state);
  }
}

async function createSelectedProof() {
  if (state.phase !== "loaded" || state.proof) return;
  const myGeneration = generation;
  const selections = proofSelections(state.ideas, state.historical, state.robustness, state.selectedHistoricalIndex);
  const idea = selections.ideas[state.selectedIdeaIndex] || null;
  const validation = selections.validations[state.selectedValidationIndex] || null;
  if (!idea || !selections.historical || !validation) return;
  const host = hostPanel();
  state = { ...state, phase: "loading", detail: "Creating immutable Proof…" };
  render(host, state);
  try {
    const [proof, productStatus] = await Promise.all([
      createProof({ idea, historical: selections.historical, validation }),
      fetchProductStatusOrUnavailable(),
    ]);
    if (myGeneration !== generation || !proofRoute()) return;
    setProofEntityInLocation(proof.entity_id);
    state = { ...state, phase: "loaded", proof, productStatus, detail: "" };
    render(hostPanel(), state);
  } catch (error) {
    if (myGeneration !== generation || !proofRoute()) return;
    state = { ...state, phase: "loaded", proof: null, productStatus: null, detail: error instanceof Error ? error.message : "Research Proof creation failed" };
    render(hostPanel(), state);
  }
}

if (typeof document !== "undefined") {
  document.addEventListener("change", (event) => {
    if (!proofRoute()) return;
    const target = event.target;
    if (!(target instanceof HTMLSelectElement)) return;
    if (target.id === "proof-existing" && target.value) {
      setProofEntityInLocation(target.value);
      void load();
      return;
    }
    if (target.id === "proof-idea") state = { ...state, selectedIdeaIndex: Number(target.value) || 0 };
    if (target.id === "proof-historical") state = { ...state, selectedHistoricalIndex: Number(target.value) || 0, selectedValidationIndex: 0 };
    if (target.id === "proof-validation") state = { ...state, selectedValidationIndex: Number(target.value) || 0 };
    render(hostPanel(), state);
  });
  document.addEventListener("click", (event) => {
    if (!proofRoute()) return;
    const action = event.target.closest?.("[data-proof-action]")?.getAttribute("data-proof-action");
    if (!action) return;
    event.preventDefault();
    if (action === "create") void createSelectedProof();
    if (action === "new") {
      setProofEntityInLocation("");
      state = { ...state, proof: null, productStatus: null, phase: "idle", detail: "" };
      void load();
    }
  });
  const observer = new MutationObserver(() => {
    const host = hostPanel();
    if (proofRoute() && host && !host.querySelector("[data-research-proof-workspace]")) void load();
    if (!proofRoute()) generation += 1;
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void load();
}
