const MODELS_API_PATH = "/api/research/models";
const MODELS_SCHEMA = "tc.research-ml-model-catalog.v1";
const RESULTS_API_PATH = "/api/research/historical-results";

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function object(value) {
  return value && typeof value === "object" && !Array.isArray(value) ? value : null;
}

export function parseModelsCatalog(payload) {
  const record = object(payload);
  if (!record || record.schema !== MODELS_SCHEMA) throw new Error("models catalog schema mismatch");
  if (!Array.isArray(record.families) || !Array.isArray(record.models)) throw new Error("models catalog families are missing");
  return Object.freeze({
    backend_available: record.backend_available === true,
    reason_code: typeof record.reason_code === "string" ? record.reason_code : null,
    detail: typeof record.detail === "string" ? record.detail : "",
    feature_names: Array.isArray(record.feature_names) ? record.feature_names.map(String) : [],
    label_rule: typeof record.label_rule === "string" ? record.label_rule : "",
    families: record.families.map((item) => {
      const family = object(item);
      if (!family || typeof family.family_id !== "string" || !family.family_id.startsWith("sklearn.")) {
        throw new Error("models family identity is invalid");
      }
      return Object.freeze({
        family_id: family.family_id,
        label: typeof family.label === "string" ? family.label : family.family_id,
      });
    }),
    models: record.models.map((item) => {
      const model = object(item);
      if (!model || typeof model.artifact_sha256 !== "string" || !model.artifact_sha256) throw new Error("fitted model identity is missing");
      return Object.freeze({
        family_id: String(model.family_id || ""),
        label: String(model.label || model.family_id || "Model"),
        historical_result_entity_id: String(model.historical_result_entity_id || ""),
        historical_result_revision: String(model.historical_result_revision || ""),
        trade_count: Number(model.trade_count) || 0,
        artifact_sha256: model.artifact_sha256,
        train_accuracy: typeof model.train_accuracy === "number" ? model.train_accuracy : null,
      });
    }),
  });
}

export function renderModelsPanel(record, results = []) {
  if (!record) {
    return `<div data-ml-models-panel data-ml-models-state="pending"><div class="empty-state is-compact"><div><strong>Checking Models backend</strong><p>Waiting for the platform-owned Models catalog.</p></div></div></div>`;
  }
  const completed = results.filter((item) => item.state === "completed" && item.execution_completed === true);
  const options = completed.map((item) => `<option value="${escapeHtml(item.entity_id)}\t${escapeHtml(item.revision)}">${escapeHtml(item.entity_id)}</option>`).join("");
  const selector = completed.length
    ? `<label class="field"><span>Historical Result</span><select data-ml-result>${options}</select></label>`
    : `<p class="note">No completed Historical Result is in custody. Run a native Retester first. Fit uses exact entity/revision identity only.</p>`;
  const fitButtons = record.backend_available && completed.length
    ? record.families.map((family) => `<button type="button" class="button button-secondary button-small" data-ml-fit="${escapeHtml(family.family_id)}"><span>Fit ${escapeHtml(family.label)}</span></button>`).join("")
    : "";
  const models = record.models.length
    ? record.models.map((model) => `<div class="stat-row" data-ml-model="${escapeHtml(model.artifact_sha256)}"><div><strong>${escapeHtml(model.label)}</strong><p class="field-help">${escapeHtml(model.historical_result_entity_id)} · ${model.trade_count} native trades · sha256 ${escapeHtml(model.artifact_sha256.slice(0, 12))}</p></div><span>${model.train_accuracy == null ? "—" : `Train ${model.train_accuracy.toFixed(2)}`}</span></div>`).join("")
    : `<div class="empty-state is-compact"><div><strong>No fitted models</strong><p>Fit an allowlisted sklearn family on native trades from one completed Historical Result. SQX still owns backtest and robustness.</p></div></div>`;
  return `<div data-ml-models-panel data-ml-models-state="loaded" data-backend-available="${record.backend_available ? "true" : "false"}">
    <p class="note">${escapeHtml(record.detail)}</p>
    ${selector}
    <div class="row-tags">${fitButtons}</div>
    ${models}
  </div>`;
}

async function readCatalog(fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(MODELS_API_PATH, { headers: { accept: "application/json" } });
  if (!response?.ok) throw new Error(`Models catalog request failed: ${response?.status ?? "unknown"}`);
  return parseModelsCatalog(await response.json());
}

async function readResults(fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(RESULTS_API_PATH, { headers: { accept: "application/json" } });
  if (!response?.ok) return [];
  const payload = await response.json();
  return Array.isArray(payload?.results) ? payload.results.filter((item) => object(item)) : [];
}

function replacePanel(zone, html) {
  const existing = zone.querySelector("[data-ml-models-panel]");
  if (existing) existing.outerHTML = html;
  else zone.innerHTML = html;
}

async function refresh(zone, fetchImpl = globalThis.fetch) {
  replacePanel(zone, renderModelsPanel(null));
  try {
    const [record, results] = await Promise.all([readCatalog(fetchImpl), readResults(fetchImpl)]);
    if (zone.isConnected) replacePanel(zone, renderModelsPanel(record, results));
  } catch (error) {
    if (!zone.isConnected) return;
    const detail = error instanceof Error ? error.message : "Models catalog failed";
    replacePanel(zone, `<div data-ml-models-panel data-ml-models-state="error"><div class="empty-state is-compact tone-error"><div><strong>Models backend unavailable</strong><p>${escapeHtml(detail)}</p></div></div></div>`);
  }
}

async function onClick(event, zone) {
  const button = event.target.closest?.("[data-ml-fit]");
  if (!button) return;
  event.preventDefault();
  const select = zone.querySelector("[data-ml-result]");
  const selected = String(select?.value || "");
  const [entityId, revision] = selected.split("\t");
  if (!entityId || !revision) return;
  try {
    const response = await fetch(MODELS_API_PATH, {
      method: "POST",
      headers: { accept: "application/json", "content-type": "application/json" },
      body: JSON.stringify({
        action: "fit",
        family_id: button.getAttribute("data-ml-fit"),
        historical_result_entity_id: entityId,
        expected_historical_result_revision: revision,
      }),
    });
    const body = await response.json().catch(() => null);
    if (!response.ok) throw new Error(body?.detail || `Fit failed: ${response.status}`);
    if (zone.isConnected) {
      const results = await readResults();
      replacePanel(zone, renderModelsPanel(parseModelsCatalog(body), results));
    }
  } catch (error) {
    if (!zone.isConnected) return;
    const detail = error instanceof Error ? error.message : "Fit failed";
    replacePanel(zone, `<div data-ml-models-panel data-ml-models-state="error"><div class="empty-state is-compact tone-error"><div><strong>Fit failed</strong><p>${escapeHtml(detail)}</p></div></div></div>`);
  }
}

function mountModels(root = document) {
  const zone = root.querySelector?.("[data-ml-models]");
  if (!zone || zone.dataset.mlModelsBound === "true") return false;
  zone.dataset.mlModelsBound = "true";
  zone.addEventListener("click", (event) => {
    void onClick(event, zone);
  });
  void refresh(zone);
  return true;
}

if (typeof document !== "undefined") {
  const app = document.querySelector("#app");
  mountModels(document);
  if (app && typeof MutationObserver !== "undefined") {
    const observer = new MutationObserver(() => mountModels(document));
    observer.observe(app, { childList: true, subtree: true });
  }
}
