import assert from "node:assert/strict";
import test from "node:test";
import { bindDataSetup, dataSetupCatalog, dataSetupSelection, dataFileInspection, inspectDataFile, renderDataSetup, selectDataSetup, mt5TerminalCatalog, mt5Metadata, readMt5Metadata, mt5History, readMt5History, exportMt5History } from "../web/data-setup.mjs";

const source = { kind: "native_sqlite", relative_path: "user/data/data.db", snapshot_sha256: "a".repeat(64) };
const dataset = { dataset_id: "sqx-data-1", symbol: "EURUSD", timeframe: "M1", broker: { profile_id: 1, name: "Broker <one>" } };
const catalog = () => ({ schema: "tc.data-setup.v1", source_build: "144.2953", source, status: "available", datasets: [dataset], native_mt5_import: { native_component_present: true, product_import_wired: false } });
const selection = () => ({ schema: "tc.data-setup-selection.v1", source, dataset, status: "needs_review", fields: {
  default_spread: { value: 0, source: "native_instrument", state: "observed_native" }, timezone: { value: null, source: "native_dataset", state: "unresolved" },
}, unresolved: [{ field: "timezone", reason_code: "timezone_missing" }], conflicts: [], native_import_performed: false, backtest_ready: false });
const inspection = () => ({ schema: "tc.data-file-inspection.v1", source_sha256: "b".repeat(64), bytes: 31,
  columns: { date: "Date", time: "Time", timestamp: null, open: "Open", high: "High", low: "Low", close: "Close", volume: null, up: null, down: null, tick_volume: null, spread: null },
  row_count: 1, date_from: "2026-09-01 00:00", date_to: "2026-09-01 00:00", timestamp_timezone: null, observed_interval_seconds: null,
  issues: [{ code: "timezone_missing", count: 1, detail: "Timezone absent" }], status: "needs_review", native_import_performed: false, backtest_ready: false });
const ok = (payload) => ({ ok: true, json: async () => payload });
const deferred = () => { let resolve; const promise = new Promise((done) => { resolve = done; }); return { resolve, promise }; };
const tick = () => new Promise((resolve) => setImmediate(resolve));
function host() {
  return { dataset: {}, isConnected: true, innerHTML: "", handlers: {}, addEventListener(name, handler) { this.handlers[name] = handler; } };
}
function change(root, selector, extra) {
  return root.handlers.change({ target: { matches: (value) => value === selector, ...extra } });
}

test("selection sends only native dataset identity and snapshot, refuses changed authority", async () => {
  let call;
  const result = await selectDataSetup(dataset.dataset_id, source.snapshot_sha256, async (...args) => { call = args; return ok(selection()); });
  assert.deepEqual(JSON.parse(call[1].body), { dataset_id: dataset.dataset_id, snapshot_sha256: source.snapshot_sha256 });
  assert.equal(result.fields.default_spread.value, 0);
  assert.throws(() => dataSetupSelection({ ...selection(), backtest_ready: true }, dataset.dataset_id, source.snapshot_sha256));
  assert.throws(() => dataSetupSelection(selection(), "sqx-data-2", source.snapshot_sha256));
  assert.throws(() => dataSetupSelection(selection(), dataset.dataset_id, "c".repeat(64)));
  assert.throws(() => dataSetupCatalog({ ...catalog(), datasets: [dataset, dataset] }));
  assert.throws(() => dataSetupCatalog({ ...catalog(), native_mt5_import: { native_component_present: true, product_import_wired: true } }));
});

test("inspection uploads original bytes only and refuses oversized files and invented readiness", async () => {
  const file = new Blob(["Date,Time,Open,High,Low,Close\n"]);
  let call;
  await inspectDataFile(file, async (...args) => { call = args; return ok({ ...inspection(), bytes: file.size }); });
  assert.equal(call[0], "/api/data-setup/inspect");
  assert.equal(call[1].headers["content-type"], "application/octet-stream");
  assert.equal(call[1].body, file);
  await assert.rejects(inspectDataFile(file, async () => ok({ ...inspection(), bytes: file.size + 1 })), /byte count/);
  await assert.rejects(inspectDataFile({ size: 16 * 1024 * 1024 + 1 }, () => { throw new Error("must not fetch"); }), /16 MiB/);
  assert.throws(() => dataFileInspection({ ...inspection(), native_import_performed: true }));
  assert.throws(() => dataFileInspection({ ...inspection(), columns: { open: "Open" } }));
});

