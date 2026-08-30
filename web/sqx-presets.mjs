const SQX_PRESETS_API_PATH = "/api/sqx-presets";
const SQX_PRESET_SCHEMA = "tc.sqx-preset-catalog.v1";

let catalogPromise = null;

export function normalizePresetCatalog(payload) {
  if (!payload || typeof payload !== "object" || payload.schema !== SQX_PRESET_SCHEMA) {
    throw new Error("Unexpected SQX preset catalog schema");
  }
  if (!Array.isArray(payload.presets) || payload.presets.length === 0) {
    throw new Error("SQX preset catalog is empty");
  }

  const seen = new Set();
  const presets = payload.presets.map((preset) => {
    if (!preset || typeof preset !== "object") throw new Error("Invalid SQX preset record");
    const required = ["preset_id", "label", "market", "source_build", "source_relative_path", "source_sha256", "reference_commit"];
    for (const key of required) {
      if (typeof preset[key] !== "string" || preset[key].length === 0) {
        throw new Error(`SQX preset is missing ${key}`);
      }
    }
    if (seen.has(preset.preset_id)) throw new Error("Duplicate SQX preset id");
    seen.add(preset.preset_id);
    const runtime = preset.runtime && typeof preset.runtime === "object"
      ? preset.runtime
      : { available: false, status: "runtime_not_configured", verified_sha256: null };
    return { ...preset, runtime };
  });

  return {
    schema: payload.schema,
    source_build: String(payload.source_build || ""),
    reference_commit: String(payload.reference_commit || ""),
    presets,
  };
}

export function selectedPresetId(search = "") {
  return new URLSearchParams(search).get("presetId") || "";
}

export function presetSelectionPath(pathname, search, presetId) {
  const params = new URLSearchParams(search);
  params.set("presetId", presetId);
  const query = params.toString();
  return query ? `${pathname}?${query}` : pathname;
}

export async function fetchSqxPresetCatalog(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Fetch is not available");
  const response = await fetchImpl(SQX_PRESETS_API_PATH, { headers: { accept: "application/json" } });
  if (!response.ok) throw new Error(`SQX preset catalog request failed (${response.status})`);
  return normalizePresetCatalog(await response.json());
}

function runtimeLabel(runtime) {
  if (runtime?.available === true && runtime.status === "verified") return "Runtime verified";
  if (runtime?.status === "hash_mismatch") return "Runtime hash mismatch";
  if (runtime?.status === "preset_missing") return "Preset missing from runtime";
  return "Runtime verification pending";
}

function makeText(tag, text, className = "") {
  const node = document.createElement(tag);
  if (className) node.className = className;
  node.textContent = text;
  return node;
}

function updateRunSurface(runSurface, preset) {
  if (!runSurface) return;
  runSurface.dataset.sqxPresetId = preset.preset_id;
  runSurface.dataset.sqxPresetMarket = preset.market;
  const profile = runSurface.querySelector(".run-field strong");
  if (profile) profile.textContent = `${preset.label} · SQX ${preset.source_build}`;
}

function renderCatalog(panel, catalog, runSurface) {
  panel.replaceChildren();

  const heading = document.createElement("div");
  heading.className = "panel-heading";
  const headingCopy = document.createElement("div");
  headingCopy.append(makeText("p", "StrategyQuant X presets", "eyebrow"));
  headingCopy.append(makeText("h2", "Choose the source pipeline profile"));
  heading.append(headingCopy);
  const status = makeText("span", `SQX ${catalog.source_build}`, "status-badge status-pending");
  heading.append(status);
  panel.append(heading);
  panel.append(makeText(
    "p",
    "These profiles are tied to the reviewed StrategyQuant X preset files and hashes. Selecting one configures Run Setup; it does not launch compute or create a strategy by itself.",
    "panel-description",
  ));

  const grid = document.createElement("div");
  grid.className = "route-card-grid";
  const selected = selectedPresetId(window.location.search);

  for (const preset of catalog.presets) {
    const card = document.createElement("article");
    card.className = "route-card";
    card.dataset.accent = preset.market === "futures" ? "orange" : preset.market === "stocks" ? "green" : "purple";
    card.dataset.sqxPresetCard = preset.preset_id;

    card.append(makeText("span", preset.market.toUpperCase(), "route-card-eyebrow"));
    card.append(makeText("span", preset.label, "route-card-title"));
    card.append(makeText("span", `Default SQX ${preset.source_build} builder preset`, "route-card-description"));
    card.append(makeText("span", runtimeLabel(preset.runtime), "route-card-description"));
    card.append(makeText("span", `Source ${preset.source_sha256.slice(0, 12)}…`, "route-card-description"));

    const button = document.createElement("button");
    button.type = "button";
    button.className = selected === preset.preset_id ? "button button-primary" : "button button-secondary";
    button.dataset.sqxPresetId = preset.preset_id;
    button.textContent = selected === preset.preset_id ? "Selected" : "Use preset";
    if (preset.runtime?.status === "hash_mismatch") {
      button.disabled = true;
      button.className = "button button-disabled";
      button.title = "The configured SQX runtime preset does not match the reviewed source hash.";
    }
    card.append(button);
    grid.append(card);

    if (selected === preset.preset_id) updateRunSurface(runSurface, preset);
  }

  panel.append(grid);
  panel.dataset.sqxPresetCatalogState = "ready";
}

function renderError(panel, error) {
  panel.replaceChildren();
  panel.append(makeText("p", "StrategyQuant X presets", "eyebrow"));
  panel.append(makeText("h2", "Preset catalog unavailable"));
  panel.append(makeText("p", error instanceof Error ? error.message : String(error), "panel-description"));
  panel.dataset.sqxPresetCatalogState = "error";
}

function ensurePresetPanel(root = document) {
  const runSurface = root.querySelector('[data-run-surface-id="shared-run-surface"]');
  if (!runSurface) return;

  let panel = root.querySelector("[data-sqx-preset-panel]");
  if (!panel) {
    panel = document.createElement("section");
    panel.className = "panel";
    panel.dataset.accent = "purple";
    panel.dataset.sqxPresetPanel = "true";
    panel.dataset.sqxPresetCatalogState = "loading";
    panel.append(makeText("p", "Loading source-bound SQX presets…", "panel-description"));
    runSurface.before(panel);
  }

  const currentState = panel.dataset.sqxPresetCatalogState;
  if (currentState === "ready" || currentState === "error") return;

  if (!catalogPromise) catalogPromise = fetchSqxPresetCatalog();
  catalogPromise
    .then((catalog) => renderCatalog(panel, catalog, runSurface))
    .catch((error) => renderError(panel, error));
}

export function bootSqxPresetIntegration(root = document.querySelector("#app")) {
  if (!root || typeof MutationObserver === "undefined") return;

  const hydrate = () => ensurePresetPanel(root);
  const observer = new MutationObserver(hydrate);
  observer.observe(root, { childList: true, subtree: true });

  root.addEventListener("click", (event) => {
    const button = event.target.closest("[data-sqx-preset-id]");
    if (!button || button.matches(":disabled")) return;
    const presetId = button.dataset.sqxPresetId;
    const path = presetSelectionPath(window.location.pathname, window.location.search, presetId);
    window.history.pushState({}, "", path);
    window.dispatchEvent(new PopStateEvent("popstate"));
  });

  hydrate();
}

if (typeof document !== "undefined") bootSqxPresetIntegration();
