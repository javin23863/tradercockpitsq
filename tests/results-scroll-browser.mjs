import { chromium } from 'playwright';
import assert from 'node:assert/strict';

const url = process.env.RESULTS_REVIEW_URL;
if (!url) throw new Error('Set RESULTS_REVIEW_URL to an authorized populated Results URL');
const browser = await chromium.launch({ ignoreDefaultArgs: ['--hide-scrollbars'] });
const page = await browser.newPage();
const ready = () => page.locator('[data-automation-workflows="loaded"]').waitFor();
const selectWithKeys = async (selector, keys) => {
  await page.locator(selector).click();
  for (const key of keys) await page.keyboard.press(key);
  await page.keyboard.press('Enter');
  await ready();
};
try {
  for (const width of [1440, 960]) {
    await page.setViewportSize({ width, height: 1000 });
    await page.goto(url.replace('resultView=overview', 'resultView=trade-analysis')); await ready();
    const grid = page.locator('.sqx-databank-grid');
    assert.ok(await grid.evaluate(n => n.scrollWidth > n.clientWidth), 'The visible databank grid must own horizontal scrolling');
    assert.ok(await grid.evaluate(n => n.getBoundingClientRect().bottom <= n.closest('[data-databank-dock]').getBoundingClientRect().bottom), 'Databank scrollbar must fit inside the drawer');
    await grid.focus(); await page.keyboard.press('ArrowRight');
    await page.waitForFunction(() => document.querySelector('.sqx-databank-grid').scrollLeft > 0);
    const main = page.locator('.automation-detail-grid');
    await main.focus(); await page.keyboard.press('PageDown');
    await page.waitForFunction(() => document.querySelector('.automation-detail-grid').scrollTop > 0);
    const dockTop = (await page.locator('[data-databank-dock]').boundingBox()).y;
    await main.hover(); await page.mouse.wheel(0, 600);
    assert.equal((await page.locator('[data-databank-dock]').boundingBox()).y, dockTop);
    await page.locator('[data-automation-result-view="equity"]').click(); await ready();
    await selectWithKeys('[data-results-sample]', ['Home', 'ArrowDown', 'ArrowDown']);
    assert.equal(new URL(page.url()).searchParams.get('sample'), 'oos');
    await selectWithKeys('[data-results-direction]', ['Home', 'ArrowDown']);
    assert.equal(new URL(page.url()).searchParams.get('direction'), 'long');
    await selectWithKeys('[data-dock-columns]', ['Home', 'ArrowDown', 'ArrowDown']);
    assert.equal(await page.locator('[data-dock-columns]').inputValue(), 'oos');
    assert.equal(await page.locator('.sqx-databank-grid th[data-column-sample="is"]').count(), 0);
    assert.ok(await page.locator('.sqx-databank-grid th[data-column-sample="oos"]').count());
    await page.locator('[data-dock-rename]').click();
    await page.locator('[data-dock-name-form] input').fill('Cancelled rename');
    await page.locator('[data-dock-cancel]').click();
    assert.ok(await page.locator('[data-dock-name-form]').isHidden());
    await page.locator('[data-automation-result-view="overview"]').click(); await ready();
    await selectWithKeys('[data-results-template]', ['Home', 'ArrowDown']);
    assert.equal(await page.locator('.results-native-report').getAttribute('open'), '');
    assert.ok(await page.locator('[data-results-overview-stats]').isVisible());
  }
  // Browser-only fixture: expose a long databank without writing any archive or native state.
  await page.route('**/api/sqx-project-results?*', async route => {
    const response = await route.fetch();
    const payload = await response.json();
    const project = payload.projects.find(p => p.name === 'Builder');
    const bank = project.databanks.find(b => b.name === 'Results');
    const first = bank.strategies[0];
    for (let i = 0; i < 40; i++) bank.strategies.push({ ...first,
      archive: `Scroll fixture ${i}.sqx`, relative_path: `user/projects/Builder/databanks/Results/Scroll fixture ${i}.sqx`,
      candidate_association: null, candidate_reconciliation: null });
    bank.strategy_count = bank.strategies.length;
    project.strategy_count += 40;
    await route.fulfill({ response, json: payload });
  });
  await page.goto(url); await ready();
  const grid = page.locator('.sqx-databank-grid');
  assert.ok(await grid.evaluate(n => n.scrollHeight > n.clientHeight));
  await grid.hover(); await page.mouse.wheel(0, 2000);
  await page.waitForFunction(() => document.querySelector('.sqx-databank-grid').scrollTop > 0);
  assert.equal(await page.locator('.automation-detail-grid').evaluate(n => n.scrollTop), 0);
  await grid.focus(); await page.keyboard.press('Control+End');
  await page.waitForFunction(() => { const n = document.querySelector('.sqx-databank-grid'); return n.scrollHeight - n.clientHeight - n.scrollTop < 2; });
  await page.locator('[data-results-dock-expand]').focus(); await page.keyboard.press('Enter');
  await selectWithKeys('[data-dock-columns]', ['Home', 'ArrowDown']);
  assert.equal(await page.locator('[data-results-dock-expand]').getAttribute('aria-pressed'), 'true');
  await page.unroute('**/api/sqx-project-results?*');
  let removed = false;
  await page.route('**/api/sqx-project-results?*', async route => {
    const response = await route.fetch(); const payload = await response.json();
    if (removed) {
      const project = payload.projects.find(p => p.name === 'Builder');
      const bank = project.databanks.find(b => b.name === 'Results');
      bank.strategies = bank.strategies.filter(s => s.archive !== new URL(url).searchParams.get('archive'));
      bank.strategy_count = bank.strategies.length; project.strategy_count--;
    }
    await route.fulfill({ response, json: payload });
  });
  // Guard every mutation: this removal is a browser fixture, never a native request.
  await page.route('**/api/**', async route => {
    if (route.request().method() === 'GET') return route.fallback();
    if (new URL(route.request().url()).pathname !== '/api/sqx-databank/remove') return route.abort();
    removed = true;
    await route.fulfill({ json: { schema: 'tc.sqx-databank-action.v1', action: 'remove', ...route.request().postDataJSON(),
      producer: 'sqx_local_web', persisted: true, results: [] } });
  });
  await page.goto(url); await ready();
  await page.locator('[data-results-dock-expand]').click();
  await selectWithKeys('[data-dock-columns]', ['Home', 'ArrowDown', 'ArrowDown']);
  const checked = await page.locator('.sqx-databank-grid input:checked').count();
  await page.locator('[data-dock-refresh]').click();
  await page.waitForFunction(() => !document.querySelector('[data-dock-refresh]').disabled);
  assert.equal(await page.locator('[data-results-dock-expand]').getAttribute('aria-pressed'), 'true');
  assert.equal(await page.locator('[data-dock-columns]').inputValue(), 'oos');
  assert.equal(await page.locator('.sqx-databank-grid input:checked').count(), checked);
  await page.locator('[data-dock-batch="remove"]').click();
  await page.locator('[data-dock-batch-form] button[type="submit"]').click();
  await page.waitForFunction(() => !new URL(location.href).searchParams.has('archive'));
  assert.equal(await page.locator('.results-strategy-header').count(), 0, 'Removing the inspected archive clears its old charts');
  removed = false;
  await page.goto(url); await ready();
  removed = true;
  await page.locator('[data-dock-refresh]').click();
  await page.waitForFunction(() => !new URL(location.href).searchParams.has('archive'));
  assert.equal(await page.locator('.results-strategy-header').count(), 0, 'Refresh clears an archive removed outside the workspace');
  console.log('PASS: independent scrolling, IS/OOS columns, filters, menus, cancellation, removed archive and Refresh reconciliation at 1440/960');
} finally { await browser.close(); }
