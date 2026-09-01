const SQX_BUILDER_CONFIG_API_PATH = "/api/sqx-builder-config";
const BUILDER_PROJECT_PATH = "user/projects/Builder/project.cfx";
const BUILDER_TASK_ENTRY = "Build-Task1.xml";
const BUILDER_CONFIG_SCHEMA = "tc.sqx-builder-config.v1";
export const BUILDER_BLOCKS_SCHEMA = "tc.sqx-builder-blocks.v1";

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

function validateNativeNode(node, path = "blocks.producer_configuration") {
  if (
    !isPlainObject(node)
    || typeof node.tag !== "string" || !node.tag
    || !isPlainObject(node.attributes)
    || Object.entries(node.attributes).some(([key, value]) => !key || typeof value !== "string")
    || !(node.text === null || typeof node.text === "string")
    || !Array.isArray(node.children)
  ) {
    throw new Error(`Native Blocks node mismatch at ${path}`);
  }
  node.children.forEach((child, index) => validateNativeNode(child, `${path}.children[${index}]`));
  return node;
}

export function blocksConfigurationFromBuilderConfig(payload) {
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
    throw new Error("Builder Blocks source identity mismatch");
  }

  const blocks = payload.blocks;
  if (
    !isPlainObject(blocks)
    || blocks.schema !== BUILDER_BLOCKS_SCHEMA
    || blocks.authority !== "native_sqx_read_only"
    || !isPlainObject(blocks.source)
    || blocks.source.source_build !== payload.source_build
    || blocks.source.project !== payload.project
    || blocks.source.relative_path !== payload.source_relative_path
    || blocks.source.archive_sha256 !== payload.archive_sha256
    || blocks.source.member !== BUILDER_TASK_ENTRY
    || !isPlainObject(blocks.semantics)
    || blocks.semantics.interpreted_by_tradercockpit !== false
    || blocks.semantics.owner !== "StrategyQuant X"
    || typeof blocks.semantics.description !== "string" || !blocks.semantics.description
    || !isPlainObject(blocks.execution)
    || blocks.execution.available !== false
    || blocks.execution.reason !== "native_sqx_builder_owns_block_configuration"
  ) {
    throw new Error("Native Builder Blocks schema mismatch");
  }

  if (blocks.producer_configuration !== null) {
    validateNativeNode(blocks.producer_configuration);
    if (blocks.producer_configuration.tag !== "Blocks") {
      throw new Error("Native Builder Blocks root must be Blocks");
    }
  }
  return blocks;
}

async function readJson(response) {
  try { return await response.json(); } catch { return null; }
}

export async function fetchNativeBuilderBlocks(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Native Builder Blocks fetch is unavailable");
  const response = await fetchImpl(SQX_BUILDER_CONFIG_API_PATH, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || `Native Builder Blocks request failed: ${response?.status ?? "unknown"}`);
  return blocksConfigurationFromBuilderConfig(payload);
}

function flattenNativeNodes(node, path = "Blocks", rows = []) {
  rows.push({ path, node });
  node.children.forEach((child, index) => {
    flattenNativeNodes(child, `${path}/${child.tag}[${index}]`, rows);
  });
  return rows;
}

export function renderNativeBuilderBlocks(blocks) {
  if (!blocks) return "";
  const root = blocks.producer_configuration;
  if (root === null) {
    return '<section data-native-builder-blocks><div class="requirement-item"><div><strong>Native rule / block space</strong><span class="status-badge status-unavailable"><span class="status-dot"></span>Blocks absent</span></div><p>The exact current Builder task does not expose a Blocks subtree.</p></div></section>';
  }
  const nodes = flattenNativeNodes(root);
  const rows = nodes.map(({ path, node }) => {
    const attributes = Object.entries(node.attributes).map(
      ([key, value]) => `<div class="stat-row"><span>${escapeHtml(key)}</span><code>${escapeHtml(value)}</code></div>`,
    ).join("");
    const text = node.text === null
      ? ""
      : `<div class="stat-row"><span>Text</span><code>${escapeHtml(node.text)}</code></div>`;
    return `<div class="requirement-item" data-native-block-node="${escapeHtml(path)}"><div><strong>${escapeHtml(node.tag)}</strong><span class="field-help">${escapeHtml(path)}</span></div>${attributes}${text}</div>`;
  }).join("");
  return `<section data-native-builder-blocks><div class="context-callout"><span class="callout-icon">↳</span><div><span class="eyebrow">Native rule / block space</span><strong>Exact current SQX Blocks structure</strong><span>Read-only producer structure from the same immutable Builder project snapshot. Native tag names and values are shown without assigning indicator, signal, operator, action, or parameter semantics.</span></div></div><div class="idea-identity"><div class="stat-row"><span>Native nodes</span><code>${nodes.length}</code></div><div class="stat-row"><span>Source member</span><code>${escapeHtml(blocks.source.member)}</code></div><div class="stat-row"><span>Archive SHA-256</span><code>${escapeHtml(blocks.source.archive_sha256)}</code></div></div><p class="field-help">TraderCockpit does not edit, classify, normalize, or execute this structure. StrategyQuant X remains authoritative for block families, parameter representations, selection rules, dependencies, and execution.</p><div class="requirement-list">${rows}</div></section>`;
}

function specificationRoute() {
  if (globalThis.location?.pathname !== "/research") return false;
  const params = new URLSearchParams(globalThis.location.search || "");
  return params.get("stage") === "construct" && params.get("tab") === "specification";
}

let generation = 0;
let boundHost = null;

async function bindBlocksInspector() {
  if (!specificationRoute()) return;
  const host = document.querySelector('[data-research-capability="builder_native_specification"]');
  if (!host || host === boundHost) return;
  boundHost = host;
  const workspace = document.createElement("div");
  workspace.dataset.nativeBuilderBlocksWorkspace = "loading";
  workspace.innerHTML = '<p class="field-help" data-native-builder-blocks-status>Reading exact native Blocks structure…</p>';
  host.append(workspace);
  const myGeneration = ++generation;
  try {
    const blocks = await fetchNativeBuilderBlocks();
    if (myGeneration !== generation || !specificationRoute() || !workspace.isConnected) return;
    workspace.dataset.nativeBuilderBlocksWorkspace = "loaded";
    workspace.innerHTML = renderNativeBuilderBlocks(blocks);
  } catch (error) {
    if (myGeneration !== generation || !specificationRoute() || !workspace.isConnected) return;
    workspace.dataset.nativeBuilderBlocksWorkspace = "failed";
    workspace.innerHTML = `<p class="idea-save-status">${escapeHtml(error instanceof Error ? error.message : "Native Builder Blocks unavailable")}</p>`;
  }
}

if (typeof document !== "undefined") {
  const observer = new MutationObserver(() => {
    if (!specificationRoute()) {
      generation += 1;
      boundHost = null;
      return;
    }
    void bindBlocksInspector();
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void bindBlocksInspector();
}
