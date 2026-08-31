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
    specificationRequirements: [...document.querySelectorAll("[data-specification-requirement]")].map((node) => node.getAttribute("data-specification-requirement")),
    buildWorkspace: document.querySelectorAll("[data-research-build-workspace]").length,
    buildApprovalState: document.querySelector("[data-build-approval-state]")?.getAttribute("data-build-approval-state") || "",
    buildLaunchGate: document.querySelector("[data-build-launch-gate]")?.getAttribute("data-build-launch-gate") || "",
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

async function waitForSpecificationBinding(tab) {
  for (let attempt = 0; attempt < 100; attempt += 1) {
    const state = await snapshot(tab);
    if (state.specificationRequirements.length > 0 && /Build locked/i.test(state.text)) return state;
    await tab.playwright.waitForTimeout(25);
  }
  assert.fail("Research Specification did not bind a successful backend requirement model");
}

async function waitForBuildWorkspace(tab, expectedApprovalState = "") {
  for (let attempt = 0; attempt < 120; attempt += 1) {
    const state = await snapshot(tab);
    if (
      state.buildWorkspace === 1
      && (!expectedApprovalState || state.buildApprovalState === expectedApprovalState)
    ) return state;
    await tab.playwright.waitForTimeout(25);
  }
  assert.fail(`Research Build workspace did not settle${expectedApprovalState ? ` to ${expectedApprovalState}` : ""}`);
}

async function waitForIdeaEditor(tab) {
  for (let attempt = 0; attempt < 100; attempt += 1) {
    const save = tab.playwright.locator('[data-idea-action="save"]');
    if (await save.count()) {
      const disabled = await save.isDisabled();
      if (!disabled) return;
    }
    await tab.playwright.waitForTimeout(25);
  }
  assert.fail("Research Idea editor did not become ready");
}

async function waitForIdeaSelection(tab) {
  for (let attempt = 0; attempt < 100; attempt += 1) {
    if (await tab.playwright.locator('[data-idea-action="select"]').count()) return;
    await tab.playwright.waitForTimeout(25);
  }
  assert.fail("saved Research Idea did not appear in catalog");
}

async function waitForIdeaText(tab, expected) {
  for (let attempt = 0; attempt < 100; attempt += 1) {
    const editor = tab.playwright.locator("#idea-draft");
    if (await editor.count()) {
      const value = await editor.inputValue();
      if (value === expected) return;
    }
    await tab.playwright.waitForTimeout(25);
  }
  assert.fail(`Research Idea text did not settle to ${expected}`);
}

async function currentIdeaRevision(tab) {
  const codes = await tab.playwright.locator("[data-idea-current-identity] code").allTextContents();
  assert.ok(codes.length >= 2, "selected Idea exposes entity and revision identity");
  return codes[1];
}

async function currentBuildRevision(tab) {
  const codes = await tab.playwright.locator("[data-build-selected-configuration] .stat-row code").allTextContents();
  assert.ok(codes.length >= 2, "selected Build configuration exposes entity and revision identity");
  return codes[1];
}

function locationString(state) {
  return `${state.pathname}${state.search}`;
}

