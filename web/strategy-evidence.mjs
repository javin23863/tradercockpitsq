import {
  runReadContext,
  runReadContextPath,
  runReadRequestPath,
} from "./run-read.mjs";

const CONTENT_ADDRESS_RE = /^tc:([a-z0-9-]+):v1:sha256:[0-9a-f]{64}$/;
const ALLOWED_STATUSES = new Set(["ready", "running", "passed", "failed", "refused"]);

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function requireAddress(value, expectedKind, name) {
  if (typeof value !== "string") throw new Error(`${name} must be a content address`);
  const match = CONTENT_ADDRESS_RE.exec(value);
  if (!match || match[1] !== expectedKind) {
    throw new Error(`${name} must reference ${expectedKind}`);
  }
  return value;
}

function optionalAddress(value, expectedKind, name) {
  if (value === null || value === undefined) return null;
  return requireAddress(value, expectedKind, name);
}

export function validateStrategyEvidencePayload(payload, requestedStrategyRef) {
  if (!payload || typeof payload !== "object" || Array.isArray(payload)) {
    throw new Error("Run evidence response must be an object");
  }
  if (payload.schema !== "tc.initial-run-read.v1") {
    throw new Error("Unexpected run evidence schema");
  }
  if (typeof requestedStrategyRef !== "string" || requestedStrategyRef.length === 0) {
    throw new Error("Requested strategy reference is required");
  }

  requireAddress(payload.run_ref, "backtest-run", "run_ref");
  requireAddress(payload.lifecycle_event_ref, "run-lifecycle-event", "lifecycle_event_ref");
  if (typeof payload.invocation_id !== "string" || payload.invocation_id.length === 0) {
    throw new Error("invocation_id must be a non-empty string");
  }
  if (!ALLOWED_STATUSES.has(payload.status)) throw new Error("Unexpected lifecycle status");
  if (typeof payload.terminal !== "boolean") throw new Error("terminal must be boolean");

  const inputs = payload.inputs;
  if (!inputs || typeof inputs !== "object" || Array.isArray(inputs)) {
    throw new Error("Run evidence inputs are missing");
  }
  requireAddress(inputs.candidate_ref, "candidate", "candidate_ref");
  requireAddress(inputs.strategy_ref, "strategy", "strategy_ref");
  requireAddress(inputs.data_ref, "data", "data_ref");
  requireAddress(inputs.execution_ref, "execution", "execution_ref");
  requireAddress(inputs.engine_build_ref, "engine-build", "engine_build_ref");
  if (inputs.strategy_ref !== requestedStrategyRef) {
    throw new Error("Exact run belongs to a different strategy reference");
  }

  const artifacts = payload.artifacts;
  if (!artifacts || typeof artifacts !== "object" || Array.isArray(artifacts)) {
    throw new Error("Run evidence artifacts are missing");
  }
  const receiptRef = optionalAddress(artifacts.receipt_ref, "run-receipt", "receipt_ref");
  const resultRef = optionalAddress(artifacts.result_ref, "result", "result_ref");
  const planRef = optionalAddress(artifacts.plan_ref, "validation-plan", "plan_ref");
  const decisionRef = optionalAddress(artifacts.decision_ref, "validation-decision", "decision_ref");
  const evidenceRef = optionalAddress(
    artifacts.evidence_manifest_ref,
    "evidence-manifest",
    "evidence_manifest_ref",
  );

  if (evidenceRef && (!receiptRef || !resultRef || !planRef || !decisionRef)) {
    throw new Error("Evidence manifest is present without a complete evidence chain");
  }
  if (decisionRef && (!resultRef || !planRef)) {
    throw new Error("Validation decision is present without result and plan custody");
  }

  if (payload.status === "passed") {
    if (!evidenceRef || payload.validation?.passed !== true) {
      throw new Error("Passed lifecycle state lacks a passing evidence chain");
    }
  }
  if (payload.reason_code === "validation_rejected") {
    if (!evidenceRef || payload.validation?.passed !== false) {
      throw new Error("Validation rejection lacks a failing evidence chain");
    }
  }

  return payload;
}

