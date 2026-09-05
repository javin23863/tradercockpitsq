import { createHash } from "node:crypto";
import { spawn, spawnSync } from "node:child_process";
import { mkdir, mkdtemp, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

import { chromium } from "playwright";

const python = process.env.PYTHON || "python3";
const baseUrl = process.env.TRADERCOCKPIT_DOCUMENTED_SETTINGS_URL || "http://127.0.0.1:42474";
const RETAINED_REFERENCE_HEAD = "958e2fe2910cbf71d51ae29e4951484a86fc4ab6";
const RETAINED_BUILDER_PROJECT_PATH = "references/strategyquant-x-144.2953/user/projects/Builder/project.cfx";
const RETAINED_BUILDER_PROJECT_GIT_BLOB_SHA1 = "6194322a7a6feab40e02d9d9ed741401749a51d1";
const RETAINED_BUILDER_PROJECT_SIZE = 47153;
const PROJECT = "RetainedBuildTask";
const NATIVE_ORDERS_B64 = "rO0ABXflABRTUU9yZGVyRmlsZUZvcm1hdDoxMQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEAAAACAAZBQVBMLkQAEE5ldyBTdHJhdGVneSAoMSkBAgIAAAAAAQAAAAABBAsAAABr4plUAAFGKPgAPbhR7AAAAGvimVQAPbhR7AAAAX/8K8AAQyxcKQAAAADMvrwgJP5J42h/RpGKj0aRio9GhpYAgAAAAIAAAACAAAAAgAAAAAFD1QvvQHwsPUnxETZGjqtyRq57gEnkoP9GkYqPAAAAAAAAAAABwIhZjwAAAAD/AAAAAA==";

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

function writeNativeArchive(home) {
  const script = `
from pathlib import Path
import base64
from zipfile import ZipFile
home = Path(${JSON.stringify(home)})
bank = home / "user/projects/${PROJECT}/databanks/Results"
bank.mkdir(parents=True, exist_ok=True)
orders = base64.b64decode(${JSON.stringify(NATIVE_ORDERS_B64)})
with ZipFile(bank / "Native.sqx", "w") as archive:
    archive.writestr("settings.xml", b"<Settings><RiskMoneyManagement><MoneyManagement><InitialCapital>10000</InitialCapital></MoneyManagement></RiskMoneyManagement></Settings>")
    archive.writestr("strategy_Portfolio.xml", b'<StrategyFile AppVersion="SQX Build 144.2953"><Strategy><Rule>native-sqx</Rule></Strategy></StrategyFile>')
    archive.writestr("version.txt", b"1")
    archive.writestr("orders.bin", orders)
`;
  const written = spawnSync(python, ["-c", script], { encoding: "utf8" });
  if (written.status !== 0) {
    throw new Error(`could not write native Results archive: ${commandOutput(written)}`);
  }
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

const fixtureRoot = await mkdtemp(join(tmpdir(), "tc-documented-settings-"));
await mkdir(join(fixtureRoot, "internal/web/SQUANT"), { recursive: true });
await writeFile(join(fixtureRoot, "internal/web/SQUANT/build.dat"), "2953", "utf8");
await mkdir(join(fixtureRoot, "internal"), { recursive: true });
await writeFile(join(fixtureRoot, "internal/SQUANT.dat"), Buffer.from("144fixture"));
await mkdir(join(fixtureRoot, `user/projects/${PROJECT}`), { recursive: true });
await writeFile(join(fixtureRoot, `user/projects/${PROJECT}/project.cfx`), referenceBuilderArchive());
writeNativeArchive(fixtureRoot);
const dataRoot = join(fixtureRoot, "application-data");

const server = spawn(
  python,
  [
    "-m",
    "tradercockpit.app_server",
    "--host",
    "127.0.0.1",
    "--port",
    new URL(baseUrl).port || "42474",
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
  throw new Error(`documented Full settings server did not become ready\n${serverOutput}`);
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
    `${baseUrl}/custom-projects?project=${encodeURIComponent(PROJECT)}&tab=settings&task=1&section=WhatToBuild`,
    { waitUntil: "domcontentloaded" },
  );
  await page.locator('[data-automation-workflows="loaded"]').waitFor({ timeout: 40000 });
  await page.locator('[data-settings-tag="WhatToBuild"]').waitFor({ timeout: 20000 });
  await page.locator('[data-settings-tag="BuildMode"] input[data-settings-attribute="generationType"]').waitFor({ timeout: 20000 });
  const whatHtml = await page.locator("form.full-settings").innerHTML();
  for (const group of ["Strategy type", "Trading direction / symmetry", "Build mode", "Stop loss", "Profit target"]) {
    if (!whatHtml.includes(`data-settings-group="${group}"`)) {
      throw new Error(`What to build missing documented group ${group}`);
    }
  }
  if (!whatHtml.includes("Genetic options") || !whatHtml.includes("Parts to improve") || !whatHtml.includes("Building blocks")) {
    throw new Error("Retained genetic/improve archive missing Genetic options, Parts to improve, or Building blocks tabs");
  }
  if (whatHtml.includes("BASIC") || whatHtml.includes("EXTENSIVE")) {
    throw new Error("What to build invented speed-tier labels");
  }

  await page.locator('[data-automation-section="GeneticOptions"]').click();
  await page.locator("[data-genetic-options]").waitFor({ timeout: 20000 });
  const population = await page.locator('[data-settings-tag="PopulationSize"] input[data-settings-text="1"]').inputValue();
  if (population !== "100") {
    throw new Error(`Genetic PopulationSize was ${population}, expected retained 100`);
  }

  await page.locator('[data-automation-section="Blocks"]').click();
  await page.locator('[data-settings-tag="Blocks"]').waitFor({ timeout: 20000 });
  const blockCount = await page.locator(".settings-block-row").count();
  if (blockCount < 50) {
    throw new Error(`Building blocks pane only rendered ${blockCount} rows`);
  }
  const generatedDump = await page.locator('[data-settings-tag="Blocks"] [data-settings-tag="Generated"]').count();
  if (generatedDump > 0) {
    throw new Error("Building blocks default view dumped nested Generated parameter trees");
  }
  await page.locator("[data-settings-calibrate-open]").click();
  await page.locator("[data-settings-calibrate-now]").waitFor({ timeout: 10000 });
  await page.locator("[data-settings-calibrate-close]").click();

  await page.locator('[data-automation-section="RiskMoneyManagement"]').click();
  await page.locator("[data-settings-exclusive-group]").waitFor({ timeout: 20000 });
  const mmRadios = page.locator("[data-settings-exclusive-group] input[data-settings-exclusive-use]");
  if (await mmRadios.count() < 2) {
    throw new Error("Money management missing exclusive Method use radios");
  }
  const uncheckedMm = page.locator("[data-settings-exclusive-group] input[data-settings-exclusive-use]:not(:checked)").first();
  const selectedMmPath = JSON.parse(await uncheckedMm.getAttribute("data-settings-path"));
  await Promise.all([
    page.waitForResponse((res) => res.url().includes("/api/sqx-project-settings") && res.ok(), { timeout: 30000 }),
    uncheckedMm.click(),
  ]);
  await page.locator('[data-automation-workflows="loaded"]').waitFor({ timeout: 40000 });
  const afterMm = await (await fetch(`${baseUrl}/api/sqx-project-topology?project=${encodeURIComponent(PROJECT)}`)).json();
  const mmNode = findNode(afterMm.tasks[0].settings.find((node) => node.tag === "RiskMoneyManagement"), "MoneyManagement");
  const mmMethods = (mmNode?.children || []).filter((child) => child.tag === "Method");
  const usedMm = mmMethods.filter((method) => method.attributes?.use === "true");
  if (usedMm.length !== 1) {
    throw new Error(`Money management exclusive use wrote ${usedMm.length} true methods`);
  }
  if (JSON.stringify(usedMm[0].path) !== JSON.stringify(selectedMmPath)) {
    throw new Error(`Money management use landed on ${JSON.stringify(usedMm[0].path)}, expected ${JSON.stringify(selectedMmPath)}`);
  }
  if (!mmMethods.some((method) => method.attributes?.use === "false")) {
    throw new Error("Money management exclusive use did not clear a sibling");
  }

  await page.locator('[data-automation-section="Data"]').click();
  await page.locator('[data-settings-tag="Setup"]').waitFor({ timeout: 20000 });
  const engineControl = page.locator('[data-settings-tag="Setup"] [data-settings-attribute="engine"]');
  await engineControl.waitFor({ timeout: 20000 });
  const dateTo = page.locator('[data-settings-tag="Setup"] input[data-settings-attribute="dateTo"]');
  await dateTo.fill("2022.12.31");
  await Promise.all([
    page.waitForResponse((res) => res.url().includes("/api/sqx-project-settings") && res.ok(), { timeout: 30000 }),
    page.locator("[data-automation-save-settings]").click(),
  ]);
  await page.locator('[data-automation-workflows="loaded"]').waitFor({ timeout: 40000 });
  await page.locator('[data-settings-tag="Setup"]').waitFor({ timeout: 20000 });
  const savedDate = await page.locator('[data-settings-tag="Setup"] input[data-settings-attribute="dateTo"]').inputValue();
  if (savedDate !== "2022.12.31") {
    throw new Error(`Data dateTo readback was ${savedDate}, expected 2022.12.31`);
  }

  await page.locator('[data-automation-section="WhatToBuild"]').click();
  await page.locator('[data-settings-tag="WhatToBuild"]').waitFor({ timeout: 40000 });
  await page.locator('[data-settings-tag="StrategyType"]').waitFor({ timeout: 20000 });
  const extraCharts = page.locator('[data-settings-tag="StrategyType"] input[data-settings-attribute="additionalCharts"]');
  await extraCharts.fill("1");
  await Promise.all([
    page.waitForResponse((res) => res.url().includes("/api/sqx-project-settings") && res.ok(), { timeout: 30000 }),
    page.locator("[data-automation-save-settings]").click(),
  ]);
  await page.locator('[data-automation-workflows="loaded"]').waitFor({ timeout: 40000 });
  const savedCharts = await page.locator('[data-settings-tag="StrategyType"] input[data-settings-attribute="additionalCharts"]').inputValue();
  if (savedCharts !== "1") {
    throw new Error(`What-to-build additionalCharts readback was ${savedCharts}, expected 1`);
  }

  await page.locator('[data-automation-tab="results"]').click();
  await page.locator('[data-automation-archive="Native.sqx"]').waitFor({ timeout: 20000 });
  await page.locator('[data-automation-archive="Native.sqx"]').click();
  await page.locator('[data-native-trade-ticket="1"]').waitFor({ timeout: 20000 });
  const tradesHtml = await page.locator("[data-results-trades]").innerHTML();
  if (!tradesHtml.includes("AAPL.D") || tradesHtml.includes("Net Profit")) {
    throw new Error("Results List of trades did not keep producer ticket/symbol or invented Net Profit");
  }
  await page.locator('[data-automation-result-view="equity"]').click();
  await page.locator("[data-results-equity]").waitFor({ timeout: 20000 });
  await page.locator('[data-automation-result-view="chart"]').click();
  await page.locator('[data-results-chart="unavailable"]').waitFor({ timeout: 20000 });

  const topology = await (await fetch(`${baseUrl}/api/sqx-project-topology?project=${encodeURIComponent(PROJECT)}`)).json();
  const data = topology.tasks[0].settings.find((node) => node.tag === "Data");
  const what = topology.tasks[0].settings.find((node) => node.tag === "WhatToBuild");
  if (findNode(data, "Setup")?.attributes?.dateTo !== "2022.12.31") {
    throw new Error("topology readback did not keep Data dateTo write");
  }
  if (findNode(what, "StrategyType")?.attributes?.additionalCharts !== "1") {
    throw new Error("topology readback did not keep What-to-build additionalCharts write");
  }
  const inspected = await (await fetch(
    `${baseUrl}/api/sqx-project-strategy?project=${encodeURIComponent(PROJECT)}&databank=Results&archive=Native.sqx&task=1`,
  )).json();
  if (inspected.orders?.payload?.trades?.[0]?.Ticket !== 1) {
    throw new Error("strategy inspect did not return producer ticket 1");
  }
  if (inspected.chart?.stored !== false) {
    throw new Error("strategy inspect invented stored chart data");
  }
  console.log("documented Full settings browser proof: pane groups, nested save readback, native trades/equity");
} finally {
  await browser.close();
}
