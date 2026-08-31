import assert from "node:assert/strict";
import { chromium } from "playwright";

import { runBrowserRegression } from "./browser-regression.mjs";

const baseUrl = process.env.TRADERCOCKPIT_BROWSER_BASE_URL || "http://127.0.0.1:4173";

const browser = await chromium.launch({ headless: true });
const context = await browser.newContext();
const page = await context.newPage();
const cdp = await context.newCDPSession(page);

const tab = {
  goto: (url) => page.goto(url, { waitUntil: "domcontentloaded" }),
  reload: () => page.reload({ waitUntil: "domcontentloaded" }),
  back: () => page.goBack({ waitUntil: "domcontentloaded" }),
  forward: () => page.goForward({ waitUntil: "domcontentloaded" }),
  playwright: {
    evaluate: (fn) => page.evaluate(fn),
    waitForTimeout: (ms) => page.waitForTimeout(ms),
    locator: (selector) => page.locator(selector),
  },
  capabilities: {
    get: async (name) => {
      if (name !== "cdp") throw new Error(`Unsupported browser capability: ${name}`);
      return cdp;
    },
  },
};

const digest = (char) => char.repeat(64);
const evidenceStrategyRef = `tc:strategy:v1:sha256:${digest("a")}`;
const otherStrategyRef = `tc:strategy:v1:sha256:${digest("9")}`;
const evidenceRunRef = `tc:backtest-run:v1:sha256:${digest("b")}`;
const evidenceManifestRef = `tc:evidence-manifest:v1:sha256:${digest("5")}`;
const evidencePayload = {
  schema: "tc.initial-run-read.v1",
  run_ref: evidenceRunRef,
  invocation_id: "browser-evidence-001",
  status: "passed",
  terminal: true,
  occurred_at: "2026-01-01T00:00:03.000000Z",
  reason_code: null,
  lifecycle_event_ref: `tc:run-lifecycle-event:v1:sha256:${digest("c")}`,
  inputs: {
    candidate_ref: `tc:candidate:v1:sha256:${digest("d")}`,
    strategy_ref: evidenceStrategyRef,
    data_ref: `tc:data:v1:sha256:${digest("e")}`,
    execution_ref: `tc:execution:v1:sha256:${digest("f")}`,
    engine_build_ref: `tc:engine-build:v1:sha256:${digest("0")}`,
    random_seed: null,
  },
  input_detail: {
    candidate: { origin: "manual" },
    strategy: { semantic_schema: "tc.strategy.rules.v1" },
    data: {
      symbol: "ES",
      timeframe: "1m",
      source: "fixture",
      dataset_revision: "browser-rev-1",
    },
    execution: { starting_cash: "100000", currency: "USD", models: [] },
    engine_build: { implementation: "tradercockpit", revision: "browser-r1" },
  },
  artifacts: {
    receipt_ref: `tc:run-receipt:v1:sha256:${digest("1")}`,
    result_ref: `tc:result:v1:sha256:${digest("2")}`,
    plan_ref: `tc:validation-plan:v1:sha256:${digest("3")}`,
    decision_ref: `tc:validation-decision:v1:sha256:${digest("4")}`,
    evidence_manifest_ref: evidenceManifestRef,
  },
  result: {
    result_schema: "tc.backtest.result.v1",
    producer_build_ref: `tc:engine-build:v1:sha256:${digest("0")}`,
  },
  validation: {
    passed: true,
    source_result_schema: "tc.backtest.result.v1",
    outcomes: [
      {
        metric_path: "metrics.profit_factor",
        operator: "gt",
        threshold: "1.3",
        actual: "1.5",
        passed: true,
      },
    ],
  },
};

async function proveStrategyEvidenceBrowserPath() {
  let requests = 0;
  await page.route("**/api/run-read?*", async (route) => {
    requests += 1;
    const url = new URL(route.request().url());
    assert.equal(url.searchParams.get("runRef"), evidenceRunRef);
    assert.equal(url.searchParams.get("invocationId"), "browser-evidence-001");
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(evidencePayload),
    });
  });

  const query = new URLSearchParams({
    runRef: evidenceRunRef,
    invocationId: "browser-evidence-001",
  }).toString();
  const evidencePath = `/strategies/${encodeURIComponent(evidenceStrategyRef)}/evidence?${query}`;

  await page.goto(`${baseUrl}${evidencePath}`, { waitUntil: "domcontentloaded" });
  const authority = page.locator(
    '[data-strategy-evidence-authority="true"][data-strategy-evidence-status="loaded"]',
  );
  await authority.waitFor({ state: "attached" });
  const firstText = await authority.textContent();
  assert.match(firstText, /Evidence verified/);
  assert.match(firstText, /Validation decision\s*Passed/);
  assert.match(firstText, /metrics\.profit_factor/);
  assert.match(firstText, /1\.5 gt 1\.3 · Passed/);
  assert.match(firstText, new RegExp(evidenceManifestRef.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")));

  const resultsLink = authority.locator('a[data-route^="/validate/results?"]');
  assert.equal(await resultsLink.count(), 1);
  const resultsHref = await resultsLink.getAttribute("href");
  const resultsUrl = new URL(resultsHref, baseUrl);
  assert.equal(resultsUrl.pathname, "/validate/results");
  assert.equal(resultsUrl.searchParams.get("runRef"), evidenceRunRef);
  assert.equal(resultsUrl.searchParams.get("invocationId"), "browser-evidence-001");

  await page.reload({ waitUntil: "domcontentloaded" });
  const reopened = page.locator(
    '[data-strategy-evidence-authority="true"][data-strategy-evidence-status="loaded"]',
  );
  await reopened.waitFor({ state: "attached" });
  const reopenedText = await reopened.textContent();
  assert.match(reopenedText, new RegExp(evidenceManifestRef.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")));
  assert.ok(requests >= 2, "Evidence reload must re-read the canonical run proof");

  const wrongPath = `/strategies/${encodeURIComponent(otherStrategyRef)}/evidence?${query}`;
  await page.goto(`${baseUrl}${wrongPath}`, { waitUntil: "domcontentloaded" });
  const refused = page.locator(
    '[data-strategy-evidence-authority="true"][data-strategy-evidence-status="error"]',
  );
  await refused.waitFor({ state: "attached" });
  const refusedText = await refused.textContent();
  assert.match(refusedText, /different strategy reference/);
  const resultText = await refused.locator("[data-strategy-evidence-result]").textContent();
  assert.doesNotMatch(
    resultText,
    new RegExp(evidenceManifestRef.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")),
  );

  await page.unroute("**/api/run-read?*");
}

try {
  const result = await runBrowserRegression(tab, { baseUrl });
  await proveStrategyEvidenceBrowserPath();
  console.log(
    `Browser regression passed: canonical ${result.canonical.length}, contextual ${result.contextual.length}, legacy ${result.legacy.length}; Strategies Evidence exact proof path passed`,
  );
} finally {
  await browser.close();
}
