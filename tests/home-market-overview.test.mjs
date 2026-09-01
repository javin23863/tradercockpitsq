import assert from "node:assert/strict";
import test from "node:test";

import {
  fetchHomeMarketOverview,
  HOME_MARKET_OVERVIEW_SCHEMA,
  parseHomeMarketOverview,
  renderHomeMarketOverview,
} from "../web/home-market-overview.mjs";

function runtimeWithMarket(market) {
  return {
    schema: "tc.runtime-status.v1",
    market_data: market,
  };
}

function currentMarket(overrides = {}) {
  return {
    schema: HOME_MARKET_OVERVIEW_SCHEMA,
    scope: "live_current",
    historical_fallback: false,
    status: "current",
    reason_code: null,
    detail: "Current producer context.",
    producer: { id: "example-live-feed" },
    context: {
      instrument: "EURUSD",
      timeframe: "M1",
      session: "producer-session",
      market_state: "producer-state",
      descriptors: { venue: "example" },
    },
    freshness: {
      state: "current",
      observed_at: "2026-09-02T12:00:00Z",
      age_seconds: 4,
      stale_after_seconds: 30,
    },
    ...overrides,
  };
}

test("Market Overview parser accepts exact live/current producer context", () => {
  const market = currentMarket();
  assert.equal(parseHomeMarketOverview(runtimeWithMarket(market)), market);
});

test("Market Overview parser rejects historical fallback and contradictory freshness", () => {
  assert.throws(
    () => parseHomeMarketOverview(runtimeWithMarket(currentMarket({ historical_fallback: true }))),
    /scope mismatch/,
  );
  assert.throws(
    () => parseHomeMarketOverview(runtimeWithMarket(currentMarket({ freshness: { ...currentMarket().freshness, state: "stale" } }))),
    /freshness mismatch/,
  );
});

test("Market Overview parser rejects malformed or partial producer context", () => {
  assert.throws(
    () => parseHomeMarketOverview(runtimeWithMarket(currentMarket({ producer: { id: "" } }))),
    /producer identity/,
  );
  assert.throws(
    () => parseHomeMarketOverview(runtimeWithMarket(currentMarket({ context: { ...currentMarket().context, instrument: "" } }))),
    /instrument/,
  );
  const unavailable = {
    schema: HOME_MARKET_OVERVIEW_SCHEMA,
    scope: "live_current",
    historical_fallback: false,
    status: "unavailable",
    reason_code: "producer_not_configured",
    detail: "No producer.",
    producer: { id: "should-not-exist" },
    context: null,
    freshness: { state: "unavailable", observed_at: null, age_seconds: null, stale_after_seconds: 30 },
  };
  assert.throws(
    () => parseHomeMarketOverview(runtimeWithMarket(unavailable)),
    /contains context/,
  );
});

test("Market Overview fetch uses only canonical runtime status", async () => {
  const market = currentMarket();
  const result = await fetchHomeMarketOverview(async (path, options) => {
    assert.equal(path, "/api/status");
    assert.equal(options.headers.accept, "application/json");
    return { ok: true, status: 200, json: async () => runtimeWithMarket(market) };
  });
  assert.equal(result, market);

  await assert.rejects(
    () => fetchHomeMarketOverview(async () => ({ ok: false, status: 503, json: async () => ({}) })),
    /request failed: 503/,
  );
});

test("Market Overview renderer preserves explicit unavailable, current, and stale states", () => {
  const unavailable = renderHomeMarketOverview({
    schema: HOME_MARKET_OVERVIEW_SCHEMA,
    scope: "live_current",
    historical_fallback: false,
    status: "unavailable",
    reason_code: "producer_not_configured",
    detail: "No live/current market-data producer is configured.",
    producer: null,
    context: null,
    freshness: { state: "unavailable", observed_at: null, age_seconds: null, stale_after_seconds: 30 },
  });
  assert.match(unavailable, /Live market data not connected/);
  assert.match(unavailable, /data-market-status="unavailable"/);
  assert.doesNotMatch(unavailable, /historical/i);

  const current = renderHomeMarketOverview(currentMarket());
  assert.match(current, /EURUSD/);
  assert.match(current, /example-live-feed/);
  assert.match(current, /Current · 4s old/);
  assert.match(current, /Historical Research data is never substituted/);

  const staleRecord = currentMarket({
    status: "stale",
    reason_code: "producer_observation_stale",
    freshness: {
      state: "stale",
      observed_at: "2026-09-02T11:59:00Z",
      age_seconds: 60,
      stale_after_seconds: 30,
    },
  });
  const stale = renderHomeMarketOverview(staleRecord);
  assert.match(stale, /data-market-status="stale"/);
  assert.match(stale, /Stale · 60s old/);
  assert.doesNotMatch(stale, /Current · 60s old/);
});
