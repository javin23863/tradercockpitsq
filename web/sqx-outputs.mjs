const SQX_OUTPUTS_API_PATH = "/api/sqx-outputs";
const SQX_OUTPUT_LIST_SCHEMA = "tc.sqx-builder-output-list.v1";
const SQX_OUTPUT_IMPORT_SCHEMA = "tc.sqx-builder-output-import.v1";

export function normalizeSqxOutputs(payload) {
  if (!payload || typeof payload !== "object" || payload.schema !== SQX_OUTPUT_LIST_SCHEMA) {
    throw new Error("Unexpected SQX output catalog schema");
  }
  if (!payload.runtime || typeof payload.runtime !== "object" || !Array.isArray(payload.outputs)) {
    throw new Error("Invalid SQX output catalog payload");
  }
  const seen = new Set();
  const outputs = payload.outputs.map((output) => {
    if (!output || typeof output !== "object") throw new Error("Invalid SQX output record");
    if (typeof output.archive !== "string" || !output.archive.endsWith(".sqx")) {
      throw new Error("SQX output is missing a native archive name");
    }
    if (seen.has(output.archive)) throw new Error("Duplicate SQX output archive");
    seen.add(output.archive);
    if (output.importable === true) {
      for (const key of ["archive_sha256", "native_version", "strategy_entry_sha256", "settings_entry_sha256"]) {
        if (typeof output[key] !== "string" || output[key].length === 0) {
          throw new Error(`Importable SQX output is missing ${key}`);
        }
      }
    }
    return output;
  });
  return {
    schema: payload.schema,
    sqx_build: String(payload.sqx_build || ""),
    project: String(payload.project || ""),
    databank: String(payload.databank || ""),
    runtime: payload.runtime,
    outputs,
  };
}

export function sqxOutputImportPath(archive) {
  const value = String(archive ?? "");
  if (!value || value.includes("/") || value.includes("\\") || !value.toLowerCase().endsWith(".sqx")) {
    throw new Error("Invalid SQX output archive");
  }
  const params = new URLSearchParams();
  params.set("archive", value);
  return `${SQX_OUTPUTS_API_PATH}/import?${params.toString()}`;
}

export async function fetchSqxOutputs(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Fetch is not available");
  const response = await fetchImpl(SQX_OUTPUTS_API_PATH, { headers: { accept: "application/json" } });
  if (!response.ok) throw new Error(`SQX output lookup failed (${response.status})`);
  return normalizeSqxOutputs(await response.json());
}

export async function importSqxOutput(archive, fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Fetch is not available");
  const response = await fetchImpl(sqxOutputImportPath(archive), {
    method: "POST",
    headers: { accept: "application/json" },
    body: "",
  });
  const payload = await response.json();
  if (!response.ok) throw new Error(payload?.detail || `SQX output import failed (${response.status})`);
  if (
    payload?.schema !== SQX_OUTPUT_IMPORT_SCHEMA
    || payload?.archive?.archive !== archive
    || typeof payload?.strategy_ref !== "string"
    || typeof payload?.candidate_ref !== "string"
    || payload?.custody !== "persisted"
  ) {
    throw new Error("SQX output import returned an unexpected custody receipt");
  }
  return payload;
}

function makeText(tag, text, className = "") {
  const node = document.createElement(tag);
  if (className) node.className = className;
  node.textContent = text;
  return node;
}

function makeField(label, value) {
  const row = document.createElement("div");
  row.className = "run-field";
  row.append(makeText("span", label), makeText("strong", value));
  return row;
}

function renderRuntimeUnavailable(panel, catalog) {
  panel.replaceChildren();
  const heading = document.createElement("div");
  heading.className = "panel-heading";
  const copy = document.createElement("div");
  copy.append(makeText("p", "SQX Builder outputs", "eyebrow"));
  copy.append(makeText("h2", "Native strategy custody"));
  heading.append(copy, makeText("span", "Producer unavailable", "status-badge status-pending"));
  panel.append(heading);
  panel.append(makeText(
    "p",
    catalog.runtime?.detail || "The configured SQX Builder Results databank is not available.",
    "panel-description",
  ));
  panel.dataset.sqxOutputCatalogState = "unavailable";
}

function renderCustodyReceipt(card, receipt) {
  const existing = card.querySelector("[data-sqx-custody-receipt]");
  if (existing) existing.remove();

  const block = document.createElement("div");
  block.className = "run-fields";
  block.dataset.sqxCustodyReceipt = receipt.archive.archive;
  block.append(
    makeField("Strategy", receipt.strategy_ref),
    makeField("Candidate", receipt.candidate_ref),
    makeField("Semantic schema", receipt.semantic_schema),
    makeField("Run binding", receipt.run_binding?.available === true ? "Available" : "Not yet bound"),
  );

  const actions = document.createElement("div");
  actions.className = "detail-actions";
  const strategyLink = document.createElement("a");
  strategyLink.className = "button button-secondary";
  strategyLink.href = `/strategies/${encodeURIComponent(receipt.strategy_ref)}/overview`;
  strategyLink.dataset.route = strategyLink.href;
  strategyLink.textContent = "Open strategy custody";
  actions.append(strategyLink);
  block.append(actions);

  const reason = makeText(
    "p",
    receipt.run_binding?.detail || "Run launch remains unavailable until an evaluator binds this exact candidate.",
    "field-help",
  );
  block.append(reason);
  card.append(block);
}

