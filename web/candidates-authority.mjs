const ZONES = ["build", "evolution", "models", "custody"];

export const CANDIDATE_AUTHORITY_ZONES = Object.freeze([...ZONES]);

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function pendingBadge(label = "Status pending") {
  return `<span class="status-badge status-pending"><span class="status-dot"></span>${escapeHtml(label)}</span>`;
}

function pendingBoundary(title, detail) {
  return `<div class="empty-state"><div class="empty-icon">—</div><div><strong>${escapeHtml(title)}</strong><p>${escapeHtml(detail)}</p></div></div>`;
}

function disabledAction(label, reason) {
  return `<button class="button button-disabled" type="button" disabled title="${escapeHtml(reason)}">${escapeHtml(label)}</button>`;
}

function panel({ zone, eyebrow, title, description, body, accent }) {
  return `<article class="panel candidates-zone candidates-zone-${zone}" data-candidates-zone="${zone}" data-accent="${accent}"><div class="panel-heading"><div><p class="eyebrow">${escapeHtml(eyebrow)}</p><h2>${escapeHtml(title)}</h2></div>${pendingBadge()}</div><p class="panel-description">${escapeHtml(description)}</p>${body}</article>`;
}

export function renderCandidatesAuthority(strategyRef) {
  if (typeof strategyRef !== "string" || strategyRef.length === 0) {
    throw new TypeError("Candidates authority requires an exact requested strategy reference");
  }

  const encodedRef = encodeURIComponent(strategyRef);
  const signalsPath = `/strategies/${encodedRef}/signals`;

  return `<section class="context-callout candidates-authority-callout"><span class="callout-icon">◇</span><div><span class="eyebrow">Candidate authority boundary</span><strong>Candidate generation modes stay distinct</strong><span>Manual / bounded build, Evolutionary Search, and model-assisted work are separate paths. This page carries the opaque requested reference but does not invent candidate identity, genetic settings, fitness, or validation state.</span></div><span class="context-lock">PRODUCER BOUND</span></section><section class="dashboard-grid three-up candidates-authority-grid">${panel({ zone: "build", eyebrow: "Manual / bounded build", title: "Builder", description: "Create or modify candidates only after the native Builder configuration producer publishes the applicable construction contract.", body: `${pendingBoundary("Builder configuration integration pending", "What to build, Parts to improve, data/backtest, trading options, and building-block settings are not inferred from the route reference.")}${disabledAction("Open Builder", "Builder configuration producer is not connected to this frontend.")}`, accent: "orange" })}${panel({ zone: "evolution", eyebrow: "Evolutionary Search", title: "Search handoff", description: "Evolutionary Search may generate candidates, but its genetic configuration and results remain owned by the backend search capability.", body: `${pendingBoundary("Evolutionary Search integration pending", "No population, generation, selection, crossover, mutation, island, restart, fitness, or frontier values are fabricated on the Candidates page.")}${disabledAction("Open Evolutionary Search", "Evolutionary Search producer is not connected to this frontend.")}`, accent: "purple" })}${panel({ zone: "models", eyebrow: "Machine Learning", title: "Eligible model assistance", description: "Models may participate only where authoritative strategy/catalog capability says they are eligible; prediction, sizing, filtering, and validation remain distinct semantics.", body: `${pendingBoundary("Model eligibility pending", "The requested strategy reference does not establish an attached model or a model-generated candidate.")}<a class="button button-secondary" href="${escapeHtml(signalsPath)}" data-route="${escapeHtml(signalsPath)}">Open Signals &amp; Models</a>`, accent: "cyan" })}</section>${panel({ zone: "custody", eyebrow: "Candidate custody", title: "Candidate records", description: "Candidate identity, lineage, generation source, objective values, and downstream validation status require authoritative candidate records.", body: pendingBoundary("Candidate records not available to this frontend", "No placeholder candidate rows, scores, ranks, champions, or validation outcomes are created while the candidate producer is pending."), accent: "green" })}`;
}
