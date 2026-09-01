const SQX_BUILDER_CONFIG_API_PATH = "/api/sqx-builder-config";
const BUILDER_PROJECT_PATH = "user/projects/Builder/project.cfx";
const BUILDER_TASK_ENTRY = "Build-Task1.xml";
const BUILDER_CONFIG_SCHEMA = "tc.sqx-builder-config.v1";
export const BUILDER_CROSS_CHECKS_SCHEMA = "tc.sqx-builder-cross-checks.v1";

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

function validateNativeNode(node, path = "cross_checks.producer_configuration") {
  if (
    !isPlainObject(node)
    || typeof node.tag !== "string" || !node.tag
    || !isPlainObject(node.attributes)
    || Object.entries(node.attributes).some(([key, value]) => !key || typeof value !== "string")
    || !(node.text === null || typeof node.text === "string")
    || !Array.isArray(node.children)
  ) {
    throw new Error(`Native CrossChecks node mismatch at ${path}`);
  }
  node.children.forEach((child, index) => validateNativeNode(child, `${path}.children[${index}]`));
  return node;
}

export function crossChecksConfigurationFromBuilderConfig(payload) {
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
    throw new Error("Builder CrossChecks source identity mismatch");
  }

  const crossChecks = payload.cross_checks;
  if (
    !isPlainObject(crossChecks)
    || crossChecks.schema !== BUILDER_CROSS_CHECKS_SCHEMA
    || crossChecks.authority !== "native_sqx_read_only"
    || !isPlainObject(crossChecks.source)
    || crossChecks.source.source_build !== payload.source_build
    || crossChecks.source.project !== payload.project
    || crossChecks.source.relative_path !== payload.source_relative_path
    || crossChecks.source.archive_sha256 !== payload.archive_sha256
    || crossChecks.source.member !== BUILDER_TASK_ENTRY
    || typeof crossChecks.enabled !== "boolean"
    || !isPlainObject(crossChecks.semantics)
    || crossChecks.semantics.interpreted_by_tradercockpit !== false
    || crossChecks.semantics.owner !== "StrategyQuant X"
    || typeof crossChecks.semantics.description !== "string" || !crossChecks.semantics.description
    || !isPlainObject(crossChecks.execution)
    || crossChecks.execution.available !== false
    || crossChecks.execution.reason !== "native_sqx_builder_owns_cross_check_configuration"
  ) {
    throw new Error("Native Builder CrossChecks schema mismatch");
  }

  if (crossChecks.producer_configuration === null) {
    if (crossChecks.enabled !== false) throw new Error("Absent CrossChecks cannot be enabled");
    return crossChecks;
  }

  validateNativeNode(crossChecks.producer_configuration);
  if (crossChecks.producer_configuration.tag !== "CrossChecks") {
    throw new Error("Native Builder CrossChecks root must be CrossChecks");
  }
  const literalEnabled = String(crossChecks.producer_configuration.attributes.use || "").toLowerCase() === "true";
  if (crossChecks.enabled !== literalEnabled) {
    throw new Error("Native Builder CrossChecks use flag mismatch");
  }
  return crossChecks;
}

async function readJson(response) {
  try { return await response.json(); } catch { return null; }
}

export async function fetchNativeBuilderCrossChecks(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Native Builder CrossChecks fetch is unavailable");
  const response = await fetchImpl(SQX_BUILDER_CONFIG_API_PATH, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || `Native Builder CrossChecks request failed: ${response?.status ?? "unknown"}`);
  return crossChecksConfigurationFromBuilderConfig(payload);
}

function flattenNativeNodes(node, path = "CrossChecks", rows = []) {
  rows.push({ path, node });
  node.children.forEach((child, index) => {
    flattenNativeNodes(child, `${path}/${child.tag}[${index}]`, rows);
  });
  return rows;
}

