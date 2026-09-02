import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import { attentionCount, fetchMarketQuotes, fetchRuntimeStatus, lastRunSummary, renderApp } from "../web/app.mjs";
import {
  APP_SURFACES,
  HOME_ZONE_IDS,
  HOME_ZONES,
  PRODUCT_ROUTE_PATHS,
  RESEARCH_WORKSPACE_IDS,
  RESEARCH_WORKSPACES,
  researchLocationMatches,
  researchNavPath,
  researchPath,
  resolveRoute,
} from "../web/model.mjs";
import { EMPTY_RESEARCH_SNAPSHOT } from "../web/research-snapshot.mjs";
import { VALIDATION_STAGES, renderValidateOverview } from "../web/research-validate.mjs";
import { assistantState, renderAssistantWidget } from "../web/assistant.mjs";
import { stageTally, verdictTally } from "../web/research-verdicts.mjs";

const runtimePayload = Object.freeze({
  schema: "tc.runtime-status.v1",
  application: { status: "ready", server: "canonical", desktop: "canonical-server-ui" },
  research_backend: {
    status: "ready",
    configured: true,
    verified: true,
    producer: "strategyquant-x",
    build: "144.2953",
    reason_code: null,
    inspection: { available: true, reason_code: null },
    execution: { available: false, reason_code: "trusted_native_gateway_not_implemented", launcher_sha256: null },
    runtime: { build: { expected: "144.2953", observed: "144.2953", verified: true, status: "ready" }, launcher: { status: "unavailable", relative_path: "sqcli.exe", verified: false, reason_code: "trusted_launcher_not_configured" }, inspection: { available: true }, execution: { available: false, reason_code: "trusted_native_gateway_not_implemented" } },
  },
  research_custody: { status: "ready", reason_code: null, contract: { record_kinds: ["idea", "configuration"], identity_schema: "tc.research-entity-id.v1", revision_schema: "tc.research-revision.v1", evidence_schema: "tc.evidence-ref.sha256.v1", current_update: "compare-and-set" } },
  market_data: { status: "unavailable", reason_code: "producer_not_configured" },
  provider: { status: "unavailable", reason_code: "provider_not_configured", provider: "openrouter", transport: "openai-compatible-chat", credential_scope: "operator", detail: "Set OPENROUTER_API_KEY in the operator environment to enable the assistant transport." },
  account: { status: "unavailable", reason_code: "authority_not_implemented" },
  macro_series: { schema: "tc.macro-series.v1", status: "unavailable", reason_code: "provider_not_configured", provider: null, provider_hookup: { credential_env: "FRED_API_KEY", series_env: "TRADERCOCKPIT_FRED_SERIES", detail: "FRED observations for operator-configured series ids." }, series: [] },
  model: { status: "unavailable", reason_code: "provider_not_configured", default_model: "z-ai/glm-5.3-flash", fallback_models: [], policy_source: "backend" },
  assistant: { schema: "tc.assistant-status.v1", identity: "Apollo", status: "unavailable", reason_code: "provider_not_configured", provider: "openrouter", model: "z-ai/glm-5.3-flash", fallback_models: [], detail: "Set OPENROUTER_API_KEY in the operator environment to enable the assistant transport.", knowledge: { library: "quant-guild", status: "unavailable", reason_code: "knowledge_corpus_unavailable", document_count: 0 } },
  extensions: { status: "unavailable", reason_code: "manifest_not_implemented" },
});
const readyAssistantRuntime = Object.freeze({
  ...runtimePayload,
  provider: { status: "ready", reason_code: null, provider: "openrouter", transport: "openai-compatible-chat", credential_scope: "operator", detail: "Assistant ready on OpenRouter with backend model policy (z-ai/glm-5.3-flash)." },
  model: { status: "ready", reason_code: null, default_model: "z-ai/glm-5.3-flash", fallback_models: [], policy_source: "backend" },
  assistant: { schema: "tc.assistant-status.v1", identity: "Apollo", status: "ready", reason_code: null, provider: "openrouter", model: "z-ai/glm-5.3-flash", fallback_models: [], detail: "Assistant ready on OpenRouter with backend model policy (z-ai/glm-5.3-flash).", knowledge: { library: "quant-guild", status: "ready", document_count: 12 } },
});
const loadedRuntimeState = Object.freeze({ phase: "loaded", payload: runtimePayload, detail: "" });

