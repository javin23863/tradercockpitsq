// Data manager: read-only view of the data StrategyQuant X actually has installed.
// Everything shown comes from the running producer (constants/getAll, main/getData,
// data/getSymbolData) through the canonical backend read models. Adding, importing,
// downloading, or editing data stays in StrategyQuant X; this desktop does not invent a
// downloader, an instrument editor, or a symbol list.

import { fetchInstalledDataSymbols, fetchSymbolData } from "./automation-workflows.mjs";
import { timeToDateString } from "./automation-settings-controls.mjs";
import { fetchSqxModule } from "./sqx-modules.mjs";
import { card, chartFrame, chip, escapeHtml, kpi, statList, table, unavailable } from "./ui.mjs";

export const DATA_MANAGER_HOST_SELECTOR = "[data-data-manager-host]";

function isoDate(millis) {
  if (!Number.isFinite(Number(millis)) || Number(millis) <= 0) return "";
  return new Date(Number(millis)).toISOString().slice(0, 10);
}

function count(value) {
  return Array.isArray(value) ? value.length : 0;
}

export function dataManagerViewModel({ module = null, installed = null, installedError = "" } = {}) {
  const rows = Array.isArray(installed?.rows) ? installed.rows : [];
  const typeNames = new Map((installed?.dataTypes || []).map((item) => [String(item.key), item.name]));
  const symbols = rows.map((row) => ({
    symbol: row.symbol,
    dataType: row.dataType != null ? (typeNames.get(String(row.dataType)) || String(row.dataType)) : "",
    timeframe: row.timeframe || "",
    dateFrom: row.dateFrom ?? null,
    dateTo: row.dateTo ?? null,
    rows: Number.isInteger(row.rows) ? row.rows : null,
    hidden: row.show === false,
  }));
  const listed = new Set(symbols.map((row) => row.symbol));
  for (const name of installed?.symbols || []) {
    if (!listed.has(name)) symbols.push({ symbol: name, dataType: "", timeframe: "", dateFrom: null, dateTo: null, rows: null, hidden: false });
  }
  return {
    state: installed ? "ready" : installedError ? "unavailable" : "pending",
    detail: installed ? installed.detail || "" : installedError,
    module,
    symbols,
    sessions: installed?.sessions || [],
    dataTypes: installed?.dataTypes || [],
    precisions: installed?.precisions || [],
    swapTypes: installed?.swapTypes || [],
    tripleSwapOptions: installed?.tripleSwapOptions || [],
  };
}

function renderSymbolsTable(view) {
  const columns = [
    { label: "Symbol" },
    { label: "Type" },
    { label: "Timeframe" },
    { label: "From" },
    { label: "To" },
    { label: "Bars", align: "right" },
    { label: "Visible" },
  ];
  const rows = view.symbols.map((row) => ({
    attrs: `data-data-symbol="${escapeHtml(row.symbol)}" data-date-from="${escapeHtml(row.dateFrom ? timeToDateString(row.dateFrom) : "")}" data-date-to="${escapeHtml(row.dateTo ? timeToDateString(row.dateTo) : "")}" tabindex="0" role="button" aria-label="Show data range for ${escapeHtml(row.symbol)}"`,
    cells: [
      `<strong>${escapeHtml(row.symbol)}</strong>`,
      escapeHtml(row.dataType || "—"),
      escapeHtml(row.timeframe || "—"),
      escapeHtml(isoDate(row.dateFrom) || "—"),
      escapeHtml(isoDate(row.dateTo) || "—"),
      row.rows === null ? "—" : escapeHtml(row.rows.toLocaleString("en-US")),
      row.hidden ? chip("hidden", "unavailable") : chip("shown", "ready"),
    ],
  }));
  return table({ columns, rows, empty: "StrategyQuant X reports no installed data series.", attrs: 'data-data-symbols-table' });
}

function renderLists(view) {
  const list = (items, empty) => (items.length
    ? `<ul class="data-manager-list">${items.map((item) => `<li>${escapeHtml(typeof item === "string" ? item : item.name)}</li>`).join("")}</ul>`
    : `<p class="field-help">${escapeHtml(empty)}</p>`);
  return `<div class="data-manager-lists">
    ${card({ title: "Sessions", sub: "Data manager → Sessions in StrategyQuant X", accent: "cyan", body: list(view.sessions, "No sessions reported.") })}
    ${card({ title: "Data types", sub: "constants/getAll dataTypes", accent: "blue", body: list(view.dataTypes, "No data types reported.") })}
    ${card({ title: "Test precisions", sub: "constants/getAll precisions", accent: "purple", body: list(view.precisions, "No precisions reported.") })}
    ${card({ title: "Swap settings", sub: "constants/getAll swapTypes / tripleSwapOptions", accent: "orange", body: list([...view.swapTypes, ...view.tripleSwapOptions], "No swap options reported.") })}
  </div>`;
}

function renderModuleCard(module) {
  if (!module) return "";
  const rows = [
    ["Native module", module.module],
    ["Archive", module.source_relative_path || "none under user/projects"],
    ["SHA-256", module.archive_sha256 ? `${module.archive_sha256.slice(0, 16)}…` : "—"],
    ["Editing", "StrategyQuant X only — this desktop has no downloader or instrument editor"],
  ];
  return card({ title: "Native Data manager", sub: module.detail || "", accent: "neutral", body: statList(rows), className: "data-manager-module" });
}

