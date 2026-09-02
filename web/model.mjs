// Product structure model. Mirrors the five accepted prototype screens in
// references/ui-authority/screenshots: six top-level surfaces, one Research surface
// composed of four dense workspaces, and the Cockpit Home board. Routes select only
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

// Cockpit Home board: hero + Recent Activity + these eight numbered cards, exactly as the
// cockpit-home prototype screen lays them out.
export const HOME_ZONES = Object.freeze([
  Object.freeze({ id: "research", number: 1, label: "Research", sub: "Active ideas & insights", accent: "purple" }),
  Object.freeze({ id: "build-backtest", number: 2, label: "Build & Backtest", sub: "Build, test and refine systems", accent: "blue" }),
  Object.freeze({ id: "prop-simulation", number: 3, label: "Prop Firm Simulation", sub: "Simulated accounts & challenges", accent: "green" }),
  Object.freeze({ id: "proof-evidence", number: 4, label: "Proof & Evidence", sub: "Documentation & validation", accent: "violet" }),
  Object.freeze({ id: "active-builds", number: 5, label: "Active Builds", sub: "Systems in development", accent: "orange" }),
  Object.freeze({ id: "candidate-review", number: 6, label: "Candidate Review", sub: "Top candidates for promotion", accent: "green" }),
  Object.freeze({ id: "system-health", number: 7, label: "System Health", sub: "Monitor & infrastructure", accent: "blue" }),
  Object.freeze({ id: "assistant", number: 8, label: "Assistant", sub: "Your trading copilot", accent: "purple" }),
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
