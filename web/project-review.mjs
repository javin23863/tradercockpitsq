import { escapeHtml } from "./ui.mjs";

const schema = "tc.research-project-review.v1";
const previews = new WeakMap();

export function renderProjectReview(project, databank) {
  if (!databank) return "";
  return `<details class="workspace-disclosure project-review" data-project-review data-project="${escapeHtml(project)}" data-databank="${escapeHtml(databank)}">
    <summary>Graph &amp; input review <span>${escapeHtml(databank)} · saved files</span></summary>
    <div class="project-review-body">
    <p class="note">Review the exact saved graph and this bank’s input inventory before preparing tracked execution. Saving a review does not start or authorize a run.</p>
    <div class="idea-actions"><button class="button button-small" type="button" data-project-review-action="preview">Review current files</button><button class="button button-small" type="button" data-project-review-action="list">Open saved reviews</button><button class="button button-small button-primary" type="button" data-project-review-action="retain" disabled>Save exact review</button></div>
    <p role="status" aria-live="polite" data-project-review-status></p><div data-project-review-content></div>
    </div></details>`;
}

export async function requestProjectReview(action, project, databank, expected, fetchImpl = globalThis.fetch) {
  const response = await fetchImpl("/api/sqx-project-review", { method: "POST",
    headers: { "content-type": "application/json", accept: "application/json" },
    body: JSON.stringify({ action, project, databank, ...(action === "retain" ? { expected_review_sha256: expected } : {}) }) });
  const payload = await response.json();
  if (!response.ok) throw new Error(payload.detail || "Project review unavailable.");
  const rows = action === "list" ? payload.reviews : [payload];
  if (payload.schema !== schema || !Array.isArray(rows) || (action === "list" && (payload.project !== project || payload.databank !== databank))
    || rows.some(row => row.schema !== schema || !/^[a-f0-9]{64}$/.test(row.review_sha256 || "")
      || row.snapshot?.project !== project || row.snapshot?.databank !== databank || row.snapshot?.launch_authorized !== false
      || !Array.isArray(row.snapshot.tasks) || !Array.isArray(row.snapshot.inputs) || !Array.isArray(row.snapshot.gaps))) {
    throw new Error("Review response does not match the selected project and bank.");
  }
  return payload;
}

export function renderReviewSnapshot(row) {
  const snapshot = row.snapshot;
  return `<section class="project-review-snapshot"><h3>${row.reviewed_at_utc ? "Saved review" : "Current saved files"}</h3>
    <p class="note">${snapshot.tasks.length} native tasks · ${snapshot.inputs.length} saved inputs · ${snapshot.inputs.filter(input => input.binding === "exact").length} exact Candidate bindings${row.reviewed_at_utc ? ` · ${escapeHtml(row.reviewed_at_utc)}` : ""}</p>
    <details class="workspace-disclosure"><summary>Native graph <span>Saved task identities</span></summary><ol>${snapshot.tasks.map(task => `<li>${escapeHtml(task.title)} <span class="note">${escapeHtml(task.kind)} · ${escapeHtml(task.entry)}${task.active === false ? " · inactive" : ""}</span></li>`).join("")}</ol></details>
    <div class="project-review-table" tabindex="0" role="region" aria-label="Reviewed input archives"><table><thead><tr><th>Input archive</th><th>Candidate binding</th><th>SHA-256</th></tr></thead><tbody>${snapshot.inputs.map(input => `<tr><td>${escapeHtml(input.archive)}</td><td>${escapeHtml(input.binding)}</td><td><code>${escapeHtml(input.archive_sha256)}</code></td></tr>`).join("") || '<tr><td colspan="3">No saved strategy files in this bank.</td></tr>'}</tbody></table></div>
    <p class="note">Tracked execution unavailable</p><ul>${snapshot.gaps.map(gap => `<li>${escapeHtml(gap)}</li>`).join("")}</ul>
    <details class="workspace-disclosure"><summary>Exact custody</summary><p>Graph SHA-256 <code>${escapeHtml(snapshot.graph_sha256)}</code></p><p>Launcher SHA-256 <code>${escapeHtml(snapshot.launcher_sha256)}</code></p><p>Review SHA-256 <code>${escapeHtml(row.review_sha256)}</code></p></details>
  </section>`;
}

if (typeof document !== "undefined") document.addEventListener("click", async event => {
  const button = event.target.closest?.("[data-project-review-action]");
  const host = button?.closest("[data-project-review]");
  if (!host || button.disabled || host.dataset.busy === "true") return;
  const action = button.dataset.projectReviewAction;
  const status = host.querySelector("[data-project-review-status]");
  const content = host.querySelector("[data-project-review-content]");
  const save = host.querySelector('[data-project-review-action="retain"]');
  const expected = previews.get(host)?.review_sha256;
  const restoreFocus = document.activeElement === button;
  if (action === "retain" && !expected) return;
  host.dataset.busy = "true";
  host.setAttribute("aria-busy", "true");
  host.querySelectorAll("button").forEach(item => { item.disabled = true; });
  status.textContent = action === "retain" ? "Verifying and retaining this exact review…" : "Reading project custody…";
  try {
    const payload = await requestProjectReview(action, host.dataset.project, host.dataset.databank, expected);
    if (!host.isConnected) return;
    previews.delete(host);
    if (action === "preview") previews.set(host, payload);
    content.innerHTML = action === "list" ? payload.reviews.map(renderReviewSnapshot).join("") || '<p class="note">No saved reviews for this project and bank.</p>' : renderReviewSnapshot(payload);
    status.textContent = action === "retain" ? "Exact review saved. No execution was authorized." : action === "list" ? "Saved reviews reopened from custody." : "Current files reviewed. Save to retain this exact graph and inventory.";
  } catch (error) {
    if (!host.isConnected) return;
    previews.delete(host);
    status.textContent = error instanceof Error ? error.message : "Project review unavailable.";
  } finally {
    if (host.isConnected) {
      host.dataset.busy = "false";
      host.setAttribute("aria-busy", "false");
      host.querySelectorAll("button").forEach(item => { item.disabled = false; });
      save.disabled = !previews.has(host);
      if (restoreFocus && (document.activeElement === document.body || document.activeElement === button)) {
        (button.disabled ? host.querySelector('[data-project-review-action="preview"]') : button).focus();
      }
    }
  }
});
