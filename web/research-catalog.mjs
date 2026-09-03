// Research → Indicators & Models Catalog (prototype screen `indicators-models-catalog`).
// Components are the exact native StrategyQuant X building blocks (signals, indicators,
// stop/limit levels, order types, exit types) read from the current Builder task, the native
// preset templates, and the user's own Ideas/Candidates. Ratings, market fit, timeframe
// fit and performance are shown as "—" because no producer exposes them yet. The Models tab
// mounts the platform-owned Machine Learning / Models modality (`/api/research/models`).

import { researchNavPath, researchWorkspace, researchLocationMatches } from "./model.mjs";
import {
  actionButton,
  card,
  chartFrame,
  chip,
  escapeHtml,
  icon,
  identityRows,
  linkButton,
  pageTitle,
  pillRow,
  tag,
  unavailable,
  viewAll,
} from "./ui.mjs";
import { fetchNativeBuilderView, nativeBlocks } from "./native-config.mjs";
import { fetchPresetCatalog } from "./research-presets.mjs";
import { currentResearchSnapshot } from "./research-snapshot.mjs";

const workspace = researchWorkspace("catalog");
const PAGE_SIZE = 20;

const KIND_META = Object.freeze({
  signals: { label: "Signals", tone: "purple", iconName: "spark", tabs: ["all", "indicators"] },
  indicators: { label: "Indicators", tone: "blue", iconName: "chart", tabs: ["all", "indicators"] },
  stopLimitBlocks: { label: "Stop / Limit Levels", tone: "cyan", iconName: "layers", tabs: ["all", "indicators"] },
  orderTypes: { label: "Order Types", tone: "orange", iconName: "target", tabs: ["all", "utilities"] },
  exitTypes: { label: "Exit Types", tone: "red", iconName: "shield", tabs: ["all", "utilities"] },
  template: { label: "Native Templates", tone: "green", iconName: "bookmark", tabs: ["all", "strategies"] },
  candidate: { label: "Native Strategies", tone: "violet", iconName: "layers", tabs: ["all", "strategies", "mine"] },
  idea: { label: "Ideas", tone: "cyan", iconName: "flask", tabs: ["all", "mine"] },
});

// ---------- component model ----------

export function componentsFromSources({ blocks = null, presets = null, snapshot = null }) {
  const components = [];
  for (const block of nativeBlocks(blocks)) {
    const meta = KIND_META[block.category] || KIND_META[block.section] || { label: block.category, tone: "neutral", iconName: "layers", tabs: ["all", "indicators"] };
    components.push({
      id: `block:${block.section}:${block.key}`,
      name: block.key,
      author: "StrategyQuant X · native block",
      kind: block.category,
      kindLabel: meta.label,
      tone: meta.tone,
      iconName: meta.iconName,
      tabs: meta.tabs,
      enabled: block.enabled,
      detail: Object.entries(block.attributes).map(([key, value]) => [key, value]),
      children: block.node.children.map((child) => `${child.tag}${Object.keys(child.attributes).length ? ` ${Object.entries(child.attributes).map(([key, value]) => `${key}=${value}`).join(" ")}` : ""}`),
    });
  }
  for (const preset of presets?.presets || []) {
    components.push({
      id: `preset:${preset.preset_id}`,
      name: preset.label,
      author: `StrategyQuant X · template · ${preset.market}`,
      kind: "template",
      kindLabel: KIND_META.template.label,
      tone: KIND_META.template.tone,
      iconName: KIND_META.template.iconName,
      tabs: KIND_META.template.tabs,
      enabled: preset.runtime?.available === true,
      detail: [["preset_id", preset.preset_id], ["market", preset.market], ["source_relative_path", preset.source_relative_path], ["source_sha256", preset.source_sha256], ["runtime.status", preset.runtime?.status ?? "—"], ["runtime.available", String(preset.runtime?.available)]],
      children: [],
    });
  }
  for (const candidate of snapshot?.candidates || []) {
    components.push({
      id: `candidate:${candidate.entity_id}`,
      name: candidate.archive_name,
      author: `Imported native survivor · SQX ${candidate.sqx_build}`,
      kind: "candidate",
      kindLabel: KIND_META.candidate.label,
      tone: KIND_META.candidate.tone,
      iconName: KIND_META.candidate.iconName,
      tabs: KIND_META.candidate.tabs,
      enabled: true,
      detail: [["entity_id", candidate.entity_id], ["revision", candidate.revision], ["archive_sha256", candidate.archive_sha256], ["strategy_sha256", candidate.strategy_sha256]],
      children: [],
    });
  }
  for (const idea of snapshot?.ideas || []) {
    components.push({
      id: `idea:${idea.entity_id}`,
      name: idea.summary || "Untitled Idea",
      author: "Your research · immutable Idea revision",
      kind: "idea",
      kindLabel: KIND_META.idea.label,
      tone: KIND_META.idea.tone,
      iconName: KIND_META.idea.iconName,
      tabs: KIND_META.idea.tabs,
      enabled: true,
      detail: [["entity_id", idea.entity_id], ["revision", idea.revision]],
      children: [],
    });
  }
  return components;
}

