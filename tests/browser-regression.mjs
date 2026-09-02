import assert from "node:assert/strict";

const TOP_LEVEL_ROUTES = Object.freeze([
  "/home",
  "/research",
  "/explore",
  "/automation",
  "/operate",
  "/settings",
]);

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
    alphaStackState: document.querySelector("[data-home-alpha-stack]")?.getAttribute("data-alpha-stack-state") || "",
    text: document.body.innerText,
  }));
}

async function waitForRuntimeStatus(tab) {
  for (let attempt = 0; attempt < 150; attempt += 1) {
    const state = await snapshot(tab);
    if (
      (state.runtimeStatus === "loaded" || state.runtimeStatus === "failed")
      && state.custodyStatus !== "loading"
      && state.marketStatus !== "loading"
    ) return state;
    await tab.playwright.waitForTimeout(20);
  }
  assert.fail("runtime, market and custody status did not settle");
}

async function waitForHomeAlphaStack(tab) {
  // The Home candidate-review zone binds Alpha Stack custody through four
  // independent async catalog fetches after runtime/custody status settles, so
  // wait for the binder to leave its pending state before asserting its content.
  for (let attempt = 0; attempt < 200; attempt += 1) {
    const state = await snapshot(tab);
    if (state.alphaStackState && state.alphaStackState !== "pending") return state;
    await tab.playwright.waitForTimeout(25);
  }
  assert.fail("Home Alpha Stack custody binder did not settle");
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

async function waitForEvolutionMode(tab) {
  for (let attempt = 0; attempt < 200; attempt += 1) {
    const state = await snapshot(tab);
    if (state.evolutionMode && !/Reading/i.test(state.evolutionMode)) return state;
    await tab.playwright.waitForTimeout(50);
  }
  assert.fail("Evolutionary Search did not bind the native search mode");
}

async function waitForSpecificationBinding(tab) {
  for (let attempt = 0; attempt < 100; attempt += 1) {
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
    assert.match(state.text, /Live Runs/, `status bar must be present on ${route}`);
    assert.match(state.text, /Last Run:/, `status bar last-run cell must be present on ${route}`);
    assert.doesNotMatch(state.text, /\$\s?\d/, `no fabricated money values on ${route}`);
    assert.doesNotMatch(state.text, /PR #/i, `stale PR authority must not appear on ${route}`);
    assert.doesNotMatch(state.text, /donor/i, `donor language must not appear on ${route}`);
    visited.push(route);
  }

  await tab.goto(`${baseUrl}/home`);
  await waitForRuntimeStatus(tab);
  const home = await waitForHomeAlphaStack(tab);
  assert.equal(home.surfaceId, "home");
  assert.equal(home.runtimeStatus, "loaded");
  assert.deepEqual(home.homeZones, [
    "research",
    "build-backtest",
    "prop-simulation",
    "proof-evidence",
    "active-builds",
    "candidate-review",
    "system-health",
    "assistant",
  ]);
  assert.match(home.text, /Cockpit Home/i);
  assert.match(home.text, /Turn Research into/i);
  assert.match(home.text, /Decisions that Compound/i);
  assert.match(home.text, /Recent Activity/i);
  assert.match(home.text, /Build & Backtest/i);
  assert.match(home.text, /Prop Firm Simulation/i);
  assert.match(home.text, /Proof & Evidence/i);
  assert.match(home.text, /Active Builds/i);
  assert.match(home.text, /Candidate Review/i);
  assert.match(home.text, /System Health/i);
  assert.match(home.text, /TraderCockpit application/i);
  assert.match(home.text, /Research backend/i);
  assert.match(home.text, /Runtime Not Configured/i);
  assert.match(home.text, /Research custody/i);
  assert.match(home.text, /Native execution/i);
  assert.match(home.text, /Disabled · Runtime Not Configured/i);
  assert.match(home.text, /Live market data/i);
  assert.match(home.text, /Producer Not Configured/i);
  assert.match(home.text, /Consumer account/i);
  assert.match(home.text, /Model access/i);
  assert.match(home.text, /Extensions/i);
  assert.match(home.text, /Assistant/);
  assert.match(home.text, /Good day, Trader\.|Assistant transport is not configured on this desktop/, "assistant readiness is described truthfully from /api/status");
  assert.doesNotMatch(home.text, /assistant is not connected yet/i);
  assert.equal(home.assistantDisabled, false, "the assistant is never disabled");
  assert.match(home.assistantReady, /^(true|false)$/);
  assert.match(home.text, /Research Candidates/i);
  assert.match(home.text, /No operator promotion after Proof yet/i);
  assert.match(home.text, /Current catalog · 0/);
  assert.doesNotMatch(home.text, /A\+ Champion|B Champion|Rules OK/);
  assert.doesNotMatch(home.text, /\$\s?\d/);

  for (const route of RESEARCH_ROUTES) {
    const routeBaseUrl = NATIVE_FIXTURE_ROUTES.has(route) ? specificationBaseUrl : baseUrl;
    await tab.goto(`${routeBaseUrl}${route}`);
    let state = await waitForRuntimeStatus(tab);
    assert.equal(state.pathname, "/research", `Research pathname for ${route}`);
    assert.equal(state.surfaceId, "research", `Research surface for ${route}`);
    assert.equal(state.shell, "tradercockpit-desktop", `product shell for ${route}`);
    assert.equal(state.runtimeStatus, "loaded", `runtime status for ${route}`);
    assert.equal(locationString(state), route, `canonical route for ${route}`);
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
    }
    if (route === "/research?workspace=evolution") {
      state = await waitForBuildWorkspace(tab);
      state = await waitForEvolutionMode(tab);
      assert.equal(state.workspaceId, "evolution");
      assert.match(state.text, /Evolutionary Search/);
      // A fresh data root has no approved configuration, so the native search
      // controls stay truthfully unavailable with no live-install fallback
      // (per the Random-vs-Genetic binding: controls appear only after approve).
      // The approved-configuration controls are covered by research-evolution.test.mjs.
      assert.match(state.evolutionMode, /Unavailable/i);
      assert.match(state.text, /No approved configuration/i);
      assert.match(state.text, /Native configuration unavailable/i);
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
      assert.match(state.text, /Machine Learning \/ Models/i);
      assert.match(state.text, /sklearn|Models backend|No fitted models|No completed Historical Result/i);
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
  await tab.goto(`${specificationBaseUrl}/home`);
  await waitForRuntimeStatus(tab);
  let assistant = await snapshot(tab);
  assert.equal(assistant.assistantReady, "false");
  assert.equal(assistant.assistantDisabled, false);
  assert.match(assistant.text, /Assistant transport is not configured on this desktop/);
  await tab.playwright.locator('[data-assistant-form] input[name="message"]').first().fill("What is bound in research custody?");
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
  await tab.playwright.locator('.primary-nav a[href="/research"]').first().click();
  await tab.playwright.waitForTimeout(60);
  let state = await snapshot(tab);
  assert.equal(state.pathname, "/research");
  assert.equal(state.surfaceId, "research");
  assert.equal(state.workspaceId, "signals");
  assert.equal(state.tabId, "overview");
  assert.equal(locationString(state), "/research?workspace=signals&tab=overview", "sidebar entry canonicalises to the first workspace/tab");

  await tab.playwright.locator('a[href="/research?workspace=validate&tab=overview"]').first().click();
  await tab.playwright.waitForTimeout(60);
  state = await snapshot(tab);
  assert.equal(locationString(state), "/research?workspace=validate&tab=overview");
  assert.equal(state.workspaceId, "validate");
  assert.equal(state.tabId, "overview");

  await tab.playwright.locator('a[href="/research?workspace=validate&tab=evidence"]').first().click();
  await tab.playwright.waitForTimeout(60);
  state = await snapshot(tab);
  assert.equal(locationString(state), "/research?workspace=validate&tab=evidence");
  assert.equal(state.tabId, "evidence");

  await tab.playwright.locator('a[href="/research?workspace=evolution"]').first().click();
  await tab.playwright.waitForTimeout(60);
  state = await snapshot(tab);
  assert.equal(locationString(state), "/research?workspace=evolution");
  assert.equal(state.workspaceId, "evolution");
  assert.equal(state.tabId, "");

  await tab.back();
  await tab.playwright.waitForTimeout(60);
  assert.equal(locationString(await snapshot(tab)), "/research?workspace=validate&tab=evidence");
  await tab.forward();
  await tab.playwright.waitForTimeout(60);
  assert.equal(locationString(await snapshot(tab)), "/research?workspace=evolution");

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
