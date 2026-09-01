const NODE_SELECTOR = [
  "[data-native-search-node]",
  "[data-native-trading-options-node]",
  "[data-native-block-node]",
  "[data-native-ranking-node]",
  "[data-native-cross-check-node]",
  "[data-native-money-management-node]",
].join(",");

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function specificationRoute(locationLike = globalThis.location) {
  if (locationLike?.pathname !== "/research") return false;
  const params = new URLSearchParams(locationLike.search || "");
  return params.get("stage") === "construct" && params.get("tab") === "specification";
}

export function nativeInspectorMatches(text, query) {
  const needle = String(query ?? "").trim().toLocaleLowerCase();
  if (!needle) return true;
  return String(text ?? "").toLocaleLowerCase().includes(needle);
}

export function renderNativeInspectorTools(query = "", visible = 0, total = 0) {
  const status = total === 0
    ? "Native structures appear here when the current canonical Builder read model exposes them."
    : query
      ? `Showing ${visible} of ${total} exact native nodes.`
      : `${total} exact native nodes currently visible across the loaded Builder inspectors.`;
  return `<section data-research-native-inspector-tools><div class="context-callout"><span class="callout-icon">⌕</span><div><span class="eyebrow">Native structure search</span><strong>Search exact current Builder structures</strong><span>Text filtering only. TraderCockpit does not classify native tags, infer parameter types, assign indicator/search/trading semantics, or alter producer configuration.</span></div></div><label class="field-label" for="research-native-structure-search">Find exact tag, path, attribute, or value</label><input id="research-native-structure-search" class="idea-editor" type="search" autocomplete="off" value="${escapeHtml(query)}" placeholder="Search exact native structure…" data-native-inspector-search /><p class="field-help" data-native-inspector-search-status>${escapeHtml(status)}</p></section>`;
}

let boundHost = null;
let toolbar = null;
let activeQuery = "";

function capabilityHost(documentLike = globalThis.document) {
  if (!documentLike || !specificationRoute()) return null;
  return documentLike.querySelector?.('[data-research-capability="builder_native_specification"]') || null;
}

export function applyNativeInspectorSearch(documentLike = globalThis.document, query = activeQuery) {
  if (!documentLike || !specificationRoute()) return { total: 0, visible: 0 };
  activeQuery = String(query ?? "");
  const nodes = [...(documentLike.querySelectorAll?.(NODE_SELECTOR) || [])];
  let visible = 0;
  for (const node of nodes) {
    const matches = nativeInspectorMatches(node.textContent || "", activeQuery);
    node.hidden = !matches;
    if (matches) visible += 1;
  }
  const status = documentLike.querySelector?.("[data-native-inspector-search-status]");
  if (status) {
    status.textContent = nodes.length === 0
      ? "Native structures appear here when the current canonical Builder read model exposes them."
      : activeQuery.trim()
        ? `Showing ${visible} of ${nodes.length} exact native nodes.`
        : `${nodes.length} exact native nodes currently visible across the loaded Builder inspectors.`;
  }
  return { total: nodes.length, visible };
}

export function ensureNativeInspectorTools(documentLike = globalThis.document) {
  if (!documentLike || !specificationRoute()) return null;
  const host = capabilityHost(documentLike);
  if (!host) return null;
  if (!toolbar?.isConnected || host !== boundHost) {
    boundHost = host;
    toolbar = documentLike.createElement("div");
    toolbar.dataset.researchNativeInspectorToolsWorkspace = "ready";
    toolbar.innerHTML = renderNativeInspectorTools(activeQuery);
    host.prepend(toolbar);
  }
  applyNativeInspectorSearch(documentLike, activeQuery);
  return toolbar;
}

if (typeof document !== "undefined") {
  document.addEventListener("input", (event) => {
    if (!specificationRoute()) return;
    const input = event.target?.closest?.("[data-native-inspector-search]");
    if (!input) return;
    applyNativeInspectorSearch(document, input.value || "");
  });
  const observer = new MutationObserver(() => {
    if (!specificationRoute()) {
      boundHost = null;
      toolbar = null;
      activeQuery = "";
      return;
    }
    ensureNativeInspectorTools();
    applyNativeInspectorSearch(document, activeQuery);
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  ensureNativeInspectorTools();
}
