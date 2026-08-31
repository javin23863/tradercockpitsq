import { runReadRequestPath } from "./run-read.mjs";

const CONTENT_ADDRESS_RE = /^tc:([a-z0-9-]+):v1:sha256:[0-9a-f]{64}$/;
const TERMINAL_RESULT_STATUSES = new Set(["completed", "passed", "failed"]);
const GATE_OPERATORS = new Set(["gt", "gte", "lt", "lte", "eq"]);
const CONTEXT_KEYS = [
  "leftRunRef",
  "leftInvocationId",
  "rightRunRef",
  "rightInvocationId",
];

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

function requiredText(value, name) {
  if (typeof value !== "string" || value.length === 0) {
    throw new Error(`${name} must be a non-empty string`);
  }
  return value;
}

function validateGateOutcome(outcome, index) {
  if (!outcome || typeof outcome !== "object" || Array.isArray(outcome)) {
    throw new Error(`validation outcome ${index} must be an object`);
  }
  requiredText(outcome.metric_path, `validation outcome ${index} metric_path`);
  if (!GATE_OPERATORS.has(outcome.operator)) {
    throw new Error(`validation outcome ${index} has an unsupported operator`);
  }
  requiredText(outcome.threshold, `validation outcome ${index} threshold`);
  requiredText(outcome.actual, `validation outcome ${index} actual`);
  if (typeof outcome.passed !== "boolean") {
    throw new Error(`validation outcome ${index} passed must be boolean`);
  }
  return outcome;
}

export function validateComparableRun(payload, requestedStrategyRef = "") {
  if (!payload || typeof payload !== "object" || Array.isArray(payload)) {
    throw new Error("Comparable run response must be an object");
  }
  if (payload.schema !== "tc.initial-run-read.v1") {
    throw new Error("Unexpected comparable run schema");
  }
  requireAddress(payload.run_ref, "backtest-run", "run_ref");
  requireAddress(payload.lifecycle_event_ref, "run-lifecycle-event", "lifecycle_event_ref");
  requiredText(payload.invocation_id, "invocation_id");
  if (!TERMINAL_RESULT_STATUSES.has(payload.status) || payload.terminal !== true) {
    throw new Error("Comparable run must be terminal with a durable result");
  }

  const inputs = payload.inputs;
  if (!inputs || typeof inputs !== "object" || Array.isArray(inputs)) {
    throw new Error("Comparable run inputs are missing");
  }
  requireAddress(inputs.candidate_ref, "candidate", "candidate_ref");
  requireAddress(inputs.strategy_ref, "strategy", "strategy_ref");
  requireAddress(inputs.data_ref, "data", "data_ref");
  requireAddress(inputs.execution_ref, "execution", "execution_ref");
  requireAddress(inputs.engine_build_ref, "engine-build", "engine_build_ref");
  if (requestedStrategyRef && inputs.strategy_ref !== requestedStrategyRef) {
    throw new Error("Comparable run belongs to a different requested strategy");
  }

  const artifacts = payload.artifacts;
  if (!artifacts || typeof artifacts !== "object" || Array.isArray(artifacts)) {
    throw new Error("Comparable run artifacts are missing");
  }
  optionalAddress(artifacts.receipt_ref, "run-receipt", "receipt_ref");
  const resultRef = requireAddress(artifacts.result_ref, "result", "result_ref");
  optionalAddress(artifacts.plan_ref, "validation-plan", "plan_ref");
  optionalAddress(artifacts.decision_ref, "validation-decision", "decision_ref");
  optionalAddress(artifacts.evidence_manifest_ref, "evidence-manifest", "evidence_manifest_ref");

  const result = payload.result;
  if (!result || typeof result !== "object" || Array.isArray(result)) {
    throw new Error("Comparable run is missing durable result metadata");
  }
  requiredText(result.result_schema, "result_schema");
  const resultProducerBuildRef = requireAddress(
    result.producer_build_ref,
    "engine-build",
    "result producer_build_ref",
  );
  if (resultProducerBuildRef !== inputs.engine_build_ref) {
    throw new Error("Result producer build does not match run engine build");
  }

  const validation = payload.validation;
  if (validation !== null && validation !== undefined) {
    if (!validation || typeof validation !== "object" || Array.isArray(validation)) {
      throw new Error("validation must be an object or null");
    }
    if (typeof validation.passed !== "boolean") {
      throw new Error("validation passed must be boolean");
    }
    if (validation.source_result_schema !== result.result_schema) {
      throw new Error("Validation schema does not match durable result schema");
    }
    if (!Array.isArray(validation.outcomes) || validation.outcomes.length === 0) {
      throw new Error("Validation decision must expose at least one gate outcome");
    }
    validation.outcomes.forEach(validateGateOutcome);
    if (validation.passed !== validation.outcomes.every((outcome) => outcome.passed)) {
      throw new Error("Validation decision does not match its gate outcomes");
    }
  }

  if (payload.status === "passed") {
    if (validation?.passed !== true || !artifacts.evidence_manifest_ref) {
      throw new Error("Passed run lacks a verified passing evidence chain");
    }
  }
  if (payload.reason_code === "validation_rejected") {
    if (validation?.passed !== false || !artifacts.evidence_manifest_ref) {
      throw new Error("Validation rejection lacks a verified failing evidence chain");
    }
  }

  return { ...payload, _validated_result_ref: resultRef };
}

