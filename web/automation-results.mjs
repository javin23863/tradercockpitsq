import {
  actionButton,
  chartFrame,
  escapeHtml,
  readable,
  table,
  unavailable,
} from "./ui.mjs";
import { workflowHref } from "./automation-settings-controls.mjs";
import {
  customProjectResultsFromPayload,
  fetchCustomProjectResults,
  projectResultsOf,
  renderProjectDatabankList,
  renderProjectDatabankStats,
} from "./custom-project-results.mjs";

export const STRATEGY_API_PATH = "/api/sqx-project-strategy";
const STRATEGY_SCHEMA = "tc.sqx-custom-project-strategy.v1";
const SQX_BUILD = "144.2953";
const RESULT_VIEWS = Object.freeze(["trades", "equity", "config", "chart"]);
const FILLED_TYPES = new Set([1, 2, 9, 11]);

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
    || record.native_version !== SQX_BUILD
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

export async function fetchProjectStrategy(project, databank, archive, task, fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Native strategy inspect fetch is unavailable");
  const exact = projectName(project);
  const bank = projectName(databank);
  const name = typeof archive === "string" && archive.toLowerCase().endsWith(".sqx") ? archive : "";
  if (!exact || !bank || !name) throw new Error("Exact native project, databank, and archive are required");
  const params = new URLSearchParams({ project: exact, databank: bank, archive: name });
  if (Number.isInteger(task)) params.set("task", String(task));
  const response = await fetchImpl(`${STRATEGY_API_PATH}?${params.toString()}`, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || `Native strategy inspect failed: ${response?.status ?? "unknown"}`);
  return projectStrategyFromPayload(payload);
}

function numberText(value) {
  return typeof value === "number" && Number.isFinite(value) ? String(value) : "—";
}

function renderTradesView(strategy) {
  const orders = strategy.orders;
  if (orders.state !== "available") {
    return unavailable(
      "List of trades unavailable",
      orders.detail || "This archive has no inspectable orders.bin records.",
      { compact: true },
    );
  }
  const trades = orders.payload.trades || [];
  if (!trades.length) {
    return unavailable(
      "No native portfolio trades",
      "The selected archive contains no filled portfolio trades. This desktop does not invent rows.",
      { compact: true },
    );
  }
  return `<div data-results-trades>
    <p class="field-help">${trades.length} native trade${trades.length === 1 ? "" : "s"} from orders.bin · ${escapeHtml(String(strategy.archive_sha256).slice(0, 12))}…</p>
    <div class="trade-table-wrap"><table class="trade-table" data-native-trades-table>
      <thead><tr><th>Ticket</th><th>Symbol</th><th>Native type</th><th>Size</th><th>Open</th><th>Close</th><th>P/L</th><th>Pips</th></tr></thead>
      <tbody>${trades.map((trade) => `<tr data-native-trade-ticket="${escapeHtml(trade.Ticket)}">
        <td><code>${escapeHtml(trade.Ticket)}</code></td>
        <td>${escapeHtml(trade.Symbol || "—")}</td>
        <td><code>${escapeHtml(trade.Type)}</code></td>
        <td>${escapeHtml(numberText(trade.Size))}</td>
        <td><code>${escapeHtml(trade.OpenTime)}</code></td>
        <td><code>${escapeHtml(trade.CloseTime)}</code></td>
        <td>${escapeHtml(numberText(trade.PL))}</td>
        <td>${escapeHtml(numberText(trade.PipsPL))}</td>
      </tr>`).join("")}</tbody>
    </table></div>
  </div>`;
}

