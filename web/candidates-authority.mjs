const ZONES = ["build", "evolution", "models", "custody"];
const BUILDER_SEARCH_START_API_PATH = "/api/builder-searches";
const BUILDER_CANDIDATES_API_PATH = "/api/builder-candidates";
const BUILDER_SEARCH_SCHEMA = "tc.builder-search.v1";
const BUILDER_CANDIDATES_SCHEMA = "tc.builder-candidates.v1";
const BUILDER_SEARCH_IMPLEMENTATION = "tradercockpit.builder-search.v2";

const BOUNDED_BUILD_CONFIG = Object.freeze({
  population_size_per_island: 4,
  maximum_generations: 1,
  crossover_probability_pct: 0,
  mutation_probability_pct: 0,
  island_count: 1,
  migration_rate_pct: 0,
  decimation_coefficient: 1,
  fresh_blood_replace_similar: false,
  fresh_blood_replace_weakest: false,
});

export const CANDIDATE_AUTHORITY_ZONES = Object.freeze([...ZONES]);
export const CANDIDATE_SEARCH_MODES = Object.freeze(["bounded", "evolution"]);

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function badge(label = "Status pending", tone = "pending") {
  return `<span class="status-badge status-${escapeHtml(tone)}"><span class="status-dot"></span>${escapeHtml(label)}</span>`;
}

function boundary(title, detail, state = "pending") {
  return `<div class="empty-state" data-builder-boundary-state="${escapeHtml(state)}"><div class="empty-icon">—</div><div><strong>${escapeHtml(title)}</strong><p>${escapeHtml(detail)}</p></div></div>`;
}

function searchAction(label, mode) {
  return `<button class="button button-primary" type="button" data-builder-search-start="${escapeHtml(mode)}">${escapeHtml(label)}</button>`;
}

function panel({ zone, eyebrow, title, description, body, accent, status = "Status pending", tone = "pending" }) {
  return `<article class="panel candidates-zone candidates-zone-${zone}" data-candidates-zone="${zone}" data-accent="${accent}"><div class="panel-heading"><div><p class="eyebrow">${escapeHtml(eyebrow)}</p><h2>${escapeHtml(title)}</h2></div>${badge(status, tone)}</div><p class="panel-description">${escapeHtml(description)}</p>${body}</article>`;
}

function requireStrategyRef(strategyRef) {
  if (typeof strategyRef !== "string" || strategyRef.length === 0) {
    throw new TypeError("Candidates authority requires an exact requested strategy reference");
  }
  return strategyRef;
}

function requireContentRef(value, kind, name) {
  const prefix = `tc:${kind}:v1:sha256:`;
  const digest = typeof value === "string" && value.startsWith(prefix)
    ? value.slice(prefix.length)
    : "";
  if (typeof value !== "string" || !value.startsWith(prefix) || !/^[0-9a-f]{64}$/.test(digest)) {
    throw new Error(`${name} is not a ${kind} v1 content address`);
  }
  return value;
}

function normalizeCandidate(record) {
  if (!record || typeof record !== "object") throw new Error("Invalid Builder candidate record");
  const objective = record.objective_values?.construction_fit;
  if (typeof objective !== "string" || !/^-?\d+(?:\.\d+)?$/.test(objective)) {
    throw new Error("Builder candidate is missing exact construction_fit custody");
  }
  if (!Number.isInteger(record.rank) || record.rank < 1) throw new Error("Builder candidate rank is invalid");
  for (const key of ["island_index", "generation_index", "node_index"]) {
    if (!Number.isInteger(record[key]) || record[key] < 0) throw new Error(`Builder candidate ${key} is invalid`);
  }
  if (typeof record.source !== "string" || record.source.length === 0) throw new Error("Builder candidate source is invalid");
  if (!Array.isArray(record.parent_candidate_refs)) throw new Error("Builder candidate parent custody is invalid");
  record.parent_candidate_refs.forEach((value, index) => requireContentRef(value, "candidate", `parent_candidate_refs[${index}]`));
  return {
    ...record,
    candidate_ref: requireContentRef(record.candidate_ref, "candidate", "candidate_ref"),
    strategy_ref: requireContentRef(record.strategy_ref, "strategy", "strategy_ref"),
    lineage_ref: requireContentRef(record.lineage_ref, "builder-lineage", "lineage_ref"),
    objective_values: { construction_fit: objective },
    parent_candidate_refs: [...record.parent_candidate_refs],
  };
}

