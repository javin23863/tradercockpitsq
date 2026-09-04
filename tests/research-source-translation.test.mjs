import test from "node:test";
import assert from "node:assert/strict";

import {
  SOURCE_TRANSLATION_API_PATH,
  fetchSourceTranslations,
  renderStrategyResults,
  requestSourceTranslation,
  translationSummary,
} from "../web/automation-results.mjs";

function topology() {
  return {
    schema: "tc.sqx-custom-project-topology.v1",
    project: "Example Workflow",
    tasks: [{ number: 1, name: "Build strategies", kind: "Build", settings: [] }],
    databanks: ["Results"],
  };
}

function strategy() {
  return {
    project: "Example Workflow",
    databank: "Results",
    archive: "Example.sqx",
    result_key: "Portfolio",
    source: { state: "available", text: "<StrategyFile/>", language: "Strategy XML", member: "strategy_Portfolio.xml" },
    results_plugins: [],
  };
}

function record(overrides = {}) {
  return {
    schema: "tc.research-source-translation.v1",
    id: "abc123",
    status: "unverified_translation",
    created_at: "2026-09-04T16:00:00Z",
    target: { id: "pine_v6", label: "Pine Script v6 (TradingView)", extension: "pine" },
    native: { project: "Example Workflow", databank: "Results", archive: "Example.sqx", pseudo_sha256: "0123456789abcdef0123" },
    model: { requested: "z-ai/glm-5.3-flash", used: "z-ai/glm-5.3-flash", fallback_used: false },
    code: "//@version=6\nstrategy('x')",
    untranslatable_markers: 1,
    verification: { state: "not_verified", detail: "Backtest it in TradingView Strategy Tester before use." },
    ...overrides,
  };
}

test("Source code view carries the Deliver panel and keeps native MT4/MT5 output as producer truth", () => {
  const html = renderStrategyResults(topology(), strategy(), { task: 1, databank: "Results", archive: "Example.sqx", resultView: "source" });
  assert.match(html, /data-source-delivery/);
  assert.match(html, /data-delivery-state="idle"/);
  assert.match(html, /Deliver to TradingView \/ Python/);
  assert.match(html, /not native StrategyQuant X outputs/);
  assert.match(html, /<strong>unverified<\/strong>/);
  assert.match(html, /data-delivery-target disabled/);
  assert.match(html, /data-delivery-translate disabled/);
  assert.match(html, /data-delivery-code hidden/);
  assert.doesNotMatch(html, /Pine Script v6/, "targets come from the backend catalog, not the markup");
});

test("Translation catalog fetch validates the backend schema and passes the exact native identity", async () => {
  let requested = "";
  const payload = await fetchSourceTranslations("Example Workflow", "Results", "Example.sqx", async (path) => {
    requested = path;
    return {
      ok: true,
      status: 200,
      json: async () => ({
        schema: "tc.research-source-translation-catalog.v1",
        translation_targets: [{ id: "pine_v6", label: "Pine Script v6 (TradingView)" }],
        translations: [],
        assistant: { configured: true, model: "z-ai/glm-5.3-flash" },
        data_root_bound: true,
      }),
    };
  });
  assert.equal(requested, `${SOURCE_TRANSLATION_API_PATH}?project=Example+Workflow&databank=Results&archive=Example.sqx`);
  assert.equal(payload.translation_targets[0].id, "pine_v6");
  await assert.rejects(
    () => fetchSourceTranslations("P", "D", "A.sqx", async () => ({ ok: true, status: 200, json: async () => ({ schema: "other" }) })),
    /invalid/,
  );
});

test("Translation request posts one target and refuses records that are not unverified translations", async () => {
  let body = "";
  const stored = await requestSourceTranslation("Example Workflow", "Results", "Example.sqx", "pine_v6", async (path, options) => {
    assert.equal(path, SOURCE_TRANSLATION_API_PATH);
    assert.equal(options.method, "POST");
    body = options.body;
    return { ok: true, status: 200, json: async () => record() };
  });
  assert.deepEqual(JSON.parse(body), { project: "Example Workflow", databank: "Results", archive: "Example.sqx", target: "pine_v6" });
  assert.equal(stored.id, "abc123");
  await assert.rejects(
    () => requestSourceTranslation("P", "D", "A.sqx", "pine_v6", async () => ({ ok: true, status: 200, json: async () => record({ status: "verified" }) })),
    /invalid/,
  );
  await assert.rejects(
    () => requestSourceTranslation("P", "D", "A.sqx", "pine_v6", async () => ({
      ok: false,
      status: 503,
      json: async () => ({ reason_code: "source_translation_native_unavailable", detail: "StrategyQuant X must be running to print the native Pseudo Code" }),
    })),
    /StrategyQuant X must be running/,
  );
});

test("Translation summary names target, native source hash, model, and untranslatable gaps", () => {
  const summary = translationSummary(record());
  assert.match(summary, /Pine Script v6 \(TradingView\)/);
  assert.match(summary, /native 0123456789ab…/);
  assert.match(summary, /z-ai\/glm-5\.3-flash/);
  assert.match(summary, /1 TC-UNTRANSLATABLE/);
  assert.doesNotMatch(translationSummary(record({ untranslatable_markers: 0 })), /TC-UNTRANSLATABLE/);
});
