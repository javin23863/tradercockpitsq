import {
  APP_SURFACES,
  RESEARCH_WORKSPACES,
  researchNavPath,
  researchPath,
  resolveRoute,
} from "./model.mjs";
import {
  chip,
  dot,
  escapeHtml,
  icon,
  readable,
  sparkline,
  tabRow,
} from "./ui.mjs";
import {
  fetchIdea,
  fetchIdeaCatalog,
  ingestIdeaSource,
  saveIdeaRevision,
} from "./research-ideas.mjs";
import { EMPTY_RESEARCH_SNAPSHOT, fetchResearchSnapshot, setCurrentResearchSnapshot } from "./research-snapshot.mjs";
import { renderHome } from "./home.mjs";
import { renderSignalsWorkspace } from "./research-signals.mjs";
import { renderEvolutionWorkspace } from "./research-evolution.mjs";
import { renderValidateWorkspace } from "./research-validate.mjs";
import { renderCatalogWorkspace } from "./research-catalog.mjs";
import { renderSecondarySurface } from "./surfaces.mjs";

export { escapeHtml };

const appRoot = typeof document !== "undefined" ? document.querySelector("#app") : null;
const RUNTIME_STATUS_API_PATH = "/api/status";
const RUNTIME_STATUS_SCHEMA = "tc.runtime-status.v1";
const MARKET_QUOTES_API_PATH = "/api/market/quotes";
const MARKET_QUOTES_SCHEMA = "tc.market-quotes.v1";
const MARKET_BARS_API_PATH = "/api/market/bars";
const MARKET_BARS_SCHEMA = "tc.market-bars.v1";
const RESEARCH_NEXT_ACTION_API_PATH = "/api/research/next-action";
const RESEARCH_NEXT_ACTION_SCHEMA = "tc.research-next-action.v1";
const RESEARCH_RECENT_WORK_API_PATH = "/api/research/recent-work";
const RESEARCH_RECENT_WORK_SCHEMA = "tc.recent-work.v1";
const DEFAULT_CHART_TIMEFRAME = "M15";

const EMPTY_MARKET_BARS_STATE = Object.freeze({ phase: "idle", payload: null, detail: "" });
const EMPTY_NEXT_ACTION_STATE = Object.freeze({ phase: "idle", payload: null, detail: "" });
const EMPTY_RECENT_WORK_STATE = Object.freeze({ phase: "idle", payload: null, detail: "" });

let runtimeStatusState = Object.freeze({ phase: "loading", payload: null, detail: "" });
let marketQuotesState = Object.freeze({ phase: "loading", payload: null, detail: "" });
let marketBarsState = EMPTY_MARKET_BARS_STATE;
let nextActionState = EMPTY_NEXT_ACTION_STATE;
let recentWorkState = EMPTY_RECENT_WORK_STATE;
let researchIdeaState = Object.freeze({ phase: "idle", catalog: [], selected: null, detail: "" });
let researchSnapshotState = EMPTY_RESEARCH_SNAPSHOT;

// ---------- read-model access ----------

export function runtimePayload(state) {
  return state?.phase === "loaded" && state.payload?.schema === RUNTIME_STATUS_SCHEMA ? state.payload : null;
}

export function marketQuotesPayload(state) {
  return state?.phase === "loaded" && state.payload?.schema === MARKET_QUOTES_SCHEMA ? state.payload : null;
}

export function marketBarsPayload(state) {
  return state?.phase === "loaded" && state.payload?.schema === MARKET_BARS_SCHEMA ? state.payload : null;
}

export function nextActionPayload(state) {
  return state?.phase === "loaded" && state.payload?.schema === RESEARCH_NEXT_ACTION_SCHEMA ? state.payload : null;
}

export function recentWorkPayload(state) {
  return state?.phase === "loaded" && state.payload?.schema === RESEARCH_RECENT_WORK_SCHEMA ? state.payload : null;
}

export async function fetchRuntimeStatus(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("runtime status fetch is unavailable");
  const response = await fetchImpl(RUNTIME_STATUS_API_PATH, { headers: { accept: "application/json" } });
  if (!response?.ok) throw new Error(`runtime status request failed: ${response?.status ?? "unknown"}`);
  const payload = await response.json();
  if (!payload || payload.schema !== RUNTIME_STATUS_SCHEMA) throw new Error("runtime status schema mismatch");
  return payload;
}

