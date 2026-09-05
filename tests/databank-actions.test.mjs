import test from "node:test";
import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { databankAction, databankBatchAction, candidatePurge, importDiscard, renderCandidatePurge, retainDatabankOperation, retainedDatabankOperations, customProjectResultsFromPayload } from "../web/custom-project-results.mjs";

const hash = bytes => createHash("sha256").update(bytes).digest("hex");
const target = { project: "Builder", databank: "Results", archives: [{ archive: "A.sqx", archive_sha256: "a".repeat(64) }, { archive: "B.sqx", archive_sha256: "b".repeat(64) }] };
const globalTarget = target;
const response = value => ({ ok: true, json: async () => value });
const candidate = { candidate_entity_id: "tc-research:candidate:v1:11111111-1111-4111-8111-111111111111",
  candidate_revision: `tc-research-revision:candidate:sha256:${"c".repeat(64)}`,
  membership_revision: `tc-research-revision:candidate-membership:sha256:${"d".repeat(64)}` };

test("load binds original file hash and retained operation across file-free resume", async () => {
  const file = new File(["native archive fixture"], "A.sqx");
  const exact = { project: "Builder", databank: "Results", archive: file.name, source_sha256: hash("native archive fixture"), operation_id: "1".repeat(32) };
  const receipt = { schema: "tc.sqx-databank-action.v1", action: "load", ...exact, ...candidate, archive_sha256: "a".repeat(64), producer: "sqx_local_web", persisted: true };
  await databankAction("load", exact, file, async (path, options) => {
    assert.equal(path, "/api/sqx-databank/load"); assert.equal(options.body, file);
    assert.deepEqual(JSON.parse(decodeURIComponent(options.headers["X-TraderCockpit-Target"])), exact);
    return response(receipt);
  });
  await databankAction("load", exact, null, async (path, options) => {
    assert.equal(path, "/api/sqx-databank/load-resume"); assert.deepEqual(JSON.parse(options.body), exact);
    assert.equal(options.headers["content-type"], "application/json"); return response(receipt);
  });
  await assert.rejects(databankAction("load", exact, new File(["changed"], "A.sqx"), () => assert.fail("No mismatched upload")), /retained import/);
  for (const change of [{ source_sha256: "f".repeat(64) }, { operation_id: "2".repeat(32) }, { candidate_entity_id: null }]) {
    await assert.rejects(databankAction("load", exact, null, async () => response({ ...receipt, ...change })), /does not match/);
  }
});

test("reconnect requires exact old and current custody with an explicit operation", async () => {
  const exact = { project: "Builder", databank: "Results", archive: "A.sqx", archive_sha256: "a".repeat(64),
    previous_archive_sha256: "b".repeat(64), ...candidate, operation_id: "3".repeat(32) };
  const receipt = { schema: "tc.sqx-databank-action.v1", action: "reconcile", ...exact,
    membership_revision: `tc-research-revision:candidate-membership:sha256:${"e".repeat(64)}`, source_sha256: null, producer: "sqx_local_web", persisted: true };
  await databankAction("reconcile", exact, null, async (path, options) => {
    assert.equal(path, "/api/sqx-databank/reconcile"); assert.deepEqual(JSON.parse(options.body), exact); return response(receipt);
  });
  for (const change of [{ archive_sha256: "f".repeat(64) }, { operation_id: "4".repeat(32) }, { candidate_revision: `tc-research-revision:candidate:sha256:${"f".repeat(64)}` }, { membership_revision: candidate.membership_revision }]) {
    await assert.rejects(databankAction("reconcile", exact, null, async () => response({ ...receipt, ...change })), /does not match/);
  }
  await assert.rejects(databankAction("reconcile", { ...exact, previous_archive_sha256: exact.archive_sha256 }, null, () => assert.fail("No unbound reconnect")));
});

