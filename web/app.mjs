import {
  APOLLO_SURFACE_ID,
  LOGICAL_STATES,
  PRIMARY_WORKSPACES,
  RUN_CONTEXT_OWNER,
  RUN_SURFACE_ID,
  UNAVAILABLE_REASON,
  contextualPath,
  pathForState,
  resolveRoute,
  workspaceForRoute,
} from "./model.mjs";

const capabilityCopy = {
  market: "Market data",
  strategy: "Strategy data",
  catalog: "Catalog data",
  run: "Run state",
  validation: "Validation results",
  robustness: "Robustness results",
  evidence: "Evidence",
  prop: "Prop rule-set",
  execution: "Execution and risk state",
};

export function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function statusBadge(label = "Status pending", tone = "pending") {
  return `<span class="status-badge status-${tone}"><span class="status-dot"></span>${escapeHtml(label)}</span>`;
}

function routeLink(path, label, className = "button button-secondary") {
  return `<a class="${className}" href="${escapeHtml(path)}" data-route="${escapeHtml(path)}">${escapeHtml(label)}</a>`;
}

function contextualRoute(path, route) {
  return route.strategyRef ? contextualPath(path, route.strategyRef) : path;
}

function contextualRouteLink(route, path, label, className = "button button-secondary") {
  return routeLink(contextualRoute(path, route), label, className);
}

function panel({ eyebrow, title, description, body, accent = "cyan", className = "" }) {
  return `
    <article class="panel ${className}" data-accent="${accent}">
      <div class="panel-heading">
        <div>
          ${eyebrow ? `<p class="eyebrow">${escapeHtml(eyebrow)}</p>` : ""}
          <h2>${escapeHtml(title)}</h2>
        </div>
        ${statusBadge()}
      </div>
      ${description ? `<p class="panel-description">${escapeHtml(description)}</p>` : ""}
      ${body}
    </article>`;
}

function unavailableState(capability, detail = UNAVAILABLE_REASON) {
  return `
    <div class="empty-state" data-capability="${escapeHtml(capability)}">
      <div class="empty-icon">—</div>
      <div>
        <strong>${escapeHtml(capabilityCopy[capability] || capability)} not available to this frontend</strong>
        <p>${escapeHtml(detail)}</p>
      </div>
    </div>`;
}

function disabledAction(label, reason = UNAVAILABLE_REASON) {
  return `<button class="button button-disabled" type="button" disabled title="${escapeHtml(reason)}">${escapeHtml(label)}</button>`;
}

function chartEmpty(label = "Chart data not available to this frontend") {
  return `
    <div class="chart-empty" role="img" aria-label="${escapeHtml(label)}">
      <div class="chart-grid"></div>
      <div class="chart-empty-copy">
        <span class="chart-mark">⌁</span>
        <strong>${escapeHtml(label)}</strong>
        <span>This frontend surface is not yet connected to its authoritative market producer.</span>
      </div>
    </div>`;
}

function routeCard({ eyebrow, title, description, path, accent = "cyan" }) {
  return `
    <a class="route-card" data-accent="${accent}" href="${escapeHtml(path)}" data-route="${escapeHtml(path)}">
      <span class="route-card-eyebrow">${escapeHtml(eyebrow)}</span>
      <span class="route-card-title">${escapeHtml(title)} <span aria-hidden="true">↗</span></span>
      <span class="route-card-description">${escapeHtml(description)}</span>
    </a>`;
}

function statRow(label, value = "Not available to this frontend") {
  return `<div class="stat-row"><span>${escapeHtml(label)}</span><strong>${escapeHtml(value)}</strong></div>`;
}

function renderPrimaryNavigation(route) {
  return PRIMARY_WORKSPACES.map((workspace) => {
    const active = workspace.id === route.workspaceId;
    const path = contextualRoute(workspace.path, route);
    return `
      <a class="primary-link ${active ? "is-active" : ""}" href="${escapeHtml(path)}" data-route="${escapeHtml(path)}" data-primary-workspace="${workspace.id}" aria-current="${active ? "page" : "false"}">
        <span class="primary-icon" aria-hidden="true">${workspace.icon}</span>
        <span>${escapeHtml(workspace.label)}</span>
        ${active ? '<span class="primary-active-mark" aria-hidden="true"></span>' : ""}
      </a>`;
  }).join("");
}

function renderSecondaryNavigation(route) {
  const workspace = workspaceForRoute(route);
  if (!workspace) return "";

  const links = workspace.states.map((state) => {
    const statePath = pathForState(workspace.id, state.id, route.strategyRef);
    const path = workspace.id === "strategies" && state.id !== "root"
      ? statePath
      : contextualRoute(statePath, route);
    const isSelected = route.stateId === state.id;
    const needsStrategy = workspace.id === "strategies" && state.segment && !route.strategyRef && !route.identityOnly;
    if (needsStrategy) {
      return `<button class="subnav-link is-disabled" type="button" disabled title="Select an exact strategy reference first">${escapeHtml(state.label)}</button>`;
    }
    return `<a class="subnav-link ${isSelected ? "is-active" : ""}" href="${escapeHtml(path)}" data-route="${escapeHtml(path)}" aria-current="${isSelected ? "page" : "false"}">${escapeHtml(state.label)}</a>`;
  }).join("");

  const contextualValidate = route.workspaceId === "strategies" && route.strategyRef
    ? `<span class="subnav-divider" aria-hidden="true"></span>${routeLink(contextualPath("/validate/run", route.strategyRef), "Test & Validate", "subnav-link subnav-action")}`
    : "";

  return `<nav class="secondary-nav" aria-label="${escapeHtml(workspace.label)} navigation"><span class="secondary-label">${escapeHtml(workspace.label)}</span>${links}${contextualValidate}</nav>`;
}

