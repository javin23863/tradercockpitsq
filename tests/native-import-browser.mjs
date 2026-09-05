// Explicit native acceptance only: manifest supplied by the owned isolated-runtime harness.
import assert from 'node:assert/strict';
import { readFile, writeFile } from 'node:fs/promises';
import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { chromium } from 'playwright';
const manifest = JSON.parse(await readFile(process.argv[2], 'utf8'));
const { base, project, bank, archive, destination, run, archive_sha256 } = manifest;
const ownedBanks = [destination, destination + ' Copy', destination + ' Move'];
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
    assert.ok(['/api/desktop/session', ...['save', 'create', 'load', 'rename', 'copy', 'move', 'remove', 'clear', 'snapshot', 'export'].map(name => '/api/sqx-databank/' + name)].includes(path), `Unexpected mutation: ${path}`);
    if (path === '/api/sqx-databank/load') {
      const target = JSON.parse(decodeURIComponent(req.headers()['x-tradercockpit-target']));
      assert.equal(target.project, project);
      assert.equal(target.databank, destination, 'Never import into an unrelated databank');
      assert.equal(target.archive, archive);
    } else if (path !== '/api/desktop/session') {
      const target = req.postDataJSON();
      assert.equal(target.project, project);
      assert.ok(ownedBanks.includes(target.databank) || (path.endsWith('/save') && target.databank === bank));
      if (target.target_databank) {
        assert.equal(target.target_project, project);
        assert.ok(ownedBanks.includes(target.target_databank));
      }
    }
  }
  return route.continue();
});
async function action(name, click) {
  const pending = page.waitForResponse(res => res.url().endsWith('/api/sqx-databank/' + name));
  await click();
  const response = await pending;
  const body = ['save', 'export'].includes(name) ? null : await response.json();
  operations.push({ action: name, status: response.status(), body, request: name === 'load' ? null : response.request().postDataJSON() });
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
  // Continue with the same admitted Candidate through native membership actions.
  async function selectBank(name, strategy) {
    await page.locator(`[data-dock-bank="${name}"]`).click();
    if (strategy) await page.locator(`tr[data-automation-archive="${strategy}"] a`).click();
    await page.locator('[data-dock-refresh]:not([disabled])').waitFor();
  }
  async function batch(name, target) {
    await page.locator(`[data-dock-batch="${name}"]`).click();
    const form = page.locator('[data-dock-batch-form]');
    await form.waitFor({ state: 'visible' });
    if (target) await form.locator('select').selectOption(target);
    return action(name, () => form.locator('button[type="submit"]').click());
  }
  for (const name of ownedBanks.slice(1)) {
    await page.locator('[data-dock-new]').click();
    await form.locator('input').fill(name);
    await action('create', () => form.locator('button[type="submit"]').click());
  }
  await selectBank(destination, archive);
  await page.locator('[data-dock-rename]').click();
  await form.locator('input').fill('Saved research strategy');
  const renamed = await action('rename', () => form.locator('button[type="submit"]').click());
  const identity = async name => {
    const rows = await row(name);
    assert.equal(rows.length, 1);
    assert.equal(rows[0].candidate_association.candidate_entity_id, imported.candidate_entity_id);
    assert.equal(rows[0].candidate_association.candidate_revision, imported.candidate_revision);
    return rows[0];
  };
  await identity(destination);
  await batch('copy', ownedBanks[1]);
  await identity(ownedBanks[1]);
  await selectBank(ownedBanks[1], renamed.archive);
  await batch('remove');
  assert.equal((await row(ownedBanks[1])).length, 0);
  await identity(destination);
  await selectBank(destination, renamed.archive);
  await batch('copy', ownedBanks[1]);
  const copies = operations.filter(op => op.action === 'copy');
  assert.notEqual(copies[0].request.operation_id, copies[1].request.operation_id);
  await selectBank(ownedBanks[1], renamed.archive);
  const exportEvent = page.waitForEvent('download');
  await action('export', () => page.locator('[data-dock-batch="export"]').click());
  await (await exportEvent).saveAs(run + '/native-selected-export.zip');
  const exported = await identity(ownedBanks[1]);
  await writeFile(run + '/native-selected-export.json', JSON.stringify({ archive: exported.archive, archive_sha256: exported.archive_sha256 }));
  execFileSync(process.env.PYTHON || 'python', ['-c', 'import sys,zipfile,hashlib,json; z=zipfile.ZipFile(sys.argv[1]); assert z.namelist()==["manifest.json",sys.argv[2]]; assert hashlib.sha256(z.read(sys.argv[2])).hexdigest()==sys.argv[3]; assert json.loads(z.read("manifest.json"))==dict(project=sys.argv[4],databank=sys.argv[5],archives=[dict(archive=sys.argv[2],archive_sha256=sys.argv[3])])', run + '/native-selected-export.zip', exported.archive, exported.archive_sha256, project, ownedBanks[1]]);
  await batch('move', ownedBanks[2]);
  assert.equal((await row(ownedBanks[1])).length, 0);
  await identity(ownedBanks[2]);
  await selectBank(ownedBanks[2], renamed.archive);
  await batch('clear');
  assert.equal((await row(ownedBanks[2])).length, 0);
  await identity(destination);
  await selectBank(destination, renamed.archive);
  await page.locator(`tr[data-automation-archive="${renamed.archive}"]`).dblclick();
  await page.reload();
  await page.locator('.results-strategy-header').waitFor();
  assert.equal(new URL(page.url()).searchParams.get('databank'), destination);
  assert.equal(new URL(page.url()).searchParams.get('archive'), renamed.archive);
  assert.equal(await page.locator('.results-strategy-header h1').innerText(), renamed.archive.replace(/\.sqx$/i, ''));
  assert.equal(await page.locator(`tr[data-automation-archive="${renamed.archive}"] input[type="checkbox"]`).isChecked(), true);
  const final = await identity(destination);
  assert.deepEqual(errors, []);
  await page.screenshot({ path: run + '/native-import-reopened.png', fullPage: true });
  await writeFile(run + '/native-import-browser.json', JSON.stringify({ status: 'passed', imported, reopened, final, source_sha256: hash(original), saved_sha256: hash(saved), url: page.url(), operations, errors }, null, 2));
} catch (error) {
  await page.screenshot({ path: run + '/native-import-failure.png', fullPage: true });
  throw error;
} finally { await browser.close(); }
