import test from "node:test";
import assert from "node:assert/strict";

import {
  createProof,
  proofCatalogFromPayload,
  proofFromPayload,
  proofSelections,
} from "../web/research-proof.mjs";

const ev = (digit) => `tc-evidence:sha256:${digit.repeat(64)}`;
const rev = (kind, digit) => `tc-research-revision:${kind}:sha256:${digit.repeat(64)}`;
const entity = (kind, digit) => `tc-research:${kind}:v1:${digit.repeat(8)}-${digit.repeat(4)}-4${digit.repeat(3)}-8${digit.repeat(3)}-${digit.repeat(12)}`;

function historical() {
  const project = "TraderCockpit-Retester-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
  return {
    schema: "tc.research-historical-result.v1",
    entity_id: entity("historical-result", "4"),
    revision: rev("historical-result", "4"),
    state: "completed",
    execution_completed: true,
    candidate_entity_id: entity("candidate", "3"),
    candidate_revision: rev("candidate", "3"),
    candidate_archive_name: "candidate.sqx",
    candidate_archive_ref: ev("d"),
    candidate_archive_sha256: "d".repeat(64),
    sqx_build: "144.2953",
    operation: "native_retester_task_1",
    retester_task: 1,
    native_project_name: project,
    native_project_relative_path: `user/projects/${project}/project.cfx`,
    source_project_ref: ev("a"),
    source_project_sha256: "a".repeat(64),
    engine_ref: ev("f"),
    engine_sha256: "f".repeat(64),
    launcher_sha256: "1".repeat(64),
    result_archive_name: "result.sqx",
    result_archive_relative_path: `user/projects/${project}/databanks/Results/result.sqx`,
    result_archive_ref: ev("e"),
    result_archive_sha256: "e".repeat(64),
    result_strategy_ref: ev("7"),
    result_strategy_sha256: "7".repeat(64),
    result_settings_ref: ev("9"),
    result_settings_sha256: "9".repeat(64),
    receipts: [{ state: "completed" }],
    partial_side_effect: true,
    validation_state: "not_run",
  };
}

function validation(source = historical()) {
  const project = "TraderCockpit-Retester-bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
  return {
    schema: "tc.research-native-robustness.v1",
    validation_ref: ev("6"),
    proof_entity_id: entity("proof", "5"),
    proof_revision: rev("proof", "5"),
    sqx_build: "144.2953",
    operation: "native_retester_cross_check",
    method: "RetestWithHigherPrecision",
    execution_state: "completed",
    producer_outcome_state: "producer_result_captured_outcome_unread",
    source_historical_result_entity_id: source.entity_id,
    source_historical_result_revision: source.revision,
    source_result_archive_ref: source.result_archive_ref,
    source_result_archive_sha256: source.result_archive_sha256,
    source_project_ref: ev("2"),
    source_project_sha256: "2".repeat(64),
    compiled_project_ref: ev("3"),
    compiled_project_sha256: "3".repeat(64),
    source_task_sha256: "4".repeat(64),
    compiled_task_sha256: "5".repeat(64),
    engine_ref: source.engine_ref,
    engine_sha256: source.engine_sha256,
    launcher_sha256: "1".repeat(64),
    native_project_name: project,
    native_project_relative_path: `user/projects/${project}/project.cfx`,
    result_archive_name: "higher.sqx",
    result_archive_ref: ev("8"),
    result_archive_sha256: "8".repeat(64),
    result_strategy_ref: ev("a"),
    result_strategy_sha256: "a".repeat(64),
    result_settings_ref: ev("b"),
    result_settings_sha256: "b".repeat(64),
    native_settings: { Precision: "1 Minute", Spread: "Current" },
    configuration_changed: false,
    receipts: [{
      action: "startOnlyTask",
      task: 1,
      state: "completed",
      project,
      project_sha256: "3".repeat(64),
      engine_sha256: source.engine_sha256,
      launcher_sha256: "1".repeat(64),
      result_archive_sha256: source.result_archive_sha256,
    }],
  };
}

