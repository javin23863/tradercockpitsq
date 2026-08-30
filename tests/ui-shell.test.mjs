import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import { renderApp } from "../web/app.mjs";
import {
  LEGACY_REDIRECTS,
  LOGICAL_STATES,
  PRIMARY_WORKSPACES,
  RUN_CONTEXT_OWNER,
  RUN_SURFACE_ID,
  contextualPath,
  pathForState,
  resolveRoute,
} from "../web/model.mjs";

const primaryLabels = PRIMARY_WORKSPACES.map((workspace) => workspace.label);
const strategyRef = "signed/spec-v2:opaque+42";
const adversarialStrategyRef = "  opaque/percent%+query?#&= Khmer ខ្មែរ  ";
const expectedStateKeys = [
  "cockpit.home",
  "strategies.root",
  "strategies.overview",
  "strategies.build",
  "strategies.signals",
  "strategies.candidates",
  "strategies.evidence",
  "explore.root",
  "explore.catalog",
  "explore.market",
  "explore.data",
  "validate.root",
  "validate.run",
  "validate.results",
  "validate.stress",
  "validate.compare",
  "validate.prop",
  "operate.root",
  "operate.runs",
  "operate.performance",
  "operate.execution-risk",
];

const productionSources = await Promise.all([
  readFile(new URL("../web/app.mjs", import.meta.url), "utf8"),
  readFile(new URL("../web/model.mjs", import.meta.url), "utf8"),
  readFile(new URL("../web/index.html", import.meta.url), "utf8"),
  readFile(new URL("../web/styles.css", import.meta.url), "utf8"),
]);

const falseAvailabilityStates = "unavailable|not available|offline|absent|broken|unsupported|down|disconnected|unreachable|failed|stopped|unhealthy|missing";
const producerAvailabilityClaimPatterns = [
  new RegExp(`\\b(?:market data|strategy custody|strategy data|catalog data|run lifecycle|run state|validation result|validation results|robustness result|robustness results|evidence(?: and provenance)?|prop rule-set|execution and risk(?: state)?)\\s+producer(?:\\s+[\\w&-]+){0,4}\\s+(?:is\\s+)?(?:${falseAvailabilityStates})\\b`, "i"),
  new RegExp(`\\b(?:producer|provider)(?:\\s+[\\w&-]+){0,4}\\s+(?:is\\s+)?(?:${falseAvailabilityStates})\\b`, "i"),
  new RegExp(`\\bruntime(?:\\s+[\\w&-]+){0,4}\\s+(?:is\\s+)?(?:${falseAvailabilityStates})\\b`, "i"),
];

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function assertNoFalseStrategyAuthorityClaim(html, label = "rendered strategy reference") {
  const renderedWithoutReference = html.replaceAll(strategyRef, "");
  for (const pattern of [
    /\bverified strategy\b/i,
    /\bauthenticated (?:strategy|reference|identity)\b/i,
    /\bsigned (?:strategy|spec|reference|identity)\b/i,
    /\bexecutable strategy\b/i,
    /LOCKED IDENTITY/i,
    /\bStrategySpecV2\b/i,
  ]) {
    assert.doesNotMatch(renderedWithoutReference, pattern, label);
  }
}

function assertNoFalseProducerAvailabilityClaim(html, label = "rendered producer boundary") {
  const renderedWithoutReference = html
    .replace(/<[^>]+>/g, " ")
    .replaceAll("&amp;", "&")
    .replaceAll(strategyRef, "");
  for (const pattern of producerAvailabilityClaimPatterns) {
    assert.doesNotMatch(renderedWithoutReference, pattern, label);
  }
}

function canonicalCase(state) {
  const selectedRef = state.workspaceId === "strategies" && state.segment
    ? strategyRef
    : "";
  const path = pathForState(state.workspaceId, state.id, selectedRef);
  return { state, path, selectedRef, route: resolveRoute(path) };
}

test("the shell freezes exactly five primary workspaces and 21 logical states", () => {
  assert.deepEqual(primaryLabels, [
    "Cockpit",
    "Strategies",
    "Explore",
    "Test & Validate",
    "Operate",
  ]);
  assert.equal(PRIMARY_WORKSPACES.length, 5);
  assert.equal(LOGICAL_STATES.length, 21);
  assert.deepEqual(LOGICAL_STATES.map((state) => state.stateKey), expectedStateKeys);
  assert.equal(new Set(LOGICAL_STATES.map((state) => state.stateKey)).size, 21);
});

