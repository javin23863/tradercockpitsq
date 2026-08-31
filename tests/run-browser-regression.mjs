import assert from "node:assert/strict";
import { chromium } from "playwright";

import { runBrowserRegression } from "./browser-regression.mjs";

const baseUrl = process.env.TRADERCOCKPIT_BROWSER_BASE_URL || "http://127.0.0.1:4173";
const digest = (char) => char.repeat(64);
const strategyRef = `tc:strategy:v1:sha256:${digest("a")}`;
const otherStrategyRef = `tc:strategy:v1:sha256:${digest("c")}`;
const runRef = `tc:backtest-run:v1:sha256:${digest("1")}`;
const resultRef = `tc:result:v1:sha256:${digest("2")}`;
const invocationId = "overview-001";

function overviewPayload(returnedStrategyRef) {
  const engineBuildRef = `tc:engine-build:v1:sha256:${digest("9")}`;
  return {
    schema: "tc.initial-run-read.v1",
    run_ref: runRef,
    invocation_id: invocationId,
    status: "passed",
    terminal: true,
    occurred_at: "2026-08-31T00:00:00.000000Z",
    reason_code: null,
    lifecycle_event_ref: `tc:run-lifecycle-event:v1:sha256:${digest("5")}`,
    inputs: {
      candidate_ref: `tc:candidate:v1:sha256:${digest("6")}`,
      strategy_ref: returnedStrategyRef,
      data_ref: `tc:data:v1:sha256:${digest("7")}`,
      execution_ref: `tc:execution:v1:sha256:${digest("8")}`,
      engine_build_ref: engineBuildRef,
      random_seed: null,
    },
    input_detail: {
      candidate: { origin: "sqx-builder" },
      strategy: { semantic_schema: "tc.strategy.rules.v1" },
      data: {
        symbol: "ES",
        timeframe: "1m",
        source: "fixture",
        dataset_revision: "rev-a",
      },
      execution: { starting_cash: "100000", currency: "USD", models: [] },
      engine_build: { implementation: "tradercockpit", revision: "rev-engine" },
    },
    artifacts: {
      receipt_ref: `tc:run-receipt:v1:sha256:${digest("0")}`,
      result_ref: resultRef,
      plan_ref: `tc:validation-plan:v1:sha256:${digest("3")}`,
      decision_ref: `tc:validation-decision:v1:sha256:${digest("4")}`,
      evidence_manifest_ref: `tc:evidence-manifest:v1:sha256:${digest("b")}`,
    },
    result: {
      result_schema: "tc.backtest.result.v1",
      producer_build_ref: engineBuildRef,
    },
    validation: {
      passed: true,
      source_result_schema: "tc.backtest.result.v1",
      outcomes: [],
    },
  };
}

async function proveStrategyOverview(page) {
  let returnedStrategyRef = strategyRef;
  await page.route("**/api/run-read?*", async (route) => {
    const url = new URL(route.request().url());
    if (
      url.searchParams.get("runRef") !== runRef ||
      url.searchParams.get("invocationId") !== invocationId
    ) {
      await route.fulfill({
        status: 404,
        contentType: "application/json",
        body: JSON.stringify({ error: "not_found", detail: "unknown browser fixture run" }),
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(overviewPayload(returnedStrategyRef)),
    });
  });

  const overviewPath = `/strategies/${encodeURIComponent(strategyRef)}/overview`;
  await page.goto(`${baseUrl}${overviewPath}`, { waitUntil: "domcontentloaded" });
  const authority = page.locator("[data-strategy-overview-authority]");
  await authority.waitFor();
  await authority.locator('input[name="runRef"]').fill(runRef);
  await authority.locator('input[name="invocationId"]').fill(invocationId);
  await authority.locator('button[type="submit"]').click();
  await page.locator('[data-strategy-overview-authority][data-strategy-overview-status="loaded"]').waitFor();

  const loadedText = await authority.textContent();
  assert.match(loadedText, new RegExp(resultRef.replaceAll(":", "\\:")));
  assert.match(loadedText, /ES · 1m/);
  assert.match(loadedText, /sqx-builder/);
  assert.equal(
    await page.locator('[data-strategy-custody-status="verified"]').count(),
    1,
  );
  assert.equal(
    await page.locator('[data-strategy-activity-status="verified"]').count(),
    1,
  );

  const firstUrl = new URL(page.url());
  assert.equal(firstUrl.pathname, overviewPath);
  assert.equal(firstUrl.searchParams.get("runRef"), runRef);
  assert.equal(firstUrl.searchParams.get("invocationId"), invocationId);

  await page.reload({ waitUntil: "domcontentloaded" });
  await page.locator('[data-strategy-overview-authority][data-strategy-overview-status="loaded"]').waitFor();
  const reloadedText = await page.locator("[data-strategy-overview-authority]").textContent();
  assert.match(reloadedText, new RegExp(resultRef.replaceAll(":", "\\:")));
  assert.match(reloadedText, /ValidationPassed/);

  returnedStrategyRef = otherStrategyRef;
  await page.reload({ waitUntil: "domcontentloaded" });
  await page.locator('[data-strategy-overview-authority][data-strategy-overview-status="error"]').waitFor();
  const refusedText = await page.locator("[data-strategy-overview-authority]").textContent();
  assert.match(refusedText, /different strategy than this Overview route/);
  assert.equal(await page.locator('[data-strategy-custody-status="verified"]').count(), 0);
}

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

try {
  const result = await runBrowserRegression(tab, { baseUrl });
  console.log(
    `Browser regression passed: canonical ${result.canonical.length}, contextual ${result.contextual.length}, legacy ${result.legacy.length}`,
  );
  await proveStrategyOverview(page);
  console.log("Strategy Overview browser acceptance passed");
} finally {
  await browser.close();
}
