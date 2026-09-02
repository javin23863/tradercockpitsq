const RUNTIME_STATUS_API_PATH = "/api/status";
const RUNTIME_STATUS_SCHEMA = "tc.runtime-status.v1";
export const HOME_MARKET_OVERVIEW_SCHEMA = "tc.home-market-overview.v1";

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

function nonEmptyString(value) {
  return typeof value === "string" && value.trim() ? value : null;
}

function nullableString(value) {
  return value === null ? null : nonEmptyString(value);
}

export function parseHomeMarketOverview(runtimePayload) {
  const runtime = object(runtimePayload);
  if (!runtime || runtime.schema !== RUNTIME_STATUS_SCHEMA) throw new Error("runtime status schema mismatch");
  const market = object(runtime.market_data);
  if (!market || market.schema !== HOME_MARKET_OVERVIEW_SCHEMA) throw new Error("Market Overview schema mismatch");
  if (market.scope !== "live_current" || market.historical_fallback !== false) {
    throw new Error("Market Overview scope mismatch");
  }
  if (!["current", "stale", "unavailable", "error"].includes(market.status)) {
    throw new Error("Market Overview status is invalid");
  }
  const freshness = object(market.freshness);
  if (!freshness || freshness.state !== market.status) throw new Error("Market Overview freshness mismatch");
  if (!Number.isInteger(freshness.stale_after_seconds) || freshness.stale_after_seconds <= 0) {
    throw new Error("Market Overview freshness window is invalid");
  }

  if (market.status === "unavailable" || market.status === "error") {
    if (market.producer !== null || market.context !== null) throw new Error("Unavailable Market Overview contains context");
    if (freshness.observed_at !== null || freshness.age_seconds !== null) {
      throw new Error("Unavailable Market Overview contains producer time");
    }
    return market;
  }

  const producer = object(market.producer);
  const context = object(market.context);
  const descriptors = object(context?.descriptors);
  if (!producer || !nonEmptyString(producer.id)) throw new Error("Market Overview producer identity is invalid");
  if (!context || !nonEmptyString(context.instrument)) throw new Error("Market Overview instrument is invalid");
  for (const field of ["timeframe", "session", "market_state"]) {
    if (nullableString(context[field]) !== context[field]) throw new Error(`Market Overview ${field} is invalid`);
  }
  if (!descriptors) throw new Error("Market Overview descriptors are invalid");
  for (const [key, value] of Object.entries(descriptors)) {
    if (!nonEmptyString(key) || !nonEmptyString(value)) throw new Error("Market Overview descriptor is invalid");
  }
  if (!nonEmptyString(freshness.observed_at) || !Number.isInteger(freshness.age_seconds) || freshness.age_seconds < 0) {
    throw new Error("Market Overview producer timestamp is invalid");
  }
  return market;
}

export async function fetchHomeMarketOverview(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Market Overview fetch is unavailable");
  const response = await fetchImpl(RUNTIME_STATUS_API_PATH, { headers: { accept: "application/json" } });
  if (!response?.ok) throw new Error(`Market Overview status request failed: ${response?.status ?? "unknown"}`);
  return parseHomeMarketOverview(await response.json());
}

function unavailable(title, detail, status) {
  return `<div class="empty-state" data-home-market-overview data-market-status="${escapeHtml(status)}"><div class="empty-icon">—</div><div><strong>${escapeHtml(title)}</strong><p>${escapeHtml(detail)}</p></div></div>`;
}

function row(label, value) {
  return `<div class="stat-row"><span>${escapeHtml(label)}</span><strong>${escapeHtml(value)}</strong></div>`;
}

export function renderHomeMarketOverview(record) {
  if (!record) return unavailable("Checking live market data", "Waiting for canonical backend Market Overview state.", "pending");
  if (record.status === "unavailable") {
    return unavailable("Live market data not connected", record.detail || "No live/current market-data producer is configured.", "unavailable");
  }
  if (record.status === "error") {
    return unavailable("Live market data unavailable", record.detail || "The configured producer could not provide current market context.", "error");
  }

  const context = record.context;
  const freshness = record.freshness;
  const rows = [
    row("Instrument", context.instrument),
    row("Timeframe / context", context.timeframe || "Producer did not supply"),
    row("Source", record.producer.id),
    row("Session", context.session || "Producer did not supply"),
    row("Market state", context.market_state || "Producer did not supply"),
    row("Observed", freshness.observed_at),
    row("Freshness", `${record.status === "stale" ? "Stale" : "Current"} · ${freshness.age_seconds}s old`),
    ...Object.entries(context.descriptors).map(([key, value]) => row(key, value)),
  ];
  return `<div data-home-market-overview data-market-status="${escapeHtml(record.status)}">${rows.join("")}<p class="panel-description">Live/current producer context only. Historical Research data is never substituted.</p></div>`;
}

