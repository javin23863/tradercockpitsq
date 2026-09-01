import assert from "node:assert/strict";

const RESEARCH_ROUTES = Object.freeze([
  ["/research?stage=construct&tab=idea", "construct", "idea"],
  ["/research?stage=construct&tab=specification", "construct", "specification"],
  ["/research?stage=construct&tab=build", "construct", "build"],
  ["/research?stage=construct&tab=candidates", "construct", "candidates"],
  ["/research?stage=backtest&tab=overview", "backtest", "overview"],
  ["/research?stage=backtest&tab=trades", "backtest", "trades"],
  ["/research?stage=backtest&tab=robustness", "backtest", "robustness"],
  ["/research?stage=backtest&tab=configuration", "backtest", "configuration"],
  ["/research?stage=proof", "proof", ""],
]);

const STALE_PLACEHOLDERS = Object.freeze([
  "Native construct compiler not implemented",
  "Candidate custody not implemented",
  "Native historical result not loaded",
  "Pending backend mapping",
]);

async function pageState(tab) {
  return tab.playwright.evaluate(() => ({
    pathname: window.location.pathname,
    search: window.location.search,
    shell: document.querySelector("[data-product-shell]")?.getAttribute("data-product-shell") || "",
    surface: document.querySelector("[data-product-shell]")?.getAttribute("data-surface-id") || "",
    stage: document.querySelector("[data-product-shell]")?.getAttribute("data-research-stage-id") || "",
    tab: document.querySelector("[data-product-shell]")?.getAttribute("data-research-tab-id") || "",
    runtime: document.querySelector("[data-product-shell]")?.getAttribute("data-runtime-status") || "",
    text: document.body.innerText,
  }));
}

async function waitUntil(tab, description, predicate, attempts = 160) {
  for (let attempt = 0; attempt < attempts; attempt += 1) {
    if (await predicate()) return;
    await tab.playwright.waitForTimeout(25);
  }
  assert.fail(description);
}

async function waitForRuntime(tab) {
  await waitUntil(tab, "Research runtime status did not settle", async () => {
    const state = await pageState(tab);
    return state.runtime === "loaded" || state.runtime === "failed";
  });
}

async function waitForWorkspace(tab, selector, description) {
  await waitUntil(tab, description, async () => (await tab.playwright.locator(selector).count()) === 1);
}

function assertNoStaleResearchPlaceholders(text, route) {
  for (const stale of STALE_PLACEHOLDERS) {
    assert.equal(
      text.includes(stale),
      false,
      `${route} has no stale implementation placeholder: ${stale}`,
    );
  }
}

