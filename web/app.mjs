import {
  APP_SURFACES,
  HOME_ZONE_IDS,
  RESEARCH_STAGES,
  researchPath,
  researchStage,
  resolveRoute,
} from "./model.mjs";
import {
  fetchIdea,
  fetchIdeaCatalog,
  saveIdeaRevision,
} from "./research-ideas.mjs";

const appRoot = typeof document !== "undefined" ? document.querySelector("#app") : null;
const RUNTIME_STATUS_API_PATH = "/api/status";
const RUNTIME_STATUS_SCHEMA = "tc.runtime-status.v1";
let runtimeStatusState = Object.freeze({ phase: "loading", payload: null, detail: "" });
let researchIdeaState = Object.freeze({ phase: "idle", catalog: [], selected: null, detail: "" });

export function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function statusBadge(label, tone = "unavailable", extra = "") {
  return `<span class="status-badge status-${escapeHtml(tone)}" ${extra}><span class="status-dot"></span>${escapeHtml(label)}</span>`;
}

function navLink(path, label, { active = false, icon = "", className = "primary-link" } = {}) {
  return `<a class="${className} ${active ? "is-active" : ""}" href="${escapeHtml(path)}" data-route="${escapeHtml(path)}" ${active ? 'aria-current="page"' : ""}>${icon ? `<span class="primary-icon" aria-hidden="true">${escapeHtml(icon)}</span>` : ""}<span>${escapeHtml(label)}</span>${active && className.includes("primary-link") ? '<span class="primary-active-mark" aria-hidden="true"></span>' : ""}</a>`;
}

function panel({ zone = "", eyebrow, title, description, body, accent = "cyan", className = "" }) {
  const zoneAttr = zone ? ` data-home-zone="${escapeHtml(zone)}"` : "";
  return `<article class="panel ${className}" data-accent="${escapeHtml(accent)}"${zoneAttr}>
    <div class="panel-heading"><div>${eyebrow ? `<p class="eyebrow">${escapeHtml(eyebrow)}</p>` : ""}<h2>${escapeHtml(title)}</h2></div></div>
    ${description ? `<p class="panel-description">${escapeHtml(description)}</p>` : ""}
    ${body}
  </article>`;
}

function unavailable(title, detail) {
  return `<div class="empty-state"><div class="empty-icon">—</div><div><strong>${escapeHtml(title)}</strong><p>${escapeHtml(detail)}</p></div></div>`;
}

function pageIntro(route, title, description, action = "") {
  return `<div class="page-intro"><div><p class="eyebrow">${escapeHtml(route.label)}</p><h1>${escapeHtml(title)}</h1><p class="lede">${escapeHtml(description)}</p></div>${action ? `<div class="page-actions">${action}</div>` : ""}</div>`;
}

function routeButton(path, label, primary = false) {
  return `<a class="button ${primary ? "button-primary" : "button-secondary"}" href="${escapeHtml(path)}" data-route="${escapeHtml(path)}">${escapeHtml(label)}</a>`;
}

