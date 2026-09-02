// Cockpit Home — the `cockpit-home` prototype screen: hero + Recent Activity + eight numbered
// cards. Every value shown comes from a backend read model or is an explicit
// "not connected / no data yet" state. Cards 5, 6 and 7 are completed by the home-* binders.

import { HOME_ZONES, researchPath } from "./model.mjs";
import {
  actionButton,
  card,
  chartFrame,
  chip,
  escapeHtml,
  footLink,
  icon,
  linkButton,
  pageTitle,
  readable,
  shortId,
  sparkline,
  table,
  tag,
  toneForStatus,
  unavailable,
  viewAll,
} from "./ui.mjs";
import { latestRecord } from "./research-snapshot.mjs";
import { renderAssistantWidget } from "./assistant.mjs";

const zoneById = new Map(HOME_ZONES.map((zone) => [zone.id, zone]));

function zoneCard(id, { body, footer = "", actions = "", className = "" }) {
  const zone = zoneById.get(id);
  return card({
    title: zone.label,
    sub: zone.sub,
    number: zone.number,
    accent: zone.accent,
    actions,
    body,
    footer,
    className,
    attrs: `data-home-zone="${escapeHtml(zone.id)}"`,
  });
}

// ---------- hero ----------

function heroArt() {
  // Decorative illustration only (no data): workflow nodes over a stylised curve.
  return `<div class="hero-art" aria-hidden="true"><svg viewBox="0 0 560 220" preserveAspectRatio="xMaxYMid slice">
    <defs>
      <linearGradient id="hero-line" x1="0" x2="1"><stop offset="0" stop-color="#7c3aed" stop-opacity=".2"/><stop offset=".6" stop-color="#a855f7"/><stop offset="1" stop-color="#22d3ee"/></linearGradient>
      <linearGradient id="hero-fill" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#7c3aed" stop-opacity=".35"/><stop offset="1" stop-color="#7c3aed" stop-opacity="0"/></linearGradient>
    </defs>
    <path d="M0 200 C60 190 90 175 130 168 S200 150 240 132 300 118 330 96 380 70 420 60 480 40 560 24 L560 220 L0 220Z" fill="url(#hero-fill)"/>
    <path d="M0 200 C60 190 90 175 130 168 S200 150 240 132 300 118 330 96 380 70 420 60 480 40 560 24" fill="none" stroke="url(#hero-line)" stroke-width="2.5"/>
    ${[[150, 160, "flask"], [250, 128, "code"], [340, 92, "spark"], [430, 58, "play"], [510, 30, "target"]].map(([x, y, name]) => `<g transform="translate(${x} ${y})"><line x1="0" y1="0" x2="0" y2="-34" stroke="#a78bfa" stroke-opacity=".5"/><circle cx="0" cy="-50" r="16" fill="#0e0a1e" stroke="#a78bfa" stroke-opacity=".7"/><g transform="translate(-8 -58)" stroke="#c4b5fd" stroke-width="1.6" fill="none" stroke-linecap="round" stroke-linejoin="round"><svg width="16" height="16" viewBox="0 0 24 24">${iconPath(name)}</svg></g></g>`).join("")}
  </svg></div>`;
}

function iconPath(name) {
  // Reuse the icon set by extracting the inner path markup from ui.icon().
  const markup = icon(name);
  const start = markup.indexOf(">") + 1;
  return markup.slice(start, markup.lastIndexOf("</svg>"));
}

function renderHero() {
  const steps = ["Research", "Build", "Validate", "Simulate", "Deploy"];
  return `<section class="hero" data-home-hero>
    <div class="hero-copy">
      <h2>Turn Research into <b>Decisions that Compound.</b></h2>
      <div class="hero-workflow">${steps.map((step, index) => `<span>${escapeHtml(step)}</span>${index < steps.length - 1 ? icon("chevron", { size: 12 }) : ""}`).join("")}</div>
      <p>A complete workflow for systematic traders — native StrategyQuant X research with exact custody at every step.</p>
      <div class="hero-actions">${linkButton(researchPath("signals", "overview"), "New Research", { primary: true, iconName: "flask" })}${linkButton(researchPath("evolution"), "Build Strategy", { iconName: "code" })}</div>
    </div>
    ${heroArt()}
  </section>`;
}