export function normalizeBuilderSearch(payload) {
  if (!payload || typeof payload !== "object" || payload.schema !== BUILDER_SEARCH_SCHEMA) {
    throw new Error("Unexpected Builder search schema");
  }
  if (payload.implementation !== BUILDER_SEARCH_IMPLEMENTATION) {
    throw new Error("Unexpected Builder search implementation revision");
  }
  requireContentRef(payload.search_ref, "builder-search", "search_ref");
  requireContentRef(payload.config_ref, "builder-config", "config_ref");
  requireStrategyRef(payload.requested_strategy_ref);
  if (!new Set(["created", "running", "complete"]).has(payload.status)) throw new Error("Builder search status is invalid");
  if (typeof payload.stage !== "string" || payload.stage.length === 0) throw new Error("Builder search stage is invalid");
  if (!Number.isInteger(payload.generation) || payload.generation < 0) throw new Error("Builder search generation is invalid");
  if (!Number.isInteger(payload.restart_count) || payload.restart_count < 0) throw new Error("Builder search restart count is invalid");
  if (!Number.isInteger(payload.evaluations) || payload.evaluations < 0) throw new Error("Builder search evaluation count is invalid");
  if (!Array.isArray(payload.candidates)) throw new Error("Builder search candidates are invalid");
  const candidates = payload.candidates.map(normalizeCandidate);
  if (payload.candidate_count !== candidates.length) throw new Error("Builder search candidate count does not match records");
  const candidateRefs = candidates.map((candidate) => candidate.candidate_ref);
  if (new Set(candidateRefs).size !== candidateRefs.length) throw new Error("Builder search contains duplicate candidate identities");
  candidates.forEach((candidate, index) => {
    if (candidate.rank !== index + 1) throw new Error("Builder search candidate ranks are not contiguous");
  });
  return { ...payload, candidates };
}

export function normalizeBuilderCandidates(payload) {
  if (!payload || typeof payload !== "object" || payload.schema !== BUILDER_CANDIDATES_SCHEMA) {
    throw new Error("Unexpected Builder candidate catalog schema");
  }
  const requestedStrategyRef = requireStrategyRef(payload.requested_strategy_ref);
  if (!Array.isArray(payload.searches) || !Array.isArray(payload.candidates)) {
    throw new Error("Invalid Builder candidate catalog payload");
  }
  const searches = payload.searches.map(normalizeBuilderSearch);
  const searchByRef = new Map();
  for (const search of searches) {
    if (search.requested_strategy_ref !== requestedStrategyRef) {
      throw new Error("Builder search belongs to another requested strategy reference");
    }
    if (searchByRef.has(search.search_ref)) {
      throw new Error("Builder candidate catalog contains duplicate search identities");
    }
    searchByRef.set(search.search_ref, search);
  }

  const candidates = payload.candidates.map((record) => {
    const candidate = normalizeCandidate(record);
    const searchRef = requireContentRef(record.search_ref, "builder-search", "candidate search_ref");
    const configRef = requireContentRef(record.config_ref, "builder-config", "candidate config_ref");
    if (!new Set(["created", "running", "complete"]).has(record.search_status)) {
      throw new Error("Builder candidate search status is invalid");
    }
    const search = searchByRef.get(searchRef);
    if (!search) throw new Error("Builder candidate references a search missing from the catalog");
    if (search.config_ref !== configRef) throw new Error("Builder candidate config_ref disagrees with its search");
    if (search.status !== record.search_status) throw new Error("Builder candidate search status disagrees with its search");
    if (!search.candidates.some((item) => item.candidate_ref === candidate.candidate_ref)) {
      throw new Error("Builder candidate is not present in its referenced search");
    }
    return { ...candidate, search_ref: searchRef, config_ref: configRef, search_status: record.search_status };
  });
  const candidateRefs = candidates.map((candidate) => candidate.candidate_ref);
  if (new Set(candidateRefs).size !== candidateRefs.length) {
    throw new Error("Builder candidate catalog contains duplicate candidate identities");
  }
  return { ...payload, searches, candidates };
}

export function builderCandidatesPath(strategyRef) {
  const params = new URLSearchParams();
  params.set("strategyRef", requireStrategyRef(strategyRef));
  return `${BUILDER_CANDIDATES_API_PATH}?${params.toString()}`;
}

export async function fetchBuilderCandidates(strategyRef, fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Fetch is not available");
  const requestedStrategyRef = requireStrategyRef(strategyRef);
  const response = await fetchImpl(builderCandidatesPath(requestedStrategyRef), {
    headers: { accept: "application/json" },
  });
  if (!response.ok) throw new Error(`Builder candidate lookup failed (${response.status})`);
  const catalog = normalizeBuilderCandidates(await response.json());
  if (catalog.requested_strategy_ref !== requestedStrategyRef) {
    throw new Error("Builder candidate response belongs to another requested strategy reference");
  }
  return catalog;
}

