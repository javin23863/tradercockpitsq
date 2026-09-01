from __future__ import annotations

from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected one match, found {count}: {old[:160]!r}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


# A current prepared Proof after restart is execution-unknown custody, not absence.
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''        if not isinstance(raw, dict) or raw.get("schema") != ROBUSTNESS_ATTEMPT_SCHEMA or raw.get("state") != "failed":\n            continue\n        if set(raw) != required:\n''',
    '''        if not isinstance(raw, dict) or raw.get("schema") != ROBUSTNESS_ATTEMPT_SCHEMA:\n            continue\n        attempt_state = raw.get("state")\n        if attempt_state not in {"failed", "prepared"}:\n            continue\n        if set(raw) != required:\n''',
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''        if stored.parent_revision is None:\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness proof has no prepared parent")\n        parent_revision = store.read_revision(stored.parent_revision)\n        try:\n            prepared = json.loads(store.read_revision_content(stored.parent_revision))\n        except (UnicodeDecodeError, json.JSONDecodeError) as exc:\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "prepared robustness proof is unreadable") from exc\n        if not isinstance(prepared, dict) or prepared.get("schema") != ROBUSTNESS_ATTEMPT_SCHEMA or prepared.get("state") != "prepared":\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness proof parent is not one prepared native attempt")\n''',
    '''        if attempt_state == "prepared":\n            if stored.parent_revision is not None:\n                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "prepared robustness proof unexpectedly has a parent")\n            parent_revision = stored\n            prepared = raw\n        else:\n            if stored.parent_revision is None:\n                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness proof has no prepared parent")\n            parent_revision = store.read_revision(stored.parent_revision)\n            try:\n                prepared = json.loads(store.read_revision_content(stored.parent_revision))\n            except (UnicodeDecodeError, json.JSONDecodeError) as exc:\n                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "prepared robustness proof is unreadable") from exc\n            if not isinstance(prepared, dict) or prepared.get("schema") != ROBUSTNESS_ATTEMPT_SCHEMA or prepared.get("state") != "prepared":\n                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness proof parent is not one prepared native attempt")\n''',
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''        if not isinstance(raw.get("failure_reason_code"), str) or not raw["failure_reason_code"]:\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness reason is invalid")\n''',
    '''        if attempt_state == "failed":\n            if not isinstance(raw.get("failure_reason_code"), str) or not raw["failure_reason_code"]:\n                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness reason is invalid")\n        elif raw.get("failure_reason_code") is not None:\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "prepared robustness proof already claims a failure reason")\n''',
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''        if raw["partial_side_effect"] and launcher is None:\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness side-effect state lacks launcher custody")\n        results.append({\n            **raw,\n            "attempt_ref": str(stored.content),\n            "proof_entity_id": str(entity),\n            "proof_revision": str(revision),\n        })\n''',
    '''        invalid_receipt = any(item.get("state") == "invalid_receipt" for item in receipts)\n        if raw["partial_side_effect"] and launcher is None and not invalid_receipt:\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness side-effect state lacks launcher custody")\n        exposed = dict(raw)\n        if attempt_state == "prepared":\n            if launcher is not None or receipts or raw["partial_side_effect"] or raw.get("failure_reason_code") is not None:\n                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "prepared robustness proof already claims execution completion state")\n            exposed.update({\n                "state": "interrupted",\n                "partial_side_effect": True,\n                "failure_reason_code": "robustness_attempt_interrupted",\n            })\n        results.append({\n            **exposed,\n            "attempt_ref": str(stored.content),\n            "proof_entity_id": str(entity),\n            "proof_revision": str(revision),\n        })\n''',
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''    """List completed runs and failed native attempts from durable Research custody."""\n''',
    '''    """List completed runs plus failed/interrupted native attempts from durable Research custody."""\n''',
)

# Never persist the malformed object that failed receipt validation. Preserve only
# command identities known by TraderCockpit and mark the nested receipt invalid.
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''    if not receipt_valid:\n        _failed_successor(\n            store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,\n            reason_code="robustness_receipt_invalid",\n            launcher_sha256=raw_launcher if isinstance(raw_launcher, str) else None,\n            receipts=receipt_items,\n            partial_side_effect=True,\n        )\n        raise ResearchRobustnessError(\n''',
    '''    if not receipt_valid:\n        nested_launcher = receipt_items[0].get("launcher_sha256") if len(receipt_items) == 1 else None\n        canonical_launcher = next((\n            value for value in (raw_launcher, nested_launcher, trusted_launcher_sha256)\n            if isinstance(value, str) and _DIGEST_RE.fullmatch(value) is not None\n        ), None)\n        invalid_receipt = ({\n            "action": "startOnlyTask",\n            "project": project_name,\n            "task": 1,\n            "state": "invalid_receipt",\n            "sqx_build": SQX_BUILD,\n            "launcher_sha256": canonical_launcher,\n            "project_sha256": compiled_project_sha,\n            "engine_sha256": engine_sha,\n            "result_archive_name": historical["result_archive_name"],\n            "result_archive_relative_path": f"user/projects/{project_name}/databanks/Results/{historical['result_archive_name']}",\n            "result_archive_sha256": source_result_sha,\n            "reason_code": "robustness_receipt_invalid",\n        },)\n        _failed_successor(\n            store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,\n            reason_code="robustness_receipt_invalid",\n            launcher_sha256=canonical_launcher,\n            receipts=invalid_receipt,\n            partial_side_effect=True,\n        )\n        raise ResearchRobustnessError(\n''',
)

