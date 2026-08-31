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
} from "../web/model.mjs";


test("top-level desktop navigation separates Home from Research", () => {
  assert.deepEqual(
    APP_SURFACES.map((surface) => surface.id),
    ["home", "research", "explore", "automation", "operate", "settings"],
  );
  assert.deepEqual(PRODUCT_ROUTE_PATHS, [
    "/home",
    "/research",
    "/explore",
    "/automation",
    "/operate",
    "/settings",
  ]);
  assert.equal(APP_SURFACES.find((surface) => surface.id === "research")?.label, "Research");
});


test("historical research stages stay internal to one Research surface", () => {
  assert.deepEqual(RESEARCH_STAGE_IDS, ["construct", "backtest", "proof"]);
  assert.deepEqual(CONSTRUCT_TAB_IDS, ["idea", "specification", "build", "candidates"]);
  assert.deepEqual(BACKTEST_TAB_IDS, ["overview", "trades", "robustness", "configuration"]);

  const idea = resolveRoute("/research", "?stage=construct&tab=idea");
  assert.equal(idea.kind, "research");
  assert.equal(idea.surfaceId, "research");
  assert.equal(idea.label, "Research");
  assert.equal(idea.researchStageId, "construct");
  assert.equal(idea.researchTabId, "idea");

  const robustness = resolveRoute("/research", "?stage=backtest&tab=robustness");
  assert.equal(robustness.researchStageId, "backtest");
  assert.equal(robustness.researchTabId, "robustness");

  const proof = resolveRoute("/research", "?stage=proof");
  assert.equal(proof.researchStageId, "proof");
  assert.equal(proof.researchTabId, null);
});


test("only canonical product routes have product authority", () => {
  assert.deepEqual(resolveRoute("/"), {
    kind: "redirect",
    redirectPath: "/home",
    path: "/",
  });

  for (const legacyPath of ["/strategyquant", "/construct/build", "/backtest/trades", "/proof"]) {
    const route = resolveRoute(legacyPath);
    assert.equal(route.surfaceId, "home");
    assert.equal(route.unknownPath, legacyPath);
    assert.equal(route.kind, "surface");
  }
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
  assert.match(home, /Open Research/);
  assert.doesNotMatch(home, />StrategyQuant X</);
  assert.doesNotMatch(home, />Construct</);
  assert.doesNotMatch(home, />Backtest</);
  assert.doesNotMatch(home, />Proof</);
});


test("Research renders the historical workflow inside its own platform workspace", () => {
  const research = renderApp(resolveRoute("/research", "?stage=construct&tab=idea"));
  assert.match(research, /data-surface-id="research"/);
  assert.match(research, /data-research-stage-id="construct"/);
  assert.match(research, /data-research-tab-id="idea"/);
  assert.match(research, />Research</);
  assert.match(research, />Construct</);
  assert.match(research, />Backtest</);
  assert.match(research, />Proof</);
  assert.match(research, />Idea</);
  assert.match(research, />Specification</);
  assert.match(research, />Build</);
  assert.match(research, />Candidates</);
  assert.match(research, /Historical research/);
  assert.doesNotMatch(research, />StrategyQuant X</);
  assert.doesNotMatch(research, /Apollo/i);
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
