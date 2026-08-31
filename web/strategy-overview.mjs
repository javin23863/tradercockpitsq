import {
  runReadContext,
  runReadContextPath,
  runReadRequestPath,
} from "./run-read.mjs";

const RUN_READ_SCHEMA = "tc.initial-run-read.v1";
const CONTENT_ADDRESS = /^tc:([a-z0-9-]+):v([1-9][0-9]*):sha256:([0-9a-f]{64})$/;

function objectValue(value, label) {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    throw new Error(`${label} must be an object`);
  }
  return value;
}

function nonEmptyText(value, label) {
  if (typeof value !== "string" || value.length === 0) {
    throw new Error(`${label} must be a non-empty string`);
  }
  return value;
}

function address(value, expectedKind, label) {
  const text = nonEmptyText(value, label);
  const match = text.match(CONTENT_ADDRESS);
  if (!match || match[1] !== expectedKind) {
    throw new Error(`${label} must be a canonical ${expectedKind} content address`);
  }
  return text;
}

function optionalAddress(value, expectedKind, label) {
  if (value == null) return null;
  return address(value, expectedKind, label);
}

function detailText(value, fallback = "Not available") {
  return typeof value === "string" && value.length > 0 ? value : fallback;
}

function terminalText(value) {
  return value === true ? "Yes" : value === false ? "No" : "Not available";
}

function validationText(validation) {
  if (validation?.passed === true) return "Passed";
  if (validation?.passed === false) return "Failed";
  return "None";
}

export function validateStrategyOverviewPayload(
  payload,
  { requestedStrategyRef, runRef, invocationId },
) {
  const record = objectValue(payload, "run read payload");
  if (record.schema !== RUN_READ_SCHEMA) {
    throw new Error("run read schema is not supported by Strategy Overview");
  }

  const expectedRunRef = address(runRef, "backtest-run", "requested run reference");
  const returnedRunRef = address(record.run_ref, "backtest-run", "returned run reference");
  if (returnedRunRef !== expectedRunRef) {
    throw new Error("backend run identity does not match the requested run");
  }

  const expectedInvocation = nonEmptyText(invocationId, "requested invocation ID");
  const returnedInvocation = nonEmptyText(record.invocation_id, "returned invocation ID");
  if (returnedInvocation !== expectedInvocation) {
    throw new Error("backend invocation identity does not match the requested invocation");
  }

  nonEmptyText(record.status, "lifecycle status");
  if (typeof record.terminal !== "boolean") {
    throw new Error("terminal lifecycle state must be explicit");
  }
  nonEmptyText(record.occurred_at, "lifecycle timestamp");
  address(record.lifecycle_event_ref, "run-lifecycle-event", "lifecycle event reference");

  const inputs = objectValue(record.inputs, "run inputs");
  address(inputs.candidate_ref, "candidate", "candidate reference");
  const returnedStrategyRef = address(inputs.strategy_ref, "strategy", "strategy reference");
  address(inputs.data_ref, "data", "data reference");
  address(inputs.execution_ref, "execution", "execution reference");
  const engineBuildRef = address(inputs.engine_build_ref, "engine-build", "engine build reference");

  const expectedStrategyRef = address(
    requestedStrategyRef,
    "strategy",
    "requested strategy reference",
  );
  if (returnedStrategyRef !== expectedStrategyRef) {
    throw new Error("exact run belongs to a different strategy than this Overview route");
  }

  const artifacts = objectValue(record.artifacts, "run artifacts");
  optionalAddress(artifacts.receipt_ref, "run-receipt", "receipt reference");
  optionalAddress(artifacts.result_ref, "result", "result reference");
  optionalAddress(artifacts.plan_ref, "validation-plan", "validation plan reference");
  optionalAddress(artifacts.decision_ref, "validation-decision", "validation decision reference");
  optionalAddress(
    artifacts.evidence_manifest_ref,
    "evidence-manifest",
    "evidence manifest reference",
  );

  if (record.result != null) {
    const result = objectValue(record.result, "result metadata");
    const producerBuildRef = address(
      result.producer_build_ref,
      "engine-build",
      "result producer build reference",
    );
    if (producerBuildRef !== engineBuildRef) {
      throw new Error("result producer build does not match the exact run engine build");
    }
    nonEmptyText(result.result_schema, "result schema");
  }

  return record;
}

