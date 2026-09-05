// Core product surfaces. All values come from
// /api/status, /api/market/quotes, native module reads, and custody catalogs.
// Live/operate producers that do not exist yet render explicit "not connected"
// states, never placeholders with numbers.

import { renderAssistantWidget } from "./assistant.mjs";
import { INSPECT_MODULE_SURFACES, RUN_MODULE_SURFACES } from "./model.mjs";
import {
  actionButton,
  card,
  chip,
  escapeHtml,
  identityRows,
  pageTitle,
  readable,
  statList,
  toneForStatus,
  unavailable,
} from "./ui.mjs";

function recordChip(record, readyLabel = "Ready") {
  if (!record) return chip("Checking…", "pending");
  const tone = toneForStatus(record.status);
  const label = tone === "ready" ? readyLabel : readable(record.reason_code, readable(record.status));
  return chip(label, tone);
}

function statusRows(record, extra = []) {
  if (!record) return unavailable("Checking…", "Waiting for the canonical /api/status read model.", { tone: "pending", compact: true });
  const rows = [["Status", readable(record.status)], ["Reason", record.reason_code ? readable(record.reason_code) : "—"], ...extra];
  return `${statList(rows)}${record.detail ? `<p class="note">${escapeHtml(record.detail)}</p>` : ""}`;
}

function runtimeSourceLabel(source) {
  if (source === "environment") return "Process override";
  if (source === "remembered" || source === "discovered") return "Remembered on this machine";
  return "Not configured";
}

function nativeRuntimeNotes(research) {
  if (!research) return "";
  const failClosed = research.status !== "ready";
  const executionClosed = research.execution?.available !== true;
  const parts = [];
  if (typeof research.detail === "string" && research.detail) {
    const attr = failClosed ? " data-runtime-recovery" : "";
    parts.push(`<p class="note"${attr}>${escapeHtml(research.detail)}</p>`);
  }
  if (executionClosed && typeof research.execution?.detail === "string" && research.execution.detail && research.execution.detail !== research.detail) {
    parts.push(`<p class="note" data-runtime-recovery>${escapeHtml(research.execution.detail)}</p>`);
  }
  return parts.join("");
}

function producerRows(producer) {
  if (!producer) return statusRows(null);
  const apolloTool = producer.purpose === "apollo_llm_tool";
  const rows = [
    ["Purpose", apolloTool ? "Apollo / LLM tool" : (producer.kind === "native_workflow_control" ? "Custom Project control" : readable(producer.kind, "—"))],
    ["Job", producer.job || "—"],
    ["Transport", readable(producer.transport, "MCP")],
    ["Endpoint", producer.endpoint_configured ? "Process-side URL configured" : "Not configured"],
    ["Credential", producer.credential_configured ? "Process-side token present" : "Not configured"],
  ];
  if (!apolloTool && producer.kind !== "native_workflow_control") {
    rows.push(
      ["Live quotes", producer.live_quotes ? "Current" : "Not claimed"],
      ["Live positions", producer.live_positions ? "Current" : "Not claimed"],
    );
  }
  return `${statList(rows)}<p class="note">${escapeHtml(producer.detail || "")}</p>`;
}

function producerChip(producer) {
  if (!producer) return chip("Checking…", "pending");
  if (producer.endpoint_configured) return chip("Endpoint configured", "pending");
  return chip(readable(producer.reason_code, "Not configured"), "unavailable");
}

function renderRunModule(route) {
  const moduleName = RUN_MODULE_SURFACES[route.surfaceId] || route.label;
  return `<div class="sqx-projects-surface" data-automation-workflows data-sqx-module="${escapeHtml(moduleName)}">${unavailable(`Loading ${moduleName}…`, `Reading user/projects/${moduleName}/project.cfx from the verified runtime.`, { tone: "pending", compact: true })}</div>`;
}

function renderCustomProjects(route, { runtime }) {
  void route;
  void runtime;
  return `<div class="sqx-projects-surface" data-automation-workflows>${unavailable("Loading native workflows…", "Listing saved Custom Projects from the verified StrategyQuant X runtime.", { tone: "pending", compact: true })}</div>`;
}

function renderInspectModuleSurface(route) {
  const moduleName = INSPECT_MODULE_SURFACES[route.surfaceId] || route.label;
  if (moduleName === "Data manager") {
    return `${pageTitle("Data organization", { subtitle: "Read installed settings and inspect price files before choosing a backtest setup." })}<div data-sqx-inspect-host data-sqx-module="Data manager"></div>`;
  }
  return `${pageTitle(route.label || moduleName, { subtitle: "Native StrategyQuant X module — no substitute editor" })}<div data-sqx-inspect-host data-sqx-module="${escapeHtml(moduleName)}">${unavailable("Reading native module…", "Inspecting the verified StrategyQuant X runtime for this module archive.", { tone: "pending", compact: true })}</div>`;
}

// ---------- Settings ----------

