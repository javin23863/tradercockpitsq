from __future__ import annotations

from pathlib import Path
import subprocess

EXPECTED_PRODUCT_HEAD = "a752a58d73be0bd6622b888fc96a84a761c92313"


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected one match, found {count}: {old[:180]!r}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


def insert_before(path: str, marker: str, addition: str) -> None:
    replace_once(path, marker, addition + marker)


# Two temporary staging commits (script + workflow) must sit directly on the
# exact reviewed product head. Refuse to patch any other candidate.
base = subprocess.check_output(["git", "rev-parse", "HEAD~2"], text=True).strip()
if base != EXPECTED_PRODUCT_HEAD:
    raise SystemExit(f"unexpected product base {base}; expected {EXPECTED_PRODUCT_HEAD}")

# ---------------------------------------------------------------------------
# 1. Native launch: the generated project root itself must retain its exact
# physical identity. A symlink/junction to another direct user/projects child
# is rejected before any SQX process can be invoked.
# ---------------------------------------------------------------------------
replace_once(
    "product/tradercockpit/sqx_gateway.py",
    '''        project_root, _ = _resolve_inside(\n            launcher.home,\n            projects_root / project_name,\n            escape_code="retester_project_path_escape",\n        )\n        if project_root.parent != projects_root or not project_root.is_dir():\n            raise SqxNativeGatewayError(\n                "retester_project_invalid",\n                "isolated Retester project is not one exact direct SQX project child",\n            )\n''',
    '''        expected_project_root = projects_root / project_name\n        project_root, _ = _resolve_inside(\n            launcher.home,\n            expected_project_root,\n            escape_code="retester_project_path_escape",\n        )\n        if project_root != expected_project_root or project_root.parent != projects_root or not project_root.is_dir():\n            raise SqxNativeGatewayError(\n                "retester_project_invalid",\n                "isolated Retester project is not the exact generated SQX project child",\n            )\n''',
)

# ---------------------------------------------------------------------------
# 2. Failed-attempt identity is returned by the originating backend operation.
# Never infer request identity from a later shared catalog snapshot.
# ---------------------------------------------------------------------------
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''class ResearchRobustnessError(ValueError):\n    def __init__(self, code: str, detail: str) -> None:\n        super().__init__(f"{code}: {detail}")\n        self.code = code\n        self.detail = detail\n''',
    '''class ResearchRobustnessError(ValueError):\n    def __init__(self, code: str, detail: str, *, attempt_ref: str | None = None) -> None:\n        super().__init__(f"{code}: {detail}")\n        self.code = code\n        self.detail = detail\n        self.attempt_ref = attempt_ref\n''',
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    ''') -> ResearchRevisionRef:\n    failed = {\n''',
    ''') -> EvidenceRef:\n    failed = {\n''',
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''    return revision.revision\n\n\ndef _completed_proof_records''',
    '''    return revision.content\n\n\ndef _completed_proof_records''',
)

