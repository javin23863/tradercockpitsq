import assert from "node:assert/strict";
import { chromium } from "playwright";

const baseUrl = process.env.TRADERCOCKPIT_OUTPUT_BROWSER_BASE_URL || "http://127.0.0.1:4174";
const expectedArchive = process.env.TRADERCOCKPIT_OUTPUT_ARCHIVE || "Generated Browser.sqx";
const browser = await chromium.launch({ headless: true });
const context = await browser.newContext();
const page = await context.newPage();

try {
  await page.goto(`${baseUrl}/validate/run`, { waitUntil: "domcontentloaded" });
  const panel = page.locator("[data-sqx-output-panel]");
  await panel.locator('[data-sqx-output-card]').first().waitFor();

  const cards = panel.locator("[data-sqx-output-card]");
  assert.equal(await cards.count(), 1, "expected one native SQX output fixture");
  const card = panel.locator(`[data-sqx-output-card="${expectedArchive}"]`);
  assert.equal(await card.count(), 1, "expected exact native SQX archive name");
  assert.match(await card.innerText(), /144\.2953/);

  await card.getByRole("button", { name: "Import to TraderCockpit" }).click();
  await page.waitForFunction(
    (archive) => document.querySelector(`[data-sqx-output-card="${archive}"]`)?.dataset.sqxCustodyStatus === "persisted",
    expectedArchive,
  );

  const receipt = card.locator("[data-sqx-custody-receipt]");
  assert.match(await receipt.innerText(), /tc:strategy:v1:sha256:/);
  assert.match(await receipt.innerText(), /tc:candidate:v1:sha256:/);
  assert.match(await receipt.innerText(), /sqx\.native-archive\.v1/);
  assert.match(await receipt.innerText(), /Not yet bound/);
  assert.equal(await receipt.getByRole("link", { name: "Open strategy custody" }).count(), 1);

  console.log("SQX output browser integration passed: native archive -> immutable strategy/candidate custody; run remains unbound");
} finally {
  await browser.close();
}
