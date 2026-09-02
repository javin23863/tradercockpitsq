import test from "node:test";
import assert from "node:assert/strict";
import {
  approvedCatalogId,
  renderOperators,
  viewFromApprovedConfiguration,
} from "../web/research-evolution.mjs";

const digest = "a".repeat(64);

function configuration({ state = "approved", selector = "genetic-evolution", kind = "genetic_evolution", label = "Genetic Evolution" } = {}) {
  return {
    schema: "tc.research-configuration.v1",
    executable_xml_sha256: digest,
    approval: { approved: state === "approved", approved_from_revision: state === "approved" ? "parent" : null },
    search: {
      schema: "tc.sqx-builder-search.v1",
      authority: "native_sqx_read_only",
      selector,
      display_mode: { kind, label, recognized: true },
      source: { configuration_state: state, executable_xml_sha256: digest, member: "Build-Task1.xml" },
      producer_configuration: {
        tag: "BuildMode",
        attributes: { generationType: selector },
        text: null,
        children: [{ tag: "CrossoverProbability", attributes: {}, text: "80", children: [] }],
      },
    },
    rankings: { schema: "tc.sqx-builder-rankings.v1", producer_configuration: { tag: "Rankings", attributes: {}, text: null, children: [] } },
  };
}

test("approved catalog prefers an explicit approved identity", () => {
  const catalog = {
    configurations: [
      { entity_id: "one", state: "compiled" },
      { entity_id: "two", state: "approved" },
      { entity_id: "three", state: "approved" },
    ],
  };
  assert.equal(approvedCatalogId(catalog, "two"), "two");
  assert.equal(approvedCatalogId(catalog, "one"), "three");
  assert.equal(approvedCatalogId({ configurations: [{ entity_id: "one", state: "compiled" }] }, "one"), "");
});

test("search controls require the approved executable binding", () => {
  const view = viewFromApprovedConfiguration(configuration());
  assert.equal(view.search.display_mode.kind, "genetic_evolution");
  assert.match(renderOperators(view), /Crossover probability/);

  const random = viewFromApprovedConfiguration(configuration({
    selector: "random-generation",
    kind: "random_discovery",
    label: "Random Discovery",
  }));
  assert.match(renderOperators(random), /Genetic Evolution operators not selected/);

  assert.throws(
    () => viewFromApprovedConfiguration(configuration({ state: "compiled" })),
    /approved configuration/,
  );
});
