import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

import {
  nativeInspectorMatches,
  renderNativeInspectorTools,
} from "../web/research-native-inspector-tools.mjs";

test("native inspector search is case-insensitive exact-text filtering only", () => {
  const row = "BuildMode/GeneticOptions[0] populationSize 123 producer-owned";
  assert.equal(nativeInspectorMatches(row, "populationSize"), true);
  assert.equal(nativeInspectorMatches(row, "geneticoptions"), true);
  assert.equal(nativeInspectorMatches(row, "  PRODUCER-OWNED  "), true);
  assert.equal(nativeInspectorMatches(row, "mutationRate"), false);
  assert.equal(nativeInspectorMatches(row, ""), true);
});

test("native inspector tools explain the semantic boundary and expose no editor actions", () => {
  const html = renderNativeInspectorTools("future", 2, 7);
  assert.match(html, /Search exact current Builder structures/);
  assert.match(html, /Text filtering only/);
  assert.match(html, /Showing 2 of 7 exact native nodes/);
  assert.match(html, /data-native-inspector-search/);
  assert.doesNotMatch(html, /save|apply|execute|switch mode|set parameter|mutation rate|risk %/i);
});

test("canonical desktop loads the native inspector search exactly once", async () => {
  const index = await readFile(new URL("../web/index.html", import.meta.url), "utf8");
  const matches = index.match(/src="\/research-native-inspector-tools\.mjs"/g) || [];
  assert.equal(matches.length, 1);
});