export async function fetchMarketQuotes(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("market quotes fetch is unavailable");
  const response = await fetchImpl(MARKET_QUOTES_API_PATH, { headers: { accept: "application/json" } });
  if (!response?.ok) throw new Error(`market quotes request failed: ${response?.status ?? "unknown"}`);
  const payload = await response.json();
  if (!payload || payload.schema !== MARKET_QUOTES_SCHEMA) throw new Error("market quotes schema mismatch");
  return payload;
}

export async function fetchMarketBars(fetchImpl = globalThis.fetch, request = {}) {
  if (typeof fetchImpl !== "function") throw new Error("market bars fetch is unavailable");
  const params = new URLSearchParams();
  if (request.symbol) params.set("symbol", request.symbol);
  if (request.timeframe) params.set("timeframe", request.timeframe);
  const suffix = params.toString() ? `?${params}` : "";
  const response = await fetchImpl(`${MARKET_BARS_API_PATH}${suffix}`, { headers: { accept: "application/json" } });
  if (!response?.ok) throw new Error(`market bars request failed: ${response?.status ?? "unknown"}`);
  const payload = await response.json();
  if (!payload || payload.schema !== MARKET_BARS_SCHEMA) throw new Error("market bars schema mismatch");
  return payload;
}

export async function fetchNextAction(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("next action fetch is unavailable");
  const response = await fetchImpl(RESEARCH_NEXT_ACTION_API_PATH, { headers: { accept: "application/json" } });
  if (!response?.ok) throw new Error(`next action request failed: ${response?.status ?? "unknown"}`);
  const payload = await response.json();
  if (!payload || payload.schema !== RESEARCH_NEXT_ACTION_SCHEMA) throw new Error("next action schema mismatch");
  return payload;
}

export async function fetchRecentWork(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("recent work fetch is unavailable");
  const response = await fetchImpl(RESEARCH_RECENT_WORK_API_PATH, { headers: { accept: "application/json" } });
  if (!response?.ok) throw new Error(`recent work request failed: ${response?.status ?? "unknown"}`);
  const payload = await response.json();
  if (!payload || payload.schema !== RESEARCH_RECENT_WORK_SCHEMA || !Array.isArray(payload.items)) {
    throw new Error("recent work schema mismatch");
  }
  return payload;
}

// ---------- chrome: rail ----------

function brandMark() {
  return `<svg class="brand-mark" width="26" height="26" viewBox="0 0 24 24" aria-hidden="true"><defs><linearGradient id="tc-brand" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#a78bfa"/><stop offset="1" stop-color="#22d3ee"/></linearGradient></defs><path d="M12 2c2.5 3 2.5 7 0 10-2.5-3-2.5-7 0-10zM12 22c-2.5-3-2.5-7 0-10 2.5 3 2.5 7 0 10zM2 12c3-2.5 7-2.5 10 0-3 2.5-7 2.5-10 0zM22 12c-3 2.5-7 2.5-10 0 3-2.5 7-2.5 10 0z" fill="url(#tc-brand)"/></svg>`;
}

function navLink(surface, active) {
  return `<a class="primary-link ${active ? "is-active" : ""}" href="${escapeHtml(surface.path)}" data-route="${escapeHtml(surface.path)}" ${active ? 'aria-current="page"' : ""}>${icon(surface.icon, { size: 17 })}<span>${escapeHtml(surface.label)}</span></a>`;
}

function railWorkspaceCard(statusState) {
  const payload = runtimePayload(statusState);
  const application = payload?.application;
  const label = application?.status === "ready" ? "Development desktop" : "Desktop";
  const sub = application
    ? `${readable(application.server, "server")} server · loopback`
    : statusState?.phase === "failed" ? "Application status unavailable" : "Checking application…";
  return `<div class="rail-card rail-workspace" data-rail-workspace><span class="rail-workspace-mark">${icon("workspace", { size: 14 })}</span><span class="rail-workspace-text"><strong>${escapeHtml(label)}</strong><small>${escapeHtml(sub)}</small></span>${icon("down", { size: 14 })}</div>`;
}

