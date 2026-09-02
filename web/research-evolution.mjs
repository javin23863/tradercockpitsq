// Research → Evolutionary Search (prototype screen `evolutionary_search_trading_dashboard`).
// Search controls come from the approved configuration executable XML (`BuildMode`,
// `Rankings`). Live GA telemetry (generations, fitness, Pareto frontier) has no producer
// seam yet and is carried as explicit frames. Build custody (compile → approve → launch)
// and Candidate import mount here through their binders.

import { researchNavPath, researchWorkspace, researchLocationMatches } from "./model.mjs";
import {
  actionButton,
  card,
  chartFrame,
  chip,
  escapeHtml,
  pageTitle,
  readable,
  table,
  unavailable,
  viewAll,
} from "./ui.mjs";
import { childNode, nativeConditions, nodeAttribute, nodeText } from "./native-config.mjs";
import { fetchConfiguration, fetchConfigurationCatalog } from "./research-build.mjs";
import { latestRecord } from "./research-snapshot.mjs";

const workspace = researchWorkspace("evolution");

function kv(rows) {
  return `<div class="kv-grid">${rows.map(([label, native, value]) => `<span class="k">${escapeHtml(label)}${native ? `<code>${escapeHtml(native)}</code>` : ""}</span><span class="v ${value === null || value === undefined || value === "—" ? "is-empty" : ""}">${escapeHtml(value === null || value === undefined ? "—" : value)}</span>`).join("")}</div>`;
}

function stripCell(label, value, { attrs = "", tone = "" } = {}) {
  return `<div class="stat-cell" ${attrs}><span class="stat-label">${escapeHtml(label)}</span><span class="stat-value ${tone ? `tone-text-${tone}` : ""}">${value}</span></div>`;
}

function pendingStrip(snapshot) {
  const job = latestRecord(snapshot.jobs);
  const stateValue = snapshot.phase === "loading"
    ? chip("Reading…", "pending")
    : job
      ? chip(readable(job.state), job.state === "submitted" ? "ready" : job.state === "failed" ? "error" : "pending")
      : chip("No native job", "unavailable");
  return `<div class="stat-strip" data-evolution-strip>
    ${stripCell("State", stateValue, { attrs: 'data-evolution-state' })}
    ${stripCell("Objective Set", `<span class="tone-text-dim">Reading…</span>`, { attrs: 'data-evolution-objective' })}
    ${stripCell("Optimization", `<span class="tone-text-dim">Reading…</span>`, { attrs: 'data-evolution-optimization' })}
    ${stripCell("Search Mode", `<span class="tone-text-dim">Reading…</span>`, { attrs: 'data-evolution-mode' })}
    ${stripCell("Deterministic Seed", `<span class="tone-text-dim" title="Not exposed by the native Builder task read model">Not exposed</span>`)}
    ${stripCell("Budget", `<span class="tone-text-dim">Reading…</span>`, { attrs: 'data-evolution-budget' })}
    ${stripCell("Time Elapsed", `<span class="tone-text-dim" title="Native Builder does not report elapsed time to TraderCockpit">—</span>`)}
    <div class="stat-cell is-actions">${actionButton("Pause", { iconName: "pause", disabled: true, title: "No native pause seam is exposed by the trusted gateway" })}${actionButton("Stop", { iconName: "stop", disabled: true, className: "button-danger", title: "No native stop seam is exposed by the trusted gateway" })}</div>
  </div>`;
}

function pendingBody(label) {
  return unavailable(`Reading ${label}…`, "Exact native BuildMode from the approved configuration.", { tone: "pending", compact: true });
}

const BUILDER_SEARCH_SCHEMA = "tc.sqx-builder-search.v1";
const BUILDER_RANKINGS_SCHEMA = "tc.sqx-builder-rankings.v1";

export function approvedCatalogId(catalog, preferredId) {
  const items = Array.isArray(catalog?.configurations) ? catalog.configurations : [];
  if (preferredId && items.some((item) => item.entity_id === preferredId && item.state === "approved")) {
    return preferredId;
  }
  const approved = items.filter((item) => item.state === "approved");
  return approved.length ? approved[approved.length - 1].entity_id : "";
}

