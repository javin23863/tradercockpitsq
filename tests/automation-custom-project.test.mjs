import test from "node:test";
import assert from "node:assert/strict";
import {
  customProjectControlFromPayload,
  fetchCustomProjectControl,
} from "../web/automation-custom-project.mjs";

function controlPayload(overrides = {}) {
  return {
    schema: "tc.sqx-custom-project-control.v1",
    source_build: "144.2953",
    project: "PortfolioComposer",
    project_sha256: "a".repeat(64),
    source_relative_path: "user/projects/PortfolioComposer/project.cfx",
    execution: { available: true, reason_code: null, detail: "ready" },
    control: { live: false, pid: null, run_enabled: true, stop_enabled: false },
    schedule: {
      enabled: false,
      reason_code: "native_schedule_action_unavailable",
      detail: "Native sqcli -project exposes action=start and action=stop only.",
    },
    ...overrides,
  };
}

test("Custom Project control parser rejects malformed records", () => {
  assert.throws(() => customProjectControlFromPayload(null));
  assert.throws(() => customProjectControlFromPayload(controlPayload({ control: { live: false } })));
});

test("Custom Project control fetch binds one exact project to the canonical API", async () => {
  let requested = "";
  const result = await fetchCustomProjectControl("PortfolioComposer", async (path) => {
    requested = path;
    return { ok: true, json: async () => controlPayload() };
  });
  assert.equal(requested, "/api/sqx-project-control?project=PortfolioComposer");
  assert.equal(result.project, "PortfolioComposer");
  assert.equal(result.control.run_enabled, true);
});