async function submitImport(button) {
  const archive = button.dataset.sqxOutputImport;
  const card = button.closest("[data-sqx-output-card]");
  const state = card?.querySelector("[data-sqx-output-state]");
  if (!archive || !card) return;
  button.disabled = true;
  if (state) state.textContent = "Persisting exact SQX strategy and candidate custody…";
  try {
    const receipt = await importSqxOutput(archive);
    card.dataset.sqxCustodyStatus = "persisted";
    button.textContent = "In custody";
    button.className = "button button-secondary";
    if (state) state.textContent = "Native SQX archive is now bound to immutable TraderCockpit strategy and candidate identities.";
    renderCustodyReceipt(card, receipt);
  } catch (error) {
    card.dataset.sqxCustodyStatus = "error";
    button.disabled = false;
    if (state) state.textContent = error?.message || "SQX output custody failed.";
  }
}

function renderCatalog(panel, catalog) {
  if (catalog.runtime?.ready !== true) {
    renderRuntimeUnavailable(panel, catalog);
    return;
  }

  panel.replaceChildren();
  const heading = document.createElement("div");
  heading.className = "panel-heading";
  const copy = document.createElement("div");
  copy.append(makeText("p", "SQX Builder outputs", "eyebrow"));
  copy.append(makeText("h2", "Native strategy custody"));
  heading.append(copy, makeText("span", `${catalog.outputs.length} native outputs`, "status-badge status-pending"));
  panel.append(heading);
  panel.append(makeText(
    "p",
    "These are native .sqx files from Builder / Results. Import hashes the SQX archive and its strategy/settings members, then creates immutable TraderCockpit strategy and candidate identities. It does not infer an executable run.",
    "panel-description",
  ));

  if (catalog.outputs.length === 0) {
    panel.append(makeText("p", "SQX Builder has not published any .sqx files to its Results databank yet.", "field-help"));
    panel.dataset.sqxOutputCatalogState = "empty";
    return;
  }

  const grid = document.createElement("div");
  grid.className = "dashboard-grid two-up";
  for (const output of catalog.outputs) {
    const card = document.createElement("article");
    card.className = "panel";
    card.dataset.accent = output.importable === true ? "cyan" : "red";
    card.dataset.sqxOutputCard = output.archive;

    const cardHeading = document.createElement("div");
    cardHeading.className = "panel-heading";
    const cardCopy = document.createElement("div");
    cardCopy.append(makeText("p", "Native SQX archive", "eyebrow"));
    cardCopy.append(makeText("h2", output.archive));
    cardHeading.append(cardCopy);
    card.append(cardHeading);

    const fields = document.createElement("div");
    fields.className = "run-fields";
    fields.append(makeField("Native version", output.native_version || "Unavailable"));
    fields.append(makeField("Archive SHA-256", output.archive_sha256 || "Unavailable"));
    fields.append(makeField("Strategy SHA-256", output.strategy_entry_sha256 || "Unavailable"));
    fields.append(makeField("Bytes", String(output.bytes ?? "Unavailable")));
    card.append(fields);

    const actions = document.createElement("div");
    actions.className = "detail-actions";
    const button = document.createElement("button");
    button.type = "button";
    button.dataset.sqxOutputImport = output.archive;
    button.textContent = "Import to TraderCockpit";
    button.className = output.importable === true ? "button button-primary" : "button button-disabled";
    button.disabled = output.importable !== true;
    if (button.disabled) button.title = output.detail || "This native SQX archive failed structural verification";
    actions.append(button);
    card.append(actions);

    const state = makeText(
      "p",
      output.importable === true
        ? "Ready for immutable custody. Run launch remains a separate producer-binding step."
        : (output.detail || "Native SQX output cannot be imported."),
      "field-help",
    );
    state.dataset.sqxOutputState = output.archive;
    card.append(state);
    grid.append(card);
  }
  panel.append(grid);
  panel.dataset.sqxOutputCatalogState = "ready";
}

function renderError(panel, error) {
  panel.replaceChildren();
  panel.append(makeText("p", "SQX Builder outputs", "eyebrow"));
  panel.append(makeText("h2", "Native output lookup failed"));
  panel.append(makeText("p", error instanceof Error ? error.message : String(error), "panel-description"));
  panel.dataset.sqxOutputCatalogState = "error";
}

function ensureOutputPanel(root = document) {
  const runSurface = root.querySelector('[data-run-surface-id="shared-run-surface"]');
  if (!runSurface) return;

  let panel = root.querySelector("[data-sqx-output-panel]");
  if (!panel) {
    panel = document.createElement("section");
    panel.className = "panel";
    panel.dataset.accent = "cyan";
    panel.dataset.sqxOutputPanel = "true";
    panel.dataset.sqxOutputCatalogState = "loading";
    panel.append(makeText("p", "Reading native SQX Builder outputs…", "panel-description"));
    runSurface.before(panel);
  }

  if (panel.dataset.sqxOutputCatalogState !== "loading") return;
  fetchSqxOutputs()
    .then((catalog) => renderCatalog(panel, catalog))
    .catch((error) => renderError(panel, error));
}

export function bootSqxOutputIntegration(root = document.querySelector("#app")) {
  if (!root || typeof MutationObserver === "undefined") return;
  const hydrate = () => ensureOutputPanel(root);
  const observer = new MutationObserver(hydrate);
  observer.observe(root, { childList: true, subtree: true });
  root.addEventListener("click", (event) => {
    const button = event.target.closest?.("[data-sqx-output-import]");
    if (!button || button.matches(":disabled")) return;
    event.preventDefault();
    void submitImport(button);
  });
  hydrate();
}

if (typeof document !== "undefined") bootSqxOutputIntegration();
