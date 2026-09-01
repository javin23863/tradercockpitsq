const SQX_BUILDER_CONFIG_API_PATH = "/api/sqx-builder-config";
const BUILDER_PROJECT_PATH = "user/projects/Builder/project.cfx";
const BUILDER_TASK_ENTRY = "Build-Task1.xml";
const BUILDER_CONFIG_SCHEMA = "tc.sqx-builder-config.v1";
export const BUILDER_TRADING_OPTIONS_SCHEMA = "tc.sqx-builder-trading-options.v1";

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function isPlainObject(value) {
  return Boolean(value && typeof value === "object" && !Array.isArray(value));
}

function digest(value) {
  return typeof value === "string" && /^[0-9a-f]{64}$/i.test(value) ? value : "";
}

function validateNativeNode(node, path = "trading_options.producer_configuration") {
  if (
    !isPlainObject(node)
    || typeof node.tag !== "string" || !node.tag
    || !isPlainObject(node.attributes)
    || Object.entries(node.attributes).some(([key, value]) => !key || typeof value !== "string")
    || !(node.text === null || typeof node.text === "string")
    || !Array.isArray(node.children)
  ) {
    throw new Error(`Native BuildTradingOptions node mismatch at ${path}`);
  }
  node.children.forEach((child, index) => validateNativeNode(child, `${path}.children[${index}]`));
  return node;
}

export function tradingOptionsConfigurationFromBuilderConfig(payload) {
  if (
    !isPlainObject(payload)
    || payload.schema !== BUILDER_CONFIG_SCHEMA
    || payload.project !== "Builder"
    || payload.source_relative_path !== BUILDER_PROJECT_PATH
    || typeof payload.source_build !== "string" || !payload.source_build
    || !digest(payload.archive_sha256)
    || !Array.isArray(payload.internal_entries)
    || payload.internal_entries.filter((entry) => entry === BUILDER_TASK_ENTRY).length !== 1
  ) {
    throw new Error("Builder trading options source identity mismatch");
  }

  const tradingOptions = payload.trading_options;
  if (
    !isPlainObject(tradingOptions)
    || tradingOptions.schema !== BUILDER_TRADING_OPTIONS_SCHEMA
    || tradingOptions.authority !== "native_sqx_read_only"
    || !isPlainObject(tradingOptions.source)
    || tradingOptions.source.source_build !== payload.source_build
    || tradingOptions.source.project !== payload.project
    || tradingOptions.source.relative_path !== payload.source_relative_path
    || tradingOptions.source.archive_sha256 !== payload.archive_sha256
    || tradingOptions.source.member !== BUILDER_TASK_ENTRY
    || !isPlainObject(tradingOptions.semantics)
    || tradingOptions.semantics.interpreted_by_tradercockpit !== false
    || tradingOptions.semantics.owner !== "StrategyQuant X"
    || typeof tradingOptions.semantics.description !== "string" || !tradingOptions.semantics.description
    || !isPlainObject(tradingOptions.execution)
    || tradingOptions.execution.available !== false
    || tradingOptions.execution.reason !== "native_sqx_builder_owns_trading_options_configuration"
  ) {
    throw new Error("Native Builder trading options schema mismatch");
  }

  if (tradingOptions.producer_configuration !== null) {
    validateNativeNode(tradingOptions.producer_configuration);
    if (tradingOptions.producer_configuration.tag !== "BuildTradingOptions") {
      throw new Error("Native Builder trading options root must be BuildTradingOptions");
    }
  }
  return tradingOptions;
}

async function readJson(response) {
  try { return await response.json(); } catch { return null; }
}

export async function fetchNativeBuilderTradingOptions(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Native Builder trading options fetch is unavailable");
  const response = await fetchImpl(SQX_BUILDER_CONFIG_API_PATH, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || `Native Builder trading options request failed: ${response?.status ?? "unknown"}`);
  return tradingOptionsConfigurationFromBuilderConfig(payload);
}

function flattenNativeNodes(node, path = "BuildTradingOptions", rows = []) {
  rows.push({ path, node });
  node.children.forEach((child, index) => {
    flattenNativeNodes(child, `${path}/${child.tag}[${index}]`, rows);
  });
  return rows;
}

