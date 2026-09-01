const CANDIDATES_API_PATH = "/api/research/candidates";
const HISTORICAL_RESULTS_API_PATH = "/api/research/historical-results";

const DEFINITIONS = Object.freeze({
  candidate: Object.freeze({
    entityKey: "candidateEntityId",
    revisionKey: "candidateRevision",
  }),
  historicalResult: Object.freeze({
    entityKey: "historicalResultEntityId",
    revisionKey: "historicalResultRevision",
  }),
});

const ROUTES = Object.freeze({
  overview: Object.freeze({
    kind: "candidate",
    selector: "#retester-candidate",
    workspace: "[data-retester-overview]",
    endpoint: CANDIDATES_API_PATH,
    catalogSchema: "tc.research-candidate-catalog.v1",
    arrayKey: "candidates",
    eligible: () => true,
    label: (record) => `${record.archive_name} · ${short(record.revision, 20, 18)}`,
  }),
  trades: Object.freeze({
    kind: "historicalResult",
    selector: "#historical-trades-result",
    workspace: "[data-research-trades]",
    endpoint: HISTORICAL_RESULTS_API_PATH,
    catalogSchema: "tc.research-historical-result-catalog.v1",
    arrayKey: "results",
    eligible: (record) => record.state === "completed" && record.execution_completed === true,
    label: (record) => `${record.result_archive_name} · ${short(record.revision, 22, 20)}`,
  }),
  configuration: Object.freeze({
    kind: "historicalResult",
    selector: "#backtest-configuration-result",
    workspace: "[data-backtest-configuration-workspace]",
    endpoint: HISTORICAL_RESULTS_API_PATH,
    catalogSchema: "tc.research-historical-result-catalog.v1",
    arrayKey: "results",
    eligible: (record) => record.state === "completed",
    label: (record) => `${short(record.revision, 22, 20)} · ${record.result_archive_name}`,
  }),
});

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function short(value, threshold, tail) {
  const text = String(value || "");
  return text.length > threshold ? `…${text.slice(-tail)}` : text;
}

function definition(kind) {
  const value = DEFINITIONS[kind];
  if (!value) throw new Error(`Unknown Research route selection kind: ${kind}`);
  return value;
}

export function readExactRouteSelection(locationLike, kind) {
  const { entityKey, revisionKey } = definition(kind);
  const params = new URLSearchParams(locationLike?.search || "");
  const entityId = params.get(entityKey);
  const revision = params.get(revisionKey);
  if (entityId === null && revision === null) return { state: "absent", entityId: null, revision: null };
  if (!entityId || !revision) return { state: "invalid", entityId, revision };
  return { state: "exact", entityId, revision };
}

export function routeWithExactSelection(locationLike, kind, record) {
  if (!record || typeof record.entity_id !== "string" || !record.entity_id || typeof record.revision !== "string" || !record.revision) {
    throw new Error("Exact Research route selection requires entity ID and revision");
  }
  const { entityKey, revisionKey } = definition(kind);
  const base = locationLike?.origin || "http://127.0.0.1";
  const url = new URL(`${locationLike?.pathname || "/research"}${locationLike?.search || ""}`, base);
  url.searchParams.set(entityKey, record.entity_id);
  url.searchParams.set(revisionKey, record.revision);
  return `${url.pathname}?${url.searchParams.toString()}${url.hash}`;
}

export function carryExactResearchSelections(currentLocationLike, targetHref) {
  const base = currentLocationLike?.origin || "http://127.0.0.1";
  const target = new URL(targetHref, base);
  if (target.origin !== base || target.pathname !== "/research") return targetHref;
  const current = new URL(`${currentLocationLike?.pathname || "/research"}${currentLocationLike?.search || ""}`, base);
  for (const { entityKey, revisionKey } of Object.values(DEFINITIONS)) {
    const entityId = current.searchParams.get(entityKey);
    const revision = current.searchParams.get(revisionKey);
    if (entityId && revision) {
      target.searchParams.set(entityKey, entityId);
      target.searchParams.set(revisionKey, revision);
    }
  }
  return `${target.pathname}?${target.searchParams.toString()}${target.hash}`;
}

export function resolveExactRouteSelection(records, selection) {
  if (!Array.isArray(records)) throw new Error("Research route selection catalog must be an array");
  if (selection?.state === "invalid") return { state: "invalid", index: -1 };
  if (selection?.state === "absent") return records.length ? { state: "default", index: 0 } : { state: "empty", index: -1 };
  if (selection?.state !== "exact") return { state: "invalid", index: -1 };
  const matches = records
    .map((record, index) => ({ record, index }))
    .filter(({ record }) => record.entity_id === selection.entityId && record.revision === selection.revision);
  return matches.length === 1 ? { state: "exact", index: matches[0].index } : { state: "stale", index: -1 };
}

function currentRoute() {
  if (globalThis.location?.pathname !== "/research") return null;
  const params = new URLSearchParams(globalThis.location.search || "");
  if (params.get("stage") !== "backtest") return null;
  return ROUTES[params.get("tab")] || null;
}