test("view separates native reference metadata from file evidence and preserves unknowns and zero", () => {
  const html = renderDataSetup({ catalog: catalog(), selection: selection(), inspection: inspection() });
  assert.match(html, /Selected reference settings/);
  assert.match(html, /do not establish the origin/);
  assert.match(html, /Default Spread<\/dt><dd>0/);
  assert.match(html, /Timezone<\/dt><dd>Unknown/);
  assert.match(html, /Broker &lt;one&gt;/);
  assert.match(html, /not a confirmed timeframe/);
  assert.match(html, /Nothing was imported/);
  assert.match(html, /Direct import is not available in this app yet/);
  assert.doesNotMatch(html, /<button[^>]*>Import|Backtest ready|Ready<\/span>/);
});

test("real binder ignores catalog and selection completion after navigation and prevents duplicate selection", async () => {
  const first = host(), lateCatalog = deferred();
  bindWithoutMt5(first, () => lateCatalog.promise);
  const loading = first.innerHTML;
  first.isConnected = false;
  lateCatalog.resolve(ok(catalog()));
  await tick();
  assert.equal(first.innerHTML, loading);

  const root = host(), lateSelection = deferred();
  let selections = 0;
  bindWithoutMt5(root, (path) => path.endsWith("/select") ? (++selections, lateSelection.promise) : Promise.resolve(ok(catalog())));
  await tick();
  const pending = change(root, "[data-data-dataset]", { value: dataset.dataset_id });
  await change(root, "[data-data-dataset]", { value: dataset.dataset_id });
  assert.equal(selections, 1);
  assert.match(root.innerHTML, /data-data-dataset disabled/);
  const before = root.innerHTML;
  root.isConnected = false;
  lateSelection.resolve(ok(selection()));
  await pending;
  assert.equal(root.innerHTML, before);
});

test("file failure retains selected native metadata without rereading authority; stale file cannot repaint", async () => {
  const root = host(), lateFile = deferred();
  let catalogs = 0;
  bindWithoutMt5(root, async (path) => {
    if (path.endsWith("/select")) return ok(selection());
    if (path.endsWith("/inspect")) return lateFile.promise;
    catalogs++; return ok(catalog());
  });
  await tick();
  await change(root, "[data-data-dataset]", { value: dataset.dataset_id });
  const pending = change(root, "[data-data-file]", { files: [Object.assign(new Blob(["data"]), { name: "prices.csv" })] });
  lateFile.resolve({ ok: false, status: 422, json: async () => ({ detail: "Price columns missing" }) });
  await pending;
  assert.equal(catalogs, 1);
  assert.match(root.innerHTML, /Selected reference settings/);
  assert.match(root.innerHTML, /Price columns missing/);
  const other = host(), stale = deferred();
  bindWithoutMt5(other, (path) => path.endsWith("/inspect") ? stale.promise : Promise.resolve(ok(catalog())));
  await tick();
  const old = change(other, "[data-data-file]", { files: [Object.assign(new Blob(["data"]), { name: "old.csv" })] });
  const before = other.innerHTML;
  other.isConnected = false;
  stale.resolve(ok(inspection()));
  await old;
  assert.equal(other.innerHTML, before);
});

