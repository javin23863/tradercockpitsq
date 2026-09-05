// Run against an already-started local static/product server:
// TRADERCOCKPIT_DATABANK_BROWSER_BASE=http://127.0.0.1:4383 node tests/databank-controls-browser.mjs
// Uses the real dock module/DOM. All action requests are injected fixtures;
// an independent network guard rejects any request to a real API.
import assert from "node:assert/strict";
import { chromium } from "playwright";

const base = new URL(process.env.TRADERCOCKPIT_DATABANK_BROWSER_BASE || "http://127.0.0.1:4383");
assert.ok(["127.0.0.1", "localhost", "[::1]"].includes(base.hostname), "Use a local fixture server");
const project = "Example Workflow";
const candidateId = "tc-research:candidate:v1:11111111-1111-4111-8111-111111111111";
const previewHash = "e".repeat(64);
const association = {
  schema: "tc.research-native-candidate-association.v1", candidate_entity_id: candidateId,
  candidate_revision: `tc-research-revision:candidate:sha256:${"c".repeat(64)}`,
  membership_revision: `tc-research-revision:candidate-membership:sha256:${"d".repeat(64)}`,
  archive_sha256: "a".repeat(64),
};
const view = { name: "Default - Main data", columns: [{ class: "ResultsName", name: "Strategy name", format: "text", header: "Strategy name" }] };
function results(names = ["A.sqx", "B.sqx"]) {
  return {
    schema: "tc.sqx-custom-project-results.v1", source_build: "144.2953", status: "ready",
    reason_code: null, detail: "Browser fixture native archive identities", project,
    databank_count: 2, strategy_count: names.length,
    projects: [{ name: project, source_relative_path: `user/projects/${project}/project.cfx`, databank_count: 2,
      strategy_count: names.length, databanks: [
        { name: "Results", strategy_count: names.length, view, strategies: names.map(archive => ({
          archive, relative_path: `user/projects/${project}/databanks/Results/${archive}`, inspectable: true,
          native_version: "1", archive_sha256: (archive === "A.sqx" ? "a" : "b").repeat(64),
          candidate_association: archive === "A.sqx" ? association : null,
        })) },
        { name: "Review", strategy_count: 0, strategies: [], view },
      ] }],
  };
}
const preview = {
  schema: "tc.research-candidate-purge.v1", intent_id: previewHash, state: "preview",
  preview: { candidate_entity_id: candidateId, entities: [candidateId], revisions: [association.candidate_revision],
    artifacts: [{ ref: `tc-evidence:sha256:${"a".repeat(64)}`, bytes: 120 }],
    shared_artifacts: [{ ref: `tc-evidence:sha256:${"f".repeat(64)}`, bytes: 80 }], staging: [], mutation_journals: [],
    memberships: [{ project, databank: "Results", archive: "A.sqx", archive_sha256: association.archive_sha256 }],
  },
};

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
page.setDefaultTimeout(7000);
const pageErrors = [], unexpectedNetwork = [];
page.on("pageerror", error => pageErrors.push(error.message));
await page.route("**/*", async route => {
  const request = route.request(), url = new URL(request.url());
  if (url.origin !== base.origin || url.pathname.startsWith("/api/") || request.method() !== "GET") {
    unexpectedNetwork.push(`${request.method()} ${url.origin}${url.pathname}`);
    return route.abort("blockedbyclient");
  }
  if (url.pathname === "/__databank_controls__") return route.fulfill({ contentType: "text/html", body:
    '<!doctype html><html><head><meta charset="utf-8"><title>Databank controls regression</title><link rel="stylesheet" href="/styles.css"></head><body><main id="fixture" style="padding:20px"></main></body></html>' });
  return route.continue();
});

