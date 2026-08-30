import assert from "node:assert/strict";

export const BROWSER_STRATEGY_REF = "signed/spec-v2:opaque+42";
export const BROWSER_ADVERSARIAL_STRATEGY_REF = "  opaque/percent%+query?#&= Khmer ខ្មែរ  ";
const encodedStrategyRef = encodeURIComponent(BROWSER_STRATEGY_REF);
const selectedStrategyPrefix = `/strategies/${encodedStrategyRef}`;
const cockpitAuthorityZones = [
  "market-overview",
  "system-status",
  "alpha-stack",
  "pipeline-overview",
  "signals",
  "risk",
  "performance",
  "quick-actions",
];
const signalsAuthorityZones = [
  "signals-zone-chart",
  "signals-zone-models",
  "signals-zone-confluence",
  "signals-zone-history",
  "signals-zone-market-state",
];

export const BROWSER_CANONICAL_CASES = [
  { path: "/cockpit", stateKey: "cockpit.home", workspace: "cockpit" },
  { path: "/strategies", stateKey: "strategies.root", workspace: "strategies" },
  { path: `${selectedStrategyPrefix}/overview`, stateKey: "strategies.overview", workspace: "strategies" },
  { path: `${selectedStrategyPrefix}/build`, stateKey: "strategies.build", workspace: "strategies" },
  { path: `${selectedStrategyPrefix}/signals`, stateKey: "strategies.signals", workspace: "strategies" },
  { path: `${selectedStrategyPrefix}/candidates`, stateKey: "strategies.candidates", workspace: "strategies" },
  { path: `${selectedStrategyPrefix}/evidence`, stateKey: "strategies.evidence", workspace: "strategies" },
  { path: "/explore", stateKey: "explore.root", workspace: "explore" },
  { path: "/explore/catalog", stateKey: "explore.catalog", workspace: "explore" },
  { path: "/explore/market", stateKey: "explore.market", workspace: "explore" },
  { path: "/explore/data", stateKey: "explore.data", workspace: "explore" },
  { path: "/validate", stateKey: "validate.root", workspace: "validate" },
  { path: "/validate/run", stateKey: "validate.run", workspace: "validate" },
  { path: "/validate/results", stateKey: "validate.results", workspace: "validate" },
  { path: "/validate/stress", stateKey: "validate.stress", workspace: "validate" },
  { path: "/validate/compare", stateKey: "validate.compare", workspace: "validate" },
  { path: "/validate/prop", stateKey: "validate.prop", workspace: "validate" },
  { path: "/operate", stateKey: "operate.root", workspace: "operate" },
  { path: "/operate/runs", stateKey: "operate.runs", workspace: "operate" },
  { path: "/operate/performance", stateKey: "operate.performance", workspace: "operate" },
  { path: "/operate/execution-risk", stateKey: "operate.execution-risk", workspace: "operate" },
];