function renderHeader(route) {
  const workspace = workspaceForRoute(route);
  const strategyContext = route.strategyRef
    ? `<span class="context-separator">/</span><span class="context-strong">Requested ref ${escapeHtml(route.strategyRef)}</span>`
    : "";
  return `
    <header class="topbar">
      <div class="topbar-context">
        <p class="eyebrow">Current workspace</p>
        <div class="context-line"><strong>${escapeHtml(workspace?.label || "Cockpit")}</strong><span class="context-separator">/</span><span>${escapeHtml(route.label || "Cockpit Home")}</span>${strategyContext}</div>
      </div>
      <div class="topbar-status" aria-label="Runtime status">
        ${statusBadge("Market context pending")}
        ${statusBadge("Runtime status pending")}
        <button class="icon-button" type="button" disabled aria-label="Alerts status pending">♧</button>
        <button class="icon-button" type="button" disabled aria-label="Settings status pending">⚙</button>
        <span class="avatar" aria-hidden="true">TC</span>
      </div>
    </header>`;
}

function renderPageIntro(route, title, description, actions = "") {
  return `
    <div class="page-intro">
      <div>
        <p class="eyebrow">${escapeHtml(route.label)}</p>
        <h1>${escapeHtml(title)}</h1>
        <p class="lede">${escapeHtml(description)}</p>
      </div>
      ${actions ? `<div class="page-actions">${actions}</div>` : ""}
    </div>`;
}

function renderCockpitHome(route) {
  return `
    ${renderPageIntro(route, "A cockpit for what is actually known", "Orient from authoritative market, strategy, validation, and runtime state. This shell keeps every missing producer visible instead of filling the workspace with prototype values.", `${contextualRouteLink(route, "/strategies", "Open Strategies", "button button-primary")} ${contextualRouteLink(route, "/validate", "Open Test & Validate")}`)}
    <section class="hero-band" data-accent="purple">
      <div class="hero-copy">
        <span class="hero-kicker">TRADERCOCKPIT / ORIENTATION</span>
        <h2>Make the next decision from evidence.</h2>
        <p>Every surface below is ready for a real producer. Nothing here implies a live market, account, worker, or validation result until the source of truth is connected.</p>
        <div class="hero-actions">${contextualRouteLink(route, "/explore", "Explore capabilities", "button button-secondary")} ${contextualRouteLink(route, "/operate", "View operational state", "button button-quiet")}</div>
      </div>
      <div class="hero-orbit" aria-hidden="true"><span></span><span></span><span></span><b>TC</b></div>
    </section>
    <section class="dashboard-grid cockpit-grid">
      ${panel({ eyebrow: "Market context", title: "Market overview", description: "Symbol, timeframe, source, and session context belong to the market producer.", body: unavailableState("market"), accent: "green" })}
      ${panel({ eyebrow: "Strategy activity", title: "Recent strategy state", description: "Custody and strategy activity are shown only when the authoritative strategy source responds.", body: unavailableState("strategy"), accent: "purple" })}
      ${panel({ eyebrow: "Validation activity", title: "Run and validation pulse", description: "Validation lanes remain independent and producer-owned.", body: unavailableState("validation"), accent: "orange" })}
      ${panel({ eyebrow: "System attention", title: "Runtime attention", description: "The cockpit does not infer health from the presence of this UI.", body: unavailableState("run"), accent: "red" })}
      ${panel({ eyebrow: "Performance", title: "Performance summary", description: "Run-owned metrics and account-wide metrics must remain distinct.", body: unavailableState("validation"), accent: "cyan", className: "wide-panel" })}
      ${panel({ eyebrow: "Quick actions", title: "Go where the work belongs", description: "Each action opens an owning workspace.", body: `<div class="route-card-grid">${routeCard({ eyebrow: "Strategies", title: "Select a strategy", description: "Open the strategy library or provide an exact server reference.", path: contextualRoute("/strategies", route), accent: "purple" })}${routeCard({ eyebrow: "Explore", title: "Inspect capabilities", description: "Browse concepts, market context, and data requirements.", path: contextualRoute("/explore", route), accent: "green" })}${routeCard({ eyebrow: "Validate", title: "Set up a run", description: "Open the shared run-control surface.", path: contextualRoute("/validate/run", route), accent: "orange" })}</div>`, accent: "cyan", className: "wide-panel" })}
    </section>`;
}

