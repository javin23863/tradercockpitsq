import test from "node:test";
import assert from "node:assert/strict";

import {
  fetchSqxPresetCatalog,
  normalizePresetCatalog,
  presetSelectionPath,
  selectedPresetId,
} from "../web/sqx-presets.mjs";

const payload = {
  schema: "tc.sqx-preset-catalog.v1",
  source_build: "144.2953",
  reference_commit: "958e2fe2910cbf71d51ae29e4951484a86fc4ab6",
  presets: [
    {
      preset_id: "sqx-default-futures",
      label: "Futures",
      market: "futures",
      source_build: "144.2953",
      source_relative_path: "internal/web/BUILDER/simpleTemplates/DefaultFutures.xml",
      source_sha256: "a792e499205470c832e079647f33e52ce11e3a119a28889819b35e84b93b813b",
      reference_commit: "958e2fe2910cbf71d51ae29e4951484a86fc4ab6",
      runtime: { available: false, status: "runtime_not_configured", verified_sha256: null },
    },
  ],
};

test("SQX preset catalog accepts source-bound records", () => {
  const catalog = normalizePresetCatalog(payload);
  assert.equal(catalog.source_build, "144.2953");
  assert.equal(catalog.presets[0].preset_id, "sqx-default-futures");
  assert.equal(catalog.presets[0].runtime.status, "runtime_not_configured");
});

test("SQX preset catalog rejects duplicate identities", () => {
  assert.throws(
    () => normalizePresetCatalog({ ...payload, presets: [payload.presets[0], payload.presets[0]] }),
    /Duplicate SQX preset id/,
  );
});

test("preset selection preserves existing strategy context", () => {
  const path = presetSelectionPath(
    "/validate/run",
    "?strategyRef=opaque%2Fref&other=value",
    "sqx-default-forex",
  );
  const url = new URL(path, "http://localhost");
  assert.equal(url.pathname, "/validate/run");
  assert.equal(url.searchParams.get("strategyRef"), "opaque/ref");
  assert.equal(url.searchParams.get("other"), "value");
  assert.equal(url.searchParams.get("presetId"), "sqx-default-forex");
  assert.equal(selectedPresetId(url.search), "sqx-default-forex");
});

test("preset catalog fetch uses the product API", async () => {
  let requested = "";
  const catalog = await fetchSqxPresetCatalog(async (path) => {
    requested = path;
    return {
      ok: true,
      status: 200,
      async json() {
        return payload;
      },
    };
  });
  assert.equal(requested, "/api/sqx-presets");
  assert.equal(catalog.presets.length, 1);
});
