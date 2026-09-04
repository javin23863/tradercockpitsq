// Typed add-on registry. The backend is the only catalog. The browser paints
// packaged native SQX plugins into registered slots and never rewrites navigation.

import { APP_SURFACES } from "./model.mjs";
import { actionButton, escapeHtml, readable, unavailable } from "./ui.mjs";

export const CAPABILITY_REGISTRY_SCHEMA = "tc.capability-addon-registry.v1";
export const CAPABILITY_ADDON_SCHEMA = "tc.capability-addon.v1";
export const CAPABILITY_REGISTRY_API_PATH = "/api/capabilities";
export const ADDON_DESCRIPTOR_VERSION = 1;
export const NONE_SCHEMA = "tc.capability-addon.none.v1";
export const REGISTERED_SLOT_IDS = Object.freeze([
  "explore.extensions",
  "automation.extensions",
  "settings.extensions",
]);
export const CAPABILITY_VIEWS = Object.freeze(["catalog", "results", "install"]);

const PLATFORM_SURFACE_IDS = Object.freeze(APP_SURFACES.map((surface) => surface.id));
const REGISTRY_KEYS = Object.freeze([
  "schema",
  "status",
  "reason_code",
  "detail",
  "nav_authority",
  "surfaces",
  "slots",
  "addons",
  "refused",
  "addon_count",
  "refused_count",
]);
const ADDON_KEYS = Object.freeze([
  "schema",
  "descriptor_version",
  "id",
  "version",
  "producer",
  "availability",
  "slot",
  "kind",
  "package",
  "native_placement",
  "source_url",
  "runtime",
  "config_schema",
  "read_schema",
  "action_schema",
  "presentation",
]);
const SLOT_KEYS = Object.freeze(["id", "surface", "kind", "label"]);
const PRESENTATION_KEYS = Object.freeze(["title", "detail", "job", "opens_in", "controls"]);
const CONTROL_KEYS = Object.freeze(["label", "detail"]);
const RUNTIME_KEYS = Object.freeze(["status", "installed", "stageable", "detail"]);
const PRODUCERS = Object.freeze(["operator", "platform", "native_sqx"]);
const KINDS = Object.freeze(["results_plugin", "authoring_skill", "sxp_extension", "operator"]);
const RUNTIME_STATUSES = Object.freeze(["packaged", "installed", "runtime_not_configured", "unavailable"]);

function object(value) {
  return value && typeof value === "object" && !Array.isArray(value) ? value : null;
}

function exactKeys(record, allowed) {
  const keys = Object.keys(record);
  return keys.length === allowed.length && keys.every((key) => allowed.includes(key));
}

function markupFree(...parts) {
  return !parts.some((part) => typeof part === "string" && /[<>]/.test(part));
}

function runtimeBadge(runtime) {
  if (runtime.installed) return { label: "Installed in SQX", tone: "ready" };
  if (runtime.status === "packaged") return { label: "Packaged", tone: "ready" };
  if (runtime.status === "runtime_not_configured") return { label: "Needs SQX runtime", tone: "pending" };
  return { label: readable(runtime.status), tone: "unavailable" };
}

function kindLabel(kind) {
  if (kind === "results_plugin") return "SQX Results plugin";
  if (kind === "sxp_extension") return "SQX Results extension";
  if (kind === "authoring_skill") return "Native authoring";
  return "Operator add-on";
}

