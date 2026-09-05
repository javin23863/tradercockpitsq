// Run against a local application with a populated Builder Results archive.
// Only desktop selection persistence is writable; no native action may run.
import assert from "node:assert/strict";
import { chromium } from "playwright";

const initial = new URL(process.env.TRADERCOCKPIT_RESULTS_SESSION_URL);
assert.ok(["127.0.0.1", "localhost"].includes(initial.hostname));
const browser = await chromium.launch({ headless: true });
const errors = [], unexpected = [];
try {
  let context = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
  async function openPage() {
    const page = await context.newPage();
    page.on("pageerror", error => errors.push(error.message));
    await page.route("**/api/**", route => {
      const request = route.request();
      if (request.method() !== "GET" && new URL(request.url()).pathname !== "/api/desktop/session") {
        unexpected.push(request.url());
        return route.abort();
      }
      return route.continue();
    });
    return page;
  }
  let page = await openPage();
  await page.goto(initial.href);
  await page.locator("[data-results-sample]").waitFor();
  await page.locator('[data-automation-result-view="trade-analysis"]').click();
  await page.locator("[data-results-sample]").selectOption("oos");
  await page.locator("[data-results-direction]").selectOption("long");
  await page.locator("[data-results-period]").selectOption("open_time");
  const selected = new URL(page.url());
  assert.equal(selected.searchParams.get("archive"), initial.searchParams.get("archive"));
  assert.equal(selected.searchParams.get("databank"), initial.searchParams.get("databank"));
  await page.waitForFunction(async () => {
    const saved = await (await fetch("/api/desktop/session")).json();
    return saved.path === location.pathname + location.search;
  });
  // A fresh browser context has no storage or history from the first window.
  await context.close();
  context = await browser.newContext({ viewport: { width: 960, height: 1000 } });
  const response = await context.request.get(initial.origin + "/api/desktop/session");
  assert.equal(response.status(), 200);
  const saved = await response.json();
  page = await openPage();
  await page.goto(initial.origin + saved.path);
  await page.locator("[data-results-period]").waitFor();
  assert.equal(new URL(page.url()).search, selected.search);
  assert.equal(await page.locator("[data-results-sample]").inputValue(), "oos");
  assert.equal(await page.locator("[data-results-direction]").inputValue(), "long");
  assert.equal(await page.locator("[data-results-period]").inputValue(), "open_time");
  assert.equal(await page.locator('[data-automation-result-view="trade-analysis"]').getAttribute("aria-selected"), "true");
  assert.deepEqual(errors, []);
  assert.deepEqual(unexpected, []);
  console.log(JSON.stringify({ status: "passed", restored: saved.path, fresh_browser_context: true, errors, unexpected }));
} finally { await browser.close(); }
