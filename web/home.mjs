// Cockpit Home — live/current orientation. Eight zones:
// Market Overview | System Status | Alpha Stack | Pipeline Overview | Signals | Risk |
// Performance | Quick Actions. Apollo is the full-page rail; Home only jumps there.

import { HOME_ZONES, researchPath } from "./model.mjs";
import {
  card,
  chip,
  escapeHtml,
  footLink,
  icon,
  linkButton,
  pageTitle,
  readable,
  tag,
  unavailable,
  viewAll,
} from "./ui.mjs";
import { assistantState } from "./assistant.mjs";

const zoneById = new Map(HOME_ZONES.map((zone) => [zone.id, zone]));

function zoneCard(id, { body, footer = "", actions = "", className = "" }) {
  const zone = zoneById.get(id);
  return card({
    title: zone.label,
    sub: zone.sub,
    number: zone.number,
    accent: zone.accent,
    actions,
    body,
    footer,
    className,
    attrs: `data-home-zone="${escapeHtml(zone.id)}"`,
  });
}

function renderHero() {
  return `<section class="hero" data-home-hero>
    <div class="hero-copy">
      <span class="hero-kicker">Live / current orientation</span>
      <h2>See what is happening <b>now.</b></h2>
      <p>Home is the live/current cockpit. It does not turn historical research into the application dashboard, and it does not fabricate live values before their producers are connected.</p>
      <div class="hero-actions">${linkButton("/builder", "Open Builder", { primary: true, iconName: "flask" })}${linkButton("/operate", "Open Operate", { iconName: "operate" })}</div>
    </div>
    <div class="hero-art" aria-hidden="true"></div>
  </section>`;
}

function formatQuoteValue(row) {
  if (row?.status !== "current" || typeof row.last !== "number" || !Number.isFinite(row.last)) return "—";
  const last = row.last.toLocaleString(undefined, { maximumFractionDigits: 2 });
  if (typeof row.change_percent !== "number" || !Number.isFinite(row.change_percent)) return last;
  const sign = row.change_percent > 0 ? "+" : "";
  return `${last} ${sign}${row.change_percent.toFixed(2)}%`;
}

function quoteTone(row) {
  if (row?.status !== "current" || typeof row.change_percent !== "number") return "flat";
  if (row.change_percent > 0) return "up";
  if (row.change_percent < 0) return "down";
  return "flat";
}

function marketOverviewCard(marketState, quotes) {
  let watchlist;
  if (!marketState || marketState.phase === "loading") {
    watchlist = unavailable("Checking market feed", "Waiting for the canonical /api/market/quotes read model.", { tone: "pending", compact: true });
  } else if (marketState.phase === "failed") {
    watchlist = unavailable("Market feed read failed", "The canonical /api/market/quotes read failed; no quotes are inferred.", { tone: "error", compact: true });
  } else {
    const rows = Array.isArray(quotes?.watchlist) ? quotes.watchlist : [];
    if (!rows.length) {
      watchlist = unavailable("Live market data not connected", "No watchlist is configured. Set a watchlist and connect a market-data provider; the Home market zone stays visible without substituting research data or demo prices.", { compact: true });
    } else {
      const connected = quotes?.status === "current";
      const note = connected
        ? `Live quotes from ${escapeHtml(quotes?.provider?.id || "connected provider")}.`
        : "Watchlist configured; live quotes appear once a market-data provider is connected.";
      watchlist = `${rows.map((row) => `<div class="stat-row" data-quote-symbol="${escapeHtml(row.symbol)}" data-quote-status="${escapeHtml(row.status || "unavailable")}" data-quote-tone="${quoteTone(row)}"><span>${escapeHtml(row.symbol)}</span><strong>${escapeHtml(formatQuoteValue(row))}</strong></div>`).join("")}<p class="note">${note}</p>`;
    }
  }
  const binder = unavailable("Checking live market context", "Waiting for canonical backend Market Overview state.", {
    tone: "pending",
    compact: true,
    attrs: 'data-home-market-overview data-market-status="pending"',
  });
  return zoneCard("market-overview", {
    actions: viewAll("/settings"),
    body: `${watchlist}${binder}`,
    footer: footLink("/settings", "Market settings", { tone: "green" }),
  });
}