export function filterComponents(components, { tab = "all", query = "", category = "" }) {
  const needle = query.trim().toLowerCase();
  return components.filter((component) => (
    component.tabs.includes(tab)
    && (!category || component.kind === category)
    && (!needle || component.name.toLowerCase().includes(needle) || component.kindLabel.toLowerCase().includes(needle))
  ));
}

export function categoryCounts(components, tab = "all") {
  const counts = new Map();
  for (const component of components) {
    if (!component.tabs.includes(tab)) continue;
    counts.set(component.kind, (counts.get(component.kind) || 0) + 1);
  }
  return counts;
}

// ---------- rendering ----------

function filterSelect(label, value, { disabled = true, title = "" } = {}) {
  return `<span class="filter-select ${disabled ? "is-disabled" : ""}" ${title ? `title="${escapeHtml(title)}"` : ""}><small>${escapeHtml(label)}</small><strong>${escapeHtml(value)} ${icon("down", { size: 12 })}</strong></span>`;
}

function filtersBar() {
  return `<div class="catalog-filters">
    <label class="catalog-search">${icon("search", { size: 14 })}<input type="search" data-catalog-search placeholder="Search indicators, models, authors, or keywords…" aria-label="Search components" /></label>
    <span data-catalog-category-select>${filterSelect("Category", "All", { disabled: false })}</span>
    ${filterSelect("Market Fit", "All", { title: "Market fit is not exposed by the native block read model" })}
    ${filterSelect("Timeframe", "All", { title: "Timeframe fit is not exposed by the native block read model" })}
    ${filterSelect("Data Type", "All", { title: "Data type is not exposed by the native block read model" })}
    ${filterSelect("Status", "All", { title: "Filter by enabled state is available via Category" })}
    ${actionButton("More Filters", { disabled: true, title: "No further producer-backed filters yet" })}
  </div>`;
}

function sideRail(components, tab, category, view) {
  const counts = categoryCounts(components, tab);
  const total = [...counts.values()].reduce((sum, value) => sum + value, 0);
  const items = [...counts.entries()].map(([kind, count]) => {
    const meta = KIND_META[kind] || { label: kind };
    return `<button type="button" class="side-item ${category === kind ? "is-active" : ""}" data-catalog-category="${escapeHtml(kind)}"><span>${escapeHtml(meta.label)}</span><span class="count">${count}</span></button>`;
  }).join("");
  const timeframes = [...new Set((view?.charts || []).map((chart) => chart.timeframe).filter(Boolean))];
  const instruments = view?.instruments || [];
  return `<div class="stack">
    <div class="card"><div class="card-body"><strong>${total} component${total === 1 ? "" : "s"} found</strong></div></div>
    <div class="card"><div class="card-body">
      <div class="side-list"><div class="side-list-title">Categories</div><button type="button" class="side-item ${category ? "" : "is-active"}" data-catalog-category=""><span>All Categories</span><span class="count">${total}</span></button>${items}</div>
      <div class="side-list"><div class="side-list-title">Market fit</div>${instruments.length ? instruments.map((row) => `<div class="check-item"><i></i><span>${escapeHtml(row.instrument)}</span><span class="count">${escapeHtml(row.data_type ? `type ${row.data_type}` : "")}</span></div>`).join("") : `<p class="note">No native instruments in the current Builder task.</p>`}<p class="note">Instruments from the exact Builder data setup; per-component market fit is not exposed.</p></div>
      <div class="side-list"><div class="side-list-title">Timeframe</div><div class="timeframe-row">${["All", ...timeframes].map((timeframe, index) => `<span class="${index === 0 ? "is-active" : ""}">${escapeHtml(timeframe)}</span>`).join("")}</div><p class="note">Timeframes from the native chart setup.</p></div>
    </div></div>
  </div>`;
}

