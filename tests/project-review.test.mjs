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
});
