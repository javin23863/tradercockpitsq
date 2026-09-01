import test from "node:test";
import assert from "node:assert/strict";

import {
  customProjectTopologyFromPayload,
  fetchCustomProjectTopology,
  renderCustomProjectTopologyResult,
} from "../web/research-custom-project.mjs";

function topology() {
  return {
    schema: "tc.sqx-custom-project-topology.v1",
    source_build: "144.2953",
    project: "Custom Project A",
    source_relative_path: "user/projects/Custom Project A/project.cfx",
    archive_sha256: "a".repeat(64),
    internal_entries: ["config.xml", "Build-Task1.xml", "SomeNativeTask-Task2.xml", "ClearDatabanks-Task3.xml", "GoToTask-Task4.xml"],
    tasks: [
      { native_task_index: 1, kind: "Build", entry_name: "Build-Task1.xml", clear_databanks: [], goto_target_label: null },
      { native_task_index: 2, kind: "SomeNativeTask", entry_name: "SomeNativeTask-Task2.xml", clear_databanks: [], goto_target_label: null },
      { native_task_index: 3, kind: "ClearDatabanks", entry_name: "ClearDatabanks-Task3.xml", clear_databanks: ["Results"], goto_target_label: null },
      { native_task_index: 4, kind: "GoToTask", entry_name: "GoToTask-Task4.xml", clear_databanks: [], goto_target_label: "Build strategies" },
    ],
    execution: { supported: false, reason: "topology_custody_only" },
  };
}

test("Custom Project parser preserves generic native tasks without inventing semantics", () => {
  const parsed = customProjectTopologyFromPayload(topology());
  assert.equal(parsed.project, "Custom Project A");
  assert.equal(parsed.tasks[1].kind, "SomeNativeTask");
  assert.deepEqual(parsed.tasks[2].clear_databanks, ["Results"]);
  assert.equal(parsed.tasks[3].goto_target_label, "Build strategies");
});

test("Custom Project parser rejects substituted identities and malformed task topology", () => {
  const wrongPath = topology();
  wrongPath.source_relative_path = "user/projects/Other/project.cfx";
  assert.throws(() => customProjectTopologyFromPayload(wrongPath), /topology is invalid/);

  const duplicate = topology();
  duplicate.tasks[1].native_task_index = 1;
  duplicate.tasks[1].entry_name = "SomeNativeTask-Task1.xml";
  assert.throws(() => customProjectTopologyFromPayload(duplicate), /task topology is invalid/);

  const inventedExecution = topology();
  inventedExecution.execution = { supported: true, reason: "ready" };
  assert.throws(() => customProjectTopologyFromPayload(inventedExecution), /topology is invalid/);
});

test("Custom Project fetch binds one exact project name to the canonical API", async () => {
  let requested = "";
  const result = await fetchCustomProjectTopology("Custom Project A", async (path) => {
    requested = path;
    return { ok: true, status: 200, json: async () => topology() };
  });
  assert.equal(requested, "/api/sqx-project-topology?project=Custom+Project+A");
  assert.equal(result.project, "Custom Project A");
  await assert.rejects(() => fetchCustomProjectTopology("../Other", async () => null), /Exact native project name is required/);
});

test("Custom Project renderer keeps opaque and typed task details truthful", () => {
  const html = renderCustomProjectTopologyResult(topology());
  assert.match(html, /Exact native project snapshot/);
  assert.match(html, /SomeNativeTask/);
  assert.match(html, /Producer semantics preserved opaquely/);
  assert.match(html, /Databanks: Results/);
  assert.match(html, /Target: Build strategies/);
  assert.match(html, /does not execute or reconstruct the native task loop/);
  assert.doesNotMatch(html, /Run task|Start task|Execute/);
});