function componentRow(component, selected) {
  return `<tr class="${selected ? "is-selected" : ""}" data-catalog-component="${escapeHtml(component.id)}" tabindex="0">
    <td><span class="icon-button" style="width:26px;height:26px;border-radius:7px" aria-hidden="true">${icon(selected ? "check" : "plus", { size: 13 })}</span></td>
    <td><div class="list-row" style="padding:0;border:0"><span class="row-icon tone-${escapeHtml(component.tone)}">${icon(component.iconName, { size: 15 })}</span><span class="row-title"><strong>${escapeHtml(component.name)}</strong><span>${escapeHtml(component.author)}</span></span></div></td>
    <td>${tag(component.kindLabel, component.tone)}</td>
    <td><span class="tone-text-dim" title="Market fit is not exposed by the producer">—</span></td>
    <td><span class="tone-text-dim" title="Timeframe fit is not exposed by the producer">—</span></td>
    <td class="is-right"><span class="tone-text-dim" title="No rating producer">—</span></td>
    <td class="is-right">${chip(component.enabled ? "Enabled" : "Off", component.enabled ? "ready" : "unavailable")}</td>
  </tr>`;
}

function componentTable(visible, page, selectedId, total) {
  const pages = Math.max(1, Math.ceil(total / PAGE_SIZE));
  const start = (page - 1) * PAGE_SIZE;
  const rows = visible.map((component) => componentRow(component, component.id === selectedId)).join("")
    || `<tr class="table-empty"><td colspan="7">No components match. Native blocks come from the current Builder task; templates from the installed runtime; strategies and ideas from your custody.</td></tr>`;
  const pageButtons = Array.from({ length: pages }, (_, index) => index + 1)
    .filter((number) => number <= 3 || number > pages - 2 || Math.abs(number - page) <= 1)
    .reduce((items, number, index, array) => {
      if (index > 0 && number - array[index - 1] > 1) items.push(`<button type="button" disabled>…</button>`);
      items.push(`<button type="button" class="${number === page ? "is-active" : ""}" data-catalog-page="${number}">${number}</button>`);
      return items;
    }, [])
    .join("");
  return `<div class="card catalog-table"><div class="card-body is-tight">
    <div class="table-wrap"><table class="data-table"><thead><tr><th></th><th>Component</th><th>Category</th><th>Market Fit</th><th>Timeframe</th><th class="is-right">Rating</th><th class="is-right">State</th></tr></thead><tbody>${rows}</tbody></table></div>
    <div class="pager" style="padding:10px 14px"><div class="pages"><button type="button" data-catalog-page="${Math.max(1, page - 1)}" ${page === 1 ? "disabled" : ""}>‹</button>${pageButtons}<button type="button" data-catalog-page="${Math.min(pages, page + 1)}" ${page >= pages ? "disabled" : ""}>›</button></div><span>Showing ${total ? start + 1 : 0}–${Math.min(total, start + visible.length)} of ${total}</span></div>
  </div></div>`;
}