function renderStrategiesRoot(route) {
  return `
    ${renderPageIntro(route, "Strategies", "Carry an opaque strategy reference through orientation, construction, signals, candidates, and evidence without treating it as backend identity.", contextualRouteLink(route, "/explore/catalog", "Browse catalog"))}
    ${route.strategyRef ? strategyContextHeader(route) : ""}
    <section class="strategy-select panel" data-accent="purple">
      <div class="panel-heading"><div><p class="eyebrow">Strategy reference</p><h2>Carry an exact strategy reference</h2></div>${statusBadge()}</div>
      <p class="panel-description">Enter an opaque reference for navigation. This input does not establish backend identity, create a durable strategy, or replace backend custody.</p>
      <form class="strategy-form" data-strategy-form>
        <label for="strategy-ref">Strategy reference</label>
        <div class="form-row"><input id="strategy-ref" name="strategyRef" type="text" autocomplete="off" placeholder="Paste an opaque strategy reference" /><button class="button button-primary" type="submit">Open reference</button></div>
        <p class="field-help">Display names and route references do not establish execution identity.</p>
      </form>
      <div class="select-divider"></div>
      ${unavailableState("strategy", "This frontend surface is not yet connected to the authoritative strategy producer. No local strategy container is fabricated.")}
    </section>
    <section class="dashboard-grid three-up">
      ${panel({ eyebrow: "Strategy reference", title: "Overview", description: "Orientation remains pending producer resolution of the requested reference.", body: disabledAction("Enter a reference", "Enter an exact strategy reference above"), accent: "purple" })}
      ${panel({ eyebrow: "Construction", title: "Build", description: "Resolve intent and policy before compute becomes eligible.", body: disabledAction("Open Build", "Select an exact strategy reference first"), accent: "orange" })}
      ${panel({ eyebrow: "Proof", title: "Evidence", description: "Strategy, run, provenance, and receipts stay linked.", body: disabledAction("Open Evidence", "Select an exact strategy reference first"), accent: "cyan" })}
    </section>`;
}

function strategyContextHeader(route) {
  return `<div class="context-callout"><span class="callout-icon">◇</span><div><span class="eyebrow">Requested strategy reference</span><strong data-requested-strategy-ref="${escapeHtml(route.strategyRef)}">${escapeHtml(route.strategyRef)}</strong><span>Opaque reference preserved for future authoritative producer resolution.</span></div><span class="context-lock">REQUESTED REFERENCE</span></div>`;
}

function strategyIdentityUnavailable(route) {
  return `${renderPageIntro(route, route.label, "This strategy child route is reachable, but it has no requested strategy reference to carry.", contextualRouteLink(route, "/strategies", "Select exact reference", "button button-primary"))}<div class="context-callout context-callout-unavailable"><span class="callout-icon">—</span><div><span class="eyebrow">Strategy reference not available to this frontend</span><strong>No requested strategy reference</strong><span>This state remains reference-only pending authoritative producer resolution. No default demo reference is substituted.</span></div><span class="context-lock">REFERENCE PENDING</span></div><section class="panel" data-accent="purple"><div class="panel-heading"><div><p class="eyebrow">${escapeHtml(route.label)}</p><h2>Awaiting producer resolution</h2></div>${statusBadge()}</div>${unavailableState("strategy", "This frontend surface is not yet connected to the authoritative strategy producer, so strategy-specific data and actions remain not available to this frontend.")}</section>`;
}

