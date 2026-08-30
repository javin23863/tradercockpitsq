const RUN_READ_ENDPOINT = "/api/run-read";

export function runReadRequestPath(runRef, invocationId) {
  const params = new URLSearchParams();
  params.set("runRef", String(runRef ?? ""));
  params.set("invocationId", String(invocationId ?? ""));
  return `${RUN_READ_ENDPOINT}?${params.toString()}`;
}

export function runReadContext(search = "") {
  const params = new URLSearchParams(search);
  const runRefs = params.getAll("runRef");
  const invocationIds = params.getAll("invocationId");
  if (runRefs.length !== 1 || invocationIds.length !== 1) return null;
  if (runRefs[0].length === 0 || invocationIds[0].length === 0) return null;
  return { runRef: runRefs[0], invocationId: invocationIds[0] };
}

export function runReadContextPath(path, runRef, invocationId, search = "") {
  const params = new URLSearchParams(search);
  params.set("runRef", String(runRef ?? ""));
  params.set("invocationId", String(invocationId ?? ""));
  const query = params.toString();
  return query ? `${path}?${query}` : path;
}

function terminalText(value) {
  return value === true ? "Yes" : value === false ? "No" : "Not available";
}

function decisionText(value) {
  return value === true ? "Passed" : value === false ? "Failed" : "None";
}

function marketText(data) {
  if (!data?.symbol || !data?.timeframe) return "Not available";
  return `${data.symbol} · ${data.timeframe}`;
}

function nativeContextText(context) {
  if (context?.kind !== "native") return "Not available";
  const producer = context.producer || "native producer";
  const project = context.source_project || "unknown project";
  const task = context.source_task ?? "?";
  return `${producer} · ${project} task ${task}`;
}

function engineText(engineBuild) {
  if (!engineBuild?.implementation || !engineBuild?.revision) return "Not available";
  return `${engineBuild.implementation} · ${engineBuild.revision}`;
}

function executionText(execution) {
  if (!execution?.starting_cash || !execution?.currency) return "Not available";
  return `${execution.starting_cash} ${execution.currency}`;
}

function executionModelsText(execution) {
  const models = Array.isArray(execution?.models) ? execution.models : [];
  if (models.length === 0) return "None";
  return models
    .map((item) => `${item?.kind ?? "unknown"}:${item?.model ?? "unknown"}`)
    .join(", ");
}

function dataCustodyRows(data) {
  if (data?.kind === "native") {
    return [
      ["Native data context", nativeContextText(data)],
      ["Native data schema", data.context_schema ?? "Not available"],
      ["Native config SHA-256", data.source_config_sha256 ?? "Not available"],
      ["Candidate archive SHA-256", data.candidate_archive_sha256 ?? "Not available"],
      ["Candidate settings SHA-256", data.candidate_settings_sha256 ?? "Not available"],
    ];
  }
  return [
    ["Market", marketText(data)],
    ["Data source", data?.source ?? "Not available"],
    ["Dataset revision", data?.dataset_revision ?? "Not available"],
  ];
}

function executionCustodyRows(execution) {
  if (execution?.kind === "native") {
    return [
      ["Native execution context", nativeContextText(execution)],
      ["Native execution schema", execution.context_schema ?? "Not available"],
      ["Execution config SHA-256", execution.source_config_sha256 ?? "Not available"],
    ];
  }
  return [
    ["Execution assumptions", executionText(execution)],
    ["Execution models", executionModelsText(execution)],
  ];
}

