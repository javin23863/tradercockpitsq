import { renderResearchCapabilityCoverage } from "./research-capabilities.mjs";
import { researchLocationMatches } from "./model.mjs";
import {
  fetchClarifyingQuestions,
  renderClarifyingQuestions,
} from "./research-questions.mjs";

const SQX_BUILDER_CONFIG_API_PATH = "/api/sqx-builder-config";
const BUILDER_PROJECT_PATH = "user/projects/Builder/project.cfx";
const BUILDER_TASK_ENTRY = "Build-Task1.xml";

export const BUILDER_CONFIG_SCHEMA = "tc.sqx-builder-config.v1";
export const BUILDER_SEARCH_SCHEMA = "tc.sqx-builder-search.v1";
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
  if (
    state === "user_selected"
    || state === "proven_default"
    || state === "native_validated"
    || state === "producer_configured"
    || state === "not_applicable"
  ) return "ready";
  return "unavailable";
}

function compactValues(values) {
  if (!values || typeof values !== "object") return "";
  const entries = Object.entries(values).filter(([, value]) => value !== null && value !== "" && value !== false);
  if (!entries.length) return "";
  return entries.map(([key, value]) => {
    const printable = Array.isArray(value)
      ? value.map((item) => typeof item === "object" ? JSON.stringify(item) : String(item)).join(", ")
      : String(value);
    return `<div class="stat-row"><span>${escapeHtml(readableState(key))}</span><code>${escapeHtml(printable)}</code></div>`;
  }).join("");
}

function validBuildGate(gate) {
  return Boolean(
    gate
    && typeof gate === "object"
    && typeof gate.locked === "boolean"
    && Array.isArray(gate.reason_codes),
  );
}

function isPlainObject(value) {
  return Boolean(value && typeof value === "object" && !Array.isArray(value));
}

function expectedSearchDisplayMode(selector) {
  const normalized = typeof selector === "string" && selector.trim()
    ? selector.trim().toLowerCase()
    : null;
  if (normalized === "genetic-evolution") {
    return { kind: "genetic_evolution", label: "Genetic Evolution", recognized: true };
  }
  if (normalized === "random-generation") {
    return { kind: "random_discovery", label: "Random Discovery", recognized: true };
  }
  if (normalized === null) {
    return { kind: "unresolved", label: "Unresolved native search mode", recognized: false };
  }
  return { kind: "native_other", label: "Other native search mode", recognized: false };
}

function validateNativeSearchNode(node, path = "search.producer_configuration") {
  if (
    !isPlainObject(node)
    || typeof node.tag !== "string"
    || !node.tag
    || !isPlainObject(node.attributes)
    || Object.entries(node.attributes).some(([key, value]) => !key || typeof value !== "string")
    || !(node.text === null || typeof node.text === "string")
    || !Array.isArray(node.children)
  ) {
    throw new ResearchSpecificationApiError(`Native search configuration node mismatch at ${path}`);
  }
  node.children.forEach((child, index) => validateNativeSearchNode(child, `${path}.children[${index}]`));
  return node;
}

export function specificationFromBuilderConfig(payload) {
  if (!payload || payload.schema !== BUILDER_CONFIG_SCHEMA) {
    throw new ResearchSpecificationApiError("Builder configuration schema mismatch");
  }
  const specification = payload.specification;
  if (!specification || specification.schema !== RESEARCH_SPECIFICATION_SCHEMA || !Array.isArray(specification.requirements)) {
    throw new ResearchSpecificationApiError("Research Specification schema mismatch");
  }
  if (!validBuildGate(specification.build_gate)) {
    throw new ResearchSpecificationApiError("Research Specification build gate mismatch");
  }
  return specification;
}

