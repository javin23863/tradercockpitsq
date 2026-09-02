// Research → Test & Validate (prototype screen `test-validate-dashboard`).
// Overview = KPI strip, validation funnel, performance/return frames, seven stage cards,
// Run & Evidence table, conclusions and next actions — counts from canonical custody
// (historical results, robustness runs, proofs) and stage availability from the exact native
// CrossChecks configuration. The deep tools (native Retester, Trades, Robustness, executed
// Configuration chain, Proof) mount on their own tabs through the existing binders.

import { researchPath, researchWorkspace, researchLocationMatches } from "./model.mjs";
import {
  actionButton,
  card,
  chartFrame,
  chip,
  escapeHtml,
  icon,
  kpi,
  linkButton,
  pageTitle,
  readable,
  shortId,
  sparkline,
  table,
  tabRow,
  tag,
  unavailable,
} from "./ui.mjs";
import { fetchRobustnessCatalog } from "./research-backtest-robustness.mjs";
import { fetchNativeBuilderCrossChecks } from "./research-cross-checks.mjs";
import { childNodes } from "./native-config.mjs";
import { currentResearchSnapshot, latestRecord } from "./research-snapshot.mjs";

const workspace = researchWorkspace("validate");

// The seven prototype stages. `native` lists the exact StrategyQuant X CrossChecks methods
// whose enable flags are surfaced under each stage; `source` names the read model that
// counts runs for the stage today.
export const VALIDATION_STAGES = Object.freeze([
  Object.freeze({ id: "initial-test", number: 1, label: "Initial Test", sub: "Native Retester baseline", tone: "purple", source: "results", native: [], tab: "initial-test" }),
  Object.freeze({ id: "fast-validation", number: 2, label: "Fast Validation", sub: "Higher-precision retest", tone: "blue", source: "robustness", native: ["RetestWithHigherPrecision"], tab: "robustness" }),
  Object.freeze({ id: "golden-validation", number: 3, label: "Golden Validation", sub: "Additional-market retest", tone: "cyan", source: null, native: ["RetestOnAdditionalMarkets"], tab: "robustness" }),
  Object.freeze({ id: "scenario-tests", number: 4, label: "Scenario Tests", sub: "What-if & parameter permutation", tone: "green", source: null, native: ["WhatIf", "OptProfileSysParamPermutation"], tab: "robustness" }),
  Object.freeze({ id: "stress-tests", number: 5, label: "Stress Tests", sub: "Monte Carlo retest & manipulation", tone: "orange", source: null, native: ["MonteCarloRetest", "MonteCarloManipulation"], tab: "robustness" }),
  Object.freeze({ id: "out-of-sample", number: 6, label: "Out-of-Sample", sub: "Walk-forward optimization & matrix", tone: "orange", source: null, native: ["WalkForwardOptimization", "WalkForwardMatrix", "SequentialOptimization"], tab: "robustness" }),
  Object.freeze({ id: "evidence", number: 7, label: "Evidence", sub: "Immutable Research Proof", tone: "violet", source: "proofs", native: [], tab: "evidence" }),
]);

function workspaceTabs(route) {
  return tabRow(workspace.tabs, route.tabId, (tab) => researchPath("validate", tab.id), { ariaLabel: "Test & Validate tabs" });
}

function hostCard({ title, sub, host, accent = "neutral", headIcon = "table", actions = "" }) {
  return card({
    title,
    sub,
    headIcon,
    accent,
    actions,
    body: `<div class="data-host" data-research-host="${escapeHtml(host)}">${unavailable("Reading canonical custody…", "This tool binds to the canonical backend read models.", { tone: "pending", compact: true })}</div>`,
  });
}

// ---------- overview ----------

export function stageCounts(snapshot, robustness = null) {
  return {
    results: snapshot.results.length,
    resultsCompleted: snapshot.results.filter((result) => result.state === "completed").length,
    robustness: robustness ? robustness.results.length : null,
    robustnessFailed: robustness ? robustness.failedAttempts.length : null,
    proofs: snapshot.proofs.length,
  };
}

function stageCount(stage, counts) {
  if (stage.source === "results") return counts.results;
  if (stage.source === "robustness") return counts.robustness;
  if (stage.source === "proofs") return counts.proofs;
  return null;
}