export async function fetchComparableRun(
  runRef,
  invocationId,
  requestedStrategyRef = "",
  fetchImpl = globalThis.fetch,
) {
  if (typeof fetchImpl !== "function") throw new Error("Fetch implementation is unavailable");
  const response = await fetchImpl(runReadRequestPath(runRef, invocationId), {
    method: "GET",
    headers: { accept: "application/json" },
  });
  let payload;
  try {
    payload = await response.json();
  } catch {
    throw new Error("Comparable run response was not valid JSON");
  }
  if (!response.ok) {
    throw new Error(payload?.detail || `Comparable run lookup failed (${response.status})`);
  }
  return validateComparableRun(payload, requestedStrategyRef);
}

function marketLabel(payload) {
  const data = payload.input_detail?.data;
  if (!data?.symbol || !data?.timeframe) return "Not available";
  return `${data.symbol} · ${data.timeframe}`;
}

function validationLabel(payload) {
  if (payload.validation?.passed === true) return "Passed";
  if (payload.validation?.passed === false) return "Failed";
  return "No validation decision";
}

function gateKey(outcome) {
  return `${outcome.metric_path}\u0000${outcome.operator}\u0000${outcome.threshold}`;
}

function comparisonGateRows(left, right) {
  const leftOutcomes = Array.isArray(left.validation?.outcomes) ? left.validation.outcomes : [];
  const rightOutcomes = Array.isArray(right.validation?.outcomes) ? right.validation.outcomes : [];
  const leftByKey = new Map(leftOutcomes.map((outcome) => [gateKey(outcome), outcome]));
  const rightByKey = new Map(rightOutcomes.map((outcome) => [gateKey(outcome), outcome]));
  const keys = [...new Set([...leftByKey.keys(), ...rightByKey.keys()])].sort();
  return keys.map((key) => {
    const leftOutcome = leftByKey.get(key) ?? null;
    const rightOutcome = rightByKey.get(key) ?? null;
    const template = leftOutcome ?? rightOutcome;
    return {
      metric_path: template.metric_path,
      operator: template.operator,
      threshold: template.threshold,
      left: leftOutcome,
      right: rightOutcome,
    };
  });
}

export function compareCanonicalRuns(leftValue, rightValue, requestedStrategyRef = "") {
  const left = validateComparableRun(leftValue, requestedStrategyRef);
  const right = validateComparableRun(rightValue, requestedStrategyRef);
  if (left.result.result_schema !== right.result.result_schema) {
    throw new Error("Results are not comparable because their result schemas differ");
  }
  return {
    schema: "tc.result-comparison-read.v1",
    result_schema: left.result.result_schema,
    same_strategy: left.inputs.strategy_ref === right.inputs.strategy_ref,
    same_candidate: left.inputs.candidate_ref === right.inputs.candidate_ref,
    same_data: left.inputs.data_ref === right.inputs.data_ref,
    same_execution: left.inputs.execution_ref === right.inputs.execution_ref,
    same_engine_build: left.inputs.engine_build_ref === right.inputs.engine_build_ref,
    same_market: marketLabel(left) === marketLabel(right),
    left,
    right,
    gates: comparisonGateRows(left, right),
  };
}

export function resultCompareContext(search = "") {
  const params = new URLSearchParams(search);
  const values = {};
  for (const key of CONTEXT_KEYS) {
    const entries = params.getAll(key);
    if (entries.length !== 1 || entries[0].length === 0) return null;
    values[key] = entries[0];
  }
  return values;
}

export function resultCompareContextPath(path, context, search = "") {
  const params = new URLSearchParams(search);
  for (const key of CONTEXT_KEYS) {
    params.delete(key);
    params.set(key, context[key]);
  }
  const query = params.toString();
  return query ? `${path}?${query}` : path;
}

function rowsHtml(rows) {
  return rows
    .map(
      ([label, value]) => `<div class="run-field"><span>${escapeHtml(label)}</span><strong>${escapeHtml(value)}</strong></div>`,
    )
    .join("");
}