export function strategyOverviewRows(payload) {
  const inputs = payload.inputs ?? {};
  const detail = payload.input_detail ?? {};
  const artifacts = payload.artifacts ?? {};
  const data = detail.data ?? {};
  const execution = detail.execution ?? {};
  const engine = detail.engine_build ?? {};
  return [
    ["Run reference", payload.run_ref],
    ["Invocation", payload.invocation_id],
    ["Lifecycle", payload.status],
    ["Terminal", terminalText(payload.terminal)],
    ["Occurred at", payload.occurred_at],
    ["Candidate", inputs.candidate_ref],
    ["Candidate origin", detailText(detail.candidate?.origin)],
    ["Strategy", inputs.strategy_ref],
    ["Strategy schema", detailText(detail.strategy?.semantic_schema)],
    ["Market", data.symbol && data.timeframe ? `${data.symbol} · ${data.timeframe}` : "Not available"],
    ["Data source", detailText(data.source)],
    ["Dataset revision", detailText(data.dataset_revision)],
    ["Data", inputs.data_ref],
    [
      "Execution assumptions",
      execution.starting_cash && execution.currency
        ? `${execution.starting_cash} ${execution.currency}`
        : "Not available",
    ],
    ["Execution", inputs.execution_ref],
    [
      "Engine",
      engine.implementation && engine.revision
        ? `${engine.implementation} · ${engine.revision}`
        : "Not available",
    ],
    ["Engine build", inputs.engine_build_ref],
    ["Receipt", artifacts.receipt_ref ?? "None"],
    ["Result", artifacts.result_ref ?? "None"],
    ["Validation", validationText(payload.validation)],
    ["Decision", artifacts.decision_ref ?? "None"],
    ["Evidence", artifacts.evidence_manifest_ref ?? "None"],
  ];
}

export function strategyOverviewActionSpecs(payload, search = "") {
  const specs = [
    {
      label: "Open exact invocation in Operate",
      path: runReadContextPath(
        "/operate/runs",
        payload.run_ref,
        payload.invocation_id,
        search,
      ),
    },
  ];
  const artifacts = payload.artifacts ?? {};
  if (artifacts.result_ref || artifacts.decision_ref || artifacts.evidence_manifest_ref) {
    specs.push({
      label: "Open verified results",
      path: runReadContextPath(
        "/validate/results",
        payload.run_ref,
        payload.invocation_id,
        search,
      ),
    });
  }
  return specs;
}

function makeField(label, value) {
  const row = document.createElement("div");
  row.className = "run-field";
  const name = document.createElement("span");
  name.textContent = label;
  const content = document.createElement("strong");
  content.textContent = String(value ?? "Not available");
  row.append(name, content);
  return row;
}

function renderRows(target, rows) {
  target.className = "run-fields";
  target.replaceChildren(...rows.map(([label, value]) => makeField(label, value)));
}

function setAuthorityStatus(authority, status) {
  authority.dataset.strategyOverviewStatus = status;
}

function renderError(authority, target, detail) {
  setAuthorityStatus(authority, "error");
  renderRows(target, [["Canonical strategy resolution", detail || "Request failed"]]);
}

function actionLink(spec) {
  const link = document.createElement("a");
  link.className = "button button-secondary";
  link.href = spec.path;
  link.dataset.route = spec.path;
  link.textContent = spec.label;
  return link;
}

function syncContext(payload) {
  if (typeof window === "undefined" || typeof window.history?.replaceState !== "function") return;
  const path = runReadContextPath(
    window.location.pathname,
    payload.run_ref,
    payload.invocation_id,
    window.location.search,
  );
  if (`${window.location.pathname}${window.location.search}` !== path) {
    window.history.replaceState(window.history.state, "", path);
  }
}

function updateBaseOverview(shell, payload) {
  const panels = shell.querySelectorAll(".dashboard-grid.three-up .panel");
  const referencePanel = panels[0];
  const activityPanel = panels[2];

  if (referencePanel) {
    const description = referencePanel.querySelector(".panel-description");
    if (description) {
      description.textContent =
        "The requested reference exactly matches the canonical StrategySpec identity returned by the inspected run.";
    }
    const stats = referencePanel.querySelectorAll(".stat-row strong");
    if (stats[0]) stats[0].textContent = "Verified through exact canonical run";
    if (stats[1]) stats[1].textContent = "Not inferred from run custody";
    referencePanel.dataset.strategyCustodyStatus = "verified";
  }

  if (activityPanel) {
    const description = activityPanel.querySelector(".panel-description");
    if (description) {
      description.textContent =
        "One exact linked invocation is verified below. This is not a latest-run or complete-history claim.";
    }
    const target = activityPanel.querySelector(".empty-state");
    if (target) {
      renderRows(target, [
        ["Run", payload.run_ref],
        ["Invocation", payload.invocation_id],
        ["Lifecycle", payload.status],
        ["Result", payload.artifacts?.result_ref ?? "None"],
      ]);
    }
    activityPanel.dataset.strategyActivityStatus = "verified";
  }
}

async function fetchOverviewRun(runRef, invocationId) {
  const response = await fetch(runReadRequestPath(runRef, invocationId), {
    method: "GET",
    headers: { accept: "application/json" },
  });
  const payload = await response.json();
  if (!response.ok) {
    throw new Error(payload?.detail || `Run lookup failed (${response.status})`);
  }
  return payload;
}

