// Native trade overlay on producer OHLC bars.
// Fills come only from one selected Historical Result. A fill is drawn only when its
// native timestamp lands on an existing producer bar. Missing bars, symbol mismatch,
// and unmapped times are skipped — never interpolated or invented.

import { researchLocationMatches, researchPath } from "./model.mjs";
import { fetchHistoricalResultDetail } from "./research-backtest-trades.mjs";
import { escapeHtml } from "./ui.mjs";

export const HISTORICAL_RESULT_ROUTE_PARAM = "historicalResult";
export const HISTORICAL_RESULT_ENTITY = /^tc-research:historical-result:v1:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;

export const BAR_TIMEFRAME_MS = Object.freeze({
  M1: 60_000,
  M5: 300_000,
  M15: 900_000,
  M30: 1_800_000,
  H1: 3_600_000,
  H4: 14_400_000,
  D1: 86_400_000,
  W1: 604_800_000,
});

function parseBarTime(value) {
  if (typeof value !== "string" || !value) return NaN;
  const ms = Date.parse(value);
  return Number.isFinite(ms) ? ms : NaN;
}

function queryParams(search) {
  if (search && typeof search === "object" && typeof search.search === "string") {
    search = search.search;
  }
  const text = typeof search === "string" ? search : "";
  const query = text.includes("?") ? text.slice(text.indexOf("?") + 1) : text.replace(/^\?/, "");
  return new URLSearchParams(query);
}

export function historicalResultFromLocation(search = globalThis.location?.search || "") {
  const params = queryParams(search);
  if (!params.has(HISTORICAL_RESULT_ROUTE_PARAM)) return { present: false, entityId: "" };
  const value = params.get(HISTORICAL_RESULT_ROUTE_PARAM);
  return {
    present: true,
    entityId: typeof value === "string" && HISTORICAL_RESULT_ENTITY.test(value) ? value : "",
  };
}

export function historicalResultRouteSearch(entityId, locationLike = globalThis.location) {
  const params = queryParams(locationLike);
  if (entityId) params.set(HISTORICAL_RESULT_ROUTE_PARAM, entityId);
  else params.delete(HISTORICAL_RESULT_ROUTE_PARAM);
  const workspace = params.get("workspace") || "signals";
  const tab = params.get("tab") || "signals";
  return researchPath(workspace, tab, `?${params.toString()}`);
}

export function completedHistoricalResults(results) {
  return (Array.isArray(results) ? results : []).filter(
    (item) => item && item.state === "completed" && item.execution_completed === true && typeof item.entity_id === "string",
  );
}

export function barIndexForTime(bars, timeframe, epochMs) {
  const duration = BAR_TIMEFRAME_MS[timeframe];
  if (!Number.isFinite(epochMs) || !duration || !Array.isArray(bars) || !bars.length) return -1;
  for (let index = 0; index < bars.length; index += 1) {
    const start = parseBarTime(bars[index]?.open_time);
    if (!Number.isFinite(start)) continue;
    const next = parseBarTime(bars[index + 1]?.open_time);
    const end = Number.isFinite(next) ? next : start + duration;
    if (epochMs >= start && epochMs < end) return index;
  }
  return -1;
}

function sameSymbol(left, right) {
  if (typeof left !== "string" || !left.trim()) return true;
  if (typeof right !== "string" || !right.trim()) return true;
  return left.trim().toUpperCase() === right.trim().toUpperCase();
}

export function overlayFills(bars, trades, { symbol = "", timeframe = "" } = {}) {
  const rows = Array.isArray(bars) ? bars : [];
  const fills = [];
  let nativeFillCount = 0;
  for (const trade of Array.isArray(trades) ? trades : []) {
    if (!trade || !Number.isInteger(trade.Ticket)) continue;
    const symbolOk = sameSymbol(trade.Symbol, symbol);
    for (const kind of ["open", "close"]) {
      const time = kind === "open" ? trade.OpenTime : trade.CloseTime;
      const price = kind === "open" ? trade.OpenPrice : trade.ClosePrice;
      nativeFillCount += 1;
      if (!symbolOk || !Number.isFinite(Number(price))) continue;
      const index = barIndexForTime(rows, timeframe, time);
      if (index < 0) continue;
      fills.push({
        ticket: trade.Ticket,
        kind,
        index,
        price: Number(price),
        pl: Number.isFinite(Number(trade.PL)) ? Number(trade.PL) : 0,
      });
    }
  }
  return { fills, nativeFillCount };
}