function runRows(payload) {
  return [
    ["Run", payload.run_ref],
    ["Invocation", payload.invocation_id],
    ["Lifecycle", payload.status],
    ["Result", payload.artifacts.result_ref],
    ["Result schema", payload.result.result_schema],
    ["Validation", validationLabel(payload)],
    ["Strategy", payload.inputs.strategy_ref],
    ["Candidate", payload.inputs.candidate_ref],
    ["Market", marketLabel(payload)],
    ["Data", payload.inputs.data_ref],
    ["Execution", payload.inputs.execution_ref],
    ["Engine build", payload.inputs.engine_build_ref],
  ];
}

function compatibilityRows(comparison) {
  return [
    ["Result schema", comparison.result_schema],
    ["Same strategy", comparison.same_strategy ? "Yes" : "No"],
    ["Same candidate", comparison.same_candidate ? "Yes" : "No"],
    ["Same market", comparison.same_market ? "Yes" : "No"],
    ["Same data identity", comparison.same_data ? "Yes" : "No"],
    ["Same execution assumptions", comparison.same_execution ? "Yes" : "No"],
    ["Same engine build", comparison.same_engine_build ? "Yes" : "No"],
  ];
}

function gateRowsHtml(gates) {
  if (gates.length === 0) {
    return rowsHtml([["Validation gates", "Neither result has a validation decision"]]);
  }
  return gates
    .map((gate) => {
      const label = `${gate.metric_path} ${gate.operator} ${gate.threshold}`;
      const left = gate.left
        ? `${gate.left.actual} · ${gate.left.passed ? "Passed" : "Failed"}`
        : "Not evaluated";
      const right = gate.right
        ? `${gate.right.actual} · ${gate.right.passed ? "Passed" : "Failed"}`
        : "Not evaluated";
      return `<div class="run-field"><span>${escapeHtml(label)}</span><strong>Left: ${escapeHtml(left)} · Right: ${escapeHtml(right)}</strong></div>`;
    })
    .join("");
}

export function renderResultCompareAuthority(requestedStrategyRef = "") {
  const contextual = requestedStrategyRef
    ? `<p class="field-help">Both runs must resolve to requested strategy ${escapeHtml(requestedStrategyRef)}.</p>`
    : `<p class="field-help">No strategy filter is active. Different strategy identities may be compared when their result schemas match.</p>`;
  return `
    <section class="panel" data-accent="cyan" data-result-compare-authority>
      <div class="panel-heading">
        <div><p class="eyebrow">Canonical comparison</p><h2>Compare two exact results</h2></div>
        <span class="status-badge status-pending" data-result-compare-status><span class="status-dot"></span>Exact identities required</span>
      </div>
      <p class="panel-description">This view compares only fields exposed by the canonical run reader. It never reads hidden result payloads, invents missing metrics, or declares a superior strategy.</p>
      <form class="strategy-form" data-result-compare-form>
        <div class="dashboard-grid two-up">
          <div>
            <p class="eyebrow">Left result</p>
            <label>Run reference<input name="leftRunRef" required autocomplete="off" placeholder="tc:backtest-run:v1:sha256:…" /></label>
            <label>Invocation ID<input name="leftInvocationId" required autocomplete="off" placeholder="initial-001" /></label>
          </div>
          <div>
            <p class="eyebrow">Right result</p>
            <label>Run reference<input name="rightRunRef" required autocomplete="off" placeholder="tc:backtest-run:v1:sha256:…" /></label>
            <label>Invocation ID<input name="rightInvocationId" required autocomplete="off" placeholder="initial-002" /></label>
          </div>
        </div>
        <div class="form-row"><button class="button button-primary" type="submit">Compare exact results</button></div>
        ${contextual}
      </form>
      <p class="field-help" data-result-compare-message aria-live="polite">Enter two exact terminal run identities.</p>
      <div class="dashboard-grid three-up" data-result-compare-output hidden>
        <article class="panel" data-accent="purple"><div class="panel-heading"><div><p class="eyebrow">Left</p><h2>Exact result A</h2></div></div><div class="run-fields" data-result-compare-left></div></article>
        <article class="panel" data-accent="green"><div class="panel-heading"><div><p class="eyebrow">Compatibility</p><h2>What can be compared</h2></div></div><div class="run-fields" data-result-compare-compatibility></div></article>
        <article class="panel" data-accent="orange"><div class="panel-heading"><div><p class="eyebrow">Right</p><h2>Exact result B</h2></div></div><div class="run-fields" data-result-compare-right></div></article>
      </div>
      <article class="panel" data-accent="cyan" data-result-compare-gates hidden>
        <div class="panel-heading"><div><p class="eyebrow">Validation evidence</p><h2>Gate outcomes side by side</h2></div></div>
        <p class="panel-description">Gate outcomes are displayed as backend-owned evidence only. Different actual values are not converted into a ranking or recommendation.</p>
        <div class="run-fields" data-result-compare-gate-rows></div>
      </article>
    </section>`;
}