const HEALTH_ROWS = Object.freeze([
  ["application", "TraderCockpit application", "operate"],
  ["research_backend", "Research backend", "research"],
  ["research_custody", "Research custody", "layers"],
  ["native_execution", "Native execution", "play"],
  ["market_data", "Live market data", "activity"],
  ["tradingview", "Apollo TradingView tool", "chart"],
  ["metatrader", "Apollo MetaTrader tool", "operate"],
  ["provider", "Model provider", "bot"],
  ["account", "Consumer account", "crown"],
  ["model", "Model access", "bot"],
  ["extensions", "Extensions", "grid"],
]);

function healthRecord(payload, key) {
  if (key === "native_execution") {
    const execution = payload.research_backend?.execution;
    if (!execution || typeof execution !== "object") return { status: "unavailable", reason_code: "execution_state_missing" };
    return execution.available === true ? { status: "ready", reason_code: null } : { status: "unavailable", reason_code: execution.reason_code || "execution_unavailable" };
  }
  if (key === "tradingview" || key === "metatrader") {
    const live = payload.live_producers && typeof payload.live_producers === "object" ? payload.live_producers : null;
    const record = live?.[key];
    return record && typeof record === "object" ? record : { status: "unavailable", reason_code: "mcp_url_not_configured" };
  }
  return payload[key] && typeof payload[key] === "object" ? payload[key] : { status: "unavailable", reason_code: "missing" };
}

export function healthValue(record, key) {
  if (key === "research_backend" && record.status === "ready") return `Ready · StrategyQuant X ${record.build}`;
  if (key === "native_execution" && record.status === "unavailable") return record.reason_code ? `Disabled · ${readable(record.reason_code)}` : "Disabled";
  const label = readable(record.status, "Unavailable");
  return record.reason_code ? `${label} · ${readable(record.reason_code)}` : label;
}

export function renderSystemHealthRows(payload) {
  return `<div class="health-list" data-home-system-status data-system-status="loaded">${HEALTH_ROWS.map(([key, label, iconName]) => {
    const record = healthRecord(payload, key);
    return `<div class="health-row stat-row" data-runtime-component="${escapeHtml(key.replaceAll("_", "-"))}" data-runtime-state="${escapeHtml(record.status)}">${icon(iconName, { size: 14 })}<strong>${escapeHtml(label)}</strong><span class="health-value">${escapeHtml(healthValue(record, key))}</span><span class="health-mark">${icon(record.status === "ready" || record.status === "current" ? "check" : "warn", { size: 14 })}</span></div>`;
  }).join("")}</div>`;
}

function systemStatusCard(statusState, runtime) {
  const body = runtime
    ? renderSystemHealthRows(runtime)
    : `<div data-home-system-status data-system-status="${statusState.phase === "failed" ? "error" : "pending"}">${statusState.phase === "failed"
      ? unavailable("Runtime status unavailable", "The canonical /api/status read failed; no component readiness is inferred.", { tone: "error", compact: true })
      : unavailable("Checking runtime status", "Waiting for the canonical backend status read model.", { tone: "pending", compact: true })}</div>`;
  return zoneCard("system-status", {
    body,
    footer: footLink("/settings", "View Status", { tone: "blue" }),
  });
}

function alphaStackCard() {
  return zoneCard("alpha-stack", {
    className: "is-wide",
    body: unavailable("Checking Alpha Stack", "Reading exact current Candidate custody without inferring promotion or deployment.", {
      tone: "pending",
      compact: true,
      attrs: 'data-home-alpha-stack data-alpha-stack-state="pending"',
    }),
    footer: footLink("/builder", "Open Builder", { tone: "purple" }),
  });
}

function pipelineOverviewCard() {
  return zoneCard("pipeline-overview", {
    className: "is-wide",
    body: `<div data-home-pipeline-body>${unavailable("Reading canonical pipeline state", "Lifecycle counts come from the Research custody catalogs. Home does not convert them into validation, promotion, or live status.", { tone: "pending", compact: true })}</div>`,
    footer: footLink("/custom-projects", "Open Custom projects", { tone: "orange" }),
  });
}

function signalsCard() {
  return zoneCard("signals", {
    actions: viewAll("/builder"),
    body: unavailable("Live signals not connected", "Current signal/confluence state requires both a live market feed and strategy/execution context. Historical backtests are not presented as live signals.", { compact: true }),
    footer: footLink("/builder", "Open Builder", { tone: "cyan" }),
  });
}

