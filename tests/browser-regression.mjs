import assert from "node:assert/strict";

const ROUTES = Object.freeze([
  "/home",
  "/construct/idea",
  "/construct/specification",
  "/construct/build",
  "/construct/candidates",
  "/backtest/overview",
  "/backtest/trades",
  "/backtest/robustness",
  "/backtest/configuration",
  "/proof",
  "/explore",
  "/automation",
  "/operate",
  "/settings",
]);

async function snapshot(tab) {
  return tab.playwright.evaluate(() => ({
    pathname: window.location.pathname,
    shell: document.querySelector("[data-product-shell]")?.getAttribute("data-product-shell") || "",
    stageId: document.querySelector("[data-product-shell]")?.getAttribute("data-stage-id") || "",
    tabId: document.querySelector("[data-product-shell]")?.getAttribute("data-tab-id") || "",
    text: document.body.innerText,
  }));
}

export async function runBrowserRegression(tab, { baseUrl }) {
  const visited = [];

  for (const route of ROUTES) {
    await tab.goto(`${baseUrl}${route}`);
    const state = await snapshot(tab);
    assert.equal(state.pathname, route, `pathname for ${route}`);
    assert.equal(state.shell, "construct-backtest-proof", `product shell for ${route}`);
    assert.doesNotMatch(state.text, /Apollo/i, `Apollo must not appear on ${route}`);
    assert.doesNotMatch(state.text, /Test & Validate/i, `old workspace label must not appear on ${route}`);
    visited.push(route);
  }

  await tab.goto(`${baseUrl}/home`);
  await tab.playwright.locator('a[href="/construct/idea"]').first().click();
  await tab.playwright.waitForTimeout(30);
  assert.equal((await snapshot(tab)).pathname, "/construct/idea");

  await tab.playwright.locator('a[href="/backtest/overview"]').first().click();
  await tab.playwright.waitForTimeout(30);
  const backtest = await snapshot(tab);
  assert.equal(backtest.pathname, "/backtest/overview");
  assert.equal(backtest.stageId, "backtest");
  assert.equal(backtest.tabId, "overview");

  await tab.playwright.locator('a[href="/proof"]').first().click();
  await tab.playwright.waitForTimeout(30);
  const proof = await snapshot(tab);
  assert.equal(proof.pathname, "/proof");
  assert.equal(proof.stageId, "proof");

  await tab.back();
  await tab.playwright.waitForTimeout(30);
  assert.equal((await snapshot(tab)).pathname, "/backtest/overview");
  await tab.forward();
  await tab.playwright.waitForTimeout(30);
  assert.equal((await snapshot(tab)).pathname, "/proof");

  await tab.goto(`${baseUrl}/definitely-not-a-product-route`);
  const unknown = await snapshot(tab);
  assert.equal(unknown.pathname, "/definitely-not-a-product-route");
  assert.equal(unknown.shell, "construct-backtest-proof");
  assert.match(unknown.text, /Unknown route/);
  assert.match(unknown.text, /Returned to Home/);

  return { routes: visited };
}
