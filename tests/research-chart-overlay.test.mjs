import assert from "node:assert/strict";
import test from "node:test";

import {
  barIndexForTime,
  completedHistoricalResults,
  historicalResultFromLocation,
  historicalResultRouteSearch,
  overlayFills,
  overlayStatus,
  tradeMarks,
} from "../web/research-chart-overlay.mjs";
import { candleGeometry } from "../web/ui.mjs";
import { chartCard } from "../web/research-signals.mjs";

const RESULT_ID = "tc-research:historical-result:v1:33333333-3333-4333-8333-333333333333";
const OTHER_ID = "tc-research:historical-result:v1:44444444-4444-4444-8444-444444444444";

const bars = Object.freeze([
  { open_time: "2026-09-03T13:30:00Z", open: 100, high: 102, low: 99, close: 101, volume: 10 },
  { open_time: "2026-09-03T13:45:00Z", open: 101, high: 101.5, low: 98.5, close: 99, volume: 8 },
]);
const barsPayload = Object.freeze({
  schema: "tc.market-bars.v1",
  status: "current",
  reason_code: null,
  detail: "OHLC bars provided by the connected market-data provider.",
  provider: { id: "example-bars" },
  symbol: "ESM5",
  timeframe: "M15",
  bars,
});
const completedResult = Object.freeze({
  entity_id: RESULT_ID,
  revision: "tc-research-revision:historical-result:sha256:" + "3".repeat(64),
  state: "completed",
  execution_completed: true,
  result_archive_name: "Survivor.sqx",
});

function trade(overrides = {}) {
  return {
    Ticket: 7,
    Symbol: "ESM5",
    OpenTime: Date.parse("2026-09-03T13:30:00Z"),
    OpenPrice: 100.5,
    CloseTime: Date.parse("2026-09-03T13:45:00Z"),
    ClosePrice: 99.25,
    PL: 12.5,
    ...overrides,
  };
}

test("route helpers parse Historical Result identities from search or path", () => {
  assert.deepEqual(historicalResultFromLocation(""), { present: false, entityId: "" });
  assert.deepEqual(
    historicalResultFromLocation(`?historicalResult=${RESULT_ID}`),
    { present: true, entityId: RESULT_ID },
  );
  assert.deepEqual(
    historicalResultFromLocation(`/research?workspace=signals&tab=signals&historicalResult=${RESULT_ID}`),
    { present: true, entityId: RESULT_ID },
  );
  assert.deepEqual(
    historicalResultFromLocation("?historicalResult=not-a-result"),
    { present: true, entityId: "" },
  );
  const selectedPath = historicalResultRouteSearch(RESULT_ID, "/research?workspace=validate&tab=trades");
  const selectedParams = new URLSearchParams(selectedPath.split("?")[1]);
  assert.equal(selectedParams.get("workspace"), "validate");
  assert.equal(selectedParams.get("tab"), "trades");
  assert.equal(selectedParams.get("historicalResult"), RESULT_ID);
  assert.equal(historicalResultFromLocation(selectedPath).entityId, RESULT_ID);
  assert.equal(
    historicalResultRouteSearch("", `/research?workspace=signals&tab=signals&historicalResult=${RESULT_ID}`),
    "/research?workspace=signals&tab=signals",
  );
});

test("completed Historical Results require native execution_completed", () => {
  assert.deepEqual(
    completedHistoricalResults([
      completedResult,
      { ...completedResult, entity_id: OTHER_ID, execution_completed: false },
      { ...completedResult, entity_id: OTHER_ID, state: "running" },
      { entity_id: OTHER_ID, state: "completed" },
    ]).map((item) => item.entity_id),
    [RESULT_ID],
  );
});

