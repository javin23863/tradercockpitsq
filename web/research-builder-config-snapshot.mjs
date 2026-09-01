const BUILDER_CONFIG_PATH = "/api/sqx-builder-config";

function isSpecificationRoute(locationLike = globalThis.location) {
  if (locationLike?.pathname !== "/research") return false;
  const params = new URLSearchParams(locationLike.search || "");
  return params.get("stage") === "construct" && params.get("tab") === "specification";
}

function requestIdentity(input, init, locationLike = globalThis.location) {
  const request = typeof Request !== "undefined" && input instanceof Request ? input : null;
  const method = String(init?.method || request?.method || "GET").toUpperCase();
  const base = locationLike?.origin || "http://127.0.0.1";
  const url = new URL(request?.url || String(input), base);
  return { method, url };
}

function cloneCapturedResponse(captured) {
  return new Response(captured.body.slice(0), {
    status: captured.status,
    statusText: captured.statusText,
    headers: captured.headers,
  });
}

export function createBuilderConfigSnapshotFetch(
  fetchImpl,
  {
    locationProvider = () => globalThis.location,
    scopeProvider = () => globalThis.document?.querySelector?.(".requirement-grid") || null,
  } = {},
) {
  if (typeof fetchImpl !== "function") throw new Error("Builder snapshot fetch requires a fetch implementation");
  let scope = null;
  let capturedPromise = null;

  return async function builderSnapshotFetch(input, init) {
    const locationLike = locationProvider();
    const { method, url } = requestIdentity(input, init, locationLike);
    const sameOrigin = !locationLike?.origin || url.origin === locationLike.origin;
    const shouldSnapshot = (
      method === "GET"
      && sameOrigin
      && url.pathname === BUILDER_CONFIG_PATH
      && !url.search
      && isSpecificationRoute(locationLike)
    );
    if (!shouldSnapshot) return fetchImpl(input, init);

    const nextScope = scopeProvider();
    if (!nextScope) return fetchImpl(input, init);
    if (nextScope !== scope) {
      scope = nextScope;
      capturedPromise = null;
    }
    if (!capturedPromise) {
      capturedPromise = (async () => {
        const response = await fetchImpl(input, init);
        return {
          status: response.status,
          statusText: response.statusText,
          headers: [...response.headers.entries()],
          body: await response.arrayBuffer(),
        };
      })();
    }
    try {
      return cloneCapturedResponse(await capturedPromise);
    } catch (error) {
      capturedPromise = null;
      throw error;
    }
  };
}

if (typeof document !== "undefined" && typeof globalThis.fetch === "function") {
  const nativeFetch = globalThis.fetch.bind(globalThis);
  globalThis.fetch = createBuilderConfigSnapshotFetch(nativeFetch);
}
