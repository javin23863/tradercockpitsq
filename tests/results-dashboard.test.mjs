import test from "node:test";
import assert from "node:assert/strict";
import { equityPanel, overviewDashboard, metricCards, date } from "../web/results-dashboard.mjs";
import { workflowHref } from "../web/automation-settings-controls.mjs";
import { fetchProjectStrategy } from "../web/automation-results.mjs";

test("Results presentation retains filter custody and shows paired balance/drawdown", () => {
  const a = { capital: null, metrics: {NetProfit: -10, NumberOfTrades: 1, Drawdown: 10},
    equity: [{trade:1,time:null,balance:-10,drawdown:-10}], time_axis_available: false,
    distribution: [{from:-10,to:-10,count:1}], periods:{year:[]}, sides:[] };
  const view = {databank:"Results",archive:"A.sqx",sample:"oos",direction:"short",period_by:"open_time"};
  const html = overviewDashboard({ project:"Builder",analytics:a },view);
  assert.match(html,/Closed-trade cumulative P\/L/);
  assert.match(html,/Closed-trade drawdown/);
  assert.match(html,/sample=oos/);
  assert.match(html,/direction=short/);
  assert.match(html,/period_by=open_time/);
  assert.match(equityPanel(a),/value="time" disabled/);
  assert.match(equityPanel(a),/aria-label="Closed-trade cumulative P\/L with aligned drawdown"/);
  assert.match(html,/Cumulative P\/L &amp; drawdown/);
  assert.equal((metricCards(a).match(/<article>/g)||[]).length,6);
  assert.equal(date(1704067200000),"2024-01-01");
  assert.equal(date(null),"Time unavailable");
  assert.match(workflowHref({project:"Builder",...view}),/period_by=open_time/);
});

test("an archive response cannot be painted under a different selected identity", async () => {
  const payload = {schema:"tc.sqx-custom-project-strategy.v1",source_build:"144.2953",project:"Builder",databank:"Results",archive:"Other.sqx",
    relative_path:"user/projects/Builder/databanks/Results/Other.sqx",archive_sha256:"a".repeat(64),native_version:"1",archive_entries:[],
    orders:{state:"unavailable",reason_code:"orders_missing"},equity:[],settings:[],config_diff:[],chart:{stored:false}};
  await assert.rejects(fetchProjectStrategy("Builder","Results","Selected.sqx",1,async()=>({ok:true,json:async()=>payload})),/does not match the selected archive/);
});