export function searchConfigurationFromBuilderConfig(payload) {
  if (!payload || payload.schema !== BUILDER_CONFIG_SCHEMA) {
    throw new ResearchSpecificationApiError("Builder configuration schema mismatch");
  }
  if (
    payload.project !== "Builder"
    || payload.source_relative_path !== BUILDER_PROJECT_PATH
    || typeof payload.source_build !== "string"
    || !payload.source_build
    || typeof payload.archive_sha256 !== "string"
    || !/^[0-9a-f]{64}$/i.test(payload.archive_sha256)
    || !Array.isArray(payload.internal_entries)
    || payload.internal_entries.filter((entry) => entry === BUILDER_TASK_ENTRY).length !== 1
  ) {
    throw new ResearchSpecificationApiError("Builder search source identity mismatch");
  }

  const search = payload.search;
  if (
    !isPlainObject(search)
    || search.schema !== BUILDER_SEARCH_SCHEMA
    || search.authority !== "native_sqx_read_only"
    || !isPlainObject(search.source)
    || search.source.source_build !== payload.source_build
    || search.source.project !== payload.project
    || search.source.relative_path !== payload.source_relative_path
    || search.source.archive_sha256 !== payload.archive_sha256
    || search.source.member !== BUILDER_TASK_ENTRY
  ) {
    throw new ResearchSpecificationApiError("Native search configuration schema mismatch");
  }

  const selector = search.selector;
  if (!(selector === null || (typeof selector === "string" && selector.trim()))) {
    throw new ResearchSpecificationApiError("Native search selector mismatch");
  }

  const expectedMode = expectedSearchDisplayMode(selector);
  if (
    !isPlainObject(search.display_mode)
    || search.display_mode.kind !== expectedMode.kind
    || search.display_mode.label !== expectedMode.label
    || search.display_mode.recognized !== expectedMode.recognized
  ) {
    throw new ResearchSpecificationApiError("Native search display mode mismatch");
  }

  if (search.producer_configuration !== null) {
    validateNativeSearchNode(search.producer_configuration);
    if (search.producer_configuration.tag !== "BuildMode") {
      throw new ResearchSpecificationApiError("Native search root must be BuildMode");
    }
  }

  if (
    selector !== null
    && (
      search.producer_configuration === null
      || search.producer_configuration.attributes.generationType !== selector
    )
  ) {
    throw new ResearchSpecificationApiError("Native search selector does not match BuildMode");
  }

  if (
    !isPlainObject(search.semantics)
    || search.semantics.interpreted_by_tradercockpit !== false
    || search.semantics.owner !== "StrategyQuant X"
    || typeof search.semantics.description !== "string"
    || !search.semantics.description
    || !isPlainObject(search.execution)
    || search.execution.available !== false
    || typeof search.execution.reason !== "string"
    || !search.execution.reason
  ) {
    throw new ResearchSpecificationApiError("Native search authority boundary mismatch");
  }

  return search;
}

async function fetchBuilderConfigurationPayload(fetchImpl) {
  if (typeof fetchImpl !== "function") {
    throw new ResearchSpecificationApiError("Specification fetch is unavailable");
  }
  const response = await fetchImpl(SQX_BUILDER_CONFIG_API_PATH, { headers: { accept: "application/json" } });
  let payload = null;
  try {
    payload = await response.json();
  } catch {
    payload = null;
  }
  if (!response?.ok) {
    throw new ResearchSpecificationApiError(
      payload?.detail || `Specification request failed: ${response?.status ?? "unknown"}`,
      {
        status: response?.status ?? 0,
        payload,
      },
    );
  }
  return payload;
}

export async function fetchResearchSpecification(fetchImpl = globalThis.fetch) {
  const payload = await fetchBuilderConfigurationPayload(fetchImpl);
  return specificationFromBuilderConfig(payload);
}

export async function fetchResearchSpecificationViewModel(fetchImpl = globalThis.fetch) {
  const payload = await fetchBuilderConfigurationPayload(fetchImpl);
  return {
    specification: specificationFromBuilderConfig(payload),
    search: searchConfigurationFromBuilderConfig(payload),
  };
}

