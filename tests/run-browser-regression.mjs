import { spawn, spawnSync } from "node:child_process";
import { once } from "node:events";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

import { chromium } from "playwright";

import { runBrowserRegression } from "./browser-regression.mjs";

const baseUrl = process.env.TRADERCOCKPIT_BROWSER_BASE_URL || "http://127.0.0.1:4173";
const specificationBaseUrl = process.env.TRADERCOCKPIT_SPECIFICATION_BASE_URL || "http://127.0.0.1:4175";
const python = process.env.PYTHON || "python";

const fixtureRoot = await mkdtemp(join(tmpdir(), "tradercockpit-sqx-acceptance-"));
const fixtureScript = String.raw`
from pathlib import Path
from zipfile import ZipFile
import sys

root = Path(sys.argv[1])
(root / "internal/web/SQUANT").mkdir(parents=True)
(root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
(root / "internal/SQUANT.dat").write_bytes(b"144fixture")
project = root / "user/projects/Builder/project.cfx"
project.parent.mkdir(parents=True)
with ZipFile(project, "w") as archive:
    archive.writestr(
        "config.xml",
        '<Project><Chart symbol="EURUSD_M1_dukas" timeframe="M30"/><InstrumentInfo instrument="EURUSD_dukascopy"/></Project>',
    )
    archive.writestr(
        "Build-Task1.xml",
        '''<Task>
          <WhatToBuild>
            <StrategyType type="simple"/>
            <MarketSides type="both"/>
            <BuildMode generationType="random-generation"/>
          </WhatToBuild>
          <Data><Setups><Setup dateFrom="2020.01.01" dateTo="2024.01.01" testPrecision="2" engine="0" slippage="1" minDist="0">
            <Chart symbol="EURUSD_M1_dukas" timeframe="M30" spread="2"/>
            <Commissions><Method use="true"/></Commissions>
          </Setup></Setups></Data>
          <Options><BuildTradingOptions><Option/></BuildTradingOptions></Options>
          <Blocks><BuildingBlocks/><OrderTypes/><ExitTypes/><CustomData/></Blocks>
          <RiskMoneyManagement><MoneyManagement><Method type="FixedSize" use="true"/><InitialCapital>10000</InitialCapital></MoneyManagement></RiskMoneyManagement>
          <Rankings><MaxStrategies>500</MaxStrategies><StopCondition type="passed-count" passedStrategies="10"/></Rankings>
          <CrossChecks use="true"/>
          <InstrumentInfo instrument="EURUSD_dukascopy"/>
        </Task>''',
    )
`;
const fixtureResult = spawnSync(python, ["-c", fixtureScript, fixtureRoot], { encoding: "utf8" });
if (fixtureResult.status !== 0) {
  throw new Error(`could not create bounded SQX browser fixture: ${fixtureResult.stderr || fixtureResult.stdout}`);
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
  if (specificationServer.exitCode === null) {
    specificationServer.kill("SIGTERM");
    await Promise.race([
      once(specificationServer, "exit"),
      new Promise((resolve) => setTimeout(resolve, 3000)),
    ]);
  }
  if (specificationServer.exitCode === null) specificationServer.kill("SIGKILL");
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
