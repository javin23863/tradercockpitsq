const SQX_BUILDER_CONFIG_API_PATH = "/api/sqx-builder-config";
const BUILDER_PROJECT_PATH = "user/projects/Builder/project.cfx";
const BUILDER_TASK_ENTRY = "Build-Task1.xml";
const BUILDER_CONFIG_SCHEMA = "tc.sqx-builder-config.v1";
export const BUILDER_MONEY_MANAGEMENT_SCHEMA = "tc.sqx-builder-money-management.v1";

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

function validateNativeNode(node, path = "money_management.producer_configuration") {
  if (
    !isPlainObject(node)
    || typeof node.tag !== "string" || !node.tag
    || !isPlainObject(node.attributes)
    || Object.entries(node.attributes).some(([key, value]) => !key || typeof value !== "string")
    || !(node.text === null || typeof node.text === "string")
    || !Array.isArray(node.children)
  ) {
    throw new Error(`Native MoneyManagement node mismatch at ${path}`);
  }
  node.children.forEach((child, index) => validateNativeNode(child, `${path}.children[${index}]`));
  return node;
}

export function moneyManagementConfigurationFromBuilderConfig(payload) {
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
    throw new Error("Builder MoneyManagement source identity mismatch");
  }

  const moneyManagement = payload.money_management;
  if (
    !isPlainObject(moneyManagement)
    || moneyManagement.schema !== BUILDER_MONEY_MANAGEMENT_SCHEMA
    || moneyManagement.authority !== "native_sqx_read_only"
    || !isPlainObject(moneyManagement.source)
    || moneyManagement.source.source_build !== payload.source_build
    || moneyManagement.source.project !== payload.project
    || moneyManagement.source.relative_path !== payload.source_relative_path
    || moneyManagement.source.archive_sha256 !== payload.archive_sha256
    || moneyManagement.source.member !== BUILDER_TASK_ENTRY
    || !isPlainObject(moneyManagement.semantics)
    || moneyManagement.semantics.interpreted_by_tradercockpit !== false
    || moneyManagement.semantics.owner !== "StrategyQuant X"
    || typeof moneyManagement.semantics.description !== "string" || !moneyManagement.semantics.description
    || !isPlainObject(moneyManagement.execution)
    || moneyManagement.execution.available !== false
    || moneyManagement.execution.reason !== "native_sqx_builder_owns_money_management_configuration"
  ) {
    throw new Error("Native Builder MoneyManagement schema mismatch");
  }

  if (moneyManagement.producer_configuration !== null) {
    validateNativeNode(moneyManagement.producer_configuration);
    if (moneyManagement.producer_configuration.tag !== "MoneyManagement") {
      throw new Error("Native Builder MoneyManagement root must be MoneyManagement");
    }
  }
  return moneyManagement;
}

async function readJson(response) {
  try { return await response.json(); } catch { return null; }
}

export async function fetchNativeBuilderMoneyManagement(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Native Builder MoneyManagement fetch is unavailable");
  const response = await fetchImpl(SQX_BUILDER_CONFIG_API_PATH, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || `Native Builder MoneyManagement request failed: ${response?.status ?? "unknown"}`);
  return moneyManagementConfigurationFromBuilderConfig(payload);
}

function flattenNativeNodes(node, path = "MoneyManagement", rows = []) {
  rows.push({ path, node });
  node.children.forEach((child, index) => {
    flattenNativeNodes(child, `${path}/${child.tag}[${index}]`, rows);
  });
  return rows;
}

export function renderNativeBuilderMoneyManagement(moneyManagement) {
  if (!moneyManagement) return "";
  const root = moneyManagement.producer_configuration;
  if (root === null) {
    return '<section data-native-builder-money-management><div class="requirement-item"><div><strong>Native sizing / money management</strong><span class="status-badge status-unavailable"><span class="status-dot"></span>MoneyManagement absent</span></div><p>The exact current Builder task does not expose a MoneyManagement subtree. TraderCockpit does not invent a sizing model or defaults.</p></div></section>';
  }
  const nodes = flattenNativeNodes(root);
  const rows = nodes.map(({ path, node }) => {
    const attributes = Object.entries(node.attributes).map(
      ([key, value]) => `<div class="stat-row"><span>${escapeHtml(key)}</span><code>${escapeHtml(value)}</code></div>`,
    ).join("");
    const text = node.text === null
      ? ""
      : `<div class="stat-row"><span>Text</span><code>${escapeHtml(node.text)}</code></div>`;
    return `<div class="requirement-item" data-native-money-management-node="${escapeHtml(path)}"><div><strong>${escapeHtml(node.tag)}</strong><span class="field-help">${escapeHtml(path)}</span></div>${attributes}${text}</div>`;
  }).join("");
  return `<section data-native-builder-money-management><div class="context-callout"><span class="callout-icon">↳</span><div><span class="eyebrow">Native sizing / money management</span><strong>Exact current SQX MoneyManagement structure</strong><span>Read-only producer structure from the same immutable Builder project snapshot. Native tag names and values are shown without assigning a sizing model, risk percentage, fixed-lot meaning, compounding behavior, stop-loss dependency, or parameter type/range semantics.</span></div></div><div class="idea-identity"><div class="stat-row"><span>Native nodes</span><code>${nodes.length}</code></div><div class="stat-row"><span>Source member</span><code>${escapeHtml(moneyManagement.source.member)}</code></div><div class="stat-row"><span>Archive SHA-256</span><code>${escapeHtml(moneyManagement.source.archive_sha256)}</code></div></div><p class="field-help">TraderCockpit does not edit, normalize, classify, calculate, simulate, or execute this structure. StrategyQuant X remains authoritative for sizing, risk, lots, compounding, dependencies, parameters, and execution.</p><div class="requirement-list">${rows}</div></section>`;
}

function specificationRoute() {
  if (globalThis.location?.pathname !== "/research") return false;
  const params = new URLSearchParams(globalThis.location.search || "");
  return params.get("stage") === "construct" && params.get("tab") === "specification";
}

let generation = 0;
let boundHost = null;

async function bindMoneyManagementInspector() {
  if (!specificationRoute()) return;
  const host = document.querySelector('[data-research-capability="builder_native_specification"]');
  if (!host || host === boundHost) return;
  boundHost = host;
  const workspace = document.createElement("div");
  workspace.dataset.nativeBuilderMoneyManagementWorkspace = "loading";
  workspace.innerHTML = '<p class="field-help" data-native-builder-money-management-status>Reading exact native MoneyManagement structure…</p>';
  host.append(workspace);
  const myGeneration = ++generation;
  try {
    const moneyManagement = await fetchNativeBuilderMoneyManagement();
    if (myGeneration !== generation || !specificationRoute() || !workspace.isConnected) return;
    workspace.dataset.nativeBuilderMoneyManagementWorkspace = "loaded";
    workspace.innerHTML = renderNativeBuilderMoneyManagement(moneyManagement);
  } catch (error) {
    if (myGeneration !== generation || !specificationRoute() || !workspace.isConnected) return;
    workspace.dataset.nativeBuilderMoneyManagementWorkspace = "failed";
    workspace.innerHTML = `<p class="idea-save-status">${escapeHtml(error instanceof Error ? error.message : "Native Builder MoneyManagement unavailable")}</p>`;
  }
}

if (typeof document !== "undefined") {
  const observer = new MutationObserver(() => {
    if (!specificationRoute()) {
      generation += 1;
      boundHost = null;
      return;
    }
    void bindMoneyManagementInspector();
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void bindMoneyManagementInspector();
}
