import assert from "node:assert/strict";
import test from "node:test";

import { renderApp } from "../web/app.mjs";
import { resolveRoute } from "../web/model.mjs";
import {
  IDEA_CATALOG_SCHEMA,
  IDEA_READ_SCHEMA,
  ResearchIdeaApiError,
  fetchIdea,
  fetchIdeaCatalog,
  ingestIdeaSource,
  saveIdeaRevision,
} from "../web/research-ideas.mjs";

const ideaRecord = Object.freeze({
  schema: IDEA_READ_SCHEMA,
  entity_id: "tc-research:idea:v1:12345678-1234-5678-1234-567812345678",
  revision: `tc-research-revision:idea:sha256:${"a".repeat(64)}`,
  parent_revision: null,
  content_ref: `tc-evidence:sha256:${"b".repeat(64)}`,
  text: "Opening range concept",
  source: "Research notebook",
});


test("Idea API client uses only canonical endpoint and exact revision write contract", async () => {
  const calls = [];
  const fetchImpl = async (path, options = {}) => {
    calls.push([path, options]);
    if (options.method === "POST") {
      return { ok: true, status: 201, json: async () => ideaRecord };
    }
    if (String(path).includes("entityId=")) {
      return { ok: true, status: 200, json: async () => ideaRecord };
    }
    return {
      ok: true,
      status: 200,
      json: async () => ({
        schema: IDEA_CATALOG_SCHEMA,
        ideas: [{ entity_id: ideaRecord.entity_id, revision: ideaRecord.revision, summary: "Opening range concept" }],
      }),
    };
  };

  const catalog = await fetchIdeaCatalog(fetchImpl);
  assert.equal(catalog.schema, IDEA_CATALOG_SCHEMA);
  assert.equal(calls[0][0], "/api/research/ideas");

  const loaded = await fetchIdea(ideaRecord.entity_id, fetchImpl);
  assert.equal(loaded, ideaRecord);
  assert.match(calls[1][0], /^\/api\/research\/ideas\?entityId=/);
  assert.doesNotMatch(calls[1][0], /path=|dataRoot=/i);

  await saveIdeaRevision({ text: "New Idea", source: "notes" }, fetchImpl);
  assert.deepEqual(JSON.parse(calls[2][1].body), { text: "New Idea", source: "notes" });

  await saveIdeaRevision({
    entityId: ideaRecord.entity_id,
    expectedRevision: ideaRecord.revision,
    text: "Updated",
    source: "notes",
  }, fetchImpl);
  assert.deepEqual(JSON.parse(calls[3][1].body), {
    entity_id: ideaRecord.entity_id,
    expected_revision: ideaRecord.revision,
    text: "Updated",
    source: "notes",
  });
  assert.equal(calls[3][1].method, "POST");
  assert.equal(calls[3][1].headers["content-type"], "application/json");
});


test("Idea API client preserves backend conflict status and refuses schema drift", async () => {
  await assert.rejects(
    () => saveIdeaRevision(
      {
        entityId: ideaRecord.entity_id,
        expectedRevision: ideaRecord.revision,
        text: "stale",
      },
      async () => ({
        ok: false,
        status: 409,
        json: async () => ({ reason_code: "current_conflict", detail: "Idea changed" }),
      }),
    ),
    (error) => {
      assert.ok(error instanceof ResearchIdeaApiError);
      assert.equal(error.status, 409);
      assert.equal(error.payload.reason_code, "current_conflict");
      return true;
    },
  );

  await assert.rejects(
    () => fetchIdeaCatalog(async () => ({ ok: true, status: 200, json: async () => ({ schema: "wrong.v1" }) })),
    /schema mismatch/i,
  );

  const ingested = await ingestIdeaSource({ filename: "note.txt", text: "A strategy buys when RSI is below 30 and sells when RSI is above 70." }, async (path, options) => {
    assert.equal(path, "/api/research/ideas/ingest");
    assert.equal(options.method, "POST");
    const body = JSON.parse(options.body);
    assert.equal(body.filename, "note.txt");
    return { ok: true, status: 201, json: async () => ideaRecord };
  });
  assert.equal(ingested, ideaRecord);
});


test("Research Idea render exposes exact selected custody and no native-compute action", () => {
  const route = resolveRoute("/research", "?stage=construct&tab=idea");
  const html = renderApp(
    route,
    { phase: "loading", payload: null, detail: "" },
    {
      phase: "loaded",
      catalog: [{ entity_id: ideaRecord.entity_id, revision: ideaRecord.revision, summary: "Opening range concept" }],
      selected: ideaRecord,
      detail: "Saved exact Idea revision.",
    },
  );

  assert.match(html, /data-research-idea-workspace/);
  assert.match(html, /Opening range concept/);
  assert.match(html, /Research notebook/);
  assert.match(html, new RegExp(ideaRecord.entity_id));
  assert.match(html, /Current revision/);
  assert.match(html, /Save new revision/);
  assert.match(html, /Saving does not create a candidate, run native compute, or infer trading semantics/);
  assert.doesNotMatch(html, /Launch Builder|Run Backtest|Start native/i);
  assert.match(html, /data-idea-action="ingest-url"/);
  assert.match(html, /data-idea-action="ingest-document"/);
});

test("ingested Idea render shows hashed quoted spans and refuses invented draft copy", () => {
  const ingested = {
    ...ideaRecord,
    ingest: {
      schema: "tc.research-source-ingest.v1",
      kind: "document",
      filename: "note.txt",
      content_sha256: "c".repeat(64),
      quoted_spans: [{ id: "span-0001", start: 0, end: 40, sha256: "d".repeat(64), text: "A strategy buys when RSI is below 30." }],
    },
    draft: {
      schema: "tc.research-source-draft.v1",
      status: "bound",
      object_kind: "strategy",
      clauses: [{ span_id: "span-0001", text: "buys when RSI is below 30", sha256: "e".repeat(64) }],
      reason_code: null,
      detail: "Typed draft bound to hashed quoted spans.",
    },
  };
  const html = renderApp(
    resolveRoute("/research", "?workspace=signals&tab=overview"),
    { phase: "loading", payload: null, detail: "" },
    { phase: "loaded", catalog: [], selected: ingested, detail: "" },
  );
  assert.match(html, /data-idea-ingest-spans/);
  assert.match(html, /data-span-id="span-0001"/);
  assert.match(html, /data-idea-object-kind="strategy"/);
  assert.match(html, /buys when RSI is below 30/);
  assert.doesNotMatch(html, /this will work live/i);
});