// ---------- recent activity ----------

export function recentActivityEntries(snapshot) {
  const entries = [];
  const proof = latestRecord(snapshot.proofs);
  if (proof) entries.push({ tone: "green", iconName: "check", title: "Research Proof created", sub: shortId(proof.revision), kind: "Proof" });
  const result = latestRecord(snapshot.results);
  if (result) entries.push({ tone: result.state === "completed" ? "green" : result.state === "failed" ? "orange" : "blue", iconName: "play", title: `Native Retester ${readable(result.state).toLowerCase()}`, sub: shortId(result.revision), kind: "Historical result" });
  const candidate = latestRecord(snapshot.candidates);
  if (candidate) entries.push({ tone: "purple", iconName: "layers", title: "Candidate imported", sub: candidate.archive_name, kind: "Candidate" });
  const job = latestRecord(snapshot.jobs);
  if (job) entries.push({ tone: job.state === "submitted" ? "green" : "orange", iconName: "activity", title: `Native Builder job ${readable(job.state).toLowerCase()}`, sub: shortId(job.configuration_revision || job.revision), kind: "Native job" });
  const configuration = latestRecord(snapshot.configurations);
  if (configuration) entries.push({ tone: "blue", iconName: "code", title: `Configuration ${readable(configuration.state).toLowerCase()}`, sub: shortId(configuration.revision), kind: "Configuration" });
  const idea = latestRecord(snapshot.ideas);
  if (idea) entries.push({ tone: "cyan", iconName: "flask", title: "Idea saved", sub: idea.summary || shortId(idea.revision), kind: "Idea" });
  return entries;
}

function renderRecentActivity(snapshot) {
  let body;
  if (snapshot.phase === "loading") {
    body = unavailable("Reading custody…", "Recent activity is derived from the canonical Research custody catalogs.", { tone: "pending", compact: true });
  } else if (snapshot.phase === "failed") {
    body = unavailable("Custody read failed", Object.values(snapshot.failures)[0] || "The Research custody catalogs could not be read.", { tone: "error", compact: true });
  } else {
    const entries = recentActivityEntries(snapshot);
    body = entries.length
      ? `<div class="activity-list" data-recent-activity>${entries.map((entry) => `<div class="activity-row"><span class="activity-icon tone-${escapeHtml(entry.tone)}">${icon(entry.iconName, { size: 14 })}</span><span class="row-title"><strong>${escapeHtml(entry.title)}</strong><span>${escapeHtml(entry.sub)}</span></span><span class="row-meta">${escapeHtml(entry.kind)}</span></div>`).join("")}</div>`
      : unavailable("No activity yet", "Start with New Research; saved Ideas, configurations, native jobs, candidates, results and proofs appear here.", { compact: true });
    if (snapshot.phase === "partial") body += `<p class="note tone-orange">Some custody catalogs failed to read: ${escapeHtml(Object.keys(snapshot.failures).join(", "))}.</p>`;
  }
  return card({ title: "Recent Activity", headIcon: "clock", accent: "neutral", actions: viewAll(researchPath("validate", "overview")), body, className: "home-activity" });
}

// ---------- numbered cards ----------

function researchCard(snapshot) {
  const ideas = snapshot.ideas.slice(-3).reverse();
  const body = snapshot.phase === "loading"
    ? unavailable("Reading Ideas…", "Immutable Idea revisions from canonical custody.", { tone: "pending", compact: true })
    : snapshot.failures.ideas
      ? unavailable("Idea custody read failed", snapshot.failures.ideas, { tone: "error", compact: true })
      : ideas.length
        ? `<div class="list-rows">${ideas.map((idea) => `<div class="list-row"><span class="row-title"><strong>${escapeHtml(idea.summary || "Untitled Idea")}</strong><span class="row-tags">${tag("Idea")}${tag(shortId(idea.revision, 8))}</span></span><span class="score-box is-empty" title="No scoring producer is connected">—</span>${sparkline("unavailable")}</div>`).join("")}</div>`
        : unavailable("No Ideas yet", "Capture the first strategy concept with New Research.", { compact: true });
  return zoneCard("research", {
    actions: viewAll(researchPath("signals", "overview")),
    body,
    footer: footLink(researchPath("signals", "overview"), "New Research", { iconName: "chevron" }),
  });
}