export function tradeMarks(fills, geometry) {
  if (!geometry || !Array.isArray(fills) || !fills.length) return "";
  const { min, span, slot, height = 100 } = geometry;
  if (!Number.isFinite(min) || !Number.isFinite(span) || span <= 0 || !Number.isFinite(slot)) return "";
  return fills.map((fill) => {
    const x = (fill.index + 0.5) * slot;
    const y = height - ((fill.price - min) / span) * height;
    const tone = fill.pl >= 0 ? "up" : "down";
    if (fill.kind === "open") {
      const size = Math.max(slot * 0.28, 1.1);
      const points = `${x.toFixed(2)},${(y - size).toFixed(2)} ${(x - size).toFixed(2)},${(y + size * 0.6).toFixed(2)} ${(x + size).toFixed(2)},${(y + size * 0.6).toFixed(2)}`;
      return `<polygon class="trade-fill tone-${tone}" data-trade-ticket="${escapeHtml(fill.ticket)}" data-trade-fill="open" points="${points}"/>`;
    }
    const radius = Math.max(slot * 0.18, 0.8);
    return `<circle class="trade-fill tone-${tone}" data-trade-ticket="${escapeHtml(fill.ticket)}" data-trade-fill="close" cx="${x.toFixed(2)}" cy="${y.toFixed(2)}" r="${radius.toFixed(2)}"/>`;
  }).join("");
}

export function overlayStatus({ selectedEntityId = "", barsReady = false, tradesState = "", reasonCode = "", mapped = 0, nativeFillCount = 0 } = {}) {
  if (reasonCode === "historical_result_invalid") {
    return {
      state: "unavailable",
      reason_code: "historical_result_invalid",
      detail: "The Historical Result identity is invalid. No fills are drawn.",
    };
  }
  if (!selectedEntityId) {
    return {
      state: "idle",
      reason_code: "historical_result_not_selected",
      detail: "Select a completed Historical Result to overlay its native fills on these producer bars.",
    };
  }
  if (!barsReady) {
    return {
      state: "unavailable",
      reason_code: "bars_unavailable",
      detail: "Native trades overlay the producer bar series. Bars are not connected, so no fills are drawn.",
    };
  }
  if (tradesState === "unavailable") {
    return {
      state: "unavailable",
      reason_code: reasonCode || "trades_unavailable",
      detail: "The selected Historical Result has no native trade readback. No fills are invented.",
    };
  }
  if (tradesState === "pending") {
    return {
      state: "pending",
      reason_code: null,
      detail: "Reading native trades for the selected Historical Result…",
    };
  }
  if (nativeFillCount === 0) {
    return {
      state: "empty",
      reason_code: "no_native_trades",
      detail: "The selected result has no native portfolio trades. None are synthesized.",
    };
  }
  if (mapped === 0) {
    return {
      state: "empty",
      reason_code: "fills_not_on_bars",
      detail: "Native fills exist but none land on these producer bars. Unmapped fills are omitted, not invented.",
    };
  }
  return {
    state: "current",
    reason_code: null,
    detail: mapped === nativeFillCount
      ? `${mapped} native fill${mapped === 1 ? "" : "s"} on producer bars.`
      : `${mapped} of ${nativeFillCount} native fills land on these producer bars. The rest are omitted.`,
  };
}

function signalsChartRoute() {
  return researchLocationMatches(globalThis.location, "signals", "signals");
}

function overlayHost() {
  if (!signalsChartRoute()) return null;
  return document.querySelector("[data-chart-card][data-trade-overlay-state] [data-trade-overlay]");
}