function requestedStrategy(shell) {
  return shell.querySelector("[data-requested-strategy-ref]")?.textContent ?? "";
}

function setStatus(authority, label, detail) {
  const status = authority.querySelector("[data-result-compare-status]");
  const message = authority.querySelector("[data-result-compare-message]");
  if (status) status.textContent = label;
  if (message) message.textContent = detail;
}

function renderComparison(authority, comparison) {
  const output = authority.querySelector("[data-result-compare-output]");
  const gates = authority.querySelector("[data-result-compare-gates]");
  const left = authority.querySelector("[data-result-compare-left]");
  const right = authority.querySelector("[data-result-compare-right]");
  const compatibility = authority.querySelector("[data-result-compare-compatibility]");
  const gateRows = authority.querySelector("[data-result-compare-gate-rows]");
  if (left) left.innerHTML = rowsHtml(runRows(comparison.left));
  if (right) right.innerHTML = rowsHtml(runRows(comparison.right));
  if (compatibility) compatibility.innerHTML = rowsHtml(compatibilityRows(comparison));
  if (gateRows) gateRows.innerHTML = gateRowsHtml(comparison.gates);
  if (output) output.hidden = false;
  if (gates) gates.hidden = false;
}

async function submitComparison(authority, form, strategyRef, fetchImpl, updateLocation = true) {
  const values = new FormData(form);
  const context = Object.fromEntries(
    CONTEXT_KEYS.map((key) => [key, values.get(key)?.toString() ?? ""]),
  );
  if (CONTEXT_KEYS.some((key) => !context[key])) return;
  const button = form.querySelector('button[type="submit"]');
  const output = authority.querySelector("[data-result-compare-output]");
  const gates = authority.querySelector("[data-result-compare-gates]");
  if (button) button.disabled = true;
  if (output) output.hidden = true;
  if (gates) gates.hidden = true;
  authority.dataset.resultCompareStatus = "loading";
  setStatus(authority, "Loading comparison", "Resolving both exact runs through the canonical run reader…");
  try {
    const [left, right] = await Promise.all([
      fetchComparableRun(context.leftRunRef, context.leftInvocationId, strategyRef, fetchImpl),
      fetchComparableRun(context.rightRunRef, context.rightInvocationId, strategyRef, fetchImpl),
    ]);
    const comparison = compareCanonicalRuns(left, right, strategyRef);
    renderComparison(authority, comparison);
    authority.dataset.resultCompareStatus = "loaded";
    setStatus(authority, "Comparison verified", `Comparable result schema: ${comparison.result_schema}`);
    if (updateLocation && typeof window !== "undefined") {
      const next = resultCompareContextPath(
        window.location.pathname,
        context,
        window.location.search,
      );
      window.history.replaceState(window.history.state, "", next);
    }
  } catch (error) {
    authority.dataset.resultCompareStatus = "error";
    setStatus(authority, "Comparison refused", error?.message || "Exact result comparison failed");
  } finally {
    if (button) button.disabled = false;
  }
}

export function installResultCompareAuthority(
  root = document.querySelector("#app"),
  fetchImpl = globalThis.fetch,
) {
  if (!root) return null;
  const shell = root.querySelector('.app-shell[data-state-key="validate.compare"]');
  if (!shell || shell.dataset.resultCompareEnhanced === "true") return null;
  const placeholder = shell.querySelector(".content-inner > .panel[data-accent=\"cyan\"]");
  if (!placeholder) return null;
  const strategyRef = requestedStrategy(shell);
  const template = document.createElement("template");
  template.innerHTML = renderResultCompareAuthority(strategyRef);
  const authority = template.content.firstElementChild;
  if (!authority) throw new Error("Result comparison authority failed to render");
  placeholder.replaceWith(authority);
  shell.dataset.resultCompareEnhanced = "true";

  const form = authority.querySelector("[data-result-compare-form]");
  form?.addEventListener("submit", (event) => {
    event.preventDefault();
    void submitComparison(authority, form, strategyRef, fetchImpl, true);
  });

  if (typeof window !== "undefined" && form) {
    const context = resultCompareContext(window.location.search);
    if (context) {
      for (const key of CONTEXT_KEYS) form.elements.namedItem(key).value = context[key];
      void submitComparison(authority, form, strategyRef, fetchImpl, false);
    }
  }
  return authority;
}

export function bootResultCompareIntegration(
  root = document.querySelector("#app"),
  fetchImpl = globalThis.fetch,
) {
  if (!root || typeof MutationObserver === "undefined") return null;
  const hydrate = () => installResultCompareAuthority(root, fetchImpl);
  const observer = new MutationObserver(hydrate);
  observer.observe(root, { childList: true, subtree: true });
  hydrate();
  return observer;
}

if (typeof document !== "undefined") bootResultCompareIntegration();
