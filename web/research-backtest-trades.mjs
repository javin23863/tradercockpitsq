import { researchLocationMatches } from "./model.mjs";
import {
  fetchHistoricalResults,
  historicalResultFromPayload,
} from "./research-backtest.mjs";
import { chartFrame } from "./ui.mjs";
import { COCKPIT_VERDICT_SCHEMA, formatMoney, formatNumber } from "./research-verdicts.mjs";

const HISTORICAL_RESULTS_API_PATH = "/api/research/historical-results";
const RESEARCH_TRADES_SCHEMA = "tc.research-historical-trades.v1";
const ORDERS_FORMAT = "SQOrderFileFormat:11";
const ORDERS_FORMAT_VERSION = 11;
const ORDERS_ENTRY = "orders.bin";
const SQX_BUILD = "144.2953";
const FILLED_TYPES = new Set([1, 2, 9, 11]);

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function tradesRoute() {
  return researchLocationMatches(globalThis.location, "validate", "trades");
}

async function readJson(response) {
  try { return await response.json(); } catch { return null; }
}

function apiError(response, payload, fallback) {
  const error = new Error(payload?.detail || fallback);
  error.status = response?.status || 0;
  error.payload = payload;
  return error;
}

function digest(value) {
  return typeof value === "string" && /^[0-9a-f]{64}$/.test(value) ? value : "";
}

function evidenceDigest(value) {
  if (typeof value !== "string" || !value.startsWith("tc-evidence:sha256:")) return "";
  return digest(value.slice("tc-evidence:sha256:".length));
}

function validNumber(value) {
  return typeof value === "number" && Number.isFinite(value);
}

function tradeFromPayload(value) {
  if (
    !value
    || typeof value !== "object"
    || !Number.isInteger(value.Ticket)
    || !Number.isInteger(value.Order)
    || !Number.isInteger(value.Type)
    || !FILLED_TYPES.has(value.Type)
    || !Number.isInteger(value.CloseType)
    || value.CloseType === 18
    || !Number.isInteger(value.SampleType)
    || !Number.isInteger(value.IsInPortfolio)
    || value.IsInPortfolio === 0
    || (value.Symbol !== null && typeof value.Symbol !== "string")
    || !validNumber(value.Size)
    || !Number.isInteger(value.OpenTime)
    || !validNumber(value.OpenPrice)
    || !Number.isInteger(value.CloseTime)
    || !validNumber(value.ClosePrice)
    || !validNumber(value.PL)
    || !validNumber(value.PipsPL)
    || !Number.isInteger(value.BarsInTrade)
    || !Number.isInteger(value.Duration)
  ) {
    throw new Error("Native trade record is invalid");
  }
  return value;
}

export function historicalTradesFromPayload(payload, historicalResult) {
  const selection = payload?.selection;
  if (
    !payload
    || payload.schema !== RESEARCH_TRADES_SCHEMA
    || payload.sqx_build !== SQX_BUILD
    || payload.orders_format !== ORDERS_FORMAT
    || payload.orders_format_version !== ORDERS_FORMAT_VERSION
    || payload.orders_entry !== ORDERS_ENTRY
    || !digest(payload.orders_entry_sha256)
    || !Number.isInteger(payload.native_order_count)
    || payload.native_order_count < 0
    || !Number.isInteger(payload.trade_count)
    || payload.trade_count < 0
    || payload.trade_count > payload.native_order_count
    || !Array.isArray(payload.trades)
    || payload.trades.length !== payload.trade_count
    || selection?.result_key !== "Portfolio"
    || selection?.direction !== 0
    || selection?.sample_type !== 127
    || selection?.filled_orders !== true
    || selection?.control_orders !== false
    || selection?.native_filter !== "filterExcludingControlOrders"
  ) {
    throw new Error("Historical Trades producer contract is invalid");
  }

  const result = historicalResultFromPayload(historicalResult);
  if (
    payload.historical_result_entity_id !== result.entity_id
    || payload.historical_result_revision !== result.revision
    || payload.candidate_entity_id !== result.candidate_entity_id
    || payload.candidate_revision !== result.candidate_revision
    || payload.result_archive_ref !== result.result_archive_ref
    || payload.result_archive_sha256 !== result.result_archive_sha256
    || evidenceDigest(payload.result_archive_ref) !== payload.result_archive_sha256
  ) {
    throw new Error("Historical Trades readback does not bind the selected result revision");
  }

  return { ...payload, trades: payload.trades.map(tradeFromPayload) };
}