export function runReadRows(payload) {
  const inputs = payload?.inputs ?? {};
  const detail = payload?.input_detail ?? {};
  const artifacts = payload?.artifacts ?? {};
  return [
    ["Status", payload?.status ?? "Not available"],
    ["Terminal", terminalText(payload?.terminal)],
    ["Run reference", payload?.run_ref ?? "Not available"],
    ["Invocation", payload?.invocation_id ?? "Not available"],
    ["Occurred at", payload?.occurred_at ?? "Not available"],
    ["Reason", payload?.reason_code ?? "None"],
    ["Candidate", inputs.candidate_ref ?? "Not available"],
    ["Candidate origin", detail.candidate?.origin ?? "Not available"],
    ["Strategy", inputs.strategy_ref ?? "Not available"],
    ["Strategy schema", detail.strategy?.semantic_schema ?? "Not available"],
    ...dataCustodyRows(detail.data),
    ["Data", inputs.data_ref ?? "Not available"],
    ...executionCustodyRows(detail.execution),
    ["Execution", inputs.execution_ref ?? "Not available"],
    ["Engine", engineText(detail.engine_build)],
    ["Engine build", inputs.engine_build_ref ?? "Not available"],
    ["Random seed", inputs.random_seed ?? "None"],
    ["Receipt", artifacts.receipt_ref ?? "None"],
    ["Result", artifacts.result_ref ?? "None"],
    ["Validation plan", artifacts.plan_ref ?? "None"],
    ["Decision", artifacts.decision_ref ?? "None"],
    ["Evidence", artifacts.evidence_manifest_ref ?? "None"],
  ];
}

function nativeResultRows(result) {
  if (result?.result_schema !== "sqx.native-retester-result.v1") return [];
  const payload = result?.payload ?? {};
  const producer = payload?.producer ?? {};
  const source = payload?.source ?? {};
  const nativeResult = payload?.result ?? {};
  return [
    ["Native task", String(producer.task ?? "Not available")],
    ["Producer exit code", String(producer.exit_code ?? "Not available")],
    ["Source archive SHA-256", source.archive_sha256 ?? "Not available"],
    ["Source settings SHA-256", source.settings_entry_sha256 ?? "Not available"],
    ["Retester config SHA-256", source.project_config_sha256 ?? "Not available"],
    ["Result archive SHA-256", nativeResult.archive_sha256 ?? "Not available"],
    ["Result archive bytes", String(nativeResult.archive_bytes ?? "Not available")],
    ["Result strategy SHA-256", nativeResult.strategy_entry_sha256 ?? "Not available"],
    ["Result settings SHA-256", nativeResult.settings_entry_sha256 ?? "Not available"],
    ["Result custody", nativeResult.custody_relative_path ?? "Not available"],
  ];
}

export function validationResultRows(payload) {
  const artifacts = payload?.artifacts ?? {};
  const result = payload?.result ?? null;
  const validation = payload?.validation ?? null;
  const outcomes = Array.isArray(validation?.outcomes) ? validation.outcomes : [];
  return [
    ["Lifecycle status", payload?.status ?? "Not available"],
    ["Terminal", terminalText(payload?.terminal)],
    ["Result schema", result?.result_schema ?? "None"],
    ...nativeResultRows(result),
    ["Validation decision", decisionText(validation?.passed)],
    ["Validated gates", String(outcomes.length)],
    ["Result", artifacts.result_ref ?? "None"],
    ["Decision", artifacts.decision_ref ?? "None"],
    ["Evidence", artifacts.evidence_manifest_ref ?? "None"],
    ["Validation plan", artifacts.plan_ref ?? "None"],
  ];
}

export function validationGateRows(payload) {
  const outcomes = Array.isArray(payload?.validation?.outcomes)
    ? payload.validation.outcomes
    : [];
  if (outcomes.length === 0) return [["Gate outcomes", "None"]];
  return outcomes.map((outcome) => [
    `Gate · ${outcome.metric_path ?? "Unknown metric"}`,
    `${outcome.actual ?? "Not available"} ${outcome.operator ?? "?"} ${outcome.threshold ?? "Not available"} · ${outcome.passed === true ? "Passed" : outcome.passed === false ? "Failed" : "Not available"}`,
  ]);
}

export function validationResultIdentityRows(payload) {
  const artifacts = payload?.artifacts ?? {};
  return [
    ["Run reference", payload?.run_ref ?? "Not available"],
    ["Invocation", payload?.invocation_id ?? "Not available"],
    ["Occurred at", payload?.occurred_at ?? "Not available"],
    ["Reason", payload?.reason_code ?? "None"],
    ["Receipt", artifacts.receipt_ref ?? "None"],
    ["Lifecycle event", payload?.lifecycle_event_ref ?? "Not available"],
  ];
}

