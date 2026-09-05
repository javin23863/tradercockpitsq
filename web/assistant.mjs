// Assistant (Apollo) — functional chat widget over the backend `/api/assistant` transport.
// The browser never sees provider credentials or chooses models; it posts the message plus a
// bounded in-session history and renders the typed reply or the backend's exact error. The
// widget is never disabled: readiness is described truthfully from `/api/status`, and sending
// while the provider is unconfigured surfaces the backend's `provider_not_configured` state.

import { escapeHtml, icon, readable } from "./ui.mjs";
import { APP_SURFACES, RESEARCH_WORKSPACES } from "./model.mjs";
import { bindClarifyingQuestions } from "./research-questions.mjs";

const ASSISTANT_API_PATH = "/api/assistant";
const ASSISTANT_TRANSCRIBE_API_PATH = "/api/assistant/transcribe";
const HISTORY_LIMIT = 12;
const MAX_VOICE_MS = 30000;
const ALLOWED_CONFIRM_PATHS = Object.freeze(new Set([
  "/api/research/ideas",
  "/api/research/clarifying-questions",
  "/api/research/configurations",
  "/api/research/native-jobs",
]));
const ALLOWED_CONFIRM_METHODS = Object.freeze(new Set(["POST"]));

// Conversation lives for the page session so navigation between surfaces keeps the thread.
const conversation = [];
let pending = false;
let voicePending = false;
let voiceRecorder = null;
let voiceTimer = 0;
let voiceChunks = [];
let voiceStream = null;

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
    accountLabel: runtime?.account
      ? (runtime.account.status === "ready" ? "Ready" : `${readable(runtime.account.reason_code, "Unavailable")} · operator credential`)
      : "Checking…",
    knowledgeLabel: assistant?.knowledge
      ? (assistant.knowledge.status === "ready"
        ? `Quant-Guild · ${assistant.knowledge.entry_count} references`
        : readable(assistant.knowledge.reason_code, "knowledge library not connected"))
      : "Checking…",
    toolsLabel: Array.isArray(assistant?.tools?.approved) && assistant.tools.approved.length
      ? `${assistant.tools.approved.join(", ")} · confirm mutations · backend only`
      : (assistant?.tools?.detail || "No approved tools"),
    voiceLabel: assistant?.voice
      ? (assistant.voice.status === "ready"
        ? `${assistant.voice.stt_model || "speech-to-text"} · desktop microphone`
        : readable(assistant.voice.reason_code, "voice unavailable"))
      : "Checking…",
    voiceReady: assistant?.voice?.status === "ready",
    detail: assistant?.detail || provider?.detail || "",
  };
}

export function assistantHistory() {
  return conversation.slice();
}

export function resetAssistantHistory() {
  conversation.length = 0;
}

function citationHtml(citations) {
  if (!Array.isArray(citations) || !citations.length) return "";
  const items = citations
    .filter((item) => item && typeof item.title === "string")
    .map((item) => {
      const title = escapeHtml(item.title);
      const href = typeof item.source_url === "string" && item.source_url ? escapeHtml(item.source_url) : "";
      const label = href ? `<a href="${href}" rel="noreferrer" target="_blank">${title}</a>` : title;
      return `<li>${label}</li>`;
    })
    .join("");
  return items ? `<ul class="assistant-citations" data-assistant-citations>${items}</ul>` : "";
}

function actionHtml(action) {
  if (!action || typeof action.id !== "string") return "";
  const state = action.state || "pending";
  const label = escapeHtml(action.label || action.tool || "Proposed action");
  if (state === "confirmed" || state === "applied") {
    return `<div class="assistant-action" data-assistant-action="${escapeHtml(action.id)}" data-assistant-action-state="${escapeHtml(state)}"><p>${label}</p><span class="field-help">${state === "applied" ? "Opened" : "Confirmed"}</span></div>`;
  }
  if (state === "dismissed" || state === "failed") {
    const detail = action.detail ? ` · ${escapeHtml(action.detail)}` : "";
    return `<div class="assistant-action" data-assistant-action="${escapeHtml(action.id)}" data-assistant-action-state="${escapeHtml(state)}"><p>${label}${detail}</p></div>`;
  }
  if (action.confirmation_required === false) {
    return `<div class="assistant-action" data-assistant-action="${escapeHtml(action.id)}" data-assistant-action-state="applied"><p>${label}</p><span class="field-help">Opened</span></div>`;
  }
  return `<div class="assistant-action" data-assistant-action="${escapeHtml(action.id)}" data-assistant-action-state="pending">
    <p>${label}</p>
    <button type="button" class="button button-primary button-small" data-assistant-action-confirm="${escapeHtml(action.id)}">Confirm</button>
    <button type="button" class="button button-small" data-assistant-action-dismiss="${escapeHtml(action.id)}">Dismiss</button>
  </div>`;
}