const terminal = { terminal_id: "mt5-123", identity_sha256: "d".repeat(64), label: "MetaTrader 5", running: true };
const terminalCatalog = (terminals = [terminal]) => ({ schema: "tc.mt5-terminals.v1", status: terminals.length ? "available" : "no_running_terminal", terminals, reason_code: null, detail: "Open MT5 to read broker settings", native_import_performed: false, backtest_ready: false });
function bindWithoutMt5(root, fetchImpl) {
  return bindDataSetup(root, (path, options) => path.includes("/mt5/") ? Promise.resolve(ok(terminalCatalog([]))) : fetchImpl(path, options));
}
function symbol(name = "EURUSD.raw") {
  return { name, description: "Raw broker symbol", path: "FX", currency_base: "EUR", currency_profit: "USD", currency_margin: null, digits: 5, spread: 0, trade_calc_mode: 0, point: 0.00001, trade_tick_size: 0.00001, trade_tick_value: 1, trade_tick_value_profit: 1, trade_tick_value_loss: 1, trade_contract_size: 100000, volume_min: 0.01, volume_max: 100, volume_step: 0.01, swap_long: null, swap_short: null, swap_mode: null, spread_float: true };
}
const metadata = (identity = terminal) => ({ schema: "tc.mt5-metadata.v1", status: "observed", terminal_id: identity.terminal_id, identity_sha256: identity.identity_sha256,
  terminal: { company: "Terminal vendor", build: 5000, connected: true }, broker: { company: "Broker <demo>", server: "Broker-Demo", currency: "EUR" },
  symbols: [symbol()], selected_symbol: null, unresolved: ["timezone", "bar_timestamp_convention", "commission", "slippage", "trading_sessions"],
  source: { kind: "mt5_terminal_api", producer: "MetaTrader5", runtime_build: "144.2953", observed_at_utc: "2026-09-05T04:00:00Z", broker_sha256: "e".repeat(64) }, native_import_performed: false, backtest_ready: false });
function click(root, selector) { return root.handlers.click({ target: { closest: (value) => value === selector ? {} : null } }); }

test("MT5 read sends server-issued identity only and rejects invented authority or mismatched metadata", async () => {
  let sent;
  const result = await readMt5Metadata(terminal.terminal_id, terminal.identity_sha256, async (path, options) => { sent = {path,body:JSON.parse(options.body)}; return ok(metadata()); });
  assert.deepEqual(sent, { path: "/api/data-setup/mt5/read", body: { terminal_id: terminal.terminal_id, identity_sha256: terminal.identity_sha256 } });
  assert.equal(result.symbols[0].spread, 0);
  assert.doesNotThrow(() => mt5Metadata({ ...metadata(), source: { ...metadata().source, observed_at_utc: "2026-09-05T04:00:00+00:00" } }, terminal.terminal_id, terminal.identity_sha256));
  assert.throws(() => mt5Metadata({ ...metadata(), status: "failed" }, terminal.terminal_id, terminal.identity_sha256));
  await assert.rejects(readMt5Metadata("C:/terminal64.exe", terminal.identity_sha256, () => assert.fail("no arbitrary path")));
  assert.throws(() => mt5Metadata(metadata(), "mt5-456", terminal.identity_sha256));
  assert.throws(() => mt5Metadata({...metadata(), backtest_ready:true}, terminal.terminal_id, terminal.identity_sha256));
  assert.throws(() => mt5Metadata({...metadata(), source:{...metadata().source, observed_at_utc:"yesterday"}}, terminal.terminal_id, terminal.identity_sha256));
  assert.throws(() => mt5Metadata({...metadata(), symbols:[{...symbol(), trade_tick_size:NaN}]}, terminal.terminal_id, terminal.identity_sha256));
  assert.throws(() => mt5TerminalCatalog({...terminalCatalog(), terminals:[terminal,terminal]}));
});

test("MT5 requires an explicit read; terminal and symbol selection never connect", async () => {
  const root=host(), calls=[];
  bindDataSetup(root, async(path,options)=>{ calls.push({path,options}); return ok(path.endsWith('/terminals') ? terminalCatalog() : path.endsWith('/read') ? metadata() : catalog()); });
  await tick();
  assert.equal(calls.filter(call=>call.options.method==='POST').length,0);
  await change(root,'[data-mt5-terminal]',{value:terminal.terminal_id});
  assert.equal(calls.filter(call=>call.options.method==='POST').length,0);
  assert.match(root.innerHTML,/may reopen the selected terminal/);
  await click(root,'[data-mt5-read]');
  assert.equal(calls.filter(call=>call.options.method==='POST').length,1);
  await change(root,'[data-mt5-symbol]',{value:'EURUSD.raw'});
  assert.equal(calls.filter(call=>call.options.method==='POST').length,1);
  assert.match(root.innerHTML,/Broker &lt;demo&gt;/);
  assert.match(root.innerHTML,/Account currency<\/span><strong[^>]*>EUR/);
  assert.match(root.innerHTML,/Spread \(points\)<\/span><strong[^>]*>0/);
  assert.match(root.innerHTML,/Tick value is not an SQX point value/);
  assert.match(root.innerHTML,/2026-09-05T04:00:00Z/);
  assert.doesNotMatch(root.innerHTML,/\$1|Backtest ready/);
});