# Public readback explicitly distinguishes interrupted/unknown custody from a
# failed attempt with a native receipt.
replace_once(
    "product/tradercockpit/research_retester_http.py",
    '''    if record.get("schema") == ROBUSTNESS_ATTEMPT_SCHEMA and record.get("state") == "failed":\n        launched_states = {"completed", "timeout", "rejected", "invalid_receipt"}\n''',
    '''    if record.get("schema") == ROBUSTNESS_ATTEMPT_SCHEMA and record.get("state") in {"failed", "interrupted"}:\n        launched_states = {"completed", "timeout", "rejected", "invalid_receipt"}\n        if record.get("state") == "interrupted":\n            if (\n                not isinstance(record.get("attempt_ref"), str)\n                or not record["attempt_ref"].startswith("tc-evidence:sha256:")\n                or not isinstance(proof_entity_id, str)\n                or not proof_entity_id.startswith("tc-research:proof:v1:")\n                or not isinstance(proof_revision, str)\n                or not proof_revision.startswith("tc-research-revision:proof:sha256:")\n                or record.get("failure_reason_code") != "robustness_attempt_interrupted"\n                or record.get("partial_side_effect") is not True\n                or record.get("launcher_sha256") is not None\n                or receipts != []\n            ):\n                raise ResearchRobustnessError(\n                    "robustness_record_corrupt",\n                    "interrupted native robustness attempt is not bound to durable prepared Proof custody",\n                )\n            return record\n''',
)

# Frontend readback accepts the explicit interrupted state and keeps malformed
# receipt failures visible even when the launcher identity itself was invalid.
replace_once(
    "web/research-backtest-robustness.mjs",
    '''    || payload.state !== "failed"\n''',
    '''    || !["failed", "interrupted"].includes(payload.state)\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''  const launchedStates = new Set(["completed", "timeout", "rejected", "invalid_receipt"]);\n''',
    '''  if (payload.state === "interrupted") {\n    if (\n      payload.failure_reason_code !== "robustness_attempt_interrupted"\n      || payload.partial_side_effect !== true\n      || payload.launcher_sha256 !== null\n      || payload.receipts.length !== 0\n    ) {\n      throw new Error("Native robustness interrupted-attempt custody is inconsistent");\n    }\n    return payload;\n  }\n  const launchedStates = new Set(["completed", "timeout", "rejected", "invalid_receipt"]);\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''  const launched = payload.receipts.some((item) => launchedStates.has(item.state));\n  if (launched !== payload.partial_side_effect || (payload.partial_side_effect && !payload.launcher_sha256)) {\n''',
    '''  const launched = payload.receipts.some((item) => launchedStates.has(item.state));\n  const invalidReceipt = payload.receipts.some((item) => item.state === "invalid_receipt");\n  if (launched !== payload.partial_side_effect || (payload.partial_side_effect && !payload.launcher_sha256 && !invalidReceipt)) {\n''',
)

