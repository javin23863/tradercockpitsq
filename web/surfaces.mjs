// Explore / Automation / Operate / Settings in the prototype grammar. All values come from
// /api/status, /api/market/quotes and the Research read models; live/operate producers that
// do not exist yet render explicit "not connected" states, never placeholders with numbers.

import { researchPath } from "./model.mjs";
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
import { renderResearchCapabilityCoverage } from "./research-capabilities.mjs";

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

// ---------- Explore ----------

function renderExplore(route, { runtime, quotes, statusState }) {
  const research = runtime?.research_backend;
  const macro = runtime?.macro_series;
  const historical = quotes?.provider_hookup?.historical_fx_indices;
  const producerCard = card({
    title: "Native research producer",
    sub: "StrategyQuant X runtime the platform is authorized to inspect and control",
    headIcon: "research",
    accent: "purple",
    actions: recordChip(research, research ? `Verified ${research.build}` : "Ready"),
    body: research
      ? statList([["Producer", research.producer || "—"], ["Build", research.build || "—"], ["Verified", String(research.verified === true)], ["Inspection", research.inspection?.available ? "Available" : readable(research.inspection?.reason_code, "Unavailable")], ["Native execution", research.execution?.available ? "Available" : `Disabled · ${readable(research.execution?.reason_code)}`]])
      : statusRows(null),
    footer: viewAll(researchPath("evolution"), "Open Evolutionary Search"),
  });
  const feedsCard = card({
    title: "Data feeds",
    sub: "Live market-data provider seam",
    headIcon: "activity",
    accent: "blue",
    actions: quotes ? chip(quotes.status === "current" ? "Live" : readable(quotes.reason_code), quotes.status === "current" ? "ready" : "unavailable") : chip("Checking…", "pending"),
    body: quotes
      ? `${statList([
        ["Live provider", quotes.provider?.id || "Not connected"],
        ["Watchlist", quotes.watchlist?.length ? quotes.watchlist.map((row) => row.symbol).join(", ") : "None configured"],
        ["Hookup", quotes.provider_hookup?.interface || "—"],
        ["Historical FX/indices", historical ? `${historical.producer || "—"} · ${historical.source || "—"} (${historical.pipeline || "—"})` : "—"],
        ["FRED", macro ? `${readable(macro.status)}${macro.provider?.id ? ` · ${macro.provider.id}` : ""}` : "Checking…"],
        ["FRED series", macro?.series?.length ? macro.series.map((row) => row.id).join(", ") : (macro ? "None configured" : "Checking…")],
      ])}<p class="note">${escapeHtml(quotes.provider_hookup?.detail || quotes.detail || "")}</p>${macro?.provider_hookup?.detail ? `<p class="note">${escapeHtml(macro.provider_hookup.detail)}</p>` : ""}${historical?.detail ? `<p class="note">${escapeHtml(historical.detail)}</p>` : ""}`
      : statusRows(null),
    footer: viewAll("/settings", "Configure"),
  });
  const modelCard = card({
    title: "Models & assistant",
    sub: "Bounded model access under the consumer account boundary",
    headIcon: "bot",
    accent: "violet",
    actions: recordChip(runtime?.model),
    body: `${statusRows(runtime?.model)}${statList([
      ["Default model", runtime?.model?.default_model || (runtime ? "—" : "Checking…")],
      ["Fallbacks", runtime?.model ? (runtime.model.fallback_models?.length ? runtime.model.fallback_models.join(", ") : "None configured") : "Checking…"],
      ["Provider", runtime?.provider ? `${runtime.provider.provider || "—"} · ${readable(runtime.provider.reason_code, readable(runtime.provider.status))}` : "Checking…"],
      ["Credential scope", runtime?.provider?.credential_scope ? readable(runtime.provider.credential_scope) : (runtime ? "—" : "Checking…")],
      ["Account", runtime?.account ? readable(runtime.account.reason_code, readable(runtime.account.status)) : "Checking…"],
    ])}<p class="note">${escapeHtml(runtime?.provider?.spend_boundary?.detail || "Model/provider/fallback policy is backend configuration; browser code never selects models or holds credentials.")}</p>`,
    footer: viewAll(researchPath("catalog", "models"), "Machine Learning / Models"),
  });
  const extensionsCard = card({
    title: "Extensions & add-ons",
    sub: "Typed registered extension slots",
    headIcon: "grid",
    accent: "orange",
    actions: recordChip(runtime?.extensions),
    body: statusRows(runtime?.extensions),
  });
  const coverage = card({
    title: "Research capability coverage",
    sub: "Where each canonical backend/native read model is exposed in the desktop",
    headIcon: "table",
    accent: "neutral",
    body: `<div class="data-host">${renderResearchCapabilityCoverage()}</div>`,
  });
  void statusState;
  return `${pageTitle("Explore", { subtitle: "Discover producers, data feeds, models and registered capabilities." })}<div class="grid grid-4">${producerCard}${feedsCard}${modelCard}${extensionsCard}</div>${coverage}`;
}