const unavailableQuotes = Object.freeze({
  schema: "tc.market-quotes.v1",
  scope: "live_current",
  historical_fallback: false,
  status: "unavailable",
  reason_code: "provider_not_configured",
  detail: "No live market-data provider is connected.",
  provider: null,
  provider_hookup: {
    interface: "tradercockpit.market_data.MarketDataProvider.fetch_quotes",
    watchlist_env: "TRADERCOCKPIT_WATCHLIST",
    credential_env: ["SCHWAB_CLIENT_ID", "SCHWAB_CLIENT_SECRET", "SCHWAB_REFRESH_TOKEN", "TRADERCOCKPIT_MARKET_API_KEY"],
    authorize_path: "/api/market/schwab/authorize",
    historical_fx_indices: { producer: "strategyquant_x", source: "dukascopy", pipeline: "native", detail: "Forex and indices history stays in StrategyQuant X Data Manager." },
    detail: "Connect a provider.",
  },
  watchlist: [
    { symbol: "ESM5", status: "unavailable", last: null, change_percent: null, currency: null, observed_at: null },
    { symbol: "NQM5", status: "unavailable", last: null, change_percent: null, currency: null, observed_at: null },
  ],
  quotes: [],
});
const liveQuotes = Object.freeze({
  ...unavailableQuotes,
  status: "current",
  reason_code: null,
  provider: { id: "example-live-feed" },
  watchlist: [{ symbol: "ESM5", status: "current", last: 5308.25, change_percent: 0.48, currency: "USD", observed_at: "2026-09-02T09:41:23Z" }],
  quotes: [{ symbol: "ESM5", status: "current", last: 5308.25, change_percent: 0.48, currency: "USD", observed_at: "2026-09-02T09:41:23Z" }],
});
const unavailableMarketState = Object.freeze({ phase: "loaded", payload: unavailableQuotes, detail: "" });
const liveMarketState = Object.freeze({ phase: "loaded", payload: liveQuotes, detail: "" });

const emptyLoadedSnapshot = Object.freeze({ ...EMPTY_RESEARCH_SNAPSHOT, phase: "loaded" });
const loadedSnapshot = Object.freeze({
  ...EMPTY_RESEARCH_SNAPSHOT,
  phase: "loaded",
  ideas: Object.freeze([{ entity_id: "tc-research:idea:v1:11111111-1111-4111-8111-111111111111", revision: "tc-research-revision:idea:sha256:aaaa", summary: "Opening range breakout" }]),
  results: Object.freeze([{ entity_id: "tc-research:historical-result:v1:22222222-2222-4222-8222-222222222222", revision: "tc-research-revision:historical-result:sha256:bbbb", state: "completed", candidate_revision: "tc-research-revision:candidate:sha256:cccc", native_project_name: "TraderCockpit-Retester-0123", retester_task: 1, validation_state: "not_run" }]),
});

function render(route, { status = loadedRuntimeState, market = unavailableMarketState, snapshot = EMPTY_RESEARCH_SNAPSHOT } = {}) {
  return renderApp(route, status, { phase: "idle", catalog: [], selected: null, detail: "" }, market, snapshot);
}

test("six top-level surfaces, in prototype order", () => {
  assert.deepEqual(APP_SURFACES.map((surface) => surface.id), ["home", "research", "explore", "automation", "operate", "settings"]);
  assert.deepEqual(PRODUCT_ROUTE_PATHS, ["/home", "/research", "/explore", "/automation", "/operate", "/settings"]);
});

test("Research is composed of the four prototype workspaces with their exact tab rows", () => {
  assert.deepEqual(RESEARCH_WORKSPACE_IDS, ["signals", "evolution", "validate", "catalog"]);
  const tabs = Object.fromEntries(RESEARCH_WORKSPACES.map((workspace) => [workspace.id, workspace.tabs.map((tab) => tab.label)]));
  assert.deepEqual(tabs.signals, ["Overview", "Signals & Models", "Order Flow", "Footprint", "Volume Profile", "Liquidity Map", "Replays", "Alerts", "Reports"]);
  assert.deepEqual(tabs.evolution, []);
  assert.deepEqual(tabs.validate, ["Overview", "Initial Test", "Trades", "Robustness", "Configuration", "Evidence"]);
  assert.deepEqual(tabs.catalog, ["All Components", "Indicators", "Models", "Strategies", "Utilities", "My Components"]);
  assert.deepEqual(RESEARCH_WORKSPACES.map((workspace) => workspace.screen), [
    "order-flow-signals-models",
    "evolutionary_search_trading_dashboard",
    "test-validate-dashboard",
    "indicators-models-catalog",
  ]);
});

