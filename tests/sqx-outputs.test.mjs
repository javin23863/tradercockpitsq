import test from "node:test";
import assert from "node:assert/strict";

import {
  fetchSqxOutputs,
  importSqxOutput,
  normalizeSqxOutputs,
  sqxOutputImportPath,
  startSqxNativeRun,
} from "../web/sqx-outputs.mjs";

const catalogPayload = {
  schema: "tc.sqx-builder-output-list.v1",
  sqx_build: "144.2953",
  project: "Builder",
  databank: "Results",
  runtime: { ready: true, status: "verified", detail: "verified" },
  outputs: [
    {
      archive: "Generated 1.sqx",
      relative_path: "user/projects/Builder/databanks/Results/Generated 1.sqx",
      bytes: 12345,
      archive_sha256: "a".repeat(64),
      native_version: "144.2953",
      strategy_entry_sha256: "b".repeat(64),
      settings_entry_sha256: "c".repeat(64),
      archive_entries: ["settings.xml", "strategy_Portfolio.xml", "version.txt"],
      importable: true,
    },
  ],
};

test("SQX output catalog accepts source-owned archive identities", () => {
  const catalog = normalizeSqxOutputs(catalogPayload);
  assert.equal(catalog.project, "Builder");
  assert.equal(catalog.databank, "Results");
  assert.equal(catalog.outputs[0].archive, "Generated 1.sqx");
  assert.equal(catalog.outputs[0].archive_sha256, "a".repeat(64));
});

test("SQX output catalog rejects duplicate archives", () => {
  assert.throws(
    () => normalizeSqxOutputs({ ...catalogPayload, outputs: [catalogPayload.outputs[0], catalogPayload.outputs[0]] }),
    /Duplicate SQX output archive/,
  );
});

test("SQX output import path encodes exact archive name and rejects traversal", () => {
  const path = sqxOutputImportPath("Generated 1.sqx");
  const url = new URL(path, "http://localhost");
  assert.equal(url.pathname, "/api/sqx-outputs/import");
  assert.equal(url.searchParams.get("archive"), "Generated 1.sqx");
  assert.throws(() => sqxOutputImportPath("../escape.sqx"), /Invalid SQX output archive/);
  assert.throws(() => sqxOutputImportPath("folder/escape.sqx"), /Invalid SQX output archive/);
});

test("SQX output lookup uses the product API", async () => {
  let requested = "";
  const catalog = await fetchSqxOutputs(async (path) => {
    requested = path;
    return {
      ok: true,
      status: 200,
      async json() {
        return catalogPayload;
      },
    };
  });
  assert.equal(requested, "/api/sqx-outputs");
  assert.equal(catalog.outputs.length, 1);
});

test("SQX output import advertises the real native Retester binding", async () => {
  let requested = "";
  let method = "";
  const candidateRef = `tc:candidate:v1:sha256:${"2".repeat(64)}`;
  const receipt = await importSqxOutput("Generated 1.sqx", async (path, options) => {
    requested = path;
    method = options.method;
    return {
      ok: true,
      status: 201,
      async json() {
        return {
          schema: "tc.sqx-builder-output-import.v1",
          archive: catalogPayload.outputs[0],
          strategy_ref: `tc:strategy:v1:sha256:${"1".repeat(64)}`,
          candidate_ref: candidateRef,
          semantic_schema: "sqx.native-archive.v1",
          candidate_origin: "sqx-builder",
          custody: "persisted",
          run_binding: {
            available: true,
            mode: "sqx-native-retester",
            request: { candidate_ref: candidateRef },
            detail: "eligible",
          },
        };
      },
    };
  });
  assert.equal(method, "POST");
  assert.equal(new URL(requested, "http://localhost").searchParams.get("archive"), "Generated 1.sqx");
  assert.equal(receipt.custody, "persisted");
  assert.equal(receipt.run_binding.available, true);
  assert.deepEqual(receipt.run_binding.request, { candidate_ref: candidateRef });
});

test("native run start sends only the exact candidate identity", async () => {
  const candidateRef = `tc:candidate:v1:sha256:${"3".repeat(64)}`;
  let requested = null;
  const receipt = await startSqxNativeRun(candidateRef, async (path, options) => {
    requested = { path, options };
    return {
      ok: true,
      status: 201,
      async json() {
        return {
          schema: "tc.sqx-native-run-start.v1",
          status: "completed",
          run_ref: `tc:backtest-run:v1:sha256:${"4".repeat(64)}`,
          invocation_id: "sqx-001",
          result_ref: `tc:result:v1:sha256:${"5".repeat(64)}`,
        };
      },
    };
  });
  assert.equal(requested.path, "/api/sqx-runs/start");
  assert.equal(requested.options.method, "POST");
  assert.deepEqual(JSON.parse(requested.options.body), { candidate_ref: candidateRef });
  assert.equal(receipt.status, "completed");
});