// ---------- Automation ----------

function renderAutomation(route, { runtime }) {
  const research = runtime?.research_backend;
  const topology = card({
    title: "Native Custom Project topology",
    sub: "Read-only custody of one saved StrategyQuant X project: numbered tasks, kinds, databanks",
    headIcon: "table",
    accent: "purple",
    actions: recordChip(research, "Runtime verified"),
    body: `<div data-research-capability="native_custom_project_topology"></div>`,
  });
  const control = card({
    title: "Automation control",
    sub: "Native task execution stays native",
    headIcon: "automation",
    accent: "orange",
    actions: chip("Not connected", "unavailable"),
    body: `${unavailable("No automation control seam yet", "Custom Project run/stop and readback connect only through the trusted native gateway. TraderCockpit does not build a task-loop engine.", { compact: true })}${statList([["Registered workflows", "0"], ["Scheduled runs", "—"], ["Last native control", "—"]])}`,
    footer: `${actionButton("Run project", { iconName: "play", disabled: true, title: "Native project control is not connected" })}${actionButton("Schedule", { iconName: "clock", disabled: true, title: "Scheduling is not connected" })}`,
  });
  const extensions = card({
    title: "Extensions",
    sub: "Add-ons contribute through typed slots only",
    headIcon: "grid",
    accent: "blue",
    actions: recordChip(runtime?.extensions),
    body: statusRows(runtime?.extensions),
  });
  return `${pageTitle("Automation", { subtitle: "Inspect and control registered native workflows without recreating their engine." })}<div class="with-rail">${topology}<div class="stack">${control}${extensions}</div></div>`;
}

// ---------- Operate ----------

function liveKpi(label, record, fallbackNote) {
  const note = record?.detail || readable(record?.reason_code, fallbackNote);
  return kpi({ label, value: "—", note, tone: toneForStatus(record?.status || "unavailable") });
}