export function viewFromApprovedConfiguration(configuration) {
  const search = configuration?.search;
  const rankings = configuration?.rankings;
  if (!search || search.schema !== BUILDER_SEARCH_SCHEMA || search.authority !== "native_sqx_read_only") {
    throw new Error("Approved configuration does not include native search controls");
  }
  if (configuration.approval?.approved !== true || search.source?.configuration_state !== "approved") {
    throw new Error("Search controls require an approved configuration");
  }
  if (search.source?.executable_xml_sha256 !== configuration.executable_xml_sha256) {
    throw new Error("Search controls are not bound to the approved executable XML");
  }
  if (rankings && rankings.schema !== BUILDER_RANKINGS_SCHEMA) {
    throw new Error("Approved configuration rankings schema mismatch");
  }
  return { search, rankings: rankings || { producer_configuration: null } };
}

export function renderEvolutionWorkspace(route, { snapshotState }) {
  const row1 = `<div class="grid grid-4">
    ${card({ title: "Search Configuration", accent: "neutral", actions: chip("Native SQX", "purple"), body: `<div data-evolution-search-config>${pendingBody("search configuration")}</div>` })}
    ${card({ title: "Population", sub: "Islands from the native BuildMode", accent: "neutral", actions: viewAll(researchNavPath("signals", "signals"), "View Specification"), body: `<div data-evolution-population>${pendingBody("population")}</div>` })}
    ${card({ title: "Generations", accent: "neutral", actions: viewAll(researchNavPath("validate", "overview"), "View History"), body: `<div data-evolution-generations>${pendingBody("generation settings")}</div>` })}
    ${card({ title: "Pareto Frontier", sub: "Objectives from native Rankings", accent: "neutral", body: chartFrame({ height: 150, state: "unavailable", detail: "Native Builder does not stream Pareto/ranking telemetry to TraderCockpit; the databank is imported as Candidates after the run.", yLabels: ["", "", ""] }) })}
  </div>`;
  const row2 = `<div class="grid grid-4">
    ${card({ title: "Variation Operators", accent: "neutral", actions: chip("Read-only", "unavailable"), body: `<div data-evolution-operators>${pendingBody("variation operators")}</div>` })}
    ${card({ title: "Fitness Evolution", accent: "neutral", className: "span-2", actions: viewAll(researchNavPath("validate", "overview"), "View Metrics"), body: chartFrame({ height: 160, state: "unavailable", detail: "Per-generation fitness telemetry is not exposed by the native Builder; TraderCockpit does not reconstruct it.", legend: [["Hypervolume", "purple"], ["Net Profit (Norm)", "cyan"], ["Max Drawdown (Inv)", "orange"], ["Turnover (Inv)", "green"]], yLabels: ["1.0", "0.5", "0.0"], xLabels: ["0", "50", "100", "150", "200", "250"] }) })}
    ${card({ title: "Islands Overview", accent: "neutral", body: `<div data-evolution-islands>${pendingBody("island settings")}</div>` })}
  </div>`;
  const row3 = `<div class="grid grid-4">
    ${card({ title: "Archive & Objectives", sub: "Native Rankings · acceptance conditions", accent: "neutral", actions: viewAll(researchNavPath("signals", "signals"), "View Rankings"), body: `<div data-evolution-objectives>${pendingBody("ranking objectives")}</div>` })}
    ${card({ title: "Top Candidates", sub: "Imported native Builder survivors", accent: "neutral", className: "span-2", actions: viewAll(researchNavPath("validate", "overview"), "Test & Validate"), body: `<div class="data-host" data-research-host="candidates">${unavailable("Reading Candidate custody…", "Exact native Results archives bound to submitted native jobs.", { tone: "pending", compact: true })}</div>` })}
    ${card({ title: "Deterministic Seed", sub: "Reproducibility & budget", accent: "neutral", body: `<div data-evolution-budget-card>${pendingBody("budget")}</div>` })}
  </div>`;
  const build = card({
    title: "Exact native configuration custody",
    sub: "Compile → review → approve → launch the native Builder through the trusted gateway",
    headIcon: "code",
    accent: "purple",
    actions: chip("Native SQX Builder", "purple"),
    body: `<div class="data-host" data-research-host="build">${unavailable("Reading configuration custody…", "Compiled snapshots and approval state from canonical custody.", { tone: "pending", compact: true })}</div>`,
  });
  return `${pageTitle(workspace.title, { actions: "" })}${pendingStrip(snapshotState)}${row1}${row2}${row3}${build}`;
}

