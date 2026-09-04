// Research → Signals & Models workspace (prototype screen `order-flow-signals-models`).
// Overview = strategy Idea custody; Signals & Models = chart context + the exact native
// Builder specification (blocks/rankings/cross-checks) + strategy panel; the analytics tabs
// (Order Flow, Footprint, Volume Profile, Liquidity Map, Replays) carry their full frame with
// explicit "no market-data provider" states until a producer exists.

import { researchNavPath, researchPath, researchWorkspace, researchLocationMatches } from "./model.mjs";
import {
  actionButton,
  candleGeometry,
  card,
  chartFrame,
  chip,
  escapeHtml,
  icon,
  linkButton,
  pageTitle,
  readable,
  ring,
  shortId,
  statList,
  tabRow,
  tag,
  unavailable,
  viewAll,
} from "./ui.mjs";
import { fetchNativeBuilderBlocks } from "./research-blocks.mjs";
import { renderAssistantWidget } from "./assistant.mjs";
import {
  completedHistoricalResults,
  historicalResultFromLocation,
  overlayFills,
  overlayStatus,
  tradeMarks,
} from "./research-chart-overlay.mjs";

const workspace = researchWorkspace("signals");

function workspaceTabs(route) {
  return tabRow(workspace.tabs, route.tabId, (tab) => researchNavPath("signals", tab.id), { ariaLabel: "Signals & Models tabs" });
}

// ---------- chart card (structural; series only from a read model) ----------