function makeField(label, value) {
  const row = document.createElement("div");
  row.className = "run-field";
  const name = document.createElement("span");
  name.textContent = label;
  const content = document.createElement("strong");
  content.textContent = value;
  row.append(name, content);
  return row;
}

function renderFieldRows(target, rows) {
  target.className = "run-fields";
  target.removeAttribute("data-capability");
  target.replaceChildren(...rows.map(([label, value]) => makeField(label, value)));
}

function renderRows(target, payload) {
  renderFieldRows(target, runReadRows(payload));
}

function renderError(target, detail, label = "Exact run lookup") {
  renderFieldRows(target, [[label, detail || "Request failed"]]);
}

async function fetchRunRead(runRef, invocationId) {
  const response = await fetch(runReadRequestPath(runRef, invocationId), {
    headers: { accept: "application/json" },
    method: "GET",
  });
  const payload = await response.json();
  if (!response.ok) {
    const error = new Error(payload?.detail || `Run lookup failed (${response.status})`);
    error.status = response.status;
    throw error;
  }
  return payload;
}

function clearRunActions(surface) {
  const actions = surface.querySelector("[data-run-read-actions]");
  if (!actions) return;
  actions.replaceChildren();
  actions.hidden = true;
}

function actionLink(path, label) {
  const link = document.createElement("a");
  link.className = "button button-secondary";
  link.href = path;
  link.dataset.route = path;
  link.textContent = label;
  return link;
}

function showRunActions(surface, payload) {
  if (typeof window === "undefined") return;
  const actions = surface.querySelector("[data-run-read-actions]");
  if (!actions) return;
  const runRef = payload.run_ref;
  const invocationId = payload.invocation_id;
  const operate = window.location.pathname === "/operate/runs";
  const peerPath = operate ? "/validate/run" : "/operate/runs";
  const peerLabel = operate
    ? "Open exact invocation in Test & Validate"
    : "Open exact invocation in Operate";
  const links = [
    actionLink(
      runReadContextPath(peerPath, runRef, invocationId, window.location.search),
      peerLabel,
    ),
  ];
  const artifacts = payload?.artifacts ?? {};
  if (artifacts.result_ref || artifacts.decision_ref || artifacts.evidence_manifest_ref) {
    const governed = Boolean(artifacts.decision_ref || artifacts.evidence_manifest_ref);
    links.push(
      actionLink(
        runReadContextPath("/validate/results", runRef, invocationId, window.location.search),
        governed ? "Open verified results" : "Open durable result",
      ),
    );
  }
  actions.replaceChildren(...links);
  actions.hidden = false;
}

function syncRunContext(runRef, invocationId) {
  if (typeof window === "undefined" || typeof window.history?.replaceState !== "function") return;
  const path = runReadContextPath(
    window.location.pathname,
    runRef,
    invocationId,
    window.location.search,
  );
  if (`${window.location.pathname}${window.location.search}` !== path) {
    window.history.replaceState(window.history.state, "", path);
  }
}

function buildLookup(surface) {
  if (surface.dataset.runReadEnhanced === "true") return;
  surface.dataset.runReadEnhanced = "true";

  const block = document.createElement("div");
  block.dataset.runReadBlock = "true";
  block.innerHTML = `
    <div class="select-divider"></div>
    <form class="strategy-form" data-run-read-form>
      <label>Exact run reference
        <input name="runRef" type="text" autocomplete="off" required placeholder="tc:backtest-run:v1:sha256:…" />
      </label>
      <label>Invocation ID
        <input name="invocationId" type="text" autocomplete="off" required placeholder="initial-001" />
      </label>
      <div class="form-row">
        <button class="button button-primary" type="submit">Load exact run state</button>
      </div>
      <p class="field-help">Read-only exact lookup. No run listing, launch, cancellation, or inferred status.</p>
    </form>
    <div class="run-fields" data-run-read-result aria-live="polite"></div>
    <div class="detail-actions" data-run-read-actions hidden></div>`;

  const footer = surface.querySelector(".run-footer");
  if (footer) footer.before(block);
  else surface.append(block);

  const result = block.querySelector("[data-run-read-result]");
  renderError(result, "Enter an exact run reference and invocation ID.");

  if (typeof window !== "undefined") {
    const context = runReadContext(window.location.search);
    if (context) {
      const form = block.querySelector("[data-run-read-form]");
      form.elements.namedItem("runRef").value = context.runRef;
      form.elements.namedItem("invocationId").value = context.invocationId;
      void submitRunRead(form, false);
    }
  }
}

