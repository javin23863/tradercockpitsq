import { installWorkflowAuthority, renderWorkflowAuthority } from "./workflow-authority.mjs";

const AUTOMATION_PATH = "/operate/runs?automation=1";

function isOperateRoute() {
  return window.location.pathname.startsWith("/operate");
}

function isAutomationMode() {
  const params = new URLSearchParams(window.location.search);
  return window.location.pathname === "/operate/runs" && params.get("automation") === "1";
}

function decorateOperate() {
  const app = document.querySelector("#app");
  if (!app) return;

  if (isOperateRoute()) {
    const nav = app.querySelector(".secondary-nav");
    if (nav && !nav.querySelector("[data-workflow-mode-link]")) {
      const link = document.createElement("a");
      link.className = `subnav-link${isAutomationMode() ? " is-active" : ""}`;
      link.href = AUTOMATION_PATH;
      link.dataset.workflowModeLink = "true";
      link.setAttribute("aria-current", isAutomationMode() ? "page" : "false");
      link.textContent = "Automation";
      nav.append(link);
    }
  }

  if (!isAutomationMode()) return;
  const content = app.querySelector(".content-inner");
  if (!content || content.dataset.workflowMode === "automation") return;
  content.dataset.workflowMode = "automation";
  content.innerHTML = renderWorkflowAuthority();
  installWorkflowAuthority(content);
}

function navigateAutomation(event) {
  const link = event.target.closest?.("[data-workflow-mode-link]");
  if (!link) return;
  event.preventDefault();
  window.history.pushState({}, "", AUTOMATION_PATH);
  window.dispatchEvent(new PopStateEvent("popstate"));
  queueMicrotask(decorateOperate);
}

export function installOperateAutomationMode() {
  if (typeof document === "undefined" || typeof window === "undefined") return;
  const app = document.querySelector("#app");
  if (!app || app.dataset.workflowModeInstalled === "true") return;
  app.dataset.workflowModeInstalled = "true";
  app.addEventListener("click", navigateAutomation, true);
  const observer = new MutationObserver(() => queueMicrotask(decorateOperate));
  observer.observe(app, { childList: true, subtree: true });
  window.addEventListener("popstate", () => queueMicrotask(decorateOperate));
  decorateOperate();
}

if (typeof document !== "undefined") {
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", installOperateAutomationMode, { once: true });
  } else {
    installOperateAutomationMode();
  }
}