export function chartCard({ title, detail, height = 260, quotes = null, bars = null, extraFrames = [], results = [], trades = null, search = "", showTradeOverlay = false }) {
  const instrument = bars?.symbol || quotes?.watchlist?.[0]?.symbol;
  const symbolLabel = instrument || "No instrument";
  const barsReady = bars?.status === "current" && Array.isArray(bars.bars) && bars.bars.length > 0;
  const completed = completedHistoricalResults(results);
  const selected = historicalResultFromLocation(search);
  const selectedId = completed.some((item) => item.entity_id === selected.entityId) ? selected.entityId : "";
  const geometryRows = barsReady ? candleGeometry(bars.bars) : null;
  const mapped = geometryRows && Array.isArray(trades) && selected.entityId
    ? overlayFills(geometryRows.rows, trades, { symbol: bars.symbol, timeframe: bars.timeframe })
    : { fills: [], nativeFillCount: 0 };
  const overlay = overlayStatus({
    selectedEntityId: selected.entityId,
    barsReady: Boolean(geometryRows),
    tradesState: selected.entityId ? (trades == null ? "pending" : (Array.isArray(trades) ? "available" : "unavailable")) : "",
    reasonCode: selected.present && !selected.entityId ? "historical_result_invalid" : "",
    mapped: mapped.fills.length,
    nativeFillCount: mapped.nativeFillCount,
  });
  const extraPrices = mapped.fills.map((fill) => fill.price);
  const geometry = geometryRows ? candleGeometry(bars.bars, extraPrices) : null;
  const symbolSub = barsReady
    ? `OHLC · ${bars.timeframe} · ${bars.provider?.id || "provider"}`
    : (quotes?.status === "current" ? `Quotes only · ${quotes.provider?.id || "provider"} — bars not supplied` : "Market-data provider not connected");
  const timeframeLabel = bars?.timeframe || "M15";
  const chartState = barsReady ? "current" : "unavailable";
  const chartDetail = barsReady
    ? ""
    : (bars?.detail || detail);
  const yLabels = geometry
    ? [String(geometry.max), "", "", String(geometry.min)]
    : ["", "", "", ""];
  const xLabels = barsReady
    ? [String(bars.bars[0].open_time || "").slice(0, 10), String(bars.bars[bars.bars.length - 1].open_time || "").slice(0, 10)]
    : [];
  const legend = mapped.fills.length ? [["Native trades", "cyan"]] : [];
  const resultOptions = [`<option value="">No Historical Result selected</option>`]
    .concat(completed.map((item) => `<option value="${escapeHtml(item.entity_id)}" ${item.entity_id === selectedId ? "selected" : ""}>${escapeHtml(item.result_archive_name || item.entity_id)} · ${escapeHtml(shortId(item.revision, 10))}</option>`))
    .join("");
  const tools = ["plus", "spark", "layers", "activity", "target", "search", "table", "grid"].map((name) => icon(name, { size: 14 })).join("");
  const openTimes = geometry
    ? geometry.rows.map((bar) => bar.open_time).filter((value) => typeof value === "string")
    : [];
  const overlayAttrs = showTradeOverlay
    ? ` data-trade-overlay-state="${escapeHtml(overlay.state)}" data-chart-symbol="${escapeHtml(bars?.symbol || "")}" data-chart-timeframe="${escapeHtml(bars?.timeframe || "")}" data-chart-min="${escapeHtml(geometry ? String(geometry.min) : "")}" data-chart-max="${escapeHtml(geometry ? String(geometry.max) : "")}" data-chart-count="${escapeHtml(geometry ? String(geometry.rows.length) : "")}" data-chart-open-times="${escapeHtml(JSON.stringify(openTimes))}"`
    : "";
  const resultPicker = showTradeOverlay
    ? `<label class="toolbar-item chart-result-picker"><span>Historical Result</span><select data-chart-historical-result aria-label="Overlay native trades from a Historical Result">${resultOptions}</select></label>`
    : "";
  return `<article class="card" data-chart-card data-bars-status="${escapeHtml(bars?.status || "pending")}"${overlayAttrs}>
    <div class="chart-toolbar">
      <span class="symbol">${icon("chart", { size: 14 })}<strong>${escapeHtml(symbolLabel)}</strong><small>${escapeHtml(symbolSub)}</small></span>
      <span class="toolbar-sep"></span>
      <span class="toolbar-item ${barsReady ? "" : "is-disabled"}" title="${barsReady ? "Requested timeframe" : "Timeframe selection needs a market-data provider"}">${escapeHtml(timeframeLabel)} ${icon("down", { size: 12 })}</span>
      ${resultPicker}
      <span class="toolbar-item is-disabled">${icon("spark", { size: 12 })} Indicators</span>
      <span class="toolbar-item is-disabled">${icon("grid", { size: 12 })} Templates</span>
      <span class="toolbar-sep"></span>
      <span class="toolbar-item is-disabled">${icon("bell", { size: 12 })} Alert</span>
      <span class="toolbar-item is-disabled">${icon("play", { size: 12 })} Replay</span>
      <span class="toolbar-right"><span class="toolbar-item is-disabled">Heikin Ashi ${icon("down", { size: 12 })}</span></span>
    </div>
    <div class="chart-body">
      <div class="chart-tools" aria-hidden="true">${tools}</div>
      <div class="chart-main">
        ${chartFrame({
          height,
          title,
          state: chartState,
          detail: chartDetail,
          yLabels,
          xLabels,
          legend: showTradeOverlay ? legend : [],
          candles: geometry ? geometry.rows : [],
          extraPrices: showTradeOverlay ? extraPrices : [],
          tradeMarksSvg: showTradeOverlay ? tradeMarks(mapped.fills, geometry) : "",
        })}
        ${showTradeOverlay ? `<p class="note" data-trade-overlay-status>${escapeHtml(overlay.detail)}</p>` : ""}
        ${extraFrames.map((frame) => chartFrame({ height: 56, title: frame, state: "unavailable", detail: "No data yet", yLabels: [] })).join("")}
      </div>
    </div>
    <div class="chart-ranges"><span>1D</span><span>5D</span><span>1M</span><span>3M</span><span>6M</span><span>YTD</span><span>1Y</span><span>5Y</span><span class="is-active">All</span><span class="ranges-right">${escapeHtml(barsReady ? "Live bars" : (quotes?.status === "current" ? "Quotes only" : "No live clock"))}</span></div>
  </article>`;
}

// ---------- rail cards ----------

