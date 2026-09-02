import { researchLocationMatches } from "./model.mjs";
const SQX_BUILDER_CONFIG_API_PATH = "/api/sqx-builder-config";
const BUILDER_PROJECT_PATH = "user/projects/Builder/project.cfx";
const BUILDER_TASK_ENTRY = "Build-Task1.xml";
const BUILDER_CONFIG_SCHEMA = "tc.sqx-builder-config.v1";
export const BUILDER_RANKINGS_SCHEMA = "tc.sqx-builder-rankings.v1";

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function countNativeNodes(node) {
  return 1 + (node.children || []).reduce((sum, child) => sum + countNativeNodes(child), 0);
}

function isPlainObject(value) {
  return Boolean(value && typeof value === "object" && !Array.isArray(value));
}

function digest(value) {
  return typeof value === "string" && /^[0-9a-f]{64}$/i.test(value) ? value : "";
}

function validateNativeNode(node, path = "rankings.producer_configuration") {
  if (
    !isPlainObject(node)
    || typeof node.tag !== "string" || !node.tag
    || !isPlainObject(node.attributes)
    || Object.entries(node.attributes).some(([key, value]) => !key || typeof value !== "string")
    || !(node.text === null || typeof node.text === "string")
    || !Array.isArray(node.children)
  ) {
    throw new Error(`Native Rankings node mismatch at ${path}`);
  }
  node.children.forEach((child, index) => validateNativeNode(child, `${path}.children[${index}]`));
  return node;
}

export function rankingsConfigurationFromBuilderConfig(payload) {
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
    throw new Error("Builder Rankings source identity mismatch");
  }

  const rankings = payload.rankings;
  if (
    !isPlainObject(rankings)
    || rankings.schema !== BUILDER_RANKINGS_SCHEMA
    || rankings.authority !== "native_sqx_read_only"
    || !isPlainObject(rankings.source)
    || rankings.source.source_build !== payload.source_build
    || rankings.source.project !== payload.project
    || rankings.source.relative_path !== payload.source_relative_path
    || rankings.source.archive_sha256 !== payload.archive_sha256
    || rankings.source.member !== BUILDER_TASK_ENTRY
    || !isPlainObject(rankings.semantics)
    || rankings.semantics.interpreted_by_tradercockpit !== false
    || rankings.semantics.owner !== "StrategyQuant X"
    || typeof rankings.semantics.description !== "string" || !rankings.semantics.description
    || !isPlainObject(rankings.execution)
    || rankings.execution.available !== false
    || rankings.execution.reason !== "native_sqx_builder_owns_ranking_configuration"
  ) {
    throw new Error("Native Builder Rankings schema mismatch");
  }

  if (rankings.producer_configuration !== null) {
    validateNativeNode(rankings.producer_configuration);
    if (rankings.producer_configuration.tag !== "Rankings") {
      throw new Error("Native Builder Rankings root must be Rankings");
    }
  }
  return rankings;
}

async function readJson(response) {
  try { return await response.json(); } catch { return null; }
}

export async function fetchNativeBuilderRankings(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Native Builder Rankings fetch is unavailable");
  const response = await fetchImpl(SQX_BUILDER_CONFIG_API_PATH, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || `Native Builder Rankings request failed: ${response?.status ?? "unknown"}`);
  return rankingsConfigurationFromBuilderConfig(payload);
}

function flattenNativeNodes(node, path = "Rankings", rows = []) {
  rows.push({ path, node });
  node.children.forEach((child, index) => {
    flattenNativeNodes(child, `${path}/${child.tag}[${index}]`, rows);
  });
  return rows;
}

