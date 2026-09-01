import test from "node:test";
import assert from "node:assert/strict";

import { createBuilderConfigSnapshotFetch } from "../web/research-builder-config-snapshot.mjs";

function specificationLocation() {
  return {
    origin: "http://127.0.0.1:4175",
    pathname: "/research",
    search: "?stage=construct&tab=specification",
  };
}

test("all Builder inspector reads share one exact response inside one Specification grid", async () => {
  let calls = 0;
  let scope = { id: 1 };
  const fetchImpl = async () => {
    calls += 1;
    return new Response(JSON.stringify({
      schema: "tc.sqx-builder-config.v1",
      archive_sha256: String(calls).padStart(64, "0"),
    }), {
      status: 200,
      headers: { "content-type": "application/json" },
    });
  };
  const fetchSnapshot = createBuilderConfigSnapshotFetch(fetchImpl, {
    locationProvider: specificationLocation,
    scopeProvider: () => scope,
  });

  const [first, second] = await Promise.all([
    fetchSnapshot("/api/sqx-builder-config", { headers: { accept: "application/json" } }),
    fetchSnapshot("/api/sqx-builder-config", { headers: { accept: "application/json" } }),
  ]);
  assert.equal(calls, 1);
  assert.deepEqual(await first.json(), await second.json());

  const third = await fetchSnapshot("/api/sqx-builder-config");
  assert.equal(calls, 1);
  assert.equal((await third.json()).archive_sha256, "1".padStart(64, "0"));

  scope = { id: 2 };
  const nextGrid = await fetchSnapshot("/api/sqx-builder-config");
  assert.equal(calls, 2);
  assert.equal((await nextGrid.json()).archive_sha256, "2".padStart(64, "0"));
});

test("snapshot transport never caches other routes or API paths", async () => {
  let calls = 0;
  const fetchImpl = async (input) => {
    calls += 1;
    return new Response(JSON.stringify({ input: String(input), calls }), { status: 200 });
  };
  const location = { ...specificationLocation(), search: "?stage=construct&tab=idea" };
  const fetchSnapshot = createBuilderConfigSnapshotFetch(fetchImpl, {
    locationProvider: () => location,
    scopeProvider: () => ({ id: 1 }),
  });
  await fetchSnapshot("/api/sqx-builder-config");
  await fetchSnapshot("/api/sqx-builder-config");
  location.search = "?stage=construct&tab=specification";
  await fetchSnapshot("/api/status");
  await fetchSnapshot("/api/status");
  assert.equal(calls, 4);
});