function kpiStrip(counts) {
  const total = counts.results + (counts.robustness ?? 0);
  return `<div class="kpi-strip" data-validate-kpis>
    ${kpi({ label: "Total Runs", value: String(total), delta: `${counts.results} retests · ${counts.robustness === null ? "…" : counts.robustness} robustness`, tone: total ? "neutral" : "unavailable" })}
    ${kpi({ label: "Pass Rate", value: "—", note: "No verdict: producer outcome preserved unread", tone: "unavailable" })}
    ${kpi({ label: "Avg. Sharpe", value: "—", note: "Not read from native result archive", tone: "unavailable" })}
    ${kpi({ label: "Out-of-Sample Sharpe", value: "—", note: "Walk-forward not connected", tone: "unavailable" })}
    ${kpi({ label: "Max Drawdown", value: "—", note: "Not read from native result archive", tone: "unavailable" })}
    ${kpi({ label: "Expectancy (R)", value: "—", note: "Not read from native result archive", tone: "unavailable" })}
    ${kpi({ label: "Profit Factor", value: "—", note: "Not read from native result archive", tone: "unavailable" })}
  </div>`;
}

function funnelRows(counts, crossChecks) {
  const first = counts.results || 0;
  return VALIDATION_STAGES.map((stage) => {
    const count = stageCount(stage, counts);
    const connected = stage.source !== null;
    const pct = connected && first > 0 && count !== null ? Math.round((count / first) * 100) : null;
    const width = connected ? Math.max(46, count && first ? (count / first) * 100 : 46) : 40;
    const nativeState = stage.native.map((method) => crossChecks?.[method]).filter((value) => value !== undefined);
    const status = connected
      ? ""
      : (nativeState.length ? (nativeState.some(Boolean) ? "native method enabled · not connected" : "native method off") : "not connected");
    return `<div class="funnel-row" data-funnel-stage="${escapeHtml(stage.id)}" data-funnel-state="${connected ? "connected" : "unavailable"}"><div class="funnel-cell"><div class="funnel-bar tone-${connected ? stage.tone : "unavailable"}" style="width:${width}%">${icon(connected ? "check" : "warn", { size: 12 })}<span>${escapeHtml(stage.label)}</span></div>${status ? `<small>${escapeHtml(status)}</small>` : ""}</div><span class="funnel-count">${count === null ? "—" : count}</span><span class="funnel-pct">${pct === null ? "—" : `${pct}%`}</span></div>`;
  }).join("");
}

function funnelCard(counts, crossChecks = null) {
  return card({
    title: "Validation Funnel",
    accent: "neutral",
    body: `<div class="funnel" data-validate-funnel>${funnelRows(counts, crossChecks)}</div><p class="note" data-validate-funnel-note>${escapeHtml(counts.proofs ? `${counts.proofs} run${counts.proofs === 1 ? "" : "s"} promoted to evidence` : "No runs promoted to evidence yet")}. Stage counts are custody records; no pass/fail is inferred.</p>`,
  });
}

function performanceCard() {
  return card({
    title: "Performance Overview",
    accent: "neutral",
    body: chartFrame({ height: 200, state: "unavailable", detail: "Equity curves are not read from the native result archive yet; TraderCockpit does not reconstruct them.", legend: [["In-Sample", "purple"], ["Out-of-Sample", "cyan"], ["Buy & Hold", "neutral"]], yLabels: ["", "", "", ""], xLabels: [] }),
    footer: `<div class="chart-ranges" style="border:0;padding:0"><span>1M</span><span>3M</span><span>6M</span><span>1Y</span><span>2Y</span><span class="is-active">All</span></div><span class="pill">Equity Curve ${icon("down", { size: 12 })}</span>`,
  });
}

function distributionCard() {
  return card({
    title: "Return Distribution (All Runs)",
    accent: "neutral",
    body: `${chartFrame({ height: 150, state: "unavailable", detail: "Per-run return distribution requires native trade rows across runs; see Trades for the exact native records.", yLabels: ["", "", ""], xLabels: ["< -20%", "-10%", "0%", "10%", "20%", "> 30%"] })}
      <div class="metric-grid">${["Median", "IQR", "Positive", "Skew"].map((label) => `<div class="metric"><span>${label}</span><strong class="is-empty">—</strong></div>`).join("")}</div>`,
  });
}

