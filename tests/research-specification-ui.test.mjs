import test from "node:test";
import assert from "node:assert/strict";

import {
  BUILDER_SEARCH_SCHEMA,
  RESEARCH_SPECIFICATION_SCHEMA,
  isSpecificationRoute,
  producerValidityFromSpecification,
  renderProducerValidity,
  renderResearchSpecification,
  searchConfigurationFromBuilderConfig,
  specificationFromBuilderConfig,
} from "../web/research-specification.mjs";

function payload() {
  const archiveSha = "a".repeat(64);
  return {
    schema: "tc.sqx-builder-config.v1",
    source_build: "144.2953",
    project: "Builder",
    source_relative_path: "user/projects/Builder/project.cfx",
    archive_sha256: archiveSha,
    internal_entries: ["config.xml", "Build-Task1.xml"],
    search: {
      schema: BUILDER_SEARCH_SCHEMA,
      authority: "native_sqx_read_only",
      source: {
        source_build: "144.2953",
        project: "Builder",
        relative_path: "user/projects/Builder/project.cfx",
        archive_sha256: archiveSha,
        member: "Build-Task1.xml",
      },
      selector: "genetic-evolution",
      display_mode: {
        kind: "genetic_evolution",
        label: "Genetic Evolution",
        recognized: true,
      },
      producer_configuration: {
        tag: "BuildMode",
        attributes: {
          generationType: "genetic-evolution",
          futureFlag: "native",
        },
        text: null,
        children: [
          {
            tag: "GeneticOptions",
            attributes: { populationSize: "123" },
            text: null,
            children: [
              {
                tag: "UnknownNativeSetting",
                attributes: { mode: "opaque" },
                text: "producer-owned",
                children: [],
              },
            ],
          },
        ],
      },
      semantics: {
        interpreted_by_tradercockpit: false,
        owner: "StrategyQuant X",
        description: "Producer-owned BuildMode structure is reflected read-only.",
      },
      execution: {
        available: false,
        reason: "native_sqx_builder_owns_search_execution",
      },
    },
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
      producer_validity: {
        state: "not_ready_for_native_validation",
        method: "authorized_sqx_loadconfig",
        native_execution_check: "loadconfig_before_start",
        local_preflight: "requirements_incomplete",
      },
      build_gate: {
        locked: true,
        reason_codes: ["unresolved:strategy_shape", "exact_native_configuration_not_compiled"],
      },
    },
  };
}

test("Specification requires the canonical nested schema, typed local gate, and producer validity", () => {
  const specification = specificationFromBuilderConfig(payload());
  assert.equal(specification.schema, RESEARCH_SPECIFICATION_SCHEMA);
  assert.equal(specification.authority, "native_sqx_read_only");
  assert.equal(producerValidityFromSpecification(specification).state, "not_ready_for_native_validation");
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

  const missingValidity = payload();
  delete missingValidity.specification.producer_validity;
  assert.throws(
    () => specificationFromBuilderConfig(missingValidity),
    /producer validity mismatch/,
  );
});

test("Native search inspector validates exact source identity and preserves unknown producer nodes", () => {
  const sample = payload();
  const search = searchConfigurationFromBuilderConfig(sample);
  assert.equal(search.schema, BUILDER_SEARCH_SCHEMA);
  assert.equal(search.selector, "genetic-evolution");
  assert.equal(search.display_mode.label, "Genetic Evolution");
  assert.equal(search.producer_configuration.attributes.futureFlag, "native");
  assert.equal(
    search.producer_configuration.children[0].children[0].tag,
    "UnknownNativeSetting",
  );
  assert.equal(
    search.producer_configuration.children[0].children[0].text,
    "producer-owned",
  );

  const substitutedSource = payload();
  substitutedSource.search.source.archive_sha256 = "b".repeat(64);
  assert.throws(
    () => searchConfigurationFromBuilderConfig(substitutedSource),
    /Native search configuration schema mismatch/,
  );

  const substitutedSelector = payload();
  substitutedSelector.search.producer_configuration.attributes.generationType = "random-generation";
  assert.throws(
    () => searchConfigurationFromBuilderConfig(substitutedSelector),
    /Native search selector does not match BuildMode/,
  );

  const missingMember = payload();
  missingMember.internal_entries = ["config.xml"];
  assert.throws(
    () => searchConfigurationFromBuilderConfig(missingMember),
    /Builder search source identity mismatch/,
  );
});

