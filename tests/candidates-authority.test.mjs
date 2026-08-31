import assert from "node:assert/strict";
import test from "node:test";

import { renderApp } from "../web/app.mjs";
import {
  CANDIDATE_AUTHORITY_ZONES,
  CANDIDATE_SEARCH_MODES,
  builderCandidatesPath,
  createCatalogRefreshGuard,
  fetchBuilderCandidates,
  normalizeBuilderCandidates,
  normalizeBuilderSearch,
  startBuilderSearch,
} from "../web/candidates-authority.mjs";
import { pathForState } from "../web/model.mjs";

const strategyRef = "signed/spec-v2:opaque+42";
const ref = (kind, char) => `tc:${kind}:v1:sha256:${char.repeat(64)}`;
const searchRef = ref("builder-search", "a");
const configRef = ref("builder-config", "b");
const candidateRef = ref("candidate", "c");
const candidateStrategyRef = ref("strategy", "d");
const lineageRef = ref("builder-lineage", "e");

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function candidateRecord(overrides = {}) {
  return {
    candidate_ref: candidateRef,
    strategy_ref: candidateStrategyRef,
    lineage_ref: lineageRef,
    objective_values: { construction_fit: "9876" },
    rank: 1,
    island_index: 0,
    generation_index: 1,
    node_index: 0,
    source: "builder-crossover",
    parent_candidate_refs: [],
    parent_strategy_ref: null,
    ...overrides,
  };
}

function searchPayload(overrides = {}) {
  return {
    schema: "tc.builder-search.v1",
    implementation: "tradercockpit.builder-search.v2",
    search_ref: searchRef,
    requested_strategy_ref: strategyRef,
    config_ref: configRef,
    config: {},
    status: "complete",
    stage: "complete",
    generation: 1,
    restart_count: 0,
    evaluations: 8,
    objective: {
      name: "construction_fit",
      direction: "maximize",
      evidence_role: "discovery",
    },
    candidate_count: 1,
    population_count: 4,
    candidates: [candidateRecord()],
    ...overrides,
  };
}

function catalogPayload(overrides = {}) {
  return {
    schema: "tc.builder-candidates.v1",
    requested_strategy_ref: strategyRef,
    searches: [searchPayload()],
    candidates: [candidateRecord({
      search_rank: 1,
      search_ref: searchRef,
      search_status: "complete",
      config_ref: configRef,
    })],
    ...overrides,
  };
}

test("Candidates exposes four authority zones with real Builder actions", () => {
  const path = pathForState("strategies", "candidates", strategyRef);
  const html = renderApp({ pathname: path, search: "" });

  assert.deepEqual(CANDIDATE_AUTHORITY_ZONES, ["build", "evolution", "models", "custody"]);
  assert.deepEqual(CANDIDATE_SEARCH_MODES, ["bounded", "evolution"]);
  for (const zone of CANDIDATE_AUTHORITY_ZONES) {
    const marker = `data-candidates-zone="${zone}"`;
    assert.equal((html.match(new RegExp(marker, "g")) || []).length, 1, zone);
  }

  for (const label of [
    "Manual / bounded build",
    "Evolutionary Search",
    "Machine Learning",
    "Candidate custody",
  ]) {
    assert.match(html, new RegExp(escapeRegExp(label), "i"), label);
  }

  assert.match(html, new RegExp(escapeRegExp(strategyRef)));
  assert.match(html, /data-builder-search-start="bounded"/);
  assert.match(html, /data-builder-search-start="evolution"/);
  assert.doesNotMatch(html, /data-builder-search-start="(?:bounded|evolution)"[^>]*disabled/i);
  assert.match(html, /Reading persisted candidate records/i);
  assert.match(html, /does not claim native SQX Builder equivalence/i);
  assert.doesNotMatch(
    html,
    /data-run-surface-id="shared-run-surface"/i,
    "Builder Candidates must remain separate from the shared RunSurface used by native Retester custody/actions",
  );
});

test("Candidates does not fabricate active results before backend custody arrives", () => {
  const path = pathForState("strategies", "candidates", strategyRef);
  const html = renderApp({ pathname: path, search: "" });

  for (const fabricated of [
    "Population 512",
    "Generations 100",
    "Tournament 4",
    "Crossover 0.85",
    "Mutation 0.08",
    "Pareto Rank 1",
    "Sharpe 2.1",
    "Win rate 68%",
    "A+ Champion",
  ]) {
    assert.doesNotMatch(html, new RegExp(escapeRegExp(fabricated), "i"), fabricated);
  }

  assert.doesNotMatch(html, /data-builder-candidate-record=/i);
  assert.match(html, /data-builder-candidate-catalog-state="loading"/i);
  assert.match(html, /status-badge status-pending/);
});

test("Builder candidate path preserves the exact opaque requested reference", () => {
  const opaque = "  opaque/percent%+query?#&= Khmer ខ្មែរ  ";
  const path = builderCandidatesPath(opaque);
  const parsed = new URL(path, "http://localhost");
  assert.equal(parsed.pathname, "/api/builder-candidates");
  assert.equal(parsed.searchParams.get("strategyRef"), opaque);
});

test("Builder catalog refresh guard rejects superseded response tokens", () => {
  const guard = createCatalogRefreshGuard();
  const hydration = guard.begin();
  assert.equal(guard.isCurrent(hydration), true);
  const postSearch = guard.begin();
  assert.equal(guard.isCurrent(hydration), false);
  assert.equal(guard.isCurrent(postSearch), true);
});