test("MT5 pending read locks duplicate actions and late result cannot bind after navigation", async () => {
  const root=host(), pending=deferred(); let reads=0;
  bindDataSetup(root, async(path)=>path.endsWith('/read') ? (++reads,pending.promise) : ok(path.endsWith('/terminals') ? terminalCatalog() : catalog()));
  await tick();
  await change(root,'[data-mt5-terminal]',{value:terminal.terminal_id});
  const active=click(root,'[data-mt5-read]');
  await click(root,'[data-mt5-read]');
  await change(root,'[data-mt5-terminal]',{value:'mt5-456'});
  assert.equal(reads,1);
  assert.match(root.innerHTML,/data-mt5-terminal disabled/);
  const before=root.innerHTML;
  root.isConnected=false;
  pending.resolve(ok(metadata()));
  await active;
  assert.equal(root.innerHTML,before);
});

test("no terminal guidance and MT5 read failure preserve other data work", async () => {
  const empty=renderDataSetup({mt5Catalog:terminalCatalog([])});
  assert.match(empty,/Open MetaTrader 5 and sign in to your broker/);
  assert.doesNotMatch(empty,/data-mt5-read/);
  const root=host();
  let reads=0;
  bindDataSetup(root, async(path)=>path.endsWith('/terminals') ? ok(terminalCatalog()) : path.endsWith('/select') ? ok(selection()) : path.endsWith('/read') ? (++reads===1 ? ok(metadata()) : {ok:false,status:409,json:async()=>({detail:'Selected terminal closed'})}) : ok(catalog()));
  await tick();
  await change(root,'[data-data-dataset]',{value:dataset.dataset_id});
  await change(root,'[data-mt5-terminal]',{value:terminal.terminal_id});
  await click(root,'[data-mt5-read]');
  assert.match(root.innerHTML,/Reported broker/);
  await click(root,'[data-mt5-read]');
  assert.match(root.innerHTML,/Selected terminal closed/);
  assert.match(root.innerHTML,/Selected reference settings/);
  assert.doesNotMatch(root.innerHTML,/Reported broker/);
});

test("MT5 symbol search sends a bounded literal and binds the observed filter", async () => {
  let sent;
  const filtered = { ...metadata(), source: { ...metadata().source, symbol_filter: "EURUSD" } };
  await readMt5Metadata(terminal.terminal_id, terminal.identity_sha256, async (path, options) => { sent = JSON.parse(options.body); return ok(filtered); }, " EURUSD ");
  assert.deepEqual(sent, { terminal_id: terminal.terminal_id, identity_sha256: terminal.identity_sha256, symbol_filter: "EURUSD" });
  for (const invalid of ["E", "EUR*", "EUR!", "EUR?", "EUR,USD", "EUR;USD", "EUR\\USD", "éuro", "A".repeat(65)]) {
    await assert.rejects(readMt5Metadata(terminal.terminal_id, terminal.identity_sha256, () => assert.fail("Invalid filter must not connect"), invalid), /Symbol search/);
  }
  assert.throws(() => mt5Metadata(filtered, terminal.terminal_id, terminal.identity_sha256, "GBPUSD"));
  assert.throws(() => mt5Metadata(metadata(), terminal.terminal_id, terminal.identity_sha256, "EURUSD"));
});

test("typing symbol search never connects or relabels an earlier metadata snapshot", async () => {
  const root = host(), reads = [];
  bindDataSetup(root, async (path, options) => {
    if (path.endsWith('/terminals')) return ok(terminalCatalog());
    if (path.endsWith('/read')) {
      const payload = JSON.parse(options.body); reads.push(payload);
      return ok({ ...metadata(), source: { ...metadata().source, symbol_filter: payload.symbol_filter || null } });
    }
    return ok(catalog());
  });
  await tick();
  await change(root, '[data-mt5-terminal]', { value: terminal.terminal_id });
  const typeFilter = (value) => root.handlers.input({ target: { matches: (selector) => selector === '[data-mt5-filter]', value } });
  typeFilter('EURUSD');
  assert.equal(reads.length, 0);
  await click(root, '[data-mt5-read]');
  assert.equal(reads.length, 1);
  assert.equal(reads[0].symbol_filter, 'EURUSD');
  typeFilter('GBPUSD');
  await change(root, '[data-mt5-symbol]', { value: 'EURUSD.raw' });
  assert.equal(reads.length, 1);
  assert.match(root.innerHTML, /data-mt5-filter value="GBPUSD"/);
  assert.match(root.innerHTML, /Symbol search used<\/span><strong[^>]*>EURUSD/);
  assert.doesNotMatch(root.innerHTML, /Symbol search used<\/span><strong[^>]*>GBPUSD/);
});