test("reconciliation hints cannot imply an exact association or another archive", () => {
  const hint = { schema: "tc.research-native-candidate-reconciliation.v1", ...candidate,
    previous_archive_sha256: "b".repeat(64), archive_sha256: "a".repeat(64) };
  const row = { archive: "A.sqx", relative_path: "user/projects/Builder/databanks/Results/A.sqx", native_version: "1",
    inspectable: true, archive_sha256: "a".repeat(64), candidate_association: null, candidate_reconciliation: hint };
  const payload = { schema: "tc.sqx-custom-project-results.v1", source_build: "144.2953", status: "ready", project: "Builder",
    projects: [{ name: "Builder", source_relative_path: "user/projects/Builder/project.cfx", databank_count: 1, strategy_count: 1,
      databanks: [{ name: "Results", strategy_count: 1, strategies: [row] }] }] };
  assert.equal(customProjectResultsFromPayload(payload).projects[0].databanks[0].strategies[0].candidate_association, null);
  for (const change of [{ archive_sha256: "f".repeat(64) }, { previous_archive_sha256: row.archive_sha256 }, { membership_revision: "unknown" }, { unavailable_reason: "ready" }, { unavailable_reason: true }]) {
    const bad = structuredClone(payload); bad.projects[0].databanks[0].strategies[0].candidate_reconciliation = { ...hint, ...change };
    assert.throws(() => customProjectResultsFromPayload(bad), /reconciliation/);
  }
  for (const unavailable_reason of ["candidate_legacy_reimport_required", "candidate_token_invalid", "candidate_archive_invalid"]) {
    const legacy = structuredClone(payload);
    legacy.projects[0].databanks[0].strategies[0].candidate_reconciliation.unavailable_reason = unavailable_reason;
    assert.equal(customProjectResultsFromPayload(legacy).projects[0].databanks[0].strategies[0].candidate_reconciliation.unavailable_reason, unavailable_reason);
  }
  row.candidate_association = { ...hint, schema: "tc.research-native-candidate-association.v1" };
  assert.throws(() => customProjectResultsFromPayload(payload), /reconciliation/);
});

test("selected archive operations reject broadened targets and mismatched native receipts", async () => {
  const target = { ...globalTarget, operation_id: "1".repeat(32) };
  const receipt = { schema: "tc.sqx-databank-action.v1", action: "remove", ...target, producer: "sqx_local_web", persisted: true, results: [] };
  let request;
  await databankBatchAction("remove", target, async (path, options) => { request = { path, body: JSON.parse(options.body) }; return response(receipt); });
  assert.deepEqual(request, { path: "/api/sqx-databank/remove", body: target });
  for (const invalid of [{ ...target, all: true }, { ...target, archives: [] }, { ...target, archives: [target.archives[0], target.archives[0]] }, { ...target, archives: [{ archive: "../A.sqx", archive_sha256: "a".repeat(64) }] }]) {
    await assert.rejects(databankBatchAction("remove", invalid, () => assert.fail("must refuse before HTTP")));
  }
  await assert.rejects(databankBatchAction("remove", target, async () => response({ ...receipt, archives: [target.archives[0]] })), /does not match/);
  const transfer = { ...target, target_project: "Builder", target_databank: "Review" };
  await assert.rejects(databankBatchAction("move", transfer, async () => response({ ...receipt, ...transfer, action: "move", target_databank: "Elsewhere" })), /does not match/);
});

test("clear uses a frozen server snapshot, including banks larger than a selected batch", async () => {
  const selection = { project: target.project, databank: target.databank };
  const frozen = { schema: "tc.sqx-databank-snapshot.v1", ...selection, snapshot_ref: `tc-evidence:sha256:${"c".repeat(64)}`, archive_count: 1500 };
  assert.equal((await databankBatchAction("snapshot", selection, async () => response(frozen))).archive_count, 1500);
  const exact = { ...selection, snapshot_ref: frozen.snapshot_ref, operation_id: "2".repeat(32) };
  await databankBatchAction("clear", exact, async (path, options) => {
    assert.equal(path, "/api/sqx-databank/clear"); assert.deepEqual(JSON.parse(options.body), exact);
    return response({ schema: "tc.sqx-databank-action.v1", ...exact, action: "clear", producer: "sqx_local_web", persisted: true, results: [] });
  });
  await assert.rejects(databankBatchAction("clear", { ...exact, snapshot_ref: "all" }, () => assert.fail("no unbound clear")));
  await assert.rejects(databankBatchAction("snapshot", selection, async () => response({ ...frozen, project: "Other" })), /does not match/);
});

