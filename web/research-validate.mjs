// Research → Test & Validate (prototype screen `test-validate-dashboard`).
// Overview = KPI strip, validation funnel, performance/return frames, seven stage cards,
// Run & Evidence table, conclusions and next actions. StrategyQuant X owns the backtest and
// its native trade records; the cockpit owns the verdict: the backend `cockpit_verdict` read
// model evaluates the exact native acceptance conditions plus the documented cockpit stage
// policy over those records, and this surface renders it. The deep tools (native Retester,
// Trades, Robustness, executed Configuration chain, Proof) mount on their own tabs through the
// existing binders.

import { researchNavPath, researchWorkspace, researchLocationMatches } from "./model.mjs";
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
import { currentResearchSnapshot, latestRecord } from "./research-snapshot.mjs";
import {
  STAGE_STATE_LABEL,
  STAGE_STATE_TONE,
  average,
  checkValueLabel,
  fetchCockpitVerdicts,
  formatMoney,
  formatNumber,
  formatPeriod,
  stageOf,
  stageTally,
  verdictTally,
} from "./research-verdicts.mjs";

const workspace = researchWorkspace("validate");

// The seven prototype stages. Every stage carries a cockpit verdict per completed native
// result (`source`: which trade records and which rules decide it). `native` lists the
// StrategyQuant X CrossChecks methods of the same family whose enable flags are shown for
// context; they do not gate the cockpit verdict.
export const VALIDATION_STAGES = Object.freeze([
  Object.freeze({ id: "initial-test", number: 1, label: "Initial Test", sub: "Native Rankings acceptance on the Retester result", tone: "purple", source: "native_condition", native: [], tab: "initial-test" }),
  Object.freeze({ id: "fast-validation", number: 2, label: "Fast Validation", sub: "Native Higher Precision acceptance", tone: "blue", source: "native_condition", native: ["RetestWithHigherPrecision"], tab: "robustness" }),
  Object.freeze({ id: "golden-validation", number: 3, label: "Golden Validation", sub: "Initial criteria on higher precision + yearly consistency", tone: "cyan", source: "cockpit_policy", native: ["RetestOnAdditionalMarkets"], tab: "robustness" }),
  Object.freeze({ id: "scenario-tests", number: 4, label: "Scenario Tests", sub: "Quarterly regimes & profit concentration", tone: "green", source: "cockpit_policy", native: ["WhatIf", "OptProfileSysParamPermutation"], tab: "robustness" }),
  Object.freeze({ id: "stress-tests", number: 5, label: "Stress Tests", sub: "Seeded Monte Carlo over native trades", tone: "orange", source: "cockpit_policy", native: ["MonteCarloRetest", "MonteCarloManipulation"], tab: "robustness" }),
  Object.freeze({ id: "out-of-sample", number: 6, label: "Out-of-Sample", sub: "Native out-of-sample trades on their own", tone: "orange", source: "cockpit_policy", native: ["WalkForwardOptimization", "WalkForwardMatrix", "SequentialOptimization"], tab: "robustness" }),
  Object.freeze({ id: "evidence", number: 7, label: "Evidence", sub: "Immutable Research Proof", tone: "violet", source: "custody", native: [], tab: "evidence" }),
]);

