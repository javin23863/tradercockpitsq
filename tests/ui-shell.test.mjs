import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import { renderApp } from "../web/app.mjs";
import {
  AUXILIARY_SURFACES,
  BACKTEST_TAB_IDS,
  CONSTRUCT_TAB_IDS,
  CORE_STAGE_IDS,
  PRODUCT_ROUTE_PATHS,
  resolveRoute,
} from "../web/model.mjs";


test("core research navigation is exactly Construct Backtest Proof", () => {
  assert.deepEqual(CORE_STAGE_IDS, ["construct", "backtest", "proof"]);
  assert.deepEqual(CONSTRUCT_TAB_IDS, ["idea", "specification", "build", "candidates"]);
  assert.deepEqual(BACKTEST_TAB_IDS, ["overview", "trades", "robustness", "configuration"]);
});


test("auxiliary surfaces remain outside the research stage bar", () => {
  assert.deepEqual(
    AUXILIARY_SURFACES.map((surface) => surface.id),
    ["home", "explore", "automation", "operate", "settings"],
  );
});


test("canonical routes resolve without legacy workspace authority", () => {
  assert.deepEqual(resolveRoute("/"), {
    kind: "redirect",
    redirectPath: "/home",
    path: "/",
  });
  assert.deepEqual(resolveRoute("/construct"), {
    kind: "redirect",
    redirectPath: "/construct/idea",
    path: "/construct",
  });
  assert.deepEqual(resolveRoute("/backtest"), {
    kind: "redirect",
    redirectPath: "/backtest/overview",
    path: "/backtest",
  });

  const proof = resolveRoute("/proof");
  assert.equal(proof.kind, "stage");
  assert.equal(proof.stageId, "proof");
  assert.equal(proof.tabId, null);

  const robustness = resolveRoute("/backtest/robustness");
  assert.equal(robustness.stageId, "backtest");
  assert.equal(robustness.tabId, "robustness");

  assert.ok(PRODUCT_ROUTE_PATHS.includes("/construct/candidates"));
  assert.ok(PRODUCT_ROUTE_PATHS.includes("/backtest/configuration"));
  assert.ok(PRODUCT_ROUTE_PATHS.includes("/settings"));
});


test("development shell renders the landed architecture and no persistent assistant", () => {
  const construct = renderApp(resolveRoute("/construct/idea"));
  assert.match(construct, /data-product-shell="construct-backtest-proof"/);
  assert.match(construct, />Construct</);
  assert.match(construct, />Backtest</);
  assert.match(construct, />Proof</);
  assert.match(construct, />Idea</);
  assert.match(construct, />Specification</);
  assert.match(construct, />Build</);
  assert.match(construct, />Candidates</);
  assert.doesNotMatch(construct, /Apollo/i);
  assert.doesNotMatch(construct, /Test &amp; Validate/i);

  const proof = renderApp(resolveRoute("/proof"));
  assert.match(proof, /Evidence chain/);
  assert.match(proof, /Native \.sqx strategy/);

  const settings = renderApp(resolveRoute("/settings"));
  assert.match(settings, /Consumer account\/OpenRouter slice will be rebuilt after consolidation/);
});


test("source tree no longer carries Apollo or old workspace authority in the canonical shell", async () => {
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
  assert.doesNotMatch(modelSource, /PRIMARY_WORKSPACES/);
  assert.doesNotMatch(modelSource, /Test & Validate/);
  assert.doesNotMatch(indexSource, /run-read\.mjs/);
  assert.doesNotMatch(indexSource, /sqx-presets\.mjs/);
  assert.doesNotMatch(indexSource, /sqx-outputs\.mjs/);
  assert.match(indexSource, /src="\/app\.mjs"/);
});