// ---------- binder: fill native BuildMode / Rankings values ----------

function evolutionRoute() {
  return researchLocationMatches(globalThis.location, "evolution");
}

function setHost(selector, html) {
  const host = document.querySelector(selector);
  if (host) host.innerHTML = html;
}

function geneticActive(view) {
  return view.search?.display_mode?.kind === "genetic_evolution";
}

function renderSearchConfig(view) {
  const mode = view.search.producer_configuration;
  const restart = childNode(mode, "EvoRestartOnStagnation");
  const inSample = childNode(mode, "EvoInSamplePeriod");
  return kv([
    ["Population size", "PopulationSize", nodeText(mode, "PopulationSize")],
    ["Max generations", "MaxGenerations", nodeText(mode, "MaxGenerations")],
    ["Initial generation type", "InitGenerationType", nodeText(mode, "InitGenerationType")],
    ["Decimation coefficient", "DecimationCoef", nodeText(mode, "DecimationCoef")],
    ["In-sample ratio", "EvoInSamplePeriod", inSample ? `${inSample.attributes.ratio ?? "—"}%` : null],
    ["Restart on finish", "EvoRestartOnFinish", nodeAttribute(mode, "EvoRestartOnFinish", "status")],
    ["Restart on stagnation", "EvoRestartOnStagnation", restart ? `${restart.attributes.status} · ${restart.attributes.generations ?? "—"} gens` : null],
  ]) + `<p class="note">Exact native values from the approved configuration (${escapeHtml(view.search.source.member)} · ${escapeHtml(view.search.display_mode.label)}). StrategyQuant X owns encoding, selection and termination semantics.</p>`;
}

function renderPopulation(view) {
  const mode = view.search.producer_configuration;
  const islands = Number.parseInt(nodeText(mode, "Islands", ""), 10);
  const population = nodeText(mode, "PopulationSize");
  if (!Number.isInteger(islands) || islands <= 0) {
    return kv([["Islands", "Islands", nodeText(mode, "Islands")], ["Population size", "PopulationSize", population]]) + unavailable("Island layout not exposed", "The native BuildMode does not define an island count.", { compact: true });
  }
  const hexes = Array.from({ length: Math.min(islands, 12) }, (_, index) => `<div class="hex"><b>Island ${index + 1}</b><small>${escapeHtml(population || "—")} pop</small><small>rank —</small></div>`).join("");
  return `<div class="hex-row">${hexes}</div>${kv([["Islands", "Islands", String(islands)], ["Population size", "PopulationSize", population], ["Migration modulo", "MigrationModulo", nodeText(mode, "MigrationModulo")], ["Migration rate", "MigrationRate", nodeText(mode, "MigrationRate")]])}<p class="note">Per-island best rank and evaluations are native runtime telemetry not exposed to TraderCockpit.</p>`;
}

function renderGenerations(view) {
  const mode = view.search.producer_configuration;
  return `${kv([["Max generations", "MaxGenerations", nodeText(mode, "MaxGenerations")], ["Current generation", "", "—"], ["Best overall rank", "", "—"], ["Improvement trend", "", "—"]])}<div class="bar-row"><span class="bar-label">Progress</span><div class="bar tone-purple"><i style="width:0%"></i></div><span class="bar-value">— / ${escapeHtml(nodeText(mode, "MaxGenerations", "—"))}</span></div><p class="note">Generation progress is native runtime telemetry; only the configured maximum is exposed.</p>`;
}

