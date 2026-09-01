import { createHash } from "node:crypto";
import { spawn, spawnSync } from "node:child_process";
import { once } from "node:events";
import { mkdir, mkdtemp, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

import { chromium } from "playwright";

import { runBrowserRegression } from "./browser-regression.mjs";
import { runResearchVerticalBrowserReview } from "./research-vertical-browser-regression.mjs";

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

function referenceBuilderArchive() {
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

function nonreferenceBuilderArchiveVariant() {
  // Structural fixture coverage only: mutate one ordinary native setting in the
  // archived project so browser acceptance proves byte identity is not an allowlist.
  // This does not claim SQX saved or accepted these bytes; the installed-SQX
  // save/load/launch chain remains a separate native-producer acceptance gate.
  const source = referenceBuilderArchive();
  const script = String.raw`
import io
import sys
from xml.etree import ElementTree
from zipfile import ZipFile

source = sys.stdin.buffer.read()
output = io.BytesIO()
with ZipFile(io.BytesIO(source)) as original, ZipFile(output, "w") as changed:
    for info in original.infolist():
        payload = original.read(info)
        if info.filename == "Build-Task1.xml":
            root = ElementTree.fromstring(payload)
            values = [item for item in root.iter() if item.tag.rsplit("}", 1)[-1] == "MaxStrategies"]
            if len(values) != 1 or not (values[0].text or "").strip().isdigit():
                raise SystemExit("reference Builder project has no single numeric MaxStrategies setting")
            values[0].text = str(int(values[0].text.strip()) + 1)
            payload = ElementTree.tostring(root, encoding="utf-8", xml_declaration=True)
        changed.writestr(info, payload)
sys.stdout.buffer.write(output.getvalue())
`;
  const changed = spawnSync(python, ["-c", script], {
    input: source,
    maxBuffer: 2 * 1024 * 1024,
  });
  if (changed.status !== 0 || !Buffer.isBuffer(changed.stdout) || changed.stdout.length === 0) {
    throw new Error(`could not create non-reference Builder fixture variant: ${commandOutput(changed)}`);
  }
  const archive = changed.stdout;
  const gitBlobSha1 = createHash("sha1")
    .update(Buffer.from(`blob ${archive.length}\0`, "ascii"))
    .update(archive)
    .digest("hex");
  if (Buffer.compare(archive, source) === 0 || gitBlobSha1 === RETAINED_BUILDER_PROJECT_GIT_BLOB_SHA1) {
    throw new Error("non-reference Builder fixture unexpectedly retained the archived byte identity");
  }
  return archive;
}

const fixtureRoot = await mkdtemp(join(tmpdir(), "tradercockpit-sqx-acceptance-"));
try {
  const archive = nonreferenceBuilderArchiveVariant();
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
  const canonicalPage = await context.newPage();
  const specificationPage = await context.newPage();
  const canonicalCdp = await context.newCDPSession(canonicalPage);
  const specificationCdp = await context.newCDPSession(specificationPage);
  const canonicalOrigin = new URL(baseUrl).origin;
  const specificationOrigin = new URL(specificationBaseUrl).origin;
  let activePage = canonicalPage;
  let activeCdp = canonicalCdp;

  function activateForUrl(url) {
    const origin = new URL(url).origin;
    if (origin === specificationOrigin) {
      activePage = specificationPage;
      activeCdp = specificationCdp;
      return;
    }
    if (origin === canonicalOrigin) {
      activePage = canonicalPage;
      activeCdp = canonicalCdp;
      return;
    }
    throw new Error(`browser regression refuses unexpected origin: ${origin}`);
  }

  // Navigation only establishes that Chromium committed the requested document.
  // Each regression then waits for and asserts the exact canonical shell/read-model
  // state it requires; DOMContentLoaded itself is not product acceptance evidence.
  const tab = {
    goto: (url) => {
      activateForUrl(url);
      return activePage.goto(url, { waitUntil: "commit" });
    },
    reload: () => activePage.reload({ waitUntil: "commit" }),
    back: () => activePage.goBack({ waitUntil: "commit" }),
    forward: () => activePage.goForward({ waitUntil: "commit" }),
    playwright: {
      evaluate: (fn) => activePage.evaluate(fn),
      waitForTimeout: (ms) => activePage.waitForTimeout(ms),
      locator: (selector) => activePage.locator(selector),
    },
    capabilities: {
      get: async (name) => {
        if (name !== "cdp") throw new Error(`Unsupported browser capability: ${name}`);
        return activeCdp;
      },
    },
  };

  const result = await runBrowserRegression(tab, { baseUrl, specificationBaseUrl });
  console.log(`Browser regression passed: ${result.routes.length} canonical product routes`);
  const research = await runResearchVerticalBrowserReview(tab, { baseUrl, specificationBaseUrl });
  console.log(
    `Research vertical review passed: ${research.routes.length} routes, `
    + `${research.mappedCapabilities} mapped capabilities, `
    + `${research.explicitlyUnavailableCapabilities} explicit producer boundaries`,
  );
} catch (error) {
  const output = specificationServerOutput.trim();
  if (output) {
    console.error(`Bounded SQX Specification server output at browser failure:\n${output}`);
  }
  throw error;
} finally {
  if (browser) await browser.close();
  await stopSpecificationServer();
  await rm(fixtureRoot, { recursive: true, force: true });
}
