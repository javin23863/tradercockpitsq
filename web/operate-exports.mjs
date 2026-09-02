// Operate export custody after Promotion. Distinct from deployment and live runs.

import { chip } from "./ui.mjs";

const EXPORTS_API_PATH = "/api/operate/exports";
const EXPORT_SCHEMA = "tc.operate-export.v1";
const EXPORT_CATALOG_SCHEMA = "tc.operate-export-catalog.v1";

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function entity(value, kind) {
  const pattern = new RegExp(`^tc-research:${kind}:v1:[0-9a-f-]{36}$`);
  return typeof value === "string" && pattern.test(value) ? value : "";
}

function revision(value, kind) {
  const pattern = new RegExp(`^tc-research-revision:${kind}:sha256:[0-9a-f]{64}$`);
  return typeof value === "string" && pattern.test(value) ? value : "";
}

function short(value) {
  const text = String(value || "");
  return text.length > 28 ? `…${text.slice(-26)}` : text;
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

function catalogItem(item) {
  if (
    !item
    || !entity(item.entity_id, "export")
    || !revision(item.revision, "export")
    || !entity(item.promotion_entity_id, "promotion")
    || !entity(item.proof_entity_id, "proof")
    || !entity(item.candidate_entity_id, "candidate")
    || typeof item.candidate_archive_name !== "string"
    || !item.candidate_archive_name
  ) {
    throw new Error("Export catalog identity is invalid");
  }
  return Object.freeze({
    entity_id: item.entity_id,
    revision: item.revision,
    promotion_entity_id: item.promotion_entity_id,
    proof_entity_id: item.proof_entity_id,
    candidate_entity_id: item.candidate_entity_id,
    candidate_archive_name: item.candidate_archive_name,
  });
}

export function exportCatalogFromPayload(payload) {
  if (!payload || payload.schema !== EXPORT_CATALOG_SCHEMA || !Array.isArray(payload.exports)) {
    throw new Error("Export catalog schema mismatch");
  }
  const exports = payload.exports.map(catalogItem);
  const entities = new Set();
  for (const item of exports) {
    if (entities.has(item.entity_id)) throw new Error("Export catalog contains duplicate entity identity");
    entities.add(item.entity_id);
  }
  return Object.freeze({ schema: EXPORT_CATALOG_SCHEMA, exports: Object.freeze(exports) });
}

export async function fetchExportCatalog(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Export catalog fetch is unavailable");
  const response = await fetchImpl(EXPORTS_API_PATH, { headers: { accept: "application/json" } });
  if (!response?.ok) throw new Error(`Export catalog request failed: ${response?.status ?? "unknown"}`);
  return exportCatalogFromPayload(await response.json());
}

export async function exportPromotion(promotionEntityId, fetchImpl = globalThis.fetch) {
  const promotion_entity_id = entity(promotionEntityId, "promotion");
  if (!promotion_entity_id) throw new Error("Exact Promotion entity identity is required");
  const response = await fetchImpl(EXPORTS_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({ action: "export", promotion_entity_id }),
  });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Export failed");
  if (!payload || payload.schema !== EXPORT_SCHEMA) throw new Error("Export schema mismatch");
  return payload;
}

export function renderOperateExports(catalog, errorDetail = "") {
  if (errorDetail) {
    return `<div data-operate-exports data-operate-exports-state="unavailable"><div class="empty-state is-compact tone-error"><div class="empty-icon">—</div><div><strong>Export catalog read failed</strong><p>${escapeHtml(errorDetail)}</p></div></div></div>`;
  }
  const exports = catalog?.exports || [];
  if (!exports.length) {
    return `<div data-operate-exports data-operate-exports-state="empty"><p class="note">No exported Delivery custody yet. Export requires an existing Promotion; it is not broker/MT4 export, live execution, or deployment.</p></div>`;
  }
  const rows = exports.map((item) => `<div class="list-row"><span class="row-title"><strong>${escapeHtml(item.candidate_archive_name)}</strong><span>Promotion ${escapeHtml(short(item.promotion_entity_id))} · Proof ${escapeHtml(short(item.proof_entity_id))}</span></span><span class="row-actions">${chip("Exported", "ready")}<button type="button" class="action-link" data-deploy-export="${escapeHtml(item.entity_id)}" title="Bind deployment custody for this Export. Not live execution, broker send, or P&amp;L.">Deploy</button></span></div>`).join("");
  return `<div data-operate-exports data-operate-exports-state="loaded">${rows}<p class="note">Export custody records promoted identities only. No strategy bytes are written outside custody and no broker send is claimed.</p></div>`;
}

function replaceHost(host, html) {
  const current = host.querySelector("[data-operate-exports]") || host;
  if (current === host && host.dataset.operateExports !== undefined) {
    host.innerHTML = html;
    return;
  }
  if (current && current !== host) current.outerHTML = html;
  else host.innerHTML = html;
}

async function bindOperateExports(host) {
  replaceHost(host, `<div data-operate-exports data-operate-exports-state="pending"><p class="note">Reading export custody…</p></div>`);
  try {
    const catalog = await fetchExportCatalog();
    if (host.isConnected) replaceHost(host, renderOperateExports(catalog));
  } catch (error) {
    if (!host.isConnected) return;
    const detail = error instanceof Error ? error.message : "Export catalog read failed";
    replaceHost(host, renderOperateExports(null, detail));
  }
}

async function onExportClick(button) {
  const promotionEntityId = button.getAttribute("data-export-promotion") || "";
  button.disabled = true;
  try {
    await exportPromotion(promotionEntityId);
    const exportsHost = document.querySelector("[data-operate-exports-host]");
    if (exportsHost) await bindOperateExports(exportsHost);
    const { refreshHomeAlphaStack } = await import("./home-alpha-stack.mjs");
    await refreshHomeAlphaStack();
    button.disabled = false;
  } catch (error) {
    button.disabled = false;
    button.title = error instanceof Error ? error.message : "Export failed";
  }
}

function mountOperateExports(root = document) {
  const host = root.querySelector?.("[data-operate-exports-host]");
  if (host && host.dataset.operateExportsBound !== "true") {
    host.dataset.operateExportsBound = "true";
    void bindOperateExports(host);
  }
  return Boolean(host);
}

if (typeof document !== "undefined") {
  const app = document.querySelector("#app");
  mountOperateExports(document);
  document.addEventListener("click", (event) => {
    const button = event.target?.closest?.("[data-export-promotion]");
    if (!button) return;
    event.preventDefault();
    void onExportClick(button);
  });
  if (app && typeof MutationObserver !== "undefined") {
    const observer = new MutationObserver(() => mountOperateExports(document));
    observer.observe(app, { childList: true, subtree: true });
  }
}
