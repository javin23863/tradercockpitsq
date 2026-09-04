import {
  actionButton,
  chartFrame,
  candleGeometry,
  escapeHtml,
  identityRows,
  table,
  unavailable,
} from "./ui.mjs";
import { barIndexForTime, overlayFills, tradeMarks } from "./research-chart-overlay.mjs";
import { workflowHref } from "./automation-settings-controls.mjs";
import {
  customProjectResultsFromPayload,
  fetchCustomProjectResults,
  projectResultsOf,
  renderProjectDatabankList,
  renderProjectDatabankStats,
} from "./custom-project-results.mjs";

export const STRATEGY_API_PATH = "/api/sqx-project-strategy";
export const RESULTS_PLUGIN_API_PATH = "/api/sqx-results-plugin";
export const RESULTS_PLUGIN_CREATE_API_PATH = "/api/sqx-results-plugins";
export const SOURCECODE_API_PATH = "/api/sqx-sourcecode";
export const OVERVIEW_API_PATH = "/api/sqx-overview";
export const RESULTS_CHART_API_PATH = "/api/sqx-results-chart";
const STRATEGY_SCHEMA = "tc.sqx-custom-project-strategy.v1";
const SQX_BUILD = "144.2953";
const CORE_TABS = Object.freeze([
  ["overview", "Overview"],
  ["sp-overview", "SP overview"],
  ["trades", "List of trades"],
  ["equity", "Equity chart"],
  ["trade-analysis", "Trade analysis"],
  ["profile", "Profile chart"],
  ["config", "Strategy config"],
  ["source", "Source Code"],
]);
const CHART_TAB = Object.freeze(["chart", "Trades on chart"]);
const OVERVIEW_TEMPLATES = Object.freeze([
  ["TSOverview", "TS Overview"],
  ["SQDefault", "SQ Default"],
  ["DefaultOpenDD", "Default with OpenDD"],
  ["SQDefaultPct", "SQ Default (% Monthly Performance)"],
  ["SQDefaultWithPortfolio", "SQ with Portfolio"],
  ["SQDefaultWithPortfolioPct", "SQ with Portfolio (% Monthly Performance)"],
]);
const SOURCE_FORMAT_TO_TYPE = Object.freeze({
  xml: "Strategy XML",
  pseudo: "Pseudo Code(*.TXT)",
  mq4: "Expert Advisor for MetaTrader4 (*.MQ4)",
  mq5: "Expert Advisor for MetaTrader5 (*.MQ5)",
  el: "EasyLanguage for Tradestation / MultiCharts (*.el)",
  pla: "EasyLanguage for MultiCharts (*.pla)",
  java: "Expert Advisor for JForex (*.java)",
});
const FILLED_TYPES = new Set([1, 2, 9, 11]);

function pluginTabs(strategy) {
  const rows = strategy?.results_plugins;
  if (Array.isArray(rows) && rows.length) {
    return rows.map((item) => [item.id, item.title || item.folder]);
  }
  return [];
}

function resultTabs(strategy) {
  return [...CORE_TABS, ...pluginTabs(strategy), CHART_TAB];
}

function knownResultView(view, strategy) {
  const current = view.resultView;
  return resultTabs(strategy).some(([id]) => id === current) ? current : "overview";
}

function pluginCreateFromPayload(value) {
  if (value == null) return { available: false, template: "CustomPlugin" };
  const record = object(value);
  if (!record || typeof record.available !== "boolean") {
    throw new Error("Native Results plugin create state is invalid");
  }
  return {
    available: record.available,
    template: typeof record.template === "string" ? record.template : "CustomPlugin",
  };
}

function object(value) {
  return value && typeof value === "object" && !Array.isArray(value) ? value : null;
}

function digest(value) {
  return typeof value === "string" && /^[0-9a-f]{64}$/.test(value) ? value : "";
}

function projectName(value) {
  return typeof value === "string"
    && value
    && value === value.trim()
    && !value.includes("/")
    && !value.includes("\\")
    && !value.includes("\0")
    && ![".", ".."].includes(value)
    ? value
    : "";
}

function validNumber(value) {
  return typeof value === "number" && Number.isFinite(value);
}

function statsSide(value) {
  if (value == null) return null;
  const record = object(value);
  if (!record || !Number.isInteger(record.NumberOfTrades) || !validNumber(record.NetProfit) || !validNumber(record.ProfitFactor)) {
    throw new Error("Native Custom Project strategy statistics are invalid");
  }
  return record;
}

function statsSample(value) {
  const record = object(value);
  if (!record) throw new Error("Native Custom Project strategy statistics are invalid");
  return {
    all: statsSide(record.all),
    long: statsSide(record.long),
    short: statsSide(record.short),
  };
}

function statisticsFromPayload(value) {
  if (value == null) return null;
  const record = object(value);
  if (!record || record.basis !== "sqx_column_formulas_over_orders.bin") {
    throw new Error("Native Custom Project strategy statistics are invalid");
  }
  return {
    basis: record.basis,
    full: statsSample(record.full),
    is: statsSample(record.is),
    oos: statsSample(record.oos),
  };
}

function chartBarsFromPayload(value) {
  if (value == null) return null;
  const record = object(value);
  if (!record || !["available", "unavailable"].includes(record.state)) {
    throw new Error("Native Custom Project strategy chart bars are invalid");
  }
  if (record.state === "available") {
    if (!Array.isArray(record.bars) || typeof record.timeframe !== "string") {
      throw new Error("Native Custom Project strategy chart bars are invalid");
    }
  }
  return record;
}

function resultsPluginsFromPayload(value) {
  if (value == null) return [];
  if (!Array.isArray(value)) throw new Error("Native Results plugin list is invalid");
  return value.filter((item) => object(item) && typeof item.id === "string" && typeof item.folder === "string");
}

function sourceFromPayload(value) {
  if (value == null) return null;
  const record = object(value);
  if (!record || !["available", "unavailable"].includes(record.state) || typeof record.language !== "string") {
    throw new Error("Native Custom Project strategy source is invalid");
  }
  return record;
}

function tradeFromPayload(value) {
  if (
    !value
    || typeof value !== "object"
    || !Number.isInteger(value.Ticket)
    || !Number.isInteger(value.Type)
    || !FILLED_TYPES.has(value.Type)
    || !validNumber(value.PL)
  ) {
    throw new Error("Native trade record is invalid");
  }
  return value;
}

export function projectStrategyFromPayload(payload) {
  const record = object(payload);
  if (
    !record
    || record.schema !== STRATEGY_SCHEMA
    || record.source_build !== SQX_BUILD
    || !projectName(record.project)
    || !projectName(record.databank)
    || typeof record.archive !== "string"
    || !record.archive.toLowerCase().endsWith(".sqx")
    || record.relative_path !== `user/projects/${record.project}/databanks/${record.databank}/${record.archive}`
    || !digest(record.archive_sha256)
    || typeof record.native_version !== "string"
    || !record.native_version
    || record.native_version.includes("\0")
    || !Array.isArray(record.archive_entries)
    || !object(record.orders)
    || !["available", "unavailable"].includes(record.orders.state)
    || !Array.isArray(record.equity)
    || !Array.isArray(record.settings)
    || !Array.isArray(record.config_diff)
    || !object(record.chart)
    || record.chart.stored !== true && record.chart.stored !== false
  ) {
    throw new Error("Native Custom Project strategy result is invalid");
  }
  record.statistics = statisticsFromPayload(record.statistics);
  record.source = sourceFromPayload(record.source);
  record.results_plugins = resultsPluginsFromPayload(record.results_plugins);
  record.results_plugin_create = pluginCreateFromPayload(record.results_plugin_create);
  record.chart.bars = chartBarsFromPayload(record.chart.bars);
  record.fitnesses = object(record.fitnesses) || {};
  record.result_name = typeof record.result_name === "string" ? record.result_name : "";
  record.result_key = typeof record.result_key === "string" ? record.result_key : "";
  record.symbols = Array.isArray(record.symbols) ? record.symbols.filter((row) => object(row) && typeof row.symbol === "string") : [];
  record.trade_analysis = object(record.trade_analysis);
  record.profile = Array.isArray(record.profile) ? record.profile.filter((point) => object(point) && validNumber(point.mae) && validNumber(point.mfe)) : [];
  if (record.orders.state === "available") {
    if (!object(record.orders.payload) || !Array.isArray(record.orders.payload.trades)) {
      throw new Error("Native Custom Project strategy trades are invalid");
    }
    record.orders.payload.trades = record.orders.payload.trades.map(tradeFromPayload);
  } else if (typeof record.orders.reason_code !== "string" || !record.orders.reason_code) {
    throw new Error("Native Custom Project strategy trades unavailable state is invalid");
  }
  return record;
}

async function readJson(response) {
  try { return await response.json(); } catch { return null; }
}