function proofPayload() {
  const h = historical();
  const v = validation(h);
  const configEntity = entity("configuration", "1");
  const configRevision = rev("configuration", "1");
  const jobEntity = entity("native-job", "2");
  const jobRevision = rev("native-job", "2");
  const candidateEntity = h.candidate_entity_id;
  const candidateRevision = h.candidate_revision;
  return {
    schema: "tc.research-proof.v1",
    entity_id: entity("proof", "7"),
    revision: rev("proof", "7"),
    content_ref: ev("c"),
    association_mode: "operator_selected_exact_idea_revision",
    sqx_build: "144.2953",
    idea: {
      schema: "tc.research-idea.v1",
      entity_id: entity("idea", "0"),
      revision: rev("idea", "0"),
      content_ref: ev("0"),
      text: "Idea",
      source: "operator",
    },
    configuration: {
      schema: "tc.research-configuration.v1",
      entity_id: configEntity,
      revision: configRevision,
      state: "approved",
      source_project_ref: ev("a"),
      source_project_sha256: "a".repeat(64),
      executable_xml_ref: ev("b"),
      executable_xml_sha256: "b".repeat(64),
    },
    native_job: {
      schema: "tc.research-native-job.v1",
      entity_id: jobEntity,
      revision: jobRevision,
      state: "submitted",
      configuration_entity_id: configEntity,
      configuration_revision: configRevision,
      launcher_sha256: "c".repeat(64),
    },
    candidate: {
      schema: "tc.research-candidate.v1",
      entity_id: candidateEntity,
      revision: candidateRevision,
      native_job_entity_id: jobEntity,
      native_job_revision: jobRevision,
      configuration_entity_id: configEntity,
      configuration_revision: configRevision,
      archive_ref: ev("d"),
      archive_sha256: "d".repeat(64),
    },
    historical_result: h,
    trades: {
      schema: "tc.research-historical-trades.v1",
      historical_result_entity_id: h.entity_id,
      historical_result_revision: h.revision,
      candidate_entity_id: candidateEntity,
      candidate_revision: candidateRevision,
      result_archive_ref: h.result_archive_ref,
      result_archive_sha256: h.result_archive_sha256,
      rows: [],
    },
    validation: v,
    truth: {
      validation_execution_completed: true,
      producer_validation_outcome: "producer_result_captured_outcome_unread",
      producer_verdict_available: false,
    },
  };
}

test("Proof parser accepts one exact bound chain and keeps verdict unread", () => {
  const proof = proofFromPayload(proofPayload());
  assert.equal(proof.truth.producer_verdict_available, false);
  assert.equal(proof.validation.producer_outcome_state, "producer_result_captured_outcome_unread");
});

test("Proof parser rejects validation rebound to another Historical Result", () => {
  const payload = proofPayload();
  payload.validation.source_historical_result_revision = rev("historical-result", "9");
  assert.throws(() => proofFromPayload(payload), /chain is inconsistent/);
});

test("Proof catalog requires exact immutable identities", () => {
  const payload = proofPayload();
  const catalog = proofCatalogFromPayload({
    schema: "tc.research-proof-catalog.v1",
    proofs: [{
      entity_id: payload.entity_id,
      revision: payload.revision,
      idea_entity_id: payload.idea.entity_id,
      idea_revision: payload.idea.revision,
      historical_result_entity_id: payload.historical_result.entity_id,
      historical_result_revision: payload.historical_result.revision,
      validation_ref: payload.validation.validation_ref,
      producer_validation_outcome: "producer_result_captured_outcome_unread",
    }],
  });
  assert.equal(catalog.length, 1);
});

test("Proof selection only offers completed Historical Results and their matching robustness runs", () => {
  const h = historical();
  const incomplete = { ...h, entity_id: entity("historical-result", "8"), revision: rev("historical-result", "8"), state: "prepared", execution_completed: false };
  const selected = proofSelections(
    [{ entity_id: entity("idea", "0"), revision: rev("idea", "0") }],
    [incomplete, h],
    [validation(h)],
    0,
  );
  assert.equal(selected.completed.length, 1);
  assert.equal(selected.historical.revision, h.revision);
  assert.equal(selected.validations.length, 1);
});

test("Proof creation posts only exact source identities", async () => {
  const h = historical();
  const v = validation(h);
  const idea = { entity_id: entity("idea", "0"), revision: rev("idea", "0") };
  let request = null;
  const responsePayload = proofPayload();
  const result = await createProof({ idea, historical: h, validation: v }, async (path, options) => {
    request = { path, options };
    return { ok: true, status: 201, json: async () => responsePayload };
  });
  assert.equal(request.path, "/api/research/proofs");
  assert.deepEqual(JSON.parse(request.options.body), {
    action: "create-proof",
    idea_entity_id: idea.entity_id,
    idea_revision: idea.revision,
    historical_result_entity_id: h.entity_id,
    historical_result_revision: h.revision,
    validation_ref: v.validation_ref,
  });
  assert.equal(result.entity_id, responsePayload.entity_id);
});