export function capabilityRegistryFromPayload(payload) {
  const record = object(payload);
  if (
    !record
    || !exactKeys(record, REGISTRY_KEYS)
    || record.schema !== CAPABILITY_REGISTRY_SCHEMA
    || record.nav_authority !== "platform"
    || !["ready", "unavailable"].includes(record.status)
    || (record.reason_code !== null && (typeof record.reason_code !== "string" || !record.reason_code))
    || typeof record.detail !== "string"
    || !Array.isArray(record.surfaces)
    || record.surfaces.length !== PLATFORM_SURFACE_IDS.length
    || record.surfaces.some((id, index) => id !== PLATFORM_SURFACE_IDS[index])
    || !Array.isArray(record.slots)
    || record.slots.length !== REGISTERED_SLOT_IDS.length
    || !Array.isArray(record.addons)
    || !Array.isArray(record.refused)
    || record.addon_count !== record.addons.length
    || record.refused_count !== record.refused.length
  ) {
    throw new Error("Capability registry schema mismatch");
  }

  const slotIds = [];
  for (const slot of record.slots) {
    if (
      !object(slot)
      || !exactKeys(slot, SLOT_KEYS)
      || typeof slot.id !== "string"
      || !REGISTERED_SLOT_IDS.includes(slot.id)
      || slotIds.includes(slot.id)
      || typeof slot.surface !== "string"
      || !PLATFORM_SURFACE_IDS.includes(slot.surface)
      || slot.kind !== "status_card"
      || typeof slot.label !== "string"
    ) {
      throw new Error("Capability registry slot is not registered");
    }
    slotIds.push(slot.id);
  }
  if (REGISTERED_SLOT_IDS.some((id) => !slotIds.includes(id))) {
    throw new Error("Capability registry is missing a registered slot");
  }

  const addons = record.addons.map((item) => addonFromPayload(item, slotIds));
  const seen = new Set();
  for (const addon of addons) {
    if (seen.has(addon.id)) throw new Error("Capability registry add-on identity is duplicated");
    seen.add(addon.id);
  }
  for (const item of record.refused) {
    if (
      !object(item)
      || typeof item.source !== "string"
      || typeof item.reason_code !== "string"
      || typeof item.detail !== "string"
    ) {
      throw new Error("Capability registry refusal is invalid");
    }
  }
  return { ...record, addons };
}

export function addonFromPayload(value, slotIds = REGISTERED_SLOT_IDS) {
  const addon = object(value);
  const presentation = object(addon?.presentation);
  const runtime = object(addon?.runtime);
  const controls = Array.isArray(presentation?.controls) ? presentation.controls : null;
  if (
    !addon
    || !exactKeys(addon, ADDON_KEYS)
    || addon.schema !== CAPABILITY_ADDON_SCHEMA
    || addon.descriptor_version !== ADDON_DESCRIPTOR_VERSION
    || typeof addon.id !== "string"
    || typeof addon.version !== "string"
    || !PRODUCERS.includes(addon.producer)
    || (addon.availability !== "ready" && addon.availability !== "unavailable")
    || !slotIds.includes(addon.slot)
    || !KINDS.includes(addon.kind)
    || (addon.package !== null && typeof addon.package !== "string")
    || (addon.native_placement !== null && typeof addon.native_placement !== "string")
    || (addon.source_url !== null && typeof addon.source_url !== "string")
    || !runtime
    || !exactKeys(runtime, RUNTIME_KEYS)
    || !RUNTIME_STATUSES.includes(runtime.status)
    || typeof runtime.installed !== "boolean"
    || typeof runtime.stageable !== "boolean"
    || typeof runtime.detail !== "string"
    || addon.config_schema !== NONE_SCHEMA
    || addon.read_schema !== NONE_SCHEMA
    || addon.action_schema !== null
    || !presentation
    || !exactKeys(presentation, PRESENTATION_KEYS)
    || typeof presentation.title !== "string"
    || typeof presentation.detail !== "string"
    || typeof presentation.job !== "string"
    || typeof presentation.opens_in !== "string"
    || !controls
    || controls.length > 6
  ) {
    throw new Error("Add-on descriptor is invalid");
  }
  for (const control of controls) {
    if (
      !object(control)
      || !exactKeys(control, CONTROL_KEYS)
      || typeof control.label !== "string"
      || typeof control.detail !== "string"
      || !markupFree(control.label, control.detail)
    ) {
      throw new Error("Add-on presentation is invalid");
    }
  }
  if (!markupFree(
    addon.presentation.title,
    addon.presentation.detail,
    addon.presentation.job,
    addon.presentation.opens_in,
    addon.id,
    addon.version,
  )) {
    throw new Error("Add-on markup is refused");
  }
  return addon;
}

