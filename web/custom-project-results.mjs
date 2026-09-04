import {
  card,
  escapeHtml,
  readable,
  statList,
  table,
  unavailable,
} from "./ui.mjs";

const RESULTS_SCHEMA = "tc.sqx-custom-project-results.v1";
const RESULTS_API_PATH = "/api/sqx-project-results";
const SQX_BUILD = "144.2953";

function object(value) {
  return value && typeof value === "object" && !Array.isArray(value) ? value : null;
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

function optionalCount(value) {
  return value === null || value === undefined || (Number.isInteger(value) && value >= 0);
}

function digest(value) {
  return typeof value === "string" && /^[0-9a-f]{64}$/.test(value) ? value : "";
}

function optionalText(value) {
  return value === null || value === undefined || typeof value === "string";
}

function databankColumnFromPayload(item) {
  const column = object(item);
  if (
    !column
    || typeof column.class !== "string"
    || !column.class
    || typeof column.name !== "string"
    || !column.name
    || typeof column.format !== "string"
    || !column.format
    || typeof column.header !== "string"
    || !column.header
    || (column.sample_type !== null && column.sample_type !== undefined && !Number.isInteger(column.sample_type))
    || (column.background !== null && column.background !== undefined && column.background !== "oos" && column.background !== "isv")
    || (column.key !== null && column.key !== undefined && (typeof column.key !== "string" || !column.key))
  ) {
    throw new Error("Native Custom Project databank column is invalid");
  }
  const sample = column.sample_type ?? null;
  return {
    ...column,
    key: typeof column.key === "string" && column.key ? column.key : (Number.isInteger(sample) ? `${column.class}:${sample}` : column.class),
    background: column.background === "oos" || column.background === "isv" ? column.background : null,
  };
}

function databankViewFromPayload(item) {
  const view = object(item);
  if (!view) return null;
  if (typeof view.name !== "string" || !view.name || !Array.isArray(view.columns) || !view.columns.length) {
    throw new Error("Native Custom Project databank view is invalid");
  }
  return { ...view, columns: view.columns.map(databankColumnFromPayload) };
}

function databankRowFromPayload(item) {
  if (item === null || item === undefined) return null;
  const row = object(item);
  const cells = object(row?.cells);
  if (
    !row
    || !cells
    || typeof row.strategy_name !== "string"
    || !row.strategy_name
    || !optionalText(row.result_key)
    || (row.filters_result !== null && row.filters_result !== undefined && row.filters_result !== "PASSED" && row.filters_result !== "FAILED")
    || !optionalText(row.filters_reason)
    || !optionalText(row.symbol)
    || !optionalText(row.timeframe)
    || row.basis !== "sqx_results_group_sqstats"
  ) {
    throw new Error("Native Custom Project databank row is invalid");
  }
  return row;
}

function strategyFromPayload(item, project, bank) {
  const archive = object(item);
  const name = typeof archive?.archive === "string" ? archive.archive : "";
  const relative = `user/projects/${project}/databanks/${bank}/${name}`;
  if (!name || !name.toLowerCase().endsWith(".sqx") || archive.relative_path !== relative) {
    throw new Error("Native Custom Project strategy archive is invalid");
  }
  if (archive.databank_row !== undefined) {
    archive.databank_row = databankRowFromPayload(archive.databank_row);
  }
  if (archive.inspectable === true) {
    if (!digest(archive.archive_sha256) || typeof archive.native_version !== "string" || !archive.native_version) {
      throw new Error("Native Custom Project strategy archive is invalid");
    }
    return archive;
  }
  if (typeof archive.reason_code !== "string" || !archive.reason_code) {
    throw new Error("Native Custom Project strategy archive is invalid");
  }
  return archive;
}

export function customProjectResultsFromPayload(payload) {
  const results = object(payload);
  if (
    !results
    || results.schema !== RESULTS_SCHEMA
    || results.source_build !== SQX_BUILD
    || results.status !== "ready"
    || !optionalCount(results.databank_count)
    || !optionalCount(results.strategy_count)
    || !Array.isArray(results.projects)
    || (results.project !== null && results.project !== undefined && !projectName(results.project))
  ) {
    throw new Error("Native Custom Project results are invalid");
  }
  for (const project of results.projects) {
    const name = projectName(project?.name);
    if (
      !name
      || project.source_relative_path !== `user/projects/${name}/project.cfx`
      || !Number.isInteger(project.databank_count)
      || project.databank_count < 0
      || !Number.isInteger(project.strategy_count)
      || project.strategy_count < 0
      || !Array.isArray(project.databanks)
      || project.databanks.length !== project.databank_count
    ) {
      throw new Error("Native Custom Project results item is invalid");
    }
    let strategies = 0;
    for (const bank of project.databanks) {
      if (
        !projectName(bank?.name)
        || !Number.isInteger(bank.strategy_count)
        || bank.strategy_count < 0
        || !Array.isArray(bank.strategies)
        || bank.strategies.length !== bank.strategy_count
      ) {
        throw new Error("Native Custom Project databank is invalid");
      }
      if (bank.view !== undefined && bank.view !== null) databankViewFromPayload(bank.view);
      for (const archive of bank.strategies) strategyFromPayload(archive, name, bank.name);
      strategies += bank.strategy_count;
    }
    if (strategies !== project.strategy_count) {
      throw new Error("Native Custom Project results item is invalid");
    }
  }
  return results;
}

async function readJson(response) {
  try { return await response.json(); } catch { return null; }
}

export async function fetchCustomProjectResults(project = "", fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Native project results fetch is unavailable");
  const exact = project ? projectName(project) : "";
  if (project && !exact) throw new Error("Exact native project name is required");
  const path = exact
    ? `${RESULTS_API_PATH}?${new URLSearchParams({ project: exact }).toString()}`
    : RESULTS_API_PATH;
  const response = await fetchImpl(path, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || `Native project results request failed: ${response?.status ?? "unknown"}`);
  return customProjectResultsFromPayload(payload);
}

export function projectResultsOf(results, project) {
  const exact = projectName(project);
  if (!results || !exact) return null;
  return results.projects.find((item) => item.name === exact) || null;
}

export function renderProjectDatabankStats(results, project) {
  const item = projectResultsOf(results, project);
  const databanks = Number.isInteger(item?.databank_count) ? String(item.databank_count) : "—";
  const strategies = Number.isInteger(item?.strategy_count) ? String(item.strategy_count) : "—";
  return [
    ["Strategies generated", "—"],
    ["Rejected", "—"],
    ["Failed", "—"],
    ["Accepted", "—"],
    ["Passed", "—"],
    ["In databank", strategies],
    ["Databanks", databanks],
    ["Strategies per hour", "—"],
    ["Time per strategy", "—"],
    ["Project running time", "—"],
  ];
}

export function databankViewOf(results, project, databank) {
  const item = projectResultsOf(results, project);
  const bank = item?.databanks?.find((row) => row.name === databank);
  return bank?.view || item?.databanks?.[0]?.view || null;
}

function groupedNumber(value, decimals) {
  const abs = Math.abs(value);
  const fixed = abs.toFixed(decimals);
  const [whole, frac] = fixed.split(".");
  const grouped = whole.replace(/\B(?=(\d{3})+(?!\d))/g, " ");
  if (!frac || /^0+$/.test(frac)) return grouped;
  return `${grouped}.${frac.replace(/0+$/, "")}`;
}

export function formatDatabankCell(column, value) {
  if (value === null || value === undefined || value === "") return "—";
  const format = column?.format || "text";
  if (format === "integer") {
    return Number.isFinite(Number(value)) ? String(Math.trunc(Number(value))) : "—";
  }
  if (format === "decimal2") {
    return Number.isFinite(Number(value)) ? groupedNumber(Number(value), 2) : "—";
  }
  if (format === "percent") {
    return Number.isFinite(Number(value)) ? `${groupedNumber(Number(value), 2)} %` : "—";
  }
  if (format === "money" || format === "drawdown") {
    if (!Number.isFinite(Number(value))) return "—";
    const number = Number(value);
    const sign = number < 0 ? "-" : "";
    return `$ ${sign}${groupedNumber(number, 2)}`;
  }
  if (format === "filters") return String(value);
  if (format === "sparkline") return value === "sparkline" ? "" : "—";
  return String(value);
}

function cellTone(column, value) {
  const format = column?.format;
  if (format === "filters" && value === "PASSED") return "green";
  if (format === "filters" && value === "FAILED") return "red";
  if (format === "drawdown") return "red";
  if ((format === "money" || format === "decimal2" || format === "percent") && Number.isFinite(Number(value))) {
    if (Number(value) < 0) return "red";
    if (format === "money" && Number(value) > 0) return "blue";
  }
  return "";
}

function columnKey(column) {
  if (typeof column?.key === "string" && column.key) return column.key;
  if (Number.isInteger(column?.sample_type)) return `${column.class}:${column.sample_type}`;
  return column?.class || "";
}

function cellValue(row, column) {
  const cells = object(row?.cells) || {};
  const key = columnKey(column);
  if (Object.prototype.hasOwnProperty.call(cells, key)) return cells[key];
  return cells[column.class];
}

function sparkFor(column, row) {
  if (column?.sample_type === 20) return row?.mini_equity_oos;
  return row?.mini_equity;
}

function sampleBandRects(ranges, className, width, height, count) {
  if (!Array.isArray(ranges) || count < 2) return "";
  return ranges.map((range) => {
    if (!Array.isArray(range) || range.length !== 2) return "";
    const start = Number(range[0]);
    const end = Number(range[1]);
    if (!Number.isInteger(start) || !Number.isInteger(end) || start < 0 || end < start) return "";
    const x = (start / count) * width;
    const x2 = (Math.min(end + 1, count) / count) * width;
    const bandWidth = Math.max(x2 - x, 0);
    if (bandWidth <= 0) return "";
    return `<rect class="${className}" x="${x.toFixed(2)}" y="0" width="${bandWidth.toFixed(2)}" height="${height}"/>`;
  }).join("");
}

function miniEquitySvg(spark) {
  const values = Array.isArray(spark?.values) ? spark.values.map(Number).filter(Number.isFinite) : [];
  if (values.length < 2) return `<span class="sqx-mini-equity is-empty">—</span>`;
  const width = 96;
  const height = 28;
  const zero = Number.isFinite(Number(spark.zero_point)) ? Number(spark.zero_point) : 0;
  const min = Math.min(...values, zero);
  const max = Math.max(...values, zero);
  const span = max - min || 1;
  const lastIndex = values.length - 1;
  const point = (value, index) => {
    const x = (index / lastIndex) * width;
    const y = height - ((value - min) / span) * (height - 2) - 1;
    return `${x.toFixed(2)},${y.toFixed(2)}`;
  };
  const line = values.map((value, index) => point(value, index)).join(" ");
  const zeroY = height - ((zero - min) / span) * (height - 2) - 1;
  const above = `${point(zero, 0)} ${values.map((value, index) => point(Math.max(value, zero), index)).join(" ")} ${point(zero, lastIndex)}`;
  const below = `${point(zero, 0)} ${values.map((value, index) => point(Math.min(value, zero), index)).join(" ")} ${point(zero, lastIndex)}`;
  const oos = sampleBandRects(spark.oos, "sqx-mini-equity-oos", width, height, values.length);
  const isv = sampleBandRects(spark.isv, "sqx-mini-equity-isv", width, height, values.length);
  return `<svg class="sqx-mini-equity" viewBox="0 0 ${width} ${height}" width="${width}" height="${height}" aria-hidden="true">
    ${oos}${isv}
    <polygon class="sqx-mini-equity-up" points="${above}"/>
    <polygon class="sqx-mini-equity-down" points="${below}"/>
    <polyline class="sqx-mini-equity-line" points="${line}"/>
    <line class="sqx-mini-equity-zero" x1="0" y1="${zeroY.toFixed(2)}" x2="${width}" y2="${zeroY.toFixed(2)}"/>
  </svg>`;
}

function filtersCell(row) {
  const status = row?.filters_result;
  if (status === "PASSED") {
    return `<span class="sqx-filters-result is-passed" title="PASSED">${escapeHtml("PASSED")}</span>`;
  }
  if (status === "FAILED") {
    const reason = row.filters_reason ? ` title="${escapeHtml(row.filters_reason)}"` : "";
    return `<span class="sqx-filters-result is-failed"${reason}>${escapeHtml("FAILED")}</span>`;
  }
  return "—";
}

function renderGridCell(column, archive, row) {
  if (column.class === "ResultsName") {
    return escapeHtml(row?.strategy_name || archive.archive.replace(/\.sqx$/i, ""));
  }
  if (column.class === "FiltersResult") return filtersCell(row);
  if (column.format === "sparkline") return miniEquitySvg(sparkFor(column, row));
  const value = cellValue(row, column);
  const tone = cellTone(column, value);
  const text = formatDatabankCell(column, value);
  return tone ? `<span class="tone-text-${escapeHtml(tone)}">${escapeHtml(text)}</span>` : escapeHtml(text);
}

function archiveRows(item, archiveHref, selectedDatabank = "", selectedArchive = "") {
  if (!item?.databanks?.length) return [];
  const rows = [];
  for (const bank of item.databanks) {
    if (!bank.strategies.length) {
      rows.push({ cells: [escapeHtml(bank.name), "—", "Empty databank"] });
      continue;
    }
    for (const archive of bank.strategies) {
      const identity = archive.inspectable
        ? `${archive.archive} · ${String(archive.archive_sha256).slice(0, 12)}…`
        : archive.archive;
      const state = archive.inspectable ? "Inspectable" : readable(archive.reason_code, "Unread");
      const href = archive.inspectable && typeof archiveHref === "function" ? archiveHref(bank.name, archive.archive) : "";
      const selected = bank.name === selectedDatabank && archive.archive === selectedArchive;
      const label = href
        ? `<a class="workflow-link" href="${escapeHtml(href)}" data-route="${escapeHtml(href)}" data-automation-databank="${escapeHtml(bank.name)}" data-automation-archive="${escapeHtml(archive.archive)}">${escapeHtml(identity)}</a>`
        : escapeHtml(identity);
      const attrs = [
        archive.inspectable ? `data-archive-inspectable="${escapeHtml(archive.archive)}"` : "",
        selected ? 'class="is-selected"' : "",
      ].filter(Boolean).join(" ");
      rows.push({
        cells: [escapeHtml(bank.name), label, escapeHtml(state)],
        attrs,
      });
    }
  }
  return rows;
}

function renderDatabankGrid(bank, { archiveHref, selectedDatabank = "", selectedArchive = "" } = {}) {
  const view = bank.view && Array.isArray(bank.view.columns) && bank.view.columns.length
    ? bank.view
    : null;
  if (!view) {
    return table({
      columns: [{ label: "Databank" }, { label: "Archive" }, { label: "State" }],
      rows: archiveRows({ databanks: [bank] }, archiveHref, selectedDatabank, selectedArchive),
    });
  }
  const head = [
    `<th class="sqx-databank-check"></th>`,
    ...view.columns.map((column) => {
      const shade = column.background === "oos" || column.background === "isv" ? ` class="background-${escapeHtml(column.background)}"` : "";
      return `<th${shade} title="${escapeHtml(column.header)}">${escapeHtml(column.header)}</th>`;
    }),
  ].join("");
  const body = bank.strategies.length
    ? bank.strategies.map((archive) => {
      const row = archive.databank_row || null;
      const selected = bank.name === selectedDatabank && archive.archive === selectedArchive;
      const href = archive.inspectable && typeof archiveHref === "function" ? archiveHref(bank.name, archive.archive) : "";
      const nameCell = view.columns[0];
      const nameHtml = href
        ? `<a class="workflow-link" href="${escapeHtml(href)}" data-route="${escapeHtml(href)}" data-automation-databank="${escapeHtml(bank.name)}" data-automation-archive="${escapeHtml(archive.archive)}">${renderGridCell(nameCell, archive, row)}</a>`
        : renderGridCell(nameCell, archive, row);
      const cells = view.columns.map((column, index) => {
        const html = index === 0 ? nameHtml : renderGridCell(column, archive, row);
        const shade = column.background === "oos" || column.background === "isv" ? ` background-${escapeHtml(column.background)}` : "";
        return `<td class="sqx-cell-${escapeHtml(column.format)}${shade}">${html}</td>`;
      }).join("");
      const attrs = [
        archive.inspectable ? `data-archive-inspectable="${escapeHtml(archive.archive)}"` : "",
        `data-automation-databank="${escapeHtml(bank.name)}"`,
        `data-automation-archive="${escapeHtml(archive.archive)}"`,
        selected ? 'class="is-selected"' : "",
      ].filter(Boolean).join(" ");
      return `<tr ${attrs}><td class="sqx-databank-check"><input type="checkbox" tabindex="-1" ${selected ? "checked" : ""} aria-label="${escapeHtml(archive.archive)}"></td>${cells}</tr>`;
    }).join("")
    : `<tr class="table-empty"><td colspan="${view.columns.length + 1}">Empty databank</td></tr>`;
  return `<div class="table-wrap sqx-databank-table" data-databank-name="${escapeHtml(bank.name)}" data-databank-view="${escapeHtml(view.name)}">
    <table class="data-table">
      <thead><tr>${head}</tr></thead>
      <tbody>${body}</tbody>
    </table>
  </div>`;
}

export function renderProjectDatabankList(results, project, { archiveHref, selectedDatabank = "", selectedArchive = "" } = {}) {
  const item = projectResultsOf(results, project);
  if (!item) {
    return unavailable(
      "Native databanks unread",
      "This desktop lists producer archives from user/projects when the verified runtime can be read. Generated, rejected, and accepted counts stay dashes until a native Custom Project run writes them.",
      { compact: true },
    );
  }
  if (!item.databanks.length) {
    return unavailable(
      "No databanks in this project yet",
      "Native Custom Project databanks appear here when StrategyQuant X writes archives under user/projects. This desktop does not invent strategy rows.",
      { compact: true },
    );
  }
  return item.databanks.map((bank) => renderDatabankGrid(bank, { archiveHref, selectedDatabank, selectedArchive })).join("");
}

export function renderNativeArchivesCard(results = null, error = "") {
  if (error) {
    return card({
      title: "Native Custom Project archives",
      sub: "Producer databanks from saved Custom Projects",
      headIcon: "layers",
      accent: "orange",
      className: "span-all",
      attrs: 'data-validate-native-archives="error"',
      body: unavailable("Native archives unavailable", error, { compact: true, tone: "error" }),
    });
  }
  if (!results) {
    return card({
      title: "Native Custom Project archives",
      sub: "Producer databanks from saved Custom Projects",
      headIcon: "layers",
      accent: "orange",
      className: "span-all",
      attrs: 'data-validate-native-archives="loading"',
      body: unavailable("Reading native databanks…", "Listing producer archives from the verified StrategyQuant X home.", { tone: "pending", compact: true }),
    });
  }
  const rows = [];
  for (const project of results.projects) {
    for (const row of archiveRows(project)) {
      rows.push({ cells: [escapeHtml(project.name), ...row.cells], attrs: row.attrs || "" });
    }
  }
  const body = rows.length
    ? `${statList([["Projects", String(results.projects.length)], ["Databanks", String(results.databank_count)], ["Strategy archives", String(results.strategy_count)]])}${table({
      columns: [{ label: "Project" }, { label: "Databank" }, { label: "Archive" }, { label: "State" }],
      rows,
    })}<p class="note">These are native producer files. They are not Historical Results until custody bind. TradingView and MetaTrader MCP are Apollo tools and do not feed this table.</p>`
    : unavailable(
      "No Custom Project archives yet",
      "Verified StrategyQuant X has no .sqx files under user/projects/*/databanks. This surface does not invent funnel counts from missing archives.",
      { compact: true },
    );
  return card({
    title: "Native Custom Project archives",
    sub: "Producer databanks from saved Custom Projects — not Apollo TradingView or MetaTrader tools",
    headIcon: "layers",
    accent: "orange",
    className: "span-all",
    attrs: 'data-validate-native-archives="loaded"',
    body,
  });
}
