import assert from "node:assert/strict";
import { mkdir, writeFile } from "node:fs/promises";
import { chromium } from "playwright";

const base = process.env.TRADERCOCKPIT_REVIEW_URL || "http://127.0.0.1:4383";
const output = ".git/ui-review/pages";
const pages = [
  ["home", "/", "[data-home-system-status][data-system-status='loaded']"],
  ["builder", "/builder", ".sqx-progress-shell"],
  ["settings-build", "/builder?tab=settings", ".full-settings"],
  ["custom-projects", "/custom-projects", "[data-project-search]"],
  ["apollo", "/apollo", ".assistant-widget[data-assistant-ready='true']"],
  ["data-manager", "/data-manager", "[data-data-dataset]:not(:disabled)"],
  ["settings", "/settings", "[data-runtime-recovery], #settings-runtime .stat-row"],
];
await mkdir(output, { recursive: true });
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage();
const errors = [];
page.on("pageerror", error => errors.push(error.message));
// This acceptance reads the installed runtime. Never launch, edit or delete its artifacts.
await page.route("**/api/**", route => {
  if (!["GET", "HEAD"].includes(route.request().method()) && !["/api/data-setup/inspect", "/api/data-setup/select"].some(path => route.request().url().endsWith(path))) {
    return route.abort();
  }
  return route.continue();
});
try {
  for (const width of [1440, 960]) {
    await page.setViewportSize({ width, height: 1000 });
    for (const [name, path, ready] of pages) {
      await page.goto(base + path);
      await page.locator(ready).first().waitFor({ timeout: 30000 });
      assert.equal(await page.locator(".primary-link").count(), 6, `${name}: six rail destinations`);
      const clipped = await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1);
      assert.equal(clipped, false, `${name} ${width}: page must not overflow horizontally`);
      if (name === "data-manager") {
        const dataset = page.locator("[data-data-dataset]");
        await dataset.selectOption({ index: 1 });
        await page.getByText("Selected reference settings", { exact: true }).waitFor();
        await page.locator("[data-data-file]").setInputFiles({ name: "inspection-example.csv", mimeType: "text/csv", buffer: Buffer.from("Date,Time,Open,High,Low,Close,Volume\n2024.01.02,10:00,100,102,99,101,20\n2024.01.02,11:00,101,103,100,102,25\n") });
        await page.getByText("Detected file contents", { exact: true }).waitFor();
      }
      await page.screenshot({ path: `${output}/${name}-${width}.png` });
      if (name === "builder") {
        const taskBounds = await page.locator("[data-results-task]").boundingBox();
        const tabsBounds = await page.locator(".automation-detail > .workflow-tabs").boundingBox();
        assert.ok(taskBounds.y >= tabsBounds.y + tabsBounds.height, "Task selector must remain in its workspace");
        const logs = page.locator(".progress-log-disclosure");
        await logs.locator("summary").focus();
        await page.keyboard.press("Enter");
        assert.equal(await logs.getAttribute("open"), "");
        await page.locator(".task-sequence summary").click();
        await page.locator("[data-automation-task-pipeline]").waitFor({ state: "visible" });
        await page.locator(".sqx-progress-column-mid details summary").click();
        await page.locator(".sqx-progress-column-mid .native-setup").waitFor({ state: "visible" });
        const dockBefore = await page.locator("[data-databank-dock]").boundingBox();
        const analysis = page.locator(".automation-detail-grid");
        await analysis.focus();
        await page.keyboard.press("End");
        await page.waitForFunction(() => document.querySelector(".automation-detail-grid").scrollTop > 0);
        assert.equal((await page.locator("[data-databank-dock]").boundingBox()).y, dockBefore.y, "Databank stays fixed while native settings scroll");
        const start = page.locator("[data-automation-control=run_project]");
        if (await start.isEnabled()) {
          await start.click();
          await page.locator("[data-automation-start-confirm][open]").waitFor();
          await page.locator('[data-automation-start-confirm] button[value="cancel"]').click();
          assert.equal(await page.locator("[data-automation-start-confirm][open]").count(), 0);
        }
      }
      if (name === "settings-build") {
        await page.locator(".task-sequence summary").click();
        await page.locator("[data-automation-task-pipeline]").waitFor({ state: "visible" });
        await page.locator(".task-sequence summary").click();
        const tabs = page.locator("[data-automation-section]");
        const last = tabs.last();
        await last.scrollIntoViewIfNeeded();
        await last.click();
        await page.locator(".full-settings").waitFor();
        assert.match(page.url(), /section=/);
      }
      if (name === "custom-projects") {
        const search = page.locator("[data-project-search]");
        await search.fill("no-such-native-project-acceptance");
        assert.equal(await page.locator("[data-automation-project-list] [data-automation-project]:visible").count(), 0);
        await page.waitForTimeout(2300); // Cross a real catalog poll while preserving the filter.
        assert.equal(await search.inputValue(), "no-such-native-project-acceptance");
        assert.equal(await page.locator("[data-automation-project-list] [data-automation-project]:visible").count(), 0);
        await search.fill("");
        assert.ok(await page.locator("[data-automation-project-list] [data-automation-project]:visible").count() > 0);
        const firstName = await page.locator("[data-automation-project-list] [data-automation-project]").first().getAttribute("data-automation-project");
        await search.fill(`  ${firstName.toUpperCase()}  `);
        assert.ok(await page.locator("[data-automation-project-list] [data-automation-project]:visible").count() > 0, "Project search ignores case and outer spaces");
        await page.locator('[data-automation-project-list] [data-automation-project]:visible [data-automation-open-tab="settings"]').first().click();
        const taskSelect = page.locator("[data-results-task]");
        await taskSelect.waitFor();
        const taskOptions = await taskSelect.locator("option").count();
        if (taskOptions > 1) {
          const target = await taskSelect.locator("option").last().getAttribute("value");
          await taskSelect.selectOption(target);
          await page.waitForURL(url => url.searchParams.get("task") === target);
          await page.locator(".full-settings").waitFor();
          assert.equal(await taskSelect.inputValue(), target);
        }
      }
      if (name === "apollo") {
        const starter = page.locator("[data-assistant-prompt]").first();
        const prompt = await starter.getAttribute("data-assistant-prompt");
        await starter.focus();
        await page.keyboard.press("Enter");
        assert.equal(await page.locator("textarea[name=message]").inputValue(), prompt);
        assert.equal(await page.locator("textarea[name=message]").evaluate(el => el === document.activeElement), true);
        assert.notEqual(await page.locator("textarea[name=message]").evaluate(el => getComputedStyle(el).outlineStyle), "none");
        assert.equal(await page.locator(".assistant-msg").count(), 0, "Draft must not send a message");
      }
      if (name === "settings") {
        await page.locator('a[href="#settings-storage"]').click();
        await page.locator("#settings-storage").waitFor({ state: "visible" });
        const bounds = await page.locator("#settings-storage").boundingBox();
        assert.ok(bounds.y >= 0 && bounds.y < 900, "Settings anchor must scroll the workspace");
      }
    }
  }
  assert.deepEqual(errors, []);
  await writeFile(`${output}/index.html`, `<!doctype html><meta charset="utf-8"><title>Workspace review</title><style>body{background:#08101c;color:#dce5f5;font:16px system-ui;margin:30px}section{margin:35px 0}img{width:100%;border:1px solid #31405c}a{color:#c3a4ff}.pair{display:grid;grid-template-columns:1fr 1fr;gap:12px}</style><h1>Connected workspace buildout</h1><p>Actual application captures at 1440 and 960 pixels. Previous layout appears alongside the new layout.</p>${pages.map(([name,path]) => `<section><h2><a href="${base + path}">${name}</a></h2><div class="pair"><div><h3>Before</h3><img src="../pages-before/${name}.png"></div><div><h3>Now · 1440</h3><img src="${name}-1440.png"></div></div><details><summary>960 pixels</summary><img src="${name}-960.png"></details></section>`).join("")}`);
  console.log("PASS: seven page views at 1440/960; native task controls, settings navigation, persistent project filter, Apollo draft/focus, six rails, no page errors.");
} finally {
  await browser.close();
}