test("Cockpit Home board preserves the eight numbered prototype cards", () => {
  assert.deepEqual(HOME_ZONE_IDS, ["research", "build-backtest", "prop-simulation", "proof-evidence", "active-builds", "candidate-review", "system-health", "assistant"]);
  assert.deepEqual(HOME_ZONES.map((zone) => zone.number), [1, 2, 3, 4, 5, 6, 7, 8]);
});

test("routes select only registered states; legacy stage/tab links canonicalise", () => {
  assert.deepEqual(resolveRoute("/"), { kind: "redirect", redirectPath: "/home", path: "/" });
  const signals = resolveRoute("/research", "?workspace=signals&tab=order-flow");
  assert.equal(signals.kind, "research");
  assert.equal(signals.workspaceId, "signals");
  assert.equal(signals.tabId, "order-flow");
  assert.equal(signals.legacy, false);

  const evolution = resolveRoute("/research", "?workspace=evolution");
  assert.equal(evolution.tabId, null);

  const legacyBuild = resolveRoute("/research", "?stage=construct&tab=build&configuration=tc-research:configuration:v1:abc");
  assert.equal(legacyBuild.workspaceId, "evolution");
  assert.equal(legacyBuild.legacy, true);
  assert.equal(legacyBuild.canonicalPath, "/research?workspace=evolution&configuration=tc-research%3Aconfiguration%3Av1%3Aabc");
  assert.equal(resolveRoute("/research", "?stage=construct&tab=idea").canonicalPath, "/research?workspace=signals&tab=overview");
  assert.equal(resolveRoute("/research", "?stage=construct&tab=specification").tabId, "signals");
  assert.equal(resolveRoute("/research", "?stage=backtest&tab=trades").tabId, "trades");
  assert.equal(resolveRoute("/research", "?stage=proof&proofEntity=x").canonicalPath, "/research?workspace=validate&tab=evidence&proofEntity=x");
  assert.equal(resolveRoute("/research", "?workspace=nonsense&tab=made-up").workspaceId, "signals");
  assert.equal(resolveRoute("/research", "?workspace=validate&tab=made-up").tabId, "overview");

  for (const legacyPath of ["/strategyquant", "/construct/build", "/backtest/trades", "/proof"]) {
    const route = resolveRoute(legacyPath);
    assert.equal(route.surfaceId, "home");
    assert.equal(route.unknownPath, legacyPath);
  }

  assert.equal(researchLocationMatches({ pathname: "/research", search: "?workspace=validate&tab=robustness" }, "validate", "robustness"), true);
  assert.equal(researchLocationMatches({ pathname: "/research", search: "?stage=backtest&tab=robustness" }, "validate", "robustness"), true);
  assert.equal(researchLocationMatches({ pathname: "/research", search: "?workspace=evolution" }, "evolution"), true);
  assert.equal(researchLocationMatches({ pathname: "/home", search: "" }, "evolution"), false);
  assert.equal(researchPath("catalog", "models"), "/research?workspace=catalog&tab=models");
});

