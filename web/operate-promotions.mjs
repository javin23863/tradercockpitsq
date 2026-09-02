// Operate promotion custody after Proof. Distinct from export, deployment, and live runs.

import { chip } from "./ui.mjs";

const PROMOTIONS_API_PATH = "/api/operate/promotions";
const PROMOTION_SCHEMA = "tc.operate-promotion.v1";
const PROMOTION_CATALOG_SCHEMA = "tc.operate-promotion-catalog.v1";

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
    || !entity(item.entity_id, "promotion")
    || !revision(item.revision, "promotion")
    || !entity(item.proof_entity_id, "proof")
    || !revision(item.proof_revision, "proof")
    || !entity(item.candidate_entity_id, "candidate")
    || !revision(item.candidate_revision, "candidate")
    || !entity(item.historical_result_entity_id, "historical-result")
    || typeof item.candidate_archive_name !== "string"
    || !item.candidate_archive_name
  ) {
    throw new Error("Promotion catalog identity is invalid");
  }
  return Object.freeze({
    entity_id: item.entity_id,
    revision: item.revision,
    proof_entity_id: item.proof_entity_id,
    proof_revision: item.proof_revision,
    candidate_entity_id: item.candidate_entity_id,
    candidate_revision: item.candidate_revision,
    candidate_archive_name: item.candidate_archive_name,
    historical_result_entity_id: item.historical_result_entity_id,
  });
}

export function promotionCatalogFromPayload(payload) {
  if (!payload || payload.schema !== PROMOTION_CATALOG_SCHEMA || !Array.isArray(payload.promotions)) {
    throw new Error("Promotion catalog schema mismatch");
  }
  const promotions = payload.promotions.map(catalogItem);
  const entities = new Set();
  for (const item of promotions) {
    if (entities.has(item.entity_id)) throw new Error("Promotion catalog contains duplicate entity identity");
    entities.add(item.entity_id);
  }
  return Object.freeze({ schema: PROMOTION_CATALOG_SCHEMA, promotions: Object.freeze(promotions) });
}

export async function fetchPromotionCatalog(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Promotion catalog fetch is unavailable");
  const response = await fetchImpl(PROMOTIONS_API_PATH, { headers: { accept: "application/json" } });
  if (!response?.ok) throw new Error(`Promotion catalog request failed: ${response?.status ?? "unknown"}`);
  return promotionCatalogFromPayload(await response.json());
}

export async function promoteProof(proofEntityId, fetchImpl = globalThis.fetch) {
  const proof_entity_id = entity(proofEntityId, "proof");
  if (!proof_entity_id) throw new Error("Exact Proof entity identity is required");
  const response = await fetchImpl(PROMOTIONS_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({ action: "promote", proof_entity_id }),
  });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Promotion failed");
  if (!payload || payload.schema !== PROMOTION_SCHEMA) throw new Error("Promotion schema mismatch");
  return payload;
}

export function renderOperatePromotions(catalog, errorDetail = "") {
  if (errorDetail) {
    return `<div data-operate-promotions data-operate-promotions-state="unavailable"><div class="empty-state is-compact tone-error"><div class="empty-icon">—</div><div><strong>Promotion catalog read failed</strong><p>${escapeHtml(errorDetail)}</p></div></div></div>`;
  }
  const promotions = catalog?.promotions || [];
  if (!promotions.length) {
    return `<div data-operate-promotions data-operate-promotions-state="empty"><p class="note">No promoted Research strategies. Promotion binds an immutable Proof; it is not a live run, export, or deployment.</p></div>`;
  }
  const rows = promotions.map((item) => `<div class="list-row"><span class="row-title"><strong>${escapeHtml(item.candidate_archive_name)}</strong><span>Proof ${escapeHtml(short(item.proof_entity_id))} · Candidate ${escapeHtml(short(item.candidate_entity_id))}</span></span><span class="row-actions">${chip("Promoted", "ready")}<button type="button" class="action-link" data-export-promotion="${escapeHtml(item.entity_id)}" title="Bind Delivery export custody for this Promotion. Not broker/MT4 export or deployment.">Export</button></span></div>`).join("");
  return `<div data-operate-promotions data-operate-promotions-state="loaded">${rows}<p class="note">Promoted identities remain Research/Delivery custody. Live runs, positions, and P&amp;L stay unconnected until an execution producer exists.</p></div>`;
}

function replaceHost(host, html) {
  const current = host.querySelector("[data-operate-promotions]") || host;
  if (current === host && host.dataset.operatePromotions !== undefined) {
    host.innerHTML = html;
    return;
  }
  if (current && current !== host) current.outerHTML = html;
  else host.innerHTML = html;
}

async function bindOperatePromotions(host) {
  replaceHost(host, `<div data-operate-promotions data-operate-promotions-state="pending"><p class="note">Reading promotion custody…</p></div>`);
  try {
    const catalog = await fetchPromotionCatalog();
    if (host.isConnected) replaceHost(host, renderOperatePromotions(catalog));
  } catch (error) {
    if (!host.isConnected) return;
    const detail = error instanceof Error ? error.message : "Promotion catalog read failed";
    replaceHost(host, renderOperatePromotions(null, detail));
  }
}

async function onPromoteClick(button) {
  const proofEntityId = button.getAttribute("data-promote-proof") || "";
  button.disabled = true;
  try {
    await promoteProof(proofEntityId);
    const operateLink = document.querySelector('.primary-nav a[href="/operate"]');
    if (operateLink) operateLink.click();
    else {
      const operateHost = document.querySelector("[data-operate-promotions-host]");
      if (operateHost) await bindOperatePromotions(operateHost);
      button.disabled = false;
    }
  } catch (error) {
    button.disabled = false;
    button.title = error instanceof Error ? error.message : "Promotion failed";
  }
}

function mountOperatePromotions(root = document) {
  const host = root.querySelector?.("[data-operate-promotions-host]");
  if (host && host.dataset.operatePromotionsBound !== "true") {
    host.dataset.operatePromotionsBound = "true";
    void bindOperatePromotions(host);
  }
  return Boolean(host);
}

if (typeof document !== "undefined") {
  const app = document.querySelector("#app");
  mountOperatePromotions(document);
  document.addEventListener("click", (event) => {
    const button = event.target?.closest?.("[data-promote-proof]");
    if (!button) return;
    event.preventDefault();
    void onPromoteClick(button);
  });
  if (app && typeof MutationObserver !== "undefined") {
    const observer = new MutationObserver(() => mountOperatePromotions(document));
    observer.observe(app, { childList: true, subtree: true });
  }
}