function strategyPanelCard() {
  return card({
    title: "Strategy Panel",
    accent: "neutral",
    actions: `${icon("dots", { size: 14 })}`,
    body: `<div class="strategy-panel-tabs"><span class="tab is-active">Signals</span><span class="tab">Models</span><span class="tab">Rules</span><span style="margin-left:auto">${actionButton("Add Signal", { iconName: "plus", disabled: true, className: "button-small", title: "Native Builder owns block enabling; use the Indicators & Models catalog to inspect" })}</span></div>
      <div data-signals-strategy-panel>${unavailable("Reading native signal blocks…", "Enabled native signal blocks from the exact current Builder task.", { tone: "pending", compact: true })}</div>`,
    footer: viewAll(researchNavPath("catalog", "indicators"), "View All Signals"),
    className: "strategy-panel",
  });
}

function signalPulseCard() {
  const bars = ["Momentum", "Order Flow", "Liquidity", "Structure", "Volatility"];
  return card({
    title: "Signal Pulse",
    accent: "neutral",
    body: `${ring({ value: NaN, label: "No live signal producer", tone: "purple", size: 110 })}
      <div class="stat-list">${bars.map((label) => `<div class="stat-row"><span>${escapeHtml(label)}</span><strong class="tone-text-dim">—</strong></div>`).join("")}</div>
      <p class="note">Live confluence requires a market-data provider plus a live strategy/deployment signal producer. Historical entries are never shown as live signals.</p>`,
  });
}

function activeModelsCard() {
  return card({
    title: "Active Models",
    accent: "neutral",
    actions: viewAll(researchNavPath("catalog", "models"), "View All"),
    body: `<div class="model-row"><span>No models connected</span><span class="grade">—</span><span class="pct">—</span><span class="toggle" aria-hidden="true"></span></div>
      <p class="note">The Machine Learning / Models modality is platform-owned and not connected yet; models appear here when its backend exists.</p>`,
  });
}

function assistantCard(runtime) {
  return card({
    title: "Assistant",
    sub: "Your trading copilot",
    headIcon: "bot",
    accent: "purple",
    actions: tag("Apollo", "purple"),
    body: renderAssistantWidget(runtime, { compact: true, placeholder: "Ask Apollo…" }),
    className: "is-assistant",
  });
}

function bottomRow(runtime, quotes) {
  const confluence = card({
    title: "Confluence",
    accent: "neutral",
    body: `<div class="grid grid-2" style="gap:6px;align-items:center">${ring({ value: NaN, label: "Score", tone: "cyan", size: 76 })}${ring({ value: NaN, label: "", tone: "purple", size: 56 })}</div>
      <div class="stat-list">${["Signals Aligning", "Model Agreement", "Market State Match", "Risk Within Limits"].map((label) => `<div class="stat-row"><span>${escapeHtml(label)}</span><strong class="tone-text-dim">— / —</strong></div>`).join("")}</div>`,
    footer: chip("No live producer", "unavailable"),
  });
  const market = card({
    title: "Market State",
    accent: "neutral",
    body: `<div class="conclusion-head"><span class="conclusion-mark">${icon("spark", { size: 18 })}</span><div><strong class="tone-text-dim">Regime not available</strong><p>Requires a live market-data provider.</p></div></div>
      ${statList([["Trend Strength", "—"], ["Market Regime", "—"], ["Volatility Regime", "—"], ["Volume Regime", "—"], ["Risk Appetite", "—"]])}`,
  });
  const context = quotes?.watchlist?.[0];
  const session = card({
    title: "Session Context",
    accent: "neutral",
    body: `<div><strong>${escapeHtml(context ? context.symbol : "No instrument")}</strong><p class="note">${escapeHtml(quotes?.status === "current" ? `Live quotes via ${quotes.provider?.id || "provider"}` : "Session data needs a market-data provider")}</p></div>
      ${statList([["Session", "—"], ["Time in Session", "—"], ["Session Volume", "—"], ["Avg. Range (15m)", "—"]])}
      <div class="bar-row"><span class="bar-label">Day Progress</span><div class="bar tone-purple"><i style="width:0%"></i></div><span class="bar-value">—</span></div>`,
  });
  const risk = card({
    title: "Risk Overlay",
    headIcon: "shield",
    accent: "orange",
    actions: chip("No account", "unavailable"),
    body: statList([["Account Risk", "—"], ["Max Daily Loss", "—"], ["Exposure", "—"], ["Kelly Fraction", "—"], ["Position Sizing", "—"]]),
    footer: linkButton("/operate", "View Risk Dashboard", { iconName: "plus", className: "button-block" }),
  });
  return `<div class="grid grid-5">${confluence}${market}${session}${risk}${assistantCard(runtime)}</div>`;
}

