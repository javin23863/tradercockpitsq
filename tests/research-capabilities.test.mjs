import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

import {
  RESEARCH_CAPABILITY_COVERAGE_SCHEMA,
  RESEARCH_CAPABILITY_COVERAGE_SCOPE,
  renderResearchCapabilityCoverage,
  researchCapabilityCoverageManifest,
  researchCapabilityCoverageSummary,
  validateResearchCapabilityCoverage,
} from "../web/research-capabilities.mjs";
import { renderResearchSpecification } from "../web/research-specification.mjs";

function specification() {
  return {
    schema: "tc.research-specification.v1",
    authority: "native_sqx_read_only",
    requirements: [],
    build_gate: { locked: false, reason_codes: [] },
  };
}

test("Research vertical manifest accounts for every current user-facing capability family", () => {
  const manifest = validateResearchCapabilityCoverage(researchCapabilityCoverageManifest());
  assert.equal(manifest.schema, RESEARCH_CAPABILITY_COVERAGE_SCHEMA);
  assert.equal(manifest.schema, "tc.research-capability-coverage.v2");
  assert.equal(manifest.authority, "research_vertical_coverage_of_canonical_backend_native_seams");
  assert.equal(manifest.scope, RESEARCH_CAPABILITY_COVERAGE_SCOPE);
  assert.equal(manifest.scope, "research_vertical_user_facing_coverage");
  assert.equal(manifest.capabilities.length, 20);
  assert.equal(new Set(manifest.capabilities.map((item) => item.id)).size, manifest.capabilities.length);

  const mapped = manifest.capabilities.filter((item) => item.coverage === "mapped");
  const unavailable = manifest.capabilities.filter((item) => item.coverage === "explicitly_unavailable");
  assert.equal(mapped.length, 12);
  assert.equal(unavailable.length, 8);
  assert.ok(mapped.every((item) => item.producer_exposure === "canonical_read_model"));
  assert.ok(mapped.every((item) => item.source_schemas.length > 0 && item.api_paths.length > 0));
  assert.ok(mapped.every((item) => item.reason_code === null));
  assert.ok(unavailable.every((item) => item.producer_exposure === "not_exposed"));
  assert.ok(unavailable.every((item) => item.source_schemas.length === 0 && item.api_paths.length === 0));
  assert.ok(unavailable.every((item) => typeof item.reason_code === "string" && item.reason_code.length > 0));
  assert.ok(manifest.capabilities.every((item) => item.desktop_modules.length > 0));
  assert.ok(manifest.capabilities.every((item) => item.surface && item.route.startsWith("/research")));
});

test("Builder mapped coverage includes every currently exposed exact native inspector", () => {
  const manifest = researchCapabilityCoverageManifest();
  const builder = manifest.capabilities.find((item) => item.id === "builder_native_specification");
  assert.ok(builder);
  for (const schema of [
    "tc.sqx-builder-search.v1",
    "tc.sqx-builder-trading-options.v1",
    "tc.sqx-builder-blocks.v1",
    "tc.sqx-builder-rankings.v1",
    "tc.sqx-builder-cross-checks.v1",
    "tc.sqx-builder-money-management.v1",
  ]) {
    assert.ok(builder.source_schemas.includes(schema), `Builder coverage includes ${schema}`);
  }
  for (const module of [
    "/research-specification.mjs",
    "/research-trading-options.mjs",
    "/research-blocks.mjs",
    "/research-rankings.mjs",
    "/research-cross-checks.mjs",
    "/research-money-management.mjs",
    "/research-native-inspector-tools.mjs",
  ]) {
    assert.ok(builder.desktop_modules.includes(module), `Builder coverage includes ${module}`);
  }
  assert.match(builder.detail, /Random Discovery and Genetic Evolution remain distinct read-only workflow lanes/);
  assert.match(builder.detail, /searched as exact text/);
});

