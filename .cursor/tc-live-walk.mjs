import { chromium } from "playwright";

const baseUrl = process.env.TRADERCOCKPIT_WALK_URL || "http://127.0.0.1:4320";
const results = [];
let sqxModule503 = false;

function record(name, pass, detail = "") {
  results.push({ name, pass, detail });
  console.log(`${pass ? "PASS" : "FAIL"}: ${name}${detail ? ` — ${detail}` : ""}`);
}

async function checkSqxModule() {
  let status = 0;
  for (let attempt = 0; attempt < 2; attempt += 1) {
    const res = await fetch(`${baseUrl}/api/sqx-module?module=builder`);
    status = res.status;
    if (status !== 503) break;
    sqxModule503 = true;
    await new Promise((r) => setTimeout(r, 500));
  }
  record("/api/sqx-module", status === 200, `HTTP ${status}`);
  return status === 200;
}

async function waitLoaded(page, timeout = 40000) {
  await page.locator('[data-automation-workflows="loaded"]').waitFor({ timeout });
}

async function hardRefresh(page, url) {
  const bust = url.includes("?") ? `${url}&_walk=${Date.now()}` : `${url}?_walk=${Date.now()}`;
  await page.goto(bust, { waitUntil: "commit", timeout: 60000 });
}

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage();
page.setDefaultTimeout(60000);
page.on("console", (msg) => {
  if (msg.type() === "error") console.error("PAGE ERROR:", msg.text());
});
page.on("requestfailed", (req) => console.error("REQ FAIL:", req.url(), req.failure()?.errorText));

