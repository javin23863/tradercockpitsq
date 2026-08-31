import test from "node:test";
import assert from "node:assert/strict";

import { apolloContext, apolloReply } from "../web/apollo-assistant.mjs";

const strategyRef = `tc:strategy:v1:sha256:${"a".repeat(64)}`;
const runRef = `tc:backtest-run:v1:sha256:${"b".repeat(64)}`;

function location(pathname, params = {}) {
  const search = new URLSearchParams(params).toString();
  return { pathname, search: search ? `?${search}` : "" };
}

test("Apollo reads exact route, strategy, and run context without inventing state", () => {
  const ctx = apolloContext(
    location(`/strategies/${encodeURIComponent(strategyRef)}/overview`, {
      runRef,
      invocationId: "initial-001",
    }),
  );
  assert.equal(ctx.workspaceId, "strategies");
  assert.equal(ctx.stateId, "overview");
  assert.equal(ctx.strategyRef, strategyRef);
  assert.equal(ctx.runRef, runRef);
  assert.equal(ctx.invocationId, "initial-001");
});

test("Apollo prepares exact results navigation when exact run context exists", () => {
  const reply = apolloReply(
    "show results",
    location(`/strategies/${encodeURIComponent(strategyRef)}/overview`, {
      runRef,
      invocationId: "initial-001",
    }),
  );
  assert.equal(reply.boundary, "navigation-only");
  assert.match(reply.text, /exact run\/invocation context/i);
  assert.equal(reply.actions.length, 1);
  assert.match(reply.actions[0].path, /^\/validate\/results\?/);
  const params = new URL(reply.actions[0].path, "http://example.test").searchParams;
  assert.equal(params.get("runRef"), runRef);
  assert.equal(params.get("invocationId"), "initial-001");
  assert.equal(params.get("strategyRef"), null);
});

test("Apollo refuses autonomous mutation requests", () => {
  for (const prompt of [
    "start the run",
    "cancel this run",
    "promote this strategy",
    "delete the evidence",
    "export everything",
  ]) {
    const reply = apolloReply(prompt, location("/validate/run", { strategyRef }));
    assert.equal(reply.boundary, "refused-autonomous-action", prompt);
    assert.match(reply.text, /does not execute or authorize product mutations/i, prompt);
    assert.ok(reply.actions.length > 0, prompt);
  }
});

test("Apollo never infers a latest run when exact identity is absent", () => {
  const reply = apolloReply(
    "show validation results",
    location(`/strategies/${encodeURIComponent(strategyRef)}/overview`),
  );
  assert.equal(reply.boundary, "navigation-only");
  assert.match(reply.text, /will not infer a latest run/i);
  assert.equal(reply.actions[0].path, `/validate/results?strategyRef=${encodeURIComponent(strategyRef)}`);
});

test("Apollo carries exact strategy context to candidates and evidence", () => {
  const candidates = apolloReply(
    "open candidates",
    location(`/strategies/${encodeURIComponent(strategyRef)}/overview`),
  );
  assert.equal(
    candidates.actions[0].path,
    `/strategies/${encodeURIComponent(strategyRef)}/candidates`,
  );

  const evidence = apolloReply(
    "show evidence",
    location(`/strategies/${encodeURIComponent(strategyRef)}/overview`, {
      runRef,
      invocationId: "initial-001",
    }),
  );
  const evidenceUrl = new URL(evidence.actions[0].path, "http://example.test");
  assert.equal(evidenceUrl.pathname, `/strategies/${encodeURIComponent(strategyRef)}/evidence`);
  assert.equal(evidenceUrl.searchParams.get("runRef"), runRef);
  assert.equal(evidenceUrl.searchParams.get("invocationId"), "initial-001");
});

test("Apollo contextual help exposes navigation only", () => {
  const reply = apolloReply("what can you do?", location("/cockpit"));
  assert.equal(reply.boundary, "navigation-only");
  assert.match(reply.text, /navigation/i);
  assert.ok(reply.actions.every((item) => item.path.startsWith("/")));
  assert.ok(!reply.actions.some((item) => /api\//.test(item.path)));
});
