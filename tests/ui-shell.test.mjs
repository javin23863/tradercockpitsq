import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import { renderApp } from "../web/app.mjs";
import {
  APP_SURFACES,
  BACKTEST_TAB_IDS,
  CONSTRUCT_TAB_IDS,
  HOME_ZONE_IDS,
  PRODUCT_ROUTE_PATHS,
  RESEARCH_STAGE_IDS,
  resolveRoute,
  strategyQuantPath,
} from "../web/model.mjs";


test("top-level desktop navigation separates Home from StrategyQuant X", () => {
  assert.deepEqual(
    APP_SURFACES.map((surface) => surface.id),
    ["home", "strategyquant", "explore", "automation", "operate", "settings"],
  );
  assert.deepEqual(PRODUCT_ROUTE_PATHS, [
    "/home",
    "/strategyquant",
    "/explore",
    "/automation",
    "/operate",
    "/settings",
  ]);
});


test("SQX historical research stages stay internal to one StrategyQuant X surface", () => {
  assert.deepEqual(RESEARCH_STAGE_IDS, ["construct", "backtest", "proof"]);
  assert.deepEqual(CONSTRUCT_TAB_IDS, ["idea", "specification", "build", "candidates"]);
  assert.deepEqual(BACKTEST_TAB_IDS, ["overview", "trades", "robustness", "configuration"]);

  const idea = resolveRoute("/strategyquant", "?stage=construct&tab=idea");
  assert.equal(idea.kind, "strategyquant");
  assert.equal(idea.surfaceId, "strategyquant");
  assert.equal(idea.researchStageId, "construct");
  assert.equal(idea.researchTabId, "idea");

  const robustness = resolveRoute("/strategyquant", "?stage=backtest&tab=robustness");
  assert.equal(robustness.researchStageId, "backtest");
  assert.equal(robustness.researchTabId, "robustness");

  const proof = resolveRoute("/strategyquant", "?stage=proof");
  assert.equal(proof.researchStageId, "proof");
  assert.equal(proof.researchTabId, null);
});


test("older research paths redirect into the single StrategyQuant X screen", () => {
  assert.deepEqual(resolveRoute("/"), {
    kind: "redirect",
    redirectPath: "/home",
    path: "/",
  });
  assert.equal(
    resolveRoute("/construct/build").redirectPath,
    strategyQuantPath("construct", "build"),
  );
  assert.equal(
    resolveRoute("/backtest/trades").redirectPath,
    strategyQuantPath("backtest", "trades"),
  );
  assert.equal(resolveRoute("/proof").redirectPath, strategyQuantPath("proof"));
});


test("Cockpit Home preserves all eight accepted live and operational zones", () => {
  assert.deepEqual(HOME_ZONE_IDS, [
    "market-overview",
    "system-status",
    "alpha-stack",
    "pipeline-overview",
    "signals",
    "risk",
    "performance",
    "quick-actions",
  ]);

  const home = renderApp(resolveRoute("/home"));
  assert.match(home, /Cockpit Home/);
  assert.match(home, /TRADERCOCKPIT \/ LIVE ORIENTATION/);
  for (const zone of HOME_ZONE_IDS) {
    assert.match(home, new RegExp(`data-home-zone="${zone}"`));
  }
  assert.match(home, /Market Overview/);
  assert.match(home, /System Status/);
  assert.match(home, /Alpha Stack/);
  assert.match(home, /Pipeline Overview/);
  assert.match(home, /Signals/);
  assert.match(home, /Risk/);
  assert.match(home, /Performance/);
  assert.match(home, /Quick Actions/);
  assert.doesNotMatch(home, />Construct</);
  assert.doesNotMatch(home, />Backtest</);
  assert.doesNotMatch(home, />Proof</);
});


test("StrategyQuant X renders the historical research workflow inside its own screen", () => {
  const sqx = renderApp(resolveRoute("/strategyquant", "?stage=construct&tab=idea"));
  assert.match(sqx, /data-surface-id="strategyquant"/);
  assert.match(sqx, /data-research-stage-id="construct"/);
  assert.match(sqx, /data-research-tab-id="idea"/);
  assert.match(sqx, />Construct</);
  assert.match(sqx, />Backtest</);
  assert.match(sqx, />Proof</);
  assert.match(sqx, />Idea</);
  assert.match(sqx, />Specification</);
  assert.match(sqx, />Build</);
  assert.match(sqx, />Candidates</);
  assert.match(sqx, /Historical research/);
  assert.doesNotMatch(sqx, /Apollo/i);
});


test("canonical shell source contains no persistent Apollo product spine", async () => {
  const [appSource, modelSource, stylesSource, indexSource] = await Promise.all([
    readFile(new URL("../web/app.mjs", import.meta.url), "utf8"),
    readFile(new URL("../web/model.mjs", import.meta.url), "utf8"),
    readFile(new URL("../web/styles.css", import.meta.url), "utf8"),
    readFile(new URL("../web/index.html", import.meta.url), "utf8"),
  ]);

  for (const source of [appSource, modelSource, stylesSource]) {
    assert.doesNotMatch(source, /APOLLO_SURFACE_ID/);
    assert.doesNotMatch(source, /apollo-persistent/);
    assert.doesNotMatch(source, /apollo-dock/);
  }
  assert.doesNotMatch(indexSource, /run-read\.mjs/);
  assert.doesNotMatch(indexSource, /sqx-presets\.mjs/);
  assert.doesNotMatch(indexSource, /sqx-outputs\.mjs/);
  assert.match(indexSource, /src="\/app\.mjs"/);
});