export function renderDataManager(view) {
  if (view.state === "pending") {
    return unavailable("Reading installed data…", "Calling StrategyQuant X constants/getAll and main/getData through the canonical read model.", { tone: "pending", compact: true });
  }
  if (view.state === "unavailable") {
    return `<div class="stack" data-data-manager-state="unavailable">
      ${unavailable("StrategyQuant X is not running", `${view.detail || "The installed-data list comes from the running producer."} Open StrategyQuant X and reload; this desktop does not cache or invent a symbol list.`, { tone: "unavailable" })}
      ${renderModuleCard(view.module)}
    </div>`;
  }
  const kpis = `<div class="kpi-strip">${[
    kpi({ label: "Data series", value: String(view.symbols.length), tone: view.symbols.length ? "ready" : "unavailable", note: "main/getData rows" }),
    kpi({ label: "Sessions", value: String(view.sessions.length), tone: "neutral", note: "Data manager → Sessions" }),
    kpi({ label: "Data types", value: String(view.dataTypes.length), tone: "neutral", note: "constants/getAll" }),
    kpi({ label: "Precisions", value: String(view.precisions.length), tone: "neutral", note: "backtest precision modes" }),
  ].join("")}</div>`;
  const symbols = card({
    title: "Installed data",
    sub: view.detail || "Official StrategyQuant X installed data series",
    accent: "green",
    className: "span-all",
    body: `${renderSymbolsTable(view)}
      <div class="data-manager-range" data-data-range>${unavailable("Data range", "Select a series to draw its data/getSymbolData availability graph.", { compact: true })}</div>
      <p class="note">Add, download (Dukascopy, file, Darwinex, crypto, Yahoo, MT5 import), clone, or edit instruments in StrategyQuant X → Data manager. This desktop lists what the producer reports and never invents a series.</p>`,
  });
  return `<div class="stack" data-data-manager-state="ready">${kpis}${symbols}${renderLists(view)}${renderModuleCard(view.module)}</div>`;
}

async function drawRange(host, row) {
  const symbol = row.getAttribute("data-data-symbol") || "";
  const dateFrom = row.getAttribute("data-date-from") || "";
  const dateTo = row.getAttribute("data-date-to") || "";
  host.querySelectorAll("[data-data-symbol]").forEach((node) => node.setAttribute("aria-current", node === row ? "true" : "false"));
  const target = host.querySelector("[data-data-range]");
  if (!target) return;
  if (!dateFrom || !dateTo) {
    target.innerHTML = unavailable(`${symbol} data range`, "StrategyQuant X reported no date range for this series.", { compact: true });
    return;
  }
  target.innerHTML = unavailable(`${symbol} data range`, "Calling StrategyQuant X data/getSymbolData…", { compact: true, tone: "pending" });
  try {
    const payload = await fetchSymbolData(dateFrom, dateTo, symbol, "No Session");
    const values = payload.points.map((point) => Number(point[1])).filter(Number.isFinite);
    target.innerHTML = chartFrame({
      title: `${symbol} · ${dateFrom} → ${dateTo}`,
      height: 140,
      state: values.length > 1 ? "current" : "unavailable",
      detail: values.length > 1 ? "" : "StrategyQuant X data/getSymbolData returned no series.",
      series: values.length > 1 ? [{ values, tone: "cyan" }] : [],
      xLabels: [dateFrom, dateTo],
    });
  } catch (error) {
    target.innerHTML = unavailable(`${symbol} data range unavailable`, error instanceof Error ? error.message : "data/getSymbolData failed.", { compact: true, tone: "error" });
  }
}

export async function loadDataManager(host, { fetchImpl = globalThis.fetch } = {}) {
  host.innerHTML = renderDataManager(dataManagerViewModel());
  const [moduleResult, installedResult] = await Promise.allSettled([
    fetchSqxModule("Data manager", fetchImpl),
    fetchInstalledDataSymbols(fetchImpl),
  ]);
  if (!host.isConnected) return null;
  const view = dataManagerViewModel({
    module: moduleResult.status === "fulfilled" ? moduleResult.value : null,
    installed: installedResult.status === "fulfilled" ? installedResult.value : null,
    installedError: installedResult.status === "rejected"
      ? (installedResult.reason instanceof Error ? installedResult.reason.message : "Installed data could not be read.")
      : "",
  });
  host.innerHTML = renderDataManager(view);
  return view;
}

export function bindDataManager(host) {
  if (!host || host.dataset.dataManagerBound === "1") return;
  host.dataset.dataManagerBound = "1";
  host.addEventListener("click", (event) => {
    const row = event.target.closest("[data-data-symbol]");
    if (row && host.contains(row)) void drawRange(host, row);
  });
  host.addEventListener("keydown", (event) => {
    if (event.key !== "Enter" && event.key !== " ") return;
    const row = event.target.closest("[data-data-symbol]");
    if (row && host.contains(row)) {
      event.preventDefault();
      void drawRange(host, row);
    }
  });
  void loadDataManager(host);
}

if (typeof document !== "undefined") {
  const attach = () => {
    const host = document.querySelector(DATA_MANAGER_HOST_SELECTOR);
    if (host) bindDataManager(host);
  };
  new MutationObserver(attach).observe(document.documentElement, { childList: true, subtree: true });
  attach();
}