export async function fetchProjectStrategy(project, databank, archive, task, fetchImpl = globalThis.fetch, extra = {}) {
  if (typeof fetchImpl !== "function") throw new Error("Native strategy inspect fetch is unavailable");
  const exact = projectName(project);
  const bank = projectName(databank);
  const name = typeof archive === "string" && archive.toLowerCase().endsWith(".sqx") ? archive : "";
  if (!exact || !bank || !name) throw new Error("Exact native project, databank, and archive are required");
  const params = new URLSearchParams({ project: exact, databank: bank, archive: name });
  if (Number.isInteger(task)) params.set("task", String(task));
  if (Number.isInteger(extra?.focusTicket)) params.set("focusTicket", String(extra.focusTicket));
  const response = await fetchImpl(`${STRATEGY_API_PATH}?${params.toString()}`, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || `Native strategy inspect failed: ${response?.status ?? "unknown"}`);
  return projectStrategyFromPayload(payload);
}

export async function fetchSourceCatalog(fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(SOURCECODE_API_PATH, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || "Source Code catalog failed");
  if (!payload || payload.schema !== "tc.sqx-sourcecode-catalog.v1" || !Array.isArray(payload.generators)) {
    throw new Error("Source Code catalog is invalid");
  }
  return payload;
}

export async function printProjectSource(project, databank, archive, config, fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(SOURCECODE_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({
      project,
      databank,
      archive,
      type: config.type,
      mmType: config.mmType,
      parametrizeType: config.parametrizeType,
      useVariables: config.useVariables !== false,
      symmetricVariables: config.symmetricVariables !== false,
      periodParams: config.periodParams !== false,
      constantsParams: config.constantsParams === true,
      shiftParams: config.shiftParams === true,
      otherParams: config.otherParams === true,
      entryParams: config.entryParams === true,
      entryLogic: config.entryLogic === true,
      exitParamsUsed: config.exitParamsUsed !== false,
      exitParamsUnused: config.exitParamsUnused === true,
      booleanParams: config.booleanParams === true,
      recommendedParams: config.recommendedParams !== false,
    }),
  });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || "Source Code print failed");
  if (!payload || payload.schema !== "tc.sqx-sourcecode.v1" || typeof payload.code !== "string") {
    throw new Error("Source Code print is invalid");
  }
  return payload;
}

export async function fetchOverviewHtml(project, databank, archive, { template = "TSOverview", sample = "full", direction = "both" } = {}, fetchImpl = globalThis.fetch) {
  const params = new URLSearchParams({ project, databank, archive, template, sample, direction });
  const response = await fetchImpl(`${OVERVIEW_API_PATH}?${params.toString()}`, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || "Overview HTML failed");
  if (!payload || payload.schema !== "tc.sqx-overview.v1") throw new Error("Overview HTML is invalid");
  return payload;
}

export async function fetchResultsChart(project, databank, archive, { stock = "" } = {}, fetchImpl = globalThis.fetch) {
  const params = new URLSearchParams({ project, databank, archive });
  if (stock) params.set("stock", stock);
  const response = await fetchImpl(`${RESULTS_CHART_API_PATH}?${params.toString()}`, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || "Results chart failed");
  if (!payload || payload.schema !== "tc.sqx-results-chart.v1") throw new Error("Results chart is invalid");
  return payload;
}

export async function sourcecodeAction(action, body, fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(SOURCECODE_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({ action, ...body }),
  });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || `Source Code ${action} failed`);
  return payload;
}

export async function createResultsPluginTab(name, fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(RESULTS_PLUGIN_CREATE_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({ name }),
  });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || "Results plugin create failed");
  if (!payload || typeof payload.id !== "string" || typeof payload.folder !== "string") {
    throw new Error("Results plugin create is invalid");
  }
  return payload;
}

function numberText(value) {
  return typeof value === "number" && Number.isFinite(value) ? String(value) : "—";
}

function sampleTrades(trades, view) {
  const rows = Array.isArray(trades) ? trades : [];
  if (view.sample === "is") return rows.filter((trade) => Number.isInteger(trade.SampleType) && trade.SampleType >= 10 && trade.SampleType < 20);
  if (view.sample === "oos") return rows.filter((trade) => Number.isInteger(trade.SampleType) && trade.SampleType >= 20 && trade.SampleType <= 30);
  return rows;
}

function directionTrades(trades, view) {
  const rows = Array.isArray(trades) ? trades : [];
  if (view.direction === "long") return rows.filter((trade) => trade.Type === 1 || trade.Type === 9);
  if (view.direction === "short") return rows.filter((trade) => trade.Type === 2 || trade.Type === 11);
  return rows;
}

function renderTradesView(strategy, view = {}) {
  const orders = strategy.orders;
  if (orders.state !== "available") {
    return `<div data-results-trades data-results-trades-state="unavailable">${unavailable(
      "List of trades unavailable",
      orders.detail || "This archive has no inspectable orders.bin records.",
      { compact: true },
    )}</div>`;
  }
  const trades = directionTrades(sampleTrades(orders.payload.trades || [], view), view);
  if (!trades.length) {
    return `<div data-results-trades data-results-trades-state="empty">${unavailable(
      "No native portfolio trades",
      "The selected archive contains no filled portfolio trades for the current sample and direction filters.",
      { compact: true },
    )}</div>`;
  }
  return `<div data-results-trades data-results-trades-state="ready">
    <p class="field-help">${trades.length} native trade${trades.length === 1 ? "" : "s"} from orders.bin · ${escapeHtml(String(strategy.archive_sha256).slice(0, 12))}… · basis ${escapeHtml(strategy.statistics?.basis || "orders.bin")}</p>
    <div class="trade-table-wrap"><table class="trade-table" data-native-trades-table>
      <thead><tr><th>Ticket</th><th>Symbol</th><th>Native type</th><th>Sample</th><th>Size</th><th>Open time</th><th>Open</th><th>Close time</th><th>Close</th><th>P/L</th><th>Pips</th><th>Bars</th><th>Duration</th></tr></thead>
      <tbody>${trades.map((trade) => `<tr data-native-trade-ticket="${escapeHtml(trade.Ticket)}">
        <td><code>${escapeHtml(trade.Ticket)}</code></td>
        <td>${escapeHtml(trade.Symbol || "—")}</td>
        <td><code>${escapeHtml(trade.Type)}</code></td>
        <td><code>${escapeHtml(numberText(trade.SampleType))}</code></td>
        <td>${escapeHtml(numberText(trade.Size))}</td>
        <td><code>${escapeHtml(trade.OpenTime)}</code></td>
        <td>${escapeHtml(numberText(trade.OpenPrice))}</td>
        <td><code>${escapeHtml(trade.CloseTime)}</code></td>
        <td>${escapeHtml(numberText(trade.ClosePrice))}</td>
        <td>${escapeHtml(numberText(trade.PL))}</td>
        <td>${escapeHtml(numberText(trade.PipsPL))}</td>
        <td>${escapeHtml(numberText(trade.BarsInTrade))}</td>
        <td>${escapeHtml(numberText(trade.Duration))}</td>
      </tr>`).join("")}</tbody>
    </table></div>
  </div>`;
}

function renderEquityView(strategy) {
  const equity = strategy.equity || [];
  const values = equity.map((point) => point.balance).filter((value) => typeof value === "number" && Number.isFinite(value));
  if (strategy.orders?.state !== "available") {
    return `<div data-results-equity data-results-equity-state="unavailable" data-equity-basis="">${unavailable(
      "Equity chart unavailable",
      strategy.orders?.detail || "Equity is the running sum of producer-recorded trade P/L. orders.bin is unread.",
      { compact: true },
    )}</div>`;
  }
  const basis = strategy.equity_basis === "archive_initial_capital"
    ? `Starts at archive InitialCapital ${numberText(strategy.initial_capital)}`
    : "Cumulative native P/L; archive InitialCapital was not recorded";
  if (values.length < 2) {
    const point = equity[0];
    return `<div data-results-equity data-results-equity-state="${values.length ? "ready" : "empty"}" data-equity-basis="${escapeHtml(strategy.equity_basis || "")}">
      <p class="field-help">${escapeHtml(basis)}. ${values.length === 1 ? `One producer equity point at ${numberText(point?.balance)}.` : "One or more closed trades are required for an equity series."} This is not an invented Net Profit figure.</p>
      ${chartFrame({
        height: 120,
        state: values.length ? "historical" : "unavailable",
        detail: values.length ? "" : "Equity draws from the native trade records of this archive. No series yet.",
        legend: [["Equity (native trades)", "purple"]],
        yLabels: values.length ? [numberText(values[0]), "", numberText(values[0])] : ["", "", ""],
        xLabels: point?.time != null ? [String(point.time)] : [],
        series: [],
      })}
    </div>`;
  }
  const low = Math.min(...values);
  const high = Math.max(...values);
  const mid = (low + high) / 2;
  const first = equity[0]?.time != null ? String(equity[0].time) : "";
  const last = equity[equity.length - 1]?.time != null ? String(equity[equity.length - 1].time) : "";
  return `<div data-results-equity data-results-equity-state="ready" data-equity-basis="${escapeHtml(strategy.equity_basis || "")}">
    <p class="field-help">${escapeHtml(basis)}. This is not an invented Net Profit figure.</p>
    ${chartFrame({
      height: 180,
      state: "historical",
      detail: "",
      legend: [["Equity (native trades)", "purple"]],
      yLabels: [numberText(high), numberText(mid), numberText(low)],
      xLabels: [first, last],
      series: [{ values, tone: "purple" }],
    })}
  </div>`;
}