function renderStrategyState(route) {
  const ref = route.strategyRef;
  if (!ref) return route.identityOnly ? strategyIdentityUnavailable(route) : renderStrategiesRoot(route);

  const header = `${renderPageIntro(route, route.label, "Requested-reference context remains attached while the owning workspace changes.", `${routeLink(contextualPath("/validate/run", ref), "Test & Validate", "button button-primary")} ${routeLink("/strategies", "Change reference", "button button-quiet")}`)}${strategyContextHeader(route)}`;

  if (route.stateId === "overview") {
    return `${header}<section class="dashboard-grid three-up">${panel({ eyebrow: "Strategy reference", title: "Requested reference", description: "The opaque reference is carried in this route; backend identity is not established here.", body: `<div class="identity-value">${escapeHtml(ref)}</div>${statRow("Backend custody", "Not available to this frontend")}${statRow("Policy", "Not available to this frontend")}`, accent: "purple" })}${panel({ eyebrow: "Intent", title: "Hypothesis and purpose", description: "Intent is displayed only after the authoritative strategy producer resolves the reference.", body: unavailableState("strategy"), accent: "orange" })}${panel({ eyebrow: "Linked activity", title: "Runs and evidence", description: "Linked activity is not inferred from the reference string.", body: unavailableState("evidence"), accent: "cyan" })}</section><section class="dashboard-grid two-up">${panel({ eyebrow: "Next valid action", title: "Continue after producer resolution", description: "Move to construction only after an authoritative producer resolves this reference.", body: routeLink(`/strategies/${encodeURIComponent(ref)}/build`, "Open Build", "button button-primary"), accent: "green" })}${panel({ eyebrow: "Capability boundary", title: "No silent materialization", description: "Missing policy remains a visible gap.", body: unavailableState("strategy", "Strategy policy and linked run state are not available until this frontend is connected to the authoritative producer."), accent: "red" })}</section>`;
  }

  if (route.stateId === "build") {
    return `${header}<section class="dashboard-grid three-up">${panel({ eyebrow: "Intent", title: "What the reference may represent", description: "The normal view begins with intent, not a giant configuration wall.", body: unavailableState("strategy"), accent: "purple" })}${panel({ eyebrow: "Resolved rules", title: "Rules already known", description: "Resolved entry, exit, sizing, and policy semantics come from the authoritative strategy producer.", body: unavailableState("strategy"), accent: "orange" })}${panel({ eyebrow: "Next action", title: "Decision still unresolved", description: "The UI does not silently invent missing strategy policy.", body: disabledAction("Resolve policy"), accent: "red" })}</section><section class="split-surface">${panel({ eyebrow: "Progressive disclosure", title: "Expert controls", description: "Controls remain capability-bound until a producer publishes them.", body: `<div class="control-list">${disabledAction("Entry rules")}${disabledAction("Exit rules")}${disabledAction("Sizing")}${disabledAction("Costs and fills")}</div>`, accent: "cyan" })}${panel({ eyebrow: "Reference", title: "Requested strategy reference", description: "Construction does not promote this reference into backend execution identity.", body: `<div class="identity-value">${escapeHtml(ref)}</div>${statusBadge("Custody status pending")}`, accent: "purple" })}</section>`;
  }

  if (route.stateId === "signals") {
    return `${header}<section class="signals-layout"><div class="chart-panel panel" data-accent="green"><div class="panel-heading"><div><p class="eyebrow">Signals & Models</p><h2>Strategy chart</h2></div>${statusBadge()}</div><div class="chart-toolbar"><span>Bars not available to this frontend</span><span>Timeframe not available to this frontend</span><span>Source not available to this frontend</span>${disabledAction("Reset view")}</div>${chartEmpty("Strategy chart data not available to this frontend")}</div><div class="side-stack">${panel({ eyebrow: "Attached inputs", title: "Indicators and models", description: "Attached consumers are shown only after the strategy producer resolves the requested reference.", body: unavailableState("catalog"), accent: "purple" })}${panel({ eyebrow: "Signal state", title: "Confluence and market condition", description: "No signal or confluence values are fabricated.", body: unavailableState("market"), accent: "orange" })}</div></section><section class="dashboard-grid three-up">${panel({ eyebrow: "Observations", title: "Related observations", description: "Observations stay tied to the requested reference and market source.", body: unavailableState("strategy"), accent: "cyan" })}${panel({ eyebrow: "Chart controls", title: "Data requirements", description: "The chart consumer publishes its source and timeframe requirements.", body: unavailableState("market"), accent: "green" })}${panel({ eyebrow: "Reference", title: "Requested strategy reference", description: "This surface remains scoped to the opaque route reference.", body: `<div class="identity-value">${escapeHtml(ref)}</div>`, accent: "purple" })}</section>`;
  }

  if (route.stateId === "candidates") {
    return `${header}<section class="search-metrics"><div class="search-metric"><span>Search producer</span><strong>Pending</strong><small>Not connected to this frontend</small></div><div class="search-metric"><span>Search context</span><strong>Opaque route reference</strong><small>${escapeHtml(ref)}</small></div><div class="search-metric"><span>Validation status</span><strong>Separate lane</strong><small>Evolution score is not validation</small></div></section><section class="dashboard-grid three-up">${panel({ eyebrow: "Evolutionary Search", title: "Bounded search", description: "Population, generations, mutation, seed, and objectives require a real search producer.", body: `<div class="control-list">${disabledAction("Search budget")}${disabledAction("Deterministic seed")}${disabledAction("Start search")}</div>`, accent: "purple" })}${panel({ eyebrow: "Objective view", title: "Fitness evolution", description: "No prototype values are promoted into production state.", body: chartEmpty("Search progress not available to this frontend"), accent: "green" })}${panel({ eyebrow: "Frontier", title: "Pareto and diversity", description: "MAP-Elites and island controls remain capability-bound.", body: unavailableState("strategy", "Evolutionary search producer is not yet connected to this frontend; no candidate persistence is invented."), accent: "orange" })}</section><section class="panel" data-accent="cyan"><div class="panel-heading"><div><p class="eyebrow">Candidate results</p><h2>Candidate table</h2></div>${statusBadge()}</div>${unavailableState("strategy", "Candidate records are not published to this frontend yet.")}</section>`;
  }

  return `${header}<section class="dashboard-grid two-up">${panel({ eyebrow: "Evidence", title: "Strategy and run evidence", description: "Evidence, provenance, certification, proof, and receipts stay linked to their producer records.", body: unavailableState("evidence"), accent: "cyan" })}${panel({ eyebrow: "Custody", title: "Provenance chain", description: "No separate Governance destination is created for missing evidence.", body: unavailableState("evidence"), accent: "purple" })}</section>`;
}

