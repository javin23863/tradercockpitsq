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
    ["Market", marketText(detail.data)],
    ["Data source", detail.data?.source ?? "Not available"],
    ["Dataset revision", detail.data?.dataset_revision ?? "Not available"],
    ["Data", inputs.data_ref ?? "Not available"],
    ["Execution assumptions", executionText(detail.execution)],
    ["Execution models", executionModelsText(detail.execution)],
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

export function validationResultRows(payload) {
  const artifacts = payload?.artifacts ?? {};
  const result = payload?.result ?? null;
  const validation = payload?.validation ?? null;
  const outcomes = Array.isArray(validation?.outcomes) ? validation.outcomes : [];
  return [
    ["Lifecycle status", payload?.status ?? "Not available"],
    ["Terminal", terminalText(payload?.terminal)],
    ["Result schema", result?.result_schema ?? "None"],
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

export async function fetchRunRead(runRef, invocationId, fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Fetch is not available");
  const response = await fetchImpl(runReadRequestPath(runRef, invocationId), {
    headers: { accept: "application/json" },
    method: "GET",
  });
  const payload = await response.json();
  if (!response.ok) {
    throw new Error(payload?.detail || `Run lookup failed (${response.status})`);
  }
  return payload;
}
