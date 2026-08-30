import assert from "node:assert/strict";
import { chromium } from "playwright";

const baseUrl = process.env.TRADERCOCKPIT_BROWSER_BASE_URL || "http://127.0.0.1:4173";
const browser = await chromium.launch({ headless: true });
const context = await browser.newContext();
const page = await context.newPage();

try {
  await page.goto(`${baseUrl}/validate/run`, { waitUntil: "domcontentloaded" });
  await page.locator('[data-sqx-preset-catalog-state="ready"]').waitFor();

  const cards = page.locator("[data-sqx-preset-card]");
  assert.equal(await cards.count(), 3, "expected exactly three reviewed SQX market presets");
  assert.deepEqual(
    await cards.locator(".route-card-title").allTextContents(),
    ["Forex", "Futures", "Stocks"],
  );
  assert.equal(await page.getByText("Runtime verification pending").count(), 3);

  await page.locator('[data-sqx-preset-id="sqx-default-futures"]').click();
  await page.waitForURL(/presetId=sqx-default-futures/);
  await page.locator('[data-sqx-preset-catalog-state="ready"]').waitFor();
  const runSurface = page.locator('[data-run-surface-id="shared-run-surface"]');
  assert.equal(await runSurface.getAttribute("data-sqx-preset-id"), "sqx-default-futures");
  assert.match(await runSurface.locator(".run-field strong").first().innerText(), /Futures · SQX 144\.2953/);
  assert.equal(await runSurface.getByRole("button", { name: "Start run" }).isDisabled(), true);

  const adversarial = "  opaque/percent%+query?#&= Khmer ខ្មែរ  ";
  const url = new URL(`${baseUrl}/validate/run`);
  url.searchParams.set("strategyRef", adversarial);
  await page.goto(url.toString(), { waitUntil: "domcontentloaded" });
  await page.locator('[data-sqx-preset-catalog-state="ready"]').waitFor();
  await page.locator('[data-sqx-preset-id="sqx-default-stockpicker"]').click();
  await page.waitForURL(/presetId=sqx-default-stockpicker/);
  const selected = new URL(page.url());
  assert.equal(selected.searchParams.get("strategyRef"), adversarial);
  assert.equal(selected.searchParams.get("presetId"), "sqx-default-stockpicker");

  console.log("SQX preset browser integration passed: 3 presets, selection identity, strategy context, fail-closed start");
} finally {
  await browser.close();
}