export function renderOperators(view) {
  if (!geneticActive(view)) {
    return unavailable("Genetic Evolution operators not selected", "Crossover, mutation and fresh blood belong to Genetic Evolution. This approved configuration is Random Discovery.", { compact: true });
  }
  const mode = view.search.producer_configuration;
  return kv([
    ["Crossover probability", "CrossoverProbability", `${nodeText(mode, "CrossoverProbability", "—")}%`],
    ["Mutation probability", "MutationProbability", `${nodeText(mode, "MutationProbability", "—")}%`],
    ["Fresh blood · replace similar", "FreshBloodReplaceSimilar", nodeText(mode, "FreshBloodReplaceSimilar")],
    ["Fresh blood · replace weakest", "FreshBloodReplaceWeakest", nodeText(mode, "FreshBloodReplaceWeakest")],
    ["Weakest replaced", "FreshBloodWeakestPct", `${nodeText(mode, "FreshBloodWeakestPct", "—")}%`],
    ["Weakest generations", "FreshBloodWeakestGenerations", nodeText(mode, "FreshBloodWeakestGenerations")],
  ]);
}

function renderIslands(view) {
  if (!geneticActive(view)) {
    return unavailable("Islands not selected", "Island migration belongs to Genetic Evolution. This approved configuration is Random Discovery.", { compact: true });
  }
  const mode = view.search.producer_configuration;
  const islands = Number.parseInt(nodeText(mode, "Islands", ""), 10);
  const rows = Number.isInteger(islands) && islands > 0
    ? Array.from({ length: Math.min(islands, 12) }, (_, index) => ({ cells: [`<strong>${index + 1}</strong>`, "—", "—", "—", "—"] }))
    : [];
  return `${table({ columns: [{ label: "Island" }, { label: "Best Rank" }, { label: "Hypervolume" }, { label: "Diversity" }, { label: "Migration" }], rows, empty: "No island count in the native BuildMode." })}
    <div class="stat-strip" style="margin-top:8px">${stripCell("Migration interval", `<code>${escapeHtml(nodeText(mode, "MigrationModulo", "—"))}</code>`)}${stripCell("Migrants / island", `<code>${escapeHtml(nodeText(mode, "MigrationRate", "—"))}</code>`)}${stripCell("Topology", `<span class="tone-text-dim">Not exposed</span>`)}</div>`;
}

function renderObjectives(view) {
  const rankings = view.rankings.producer_configuration;
  if (!rankings) return unavailable("No Rankings subtree", "The exact current Builder task does not expose Rankings.", { compact: true });
  const fitness = childNode(rankings, "FitnessCriteria");
  const rankingType = nodeAttribute(childNode(fitness, "Settings"), "Ranking", "type", "—");
  const conditions = nativeConditions(childNode(rankings, "Conditions"));
  const stop = childNode(rankings, "StopCondition");
  return `${kv([["Fitness ranking", "FitnessCriteria/Settings/Ranking@type", rankingType], ["Fitness method", "FitnessCriteria@method", fitness?.attributes?.method ?? null], ["Max strategies", "MaxStrategies", nodeText(rankings, "MaxStrategies")], ["Stop condition", "StopCondition@type", stop?.attributes?.type ?? null], ["Dismiss too similar", "DismissTooSimilarStrategies", nodeText(rankings, "DismissTooSimilarStrategies")]])}
    <p class="detail-section-title">Acceptance conditions (${conditions.length})</p>
    <div class="list-rows">${conditions.map((condition) => `<div class="objective-row"><span class="obj-dir">${chip(condition.enabled ? "Enabled" : "Off", condition.enabled ? "ready" : "unavailable")}</span><span class="obj-name"><code>${escapeHtml(condition.column)}</code> ${escapeHtml(condition.comparator)} <code>${escapeHtml(condition.value)}</code></span><span class="obj-weight">native</span></div>`).join("") || `<p class="note">No acceptance conditions in the native Rankings.</p>`}</div>`;
}

