import { icon } from "./ui.mjs";

const RUNTIME_STATUS_API_PATH = "/api/status";
const RUNTIME_STATUS_SCHEMA = "tc.runtime-status.v1";

const COMPONENTS = Object.freeze([
  ["application", "TraderCockpit application"],
  ["research_backend", "Research backend"],
  ["research_custody", "Research custody"],
  ["native_execution", "Native execution"],
  ["market_data", "Live market data"],
  ["tradingview", "Apollo TradingView tool"],
  ["metatrader", "Apollo MetaTrader tool"],
  ["provider", "Model provider"],
  ["account", "Consumer account"],
  ["model", "Model access"],
  ["extensions", "Extensions"],
]);

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

function readableCode(value) {
  if (!value) return "";
  return String(value).replaceAll("_", " ").replaceAll("-", " ").replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function stateLabel(record) {
  const status = record?.status;
  const labels = {
    ready: "Ready",
    current: "Current",
    stale: "Stale",
    pending: "Pending",
    unavailable: "Unavailable",
    invalid: "Invalid",
    error: "Error",
  };
  return labels[status] || "Unavailable";
}

function componentRecord(payload, key) {
  if (key === "native_execution") {
    const execution = object(payload.research_backend)?.execution;
    if (!object(execution)) return { status: "unavailable", reason_code: "execution_state_missing" };
    const detail = typeof execution.detail === "string" && execution.detail ? execution.detail : null;
    return execution.available === true
      ? { status: "ready", reason_code: null, detail }
      : { status: "unavailable", reason_code: execution.reason_code || "execution_unavailable", detail };
  }
  if (key === "tradingview" || key === "metatrader") {
    const live = object(payload.live_producers);
    const record = object(live?.[key]);
    return record || { status: "unavailable", reason_code: "mcp_url_not_configured" };
  }
  return object(payload[key]);
}

function nativeRecoveryNotes(records) {
  const notes = [];
  for (const key of ["research_backend", "native_execution"]) {
    const record = records[key];
    if (!record || record.status === "ready" || record.status === "current") continue;
    if (typeof record.detail !== "string" || !record.detail) continue;
    if (!notes.includes(record.detail)) notes.push(record.detail);
  }
  return notes;
}

function validateComponent(record, key) {
  if (!record) throw new Error(`System Status component ${key} is missing`);
  const allowed = new Set(["ready", "current", "stale", "pending", "unavailable", "invalid", "error"]);
  if (!allowed.has(record.status)) throw new Error(`System Status component ${key} has invalid status`);
  if (record.reason_code !== null && record.reason_code !== undefined && (typeof record.reason_code !== "string" || !record.reason_code)) {
    throw new Error(`System Status component ${key} has invalid reason code`);
  }
}

export function parseHomeSystemStatus(payload) {
  const runtime = object(payload);
  if (!runtime || runtime.schema !== RUNTIME_STATUS_SCHEMA) throw new Error("runtime status schema mismatch");

  const parsed = {};
  for (const [key] of COMPONENTS) {
    const record = componentRecord(runtime, key);
    validateComponent(record, key);
    parsed[key] = record;
  }

  const research = parsed.research_backend;
  if (research.status === "ready") {
    if (research.verified !== true || typeof research.build !== "string" || !research.build) {
      throw new Error("ready Research backend lacks verified build identity");
    }
  }

  const market = parsed.market_data;
  if (["current", "stale"].includes(market.status)) {
    if (market.scope !== "live_current" || market.historical_fallback !== false) {
      throw new Error("live market status scope is invalid");
    }
  }

  return Object.freeze(parsed);
}

export async function fetchHomeSystemStatus(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("System Status fetch is unavailable");
  const response = await fetchImpl(RUNTIME_STATUS_API_PATH, { headers: { accept: "application/json" } });
  if (!response?.ok) throw new Error(`System Status request failed: ${response?.status ?? "unknown"}`);
  return parseHomeSystemStatus(await response.json());
}

function displayValue(record, key) {
  if (key === "research_backend" && record.status === "ready") {
    return `Ready · StrategyQuant X ${record.build}`;
  }
  if (key === "native_execution" && record.status === "unavailable") {
    return record.reason_code ? `Disabled · ${readableCode(record.reason_code)}` : "Disabled";
  }
  const label = stateLabel(record);
  return record.reason_code ? `${label} · ${readableCode(record.reason_code)}` : label;
}

const COMPONENT_ICONS = Object.freeze({
  application: "operate",
  research_backend: "research",
  research_custody: "layers",
  native_execution: "play",
  market_data: "activity",
  tradingview: "chart",
  metatrader: "operate",
  provider: "bot",
  account: "crown",
  model: "bot",
  extensions: "grid",
});

export function renderHomeSystemStatus(records) {
  if (!records) {
    return `<div data-home-system-status data-system-status="pending"><div class="empty-state is-compact"><div class="empty-icon">${icon("activity", { size: 14 })}</div><div><strong>Checking runtime status</strong><p>Waiting for the canonical backend status read model.</p></div></div></div>`;
  }

  const rows = COMPONENTS.map(([key, label]) => {
    const record = records[key];
    const healthy = record.status === "ready" || record.status === "current";
    return `<div class="health-row stat-row" data-runtime-component="${escapeHtml(key.replaceAll("_", "-"))}" data-runtime-state="${escapeHtml(record.status)}">${icon(COMPONENT_ICONS[key] || "dots", { size: 14 })}<strong>${escapeHtml(label)}</strong><span class="health-value">${escapeHtml(displayValue(record, key))}</span><span class="health-mark">${icon(healthy ? "check" : "warn", { size: 14 })}</span></div>`;
  }).join("");
  const notes = nativeRecoveryNotes(records)
    .map((text) => `<p class="note" data-runtime-recovery>${escapeHtml(text)}</p>`)
    .join("");
  return `<div class="health-list" data-home-system-status data-system-status="loaded">${rows}${notes}</div>`;
}

function replaceSystemBody(zone, html) {
  const existing = zone.querySelector("[data-home-system-status]");
  if (existing) {
    existing.outerHTML = html;
    return true;
  }
  for (const row of [...zone.querySelectorAll(":scope > .stat-row")]) row.remove();
  zone.insertAdjacentHTML("beforeend", html);
  return true;
}

function setHomeRuntimeStatus(zone, status) {
  const shell = zone.closest?.('[data-product-shell][data-surface-id="home"]');
  if (shell) shell.setAttribute("data-runtime-status", status);
}

async function bindSystemStatus(zone) {
  setHomeRuntimeStatus(zone, "loading");
  replaceSystemBody(zone, renderHomeSystemStatus(null));
  try {
    const records = await fetchHomeSystemStatus();
    if (zone.isConnected) {
      replaceSystemBody(zone, renderHomeSystemStatus(records));
      setHomeRuntimeStatus(zone, "loaded");
    }
  } catch (error) {
    if (!zone.isConnected) return;
    const detail = error instanceof Error ? error.message : "System Status read failed";
    replaceSystemBody(
      zone,
      `<div data-home-system-status data-system-status="error"><div class="empty-state is-compact tone-error"><div class="empty-icon">${icon("warn", { size: 14 })}</div><div><strong>Runtime status unavailable</strong><p>${escapeHtml(detail)}</p></div></div></div>`,
    );
    setHomeRuntimeStatus(zone, "failed");
  }
}

function mountHomeSystemStatus(root = document) {
  const zone = root.querySelector?.('[data-home-zone="system-status"]');
  if (!zone || zone.dataset.systemStatusBound === "true") return false;
  zone.dataset.systemStatusBound = "true";
  void bindSystemStatus(zone);
  return true;
}

if (typeof document !== "undefined") {
  const app = document.querySelector("#app");
  mountHomeSystemStatus(document);
  if (app && typeof MutationObserver !== "undefined") {
    const observer = new MutationObserver(() => mountHomeSystemStatus(document));
    observer.observe(app, { childList: true, subtree: true });
  }
}