// Compact form for the ticker strip's market-context cell (the "Market Open · time" cell of
// the prototype). Same parsed record, same explicit states.
export function renderMarketContext(record, errorDetail = "") {
  const dot = (tone) => `<span class="dot tone-${escapeHtml(tone)}" aria-hidden="true"></span>`;
  if (record === null && errorDetail) {
    return `<span class="market-state tone-error" data-market-status="error">${dot("error")}<span>Market state · read failed</span></span><span class="market-time">${escapeHtml(errorDetail)}</span>`;
  }
  if (!record) {
    return `<span class="market-state" data-market-status="pending">${dot("pending")}<span>Market state · checking</span></span><span class="market-time">Waiting for the live market read model</span>`;
  }
  if (record.status === "unavailable" || record.status === "error") {
    const label = record.status === "error" ? "Market data error" : "Market data not connected";
    return `<span class="market-state" data-market-status="${escapeHtml(record.status)}">${dot(record.status === "error" ? "error" : "unavailable")}<span>${escapeHtml(label)}</span></span><span class="market-time">${escapeHtml(record.reason_code ? String(record.reason_code).replaceAll("_", " ") : "No live producer")}</span>`;
  }
  const state = record.context.market_state || record.context.session || "Market state";
  const tone = record.status === "stale" ? "stale" : "current";
  return `<span class="market-state tone-${tone}" data-market-status="${escapeHtml(record.status)}">${dot(tone)}<span>${escapeHtml(state)}${record.status === "stale" ? " · stale" : ""}</span></span><span class="market-time">${escapeHtml(record.freshness.observed_at)} · ${escapeHtml(record.producer.id)}</span>`;
}

function replaceMarketBody(zone, html) {
  const current = zone.querySelector("[data-home-market-overview]") || zone.querySelector(".empty-state");
  if (!current) return false;
  current.outerHTML = html;
  return true;
}

async function bindMarketOverview(zone) {
  replaceMarketBody(zone, renderHomeMarketOverview(null));
  try {
    const record = await fetchHomeMarketOverview();
    if (zone.isConnected) replaceMarketBody(zone, renderHomeMarketOverview(record));
  } catch (error) {
    if (!zone.isConnected) return;
    const detail = error instanceof Error ? error.message : "Market Overview read failed";
    replaceMarketBody(zone, unavailable("Market Overview read failed", detail, "error"));
  }
}

async function bindMarketContext(cell) {
  cell.innerHTML = renderMarketContext(undefined);
  cell.setAttribute("data-market-context-state", "pending");
  try {
    const record = await fetchHomeMarketOverview();
    if (!cell.isConnected) return;
    cell.innerHTML = renderMarketContext(record);
    cell.setAttribute("data-market-context-state", record.status);
  } catch (error) {
    if (!cell.isConnected) return;
    cell.innerHTML = renderMarketContext(null, error instanceof Error ? error.message : "Market Overview read failed");
    cell.setAttribute("data-market-context-state", "error");
  }
}

function mountHomeMarketOverview(root = document) {
  let mounted = false;
  const zone = root.querySelector?.('[data-home-zone="market-overview"]');
  if (zone && zone.dataset.marketOverviewBound !== "true") {
    zone.dataset.marketOverviewBound = "true";
    void bindMarketOverview(zone);
    mounted = true;
  }
  const cell = root.querySelector?.("[data-market-context]");
  if (cell && cell.dataset.marketContextBound !== "true") {
    cell.dataset.marketContextBound = "true";
    void bindMarketContext(cell);
    mounted = true;
  }
  return mounted;
}

if (typeof document !== "undefined") {
  const app = document.querySelector("#app");
  mountHomeMarketOverview(document);
  if (app && typeof MutationObserver !== "undefined") {
    const observer = new MutationObserver(() => mountHomeMarketOverview(document));
    observer.observe(app, { childList: true, subtree: true });
  }
}
