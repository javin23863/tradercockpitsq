import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import {
  producerValidityFromBuilderConfig,
  renderProducerValidity,
} from "../web/research-specification-producer-validity.mjs";

function payload({ state = "pending_native_validation", locked = false } = {}) {
  return {
    schema: "tc.sqx-builder-config.v1",
    specification: {
      schema: "tc.research-specification.v1",
      producer_validity: {
        state,
        method: "authorized_sqx_loadconfig",
        native_execution_check: "loadconfig_before_start",
        local_preflight: state === "pending_native_validation" ? "requirements_complete" : "requirements_incomplete",
      },
      build_gate: {
        locked,
        reason_codes: locked ? ["unresolved:strategy_shape"] : [],
      },
    },
  };
}

test("Specification keeps complete local preflight distinct from pending native producer validation", () => {
  const validity = producerValidityFromBuilderConfig(payload());
  assert.equal(validity.state, "pending_native_validation");
  assert.equal(validity.localPreflight, "requirements_complete");
  assert.equal(validity.localGateLocked, false);

  const html = renderProducerValidity(validity);
  assert.match(html, /Native validation pending/);
  assert.match(html, /authorized_sqx_loadconfig/);
  assert.match(html, /loadconfig_before_start/);
  assert.doesNotMatch(html, /native validated/i);
  assert.doesNotMatch(html, /producer accepted/i);
});

test("Specification reports incomplete local preflight as not ready for native validation", () => {
  const validity = producerValidityFromBuilderConfig(payload({ state: "not_ready_for_native_validation", locked: true }));
  assert.equal(validity.state, "not_ready_for_native_validation");
  assert.equal(validity.localPreflight, "requirements_incomplete");
  assert.match(renderProducerValidity(validity), /Not ready for native validation/);
});

test("producer validity parser fails closed on local/native contradictions", () => {
  assert.throws(
    () => producerValidityFromBuilderConfig(payload({ state: "pending_native_validation", locked: true })),
    /contradicts local Specification readiness/,
  );

  const changedAuthority = payload();
  changedAuthority.specification.producer_validity.method = "browser_assumed_valid";
  assert.throws(
    () => producerValidityFromBuilderConfig(changedAuthority),
    /validation authority changed/,
  );
});

test("canonical desktop loads the producer-validity binder exactly once", async () => {
  const html = await readFile(new URL("../web/index.html", import.meta.url), "utf8");
  assert.equal((html.match(/research-specification-producer-validity\.mjs/g) || []).length, 1);
});
