// One read of the six canonical Research custody catalogs, shared by Home, the status bar,
// and the Research workspaces. Records are passed through unchanged; nothing is derived
// beyond counts. Failures stay per-catalog so a missing authority never hides another.

import { fetchIdeaCatalog } from "./research-ideas.mjs";
import { fetchConfigurationCatalog } from "./research-build.mjs";
import { nativeJobCatalogFromPayload } from "./research-build-launch.mjs";
import { candidateCatalogFromPayload } from "./research-candidates.mjs";
import { fetchHistoricalResults } from "./research-backtest.mjs";
import { fetchProofCatalog } from "./research-proof.mjs";

const NATIVE_JOBS_API_PATH = "/api/research/native-jobs";
const CANDIDATES_API_PATH = "/api/research/candidates";

export const EMPTY_RESEARCH_SNAPSHOT = Object.freeze({
  phase: "loading",
  ideas: Object.freeze([]),
  configurations: Object.freeze([]),
  jobs: Object.freeze([]),
  candidates: Object.freeze([]),
  results: Object.freeze([]),
  proofs: Object.freeze([]),
  failures: Object.freeze({}),
});

async function readCatalog(path, parser, fetchImpl) {
  if (typeof fetchImpl !== "function") throw new Error("Research catalog fetch is unavailable");
  const response = await fetchImpl(path, { headers: { accept: "application/json" } });
  let payload = null;
  try { payload = await response.json(); } catch { payload = null; }
  if (!response?.ok) throw new Error(payload?.detail || `Research catalog request failed: ${response?.status ?? "unknown"}`);
  return parser(payload);
}

export async function fetchResearchSnapshot(fetchImpl = globalThis.fetch) {
  const readers = {
    ideas: async () => (await fetchIdeaCatalog(fetchImpl)).ideas,
    configurations: async () => (await fetchConfigurationCatalog(fetchImpl)).configurations,
    jobs: () => readCatalog(NATIVE_JOBS_API_PATH, nativeJobCatalogFromPayload, fetchImpl),
    candidates: () => readCatalog(CANDIDATES_API_PATH, candidateCatalogFromPayload, fetchImpl),
    results: () => fetchHistoricalResults(fetchImpl),
    proofs: () => fetchProofCatalog(fetchImpl),
  };
  const names = Object.keys(readers);
  const settled = await Promise.allSettled(names.map((name) => readers[name]()));
  const snapshot = { phase: "loaded", failures: {} };
  settled.forEach((result, index) => {
    const name = names[index];
    if (result.status === "fulfilled" && Array.isArray(result.value)) {
      snapshot[name] = Object.freeze([...result.value]);
    } else {
      snapshot[name] = Object.freeze([]);
      snapshot.failures[name] = result.status === "rejected"
        ? (result.reason instanceof Error ? result.reason.message : "Canonical custody read failed")
        : "Canonical custody catalog is malformed";
    }
  });
  if (Object.keys(snapshot.failures).length === names.length) snapshot.phase = "failed";
  else if (Object.keys(snapshot.failures).length) snapshot.phase = "partial";
  snapshot.failures = Object.freeze(snapshot.failures);
  return Object.freeze(snapshot);
}

let currentSnapshot = EMPTY_RESEARCH_SNAPSHOT;

export function setCurrentResearchSnapshot(snapshot) {
  currentSnapshot = snapshot || EMPTY_RESEARCH_SNAPSHOT;
}

export function currentResearchSnapshot() {
  return currentSnapshot;
}

export function countBy(records, key = "state") {
  const result = Object.create(null);
  for (const record of records) result[record?.[key]] = (result[record?.[key]] || 0) + 1;
  return result;
}

export function latestRecord(records) {
  return records.length ? records[records.length - 1] : null;
}