const historyRequest = () => ({ terminal_id: terminal.terminal_id, identity_sha256: terminal.identity_sha256, broker_sha256: metadata().source.broker_sha256, symbol: symbol().name, timeframe: "H1", date_from: "2026-09-01", date_to: "2026-09-02" });
const history = (request = historyRequest()) => ({ schema: "tc.mt5-history.v1", status: "captured", request,
  history_ref: `tc-evidence:sha256:${"f".repeat(64)}`, csv_ref: `tc-evidence:sha256:${"a".repeat(64)}`, source_sha256: "a".repeat(64), bytes: 10,
  row_count: 2, date_from: `${request.date_from}T00:00:00Z`, date_to: `${request.date_from}T01:00:00Z`, timezone: "UTC", bar_timestamp_convention: "start_of_bar", coverage_complete: null, gap_count: 0,
  broker: metadata().broker, symbol_metadata: symbol(request.symbol), source: metadata().source, unresolved: ["broker_timezone", "trading_sessions", "commission", "slippage", "native_broker_profile"], native_import_performed: false, backtest_ready: false });

test("history sends exact reported identity and UTC selection; rejects invalid ranges and mismatched provenance", async () => {
  let sent;
  await readMt5History(metadata(), symbol().name, "H1", "2026-09-01", "2026-09-02", async (path, options) => { sent = { path, body: JSON.parse(options.body) }; return ok(history()); });
  assert.deepEqual(sent, { path: "/api/data-setup/mt5/history", body: historyRequest() });
  const noFetch = () => assert.fail("Invalid history request must not connect");
  for (const args of [["unreported", "H1", "2026-09-01", "2026-09-02"], [symbol().name, "H2", "2026-09-01", "2026-09-02"], [symbol().name, "H1", "2026-02-30", "2026-03-01"], [symbol().name, "H1", "2026-09-02", "2026-09-02"], [symbol().name, "M1", "2026-08-01", "2026-09-01"], [symbol().name, "H1", "2999-01-01", "2999-01-02"]]) {
    await assert.rejects(readMt5History(metadata(), ...args, noFetch), /completed UTC date range/);
  }
  for (const patch of [{ request: { ...historyRequest(), symbol: "GBPUSD" } }, { source: { ...metadata().source, broker_sha256: "0".repeat(64) } }, { broker: { ...metadata().broker, server: "Different" } }, { symbol_metadata: symbol("GBPUSD") }, { date_to: "2026-09-02T00:00:00Z" }, { coverage_complete: true }, { backtest_ready: true }, { csv_ref: `tc-evidence:sha256:${"b".repeat(64)}` }]) {
    assert.throws(() => mt5History({ ...history(), ...patch }, historyRequest(), metadata().broker), /does not match/);
  }
});

async function historyHost(fetchHistory) {
  const root = host(), calls = [];
  bindDataSetup(root, async (path, options) => {
    calls.push({ path, options });
    if (path.endsWith('/history') || path.endsWith('/export')) return fetchHistory(path, options);
    return ok(path.endsWith('/terminals') ? terminalCatalog() : path.endsWith('/read') ? metadata() : path.endsWith('/select') ? selection() : catalog());
  });
  await tick();
  await change(root, '[data-data-dataset]', { value: dataset.dataset_id });
  await change(root, '[data-mt5-terminal]', { value: terminal.terminal_id });
  await click(root, '[data-mt5-read]');
  await change(root, '[data-mt5-symbol]', { value: symbol().name });
  await change(root, '[data-mt5-from]', { value: '2026-09-01' });
  await change(root, '[data-mt5-to]', { value: '2026-09-02' });
  return { root, calls };
}