function stageCards(counts, crossChecks = null) {
  return `<div class="grid grid-7" data-validate-stages>${VALIDATION_STAGES.map((stage) => {
    const count = stageCount(stage, counts);
    const connected = stage.source !== null;
    const nativeTags = stage.native.map((method) => {
      const state = crossChecks?.[method];
      return tag(`${method} · ${state === undefined ? "?" : state ? "on" : "off"}`, state ? "green" : "neutral");
    }).join("");
    const metrics = stage.id === "evidence"
      ? [["Proofs", count === null ? "—" : String(count)], ["Outcome", "unread"], ["Verdict", "—"]]
      : [["Pass Rate", "—"], ["Avg. Sharpe", "—"], ["Profit Factor", "—"]];
    return card({
      title: stage.label,
      sub: stage.sub,
      number: stage.number,
      accent: stage.tone,
      className: "stage-card",
      attrs: `data-validation-stage="${escapeHtml(stage.id)}" data-stage-state="${connected ? "connected" : "unavailable"}"`,
      actions: `<span class="stage-runs">${count === null ? "not connected" : `${count} run${count === 1 ? "" : "s"}`}</span>`,
      body: `<div class="stage-metrics">${metrics.map(([label, value]) => `<div class="metric"><span>${escapeHtml(label)}</span><strong class="${value === "—" ? "is-empty" : ""}">${escapeHtml(value)}</strong></div>`).join("")}</div>${sparkline("unavailable")}${nativeTags ? `<div class="stage-native">${nativeTags}</div>` : ""}`,
      footer: linkButton(researchPath("validate", stage.tab), stage.id === "evidence" ? "View Evidence" : "View Details", { className: "button-small" }),
    });
  }).join("")}</div>`;
}

function runRows(snapshot, robustness) {
  const rows = [];
  for (const result of snapshot.results.slice().reverse()) {
    rows.push({
      cells: [
        `<code>${escapeHtml(shortId(result.revision, 10))}</code>`,
        tag("Initial Test", "purple"),
        escapeHtml(result.native_project_name || "—"),
        "—", "—", "—", "—", "—",
        `<span class="tone-text-dim" title="Producer outcome preserved unread">—</span>`,
        chip(readable(result.state), result.state === "completed" ? "ready" : result.state === "failed" ? "error" : "pending"),
        "—",
      ],
    });
  }
  for (const run of (robustness?.results || []).slice().reverse()) {
    rows.push({
      cells: [
        `<code>${escapeHtml(shortId(run.validation_ref, 10))}</code>`,
        tag("Fast Validation", "blue"),
        escapeHtml(run.native_project_name || "—"),
        `Precision ${escapeHtml(run.native_settings?.Precision ?? "—")}`,
        "—", "—", "—", "—",
        `<span class="tone-text-dim" title="Producer outcome preserved unread">—</span>`,
        chip(readable(run.execution_state), "ready"),
        "—",
      ],
    });
  }
  for (const attempt of (robustness?.failedAttempts || []).slice().reverse()) {
    rows.push({
      cells: [
        `<code>${escapeHtml(shortId(attempt.attempt_ref, 10))}</code>`,
        tag("Fast Validation", "blue"),
        escapeHtml(attempt.native_project_name || "—"),
        "—", "—", "—", "—", "—",
        `<span class="tone-text-dim">—</span>`,
        chip("Failed", "error"),
        "—",
      ],
    });
  }
  for (const proof of snapshot.proofs.slice().reverse()) {
    rows.push({
      cells: [
        `<code>${escapeHtml(shortId(proof.revision, 10))}</code>`,
        tag("Evidence", "violet"),
        "—", "—", "—", "—", "—", "—",
        `<span class="tone-text-dim" title="Producer outcome preserved unread">—</span>`,
        chip("Promoted to evidence", "ready"),
        "—",
      ],
    });
  }
  return rows;
}

