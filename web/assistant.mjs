// Assistant (Apollo) — functional chat widget over the backend `/api/assistant` transport.
// The browser never sees provider credentials or chooses models; it posts the message plus a
// bounded in-session history and renders the typed reply or the backend's exact error. The
// widget is never disabled: readiness is described truthfully from `/api/status`, and sending
// while the provider is unconfigured surfaces the backend's `provider_not_configured` state.

import { escapeHtml, icon, readable } from "./ui.mjs";

const ASSISTANT_API_PATH = "/api/assistant";
const HISTORY_LIMIT = 12;

// Conversation lives for the page session so navigation between surfaces keeps the thread.
const conversation = [];
let pending = false;

export function assistantState(runtime) {
  const assistant = runtime?.assistant || null;
  const provider = runtime?.provider || null;
  const model = runtime?.model || null;
  const ready = (assistant?.status || provider?.status) === "ready";
  const modelName = assistant?.model || model?.default_model || null;
  return {
    ready,
    checking: !runtime,
    modelName,
    providerName: assistant?.provider || provider?.provider || "openrouter",
    modelLabel: runtime
      ? (ready ? `${modelName} via ${assistant?.provider || provider?.provider || "OpenRouter"}` : `${modelName ? `${modelName} · ` : ""}${readable(provider?.reason_code || assistant?.reason_code, "provider not configured")}`)
      : "Checking…",
    accountLabel: runtime?.provider?.credential_scope === "consumer"
      ? (runtime.provider.spend_boundary?.provider_enforced ? "Consumer · provider enforced" : "Consumer")
      : runtime?.account
        ? (runtime.account.status === "ready" ? "Ready" : `${readable(runtime.account.reason_code, "Unavailable")} · operator credential`)
        : "Checking…",
    detail: assistant?.detail || provider?.detail || "",
    knowledgeLabel: assistant?.knowledge
      ? (assistant.knowledge.status === "ready"
        ? `Quant-Guild · ${assistant.knowledge.document_count} excerpts`
        : readable(assistant.knowledge.reason_code, "knowledge library not connected"))
      : "Checking…",
  };
}

export function assistantHistory() {
  return conversation.slice();
}

export function resetAssistantHistory() {
  conversation.length = 0;
}

function messageHtml(entry) {
  const tone = entry.role === "user" ? "is-user" : entry.error ? "is-error" : "is-assistant";
  const meta = entry.role === "assistant" && entry.model ? `<small>${escapeHtml(entry.model)}${entry.fallback ? " · fallback" : ""}</small>` : "";
  return `<div class="assistant-msg ${tone}" data-assistant-role="${escapeHtml(entry.role)}"${entry.error ? ' data-assistant-error' : ""}><p>${escapeHtml(entry.content)}</p>${meta}</div>`;
}

export function renderAssistantThread(entries = conversation) {
  if (!entries.length) return "";
  return entries.map(messageHtml).join("");
}

export function renderAssistantWidget(runtime, { compact = false, placeholder = "Ask Apollo about your research, runtime or verdicts…" } = {}) {
  const state = assistantState(runtime);
  const greeting = state.checking
    ? "Connecting to the assistant backend…"
    : state.ready
      ? "Good day, Trader."
      : "Assistant transport is not configured on this desktop.";
  const detail = state.checking
    ? ""
    : state.ready
      ? `Ask about custody, the native runtime, or the cockpit verdict. Model policy: ${state.modelName} via ${state.providerName}.`
      : `${state.detail || "Set OPENROUTER_API_KEY in the operator environment."} You can still send; the backend answers with its exact state.`;
  const intro = compact
    ? `<div class="assistant-text"><strong>${escapeHtml(greeting)}</strong>${detail ? `<span>${escapeHtml(detail)}</span>` : ""}</div>`
    : `<div class="assistant-bubble"><span class="assistant-avatar">${icon("bot", { size: 15 })}</span><div class="assistant-text"><strong>${escapeHtml(greeting)}</strong>${detail ? `<span>${escapeHtml(detail)}</span>` : ""}<ul><li>Model access: ${escapeHtml(state.modelLabel)}</li><li>Consumer account: ${escapeHtml(state.accountLabel)}</li><li>Knowledge library: ${escapeHtml(state.knowledgeLabel)}</li></ul></div></div>`;
  return `<div class="assistant-widget ${compact ? "is-compact" : ""}" data-assistant-widget data-assistant-ready="${state.ready ? "true" : "false"}">
    ${intro}
    <div class="assistant-thread" data-assistant-thread aria-live="polite">${renderAssistantThread()}</div>
    <form class="assistant-form" data-assistant-form autocomplete="off">
      <input type="text" name="message" maxlength="4000" placeholder="${escapeHtml(placeholder)}" aria-label="Message the assistant" required>
      <button type="submit" class="button button-primary" data-assistant-ask>${icon("spark", { size: 13 })}<span>Ask</span></button>
    </form>
  </div>`;
}

async function readJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

export async function sendAssistantMessage(message, fetchImpl = globalThis.fetch) {
  const history = conversation.filter((entry) => !entry.error).slice(-HISTORY_LIMIT).map(({ role, content }) => ({ role, content }));
  conversation.push({ role: "user", content: message });
  const response = await fetchImpl(ASSISTANT_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({ message, history }),
  });
  const payload = await readJson(response);
  if (!response?.ok || !payload?.reply) {
    const detail = payload?.detail || `Assistant request failed (${response?.status ?? "no response"})`;
    conversation.push({ role: "assistant", content: `${readable(payload?.reason_code, "Assistant unavailable")}: ${detail}`, error: true, reasonCode: payload?.reason_code || null });
    return { ok: false, payload };
  }
  conversation.push({ role: "assistant", content: payload.reply, model: payload.model, fallback: payload.fallback_used === true });
  return { ok: true, payload };
}

function refreshThreads() {
  for (const thread of document.querySelectorAll("[data-assistant-thread]")) {
    thread.innerHTML = renderAssistantThread();
    thread.scrollTop = thread.scrollHeight;
  }
  for (const form of document.querySelectorAll("[data-assistant-form]")) {
    const button = form.querySelector("[data-assistant-ask]");
    if (button) button.classList.toggle("is-busy", pending);
    form.setAttribute("data-assistant-pending", pending ? "true" : "false");
  }
}

async function submit(form) {
  const input = form.querySelector('input[name="message"]');
  const message = input?.value.trim();
  if (!message || pending) return;
  pending = true;
  input.value = "";
  refreshThreads();
  try {
    await sendAssistantMessage(message);
  } catch (error) {
    conversation.push({ role: "assistant", content: `Assistant unreachable: ${error instanceof Error ? error.message : "network error"}`, error: true, reasonCode: "network_error" });
  } finally {
    pending = false;
    refreshThreads();
    input?.focus();
  }
}

function bindForms() {
  for (const form of document.querySelectorAll("[data-assistant-form]:not([data-assistant-bound])")) {
    form.setAttribute("data-assistant-bound", "true");
    form.addEventListener("submit", (event) => {
      event.preventDefault();
      void submit(form);
    });
    const thread = form.parentElement?.querySelector("[data-assistant-thread]");
    if (thread) {
      thread.innerHTML = renderAssistantThread();
      thread.scrollTop = thread.scrollHeight;
    }
  }
}

if (typeof document !== "undefined") {
  const observer = new MutationObserver(bindForms);
  observer.observe(document.documentElement, { childList: true, subtree: true });
  bindForms();
}
