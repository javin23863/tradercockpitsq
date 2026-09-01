from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


path = Path("web/research-backtest-robustness.mjs")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''export function robustnessCatalogFromPayload(payload) {
  if (!payload || payload.schema !== ROBUSTNESS_CATALOG_SCHEMA || !Array.isArray(payload.results)) {
    throw new Error("Native robustness catalog schema is invalid");
  }
  return payload.results.map(robustnessResultFromPayload);
}

''',
    '''export function robustnessCatalogFromPayload(payload) {
  if (!payload || payload.schema !== ROBUSTNESS_CATALOG_SCHEMA || !Array.isArray(payload.results)) {
    throw new Error("Native robustness catalog schema is invalid");
  }
  return payload.results.map(robustnessResultFromPayload);
}

export function robustnessResultForHistorical(catalog, historicalResult) {
  if (!Array.isArray(catalog)) throw new Error("Native robustness catalog is invalid");
  const source = historicalResultFromPayload(historicalResult);
  return catalog.find((item) => item.source_historical_result_revision === source.revision) || null;
}

''',
    "catalog selector helper",
)
text = replace_once(
    text,
    '''    let validation = catalog[0] || null;
    let detail = "";
    if (requestedRef) {
      try {
        validation = await fetchRobustnessResult(requestedRef);
      } catch (error) {
        detail = `Saved robustness result unavailable: ${error instanceof Error ? error.message : "readback failed"}`;
      }
    }
''',
    '''    const completed = results.filter((item) => item.state === "completed" && item.execution_completed === true);
    let selectedIndex = 0;
    let validation = completed[0] ? robustnessResultForHistorical(catalog, completed[0]) : null;
    let detail = "";
    if (requestedRef) {
      try {
        validation = await fetchRobustnessResult(requestedRef);
        const sourceIndex = completed.findIndex((item) => item.revision === validation.source_historical_result_revision);
        if (sourceIndex >= 0) selectedIndex = sourceIndex;
      } catch (error) {
        detail = `Saved robustness result unavailable: ${error instanceof Error ? error.message : "readback failed"}`;
      }
    }
''',
    "load catalog binding",
)
text = replace_once(text, "      selectedIndex: 0,\n", "      selectedIndex,\n", "load selected index")
text = replace_once(
    text,
    '''async function start(button) {
  if (state.phase === "loading" || !state.runtimeReady) return;
''',
    '''async function start(button) {
  const higherCapability = state.capabilities?.methods?.find((item) => item.method === HIGHER_PRECISION_METHOD) || null;
  if (state.phase === "loading" || !state.runtimeReady || higherCapability?.state !== "ready") return;
''',
    "start capability guard",
)
text = replace_once(
    text,
    '''    const selected = completed[selectedIndex];
    const validation = state.catalog.find((item) => item.source_historical_result_revision === selected.revision) || null;
''',
    '''    const selected = completed[selectedIndex];
    const validation = robustnessResultForHistorical(state.catalog, selected);
''',
    "selection catalog binding",
)
path.write_text(text, encoding="utf-8")

path = Path("tests/research-backtest-robustness.test.mjs")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    "  robustnessResultFromPayload,\n  startHigherPrecision,\n",
    "  robustnessResultForHistorical,\n  robustnessResultFromPayload,\n  startHigherPrecision,\n",
    "selector test import",
)
text += r'''

test("robustness catalog selection binds to the selected Historical Result revision", () => {
  const first = historical();
  const second = {
    ...historical(),
    entity_id: "tc-research:historical-result:v1:44444444-4444-4444-8444-444444444444",
    revision: `tc-research-revision:historical-result:sha256:${"4".repeat(64)}`,
    result_archive_ref: `tc-evidence:sha256:${"5".repeat(64)}`,
    result_archive_sha256: "5".repeat(64),
  };
  const firstRun = robustness();
  const secondRun = {
    ...robustness(),
    validation_ref: `tc-evidence:sha256:${"6".repeat(64)}`,
    source_historical_result_entity_id: second.entity_id,
    source_historical_result_revision: second.revision,
    source_result_archive_ref: second.result_archive_ref,
    source_result_archive_sha256: second.result_archive_sha256,
    result_archive_ref: `tc-evidence:sha256:${"7".repeat(64)}`,
    result_archive_sha256: "7".repeat(64),
  };
  const catalog = [secondRun, firstRun].map(robustnessResultFromPayload);
  assert.equal(robustnessResultForHistorical(catalog, first)?.validation_ref, firstRun.validation_ref);
  assert.equal(robustnessResultForHistorical(catalog, second)?.validation_ref, secondRun.validation_ref);
});
'''
path.write_text(text, encoding="utf-8")
