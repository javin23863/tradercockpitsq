import test from "node:test";
import assert from "node:assert/strict";

import {
  fetchPresetCatalog,
  presetCatalogFromPayload,
  renderPresetCatalog,
} from "../web/research-presets.mjs";

function catalog() {
  return {
    schema: "tc.sqx-preset-catalog.v1",
    source_build: "144.2953",
    execution_available: false,
    execution_reason: "trusted_native_gateway_not_implemented",
    presets: [
      {
        preset_id: "sqx-default-forex",
        label: "Forex",
        market: "forex",
        source_build: "144.2953",
        source_relative_path: "internal/web/BUILDER/simpleTemplates/DefaultForex.xml",
        source_sha256: "a".repeat(64),
        runtime: { available: true, status: "verified", verified_sha256: "a".repeat(64), observed_build: "144.2953" },
      },
      {
        preset_id: "sqx-default-futures",
        label: "Futures",
        market: "futures",
        source_build: "144.2953",
        source_relative_path: "internal/web/BUILDER/simpleTemplates/DefaultFutures.xml",
        source_sha256: "b".repeat(64),
        runtime: { available: false, status: "preset_missing", verified_sha256: null, observed_build: "144.2953" },
      },
    ],
  };
}

test("Preset catalog parser keeps source identity separate from Builder validity", () => {
  const parsed = presetCatalogFromPayload(catalog());
  assert.equal(parsed.presets.length, 2);
  assert.equal(parsed.presets[0].runtime.status, "verified");
  assert.equal(parsed.presets[1].runtime.status, "preset_missing");
});

test("Preset catalog parser rejects contradictory verified runtime state", () => {
  const wrongHash = catalog();
  wrongHash.presets[0].runtime.verified_sha256 = "c".repeat(64);
  assert.throws(() => presetCatalogFromPayload(wrongHash), /verified state is inconsistent/);

  const duplicate = catalog();
  duplicate.presets[1].preset_id = duplicate.presets[0].preset_id;
  assert.throws(() => presetCatalogFromPayload(duplicate), /entry is invalid/);
});

test("Preset catalog fetch uses only the canonical read-only endpoint", async () => {
  let requested = "";
  const result = await fetchPresetCatalog(async (path, options) => {
    requested = path;
    assert.deepEqual(options, { headers: { accept: "application/json" } });
    return { ok: true, status: 200, json: async () => catalog() };
  });
  assert.equal(requested, "/api/sqx-presets");
  assert.equal(result.schema, "tc.sqx-preset-catalog.v1");
});

test("Preset renderer does not claim preset binding or execution", () => {
  const html = renderPresetCatalog(catalog());
  assert.match(html, /Native preset catalog/);
  assert.match(html, /Read-only source inspection/);
  assert.match(html, /Forex/);
  assert.match(html, /Preset Missing/);
  assert.match(html, /does not infer that this preset is bound to the current Builder project/);
  assert.doesNotMatch(html, /Use preset|Apply preset|Launch|Execute/);
});
