import { createHash } from "node:crypto";
import { spawn, spawnSync } from "node:child_process";
import { once } from "node:events";
import { mkdir, mkdtemp, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

import { chromium } from "playwright";

const python = process.env.PYTHON || "python3";
const baseUrl = process.env.TRADERCOCKPIT_NESTED_SETTINGS_URL || "http://127.0.0.1:42473";
const RETAINED_REFERENCE_HEAD = "958e2fe2910cbf71d51ae29e4951484a86fc4ab6";
const RETAINED_BUILDER_PROJECT_PATH = "references/strategyquant-x-144.2953/user/projects/Builder/project.cfx";
const RETAINED_BUILDER_PROJECT_GIT_BLOB_SHA1 = "6194322a7a6feab40e02d9d9ed741401749a51d1";
const RETAINED_BUILDER_PROJECT_SIZE = 47153;
const PROJECT = "RetainedBuildTask";

function commandOutput(result) {
  return `${result.stderr?.toString?.() || ""}${result.stdout?.toString?.() || ""}`.trim();
}

function referenceBuilderArchive() {
  const fetched = spawnSync(
    "git",
    ["fetch", "--no-tags", "--depth=1", "origin", RETAINED_REFERENCE_HEAD],
    { encoding: "utf8" },
  );
  if (fetched.status !== 0) {
    throw new Error(`could not fetch retained SQX reference commit ${RETAINED_REFERENCE_HEAD}: ${commandOutput(fetched)}`);
  }
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

const fixtureRoot = await mkdtemp(join(tmpdir(), "tc-nested-settings-"));
await mkdir(join(fixtureRoot, "internal/web/SQUANT"), { recursive: true });
await writeFile(join(fixtureRoot, "internal/web/SQUANT/build.dat"), "2953", "utf8");
await mkdir(join(fixtureRoot, "internal"), { recursive: true });
await writeFile(join(fixtureRoot, "internal/SQUANT.dat"), Buffer.from("144fixture"));
await mkdir(join(fixtureRoot, `user/projects/${PROJECT}`), { recursive: true });
await writeFile(join(fixtureRoot, `user/projects/${PROJECT}/project.cfx`), referenceBuilderArchive());
const dataRoot = join(fixtureRoot, "application-data");

const server = spawn(
  python,
  [
    "-m",
    "tradercockpit.app_server",
    "--host",
    "127.0.0.1",
    "--port",
    new URL(baseUrl).port || "42473",
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
      const response = await fetch(`${baseUrl}/api/sqx-projects`, { headers: { accept: "application/json" } });
      if (response.ok) {
        const payload = await response.json();
        if (payload?.schema === "tc.sqx-custom-projects.v1") return payload;
      }
    } catch {
      // startup race
    }
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  throw new Error(`nested Full settings server did not become ready\n${serverOutput}`);
}

function findNode(node, tag) {
  if (!node) return null;
  if (node.tag === tag) return node;
  for (const child of node.children || []) {
    const found = findNode(child, tag);
    if (found) return found;
  }
  return null;
}

const catalog = await waitForServer();
server.unref();
if (!catalog.projects.some((item) => item.name === PROJECT)) {
  throw new Error(`Custom Project catalog missing ${PROJECT}: ${JSON.stringify(catalog.projects)}`);
}

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage();
try {
  await page.goto(
    `${baseUrl}/custom-projects?project=${encodeURIComponent(PROJECT)}&tab=settings&task=1&section=Rankings`,
    { waitUntil: "domcontentloaded" },
  );
  await page.locator('[data-automation-workflows="loaded"]').waitFor({ timeout: 20000 });
  await page.locator("table.settings-condition-table").waitFor({ timeout: 20000 });
  const rankingHtml = await page.locator("form.full-settings").innerHTML();
  if (rankingHtml.includes("<select") || rankingHtml.includes("BASIC") || rankingHtml.includes("EXTENSIVE")) {
    throw new Error("Ranking pane invented selects or speed-tier labels");
  }
  if (!rankingHtml.includes("ProfitFactor")) {
    throw new Error("Ranking pane missing retained ProfitFactor condition");
  }
  const maxStrategies = page.locator('[data-settings-tag="MaxStrategies"] input[data-settings-text="1"]');
  await maxStrategies.fill("999");
  await Promise.all([
    page.waitForResponse((res) => res.url().includes("/api/sqx-project-settings") && res.ok(), { timeout: 30000 }),
    page.locator("[data-automation-save-settings]").click(),
  ]);
  await page.locator('[data-automation-workflows="loaded"]').waitFor({ timeout: 30000 });
  await page.locator("table.settings-condition-table").waitFor({ timeout: 20000 });
  const savedMax = await page.locator('[data-settings-tag="MaxStrategies"] input[data-settings-text="1"]').inputValue();
  if (savedMax !== "999") {
    throw new Error(`MaxStrategies readback was ${savedMax}, expected 999`);
  }

  await page.locator('[data-automation-section="CrossChecks"]').click();
  const wfoOpen = page.locator('.cross-check-method[data-settings-tag="WalkForwardOptimization"] a[data-automation-method="WalkForwardOptimization"]');
  await wfoOpen.waitFor({ timeout: 20000 });
  const crossHtml = await page.locator("form.full-settings").innerHTML();
  if (crossHtml.includes("BASIC") || crossHtml.includes("STANDARD") || crossHtml.includes("EXTENSIVE")) {
    throw new Error("Cross-check pane invented speed-tier labels");
  }
  if (crossHtml.includes('data-settings-tag="WhatIf"') && /data-settings-tag="WhatIf"[\s\S]{0,400}data-automation-method="WhatIf"/.test(crossHtml)) {
    throw new Error("Empty WhatIf invented an Open dialog");
  }
  await wfoOpen.click();
  await page.locator('[data-cross-check-method="WalkForwardOptimization"]').waitFor({ timeout: 20000 });
  await page.locator('[data-automation-method-pane="settings"]').waitFor();
  await page.locator('[data-automation-method-pane="filtering"]').waitFor();
  const param1 = page.locator('[data-settings-tag="Param1"] input');
  await param1.fill("25");
  await Promise.all([
    page.waitForResponse((res) => res.url().includes("/api/sqx-project-settings") && res.ok(), { timeout: 30000 }),
    page.locator("[data-automation-save-settings]").click(),
  ]);
  await page.locator('[data-automation-workflows="loaded"]').waitFor({ timeout: 30000 });
  await page.locator('[data-cross-check-method="WalkForwardOptimization"]').waitFor({ timeout: 20000 });
  const savedParam = await page.locator('[data-settings-tag="Param1"] input').inputValue();
  if (savedParam !== "25") {
    throw new Error(`Walk-Forward Param1 readback was ${savedParam}, expected 25`);
  }
  await page.locator('a[data-automation-section="CrossChecks"]').first().click();
  const hpOpen = page.locator('.cross-check-method[data-settings-tag="RetestWithHigherPrecision"] a[data-automation-method="RetestWithHigherPrecision"]');
  await hpOpen.waitFor({ timeout: 20000 });
  await hpOpen.click();
  await page.locator('[data-cross-check-method="RetestWithHigherPrecision"]').waitFor({ timeout: 20000 });
  const precision = await page.locator('[data-settings-tag="Precision"] input[data-settings-text="1"]').inputValue();
  if (precision !== "2") {
    throw new Error(`Higher Precision text was ${precision}, expected retained 2`);
  }

  const topology = await (await fetch(`${baseUrl}/api/sqx-project-topology?project=${encodeURIComponent(PROJECT)}`)).json();
  const rankings = topology.tasks[0].settings.find((node) => node.tag === "Rankings");
  const crossChecks = topology.tasks[0].settings.find((node) => node.tag === "CrossChecks");
  if (findNode(rankings, "MaxStrategies")?.text !== "999") {
    throw new Error("topology readback did not keep MaxStrategies text write");
  }
  if (findNode(findNode(crossChecks, "WalkForwardOptimization"), "Param1")?.attributes?.value !== "25") {
    throw new Error("topology readback did not keep Walk-Forward Param1 write");
  }
  console.log("nested Full settings browser proof: Ranking text and Walk-Forward Param1 saved");
} finally {
  await browser.close();
}
