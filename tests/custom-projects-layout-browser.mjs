import { createHash } from "node:crypto";
import { spawn, spawnSync } from "node:child_process";
import { mkdir, mkdtemp, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

import { chromium } from "playwright";

const python = process.env.PYTHON || "python3";
const baseUrl = process.env.TRADERCOCKPIT_LAYOUT_URL || "http://127.0.0.1:42480";
const RETAINED_REFERENCE_HEAD = "958e2fe2910cbf71d51ae29e4951484a86fc4ab6";
const RETAINED_BUILDER_PROJECT_PATH = "references/strategyquant-x-144.2953/user/projects/Builder/project.cfx";
const RETAINED_BUILDER_PROJECT_GIT_BLOB_SHA1 = "6194322a7a6feab40e02d9d9ed741401749a51d1";
const RETAINED_BUILDER_PROJECT_SIZE = 47153;
const PROJECT = "RetainedBuildTask";

function commandOutput(result) {
  return `${result.stderr?.toString?.() || ""}${result.stdout?.toString?.() || ""}`.trim();
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

const fixtureRoot = await mkdtemp(join(tmpdir(), "tc-custom-projects-layout-"));
await mkdir(join(fixtureRoot, "internal/web/SQUANT"), { recursive: true });
await writeFile(join(fixtureRoot, "internal/web/SQUANT/build.dat"), "2953", "utf8");
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
    new URL(baseUrl).port || "42480",
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
  throw new Error(`Custom projects layout server did not become ready\n${serverOutput}`);
}

const catalog = await waitForServer();
server.unref();
if (!catalog.projects.some((item) => item.name === PROJECT)) {
  throw new Error(`Custom Project catalog missing ${PROJECT}: ${JSON.stringify(catalog.projects)}`);
}

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage();
page.setDefaultTimeout(30000);
page.setDefaultNavigationTimeout(30000);
try {
  await page.goto(`${baseUrl}/custom-projects`, { waitUntil: "domcontentloaded" });
  await page.locator('[data-automation-workflows="loaded"]').waitFor({ timeout: 40000 });
  await page.locator(`[data-automation-project="${PROJECT}"]`).waitFor({ timeout: 20000 });
  const list = await page.locator("[data-automation-project-board]").innerText();
  for (const needle of ["Custom projects", "[ Tasks (", "[ Engine ]", "[ Results ]", "DATABANKS:", "STRATEGIES:", "Create new project", "Open existing project"]) {
    if (!list.includes(needle)) {
      throw new Error(`Custom projects list missing official control ${needle}`);
    }
  }
  if (/DJ CFD|GOLD BREAKOUT|NQ BREAKOUT|GBPJPY/.test(list)) {
    throw new Error("Custom projects list invented personal SQX project names");
  }
  if (list.includes("Custom Project workflows")) {
    throw new Error("Custom projects list still used the purple workflow card copy");
  }
  const createDisabled = await page.locator(".button-sqx-create").isDisabled();
  if (!createDisabled) {
    throw new Error("Create new project must stay fail-closed");
  }

  await page.locator(`[data-automation-project="${PROJECT}"] [data-automation-open="${PROJECT}"]:not([data-automation-open-tab])`).click();
  await page.locator(`[data-automation-project-detail="${PROJECT}"]`).waitFor({ timeout: 40000 });
  const detail = page.locator(`[data-automation-project-detail="${PROJECT}"]`);
  if (await detail.getAttribute("data-sqx-module-mode") !== "custom") {
    throw new Error("Custom project detail must stay in custom module mode (pipeline + Back crumb)");
  }
  await detail.locator("[data-automation-back]").waitFor({ timeout: 20000 });
  await detail.locator("[data-automation-task-pipeline]").waitFor({ timeout: 20000 });
  const engine = page.locator('[data-settings-tag="Setup"] [data-settings-attribute="engine"]').first();
  const generation = page.locator('[data-settings-tag="BuildMode"] input[data-settings-attribute="generationType"]').first();
  await engine.waitFor({ timeout: 20000 });
  await generation.waitFor({ timeout: 20000 });
  const engineType = await engine.evaluate((node) => node.type);
  const generationType = await generation.evaluate((node) => node.type);
  if (!["radio", "select-one"].includes(engineType) || generationType !== "radio") {
    throw new Error("Engine must be an official SQX choice and generation type must stay a radio");
  }
  await page.locator('[data-automation-tab="settings"]').click();
  await detail.locator("[data-automation-back]").waitFor({ timeout: 20000 });
  await detail.locator("[data-automation-task-pipeline]").waitFor({ timeout: 20000 });

  await page.locator('[data-automation-tab="progress"]').click();
  await detail.locator("[data-automation-progress-stats]").waitFor({ timeout: 20000 });
  const progressText = await detail.innerText();
  for (const needle of ["Total tested", "Failed", "Passed", "Rate", "Fitness series", "Live results"]) {
    if (!progressText.includes(needle)) {
      throw new Error(`Progress tab missing producer-bound label ${needle}`);
    }
  }
  if (/Running time|Heap memory|Top Strategy|Databank Fitness/.test(progressText)) {
    throw new Error("Progress tab invented SQX duration/memory/fitness chrome");
  }
  await detail.locator("[data-automation-native-setup]").waitFor({ timeout: 20000 });

  await page.locator('[data-automation-tab="results"]').click();
  await detail.locator("[data-results-databank-toolbar]").waitFor({ timeout: 20000 });
  await detail.locator("[data-results-toolbar]").waitFor({ timeout: 20000 });
  const resultsText = await detail.innerText();
  if (!resultsText.includes("No result chosen - Double-click on result on databank to see the details")) {
    throw new Error("Results tab missing databank empty-state copy");
  }
  if (/>\s*Load\s*<|>\s*Save\s*<|>\s*Delete\s*</.test(resultsText)) {
    throw new Error("Results tab invented databank Load/Save/Delete chrome");
  }

  console.log("Custom projects layout browser proof: official list row, pipeline + Back on Progress and Full settings, native engine/generation radios, Progress/Results producer bindings");
} finally {
  await browser.close();
  process.exit(0);
}