export const BROWSER_LEGACY_CASES = [
  { path: "/", name: "home", target: "/cockpit", stateKey: "cockpit.home", workspace: "cockpit" },
  { path: "/home", name: "home", target: "/cockpit", stateKey: "cockpit.home", workspace: "cockpit" },
  { path: "/strategy-signals", name: "strategy-signals", target: `${selectedStrategyPrefix}/signals`, stateKey: "strategies.signals", workspace: "strategies", consumesStrategyRef: true },
  { path: "/research", name: "research", target: "/explore/catalog", stateKey: "explore.catalog", workspace: "explore" },
  { path: "/validation", name: "validate", target: "/validate/results", stateKey: "validate.results", workspace: "validate" },
  { path: "/old/validate", name: "validate", target: "/validate/results", stateKey: "validate.results", workspace: "validate" },
  { path: "/evolution", name: "evolution", target: `${selectedStrategyPrefix}/candidates`, stateKey: "strategies.candidates", workspace: "strategies", consumesStrategyRef: true },
  { path: "/evolutionary-search", name: "evolution", target: `${selectedStrategyPrefix}/candidates`, stateKey: "strategies.candidates", workspace: "strategies", consumesStrategyRef: true },
  { path: "/prop", name: "prop", target: "/validate/prop", stateKey: "validate.prop", workspace: "validate" },
  { path: "/prop-simulation", name: "prop", target: "/validate/prop", stateKey: "validate.prop", workspace: "validate" },
  { path: "/monitor", name: "monitor", target: "/operate/runs", stateKey: "operate.runs", workspace: "operate" },
  { path: "/performance", name: "performance", target: "/operate/performance", stateKey: "operate.performance", workspace: "operate" },
  { path: "/execution", name: "execution", target: "/operate/execution-risk", stateKey: "operate.execution-risk", workspace: "operate" },
  { path: "/execution-risk", name: "execution", target: "/operate/execution-risk", stateKey: "operate.execution-risk", workspace: "operate" },
  { path: "/governance", name: "governance", target: `${selectedStrategyPrefix}/evidence`, stateKey: "strategies.evidence", workspace: "strategies", consumesStrategyRef: true },
  { path: "/chart", name: "chart", target: `${selectedStrategyPrefix}/signals`, stateKey: "strategies.signals", workspace: "strategies", consumesStrategyRef: true },
  { path: "/backtest", name: "backtest", target: "/validate/results", stateKey: "validate.results", workspace: "validate" },
  { path: "/proof", name: "proof", target: `${selectedStrategyPrefix}/evidence`, stateKey: "strategies.evidence", workspace: "strategies", consumesStrategyRef: true },
];

async function readShellState(tab) {
  return tab.playwright.evaluate(() => {
    const shell = document.querySelector(".app-shell");
    const app = document.querySelector("#app");
    const activePrimary = document.querySelector('.primary-link[aria-current="page"]');
    const url = new URL(location.href);
    const shellText = shell?.textContent || "";
    const renderedWithoutReference = shellText.replaceAll("signed/spec-v2:opaque+42", "");
    const falseAvailabilityStates = "unavailable|not available|offline|absent|broken|unsupported|down|disconnected|unreachable|failed|stopped|unhealthy|missing";
    const falseProducerAvailabilityClaimPatterns = [
      new RegExp(`\\b(?:market data|strategy custody|strategy data|catalog data|run lifecycle|run state|validation result|validation results|robustness result|robustness results|evidence(?: and provenance)?|prop rule-set|execution and risk(?: state)?)\\s+producer(?:\\s+[\\w&-]+){0,4}\\s+(?:is\\s+)?(?:${falseAvailabilityStates})\\b`, "i"),
      new RegExp(`\\b(?:producer|provider)(?:\\s+[\\w&-]+){0,4}\\s+(?:is\\s+)?(?:${falseAvailabilityStates})\\b`, "i"),
      new RegExp(`\\bruntime(?:\\s+[\\w&-]+){0,4}\\s+(?:is\\s+)?(?:${falseAvailabilityStates})\\b`, "i"),
    ];
    const falseProducerAvailabilityClaims = falseProducerAvailabilityClaimPatterns
      .filter((pattern) => pattern.test(renderedWithoutReference))
      .map((pattern) => pattern.source);
    const cockpitZoneNames = [
      "market-overview",
      "system-status",
      "alpha-stack",
      "pipeline-overview",
      "signals",
      "risk",
      "performance",
      "quick-actions",
    ];
    const signalsZoneNames = [
      "signals-zone-chart",
      "signals-zone-models",
      "signals-zone-confluence",
      "signals-zone-history",
      "signals-zone-market-state",
    ];
    return {
      url: `${url.pathname}${url.search}`,
      stateKey: shell?.dataset.stateKey,
      workspace: shell?.dataset.workspace,
      identityOnly: shell?.dataset.identityOnly,
      requestedStrategyRef: shell?.dataset.requestedStrategyRef,
      activePrimaryCount: document.querySelectorAll('.primary-link[aria-current="page"]').length,
      activePrimary: activePrimary?.dataset.primaryWorkspace,
      strategyRef: url.searchParams.get("strategyRef"),
      strategyTextPresent: shellText.includes("signed/spec-v2:opaque+42"),
      producerIntegrationPending: shellText.includes("Producer integration pending"),
      runtimeStatusPending: /Runtime status pending/i.test(shellText),
      runtimeUnavailable: /Runtime unavailable/i.test(shellText),
      pendingBadgeCount: document.querySelectorAll(".status-badge.status-pending").length,
      unavailableBadgeCount: document.querySelectorAll(".status-badge.status-unavailable").length,
      cockpitAuthorityZones: cockpitZoneNames.filter((zone) => document.querySelector(`.cockpit-zone-${zone}`)),
      signalsAuthorityZones: signalsZoneNames.filter((zone) => document.querySelectorAll(`.${zone}`).length === 1),
      falseProducerAvailabilityClaims,
      falseAuthorityClaims: {
        verifiedStrategy: /\bverified strategy\b/i.test(renderedWithoutReference),
        authenticatedStrategy: /\bauthenticated (?:strategy|reference|identity)\b/i.test(renderedWithoutReference),
        signedStrategy: /\bsigned (?:strategy|spec|reference|identity)\b/i.test(renderedWithoutReference),
        executableStrategy: /\bexecutable strategy\b/i.test(renderedWithoutReference),
        lockedIdentity: /LOCKED IDENTITY/i.test(renderedWithoutReference),
        strategySpecV2: /\bStrategySpecV2\b/i.test(renderedWithoutReference),
      },
      apolloSurfaceCount: document.querySelectorAll("[data-apollo-surface]").length,
      apolloInstance: document.querySelector("#apollo-persistent")?.dataset.apolloInstance,
      apolloMountCount: app?.dataset.apolloMountCount,
    };
  });
}

