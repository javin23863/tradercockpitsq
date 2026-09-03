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
  selectCapabilityAddons,
} from "../web/capability-registry.mjs";

const SLOTS = Object.freeze([
  { id: "explore.extensions", surface: "explore", kind: "status_card", label: "Explore extensions" },
  { id: "automation.extensions", surface: "automation", kind: "status_card", label: "Automation extensions" },
  { id: "settings.extensions", surface: "settings", kind: "status_card", label: "Settings extensions" },
]);

function presentation(overrides = {}) {
  return {
    title: "Watch note",
    detail: "Operator reminder for Explore.",
    job: "",
    opens_in: "",
    controls: [],
    ...overrides,
  };
}

function runtime(overrides = {}) {
  return {
    status: "packaged",
    installed: false,
    stageable: false,
    detail: "Operator add-on. No native Results install.",
    ...overrides,
  };
}

function addon(overrides = {}) {
  return {
    schema: CAPABILITY_ADDON_SCHEMA,
    descriptor_version: ADDON_DESCRIPTOR_VERSION,
    id: "operator.watch-note",
    version: "1.0.0",
    producer: "operator",
    availability: "ready",
    slot: "explore.extensions",
    kind: "operator",
    package: null,
    native_placement: null,
    source_url: null,
    runtime: runtime(),
    config_schema: NONE_SCHEMA,
    read_schema: NONE_SCHEMA,
    action_schema: null,
    presentation: presentation(),
    ...overrides,
  };
}

function nativePlugin(overrides = {}) {
  return addon({
    id: "native.runcompare",
    producer: "native_sqx",
    slot: "automation.extensions",
    kind: "results_plugin",
    package: "RunCompare-EN.zip",
    native_placement: "user/extend/ResultsPlugins/RunCompare",
    source_url: "https://strategyquant.com/codebase/how-runcompare-simplifies-optimization/",
    runtime: runtime({
      status: "packaged",
      stageable: true,
      detail: "Packaged. Install into the authorized StrategyQuant X runtime to use it on Results.",
    }),
    presentation: presentation({
      title: "RunCompare",
      job: "Compare each backtest run against the last one.",
      detail: "StrategyQuant X Results plugin.",
      opens_in: "StrategyQuant X → Results → RunCompare",
      controls: [{ label: "Strategy name", detail: "Give each strategy a unique name." }],
    }),
    ...overrides,
  });
}

function registry(overrides = {}) {
  const addons = overrides.addons ?? [];
  return {
    schema: CAPABILITY_REGISTRY_SCHEMA,
    status: "ready",
    reason_code: null,
    detail: "Native StrategyQuant X plugins are packaged. Results plugins install into SQX; their settings stay in StrategyQuant X.",
    nav_authority: "platform",
    surfaces: APP_SURFACES.map((surface) => surface.id),
    slots: SLOTS.map((slot) => ({ ...slot })),
    refused: [],
    addon_count: addons.length,
    refused_count: 0,
    ...overrides,
    addons,
  };
}