# Failed/interrupted cards are scoped to the selected exact Historical Result.
insert_after = '''export function robustnessResultsForHistorical(catalog, historicalResult) {\n  if (!Array.isArray(catalog)) throw new Error("Native robustness catalog is invalid");\n  const source = historicalResultFromPayload(historicalResult);\n  return catalog.filter((item) => (\n    item.source_historical_result_entity_id === source.entity_id\n    && item.source_historical_result_revision === source.revision\n  ));\n}\n'''
replace_once(
    "web/research-backtest-robustness.mjs",
    insert_after,
    insert_after + '''\nexport function robustnessAttemptsForHistorical(attempts, historicalResult) {\n  if (!Array.isArray(attempts)) throw new Error("Native robustness attempt catalog is invalid");\n  const source = historicalResultFromPayload(historicalResult);\n  return attempts.filter((item) => (\n    item.source_historical_result_entity_id === source.entity_id\n    && item.source_historical_result_revision === source.revision\n  ));\n}\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''  const matchingValidations = selected ? robustnessResultsForHistorical(current.catalog, selected) : [];\n''',
    '''  const matchingValidations = selected ? robustnessResultsForHistorical(current.catalog, selected) : [];\n  const matchingAttempts = selected ? robustnessAttemptsForHistorical(current.failedAttempts, selected) : [];\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''  const validationPicker = matchingValidations.length\n    ? `<label class="field-label" for="robustness-validation-result">Captured robustness run</label><select id="robustness-validation-result" class="idea-editor" ${locked ? "disabled" : ""}>${matchingValidations.length > 1 && !current.validation ? '<option value="" selected>Choose exact robustness run</option>' : ""}${matchingValidations.map((item) => `<option value="${escapeHtml(item.validation_ref)}" ${current.validation?.validation_ref === item.validation_ref ? "selected" : ""}>${escapeHtml(short(item.validation_ref))} · ${escapeHtml(short(item.proof_revision))}</option>`).join("")}</select>`\n    : '<p class="field-help">No completed robustness run is registered for the selected Historical Result.</p>';\n''',
    '''  const validationPicker = current.validation?.schema === ROBUSTNESS_ATTEMPT_SCHEMA\n    ? `<p class="field-help" data-robustness-attempt-selection>Exact native attempt selected: <code>${escapeHtml(short(current.validation.attempt_ref))}</code>. Completed-run selection is hidden so custody identities cannot be cross-displayed.</p>`\n    : matchingValidations.length\n      ? `<label class="field-label" for="robustness-validation-result">Captured robustness run</label><select id="robustness-validation-result" class="idea-editor" ${locked ? "disabled" : ""}>${matchingValidations.length > 1 && !current.validation ? '<option value="" selected>Choose exact robustness run</option>' : ""}${matchingValidations.map((item) => `<option value="${escapeHtml(item.validation_ref)}" ${current.validation?.validation_ref === item.validation_ref ? "selected" : ""}>${escapeHtml(short(item.validation_ref))} · ${escapeHtml(short(item.proof_revision))}</option>`).join("")}</select>`\n      : '<p class="field-help">No completed robustness run is registered for the selected Historical Result.</p>';\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''${current.failedAttempts.length ? `<div class="requirement-list" data-robustness-failed-attempts>${current.failedAttempts.map((item) => `<div class="requirement-item"><div><strong>Failed native attempt</strong><span class="status-badge status-unavailable"><span class="status-dot"></span>${escapeHtml(item.failure_reason_code)}</span></div><p><code>${escapeHtml(short(item.attempt_ref))}</code> · partial side effect ${item.partial_side_effect ? "possible" : "not observed"}</p></div>`).join("")}</div>` : ""}</section>\n''',
    '''${matchingAttempts.length ? `<div class="requirement-list" data-robustness-failed-attempts>${matchingAttempts.map((item) => `<div class="requirement-item"><div><strong>${item.state === "interrupted" ? "Interrupted native attempt" : "Failed native attempt"}</strong><span class="status-badge status-unavailable"><span class="status-dot"></span>${escapeHtml(item.failure_reason_code)}</span></div><p><code>${escapeHtml(short(item.attempt_ref))}</code> · partial side effect ${item.partial_side_effect ? "possible" : "not observed"}</p></div>`).join("")}</div>` : ""}</section>\n''',
)