export async function fetchStrategyEvidence(
  requestedStrategyRef,
  runRef,
  invocationId,
  fetchImpl = globalThis.fetch,
) {
  if (typeof fetchImpl !== "function") throw new Error("Fetch implementation is unavailable");
  const response = await fetchImpl(runReadRequestPath(runRef, invocationId), {
    headers: { accept: "application/json" },
    method: "GET",
  });
  let payload;
  try {
    payload = await response.json();
  } catch {
    throw new Error("Exact run evidence response was not valid JSON");
  }
  if (!response.ok) {
    throw new Error(payload?.detail || `Exact run evidence lookup failed (${response.status})`);
  }
  return validateStrategyEvidencePayload(payload, requestedStrategyRef);
}

function decisionLabel(payload) {
  if (payload.validation?.passed === true) return "Passed";
  if (payload.validation?.passed === false) return "Failed";
  return "No validation decision";
}

function artifactRows(payload) {
  const artifacts = payload.artifacts ?? {};
  return [
    ["Run", payload.run_ref],
    ["Invocation", payload.invocation_id],
    ["Lifecycle", payload.status],
    ["Lifecycle event", payload.lifecycle_event_ref],
    ["Candidate", payload.inputs.candidate_ref],
    ["Strategy", payload.inputs.strategy_ref],
    ["Receipt", artifacts.receipt_ref ?? "None"],
    ["Result", artifacts.result_ref ?? "None"],
    ["Validation plan", artifacts.plan_ref ?? "None"],
    ["Decision", artifacts.decision_ref ?? "None"],
    ["Evidence manifest", artifacts.evidence_manifest_ref ?? "None"],
  ];
}

function gateRows(payload) {
  const outcomes = Array.isArray(payload.validation?.outcomes) ? payload.validation.outcomes : [];
  if (outcomes.length === 0) return [["Validation gates", "None"]];
  return outcomes.map((outcome) => [
    outcome.metric_path ?? "Unknown metric",
    `${outcome.actual ?? "Not available"} ${outcome.operator ?? "?"} ${outcome.threshold ?? "Not available"} · ${outcome.passed === true ? "Passed" : outcome.passed === false ? "Failed" : "Not available"}`,
  ]);
}

function rowsHtml(rows) {
  return rows
    .map(
      ([label, value]) => `<div class="run-field"><span>${escapeHtml(label)}</span><strong>${escapeHtml(value)}</strong></div>`,
    )
    .join("");
}

export function renderStrategyEvidenceAuthority(requestedStrategyRef) {
  return `
    <article class="panel" data-accent="cyan">
      <div class="panel-heading">
        <div><p class="eyebrow">Evidence lookup</p><h2>Load exact run proof</h2></div>
        <span class="status-badge status-pending" data-strategy-evidence-badge><span class="status-dot"></span>Exact identity required</span>
      </div>
      <p class="panel-description">Evidence is read only through the canonical run reader. The requested strategy must match the run's verified StrategySpec identity exactly.</p>
      <form class="strategy-form" data-strategy-evidence-form>
        <label>Exact run reference
          <input name="runRef" type="text" autocomplete="off" required placeholder="tc:backtest-run:v1:sha256:…" />
        </label>
        <label>Invocation ID
          <input name="invocationId" type="text" autocomplete="off" required placeholder="initial-001" />
        </label>
        <div class="form-row"><button class="button button-primary" type="submit">Load evidence</button></div>
        <p class="field-help">Requested strategy: ${escapeHtml(requestedStrategyRef)}. No run is inferred from the route.</p>
      </form>
      <p class="field-help" data-strategy-evidence-message aria-live="polite">Enter an exact run reference and invocation ID.</p>
    </article>
    <article class="panel" data-accent="purple">
      <div class="panel-heading">
        <div><p class="eyebrow">Verified custody</p><h2>Strategy and run evidence</h2></div>
        <span class="status-badge status-pending"><span class="status-dot"></span>Read only</span>
      </div>
      <p class="panel-description">Lifecycle, receipt, result, validation, and evidence identities remain separate. A passing validation decision does not imply champion, promotion, deployment, or live-trading state.</p>
      <div class="run-fields" data-strategy-evidence-result>${rowsHtml([["Evidence chain", "Not loaded"]])}</div>
      <div class="detail-actions" data-strategy-evidence-actions hidden></div>
    </article>`;
}

function setMessage(authority, label, detail) {
  const badge = authority.querySelector("[data-strategy-evidence-badge]");
  const message = authority.querySelector("[data-strategy-evidence-message]");
  if (badge) badge.textContent = label;
  if (message) message.textContent = detail;
}

