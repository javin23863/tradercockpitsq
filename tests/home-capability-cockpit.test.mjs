import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

import {
  homeCapabilityModel,
  renderHomeCapabilityCockpit,
} from "../web/home-capability-cockpit.mjs";


test("Home capability cockpit exposes the complete current Research boundary", () => {
  const model = homeCapabilityModel();

  assert.equal(model.schema, "tc.research-capability-coverage.v2");
  assert.deepEqual(model.summary, {
    mapped: 12,
    explicitly_unavailable: 8,
    intentionally_hidden: 0,
  });
  assert.equal(model.mapped.length, 12);
  assert.equal(model.unavailable.length, 8);
  assert.equal(model.hidden.length, 0);
  assert.equal(model.workflow.length, 7);

  const mappedIds = new Set(model.mapped.map((item) => item.id));
  assert.ok(mappedIds.has("builder_native_specification"));
  assert.ok(mappedIds.has("native_builder_execution"));
  assert.ok(mappedIds.has("native_output_candidate_import"));
  assert.ok(mappedIds.has("native_historical_retester"));
  assert.ok(mappedIds.has("native_higher_precision_robustness"));
  assert.ok(mappedIds.has("research_proof"));
});


test("Home capability cockpit renders mapped capabilities and explicit producer boundaries", () => {
  const model = homeCapabilityModel();
  const html = renderHomeCapabilityCockpit();

  assert.match(html, /data-home-capability-cockpit/);
  assert.match(html, /The backend is already a usable research system\. This is the map\./);
  assert.match(html, />12<\/strong><span>mapped<\/span>/);
  assert.match(html, />8<\/strong><span>explicit boundaries<\/span>/);
  assert.match(html, />0<\/strong><span>silently hidden<\/span>/);
  assert.match(html, /TraderCockpit now exposes the producer-backed Research workflow directly/);

  for (const item of model.mapped) {
    assert.match(html, new RegExp(`data-home-capability="${item.id}"`));
    assert.ok(html.includes(`href="${item.route.replaceAll("&", "&amp;")}"`));
  }
  for (const item of model.unavailable) {
    assert.match(html, new RegExp(`data-home-capability-boundary="${item.id}"`));
    assert.ok(html.includes(item.reason_code));
  }
});


test("desktop document loads the capability cockpit before legacy Home binders and retires the legacy hero", () => {
  const html = readFileSync(new URL("../web/index.html", import.meta.url), "utf8");
  const css = readFileSync(new URL("../web/home-capability-cockpit.css", import.meta.url), "utf8");
  const capabilityScript = html.indexOf('/home-capability-cockpit.mjs');
  const marketScript = html.indexOf('/home-market-overview.mjs');

  assert.ok(html.includes('/home-capability-cockpit.css'));
  assert.ok(capabilityScript >= 0);
  assert.ok(marketScript >= 0);
  assert.ok(capabilityScript < marketScript);
  assert.match(css, /\.home-capability-cockpit \+ \.hero-band\s*\{[^}]*display:\s*none;/s);
});
