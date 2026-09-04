import test from "node:test";
import assert from "node:assert/strict";

import { applyPromptToComposer, renderAssistantStart, renderAssistantWidget, resetAssistantHistory } from "../web/assistant.mjs";
import { renderSecondarySurface } from "../web/surfaces.mjs";

const readyRuntime = {
  schema: "tc.runtime-status.v1",
  assistant: { status: "ready", provider: "openrouter", model: "z-ai/glm-5.3-flash", knowledge: { catalog: "Quant-Guild", references: 27 }, tools: ["retrieve_quant_guild"], voice: { status: "ready", model: "openai/whisper-1" } },
  model: { status: "ready", provider: "openrouter", model: "z-ai/glm-5.3-flash" },
  provider: { status: "ready" },
  account: { status: "unavailable", reason_code: "authority_not_implemented" },
};

const nextAction = {
  schema: "tc.research-next-action.v1",
  current_stage: "idea",
  next_action: { id: "create_idea", label: "Create an Idea", path: "/research?workspace=signals&tab=overview" },
  locked_stages: ["specification", "build"],
  detail: "Text entry mints Idea custody only. It does not create a candidate or launch native compute.",
};

test("Apollo start panel shows the read-model next action, a matching prompt, and starter prompts", () => {
  resetAssistantHistory();
  const page = renderAssistantWidget(readyRuntime, { layout: "page", nextAction });
  assert.match(page, /data-assistant-start/);
  assert.match(page, /data-assistant-next-action="create_idea"/);
  assert.match(page, /href="\/research\?workspace=signals&amp;tab=overview"[^>]*>Create an Idea</);
  assert.match(page, /data-assistant-prompt="Draft a strategy Idea from this source/);
  assert.match(page, /Text entry mints Idea custody only/);
  assert.match(page, /data-assistant-prompt="What can you do on this desktop right now\?"/);
  assert.match(page, /never invents bars, trades, ratios, or live results/);
  assert.ok(page.indexOf("data-assistant-start") < page.indexOf("data-assistant-thread"));
  assert.doesNotMatch(page, /\$\s?\d|Sharpe 1\./, "no invented numbers in the empty state");
});

test("Apollo start panel degrades truthfully without a next action or provider", () => {
  const none = renderAssistantStart({ ready: true }, { detail: "Research custody is not connected.", next_action: null });
  assert.match(none, /data-assistant-next-action=""/);
  assert.match(none, /Research custody is not connected\./);
  assert.doesNotMatch(none, /is-next/);
  const unconfigured = renderAssistantStart({ ready: false, detail: "Set OPENROUTER_API_KEY in the operator environment." }, nextAction);
  assert.match(unconfigured, /Set OPENROUTER_API_KEY/);
  assert.match(unconfigured, /Prompts still send; the backend answers with its exact state/);
});

test("Apollo surface passes the shared next-action state into the page", () => {
  resetAssistantHistory();
  const html = renderSecondarySurface({ surfaceId: "apollo", label: "Apollo" }, { runtime: readyRuntime, nextAction });
  assert.match(html, /data-assistant-page/);
  assert.match(html, /data-assistant-next-action="create_idea"/);
});

test("Prompt chips fill the composer and select the placeholder without sending", () => {
  const field = {
    value: "",
    focused: false,
    selection: null,
    focus() { this.focused = true; },
    setSelectionRange(a, b) { this.selection = [a, b]; },
  };
  const widget = { querySelector: (selector) => (selector.includes("message") ? field : null) };
  const prompt = "Draft a strategy Idea from this source: <paste a URL, a paper excerpt, or describe the idea>";
  assert.equal(applyPromptToComposer(widget, prompt), true);
  assert.equal(field.value, prompt);
  assert.equal(field.focused, true);
  assert.deepEqual(field.selection, [prompt.indexOf("<"), prompt.length]);
  assert.equal(applyPromptToComposer({ querySelector: () => null }, "x"), false);
});