test("Builder search POST sends exact reference and explicit config", async () => {
  const calls = [];
  const fetchImpl = async (url, options) => {
    calls.push({ url, options });
    return { ok: true, status: 201, json: async () => searchPayload() };
  };

  const config = { maximum_generations: 1, random_seed: 73 };
  const result = await startBuilderSearch(strategyRef, config, fetchImpl);
  assert.equal(result.search_ref, searchRef);
  assert.equal(calls.length, 1);
  assert.equal(calls[0].url, "/api/builder-searches");
  assert.equal(calls[0].options.method, "POST");
  assert.deepEqual(JSON.parse(calls[0].options.body), { strategyRef, config });
});

test("Builder candidate GET accepts only canonical catalog records", async () => {
  const catalog = catalogPayload();
  const calls = [];
  const fetchImpl = async (url, options) => {
    calls.push({ url, options });
    return { ok: true, status: 200, json: async () => catalog };
  };

  const normalized = await fetchBuilderCandidates(strategyRef, fetchImpl);
  assert.equal(normalized.candidates[0].candidate_ref, candidateRef);
  assert.equal(normalized.candidates[0].objective_values.construction_fit, "9876");
  assert.equal(normalized.candidates[0].search_rank, 1);
  assert.equal(calls.length, 1);
  assert.equal(new URL(calls[0].url, "http://localhost").searchParams.get("strategyRef"), strategyRef);
  assert.equal(calls[0].options.headers.accept, "application/json");
});

test("Builder frontend rejects malformed search and candidate custody", () => {
  assert.throws(
    () => normalizeBuilderSearch(searchPayload({ candidate_count: 2 })),
    /candidate count/i,
  );
  assert.throws(
    () => normalizeBuilderCandidates({
      schema: "tc.builder-candidates.v1",
      requested_strategy_ref: strategyRef,
      searches: [searchPayload()],
      candidates: [candidateRecord({ lineage_ref: "not-lineage", search_rank: 1 })],
    }),
    /builder-lineage/i,
  );
  assert.throws(
    () => normalizeBuilderSearch(searchPayload({
      search_ref: `tc:builder-search:v1:sha256:${"g".repeat(64)}`,
    })),
    /builder-search v1 content address/i,
  );
  assert.throws(
    () => normalizeBuilderSearch(searchPayload({
      candidates: [candidateRecord({ rank: 2 })],
    })),
    /ranks are not contiguous/i,
  );
  assert.throws(
    () => normalizeBuilderSearch(searchPayload({
      implementation: "tradercockpit.builder-search.v1",
    })),
    /implementation revision/i,
  );
  assert.throws(
    () => normalizeBuilderCandidates(catalogPayload({
      candidates: [candidateRecord({
        rank: 2,
        search_rank: 1,
        search_ref: searchRef,
        search_status: "complete",
        config_ref: configRef,
      })],
    })),
    /catalog ranks are not contiguous/i,
  );
});

test("Builder catalog rejects cross-strategy and cross-search custody", () => {
  assert.throws(
    () => normalizeBuilderCandidates(catalogPayload({
      searches: [searchPayload({ requested_strategy_ref: "another-strategy" })],
    })),
    /another requested strategy reference/i,
  );
  assert.throws(
    () => normalizeBuilderCandidates(catalogPayload({
      candidates: [candidateRecord({
        search_rank: 1,
        search_ref: searchRef,
        search_status: "complete",
        config_ref: ref("builder-config", "f"),
      })],
    })),
    /config_ref disagrees/i,
  );
  assert.throws(
    () => normalizeBuilderCandidates(catalogPayload({
      candidates: [candidateRecord({
        candidate_ref: ref("candidate", "f"),
        search_rank: 1,
        search_ref: searchRef,
        search_status: "complete",
        config_ref: configRef,
      })],
    })),
    /not present in its referenced search/i,
  );
  assert.throws(
    () => normalizeBuilderCandidates(catalogPayload({
      candidates: [candidateRecord({
        search_rank: 2,
        search_ref: searchRef,
        search_status: "complete",
        config_ref: configRef,
      })],
    })),
    /search_rank disagrees/i,
  );
});

test("Builder fetches reject responses for another requested strategy reference", async () => {
  const other = "another-strategy";
  const searchFetch = async () => ({
    ok: true,
    status: 201,
    json: async () => searchPayload({ requested_strategy_ref: other }),
  });
  await assert.rejects(
    startBuilderSearch(strategyRef, {}, searchFetch),
    /another requested strategy reference/i,
  );

  const catalogFetch = async () => ({
    ok: true,
    status: 200,
    json: async () => catalogPayload({
      requested_strategy_ref: other,
      searches: [searchPayload({ requested_strategy_ref: other })],
    }),
  });
  await assert.rejects(
    fetchBuilderCandidates(strategyRef, catalogFetch),
    /another requested strategy reference/i,
  );
});

test("model assistance routes to Signals & Models without creating a second model workspace", () => {
  const path = pathForState("strategies", "candidates", strategyRef);
  const html = renderApp({ pathname: path, search: "" });
  const expected = `/strategies/${encodeURIComponent(strategyRef)}/signals`;

  assert.match(html, new RegExp(`data-route="${escapeRegExp(expected)}"`));
  assert.equal((html.match(/data-primary-navigation(?:\s|=)/g) || []).length, 1);
  assert.equal((html.match(/data-apollo-surface(?:\s|=)/g) || []).length, 1);
});