function validCatalogRecord(record) {
  return Boolean(
    record
    && typeof record === "object"
    && typeof record.entity_id === "string"
    && record.entity_id
    && typeof record.revision === "string"
    && record.revision,
  );
}

async function fetchRouteRecords(route, fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(route.endpoint, { headers: { accept: "application/json" } });
  let payload = null;
  try { payload = await response.json(); } catch { payload = null; }
  if (!response?.ok || payload?.schema !== route.catalogSchema || !Array.isArray(payload?.[route.arrayKey])) {
    throw new Error("Exact Backtest route selection catalog is unavailable");
  }
  const records = payload[route.arrayKey].filter(route.eligible);
  if (records.some((record) => !validCatalogRecord(record))) {
    throw new Error("Exact Backtest route selection catalog identity is invalid");
  }
  return records;
}

function failVisible(route, detail) {
  const workspace = globalThis.document?.querySelector(route.workspace);
  if (!workspace) return;
  workspace.dataset.routeSelectionState = "failed";
  workspace.innerHTML = `<div class="empty-state"><div class="empty-icon">!</div><div><strong>Exact bookmarked selection unavailable</strong><p>${escapeHtml(detail || "The selected entity/revision is not present in canonical custody.")}</p></div></div>`;
}

function optionLabelsMatch(select, records, route) {
  if (select.options.length !== records.length) return false;
  const labels = records.map(route.label);
  if (new Set(labels).size !== labels.length) return false;
  return labels.every((label, index) => select.options[index]?.textContent?.trim() === label);
}

let generation = 0;
let boundSelect = null;
let boundRecords = [];
let boundRoute = null;

async function bindRouteSelection() {
  const route = currentRoute();
  const select = route ? globalThis.document?.querySelector(route.selector) : null;
  if (!route || !select || (route === boundRoute && select === boundSelect)) return;
  const current = ++generation;
  boundRoute = route;
  boundSelect = select;
  boundRecords = [];
  select.disabled = true;
  select.closest(route.workspace)?.setAttribute("data-route-selection-state", "loading");
  try {
    const records = await fetchRouteRecords(route);
    if (current !== generation || currentRoute() !== route || !select.isConnected) return;
    if (!optionLabelsMatch(select, records, route)) {
      throw new Error("Rendered selector does not match the canonical exact-identity catalog");
    }
    const selection = readExactRouteSelection(globalThis.location, route.kind);
    const resolution = resolveExactRouteSelection(records, selection);
    if (["invalid", "stale"].includes(resolution.state)) {
      throw new Error("The route entity/revision is incomplete, stale, or absent from canonical custody");
    }
    boundRecords = records;
    if (resolution.index >= 0 && select.selectedIndex !== resolution.index) {
      select.value = String(resolution.index);
      select.dispatchEvent(new Event("change", { bubbles: true }));
    }
    if (resolution.state === "default" && records[0]) {
      globalThis.history?.replaceState?.(null, "", routeWithExactSelection(globalThis.location, route.kind, records[0]));
    }
    select.disabled = records.length === 0;
    select.closest(route.workspace)?.setAttribute("data-route-selection-state", "ready");
  } catch (error) {
    if (current !== generation || currentRoute() !== route) return;
    boundRecords = [];
    select.disabled = true;
    failVisible(route, error instanceof Error ? error.message : "Exact route selection refused");
  }
}

if (typeof document !== "undefined") {
  document.addEventListener("change", (event) => {
    const route = currentRoute();
    if (!route || event.target !== boundSelect || route !== boundRoute) return;
    const index = Number(event.target.value);
    if (!Number.isInteger(index) || index < 0 || index >= boundRecords.length) {
      failVisible(route, "Rendered selection index no longer matches canonical custody");
      return;
    }
    globalThis.history?.replaceState?.(null, "", routeWithExactSelection(globalThis.location, route.kind, boundRecords[index]));
  });
  document.addEventListener("click", (event) => {
    const route = currentRoute();
    const protectedAction = event.target?.closest?.('[data-retester-action="start"], [data-backtest-configuration-action="inspect"]');
    if (route && protectedAction) {
      const workspace = protectedAction.closest(route.workspace);
      if (workspace?.dataset.routeSelectionState !== "ready") {
        event.preventDefault();
        event.stopImmediatePropagation();
        failVisible(route, "Exact route identity must be reconciled before this action is available");
        return;
      }
    }
    const anchor = event.target?.closest?.("a[href]");
    if (!anchor || globalThis.location?.pathname !== "/research") return;
    const carried = carryExactResearchSelections(globalThis.location, anchor.getAttribute("href"));
    if (carried !== anchor.getAttribute("href")) anchor.setAttribute("href", carried);
  }, true);
  const observer = new MutationObserver(() => {
    if (!currentRoute()) {
      generation += 1;
      boundSelect = null;
      boundRecords = [];
      boundRoute = null;
      return;
    }
    void bindRouteSelection();
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void bindRouteSelection();
}
