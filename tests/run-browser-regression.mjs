import assert from "node:assert/strict";
import { chromium } from "playwright";

import { runBrowserRegression } from "./browser-regression.mjs";

const baseUrl = process.env.TRADERCOCKPIT_BROWSER_BASE_URL || "http://127.0.0.1:4173";
const digest = (char) => char.repeat(64);
const strategyRef = `tc:strategy:v1:sha256:${digest("a")}`;
const leftRunRef = `tc:backtest-run:v1:sha256:${digest("1")}`;
const rightRunRef = `tc:backtest-run:v1:sha256:${digest("2")}`;
const leftResultRef = `tc:result:v1:sha256:${digest("3")}`;
const rightResultRef = `tc:result:v1:sha256:${digest("4")}`;

function comparisonPayload({
  runRef,
  invocationId,
  lifecycleDigest,
  candidateDigest,
  dataDigest,
  executionDigest,
  engineDigest,
  resultRef,
  planDigest,
  decisionDigest,
  evidenceDigest,
  actual,
  resultSchema,
}) {
  const engineRef = `tc:engine-build:v1:sha256:${digest(engineDigest)}`;
  return {
    schema: "tc.initial-run-read.v1",
    run_ref: runRef,
    invocation_id: invocationId,
    status: "passed",
    terminal: true,
    occurred_at: "2026-01-01T00:00:00.000000Z",
    reason_code: null,
    lifecycle_event_ref: `tc:run-lifecycle-event:v1:sha256:${digest(lifecycleDigest)}`,
    inputs: {
      candidate_ref: `tc:candidate:v1:sha256:${digest(candidateDigest)}`,
      strategy_ref: strategyRef,
      data_ref: `tc:data:v1:sha256:${digest(dataDigest)}`,
      execution_ref: `tc:execution:v1:sha256:${digest(executionDigest)}`,
      engine_build_ref: engineRef,
      random_seed: null,
    },
    input_detail: {
      candidate: { origin: "manual" },
      strategy: { semantic_schema: "tc.strategy.rules.v1" },
      data: {
        symbol: "ES",
        timeframe: "1m",
        source: "fixture",
        dataset_revision: `rev-${dataDigest}`,
      },
      execution: { starting_cash: "100000", currency: "USD", models: [] },
      engine_build: { implementation: "tradercockpit", revision: `r-${engineDigest}` },
    },
    artifacts: {
      receipt_ref: `tc:run-receipt:v1:sha256:${digest("0")}`,
      result_ref: resultRef,
      plan_ref: `tc:validation-plan:v1:sha256:${digest(planDigest)}`,
      decision_ref: `tc:validation-decision:v1:sha256:${digest(decisionDigest)}`,
      evidence_manifest_ref: `tc:evidence-manifest:v1:sha256:${digest(evidenceDigest)}`,
    },
    result: {
      result_schema: resultSchema,
      producer_build_ref: engineRef,
    },
    validation: {
      passed: true,
      source_result_schema: resultSchema,
      outcomes: [
        {
          metric_path: "metrics.profit_factor",
          operator: "gt",
          threshold: "1.3",
          actual,
          passed: true,
        },
      ],
    },
  };
}

async function proveResultComparison(page) {
  let rightSchema = "tc.backtest.result.v1";
  await page.route("**/api/run-read?*", async (route) => {
    const url = new URL(route.request().url());
    const runRef = url.searchParams.get("runRef");
    const invocationId = url.searchParams.get("invocationId");
    let payload;
    if (runRef === leftRunRef && invocationId === "left-001") {
      payload = comparisonPayload({
        runRef: leftRunRef,
        invocationId,
        lifecycleDigest: "5",
        candidateDigest: "6",
        dataDigest: "7",
        executionDigest: "8",
        engineDigest: "9",
        resultRef: leftResultRef,
        planDigest: "a",
        decisionDigest: "b",
        evidenceDigest: "c",
        actual: "1.5",
        resultSchema: "tc.backtest.result.v1",
      });
    } else if (runRef === rightRunRef && invocationId === "right-001") {
      payload = comparisonPayload({
        runRef: rightRunRef,
        invocationId,
        lifecycleDigest: "d",
        candidateDigest: "e",
        dataDigest: "f",
        executionDigest: "1",
        engineDigest: "2",
        resultRef: rightResultRef,
        planDigest: "3",
        decisionDigest: "4",
        evidenceDigest: "5",
        actual: "1.8",
        resultSchema: rightSchema,
      });
    } else {
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
      body: JSON.stringify(payload),
    });
  });

  await page.goto(`${baseUrl}/validate/compare`, { waitUntil: "domcontentloaded" });
  const authority = page.locator("[data-result-compare-authority]");
  await authority.waitFor();
  await authority.locator('input[name="leftRunRef"]').fill(leftRunRef);
  await authority.locator('input[name="leftInvocationId"]').fill("left-001");
  await authority.locator('input[name="rightRunRef"]').fill(rightRunRef);
  await authority.locator('input[name="rightInvocationId"]').fill("right-001");
  await authority.locator('button[type="submit"]').click();
  await page.locator('[data-result-compare-authority][data-result-compare-status="loaded"]').waitFor();

  const loadedText = await authority.textContent();
  assert.match(loadedText, new RegExp(leftResultRef.replaceAll(":", "\\:")));
  assert.match(loadedText, new RegExp(rightResultRef.replaceAll(":", "\\:")));
  assert.match(loadedText, /Comparable result schema: tc\.backtest\.result\.v1/);
  assert.match(loadedText, /Left: 1\.5 · Passed · Right: 1\.8 · Passed/);
  assert.doesNotMatch(loadedText, /winner|best strategy|superior strategy/i);

  const firstUrl = new URL(page.url());
  assert.equal(firstUrl.pathname, "/validate/compare");
  assert.equal(firstUrl.searchParams.get("leftRunRef"), leftRunRef);
  assert.equal(firstUrl.searchParams.get("leftInvocationId"), "left-001");
  assert.equal(firstUrl.searchParams.get("rightRunRef"), rightRunRef);
  assert.equal(firstUrl.searchParams.get("rightInvocationId"), "right-001");

  await page.reload({ waitUntil: "domcontentloaded" });
  await page.locator('[data-result-compare-authority][data-result-compare-status="loaded"]').waitFor();
  const reloadedText = await page.locator("[data-result-compare-authority]").textContent();
  assert.match(reloadedText, new RegExp(leftResultRef.replaceAll(":", "\\:")));
  assert.match(reloadedText, new RegExp(rightResultRef.replaceAll(":", "\\:")));
  assert.match(reloadedText, /Left: 1\.5 · Passed · Right: 1\.8 · Passed/);

  rightSchema = "tc.other.result.v1";
  await page.reload({ waitUntil: "domcontentloaded" });
  await page.locator('[data-result-compare-authority][data-result-compare-status="error"]').waitFor();
  const refusedText = await page.locator("[data-result-compare-authority]").textContent();
  assert.match(refusedText, /result schemas differ/);
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
  await proveResultComparison(page);
  console.log("Result comparison browser acceptance passed");
} finally {
  await browser.close();
}