function renderExploreRoot(route) {
  return `${renderPageIntro(route, "Explore", "Discover concepts, indicators, models, market information, and data requirements without exposing backend library architecture as the product mental model.", contextualRouteLink(route, "/explore/catalog", "Open Catalog", "button button-primary"))}<section class="dashboard-grid three-up">${routeCard({ eyebrow: "Catalog", title: "Indicators & Models", description: "Search, inspect requirements, and use or compare supported concepts.", path: contextualRoute("/explore/catalog", route), accent: "purple" })}${routeCard({ eyebrow: "Market Workspace", title: "Investigate the market", description: "Research market behavior independently of a requested strategy reference.", path: contextualRoute("/explore/market", route), accent: "green" })}${routeCard({ eyebrow: "Market Data", title: "Understand coverage", description: "Review source, timeframe, and provider/local availability.", path: contextualRoute("/explore/data", route), accent: "cyan" })}</section><section class="panel" data-accent="orange"><div class="panel-heading"><div><p class="eyebrow">Discovery boundary</p><h2>Explore is not a second strategy workspace</h2></div>${statusBadge()}</div><p class="panel-description">Market investigation belongs here. A requested reference’s chart, attached inputs, and signals belong under Strategies after producer resolution.</p>${unavailableState("catalog")}</section>`;
}

function renderCatalog(route) {
  return `${renderPageIntro(route, "Indicators & Models Catalog", "Search and inspect concepts through source-owned fields. No catalog entries are fabricated while the producer is not connected to this frontend.", contextualRouteLink(route, "/strategies", "Open Strategies"))}<section class="catalog-layout"><div class="catalog-list panel" data-accent="purple"><div class="panel-heading"><div><p class="eyebrow">Catalog</p><h2>Search and filter</h2></div>${statusBadge()}</div><div class="catalog-search"><input type="search" placeholder="Search catalog" disabled aria-label="Search catalog not available to this frontend" />${disabledAction("Filter", "Catalog producer is not yet connected to this frontend.")}</div>${unavailableState("catalog", "This frontend surface is not yet connected to the catalog producer. No catalog entries are fabricated.")}</div><div class="catalog-detail panel" data-accent="green"><div class="panel-heading"><div><p class="eyebrow">Details</p><h2>Select a result</h2></div>${statusBadge()}</div>${unavailableState("catalog", "Requirements, display geometry, compute availability, and source-specific fields are not available to this frontend until a result is selected.")}<div class="detail-actions">${disabledAction("Add to chart", "Select a catalog result first")}${disabledAction("Add to strategy", "Select a catalog result first")}</div></div></section>`;
}

function renderMarketWorkspace(route) {
  return `${renderPageIntro(route, "Market Workspace", "Investigate market behavior independently of a requested strategy reference. Data source and timeframe stay producer-owned.", contextualRouteLink(route, "/explore/data", "Review Market Data"))}<section class="market-layout"><div class="chart-panel panel" data-accent="green"><div class="panel-heading"><div><p class="eyebrow">Market investigation</p><h2>Market chart</h2></div>${statusBadge()}</div><div class="chart-toolbar"><span>Symbol not available to this frontend</span><span>Timeframe not available to this frontend</span><span>Source not available to this frontend</span>${disabledAction("Reset view")}</div>${chartEmpty("Market chart data not available to this frontend")}</div><div class="side-stack">${panel({ eyebrow: "Market state", title: "Context", description: "Session, condition, and source context are not inferred.", body: unavailableState("market"), accent: "orange" })}${panel({ eyebrow: "Observations", title: "Research notes", description: "Research output must be linked to an actual market context.", body: disabledAction("Add observation"), accent: "purple" })}</div></section>`;
}

function renderMarketData(route) {
  return `${renderPageIntro(route, "Market Data", "Understand available market data, source, coverage, timeframe capability, and provider/local availability.", contextualRouteLink(route, "/explore/market", "Open Market Workspace"))}<section class="dashboard-grid two-up">${panel({ eyebrow: "DataLakePanel", title: "Coverage and sources", description: "The existing data lake consumer belongs in Explore / Market Data.", body: unavailableState("market", "This frontend surface is not yet connected to the DataLakePanel producer; no coverage rows are invented."), accent: "cyan" })}${panel({ eyebrow: "Requirements", title: "Capability matrix", description: "Source-specific requirements remain source-specific.", body: unavailableState("market"), accent: "purple" })}</section>`;
}

