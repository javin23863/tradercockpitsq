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

async function runApolloAcceptance() {
  const strategyRef = `tc:strategy:v1:sha256:${"a".repeat(64)}`;
  const runRef = `tc:backtest-run:v1:sha256:${"b".repeat(64)}`;
  const path = `/strategies/${encodeURIComponent(strategyRef)}/overview?${new URLSearchParams({ runRef, invocationId: "initial-001" })}`;
  await page.goto(`${baseUrl}${path}`, { waitUntil: "domcontentloaded" });

  const surface = page.locator("[data-apollo-surface]");
  const input = surface.locator(".apollo-form input");
  await input.waitFor({ state: "visible" });
  if (await input.isDisabled()) throw new Error("Apollo input remained disabled");
  const instance = await surface.getAttribute("data-apollo-instance");
  if (instance !== "1") throw new Error(`Unexpected Apollo instance ${instance}`);

  await input.fill("where am I");
  await surface.locator(".apollo-form button").click();
  const hint = surface.locator(".apollo-hint");
  await hint.waitFor({ state: "visible" });
  const contextText = await hint.textContent();
  if (!contextText?.includes(strategyRef) || !contextText.includes("initial-001")) {
    throw new Error("Apollo did not report exact strategy/run context");
  }

  await surface.locator('[data-apollo-actions] a', { hasText: "Open candidates" }).click();
  await page.waitForURL((url) => url.pathname.endsWith("/candidates"));
  const sameSurface = page.locator("[data-apollo-surface]");
  if ((await sameSurface.getAttribute("data-apollo-instance")) !== instance) {
    throw new Error("Apollo remounted during SPA navigation");
  }
  if (!(await sameSurface.locator(".apollo-hint").textContent())?.includes("initial-001")) {
    throw new Error("Apollo response did not persist across SPA navigation");
  }

  const apiRequests = [];
  const recordRequest = (request) => {
    const url = new URL(request.url());
    if (url.pathname.startsWith("/api/")) apiRequests.push(url.pathname);
  };
  page.on("request", recordRequest);
  await sameSurface.locator(".apollo-form input").fill("start the run");
  await sameSurface.locator(".apollo-form button").click();
  await page.waitForTimeout(50);
  page.off("request", recordRequest);
  if ((await sameSurface.locator(".apollo-hint").getAttribute("data-apollo-boundary")) !== "refused-autonomous-action") {
    throw new Error("Apollo did not refuse autonomous mutation request");
  }
  if (apiRequests.length !== 0) {
    throw new Error(`Apollo mutation refusal issued API requests: ${apiRequests.join(", ")}`);
  }
}

try {
  const result = await runBrowserRegression(tab, { baseUrl });
  await runApolloAcceptance();
  console.log(
    `Browser regression passed: canonical ${result.canonical.length}, contextual ${result.contextual.length}, legacy ${result.legacy.length}; Apollo acceptance passed`,
  );
} finally {
  await browser.close();
}
