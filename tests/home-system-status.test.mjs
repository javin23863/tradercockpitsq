import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import {
  fetchHomeSystemStatus,
  parseHomeSystemStatus,
  renderHomeSystemStatus,
} from "../web/home-system-status.mjs";

function runtimePayload(marketStatus = "unavailable") {
  const market = marketStatus === "unavailable"
    ? {
        schema: "tc.home-market-overview.v1",
        scope: "live_current",
        historical_fallback: false,
        status: "unavailable",
        reason_code: "producer_not_configured",
        detail: "No live/current market-data producer is configured.",
        producer: null,
        context: null,
        freshness: { state: "unavailable", observed_at: null, age_seconds: null, stale_after_seconds: 30 },
      }
    : {
        schema: "tc.home-market-overview.v1",
        scope: "live_current",
        historical_fallback: false,
        status: marketStatus,
        reason_code: marketStatus === "stale" ? "producer_observation_stale" : null,
        producer: { id: "feed" },
        context: { instrument: "EURUSD", timeframe: "M1", session: null, market_state: null, descriptors: {} },
        freshness: {
          state: marketStatus,
          observed_at: "2026-09-02T12:00:00Z",
          age_seconds: marketStatus === "stale" ? 60 : 4,
          stale_after_seconds: 30,
        },
      };

  return {
    schema: "tc.runtime-status.v1",
    application: { status: "ready", server: "canonical", desktop: "canonical-server-ui" },
    research_backend: {
      status: "ready",
      configured: true,
      verified: true,
      producer: "strategyquant-x",
      build: "144.2953",
      reason_code: null,
      execution: { available: false, reason_code: "trusted_launcher_not_configured" },
    },
    research_custody: { status: "ready", reason_code: null },
    market_data: market,
    provider: { status: "unavailable", reason_code: "provider_not_configured" },
    account: { status: "unavailable", reason_code: "authority_not_implemented" },
    model: { status: "unavailable", reason_code: "policy_not_implemented" },
    extensions: { status: "error", reason_code: "manifest_read_failed" },
  };
}

test("System Status parser requires every canonical Home health component", () => {
  const parsed = parseHomeSystemStatus(runtimePayload());
  assert.equal(parsed.application.status, "ready");
  assert.equal(parsed.research_backend.build, "144.2953");
  assert.equal(parsed.native_execution.status, "unavailable");
  assert.equal(parsed.market_data.status, "unavailable");
  assert.equal(parsed.provider.reason_code, "provider_not_configured");
  assert.equal(parsed.extensions.status, "error");

  const missingProvider = runtimePayload();
  delete missingProvider.provider;
  assert.throws(() => parseHomeSystemStatus(missingProvider), /provider is missing/);
});

test("System Status preserves current and stale live-data health without promotion", () => {
  const current = parseHomeSystemStatus(runtimePayload("current"));
  const stale = parseHomeSystemStatus(runtimePayload("stale"));
  assert.equal(current.market_data.status, "current");
  assert.equal(stale.market_data.status, "stale");

  const staleHtml = renderHomeSystemStatus(stale);
  assert.match(staleHtml, /data-runtime-component="market-data" data-runtime-state="stale"/);
  assert.match(staleHtml, /Stale · Producer Observation Stale/);
  assert.doesNotMatch(staleHtml, /market-data[^>]*data-runtime-state="ready"/);
});

test("System Status rejects fabricated ready Research identity and invalid live scope", () => {
  const badResearch = runtimePayload();
  badResearch.research_backend = {
    ...badResearch.research_backend,
    verified: false,
  };
  assert.throws(() => parseHomeSystemStatus(badResearch), /verified build identity/);

  const badMarket = runtimePayload("current");
  badMarket.market_data = { ...badMarket.market_data, scope: "historical" };
  assert.throws(() => parseHomeSystemStatus(badMarket), /live market status scope/);
});

test("System Status renders provider, account, model, extensions and native execution distinctly", () => {
  const html = renderHomeSystemStatus(parseHomeSystemStatus(runtimePayload()));
  assert.match(html, /TraderCockpit application/);
  assert.match(html, /Research backend/);
  assert.match(html, /Ready · StrategyQuant X 144\.2953/);
  assert.match(html, /Native execution/);
  assert.match(html, /Unavailable · Trusted Launcher Not Configured/);
  assert.match(html, /Live market data/);
  assert.match(html, /Model provider/);
  assert.match(html, /Unavailable · Provider Not Configured/);
  assert.match(html, /Consumer account/);
  assert.match(html, /Model access/);
  assert.match(html, /Extensions/);
  assert.match(html, /Error · Manifest Read Failed/);
});

test("System Status fetch uses only canonical runtime status", async () => {
  const payload = runtimePayload();
  const parsed = await fetchHomeSystemStatus(async (path, options) => {
    assert.equal(path, "/api/status");
    assert.equal(options.headers.accept, "application/json");
    return { ok: true, status: 200, json: async () => payload };
  });
  assert.equal(parsed.provider.reason_code, "provider_not_configured");

  await assert.rejects(
    () => fetchHomeSystemStatus(async () => ({ ok: false, status: 500, json: async () => ({}) })),
    /request failed: 500/,
  );
});

test("desktop loads the canonical Home System Status binder", async () => {
  const source = await readFile(new URL("../web/index.html", import.meta.url), "utf8");
  assert.match(source, /src="\/home-system-status\.mjs"/);
});
