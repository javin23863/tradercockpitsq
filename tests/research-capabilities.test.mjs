import test from "node:test";
import assert from "node:assert/strict";

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

test("Research capability manifest inventories the current user-operable read-model surface", () => {
  const manifest = validateResearchCapabilityCoverage(researchCapabilityCoverageManifest());
  assert.equal(manifest.schema, RESEARCH_CAPABILITY_COVERAGE_SCHEMA);
  assert.equal(manifest.authority, "coverage_inventory_of_canonical_backend_native_read_models");
  assert.equal(manifest.scope, RESEARCH_CAPABILITY_COVERAGE_SCOPE);
  assert.equal(manifest.scope, "user_operable_research_workflow_and_readback");
  assert.equal(manifest.capabilities.length, 12);
  assert.equal(new Set(manifest.capabilities.map((item) => item.id)).size, manifest.capabilities.length);
  assert.ok(manifest.capabilities.every((item) => item.source_schemas.length > 0));
  assert.ok(manifest.capabilities.every((item) => item.api_paths.length > 0));
});

test("Every currently exposed user-operable capability has an explicit desktop mapping", () => {
  const manifest = researchCapabilityCoverageManifest();
  const summary = researchCapabilityCoverageSummary(manifest);
  assert.deepEqual(summary, { mapped: 12, partially_mapped: 0, unmapped: 0 });
  assert.ok(manifest.capabilities.every((item) => item.coverage === "mapped"));
  assert.ok(manifest.capabilities.every((item) => item.surface && item.route));
  assert.equal(
    manifest.capabilities.find((item) => item.id === "native_preset_inspection").route,
    "/research?stage=construct&tab=specification",
  );
  assert.equal(
    manifest.capabilities.find((item) => item.id === "native_custom_project_topology").route,
    "/custom-projects",
  );
});

test("Coverage validation rejects contradictory mappings", () => {
  const duplicate = researchCapabilityCoverageManifest();
  duplicate.capabilities.push({ ...duplicate.capabilities[0] });
  assert.throws(() => validateResearchCapabilityCoverage(duplicate), /entry is invalid/);

  const missingMapping = researchCapabilityCoverageManifest();
  const topology = missingMapping.capabilities.find((item) => item.id === "native_custom_project_topology");
  topology.route = null;
  assert.throws(() => validateResearchCapabilityCoverage(missingMapping), /requires an explicit desktop mapping/);

  const wrongScope = researchCapabilityCoverageManifest();
  wrongScope.scope = "all_platform_status_primitives";
  assert.throws(() => validateResearchCapabilityCoverage(wrongScope), /schema mismatch/);
});

test("Coverage renderer reports mapped search workflows without inventing unexposed producer depth", () => {
  const html = renderResearchCapabilityCoverage();
  assert.match(html, /12 mapped · 0 partial · 0 unmapped/);
  assert.match(html, /Native preset inspection/);
  assert.match(html, /Native Custom Project workflows/);
  assert.doesNotMatch(html, /No desktop mapping/);
  assert.match(html, /user-operable Research workflow\/readback capabilities/);
  assert.match(html, /tc\.sqx-preset-catalog\.v1/);
  assert.match(html, /tc\.sqx-builder-search\.v1/);
  assert.match(html, /\/api\/sqx-project-topology/);
  assert.match(html, /Random Discovery/);
  assert.match(html, /Genetic Evolution/);
  assert.doesNotMatch(html, /indicator family|signal family|parameter editor|population editor|island controls|migration controls/i);
});

test("Specification visibly includes the current Research capability coverage inspector", () => {
  const html = renderResearchSpecification(specification());
  assert.match(html, /data-research-capability-coverage/);
  assert.match(html, /Research capability coverage/);
  assert.match(html, /native_preset_inspection/);
  assert.match(html, /native_custom_project_topology/);
});
