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

function strategyFromPayload(item, project, bank) {
  const archive = object(item);
  const name = typeof archive?.archive === "string" ? archive.archive : "";
  const relative = `user/projects/${project}/databanks/${bank}/${name}`;
  if (!name || !name.toLowerCase().endsWith(".sqx") || archive.relative_path !== relative) {
    throw new Error("Native Custom Project strategy archive is invalid");
  }
  if (archive.inspectable === true) {
    if (!digest(archive.archive_sha256) || archive.native_version !== SQX_BUILD) {
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

function tableRows(cellsList) {
  return cellsList.map((cells) => ({ cells: cells.map((cell) => escapeHtml(cell)) }));
}

function archiveRows(item) {
  if (!item?.databanks?.length) return [];
  const rows = [];
  for (const bank of item.databanks) {
    if (!bank.strategies.length) {
      rows.push([bank.name, "—", "Empty databank"]);
      continue;
    }
    for (const archive of bank.strategies) {
      const identity = archive.inspectable
        ? `${archive.archive} · ${String(archive.archive_sha256).slice(0, 12)}…`
        : archive.archive;
      const state = archive.inspectable ? "Inspectable" : readable(archive.reason_code, "Unread");
      rows.push([bank.name, identity, state]);
    }
  }
  return rows;
}

export function renderProjectDatabankList(results, project) {
  const item = projectResultsOf(results, project);
  if (!item) {
    return unavailable(
      "Native databanks unread",
      "This desktop lists producer archives from user/projects when the verified runtime can be read. Generated, rejected, and accepted counts stay dashes until StrategyQuant X MCP streams them.",
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
  return table({
    columns: [{ label: "Databank" }, { label: "Archive" }, { label: "State" }],
    rows: tableRows(archiveRows(item)),
  });
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
    rows.push(...archiveRows(project).map(([bank, archive, state]) => [project.name, bank, archive, state]));
  }
  const body = rows.length
    ? `${statList([["Projects", String(results.projects.length)], ["Databanks", String(results.databank_count)], ["Strategy archives", String(results.strategy_count)]])}${table({
      columns: [{ label: "Project" }, { label: "Databank" }, { label: "Archive" }, { label: "State" }],
      rows: tableRows(rows),
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
