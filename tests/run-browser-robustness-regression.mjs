import assert from "node:assert/strict";

import { chromium } from "playwright";

const baseUrl = process.env.TRADERCOCKPIT_BROWSER_BASE_URL || "http://127.0.0.1:4173";

let browser = null;
try {
  browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  await page.goto(`${baseUrl}/research?stage=backtest&tab=robustness`, { waitUntil: "domcontentloaded" });

  for (let attempt = 0; attempt < 100; attempt += 1) {
    if (await page.locator("[data-robustness-workspace]").count()) break;
    await page.waitForTimeout(25);
  }

  assert.equal(
    await page.locator("[data-robustness-workspace]").count(),
    1,
    "Backtest Robustness must mount its producer-backed workspace",
  );
  const text = await page.locator("[data-robustness-workspace]").innerText();
  assert.match(text, /Native robustness methods/i);
  assert.match(text, /Higher Precision/i);
  assert.match(text, /System Parameter Permutation/i);
  assert.match(text, /Monte Carlo · trade manipulation/i);
  assert.equal(
    (text.match(/Native execution wired/gi) || []).length,
    3,
    "exactly the three implemented native methods are shown as wired",
  );
  assert.match(text, /Additional Markets/i);
  assert.match(text, /Monte Carlo · full retest/i);
  assert.match(text, /Walk-Forward \/ Matrix/i);
  assert.match(text, /Not connected/i);
  assert.match(text, /No completed Historical Results/i);
  assert.doesNotMatch(text, /passed robustness/i);
  assert.doesNotMatch(text, /validation passed/i);

  const higherButton = page.locator('[data-robustness-action="higher-precision"]');
  const systemParameterButton = page.locator('[data-robustness-action="system-parameter-permutation"]');
  const monteCarloButton = page.locator('[data-robustness-action="monte-carlo-manipulation"]');
  assert.equal(await higherButton.count(), 1);
  assert.equal(await systemParameterButton.count(), 1);
  assert.equal(await monteCarloButton.count(), 1);
  assert.equal(await higherButton.isDisabled(), true, "native Higher Precision is disabled without a configured Retester runtime/input");
  assert.equal(await systemParameterButton.isDisabled(), true, "native System Parameter Permutation is disabled without a configured Retester runtime/input");
  assert.equal(await monteCarloButton.isDisabled(), true, "native Monte Carlo is disabled without a configured Retester runtime/input");

  console.log("Backtest Robustness browser acceptance passed");
} finally {
  if (browser) await browser.close();
}