function flattenNativeSearchNodes(node, path = "BuildMode", rows = []) {
  rows.push({ path, node });
  node.children.forEach((child, index) => {
    flattenNativeSearchNodes(child, `${path}/${child.tag}[${index}]`, rows);
  });
  return rows;
}

function renderSearchModeLanes(search) {
  if (!search?.display_mode) return "";
  const kind = search.display_mode.kind;
  const recognized = search.display_mode.recognized;
  const modes = [
    {
      kind: "random_discovery",
      label: "Random Discovery",
      detail: "Native random-generation workflow. Genetic Evolution settings are not presented as active controls in this mode.",
    },
    {
      kind: "genetic_evolution",
      label: "Genetic Evolution",
      detail: "Native genetic-evolution workflow. SQX retains authority for population, ranking, selection, crossover, mutation, islands, migration, restart, and related search behavior.",
    },
  ];

  const rows = modes.map((mode) => {
    const selected = recognized && kind === mode.kind;
    const tone = selected ? "ready" : "unavailable";
    const label = selected ? "Selected native mode" : "Not selected";
    return `<div class="requirement-item" data-native-search-mode="${escapeHtml(mode.kind)}" data-selected="${selected ? "true" : "false"}"><div><strong>${escapeHtml(mode.label)}</strong><span class="status-badge status-${tone}"><span class="status-dot"></span>${escapeHtml(label)}</span></div><p>${escapeHtml(mode.detail)}</p></div>`;
  }).join("");

  const unknown = recognized
    ? ""
    : `<div class="requirement-item" data-native-search-mode="native_other" data-selected="true"><div><strong>${escapeHtml(search.display_mode.label)}</strong><span class="status-badge status-ready"><span class="status-dot"></span>Exact native selector</span></div><p>TraderCockpit keeps this producer mode visible without assigning Random Discovery or Genetic Evolution semantics.</p></div>`;

  return `<section data-native-search-mode-lanes><div class="context-callout"><span class="callout-icon">↳</span><div><span class="eyebrow">Native search workflows</span><strong>Random Discovery and Genetic Evolution are distinct</strong><span>This is a read-only mode view of the current native Builder task. It does not switch modes or execute search.</span></div></div><div class="requirement-list">${rows}${unknown}</div></section>`;
}

function renderNativeSearchConfiguration(search) {
  if (!search) return "";
  const selector = search.selector ?? "unresolved";
  const modeNote = search.display_mode.recognized
    ? "Presentation label for the exact native selector; SQX retains semantic authority."
    : "Opaque native selector; TraderCockpit does not assign producer semantics.";
  const nodes = search.producer_configuration
    ? flattenNativeSearchNodes(search.producer_configuration)
    : [];
  const rows = nodes.map(({ path, node }) => {
    const attributes = Object.entries(node.attributes).map(
      ([key, value]) => `<div class="stat-row"><span>${escapeHtml(key)}</span><code>${escapeHtml(value)}</code></div>`,
    ).join("");
    const text = node.text === null
      ? ""
      : `<div class="stat-row"><span>Text</span><code>${escapeHtml(node.text)}</code></div>`;
    return `<div class="requirement-item" data-native-search-node="${escapeHtml(path)}"><div><strong>${escapeHtml(node.tag)}</strong><span class="field-help">${escapeHtml(path)}</span></div>${attributes}${text}</div>`;
  }).join("");

  return `${renderSearchModeLanes(search)}<section data-native-search-configuration><div class="requirement-item"><div><strong>Native Search Configuration</strong><span class="status-badge status-ready"><span class="status-dot"></span>Read-only</span></div><p><strong>${escapeHtml(search.display_mode.label)}</strong></p><div class="stat-row"><span>Exact native selector</span><code>${escapeHtml(selector)}</code></div><div class="stat-row"><span>Source member</span><code>${escapeHtml(search.source.member)}</code></div><p>${escapeHtml(modeNote)}</p><p class="field-help">Read-only producer structure. TraderCockpit does not interpret or execute native search, ranking, selection, mutation, crossover, or genetic behavior.</p></div>${rows}</section>`;
}

