import { contextualPath, pathForState, resolveRoute } from "./model.mjs";
import { runReadContext, runReadContextPath } from "./run-read.mjs";

const MUTATING_INTENT = /\b(start|launch|execute|cancel|promote|delete|remove|export|submit|approve)\b/i;

function locationParts(locationLike = {}) {
  return {
    pathname: String(locationLike.pathname || "/cockpit"),
    search: String(locationLike.search || ""),
  };
}

export function apolloContext(locationLike = {}) {
  const { pathname, search } = locationParts(locationLike);
  const route = resolveRoute(pathname, search);
  const run = runReadContext(search);
  return {
    pathname,
    search,
    workspaceId: route.workspaceId,
    stateId: route.stateId,
    label: route.label,
    strategyRef: route.strategyRef || "",
    runRef: run?.runRef || "",
    invocationId: run?.invocationId || "",
  };
}

function exactRunPath(path, context) {
  if (context.runRef && context.invocationId) {
    return runReadContextPath(path, context.runRef, context.invocationId, context.search);
  }
  return context.strategyRef ? contextualPath(path, context.strategyRef) : path;
}

function strategyPath(stateId, context) {
  if (!context.strategyRef) return "/strategies";
  return pathForState("strategies", stateId, context.strategyRef);
}

function action(label, path) {
  return { label, path };
}

function contextualActions(context) {
  const actions = [];
  if (context.strategyRef) {
    actions.push(action("Open strategy overview", strategyPath("overview", context)));
    actions.push(action("Open candidates", strategyPath("candidates", context)));
    actions.push(action("Open evidence", exactRunPath(strategyPath("evidence", context), context)));
  } else {
    actions.push(action("Open Strategies", "/strategies"));
  }
  actions.push(action("Open Run Setup", exactRunPath("/validate/run", context)));
  if (context.runRef && context.invocationId) {
    actions.push(action("Open verified results", exactRunPath("/validate/results", context)));
    actions.push(action("Open exact run in Operate", exactRunPath("/operate/runs", context)));
  }
  return actions.slice(0, 4);
}

export function apolloReply(message, locationLike = {}) {
  const text = String(message ?? "").trim();
  if (!text) {
    return { text: "Enter a question or navigation request.", actions: [], boundary: "navigation-only" };
  }

  const context = apolloContext(locationLike);
  const lower = text.toLowerCase();

  if (MUTATING_INTENT.test(text)) {
    return {
      text: "Apollo does not execute or authorize product mutations. I can prepare navigation to the owning surface so you can make the explicit action there.",
      actions: contextualActions(context),
      boundary: "refused-autonomous-action",
    };
  }

  if (/\b(where|context|current|what page|what route)\b/.test(lower)) {
    const parts = [`You are in ${context.label || `${context.workspaceId}/${context.stateId}`}.`];
    if (context.strategyRef) parts.push(`Strategy context: ${context.strategyRef}.`);
    if (context.runRef && context.invocationId) {
      parts.push(`Exact run context is present for invocation ${context.invocationId}.`);
    } else {
      parts.push("No exact run/invocation context is present in the URL.");
    }
    return { text: parts.join(" "), actions: contextualActions(context), boundary: "navigation-only" };
  }

  if (/\b(candidate|builder|evolution)\b/.test(lower)) {
    return {
      text: context.strategyRef
        ? "Candidates belong to the current strategy context. Apollo can open that product surface but will not start Builder or Evolutionary Search itself."
        : "Select an exact strategy reference before opening its Candidates surface.",
      actions: [action(context.strategyRef ? "Open candidates" : "Select strategy", strategyPath("candidates", context))],
      boundary: "navigation-only",
    };
  }

  if (/\b(evidence|proof|provenance)\b/.test(lower)) {
    return {
      text: "Evidence is read from canonical custody. Apollo can carry the current exact strategy/run context to the Evidence surface without creating or certifying evidence.",
      actions: [action("Open evidence", exactRunPath(strategyPath("evidence", context), context))],
      boundary: "navigation-only",
    };
  }

  if (/\b(result|validation|gate)\b/.test(lower)) {
    return {
      text: context.runRef && context.invocationId
        ? "An exact run/invocation context is available. Apollo can open the verified Results reader for that same identity."
        : "Results require an exact run reference and invocation ID; Apollo will not infer a latest run.",
      actions: [action("Open Results", exactRunPath("/validate/results", context))],
      boundary: "navigation-only",
    };
  }

  if (/\b(compare)\b/.test(lower)) {
    return {
      text: "Comparison is an explicit read-only validation surface. Apollo can open it but does not select a winner or rank strategies.",
      actions: [action("Open Compare", context.strategyRef ? contextualPath("/validate/compare", context.strategyRef) : "/validate/compare")],
      boundary: "navigation-only",
    };
  }

  if (/\b(next|help|what can|options|navigate|go)\b/.test(lower)) {
    return {
      text: "I can explain the current route and prepare navigation to canonical product surfaces. Compute, cancellation, promotion, deletion, export, and other mutations remain explicit actions on their owning surfaces.",
      actions: contextualActions(context),
      boundary: "navigation-only",
    };
  }

  return {
    text: "Apollo currently supports route context, canonical navigation, and explicit-action preparation. It does not answer from hidden product state or perform autonomous actions.",
    actions: contextualActions(context),
    boundary: "navigation-only",
  };
}

