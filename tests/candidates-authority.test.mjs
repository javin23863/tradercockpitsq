import assert from "node:assert/strict";
import test from "node:test";

import { renderApp } from "../web/app.mjs";
import { CANDIDATE_AUTHORITY_ZONES } from "../web/candidates-authority.mjs";
import { pathForState } from "../web/model.mjs";

const strategyRef = "signed/spec-v2:opaque+42";

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

test("Candidates freezes four producer-bound authority zones", () => {
  const path = pathForState("strategies", "candidates", strategyRef);
  const html = renderApp({ pathname: path, search: "" });

  assert.deepEqual(CANDIDATE_AUTHORITY_ZONES, ["build", "evolution", "models", "custody"]);
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
  assert.match(html, /Builder configuration integration pending/i);
  assert.match(html, /Evolutionary Search integration pending/i);
  assert.match(html, /Model eligibility pending/i);
  assert.match(html, /Candidate records not available to this frontend/i);
});

test("Candidates does not reproduce or fabricate active GA and result semantics", () => {
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

  assert.doesNotMatch(html, /<table\b/i);
  assert.doesNotMatch(html, /data-(?:candidate|fitness|rank|generation|population)=/i);
  assert.match(html, /status-badge status-pending/);
});

test("model assistance routes to Signals & Models without creating a second model workspace", () => {
  const path = pathForState("strategies", "candidates", strategyRef);
  const html = renderApp({ pathname: path, search: "" });
  const expected = `/strategies/${encodeURIComponent(strategyRef)}/signals`;

  assert.match(html, new RegExp(`data-route="${escapeRegExp(expected)}"`));
  assert.equal((html.match(/data-primary-navigation(?:\s|=)/g) || []).length, 1);
  assert.equal((html.match(/data-apollo-surface(?:\s|=)/g) || []).length, 1);
});