function renderValidationRoot(route) {
  return `${renderPageIntro(route, "Test & Validate", "Answer whether a producer-resolved strategy or run deserves trust. Initial, Fast, and Golden remain peer lanes; Prop Simulation is optional.", contextualRouteLink(route, "/validate/run", "Open Run Setup", "button button-primary"))}${route.strategyRef ? strategyContextHeader(route) : ""}<section class="validation-summary panel" data-accent="orange"><div class="panel-heading"><div><p class="eyebrow">Validation overview</p><h2>Latest outcomes</h2></div>${statusBadge()}</div><div class="validation-lanes"><div class="lane"><span class="lane-index">01</span><strong>Initial</strong><span>Independent lane</span>${statusBadge()}</div><div class="lane"><span class="lane-index">02</span><strong>Fast</strong><span>Independent lane</span>${statusBadge()}</div><div class="lane"><span class="lane-index">03</span><strong>Golden</strong><span>Independent lane</span>${statusBadge()}</div></div>${unavailableState("validation", "No producer-resolved strategy or run outcome is connected to this frontend.")}</section><section class="dashboard-grid three-up">${routeCard({ eyebrow: "Run Setup", title: "Configure a run", description: "Use the shared RunSurface for profile, plan, timeframe, and lifecycle.", path: contextualRoute("/validate/run", route), accent: "orange" })}${routeCard({ eyebrow: "Results", title: "Read outcomes", description: "Open validation results and evidence-linked metrics.", path: contextualRoute("/validate/results", route), accent: "green" })}${routeCard({ eyebrow: "Stress & Robustness", title: "Test stability", description: "Review supported stress, OOS, walk-forward, and Monte Carlo outputs.", path: contextualRoute("/validate/stress", route), accent: "purple" })}</section><section class="dashboard-grid three-up">${routeCard({ eyebrow: "Compare", title: "Compare results", description: "Compare compatible completed results when producer data permits.", path: contextualRoute("/validate/compare", route), accent: "cyan" })}${routeCard({ eyebrow: "Prop Simulation", title: "Simulate a rule set", description: "Optional validation against a selected account/rule set.", path: contextualRoute("/validate/prop", route), accent: "red" })}${panel({ eyebrow: "Capability boundary", title: "No mandatory funnel", description: "The UI does not impose Initial → Fast → Golden → Prop.", body: unavailableState("validation", "The frontend is not yet connected to the backend-owned plan and lane producer."), accent: "orange" })}</section>`;
}

function renderRunSurface(contextLabel, strategyRef = "") {
  const requestedStrategy = strategyRef
    ? `<strong data-requested-strategy-ref="${escapeHtml(strategyRef)}">${escapeHtml(strategyRef)}</strong>`
    : "<strong>Not available to this frontend</strong>";
  return `<section class="run-surface panel" data-run-surface-id="${RUN_SURFACE_ID}" data-run-context-owner="${RUN_CONTEXT_OWNER}" data-run-surface-implementation="${RUN_SURFACE_ID}" data-accent="orange"><div class="panel-heading"><div><p class="eyebrow">${escapeHtml(contextLabel)}</p><h2>Shared RunSurface</h2></div>${statusBadge()}</div><p class="panel-description">Test & Validate and Operate share the same run lifecycle surface. This view does not create a second poller or control implementation.</p><div class="run-fields"><div class="run-field"><span>Profile selection</span><strong>Not available to this frontend</strong></div><div class="run-field"><span>Pipeline / phase plan</span><strong>Not available to this frontend</strong></div><div class="run-field"><span>Timeframe</span><strong>Not available to this frontend</strong></div><div class="run-field"><span>Requested strategy reference</span>${requestedStrategy}</div></div><div class="run-footer"><span class="run-refusal">${escapeHtml(UNAVAILABLE_REASON)}</span>${disabledAction("Start run")}${disabledAction("Cancel run", "Run status is not available to this frontend.")}</div></section>`;
}

function renderValidationState(route) {
  if (route.stateId === "run") return `${renderPageIntro(route, "Run Setup", "Configure and start a run through the existing lifecycle source. Refusal and cancellation remain producer-owned.", route.strategyRef ? routeLink(`/strategies/${encodeURIComponent(route.strategyRef)}/overview`, "Return to strategy") : routeLink("/strategies", "Select a strategy"))}${route.strategyRef ? strategyContextHeader(route) : ""}${renderRunSurface("Test & Validate / Run Setup", route.strategyRef)}`;
  if (route.stateId === "results") return `${renderPageIntro(route, "Results", "Read backtest and validation outcomes from a producer-resolved strategy/run and their evidence.", contextualRouteLink(route, "/validate/run", "Run Setup"))}${route.strategyRef ? strategyContextHeader(route) : ""}<section class="dashboard-grid two-up">${panel({ eyebrow: "StrategyValidationOverview", title: "Validation results", description: "Trade summaries and evidence-linked metrics come from the result reader.", body: unavailableState("validation"), accent: "green" })}${panel({ eyebrow: "Result identity", title: "Exact run context", description: "No run identity is inferred from route labels.", body: unavailableState("run"), accent: "purple" })}</section>`;
  if (route.stateId === "stress") return `${renderPageIntro(route, "Stress & Robustness", "Review supported Monte Carlo, stress, stability, OOS, walk-forward, and sensitivity outputs only when their producers respond.", contextualRouteLink(route, "/validate/results", "Open Results"))}${route.strategyRef ? strategyContextHeader(route) : ""}<section class="dashboard-grid two-up">${panel({ eyebrow: "MonteCarloAnalysis", title: "Robustness readers", description: "Monte Carlo and other supported robustness readers remain separate from validation status.", body: unavailableState("robustness"), accent: "purple" })}${panel({ eyebrow: "Stability", title: "Stress and OOS", description: "Tests without a producer response render as honest gaps.", body: unavailableState("robustness"), accent: "orange" })}</section>`;
  if (route.stateId === "compare") return `${renderPageIntro(route, "Compare", "Compare compatible completed results without inventing a comparison engine or cross-run values.", contextualRouteLink(route, "/validate/results", "Open Results"))}${route.strategyRef ? strategyContextHeader(route) : ""}<section class="panel" data-accent="cyan"><div class="panel-heading"><div><p class="eyebrow">Read-only comparison</p><h2>Compatible result set</h2></div>${statusBadge()}</div>${unavailableState("validation", "A truthful comparison is pending a producer response with completed result identity.")}${disabledAction("Compare selected results", "Compatible completed results are not available to this frontend.")}</section>`;
  return `${renderPageIntro(route, "Prop Simulation", "Validate an eligible strategy, candidate, or champion against a selected account/rule set. This lane is optional and not automatically unlocked by Golden.", contextualRouteLink(route, "/validate", "Validation overview"))}${route.strategyRef ? strategyContextHeader(route) : ""}<section class="dashboard-grid two-up">${panel({ eyebrow: "PropRuleFitAssistant", title: "Rule-set selection", description: "Rule and account evidence remain source-owned.", body: unavailableState("prop"), accent: "red" })}${panel({ eyebrow: "Simulation", title: "Eligibility and outcomes", description: "No prop-firm rules, account limits, or pass/fail values are fabricated.", body: unavailableState("prop"), accent: "orange" })}</section>`;
}