function renderOperate(route, { runtime, quotes }) {
  const signals = runtime?.live_signals;
  const riskRecord = runtime?.live_risk;
  const performance = runtime?.scoped_performance;
  const propSim = runtime?.prop_simulation;
  const kpis = `<div class="kpi-strip">${liveKpi("Live Runs", signals, "No live deployment producer")}${liveKpi("Positions", riskRecord, "No account producer")}${liveKpi("Daily P&L", performance, "No live execution producer")}${liveKpi("Buying Power", riskRecord, "No account producer")}${liveKpi("Drawdown", performance, "No live execution producer")}${liveKpi("Open Risk", riskRecord, "No account producer")}</div>`;
  const runs = card({
    title: "Live runs",
    sub: "Deployed strategies and shadow runs",
    headIcon: "activity",
    accent: "green",
    actions: recordChip(signals, "Live"),
    body: table({ columns: [{ label: "Strategy" }, { label: "Mode" }, { label: "Account" }, { label: "Status" }], rows: [], empty: signals?.detail || "No live or shadow runs. Historical research results are never shown as live runs." }),
  });
  const positions = card({
    title: "Positions",
    headIcon: "layers",
    accent: "blue",
    actions: recordChip(riskRecord, "Connected"),
    body: table({ columns: [{ label: "Instrument" }, { label: "Side" }, { label: "Size", align: "right" }, { label: "P&L", align: "right" }], rows: [], empty: riskRecord?.detail || "No positions. Connect a broker/account producer." }),
  });
  const broker = card({
    title: "Broker connection",
    headIcon: "shield",
    accent: "orange",
    actions: recordChip(runtime?.account, "Connected"),
    body: statusRows(runtime?.account),
    footer: viewAll("/settings", "Configure account"),
  });
  const risk = card({
    title: "Risk limits",
    headIcon: "shield",
    accent: "red",
    actions: recordChip(riskRecord),
    body: statList([["Daily loss limit", "—"], ["Max drawdown", "—"], ["Gross exposure", "—"], ["Position sizing", "—"]]),
    footer: chip(readable(riskRecord?.reason_code, "Requires account producer"), toneForStatus(riskRecord?.status || "unavailable")),
  });
  const simulation = card({
    title: "Prop firm simulation",
    sub: "Simulated accounts & challenges",
    headIcon: "target",
    accent: "green",
    actions: recordChip(propSim, "Connected"),
    body: propSim
      ? `${unavailable(readable(propSim.reason_code, "No simulation account connected"), propSim.detail, { compact: true })}${statList([["Balance", "—"], ["P&L", "—"], ["Challenge day", "—"], ["Drawdown", "—"]])}`
      : unavailable("Checking simulation status…", "Waiting for the canonical /api/status read model.", { tone: "pending", compact: true }),
    footer: chip(readable(propSim?.reason_code, "Requires simulation producer"), toneForStatus(propSim?.status || "unavailable")),
  });
  const feed = card({
    title: "Market data",
    headIcon: "chart",
    accent: "cyan",
    actions: quotes ? chip(quotes.status === "current" ? "Live" : readable(quotes.reason_code), quotes.status === "current" ? "ready" : "unavailable") : chip("Checking…", "pending"),
    body: quotes ? statList([["Provider", quotes.provider?.id || "Not connected"], ["Watchlist", quotes.watchlist?.length ? String(quotes.watchlist.length) : "0"]]) : statusRows(null),
  });
  const promotions = card({
    title: "Promoted strategies",
    sub: "Operator promotion after Proof — not live, export, or deployment",
    headIcon: "check",
    accent: "green",
    body: `<div data-operate-promotions-host>${unavailable("Reading promotion custody…", "Promotion binds an immutable Research Proof. It never converts historical evidence into live runs.", { tone: "pending", compact: true })}</div>`,
  });
  const exports = card({
    title: "Exported strategies",
    sub: "Delivery export custody after Promotion — not broker send or deployment",
    headIcon: "external",
    accent: "purple",
    body: `<div data-operate-exports-host>${unavailable("Reading export custody…", "Export records promoted identities only. It never writes strategy bytes outside custody or claims broker/MT4 export.", { tone: "pending", compact: true })}</div>`,
  });
  return `${pageTitle("Operate", { subtitle: "Live and simulated operation — explicitly separate from historical research." })}${kpis}<div class="grid grid-3">${runs}${positions}${broker}${risk}${simulation}${feed}${promotions}${exports}</div>`;
}

// ---------- Settings ----------

