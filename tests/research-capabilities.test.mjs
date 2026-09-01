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
  assert.equal(manifest.authority, "canonical_backend_native_read_models");
  assert.equal(manifest.scope, RESEARCH_CAPABILITY_COVERAGE_SCOPE);
  assert.equal(manifest.scope, "user_operable_research_workflow_and_readback");
  assert.equal(manifest.capabilities.length, 12);
  assert.equal(new Set(manifest.capabilities.map((item) => item.id)).size, manifest.capabilities.length);
  assert.ok(manifest.capabilities.every((item) => item.source_schemas.length > 0));
  assert.ok(manifest.capabilities.every((item) => item.api_paths.length > 0));
});

test("Current backend-exposed desktop gaps are explicit rather than silently absent", () => {
  const manifest = researchCapabilityCoverageManifest();
  const summary = researchCapabilityCoverageSummary(manifest);
  const unmapped = manifest.capabilities.filter((item) => item.coverage === "unmapped");

  assert.deepEqual(summary, { mapped: 10, partially_mapped: 0, unmapped: 2 });
  assert.deepEqual(
    unmapped.map((item) => item.id),
    ["native_preset_inspection", "native_custom_project_topology"],
  );
  assert.ok(unmapped.every((item) => item.surface === null && item.route === null));
});

test("Coverage validation rejects hidden or contradictory mappings", () => {
  const duplicate = researchCapabilityCoverageManifest();
  duplicate.capabilities.push({ ...duplicate.capabilities[0] });
  assert.throws(() => validateResearchCapabilityCoverage(duplicate), /entry is invalid/);

  const falseMapping = researchCapabilityCoverageManifest();
  const preset = falseMapping.capabilities.find((item) => item.id === "native_preset_inspection");
  preset.surface = "Research / Construct / Specification";
  assert.throws(() => validateResearchCapabilityCoverage(falseMapping), /must not claim a desktop mapping/);

  const missingMapping = researchCapabilityCoverageManifest();
  const idea = missingMapping.capabilities.find((item) => item.id === "idea_revision_custody");
  idea.route = null;
  assert.throws(() => validateResearchCapabilityCoverage(missingMapping), /requires an explicit desktop mapping/);

  const wrongScope = researchCapabilityCoverageManifest();
  wrongScope.scope = "all_platform_status_primitives";
  assert.throws(() => validateResearchCapabilityCoverage(wrongScope), /schema mismatch/);
});

test("Coverage renderer names mapped and unmapped capabilities with backend authority", () => {
  const html = renderResearchCapabilityCoverage();
  assert.match(html, /10 mapped · 0 partial · 2 unmapped/);
  assert.match(html, /Native preset inspection/);
  assert.match(html, /Native Custom Project topology/);
  assert.match(html, /No desktop mapping/);
  assert.match(html, /user-operable Research workflow\/readback capabilities/);
  assert.match(html, /tc\.sqx-preset-catalog\.v1/);
  assert.match(html, /\/api\/sqx-project-topology/);
  assert.doesNotMatch(html, /Random Discovery|Genetic Evolution|crossover|mutation/);
});

test("Specification visibly includes the current Research capability coverage inspector", () => {
  const html = renderResearchSpecification(specification());
  assert.match(html, /data-research-capability-coverage/);
  assert.match(html, /Research capability coverage/);
  assert.match(html, /native_preset_inspection/);
  assert.match(html, /native_custom_project_topology/);
});
