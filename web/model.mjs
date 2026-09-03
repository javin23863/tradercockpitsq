// Product structure model. Six top-level surfaces, one Research surface composed of
// four dense workspaces, and the live/current Cockpit Home zones. Routes select only
// registered states; arbitrary query text never creates a product state.

export const APP_SURFACES = Object.freeze([
  Object.freeze({ id: "home", label: "Home", path: "/home", icon: "home" }),
  Object.freeze({ id: "research", label: "Research", path: "/research", icon: "research" }),
  Object.freeze({ id: "explore", label: "Explore", path: "/explore", icon: "explore" }),
  Object.freeze({ id: "automation", label: "Automation", path: "/automation", icon: "automation" }),
  Object.freeze({ id: "operate", label: "Operate", path: "/operate", icon: "operate" }),
  Object.freeze({ id: "settings", label: "Settings", path: "/settings", icon: "settings" }),
]);

function tabs(list) {
  return Object.freeze(list.map(([id, label]) => Object.freeze({ id, label })));
}

// Research workspaces = the four Research prototype screens. Tabs are the exact tab rows
// shown in those screens; the custody chain (Idea → Specification → Build → Candidates →
// Backtest → Robustness → Proof) is folded into them rather than condensed away.
export const RESEARCH_WORKSPACES = Object.freeze([
  Object.freeze({
    id: "signals",
    label: "Signals & Models",
    title: "Order Flow Signals & Models",
    screen: "order-flow-signals-models",
    tabs: tabs([
      ["overview", "Overview"],
      ["signals", "Signals & Models"],
      ["order-flow", "Order Flow"],
      ["footprint", "Footprint"],
      ["volume-profile", "Volume Profile"],
      ["liquidity-map", "Liquidity Map"],
      ["replays", "Replays"],
      ["alerts", "Alerts"],
      ["reports", "Reports"],
    ]),
  }),
  Object.freeze({
    id: "evolution",
    label: "Evolutionary Search",
    title: "Evolutionary Search",
    screen: "evolutionary_search_trading_dashboard",
    tabs: tabs([]),
  }),
  Object.freeze({
    id: "validate",
    label: "Test & Validate",
    title: "Test & Validate",
    screen: "test-validate-dashboard",
    tabs: tabs([
      ["overview", "Overview"],
      ["initial-test", "Initial Test"],
      ["trades", "Trades"],
      ["robustness", "Robustness"],
      ["configuration", "Configuration"],
      ["evidence", "Evidence"],
    ]),
  }),
  Object.freeze({
    id: "catalog",
    label: "Indicators & Models",
    title: "Indicators & Models Catalog",
    screen: "indicators-models-catalog",
    tabs: tabs([
      ["all", "All Components"],
      ["indicators", "Indicators"],
      ["models", "Models"],
      ["strategies", "Strategies"],
      ["utilities", "Utilities"],
      ["mine", "My Components"],
    ]),
  }),
]);

// Cockpit Home: the eight live/current zones. Card titles in cockpit-home.png are
// illustrative framing; the product Home contract is these zones plus persistent Apollo.
export const HOME_ZONES = Object.freeze([
  Object.freeze({ id: "market-overview", number: 1, label: "Market Overview", sub: "Current watchlist and live context", accent: "green" }),
  Object.freeze({ id: "system-status", number: 2, label: "System Status", sub: "Engine and component readiness", accent: "red" }),
  Object.freeze({ id: "alpha-stack", number: 3, label: "Alpha Stack", sub: "Strategy and deployment identities", accent: "purple" }),
  Object.freeze({ id: "pipeline-overview", number: 4, label: "Pipeline Overview", sub: "Research through deployment lifecycle", accent: "orange" }),
  Object.freeze({ id: "signals", number: 5, label: "Signals", sub: "Live signal and confluence pulse", accent: "cyan" }),
  Object.freeze({ id: "risk", number: 6, label: "Risk", sub: "Current exposure and account risk", accent: "red" }),
  Object.freeze({ id: "performance", number: 7, label: "Performance", sub: "Live / current scoped results", accent: "green" }),
  Object.freeze({ id: "quick-actions", number: 8, label: "Quick Actions", sub: "Go where the work belongs", accent: "cyan" }),
]);
export const HOME_ZONE_IDS = Object.freeze(HOME_ZONES.map((zone) => zone.id));

export const RESEARCH_WORKSPACE_IDS = Object.freeze(RESEARCH_WORKSPACES.map((workspace) => workspace.id));
export const PRODUCT_ROUTE_PATHS = Object.freeze(APP_SURFACES.map((surface) => surface.path));

const surfaceByPath = new Map(APP_SURFACES.map((surface) => [surface.path, surface]));
const workspaceById = new Map(RESEARCH_WORKSPACES.map((workspace) => [workspace.id, workspace]));

