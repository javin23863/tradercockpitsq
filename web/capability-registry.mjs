// Typed add-on registry. The backend is the only catalog. The browser renders escaped
// title/detail into registered slots and never rewrites top-level navigation.

import { APP_SURFACES } from "./model.mjs";
import { escapeHtml, readable, unavailable } from "./ui.mjs";

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
  "config_schema",
  "read_schema",
  "action_schema",
  "presentation",
]);
const SLOT_KEYS = Object.freeze(["id", "surface", "kind", "label"]);

function object(value) {
  return value && typeof value === "object" && !Array.isArray(value) ? value : null;
}

function exactKeys(record, allowed) {
  const keys = Object.keys(record);
  return keys.length === allowed.length && keys.every((key) => allowed.includes(key));
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
  return record;
}

export function addonFromPayload(value, slotIds = REGISTERED_SLOT_IDS) {
  const addon = object(value);
  if (
    !addon
    || !exactKeys(addon, ADDON_KEYS)
    || addon.schema !== CAPABILITY_ADDON_SCHEMA
    || addon.descriptor_version !== ADDON_DESCRIPTOR_VERSION
    || typeof addon.id !== "string"
    || typeof addon.version !== "string"
    || (addon.producer !== "operator" && addon.producer !== "platform")
    || (addon.availability !== "ready" && addon.availability !== "unavailable")
    || !slotIds.includes(addon.slot)
    || addon.config_schema !== NONE_SCHEMA
    || addon.read_schema !== NONE_SCHEMA
    || addon.action_schema !== null
    || !object(addon.presentation)
    || typeof addon.presentation.title !== "string"
    || typeof addon.presentation.detail !== "string"
  ) {
    throw new Error("Add-on descriptor is invalid");
  }
  if (Object.keys(addon.presentation).some((key) => key !== "title" && key !== "detail")) {
    throw new Error("Add-on presentation is invalid");
  }
  if (/[<>]/.test(`${addon.presentation.title}${addon.presentation.detail}${addon.id}${addon.version}`)) {
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

export function renderCapabilitySlot(registry, slotId) {
  const manifest = capabilityRegistryFromPayload(registry);
  const slot = manifest.slots.find((item) => item.id === slotId);
  if (!slot) throw new Error("Capability slot is not registered");
  const bound = manifest.addons.filter((addon) => addon.slot === slotId);
  const refusedHere = manifest.refused;
  const rows = bound.length
    ? bound.map((addon) => `<div class="requirement-item" data-capability-addon="${escapeHtml(addon.id)}"><div><strong>${escapeHtml(addon.presentation.title)}</strong><span class="status-badge status-${addon.availability === "ready" ? "ready" : "unavailable"}"><span class="status-dot"></span>${escapeHtml(readable(addon.availability))}</span></div><p>${escapeHtml(addon.presentation.detail)}</p><div class="stat-row"><span>Identity</span><code>${escapeHtml(addon.id)}</code></div><div class="stat-row"><span>Producer</span><code>${escapeHtml(addon.producer)}</code></div></div>`).join("")
    : unavailable("No add-ons in this slot", `Typed slot ${slotId} is registered. Nothing is bound. Add-ons cannot invent a placement or rewrite navigation.`, { compact: true });
  const refused = refusedHere.length
    ? `<p class="note">${escapeHtml(String(refusedHere.length))} descriptor${refusedHere.length === 1 ? "" : "s"} failed closed and ${refusedHere.length === 1 ? "was" : "were"} not bound.</p>`
    : "";
  return `<section data-capability-slot-body="${escapeHtml(slotId)}"><p class="note">Top-level surfaces stay ${escapeHtml(PLATFORM_SURFACE_IDS.join(" · "))}. Add-ons cannot add a surface, inject script/HTML, or replace Research stages.</p>${rows}${refused}</section>`;
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
      try {
        host.setAttribute("data-capability-registry-state", "ready");
        host.innerHTML = renderCapabilitySlot(registry, slotId);
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
      host.innerHTML = unavailable("Typed add-on registry unavailable", detail, { tone: "error", compact: true });
    }
  }
}

if (typeof document !== "undefined") {
  const observer = new MutationObserver(() => {
    if (registryHosts().length) void paintRegistry();
    else generation += 1;
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void paintRegistry();
}