function railProgressCard(snapshotState, nextAction) {
  const chain = [
    ["Idea", snapshotState.ideas.length],
    ["Configuration", snapshotState.configurations.length],
    ["Native job", snapshotState.jobs.length],
    ["Candidate", snapshotState.candidates.length],
    ["Historical result", snapshotState.results.length],
    ["Proof", snapshotState.proofs.length],
  ];
  const reached = chain.filter(([, count]) => count > 0).length;
  const pct = Math.round((reached / chain.length) * 100);
  const detail = snapshotState.phase === "loading" ? "Reading custody…" : `${reached} / ${chain.length} stages`;
  const action = nextAction?.next_action;
  const nextLine = action
    ? `<span class="next-action-label"><a href="${escapeHtml(action.path)}" data-route="${escapeHtml(action.path)}" data-research-next-action="${escapeHtml(action.id)}">${escapeHtml(action.label)}</a></span>`
    : "";
  return `<div class="rail-card" data-rail-progress><div class="rail-card-row"><strong>Research progress</strong><small>${escapeHtml(detail)}</small></div><div class="bar tone-purple"><i style="width:${snapshotState.phase === "loading" ? 0 : pct}%"></i></div><small>Custody stages with at least one record</small>${nextLine}</div>`;
}

function railRecentWorkCard(recentState) {
  const items = recentWorkPayload(recentState)?.items || [];
  let body = "";
  if (recentState?.phase === "failed") {
    body = `<small>${escapeHtml(recentState.detail || "Recent work unavailable")}</small>`;
  } else if (recentState?.phase !== "loaded") {
    body = `<small>Reading typed identities…</small>`;
  } else if (!items.length) {
    body = `<small>No typed indicator, strategy, or model identities yet</small>`;
  } else {
    body = items.map((item) => `<a href="${escapeHtml(item.path)}" data-route="${escapeHtml(item.path)}" data-recent-work-kind="${escapeHtml(item.object_kind)}"><strong>${escapeHtml(item.summary)}</strong><small>${escapeHtml(readable(item.object_kind))}</small></a>`).join("");
  }
  return `<div class="rail-card" data-rail-recent-work><div class="rail-card-row"><strong>Recent work</strong></div>${body}</div>`;
}

function railPlanCard(statusState) {
  const account = runtimePayload(statusState)?.account;
  const value = account?.status === "ready" ? "Account connected" : "No account";
  const sub = account ? readable(account.reason_code, "Account authority unavailable") : "Checking account…";
  return `<div class="rail-card tone-purple" data-rail-plan><div class="rail-card-row"><span class="rail-workspace-mark" style="background:rgba(124,58,237,.22)">${icon("crown", { size: 14 })}</span><span class="rail-workspace-text"><strong>${escapeHtml(value)}</strong><small>${escapeHtml(sub)}</small></span></div></div>`;
}

function renderRail(route, statusState, snapshotState, nextAction, recentState) {
  const application = runtimePayload(statusState)?.application;
  const tone = application?.status === "ready" ? "ready" : statusState?.phase === "failed" ? "error" : "pending";
  return `<aside class="rail">
    <div class="brand">${brandMark()}<span class="brand-name">TraderCockpit</span></div>
    <nav class="primary-nav" aria-label="Product navigation">${APP_SURFACES.map((surface) => navLink(surface, route.surfaceId === surface.id)).join("")}</nav>
    <div class="rail-bottom">
      ${railWorkspaceCard(statusState)}
      ${railProgressCard(snapshotState, nextAction)}
      ${railRecentWorkCard(recentState)}
      ${railPlanCard(statusState)}
      <div class="rail-version">${dot(tone)}<span>TraderCockpit desktop</span></div>
    </div>
  </aside>`;
}

// ---------- chrome: market ticker ----------

function quoteTone(row) {
  if (row?.status !== "current" || typeof row.change_percent !== "number") return "flat";
  if (row.change_percent > 0) return "up";
  if (row.change_percent < 0) return "down";
  return "flat";
}

function formatLast(row) {
  if (row?.status !== "current" || typeof row.last !== "number" || !Number.isFinite(row.last)) return "—";
  return row.last.toLocaleString("en-US", { maximumFractionDigits: 2 });
}

function formatChange(row) {
  if (row?.status !== "current" || typeof row.change_percent !== "number" || !Number.isFinite(row.change_percent)) return "—";
  const sign = row.change_percent > 0 ? "+" : "";
  return `${sign}${row.change_percent.toFixed(2)}%`;
}