function pathLabel(path) {
  return (path || []).join(" / ");
}

function renderConfigView(strategy) {
  const diff = strategy.config_diff || [];
  const applyUpdates = diff.map((item) => {
    if (item.attribute) {
      return { path: item.path, attribute: item.attribute, value: item.archive_value };
    }
    return { path: item.path, text: item.archive_value };
  });
  const encoded = escapeHtml(JSON.stringify(applyUpdates));
  const rows = diff.map((item) => ({
    cells: [
      escapeHtml(pathLabel(item.path)),
      escapeHtml(item.attribute ? item.attribute : "text"),
      escapeHtml(item.task_value),
      escapeHtml(item.archive_value),
    ],
  }));
  const tableHtml = diff.length
    ? table({
      columns: [{ label: "Path" }, { label: "Field" }, { label: "Current task" }, { label: "Archive settings.xml" }],
      rows,
    })
    : unavailable(
      "Strategy config matches this task",
      "Archive settings.xml has no differing existing attributes or text versus the selected task XML. Extra archive-only elements are not copied.",
      { compact: true },
    );
  return `<div data-results-config>
    <p class="field-help">Compare the selected archive settings.xml with the current task XML. Apply writes only overlapping existing fields.</p>
    ${tableHtml}
    ${diff.length ? `<div class="idea-actions">${actionButton("Apply strategy config", { primary: true, attrs: `data-automation-apply-config data-config-updates="${encoded}"` })}</div>` : ""}
    <p class="idea-save-status" data-automation-settings-status></p>
  </div>`;
}

function storeChartSettingsHref(strategy) {
  return workflowHref({
    project: strategy.project,
    tab: "settings",
    task: strategy.task_index || "",
    section: "Options",
  });
}

function renderChartToolbar(strategy) {
  const stored = strategy.chart?.store_chart_data === true || strategy.chart?.stored === true;
  const settingsHref = storeChartSettingsHref(strategy);
  const hint = stored
    ? "This archive stored chart members. Indicator overlays come from StrategyQuant X resultsCharts/loadChartData, not a substitute series."
    : `Strategy doesn't have chart data stored. To see native chart indicators, check <a class="workflow-link" href="${escapeHtml(settingsHref)}" data-route="${escapeHtml(settingsHref)}" data-automation-section="Options">Store Chart Data</a> in Settings — Strategy options and repeat the backtest.`;
  return `<div class="sqx-chart-toolbar" data-chart-toolbar>
    <div class="sqx-chart-indicators">
      <button type="button" class="button button-small" data-chart-indicators-toggle disabled aria-expanded="false" aria-haspopup="true" title="Indicators come from StrategyQuant X resultsCharts/loadChartData">Show indicators</button>
      <ul class="sqx-chart-indicator-list" hidden data-chart-indicator-list></ul>
    </div>
    <button type="button" class="button button-small is-active" data-chart-grid>Grid</button>
    <span class="sqx-chart-zoom">Zoom
      <button type="button" class="button button-small" data-chart-zoom="-">-</button>
      <button type="button" class="button button-small" data-chart-zoom="reset">reset</button>
      <button type="button" class="button button-small" data-chart-zoom="+">+</button>
    </span>
    <span>Show/hide
      <button type="button" class="button button-small is-active" data-chart-toggle="price">Price</button>
      <button type="button" class="button button-small is-active" data-chart-toggle="ticket">Ticket</button>
      <button type="button" class="button button-small is-active" data-chart-toggle="pl">Profit/Loss</button>
    </span>
    <button type="button" class="button button-small" data-chart-trade="prev">&lt; Previous trade</button>
    <button type="button" class="button button-small" data-chart-trade="next">Next trade &gt;</button>
  </div>
  <p class="field-help" data-chart-store-hint>${hint}</p>`;
}

function visibleChartBars(bars, ticket, zoom, trades) {
  const rows = Array.isArray(bars?.bars) ? bars.bars : [];
  if (zoom <= 1 || rows.length < 3) return bars;
  const count = Math.max(20, Math.round(rows.length / zoom));
  if (count >= rows.length) return bars;
  const trade = (trades || []).find((item) => String(item.Ticket) === String(ticket));
  let idx = Math.floor(rows.length / 2);
  if (trade) {
    const closeAt = barIndexForTime(rows, bars.timeframe, trade.CloseTime);
    const openAt = barIndexForTime(rows, bars.timeframe, trade.OpenTime);
    idx = closeAt >= 0 ? closeAt : (openAt >= 0 ? openAt : idx);
  }
  const start = Math.max(0, Math.min(idx - Math.floor(count / 2), rows.length - count));
  return { ...bars, bars: rows.slice(start, start + count) };
}

function renderChartPlot(strategy, bars, { legendName = "Sidecar OHLC" } = {}) {
  const trades = strategy.orders?.state === "available" ? strategy.orders.payload.trades || [] : [];
  const mapped = overlayFills(bars.bars, trades, { symbol: bars.symbol || "", timeframe: bars.timeframe || "" });
  const extra = mapped.fills.map((fill) => fill.price);
  const geometry = candleGeometry(bars.bars, extra);
  const marks = geometry
    ? tradeMarks(mapped.fills, { min: geometry.min, span: geometry.span, slot: geometry.slot, height: geometry.height })
    : "";
  const first = bars.bars[0]?.open_time || "";
  const last = bars.bars[bars.bars.length - 1]?.open_time || "";
  return `<p class="field-help">${escapeHtml(bars.detail || "")} ${mapped.fills.length} of ${mapped.nativeFillCount} native fills land on these bars. Unmapped fills are omitted.</p>
      ${chartFrame({
        height: 280,
        state: "historical",
        detail: "",
        legend: [[legendName, "purple"], ["Native fills", "green"]],
        yLabels: geometry ? [numberText(geometry.max), numberText((geometry.max + geometry.min) / 2), numberText(geometry.min)] : [],
        xLabels: [String(first), String(last)],
        candles: bars.bars,
        extraPrices: extra,
        tradeMarksSvg: marks,
      })}`;
}

function renderChartView(strategy) {
  const bars = strategy.chart?.bars;
  const toolbar = renderChartToolbar(strategy);
  if (bars?.state === "available" && Array.isArray(bars.bars) && bars.bars.length) {
    return `<div data-results-chart="sidecar" data-chart-basis="${escapeHtml(bars.basis || "")}" data-chart-show-price="1" data-chart-show-ticket="1" data-chart-show-pl="1">
      ${toolbar}
      <div data-chart-body>${renderChartPlot(strategy, bars)}</div>
    </div>`;
  }
  if (strategy.chart?.stored === true) {
    return `<div data-results-chart="stored" data-chart-show-price="1" data-chart-show-ticket="1" data-chart-show-pl="1">
      ${toolbar}
      <div data-chart-body><p class="field-help">This archive stored chart members: ${escapeHtml((strategy.chart.entries || []).join(", "))}. A candle overlay is not synthesized from trades.</p></div>
    </div>`;
  }
  return `<div data-results-chart="unavailable" data-chart-show-price="1" data-chart-show-ticket="1" data-chart-show-pl="1">
    ${toolbar}
    <div data-chart-body>${unavailable(
    "Trades on chart unavailable",
    strategy.chart?.detail || "This archive did not store chart data.",
    { compact: true },
  )}</div>
  </div>`;
}

function renderResultTabs(topology, view, strategy = null) {
  const currentView = knownResultView(view, strategy);
  return `<div class="settings-nested-tabs" role="tablist">${resultTabs(strategy).map(([id, label]) => {
    const href = workflowHref({
      project: topology.project,
      tab: "results",
      task: view.task,
      databank: view.databank,
      archive: view.archive,
      resultView: id,
      sample: view.sample === "is" || view.sample === "oos" ? view.sample : "",
      direction: view.direction === "long" || view.direction === "short" ? view.direction : "",
    });
    const current = id === currentView;
    return `<a class="workflow-tab ${current ? "is-current" : ""}" role="tab" aria-selected="${current}" href="${escapeHtml(href)}" data-automation-result-view="${id}">${escapeHtml(label)}</a>`;
  }).join("")}</div>`;
}

