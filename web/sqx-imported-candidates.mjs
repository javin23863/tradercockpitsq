import { startSqxNativeRun } from "./sqx-outputs.mjs";

const SQX_IMPORTED_CANDIDATES_API_PATH = "/api/sqx-imported-candidates";
const SQX_IMPORTED_CANDIDATE_LIST_SCHEMA = "tc.sqx-imported-candidate-list.v1";

export function normalizeImportedSqxCandidates(payload) {
  if (
    !payload
    || typeof payload !== "object"
    || payload.schema !== SQX_IMPORTED_CANDIDATE_LIST_SCHEMA
    || !Array.isArray(payload.candidates)
  ) {
    throw new Error("Unexpected imported SQX candidate catalog schema");
  }
  const seen = new Set();
  const candidates = payload.candidates.map((candidate) => {
    if (!candidate || typeof candidate !== "object") {
      throw new Error("Invalid imported SQX candidate record");
    }
    for (const key of [
      "candidate_ref",
      "strategy_ref",
      "archive_sha256",
      "custody_relative_path",
      "semantic_schema",
    ]) {
      if (typeof candidate[key] !== "string" || candidate[key].length === 0) {
        throw new Error(`Imported SQX candidate is missing ${key}`);
      }
    }
    if (!candidate.candidate_ref.startsWith("tc:candidate:v1:sha256:")) {
      throw new Error("Imported SQX candidate has invalid candidate identity");
    }
    if (seen.has(candidate.candidate_ref)) {
      throw new Error("Duplicate imported SQX candidate identity");
    }
    seen.add(candidate.candidate_ref);
    if (candidate.run_binding?.available !== true) {
      throw new Error("Imported SQX candidate is missing native Retester binding");
    }
    return candidate;
  });
  return { schema: payload.schema, candidates };
}

export async function fetchImportedSqxCandidates(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Fetch is not available");
  const response = await fetchImpl(SQX_IMPORTED_CANDIDATES_API_PATH, {
    headers: { accept: "application/json" },
  });
  const payload = await response.json();
  if (!response.ok) {
    throw new Error(payload?.detail || `Imported candidate lookup failed (${response.status})`);
  }
  return normalizeImportedSqxCandidates(payload);
}

function text(tag, value, className = "") {
  const node = document.createElement(tag);
  if (className) node.className = className;
  node.textContent = value;
  return node;
}

function field(label, value) {
  const row = document.createElement("div");
  row.className = "run-field";
  row.append(text("span", label), text("strong", value));
  return row;
}

function renderCandidateCard(candidate) {
  const card = document.createElement("article");
  card.className = "panel";
  card.dataset.accent = "cyan";
  card.dataset.importedSqxCandidate = candidate.candidate_ref;

  const heading = document.createElement("div");
  heading.className = "panel-heading";
  const copy = document.createElement("div");
  copy.append(text("p", "Durable imported candidate", "eyebrow"));
  copy.append(text("h2", candidate.candidate_ref));
  heading.append(copy);
  card.append(heading);

  const fields = document.createElement("div");
  fields.className = "run-fields";
  fields.append(
    field("Strategy", candidate.strategy_ref),
    field("Archive SHA-256", candidate.archive_sha256),
    field("Native version", String(candidate.native_version || "Unavailable")),
    field("Custody path", candidate.custody_relative_path),
  );
  card.append(fields);

  const actions = document.createElement("div");
  actions.className = "detail-actions";
  const strategy = document.createElement("a");
  strategy.className = "button button-secondary";
  strategy.href = `/strategies/${encodeURIComponent(candidate.strategy_ref)}/overview`;
  strategy.dataset.route = strategy.href;
  strategy.textContent = "Open strategy custody";
  actions.append(strategy);

  const run = document.createElement("button");
  run.type = "button";
  run.className = "button button-primary";
  run.dataset.importedSqxRun = candidate.candidate_ref;
  run.textContent = "Run native Retest";
  actions.append(run);
  card.append(actions);

  const status = text(
    "p",
    candidate.run_binding?.detail || "Candidate is eligible for native Retester execution.",
    "field-help",
  );
  status.dataset.importedSqxRunState = candidate.candidate_ref;
  card.append(status);
  return card;
}