function enhanceRunSurfaces() {
  document.querySelectorAll("[data-run-surface-id]").forEach(buildLookup);
}

async function submitRunRead(form, updateLocation = true) {
  const surface = form.closest("[data-run-surface-id]");
  const result = surface?.querySelector("[data-run-read-result]");
  if (!surface || !result) return;

  const data = new FormData(form);
  const runRef = data.get("runRef")?.toString() ?? "";
  const invocationId = data.get("invocationId")?.toString() ?? "";
  if (runRef.length === 0 || invocationId.length === 0) return;

  const button = form.querySelector('button[type="submit"]');
  if (button) button.disabled = true;
  clearRunActions(surface);
  renderError(result, "Loading exact run state…");

  try {
    const payload = await fetchRunRead(runRef, invocationId);
    renderRows(result, payload);
    surface.dataset.runReadStatus = payload.status || "loaded";
    if (updateLocation) syncRunContext(payload.run_ref, payload.invocation_id);
    showRunActions(surface, payload);
    const footerMessage = surface.querySelector(".run-refusal");
    if (footerMessage) {
      footerMessage.textContent = "Exact run state loaded read-only. Start and cancel integration remain pending.";
    }
  } catch (error) {
    renderError(result, error?.message || "Exact run lookup could not reach the backend boundary.");
    surface.dataset.runReadStatus = "error";
  } finally {
    if (button) button.disabled = false;
  }
}

function enhanceValidationResults() {
  if (typeof window === "undefined" || window.location.pathname !== "/validate/results") return;
  const shell = document.querySelector('.app-shell[data-state-key="validate.results"]');
  if (!shell || shell.dataset.runResultsEnhanced === "true") return;
  const context = runReadContext(window.location.search);
  if (!context) return;

  const panels = shell.querySelectorAll(".dashboard-grid.two-up .panel");
  if (panels.length < 2) return;
  const resultTarget = panels[0].querySelector(".empty-state");
  const identityTarget = panels[1].querySelector(".empty-state");
  if (!resultTarget || !identityTarget) return;

  shell.dataset.runResultsEnhanced = "true";
  renderError(resultTarget, "Loading durable result chain…", "Durable results");
  renderError(identityTarget, "Loading exact run identity…", "Exact run context");

  void (async () => {
    try {
      const payload = await fetchRunRead(context.runRef, context.invocationId);
      renderFieldRows(
        resultTarget,
        [...validationResultRows(payload), ...validationGateRows(payload)],
      );
      renderFieldRows(identityTarget, validationResultIdentityRows(payload));
      shell.dataset.runResultsStatus = payload.status || "loaded";
    } catch (error) {
      renderError(
        resultTarget,
        error?.message || "Durable result lookup could not reach the backend boundary.",
        "Durable results",
      );
      renderError(identityTarget, "Run identity was not accepted by the backend reader.", "Exact run context");
      shell.dataset.runResultsStatus = "error";
    }
  })();
}

function enhanceReadSurfaces() {
  enhanceRunSurfaces();
  enhanceValidationResults();
}

if (typeof document !== "undefined") {
  document.addEventListener("submit", (event) => {
    const form = event.target.closest?.("[data-run-read-form]");
    if (!form) return;
    event.preventDefault();
    void submitRunRead(form);
  });

  const root = document.querySelector("#app");
  if (root) {
    enhanceReadSurfaces();
    new MutationObserver(enhanceReadSurfaces).observe(root, { childList: true, subtree: true });
  }
}