test("bulk download checks both bundle bytes and the selected native archive manifest", async () => {
  const bytes = Buffer.from("bundle bytes");
  const manifest = JSON.stringify({ archives: target.archives, databank: target.databank, project: target.project });
  const headers = { "X-Archive-Sha256": hash(bytes), "X-Selection-Sha256": hash(manifest) };
  const downloaded = await databankBatchAction("export", target, async () => new Response(bytes, { headers }));
  assert.equal(downloaded.size, bytes.length);
  await assert.rejects(databankBatchAction("export", target, async () => new Response(bytes, { headers: { ...headers, "X-Selection-Sha256": "f".repeat(64) } })), /selected strategies/);
  await assert.rejects(databankBatchAction("export", target, async () => new Response("changed bytes", { headers })), /integrity check/);
});

test("candidate deletion binds the reviewed preview and explains retained shared files", async () => {
  const id = "tc-research:candidate:v1:11111111-1111-4111-8111-111111111111";
  const preview = { schema: "tc.research-candidate-purge.v1", state: "preview", intent_id: "c".repeat(64),
    preview: { candidate_entity_id: id, entities: [id], revisions: ["revision"], artifacts: [{ bytes: 2048 }],
      shared_artifacts: [{ bytes: 1024 }], staging: [], mutation_journals: [], memberships: [{ project: "Builder", databank: "Results", archive: "<A>.sqx" }] } };
  const fetched = await candidatePurge("preview", { candidate_entity_id: id }, async () => response(preview));
  const html = renderCandidatePurge(fetched);
  assert.match(html, /1 shared artifacts will remain/); assert.match(html, /&lt;A&gt;/);
  assert.match(html, /Original desktop imports/);
  const exact = { candidate_entity_id: id, expected_preview_sha256: preview.intent_id };
  await candidatePurge("confirm", exact, async (path, request) => {
    assert.equal(path, "/api/sqx-databank/purge-confirm"); assert.deepEqual(JSON.parse(request.body), exact);
    return response({ ...preview, state: "completed", reclaimed_bytes: 2048, reclaimed_byte_measure: "file_content_bytes", reclamation_uncertain_paths: [] });
  });
  await assert.rejects(candidatePurge("confirm", exact, async () => response({ ...preview, state: "prepared" })), /does not match/);
  await assert.rejects(candidatePurge("confirm", { ...exact, all: true }, () => assert.fail("No broadened delete")));
  await assert.rejects(candidatePurge("preview", { candidate_entity_id: id }, async () => response({ ...preview,
    preview: { ...preview.preview, mutation_journals: [{ bytes: 1, candidate_entity_id: "another-candidate" }] } })), /does not match/);
});

