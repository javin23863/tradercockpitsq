const SQX_OUTPUTS_API_PATH = "/api/sqx-outputs";
const SQX_OUTPUT_LIST_SCHEMA = "tc.sqx-builder-output-list.v1";
const SQX_OUTPUT_IMPORT_SCHEMA = "tc.sqx-builder-output-import.v1";

export function normalizeSqxOutputs(payload) {
  if (!payload || typeof payload !== "object" || payload.schema !== SQX_OUTPUT_LIST_SCHEMA) {
    throw new Error("Unexpected SQX output catalog schema");
  }
  if (!payload.runtime || typeof payload.runtime !== "object" || !Array.isArray(payload.outputs)) {
    throw new Error("Invalid SQX output catalog payload");
  }

  const seen = new Set();
  const outputs = payload.outputs.map((output) => {
    if (!output || typeof output !== "object") throw new Error("Invalid SQX output record");
    if (typeof output.archive !== "string" || !output.archive.endsWith(".sqx")) {
      throw new Error("SQX output is missing a native archive name");
    }
    if (seen.has(output.archive)) throw new Error("Duplicate SQX output archive");
    seen.add(output.archive);

    if (output.importable === true) {
      for (const key of [
        "archive_sha256",
        "native_version",
        "strategy_entry_sha256",
        "settings_entry_sha256",
      ]) {
        if (typeof output[key] !== "string" || output[key].length === 0) {
          throw new Error(`Importable SQX output is missing ${key}`);
        }
      }
    }
    return output;
  });

  return {
    schema: payload.schema,
    sqx_build: String(payload.sqx_build || ""),
    project: String(payload.project || ""),
    databank: String(payload.databank || ""),
    runtime: payload.runtime,
    outputs,
  };
}

export function sqxOutputImportPath(archive) {
  const value = String(archive ?? "");
  if (
    !value
    || value.includes("/")
    || value.includes("\\")
    || !value.toLowerCase().endsWith(".sqx")
  ) {
    throw new Error("Invalid SQX output archive");
  }
  const params = new URLSearchParams();
  params.set("archive", value);
  return `${SQX_OUTPUTS_API_PATH}/import?${params.toString()}`;
}

export async function fetchSqxOutputs(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Fetch is not available");
  const response = await fetchImpl(SQX_OUTPUTS_API_PATH, {
    headers: { accept: "application/json" },
  });
  if (!response.ok) throw new Error(`SQX output lookup failed (${response.status})`);
  return normalizeSqxOutputs(await response.json());
}

export async function importSqxOutput(archive, fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Fetch is not available");
  const response = await fetchImpl(sqxOutputImportPath(archive), {
    method: "POST",
    headers: { accept: "application/json" },
    body: "",
  });
  const payload = await response.json();
  if (!response.ok) {
    throw new Error(payload?.detail || `SQX output import failed (${response.status})`);
  }
  if (
    payload?.schema !== SQX_OUTPUT_IMPORT_SCHEMA
    || payload?.archive?.archive !== archive
    || typeof payload?.strategy_ref !== "string"
    || typeof payload?.candidate_ref !== "string"
    || payload?.custody !== "persisted"
  ) {
    throw new Error("SQX output import returned an unexpected custody receipt");
  }
  return payload;
}