function metricCell(label, value = "—", tone = "") {
  const empty = value === "—";
  return `<div class="metric"><span>${escapeHtml(label)}</span><strong class="${empty ? "is-empty" : ""} ${tone ? `tone-${tone}` : ""}" ${empty ? 'title="Not read from the native result archive yet"' : ""}>${escapeHtml(value)}</strong></div>`;
}

function buildBacktestCard(snapshot) {
  const result = latestRecord(snapshot.results);
  let body;
  if (snapshot.phase === "loading") {
    body = unavailable("Reading historical results…", "Native Retester custody.", { tone: "pending", compact: true });
  } else if (snapshot.failures.results) {
    body = unavailable("Historical result custody read failed", snapshot.failures.results, { tone: "error", compact: true });
  } else if (!result) {
    body = `${unavailable("No native backtest yet", "Run the native Retester on an imported Candidate in Test & Validate.", { compact: true })}<div class="metric-grid">${metricCell("Net Profit")}${metricCell("Sharpe")}${metricCell("Win Rate")}${metricCell("Max DD")}</div>`;
  } else {
    const tone = result.state === "completed" ? "ready" : result.state === "failed" ? "error" : "pending";
    body = `<div class="list-row" style="padding-top:0"><span class="row-title"><strong>${escapeHtml(result.native_project_name || "Native Retester")}</strong><span>Candidate ${escapeHtml(shortId(result.candidate_revision, 10))} · task ${escapeHtml(result.retester_task || "—")}</span></span>${chip(readable(result.state), tone)}</div>
      <div class="metric-grid">${metricCell("Net Profit")}${metricCell("Sharpe")}${metricCell("Win Rate")}${metricCell("Max DD")}</div>
      ${chartFrame({ height: 64, state: "unavailable", detail: "Equity series is not read from the native result archive yet.", yLabels: [] })}
      <p class="note">Producer metrics are not reconstructed by TraderCockpit; the exact native result archive is preserved in custody.</p>`;
  }
  return zoneCard("build-backtest", {
    actions: viewAll(researchPath("validate", "overview")),
    body,
    footer: footLink(researchPath("evolution"), "Go to Builder", { tone: "blue" }),
  });
}

function propSimulationCard(runtime) {
  const record = runtime?.prop_simulation;
  const chipLabel = !runtime
    ? "Checking…"
    : record?.status === "ready"
      ? "Connected"
      : readable(record?.reason_code, "Not connected");
  const chipTone = !runtime ? "pending" : toneForStatus(record?.status || "unavailable");
  const title = record?.status === "ready" ? "Simulation account" : "No simulation account";
  const subtitle = record?.detail || "Prop-firm / paper simulation producer";
  const chartDetail = record?.detail || "Connect a simulation account in Operate.";
  const body = !runtime
    ? unavailable("Checking simulation status…", "Waiting for the canonical /api/status read model.", { tone: "pending", compact: true })
    : `<div class="list-row" style="padding-top:0"><span class="row-title"><strong>${escapeHtml(title)}</strong><span>${escapeHtml(subtitle)}</span></span>${chip(chipLabel, chipTone)}</div>
    <div class="metric-grid">${metricCell("Balance")}${metricCell("P&L")}</div>
    ${chartFrame({ height: 64, state: record?.status === "ready" ? "ready" : "unavailable", detail: chartDetail, yLabels: [] })}
    <div class="bar-row"><span class="bar-label">Challenge progress</span><div class="bar tone-green"><i style="width:0%"></i></div><span class="bar-value">—</span></div>`;
  return zoneCard("prop-simulation", {
    actions: viewAll("/operate"),
    body,
    footer: `<span class="note">Day — of —</span>${chip(readable(record?.reason_code, "Rules —"), chipTone)}`,
  });
}