export function cockpitVerdictFromDetail(payload) {
  const readback = payload?.cockpit_verdict;
  if (!readback || typeof readback !== "object") {
    return { state: "unavailable", reason_code: "cockpit_verdict_missing", detail: "Cockpit verdict is not attached to this Historical Result.", payload: null };
  }
  if (readback.state === "unavailable") {
    if (typeof readback.reason_code !== "string" || !readback.reason_code) {
      throw new Error("Cockpit verdict unavailable state is invalid");
    }
    return { state: "unavailable", reason_code: readback.reason_code, detail: typeof readback.detail === "string" ? readback.detail : readback.reason_code, payload: null };
  }
  if (readback.state !== "available" || !readback.payload || readback.payload.schema !== COCKPIT_VERDICT_SCHEMA) {
    throw new Error("Cockpit verdict schema mismatch");
  }
  return { state: "available", reason_code: null, detail: null, payload: readback.payload };
}

export function historicalResultDetailFromPayload(payload) {
  const result = historicalResultFromPayload(payload);
  const readback = payload?.trades_readback;
  if (!readback || !["available", "unavailable"].includes(readback.state)) {
    throw new Error("Historical Trades readback state is missing");
  }
  const verdict = cockpitVerdictFromDetail(payload);
  if (readback.state === "available") {
    return {
      result,
      tradesReadback: {
        state: "available",
        payload: historicalTradesFromPayload(readback.payload, result),
      },
      verdict,
    };
  }
  if (typeof readback.reason_code !== "string" || !readback.reason_code || typeof readback.detail !== "string" || !readback.detail) {
    throw new Error("Historical Trades unavailable state is invalid");
  }
  return {
    result,
    tradesReadback: {
      state: "unavailable",
      reason_code: readback.reason_code,
      detail: readback.detail,
    },
    verdict,
  };
}

export async function fetchHistoricalResultDetail(entityId, fetchImpl = globalThis.fetch) {
  if (typeof entityId !== "string" || !entityId) throw new Error("Historical result entity identity is required");
  const response = await fetchImpl(`${HISTORICAL_RESULTS_API_PATH}?entityId=${encodeURIComponent(entityId)}`, {
    headers: { accept: "application/json" },
  });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Historical Trades readback failed");
  const detail = historicalResultDetailFromPayload(payload);
  if (detail.result.entity_id !== entityId) throw new Error("Historical result detail identity changed during readback");
  return detail;
}

function short(value) {
  const text = String(value || "");
  return text.length > 22 ? `…${text.slice(-20)}` : text;
}

function numberText(value) {
  return typeof value === "number" && Number.isFinite(value) ? String(value) : "—";
}

function metricCell(label, value, note = "") {
  const empty = value === "—";
  return `<div class="metric"><span>${escapeHtml(label)}</span><strong class="${empty ? "is-empty" : ""}"${note ? ` title="${escapeHtml(note)}"` : ""}>${escapeHtml(value)}</strong></div>`;
}

export function renderTradesVerdict(verdict) {
  if (!verdict || verdict.state !== "available" || !verdict.payload) {
    const detail = verdict?.detail || "Cockpit verdict is not attached to this Historical Result.";
    return `<div data-trades-verdict="unavailable">${unavailableBlock("Cockpit verdict unavailable", detail, verdict?.reason_code)}</div>`;
  }
  const body = verdict.payload;
  const stats = body.statistics?.full || {};
  const history = body.chart_history;
  const monthsNote = stats.months_basis === "chart_history"
    ? `Native Setup ${history?.date_from || ""} → ${history?.date_to || ""}`
    : "Traded span (first open to last close); native Setup dateFrom/dateTo was not readable";
  const equity = Array.isArray(body.equity) ? body.equity : [];
  const values = equity.map((point) => point.balance).filter((value) => typeof value === "number" && Number.isFinite(value));
  let chart;
  if (values.length > 1) {
    const low = Math.min(...values);
    const high = Math.max(...values);
    const mid = (low + high) / 2;
    const first = new Date(equity[0].time).toISOString().slice(0, 7);
    const last = new Date(equity[equity.length - 1].time).toISOString().slice(0, 7);
    chart = chartFrame({
      height: 160,
      state: "historical",
      detail: "",
      legend: [["Equity (native trades)", "purple"]],
      yLabels: [formatMoney(high), formatMoney(mid), formatMoney(low)],
      xLabels: [first, last],
      series: [{ values, tone: "purple" }],
    });
  } else {
    chart = chartFrame({
      height: 120,
      state: "unavailable",
      detail: "Equity draws from the native trade records of this result. No series yet.",
      legend: [["Equity (native trades)", "purple"]],
      yLabels: ["", "", ""],
      xLabels: [],
    });
  }
  return `<div data-trades-verdict="available" data-months-basis="${escapeHtml(stats.months_basis || "traded_span")}">
    <div class="metric-grid">
      ${metricCell("Net Profit", formatMoney(stats.NetProfit))}
      ${metricCell("Profit Factor", formatNumber(stats.ProfitFactor))}
      ${metricCell("Ret/DD", formatNumber(stats.ReturnDDRatio))}
      ${metricCell("Max DD", formatMoney(stats.Drawdown))}
      ${metricCell("Expectancy", formatMoney(stats.Expectancy))}
      ${metricCell("Avg trades / month", formatNumber(stats.AvgTradesPerMonth), monthsNote)}
    </div>
    <p class="note">Cockpit verdict over exact native trades. Avg trades/month uses ${escapeHtml(monthsNote)}.</p>
    ${chart}
  </div>`;
}