function renderLoaded(authority, payload) {
  const target = authority.querySelector("[data-strategy-evidence-result]");
  const actions = authority.querySelector("[data-strategy-evidence-actions]");
  if (target) {
    target.innerHTML = rowsHtml([
      ["Validation decision", decisionLabel(payload)],
      ...artifactRows(payload),
      ...gateRows(payload),
    ]);
  }
  if (actions && typeof window !== "undefined") {
    const resultPath = runReadContextPath(
      "/validate/results",
      payload.run_ref,
      payload.invocation_id,
      window.location.search,
    );
    actions.innerHTML = `<a class="button button-secondary" href="${escapeHtml(resultPath)}" data-route="${escapeHtml(resultPath)}">Open exact validation results</a>`;
    actions.hidden = false;
  }
}

async function submitEvidence(authority, form, requestedStrategyRef, fetchImpl, updateLocation = true) {
  const data = new FormData(form);
  const runRef = data.get("runRef")?.toString() ?? "";
  const invocationId = data.get("invocationId")?.toString() ?? "";
  if (!runRef || !invocationId) return;
  const button = form.querySelector('button[type="submit"]');
  const target = authority.querySelector("[data-strategy-evidence-result]");
  const actions = authority.querySelector("[data-strategy-evidence-actions]");
  if (button) button.disabled = true;
  if (actions) actions.hidden = true;
  authority.dataset.strategyEvidenceStatus = "loading";
  setMessage(authority, "Loading evidence", "Resolving the exact canonical run evidence chain…");
  try {
    const payload = await fetchStrategyEvidence(requestedStrategyRef, runRef, invocationId, fetchImpl);
    renderLoaded(authority, payload);
    authority.dataset.strategyEvidenceStatus = "loaded";
    setMessage(authority, "Evidence verified", `Loaded ${payload.run_ref} / ${payload.invocation_id}`);
    if (updateLocation && typeof window !== "undefined") {
      const next = runReadContextPath(
        window.location.pathname,
        payload.run_ref,
        payload.invocation_id,
        window.location.search,
      );
      window.history.replaceState(window.history.state, "", next);
    }
  } catch (error) {
    authority.dataset.strategyEvidenceStatus = "error";
    if (target) target.innerHTML = rowsHtml([["Evidence lookup", error?.message || "Exact evidence lookup failed"]]);
    setMessage(authority, "Evidence not loaded", error?.message || "Exact evidence lookup failed");
  } finally {
    if (button) button.disabled = false;
  }
}

export function installStrategyEvidenceAuthority(root = document.querySelector("#app"), fetchImpl = globalThis.fetch) {
  if (!root) return null;
  const shell = root.querySelector('.app-shell[data-state-key="strategies.evidence"]');
  if (!shell || shell.dataset.strategyEvidenceEnhanced === "true") return null;
  const requested = shell.querySelector("[data-requested-strategy-ref]")?.textContent ?? "";
  if (!requested) return null;
  const grid = shell.querySelector(".dashboard-grid.two-up");
  if (!grid) return null;

  const template = document.createElement("template");
  template.innerHTML = renderStrategyEvidenceAuthority(requested);
  grid.replaceChildren(template.content);
  grid.dataset.strategyEvidenceAuthority = "true";
  shell.dataset.strategyEvidenceEnhanced = "true";

  const form = grid.querySelector("[data-strategy-evidence-form]");
  form?.addEventListener("submit", (event) => {
    event.preventDefault();
    void submitEvidence(grid, form, requested, fetchImpl, true);
  });

  if (typeof window !== "undefined") {
    const context = runReadContext(window.location.search);
    if (context && form) {
      form.elements.namedItem("runRef").value = context.runRef;
      form.elements.namedItem("invocationId").value = context.invocationId;
      void submitEvidence(grid, form, requested, fetchImpl, false);
    }
  }
  return grid;
}

export function bootStrategyEvidenceIntegration(
  root = document.querySelector("#app"),
  fetchImpl = globalThis.fetch,
) {
  if (!root || typeof MutationObserver === "undefined") return null;
  const hydrate = () => installStrategyEvidenceAuthority(root, fetchImpl);
  const observer = new MutationObserver(hydrate);
  observer.observe(root, { childList: true, subtree: true });
  hydrate();
  return observer;
}

if (typeof document !== "undefined") bootStrategyEvidenceIntegration();