async function submitOverview(form, updateLocation = true) {
  const authority = form.closest("[data-strategy-overview-authority]");
  const shell = authority?.closest('.app-shell[data-state-key="strategies.overview"]');
  const result = authority?.querySelector("[data-strategy-overview-result]");
  const actions = authority?.querySelector("[data-strategy-overview-actions]");
  const requestedStrategyRef = authority?.dataset.requestedStrategyRef ?? "";
  if (!authority || !shell || !result || !actions) return;

  const data = new FormData(form);
  const runRef = data.get("runRef")?.toString() ?? "";
  const invocationId = data.get("invocationId")?.toString() ?? "";
  if (!runRef || !invocationId) return;

  const button = form.querySelector('button[type="submit"]');
  if (button) button.disabled = true;
  actions.hidden = true;
  actions.replaceChildren();
  setAuthorityStatus(authority, "loading");
  renderRows(result, [["Canonical strategy resolution", "Loading exact run custody…"]]);

  try {
    const payload = validateStrategyOverviewPayload(
      await fetchOverviewRun(runRef, invocationId),
      { requestedStrategyRef, runRef, invocationId },
    );
    renderRows(result, strategyOverviewRows(payload));
    updateBaseOverview(shell, payload);
    const specs = strategyOverviewActionSpecs(
      payload,
      typeof window === "undefined" ? "" : window.location.search,
    );
    actions.replaceChildren(...specs.map(actionLink));
    actions.hidden = false;
    setAuthorityStatus(authority, "loaded");
    if (updateLocation) syncContext(payload);
  } catch (error) {
    renderError(
      authority,
      result,
      error?.message || "Exact strategy/run custody could not be verified.",
    );
  } finally {
    if (button) button.disabled = false;
  }
}

function requestedStrategyFromPath(pathname) {
  const match = String(pathname || "").match(/^\/strategies\/([^/]+)\/overview$/);
  if (!match) return "";
  try {
    return decodeURIComponent(match[1]);
  } catch {
    return "";
  }
}

function enhanceStrategyOverview() {
  if (typeof window === "undefined") return;
  const requestedStrategyRef = requestedStrategyFromPath(window.location.pathname);
  if (!requestedStrategyRef) return;
  const shell = document.querySelector('.app-shell[data-state-key="strategies.overview"]');
  if (!shell || shell.dataset.strategyOverviewEnhanced === "true") return;
  shell.dataset.strategyOverviewEnhanced = "true";

  const authority = document.createElement("section");
  authority.className = "panel";
  authority.dataset.accent = "purple";
  authority.dataset.strategyOverviewAuthority = "true";
  authority.dataset.strategyOverviewStatus = "idle";
  authority.dataset.requestedStrategyRef = requestedStrategyRef;
  authority.innerHTML = `
    <div class="panel-heading">
      <div>
        <p class="eyebrow">Canonical strategy resolution</p>
        <h2>Verify linked activity</h2>
      </div>
      <span class="status-badge status-pending"><span class="status-dot"></span>Exact lookup</span>
    </div>
    <p class="panel-description">Verify this requested strategy against one exact canonical run and invocation. This does not discover latest activity, infer strategy policy, or create a second strategy authority.</p>
    <form class="strategy-form" data-strategy-overview-form>
      <label>Exact run reference
        <input name="runRef" type="text" autocomplete="off" required placeholder="tc:backtest-run:v1:sha256:…" />
      </label>
      <label>Invocation ID
        <input name="invocationId" type="text" autocomplete="off" required placeholder="initial-001" />
      </label>
      <div class="form-row"><button class="button button-primary" type="submit">Verify exact linked run</button></div>
      <p class="field-help">The canonical run reader must return the exact StrategySpec identity encoded by this Overview route.</p>
    </form>
    <div class="run-fields" data-strategy-overview-result aria-live="polite"></div>
    <div class="detail-actions" data-strategy-overview-actions hidden></div>`;

  const contextCallout = shell.querySelector(".context-callout");
  if (contextCallout) contextCallout.after(authority);
  else shell.prepend(authority);

  const result = authority.querySelector("[data-strategy-overview-result]");
  renderRows(result, [["Canonical strategy resolution", "Enter one exact run reference and invocation ID."]]);

  const context = runReadContext(window.location.search);
  if (context) {
    const form = authority.querySelector("[data-strategy-overview-form]");
    form.elements.namedItem("runRef").value = context.runRef;
    form.elements.namedItem("invocationId").value = context.invocationId;
    void submitOverview(form, false);
  }
}

if (typeof document !== "undefined") {
  document.addEventListener("submit", (event) => {
    const form = event.target.closest?.("[data-strategy-overview-form]");
    if (!form) return;
    event.preventDefault();
    void submitOverview(form);
  });

  const root = document.querySelector("#app");
  if (root) {
    enhanceStrategyOverview();
    new MutationObserver(enhanceStrategyOverview).observe(root, {
      childList: true,
      subtree: true,
    });
  }
}