export function renderNativeBuilderCrossChecks(crossChecks) {
  if (!crossChecks) return "";
  const root = crossChecks.producer_configuration;
  if (root === null) {
    return '<section data-native-builder-cross-checks><div class="requirement-item"><div><strong>Native CrossChecks configuration</strong><span class="status-badge status-unavailable"><span class="status-dot"></span>CrossChecks absent</span></div><p>The exact current Builder task does not expose a CrossChecks subtree. TraderCockpit does not invent a validation profile or defaults.</p></div></section>';
  }
  const nodes = flattenNativeNodes(root);
  const rows = nodes.map(({ path, node }) => {
    const attributes = Object.entries(node.attributes).map(
      ([key, value]) => `<div class="stat-row"><span>${escapeHtml(key)}</span><code>${escapeHtml(value)}</code></div>`,
    ).join("");
    const text = node.text === null
      ? ""
      : `<div class="stat-row"><span>Text</span><code>${escapeHtml(node.text)}</code></div>`;
    return `<div class="requirement-item" data-native-cross-check-node="${escapeHtml(path)}"><div><strong>${escapeHtml(node.tag)}</strong><span class="field-help">${escapeHtml(path)}</span></div>${attributes}${text}</div>`;
  }).join("");
  return `<section data-native-builder-cross-checks><div class="context-callout"><span class="callout-icon">↳</span><div><span class="eyebrow">Native CrossChecks configuration</span><strong>Exact current SQX CrossChecks structure</strong><span>Read-only producer structure from the same immutable Builder project snapshot. Native tag names and values are shown without assigning validation-method taxonomy, pass/fail meaning, profile defaults, threshold meaning, robustness scoring, or execution semantics.</span></div></div><div class="idea-identity"><div class="stat-row"><span>Native nodes</span><code>${nodes.length}</code></div><div class="stat-row"><span>Literal use compatibility flag</span><code>${crossChecks.enabled ? "true" : "false"}</code></div><div class="stat-row"><span>Source member</span><code>${escapeHtml(crossChecks.source.member)}</code></div><div class="stat-row"><span>Archive SHA-256</span><code>${escapeHtml(crossChecks.source.archive_sha256)}</code></div></div><p class="field-help">The use flag is only the existing Specification applicability fact. It is not a validation result. TraderCockpit does not edit, normalize, classify, execute, score, or judge this structure; StrategyQuant X remains authoritative.</p><div class="requirement-list">${rows}</div></section>`;
}

function specificationRoute() {
  if (globalThis.location?.pathname !== "/research") return false;
  const params = new URLSearchParams(globalThis.location.search || "");
  return params.get("stage") === "construct" && params.get("tab") === "specification";
}

let generation = 0;
let boundHost = null;

async function bindCrossChecksInspector() {
  if (!specificationRoute()) return;
  const host = document.querySelector('[data-research-capability="builder_native_specification"]');
  if (!host || host === boundHost) return;
  boundHost = host;
  const workspace = document.createElement("div");
  workspace.dataset.nativeBuilderCrossChecksWorkspace = "loading";
  workspace.innerHTML = '<p class="field-help" data-native-builder-cross-checks-status>Reading exact native CrossChecks structure…</p>';
  host.append(workspace);
  const myGeneration = ++generation;
  try {
    const crossChecks = await fetchNativeBuilderCrossChecks();
    if (myGeneration !== generation || !specificationRoute() || !workspace.isConnected) return;
    workspace.dataset.nativeBuilderCrossChecksWorkspace = "loaded";
    workspace.innerHTML = renderNativeBuilderCrossChecks(crossChecks);
  } catch (error) {
    if (myGeneration !== generation || !specificationRoute() || !workspace.isConnected) return;
    workspace.dataset.nativeBuilderCrossChecksWorkspace = "failed";
    workspace.innerHTML = `<p class="idea-save-status">${escapeHtml(error instanceof Error ? error.message : "Native Builder CrossChecks unavailable")}</p>`;
  }
}

if (typeof document !== "undefined") {
  const observer = new MutationObserver(() => {
    if (!specificationRoute()) {
      generation += 1;
      boundHost = null;
      return;
    }
    void bindCrossChecksInspector();
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void bindCrossChecksInspector();
}