# Backend regression: malformed success receipt becomes canonical failed custody;
# uncaught termination after gateway entry leaves a visible interrupted attempt.
backend_test = '''\n    def test_malformed_success_receipt_is_normalized_to_durable_invalid_receipt(self) -> None:\n        source_result = self._archive_bytes("baseline")\n        project = self._project_bytes(self._task_xml())\n        with TemporaryDirectory() as tmp:\n            root = Path(tmp)\n            home = self._runtime(root / "sqx", project)\n            store = FileResearchCustodyStore(root / "data")\n            historical = self._historical(store, source_result)\n            ValidGateway = self._gateway_factory(home, "higher-precision")\n\n            class MalformedGateway(ValidGateway):\n                def launch_retester_task(self, *args, **kwargs):\n                    result = super().launch_retester_task(*args, **kwargs)\n                    result["launcher_sha256"] = "not-a-digest"\n                    return result\n\n            with patch("tradercockpit.research_robustness.read_current_historical_result", return_value=historical):\n                with self.assertRaises(ResearchRobustnessError) as caught:\n                    start_native_higher_precision(\n                        store, home, self.LAUNCHER_SHA,\n                        historical_result_entity_id=self.HISTORICAL_ENTITY,\n                        expected_historical_result_revision=self.HISTORICAL_REVISION,\n                        gateway_factory=MalformedGateway,\n                    )\n            self.assertEqual(caught.exception.code, "robustness_receipt_invalid")\n            catalog = list_native_robustness_results(store)\n            self.assertEqual(len(catalog["failed_attempts"]), 1)\n            attempt = catalog["failed_attempts"][0]\n            self.assertEqual(attempt["state"], "failed")\n            self.assertEqual(attempt["failure_reason_code"], "robustness_receipt_invalid")\n            self.assertTrue(attempt["partial_side_effect"])\n            self.assertEqual(attempt["receipts"][0]["state"], "invalid_receipt")\n            self.assertEqual(attempt["receipts"][0]["project_sha256"], attempt["compiled_project_sha256"])\n            self.assertEqual(attempt["receipts"][0]["engine_sha256"], attempt["engine_sha256"])\n            self.assertEqual(attempt["receipts"][0]["result_archive_sha256"], attempt["source_result_archive_sha256"])\n            self.assertEqual(read_native_robustness_result(store, attempt["attempt_ref"]), attempt)\n\n    def test_prepared_proof_left_by_uncaught_termination_reopens_as_interrupted(self) -> None:\n        source_result = self._archive_bytes("baseline")\n        project = self._project_bytes(self._task_xml())\n        with TemporaryDirectory() as tmp:\n            root = Path(tmp)\n            home = self._runtime(root / "sqx", project)\n            store = FileResearchCustodyStore(root / "data")\n            historical = self._historical(store, source_result)\n\n            class TerminatingGateway:\n                def __init__(self, sqx_home, trusted_launcher_sha256):\n                    self.home = Path(sqx_home)\n\n                def launch_retester_task(self, project_name, **kwargs):\n                    marker = self.home / "user/projects" / project_name / "native-started.marker"\n                    marker.write_text("native launch may have started", encoding="utf-8")\n                    raise KeyboardInterrupt("simulated process termination")\n\n            with patch("tradercockpit.research_robustness.read_current_historical_result", return_value=historical):\n                with self.assertRaises(KeyboardInterrupt):\n                    start_native_higher_precision(\n                        store, home, self.LAUNCHER_SHA,\n                        historical_result_entity_id=self.HISTORICAL_ENTITY,\n                        expected_historical_result_revision=self.HISTORICAL_REVISION,\n                        gateway_factory=TerminatingGateway,\n                    )\n            catalog = list_native_robustness_results(store)\n            self.assertEqual(catalog["results"], [])\n            self.assertEqual(len(catalog["failed_attempts"]), 1)\n            attempt = catalog["failed_attempts"][0]\n            self.assertEqual(attempt["state"], "interrupted")\n            self.assertEqual(attempt["failure_reason_code"], "robustness_attempt_interrupted")\n            self.assertTrue(attempt["partial_side_effect"])\n            self.assertIsNone(attempt["launcher_sha256"])\n            self.assertEqual(attempt["receipts"], [])\n            self.assertEqual(read_native_robustness_result(store, attempt["attempt_ref"]), attempt)\n'''
replace_once(
    "tests/product/test_research_robustness.py",
    '''    def test_invalid_validation_ref_is_typed(self) -> None:\n''',
    backend_test + '''\n    def test_invalid_validation_ref_is_typed(self) -> None:\n''',
)

