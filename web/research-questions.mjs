const RESEARCH_CLARIFYING_QUESTIONS_API_PATH = "/api/research/clarifying-questions";
export const CLARIFYING_QUESTIONS_SCHEMA = "tc.research-clarifying-questions.v1";

export class ClarifyingQuestionApiError extends Error {
  constructor(message, { status = 0, payload = null } = {}) {
    super(message);
    this.name = "ClarifyingQuestionApiError";
    this.status = status;
    this.payload = payload;
  }
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

async function readJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

function ensureRecord(payload, detail) {
  if (!payload || payload.schema !== CLARIFYING_QUESTIONS_SCHEMA || !Array.isArray(payload.questions)) {
    throw new ClarifyingQuestionApiError(detail);
  }
  return payload;
}

export async function fetchClarifyingQuestions(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") {
    throw new ClarifyingQuestionApiError("Clarifying questions fetch is unavailable");
  }
  const response = await fetchImpl(RESEARCH_CLARIFYING_QUESTIONS_API_PATH, {
    headers: { accept: "application/json" },
  });
  const payload = await readJson(response);
  if (!response?.ok) {
    throw new ClarifyingQuestionApiError(
      payload?.detail || `Clarifying questions request failed: ${response?.status ?? "unknown"}`,
      { status: response?.status ?? 0, payload },
    );
  }
  return ensureRecord(payload, "Clarifying questions schema mismatch");
}

export async function answerClarifyingQuestion({ fieldId, answerId, entityId = "" } = {}, fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") {
    throw new ClarifyingQuestionApiError("Clarifying answer write is unavailable");
  }
  const body = { field_id: fieldId, answer_id: answerId };
  if (entityId) body.entity_id = entityId;
  const response = await fetchImpl(RESEARCH_CLARIFYING_QUESTIONS_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify(body),
  });
  const payload = await readJson(response);
  if (!response?.ok) {
    throw new ClarifyingQuestionApiError(
      payload?.detail || `Clarifying answer failed: ${response?.status ?? "unknown"}`,
      { status: response?.status ?? 0, payload },
    );
  }
  return ensureRecord(payload, "Clarifying questions schema mismatch");
}

function answerButtons(question, { disabled = false } = {}) {
  const answers = Array.isArray(question?.allowed_answers) ? question.allowed_answers : [];
  if (!answers.length) return "";
  return `<div class="clarifying-answers">${answers.map((item) => (
    `<button type="button" class="button button-secondary" data-clarifying-answer="${escapeHtml(item.id)}" data-clarifying-field="${escapeHtml(question.id)}" ${disabled ? "disabled" : ""}><span>${escapeHtml(item.label)}</span></button>`
  )).join("")}</div>`;
}

export function renderCurrentQuestion(question, { compact = false } = {}) {
  if (!question || typeof question !== "object") return "";
  const status = question.status || "open";
  const blocked = status === "blocked";
  const prompt = question.prompt || question.label || "Unresolved Specification field";
  const note = blocked
    ? (question.reason_code === "watchlist_empty"
      ? "No watchlist is configured, so this field cannot be answered without inventing a symbol."
      : "This field is blocked until its producer or allowed answers exist. Build stays locked.")
    : "Typed answers only. Invented values are refused.";
  return `<div class="clarifying-current" data-clarifying-current="${escapeHtml(question.id)}" data-clarifying-status="${escapeHtml(status)}">
    <p class="clarifying-prompt"><strong>${escapeHtml(compact ? "Apollo needs this next" : "Current question")}</strong> ${escapeHtml(prompt)}</p>
    ${answerButtons(question, { disabled: blocked })}
    <p class="field-help">${escapeHtml(note)}</p>
  </div>`;
}