export async function startBuilderSearch(strategyRef, config = {}, fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Fetch is not available");
  const requestedStrategyRef = requireStrategyRef(strategyRef);
  if (!config || typeof config !== "object" || Array.isArray(config)) throw new Error("Builder config must be an object");
  const response = await fetchImpl(BUILDER_SEARCH_START_API_PATH, {
    method: "POST",
    headers: {
      accept: "application/json",
      "content-type": "application/json",
    },
    body: JSON.stringify({ strategyRef: requestedStrategyRef, config }),
  });
  let payload = null;
  try {
    payload = await response.json();
  } catch {
    // Error text below is sufficient when the server has not exposed the route yet.
  }
  if (!response.ok) throw new Error(payload?.detail || `Builder search failed (${response.status})`);
  const search = normalizeBuilderSearch(payload);
  if (search.requested_strategy_ref !== requestedStrategyRef) {
    throw new Error("Builder search response belongs to another requested strategy reference");
  }
  return search;
}

export function renderCandidatesAuthority(strategyRef) {
  requireStrategyRef(strategyRef);
  const encodedRef = encodeURIComponent(strategyRef);
  const signalsPath = `/strategies/${encodedRef}/signals`;

  return `<section class="candidates-authority" data-candidates-authority data-requested-strategy-ref="${escapeHtml(strategyRef)}"><section class="context-callout candidates-authority-callout"><span class="callout-icon">◇</span><div><span class="eyebrow">Candidate authority boundary</span><strong>Generated candidates remain producer-bound</strong><span>Bounded Build and Evolutionary Search use explicit TraderCockpit-owned BuilderSearchConfigV1 behavior. Native SQX settings are imported only when the backend provides that provenance; this page does not infer them from the route reference.</span></div><span class="context-lock">BUILDER BOUND</span></section><section class="dashboard-grid three-up candidates-authority-grid">${panel({ zone: "build", eyebrow: "Manual / bounded build", title: "Builder", description: "Run a small deterministic TraderCockpit construction sweep through the canonical Builder search service. This action does not claim native SQX Builder equivalence.", body: `${boundary("Bounded product build", "Uses an explicit four-candidate, one-generation TraderCockpit preset with crossover, mutation, migration, and fresh-blood replacement disabled.", "ready")}<div class="detail-actions">${searchAction("Build bounded candidates", "bounded")}</div><p class="field-help" data-builder-action-state="bounded">Ready to submit the exact requested strategy reference to the Builder backend.</p>`, accent: "orange", status: "Action ready", tone: "pending" })}${panel({ zone: "evolution", eyebrow: "Evolutionary Search", title: "Search handoff", description: "Run the canonical product-owned evolutionary search using BuilderSearchConfigV1 backend defaults, including real selection, crossover/mutation, evaluation, ranking, and candidate custody.", body: `${boundary("Evolutionary product search", "The backend owns the search configuration, objective, generation loop, restart policy, ranking, and immutable candidate custody.", "ready")}<div class="detail-actions">${searchAction("Run Evolutionary Search", "evolution")}</div><p class="field-help" data-builder-action-state="evolution">Ready to start the bounded backend search.</p>`, accent: "purple", status: "Action ready", tone: "pending" })}${panel({ zone: "models", eyebrow: "Machine Learning", title: "Eligible model assistance", description: "Models may participate only where authoritative strategy/catalog capability says they are eligible; prediction, sizing, filtering, and validation remain distinct semantics.", body: `${boundary("Model eligibility pending", "The requested strategy reference does not establish an attached model or a model-generated candidate.")}<a class="button button-secondary" href="${escapeHtml(signalsPath)}" data-route="${escapeHtml(signalsPath)}">Open Signals &amp; Models</a>`, accent: "cyan" })}</section>${panel({ zone: "custody", eyebrow: "Candidate custody", title: "Candidate records", description: "Candidate identity, lineage, generation source, objective values, and rank come only from durable Builder search records verified against immutable object custody.", body: `<div data-builder-candidate-catalog data-builder-candidate-catalog-state="loading">${boundary("Reading persisted candidate records", "The page is requesting the canonical Builder candidate catalog for this exact opaque reference.")}</div>`, accent: "green", status: "Catalog", tone: "pending" })}</section>`;
}

function makeText(tag, text, className = "") {
  const node = document.createElement(tag);
  if (className) node.className = className;
  node.textContent = text;
  return node;
}

function makeField(label, value) {
  const row = document.createElement("div");
  row.className = "run-field";
  row.append(makeText("span", label), makeText("strong", value));
  return row;
}