function tickerCell(row) {
  const tone = quoteTone(row);
  const state = row.status === "current" ? (tone === "flat" ? "current" : tone) : "unavailable";
  return `<div class="ticker-cell" data-quote-symbol="${escapeHtml(row.symbol)}" data-quote-status="${escapeHtml(row.status || "unavailable")}" data-quote-tone="${tone}"><span class="ticker-mark tone-${tone}">${escapeHtml(String(row.symbol).slice(0, 1))}</span><span class="ticker-text"><b>${escapeHtml(row.symbol)}</b><span class="ticker-values"><span class="last">${escapeHtml(formatLast(row))}</span><span class="chg">${escapeHtml(formatChange(row))}</span></span></span>${sparkline(state)}</div>`;
}

function renderMarketTicker(marketState) {
  const context = `<div class="ticker-cell ticker-context" data-market-context data-market-context-state="pending"><span class="market-state">${dot("pending")}<span>Market state · checking</span></span><span class="market-time">Waiting for the live market read model</span></div>`;
  if (!marketState || marketState.phase === "loading") {
    return `<div class="market-ticker" data-market-ticker="loading" aria-label="Market ticker"><div class="ticker-cell ticker-empty">Checking the live quotes read model…</div>${context}</div>`;
  }
  if (marketState.phase === "failed") {
    return `<div class="market-ticker" data-market-ticker="unavailable" aria-label="Market ticker"><div class="ticker-cell ticker-empty">${chip("Quotes read failed", "error")}<span>The canonical /api/market/quotes read failed; no quotes are inferred.</span></div>${context}</div>`;
  }
  const payload = marketQuotesPayload(marketState);
  const connected = payload?.status === "current";
  const watchlist = Array.isArray(payload?.watchlist) ? payload.watchlist : [];
  if (!watchlist.length) {
    return `<div class="market-ticker" data-market-ticker="unavailable" aria-label="Market ticker"><div class="ticker-cell ticker-empty">${chip(readable(payload?.reason_code, "Live market data not connected"), "unavailable")}<span>No watchlist configured (TRADERCOCKPIT_WATCHLIST). Connect a market-data provider to populate live quotes.</span></div>${context}</div>`;
  }
  return `<div class="market-ticker" data-market-ticker="${connected ? "live" : "unavailable"}" aria-label="Market ticker" title="${escapeHtml(connected ? `Live quotes · ${payload.provider?.id || "provider"}` : `${readable(payload?.reason_code, "Live market data not connected")} · values appear when a provider is connected`)}">${watchlist.map(tickerCell).join("")}${context}</div>`;
}

// ---------- research workspace switcher ----------

function renderResearchSwitcher(route) {
  return tabRow(
    RESEARCH_WORKSPACES.map((workspace) => ({ id: workspace.id, label: workspace.label })),
    route.workspaceId,
    (item) => researchNavPath(item.id),
    { className: "workspace-switcher", ariaLabel: "Research workspaces" },
  );
}

function renderResearch(route, states) {
  const body = route.workspaceId === "signals"
    ? renderSignalsWorkspace(route, states)
    : route.workspaceId === "evolution"
      ? renderEvolutionWorkspace(route, states)
      : route.workspaceId === "validate"
        ? renderValidateWorkspace(route, states)
        : renderCatalogWorkspace(route, states);
  return `${renderResearchSwitcher(route)}${body}`;
}

function renderContent(route, states) {
  if (route.kind === "redirect") {
    const [pathname, query = ""] = String(route.redirectPath || "/home").split("?");
    return renderContent(resolveRoute(pathname, query ? `?${query}` : ""), states);
  }
  if (route.kind === "research") return renderResearch(route, states);
  if (route.surfaceId === "home") return renderHome(route, states);
  return renderSecondarySurface(route, states);
}

