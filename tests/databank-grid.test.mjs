import test from "node:test";
import assert from "node:assert/strict";

import { DATABANK_GRID_COLUMNS, databankCellValue, fetchDatabankColumns, formatDatabankCell, renderResultsPanel } from "../web/automation-results.mjs";
import { customProjectResultsFromPayload } from "../web/custom-project-results.mjs";

function topology() {
  return { schema: "tc.sqx-custom-project-topology.v1", project: "Example Workflow", tasks: [{ number: 1, native_task_index: 1, name: "Build strategies", kind: "Build", settings: [] }], databanks: ["Results"] };
}

function results() {
  return {
    schema: "tc.sqx-custom-project-results.v1",
    source_build: "144.2953",
    status: "ready",
    reason_code: null,
    detail: "Native Custom Project databanks",
    project: "Example Workflow",
    databank_count: 1,
    strategy_count: 1,
    projects: [{
      name: "Example Workflow",
      source_relative_path: "user/projects/Example Workflow/project.cfx",
      databank_count: 1,
      strategy_count: 1,
      databanks: [{ name: "Results", strategy_count: 1, strategies: [{ archive: "Alpha.sqx", relative_path: "user/projects/Example Workflow/databanks/Results/Alpha.sqx", inspectable: true, native_version: "144.2953", archive_sha256: "a".repeat(64) }] }],
    }],
  };
}

test("Results panel marks the databank grid with its project so columns can be fetched", () => {
  const parsed = customProjectResultsFromPayload(results());
  const html = renderResultsPanel(topology(), parsed, { task: 1 });
  assert.match(html, /data-databank-grid data-databank-grid-project="Example Workflow"/);
  assert.match(html, /data-databank-grid-status>Loading databank columns…/);
  assert.match(html, /data-automation-archive="Alpha.sqx"/);
});

test("Databank columns fetch validates schema and basis", async () => {
  let requested = "";
  const payload = await fetchDatabankColumns("Example Workflow", "Results", async (path) => {
    requested = path;
    return { ok: true, status: 200, json: async () => ({ schema: "tc.sqx-databank-columns.v1", basis: "sqx_column_formulas_over_orders.bin", rows: [], archive_count: 0, computed_count: 0, truncated: false }) };
  });
  assert.equal(requested, "/api/sqx-databank-columns?project=Example+Workflow&databank=Results");
  assert.deepEqual(payload.rows, []);
  await assert.rejects(
    () => fetchDatabankColumns("P", "D", async () => ({ ok: true, status: 200, json: async () => ({ schema: "tc.sqx-databank-columns.v1", basis: "invented", rows: [] }) })),
    /invalid/,
  );
});

test("Databank cell helpers read SQX column names and never invent values", () => {
  const row = { archive: "Alpha.sqx", state: "ready", columns: { NetProfit: 1234.5, NumberOfTrades: 42, ProfitFactor: 1.37, WinningPct: 55.5, Drawdown: 210, DrawdownPct: 3.4, ReturnDDRatio: 5.9 }, fitness_is: 0.6123 };
  assert.equal(databankCellValue(row, "NetProfit"), 1234.5);
  assert.equal(databankCellValue(row, "NumberOfTrades"), 42);
  assert.equal(databankCellValue(row, "fitness_is"), 0.6123);
  assert.equal(databankCellValue({ state: "unavailable", columns: null }, "NetProfit"), null);
  assert.equal(formatDatabankCell(null), "—");
  assert.equal(formatDatabankCell(42, {}), "42");
  assert.equal(formatDatabankCell(55.5, { pct: true }), "55.5%");
  assert.equal(formatDatabankCell(0.6123, { fitness: true }), "0.612");
  assert.deepEqual(DATABANK_GRID_COLUMNS.map(([key]) => key), ["NetProfit", "NumberOfTrades", "ProfitFactor", "WinningPct", "Drawdown", "DrawdownPct", "ReturnDDRatio", "fitness_is"]);
});
