const DATA_CONTEXTS_API_PATH = "/api/data-contexts";

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function contextRefPattern(kind) {
  return new RegExp(`^tc:${kind}:v1:sha256:[0-9a-f]{64}$`);
}

export function validateDataContextRecord(value) {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    throw new Error("Data/trading context response must be an object");
  }
  if (value.schema !== "tc.data-trading-context.v1") {
    throw new Error("Unexpected data/trading context schema");
  }
  if (!contextRefPattern("data-trading-context").test(value.context_ref ?? "")) {
    throw new Error("Invalid data/trading context identity");
  }
  if (!value.data || !contextRefPattern("data").test(value.data.ref ?? "")) {
    throw new Error("Invalid canonical data identity");
  }
  if (!value.execution || !contextRefPattern("execution").test(value.execution.ref ?? "")) {
    throw new Error("Invalid canonical execution identity");
  }
  if (
    value.authority?.market_and_dataset_identity !== "user-supplied" ||
    value.authority?.execution_assumptions !== "tradercockpit-owned" ||
    value.authority?.native_sqx_binding !== false
  ) {
    throw new Error("Invalid data/trading context authority boundary");
  }
  return value;
}

function renderContext(context) {
  const validated = validateDataContextRecord(context);
  return `
    <article class="run-field" data-data-context-ref="${escapeHtml(validated.context_ref)}">
      <span>${escapeHtml(validated.data.symbol)} · ${escapeHtml(validated.data.timeframe)}</span>
      <strong>${escapeHtml(validated.data.source)} / ${escapeHtml(validated.data.dataset_revision)}</strong>
      <small>${escapeHtml(validated.data.start)} → ${escapeHtml(validated.data.end)}</small>
      <small>Data ref: ${escapeHtml(validated.data.ref)}</small>
      <small>Execution ref: ${escapeHtml(validated.execution.ref)}</small>
      <small>Context ref: ${escapeHtml(validated.context_ref)}</small>
    </article>`;
}

function renderContextList(contexts) {
  if (!Array.isArray(contexts) || contexts.length === 0) {
    return `<div class="empty-state"><div class="empty-icon">—</div><div><strong>No saved research contexts</strong><p>Create one from explicit dataset identity and research execution assumptions. No market availability is inferred.</p></div></div>`;
  }
  return `<div class="run-fields">${contexts.map(renderContext).join("")}</div>`;
}

async function readJson(response) {
  let payload;
  try {
    payload = await response.json();
  } catch {
    throw new Error("Data/trading context response was not valid JSON");
  }
  if (!response.ok) {
    throw new Error(payload?.detail || payload?.error || `Request failed (${response.status})`);
  }
  return payload;
}

export async function loadDataContexts(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Fetch implementation is unavailable");
  const payload = await readJson(await fetchImpl(DATA_CONTEXTS_API_PATH, { headers: { accept: "application/json" } }));
  if (payload?.schema !== "tc.data-trading-context-list.v1" || !Array.isArray(payload.contexts)) {
    throw new Error("Unexpected data/trading context list schema");
  }
  return payload.contexts.map(validateDataContextRecord);
}

export async function createDataContext(request, fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Fetch implementation is unavailable");
  return validateDataContextRecord(
    await readJson(
      await fetchImpl(DATA_CONTEXTS_API_PATH, {
        method: "POST",
        headers: {
          accept: "application/json",
          "content-type": "application/json",
        },
        body: JSON.stringify(request),
      }),
    ),
  );
}

function formRequest(form) {
  const fields = new FormData(form);
  const names = [
    "symbol",
    "timeframe",
    "source",
    "datasetRevision",
    "timezone",
    "sessionCalendar",
    "start",
    "end",
    "adjustmentPolicy",
    "startingCash",
    "currency",
    "fillModel",
  ];
  return Object.fromEntries(names.map((name) => [name, fields.get(name)?.toString() ?? ""]));
}