replace_once(
    "product/tradercockpit/research_robustness.py",
    '''        except ResearchRetesterError as exc:\n            _failed_successor(\n                store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,\n                reason_code=exc.code, launcher_sha256=None, receipts=(), partial_side_effect=False,\n            )\n            raise ResearchRobustnessError(exc.code, exc.detail) from exc\n''',
    '''        except ResearchRetesterError as exc:\n            attempt_ref = _failed_successor(\n                store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,\n                reason_code=exc.code, launcher_sha256=None, receipts=(), partial_side_effect=False,\n            )\n            raise ResearchRobustnessError(exc.code, exc.detail, attempt_ref=str(attempt_ref)) from exc\n''',
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''        if launch_engine_sha != engine_sha:\n            code = "robustness_engine_changed_before_execution"\n            _failed_successor(\n                store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,\n                reason_code=code, launcher_sha256=None, receipts=(), partial_side_effect=False,\n            )\n            raise ResearchRobustnessError(\n                code,\n                "installed SQTradingLib.jar changed before native robustness launch",\n            )\n''',
    '''        if launch_engine_sha != engine_sha:\n            code = "robustness_engine_changed_before_execution"\n            attempt_ref = _failed_successor(\n                store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,\n                reason_code=code, launcher_sha256=None, receipts=(), partial_side_effect=False,\n            )\n            raise ResearchRobustnessError(\n                code,\n                "installed SQTradingLib.jar changed before native robustness launch",\n                attempt_ref=str(attempt_ref),\n            )\n''',
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''        except SqxNativeGatewayError as exc:\n            model = exc.read_model()\n            receipts = tuple(dict(item) for item in model["receipts"])\n            launcher = next((item.get("launcher_sha256") for item in reversed(receipts) if item.get("launcher_sha256")), None)\n            _failed_successor(\n                store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,\n                reason_code=exc.code,\n                launcher_sha256=launcher if isinstance(launcher, str) else None,\n                receipts=receipts,\n                partial_side_effect=bool(model["partial_side_effect"]),\n            )\n            raise ResearchRobustnessError(exc.code, exc.detail) from exc\n''',
    '''        except SqxNativeGatewayError as exc:\n            model = exc.read_model()\n            receipts = tuple(dict(item) for item in model["receipts"])\n            launcher = next((item.get("launcher_sha256") for item in reversed(receipts) if item.get("launcher_sha256")), None)\n            attempt_ref = _failed_successor(\n                store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,\n                reason_code=exc.code,\n                launcher_sha256=launcher if isinstance(launcher, str) else None,\n                receipts=receipts,\n                partial_side_effect=bool(model["partial_side_effect"]),\n            )\n            raise ResearchRobustnessError(exc.code, exc.detail, attempt_ref=str(attempt_ref)) from exc\n''',
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''            _failed_successor(\n                store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,\n                reason_code="robustness_receipt_invalid",\n                launcher_sha256=canonical_launcher,\n                receipts=invalid_receipt,\n                partial_side_effect=True,\n            )\n            raise ResearchRobustnessError(\n                "robustness_receipt_invalid",\n                "native Retester gateway returned an invalid Higher Precision receipt",\n            )\n''',
    '''            attempt_ref = _failed_successor(\n                store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,\n                reason_code="robustness_receipt_invalid",\n                launcher_sha256=canonical_launcher,\n                receipts=invalid_receipt,\n                partial_side_effect=True,\n            )\n            raise ResearchRobustnessError(\n                "robustness_receipt_invalid",\n                "native Retester gateway returned an invalid Higher Precision receipt",\n                attempt_ref=str(attempt_ref),\n            )\n''',
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''        except ResearchRetesterError as exc:\n            _failed_successor(\n                store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,\n                reason_code=exc.code, launcher_sha256=launcher_sha, receipts=receipts, partial_side_effect=True,\n            )\n            raise ResearchRobustnessError(exc.code, exc.detail) from exc\n        except ResearchRobustnessError as exc:\n            _failed_successor(\n                store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,\n                reason_code=exc.code, launcher_sha256=launcher_sha, receipts=receipts, partial_side_effect=True,\n            )\n            raise\n''',
    '''        except ResearchRetesterError as exc:\n            attempt_ref = _failed_successor(\n                store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,\n                reason_code=exc.code, launcher_sha256=launcher_sha, receipts=receipts, partial_side_effect=True,\n            )\n            raise ResearchRobustnessError(exc.code, exc.detail, attempt_ref=str(attempt_ref)) from exc\n        except ResearchRobustnessError as exc:\n            attempt_ref = _failed_successor(\n                store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,\n                reason_code=exc.code, launcher_sha256=launcher_sha, receipts=receipts, partial_side_effect=True,\n            )\n            raise ResearchRobustnessError(exc.code, exc.detail, attempt_ref=str(attempt_ref)) from exc\n''',
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''        except (ResearchCustodyError, OSError) as exc:\n            try:\n                _failed_successor(\n                    store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,\n                    reason_code="robustness_completion_custody_failed",\n                    launcher_sha256=launcher_sha,\n                    receipts=receipts,\n                    partial_side_effect=True,\n                )\n            except (ResearchCustodyError, OSError) as failure_exc:\n                raise ResearchRobustnessError(\n                    "robustness_completion_custody_failed",\n                    "native execution completed, but result custody and failed-state custody could not be persisted",\n                ) from failure_exc\n            detail = exc.detail if isinstance(exc, ResearchCustodyError) else str(exc)\n            raise ResearchRobustnessError("robustness_completion_custody_failed", detail) from exc\n        except ResearchRobustnessError as exc:\n            try:\n                _failed_successor(\n                    store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,\n                    reason_code=exc.code,\n                    launcher_sha256=launcher_sha,\n                    receipts=receipts,\n                    partial_side_effect=True,\n                )\n            except (ResearchCustodyError, OSError) as failure_exc:\n                raise ResearchRobustnessError(\n                    "robustness_completion_custody_failed",\n                    "native execution completed, but result validation and failed-state custody could not be persisted",\n                ) from failure_exc\n            raise\n''',
    '''        except (ResearchCustodyError, OSError) as exc:\n            try:\n                attempt_ref = _failed_successor(\n                    store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,\n                    reason_code="robustness_completion_custody_failed",\n                    launcher_sha256=launcher_sha,\n                    receipts=receipts,\n                    partial_side_effect=True,\n                )\n            except (ResearchCustodyError, OSError) as failure_exc:\n                raise ResearchRobustnessError(\n                    "robustness_completion_custody_failed",\n                    "native execution completed, but result custody and failed-state custody could not be persisted",\n                ) from failure_exc\n            detail = exc.detail if isinstance(exc, ResearchCustodyError) else str(exc)\n            raise ResearchRobustnessError(\n                "robustness_completion_custody_failed", detail, attempt_ref=str(attempt_ref)\n            ) from exc\n        except ResearchRobustnessError as exc:\n            try:\n                attempt_ref = _failed_successor(\n                    store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,\n                    reason_code=exc.code,\n                    launcher_sha256=launcher_sha,\n                    receipts=receipts,\n                    partial_side_effect=True,\n                )\n            except (ResearchCustodyError, OSError) as failure_exc:\n                raise ResearchRobustnessError(\n                    "robustness_completion_custody_failed",\n                    "native execution completed, but result validation and failed-state custody could not be persisted",\n                ) from failure_exc\n            raise ResearchRobustnessError(exc.code, exc.detail, attempt_ref=str(attempt_ref)) from exc\n''',
)

replace_once(
    "product/tradercockpit/research_retester_http.py",
    '''    return status, {\n        "error": error,\n        "reason_code": exc.code,\n        "detail": exc.detail,\n    }\n''',
    '''    payload: dict[str, object] = {\n        "error": error,\n        "reason_code": exc.code,\n        "detail": exc.detail,\n    }\n    if isinstance(exc.attempt_ref, str) and exc.attempt_ref.startswith("tc-evidence:sha256:"):\n        payload["attempt_ref"] = exc.attempt_ref\n    return status, payload\n''',
)

# ---------------------------------------------------------------------------
# 3. Frontend: consume only the backend-returned attempt identity. Never bind
# a failure by catalog set difference. Any start failure revokes launch authority
# until a successful workspace refresh re-establishes current runtime/capability.
# ---------------------------------------------------------------------------
replace_once(
    "web/research-backtest-robustness.mjs",
    '''export function robustnessNewAttemptForHistorical(attempts, historicalResult, previousAttemptRefs = []) {\n  if (!Array.isArray(previousAttemptRefs) || previousAttemptRefs.some((item) => typeof item !== "string")) {\n    throw new Error("Previous robustness attempt identities are invalid");\n  }\n  const previous = new Set(previousAttemptRefs);\n  const candidates = robustnessAttemptsForHistorical(attempts, historicalResult)\n    .filter((item) => !previous.has(item.attempt_ref));\n  return candidates.length === 1 ? candidates[0] : null;\n}\n\nexport function robustnessExecutionAvailable''',
    '''export function robustnessAttemptRefFromStartError(error) {\n  const value = error?.payload?.attempt_ref;\n  return typeof value === "string" && /^tc-evidence:sha256:[0-9a-f]{64}$/.test(value) ? value : "";\n}\n\nexport async function fetchRobustnessAttemptForStartError(error, historicalResult, fetchImpl = globalThis.fetch) {\n  const attemptRef = robustnessAttemptRefFromStartError(error);\n  if (!attemptRef) return null;\n  const attempt = await fetchRobustnessResult(attemptRef, fetchImpl);\n  if (attempt.schema !== ROBUSTNESS_ATTEMPT_SCHEMA) {\n    throw new Error("Native robustness failure did not return an attempt record");\n  }\n  const matches = robustnessAttemptsForHistorical([attempt], historicalResult);\n  if (matches.length !== 1) {\n    throw new Error("Native robustness failed attempt does not bind the originating Historical Result");\n  }\n  return attempt;\n}\n\nexport function robustnessStartFailureState(current, failedAttempt, failedAttempts, detail) {\n  return {\n    ...current,\n    phase: "failed",\n    runtimeReady: false,\n    capabilities: null,\n    validation: failedAttempt,\n    suppressCompletedPicker: !failedAttempt,\n    inFlightSource: null,\n    failedAttempts,\n    detail,\n  };\n}\n\nexport function robustnessExecutionAvailable''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''  const previousAttemptRefs = robustnessAttemptsForHistorical(state.failedAttempts, selected).map((item) => item.attempt_ref);\n  const inFlightSource = { entity_id: selected.entity_id, revision: selected.revision, previousAttemptRefs };\n''',
    '''  const inFlightSource = { entity_id: selected.entity_id, revision: selected.revision };\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''  } catch (error) {\n    let failedAttempts = state.failedAttempts;\n    let failedAttempt = null;\n    try {\n      failedAttempts = (await fetchRobustnessCatalog()).failedAttempts;\n      failedAttempt = robustnessNewAttemptForHistorical(failedAttempts, selected, inFlightSource.previousAttemptRefs);\n    } catch {}\n    if (failedAttempt) persistValidationRef(failedAttempt.attempt_ref);\n    else clearValidationRef();\n    state = { ...state, phase: "loaded", validation: failedAttempt, suppressCompletedPicker: !failedAttempt, inFlightSource: null, failedAttempts, detail: error instanceof Error ? error.message : "Native Higher Precision execution failed" };\n  }\n''',
    '''  } catch (error) {\n    let failedAttempt = null;\n    try { failedAttempt = await fetchRobustnessAttemptForStartError(error, selected); } catch {}\n    let failedAttempts = state.failedAttempts;\n    try { failedAttempts = (await fetchRobustnessCatalog()).failedAttempts; } catch {}\n    if (failedAttempt && !failedAttempts.some((item) => item.attempt_ref === failedAttempt.attempt_ref)) {\n      failedAttempts = [failedAttempt, ...failedAttempts];\n    }\n    if (failedAttempt) persistValidationRef(failedAttempt.attempt_ref);\n    else clearValidationRef();\n    state = robustnessStartFailureState(\n      state,\n      failedAttempt,\n      failedAttempts,\n      error instanceof Error ? error.message : "Native Higher Precision execution failed",\n    );\n  }\n''',
)

# ---------------------------------------------------------------------------
# Regressions.
# ---------------------------------------------------------------------------
insert_before(
    "tests/product/test_sqx_retester_gateway.py",
    '''    def test_staged_baseline_change_refuses_before_runner(self) -> None:\n''',
    '''    def test_project_root_redirection_to_other_project_refuses_before_runner(self) -> None:\n        with TemporaryDirectory() as tmp:\n            home, project_name, launcher_hash, project_hash, engine_hash, baseline_hash = self._runtime(Path(tmp))\n            project_root = home / "user/projects" / project_name\n            other = home / "user/projects/OtherProject"\n            (other / "databanks/Results").mkdir(parents=True)\n            (other / "project.cfx").write_bytes(b"exact retester project")\n            (other / "databanks/Results/Baseline.sqx").write_bytes(b"exact staged baseline")\n            (project_root / "databanks/Results/Baseline.sqx").unlink()\n            (project_root / "databanks/Results").rmdir()\n            (project_root / "databanks").rmdir()\n            (project_root / "project.cfx").unlink()\n            project_root.rmdir()\n            try:\n                project_root.symlink_to(other, target_is_directory=True)\n            except OSError as exc:\n                self.skipTest(f"directory symlink unavailable on this platform: {exc}")\n            calls = 0\n\n            def runner(*args, **kwargs):\n                nonlocal calls\n                calls += 1\n                return subprocess.CompletedProcess(args, 0)\n\n            with self.assertRaises(SqxNativeGatewayError) as caught:\n                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_retester_task(\n                    project_name,\n                    expected_project_sha256=project_hash,\n                    expected_engine_sha256=engine_hash,\n                    result_archive_name="Baseline.sqx",\n                    expected_result_archive_sha256=baseline_hash,\n                )\n        self.assertEqual(caught.exception.code, "retester_project_invalid")\n        self.assertEqual(calls, 0)\n\n''',
)
replace_once(
    "tests/product/test_research_robustness.py",
    '''            self.assertEqual(caught.exception.code, "sqx_control_timeout")\n            failed = self._current_proof_payload(store)\n''',
    '''            self.assertEqual(caught.exception.code, "sqx_control_timeout")\n            self.assertRegex(caught.exception.attempt_ref or "", r"^tc-evidence:sha256:[0-9a-f]{64}$")\n            failed = self._current_proof_payload(store)\n''',
)
replace_once(
    "tests/product/test_research_robustness.py",
    '''            attempt = catalog["failed_attempts"][0]\n            self.assertEqual(attempt["failure_reason_code"], "sqx_control_timeout")\n            self.assertEqual(read_native_robustness_result(store, attempt["attempt_ref"]), attempt)\n''',
    '''            attempt = catalog["failed_attempts"][0]\n            self.assertEqual(attempt["failure_reason_code"], "sqx_control_timeout")\n            self.assertEqual(caught.exception.attempt_ref, attempt["attempt_ref"])\n            self.assertEqual(read_native_robustness_result(store, attempt["attempt_ref"]), attempt)\n''',
)
replace_once(
    "tests/product/test_research_robustness_http_boundary.py",
    '''from tradercockpit.research_custody import FileResearchCustodyStore\n''',
    '''from tradercockpit.research_custody import FileResearchCustodyStore\nfrom tradercockpit.research_robustness import ResearchRobustnessError\n''',
)
insert_before(
    "tests/product/test_research_robustness_http_boundary.py",
    '''    def test_robustness_reopen_forwards_only_exact_validation_evidence_ref(self) -> None:\n''',
    '''    def test_failed_start_returns_only_originating_attempt_identity_when_available(self) -> None:\n        allowed = {\n            "action": "start-higher-precision",\n            "historical_result_entity_id": self.HISTORICAL_ENTITY,\n            "expected_historical_result_revision": self.HISTORICAL_REVISION,\n        }\n        attempt_ref = f"tc-evidence:sha256:{'a' * 64}"\n        with TemporaryDirectory() as tmp:\n            server, thread, _store = self._server(Path(tmp))\n            endpoint = f"http://127.0.0.1:{server.server_port}/api/research/historical-results"\n            try:\n                with patch(\n                    "tradercockpit.research_retester_http.start_native_higher_precision",\n                    side_effect=ResearchRobustnessError("sqx_control_timeout", "timed out", attempt_ref=attempt_ref),\n                ):\n                    status, payload = self._post(endpoint, allowed)\n                self.assertEqual(status, 409)\n                self.assertEqual(payload["attempt_ref"], attempt_ref)\n\n                with patch(\n                    "tradercockpit.research_retester_http.start_native_higher_precision",\n                    side_effect=ResearchRobustnessError("runtime_not_configured", "runtime unavailable"),\n                ):\n                    status, payload = self._post(endpoint, allowed)\n                self.assertEqual(status, 503)\n                self.assertNotIn("attempt_ref", payload)\n            finally:\n                server.shutdown()\n                server.server_close()\n                thread.join()\n\n''',
)

replace_once(
    "tests/research-backtest-robustness.test.mjs",
    '''  robustnessAttemptsForHistorical,\n  robustnessExecutionAvailable,\n  robustnessNewAttemptForHistorical,\n  robustnessCapabilitiesFromPayload,\n''',
    '''  robustnessAttemptsForHistorical,\n  robustnessAttemptRefFromStartError,\n  fetchRobustnessAttemptForStartError,\n  robustnessExecutionAvailable,\n  robustnessStartFailureState,\n  robustnessCapabilitiesFromPayload,\n''',
)
# Replace the fifth-round heuristic/state test with an exact-originating-request test.
start_marker = 'test("failed rerun discovery is exact and stale workspace state cannot execute", () => {'
text_path = Path("tests/research-backtest-robustness.test.mjs")
text = text_path.read_text(encoding="utf-8")
start = text.find(start_marker)
if start < 0:
    raise SystemExit("frontend test marker missing")
end = text.find("\n});", start)
if end < 0:
    raise SystemExit("frontend test end missing")
end += len("\n});")
replacement = '''test("failed start uses only backend originating attempt identity and revokes launch authority", async () => {\n  const source = historical();\n  const exactAttempt = {\n    schema: "tc.research-native-robustness-attempt.v1", state: "failed", sqx_build: "144.2953", operation: "native_retester_cross_check", method: "RetestWithHigherPrecision",\n    attempt_ref: `tc-evidence:sha256:${"1".repeat(64)}`, proof_entity_id: "tc-research:proof:v1:55555555-5555-4555-8555-555555555555", proof_revision: `tc-research-revision:proof:sha256:${"2".repeat(64)}`,\n    source_historical_result_entity_id: source.entity_id, source_historical_result_revision: source.revision,\n    source_result_archive_ref: `tc-evidence:sha256:${sourceArchiveSha}`, source_result_archive_sha256: sourceArchiveSha,\n    source_project_ref: `tc-evidence:sha256:${sourceProjectSha}`, source_project_sha256: sourceProjectSha,\n    compiled_project_ref: `tc-evidence:sha256:${compiledProjectSha}`, compiled_project_sha256: compiledProjectSha,\n    configuration_changed: true, source_task_sha256: sourceTaskSha, compiled_task_sha256: compiledTaskSha, native_settings: { Precision: "2", Spread: "3" },\n    engine_ref: `tc-evidence:sha256:${engineSha}`, engine_sha256: engineSha, launcher_sha256: launcherSha,\n    native_project_name: projectName, native_project_relative_path: `user/projects/${projectName}/project.cfx`,\n    failure_reason_code: "sqx_command_timeout", partial_side_effect: true, receipts: [{\n      action: "startOnlyTask", task: 1, project: projectName, state: "timeout", launcher_sha256: launcherSha,\n      project_sha256: compiledProjectSha, engine_sha256: engineSha, result_archive_sha256: sourceArchiveSha,\n    }],\n  };\n  const error = new Error("failed");\n  error.payload = { attempt_ref: exactAttempt.attempt_ref };\n  assert.equal(robustnessAttemptRefFromStartError(error), exactAttempt.attempt_ref);\n  let requestedBody = null;\n  const fetched = await fetchRobustnessAttemptForStartError(error, source, async (_url, options) => {\n    requestedBody = JSON.parse(options.body);\n    return response(exactAttempt);\n  });\n  assert.deepEqual(requestedBody, { action: "read-robustness", validation_ref: exactAttempt.attempt_ref });\n  assert.equal(fetched.attempt_ref, exactAttempt.attempt_ref);\n  assert.equal(robustnessAttemptRefFromStartError({ payload: { attempt_ref: "not-an-evidence-ref" } }), "");\n\n  const failedState = robustnessStartFailureState(\n    { phase: "running", runtimeReady: true, capabilities: { methods: [{ state: "ready" }] }, validation: null, suppressCompletedPicker: true, inFlightSource: source, failedAttempts: [] },\n    fetched,\n    [fetched],\n    "native start failed",\n  );\n  assert.equal(failedState.phase, "failed");\n  assert.equal(failedState.runtimeReady, false);\n  assert.equal(failedState.capabilities, null);\n  assert.equal(failedState.validation.attempt_ref, exactAttempt.attempt_ref);\n  assert.equal(robustnessExecutionAvailable(failedState.phase, true, { state: "ready" }, source), false);\n  assert.equal(robustnessExecutionAvailable("loaded", true, { state: "ready" }, source), true);\n});'''
text_path.write_text(text[:start] + replacement + text[end:], encoding="utf-8")
