from __future__ import annotations

from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected one match, found {count}: {old[:180]!r}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


def insert_before(path: str, marker: str, addition: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    if text.count(marker) != 1:
        raise SystemExit(f"{path}: expected one insertion marker, found {text.count(marker)}: {marker!r}")
    file.write_text(text.replace(marker, addition + marker, 1), encoding="utf-8")


# 1. The exact staged Historical Result filename must remain the physical file
#    opened by SQX, not merely resolve somewhere inside the Results directory.
replace_once(
    "product/tradercockpit/sqx_gateway.py",
    '''            result_file, result_relative = _resolve_inside(\n                launcher.home,\n                results_root / result_archive_name,\n                escape_code="retester_result_archive_path_escape",\n            )\n            if result_file.parent != results_root or not result_file.is_file():\n                raise SqxNativeGatewayError(\n                    "retester_result_archive_missing",\n                    "exact staged Retester result archive is missing",\n                )\n''',
    '''            expected_result_file = results_root / result_archive_name\n            result_file, result_relative = _resolve_inside(\n                launcher.home,\n                expected_result_file,\n                escape_code="retester_result_archive_path_escape",\n            )\n            if result_file != expected_result_file:\n                raise SqxNativeGatewayError(\n                    "retester_result_archive_path_escape",\n                    "exact staged Retester result archive was redirected away from its generated path",\n                )\n            if result_file.parent != results_root or not result_file.is_file():\n                raise SqxNativeGatewayError(\n                    "retester_result_archive_missing",\n                    "exact staged Retester result archive is missing",\n                )\n''',
)

# 2. Proof readback must bind its claimed source archive back to the exact
#    immutable Historical Result revision, not only to its prepared Proof parent.
historical_helper = '''\n\ndef _validate_historical_source_binding(\n    store: FileResearchCustodyStore,\n    payload: dict[str, object],\n) -> None:\n    try:\n        source_entity = ResearchEntityId.parse(payload["source_historical_result_entity_id"])\n        source_revision = ResearchRevisionRef.parse(payload["source_historical_result_revision"])\n        source_ref = EvidenceRef.parse(payload["source_result_archive_ref"])\n    except (KeyError, TypeError, ResearchCustodyError) as exc:\n        raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "robustness Proof source identities are invalid") from exc\n    if source_entity.kind != ResearchKind.HISTORICAL_RESULT or source_revision.kind != ResearchKind.HISTORICAL_RESULT:\n        raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "robustness Proof source is not Historical Result custody")\n    try:\n        stored_source = store.read_revision(source_revision)\n        source_content = json.loads(store.read_revision_content(source_revision))\n        source_bytes = store.read_evidence(source_ref)\n    except (ResearchCustodyError, UnicodeDecodeError, json.JSONDecodeError) as exc:\n        raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "robustness Proof source Historical Result revision is unavailable or corrupt") from exc\n    if stored_source.entity_id != source_entity:\n        raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "robustness Proof source revision belongs to another Historical Result entity")\n    if (\n        not isinstance(source_content, dict)\n        or source_content.get("schema") != "tc.research-historical-result-content.v1"\n        or source_content.get("state") != "completed"\n        or source_content.get("result_archive_ref") != str(source_ref)\n        or source_content.get("result_archive_sha256") != payload.get("source_result_archive_sha256")\n        or payload.get("source_result_archive_ref") != source_content.get("result_archive_ref")\n    ):\n        raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "robustness Proof source archive does not match its Historical Result revision")\n    digest = _digest(source_content.get("result_archive_sha256"), "robustness_proof_catalog_corrupt")\n    if source_ref.digest != digest or sha256(source_bytes).hexdigest() != digest or source_ref not in set(stored_source.evidence):\n        raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "robustness Proof source Historical Result archive evidence binding is invalid")\n'''
insert_before(
    "product/tradercockpit/research_robustness.py",
    "\ndef _failed_successor(\n",
    historical_helper,
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''        record_ref = stored.content\n        record = _read_record(store, record_ref)\n        if stored.parent_revision is None:\n''',
    '''        record_ref = stored.content\n        record = _read_record(store, record_ref)\n        _validate_historical_source_binding(store, record)\n        if stored.parent_revision is None:\n''',
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''        try:\n            source_entity = ResearchEntityId.parse(raw["source_historical_result_entity_id"])\n            source_revision = ResearchRevisionRef.parse(raw["source_historical_result_revision"])\n        except (KeyError, TypeError, ResearchCustodyError) as exc:\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness proof source identities are invalid") from exc\n        if source_entity.kind != ResearchKind.HISTORICAL_RESULT or source_revision.kind != ResearchKind.HISTORICAL_RESULT:\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness source is not Historical Result custody")\n''',
    '''        _validate_historical_source_binding(store, raw)\n''',
)

# 3. Start responses are generation-bound, and a completed native response must
#    be reconciled against a freshly fetched Historical Result catalog before UI display.
insert_before(
    "web/research-backtest-robustness.mjs",
    "\nlet generation = 0;\n",
    '''\nexport function robustnessOperationIsCurrent(startGeneration, currentGeneration, routeActive) {\n  return Number.isInteger(startGeneration) && Number.isInteger(currentGeneration)\n    && startGeneration === currentGeneration && routeActive === true;\n}\n\nexport function robustnessCurrentSourceIndex(results, source) {\n  if (!Array.isArray(results) || !source || typeof source.entity_id !== "string" || typeof source.revision !== "string") return -1;\n  return results.findIndex((item) => (\n    item?.state === "completed"\n    && item?.execution_completed === true\n    && item.entity_id === source.entity_id\n    && item.revision === source.revision\n  ));\n}\n''',
)
web = Path("web/research-backtest-robustness.mjs")
text = web.read_text(encoding="utf-8")
start_marker = "async function start(button) {\n"
end_marker = "\n}\n\nif (typeof document !== \"undefined\") {"
if text.count(start_marker) != 1 or text.count(end_marker) != 1:
    raise SystemExit("web/research-backtest-robustness.mjs: start function markers changed")
head, remainder = text.split(start_marker, 1)
_old_body, tail = remainder.split(end_marker, 1)
new_start = '''async function start(button) {\n  const startGeneration = generation;\n  const higherCapability = state.capabilities?.methods?.find((item) => item.method === HIGHER_PRECISION_METHOD) || null;\n  if (["loading", "running"].includes(state.phase) || !state.runtimeReady || higherCapability?.state !== "ready") return;\n  const completed = state.results.filter((item) => item.state === "completed" && item.execution_completed === true);\n  const selected = completed[state.selectedIndex];\n  if (!selected) return;\n  const inFlightSource = { entity_id: selected.entity_id, revision: selected.revision };\n  clearValidationRef();\n  state = { ...state, phase: "running", inFlightSource, validation: null, suppressCompletedPicker: true, detail: "Running Higher Precision in SQX…" };\n  render(panel(), state);\n  try {\n    const validation = await startHigherPrecision(selected);\n    if (!robustnessOperationIsCurrent(startGeneration, generation, robustnessRoute())) return;\n\n    let refreshedResults;\n    try {\n      refreshedResults = await fetchHistoricalResults();\n    } catch (refreshError) {\n      if (!robustnessOperationIsCurrent(startGeneration, generation, robustnessRoute())) return;\n      clearValidationRef();\n      state = {\n        ...state,\n        phase: "failed",\n        results: [],\n        selectedIndex: 0,\n        runtimeReady: false,\n        capabilities: null,\n        validation: null,\n        suppressCompletedPicker: true,\n        inFlightSource: null,\n        detail: `Native result captured, but current Historical Result custody could not be refreshed: ${refreshError instanceof Error ? refreshError.message : "readback failed"}. Receipt was not cross-displayed.`,\n      };\n      render(panel(), state);\n      return;\n    }\n    if (!robustnessOperationIsCurrent(startGeneration, generation, robustnessRoute())) return;\n\n    const sourceIndex = robustnessCurrentSourceIndex(refreshedResults, inFlightSource);\n    if (sourceIndex < 0) {\n      clearValidationRef();\n      state = { ...state, phase: "loaded", results: refreshedResults, selectedIndex: 0, validation: null, suppressCompletedPicker: true, inFlightSource: null, detail: "Native result captured, but its source Historical Result is no longer current; receipt was not cross-displayed." };\n    } else {\n      persistValidationRef(validation.validation_ref);\n      state = { ...state, phase: "loaded", results: refreshedResults, selectedIndex: sourceIndex, validation, suppressCompletedPicker: false, inFlightSource: null, catalog: [validation, ...state.catalog.filter((item) => item.validation_ref !== validation.validation_ref)], detail: "Native Higher Precision result captured. Producer verdict remains unread." };\n    }\n  } catch (error) {\n    if (!robustnessOperationIsCurrent(startGeneration, generation, robustnessRoute())) return;\n    let failedAttempt = null;\n    try { failedAttempt = await fetchRobustnessAttemptForStartError(error, selected); } catch {}\n    let failedAttempts = state.failedAttempts;\n    try { failedAttempts = (await fetchRobustnessCatalog()).failedAttempts; } catch {}\n    if (!robustnessOperationIsCurrent(startGeneration, generation, robustnessRoute())) return;\n    if (failedAttempt && !failedAttempts.some((item) => item.attempt_ref === failedAttempt.attempt_ref)) {\n      failedAttempts = [failedAttempt, ...failedAttempts];\n    }\n    if (failedAttempt) persistValidationRef(failedAttempt.attempt_ref);\n    else clearValidationRef();\n    state = robustnessStartFailureState(\n      state,\n      failedAttempt,\n      failedAttempts,\n      error instanceof Error ? error.message : "Native Higher Precision execution failed",\n    );\n  }\n  render(panel(), state);\n'''
web.write_text(head + new_start + end_marker + tail, encoding="utf-8")

# 4. Regression fixtures now create real immutable Historical Result source
#    revisions so Proof readback can verify the full source link.
replace_once(
    "tests/product/test_research_robustness.py",
    "from tradercockpit.research_custody import FileResearchCustodyStore, ResearchCustodyError, ResearchRevisionRef\n",
    "from tradercockpit.research_custody import FileResearchCustodyStore, ResearchCustodyError, ResearchEntityId, ResearchRevisionRef\n",
)
replace_once(
    "tests/product/test_research_robustness.py",
    '''    def _historical(self, store: FileResearchCustodyStore, source: bytes) -> dict[str, object]:\n        ref = store.put_evidence(source)\n        inspected = inspect_sqx_output_bytes(source, archive_name="Baseline.sqx")\n        return {\n            "revision": self.HISTORICAL_REVISION,\n            "state": "completed",\n            "execution_completed": True,\n            "sqx_build": "144.2953",\n            "result_archive_name": "Baseline.sqx",\n            "result_archive_ref": str(ref),\n            "result_archive_sha256": inspected["archive_sha256"],\n        }\n''',
    '''    def _historical(self, store: FileResearchCustodyStore, source: bytes) -> dict[str, object]:\n        ref = store.put_evidence(source)\n        inspected = inspect_sqx_output_bytes(source, archive_name="Baseline.sqx")\n        entity = ResearchEntityId.parse(self.HISTORICAL_ENTITY)\n        try:\n            current = store.current(entity)\n        except ResearchCustodyError as exc:\n            if exc.code != "current_pointer_missing":\n                raise\n            current = None\n        content = json.dumps({\n            "schema": "tc.research-historical-result-content.v1",\n            "state": "completed",\n            "result_archive_ref": str(ref),\n            "result_archive_sha256": inspected["archive_sha256"],\n        }, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")\n        revision = store.create_revision(entity, content, parent_revision=current, evidence=(ref,))\n        store.compare_and_set_current(entity, expected_revision=current, target_revision=revision.revision)\n        self.HISTORICAL_REVISION = str(revision.revision)\n        return {\n            "revision": self.HISTORICAL_REVISION,\n            "state": "completed",\n            "execution_completed": True,\n            "sqx_build": "144.2953",\n            "result_archive_name": "Baseline.sqx",\n            "result_archive_ref": str(ref),\n            "result_archive_sha256": inspected["archive_sha256"],\n        }\n''',
)

insert_before(
    "tests/product/test_sqx_retester_gateway.py",
    "\n    def test_staged_baseline_change_refuses_before_runner(self) -> None:\n",
    '''\n    def test_staged_baseline_file_redirection_refuses_before_runner(self) -> None:\n        with TemporaryDirectory() as tmp:\n            home, project_name, launcher_hash, project_hash, engine_hash, baseline_hash = self._runtime(Path(tmp))\n            results = home / "user/projects" / project_name / "databanks/Results"\n            baseline = results / "Baseline.sqx"\n            redirected = results / "Redirected.sqx"\n            redirected.write_bytes(baseline.read_bytes())\n            baseline.unlink()\n            try:\n                baseline.symlink_to(redirected)\n            except OSError as exc:\n                self.skipTest(f"file symlink unavailable on this platform: {exc}")\n            calls = 0\n\n            def runner(*args, **kwargs):\n                nonlocal calls\n                calls += 1\n                return subprocess.CompletedProcess(args, 0)\n\n            with self.assertRaises(SqxNativeGatewayError) as caught:\n                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_retester_task(\n                    project_name,\n                    expected_project_sha256=project_hash,\n                    expected_engine_sha256=engine_hash,\n                    result_archive_name="Baseline.sqx",\n                    expected_result_archive_sha256=baseline_hash,\n                )\n        self.assertEqual(caught.exception.code, "retester_result_archive_path_escape")\n        self.assertEqual(calls, 0)\n\n''',
)

insert_before(
    "tests/product/test_research_robustness.py",
    "\n    def test_revision_substitution_is_refused_before_native_execution(self) -> None:\n",
    '''\n    def test_proof_readback_requires_existing_historical_source_revision(self) -> None:\n        source_result = self._archive_bytes("baseline")\n        project = self._project_bytes(self._task_xml())\n        with TemporaryDirectory() as tmp:\n            root = Path(tmp)\n            home = self._runtime(root / "sqx", project)\n            store = FileResearchCustodyStore(root / "data")\n            historical = self._historical(store, source_result)\n            with patch("tradercockpit.research_robustness.read_current_historical_result", return_value=historical):\n                result = start_native_higher_precision(\n                    store, home, self.LAUNCHER_SHA,\n                    historical_result_entity_id=self.HISTORICAL_ENTITY,\n                    expected_historical_result_revision=self.HISTORICAL_REVISION,\n                    gateway_factory=self._gateway_factory(home, "higher-precision"),\n                )\n            historical_revision = ResearchRevisionRef.parse(self.HISTORICAL_REVISION)\n            revision_path = store.base / "revisions" / historical_revision.kind.value / historical_revision.digest[:2] / f"{historical_revision.digest}.json"\n            revision_path.unlink()\n            with self.assertRaises(ResearchRobustnessError) as caught:\n                read_native_robustness_result(store, result["validation_ref"])\n            self.assertEqual(caught.exception.code, "robustness_proof_catalog_corrupt")\n\n''',
)

# Frontend pure regressions pin the operation-generation and fresh-source rules
# used by the async start path.
replace_once(
    "tests/research-backtest-robustness.test.mjs",
    '''  robustnessExecutionAvailable,\n  robustnessStartFailureState,\n''',
    '''  robustnessExecutionAvailable,\n  robustnessOperationIsCurrent,\n  robustnessCurrentSourceIndex,\n  robustnessStartFailureState,\n''',
)
insert_before(
    "tests/research-backtest-robustness.test.mjs",
    "\ntest(\"robustness result requires durable proof identity\", () => {\n",
    '''\ntest("native start responses are generation-bound and source-currentness uses fresh results", () => {\n  assert.equal(robustnessOperationIsCurrent(7, 7, true), true);\n  assert.equal(robustnessOperationIsCurrent(7, 8, true), false);\n  assert.equal(robustnessOperationIsCurrent(7, 7, false), false);\n\n  const current = historical();\n  assert.equal(robustnessCurrentSourceIndex([current], { entity_id: current.entity_id, revision: current.revision }), 0);\n  const advanced = { ...current, revision: `tc-research-revision:historical-result:sha256:${"0".repeat(64)}` };\n  assert.equal(robustnessCurrentSourceIndex([advanced], { entity_id: current.entity_id, revision: current.revision }), -1);\n});\n\n''',
)
