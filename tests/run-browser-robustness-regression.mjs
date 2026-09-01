import assert from "node:assert/strict";

import { chromium } from "playwright";

const baseUrl = process.env.TRADERCOCKPIT_BROWSER_BASE_URL || "http://127.0.0.1:4173";

let browser = null;
try {
  browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  const missingValidation = `tc-evidence:sha256:${"f".repeat(64)}`;
  await page.goto(`${baseUrl}/research?stage=backtest&tab=robustness&validationRef=${missingValidation}`, { waitUntil: "domcontentloaded" });

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
  assert.match(text, /Producer unavailable/i);
  assert.doesNotMatch(text, /Native execution wired/i);
  assert.match(text, /Additional Markets/i);
  assert.match(text, /Monte Carlo · trade manipulation/i);
  assert.match(text, /Monte Carlo · full retest/i);
  assert.match(text, /System Parameter Permutation/i);
  assert.match(text, /Walk-Forward \/ Matrix/i);
  assert.match(text, /Not connected/i);
  assert.match(text, /No completed Historical Results/i);
  assert.match(text, /Saved robustness result unavailable/i);
  assert.doesNotMatch(text, /passed robustness/i);
  assert.doesNotMatch(text, /validation passed/i);

  const runButton = page.locator('[data-robustness-action="start"]');
  assert.equal(await runButton.count(), 1);
  assert.equal(await runButton.isDisabled(), true, "native Higher Precision is disabled without a configured Retester runtime/input");

  console.log("Backtest Robustness browser acceptance passed");
} finally {
  if (browser) await browser.close();
}
