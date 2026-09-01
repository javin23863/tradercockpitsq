import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import {
  carryExactResearchSelections,
  readExactRouteSelection,
  resolveExactRouteSelection,
  routeWithExactSelection,
} from "../web/research-backtest-route-selection.mjs";

const first = { entity_id: "tc-research:candidate:v1:first", revision: "tc-revision:sha256:first" };
const second = { entity_id: "tc-research:candidate:v1:second", revision: "tc-revision:sha256:second" };
const result = {
  entity_id: "tc-research:historical-result:v1:result-two",
  revision: "tc-revision:sha256:result-two",
  candidate_entity_id: second.entity_id,
  candidate_revision: second.revision,
};

function location(search) {
  return {
    origin: "http://127.0.0.1:4173",
    pathname: "/research",
    search,
  };
}

test("exact Candidate bookmark resolves the selected later catalog record instead of index zero", () => {
  const selected = readExactRouteSelection(
    location("?stage=backtest&tab=overview&candidateEntityId=tc-research%3Acandidate%3Av1%3Asecond&candidateRevision=tc-revision%3Asha256%3Asecond"),
    "candidate",
  );
  assert.deepEqual(selected, {
    state: "exact",
    entityId: second.entity_id,
    revision: second.revision,
  });
  assert.deepEqual(resolveExactRouteSelection([first, second], selected), { state: "exact", index: 1 });
});

test("partial or stale Backtest bookmarks fail closed", () => {
  assert.equal(
    readExactRouteSelection(location("?stage=backtest&tab=overview&candidateEntityId=only-entity"), "candidate").state,
    "invalid",
  );
  assert.deepEqual(
    resolveExactRouteSelection([first, second], { state: "exact", entityId: "missing", revision: "missing" }),
    { state: "stale", index: -1 },
  );
});

test("a first-time absent Candidate may default once but is immediately encoded as exact identity", () => {
  const absent = readExactRouteSelection(location("?stage=backtest&tab=overview"), "candidate");
  assert.deepEqual(resolveExactRouteSelection([first, second], absent), { state: "default", index: 0 });
  const href = routeWithExactSelection(location("?stage=backtest&tab=overview"), "candidate", first);
  const url = new URL(href, "http://127.0.0.1:4173");
  assert.equal(url.searchParams.get("candidateEntityId"), first.entity_id);
  assert.equal(url.searchParams.get("candidateRevision"), first.revision);
});

test("changing Candidate clears any downstream Historical Result bookmark", () => {
  const current = location(
    "?stage=backtest&tab=overview"
      + "&candidateEntityId=tc-research%3Acandidate%3Av1%3Afirst"
      + "&candidateRevision=tc-revision%3Asha256%3Afirst"
      + "&historicalResultEntityId=tc-research%3Ahistorical-result%3Av1%3Aold"
      + "&historicalResultRevision=tc-revision%3Asha256%3Aold",
  );
  const next = new URL(routeWithExactSelection(current, "candidate", second), current.origin);
  assert.equal(next.searchParams.get("candidateEntityId"), second.entity_id);
  assert.equal(next.searchParams.get("candidateRevision"), second.revision);
  assert.equal(next.searchParams.has("historicalResultEntityId"), false);
  assert.equal(next.searchParams.has("historicalResultRevision"), false);
});

test("selecting a Historical Result binds its exact parent Candidate into the route", () => {
  const next = new URL(
    routeWithExactSelection(location("?stage=backtest&tab=trades"), "historicalResult", result),
    "http://127.0.0.1:4173",
  );
  assert.equal(next.searchParams.get("historicalResultEntityId"), result.entity_id);
  assert.equal(next.searchParams.get("historicalResultRevision"), result.revision);
  assert.equal(next.searchParams.get("candidateEntityId"), second.entity_id);
  assert.equal(next.searchParams.get("candidateRevision"), second.revision);

  assert.throws(
    () => routeWithExactSelection(location("?stage=backtest&tab=trades"), "historicalResult", {
      entity_id: result.entity_id,
      revision: result.revision,
    }),
    /exact parent Candidate identity/,
  );
});

test("Research navigation carries coherent Candidate and Historical Result identity across Backtest tabs", () => {
  const current = location(
    "?stage=backtest&tab=overview"
      + "&candidateEntityId=tc-research%3Acandidate%3Av1%3Asecond"
      + "&candidateRevision=tc-revision%3Asha256%3Asecond"
      + "&historicalResultEntityId=tc-research%3Ahistorical-result%3Av1%3Aresult-two"
      + "&historicalResultRevision=tc-revision%3Asha256%3Aresult-two",
  );
  const carried = carryExactResearchSelections(current, "/research?stage=backtest&tab=trades");
  const url = new URL(carried, current.origin);
  assert.equal(url.searchParams.get("candidateEntityId"), "tc-research:candidate:v1:second");
  assert.equal(url.searchParams.get("candidateRevision"), "tc-revision:sha256:second");
  assert.equal(url.searchParams.get("historicalResultEntityId"), "tc-research:historical-result:v1:result-two");
  assert.equal(url.searchParams.get("historicalResultRevision"), "tc-revision:sha256:result-two");
  assert.equal(url.searchParams.get("tab"), "trades");
});

test("selection carrying never rewrites non-Research destinations", () => {
  const current = location(
    "?stage=backtest&tab=overview&candidateEntityId=entity&candidateRevision=revision",
  );
  assert.equal(carryExactResearchSelections(current, "/home"), "/home");
  assert.equal(carryExactResearchSelections(current, "https://example.invalid/research"), "https://example.invalid/research");
});

test("canonical desktop loads exact Backtest route reconciliation exactly once", async () => {
  const html = await readFile(new URL("../web/index.html", import.meta.url), "utf8");
  assert.equal((html.match(/research-backtest-route-selection\.mjs/g) || []).length, 1);

  const source = await readFile(new URL("../web/research-backtest-route-selection.mjs", import.meta.url), "utf8");
  assert.match(source, /data-route-selection-state/);
  assert.match(source, /data-retester-action/);
  assert.match(source, /stopImmediatePropagation/);
});