function readableCode(value) {
  if (!value) return "Unavailable";
  return String(value).replaceAll("_", " ").replaceAll("-", " ").replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function runtimePayload(state) {
  return state?.phase === "loaded" && state.payload?.schema === RUNTIME_STATUS_SCHEMA
    ? state.payload
    : null;
}

function applicationSummary(state) {
  const payload = runtimePayload(state);
  if (!payload) return state?.phase === "failed" ? ["Status unavailable", "unavailable"] : ["Checking application", "unavailable"];
  return payload.application?.status === "ready"
    ? ["Application ready", "ready"]
    : ["Application unavailable", "unavailable"];
}

function researchSummary(state) {
  const research = runtimePayload(state)?.research_backend;
  if (!research) return state?.phase === "failed" ? ["Research status unavailable", "unavailable"] : ["Checking research backend", "unavailable"];
  if (research.status === "ready") return [`Research backend ${research.build || "ready"}`, "ready"];
  return [`Research backend ${research.status || "unavailable"}`, "unavailable"];
}

function statusValue(record, { ready = "Ready", unavailable = "Unavailable" } = {}) {
  if (!record) return unavailable;
  if (record.status === "ready") return ready;
  const label = record.status === "invalid" ? "Invalid" : unavailable;
  return record.reason_code ? `${label} · ${readableCode(record.reason_code)}` : label;
}

function renderSystemStatus(state) {
  const payload = runtimePayload(state);
  if (!payload) {
    const label = state?.phase === "failed" ? "Runtime status unavailable" : "Checking runtime status";
    const detail = state?.phase === "failed"
      ? "The canonical /api/status read failed; no component readiness is inferred."
      : "Waiting for the canonical backend status read model.";
    return unavailable(label, detail);
  }

  const research = payload.research_backend;
  const researchValue = research?.status === "ready"
    ? `Ready · StrategyQuant X ${research.build}`
    : statusValue(research);
  const execution = research?.execution;
  const executionValue = execution?.available
    ? "Ready"
    : `Disabled · ${readableCode(execution?.reason_code)}`;
  const rows = [
    ["TraderCockpit application", statusValue(payload.application)],
    ["Research backend", researchValue],
    ["Research custody", statusValue(payload.research_custody)],
    ["Native execution", executionValue],
    ["Live market data", statusValue(payload.market_data)],
    ["Consumer account", statusValue(payload.account)],
    ["Model access", statusValue(payload.model)],
    ["Extensions", statusValue(payload.extensions)],
  ];
  return rows.map(([label, value]) => `<div class="stat-row" data-runtime-component="${escapeHtml(label.toLowerCase().replaceAll(" ", "-"))}"><span>${escapeHtml(label)}</span><strong>${escapeHtml(value)}</strong></div>`).join("");
}

export async function fetchRuntimeStatus(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("runtime status fetch is unavailable");
  const response = await fetchImpl(RUNTIME_STATUS_API_PATH, { headers: { accept: "application/json" } });
  if (!response?.ok) throw new Error(`runtime status request failed: ${response?.status ?? "unknown"}`);
  const payload = await response.json();
  if (!payload || payload.schema !== RUNTIME_STATUS_SCHEMA) throw new Error("runtime status schema mismatch");
  return payload;
}

function renderRail(route, statusState) {
  const [applicationLabel, applicationTone] = applicationSummary(statusState);
  const [researchLabel, researchTone] = researchSummary(statusState);
  return `<aside class="rail">
    <div class="brand"><span class="brand-mark">TC</span><span class="brand-name">TraderCockpit</span></div>
    <div class="rail-context"><span class="context-pulse"></span><span>Development product</span></div>
    <nav class="primary-nav" aria-label="Product navigation">
      ${APP_SURFACES.map((surface) => navLink(surface.path, surface.label, {
        active: route.surfaceId === surface.id,
        icon: surface.icon,
      })).join("")}
    </nav>
    <div class="rail-footer">
      <div class="rail-footer-line">${statusBadge(applicationLabel, applicationTone)}</div>
      <div class="rail-footer-line">${statusBadge(researchLabel, researchTone)}</div>
      <div class="rail-footer-meta">Home + dedicated Research</div>
    </div>
  </aside>`;
}

function renderTopbar(route, statusState) {
  const context = route.kind === "research"
    ? `Research / ${route.researchStageLabel}${route.researchTabLabel ? ` / ${route.researchTabLabel}` : ""}`
    : route.label;
  const [applicationLabel, applicationTone] = applicationSummary(statusState);
  const [researchLabel, researchTone] = researchSummary(statusState);
  return `<header class="topbar">
    <div class="topbar-context"><p class="eyebrow">Current product surface</p><div class="context-line"><strong>${escapeHtml(context)}</strong><span class="context-separator">/</span><span>development trunk</span></div></div>
    <div class="topbar-status">${statusBadge(applicationLabel, applicationTone)}${statusBadge(researchLabel, researchTone)}<span class="avatar" aria-hidden="true">TC</span></div>
  </header>`;
}

function renderResearchNavigation(route) {
  if (route.kind !== "research") return "";
  const stage = researchStage(route.researchStageId);
  const stageLinks = RESEARCH_STAGES.map((candidate) => navLink(
    researchPath(candidate.id),
    candidate.label,
    { active: route.researchStageId === candidate.id, className: "subnav-link" },
  )).join("");
  const tabLinks = stage?.tabs.length
    ? `<span class="secondary-label research-tab-label">${escapeHtml(stage.label)} tabs</span>${stage.tabs.map((tab) => navLink(
        researchPath(stage.id, tab.id),
        tab.label,
        { active: route.researchTabId === tab.id, className: "subnav-link" },
      )).join("")}`
    : "";
  return `<nav class="secondary-nav" aria-label="Research navigation"><span class="secondary-label">Research</span>${stageLinks}${tabLinks}</nav>`;
}

function renderHome(route, statusState) {
  return `${pageIntro(route, "Cockpit Home", "Current market, system, signal, risk, performance, and pipeline orientation. Historical strategy research lives in the separate Research workspace.", routeButton("/research", "Open Research", true))}
    <section class="hero-band" data-accent="purple"><div class="hero-copy"><span class="hero-kicker">TRADERCOCKPIT / LIVE ORIENTATION</span><h2>See what is happening now, then go to the owning workspace.</h2><p>Home is the live/current cockpit. It does not turn historical research into the application dashboard, and it does not fabricate live values before their producers are connected.</p><div class="hero-actions">${routeButton("/operate", "Open Operate")}${routeButton("/explore", "Explore capabilities")}</div></div><div class="hero-orbit" aria-hidden="true"><span></span><span></span><span></span><b>TC</b></div></section>
    <section class="dashboard-grid cockpit-grid" data-home-zone-count="${HOME_ZONE_IDS.length}">
      ${panel({ zone: "market-overview", eyebrow: "Market Overview", title: "Market context", description: "Current symbol, timeframe, session, source, and market condition come from the live market-data authority.", body: unavailable("Live market data not connected", "The Home screen keeps the market zone visible without substituting historical research data or demo prices."), accent: "green" })}
      ${panel({ zone: "system-status", eyebrow: "System Status", title: "Runtime attention", description: "Application, research backend, market-data, account/model, and extension readiness come from one canonical backend status model.", body: renderSystemStatus(statusState), accent: "red" })}
      ${panel({ zone: "alpha-stack", eyebrow: "Alpha Stack", title: "Strategy and deployment stack", description: "Current strategy, candidate, champion, and deployed context comes from authoritative custody and execution state.", body: unavailable("Alpha Stack not connected", "Historical research candidates may feed this stack after custody, but Home does not manufacture them."), accent: "purple", className: "cockpit-zone-wide" })}
      ${panel({ zone: "pipeline-overview", eyebrow: "Pipeline Overview", title: "Current pipeline state", description: "Show where work is moving from research through validation and deployment, with attention states owned by the backend.", body: unavailable("Pipeline read model not connected", "No phase count or completion verdict is inferred from the frontend."), accent: "orange", className: "cockpit-zone-wide" })}
      ${panel({ zone: "signals", eyebrow: "Signals", title: "Signal pulse", description: "Current signal/confluence state requires both a live market feed and strategy/execution context.", body: unavailable("Live signals not connected", "Historical backtests are not presented as live signals."), accent: "cyan" })}
      ${panel({ zone: "risk", eyebrow: "Risk", title: "Risk and exposure", description: "Current portfolio, broker, exposure, loss usage, and deployment risk are separate from historical research metrics.", body: unavailable("Live risk state not connected", "Risk remains unavailable until an execution/account authority is configured."), accent: "red" })}
      ${panel({ zone: "performance", eyebrow: "Performance", title: "Current performance", description: "Live/account performance and historical research performance must remain explicitly scoped and never silently mixed.", body: unavailable("Current performance not connected", "Historical results remain in Research unless deliberately summarized with clear scope."), accent: "green", className: "cockpit-zone-wide" })}
      ${panel({ zone: "quick-actions", eyebrow: "Quick Actions", title: "Go where the work belongs", description: "Navigation only; these actions do not create hidden workflows or duplicate producer state.", body: `<div class="quick-action-grid">${routeButton("/research", "Historical research", true)}${routeButton("/operate", "Live operations")}${routeButton("/explore", "Explore")}${routeButton("/automation", "Automation")}</div>`, accent: "cyan", className: "cockpit-zone-wide" })}
    </section>`;
}

function renderIdeaCatalog(state) {
  if (state.phase === "loading" || state.phase === "idle") {
    return `<div class="idea-catalog-state">Loading saved Ideas…</div>`;
  }
  if (state.phase === "failed") {
    return `<div class="idea-catalog-state idea-error">${escapeHtml(state.detail || "Idea catalog unavailable")}</div>`;
  }
  if (!state.catalog.length) {
    return `<div class="idea-catalog-state">No saved Ideas yet.</div>`;
  }
  return `<div class="idea-catalog-list">${state.catalog.map((idea) => {
    const active = state.selected?.entity_id === idea.entity_id;
    return `<button class="idea-catalog-item ${active ? "is-active" : ""}" type="button" data-idea-action="select" data-idea-entity-id="${escapeHtml(idea.entity_id)}"><strong>${escapeHtml(idea.summary)}</strong><span>${escapeHtml(String(idea.revision).slice(-12))}</span></button>`;
  }).join("")}</div>`;
}

function renderConstructIdea(route, state) {
  const selected = state?.selected || null;
  const isLoading = state?.phase === "loading" || state?.phase === "idle";
  const revisionDetail = selected
    ? `<div class="idea-identity" data-idea-current-identity><div><span>Idea entity</span><code>${escapeHtml(selected.entity_id)}</code></div><div><span>Current revision</span><code>${escapeHtml(selected.revision)}</code></div>${selected.parent_revision ? `<div><span>Parent revision</span><code>${escapeHtml(selected.parent_revision)}</code></div>` : ""}<div><span>Content identity</span><code>${escapeHtml(selected.content_ref)}</code></div></div>`
    : `<div class="idea-new-state"><strong>New Idea</strong><span>Saving will mint the Idea identity on the backend and create its first immutable revision.</span></div>`;
  const detail = state?.detail ? `<p class="idea-save-status" data-idea-save-status>${escapeHtml(state.detail)}</p>` : `<p class="idea-save-status" data-idea-save-status></p>`;
  return `${pageIntro(route, "Idea", "Capture the strategy concept and provenance before native configuration or candidate identity exists.")}
    <section class="idea-workspace" data-research-idea-workspace>
      ${panel({ eyebrow: "Saved Ideas", title: "Revision custody", description: "Select an existing Idea or start a new one. Identity is created only by the canonical backend.", body: `<button class="button button-secondary" type="button" data-idea-action="new">New Idea</button>${renderIdeaCatalog(state)}`, accent: "cyan", className: "idea-catalog-panel" })}
      ${panel({ eyebrow: "Historical research", title: "Strategy idea", description: "Idea revisions preserve source text and provenance only. Saving does not create a candidate, run native compute, or infer trading semantics.", body: `${revisionDetail}<label class="field-label" for="idea-draft">Idea draft</label><textarea id="idea-draft" class="idea-editor" maxlength="100000" placeholder="Describe the strategy idea, source, indicator, or existing native strategy…" ${isLoading ? "disabled" : ""}>${escapeHtml(selected?.text || "")}</textarea><label class="field-label" for="idea-source">Source / provenance</label><textarea id="idea-source" class="idea-source-editor" maxlength="20000" placeholder="Where did this idea come from? Notes, observation, native strategy/template reference…" ${isLoading ? "disabled" : ""}>${escapeHtml(selected?.source || "")}</textarea><p class="field-help">Each save creates a new immutable content-addressed Idea revision and compare-and-set updates only this Idea's current pointer.</p>${detail}<div class="idea-actions"><button class="button button-primary" type="button" data-idea-action="save" ${isLoading ? "disabled" : ""}>${selected ? "Save new revision" : "Save Idea"}</button>${selected ? `<button class="button button-secondary" type="button" data-idea-action="reload">Reload saved revision</button>` : ""}</div>`, accent: "purple", className: "idea-editor-panel" })}
    </section>`;
}

function renderSpecification(route) {
  const groups = ["Strategy shape", "Entry / conditions", "Exit / risk rules", "Market & historical data", "Trading assumptions", "Sizing", "Search / build mode", "Ranking & filters", "Validation profile"];
  return `${pageIntro(route, "Specification", "Resolve the native research requirements needed to compile one exact historical-research configuration.")}
    ${panel({ eyebrow: "Native requirements", title: "Construct plan", description: "These groups present backend research requirements without reproducing the native strategy engine.", body: `<div class="requirement-grid">${groups.map((group) => `<div class="requirement-item"><strong>${escapeHtml(group)}</strong>${statusBadge("Loading canonical authority", "unavailable")}</div>`).join("")}</div>`, accent: "orange", className: "wide-panel" })}`;
}

function renderBuild(route) {
  return `${pageIntro(route, "Build", "Review and launch an exact approved native research configuration. The platform does not run a duplicate GA.")}
    <section class="dashboard-grid">
      ${panel({ eyebrow: "Native backend", title: "Executable configuration", description: "Exact source/template, bytes, diff, approval receipt, and producer build identity must be inspectable before launch.", body: unavailable("Loading configuration custody", "Reading the canonical configuration catalog and exact executable-byte authority."), accent: "orange", className: "wide-panel" })}
      ${panel({ eyebrow: "Runtime", title: "Research backend readiness", description: "Only verified native runtime state may enable historical research compute.", body: unavailable("Loading native execution authority", "Reading trusted runtime readiness and approved exact Builder-job controls."), accent: "red" })}
    </section>`;
}

function renderCandidates(route) {
  return `${pageIntro(route, "Candidates", "Candidate Lab consumes real native Builder survivors. It does not generate strategies itself.")}
    ${panel({ eyebrow: "Candidate Lab", title: "Native strategy survivors", description: "Candidates bind idea/source, exact configuration, native Builder job, and native artifact identity.", body: unavailable("Loading Candidate custody", "Reading canonical Candidate, submitted native-job, and inspectable Results archive authorities."), accent: "purple", className: "wide-panel" })}`;
}

function renderBacktest(route) {
  const detail = {
    overview: ["Overview", "Historical producer-backed performance summary, run lifecycle, validation funnel, and compatible compare actions."],
    trades: ["Trades", "Actual historical native trade records and chart context only; no synthetic trades."],
    robustness: ["Robustness", "Native validation methods rendered from exact historical test plans rather than permanent method tabs."],
    configuration: ["Configuration", "The immutable native configuration that actually executed, with source-to-executed custody."],
  }[route.researchTabId] || ["Backtest", "Historical native result surface"];
  return `${pageIntro(route, detail[0], detail[1])}${panel({ eyebrow: "Native research", title: detail[0], description: "Historical state is loaded from canonical Candidate, Retester, trade, robustness, and executed-chain custody according to this route.", body: unavailable("Loading historical custody", "Reading the route-specific canonical historical Research authority without inventing metrics or verdicts."), accent: route.researchTabId === "robustness" ? "orange" : "cyan", className: "wide-panel" })}`;
}

function renderProof(route) {
  const chain = ["Intent / Idea revision", "Exact native configuration", "Producer build / job", "Native strategy artifact", "Historical results / trades", "Validation methods / outcomes", "Current product status"];
  return `${pageIntro(route, "Proof", "Bind the exact historical research request, native execution, surviving artifact, and evidence chain.")}
    ${panel({ eyebrow: "Evidence chain", title: "Exact identities, no inferred proof", description: "Exact historical identities are loaded from canonical Proof custody; no missing step or producer outcome is inferred.", body: `<div class="proof-chain">${chain.map((item, index) => `<div class="proof-step"><span>${index + 1}</span><strong>${escapeHtml(item)}</strong>${statusBadge("Loading", "unavailable")}</div>`).join("")}</div>`, accent: "green", className: "wide-panel" })}`;
}

function renderResearch(route, ideaState) {
  if (route.researchStageId === "construct") {
    if (route.researchTabId === "idea") return renderConstructIdea(route, ideaState);
    if (route.researchTabId === "specification") return renderSpecification(route);
    if (route.researchTabId === "build") return renderBuild(route);
    return renderCandidates(route);
  }
  if (route.researchStageId === "backtest") return renderBacktest(route);
  return renderProof(route);
}

function renderSurface(route, statusState) {
  if (route.surfaceId === "home") return renderHome(route, statusState);
  const copy = {
    explore: ["Explore", "Discover registered capabilities, markets, data, native templates/strategies, validation methods, and installed add-ons.", "Capability manifest not implemented"],
    automation: ["Automation", "Present and control native backend projects without recreating their task engine in the platform.", "Automation read surface not implemented"],
    operate: ["Operate", "Live/deployed runs, execution, performance, and risk belong here when those capabilities truthfully exist.", "Live operational capability not configured"],
    settings: ["Settings", "Account, allowance, model policy, native runtime, provider, and installed capability configuration.", "Consumer account and model access are not implemented yet"],
  }[route.surfaceId] || ["Home", "Current product orientation", "Unavailable"];
  return `${pageIntro(route, copy[0], copy[1])}${panel({ eyebrow: "Product surface", title: copy[0], description: "This development desktop does not fabricate backend capabilities.", body: unavailable(copy[2], "The surface will activate from canonical backend state when its implementation is complete."), accent: "purple", className: "wide-panel" })}`;
}

function renderContent(route, statusState, ideaState) {
  if (route.kind === "research") return renderResearch(route, ideaState);
  return renderSurface(route, statusState);
}

export function renderApp(
  route,
  statusState = { phase: "loading", payload: null, detail: "" },
  ideaState = { phase: "idle", catalog: [], selected: null, detail: "" },
) {
  return `<div class="app-shell" data-product-shell="tradercockpit-desktop" data-runtime-status="${escapeHtml(statusState.phase || "loading")}" data-surface-id="${escapeHtml(route.surfaceId || "")}" data-research-stage-id="${escapeHtml(route.researchStageId || "")}" data-research-tab-id="${escapeHtml(route.researchTabId || "")}">
    ${renderRail(route, statusState)}
    <div class="main-shell">${renderTopbar(route, statusState)}${renderResearchNavigation(route)}<main class="content-scroll"><div class="content-inner">${route.unknownPath ? `<div class="context-callout"><span class="callout-icon">—</span><div><span class="eyebrow">Unknown route</span><strong>${escapeHtml(route.unknownPath)}</strong><span>Returned to Home without inventing a product surface.</span></div></div>` : ""}${renderContent(route, statusState, ideaState)}</div></main></div>
  </div>`;
}

function currentRoute() {
  return resolveRoute(window.location.pathname, window.location.search);
}

function isIdeaRoute(route) {
  return route?.kind === "research" && route.researchStageId === "construct" && route.researchTabId === "idea";
}

function renderCurrentRoute({ replaceRedirect = true } = {}) {
  if (!appRoot) return;
  let route = currentRoute();
  if (route.kind === "redirect") {
    if (replaceRedirect) window.history.replaceState({}, "", route.redirectPath);
    route = currentRoute();
  }
  appRoot.innerHTML = renderApp(route, runtimeStatusState, researchIdeaState);
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
    runtimeStatusState = Object.freeze({
      phase: "failed",
      payload: null,
      detail: error instanceof Error ? error.message : "runtime status read failed",
    });
  }
  renderCurrentRoute({ replaceRedirect: false });
}

async function loadIdeaCatalog({ selected = researchIdeaState.selected } = {}) {
  researchIdeaState = Object.freeze({
    phase: "loading",
    catalog: researchIdeaState.catalog,
    selected,
    detail: "",
  });
  renderCurrentRoute({ replaceRedirect: false });
  try {
    const payload = await fetchIdeaCatalog();
    const selectedStillExists = selected && payload.ideas.some((idea) => idea.entity_id === selected.entity_id);
    researchIdeaState = Object.freeze({
      phase: "loaded",
      catalog: Object.freeze([...payload.ideas]),
      selected: selectedStillExists ? selected : null,
      detail: "",
    });
  } catch (error) {
    researchIdeaState = Object.freeze({
      phase: "failed",
      catalog: [],
      selected,
      detail: error instanceof Error ? error.message : "Idea catalog read failed",
    });
  }
  renderCurrentRoute({ replaceRedirect: false });
}

async function selectIdea(entityId) {
  researchIdeaState = Object.freeze({
    phase: "loading",
    catalog: researchIdeaState.catalog,
    selected: researchIdeaState.selected,
    detail: "Loading saved revision…",
  });
  renderCurrentRoute({ replaceRedirect: false });
  try {
    const selected = await fetchIdea(entityId);
    researchIdeaState = Object.freeze({
      phase: "loaded",
      catalog: researchIdeaState.catalog,
      selected,
      detail: "",
    });
  } catch (error) {
    researchIdeaState = Object.freeze({
      phase: "failed",
      catalog: researchIdeaState.catalog,
      selected: null,
      detail: error instanceof Error ? error.message : "Idea read failed",
    });
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
    const saved = await saveIdeaRevision({
      entityId: selected?.entity_id || "",
      expectedRevision: selected?.revision || "",
      text,
      source,
    });
    let catalog = researchIdeaState.catalog;
    let detail = "Saved exact Idea revision.";
    try {
      const catalogPayload = await fetchIdeaCatalog();
      catalog = Object.freeze([...catalogPayload.ideas]);
    } catch {
      detail = "Saved exact Idea revision; catalog refresh is temporarily unavailable.";
    }
    researchIdeaState = Object.freeze({
      phase: "loaded",
      catalog,
      selected: saved,
      detail,
    });
    renderCurrentRoute({ replaceRedirect: false });
  } catch (error) {
    const reason = error?.payload?.reason_code === "current_conflict"
      ? "Save refused: this Idea changed elsewhere. Reload the saved revision before retrying."
      : `Save refused: ${error instanceof Error ? error.message : "Idea save failed"}`;
    setIdeaSaveStatus(reason, "error");
    if (button) button.disabled = false;
  }
}

function newIdea() {
  researchIdeaState = Object.freeze({
    phase: researchIdeaState.phase === "failed" ? "failed" : "loaded",
    catalog: researchIdeaState.catalog,
    selected: null,
    detail: "",
  });
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
  renderCurrentRoute();
  void loadRuntimeStatus();
}

if (typeof document !== "undefined") bootApp();