try {
  await page.goto(new URL("/__databank_controls__", base).href);
  const mountDock = async ({ fixture, afterPurge, preview, project, previousCalls = [], selectedArchive = "A.sqx", partialClear = false, importRetry = false, discardImport = false, discardRefusal = null, reconnect = false, association }) => {
    const { customProjectResultsFromPayload, renderDatabankDock, bindDatabankDock } = await import("/custom-project-results.mjs");
    const root = document.querySelector("#fixture");
    window.dockCalls = previousCalls;
    window.dockSelections = [];
    window.dockChanged = [];
    let confirmations = 0;
    const mockFetch = async (path, options = {}) => {
      const body = options.body instanceof Blob ? JSON.parse(decodeURIComponent(options.headers["X-TraderCockpit-Target"])) : options.body ? JSON.parse(options.body) : null;
      window.dockCalls.push({ path, method: options.method || "GET", body });
      if (discardImport && path.startsWith("/api/sqx-databank/import-discard-")) {
        const { expected_preview_sha256, ...request } = body;
        const binding = { request, operation_id: request.operation_id, mutation_id: "7".repeat(64), journal_sha256: "8".repeat(64), phase: "prepared", native_disposition: "not_submitted" };
        const result = { ...preview, ...(discardRefusal && window.dockCalls.filter(call => call.path.endsWith("import-discard-preview")).length > 1 ? { intent_id: "6".repeat(64) } : {}), preview: { ...preview.preview, memberships: [], revisions: [], cancel_import: binding,
          mutation_journals: [{ path: `databank-actions/${binding.mutation_id}.json`, mutation_id: binding.mutation_id, sha256: binding.journal_sha256,
            candidate_entity_id: preview.preview.candidate_entity_id, candidate_revision: null, action: "load", bytes: 99,
            source: { project: request.project, databank: request.databank, archive: request.archive, archive_sha256: request.source_sha256 } }],
        } };
        if (path.endsWith("-confirm")) {
          if (window.dockCalls.filter(call => call.path === path).length === 1) return Response.json(discardRefusal
            ? { error: "invalid_state", reason_code: discardRefusal, detail: "Fixture deletion did not start." }
            : { detail: "Fixture deletion response interrupted." }, { status: 409 });
          return Response.json({ ...result, state: "completed", reclaimed_bytes: 219, reclaimed_byte_measure: "file_content_bytes", reclamation_uncertain_paths: [] });
        }
        return Response.json(result);
      }
      if (importRetry && path === "/api/sqx-databank/load") return Response.json({ detail: "Fixture import retained; response interrupted." }, { status: 409 });
      if (importRetry && path === "/api/sqx-databank/load-resume") return Response.json({
        ...association, schema: "tc.sqx-databank-action.v1", action: "load", ...body, archive_sha256: "b".repeat(64), producer: "sqx_local_web", persisted: true,
      });
      if (reconnect && path === "/api/sqx-databank/reconcile") return Response.json({
        schema: "tc.sqx-databank-action.v1", action: "reconcile", ...body,
        membership_revision: `tc-research-revision:candidate-membership:sha256:${"f".repeat(64)}`,
        producer: "sqx_local_web", persisted: true, source_sha256: null,
      });
      if (path === "/api/sqx-databank/purge-preview") return Response.json(preview);
      if (path === "/api/sqx-databank/purge-confirm") {
        confirmations++;
        if (confirmations === 1) return Response.json({ reason_code: "databank_native_refused", detail: "Fixture native removal refused; retry the same confirmed preview." }, { status: 409 });
        return Response.json({ ...preview, state: "completed", reclaimed_bytes: 120, reclaimed_byte_measure: "file_content_bytes", reclamation_uncertain_paths: [] });
      }
      if (path === "/api/sqx-databank/rename") return Response.json({ detail: "Fixture rename refused; selection remains available." }, { status: 409 });
      if (partialClear && path === "/api/sqx-databank/snapshot") return Response.json({
        schema: "tc.sqx-databank-snapshot.v1", project, databank: "Results", snapshot_ref: `tc-evidence:sha256:${"9".repeat(64)}`, archive_count: 2,
      });
      if (partialClear && path === "/api/sqx-databank/clear") {
        if (window.dockCalls.filter(call => call.path === path).length === 1) return Response.json({
          reason_code: "databank_mutation_interrupted", detail: "Fixture clear removed A before interruption; retry the original two-strategy snapshot.",
        }, { status: 409 });
        return Response.json({ schema: "tc.sqx-databank-action.v1", action: "clear", ...body,
          producer: "sqx_local_web", persisted: true, results: [], removed_count: 2 });
      }
      if (path === `/api/sqx-project-results?${new URLSearchParams({ project })}`) return Response.json(afterPurge);
      throw new Error(`Unexpected injected operation: ${path}`);
    };
    // If a production caller accidentally bypasses the injected fetch, fail the
    // test without allowing that request to reach the local/native server.
    window.fetch = async () => { throw new Error("Unexpected global fetch in dock regression"); };
    const parsed = customProjectResultsFromPayload(fixture);
    const state = { results: parsed, project, databank: "Results", archive: selectedArchive };
    root.innerHTML = renderDatabankDock(parsed, project, state);
    bindDatabankDock(root, state, { fetchImpl: mockFetch,
      onSelect: (...selection) => window.dockSelections.push(selection), onChanged: action => window.dockChanged.push(action) });
  };
  await page.evaluate(mountDock, { fixture: results(), afterPurge: results(["B.sqx"]), preview, project });

  const row = name => page.locator(`tr[data-automation-archive="${name}"]`);
  const calls = () => page.evaluate(() => window.dockCalls);
  const nameForm = page.locator("[data-dock-name-form]");
  const batchForm = page.locator("[data-dock-batch-form]");
  const purgeForm = page.locator("[data-dock-purge-form]");

  // The former bug submitted A's edited name against B after a row change.
  await page.locator("[data-dock-rename]").click();
  assert.equal(await nameForm.locator("input").inputValue(), "A");
  await nameForm.locator("input").fill("Edited A");
  await row("B.sqx").locator("a").click();
  await nameForm.waitFor({ state: "hidden" });
  assert.equal((await calls()).length, 0);
  await page.locator("[data-dock-rename]").click();
  assert.equal(await nameForm.locator("input").inputValue(), "B");
  await nameForm.locator("input").fill("Edited B");
  await nameForm.locator('button[type="submit"]').click();
  await page.locator("[data-dock-status]").filter({ hasText: "Fixture rename refused" }).waitFor();
  const firstRename = (await calls())[0].body;
  assert.match(firstRename.operation_id, /^[0-9a-f]{32}$/);
  assert.deepEqual(firstRename, { project, databank: "Results", archive: "B.sqx", archive_sha256: "b".repeat(64), new_name: "Edited B", operation_id: firstRename.operation_id });
  await page.locator("[data-dock-rename]").click();
  await nameForm.locator("input").fill("Edited B");
  await nameForm.locator('button[type="submit"]').click();
  await page.locator("[data-dock-status]").filter({ hasText: "Fixture rename refused" }).waitFor();
  assert.deepEqual((await calls())[1].body, firstRename, "An exact refused rename retains its operation ID on retry");
  const beforeReload = await calls();
  await page.reload();
  await page.evaluate(mountDock, { fixture: results(), afterPurge: results(["B.sqx"]), preview, project, previousCalls: beforeReload, selectedArchive: "B.sqx" });
  await page.locator("[data-dock-rename]").click();
  await nameForm.locator("input").fill("Edited B");
  await nameForm.locator('button[type="submit"]').click();
  await page.locator("[data-dock-status]").filter({ hasText: "Fixture rename refused" }).waitFor();
  assert.deepEqual((await calls())[2].body, firstRename, "Reload/rebind retains the unresolved exact operation ID");

  // Space uses native checkbox behavior; Enter uses the production key handler.
  await row("A.sqx").locator("a").click();
  const checkB = row("B.sqx").getByRole("checkbox");
  await checkB.focus(); await checkB.press("Space");
  assert.equal(await checkB.isChecked(), true);
  assert.match(await page.locator("[data-dock-records]").innerText(), /Selected: 2/);
  await checkB.press("Enter");
  assert.equal(await checkB.isChecked(), false);
  assert.match(await page.locator("[data-dock-records]").innerText(), /Selected: 1/);
  await checkB.press("Enter");
  await page.locator('[data-dock-batch="remove"]').click();
  assert.match(await batchForm.innerText(), /Remove 2 strategies/);
  await row("A.sqx").getByRole("checkbox").uncheck();
  await batchForm.waitFor({ state: "hidden" });
  assert.equal((await calls()).filter(call => call.path.endsWith("/remove")).length, 0);
  await page.locator('[data-dock-batch="remove"]').click();
  assert.match(await batchForm.innerText(), /Remove 1 strategies/);
  await row("A.sqx").locator("a").click();
  await batchForm.waitFor({ state: "hidden" });

  // Pure preview/cancel cannot issue confirmation or change native selection.
  await page.locator("[data-dock-purge]").click();
  await purgeForm.waitFor({ state: "visible" });
  assert.match(await purgeForm.innerText(), /1 shared artifacts/);
  await page.locator("[data-dock-purge-cancel]").click();
  await purgeForm.waitFor({ state: "hidden" });
  assert.equal((await calls()).filter(call => call.path.endsWith("purge-confirm")).length, 0);
  assert.equal(await row("A.sqx").getByRole("checkbox").isChecked(), true);
  await page.locator("[data-dock-purge]").click();
  await purgeForm.waitFor({ state: "visible" });
  await row("B.sqx").locator("a").click();
  await purgeForm.waitFor({ state: "hidden" });
  assert.equal(await page.locator("[data-dock-purge]").isVisible(), false, "Unadmitted B has no Candidate purge action");

  // The original preview token remains available after a refused confirmation.
  await row("A.sqx").locator("a").click();
  await page.locator("[data-dock-purge]").click();
  await purgeForm.waitFor({ state: "visible" });
  await purgeForm.locator('button[type="submit"]').click();
  await page.locator("[data-dock-status]").filter({ hasText: "Fixture native removal refused" }).waitFor();
  assert.equal(await purgeForm.isVisible(), true);
  assert.equal(await purgeForm.locator('button[type="submit"]').isEnabled(), true);
  await purgeForm.locator('button[type="submit"]').click();
  await page.locator("[data-dock-status]").filter({ hasText: "Candidate deletion completed" }).waitFor();
  const observed = await calls();
  const confirms = observed.filter(call => call.path.endsWith("purge-confirm"));
  assert.equal(confirms.length, 2);
  for (const call of confirms) assert.deepEqual(call.body, { candidate_entity_id: candidateId, expected_preview_sha256: previewHash });
  assert.equal(observed.filter(call => call.path.endsWith("purge-preview")).length, 3);
  assert.equal(await row("A.sqx").count(), 0);
  assert.equal(await row("B.sqx").count(), 1);
  assert.deepEqual(await page.evaluate(() => window.dockChanged), ["purge"]);

  // A partial native Clear changes the bank. Recovery must replay the frozen
  // request, not obtain a different snapshot from the remaining rows.
  await page.reload();
  await page.evaluate(mountDock, { fixture: results(), afterPurge: results([]), preview, project, partialClear: true });
  await page.locator('[data-dock-batch="clear"]').click();
  await batchForm.waitFor({ state: "visible" });
  assert.match(await batchForm.innerText(), /Remove 2 strategies/);
  await batchForm.locator('button[type="submit"]').click();
  await page.locator("[data-dock-status]").filter({ hasText: "Fixture clear removed A before interruption" }).waitFor();
  const partialCalls = await calls();
  const originalClear = partialCalls.find(call => call.path.endsWith("/clear")).body;
  assert.match(originalClear.operation_id, /^[0-9a-f]{32}$/);
  assert.deepEqual(originalClear, { project, databank: "Results", snapshot_ref: `tc-evidence:sha256:${"9".repeat(64)}`, operation_id: originalClear.operation_id });
  await page.locator(`[data-dock-retry="${originalClear.operation_id}"]`).waitFor({ state: "visible" });
  await page.reload();
  await page.evaluate(mountDock, { fixture: results(["B.sqx"]), afterPurge: results([]), preview, project,
    previousCalls: partialCalls, selectedArchive: "B.sqx", partialClear: true });
  assert.equal(await row("A.sqx").count(), 0);
  assert.equal(await row("B.sqx").count(), 1);
  const retryClear = page.locator(`[data-dock-retry="${originalClear.operation_id}"]`);
  await retryClear.waitFor({ state: "visible" });
  await retryClear.click();
  await page.locator("[data-dock-status]").filter({ hasText: "Original databank operation verified and saved." }).waitFor();
  const recoveredCalls = await calls();
  const clearRequests = recoveredCalls.filter(call => call.path.endsWith("/clear"));
  assert.equal(clearRequests.length, 2);
  assert.deepEqual(clearRequests[1].body, originalClear, "Restart retry preserves the exact operation ID and original full-bank snapshot");
  assert.equal(recoveredCalls.filter(call => call.path.endsWith("/snapshot")).length, 1, "Retry must not replace the confirmed snapshot");
  assert.equal(await retryClear.count(), 0, "Verified completion removes the pending operation control");
  assert.equal(await row("B.sqx").count(), 0);
  assert.deepEqual(await page.evaluate(() => window.dockSelections.at(-1)), [project, "Results", ""], "Deleted archive is cleared from the shared selection after retry");
  assert.deepEqual(await page.evaluate(() => window.dockChanged), ["clear"]);

  // An upload interrupted after retention is recoverable without asking the
  // browser to retain file bytes across a full reload.
  await page.reload();
  await page.evaluate(mountDock, { fixture: results([]), afterPurge: results(["Imported.sqx"]), preview, project, importRetry: true, association });
  await page.locator("[data-dock-load]").setInputFiles({ name: "Imported.sqx", mimeType: "application/octet-stream", buffer: Buffer.from("native import fixture") });
  await page.locator("[data-dock-status]").filter({ hasText: "Fixture import retained" }).waitFor();
  const importCalls = await calls();
  const originalImport = importCalls.find(call => call.path.endsWith("/load")).body;
  assert.match(originalImport.operation_id, /^[0-9a-f]{32}$/);
  assert.match(originalImport.source_sha256, /^[0-9a-f]{64}$/);
  assert.deepEqual(Object.keys(originalImport).sort(), ["archive", "databank", "operation_id", "project", "source_sha256"]);
  await page.reload();
  await page.evaluate(mountDock, { fixture: results([]), afterPurge: results(["Imported.sqx"]), preview, project, importRetry: true, association, previousCalls: importCalls });
  const retryImport = page.locator(`[data-dock-retry="${originalImport.operation_id}"]`);
  await retryImport.click();
  await page.locator("[data-dock-status]").filter({ hasText: "Original databank operation verified and saved" }).waitFor();
  const resumedCalls = await calls();
  const resumed = resumedCalls.find(call => call.path.endsWith("/load-resume"));
  assert.deepEqual(resumed.body, originalImport);
  assert.equal(await retryImport.count(), 0);
  assert.deepEqual(await page.evaluate(() => window.dockSelections.at(-1)), [project, "Results", "Imported.sqx"]);

  // Cancelling a preview is local. Confirmed discard survives reload and never
  // retries the native upload, nor clears another pending mutation or selection.
  await page.reload();
  await page.evaluate(mountDock, { fixture: results(), afterPurge: results(), preview, project, importRetry: true, discardImport: true, association });
  await page.locator("[data-dock-load]").setInputFiles({ name: "Discard.sqx", mimeType: "application/octet-stream", buffer: Buffer.from("unfinished import fixture") });
  await page.locator("[data-dock-status]").filter({ hasText: "Fixture import retained" }).waitFor();
  const discardRequest = (await calls()).find(call => call.path.endsWith("/load")).body;
  await page.locator(`[data-dock-discard="${discardRequest.operation_id}"]`).click();
  await purgeForm.waitFor({ state: "visible" });
  assert.match(await purgeForm.innerText(), /Discard this unfinished import/);
  await page.locator("[data-dock-purge-cancel]").click();
  await purgeForm.waitFor({ state: "hidden" });
  assert.equal((await calls()).filter(call => call.path.endsWith("import-discard-confirm")).length, 0);
  await page.locator(`[data-dock-discard="${discardRequest.operation_id}"]`).click();
  await purgeForm.waitFor({ state: "visible" });
  await purgeForm.locator('button[type="submit"]').click();
  await page.locator("[data-dock-status]").filter({ hasText: "Fixture deletion response interrupted" }).waitFor();
  const discardCalls = await calls();
  await page.reload();
  await page.evaluate(mountDock, { fixture: results(), afterPurge: results(), preview, project, importRetry: true, discardImport: true, association, previousCalls: discardCalls });
  const retryDiscard = page.locator(`[data-dock-retry="${discardRequest.operation_id}"]`);
  assert.match(await retryDiscard.innerText(), /Retry import deletion/);
  await retryDiscard.click();
  await page.locator("[data-dock-status]").filter({ hasText: "Unfinished import discarded" }).waitFor();
  const discardedCalls = await calls();
  const discardConfirms = discardedCalls.filter(call => call.path.endsWith("import-discard-confirm"));
  assert.equal(discardConfirms.length, 2);
  for (const call of discardConfirms) assert.deepEqual(call.body, { ...discardRequest, expected_preview_sha256: previewHash });
  assert.equal(discardedCalls.filter(call => call.path.endsWith("/load-resume")).length, 0);
  assert.equal(await retryDiscard.count(), 0);
  assert.equal(await page.locator(`[data-dock-retry="${firstRename.operation_id}"]`).count(), 1);
  assert.equal(await row("A.sqx").getByRole("checkbox").isChecked(), true);
  assert.deepEqual(await page.evaluate(() => window.dockChanged), ["import-discard"]);

  for (const reason of ["databank_import_discard_preview_changed", "databank_import_submitted"]) {
    await page.reload();
    await page.evaluate(mountDock, { fixture: results(), afterPurge: results(), preview, project, importRetry: true, discardImport: true, discardRefusal: reason, association });
    await page.locator("[data-dock-load]").setInputFiles({ name: `${reason}.sqx`, mimeType: "application/octet-stream", buffer: Buffer.from("preflight refusal fixture") });
    await page.locator("[data-dock-status]").filter({ hasText: "Fixture import retained" }).waitFor();
    const request = (await calls()).find(call => call.path.endsWith("/load")).body;
    const discard = page.locator(`[data-dock-discard="${request.operation_id}"]`);
    await discard.click(); await purgeForm.waitFor({ state: "visible" });
    await purgeForm.locator('button[type="submit"]').click();
    await page.locator("[data-dock-status]").filter({ hasText: "Fixture deletion did not start" }).waitFor();
    assert.equal(await purgeForm.isVisible(), false);
    assert.equal(await discard.isVisible(), true, "An explicit pre-intent refusal permits a fresh review");
    const retry = page.locator(`[data-dock-retry="${request.operation_id}"]`);
    assert.match(await retry.innerText(), /Retry load/);
    if (reason === "databank_import_discard_preview_changed") {
      await discard.click(); await purgeForm.waitFor({ state: "visible" });
      await purgeForm.locator('button[type="submit"]').click();
      await page.locator("[data-dock-status]").filter({ hasText: "Unfinished import discarded" }).waitFor();
      assert.equal((await calls()).filter(call => call.path.endsWith("import-discard-confirm"))[1].body.expected_preview_sha256, "6".repeat(64));
    } else {
      await retry.click();
      await page.locator("[data-dock-status]").filter({ hasText: "Original databank operation verified and saved" }).waitFor();
      assert.deepEqual((await calls()).find(call => call.path.endsWith("/load-resume")).body, request);
    }
  }

  // Merely reading or refreshing a changed native archive never reconnects it.
  const detached = results(["A.sqx"]);
  const detachedRow = detached.projects[0].databanks[0].strategies[0];
  detachedRow.candidate_association = null;
  detachedRow.candidate_reconciliation = { ...association, schema: "tc.research-native-candidate-reconciliation.v1", previous_archive_sha256: "e".repeat(64) };
  await page.reload();
  await page.evaluate(mountDock, { fixture: detached, afterPurge: results(["A.sqx"]), preview, project, reconnect: true, association });
  assert.deepEqual(await calls(), []);
  assert.equal(await page.locator("[data-dock-purge]").isVisible(), false);
  assert.match(await page.locator("[data-dock-reconcile-context]").innerText(), /does not run or validate/);
  await page.locator("[data-dock-reconcile]").click();
  await page.locator("[data-dock-status]").filter({ hasText: "Saved candidate reconnected" }).waitFor();
  const reconnectRequest = (await calls()).find(call => call.path.endsWith("/reconcile")).body;
  assert.deepEqual(reconnectRequest, { project, databank: "Results", archive: "A.sqx", archive_sha256: association.archive_sha256,
    previous_archive_sha256: "e".repeat(64), candidate_entity_id: association.candidate_entity_id,
    candidate_revision: association.candidate_revision, membership_revision: association.membership_revision,
    operation_id: reconnectRequest.operation_id });
  assert.match(reconnectRequest.operation_id, /^[0-9a-f]{32}$/);
  assert.equal(await page.locator("[data-dock-reconcile-context]").isVisible(), false);
  assert.equal(await page.locator("[data-dock-purge]").isVisible(), true);
  detachedRow.candidate_reconciliation.unavailable_reason = "candidate_legacy_reimport_required";
  await page.reload();
  await page.evaluate(mountDock, { fixture: detached, afterPurge: results(["A.sqx"]), preview, project, reconnect: true, association });
  assert.equal(await page.locator("[data-dock-reconcile]").isDisabled(), true);
  assert.match(await page.locator("[data-dock-reconcile-note]").innerText(), /Load .sqx into a different databank/);
  await page.locator("[data-dock-reconcile]").dispatchEvent("click");
  assert.deepEqual(await calls(), [], "Even a dispatched click cannot submit an unavailable reconnect");
  assert.equal(await page.locator("[data-dock-save]").isVisible(), true);
  assert.equal(await page.locator("[data-dock-purge]").isVisible(), false);
  assert.deepEqual(pageErrors, []);
  assert.deepEqual(unexpectedNetwork, []);
  console.log(JSON.stringify({ status: "passed", checks: ["rename target invalidation", "rename operation ID retry across reload/rebind", "bulk confirmation invalidation", "Space/Enter selection counts", "pure preview cancellation", "unadmitted Candidate state", "exact-hash confirmation retry", "partial Clear restart replays frozen snapshot and operation ID", "interrupted import resumes exact original request after reload without file", "explicit candidate reconnect binds old/new archive and membership"], injectedRequests: [...observed, ...recoveredCalls, ...resumedCalls, ...await calls()].map(call => ({ path: call.path, method: call.method })), realApiRequests: 0, pageErrors: [] }, null, 2));
} catch (error) {
  console.error(JSON.stringify(await page.evaluate(() => ({ status: document.querySelector("[data-dock-status]")?.textContent, calls: window.dockCalls })), null, 2));
  throw error;
} finally {
  await browser.close();
}