export function renderApp(
  route,
  statusState = { phase: "loading", payload: null, detail: "" },
  ideaState = { phase: "idle", catalog: [], selected: null, detail: "" },
  marketState = { phase: "loading", payload: null, detail: "" },
  snapshotState = EMPTY_RESEARCH_SNAPSHOT,
  barsState = EMPTY_MARKET_BARS_STATE,
  nextActionState = EMPTY_NEXT_ACTION_STATE,
  recentWorkState = EMPTY_RECENT_WORK_STATE,
) {
  const nextAction = nextActionPayload(nextActionState);
  const states = {
    statusState,
    ideaState,
    marketState,
    snapshotState,
    runtime: runtimePayload(statusState),
    quotes: marketQuotesPayload(marketState),
    bars: marketBarsPayload(barsState),
    nextAction,
    barsState,
    nextActionState,
    recentWorkState,
  };
  const unknown = route.unknownPath
    ? `<div class="banner tone-orange" data-unknown-route>${icon("warn", { size: 14 })}<span><strong>Unknown route</strong> <code>${escapeHtml(route.unknownPath)}</code> — Returned to Home without inventing a product surface.</span></div>`
    : "";
  return `<div class="app-shell" data-product-shell="tradercockpit-desktop" data-runtime-status="${escapeHtml(statusState.phase || "loading")}" data-market-status="${escapeHtml(marketState.phase || "loading")}" data-custody-status="${escapeHtml(snapshotState.phase || "loading")}" data-surface-id="${escapeHtml(route.surfaceId || "")}" data-workspace-id="${escapeHtml(route.workspaceId || "")}" data-tab-id="${escapeHtml(route.tabId || "")}">
    ${renderRail(route, statusState, snapshotState, nextAction, recentWorkState)}
    <div class="main-shell">${renderMarketTicker(marketState)}<main class="content-scroll"><div class="content-inner">${unknown}${renderContent(route, states)}</div></main></div>
  </div>`;
}

// ---------- routing / boot ----------

function currentRoute() {
  return resolveRoute(window.location.pathname, window.location.search);
}

function isIdeaRoute(route) {
  return route?.kind === "research" && route.workspaceId === "signals" && route.tabId === "overview";
}

const DESKTOP_SESSION_API_PATH = "/api/desktop/session";
let persistedSessionPath = "";
let sessionWrite = Promise.resolve();

function sessionPathFromRoute(route) {
  if (route?.kind === "research") return route.canonicalPath;
  if (route?.kind === "surface") return route.path + (["/builder", "/custom-projects"].includes(route.path) ? window.location.search : "");
  if (route?.kind === "redirect") return route.redirectPath;
  return "/home";
}

function persistDesktopSession(route) {
  const path = sessionPathFromRoute(route);
  if (!path || path === persistedSessionPath) return;
  persistedSessionPath = path;
  sessionWrite = sessionWrite.then(() => globalThis.fetch(DESKTOP_SESSION_API_PATH, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ path }),
  })).then(response => {
    if (!response.ok && persistedSessionPath === path) persistedSessionPath = "";
  }).catch(() => { if (persistedSessionPath === path) persistedSessionPath = ""; });
}

let lastPaintedSurfaceId = "";

function patchShellChrome(route) {
  const wrap = document.createElement("div");
  wrap.innerHTML = renderApp(route, runtimeStatusState, researchIdeaState, marketQuotesState, researchSnapshotState, marketBarsState, nextActionState, recentWorkState);
  const shell = appRoot.querySelector("[data-product-shell]");
  const nextShell = wrap.querySelector("[data-product-shell]");
  if (shell && nextShell) {
    shell.dataset.runtimeStatus = nextShell.dataset.runtimeStatus || "";
    shell.dataset.marketStatus = nextShell.dataset.marketStatus || "";
    shell.dataset.custodyStatus = nextShell.dataset.custodyStatus || "";
  }
  for (const selector of [".rail", ".market-ticker"]) {
    const cur = appRoot.querySelector(selector);
    const neu = wrap.querySelector(selector);
    if (cur && neu) cur.replaceWith(neu);
  }
}