function renderOperateRoot(route) {
  return `${renderPageIntro(route, "Operate", "Observe what is actually running or producing operational state now. This workspace does not imply broker deployment or live trading.", contextualRouteLink(route, "/operate/runs", "View Runs", "button button-primary"))}${route.strategyRef ? strategyContextHeader(route) : ""}<section class="dashboard-grid three-up">${routeCard({ eyebrow: "Runs", title: "Running work", description: "Observe and manage the same shared run lifecycle surface used by validation.", path: contextualRoute("/operate/runs", route), accent: "orange" })}${routeCard({ eyebrow: "Performance", title: "Run performance", description: "Show only the scope the current producer supports.", path: contextualRoute("/operate/performance", route), accent: "green" })}${routeCard({ eyebrow: "Execution & Risk", title: "Operational capability", description: "Broker, portfolio, and risk state remain capability-bound.", path: contextualRoute("/operate/execution-risk", route), accent: "red" })}</section><section class="panel" data-accent="red"><div class="panel-heading"><div><p class="eyebrow">Conservative operational surface</p><h2>No implied live account</h2></div>${statusBadge()}</div><p class="panel-description">Backtest or research activity is not called live trading. Open positions, orders, exposure, VaR, approvals, and deployment state require real producers.</p>${unavailableState("execution")}</section>`;
}

function renderOperateState(route) {
  if (route.stateId === "runs") return `${renderPageIntro(route, "Runs", "Observe and manage running work through the same shared lifecycle surface used by Test & Validate.", contextualRouteLink(route, "/validate/run", "Configure in Test & Validate"))}${route.strategyRef ? strategyContextHeader(route) : ""}${renderRunSurface("Operate / Runs", route.strategyRef)}`;
  if (route.stateId === "performance") return `${renderPageIntro(route, "Performance", "Present the scope supported by the current producer. Run-owned performance is not upgraded into live account performance by presentation.", contextualRouteLink(route, "/operate/runs", "View Runs"))}${route.strategyRef ? strategyContextHeader(route) : ""}<section class="dashboard-grid two-up">${panel({ eyebrow: "Performance", title: "Run-owned performance", description: "Account-wide or live metrics are not inferred.", body: unavailableState("validation"), accent: "green" })}${panel({ eyebrow: "Scope", title: "Metric applicability", description: "The result reader publishes the scope and data window for each metric.", body: unavailableState("validation"), accent: "cyan" })}</section>`;
  return `${renderPageIntro(route, "Execution & Risk", "Keep operational concepts visibly pending until real producers respond.", contextualRouteLink(route, "/operate", "Operate overview"))}${route.strategyRef ? strategyContextHeader(route) : ""}<section class="dashboard-grid three-up">${panel({ eyebrow: "Execution", title: "Orders and positions", description: "Broker execution state is not available to this frontend.", body: unavailableState("execution"), accent: "red" })}${panel({ eyebrow: "Risk", title: "Exposure and limits", description: "Portfolio exposure, VaR, and daily loss usage are not fabricated.", body: unavailableState("execution"), accent: "orange" })}${panel({ eyebrow: "Approval", title: "Deployment state", description: "No broker or deployment authority is implied.", body: unavailableState("execution"), accent: "purple" })}</section>`;
}