test("uncertain mutation survives rebind; completed action gets a fresh identity", async () => {
  const entries = new Map();
  const storage = { get length() { return entries.size; }, key: index => [...entries.keys()][index], getItem: key => entries.get(key) ?? null, setItem: (key, value) => entries.set(key, value), removeItem: key => entries.delete(key) };
  const first = await retainDatabankOperation("remove", target, storage);
  const retry = await retainDatabankOperation("remove", structuredClone(target), storage);
  assert.match(first.target.operation_id, /^[a-f0-9]{32}$/);
  assert.deepEqual(retry.target, first.target);
  assert.deepEqual(retainedDatabankOperations("Builder", storage), [{ action: "remove", target: first.target }]);
  assert.deepEqual(retainedDatabankOperations("Other", storage), []);
  const changed = await retainDatabankOperation("remove", { ...target, databank: "Other" }, storage);
  assert.notEqual(changed.target.operation_id, first.target.operation_id);
  retry.completed();
  const next = await retainDatabankOperation("remove", target, storage);
  assert.notEqual(next.target.operation_id, first.target.operation_id);
  first.completed();
  assert.equal((await retainDatabankOperation("remove", target, storage)).target.operation_id, next.target.operation_id, "A late old receipt cannot discard a newer pending operation");
  await assert.rejects(retainDatabankOperation("remove", target, { ...storage, setItem() {}, getItem: () => null }), /Cannot retain/);
  for (const action of ["load", "reconcile"]) {
    const exact = action === "load" ? { project: "Builder", databank: "Results", archive: "A.sqx", source_sha256: "a".repeat(64) }
      : { project: "Builder", databank: "Results", archive: "A.sqx", archive_sha256: "a".repeat(64), previous_archive_sha256: "b".repeat(64), ...candidate };
    const pending = await retainDatabankOperation(action, exact, storage);
    assert.deepEqual((await retainDatabankOperation(action, structuredClone(exact), storage)).target, pending.target);
    assert.ok(retainedDatabankOperations("Builder", storage).some(row => row.action === action && row.target.operation_id === pending.target.operation_id));
    assert.notEqual((await retainDatabankOperation(action, { ...exact, archive: "Other.sqx" }, storage)).target.operation_id, pending.target.operation_id);
  }
});

test("a fresh browser recovers exact import and confirmed deletion without minting an intent", async () => {
  const entries = new Map();
  const storage = { get length() { return entries.size; }, key: index => [...entries.keys()][index], getItem: key => entries.get(key) ?? null, setItem: (key, value) => entries.set(key, value), removeItem: key => entries.delete(key) };
  const exact = { project: "Builder", databank: "Results", archive: "A.sqx", source_sha256: "a".repeat(64) };
  const recovered = { action: "load", target: { ...exact, operation_id: "1".repeat(32) } };
  const payload = { schema: "tc.sqx-custom-project-results.v1", source_build: "144.2953", status: "ready", project: "Builder", projects: [], import_recovery: { status: "ready", operations: [recovered] } };
  assert.equal(customProjectResultsFromPayload(payload), payload);
  for (const invalid of [{ ...recovered, candidate_token: "private" }, { ...recovered, target: { ...recovered.target, project: "Other" } }, { ...recovered, discard_preview_sha256: "invalid" }]) {
    assert.throws(() => customProjectResultsFromPayload({ ...payload, import_recovery: { status: "ready", operations: [invalid] } }), /identity/);
  }
  assert.throws(() => customProjectResultsFromPayload({ ...payload, import_recovery: { status: "ready", operations: [recovered, recovered] } }), /identity/);
  assert.throws(() => customProjectResultsFromPayload({ ...payload, import_recovery: { status: "unavailable", detail: "Unavailable", operations: [recovered] } }), /response/);
  assert.deepEqual(retainedDatabankOperations("Builder", storage, [recovered]), [recovered]);
  assert.equal(entries.size, 0, "Reading recovery does not write or submit an operation");
  const first = await retainDatabankOperation("load", exact, storage, recovered);
  assert.equal(first.target.operation_id, recovered.target.operation_id);
  const deleting = { ...recovered, discard_preview_sha256: "d".repeat(64) };
  const merged = retainedDatabankOperations("Builder", storage, [deleting]);
  assert.equal(merged.length, 1);
  const retry = await retainDatabankOperation("load", exact, storage, merged[0]);
  assert.equal(retry.discard_preview_sha256, deleting.discard_preview_sha256);
  assert.throws(() => retainedDatabankOperations("Builder", storage, [{ ...deleting, target: { ...deleting.target, archive: "Other.sqx" } }]), /conflicts/);
  await assert.rejects(retainDatabankOperation("load", exact, storage, { ...deleting, discard_preview_sha256: "e".repeat(64) }), /conflicts/);
  await assert.rejects(retainDatabankOperation("load", exact, storage, { ...deleting, target: { ...deleting.target, operation_id: "2".repeat(32) } }), /conflicts/);
  assert.deepEqual(retainedDatabankOperations("Other", storage, [deleting]), []);
  retry.completed();
  assert.equal(entries.size, 0);
});

