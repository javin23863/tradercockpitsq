import { createHash } from "node:crypto";
import { spawn, spawnSync } from "node:child_process";
import { chmod, mkdir, mkdtemp, readFile, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

import { chromium } from "playwright";

const python = process.env.PYTHON || "python3";
const baseUrl = process.env.TRADERCOCKPIT_LAUNCH_URL || "http://127.0.0.1:42478";
const RETAINED_REFERENCE_HEAD = "958e2fe2910cbf71d51ae29e4951484a86fc4ab6";
const RETAINED_BUILDER_PROJECT_PATH = "references/strategyquant-x-144.2953/user/projects/Builder/project.cfx";
const RETAINED_BUILDER_PROJECT_GIT_BLOB_SHA1 = "6194322a7a6feab40e02d9d9ed741401749a51d1";
const RETAINED_BUILDER_PROJECT_SIZE = 47153;
const PROJECT = "RetainedBuildTask";

function commandOutput(result) {
  return `${result.stderr?.toString?.() || ""}${result.stdout?.toString?.() || ""}`.trim();
}

async function fetchJsonWithTimeout(url, { fetchImpl = fetch, timeoutMs = 5000, method = "GET", body } = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetchImpl(url, {
      method,
      headers: { accept: "application/json", ...(body ? { "content-type": "application/json" } : {}) },
      body,
      signal: controller.signal,
    });
    const payload = await response.json().catch(() => null);
    return { ok: response?.ok, status: response?.status, payload };
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

const fixtureRoot = await mkdtemp(join(tmpdir(), "tc-custom-project-launch-"));
await mkdir(join(fixtureRoot, "internal/web/SQUANT"), { recursive: true });
await writeFile(join(fixtureRoot, "internal/web/SQUANT/build.dat"), "2953", "utf8");
await writeFile(join(fixtureRoot, "internal/SQUANT.dat"), Buffer.from("144fixture"));
const archive = referenceBuilderArchive();
await mkdir(join(fixtureRoot, `user/projects/${PROJECT}`), { recursive: true });
await writeFile(join(fixtureRoot, `user/projects/${PROJECT}/project.cfx`), archive);
const launcherPath = join(fixtureRoot, "sqcli.exe");
const launcherSource = `#!/usr/bin/env python3
import sys
import time
from pathlib import Path
home = Path(__file__).resolve().parent
argv_log = home / "user" / "sqcli-argv.log"
argv_log.parent.mkdir(parents=True, exist_ok=True)
with argv_log.open("a", encoding="utf-8") as handle:
    handle.write("\\t".join(sys.argv[1:]) + "\\n")
producer = home / "log" / "sqcli.log"
producer.parent.mkdir(parents=True, exist_ok=True)
sentinel = home / "user" / "sqcli-stop"
if any(item == "action=start" for item in sys.argv):
    producer.write_text("Custom Project started\\nTask 1 running\\n", encoding="utf-8")
    while not sentinel.exists():
        time.sleep(0.1)
elif any(item == "action=stop" for item in sys.argv):
    if producer.exists():
        producer.write_text(producer.read_text(encoding="utf-8") + "Custom Project stop requested\\n", encoding="utf-8")
    sentinel.write_text("stop\\n", encoding="utf-8")
`;
await writeFile(launcherPath, launcherSource, "utf8");
await chmod(launcherPath, 0o755);
const launcherSha256 = createHash("sha256").update(await readFile(launcherPath)).digest("hex");
const dataRoot = join(fixtureRoot, "application-data");
const port = new URL(baseUrl).port || "42478";
const starter = join(fixtureRoot, "start_desktop.py");
await writeFile(
  starter,
  [
    "from pathlib import Path",
    "import time",
    "from tradercockpit.desktop import start_desktop_server",
    `start_desktop_server(web_root=Path(${JSON.stringify(join(process.cwd(), "web"))}), data_root=Path(${JSON.stringify(dataRoot)}), sqx_home=Path(${JSON.stringify(fixtureRoot)}), trusted_launcher_sha256=${JSON.stringify(launcherSha256)}, port=${Number(port)}, start_path="/custom-projects")`,
    "print('desktop-ready', flush=True)",
    "time.sleep(3600)",
  ].join("\n"),
  "utf8",
);

const server = spawn(
  python,
  [starter],
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
      const { ok, payload } = await fetchJsonWithTimeout(`${baseUrl}/api/sqx-projects`, { timeoutMs: 1000 });
      if (ok && payload?.schema === "tc.sqx-custom-projects.v1") return payload;
    } catch {
      // startup race
    }
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  throw new Error(`desktop launch server did not become ready\n${serverOutput}`);
}

const catalog = await waitForServer();
server.unref();
if (!catalog.control?.available) {
  throw new Error(`Custom Project launch was not ready on the desktop: ${JSON.stringify(catalog.control)}`);
}

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage();
page.setDefaultTimeout(30000);
page.setDefaultNavigationTimeout(30000);
try {
  console.log("Proof: Custom projects catalog shows launch-ready Start");
  await page.goto(`${baseUrl}/custom-projects`, { waitUntil: "domcontentloaded" });
  await page.locator("[data-product-shell]").waitFor({ timeout: 20000 });
  await page.locator('[data-automation-workflows="loaded"]').waitFor({ timeout: 40000 });
  await page.locator(`[data-automation-project="${PROJECT}"]`).waitFor({ timeout: 20000 });

  console.log("Proof: open Progress and start the saved Custom Project");
  await page.goto(
    `${baseUrl}/custom-projects?project=${encodeURIComponent(PROJECT)}&tab=progress`,
    { waitUntil: "domcontentloaded" },
  );
  await page.locator(`[data-automation-project-detail="${PROJECT}"]`).waitFor({ timeout: 40000 });
  await page.locator('[data-automation-control="run_project"]').click();
  await page.locator('[data-automation-progress-running="true"]').waitFor({ timeout: 20000 });
  await page.locator("[data-automation-progress-log]").waitFor({ timeout: 10000 });
  const progressText = await page.locator("[data-automation-progress-log]").innerText();
  if (!progressText.includes("Task 1 running")) {
    throw new Error(`Progress did not stream the producer log: ${progressText}`);
  }
  const argv = await readFile(join(fixtureRoot, "user/sqcli-argv.log"), "utf8");
  if (!argv.includes("action=start") || !argv.includes(`name=${PROJECT}`)) {
    throw new Error(`trusted launcher did not receive official start argv: ${argv}`);
  }
  if (argv.includes("action=loadconfig")) {
    throw new Error("Custom Project start invented a loadconfig command");
  }

  const progress = await fetchJsonWithTimeout(`${baseUrl}/api/sqx-project-progress?project=${encodeURIComponent(PROJECT)}`);
  if (!progress.ok || progress.payload.running !== true || progress.payload.generated != null) {
    throw new Error(`progress read model invented stats or was not running: ${JSON.stringify(progress.payload)}`);
  }

  console.log("Proof: Stop sends official action=stop");
  await page.locator('[data-automation-control="stop_project"]').click();
  for (let attempt = 0; attempt < 20; attempt += 1) {
    const stopArgv = await readFile(join(fixtureRoot, "user/sqcli-argv.log"), "utf8");
    if (stopArgv.includes("action=stop") && stopArgv.includes(`name=${PROJECT}`)) {
      console.log("Custom Project launch browser proof: official start/stop argv, supervisor-backed running state, producer log stream");
      break;
    }
    if (attempt === 19) {
      throw new Error("trusted launcher did not receive official stop argv");
    }
    await new Promise((resolve) => setTimeout(resolve, 150));
  }
} finally {
  await browser.close();
}