function renderCurrentRoute({ replaceRedirect = true } = {}) {
  if (!appRoot) return;
  let route = currentRoute();
  if (route.kind === "redirect") {
    if (replaceRedirect) window.history.replaceState({}, "", route.redirectPath);
    route = currentRoute();
  }
  if (route.kind === "research" && replaceRedirect && `${window.location.pathname}${window.location.search}` !== route.canonicalPath) {
    window.history.replaceState(window.history.state, "", route.canonicalPath);
    route = currentRoute();
  }
  const surfaceKey = route.kind === "research" ? `research:${route.workspaceId}:${route.tabId}` : (route.surfaceId || "");
  // These workspaces own their pending operations and selection. Read-model
  // refreshes update chrome without unmounting them; navigation still replaces them.
  const keepMain = Boolean(appRoot.querySelector("[data-automation-workflows], [data-sqx-inspect-host], [data-robustness-workspace], [data-research-proof-workspace]"))
    && lastPaintedSurfaceId === surfaceKey;
  if (keepMain) {
    patchShellChrome(route);
    persistDesktopSession(route);
    return;
  }
  lastPaintedSurfaceId = surfaceKey;
  appRoot.innerHTML = renderApp(route, runtimeStatusState, researchIdeaState, marketQuotesState, researchSnapshotState, marketBarsState, nextActionState, recentWorkState);
  persistDesktopSession(route);
  if (isIdeaRoute(route) && researchIdeaState.phase === "idle") void loadIdeaCatalog();
  else syncIdeaFromRoute(route);
  document.dispatchEvent(new CustomEvent("tradercockpit:shell-painted"));
}

function navigate(path) {
  if (currentRoute().kind === "research") lastPaintedSurfaceId = "";
  window.history.pushState({}, "", path);
  renderCurrentRoute();
}

async function loadRuntimeStatus() {
  try {
    const payload = await fetchRuntimeStatus();
    runtimeStatusState = Object.freeze({ phase: "loaded", payload, detail: "" });
  } catch (error) {
    runtimeStatusState = Object.freeze({ phase: "failed", payload: null, detail: error instanceof Error ? error.message : "runtime status read failed" });
  }
  renderCurrentRoute({ replaceRedirect: false });
}

async function loadMarketQuotes() {
  try {
    const payload = await fetchMarketQuotes();
    marketQuotesState = Object.freeze({ phase: "loaded", payload, detail: "" });
  } catch (error) {
    marketQuotesState = Object.freeze({ phase: "failed", payload: null, detail: error instanceof Error ? error.message : "market quotes read failed" });
  }
  renderCurrentRoute({ replaceRedirect: false });
  void loadMarketBars();
}

async function loadMarketBars() {
  try {
    const quotes = marketQuotesPayload(marketQuotesState);
    const symbol = quotes?.watchlist?.[0]?.symbol;
    const payload = await fetchMarketBars(globalThis.fetch, {
      ...(symbol ? { symbol } : {}),
      timeframe: DEFAULT_CHART_TIMEFRAME,
    });
    marketBarsState = Object.freeze({ phase: "loaded", payload, detail: "" });
  } catch (error) {
    marketBarsState = Object.freeze({ phase: "failed", payload: null, detail: error instanceof Error ? error.message : "market bars read failed" });
  }
  renderCurrentRoute({ replaceRedirect: false });
}

async function loadNextAction() {
  try {
    const payload = await fetchNextAction();
    nextActionState = Object.freeze({ phase: "loaded", payload, detail: "" });
  } catch (error) {
    nextActionState = Object.freeze({ phase: "failed", payload: null, detail: error instanceof Error ? error.message : "next action read failed" });
  }
  renderCurrentRoute({ replaceRedirect: false });
}

async function loadRecentWork() {
  try {
    const payload = await fetchRecentWork();
    recentWorkState = Object.freeze({ phase: "loaded", payload, detail: "" });
  } catch (error) {
    recentWorkState = Object.freeze({ phase: "failed", payload: null, detail: error instanceof Error ? error.message : "recent work read failed" });
  }
  renderCurrentRoute({ replaceRedirect: false });
}

async function loadResearchSnapshot() {
  try {
    researchSnapshotState = await fetchResearchSnapshot();
  } catch (error) {
    researchSnapshotState = Object.freeze({ ...EMPTY_RESEARCH_SNAPSHOT, phase: "failed", failures: Object.freeze({ all: error instanceof Error ? error.message : "custody read failed" }) });
  }
  setCurrentResearchSnapshot(researchSnapshotState);
  renderCurrentRoute({ replaceRedirect: false });
}