// ---------- tabs ----------

function renderIdeaCatalog(state) {
  if (state.phase === "loading" || state.phase === "idle") return `<div class="idea-catalog-state">Loading saved Ideas…</div>`;
  if (state.phase === "failed") return `<div class="idea-catalog-state idea-error">${escapeHtml(state.detail || "Idea catalog unavailable")}</div>`;
  if (!state.catalog.length) return `<div class="idea-catalog-state">No saved Ideas yet.</div>`;
  return `<div class="idea-catalog-list">${state.catalog.map((idea) => {
    const active = state.selected?.entity_id === idea.entity_id;
    return `<button class="idea-catalog-item ${active ? "is-active" : ""}" type="button" data-idea-action="select" data-idea-entity-id="${escapeHtml(idea.entity_id)}"><strong>${escapeHtml(idea.summary)}</strong><span>${escapeHtml(String(idea.revision).slice(-12))}</span></button>`;
  }).join("")}</div>`;
}

function renderOverviewTab(route, { ideaState, runtime, nextAction }) {
  const state = ideaState || { phase: "idle", catalog: [], selected: null, detail: "" };
  const selected = state.selected || null;
  const isLoading = state.phase === "loading" || state.phase === "idle";
  const revisionDetail = selected
    ? `<div class="idea-identity" data-idea-current-identity><div><span>Idea entity</span><code>${escapeHtml(selected.entity_id)}</code></div><div><span>Current revision</span><code>${escapeHtml(selected.revision)}</code></div>${selected.parent_revision ? `<div><span>Parent revision</span><code>${escapeHtml(selected.parent_revision)}</code></div>` : ""}<div><span>Content identity</span><code>${escapeHtml(selected.content_ref)}</code></div></div>`
    : `<div class="idea-new-state"><strong>New Idea</strong><span>Saving will mint the Idea identity on the backend and create its first immutable revision.</span></div>`;
  const detail = `<p class="idea-save-status" data-idea-save-status>${escapeHtml(state.detail || "")}</p>`;
  const catalogCard = card({
    title: "Saved Ideas",
    sub: "Revision custody",
    headIcon: "bookmark",
    accent: "cyan",
    body: `<p class="note">Select an existing Idea or start a new one. Identity is created only by the canonical backend.</p><button class="button button-secondary" type="button" data-idea-action="new">${icon("plus", { size: 14 })}<span>New Idea</span></button>${renderIdeaCatalog(state)}`,
    className: "idea-catalog-panel",
  });
  const ingest = selected?.ingest;
  const draft = selected?.draft;
  const spans = Array.isArray(ingest?.quoted_spans) ? ingest.quoted_spans : [];
  const spanBlock = spans.length
    ? `<div class="idea-span-list" data-idea-ingest-spans="${escapeHtml(String(ingest.content_sha256 || ""))}">${spans.slice(0, 12).map((span) => `<blockquote class="idea-span" data-span-id="${escapeHtml(span.id)}"><code>${escapeHtml(span.id)}</code> <code>${escapeHtml(String(span.sha256 || "").slice(0, 12))}</code><p>${escapeHtml(span.text)}</p></blockquote>`).join("")}${spans.length > 12 ? `<p class="note">${spans.length - 12} more hashed spans in custody.</p>` : ""}</div>`
    : "";
  const draftBlock = draft
    ? `<div class="idea-draft-record" data-idea-draft-status="${escapeHtml(draft.status || "unavailable")}" data-idea-object-kind="${escapeHtml(draft.object_kind || "unresolved")}"><strong>${escapeHtml(readable(draft.object_kind, "Unresolved"))}</strong><p class="note">${escapeHtml(draft.detail || "Typed draft is bound only to quoted spans.")}</p>${Array.isArray(draft.clauses) && draft.clauses.length ? `<ul class="idea-draft-clauses">${draft.clauses.map((clause) => `<li data-span-id="${escapeHtml(clause.span_id)}">${escapeHtml(clause.text)}</li>`).join("")}</ul>` : ""}</div>`
    : `<p class="note">Ingest a URL or document to mint hashed quoted spans. Apollo may draft indicator, strategy, or model meaning only from those spans.</p>`;
  const editorCard = card({
    title: "Strategy idea",
    sub: "Historical research · concept, source and provenance",
    headIcon: "flask",
    accent: "purple",
    body: `<p class="note">Idea revisions preserve source text and provenance only. Saving does not create a candidate, run native compute, or infer trading semantics.</p>${revisionDetail}<label class="field-label" for="idea-draft">Idea draft</label><textarea id="idea-draft" class="idea-editor" maxlength="100000" placeholder="Describe the strategy idea, source, indicator, or existing native strategy…" ${isLoading ? "disabled" : ""}>${escapeHtml(selected?.text || "")}</textarea><label class="field-label" for="idea-source">Source / provenance</label><textarea id="idea-source" class="idea-editor idea-source-editor" maxlength="20000" placeholder="Where did this idea come from? Notes, observation, native strategy/template reference…" ${isLoading ? "disabled" : ""}>${escapeHtml(selected?.source || "")}</textarea><label class="field-label" for="idea-ingest-url">Ingest URL</label><input id="idea-ingest-url" class="idea-editor" type="url" maxlength="2000" placeholder="https://…" ${isLoading ? "disabled" : ""} /><label class="field-label" for="idea-ingest-document">Or paste a document</label><textarea id="idea-ingest-document" class="idea-editor idea-source-editor" maxlength="100000" placeholder="Paste paper text, notes, or a strategy write-up…" ${isLoading ? "disabled" : ""}></textarea><p class="field-help">Ingest hashes the exact body and stores quoted spans. Apollo cannot invent clauses that are not in those spans. Each save or ingest creates a new immutable Idea revision.</p>${detail}<div class="idea-actions"><button class="button button-primary" type="button" data-idea-action="save" ${isLoading ? "disabled" : ""}>${selected ? "Save new revision" : "Save Idea"}</button><button class="button button-secondary" type="button" data-idea-action="ingest-url" ${isLoading ? "disabled" : ""}>Ingest URL</button><button class="button button-secondary" type="button" data-idea-action="ingest-document" ${isLoading ? "disabled" : ""}>Ingest document</button>${selected ? `<button class="button button-secondary" type="button" data-idea-action="reload">Reload saved revision</button>` : ""}</div>${spanBlock}${draftBlock}`,
    className: "idea-editor-panel",
  });
  const next = nextAction?.next_action;
  const nextBody = next
    ? `<div class="list-rows" data-research-next-action="${escapeHtml(next.id)}"><a class="list-row is-next" href="${escapeHtml(next.path)}" data-route="${escapeHtml(next.path)}"><span class="row-title"><strong>${escapeHtml(next.label)}</strong><span>${escapeHtml(nextAction.detail || "One legal next action from custody.")}</span></span>${icon("chevron", { size: 14 })}</a></div><p class="note">Current stage: ${escapeHtml(nextAction.current_stage || "unknown")}. Locked stages stay locked until this action completes.</p>`
    : unavailable(
      nextAction?.reason_code ? readable(nextAction.reason_code, "Next action unavailable") : "Reading next action…",
      nextAction?.detail || "The sequential Research step comes from custody catalogs.",
      { tone: nextAction?.reason_code ? "unavailable" : "pending", compact: true },
    );
  const rail = `<div class="stack">${card({
    title: "Next step",
    headIcon: "activity",
    accent: "cyan",
    body: nextBody,
  })}${assistantCard(runtime)}</div>`;
  return `<div class="with-rail"><section class="idea-workspace" data-research-idea-workspace>${catalogCard}${editorCard}</section>${rail}</div>`;
}

