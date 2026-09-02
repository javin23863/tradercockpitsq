import { fetchIdeaCatalog } from "./research-ideas.mjs";
import { fetchConfigurationCatalog } from "./research-build.mjs";
import { nativeJobCatalogFromPayload } from "./research-build-launch.mjs";
import { candidateCatalogFromPayload } from "./research-candidates.mjs";
import { fetchHistoricalResults } from "./research-backtest.mjs";
import { fetchProofCatalog } from "./research-proof.mjs";

const NATIVE_JOBS = "/api/research/native-jobs";
const CANDIDATES = "/api/research/candidates";

function esc(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

async function json(response) {
  try { return await response.json(); } catch { return null; }
}

async function readCatalog(path, parser, fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Pipeline catalog fetch is unavailable");
  const response = await fetchImpl(path, { headers: { accept: "application/json" } });
  const payload = await json(response);
  if (!response?.ok) throw new Error(payload?.detail || `Pipeline catalog request failed: ${response?.status ?? "unknown"}`);
  return parser(payload);
}

export function fetchHomeNativeJobs(fetchImpl = globalThis.fetch) {
  return readCatalog(NATIVE_JOBS, nativeJobCatalogFromPayload, fetchImpl);
}

export function fetchHomeCandidates(fetchImpl = globalThis.fetch) {
  return readCatalog(CANDIDATES, candidateCatalogFromPayload, fetchImpl);
}

function counts(records, key = "state") {
  const result = Object.create(null);
  for (const record of records) result[record?.[key]] = (result[record?.[key]] || 0) + 1;
  return result;
}

export function summarizeHomePipelineCatalogs({ ideas, configurations, jobs, candidates, results, proofs }) {
  if (!Array.isArray(ideas?.ideas)) throw new Error("Idea catalog entries are invalid");
  if (!Array.isArray(configurations?.configurations)) throw new Error("Configuration catalog entries are invalid");
  for (const value of [jobs, candidates, results, proofs]) {
    if (!Array.isArray(value)) throw new Error("Pipeline catalog entries are invalid");
  }
  const configurationStates = counts(configurations.configurations);
  const jobStates = counts(jobs);
  const resultStates = counts(results);
  return Object.freeze({
    idea: { count: ideas.ideas.length },
    configuration: {
      count: configurations.configurations.length,
      compiled: configurationStates.compiled || 0,
      approved: configurationStates.approved || 0,
    },
    native_job: {
      count: jobs.length,
      prepared: jobStates.prepared || 0,
      submitted: jobStates.submitted || 0,
      failed: jobStates.failed || 0,
    },
    candidate: { count: candidates.length },
    historical_result: {
      count: results.length,
      prepared: resultStates.prepared || 0,
      completed: resultStates.completed || 0,
      failed: resultStates.failed || 0,
      validation_not_run: results.filter((item) => item?.validation_state === "not_run").length,
    },
    proof: {
      count: proofs.length,
      outcome_unread: proofs.filter((item) => item?.producer_validation_outcome === "producer_result_captured_outcome_unread").length,
    },
  });
}

export async function fetchHomePipelineSnapshot(fetchImpl = globalThis.fetch) {
  const names = ["ideas", "configurations", "jobs", "candidates", "results", "proofs"];
  const settled = await Promise.allSettled([
    fetchIdeaCatalog(fetchImpl),
    fetchConfigurationCatalog(fetchImpl),
    fetchHomeNativeJobs(fetchImpl),
    fetchHomeCandidates(fetchImpl),
    fetchHistoricalResults(fetchImpl),
    fetchProofCatalog(fetchImpl),
  ]);
  const values = Object.create(null);
  const failures = Object.create(null);
  settled.forEach((result, index) => {
    const name = names[index];
    if (result.status === "fulfilled") values[name] = result.value;
    else failures[name] = result.reason instanceof Error ? result.reason.message : "Canonical lifecycle read failed";
  });
  if (Object.keys(failures).length) return Object.freeze({ phase: "partial", failures, values });
  return Object.freeze({ phase: "loaded", summary: summarizeHomePipelineCatalogs(values) });
}

function badge(label, tone = "ready") {
  return `<span class="status-badge status-${esc(tone)}"><span class="status-dot"></span>${esc(label)}</span>`;
}

function row(label, value) {
  return `<div class="stat-row"><span>${esc(label)}</span><strong>${esc(value)}</strong></div>`;
}

function item(key, title, body) {
  return `<div class="requirement-item" data-home-pipeline-stage="${esc(key)}"><div><strong>${esc(title)}</strong>${body.badge}</div>${body.rows || ""}<p>${esc(body.detail)}</p></div>`;
}

function loadedHtml(summary) {
  return [
    item("idea", "Idea", { badge: badge(`Current custody · ${summary.idea.count}`), detail: "Current immutable Research Idea custody only." }),
    item("configuration", "Configuration", { badge: badge(`Current custody · ${summary.configuration.count}`), rows: row("Compiled", summary.configuration.compiled) + row("Approved", summary.configuration.approved), detail: "Approval is exact configuration custody, not a validation verdict." }),
    item("native-job", "Native Builder job", { badge: badge(`Current custody · ${summary.native_job.count}`), rows: row("Prepared", summary.native_job.prepared) + row("Submitted", summary.native_job.submitted) + row("Failed", summary.native_job.failed), detail: "Submitted means native control receipts completed; it does not mean validation passed." }),
    item("candidate", "Candidate", { badge: badge(`Current custody · ${summary.candidate.count}`), detail: "Current imported native Candidate custody; no rank, champion, or promotion is inferred." }),
    item("historical-result", "Historical Result", { badge: badge(`Current custody · ${summary.historical_result.count}`), rows: row("Prepared", summary.historical_result.prepared) + row("Execution completed", summary.historical_result.completed) + row("Execution failed", summary.historical_result.failed) + row("Validation not run", summary.historical_result.validation_not_run), detail: "Native Retester completion remains distinct from validation outcome." }),
    item("proof", "Research Proof", { badge: badge(`Current custody · ${summary.proof.count}`), rows: row("Producer validation outcome unread", summary.proof.outcome_unread), detail: "Proof preserves the exact historical chain; it does not reconstruct an SQX pass/fail verdict." }),
    item("deployment", "Promotion / Deployment", { badge: badge("Unavailable", "unavailable"), detail: "Promotion/deployment pipeline authority not connected." }),
  ].join("");
}

function partialHtml(snapshot) {
  const map = {
    ideas: "Idea",
    configurations: "Configuration",
    jobs: "Native Builder job",
    candidates: "Candidate",
    results: "Historical Result",
    proofs: "Research Proof",
  };
  return Object.entries(map).map(([key, title]) => {
    if (snapshot.failures[key]) return item(key, title, { badge: badge("Read failed", "unavailable"), detail: snapshot.failures[key] });
    return item(key, title, { badge: badge("Read available"), detail: "Canonical lifecycle authority responded; the aggregate summary is withheld because another stage failed." });
  }).join("") + item("deployment", "Promotion / Deployment", { badge: badge("Unavailable", "unavailable"), detail: "Promotion/deployment pipeline authority not connected." });
}

export function renderHomePipelineOverview(snapshot) {
  if (!snapshot) return '<div class="empty-state"><div class="empty-icon">…</div><div><strong>Reading canonical pipeline state</strong><p>No frontend phase count or verdict is inferred.</p></div></div>';
  const stages = snapshot.phase === "loaded" ? loadedHtml(snapshot.summary) : partialHtml(snapshot);
  return `<div data-home-pipeline-overview>${stages}<p class="field-help">Research custody, submitted execution, and completed native runs are lifecycle evidence only. Home does not convert them into validation pass, promotion, deployment, or live status.</p></div>`;
}

export function ensureHomePipelineBody(zone) {
  if (!zone || typeof zone.querySelector !== "function") return null;
  const existing = zone.querySelector("[data-home-pipeline-body]");
  if (existing) return existing;
  const placeholder = zone.querySelector(".empty-state");
  if (!placeholder || typeof globalThis.document?.createElement !== "function") return null;
  const body = globalThis.document.createElement("div");
  body.setAttribute("data-home-pipeline-body", "");
  placeholder.replaceWith(body);
  return body;
}

let activeZone = null;
let generation = 0;

async function bindHomePipelineOverview() {
  const zone = globalThis.document?.querySelector?.('[data-home-zone="active-builds"]');
  if (!zone) { activeZone = null; return; }
  if (zone === activeZone) return;
  const body = ensureHomePipelineBody(zone);
  if (!body) return;
  activeZone = zone;
  const current = ++generation;
  body.innerHTML = renderHomePipelineOverview(null);
  const snapshot = await fetchHomePipelineSnapshot();
  if (current !== generation || zone !== activeZone || !zone.isConnected || !body.isConnected) return;
  body.innerHTML = renderHomePipelineOverview(snapshot);
}

if (typeof document !== "undefined") {
  const observer = new MutationObserver(() => { void bindHomePipelineOverview(); });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void bindHomePipelineOverview();
}