test("history is explicit, locks all MT5 actions while pending, clears stale captures on changes and failures", async () => {
  let reads = 0;
  const pending = deferred();
  const { root, calls } = await historyHost(async () => ++reads === 1 ? pending.promise : { ok: false, status: 409, json: async () => ({ detail: "Terminal history unavailable" }) });
  assert.equal(calls.filter((call) => call.path.endsWith('/history')).length, 0);
  const active = click(root, '[data-mt5-history]');
  await click(root, '[data-mt5-history]');
  await click(root, '[data-mt5-read]');
  await click(root, '[data-mt5-refresh]');
  await change(root, '[data-mt5-symbol]', { value: '' });
  await change(root, '[data-mt5-from]', { value: '2026-08-01' });
  assert.equal(reads, 1);
  assert.match(root.innerHTML, /data-mt5-symbol disabled/);
  pending.resolve(ok(history()));
  await active;
  assert.match(root.innerHTML, /Captured history/);
  assert.match(root.innerHTML, /not confirmed missing bars/);
  assert.match(root.innerHTML, /Coverage<\/span><strong[^>]*>Not confirmed/);
  assert.match(root.innerHTML, /Selected reference settings/);
  await click(root, '[data-mt5-history]');
  assert.match(root.innerHTML, /Terminal history unavailable/);
  assert.doesNotMatch(root.innerHTML, /Captured history|Download captured CSV/);
  assert.match(root.innerHTML, /Reported broker/);
  const captured = await historyHost(async () => ok(history()));
  for (const [selector, value] of [['[data-mt5-timeframe]', 'H4'], ['[data-mt5-from]', '2026-09-01'], ['[data-mt5-to]', '2026-09-02'], ['[data-mt5-symbol]', symbol().name], ['[data-mt5-terminal]', terminal.terminal_id]]) {
    // Reset the date/time selection before each explicit capture.
    await change(captured.root, '[data-mt5-timeframe]', { value: 'H1' });
    await click(captured.root, '[data-mt5-history]');
    assert.match(captured.root.innerHTML, /Captured history/);
    const before = captured.calls.length;
    await change(captured.root, selector, { value });
    assert.equal(captured.calls.length, before);
    assert.doesNotMatch(captured.root.innerHTML, /Captured history|Download captured CSV/);
  }
});

test("history completion after navigation cannot paint or download; CSV download verifies the captured bytes", async () => {
  const pending = deferred();
  const { root } = await historyHost(() => pending.promise);
  const active = click(root, '[data-mt5-history]');
  const before = root.innerHTML;
  root.isConnected = false;
  pending.resolve(ok(history()));
  await active;
  assert.equal(root.innerHTML, before);
  const csv = new Blob(['time,open\n2026-09-01T00:00:00Z,1\n'], { type: 'text/csv' });
  const hash = Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256', await csv.arrayBuffer())), (byte) => byte.toString(16).padStart(2, '0')).join('');
  const capture = { ...history(), bytes: csv.size, source_sha256: hash, csv_ref: `tc-evidence:sha256:${hash}` };
  let body;
  const fetchCsv = async (path, options) => { assert.equal(path, '/api/data-setup/mt5/history/export'); body = JSON.parse(options.body); return new Response(csv, { headers: { 'content-type': 'text/csv; charset=utf-8' } }); };
  assert.equal((await exportMt5History(capture, fetchCsv)).size, csv.size);
  assert.deepEqual(body, { history_ref: capture.history_ref });
  await assert.rejects(exportMt5History({ ...capture, source_sha256: 'a'.repeat(64) }, fetchCsv), /does not match/);
  let downloaded;
  const ui = await historyHost((path, options) => path.endsWith('/export') ? fetchCsv(path, options) : ok(capture));
  ui.root.ownerDocument = { createElement: () => ({ click() { downloaded = { href: this.href, filename: this.download }; } }) };
  await click(ui.root, '[data-mt5-history]');
  await click(ui.root, '[data-mt5-export]');
  assert.equal(downloaded.filename, 'MT5-history.csv');
  assert.match(downloaded.href, /^blob:/);
  await new Promise((resolve) => setTimeout(resolve, 10));
  await assert.rejects(fetch(downloaded.href));
});