// Pre-prototype routes (`stage`/`tab`) resolve to their prototype workspace/tab so saved
// links, tests, and binder predicates keep working; the app canonicalises the URL.
const LEGACY_RESEARCH_ROUTES = Object.freeze({
  "construct/idea": ["signals", "overview"],
  "construct/specification": ["signals", "signals"],
  "construct/build": ["evolution", ""],
  "construct/candidates": ["evolution", ""],
  "backtest/overview": ["validate", "initial-test"],
  "backtest/trades": ["validate", "trades"],
  "backtest/robustness": ["validate", "robustness"],
  "backtest/configuration": ["validate", "configuration"],
  "proof/": ["validate", "evidence"],
});

export function normalizePath(pathname = "/home") {
  const raw = String(pathname || "/home").split("?")[0];
  if (raw === "/") return "/";
  return raw.replace(/\/+$/, "") || "/home";
}

export function researchWorkspace(workspaceId) {
  return workspaceById.get(workspaceId) || null;
}

const RESEARCH_STRUCTURAL_KEYS = Object.freeze(["workspace", "tab", "stage"]);

export function researchPath(workspaceId = "signals", tabId = "", identitySearch = "") {
  const workspace = researchWorkspace(workspaceId) || RESEARCH_WORKSPACES[0];
  const params = new URLSearchParams();
  params.set("workspace", workspace.id);
  if (workspace.tabs.length > 0) {
    const tab = workspace.tabs.find((candidate) => candidate.id === tabId) || workspace.tabs[0];
    params.set("tab", tab.id);
  }
  const source = new URLSearchParams(typeof identitySearch === "string" ? identitySearch : identitySearch?.search || "");
  for (const [key, value] of source.entries()) {
    if (RESEARCH_STRUCTURAL_KEYS.includes(key) || !value) continue;
    params.set(key, value);
  }
  return `/research?${params.toString()}`;
}

export function researchNavPath(workspaceId = "signals", tabId = "") {
  return researchPath(workspaceId, tabId, globalThis.location?.search || "");
}

function selectedResearchState(params) {
  let requestedWorkspace = params.get("workspace");
  let requestedTab = params.get("tab");
  let legacy = false;
  if (!requestedWorkspace && (params.has("stage") || (requestedTab && !params.has("workspace")))) {
    const stage = params.get("stage") || "construct";
    const key = `${stage}/${stage === "proof" ? "" : (requestedTab || "")}`;
    const mapped = LEGACY_RESEARCH_ROUTES[key] || LEGACY_RESEARCH_ROUTES[`${stage}/${defaultLegacyTab(stage)}`];
    if (mapped) {
      [requestedWorkspace, requestedTab] = mapped;
      legacy = true;
    }
  }
  const workspace = researchWorkspace(requestedWorkspace || "") || RESEARCH_WORKSPACES[0];
  let tab = null;
  if (workspace.tabs.length > 0) {
    tab = workspace.tabs.find((candidate) => candidate.id === (requestedTab || workspace.tabs[0].id)) || workspace.tabs[0];
  }
  return { workspace, tab, legacy };
}

function defaultLegacyTab(stage) {
  if (stage === "construct") return "idea";
  if (stage === "backtest") return "overview";
  return "";
}

function resolveResearch(search = "") {
  const params = new URLSearchParams(search);
  const { workspace, tab, legacy } = selectedResearchState(params);
  return {
    kind: "research",
    surfaceId: "research",
    label: "Research",
    path: "/research",
    workspaceId: workspace.id,
    workspaceLabel: workspace.label,
    workspaceTitle: workspace.title,
    tabId: tab?.id || null,
    tabLabel: tab?.label || null,
    canonicalPath: researchPath(workspace.id, tab?.id || "", search),
    legacy,
  };
}

export function resolveRoute(pathname = "/home", search = "") {
  const path = normalizePath(pathname);
  if (path === "/") {
    return { kind: "redirect", redirectPath: "/home", path };
  }

  if (path === "/research") return resolveResearch(search);

  const surface = surfaceByPath.get(path);
  if (surface) {
    return {
      kind: "surface",
      surfaceId: surface.id,
      label: surface.label,
      path: surface.path,
    };
  }

  return {
    kind: "surface",
    surfaceId: "home",
    label: "Home",
    path: "/home",
    unknownPath: path,
  };
}

// Shared predicate for the read-model binder modules: does this location select the given
// Research workspace (and tab, when the workspace has tabs)? Accepts canonical and legacy
// query forms so existing links keep binding.
export function researchLocationMatches(locationLike, workspaceId, tabId = null) {
  if (!locationLike || normalizePath(locationLike.pathname || "") !== "/research") return false;
  const route = resolveResearch(locationLike.search || "");
  if (route.workspaceId !== workspaceId) return false;
  if (tabId === null || tabId === undefined) return true;
  return route.tabId === tabId;
}