function radarEmpty() {
  const points = 6;
  const rings = [1, 0.66, 0.33];
  const cx = 80; const cy = 70; const r = 56;
  const polygon = (scale) => Array.from({ length: points }, (_, index) => {
    const angle = (Math.PI * 2 * index) / points - Math.PI / 2;
    return `${(cx + Math.cos(angle) * r * scale).toFixed(1)},${(cy + Math.sin(angle) * r * scale).toFixed(1)}`;
  }).join(" ");
  return `<div class="radar" aria-label="No graded evidence yet"><svg viewBox="0 0 160 140">${rings.map((scale) => `<polygon points="${polygon(scale)}"/>`).join("")}${Array.from({ length: points }, (_, index) => { const angle = (Math.PI * 2 * index) / points - Math.PI / 2; return `<line x1="${cx}" y1="${cy}" x2="${(cx + Math.cos(angle) * r).toFixed(1)}" y2="${(cy + Math.sin(angle) * r).toFixed(1)}"/>`; }).join("")}<polygon class="radar-empty" points="${polygon(0.18)}"/></svg></div>`;
}

function proofEvidenceCard(snapshot) {
  const count = snapshot.proofs.length;
  const unread = snapshot.proofs.filter((proof) => proof.producer_validation_outcome === "producer_result_captured_outcome_unread").length;
  const grades = ["Performance", "Robustness", "Overfitting", "Data Quality"];
  const status = snapshot.phase === "loading"
    ? chip("Reading…", "pending")
    : snapshot.failures.proofs
      ? chip("Read failed", "error")
      : chip(`${count} proof${count === 1 ? "" : "s"}`, count ? "ready" : "unavailable");
  const body = `<div class="grid grid-2" style="gap:8px">
      <div class="grade-list">${grades.map((grade) => `<div><span class="g-label">${escapeHtml(grade)}</span><div class="grade"><b title="Not graded: TraderCockpit does not compute a verdict; the producer outcome is preserved unread">—</b><small>Not graded</small></div></div>`).join("")}</div>
      ${radarEmpty()}
    </div>
    <p class="note">${escapeHtml(count ? `${unread} of ${count} proofs preserve the producer validation outcome unread. No pass/fail verdict is inferred.` : "Proof binds Idea → configuration → native job → artifact → result → validation. Create one in Test & Validate → Evidence.")}</p>`;
  return zoneCard("proof-evidence", {
    actions: `${status}${viewAll(researchPath("validate", "evidence"))}`,
    body,
    footer: footLink(researchPath("validate", "evidence"), "View Evidence", { iconName: "external" }),
  });
}

function nextStepFor(job) {
  if (job.state === "submitted") return "Import native output";
  if (job.state === "prepared") return "Awaiting native submission";
  if (job.state === "failed") return "Review control receipt";
  return readable(job.state);
}

function activeBuildsCard(snapshot) {
  const jobs = snapshot.jobs.slice(-4).reverse();
  const jobsTable = jobs.length
    ? table({
      columns: [{ label: "Name" }, { label: "State" }, { label: "Next Step" }, { label: "ETA", align: "right" }],
      rows: jobs.map((job) => ({ cells: [`<strong>Builder · ${escapeHtml(shortId(job.configuration_revision || job.revision, 8))}</strong>`, chip(readable(job.state), job.state === "submitted" ? "ready" : job.state === "failed" ? "error" : "pending"), escapeHtml(nextStepFor(job)), `<span class="tone-text-dim" title="Native Builder does not report progress to TraderCockpit">—</span>`] })),
    })
    : "";
  const body = `${jobsTable}${unavailable("Reading pipeline state…", "Lifecycle counts come from the canonical Research custody catalogs.", { tone: "pending", compact: true })}`;
  return zoneCard("active-builds", {
    body,
    footer: footLink(researchPath("evolution"), "View All Builds", { tone: "orange" }),
  });
}

function candidateReviewCard(snapshot) {
  const candidates = snapshot.candidates.slice(-4).reverse();
  const rows = candidates.map((candidate) => ({
    cells: [
      `<span class="row-title"><strong>${escapeHtml(candidate.archive_name)}</strong><span>SQX ${escapeHtml(candidate.sqx_build)} · ${escapeHtml(shortId(candidate.strategy_sha256, 8))}</span></span>`,
      `<span class="tone-text-dim" title="No native score is read for this Candidate yet">—</span>`,
      linkButton(researchPath("evolution"), "Inspect", { className: "button-small" }),
    ],
  }));
  const body = `${table({ columns: [{ label: "Strategy" }, { label: "Score", align: "right" }, { label: "Decision", align: "right" }], rows, empty: snapshot.phase === "loading" ? "Reading Candidate custody…" : "No imported native Candidates yet." })}${unavailable("Reading promotion state…", "Promotion after Proof is Delivery custody, distinct from export and deployment.", { tone: "pending", compact: true })}`;
  return zoneCard("candidate-review", {
    actions: viewAll(researchPath("evolution")),
    body,
    footer: footLink(researchPath("evolution"), "Go to Review", { tone: "green" }),
  });
}