export async function runBrowserRegression(tab, { baseUrl, specificationBaseUrl = baseUrl }) {
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
  assert.match(home.text, /Research custody/i);
  assert.match(home.text, /Ready/i);
  assert.match(home.text, /Native execution/i);
  assert.match(home.text, /Disabled · Runtime Not Configured/i);
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
    const routeBaseUrl = (
      route === "/research?stage=construct&tab=specification"
      || route === "/research?stage=construct&tab=build"
    ) ? specificationBaseUrl : baseUrl;
    await tab.goto(`${routeBaseUrl}${route}`);
    let state = await waitForRuntimeStatus(tab);
    assert.equal(state.pathname, "/research", `Research pathname for ${route}`);
    assert.equal(state.surfaceId, "research", `Research surface for ${route}`);
    assert.equal(state.shell, "tradercockpit-desktop", `product shell for ${route}`);
    assert.equal(state.runtimeStatus, "loaded", `runtime status for ${route}`);
    assert.match(state.text, /Research/);
    if (route === "/research?stage=construct&tab=specification") {
      state = await waitForSpecificationBinding(tab);
      assert.equal(state.researchStageId, "construct");
      assert.equal(state.researchTabId, "specification");
      assert.doesNotMatch(state.text, /Pending backend mapping/i);
      assert.doesNotMatch(state.text, /Native Specification unavailable/i);
      assert.match(state.text, /Build locked/i);
      assert.ok(state.specificationRequirements.includes("source_provenance"));
      assert.ok(state.specificationRequirements.includes("historical_backtest"));
    }
    if (route === "/research?stage=construct&tab=build") {
      state = await waitForBuildWorkspace(tab);
      assert.equal(state.researchStageId, "construct");
      assert.equal(state.researchTabId, "build");
      assert.match(state.text, /Compiled snapshots/i);
      assert.match(state.text, /No compiled configurations yet/i);
      assert.doesNotMatch(state.text, /Native construct compiler not implemented/i);
    }
    visited.push(route);
  }

  await tab.goto(`${specificationBaseUrl}/research?stage=construct&tab=build`);
  await waitForRuntimeStatus(tab);
  await waitForBuildWorkspace(tab);
  await tab.playwright.locator('[data-build-action="compile"]').click();
  let buildState = await waitForBuildWorkspace(tab, "compiled");
  assert.equal(buildState.buildLaunchGate, "disabled");
  assert.match(buildState.text, /exact_native_builder_task_snapshot/i);
  assert.match(buildState.text, /Byte identical/i);
  assert.match(buildState.text, /Source project SHA-256/i);
  assert.match(buildState.text, /Executable XML SHA-256/i);
  assert.match(buildState.text, /native_launch_not_in_this_slice/i);
  assert.equal(await tab.playwright.locator('[data-build-launch-disabled]').isDisabled(), true);
  assert.equal(await tab.playwright.locator('[data-build-action="launch"]').count(), 0);
  const compiledRevision = await currentBuildRevision(tab);
  assert.match(compiledRevision, /^tc-research-revision:configuration:sha256:/);

  await tab.playwright.locator('[data-build-action="approve"]').click();
  buildState = await waitForBuildWorkspace(tab, "approved");
  const approvedRevision = await currentBuildRevision(tab);
  assert.notEqual(approvedRevision, compiledRevision, "approval creates a new immutable configuration revision");
  assert.match(buildState.text, /Approved exact revision/i);
  assert.equal(buildState.buildLaunchGate, "disabled");
  assert.equal(await tab.playwright.locator('[data-build-launch-disabled]').isDisabled(), true);

  await tab.reload();
  await waitForRuntimeStatus(tab);
  buildState = await waitForBuildWorkspace(tab, "approved");
  assert.equal(await currentBuildRevision(tab), approvedRevision, "reload recovers the exact approved configuration revision");
  assert.match(buildState.text, /Approved exact revision/i);
  assert.equal(buildState.buildLaunchGate, "disabled");

  await tab.goto(`${baseUrl}/research?stage=construct&tab=idea`);
  await waitForRuntimeStatus(tab);
  await waitForIdeaEditor(tab);
  assert.match((await snapshot(tab)).text, /No saved Ideas yet/i);
  await tab.playwright.locator("#idea-draft").fill("Browser persisted opening-range idea");
  await tab.playwright.locator("#idea-source").fill("Browser acceptance source");
  await tab.playwright.locator('[data-idea-action="save"]').click();
  await waitForIdeaText(tab, "Browser persisted opening-range idea");
  let firstRevision = "";
  for (let attempt = 0; attempt < 100; attempt += 1) {
    if (await tab.playwright.locator("[data-idea-current-identity]").count()) {
      firstRevision = await currentIdeaRevision(tab);
      break;
    }
    await tab.playwright.waitForTimeout(25);
  }
  assert.match(firstRevision, /^tc-research-revision:idea:sha256:/);
  assert.match((await snapshot(tab)).text, /Saved exact Idea revision/i);

  await tab.playwright.locator("#idea-draft").fill("Browser persisted opening-range idea — revision two");
  await tab.playwright.locator('[data-idea-action="save"]').click();
  await waitForIdeaText(tab, "Browser persisted opening-range idea — revision two");
  let secondRevision = firstRevision;
  for (let attempt = 0; attempt < 100; attempt += 1) {
    secondRevision = await currentIdeaRevision(tab);
    if (secondRevision !== firstRevision) break;
    await tab.playwright.waitForTimeout(25);
  }
  assert.notEqual(secondRevision, firstRevision, "saving a successor changes exact Idea revision identity");

  await tab.reload();
  await waitForRuntimeStatus(tab);
  await waitForIdeaSelection(tab);
  await tab.playwright.locator('[data-idea-action="select"]').first().click();
  await waitForIdeaText(tab, "Browser persisted opening-range idea — revision two");
  assert.equal(await currentIdeaRevision(tab), secondRevision);
  assert.equal(await tab.playwright.locator("#idea-source").inputValue(), "Browser acceptance source");
  assert.match((await snapshot(tab)).text, /Current revision/i);

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