function unavailableBlock(title, detail, code = "") {
  return `<div class="empty-state is-compact"><div class="empty-icon">—</div><div><strong>${escapeHtml(title)}</strong><p>${escapeHtml(detail)}</p>${code ? `<code>${escapeHtml(code)}</code>` : ""}</div></div>`;
}

function tradesTable(payload) {
  if (!payload.trades.length) {
    return `<div class="empty-state"><div class="empty-icon">—</div><div><strong>No native portfolio trades</strong><p>The selected result contains no records matching the native Portfolio filled/non-control filter. No trades are synthesized.</p></div></div>`;
  }
  const rows = payload.trades.map((trade) => `<tr data-native-trade-ticket="${escapeHtml(trade.Ticket)}">
    <td><code>${escapeHtml(trade.Ticket)}</code></td>
    <td>${escapeHtml(trade.Symbol || "—")}</td>
    <td><code>${escapeHtml(trade.Type)}</code></td>
    <td>${escapeHtml(numberText(trade.Size))}</td>
    <td><code>${escapeHtml(trade.OpenTime)}</code></td>
    <td>${escapeHtml(numberText(trade.OpenPrice))}</td>
    <td><code>${escapeHtml(trade.CloseTime)}</code></td>
    <td>${escapeHtml(numberText(trade.ClosePrice))}</td>
    <td>${escapeHtml(numberText(trade.PL))}</td>
    <td>${escapeHtml(numberText(trade.PipsPL))}</td>
    <td>${escapeHtml(trade.BarsInTrade)}</td>
    <td>${escapeHtml(trade.Duration)} s</td>
  </tr>`).join("");
  return `<div class="trade-table-wrap"><table class="trade-table" data-native-trades-table>
    <thead><tr><th>Ticket</th><th>Symbol</th><th>Native type</th><th>Size</th><th>Open time</th><th>Open</th><th>Close time</th><th>Close</th><th>P/L</th><th>Pips</th><th>Bars</th><th>Duration</th></tr></thead>
    <tbody>${rows}</tbody>
  </table></div>`;
}

function readbackBody(detail) {
  if (!detail) {
    return `<div class="empty-state"><div class="empty-icon">—</div><div><strong>Select a completed historical result</strong><p>Trades are read only from one immutable native result revision.</p></div></div>`;
  }
  if (detail.tradesReadback.state === "unavailable") {
    return `<div class="empty-state"><div class="empty-icon">—</div><div><strong>Native Trades unavailable</strong><p>${escapeHtml(detail.tradesReadback.detail)}</p><code>${escapeHtml(detail.tradesReadback.reason_code)}</code></div></div>`;
  }
  const payload = detail.tradesReadback.payload;
  return `<div data-trades-result-revision="${escapeHtml(detail.result.revision)}">
    <div class="context-callout"><span class="callout-icon">↳</span><div><span class="eyebrow">Native result readback</span><strong>${escapeHtml(payload.trade_count)} portfolio trade record${payload.trade_count === 1 ? "" : "s"}</strong><span>${escapeHtml(payload.native_order_count)} total native order records · exact ${escapeHtml(payload.orders_entry)} custody</span></div></div>
    <div class="idea-identity">
      <div class="stat-row"><span>Historical result</span><code>${escapeHtml(payload.historical_result_entity_id)}</code></div>
      <div class="stat-row"><span>Result revision</span><code>${escapeHtml(payload.historical_result_revision)}</code></div>
      <div class="stat-row"><span>Result archive SHA-256</span><code>${escapeHtml(payload.result_archive_sha256)}</code></div>
      <div class="stat-row"><span>Orders SHA-256</span><code>${escapeHtml(payload.orders_entry_sha256)}</code></div>
      <div class="stat-row"><span>Native order format</span><code>${escapeHtml(payload.orders_format)}</code></div>
    </div>
    ${renderTradesVerdict(detail.verdict)}
    ${tradesTable(payload)}
  </div>`;
}

let generation = 0;
let state = {
  phase: "idle",
  results: [],
  selectedIndex: 0,
  detail: null,
  message: "",
};

