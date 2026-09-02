import assert from "node:assert/strict";

import { chromium } from "playwright";

const baseUrl = process.env.TRADERCOCKPIT_BROWSER_BASE_URL || "http://127.0.0.1:4173";

let browser = null;
try {
  browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  const missingValidation = `tc-evidence:sha256:${"f".repeat(64)}`;

  await page.goto(`${baseUrl}/home`, { waitUntil: "domcontentloaded" });
  await page.getByRole("link", { name: "Open Research", exact: true }).click();
  await page.getByRole("link", { name: "Backtest", exact: true }).click();
  await page.getByRole("link", { name: "Robustness", exact: true }).click();
  for (let attempt = 0; attempt < 100; attempt += 1) {
    if (await page.locator("[data-robustness-workspace]").count()) break;
    await page.waitForTimeout(25);
  }
  assert.equal(
    await page.locator("[data-robustness-workspace]").count(),
    1,
    "Backtest Robustness must mount after ordinary SPA navigation from Home",
  );

  await page.goto(`${baseUrl}/research?stage=backtest&tab=robustness&validationRef=${missingValidation}`, { waitUntil: "domcontentloaded" });
  for (let attempt = 0; attempt < 100; attempt += 1) {
    if (await page.locator("[data-robustness-workspace]").count()) break;
    await page.waitForTimeout(25);
  }
  const workspace = page.locator("[data-robustness-workspace]");
  assert.equal(
    await workspace.count(),
    1,
    "Backtest Robustness must also mount on direct bookmarked entry",
  );

  await workspace.getByText("Producer unavailable", { exact: false }).first().waitFor({ state: "visible" });
  const text = await workspace.innerText();
  assert.match(text, /Native robustness methods/i);
  assert.match(text, /Higher Precision/i);
  assert.match(text, /Producer unavailable/i);
  assert.doesNotMatch(text, /Checking producer/i);
  assert.doesNotMatch(text, /Native execution wired/i);
  assert.match(text, /Additional Markets/i);
  assert.match(text, /Monte Carlo retest/i);
  assert.match(text, /Walk-Forward/i);
  assert.match(text, /Walk-Forward Matrix/i);
  assert.match(text, /What-If/i);
  assert.match(text, /System Parameter Permutation/i);
  assert.match(text, /Monte Carlo manipulation/i);
  assert.match(text, /Sequential Optimization/i);
  assert.doesNotMatch(text, /Not connected/i);
  assert.doesNotMatch(text, /Monte Carlo · trade manipulation/i);
  assert.doesNotMatch(text, /Monte Carlo · full retest/i);
  assert.doesNotMatch(text, /Walk-Forward \/ Matrix/i);
  assert.match(text, /No completed Historical Results/i);
  assert.match(text, /Saved robustness result unavailable/i);
  assert.doesNotMatch(text, /passed robustness/i);
  assert.doesNotMatch(text, /validation passed/i);

  const runButton = page.locator('[data-robustness-action="start"]');
  assert.equal(await runButton.count(), 1);
  assert.equal(await runButton.isDisabled(), true, "native Higher Precision is disabled without a configured Retester runtime/input");
  const additionalButton = page.locator('[data-robustness-action="start-additional-markets"]');
  assert.equal(await additionalButton.count(), 1);
  assert.equal(await additionalButton.isDisabled(), true, "native Additional Markets is disabled without a configured Retester runtime/input");
  const walkForwardButton = page.locator('[data-robustness-action="start-walk-forward"]');
  assert.equal(await walkForwardButton.count(), 1);
  assert.equal(await walkForwardButton.isDisabled(), true, "native Walk-Forward is disabled without a configured Retester runtime/input");
  const manipulationButton = page.locator('[data-robustness-action="start-monte-carlo-manipulation"]');
  assert.equal(await manipulationButton.count(), 1);
  assert.equal(await manipulationButton.isDisabled(), true, "native Monte Carlo manipulation is disabled without a configured Retester runtime/input");
  const sequentialButton = page.locator('[data-robustness-action="start-sequential-optimization"]');
  assert.equal(await sequentialButton.count(), 1);
  assert.equal(await sequentialButton.isDisabled(), true, "native Sequential Optimization is disabled without a configured Retester runtime/input");

  console.log("Backtest Robustness browser acceptance passed");
} finally {
  if (browser) await browser.close();
}
