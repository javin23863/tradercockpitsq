import { escapeHtml, pageTitle, statList, unavailable } from "./ui.mjs";

const SQX_MODULE_API_PATH = "/api/sqx-module";
const MODULE_SCHEMA = "tc.sqx-run-module.v1";
const SQX_BUILD = "144.2953";

function object(value) {
  return value && typeof value === "object" && !Array.isArray(value) ? value : null;
}

function digest(value) {
  return typeof value === "string" && /^[0-9a-f]{64}$/.test(value) ? value : "";
}

export function sqxModuleFromPayload(payload) {
  const record = object(payload);
  if (
    !record
    || record.schema !== MODULE_SCHEMA
    || record.source_build !== SQX_BUILD
    || !["run", "inspect"].includes(record.kind)
    || typeof record.module !== "string"
    || !record.module
    || !["ready", "unavailable"].includes(record.status)
    || (record.status === "unavailable" && (typeof record.reason_code !== "string" || !record.reason_code))
    || (record.detail !== null && record.detail !== undefined && typeof record.detail !== "string")
    || record.editor_wired !== false
    || !object(record.control)
    || typeof record.control.available !== "boolean"
    || (record.control.available
      ? record.control.reason_code != null && record.control.reason_code !== ""
      : typeof record.control.reason_code !== "string" || !record.control.reason_code)
  ) {
    throw new Error("Native SQX module record is invalid");
  }
  if (record.archive_sha256 != null && !digest(record.archive_sha256)) {
    throw new Error("Native SQX module archive digest is invalid");
  }
  return record;
}

async function readJson(response) {
  try { return await response.json(); } catch { return null; }
}

export async function fetchSqxModule(module, fetchImpl = globalThis.fetch) {
  if (typeof module !== "string" || !module.trim()) throw new Error("Exact SQX module name is required");
  if (typeof fetchImpl !== "function") throw new Error("Native module fetch is unavailable");
  const path = `${SQX_MODULE_API_PATH}?${new URLSearchParams({ module: module.trim() }).toString()}`;
  const response = await fetchImpl(path, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw new Error(payload?.detail || `Native module request failed: ${response?.status ?? "unknown"}`);
  return sqxModuleFromPayload(payload);
}

export function renderInspectModule(route, moduleRecord) {
  const label = route.label || moduleRecord?.module || "Module";
  if (!moduleRecord) {
    return `${pageTitle(label, { subtitle: "Native StrategyQuant X module" })}${unavailable("Reading native module…", "Inspecting the verified StrategyQuant X runtime for this module archive.", { tone: "pending" })}`;
  }
  const rows = [
    ["Module", moduleRecord.module],
    ["Kind", moduleRecord.kind],
    ["Status", moduleRecord.status],
    ["Reason", moduleRecord.reason_code || "—"],
    ["Archive", moduleRecord.source_relative_path || "—"],
    ["SHA-256", moduleRecord.archive_sha256 || "—"],
  ];
  return `${pageTitle(label, { subtitle: "Native StrategyQuant X module — no substitute editor" })}
    <section class="card accent-orange" data-sqx-inspect-module="${escapeHtml(moduleRecord.module)}">
      <div class="card-body">
        ${unavailable(moduleRecord.module, moduleRecord.detail || "This desktop does not invent a native editor.", { compact: true, tone: "unavailable" })}
        ${statList(rows)}
        <p class="note">Open this module in StrategyQuant X when the archive exists. This desktop does not invent a data downloader or AlgoWizard block editor.</p>
      </div>
    </section>`;
}

export function bindInspectModule(moduleName) {
  const root = document.querySelector("[data-sqx-inspect-host]");
  if (!root || root.dataset.sqxInspectBound === moduleName) return;
  root.dataset.sqxInspectBound = moduleName;
  root.innerHTML = unavailable("Reading native module…", "Inspecting the verified StrategyQuant X runtime.", { tone: "pending", compact: true });
  void fetchSqxModule(moduleName).then((record) => {
    if (!root.isConnected) return;
    root.innerHTML = renderInspectModule({ label: moduleName }, record);
  }).catch((error) => {
    if (!root.isConnected) return;
    root.innerHTML = unavailable(
      `${moduleName} unavailable`,
      error instanceof Error ? error.message : "Native module could not be read.",
      { compact: true, tone: "error" },
    );
  });
}

if (typeof document !== "undefined") {
  const observer = new MutationObserver(() => {
    const host = document.querySelector("[data-sqx-inspect-host]");
    const moduleName = host?.getAttribute("data-sqx-module") || "";
    if (moduleName) bindInspectModule(moduleName);
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  const host = document.querySelector("[data-sqx-inspect-host]");
  const moduleName = host?.getAttribute("data-sqx-module") || "";
  if (moduleName) bindInspectModule(moduleName);
}