test("Research depth that is not exposed through a canonical seam is explicitly unavailable", () => {
  const manifest = researchCapabilityCoverageManifest();
  const ids = new Map(manifest.capabilities.map((item) => [item.id, item]));
  const expected = {
    typed_rule_block_authoring: "typed_native_block_descriptor_and_write_seam_not_exposed",
    typed_search_parameter_authoring: "native_search_parameter_descriptor_and_write_seam_not_exposed",
    typed_data_trading_input_authoring: "native_data_and_trading_write_seam_not_exposed",
    typed_money_management_atm_authoring: "native_money_management_descriptor_and_write_seam_not_exposed",
    robustness_method_family_depth: "only_higher_precision_canonical_method_exposed",
    robustness_producer_outcome_readback: "authoritative_native_robustness_outcome_seam_not_exposed",
    custom_project_task_parameter_control: "generic_native_task_control_seam_not_exposed",
    historical_performance_metrics_readback: "authoritative_native_metric_readback_seam_not_exposed",
  };
  for (const [id, reason] of Object.entries(expected)) {
    const item = ids.get(id);
    assert.ok(item, `${id} is inventoried`);
    assert.equal(item.coverage, "explicitly_unavailable");
    assert.equal(item.reason_code, reason);
  }
  assert.match(ids.get("typed_rule_block_authoring").detail, /Indicator, signal, raw-indicator, operator\/comparison/);
  assert.match(ids.get("typed_search_parameter_authoring").detail, /crossover, mutation, islands, migration/);
  assert.match(ids.get("custom_project_task_parameter_control").detail, /ClearDatabanks\/GoToTask/);
  assert.match(ids.get("robustness_method_family_depth").detail, /Higher Precision is the only robustness method/);
});

test("Coverage validation rejects contradictory exposure claims", () => {
  const duplicate = researchCapabilityCoverageManifest();
  duplicate.capabilities.push({ ...duplicate.capabilities[0] });
  assert.throws(() => validateResearchCapabilityCoverage(duplicate), /entry is invalid/);

  const mappedWithoutApi = researchCapabilityCoverageManifest();
  const topology = mappedWithoutApi.capabilities.find((item) => item.id === "native_custom_project_topology");
  topology.api_paths = [];
  assert.throws(() => validateResearchCapabilityCoverage(mappedWithoutApi), /Mapped Research capability/);

  const unavailableWithSchema = researchCapabilityCoverageManifest();
  const typed = unavailableWithSchema.capabilities.find((item) => item.id === "typed_rule_block_authoring");
  typed.source_schemas = ["invented.schema"];
  assert.throws(() => validateResearchCapabilityCoverage(unavailableWithSchema), /Unavailable\/hidden Research capability/);

  const wrongScope = researchCapabilityCoverageManifest();
  wrongScope.scope = "all_platform_status_primitives";
  assert.throws(() => validateResearchCapabilityCoverage(wrongScope), /schema mismatch/);
});

test("Coverage renderer proves mapped and unavailable families without pretending future native depth exists", () => {
  const html = renderResearchCapabilityCoverage();
  assert.match(html, /12 mapped · 8 explicitly unavailable · 0 intentionally hidden/);
  assert.match(html, /Native preset inspection/);
  assert.match(html, /Native Custom Project topology/);
  assert.match(html, /Typed rule\/block families and parameter editors/);
  assert.match(html, /Typed Random Discovery \/ Genetic Evolution controls/);
  assert.match(html, /Additional producer-backed validation \/ Robustness methods/);
  assert.match(html, /Unavailable reason: typed_native_block_descriptor_and_write_seam_not_exposed/);
  assert.match(html, /Unavailable reason: only_higher_precision_canonical_method_exposed/);
  assert.match(html, /tc\.sqx-builder-trading-options\.v1/);
  assert.match(html, /tc\.sqx-builder-money-management\.v1/);
  assert.match(html, /Open owning Research surface/);
  assert.doesNotMatch(html, /unmapped/i);
});

test("Every mapped desktop module is loaded by the canonical desktop or owned by app.mjs", async () => {
  const index = await readFile(new URL("../web/index.html", import.meta.url), "utf8");
  const manifest = researchCapabilityCoverageManifest();
  const modules = new Set(manifest.capabilities.flatMap((item) => item.desktop_modules));
  for (const module of modules) {
    if (module === "/app.mjs" || module === "/research-capabilities.mjs") continue;
    assert.ok(index.includes(`src="${module}"`), `${module} is loaded by the canonical desktop`);
  }
  assert.ok(index.includes('src="/app.mjs"'));
});

test("Specification render still embeds coverage for pure rendering consumers", () => {
  const html = renderResearchSpecification(specification());
  assert.match(html, /data-research-capability-coverage/);
  assert.match(html, /Research vertical coverage/);
  assert.match(html, /native_preset_inspection/);
  assert.match(html, /typed_rule_block_authoring/);
});

test("Research coverage summary has no silently unmapped state", () => {
  assert.deepEqual(researchCapabilityCoverageSummary(), {
    mapped: 12,
    explicitly_unavailable: 8,
    intentionally_hidden: 0,
  });
});