function renderSignalsTab(route, { runtime, quotes, bars, snapshotState }) {
  const main = `<div class="stack">
    ${chartCard({
      title: "Price · order-flow overlays",
      detail: "Connect a market-data provider that supplies OHLC bars. Last/change quotes are not a candle substitute.",
      quotes,
      bars,
      extraFrames: ["Volume", "CVD"],
      results: snapshotState?.results || [],
      search: route?.canonicalPath || "",
      showTradeOverlay: true,
    })}
    ${card({
      title: "Native Strategy Specification",
      sub: "Clarifying questions + exact current StrategyQuant X Builder task",
      headIcon: "code",
      accent: "purple",
      actions: chip("Native SQX", "purple"),
      body: `<div class="requirement-grid" data-research-specification-grid><div class="requirement-item"><strong>Reading native Builder configuration…</strong><p>Strategy shape, market identity, historical data setup, building blocks, rankings, cross-checks, money management and the exact native search mode, without launching SQX.</p></div></div>`,
    })}
  </div>`;
  const rail = `<div class="stack">${strategyPanelCard()}${signalPulseCard()}${activeModelsCard()}</div>`;
  return `<div class="with-rail-wide with-rail">${main}${rail}</div>${bottomRow(runtime, quotes)}`;
}

const ANALYTICS_TABS = Object.freeze({
  "order-flow": ["Order Flow", "Bid/ask imbalance, delta and CVD need a tick-level market-data provider.", ["Delta", "CVD"]],
  footprint: ["Footprint", "Footprint clusters need a tick/volume-at-price market-data provider.", ["Volume at price"]],
  "volume-profile": ["Volume Profile", "Session and composite volume profiles need a market-data provider with historical bars.", ["Session profile"]],
  "liquidity-map": ["Liquidity Map", "Resting liquidity and sweep detection need level-2 market data.", ["Liquidity heat"]],
  replays: ["Replays", "Bar replay needs a historical market-data provider; native backtest trades are shown in Test & Validate → Trades.", ["Replay timeline"]],
});