# Frontend parser/filter regressions for interrupted custody and baseline scoping.
replace_once(
    "tests/research-backtest-robustness.test.mjs",
    '''  robustnessAttemptFromPayload,\n''',
    '''  robustnessAttemptFromPayload,\n  robustnessAttemptsForHistorical,\n''',
)
frontend_test = '''\n\ntest("interrupted attempts remain readable and attempt lists are exact-baseline scoped", () => {\n  const interrupted = {\n    schema: "tc.research-native-robustness-attempt.v1", state: "interrupted", sqx_build: "144.2953", operation: "native_retester_cross_check", method: "RetestWithHigherPrecision",\n    attempt_ref: `tc-evidence:sha256:${"0".repeat(64)}`, proof_entity_id: "tc-research:proof:v1:55555555-5555-4555-8555-555555555555", proof_revision: `tc-research-revision:proof:sha256:${"1".repeat(64)}`,\n    source_historical_result_entity_id: historicalEntity, source_historical_result_revision: historicalRevision,\n    source_result_archive_ref: `tc-evidence:sha256:${sourceArchiveSha}`, source_result_archive_sha256: sourceArchiveSha,\n    source_project_ref: `tc-evidence:sha256:${sourceProjectSha}`, source_project_sha256: sourceProjectSha,\n    compiled_project_ref: `tc-evidence:sha256:${compiledProjectSha}`, compiled_project_sha256: compiledProjectSha,\n    configuration_changed: true, source_task_sha256: sourceTaskSha, compiled_task_sha256: compiledTaskSha, native_settings: { Precision: "2", Spread: "3" },\n    engine_ref: `tc-evidence:sha256:${engineSha}`, engine_sha256: engineSha, launcher_sha256: null,\n    native_project_name: projectName, native_project_relative_path: `user/projects/${projectName}/project.cfx`,\n    failure_reason_code: "robustness_attempt_interrupted", partial_side_effect: true, receipts: [],\n  };\n  const parsed = robustnessAttemptFromPayload(interrupted);\n  assert.equal(parsed.state, "interrupted");\n\n  const other = {\n    ...interrupted,\n    attempt_ref: `tc-evidence:sha256:${"9".repeat(64)}`,\n    source_historical_result_entity_id: "tc-research:historical-result:v1:44444444-4444-4444-8444-444444444444",\n    source_historical_result_revision: `tc-research-revision:historical-result:sha256:${"8".repeat(64)}`,\n  };\n  assert.deepEqual(robustnessAttemptsForHistorical([parsed, other], historical()).map((item) => item.attempt_ref), [parsed.attempt_ref]);\n});\n'''
path = Path("tests/research-backtest-robustness.test.mjs")
path.write_text(path.read_text(encoding="utf-8") + frontend_test, encoding="utf-8")

# Temporary patch artifacts must not enter the merge candidate.
for obsolete in (
    ".github/scripts/apply_robustness_review_round4.py",
    ".github/workflows/apply-robustness-review-round4.yml",
):
    Path(obsolete).unlink(missing_ok=True)

print("fourth-round robustness review corrections applied")
