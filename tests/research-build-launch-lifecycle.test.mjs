import assert from "node:assert/strict";
import test from "node:test";

test("launch gate settles, rebinds on selection, and rejects stale or failed reads", async () => {
  const original = Object.fromEntries(["document", "location", "fetch", "MutationObserver"].map((key) => [key, globalThis[key]]));
  const callbacks = [];
  let paints = 0;
  let requests = 0;
  let gate;
  let failStatus = false;
  let releaseSlow;
  let releaseLaunch;
  let click;
  let launchModule;
  let validLaunchResponses = 0;
  const tick = () => new Promise((resolve) => setImmediate(resolve));
  const mutate = () => callbacks.forEach((callback) => callback());
  function newGate() {
    let markup = "";
    return {
      isConnected: true, dataset: {},
      get innerHTML() { return markup; },
      set innerHTML(value) {
        markup = value;
        paints += 1;
        // Bound the regression even if an implementation observes its own paint forever.
        if (paints <= 30) queueMicrotask(mutate);
      },
    };
  }
  try {
    gate = newGate();
    globalThis.location = new URL("http://localhost/research?workspace=evolution&configuration=first");
    globalThis.document = { documentElement: {}, querySelector: () => gate, addEventListener(name, callback) { if (name === "click") click = callback; } };
    globalThis.MutationObserver = class {
      constructor(callback) { callbacks.push(callback); }
      observe() {}
    };
    globalThis.fetch = async (path, options = {}) => {
      requests += 1;
      const url = new URL(path, "http://localhost");
      let payload;
      if (options.method === "POST") {
        const input = JSON.parse(options.body);
        await new Promise((resolve) => { releaseLaunch = resolve; });
        payload = {
          schema: "tc.research-native-job.v1", entity_id: "old-job", revision: "old-job-revision",
          configuration_entity_id: input.configuration_entity_id, configuration_revision: input.expected_configuration_revision,
          executable_xml_ref: `tc-evidence:sha256:${"a".repeat(64)}`, executable_xml_sha256: "a".repeat(64),
          sqx_build: "144.2953", operation: "builder_loadconfig_start", staged_config_relative_path: "old-config.xml",
          state: "submitted", receipts: ["loadconfig", "start"].map((action, index) => ({
            sequence: index + 1, action, project: "Builder", state: "completed",
            exit_code: index === 0 ? 0 : null, reason_code: null,
            launcher_sha256: "b".repeat(64), config_sha256: "a".repeat(64), sqx_build: "144.2953",
          })),
          partial_side_effect: false, failure_reason_code: null, launcher_sha256: "b".repeat(64),
        };
        launchModule.nativeJobFromPayload(payload);
        validLaunchResponses += 1;
      } else if (url.pathname.endsWith("configurations")) {
        const entity = url.searchParams.get("entityId");
        if (entity === "slow") await new Promise((resolve) => { releaseSlow = resolve; });
        if (entity === "missing") return { ok: false, status: 404, json: async () => ({ detail: "Configuration not found" }) };
        payload = { schema: "tc.research-configuration.v1", entity_id: entity, revision: `revision-${entity}`, state: "approved", approval: { approved: true }, executable_xml_sha256: "a".repeat(64) };
      } else if (url.pathname.endsWith("native-jobs")) {
        payload = { schema: "tc.research-native-job-catalog.v1", jobs: [] };
      } else {
        if (failStatus) throw new Error("Status unavailable");
        payload = { schema: "tc.runtime-status.v1", research_backend: { execution: { available: true, gateway_available: true, launcher_verified: true, launcher_sha256: "b".repeat(64) } } };
      }
      return { ok: true, json: async () => payload };
    };
    launchModule = await import(`../web/research-build-launch.mjs?lifecycle=${Date.now()}`);
    await tick();
    assert.equal(gate.dataset.buildLaunchGate, "ready");
    assert.equal(requests, 3, "an idle gate must fetch each read model only once");
    mutate();
    await tick();
    assert.equal(requests, 3, "unrelated DOM changes must not refetch");
    const button = { disabled: false, textContent: "Launch Builder" };
    click({ target: { closest: () => button } });
    assert.equal(button.disabled, true);

    globalThis.location.search = "?workspace=evolution&configuration=slow";
    mutate();
    assert.equal(gate.dataset.buildLaunchGate, "loading");
    assert.match(gate.innerHTML, /disabled/);
    globalThis.location.search = "?workspace=evolution&configuration=second";
    failStatus = true;
    mutate();
    await tick();
    assert.equal(gate.dataset.buildLaunchGate, "runtime-unavailable");
    assert.match(gate.innerHTML, /Status unavailable/);
    assert.doesNotMatch(gate.innerHTML, /data-native-builder-launch/);
    releaseSlow();
    await tick();
    assert.equal(gate.dataset.buildLaunchGate, "runtime-unavailable", "late configuration must not restore launch authority");
    releaseLaunch();
    await tick();
    assert.equal(validLaunchResponses, 1, "the stale POST must be a valid success response");
    assert.equal(gate.dataset.buildLaunchGate, "runtime-unavailable", "old launch receipt must not render on a different configuration");
    assert.doesNotMatch(gate.innerHTML, /old-job/);

    failStatus = false;
    gate.isConnected = false;
    gate = newGate();
    mutate();
    await tick();
    assert.equal(gate.dataset.buildLaunchGate, "ready", "replacement host must re-read current state");
    globalThis.location.search = "?workspace=evolution&configuration=missing";
    mutate();
    await tick();
    assert.match(gate.innerHTML, /Configuration not found/);
    assert.doesNotMatch(gate.innerHTML, /data-native-builder-launch/);
    const settledRequests = requests;
    mutate();
    await tick();
    assert.equal(requests, settledRequests, "failed reads must not create a retry storm");
    globalThis.location = new URL("http://localhost/home");
    mutate();
    await tick();
    assert.equal(requests, settledRequests);
  } finally {
    for (const [key, value] of Object.entries(original)) {
      if (value === undefined) delete globalThis[key];
      else globalThis[key] = value;
    }
  }
});
