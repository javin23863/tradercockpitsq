from __future__ import annotations

from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected one match, found {count}: {old[:180]!r}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


# ---------------------------------------------------------------------------
# Active prepared Proofs are process-local running state. A durable prepared
# Proof is classified as interrupted only after its owning operation has
# actually left this process and the current pointer still names that revision.
# ---------------------------------------------------------------------------
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''from hashlib import sha256\nfrom io import BytesIO\nimport json\nfrom pathlib import Path\nimport re\nfrom uuid import UUID, uuid4\n''',
    '''from contextlib import contextmanager\nfrom hashlib import sha256\nfrom io import BytesIO\nimport json\nfrom pathlib import Path\nimport re\nfrom threading import Lock\nfrom uuid import UUID, uuid4\n''',
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''_CURRENT_POINTER_TEMP_RE = re.compile(\n    r"^\\.[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.json\\.tmp-[0-9]+-[0-9a-f]{32}$"\n)\n\n\nclass ResearchRobustnessError(ValueError):\n''',
    '''_CURRENT_POINTER_TEMP_RE = re.compile(\n    r"^\\.[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.json\\.tmp-[0-9]+-[0-9a-f]{32}$"\n)\n_ACTIVE_PROOF_LOCK = Lock()\n_ACTIVE_PROOF_ENTITIES: set[str] = set()\n\n\n@contextmanager\ndef _active_proof(entity: ResearchEntityId):\n    key = str(entity)\n    with _ACTIVE_PROOF_LOCK:\n        if key in _ACTIVE_PROOF_ENTITIES:\n            raise ResearchRobustnessError("robustness_proof_active_duplicate", "robustness Proof is already active in this process")\n        _ACTIVE_PROOF_ENTITIES.add(key)\n    try:\n        yield\n    finally:\n        with _ACTIVE_PROOF_LOCK:\n            _ACTIVE_PROOF_ENTITIES.discard(key)\n\n\ndef _proof_is_active(entity: ResearchEntityId) -> bool:\n    with _ACTIVE_PROOF_LOCK:\n        return str(entity) in _ACTIVE_PROOF_ENTITIES\n\n\nclass ResearchRobustnessError(ValueError):\n''',
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''        attempt_state = raw.get("state")\n        if attempt_state not in {"failed", "prepared"}:\n            continue\n        if set(raw) != required:\n''',
    '''        attempt_state = raw.get("state")\n        if attempt_state not in {"failed", "prepared"}:\n            continue\n        if attempt_state == "prepared":\n            if _proof_is_active(entity):\n                continue\n            if store.current(entity) != revision:\n                continue\n        if set(raw) != required:\n''',
)

# Scope every visible prepared Proof to the whole post-creation operation. The
# context manager clears on normal return and BaseException unwinding; after a
# hard process restart the in-memory active set is naturally empty.
robustness_path = Path("product/tradercockpit/research_robustness.py")
text = robustness_path.read_text(encoding="utf-8")
marker = "    proof_entity = store.create_entity(ResearchKind.PROOF)\n"
if text.count(marker) != 1:
    raise SystemExit(f"research_robustness.py: expected one proof creation marker, found {text.count(marker)}")
head, tail = text.split(marker, 1)
if "\n\ndef " in tail or "\n\nclass " in tail:
    raise SystemExit("research_robustness.py: start function is no longer final top-level definition")
indented_tail = "".join(("    " + line) if line.strip() else line for line in tail.splitlines(keepends=True))
robustness_path.write_text(
    head + marker + "    with _active_proof(proof_entity):\n" + indented_tail,
    encoding="utf-8",
)

# ---------------------------------------------------------------------------
# The staged Results databank must remain physically under the generated
# isolated project. In-home symlink/junction redirection is rejected before SQX.
# ---------------------------------------------------------------------------
replace_once(
    "product/tradercockpit/sqx_gateway.py",
    '''            results_root, _ = _resolve_inside(\n                launcher.home,\n                project_root / "databanks/Results",\n                escape_code="retester_result_archive_path_escape",\n            )\n            result_file, result_relative = _resolve_inside(\n''',
    '''            databanks_root, _ = _resolve_inside(\n                launcher.home,\n                project_root / "databanks",\n                escape_code="retester_result_archive_path_escape",\n            )\n            if databanks_root != project_root / "databanks" or databanks_root.parent != project_root or not databanks_root.is_dir():\n                raise SqxNativeGatewayError(\n                    "retester_result_archive_path_escape",\n                    "isolated Retester databanks directory was redirected outside the generated project",\n                )\n            results_root, _ = _resolve_inside(\n                launcher.home,\n                databanks_root / "Results",\n                escape_code="retester_result_archive_path_escape",\n            )\n            if results_root != databanks_root / "Results" or results_root.parent != databanks_root or not results_root.is_dir():\n                raise SqxNativeGatewayError(\n                    "retester_result_archive_path_escape",\n                    "isolated Retester Results databank was redirected outside the generated project",\n                )\n            result_file, result_relative = _resolve_inside(\n''',
)

# ---------------------------------------------------------------------------
# UI state: only a successfully refreshed workspace can execute. Starting a new
# run clears the old exact URL identity immediately. A failed run selects the
# one newly discovered exact attempt when unambiguous; otherwise it stays
# deliberately unbound and hides the completed-run picker.
# ---------------------------------------------------------------------------
replace_once(
    "web/research-backtest-robustness.mjs",
    '''export function robustnessAttemptsForHistorical(attempts, historicalResult) {\n  if (!Array.isArray(attempts)) throw new Error("Native robustness attempt catalog is invalid");\n  const source = historicalResultFromPayload(historicalResult);\n  return attempts.filter((item) => (\n    item.source_historical_result_entity_id === source.entity_id\n    && item.source_historical_result_revision === source.revision\n  ));\n}\n\nexport function robustnessResultForHistorical''',
    '''export function robustnessAttemptsForHistorical(attempts, historicalResult) {\n  if (!Array.isArray(attempts)) throw new Error("Native robustness attempt catalog is invalid");\n  const source = historicalResultFromPayload(historicalResult);\n  return attempts.filter((item) => (\n    item.source_historical_result_entity_id === source.entity_id\n    && item.source_historical_result_revision === source.revision\n  ));\n}\n\nexport function robustnessNewAttemptForHistorical(attempts, historicalResult, previousAttemptRefs = []) {\n  if (!Array.isArray(previousAttemptRefs) || previousAttemptRefs.some((item) => typeof item !== "string")) {\n    throw new Error("Previous robustness attempt identities are invalid");\n  }\n  const previous = new Set(previousAttemptRefs);\n  const candidates = robustnessAttemptsForHistorical(attempts, historicalResult)\n    .filter((item) => !previous.has(item.attempt_ref));\n  return candidates.length === 1 ? candidates[0] : null;\n}\n\nexport function robustnessExecutionAvailable(phase, runtimeReady, higherCapability, selected) {\n  return phase === "loaded" && runtimeReady === true && higherCapability?.state === "ready" && Boolean(selected);\n}\n\nexport function robustnessResultForHistorical''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''let state = { phase: "idle", results: [], selectedIndex: 0, runtimeReady: false, capabilities: null, catalog: [], failedAttempts: [], validation: null, inFlightSource: null, detail: "" };\n''',
    '''let state = { phase: "idle", results: [], selectedIndex: 0, runtimeReady: false, capabilities: null, catalog: [], failedAttempts: [], validation: null, suppressCompletedPicker: false, inFlightSource: null, detail: "" };\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''  const higherCapability = current.capabilities?.methods?.find((item) => item.method === HIGHER_PRECISION_METHOD) || null;\n  const locked = current.phase === "running";\n  const canRun = !["loading", "running"].includes(current.phase) && current.runtimeReady && higherCapability?.state === "ready" && selected;\n''',
    '''  const higherCapability = current.capabilities?.methods?.find((item) => item.method === HIGHER_PRECISION_METHOD) || null;\n  const locked = current.phase !== "loaded";\n  const canRun = robustnessExecutionAvailable(current.phase, current.runtimeReady, higherCapability, selected);\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''  const validationPicker = current.validation?.schema === ROBUSTNESS_ATTEMPT_SCHEMA\n    ? `<p class="field-help" data-robustness-attempt-selection>Exact native attempt selected: <code>${escapeHtml(short(current.validation.attempt_ref))}</code>. Completed-run selection is hidden so custody identities cannot be cross-displayed.</p>`\n    : matchingValidations.length\n''',
    '''  const validationPicker = current.validation?.schema === ROBUSTNESS_ATTEMPT_SCHEMA\n    ? `<p class="field-help" data-robustness-attempt-selection>Exact native attempt selected: <code>${escapeHtml(short(current.validation.attempt_ref))}</code>. Completed-run selection is hidden so custody identities cannot be cross-displayed.</p>`\n    : current.suppressCompletedPicker\n      ? '<p class="field-help" data-robustness-unbound-attempt>No exact completed or failed attempt is selected for the most recent execution.</p>'\n    : matchingValidations.length\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''    let selectedIndex = 0;\n    let validation = completed[0] ? robustnessResultForHistorical(catalog, completed[0]) : null;\n    let detail = "";\n''',
    '''    let selectedIndex = 0;\n    let validation = completed[0] ? robustnessResultForHistorical(catalog, completed[0]) : null;\n    let suppressCompletedPicker = false;\n    let detail = "";\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''          validation = null;\n          clearValidationRef();\n          detail = "Saved robustness result source Historical Result is no longer current; receipt was not displayed.";\n''',
    '''          validation = null;\n          suppressCompletedPicker = true;\n          clearValidationRef();\n          detail = "Saved robustness result source Historical Result is no longer current; receipt was not displayed.";\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''      } catch (error) {\n        validation = null;\n        clearValidationRef();\n        detail = `Saved robustness result unavailable: ${error instanceof Error ? error.message : "readback failed"}`;\n''',
    '''      } catch (error) {\n        validation = null;\n        suppressCompletedPicker = true;\n        clearValidationRef();\n        detail = `Saved robustness result unavailable: ${error instanceof Error ? error.message : "readback failed"}`;\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''      failedAttempts,\n      validation,\n      inFlightSource: null,\n''',
    '''      failedAttempts,\n      validation,\n      suppressCompletedPicker,\n      inFlightSource: null,\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''    state = { ...state, phase: "failed", detail: error instanceof Error ? error.message : "Native robustness workspace unavailable" };\n''',
    '''    state = { ...state, phase: "failed", runtimeReady: false, capabilities: null, suppressCompletedPicker: true, inFlightSource: null, detail: error instanceof Error ? error.message : "Native robustness workspace unavailable" };\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''  const inFlightSource = { entity_id: selected.entity_id, revision: selected.revision };\n  state = { ...state, phase: "running", inFlightSource, validation: null, detail: "Running Higher Precision in SQX…" };\n''',
    '''  const previousAttemptRefs = robustnessAttemptsForHistorical(state.failedAttempts, selected).map((item) => item.attempt_ref);\n  const inFlightSource = { entity_id: selected.entity_id, revision: selected.revision, previousAttemptRefs };\n  clearValidationRef();\n  state = { ...state, phase: "running", inFlightSource, validation: null, suppressCompletedPicker: true, detail: "Running Higher Precision in SQX…" };\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''      state = { ...state, phase: "loaded", validation: null, inFlightSource: null, detail: "Native result captured, but its source Historical Result is no longer current; receipt was not cross-displayed." };\n    } else {\n      persistValidationRef(validation.validation_ref);\n      state = { ...state, phase: "loaded", selectedIndex: sourceIndex, validation, inFlightSource: null, catalog: [validation, ...state.catalog.filter((item) => item.validation_ref !== validation.validation_ref)], detail: "Native Higher Precision result captured. Producer verdict remains unread." };\n    }\n  } catch (error) {\n    let failedAttempts = state.failedAttempts;\n    try { failedAttempts = (await fetchRobustnessCatalog()).failedAttempts; } catch {}\n    state = { ...state, phase: "loaded", inFlightSource: null, failedAttempts, detail: error instanceof Error ? error.message : "Native Higher Precision execution failed" };\n''',
    '''      state = { ...state, phase: "loaded", validation: null, suppressCompletedPicker: true, inFlightSource: null, detail: "Native result captured, but its source Historical Result is no longer current; receipt was not cross-displayed." };\n    } else {\n      persistValidationRef(validation.validation_ref);\n      state = { ...state, phase: "loaded", selectedIndex: sourceIndex, validation, suppressCompletedPicker: false, inFlightSource: null, catalog: [validation, ...state.catalog.filter((item) => item.validation_ref !== validation.validation_ref)], detail: "Native Higher Precision result captured. Producer verdict remains unread." };\n    }\n  } catch (error) {\n    let failedAttempts = state.failedAttempts;\n    let failedAttempt = null;\n    try {\n      failedAttempts = (await fetchRobustnessCatalog()).failedAttempts;\n      failedAttempt = robustnessNewAttemptForHistorical(failedAttempts, selected, inFlightSource.previousAttemptRefs);\n    } catch {}\n    if (failedAttempt) persistValidationRef(failedAttempt.attempt_ref);\n    else clearValidationRef();\n    state = { ...state, phase: "loaded", validation: failedAttempt, suppressCompletedPicker: !failedAttempt, inFlightSource: null, failedAttempts, detail: error instanceof Error ? error.message : "Native Higher Precision execution failed" };\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''      state = { ...state, selectedIndex, validation, detail: "" };\n''',
    '''      state = { ...state, selectedIndex, validation, suppressCompletedPicker: false, detail: "" };\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''      state = { ...state, validation, detail: "" };\n''',
    '''      state = { ...state, validation, suppressCompletedPicker: false, detail: "" };\n''',
)

# ---------------------------------------------------------------------------
# Regressions
# ---------------------------------------------------------------------------
replace_once(
    "tests/product/test_research_robustness.py",
    '''from pathlib import Path\nfrom tempfile import TemporaryDirectory\nimport unittest\n''',
    '''from pathlib import Path\nfrom tempfile import TemporaryDirectory\nfrom threading import Event, Thread\nimport unittest\n''',
)
active_test = '''\n    def test_active_prepared_proof_is_not_reported_as_interrupted(self) -> None:\n        source_result = self._archive_bytes("baseline")\n        project = self._project_bytes(self._task_xml())\n        with TemporaryDirectory() as tmp:\n            root = Path(tmp)\n            home = self._runtime(root / "sqx", project)\n            store = FileResearchCustodyStore(root / "data")\n            historical = self._historical(store, source_result)\n            BaseGateway = self._gateway_factory(home, "higher-precision")\n            entered = Event()\n            release = Event()\n\n            class BlockingGateway(BaseGateway):\n                def launch_retester_task(self, *args, **kwargs):\n                    entered.set()\n                    if not release.wait(5):\n                        raise AssertionError("blocking gateway was not released")\n                    return super().launch_retester_task(*args, **kwargs)\n\n            outcome: dict[str, object] = {}\n\n            def worker() -> None:\n                try:\n                    outcome["result"] = start_native_higher_precision(\n                        store, home, self.LAUNCHER_SHA,\n                        historical_result_entity_id=self.HISTORICAL_ENTITY,\n                        expected_historical_result_revision=self.HISTORICAL_REVISION,\n                        gateway_factory=BlockingGateway,\n                    )\n                except BaseException as exc:  # pragma: no cover - asserted below\n                    outcome["error"] = exc\n\n            with patch("tradercockpit.research_robustness.read_current_historical_result", return_value=historical):\n                thread = Thread(target=worker)\n                thread.start()\n                self.assertTrue(entered.wait(5))\n                catalog = list_native_robustness_results(store)\n                self.assertEqual(catalog["results"], [])\n                self.assertEqual(catalog["failed_attempts"], [])\n                release.set()\n                thread.join(5)\n            self.assertFalse(thread.is_alive())\n            self.assertNotIn("error", outcome)\n            self.assertEqual(len(list_native_robustness_results(store)["results"]), 1)\n\n'''
replace_once(
    "tests/product/test_research_robustness.py",
    '''    def test_prepared_proof_left_by_uncaught_termination_reopens_as_interrupted(self) -> None:\n''',
    active_test + '''    def test_prepared_proof_left_by_uncaught_termination_reopens_as_interrupted(self) -> None:\n''',
)

symlink_test = '''\n    def test_results_databank_redirection_inside_sqx_home_refuses_before_runner(self) -> None:\n        with TemporaryDirectory() as tmp:\n            home, project_name, launcher_hash, project_hash, engine_hash, baseline_hash = self._runtime(Path(tmp))\n            project_root = home / "user/projects" / project_name\n            results = project_root / "databanks/Results"\n            alternate = home / "user/projects/OtherProject/databanks/Results"\n            alternate.mkdir(parents=True)\n            (alternate / "Baseline.sqx").write_bytes(b"exact staged baseline")\n            (results / "Baseline.sqx").unlink()\n            results.rmdir()\n            try:\n                results.symlink_to(alternate, target_is_directory=True)\n            except OSError as exc:\n                self.skipTest(f"directory symlink unavailable on this platform: {exc}")\n            calls = 0\n\n            def runner(*args, **kwargs):\n                nonlocal calls\n                calls += 1\n                return subprocess.CompletedProcess(args, 0)\n\n            with self.assertRaises(SqxNativeGatewayError) as caught:\n                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_retester_task(\n                    project_name,\n                    expected_project_sha256=project_hash,\n                    expected_engine_sha256=engine_hash,\n                    result_archive_name="Baseline.sqx",\n                    expected_result_archive_sha256=baseline_hash,\n                )\n        self.assertEqual(caught.exception.code, "retester_result_archive_path_escape")\n        self.assertEqual(calls, 0)\n\n'''
replace_once(
    "tests/product/test_sqx_retester_gateway.py",
    '''    def test_staged_baseline_change_refuses_before_runner(self) -> None:\n''',
    symlink_test + '''    def test_staged_baseline_change_refuses_before_runner(self) -> None:\n''',
)

replace_once(
    "tests/research-backtest-robustness.test.mjs",
    '''  robustnessAttemptFromPayload,\n  robustnessAttemptsForHistorical,\n''',
    '''  robustnessAttemptFromPayload,\n  robustnessAttemptsForHistorical,\n  robustnessExecutionAvailable,\n  robustnessNewAttemptForHistorical,\n''',
)
frontend_test = '''\n\ntest("failed rerun discovery is exact and stale workspace state cannot execute", () => {\n  const source = historical();\n  const prior = {\n    attempt_ref: `tc-evidence:sha256:${"1".repeat(64)}`,\n    source_historical_result_entity_id: source.entity_id,\n    source_historical_result_revision: source.revision,\n  };\n  const fresh = {\n    ...prior,\n    attempt_ref: `tc-evidence:sha256:${"2".repeat(64)}`,\n  };\n  assert.equal(\n    robustnessNewAttemptForHistorical([prior, fresh], source, [prior.attempt_ref])?.attempt_ref,\n    fresh.attempt_ref,\n  );\n  assert.equal(\n    robustnessNewAttemptForHistorical([prior, fresh], source, []),\n    null,\n  );\n  assert.equal(robustnessExecutionAvailable("failed", true, { state: "ready" }, source), false);\n  assert.equal(robustnessExecutionAvailable("loading", true, { state: "ready" }, source), false);\n  assert.equal(robustnessExecutionAvailable("loaded", true, { state: "ready" }, source), true);\n});\n'''
path = Path("tests/research-backtest-robustness.test.mjs")
path.write_text(path.read_text(encoding="utf-8") + frontend_test, encoding="utf-8")

# Temporary correction artifacts must not enter the merge candidate.
for obsolete in (
    ".github/scripts/apply_robustness_review_round5.py",
    ".github/workflows/apply-robustness-review-round5.yml",
):
    Path(obsolete).unlink(missing_ok=True)

print("fifth-round robustness review corrections applied")