function persistSelection(entityId) {
  if (!globalThis.location) return;
  const path = historicalResultRouteSearch(entityId, globalThis.location);
  if (`${globalThis.location.pathname}${globalThis.location.search}` === path) return;
  globalThis.dispatchEvent(new CustomEvent("tradercockpit:navigate", { detail: { path } }));
}

let generation = 0;

async function paintOverlay() {
  if (!signalsChartRoute()) return;
  const card = document.querySelector("[data-chart-card][data-trade-overlay-state]");
  const host = card?.querySelector("[data-trade-overlay]");
  if (!card || !host) return;
  const current = ++generation;
  const selected = historicalResultFromLocation();
  const statusNode = card.querySelector("[data-trade-overlay-status]");
  const paintKey = `${selected.entityId || (selected.present ? "invalid" : "none")}:${card.getAttribute("data-chart-count") || "0"}`;
  if (host.getAttribute("data-trade-overlay-for") === paintKey) return;
  host.setAttribute("data-trade-overlay-for", paintKey);

  const applyStatus = (status, { clear = true } = {}) => {
    if (clear) host.replaceChildren();
    if (statusNode) statusNode.textContent = status.detail;
    card.setAttribute("data-trade-overlay-state", status.state);
  };

  if (selected.present && !selected.entityId) {
    applyStatus(overlayStatus({ reasonCode: "historical_result_invalid" }));
    return;
  }
  if (!selected.entityId) {
    applyStatus(overlayStatus({}));
    return;
  }
  let openTimes = [];
  try {
    openTimes = JSON.parse(card.getAttribute("data-chart-open-times") || "[]");
  } catch {
    openTimes = [];
  }
  const bars = (Array.isArray(openTimes) ? openTimes : []).map((open_time) => ({ open_time }));
  if (!bars.length) {
    applyStatus(overlayStatus({ selectedEntityId: selected.entityId, barsReady: false }));
    return;
  }
  try {
    const detail = await fetchHistoricalResultDetail(selected.entityId);
    if (current !== generation || !host.isConnected) return;
    if (detail.tradesReadback.state !== "available") {
      applyStatus(overlayStatus({
        selectedEntityId: selected.entityId,
        barsReady: true,
        tradesState: "unavailable",
        reasonCode: detail.tradesReadback.reason_code || "trades_unavailable",
      }));
      if (statusNode) statusNode.textContent = detail.tradesReadback.detail || statusNode.textContent;
      return;
    }
    const mapped = overlayFills(bars, detail.tradesReadback.payload.trades, {
      symbol: card.getAttribute("data-chart-symbol") || "",
      timeframe: card.getAttribute("data-chart-timeframe") || "",
    });
    const min = Number(card.getAttribute("data-chart-min"));
    const max = Number(card.getAttribute("data-chart-max"));
    const count = Number(card.getAttribute("data-chart-count"));
    const geometry = Number.isFinite(min) && Number.isFinite(max) && Number.isFinite(count) && count > 0
      ? { min, span: (max - min) || 1, slot: 100 / count, height: 100 }
      : null;
    const status = overlayStatus({
      selectedEntityId: selected.entityId,
      barsReady: true,
      tradesState: "available",
      mapped: mapped.fills.length,
      nativeFillCount: mapped.nativeFillCount,
    });
    host.innerHTML = tradeMarks(mapped.fills, geometry);
    if (statusNode) statusNode.textContent = status.detail;
    card.setAttribute("data-trade-overlay-state", status.state);
  } catch (error) {
    if (current !== generation || !host.isConnected) return;
    applyStatus({
      state: "unavailable",
      detail: error instanceof Error ? error.message : "Native trade overlay failed.",
    });
  }
}

if (typeof document !== "undefined") {
  document.addEventListener("change", (event) => {
    if (!signalsChartRoute() || event.target?.getAttribute?.("data-chart-historical-result") == null) return;
    persistSelection(event.target.value || "");
  });
  const observer = new MutationObserver(() => {
    if (signalsChartRoute() && overlayHost()) void paintOverlay();
    if (!signalsChartRoute()) generation += 1;
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void paintOverlay();
}
