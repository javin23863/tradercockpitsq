const SQX_PRESETS_API_PATH = "/api/sqx-presets";
const SQX_PRESET_SCHEMA = "tc.sqx-preset-catalog.v1";
const SQX_PRESET_LAUNCH_SCHEMA = "tc.sqx-preset-launch.v1";

export function normalizePresetCatalog(payload) {
  if (!payload || typeof payload !== "object" || payload.schema !== SQX_PRESET_SCHEMA) {
    throw new Error("Unexpected SQX preset catalog schema");
  }
  if (!Array.isArray(payload.presets) || payload.presets.length === 0) {
    throw new Error("SQX preset catalog is empty");
  }

  const seen = new Set();
  const presets = payload.presets.map((preset) => {
    if (!preset || typeof preset !== "object") throw new Error("Invalid SQX preset record");
    const required = [
      "preset_id",
      "label",
      "market",
      "source_build",
      "source_relative_path",
      "source_sha256",
      "reference_commit",
    ];
    for (const key of required) {
      if (typeof preset[key] !== "string" || preset[key].length === 0) {
        throw new Error(`SQX preset is missing ${key}`);
      }
    }
    if (seen.has(preset.preset_id)) throw new Error("Duplicate SQX preset id");
    seen.add(preset.preset_id);
    const runtime = preset.runtime && typeof preset.runtime === "object"
      ? preset.runtime
      : {
          available: false,
          status: "runtime_not_configured",
          verified_sha256: null,
          launch_available: false,
          launch_status: "runtime_not_configured",
          launch_detail: "SQX_HOME is not configured",
          observed_build: null,
        };
    return { ...preset, runtime };
  });

  return {
    schema: payload.schema,
    source_build: String(payload.source_build || ""),
    reference_commit: String(payload.reference_commit || ""),
    presets,
  };
}

export function selectedPresetId(search = "") {
  return new URLSearchParams(search).get("presetId") || "";
}

export function presetSelectionPath(pathname, search, presetId) {
  const params = new URLSearchParams(search);
  params.set("presetId", String(presetId ?? ""));
  const query = params.toString();
  return query ? `${pathname}?${query}` : pathname;
}

export function sqxPresetLaunchPath(presetId) {
  const value = String(presetId ?? "");
  if (!/^[a-z0-9-]+$/.test(value)) throw new Error("Invalid SQX preset ID");
  return `${SQX_PRESETS_API_PATH}/${value}/launch`;
}

export async function fetchSqxPresetCatalog(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Fetch is not available");
  const response = await fetchImpl(SQX_PRESETS_API_PATH, {
    headers: { accept: "application/json" },
  });
  if (!response.ok) throw new Error(`SQX preset catalog request failed (${response.status})`);
  return normalizePresetCatalog(await response.json());
}

export async function launchSqxPreset(presetId, fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Fetch is not available");
  const response = await fetchImpl(sqxPresetLaunchPath(presetId), {
    method: "POST",
    headers: { accept: "application/json" },
    body: "",
  });
  const payload = await response.json();
  if (!response.ok) {
    throw new Error(payload?.detail || `SQX preset launch failed (${response.status})`);
  }
  if (
    payload?.schema !== SQX_PRESET_LAUNCH_SCHEMA
    || payload?.state !== "submitted"
    || payload?.preset_id !== presetId
    || !Number.isInteger(payload?.control_requests_submitted)
  ) {
    throw new Error("SQX preset launch returned an unexpected receipt");
  }
  return payload;
}
