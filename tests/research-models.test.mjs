import assert from "node:assert/strict";
import test from "node:test";

import { parseModelsCatalog, renderModelsPanel } from "../web/research-models.mjs";

const FAMILIES = [
  { family_id: "sklearn.linear_model.LogisticRegression", label: "Logistic regression", enabled: true },
  { family_id: "sklearn.tree.DecisionTreeClassifier", label: "Decision tree", enabled: true },
  { family_id: "sklearn.ensemble.RandomForestClassifier", label: "Random forest", enabled: true },
  { family_id: "sklearn.ensemble.GradientBoostingClassifier", label: "Gradient boosting", enabled: true },
];

function catalog(overrides = {}) {
  return {
    schema: "tc.research-ml-model-catalog.v1",
    scope: "historical_research",
    backend: "sklearn",
    backend_available: true,
    reason_code: null,
    feature_names: ["Duration", "MAE", "MFE", "PipsPL"],
    label_rule: "producer_pl_positive",
    families: FAMILIES,
    models: [],
    detail: "Allowlisted sklearn classifiers fit on native SQX trade records.",
    ...overrides,
  };
}

test("models parser requires sklearn family ids and refuses a path write shape", () => {
  const parsed = parseModelsCatalog(catalog());
  assert.equal(parsed.families[0].family_id, "sklearn.linear_model.LogisticRegression");
  assert.equal(parsed.families.length, 4);
  assert.throws(() => parseModelsCatalog({ ...catalog(), schema: "nope" }), /schema mismatch/);
  assert.throws(
    () => parseModelsCatalog(catalog({ families: [{ family_id: "C:/models/tree.pkl", label: "x" }] })),
    /family identity/,
  );
});

test("models panel fits the four enabled families by Historical Result identity only", () => {
  const html = renderModelsPanel(parseModelsCatalog(catalog()), [
    { entity_id: "tc-research:historical-result:v1:33333333-3333-4333-8333-333333333333", revision: "tc-research-revision:historical-result:sha256:" + "3".repeat(64), state: "completed", execution_completed: true },
  ]);
  assert.match(html, /data-ml-fit="sklearn\.linear_model\.LogisticRegression"/);
  assert.match(html, /data-ml-fit="sklearn\.tree\.DecisionTreeClassifier"/);
  assert.match(html, /data-ml-fit="sklearn\.ensemble\.RandomForestClassifier"/);
  assert.match(html, /data-ml-fit="sklearn\.ensemble\.GradientBoostingClassifier"/);
  assert.doesNotMatch(html, /Neural Net|MLPClassifier/);
  assert.doesNotMatch(html, /<input[^>]+type="file"/);
  assert.doesNotMatch(html, /name="(?:path|sqx_home|model_path)"/);
});

test("fitted rows publish train, OOS, expected value, and Sharpe", () => {
  const html = renderModelsPanel(parseModelsCatalog(catalog({
    models: [{
      family_id: "sklearn.tree.DecisionTreeClassifier",
      label: "Decision tree",
      historical_result_entity_id: "tc-research:historical-result:v1:33333333-3333-4333-8333-333333333333",
      historical_result_revision: "tc-research-revision:historical-result:sha256:" + "3".repeat(64),
      trade_count: 8,
      artifact_sha256: "a".repeat(64),
      train_accuracy: 1,
      oos_accuracy: 0.5,
      expected_value: {
        status: "available",
        n: 8,
        n_win: 4,
        p_win: 0.5,
        avg_win: 20,
        avg_loss: -10,
        expected_value: 5,
      },
      sharpe: {
        status: "available",
        sharpe: 0.4,
        n: 8,
        mean_return: 5,
        stdev_return: 12.5,
      },
    }],
  })));
  assert.match(html, /Train 1\.00/);
  assert.match(html, /OOS 0\.50/);
  assert.match(html, /EV 5\.00/);
  assert.match(html, /Sharpe 0\.40/);
  assert.match(html, /p_win 0\.50/);
  assert.match(html, /stdev 12\.50/);
  assert.doesNotMatch(html, /\.pkl/);
  assert.doesNotMatch(html, /data-ml-bind=/);
});

test("fitted rows bind a catalog digest onto a native Candidate without a pickle path", () => {
  const digest = "a".repeat(64);
  const html = renderModelsPanel(parseModelsCatalog(catalog({
    models: [{
      family_id: "sklearn.tree.DecisionTreeClassifier",
      label: "Decision tree",
      historical_result_entity_id: "tc-research:historical-result:v1:33333333-3333-4333-8333-333333333333",
      historical_result_revision: "tc-research-revision:historical-result:sha256:" + "3".repeat(64),
      trade_count: 8,
      artifact_sha256: digest,
      train_accuracy: 1,
      oos_accuracy: 0.5,
      expected_value: { status: "available", expected_value: 5, p_win: 0.5, avg_win: 20, avg_loss: -10 },
      sharpe: { status: "available", sharpe: 0.4, mean_return: 5, stdev_return: 12.5, n: 8 },
    }],
  })), [], [{
    entity_id: "tc-research:candidate:v1:22222222-2222-4222-8222-222222222222",
    revision: "tc-research-revision:candidate:sha256:" + "2".repeat(64),
    archive_name: "Survivor.sqx",
  }]);
  assert.match(html, /data-ml-candidate/);
  assert.match(html, new RegExp(`data-ml-bind="${digest}"`));
  assert.match(html, /Bind to Candidate/);
  assert.match(html, /EV 5\.00/);
  assert.match(html, /Sharpe 0\.40/);
  assert.doesNotMatch(html, /\.pkl/);
  assert.doesNotMatch(html, /name="(?:path|sqx_home|model_path)"/);
});

test("already bound digest shows the Candidate archive and hides Bind", () => {
  const digest = "a".repeat(64);
  const html = renderModelsPanel(parseModelsCatalog(catalog({
    models: [{
      family_id: "sklearn.tree.DecisionTreeClassifier",
      label: "Decision tree",
      historical_result_entity_id: "tc-research:historical-result:v1:33333333-3333-4333-8333-333333333333",
      historical_result_revision: "tc-research-revision:historical-result:sha256:" + "3".repeat(64),
      trade_count: 8,
      artifact_sha256: digest,
      train_accuracy: 1,
      expected_value: { status: "available", expected_value: 5 },
      sharpe: { status: "available", sharpe: 0.4 },
    }],
  })), [], [{
    entity_id: "tc-research:candidate:v1:22222222-2222-4222-8222-222222222222",
    revision: "tc-research-revision:candidate:sha256:" + "2".repeat(64),
    archive_name: "Survivor.sqx",
    ml_model_artifact_sha256: digest,
  }]);
  assert.match(html, /bound Survivor\.sqx/);
  assert.doesNotMatch(html, /data-ml-bind=/);
});
