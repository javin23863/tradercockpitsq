import { writeFileSync } from "node:fs";

import {
  documentedSettingsTabs,
  isImproveExisting,
  renderFullSettings,
} from "../web/automation-full-settings.mjs";

const baseUrl = process.env.TRADERCOCKPIT_WALK_URL || "http://127.0.0.1:4320";
const results = [];

function record(name, pass, detail = "") {
  results.push({ name, pass, detail });
  console.log(`${pass ? "PASS" : "FAIL"}: ${name}${detail ? ` — ${detail}` : ""}`);
}

async function getJson(path) {
  const response = await fetch(`${baseUrl}${path}`, { headers: { accept: "application/json" } });
  const text = await response.text();
  let payload = null;
  try {
    payload = JSON.parse(text);
  } catch {
    payload = { raw: text.slice(0, 200) };
  }
  return { status: response.status, payload };
}

const moduleRes = await getJson("/api/sqx-module?module=builder");
record("/api/sqx-module", moduleRes.status === 200, `HTTP ${moduleRes.status}`);
if (moduleRes.status === 503) {
  const retry = await getJson("/api/sqx-module?module=builder");
  record("/api/sqx-module retry", retry.status === 200, `HTTP ${retry.status}`);
}

const files = await getJson("/api/sqx-build-type-files");
const fitness = await getJson("/api/sqx-ranking-fitness-types");
record("buildType/listFiles wrapper", files.status === 200 || files.status === 409, `HTTP ${files.status}`);
record("fitnessMethodStrategyResult/list wrapper", fitness.status === 200 || fitness.status === 409, `HTTP ${fitness.status}`);

const topologyRes = await getJson("/api/sqx-project-topology?project=Builder");
const topology = topologyRes.payload;
const task = topology?.tasks?.[0];
record("Builder topology", topologyRes.status === 200 && Boolean(task), `HTTP ${topologyRes.status} tasks=${topology?.tasks?.length ?? 0}`);

if (task) {
  const tabs = documentedSettingsTabs(task).map((tab) => tab.id);
  const what = renderFullSettings(task, "WhatToBuild", "Builder");
  const whatGroups = ["Strategy type", "Trading direction / symmetry", "Build mode"];
  const missingWhat = whatGroups.filter((name) => !what.includes(name));
  record("What to build groups", missingWhat.length === 0, missingWhat.join(", ") || "type/direction/build mode present");

  if (isImproveExisting(task) && tabs.includes("PartsToImprove")) {
    const parts = renderFullSettings(task, "PartsToImprove", "Builder");
    const hasActions = ["Add or replace", "Replace", "Add"].every((label) => parts.includes(label));
    const radioCount = (parts.match(/data-settings-attribute="action"/g) || []).length;
    record("Parts to improve action radios", radioCount >= 3 && hasActions, `radios=${radioCount}`);
  } else {
    record("Parts to improve action radios", true, "tab hidden; Builder StrategyType is not improve");
  }

  if (tabs.includes("GeneticOptions")) {
    const genetic = renderFullSettings(task, "GeneticOptions", "Builder");
    record("Genetic options cards", genetic.includes("data-settings-group="), "card groups present");
    record("Genetic options no Other settings dump", !genetic.includes('data-settings-group="Other settings"'));
  } else {
    record("Genetic options cards", false, "GeneticOptions tab missing");
    record("Genetic options no Other settings dump", false, "GeneticOptions tab missing");
  }

  const blocks = renderFullSettings(task, "Blocks", "Builder");
  const titleMatch = blocks.match(/class="settings-block-title">([^<]+)</);
  record("Blocks accordion title Signals", titleMatch?.[1] === "Signals", `title="${titleMatch?.[1] || ""}"`);
  record("Blocks list scroll markup", blocks.includes("settings-block-scroll-fill"), "scroll fill class present");

  const money = renderFullSettings(task, "RiskMoneyManagement", "Builder");
  const mmRadios = (money.match(/data-settings-exclusive-use/g) || []).length;
  record("Money management exclusive Type radios", mmRadios >= 2 && money.includes("data-settings-exclusive-group"), `radios=${mmRadios}`);

  const ranking = renderFullSettings(task, "Rankings", "Builder");
  record("Ranking two columns", ranking.includes("sqx-settings-grid-col-left") && ranking.includes("sqx-settings-grid-col-right"));
  record("Ranking condition table", ranking.includes("settings-condition-table"));
  record("Ranking automatic-filters gear → problem table", ranking.includes('data-settings-dialog="ranking-automatic-filters"') && ranking.includes("settings-problem-table"));
  record("Ranking no h4 Problem dumps", !ranking.includes("<h4>Problem"));
  record("Ranking Close without Save", ranking.includes("data-settings-dialog-close") && !ranking.includes("data-settings-dialog-save=\"ranking-automatic-filters\""));

  const cross = renderFullSettings(task, "CrossChecks", "Builder");
  record("Cross checks Settings gear", /data-settings-dialog-open="cross-[^"]+-settings"/.test(cross));
  record("Cross checks Filters gear", /data-settings-dialog-open="cross-[^"]+-filtering"/.test(cross));
  record("Cross checks Close without Save", cross.includes("data-settings-dialog-close"));
}

const catalog = await getJson("/api/sqx-projects");
const projectName = catalog.payload?.projects?.[0]?.name;
if (!projectName) {
  record("Custom projects catalog", false, "no projects");
} else {
  const custom = await getJson(`/api/sqx-project-topology?project=${encodeURIComponent(projectName)}`);
  const customTask = custom.payload?.tasks?.[0];
  const html = customTask ? renderFullSettings(customTask, "", projectName) : "";
  record(
    "Custom projects Full settings pipeline + Back",
    custom.status === 200 && Boolean(customTask) && html.includes("full-settings"),
    `project=${projectName} HTTP ${custom.status}`,
  );
}

const summary = { sqxModule503: moduleRes.status === 503, results };
console.log("\n--- SUMMARY ---");
console.log(JSON.stringify(summary, null, 2));
writeFileSync(new URL("./tc-live-walk-results.json", import.meta.url), JSON.stringify(summary, null, 2));
process.exitCode = results.some((row) => !row.pass) ? 1 : 0;
