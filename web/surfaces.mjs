// SQX program-layout modules plus Operate / Settings. All values come from
// /api/status, /api/market/quotes, native module reads, and custody catalogs.
// Live/operate producers that do not exist yet render explicit "not connected"
// states, never placeholders with numbers.

import { INSPECT_MODULE_SURFACES, RUN_MODULE_SURFACES } from "./model.mjs";
import {
  actionButton,
  card,
  chip,
  escapeHtml,
  identityRows,
  kpi,
  pageTitle,
  readable,
  statList,
  table,
  toneForStatus,
  unavailable,
  viewAll,
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

function renderRunModule(route, { runtime }) {
  const research = runtime?.research_backend;
  const moduleName = RUN_MODULE_SURFACES[route.surfaceId] || route.label;
  const workflows = card({
    title: `${moduleName} · Progress | Full settings | Results`,
    sub: `Native ${moduleName} archive from the verified StrategyQuant X runtime — not a platform engine`,
    headIcon: route.surfaceId === "builder" ? "flask" : "automation",
    accent: "blue",
    className: "span-all",
    actions: recordChip(research, "Runtime verified"),
    body: `<div data-automation-workflows data-sqx-module="${escapeHtml(moduleName)}">${unavailable(`Loading ${moduleName}…`, `Reading user/projects/${moduleName}/project.cfx from the verified runtime.`, { tone: "pending", compact: true })}</div>`,
  });
  return `${pageTitle(moduleName, { subtitle: "Same native Progress, Full settings, and Results shell bound to this module archive." })}<div class="stack">${workflows}</div>`;
}

function renderCustomProjects(route, { runtime }) {
  void route;
  void runtime;
  return `<div class="sqx-projects-surface" data-automation-workflows>${unavailable("Loading native workflows…", "Listing saved Custom Projects from the verified StrategyQuant X runtime.", { tone: "pending", compact: true })}</div>`;
}

function renderInspectModuleSurface(route) {
  const moduleName = INSPECT_MODULE_SURFACES[route.surfaceId] || route.label;
  return `${pageTitle(moduleName, { subtitle: "Native StrategyQuant X module — no substitute editor" })}<div data-sqx-inspect-host data-sqx-module="${escapeHtml(moduleName)}">${unavailable("Reading native module…", "Inspecting the verified StrategyQuant X runtime for this module archive.", { tone: "pending", compact: true })}</div>`;
}

// ---------- Operate ----------

function renderOperate(route, { quotes }) {
  const kpis = `<div class="kpi-strip">${["Live Runs", "Positions", "Daily P&L", "Buying Power", "Drawdown", "Open Risk"].map((label) => kpi({ label, value: "—", note: "No live execution/account producer", tone: "unavailable" })).join("")}</div>`;
  const runs = card({
    title: "Live runs",
    sub: "Deployed strategies and shadow runs",
    headIcon: "activity",
    accent: "green",
    actions: chip("Not connected", "unavailable"),
    body: table({ columns: [{ label: "Strategy" }, { label: "Mode" }, { label: "Account" }, { label: "Status" }], rows: [], empty: "No live or shadow runs. Promotion requires a live execution producer; historical research results are never shown as live runs." }),
  });
  const positions = card({
    title: "Positions",
    headIcon: "layers",
    accent: "blue",
    body: table({ columns: [{ label: "Instrument" }, { label: "Side" }, { label: "Size", align: "right" }, { label: "P&L", align: "right" }], rows: [], empty: "No positions. Connect a broker/account producer." }),
  });
  const broker = card({
    title: "Broker / execution",
    sub: "Live account and order producer — not Custom Project Automation",
    headIcon: "shield",
    accent: "orange",
    actions: chip("Not connected", "unavailable"),
    body: unavailable("No broker producer connected", "Operate stays empty until a live account/execution producer exists. Apollo's MetaTrader tool is not this producer.", { compact: true }),
    footer: viewAll("/settings", "Settings"),
  });
  const risk = card({
    title: "Risk limits",
    headIcon: "shield",
    accent: "red",
    body: statList([["Daily loss limit", "—"], ["Max drawdown", "—"], ["Gross exposure", "—"], ["Position sizing", "—"]]),
    footer: chip("Requires account/execution producer", "unavailable"),
  });
  const simulation = card({
    title: "Prop firm simulation",
    sub: "Simulated accounts & challenges",
    headIcon: "target",
    accent: "green",
    body: unavailable("No simulation account connected", "Prop-firm / paper simulation is part of Delivery / Simulation after Proof; it never converts historical evidence into live truth.", { compact: true }),
  });
  const feed = card({
    title: "Market data",
    sub: "Live quote and bar producer — not Custom Project databanks",
    headIcon: "chart",
    accent: "cyan",
    actions: quotes ? chip(quotes.status === "current" ? "Live" : readable(quotes.reason_code), quotes.status === "current" ? "ready" : "unavailable") : chip("Not connected", "unavailable"),
    body: quotes
      ? `${statList([["Watchlist", quotes.watchlist?.length ? quotes.watchlist.map((row) => row.symbol).join(", ") : "None configured"], ["Hookup", quotes.provider_hookup?.interface || "—"]])}<p class="note">${escapeHtml(quotes.provider_hookup?.detail || quotes.detail || "Live quotes stay unavailable until a market-data provider is configured.")}</p>`
      : unavailable("No live market-data producer", "Apollo's TradingView tool is not this producer.", { compact: true }),
    footer: viewAll("/settings", "Settings"),
  });
  return `${pageTitle("Operate", { subtitle: "Live and simulated operation — explicitly separate from historical research." })}${kpis}<div class="grid grid-3">${runs}${positions}${broker}${risk}${simulation}${feed}</div>`;
}

// ---------- Settings ----------

function renderSettings(route, { runtime, quotes, statusState }) {
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
      ? `${statList([["Expected build", nativeRuntime.build?.expected || "—"], ["Observed build", nativeRuntime.build?.observed || "—"], ["Build verified", String(nativeRuntime.build?.verified === true)], ["Inspection", nativeRuntime.inspection?.available ? "Available" : readable(nativeRuntime.inspection?.reason_code, "Unavailable")], ["Launcher", nativeRuntime.launcher ? `${nativeRuntime.launcher.relative_path || "—"} · ${readable(nativeRuntime.launcher.status)}` : "—"], ["Launcher trust", nativeRuntime.launcher ? readable(nativeRuntime.launcher.reason_code, nativeRuntime.launcher.verified ? "Verified" : "Unverified") : "—"], ["Execution", nativeRuntime.execution?.available ? "Available" : `Disabled · ${readable(nativeRuntime.execution?.reason_code)}`]])}${nativeRuntimeNotes(research)}`
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
    sub: "Start uses the verified StrategyQuant X runtime and trusted launcher. There is no StrategyQuant X MCP.",
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
  const catalog = card({
    title: "Native StrategyQuant X plugins",
    sub: "These run inside SQX Results. They are not a top-level product tab.",
    headIcon: "grid",
    accent: "orange",
    className: "span-all",
    actions: recordChip(runtime?.extensions, "Packaged"),
    body: `<div data-capability-registry data-capability-slot="explore.extensions" data-capability-view="catalog">${unavailable("Loading native plugins…", "Packaged StrategyQuant X Results plugins and authoring skills.", { tone: "pending", compact: true })}</div>`,
  });
  const extensions = card({
    title: "Install SQX plugins",
    sub: "Copy packaged plugins into the authorized StrategyQuant X runtime. Settings stay in SQX Results.",
    headIcon: "grid",
    accent: "orange",
    className: "span-all",
    actions: recordChip(runtime?.extensions, "Packaged"),
    body: `<div data-capability-registry data-capability-slot="settings.extensions" data-capability-view="install">${unavailable("Loading plugin install list…", "Install uses the verified runtime. The browser cannot choose this path.", { tone: "pending", compact: true })}</div>`,
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
  return `${pageTitle("Settings", { subtitle: "Account, model policy, native runtime, Apollo tools, Custom Project launch, and custody." })}<div class="grid grid-3">${account}${model}${native}${feeds}${metatrader}${launch}${custodyCard}${catalog}${extensions}${application}</div>`;
}

export function renderSecondarySurface(route, states) {
  if (route.surfaceId in RUN_MODULE_SURFACES) return renderRunModule(route, states);
  if (route.surfaceId === "custom-projects") return renderCustomProjects(route, states);
  if (route.surfaceId in INSPECT_MODULE_SURFACES) return renderInspectModuleSurface(route);
  if (route.surfaceId === "operate") return renderOperate(route, states);
  if (route.surfaceId === "settings") return renderSettings(route, states);
  return `${pageTitle(route.label || "TraderCockpit")}${unavailable("Unknown surface", "Returned without inventing a product surface.")}`;
}
