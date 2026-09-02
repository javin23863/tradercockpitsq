import assert from "node:assert/strict";
import test from "node:test";

import { parseModelsCatalog, renderModelsPanel } from "../web/research-models.mjs";

function catalog(overrides = {}) {
  return {
    schema: "tc.research-ml-model-catalog.v1",
    backend: "sklearn",
    backend_available: true,
    reason_code: null,
    feature_names: ["Duration", "MAE", "MFE", "PipsPL"],
    label_rule: "producer_pl_positive",
    families: [{ family_id: "sklearn.tree.DecisionTreeClassifier", label: "Decision tree" }],
    models: [],
    detail: "Allowlisted sklearn classifiers fit on native SQX trade records.",
    ...overrides,
  };
}

test("models parser requires sklearn family ids and refuses a path write shape", () => {
  const parsed = parseModelsCatalog(catalog());
  assert.equal(parsed.families[0].family_id, "sklearn.tree.DecisionTreeClassifier");
  assert.throws(() => parseModelsCatalog({ ...catalog(), schema: "nope" }), /schema mismatch/);
  assert.throws(
    () => parseModelsCatalog(catalog({ families: [{ family_id: "C:/models/tree.pkl", label: "x" }] })),
    /family identity/,
  );
});

test("models panel fits by family_id and Historical Result identity only", () => {
  const html = renderModelsPanel(parseModelsCatalog(catalog()), [
    { entity_id: "tc-research:historical-result:v1:33333333-3333-4333-8333-333333333333", revision: "tc-research-revision:historical-result:sha256:" + "3".repeat(64), state: "completed", execution_completed: true },
  ]);
  assert.match(html, /data-ml-fit="sklearn\.tree\.DecisionTreeClassifier"/);
  assert.doesNotMatch(html, /<input[^>]+type="file"/);
  assert.doesNotMatch(html, /name="(?:path|sqx_home|model_path)"/);
});

test("models panel binds a catalog artifact to an imported Candidate", () => {
  const artifact = "a".repeat(64);
  const html = renderModelsPanel(
    parseModelsCatalog(catalog({
      models: [{
        family_id: "sklearn.tree.DecisionTreeClassifier",
        label: "Decision tree",
        historical_result_entity_id: "tc-research:historical-result:v1:33333333-3333-4333-8333-333333333333",
        historical_result_revision: "tc-research-revision:historical-result:sha256:" + "3".repeat(64),
        trade_count: 4,
        artifact_sha256: artifact,
        train_accuracy: 0.5,
      }],
    })),
    [],
    [{
      entity_id: "tc-research:candidate:v1:22222222-2222-4222-8222-222222222222",
      revision: "tc-research-revision:candidate:sha256:" + "f".repeat(64),
      archive_name: "Survivor.sqx",
      ml_model_artifact_sha256: null,
    }],
  );
  assert.match(html, /data-ml-bind="/);
  assert.match(html, /data-ml-candidate/);
  assert.doesNotMatch(html, /\.pkl/);
});
