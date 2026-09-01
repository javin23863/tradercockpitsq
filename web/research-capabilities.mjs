export const RESEARCH_CAPABILITY_COVERAGE_SCHEMA = "tc.research-capability-coverage.v2";
export const RESEARCH_CAPABILITY_COVERAGE_SCOPE = "research_vertical_user_facing_coverage";

const COVERAGE_STATES = new Set(["mapped", "explicitly_unavailable", "intentionally_hidden"]);
const EXPOSURE_STATES = new Set(["canonical_read_model", "not_exposed", "non_user_facing"]);
const RESEARCH_SPECIFICATION_ROUTE = "/research?stage=construct&tab=specification";
const RESEARCH_ROBUSTNESS_ROUTE = "/research?stage=backtest&tab=robustness";

const CAPABILITIES = Object.freeze([
  Object.freeze({
    id: "idea_revision_custody",
    label: "Idea revision custody",
    producer_exposure: "canonical_read_model",
    source_schemas: Object.freeze(["tc.research-idea.v1", "tc.research-idea-catalog.v1"]),
    api_paths: Object.freeze(["/api/research/ideas"]),
    desktop_modules: Object.freeze(["/app.mjs"]),
    coverage: "mapped",
    reason_code: null,
    surface: "Research / Construct / Idea",
    route: "/research?stage=construct&tab=idea",
    detail: "Create, revise, select, and reopen immutable Idea revisions through canonical custody.",
  }),
  Object.freeze({
    id: "builder_native_specification",
    label: "Native Builder specification inspection",
    producer_exposure: "canonical_read_model",
    source_schemas: Object.freeze([
      "tc.sqx-builder-config.v1",
      "tc.sqx-builder-search.v1",
      "tc.sqx-builder-trading-options.v1",
      "tc.sqx-builder-blocks.v1",
      "tc.sqx-builder-rankings.v1",
      "tc.sqx-builder-cross-checks.v1",
      "tc.sqx-builder-money-management.v1",
      "tc.research-specification.v1",
    ]),
    api_paths: Object.freeze(["/api/sqx-builder-config"]),
    desktop_modules: Object.freeze([
      "/research-specification.mjs",
      "/research-blocks.mjs",
      "/research-rankings.mjs",
      "/research-cross-checks.mjs",
      "/research-money-management.mjs",
      "/research-trading-options.mjs",
      "/research-native-inspector-tools.mjs",
    ]),
    coverage: "mapped",
    reason_code: null,
    surface: "Research / Construct / Specification",
    route: RESEARCH_SPECIFICATION_ROUTE,
    detail: "Inspects current strategy/market/data requirements plus exact producer-owned BuildMode, BuildTradingOptions, Blocks, Rankings, CrossChecks, and MoneyManagement structures. Random Discovery and Genetic Evolution remain distinct read-only workflow lanes. Native structures can be searched as exact text without assigning indicator, parameter, search, trading, ranking, validation, sizing, or execution semantics to TraderCockpit.",
  }),
  Object.freeze({
    id: "native_preset_inspection",
    label: "Native preset inspection",
    producer_exposure: "canonical_read_model",
    source_schemas: Object.freeze(["tc.sqx-preset-catalog.v1", "tc.sqx-preset.v1"]),
    api_paths: Object.freeze(["/api/sqx-presets"]),
    desktop_modules: Object.freeze(["/research-presets.mjs"]),
    coverage: "mapped",
    reason_code: null,
    surface: "Research / Construct / Specification",
    route: RESEARCH_SPECIFICATION_ROUTE,
    detail: "Inspect exact source-bound native preset identities and runtime-file status without inferring that a preset is bound to the current Builder project or treating preset identity as Builder validity.",
  }),
  Object.freeze({
    id: "builder_configuration_custody",
    label: "Builder configuration custody",
    producer_exposure: "canonical_read_model",
    source_schemas: Object.freeze(["tc.research-configuration.v1", "tc.research-configuration-catalog.v1"]),
    api_paths: Object.freeze(["/api/research/configurations"]),
    desktop_modules: Object.freeze(["/research-build.mjs"]),
    coverage: "mapped",
    reason_code: null,
    surface: "Research / Construct / Build",
    route: "/research?stage=construct&tab=build",
    detail: "Compile the exact current native Builder task snapshot, inspect immutable byte identity, approve one exact revision, and reopen the same revision.",
  }),
  Object.freeze({
    id: "native_builder_execution",
    label: "Native Builder execution",
    producer_exposure: "canonical_read_model",
    source_schemas: Object.freeze(["tc.research-native-job.v1", "tc.research-native-job-catalog.v1"]),
    api_paths: Object.freeze(["/api/research/native-jobs"]),
    desktop_modules: Object.freeze(["/research-build-launch.mjs"]),
    coverage: "mapped",
    reason_code: null,
    surface: "Research / Construct / Build",
    route: "/research?stage=construct&tab=build",
    detail: "Launch an approved exact Builder configuration through the trusted native gateway and retain durable submitted/failed control custody and readback.",
  }),
  Object.freeze({
    id: "native_output_candidate_import",
    label: "Native output discovery and Candidate import",
    producer_exposure: "canonical_read_model",
    source_schemas: Object.freeze(["tc.sqx-builder-output-list.v1", "tc.research-candidate.v1", "tc.research-candidate-catalog.v1"]),
    api_paths: Object.freeze(["/api/sqx-outputs", "/api/research/candidates"]),
    desktop_modules: Object.freeze(["/research-candidates.mjs"]),
    coverage: "mapped",
    reason_code: null,
    surface: "Research / Construct / Candidates",
    route: "/research?stage=construct&tab=candidates",
    detail: "Inspect native Results archives and explicitly bind one exact submitted native job revision to one exact selected output archive using operator_selected_exact_native_output provenance.",
  }),
  Object.freeze({
    id: "native_historical_retester",
    label: "Native Historical Retester execution/readback",
    producer_exposure: "canonical_read_model",
    source_schemas: Object.freeze(["tc.research-historical-result.v1", "tc.research-historical-result-catalog.v1"]),
    api_paths: Object.freeze(["/api/research/historical-results"]),
    desktop_modules: Object.freeze(["/research-backtest.mjs"]),
    coverage: "mapped",
    reason_code: null,
    surface: "Research / Backtest / Overview",
    route: "/research?stage=backtest&tab=overview",
    detail: "Run and reopen the canonical native Retester task-1 Historical Result lifecycle for an exact Candidate revision without fabricating performance metrics.",
  }),
  Object.freeze({
    id: "native_trade_rows",
    label: "Native trade-row readback",
    producer_exposure: "canonical_read_model",
    source_schemas: Object.freeze(["tc.research-historical-trades.v1"]),
    api_paths: Object.freeze(["/api/research/historical-results"]),
    desktop_modules: Object.freeze(["/research-backtest-trades.mjs"]),
    coverage: "mapped",
    reason_code: null,
    surface: "Research / Backtest / Trades",
    route: "/research?stage=backtest&tab=trades",
    detail: "Read and render exact Portfolio filled/non-control SQX orders.bin rows from one immutable completed Historical Result.",
  }),
  Object.freeze({
    id: "executed_chain_inspection",
    label: "Executed Research chain inspection",
    producer_exposure: "canonical_read_model",
    source_schemas: Object.freeze(["tc.research-configuration.v1", "tc.research-native-job.v1", "tc.research-candidate.v1", "tc.research-historical-result.v1"]),
    api_paths: Object.freeze(["/api/research/configurations", "/api/research/native-jobs", "/api/research/candidates", "/api/research/historical-results"]),
    desktop_modules: Object.freeze(["/research-backtest-configuration.mjs"]),
    coverage: "mapped",
    reason_code: null,
    surface: "Research / Backtest / Configuration",
    route: "/research?stage=backtest&tab=configuration",
    detail: "Reconstruct and display the exact approved configuration → submitted Builder job → Candidate → Historical Result chain with fail-closed identity checks.",
  }),
  Object.freeze({
    id: "native_higher_precision_robustness",
    label: "Native Higher Precision robustness",
    producer_exposure: "canonical_read_model",
    source_schemas: Object.freeze(["tc.research-native-robustness-capabilities.v1", "tc.research-native-robustness.v1", "tc.research-native-robustness-attempt.v1", "tc.research-native-robustness-catalog.v1"]),
    api_paths: Object.freeze(["/api/research/historical-results"]),
    desktop_modules: Object.freeze(["/research-backtest-robustness.mjs"]),
    coverage: "mapped",
    reason_code: null,
    surface: "Research / Backtest / Robustness",
    route: RESEARCH_ROBUSTNESS_ROUTE,
    detail: "Inspect producer capability, execute Higher Precision through installed SQX, and reopen completed/failed/interrupted evidence without reconstructing a robustness verdict.",
  }),
  Object.freeze({
    id: "native_custom_project_topology",
    label: "Native Custom Project topology",
    producer_exposure: "canonical_read_model",
    source_schemas: Object.freeze(["tc.sqx-custom-project-topology.v1"]),
    api_paths: Object.freeze(["/api/sqx-project-topology"]),
    desktop_modules: Object.freeze(["/research-custom-project.mjs"]),
    coverage: "mapped",
    reason_code: null,
    surface: "Research / Construct / Specification",
    route: RESEARCH_SPECIFICATION_ROUTE,
    detail: "Inspect one exact saved native project by direct user/projects child name and render immutable numbered task topology plus source-proven ClearDatabanks/GoToTask details. Unknown native task kinds remain opaque and execution is not inferred.",
  }),
  Object.freeze({
    id: "research_proof",
    label: "Immutable Research Proof and reopen",
    producer_exposure: "canonical_read_model",
    source_schemas: Object.freeze(["tc.research-proof.v1", "tc.research-proof-catalog.v1", "tc.runtime-status.v1"]),
    api_paths: Object.freeze(["/api/research/proofs", "/api/status"]),
    desktop_modules: Object.freeze(["/research-proof.mjs"]),
    coverage: "mapped",
    reason_code: null,
    surface: "Research / Proof",
    route: "/research?stage=proof",
    detail: "Create and reopen the exact immutable historical Research chain while presenting mutable current product status separately.",
  }),
  Object.freeze({
    id: "typed_rule_block_authoring",
    label: "Typed rule/block families and parameter editors",
    producer_exposure: "not_exposed",
    source_schemas: Object.freeze([]),
    api_paths: Object.freeze([]),
    desktop_modules: Object.freeze(["/research-capabilities.mjs"]),
    coverage: "explicitly_unavailable",
    reason_code: "typed_native_block_descriptor_and_write_seam_not_exposed",
    surface: "Research / Construct / Specification",
    route: RESEARCH_SPECIFICATION_ROUTE,
    detail: "Indicator, signal, raw-indicator, operator/comparison, price/candle, time-condition, order-action, exit, custom-block, weighted/ranged/selected/nested/dependent parameter authoring is not exposed through a canonical typed descriptor/write seam. The current exact Blocks tree remains inspectable and searchable without inventing taxonomy or editors.",
  }),
  Object.freeze({
    id: "typed_search_parameter_authoring",
    label: "Typed Random Discovery / Genetic Evolution controls",
    producer_exposure: "not_exposed",
    source_schemas: Object.freeze([]),
    api_paths: Object.freeze([]),
    desktop_modules: Object.freeze(["/research-capabilities.mjs"]),
    coverage: "explicitly_unavailable",
    reason_code: "native_search_parameter_descriptor_and_write_seam_not_exposed",
    surface: "Research / Construct / Specification",
    route: RESEARCH_SPECIFICATION_ROUTE,
    detail: "The canonical seam exposes the exact native selector and BuildMode structure read-only, but not a typed write contract for population, generations, crossover, mutation, islands, migration, starting population, decimation, diversity, duplicate handling, fresh blood, weakest replacement, cadence, restart/stagnation, or final-generation settings.",
  }),
  Object.freeze({
    id: "typed_data_trading_input_authoring",
    label: "Typed data, backtest-input, session and trading-option authoring",
    producer_exposure: "not_exposed",
    source_schemas: Object.freeze([]),
    api_paths: Object.freeze([]),
    desktop_modules: Object.freeze(["/research-capabilities.mjs"]),
    coverage: "explicitly_unavailable",
    reason_code: "native_data_and_trading_write_seam_not_exposed",
    surface: "Research / Construct / Specification",
    route: RESEARCH_SPECIFICATION_ROUTE,
    detail: "Current chart/symbol/timeframe, historical range, precision, spread, slippage, commission presence, and exact BuildTradingOptions are inspectable. A typed producer-backed write seam for those settings, session controls, dependencies, or defaults is not exposed.",
  }),
  Object.freeze({
    id: "typed_money_management_atm_authoring",
    label: "Typed money-management / ATM authoring",
    producer_exposure: "not_exposed",
    source_schemas: Object.freeze([]),
    api_paths: Object.freeze([]),
    desktop_modules: Object.freeze(["/research-capabilities.mjs"]),
    coverage: "explicitly_unavailable",
    reason_code: "native_money_management_descriptor_and_write_seam_not_exposed",
    surface: "Research / Construct / Specification",
    route: RESEARCH_SPECIFICATION_ROUTE,
    detail: "The exact MoneyManagement subtree is inspectable and searchable. No canonical typed producer contract currently exposes sizing models, ATM rules, lot/risk semantics, compounding, dependencies, parameter ranges, or writes.",
  }),
  Object.freeze({
    id: "robustness_method_family_depth",
    label: "Additional producer-backed validation / Robustness methods",
    producer_exposure: "not_exposed",
    source_schemas: Object.freeze([]),
    api_paths: Object.freeze([]),
    desktop_modules: Object.freeze(["/research-capabilities.mjs"]),
    coverage: "explicitly_unavailable",
    reason_code: "only_higher_precision_canonical_method_exposed",
    surface: "Research / Backtest / Robustness",
    route: RESEARCH_ROBUSTNESS_ROUTE,
    detail: "Higher Precision is the only robustness method currently connected through the canonical producer-backed capability seam. Other native validation families remain unavailable until their exact executable configuration and readback are exposed and verified.",
  }),
  Object.freeze({
    id: "robustness_producer_outcome_readback",
    label: "Producer-authored robustness outcome interpretation",
    producer_exposure: "not_exposed",
    source_schemas: Object.freeze([]),
    api_paths: Object.freeze([]),
    desktop_modules: Object.freeze(["/research-capabilities.mjs"]),
    coverage: "explicitly_unavailable",
    reason_code: "authoritative_native_robustness_outcome_seam_not_exposed",
    surface: "Research / Backtest / Robustness",
    route: RESEARCH_ROBUSTNESS_ROUTE,
    detail: "Completed native robustness evidence is captured, but no authoritative producer outcome parser is connected. TraderCockpit therefore keeps producer_result_captured_outcome_unread visible instead of calculating its own verdict.",
  }),
  Object.freeze({
    id: "custom_project_task_parameter_control",
    label: "Generic Custom Project task parameters, dependencies and execution controls",
    producer_exposure: "not_exposed",
    source_schemas: Object.freeze([]),
    api_paths: Object.freeze([]),
    desktop_modules: Object.freeze(["/research-capabilities.mjs"]),
    coverage: "explicitly_unavailable",
    reason_code: "generic_native_task_control_seam_not_exposed",
    surface: "Research / Construct / Specification",
    route: RESEARCH_SPECIFICATION_ROUTE,
    detail: "The current topology seam proves numbered task identity and source-proven ClearDatabanks/GoToTask details only. Generic task parameters, dependency resolution, mutation controls, and execution are not exposed and are not reconstructed by TraderCockpit.",
  }),
  Object.freeze({
    id: "historical_performance_metrics_readback",
    label: "Producer-authored historical performance metric readback",
    producer_exposure: "not_exposed",
    source_schemas: Object.freeze([]),
    api_paths: Object.freeze([]),
    desktop_modules: Object.freeze(["/research-capabilities.mjs"]),
    coverage: "explicitly_unavailable",
    reason_code: "authoritative_native_metric_readback_seam_not_exposed",
    surface: "Research / Backtest / Overview",
    route: "/research?stage=backtest&tab=overview",
    detail: "Historical Result custody and native trade rows are connected, but the canonical seam does not yet expose authoritative producer performance metrics. Backtest Overview therefore does not fabricate profit, drawdown, fitness, or pass/fail values from archive presence.",
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
  return String(value || "explicitly_unavailable")
    .replaceAll("_", " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export function researchCapabilityCoverageManifest() {
  const capabilities = CAPABILITIES.map((item) => ({
    ...item,
    source_schemas: [...item.source_schemas],
    api_paths: [...item.api_paths],
    desktop_modules: [...item.desktop_modules],
  }));
  return {
    schema: RESEARCH_CAPABILITY_COVERAGE_SCHEMA,
    authority: "research_vertical_coverage_of_canonical_backend_native_seams",
    scope: RESEARCH_CAPABILITY_COVERAGE_SCOPE,
    capabilities,
  };
}

export function validateResearchCapabilityCoverage(payload) {
  if (
    !payload
    || payload.schema !== RESEARCH_CAPABILITY_COVERAGE_SCHEMA
    || payload.authority !== "research_vertical_coverage_of_canonical_backend_native_seams"
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
      || !EXPOSURE_STATES.has(item.producer_exposure)
      || !Array.isArray(item.source_schemas) || item.source_schemas.some((value) => typeof value !== "string" || !value)
      || !Array.isArray(item.api_paths) || item.api_paths.some((value) => typeof value !== "string" || !value.startsWith("/api/"))
      || !Array.isArray(item.desktop_modules) || item.desktop_modules.length === 0 || item.desktop_modules.some((value) => typeof value !== "string" || !value.startsWith("/"))
      || !COVERAGE_STATES.has(item.coverage)
      || typeof item.surface !== "string" || !item.surface
      || typeof item.route !== "string" || !item.route.startsWith("/research")
      || typeof item.detail !== "string" || !item.detail
    ) {
      throw new Error("Research capability coverage entry is invalid");
    }
    if (item.coverage === "mapped") {
      if (
        item.producer_exposure !== "canonical_read_model"
        || item.reason_code !== null
        || item.source_schemas.length === 0
        || item.api_paths.length === 0
      ) {
        throw new Error("Mapped Research capability must bind one canonical exposed read model");
      }
    } else if (
      typeof item.reason_code !== "string" || !item.reason_code
      || item.producer_exposure === "canonical_read_model"
      || item.source_schemas.length !== 0
      || item.api_paths.length !== 0
    ) {
      throw new Error("Unavailable/hidden Research capability must carry one non-exposure reason");
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
  }, { mapped: 0, explicitly_unavailable: 0, intentionally_hidden: 0 });
}

export function renderResearchCapabilityCoverage(payload = researchCapabilityCoverageManifest()) {
  const manifest = validateResearchCapabilityCoverage(payload);
  const summary = researchCapabilityCoverageSummary(manifest);
  const rows = manifest.capabilities.map((item) => {
    const mapped = item.coverage === "mapped";
    const tone = mapped ? "ready" : "unavailable";
    const authority = mapped
      ? `${item.source_schemas.join(" · ")} · ${item.api_paths.join(" · ")}`
      : `Unavailable reason: ${item.reason_code}`;
    return `<div class="requirement-item" data-research-capability="${escapeHtml(item.id)}" data-research-capability-coverage="${escapeHtml(item.coverage)}" data-producer-exposure="${escapeHtml(item.producer_exposure)}"><div><strong>${escapeHtml(item.label)}</strong><span class="status-badge status-${tone}"><span class="status-dot"></span>${escapeHtml(readableCoverage(item.coverage))}</span></div><p>${escapeHtml(item.detail)}</p><div class="stat-row"><span>Owning desktop surface</span><code>${escapeHtml(item.surface)}</code></div><div class="stat-row"><span>Coverage authority</span><code>${escapeHtml(authority)}</code></div><div class="stat-row"><span>Desktop modules</span><code>${escapeHtml(item.desktop_modules.join(" · "))}</code></div><a class="field-help" href="${escapeHtml(item.route)}" data-route="${escapeHtml(item.route)}">Open owning Research surface →</a></div>`;
  }).join("");
  return `<section data-research-capability-coverage data-research-capability-schema="${escapeHtml(manifest.schema)}"><div class="context-callout"><span class="callout-icon">↳</span><div><span class="eyebrow">Research vertical coverage</span><strong>${summary.mapped} mapped · ${summary.explicitly_unavailable} explicitly unavailable · ${summary.intentionally_hidden} intentionally hidden</strong><span>Every current user-facing Research capability family is accounted for. Canonical producer-backed seams are mapped to the desktop; deeper native behavior without a typed/readback/write seam is shown explicitly unavailable rather than silently omitted or recreated in TraderCockpit.</span></div></div><div class="requirement-list">${rows}</div></section>`;
}

function specificationRoute() {
  if (globalThis.location?.pathname !== "/research") return false;
  const params = new URLSearchParams(globalThis.location.search || "");
  return params.get("stage") === "construct" && params.get("tab") === "specification";
}

let coverageGrid = null;
let coverageWorkspace = null;

export function ensureResearchCapabilityCoverage(documentLike = globalThis.document) {
  if (!documentLike || !specificationRoute()) return null;
  const grid = documentLike.querySelector?.(".requirement-grid");
  if (!grid) return null;
  if (!coverageWorkspace?.isConnected || coverageGrid !== grid) {
    coverageGrid = grid;
    coverageWorkspace = documentLike.createElement("div");
    coverageWorkspace.dataset.researchCapabilityCoverageWorkspace = "ready";
    coverageWorkspace.innerHTML = renderResearchCapabilityCoverage();
    grid.insertAdjacentElement("afterend", coverageWorkspace);
  }
  for (const duplicate of grid.querySelectorAll?.("[data-research-capability-coverage]") || []) {
    duplicate.remove();
  }
  return coverageWorkspace;
}

if (typeof document !== "undefined") {
  const observer = new MutationObserver(() => {
    if (!specificationRoute()) {
      coverageGrid = null;
      coverageWorkspace = null;
      return;
    }
    ensureResearchCapabilityCoverage();
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  ensureResearchCapabilityCoverage();
}
