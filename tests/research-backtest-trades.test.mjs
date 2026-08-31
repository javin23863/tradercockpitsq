import assert from "node:assert/strict";
import test from "node:test";

import {
  fetchHistoricalResultDetail,
  historicalResultDetailFromPayload,
  historicalTradesFromPayload,
} from "../web/research-backtest-trades.mjs";

const candidateArchiveSha = "a".repeat(64);
const resultArchiveSha = "b".repeat(64);
const projectSha = "c".repeat(64);
const engineSha = "d".repeat(64);
const launcherSha = "e".repeat(64);
const resultStrategySha = "f".repeat(64);
const resultSettingsSha = "1".repeat(64);
const ordersSha = "6".repeat(64);
const candidateEntity = "tc-research:candidate:v1:11111111-1111-4111-8111-111111111111";
const candidateRevision = `tc-research-revision:candidate:sha256:${"2".repeat(64)}`;
const resultEntity = "tc-research:historical-result:v1:33333333-3333-4333-8333-333333333333";
const resultRevision = `tc-research-revision:historical-result:sha256:${"3".repeat(64)}`;
const projectName = "TraderCockpit-Retester-33333333333343338333333333333333";

function result(overrides = {}) {
  return {
    schema: "tc.research-historical-result.v1",
    entity_id: resultEntity,
    revision: resultRevision,
    parent_revision: `tc-research-revision:historical-result:sha256:${"4".repeat(64)}`,
    state: "completed",
    candidate_entity_id: candidateEntity,
    candidate_revision: candidateRevision,
    candidate_archive_name: "Survivor.sqx",
    candidate_archive_ref: `tc-evidence:sha256:${candidateArchiveSha}`,
    candidate_archive_sha256: candidateArchiveSha,
    sqx_build: "144.2953",
    operation: "native_retester_task_1",
    retester_task: 1,
    native_project_name: projectName,
    native_project_relative_path: `user/projects/${projectName}/project.cfx`,
    source_project_ref: `tc-evidence:sha256:${projectSha}`,
    source_project_sha256: projectSha,
    engine_ref: `tc-evidence:sha256:${engineSha}`,
    engine_sha256: engineSha,
    launcher_sha256: launcherSha,
    receipts: [{ action: "startOnlyTask", state: "completed", task: 1, exit_code: 0 }],
    partial_side_effect: false,
    result_archive_name: "Survivor.sqx",
    result_archive_relative_path: `user/projects/${projectName}/databanks/Results/Survivor.sqx`,
    result_archive_ref: `tc-evidence:sha256:${resultArchiveSha}`,
    result_archive_sha256: resultArchiveSha,
    result_strategy_ref: `tc-evidence:sha256:${resultStrategySha}`,
    result_strategy_sha256: resultStrategySha,
    result_settings_ref: `tc-evidence:sha256:${resultSettingsSha}`,
    result_settings_sha256: resultSettingsSha,
    failure_reason_code: null,
    execution_completed: true,
    validation_state: "not_run",
    reused: false,
    ...overrides,
  };
}

function trade(overrides = {}) {
  return {
    Symbol: "EURUSD",
    SetupName: "Setup A",
    StrategyName: "Strategy A",
    Comment: "native",
    Ticket: 7,
    Order: 7,
    Type: 1,
    CloseType: 0,
    SampleType: 1,
    OriginalOpenTime: 1700000000000,
    OriginalType: 1,
    Size: 1,
    OriginalPrice: 1.1,
    OpenTime: 1700000000000,
    OpenPrice: 1.1,
    CloseTime: 1700003600000,
    ClosePrice: 1.11,
    StopLoss: 1.09,
    TakeProfit: 1.12,
    BarsInTrade: 12,
    PL: 100,
    PctPL: 1,
    PctPL_TWR: 1,
    PipsPL: 10,
    DD: -25,
    PctDD: -0.25,
    PipsDD: -2.5,
    CommSwap: -1.5,
    CommSwapApplied: true,
    MAE: -30,
    PipsMAE: -3,
    MFE: 120,
    PipsMFE: 12,
    Duration: 3600,
    AccountBalance: 10100,
    PctAccountBalance: 1.01,
    PipsAccountBalance: 1010,
    MagicNumber: 42,
    IsInPortfolio: 1,
    Extra1: 0,
    SlippageInMoney: 0.25,
    ExitIndex: 2,
    ATROnOpen: 0.0015,
    ...overrides,
  };
}

