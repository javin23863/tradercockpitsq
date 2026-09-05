import assert from "node:assert/strict";

const TOP_LEVEL_ROUTES = Object.freeze([
  "/home",
  "/builder",
  "/custom-projects",
  "/apollo",
  "/data-manager",
  "/settings",
]);
const EXPECTED_NAV = TOP_LEVEL_ROUTES;

// Prototype Research workspaces/tabs (see references/ui-authority/screenshots).
const RESEARCH_ROUTES = Object.freeze([
  "/research?workspace=signals&tab=overview",
  "/research?workspace=signals&tab=signals",
  "/research?workspace=signals&tab=order-flow",
  "/research?workspace=signals&tab=footprint",
  "/research?workspace=signals&tab=volume-profile",
  "/research?workspace=signals&tab=liquidity-map",
  "/research?workspace=signals&tab=replays",
  "/research?workspace=signals&tab=alerts",
  "/research?workspace=signals&tab=reports",
  "/research?workspace=evolution",
  "/research?workspace=validate&tab=overview",
  "/research?workspace=validate&tab=initial-test",
  "/research?workspace=validate&tab=trades",
  "/research?workspace=validate&tab=robustness",
  "/research?workspace=validate&tab=configuration",
  "/research?workspace=validate&tab=evidence",
  "/research?workspace=catalog&tab=all",
  "/research?workspace=catalog&tab=indicators",
  "/research?workspace=catalog&tab=models",
  "/research?workspace=catalog&tab=strategies",
  "/research?workspace=catalog&tab=utilities",
  "/research?workspace=catalog&tab=mine",
]);

// Routes that need the bounded SQX fixture server (native Builder configuration reads).
const NATIVE_FIXTURE_ROUTES = new Set([
  "/research?workspace=signals&tab=signals",
  "/research?workspace=evolution",
  "/research?workspace=catalog&tab=all",
  "/research?workspace=catalog&tab=indicators",
  "/research?workspace=catalog&tab=strategies",
  "/research?workspace=catalog&tab=utilities",
  "/research?workspace=catalog&tab=mine",
]);