async function settle(tab) {
  await tab.playwright.waitForTimeout(60);
}

function assertShellState(observed, expected, label, expectedRequestedStrategyRef = "") {
  assert.equal(observed.stateKey, expected.stateKey, label);
  assert.equal(observed.workspace, expected.workspace, label);
  assert.equal(observed.requestedStrategyRef, expectedRequestedStrategyRef, label);
  assert.equal(observed.activePrimaryCount, 1, label);
  assert.equal(observed.activePrimary, expected.workspace, label);
  assert.equal(observed.apolloSurfaceCount, 1, label);
  assert.equal(observed.apolloMountCount, "1", label);
  assert.equal(observed.apolloInstance, "1", label);
  assert.equal(observed.producerIntegrationPending, true, label);
  assert.equal(observed.runtimeStatusPending, true, label);
  assert.equal(observed.runtimeUnavailable, false, label);
  assert.ok(observed.pendingBadgeCount > 0, label);
  assert.equal(observed.unavailableBadgeCount, 0, label);
}

function assertNoFalseStrategyAuthorityClaims(observed, label) {
  for (const [claim, present] of Object.entries(observed.falseAuthorityClaims)) {
    assert.equal(present, false, `${label}: ${claim}`);
  }
}

function assertNoFalseProducerAvailabilityClaims(observed, label) {
  assert.equal(observed.falseProducerAvailabilityClaims.length, 0, label);
}

