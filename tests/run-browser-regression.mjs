import { createHash } from "node:crypto";
import { spawn, spawnSync } from "node:child_process";
import { once } from "node:events";
import { mkdir, mkdtemp, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

import { chromium } from "playwright";

import { runBrowserRegression } from "./browser-regression.mjs";

const baseUrl = process.env.TRADERCOCKPIT_BROWSER_BASE_URL || "http://127.0.0.1:4173";
const specificationBaseUrl = process.env.TRADERCOCKPIT_SPECIFICATION_BASE_URL || "http://127.0.0.1:4175";
const python = process.env.PYTHON || "python";

const RETAINED_REFERENCE_HEAD = "958e2fe2910cbf71d51ae29e4951484a86fc4ab6";
const RETAINED_BUILDER_PROJECT_PATH = "references/strategyquant-x-144.2953/user/projects/Builder/project.cfx";
const RETAINED_BUILDER_PROJECT_GIT_BLOB_SHA1 = "6194322a7a6feab40e02d9d9ed741401749a51d1";
const RETAINED_BUILDER_PROJECT_SIZE = 47153;

function commandOutput(result) {
  return `${result.stderr?.toString?.() || ""}${result.stdout?.toString?.() || ""}`.trim();
}

function retainedBuilderArchive() {
  const fetched = spawnSync(
    "git",
    ["fetch", "--no-tags", "--depth=1", "origin", RETAINED_REFERENCE_HEAD],
    { encoding: "utf8" },
  );
  if (fetched.status !== 0) {
    throw new Error(`could not fetch retained SQX reference commit ${RETAINED_REFERENCE_HEAD}: ${commandOutput(fetched)}`);
  }

  const resolved = spawnSync("git", ["rev-parse", "FETCH_HEAD"], { encoding: "utf8" });
  const resolvedHead = resolved.stdout?.trim() || "";
  if (resolved.status !== 0 || resolvedHead !== RETAINED_REFERENCE_HEAD) {
    throw new Error(
      `retained SQX reference identity mismatch: expected ${RETAINED_REFERENCE_HEAD}, observed ${resolvedHead || commandOutput(resolved)}`,
    );
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
  if (
    archive.length !== RETAINED_BUILDER_PROJECT_SIZE
    || gitBlobSha1 !== RETAINED_BUILDER_PROJECT_GIT_BLOB_SHA1
  ) {
    throw new Error(
      "retained SQX Builder archive identity mismatch: "
      + `expected ${RETAINED_BUILDER_PROJECT_SIZE} bytes/${RETAINED_BUILDER_PROJECT_GIT_BLOB_SHA1}, `
      + `observed ${archive.length} bytes/${gitBlobSha1}`,
    );
  }
  return archive;
}

const fixtureRoot = await mkdtemp(join(tmpdir(), "tradercockpit-sqx-acceptance-"));
try {
  const archive = retainedBuilderArchive();
  await mkdir(join(fixtureRoot, "internal/web/SQUANT"), { recursive: true });
  await writeFile(join(fixtureRoot, "internal/web/SQUANT/build.dat"), "2953", "utf8");
  await mkdir(join(fixtureRoot, "internal"), { recursive: true });
  await writeFile(join(fixtureRoot, "internal/SQUANT.dat"), Buffer.from("144fixture"));
  await mkdir(join(fixtureRoot, "user/projects/Builder"), { recursive: true });
  await writeFile(join(fixtureRoot, "user/projects/Builder/project.cfx"), archive);
} catch (error) {
  await rm(fixtureRoot, { recursive: true, force: true });
  throw error;
}

const specificationDataRoot = join(fixtureRoot, "application-data");
const specificationServer = spawn(
  python,
  [
    "-m",
    "tradercockpit.app_server",
    "--host",
    "127.0.0.1",
    "--port",
    new URL(specificationBaseUrl).port || "4175",
    "--sqx-home",
    fixtureRoot,
    "--data-root",
    specificationDataRoot,
  ],
  { stdio: ["ignore", "pipe", "pipe"] },
);
let specificationServerOutput = "";
specificationServer.stdout.on("data", (chunk) => { specificationServerOutput += chunk.toString(); });
specificationServer.stderr.on("data", (chunk) => { specificationServerOutput += chunk.toString(); });

async function waitForSpecificationServer() {
  for (let attempt = 0; attempt < 60; attempt += 1) {
    if (specificationServer.exitCode !== null) break;
    try {
      const response = await fetch(`${specificationBaseUrl}/api/sqx-builder-config`, {
        headers: { accept: "application/json" },
      });
      if (response.ok) {
        const payload = await response.json();
        if (payload?.schema === "tc.sqx-builder-config.v1") return;
      }
    } catch {
      // Server startup race; retry below.
    }
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  throw new Error(`bounded SQX Specification server did not become ready\n${specificationServerOutput}`);
}

async function stopSpecificationServer() {
  if (specificationServer.exitCode !== null) return;
  const exitPromise = once(specificationServer, "exit");
  specificationServer.kill("SIGTERM");
  const exited = await Promise.race([
    exitPromise.then(() => true),
    new Promise((resolve) => setTimeout(() => resolve(false), 3000)),
  ]);
  if (!exited && specificationServer.exitCode === null) {
    specificationServer.kill("SIGKILL");
    await Promise.race([
      once(specificationServer, "exit"),
      new Promise((resolve) => setTimeout(resolve, 1000)),
    ]);
  }
}

let browser = null;
try {
  await waitForSpecificationServer();
  browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();
  const cdp = await context.newCDPSession(page);

  const tab = {
    goto: (url) => page.goto(url, { waitUntil: "domcontentloaded" }),
    reload: () => page.reload({ waitUntil: "domcontentloaded" }),
    back: () => page.goBack({ waitUntil: "domcontentloaded" }),
    forward: () => page.goForward({ waitUntil: "domcontentloaded" }),
    playwright: {
      evaluate: (fn) => page.evaluate(fn),
      waitForTimeout: (ms) => page.waitForTimeout(ms),
      locator: (selector) => page.locator(selector),
    },
    capabilities: {
      get: async (name) => {
        if (name !== "cdp") throw new Error(`Unsupported browser capability: ${name}`);
        return cdp;
      },
    },
  };

  const result = await runBrowserRegression(tab, { baseUrl, specificationBaseUrl });
  console.log(`Browser regression passed: ${result.routes.length} canonical product routes`);
} finally {
  if (browser) await browser.close();
  await stopSpecificationServer();
  await rm(fixtureRoot, { recursive: true, force: true });
}