export function renderResearchSpecification(specification, search = null, questions = null) {
  const questionHtml = questions ? renderClarifyingQuestions(questions) : "";
  const gate = specification?.build_gate;
  const gateValid = validBuildGate(gate);
  const questionLocked = questions?.build_gate?.locked === true;
  const locked = (gateValid ? gate.locked : true) || questionLocked;
  const gateLabel = locked ? "Build locked" : "Build requirements resolved";
  const gateTone = locked ? "unavailable" : "ready";
  const reasons = [
    ...(gateValid ? gate.reason_codes : ["invalid_or_missing_build_gate"]),
    ...((questionLocked && Array.isArray(questions.build_gate?.reason_codes)) ? questions.build_gate.reason_codes : []),
  ];
  const uniqueReasons = [...new Set(reasons)];
  const summary = `<div class="requirement-item specification-gate"><strong>Build gate</strong><span class="status-badge status-${gateTone}"><span class="status-dot"></span>${escapeHtml(gateLabel)}</span><p>${escapeHtml(uniqueReasons.join(" · ") || "No unresolved requirement reasons reported")}</p></div>`;
  const requirements = Array.isArray(specification?.requirements) ? specification.requirements : [];
  const renderedRequirements = requirements.map((requirement) => {
    const state = requirement.state || "unresolved";
    const required = requirement.required ? "Required" : "Conditional";
    return `<div class="requirement-item" data-specification-requirement="${escapeHtml(requirement.id)}"><div><strong>${escapeHtml(requirement.label)}</strong><span class="field-help">${escapeHtml(required)}</span></div><span class="status-badge status-${stateTone(state)}"><span class="status-dot"></span>${escapeHtml(readableState(state))}</span><p>${escapeHtml(requirement.detail || "")}</p>${compactValues(requirement.values)}<p class="field-help">Evidence: ${escapeHtml(requirement.evidence?.native_source_path || "native SQX")}</p></div>`;
  }).join("");
  return questionHtml + summary + renderedRequirements + renderNativeSearchConfiguration(search) + renderResearchCapabilityCoverage();
}

export function isSpecificationRoute(locationLike = globalThis.location) {
  return researchLocationMatches(locationLike, "signals", "signals");
}

let activeGrid = null;
async function bindSpecification() {
  if (!isSpecificationRoute()) return;
  const grid = globalThis.document?.querySelector(".requirement-grid");
  if (!grid || grid === activeGrid) return;
  activeGrid = grid;
  grid.innerHTML = '<div class="requirement-item"><strong>Resolving requirements…</strong><p>Reading clarifying questions and the exact native Builder configuration without launching SQX.</p></div>';
  let questions = null;
  try {
    questions = await fetchClarifyingQuestions();
  } catch {
    questions = null;
  }
  try {
    const viewModel = await fetchResearchSpecificationViewModel();
    if (grid !== activeGrid || !grid.isConnected) return;
    grid.innerHTML = renderResearchSpecification(viewModel.specification, viewModel.search, questions);
  } catch (error) {
    if (grid !== activeGrid || !grid.isConnected) return;
    const detail = error instanceof Error ? error.message : "Specification unavailable";
    const questionHtml = questions ? renderClarifyingQuestions(questions) : "";
    grid.innerHTML = `${questionHtml}<div class="requirement-item"><strong>Native Specification unavailable</strong><span class="status-badge status-unavailable"><span class="status-dot"></span>Build locked</span><p>${escapeHtml(detail)}</p></div>`;
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
  globalThis.window?.addEventListener("tradercockpit:custody-changed", () => {
    if (!isSpecificationRoute()) return;
    activeGrid = null;
    void bindSpecification();
  });
}
