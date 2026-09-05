import { chromium } from 'playwright';
import { mkdir, writeFile, readFile } from 'node:fs/promises';
import assert from 'node:assert/strict';
const out=process.env.RESULTS_REVIEW_OUTPUT || '.git/ui-review/results';await mkdir(out,{recursive:true});
const url=process.env.RESULTS_REVIEW_URL;
if(!url) throw new Error('Set RESULTS_REVIEW_URL to an authorized populated Results Overview URL');
const parsed=new URL(url),base=parsed.origin;
const browser=await chromium.launch({ignoreDefaultArgs:['--hide-scrollbars']});
const page=await browser.newPage({viewport:{width:1440,height:1000}});
const errors=[], captures=[], checks=[];
page.on('pageerror',e=>errors.push(e.message));
async function ready(){await page.locator('[data-automation-workflows="loaded"]').waitFor({timeout:45000});}
async function capture(title){await ready();const path=`${title}.png`;await page.screenshot({path:out+'/'+path,fullPage:true});captures.push({title,path,url:page.url()});}
try {
 await page.goto(url);await ready();
 const params=new URLSearchParams({project:parsed.searchParams.get('project') || 'Builder',databank:parsed.searchParams.get('databank'),archive:parsed.searchParams.get('archive')});
 const api=base+'/api/sqx-project-strategy?'+params;
 const original=await (await page.request.get(api)).json();
 await writeFile(out+'/native-read-model.json',JSON.stringify(original,null,2));
 assert.equal(await page.locator('.results-metrics article').count(),6);
 assert.equal(await page.locator('.sqx-task-column').count(),0);
 assert.equal(original.analytics.trades.length,original.analytics.metrics.NumberOfTrades);
 assert.equal(original.analytics.metrics.NetProfit,Math.round(original.analytics.trades.reduce((sum,t)=>sum+t.PL,0)*100)/100);
 await capture('01-overview');
 await page.locator('.results-hero-chart a').click();await ready();
 assert.ok(page.url().includes('resultView=equity'));
 await page.locator('[data-equity-zoom]').fill('3');
 assert.match(await page.locator('[data-equity-tooltip]').innerText(),/Showing/);
 await page.locator('[data-equity-plot]').focus();await page.keyboard.press('ArrowRight');
 assert.match(await page.locator('[data-equity-tooltip]').innerText(),/Drawdown/);
 await page.locator('[data-equity-axis]').selectOption('time');
 await page.locator('[data-equity-reset]').click();
 assert.equal(await page.locator('[data-equity-zoom]').inputValue(),'1');
 await capture('02-equity');
 await page.locator('[data-results-sample]').selectOption('oos');await ready();
 await page.locator('[data-results-direction]').selectOption('short');await ready();
 await page.locator('[data-automation-result-view="trade-analysis"]').click();await ready();
 assert.ok(page.url().includes('sample=oos')&&page.url().includes('direction=short'));
 await page.locator('[data-results-period]').selectOption('open_time');await ready();
 await page.reload();await ready();
 assert.equal(await page.locator('[data-results-period]').inputValue(),'open_time');
 const filtered=await (await page.request.get(api+'&sample=oos&direction=short&period_by=open_time')).json();
 assert.equal(filtered.analytics.trades.length,filtered.analytics.metrics.NumberOfTrades);
 assert.equal(await page.locator('.results-metrics article').nth(2).locator('strong').innerText(),String(filtered.analytics.trades.length));
 checks.push('filters persist across tabs/reload and agree with API');
 for(const suffix of ['&sample=bad','&direction=bad','&period_by=bad','&sample=is&sample=oos'])assert.equal((await page.request.get(api+suffix)).status(),400);
 await page.goto(url.replace('overview','trade-analysis'));await ready();await capture('03-analysis');
 const scroll=page.locator('.automation-detail-grid');
 const maxScroll=await scroll.evaluate(n=>n.scrollHeight-n.clientHeight);
 const step=await scroll.evaluate(n=>n.clientHeight-30);
 for(let top=step,index=2;top<maxScroll+step;top+=step,index++){
  await scroll.evaluate((n,y)=>{n.scrollTop=y;},Math.min(top,maxScroll));
  await capture(`03-analysis-${String(index).padStart(2,'0')}`);
 }
 await page.locator('[data-automation-result-view="trades"]').click();await ready();await capture('04-trades');
 const expand=page.locator('[data-results-dock-expand]');await expand.focus();await page.keyboard.press('Enter');
 assert.equal(await expand.getAttribute('aria-pressed'),'true');await expand.click();
 await page.locator('[data-databank-dock] > summary').focus();await page.keyboard.press('Enter');
 assert.equal(await page.locator('[data-databank-dock]').getAttribute('open'),null);await page.keyboard.press('Enter');
 for(const [view,title]of [['prop-mc','05-prop-mc'],['prop-analytics','06-prop-analytics']]){
  await page.locator(`[data-automation-result-view="${view}"]`).click();await ready();
  await page.waitForFunction(()=>document.querySelector('[data-results-plugin]')?.contentDocument?.documentElement.style.getPropertyValue('--bg-body'));
  await capture(title);
 }
 for(const [view,title]of [['sp-overview','08-symbols'],['profile','09-profile'],['config','10-config'],['source','11-source'],['chart','12-price-chart']]) {
  await page.locator(`[data-automation-result-view="${view}"]`).click();await ready();
  if(view==='source') {
   await page.waitForFunction(()=>document.querySelector('[data-source-text]')?.textContent?.includes('<'));
   const source=await page.locator('[data-source-text]').innerText();
   const [download]=await Promise.all([page.waitForEvent('download'),page.locator('[data-source-save]').click()]);
   assert.equal(await readFile(await download.path(),'utf8'),source);
   checks.push('native source export equals visible source');
  }
  await capture(title);
 }
 await page.goto(url);await ready();
 const second=page.locator('[data-databank-dock] tr[data-automation-archive]').nth(1);
 if(await second.count()) {
  const name=await second.getAttribute('data-automation-archive');await second.click();
  assert.equal(new URL(page.url()).searchParams.get('archive'),parsed.searchParams.get('archive'));
  await second.dblclick();await ready();
  assert.equal(new URL(page.url()).searchParams.get('archive'),name);
  assert.match(await page.locator('.results-strategy-header h1').innerText(),new RegExp(name.replace(/\.sqx$/i,'')));
  checks.push('bulk selection keeps viewed identity; double-click switches strategy');
 }
 await page.goto(url);await ready();await page.setViewportSize({width:960,height:1000});await capture('07-overview-960');
 assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false);
 const after=await(await page.request.get(api)).json();assert.equal(after.archive_sha256,original.archive_sha256);
 checks.push('archive SHA unchanged','equity zoom/reset/keyboard','databank expand/collapse keyboard','960px no document overflow');
 assert.deepEqual(errors,[]);
}finally{await browser.close();await writeFile(out+'/receipt.json',JSON.stringify({captures,checks,errors},null,2));}