function renderCatalog(root, catalog) {
  root.querySelector("[data-imported-sqx-candidates-panel]")?.remove();
  if (catalog.candidates.length === 0) return;

  const runSurface = root.querySelector('[data-run-surface-id="shared-run-surface"]');
  if (!runSurface) return;

  const panel = document.createElement("section");
  panel.className = "panel";
  panel.dataset.accent = "purple";
  panel.dataset.importedSqxCandidatesPanel = "true";

  const heading = document.createElement("div");
  heading.className = "panel-heading";
  const copy = document.createElement("div");
  copy.append(text("p", "TraderCockpit custody", "eyebrow"));
  copy.append(text("h2", "Imported native candidates"));
  heading.append(copy, text("span", `${catalog.candidates.length} durable candidates`, "status-badge status-pending"));
  panel.append(heading);
  panel.append(text(
    "p",
    "These candidates are read from TraderCockpit's immutable custody, not from the live SQX Builder Results databank. They remain discoverable after Builder moves or clears the source archive.",
    "panel-description",
  ));

  const grid = document.createElement("div");
  grid.className = "dashboard-grid two-up";
  for (const candidate of catalog.candidates) grid.append(renderCandidateCard(candidate));
  panel.append(grid);
  runSurface.before(panel);
}

async function runCandidate(button) {
  const candidateRef = button.dataset.importedSqxRun;
  const card = button.closest("[data-imported-sqx-candidate]");
  const state = card?.querySelector("[data-imported-sqx-run-state]");
  if (!candidateRef || !card) return;
  button.disabled = true;
  button.textContent = "Running Retester…";
  if (state) state.textContent = "Executing the exact durably custodied candidate through native Retester…";
  try {
    const receipt = await startSqxNativeRun(candidateRef);
    const params = new URLSearchParams();
    params.set("runRef", receipt.run_ref);
    params.set("invocationId", receipt.invocation_id);
    globalThis.location.assign(`/validate/results?${params.toString()}`);
  } catch (error) {
    button.disabled = false;
    button.textContent = "Run native Retest";
    if (state) state.textContent = error?.message || "Native SQX Retester execution failed.";
  }
}

export function bootImportedSqxCandidates(root = document.querySelector("#app")) {
  if (!root || typeof MutationObserver === "undefined") return;
  let hydratedRoute = "";
  let requestSerial = 0;

  const hydrate = () => {
    const runSurface = root.querySelector('[data-run-surface-id="shared-run-surface"]');
    if (!runSurface) {
      hydratedRoute = "";
      root.querySelector("[data-imported-sqx-candidates-panel]")?.remove();
      return;
    }
    const routeKey = `${globalThis.location?.pathname || ""}${globalThis.location?.search || ""}`;
    if (hydratedRoute === routeKey) return;
    hydratedRoute = routeKey;
    const serial = ++requestSerial;
    fetchImportedSqxCandidates()
      .then((catalog) => {
        if (serial === requestSerial) renderCatalog(root, catalog);
      })
      .catch(() => {
        if (serial === requestSerial) root.querySelector("[data-imported-sqx-candidates-panel]")?.remove();
      });
  };

  const observer = new MutationObserver(hydrate);
  observer.observe(root, { childList: true, subtree: true });
  root.addEventListener("click", (event) => {
    const button = event.target.closest?.("[data-imported-sqx-run]");
    if (!button || button.matches(":disabled")) return;
    event.preventDefault();
    void runCandidate(button);
  });
  hydrate();
}

if (typeof document !== "undefined") bootImportedSqxCandidates();