test("every canonical state is distinct, reachable, and activates the owning primary workspace", () => {
  const cases = LOGICAL_STATES.map(canonicalCase);
  assert.equal(new Set(cases.map(({ path }) => path)).size, 21);

  for (const { state, path, route } of cases) {
    assert.equal(route.kind, "state", path);
    assert.equal(route.stateKey, state.stateKey, path);
    assert.equal(route.workspaceId, state.workspaceId, path);
    assert.equal(route.stateId, state.id, path);
    if (state.segment) assert.equal(route.strategyRef, strategyRef, path);

    const html = renderApp({ pathname: path, search: "" });
    assert.match(html, /Producer integration pending/i, path);
    assertNoFalseProducerAvailabilityClaim(html, path);
    assert.match(
      html,
      new RegExp(`data-state-key="${escapeRegExp(state.stateKey)}"`),
      path,
    );
    assert.match(
      html,
      new RegExp(`<a class="primary-link is-active"[^>]*data-primary-workspace="${escapeRegExp(state.workspaceId)}"`),
      path,
    );
  }
});

test("one legacy redirect table maps every recognized alias to a canonical state", () => {
  assert.equal(Object.keys(LEGACY_REDIRECTS).length, 18);

  for (const [legacyPath, legacy] of Object.entries(LEGACY_REDIRECTS)) {
    const redirect = resolveRoute(
      legacyPath,
      `?unrelated=keep&strategyRef=${encodeURIComponent(strategyRef)}`,
    );
    assert.equal(redirect.kind, "redirect", legacyPath);
    assert.equal(redirect.legacyPath, legacyPath, legacyPath);
    assert.equal(redirect.legacyName, legacy.legacyName, legacyPath);

    const target = new URL(redirect.redirectPath, "http://localhost");
    const expectedPath = pathForState(
      legacy.workspaceId,
      legacy.stateId,
      legacy.workspaceId === "strategies" && legacy.stateId !== "root"
        ? strategyRef
        : "",
    );
    assert.equal(target.pathname, expectedPath, legacyPath);
    assert.equal(target.searchParams.get("unrelated"), "keep", legacyPath);

    const strategyTarget = legacy.workspaceId === "strategies" && legacy.stateId !== "root";
    assert.equal(
      target.searchParams.get("strategyRef"),
      strategyTarget ? null : strategyRef,
      legacyPath,
    );

    const canonical = resolveRoute(target.pathname, target.search);
    assert.equal(canonical.stateKey, `${legacy.workspaceId}.${legacy.stateId}`, legacyPath);
    assert.equal(canonical.strategyRef, strategyRef, legacyPath);
  }

  assert.equal(resolveRoute("/validate").stateKey, "validate.root");
  assert.equal(resolveRoute("/validate/results", "?unrelated=keep").strategyRef, "");

  for (const legacyPath of ["/strategy-signals", "/evolution", "/governance", "/chart", "/proof"]) {
    const redirect = resolveRoute(legacyPath);
    assert.equal(redirect.kind, "redirect", legacyPath);
    assert.match(redirect.redirectPath, /^\/strategies\/(signals|candidates|evidence)$/);
    assert.match(renderApp({ pathname: legacyPath, search: "" }), /data-identity-only="true"/);
    assert.doesNotMatch(renderApp({ pathname: legacyPath, search: "" }), /signed\/spec/);
  }
});

test("the rendered shell has one primary navigation and one Apollo surface", () => {
  const html = renderApp({ pathname: "/explore/catalog", search: "" });
  assert.equal((html.match(/data-primary-workspace=/g) || []).length, 5);
  assert.equal((html.match(/data-primary-navigation(?:\s|=)/g) || []).length, 1);
  assert.equal((html.match(/data-apollo-surface(?:\s|=)/g) || []).length, 1);
  assert.match(html, /data-apollo-surface-id="apollo-persistent"/);
  assert.doesNotMatch(html, /<[^>]+>Research<\/[^>]+>/);
  assert.doesNotMatch(html, /<[^>]+>Monitor<\/[^>]+>/);
});

test("Test & Validate and Operate render one shared RunSurface implementation and owner", () => {
  const validate = renderApp({ pathname: "/validate/run", search: "" });
  const operate = renderApp({ pathname: "/operate/runs", search: "" });
  const marker = `data-run-surface-id="${RUN_SURFACE_ID}"`;
  const owner = `data-run-context-owner="${RUN_CONTEXT_OWNER}"`;
  const implementation = `data-run-surface-implementation="${RUN_SURFACE_ID}"`;
  for (const html of [validate, operate]) {
    assert.equal((html.match(new RegExp(marker, "g")) || []).length, 1);
    assert.equal((html.match(new RegExp(owner, "g")) || []).length, 1);
    assert.equal((html.match(new RegExp(implementation, "g")) || []).length, 1);
    assert.match(html, /same run lifecycle surface/i);
  }
  assert.equal((productionSources[0].match(/function renderRunSurface/g) || []).length, 1);
});

