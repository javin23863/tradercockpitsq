import assert from "node:assert/strict";
import { unlink } from "node:fs/promises";
import { chromium } from "playwright";

const baseUrl = process.env.TRADERCOCKPIT_OUTPUT_BROWSER_BASE_URL || "http://127.0.0.1:4174";
const expectedArchive = process.env.TRADERCOCKPIT_OUTPUT_ARCHIVE || "Generated Browser.sqx";
const sourcePath = process.env.TRADERCOCKPIT_OUTPUT_SOURCE_PATH
  || `/tmp/sqx-output-fixture/user/projects/Builder/databanks/Results/${expectedArchive}`;
const browser = await chromium.launch({ headless: true });
const context = await browser.newContext();
const page = await context.newPage();

try {
  await page.goto(`${baseUrl}/validate/run`, { waitUntil: "domcontentloaded" });
  const panel = page.locator("[data-sqx-output-panel]");
  await panel.locator("[data-sqx-output-card]").first().waitFor();

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
  const receiptText = await receipt.innerText();
  assert.match(receiptText, /tc:strategy:v1:sha256:/);
  assert.match(receiptText, /tc:candidate:v1:sha256:/);
  assert.match(receiptText, /sqx\.native-archive\.v1/);
  assert.match(receiptText, /Native Retester\s+Eligible/);
  assert.match(receiptText, /persisted in TraderCockpit custody/);
  assert.equal(await receipt.getByRole("link", { name: "Open strategy custody" }).count(), 1);
  const runButton = receipt.getByRole("button", { name: "Run native Retest" });
  assert.equal(await runButton.count(), 1);
  const candidateRef = await runButton.getAttribute("data-sqx-native-run");
  assert.match(candidateRef || "", /^tc:candidate:v1:sha256:[0-9a-f]{64}$/);

  // The imported candidate must remain a usable product object even when SQX
  // later clears/moves the mutable Builder Results source archive.
  await unlink(sourcePath);
  await page.reload({ waitUntil: "domcontentloaded" });
  const durablePanel = page.locator("[data-imported-sqx-candidates-panel]");
  await durablePanel.waitFor();
  const durableCard = durablePanel.locator(`[data-imported-sqx-candidate="${candidateRef}"]`);
  await durableCard.waitFor();
  assert.match(await durablePanel.innerText(), /TraderCockpit custody/);
  assert.match(await durablePanel.innerText(), /remain discoverable after Builder moves or clears/);
  assert.equal(await durableCard.getByRole("button", { name: "Run native Retest" }).count(), 1);
  assert.equal(
    await page.locator(`[data-sqx-output-card="${expectedArchive}"]`).count(),
    0,
    "live Builder source should be absent after cleanup",
  );

  console.log("SQX output browser integration passed: native archive -> durable custody -> Builder cleanup -> reload -> same runnable candidate");
} finally {
  await browser.close();
}