function detailPanel(component) {
  if (!component) {
    return card({ title: "Component detail", accent: "neutral", body: unavailable("Select a component", "Pick a native block, template, strategy or idea to inspect its exact producer attributes.", { compact: true }) });
  }
  const actions = component.kind === "candidate"
    ? linkButton(researchNavPath("validate", "initial-test"), "Run Initial Test", { primary: true, iconName: "play" })
    : component.kind === "idea"
      ? linkButton(researchNavPath("signals", "overview"), "Open Idea", { primary: true, iconName: "flask" })
      : actionButton("Add to Strategy", { primary: true, iconName: "plus", disabled: true, title: "StrategyQuant X owns block enabling; TraderCockpit reflects the exact current task" });
  return `<article class="card is-highlight"><div class="card-body">
    <div class="component-detail-head"><span class="row-icon tone-${escapeHtml(component.tone)}">${icon(component.iconName, { size: 20 })}</span><div><h3>${escapeHtml(component.name)}</h3><p>${escapeHtml(component.author)}</p><div class="row-tags" style="margin-top:6px">${tag(component.kindLabel, component.tone)}${chip(component.enabled ? "Enabled in current task" : "Off in current task", component.enabled ? "ready" : "unavailable")}</div></div></div>
    <p class="note">Exact native attributes; TraderCockpit does not assign indicator, signal, operator or parameter semantics.</p>
    <p class="detail-section-title">Native attributes</p>
    ${identityRows(component.detail)}
    ${component.children.length ? `<p class="detail-section-title">Native children</p><div class="row-tags">${component.children.map((child) => tag(child))}</div>` : ""}
    <p class="detail-section-title">Market fit</p><div class="row-tags">${["Equities", "Futures", "Crypto"].map((market) => `<span class="pill is-disabled" title="Not exposed by the producer">${escapeHtml(market)}</span>`).join("")}</div>
    <p class="detail-section-title">Timeframe</p><div class="timeframe-row">${["5m", "15m", "30m", "1h", "4h", "1D"].map((tf) => `<span title="Timeframe fit is not exposed by the producer">${tf}</span>`).join("")}</div>
    <p class="detail-section-title">Performance <small class="tone-text-dim">(Backtest)</small></p>
    <div class="metric-grid">${["Sharpe", "Sortino", "Win Rate", "Profit Factor"].map((label) => `<div class="metric"><span>${label}</span><strong class="is-empty">—</strong></div>`).join("")}</div>
    ${chartFrame({ height: 60, state: "unavailable", detail: "No per-component performance producer.", yLabels: [] })}
    <p class="detail-section-title">Actions</p>
    <div class="row-tags">${actions}${linkButton(researchNavPath("signals", "signals"), "View in Specification", { iconName: "code" })}${actionButton("Compare", { iconName: "compare", disabled: true, title: "Compare needs a performance producer" })}</div>
  </div></article>`;
}

function renderCatalogBody(state, route) {
  const filtered = filterComponents(state.components, { tab: route.tabId, query: state.query, category: state.category });
  const pages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const page = Math.min(state.page, pages);
  const visible = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);
  const selected = filtered.find((component) => component.id === state.selectedId) || visible[0] || null;
  return `<div class="with-left-rail">${sideRail(state.components, route.tabId, state.category, state.view)}<div class="with-rail-wide with-rail">${componentTable(visible, page, selected?.id, filtered.length)}${detailPanel(selected)}</div></div>`;
}

function modelsTab() {
  return `<div class="with-rail">${card({
    title: "Machine Learning / Models",
    sub: "Platform-owned sklearn modality · native-trade features",
    headIcon: "bot",
    accent: "purple",
    body: `<div data-ml-models></div>`,
  })}${card({ title: "Native search modalities", accent: "neutral", body: `<p class="note">Random Discovery and Genetic Evolution are native StrategyQuant X Builder modes; inspect the exact selected mode in Evolutionary Search.</p>`, footer: viewAll(researchNavPath("evolution"), "Open Evolutionary Search") })}</div>`;
}

