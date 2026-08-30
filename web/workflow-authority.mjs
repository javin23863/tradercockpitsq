const WORKFLOW_LIST_PATH = "/api/workflows/runs/list";
const WORKFLOW_START_PATH = "/api/workflows/runs";
const WORKFLOW_IMPORT_PATH = "/api/workflows/import-sqx";

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function runCard(run) {
  const name = run?.plan?.name || "Workflow";
  const status = run?.status || "unknown";
  const steps = Number.isInteger(run?.step_count) ? run.step_count : "—";
  const ref = typeof run?.run_ref === "string" ? run.run_ref : "invalid-ref";
  return `<article class="route-card" data-workflow-run-ref="${escapeHtml(ref)}">
    <span class="route-card-eyebrow">${escapeHtml(status)}</span>
    <span class="route-card-title">${escapeHtml(name)}</span>
    <span class="route-card-description">${escapeHtml(steps)} executed steps · ${escapeHtml(ref)}</span>
  </article>`;
}

function parseObject(text, label, { allowEmpty = false } = {}) {
  const raw = String(text ?? "").trim();
  if (!raw && allowEmpty) return {};
  if (!raw) throw new Error(`${label} is required`);
  let value;
  try {
    value = JSON.parse(raw);
  } catch {
    throw new Error(`${label} must be valid JSON`);
  }
  if (!value || Array.isArray(value) || typeof value !== "object") {
    throw new Error(`${label} must be a JSON object`);
  }
  return value;
}

async function jsonRequest(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: { "content-type": "application/json", ...(options.headers || {}) },
  });
  let payload;
  try {
    payload = await response.json();
  } catch {
    throw new Error(`Workflow API returned non-JSON response (${response.status})`);
  }
  if (!response.ok) {
    throw new Error(payload?.detail || payload?.error || `Workflow API failed (${response.status})`);
  }
  return payload;
}

export function renderWorkflowAuthority() {
  return `<section data-workflow-authority>
    <div class="page-intro">
      <div><p class="eyebrow">Automation</p><h1>Workflow Automation</h1><p class="lede">Run bounded task graphs against registered TraderCockpit capabilities. Workflow state and output custody are durable; missing capability handlers fail visibly instead of being simulated.</p></div>
    </div>
    <section class="dashboard-grid two-up">
      <article class="panel" data-accent="purple">
        <div class="panel-heading"><div><p class="eyebrow">Plan</p><h2>Run a TraderCockpit workflow</h2></div></div>
        <p class="panel-description">Paste a canonical workflow-plan object or import a retained SQX Custom Project below. Action names dispatch to real registered product handlers.</p>
        <form data-workflow-run-form>
          <label for="workflow-run-key">Run key</label>
          <input id="workflow-run-key" name="runKey" type="text" autocomplete="off" placeholder="Unique run key" required />
          <label for="workflow-plan-json">Workflow plan JSON</label>
          <textarea id="workflow-plan-json" name="plan" rows="12" placeholder='{"schema":"tc.workflow-plan.v1", ...}' required></textarea>
          <label for="workflow-inputs-json">Inputs JSON</label>
          <textarea id="workflow-inputs-json" name="inputs" rows="4" placeholder="{}">{}</textarea>
          <div class="detail-actions"><button class="button button-primary" type="submit">Run workflow</button></div>
        </form>
      </article>
      <article class="panel" data-accent="cyan">
        <div class="panel-heading"><div><p class="eyebrow">SQX compatibility</p><h2>Import Custom Project</h2></div></div>
        <p class="panel-description">Import source-visible numbered topology. GoTo labels require explicit canonical task mapping; hidden label resolution is never guessed.</p>
        <form data-workflow-import-form>
          <label for="workflow-project">SQX project</label>
          <input id="workflow-project" name="project" type="text" autocomplete="off" placeholder="Saved project name" required />
          <label for="workflow-goto-json">GoTo label mapping JSON</label>
          <textarea id="workflow-goto-json" name="gotoTargets" rows="5" placeholder='{"Loop label":"task-1"}'>{}</textarea>
          <div class="detail-actions"><button class="button button-secondary" type="submit">Import into plan editor</button></div>
        </form>
      </article>
    </section>
    <section class="panel" data-accent="orange">
      <div class="panel-heading"><div><p class="eyebrow">Durable runs</p><h2>Automation history</h2></div><button class="button button-quiet" type="button" data-workflow-refresh>Refresh</button></div>
      <div data-workflow-message aria-live="polite"></div>
      <div class="route-card-grid" data-workflow-runs><div class="empty-state"><div class="empty-icon">—</div><div><strong>Loading workflow runs</strong><p>Reading durable workflow state.</p></div></div></div>
    </section>
  </section>`;
}