function renderAnalyticsTab(route, { quotes, bars, runtime }) {
  const [title, detail, frames] = ANALYTICS_TABS[route.tabId];
  const rail = `<div class="stack">${card({
    title: `${title} settings`,
    accent: "neutral",
    body: `${statList([["Provider", quotes?.status === "current" ? quotes.provider?.id || "connected" : "Not connected"], ["Instrument", bars?.symbol || quotes?.watchlist?.[0]?.symbol || "—"], ["Aggregation", "—"], ["Session", "—"]])}<p class="note">Live analytics stay explicitly scoped from historical research. Nothing here is derived from backtest results.</p>`,
    footer: linkButton("/settings", "Configure data feed", { className: "button-block" }),
  })}${assistantCard(runtime)}</div>`;
  return `<div class="with-rail">${chartCard({ title, detail, height: 320, quotes, bars, extraFrames: frames })}${rail}</div>`;
}

function renderAlertsTab(route, { runtime }) {
  const body = `<div class="table-wrap"><table class="data-table"><thead><tr><th>Alert</th><th>Condition</th><th>Instrument</th><th>Status</th></tr></thead><tbody><tr class="table-empty"><td colspan="4">No alerts. Alert conditions require a live market-data provider and a live signal producer.</td></tr></tbody></table></div>`;
  return `<div class="with-rail">${card({ title: "Alerts", headIcon: "bell", accent: "orange", actions: actionButton("New Alert", { iconName: "plus", disabled: true, className: "button-small", title: "Alert producer not connected" }), body })}${assistantCard(runtime)}</div>`;
}

