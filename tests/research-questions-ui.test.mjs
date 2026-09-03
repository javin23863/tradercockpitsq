import assert from "node:assert/strict";
import test from "node:test";

import {
  CLARIFYING_QUESTIONS_SCHEMA,
  ClarifyingQuestionApiError,
  answerClarifyingQuestion,
  fetchClarifyingQuestions,
  renderClarifyingQuestions,
  renderCurrentQuestion,
} from "../web/research-questions.mjs";
import { renderResearchSpecification } from "../web/research-specification.mjs";
import { renderAssistantWidget } from "../web/assistant.mjs";
import { RESEARCH_SPECIFICATION_SCHEMA } from "../web/research-specification.mjs";

const questions = Object.freeze({
  schema: CLARIFYING_QUESTIONS_SCHEMA,
  idea_entity_id: "tc-research:idea:v1:12345678-1234-5678-1234-567812345678",
  idea_revision: "rev-1",
  object_kind: "strategy",
  open_count: 1,
  blocked_count: 0,
  current_question: {
    id: "market_identity",
    label: "Market identity",
    prompt: "Which configured watchlist symbol should this plan use?",
    status: "open",
    required: true,
    allowed_answers: [
      { id: "ESM5", label: "ESM5" },
      { id: "NQ", label: "NQ" },
    ],
  },
  questions: [
    {
      id: "object_kind",
      label: "Object kind",
      prompt: "Is this Idea an indicator, a strategy, or a model?",
      status: "resolved",
      required: true,
      source: "user_selected",
      answer: { id: "strategy", label: "Strategy" },
      allowed_answers: [],
    },
    {
      id: "market_identity",
      label: "Market identity",
      prompt: "Which configured watchlist symbol should this plan use?",
      status: "open",
      required: true,
      allowed_answers: [
        { id: "ESM5", label: "ESM5" },
        { id: "NQ", label: "NQ" },
      ],
    },
  ],
  build_gate: {
    locked: true,
    reason_codes: ["unresolved:market_identity"],
    next_authority: "answer_clarifying_questions",
  },
  reason_code: "unresolved_specification_fields",
  detail: "Typed answers only.",
});

test("clarifying-question client posts only allowed field/answer ids", async () => {
  const calls = [];
  const fetchImpl = async (path, options = {}) => {
    calls.push([path, options]);
    if (options.method === "POST") {
      return { ok: true, status: 200, json: async () => questions };
    }
    return { ok: true, status: 200, json: async () => questions };
  };
  const loaded = await fetchClarifyingQuestions(fetchImpl);
  assert.equal(loaded.schema, CLARIFYING_QUESTIONS_SCHEMA);
  assert.equal(calls[0][0], "/api/research/clarifying-questions");
  await answerClarifyingQuestion({ fieldId: "market_identity", answerId: "NQ" }, fetchImpl);
  assert.equal(calls[1][1].method, "POST");
  assert.deepEqual(JSON.parse(calls[1][1].body), { field_id: "market_identity", answer_id: "NQ" });
});

test("clarifying-question client preserves backend refusal of invented answers", async () => {
  await assert.rejects(
    () => answerClarifyingQuestion({ fieldId: "market_identity", answerId: "ES" }, async () => ({
      ok: false,
      status: 400,
      json: async () => ({ error: "invalid_request", reason_code: "answer_not_allowed", detail: "answer is not in the allowed set for this field" }),
    })),
    (error) => error instanceof ClarifyingQuestionApiError && error.payload?.reason_code === "answer_not_allowed",
  );
});

test("Specification render keeps Build locked and shows typed answer buttons", () => {
  const html = renderClarifyingQuestions(questions);
  assert.match(html, /data-clarifying-questions/);
  assert.match(html, /data-clarifying-current="market_identity"/);
  assert.match(html, /data-clarifying-answer="ESM5"/);
  assert.match(html, /data-clarifying-answer="NQ"/);
  assert.match(html, /Build locked/);
  assert.doesNotMatch(html, /data-clarifying-answer="ES"/);

  const spec = renderResearchSpecification(
    {
      schema: RESEARCH_SPECIFICATION_SCHEMA,
      build_gate: { locked: true, reason_codes: ["unresolved:strategy_shape"] },
      requirements: [{ id: "strategy_shape", label: "Strategy shape", state: "unresolved", required: true, detail: "Missing", evidence: { native_source_path: "task" }, values: {} }],
    },
    null,
    questions,
  );
  assert.match(spec, /data-clarifying-questions/);
  assert.match(spec, /unresolved:market_identity/);
  assert.match(spec, /unresolved:strategy_shape/);
});

test("Apollo widget hosts the current clarifying question", () => {
  const html = renderAssistantWidget(null);
  assert.match(html, /data-assistant-question/);
  const chip = renderCurrentQuestion(questions.current_question, { compact: true });
  assert.match(chip, /Apollo needs this next/);
  assert.match(chip, /data-clarifying-answer="NQ"/);
});

test("Idea-required questions do not flip a resolved native Build gate to locked", () => {
  const html = renderResearchSpecification(
    {
      schema: RESEARCH_SPECIFICATION_SCHEMA,
      build_gate: { locked: false, reason_codes: [] },
      requirements: [{
        id: "source_provenance",
        label: "Source provenance",
        state: "producer_configured",
        required: true,
        detail: "Exact native archive identity is preserved.",
        evidence: { native_source_path: "user/projects/Builder/project.cfx" },
        values: {},
      }],
    },
    null,
    {
      schema: CLARIFYING_QUESTIONS_SCHEMA,
      idea_entity_id: null,
      questions: [],
      current_question: null,
      open_count: 0,
      blocked_count: 0,
      reason_code: "idea_required",
      build_gate: { locked: true, reason_codes: ["idea_required"], next_authority: "create_idea" },
    },
  );
  assert.match(html, /Build requirements resolved/);
  assert.match(html, /Idea required/);
  assert.doesNotMatch(html, /Build locked/);
});
