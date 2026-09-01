from __future__ import annotations

from pathlib import Path


def read(path: str) -> str:
    return Path(path).read_text(encoding="utf-8")


def write(path: str, text: str) -> None:
    Path(path).write_text(text, encoding="utf-8")


def replace_once(path: str, old: str, new: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected one replacement target, found {count}")
    write(path, text.replace(old, new, 1))


# 1. Native gateway: a launched timeout/rejection/invalid runner receipt is a
# possible native side effect even when the process did not report completion.
replace_once(
    "product/tradercockpit/sqx_gateway.py",
    '''    def read_model(self) -> dict[str, object]:\n        completed = sum(item.get("state") == "completed" for item in self.receipts)\n        return {\n            "schema": SQX_NATIVE_CONTROL_ERROR_SCHEMA,\n            "error": "native_control_refused",\n            "reason_code": self.code,\n            "detail": self.detail,\n            "control_requests_completed": completed,\n            "partial_side_effect": completed > 0,\n            "receipts": [dict(item) for item in self.receipts],\n        }\n''',
    '''    def read_model(self) -> dict[str, object]:\n        completed = sum(item.get("state") == "completed" for item in self.receipts)\n        launched_states = {"completed", "timeout", "rejected", "invalid_receipt"}\n        partial_side_effect = any(item.get("state") in launched_states for item in self.receipts)\n        return {\n            "schema": SQX_NATIVE_CONTROL_ERROR_SCHEMA,\n            "error": "native_control_refused",\n            "reason_code": self.code,\n            "detail": self.detail,\n            "control_requests_completed": completed,\n            "partial_side_effect": partial_side_effect,\n            "receipts": [dict(item) for item in self.receipts],\n        }\n''',
)

# 2. Bookmarked native robustness readback must be backed by a current completed
# Research proof chain. A detached evidence object is not enough.
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''    payload = _read_record(store, ref)\n    matches = [item for item in _completed_proof_records(store) if item.get("validation_ref") == str(ref)]\n    if len(matches) > 1:\n        raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "multiple current robustness proofs reference one validation record")\n    return matches[0] if matches else {**payload, "validation_ref": str(ref)}\n''',
    '''    matches = [item for item in _completed_proof_records(store) if item.get("validation_ref") == str(ref)]\n    if not matches:\n        raise ResearchRobustnessError(\n            "robustness_proof_required",\n            "validation_ref is not registered as the current completed content of a Research proof",\n        )\n    if len(matches) > 1:\n        raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "multiple current robustness proofs reference one validation record")\n    return matches[0]\n''',
)

# 3. Completion custody is part of execution truth. Keep the proof at prepared
# until all result evidence and record revalidation have succeeded; any
# recoverable post-launch custody failure attempts a failed successor containing
# the exact launcher/receipt and partial-side-effect truth.
robustness_path = "product/tradercockpit/research_robustness.py"
robustness_text = read(robustness_path)
completion_marker = "    result_ref = store.put_evidence(result_bytes)\n"
completion_index = robustness_text.find(completion_marker)
if completion_index < 0:
    raise RuntimeError("research_robustness.py: completion custody marker not found")
new_completion = '''    try:\n        result_ref = store.put_evidence(result_bytes)\n        result_strategy_ref = store.put_evidence(result_strategy)\n        result_settings_ref = store.put_evidence(result_settings)\n\n        record = {\n            "schema": ROBUSTNESS_RECORD_SCHEMA,\n            "sqx_build": SQX_BUILD,\n            "operation": ROBUSTNESS_OPERATION,\n            "method": ROBUSTNESS_METHOD_HIGHER_PRECISION,\n            "source_historical_result_entity_id": historical_result_entity_id,\n            "source_historical_result_revision": expected_historical_result_revision,\n            "source_result_archive_ref": str(source_result_ref),\n            "source_result_archive_sha256": source_result_sha,\n            "source_project_ref": str(source_project_ref),\n            "source_project_sha256": source_project_sha,\n            "compiled_project_ref": str(compiled_project_ref),\n            "compiled_project_sha256": compiled_project_sha,\n            "configuration_changed": plan["configuration_changed"],\n            "source_task_sha256": plan["source_task_sha256"],\n            "compiled_task_sha256": plan["compiled_task_sha256"],\n            "native_settings": plan["native_settings"],\n            "engine_ref": str(engine_ref),\n            "engine_sha256": engine_sha,\n            "launcher_sha256": launcher_sha,\n            "native_project_name": project_name,\n            "native_project_relative_path": project_relative,\n            "receipts": [dict(item) for item in receipts],\n            "result_archive_name": result_info["archive"],\n            "result_archive_ref": str(result_ref),\n            "result_archive_sha256": result_info["archive_sha256"],\n            "result_strategy_ref": str(result_strategy_ref),\n            "result_strategy_sha256": result_info["strategy_entry_sha256"],\n            "result_settings_ref": str(result_settings_ref),\n            "result_settings_sha256": result_info["settings_entry_sha256"],\n            "execution_state": "completed",\n            "producer_outcome_state": ROBUSTNESS_OUTCOME_UNREAD,\n        }\n        completed_revision = store.create_revision(\n            proof_entity,\n            _canonical(record),\n            parent_revision=prepared_revision.revision,\n            evidence=prepared_evidence + (result_ref, result_strategy_ref, result_settings_ref),\n        )\n        record_ref = completed_revision.content\n        reopened = _read_record(store, record_ref)\n        store.compare_and_set_current(\n            proof_entity,\n            expected_revision=prepared_revision.revision,\n            target_revision=completed_revision.revision,\n        )\n    except ResearchCustodyError as exc:\n        try:\n            _failed_successor(\n                store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,\n                reason_code="robustness_completion_custody_failed",\n                launcher_sha256=launcher_sha,\n                receipts=receipts,\n                partial_side_effect=True,\n            )\n        except ResearchCustodyError as failure_exc:\n            raise ResearchRobustnessError(\n                "robustness_completion_custody_failed",\n                "native execution completed, but result custody and failed-state custody could not be persisted",\n            ) from failure_exc\n        raise ResearchRobustnessError("robustness_completion_custody_failed", exc.detail) from exc\n    except ResearchRobustnessError as exc:\n        try:\n            _failed_successor(\n                store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,\n                reason_code=exc.code,\n                launcher_sha256=launcher_sha,\n                receipts=receipts,\n                partial_side_effect=True,\n            )\n        except ResearchCustodyError as failure_exc:\n            raise ResearchRobustnessError(\n                "robustness_completion_custody_failed",\n                "native execution completed, but result validation and failed-state custody could not be persisted",\n            ) from failure_exc\n        raise\n\n    return {\n        **reopened,\n        "validation_ref": str(record_ref),\n        "proof_entity_id": str(proof_entity),\n        "proof_revision": str(completed_revision.revision),\n    }\n'''
write(robustness_path, robustness_text[:completion_index] + new_completion)

# 4. The HTTP public boundary must also require proof identities, and detached
# proof refs are a not-found condition rather than an acceptable legacy record.
replace_once(
    "product/tradercockpit/research_retester_http.py",
    '''    not_found = {"robustness_record_ref_invalid"}\n''',
    '''    not_found = {"robustness_record_ref_invalid", "robustness_proof_required"}\n''',
)
replace_once(
    "product/tradercockpit/research_retester_http.py",
    '''    receipts = record.get("receipts")\n    receipt = receipts[0] if isinstance(receipts, list) and len(receipts) == 1 and isinstance(receipts[0], dict) else None\n    if (\n''',
    '''    receipts = record.get("receipts")\n    receipt = receipts[0] if isinstance(receipts, list) and len(receipts) == 1 and isinstance(receipts[0], dict) else None\n    proof_entity_id = record.get("proof_entity_id")\n    proof_revision = record.get("proof_revision")\n    if (\n''',
)
replace_once(
    "product/tradercockpit/research_retester_http.py",
    '''        or record.get("producer_outcome_state") != ROBUSTNESS_OUTCOME_UNREAD\n        or receipt is None\n''',
    '''        or record.get("producer_outcome_state") != ROBUSTNESS_OUTCOME_UNREAD\n        or not isinstance(proof_entity_id, str)\n        or not proof_entity_id.startswith("tc-research:proof:v1:")\n        or not isinstance(proof_revision, str)\n        or not proof_revision.startswith("tc-research-revision:proof:sha256:")\n        or receipt is None\n''',
)

# 5. Browser schema requires durable proof identities instead of accepting an
# evidence-only record.
replace_once(
    "web/research-backtest-robustness.mjs",
    '''    "validation_ref",\n    "source_historical_result_entity_id",\n''',
    '''    "validation_ref",\n    "proof_entity_id",\n    "proof_revision",\n    "source_historical_result_entity_id",\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''  const hasProofEntity = typeof payload.proof_entity_id === "string" && payload.proof_entity_id.length > 0;\n  const hasProofRevision = typeof payload.proof_revision === "string" && payload.proof_revision.length > 0;\n  if (\n    hasProofEntity !== hasProofRevision\n    || (hasProofEntity && !/^tc-research:proof:v1:[0-9a-f-]{36}$/.test(payload.proof_entity_id))\n    || (hasProofRevision && !/^tc-research-revision:proof:sha256:[0-9a-f]{64}$/.test(payload.proof_revision))\n  ) {\n    throw new Error("Native robustness proof custody is inconsistent");\n  }\n''',
    '''  if (\n    !/^tc-research:proof:v1:[0-9a-f-]{36}$/.test(payload.proof_entity_id)\n    || !/^tc-research-revision:proof:sha256:[0-9a-f]{64}$/.test(payload.proof_revision)\n  ) {\n    throw new Error("Native robustness proof custody is inconsistent");\n  }\n''',
)

# 6. Never collapse multiple runs for one baseline. The default helper selects
# only an unambiguous single run; callers can select an exact validation ref.
replace_once(
    "web/research-backtest-robustness.mjs",
    '''export function robustnessResultForHistorical(catalog, historicalResult) {\n  if (!Array.isArray(catalog)) throw new Error("Native robustness catalog is invalid");\n  const source = historicalResultFromPayload(historicalResult);\n  return catalog.find((item) => item.source_historical_result_revision === source.revision) || null;\n}\n''',
    '''export function robustnessResultsForHistorical(catalog, historicalResult) {\n  if (!Array.isArray(catalog)) throw new Error("Native robustness catalog is invalid");\n  const source = historicalResultFromPayload(historicalResult);\n  return catalog.filter((item) => (\n    item.source_historical_result_entity_id === source.entity_id\n    && item.source_historical_result_revision === source.revision\n  ));\n}\n\nexport function robustnessResultForHistorical(catalog, historicalResult, validationRef = "") {\n  const matches = robustnessResultsForHistorical(catalog, historicalResult);\n  if (validationRef) return matches.find((item) => item.validation_ref === validationRef) || null;\n  return matches.length === 1 ? matches[0] : null;\n}\n''',
)

# 7. Render all exact runs for the selected baseline through an explicit receipt
# selector. Multiple runs no longer resolve by UUID/catalog ordering.
replace_once(
    "web/research-backtest-robustness.mjs",
    '''  const selected = completed[current.selectedIndex] || null;\n  const higherCapability = current.capabilities?.methods?.find((item) => item.method === HIGHER_PRECISION_METHOD) || null;\n''',
    '''  const selected = completed[current.selectedIndex] || null;\n  const matchingValidations = selected ? robustnessResultsForHistorical(current.catalog, selected) : [];\n  const higherCapability = current.capabilities?.methods?.find((item) => item.method === HIGHER_PRECISION_METHOD) || null;\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''  workspace.innerHTML = `<div class="dashboard-grid">\n''',
    '''  const validationPicker = matchingValidations.length\n    ? `<label class="field-label" for="robustness-validation-result">Captured robustness run</label><select id="robustness-validation-result" class="idea-editor">${matchingValidations.length > 1 && !current.validation ? '<option value="" selected>Choose exact robustness run</option>' : ""}${matchingValidations.map((item) => `<option value="${escapeHtml(item.validation_ref)}" ${current.validation?.validation_ref === item.validation_ref ? "selected" : ""}>${escapeHtml(short(item.validation_ref))} · ${escapeHtml(short(item.proof_revision))}</option>`).join("")}</select>`\n    : '<p class="field-help">No completed robustness run is registered for the selected Historical Result.</p>';\n  workspace.innerHTML = `<div class="dashboard-grid">\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''    <section class="panel" data-accent="purple"><div class="panel-heading"><div><p class="eyebrow">Immutable readback</p><h2>Robustness result custody</h2></div></div>${resultPanel(current.validation)}</section>\n''',
    '''    <section class="panel" data-accent="purple"><div class="panel-heading"><div><p class="eyebrow">Immutable readback</p><h2>Robustness result custody</h2></div></div>${validationPicker}${resultPanel(current.validation)}</section>\n''',
)

# 8. URL identity can be removed when its exact source is no longer selectable.
replace_once(
    "web/research-backtest-robustness.mjs",
    '''function persistValidationRef(validationRef) {\n  if (!globalThis.history?.replaceState || !globalThis.location) return;\n  const url = new URL(globalThis.location.href);\n  url.searchParams.set("stage", "backtest");\n  url.searchParams.set("tab", "robustness");\n  url.searchParams.set("validationRef", validationRef);\n  globalThis.history.replaceState({}, "", `${url.pathname}${url.search}`);\n}\n''',
    '''function persistValidationRef(validationRef) {\n  if (!globalThis.history?.replaceState || !globalThis.location) return;\n  const url = new URL(globalThis.location.href);\n  url.searchParams.set("stage", "backtest");\n  url.searchParams.set("tab", "robustness");\n  url.searchParams.set("validationRef", validationRef);\n  globalThis.history.replaceState({}, "", `${url.pathname}${url.search}`);\n}\n\nfunction clearValidationRef() {\n  if (!globalThis.history?.replaceState || !globalThis.location) return;\n  const url = new URL(globalThis.location.href);\n  url.searchParams.delete("validationRef");\n  globalThis.history.replaceState({}, "", `${url.pathname}${url.search}`);\n}\n''',
)

# 9. A bookmarked proof is displayed only when its exact Historical Result
# entity+revision remains in the current completed catalog.
replace_once(
    "web/research-backtest-robustness.mjs",
    '''    if (requestedRef) {\n      try {\n        validation = await fetchRobustnessResult(requestedRef);\n        const sourceIndex = completed.findIndex((item) => item.revision === validation.source_historical_result_revision);\n        if (sourceIndex >= 0) selectedIndex = sourceIndex;\n      } catch (error) {\n        detail = `Saved robustness result unavailable: ${error instanceof Error ? error.message : "readback failed"}`;\n      }\n    }\n''',
    '''    if (requestedRef) {\n      try {\n        const requestedValidation = await fetchRobustnessResult(requestedRef);\n        const sourceIndex = completed.findIndex((item) => (\n          item.entity_id === requestedValidation.source_historical_result_entity_id\n          && item.revision === requestedValidation.source_historical_result_revision\n        ));\n        if (sourceIndex >= 0) {\n          selectedIndex = sourceIndex;\n          validation = requestedValidation;\n        } else {\n          validation = null;\n          clearValidationRef();\n          detail = "Saved robustness result source Historical Result is no longer current; receipt was not displayed.";\n        }\n      } catch (error) {\n        detail = `Saved robustness result unavailable: ${error instanceof Error ? error.message : "readback failed"}`;\n      }\n    }\n''',
)

# 10. Historical selection and validation selection are separate exact identities.
replace_once(
    "web/research-backtest-robustness.mjs",
    '''  document.addEventListener("change", (event) => {\n    if (!robustnessRoute() || event.target?.id !== "robustness-source-result") return;\n    const selectedIndex = Number(event.target.value);\n    const completed = state.results.filter((item) => item.state === "completed" && item.execution_completed === true);\n    if (!Number.isInteger(selectedIndex) || selectedIndex < 0 || selectedIndex >= completed.length) return;\n    const selected = completed[selectedIndex];\n    const validation = robustnessResultForHistorical(state.catalog, selected);\n    state = { ...state, selectedIndex, validation, detail: "" };\n    render(panel(), state);\n  });\n''',
    '''  document.addEventListener("change", (event) => {\n    if (!robustnessRoute()) return;\n    const completed = state.results.filter((item) => item.state === "completed" && item.execution_completed === true);\n    if (event.target?.id === "robustness-source-result") {\n      const selectedIndex = Number(event.target.value);\n      if (!Number.isInteger(selectedIndex) || selectedIndex < 0 || selectedIndex >= completed.length) return;\n      const selected = completed[selectedIndex];\n      const validation = robustnessResultForHistorical(state.catalog, selected);\n      if (validation) persistValidationRef(validation.validation_ref);\n      else clearValidationRef();\n      state = { ...state, selectedIndex, validation, detail: "" };\n      render(panel(), state);\n      return;\n    }\n    if (event.target?.id === "robustness-validation-result") {\n      const selected = completed[state.selectedIndex];\n      if (!selected) return;\n      const validationRef = typeof event.target.value === "string" ? event.target.value : "";\n      const validation = validationRef\n        ? robustnessResultForHistorical(state.catalog, selected, validationRef)\n        : null;\n      if (validationRef && !validation) return;\n      if (validation) persistValidationRef(validation.validation_ref);\n      else clearValidationRef();\n      state = { ...state, validation, detail: "" };\n      render(panel(), state);\n    }\n  });\n''',
)

# Regression coverage: gateway side-effect semantics.
replace_once(
    "tests/product/test_sqx_retester_gateway.py",
    '''        self.assertFalse(model["partial_side_effect"])\n''',
    '''        self.assertTrue(model["partial_side_effect"])\n''',
)
replace_once(
    "tests/product/test_sqx_retester_gateway.py",
    '''\n\nif __name__ == "__main__":\n    unittest.main()\n''',
    '''\n    def test_timeout_marks_launched_retester_as_possible_partial_side_effect(self) -> None:\n        with TemporaryDirectory() as tmp:\n            home, project_name, launcher_hash, project_hash, engine_hash = self._runtime(Path(tmp))\n\n            def runner(command, **kwargs):\n                raise subprocess.TimeoutExpired(command, timeout=kwargs["timeout"])\n\n            with self.assertRaises(SqxNativeGatewayError) as caught:\n                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_retester_task(\n                    project_name,\n                    expected_project_sha256=project_hash,\n                    expected_engine_sha256=engine_hash,\n                )\n\n        model = caught.exception.read_model()\n        self.assertEqual(model["control_requests_completed"], 0)\n        self.assertTrue(model["partial_side_effect"])\n        self.assertEqual(model["receipts"][0]["state"], "timeout")\n\n\nif __name__ == "__main__":\n    unittest.main()\n''',
)

# Regression coverage: durable proof requirement and post-execution completion
# custody failure.
replace_once(
    "tests/product/test_research_robustness.py",
    '''from tradercockpit.research_custody import FileResearchCustodyStore, ResearchRevisionRef\n''',
    '''from tradercockpit.research_custody import FileResearchCustodyStore, ResearchCustodyError, ResearchRevisionRef\n''',
)
replace_once(
    "tests/product/test_research_robustness.py",
    '''                            "state": "completed",\n                            "launcher_sha256": outer.LAUNCHER_SHA,\n''',
    '''                            "state": "timeout",\n                            "launcher_sha256": outer.LAUNCHER_SHA,\n''',
)
replace_once(
    "tests/product/test_research_robustness.py",
    '''    def test_invalid_validation_ref_is_typed(self) -> None:\n''',
    '''    def test_completion_custody_failure_persists_failed_proof_after_native_execution(self) -> None:\n        source_result = self._archive_bytes("baseline")\n        result_bytes = self._archive_bytes("higher-precision")\n        project = self._project_bytes(self._task_xml())\n        with TemporaryDirectory() as tmp:\n            root = Path(tmp)\n            home = self._runtime(root / "sqx", project)\n            store = FileResearchCustodyStore(root / "data")\n            historical = self._historical(store, source_result)\n            original_put = store.put_evidence\n\n            def flaky_put(value: bytes):\n                if value == result_bytes:\n                    raise ResearchCustodyError("immutable_evidence_corrupt", "simulated completed-result evidence failure")\n                return original_put(value)\n\n            with patch("tradercockpit.research_robustness.read_current_historical_result", return_value=historical):\n                with patch.object(store, "put_evidence", side_effect=flaky_put):\n                    with self.assertRaises(ResearchRobustnessError) as caught:\n                        start_native_higher_precision(\n                            store,\n                            home,\n                            self.LAUNCHER_SHA,\n                            historical_result_entity_id=self.HISTORICAL_ENTITY,\n                            expected_historical_result_revision=self.HISTORICAL_REVISION,\n                            gateway_factory=self._gateway_factory(home, "higher-precision"),\n                        )\n            self.assertEqual(caught.exception.code, "robustness_completion_custody_failed")\n            failed = self._current_proof_payload(store)\n            self.assertEqual(failed["state"], "failed")\n            self.assertEqual(failed["failure_reason_code"], "robustness_completion_custody_failed")\n            self.assertTrue(failed["partial_side_effect"])\n            self.assertEqual(failed["launcher_sha256"], self.LAUNCHER_SHA)\n            self.assertEqual(failed["receipts"][0]["state"], "completed")\n\n    def test_detached_validation_evidence_is_not_reopened_without_current_proof(self) -> None:\n        source_result = self._archive_bytes("baseline")\n        project = self._project_bytes(self._task_xml())\n        with TemporaryDirectory() as tmp:\n            root = Path(tmp)\n            home = self._runtime(root / "sqx", project)\n            store = FileResearchCustodyStore(root / "data")\n            historical = self._historical(store, source_result)\n            with patch("tradercockpit.research_robustness.read_current_historical_result", return_value=historical):\n                result = start_native_higher_precision(\n                    store,\n                    home,\n                    self.LAUNCHER_SHA,\n                    historical_result_entity_id=self.HISTORICAL_ENTITY,\n                    expected_historical_result_revision=self.HISTORICAL_REVISION,\n                    gateway_factory=self._gateway_factory(home, "higher-precision"),\n                )\n            for pointer in (store.base / "current" / "proof").glob("*.json"):\n                pointer.unlink()\n            with self.assertRaises(ResearchRobustnessError) as caught:\n                read_native_robustness_result(store, result["validation_ref"])\n            self.assertEqual(caught.exception.code, "robustness_proof_required")\n\n    def test_invalid_validation_ref_is_typed(self) -> None:\n''',
)

# HTTP fixture and fail-closed public proof identity.
replace_once(
    "tests/product/test_research_robustness_http_boundary.py",
    '''            "validation_ref": self.VALIDATION_REF,\n            "operation": "native_retester_cross_check",\n''',
    '''            "validation_ref": self.VALIDATION_REF,\n            "proof_entity_id": "tc-research:proof:v1:77777777-7777-4777-8777-777777777777",\n            "proof_revision": f"tc-research-revision:proof:sha256:{'8' * 64}",\n            "operation": "native_retester_cross_check",\n''',
)
replace_once(
    "tests/product/test_research_robustness_http_boundary.py",
    '''    def test_capability_and_catalog_reads_accept_no_browser_injected_settings(self) -> None:\n''',
    '''    def test_public_readback_requires_registered_proof_identity_shape(self) -> None:\n        with TemporaryDirectory() as tmp:\n            server, thread, _store = self._server(Path(tmp))\n            endpoint = f"http://127.0.0.1:{server.server_port}/api/research/historical-results"\n            try:\n                record = self._robustness_record()\n                record.pop("proof_entity_id")\n                record.pop("proof_revision")\n                with patch(\n                    "tradercockpit.research_retester_http.read_native_robustness_result",\n                    return_value=record,\n                ):\n                    status, payload = self._post(\n                        endpoint,\n                        {"action": "read-robustness", "validation_ref": self.VALIDATION_REF},\n                    )\n                self.assertEqual(status, 409)\n                self.assertEqual(payload["reason_code"], "robustness_record_corrupt")\n            finally:\n                server.shutdown()\n                server.server_close()\n                thread.join()\n\n    def test_capability_and_catalog_reads_accept_no_browser_injected_settings(self) -> None:\n''',
)

# JS unit coverage for durable proof shape and multiple exact runs.
replace_once(
    "tests/research-backtest-robustness.test.mjs",
    '''  robustnessResultForHistorical,\n  robustnessResultFromPayload,\n''',
    '''  robustnessResultForHistorical,\n  robustnessResultsForHistorical,\n  robustnessResultFromPayload,\n''',
)
with Path("tests/research-backtest-robustness.test.mjs").open("a", encoding="utf-8") as handle:
    handle.write('''\n\ntest("robustness result requires durable proof identity", () => {\n  assert.throws(\n    () => robustnessResultFromPayload(robustness({ proof_entity_id: undefined, proof_revision: undefined })),\n    /identity is invalid|proof custody is inconsistent/,\n  );\n});\n\ntest("multiple robustness runs for one baseline require exact validation selection", () => {\n  const source = historical();\n  const first = robustness();\n  const second = robustness({\n    validation_ref: `tc-evidence:sha256:${"8".repeat(64)}`,\n    proof_entity_id: "tc-research:proof:v1:44444444-4444-4444-8444-444444444444",\n    proof_revision: `tc-research-revision:proof:sha256:${"b".repeat(64)}`,\n    result_archive_ref: `tc-evidence:sha256:${"9".repeat(64)}`,\n    result_archive_sha256: "9".repeat(64),\n  });\n  const catalog = [first, second].map(robustnessResultFromPayload);\n  assert.equal(robustnessResultsForHistorical(catalog, source).length, 2);\n  assert.equal(robustnessResultForHistorical(catalog, source), null);\n  assert.equal(\n    robustnessResultForHistorical(catalog, source, second.validation_ref)?.validation_ref,\n    second.validation_ref,\n  );\n});\n''')

print("Applied robustness review round two corrections")
