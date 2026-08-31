import test from "node:test";
import assert from "node:assert/strict";

import {
  RESEARCH_SPECIFICATION_SCHEMA,
  renderResearchSpecification,
  specificationFromBuilderConfig,
} from "../web/research-specification.mjs";

function payload() {
  return {
    schema: "tc.sqx-builder-config.v1",
    specification: {
      schema: RESEARCH_SPECIFICATION_SCHEMA,
      authority: "native_sqx_read_only",
      requirements: [
        {
          id: "strategy_shape",
          label: "Strategy shape",
          state: "unresolved",
          required: true,
          detail: "Strategy type is not resolved.",
          evidence: { native_source_path: "sources/plugins/SettingsWhatToBuild" },
          values: {},
        },
        {
          id: "market_identity",
          label: "Market identity",
          state: "user_selected",
          required: true,
          detail: "Native market facts are present.",
          evidence: { native_source_path: "user/projects/Builder/project.cfx" },
          values: { instruments: ["EURUSD_dukascopy"] },
        },
      ],
      build_gate: {
        locked: true,
        reason_codes: ["unresolved:strategy_shape", "exact_native_configuration_not_compiled"],
      },
    },
  };
}

test("Specification requires the canonical nested schema", () => {
  const specification = specificationFromBuilderConfig(payload());
  assert.equal(specification.schema, RESEARCH_SPECIFICATION_SCHEMA);
  assert.equal(specification.authority, "native_sqx_read_only");
  assert.throws(
    () => specificationFromBuilderConfig({ schema: "tc.sqx-builder-config.v1" }),
    /Research Specification schema mismatch/,
  );
});

test("Specification rendering keeps unresolved native meaning and Build lock visible", () => {
  const html = renderResearchSpecification(payload().specification);
  assert.match(html, /Build locked/);
  assert.match(html, /Unresolved/);
  assert.match(html, /User Selected/);
  assert.match(html, /unresolved:strategy_shape/);
  assert.match(html, /exact_native_configuration_not_compiled/);
  assert.match(html, /EURUSD_dukascopy/);
  assert.doesNotMatch(html, /Pending backend mapping/);
});