export function runTable(snapshot, robustness = null) {
  return table({
    className: "run-table",
    attrs: 'data-validate-run-table',
    columns: [
      { label: "Run ID" }, { label: "Stage" }, { label: "Dataset" }, { label: "Period" },
      { label: "Net Profit", align: "right" }, { label: "Sharpe", align: "right" }, { label: "Max DD", align: "right" }, { label: "Profit Factor", align: "right" },
      { label: "Pass", align: "right" }, { label: "Status" }, { label: "Completed", align: "right" },
    ],
    rows: runRows(snapshot, robustness),
    empty: snapshot.phase === "loading" ? "Reading canonical custody…" : "No native runs yet. Run the native Retester on an imported Candidate from Initial Test.",
  });
}

function runTableCard(snapshot, robustness = null) {
  const count = snapshot.results.length + (robustness?.results.length ?? 0) + (robustness?.failedAttempts.length ?? 0) + snapshot.proofs.length;
  return card({
    title: "Run & Evidence Table",
    accent: "neutral",
    className: "span-2",
    actions: `${tag(String(count))}<span class="pill">All Stages ${icon("down", { size: 12 })}</span><span class="pill">All Datasets ${icon("down", { size: 12 })}</span><span class="icon-button">${icon("search", { size: 13 })}</span>`,
    body: `<div data-validate-run-host>${runTable(snapshot, robustness)}</div><p class="note">Net profit, Sharpe, drawdown and profit factor are not reconstructed from the native result archives; the exact archives and trade rows are preserved in custody.</p>`,
  });
}

function conclusionsCard(counts) {
  const rows = ["Statistical Robustness", "Risk Controls", "Regime Resilience", "Overfitting Risk"];
  return card({
    title: "Validation Conclusions",
    accent: "neutral",
    body: `<div class="conclusion-head"><span class="conclusion-mark">${icon("check", { size: 22 })}</span><div><strong class="tone-text-dim">No verdict</strong><p>TraderCockpit does not compute pass/fail. ${escapeHtml(counts.proofs ? `${counts.proofs} proof${counts.proofs === 1 ? "" : "s"} preserve the native producer outcome unread.` : "The native producer outcome stays with the exact result archive.")}</p></div></div>
      <div class="stat-list">${rows.map((row) => `<div class="stat-row"><span>${escapeHtml(row)}</span><strong class="tone-text-dim">Not assessed</strong></div>`).join("")}</div>`,
  });
}

function nextActionsCard() {
  const actions = [
    ["Deploy to Paper", "Deploy to paper trading for final verification", "/operate", false],
    ["Shadow Live", "Run in shadow mode for 2–4 weeks", "/operate", false],
    ["Promote to Live", "Enable live trading with risk limits", "/operate", false],
    ["Add to Watchlist", "Monitor and alert on performance", "/operate", false],
    ["Schedule Review", "Review reminder", "/automation", false],
  ];
  return card({
    title: "Next Actions",
    accent: "neutral",
    body: `<div class="stack" style="gap:8px">${actions.map(([label, sub, path]) => `<a class="action-row is-disabled" href="${escapeHtml(path)}" data-route="${escapeHtml(path)}" title="Requires the Operate/Automation producer (not connected)"><span class="action-icon">${icon("target", { size: 14 })}</span><span class="row-title"><strong>${escapeHtml(label)}</strong><span>${escapeHtml(sub)} · not connected</span></span>${icon("chevron", { size: 14, className: "arrow" })}</a>`).join("")}</div>`,
  });
}

function renderOverview(route, { snapshotState }) {
  const counts = stageCounts(snapshotState, null);
  return `<div data-validate-overview>
    ${kpiStrip(counts)}
    <div class="grid grid-3">${funnelCard(counts)}${performanceCard()}${distributionCard()}</div>
    ${stageCards(counts)}
    <div class="grid grid-4">${runTableCard(snapshotState)}${conclusionsCard(counts)}${nextActionsCard()}</div>
  </div>`;
}

// ---------- tool tabs ----------