async function loadIdeaCatalog({ selected = researchIdeaState.selected } = {}) {
  researchIdeaState = Object.freeze({ phase: "loading", catalog: researchIdeaState.catalog, selected, detail: "" });
  renderCurrentRoute({ replaceRedirect: false });
  try {
    const payload = await fetchIdeaCatalog();
    const selectedStillExists = selected && payload.ideas.some((idea) => idea.entity_id === selected.entity_id);
    researchIdeaState = Object.freeze({ phase: "loaded", catalog: Object.freeze([...payload.ideas]), selected: selectedStillExists ? selected : null, detail: "" });
  } catch (error) {
    researchIdeaState = Object.freeze({ phase: "failed", catalog: [], selected, detail: error instanceof Error ? error.message : "Idea catalog read failed" });
  }
  syncIdeaFromRoute(currentRoute());
  renderCurrentRoute({ replaceRedirect: false });
}

function ideaQueryId() {
  return new URLSearchParams(globalThis.window?.location?.search || "").get("idea") || "";
}

function writeIdeaQuery(entityId) {
  if (typeof window === "undefined") return;
  const params = new URLSearchParams(window.location.search);
  if (entityId) params.set("idea", entityId);
  else params.delete("idea");
  const next = researchPath("signals", "overview", `?${params.toString()}`);
  if (`${window.location.pathname}${window.location.search}` === next) return;
  window.history.replaceState(window.history.state, "", next);
  persistDesktopSession(currentRoute());
}

function syncIdeaFromRoute(route) {
  if (!isIdeaRoute(route)) return;
  const wanted = ideaQueryId();
  if (!wanted || researchIdeaState.selected?.entity_id === wanted || researchIdeaState.phase === "loading") return;
  void selectIdea(wanted);
}

async function selectIdea(entityId) {
  researchIdeaState = Object.freeze({ phase: "loading", catalog: researchIdeaState.catalog, selected: researchIdeaState.selected, detail: "Loading saved revision…" });
  renderCurrentRoute({ replaceRedirect: false });
  try {
    const selected = await fetchIdea(entityId);
    researchIdeaState = Object.freeze({ phase: "loaded", catalog: researchIdeaState.catalog, selected, detail: "" });
    writeIdeaQuery(selected.entity_id);
  } catch (error) {
    researchIdeaState = Object.freeze({ phase: "failed", catalog: researchIdeaState.catalog, selected: null, detail: error instanceof Error ? error.message : "Idea read failed" });
  }
  renderCurrentRoute({ replaceRedirect: false });
}

function setIdeaSaveStatus(message, tone = "") {
  const status = appRoot?.querySelector?.("[data-idea-save-status]");
  if (!status) return;
  status.textContent = message;
  status.dataset.tone = tone;
}

async function saveIdeaFromEditor() {
  const text = appRoot?.querySelector?.("#idea-draft")?.value ?? "";
  const source = appRoot?.querySelector?.("#idea-source")?.value ?? "";
  const selected = researchIdeaState.selected;
  const button = appRoot?.querySelector?.('[data-idea-action="save"]');
  if (button) button.disabled = true;
  setIdeaSaveStatus("Saving immutable revision…", "pending");
  try {
    const saved = await saveIdeaRevision({ entityId: selected?.entity_id || "", expectedRevision: selected?.revision || "", text, source });
    let catalog = researchIdeaState.catalog;
    let detail = "Saved exact Idea revision.";
    try {
      const catalogPayload = await fetchIdeaCatalog();
      catalog = Object.freeze([...catalogPayload.ideas]);
    } catch {
      detail = "Saved exact Idea revision; catalog refresh is temporarily unavailable.";
    }
    researchIdeaState = Object.freeze({ phase: "loaded", catalog, selected: saved, detail });
    writeIdeaQuery(saved.entity_id);
    renderCurrentRoute({ replaceRedirect: false });
    globalThis.window?.dispatchEvent(new CustomEvent("tradercockpit:custody-changed", { detail: { source: "idea" } }));
  } catch (error) {
    const reason = error?.payload?.reason_code === "current_conflict"
      ? "Save refused: this Idea changed elsewhere. Reload the saved revision before retrying."
      : `Save refused: ${error instanceof Error ? error.message : "Idea save failed"}`;
    setIdeaSaveStatus(reason, "error");
    if (button) button.disabled = false;
  }
}

