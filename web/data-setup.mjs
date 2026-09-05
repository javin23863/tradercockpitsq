import { escapeHtml, readable, statList, unavailable } from "./ui.mjs";

const API = "/api/data-setup";
const MT5_API = `${API}/mt5`;
const MAX_BYTES = 16 * 1024 * 1024;
const object = (value) => value && typeof value === "object" && !Array.isArray(value);
const digest = (value) => typeof value === "string" && /^[a-f0-9]{64}$/.test(value);
const text = (value) => value === null || value === undefined ? "Unknown" : typeof value === "object" ? JSON.stringify(value) : String(value);
const sourceValid = (source) => object(source) && source.kind === "native_sqlite" && typeof source.relative_path === "string" && digest(source.snapshot_sha256);
const MT5_SYMBOL_TEXT = ["description", "path", "currency_base", "currency_profit", "currency_margin"];
const MT5_SYMBOL_NUMBERS = ["digits", "spread", "trade_calc_mode", "point", "trade_tick_size", "trade_tick_value", "trade_tick_value_profit", "trade_tick_value_loss", "trade_contract_size", "volume_min", "volume_max", "volume_step", "swap_long", "swap_short", "swap_mode"];
const nullableText = (value) => value === null || typeof value === "string";
const nullableNumber = (value) => value === null || (typeof value === "number" && Number.isFinite(value));
const mt5Identity = (value) => typeof value === "string" && /^mt5-[1-9][0-9]*$/.test(value);
const noExecution = (value) => value.native_import_performed === false && value.backtest_ready === false;
const MT5_TIMEFRAMES = { M1: 60, M5: 300, M15: 900, M30: 1800, H1: 3600, H4: 14400, D1: 86400 };
const utcTimestamp = (value) => typeof value === "string" && /(Z|\+00:00)$/.test(value) && Number.isFinite(Date.parse(value));
const evidenceRef = (value) => typeof value === "string" && /^tc-evidence:sha256:[a-f0-9]{64}$/.test(value);

export function mt5TerminalCatalog(payload) {
  if (!object(payload) || payload.schema !== "tc.mt5-terminals.v1" || !["available", "no_running_terminal", "unavailable"].includes(payload.status)
    || !Array.isArray(payload.terminals) || payload.terminals.some((row) => !object(row) || !mt5Identity(row.terminal_id) || !digest(row.identity_sha256) || typeof row.label !== "string" || row.running !== true)
    || new Set(payload.terminals.map((row) => row.terminal_id)).size !== payload.terminals.length
    || (payload.status === "available" ? !payload.terminals.length : payload.terminals.length !== 0)
    || !nullableText(payload.reason_code) || typeof payload.detail !== "string" || !noExecution(payload)) {
    throw new Error("MT5 terminal discovery response is invalid");
  }
  return payload;
}

function mt5SymbolValid(row) {
  return object(row) && typeof row.name === "string" && row.name.trim()
    && MT5_SYMBOL_TEXT.every((key) => nullableText(row[key]))
    && MT5_SYMBOL_NUMBERS.every((key) => nullableNumber(row[key]))
    && (row.spread_float === null || typeof row.spread_float === "boolean");
}

export function mt5Metadata(payload, terminalId, identity, symbolFilter = "") {
  if (!object(payload) || payload.schema !== "tc.mt5-metadata.v1" || payload.status !== "observed" || payload.terminal_id !== terminalId || payload.identity_sha256 !== identity
    || !object(payload.terminal) || !nullableText(payload.terminal.company) || !nullableNumber(payload.terminal.build) || typeof payload.terminal.connected !== "boolean"
    || !object(payload.broker) || !["company", "server", "currency"].every((key) => nullableText(payload.broker[key]))
    || !Array.isArray(payload.symbols) || payload.symbols.some((row) => !mt5SymbolValid(row))
    || new Set(payload.symbols.map((row) => row.name)).size !== payload.symbols.length
    || !(payload.selected_symbol === null || (mt5SymbolValid(payload.selected_symbol) && payload.symbols.some((row) => row.name === payload.selected_symbol.name)))
    || !Array.isArray(payload.unresolved) || payload.unresolved.some((key) => typeof key !== "string")
    || payload.source?.kind !== "mt5_terminal_api" || payload.source.producer !== "MetaTrader5" || payload.source.runtime_build !== "144.2953"
    || !digest(payload.source.broker_sha256)
    || (payload.source.symbol_filter ?? null) !== (symbolFilter || null)
    || typeof payload.source.observed_at_utc !== "string" || !/(Z|\+00:00)$/.test(payload.source.observed_at_utc) || !Number.isFinite(Date.parse(payload.source.observed_at_utc)) || !noExecution(payload)) {
    throw new Error("MT5 metadata does not match the selected terminal identity");
  }
  return payload;
}