export async function fetchCapabilityRegistry(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("capability registry fetch is unavailable");
  const response = await fetchImpl(CAPABILITY_REGISTRY_API_PATH, { headers: { accept: "application/json" } });
  if (!response?.ok) throw new Error(`capability registry request failed: ${response?.status ?? "unknown"}`);
  return capabilityRegistryFromPayload(await response.json());
}

export function selectCapabilityAddons(addons, view, slotId) {
  if (view === "catalog" || view === "install") return addons;
  if (view === "results") {
    return addons.filter((addon) => addon.kind === "results_plugin" || addon.kind === "sxp_extension");
  }
  return addons.filter((addon) => addon.slot === slotId);
}

function renderControls(controls) {
  if (!controls.length) return "";
  const rows = controls.map((control) => `<div class="plugin-control"><strong>${escapeHtml(control.label)}</strong><span>${escapeHtml(control.detail)}</span></div>`).join("");
  return `<div class="plugin-controls"><span class="plugin-controls-label">Adjust in StrategyQuant X</span>${rows}</div>`;
}

function renderStageAction(addon) {
  if (addon.kind === "authoring_skill") {
    return `<p class="note">Packaged for native authoring. It is not a Results tab.</p>`;
  }
  if (addon.runtime.installed) {
    return actionButton("Installed in SQX", {
      disabled: true,
      title: addon.runtime.detail,
    });
  }
  if (addon.runtime.stageable) {
    return actionButton("Install into SQX", {
      primary: true,
      attrs: `data-capability-stage="${escapeHtml(addon.id)}"`,
      title: "Copies this plugin into the authorized StrategyQuant X runtime. Settings stay in SQX Results.",
    });
  }
  return actionButton("Install into SQX", {
    disabled: true,
    attrs: `data-capability-stage="${escapeHtml(addon.id)}"`,
    title: addon.runtime.detail || "Install requires a verified StrategyQuant X runtime. The browser cannot choose this path.",
  });
}

function renderPluginCard(addon) {
  const badge = runtimeBadge(addon.runtime);
  const job = addon.presentation.job || addon.presentation.detail;
  const opens = addon.presentation.opens_in
    ? `<p class="plugin-opens">Opens in ${escapeHtml(addon.presentation.opens_in)}</p>`
    : "";
  return `<article class="plugin-card" data-capability-addon="${escapeHtml(addon.id)}" data-capability-kind="${escapeHtml(addon.kind)}"><header class="plugin-card-head"><div><span class="plugin-kind">${escapeHtml(kindLabel(addon.kind))}</span><h3>${escapeHtml(addon.presentation.title)}</h3></div><span class="status-badge status-${badge.tone}"><span class="status-dot"></span>${escapeHtml(badge.label)}</span></header><p class="plugin-job">${escapeHtml(job)}</p>${opens}${renderControls(addon.presentation.controls)}<footer class="plugin-card-foot">${renderStageAction(addon)}</footer></article>`;
}

function renderInstallRow(addon) {
  const badge = runtimeBadge(addon.runtime);
  return `<div class="plugin-install-row" data-capability-addon="${escapeHtml(addon.id)}"><div><strong>${escapeHtml(addon.presentation.title)}</strong><p>${escapeHtml(addon.runtime.detail || addon.presentation.job || addon.presentation.detail)}</p></div><div class="plugin-install-actions"><span class="status-badge status-${badge.tone}"><span class="status-dot"></span>${escapeHtml(badge.label)}</span>${renderStageAction(addon)}</div></div>`;
}