function riskCard() {
  return zoneCard("risk", {
    actions: chip("Not connected", "unavailable"),
    body: `${unavailable("Live risk state not connected", "Current portfolio, broker, exposure, loss usage, and deployment risk are separate from historical research metrics.", { compact: true })}<div class="metric-grid"><div class="metric"><span>Exposure</span><strong class="is-empty" title="Requires a live execution/account producer">—</strong></div><div class="metric"><span>Drawdown</span><strong class="is-empty" title="Requires a live execution/account producer">—</strong></div></div>`,
    footer: footLink("/operate", "Open Operate", { tone: "red" }),
  });
}

function performanceCard() {
  return zoneCard("performance", {
    className: "is-wide",
    actions: tag("Live / current", "green"),
    body: `${unavailable("Current performance not connected", "Live/account performance and historical research performance remain explicitly scoped and are never silently mixed.", { compact: true })}<div class="metric-grid"><div class="metric"><span>Daily P&amp;L</span><strong class="is-empty" title="Requires a live execution/account producer">—</strong></div><div class="metric"><span>Buying power</span><strong class="is-empty" title="Requires a live execution/account producer">—</strong></div></div>`,
    footer: footLink("/operate", "Open Operate", { tone: "green" }),
  });
}

function quickActionsCard(nextAction) {
  const tiles = [
    ["/builder", "Builder", "Native build settings"],
    ["/retester", "Retester", "Retest saved strategies"],
    ["/optimizer", "Optimizer", "Native walk-forward / optimize"],
    ["/custom-projects", "Custom projects", "Saved task pipelines"],
    [researchPath("signals", "overview"), "Idea", "Custody, not the pipeline"],
    [researchPath("validate", "evidence"), "Proof", "Immutable evidence chain"],
    ["/operate", "Operate", "Live / simulation"],
  ];
  const next = nextAction?.next_action;
  const nextPath = next?.path || "";
  const known = tiles.some(([href]) => href === nextPath);
  const rendered = nextPath && !known
    ? [[nextPath, next.label, nextAction.detail || "One legal next action"], ...tiles]
    : tiles;
  const body = `<div class="quick-tile-grid">${rendered.map(([href, title, detail]) => {
    const isNext = Boolean(nextPath) && href === nextPath;
    const cls = nextPath ? (isNext ? "quick-tile is-next" : "quick-tile is-muted") : "quick-tile";
    const nextAttr = isNext ? ` data-research-next-action="${escapeHtml(next.id)}"` : "";
    return `<a class="${cls}" href="${escapeHtml(href)}" data-route="${escapeHtml(href)}"${nextAttr}><strong>${escapeHtml(title)}</strong><span>${escapeHtml(detail)}</span></a>`;
  }).join("")}</div>
    <p class="note">${nextPath ? "One legal next action is emphasized. Locked stages stay locked." : "Navigation only. These actions do not create hidden workflows or duplicate producer state."}</p>`;
  return zoneCard("quick-actions", {
    className: "is-wide",
    body,
  });
}

function assistantPanel(runtime) {
  const state = assistantState(runtime);
  return card({
    title: "Apollo",
    sub: "Bounded trading copilot",
    accent: "purple",
    actions: tag("Apollo", "purple"),
    body: `<p class="note">${escapeHtml(state.modelLabel)}. Ask, Speak, Quant-Guild, and approved tools live on the Apollo rail — this card does not mount a second thread.</p>${linkButton("/apollo", "Open Apollo", { primary: true, iconName: "bot" })}`,
    className: "is-assistant home-assistant",
    attrs: 'data-home-assistant data-assistant-jump="apollo"',
  });
}

export function renderHome(route, { statusState, snapshotState, runtime, marketState, quotes, nextAction }) {
  void route;
  void snapshotState;
  return `${pageTitle("Getting started", { subtitle: "Current market, system, signal, risk, performance, and pipeline orientation. Native strategy work lives in Builder, Retester, Optimizer, and Custom projects." })}
    ${renderHero()}
    <section class="home-board" data-home-board data-home-zone-count="${HOME_ZONES.length}">
      ${marketOverviewCard(marketState, quotes)}
      ${systemStatusCard(statusState, runtime)}
      ${alphaStackCard()}
      ${pipelineOverviewCard()}
      ${signalsCard()}
      ${riskCard()}
      ${performanceCard()}
      ${quickActionsCard(nextAction)}
    </section>
    ${assistantPanel(runtime)}`;
}