function renderContent(route) {
  if (route.workspaceId === "cockpit") return renderCockpitHome(route);
  if (route.workspaceId === "strategies") return route.stateId === "root" ? renderStrategiesRoot(route) : renderStrategyState(route);
  if (route.workspaceId === "explore") {
    if (route.stateId === "root") return renderExploreRoot(route);
    if (route.stateId === "catalog") return renderCatalog(route);
    if (route.stateId === "market") return renderMarketWorkspace(route);
    return renderMarketData(route);
  }
  if (route.workspaceId === "validate") return route.stateId === "root" ? renderValidationRoot(route) : renderValidationState(route);
  return route.stateId === "root" ? renderOperateRoot(route) : renderOperateState(route);
}

function renderApollo() {
  return `
    <section class="apollo-dock" id="${APOLLO_SURFACE_ID}" data-apollo-surface data-apollo-surface-id="${APOLLO_SURFACE_ID}" aria-label="Apollo assistant">
      <div class="apollo-brand"><span class="apollo-orb">✦</span><div><strong>Apollo</strong><span>Context-aware assistant surface</span></div></div>
      <div class="apollo-status">${statusBadge("Runtime status pending")}</div>
      <form class="apollo-form" data-apollo-form>
        <input type="text" disabled placeholder="Apollo is inactive while runtime connection is pending" aria-label="Apollo message input pending runtime connection" />
        <button class="apollo-send" type="submit" disabled aria-label="Send Apollo message">➤</button>
      </form>
      <span class="apollo-hint">One persistent surface · no autonomous actions</span>
    </section>`;
}

export function renderApp(location = { pathname: "/cockpit", search: "" }) {
  const requestedRoute = resolveRoute(location.pathname, location.search);
  const route = requestedRoute.kind === "redirect"
    ? (() => {
        const canonical = new URL(requestedRoute.redirectPath, "http://localhost");
        return resolveRoute(canonical.pathname, canonical.search);
      })()
    : requestedRoute;
  const workspace = workspaceForRoute(route);
  return `
    <div class="app-shell" data-workspace="${escapeHtml(route.workspaceId)}" data-state="${escapeHtml(route.stateId)}" data-state-key="${escapeHtml(route.stateKey)}" data-canonical-route="${escapeHtml(route.path)}" data-requested-strategy-ref="${escapeHtml(route.strategyRef)}" data-identity-only="${route.identityOnly ? "true" : "false"}" data-logical-state-count="${LOGICAL_STATES.length}">
      <aside class="rail">
        <div class="brand"><span class="brand-mark">ESQ</span><span class="brand-name">TraderCockpit</span></div>
        <div class="rail-context"><span class="context-pulse"></span><span>Source-bound shell</span></div>
        <nav class="primary-nav" data-primary-navigation aria-label="Primary workspace navigation">${renderPrimaryNavigation(route)}</nav>
        <div class="rail-footer"><div class="rail-footer-line">${statusBadge("Producer integration pending")}</div><div class="rail-footer-meta">Five workspaces · ${LOGICAL_STATES.length} states</div></div>
      </aside>
      <main class="main-shell">
        ${renderHeader(route)}
        ${renderSecondaryNavigation(route)}
        <div class="content-scroll"><div class="content-inner">${renderContent(route)}</div></div>
        ${renderApollo()}
      </main>
    </div>`;
}

export function boot(root = document.querySelector("#app")) {
  if (!root) throw new Error("TraderCockpit shell root not found");

  let persistentApollo = null;
  let apolloMountCount = 0;

  const render = () => {
    const current = resolveRoute(window.location.pathname, window.location.search);
    if (current.kind === "redirect") {
      window.history.replaceState({}, "", current.redirectPath);
    }

    const template = document.createElement("template");
    template.innerHTML = renderApp(window.location);
    const nextApollo = template.content.querySelector(`[data-apollo-surface]`);
    if (!nextApollo) throw new Error("Apollo surface missing from shell render");

    if (!persistentApollo) {
      persistentApollo = nextApollo;
      apolloMountCount += 1;
      persistentApollo.setAttribute("data-apollo-instance", String(apolloMountCount));
    } else {
      nextApollo.replaceWith(persistentApollo);
    }

    root.replaceChildren(template.content);
    root.dataset.apolloMountCount = String(apolloMountCount);
    root.dataset.apolloSurfaceId = APOLLO_SURFACE_ID;
  };

  const navigate = (path) => {
    window.history.pushState({}, "", path);
    render();
    document.querySelector(".content-scroll")?.scrollTo({ top: 0, behavior: "instant" });
  };

  root.addEventListener("click", (event) => {
    const link = event.target.closest("[data-route]");
    if (!link || link.matches(":disabled")) return;
    event.preventDefault();
    navigate(link.dataset.route);
  });

  root.addEventListener("submit", (event) => {
    const form = event.target.closest("[data-strategy-form]");
    if (!form) return;
    event.preventDefault();
    const value = new FormData(form).get("strategyRef")?.toString() ?? "";
    if (value.length === 0) return;
    navigate(`/strategies/${encodeURIComponent(value)}/overview`);
  });

  window.addEventListener("popstate", render);
  render();
}

if (typeof document !== "undefined") boot();