test("empty catalog paints registered slots without rewriting navigation", () => {
  const parsed = capabilityRegistryFromPayload(registry());
  assert.equal(parsed.nav_authority, "platform");
  assert.deepEqual(parsed.surfaces, ["home", "research", "explore", "automation", "operate", "settings"]);
  assert.deepEqual(parsed.slots.map((slot) => slot.id), REGISTERED_SLOT_IDS);
  assert.equal(APP_SURFACES.length, 6);
  const html = renderCapabilitySlot(parsed, "explore.extensions", "catalog");
  assert.match(html, /No native plugins in this view/);
  assert.match(html, /cannot invent a placement or rewrite navigation/);
  assert.doesNotMatch(html, /No add-ons in this slot/);
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
    () => addonFromPayload(addon({ presentation: presentation({ title: "Watch", detail: "<img src=x>" }) })),
    /markup is refused/,
  );
  assert.throws(
    () => addonFromPayload(addon({ producer: "sqx-lab" })),
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

test("slot renderer shows plugin job, SQX controls, and install without identity dumps", () => {
  const bound = registry({
    addons: [
      nativePlugin(),
      addon({
        presentation: presentation({ title: "Watch & hold", detail: "Keep the 'ES' watchlist honest." }),
      }),
    ],
  });
  const html = renderCapabilitySlot(bound, "explore.extensions", "catalog");
  assert.match(html, /data-capability-addon="native\.runcompare"/);
  assert.match(html, /RunCompare/);
  assert.match(html, /Compare each backtest run against the last one\./);
  assert.match(html, /Opens in StrategyQuant X → Results → RunCompare/);
  assert.match(html, /Adjust in StrategyQuant X/);
  assert.match(html, /Strategy name/);
  assert.match(html, /data-capability-stage="native\.runcompare"/);
  assert.match(html, /Install into SQX/);
  assert.match(html, /Watch &amp; hold/);
  assert.match(html, /Keep the &#039;ES&#039; watchlist honest\./);
  assert.doesNotMatch(html, /<script>/);
  assert.doesNotMatch(html, />Identity</);
  assert.doesNotMatch(html, />Producer</);
  const results = renderCapabilitySlot(bound, "automation.extensions", "results");
  assert.match(results, /RunCompare/);
  assert.doesNotMatch(results, /Watch &amp; hold/);
  const install = renderCapabilitySlot(bound, "settings.extensions", "install");
  assert.match(install, /plugin-install-row/);
  assert.match(install, /Install into SQX/);
});

test("selectCapabilityAddons keeps Results plugins on Automation and the full shelf on Explore", () => {
  const addons = [
    nativePlugin(),
    addon({ id: "native.sqx-lab", kind: "authoring_skill", slot: "explore.extensions", producer: "native_sqx" }),
  ];
  assert.equal(selectCapabilityAddons(addons, "catalog", "explore.extensions").length, 2);
  assert.equal(selectCapabilityAddons(addons, "results", "automation.extensions").length, 1);
  assert.equal(selectCapabilityAddons(addons, "install", "settings.extensions").length, 2);
});

test("fetchCapabilityRegistry uses only the canonical registry path", async () => {
  const payload = registry({ addons: [addon()] });
  const parsed = await fetchCapabilityRegistry(async (path, options) => {
    assert.equal(path, CAPABILITY_REGISTRY_API_PATH);
    assert.equal(options.headers.accept, "application/json");
    return { ok: true, json: async () => payload };
  });
  assert.equal(parsed.addons[0].id, "operator.watch-note");
});

test("Explore Automation and Settings host plugin views without extra surfaces", () => {
  const states = {
    runtime: {
      extensions: { status: "ready", reason_code: null, nav_authority: "platform", slot_count: 3, addon_count: 7, refused_count: 0 },
    },
    quotes: null,
    statusState: { phase: "loaded" },
  };
  const explore = renderSecondarySurface({ surfaceId: "explore", label: "Explore" }, states);
  const automation = renderSecondarySurface({ surfaceId: "automation", label: "Automation" }, states);
  const settings = renderSecondarySurface({ surfaceId: "settings", label: "Settings" }, states);
  assert.match(explore, /data-capability-slot="explore\.extensions"/);
  assert.match(explore, /data-capability-view="catalog"/);
  assert.match(explore, /Native StrategyQuant X plugins/);
  assert.match(explore, /Loading native plugins/);
  assert.doesNotMatch(explore, /Research capability coverage/);
  assert.match(automation, /data-automation-workflows/);
  assert.match(automation, /Custom Project workflows/);
  assert.match(automation, /StrategyQuant X MCP/);
  assert.doesNotMatch(automation, /TradingView/);
  assert.doesNotMatch(automation, /MetaTrader/);
  assert.doesNotMatch(automation, /data-capability-slot="automation\.extensions"/);
  assert.match(settings, /data-capability-slot="settings\.extensions"/);
  assert.match(settings, /data-capability-view="install"/);
  assert.match(settings, /Install SQX plugins/);
  for (const html of [explore, settings]) {
    assert.match(html, /data-capability-registry/);
    assert.doesNotMatch(html, /data-route="\/addons"/);
    assert.doesNotMatch(html, /No add-ons in this slot/);
  }
  assert.doesNotMatch(automation, /data-route="\/addons"/);
  assert.deepEqual(APP_SURFACES.map((surface) => surface.id), ["home", "research", "explore", "automation", "operate", "settings"]);
  assert.equal(APP_SURFACES.length, 6);
});
