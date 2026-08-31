const RESEARCH_IDEAS_API_PATH = "/api/research/ideas";
export const IDEA_READ_SCHEMA = "tc.research-idea.v1";
export const IDEA_CATALOG_SCHEMA = "tc.research-idea-catalog.v1";

export class ResearchIdeaApiError extends Error {
  constructor(message, { status = 0, payload = null } = {}) {
    super(message);
    this.name = "ResearchIdeaApiError";
    this.status = status;
    this.payload = payload;
  }
}

async function responsePayload(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

function ensureSchema(payload, schema, detail) {
  if (!payload || payload.schema !== schema) throw new ResearchIdeaApiError(detail);
  return payload;
}

export async function fetchIdeaCatalog(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new ResearchIdeaApiError("Idea catalog fetch is unavailable");
  const response = await fetchImpl(RESEARCH_IDEAS_API_PATH, {
    headers: { accept: "application/json" },
  });
  const payload = await responsePayload(response);
  if (!response?.ok) {
    throw new ResearchIdeaApiError(payload?.detail || `Idea catalog request failed: ${response?.status ?? "unknown"}`, {
      status: response?.status ?? 0,
      payload,
    });
  }
  return ensureSchema(payload, IDEA_CATALOG_SCHEMA, "Idea catalog schema mismatch");
}

export async function fetchIdea(entityId, fetchImpl = globalThis.fetch) {
  if (typeof entityId !== "string" || !entityId) throw new ResearchIdeaApiError("Idea entity id is required");
  if (typeof fetchImpl !== "function") throw new ResearchIdeaApiError("Idea fetch is unavailable");
  const path = `${RESEARCH_IDEAS_API_PATH}?${new URLSearchParams({ entityId }).toString()}`;
  const response = await fetchImpl(path, { headers: { accept: "application/json" } });
  const payload = await responsePayload(response);
  if (!response?.ok) {
    throw new ResearchIdeaApiError(payload?.detail || `Idea request failed: ${response?.status ?? "unknown"}`, {
      status: response?.status ?? 0,
      payload,
    });
  }
  return ensureSchema(payload, IDEA_READ_SCHEMA, "Idea schema mismatch");
}

export async function saveIdeaRevision(
  { entityId = "", expectedRevision = "", text, source = "" },
  fetchImpl = globalThis.fetch,
) {
  if (typeof fetchImpl !== "function") throw new ResearchIdeaApiError("Idea save is unavailable");
  const body = entityId
    ? {
        entity_id: entityId,
        expected_revision: expectedRevision,
        text,
        source,
      }
    : { text, source };
  const response = await fetchImpl(RESEARCH_IDEAS_API_PATH, {
    method: "POST",
    headers: {
      accept: "application/json",
      "content-type": "application/json",
    },
    body: JSON.stringify(body),
  });
  const payload = await responsePayload(response);
  if (!response?.ok) {
    throw new ResearchIdeaApiError(payload?.detail || `Idea save failed: ${response?.status ?? "unknown"}`, {
      status: response?.status ?? 0,
      payload,
    });
  }
  return ensureSchema(payload, IDEA_READ_SCHEMA, "Saved Idea schema mismatch");
}