export async function runBrowserRegression(tab, { baseUrl = "http://127.0.0.1:4173" } = {}) {
  const canonical = [];
  for (const expected of BROWSER_CANONICAL_CASES) {
    await tab.goto(`${baseUrl}${expected.path}`);
    await settle(tab);
    const observed = await readShellState(tab);
    const expectedRequestedStrategyRef = expected.path.includes(selectedStrategyPrefix)
      ? BROWSER_STRATEGY_REF
      : "";
    assertShellState(observed, expected, expected.path, expectedRequestedStrategyRef);
    assertNoFalseStrategyAuthorityClaims(observed, expected.path);
    assertNoFalseProducerAvailabilityClaims(observed, expected.path);
    if (expected.stateKey === "cockpit.home") {
      assert.deepEqual(Array.from(observed.cockpitAuthorityZones), cockpitAuthorityZones, "Cockpit Home authority zones");
    }
    if (expected.stateKey === "strategies.signals") {
      assert.deepEqual(Array.from(observed.signalsAuthorityZones), signalsAuthorityZones, "Signals & Models authority zones");
    }
    canonical.push({ ...expected, observed });
  }

  await tab.goto(`${baseUrl}/explore/catalog`);
  await settle(tab);
  const producerBoundary = await readShellState(tab);
  assert.equal(producerBoundary.producerIntegrationPending, true);
  assert.equal(producerBoundary.runtimeStatusPending, true);
  assert.equal(producerBoundary.runtimeUnavailable, false);
  assert.ok(producerBoundary.pendingBadgeCount > 0);
  assert.equal(producerBoundary.unavailableBadgeCount, 0);
  assertNoFalseProducerAvailabilityClaims(producerBoundary, "producer boundary");
  assertNoFalseStrategyAuthorityClaims(producerBoundary, "producer boundary");

  const contextualCases = [
    { path: `/cockpit?strategyRef=${encodedStrategyRef}`, stateKey: "cockpit.home", workspace: "cockpit" },
    { path: `/strategies?strategyRef=${encodedStrategyRef}`, stateKey: "strategies.root", workspace: "strategies" },
    { path: `/explore?strategyRef=${encodedStrategyRef}`, stateKey: "explore.root", workspace: "explore" },
    { path: `/explore/catalog?strategyRef=${encodedStrategyRef}`, stateKey: "explore.catalog", workspace: "explore" },
    { path: `/explore/market?strategyRef=${encodedStrategyRef}`, stateKey: "explore.market", workspace: "explore" },
    { path: `/explore/data?strategyRef=${encodedStrategyRef}`, stateKey: "explore.data", workspace: "explore" },
    { path: `/validate?strategyRef=${encodedStrategyRef}`, stateKey: "validate.root", workspace: "validate" },
    { path: `/validate/run?strategyRef=${encodedStrategyRef}`, stateKey: "validate.run", workspace: "validate" },
    { path: `/validate/results?strategyRef=${encodedStrategyRef}`, stateKey: "validate.results", workspace: "validate" },
    { path: `/validate/stress?strategyRef=${encodedStrategyRef}`, stateKey: "validate.stress", workspace: "validate" },
    { path: `/validate/compare?strategyRef=${encodedStrategyRef}`, stateKey: "validate.compare", workspace: "validate" },
    { path: `/validate/prop?strategyRef=${encodedStrategyRef}`, stateKey: "validate.prop", workspace: "validate" },
    { path: `/operate?strategyRef=${encodedStrategyRef}`, stateKey: "operate.root", workspace: "operate" },
    { path: `/operate/runs?strategyRef=${encodedStrategyRef}`, stateKey: "operate.runs", workspace: "operate" },
    { path: `/operate/performance?strategyRef=${encodedStrategyRef}`, stateKey: "operate.performance", workspace: "operate" },
    { path: `/operate/execution-risk?strategyRef=${encodedStrategyRef}`, stateKey: "operate.execution-risk", workspace: "operate" },
  ];
  const contextual = [];
  for (const expected of contextualCases) {
    await tab.goto(`${baseUrl}${expected.path}`);
    await settle(tab);
    const observed = await readShellState(tab);
    assertShellState(observed, expected, expected.path, BROWSER_STRATEGY_REF);
    assert.equal(observed.strategyRef, BROWSER_STRATEGY_REF, expected.path);
    assert.equal(observed.strategyTextPresent, true, expected.path);
    assertNoFalseStrategyAuthorityClaims(observed, expected.path);
    assertNoFalseProducerAvailabilityClaims(observed, expected.path);
    contextual.push({ ...expected, observed });
  }

  const legacy = [];
  for (const expected of BROWSER_LEGACY_CASES) {
    await tab.goto(`${baseUrl}${expected.path}?unrelated=keep&strategyRef=${encodedStrategyRef}`);
    await settle(tab);
    const observed = await readShellState(tab);
    assertShellState(observed, expected, expected.path, BROWSER_STRATEGY_REF);
    const targetUrl = new URL(observed.url, baseUrl);
    assert.equal(targetUrl.pathname, expected.target, expected.path);
    assert.equal(targetUrl.searchParams.get("unrelated"), "keep", expected.path);
    assert.equal(
      targetUrl.searchParams.get("strategyRef"),
      expected.consumesStrategyRef ? null : BROWSER_STRATEGY_REF,
      expected.path,
    );
    assertNoFalseStrategyAuthorityClaims(observed, expected.path);
    assertNoFalseProducerAvailabilityClaims(observed, expected.path);
    legacy.push({ ...expected, observed });
  }

  await tab.goto(`${baseUrl}/strategy-signals`);
  await settle(tab);
  const missingIdentity = await readShellState(tab);
  assertShellState(
    missingIdentity,
    { stateKey: "strategies.signals", workspace: "strategies" },
    "legacy missing identity",
  );
  assert.equal(missingIdentity.identityOnly, "true");
  assert.equal(missingIdentity.strategyRef, null);
  assertNoFalseStrategyAuthorityClaims(missingIdentity, "legacy missing identity");
  assertNoFalseProducerAvailabilityClaims(missingIdentity, "legacy missing identity");

  await tab.goto(`${baseUrl}/strategies`);
  await settle(tab);
  await tab.playwright.locator('[data-strategy-form] input[name="strategyRef"]').fill(BROWSER_ADVERSARIAL_STRATEGY_REF);
  await tab.playwright.locator('[data-strategy-form] button[type="submit"]').click();
  await settle(tab);
  const adversarialForm = await readShellState(tab);
  const adversarialPath = `/strategies/${encodeURIComponent(BROWSER_ADVERSARIAL_STRATEGY_REF)}/overview`;
  assertShellState(
    adversarialForm,
    { stateKey: "strategies.overview", workspace: "strategies" },
    "adversarial form reference",
    BROWSER_ADVERSARIAL_STRATEGY_REF,
  );
  assert.equal(new URL(adversarialForm.url, baseUrl).pathname, adversarialPath);
  assert.equal(adversarialForm.requestedStrategyRef, BROWSER_ADVERSARIAL_STRATEGY_REF);
  assertNoFalseProducerAvailabilityClaims(adversarialForm, "adversarial form reference");

  await tab.playwright.locator('.page-actions a[data-route^="/validate/run?strategyRef="]').click();
  await settle(tab);
  const adversarialValidate = await readShellState(tab);
  assertShellState(
    adversarialValidate,
    { stateKey: "validate.run", workspace: "validate" },
    "adversarial validation handoff",
    BROWSER_ADVERSARIAL_STRATEGY_REF,
  );
  assert.equal(adversarialValidate.strategyRef, BROWSER_ADVERSARIAL_STRATEGY_REF);

  await tab.playwright.locator('a[data-primary-workspace="explore"]').click();
  await settle(tab);
  const adversarialExplore = await readShellState(tab);
  assertShellState(
    adversarialExplore,
    { stateKey: "explore.root", workspace: "explore" },
    "adversarial workspace navigation",
    BROWSER_ADVERSARIAL_STRATEGY_REF,
  );
  assert.equal(adversarialExplore.strategyRef, BROWSER_ADVERSARIAL_STRATEGY_REF);
  const adversarialReference = {
    fixture: BROWSER_ADVERSARIAL_STRATEGY_REF,
    afterForm: adversarialForm,
    afterValidationHandoff: adversarialValidate,
    afterWorkspaceNavigation: adversarialExplore,
  };

  await tab.goto(`${baseUrl}${selectedStrategyPrefix}/signals`);
  await settle(tab);
  const cdp = await tab.capabilities.get("cdp");
  const remoteApollo = await cdp.send("Runtime.evaluate", {
    expression: "document.querySelector('#apollo-persistent')",
    returnByValue: false,
    objectGroup: "tc-browser-regression",
  });
  const apolloObjectId = remoteApollo.result.objectId;
  const identityStart = await readShellState(tab);

  await tab.playwright.locator('a[data-primary-workspace="explore"]').click();
  await settle(tab);
  const sameAfterExplore = await cdp.send("Runtime.callFunctionOn", {
    objectId: apolloObjectId,
    functionDeclaration: "function () { return this === document.querySelector('#apollo-persistent'); }",
    returnByValue: true,
  });
  const identityExplore = await readShellState(tab);

  await tab.playwright.locator('a[data-primary-workspace="strategies"]').click();
  await settle(tab);
  const sameAfterStrategies = await cdp.send("Runtime.callFunctionOn", {
    objectId: apolloObjectId,
    functionDeclaration: "function () { return this === document.querySelector('#apollo-persistent'); }",
    returnByValue: true,
  });
  const identityStrategies = await readShellState(tab);
  await cdp.send("Runtime.releaseObjectGroup", { objectGroup: "tc-browser-regression" });

  assert.equal(sameAfterExplore.result?.value, true, "Apollo root changed during Explore navigation");
  assert.equal(sameAfterStrategies.result?.value, true, "Apollo root changed during Strategies navigation");
  assert.equal(identityExplore.strategyTextPresent, true);
  assert.equal(identityStrategies.strategyTextPresent, true);
  const apolloIdentity = {
    start: identityStart,
    afterExplore: { ...identityExplore, sameDomNode: sameAfterExplore.result?.value === true },
    afterStrategies: { ...identityStrategies, sameDomNode: sameAfterStrategies.result?.value === true },
  };

  await tab.goto(`${baseUrl}/strategies?strategyRef=${encodedStrategyRef}`);
  await settle(tab);
  await tab.reload();
  await settle(tab);
  const afterRefresh = await readShellState(tab);
  assertShellState(afterRefresh, { stateKey: "strategies.root", workspace: "strategies" }, "refresh", BROWSER_STRATEGY_REF);
  assert.equal(afterRefresh.strategyRef, BROWSER_STRATEGY_REF);
  await tab.playwright.locator('a[data-primary-workspace="validate"]').click();
  await settle(tab);
  const afterValidate = await readShellState(tab);
  assertShellState(afterValidate, { stateKey: "validate.root", workspace: "validate" }, "validate history", BROWSER_STRATEGY_REF);
  assert.equal(afterValidate.strategyRef, BROWSER_STRATEGY_REF);
  await tab.playwright.locator('a[data-primary-workspace="operate"]').click();
  await settle(tab);
  const afterOperate = await readShellState(tab);
  assertShellState(afterOperate, { stateKey: "operate.root", workspace: "operate" }, "operate history", BROWSER_STRATEGY_REF);
  assert.equal(afterOperate.strategyRef, BROWSER_STRATEGY_REF);
  await tab.back();
  await settle(tab);
  const afterBack = await readShellState(tab);
  assertShellState(afterBack, { stateKey: "validate.root", workspace: "validate" }, "back", BROWSER_STRATEGY_REF);
  assert.equal(afterBack.strategyRef, BROWSER_STRATEGY_REF);
  await tab.forward();
  await settle(tab);
  const afterForward = await readShellState(tab);
  assertShellState(afterForward, { stateKey: "operate.root", workspace: "operate" }, "forward", BROWSER_STRATEGY_REF);
  assert.equal(afterForward.strategyRef, BROWSER_STRATEGY_REF);
  const history = { afterRefresh, afterValidate, afterOperate, afterBack, afterForward };

  await tab.goto(`${baseUrl}/explore/catalog?unrelated=keep`);
  await settle(tab);
  const unrelatedQuery = await readShellState(tab);
  assert.equal(unrelatedQuery.strategyRef, null, "unrelated query became strategy identity");
  assert.equal(unrelatedQuery.strategyTextPresent, false, "unrelated query rendered strategy identity");

  return { canonical, contextual, legacy, producerBoundary, adversarialReference, apolloIdentity, history, unrelatedQuery };
}
