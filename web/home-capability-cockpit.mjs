import {
  researchCapabilityCoverageManifest,
  researchCapabilityCoverageSummary,
} from "./research-capabilities.mjs";

const WORKFLOW = Object.freeze([
  Object.freeze({ label: "Idea", path: "/research?stage=construct&tab=idea", detail: "Capture immutable research intent and provenance." }),
  Object.freeze({ label: "Specification", path: "/research?stage=construct&tab=specification", detail: "Inspect the exact native Builder configuration surface." }),
  Object.freeze({ label: "Build", path: "/research?stage=construct&tab=build", detail: "Approve exact configuration bytes and launch native Builder." }),
  Object.freeze({ label: "Candidates", path: "/research?stage=construct&tab=candidates", detail: "Bind exact native Builder output into Candidate custody." }),
  Object.freeze({ label: "Backtest", path: "/research?stage=backtest&tab=overview", detail: "Run and reopen native Historical Retester evidence." }),
  Object.freeze({ label: "Robustness", path: "/research?stage=backtest&tab=robustness", detail: "Execute producer-backed Higher Precision validation." }),
  Object.freeze({ label: "Proof", path: "/research?stage=proof", detail: "Freeze and reopen the exact historical evidence chain." }),
]);

function esc(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function routeLink(path, label, className = "button button-secondary") {
  return `<a class="${esc(className)}" href="${esc(path)}" data-route="${esc(path)}">${esc(label)}</a>`;
}

export function homeCapabilityModel(payload = researchCapabilityCoverageManifest()) {
  const summary = researchCapabilityCoverageSummary(payload);
  const mapped = payload.capabilities.filter((item) => item.coverage === "mapped");
  const unavailable = payload.capabilities.filter((item) => item.coverage === "explicitly_unavailable");
  const hidden = payload.capabilities.filter((item) => item.coverage === "intentionally_hidden");
  return Object.freeze({
    schema: payload.schema,
    summary: Object.freeze({ ...summary }),
    mapped: Object.freeze(mapped.map((item) => Object.freeze({ ...item }))),
    unavailable: Object.freeze(unavailable.map((item) => Object.freeze({ ...item }))),
    hidden: Object.freeze(hidden.map((item) => Object.freeze({ ...item }))),
    workflow: WORKFLOW,
  });
}

function capabilityCard(item) {
  return `<article class="home-capability-card" data-home-capability="${esc(item.id)}">
    <div class="home-capability-card-head"><span class="status-badge status-mapped"><span class="status-dot"></span>Mapped</span><code>${esc(item.source_schemas[0] || "canonical read model")}</code></div>
    <h3>${esc(item.label)}</h3>
    <p>${esc(item.detail)}</p>
    <div class="home-capability-card-meta"><span>${esc(item.surface)}</span><span>${esc(item.api_paths.join(" · "))}</span></div>
    ${routeLink(item.route, "Open capability →", "home-capability-link")}
  </article>`;
}

function unavailableCard(item) {
  return `<article class="home-boundary-card" data-home-capability-boundary="${esc(item.id)}">
    <div><span class="status-badge status-unavailable"><span class="status-dot"></span>Producer seam unavailable</span><code>${esc(item.reason_code)}</code></div>
    <h3>${esc(item.label)}</h3>
    <p>${esc(item.detail)}</p>
    ${routeLink(item.route, "Open owning surface →", "home-capability-link")}
  </article>`;
}

function workflowStep(step, index) {
  return `<a class="home-workflow-step" href="${esc(step.path)}" data-route="${esc(step.path)}" data-home-workflow-step="${index + 1}">
    <span>${index + 1}</span><div><strong>${esc(step.label)}</strong><p>${esc(step.detail)}</p></div><b aria-hidden="true">→</b>
  </a>`;
}

export function renderHomeCapabilityCockpit(payload = researchCapabilityCoverageManifest()) {
  const model = homeCapabilityModel(payload);
  return `<section class="home-capability-cockpit" data-home-capability-cockpit data-home-capability-schema="${esc(model.schema)}">
    <div class="home-capability-summary">
      <div><span class="eyebrow">Current Research product boundary</span><h2>The implemented Research product spine is here. This is the map.</h2><p>These cards describe implemented canonical seams, not current runtime readiness. The top-bar and Operational readiness surfaces continue to show whether StrategyQuant X and other producers are actually configured right now.</p></div>
      <div class="home-capability-counts" aria-label="Research capability coverage">
        <div data-home-capability-count="mapped"><strong>${model.summary.mapped}</strong><span>mapped</span></div>
        <div data-home-capability-count="unavailable"><strong>${model.summary.explicitly_unavailable}</strong><span>explicit boundaries</span></div>
        <div data-home-capability-count="hidden"><strong>${model.summary.intentionally_hidden}</strong><span>silently hidden</span></div>
      </div>
    </div>

    <div class="home-workflow" data-home-research-workflow>
      ${model.workflow.map(workflowStep).join("")}
    </div>

    <div class="home-capability-section-head"><div><span class="eyebrow">Mapped product surface</span><h2>Producer-backed capabilities</h2><p>Every card below maps to an implemented canonical read model or execution/custody seam. A mapped seam may still be runtime-unavailable until its producer is configured.</p></div>${routeLink("/research", "Open Research", "button button-primary")}</div>
    <div class="home-capability-grid">${model.mapped.map(capabilityCard).join("")}</div>

    <details class="home-boundary-disclosure" data-home-capability-boundaries>
      <summary><span><strong>${model.unavailable.length} explicit producer boundaries</strong><small>Capabilities we refuse to fake until StrategyQuant X exposes a verified typed/readback/write seam.</small></span><b>Review boundaries</b></summary>
      <div class="home-boundary-grid">${model.unavailable.map(unavailableCard).join("")}</div>
    </details>
  </section>`;
}

function setTextIfChanged(node, value) {
  if (node && node.textContent !== value) node.textContent = value;
}

function insertCapabilityCockpit(content) {
  let workspace = content.querySelector?.("[data-home-capability-cockpit]");
  if (workspace) return workspace;
  const hero = content.querySelector?.(".hero-band");
  const dashboard = content.querySelector?.("[data-home-zone-count]");
  const anchor = hero || dashboard;
  if (!anchor || typeof globalThis.document?.createElement !== "function") return null;
  workspace = globalThis.document.createElement("div");
  workspace.innerHTML = renderHomeCapabilityCockpit();
  const section = workspace.firstElementChild;
  if (!section) return null;
  anchor.insertAdjacentElement("beforebegin", section);
  return section;
}

function ensureOperationalHeading(content) {
  const dashboard = content.querySelector?.("[data-home-zone-count]");
  if (!dashboard) return null;
  let heading = content.querySelector?.("[data-home-operational-readiness-heading]");
  if (heading) return heading;
  if (typeof globalThis.document?.createElement !== "function") return null;
  heading = globalThis.document.createElement("div");
  heading.className = "home-operational-heading";
  heading.dataset.homeOperationalReadinessHeading = "";
  heading.innerHTML = '<span class="eyebrow">Live operations boundary</span><h2>Operational readiness</h2><p>These existing Home zones remain visible as truthful readiness surfaces, but they no longer define the product while their live producers are not connected.</p>';
  dashboard.insertAdjacentElement("beforebegin", heading);
  return heading;
}

export function ensureHomeCapabilityCockpit(documentLike = globalThis.document) {
  if (!documentLike) return null;
  const shell = documentLike.querySelector?.('[data-product-shell="tradercockpit-desktop"][data-surface-id="home"]');
  const content = shell?.querySelector?.(".content-inner");
  if (!content) return null;

  const intro = content.querySelector?.(".page-intro");
  setTextIfChanged(intro?.querySelector?.("h1"), "Capability Cockpit");
  setTextIfChanged(
    intro?.querySelector?.(".lede"),
    "Cockpit Home now starts with implemented TraderCockpit capabilities and keeps current producer readiness separate and fail-visible.",
  );

  const cockpit = insertCapabilityCockpit(content);
  ensureOperationalHeading(content);
  return cockpit;
}

let activeContent = null;

function bindHomeCapabilityCockpit() {
  const content = globalThis.document?.querySelector?.('[data-surface-id="home"] .content-inner');
  if (!content) {
    activeContent = null;
    return;
  }
  if (content === activeContent && content.querySelector?.("[data-home-capability-cockpit]")) return;
  activeContent = content;
  ensureHomeCapabilityCockpit();
}

if (typeof document !== "undefined") {
  const observer = new MutationObserver(() => bindHomeCapabilityCockpit());
  observer.observe(document.documentElement, { childList: true, subtree: true });
  bindHomeCapabilityCockpit();
}