test("Research chrome keeps custody identities when switching workspace or tab", () => {
  const validationRef = `tc-evidence:sha256:${"ab".repeat(32)}`;
  const search = `?workspace=evolution&configuration=tc-research%3Aconfiguration%3Av1%3Aabc&proofEntity=tc-research%3Aproof%3Av1%3Ax&validationRef=${encodeURIComponent(validationRef)}`;
  const hopped = researchPath("validate", "trades", search);
  const params = new URLSearchParams(hopped.split("?")[1]);
  assert.equal(params.get("workspace"), "validate");
  assert.equal(params.get("tab"), "trades");
  assert.equal(params.get("configuration"), "tc-research:configuration:v1:abc");
  assert.equal(params.get("proofEntity"), "tc-research:proof:v1:x");
  assert.equal(params.get("validationRef"), validationRef);
  assert.equal(params.has("stage"), false);

  const reopened = resolveRoute("/research", search);
  assert.equal(reopened.workspaceId, "evolution");
  assert.equal(reopened.canonicalPath.includes("configuration=tc-research%3Aconfiguration%3Av1%3Aabc"), true);
  assert.equal(reopened.canonicalPath.includes("proofEntity=tc-research%3Aproof%3Av1%3Ax"), true);
  assert.equal(reopened.canonicalPath.includes(`validationRef=${encodeURIComponent(validationRef)}`), true);

  const homeStart = render(resolveRoute("/home"));
  assert.match(homeStart, /href="\/research\?workspace=signals&amp;tab=overview"/);
  assert.equal(homeStart.includes("configuration="), false);

  const previous = globalThis.location;
  globalThis.location = { search, pathname: "/research" };
  try {
    const nav = new URLSearchParams(researchNavPath("signals", "overview").split("?")[1]);
    assert.equal(nav.get("workspace"), "signals");
    assert.equal(nav.get("tab"), "overview");
    assert.equal(nav.get("configuration"), "tc-research:configuration:v1:abc");
    const chrome = render(reopened);
    assert.match(chrome, /href="\/research\?workspace=validate&amp;tab=overview&amp;configuration=tc-research%3Aconfiguration%3Av1%3Aabc/);
  } finally {
    if (previous === undefined) delete globalThis.location;
    else globalThis.location = previous;
  }
});

test("global chrome: rail, top chips, market ticker and status bar read only backend state", () => {
  const home = render(resolveRoute("/home"), { snapshot: emptyLoadedSnapshot });
  assert.match(home, /data-product-shell="tradercockpit-desktop"/);
  for (const surface of APP_SURFACES) assert.match(home, new RegExp(`data-route="${surface.path}"[^>]*aria-current|href="${surface.path}"`));
  for (const chipKey of ["data-feeds", "broker", "compute", "automation"]) assert.match(home, new RegExp(`data-chip="${chipKey}"`));
  assert.match(home, /data-chip="compute" data-tone="ready"/);
  assert.match(home, /data-chip="broker" data-tone="unavailable"/);
  assert.match(home, /data-market-ticker="unavailable"/);
  assert.match(home, /data-quote-symbol="ESM5"/);
  assert.match(home, /data-quote-symbol="NQM5"/);
  assert.match(home, /data-market-context/);
  assert.match(home, /Live Runs/);
  assert.match(home, /Positions/);
  assert.match(home, /Daily P&amp;L/);
  assert.match(home, /Buying Power/);
  assert.match(home, /Drawdown/);
  assert.match(home, /Last Run:/);
  assert.match(home, /No native run recorded/);
  assert.equal(attentionCount(runtimePayload), 6);
  assert.doesNotMatch(home, /\$\s?\d/);
  assert.doesNotMatch(home, /\d+\.\d+%/);
});

test("market ticker shows values only from a current provider record", () => {
  const unavailable = render(resolveRoute("/home"));
  assert.match(unavailable, /data-quote-symbol="ESM5" data-quote-status="unavailable"/);
  assert.doesNotMatch(unavailable, /5,308\.25/);

  const live = render(resolveRoute("/home"), { market: liveMarketState });
  assert.match(live, /data-market-ticker="live"/);
  assert.match(live, /data-quote-symbol="ESM5" data-quote-status="current" data-quote-tone="up"/);
  assert.match(live, /5,308\.25/);
  assert.match(live, /\+0\.48%/);
  assert.match(live, /data-chip="data-feeds" data-tone="ready"/);

  const failed = render(resolveRoute("/home"), { market: { phase: "failed", payload: null, detail: "boom" } });
  assert.match(failed, /Quotes read failed/);
});

test("Cockpit Home renders the prototype board from custody and status read models", () => {
  const home = render(resolveRoute("/home"), { snapshot: loadedSnapshot });
  assert.match(home, /Cockpit Home/);
  assert.match(home, /Turn Research into/);
  assert.match(home, /Decisions that Compound\./);
  assert.match(home, /New Research/);
  assert.match(home, /Build Strategy/);
  assert.match(home, /Recent Activity/);
  assert.match(home, /Native Retester completed/);
  assert.match(home, /Idea saved/);
  for (const zone of HOME_ZONE_IDS) assert.match(home, new RegExp(`data-home-zone="${zone}"`));
  const order = HOME_ZONE_IDS.map((zone) => home.indexOf(`data-home-zone="${zone}"`));
  assert.deepEqual([...order].sort((a, b) => a - b), order, "cards keep prototype order");
  assert.match(home, /Opening range breakout/);
  assert.match(home, /TraderCockpit-Retester-0123/);
  assert.match(home, /No simulation account/);
  assert.match(home, /Not graded/);
  assert.match(home, /System Health/);
  assert.match(home, /data-runtime-component="research-backend" data-runtime-state="ready"/);
  assert.match(home, /Ready · StrategyQuant X 144\.2953/);
  assert.match(home, /Disabled · Trusted Native Gateway Not Implemented/);
  assert.match(home, /Unavailable · Producer Not Configured/);
  assert.match(home, /TraderCockpit application/);
  assert.match(home, /Consumer account/);
  assert.match(home, /Model access/);
  assert.match(home, /Knowledge library/);
  assert.match(home, /Extensions/);
  assert.match(home, /data-assistant-widget data-assistant-ready="false"/);
  assert.match(home, /Assistant transport is not configured on this desktop/);
  assert.match(home, /data-assistant-form/);
  assert.match(home, /<button[^>]*data-assistant-ask/);
  assert.doesNotMatch(home, /<button[^>]*disabled[^>]*data-assistant-ask/, "the assistant is never disabled");
  assert.doesNotMatch(home, /assistant is not connected yet/i);
  assert.doesNotMatch(home, /Champion/);
  assert.doesNotMatch(home, /Pass<\/span>/);
  assert.doesNotMatch(home, /\$\s?\d/);
});

test("assistant widget is functional and truthful in every provider state", () => {
  const unconfigured = assistantState(runtimePayload);
  assert.equal(unconfigured.ready, false);
  assert.match(unconfigured.modelLabel, /z-ai\/glm-5\.3-flash · Provider Not Configured/);
  const ready = assistantState(readyAssistantRuntime);
  assert.equal(ready.ready, true);
  assert.equal(ready.modelLabel, "z-ai/glm-5.3-flash via openrouter");

  const widget = renderAssistantWidget(readyAssistantRuntime);
  assert.match(widget, /data-assistant-ready="true"/);
  assert.match(widget, /Good day, Trader\./);
  assert.match(widget, /Model policy: z-ai\/glm-5\.3-flash via openrouter/);
  assert.match(widget, /Knowledge library: Quant-Guild · 12 excerpts/);
  assert.match(widget, /<form class="assistant-form" data-assistant-form/);
  assert.match(widget, /<input type="text" name="message" maxlength="4000"/);
  assert.doesNotMatch(widget, /disabled/);
  assert.doesNotMatch(renderAssistantWidget(runtimePayload), /disabled/);
  assert.doesNotMatch(renderAssistantWidget(null), /disabled/);
  assert.match(renderAssistantWidget(null), /Connecting to the assistant backend/);
});

test("Home before status/custody load keeps explicit pending states and the Home shell", () => {
  const home = renderApp(resolveRoute("/home"));
  assert.match(home, /data-runtime-status="loading"/);
  assert.match(home, /data-custody-status="loading"/);
  assert.match(home, /Checking runtime status/);
  assert.match(home, /Reading custody…/);
  assert.doesNotMatch(home, /Application ready/);
  assert.doesNotMatch(home, />StrategyQuant X</);
});

test("runtime status and market quotes fetches accept only their canonical schemas", async () => {
  const payload = await fetchRuntimeStatus(async (path, options) => {
    assert.equal(path, "/api/status");
    assert.equal(options.headers.accept, "application/json");
    return { ok: true, status: 200, json: async () => runtimePayload };
  });
  assert.equal(payload, runtimePayload);
  await assert.rejects(() => fetchRuntimeStatus(async () => ({ ok: true, status: 200, json: async () => ({ schema: "wrong.v1" }) })), /schema mismatch/);
  await assert.rejects(() => fetchRuntimeStatus(async () => ({ ok: false, status: 503, json: async () => ({}) })), /request failed: 503/);

  const quotes = await fetchMarketQuotes(async (path) => {
    assert.equal(path, "/api/market/quotes");
    return { ok: true, status: 200, json: async () => unavailableQuotes };
  });
  assert.equal(quotes, unavailableQuotes);
  await assert.rejects(() => fetchMarketQuotes(async () => ({ ok: true, status: 200, json: async () => ({ schema: "wrong" }) })), /schema mismatch/);
});

test("status bar last-run summary is custody, never a verdict", () => {
  assert.deepEqual(lastRunSummary(EMPTY_RESEARCH_SNAPSHOT), { label: "Reading custody…", tone: "pending", state: "pending" });
  assert.equal(lastRunSummary({ ...EMPTY_RESEARCH_SNAPSHOT, phase: "loaded" }).label, "No native run recorded");
  const summary = lastRunSummary(loadedSnapshot);
  assert.match(summary.label, /Native Retester/);
  assert.equal(summary.state, "completed");
});

test("Signals & Models workspace renders all nine tabs, the chart frame and the native specification host", () => {
  const overview = render(resolveRoute("/research", "?workspace=signals&tab=overview"));
  assert.match(overview, /data-surface-id="research"/);
  assert.match(overview, /data-workspace-id="signals" data-tab-id="overview"/);
  assert.match(overview, /Order Flow Signals &amp; Models/);
  for (const label of ["Overview", "Signals &amp; Models", "Order Flow", "Footprint", "Volume Profile", "Liquidity Map", "Replays", "Alerts", "Reports"]) {
    assert.match(overview, new RegExp(`>${label}<`), label);
  }
  for (const label of ["Evolutionary Search", "Test &amp; Validate", "Indicators &amp; Models"]) assert.match(overview, new RegExp(`>${label}<`));
  assert.match(overview, /data-research-idea-workspace/);
  assert.match(overview, /Saving does not create a candidate, run native compute, or infer trading semantics/);

  const signals = render(resolveRoute("/research", "?workspace=signals&tab=signals"));
  assert.match(signals, /data-chart-card/);
  assert.match(signals, /data-chart-state="unavailable"/);
  assert.match(signals, /Native Strategy Specification/);
  assert.match(signals, /class="requirement-grid" data-research-specification-grid/);
  assert.match(signals, /Strategy Panel/);
  assert.match(signals, /Signal Pulse/);
  assert.match(signals, /Active Models/);
  assert.match(signals, /Confluence/);
  assert.match(signals, /Market State/);
  assert.match(signals, /Session Context/);
  assert.match(signals, /Risk Overlay/);
  assert.doesNotMatch(signals, /Strong Bullish/);

  const orderFlow = render(resolveRoute("/research", "?workspace=signals&tab=order-flow"));
  assert.match(orderFlow, /tick-level market-data provider/);
});

test("Evolutionary Search renders the prototype strip, cards and custody hosts", () => {
  const evolution = render(resolveRoute("/research", "?workspace=evolution"), { snapshot: emptyLoadedSnapshot });
  assert.match(evolution, /data-workspace-id="evolution"/);
  for (const label of ["State", "Objective Set", "Optimization", "Search Mode", "Deterministic Seed", "Budget", "Time Elapsed"]) assert.match(evolution, new RegExp(`>${label}<`));
  for (const title of ["Search Configuration", "Population", "Generations", "Pareto Frontier", "Variation Operators", "Fitness Evolution", "Islands Overview", "Archive &amp; Objectives", "Top Candidates"]) {
    assert.match(evolution, new RegExp(`>${title}<`), title);
  }
  assert.match(evolution, /data-research-host="build"/);
  assert.match(evolution, /data-research-host="candidates"/);
  assert.match(evolution, /data-evolution-strip/);
  assert.match(evolution, /Pause/);
  assert.match(evolution, /Stop/);
  assert.match(evolution, /No native job/);
});

test("Test & Validate renders KPIs, the seven-stage funnel, run table, conclusions and tool hosts", () => {
  const overview = render(resolveRoute("/research", "?workspace=validate&tab=overview"), { snapshot: loadedSnapshot });
  assert.deepEqual(VALIDATION_STAGES.map((stage) => stage.label), ["Initial Test", "Fast Validation", "Golden Validation", "Scenario Tests", "Stress Tests", "Out-of-Sample", "Evidence"]);
  for (const label of ["Total Runs", "Pass Rate", "Avg. Ret/DD", "Out-of-Sample PF", "Max Drawdown", "Expectancy", "Profit Factor"]) assert.match(overview, new RegExp(label.replace(/[()/]/g, "\\$&")));
  for (const stage of VALIDATION_STAGES) assert.match(overview, new RegExp(`data-validation-stage="${stage.id}"`));
  // A completed native result exists, so every verdict block reports "computing" until the
  // binder reads the cockpit verdict; nothing is inferred client-side.
  assert.match(overview, /data-validate-overview data-verdict-state="loading"/);
  assert.match(overview, /data-funnel-stage="initial-test" data-funnel-state="loading" data-funnel-source="native_condition"/);
  assert.match(overview, /data-funnel-stage="stress-tests" data-funnel-state="loading" data-funnel-source="cockpit_policy"/);
  assert.match(overview, /Computing cockpit verdicts/);
  assert.match(overview, /Run &amp; Evidence Table/);
  assert.match(overview, /TraderCockpit-Retester-0123/);
  assert.match(overview, /Validation Conclusions/);
  assert.match(overview, /Next Actions/);
  assert.match(overview, /Deploy to Paper/);
  assert.doesNotMatch(overview, /Robust &amp; Deployable/);
  assert.doesNotMatch(overview, /\d+\.\d+%/);

  const empty = render(resolveRoute("/research", "?workspace=validate&tab=overview"), { snapshot: emptyLoadedSnapshot });
  assert.match(empty, /data-validate-overview data-verdict-state="empty"/);
  assert.match(empty, /No verdict yet/);
  assert.match(empty, /No completed native result to judge yet/);
  assert.match(empty, /data-funnel-stage="out-of-sample" data-funnel-state="empty"/);

  const hosts = { "initial-test": "retester", trades: "trades", robustness: "robustness", configuration: "configuration", evidence: "proof" };
  for (const [tab, host] of Object.entries(hosts)) {
    const html = render(resolveRoute("/research", `?workspace=validate&tab=${tab}`));
    assert.match(html, new RegExp(`data-research-host="${host}"`), tab);
    assert.match(html, /class="empty-state/);
  }
});

test("Test & Validate renders the cockpit verdict once the backend read model arrives", () => {
  const stages = [
    ["initial-test", "pass", "native_condition", [{ label: "ProfitFactor (in-sample) > 1.3", column: "ProfitFactor", sample: "in-sample", comparator: ">", threshold: 1.3, value: 2.41, state: "pass", source: "native_condition" }]],
    ["fast-validation", "pass", "native_condition", [{ label: "ProfitFactor (full sample) > 1.3", column: "ProfitFactor", sample: "full sample", comparator: ">", threshold: 1.3, value: 2.2, state: "pass", source: "native_condition" }]],
    ["golden-validation", "pass", "cockpit_policy", [{ label: "Profitable calendar years", comparator: ">=", threshold: 60, value: 80, unit: "%", state: "pass", source: "cockpit_policy" }]],
    ["scenario-tests", "pass", "cockpit_policy", [{ label: "Profitable calendar quarters", comparator: ">=", threshold: 50, value: 75, unit: "%", state: "pass", source: "cockpit_policy" }]],
    ["stress-tests", "fail", "cockpit_policy", [{ label: "Monte Carlo drawdown (95th percentile)", comparator: "<=", threshold: 900, value: 1240, state: "fail", source: "cockpit_policy" }]],
    ["out-of-sample", "not_run", "cockpit_policy", []],
    ["evidence", "not_run", "custody", []],
  ].map(([id, state, source, checks]) => ({ id, state, source, basis: "historical_result", detail: `${id} detail`, checks, checks_passed: checks.filter((check) => check.state === "pass").length, checks_total: checks.length }));
  const statistics = { NumberOfTrades: 120, NetProfit: 4820.5, ProfitFactor: 2.41, Drawdown: 612.2, ReturnDDRatio: 7.87, Expectancy: 40.17, WinningPct: 58, first_open_time: 1600000000000, last_close_time: 1700000000000, initial_capital: 10000 };
  const verdict = {
    schema: "tc.research-cockpit-verdict.v1",
    authority: "tradercockpit",
    policy: { source: "default", values: {} },
    initial_capital: 10000,
    initial_capital_source: "native_money_management",
    native_conditions: { state: "available" },
    statistics: { full: statistics, in_sample: statistics, out_of_sample: null, higher_precision: statistics },
    equity: [{ time: 1600000000000, balance: 10000 }, { time: 1650000000000, balance: 12000 }, { time: 1700000000000, balance: 14820.5 }],
    stages,
    verdict: { state: "fail", label: "Rejected", stages_passed: 4, stages_total: 7 },
  };
  const entries = [{ result: loadedSnapshot.results[0], verdict, state: "available", reason: null }];
  assert.deepEqual(stageTally(entries, "stress-tests"), { pass: 0, fail: 1, incomplete: 0, not_run: 0, total: 1 });
  assert.deepEqual(verdictTally(entries), { pass: 0, fail: 1, incomplete: 0, in_progress: 0, total: 1 });

  const html = renderValidateOverview(loadedSnapshot, { entries, flags: { RetestWithHigherPrecision: true, MonteCarloManipulation: false } });
  assert.match(html, /data-validate-overview data-verdict-state="computed"/);
  assert.match(html, /data-funnel-stage="initial-test" data-funnel-state="pass"/);
  assert.match(html, /data-funnel-stage="stress-tests" data-funnel-state="fail"/);
  assert.match(html, /data-funnel-stage="out-of-sample" data-funnel-state="not_run"/);
  assert.match(html, /1 fail · native method off/);
  assert.match(html, /data-validation-stage="stress-tests" data-stage-state="fail" data-stage-source="cockpit_policy"/);
  assert.match(html, /data-validation-stage="golden-validation" data-stage-state="pass"/);
  assert.match(html, /class="check-dot is-fail"/);
  assert.match(html, /data-validate-conclusions="fail"/);
  assert.match(html, /data-verdict-label>Rejected</);
  assert.match(html, /Statistical Robustness<\/span><strong class="tone-text-red">Weak/);
  assert.match(html, /Regime Resilience<\/span><strong class="tone-text-green">Consistent/);
  assert.match(html, /data-run-verdict="fail"/);
  assert.match(html, /4,820\.50/);
  assert.match(html, /data-validate-performance="historical"/);
  assert.match(html, /<path class="tone-purple" d="M0\.00/);
  assert.match(html, /data-kpi="pass-rate"[^>]*>[\s\S]*?0%/);
  assert.match(html, /StrategyQuant X produced the trades; the cockpit computes the verdict/);
});

test("Indicators & Models catalog renders the prototype pills, filters and Models modality state", () => {
  const all = render(resolveRoute("/research", "?workspace=catalog&tab=all"));
  for (const label of ["All Components", "Indicators", "Models", "Strategies", "Utilities", "My Components"]) assert.match(all, new RegExp(`>${label}`));
  assert.match(all, /data-catalog-search/);
  assert.match(all, /data-catalog-root data-catalog-tab="all"/);
  assert.match(all, /Publish Component/);
  const models = render(resolveRoute("/research", "?workspace=catalog&tab=models"));
  assert.match(models, /data-ml-models/);
  assert.match(models, /Checking Models backend/);
  const utilities = render(resolveRoute("/research", "?workspace=catalog&tab=utilities"));
  assert.match(utilities, /data-research-capability="native_custom_project_topology"/);
  assert.match(utilities, /data-research-capability="native_preset_inspection"/);
});

test("Explore, Automation, Operate and Settings use the same grammar with truthful states", () => {
  const explore = render(resolveRoute("/explore"));
  assert.match(explore, /Native research producer/);
  assert.match(explore, /dukascopy/);
  assert.match(explore, /FRED_API_KEY|FRED/);
  assert.match(explore, /Research capability coverage/);
  assert.match(explore, /data-research-capability="research_proof"/);
  const automation = render(resolveRoute("/automation"));
  assert.match(automation, /data-research-capability="native_custom_project_topology"/);
  assert.match(automation, /No automation control seam yet/);
  const operate = render(resolveRoute("/operate"));
  assert.match(operate, /No live or shadow runs/);
  assert.doesNotMatch(operate, /\$\s?\d/);
  const settings = render(resolveRoute("/settings"));
  assert.match(settings, /Expected build/);
  assert.match(settings, /144\.2953/);
  assert.match(settings, /data-native-runtime-setup/);
  assert.match(settings, /TRADERCOCKPIT_WATCHLIST/);
  assert.match(settings, /FRED_API_KEY/);
  assert.match(settings, /Connect Schwab/);
  assert.match(settings, /href="\/api\/market\/schwab\/authorize"/);
  assert.match(settings, /Sign in with Google/);
  const unknown = render(resolveRoute("/definitely-not-a-route"));
  assert.match(unknown, /data-unknown-route/);
  assert.match(unknown, /Returned to Home/);
});

test("shell sources carry no stale authority, donor language, or hard-coded market values", async () => {
  const files = ["app.mjs", "model.mjs", "ui.mjs", "home.mjs", "styles.css", "index.html", "research-signals.mjs", "research-evolution.mjs", "research-validate.mjs", "research-catalog.mjs", "surfaces.mjs"];
  const sources = Object.fromEntries(await Promise.all(files.map(async (file) => [file, await readFile(new URL(`../web/${file}`, import.meta.url), "utf8")])));
  for (const [file, source] of Object.entries(sources)) {
    assert.doesNotMatch(source, /APOLLO_SURFACE_ID|apollo-persistent|apollo-dock/, file);
    assert.doesNotMatch(source, /PR #/, file);
    assert.doesNotMatch(source, /donor/i, file);
    if (file.endsWith(".mjs")) assert.doesNotMatch(source, /\b(ESM5|NQM5|GCJ5|CLM5|BTCUSD)\b/, `${file} must not hard-code ticker symbols`);
  }
  assert.match(sources["index.html"], /src="\/app\.mjs"/);
  for (const binder of ["home-market-overview", "home-system-status", "native-runtime", "home-alpha-stack", "home-pipeline-overview", "research-specification", "research-blocks", "research-rankings", "research-cross-checks", "research-money-management", "research-presets", "research-custom-project", "research-build", "research-build-launch", "research-candidates", "research-backtest", "research-backtest-trades", "research-backtest-configuration", "research-backtest-robustness", "research-proof", "research-models"]) {
    assert.match(sources["index.html"], new RegExp(`src="/${binder}\\.mjs"`), binder);
  }
});
