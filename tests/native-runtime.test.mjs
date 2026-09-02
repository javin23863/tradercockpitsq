import assert from "node:assert/strict";
import test from "node:test";

import {
  parseNativeRuntimeDiscovery,
  renderNativeRuntimeSetup,
} from "../web/native-runtime.mjs";

const candidateId = `tc-sqx-home:sha256:${"ab".repeat(32)}`;

function discovery(overrides = {}) {
  return {
    schema: "tc.sqx-runtime-discovery.v1",
    expected_build: "144.2953",
    process_pinned: false,
    saved: null,
    candidates: [
      {
        candidate_id: candidateId,
        home_path: "C:/StrategyQuantX",
        label: "StrategyQuantX",
        bindable: true,
        reason_code: null,
        launcher_sha256: "a".repeat(64),
        observed_build: "144.2953",
      },
    ],
    recovery: {
      action: "bind",
      reason_code: "runtime_not_configured",
      detail: "No saved runtime. Bind a discovered SQX 144.2953 home, then reopen the desktop.",
    },
    ...overrides,
  };
}

test("native runtime parser keeps candidate_id bind-only and rejects path writes", () => {
  const parsed = parseNativeRuntimeDiscovery(discovery());
  assert.equal(parsed.candidates[0].candidate_id, candidateId);
  assert.equal(parsed.candidates[0].bindable, true);
  assert.throws(() => parseNativeRuntimeDiscovery({ ...discovery(), schema: "nope" }), /schema mismatch/);
  assert.throws(
    () => parseNativeRuntimeDiscovery(discovery({ candidates: [{ candidate_id: "C:/StrategyQuantX", home_path: "C:/StrategyQuantX", bindable: true }] })),
    /candidate identity/,
  );
});

test("native runtime setup binds by candidate_id and never offers a path input", () => {
  const html = renderNativeRuntimeSetup(parseNativeRuntimeDiscovery(discovery()));
  assert.match(html, new RegExp(`data-native-runtime-bind="${candidateId}"`));
  assert.match(html, /C:\/StrategyQuantX/);
  assert.doesNotMatch(html, /<input[^>]+type="file"/);
  assert.doesNotMatch(html, /<input[^>]+name="(?:sqx_home|home_path|path)"/);
  const pinned = renderNativeRuntimeSetup(parseNativeRuntimeDiscovery(discovery({ process_pinned: true, recovery: { action: "none", reason_code: "process_runtime_pinned", detail: "Pinned." } })));
  assert.doesNotMatch(pinned, /data-native-runtime-bind=/);
});