function setBadgeReady(surface) {
  const badge = surface.querySelector(".apollo-status .status-badge");
  if (!badge) return;
  badge.className = "status-badge status-pending";
  badge.innerHTML = '<span class="status-dot"></span>Apollo navigation ready';
}

function renderReply(surface, reply) {
  const hint = surface.querySelector(".apollo-hint");
  if (hint) {
    hint.textContent = reply.text;
    hint.dataset.apolloBoundary = reply.boundary;
  }

  let actions = surface.querySelector("[data-apollo-actions]");
  if (!actions) {
    actions = document.createElement("div");
    actions.className = "detail-actions";
    actions.dataset.apolloActions = "true";
    surface.append(actions);
  }
  actions.replaceChildren(
    ...reply.actions.map(({ label, path }) => {
      const link = document.createElement("a");
      link.className = "button button-quiet";
      link.href = path;
      link.dataset.route = path;
      link.textContent = label;
      return link;
    }),
  );
  actions.hidden = reply.actions.length === 0;
}

function enhanceApollo() {
  const surface = document.querySelector("[data-apollo-surface]");
  if (!surface || surface.dataset.apolloAssistantEnhanced === "true") return;
  surface.dataset.apolloAssistantEnhanced = "true";
  const input = surface.querySelector(".apollo-form input");
  const button = surface.querySelector(".apollo-form button");
  if (input) {
    input.disabled = false;
    input.placeholder = "Ask where to go next or prepare a navigation action";
    input.setAttribute("aria-label", "Apollo navigation request");
  }
  if (button) button.disabled = false;
  setBadgeReady(surface);
  const hint = surface.querySelector(".apollo-hint");
  if (hint) hint.textContent = "Route-aware guidance · navigation preparation only · no autonomous actions";
}

if (typeof document !== "undefined") {
  document.addEventListener("submit", (event) => {
    const form = event.target.closest?.("[data-apollo-form]");
    if (!form) return;
    event.preventDefault();
    const surface = form.closest("[data-apollo-surface]");
    const input = form.querySelector("input");
    if (!surface || !input) return;
    const reply = apolloReply(input.value, window.location);
    renderReply(surface, reply);
    input.value = "";
  });

  const root = document.querySelector("#app");
  if (root) {
    enhanceApollo();
    new MutationObserver(enhanceApollo).observe(root, { childList: true, subtree: true });
  }
}
