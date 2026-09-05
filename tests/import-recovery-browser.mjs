// Manifest points to an isolated acceptance store with a prepared-only import.
// Phase 1 confirms deletion; the server injects one file-cleanup interruption.
// Phase 2 uses a new server port/context and retries the durable confirmation.
import assert from "node:assert/strict";
import { readFile, writeFile } from "node:fs/promises";
import { chromium } from "playwright";

const { base, request, run, phase, expected_preview_sha256 } = JSON.parse(await readFile(process.argv[2], "utf8"));
assert.equal(new URL(base).hostname, "127.0.0.1");
assert.ok([1, 2].includes(phase));
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width: phase === 1 ? 1440 : 960, height: 1000 } });
const errors = [], posts = [];
page.on("pageerror", error => errors.push(error.message));
await page.route("**/api/**", route => {
  const req = route.request(), path = new URL(req.url()).pathname;
  if (req.method() !== "GET") {
    assert.ok(["/api/desktop/session", "/api/sqx-databank/import-discard-preview", "/api/sqx-databank/import-discard-confirm"].includes(path), "No native operation may run");
    if (path !== "/api/desktop/session") posts.push({ path, body: req.postDataJSON() });
  }
  return route.continue();
});
try {
  const path = "/builder?" + new URLSearchParams({ tab: "results", task: "1", databank: request.databank });
  await page.goto(base + path);
  const retry = page.locator(`[data-dock-retry="${request.operation_id}"]`);
  await retry.waitFor();
  assert.equal(await page.evaluate(() => Object.keys(localStorage).filter(key => key.startsWith("tc.databank-operation.")).length), 0);
  assert.equal(posts.length, 0, "Opening recovery cannot submit an action");
  if (phase === 1) {
    await page.locator(`[data-dock-discard="${request.operation_id}"]`).click();
    const form = page.locator("[data-dock-purge-form]");
    await form.waitFor({ state: "visible" });
    const response = page.waitForResponse(res => res.url().endsWith("/import-discard-confirm"));
    await form.locator('button[type="submit"]').click();
    assert.equal((await response).ok(), false, "The isolated server must interrupt cleanup");
    await retry.filter({ hasText: "Retry import deletion" }).waitFor();
    assert.equal(posts.length, 2);
    assert.deepEqual(posts[0].body, request);
    const { expected_preview_sha256: hash, ...confirmed } = posts[1].body;
    assert.match(hash, /^[0-9a-f]{64}$/);
    assert.deepEqual(confirmed, request);
  } else {
    assert.match(await retry.innerText(), /Retry import deletion/);
    assert.equal(await page.locator(`[data-dock-discard="${request.operation_id}"]`).count(), 0);
    await retry.click();
    await page.locator("[data-dock-status]").filter({ hasText: "Unfinished import discarded" }).waitFor();
    assert.equal(await retry.count(), 0);
    assert.deepEqual(posts, [{ path: "/api/sqx-databank/import-discard-confirm", body: { ...request, expected_preview_sha256 } }]);
  }
  assert.deepEqual(errors, []);
  await page.screenshot({ path: `${run}/recovery-${phase}.png`, fullPage: true });
  await writeFile(`${run}/browser-${phase}.json`, JSON.stringify({ status: "passed", base, phase, posts, errors }, null, 2));
} finally { await browser.close(); }