function proposedActionsHtml(actions) {
  if (!Array.isArray(actions) || !actions.length) return "";
  return `<div class="assistant-actions" data-assistant-actions>${actions.map(actionHtml).join("")}</div>`;
}

function messageHtml(entry) {
  const tone = entry.role === "user" ? "is-user" : entry.error ? "is-error" : "is-assistant";
  const meta = entry.role === "assistant" && entry.model ? `<small>${escapeHtml(entry.model)}${entry.fallback ? " · fallback" : ""}</small>` : "";
  const citations = entry.role === "assistant" ? citationHtml(entry.citations) : "";
  const actions = entry.role === "assistant" ? proposedActionsHtml(entry.proposedActions) : "";
  const knowledgeState = entry.role === "assistant" && entry.knowledgeState ? ` data-assistant-knowledge-state="${escapeHtml(entry.knowledgeState)}"` : "";
  const toolsUsed = entry.role === "assistant" && Array.isArray(entry.toolsUsed) && entry.toolsUsed.length
    ? ` data-assistant-tools-used="${escapeHtml(entry.toolsUsed.map((item) => item.name).filter(Boolean).join(" "))}"`
    : "";
  const transcriptAttr = entry.role === "user" && entry.source === "voice" ? ' data-assistant-transcript="true"' : "";
  const transcriptMeta = entry.role === "user" && entry.source === "voice" ? "<small>Transcript</small>" : "";
  return `<div class="assistant-msg ${tone}" data-assistant-role="${escapeHtml(entry.role)}"${entry.error ? ' data-assistant-error' : ""}${knowledgeState}${toolsUsed}${transcriptAttr}><p>${escapeHtml(entry.content)}</p>${meta}${transcriptMeta}${citations}${actions}</div>`;
}

export function renderAssistantThread(entries = conversation) {
  if (!entries.length) return "";
  return entries.map(messageHtml).join("");
}

function assistantStatusItems(state) {
  return `<ul>
    <li>Model access: ${escapeHtml(state.modelLabel)}</li>
    <li>Consumer account: ${escapeHtml(state.accountLabel)}</li>
    <li data-assistant-knowledge>Knowledge library: ${escapeHtml(state.knowledgeLabel)}</li>
    <li data-assistant-tools>Approved tools: ${escapeHtml(state.toolsLabel)}</li>
    <li data-assistant-voice-status>Voice: ${escapeHtml(state.voiceLabel)}</li>
  </ul>`;
}

