import assert from "node:assert/strict";

const TOP_LEVEL_ROUTES = Object.freeze([
  "/home",
  "/strategyquant",
  "/explore",
  "/automation",
  "/operate",
  "/settings",
]);

const SQX_RESEARCH_ROUTES = Object.freeze([
  "/strategyquant?stage=construct&tab=idea",
  "/strategyquant?stage=construct&tab=specification",
  "/strategyquant?stage=construct&tab=build",
  "/strategyquant?stage=construct&tab=candidates",
  "/strategyquant?stage=backtest&tab=overview",
  "/strategyquant?stage=backtest&tab=trades",
  "/strategyquant?stage=backtest&tab=robustness",
  "/strategyquant?stage=backtest&tab=configuration",
  "/strategyquant?stage=proof",
]);

async function snapshot(tab) {
  return tab.playwright.evaluate(() => ({
    pathname: window.location.pathname,
    search: window.location.search,
    shell: document.querySelector("[data-product-shell]")?.getAttribute("data-product-shell") || "",
    surfaceId: document.querySelector("[data-product-shell]")?.getAttribute("data-surface-id") || "",
    researchStageId: document.querySelector("[data-product-shell]")?.getAttribute("data-research-stage-id") || "",
    researchTabId: document.querySelector("[data-product-shell]")?.getAttribute("data-research-tab-id") || "",
    homeZones: [...document.querySelectorAll("[data-home-zone]")].map((node) => node.getAttribute("data-home-zone")),
    text: document.body.innerText,
  }));
}

function locationString(state) {
  return `${state.pathname}${state.search}`;
}

export async function runBrowserRegression(tab, { baseUrl }) {
  const visited = [];

  for (const route of TOP_LEVEL_ROUTES) {
    await tab.goto(`${baseUrl}${route}`);
    const state = await snapshot(tab);
    assert.equal(state.pathname, route, `pathname for ${route}`);
    assert.equal(state.shell, "tradercockpit-desktop", `product shell for ${route}`);
    assert.doesNotMatch(state.text, /Apollo/i, `Apollo must not appear on ${route}`);
    visited.push(route);
  }

  await tab.goto(`${baseUrl}/home`);
  const home = await snapshot(tab);
  assert.equal(home.surfaceId, "home");
  assert.deepEqual(home.homeZones, [
    "market-overview",
    "system-status",
    "alpha-stack",
    "pipeline-overview",
    "signals",
    "risk",
    "performance",
    "quick-actions",
  ]);
  assert.match(home.text, /Cockpit Home/i);
  assert.match(home.text, /Market Overview/i);
  assert.match(home.text, /System Status/i);
  assert.match(home.text, /Alpha Stack/i);
  assert.match(home.text, /Pipeline Overview/i);
  assert.match(home.text, /Signals/i);
  assert.match(home.text, /Risk/i);
  assert.match(home.text, /Performance/i);
  assert.match(home.text, /Quick Actions/i);

  for (const route of SQX_RESEARCH_ROUTES) {
    await tab.goto(`${baseUrl}${route}`);
    const state = await snapshot(tab);
    assert.equal(state.pathname, "/strategyquant", `SQX screen pathname for ${route}`);
    assert.equal(state.surfaceId, "strategyquant", `SQX surface for ${route}`);
    assert.equal(state.shell, "tradercockpit-desktop", `product shell for ${route}`);
    assert.match(state.text, /StrategyQuant X/);
    visited.push(route);
  }

  await tab.goto(`${baseUrl}/home`);
  await tab.playwright.locator('a[href="/strategyquant"]').first().click();
  await tab.playwright.waitForTimeout(30);
  let state = await snapshot(tab);
  assert.equal(state.pathname, "/strategyquant");
  assert.equal(state.surfaceId, "strategyquant");
  assert.equal(state.researchStageId, "construct");
  assert.equal(state.researchTabId, "idea");

  await tab.playwright.locator('a[href="/strategyquant?stage=backtest&tab=overview"]').first().click();
  await tab.playwright.waitForTimeout(30);
  state = await snapshot(tab);
  assert.equal(locationString(state), "/strategyquant?stage=backtest&tab=overview");
  assert.equal(state.researchStageId, "backtest");
  assert.equal(state.researchTabId, "overview");

  await tab.playwright.locator('a[href="/strategyquant?stage=proof"]').first().click();
  await tab.playwright.waitForTimeout(30);
  state = await snapshot(tab);
  assert.equal(locationString(state), "/strategyquant?stage=proof");
  assert.equal(state.researchStageId, "proof");
  assert.equal(state.researchTabId, "");

  await tab.back();
  await tab.playwright.waitForTimeout(30);
  assert.equal(locationString(await snapshot(tab)), "/strategyquant?stage=backtest&tab=overview");
  await tab.forward();
  await tab.playwright.waitForTimeout(30);
  assert.equal(locationString(await snapshot(tab)), "/strategyquant?stage=proof");

  await tab.goto(`${baseUrl}/construct/build`);
  await tab.playwright.waitForTimeout(30);
  state = await snapshot(tab);
  assert.equal(locationString(state), "/strategyquant?stage=construct&tab=build");
  assert.equal(state.surfaceId, "strategyquant");

  await tab.goto(`${baseUrl}/definitely-not-a-product-route`);
  const unknown = await snapshot(tab);
  assert.equal(unknown.pathname, "/definitely-not-a-product-route");
  assert.equal(unknown.shell, "tradercockpit-desktop");
  assert.equal(unknown.surfaceId, "home");
  assert.match(unknown.text, /Unknown route/);
  assert.match(unknown.text, /Returned to Home/);

  return { routes: visited };
}