test("unfinished import deletion binds the original request and allows only its unpublished journal", async () => {
  const exact = { project: "Builder", databank: "Results", archive: "A.sqx", source_sha256: "a".repeat(64), operation_id: "1".repeat(32) };
  const binding = { request: exact, operation_id: exact.operation_id, mutation_id: "b".repeat(64), journal_sha256: "c".repeat(64), phase: "prepared", native_disposition: "not_submitted" };
  const preview = { schema: "tc.research-candidate-purge.v1", state: "preview", intent_id: "d".repeat(64), preview: {
    candidate_entity_id: candidate.candidate_entity_id, entities: [candidate.candidate_entity_id], revisions: [], artifacts: [{ bytes: 123 }], shared_artifacts: [], staging: [], memberships: [], cancel_import: binding,
    mutation_journals: [{ path: `databank-actions/${binding.mutation_id}.json`, sha256: binding.journal_sha256, mutation_id: binding.mutation_id,
      candidate_entity_id: candidate.candidate_entity_id, candidate_revision: null, action: "load", bytes: 45,
      source: { project: exact.project, databank: exact.databank, archive: exact.archive, archive_sha256: exact.source_sha256 } }],
  } };
  assert.match(renderCandidatePurge(await importDiscard("preview", exact, async (path, options) => {
    assert.equal(path, "/api/sqx-databank/import-discard-preview"); assert.deepEqual(JSON.parse(options.body), exact); return response(preview);
  })), /Discard this unfinished import/);
  await assert.rejects(candidatePurge("preview", { candidate_entity_id: candidate.candidate_entity_id }, async () => response(preview)), /does not match/, "Ordinary candidate deletion still refuses unpublished journal revisions");
  for (const change of [{ native_disposition: "confirmed_absent" }, { phase: "load_submitted" }, { operation_id: "2".repeat(32) }, { request: { ...exact, source_sha256: "f".repeat(64) } }]) {
    await assert.rejects(importDiscard("preview", exact, async () => response({ ...preview, preview: { ...preview.preview, cancel_import: { ...binding, ...change } } })), /does not match/);
  }
  const confirm = { ...exact, expected_preview_sha256: preview.intent_id };
  for (const code of ["candidate_purge_preview_changed", "databank_import_discard_preview_changed", "databank_import_submitted", "databank_io_unavailable"]) {
    await assert.rejects(importDiscard("confirm", confirm, async () => Response.json({ error: "invalid_state", reason_code: code }, { status: 409 })),
      error => error.discardNotStarted === ["databank_import_discard_preview_changed", "databank_import_submitted"].includes(code));
  }
  const completed = { ...preview, state: "completed", reclaimed_bytes: 168, reclaimed_byte_measure: "file_content_bytes", reclamation_uncertain_paths: [] };
  await importDiscard("confirm", confirm, async (path, options) => {
    assert.equal(path, "/api/sqx-databank/import-discard-confirm"); assert.deepEqual(JSON.parse(options.body), confirm); return response(completed);
  });
  await assert.rejects(importDiscard("confirm", confirm, async () => response({ ...completed, intent_id: "f".repeat(64) })), /does not match/);
  const entries = new Map();
  const storage = { get length() { return entries.size; }, key: index => [...entries.keys()][index], getItem: key => entries.get(key) ?? null, setItem: (key, value) => entries.set(key, value), removeItem: key => entries.delete(key) };
  const { operation_id, ...target } = exact;
  const retained = await retainDatabankOperation("load", target, storage);
  retained.confirmDiscard(preview.intent_id);
  const reopened = await retainDatabankOperation("load", target, storage);
  assert.equal(reopened.discard_preview_sha256, preview.intent_id);
  assert.equal(reopened.target.operation_id, retained.target.operation_id);
  assert.throws(() => reopened.confirmDiscard("f".repeat(64)), /changed/);
  assert.equal(retainedDatabankOperations("Builder", storage)[0].discard_preview_sha256, preview.intent_id);
  reopened.completed(); assert.equal(entries.size, 0);
});
