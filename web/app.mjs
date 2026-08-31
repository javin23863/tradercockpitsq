import {
  AUXILIARY_SURFACES,
  CORE_STAGES,
  resolveRoute,
  stageForRoute,
} from "./model.mjs";

const appRoot = typeof document !== "undefined" ? document.querySelector("#app") : null;

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

function panel({ eyebrow, title, description, body, accent = "cyan", className = "" }) {
  return `<article class="panel ${className}" data-accent="${escapeHtml(accent)}">
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

function renderRail(route) {
  const home = AUXILIARY_SURFACES.find((surface) => surface.id === "home");
  const auxiliary = AUXILIARY_SURFACES.filter((surface) => surface.id !== "home");
  return `<aside class="rail">
    <div class="brand"><span class="brand-mark">TC</span><span class="brand-name">TraderCockpit</span></div>
    <div class="rail-context"><span class="context-pulse"></span><span>Development product</span></div>
    <nav class="primary-nav" aria-label="Product navigation">
      ${navLink(home.path, home.label, { active: route.surfaceId === "home", icon: home.icon })}
      ${CORE_STAGES.map((stage) => navLink(stage.path, stage.label, { active: route.stageId === stage.id, icon: stage.icon })).join("")}
      <div class="rail-context"><span>Auxiliary</span></div>
      ${auxiliary.map((surface) => navLink(surface.path, surface.label, { active: route.surfaceId === surface.id, icon: surface.icon })).join("")}
    </nav>
    <div class="rail-footer">
      <div class="rail-footer-line">${statusBadge("Canonical server", "ready")}</div>
      <div class="rail-footer-line">${statusBadge("SQX checking", "unavailable", 'data-sqx-runtime-badge="true"')}</div>
      <div class="rail-footer-meta">Construct · Backtest · Proof</div>
    </div>
  </aside>`;
}

function renderTopbar(route) {
  const stage = stageForRoute(route);
  const context = stage ? `${stage.label}${route.tabId ? ` / ${route.label}` : ""}` : route.label;
  return `<header class="topbar">
    <div class="topbar-context"><p class="eyebrow">Current product surface</p><div class="context-line"><strong>${escapeHtml(context)}</strong><span class="context-separator">/</span><span>development trunk</span></div></div>
    <div class="topbar-status">${statusBadge("Application server connected", "ready")}${statusBadge("Native SQX pending", "unavailable", 'data-sqx-runtime-badge="true"')}<span class="avatar" aria-hidden="true">TC</span></div>
  </header>`;
}

function renderStageTabs(route) {
  const stage = stageForRoute(route);
  if (!stage || stage.tabs.length === 0) return "";
  return `<nav class="secondary-nav" aria-label="${escapeHtml(stage.label)} tabs"><span class="secondary-label">${escapeHtml(stage.label)}</span>${stage.tabs.map((tab) => navLink(tab.path, tab.label, { active: route.tabId === tab.id, className: "subnav-link" })).join("")}</nav>`;
}

function renderHome(route) {
  return `${pageIntro(route, "Development product", "The repository is being consolidated around one native-SQX product spine and one desktop application. This screen is the progress surface future features must land into.", routeButton("/construct/idea", "Open Construct", true))}
    <section class="hero-band" data-accent="purple"><div class="hero-copy"><span class="hero-kicker">CONSOLIDATED PRODUCT TRUNK</span><h2>One product, one runtime, one visible path.</h2><p>StrategyQuant X owns strategy research and quantitative production. TraderCockpit owns the desktop experience, custody, configuration, control/readback, account boundary, and proof. Historical duplicate producer code is not part of this shell.</p><div class="hero-actions">${routeButton("/construct/idea", "Start at Idea", true)}${routeButton("/proof", "View Proof surface")}</div></div><div class="hero-orbit" aria-hidden="true"><span></span><span></span><span></span><b>TC</b></div></section>
    <section class="dashboard-grid">
      ${panel({ eyebrow: "Architecture", title: "Product spine", description: "The fixed research workflow is now the UI authority.", body: `<div class="stat-row"><span>Stages</span><strong>Construct → Backtest → Proof</strong></div><div class="stat-row"><span>Duplicate Builder/GA</span><strong>Removed from production</strong></div>`, accent: "green" })}
      ${panel({ eyebrow: "Native backend", title: "StrategyQuant X", description: "Runtime truth is read from the backend; this shell never invents producer state.", body: `<div data-sqx-runtime-summary>${unavailable("Checking native SQX runtime", "The application is reading configured native runtime availability.")}</div>`, accent: "orange" })}
      ${panel({ eyebrow: "Repository", title: "Consolidation gate", description: "Feature expansion remains paused while native donor work and the desktop foundation are reconciled.", body: `<div class="stat-row"><span>Open implementation donors</span><strong>PR #15 · PR #23</strong></div><div class="stat-row"><span>Consumer account branch</span><strong>Frozen for rebuild</strong></div>`, accent: "purple" })}
      ${panel({ eyebrow: "Desktop", title: "Delivery rule", description: "Future user-facing work is only complete when it appears in this same development application.", body: `<div class="stat-row"><span>Web surface</span><strong>Canonical</strong></div><div class="stat-row"><span>Native window host</span><strong>Development foundation</strong></div>`, accent: "cyan" })}
    </section>`;
}

function renderConstructIdea(route) {
  return `${pageIntro(route, "Idea", "Capture the trading concept and its source before any run or candidate identity exists.", routeButton("/construct/specification", "Review Specification"))}
    <section class="dashboard-grid">
      ${panel({ eyebrow: "Intent", title: "Strategy idea", description: "The editor is a development surface only until durable IdeaRevision custody is integrated.", body: `<label class="field-label" for="idea-draft">Idea draft</label><textarea id="idea-draft" class="idea-editor" placeholder="Describe the trading idea, source, indicator, or existing native strategy…"></textarea><p class="field-help">Draft text is not yet persisted and cannot launch compute.</p><button class="button button-disabled" type="button" disabled>Save revision — not wired yet</button>`, accent: "purple", className: "wide-panel" })}
      ${panel({ eyebrow: "Authoring authority", title: "Native SQX first", description: "Native SQX AI/AlgoWizard is the primary strategy-authoring authority when a supported invocation seam is available.", body: unavailable("Native authoring bridge not integrated", "MCP remains limited to its proven inspection/control tools; optional sqx-lab is not the universal idea path."), accent: "cyan" })}
    </section>`;
}

function renderSpecification(route) {
  const groups = ["Strategy shape", "Entry / conditions", "Exit / risk rules", "Market & data", "Trading assumptions", "Sizing", "Search / build mode", "Ranking & filters", "Validation profile"];
  return `${pageIntro(route, "Specification", "Resolve only the native SQX requirements needed to compile one exact executable configuration.", routeButton("/construct/build", "Open Build"))}
    ${panel({ eyebrow: "Native requirements", title: "Construct plan", description: "These groups are the stable product presentation over native Builder requirements. Backend field-state and evidence are not wired yet.", body: `<div class="requirement-grid">${groups.map((group) => `<div class="requirement-item"><strong>${escapeHtml(group)}</strong>${statusBadge("Pending backend mapping", "unavailable")}</div>`).join("")}</div>`, accent: "orange", className: "wide-panel" })}`;
}

function renderBuild(route) {
  return `${pageIntro(route, "Build", "Review and launch only an exact approved native SQX Builder configuration. TraderCockpit does not run its own GA.")}
    <section class="dashboard-grid">
      ${panel({ eyebrow: "Configuration", title: "Executable native snapshot", description: "The final source/template, exact bytes, diff, approval receipt, and SQX build identity must be visible before launch.", body: unavailable("Construct compiler not integrated", "The prior TraderCockpit-owned evolution engine has been removed. Native Builder control will be integrated from vetted SQX adapter material."), accent: "orange", className: "wide-panel" })}
      ${panel({ eyebrow: "Runtime", title: "SQX readiness", description: "Only verified native runtime state may enable compute.", body: `<div data-sqx-runtime-summary>${unavailable("Checking SQX runtime", "No launch control is exposed until the native gateway and exact configuration custody are reconciled.")}</div>`, accent: "red" })}
    </section>`;
}

function renderCandidates(route) {
  return `${pageIntro(route, "Candidates", "Candidate Lab consumes real native Builder survivors. It is not a generator.")}
    ${panel({ eyebrow: "Candidate Lab", title: "Native .sqx survivors", description: "Candidates will bind Idea → Construct plan → exact configuration → native Builder job → exact .sqx artifact.", body: unavailable("No canonical candidate set loaded", "Vetted PR #23 custody/readback material will be integrated after shared contracts are reconciled."), accent: "purple", className: "wide-panel" })}`;
}

function renderBacktest(route) {
  const detail = {
    overview: ["Overview", "Producer-backed performance summary, run lifecycle, validation funnel, and compatible compare actions."],
    trades: ["Trades", "Actual native trade records and chart context only; no synthetic trades."],
    robustness: ["Robustness", "Native SQX validation methods render dynamically from exact backend plans rather than permanent method tabs."],
    configuration: ["Configuration", "The immutable native configuration that actually executed, including source-to-executed diff and custody identity."],
  }[route.tabId] || ["Backtest", "Native result surface"];
  return `${pageIntro(route, detail[0], detail[1], routeButton("/proof", "Open Proof"))}
    ${panel({ eyebrow: "Native result authority", title: detail[0], description: "This surface stays unavailable until a canonical native candidate/result chain exists.", body: unavailable("Native backtest evidence not loaded", "PR #23 contains vetted native Retester/readback donor material, but it is not merged into the cleaned trunk yet."), accent: route.tabId === "robustness" ? "orange" : "cyan", className: "wide-panel" })}`;
}

function renderProof(route) {
  const chain = ["Intent / Idea revision", "Exact native configuration", "SQX build / job", "Native .sqx strategy", "Native results / trades", "Validation methods / outcomes", "Current product status"];
  return `${pageIntro(route, "Proof", "Show what was requested, what native SQX executed, what artifact survived, and what evidence supports its current status.")}
    ${panel({ eyebrow: "Evidence chain", title: "Exact identities, no inferred proof", description: "Every section remains pending until the underlying native artifact/read model exists.", body: `<div class="proof-chain">${chain.map((item, index) => `<div class="proof-step"><span>${index + 1}</span><strong>${escapeHtml(item)}</strong>${statusBadge("Pending", "unavailable")}</div>`).join("")}</div>`, accent: "green", className: "wide-panel" })}`;
}

function renderAuxiliary(route) {
  if (route.surfaceId === "home") return renderHome(route);
  const copy = {
    explore: ["Explore", "Search backend-registered capabilities, indicators, native strategies/templates, validation methods, delivery targets, and installed add-ons.", "Capability manifest not integrated"],
    automation: ["Automation", "Present and control native SQX Custom Projects without recreating their task engine in TraderCockpit.", "Native Custom Project topology not integrated"],
    operate: ["Operate", "Execution, performance, and risk surfaces appear only for capabilities that truthfully exist.", "Operational capability not configured"],
    settings: ["Settings", "Account, allowance, model policy, native runtime, and installed capability configuration belong here.", "Consumer account/OpenRouter slice will be rebuilt after consolidation"],
  }[route.surfaceId] || ["Home", "Product orientation", "Unavailable"];
  return `${pageIntro(route, copy[0], copy[1])}${panel({ eyebrow: "Auxiliary surface", title: copy[0], description: "This development shell does not fabricate backend capabilities.", body: unavailable(copy[2], "The surface will activate from canonical backend state when its consolidation/integration gate is complete."), accent: "purple", className: "wide-panel" })}`;
}

function renderContent(route) {
  if (route.kind === "auxiliary") return renderAuxiliary(route);
  if (route.stageId === "proof") return renderProof(route);
  if (route.stageId === "construct") {
    if (route.tabId === "idea") return renderConstructIdea(route);
    if (route.tabId === "specification") return renderSpecification(route);
    if (route.tabId === "build") return renderBuild(route);
    return renderCandidates(route);
  }
  if (route.stageId === "backtest") return renderBacktest(route);
  return renderHome({ kind: "auxiliary", surfaceId: "home", label: "Home", path: "/home" });
}

export function renderApp(route) {
  return `<div class="app-shell" data-product-shell="construct-backtest-proof" data-route-kind="${escapeHtml(route.kind)}" data-stage-id="${escapeHtml(route.stageId || "")}" data-tab-id="${escapeHtml(route.tabId || "")}">
    ${renderRail(route)}
    <div class="main-shell">${renderTopbar(route)}${renderStageTabs(route)}<main class="content-scroll"><div class="content-inner">${route.unknownPath ? `<div class="context-callout context-callout-unavailable"><span class="callout-icon">—</span><div><span class="eyebrow">Unknown route</span><strong>${escapeHtml(route.unknownPath)}</strong><span>Returned to Home without inventing a product surface.</span></div></div>` : ""}${renderContent(route)}</div></main></div>
  </div>`;
}

async function refreshSqxRuntime(root = appRoot, fetchImpl = globalThis.fetch) {
  if (!root || typeof fetchImpl !== "function") return;
  let ready = false;
  let summary = "SQX runtime not configured";
  try {
    const response = await fetchImpl("/api/sqx-presets", { headers: { accept: "application/json" } });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const payload = await response.json();
    const presets = Array.isArray(payload?.presets) ? payload.presets : [];
    ready = presets.some((preset) => preset?.runtime?.available === true);
    summary = ready
      ? "Verified SQX runtime detected"
      : "SQX runtime not configured or no reviewed preset is available";
  } catch (error) {
    summary = `SQX status unavailable: ${error?.message || "request failed"}`;
  }

  root.querySelectorAll("[data-sqx-runtime-badge]").forEach((badge) => {
    badge.className = `status-badge status-${ready ? "ready" : "unavailable"}`;
    badge.innerHTML = `<span class="status-dot"></span>${escapeHtml(ready ? "SQX verified" : "SQX unavailable")}`;
  });
  root.querySelectorAll("[data-sqx-runtime-summary]").forEach((target) => {
    target.innerHTML = ready
      ? `<div class="stat-row"><span>Native runtime</span><strong>${escapeHtml(summary)}</strong></div>`
      : unavailable("Native runtime unavailable", summary);
  });
}

function renderCurrentRoute({ replace = false } = {}) {
  if (!appRoot || typeof window === "undefined") return;
  const route = resolveRoute(window.location.pathname);
  if (route.kind === "redirect") {
    window.history[replace ? "replaceState" : "replaceState"]({}, "", route.redirectPath);
    renderCurrentRoute({ replace: true });
    return;
  }
  appRoot.innerHTML = renderApp(route);
  void refreshSqxRuntime(appRoot);
}

export function boot(root = appRoot) {
  if (!root || typeof window === "undefined") return;
  root.addEventListener("click", (event) => {
    const link = event.target.closest("a[data-route]");
    if (!link) return;
    if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    event.preventDefault();
    window.history.pushState({}, "", link.getAttribute("href"));
    renderCurrentRoute();
  });
  window.addEventListener("popstate", () => renderCurrentRoute());
  renderCurrentRoute({ replace: true });
}

if (appRoot) boot(appRoot);