export async function fetchMt5Terminals(fetchImpl = globalThis.fetch) {
  return mt5TerminalCatalog(await request(`${MT5_API}/terminals`, { headers: { accept: "application/json" } }, fetchImpl));
}

export async function readMt5Metadata(terminalId, identity, fetchImpl = globalThis.fetch, symbolFilter = "") {
  if (!mt5Identity(terminalId) || !digest(identity)) throw new Error("Choose a discovered MT5 terminal first");
  if (typeof symbolFilter !== "string") throw new Error("Symbol search must be text");
  const filter = symbolFilter.trim();
  if (filter && (filter.length < 2 || filter.length > 64 || !/^[A-Za-z0-9._# /-]+$/.test(filter))) {
    throw new Error("Symbol search needs 2–64 letters, numbers, spaces, or . _ # / -. Do not use wildcards.");
  }
  return mt5Metadata(await request(`${MT5_API}/read`, {
    method: "POST", headers: { "content-type": "application/json", accept: "application/json" },
    body: JSON.stringify({ terminal_id: terminalId, identity_sha256: identity, ...(filter ? { symbol_filter: filter } : {}) }),
  }, fetchImpl), terminalId, identity, filter);
}

function historyRequestValid(value) {
  const day = (date) => typeof date === "string" && /^\d{4}-\d{2}-\d{2}$/.test(date) && Number.isFinite(Date.parse(date)) && new Date(date).toISOString().slice(0, 10) === date;
  return object(value) && Object.keys(value).sort().join() === "broker_sha256,date_from,date_to,identity_sha256,symbol,terminal_id,timeframe"
    && mt5Identity(value.terminal_id) && digest(value.identity_sha256) && digest(value.broker_sha256)
    && typeof value.symbol === "string" && value.symbol.trim() && Object.hasOwn(MT5_TIMEFRAMES, value.timeframe)
    && day(value.date_from) && day(value.date_to) && value.date_from < value.date_to && value.date_to <= new Date().toISOString().slice(0, 10)
    && (Date.parse(value.date_to) - Date.parse(value.date_from)) / (MT5_TIMEFRAMES[value.timeframe] * 1000) <= 10000;
}

export function mt5History(payload, requested, broker) {
  if (!historyRequestValid(requested) || !object(payload) || payload.schema !== "tc.mt5-history.v1" || payload.status !== "captured"
    || !historyRequestValid(payload.request) || Object.keys(requested).some((key) => payload.request[key] !== requested[key])
    || !evidenceRef(payload.history_ref) || !evidenceRef(payload.csv_ref) || !digest(payload.source_sha256) || payload.csv_ref !== `tc-evidence:sha256:${payload.source_sha256}`
    || !Number.isInteger(payload.bytes) || payload.bytes < 1 || !Number.isInteger(payload.row_count) || payload.row_count < 1 || payload.row_count > 10000
    || !utcTimestamp(payload.date_from) || !utcTimestamp(payload.date_to) || Date.parse(payload.date_from) > Date.parse(payload.date_to)
    || Date.parse(payload.date_from) < Date.parse(requested.date_from) || Date.parse(payload.date_to) >= Date.parse(requested.date_to)
    || payload.timezone !== "UTC" || payload.bar_timestamp_convention !== "start_of_bar" || payload.coverage_complete !== null
    || !Number.isInteger(payload.gap_count) || payload.gap_count < 0 || !object(payload.broker) || !object(broker)
    || ["company", "server", "currency"].some((key) => typeof payload.broker[key] !== "string" || payload.broker[key] !== broker[key])
    || !mt5SymbolValid(payload.symbol_metadata) || payload.symbol_metadata.name !== requested.symbol
    || payload.source?.kind !== "mt5_terminal_api" || payload.source.producer !== "MetaTrader5" || payload.source.runtime_build !== "144.2953"
    || payload.source.broker_sha256 !== requested.broker_sha256 || !utcTimestamp(payload.source.observed_at_utc)
    || !Array.isArray(payload.unresolved) || payload.unresolved.some((key) => typeof key !== "string") || !noExecution(payload)) {
    throw new Error("MT5 history does not match the requested broker, symbol, or UTC range");
  }
  return payload;
}

export async function readMt5History(metadata, symbol, timeframe, dateFrom, dateTo, fetchImpl = globalThis.fetch) {
  const requested = { terminal_id: metadata?.terminal_id, identity_sha256: metadata?.identity_sha256, broker_sha256: metadata?.source?.broker_sha256, symbol, timeframe, date_from: dateFrom, date_to: dateTo };
  if (!historyRequestValid(requested) || !metadata.symbols.some((row) => row.name === symbol)) throw new Error("Choose a reported symbol and a completed UTC date range of at most 10,000 bars");
  return mt5History(await request(`${MT5_API}/history`, { method: "POST", headers: { "content-type": "application/json", accept: "application/json" }, body: JSON.stringify(requested) }, fetchImpl), requested, metadata.broker);
}

export async function exportMt5History(history, fetchImpl = globalThis.fetch) {
  if (!evidenceRef(history?.history_ref) || !digest(history.source_sha256)) throw new Error("Read price history before downloading it");
  const response = await fetchImpl(`${MT5_API}/history/export`, { method: "POST", headers: { "content-type": "application/json", accept: "text/csv" }, body: JSON.stringify({ history_ref: history.history_ref }) });
  if (!response.ok) {
    let payload;
    try { payload = await response.json(); } catch { /* Keep the HTTP failure if no JSON detail exists. */ }
    throw new Error(payload?.detail || `History download failed: ${response.status}`);
  }
  if (!response.headers.get("content-type")?.startsWith("text/csv")) throw new Error("History download is not CSV");
  const blob = await response.blob();
  const hash = Array.from(new Uint8Array(await crypto.subtle.digest("SHA-256", await blob.arrayBuffer())), (byte) => byte.toString(16).padStart(2, "0")).join("");
  if (blob.size !== history.bytes || hash !== history.source_sha256) throw new Error("History download does not match the captured CSV");
  return blob;
}

export function dataSetupCatalog(payload) {
  if (!object(payload) || payload.schema !== "tc.data-setup.v1" || payload.source_build !== "144.2953"
    || !["available", "unavailable"].includes(payload.status) || !Array.isArray(payload.datasets)
    || (payload.status === "available" && !sourceValid(payload.source))
    || payload.datasets.some((row) => !object(row) || typeof row.dataset_id !== "string" || !row.dataset_id || typeof row.symbol !== "string" || !row.symbol
      || !(row.broker === null || (object(row.broker) && ["string", "number"].includes(typeof row.broker.profile_id) && (row.broker.name === null || typeof row.broker.name === "string"))))
    || new Set(payload.datasets.map((row) => row.dataset_id)).size !== payload.datasets.length
    || !object(payload.native_mt5_import) || typeof payload.native_mt5_import.native_component_present !== "boolean" || payload.native_mt5_import.product_import_wired !== false) {
    throw new Error("Installed data response is invalid");
  }
  return payload;
}

export function dataSetupSelection(payload, datasetId, snapshot) {
  if (!object(payload) || payload.schema !== "tc.data-setup-selection.v1" || !["resolved", "needs_review"].includes(payload.status)
    || payload.dataset?.dataset_id !== datasetId || !sourceValid(payload.source) || payload.source.snapshot_sha256 !== snapshot
    || !object(payload.fields) || Object.values(payload.fields).some((field) => !object(field)
      || !(field.value === null || typeof field.value === "string" || (typeof field.value === "number" && Number.isFinite(field.value)))
      || !["native_dataset", "native_instrument", "native_broker_profile"].includes(field.source)
      || !["observed_native", "unresolved", "conflict"].includes(field.state))
    || !Array.isArray(payload.unresolved) || !Array.isArray(payload.conflicts)
    || [...payload.unresolved, ...payload.conflicts].some((row) => !object(row) || typeof row.field !== "string" || typeof row.reason_code !== "string")
    || (payload.status === "resolved" && (payload.unresolved.length || payload.conflicts.length))
    || payload.native_import_performed !== false || payload.backtest_ready !== false) {
    throw new Error("Selected data response does not match this dataset snapshot");
  }
  return payload;
}

export function dataFileInspection(payload) {
  const columns = ["date", "time", "timestamp", "open", "high", "low", "close", "volume", "up", "down", "tick_volume", "spread"];
  if (!object(payload) || payload.schema !== "tc.data-file-inspection.v1" || !digest(payload.source_sha256)
    || !Number.isInteger(payload.bytes) || payload.bytes < 1 || payload.bytes > MAX_BYTES
    || !Number.isInteger(payload.row_count) || payload.row_count < 0 || !object(payload.columns)
    || columns.some((key) => payload.columns[key] !== null && typeof payload.columns[key] !== "string")
    || ![payload.date_from, payload.date_to, payload.timestamp_timezone].every((value) => value === null || typeof value === "string")
    || !(payload.observed_interval_seconds === null || (Number.isFinite(payload.observed_interval_seconds) && payload.observed_interval_seconds > 0))
    || !Array.isArray(payload.issues) || payload.issues.some((issue) => !object(issue) || typeof issue.code !== "string" || !Number.isInteger(issue.count) || issue.count < 0 || typeof issue.detail !== "string")
    || !["inspected", "needs_review"].includes(payload.status) || payload.native_import_performed !== false || payload.backtest_ready !== false) {
    throw new Error("Price file inspection response is invalid");
  }
  return payload;
}

async function request(path, options, fetchImpl) {
  const response = await fetchImpl(path, options);
  let payload;
  try { payload = await response.json(); } catch { throw new Error("Data setup returned unreadable data"); }
  if (!response.ok) throw new Error(payload?.detail || `Data setup request failed: ${response.status}`);
  return payload;
}

export async function fetchDataSetup(fetchImpl = globalThis.fetch) {
  return dataSetupCatalog(await request(API, { headers: { accept: "application/json" } }, fetchImpl));
}

export async function selectDataSetup(datasetId, snapshot, fetchImpl = globalThis.fetch) {
  if (typeof datasetId !== "string" || !datasetId || !digest(snapshot)) throw new Error("Select an installed dataset snapshot first");
  return dataSetupSelection(await request(`${API}/select`, {
    method: "POST", headers: { "content-type": "application/json", accept: "application/json" },
    body: JSON.stringify({ dataset_id: datasetId, snapshot_sha256: snapshot }),
  }, fetchImpl), datasetId, snapshot);
}

export async function inspectDataFile(file, fetchImpl = globalThis.fetch) {
  if (!file || !Number.isInteger(file.size) || file.size < 1 || file.size > MAX_BYTES) throw new Error("Choose a nonempty price file up to 16 MiB");
  const result = dataFileInspection(await request(`${API}/inspect`, {
    method: "POST", headers: { "content-type": "application/octet-stream", accept: "application/json" }, body: file,
  }, fetchImpl));
  if (result.bytes !== file.size) throw new Error("Inspection byte count does not match this file");
  return result;
}

function evidence(source) {
  return `<details class="data-setup-evidence"><summary>Source details</summary><pre>${escapeHtml(text(source))}</pre></details>`;
}

function renderSelection(selection) {
  if (!selection) return '<p class="note">Choose an installed dataset to fill its native settings automatically.</p>';
  const questions = [...selection.unresolved, ...selection.conflicts];
  return `<h3>Selected reference settings</h3><p class="note">Read from the installed engine. These settings do not establish the origin of a file you inspect.</p>
    <dl class="data-setup-fields">${Object.entries(selection.fields).map(([name, field]) => `<div><dt>${escapeHtml(readable(name))}</dt><dd>${name === "commissions" && field.value !== null ? `<details><summary>Native commission settings</summary><pre>${escapeHtml(text(field.value))}</pre></details>` : escapeHtml(name === "bar_timestamp_convention" && field.value !== null ? readable(field.value) : text(field.value))}${evidence(field.source)}</dd></div>`).join("")}</dl>
    <p class="note">Contract and cost values use native units. Currency is not reported here.</p>
    ${questions.length ? `<h3>Needs review</h3><ul>${questions.map((item) => `<li><strong>${escapeHtml(readable(item.field))}</strong>: ${escapeHtml(readable(item.reason_code))}${"dataset_value" in item ? ` — dataset: ${escapeHtml(text(item.dataset_value))}; broker: ${escapeHtml(text(item.broker_value))}` : ""}</li>`).join("")}</ul>` : '<p class="note">Available reference fields are filled. Backtest setup has not been applied or verified.</p>'}${evidence(selection.source)}`;
}

function renderInspection(result) {
  if (!result) return "";
  return `<h3>Detected file contents</h3>${statList([
    ["Rows", result.row_count], ["First timestamp", text(result.date_from)], ["Last timestamp", text(result.date_to)],
    ["Timestamp timezone", text(result.timestamp_timezone)], ["Observed spacing (seconds)", text(result.observed_interval_seconds)],
  ])}<p class="note">Observed spacing is a suggestion, not a confirmed timeframe. A timestamp without a timezone does not identify the broker clock.</p>
    <details><summary>Detected columns</summary>${statList(Object.entries(result.columns).map(([key, value]) => [readable(key), text(value)]))}</details>
    ${result.issues.length ? `<h3>Needs review</h3><ul>${result.issues.map((issue) => `<li>${escapeHtml(issue.detail)} (${issue.count})</li>`).join("")}</ul>` : '<p class="note">No issues reported by these file checks.</p>'}
    ${evidence({ sha256: result.source_sha256, bytes: result.bytes })}<p class="note">File inspected only. Nothing was imported into the engine and no backtest was run.</p>`;
}

function renderMt5History(state, busy) {
  const history = state.mt5History;
  return `<h3>Price history</h3><p class="note">Read ${escapeHtml(state.mt5Symbol)} from the selected terminal. The initial range is the last seven completed UTC days at H1. Availability depends on the history held by your terminal.</p>
    <div class="data-setup-pickers"><label>Timeframe<select data-mt5-timeframe ${busy ? "disabled" : ""}>${Object.keys(MT5_TIMEFRAMES).map((name) => `<option ${name === state.mt5Timeframe ? "selected" : ""}>${name}</option>`).join("")}</select></label>
    <label>From (UTC, inclusive)<input class="workflow-input" type="date" data-mt5-from value="${escapeHtml(state.mt5From)}" max="${new Date().toISOString().slice(0, 10)}" ${busy ? "disabled" : ""}></label>
    <label>To (UTC, exclusive)<input class="workflow-input" type="date" data-mt5-to value="${escapeHtml(state.mt5To)}" max="${new Date().toISOString().slice(0, 10)}" ${busy ? "disabled" : ""}></label></div>
    <button class="button" type="button" data-mt5-history ${busy ? "disabled" : ""}>Read price history</button>
    <p class="note">Up to 10,000 potential bars per read. Connecting may reopen the terminal if it closes. Nothing is imported into the engine.</p>
    <div role="status" aria-live="polite">${state.mt5HistoryReading ? '<p class="note">Reading price history…</p>' : ""}${state.mt5Exporting ? '<p class="note">Preparing captured CSV…</p>' : ""}${state.mt5HistoryError ? unavailable("Price history unavailable", state.mt5HistoryError, { tone: "error", compact: true }) : ""}</div>
    ${history ? `<h3>Captured history</h3>${statList([["Symbol / timeframe", `${history.request.symbol} / ${history.request.timeframe}`], ["Rows", history.row_count], ["First bar (UTC)", history.date_from], ["Last bar (UTC)", history.date_to], ["Bar timestamp", "Start of bar"], ["Clock-spacing gaps", history.gap_count], ["Coverage", "Not confirmed"]])}
    <p class="note">Gaps describe clock spacing, not confirmed missing bars. Trading breaks and terminal history limits may affect coverage.</p>
    <p class="note">Still unresolved: ${history.unresolved.map((key) => escapeHtml(readable(key))).join(", ")}. No backtest setup has been applied or verified.</p>
    ${evidence({ request: history.request, source: history.source, history_ref: history.history_ref, csv_ref: history.csv_ref, sha256: history.source_sha256, bytes: history.bytes })}
    <button class="button" type="button" data-mt5-export ${busy ? "disabled" : ""}>Download captured CSV</button>` : ""}`;
}

function renderMt5(state) {
  const terminals = state.mt5Catalog?.terminals || [];
  const busy = state.mt5Loading || state.mt5Reading || state.mt5HistoryReading || state.mt5Exporting;
  const metadata = state.mt5Metadata;
  const symbol = metadata?.symbols.find((row) => row.name === state.mt5Symbol);
  const selected = terminals.some((row) => row.terminal_id === state.mt5TerminalId);
  const primaryFields = [["trade_tick_size", "Tick size"], ["trade_tick_value", "Tick value"], ["point", "Point"], ["trade_contract_size", "Contract size"], ["currency_base", "Base currency"], ["currency_profit", "Profit currency"], ["currency_margin", "Margin currency"], ["spread", "Spread (points)"], ["spread_float", "Floating spread"]];
  return `<section class="card span-2" data-mt5-panel><div class="card-head"><h2 class="card-title">MetaTrader 5 broker settings</h2><button type="button" class="button" data-mt5-refresh ${busy ? "disabled" : ""}>Refresh terminals</button></div><div class="card-body">
    <p class="note">Use an open MetaTrader 5 terminal signed in to your broker. Choose it below, then read the broker’s reported settings.</p>
    <div role="status" aria-live="polite">${state.mt5Loading ? '<p class="note">Looking for open MT5 terminals…</p>' : ""}${state.mt5Reading ? '<p class="note">Reading broker settings…</p>' : ""}${state.mt5Error ? unavailable("MT5 settings unavailable", state.mt5Error, { tone: "error", compact: true }) : ""}</div>
    ${state.mt5Catalog?.status === "no_running_terminal" ? '<p class="note">No open MT5 terminal found. Open MetaTrader 5 and sign in to your broker, then Refresh terminals.</p>' : state.mt5Catalog?.status === "unavailable" ? unavailable("Terminal discovery unavailable", state.mt5Catalog.detail, { compact: true }) : ""}
    ${terminals.length ? `<div class="data-setup-pickers"><label>Open terminal<select data-mt5-terminal ${busy ? "disabled" : ""}><option value="">Choose an open terminal</option>${terminals.map((terminal) => `<option value="${escapeHtml(terminal.terminal_id)}" ${terminal.terminal_id === state.mt5TerminalId ? "selected" : ""}>${escapeHtml(terminal.label)} (${escapeHtml(terminal.terminal_id)})</option>`).join("")}</select></label></div>
    <label class="data-setup-pickers">Symbol search (optional)<input type="text" data-mt5-filter value="${escapeHtml(state.mt5SymbolFilter || "")}" placeholder="Example: EURUSD" maxlength="64" ${busy ? "disabled" : ""}></label>
    <p class="note">Narrow the symbols returned by your broker. Leave blank to request all symbols; large catalogs may need a search.</p>
    <button type="button" class="button button-primary" data-mt5-read ${busy || !selected ? "disabled" : ""}>Read broker settings</button>
    <p class="note">Connecting may reopen the selected terminal if it closes during connection. This reads metadata only; it does not place orders or import price history.</p>` : ""}
    ${metadata ? `<div class="data-mt5-readout grid-2"><div><h3>Reported broker</h3>${statList([["Company", text(metadata.broker.company)], ["Server", text(metadata.broker.server)], ["Account currency", text(metadata.broker.currency)], ["Terminal company", text(metadata.terminal.company)], ["MT5 build", text(metadata.terminal.build)], ["Connected at read", metadata.terminal.connected ? "Yes" : "No"], ["Read at (UTC)", metadata.source.observed_at_utc], ["Symbol search used", metadata.source.symbol_filter || "All symbols"]])}${evidence(metadata.source)}<p class="note">Snapshot from this read. Use Read broker settings again to refresh it.</p>
    <h3>Still unresolved</h3><ul>${metadata.unresolved.map((key) => `<li>${escapeHtml(readable(key))}</li>`).join("")}</ul><p class="note">MT5 values have not been converted into SQX settings or applied to a backtest. Broker timezone is not inferred.</p></div>
    <div><h3>Reported instrument</h3><label>MT5 symbol<select data-mt5-symbol ${busy ? "disabled" : ""}><option value="">Choose a reported symbol</option>${metadata.symbols.map((row) => `<option value="${escapeHtml(row.name)}" ${row.name === state.mt5Symbol ? "selected" : ""}>${escapeHtml(row.name)}</option>`).join("")}</select></label>${!metadata.symbols.length ? '<p class="note">No symbols were returned by the terminal.</p>' : ""}
    ${symbol ? `${statList(primaryFields.map(([key, label]) => [label, text(symbol[key])]))}<p class="note">Raw values reported by MT5 for ${escapeHtml(symbol.name)}. Tick value is not an SQX point value.</p><details><summary>All reported symbol fields</summary>${statList([["name", symbol.name], ...MT5_SYMBOL_TEXT.map((key) => [key, text(symbol[key])]), ...MT5_SYMBOL_NUMBERS.map((key) => [key, text(symbol[key])]), ["spread_float", text(symbol.spread_float)]])}</details>${evidence(metadata.source)}` : ""}</div></div>` : ""}
    ${symbol ? renderMt5History(state, busy) : ""}</div></section>`;
}

export function renderDataSetup(state = {}) {
  const rows = state.catalog?.datasets || [];
  const brokers = [...new Map(rows.filter((row) => row.broker).map((row) => [String(row.broker.profile_id), row.broker.name])).entries()];
  const filtered = rows.filter((row) => !state.broker || (state.broker === "unassigned" ? !row.broker : String(row.broker?.profile_id) === state.broker));
  const busy = state.loading || state.selecting;
  return `<div class="data-setup grid-2">
    ${renderMt5(state)}
    <section class="card"><div class="card-head"><h2 class="card-title">Installed data</h2><button type="button" class="button" data-data-refresh ${busy ? "disabled" : ""}>Refresh</button></div><div class="card-body">
      <p class="note">Choose a broker profile and instrument already saved in the engine.</p>
      <div class="data-setup-pickers"><label>Broker profile<select class="workflow-input" data-data-broker ${busy ? "disabled" : ""}><option value="">All installed profiles</option>${brokers.map(([id, name]) => `<option value="${escapeHtml(id)}" ${state.broker === id ? "selected" : ""}>${escapeHtml(text(name))}</option>`).join("")}<option value="unassigned" ${state.broker === "unassigned" ? "selected" : ""}>No broker profile</option></select></label>
      <label>Instrument / dataset<select class="workflow-input" data-data-dataset ${busy ? "disabled" : ""}><option value="">Choose installed data</option>${filtered.map((row) => `<option value="${escapeHtml(row.dataset_id)}" ${state.datasetId === row.dataset_id ? "selected" : ""}>${escapeHtml(row.symbol)} · ${escapeHtml(text(row.timeframe))}</option>`).join("")}</select></label></div>
      <div role="status" aria-live="polite">${state.loading ? '<p class="note">Reading installed data…</p>' : state.selecting ? '<p class="note">Reading reference settings…</p>' : ""}${state.catalogError ? unavailable("Installed data unavailable", state.catalogError, { compact: true, tone: "error" }) : ""}${state.selectionError ? unavailable("Reference settings unavailable", state.selectionError, { compact: true, tone: "error" }) : ""}</div>
      ${state.catalog?.status === "unavailable" ? unavailable("Installed data unavailable", readable(state.catalog.reason_code), { compact: true }) : !state.loading && state.catalog && !rows.length ? '<p class="note">No installed datasets found.</p>' : ""}${renderSelection(state.selection)}
    </div></section>
    <section class="card"><div class="card-head"><h2 class="card-title">Inspect a price file</h2></div><div class="card-body">
      <p class="note">Check a CSV or text file before import. We detect columns, dates, and data issues. Files stay unchanged. Use an export from MetaTrader, TradingView, or your Python broker.</p>
      <label class="data-setup-file">Price file (up to 16 MiB)<input type="file" data-data-file accept=".csv,.tsv,.txt" ${state.inspecting ? "disabled" : ""}></label>
      <div role="status" aria-live="polite"><p class="note">${escapeHtml(state.fileName || "Choose a file to detect its format.")}${state.inspecting ? " — Inspecting…" : ""}</p>${state.fileError ? unavailable("File could not be inspected", state.fileError, { compact: true, tone: "error" }) : ""}</div>${renderInspection(state.inspection)}
      <details class="data-setup-connection"><summary>Direct MetaTrader 5 import</summary><p class="note">${state.catalog?.native_mt5_import?.native_component_present ? "The native MT5 import component is installed." : "The native MT5 import component has not been confirmed."} Direct import is not available in this app yet. No terminal is connected by choosing a file.</p><a href="https://strategyquant.com/doc/quantdatamanager/metatrader5-data-import/" target="_blank" rel="noopener noreferrer">Official MT5 import guide</a></details>
    </div></section></div>`;
}

export function bindDataSetup(root, fetchImpl = globalThis.fetch) {
  if (!root || root.dataset.dataSetupBound) return;
  root.dataset.dataSetupBound = "true";
  const today = new Date().toISOString().slice(0, 10);
  const state = { mt5Timeframe: "H1", mt5From: new Date(Date.parse(today) - 7 * 86400000).toISOString().slice(0, 10), mt5To: today };
  let catalogGeneration = 0, selectionGeneration = 0, fileGeneration = 0;
  let mt5Generation = 0;
  const current = () => root.isConnected;
  const render = () => { if (current()) root.innerHTML = renderDataSetup(state); };
  const mt5Busy = () => state.mt5Loading || state.mt5Reading || state.mt5HistoryReading || state.mt5Exporting;
  const clearHistory = () => { state.mt5History = null; state.mt5HistoryError = ""; };
  async function loadMt5() {
    clearHistory();
    const generation = ++mt5Generation;
    Object.assign(state, { mt5Loading: true, mt5Reading: false, mt5Catalog: null, mt5Metadata: null, mt5Error: "", mt5TerminalId: "", mt5Symbol: "" });
    render();
    try {
      const catalog = await fetchMt5Terminals(fetchImpl);
      if (!current() || generation !== mt5Generation) return;
      state.mt5Catalog = catalog;
    } catch (error) {
      if (!current() || generation !== mt5Generation) return;
      state.mt5Error = error.message;
    }
    if (!current() || generation !== mt5Generation) return;
    state.mt5Loading = false;
    render();
  }
  async function readMt5() {
    const terminal = state.mt5Catalog?.terminals.find((row) => row.terminal_id === state.mt5TerminalId);
    if (!terminal || mt5Busy()) return;
    clearHistory();
    const generation = ++mt5Generation;
    Object.assign(state, { mt5Reading: true, mt5Metadata: null, mt5Symbol: "", mt5Error: "" });
    render();
    try {
      const metadata = await readMt5Metadata(terminal.terminal_id, terminal.identity_sha256, fetchImpl, state.mt5SymbolFilter || "");
      if (!current() || generation !== mt5Generation) return;
      state.mt5Metadata = metadata;
    } catch (error) {
      if (!current() || generation !== mt5Generation) return;
      state.mt5Error = error.message;
    }
    if (!current() || generation !== mt5Generation) return;
    state.mt5Reading = false;
    render();
  }
  async function readHistory() {
    if (mt5Busy() || !state.mt5Metadata || !state.mt5Symbol) return;
    const generation = ++mt5Generation;
    clearHistory();
    state.mt5HistoryReading = true;
    render();
    try {
      const history = await readMt5History(state.mt5Metadata, state.mt5Symbol, state.mt5Timeframe, state.mt5From, state.mt5To, fetchImpl);
      if (!current() || generation !== mt5Generation) return;
      state.mt5History = history;
    } catch (error) {
      if (!current() || generation !== mt5Generation) return;
      state.mt5HistoryError = error.message;
    }
    if (!current() || generation !== mt5Generation) return;
    state.mt5HistoryReading = false;
    render();
  }
  async function downloadHistory() {
    if (mt5Busy() || !state.mt5History) return;
    const generation = ++mt5Generation;
    state.mt5Exporting = true;
    state.mt5HistoryError = "";
    render();
    try {
      const blob = await exportMt5History(state.mt5History, fetchImpl);
      if (!current() || generation !== mt5Generation) return;
      const url = URL.createObjectURL(blob);
      try {
        const link = root.ownerDocument.createElement("a");
        link.href = url;
        link.download = "MT5-history.csv";
        link.click();
      } finally { setTimeout(() => URL.revokeObjectURL(url), 0); }
    } catch (error) {
      if (!current() || generation !== mt5Generation) return;
      state.mt5HistoryError = error.message;
    }
    if (!current() || generation !== mt5Generation) return;
    state.mt5Exporting = false;
    render();
  }
  async function load() {
    const generation = ++catalogGeneration;
    ++selectionGeneration;
    Object.assign(state, { loading: true, selecting: false, selection: null, catalog: null, catalogError: "", selectionError: "", broker: "", datasetId: "" });
    render();
    try {
      const catalog = await fetchDataSetup(fetchImpl);
      if (!current() || generation !== catalogGeneration) return;
      state.catalog = catalog;
    } catch (error) {
      if (!current() || generation !== catalogGeneration) return;
      state.catalogError = error.message;
    }
    if (!current() || generation !== catalogGeneration) return;
    state.loading = false;
    render();
  }
  root.addEventListener("input", (event) => {
    if (event.target.matches?.("[data-mt5-filter]") && !mt5Busy()) state.mt5SymbolFilter = event.target.value;
  });
  root.addEventListener("click", (event) => {
    if (event.target.closest?.("[data-mt5-refresh]") && !mt5Busy()) return loadMt5();
    if (event.target.closest?.("[data-mt5-read]")) return readMt5();
    if (event.target.closest?.("[data-mt5-history]")) return readHistory();
    if (event.target.closest?.("[data-mt5-export]")) return downloadHistory();
    if (event.target.closest?.("[data-data-refresh]") && !state.loading && !state.selecting) void load();
  });
  root.addEventListener("change", async (event) => {
    const target = event.target;
    if (target.matches?.("[data-mt5-terminal]") && !mt5Busy()) {
      ++mt5Generation;
      clearHistory();
      Object.assign(state, { mt5TerminalId: target.value, mt5Metadata: null, mt5Symbol: "", mt5Error: "" });
      render();
      return;
    }
    if (target.matches?.("[data-mt5-symbol]") && state.mt5Metadata && !mt5Busy()) {
      ++mt5Generation;
      clearHistory();
      state.mt5Symbol = state.mt5Metadata.symbols.some((row) => row.name === target.value) ? target.value : "";
      render();
      return;
    }
    for (const [selector, key] of [["[data-mt5-timeframe]", "mt5Timeframe"], ["[data-mt5-from]", "mt5From"], ["[data-mt5-to]", "mt5To"]]) {
      if (target.matches?.(selector) && !mt5Busy()) {
        ++mt5Generation;
        clearHistory();
        state[key] = target.value;
        render();
        return;
      }
    }
    if (target.matches?.("[data-data-broker]") && !state.loading && !state.selecting) {
      ++selectionGeneration;
      Object.assign(state, { broker: target.value, datasetId: "", selection: null, selectionError: "" });
      render();
    } else if (target.matches?.("[data-data-dataset]") && !state.loading && !state.selecting) {
      const generation = ++selectionGeneration;
      Object.assign(state, { datasetId: target.value, selection: null, selectionError: "" });
      if (!state.datasetId) { render(); return; }
      state.selecting = true;
      render();
      try {
        const selection = await selectDataSetup(state.datasetId, state.catalog.source.snapshot_sha256, fetchImpl);
        if (!current() || generation !== selectionGeneration) return;
        state.selection = selection;
      } catch (error) {
        if (!current() || generation !== selectionGeneration) return;
        state.selectionError = error.message;
      }
      if (!current() || generation !== selectionGeneration) return;
      state.selecting = false;
      render();
    } else if (target.matches?.("[data-data-file]") && !state.inspecting) {
      const file = target.files?.[0];
      if (!file) return;
      const generation = ++fileGeneration;
      Object.assign(state, { inspecting: true, inspection: null, fileError: "", fileName: file.name });
      render();
      try {
        const inspection = await inspectDataFile(file, fetchImpl);
        if (!current() || generation !== fileGeneration) return;
        state.inspection = inspection;
      } catch (error) {
        if (!current() || generation !== fileGeneration) return;
        state.fileError = error.message;
      }
      if (!current() || generation !== fileGeneration) return;
      state.inspecting = false;
      render();
    }
  });
  void load();
  void loadMt5();
}
