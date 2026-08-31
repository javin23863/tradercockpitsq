export const APP_SURFACES = Object.freeze([
  Object.freeze({ id: "home", label: "Home", path: "/home", icon: "⌂" }),
  Object.freeze({ id: "research", label: "Research", path: "/research", icon: "◇" }),
  Object.freeze({ id: "explore", label: "Explore", path: "/explore", icon: "⌕" }),
  Object.freeze({ id: "automation", label: "Automation", path: "/automation", icon: "↻" }),
  Object.freeze({ id: "operate", label: "Operate", path: "/operate", icon: "◉" }),
  Object.freeze({ id: "settings", label: "Settings", path: "/settings", icon: "⚙" }),
]);

export const RESEARCH_STAGES = Object.freeze([
  Object.freeze({
    id: "construct",
    label: "Construct",
    tabs: Object.freeze([
      Object.freeze({ id: "idea", label: "Idea" }),
      Object.freeze({ id: "specification", label: "Specification" }),
      Object.freeze({ id: "build", label: "Build" }),
      Object.freeze({ id: "candidates", label: "Candidates" }),
    ]),
  }),
  Object.freeze({
    id: "backtest",
    label: "Backtest",
    tabs: Object.freeze([
      Object.freeze({ id: "overview", label: "Overview" }),
      Object.freeze({ id: "trades", label: "Trades" }),
      Object.freeze({ id: "robustness", label: "Robustness" }),
      Object.freeze({ id: "configuration", label: "Configuration" }),
    ]),
  }),
  Object.freeze({ id: "proof", label: "Proof", tabs: Object.freeze([]) }),
]);

export const HOME_ZONE_IDS = Object.freeze([
  "market-overview",
  "system-status",
  "alpha-stack",
  "pipeline-overview",
  "signals",
  "risk",
  "performance",
  "quick-actions",
]);

export const RESEARCH_STAGE_IDS = Object.freeze(RESEARCH_STAGES.map((stage) => stage.id));
export const CONSTRUCT_TAB_IDS = Object.freeze(RESEARCH_STAGES[0].tabs.map((tab) => tab.id));
export const BACKTEST_TAB_IDS = Object.freeze(RESEARCH_STAGES[1].tabs.map((tab) => tab.id));
export const PRODUCT_ROUTE_PATHS = Object.freeze(APP_SURFACES.map((surface) => surface.path));

const surfaceByPath = new Map(APP_SURFACES.map((surface) => [surface.path, surface]));
const stageById = new Map(RESEARCH_STAGES.map((stage) => [stage.id, stage]));

export function normalizePath(pathname = "/home") {
  const raw = String(pathname || "/home").split("?")[0];
  if (raw === "/") return "/";
  return raw.replace(/\/+$/, "") || "/home";
}

export function researchStage(stageId) {
  return stageById.get(stageId) || null;
}

export function researchPath(stageId = "construct", tabId = "") {
  const stage = researchStage(stageId) || RESEARCH_STAGES[0];
  const params = new URLSearchParams();
  params.set("stage", stage.id);
  if (stage.tabs.length > 0) {
    const tab = stage.tabs.find((candidate) => candidate.id === tabId) || stage.tabs[0];
    params.set("tab", tab.id);
  }
  return `/research?${params.toString()}`;
}

function resolveResearch(search = "") {
  const params = new URLSearchParams(search);
  const requestedStage = params.get("stage") || "construct";
  const stage = researchStage(requestedStage) || RESEARCH_STAGES[0];
  let tab = null;
  if (stage.tabs.length > 0) {
    const requestedTab = params.get("tab") || stage.tabs[0].id;
    tab = stage.tabs.find((candidate) => candidate.id === requestedTab) || stage.tabs[0];
  }
  return {
    kind: "research",
    surfaceId: "research",
    label: "Research",
    path: "/research",
    researchStageId: stage.id,
    researchStageLabel: stage.label,
    researchTabId: tab?.id || null,
    researchTabLabel: tab?.label || null,
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