test("opaque requested strategy references survive encode/decode, navigation, and deep links", () => {
  const selectedStrategyPath = pathForState("strategies", "signals", strategyRef);
  const selectedStrategySegment = new URL(selectedStrategyPath, "http://localhost").pathname.split("/")[2];
  assert.equal(decodeURIComponent(selectedStrategySegment), strategyRef);
  const selectedRoute = resolveRoute(selectedStrategyPath);
  assert.equal(selectedRoute.strategyRef, strategyRef);
  assert.equal(pathForState("strategies", "signals", selectedRoute.strategyRef), selectedStrategyPath);

  const contextualRoutes = [
    contextualPath("/explore", strategyRef),
    contextualPath("/strategies", strategyRef),
    contextualPath("/validate", strategyRef),
    contextualPath("/validate/results", strategyRef),
    contextualPath("/operate", strategyRef),
    contextualPath("/operate/runs", strategyRef),
  ];
  for (const routePath of contextualRoutes) {
    const url = new URL(routePath, "http://localhost");
    const route = resolveRoute(url.pathname, url.search);
    assert.equal(route.strategyRef, strategyRef, routePath);
    const html = renderApp({ pathname: url.pathname, search: url.search });
    assert.match(html, /signed\/spec-v2:opaque\+42/);
    assert.match(html, /Producer integration pending/i, routePath);
    assertNoFalseProducerAvailabilityClaim(html, routePath);
    assertNoFalseStrategyAuthorityClaim(html, routePath);
  }

  const contextualRootHtml = renderApp({
    pathname: "/strategies",
    search: `?strategyRef=${encodeURIComponent(strategyRef)}`,
  });
  assert.match(contextualRootHtml, /data-route="\/strategies\?strategyRef=signed%2Fspec-v2%3Aopaque%2B42"/);
  assertNoFalseStrategyAuthorityClaim(contextualRootHtml);
  const contextualExploreHtml = renderApp({
    pathname: "/explore",
    search: `?strategyRef=${encodeURIComponent(strategyRef)}`,
  });
  assert.match(contextualExploreHtml, /data-route="\/explore\/catalog\?strategyRef=signed%2Fspec-v2%3Aopaque%2B42"/);
  assertNoFalseStrategyAuthorityClaim(contextualExploreHtml);

  const html = renderApp({ pathname: selectedStrategyPath, search: "" });
  assert.match(html, /signed\/spec-v2:opaque\+42/);
  assert.match(html, /data-requested-strategy-ref="signed\/spec-v2:opaque\+42"/);
  assert.match(html, /\/validate\/run\?strategyRef=signed%2Fspec-v2%3Aopaque%2B42/);
  assert.doesNotMatch(html, /display-name-as-execution-id/i);
  assertNoFalseStrategyAuthorityClaim(html);

  const runHtml = renderApp({
    pathname: "/validate/run",
    search: `?strategyRef=${encodeURIComponent(strategyRef)}`,
  });
  assert.match(runHtml, /data-requested-strategy-ref="signed\/spec-v2:opaque\+42"/);
  assertNoFalseStrategyAuthorityClaim(runHtml);
});