export function renderClarifyingQuestions(record) {
  if (!record || record.schema !== CLARIFYING_QUESTIONS_SCHEMA) {
    throw new ClarifyingQuestionApiError("Clarifying questions schema mismatch");
  }
  const gate = record.build_gate && typeof record.build_gate === "object" ? record.build_gate : { locked: true, reason_codes: ["invalid_or_missing_build_gate"] };
  const locked = gate.locked !== false;
  const reasons = Array.isArray(gate.reason_codes) ? gate.reason_codes : [];
  if (record.reason_code === "idea_required") {
    return `<section data-clarifying-questions data-clarifying-reason="idea_required"><div class="requirement-item specification-gate"><strong>Clarifying questions</strong><span class="status-badge status-unavailable"><span class="status-dot"></span>Idea required</span><p>Create or ingest an Idea first. Questions bind only to unresolved Specification fields.</p></div></section>`;
  }
  const questions = Array.isArray(record.questions) ? record.questions : [];
  const current = renderCurrentQuestion(record.current_question);
  const rows = questions.map((item) => {
    const state = item.status || "open";
    const tone = state === "resolved" ? "ready" : "unavailable";
    const answer = item.answer?.label || item.reason_code || "Unanswered";
    return `<div class="requirement-item" data-clarifying-field="${escapeHtml(item.id)}" data-clarifying-status="${escapeHtml(state)}"><div><strong>${escapeHtml(item.label)}</strong><span class="field-help">${item.required ? "Required" : "Conditional"}</span></div><span class="status-badge status-${tone}"><span class="status-dot"></span>${escapeHtml(state.replaceAll("_", " "))}</span><p>${escapeHtml(item.prompt || "")}</p><p class="field-help">${escapeHtml(item.source ? `${item.source}: ${answer}` : answer)}</p></div>`;
  }).join("");
  return `<section data-clarifying-questions data-clarifying-open="${escapeHtml(String(record.open_count ?? 0))}">
    <div class="requirement-item specification-gate"><strong>Clarifying questions</strong><span class="status-badge status-${locked ? "unavailable" : "ready"}"><span class="status-dot"></span>${escapeHtml(locked ? "Build locked" : "Required questions resolved")}</span><p>${escapeHtml(reasons.join(" · ") || "No unresolved Specification questions.")}</p></div>
    ${current}${rows}
  </section>`;
}

function fillHosts(record) {
  const html = renderClarifyingQuestions(record);
  for (const host of [...document.querySelectorAll("[data-clarifying-questions]")]) {
    host.outerHTML = html;
  }
  const currentHtml = renderCurrentQuestion(record.current_question, { compact: true });
  for (const host of document.querySelectorAll("[data-assistant-question]")) {
    host.innerHTML = currentHtml;
    host.hidden = !currentHtml;
  }
}

async function refreshQuestions(fetchImpl = globalThis.fetch) {
  const record = await fetchClarifyingQuestions(fetchImpl);
  fillHosts(record);
  return record;
}

async function submitAnswer(button) {
  const fieldId = button.getAttribute("data-clarifying-field");
  const answerId = button.getAttribute("data-clarifying-answer");
  if (!fieldId || !answerId) return;
  for (const item of document.querySelectorAll("[data-clarifying-answer]")) item.disabled = true;
  try {
    const record = await answerClarifyingQuestion({ fieldId, answerId });
    fillHosts(record);
    globalThis.window?.dispatchEvent(new CustomEvent("tradercockpit:custody-changed", { detail: { source: "clarifying-questions" } }));
  } catch (error) {
    const detail = error instanceof Error ? error.message : "Clarifying answer failed";
    for (const host of document.querySelectorAll("[data-assistant-question]")) {
      host.hidden = false;
      host.innerHTML = `<p class="clarifying-prompt" data-clarifying-error>${escapeHtml(detail)}</p>${host.innerHTML}`;
    }
    for (const item of document.querySelectorAll("[data-clarifying-answer]")) item.disabled = false;
  }
}

let clicksBound = false;
function bindClicks() {
  if (typeof document === "undefined" || clicksBound) return;
  clicksBound = true;
  document.addEventListener("click", (event) => {
    const button = event.target.closest?.("[data-clarifying-answer]");
    if (!button || button.disabled) return;
    event.preventDefault();
    void submitAnswer(button);
  });
}

export function bindClarifyingQuestions() {
  if (typeof document === "undefined") return;
  bindClicks();
  void refreshQuestions().catch(() => {});
}

if (typeof document !== "undefined") {
  bindClicks();
}
