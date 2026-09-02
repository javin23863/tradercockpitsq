// Operate deployment custody after Export. Distinct from live execution, positions, and P&L.

import { chip, table } from "./ui.mjs";

const DEPLOYMENTS_API_PATH = "/api/operate/deployments";
const DEPLOYMENT_SCHEMA = "tc.operate-deployment.v1";
const DEPLOYMENT_CATALOG_SCHEMA = "tc.operate-deployment-catalog.v1";

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

function readable(code, fallback = "Unavailable") {
  if (!code) return fallback;
  return String(code).replaceAll("_", " ").replace(/\b\w/g, (char) => char.toUpperCase());
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
    || !entity(item.entity_id, "deployment")
    || !revision(item.revision, "deployment")
    || !entity(item.export_entity_id, "export")
    || !entity(item.candidate_entity_id, "candidate")
    || typeof item.candidate_archive_name !== "string"
    || !item.candidate_archive_name
    || item.mode !== "identity_only"
    || item.status !== "execution_not_connected"
  ) {
    throw new Error("Deployment catalog identity is invalid");
  }
  return Object.freeze({
    entity_id: item.entity_id,
    revision: item.revision,
    export_entity_id: item.export_entity_id,
    candidate_entity_id: item.candidate_entity_id,
    candidate_archive_name: item.candidate_archive_name,
    mode: item.mode,
    status: item.status,
  });
}

export function deploymentCatalogFromPayload(payload) {
  if (!payload || payload.schema !== DEPLOYMENT_CATALOG_SCHEMA || !Array.isArray(payload.deployments)) {
    throw new Error("Deployment catalog schema mismatch");
  }
  const deployments = payload.deployments.map(catalogItem);
  const entities = new Set();
  for (const item of deployments) {
    if (entities.has(item.entity_id)) throw new Error("Deployment catalog contains duplicate entity identity");
    entities.add(item.entity_id);
  }
  return Object.freeze({ schema: DEPLOYMENT_CATALOG_SCHEMA, deployments: Object.freeze(deployments) });
}

export async function fetchDeploymentCatalog(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Deployment catalog fetch is unavailable");
  const response = await fetchImpl(DEPLOYMENTS_API_PATH, { headers: { accept: "application/json" } });
  if (!response?.ok) throw new Error(`Deployment catalog request failed: ${response?.status ?? "unknown"}`);
  return deploymentCatalogFromPayload(await response.json());
}

export async function deployExport(exportEntityId, fetchImpl = globalThis.fetch) {
  const export_entity_id = entity(exportEntityId, "export");
  if (!export_entity_id) throw new Error("Exact Export entity identity is required");
  const response = await fetchImpl(DEPLOYMENTS_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({ action: "deploy", export_entity_id }),
  });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Deployment failed");
  if (!payload || payload.schema !== DEPLOYMENT_SCHEMA) throw new Error("Deployment schema mismatch");
  return payload;
}

export function renderOperateLiveRuns(catalog, liveDeployment, errorDetail = "") {
  if (errorDetail) {
    return `<div data-operate-live-runs data-operate-live-runs-state="unavailable"><div class="empty-state is-compact tone-error"><div class="empty-icon">—</div><div><strong>Deployment catalog read failed</strong><p>${escapeHtml(errorDetail)}</p></div></div></div>`;
  }
  const deployments = catalog?.deployments || [];
  if (!deployments.length) {
    const empty = "No deployment custody yet. Deployment requires an existing Export and does not claim live execution, fills, positions, or P&L.";
    const note = liveDeployment?.detail || empty;
    return `<div data-operate-live-runs data-operate-live-runs-state="empty">${table({ columns: [{ label: "Strategy" }, { label: "Mode" }, { label: "Account" }, { label: "Status" }], rows: [], empty })}<p class="note">${escapeHtml(note)}</p></div>`;
  }
  const rows = deployments.map((item) => ({
    cells: [
      escapeHtml(item.candidate_archive_name),
      escapeHtml(readable(item.mode, "Identity only")),
      "—",
      escapeHtml(readable(item.status, "Execution not connected")),
    ],
  }));
  return `<div data-operate-live-runs data-operate-live-runs-state="loaded">${table({ columns: [{ label: "Strategy" }, { label: "Mode" }, { label: "Account" }, { label: "Status" }], rows })}<p class="note">Deployment custody rows bind exported identities only. ${escapeHtml(liveDeployment?.detail || "No broker connection, fills, positions, or P&L are claimed.")}</p></div>`;
}

function replaceHost(host, html) {
  const current = host.querySelector("[data-operate-live-runs]") || host;
  if (current === host && host.dataset.operateLiveRuns !== undefined) {
    host.innerHTML = html;
    return;
  }
  if (current && current !== host) current.outerHTML = html;
  else host.innerHTML = html;
}

async function bindOperateLiveRuns(host) {
  replaceHost(host, `<div data-operate-live-runs data-operate-live-runs-state="pending"><p class="note">Reading deployment custody…</p></div>`);
  let liveDeployment = null;
  try {
    const statusResponse = await fetch("/api/status", { headers: { accept: "application/json" } });
    if (statusResponse?.ok) {
      const status = await statusResponse.json();
      liveDeployment = status?.live_deployment || null;
    }
  } catch {
    liveDeployment = null;
  }
  try {
    const catalog = await fetchDeploymentCatalog();
    if (host.isConnected) replaceHost(host, renderOperateLiveRuns(catalog, liveDeployment));
  } catch (error) {
    if (!host.isConnected) return;
    const detail = error instanceof Error ? error.message : "Deployment catalog read failed";
    replaceHost(host, renderOperateLiveRuns(null, liveDeployment, detail));
  }
}

async function onDeployClick(button) {
  const exportEntityId = button.getAttribute("data-deploy-export") || "";
  button.disabled = true;
  try {
    await deployExport(exportEntityId);
    const liveRunsHost = document.querySelector("[data-operate-live-runs-host]");
    if (liveRunsHost) await bindOperateLiveRuns(liveRunsHost);
    const { refreshHomeAlphaStack } = await import("./home-alpha-stack.mjs");
    await refreshHomeAlphaStack();
    button.disabled = false;
  } catch (error) {
    button.disabled = false;
    button.title = error instanceof Error ? error.message : "Deployment failed";
  }
}

function mountOperateLiveRuns(root = document) {
  const host = root.querySelector?.("[data-operate-live-runs-host]");
  if (host && host.dataset.operateLiveRunsBound !== "true") {
    host.dataset.operateLiveRunsBound = "true";
    void bindOperateLiveRuns(host);
  }
  return Boolean(host);
}

if (typeof document !== "undefined") {
  const app = document.querySelector("#app");
  mountOperateLiveRuns(document);
  document.addEventListener("click", (event) => {
    const button = event.target?.closest?.("[data-deploy-export]");
    if (!button) return;
    event.preventDefault();
    void onDeployClick(button);
  });
  if (app && typeof MutationObserver !== "undefined") {
    const observer = new MutationObserver(() => mountOperateLiveRuns(document));
    observer.observe(app, { childList: true, subtree: true });
  }
}

export { chip, readable };