export function renderNativeBuilderTradingOptions(tradingOptions) {
  if (!tradingOptions) return "";
  const root = tradingOptions.producer_configuration;
  if (root === null) {
    return '<section data-native-builder-trading-options><div class="requirement-item"><div><strong>Native trading assumptions</strong><span class="status-badge status-unavailable"><span class="status-dot"></span>BuildTradingOptions absent</span></div><p>The exact current Builder task does not expose a BuildTradingOptions subtree. TraderCockpit does not invent trading assumptions or defaults.</p></div></section>';
  }
  const nodes = flattenNativeNodes(root);
  const rows = nodes.map(({ path, node }) => {
    const attributes = Object.entries(node.attributes).map(
      ([key, value]) => `<div class="stat-row"><span>${escapeHtml(key)}</span><code>${escapeHtml(value)}</code></div>`,
    ).join("");
    const text = node.text === null
      ? ""
      : `<div class="stat-row"><span>Text</span><code>${escapeHtml(node.text)}</code></div>`;
    return `<div class="requirement-item" data-native-trading-options-node="${escapeHtml(path)}"><div><strong>${escapeHtml(node.tag)}</strong><span class="field-help">${escapeHtml(path)}</span></div>${attributes}${text}</div>`;
  }).join("");
  return `<section data-native-builder-trading-options><div class="context-callout"><span class="callout-icon">↳</span><div><span class="eyebrow">Native trading assumptions</span><strong>Exact current SQX BuildTradingOptions structure</strong><span>Read-only producer structure from the same immutable Builder project snapshot. Native tag names and values are shown without assigning order, exit, session, stop, target, timing, numeric, dependency, or other trading semantics.</span></div></div><div class="idea-identity"><div class="stat-row"><span>Native nodes</span><code>${nodes.length}</code></div><div class="stat-row"><span>Source member</span><code>${escapeHtml(tradingOptions.source.member)}</code></div><div class="stat-row"><span>Archive SHA-256</span><code>${escapeHtml(tradingOptions.source.archive_sha256)}</code></div></div><p class="field-help">TraderCockpit does not edit, normalize, classify, calculate, simulate, or execute this structure. StrategyQuant X remains authoritative for every BuildTradingOptions value, dependency, interpretation, and execution behavior.</p><div class="requirement-list">${rows}</div></section>`;
}

function specificationRoute() {
  if (globalThis.location?.pathname !== "/research") return false;
  const params = new URLSearchParams(globalThis.location.search || "");
  return params.get("stage") === "construct" && params.get("tab") === "specification";
}

let generation = 0;
let boundHost = null;

async function bindTradingOptionsInspector() {
  if (!specificationRoute()) return;
  const host = document.querySelector('[data-research-capability="builder_native_specification"]');
  if (!host || host === boundHost) return;
  boundHost = host;
  const workspace = document.createElement("div");
  workspace.dataset.nativeBuilderTradingOptionsWorkspace = "loading";
  workspace.innerHTML = '<p class="field-help" data-native-builder-trading-options-status>Reading exact native BuildTradingOptions structure…</p>';
  host.append(workspace);
  const myGeneration = ++generation;
  try {
    const tradingOptions = await fetchNativeBuilderTradingOptions();
    if (myGeneration !== generation || !specificationRoute() || !workspace.isConnected) return;
    workspace.dataset.nativeBuilderTradingOptionsWorkspace = "loaded";
    workspace.innerHTML = renderNativeBuilderTradingOptions(tradingOptions);
  } catch (error) {
    if (myGeneration !== generation || !specificationRoute() || !workspace.isConnected) return;
    workspace.dataset.nativeBuilderTradingOptionsWorkspace = "failed";
    workspace.innerHTML = `<p class="idea-save-status">${escapeHtml(error instanceof Error ? error.message : "Native Builder trading options unavailable")}</p>`;
  }
}

if (typeof document !== "undefined") {
  const observer = new MutationObserver(() => {
    if (!specificationRoute()) {
      generation += 1;
      boundHost = null;
      return;
    }
    void bindTradingOptionsInspector();
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void bindTradingOptionsInspector();
}
