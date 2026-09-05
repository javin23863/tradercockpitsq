// Explicit native acceptance only: manifest supplied by the owned isolated-runtime harness.
import assert from 'node:assert/strict';
import { readFile, writeFile } from 'node:fs/promises';
import { createHash } from 'node:crypto';
import { chromium } from 'playwright';
const manifest = JSON.parse(await readFile(process.argv[2], 'utf8'));
const { base, project, bank, archive, destination, run, archive_sha256 } = manifest;
assert.equal(new URL(base).hostname, '127.0.0.1');
assert.equal(project, 'Builder');
assert.match(destination, /^TC UI Legacy [a-f0-9]{8}$/);
const hash = bytes => createHash('sha256').update(bytes).digest('hex');
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
page.setDefaultTimeout(60000);
const operations = [], errors = [];
page.on('pageerror', error => errors.push(error.message));
await page.route('**/api/**', route => {
  const req = route.request(), path = new URL(req.url()).pathname;
  if (req.method() !== 'GET') {
    assert.ok(['/api/desktop/session', '/api/sqx-databank/save', '/api/sqx-databank/create', '/api/sqx-databank/load'].includes(path), `Unexpected mutation: ${path}`);
    if (path === '/api/sqx-databank/load') {
      const target = JSON.parse(decodeURIComponent(req.headers()['x-tradercockpit-target']));
      assert.equal(target.project, project);
      assert.equal(target.databank, destination, 'Never import into an unrelated databank');
      assert.equal(target.archive, archive);
    }
  }
  return route.continue();
});
async function action(name, click) {
  const pending = page.waitForResponse(res => res.url().endsWith('/api/sqx-databank/' + name));
  await click();
  const response = await pending;
  const body = name === 'save' ? null : await response.json();
  operations.push({ action: name, status: response.status(), body });
  await writeFile(run + '/native-import-operations.json', JSON.stringify(operations, null, 2));
  assert.equal(response.status(), 200, JSON.stringify(body));
  await page.locator('[data-dock-refresh]:not([disabled])').waitFor();
  return body;
}
async function row(databank) {
  const response = await page.request.get(base + '/api/sqx-project-results?' + new URLSearchParams({ project }));
  assert.ok(response.ok());
  const payload = await response.json();
  return payload.projects.find(p => p.name === project).databanks.find(b => b.name === databank).strategies;
}
try {
  await page.goto(base + '/builder?' + new URLSearchParams({ tab: 'results', task: '1', databank: bank, archive, resultView: 'overview' }));
  await page.locator('[data-dock-reconcile-context]:not([hidden])').waitFor();
  assert.equal(await page.locator('[data-dock-reconcile]').isDisabled(), true);
  assert.equal((await row(bank)).find(r => r.archive === archive).candidate_reconciliation.unavailable_reason, 'candidate_legacy_reimport_required');
  const downloadEvent = page.waitForEvent('download');
  await action('save', () => page.locator('[data-dock-save]').click());
  const download = await downloadEvent;
  const original = await readFile(await download.path());
  assert.equal(hash(original), archive_sha256);
  await page.locator('[data-dock-new]').click();
  const form = page.locator('[data-dock-name-form]');
  await form.locator('input').fill(destination);
  await action('create', () => form.locator('button[type="submit"]').click());
  assert.equal((await row(destination)).length, 0);
  const imported = await action('load', () => page.locator('[data-dock-load]').setInputFiles({ name: archive, mimeType: 'application/octet-stream', buffer: original }));
  assert.notEqual(imported.candidate_entity_id, manifest.candidate.entity_id);
  assert.equal(imported.source_sha256, archive_sha256);
  const repeated = await action('load', () => page.locator('[data-dock-load]').setInputFiles({ name: archive, mimeType: 'application/octet-stream', buffer: original }));
  assert.equal(repeated.candidate_entity_id, imported.candidate_entity_id);
  assert.equal(repeated.candidate_revision, imported.candidate_revision);
  assert.equal(repeated.reused, true);
  assert.equal((await row(destination)).length, 1);
  const importedRow = page.locator(`tr[data-automation-archive="${archive}"]`);
  await importedRow.dblclick();
  await page.locator('.results-strategy-header').waitFor();
  await page.setViewportSize({ width: 960, height: 1000 });
  await page.reload();
  await page.locator('[data-dock-purge]:not([hidden])').waitFor();
  const reopened = (await row(destination))[0].candidate_association;
  assert.equal(reopened.candidate_entity_id, imported.candidate_entity_id);
  assert.equal(reopened.candidate_revision, imported.candidate_revision);
  const savedEvent = page.waitForEvent('download');
  await action('save', () => page.locator('[data-dock-save]').click());
  const saved = await readFile(await (await savedEvent).path());
  assert.equal(hash(saved), reopened.archive_sha256);
  assert.deepEqual(errors, []);
  await page.screenshot({ path: run + '/native-import-reopened.png', fullPage: true });
  await writeFile(run + '/native-import-browser.json', JSON.stringify({ status: 'passed', imported, reopened, source_sha256: hash(original), saved_sha256: hash(saved), url: page.url(), operations, errors }, null, 2));
} catch (error) {
  await page.screenshot({ path: run + '/native-import-failure.png', fullPage: true });
  throw error;
} finally { await browser.close(); }
