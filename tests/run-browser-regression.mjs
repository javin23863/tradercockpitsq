import { chromium } from "playwright";

import { runBrowserRegression } from "./browser-regression.mjs";

const baseUrl = process.env.TRADERCOCKPIT_BROWSER_BASE_URL || "http://127.0.0.1:4173";

const browser = await chromium.launch({ headless: true });
const context = await browser.newContext();
const page = await context.newPage();
const cdp = await context.newCDPSession(page);

const tab = {
  goto: (url) => page.goto(url, { waitUntil: "domcontentloaded" }),
  reload: () => page.reload({ waitUntil: "domcontentloaded" }),
  back: () => page.goBack({ waitUntil: "domcontentloaded" }),
  forward: () => page.goForward({ waitUntil: "domcontentloaded" }),
  playwright: {
    evaluate: (fn) => page.evaluate(fn),
    waitForTimeout: (ms) => page.waitForTimeout(ms),
    locator: (selector) => page.locator(selector),
  },
  capabilities: {
    get: async (name) => {
      if (name !== "cdp") throw new Error(`Unsupported browser capability: ${name}`);
      return cdp;
    },
  },
};

try {
  const result = await runBrowserRegression(tab, { baseUrl });
  console.log(
    `Browser regression passed: canonical ${result.canonical.length}, contextual ${result.contextual.length}, legacy ${result.legacy.length}`,
  );
} finally {
  await browser.close();
}
