const NATIVE_RUNTIME_API_PATH = "/api/native-runtime";
const NATIVE_RUNTIME_SCHEMA = "tc.sqx-runtime-discovery.v1";
const CANDIDATE_PREFIX = "tc-sqx-home:sha256:";

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function object(value) {
  return value && typeof value === "object" && !Array.isArray(value) ? value : null;
}

function readableCode(value) {
  if (!value) return "";
  return String(value).replaceAll("_", " ").replaceAll("-", " ").replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export function parseNativeRuntimeDiscovery(payload) {
  const record = object(payload);
  if (!record || record.schema !== NATIVE_RUNTIME_SCHEMA) throw new Error("native runtime discovery schema mismatch");
  if (typeof record.expected_build !== "string" || !record.expected_build) throw new Error("native runtime expected build is missing");
  if (typeof record.process_pinned !== "boolean") throw new Error("native runtime process pin is missing");
  if (record.saved != null && !object(record.saved)) throw new Error("native runtime saved pointer is invalid");
  if (!Array.isArray(record.candidates)) throw new Error("native runtime candidates are missing");
  const recovery = object(record.recovery);
  if (!recovery || typeof recovery.action !== "string") throw new Error("native runtime recovery is missing");
  return Object.freeze({
    expected_build: record.expected_build,
    process_pinned: record.process_pinned,
    saved: record.saved,
    candidates: record.candidates.map((item) => {
      const candidate = object(item);
      if (!candidate || typeof candidate.candidate_id !== "string" || !candidate.candidate_id.startsWith(CANDIDATE_PREFIX)) {
        throw new Error("native runtime candidate identity is invalid");
      }
      if (typeof candidate.home_path !== "string" || !candidate.home_path) throw new Error("native runtime candidate path is missing");
      return Object.freeze({
        candidate_id: candidate.candidate_id,
        home_path: candidate.home_path,
        label: typeof candidate.label === "string" ? candidate.label : candidate.home_path,
        bindable: candidate.bindable === true,
        reason_code: typeof candidate.reason_code === "string" ? candidate.reason_code : null,
        launcher_sha256: typeof candidate.launcher_sha256 === "string" ? candidate.launcher_sha256 : null,
        observed_build: typeof candidate.observed_build === "string" ? candidate.observed_build : null,
      });
    }),
    recovery: Object.freeze({
      action: recovery.action,
      reason_code: typeof recovery.reason_code === "string" ? recovery.reason_code : null,
      detail: typeof recovery.detail === "string" ? recovery.detail : "",
    }),
  });
}

export async function fetchNativeRuntimeDiscovery(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Native runtime fetch is unavailable");
  const response = await fetchImpl(NATIVE_RUNTIME_API_PATH, { headers: { accept: "application/json" } });
  if (!response?.ok) throw new Error(`Native runtime request failed: ${response?.status ?? "unknown"}`);
  return parseNativeRuntimeDiscovery(await response.json());
}

function candidateRow(candidate, { processPinned, savedId }) {
  const bound = savedId === candidate.candidate_id;
  const state = candidate.bindable ? (bound ? "Saved" : "Verified") : readableCode(candidate.reason_code) || "Not verified";
  const bind = candidate.bindable && !processPinned
    ? `<button type="button" class="button button-secondary button-small" data-native-runtime-bind="${escapeHtml(candidate.candidate_id)}"><span>${bound ? "Rebind" : "Bind"}</span></button>`
    : "";
  return `<div class="stat-row" data-native-runtime-candidate="${escapeHtml(candidate.candidate_id)}" data-bindable="${candidate.bindable ? "true" : "false"}"><div><strong>${escapeHtml(candidate.label)}</strong><p class="field-help">${escapeHtml(candidate.home_path)}</p></div><span>${escapeHtml(state)}</span>${bind}</div>`;
}

export function renderNativeRuntimeSetup(record) {
  if (!record) {
    return `<div data-native-runtime-panel data-native-runtime-state="pending"><div class="empty-state is-compact"><div><strong>Checking installed runtimes</strong><p>Waiting for native runtime discovery.</p></div></div></div>`;
  }
  const savedId = record.saved?.candidate_id || "";
  const rows = record.candidates.length
    ? record.candidates.map((item) => candidateRow(item, { processPinned: record.process_pinned, savedId })).join("")
    : `<div class="empty-state is-compact"><div><strong>No SQX 144.2953 home found</strong><p>Install StrategyQuant X or set SQX_HOME, then reopen Settings.</p></div></div>`;
  const clear = record.saved && !record.process_pinned
    ? `<button type="button" class="button button-secondary button-small" data-native-runtime-clear="true"><span>Clear saved runtime</span></button>`
    : "";
  return `<div data-native-runtime-panel data-native-runtime-state="loaded" data-process-pinned="${record.process_pinned ? "true" : "false"}">
    <p class="note">${escapeHtml(record.recovery.detail)}</p>
    ${rows}
    ${clear}
  </div>`;
}

async function writeNativeRuntime(action, candidateId, fetchImpl = globalThis.fetch) {
  const payload = action === "clear" ? { action: "clear" } : { action: "bind", candidate_id: candidateId };
  if (action === "bind" && (typeof candidateId !== "string" || !candidateId.startsWith(CANDIDATE_PREFIX))) {
    throw new Error("bind requires a discovered candidate_id");
  }
  const response = await fetchImpl(NATIVE_RUNTIME_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify(payload),
  });
  const body = await response.json().catch(() => null);
  if (!response?.ok) throw new Error(body?.detail || `Native runtime write failed: ${response?.status ?? "unknown"}`);
  return parseNativeRuntimeDiscovery(body);
}

