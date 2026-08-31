import {
  APP_SURFACES,
  BACKTEST_TAB_IDS,
  CONSTRUCT_TAB_IDS,
  HOME_ZONE_IDS,
  RESEARCH_STAGES,
  resolveRoute,
  researchStage,
} from "./model.mjs";

const RUNTIME_STATUS_API_PATH = "/api/status";
const RUNTIME_STATUS_SCHEMA = "tc.runtime-status.v1";

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function navLink(path, label, { active = false, icon = "" } = {}) {
  return `<a class="nav-link${active ? " is-active" : ""}" href="${escapeHtml(path)}" data-route="${escapeHtml(path)}">${icon ? `<span class="nav-icon" aria-hidden="true">${escapeHtml(icon)}</span>` : ""}<span>${escapeHtml(label)}</span></a>`;
}

function statusBadge(label, tone = "unavailable") {
  return `<span class="status-badge status-${escapeHtml(tone)}"><span class="status-dot"></span>${escapeHtml(label)}</span>`;
}

function panel({ id, title, eyebrow = "", description = "", body = "", homeZone = false }) {
  return `<article class="panel" data-panel="${escapeHtml(id)}"${homeZone ? ` data-home-zone="${escapeHtml(id)}"` : ""}>
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

function launcherStatusValue(research) {
  const launcher = research?.runtime?.launcher;
  if (!launcher) return "Unavailable";
  if (launcher.verified === true && typeof launcher.observed_sha256 === "string") {
    return `Verified · SHA-256 ${launcher.observed_sha256}`;
  }
  return statusValue(launcher);
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
    ["Native launcher", launcherStatusValue(research)],
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
    candidate.path,
    candidate.label,
    { active: route.researchStageId === candidate.id },
  )).join("");
  const tabIds = route.researchStageId === "construct"
    ? CONSTRUCT_TAB_IDS
    : route.researchStageId === "backtest"
      ? BACKTEST_TAB_IDS
      : [];
  const tabs = tabIds.map((tabId) => {
    const tab = stage.tabs.find((candidate) => candidate.id === tabId);
    return navLink(tab.path, tab.label, { active: route.researchTabId === tabId });
  }).join("");
  return `<div class="research-nav"><nav class="stage-nav" aria-label="Research stages">${stageLinks}</nav>${tabs ? `<nav class="tab-nav" aria-label="Research tabs">${tabs}</nav>` : ""}</div>`;
}

function renderHome(route, statusState) {
  const intro = pageIntro(
    route,
    "Cockpit Home",
    "Current market, system, signal, risk, performance, and pipeline orientation. Historical strategy research lives in the separate Research workspace.",
    routeButton("/research", "Open Research", true),
  );
  const orientation = panel({
    id: "home-orientation",
    eyebrow: "TraderCockpit / live orientation",
    title: "See what is happening now, then go to the owning workspace.",
    description: "Home is the live/current cockpit. It does not turn historical research into the application dashboard, and it does not fabricate live values before their producers are connected.",
    body: `<div class="button-row">${routeButton("/operate", "Open Operate")}${routeButton("/explore", "Explore capabilities")}</div>`,
  });
  const zones = [
    panel({
      id: "market-overview",
      homeZone: true,
      eyebrow: "Market Overview",
      title: "Market context",
      description: "Current symbol, timeframe, session, source, and market condition come from the live market-data authority.",
      body: unavailable("Live market data not connected", "The Home screen keeps the market zone visible without substituting historical research data or demo prices."),
    }),
    panel({
      id: "system-status",
      homeZone: true,
      eyebrow: "System Status",
      title: "Runtime attention",
      description: "Application, research backend, market-data, account/model, and extension readiness come from one canonical backend status model.",
      body: `<div class="stat-list">${renderSystemStatus(statusState)}</div>`,
    }),
    panel({
      id: "alpha-stack",
      homeZone: true,
      eyebrow: "Alpha Stack",
      title: "Strategy and deployment stack",
      description: "Current strategy, candidate, champion, and deployed context comes from authoritative custody and execution state.",
      body: unavailable("Alpha Stack not connected", "Historical research candidates may feed this stack after custody, but Home does not manufacture them."),
    }),
    panel({
      id: "pipeline-overview",
      homeZone: true,
      eyebrow: "Pipeline Overview",
      title: "Current pipeline state",
      description: "Show where work is moving from research through validation and deployment, with attention states owned by the backend.",
      body: unavailable("Pipeline read model not connected", "No phase count or completion verdict is inferred from the frontend."),
    }),
    panel({
      id: "signals",
      homeZone: true,
      eyebrow: "Signals",
      title: "Signal pulse",
      description: "Current signal/confluence state requires both a live market feed and strategy/execution context.",
      body: unavailable("Live signals not connected", "Historical backtests are not presented as live signals."),
    }),
    panel({
      id: "risk",
      homeZone: true,
      eyebrow: "Risk",
      title: "Risk and exposure",
      description: "Current portfolio, broker, exposure, loss usage, and deployment risk are separate from historical research metrics.",
      body: unavailable("Live risk state not connected", "Risk remains unavailable until an execution/account authority is configured."),
    }),
    panel({
      id: "performance",
      homeZone: true,
      eyebrow: "Performance",
      title: "Current performance",
      description: "Live/account performance and historical research performance must remain explicitly scoped and never silently mixed.",
      body: unavailable("Current performance not connected", "Historical results remain in Research unless deliberately summarized with clear scope."),
    }),
    panel({
      id: "quick-actions",
      homeZone: true,
      eyebrow: "Quick Actions",
      title: "Go where the work belongs",
      description: "Navigation only; these actions do not create hidden workflows or duplicate producer state.",
      body: `<div class="quick-grid">${routeButton("/research", "Historical research", true)}${routeButton("/operate", "Live operations")}${routeButton("/explore", "Explore")}${routeButton("/automation", "Automation")}</div>`,
    }),
  ].join("");
  return `${intro}${orientation}<div class="panel-grid home-grid">${zones}</div>`;
}

function renderResearch(route) {
  const intro = pageIntro(
    route,
    "Research",
    "Historical strategy construction, native execution evidence, backtesting, and proof live here. StrategyQuant X is the backend producer where provenance requires it, not the product workspace name.",
  );
  const nav = renderResearchNavigation(route);
  const stage = researchStage(route.researchStageId);
  const tab = route.researchTabId ? stage.tabs.find((candidate) => candidate.id === route.researchTabId) : null;
  const title = tab?.label || stage.label;
  const routeLabel = route.researchStageLabel + (route.researchTabLabel ? ` / ${route.researchTabLabel}` : "");
  const description = route.researchStageId === "construct"
    ? "Capture intent, resolve exact native requirements, compile an approved native configuration, then consume real producer candidates."
    : route.researchStageId === "backtest"
      ? "Inspect producer-backed historical results, trades, robustness, and the exact configuration that ran."
      : "Bind the exact idea, configuration, runtime, native job, artifact, result, and validation evidence into durable proof.";
  return `${intro}${nav}${panel({
    id: `research-${route.researchStageId}-${route.researchTabId || "root"}`,
    eyebrow: routeLabel,
    title,
    description,
    body: unavailable(
      `${title} is not connected yet`,
      "This development surface is intentionally present before its producer-backed contract is implemented. No historical result or native action is fabricated.",
    ),
  })}`;
}

function renderSurface(route) {
  const intro = pageIntro(route, route.label, `${route.label} is part of the TraderCockpit desktop product and will be connected through its owning backend contracts.`);
  return `${intro}${panel({
    id: `${route.surfaceId}-foundation`,
    eyebrow: route.label,
    title: `${route.label} foundation`,
    description: "The surface is reserved in the product hierarchy without fabricated data or behavior.",
    body: unavailable("Not connected yet", "This surface will become active only when its canonical producer/read-model contract is implemented."),
  })}`;
}

export function renderApp(route, statusState = { phase: "loading", payload: null, detail: "" }) {
  const unknown = route.unknownPath
    ? `<div class="notice" data-route-notice><strong>Unknown route</strong><span>${escapeHtml(route.unknownPath)} is not a product route. Returned to Home without preserving a legacy redirect.</span></div>`
    : "";
  const content = route.kind === "research"
    ? renderResearch(route)
    : route.surfaceId === "home"
      ? renderHome(route, statusState)
      : renderSurface(route);
  return `<div class="app-shell" data-product-shell="tradercockpit-desktop" data-runtime-status="${escapeHtml(statusState.phase)}" data-surface-id="${escapeHtml(route.surfaceId || "")}" data-research-stage-id="${escapeHtml(route.researchStageId || "")}" data-research-tab-id="${escapeHtml(route.researchTabId || "")}">
    ${renderRail(route, statusState)}
    <div class="workspace">
      ${renderTopbar(route, statusState)}
      <main class="content">${unknown}${content}</main>
    </div>
  </div>`;
}

function installNavigation(render) {
  document.addEventListener("click", (event) => {
    const link = event.target.closest("a[data-route]");
    if (!link) return;
    const url = new URL(link.href, window.location.origin);
    if (url.origin !== window.location.origin) return;
    event.preventDefault();
    history.pushState({}, "", `${url.pathname}${url.search}`);
    render();
  });
  window.addEventListener("popstate", render);
}

export function boot() {
  const root = document.querySelector("#app");
  if (!root) throw new Error("#app root is missing");
  let statusState = { phase: "loading", payload: null, detail: "" };
  const render = () => {
    const route = resolveRoute(window.location.pathname, window.location.search);
    if (route.kind === "redirect") {
      history.replaceState({}, "", route.redirectPath);
      return render();
    }
    root.innerHTML = renderApp(route, statusState);
  };
  installNavigation(render);
  render();
  fetchRuntimeStatus()
    .then((payload) => {
      statusState = { phase: "loaded", payload, detail: "" };
      render();
    })
    .catch((error) => {
      statusState = { phase: "failed", payload: null, detail: String(error?.message || error) };
      render();
    });
}

if (typeof document !== "undefined") {
  boot();
}