export function renderDataContextAuthority() {
  return `
    <section class="dashboard-grid two-up" data-data-context-authority>
      <article class="panel" data-accent="cyan">
        <div class="panel-heading">
          <div><p class="eyebrow">Research run context</p><h2>Bind exact data assumptions</h2></div>
          <span class="status-badge status-pending" data-data-context-status><span class="status-dot"></span>Ready to configure</span>
        </div>
        <p class="panel-description">Create canonical DataSpec and ExecutionSpec identities for TraderCockpit research. Symbol, source, dataset revision, session and dates are explicit user-supplied identity—not claims about provider availability.</p>
        <form class="strategy-form" data-data-context-form>
          <div class="run-fields">
            <label class="run-field"><span>Symbol</span><input name="symbol" required autocomplete="off" placeholder="ES" /></label>
            <label class="run-field"><span>Timeframe</span><input name="timeframe" required autocomplete="off" placeholder="1m" /></label>
            <label class="run-field"><span>Source token</span><input name="source" required autocomplete="off" placeholder="local-fixture" /></label>
            <label class="run-field"><span>Dataset revision</span><input name="datasetRevision" required autocomplete="off" placeholder="rev-2026-08-31" /></label>
            <label class="run-field"><span>Timezone</span><input name="timezone" required autocomplete="off" placeholder="America/Chicago" /></label>
            <label class="run-field"><span>Session calendar</span><input name="sessionCalendar" required autocomplete="off" placeholder="CME" /></label>
            <label class="run-field"><span>Start (ISO-8601 with offset)</span><input name="start" required autocomplete="off" placeholder="2026-01-01T00:00:00Z" /></label>
            <label class="run-field"><span>End (ISO-8601 with offset)</span><input name="end" required autocomplete="off" placeholder="2026-02-01T00:00:00Z" /></label>
            <label class="run-field"><span>Adjustment policy</span><input name="adjustmentPolicy" value="none" required autocomplete="off" /></label>
            <label class="run-field"><span>Starting cash</span><input name="startingCash" value="100000" required inputmode="decimal" /></label>
            <label class="run-field"><span>Currency</span><input name="currency" value="USD" required autocomplete="off" /></label>
            <label class="run-field"><span>Fill model</span><input name="fillModel" value="bar-close" required autocomplete="off" /></label>
          </div>
          <div class="run-footer">
            <span class="run-refusal">Defaults are TraderCockpit research assumptions. They are not native SQX settings or live-provider facts.</span>
            <button class="button button-primary" type="submit">Save research context</button>
          </div>
        </form>
        <p class="field-help" data-data-context-message aria-live="polite"></p>
      </article>
      <article class="panel" data-accent="purple">
        <div class="panel-heading">
          <div><p class="eyebrow">Canonical custody</p><h2>Saved contexts</h2></div>
          <span class="status-badge status-pending"><span class="status-dot"></span>Read-only identities</span>
        </div>
        <p class="panel-description">These records reopen exact DataSpec/ExecutionSpec pairs. Market coverage and provider availability remain separate producer-owned facts.</p>
        <div data-data-context-list>${renderContextList([])}</div>
      </article>
    </section>`;
}

export function installDataContextAuthority(root, fetchImpl = globalThis.fetch) {
  const authority = root?.querySelector?.("[data-data-context-authority]");
  if (!authority) return null;

  const list = authority.querySelector("[data-data-context-list]");
  const status = authority.querySelector("[data-data-context-status]");
  const message = authority.querySelector("[data-data-context-message]");
  const form = authority.querySelector("[data-data-context-form]");

  const setState = (label, detail = "") => {
    if (status) status.lastChild.textContent = label;
    if (message) message.textContent = detail;
  };

  const refresh = async () => {
    setState("Loading contexts");
    try {
      const contexts = await loadDataContexts(fetchImpl);
      if (list) list.innerHTML = renderContextList(contexts);
      setState("Context custody ready", `${contexts.length} saved context${contexts.length === 1 ? "" : "s"}.`);
      return contexts;
    } catch (error) {
      if (list) list.innerHTML = `<div class="empty-state"><div class="empty-icon">—</div><div><strong>Context data not available to this frontend</strong><p>${escapeHtml(error.message)}</p></div></div>`;
      setState("Context data pending", error.message);
      return [];
    }
  };

  form?.addEventListener("submit", async (event) => {
    event.preventDefault();
    setState("Saving context");
    try {
      const created = await createDataContext(formRequest(form), fetchImpl);
      setState("Context saved", `Saved ${created.context_ref}`);
      await refresh();
    } catch (error) {
      setState("Save refused", error.message);
    }
  });

  void refresh();
  return { refresh };
}

function ensureDataContextSurface(root = document.querySelector("#app"), fetchImpl = globalThis.fetch) {
  if (!root || window.location.pathname !== "/explore/data") return;
  if (root.querySelector("[data-data-context-authority]")) return;

  const content = root.querySelector(".content-inner");
  if (!content) return;

  const template = document.createElement("template");
  template.innerHTML = renderDataContextAuthority();
  const authority = template.content.firstElementChild;
  if (!authority) throw new Error("Data/trading context authority failed to render");

  const intro = content.querySelector(".page-intro");
  if (intro) intro.after(authority);
  else content.prepend(authority);
  installDataContextAuthority(root, fetchImpl);
}

export function bootDataContextIntegration(
  root = document.querySelector("#app"),
  fetchImpl = globalThis.fetch,
) {
  if (!root || typeof MutationObserver === "undefined") return null;

  const hydrate = () => ensureDataContextSurface(root, fetchImpl);
  const observer = new MutationObserver(hydrate);
  observer.observe(root, { childList: true, subtree: true });
  hydrate();
  return observer;
}

if (typeof document !== "undefined") bootDataContextIntegration();
