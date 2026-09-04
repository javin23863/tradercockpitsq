import assert from "node:assert/strict";
import test from "node:test";

import { renderSecondarySurface } from "../web/surfaces.mjs";

function settingsHtml(research) {
  return renderSecondarySurface(
    { surfaceId: "settings", label: "Settings" },
    {
      runtime: {
        research_backend: research,
        account: { status: "unavailable", reason_code: "authority_not_implemented" },
        model: { status: "unavailable", reason_code: "provider_not_configured" },
        provider: { status: "unavailable", reason_code: "provider_not_configured" },
        research_custody: { status: "unavailable", reason_code: "store_not_bound" },
        extensions: { status: "ready", reason_code: null, registry_schema: "tc.capability-addon-registry.v1", nav_authority: "platform", slot_count: 3, addon_count: 0, refused_count: 0 },
        application: { status: "ready", server: "canonical", desktop: "canonical-server-ui" },
      },
      quotes: null,
      statusState: { phase: "loaded" },
    },
  );
}

test("Settings native runtime card shows fail-closed recovery copy", () => {
  const html = settingsHtml({
    status: "unavailable",
    configured: false,
    verified: false,
    producer: "strategyquant-x",
    build: null,
    reason_code: "runtime_not_configured",
    detail: "Set SQX_HOME or pass --sqx-home to the installed StrategyQuant X 144.2953 runtime. A unique 144.2953 install in the usual Windows locations can be remembered for this machine. The browser cannot choose this path.",
    runtime: {
      build: { expected: "144.2953", observed: null, verified: false },
      inspection: { available: false, reason_code: "runtime_not_configured" },
      launcher: { relative_path: "sqcli.exe", status: "unavailable", verified: false, reason_code: "runtime_not_verified" },
      execution: { available: false, reason_code: "runtime_not_configured" },
    },
    execution: {
      available: false,
      reason_code: "runtime_not_configured",
      detail: "Set SQX_HOME or pass --sqx-home to the installed StrategyQuant X 144.2953 runtime. A unique 144.2953 install in the usual Windows locations can be remembered for this machine. The browser cannot choose this path.",
    },
  });
  assert.match(html, /Native research runtime/);
  assert.match(html, /data-runtime-recovery/);
  assert.match(html, /Set SQX_HOME or pass --sqx-home/);
  assert.match(html, /browser cannot choose this path/);
  assert.match(html, /usual Windows locations/);
  assert.match(html, /data-capability-slot="settings\.extensions"/);
  assert.doesNotMatch(html, /C:\\|sqx_home=|path picker/i);
});

test("Settings native runtime card names two installs without a path", () => {
  const html = settingsHtml({
    status: "unavailable",
    configured: false,
    verified: false,
    producer: "strategyquant-x",
    build: null,
    reason_code: "sqx_install_ambiguous",
    detail: "More than one StrategyQuant X 144.2953 install was found. Set SQX_HOME or pass --sqx-home to the authorized one. The browser cannot choose this path.",
    binding: { source: "none" },
    runtime: {
      build: { expected: "144.2953", observed: null, verified: false },
      inspection: { available: false, reason_code: "runtime_not_configured" },
      launcher: { relative_path: "sqcli.exe", status: "unavailable", verified: false, reason_code: "runtime_not_verified" },
      execution: { available: false, reason_code: "sqx_install_ambiguous" },
    },
    execution: {
      available: false,
      reason_code: "sqx_install_ambiguous",
      detail: "More than one StrategyQuant X 144.2953 install was found. Set SQX_HOME or pass --sqx-home to the authorized one. The browser cannot choose this path.",
    },
  });
  assert.match(html, /More than one StrategyQuant X 144\.2953 install was found/);
  assert.match(html, /Runtime source/);
  assert.match(html, /Not configured/);
  assert.doesNotMatch(html, /C:\\|Downloads|Choose a folder/i);
});

test("Settings native runtime card keeps verified copy and launcher recovery separate", () => {
  const html = settingsHtml({
    status: "ready",
    configured: true,
    verified: true,
    producer: "strategyquant-x",
    build: "144.2953",
    reason_code: null,
    binding: { source: "remembered" },
    detail: "Verified StrategyQuant X 144.2953 runtime for native research inspection and approval-gated Builder control.",
    runtime: {
      build: { expected: "144.2953", observed: "144.2953", verified: true },
      inspection: { available: true, reason_code: null },
      launcher: { relative_path: "sqcli.exe", status: "unavailable", verified: false, reason_code: "trusted_launcher_not_configured" },
      execution: { available: false, reason_code: "trusted_launcher_not_configured" },
    },
    execution: {
      available: false,
      reason_code: "trusted_launcher_not_configured",
      detail: "Set SQX_LAUNCHER_SHA256 to the SHA-256 digest of the installed sqcli.exe. The browser cannot choose this value.",
    },
  });
  assert.match(html, /Verified StrategyQuant X 144\.2953/);
  assert.match(html, /Remembered on this machine/);
  assert.match(html, /data-runtime-recovery/);
  assert.match(html, /SQX_LAUNCHER_SHA256/);
  assert.doesNotMatch(html, /bind|discover|Choose a folder/i);
});
