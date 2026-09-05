import test from "node:test";
import assert from "node:assert/strict";
import { bindResultsPluginHost } from "../web/automation-results.mjs";

test("native result plugins inherit the product theme without losing strategy messages", (t) => {
  const properties = {}, messages = [], listeners = {};
  const frame = {
    contentDocument: { readyState: "complete", documentElement: { style: { setProperty: (key, value) => { properties[key] = value; } } }, body: { style: {} } },
    contentWindow: { postMessage: (message) => messages.push(message) },
    addEventListener: (name, handler) => { listeners[name] = handler; },
  };
  const previousStyle = globalThis.getComputedStyle, previousWindow = globalThis.window;
  t.after(() => { globalThis.getComputedStyle = previousStyle; globalThis.window = previousWindow; });
  globalThis.window = new EventTarget();
  globalThis.getComputedStyle = () => ({ fontFamily: "system-ui", getPropertyValue: (key) => ({ "--card": "#0b1423", "--purple": "#7541e8" })[key] || "" });
  const root = { querySelector: () => frame, ownerDocument: { documentElement: {} } };
  bindResultsPluginHost(root, { project: "Builder", databank: "Results", archive: "Example.sqx" });
  assert.equal(properties["--bg-body"], "#0b1423");
  assert.equal(properties["--primary"], "#7541e8");
  assert.equal(frame.contentDocument.body.style.fontFamily, "system-ui");
  assert.deepEqual(messages.map(message => message.type), ["SET_THEME", "STRATEGY_DATA"]);
  assert.equal(messages[1].data.strategyName, "Example");
  listeners.load();
  assert.equal(messages.length, 4);
  bindResultsPluginHost(null, null);
});
