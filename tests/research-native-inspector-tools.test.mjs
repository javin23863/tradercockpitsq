import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

import {
  nativeInspectorMatches,
  renderNativeInspectorTools,
  setNativeInspectorStatus,
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

test("native inspector status rendering is idempotent for MutationObserver callbacks", () => {
  let writes = 0;
  const status = {
    value: "7 exact native nodes currently visible across the loaded Builder inspectors.",
    get textContent() {
      return this.value;
    },
    set textContent(next) {
      writes += 1;
      this.value = next;
    },
  };

  assert.equal(
    setNativeInspectorStatus(status, "7 exact native nodes currently visible across the loaded Builder inspectors."),
    false,
  );
  assert.equal(writes, 0, "unchanged status must not create a new child-list mutation");

  assert.equal(setNativeInspectorStatus(status, "Showing 2 of 7 exact native nodes."), true);
  assert.equal(writes, 1);
  assert.equal(setNativeInspectorStatus(status, "Showing 2 of 7 exact native nodes."), false);
  assert.equal(writes, 1, "repeat observer passes remain mutation-free once status is settled");
});

test("canonical desktop loads the native inspector search exactly once", async () => {
  const index = await readFile(new URL("../web/index.html", import.meta.url), "utf8");
  const matches = index.match(/src="\/research-native-inspector-tools\.mjs"/g) || [];
  assert.equal(matches.length, 1);
});
