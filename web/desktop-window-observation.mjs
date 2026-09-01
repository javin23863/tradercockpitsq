export const DESKTOP_WINDOW_OBSERVATION_STATE_KEY = "tradercockpit_window_observation";

function stringAttribute(node, name) {
  return node?.getAttribute?.(name) || "";
}

export function currentDesktopWindowObservation(
  documentLike = globalThis.document,
  locationLike = globalThis.location,
) {
  const shell = documentLike?.querySelector?.('[data-product-shell="tradercockpit-desktop"]') || null;
  return {
    location_pathname: locationLike?.pathname || "",
    location_search: locationLike?.search || "",
    document_title: documentLike?.title || "",
    product_shell: stringAttribute(shell, "data-product-shell"),
    surface_id: stringAttribute(shell, "data-surface-id"),
    research_stage_id: stringAttribute(shell, "data-research-stage-id"),
    research_tab_id: stringAttribute(shell, "data-research-tab-id"),
    page_heading: documentLike?.querySelector?.(".content-inner h1")?.textContent?.trim?.() || "",
    idea_workspace: Boolean(documentLike?.querySelector?.("[data-research-idea-workspace]")),
    idea_save_action: Boolean(documentLike?.querySelector?.('[data-idea-action="save"]')),
  };
}

export function isSettledDesktopWindowObservation(observation) {
  if (
    !observation
    || observation.product_shell !== "tradercockpit-desktop"
    || !observation.surface_id
  ) return false;
  if (observation.surface_id !== "research") return true;
  if (observation.research_stage_id === "proof") return true;
  if (!observation.research_stage_id || !observation.research_tab_id) return false;
  if (
    observation.research_stage_id === "construct"
    && observation.research_tab_id === "idea"
  ) {
    return observation.idea_workspace === true && observation.idea_save_action === true;
  }
  return true;
}

export function publishDesktopWindowObservation(
  windowLike = globalThis.window,
  documentLike = globalThis.document,
) {
  const state = windowLike?.pywebview?.state;
  if (!state) return null;
  const observation = currentDesktopWindowObservation(documentLike, windowLike.location);
  state[DESKTOP_WINDOW_OBSERVATION_STATE_KEY] = observation;
  return observation;
}

export function installDesktopWindowObservation({
  windowLike = globalThis.window,
  documentLike = globalThis.document,
  intervalMilliseconds = 50,
  timeoutMilliseconds = 20_000,
  setIntervalImpl = globalThis.setInterval,
  clearIntervalImpl = globalThis.clearInterval,
} = {}) {
  if (!windowLike?.addEventListener || typeof setIntervalImpl !== "function") {
    return { stop() {} };
  }

  let timer = null;
  let started = false;
  let deadline = 0;

  const stop = () => {
    if (timer !== null && typeof clearIntervalImpl === "function") clearIntervalImpl(timer);
    timer = null;
  };

  const report = () => {
    const observation = publishDesktopWindowObservation(windowLike, documentLike);
    if (!observation) return;
    if (isSettledDesktopWindowObservation(observation) || Date.now() >= deadline) stop();
  };

  const start = () => {
    if (started || !windowLike?.pywebview?.state) return;
    started = true;
    deadline = Date.now() + timeoutMilliseconds;
    report();
    if (timer === null && !isSettledDesktopWindowObservation(
      currentDesktopWindowObservation(documentLike, windowLike.location),
    )) {
      timer = setIntervalImpl(report, intervalMilliseconds);
    }
  };

  if (windowLike.pywebview?.state) start();
  else windowLike.addEventListener("pywebviewready", start, { once: true });

  return { stop };
}

if (typeof window !== "undefined" && typeof document !== "undefined") {
  installDesktopWindowObservation();
}
