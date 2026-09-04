import { createHash } from "node:crypto";
import { spawn, spawnSync } from "node:child_process";
import { mkdir, mkdtemp, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

import { chromium } from "playwright";

const python = process.env.PYTHON || "python3";
const baseUrl = process.env.TRADERCOCKPIT_MODULE_RAIL_URL || "http://127.0.0.1:42476";
const RETAINED_REFERENCE_HEAD = "958e2fe2910cbf71d51ae29e4951484a86fc4ab6";
const RETAINED_BUILDER_PROJECT_PATH = "references/strategyquant-x-144.2953/user/projects/Builder/project.cfx";
const RETAINED_BUILDER_PROJECT_GIT_BLOB_SHA1 = "6194322a7a6feab40e02d9d9ed741401749a51d1";
const RETAINED_BUILDER_PROJECT_SIZE = 47153;
const PROJECT = "RetainedBuildTask";
const EXPECTED_NAV = [
  "/home",
  "/builder",
  "/retester",
  "/optimizer",
  "/data-manager",
  "/custom-projects",
  "/algowizard",
  "/operate",
  "/settings",
];
const EXPECTED_LABELS = [
  "Getting started",
  "Builder",
  "Retester",
  "Optimizer",
  "Data manager",
  "Custom projects",
  "AlgoWizard",
  "Operate",
  "Settings",
];

function commandOutput(result) {
  return `${result.stderr?.toString?.() || ""}${result.stdout?.toString?.() || ""}`.trim();
}

async function fetchJsonWithTimeout(url, { fetchImpl = fetch, timeoutMs = 5000 } = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetchImpl(url, { headers: { accept: "application/json" }, signal: controller.signal });
    const payload = await response.json().catch(() => null);
    if (!response?.ok) throw new Error(payload?.detail || payload?.reason_code || `HTTP ${response?.status ?? "unknown"}`);
    return payload;
  } finally {
    clearTimeout(timer);
  }
}

function referenceBuilderArchive() {
  const shown = spawnSync(
    "git",
    ["show", `${RETAINED_REFERENCE_HEAD}:${RETAINED_BUILDER_PROJECT_PATH}`],
    { maxBuffer: 2 * 1024 * 1024 },
  );
  if (shown.status !== 0 || !Buffer.isBuffer(shown.stdout)) {
    throw new Error(`could not materialize retained SQX Builder archive: ${commandOutput(shown)}`);
  }
  const archive = shown.stdout;
  const gitBlobSha1 = createHash("sha1")
    .update(Buffer.from(`blob ${archive.length}\0`, "ascii"))
    .update(archive)
    .digest("hex");
  if (archive.length !== RETAINED_BUILDER_PROJECT_SIZE || gitBlobSha1 !== RETAINED_BUILDER_PROJECT_GIT_BLOB_SHA1) {
    throw new Error(
      "retained SQX Builder archive identity mismatch: "
      + `expected ${RETAINED_BUILDER_PROJECT_SIZE} bytes/${RETAINED_BUILDER_PROJECT_GIT_BLOB_SHA1}, `
      + `observed ${archive.length} bytes/${gitBlobSha1}`,
    );
  }
  return archive;
}

const fixtureRoot = await mkdtemp(join(tmpdir(), "tc-module-rail-"));
await mkdir(join(fixtureRoot, "internal/web/SQUANT"), { recursive: true });
await writeFile(join(fixtureRoot, "internal/web/SQUANT/build.dat"), "2953", "utf8");
await mkdir(join(fixtureRoot, "internal"), { recursive: true });
await writeFile(join(fixtureRoot, "internal/SQUANT.dat"), Buffer.from("144fixture"));
const archive = referenceBuilderArchive();
await mkdir(join(fixtureRoot, "user/projects/Builder/databanks/Results"), { recursive: true });
await writeFile(join(fixtureRoot, "user/projects/Builder/project.cfx"), archive);
await mkdir(join(fixtureRoot, `user/projects/${PROJECT}`), { recursive: true });
await writeFile(join(fixtureRoot, `user/projects/${PROJECT}/project.cfx`), archive);
const dataRoot = join(fixtureRoot, "application-data");

const server = spawn(
  python,
  [
    "-m",
    "tradercockpit.app_server",
    "--host",
    "127.0.0.1",
    "--port",
    new URL(baseUrl).port || "42475",
    "--sqx-home",
    fixtureRoot,
    "--data-root",
    dataRoot,
    "--web-root",
    join(process.cwd(), "web"),
  ],
  {
    stdio: ["ignore", "pipe", "pipe"],
    env: { ...process.env, PYTHONPATH: `${process.cwd()}/product`, OPENROUTER_API_KEY: "" },
  },
);
let serverOutput = "";
server.stdout.on("data", (chunk) => { serverOutput += chunk.toString(); });
server.stderr.on("data", (chunk) => { serverOutput += chunk.toString(); });

