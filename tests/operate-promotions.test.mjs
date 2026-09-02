import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import { renderValidateOverview } from "../web/research-validate.mjs";
import {
  promotionCatalogFromPayload,
  renderOperatePromotions,
} from "../web/operate-promotions.mjs";

const proofEntity = "tc-research:proof:v1:aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
const promotionEntity = "tc-research:promotion:v1:bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb";
const candidateEntity = "tc-research:candidate:v1:cccccccc-cccc-4ccc-8ccc-cccccccccccc";
const historicalEntity = "tc-research:historical-result:v1:dddddddd-dddd-4ddd-8ddd-dddddddddddd";

function catalog(promotions) {
  return {
    schema: "tc.operate-promotion-catalog.v1",
    promotions,
  };
}

function promotion(overrides = {}) {
  return {
    entity_id: promotionEntity,
    revision: `tc-research-revision:promotion:sha256:${"a".repeat(64)}`,
    proof_entity_id: proofEntity,
    proof_revision: `tc-research-revision:proof:sha256:${"b".repeat(64)}`,
    candidate_entity_id: candidateEntity,
    candidate_revision: `tc-research-revision:candidate:sha256:${"c".repeat(64)}`,
    candidate_archive_name: "Survivor.sqx",
    historical_result_entity_id: historicalEntity,
    ...overrides,
  };
}

test("promotion catalog parser keeps identities without inventing live state", () => {
  const parsed = promotionCatalogFromPayload(catalog([promotion()]));
  assert.equal(parsed.promotions.length, 1);
  assert.equal(parsed.promotions[0].candidate_archive_name, "Survivor.sqx");
  assert.equal("live" in parsed.promotions[0], false);
  assert.equal("deployed" in parsed.promotions[0], false);
  assert.throws(
    () => promotionCatalogFromPayload(catalog([promotion(), promotion()])),
    /duplicate entity identity/,
  );
});

test("Operate promotion host renders current zero and loaded identities", () => {
  const empty = renderOperatePromotions({ promotions: [] });
  assert.match(empty, /No promoted Research strategies/);
  assert.doesNotMatch(empty, /Survivor\.sqx/);
  const loaded = renderOperatePromotions(promotionCatalogFromPayload(catalog([promotion()])));
  assert.match(loaded, /Survivor\.sqx/);
  assert.match(loaded, /Promoted/);
  assert.match(loaded, /Live runs, positions, and P&amp;L stay unconnected/);
});

test("Next Actions enables Promote after Proof only when a Proof exists", () => {
  const empty = renderValidateOverview({ proofs: [], results: [], jobs: [], candidates: [], phase: "loaded" });
  assert.match(empty, /Promote after Proof/);
  assert.doesNotMatch(empty, /data-promote-proof=/);
  const ready = renderValidateOverview({
    proofs: [{ entity_id: proofEntity, revision: `tc-research-revision:proof:sha256:${"b".repeat(64)}` }],
    results: [],
    jobs: [],
    candidates: [],
    phase: "loaded",
  });
  assert.match(ready, new RegExp(`data-promote-proof="${proofEntity}"`));
  assert.match(ready, /Promote to Live/);
});

test("desktop loads the Operate promotions binder", async () => {
  const source = await readFile(new URL("../web/index.html", import.meta.url), "utf8");
  assert.match(source, /src="\/operate-promotions\.mjs"/);
});