async function snapshot(tab) {
  return tab.playwright.evaluate(() => ({
    pathname: window.location.pathname,
    search: window.location.search,
    shell: document.querySelector("[data-product-shell]")?.getAttribute("data-product-shell") || "",
    runtimeStatus: document.querySelector("[data-product-shell]")?.getAttribute("data-runtime-status") || "",
    surfaceId: document.querySelector("[data-product-shell]")?.getAttribute("data-surface-id") || "",
    workspaceId: document.querySelector("[data-product-shell]")?.getAttribute("data-workspace-id") || "",
    tabId: document.querySelector("[data-product-shell]")?.getAttribute("data-tab-id") || "",
    custodyStatus: document.querySelector("[data-product-shell]")?.getAttribute("data-custody-status") || "",
    marketStatus: document.querySelector("[data-product-shell]")?.getAttribute("data-market-status") || "",
    legacyBands: document.querySelectorAll(".topbar, .status-bar, [data-attention-count], [data-last-run-state]").length,
    contentBottom: document.querySelector(".content-scroll")?.getBoundingClientRect().bottom,
    viewportHeight: window.innerHeight,
    homeZones: [...document.querySelectorAll("[data-home-zone]")].map((node) => node.getAttribute("data-home-zone")),
    tickerSymbols: [...document.querySelectorAll("[data-quote-symbol]")].map((node) => node.getAttribute("data-quote-symbol")),
    catalogRows: document.querySelectorAll("[data-catalog-component]").length,
    validationStages: [...document.querySelectorAll("[data-validation-stage]")].map((node) => node.getAttribute("data-validation-stage")),
    evolutionMode: document.querySelector("[data-evolution-mode] .chip")?.textContent?.trim() || "",
    assistantDisabled: document.querySelector("[data-assistant-ask]")?.disabled ?? null,
    assistantReady: document.querySelector("[data-assistant-widget]")?.getAttribute("data-assistant-ready") || "",
    assistantMessages: [...document.querySelectorAll("[data-assistant-role]")].map((node) => ({ role: node.getAttribute("data-assistant-role"), error: node.hasAttribute("data-assistant-error"), text: node.textContent.trim() })),
    verdictState: document.querySelector("[data-validate-overview]")?.getAttribute("data-verdict-state") || "",
    funnelStates: [...document.querySelectorAll("[data-funnel-stage]")].map((node) => `${node.getAttribute("data-funnel-stage")}:${node.getAttribute("data-funnel-state")}`),
    funnelText: document.querySelector("[data-validate-funnel-card]")?.innerText || "",
    specificationRequirements: [...document.querySelectorAll("[data-specification-requirement]")].map((node) => node.getAttribute("data-specification-requirement")),
    buildWorkspace: document.querySelectorAll("[data-research-build-workspace]").length,
    buildApprovalState: document.querySelector("[data-build-approval-state]")?.getAttribute("data-build-approval-state") || "",
    buildLaunchGate: document.querySelector("[data-build-launch-gate]")?.getAttribute("data-build-launch-gate") || "",
    tradesWorkspace: document.querySelectorAll("[data-research-trades]").length,
    mlModelsState: document.querySelector("[data-ml-models-panel]")?.getAttribute("data-ml-models-state") || "",
    mlBackendAvailable: document.querySelector("[data-ml-models-panel]")?.getAttribute("data-backend-available") || "",
    overlayPicker: Boolean(document.querySelector("[data-chart-historical-result]")),
    overlayState: document.querySelector("[data-chart-card][data-trade-overlay-state]")?.getAttribute("data-trade-overlay-state") || "",
    tradeFills: document.querySelectorAll("[data-trade-fill]").length,
    capabilitySlots: [...document.querySelectorAll("[data-capability-registry][data-capability-slot]")].map((node) => node.getAttribute("data-capability-slot")),
    automationState: document.querySelector("[data-automation-workflows]")?.getAttribute("data-automation-workflows") || "",
    navRoutes: [...document.querySelectorAll(".primary-nav [data-route]")].map((node) => node.getAttribute("data-route")),
    text: document.body.innerText,
  }));
}

async function waitForAutomationWorkflows(tab) {
  for (let attempt = 0; attempt < 150; attempt += 1) {
    const state = await snapshot(tab);
    if (state.automationState === "loaded" || state.automationState === "failed") return state;
    await tab.playwright.waitForTimeout(20);
  }
  assert.fail("Automation workflows did not bind");
}

async function waitForRuntimeStatus(tab) {
  let lastState;
  for (let attempt = 0; attempt < 150; attempt += 1) {
    const state = await snapshot(tab);
    lastState = state;
    if (
      (state.runtimeStatus === "loaded" || state.runtimeStatus === "failed")
      && state.custodyStatus !== "loading"
      && state.marketStatus !== "loading"
    ) return state;
    await tab.playwright.waitForTimeout(20);
  }
  assert.fail(`runtime, market and custody status did not settle: ${JSON.stringify({
    pathname: lastState?.pathname, search: lastState?.search,
    runtime: lastState?.runtimeStatus, market: lastState?.marketStatus, custody: lastState?.custodyStatus,
  })}`);
}

async function waitForVerdictState(tab) {
  for (let attempt = 0; attempt < 150; attempt += 1) {
    const state = await snapshot(tab);
    if (state.verdictState && state.verdictState !== "loading") return state;
    await tab.playwright.waitForTimeout(100);
  }
  throw new Error("cockpit verdict overview never left the loading state");
}

