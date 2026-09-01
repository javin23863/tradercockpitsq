const BUILDER_CONFIG_API_PATH = "/api/sqx-builder-config";
const BUILDER_CONFIG_SCHEMA = "tc.sqx-builder-config.v1";
const SPECIFICATION_SCHEMA = "tc.research-specification.v1";

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function isSpecificationRoute(locationLike = globalThis.location) {
  if (locationLike?.pathname !== "/research") return false;
  const params = new URLSearchParams(locationLike.search || "");
  return params.get("stage") === "construct" && params.get("tab") === "specification";
}

export function producerValidityFromBuilderConfig(payload) {
  if (!payload || payload.schema !== BUILDER_CONFIG_SCHEMA) {
    throw new Error("Builder configuration schema mismatch for producer validity");
  }
  const specification = payload.specification;
  const validity = specification?.producer_validity;
  const gate = specification?.build_gate;
  if (
    !specification
    || specification.schema !== SPECIFICATION_SCHEMA
    || !validity
    || typeof validity !== "object"
    || !gate
    || typeof gate !== "object"
    || typeof gate.locked !== "boolean"
  ) {
    throw new Error("Specification producer validity contract is missing");
  }
  if (
    validity.method !== "authorized_sqx_loadconfig"
    || validity.native_execution_check !== "loadconfig_before_start"
  ) {
    throw new Error("Specification producer validation authority changed");
  }
  if (validity.state === "pending_native_validation") {
    if (validity.local_preflight !== "requirements_complete" || gate.locked !== false) {
      throw new Error("Pending native validation contradicts local Specification readiness");
    }
  } else if (validity.state === "not_ready_for_native_validation") {
    if (validity.local_preflight !== "requirements_incomplete" || gate.locked !== true) {
      throw new Error("Native validation refusal contradicts the local Specification gate");
    }
  } else {
    throw new Error("Unknown Specification producer validity state");
  }
  return Object.freeze({
    state: validity.state,
    method: validity.method,
    nativeExecutionCheck: validity.native_execution_check,
    localPreflight: validity.local_preflight,
    localGateLocked: gate.locked,
  });
}

export function renderProducerValidity(validity) {
  if (!validity || typeof validity !== "object") throw new Error("Producer validity is required");
  const pending = validity.state === "pending_native_validation";
  const label = pending ? "Native validation pending" : "Not ready for native validation";
  const detail = pending
    ? "Local Builder requirements are complete, but StrategyQuant X has not yet accepted these exact bytes through the authorized loadconfig check. Producer validity remains pending until native execution performs loadconfig before start."
    : "Local Builder requirements are incomplete, so the exact configuration is not eligible for the authorized StrategyQuant X loadconfig validation step.";
  return `<div class="requirement-item" data-specification-producer-validity="${escapeHtml(validity.state)}"><div><strong>Producer validity</strong><span class="status-badge status-unavailable"><span class="status-dot"></span>${escapeHtml(label)}</span></div><div class="stat-row"><span>Local preflight</span><code>${escapeHtml(validity.localPreflight)}</code></div><div class="stat-row"><span>Native authority</span><code>${escapeHtml(validity.method)}</code></div><div class="stat-row"><span>Execution check</span><code>${escapeHtml(validity.nativeExecutionCheck)}</code></div><p>${escapeHtml(detail)}</p></div>`;
}

export function applyProducerValidity(grid, validity) {
  const gate = grid?.querySelector?.(".specification-gate");
  if (!gate) throw new Error("Specification local gate is unavailable");
  const pending = validity.state === "pending_native_validation";
  const heading = gate.querySelector("strong");
  const badge = gate.querySelector(".status-badge");
  if (!heading || !badge) throw new Error("Specification local gate markup changed");
  heading.textContent = "Local build preflight";
  badge.classList.toggle("status-ready", pending);
  badge.classList.toggle("status-unavailable", !pending);
  badge.innerHTML = `<span class="status-dot"></span>${pending ? "Local requirements complete" : "Local requirements incomplete"}`;
  gate.dataset.producerValidityState = validity.state;
  grid.querySelector("[data-specification-producer-validity]")?.remove();
  gate.insertAdjacentHTML("afterend", renderProducerValidity(validity));
  return gate;
}

async function fetchProducerValidity(fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(BUILDER_CONFIG_API_PATH, { headers: { accept: "application/json" } });
  let payload = null;
  try { payload = await response.json(); } catch { payload = null; }
  if (!response?.ok) throw new Error(payload?.detail || "Producer validity read failed");
  return producerValidityFromBuilderConfig(payload);
}

let generation = 0;
let activeGate = null;

async function bindProducerValidity() {
  if (!isSpecificationRoute()) return;
  const grid = globalThis.document?.querySelector?.(".requirement-grid");
  const gate = grid?.querySelector?.(".specification-gate");
  if (!grid || !gate || gate === activeGate) return;
  const current = ++generation;
  activeGate = gate;
  gate.dataset.producerValidityState = "loading";
  try {
    const validity = await fetchProducerValidity();
    if (current !== generation || !isSpecificationRoute() || !gate.isConnected) return;
    applyProducerValidity(grid, validity);
  } catch (error) {
    if (current !== generation || !isSpecificationRoute() || !gate.isConnected) return;
    const detail = error instanceof Error ? error.message : "Producer validity unavailable";
    const heading = gate.querySelector("strong");
    const badge = gate.querySelector(".status-badge");
    if (heading) heading.textContent = "Local build preflight";
    if (badge) {
      badge.classList.remove("status-ready");
      badge.classList.add("status-unavailable");
      badge.innerHTML = '<span class="status-dot"></span>Producer validity unavailable';
    }
    gate.dataset.producerValidityState = "failed";
    grid.querySelector("[data-specification-producer-validity]")?.remove();
    gate.insertAdjacentHTML("afterend", `<div class="requirement-item" data-specification-producer-validity="failed"><div><strong>Producer validity</strong><span class="status-badge status-unavailable"><span class="status-dot"></span>Unavailable</span></div><p>${escapeHtml(detail)}</p></div>`);
  }
}

if (typeof document !== "undefined") {
  const observer = new MutationObserver(() => {
    if (!isSpecificationRoute()) {
      generation += 1;
      activeGate = null;
      return;
    }
    void bindProducerValidity();
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void bindProducerValidity();
}