async function reviewRoute(tab, baseUrl, route, stage, researchTab, workspaceSelector) {
  await tab.goto(`${baseUrl}${route}`);
  await waitForRuntime(tab);
  if (workspaceSelector) await waitForWorkspace(tab, workspaceSelector, `${route} canonical workspace did not bind`);
  const state = await pageState(tab);
  assert.equal(state.pathname, "/research", `${route} remains in Research`);
  assert.equal(state.shell, "tradercockpit-desktop", `${route} uses canonical desktop shell`);
  assert.equal(state.surface, "research", `${route} is Research-owned`);
  assert.equal(state.stage, stage, `${route} stage`);
  assert.equal(state.tab, researchTab, `${route} tab`);
  assert.match(state.text, /Research/i, `${route} keeps Research context visible`);
  assert.doesNotMatch(state.text, /Apollo|donor|PR #/i, `${route} has no obsolete architecture authority`);
  assertNoStaleResearchPlaceholders(state.text, route);
}

async function reviewAllResearchRoutes(tab, baseUrl) {
  const workspaces = new Map([
    ["/research?stage=construct&tab=idea", "[data-research-idea-workspace]"],
    ["/research?stage=construct&tab=build", "[data-research-build-workspace]"],
    ["/research?stage=construct&tab=candidates", "[data-candidate-workspace-body]"],
    ["/research?stage=backtest&tab=overview", "[data-retester-overview]"],
    ["/research?stage=backtest&tab=trades", "[data-research-trades]"],
    ["/research?stage=backtest&tab=robustness", "[data-robustness-workspace]"],
    ["/research?stage=backtest&tab=configuration", "[data-backtest-configuration-workspace]"],
    ["/research?stage=proof", "[data-research-proof-workspace]"],
  ]);
  for (const [route, stage, researchTab] of RESEARCH_ROUTES) {
    if (researchTab === "specification") continue;
    await reviewRoute(tab, baseUrl, route, stage, researchTab, workspaces.get(route));
  }
}

async function waitForCoverage(tab) {
  await waitUntil(tab, "Research coverage authority did not settle", async () => {
    const workspace = tab.playwright.locator('[data-research-capability-coverage-workspace="ready"]');
    const cards = tab.playwright.locator("[data-research-capability]");
    return (await workspace.count()) === 1 && (await cards.count()) === 20;
  });
}

async function assertCoverageAccounting(tab) {
  await waitForCoverage(tab);
  assert.equal(await tab.playwright.locator('[data-research-capability-coverage="mapped"]').count(), 12);
  assert.equal(await tab.playwright.locator('[data-research-capability-coverage="explicitly_unavailable"]').count(), 8);
  assert.equal(await tab.playwright.locator('[data-research-capability-coverage="intentionally_hidden"]').count(), 0);
  const text = (await pageState(tab)).text;
  assert.match(text, /12 mapped · 8 explicitly unavailable · 0 intentionally hidden/i);
  for (const reason of [
    "typed_native_block_descriptor_and_write_seam_not_exposed",
    "native_search_parameter_descriptor_and_write_seam_not_exposed",
    "native_data_and_trading_write_seam_not_exposed",
    "native_money_management_descriptor_and_write_seam_not_exposed",
    "only_higher_precision_canonical_method_exposed",
    "authoritative_native_robustness_outcome_seam_not_exposed",
    "generic_native_task_control_seam_not_exposed",
    "authoritative_native_metric_readback_seam_not_exposed",
  ]) {
    assert.match(text, new RegExp(reason), `coverage exposes ${reason}`);
  }
  assert.doesNotMatch(text, /\bunmapped\b/i, "Research coverage has no silent unmapped state");
}

async function waitForNativeSpecificationDepth(tab) {
  const selectors = [
    "[data-native-search-configuration]",
    "[data-native-builder-trading-options]",
    "[data-native-builder-blocks]",
    "[data-native-builder-rankings]",
    "[data-native-builder-cross-checks]",
    "[data-native-builder-money-management]",
    "[data-research-native-inspector-tools]",
  ];
  await waitUntil(tab, "Complete producer-backed Specification depth did not bind", async () => {
    for (const selector of selectors) {
      if ((await tab.playwright.locator(selector).count()) !== 1) return false;
    }
    return true;
  }, 240);
}

async function reviewNativeSpecification(tab, specificationBaseUrl) {
  const route = "/research?stage=construct&tab=specification";
  await tab.goto(`${specificationBaseUrl}${route}`);
  await waitForRuntime(tab);
  await waitForCoverage(tab);
  await waitUntil(tab, "Specification requirements did not bind", async () => (
    (await tab.playwright.locator("[data-specification-requirement]").count()) > 0
  ));
  await waitForNativeSpecificationDepth(tab);
  let state = await pageState(tab);
  assert.equal(state.stage, "construct");
  assert.equal(state.tab, "specification");
  assert.match(state.text, /Build requirements resolved/i);
  assert.match(state.text, /Random Discovery/i);
  assert.match(state.text, /Genetic Evolution/i);
  assert.match(state.text, /Exact current SQX BuildTradingOptions structure/i);
  assert.match(state.text, /Exact current SQX Blocks structure/i);
  assert.match(state.text, /Exact current SQX Rankings structure/i);
  assert.match(state.text, /Exact current SQX CrossChecks structure/i);
  assert.match(state.text, /Exact current SQX MoneyManagement structure/i);
  assert.match(state.text, /Search exact current Builder structures/i);
  assertNoStaleResearchPlaceholders(state.text, route);
  await assertCoverageAccounting(tab);

  const nodes = tab.playwright.locator([
    "[data-native-search-node]",
    "[data-native-trading-options-node]",
    "[data-native-block-node]",
    "[data-native-ranking-node]",
    "[data-native-cross-check-node]",
    "[data-native-money-management-node]",
  ].join(","));
  const total = await nodes.count();
  assert.ok(total > 6, "multiple exact native structures are loaded together");
  const search = tab.playwright.locator("[data-native-inspector-search]");
  await search.fill("MaxStrategies");
  await waitUntil(tab, "native structure search did not filter", async () => {
    const result = await tab.playwright.evaluate(() => {
      const selector = [
        "[data-native-search-node]",
        "[data-native-trading-options-node]",
        "[data-native-block-node]",
        "[data-native-ranking-node]",
        "[data-native-cross-check-node]",
        "[data-native-money-management-node]",
      ].join(",");
      const cards = [...document.querySelectorAll(selector)];
      return {
        total: cards.length,
        visible: cards.filter((card) => !card.hidden).length,
        status: document.querySelector("[data-native-inspector-search-status]")?.textContent || "",
      };
    });
    return result.total > 1 && result.visible > 0 && result.visible < result.total && /Showing \d+ of \d+ exact native nodes/.test(result.status);
  });
  await search.fill("");
  await waitUntil(tab, "native structure search did not clear", async () => {
    const hidden = await tab.playwright.evaluate(() => document.querySelectorAll('[data-native-search-node][hidden], [data-native-trading-options-node][hidden], [data-native-block-node][hidden], [data-native-ranking-node][hidden], [data-native-cross-check-node][hidden], [data-native-money-management-node][hidden]').length);
    return hidden === 0;
  });
}

async function reviewCoverageWithoutSqx(tab, baseUrl) {
  await tab.goto(`${baseUrl}/research?stage=construct&tab=specification`);
  await waitForRuntime(tab);
  await assertCoverageAccounting(tab);
  await waitUntil(tab, "Unavailable native Specification did not fail visibly", async () => /Native Specification unavailable|Build locked/i.test((await pageState(tab)).text));
  const text = (await pageState(tab)).text;
  assert.match(text, /Research vertical coverage/i);
  assert.match(text, /typed_native_block_descriptor_and_write_seam_not_exposed/);
  assertNoStaleResearchPlaceholders(text, "/research?stage=construct&tab=specification (no SQX)");
}

export async function runResearchVerticalBrowserReview(tab, { baseUrl, specificationBaseUrl = baseUrl }) {
  await reviewAllResearchRoutes(tab, baseUrl);
  await reviewNativeSpecification(tab, specificationBaseUrl);
  await reviewCoverageWithoutSqx(tab, baseUrl);
  return {
    routes: RESEARCH_ROUTES.map(([route]) => route),
    mappedCapabilities: 12,
    explicitlyUnavailableCapabilities: 8,
  };
}
