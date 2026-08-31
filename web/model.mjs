export const CORE_STAGES = Object.freeze([
  Object.freeze({
    id: "construct",
    label: "Construct",
    path: "/construct/idea",
    icon: "◇",
    tabs: Object.freeze([
      Object.freeze({ id: "idea", label: "Idea", path: "/construct/idea" }),
      Object.freeze({ id: "specification", label: "Specification", path: "/construct/specification" }),
      Object.freeze({ id: "build", label: "Build", path: "/construct/build" }),
      Object.freeze({ id: "candidates", label: "Candidates", path: "/construct/candidates" }),
    ]),
  }),
  Object.freeze({
    id: "backtest",
    label: "Backtest",
    path: "/backtest/overview",
    icon: "▥",
    tabs: Object.freeze([
      Object.freeze({ id: "overview", label: "Overview", path: "/backtest/overview" }),
      Object.freeze({ id: "trades", label: "Trades", path: "/backtest/trades" }),
      Object.freeze({ id: "robustness", label: "Robustness", path: "/backtest/robustness" }),
      Object.freeze({ id: "configuration", label: "Configuration", path: "/backtest/configuration" }),
    ]),
  }),
  Object.freeze({
    id: "proof",
    label: "Proof",
    path: "/proof",
    icon: "✓",
    tabs: Object.freeze([]),
  }),
]);

export const AUXILIARY_SURFACES = Object.freeze([
  Object.freeze({ id: "home", label: "Home", path: "/home", icon: "⌂" }),
  Object.freeze({ id: "explore", label: "Explore", path: "/explore", icon: "⌕" }),
  Object.freeze({ id: "automation", label: "Automation", path: "/automation", icon: "↻" }),
  Object.freeze({ id: "operate", label: "Operate", path: "/operate", icon: "◉" }),
  Object.freeze({ id: "settings", label: "Settings", path: "/settings", icon: "⚙" }),
]);

export const CORE_STAGE_IDS = Object.freeze(CORE_STAGES.map((stage) => stage.id));
export const CONSTRUCT_TAB_IDS = Object.freeze(CORE_STAGES[0].tabs.map((tab) => tab.id));
export const BACKTEST_TAB_IDS = Object.freeze(CORE_STAGES[1].tabs.map((tab) => tab.id));

const auxiliaryByPath = new Map(AUXILIARY_SURFACES.map((surface) => [surface.path, surface]));
const stageById = new Map(CORE_STAGES.map((stage) => [stage.id, stage]));
const tabByPath = new Map(
  CORE_STAGES.flatMap((stage) => stage.tabs.map((tab) => [tab.path, { stage, tab }])),
);

export function normalizePath(pathname = "/home") {
  const raw = String(pathname || "/home").split("?")[0];
  if (raw === "/") return "/";
  return raw.replace(/\/+$/, "") || "/home";
}

export function stageForRoute(route) {
  return route?.stageId ? stageById.get(route.stageId) || null : null;
}

export function resolveRoute(pathname = "/home") {
  const path = normalizePath(pathname);

  if (path === "/") {
    return { kind: "redirect", redirectPath: "/home", path };
  }
  if (path === "/construct") {
    return { kind: "redirect", redirectPath: "/construct/idea", path };
  }
  if (path === "/backtest") {
    return { kind: "redirect", redirectPath: "/backtest/overview", path };
  }

  const auxiliary = auxiliaryByPath.get(path);
  if (auxiliary) {
    return {
      kind: "auxiliary",
      surfaceId: auxiliary.id,
      label: auxiliary.label,
      path: auxiliary.path,
    };
  }

  if (path === "/proof") {
    return {
      kind: "stage",
      stageId: "proof",
      tabId: null,
      label: "Proof",
      path,
    };
  }

  const tabRecord = tabByPath.get(path);
  if (tabRecord) {
    return {
      kind: "stage",
      stageId: tabRecord.stage.id,
      tabId: tabRecord.tab.id,
      label: tabRecord.tab.label,
      path,
    };
  }

  return {
    kind: "auxiliary",
    surfaceId: "home",
    label: "Home",
    path: "/home",
    unknownPath: path,
  };
}

export function pathForStage(stageId) {
  return stageById.get(stageId)?.path || "/home";
}

export function pathForTab(stageId, tabId) {
  const stage = stageById.get(stageId);
  return stage?.tabs.find((tab) => tab.id === tabId)?.path || stage?.path || "/home";
}

export const PRODUCT_ROUTE_PATHS = Object.freeze([
  ...AUXILIARY_SURFACES.map((surface) => surface.path),
  ...CORE_STAGES.flatMap((stage) => stage.tabs.map((tab) => tab.path)),
  "/proof",
]);