function renderSettings(route, { runtime, quotes, statusState }) {
  const research = runtime?.research_backend;
  const nativeRuntime = research?.runtime;
  const macro = runtime?.macro_series;
  const historical = quotes?.provider_hookup?.historical_fx_indices;
  const connectSchwab = quotes?.provider_hookup?.authorize_path
    ? `<p class="note"><a class="button button-secondary" href="${escapeHtml(quotes.provider_hookup.authorize_path)}">Connect Schwab</a> Desktop <code>--port</code> must match <code>SCHWAB_CALLBACK_URL</code>.</p>`
    : "";
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
  const nativeStatus = nativeRuntime
    ? `${statList([["Expected build", nativeRuntime.build?.expected || "—"], ["Observed build", nativeRuntime.build?.observed || "—"], ["Build verified", String(nativeRuntime.build?.verified === true)], ["Inspection", nativeRuntime.inspection?.available ? "Available" : readable(nativeRuntime.inspection?.reason_code, "Unavailable")], ["Launcher", nativeRuntime.launcher ? `${nativeRuntime.launcher.relative_path || "—"} · ${readable(nativeRuntime.launcher.status)}` : "—"], ["Launcher trust", nativeRuntime.launcher ? readable(nativeRuntime.launcher.reason_code, nativeRuntime.launcher.verified ? "Verified" : "Unverified") : "—"], ["Execution", nativeRuntime.execution?.available ? "Available" : `Disabled · ${readable(nativeRuntime.execution?.reason_code)}`]])}<p class="note">${escapeHtml(research.detail || "")}</p>`
    : statusRows(research);
  const native = card({
    title: "Native research runtime",
    sub: "StrategyQuant X build, launcher trust and execution gate",
    headIcon: "research",
    accent: "blue",
    actions: recordChip(research, research ? `Verified ${research.build}` : "Ready"),
    body: `${nativeStatus}<div data-native-runtime-setup></div>`,
  });
  const feeds = card({
    title: "Data feeds",
    sub: "Live market-data provider seam",
    headIcon: "activity",
    accent: "cyan",
    actions: quotes ? chip(quotes.status === "current" ? "Live" : readable(quotes.reason_code), quotes.status === "current" ? "ready" : "unavailable") : chip("Checking…", "pending"),
    body: quotes
      ? `${identityRows([
        ["Interface", quotes.provider_hookup?.interface || "—"],
        ["Watchlist env", quotes.provider_hookup?.watchlist_env || "—"],
        ["Configured symbols", quotes.watchlist?.map((row) => row.symbol).join(", ") || "none"],
        ["Live credentials", Array.isArray(quotes.provider_hookup?.credential_env) ? quotes.provider_hookup.credential_env.join(", ") : (quotes.provider_hookup?.credential_env || "—")],
        ["Historical FX/indices", historical ? `${historical.producer || "—"} · ${historical.source || "native"}` : "—"],
        ["FRED env", macro?.provider_hookup?.credential_env || "FRED_API_KEY"],
        ["FRED series env", macro?.provider_hookup?.series_env || "TRADERCOCKPIT_FRED_SERIES"],
        ["FRED status", macro ? readable(macro.reason_code, readable(macro.status)) : "Checking…"],
      ])}<p class="note">${escapeHtml(quotes.provider_hookup?.detail || "")}</p>${connectSchwab}`
      : unavailable("Checking…", "Waiting for /api/market/quotes.", { tone: "pending", compact: true }),
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
  const extensions = card({
    title: "Extensions",
    headIcon: "grid",
    accent: "orange",
    actions: recordChip(runtime?.extensions),
    body: statusRows(runtime?.extensions),
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
  return `${pageTitle("Settings", { subtitle: "Account, model policy, native runtime, data feeds and custody." })}<div class="grid grid-3">${account}${model}${native}${feeds}${custodyCard}${extensions}${application}</div>`;
}

export function renderSecondarySurface(route, states) {
  if (route.surfaceId === "explore") return renderExplore(route, states);
  if (route.surfaceId === "automation") return renderAutomation(route, states);
  if (route.surfaceId === "operate") return renderOperate(route, states);
  if (route.surfaceId === "settings") return renderSettings(route, states);
  return `${pageTitle(route.label || "TraderCockpit")}${unavailable("Unknown surface", "Returned without inventing a product surface.")}`;
}