test("native open and close map onto M15 producer bars; off-series times are omitted", () => {
  assert.equal(barIndexForTime(bars, "M15", Date.parse("2026-09-03T13:30:00Z")), 0);
  assert.equal(barIndexForTime(bars, "M15", Date.parse("2026-09-03T13:44:59Z")), 0);
  assert.equal(barIndexForTime(bars, "M15", Date.parse("2026-09-03T13:45:00Z")), 1);
  assert.equal(barIndexForTime(bars, "M15", Date.parse("2026-09-03T13:59:00Z")), 1);
  assert.equal(barIndexForTime(bars, "M15", Date.parse("2026-09-03T12:00:00Z")), -1);
  assert.equal(barIndexForTime(bars, "M15", Date.parse("2026-09-03T14:00:00Z")), -1);

  const mapped = overlayFills(bars, [trade()], { symbol: "ESM5", timeframe: "M15" });
  assert.equal(mapped.nativeFillCount, 2);
  assert.equal(mapped.fills.length, 2);
  assert.deepEqual(mapped.fills.map((fill) => [fill.kind, fill.index, fill.ticket]), [
    ["open", 0, 7],
    ["close", 1, 7],
  ]);

  const skipped = overlayFills(bars, [trade({
    OpenTime: Date.parse("2026-09-03T12:00:00Z"),
    CloseTime: Date.parse("2026-09-03T14:00:00Z"),
  })], { symbol: "ESM5", timeframe: "M15" });
  assert.equal(skipped.nativeFillCount, 2);
  assert.equal(skipped.fills.length, 0);
});

test("symbol mismatch and unselected results never invent fills", () => {
  const mismatch = overlayFills(bars, [trade({ Symbol: "EURUSD" })], { symbol: "ESM5", timeframe: "M15" });
  assert.equal(mismatch.nativeFillCount, 2);
  assert.equal(mismatch.fills.length, 0);
  assert.equal(overlayStatus({
    selectedEntityId: RESULT_ID,
    barsReady: true,
    tradesState: "available",
    mapped: 0,
    nativeFillCount: 2,
  }).reason_code, "fills_not_on_bars");

  const idle = overlayStatus({});
  assert.equal(idle.state, "idle");
  assert.equal(idle.reason_code, "historical_result_not_selected");

  const html = chartCard({
    title: "Price",
    bars: barsPayload,
    results: [completedResult],
    search: "/research?workspace=signals&tab=signals",
    showTradeOverlay: true,
  });
  assert.match(html, /data-chart-historical-result/);
  assert.match(html, /data-trade-overlay-state="idle"/);
  assert.doesNotMatch(html, /data-trade-fill/);
});

test("Signals chart overlays native fills only for the selected Historical Result", () => {
  const geometry = candleGeometry(bars);
  const mapped = overlayFills(geometry.rows, [trade()], { symbol: "ESM5", timeframe: "M15" });
  const marks = tradeMarks(mapped.fills, geometry);
  assert.match(marks, /data-trade-fill="open"/);
  assert.match(marks, /data-trade-fill="close"/);
  assert.match(marks, /data-trade-ticket="7"/);
  assert.match(marks, /tone-up/);

  const selected = chartCard({
    title: "Price",
    bars: barsPayload,
    results: [completedResult],
    trades: [trade()],
    search: `/research?workspace=signals&tab=signals&historicalResult=${RESULT_ID}`,
    showTradeOverlay: true,
  });
  assert.match(selected, /data-trade-overlay-state="current"/);
  assert.match(selected, /data-trade-fill="open"/);
  assert.match(selected, /data-trade-fill="close"/);
  assert.match(selected, new RegExp(`option value="${RESULT_ID}" selected`));
  assert.match(selected, /2 native fills on producer bars/);

  const invalid = chartCard({
    title: "Price",
    bars: barsPayload,
    results: [completedResult],
    trades: [trade()],
    search: "/research?workspace=signals&tab=signals&historicalResult=not-a-result",
    showTradeOverlay: true,
  });
  assert.match(invalid, /data-trade-overlay-state="unavailable"/);
  assert.match(invalid, /historical_result_invalid|invalid/);
  assert.doesNotMatch(invalid, /data-trade-fill/);

  const mismatch = chartCard({
    title: "Price",
    bars: barsPayload,
    results: [completedResult],
    trades: [trade({ Symbol: "EURUSD" })],
    search: `/research?workspace=signals&tab=signals&historicalResult=${RESULT_ID}`,
    showTradeOverlay: true,
  });
  assert.match(mismatch, /data-trade-overlay-state="empty"/);
  assert.match(mismatch, /none land on these producer bars/);
  assert.doesNotMatch(mismatch, /data-trade-fill/);
});
