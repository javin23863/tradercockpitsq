const RESEARCH_CONFIGURATIONS_API_PATH = "/api/research/configurations";
export const CONFIGURATION_SCHEMA = "tc.research-configuration.v1";
export const CONFIGURATION_CATALOG_SCHEMA = "tc.research-configuration-catalog.v1";

export class ResearchBuildApiError extends Error {
  constructor(message, { status = 0, payload = null } = {}) {
    super(message);
    this.name = "ResearchBuildApiError";
    this.status = status;
    this.payload = payload;
  }
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function readable(value) {
  return String(value || "")
    .replaceAll("_", " ")
    .replaceAll("-", " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function responsePayloadError(response, payload, fallback) {
  return new ResearchBuildApiError(payload?.detail || fallback, {
    status: response?.status ?? 0,
    payload,
  });
}

async function readJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

export function configurationFromPayload(payload) {
  if (!payload || payload.schema !== CONFIGURATION_SCHEMA) {
    throw new ResearchBuildApiError("Research configuration schema mismatch");
  }
  const requiredStrings = [
    "entity_id",
    "revision",
    "content_ref",
    "sqx_build",
    "source_project_path",
    "source_project_sha256",
    "source_project_ref",
    "source_entry",
    "source_entry_ref",
    "executable_xml_ref",
    "executable_xml_sha256",
    "assembly_mode",
  ];
  if (requiredStrings.some((key) => typeof payload[key] !== "string" || !payload[key])) {
    throw new ResearchBuildApiError("Research configuration identity is incomplete");
  }
  if (!payload.review || typeof payload.review.changed !== "boolean" || typeof payload.review.summary !== "string") {
    throw new ResearchBuildApiError("Research configuration review is invalid");
  }
  if (!payload.approval || typeof payload.approval.approved !== "boolean") {
    throw new ResearchBuildApiError("Research configuration approval is invalid");
  }
  if (!payload.launch || payload.launch.enabled !== false || typeof payload.launch.reason_code !== "string") {
    throw new ResearchBuildApiError("Research configuration launch gate is invalid");
  }
  if (!Array.isArray(payload.approved_changes) || payload.approved_changes.some((item) => typeof item !== "string")) {
    throw new ResearchBuildApiError("Research configuration change list is invalid");
  }
  if (!["compiled", "approved"].includes(payload.state)) {
    throw new ResearchBuildApiError("Research configuration state is invalid");
  }
  if (payload.state === "compiled") {
    if (
      payload.parent_revision !== null
      || payload.approval.approved !== false
      || payload.approval.approved_from_revision !== null
    ) {
      throw new ResearchBuildApiError("Compiled configuration approval shape is invalid");
    }
  } else if (
    typeof payload.parent_revision !== "string"
    || !payload.parent_revision
    || payload.approval.approved !== true
    || payload.approval.approved_from_revision !== payload.parent_revision
  ) {
    throw new ResearchBuildApiError("Approved configuration approval shape is invalid");
  }
  return payload;
}

export function configurationCatalogFromPayload(payload) {
  if (!payload || payload.schema !== CONFIGURATION_CATALOG_SCHEMA || !Array.isArray(payload.configurations)) {
    throw new ResearchBuildApiError("Research configuration catalog schema mismatch");
  }
  for (const item of payload.configurations) {
    if (
      !item
      || typeof item.entity_id !== "string"
      || !item.entity_id
      || typeof item.revision !== "string"
      || !item.revision
      || !["compiled", "approved"].includes(item.state)
      || typeof item.source_project_sha256 !== "string"
      || typeof item.executable_xml_sha256 !== "string"
    ) {
      throw new ResearchBuildApiError("Research configuration catalog entry is invalid");
    }
  }
  return payload;
}

export async function fetchConfigurationCatalog(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new ResearchBuildApiError("Configuration fetch is unavailable");
  const response = await fetchImpl(RESEARCH_CONFIGURATIONS_API_PATH, { headers: { accept: "application/json" } });
  const payload = await readJson(response);
  if (!response?.ok) throw responsePayloadError(response, payload, `Configuration catalog request failed: ${response?.status ?? "unknown"}`);
  return configurationCatalogFromPayload(payload);
}

export async function fetchConfiguration(entityId, fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new ResearchBuildApiError("Configuration fetch is unavailable");
  if (typeof entityId !== "string" || !entityId) throw new ResearchBuildApiError("Configuration entity id is required");
  const response = await fetchImpl(`${RESEARCH_CONFIGURATIONS_API_PATH}?entityId=${encodeURIComponent(entityId)}`, {
    headers: { accept: "application/json" },
  });
  const payload = await readJson(response);
  if (!response?.ok) throw responsePayloadError(response, payload, `Configuration request failed: ${response?.status ?? "unknown"}`);
  return configurationFromPayload(payload);
}

async function postConfiguration(payload, fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new ResearchBuildApiError("Configuration write is unavailable");
  const response = await fetchImpl(RESEARCH_CONFIGURATIONS_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify(payload),
  });
  const body = await readJson(response);
  if (!response?.ok) throw responsePayloadError(response, body, `Configuration write failed: ${response?.status ?? "unknown"}`);
  return configurationFromPayload(body);
}

export function compileConfiguration(fetchImpl = globalThis.fetch) {
  return postConfiguration({ action: "compile" }, fetchImpl);
}

export function approveConfiguration(entityId, expectedRevision, fetchImpl = globalThis.fetch) {
  if (typeof entityId !== "string" || !entityId || typeof expectedRevision !== "string" || !expectedRevision) {
    throw new ResearchBuildApiError("Configuration approval requires entity and revision identity");
  }
  return postConfiguration({
    action: "approve",
    entity_id: entityId,
    expected_revision: expectedRevision,
  }, fetchImpl);
}

export function configurationSelectionTarget(catalog, preferredEntityId = "", selectedEntityId = "") {
  if (preferredEntityId) return preferredEntityId;
  if (selectedEntityId) return selectedEntityId;
  return catalog.length === 1 ? catalog[0].entity_id : "";
}

export async function refreshConfigurationAfterConflict(entityId, detail, fetchImpl = globalThis.fetch) {
  const catalogPayload = await fetchConfigurationCatalog(fetchImpl);
  const catalog = Object.freeze([...catalogPayload.configurations]);
  const selected = await fetchConfiguration(entityId, fetchImpl);
  return Object.freeze({ phase: "loaded", catalog, selected, detail });
}

export function isBuildRoute(locationLike = globalThis.location) {
  if (locationLike?.pathname !== "/research") return false;
  const params = new URLSearchParams(locationLike.search || "");
  return params.get("stage") === "construct" && params.get("tab") === "build";
}

function badge(label, tone) {
  return `<span class="status-badge status-${escapeHtml(tone)}"><span class="status-dot"></span>${escapeHtml(label)}</span>`;
}

function identityRow(label, value) {
  return `<div class="stat-row"><span>${escapeHtml(label)}</span><code>${escapeHtml(value)}</code></div>`;
}

export function renderBuildWorkspace({ phase = "loaded", catalog = [], selected = null, detail = "" } = {}) {
  const selectedEntity = selected?.entity_id || "";
  const catalogMarkup = catalog.length
    ? `<div class="idea-catalog-list">${catalog.map((item) => `<button class="idea-catalog-item ${item.entity_id === selectedEntity ? "is-active" : ""}" type="button" data-build-action="select" data-configuration-entity-id="${escapeHtml(item.entity_id)}"><strong>${escapeHtml(readable(item.state))}</strong><span>${escapeHtml(String(item.revision).slice(-12))}</span></button>`).join("")}</div>`
    : '<div class="idea-catalog-state">No compiled configurations yet.</div>';

  let detailMarkup = '<div class="empty-state"><div class="empty-icon">—</div><div><strong>No configuration selected</strong><p>Compile the exact current native Builder task snapshot to create immutable configuration custody.</p></div></div>';
  if (phase === "loading") {
    detailMarkup = '<div class="empty-state"><div class="empty-icon">…</div><div><strong>Loading configuration custody</strong><p>Reading canonical local configuration revisions.</p></div></div>';
  } else if (selected) {
    const approved = selected.approval?.approved === true;
    const approvalLabel = approved ? "Approved" : "Review required";
    const approvalTone = approved ? "ready" : "unavailable";
    const approveButton = approved
      ? '<button class="button button-primary" type="button" disabled data-build-approval-complete>Approved exact revision</button>'
      : '<button class="button button-primary" type="button" data-build-action="approve">Approve exact revision</button>';
    detailMarkup = `<div data-build-selected-configuration>
      <div class="idea-identity">
        ${identityRow("Configuration entity", selected.entity_id)}
        ${identityRow("Current revision", selected.revision)}
        ${selected.parent_revision ? identityRow("Compiled parent", selected.parent_revision) : ""}
        ${identityRow("SQX build", selected.sqx_build)}
        ${identityRow("Source project", selected.source_project_path)}
        ${identityRow("Source project SHA-256", selected.source_project_sha256)}
        ${identityRow("Executable XML SHA-256", selected.executable_xml_sha256)}
        ${identityRow("Assembly mode", selected.assembly_mode)}
      </div>
      <div class="requirement-item">
        <div><strong>Exact-byte review</strong>${badge(selected.review.changed ? "Changed" : "Byte identical", selected.review.changed ? "unavailable" : "ready")}</div>
        <p>${escapeHtml(selected.review.summary)}</p>
        <p class="field-help">Approved changes: ${escapeHtml(selected.approved_changes.length ? selected.approved_changes.join(", ") : "none")}</p>
      </div>
      <div class="requirement-item" data-build-approval-state="${approved ? "approved" : "compiled"}">
        <div><strong>Approval</strong>${badge(approvalLabel, approvalTone)}</div>
        <p>${approved ? `Approval binds compiled revision ${escapeHtml(selected.approval.approved_from_revision || "")}.` : "Approval will create a new immutable revision without changing the executable XML identity."}</p>
        ${approveButton}
      </div>
      <div class="requirement-item" data-build-launch-gate="disabled">
        <div><strong>Native launch</strong>${badge("Disabled", "unavailable")}</div>
        <p>${escapeHtml(selected.launch.reason_code)}</p>
        <button class="button button-secondary" type="button" disabled data-build-launch-disabled>Launch Builder</button>
      </div>
    </div>`;
  }

  return `<section class="idea-workspace" data-research-build-workspace>
    <article class="panel idea-catalog-panel" data-accent="cyan">
      <div class="panel-heading"><div><p class="eyebrow">Configuration custody</p><h2>Compiled snapshots</h2></div></div>
      <p class="panel-description">Each compile creates an immutable configuration entity from the exact current native Builder task bytes.</p>
      <button class="button button-secondary" type="button" data-build-action="compile" ${phase === "loading" ? "disabled" : ""}>Compile current native snapshot</button>
      ${catalogMarkup}
    </article>
    <article class="panel idea-editor-panel" data-accent="orange">
      <div class="panel-heading"><div><p class="eyebrow">Construct / Build</p><h2>Exact native configuration review</h2></div></div>
      <p class="panel-description">TraderCockpit owns custody and approval only. SQX retains configuration meaning and native execution.</p>
      ${detail ? `<p class="idea-save-status" data-build-status>${escapeHtml(detail)}</p>` : '<p class="idea-save-status" data-build-status></p>'}
      ${detailMarkup}
    </article>
  </section>`;
}

let activeRoot = null;
let buildState = Object.freeze({ phase: "idle", catalog: [], selected: null, detail: "" });

function findBuildRoot() {
  if (!isBuildRoute()) return null;
  const content = globalThis.document?.querySelector(".content-inner");
  if (!content) return null;
  const grid = content.querySelector(".dashboard-grid");
  return grid || content.querySelector("[data-research-build-workspace]");
}

function renderBoundRoot() {
  if (!activeRoot?.isConnected) return;
  activeRoot.outerHTML = renderBuildWorkspace(buildState);
  activeRoot = globalThis.document?.querySelector("[data-research-build-workspace]") || null;
}

async function loadCatalog(preferredEntityId = "") {
  const selectedEntityId = buildState.selected?.entity_id || "";
  buildState = Object.freeze({ ...buildState, phase: "loading", detail: "" });
  renderBoundRoot();
  try {
    const catalogPayload = await fetchConfigurationCatalog();
    const catalog = Object.freeze([...catalogPayload.configurations]);
    const target = configurationSelectionTarget(catalog, preferredEntityId, selectedEntityId);
    const selected = target ? await fetchConfiguration(target) : null;
    buildState = Object.freeze({ phase: "loaded", catalog, selected, detail: "" });
  } catch (error) {
    buildState = Object.freeze({
      phase: "failed",
      catalog: [],
      selected: null,
      detail: error instanceof Error ? error.message : "Configuration custody unavailable",
    });
  }
  renderBoundRoot();
}

async function compileCurrent() {
  buildState = Object.freeze({ ...buildState, phase: "loading", detail: "Compiling exact native snapshot…" });
  renderBoundRoot();
  try {
    const compiled = await compileConfiguration();
    await loadCatalog(compiled.entity_id);
  } catch (error) {
    buildState = Object.freeze({ ...buildState, phase: "failed", detail: error instanceof Error ? error.message : "Configuration compile failed" });
    renderBoundRoot();
  }
}

async function approveCurrent() {
  const selected = buildState.selected;
  if (!selected) return;
  buildState = Object.freeze({ ...buildState, phase: "loading", detail: "Approving exact revision…" });
  renderBoundRoot();
  try {
    const approved = await approveConfiguration(selected.entity_id, selected.revision);
    await loadCatalog(approved.entity_id);
  } catch (error) {
    const detail = error instanceof Error ? error.message : "Configuration approval failed";
    if (error instanceof ResearchBuildApiError && error.status === 409) {
      try {
        buildState = await refreshConfigurationAfterConflict(selected.entity_id, detail);
      } catch (refreshError) {
        buildState = Object.freeze({
          phase: "failed",
          catalog: [],
          selected: null,
          detail: refreshError instanceof Error ? refreshError.message : detail,
        });
      }
    } else {
      buildState = Object.freeze({ ...buildState, phase: "failed", detail });
    }
    renderBoundRoot();
  }
}

async function selectConfiguration(entityId) {
  buildState = Object.freeze({ ...buildState, phase: "loading", detail: "Loading saved configuration…" });
  renderBoundRoot();
  try {
    const selected = await fetchConfiguration(entityId);
    buildState = Object.freeze({ ...buildState, phase: "loaded", selected, detail: "" });
  } catch (error) {
    buildState = Object.freeze({ ...buildState, phase: "failed", selected: null, detail: error instanceof Error ? error.message : "Configuration read failed" });
  }
  renderBoundRoot();
}

async function bindBuild() {
  if (!isBuildRoute()) {
    activeRoot = null;
    buildState = Object.freeze({ phase: "idle", catalog: [], selected: null, detail: "" });
    return;
  }
  const root = findBuildRoot();
  if (!root || root === activeRoot) return;
  activeRoot = root;
  activeRoot.outerHTML = renderBuildWorkspace({ phase: "loading" });
  activeRoot = globalThis.document?.querySelector("[data-research-build-workspace]") || null;
  await loadCatalog();
}

if (typeof document !== "undefined") {
  document.addEventListener("click", (event) => {
    const target = event.target?.closest?.("[data-build-action]");
    if (!target || !isBuildRoute()) return;
    const action = target.dataset.buildAction;
    if (action === "compile") void compileCurrent();
    if (action === "approve") void approveCurrent();
    if (action === "select") void selectConfiguration(target.dataset.configurationEntityId || "");
  });

  const observer = new MutationObserver(() => { void bindBuild(); });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void bindBuild();
}