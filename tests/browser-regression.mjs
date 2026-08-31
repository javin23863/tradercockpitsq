import assert from "node:assert/strict";

const TOP_LEVEL_ROUTES = Object.freeze([
  "/home",
  "/research",
  "/explore",
  "/automation",
  "/operate",
  "/settings",
]);

const RESEARCH_ROUTES = Object.freeze([
  "/research?stage=construct&tab=idea",
  "/research?stage=construct&tab=specification",
  "/research?stage=construct&tab=build",
  "/research?stage=construct&tab=candidates",
  "/research?stage=backtest&tab=overview",
  "/research?stage=backtest&tab=trades",
  "/research?stage=backtest&tab=robustness",
  "/research?stage=backtest&tab=configuration",
  "/research?stage=proof",
]);

async function snapshot(tab) {
  return tab.playwright.evaluate(() => ({
    pathname: window.location.pathname,
    search: window.location.search,
    shell: document.querySelector("[data-product-shell]")?.getAttribute("data-product-shell") || "",
    runtimeStatus: document.querySelector("[data-product-shell]")?.getAttribute("data-runtime-status") || "",
    surfaceId: document.querySelector("[data-product-shell]")?.getAttribute("data-surface-id") || "",
    researchStageId: document.querySelector("[data-product-shell]")?.getAttribute("data-research-stage-id") || "",
    researchTabId: document.querySelector("[data-product-shell]")?.getAttribute("data-research-tab-id") || "",
    homeZones: [...document.querySelectorAll("[data-home-zone]")].map((node) => node.getAttribute("data-home-zone")),
    text: document.body.innerText,
  }));
}

async function waitForRuntimeStatus(tab) {
  for (let attempt = 0; attempt < 50; attempt += 1) {
    const state = await snapshot(tab);
    if (state.runtimeStatus === "loaded" || state.runtimeStatus === "failed") return state;
    await tab.playwright.waitForTimeout(20);
  }
  assert.fail("runtime status did not settle");
}

function locationString(state) {
  return `${state.pathname}${state.search}`;
}

export async function runBrowserRegression(tab, { baseUrl }) {
  const visited = [];

  for (const route of TOP_LEVEL_ROUTES) {
    await tab.goto(`${baseUrl}${route}`);
    const state = await waitForRuntimeStatus(tab);
    assert.equal(state.pathname, route, `pathname for ${route}`);
    assert.equal(state.shell, "tradercockpit-desktop", `product shell for ${route}`);
    assert.equal(state.runtimeStatus, "loaded", `runtime status for ${route}`);
    assert.doesNotMatch(state.text, /Apollo/i, `Apollo must not appear on ${route}`);
    assert.doesNotMatch(state.text, /PR #/i, `stale PR authority must not appear on ${route}`);
    assert.doesNotMatch(state.text, /donor/i, `donor language must not appear on ${route}`);
    visited.push(route);
  }

  await tab.goto(`${baseUrl}/home`);
  const home = await waitForRuntimeStatus(tab);
  assert.equal(home.surfaceId, "home");
  assert.equal(home.runtimeStatus, "loaded");
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
  assert.match(home.text, /TraderCockpit application/i);
  assert.match(home.text, /Application ready/i);
  assert.match(home.text, /Research backend/i);
  assert.match(home.text, /Runtime Not Configured/i);
  assert.match(home.text, /Native execution/i);
  assert.match(home.text, /Trusted Native Gateway Not Implemented/i);
  assert.match(home.text, /Live market data/i);
  assert.match(home.text, /Producer Not Configured/i);
  assert.match(home.text, /Consumer account/i);
  assert.match(home.text, /Model access/i);
  assert.match(home.text, /Extensions/i);
  assert.match(home.text, /Alpha Stack/i);
  assert.match(home.text, /Pipeline Overview/i);
  assert.match(home.text, /Signals/i);
  assert.match(home.text, /Risk/i);
  assert.match(home.text, /Performance/i);
  assert.match(home.text, /Quick Actions/i);
  assert.match(home.text, /Open Research/i);

  for (const route of RESEARCH_ROUTES) {
    await tab.goto(`${baseUrl}${route}`);
    const state = await waitForRuntimeStatus(tab);
    assert.equal(state.pathname, "/research", `Research pathname for ${route}`);
    assert.equal(state.surfaceId, "research", `Research surface for ${route}`);
    assert.equal(state.shell, "tradercockpit-desktop", `product shell for ${route}`);
    assert.equal(state.runtimeStatus, "loaded", `runtime status for ${route}`);
    assert.match(state.text, /Research/);
    visited.push(route);
  }

  await tab.goto(`${baseUrl}/home`);
  await waitForRuntimeStatus(tab);
  await tab.playwright.locator('a[href="/research"]').first().click();
  await tab.playwright.waitForTimeout(30);
  let state = await snapshot(tab);
  assert.equal(state.pathname, "/research");
  assert.equal(state.surfaceId, "research");
  assert.equal(state.researchStageId, "construct");
  assert.equal(state.researchTabId, "idea");

  await tab.playwright.locator('a[href="/research?stage=backtest&tab=overview"]').first().click();
  await tab.playwright.waitForTimeout(30);
  state = await snapshot(tab);
  assert.equal(locationString(state), "/research?stage=backtest&tab=overview");
  assert.equal(state.researchStageId, "backtest");
  assert.equal(state.researchTabId, "overview");

  await tab.playwright.locator('a[href="/research?stage=proof"]').first().click();
  await tab.playwright.waitForTimeout(30);
  state = await snapshot(tab);
  assert.equal(locationString(state), "/research?stage=proof");
  assert.equal(state.researchStageId, "proof");
  assert.equal(state.researchTabId, "");

  await tab.back();
  await tab.playwright.waitForTimeout(30);
  assert.equal(locationString(await snapshot(tab)), "/research?stage=backtest&tab=overview");
  await tab.forward();
  await tab.playwright.waitForTimeout(30);
  assert.equal(locationString(await snapshot(tab)), "/research?stage=proof");

  for (const obsoletePath of ["/strategyquant", "/construct/build", "/backtest/trades", "/proof"]) {
    await tab.goto(`${baseUrl}${obsoletePath}`);
    const obsolete = await waitForRuntimeStatus(tab);
    assert.equal(obsolete.pathname, obsoletePath);
    assert.equal(obsolete.surfaceId, "home");
    assert.match(obsolete.text, /Unknown route/i);
    assert.match(obsolete.text, /Returned to Home/i);
  }

  await tab.goto(`${baseUrl}/definitely-not-a-product-route`);
  const unknown = await waitForRuntimeStatus(tab);
  assert.equal(unknown.pathname, "/definitely-not-a-product-route");
  assert.equal(unknown.shell, "tradercockpit-desktop");
  assert.equal(unknown.surfaceId, "home");
  assert.match(unknown.text, /Unknown route/i);
  assert.match(unknown.text, /Returned to Home/i);

  return { routes: visited };
}