// System Health — initial rows rendered from the same /api/status payload the binder reads,
// in the binder's row format so it can replace them in place.
const HEALTH_ROWS = Object.freeze([
  ["application", "TraderCockpit application", "operate"],
  ["research_backend", "Research backend", "research"],
  ["research_custody", "Research custody", "layers"],
  ["native_execution", "Native execution", "play"],
  ["market_data", "Live market data", "activity"],
  ["provider", "Model provider", "bot"],
  ["account", "Consumer account", "crown"],
  ["model", "Model access", "bot"],
  ["extensions", "Extensions", "grid"],
]);

function healthRecord(payload, key) {
  if (key === "native_execution") {
    const execution = payload.research_backend?.execution;
    if (!execution || typeof execution !== "object") return { status: "unavailable", reason_code: "execution_state_missing" };
    return execution.available === true ? { status: "ready", reason_code: null } : { status: "unavailable", reason_code: execution.reason_code || "execution_unavailable" };
  }
  return payload[key] && typeof payload[key] === "object" ? payload[key] : { status: "unavailable", reason_code: "missing" };
}

export function healthValue(record, key) {
  if (key === "research_backend" && record.status === "ready") return `Ready · StrategyQuant X ${record.build}`;
  if (key === "native_execution" && record.status === "unavailable") return record.reason_code ? `Disabled · ${readable(record.reason_code)}` : "Disabled";
  const label = readable(record.status, "Unavailable");
  return record.reason_code ? `${label} · ${readable(record.reason_code)}` : label;
}

export function renderSystemHealthRows(payload) {
  return `<div class="health-list" data-home-system-status data-system-status="loaded">${HEALTH_ROWS.map(([key, label, iconName]) => {
    const record = healthRecord(payload, key);
    return `<div class="health-row stat-row" data-runtime-component="${escapeHtml(key.replaceAll("_", "-"))}" data-runtime-state="${escapeHtml(record.status)}">${icon(iconName, { size: 14 })}<strong>${escapeHtml(label)}</strong><span class="health-value">${escapeHtml(healthValue(record, key))}</span><span class="health-mark">${icon(record.status === "ready" || record.status === "current" ? "check" : "warn", { size: 14 })}</span></div>`;
  }).join("")}</div>`;
}

function systemHealthCard(statusState, runtime) {
  const body = runtime
    ? renderSystemHealthRows(runtime)
    : `<div data-home-system-status data-system-status="${statusState.phase === "failed" ? "error" : "pending"}">${statusState.phase === "failed"
      ? unavailable("Runtime status unavailable", "The canonical /api/status read failed; no component readiness is inferred.", { tone: "error", compact: true })
      : unavailable("Checking runtime status", "Waiting for the canonical backend status read model.", { tone: "pending", compact: true })}</div>`;
  return zoneCard("system-health", {
    body,
    footer: footLink("/settings", "View Status", { tone: "blue" }),
  });
}

function assistantCard(runtime) {
  return zoneCard("assistant", {
    actions: tag("Apollo", "purple"),
    body: `${renderAssistantWidget(runtime)}<p class="note">The assistant explains cockpit read models and never owns producer truth or mutates native state.</p>`,
    className: "is-assistant",
  });
}

export function renderHome(route, { statusState, snapshotState, runtime }) {
  return `${pageTitle("Cockpit Home")}
    <div class="home-top">${renderHero()}${renderRecentActivity(snapshotState)}</div>
    <section class="home-board" data-home-board data-home-zone-count="${HOME_ZONES.length}">
      ${researchCard(snapshotState)}
      ${buildBacktestCard(snapshotState)}
      ${propSimulationCard(runtime)}
      ${proofEvidenceCard(snapshotState)}
      ${activeBuildsCard(snapshotState)}
      ${candidateReviewCard(snapshotState)}
      ${systemHealthCard(statusState, runtime)}
      ${assistantCard(runtime)}
    </section>`;
}