export function renderNativeBuilderRankings(rankings) {
  if (!rankings) return "";
  const root = rankings.producer_configuration;
  if (root === null) {
    return '<section data-native-builder-rankings><div class="requirement-item"><div><strong>Native ranking / stop configuration</strong><span class="status-badge status-unavailable"><span class="status-dot"></span>Rankings absent</span></div><p>The exact current Builder task does not expose a Rankings subtree. TraderCockpit does not invent defaults.</p></div></section>';
  }
  const nodes = flattenNativeNodes(root);
  const rows = nodes.map(({ path, node }) => {
    const attributes = Object.entries(node.attributes).map(
      ([key, value]) => `<div class="stat-row"><span>${escapeHtml(key)}</span><code>${escapeHtml(value)}</code></div>`,
    ).join("");
    const text = node.text === null
      ? ""
      : `<div class="stat-row"><span>Text</span><code>${escapeHtml(node.text)}</code></div>`;
    return `<div class="requirement-item" data-native-ranking-node="${escapeHtml(path)}"><div><strong>${escapeHtml(node.tag)}</strong><span class="field-help">${escapeHtml(path)}</span></div>${attributes}${text}</div>`;
  }).join("");
  return `<section data-native-builder-rankings><div class="context-callout"><span class="callout-icon">↳</span><div><span class="eyebrow">Native ranking / stop configuration</span><strong>Exact current SQX Rankings structure</strong><span>Read-only producer structure from the same immutable Builder project snapshot. Native tag names and values are shown without assigning a fitness objective, maximize/minimize direction, threshold interpretation, selection algorithm, or stop semantics.</span></div></div><div class="idea-identity"><div class="stat-row"><span>Native nodes</span><code>${nodes.length}</code></div><div class="stat-row"><span>Source member</span><code>${escapeHtml(rankings.source.member)}</code></div><div class="stat-row"><span>Archive SHA-256</span><code>${escapeHtml(rankings.source.archive_sha256)}</code></div></div><p class="field-help">TraderCockpit does not edit, normalize, score, rank, select, or execute this structure. StrategyQuant X remains authoritative for objectives, directionality, filters, thresholds, selection rules, and stopping behavior.</p><div class="requirement-list">${rows}</div></section>`;
}

function specificationRoute() {
  return researchLocationMatches(globalThis.location, "signals", "signals");
}

let generation = 0;
let boundHost = null;

async function bindRankingsInspector() {
  if (!specificationRoute()) return;
  const host = document.querySelector('[data-research-capability="builder_native_specification"]');
  if (!host || host === boundHost) return;
  boundHost = host;
  const workspace = document.createElement("div");
  workspace.dataset.nativeBuilderRankingsWorkspace = "loading";
  workspace.innerHTML = '<p class="field-help" data-native-builder-rankings-status>Reading exact native Rankings structure…</p>';
  host.append(workspace);
  const myGeneration = ++generation;
  try {
    const rankings = await fetchNativeBuilderRankings();
    if (myGeneration !== generation || !specificationRoute() || !workspace.isConnected) return;
    workspace.dataset.nativeBuilderRankingsWorkspace = "loaded";
    const nodeCount = rankings.producer_configuration ? countNativeNodes(rankings.producer_configuration) : 0;
    workspace.innerHTML = `<details class="native-tree"><summary><span class="native-tree-title">${escapeHtml("Native ranking / stop configuration")}</span><span class="native-tree-meta">${nodeCount} native nodes · exact SQX <code>Rankings</code> subtree</span></summary>${renderNativeBuilderRankings(rankings)}</details>`;
  } catch (error) {
    if (myGeneration !== generation || !specificationRoute() || !workspace.isConnected) return;
    workspace.dataset.nativeBuilderRankingsWorkspace = "failed";
    workspace.innerHTML = `<p class="idea-save-status">${escapeHtml(error instanceof Error ? error.message : "Native Builder Rankings unavailable")}</p>`;
  }
}

if (typeof document !== "undefined") {
  const observer = new MutationObserver(() => {
    if (!specificationRoute()) {
      generation += 1;
      boundHost = null;
      return;
    }
    void bindRankingsInspector();
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void bindRankingsInspector();
}
