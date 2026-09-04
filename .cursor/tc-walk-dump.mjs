import { chromium } from "playwright";

const url = "http://127.0.0.1:4320/builder?tab=settings&task=1";
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage();
const errors = [];
page.on("console", (msg) => {
  if (msg.type() === "error") errors.push(`console ${msg.text()}`);
});
page.on("pageerror", (err) => errors.push(`throw ${err.message}`));
page.on("requestfailed", (req) => errors.push(`fail ${req.url()} ${req.failure()?.errorText}`));
await page.goto(url, { waitUntil: "commit", timeout: 60000 });
await page.waitForTimeout(8000);
const dump = await page.evaluate(() => ({
  href: location.href,
  ready: document.readyState,
  host: document.querySelector("[data-automation-workflows]")?.getAttribute("data-automation-workflows") || null,
  text: (document.body?.innerText || "").slice(0, 1000),
  scripts: [...document.scripts].map((s) => s.src).filter(Boolean),
}));
console.log(JSON.stringify({ dump, errors }, null, 2));
await browser.close();
