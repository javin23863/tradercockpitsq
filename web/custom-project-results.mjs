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
  const association = archive.candidate_association;
  if (association != null && (association.schema !== "tc.research-native-candidate-association.v1"
    || !/^tc-research:candidate:v1:[0-9a-f-]{36}$/.test(association.candidate_entity_id || "")
    || !/^tc-research-revision:candidate:sha256:[0-9a-f]{64}$/.test(association.candidate_revision || "")
    || !/^tc-research-revision:candidate-membership:sha256:[0-9a-f]{64}$/.test(association.membership_revision || "")
    || association.archive_sha256 !== archive.archive_sha256 || archive.inspectable !== true)) {
    throw new Error("Candidate association does not match this native archive");
  }
  const reconciliation = archive.candidate_reconciliation;
  if (reconciliation != null && (association != null || reconciliation.schema !== "tc.research-native-candidate-reconciliation.v1"
    || !candidateIdentity(reconciliation) || !digest(reconciliation.previous_archive_sha256)
    || (reconciliation.unavailable_reason != null && !["candidate_legacy_reimport_required", "candidate_token_invalid", "candidate_archive_invalid"].includes(reconciliation.unavailable_reason))
    || reconciliation.previous_archive_sha256 === archive.archive_sha256
    || reconciliation.archive_sha256 !== archive.archive_sha256 || archive.inspectable !== true)) {
    throw new Error("Candidate reconciliation does not match this native archive");
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
  if (results.import_recovery !== undefined) {
    const recovery = results.import_recovery;
    if (!object(recovery) || !["ready", "unavailable"].includes(recovery.status) || !Array.isArray(recovery.operations)
        || (recovery.status === "unavailable" && (recovery.operations.length || typeof recovery.detail !== "string"))) throw new Error("Import recovery response is invalid");
    const ids = new Set();
    for (const row of recovery.operations) {
      if (!object(row) || Object.keys(row).some(key => !["action", "target", "discard_preview_sha256"].includes(key))
          || row.action !== "load" || !object(row.target) || Object.keys(row.target).sort().join(",") !== "archive,databank,operation_id,project,source_sha256"
          || !["project", "databank", "archive"].every(key => projectName(row.target[key])) || !row.target.archive.toLowerCase().endsWith(".sqx")
          || !operationId(row.target.operation_id) || !digest(row.target.source_sha256)
          || (results.project && row.target.project !== results.project)
          || (row.discard_preview_sha256 !== undefined && !digest(row.discard_preview_sha256)) || ids.has(row.target.operation_id)) throw new Error("Retained import identity is invalid");
      ids.add(row.target.operation_id);
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

function columnSample(column) {
  if (column.sample_type >= 10 && column.sample_type <= 19) return "is";
  if (column.sample_type >= 20 && column.sample_type <= 30) return "oos";
  return "";
}

function renderDatabankGrid(bank, { archiveHref, selectedDatabank = "", selectedArchive = "", checkedArchives = null, columns = "all" } = {}) {
  const view = bank.view && Array.isArray(bank.view.columns) && bank.view.columns.length
    ? bank.view
    : null;
  if (!view) {
    return table({
      columns: [{ label: "Databank" }, { label: "Archive" }, { label: "State" }],
      rows: archiveRows({ databanks: [bank] }, archiveHref, selectedDatabank, selectedArchive),
    });
  }
  const visibleColumns = view.columns.filter(column => columns === "all" || !columnSample(column) || columnSample(column) === columns);
  const head = [
    `<th class="sqx-databank-check"></th>`,
    ...visibleColumns.map((column) => {
      const shade = column.background === "oos" || column.background === "isv" ? ` class="background-${escapeHtml(column.background)}"` : "";
      return `<th${shade} data-column-sample="${columnSample(column)}" title="${escapeHtml(column.header)}">${escapeHtml(column.header)}</th>`;
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
      const cells = visibleColumns.map((column, index) => {
        const html = index === 0 ? nameHtml : renderGridCell(column, archive, row);
        const shade = column.background === "oos" || column.background === "isv" ? ` background-${escapeHtml(column.background)}` : "";
        return `<td class="sqx-cell-${escapeHtml(column.format)}${shade}" data-column-sample="${columnSample(column)}">${html}</td>`;
      }).join("");
      const attrs = [
        archive.inspectable ? `data-archive-inspectable="${escapeHtml(archive.archive)}"` : "",
        `data-automation-databank="${escapeHtml(bank.name)}"`,
        `data-automation-archive="${escapeHtml(archive.archive)}"`,
        selected ? 'class="is-selected"' : "",
      ].filter(Boolean).join(" ");
      const checked = checkedArchives ? checkedArchives.includes(archive.archive) : selected;
      return `<tr ${attrs}><td class="sqx-databank-check"><input type="checkbox" ${checked ? "checked" : ""} aria-label="Select ${escapeHtml(archive.archive)}"></td>${cells}</tr>`;
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

const DATABANK_API = "/api/sqx-databank";
const MAX_ARCHIVE_BYTES = 16 * 1024 * 1024;
const evidenceRef = (value) => typeof value === "string" && /^tc-evidence:sha256:[0-9a-f]{64}$/.test(value);
const operationId = value => typeof value === "string" && /^[0-9a-f]{32}$/.test(value);
function candidateIdentity(value) {
  return /^tc-research:candidate:v1:[0-9a-f-]{36}$/.test(value?.candidate_entity_id || "")
    && /^tc-research-revision:candidate:sha256:[0-9a-f]{64}$/.test(value?.candidate_revision || "")
    && /^tc-research-revision:candidate-membership:sha256:[0-9a-f]{64}$/.test(value?.membership_revision || "");
}
async function bytesDigest(blob) {
  return Array.from(new Uint8Array(await crypto.subtle.digest("SHA-256", await blob.arrayBuffer())), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

// Keep uncertain requests across navigation/restart; discard only a verified receipt.
export async function retainDatabankOperation(action, target, storage = globalThis.localStorage, recovered = null) {
  const exact = { action, target: Object.fromEntries(Object.entries(target).sort(([a], [b]) => a.localeCompare(b))) };
  const key = `tc.databank-operation.${await bytesDigest(new Blob([JSON.stringify(exact)]))}`;
  const raw = storage.getItem(key);
  const existing = raw === null ? recovered && structuredClone(recovered) : JSON.parse(raw);
  if (existing && (existing.action !== action || !operationId(existing.target?.operation_id)
    || Object.keys(existing.target).length !== Object.keys(exact.target).length + 1
    || Object.entries(exact.target).some(([key, value]) => JSON.stringify(existing.target[key]) !== JSON.stringify(value)))) throw new Error("Saved databank operation is unreadable; no change was submitted.");
  if (existing && recovered) {
    if (existing.target.operation_id !== recovered.target.operation_id
        || (existing.discard_preview_sha256 && recovered.discard_preview_sha256 && existing.discard_preview_sha256 !== recovered.discard_preview_sha256)) throw new Error("Retained import recovery conflicts with this browser. Files were kept.");
    if (recovered.discard_preview_sha256) existing.discard_preview_sha256 = recovered.discard_preview_sha256;
  }
  const saved = existing || { action, target: { ...exact.target, operation_id: crypto.randomUUID().replaceAll("-", "") } };
  let encoded = JSON.stringify(saved);
  storage.setItem(key, encoded);
  if (storage.getItem(key) !== encoded) throw new Error("Cannot retain this operation safely; no change was submitted.");
  return { ...saved, confirmDiscard: (previewHash) => {
    if (action !== "load" || !digest(previewHash) || storage.getItem(key) !== encoded
      || (saved.discard_preview_sha256 && saved.discard_preview_sha256 !== previewHash)) throw new Error("Retained import deletion changed; no deletion was submitted.");
    saved.discard_preview_sha256 = previewHash;
    encoded = JSON.stringify(saved);
    storage.setItem(key, encoded);
    if (storage.getItem(key) !== encoded) throw new Error("Cannot retain this deletion safely; no deletion was submitted.");
  }, discardNotStarted: (previewHash) => {
    if (action !== "load" || saved.discard_preview_sha256 !== previewHash || storage.getItem(key) !== encoded) throw new Error("Retained deletion changed; refresh before retrying.");
    delete saved.discard_preview_sha256;
    encoded = JSON.stringify(saved);
    storage.setItem(key, encoded);
    if (storage.getItem(key) !== encoded) throw new Error("Cannot retain the refused deletion state; refresh before retrying.");
  }, completed: () => { if (storage.getItem(key) === encoded) storage.removeItem(key); } };
}

export function retainedDatabankOperations(project, storage = globalThis.localStorage, recovered = []) {
  const pending = [];
  for (let index = 0; index < (storage?.length || 0); index++) {
    const key = storage.key(index);
    if (!key?.startsWith("tc.databank-operation.")) continue;
    const entry = JSON.parse(storage.getItem(key));
    if (!["load", "reconcile", "rename", "copy", "move", "remove", "clear"].includes(entry?.action) || !operationId(entry.target?.operation_id)) throw new Error("A retained databank operation is unreadable.");
    if (entry.discard_preview_sha256 !== undefined && (entry.action !== "load" || !digest(entry.discard_preview_sha256))) throw new Error("A retained import deletion is unreadable.");
    if (entry.target.project === project) pending.push(entry);
  }
  for (const entry of recovered.filter(row => row.target.project === project)) {
    const existing = pending.find(row => row.target.operation_id === entry.target.operation_id);
    if (!existing) { pending.push(entry); continue; }
    if (existing.action !== entry.action || Object.keys(existing.target).length !== Object.keys(entry.target).length
        || Object.entries(entry.target).some(([key, value]) => existing.target[key] !== value)
        || (existing.discard_preview_sha256 && entry.discard_preview_sha256 && existing.discard_preview_sha256 !== entry.discard_preview_sha256)) throw new Error("Retained import recovery conflicts with this browser. Files were kept.");
    if (entry.discard_preview_sha256) existing.discard_preview_sha256 = entry.discard_preview_sha256;
  }
  return pending;
}

export async function databankBatchAction(action, target, fetchImpl = globalThis.fetch) {
  const fields = {
    snapshot: ["project", "databank"], clear: ["project", "databank", "snapshot_ref", "operation_id"],
    remove: ["project", "databank", "archives", "operation_id"], export: ["project", "databank", "archives"],
    copy: ["project", "databank", "archives", "target_project", "target_databank", "operation_id"],
    move: ["project", "databank", "archives", "target_project", "target_databank", "operation_id"],
  }[action];
  const validRow = (row) => object(row) && Object.keys(row).sort().join() === "archive,archive_sha256"
    && projectName(row.archive) && row.archive.toLowerCase().endsWith(".sqx") && digest(row.archive_sha256);
  if (!fields || !object(target) || Object.keys(target).sort().join() !== [...fields].sort().join()
    || !projectName(target.project) || !projectName(target.databank)
    || (fields.includes("operation_id") && !operationId(target.operation_id))
    || (fields.includes("archives") && (!Array.isArray(target.archives) || target.archives.length < 1 || target.archives.length > 100
      || !target.archives.every(validRow) || new Set(target.archives.map(row => row.archive.toLowerCase())).size !== target.archives.length))
    || (fields.includes("snapshot_ref") && !evidenceRef(target.snapshot_ref))
    || (fields.includes("target_project") && (!projectName(target.target_project) || !projectName(target.target_databank)))) {
    throw new Error("Choose an exact databank and up to 100 distinct strategies.");
  }
  const response = await fetchImpl(`${DATABANK_API}/${action}`, {
    method: "POST", headers: { "content-type": "application/json", accept: action === "export" ? "application/zip" : "application/json" }, body: JSON.stringify(target),
  });
  if (!response.ok) { const error = await readJson(response); throw new Error(error?.detail || `Databank action failed: ${response.status}`); }
  if (action === "export") {
    const blob = await response.blob();
    if (!digest(response.headers.get("X-Archive-Sha256")) || await bytesDigest(blob) !== response.headers.get("X-Archive-Sha256")) throw new Error("Downloaded collection failed its integrity check.");
    const selection = JSON.stringify({ archives: target.archives.map(({ archive, archive_sha256 }) => ({ archive, archive_sha256 })), databank: target.databank, project: target.project });
    if (response.headers.get("X-Selection-Sha256") !== await bytesDigest(new Blob([selection]))) throw new Error("Downloaded collection does not match the selected strategies.");
    return blob;
  }
  const payload = await readJson(response);
  if (action === "snapshot") {
    if (payload?.schema !== "tc.sqx-databank-snapshot.v1" || payload.project !== target.project || payload.databank !== target.databank
      || !evidenceRef(payload.snapshot_ref) || !Number.isInteger(payload.archive_count) || payload.archive_count < 0) throw new Error("Databank snapshot does not match this selection.");
  } else if (payload?.schema !== "tc.sqx-databank-action.v1" || payload.action !== action || payload.project !== target.project || payload.databank !== target.databank
    || payload.persisted !== true || payload.producer !== "sqx_local_web" || payload.operation_id !== target.operation_id
    || (action === "clear" ? payload.snapshot_ref !== target.snapshot_ref : JSON.stringify(payload.archives) !== JSON.stringify(target.archives))
    || !Array.isArray(payload.results) || !payload.results.every(validRow)
    || (["copy", "move"].includes(action)
      ? payload.results.length !== target.archives.length || payload.results.some((row, index) => row.archive !== target.archives[index].archive)
      : payload.results.length !== 0)
    || (["copy", "move"].includes(action) && (payload.target_project !== target.target_project || payload.target_databank !== target.target_databank))) {
    throw new Error("Databank response does not match this operation.");
  }
  return payload;
}

export async function candidatePurge(action, target, fetchImpl = globalThis.fetch) {
  const fields = action === "preview" ? ["candidate_entity_id"] : action === "confirm" ? ["candidate_entity_id", "expected_preview_sha256"] : [];
  if (!fields.length || !object(target) || Object.keys(target).sort().join() !== fields.sort().join()
    || !/^tc-research:candidate:v1:[0-9a-f-]{36}$/.test(target.candidate_entity_id || "")
    || (action === "confirm" && !digest(target.expected_preview_sha256))) throw new Error("Choose an exact saved candidate and deletion preview.");
  const response = await fetchImpl(`${DATABANK_API}/purge-${action}`, {
    method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify(target),
  });
  const payload = await readJson(response);
  if (!response.ok) throw new Error(payload?.detail || "Candidate deletion could not be completed.");
  return validateCandidatePurge(payload, action, target);
}

function validateCandidatePurge(payload, action, target, cancelImport = null) {
  if (payload?.schema !== "tc.research-candidate-purge.v1" || !digest(payload.intent_id)
    || payload.preview?.candidate_entity_id !== target.candidate_entity_id
    || (action === "confirm" && (payload.intent_id !== target.expected_preview_sha256 || payload.state !== "completed"))
    || (action === "confirm" && (!Number.isSafeInteger(payload.reclaimed_bytes) || payload.reclaimed_bytes < 0
      || payload.reclaimed_byte_measure !== "file_content_bytes" || !Array.isArray(payload.reclamation_uncertain_paths)))
    || (action === "preview" && !["preview", "prepared", "deleting"].includes(payload.state))
    || ["entities", "revisions", "artifacts", "shared_artifacts", "memberships", "staging", "mutation_journals"].some(key => !Array.isArray(payload.preview[key]))
    || payload.preview.mutation_journals.some(row => !object(row) || row.candidate_entity_id !== target.candidate_entity_id
      || !digest(row.sha256) || !digest(row.mutation_id) || row.path !== `databank-actions/${row.mutation_id}.json`
      || (!/^tc-research-revision:candidate:sha256:[a-f0-9]{64}$/.test(row.candidate_revision || "")
        && !(cancelImport && row.mutation_id === cancelImport.mutation_id && row.action === "load" && row.candidate_revision === null))
      || !["load", "rename", "copy", "move", "remove"].includes(row.action) || !object(row.source)
      || !projectName(row.source.project) || !projectName(row.source.databank)
      || !projectName(row.source.archive) || !row.source.archive.toLowerCase().endsWith(".sqx") || !digest(row.source.archive_sha256))
    || [...payload.preview.artifacts, ...payload.preview.shared_artifacts, ...payload.preview.staging, ...payload.preview.mutation_journals].some(row => !Number.isSafeInteger(row.bytes) || row.bytes < 0)) {
    throw new Error("Candidate deletion response does not match the selected candidate.");
  }
  return payload;
}

export async function importDiscard(action, target, fetchImpl = globalThis.fetch) {
  const fields = ["project", "databank", "archive", "source_sha256", "operation_id", ...(action === "confirm" ? ["expected_preview_sha256"] : [])];
  if (!["preview", "confirm"].includes(action) || !object(target) || Object.keys(target).sort().join() !== fields.sort().join()
    || !projectName(target.project) || !projectName(target.databank) || !projectName(target.archive) || !target.archive.toLowerCase().endsWith(".sqx")
    || !digest(target.source_sha256) || !operationId(target.operation_id)
    || (action === "confirm" && !digest(target.expected_preview_sha256))) throw new Error("Choose the exact retained import and deletion preview.");
  const response = await fetchImpl(`${DATABANK_API}/import-discard-${action}`, {
    method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify(target),
  });
  const payload = await readJson(response);
  if (!response.ok) {
    const error = new Error(payload?.detail || "Retained import deletion could not be completed.");
    // Only these explicit pre-intent refusals permit a new preview or load resume.
    // A transport failure or an interrupted purge keeps the confirmed deletion.
    error.discardNotStarted = action === "confirm" && response.status === 409 && payload?.error === "invalid_state"
      && ["databank_import_discard_preview_changed", "databank_import_submitted"].includes(payload.reason_code);
    throw error;
  }
  const binding = payload?.preview?.cancel_import;
  const { expected_preview_sha256, ...request } = target;
  if (!object(binding) || binding.native_disposition !== "not_submitted" || binding.phase !== "prepared"
    || !digest(binding.mutation_id) || !digest(binding.journal_sha256) || binding.operation_id !== target.operation_id
    || !object(binding.request) || Object.keys(binding.request).sort().join() !== Object.keys(request).sort().join()
    || Object.keys(request).some(key => binding.request[key] !== request[key])
    || !/^tc-research:candidate:v1:[0-9a-f-]{36}$/.test(payload.preview.candidate_entity_id || "")
    || !Array.isArray(payload.preview.memberships) || payload.preview.memberships.length !== 0
    || !Array.isArray(payload.preview.mutation_journals) || payload.preview.mutation_journals.length !== 1
    || payload.preview.mutation_journals[0].action !== "load"
    || payload.preview.mutation_journals[0].mutation_id !== binding.mutation_id
    || payload.preview.mutation_journals[0].sha256 !== binding.journal_sha256
    || ["project", "databank", "archive"].some(key => payload.preview.mutation_journals[0].source?.[key] !== request[key])
    || payload.preview.mutation_journals[0].source?.archive_sha256 !== request.source_sha256) throw new Error("Retained import deletion response does not match this request.");
  return validateCandidatePurge(payload, action, { candidate_entity_id: payload.preview.candidate_entity_id, expected_preview_sha256 }, binding);
}

export function renderCandidatePurge(payload) {
  const preview = payload.preview;
  const bytes = [...preview.artifacts, ...preview.staging, ...preview.mutation_journals].reduce((sum, row) => sum + row.bytes, 0);
  return `<p><strong>${preview.cancel_import ? "Discard this unfinished import and its retained files?" : "Delete this candidate and retained files?"}</strong></p>
    <p>${preview.memberships.length} native memberships, ${preview.revisions.length} retained revisions; ${(bytes / 1048576).toFixed(2)} MiB of retained files eligible for removal.</p>
    <ul>${preview.memberships.map(row => `<li>${escapeHtml(row.project)} / ${escapeHtml(row.databank)} / ${escapeHtml(row.archive)}</li>`).join("")}</ul>
    <p>${preview.shared_artifacts.length} shared artifacts will remain for other records. Original desktop imports and independently saved exports are kept. Deleted retained results cannot be reopened or reproduced from this storage.</p>`;
}

export async function databankAction(action, target, file = null, fetchImpl = globalThis.fetch) {
  const fields = { load: ["project", "databank", "archive", "source_sha256", "operation_id"],
    reconcile: ["project", "databank", "archive", "archive_sha256", "previous_archive_sha256", "candidate_entity_id", "candidate_revision", "membership_revision", "operation_id"],
    save: ["project", "databank", "archive", "archive_sha256"], rename: ["project", "databank", "archive", "archive_sha256", "new_name", "operation_id"], create: ["project", "databank"] }[action];
  if (!fields || !object(target) || Object.keys(target).sort().join() !== [...fields].sort().join()
    || !projectName(target.project) || !projectName(target.databank)
    || (fields.includes("archive") && (!projectName(target.archive) || !target.archive.toLowerCase().endsWith(".sqx")))
    || (fields.includes("archive_sha256") && !digest(target.archive_sha256))
    || (fields.includes("operation_id") && !operationId(target.operation_id))
    || (action === "load" && !digest(target.source_sha256))
    || (action === "reconcile" && (!candidateIdentity(target) || !digest(target.previous_archive_sha256) || target.previous_archive_sha256 === target.archive_sha256))
    || (action === "rename" && (!projectName(target.new_name) || target.new_name.toLowerCase().endsWith(".sqx")))) throw new Error("Choose an exact project, databank, and strategy before this action");
  const headers = { accept: action === "save" ? "application/octet-stream" : "application/json" };
  let body, sourceSha = action === "load" ? target.source_sha256 : null;
  if (action === "load" && file !== null) {
    if (!file || file.name !== target.archive || !Number.isInteger(file.size) || file.size < 1 || file.size > MAX_ARCHIVE_BYTES) throw new Error("Choose the exact nonempty .sqx file up to 16 MiB");
    if (await bytesDigest(file) !== sourceSha) throw new Error("Selected file does not match the retained import.");
    headers["content-type"] = "application/octet-stream";
    headers["X-TraderCockpit-Target"] = encodeURIComponent(JSON.stringify(target));
    body = file;
  } else {
    headers["content-type"] = "application/json";
    body = JSON.stringify(target);
  }
  const endpoint = action === "load" && file === null ? "load-resume" : action;
  const response = await fetchImpl(`${DATABANK_API}/${endpoint}`, { method: "POST", headers, body });
  if (!response.ok) {
    const error = await readJson(response);
    throw new Error(error?.detail || `Databank action failed: ${response.status}`);
  }
  if (action === "save") {
    const blob = await response.blob();
    if (response.headers.get("X-Archive-Sha256") !== target.archive_sha256 || await bytesDigest(blob) !== target.archive_sha256) throw new Error("Downloaded strategy does not match the selected archive");
    return blob;
  }
  const payload = await readJson(response);
  const expectedArchive = action === "create" ? null : action === "rename" ? `${target.new_name}.sqx` : target.archive;
  if (payload?.schema !== "tc.sqx-databank-action.v1" || payload.action !== action || payload.project !== target.project || payload.databank !== target.databank
    || payload.archive !== expectedArchive || (action === "create" ? payload.archive_sha256 !== null : !digest(payload.archive_sha256))
    || (fields.includes("operation_id") && payload.operation_id !== target.operation_id)
    || (["load", "reconcile"].includes(action) && !candidateIdentity(payload))
    || (action === "reconcile" && (payload.archive_sha256 !== target.archive_sha256
      || payload.candidate_entity_id !== target.candidate_entity_id || payload.candidate_revision !== target.candidate_revision
      || payload.membership_revision === target.membership_revision))
    || payload.source_sha256 !== sourceSha || payload.producer !== "sqx_local_web" || payload.persisted !== true) throw new Error("Databank response does not match this operation");
  return payload;
}

export function renderDatabankDock(results, project, { projects = null, databank = "", archive = "", checkedArchives = null, columns = "all", busy = false, error = "", notice = "", purgePreview = null, pendingOperations = [] } = {}) {
  const item = projectResultsOf(results, project);
  const bank = item?.databanks?.find((row) => row.name === databank) || item?.databanks?.[0];
  const selected = bank?.strategies.find((row) => row.archive === archive);
  const target = selected?.inspectable && digest(selected.archive_sha256);
  const checked = checkedArchives || (selected ? [selected.archive] : []);
  const count = bank?.strategies.filter(row => checked.includes(row.archive)).length || 0;
  return `<details class="sqx-databank-dock" data-databank-dock open><summary>Databanks${project ? ` · ${escapeHtml(project)}` : ""}<button type="button" class="results-dock-expand" data-results-dock-expand aria-pressed="false">Expand table</button></summary><div class="sqx-databank-body">
    ${projects ? `<label>Project for databanks<select data-dock-project ${busy ? "disabled" : ""}><option value="">Choose a project</option>${projects.map((name) => `<option value="${escapeHtml(name)}" ${name === project ? "selected" : ""}>${escapeHtml(name)}</option>`).join("")}</select></label>` : ""}
    ${!project ? '<p class="note">Choose a project to view its databanks. Strategies from different projects are kept separate.</p>' : `<div class="sqx-databank-toolbar" data-results-databank-toolbar>
      <div class="sqx-databank-toolbar-actions">${bank ? `<label class="button button-small">Load .sqx<input type="file" data-dock-load accept=".sqx" ${busy ? "disabled" : ""}></label>` : ""}
      <button class="button button-small" type="button" data-dock-open ${!target ? "hidden" : ""} ${busy ? "disabled" : ""}>Open results</button><button class="button button-small" type="button" data-dock-save ${!target ? "hidden" : ""} ${busy ? "disabled" : ""}>Save .sqx</button><button class="button button-small" type="button" data-dock-rename ${!target ? "hidden" : ""} ${busy ? "disabled" : ""}>Rename strategy</button>
      <button class="button button-small" type="button" data-dock-new ${busy ? "disabled" : ""}>New databank</button><button class="button button-small" type="button" data-dock-refresh ${busy ? "disabled" : ""}>Refresh</button>
      <button class="button button-small" type="button" data-dock-batch="export" ${busy || !count ? "disabled" : ""}>Save selected</button>
      <button class="button button-small" type="button" data-dock-batch="copy" ${busy || !count ? "disabled" : ""}>Copy selected</button>
      <button class="button button-small" type="button" data-dock-batch="move" ${busy || !count ? "disabled" : ""}>Move selected</button>
      <button class="button button-small" type="button" data-dock-batch="remove" ${busy || !count ? "disabled" : ""}>Remove selected</button>
      <button class="button button-small" type="button" data-dock-batch="clear" ${busy || !bank?.strategy_count ? "disabled" : ""}>Clear databank</button></div>
      <button class="button button-small" type="button" data-dock-purge ${selected?.candidate_association ? "" : "hidden"} ${busy ? "disabled" : ""}>Delete candidate and retained files…</button>
      <span data-dock-reconcile-context ${selected?.candidate_reconciliation ? "" : "hidden"}><button class="button button-small" type="button" data-dock-reconcile ${busy || selected?.candidate_reconciliation?.unavailable_reason ? "disabled" : ""}>Reconnect saved candidate</button><span class="note" data-dock-reconcile-note>${escapeHtml(reconciliationNote(selected?.candidate_reconciliation))}</span></span>
</div>
      <div class="sqx-databank-navigation"><div class="workflow-tabs" aria-label="Databanks">${(item?.databanks || []).map((row) => `<button class="workflow-tab ${row.name === bank?.name ? "is-current" : ""}" type="button" data-dock-bank="${escapeHtml(row.name)}" aria-pressed="${row.name === bank?.name}" ${busy ? "disabled" : ""}>${escapeHtml(row.name)}</button>`).join("")}</div>      <div class="sqx-databank-toolbar-meta"><span data-dock-records>Records: ${bank?.strategy_count ?? "—"} (Selected: ${count})</span><label title="View: ${escapeHtml(bank?.view?.name || 'Default - Main data')} · native samples, both directions">Columns <select data-dock-columns ${busy ? "disabled" : ""}>${[["all", "IS + OOS"], ["is", "In-sample (IS)"], ["oos", "Out-of-sample (OOS)"]].map(([value, label]) => `<option value="${value}" ${columns === value ? "selected" : ""}>${label}</option>`).join("")}</select></label></div></div>
      <div class="sqx-databank-grid" tabindex="0" role="region" aria-label="Strategy databank — scroll rows and columns">${bank ? renderDatabankGrid(bank, { archiveHref: () => "#", selectedDatabank: bank.name, selectedArchive: archive, checkedArchives: checked, columns }) : unavailable("Databank not loaded", "Create a databank or choose another project.", { compact: true })}</div>
      <p class="note">Use checkboxes for bulk actions. Double-click a row to open results. Removing from a bank keeps retained candidate history.</p>
      <form data-dock-purge-form ${purgePreview ? "" : "hidden"}>${purgePreview ? renderCandidatePurge(purgePreview) : ""}<button class="button button-small" type="submit" ${busy ? "disabled" : ""}>${purgePreview?.preview.cancel_import ? "Discard retained import" : "Delete candidate and retained files"}</button><button class="button button-small" type="button" data-dock-purge-cancel ${busy ? "disabled" : ""}>Cancel</button></form>
      <form data-dock-batch-form hidden><p data-dock-batch-summary></p><label data-dock-destination>Destination databank<select name="target_databank">${(item?.databanks || []).filter(row => row.name !== bank?.name).map(row => `<option value="${escapeHtml(row.name)}">${escapeHtml(row.name)}</option>`).join("")}</select></label><button class="button button-small" type="submit">Confirm</button><button class="button button-small" type="button" data-dock-batch-cancel>Cancel</button></form>
      <form data-dock-name-form hidden><label data-dock-name-label>Name<input name="name" type="text" required maxlength="120"></label><button class="button button-small" type="submit">Confirm</button><button class="button button-small" type="button" data-dock-cancel>Cancel</button></form>`}
    ${pendingOperations.length ? `<div><p>These operations have no verified response. Retry uses the originally confirmed files and snapshot.</p>${pendingOperations.map(operation => `<button class="button button-small" type="button" data-dock-retry="${escapeHtml(operation.target.operation_id)}" ${busy ? "disabled" : ""}>Retry ${operation.discard_preview_sha256 ? "import deletion" : escapeHtml(operation.action)} · ${escapeHtml(operation.target.databank)}${operation.target.archive ? ` · ${escapeHtml(operation.target.archive)}` : ""}</button>${operation.action === "load" && !operation.discard_preview_sha256 ? `<button class="button button-small" type="button" data-dock-discard="${escapeHtml(operation.target.operation_id)}" ${busy ? "disabled" : ""}>Discard retained import…</button>` : ""}`).join("")}</div>` : ""}
    <p class="idea-save-status" role="status" aria-live="polite" data-dock-status>${escapeHtml(error || notice || (busy ? "Reading native databank…" : ""))}</p>
    </div></details>`;
}

function reconciliationNote(hint) {
  return hint?.unavailable_reason
    ? "This saved candidate cannot be reconnected because its identity marker is missing or unreadable. Save .sqx, then Load .sqx into a different databank to import a new candidate. Existing candidate history stays separate; native compatibility is checked during import."
    : "Verifies saved storage identity; does not run or validate the strategy.";
}

export function bindDatabankDock(root, initial, { fetchImpl = globalThis.fetch, onSelect = () => {}, onOpen = () => {}, onChanged = () => {} } = {}) {
  let dock = root?.querySelector?.("[data-databank-dock]");
  if (!dock) return;
  const state = { busy: false, error: "", notice: "", ...initial };
  state.checkedArchives = initial.checkedArchives || (initial.archive ? [initial.archive] : []);
  let generation = 0, nameAction = "";
  let batchAction = "", batchTarget = null;
  function cancelForms() {
    nameAction = ""; batchTarget = null; state.purgePreview = null;
    for (const form of dock.querySelectorAll("[data-dock-name-form], [data-dock-batch-form], [data-dock-purge-form]")) form.hidden = true;
  }
  const current = () => root.isConnected && dock.isConnected;
  const selectedBank = () => projectResultsOf(state.results, state.project)?.databanks.find((row) => row.name === state.databank) || projectResultsOf(state.results, state.project)?.databanks[0];
  const selection = () => selectedBank()?.strategies.find((row) => row.archive === state.archive);
  const select = () => { state.databank = selectedBank()?.name || state.databank || ""; onSelect(state.project, state.databank, state.archive || ""); };
  const checkedRows = () => (selectedBank()?.strategies || []).filter(row => state.checkedArchives.includes(row.archive))
    .map(row => ({ archive: row.archive, archive_sha256: row.archive_sha256 }));
  function download(blob, name) {
    const url = URL.createObjectURL(blob);
    try { const link = root.ownerDocument.createElement("a"); link.href = url; link.download = name; link.click(); }
    finally { setTimeout(() => URL.revokeObjectURL(url), 1000); }
  }
  function paintSelection() {
    for (const item of dock.querySelectorAll("tr[data-automation-archive]")) {
      const name = item.getAttribute("data-automation-archive");
      item.classList.toggle("is-selected", name === state.archive);
      const check = item.querySelector("input"); if (check) check.checked = state.checkedArchives.includes(name);
    }
    for (const button of dock.querySelectorAll("[data-dock-open], [data-dock-save], [data-dock-rename]")) button.hidden = !selection()?.inspectable;
    const purge = dock.querySelector("[data-dock-purge]"); if (purge) purge.hidden = !selection()?.candidate_association;
    const reconnect = dock.querySelector("[data-dock-reconcile-context]"); if (reconnect) reconnect.hidden = !selection()?.candidate_reconciliation;
    if (reconnect) {
      reconnect.querySelector("button").disabled = state.busy || Boolean(selection()?.candidate_reconciliation?.unavailable_reason);
      reconnect.querySelector("[data-dock-reconcile-note]").textContent = reconciliationNote(selection()?.candidate_reconciliation);
    }
    for (const button of dock.querySelectorAll("[data-dock-batch]:not([data-dock-batch='clear'])")) button.disabled = state.busy || !checkedRows().length;
    dock.querySelector("[data-dock-records]").textContent = `Records: ${selectedBank().strategy_count} (Selected: ${checkedRows().length})`;
  }
  function render() {
    if (!current()) return;
    try {
      state.pendingOperations = retainedDatabankOperations(state.project, globalThis.localStorage, state.results?.import_recovery?.operations || []);
      if (state.results?.import_recovery?.status === "unavailable") state.error = state.results.import_recovery.detail;
    }
    catch (error) { state.error = error.message; state.pendingOperations = []; }
    const open = dock.open;
    const wrap = root.ownerDocument.createElement("div");
    wrap.innerHTML = renderDatabankDock(state.results, state.project, state);
    const next = wrap.firstElementChild;
    next.open = open;
    next.classList.toggle("is-expanded", dock.classList.contains("is-expanded"));
    const expand = next.querySelector("[data-results-dock-expand]");
    if (expand && next.classList.contains("is-expanded")) { expand.setAttribute("aria-pressed", "true"); expand.textContent = "Compact table"; }
    dock.replaceWith(next);
    dock = next;
    bind();
  }
  async function readProject(project, refresh = false) {
    if (state.busy || (project && !refresh && !state.projects?.includes(project))) return;
    cancelForms();
    const revision = ++generation;
    Object.assign(state, { project, ...(!refresh ? { databank: "", archive: "", checkedArchives: [], results: null } : {}), busy: true, error: "", notice: "" });
    render();
    try { const results = project ? await fetchCustomProjectResults(project, fetchImpl) : null; if (!current() || revision !== generation) return; state.results = results; }
    catch (error) { if (!current() || revision !== generation) return; state.error = error.message; }
    if (!current() || revision !== generation) return;
    state.busy = false; select(); render();
    if (refresh) onChanged("refresh", state.results);
  }
  async function act(action, file = null, name = "") {
    if (state.busy || !state.project) return;
    const bank = selectedBank(), archive = selection();
    const target = { project: state.project, databank: action === "create" ? name : bank?.name };
    if (action === "load") target.archive = file?.name;
    if (action === "save" || action === "rename") Object.assign(target, { archive: archive?.archive, archive_sha256: archive?.archive_sha256 });
    if (action === "rename") target.new_name = name;
    if (action === "reconcile") {
      const hint = archive?.candidate_reconciliation;
      if (!hint || hint.unavailable_reason) return;
      Object.assign(target, { archive: archive.archive, archive_sha256: hint.archive_sha256,
        previous_archive_sha256: hint.previous_archive_sha256, candidate_entity_id: hint.candidate_entity_id,
        candidate_revision: hint.candidate_revision, membership_revision: hint.membership_revision });
    }
    const revision = ++generation;
    Object.assign(state, { busy: true, error: "", notice: "" }); render();
    try {
      if (action === "load") {
        if (!file || file.size < 1 || file.size > MAX_ARCHIVE_BYTES) throw new Error("Choose a nonempty .sqx file up to 16 MiB");
        target.source_sha256 = await bytesDigest(file);
        if (!current() || revision !== generation) return;
      }
      const recovered = state.pendingOperations?.find(row => row.action === action && Object.entries(target).every(([key, value]) => row.target[key] === value));
      const operation = ["load", "rename", "reconcile"].includes(action) ? await retainDatabankOperation(action, target, undefined, recovered) : null;
      if (operation?.discard_preview_sha256) throw new Error("Finish the retained import deletion before importing this file again.");
      const result = await databankAction(action, operation?.target || target, file, fetchImpl);
      operation?.completed();
      if (!current() || revision !== generation) return;
      if (action === "save") {
        download(result, target.archive);
        state.notice = "Strategy prepared for saving.";
      } else {
        state.notice = "Native change saved. Refreshing databank…";
        const results = await fetchCustomProjectResults(state.project, fetchImpl);
        if (!current() || revision !== generation) return;
        Object.assign(state, { results, databank: result.databank, archive: result.archive || "", checkedArchives: result.archive ? [result.archive] : [], notice: action === "reconcile" ? "Saved candidate reconnected. Storage identity verified; no strategy execution or validation performed." : "Native change saved." });
        select();
        onChanged(action, state.results);
      }
    } catch (error) { if (!current() || revision !== generation) return; state.error = error.message; }
    if (!current() || revision !== generation) return;
    state.busy = false; render();
  }
  async function prepareBatch(action) {
    if (state.busy) return;
    batchAction = action;
    batchTarget = { project: state.project, databank: selectedBank()?.name, archives: checkedRows() };
    if (action === "export") { await executeBatch(); return; }
    let count = batchTarget.archives.length;
    if (action === "clear") {
      const revision = ++generation;
      state.busy = true; state.error = ""; render();
      try {
        const snapshot = await databankBatchAction("snapshot", { project: state.project, databank: selectedBank()?.name }, fetchImpl);
        if (!current() || revision !== generation) return;
        count = snapshot.archive_count;
        batchTarget = { project: state.project, databank: snapshot.databank, snapshot_ref: snapshot.snapshot_ref };
      } catch (error) {
        if (!current() || revision !== generation) return;
        state.busy = false; state.error = error.message; batchTarget = null; render(); return;
      }
      state.busy = false; render();
    }
    const form = dock.querySelector("[data-dock-batch-form]");
    const transfer = ["copy", "move"].includes(action);
    if (transfer && !(projectResultsOf(state.results, state.project)?.databanks || []).some(row => row.name !== selectedBank()?.name)) {
      state.error = "Create a destination databank first."; batchTarget = null; render(); return;
    }
    form.querySelector("[data-dock-destination]").hidden = !transfer;
    form.querySelector("[data-dock-batch-summary]").textContent = transfer
      ? `${action === "copy" ? "Copy" : "Move"} ${count} selected strategies to another databank in ${state.project}.`
      : `Remove ${count} strategies from ${batchTarget.databank}? Retained candidate history and other databanks are kept.`;
    form.hidden = false;
    form.querySelector('button[type="submit"]').focus();
  }
  async function executeBatch(destination = "") {
    if (state.busy || !batchTarget) return;
    const action = batchAction;
    const target = { ...batchTarget, ...(["copy", "move"].includes(action) ? { target_project: state.project, target_databank: destination } : {}) };
    const revision = ++generation;
    state.busy = true; state.error = ""; state.notice = ""; render();
    try {
      const operation = action !== "export" ? await retainDatabankOperation(action, target) : null;
      const result = await databankBatchAction(action, operation?.target || target, fetchImpl);
      operation?.completed();
      if (!current() || revision !== generation) return;
      if (action === "export") {
        download(result, `${state.databank}-strategies.zip`);
        state.notice = "Selected strategies prepared for saving.";
      } else {
        const results = await fetchCustomProjectResults(state.project, fetchImpl);
        if (!current() || revision !== generation) return;
        state.results = results;
        if (["move", "remove", "clear"].includes(action)) { state.archive = ""; state.checkedArchives = []; }
        state.notice = "Databank change verified and saved."; select(); onChanged(action, state.results);
      }
    } catch (error) { if (!current() || revision !== generation) return; state.error = error.message; }
    if (!current() || revision !== generation) return;
    batchTarget = null; state.busy = false; render();
  }
  async function discardImport(id, confirm = false) {
    if (state.busy) return;
    const pending = state.pendingOperations?.find(row => row.action === "load" && row.target.operation_id === id);
    if (!pending) return;
    const previewHash = confirm ? pending.discard_preview_sha256 || state.purgePreview?.intent_id : null;
    const revision = ++generation;
    let operation;
    Object.assign(state, { busy: true, error: "", notice: confirm ? "Discarding the confirmed retained import…" : "Reading retained import files…" }); render();
    try {
      const { operation_id, ...target } = pending.target;
      operation = await retainDatabankOperation("load", target, undefined, pending);
      if (operation.target.operation_id !== operation_id) throw new Error("Pending import changed. Refresh before retrying.");
      if (confirm) operation.confirmDiscard(previewHash);
      const result = await importDiscard(confirm ? "confirm" : "preview", { ...operation.target, ...(confirm ? { expected_preview_sha256: previewHash } : {}) }, fetchImpl);
      if (confirm) operation.completed();
      if (!current() || revision !== generation) return;
      if (confirm) {
        state.purgePreview = null;
        state.results = await fetchCustomProjectResults(state.project, fetchImpl);
        if (!current() || revision !== generation) return;
        state.notice = `Unfinished import discarded. ${(result.reclaimed_bytes / 1048576).toFixed(2)} MiB of retained file content removed. Shared files and original desktop imports were kept.${result.reclamation_uncertain_paths.length ? " Some interrupted file operations have uncertain byte accounting." : ""}`;
        onChanged("import-discard");
      } else { state.purgePreview = result; state.notice = "Review the retained files before discarding this import."; }
    } catch (error) {
      if (error.discardNotStarted && operation) {
        try { operation.discardNotStarted(previewHash); } catch (storageError) { error = storageError; }
        state.purgePreview = null;
      }
      if (!current() || revision !== generation) return;
      state.error = error.message;
    }
    if (!current() || revision !== generation) return;
    state.busy = false; render();
  }
  async function purgeCandidate(confirm = false) {
    if (state.busy) return;
    if (confirm && state.purgePreview?.preview.cancel_import) return discardImport(state.purgePreview.preview.cancel_import.operation_id, true);
    const id = confirm ? state.purgePreview?.preview.candidate_entity_id : selection()?.candidate_association?.candidate_entity_id;
    if (!id) return;
    const target = { candidate_entity_id: id, ...(confirm ? { expected_preview_sha256: state.purgePreview.intent_id } : {}) };
    const revision = ++generation;
    Object.assign(state, { busy: true, error: "", notice: confirm ? "Deleting the confirmed candidate files…" : "Reading affected files…" }); render();
    try {
      const result = await candidatePurge(confirm ? "confirm" : "preview", target, fetchImpl);
      if (!current() || revision !== generation) return;
      if (confirm) {
        state.purgePreview = null; state.archive = ""; state.checkedArchives = [];
        state.results = await fetchCustomProjectResults(state.project, fetchImpl);
        if (!current() || revision !== generation) return;
        state.notice = `Candidate deletion completed. ${(result.reclaimed_bytes / 1048576).toFixed(2)} MiB of retained file content removed. Shared artifacts and original desktop files were kept.${result.reclamation_uncertain_paths.length ? ` Removed-byte accounting is uncertain for ${result.reclamation_uncertain_paths.length} interrupted file operations.` : ""}`; select(); onChanged("purge", state.results);
      } else { state.purgePreview = result; state.notice = "Review the affected files before deleting."; }
    } catch (error) { if (!current() || revision !== generation) return; state.error = error.message; }
    if (!current() || revision !== generation) return;
    state.busy = false; render();
  }
  async function retryOperation(id) {
    if (state.busy) return;
    const pending = state.pendingOperations?.find(row => row.target.operation_id === id);
    if (!pending) return;
    if (pending.discard_preview_sha256) return discardImport(id, true);
    cancelForms();
    const revision = ++generation;
    state.busy = true; state.error = ""; render();
    try {
      const { operation_id, ...target } = pending.target;
      const operation = await retainDatabankOperation(pending.action, target, undefined, pending);
      if (operation.target.operation_id !== operation_id) throw new Error("Pending operation changed. Refresh before retrying.");
      const result = ["load", "rename", "reconcile"].includes(pending.action)
        ? await databankAction(pending.action, operation.target, null, fetchImpl)
        : await databankBatchAction(pending.action, operation.target, fetchImpl);
      operation.completed();
      if (!current() || revision !== generation) return;
      state.results = await fetchCustomProjectResults(state.project, fetchImpl);
      if (!current() || revision !== generation) return;
      if (pending.action === "rename" && state.databank === target.databank) {
        if (state.archive === target.archive) state.archive = result.archive;
        state.checkedArchives = state.checkedArchives.map(name => name === target.archive ? result.archive : name);
      }
      if (pending.action === "load") { state.databank = result.databank; state.archive = result.archive; state.checkedArchives = [result.archive]; }
      const remaining = new Set(selectedBank()?.strategies.map(row => row.archive) || []);
      if (!remaining.has(state.archive)) state.archive = "";
      state.checkedArchives = state.checkedArchives.filter(name => remaining.has(name));
      state.notice = "Original databank operation verified and saved."; select(); onChanged(pending.action, state.results);
    } catch (error) { if (!current() || revision !== generation) return; state.error = error.message; }
    if (!current() || revision !== generation) return;
    state.busy = false; render();
  }
  function bind() {
    dock.addEventListener("change", (event) => {
      if (state.busy) return;
      if (event.target.matches("[data-dock-columns]")) {
        state.columns = ["is", "oos"].includes(event.target.value) ? event.target.value : "all";
        render(); dock.querySelector("[data-dock-columns]").focus(); return;
      }
      if (event.target.matches(".sqx-databank-check input")) {
        cancelForms();
        const name = event.target.closest("tr").getAttribute("data-automation-archive");
        state.checkedArchives = state.checkedArchives.filter(value => value !== name);
        if (event.target.checked) state.checkedArchives.push(name);
        state.archive = name; select(); paintSelection(); return;
      }
      if (event.target.matches("[data-dock-project]")) { void readProject(event.target.value); return; }
      if (event.target.matches("[data-dock-load]")) { const file = event.target.files?.[0]; if (file) void act("load", file); }
    });
    dock.addEventListener("click", (event) => {
      const row = event.target.closest("[data-automation-archive]");
      if (row) {
        if (event.target.matches?.(".sqx-databank-check input")) { event.stopPropagation(); return; }
        event.preventDefault(); event.stopPropagation(); if (state.busy) return;
        cancelForms();
        state.archive = row.getAttribute("data-automation-archive"); select();
        state.checkedArchives = [state.archive]; paintSelection();
        return;
      }
      if (state.busy) return;
      const retry = event.target.closest("[data-dock-retry]");
      if (retry) { void retryOperation(retry.getAttribute("data-dock-retry")); return; }
      const discard = event.target.closest("[data-dock-discard]");
      if (discard) { cancelForms(); void discardImport(discard.getAttribute("data-dock-discard")); return; }
      const batch = event.target.closest("[data-dock-batch]");
      if (event.target.closest("[data-dock-purge]")) { cancelForms(); void purgeCandidate(); return; }
      if (event.target.closest("[data-dock-purge-cancel]")) { cancelForms(); return; }
      if (batch) { void prepareBatch(batch.getAttribute("data-dock-batch")); return; }
      if (event.target.closest("[data-dock-batch-cancel]")) { batchTarget = null; dock.querySelector("[data-dock-batch-form]").hidden = true; return; }
      if (event.target.closest("[data-dock-refresh]")) { void readProject(state.project, true); return; }
      if (event.target.closest("[data-dock-open]")) { onOpen(state.project, selectedBank()?.name, state.archive); return; }
      const bank = event.target.closest("[data-dock-bank]");
      if (bank) { cancelForms(); state.databank = bank.getAttribute("data-dock-bank"); state.archive = ""; state.checkedArchives = []; state.error = ""; select(); render(); return; }
      if (event.target.closest("[data-dock-save]")) { void act("save"); return; }
      if (event.target.closest("[data-dock-reconcile]")) { cancelForms(); void act("reconcile"); return; }
      if (event.target.closest("[data-dock-new], [data-dock-rename]")) {
        nameAction = event.target.closest("[data-dock-new]") ? "create" : "rename";
        const form = dock.querySelector("[data-dock-name-form]"); form.hidden = false;
        const input = form.querySelector("input"); input.value = nameAction === "rename" ? state.archive.replace(/\.sqx$/i, "") : ""; input.focus();
      }
      if (event.target.closest("[data-dock-cancel]")) dock.querySelector("[data-dock-name-form]").hidden = true;
    });
    dock.addEventListener("dblclick", (event) => {
      const row = event.target.closest("[data-automation-archive]");
      if (!row) return; event.preventDefault(); event.stopPropagation();
      if (!state.busy) onOpen(state.project, selectedBank()?.name, row.getAttribute("data-automation-archive"));
    });
    dock.addEventListener("keydown", (event) => {
      if (event.key !== "Enter" || !event.target.matches(".sqx-databank-check input")) return;
      event.preventDefault(); event.target.click();
    });
    dock.addEventListener("submit", (event) => {
      if (event.target.matches("[data-dock-purge-form]")) { event.preventDefault(); void purgeCandidate(true); return; }
      if (event.target.matches("[data-dock-batch-form]")) { event.preventDefault(); void executeBatch(event.target.querySelector("select").value); return; }
      if (!event.target.matches("[data-dock-name-form]")) return;
      event.preventDefault(); void act(nameAction, null, event.target.querySelector("input").value.trim());
    });
  }
  select(); render();
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