function renderToolTab(route, states) {
  if (route.tabId === "initial-test") {
    return `<div class="stack">${hostCard({ title: "Initial Test · Native Retester", sub: "Run the native Retester task on an imported Candidate and read back the exact historical result", host: "retester", accent: "purple", headIcon: "play" })}</div>`;
  }
  if (route.tabId === "trades") {
    return hostCard({ title: "Trades · native records", sub: "Exact Portfolio filled/non-control rows from the completed native result archive", host: "trades", accent: "cyan", headIcon: "table" });
  }
  if (route.tabId === "robustness") {
    return hostCard({ title: "Robustness · producer-backed methods", sub: "Higher Precision retest through the installed StrategyQuant X Retester", host: "robustness", accent: "orange", headIcon: "shield" });
  }
  if (route.tabId === "configuration") {
    return hostCard({ title: "Configuration · executed chain", sub: "approved configuration → submitted Builder job → Candidate archive → completed native result", host: "configuration", accent: "blue", headIcon: "code" });
  }
  return hostCard({ title: "Evidence · Research Proof", sub: "Bind exact Idea, configuration, native job, artifact, result and validation identities", host: "proof", accent: "violet", headIcon: "check" });
}

export function renderValidateWorkspace(route, states) {
  const { snapshotState } = states;
  const candidate = latestRecord(snapshotState.candidates);
  const selector = `<span class="pill" title="${escapeHtml(candidate ? `Latest imported Candidate · ${candidate.archive_sha256}` : "No imported Candidate yet")}">${escapeHtml(candidate ? candidate.archive_name : "No Candidate selected")} ${icon("down", { size: 12 })}</span>`;
  const actions = `${selector}${actionButton("Compare", { iconName: "compare", disabled: true, title: "Compare needs two completed native results with read metrics" })}${linkButton(researchPath("validate", "robustness"), "New Validation", { primary: true, iconName: "plus" })}`;
  const body = route.tabId === "overview" ? renderOverview(route, states) : renderToolTab(route, states);
  return `${pageTitle(workspace.title, { subtitle: "Prove robustness. Validate edges. Build conviction.", actions })}${workspaceTabs(route)}${body}`;
}

// ---------- binder: robustness runs + native CrossChecks flags for the overview ----------

function overviewRoute() {
  return researchLocationMatches(globalThis.location, "validate", "overview");
}

export function crossCheckFlags(crossChecks) {
  const root = crossChecks?.producer_configuration;
  if (!root) return {};
  const flags = {};
  for (const method of root.children) flags[method.tag] = method.attributes.use === "true";
  return flags;
}

let boundOverview = null;
let generation = 0;

async function bindOverview(readSnapshot) {
  if (!overviewRoute()) { boundOverview = null; return; }
  const host = document.querySelector("[data-validate-overview]");
  if (!host || host === boundOverview) return;
  boundOverview = host;
  const current = ++generation;
  const [robustnessSettled, crossChecksSettled] = await Promise.allSettled([fetchRobustnessCatalog(), fetchNativeBuilderCrossChecks()]);
  if (current !== generation || !host.isConnected) return;
  const robustness = robustnessSettled.status === "fulfilled" ? robustnessSettled.value : null;
  const flags = crossChecksSettled.status === "fulfilled" ? crossCheckFlags(crossChecksSettled.value) : null;
  const snapshot = readSnapshot();
  const counts = stageCounts(snapshot, robustness);
  const funnel = host.querySelector("[data-validate-funnel]");
  if (funnel) funnel.innerHTML = funnelRows(counts, flags);
  const stages = host.querySelector("[data-validate-stages]");
  if (stages) stages.outerHTML = stageCards(counts, flags);
  const runHost = host.querySelector("[data-validate-run-host]");
  if (runHost) runHost.innerHTML = runTable(snapshot, robustness);
  const kpis = host.querySelector("[data-validate-kpis]");
  if (kpis) kpis.outerHTML = kpiStrip(counts);
  if (robustnessSettled.status === "rejected") {
    host.insertAdjacentHTML("afterbegin", `<p class="note tone-orange" data-validate-robustness-error>Robustness catalog unavailable: ${escapeHtml(robustnessSettled.reason instanceof Error ? robustnessSettled.reason.message : "read failed")}</p>`);
  }
}

if (typeof document !== "undefined") {
  const observer = new MutationObserver(() => { void bindOverview(currentResearchSnapshot); });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void bindOverview(currentResearchSnapshot);
}
