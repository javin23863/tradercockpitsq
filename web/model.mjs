export const UNAVAILABLE_REASON =
  "Producer integration pending: this frontend surface is not yet connected to its authoritative backend producer.";

export const APOLLO_SURFACE_ID = "apollo-persistent";
export const RUN_SURFACE_ID = "shared-run-surface";
export const RUN_CONTEXT_OWNER = "shared-run-context";

export const PRIMARY_WORKSPACES = [
  {
    id: "cockpit",
    label: "Cockpit",
    icon: "⌂",
    path: "/cockpit",
    states: [{ id: "home", label: "Cockpit Home", path: "/cockpit" }],
  },
  {
    id: "strategies",
    label: "Strategies",
    icon: "◇",
    path: "/strategies",
    states: [
      { id: "root", label: "Strategies root", path: "/strategies" },
      { id: "overview", label: "Overview", segment: "overview" },
      { id: "build", label: "Build", segment: "build" },
      { id: "signals", label: "Signals & Models", segment: "signals" },
      { id: "candidates", label: "Candidates", segment: "candidates" },
      { id: "evidence", label: "Evidence", segment: "evidence" },
    ],
  },
  {
    id: "explore",
    label: "Explore",
    icon: "⌕",
    path: "/explore",
    states: [
      { id: "root", label: "Explore root", path: "/explore" },
      { id: "catalog", label: "Catalog", path: "/explore/catalog" },
      {
        id: "market",
        label: "Market Workspace",
        path: "/explore/market",
      },
      { id: "data", label: "Market Data", path: "/explore/data" },
    ],
  },
  {
    id: "validate",
    label: "Test & Validate",
    icon: "✓",
    path: "/validate",
    states: [
      { id: "root", label: "Test & Validate root", path: "/validate" },
      { id: "run", label: "Run Setup", path: "/validate/run" },
      { id: "results", label: "Results", path: "/validate/results" },
      {
        id: "stress",
        label: "Stress & Robustness",
        path: "/validate/stress",
      },
      { id: "compare", label: "Compare", path: "/validate/compare" },
      {
        id: "prop",
        label: "Prop Simulation",
        path: "/validate/prop",
      },
    ],
  },
  {
    id: "operate",
    label: "Operate",
    icon: "◉",
    path: "/operate",
    states: [
      { id: "root", label: "Operate root", path: "/operate" },
      { id: "runs", label: "Runs", path: "/operate/runs" },
      {
        id: "performance",
        label: "Performance",
        path: "/operate/performance",
      },
      {
        id: "execution-risk",
        label: "Execution & Risk",
        path: "/operate/execution-risk",
      },
    ],
  },
];

export const LEGACY_REDIRECTS = Object.freeze({
  "/": { legacyName: "home", workspaceId: "cockpit", stateId: "home" },
  "/home": { legacyName: "home", workspaceId: "cockpit", stateId: "home" },
  "/strategy-signals": {
    legacyName: "strategy-signals",
    workspaceId: "strategies",
    stateId: "signals",
  },
  "/research": {
    legacyName: "research",
    workspaceId: "explore",
    stateId: "catalog",
  },
  "/validation": {
    legacyName: "validate",
    workspaceId: "validate",
    stateId: "results",
  },
  "/old/validate": {
    legacyName: "validate",
    workspaceId: "validate",
    stateId: "results",
  },
  "/evolution": {
    legacyName: "evolution",
    workspaceId: "strategies",
    stateId: "candidates",
  },
  "/evolutionary-search": {
    legacyName: "evolution",
    workspaceId: "strategies",
    stateId: "candidates",
  },
  "/prop": { legacyName: "prop", workspaceId: "validate", stateId: "prop" },
  "/prop-simulation": {
    legacyName: "prop",
    workspaceId: "validate",
    stateId: "prop",
  },
  "/monitor": { legacyName: "monitor", workspaceId: "operate", stateId: "runs" },
  "/performance": {
    legacyName: "performance",
    workspaceId: "operate",
    stateId: "performance",
  },
  "/execution": {
    legacyName: "execution",
    workspaceId: "operate",
    stateId: "execution-risk",
  },
  "/execution-risk": {
    legacyName: "execution",
    workspaceId: "operate",
    stateId: "execution-risk",
  },
  "/governance": {
    legacyName: "governance",
    workspaceId: "strategies",
    stateId: "evidence",
  },
  "/chart": {
    legacyName: "chart",
    workspaceId: "strategies",
    stateId: "signals",
  },
  "/backtest": {
    legacyName: "backtest",
    workspaceId: "validate",
    stateId: "results",
  },
  "/proof": {
    legacyName: "proof",
    workspaceId: "strategies",
    stateId: "evidence",
  },
});

const strategyStateBySegment = new Map(
  PRIMARY_WORKSPACES.find((workspace) => workspace.id === "strategies").states
    .filter((state) => state.segment)
    .map((state) => [state.segment, state]),
);

