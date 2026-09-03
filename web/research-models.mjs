import { candidateCatalogFromPayload } from "./research-candidates.mjs";

const MODELS_API_PATH = "/api/research/models";
const MODELS_SCHEMA = "tc.research-ml-model-catalog.v1";
const RESULTS_API_PATH = "/api/research/historical-results";
const CANDIDATES_API_PATH = "/api/research/candidates";

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

function metricRecord(value, numericKey) {
  const record = object(value);
  if (!record) {
    return Object.freeze({
      status: "unavailable",
      reason_code: null,
      value: typeof value === "number" ? value : null,
      n: null,
      p_win: null,
      avg_win: null,
      avg_loss: null,
      mean_return: null,
      stdev_return: null,
    });
  }
  const numeric = record[numericKey];
  return Object.freeze({
    status: typeof record.status === "string" ? record.status : (typeof numeric === "number" ? "available" : "unavailable"),
    reason_code: typeof record.reason_code === "string" ? record.reason_code : null,
    value: typeof numeric === "number" ? numeric : null,
    n: typeof record.n === "number" ? record.n : null,
    p_win: typeof record.p_win === "number" ? record.p_win : null,
    avg_win: typeof record.avg_win === "number" ? record.avg_win : null,
    avg_loss: typeof record.avg_loss === "number" ? record.avg_loss : null,
    mean_return: typeof record.mean_return === "number" ? record.mean_return : null,
    stdev_return: typeof record.stdev_return === "number" ? record.stdev_return : null,
  });
}

function formatScore(value, label, unavailable = `${label} unavailable`) {
  return value == null ? unavailable : `${label} ${value.toFixed(2)}`;
}

function formatMetric(record, label) {
  if (record.value == null) {
    const reason = record.reason_code ? ` (${escapeHtml(record.reason_code)})` : "";
    return `${label} unavailable${reason}`;
  }
  return `${label} ${record.value.toFixed(2)}`;
}

function boundCandidate(model, candidates) {
  return candidates.find((item) => item.ml_model_artifact_sha256 === model.artifact_sha256) || null;
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
        enabled: family.enabled !== false,
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
        oos_accuracy: typeof model.oos_accuracy === "number" ? model.oos_accuracy : null,
        expected_value: metricRecord(model.expected_value, "expected_value"),
        sharpe: metricRecord(model.sharpe, "sharpe"),
      });
    }),
  });
}

