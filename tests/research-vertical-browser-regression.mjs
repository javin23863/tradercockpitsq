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
    title: document.title,
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

async function waitForRouteSettled(tab, route) {
  await waitUntil(tab, `${route} canonical workspace did not settle successfully`, async () => {
    const state = await pageState(tab);
    if (route === "/research?stage=construct&tab=idea") {
      const save = tab.playwright.locator('[data-idea-action="save"]');
      return (await save.count()) === 1 && !(await save.isDisabled());
    }
    if (route === "/research?stage=construct&tab=build") {
      return /No compiled configurations yet/i.test(state.text)
        && (await tab.playwright.locator("[data-research-build-workspace]").count()) === 1;
    }
    if (route === "/research?stage=construct&tab=candidates") {
      return (await tab.playwright.locator('[data-research-candidates-workspace="loaded"]').count()) === 1
        && /No imported native candidates/i.test(state.text);
    }
    if (route === "/research?stage=backtest&tab=overview") {
      return /No imported Candidates/i.test(state.text)
        && /No native historical result/i.test(state.text)
        && (await tab.playwright.locator('[data-retester-overview][data-route-selection-state="ready"]').count()) === 1;
    }
    if (route === "/research?stage=backtest&tab=trades") {
      return /No completed native Historical Result is available for Trades/i.test(state.text)
        && (await tab.playwright.locator('[data-research-trades][data-route-selection-state="ready"]').count()) === 1;
    }
    if (route === "/research?stage=backtest&tab=robustness") {
      return /No completed Historical Results/i.test(state.text)
        && !/Loading native robustness custody/i.test(state.text);
    }
    if (route === "/research?stage=backtest&tab=configuration") {
      return /No completed native Retester results/i.test(state.text)
        && /No executed chain selected/i.test(state.text)
        && (await tab.playwright.locator('[data-backtest-configuration-workspace][data-route-selection-state="ready"]').count()) === 1;
    }
    if (route === "/research?stage=proof") {
      return /No saved user-facing Proof records yet/i.test(state.text)
        && /No completed Historical Results/i.test(state.text)
        && /No matching completed Higher Precision run/i.test(state.text)
        && (await tab.playwright.locator("[data-research-proof-workspace] .idea-error").count()) === 0;
    }
    return false;
  }, 240);
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

function expectedWindowTitle(stage, researchTab) {
  const readable = (value) => String(value || "").replaceAll("-", " ").replace(/\b\w/g, (letter) => letter.toUpperCase());
  const parts = [readable(stage), readable(researchTab)].filter(Boolean);
  return `TraderCockpit — Research / ${parts.join(" / ")}`;
}

async function reviewRoute(tab, baseUrl, route, stage, researchTab, workspaceSelector) {
  await tab.goto(`${baseUrl}${route}`);
  await waitForRuntime(tab);
  if (workspaceSelector) await waitForWorkspace(tab, workspaceSelector, `${route} canonical workspace did not bind`);
  await waitForRouteSettled(tab, route);
  const state = await pageState(tab);
  assert.equal(state.pathname, "/research", `${route} remains in Research`);
  assert.equal(state.shell, "tradercockpit-desktop", `${route} uses canonical desktop shell`);
  assert.equal(state.surface, "research", `${route} is Research-owned`);
  assert.equal(state.stage, stage, `${route} stage`);
  assert.equal(state.tab, researchTab, `${route} tab`);
  assert.equal(state.title, expectedWindowTitle(stage, researchTab), `${route} exposes rendered shell identity in the window title`);
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
    '[data-specification-producer-validity="pending_native_validation"]',
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
  assert.equal(state.title, "TraderCockpit — Research / Construct / Specification");
  assert.match(state.text, /Local requirements complete/i);
  assert.match(state.text, /Native validation pending/i);
  assert.match(state.text, /authorized_sqx_loadconfig/i);
  assert.doesNotMatch(state.text, /Build requirements resolved/i);
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

  const archiveDigests = await tab.playwright.evaluate(() => {
    const values = [];
    const builder = document.querySelector('[data-research-capability="builder_native_specification"]');
    for (const row of builder?.querySelectorAll(".stat-row") || []) {
      if (row.querySelector("span")?.textContent?.trim() !== "Archive SHA-256") continue;
      const value = row.querySelector("code")?.textContent?.trim() || "";
      if (/^[0-9a-f]{64}$/i.test(value)) values.push(value);
    }
    return [...new Set(values)];
  });
  assert.equal(archiveDigests.length, 1, "all rendered native inspectors bind one exact Builder archive snapshot");

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