function newIdea() {
  researchIdeaState = Object.freeze({ phase: researchIdeaState.phase === "failed" ? "failed" : "loaded", catalog: researchIdeaState.catalog, selected: null, detail: "" });
  writeIdeaQuery("");
  renderCurrentRoute({ replaceRedirect: false });
}

async function ingestIdeaFromEditor(kind) {
  const selected = researchIdeaState.selected;
  const button = appRoot?.querySelector?.(`[data-idea-action="${kind === "url" ? "ingest-url" : "ingest-document"}"]`);
  if (button) button.disabled = true;
  setIdeaSaveStatus(kind === "url" ? "Ingesting URL…" : "Ingesting document…", "pending");
  try {
    const request = {
      entityId: selected?.entity_id || "",
      expectedRevision: selected?.revision || "",
    };
    if (kind === "url") {
      request.url = appRoot?.querySelector?.("#idea-ingest-url")?.value?.trim() || "";
    } else {
      request.filename = "pasted.txt";
      request.text = appRoot?.querySelector?.("#idea-ingest-document")?.value ?? "";
    }
    const saved = await ingestIdeaSource(request);
    let catalog = researchIdeaState.catalog;
    let detail = "Ingested exact source revision with hashed quoted spans.";
    try {
      const catalogPayload = await fetchIdeaCatalog();
      catalog = Object.freeze([...catalogPayload.ideas]);
    } catch {
      detail = "Ingested exact source revision; catalog refresh is temporarily unavailable.";
    }
    researchIdeaState = Object.freeze({ phase: "loaded", catalog, selected: saved, detail });
    writeIdeaQuery(saved.entity_id);
    renderCurrentRoute({ replaceRedirect: false });
    globalThis.window?.dispatchEvent(new CustomEvent("tradercockpit:custody-changed", { detail: { source: "idea" } }));
  } catch (error) {
    const reason = error?.payload?.reason_code === "current_conflict"
      ? "Ingest refused: this Idea changed elsewhere. Reload the saved revision before retrying."
      : `Ingest refused: ${error instanceof Error ? error.message : "Idea ingest failed"}`;
    setIdeaSaveStatus(reason, "error");
    if (button) button.disabled = false;
  }
}

export function bootApp() {
  if (!appRoot || typeof window === "undefined") return;
  appRoot.addEventListener("click", (event) => {
    const ideaAction = event.target.closest?.("[data-idea-action]");
    if (ideaAction && isIdeaRoute(currentRoute())) {
      const action = ideaAction.getAttribute("data-idea-action");
      event.preventDefault();
      if (action === "new") newIdea();
      if (action === "select") void selectIdea(ideaAction.getAttribute("data-idea-entity-id") || "");
      if (action === "reload" && researchIdeaState.selected?.entity_id) void selectIdea(researchIdeaState.selected.entity_id);
      if (action === "save") void saveIdeaFromEditor();
      if (action === "ingest-url") void ingestIdeaFromEditor("url");
      if (action === "ingest-document") void ingestIdeaFromEditor("document");
      return;
    }

    const link = event.target.closest?.("a[data-route]");
    if (!link || event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    const href = link.getAttribute("href");
    if (!href || !href.startsWith("/")) return;
    event.preventDefault();
    navigate(href);
  });
  window.addEventListener("popstate", () => {
    lastPaintedSurfaceId = "";
    renderCurrentRoute();
  });
  window.addEventListener("tradercockpit:location-changed", () => persistDesktopSession(currentRoute()));
  window.addEventListener("tradercockpit:navigate", (event) => {
    const path = event?.detail?.path;
    if (typeof path !== "string" || !path.startsWith("/") || path.includes("\\") || path.includes("..")) return;
    const [pathname, search = ""] = path.split("?");
    const route = resolveRoute(pathname, search ? `?${search}` : "");
    if (route.unknownPath) return;
    const next = route.redirectPath || route.canonicalPath || route.path;
    if (!next) return;
    navigate(next);
  });
  window.addEventListener("tradercockpit:custody-changed", () => {
    void loadResearchSnapshot();
    void loadNextAction();
    void loadRecentWork();
  });
  renderCurrentRoute();
  void loadRuntimeStatus();
  void loadMarketQuotes();
  void loadResearchSnapshot();
  void loadNextAction();
  void loadRecentWork();
}

if (typeof document !== "undefined") bootApp();
