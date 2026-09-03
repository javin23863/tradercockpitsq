import { researchLocationMatches } from "./model.mjs";
const SQX_PRESETS_API_PATH = "/api/sqx-presets";
const PRESET_CATALOG_SCHEMA = "tc.sqx-preset-catalog.v1";
const SQX_BUILD = "144.2953";

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function digest(value) {
  return typeof value === "string" && /^[0-9a-f]{64}$/.test(value) ? value : "";
}

export function presetCatalogFromPayload(payload) {
  if (
    !payload
    || payload.schema !== PRESET_CATALOG_SCHEMA
    || payload.source_build !== SQX_BUILD
    || !Array.isArray(payload.presets)
  ) {
    throw new Error("Native preset catalog is invalid");
  }

  const ids = new Set();
  for (const preset of payload.presets) {
    const runtime = preset?.runtime;
    if (
      !preset
      || typeof preset.preset_id !== "string" || !preset.preset_id
      || ids.has(preset.preset_id)
      || typeof preset.label !== "string" || !preset.label
      || typeof preset.market !== "string" || !preset.market
      || preset.source_build !== SQX_BUILD
      || typeof preset.source_relative_path !== "string" || !preset.source_relative_path
      || !digest(preset.source_sha256)
      || !runtime || typeof runtime.available !== "boolean"
      || typeof runtime.status !== "string" || !runtime.status
      || (runtime.verified_sha256 !== null && !digest(runtime.verified_sha256))
      || (runtime.observed_build !== null && runtime.observed_build !== SQX_BUILD)
    ) {
      throw new Error("Native preset catalog entry is invalid");
    }
    if (
      runtime.available
      && (runtime.status !== "verified" || runtime.verified_sha256 !== preset.source_sha256 || runtime.observed_build !== SQX_BUILD)
    ) {
      throw new Error("Native preset verified state is inconsistent");
    }
    ids.add(preset.preset_id);
  }
  return payload;
}

async function readJson(response) {
  try { return await response.json(); } catch { return null; }
}

export async function fetchPresetCatalog(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Native preset catalog fetch is unavailable");
  const response = await fetchImpl(SQX_PRESETS_API_PATH, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || `Native preset catalog request failed: ${response?.status ?? "unknown"}`);
  return presetCatalogFromPayload(payload);
}

function readable(value) {
  return String(value || "unavailable").replaceAll("_", " ").replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export function renderPresetCatalog(payload) {
  const catalog = presetCatalogFromPayload(payload);
  const rows = catalog.presets.length
    ? catalog.presets.map((preset) => {
      const runtime = preset.runtime;
      const tone = runtime.available ? "ready" : "unavailable";
      return `<div class="requirement-item" data-native-preset="${escapeHtml(preset.preset_id)}"><div><strong>${escapeHtml(preset.label)}</strong><span class="status-badge status-${tone}"><span class="status-dot"></span>${escapeHtml(readable(runtime.status))}</span></div><div class="stat-row"><span>Market</span><code>${escapeHtml(preset.market)}</code></div><div class="stat-row"><span>Native source</span><code>${escapeHtml(preset.source_relative_path)}</code></div><div class="stat-row"><span>Source SHA-256</span><code>${escapeHtml(preset.source_sha256)}</code></div><p class="field-help">Source-bound preset inspection only. TraderCockpit does not infer that this preset is bound to the current Builder project.</p></div>`;
    }).join("")
    : '<p class="field-help">The canonical native preset catalog is empty.</p>';
  return `<div data-native-preset-catalog-result><div class="context-callout"><span class="callout-icon">↳</span><div><span class="eyebrow">Native preset catalog</span><strong>Read-only source inspection</strong><span>Preset identity and runtime-file verification are shown separately from Builder project validity or preset binding.</span></div></div><div class="requirement-list">${rows}</div></div>`;
}

function specificationRoute() {
  return researchLocationMatches(globalThis.location, "signals", "signals")
    || researchLocationMatches(globalThis.location, "catalog", "utilities")
    || (globalThis.location?.pathname === "/custom-projects")
    || (globalThis.location?.pathname === "/automation");
}

let generation = 0;
let boundHost = null;

function capabilityHost() {
  if (!specificationRoute()) return null;
  return document.querySelector('[data-research-capability="native_preset_inspection"]');
}

async function bindPresetInspector() {
  const host = capabilityHost();
  if (!host || host === boundHost) return;
  boundHost = host;
  const myGeneration = ++generation;
  const workspace = document.createElement("div");
  workspace.dataset.nativePresetCatalogWorkspace = "loading";
  workspace.innerHTML = '<p class="idea-save-status" data-native-preset-status>Reading native preset catalog…</p><div data-native-preset-result></div>';
  host.append(workspace);
  try {
    const catalog = await fetchPresetCatalog();
    if (myGeneration !== generation || !specificationRoute() || !workspace.isConnected) return;
    workspace.dataset.nativePresetCatalogWorkspace = "loaded";
    workspace.querySelector("[data-native-preset-status]").textContent = "Native preset catalog loaded.";
    workspace.querySelector("[data-native-preset-result]").innerHTML = renderPresetCatalog(catalog);
  } catch (error) {
    if (myGeneration !== generation || !specificationRoute() || !workspace.isConnected) return;
    workspace.dataset.nativePresetCatalogWorkspace = "failed";
    workspace.querySelector("[data-native-preset-status]").textContent = error instanceof Error ? error.message : "Native preset catalog unavailable";
  }
}

if (typeof document !== "undefined") {
  const observer = new MutationObserver(() => {
    if (!specificationRoute()) {
      generation += 1;
      boundHost = null;
      return;
    }
    void bindPresetInspector();
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void bindPresetInspector();
}