async function waitForAssistantReply(tab, expectedCount) {
  for (let attempt = 0; attempt < 150; attempt += 1) {
    const state = await snapshot(tab);
    if (state.assistantMessages.length >= expectedCount) return state;
    await tab.playwright.waitForTimeout(100);
  }
  throw new Error("assistant reply never arrived");
}

async function waitForCatalogRows(tab) {
  for (let attempt = 0; attempt < 200; attempt += 1) {
    const state = await snapshot(tab);
    if (state.catalogRows > 0) return state;
    await tab.playwright.waitForTimeout(50);
  }
  assert.fail("Indicators & Models catalog did not bind native building blocks");
}

async function waitForModelsPanel(tab) {
  for (let attempt = 0; attempt < 100; attempt += 1) {
    const state = await snapshot(tab);
    if (state.mlModelsState === "loaded" || state.mlModelsState === "error") return state;
    await tab.playwright.waitForTimeout(25);
  }
  assert.fail("Indicators & Models catalog Models tab did not bind the platform Models catalog");
}

async function waitForEvolutionMode(tab) {
  for (let attempt = 0; attempt < 200; attempt += 1) {
    const state = await snapshot(tab);
    if (state.evolutionMode && !/Reading/i.test(state.evolutionMode)) return state;
    await tab.playwright.waitForTimeout(50);
  }
  assert.fail("Evolutionary Search did not bind the native search mode");
}

async function waitForSpecificationBinding(tab) {
  for (let attempt = 0; attempt < 200; attempt += 1) {
    const state = await snapshot(tab);
    if (
      state.specificationRequirements.length > 0
      && /Build (locked|requirements resolved)/i.test(state.text)
    ) return state;
    await tab.playwright.waitForTimeout(25);
  }
  assert.fail("Research Specification did not bind a successful backend requirement model");
}

async function waitForBuildWorkspace(tab, expectedApprovalState = "") {
  for (let attempt = 0; attempt < 200; attempt += 1) {
    const state = await snapshot(tab);
    if (
      state.buildWorkspace === 1
      && (!expectedApprovalState || state.buildApprovalState === expectedApprovalState)
      && (!expectedApprovalState || ["approval-required", "runtime-unavailable", "ready", "submitted", "failed"].includes(state.buildLaunchGate))
    ) return state;
    await tab.playwright.waitForTimeout(25);
  }
  assert.fail(`Research Build workspace did not settle${expectedApprovalState ? ` to ${expectedApprovalState}` : ""}`);
}