function tradesPanel() {
  if (!tradesRoute()) return null;
  return document.querySelector('[data-research-host="trades"]');
}

function render(panel, current) {
  if (!panel?.isConnected) return;
  const results = current.results;
  const selected = results[current.selectedIndex] || null;
  const host = panel.querySelector(".empty-state")?.parentElement || panel;
  panel.querySelector(".empty-state")?.remove();
  let workspace = panel.querySelector("[data-research-trades]");
  if (!workspace) {
    workspace = document.createElement("div");
    workspace.dataset.researchTrades = "";
    host.append(workspace);
  }
  workspace.innerHTML = `<div class="panel" data-accent="cyan">
    <div class="panel-heading"><div><p class="eyebrow">Exact Historical Result</p><h2>Native trade records</h2></div></div>
    <label class="field-label" for="historical-trades-result">Completed Retester result</label>
    <select id="historical-trades-result" class="idea-editor" ${results.length ? "" : "disabled"}>${results.length ? results.map((result, index) => `<option value="${index}" ${index === current.selectedIndex ? "selected" : ""}>${escapeHtml(result.result_archive_name)} · ${escapeHtml(short(result.revision))}</option>`).join("") : '<option>No completed historical results</option>'}</select>
    ${selected ? `<div class="idea-identity"><div class="stat-row"><span>Selected revision</span><code>${escapeHtml(selected.revision)}</code></div><div class="stat-row"><span>Candidate revision</span><code>${escapeHtml(selected.candidate_revision)}</code></div></div>` : ""}
    <p class="field-help">The browser selects only a canonical Historical Result identity. The backend reopens its immutable native archive and reads the producer-owned order record; alternate exports and synthetic trades are not accepted.</p>
    <p class="idea-save-status" data-trades-status>${escapeHtml(current.message || "")}</p>
    ${current.phase === "loading" ? '<div class="idea-catalog-state">Loading exact native trade readback…</div>' : readbackBody(current.detail)}
  </div>`;
}

async function loadSelected() {
  const currentGeneration = ++generation;
  const panel = tradesPanel();
  const selected = state.results[state.selectedIndex] || null;
  if (!panel || !selected) {
    state = { ...state, phase: "loaded", detail: null, message: "" };
    render(panel, state);
    return;
  }
  state = { ...state, phase: "loading", detail: null, message: "Reading exact Historical Result revision…" };
  render(panel, state);
  try {
    const detail = await fetchHistoricalResultDetail(selected.entity_id);
    if (currentGeneration !== generation || !tradesRoute()) return;
    if (detail.result.revision !== selected.revision || detail.result.result_archive_sha256 !== selected.result_archive_sha256) {
      throw new Error("Historical result changed between catalog and Trades detail read");
    }
    state = { ...state, phase: "loaded", detail, message: "Exact native Trades readback loaded." };
  } catch (error) {
    if (currentGeneration !== generation || !tradesRoute()) return;
    state = { ...state, phase: "failed", detail: null, message: error instanceof Error ? error.message : "Historical Trades readback failed" };
  }
  render(tradesPanel(), state);
}

async function load() {
  const currentGeneration = ++generation;
  const panel = tradesPanel();
  if (!panel) return;
  state = { phase: "loading", results: [], selectedIndex: 0, detail: null, message: "Loading completed Historical Results…" };
  render(panel, state);
  try {
    const catalog = await fetchHistoricalResults();
    if (currentGeneration !== generation || !tradesRoute()) return;
    const results = catalog.filter((result) => result.state === "completed" && result.execution_completed === true);
    state = { phase: "loaded", results, selectedIndex: 0, detail: null, message: results.length ? "" : "No completed native Historical Result is available for Trades." };
    render(tradesPanel(), state);
    if (results.length) await loadSelected();
  } catch (error) {
    if (currentGeneration !== generation || !tradesRoute()) return;
    state = { phase: "failed", results: [], selectedIndex: 0, detail: null, message: error instanceof Error ? error.message : "Historical Result catalog unavailable" };
    render(tradesPanel(), state);
  }
}

if (typeof document !== "undefined") {
  document.addEventListener("change", (event) => {
    if (!tradesRoute() || event.target?.id !== "historical-trades-result") return;
    const selectedIndex = Number(event.target.value);
    if (!Number.isInteger(selectedIndex) || selectedIndex < 0 || selectedIndex >= state.results.length) return;
    state = { ...state, selectedIndex, detail: null, message: "" };
    void loadSelected();
  });
  const observer = new MutationObserver(() => {
    const panel = tradesPanel();
    if (tradesRoute() && panel && !panel.querySelector("[data-research-trades]")) void load();
    if (!tradesRoute()) generation += 1;
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void load();
}
