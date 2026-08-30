import assert from "node:assert/strict";
import { chromium } from "playwright";

const baseUrl = process.env.TRADERCOCKPIT_BUILDER_BROWSER_BASE_URL || "http://127.0.0.1:4175";
const strategyRef = "  browser builder opaque % + ? # & = Khmer ខ្មែរ  ";
const path = `/strategies/${encodeURIComponent(strategyRef)}/candidates`;
const browser = await chromium.launch({ headless: true });
const context = await browser.newContext();
const page = await context.newPage();

async function waitForCatalog(state = "ready") {
  await page.waitForFunction(
    (expected) => document.querySelector("[data-builder-candidate-catalog]")?.dataset.builderCandidateCatalogState === expected,
    state,
  );
}

async function candidateSnapshot() {
  return page.locator("[data-builder-candidate-record]").evaluateAll((cards) => cards.map((card) => ({
    candidateRef: card.dataset.builderCandidateRecord,
    text: card.textContent,
  })));
}

try {
  await page.goto(`${baseUrl}${path}`, { waitUntil: "domcontentloaded" });
  await waitForCatalog();
  assert.equal(
    await page.locator("[data-candidates-authority]").getAttribute("data-requested-strategy-ref"),
    strategyRef,
    "Candidates page must preserve the exact opaque requested reference",
  );
  assert.equal(
    await page.locator("[data-builder-candidate-record]").count(),
    0,
    "fresh fixture must begin with no persisted candidates",
  );
  assert.match(
    await page.locator("[data-builder-candidate-catalog]").innerText(),
    /No persisted Builder candidates/i,
  );

  const bounded = page.getByRole("button", { name: "Build bounded candidates" });
  assert.equal(await bounded.count(), 1, "bounded Builder action must be present");
  await bounded.click();
  await page.waitForFunction(() => document.querySelectorAll("[data-builder-candidate-record]").length > 0);

  const actionText = await page.locator('[data-builder-action-state="bounded"]').innerText();
  assert.match(actionText, /Search complete/i);
  assert.match(actionText, /persisted candidates/i);

  const first = await candidateSnapshot();
  assert.equal(first.length, 4, "bounded product preset must persist four surviving candidates");
  assert.equal(new Set(first.map((row) => row.candidateRef)).size, first.length, "candidate identities must be unique");
  for (const row of first) {
    assert.match(row.candidateRef, /^tc:candidate:v1:sha256:[0-9a-f]{64}$/);
    assert.match(row.text, /tc:strategy:v1:sha256:/);
    assert.match(row.text, /tc:builder-lineage:v1:sha256:/);
    assert.match(row.text, /Objective/);
  }

  await page.reload({ waitUntil: "domcontentloaded" });
  await waitForCatalog();
  assert.equal(
    await page.locator("[data-candidates-authority]").getAttribute("data-requested-strategy-ref"),
    strategyRef,
    "reload must preserve the exact opaque requested reference",
  );
  const reopened = await candidateSnapshot();
  assert.deepEqual(
    reopened,
    first,
    "reload must reopen the same durable candidate identities and displayed objective custody",
  );

  console.log("Builder Candidates browser integration passed: exact opaque ref -> click -> search -> custody -> reload/reopen");
} finally {
  await browser.close();
}