test("Native search presentation distinguishes known modes but keeps unknown producer values opaque", () => {
  const random = payload();
  random.search.selector = "random-generation";
  random.search.display_mode = {
    kind: "random_discovery",
    label: "Random Discovery",
    recognized: true,
  };
  random.search.producer_configuration.attributes.generationType = "random-generation";
  assert.equal(searchConfigurationFromBuilderConfig(random).display_mode.label, "Random Discovery");

  const future = payload();
  future.search.selector = "future-native-search";
  future.search.display_mode = {
    kind: "native_other",
    label: "Other native search mode",
    recognized: false,
  };
  future.search.producer_configuration.attributes.generationType = "future-native-search";
  const parsed = searchConfigurationFromBuilderConfig(future);
  assert.equal(parsed.selector, "future-native-search");
  assert.equal(parsed.display_mode.recognized, false);
});

test("Specification rendering keeps unresolved local meaning and native validation refusal visible", () => {
  const html = renderResearchSpecification(payload().specification);
  assert.match(html, /Local build preflight/);
  assert.match(html, /Local requirements incomplete/);
  assert.match(html, /Not ready for native validation/);
  assert.match(html, /authorized_sqx_loadconfig/);
  assert.match(html, /Unresolved/);
  assert.match(html, /User Selected/);
  assert.match(html, /unresolved:strategy_shape/);
  assert.match(html, /exact_native_configuration_not_compiled/);
  assert.match(html, /EURUSD_dukascopy/);
  assert.doesNotMatch(html, /Pending backend mapping/);
  assert.doesNotMatch(html, /Build requirements resolved/);
});

test("Specification renders read-only native search structure without execution controls", () => {
  const sample = payload();
  const html = renderResearchSpecification(
    specificationFromBuilderConfig(sample),
    searchConfigurationFromBuilderConfig(sample),
  );
  assert.match(html, /Native Search Configuration/);
  assert.match(html, /Genetic Evolution/);
  assert.match(html, /genetic-evolution/);
  assert.match(html, /UnknownNativeSetting/);
  assert.match(html, /producer-owned/);
  assert.match(html, /does not interpret or execute native search/);
  assert.doesNotMatch(html, /<button/i);
  assert.doesNotMatch(html, /Start Genetic|Run Genetic|Start Search/i);
});

test("complete local preflight remains distinct from pending native producer validation", () => {
  const specification = payload().specification;
  specification.requirements[0].state = "producer_configured";
  specification.requirements[0].detail = "The exact native task configures this family for SQX validation during loadconfig.";
  specification.build_gate = { locked: false, reason_codes: [] };
  specification.producer_validity = {
    state: "pending_native_validation",
    method: "authorized_sqx_loadconfig",
    native_execution_check: "loadconfig_before_start",
    local_preflight: "requirements_complete",
  };

  const parsed = specificationFromBuilderConfig({
    schema: "tc.sqx-builder-config.v1",
    specification,
  });
  const html = renderResearchSpecification(parsed);
  assert.match(html, /Local requirements complete/);
  assert.match(html, /Native validation pending/);
  assert.match(html, /authorized_sqx_loadconfig/);
  assert.match(html, /Producer Configured/);
  assert.match(html, /status-ready/);
  assert.doesNotMatch(html, /Build requirements resolved/);
  assert.doesNotMatch(html, /native validated/i);
  assert.doesNotMatch(html, /producer accepted/i);
});

test("producer validity parser rejects local/native contradictions", () => {
  const contradictory = payload().specification;
  contradictory.producer_validity = {
    state: "pending_native_validation",
    method: "authorized_sqx_loadconfig",
    native_execution_check: "loadconfig_before_start",
    local_preflight: "requirements_complete",
  };
  assert.throws(
    () => producerValidityFromSpecification(contradictory),
    /producer validity mismatch/,
  );

  const changedAuthority = payload().specification;
  changedAuthority.producer_validity.method = "browser_assumed_valid";
  assert.throws(
    () => producerValidityFromSpecification(changedAuthority),
    /producer validity mismatch/,
  );
  assert.match(renderProducerValidity(changedAuthority), /Producer validity/);
  assert.match(renderProducerValidity(changedAuthority), /Unavailable/);
});

test("Malformed build gate renders local refusal rather than resolved or native accepted", () => {
  const specification = payload().specification;
  delete specification.build_gate;
  const html = renderResearchSpecification(specification);
  assert.match(html, /Local requirements incomplete/);
  assert.match(html, /invalid_or_missing_build_gate/);
  assert.match(html, /Producer validity/);
  assert.match(html, /Unavailable/);
  assert.doesNotMatch(html, /Local requirements complete/);
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
