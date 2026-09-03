import {
  APP_SURFACES,
  RESEARCH_WORKSPACES,
  researchNavPath,
  resolveRoute,
} from "./model.mjs";
import {
  chip,
  dot,
  escapeHtml,
  icon,
  linkButton,
  readable,
  shortId,
  sparkline,
  tabRow,
} from "./ui.mjs";
import {
  fetchIdea,
  fetchIdeaCatalog,
  saveIdeaRevision,
} from "./research-ideas.mjs";
import { EMPTY_RESEARCH_SNAPSHOT, fetchResearchSnapshot, latestRecord, setCurrentResearchSnapshot } from "./research-snapshot.mjs";
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

let runtimeStatusState = Object.freeze({ phase: "loading", payload: null, detail: "" });
let marketQuotesState = Object.freeze({ phase: "loading", payload: null, detail: "" });
let researchIdeaState = Object.freeze({ phase: "idle", catalog: [], selected: null, detail: "" });
let researchSnapshotState = EMPTY_RESEARCH_SNAPSHOT;

// ---------- read-model access ----------

export function runtimePayload(state) {
  return state?.phase === "loaded" && state.payload?.schema === RUNTIME_STATUS_SCHEMA ? state.payload : null;
}

export function marketQuotesPayload(state) {
  return state?.phase === "loaded" && state.payload?.schema === MARKET_QUOTES_SCHEMA ? state.payload : null;
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

function railProgressCard(snapshotState) {
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
  return `<div class="rail-card" data-rail-progress><div class="rail-card-row"><strong>Research progress</strong><small>${escapeHtml(detail)}</small></div><div class="bar tone-purple"><i style="width:${snapshotState.phase === "loading" ? 0 : pct}%"></i></div><small>Custody stages with at least one record</small></div>`;
}

function railPlanCard(statusState) {
  const account = runtimePayload(statusState)?.account;
  const value = account?.status === "ready" ? "Account connected" : "No account";
  const sub = account ? readable(account.reason_code, "Account authority unavailable") : "Checking account…";
  return `<div class="rail-card tone-purple" data-rail-plan><div class="rail-card-row"><span class="rail-workspace-mark" style="background:rgba(124,58,237,.22)">${icon("crown", { size: 14 })}</span><span class="rail-workspace-text"><strong>${escapeHtml(value)}</strong><small>${escapeHtml(sub)}</small></span></div></div>`;
}

function renderRail(route, statusState, snapshotState) {
  const application = runtimePayload(statusState)?.application;
  const tone = application?.status === "ready" ? "ready" : statusState?.phase === "failed" ? "error" : "pending";
  return `<aside class="rail">
    <div class="brand">${brandMark()}<span class="brand-name">TraderCockpit</span></div>
    <nav class="primary-nav" aria-label="Product navigation">${APP_SURFACES.map((surface) => navLink(surface, route.surfaceId === surface.id)).join("")}</nav>
    <div class="rail-bottom">
      ${railWorkspaceCard(statusState)}
      ${railProgressCard(snapshotState)}
      ${railPlanCard(statusState)}
      <div class="rail-version">${dot(tone)}<span>TraderCockpit desktop</span></div>
    </div>
  </aside>`;
}

// ---------- chrome: top bar ----------

function chipState(record) {
  if (!record) return ["Checking…", "pending"];
  if (record.status === "ready" || record.status === "current") return [record.status === "current" ? "Live" : "Ready", "ready"];
  if (record.status === "stale") return ["Stale", "stale"];
  if (record.status === "error" || record.status === "invalid") return [readable(record.reason_code, "Error"), "error"];
  return [readable(record.reason_code, "Not connected"), "unavailable"];
}

function topChip(label, record, key) {
  const [value, tone] = chipState(record);
  return `<div class="top-chip" data-chip="${escapeHtml(key)}" data-tone="${escapeHtml(tone)}" title="${escapeHtml(`${label}: ${value}`)}"><span class="chip-label">${escapeHtml(label)}</span><span class="chip-value">${dot(tone)}<span>${escapeHtml(value)}</span></span></div>`;
}

export function attentionCount(payload) {
  if (!payload) return null;
  const records = [
    payload.application,
    payload.research_backend,
    payload.research_custody,
    payload.market_data,
    payload.provider,
    payload.account,
    payload.model,
    payload.extensions,
    payload.research_backend?.execution?.available === true ? { status: "ready" } : { status: "unavailable" },
  ];
  return records.filter((record) => !record || !["ready", "current"].includes(record.status)).length;
}

function renderTopbar(statusState, marketState) {
  const payload = runtimePayload(statusState);
  const quotes = marketQuotesPayload(marketState);
  const dataFeeds = quotes
    ? { status: quotes.status, reason_code: quotes.reason_code }
    : marketState?.phase === "failed" ? { status: "error", reason_code: "quotes_read_failed" } : null;
  const compute = payload?.research_backend
    ? { status: payload.research_backend.status, reason_code: payload.research_backend.reason_code }
    : statusState?.phase === "failed" ? { status: "error", reason_code: "status_read_failed" } : null;
  const attention = attentionCount(payload);
  return `<header class="topbar">
    <div class="workspace-chip" data-workspace><span class="workspace-text"><span class="workspace-label">Workspace</span><strong>${escapeHtml(payload?.application?.status === "ready" ? "Development desktop" : "TraderCockpit")}</strong></span>${icon("down", { size: 15 })}</div>
    <div class="topbar-chips" aria-label="Operational readiness">
      ${topChip("Data Feeds", dataFeeds, "data-feeds")}
      ${topChip("Broker", payload?.account ?? (statusState?.phase === "failed" ? { status: "error", reason_code: "status_read_failed" } : null), "broker")}
      ${topChip("Compute", compute, "compute")}
      ${topChip("Automation", payload?.extensions ?? (statusState?.phase === "failed" ? { status: "error", reason_code: "status_read_failed" } : null), "automation")}
    </div>
    <div class="topbar-tools">
      <label class="topbar-search" title="Search is not connected yet">${icon("search", { size: 14 })}<input type="search" placeholder="Search" aria-label="Search (not connected yet)" disabled /><kbd>⌘ K</kbd></label>
      <span class="icon-button" title="${escapeHtml(attention === null ? "Attention items: checking" : `${attention} components need attention`)}" data-attention-count="${attention === null ? "" : attention}">${icon("bell", { size: 15 })}<span class="badge-count ${attention === 0 ? "is-zero" : ""}">${attention === null ? "…" : attention}</span></span>
    </div>
  </header>`;
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

// ---------- chrome: status bar ----------

function statusCell(label, value, { tone = "", iconName = "", attrs = "" } = {}) {
  const valueHtml = value === null
    ? `<span class="value-unavailable" title="Requires a live execution/account producer (Operate); not connected">—</span>`
    : `<strong class="${tone ? `tone-text-${tone}` : ""}">${escapeHtml(value)}</strong>`;
  return `<div class="status-cell" ${attrs}>${iconName ? icon(iconName, { size: 14 }) : ""}<span>${escapeHtml(label)}</span>${valueHtml}</div>`;
}

export function lastRunSummary(snapshotState) {
  if (snapshotState.phase === "loading") return { label: "Reading custody…", tone: "pending", state: "pending" };
  const result = latestRecord(snapshotState.results);
  if (result) {
    return {
      label: `Native Retester · ${shortId(result.revision, 10)}`,
      state: result.state,
      tone: result.state === "completed" ? "ready" : result.state === "failed" ? "error" : "pending",
    };
  }
  const job = latestRecord(snapshotState.jobs);
  if (job) {
    return {
      label: `Native Builder job · ${shortId(job.revision, 10)}`,
      state: job.state,
      tone: job.state === "submitted" ? "ready" : job.state === "failed" ? "error" : "pending",
    };
  }
  if (snapshotState.failures?.results || snapshotState.failures?.jobs) return { label: "Run custody unavailable", state: "read failed", tone: "error" };
  return { label: "No native run recorded", state: "none", tone: "unavailable" };
}

function renderStatusBar(snapshotState) {
  const last = lastRunSummary(snapshotState);
  const validatePath = researchNavPath("validate", "overview");
  return `<footer class="status-bar" aria-label="Operational status">
    ${statusCell("Live Runs", null, { iconName: "activity" })}
    ${statusCell("Positions", null)}
    ${statusCell("Daily P&L", null)}
    ${statusCell("Buying Power", null)}
    ${statusCell("Drawdown", null)}
    <div class="status-cell status-spacer"></div>
    <div class="status-cell status-last-run" data-last-run-state="${escapeHtml(last.state)}"><span>Last Run:</span><strong>${escapeHtml(last.label)}</strong>${chip(readable(last.state), last.tone)}</div>
    <div class="status-cell">${linkButton(validatePath, "View", { className: "button-small" })}</div>
  </footer>`;
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
) {
  const states = { statusState, ideaState, marketState, snapshotState, runtime: runtimePayload(statusState), quotes: marketQuotesPayload(marketState) };
  const unknown = route.unknownPath
    ? `<div class="banner tone-orange" data-unknown-route>${icon("warn", { size: 14 })}<span><strong>Unknown route</strong> <code>${escapeHtml(route.unknownPath)}</code> — Returned to Home without inventing a product surface.</span></div>`
    : "";
  return `<div class="app-shell" data-product-shell="tradercockpit-desktop" data-runtime-status="${escapeHtml(statusState.phase || "loading")}" data-market-status="${escapeHtml(marketState.phase || "loading")}" data-custody-status="${escapeHtml(snapshotState.phase || "loading")}" data-surface-id="${escapeHtml(route.surfaceId || "")}" data-workspace-id="${escapeHtml(route.workspaceId || "")}" data-tab-id="${escapeHtml(route.tabId || "")}">
    ${renderRail(route, statusState, snapshotState)}
    <div class="main-shell">${renderTopbar(statusState, marketState)}${renderMarketTicker(marketState)}<main class="content-scroll"><div class="content-inner">${unknown}${renderContent(route, states)}</div></main>${renderStatusBar(snapshotState)}</div>
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

function sessionPathFromRoute(route) {
  if (route?.kind === "research") return route.canonicalPath;
  if (route?.kind === "surface") return route.path;
  if (route?.kind === "redirect") return route.redirectPath;
  return "/home";
}

function persistDesktopSession(route) {
  const path = sessionPathFromRoute(route);
  if (!path || path === persistedSessionPath) return;
  persistedSessionPath = path;
  void globalThis.fetch(DESKTOP_SESSION_API_PATH, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ path }),
  }).catch(() => {});
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
  appRoot.innerHTML = renderApp(route, runtimeStatusState, researchIdeaState, marketQuotesState, researchSnapshotState);
  persistDesktopSession(route);
  if (isIdeaRoute(route) && researchIdeaState.phase === "idle") void loadIdeaCatalog();
}

function navigate(path) {
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
  renderCurrentRoute({ replaceRedirect: false });
}

async function selectIdea(entityId) {
  researchIdeaState = Object.freeze({ phase: "loading", catalog: researchIdeaState.catalog, selected: researchIdeaState.selected, detail: "Loading saved revision…" });
  renderCurrentRoute({ replaceRedirect: false });
  try {
    const selected = await fetchIdea(entityId);
    researchIdeaState = Object.freeze({ phase: "loaded", catalog: researchIdeaState.catalog, selected, detail: "" });
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
    renderCurrentRoute({ replaceRedirect: false });
    void loadResearchSnapshot();
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
  renderCurrentRoute({ replaceRedirect: false });
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
      return;
    }

    const link = event.target.closest?.("a[data-route]");
    if (!link || event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    const href = link.getAttribute("href");
    if (!href || !href.startsWith("/")) return;
    event.preventDefault();
    navigate(href);
  });
  window.addEventListener("popstate", () => renderCurrentRoute());
  window.addEventListener("tradercockpit:custody-changed", () => { void loadResearchSnapshot(); });
  renderCurrentRoute();
  void loadRuntimeStatus();
  void loadMarketQuotes();
  void loadResearchSnapshot();
}

if (typeof document !== "undefined") bootApp();