function workspaceTabs(route) {
  return tabRow(workspace.tabs, route.tabId, (tab) => researchNavPath("validate", tab.id), { ariaLabel: "Test & Validate tabs" });
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

// `entries` = cockpit verdict readbacks ({ result, verdict, state, reason }) for completed
// results, newest first. `null` while the verdicts are still loading.
function latestVerdict(entries) {
  return (entries || []).find((entry) => entry.verdict)?.verdict || null;
}

function verdictState(entries) {
  if (entries === null) return "loading";
  return entries.some((entry) => entry.verdict) ? "computed" : "empty";
}

function stateChip(state, { attrs = "" } = {}) {
  return chip(STAGE_STATE_LABEL[state] || readable(state), STAGE_STATE_TONE[state] || "unavailable", { attrs });
}

function kpiStrip(counts, entries = null) {
  const total = counts.results + (counts.robustness ?? 0);
  const verdicts = verdictTally(entries || []);
  const stats = (entries || []).map((entry) => entry.verdict?.statistics?.full).filter(Boolean);
  const oos = (entries || []).map((entry) => entry.verdict?.statistics?.out_of_sample).filter(Boolean);
  const decided = verdicts.pass + verdicts.fail;
  const passRate = decided ? Math.round((verdicts.pass / decided) * 100) : null;
  const pending = entries === null
    ? "Computing cockpit verdicts…"
    : verdicts.total
      ? `${verdicts.total} judged · ${verdicts.in_progress} in progress · ${verdicts.incomplete} incomplete`
      : "No completed native result to judge yet";
  const maxDd = stats.length ? Math.max(...stats.map((item) => Number(item.Drawdown))) : null;
  return `<div class="kpi-strip" data-validate-kpis data-verdict-state="${verdictState(entries)}">
    ${kpi({ label: "Total Runs", value: String(total), delta: `${counts.results} retests · ${counts.robustness === null ? "…" : counts.robustness} robustness`, tone: total ? "neutral" : "unavailable" })}
    ${kpi({ label: "Pass Rate", value: passRate === null ? "—" : `${passRate}%`, note: passRate === null ? pending : `${verdicts.pass} of ${decided} decided · ${verdicts.incomplete + verdicts.in_progress} open`, tone: passRate === null ? "unavailable" : passRate >= 50 ? "green" : "orange", attrs: 'data-kpi="pass-rate"' })}
    ${kpi({ label: "Avg. Ret/DD", value: stats.length ? formatNumber(average(stats.map((item) => item.ReturnDDRatio))) : "—", note: stats.length ? "SQX ReturnDDRatio over native trades" : pending, tone: stats.length ? "neutral" : "unavailable" })}
    ${kpi({ label: "Out-of-Sample PF", value: oos.length ? formatNumber(average(oos.map((item) => item.ProfitFactor))) : "—", note: oos.length ? `${oos.length} result${oos.length === 1 ? "" : "s"} with native out-of-sample trades` : (entries === null ? pending : "No native out-of-sample trades"), tone: oos.length ? "neutral" : "unavailable" })}
    ${kpi({ label: "Max Drawdown", value: maxDd === null ? "—" : formatMoney(maxDd), note: maxDd === null ? pending : "Worst SQX Drawdown across judged results", tone: maxDd === null ? "unavailable" : "neutral" })}
    ${kpi({ label: "Expectancy", value: stats.length ? formatMoney(average(stats.map((item) => item.Expectancy))) : "—", note: stats.length ? "Net profit per native trade" : pending, tone: stats.length ? "neutral" : "unavailable" })}
    ${kpi({ label: "Profit Factor", value: stats.length ? formatNumber(average(stats.map((item) => item.ProfitFactor))) : "—", note: stats.length ? "SQX ProfitFactor over native trades" : pending, tone: stats.length ? "neutral" : "unavailable" })}
  </div>`;
}

function stageStatus(tally, crossChecks, stage) {
  if (tally.total === 0) return "no judged result yet";
  const parts = [];
  if (tally.pass) parts.push(`${tally.pass} pass`);
  if (tally.fail) parts.push(`${tally.fail} fail`);
  if (tally.incomplete) parts.push(`${tally.incomplete} incomplete`);
  if (tally.not_run) parts.push(`${tally.not_run} not run`);
  const nativeState = stage.native.map((method) => crossChecks?.[method]).filter((value) => value !== undefined);
  if (nativeState.length && !nativeState.some(Boolean)) parts.push("native method off");
  return parts.join(" · ");
}

function funnelRows(counts, crossChecks, entries = null) {
  const judged = (entries || []).filter((entry) => entry.verdict).length;
  return VALIDATION_STAGES.map((stage) => {
    const tally = stageTally(entries || [], stage.id);
    const count = entries === null ? null : tally.pass;
    const pct = judged > 0 && count !== null ? Math.round((count / judged) * 100) : null;
    const width = judged > 0 && count !== null ? Math.max(38, (count / judged) * 100) : 46;
    const state = entries === null ? "loading" : (tally.total ? (tally.pass ? "pass" : tally.fail ? "fail" : tally.incomplete ? "incomplete" : "not_run") : "empty");
    const tone = state === "pass" ? stage.tone : "unavailable";
    const status = entries === null ? "computing cockpit verdict…" : stageStatus(tally, crossChecks, stage);
    return `<div class="funnel-row" data-funnel-stage="${escapeHtml(stage.id)}" data-funnel-state="${escapeHtml(state)}" data-funnel-source="${escapeHtml(stage.source)}"><div class="funnel-cell"><div class="funnel-bar tone-${tone}" style="width:${width}%">${icon(state === "pass" ? "check" : state === "fail" ? "warn" : "activity", { size: 12 })}<span>${escapeHtml(stage.label)}</span></div><small>${escapeHtml(status)}</small></div><span class="funnel-count">${count === null ? "—" : `${count}/${judged}`}</span><span class="funnel-pct">${pct === null ? "—" : `${pct}%`}</span></div>`;
  }).join("");
}

function funnelCard(counts, crossChecks = null, entries = null) {
  const judged = (entries || []).filter((entry) => entry.verdict).length;
  const note = entries === null
    ? "Computing the cockpit verdict for each completed native result…"
    : judged
      ? `${judged} completed native result${judged === 1 ? "" : "s"} judged · counts are results passing each stage. StrategyQuant X produced the trades; the cockpit computes the verdict.`
      : "No completed native result to judge yet. Run the native Retester from Initial Test; the cockpit computes every stage verdict from the exact native trade records.";
  return card({
    title: "Validation Funnel",
    accent: "neutral",
    attrs: `data-validate-funnel-card data-verdict-state="${verdictState(entries)}"`,
    body: `<div class="funnel" data-validate-funnel>${funnelRows(counts, crossChecks, entries)}</div><p class="note" data-validate-funnel-note>${escapeHtml(note)}</p>`,
  });
}

function performanceCard(entries = null) {
  const verdict = latestVerdict(entries);
  const equity = verdict?.equity || [];
  const stats = verdict?.statistics?.full;
  if (equity.length > 1) {
    const values = equity.map((point) => point.balance);
    const low = Math.min(...values);
    const high = Math.max(...values);
    const mid = (low + high) / 2;
    const first = new Date(equity[0].time).toISOString().slice(0, 7);
    const last = new Date(equity[equity.length - 1].time).toISOString().slice(0, 7);
    return card({
      title: "Performance Overview",
      accent: "neutral",
      attrs: 'data-validate-performance="historical"',
      body: chartFrame({ height: 200, state: "historical", detail: "", legend: [["Equity (native trades)", "purple"]], yLabels: [formatMoney(high), formatMoney(mid), formatMoney(low)], xLabels: [first, last], series: [{ values, tone: "purple" }] }),
      footer: `<span class="note" style="margin:0">Latest judged result · ${escapeHtml(String(stats?.NumberOfTrades ?? equity.length))} native trades · initial capital ${escapeHtml(formatMoney(verdict.initial_capital))}</span><span class="pill">Equity Curve ${icon("down", { size: 12 })}</span>`,
    });
  }
  return card({
    title: "Performance Overview",
    accent: "neutral",
    attrs: 'data-validate-performance="empty"',
    body: chartFrame({ height: 200, state: "unavailable", detail: entries === null ? "Reading native trade records…" : "The equity curve draws from the native trade records of the latest completed result. No completed native result yet.", legend: [["Equity (native trades)", "purple"]], yLabels: ["", "", "", ""], xLabels: [] }),
    footer: `<div class="chart-ranges" style="border:0;padding:0"><span>1M</span><span>3M</span><span>6M</span><span>1Y</span><span>2Y</span><span class="is-active">All</span></div><span class="pill">Equity Curve ${icon("down", { size: 12 })}</span>`,
  });
}

function distributionCard(entries = null) {
  const stats = (entries || []).map((entry) => entry.verdict?.statistics?.full).filter(Boolean);
  if (!stats.length) {
    return card({
      title: "Return Distribution (All Runs)",
      accent: "neutral",
      attrs: 'data-validate-distribution="empty"',
      body: `${chartFrame({ height: 150, state: "unavailable", detail: entries === null ? "Reading native trade records…" : "Per-run net profit distribution appears once completed native results exist.", yLabels: ["", "", ""], xLabels: ["< -20%", "-10%", "0%", "10%", "20%", "> 30%"] })}
        <div class="metric-grid">${["Median", "IQR", "Positive", "Skew"].map((label) => `<div class="metric"><span>${label}</span><strong class="is-empty">—</strong></div>`).join("")}</div>`,
    });
  }
  const returns = stats.map((item) => (Number(item.NetProfit) / Number(item.initial_capital || 1)) * 100).sort((a, b) => a - b);
  const quantile = (q) => returns[Math.min(returns.length - 1, Math.max(0, Math.round((returns.length - 1) * q)))];
  const positive = Math.round((returns.filter((value) => value > 0).length / returns.length) * 100);
  const bins = [-Infinity, -20, -10, 0, 10, 20, 30, Infinity];
  const histogram = bins.slice(0, -1).map((low, index) => returns.filter((value) => value >= low && value < bins[index + 1]).length);
  return card({
    title: "Return Distribution (All Runs)",
    accent: "neutral",
    attrs: 'data-validate-distribution="historical"',
    body: `${chartFrame({ height: 150, state: "historical", detail: "", yLabels: [String(Math.max(...histogram)), "", "0"], xLabels: ["< -20%", "-10%", "0%", "10%", "20%", "> 30%"], series: [{ values: histogram, tone: "cyan" }] })}
      <div class="metric-grid">${[["Median", `${formatNumber(quantile(0.5))}%`], ["IQR", `${formatNumber(quantile(0.75) - quantile(0.25))}%`], ["Positive", `${positive}%`], ["Runs", String(returns.length)]].map(([label, value]) => `<div class="metric"><span>${label}</span><strong>${escapeHtml(value)}</strong></div>`).join("")}</div>
      <p class="note">Net profit as a share of initial capital per judged native result.</p>`,
  });
}

function stageMetric(stage, latest) {
  const stageRecord = stageOf(latest, stage.id);
  if (!stageRecord) return ["Latest", "—"];
  const find = (needle) => stageRecord.checks.find((check) => check.label.includes(needle));
  if (stage.id === "initial-test" || stage.id === "fast-validation") {
    const check = find("ProfitFactor");
    return ["Profit Factor", check ? checkValueLabel(check) : "—"];
  }
  if (stage.id === "golden-validation") return ["Profitable years", checkValueLabel(find("Profitable calendar years") || {})];
  if (stage.id === "scenario-tests") return ["Profitable quarters", checkValueLabel(find("Profitable calendar quarters") || {})];
  if (stage.id === "stress-tests") return ["MC drawdown P95", checkValueLabel(find("drawdown (95th") || {})];
  if (stage.id === "out-of-sample") return ["OOS profit factor", checkValueLabel(find("profit factor") || {})];
  return ["Proofs", checkValueLabel(stageRecord.checks[0] || {})];
}

function checkDots(stageRecord) {
  if (!stageRecord?.checks?.length) return "";
  return `<div class="check-dots" aria-label="checks">${stageRecord.checks.map((check) => `<span class="check-dot is-${escapeHtml(check.state)}" title="${escapeHtml(`${check.label}: ${checkValueLabel(check)} · ${STAGE_STATE_LABEL[check.state] || check.state}`)}"></span>`).join("")}</div>`;
}

function stageCards(counts, crossChecks = null, entries = null) {
  const latest = latestVerdict(entries);
  return `<div class="grid grid-7" data-validate-stages data-verdict-state="${verdictState(entries)}">${VALIDATION_STAGES.map((stage) => {
    const tally = stageTally(entries || [], stage.id);
    const latestStage = stageOf(latest, stage.id);
    const latestState = latestStage?.state || null;
    const nativeTags = stage.native.map((method) => {
      const state = crossChecks?.[method];
      const bound = (latest?.native_methods || []).find((item) => item.method === method);
      const bits = [state === undefined ? "?" : state ? "on" : "off"];
      if (bound?.bound_result === "bound") bits.push("bound");
      if (bound?.producer_column_count) bits.push(`${bound.producer_column_count} producer cols`);
      return tag(`${method} · ${bits.join(" · ")}`, state ? "green" : "neutral");
    }).join("");
    const passRate = tally.total ? `${Math.round((tally.pass / tally.total) * 100)}%` : "—";
    const [metricLabel, metricValue] = stageMetric(stage, latest);
    const metrics = [["Pass Rate", passRate], ["Checks", latestStage ? `${latestStage.checks_passed}/${latestStage.checks_total}` : "—"], [metricLabel, metricValue]];
    const runs = entries === null ? "computing…" : tally.total ? `${tally.pass}/${tally.total} pass` : "no judged result";
    return card({
      title: stage.label,
      sub: stage.sub,
      number: stage.number,
      accent: stage.tone,
      className: "stage-card",
      attrs: `data-validation-stage="${escapeHtml(stage.id)}" data-stage-state="${escapeHtml(latestState || (entries === null ? "loading" : "empty"))}" data-stage-source="${escapeHtml(stage.source)}" title="${escapeHtml(latestStage?.detail || stage.sub)}"`,
      actions: `<span class="stage-runs">${escapeHtml(runs)}</span>`,
      body: `${latestState ? `<div class="stage-verdict">${stateChip(latestState, { attrs: 'data-stage-latest' })}</div>` : ""}<div class="stage-metrics">${metrics.map(([label, value]) => `<div class="metric"><span>${escapeHtml(label)}</span><strong class="${value === "—" ? "is-empty" : ""}">${escapeHtml(value)}</strong></div>`).join("")}</div>${checkDots(latestStage) || sparkline("unavailable")}${nativeTags ? `<div class="stage-native">${nativeTags}</div>` : ""}`,
      footer: linkButton(researchNavPath("validate", stage.tab), stage.id === "evidence" ? "View Evidence" : "View Details", { className: "button-small" }),
    });
  }).join("")}</div>`;
}

function verdictCell(verdict) {
  if (!verdict) return `<span class="tone-text-dim" title="Cockpit verdict not available">—</span>`;
  const state = verdict.verdict.state;
  const tone = state === "pass" ? "ready" : state === "fail" ? "error" : state === "incomplete" ? "warn" : "pending";
  return chip(verdict.verdict.label, tone, { attrs: `data-run-verdict="${escapeHtml(state)}" title="${escapeHtml(`${verdict.verdict.stages_passed}/${verdict.verdict.stages_total} stages pass`)}"` });
}

function runRows(snapshot, robustness, entries = null) {
  const byRevision = new Map((entries || []).map((entry) => [entry.result?.revision, entry.verdict]));
  const rows = [];
  for (const result of snapshot.results.slice().reverse()) {
    const verdict = byRevision.get(result.revision) || null;
    const stats = verdict?.statistics?.full;
    rows.push({
      attrs: `data-run-revision="${escapeHtml(result.revision)}"`,
      cells: [
        `<code>${escapeHtml(shortId(result.revision, 10))}</code>`,
        tag("Initial Test", "purple"),
        escapeHtml(result.native_project_name || "—"),
        escapeHtml(formatPeriod(stats)),
        stats ? escapeHtml(formatMoney(stats.NetProfit)) : "—",
        stats ? escapeHtml(formatNumber(stats.ReturnDDRatio)) : "—",
        stats ? escapeHtml(formatMoney(stats.Drawdown)) : "—",
        stats ? escapeHtml(formatNumber(stats.ProfitFactor)) : "—",
        verdictCell(verdict),
        chip(readable(result.state), result.state === "completed" ? "ready" : result.state === "failed" ? "error" : "pending"),
        stats ? escapeHtml(String(stats.NumberOfTrades)) : "—",
      ],
    });
  }
  for (const run of (robustness?.results || []).slice().reverse()) {
    const verdict = byRevision.get(run.source_historical_result_revision) || null;
    const stats = verdict?.statistics?.higher_precision;
    const stage = stageOf(verdict, "fast-validation");
    rows.push({
      cells: [
        `<code>${escapeHtml(shortId(run.validation_ref, 10))}</code>`,
        tag("Fast Validation", "blue"),
        escapeHtml(run.native_project_name || "—"),
        `Precision ${escapeHtml(run.native_settings?.Precision ?? "—")}`,
        stats ? escapeHtml(formatMoney(stats.NetProfit)) : "—",
        stats ? escapeHtml(formatNumber(stats.ReturnDDRatio)) : "—",
        stats ? escapeHtml(formatMoney(stats.Drawdown)) : "—",
        stats ? escapeHtml(formatNumber(stats.ProfitFactor)) : "—",
        stage ? stateChip(stage.state) : `<span class="tone-text-dim">—</span>`,
        chip(readable(run.execution_state), "ready"),
        stats ? escapeHtml(String(stats.NumberOfTrades)) : "—",
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
    const verdict = byRevision.get(proof.historical_result_revision) || null;
    rows.push({
      cells: [
        `<code>${escapeHtml(shortId(proof.revision, 10))}</code>`,
        tag("Evidence", "violet"),
        "—", "—", "—", "—", "—", "—",
        verdictCell(verdict),
        chip("Promoted to evidence", "ready"),
        "—",
      ],
    });
  }
  return rows;
}

export function runTable(snapshot, robustness = null, entries = null) {
  return table({
    className: "run-table",
    attrs: 'data-validate-run-table',
    columns: [
      { label: "Run ID" }, { label: "Stage" }, { label: "Dataset" }, { label: "Period" },
      { label: "Net Profit", align: "right" }, { label: "Ret/DD", align: "right" }, { label: "Max DD", align: "right" }, { label: "Profit Factor", align: "right" },
      { label: "Verdict", align: "right" }, { label: "Status" }, { label: "Trades", align: "right" },
    ],
    rows: runRows(snapshot, robustness, entries),
    empty: snapshot.phase === "loading" ? "Reading canonical custody…" : "No native runs yet. Run the native Retester on an imported Candidate from Initial Test.",
  });
}

function runTableCard(snapshot, robustness = null, entries = null) {
  const count = snapshot.results.length + (robustness?.results.length ?? 0) + (robustness?.failedAttempts.length ?? 0) + snapshot.proofs.length;
  return card({
    title: "Run & Evidence Table",
    accent: "neutral",
    className: "span-2",
    actions: `${tag(String(count))}<span class="pill">All Stages ${icon("down", { size: 12 })}</span><span class="pill">All Datasets ${icon("down", { size: 12 })}</span><span class="icon-button">${icon("search", { size: 13 })}</span>`,
    body: `<div data-validate-run-host>${runTable(snapshot, robustness, entries)}</div><p class="note">Net profit, Ret/DD, drawdown and profit factor follow the published SQX column formulas over the exact native trade records; the verdict is the cockpit's.</p>`,
  });
}

function conclusionRows(verdict) {
  const stress = stageOf(verdict, "stress-tests");
  const scenario = stageOf(verdict, "scenario-tests");
  const oos = stageOf(verdict, "out-of-sample");
  const ddCheck = stress?.checks.find((check) => check.label.includes("drawdown (95th"));
  const wording = (stage, pass, fail) => (stage?.state === "pass" ? pass : stage?.state === "fail" ? fail : STAGE_STATE_LABEL[stage?.state] || "Not assessed");
  const risk = ddCheck ? (ddCheck.state === "pass" ? "Within limit" : ddCheck.state === "fail" ? "Exceeds limit" : "Unevaluated") : "Not assessed";
  return [
    ["Statistical Robustness", wording(stress, "Strong", "Weak"), stress?.state],
    ["Risk Controls", risk, ddCheck?.state],
    ["Regime Resilience", wording(scenario, "Consistent", "Regime-dependent"), scenario?.state],
    ["Overfitting Risk", wording(oos, "Low", "High"), oos?.state],
  ];
}

function conclusionsCard(counts, entries = null) {
  const verdict = latestVerdict(entries);
  const rows = ["Statistical Robustness", "Risk Controls", "Regime Resilience", "Overfitting Risk"];
  if (!verdict) {
    const detail = entries === null ? "Computing the cockpit verdict from the native trade records…" : "No completed native result to judge yet. The cockpit computes the verdict as soon as the native Retester completes a run.";
    return card({
      title: "Validation Conclusions",
      accent: "neutral",
      attrs: 'data-validate-conclusions="empty"',
      body: `<div class="conclusion-head"><span class="conclusion-mark">${icon("activity", { size: 22 })}</span><div><strong class="tone-text-dim">${entries === null ? "Computing…" : "No verdict yet"}</strong><p>${escapeHtml(detail)}</p></div></div>
        <div class="stat-list">${rows.map((row) => `<div class="stat-row"><span>${escapeHtml(row)}</span><strong class="tone-text-dim">Not assessed</strong></div>`).join("")}</div>`,
    });
  }
  const overall = verdict.verdict;
  const tone = overall.state === "pass" ? "green" : overall.state === "fail" ? "red" : "orange";
  const openStages = verdict.stages.filter((stage) => stage.state !== "pass");
  const summary = overall.state === "pass"
    ? "Every stage verdict passed on the exact native trade records."
    : openStages.slice(0, 3).map((stage) => `${VALIDATION_STAGES.find((item) => item.id === stage.id)?.label || stage.id}: ${STAGE_STATE_LABEL[stage.state] || stage.state}`).join(" · ");
  return card({
    title: "Validation Conclusions",
    accent: "neutral",
    attrs: `data-validate-conclusions="${escapeHtml(overall.state)}"`,
    body: `<div class="conclusion-head"><span class="conclusion-mark tone-${tone}">${icon(overall.state === "pass" ? "check" : overall.state === "fail" ? "warn" : "activity", { size: 22 })}</span><div><strong data-verdict-label>${escapeHtml(overall.label)}</strong><p>${escapeHtml(`${overall.stages_passed}/${overall.stages_total} stages pass · ${summary}`)}</p></div></div>
      <div class="stat-list">${conclusionRows(verdict).map(([label, value, state]) => `<div class="stat-row"><span>${escapeHtml(label)}</span><strong class="${state === "pass" ? "tone-text-green" : state === "fail" ? "tone-text-red" : "tone-text-dim"}">${escapeHtml(value)}</strong></div>`).join("")}</div>
      <p class="note">Cockpit verdict policy: ${escapeHtml(verdict.policy?.source === "environment" ? "backend override" : "backend defaults")} · initial capital ${escapeHtml(formatMoney(verdict.initial_capital))} (${escapeHtml(readable(verdict.initial_capital_source))}).</p>`,
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

// `entries` = cockpit verdict readbacks (null while loading), `robustness` = robustness catalog,
// `flags` = native CrossChecks enable flags. Exported so the verdict-driven rendering is testable
// without a DOM.
export function renderValidateOverview(snapshot, { entries = null, robustness = null, flags = null } = {}) {
  const counts = stageCounts(snapshot, robustness);
  return `<div data-validate-overview data-verdict-state="${verdictState(entries)}">
    ${kpiStrip(counts, entries)}
    <div class="grid grid-3">${funnelCard(counts, flags, entries)}${performanceCard(entries)}${distributionCard(entries)}</div>
    ${stageCards(counts, flags, entries)}
    <div class="grid grid-4">${runTableCard(snapshot, robustness, entries)}${conclusionsCard(counts, entries)}${nextActionsCard()}</div>
  </div>`;
}

function renderOverview(route, { snapshotState }) {
  const counts = stageCounts(snapshotState, null);
  // Verdicts load through the binder; until then every verdict-driven block reports "computing".
  const entries = snapshotState.phase === "loading" || counts.resultsCompleted ? null : [];
  return renderValidateOverview(snapshotState, { entries });
}

// ---------- tool tabs ----------

function renderToolTab(route, states) {
  if (route.tabId === "initial-test") {
    return `<div class="stack">${hostCard({ title: "Initial Test · Native Retester", sub: "Run the native Retester task on an imported Candidate and read back the exact historical result", host: "retester", accent: "purple", headIcon: "play" })}</div>`;
  }
  if (route.tabId === "trades") {
    return hostCard({ title: "Trades · native records", sub: "Exact Portfolio filled/non-control rows plus the cockpit verdict statistics and equity for the selected result", host: "trades", accent: "cyan", headIcon: "table" });
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
  const actions = `${selector}${actionButton("Compare", { iconName: "compare", disabled: true, title: "Compare needs two completed native results with read metrics" })}${linkButton(researchNavPath("validate", "robustness"), "New Validation", { primary: true, iconName: "plus" })}`;
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
  const snapshot = readSnapshot();
  const [robustnessSettled, crossChecksSettled, verdictsSettled] = await Promise.allSettled([
    fetchRobustnessCatalog(),
    fetchNativeBuilderCrossChecks(),
    fetchCockpitVerdicts(snapshot.results),
  ]);
  if (current !== generation || !host.isConnected) return;
  const robustness = robustnessSettled.status === "fulfilled" ? robustnessSettled.value : null;
  const flags = crossChecksSettled.status === "fulfilled" ? crossCheckFlags(crossChecksSettled.value) : null;
  const entries = verdictsSettled.status === "fulfilled" ? verdictsSettled.value : [];
  const counts = stageCounts(snapshot, robustness);
  const swaps = [
    ["[data-validate-funnel-card]", () => funnelCard(counts, flags, entries)],
    ["[data-validate-stages]", () => stageCards(counts, flags, entries)],
    ["[data-validate-kpis]", () => kpiStrip(counts, entries)],
    ["[data-validate-performance]", () => performanceCard(entries)],
    ["[data-validate-distribution]", () => distributionCard(entries)],
    ["[data-validate-conclusions]", () => conclusionsCard(counts, entries)],
  ];
  for (const [selector, render] of swaps) {
    const element = host.querySelector(selector);
    if (element) element.outerHTML = render();
  }
  host.setAttribute("data-verdict-state", verdictState(entries));
  const runHost = host.querySelector("[data-validate-run-host]");
  if (runHost) runHost.innerHTML = runTable(snapshot, robustness, entries);
  if (robustnessSettled.status === "rejected") {
    host.insertAdjacentHTML("afterbegin", `<p class="note tone-orange" data-validate-robustness-error>Robustness catalog unavailable: ${escapeHtml(robustnessSettled.reason instanceof Error ? robustnessSettled.reason.message : "read failed")}</p>`);
  }
  const failed = entries.filter((entry) => entry.state === "unavailable");
  if (failed.length) {
    host.insertAdjacentHTML("afterbegin", `<p class="note tone-orange" data-validate-verdict-error>Cockpit verdict unavailable for ${failed.length} result${failed.length === 1 ? "" : "s"}: ${escapeHtml(failed[0].reason || "read failed")}</p>`);
  }
}

if (typeof document !== "undefined") {
  const observer = new MutationObserver(() => { void bindOverview(currentResearchSnapshot); });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void bindOverview(currentResearchSnapshot);
}