try {
  if (!(await checkSqxModule())) {
    throw new Error("sqx-module not ready");
  }

  const url = `${baseUrl}/builder?tab=settings&task=1`;
  const t0 = Date.now();
  await hardRefresh(page, url);
  const reading = page.locator("text=Reading project.cfx");
  try {
    await reading.waitFor({ state: "visible", timeout: 500 });
  } catch {
    // may already be past placeholder
  }
  await waitLoaded(page, 40000);
  const elapsed = Date.now() - t0;
  const stillReading = await reading.isVisible().catch(() => false);
  record("Leave Reading project.cfx ≤2s", !stillReading && elapsed < 8000, `${elapsed}ms, visible=${stillReading}`);

  // 1 What to build
  await page.locator('[data-automation-section="WhatToBuild"]').click();
  await page.locator('[data-settings-tag="WhatToBuild"]').waitFor({ timeout: 20000 });
  const whatHtml = await page.locator('[data-settings-tag="WhatToBuild"]').innerHTML();
  const whatGroups = ["Strategy type", "Trading direction / symmetry", "Build mode"];
  const missingWhat = whatGroups.filter((g) => !whatHtml.includes(`data-settings-group="${g}"`) && !whatHtml.includes(g));
  record("What to build groups", missingWhat.length === 0, missingWhat.join(", ") || "type/direction/build mode present");

  // 2 Parts to improve
  const partsTab = page.locator('[data-automation-section="PartsToImprove"]');
  if (await partsTab.count()) {
    await partsTab.click();
    await page.locator('[data-settings-tag="PartsToImprove"]').waitFor({ timeout: 20000 });
    const actionRadios = page.locator('[data-settings-tag="PartsToImprove"] input[type="radio"][data-settings-attribute="action"]');
    const radioCount = await actionRadios.count();
    const labels = await page.locator('[data-settings-tag="PartsToImprove"] .settings-radio-choice label').allTextContents();
    const hasActions = ["Add or replace", "Replace", "Add"].every((l) => labels.some((t) => t.includes(l)));
    record("Parts to improve action radios", radioCount >= 3 && hasActions, `radios=${radioCount}, labels=${labels.join("|")}`);
  } else {
    record("Parts to improve action radios", false, "PartsToImprove tab not shown (Builder type=simple hides tab per isImproveExisting)");
  }

  // 3 Genetic options
  await page.locator('[data-automation-section="GeneticOptions"]').click();
  await page.locator("[data-genetic-options]").waitFor({ timeout: 20000 });
  const geneticHtml = await page.locator("[data-genetic-options]").innerHTML();
  const hasCards = geneticHtml.includes("data-settings-group=");
  const hasOtherDump = geneticHtml.includes('data-settings-group="Other settings"');
  record("Genetic options cards", hasCards, hasOtherDump ? "unexpected Other settings group" : "card groups present");
  record("Genetic options no Other settings dump", !hasOtherDump);

  // 4 Blocks
  await page.locator('[data-automation-section="Blocks"]').click();
  await page.locator('[data-settings-tag="Blocks"]').waitFor({ timeout: 20000 });
  const signalsAccordion = page.locator('.settings-block-accordion[data-settings-group="Signals"]');
  const signalsTitle = page.locator('.settings-block-accordion summary .settings-block-title').first();
  const titleText = await signalsTitle.textContent().catch(() => "");
  const scroll = page.locator(".settings-block-scroll-fill").first();
  const scrollable = await scroll.evaluate((el) => el.scrollHeight > el.clientHeight);
  record("Blocks accordion title Signals", titleText.trim() === "Signals", `title="${titleText?.trim()}"`);
  record("Blocks list scrolls", scrollable, `scrollHeight>${await scroll.evaluate((el) => el.clientHeight)}`);

  // 5 Money management — verify exclusive radios, do NOT click/persist
  await page.locator('[data-automation-section="RiskMoneyManagement"]').click();
  await page.locator('[data-settings-tag="RiskMoneyManagement"]').waitFor({ timeout: 20000 });
  const mmGroup = page.locator("[data-settings-exclusive-group]").first();
  await mmGroup.waitFor({ timeout: 20000 });
  const mmRadios = page.locator("[data-settings-exclusive-group] input[data-settings-exclusive-use]");
  const mmCount = await mmRadios.count();
  const checked = await mmRadios.evaluateAll((nodes) => nodes.filter((n) => n.checked).length);
  record("Money management exclusive Type radios", mmCount >= 2 && checked === 1, `radios=${mmCount}, checked=${checked}`);

  // 6 Ranking
  await page.locator('[data-automation-section="Rankings"]').click();
  await page.locator('[data-settings-tag="Rankings"]').waitFor({ timeout: 20000 });
  const ranking = page.locator('[data-settings-tag="Rankings"]');
  const twoCols = await ranking.locator(".sqx-settings-grid-col-left, .sqx-settings-grid-col-right").count();
  const condTable = await ranking.locator("table.settings-condition-table, .settings-condition-table").count();
  record("Ranking two columns", twoCols >= 2, `cols=${twoCols}`);
  record("Ranking condition table", condTable >= 1, `tables=${condTable}`);

  const autoGear = page.locator('[data-settings-dialog-open="ranking-automatic-filters"]');
  if (await autoGear.count()) {
    await autoGear.click();
    const dialog = page.locator('[data-settings-dialog="ranking-automatic-filters"]');
    await dialog.waitFor({ state: "visible", timeout: 10000 });
    const problemTable = dialog.locator("table.settings-problem-table");
    const hasTable = await problemTable.count() > 0;
    const headers = hasTable ? await problemTable.locator("thead th").allTextContents() : [];
    const hasUseCodeDismiss = ["use", "code", "dismiss"].every((h) => headers.some((t) => t.toLowerCase().includes(h)));
    const h4Problems = await dialog.locator('h4:has-text("Problem")').count();
    record("Ranking automatic-filters gear → problem table", hasTable && hasUseCodeDismiss, `headers=${headers.join("/")}`);
    record("Ranking no h4 Problem dumps", h4Problems === 0, `h4 count=${h4Problems}`);
    await dialog.locator("[data-settings-dialog-close]").click();
    await dialog.waitFor({ state: "hidden", timeout: 5000 });
    record("Ranking Close without Save", !(await dialog.isVisible()));
  } else {
    record("Ranking automatic-filters gear → problem table", false, "gear not found");
    record("Ranking no h4 Problem dumps", false, "dialog not opened");
    record("Ranking Close without Save", false, "dialog not opened");
  }

  // 7 Cross checks
  await page.locator('[data-automation-section="CrossChecks"]').click();
  await page.locator('[data-settings-tag="CrossChecks"]').waitFor({ timeout: 20000 });
  const settingsGear = page.locator('[data-settings-dialog-open^="cross-"][data-settings-dialog-open$="-settings"]').first();
  const filterGear = page.locator('[data-settings-dialog-open^="cross-"][data-settings-dialog-open$="-filtering"]').first();
  if (await settingsGear.count()) {
    await settingsGear.click();
    const sDialog = page.locator('[data-settings-dialog]').filter({ has: page.locator(":visible") }).first();
    await sDialog.waitFor({ state: "visible", timeout: 10000 });
    await sDialog.locator("[data-settings-dialog-close]").click();
    await sDialog.waitFor({ state: "hidden", timeout: 5000 });
    record("Cross checks Settings gear Close", true);
  } else {
    record("Cross checks Settings gear Close", false, "no settings gear");
  }
  if (await filterGear.count()) {
    await filterGear.click();
    const fDialog = page.locator('[data-settings-dialog]').filter({ has: page.locator(":visible") }).first();
    await fDialog.waitFor({ state: "visible", timeout: 10000 });
    await fDialog.locator("[data-settings-dialog-close]").click();
    await fDialog.waitFor({ state: "hidden", timeout: 5000 });
    record("Cross checks Filters gear Close", true);
  } else {
    record("Cross checks Filters gear Close", false, "no filters gear");
  }

  // 8 Custom projects
  const projectsRes = await fetch(`${baseUrl}/api/sqx-projects`);
  const catalog = await projectsRes.json();
  const projectName = catalog.projects?.[0]?.name;
  if (!projectName) {
    record("Custom projects Full settings pipeline + Back", false, "no projects in catalog");
  } else {
    const enc = encodeURIComponent(projectName);
    await page.goto(`${baseUrl}/custom-projects?project=${enc}&tab=settings&task=1`, { waitUntil: "domcontentloaded" });
    await waitLoaded(page);
    const detail = page.locator(`[data-automation-project-detail="${projectName}"]`);
    await detail.waitFor({ timeout: 40000 });
    const mode = await detail.getAttribute("data-sqx-module-mode");
    const hasBack = await detail.locator("[data-automation-back]").count();
    const hasPipeline = await detail.locator("[data-automation-task-pipeline]").count();
    record("Custom projects Full settings pipeline + Back", mode === "custom" && hasBack > 0 && hasPipeline > 0, `project=${projectName}, mode=${mode}`);
  }

  console.log("\n--- SUMMARY ---");
  console.log(JSON.stringify({ sqxModule503, results }, null, 2));
  const failed = results.filter((r) => !r.pass);
  process.exitCode = failed.length ? 1 : 0;
} catch (error) {
  console.error("WALK ERROR:", error.message);
  try {
    const dump = await page.evaluate(() => {
      const el = document.querySelector("[data-automation-workflows]");
      return {
        href: location.href,
        title: document.title,
        hostState: el?.getAttribute("data-automation-workflows") || null,
        hostText: (el?.innerText || "").slice(0, 800),
        bodyStart: (document.body?.innerText || "").slice(0, 800),
      };
    });
    console.error("WALK DUMP:", JSON.stringify(dump, null, 2));
  } catch (dumpError) {
    console.error("WALK DUMP FAILED:", dumpError.message);
  }
  process.exitCode = 2;
} finally {
  await browser.close();
}