export function renderCapabilitySlot(registry, slotId, view = "slot") {
  const manifest = capabilityRegistryFromPayload(registry);
  const slot = manifest.slots.find((item) => item.id === slotId);
  if (!slot) throw new Error("Capability slot is not registered");
  const bound = selectCapabilityAddons(manifest.addons, view, slotId);
  const layout = view === "install" ? "install" : "catalog";
  const rows = bound.length
    ? (layout === "install"
      ? `<div class="plugin-install-list">${bound.map(renderInstallRow).join("")}</div>`
      : `<div class="plugin-shelf">${bound.map(renderPluginCard).join("")}</div>`)
    : unavailable(
      "No native plugins in this view",
      "Packaged StrategyQuant X plugins bind registered slots only. They cannot invent a placement or rewrite navigation.",
      { compact: true },
    );
  const refused = manifest.refused.length
    ? `<p class="note">${escapeHtml(String(manifest.refused.length))} descriptor${manifest.refused.length === 1 ? "" : "s"} failed closed and ${manifest.refused.length === 1 ? "was" : "were"} not bound.</p>`
    : "";
  return `<section data-capability-slot-body="${escapeHtml(slotId)}" data-capability-view-body="${escapeHtml(view)}">${rows}${refused}</section>`;
}

function registryHosts() {
  return [...document.querySelectorAll("[data-capability-registry][data-capability-slot]")];
}

let generation = 0;

async function paintRegistry() {
  const hosts = registryHosts();
  if (!hosts.length) return;
  if (hosts.every((host) => host.getAttribute("data-capability-registry-state"))) return;
  const current = ++generation;
  try {
    const registry = await fetchCapabilityRegistry();
    if (current !== generation) return;
    for (const host of hosts) {
      if (!host.isConnected) continue;
      const slotId = host.getAttribute("data-capability-slot") || "";
      const view = host.getAttribute("data-capability-view") || "slot";
      try {
        host.setAttribute("data-capability-registry-state", "ready");
        host.innerHTML = renderCapabilitySlot(registry, slotId, view);
      } catch {
        host.setAttribute("data-capability-registry-state", "unavailable");
        host.innerHTML = unavailable(
          "Typed add-on slot unavailable",
          "This placement is not a registered slot. Add-ons cannot rewrite navigation.",
          { tone: "error", compact: true },
        );
      }
    }
  } catch (error) {
    if (current !== generation) return;
    const detail = error instanceof Error ? error.message : "Capability registry unavailable";
    for (const host of hosts) {
      if (!host.isConnected) continue;
      host.setAttribute("data-capability-registry-state", "unavailable");
      host.innerHTML = unavailable("Native plugins unavailable", detail, { tone: "error", compact: true });
    }
  }
}

function resetRegistryPaint() {
  for (const host of registryHosts()) host.removeAttribute("data-capability-registry-state");
  generation += 1;
  void paintRegistry();
}

async function stagePlugin(pluginId) {
  const response = await fetch(CAPABILITY_REGISTRY_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({ action: "stage", id: pluginId }),
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(typeof payload.detail === "string" ? payload.detail : `Install failed (${response.status})`);
  }
}

if (typeof document !== "undefined") {
  document.addEventListener("click", (event) => {
    const button = event.target instanceof Element ? event.target.closest("[data-capability-stage]") : null;
    if (!(button instanceof HTMLButtonElement) || button.disabled) return;
    event.preventDefault();
    const pluginId = button.getAttribute("data-capability-stage") || "";
    if (!pluginId) return;
    button.disabled = true;
    void stagePlugin(pluginId)
      .catch((error) => {
        const detail = error instanceof Error ? error.message : "Install failed";
        button.title = detail;
      })
      .finally(() => {
        resetRegistryPaint();
      });
  });
  const observer = new MutationObserver(() => {
    if (registryHosts().length) void paintRegistry();
    else generation += 1;
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void paintRegistry();
}
