import test from "node:test";
import assert from "node:assert/strict";
import { renderProjectReview, renderReviewSnapshot, requestProjectReview } from "../web/project-review.mjs";

const snapshot = { project: "P", databank: "Results", launch_authorized: false,
  tasks: [{ title: "<Retest>", kind: "Retest", entry: "Retest-Task1.xml", active: true }],
  inputs: [{ archive: "<A>.sqx", binding: "unadmitted", archive_sha256: "a".repeat(64) }], gaps: ["Capture unavailable"] };
const row = { schema: "tc.research-project-review.v1", snapshot, review_sha256: "b".repeat(64) };

test("project review binds requests and refuses stale scope or execution claims", async () => {
  let body;
  const fetcher = async (url, options) => { assert.equal(url, "/api/sqx-project-review"); body = JSON.parse(options.body); return { ok: true, json: async () => row }; };
  assert.equal(await requestProjectReview("retain", "P", "Results", row.review_sha256, fetcher), row);
  assert.deepEqual(body, { action: "retain", project: "P", databank: "Results", expected_review_sha256: row.review_sha256 });
  for (const changed of [{ project: "Other" }, { databank: "Final" }, { launch_authorized: true }]) {
    await assert.rejects(requestProjectReview("preview", "P", "Results", null, async () => ({ ok: true, json: async () => ({ ...row, snapshot: { ...snapshot, ...changed } }) })));
  }
});

test("project review preserves native names and honest scope with accessible controls", () => {
  assert.equal(renderProjectReview("P", ""), "");
  assert.match(renderProjectReview("P", "Results"), /Save exact review/);
  assert.match(renderProjectReview("P", "Results"), /aria-live="polite"/);
  const html = renderReviewSnapshot(row);
  assert.match(html, /&lt;Retest&gt;/);
  assert.match(html, /&lt;A&gt;.sqx/);
  assert.match(html, /Tracked execution unavailable/);
  assert.match(html, /tabindex="0"/);
  assert.match(html, /selected bank \(older review\)/);
});

test("project review displays all banks and native roles without changing selected-bank compatibility", async () => {
  const current = { ...row, snapshot: { ...snapshot,
    tasks: [{ ...snapshot.tasks[0], banks: [{ role: "Source", databank: "Results" }, { role: "Target", databank: "<Final>" }] }],
    banks: [{ name: "Results", inputs: snapshot.inputs, storage: "present" },
      { name: "<Final>", inputs: [{ archive: "survivor.sqx", binding: "exact", archive_sha256: "c".repeat(64) }], storage: "present" },
      { name: "Scratch", inputs: [], storage: "not_created" }] } };
  const html = renderReviewSnapshot(current);
  assert.match(html, /3 project banks · 2 saved archives · 1 exact Candidate bindings/);
  assert.match(html, /Source: Results/);
  assert.match(html, /Target: &lt;Final&gt;/);
  assert.match(html, /survivor.sqx/);
  assert.match(html, /Scratch: 0 saved · storage not created/);
  for (const banks of [null, {}, [{ name: "Results", inputs: null }]]) {
    await assert.rejects(requestProjectReview("preview", "P", "Results", null, async () => ({ ok: true, json: async () => ({ ...current, snapshot: { ...current.snapshot, banks } }) })));
  }
});
