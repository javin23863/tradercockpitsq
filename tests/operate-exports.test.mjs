import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import {
  exportCatalogFromPayload,
  renderOperateExports,
} from "../web/operate-exports.mjs";

const promotionEntity = "tc-research:promotion:v1:bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb";
const proofEntity = "tc-research:proof:v1:aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
const candidateEntity = "tc-research:candidate:v1:cccccccc-cccc-4ccc-8ccc-cccccccccccc";
const exportEntity = "tc-research:export:v1:dddddddd-dddd-4ddd-8ddd-dddddddddddd";

function catalog(exports) {
  return {
    schema: "tc.operate-export-catalog.v1",
    exports,
  };
}

function exportRecord(overrides = {}) {
  return {
    entity_id: exportEntity,
    revision: `tc-research-revision:export:sha256:${"a".repeat(64)}`,
    promotion_entity_id: promotionEntity,
    proof_entity_id: proofEntity,
    candidate_entity_id: candidateEntity,
    candidate_archive_name: "Survivor.sqx",
    ...overrides,
  };
}

test("export catalog parser keeps identities without inventing broker or live state", () => {
  const parsed = exportCatalogFromPayload(catalog([exportRecord()]));
  assert.equal(parsed.exports.length, 1);
  assert.equal(parsed.exports[0].candidate_archive_name, "Survivor.sqx");
  assert.equal("live" in parsed.exports[0], false);
  assert.equal("deployed" in parsed.exports[0], false);
  assert.equal("broker" in parsed.exports[0], false);
  assert.throws(
    () => exportCatalogFromPayload(catalog([exportRecord(), exportRecord()])),
    /duplicate entity identity/,
  );
});

test("Operate export host renders current zero and loaded identities", () => {
  const empty = renderOperateExports({ exports: [] });
  assert.match(empty, /No exported Delivery custody yet/);
  assert.match(empty, /not broker\/MT4 export/);
  assert.doesNotMatch(empty, /Survivor\.sqx/);
  const loaded = renderOperateExports(exportCatalogFromPayload(catalog([exportRecord()])));
  assert.match(loaded, /Survivor\.sqx/);
  assert.match(loaded, /Exported/);
  assert.match(loaded, /No strategy bytes are written outside custody/);
});

test("desktop loads the Operate exports binder", async () => {
  const source = await readFile(new URL("../web/index.html", import.meta.url), "utf8");
  assert.match(source, /src="\/operate-exports\.mjs"/);
});
