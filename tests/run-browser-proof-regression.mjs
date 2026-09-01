import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

import { chromium } from "playwright";

const host = "127.0.0.1";
const port = Number(process.env.TRADERCOCKPIT_PROOF_BROWSER_PORT || "4175");
const baseUrl = `http://${host}:${port}`;
const python = process.env.PYTHON || "python";

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function waitForReady(stderr) {
  for (let attempt = 0; attempt < 100; attempt += 1) {
    try {
      const response = await fetch(`${baseUrl}/api/status`);
      if (response.ok) return;
    } catch {
      // Server is still binding.
    }
    await delay(50);
  }
  throw new Error(`Research Proof fixture server did not become ready.\n${stderr()}`);
}

async function startFixture(dataRoot) {
  const child = spawn(
    python,
    [
      "tests/product/proof_browser_fixture_server.py",
      "--host",
      host,
      "--port",
      String(port),
      "--data-root",
      dataRoot,
      "--web-root",
      "web",
    ],
    { stdio: ["ignore", "pipe", "pipe"] },
  );
  child.stdout.setEncoding("utf8");
  child.stderr.setEncoding("utf8");
  let stdout = "";
  let stderr = "";
  child.stderr.on("data", (chunk) => { stderr += chunk; });

  const fixture = await new Promise((resolve, reject) => {
    let settled = false;
    const finish = (callback, value) => {
      if (settled) return;
      settled = true;
      callback(value);
    };
    const timer = setTimeout(() => {
      finish(reject, new Error(`Timed out waiting for PROOF_FIXTURE marker.\n${stderr}`));
    }, 10_000);
    child.stdout.on("data", (chunk) => {
      stdout += chunk;
      for (const line of stdout.split(/\r?\n/)) {
        if (!line.startsWith("PROOF_FIXTURE=")) continue;
        clearTimeout(timer);
        try {
          finish(resolve, JSON.parse(line.slice("PROOF_FIXTURE=".length)));
        } catch (error) {
          finish(reject, error);
        }
        return;
      }
    });
    child.once("exit", (code, signal) => {
      clearTimeout(timer);
      finish(reject, new Error(`Proof fixture exited before readiness: code=${code} signal=${signal}\n${stderr}`));
    });
  });

  await waitForReady(() => stderr);
  return { child, fixture, stderr: () => stderr };
}

async function stopFixture(server) {
  if (!server?.child || server.child.exitCode !== null) return;
  const exited = new Promise((resolve) => server.child.once("exit", resolve));
  server.child.kill("SIGTERM");
  await Promise.race([
    exited,
    (async () => {
      await delay(3000);
      if (server.child.exitCode === null) server.child.kill("SIGKILL");
      await exited;
    })(),
  ]);
}

async function assertBookmarkedProof(page, fixture) {
  const url = `${baseUrl}/research?stage=proof&proofEntity=${encodeURIComponent(fixture.entity_id)}`;
  await page.goto(url, { waitUntil: "domcontentloaded" });
  const workspace = page.locator("[data-research-proof-workspace]");
  await workspace.waitFor({ state: "visible" });
  const proof = page.locator(`[data-research-proof-entity="${fixture.entity_id}"]`);
  await proof.waitFor({ state: "visible" });
  const text = await proof.innerText();
  assert.match(text, /Exact Research chain recovered/i);
  assert.ok(text.includes(fixture.revision), "bookmarked Proof must reopen the exact immutable Proof revision");
  assert.ok(text.includes(fixture.idea_revision), "bookmarked Proof must preserve the exact Idea revision");
  assert.ok(text.includes(fixture.historical_result_revision), "bookmarked Proof must preserve the exact Historical Result revision");
  assert.ok(text.includes(fixture.validation_ref), "bookmarked Proof must preserve the exact Higher Precision validation reference");
  assert.match(text, /Outcome unread/i);
  assert.doesNotMatch(text, /validation passed/i);
  assert.doesNotMatch(text, /passed robustness/i);

  const response = await page.request.get(`${baseUrl}/api/research/proofs?${new URLSearchParams({ entityId: fixture.entity_id })}`);
  assert.equal(response.ok(), true, "canonical Proof API must reopen the bookmarked Proof");
  const payload = await response.json();
  assert.equal(payload.entity_id, fixture.entity_id);
  assert.equal(payload.revision, fixture.revision);
  assert.equal(payload.idea.revision, fixture.idea_revision);
  assert.equal(payload.historical_result.revision, fixture.historical_result_revision);
  assert.equal(payload.validation.validation_ref, fixture.validation_ref);
  assert.equal(payload.truth.producer_verdict_available, false);
}

const dataRoot = await mkdtemp(join(tmpdir(), "tradercockpit-proof-browser-"));
let browser = null;
let first = null;
let second = null;
try {
  first = await startFixture(dataRoot);
  assert.equal(first.fixture.reused, false, "fresh data root must mint the user-facing Proof once");

  browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  await assertBookmarkedProof(page, first.fixture);

  await stopFixture(first);
  first = null;

  second = await startFixture(dataRoot);
  assert.equal(second.fixture.reused, true, "restart must reuse the persisted immutable Proof");
  assert.equal(second.fixture.entity_id, (await page.locator("[data-research-proof-entity]").getAttribute("data-research-proof-entity")));
  assert.equal(second.fixture.revision, second.fixture.revision);
  assert.equal(second.fixture.entity_id, second.fixture.entity_id);
  await assertBookmarkedProof(page, second.fixture);

  console.log(`Research Proof restart browser acceptance passed: ${second.fixture.entity_id} ${second.fixture.revision}`);
} finally {
  await stopFixture(first);
  await stopFixture(second);
  if (browser) await browser.close();
  await rm(dataRoot, { recursive: true, force: true });
}
