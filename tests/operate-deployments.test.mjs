import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import {
  deploymentCatalogFromPayload,
  renderOperateLiveRuns,
} from "../web/operate-deployments.mjs";

const exportEntity = "tc-research:export:v1:dddddddd-dddd-4ddd-8ddd-dddddddddddd";
const candidateEntity = "tc-research:candidate:v1:cccccccc-cccc-4ccc-8ccc-cccccccccccc";
const deploymentEntity = "tc-research:deployment:v1:ffffffff-ffff-4fff-8fff-ffffffffffff";

function catalog(deployments) {
  return {
    schema: "tc.operate-deployment-catalog.v1",
    deployments,
  };
}

function deploymentRecord(overrides = {}) {
  return {
    entity_id: deploymentEntity,
    revision: `tc-research-revision:deployment:sha256:${"a".repeat(64)}`,
    export_entity_id: exportEntity,
    candidate_entity_id: candidateEntity,
    candidate_archive_name: "Survivor.sqx",
    mode: "identity_only",
    status: "execution_not_connected",
    ...overrides,
  };
}

const liveDeployment = {
  schema: "tc.live-deployment.v1",
  status: "unavailable",
  reason_code: "execution_not_connected",
  detail: "Deployment custody records exported identities only.",
};

test("deployment catalog parser keeps identities without inventing live execution", () => {
  const parsed = deploymentCatalogFromPayload(catalog([deploymentRecord()]));
  assert.equal(parsed.deployments.length, 1);
  assert.equal(parsed.deployments[0].candidate_archive_name, "Survivor.sqx");
  assert.equal(parsed.deployments[0].mode, "identity_only");
  assert.equal(parsed.deployments[0].status, "execution_not_connected");
  assert.equal("live" in parsed.deployments[0], false);
  assert.equal("broker" in parsed.deployments[0], false);
  assert.throws(
    () => deploymentCatalogFromPayload(catalog([deploymentRecord(), deploymentRecord()])),
    /duplicate entity identity/,
  );
});

test("Operate live runs host renders current zero and loaded deployment custody rows", () => {
  const empty = renderOperateLiveRuns({ deployments: [] }, liveDeployment);
  assert.match(empty, /No deployment custody yet/);
  assert.match(empty, /does not claim live execution/);
  assert.doesNotMatch(empty, /Survivor\.sqx/);
  const loaded = renderOperateLiveRuns(deploymentCatalogFromPayload(catalog([deploymentRecord()])), liveDeployment);
  assert.match(loaded, /Survivor\.sqx/);
  assert.match(loaded, /Identity Only/);
  assert.match(loaded, /Execution Not Connected/);
  assert.match(loaded, /Deployment custody rows bind exported identities only/);
});

test("desktop loads the Operate deployments binder", async () => {
  const source = await readFile(new URL("../web/index.html", import.meta.url), "utf8");
  assert.match(source, /src="\/operate-deployments\.mjs"/);
});
