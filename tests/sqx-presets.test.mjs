import test from "node:test";
import assert from "node:assert/strict";

import {
  fetchSqxPresetCatalog,
  launchSqxPreset,
  normalizePresetCatalog,
  presetSelectionPath,
  selectedPresetId,
  sqxPresetLaunchPath,
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
      runtime: {
        available: false,
        status: "runtime_not_configured",
        verified_sha256: null,
        launch_available: false,
        launch_status: "runtime_not_configured",
        launch_detail: "SQX_HOME is not configured",
        observed_build: null,
        launcher_sha256: null,
        launcher_identity_source: null,
      },
    },
  ],
};

test("SQX preset catalog accepts source-bound records", () => {
  const catalog = normalizePresetCatalog(payload);
  assert.equal(catalog.source_build, "144.2953");
  assert.equal(catalog.presets[0].preset_id, "sqx-default-futures");
  assert.equal(catalog.presets[0].runtime.status, "runtime_not_configured");
  assert.equal(catalog.presets[0].runtime.launch_available, false);
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

test("SQX preset launch path is bound to exact source preset identity", () => {
  assert.equal(
    sqxPresetLaunchPath("sqx-default-futures"),
    "/api/sqx-presets/sqx-default-futures/launch",
  );
  assert.throws(() => sqxPresetLaunchPath("../futures"), /Invalid SQX preset ID/);
});

test("SQX preset launch uses protected JSON control request and consumes receipt", async () => {
  let requested = "";
  let options = null;
  const receipt = await launchSqxPreset("sqx-default-futures", async (path, requestOptions) => {
    requested = path;
    options = requestOptions;
    return {
      ok: true,
      status: 202,
      async json() {
        return {
          schema: "tc.sqx-preset-launch.v1",
          preset_id: "sqx-default-futures",
          market: "futures",
          sqx_build: "144.2953",
          source_sha256: payload.presets[0].source_sha256,
          launcher_sha256: "b".repeat(64),
          project: "Builder",
          state: "submitted",
          control_requests_submitted: 2,
          receipts: [
            { sequence: 1, action: "loadconfig", state: "completed", exit_code: 0 },
            { sequence: 2, action: "start", state: "completed", exit_code: 0 },
          ],
        };
      },
    };
  });

  assert.equal(requested, "/api/sqx-presets/sqx-default-futures/launch");
  assert.equal(options.method, "POST");
  assert.equal(options.headers["content-type"], "application/json");
  assert.equal(options.body, "{}");
  assert.equal(receipt.state, "submitted");
  assert.equal(receipt.control_requests_submitted, 2);
});

test("SQX preset launch preserves structured partial-side-effect refusal", async () => {
  const refusal = {
    error: "producer_error",
    reason_code: "sqx_command_rejected",
    detail: "SQX start command exited with code 7",
    control_requests_completed: 1,
    partial_side_effect: true,
    receipts: [
      { sequence: 1, action: "loadconfig", state: "completed", exit_code: 0 },
      { sequence: 2, action: "start", state: "rejected", exit_code: 7 },
    ],
  };

  await assert.rejects(
    async () => {
      try {
        await launchSqxPreset("sqx-default-futures", async () => ({
          ok: false,
          status: 502,
          async json() {
            return refusal;
          },
        }));
      } catch (error) {
        assert.deepEqual(error.payload, refusal);
        throw error;
      }
    },
    /SQX start command exited with code 7/,
  );
});

test("SQX preset launch surfaces ordinary backend refusal", async () => {
  await assert.rejects(
    () => launchSqxPreset("sqx-default-futures", async () => ({
      ok: false,
      status: 503,
      async json() {
        return { detail: "SQX launcher identity is not configured" };
      },
    })),
    /SQX launcher identity is not configured/,
  );
});