function renderCatalog(container, catalog) {
  container.replaceChildren();
  container.dataset.builderCandidateCatalogState = "ready";
  if (catalog.candidates.length === 0) {
    const empty = document.createElement("div");
    empty.className = "empty-state";
    empty.append(makeText("div", "—", "empty-icon"));
    const copy = document.createElement("div");
    copy.append(makeText("strong", "No persisted Builder candidates for this reference"));
    copy.append(makeText("p", "Run Bounded Build or Evolutionary Search to create candidate custody."));
    empty.append(copy);
    container.append(empty);
    return;
  }

  const grid = document.createElement("div");
  grid.className = "dashboard-grid two-up";
  for (const candidate of catalog.candidates) {
    const card = document.createElement("article");
    card.className = "panel";
    card.dataset.accent = "green";
    card.dataset.builderCandidateRecord = candidate.candidate_ref;

    const heading = document.createElement("div");
    heading.className = "panel-heading";
    const copy = document.createElement("div");
    copy.append(makeText("p", `Rank ${candidate.rank}`, "eyebrow"));
    copy.append(makeText("h2", candidate.source));
    heading.append(copy, makeText("span", candidate.objective_values.construction_fit, "status-badge status-pending"));
    card.append(heading);

    const fields = document.createElement("div");
    fields.className = "run-fields";
    fields.append(
      makeField("Candidate", candidate.candidate_ref),
      makeField("Strategy", candidate.strategy_ref),
      makeField("Lineage", candidate.lineage_ref),
      makeField("Generation", String(candidate.generation_index)),
      makeField("Island", String(candidate.island_index + 1)),
      makeField("Objective", candidate.objective_values.construction_fit),
    );
    card.append(fields);

    const link = document.createElement("a");
    link.className = "button button-secondary";
    link.href = `/strategies/${encodeURIComponent(candidate.strategy_ref)}/overview`;
    link.dataset.route = link.href;
    link.textContent = "Open candidate strategy";
    card.append(link);
    grid.append(card);
  }
  container.append(grid);
}

function renderCatalogError(container, error) {
  container.replaceChildren();
  container.dataset.builderCandidateCatalogState = "error";
  const empty = document.createElement("div");
  empty.className = "empty-state";
  empty.append(makeText("div", "—", "empty-icon"));
  const copy = document.createElement("div");
  copy.append(makeText("strong", "Builder candidate catalog not loaded"));
  copy.append(makeText("p", error?.message || "Builder candidate lookup failed."));
  empty.append(copy);
  container.append(empty);
}

async function refreshCatalog(authority) {
  const container = authority.querySelector("[data-builder-candidate-catalog]");
  if (!container) return;
  const strategyRef = authority.dataset.requestedStrategyRef;
  container.dataset.builderCandidateCatalogState = "loading";
  try {
    renderCatalog(container, await fetchBuilderCandidates(strategyRef));
  } catch (error) {
    renderCatalogError(container, error);
  }
}

async function submitSearch(authority, button) {
  const mode = button.dataset.builderSearchStart;
  if (!CANDIDATE_SEARCH_MODES.includes(mode)) return;
  const strategyRef = authority.dataset.requestedStrategyRef;
  const state = authority.querySelector(`[data-builder-action-state="${mode}"]`);
  const config = mode === "bounded" ? BOUNDED_BUILD_CONFIG : {};
  button.disabled = true;
  if (state) state.textContent = mode === "bounded" ? "Running bounded Builder search…" : "Running Evolutionary Search…";
  try {
    const search = await startBuilderSearch(strategyRef, config);
    if (state) {
      state.textContent = `Search ${search.status}; generation ${search.generation}; ${search.evaluations} evaluations; ${search.candidate_count} persisted candidates.`;
    }
    await refreshCatalog(authority);
  } catch (error) {
    if (state) state.textContent = error?.message || "Builder search failed.";
  } finally {
    button.disabled = false;
  }
}

function hydrateAuthority(authority) {
  if (authority.dataset.builderHydrated === "true") return;
  authority.dataset.builderHydrated = "true";
  void refreshCatalog(authority);
}

export function bootCandidatesAuthority(root = document.querySelector("#app")) {
  if (!root || typeof MutationObserver === "undefined") return;
  const hydrate = () => root.querySelectorAll("[data-candidates-authority]").forEach(hydrateAuthority);
  const observer = new MutationObserver(hydrate);
  observer.observe(root, { childList: true, subtree: true });
  root.addEventListener("click", (event) => {
    const button = event.target.closest?.("[data-builder-search-start]");
    if (!button || button.matches(":disabled")) return;
    const authority = button.closest("[data-candidates-authority]");
    if (!authority) return;
    event.preventDefault();
    void submitSearch(authority, button);
  });
  hydrate();
}

if (typeof document !== "undefined") bootCandidatesAuthority();
