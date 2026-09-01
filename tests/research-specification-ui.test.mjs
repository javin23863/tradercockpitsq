import test from "node:test";
import assert from "node:assert/strict";

import {
  RESEARCH_SPECIFICATION_SCHEMA,
  isSpecificationRoute,
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

test("Specification requires the canonical nested schema and typed build gate", () => {
  const specification = specificationFromBuilderConfig(payload());
  assert.equal(specification.schema, RESEARCH_SPECIFICATION_SCHEMA);
  assert.equal(specification.authority, "native_sqx_read_only");
  assert.throws(
    () => specificationFromBuilderConfig({ schema: "tc.sqx-builder-config.v1" }),
    /Research Specification schema mismatch/,
  );

  const missingGate = payload();
  delete missingGate.specification.build_gate;
  assert.throws(
    () => specificationFromBuilderConfig(missingGate),
    /Research Specification build gate mismatch/,
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

test("producer-configured native state renders resolved Build authority as ready", () => {
  const specification = payload().specification;
  specification.requirements[0].state = "producer_configured";
  specification.requirements[0].detail = "The exact native task configures this family for SQX validation during loadconfig.";
  specification.build_gate = { locked: false, reason_codes: [] };

  const parsed = specificationFromBuilderConfig({
    schema: "tc.sqx-builder-config.v1",
    specification,
  });
  const html = renderResearchSpecification(parsed);
  assert.match(html, /Build requirements resolved/);
  assert.match(html, /Producer Configured/);
  assert.match(html, /status-ready/);
  assert.doesNotMatch(html, /Build locked/);
  assert.doesNotMatch(html, /status-unavailable[^>]*><span class="status-dot"><\/span>Producer Configured/);
});

test("Malformed build gate renders locked rather than resolved", () => {
  const specification = payload().specification;
  delete specification.build_gate;
  const html = renderResearchSpecification(specification);
  assert.match(html, /Build locked/);
  assert.match(html, /invalid_or_missing_build_gate/);
  assert.doesNotMatch(html, /Build requirements resolved/);
});

test("Specification binds only to the canonical query-based Construct route", () => {
  assert.equal(
    isSpecificationRoute({ pathname: "/research", search: "?stage=construct&tab=specification" }),
    true,
  );
  assert.equal(
    isSpecificationRoute({ pathname: "/research", search: "?tab=specification&stage=construct" }),
    true,
  );
  assert.equal(
    isSpecificationRoute({ pathname: "/research/construct/specification", search: "" }),
    false,
  );
  assert.equal(
    isSpecificationRoute({ pathname: "/research", search: "?stage=construct&tab=idea" }),
    false,
  );
});
