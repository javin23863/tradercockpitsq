import test from "node:test";
import assert from "node:assert/strict";

import { dataManagerViewModel, loadDataManager, renderDataManager } from "../web/data-manager.mjs";
import { renderSecondarySurface } from "../web/surfaces.mjs";

function installed() {
  return {
    schema: "tc.sqx-installed-data.v1",
    source_build: "144.2953",
    symbols: ["EURUSD", "DJ CFD", "GBPUSD_M1_dukas"],
    rows: [
      { symbol: "EURUSD", dataType: "3", timeframe: "M1", dateFrom: 1483228800000, dateTo: 1704067200000, rows: 2600000, show: true },
      { symbol: "DJ CFD", dataType: "5", timeframe: "H1", show: false },
    ],
    dataTypes: [{ key: "3", name: "Forex" }, { key: "5", name: "CFD" }],
    sessions: ["No Session", "London"],
    precisions: [{ key: "1", name: "Selected timeframe" }, { key: "2", name: "1 minute" }],
    swapTypes: ["money", "percent"],
    tripleSwapOptions: ["WEDNESDAY"],
    detail: "Official StrategyQuant X constants/getAll types plus main/getData symbols and sessions.",
  };
}

function moduleRecord() {
  return {
    schema: "tc.sqx-run-module.v1",
    source_build: "144.2953",
    module: "Data manager",
    kind: "inspect",
    status: "unavailable",
    reason_code: "native_module_archive_missing",
    detail: "Verified StrategyQuant X has no Data manager archive under user/projects. This desktop does not invent a data downloader or manager.",
    editor_wired: false,
    control: { available: false, reason_code: "trusted_launcher_not_configured", detail: "Set SQX_LAUNCHER_SHA256." },
  };
}

function fakeDocumentHost() {
  const host = {
    innerHTML: "",
    isConnected: true,
    dataset: {},
    listeners: {},
    addEventListener(type, fn) { this.listeners[type] = fn; },
    querySelector() { return null; },
    querySelectorAll() { return []; },
    contains() { return true; },
  };
  return host;
}

test("Data manager view model maps producer rows, type names, and hidden series without inventing symbols", () => {
  const view = dataManagerViewModel({ installed: installed(), module: moduleRecord() });
  assert.equal(view.state, "ready");
  assert.equal(view.symbols.length, 3);
  assert.deepEqual(view.symbols[0], { symbol: "EURUSD", dataType: "Forex", timeframe: "M1", dateFrom: 1483228800000, dateTo: 1704067200000, rows: 2600000, hidden: false });
  assert.equal(view.symbols[1].dataType, "CFD");
  assert.equal(view.symbols[1].hidden, true);
  assert.deepEqual(view.symbols[2], { symbol: "GBPUSD_M1_dukas", dataType: "", timeframe: "", dateFrom: null, dateTo: null, rows: null, hidden: false });
  assert.deepEqual(view.sessions, ["No Session", "London"]);
  assert.equal(dataManagerViewModel({}).state, "pending");
  assert.equal(dataManagerViewModel({ installedError: "SQX web is down" }).state, "unavailable");
});

test("Data manager renders installed data from the read model and fails closed when SQX is not running", () => {
  const ready = renderDataManager(dataManagerViewModel({ installed: installed(), module: moduleRecord() }));
  assert.match(ready, /data-data-manager-state="ready"/);
  assert.match(ready, /data-data-symbol="EURUSD"/);
  assert.match(ready, /data-date-from="2017\.01\.01"/);
  assert.match(ready, /data-date-to="2024\.01\.01"/);
  assert.match(ready, /2,600,000/);
  assert.match(ready, /Forex/);
  assert.match(ready, /hidden/);
  assert.match(ready, /London/);
  assert.match(ready, /Selected timeframe/);
  assert.match(ready, /WEDNESDAY/);
  assert.match(ready, /Native Data manager/);
  assert.match(ready, /Add, download \(Dukascopy, file, Darwinex, crypto, Yahoo, MT5 import\)/);
  assert.doesNotMatch(ready, /\$\s?\d|Net Profit/);

  const down = renderDataManager(dataManagerViewModel({ installedError: "StrategyQuant X local web is not running.", module: moduleRecord() }));
  assert.match(down, /data-data-manager-state="unavailable"/);
  assert.match(down, /StrategyQuant X is not running/);
  assert.match(down, /does not cache or invent a symbol list/);
  assert.doesNotMatch(down, /data-data-symbol=/);

  const empty = renderDataManager(dataManagerViewModel({ installed: { ...installed(), symbols: [], rows: [] } }));
  assert.match(empty, /reports no installed data series/);
});

test("Data manager surface mounts its own host instead of the generic inspect card", () => {
  const html = renderSecondarySurface({ surfaceId: "data-manager", label: "Data manager" }, {});
  assert.match(html, /data-data-manager-host/);
  assert.match(html, /read-only; adding or importing series stays in StrategyQuant X/);
  assert.doesNotMatch(html, /data-sqx-inspect-host/);
});

test("Data manager load composes module and installed-data reads and keeps the module card when data is unavailable", async () => {
  const calls = [];
  const okFetch = async (path) => {
    calls.push(path);
    if (path.startsWith("/api/sqx-module")) return { ok: true, status: 200, json: async () => moduleRecord() };
    if (path === "/api/sqx-installed-data") return { ok: true, status: 200, json: async () => installed() };
    throw new Error(`unexpected ${path}`);
  };
  const host = fakeDocumentHost();
  const view = await loadDataManager(host, { fetchImpl: okFetch });
  assert.equal(view.state, "ready");
  assert.equal(view.module.module, "Data manager");
  assert.match(host.innerHTML, /data-data-manager-state="ready"/);
  assert.ok(calls.includes("/api/sqx-installed-data"));

  const downFetch = async (path) => {
    if (path.startsWith("/api/sqx-module")) return { ok: true, status: 200, json: async () => moduleRecord() };
    return { ok: false, status: 503, json: async () => ({ reason_code: "sqx_web_unavailable", detail: "StrategyQuant X local web is not running." }) };
  };
  const down = await loadDataManager(fakeDocumentHost(), { fetchImpl: downFetch });
  assert.equal(down.state, "unavailable");
  assert.match(down.detail, /not running/);
  assert.equal(down.module.module, "Data manager");
});