export function renderAssistantWidget(runtime, { compact = false, layout = "card", placeholder = "Ask Apollo about your research, runtime or verdicts…" } = {}) {
  const state = assistantState(runtime);
  const page = layout === "page";
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
  if (page) {
    return `<div class="assistant-widget is-page" data-assistant-widget data-assistant-layout="page" data-assistant-ready="${state.ready ? "true" : "false"}" data-assistant-voice-state="${state.voiceReady ? "ready" : "unavailable"}">
      <header class="assistant-page-head">
        <div class="assistant-page-title"><span class="assistant-avatar">${icon("bot", { size: 15 })}</span><div class="assistant-text"><strong>Apollo</strong><span>${escapeHtml(greeting)}</span></div></div>
        <p class="note">${detail ? escapeHtml(detail) : escapeHtml(state.modelLabel)}</p>
        <details class="assistant-page-status" data-assistant-intro>
          <summary>Readiness, tools, and voice</summary>
          ${assistantStatusItems(state)}
        </details>
      </header>
      <div class="assistant-question" data-assistant-question hidden></div>
      <section class="assistant-welcome" aria-label="Start a conversation">
        <h1>Turn a research question into a next step.</h1>
        <p>Apollo can explain your native setup and recorded results. Choose a starting point, edit the message, then ask.</p>
        <div class="assistant-starters">${[
          ["Check my setup", "Explain the current native runtime and data readiness. What is missing before I can build a strategy?"],
          ["Understand my results", "Help me interpret my recorded strategy results. Explain in-sample versus out-of-sample performance and ask which strategy I want to review."],
          ["Shape a strategy idea", "Help me turn a trading idea into a clear specification. Ask about my source, market, timeframe, entry rules and risk constraints before proposing changes."],
        ].map(([label, prompt]) => `<button type="button" data-assistant-prompt="${escapeHtml(prompt)}"><strong>${escapeHtml(label)}</strong><span>${escapeHtml(prompt)}</span>${icon("chevron", { size: 16 })}</button>`).join("")}</div>
      </section>
      <div class="assistant-thread" data-assistant-thread aria-live="polite">${renderAssistantThread()}</div>
      <form class="assistant-form" data-assistant-form autocomplete="off">
        <textarea name="message" rows="3" maxlength="4000" placeholder="${escapeHtml(placeholder)}" aria-label="Message the assistant" required></textarea>
        <div class="assistant-form-actions">
          <button type="button" class="button" data-assistant-voice aria-label="Speak to Apollo">${icon("mic", { size: 13 })}<span>Speak</span></button>
          <button type="submit" class="button button-primary" data-assistant-ask>${icon("spark", { size: 13 })}<span>Ask</span></button>
        </div>
      </form>
    </div>`;
  }
  const intro = compact
    ? `<div class="assistant-text"><strong>${escapeHtml(greeting)}</strong>${detail ? `<span>${escapeHtml(detail)}</span>` : ""}</div>`
      : `<div class="assistant-bubble"><span class="assistant-avatar">${icon("bot", { size: 15 })}</span><div class="assistant-text"><strong>${escapeHtml(greeting)}</strong>${detail ? `<span>${escapeHtml(detail)}</span>` : ""}${assistantStatusItems(state)}</div></div>`;
  return `<div class="assistant-widget ${compact ? "is-compact" : ""}" data-assistant-widget data-assistant-ready="${state.ready ? "true" : "false"}" data-assistant-voice-state="${state.voiceReady ? "ready" : "unavailable"}">
    ${intro}
    <div class="assistant-question" data-assistant-question hidden></div>
    <div class="assistant-thread" data-assistant-thread aria-live="polite">${renderAssistantThread()}</div>
    <form class="assistant-form" data-assistant-form autocomplete="off">
      <input type="text" name="message" maxlength="4000" placeholder="${escapeHtml(placeholder)}" aria-label="Message the assistant" required>
      <button type="button" class="button" data-assistant-voice aria-label="Speak to Apollo">${icon("mic", { size: 13 })}<span>Speak</span></button>
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

export async function sendAssistantMessage(message, fetchImpl = globalThis.fetch, options = {}) {
  const source = options.source === "voice" ? "voice" : "typed";
  const history = conversation.filter((entry) => !entry.error).slice(-HISTORY_LIMIT).map(({ role, content }) => ({ role, content }));
  conversation.push({ role: "user", content: message, source });
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
  conversation.push({
    role: "assistant",
    content: payload.reply,
    model: payload.model,
    fallback: payload.fallback_used === true,
    citations: Array.isArray(payload.knowledge?.citations) ? payload.knowledge.citations : [],
    knowledgeState: payload.knowledge?.state || null,
    toolsUsed: Array.isArray(payload.tools_used) ? payload.tools_used : [],
    proposedActions: Array.isArray(payload.proposed_actions) ? payload.proposed_actions.map((item) => ({ ...item, state: item?.confirmation_required === false ? "applied" : "pending" })) : [],
  });
  applyImmediateNavigation(payload.proposed_actions);
  return { ok: true, payload };
}

export function captureSupported(mediaDevices = globalThis.navigator?.mediaDevices, recorder = globalThis.MediaRecorder) {
  return Boolean(mediaDevices && typeof mediaDevices.getUserMedia === "function" && typeof recorder === "function");
}

export async function transcribeAssistantAudio(blob, fetchImpl = globalThis.fetch) {
  const type = typeof blob?.type === "string" && blob.type ? blob.type : "audio/webm";
  const response = await fetchImpl(ASSISTANT_TRANSCRIBE_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": type },
    body: blob,
  });
  const payload = await readJson(response);
  if (!response?.ok || typeof payload?.transcript !== "string" || !payload.transcript.trim()) {
    const error = new Error(payload?.detail || `Speech-to-text failed (${response?.status ?? "no response"})`);
    error.reasonCode = payload?.reason_code || "stt_unavailable";
    error.payload = payload;
    throw error;
  }
  return payload;
}

function findProposedAction(actionId) {
  for (const entry of conversation) {
    for (const action of entry.proposedActions || []) {
      if (action && action.id === actionId) return action;
    }
  }
  return null;
}

function setProposedActionState(actionId, state, detail = "") {
  const action = findProposedAction(actionId);
  if (!action) return;
  action.state = state;
  action.detail = detail;
  refreshThreads();
}

export function isAllowedNavigatePath(path) {
  if (typeof path !== "string" || !path.startsWith("/") || /[\\%]/.test(path) || path.includes("..")) return false;
  const [pathname, query = ""] = path.split("?");
  if (query.includes("&") && query.split("&").some((part) => !part)) return false;
  const surface = APP_SURFACES.find((item) => item.path === pathname);
  if (surface && pathname !== "/research") return query === "";
  if (pathname !== "/research") return false;
  const params = new URLSearchParams(query);
  const keys = [...params.keys()];
  if (keys.some((key) => key !== "workspace" && key !== "tab")) return false;
  const workspaceId = params.get("workspace") || "signals";
  const workspace = RESEARCH_WORKSPACES.find((item) => item.id === workspaceId);
  if (!workspace) return false;
  if (!workspace.tabs.length) return !params.has("tab");
  const tabId = params.get("tab") || workspace.tabs[0].id;
  return workspace.tabs.some((tab) => tab.id === tabId);
}

function applyImmediateNavigation(actions) {
  if (typeof window === "undefined" || !Array.isArray(actions)) return;
  for (const action of actions) {
    if (!action || action.confirmation_required !== false || action.tool !== "navigate_surface") continue;
    if (typeof action.path !== "string" || !isAllowedNavigatePath(action.path)) continue;
    window.dispatchEvent(new CustomEvent("tradercockpit:navigate", { detail: { path: action.path } }));
  }
}

export async function executeProposedAction(action, fetchImpl = globalThis.fetch) {
  if (!action || action.native_mutation === true) {
    throw new Error("that proposed action is not approved");
  }
  if (action.confirmation_required === false) {
    if (action.tool !== "navigate_surface" || !isAllowedNavigatePath(action.path)) {
      throw new Error("navigate_surface accepts only canonical product paths");
    }
    if (typeof window !== "undefined") {
      window.dispatchEvent(new CustomEvent("tradercockpit:navigate", { detail: { path: action.path } }));
    }
    return { ok: true, navigated: action.path };
  }
  if (!ALLOWED_CONFIRM_METHODS.has(action.method) || !ALLOWED_CONFIRM_PATHS.has(action.path)) {
    throw new Error("that proposed action path is not approved");
  }
  if (!action.body || typeof action.body !== "object" || Array.isArray(action.body)) {
    throw new Error("that proposed action body is not approved");
  }
  const response = await fetchImpl(action.path, {
    method: action.method,
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify(action.body),
  });
  let payload = null;
  try {
    payload = await response.json();
  } catch {
    payload = null;
  }
  if (!response?.ok) {
    throw new Error(payload?.detail || payload?.reason_code || `Action failed (${response?.status ?? "no response"})`);
  }
  if (typeof window !== "undefined") {
    window.dispatchEvent(new CustomEvent("tradercockpit:custody-changed", { detail: { source: action.tool } }));
  }
  return { ok: true, payload };
}

async function confirmAction(actionId) {
  const action = findProposedAction(actionId);
  if (!action || action.state === "confirmed" || action.state === "dismissed") return;
  try {
    await executeProposedAction(action);
    setProposedActionState(actionId, "confirmed");
  } catch (error) {
    setProposedActionState(actionId, "failed", error instanceof Error ? error.message : "action failed");
  }
}

function dismissAction(actionId) {
  const action = findProposedAction(actionId);
  if (!action || action.state === "confirmed") return;
  setProposedActionState(actionId, "dismissed");
}

function refreshThreads() {
  if (typeof document === "undefined") return;
  for (const thread of document.querySelectorAll("[data-assistant-thread]")) {
    thread.innerHTML = renderAssistantThread();
    thread.scrollTop = thread.scrollHeight;
  }
  for (const form of document.querySelectorAll("[data-assistant-form]")) {
    const ask = form.querySelector("[data-assistant-ask]");
    if (ask) ask.classList.toggle("is-busy", pending);
    const voice = form.querySelector("[data-assistant-voice]");
    if (voice) {
      voice.classList.toggle("is-busy", pending);
      voice.classList.toggle("is-recording", Boolean(voiceRecorder));
      const label = voice.querySelector("span");
      if (label) label.textContent = voiceRecorder ? "Stop" : "Speak";
    }
    form.setAttribute("data-assistant-pending", pending ? "true" : "false");
    form.setAttribute("data-assistant-voice-recording", voiceRecorder ? "true" : "false");
  }
}

async function submit(form) {
  const field = form.querySelector('textarea[name="message"], input[name="message"]');
  const message = field?.value.trim();
  if (!message || pending || voiceRecorder) return;
  pending = true;
  field.value = "";
  refreshThreads();
  try {
    await sendAssistantMessage(message);
  } catch (error) {
    conversation.push({ role: "assistant", content: `Assistant unreachable: ${error instanceof Error ? error.message : "network error"}`, error: true, reasonCode: "network_error" });
  } finally {
    pending = false;
    refreshThreads();
    field?.focus();
  }
}

function stopTracks(stream) {
  for (const track of stream?.getTracks?.() || []) track.stop();
}

function waitForRecorderStop(recorder) {
  if (!recorder || recorder.state === "inactive") return Promise.resolve();
  return new Promise((resolve, reject) => {
    recorder.addEventListener("error", () => reject(new Error("microphone capture failed")), { once: true });
    recorder.addEventListener("stop", () => resolve(), { once: true });
    recorder.stop();
  });
}

async function stopVoiceAndSend() {
  const recorder = voiceRecorder;
  const stream = voiceStream;
  if (!recorder) return;
  voiceRecorder = null;
  voiceStream = null;
  if (voiceTimer) {
    globalThis.clearTimeout(voiceTimer);
    voiceTimer = 0;
  }
  refreshThreads();
  try {
    await waitForRecorderStop(recorder);
  } catch (error) {
    stopTracks(stream);
    voiceChunks = [];
    conversation.push({ role: "assistant", content: `Capture unavailable: ${error instanceof Error ? error.message : "microphone capture failed"}`, error: true, reasonCode: "capture_unavailable" });
    refreshThreads();
    return;
  }
  stopTracks(stream);
  const blob = new Blob(voiceChunks, { type: recorder.mimeType || "audio/webm" });
  voiceChunks = [];
  if (!blob.size) {
    conversation.push({ role: "assistant", content: "Capture unavailable: no audio was recorded.", error: true, reasonCode: "capture_unavailable" });
    refreshThreads();
    return;
  }
  pending = true;
  voicePending = true;
  refreshThreads();
  try {
    const payload = await transcribeAssistantAudio(blob);
    await sendAssistantMessage(payload.transcript, globalThis.fetch, { source: "voice" });
  } catch (error) {
    conversation.push({
      role: "assistant",
      content: `${readable(error.reasonCode, "Voice unavailable")}: ${error instanceof Error ? error.message : "speech-to-text failed"}`,
      error: true,
      reasonCode: error.reasonCode || "stt_unavailable",
    });
  } finally {
    pending = false;
    voicePending = false;
    refreshThreads();
  }
}

export async function startVoiceCapture() {
  if (pending || voicePending) return;
  if (voiceRecorder) {
    await stopVoiceAndSend();
    return;
  }
  if (!captureSupported()) {
    conversation.push({ role: "assistant", content: "Capture unavailable: this desktop has no microphone capture.", error: true, reasonCode: "capture_unavailable" });
    refreshThreads();
    return;
  }
  let stream;
  try {
    stream = await globalThis.navigator.mediaDevices.getUserMedia({ audio: true, video: false });
  } catch {
    conversation.push({ role: "assistant", content: "Capture unavailable: microphone permission was denied or no microphone is connected.", error: true, reasonCode: "capture_unavailable" });
    refreshThreads();
    return;
  }
  const mime = (globalThis.MediaRecorder.isTypeSupported?.("audio/webm;codecs=opus") && "audio/webm;codecs=opus")
    || (globalThis.MediaRecorder.isTypeSupported?.("audio/webm") && "audio/webm")
    || "";
  const recorder = mime ? new globalThis.MediaRecorder(stream, { mimeType: mime }) : new globalThis.MediaRecorder(stream);
  voiceChunks = [];
  voiceStream = stream;
  recorder.addEventListener("dataavailable", (event) => {
    if (event.data && event.data.size) voiceChunks.push(event.data);
  });
  try {
    recorder.start();
  } catch {
    stopTracks(stream);
    voiceChunks = [];
    voiceStream = null;
    conversation.push({ role: "assistant", content: "Capture unavailable: this desktop could not start microphone recording.", error: true, reasonCode: "capture_unavailable" });
    refreshThreads();
    return;
  }
  voiceRecorder = recorder;
  voiceTimer = globalThis.setTimeout(() => { void stopVoiceAndSend(); }, MAX_VOICE_MS);
  refreshThreads();
}

function bindForms() {
  let boundNew = false;
  for (const form of document.querySelectorAll("[data-assistant-form]:not([data-assistant-bound])")) {
    boundNew = true;
    form.setAttribute("data-assistant-bound", "true");
    form.addEventListener("submit", (event) => {
      event.preventDefault();
      void submit(form);
    });
    const composer = form.querySelector('textarea[name="message"]');
    if (composer) {
      composer.addEventListener("keydown", (event) => {
        if ((event.ctrlKey || event.metaKey) && event.key === "Enter") {
          event.preventDefault();
          void submit(form);
        }
      });
    }
    const thread = form.parentElement?.querySelector("[data-assistant-thread]");
    if (thread) {
      thread.innerHTML = renderAssistantThread();
      thread.scrollTop = thread.scrollHeight;
    }
  }
  if (boundNew) bindClarifyingQuestions();
}

function bindActionClicks() {
  if (typeof document === "undefined" || document.documentElement.dataset.assistantActionsBound === "true") return;
  document.documentElement.dataset.assistantActionsBound = "true";
  document.addEventListener("click", (event) => {
    const starter = event.target.closest?.("[data-assistant-prompt]");
    if (starter) {
      const input = starter.closest("[data-assistant-widget]")?.querySelector("[name=message]");
      if (input) {
        input.value = starter.getAttribute("data-assistant-prompt") || "";
        input.dispatchEvent(new Event("input", { bubbles: true }));
        input.focus();
      }
      return;
    }
    const confirm = event.target.closest?.("[data-assistant-action-confirm]");
    if (confirm) {
      event.preventDefault();
      void confirmAction(confirm.getAttribute("data-assistant-action-confirm") || "");
      return;
    }
    const dismiss = event.target.closest?.("[data-assistant-action-dismiss]");
    if (dismiss) {
      event.preventDefault();
      dismissAction(dismiss.getAttribute("data-assistant-action-dismiss") || "");
    }
  });
}

function bindVoiceClicks() {
  if (typeof document === "undefined" || document.documentElement.dataset.assistantVoiceBound === "true") return;
  document.documentElement.dataset.assistantVoiceBound = "true";
  document.addEventListener("click", (event) => {
    const button = event.target.closest?.("[data-assistant-voice]");
    if (!button) return;
    event.preventDefault();
    void startVoiceCapture();
  });
}

if (typeof document !== "undefined") {
  const observer = new MutationObserver(bindForms);
  observer.observe(document.documentElement, { childList: true, subtree: true });
  bindForms();
  bindActionClicks();
  bindVoiceClicks();
}