function moneyText(value) {
  if (!validNumber(value)) return "—";
  return value.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function pctText(value) {
  return validNumber(value) ? `${numberText(value)} %` : "—";
}

function statsTone(value) {
  if (!validNumber(value)) return "";
  if (value > 0) return "tone-text-green";
  if (value < 0) return "tone-text-red";
  return "";
}

function sampleKey(view) {
  return view.sample === "is" || view.sample === "oos" ? view.sample : "full";
}

function directionKey(view) {
  return view.direction === "long" || view.direction === "short" ? view.direction : "both";
}

function sampleStats(strategy, view) {
  return strategy?.statistics?.[sampleKey(view)] || null;
}

function statsCell(record, field, { money = false, pct = false } = {}) {
  if (!record) return "—";
  const value = record[field];
  const text = money ? moneyText(value) : pct ? pctText(value) : numberText(value);
  const tone = field === "NetProfit" || field === "GrossProfit" || field === "Expectancy" || field === "final_equity" ? statsTone(value) : "";
  return tone ? `<span class="${tone}">${escapeHtml(text)}</span>` : escapeHtml(text);
}

function renderUnavailableResult(label) {
  return unavailable(
    `${label} unavailable`,
    "This Results pane has no producer read model yet. List of trades, equity, and strategy config come from orders.bin / settings.xml.",
    { compact: true },
  );
}

function databankRecordCount(results, project, databank) {
  const item = projectResultsOf(results, project);
  const bank = item?.databanks?.find((row) => row.name === databank);
  return Number.isInteger(bank?.strategy_count) ? bank.strategy_count : null;
}

function resultViewLabel(view, strategy) {
  const current = knownResultView(view, strategy);
  return resultTabs(strategy).find(([id]) => id === current)?.[1] || "Overview";
}

function producerPassFail(strategy) {
  const columns = strategy?.columns && typeof strategy.columns === "object" ? strategy.columns : null;
  const failed = strategy?.failed ?? columns?.Failed ?? columns?.FAILED ?? null;
  const passed = strategy?.passed ?? columns?.Passed ?? columns?.PASSED ?? null;
  return { failed, passed };
}

function renderDatabankToolbar(topology, view, results, strategy) {
  const item = projectResultsOf(results, topology.project);
  const databank = view.databank || item?.databanks?.[0]?.name || "";
  const records = databank ? databankRecordCount(results, topology.project, databank) : null;
  const viewLabel = resultViewLabel(view, strategy);
  const { failed, passed } = producerPassFail(strategy);
  const countText = records === null ? "—" : String(records);
  const failedMeta = failed == null ? "" : `<span data-results-failed>${escapeHtml(String(failed))}</span>`;
  const passedMeta = passed == null ? "" : `<span data-results-passed>${escapeHtml(String(passed))}</span>`;
  const archiveLabel = strategy?.archive ? `<span>${escapeHtml(strategy.archive)}</span>` : "";
  return `<div class="sqx-databank-toolbar" data-results-databank-toolbar>
    <div class="sqx-databank-toolbar-meta">
      <span>Records: ${escapeHtml(countText)}</span>
      <span>View: ${escapeHtml(viewLabel)}</span>
      ${archiveLabel}
      ${failedMeta ? `<span>FAILED: ${failedMeta}</span>` : ""}
      ${passedMeta ? `<span>PASSED: ${passedMeta}</span>` : ""}
    </div>
  </div>`;
}

function renderResultsToolbar(topology, view, strategy) {
  const sample = sampleKey(view);
  const direction = directionKey(view);
  const hasStats = Boolean(strategy?.statistics);
  const hrefFor = (nextSample, nextDirection = direction) => workflowHref({
    project: topology.project,
    tab: "results",
    task: view.task,
    databank: view.databank,
    archive: view.archive,
    resultView: view.resultView,
    sample: nextSample === "full" ? "" : nextSample,
    direction: nextDirection === "both" ? "" : nextDirection,
  });
  const sampleSelect = hasStats
    ? `<select data-results-sample>
        <option value="full" ${sample === "full" ? "selected" : ""} data-route="${escapeHtml(hrefFor("full"))}">Full sample</option>
        <option value="is" ${sample === "is" ? "selected" : ""} data-route="${escapeHtml(hrefFor("is"))}">In-sample</option>
        <option value="oos" ${sample === "oos" ? "selected" : ""} data-route="${escapeHtml(hrefFor("oos"))}">Out-of-sample</option>
      </select>`
    : `<select disabled title="Sample filter needs inspectable orders.bin"><option value="">—</option></select>`;
  const directionSelect = hasStats
    ? `<select data-results-direction>
        <option value="both" ${direction === "both" ? "selected" : ""} data-route="${escapeHtml(hrefFor(sample, "both"))}">Both</option>
        <option value="long" ${direction === "long" ? "selected" : ""} data-route="${escapeHtml(hrefFor(sample, "long"))}">Long</option>
        <option value="short" ${direction === "short" ? "selected" : ""} data-route="${escapeHtml(hrefFor(sample, "short"))}">Short</option>
      </select>`
    : `<select disabled title="Direction filter needs inspectable orders.bin"><option value="both">Both</option></select>`;
  const canCreate = Boolean(strategy?.results_plugin_create?.available);
  const templateOptions = OVERVIEW_TEMPLATES.map(([id, label]) => (
    `<option value="${escapeHtml(id)}" ${id === "TSOverview" ? "selected" : ""}>${escapeHtml(label)}</option>`
  )).join("");
  return `<div class="sqx-results-toolbar" data-results-toolbar>
    <label>Direction ${directionSelect}</label>
    <label>Sample ${sampleSelect}</label>
    <label>Template <select ${hasStats ? "" : "disabled"} data-results-template title="TS Overview columns from native trades until StrategyQuant X returns overview/getOverviewContent.">${templateOptions}</select></label>
    <button type="button" class="button button-small" data-results-new-analysis ${canCreate ? "" : "disabled"} title="Copy user/extend/ResultsPlugins/CustomPlugin into a new Results tab">${escapeHtml("+ New analysis")}</button>
    <dialog class="sqx-results-dialog" data-results-new-analysis-modal>
      <form method="dialog" data-results-new-analysis-form>
        <h3>Create new Result analysis plugin</h3>
        <p>Adds a new custom plugin analytics tab to Results. It copies the native CustomPlugin example into user/extend/ResultsPlugins.</p>
        <label>Name of custom plugin tab (must be unique)
          <input type="text" name="name" maxlength="80" placeholder="My Analysis" required>
        </label>
        <p class="field-help" data-results-new-analysis-status></p>
        <div class="sqx-results-dialog-actions">
          <button type="button" class="button button-small" data-results-new-analysis-close>Close</button>
          <button type="submit" value="create" class="button button-small">Create</button>
        </div>
      </form>
    </dialog>
  </div>`;
}

function overviewRows(sides) {
  return [
    ["Total Net Profit", "NetProfit", { money: true }],
    ["Gross Profit", "GrossProfit", { money: true }],
    ["Gross Loss", "GrossLoss", { money: true }],
    ["Profit Factor", "ProfitFactor", {}],
    ["Total Number of trades", "NumberOfTrades", {}],
    ["Percent Profitable", "WinningPct", { pct: true }],
    ["Winning Trades", "NumberOfProfits", {}],
    ["Losing Trades", "NumberOfLosses", {}],
    ["Avg. Trade Net Profit", "Expectancy", { money: true }],
    ["Drawdown", "Drawdown", { money: true }],
    ["% Drawdown", "DrawdownPct", { pct: true }],
    ["Return / DD Ratio", "ReturnDDRatio", {}],
  ].map(([label, field, opts]) => ({
    cells: [
      escapeHtml(label),
      statsCell(sides?.all, field, opts),
      statsCell(sides?.long, field, opts),
      statsCell(sides?.short, field, opts),
    ],
  }));
}

function renderOverviewView(strategy, view = {}) {
  if (!strategy) {
    return `<div class="sqx-results-empty" data-results-overview data-results-overview-state="empty" data-results-empty="true">
      <p>No result chosen - Double-click on result on databank to see the details</p>
      <p>No strategy selected</p>
    </div>`;
  }
  const sides = sampleStats(strategy, view);
  const fitness = validNumber(strategy.fitnesses?.IS) ? numberText(strategy.fitnesses.IS) : "—";
  const statsTable = sides
    ? table({
      columns: [{ label: "TS Overview" }, { label: "All", align: "right" }, { label: "Long", align: "right" }, { label: "Short", align: "right" }],
      rows: overviewRows(sides),
      className: "sqx-overview-table",
      attrs: 'data-results-overview-stats="1"',
    })
    : unavailable(
      "Overview unavailable",
      strategy.orders?.state === "available"
        ? "Statistics could not be computed from producer orders.bin for this archive."
        : (strategy.orders?.detail || "orders.bin is unread for this archive."),
      { compact: true },
    );
  const overviewState = sides ? "ready" : "unavailable";
  const identity = identityRows([
    ["Result", strategy.result_name || strategy.archive],
    ["Result key", strategy.result_key || "—"],
    ["Databank", strategy.databank],
    ["Fitness IS", fitness],
    ["Native version", strategy.native_version],
    ["Archive SHA-256", `${String(strategy.archive_sha256).slice(0, 12)}…`],
    ["Stats basis", strategy.statistics?.basis || "orders unread"],
  ]);
  return `<div data-results-overview data-results-overview-state="${overviewState}">
    <p class="field-help" data-overview-status>TS Overview columns from native trades. Other templates load StrategyQuant X overview/getOverviewContent.</p>
    <iframe class="sqx-overview-frame" hidden sandbox data-overview-frame title="StrategyQuant X overview"></iframe>
    ${statsTable}${identity}
  </div>`;
}

function renderSpOverviewView(strategy, view = {}) {
  const rows = (strategy?.symbols || []).map((row) => ({
    cells: [
      escapeHtml(row.symbol),
      escapeHtml(numberText(row.NumberOfTrades)),
      statsCell(row, "NetProfit", { money: true }),
      escapeHtml(numberText(row.ProfitFactor)),
      escapeHtml(pctText(row.WinningPct)),
    ],
  }));
  if (!rows.length) {
    return unavailable("SP overview unavailable", "No per-symbol native trades in this archive.", { compact: true });
  }
  return `<div data-results-sp-overview>
    <p class="field-help">One row per orders.bin Symbol. Numbers use the same SQX column formulas as Overview.</p>
    ${table({
      columns: [{ label: "Symbol" }, { label: "Trades", align: "right" }, { label: "Net Profit", align: "right" }, { label: "Profit Factor", align: "right" }, { label: "Win %", align: "right" }],
      rows,
    })}
  </div>`;
}

function renderTradeAnalysisView(strategy) {
  const years = strategy?.trade_analysis?.years;
  if (!Array.isArray(years) || !years.length) {
    return unavailable("Trade analysis unavailable", "Yearly net profit is summed from producer-recorded trade P/L.", { compact: true });
  }
  const values = years.map((row) => row.net_profit).filter((value) => validNumber(value));
  return `<div data-results-trade-analysis>
    <p class="field-help">Period by Close Time. MAE avg ${escapeHtml(moneyText(strategy.trade_analysis.mae_avg))} · MFE avg ${escapeHtml(moneyText(strategy.trade_analysis.mfe_avg))}.</p>
    ${chartFrame({
      height: 140,
      state: values.length > 1 ? "historical" : "unavailable",
      detail: values.length > 1 ? "" : "Need two yearly points for a series.",
      legend: [["Yearly net profit", "purple"]],
      yLabels: values.length ? [moneyText(Math.max(...values)), "", moneyText(Math.min(...values))] : ["", "", ""],
      xLabels: years.length ? [years[0].period, years[years.length - 1].period] : [],
      series: values.length > 1 ? [{ values, tone: "purple" }] : [],
    })}
    ${table({
      columns: [{ label: "Year" }, { label: "Net Profit", align: "right" }],
      rows: years.map((row) => ({
        cells: [
          escapeHtml(row.period),
          `<span class="${statsTone(row.net_profit)}">${escapeHtml(moneyText(row.net_profit))}</span>`,
        ],
      })),
    })}
  </div>`;
}

function renderProfileView(strategy) {
  const points = strategy?.profile || [];
  if (points.length < 2) {
    return unavailable("Profile chart unavailable", "MAE / MFE points come from producer-recorded trades.", { compact: true });
  }
  const mae = points.map((point) => point.mae);
  const mfe = points.map((point) => point.mfe);
  const maxMae = Math.max(...mae);
  const maxMfe = Math.max(...mfe);
  const spanMae = maxMae || 1;
  const spanMfe = maxMfe || 1;
  const dots = points.map((point) => {
    const x = (point.mae / spanMae) * 100;
    const y = 100 - (point.mfe / spanMfe) * 100;
    const tone = point.pl >= 0 ? "#3dd68c" : "#f07178";
    return `<circle cx="${x.toFixed(2)}" cy="${y.toFixed(2)}" r="0.9" fill="${tone}" />`;
  }).join("");
  return `<div data-results-profile>
    <p class="field-help">${points.length} MAE/MFE points from orders.bin (sampled if over 400). Green = winning trade.</p>
    <svg class="sqx-profile-chart" viewBox="0 0 100 100" preserveAspectRatio="none" aria-label="MAE versus MFE">${dots}</svg>
    <p class="note">X = MAE · Y = MFE</p>
  </div>`;
}

function renderSourceView(strategy) {
  const source = strategy?.source;
  const text = source?.state === "available" && source.text ? source.text : "";
  const warning = source?.state === "available"
    ? (source.detail || "")
    : (source?.detail || "This archive has no readable strategy_Portfolio.xml.");
  return `<div data-results-source>
    <div class="sqx-source-toolbar" data-source-toolbar>
      <label>Source code type
        <select data-source-type>
          <option value="Strategy XML" selected>Strategy XML</option>
        </select>
      </label>
      <button type="button" class="button button-small" data-source-save>Save to file</button>
      <button type="button" class="button button-small" data-source-copy>Copy to clipboard</button>
      <button type="button" class="button button-small" data-source-refresh title="Refresh">Refresh</button>
      <span class="field-help" hidden data-source-copied>Copied to clipboard</span>
      <span data-source-ea hidden>
        <button type="button" class="button button-small" data-source-save-ea="mt4">Save as EA (MT4)</button>
        <button type="button" class="button button-small" data-source-save-ea="mt5">Save as EA (MT5)</button>
        <button type="button" class="button button-small" data-source-configure>Configure</button>
      </span>
      <details class="sqx-source-params">
        <summary>Parameter variables</summary>
        <label><input type="radio" name="parametrizeType" value="0" checked data-source-parametrize> Recommended parameters</label>
        <label><input type="radio" name="parametrizeType" value="1" data-source-parametrize> Your own settings</label>
        <div class="sqx-source-own" data-source-own hidden>
          <label><input type="checkbox" data-source-flag="periodParams" checked> Periods</label>
          <label><input type="checkbox" data-source-flag="constantsParams"> Constants</label>
          <label><input type="checkbox" data-source-flag="shiftParams"> Shifts</label>
          <label><input type="checkbox" data-source-flag="otherParams"> Other params</label>
          <label><input type="checkbox" data-source-flag="entryParams"> Entry (levels)</label>
          <label><input type="checkbox" data-source-flag="entryLogic"> Entry (logic)</label>
          <label><input type="checkbox" data-source-flag="exitParamsUsed" checked> Exit params (SL, PT,...) only used</label>
          <label><input type="checkbox" data-source-flag="exitParamsUnused"> Exit params (SL, PT,...) unused</label>
          <label><input type="checkbox" data-source-flag="booleanParams"> Boolean params</label>
          <label><input type="checkbox" data-source-flag="symmetricVariables" checked> Symmetric variables for Long / Short</label>
        </div>
        <label><input type="radio" name="parametrizeType" value="2" data-source-parametrize> Don't use parameters</label>
      </details>
      <label>MM used
        <select data-source-mm>
          <option value="fromStrategy" selected>From strategy</option>
        </select>
      </label>
    </div>
    <p class="field-help" data-source-status>${escapeHtml(source?.language || "Strategy XML")} from ${escapeHtml(source?.member || "strategy_Portfolio.xml")}. ${escapeHtml(warning)}</p>
    <pre class="sqx-source" data-source-text>${escapeHtml(text || "No strategy selected.")}</pre>
    <dialog class="sqx-results-dialog" data-source-mt-modal>
      <form method="dialog" data-source-mt-form>
        <h3>Choose path to your MetaTrader installation</h3>
        <p>StrategyQuant X writes the EA into the Experts data folder. TraderCockpit only forwards these paths to sourcecode/saveMTPaths.</p>
        <label>Path to MetaTrader4 <input type="text" name="mt4InstallPath" data-mt-path="mt4InstallPath"></label>
        <label>MetaTrader4 Experts Data Folder <input type="text" name="mt4DataPath" data-mt-path="mt4DataPath"></label>
        <label>Path to MetaTrader5 <input type="text" name="mt5InstallPath" data-mt-path="mt5InstallPath"></label>
        <label>MetaTrader5 Experts Data Folder <input type="text" name="mt5DataPath" data-mt-path="mt5DataPath"></label>
        <p class="field-help" data-source-mt-status></p>
        <div class="sqx-results-dialog-actions">
          <button type="button" class="button button-small" data-source-mt-close>Close</button>
          <button type="submit" value="save" class="button button-small">Save</button>
        </div>
      </form>
    </dialog>
  </div>`;
}

function pluginTab(strategy, tabId) {
  return (strategy?.results_plugins || []).find((item) => item.id === tabId) || null;
}

function renderPluginView(strategy, plugin, viewLabel) {
  const label = plugin.title || plugin.folder || viewLabel || "Results plugin";
  if (!plugin?.installed || !plugin.folder) {
    return `<div data-results-plugin-host="${escapeHtml(plugin.id || "")}">${unavailable(
      `${label} unavailable`,
      plugin?.folder
        ? `Native plugin ${plugin.folder} is not installed under user/extend/ResultsPlugins.`
        : "This StrategyQuant X runtime has no matching Results plugin folder.",
      { compact: true },
    )}</div>`;
  }
  const src = `${RESULTS_PLUGIN_API_PATH}/${encodeURIComponent(plugin.folder)}/index.html`;
  return `<div data-results-plugin-host="${escapeHtml(plugin.id || plugin.folder)}">
    <p class="field-help">Native SQX Results plugin ${escapeHtml(plugin.folder)}. GET_STATS / GET_ORDERS / GET_SOURCE_CODE are this archive's producer records, not a substitute engine.</p>
    <iframe class="sqx-plugin-frame" title="${escapeHtml(plugin.title || plugin.folder)}" data-results-plugin="${escapeHtml(plugin.folder)}" src="${escapeHtml(src)}"></iframe>
  </div>`;
}

function pluginStats(strategy, params = {}) {
  const sampleType = String(params.sampleType || "127");
  const sample = sampleType === "10" || (Number(sampleType) >= 10 && Number(sampleType) < 20) ? "is"
    : sampleType === "20" || (Number(sampleType) >= 20 && Number(sampleType) <= 30) ? "oos"
      : "full";
  const direction = String(params.direction || "0");
  const side = direction === "1" ? "long" : direction === "-1" ? "short" : "all";
  return strategy?.statistics?.[sample]?.[side] || strategy?.statistics?.full?.all || null;
}

function pluginOrders(strategy, params = {}) {
  const trades = strategy?.orders?.state === "available" ? strategy.orders.payload.trades || [] : [];
  return directionTrades(
    sampleTrades(trades, {
      sample: String(params.sampleType) === "10" || (Number(params.sampleType) >= 10 && Number(params.sampleType) < 20)
        ? "is"
        : String(params.sampleType) === "20" || (Number(params.sampleType) >= 20 && Number(params.sampleType) <= 30)
          ? "oos"
          : "full",
      direction: String(params.direction) === "1" ? "long" : String(params.direction) === "-1" ? "short" : "both",
    }),
    {
      direction: String(params.direction) === "1" ? "long" : String(params.direction) === "-1" ? "short" : "both",
    },
  ).map((trade) => ({
    Ticket: trade.Ticket,
    Symbol: trade.Symbol,
    ResultName: trade.SetupName,
    NumberOfTrade: trade.Order,
    OpenTime: trade.OpenTime,
    CloseTime: trade.CloseTime,
    Type: trade.Type,
    CloseType: trade.CloseType,
    SampleType: trade.SampleType,
    OpenPrice: trade.OpenPrice,
    ClosePrice: trade.ClosePrice,
    Size: trade.Size,
    BarsInTrade: trade.BarsInTrade,
    TimeInTrade: trade.Duration,
    ProfitLoss: trade.PL,
    ProfitLossPct: trade.PctPL,
    ProfitLossPips: trade.PipsPL,
    Drawdown: trade.DD,
    PctDrawdown: trade.PctDD,
    MAE: trade.MAE,
    MFE: trade.MFE,
    Balance: trade.AccountBalance,
    CommSwap: trade.CommSwap,
    SlippageInMoney: trade.SlippageInMoney,
    Comment: trade.Comment,
    SLLevel: trade.StopLoss,
    PTLevel: trade.TakeProfit,
  }));
}

let pluginBridge = null;
let sourceBridge = null;
let overviewBridge = null;
let chartBridge = null;

function sourceConfigFrom(root) {
  const parametrize = Number(root.querySelector("[data-source-parametrize]:checked")?.value || 0);
  const flags = {};
  root.querySelectorAll("[data-source-flag]").forEach((input) => {
    flags[input.getAttribute("data-source-flag")] = input.checked;
  });
  return {
    type: root.querySelector("[data-source-type]")?.value || "Strategy XML",
    mmType: root.querySelector("[data-source-mm]")?.value || "fromStrategy",
    parametrizeType: parametrize,
    useVariables: true,
    ...flags,
  };
}

function sourceExtension(type, catalog) {
  const generator = (catalog?.generators || []).find((item) => item.name === type);
  const ext = generator?.extension;
  if (Array.isArray(ext) && ext[0]?.types) return String(ext[0].types);
  if (ext && typeof ext === "object" && ext.types) return String(ext.types);
  if (type === "Strategy XML") return "xml";
  return "txt";
}

export function bindResultsPluginHost(root, strategy) {
  pluginBridge?.abort();
  pluginBridge = null;
  const frame = root?.querySelector?.("iframe[data-results-plugin]");
  if (!frame || !strategy) return;
  pluginBridge = new AbortController();
  const { signal } = pluginBridge;
  const sendContext = () => {
    frame.contentWindow?.postMessage({ type: "SET_THEME", theme: "dark" }, "*");
    frame.contentWindow?.postMessage({
      type: "STRATEGY_DATA",
      data: {
        projectName: strategy.project,
        databankName: strategy.databank,
        strategyName: String(strategy.archive || "").replace(/\.sqx$/i, ""),
        resultKey: strategy.result_key || "Portfolio",
      },
    }, "*");
  };
  window.addEventListener("message", (event) => {
    if (event.source !== frame.contentWindow) return;
    const type = event.data?.type;
    const params = event.data?.params || {};
    if (type === "GET_STATS") {
      frame.contentWindow?.postMessage({ type: "STATS_RESPONSE", data: { stats: pluginStats(strategy, params) || {} } }, "*");
    } else if (type === "GET_ORDERS") {
      frame.contentWindow?.postMessage({ type: "ORDERS_RESPONSE", data: { orders: pluginOrders(strategy, params) } }, "*");
    } else if (type === "GET_LAST_SETTINGS_XML") {
      frame.contentWindow?.postMessage({ type: "LAST_SETTINGS_XML_RESPONSE", data: { lastSettingsXml: "" } }, "*");
    } else if (type === "GET_SOURCE_CODE") {
      const codeType = params.type || SOURCE_FORMAT_TO_TYPE[params.format] || "Pseudo Code(*.TXT)";
      printProjectSource(strategy.project, strategy.databank, strategy.archive, {
        type: codeType,
        mmType: params.mmType || "fromStrategy",
        parametrizeType: 0,
      }).then((printed) => {
        frame.contentWindow?.postMessage({
          type: "SOURCE_CODE_RESPONSE",
          data: {
            code: printed.code,
            warning: printed.warning,
            learnMoreURL: printed.learnMoreURL,
            success: printed.success || "ok",
          },
        }, "*");
      }).catch((error) => {
        frame.contentWindow?.postMessage({
          type: "SOURCE_CODE_RESPONSE",
          data: {
            code: "",
            warning: error instanceof Error ? error.message : "Source Code print failed",
            success: "error",
          },
        }, "*");
      });
    }
  }, { signal });
  frame.addEventListener("load", sendContext, { signal });
}

export function bindSourceCodeHost(root, strategy) {
  sourceBridge?.abort();
  sourceBridge = null;
  const pane = root?.querySelector?.("[data-results-source]");
  if (!pane || !strategy) return;
  sourceBridge = new AbortController();
  const { signal } = sourceBridge;
  const typeSelect = pane.querySelector("[data-source-type]");
  const mmSelect = pane.querySelector("[data-source-mm]");
  const status = pane.querySelector("[data-source-status]");
  const pre = pane.querySelector("[data-source-text]");
  const own = pane.querySelector("[data-source-own]");
  const copied = pane.querySelector("[data-source-copied]");
  let catalog = null;
  const syncOwn = () => {
    const selected = pane.querySelector("[data-source-parametrize]:checked");
    if (own) own.hidden = selected?.value !== "1";
  };
  const refresh = async () => {
    if (status) status.textContent = "Printing native source…";
    try {
      const printed = await printProjectSource(strategy.project, strategy.databank, strategy.archive, sourceConfigFrom(pane));
      if (pre) pre.textContent = printed.code || "No strategy selected.";
      if (status) {
        const warn = printed.warning ? ` ${printed.warning}` : "";
        status.textContent = `${printed.type} from StrategyQuant X sourcecode/print.${warn}`;
      }
    } catch (error) {
      if (status) status.textContent = error instanceof Error ? error.message : "Source Code print failed";
    }
  };
  fetchSourceCatalog().then((payload) => {
    catalog = payload;
    if (typeSelect) {
      typeSelect.innerHTML = payload.generators.map((item) => (
        `<option value="${escapeHtml(item.name)}" ${item.name === "Strategy XML" ? "selected" : ""}>${escapeHtml(item.name)}</option>`
      )).join("");
    }
    if (mmSelect && Array.isArray(payload.mmTypes) && payload.mmTypes.length) {
      mmSelect.innerHTML = payload.mmTypes.map((item) => (
        `<option value="${escapeHtml(item.value)}" ${item.value === "fromStrategy" ? "selected" : ""}>${escapeHtml(item.name)}</option>`
      )).join("");
    }
    if (status && payload.producer === "unavailable") {
      status.textContent = `${status.textContent} ${payload.detail || "Keep StrategyQuant X open for EasyLanguage / MQL."}`;
    }
    const ea = pane.querySelector("[data-source-ea]");
    if (ea) ea.hidden = payload.export_ea?.available !== true;
    pane.querySelectorAll("[data-mt-path]").forEach((input) => {
      const key = input.getAttribute("data-mt-path");
      if (key && payload.export_ea && typeof payload.export_ea[key] === "string") input.value = payload.export_ea[key];
    });
  }).catch(() => {});
  pane.addEventListener("change", (event) => {
    if (event.target.closest("[data-source-parametrize]")) syncOwn();
    if (event.target.closest("[data-source-type], [data-source-mm], [data-source-parametrize], [data-source-flag]")) {
      refresh();
    }
    const install = event.target.closest("[data-mt-path='mt4InstallPath'], [data-mt-path='mt5InstallPath']");
    if (install?.value) {
      const mt4 = install.getAttribute("data-mt-path") === "mt4InstallPath";
      sourcecodeAction("getDataPath", { installPath: install.value, isMT4: mt4 }).then((resolved) => {
        const target = pane.querySelector(`[data-mt-path="${mt4 ? "mt4DataPath" : "mt5DataPath"}"]`);
        if (target && resolved.dataPath) target.value = resolved.dataPath;
      }).catch(() => {});
    }
  }, { signal });
  pane.addEventListener("click", async (event) => {
    if (event.target.closest("[data-source-refresh]")) {
      event.preventDefault();
      refresh();
      return;
    }
    if (event.target.closest("[data-source-copy]")) {
      event.preventDefault();
      const text = pre?.textContent || "";
      try {
        await navigator.clipboard.writeText(text);
        if (copied) {
          copied.hidden = false;
          setTimeout(() => { copied.hidden = true; }, 2000);
        }
      } catch {
        if (status) status.textContent = "Copy to clipboard failed.";
      }
      return;
    }
    if (event.target.closest("[data-source-save]")) {
      event.preventDefault();
      const type = typeSelect?.value || "Strategy XML";
      const blob = new Blob([pre?.textContent || ""], { type: "text/plain" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `${String(strategy.archive || "strategy").replace(/\.sqx$/i, "")}.${sourceExtension(type, catalog)}`;
      link.click();
      URL.revokeObjectURL(url);
      return;
    }
    if (event.target.closest("[data-source-configure]")) {
      event.preventDefault();
      pane.querySelector("[data-source-mt-modal]")?.showModal?.();
      return;
    }
    if (event.target.closest("[data-source-mt-close]")) {
      event.preventDefault();
      pane.querySelector("[data-source-mt-modal]")?.close?.();
      return;
    }
    const saveEa = event.target.closest("[data-source-save-ea]");
    if (saveEa) {
      event.preventDefault();
      const mt4 = saveEa.getAttribute("data-source-save-ea") === "mt4";
      const mtStatus = pane.querySelector("[data-source-mt-status]") || status;
      const install = pane.querySelector(`[data-mt-path="${mt4 ? "mt4InstallPath" : "mt5InstallPath"}"]`)?.value;
      const data = pane.querySelector(`[data-mt-path="${mt4 ? "mt4DataPath" : "mt5DataPath"}"]`)?.value;
      if (!install || !data) {
        pane.querySelector("[data-source-mt-modal]")?.showModal?.();
        if (mtStatus) mtStatus.textContent = "Configure the MetaTrader installation and Experts folders first.";
        return;
      }
      if (status) status.textContent = `Saving EA to ${mt4 ? "MT4" : "MT5"} via sourcecode/saveEA…`;
      try {
        const saved = await sourcecodeAction("saveEA", {
          project: strategy.project,
          databank: strategy.databank,
          archive: strategy.archive,
          isMT4: mt4,
          ...sourceConfigFrom(pane),
        });
        if (status) status.textContent = saved.success ? "EA saved." : (saved.detail || "Save as EA failed.");
      } catch (error) {
        if (status) status.textContent = error instanceof Error ? error.message : "Save as EA failed.";
      }
    }
  }, { signal });
  pane.querySelector("[data-source-mt-form]")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const mtStatus = pane.querySelector("[data-source-mt-status]");
    const paths = {};
    pane.querySelectorAll("[data-mt-path]").forEach((input) => {
      paths[input.getAttribute("data-mt-path")] = input.value || "";
    });
    if (mtStatus) mtStatus.textContent = "Saving MetaTrader paths…";
    try {
      const saved = await sourcecodeAction("saveMTPaths", paths);
      if (mtStatus) mtStatus.textContent = saved.success ? "Settings saved." : (saved.detail || "saveMTPaths failed.");
      if (saved.success) pane.querySelector("[data-source-mt-modal]")?.close?.();
    } catch (error) {
      if (mtStatus) mtStatus.textContent = error instanceof Error ? error.message : "saveMTPaths failed.";
    }
  }, { signal });
  syncOwn();
}

export function bindOverviewHost(root, strategy) {
  overviewBridge?.abort();
  overviewBridge = null;
  const host = root?.querySelector?.("[data-results-overview]");
  const select = root?.querySelector?.("[data-results-template]");
  if (!host || !strategy) return;
  overviewBridge = new AbortController();
  const { signal } = overviewBridge;
  const frame = host.querySelector("[data-overview-frame]");
  const status = host.querySelector("[data-overview-status]");
  const stats = host.querySelector("[data-results-overview-stats]");
  const load = async () => {
    const template = select?.value || "TSOverview";
    if (template === "TSOverview") {
      if (frame) frame.hidden = true;
      if (stats) stats.hidden = false;
      if (status) status.textContent = "TS Overview columns from native trades. Other templates load StrategyQuant X overview/getOverviewContent.";
      return;
    }
    if (status) status.textContent = `Loading ${template} from StrategyQuant X overview/getOverviewContent…`;
    try {
      const payload = await fetchOverviewHtml(strategy.project, strategy.databank, strategy.archive, {
        template,
        sample: sampleKey({ sample: root.querySelector("[data-results-sample]")?.value }),
        direction: directionKey({ direction: root.querySelector("[data-results-direction]")?.value }),
      });
      if (payload.overviewHtml && frame) {
        frame.srcdoc = payload.overviewHtml;
        frame.hidden = false;
        if (stats) stats.hidden = true;
        if (status) status.textContent = `${payload.template} from StrategyQuant X.`;
      } else {
        if (frame) frame.hidden = true;
        if (stats) stats.hidden = false;
        if (status) status.textContent = payload.detail || "StrategyQuant X returned no overview HTML. TS Overview columns stay from native trades.";
      }
    } catch (error) {
      if (frame) frame.hidden = true;
      if (stats) stats.hidden = false;
      if (status) status.textContent = error instanceof Error ? error.message : "Overview HTML failed";
    }
  };
  select?.addEventListener("change", load, { signal });
  load();
}

export function bindChartHost(root, strategy) {
  chartBridge?.abort();
  chartBridge = null;
  const pane = root?.querySelector?.("[data-results-chart]");
  if (!pane) return;
  chartBridge = new AbortController();
  const { signal } = chartBridge;
  let live = strategy;
  let windowBars = strategy?.chart?.bars;
  let zoom = 1;
  const allTickets = [...new Set(
    (strategy?.orders?.state === "available" ? strategy.orders.payload.trades || [] : [])
      .filter((trade) => Number.isInteger(trade.Ticket))
      .sort((left, right) => (Number(left.CloseTime) || 0) - (Number(right.CloseTime) || 0))
      .map((trade) => String(trade.Ticket)),
  )];
  let tradeAt = Math.max(0, allTickets.length - 1);
  const tickets = () => [...new Set([...pane.querySelectorAll("[data-trade-ticket]")].map((node) => node.getAttribute("data-trade-ticket")).filter(Boolean))];
  const highlight = (ticket) => {
    pane.querySelectorAll("[data-trade-ticket]").forEach((node) => {
      node.classList.toggle("is-current", node.getAttribute("data-trade-ticket") === ticket);
    });
  };
  const paint = () => {
    const body = pane.querySelector("[data-chart-body]");
    if (!body || windowBars?.state !== "available") return;
    const trades = live?.orders?.state === "available" ? live.orders.payload.trades || [] : [];
    const sliced = visibleChartBars(windowBars, allTickets[tradeAt], zoom, trades);
    body.innerHTML = renderChartPlot(live, sliced);
    highlight(allTickets[tradeAt]);
  };
  const indicatorToggle = pane.querySelector("[data-chart-indicators-toggle]");
  const indicatorList = pane.querySelector("[data-chart-indicator-list]");
  const hint = pane.querySelector("[data-chart-store-hint]");
  pane.addEventListener("click", (event) => {
    if (event.target.closest("[data-chart-indicators-toggle]")) {
      event.preventDefault();
      if (indicatorToggle?.disabled) return;
      const open = Boolean(indicatorList?.hidden);
      if (indicatorList) indicatorList.hidden = !open;
      indicatorToggle?.setAttribute("aria-expanded", open ? "true" : "false");
      return;
    }
    if (event.target.closest("[data-chart-grid]")) {
      event.preventDefault();
      const button = event.target.closest("[data-chart-grid]");
      button.classList.toggle("is-active");
      pane.querySelector(".chart-grid")?.classList.toggle("is-nogrid", !button.classList.contains("is-active"));
      return;
    }
    const zoomBtn = event.target.closest("[data-chart-zoom]");
    if (zoomBtn) {
      event.preventDefault();
      const action = zoomBtn.getAttribute("data-chart-zoom");
      if (action === "+") zoom = Math.min(4, zoom + 0.25);
      else if (action === "-") zoom = Math.max(1, zoom - 0.25);
      else zoom = 1;
      paint();
      return;
    }
    const toggle = event.target.closest("[data-chart-toggle]");
    if (toggle) {
      event.preventDefault();
      const kind = toggle.getAttribute("data-chart-toggle");
      toggle.classList.toggle("is-active");
      pane.setAttribute(`data-chart-show-${kind}`, toggle.classList.contains("is-active") ? "1" : "0");
      return;
    }
    const step = event.target.closest("[data-chart-trade]");
    const ids = allTickets;
    if (step && ids.length) {
      event.preventDefault();
      tradeAt = (tradeAt + (step.getAttribute("data-chart-trade") === "next" ? 1 : ids.length - 1)) % ids.length;
      const ticket = ids[tradeAt];
      const onChart = tickets().includes(ticket);
      const finish = () => {
        paint();
        highlight(ticket);
      };
      if (onChart) {
        finish();
      } else if (Number.isInteger(Number(ticket))) {
        fetchProjectStrategy(live.project, live.databank, live.archive, live.task_index, globalThis.fetch, { focusTicket: Number(ticket) }).then((nextStrategy) => {
          live = nextStrategy;
          windowBars = nextStrategy.chart?.bars;
          if (windowBars?.basis) pane.setAttribute("data-chart-basis", windowBars.basis);
          finish();
        }).catch(() => finish());
      } else {
        finish();
      }
    }
  }, { signal });
  if (!strategy?.project || !strategy.databank || !strategy.archive) return;
  if (strategy.chart?.stored !== true) return;
  fetchResultsChart(strategy.project, strategy.databank, strategy.archive).then((payload) => {
    const indicators = Array.isArray(payload.indicators) ? payload.indicators : [];
    if (indicatorList) {
      indicatorList.innerHTML = indicators.map((item) => (
        `<li><label><input type="checkbox" data-chart-indicator-id="${escapeHtml(item.id)}" ${item.show ? "checked" : ""}>${escapeHtml(item.title)}</label></li>`
      )).join("");
    }
    if (indicatorToggle) {
      indicatorToggle.disabled = indicators.length === 0;
      indicatorToggle.title = indicators.length
        ? "Native StrategyQuant X chart indicators"
        : "Indicators need Store Chart Data on a repeated native backtest";
    }
    if (hint && payload.detail && payload.stored !== true && pane.getAttribute("data-results-chart") !== "sidecar") {
      const settingsHref = storeChartSettingsHref(strategy);
      hint.innerHTML = `${escapeHtml(payload.detail)} Check <a class="workflow-link" href="${escapeHtml(settingsHref)}" data-route="${escapeHtml(settingsHref)}" data-automation-section="Options">Store Chart Data</a> in Settings — Strategy options and repeat the backtest.`;
    }
    const nativeBars = payload.bars;
    const body = pane.querySelector("[data-chart-body]");
    if (body && nativeBars?.state === "available" && Array.isArray(nativeBars.bars) && nativeBars.bars.length) {
      pane.setAttribute("data-results-chart", "native");
      pane.setAttribute("data-chart-basis", nativeBars.basis || "sqx_results_charts");
      windowBars = nativeBars;
      body.innerHTML = renderChartPlot(strategy, nativeBars, { legendName: "StrategyQuant X OHLC" });
    }
  }).catch((error) => {
    if (hint) hint.textContent = error instanceof Error ? error.message : "Results chart failed";
  });
}

export function bindResultsChrome(root, strategy) {
  bindResultsPluginHost(root, strategy);
  bindSourceCodeHost(root, strategy);
  bindOverviewHost(root, strategy);
  bindChartHost(root, strategy);
}

function renderStrategyChrome(topology, view, body, strategy = null) {
  const resultView = knownResultView(view, strategy);
  const head = strategy
    ? `<p class="workflow-crumb"><strong>${escapeHtml(strategy.databank)}</strong><span>/</span><strong>${escapeHtml(strategy.archive)}</strong></p>`
    : "";
  return `<div class="workflow-strategy-results"${strategy ? ` data-results-archive="${escapeHtml(strategy.archive)}"` : ""}>
    ${head}
    ${renderResultTabs(topology, { ...view, resultView }, strategy)}
    ${renderResultsToolbar(topology, { ...view, resultView }, strategy)}
    ${body}
  </div>`;
}

export function renderStrategyResults(topology, strategy, view, error = "") {
  const resultView = knownResultView(view, strategy);
  const plugin = pluginTab(strategy, resultView);
  let body = "";
  if (error) {
    body = unavailable("Could not inspect this archive", error, { compact: true, tone: "error" });
  } else if (!strategy && resultView !== "overview") {
    body = unavailable("Reading archive…", "Inspecting producer orders.bin and settings.xml from this databank .sqx.", { compact: true, tone: "pending" });
  } else if (resultView === "overview") body = renderOverviewView(strategy, view);
  else if (resultView === "sp-overview") body = renderSpOverviewView(strategy, view);
  else if (resultView === "equity") body = renderEquityView(strategy);
  else if (resultView === "config") body = renderConfigView(strategy);
  else if (resultView === "chart") body = renderChartView(strategy);
  else if (resultView === "trades") body = renderTradesView(strategy, view);
  else if (resultView === "trade-analysis") body = renderTradeAnalysisView(strategy);
  else if (resultView === "profile") body = renderProfileView(strategy);
  else if (resultView === "source") body = renderSourceView(strategy);
  else if (plugin) body = renderPluginView(strategy, plugin, resultTabs(strategy).find(([id]) => id === resultView)?.[1]);
  else body = renderUnavailableResult(resultTabs(strategy).find((item) => item[0] === resultView)?.[1] || resultView);
  return renderStrategyChrome(topology, { ...view, resultView }, body, error ? null : strategy);
}

export function renderResultsPanel(topology, results, view = {}, strategy = null, strategyError = "") {
  const item = projectResultsOf(results, topology.project);
  const resultView = knownResultView(view, strategy);
  const selected = Boolean(view.archive && view.databank);
  const toolbar = renderDatabankToolbar(topology, view, results, strategy);
  const list = renderProjectDatabankList(results, topology.project, {
    archiveHref: (bank, archive) => workflowHref({
      project: topology.project,
      tab: "results",
      task: view.task,
      databank: bank,
      archive,
      resultView: "overview",
    }),
    selectedDatabank: view.databank || "",
    selectedArchive: view.archive || "",
  });
  const detail = selected
    ? renderStrategyResults(topology, strategy, { ...view, resultView }, strategyError)
    : renderStrategyResults(topology, null, { ...view, resultView: "overview" }, "");
  return `<div class="workflow-progress-panel">
    ${toolbar}
    ${item?.databanks?.length ? `<div class="sqx-databank-grid">${list}</div>` : ""}
    ${detail}
  </div>`;
}

export { customProjectResultsFromPayload, fetchCustomProjectResults, renderProjectDatabankStats };
