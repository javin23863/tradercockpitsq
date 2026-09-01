export const RESEARCH_CAPABILITY_COVERAGE_SCHEMA = "tc.research-capability-coverage.v1";
export const RESEARCH_CAPABILITY_COVERAGE_SCOPE = "user_operable_research_workflow_and_readback";

const COVERAGE_STATES = new Set(["mapped", "partially_mapped", "unmapped"]);

const CAPABILITIES = Object.freeze([
  Object.freeze({
    id: "idea_revision_custody",
    label: "Idea revision custody",
    source_schemas: Object.freeze(["tc.research-idea.v1", "tc.research-idea-catalog.v1"]),
    api_paths: Object.freeze(["/api/research/ideas"]),
    coverage: "mapped",
    surface: "Research / Construct / Idea",
    route: "/research?stage=construct&tab=idea",
    detail: "Create, revise, select, and reopen immutable Idea revisions through canonical custody.",
  }),
  Object.freeze({
    id: "builder_native_specification",
    label: "Native Builder specification inspection",
    source_schemas: Object.freeze([
      "tc.sqx-builder-config.v1",
      "tc.sqx-builder-search.v1",
      "tc.research-specification.v1",
    ]),
    api_paths: Object.freeze(["/api/sqx-builder-config"]),
    coverage: "mapped",
    surface: "Research / Construct / Specification",
    route: "/research?stage=construct&tab=specification",
    detail: "Inspects the currently exposed native strategy/market shape, one data setup, trading-options/Blocks/money-management presence, exact native search selector plus opaque BuildMode structure, ranking stop/max, and CrossChecks state without interpreting or executing producer-owned search/genetic semantics.",
  }),
  Object.freeze({
    id: "native_preset_inspection",
    label: "Native preset inspection",
    source_schemas: Object.freeze(["tc.sqx-preset-catalog.v1", "tc.sqx-preset.v1"]),
    api_paths: Object.freeze(["/api/sqx-presets"]),
    coverage: "mapped",
    surface: "Research / Construct / Specification",
    route: "/research?stage=construct&tab=specification",
    detail: "Inspect exact source-bound native preset identities and runtime-file status without inferring that any preset is bound to the current Builder project or treating preset identity as Builder validity.",
  }),
  Object.freeze({
    id: "builder_configuration_custody",
    label: "Builder configuration custody",
    source_schemas: Object.freeze(["tc.research-configuration.v1", "tc.research-configuration-catalog.v1"]),
    api_paths: Object.freeze(["/api/research/configurations"]),
    coverage: "mapped",
    surface: "Research / Construct / Build",
    route: "/research?stage=construct&tab=build",
    detail: "Compile the exact current native Builder task snapshot, inspect immutable byte identity, and approve one exact revision.",
  }),
  Object.freeze({
    id: "native_builder_execution",
    label: "Native Builder execution",
    source_schemas: Object.freeze(["tc.research-native-job.v1", "tc.research-native-job-catalog.v1"]),
    api_paths: Object.freeze(["/api/research/native-jobs"]),
    coverage: "mapped",
    surface: "Research / Construct / Build",
    route: "/research?stage=construct&tab=build",
    detail: "Launch an approved exact Builder configuration through the trusted native gateway and retain durable submitted/failed job custody/readback.",
  }),
  Object.freeze({
    id: "native_output_candidate_import",
    label: "Native output discovery and Candidate import",
    source_schemas: Object.freeze(["tc.sqx-builder-output-list.v1", "tc.research-candidate.v1", "tc.research-candidate-catalog.v1"]),
    api_paths: Object.freeze(["/api/sqx-outputs", "/api/research/candidates"]),
    coverage: "mapped",
    surface: "Research / Construct / Candidates",
    route: "/research?stage=construct&tab=candidates",
    detail: "Inspect native Results archives and explicitly bind one exact submitted native job revision to one exact selected output archive.",
  }),
  Object.freeze({
    id: "native_historical_retester",
    label: "Native Historical Retester execution/readback",
    source_schemas: Object.freeze(["tc.research-historical-result.v1", "tc.research-historical-result-catalog.v1"]),
    api_paths: Object.freeze(["/api/research/historical-results"]),
    coverage: "mapped",
    surface: "Research / Backtest / Overview",
    route: "/research?stage=backtest&tab=overview",
    detail: "Run and reopen the canonical native Retester task-1 Historical Result lifecycle for an exact Candidate revision.",
  }),
  Object.freeze({
    id: "native_trade_rows",
    label: "Native trade-row readback",
    source_schemas: Object.freeze(["tc.research-historical-trades.v1"]),
    api_paths: Object.freeze(["/api/research/historical-results"]),
    coverage: "mapped",
    surface: "Research / Backtest / Trades",
    route: "/research?stage=backtest&tab=trades",
    detail: "Read and render exact Portfolio filled/non-control SQX orders.bin rows from one immutable completed Historical Result.",
  }),
  Object.freeze({
    id: "executed_chain_inspection",
    label: "Executed Research chain inspection",
    source_schemas: Object.freeze(["tc.research-configuration.v1", "tc.research-native-job.v1", "tc.research-candidate.v1", "tc.research-historical-result.v1"]),
    api_paths: Object.freeze(["/api/research/configurations", "/api/research/native-jobs", "/api/research/candidates", "/api/research/historical-results"]),
    coverage: "mapped",
    surface: "Research / Backtest / Configuration",
    route: "/research?stage=backtest&tab=configuration",
    detail: "Reconstruct and display the exact approved configuration → submitted Builder job → Candidate → Historical Result chain with fail-closed identity checks.",
  }),
  Object.freeze({
    id: "native_higher_precision_robustness",
    label: "Native Higher Precision robustness",
    source_schemas: Object.freeze(["tc.research-native-robustness-capabilities.v1", "tc.research-native-robustness.v1", "tc.research-native-robustness-attempt.v1", "tc.research-native-robustness-catalog.v1"]),
    api_paths: Object.freeze(["/api/research/historical-results"]),
    coverage: "mapped",
    surface: "Research / Backtest / Robustness",
    route: "/research?stage=backtest&tab=robustness",
    detail: "Inspect producer capability, execute Higher Precision through installed SQX, and reopen completed/failed/interrupted evidence without reconstructing a robustness verdict.",
  }),
  Object.freeze({
    id: "native_custom_project_topology",
    label: "Native Custom Project topology",
    source_schemas: Object.freeze(["tc.sqx-custom-project-topology.v1"]),
    api_paths: Object.freeze(["/api/sqx-project-topology"]),
    coverage: "mapped",
    surface: "Research / Construct / Specification",
    route: "/research?stage=construct&tab=specification",
    detail: "Inspect one exact saved native project by direct user/projects child name and render immutable numbered task topology plus source-proven ClearDatabanks/GoToTask details. Unknown native task kinds remain opaque and execution is not inferred.",
  }),
  Object.freeze({
    id: "research_proof",
    label: "Immutable Research Proof and reopen",
    source_schemas: Object.freeze(["tc.research-proof.v1", "tc.research-proof-catalog.v1", "tc.runtime-status.v1"]),
    api_paths: Object.freeze(["/api/research/proofs", "/api/status"]),
    coverage: "mapped",
    surface: "Research / Proof",
    route: "/research?stage=proof",
    detail: "Create and reopen the exact immutable historical Research chain while presenting mutable current product status separately.",
  }),
]);

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function readableCoverage(value) {
  return String(value || "unmapped")
    .replaceAll("_", " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export function researchCapabilityCoverageManifest() {
  const capabilities = CAPABILITIES.map((item) => ({
    ...item,
    source_schemas: [...item.source_schemas],
    api_paths: [...item.api_paths],
  }));
  return {
    schema: RESEARCH_CAPABILITY_COVERAGE_SCHEMA,
    authority: "coverage_inventory_of_canonical_backend_native_read_models",
    scope: RESEARCH_CAPABILITY_COVERAGE_SCOPE,
    capabilities,
  };
}

export function validateResearchCapabilityCoverage(payload) {
  if (
    !payload
    || payload.schema !== RESEARCH_CAPABILITY_COVERAGE_SCHEMA
    || payload.authority !== "coverage_inventory_of_canonical_backend_native_read_models"
    || payload.scope !== RESEARCH_CAPABILITY_COVERAGE_SCOPE
    || !Array.isArray(payload.capabilities)
  ) {
    throw new Error("Research capability coverage schema mismatch");
  }
  const seen = new Set();
  for (const item of payload.capabilities) {
    if (
      !item
      || typeof item.id !== "string" || !item.id
      || seen.has(item.id)
      || typeof item.label !== "string" || !item.label
      || !Array.isArray(item.source_schemas) || item.source_schemas.length === 0 || item.source_schemas.some((value) => typeof value !== "string" || !value)
      || !Array.isArray(item.api_paths) || item.api_paths.length === 0 || item.api_paths.some((value) => typeof value !== "string" || !value.startsWith("/api/"))
      || !COVERAGE_STATES.has(item.coverage)
      || typeof item.detail !== "string" || !item.detail
    ) {
      throw new Error("Research capability coverage entry is invalid");
    }
    if (item.coverage === "unmapped") {
      if (item.surface !== null || item.route !== null) throw new Error("Unmapped Research capability must not claim a desktop mapping");
    } else if (typeof item.surface !== "string" || !item.surface || typeof item.route !== "string" || !item.route) {
      throw new Error("Mapped Research capability requires an explicit desktop mapping");
    }
    seen.add(item.id);
  }
  return payload;
}

export function researchCapabilityCoverageSummary(payload = researchCapabilityCoverageManifest()) {
  const manifest = validateResearchCapabilityCoverage(payload);
  return manifest.capabilities.reduce((summary, item) => {
    summary[item.coverage] += 1;
    return summary;
  }, { mapped: 0, partially_mapped: 0, unmapped: 0 });
}

export function renderResearchCapabilityCoverage(payload = researchCapabilityCoverageManifest()) {
  const manifest = validateResearchCapabilityCoverage(payload);
  const summary = researchCapabilityCoverageSummary(manifest);
  const rows = manifest.capabilities.map((item) => {
    const tone = item.coverage === "mapped" ? "ready" : "unavailable";
    const mapping = item.coverage === "unmapped"
      ? "No desktop mapping"
      : `${item.surface} · ${item.route}`;
    return `<div class="requirement-item" data-research-capability="${escapeHtml(item.id)}"><div><strong>${escapeHtml(item.label)}</strong><span class="status-badge status-${tone}"><span class="status-dot"></span>${escapeHtml(readableCoverage(item.coverage))}</span></div><p>${escapeHtml(item.detail)}</p><div class="stat-row"><span>Desktop mapping</span><code>${escapeHtml(mapping)}</code></div><div class="stat-row"><span>Backend schemas</span><code>${escapeHtml(item.source_schemas.join(" · "))}</code></div><div class="stat-row"><span>API</span><code>${escapeHtml(item.api_paths.join(" · "))}</code></div></div>`;
  }).join("");
  return `<section data-research-capability-coverage><div class="context-callout"><span class="callout-icon">↳</span><div><span class="eyebrow">Research capability coverage</span><strong>${summary.mapped} mapped · ${summary.partially_mapped} partial · ${summary.unmapped} unmapped</strong><span>This inventory covers current user-operable Research workflow/readback capabilities exposed by canonical backend/native read models. Platform readiness/custody primitives and future producer depth without a read model are outside this slice.</span></div></div><div class="requirement-list">${rows}</div></section>`;
}
