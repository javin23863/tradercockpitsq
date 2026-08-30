import assert from "node:assert/strict";
import test from "node:test";

import {
  createDataContext,
  loadDataContexts,
  renderDataContextAuthority,
  validateDataContextRecord,
} from "../web/data-context.mjs";

function record() {
  return {
    schema: "tc.data-trading-context.v1",
    context_ref: `tc:data-trading-context:v1:sha256:${"a".repeat(64)}`,
    authority: {
      market_and_dataset_identity: "user-supplied",
      execution_assumptions: "tradercockpit-owned",
      native_sqx_binding: false,
    },
    data: {
      ref: `tc:data:v1:sha256:${"b".repeat(64)}`,
      symbol: "ES",
      timeframe: "1m",
      source: "local-fixture",
      dataset_revision: "rev-1",
      timezone: "America/Chicago",
      session_calendar: "CME",
      start: "2026-01-01T00:00:00.000000Z",
      end: "2026-02-01T00:00:00.000000Z",
      adjustment_policy: "none",
    },
    execution: {
      ref: `tc:execution:v1:sha256:${"c".repeat(64)}`,
      starting_cash: "100000",
      currency: "USD",
      models: [{ kind: "fill", model: "bar-close", parameters: {} }],
    },
  };
}

function response(payload, { ok = true, status = 200 } = {}) {
  return {
    ok,
    status,
    async json() {
      return payload;
    },
  };
}

test("Market Data authority is operational configuration, not fabricated coverage", () => {
  const html = renderDataContextAuthority();
  assert.match(html, /Save research context/);
  assert.match(html, /user-supplied identity/);
  assert.match(html, /not native SQX settings or live-provider facts/);
  assert.doesNotMatch(html, /fake|demo candidate/i);
});

test("record validation requires canonical refs and explicit authority boundary", () => {
  assert.equal(validateDataContextRecord(record()).data.symbol, "ES");
  assert.throws(
    () =>
      validateDataContextRecord({
        ...record(),
        authority: { ...record().authority, native_sqx_binding: true },
      }),
    /authority boundary/,
  );
  assert.throws(
    () => validateDataContextRecord({ ...record(), context_ref: "local-1" }),
    /context identity/,
  );
});

test("list reader preserves backend canonical identities", async () => {
  const expected = record();
  const calls = [];
  const fetchImpl = async (url, options) => {
    calls.push([url, options]);
    return response({
      schema: "tc.data-trading-context-list.v1",
      contexts: [expected],
    });
  };
  const contexts = await loadDataContexts(fetchImpl);
  assert.deepEqual(contexts, [expected]);
  assert.equal(calls[0][0], "/api/data-contexts");
  assert.equal(calls[0][1].headers.accept, "application/json");
});

test("create sends explicit assumptions and trusts only validated backend identity", async () => {
  const expected = record();
  let submitted;
  const fetchImpl = async (url, options) => {
    assert.equal(url, "/api/data-contexts");
    assert.equal(options.method, "POST");
    submitted = JSON.parse(options.body);
    return response(expected, { status: 201 });
  };
  const request = {
    symbol: "ES",
    timeframe: "1m",
    source: "local-fixture",
    datasetRevision: "rev-1",
    timezone: "America/Chicago",
    sessionCalendar: "CME",
    start: "2026-01-01T00:00:00Z",
    end: "2026-02-01T00:00:00Z",
    adjustmentPolicy: "none",
    startingCash: "100000",
    currency: "USD",
    fillModel: "bar-close",
  };
  const created = await createDataContext(request, fetchImpl);
  assert.deepEqual(submitted, request);
  assert.equal(created.context_ref, expected.context_ref);
});

test("backend refusal is surfaced instead of replaced with local state", async () => {
  const fetchImpl = async () =>
    response(
      { error: "invalid_request", detail: "datasetRevision must be supplied" },
      { ok: false, status: 400 },
    );
  await assert.rejects(
    () => createDataContext({}, fetchImpl),
    /datasetRevision must be supplied/,
  );
});