function replacePanel(zone, html) {
  const existing = zone.querySelector("[data-native-runtime-panel]");
  if (existing) {
    existing.outerHTML = html;
    return;
  }
  zone.innerHTML = html;
}

async function refresh(zone, fetchImpl = globalThis.fetch) {
  replacePanel(zone, renderNativeRuntimeSetup(null));
  try {
    const record = await fetchNativeRuntimeDiscovery(fetchImpl);
    if (zone.isConnected) replacePanel(zone, renderNativeRuntimeSetup(record));
  } catch (error) {
    if (!zone.isConnected) return;
    const detail = error instanceof Error ? error.message : "Native runtime discovery failed";
    replacePanel(
      zone,
      `<div data-native-runtime-panel data-native-runtime-state="error"><div class="empty-state is-compact tone-error"><div><strong>Runtime discovery unavailable</strong><p>${escapeHtml(detail)}</p></div></div></div>`,
    );
  }
}

async function onClick(event, zone) {
  const bind = event.target.closest?.("[data-native-runtime-bind]");
  const clear = event.target.closest?.("[data-native-runtime-clear]");
  if (!bind && !clear) return;
  event.preventDefault();
  try {
    const record = bind
      ? await writeNativeRuntime("bind", bind.getAttribute("data-native-runtime-bind"))
      : await writeNativeRuntime("clear");
    if (zone.isConnected) replacePanel(zone, renderNativeRuntimeSetup(record));
  } catch (error) {
    if (!zone.isConnected) return;
    const detail = error instanceof Error ? error.message : "Native runtime write failed";
    replacePanel(
      zone,
      `<div data-native-runtime-panel data-native-runtime-state="error"><div class="empty-state is-compact tone-error"><div><strong>Runtime setup failed</strong><p>${escapeHtml(detail)}</p></div></div></div>`,
    );
  }
}

function mountNativeRuntimeSetup(root = document) {
  const zone = root.querySelector?.("[data-native-runtime-setup]");
  if (!zone || zone.dataset.nativeRuntimeBound === "true") return false;
  zone.dataset.nativeRuntimeBound = "true";
  zone.addEventListener("click", (event) => {
    void onClick(event, zone);
  });
  void refresh(zone);
  return true;
}

if (typeof document !== "undefined") {
  const app = document.querySelector("#app");
  mountNativeRuntimeSetup(document);
  if (app && typeof MutationObserver !== "undefined") {
    const observer = new MutationObserver(() => mountNativeRuntimeSetup(document));
    observer.observe(app, { childList: true, subtree: true });
  }
}