function renderBudget(view, snapshot) {
  const rankings = view.rankings.producer_configuration;
  const stop = childNode(rankings, "StopCondition");
  const maxStrategies = nodeText(rankings, "MaxStrategies");
  const candidates = snapshot?.candidates?.length ?? 0;
  return `${kv([["Reproducibility", "", "Exact configuration bytes + SHA-256 in custody"], ["Seed", "", "Not exposed by producer"], ["Total budget", "MaxStrategies", maxStrategies ? `${maxStrategies} strategies` : null], ["Stop condition", "StopCondition@type", stop?.attributes?.type ?? null], ["Passed strategies", "StopCondition@passedStrategies", stop?.attributes?.passedStrategies ?? null], ["Restart count", "StopCondition@restartCount", stop?.attributes?.restartCount ?? null], ["Imported candidates", "", String(candidates)]])}<p class="note">Compute nodes, evaluations/sec and parallelism are native runtime telemetry not exposed to TraderCockpit.</p>`;
}

function fillStrip(view) {
  const rankings = view.rankings.producer_configuration;
  const fitness = childNode(rankings, "FitnessCriteria");
  const rankingType = nodeAttribute(childNode(fitness, "Settings"), "Ranking", "type", "");
  const conditions = nativeConditions(childNode(rankings, "Conditions"));
  setHost("[data-evolution-objective] .stat-value", rankingType ? `<code>${escapeHtml(rankingType)}</code><span class="tone-text-dim">+ ${conditions.length} conditions</span>` : `<span class="tone-text-dim">Not exposed</span>`);
  setHost("[data-evolution-optimization] .stat-value", `<span>${escapeHtml(conditions.length > 1 ? "Multi-condition" : "Single-condition")}</span><span class="tone-text-dim">native ranking</span>`);
  const mode = view.search.display_mode;
  setHost("[data-evolution-mode] .stat-value", `${chip(mode.label, mode.recognized ? "purple" : "unavailable")}${view.search.selector ? `<code>${escapeHtml(view.search.selector)}</code>` : ""}`);
  const maxStrategies = nodeText(rankings, "MaxStrategies");
  const stop = childNode(rankings, "StopCondition");
  setHost("[data-evolution-budget] .stat-value", maxStrategies ? `<span>${escapeHtml(maxStrategies)} strategies</span><span class="tone-text-dim">${escapeHtml(stop?.attributes?.type || "")}</span>` : `<span class="tone-text-dim">Not exposed</span>`);
}

let boundStrip = null;
let generation = 0;

async function bindEvolution() {
  if (!evolutionRoute()) { boundStrip = null; return; }
  const strip = document.querySelector("[data-evolution-strip]");
  if (!strip || strip === boundStrip) return;
  boundStrip = strip;
  const current = ++generation;
  try {
    const catalog = await fetchConfigurationCatalog();
    const entityId = approvedCatalogId(catalog, new URLSearchParams(globalThis.location?.search || "").get("configuration"));
    if (!entityId) {
      throw new Error("No approved configuration. Compile and approve a Builder task to bind Random Discovery or Genetic Evolution controls.");
    }
    const view = viewFromApprovedConfiguration(await fetchConfiguration(entityId));
    if (current !== generation || !strip.isConnected) return;
    fillStrip(view);
    setHost("[data-evolution-search-config]", renderSearchConfig(view));
    setHost("[data-evolution-population]", renderPopulation(view));
    setHost("[data-evolution-generations]", renderGenerations(view));
    setHost("[data-evolution-operators]", renderOperators(view));
    setHost("[data-evolution-islands]", renderIslands(view));
    setHost("[data-evolution-objectives]", renderObjectives(view));
    setHost("[data-evolution-budget-card]", renderBudget(view, null));
  } catch (error) {
    if (current !== generation || !strip.isConnected) return;
    const detail = error instanceof Error ? error.message : "Native Builder configuration unavailable";
    for (const selector of ["[data-evolution-search-config]", "[data-evolution-population]", "[data-evolution-generations]", "[data-evolution-operators]", "[data-evolution-islands]", "[data-evolution-objectives]", "[data-evolution-budget-card]"]) {
      setHost(selector, unavailable("Native configuration unavailable", detail, { tone: "error", compact: true }));
    }
    for (const selector of ["[data-evolution-objective]", "[data-evolution-optimization]", "[data-evolution-mode]", "[data-evolution-budget]"]) {
      setHost(`${selector} .stat-value`, chip("Unavailable", "error"));
    }
  }
}

if (typeof document !== "undefined") {
  const observer = new MutationObserver(() => { void bindEvolution(); });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void bindEvolution();
}