export function renderModelsPanel(record, results = [], candidates = []) {
  if (!record) {
    return `<div data-ml-models-panel data-ml-models-state="pending"><div class="empty-state is-compact"><div><strong>Checking Models backend</strong><p>Waiting for the platform-owned Models catalog.</p></div></div></div>`;
  }
  const completed = results.filter((item) => item.state === "completed" && item.execution_completed === true);
  const options = completed.map((item) => `<option value="${escapeHtml(item.entity_id)}\t${escapeHtml(item.revision)}">${escapeHtml(item.entity_id)}</option>`).join("");
  const selector = completed.length
    ? `<label class="field-label" for="ml-result">Historical Result</label><select id="ml-result" class="idea-editor" data-ml-result>${options}</select>`
    : `<p class="note">No completed Historical Result is in custody. Run a native Retester first. Fit uses exact entity/revision identity only.</p>`;
  const enabledFamilies = record.families.filter((family) => family.enabled);
  const fitButtons = record.backend_available && completed.length
    ? enabledFamilies.map((family) => `<button type="button" class="button button-secondary button-small" data-ml-fit="${escapeHtml(family.family_id)}"><span>Fit ${escapeHtml(family.label)}</span></button>`).join("")
    : "";
  const candidateOptions = candidates.map((item) => `<option value="${escapeHtml(item.entity_id)}\t${escapeHtml(item.revision)}">${escapeHtml(item.archive_name)} · ${escapeHtml(item.entity_id)}</option>`).join("");
  const candidateSelector = candidates.length
    ? `<label class="field-label" for="ml-candidate">Native Candidate</label><select id="ml-candidate" class="idea-editor" data-ml-candidate>${candidateOptions}</select>`
    : `<p class="note">No imported native Candidate is in custody. Import a survivor before binding a fitted model.</p>`;
  const models = record.models.length
    ? record.models.map((model) => {
      const bound = boundCandidate(model, candidates);
      const evParts = model.expected_value.value == null
        ? ""
        : ` · p_win ${model.expected_value.p_win?.toFixed(2) ?? "—"} · avg_win ${model.expected_value.avg_win?.toFixed(2) ?? "—"} · avg_loss ${model.expected_value.avg_loss?.toFixed(2) ?? "—"}`;
      const sharpeParts = model.sharpe.value == null
        ? ""
        : ` · mean ${model.sharpe.mean_return?.toFixed(2) ?? "—"} · stdev ${model.sharpe.stdev_return?.toFixed(2) ?? "—"} · n ${model.sharpe.n ?? model.trade_count}`;
      const bindButton = candidates.length && !bound
        ? `<button type="button" class="button button-secondary button-small" data-ml-bind="${escapeHtml(model.artifact_sha256)}"><span>Bind to Candidate</span></button>`
        : "";
      return `<div class="stat-row" data-ml-model="${escapeHtml(model.artifact_sha256)}"><div><strong>${escapeHtml(model.label)}</strong><p class="field-help">${escapeHtml(model.historical_result_entity_id)} · ${model.trade_count} native trades · sha256 ${escapeHtml(model.artifact_sha256.slice(0, 12))}${bound ? ` · bound ${escapeHtml(bound.archive_name)}` : ""}</p><p class="field-help">${formatMetric(model.expected_value, "EV")}${evParts}</p><p class="field-help">${formatMetric(model.sharpe, "Sharpe")}${sharpeParts}</p></div><span>${formatScore(model.train_accuracy, "Train")} · ${formatScore(model.oos_accuracy, "OOS", "OOS unavailable")}</span>${bindButton}</div>`;
    }).join("")
    : `<div class="empty-state is-compact"><div><strong>No fitted models</strong><p>Fit an allowlisted sklearn family on native trades from one completed Historical Result. SQX still owns backtest and robustness. Expected value and Sharpe stay visible on every fitted row.</p></div></div>`;
  return `<div data-ml-models-panel data-ml-models-state="loaded" data-backend-available="${record.backend_available ? "true" : "false"}">
    <p class="note">${escapeHtml(record.detail)}</p>
    ${selector}
    <div class="row-tags">${fitButtons}</div>
    ${candidateSelector}
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

async function readCandidates(fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(CANDIDATES_API_PATH, { headers: { accept: "application/json" } });
  if (!response?.ok) return [];
  try {
    return candidateCatalogFromPayload(await response.json());
  } catch {
    return [];
  }
}

async function panelInputs(fetchImpl = globalThis.fetch) {
  const [results, candidates] = await Promise.all([readResults(fetchImpl), readCandidates(fetchImpl)]);
  return { results, candidates };
}

function replacePanel(zone, html) {
  const existing = zone.querySelector("[data-ml-models-panel]");
  if (existing) existing.outerHTML = html;
  else zone.innerHTML = html;
}

async function refresh(zone, fetchImpl = globalThis.fetch) {
  replacePanel(zone, renderModelsPanel(null));
  try {
    const [record, inputs] = await Promise.all([readCatalog(fetchImpl), panelInputs(fetchImpl)]);
    if (zone.isConnected) replacePanel(zone, renderModelsPanel(record, inputs.results, inputs.candidates));
  } catch (error) {
    if (!zone.isConnected) return;
    const detail = error instanceof Error ? error.message : "Models catalog failed";
    replacePanel(zone, `<div data-ml-models-panel data-ml-models-state="error"><div class="empty-state is-compact tone-error"><div><strong>Models backend unavailable</strong><p>${escapeHtml(detail)}</p></div></div></div>`);
  }
}

async function onClick(event, zone) {
  const bind = event.target.closest?.("[data-ml-bind]");
  const fit = event.target.closest?.("[data-ml-fit]");
  if (!bind && !fit) return;
  event.preventDefault();
  try {
    if (bind) {
      const select = zone.querySelector("[data-ml-candidate]");
      const selected = String(select?.value || "");
      const [entityId, revision] = selected.split("\t");
      if (!entityId || !revision) return;
      const response = await fetch(CANDIDATES_API_PATH, {
        method: "POST",
        headers: { accept: "application/json", "content-type": "application/json" },
        body: JSON.stringify({
          action: "bind-ml-model",
          candidate_entity_id: entityId,
          expected_candidate_revision: revision,
          artifact_sha256: bind.getAttribute("data-ml-bind"),
        }),
      });
      const body = await response.json().catch(() => null);
      if (!response.ok) throw new Error(body?.detail || `Bind failed: ${response.status}`);
      if (zone.isConnected) {
        const [record, inputs] = await Promise.all([readCatalog(), panelInputs()]);
        replacePanel(zone, renderModelsPanel(record, inputs.results, inputs.candidates));
      }
      return;
    }
    const select = zone.querySelector("[data-ml-result]");
    const selected = String(select?.value || "");
    const [entityId, revision] = selected.split("\t");
    if (!entityId || !revision) return;
    const response = await fetch(MODELS_API_PATH, {
      method: "POST",
      headers: { accept: "application/json", "content-type": "application/json" },
      body: JSON.stringify({
        action: "fit",
        family_id: fit.getAttribute("data-ml-fit"),
        historical_result_entity_id: entityId,
        expected_historical_result_revision: revision,
      }),
    });
    const body = await response.json().catch(() => null);
    if (!response.ok) throw new Error(body?.detail || `Fit failed: ${response.status}`);
    if (zone.isConnected) {
      const inputs = await panelInputs();
      replacePanel(zone, renderModelsPanel(parseModelsCatalog(body), inputs.results, inputs.candidates));
    }
  } catch (error) {
    if (!zone.isConnected) return;
    const detail = error instanceof Error ? error.message : "Models write failed";
    replacePanel(zone, `<div data-ml-models-panel data-ml-models-state="error"><div class="empty-state is-compact tone-error"><div><strong>Models write failed</strong><p>${escapeHtml(detail)}</p></div></div></div>`);
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