export function renderCatalogWorkspace(route, { snapshotState }) {
  const pills = pillRow(workspace.tabs, route.tabId, (tab) => researchNavPath("catalog", tab.id));
  const actions = actionButton("Publish Component", { iconName: "plus", disabled: true, title: "Component publishing needs the capability registry (not connected)" });
  const body = route.tabId === "models"
    ? modelsTab()
    : `${filtersBar()}<div data-catalog-root data-catalog-tab="${escapeHtml(route.tabId)}">${unavailable("Reading native block space…", "Exact StrategyQuant X building blocks, native templates and your custody records.", { tone: "pending", compact: true })}</div>${route.tabId === "utilities" ? utilitiesHosts() : ""}`;
  void snapshotState;
  return `${pageTitle(workspace.title, { subtitle: "Discover, evaluate, and add professional indicators, models, and research components.", actions })}${pills}${body}`;
}

function utilitiesHosts() {
  return `<div class="grid grid-2">
    ${card({ title: "Native project topology", sub: "Read-only custody of one saved StrategyQuant X project", headIcon: "table", accent: "neutral", body: `<div data-research-capability="native_custom_project_topology"></div>` })}
    ${card({ title: "Native preset templates", sub: "Installed runtime template verification", headIcon: "bookmark", accent: "neutral", body: `<div data-research-capability="native_preset_inspection"></div>` })}
  </div>`;
}

// ---------- binder ----------

function catalogRoute() {
  return researchLocationMatches(globalThis.location, "catalog");
}

const state = { components: [], view: null, query: "", category: "", page: 1, selectedId: "", phase: "idle" };
let boundRoot = null;
let generation = 0;

function currentRouteTab() {
  return document.querySelector("[data-catalog-root]")?.getAttribute("data-catalog-tab") || "all";
}

function paint() {
  const root = document.querySelector("[data-catalog-root]");
  if (!root) return;
  if (state.phase === "failed") {
    root.innerHTML = unavailable("Native block space unavailable", state.detail || "Native Builder configuration read failed", { tone: "error", compact: true });
    return;
  }
  root.innerHTML = renderCatalogBody(state, { tabId: currentRouteTab() });
}

async function bindCatalog() {
  if (!catalogRoute()) { boundRoot = null; return; }
  const root = document.querySelector("[data-catalog-root]");
  if (!root || root === boundRoot) return;
  boundRoot = root;
  const current = ++generation;
  const [viewSettled, presetsSettled] = await Promise.allSettled([fetchNativeBuilderView(), fetchPresetCatalog()]);
  if (current !== generation || !root.isConnected) return;
  const view = viewSettled.status === "fulfilled" ? viewSettled.value : null;
  const presets = presetsSettled.status === "fulfilled" ? presetsSettled.value : null;
  if (!view && !presets) {
    state.phase = "failed";
    state.detail = viewSettled.reason instanceof Error ? viewSettled.reason.message : "Native Builder configuration read failed";
  } else {
    state.phase = "loaded";
    state.view = view;
    state.components = componentsFromSources({ blocks: view?.blocks, presets, snapshot: currentResearchSnapshot() });
  }
  paint();
}

if (typeof document !== "undefined") {
  const observer = new MutationObserver(() => { void bindCatalog(); });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  document.addEventListener("input", (event) => {
    if (!catalogRoute() || !event.target?.matches?.("[data-catalog-search]")) return;
    state.query = event.target.value || "";
    state.page = 1;
    paint();
  });
  document.addEventListener("click", (event) => {
    if (!catalogRoute()) return;
    const categoryButton = event.target.closest?.("[data-catalog-category]");
    if (categoryButton) {
      state.category = categoryButton.getAttribute("data-catalog-category") || "";
      state.page = 1;
      paint();
      return;
    }
    const pageButton = event.target.closest?.("[data-catalog-page]");
    if (pageButton && !pageButton.disabled) {
      state.page = Number.parseInt(pageButton.getAttribute("data-catalog-page"), 10) || 1;
      paint();
      return;
    }
    const row = event.target.closest?.("[data-catalog-component]");
    if (row) {
      state.selectedId = row.getAttribute("data-catalog-component") || "";
      paint();
    }
  });
  void bindCatalog();
}