function renderReportsTab(route, { snapshotState, runtime }) {
  const proofs = snapshotState.proofs.slice().reverse();
  const rows = proofs.map((proof) => `<div class="list-row"><span class="activity-icon tone-green">${icon("check", { size: 14 })}</span><span class="row-title"><strong>Research Proof ${escapeHtml(shortId(proof.revision, 10))}</strong><span>Historical result ${escapeHtml(shortId(proof.historical_result_revision, 10))} · producer outcome ${escapeHtml(proof.producer_validation_outcome === "producer_result_captured_outcome_unread" ? "unread" : String(proof.producer_validation_outcome))}</span></span>${linkButton(`${researchPath("validate", "evidence")}&proofEntity=${encodeURIComponent(proof.entity_id)}`, "Open", { className: "button-small" })}</div>`).join("");
  const body = snapshotState.phase === "loading"
    ? unavailable("Reading Proof custody…", "Reports are the immutable Research Proof records.", { tone: "pending", compact: true })
    : rows
      ? `<div class="list-rows">${rows}</div>`
      : unavailable("No reports yet", "A report is an immutable Research Proof. Create one in Test & Validate → Evidence once a native result and validation exist.", { compact: true });
  return `<div class="with-rail">${card({ title: "Reports", sub: "Immutable Research Proof records", headIcon: "table", accent: "green", body })}${assistantCard(runtime)}</div>`;
}

export function renderSignalsWorkspace(route, states) {
  const actions = `${actionButton("Save Layout", { disabled: true, title: "Layout persistence is not available yet" })}${linkButton(researchNavPath("catalog", "indicators"), "New Signal", { primary: true, iconName: "plus" })}<span class="icon-button">${icon("dots", { size: 14 })}</span>`;
  let body;
  if (route.tabId === "overview") body = renderOverviewTab(route, states);
  else if (route.tabId === "signals") body = renderSignalsTab(route, states);
  else if (route.tabId === "alerts") body = renderAlertsTab(route, states);
  else if (route.tabId === "reports") body = renderReportsTab(route, states);
  else body = renderAnalyticsTab(route, states);
  return `${pageTitle(workspace.title, { actions })}${workspaceTabs(route)}${body}`;
}

// ---------- strategy panel binder: enabled native signal blocks ----------

function signalsRoute() {
  return researchLocationMatches(globalThis.location, "signals", "signals");
}

function renderSignalBlocks(blocks) {
  const root = blocks.producer_configuration;
  if (!root) return unavailable("No Blocks subtree", "The exact current Builder task does not expose building blocks.", { compact: true });
  const building = root.children.find((child) => child.tag === "BuildingBlocks");
  const signals = (building?.children || []).filter((node) => node.tag === "Block" && node.attributes.category === "signals");
  const enabled = signals.filter((node) => node.attributes.use === "true");
  const shown = enabled.slice(0, 6);
  return `<p class="note">${enabled.length} of ${signals.length} native signal blocks enabled in the current Builder task (read-only; StrategyQuant X owns semantics).</p>
    ${shown.map((node) => `<div class="signal-item"><div class="signal-top"><span class="signal-icon tone-purple">${escapeHtml(String(node.attributes.key || "?").slice(0, 1))}</span><strong>${escapeHtml(node.attributes.key)}</strong>${chip("Enabled", "ready")}</div><p>Native block <code>${escapeHtml(node.attributes.key)}</code> · weight ${escapeHtml(node.attributes.weight || "—")}</p><div class="signal-foot"><span>category ${escapeHtml(node.attributes.category)}</span><span class="toggle is-on" aria-hidden="true"></span></div></div>`).join("")}
    ${enabled.length > shown.length ? `<p class="note">${enabled.length - shown.length} more enabled signal blocks in the catalog.</p>` : ""}`;
}

let boundPanel = null;
let generation = 0;

async function bindStrategyPanel() {
  if (!signalsRoute()) { boundPanel = null; return; }
  const host = document.querySelector("[data-signals-strategy-panel]");
  if (!host || host === boundPanel) return;
  boundPanel = host;
  const current = ++generation;
  try {
    const blocks = await fetchNativeBuilderBlocks();
    if (current !== generation || !host.isConnected) return;
    host.innerHTML = renderSignalBlocks(blocks);
  } catch (error) {
    if (current !== generation || !host.isConnected) return;
    host.innerHTML = unavailable("Native blocks unavailable", error instanceof Error ? error.message : "Native Builder Blocks read failed", { tone: "error", compact: true });
  }
}

if (typeof document !== "undefined") {
  const observer = new MutationObserver(() => { void bindStrategyPanel(); });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void bindStrategyPanel();
}
