import test from "node:test";
import assert from "node:assert/strict";

import { productWindowTitle, synchronizeProductWindowTitle } from "../web/window-title.mjs";

function shell(attributes) {
  return {
    getAttribute(name) {
      return attributes[name] ?? null;
    },
  };
}

test("Research window title is derived from the rendered canonical shell route", () => {
  const research = shell({
    "data-product-shell": "tradercockpit-desktop",
    "data-surface-id": "research",
    "data-research-stage-id": "construct",
    "data-research-tab-id": "idea",
  });
  assert.equal(productWindowTitle(research), "TraderCockpit — Research / Construct / Idea");

  const proof = shell({
    "data-product-shell": "tradercockpit-desktop",
    "data-surface-id": "research",
    "data-research-stage-id": "proof",
    "data-research-tab-id": "",
  });
  assert.equal(productWindowTitle(proof), "TraderCockpit — Research / Proof");
});

test("non-Research or invalid shell never fabricates a Research title", () => {
  assert.equal(productWindowTitle(null), "TraderCockpit");
  assert.equal(productWindowTitle(shell({ "data-product-shell": "other", "data-surface-id": "research" })), "TraderCockpit");
  assert.equal(productWindowTitle(shell({ "data-product-shell": "tradercockpit-desktop", "data-surface-id": "home" })), "TraderCockpit");
});

test("document synchronization exposes only the actual rendered shell state", () => {
  const documentLike = {
    title: "stale",
    querySelector() {
      return shell({
        "data-product-shell": "tradercockpit-desktop",
        "data-surface-id": "research",
        "data-research-stage-id": "backtest",
        "data-research-tab-id": "robustness",
      });
    },
  };
  assert.equal(synchronizeProductWindowTitle(documentLike), "TraderCockpit — Research / Backtest / Robustness");
  assert.equal(documentLike.title, "TraderCockpit — Research / Backtest / Robustness");
});
