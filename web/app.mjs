import {
  APP_SURFACES,
  HOME_ZONE_IDS,
  RESEARCH_STAGES,
  researchPath,
  researchStage,
  resolveRoute,
} from "./model.mjs";

const appRoot = typeof document !== "undefined" ? document.querySelector("#app") : null;
const RUNTIME_STATUS_API_PATH = "/api/status";
const RUNTIME_STATUS_SCHEMA = "tc.runtime-status.v1";
let runtimeStatusState = Object.freeze({ phase: "loading", payload: null, detail: "" });

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

function renderConstructIdea(route) {
  return `${pageIntro(route, "Idea", "Capture the strategy concept and provenance before native configuration or candidate identity exists.")}
    ${panel({ eyebrow: "Historical research", title: "Strategy idea", description: "This is inside the platform Research workspace. Durable IdeaRevision custody is not integrated yet.", body: `<label class="field-label" for="idea-draft">Idea draft</label><textarea id="idea-draft" class="idea-editor" placeholder="Describe the strategy idea, source, indicator, or existing native strategy…"></textarea><p class="field-help">Draft text is not persisted and cannot launch native compute.</p><button class="button button-disabled" type="button" disabled>Save revision — not wired yet</button>`, accent: "purple", className: "wide-panel" })}`;
}

function renderSpecification(route) {
  const groups = ["Strategy shape", "Entry / conditions", "Exit / risk rules", "Market & historical data", "Trading assumptions", "Sizing", "Search / build mode", "Ranking & filters", "Validation profile"];
  return `${pageIntro(route, "Specification", "Resolve the native research requirements needed to compile one exact historical-research configuration.")}
    ${panel({ eyebrow: "Native requirements", title: "Construct plan", description: "These groups present backend research requirements without reproducing the native strategy engine.", body: `<div class="requirement-grid">${groups.map((group) => `<div class="requirement-item"><strong>${escapeHtml(group)}</strong>${statusBadge("Pending backend mapping", "unavailable")}</div>`).join("")}</div>`, accent: "orange", className: "wide-panel" })}`;
}

function renderBuild(route) {
  return `${pageIntro(route, "Build", "Review and launch an exact approved native research configuration. The platform does not run a duplicate GA.")}
    <section class="dashboard-grid">
      ${panel({ eyebrow: "Native backend", title: "Executable configuration", description: "Exact source/template, bytes, diff, approval receipt, and producer build identity must be inspectable before launch.", body: unavailable("Native construct compiler not implemented", "The platform has no substitute Builder or evolution engine."), accent: "orange", className: "wide-panel" })}
      ${panel({ eyebrow: "Runtime", title: "Research backend readiness", description: "Only verified native runtime state may enable historical research compute.", body: unavailable("Trusted native gateway not implemented", "Native execution remains disabled until the launcher identity and control boundary are verified."), accent: "red" })}
    </section>`;
}

function renderCandidates(route) {
  return `${pageIntro(route, "Candidates", "Candidate Lab consumes real native Builder survivors. It does not generate strategies itself.")}
    ${panel({ eyebrow: "Candidate Lab", title: "Native strategy survivors", description: "Candidates bind idea/source, exact configuration, native Builder job, and native artifact identity.", body: unavailable("Candidate custody not implemented", "No native candidate/result identity chain exists in the clean application yet."), accent: "purple", className: "wide-panel" })}`;
}

function renderBacktest(route) {
  const detail = {
    overview: ["Overview", "Historical producer-backed performance summary, run lifecycle, validation funnel, and compatible compare actions."],
    trades: ["Trades", "Actual historical native trade records and chart context only; no synthetic trades."],
    robustness: ["Robustness", "Native validation methods rendered from exact historical test plans rather than permanent method tabs."],
    configuration: ["Configuration", "The immutable native configuration that actually executed, with source-to-executed custody."],
  }[route.researchTabId] || ["Backtest", "Historical native result surface"];
  return `${pageIntro(route, detail[0], detail[1])}${panel({ eyebrow: "Native research", title: detail[0], description: "This historical research surface remains unavailable until a canonical native candidate/result chain exists.", body: unavailable("Native historical result not loaded", "Native Retester/result custody will be implemented only through the trusted native gateway and canonical application identities."), accent: route.researchTabId === "robustness" ? "orange" : "cyan", className: "wide-panel" })}`;
}

function renderProof(route) {
  const chain = ["Intent / Idea revision", "Exact native configuration", "Producer build / job", "Native strategy artifact", "Historical results / trades", "Validation methods / outcomes", "Current product status"];
  return `${pageIntro(route, "Proof", "Bind the exact historical research request, native execution, surviving artifact, and evidence chain.")}
    ${panel({ eyebrow: "Evidence chain", title: "Exact identities, no inferred proof", description: "Every item stays pending until its underlying native artifact/read model exists.", body: `<div class="proof-chain">${chain.map((item, index) => `<div class="proof-step"><span>${index + 1}</span><strong>${escapeHtml(item)}</strong>${statusBadge("Pending", "unavailable")}</div>`).join("")}</div>`, accent: "green", className: "wide-panel" })}`;
}

function renderResearch(route) {
  if (route.researchStageId === "construct") {
    if (route.researchTabId === "idea") return renderConstructIdea(route);
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

function renderContent(route, statusState) {
  if (route.kind === "research") return renderResearch(route);
  return renderSurface(route, statusState);
}

export function renderApp(route, statusState = { phase: "loading", payload: null, detail: "" }) {
  return `<div class="app-shell" data-product-shell="tradercockpit-desktop" data-runtime-status="${escapeHtml(statusState.phase || "loading")}" data-surface-id="${escapeHtml(route.surfaceId || "")}" data-research-stage-id="${escapeHtml(route.researchStageId || "")}" data-research-tab-id="${escapeHtml(route.researchTabId || "")}">
    ${renderRail(route, statusState)}
    <div class="main-shell">${renderTopbar(route, statusState)}${renderResearchNavigation(route)}<main class="content-scroll"><div class="content-inner">${route.unknownPath ? `<div class="context-callout"><span class="callout-icon">—</span><div><span class="eyebrow">Unknown route</span><strong>${escapeHtml(route.unknownPath)}</strong><span>Returned to Home without inventing a product surface.</span></div></div>` : ""}${renderContent(route, statusState)}</div></main></div>
  </div>`;
}

function currentRoute() {
  return resolveRoute(window.location.pathname, window.location.search);
}

function renderCurrentRoute({ replaceRedirect = true } = {}) {
  if (!appRoot) return;
  let route = currentRoute();
  if (route.kind === "redirect") {
    if (replaceRedirect) window.history.replaceState({}, "", route.redirectPath);
    route = currentRoute();
  }
  appRoot.innerHTML = renderApp(route, runtimeStatusState);
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

export function bootApp() {
  if (!appRoot || typeof window === "undefined") return;
  appRoot.addEventListener("click", (event) => {
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
