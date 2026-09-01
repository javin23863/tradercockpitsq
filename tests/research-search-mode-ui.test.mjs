import test from "node:test";
import assert from "node:assert/strict";

import { renderResearchSpecification } from "../web/research-specification.mjs";

function specification() {
  return {
    schema: "tc.research-specification.v1",
    authority: "native_sqx_read_only",
    requirements: [],
    build_gate: { locked: false, reason_codes: [] },
  };
}

function search(selector, kind, label, recognized) {
  return {
    selector,
    display_mode: { kind, label, recognized },
    source: { member: "Build-Task1.xml" },
    producer_configuration: {
      tag: "BuildMode",
      attributes: { generationType: selector },
      text: null,
      children: [],
    },
  };
}

test("Genetic Evolution is visibly distinct from Random Discovery", () => {
  const html = renderResearchSpecification(
    specification(),
    search("genetic-evolution", "genetic_evolution", "Genetic Evolution", true),
  );
  assert.match(html, /data-native-search-mode-lanes/);
  assert.match(html, /data-native-search-mode="random_discovery" data-selected="false"/);
  assert.match(html, /data-native-search-mode="genetic_evolution" data-selected="true"/);
  assert.match(html, /Genetic Evolution/);
  assert.match(html, /Random Discovery/);
  assert.match(html, /population, ranking, selection, crossover, mutation, islands, migration, restart/);
  assert.doesNotMatch(html, /<button|<input|<select/);
});

test("Random Discovery does not present Genetic Evolution as selected", () => {
  const html = renderResearchSpecification(
    specification(),
    search("random-generation", "random_discovery", "Random Discovery", true),
  );
  assert.match(html, /data-native-search-mode="random_discovery" data-selected="true"/);
  assert.match(html, /data-native-search-mode="genetic_evolution" data-selected="false"/);
  assert.match(html, /Genetic Evolution settings are not presented as active controls/);
  assert.doesNotMatch(html, /<button|<input|<select/);
});

test("unknown native mode remains visible without Random or Genetic promotion", () => {
  const html = renderResearchSpecification(
    specification(),
    search("future-native-search", "native_other", "Other native search mode", false),
  );
  assert.match(html, /data-native-search-mode="native_other" data-selected="true"/);
  assert.match(html, /keeps this producer mode visible without assigning Random Discovery or Genetic Evolution semantics/);
  assert.match(html, /future-native-search/);
});
