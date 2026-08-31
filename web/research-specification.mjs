const SQX_BUILDER_CONFIG_API_PATH = "/api/sqx-builder-config";
export const BUILDER_CONFIG_SCHEMA = "tc.sqx-builder-config.v1";
export const RESEARCH_SPECIFICATION_SCHEMA = "tc.research-specification.v1";

export class ResearchSpecificationApiError extends Error {
  constructor(message, { status = 0, payload = null } = {}) {
    super(message);
    this.name = "ResearchSpecificationApiError";
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

function readableState(value) {
  return String(value || "unresolved")
    .replaceAll("_", " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function stateTone(state) {
  if (state === "user_selected" || state === "proven_default" || state === "not_applicable") return "ready";
  return "unavailable";
}

function compactValues(values) {
  if (!values || typeof values !== "object") return "";
  const entries = Object.entries(values).filter(([, value]) => value !== null && value !== "" && value !== false);
  if (!entries.length) return "";
  return entries.map(([key, value]) => {
    const printable = Array.isArray(value) ? value.map((item) => typeof item === "object" ? JSON.stringify(item) : String(item)).join(", ") : String(value);
    return `<div class="stat-row"><span>${escapeHtml(readableState(key))}</span><code>${escapeHtml(printable)}</code></div>`;
  }).join("");
}

export function specificationFromBuilderConfig(payload) {
  if (!payload || payload.schema !== BUILDER_CONFIG_SCHEMA) {
    throw new ResearchSpecificationApiError("Builder configuration schema mismatch");
  }
  const specification = payload.specification;
  if (!specification || specification.schema !== RESEARCH_SPECIFICATION_SCHEMA || !Array.isArray(specification.requirements)) {
    throw new ResearchSpecificationApiError("Research Specification schema mismatch");
  }
  return specification;
}

export async function fetchResearchSpecification(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new ResearchSpecificationApiError("Specification fetch is unavailable");
  const response = await fetchImpl(SQX_BUILDER_CONFIG_API_PATH, { headers: { accept: "application/json" } });
  let payload = null;
  try {
    payload = await response.json();
  } catch {
    payload = null;
  }
  if (!response?.ok) {
    throw new ResearchSpecificationApiError(payload?.detail || `Specification request failed: ${response?.status ?? "unknown"}`, {
      status: response?.status ?? 0,
      payload,
    });
  }
  return specificationFromBuilderConfig(payload);
}

export function renderResearchSpecification(specification) {
  const gate = specification.build_gate || {};
  const gateLabel = gate.locked ? "Build locked" : "Build requirements resolved";
  const gateTone = gate.locked ? "unavailable" : "ready";
  const reasons = Array.isArray(gate.reason_codes) ? gate.reason_codes : [];
  const summary = `<div class="requirement-item specification-gate"><strong>Build gate</strong><span class="status-badge status-${gateTone}"><span class="status-dot"></span>${escapeHtml(gateLabel)}</span><p>${escapeHtml(reasons.join(" · ") || "No unresolved requirement reasons reported")}</p></div>`;
  const requirements = specification.requirements.map((requirement) => {
    const state = requirement.state || "unresolved";
    const required = requirement.required ? "Required" : "Conditional";
    return `<div class="requirement-item" data-specification-requirement="${escapeHtml(requirement.id)}"><div><strong>${escapeHtml(requirement.label)}</strong><span class="field-help">${escapeHtml(required)}</span></div><span class="status-badge status-${stateTone(state)}"><span class="status-dot"></span>${escapeHtml(readableState(state))}</span><p>${escapeHtml(requirement.detail || "")}</p>${compactValues(requirement.values)}<p class="field-help">Evidence: ${escapeHtml(requirement.evidence?.native_source_path || "native SQX")}</p></div>`;
  }).join("");
  return summary + requirements;
}

function isSpecificationRoute() {
  return globalThis.location?.pathname === "/research/construct/specification";
}

let activeGrid = null;
async function bindSpecification() {
  if (!isSpecificationRoute()) return;
  const grid = globalThis.document?.querySelector(".requirement-grid");
  if (!grid || grid === activeGrid) return;
  activeGrid = grid;
  grid.innerHTML = '<div class="requirement-item"><strong>Resolving native requirements…</strong><p>Reading the exact native Builder configuration without launching SQX.</p></div>';
  try {
    const specification = await fetchResearchSpecification();
    if (grid !== activeGrid || !grid.isConnected) return;
    grid.innerHTML = renderResearchSpecification(specification);
  } catch (error) {
    if (grid !== activeGrid || !grid.isConnected) return;
    const detail = error instanceof Error ? error.message : "Specification unavailable";
    grid.innerHTML = `<div class="requirement-item"><strong>Native Specification unavailable</strong><span class="status-badge status-unavailable"><span class="status-dot"></span>Build locked</span><p>${escapeHtml(detail)}</p></div>`;
  }
}

if (typeof document !== "undefined") {
  const observer = new MutationObserver(() => {
    if (!isSpecificationRoute()) {
      activeGrid = null;
      return;
    }
    void bindSpecification();
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void bindSpecification();
}