function renderEquityView(strategy) {
  const equity = strategy.equity || [];
  const values = equity.map((point) => point.balance).filter((value) => typeof value === "number" && Number.isFinite(value));
  if (strategy.orders?.state !== "available") {
    return unavailable(
      "Equity chart unavailable",
      strategy.orders?.detail || "Equity is the running sum of producer-recorded trade P/L. orders.bin is unread.",
      { compact: true },
    );
  }
  const basis = strategy.equity_basis === "archive_initial_capital"
    ? `Starts at archive InitialCapital ${numberText(strategy.initial_capital)}`
    : "Cumulative native P/L; archive InitialCapital was not recorded";
  if (values.length < 2) {
    const point = equity[0];
    return `<div data-results-equity data-equity-basis="${escapeHtml(strategy.equity_basis || "")}">
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
  return `<div data-results-equity data-equity-basis="${escapeHtml(strategy.equity_basis || "")}">
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

function renderChartView(strategy) {
  if (strategy.chart?.stored === true) {
    return `<div data-results-chart="stored"><p class="field-help">This archive stored chart members: ${escapeHtml((strategy.chart.entries || []).join(", "))}. A candle overlay is not synthesized from trades.</p></div>`;
  }
  return `<div data-results-chart="unavailable">${unavailable(
    "Trades on chart unavailable",
    strategy.chart?.detail || "This archive did not store chart data.",
    { compact: true },
  )}</div>`;
}

function renderResultTabs(topology, view) {
  const items = [
    ["trades", "List of trades"],
    ["equity", "Equity chart"],
    ["config", "Strategy config"],
    ["chart", "Trades on chart"],
  ];
  return `<div class="settings-nested-tabs" role="tablist">${items.map(([id, label]) => {
    const href = workflowHref({
      project: topology.project,
      tab: "results",
      task: view.task,
      databank: view.databank,
      archive: view.archive,
      resultView: id,
    });
    const current = id === view.resultView;
    return `<a class="workflow-tab ${current ? "is-current" : ""}" role="tab" aria-selected="${current}" href="${escapeHtml(href)}" data-automation-result-view="${id}">${escapeHtml(label)}</a>`;
  }).join("")}</div>`;
}

export function renderStrategyResults(topology, strategy, view, error = "") {
  if (error) {
    return unavailable("Could not inspect this archive", error, { compact: true, tone: "error" });
  }
  if (!strategy) {
    return unavailable("Reading archive…", "Inspecting producer orders.bin and settings.xml from this databank .sqx.", { compact: true, tone: "pending" });
  }
  let body = "";
  if (view.resultView === "equity") body = renderEquityView(strategy);
  else if (view.resultView === "config") body = renderConfigView(strategy);
  else if (view.resultView === "chart") body = renderChartView(strategy);
  else body = renderTradesView(strategy);
  return `<div class="workflow-strategy-results" data-results-archive="${escapeHtml(strategy.archive)}">
    <p class="workflow-crumb"><strong>${escapeHtml(strategy.databank)}</strong><span>/</span><strong>${escapeHtml(strategy.archive)}</strong></p>
    ${renderResultTabs(topology, view)}
    ${body}
  </div>`;
}

export function renderResultsPanel(topology, results, view = {}, strategy = null, strategyError = "") {
  const item = projectResultsOf(results, topology.project);
  const emptyReason = "Native Custom Project launch is still unwired, so a new run cannot write archives here. Existing databank .sqx files can still be inspected.";
  if (!item?.databanks?.length) {
    return `<div class="workflow-progress-panel">
      ${unavailable("No databanks in this project yet", emptyReason, { compact: true })}
    </div>`;
  }
  const list = renderProjectDatabankList(results, topology.project, {
    archiveHref: (bank, archive) => workflowHref({
      project: topology.project,
      tab: "results",
      task: view.task,
      databank: bank,
      archive,
      resultView: "trades",
    }),
  });
  const selected = view.archive && view.databank
    ? renderStrategyResults(topology, strategy, {
      ...view,
      resultView: RESULT_VIEWS.includes(view.resultView) ? view.resultView : "trades",
    }, strategyError)
    : `<p class="field-help">Select an inspectable .sqx to open List of trades, equity, and strategy config from that archive. This desktop does not invent P&amp;L.</p>`;
  return `<div class="workflow-progress-panel">
    ${list}
    ${selected}
  </div>`;
}

export { customProjectResultsFromPayload, fetchCustomProjectResults, renderProjectDatabankStats };
