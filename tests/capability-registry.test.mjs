import assert from "node:assert/strict";
import test from "node:test";

import { APP_SURFACES } from "../web/model.mjs";
import { renderSecondarySurface } from "../web/surfaces.mjs";
import {
  ADDON_DESCRIPTOR_VERSION,
  CAPABILITY_ADDON_SCHEMA,
  CAPABILITY_REGISTRY_API_PATH,
  CAPABILITY_REGISTRY_SCHEMA,
  NONE_SCHEMA,
  REGISTERED_SLOT_IDS,
  addonFromPayload,
  capabilityRegistryFromPayload,
  fetchCapabilityRegistry,
  renderCapabilitySlot,
} from "../web/capability-registry.mjs";

const SLOTS = Object.freeze([
  { id: "explore.extensions", surface: "explore", kind: "status_card", label: "Explore extensions" },
  { id: "automation.extensions", surface: "automation", kind: "status_card", label: "Automation extensions" },
  { id: "settings.extensions", surface: "settings", kind: "status_card", label: "Settings extensions" },
]);

function addon(overrides = {}) {
  return {
    schema: CAPABILITY_ADDON_SCHEMA,
    descriptor_version: ADDON_DESCRIPTOR_VERSION,
    id: "operator.watch-note",
    version: "1.0.0",
    producer: "operator",
    availability: "ready",
    slot: "explore.extensions",
    config_schema: NONE_SCHEMA,
    read_schema: NONE_SCHEMA,
    action_schema: null,
    presentation: { title: "Watch note", detail: "Operator reminder for Explore." },
    ...overrides,
  };
}

function registry(overrides = {}) {
  return {
    schema: CAPABILITY_REGISTRY_SCHEMA,
    status: "ready",
    reason_code: null,
    detail: "Typed add-on registry is ready. Add-ons bind registered slots only and cannot rewrite top-level navigation or inject script/HTML.",
    nav_authority: "platform",
    surfaces: APP_SURFACES.map((surface) => surface.id),
    slots: SLOTS.map((slot) => ({ ...slot })),
    addons: [],
    refused: [],
    addon_count: 0,
    refused_count: 0,
    ...overrides,
  };
}

test("empty registry paints registered slots without rewriting navigation", () => {
  const parsed = capabilityRegistryFromPayload(registry());
  assert.equal(parsed.nav_authority, "platform");
  assert.deepEqual(parsed.surfaces, ["home", "research", "explore", "automation", "operate", "settings"]);
  assert.deepEqual(parsed.slots.map((slot) => slot.id), REGISTERED_SLOT_IDS);
  assert.equal(APP_SURFACES.length, 6);
  const html = renderCapabilitySlot(parsed, "explore.extensions");
  assert.match(html, /No add-ons in this slot/);
  assert.match(html, /cannot invent a placement or rewrite navigation/);
  assert.match(html, /Top-level surfaces stay home · research · explore · automation · operate · settings/);
  assert.doesNotMatch(html, /data-route="\/addons"/);
});

test("parser refuses extra surfaces, navigation slots, and markup", () => {
  assert.throws(
    () => capabilityRegistryFromPayload(registry({ surfaces: [...APP_SURFACES.map((surface) => surface.id), "addons"] })),
    /schema mismatch/,
  );
  assert.throws(
    () => capabilityRegistryFromPayload(registry({ nav_authority: "addon" })),
    /schema mismatch/,
  );
  const navSlot = registry({
    slots: [
      { id: "explore.extensions", surface: "explore", kind: "navigation", label: "Explore extensions" },
      SLOTS[1],
      SLOTS[2],
    ],
  });
  assert.throws(() => capabilityRegistryFromPayload(navSlot), /slot is not registered/);
  assert.throws(
    () => addonFromPayload(addon({ presentation: { title: "Watch", detail: "<img src=x>" } })),
    /markup is refused/,
  );
  assert.throws(
    () => addonFromPayload(addon({ producer: "native_sqx" })),
    /invalid/,
  );
  assert.throws(
    () => addonFromPayload(addon({ action_schema: "tc.capability-addon.mutate.v1" })),
    /invalid/,
  );
  assert.throws(
    () => addonFromPayload(addon({ route: "/settings" })),
    /invalid/,
  );
});

test("slot renderer escapes add-on text and never uses raw HTML", () => {
  const bound = registry({
    addons: [addon({ presentation: { title: "Watch & hold", detail: "Keep the 'ES' watchlist honest." } })],
    addon_count: 1,
  });
  const html = renderCapabilitySlot(bound, "explore.extensions");
  assert.match(html, /data-capability-addon="operator\.watch-note"/);
  assert.match(html, /Watch &amp; hold/);
  assert.match(html, /Keep the &#039;ES&#039; watchlist honest\./);
  assert.doesNotMatch(html, /<script>/);
  assert.match(html, /<code>operator<\/code>/);
});

test("fetchCapabilityRegistry uses only the canonical registry path", async () => {
  const payload = registry({ addons: [addon()], addon_count: 1 });
  const parsed = await fetchCapabilityRegistry(async (path, options) => {
    assert.equal(path, CAPABILITY_REGISTRY_API_PATH);
    assert.equal(options.headers.accept, "application/json");
    return { ok: true, json: async () => payload };
  });
  assert.equal(parsed.addons[0].id, "operator.watch-note");
});

test("Explore Automation and Settings host registered slots without extra surfaces", () => {
  const states = {
    runtime: {
      extensions: { status: "ready", reason_code: null, nav_authority: "platform", slot_count: 3, addon_count: 0, refused_count: 0 },
    },
    quotes: null,
    statusState: { phase: "loaded" },
  };
  const explore = renderSecondarySurface({ surfaceId: "explore", label: "Explore" }, states);
  const automation = renderSecondarySurface({ surfaceId: "automation", label: "Automation" }, states);
  const settings = renderSecondarySurface({ surfaceId: "settings", label: "Settings" }, states);
  assert.match(explore, /data-capability-slot="explore\.extensions"/);
  assert.match(automation, /data-capability-slot="automation\.extensions"/);
  assert.match(settings, /data-capability-slot="settings\.extensions"/);
  for (const html of [explore, automation, settings]) {
    assert.match(html, /data-capability-registry/);
    assert.match(html, /Add-ons cannot rewrite top-level navigation/);
    assert.doesNotMatch(html, /data-route="\/addons"/);
  }
  assert.deepEqual(APP_SURFACES.map((surface) => surface.id), ["home", "research", "explore", "automation", "operate", "settings"]);
  assert.equal(APP_SURFACES.length, 6);
});