const staticRoutes = new Map(
  PRIMARY_WORKSPACES.flatMap((workspace) =>
    workspace.states
      .filter((state) => state.path)
      .map((state) => [
        state.path,
        {
          workspaceId: workspace.id,
          stateId: state.id,
          label: state.label,
          path: state.path,
        },
      ]),
  ),
);

export const LOGICAL_STATES = PRIMARY_WORKSPACES.flatMap((workspace) =>
  workspace.states.map((state) => ({
    stateKey: `${workspace.id}.${state.id}`,
    workspaceId: workspace.id,
    workspaceLabel: workspace.label,
    ...state,
  })),
);

export function normalizePath(pathname = "/cockpit") {
  const path = String(pathname).split("?")[0] || "/cockpit";
  if (path === "/") return "/";
  return path.replace(/\/+$/, "") || "/cockpit";
}

export function pathForState(workspaceId, stateId, strategyRef = "") {
  const workspace = PRIMARY_WORKSPACES.find(
    (candidate) => candidate.id === workspaceId,
  );
  const state = workspace?.states.find((candidate) => candidate.id === stateId);
  if (!workspace || !state) return "/cockpit";

  if (workspaceId === "strategies" && state.segment) {
    if (!strategyRef) return `/strategies/${state.segment}`;
    return `/strategies/${encodeURIComponent(strategyRef)}/${state.segment}`;
  }

  return state.path || workspace.path;
}

export function contextualPath(path, strategyRef = "") {
  if (!strategyRef) return path;
  return `${path}?strategyRef=${encodeURIComponent(strategyRef)}`;
}

function routeRecord(workspaceId, stateId, path, strategyRef = "", extra = {}) {
  const workspace = PRIMARY_WORKSPACES.find(
    (candidate) => candidate.id === workspaceId,
  );
  const state = workspace?.states.find((candidate) => candidate.id === stateId);
  return {
    kind: "state",
    workspaceId,
    stateId,
    stateKey: `${workspaceId}.${stateId}`,
    label: state?.label || stateId,
    path,
    strategyRef,
    ...extra,
  };
}

function queryWithContext(path, search, consumeStrategyRef = false) {
  const params = new URLSearchParams(search);
  if (consumeStrategyRef) params.delete("strategyRef");
  const query = params.toString();
  return query ? `${path}?${query}` : path;
}

function legacyRedirect(normalizedPath, search) {
  const legacy = LEGACY_REDIRECTS[normalizedPath];
  if (!legacy) return null;

  const params = new URLSearchParams(search);
  const strategyRef = params.get("strategyRef") || "";
  const consumesStrategyRef =
    legacy.workspaceId === "strategies" &&
    legacy.stateId !== "root" &&
    Boolean(strategyRef);
  const targetPath = pathForState(
    legacy.workspaceId,
    legacy.stateId,
    strategyRef,
  );

  return {
    kind: "redirect",
    legacyName: legacy.legacyName,
    legacyPath: normalizedPath,
    redirectPath: queryWithContext(targetPath, search, consumesStrategyRef),
    strategyRef,
  };
}

export function resolveRoute(pathname = "/cockpit", search = "") {
  const normalizedPath = normalizePath(pathname);
  const queryStrategyRef = new URLSearchParams(search).get("strategyRef") || "";

  const redirect = legacyRedirect(normalizedPath, search);
  if (redirect) return redirect;

  const strategyMatch = normalizedPath.match(
    /^\/strategies\/([^/]+)\/([^/]+)$/,
  );
  if (strategyMatch) {
    let strategyRef;
    try {
      strategyRef = decodeURIComponent(strategyMatch[1]);
    } catch {
      strategyRef = "";
    }
    const state = strategyStateBySegment.get(strategyMatch[2]);
    if (state && strategyRef) {
      return routeRecord("strategies", state.id, normalizedPath, strategyRef);
    }
  }

  const identityOnlyStrategyMatch = normalizedPath.match(
    /^\/strategies\/([^/]+)$/,
  );
  if (identityOnlyStrategyMatch) {
    const state = strategyStateBySegment.get(identityOnlyStrategyMatch[1]);
    if (state) {
      return routeRecord(
        "strategies",
        state.id,
        normalizedPath,
        "",
        { identityOnly: true },
      );
    }
  }

  const staticRoute = staticRoutes.get(normalizedPath);
  if (staticRoute) {
    return routeRecord(
      staticRoute.workspaceId,
      staticRoute.stateId,
      staticRoute.path,
      queryStrategyRef,
    );
  }

  return routeRecord("cockpit", "home", "/cockpit", queryStrategyRef, {
    unknownPath: normalizedPath,
  });
}

export function workspaceForRoute(route) {
  return PRIMARY_WORKSPACES.find(
    (workspace) => workspace.id === route.workspaceId,
  );
}
