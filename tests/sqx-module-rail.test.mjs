import assert from "node:assert/strict";
import test from "node:test";

import { APP_SURFACES, resolveRoute } from "../web/model.mjs";
import { CUSTOM_PROJECTS_PATH, RUN_MODULE_PATHS, workflowHref } from "../web/automation-settings-controls.mjs";
import { renderWorkflowDetail } from "../web/automation-workflows.mjs";
import { sqxModuleFromPayload } from "../web/sqx-modules.mjs";
import { renderSecondarySurface } from "../web/surfaces.mjs";

function topology() {
  return {
    schema: "tc.sqx-custom-project-topology.v1",
    source_build: "144.2953",
    project: "Builder",
    source_relative_path: "user/projects/Builder/project.cfx",
    archive_sha256: "a".repeat(64),
    internal_entries: ["config.xml", "Build-Task1.xml"],
    tasks: [
      {
        native_task_index: 1,
        kind: "Build",
        entry_name: "Build-Task1.xml",
        name: "Build strategies",
        active: true,
        clear_databanks: [],
        goto_target_label: null,
        settings: [],
      },
    ],
    native_setup: null,
    execution: { supported: false, reason: "topology_custody_only" },
  };
}

test("rail labels match official SQX modules and drop Explore / Research pipeline names", () => {
  assert.deepEqual(APP_SURFACES.map((surface) => surface.label), [
    "Getting started",
    "Builder",
    "Data manager",
    "Custom projects",
    "Apollo",
    "Operate",
    "Settings",
  ]);
  assert.equal(APP_SURFACES.some((surface) => surface.id === "explore"), false);
  assert.equal(APP_SURFACES.some((surface) => surface.id === "research"), false);
  assert.equal(APP_SURFACES.some((surface) => surface.id === "automation"), false);
});

test("legacy Explore / Automation / bare Research URLs do not invent product pages", () => {
  assert.equal(resolveRoute("/explore").redirectPath, "/home");
  assert.equal(resolveRoute("/research").redirectPath, "/builder");
  assert.equal(resolveRoute("/retester").redirectPath, "/builder");
  assert.equal(resolveRoute("/optimizer").redirectPath, "/builder");
  assert.equal(resolveRoute("/automation", "?project=RetainedBuildTask").redirectPath, "/custom-projects?project=RetainedBuildTask");
  assert.equal(resolveRoute("/algowizard").redirectPath, "/apollo");
  assert.equal(resolveRoute("/builder").surfaceId, "builder");
  assert.equal(resolveRoute("/custom-projects").surfaceId, "custom-projects");
  assert.equal(resolveRoute("/apollo").surfaceId, "apollo");
});

test("workflow hrefs bind the current module path instead of Automation", () => {
  assert.equal(workflowHref({ path: "/builder", tab: "settings", task: 1 }), "/builder?tab=settings&task=1");
  assert.equal(workflowHref({ path: CUSTOM_PROJECTS_PATH, project: "RetainedBuildTask" }), "/custom-projects?project=RetainedBuildTask");
  assert.deepEqual(RUN_MODULE_PATHS["/builder"], "Builder");
});

test("Builder module shell hides the Custom Project catalog crumb", () => {
  const html = renderWorkflowDetail(topology(), { available: false, reason_code: "native_custom_project_launch_unwired", detail: "Launch unwired" }, null, {
    tab: "settings",
    task: 1,
    module: "Builder",
  });
  assert.match(html, /data-sqx-module-mode="run"/);
  assert.match(html, /Native module archive/);
  assert.doesNotMatch(html, /All workflows/);
  assert.match(html, /Full settings/);
});

test("module payload parser refuses invented ready inspect editors", () => {
  assert.throws(
    () => sqxModuleFromPayload({
      schema: "tc.sqx-run-module.v1",
      source_build: "144.2953",
      module: "AlgoWizard",
      kind: "inspect",
      status: "ready",
      reason_code: null,
      detail: "editor",
      project: "AlgoWizard",
      source_relative_path: "user/projects/AlgoWizard/project.cfx",
      archive_sha256: "a".repeat(64),
      task_count: 1,
      databank_count: 0,
      strategy_count: 0,
      editor_wired: true,
      control: { available: false },
    }),
    /invalid/,
  );
});

test("Data manager surface reads installed producer data and does not invent a downloader", () => {
  const html = renderSecondarySurface({ surfaceId: "data-manager", label: "Data manager" }, {});
  assert.match(html, /data-data-manager-host/);
  assert.match(html, /data-sqx-module="Data manager"/);
  assert.match(html, /read-only; adding or importing series stays in StrategyQuant X/);
  assert.doesNotMatch(html, /drag-drop|Download data|Connect feed|<input/i);
});

test("Apollo rail is the full-page assistant, not an AlgoWizard editor", () => {
  const html = renderSecondarySurface({ surfaceId: "apollo", label: "Apollo" }, { runtime: null });
  assert.match(html, /data-assistant-page/);
  assert.match(html, /data-assistant-layout="page"/);
  assert.match(html, /<textarea[^>]*name="message"/);
  assert.match(html, /data-assistant-form/);
  assert.match(html, /data-assistant-ask/);
  assert.match(html, /data-assistant-voice/);
  assert.doesNotMatch(html, /data-sqx-inspect-host/);
  assert.doesNotMatch(html, /drag-drop|block editor|Download data/i);
});
