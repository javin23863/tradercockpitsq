// Cockpit verdict read model (`tc.research-cockpit-verdict.v1`).
// The backend computes the seven-stage verdict per completed native Historical Result
// from the exact native trade records and the approved native acceptance conditions.
// This module only reads it and shapes it for the Test & Validate surface.

const HISTORICAL_RESULTS_API_PATH = "/api/research/historical-results";
export const COCKPIT_VERDICT_SCHEMA = "tc.research-cockpit-verdict.v1";
const VERDICT_FETCH_LIMIT = 20;

export const STAGE_STATE_TONE = Object.freeze({
  pass: "ready",
  fail: "error",
  incomplete: "warn",
  not_run: "pending",
});

export const STAGE_STATE_LABEL = Object.freeze({
  pass: "Pass",
  fail: "Fail",
  incomplete: "Incomplete",
  not_run: "Not run",
});

async function readJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

export async function fetchCockpitVerdict(entityId, fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(`${HISTORICAL_RESULTS_API_PATH}?entityId=${encodeURIComponent(entityId)}`, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok || !payload) {
    throw new Error(payload?.detail || `Historical result read failed (${response?.status ?? "no response"})`);
  }
  const readback = payload.cockpit_verdict;
  const verdict = readback?.state === "available" && readback.payload?.schema === COCKPIT_VERDICT_SCHEMA ? readback.payload : null;
  return {
    result: payload,
    verdict,
    state: verdict ? "available" : "unavailable",
    reason: verdict ? null : (readback?.detail || readback?.reason_code || "cockpit verdict unavailable"),
  };
}

// Verdicts for the most recent completed results (bounded so the overview stays responsive).
export async function fetchCockpitVerdicts(results, fetchImpl = globalThis.fetch, limit = VERDICT_FETCH_LIMIT) {
  const completed = results.filter((result) => result.state === "completed").slice(-limit).reverse();
  const settled = await Promise.allSettled(completed.map((result) => fetchCockpitVerdict(result.entity_id, fetchImpl)));
  return settled.map((entry, index) => (
    entry.status === "fulfilled"
      ? entry.value
      : { result: completed[index], verdict: null, state: "unavailable", reason: entry.reason instanceof Error ? entry.reason.message : "read failed" }
  ));
}

export function stageOf(verdict, stageId) {
  return verdict?.stages?.find((stage) => stage.id === stageId) || null;
}

// Per-stage tally across all fetched verdicts.
export function stageTally(entries, stageId) {
  const tally = { pass: 0, fail: 0, incomplete: 0, not_run: 0, total: 0 };
  for (const entry of entries) {
    const stage = stageOf(entry.verdict, stageId);
    if (!stage) continue;
    tally.total += 1;
    if (stage.state in tally) tally[stage.state] += 1;
  }
  return tally;
}

export function verdictTally(entries) {
  const tally = { pass: 0, fail: 0, incomplete: 0, in_progress: 0, total: 0 };
  for (const entry of entries) {
    const state = entry.verdict?.verdict?.state;
    if (!state) continue;
    tally.total += 1;
    if (state in tally) tally[state] += 1;
  }
  return tally;
}

export function average(values) {
  const numbers = values.map(Number).filter(Number.isFinite);
  if (!numbers.length) return null;
  return numbers.reduce((sum, value) => sum + value, 0) / numbers.length;
}

export function formatNumber(value, digits = 2) {
  if (value === null || value === undefined || !Number.isFinite(Number(value))) return "—";
  return Number(value).toLocaleString("en-US", { minimumFractionDigits: digits, maximumFractionDigits: digits });
}

export function formatMoney(value) {
  if (value === null || value === undefined || !Number.isFinite(Number(value))) return "—";
  const number = Number(value);
  const sign = number < 0 ? "−" : "";
  return `${sign}${Math.abs(number).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

export function formatPeriod(statistics) {
  const first = statistics?.first_open_time;
  const last = statistics?.last_close_time;
  if (!Number.isFinite(first) || !Number.isFinite(last)) return "—";
  const start = new Date(first).toISOString().slice(0, 10);
  const end = new Date(last).toISOString().slice(0, 10);
  return `${start} → ${end}`;
}

export function checkValueLabel(check) {
  if (check.value === null || check.value === undefined) return "—";
  const unit = check.unit || "";
  return `${typeof check.value === "number" ? formatNumber(check.value, Number.isInteger(check.value) ? 0 : 2) : String(check.value)}${unit}`;
}