export function installWorkflowAuthority(root = document) {
  const surface = root.querySelector?.("[data-workflow-authority]");
  if (!surface || surface.dataset.workflowInstalled === "true") return;
  surface.dataset.workflowInstalled = "true";

  const runsNode = surface.querySelector("[data-workflow-runs]");
  const messageNode = surface.querySelector("[data-workflow-message]");
  const planNode = surface.querySelector("[name='plan']");

  const message = (text, tone = "") => {
    if (!messageNode) return;
    messageNode.textContent = text;
    messageNode.dataset.tone = tone;
  };

  const refresh = async () => {
    try {
      const payload = await jsonRequest(WORKFLOW_LIST_PATH, { method: "GET" });
      if (payload?.schema !== "tc.workflow-run-list.v1" || !Array.isArray(payload.runs)) {
        throw new Error("Workflow list contract is invalid");
      }
      if (runsNode) {
        runsNode.innerHTML = payload.runs.length
          ? payload.runs.map(runCard).join("")
          : '<div class="empty-state"><div class="empty-icon">—</div><div><strong>No workflow runs yet</strong><p>Import or enter a plan and start the first automation run.</p></div></div>';
      }
      message(`Loaded ${payload.runs.length} durable workflow run${payload.runs.length === 1 ? "" : "s"}.`);
    } catch (error) {
      if (runsNode) runsNode.innerHTML = "";
      message(error.message || String(error), "error");
    }
  };

  surface.querySelector("[data-workflow-refresh]")?.addEventListener("click", refresh);

  surface.querySelector("[data-workflow-import-form]")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    try {
      const project = String(data.get("project") || "").trim();
      if (!project) throw new Error("SQX project is required");
      const gotoTargets = parseObject(data.get("gotoTargets"), "GoTo mapping", { allowEmpty: true });
      const payload = await jsonRequest(WORKFLOW_IMPORT_PATH, {
        method: "POST",
        body: JSON.stringify({ project, gotoTargets }),
      });
      if (payload?.schema !== "tc.workflow-import.v1" || !payload.plan || typeof payload.plan_ref !== "string") {
        throw new Error("Workflow import contract is invalid");
      }
      planNode.value = JSON.stringify(payload.plan, null, 2);
      message(`Imported ${project} as ${payload.plan_ref}. Review the plan and run it explicitly.`);
    } catch (error) {
      message(error.message || String(error), "error");
    }
  });

  surface.querySelector("[data-workflow-run-form]")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    try {
      const runKey = String(data.get("runKey") || "").trim();
      if (!runKey) throw new Error("Run key is required");
      const plan = parseObject(data.get("plan"), "Workflow plan");
      const inputs = parseObject(data.get("inputs"), "Workflow inputs", { allowEmpty: true });
      const payload = await jsonRequest(WORKFLOW_START_PATH, {
        method: "POST",
        body: JSON.stringify({ plan, runKey, inputs }),
      });
      if (payload?.schema !== "tc.workflow-run.v1" || typeof payload.run_ref !== "string") {
        throw new Error("Workflow run contract is invalid");
      }
      message(`Workflow ${payload.status}: ${payload.run_ref}.`);
      await refresh();
    } catch (error) {
      message(error.message || String(error), "error");
    }
  });

  refresh();
}