async function waitForTradesWorkspace(tab) {
  for (let attempt = 0; attempt < 100; attempt += 1) {
    const state = await snapshot(tab);
    if (state.tradesWorkspace === 1 && /No completed native Historical Result is available for Trades/i.test(state.text)) {
      return state;
    }
    await tab.playwright.waitForTimeout(25);
  }
  assert.fail("Research Backtest Trades did not settle to its producer-backed empty state");
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
    assert.equal(state.legacyBands, 0, `legacy global bands are absent on ${route}`);
    assert.doesNotMatch(state.text, /Live Runs|Last Run:/, `legacy status text is absent on ${route}`);
    assert.ok(Math.abs(state.contentBottom - state.viewportHeight) < 1, `content uses the former bottom band space on ${route}`);
    assert.doesNotMatch(state.text, /\$\s?\d/, `no fabricated money values on ${route}`);
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
  assert.match(home.text, /Getting started/i);
  assert.match(home.text, /Build, test,\s+review\./i);
  assert.match(home.text, /Market Overview/i);
  assert.match(home.text, /System Status/i);
  assert.match(home.text, /Alpha Stack/i);
  assert.match(home.text, /Pipeline Overview/i);
  assert.match(home.text, /Live signals not connected/i);
  assert.match(home.text, /Live risk state not connected/i);
  assert.match(home.text, /Current performance not connected/i);
  assert.match(home.text, /Quick Actions/i);
  assert.doesNotMatch(home.text, /Decisions that Compound/i);
  assert.doesNotMatch(home.text, /Recent Activity/i);
  assert.match(home.text, /TraderCockpit application/i);
  assert.match(home.text, /Research backend/i);
  assert.match(home.text, /Runtime Not Configured/i);
  assert.match(home.text, /Research custody/i);
  assert.match(home.text, /Native execution/i);
  assert.match(home.text, /Disabled · Runtime Not Configured/i);
  assert.match(home.text, /Live market data/i);
  assert.match(home.text, /Apollo TradingView tool/i);
  assert.match(home.text, /Apollo MetaTrader tool/i);
  assert.match(home.text, /Producer Not Configured/i);
  assert.match(home.text, /Consumer account/i);
  assert.match(home.text, /Model access/i);
  assert.match(home.text, /Extensions/i);
  assert.match(home.text, /Ready/);
  assert.doesNotMatch(home.text, /Manifest Not Implemented/i);
  assert.deepEqual(home.navRoutes, EXPECTED_NAV);
  assert.match(home.text, /Open Apollo/);
  assert.doesNotMatch(home.text, /assistant is not connected yet/i);
  assert.equal(home.assistantDisabled, null, "Home jumps to Apollo instead of mounting a second thread");
  assert.equal(home.assistantReady, "");
  assert.match(home.text, /Research Candidates/i);
  assert.match(home.text, /Promotion authority not connected/i);
  assert.match(home.text, /Current custody · 0/);
  assert.doesNotMatch(home.text, /A\+ Champion|B Champion|Rules OK/);
  assert.doesNotMatch(home.text, /\$\s?\d/);

  await tab.goto(`${baseUrl}/settings`);
  const coreSettings = await waitForRuntimeStatus(tab);
  assert.deepEqual(coreSettings.navRoutes, EXPECTED_NAV);
  assert.deepEqual(coreSettings.capabilitySlots, [], "optional catalog stays outside the core workflow");
  assert.doesNotMatch(coreSettings.text, /Install SQX plugins|RunCompare|Add-ons workspace|\/addons/i);
  const catalog = await tab.playwright.evaluate(async () => {
    const response = await fetch("/api/capabilities");
    return { ok: response.ok, payload: await response.json() };
  });
  assert.equal(catalog.ok, true);
  assert.equal(catalog.payload.schema, "tc.capability-addon-registry.v1");
  assert.equal(catalog.payload.status, "ready");
  assert.deepEqual(catalog.payload.surfaces, EXPECTED_NAV.map((route) => route.slice(1)));
  assert.equal(catalog.payload.addon_count, 7, "packaged native inventory remains available through the backend");

  await tab.goto(`${baseUrl}/custom-projects`);
  await waitForRuntimeStatus(tab);
  const automation = await waitForAutomationWorkflows(tab);
  assert.match(automation.text, /Custom projects|Create new project|No saved Custom Projects/i);
  assert.doesNotMatch(automation.text, /TradingView/i);
  assert.doesNotMatch(automation.text, /MetaTrader 5/i);
  assert.doesNotMatch(automation.text, /StrategyQuant X MCP card|Retained Custom Project tools/i);
  assert.doesNotMatch(automation.text, /No automation control seam yet/);
  assert.doesNotMatch(automation.text, /DJ CFD|GOLD BREAKOUT|NQ_M1_dukas/);

  await tab.goto(`${baseUrl}/operate`);
  const operate = await waitForRuntimeStatus(tab);
  assert.equal(operate.pathname, "/home");
  assert.equal(operate.surfaceId, "home");
  assert.match(operate.text, /Getting started/i);
  assert.match(operate.text, /Live risk state not connected/i);
  assert.doesNotMatch(operate.text, /Open Operate/i);
  assert.doesNotMatch(operate.text, /\$\s?\d/);

  await tab.goto(`${baseUrl}/settings`);
  const settings = await waitForRuntimeStatus(tab);
  assert.match(settings.text, /Apollo TradingView MCP/i);
  assert.match(settings.text, /Apollo MetaTrader MCP/i);
  assert.doesNotMatch(settings.text, /Retained Custom Project tools/i);
  assert.match(settings.text, /Custom Project launch/i);
  assert.match(settings.text, /TraderCockpit has no SQX MCP adapter/i);
  assert.doesNotMatch(settings.text, /Install SQX plugins|Native StrategyQuant X plugins/i);

  for (const route of RESEARCH_ROUTES) {
    const routeBaseUrl = NATIVE_FIXTURE_ROUTES.has(route) ? specificationBaseUrl : baseUrl;
    await tab.goto(`${routeBaseUrl}${route}`);
    let state = await waitForRuntimeStatus(tab);
    assert.equal(state.pathname, "/research", `Research pathname for ${route}`);
    assert.equal(state.surfaceId, "research", `Research surface for ${route}`);
    assert.equal(state.shell, "tradercockpit-desktop", `product shell for ${route}`);
    assert.equal(state.runtimeStatus, "loaded", `runtime status for ${route}`);
    assert.equal(locationString(state), route, `canonical route for ${route}`);
    assert.equal(state.legacyBands, 0, `legacy global bands are absent on ${route}`);
    assert.match(state.text, /Signals & Models[\s\S]*Evolutionary Search[\s\S]*Test & Validate[\s\S]*Indicators & Models/, `workspace switcher on ${route}`);
    assert.doesNotMatch(state.text, /\$\s?\d/, `no fabricated money values on ${route}`);
    if (route === "/research?workspace=signals&tab=signals") {
      state = await waitForSpecificationBinding(tab);
      assert.equal(state.workspaceId, "signals");
      assert.equal(state.tabId, "signals");
      assert.match(state.text, /Order Flow Signals & Models/);
      assert.match(state.text, /Strategy Panel/);
      assert.match(state.text, /Signal Pulse/);
      assert.match(state.text, /Native Strategy Specification/i);
      assert.doesNotMatch(state.text, /Native Specification unavailable/i);
      assert.match(state.text, /Build requirements resolved/i);
      assert.match(state.text, /Producer Configured/i);
      assert.doesNotMatch(state.text, /Build locked/i);
      assert.ok(state.specificationRequirements.includes("source_provenance"));
      assert.ok(state.specificationRequirements.includes("historical_backtest"));
      assert.equal(state.overlayPicker, true, "Signals chart offers a Historical Result overlay picker");
      assert.equal(state.tradeFills, 0, "fixture desktop does not invent trade fills");
      assert.match(state.overlayState, /^(idle|unavailable)$/);
      assert.match(state.text, /Historical Result/);
    }
    if (route === "/research?workspace=evolution") {
      state = await waitForBuildWorkspace(tab);
      state = await waitForEvolutionMode(tab);
      assert.equal(state.workspaceId, "evolution");
      assert.match(state.text, /Evolutionary Search/);
      assert.match(state.text, /No approved configuration|Compile and approve/i);
      assert.match(state.evolutionMode, /Unavailable/i);
      assert.match(state.text, /Compiled snapshots/i);
      assert.match(state.text, /No compiled configurations yet/i);
      assert.match(state.text, /Top Candidates/);
      assert.doesNotMatch(state.text, /Native construct compiler not implemented/i);
    }
    if (route === "/research?workspace=validate&tab=overview") {
      state = await waitForVerdictState(tab);
      assert.deepEqual(state.validationStages, ["initial-test", "fast-validation", "golden-validation", "scenario-tests", "stress-tests", "out-of-sample", "evidence"]);
      assert.match(state.text, /Validation Funnel/);
      assert.match(state.text, /Run & Evidence Table/);
      // No completed native result exists on the acceptance desktop, so the cockpit verdict is
      // truthfully empty for every stage (never fabricated, never "not connected").
      assert.equal(state.verdictState, "empty");
      assert.deepEqual(state.funnelStates, ["initial-test:empty", "fast-validation:empty", "golden-validation:empty", "scenario-tests:empty", "stress-tests:empty", "out-of-sample:empty", "evidence:empty"]);
      assert.match(state.text, /No verdict yet/);
      assert.match(state.funnelText, /the cockpit computes every stage verdict from the exact native trade records/i);
      assert.doesNotMatch(state.funnelText, /not connected/i, "funnel stages are judged by the cockpit, never 'not connected'");
      assert.doesNotMatch(state.text, /Robust & Deployable/i);
    }
    if (route === "/research?workspace=validate&tab=trades") {
      state = await waitForTradesWorkspace(tab);
      assert.equal(state.workspaceId, "validate");
      assert.equal(state.tabId, "trades");
      assert.equal(state.tradesWorkspace, 1);
      assert.doesNotMatch(state.text, /Native historical result not loaded/i);
      assert.match(state.text, /No completed native Historical Result is available for Trades/i);
    }
    if (route === "/research?workspace=catalog&tab=all") {
      state = await waitForCatalogRows(tab);
      assert.match(state.text, /components found/i);
      assert.match(state.text, /StrategyQuant X · native block/);
      assert.ok(state.catalogRows > 0 && state.catalogRows <= 20, "catalog pages native blocks");
    }
    if (route === "/research?workspace=catalog&tab=models") {
      state = await waitForModelsPanel(tab);
      assert.match(state.mlModelsState, /^(loaded|error)$/);
      assert.match(state.text, /historical_explanatory|No completed Historical Result|sklearn/i);
      assert.doesNotMatch(state.text, /Neural Net/i);
    }
    visited.push(route);
  }

  // Legacy stage/tab links canonicalise to the prototype workspaces (bookmarks keep working).
  await tab.goto(`${baseUrl}/research?stage=proof&proofEntity=tc-research%3Aproof%3Av1%3A00000000-0000-4000-8000-000000000000`);
  await waitForRuntimeStatus(tab);
  assert.equal(locationString(await snapshot(tab)), "/research?workspace=validate&tab=evidence&proofEntity=tc-research%3Aproof%3Av1%3A00000000-0000-4000-8000-000000000000");
  await tab.goto(`${baseUrl}/research?stage=construct&tab=idea`);
  await waitForRuntimeStatus(tab);
  assert.equal(locationString(await snapshot(tab)), "/research?workspace=signals&tab=overview");

  // Assistant round trip on the fixture desktop, which runs without a provider credential: the
  // widget stays enabled and the backend's exact provider_not_configured state comes back.
  await tab.goto(`${specificationBaseUrl}/apollo`);
  await waitForRuntimeStatus(tab);
  let assistant = await snapshot(tab);
  assert.equal(assistant.assistantReady, "false");
  assert.equal(assistant.assistantDisabled, false);
  assert.match(assistant.text, /Assistant transport is not configured on this desktop/);
  assert.match(await tab.playwright.locator("[data-assistant-voice-status]").first().textContent(), /Voice: Provider Not Configured/);
  assert.equal(await tab.playwright.locator("[data-assistant-voice]").first().isDisabled(), false);
  await tab.playwright.locator('[data-assistant-form] textarea[name="message"]').first().fill("What is bound in research custody?");
  await tab.playwright.locator("[data-assistant-ask]").first().click();
  assistant = await waitForAssistantReply(tab, 2);
  assert.deepEqual(assistant.assistantMessages.map((message) => message.role), ["user", "assistant"]);
  assert.equal(assistant.assistantMessages[0].text, "What is bound in research custody?");
  assert.equal(assistant.assistantMessages[1].error, true);
  assert.match(assistant.assistantMessages[1].text, /Provider Not Configured: Set OPENROUTER_API_KEY/);

  await tab.goto(`${specificationBaseUrl}/research?workspace=evolution`);
  await waitForRuntimeStatus(tab);
  await waitForBuildWorkspace(tab);
  await tab.playwright.locator('[data-build-action="compile"]').click();
  let buildState = await waitForBuildWorkspace(tab, "compiled");
  assert.equal(buildState.buildLaunchGate, "approval-required");
  assert.match(buildState.text, /exact_native_builder_task_snapshot/i);
  assert.match(buildState.text, /Byte identical/i);
  assert.match(buildState.text, /Source project SHA-256/i);
  assert.match(buildState.text, /Executable XML SHA-256/i);
  assert.match(buildState.text, /Approve the exact configuration revision/i);
  assert.equal(await tab.playwright.locator('[data-build-launch-gate] button').isDisabled(), true);
  assert.equal(await tab.playwright.locator('[data-native-builder-launch]').count(), 0);
  const compiledRevision = await currentBuildRevision(tab);
  assert.match(compiledRevision, /^tc-research-revision:configuration:sha256:/);

  await tab.playwright.locator('[data-build-action="approve"]').click();
  buildState = await waitForBuildWorkspace(tab, "approved");
  const approvedRevision = await currentBuildRevision(tab);
  assert.notEqual(approvedRevision, compiledRevision, "approval creates a new immutable configuration revision");
  assert.match(buildState.text, /Approved exact revision/i);
  assert.equal(buildState.buildLaunchGate, "runtime-unavailable");
  assert.equal(await tab.playwright.locator('[data-build-launch-gate] button').isDisabled(), true);

  await tab.reload();
  await waitForRuntimeStatus(tab);
  buildState = await waitForBuildWorkspace(tab, "approved");
  assert.equal(await currentBuildRevision(tab), approvedRevision, "reload recovers the exact approved configuration revision");
  assert.match(buildState.text, /Approved exact revision/i);
  assert.equal(buildState.buildLaunchGate, "runtime-unavailable");

  await tab.goto(`${baseUrl}/research?workspace=signals&tab=overview`);
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
  await tab.playwright.locator('.primary-nav a[href="/builder"]').first().click();
  await tab.playwright.waitForTimeout(60);
  let state = await snapshot(tab);
  assert.equal(state.pathname, "/builder");
  assert.equal(state.surfaceId, "builder");
  assert.match(state.text, /Builder/);
  assert.doesNotMatch(state.text, /Evolutionary Search/);
  assert.doesNotMatch(state.text, /Signals & Models/);

  await tab.playwright.locator('.primary-nav a[href="/custom-projects"]').first().click();
  await tab.playwright.waitForTimeout(60);
  state = await snapshot(tab);
  assert.equal(state.pathname, "/custom-projects");
  assert.equal(state.surfaceId, "custom-projects");
  assert.match(state.text, /Custom projects|Custom Project/);

  await tab.goto(`${baseUrl}/explore`);
  await tab.playwright.waitForTimeout(60);
  state = await snapshot(tab);
  assert.equal(state.pathname, "/home");
  assert.equal(state.surfaceId, "home");
  assert.doesNotMatch(state.text, /Install them here/);
  assert.doesNotMatch(state.navRoutes.join(" "), /\/explore/);

  await tab.goto(`${baseUrl}/research`);
  await tab.playwright.waitForTimeout(60);
  state = await snapshot(tab);
  assert.equal(state.pathname, "/builder");
  assert.equal(state.surfaceId, "builder");

  for (const legacyModulePath of ["/retester", "/optimizer"]) {
    await tab.goto(`${baseUrl}${legacyModulePath}`);
    await tab.playwright.waitForTimeout(60);
    state = await snapshot(tab);
    assert.equal(state.pathname, "/builder");
    assert.equal(state.surfaceId, "builder");
    assert.doesNotMatch(state.navRoutes.join(" "), new RegExp(legacyModulePath.replace("/", "\\/")));
  }

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
