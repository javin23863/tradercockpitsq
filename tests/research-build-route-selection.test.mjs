import assert from "node:assert/strict";
import test from "node:test";

import {
  configurationRouteEntity,
  configurationRouteSearch,
  configurationSelectionTarget,
} from "../web/research-build.mjs";

const first = { entity_id: "first" };
const second = { entity_id: "second" };

test("route identity restores the exact selected configuration among multiple entities", () => {
  assert.equal(
    configurationSelectionTarget([first, second], "", "", "second"),
    "second",
  );
  assert.equal(
    configurationSelectionTarget([first, second], "", "", "missing"),
    "",
  );
});

test("configuration route identity round-trips without disturbing Build route state", () => {
  const location = { search: "?workspace=evolution" };
  const search = configurationRouteSearch("tc-research:configuration:v1:abc", location);
  assert.equal(
    configurationRouteEntity({ search }),
    "tc-research:configuration:v1:abc",
  );
  const params = new URLSearchParams(search);
  assert.equal(params.get("workspace"), "evolution");
  assert.equal(params.get("configuration"), "tc-research:configuration:v1:abc");
});

test("preferred and in-memory identities win only when still present in catalog", () => {
  assert.equal(configurationSelectionTarget([first, second], "missing", "second", "first"), "second");
  assert.equal(configurationSelectionTarget([first], "missing", "missing", "missing"), "first");
});
