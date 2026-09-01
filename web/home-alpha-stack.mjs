const RESEARCH_CANDIDATES_API_PATH = "/api/research/candidates";
const CANDIDATE_CATALOG_SCHEMA = "tc.research-candidate-catalog.v1";
const CANDIDATE_SCHEMA = "tc.research-candidate.v1";
const DIGEST_RE = /^[0-9a-f]{64}$/;
const CANDIDATE_ENTITY_RE = /^tc-research:candidate:v1:[0-9a-f-]{36}$/;
const CANDIDATE_REVISION_RE = /^tc-revision:candidate:v1:[0-9a-f]{64}$/;

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function object(value) {
  return value && typeof value === "object" && !Array.isArray(value) ? value : null;
}

function exactCandidate(value) {
  const candidate = object(value);
  if (!candidate || candidate.schema !== CANDIDATE_SCHEMA) throw new Error("Candidate schema mismatch");
  if (typeof candidate.entity_id !== "string" || !CANDIDATE_ENTITY_RE.test(candidate.entity_id)) {
    throw new Error("Candidate entity identity is invalid");
  }
  if (typeof candidate.revision !== "string" || !CANDIDATE_REVISION_RE.test(candidate.revision)) {
    throw new Error("Candidate revision identity is invalid");
  }
  if (typeof candidate.archive_name !== "string" || !candidate.archive_name || !candidate.archive_name.toLowerCase().endsWith(".sqx")) {
    throw new Error("Candidate archive identity is invalid");
  }
  if (typeof candidate.archive_sha256 !== "string" || !DIGEST_RE.test(candidate.archive_sha256)) {
    throw new Error("Candidate archive digest is invalid");
  }
  if (typeof candidate.strategy_sha256 !== "string" || !DIGEST_RE.test(candidate.strategy_sha256)) {
    throw new Error("Candidate strategy digest is invalid");
  }
  if (typeof candidate.sqx_build !== "string" || !candidate.sqx_build.trim()) {
    throw new Error("Candidate producer build is invalid");
  }
  return Object.freeze({
    entity_id: candidate.entity_id,
    revision: candidate.revision,
    archive_name: candidate.archive_name,
    archive_sha256: candidate.archive_sha256,
    strategy_sha256: candidate.strategy_sha256,
    sqx_build: candidate.sqx_build,
  });
}

export function parseHomeAlphaCandidates(payload) {
  const catalog = object(payload);
  if (!catalog || catalog.schema !== CANDIDATE_CATALOG_SCHEMA || !Array.isArray(catalog.candidates)) {
    throw new Error("Candidate catalog schema mismatch");
  }
  const candidates = catalog.candidates.map(exactCandidate);
  const entities = new Set();
  const revisions = new Set();
  for (const candidate of candidates) {
    if (entities.has(candidate.entity_id)) throw new Error("Candidate catalog contains duplicate entity identity");
    if (revisions.has(candidate.revision)) throw new Error("Candidate catalog contains duplicate revision identity");
    entities.add(candidate.entity_id);
    revisions.add(candidate.revision);
  }
  return Object.freeze({
    schema: CANDIDATE_CATALOG_SCHEMA,
    candidates: Object.freeze(candidates),
  });
}

export async function fetchHomeAlphaCandidates(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Alpha Stack fetch is unavailable");
  const response = await fetchImpl(RESEARCH_CANDIDATES_API_PATH, { headers: { accept: "application/json" } });
  if (!response?.ok) throw new Error(`Candidate catalog request failed: ${response?.status ?? "unknown"}`);
  return parseHomeAlphaCandidates(await response.json());
}

function unavailableStage(label, reason) {
  return `<div class="stat-row" data-alpha-stage="${escapeHtml(label.toLowerCase().replaceAll(" ", "-"))}" data-alpha-stage-state="unavailable"><span>${escapeHtml(label)}</span><strong>Unavailable · ${escapeHtml(reason)}</strong></div>`;
}

function candidateRows(candidates) {
  if (!candidates.length) {
    return `<div class="stat-row" data-alpha-stage="research-candidates" data-alpha-stage-state="current"><span>Research Candidates</span><strong>Current catalog · 0</strong></div><p class="panel-description">No current native Research Candidate custody exists.</p>`;
  }
  const identities = candidates.map((candidate) => `<div class="alpha-candidate" data-alpha-candidate><div><span>Candidate entity</span><code>${escapeHtml(candidate.entity_id)}</code></div><div><span>Current revision</span><code>${escapeHtml(candidate.revision)}</code></div><div><span>Native archive</span><code>${escapeHtml(candidate.archive_name)}</code></div><div><span>Strategy SHA-256</span><code>${escapeHtml(candidate.strategy_sha256)}</code></div><div><span>Producer build</span><code>${escapeHtml(candidate.sqx_build)}</code></div></div>`).join("");
  return `<div class="stat-row" data-alpha-stage="research-candidates" data-alpha-stage-state="current"><span>Research Candidates</span><strong>Current catalog · ${candidates.length}</strong></div>${identities}`;
}

export function renderHomeAlphaStack(catalog, errorDetail = "") {
  const candidateBody = catalog
    ? candidateRows(catalog.candidates)
    : `<div class="stat-row" data-alpha-stage="research-candidates" data-alpha-stage-state="unavailable"><span>Research Candidates</span><strong>Unavailable · Candidate custody read failed</strong></div>${errorDetail ? `<p class="panel-description">${escapeHtml(errorDetail)}</p>` : ""}`;
  return `<div data-home-alpha-stack data-alpha-stack-state="${catalog ? "loaded" : "unavailable"}">
    ${candidateBody}
    ${unavailableStage("Promoted Research Strategy", "Promotion authority not connected")}
    ${unavailableStage("Exported Strategy", "Export authority not connected")}
    ${unavailableStage("Deployed / Live Strategy", "Deployment authority not connected")}
    <p class="panel-description">Research Candidate custody is historical/research evidence only. It does not imply promotion, export, deployment, or live execution.</p>
  </div>`;
}

function pendingAlphaStack() {
  return `<div data-home-alpha-stack data-alpha-stack-state="pending"><div class="empty-state"><div class="empty-icon">—</div><div><strong>Checking Alpha Stack</strong><p>Reading exact current Candidate custody without inferring promotion or deployment.</p></div></div></div>`;
}

function replaceAlphaBody(zone, html) {
  const current = zone.querySelector("[data-home-alpha-stack]") || zone.querySelector(".empty-state");
  if (!current) return false;
  current.outerHTML = html;
  return true;
}

async function bindAlphaStack(zone) {
  replaceAlphaBody(zone, pendingAlphaStack());
  try {
    const catalog = await fetchHomeAlphaCandidates();
    if (zone.isConnected) replaceAlphaBody(zone, renderHomeAlphaStack(catalog));
  } catch (error) {
    if (!zone.isConnected) return;
    const detail = error instanceof Error ? error.message : "Candidate custody read failed";
    replaceAlphaBody(zone, renderHomeAlphaStack(null, detail));
  }
}

function mountHomeAlphaStack(root = document) {
  const zone = root.querySelector?.('[data-home-zone="alpha-stack"]');
  if (!zone || zone.dataset.alphaStackBound === "true") return false;
  zone.dataset.alphaStackBound = "true";
  void bindAlphaStack(zone);
  return true;
}

if (typeof document !== "undefined") {
  const app = document.querySelector("#app");
  mountHomeAlphaStack(document);
  if (app && typeof MutationObserver !== "undefined") {
    const observer = new MutationObserver(() => mountHomeAlphaStack(document));
    observer.observe(app, { childList: true, subtree: true });
  }
}