function renderSettings(route, { runtime, statusState }) {
  const research = runtime?.research_backend;
  const nativeRuntime = research?.runtime;
  const account = card({
    title: "Consumer account",
    sub: "Google authenticates the consumer to the platform",
    headIcon: "crown",
    accent: "purple",
    actions: recordChip(runtime?.account, "Signed in"),
    body: statusRows(runtime?.account),
    footer: actionButton("Sign in with Google", { disabled: true, title: "Account authority is not implemented yet" }),
  });
  const model = card({
    title: "Model access",
    sub: "Provider-bounded spend, backend-selected model policy",
    headIcon: "bot",
    accent: "violet",
    actions: recordChip(runtime?.model),
    body: `${statusRows(runtime?.model)}${statList([["Provider authority", runtime?.provider ? readable(runtime.provider.reason_code, readable(runtime.provider.status)) : "Checking…"]])}`,
  });
  const native = card({
    title: "Native research runtime",
    sub: "StrategyQuant X build, launcher trust and execution gate",
    headIcon: "research",
    accent: "blue",
    actions: recordChip(research, research ? `Verified ${research.build}` : "Ready"),
    body: nativeRuntime
      ? `${statList([...(research?.binding?.source ? [["Runtime source", runtimeSourceLabel(research.binding.source)]] : []), ["Expected build", nativeRuntime.build?.expected || "—"], ["Observed build", nativeRuntime.build?.observed || "—"], ["Build verified", String(nativeRuntime.build?.verified === true)], ["Inspection", nativeRuntime.inspection?.available ? "Available" : readable(nativeRuntime.inspection?.reason_code, "Unavailable")], ["Launcher", nativeRuntime.launcher ? `${nativeRuntime.launcher.relative_path || "—"} · ${readable(nativeRuntime.launcher.status)}` : "—"], ["Launcher trust", nativeRuntime.launcher ? readable(nativeRuntime.launcher.reason_code, nativeRuntime.launcher.verified ? "Verified" : "Unverified") : "—"], ["Execution", nativeRuntime.execution?.available ? "Available" : `Disabled · ${readable(nativeRuntime.execution?.reason_code)}`]])}${nativeRuntimeNotes(research)}`
      : statusRows(research),
  });
  const feeds = card({
    title: "Apollo TradingView MCP",
    sub: "LLM tool so Apollo can interact with TradingView. Not Automation and not the robustness pipeline.",
    headIcon: "chart",
    accent: "cyan",
    actions: producerChip(runtime?.live_producers?.tradingview),
    body: producerRows(runtime?.live_producers?.tradingview),
  });
  const metatrader = card({
    title: "Apollo MetaTrader MCP",
    sub: "LLM tool so Apollo can interact with MetaTrader 5. Not Automation and not Operate P&L.",
    headIcon: "operate",
    accent: "green",
    actions: producerChip(runtime?.live_producers?.metatrader),
    body: producerRows(runtime?.live_producers?.metatrader),
  });
  const launchReady = research?.execution?.available === true;
  const launch = card({
    title: "Custom Project launch",
    sub: "Start uses the verified StrategyQuant X runtime and trusted launcher. TraderCockpit has no SQX MCP adapter.",
    headIcon: "automation",
    accent: "orange",
    actions: chip(launchReady ? "Launch ready" : readable(research?.execution?.reason_code, "Launch not ready"), launchReady ? "ready" : "unavailable"),
    body: `<p class="note">Start and stop call official sqcli -project action=start|stop and register the start process with the desktop worker supervisor. Progress streams producer log files. TradingView and MetaTrader MCP are Apollo tools, not this control seam.</p>`,
  });
  const custody = runtime?.research_custody;
  const custodyCard = card({
    title: "Research custody",
    sub: "Canonical local content-addressed store",
    headIcon: "layers",
    accent: "green",
    actions: recordChip(custody, "Bound"),
    body: custody?.contract
      ? identityRows([["Record kinds", custody.contract.record_kinds?.join(", ") || "—"], ["Identity schema", custody.contract.identity_schema || "—"], ["Revision schema", custody.contract.revision_schema || "—"], ["Evidence schema", custody.contract.evidence_schema || "—"], ["Current update", custody.contract.current_update || "—"]])
      : statusRows(custody),
  });
  const application = card({
    title: "Application",
    headIcon: "operate",
    accent: "neutral",
    actions: recordChip(runtime?.application),
    body: runtime?.application
      ? statList([["Server", readable(runtime.application.server)], ["Desktop", readable(runtime.application.desktop)], ["Status read", statusState.phase]])
      : statusRows(null),
  });
  return `${pageTitle("Settings", { subtitle: "Account, model policy, native runtime, Apollo tools, Custom Project launch, and custody." })}<div class="grid grid-3">${account}${model}${native}${feeds}${metatrader}${launch}${custodyCard}${application}</div>`;
}

function renderApolloSurface(route, { runtime }) {
  void route;
  return `<div class="assistant-page" data-assistant-page>${renderAssistantWidget(runtime, { layout: "page" })}</div>`;
}

export function renderSecondarySurface(route, states) {
  if (route.surfaceId === "apollo") return renderApolloSurface(route, states);
  if (route.surfaceId in RUN_MODULE_SURFACES) return renderRunModule(route, states);
  if (route.surfaceId === "custom-projects") return renderCustomProjects(route, states);
  if (route.surfaceId in INSPECT_MODULE_SURFACES) return renderInspectModuleSurface(route);
  if (route.surfaceId === "settings") return renderSettings(route, states);
  return `${pageTitle(route.label || "TraderCockpit")}${unavailable("Unknown surface", "Returned without inventing a product surface.")}`;
}