async function waitForServer() {
  for (let attempt = 0; attempt < 80; attempt += 1) {
    if (server.exitCode !== null) break;
    try {
      const payload = await fetchJsonWithTimeout(`${baseUrl}/api/sqx-module?module=Builder`, { timeoutMs: 1000 });
      if (payload?.schema === "tc.sqx-run-module.v1") return payload;
    } catch {
      // startup race
    }
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  throw new Error(`module rail server did not become ready\n${serverOutput}`);
}

const builderModule = await waitForServer();
server.unref();
if (builderModule.status !== "ready" || builderModule.project !== "Builder") {
  throw new Error(`Builder module was not ready: ${JSON.stringify(builderModule)}`);
}

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage();
page.setDefaultTimeout(30000);
page.setDefaultNavigationTimeout(30000);
try {
  console.log("Proof: loading Getting started / home");
  await page.goto(`${baseUrl}/home`, { waitUntil: "domcontentloaded" });
  await page.locator("[data-product-shell]").waitFor({ timeout: 20000 });
  const nav = await page.locator(".primary-nav [data-route]").evaluateAll((nodes) => (
    nodes.map((node) => ({ href: node.getAttribute("data-route"), label: node.textContent.trim() }))
  ));
  console.log("Proof: rail labels/routes verified");
  if (nav.map((item) => item.href).join(",") !== EXPECTED_NAV.join(",")) {
    throw new Error(`rail routes were ${nav.map((item) => item.href).join("|")}`);
  }
  if (nav.map((item) => item.label).join(",") !== EXPECTED_LABELS.join(",")) {
    throw new Error(`rail labels were ${nav.map((item) => item.label).join("|")}`);
  }
  const homeText = await page.locator("body").innerText();
  if (homeText.includes("Explore") && nav.some((item) => item.label === "Explore")) {
    throw new Error("Explore remained a left-rail label");
  }
  if (nav.some((item) => /Research|Explore|Automation|Evolutionary Search|Signals/.test(item.label))) {
    throw new Error(`invented pipeline labels remained on the rail: ${nav.map((item) => item.label).join("|")}`);
  }

  console.log("Proof: Builder module settings shell");
  await page.locator('.primary-nav a[href="/builder"]').click();
  await page.locator('[data-automation-workflows="loaded"]').waitFor({ timeout: 40000 });
  await page.locator('[data-sqx-module-mode="run"]').waitFor({ timeout: 10000 });
  await page.goto(`${baseUrl}/builder?tab=settings&task=1&section=WhatToBuild`, { waitUntil: "domcontentloaded" });
  await page.locator('[data-automation-workflows="loaded"]').waitFor({ timeout: 60000 });
  await page.locator('[data-settings-tag="WhatToBuild"]').waitFor({ timeout: 40000 });
  const whatHtml = await page.locator("form.full-settings").innerHTML();
  for (const group of ["Strategy type", "Trading direction / symmetry", "Build mode", "Stop loss", "Profit target"]) {
    if (!whatHtml.includes(`data-settings-group="${group}"`)) {
      throw new Error(`Builder Full settings missing documented group ${group}`);
    }
  }
  if (whatHtml.includes("BASIC") || whatHtml.includes("EXTENSIVE") || whatHtml.includes("Net Profit")) {
    throw new Error("Builder Full settings invented speed-tier labels or Net Profit");
  }

  console.log("Proof: Custom projects native pipeline remains");
  await page.locator('.primary-nav a[href="/custom-projects"]').click();
  await page.locator('[data-automation-workflows="loaded"]').waitFor({ timeout: 40000 });
  await page.locator(`[data-automation-project="${PROJECT}"]`).waitFor({ timeout: 20000 });
  await page.goto(
    `${baseUrl}/custom-projects?project=${encodeURIComponent(PROJECT)}&tab=settings&task=1&section=WhatToBuild`,
    { waitUntil: "domcontentloaded" },
  );
  await page.locator(`[data-automation-project-detail="${PROJECT}"]`).waitFor({ timeout: 60000 });
  await page.locator('[data-settings-tag="WhatToBuild"]').waitFor({ timeout: 40000 });
  if (!(await page.locator("[data-automation-task-pipeline]").count())) {
    throw new Error("Custom projects lost the native task pipeline");
  }
  const addTask = page.locator(".task-add");
  if (!(await addTask.count())) {
    throw new Error("Custom projects lost the native add-task control");
  }
  if (!(await addTask.isDisabled())) {
    throw new Error("Custom projects add-task control is not fail-closed");
  }

  console.log("Proof: Retester fail-closed (missing archive)");
  await page.locator('.primary-nav a[href="/retester"]').click();
  await page.locator('[data-automation-workflows="unavailable"], [data-automation-workflows="failed"]').waitFor({ timeout: 20000 });
  const retesterText = await page.locator("[data-automation-workflows]").innerText();
  if (!/Retester/i.test(retesterText) || /invented task|What-If|Condition row/i.test(retesterText)) {
    throw new Error(`Retester did not fail closed on a missing archive: ${retesterText}`);
  }

  console.log("Proof: /explore redirects/lands on Getting started");
  await page.goto(`${baseUrl}/explore`, { waitUntil: "domcontentloaded" });
  await page.waitForFunction(() => window.location.pathname === "/home", null, { timeout: 10000 });
  const exploreText = await page.locator("body").innerText();
  if (exploreText.includes("Install them here") || exploreText.includes("data-capability-slot=\"explore.extensions\"")) {
    throw new Error("Explore URL still rendered a plugin-install product page");
  }
  const exploreNav = await page.locator(".primary-nav [data-route]").evaluateAll((nodes) => nodes.map((node) => node.getAttribute("data-route")));
  if (exploreNav.includes("/explore") || exploreNav.includes("/research") || exploreNav.includes("/automation")) {
    throw new Error(`legacy product routes remained on the rail after /explore: ${exploreNav.join("|")}`);
  }

  console.log("Proof: AlgoWizard stays unavailable without an editor wired");
  const modulePayload = await fetchJsonWithTimeout(`${baseUrl}/api/sqx-module?module=AlgoWizard`, { timeoutMs: 2000 });
  if (modulePayload.editor_wired !== false || modulePayload.status !== "unavailable") {
    throw new Error(`AlgoWizard did not stay fail-closed: ${JSON.stringify(modulePayload)}`);
  }

  console.log("SQX module rail browser proof: rail labels, Builder Full settings, Custom projects pipeline, fail-closed Retester, Explore redirect");
} finally {
  await browser.close();
}
