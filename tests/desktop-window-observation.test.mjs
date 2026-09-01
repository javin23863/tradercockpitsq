import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import {
  DESKTOP_WINDOW_OBSERVATION_STATE_KEY,
  currentDesktopWindowObservation,
  installDesktopWindowObservation,
  isSettledDesktopWindowObservation,
  publishDesktopWindowObservation,
} from "../web/desktop-window-observation.mjs";

function researchDocument({ settled = true } = {}) {
  const shell = {
    getAttribute(name) {
      return {
        "data-product-shell": "tradercockpit-desktop",
        "data-surface-id": "research",
        "data-research-stage-id": "construct",
        "data-research-tab-id": "idea",
      }[name] || "";
    },
  };
  return {
    title: "TraderCockpit — Research / Construct / Idea",
    querySelector(selector) {
      if (selector === '[data-product-shell="tradercockpit-desktop"]') return shell;
      if (selector === ".content-inner h1") return { textContent: " Idea " };
      if (selector === "[data-research-idea-workspace]") return settled ? {} : null;
      if (selector === '[data-idea-action="save"]') return settled ? {} : null;
      return null;
    },
  };
}

function researchWindow(state = null) {
  const listeners = new Map();
  const windowLike = {
    location: { pathname: "/research", search: "" },
    pywebview: state ? { state } : undefined,
    addEventListener(name, handler) {
      listeners.set(name, handler);
    },
  };
  return { windowLike, listeners };
}

test("actual Research DOM snapshot contains canonical shell and Idea controls", () => {
  const observation = currentDesktopWindowObservation(
    researchDocument(),
    { pathname: "/research", search: "" },
  );
  assert.deepEqual(observation, {
    location_pathname: "/research",
    location_search: "",
    document_title: "TraderCockpit — Research / Construct / Idea",
    product_shell: "tradercockpit-desktop",
    surface_id: "research",
    research_stage_id: "construct",
    research_tab_id: "idea",
    page_heading: "Idea",
    idea_workspace: true,
    idea_save_action: true,
  });
  assert.equal(isSettledDesktopWindowObservation(observation), true);
});

test("publisher writes only to pywebview built-in shared state", () => {
  const state = {};
  const { windowLike } = researchWindow(state);
  const observation = publishDesktopWindowObservation(windowLike, researchDocument());
  assert.deepEqual(state[DESKTOP_WINDOW_OBSERVATION_STATE_KEY], observation);

  const ordinaryBrowser = researchWindow(null).windowLike;
  assert.equal(publishDesktopWindowObservation(ordinaryBrowser, researchDocument()), null);
});

test("observer waits for pywebviewready and keeps sampling until Research Idea settles", () => {
  const state = {};
  const { windowLike, listeners } = researchWindow(null);
  let settled = false;
  const documentLike = researchDocument({ settled: false });
  documentLike.querySelector = (selector) => {
    const base = researchDocument({ settled }).querySelector(selector);
    return base;
  };

  let interval = null;
  let cleared = false;
  installDesktopWindowObservation({
    windowLike,
    documentLike,
    setIntervalImpl(callback) {
      interval = callback;
      return 41;
    },
    clearIntervalImpl(id) {
      if (id === 41) cleared = true;
    },
  });

  assert.equal(state[DESKTOP_WINDOW_OBSERVATION_STATE_KEY], undefined);
  assert.equal(typeof listeners.get("pywebviewready"), "function");

  windowLike.pywebview = { state };
  listeners.get("pywebviewready")();
  assert.equal(state[DESKTOP_WINDOW_OBSERVATION_STATE_KEY].idea_workspace, false);
  assert.equal(typeof interval, "function");

  settled = true;
  interval();
  assert.equal(state[DESKTOP_WINDOW_OBSERVATION_STATE_KEY].idea_workspace, true);
  assert.equal(state[DESKTOP_WINDOW_OBSERVATION_STATE_KEY].idea_save_action, true);
  assert.equal(cleared, true);
});

test("canonical desktop loads shared-state observation module exactly once", async () => {
  const html = await readFile(new URL("../web/index.html", import.meta.url), "utf8");
  assert.equal((html.match(/desktop-window-observation\.mjs/g) || []).length, 1);
});