test("opaque strategy references are not normalized before routing", () => {
  const appSource = productionSources[0];
  assert.match(
    appSource,
    /new FormData\(form\)\.get\("strategyRef"\)\?\.toString\(\) \?\? ""/,
  );
  assert.doesNotMatch(
    appSource,
    /new FormData\(form\)\.get\("strategyRef"\)[\s\S]{0,120}\.trim\(/,
  );

  const path = pathForState("strategies", "overview", adversarialStrategyRef);
  const route = resolveRoute(path);
  assert.equal(route.strategyRef, adversarialStrategyRef);
  assert.equal(
    decodeURIComponent(new URL(path, "http://localhost").pathname.split("/")[2]),
    adversarialStrategyRef,
  );

  const validationPath = contextualPath("/validate/run", adversarialStrategyRef);
  const validationUrl = new URL(validationPath, "http://localhost");
  assert.equal(
    resolveRoute(validationUrl.pathname, validationUrl.search).strategyRef,
    adversarialStrategyRef,
  );
});

test("strategy child routes without an exact reference remain distinct and unavailable", () => {
  const route = resolveRoute("/strategies/candidates");
  assert.equal(route.stateKey, "strategies.candidates");
  assert.equal(route.identityOnly, true);
  const html = renderApp({ pathname: "/strategies/candidates", search: "" });
  assert.match(html, /data-identity-only="true"/);
  assert.match(html, /No requested strategy reference/);
  assert.match(html, /<h1>Candidates<\/h1>/);
  assert.doesNotMatch(html, /signed\/spec/);
  assertNoFalseStrategyAuthorityClaim(html);
});

test("pending status semantics remain distinct from unavailable", () => {
  for (const path of ["/cockpit", "/explore/catalog", "/validate/run", "/operate/runs"]) {
    const html = renderApp({ pathname: path, search: "" });
    assert.match(html, /status-badge status-pending/, path);
    assert.doesNotMatch(html, /status-badge status-unavailable/, path);
  }
  assert.match(
    productionSources[0],
    /function statusBadge\(label = "Status pending", tone = "pending"\)/,
  );
});

test("producer messaging distinguishes frontend integration pending from backend capability", () => {
  const html = renderApp({ pathname: "/explore/catalog", search: "" });
  assert.match(html, /Producer integration pending/i);
  assert.match(html, /not yet connected to (?:its )?authoritative backend producer|not yet connected to the catalog producer/i);
  assert.doesNotMatch(html, /backend capability unavailable/i);
  assert.doesNotMatch(html, /source-only project/i);
  assertNoFalseProducerAvailabilityClaim(html);

  const marketHtml = renderApp({ pathname: "/explore/market", search: "" });
  assert.match(marketHtml, /Market data not available to this frontend/i);
  assertNoFalseProducerAvailabilityClaim(marketHtml, "market data surface");

  const cockpitHtml = renderApp({ pathname: "/cockpit", search: "" });
  assert.match(cockpitHtml, /Runtime status pending/i);
  assert.doesNotMatch(cockpitHtml, /Runtime unavailable/i);
  assertNoFalseProducerAvailabilityClaim(cockpitHtml, "runtime surface");

  const productionText = productionSources.join("\n");
  assert.doesNotMatch(productionText, /No authoritative producer is connected/i);
  assert.doesNotMatch(productionText, /source-only project/i);
  assert.doesNotMatch(productionText, /Runtime unavailable/i);
  assert.doesNotMatch(productionText, /\bproducer unavailable\b/i);
});

test("producer availability adversary separates frontend gaps from producer claims", () => {
  const validFrontendAvailability = [
    "Market data not available to this frontend.",
    "Run state not available to this frontend.",
    "No authoritative result has been received.",
    "Producer integration pending.",
    "Runtime status pending.",
  ];
  const invalidProducerAvailability = [
    "Market data producer unavailable.",
    "Strategy custody producer unavailable.",
    "Run lifecycle producer offline.",
    "Validation result producer is unsupported.",
    "Runtime unavailable.",
    "Producer down.",
    "Producer disconnected.",
    "Provider unreachable.",
    "Runtime failed.",
    "Runtime stopped.",
    "Runtime unhealthy.",
    "Runtime missing.",
  ];
  const makesProducerAvailabilityClaim = (text) => producerAvailabilityClaimPatterns.some((pattern) => pattern.test(text));

  for (const text of validFrontendAvailability) {
    assert.equal(makesProducerAvailabilityClaim(text), false, text);
  }
  for (const text of invalidProducerAvailability) {
    assert.equal(makesProducerAvailabilityClaim(text), true, text);
  }
});

test("frontend remains unbound until the real backend integration contract arrives", () => {
  const frontendSource = productionSources.slice(0, 2).join("\n");
  assert.doesNotMatch(frontendSource, /\b(?:fetch|WebSocket|XMLHttpRequest)\s*\(/i);
  assert.doesNotMatch(frontendSource, /\/(?:api|v1)(?:\/|["'`])/i);
  assert.doesNotMatch(frontendSource, /\b(?:response\.json|strategyDTO|resultDTO|runStatus)\b/i);
});

test("the frontend has no hardcoded measurement or demo identity values", () => {
  const forbiddenValues = [
    "ESZ5",
    "VIX",
    "$24,000",
    "98%",
    "4/5",
    "Population 512",
    "Pareto Rank 1",
    "Sharpe 2.1",
    "Win rate 68%",
  ];
  const renderedSurfaces = [
    renderApp({ pathname: "/cockpit", search: "" }),
    renderApp({ pathname: "/explore/market", search: "" }),
    renderApp({ pathname: "/validate/results", search: "" }),
    renderApp({ pathname: "/operate/performance", search: "" }),
  ];
  for (const source of [...productionSources, ...renderedSurfaces]) {
    for (const value of forbiddenValues) {
      assert.doesNotMatch(source, new RegExp(escapeRegExp(value), "i"), value);
    }
  }
});

test("the browser regression contract keeps Apollo reconciliation persistent", () => {
  const appSource = productionSources[0];
  assert.match(appSource, /let persistentApollo = null/);
  assert.match(appSource, /nextApollo\.replaceWith\(persistentApollo\)/);
  assert.match(appSource, /root\.dataset\.apolloMountCount/);
  assert.match(appSource, /data-apollo-instance/);
});