function trades(overrides = {}) {
  return {
    schema: "tc.research-historical-trades.v1",
    historical_result_entity_id: resultEntity,
    historical_result_revision: resultRevision,
    candidate_entity_id: candidateEntity,
    candidate_revision: candidateRevision,
    result_archive_ref: `tc-evidence:sha256:${resultArchiveSha}`,
    result_archive_sha256: resultArchiveSha,
    sqx_build: "144.2953",
    orders_format: "SQOrderFileFormat:11",
    orders_format_version: 11,
    orders_entry: "orders.bin",
    orders_entry_sha256: ordersSha,
    native_order_count: 3,
    trade_count: 1,
    selection: {
      result_key: "Portfolio",
      direction: 0,
      sample_type: 127,
      expired: false,
      control_orders: false,
      native_filter: "filterExcludingControlOrders",
    },
    trades: [trade()],
    ...overrides,
  };
}

function response(payload, { ok = true, status = 200 } = {}) {
  return { ok, status, async json() { return payload; } };
}

test("Trades readback accepts only exact native Portfolio selection bound to one result revision", () => {
  const parsed = historicalTradesFromPayload(trades(), result());
  assert.equal(parsed.trade_count, 1);
  assert.equal(parsed.trades[0].Ticket, 7);
  assert.equal(parsed.trades[0].Duration, 3600);

  assert.throws(
    () => historicalTradesFromPayload(trades({ historical_result_revision: `tc-research-revision:historical-result:sha256:${"8".repeat(64)}` }), result()),
    /does not bind the selected result revision/,
  );
  assert.throws(
    () => historicalTradesFromPayload(trades({ selection: { ...trades().selection, native_filter: "synthetic" } }), result()),
    /producer contract is invalid/,
  );
  assert.throws(
    () => historicalTradesFromPayload(trades({ trades: [trade({ Type: 3 })] }), result()),
    /Native trade record is invalid/,
  );
});

test("Historical Result detail validates available and unavailable Trades states", () => {
  const available = historicalResultDetailFromPayload({
    ...result(),
    trades_readback: { state: "available", payload: trades() },
  });
  assert.equal(available.result.revision, resultRevision);
  assert.equal(available.tradesReadback.payload.schema, "tc.research-historical-trades.v1");

  const unavailable = historicalResultDetailFromPayload({
    ...result(),
    trades_readback: {
      state: "unavailable",
      reason_code: "sqx_orders_member_invalid",
      detail: "orders.bin is unavailable",
    },
  });
  assert.equal(unavailable.tradesReadback.state, "unavailable");
  assert.equal(unavailable.tradesReadback.reason_code, "sqx_orders_member_invalid");
});

test("Trades detail fetch selects only one exact Historical Result entity", async () => {
  let request;
  const detail = await fetchHistoricalResultDetail(resultEntity, async (url, options) => {
    request = { url, options };
    return response({ ...result(), trades_readback: { state: "available", payload: trades() } });
  });

  assert.equal(request.url, `/api/research/historical-results?entityId=${encodeURIComponent(resultEntity)}`);
  assert.deepEqual(request.options, { headers: { accept: "application/json" } });
  assert.equal(detail.result.entity_id, resultEntity);
  assert.equal(detail.tradesReadback.payload.result_archive_sha256, resultArchiveSha);

  await assert.rejects(
    fetchHistoricalResultDetail(resultEntity, async () => response({
      ...result({ entity_id: "tc-research:historical-result:v1:44444444-4444-4444-8444-444444444444" }),
      trades_readback: { state: "available", payload: trades() },
    })),
    /does not bind|identity changed/,
  );
});
