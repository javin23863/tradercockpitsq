from __future__ import annotations

from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected one match, found {count}: {old[:100]!r}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


# 1. Bind the exact staged baseline archive into native Retester preflight/receipt.
path = "product/tradercockpit/sqx_gateway.py"
replace_once(
    path,
    """class _VerifiedRetesterContext:\n    home: Path\n    launcher: Path\n    launcher_sha256: str\n    project_name: str\n    project_file: Path\n    project_relative_path: str\n    project_sha256: str\n    engine_sha256: str\n""",
    """class _VerifiedRetesterContext:\n    home: Path\n    launcher: Path\n    launcher_sha256: str\n    project_name: str\n    project_file: Path\n    project_relative_path: str\n    project_sha256: str\n    engine_sha256: str\n    result_archive_name: str | None\n    result_archive_relative_path: str | None\n    result_archive_sha256: str | None\n""",
)
replace_once(
    path,
    """    def _preflight_retester(\n        self,\n        project_name: str,\n        expected_project_sha256: str | None,\n        expected_engine_sha256: str | None,\n    ) -> _VerifiedRetesterContext:\n""",
    """    def _preflight_retester(\n        self,\n        project_name: str,\n        expected_project_sha256: str | None,\n        expected_engine_sha256: str | None,\n        result_archive_name: str | None = None,\n        expected_result_archive_sha256: str | None = None,\n    ) -> _VerifiedRetesterContext:\n""",
)
replace_once(
    path,
    """        if observed_project != expected_project:\n            raise SqxNativeGatewayError(\n                \"retester_project_hash_mismatch\",\n                \"isolated Retester project does not match its staged identity\",\n            )\n\n        engine, engine_relative = _resolve_inside(\n""",
    """        if observed_project != expected_project:\n            raise SqxNativeGatewayError(\n                \"retester_project_hash_mismatch\",\n                \"isolated Retester project does not match its staged identity\",\n            )\n\n        observed_result_name: str | None = None\n        observed_result_relative: str | None = None\n        observed_result_sha: str | None = None\n        if result_archive_name is not None or expected_result_archive_sha256 is not None:\n            if (\n                not isinstance(result_archive_name, str)\n                or not result_archive_name\n                or Path(result_archive_name).name != result_archive_name\n                or \"/\" in result_archive_name\n                or \"\\\\\" in result_archive_name\n                or not result_archive_name.lower().endswith(\".sqx\")\n            ):\n                raise SqxNativeGatewayError(\n                    \"retester_result_archive_invalid\",\n                    \"native Retester control requires one exact staged SQX result filename\",\n                )\n            expected_result = _trusted_digest(\n                expected_result_archive_sha256,\n                missing_code=\"retester_result_archive_identity_not_configured\",\n                invalid_code=\"retester_result_archive_identity_invalid\",\n            )\n            results_root, _ = _resolve_inside(\n                launcher.home,\n                project_root / \"databanks/Results\",\n                escape_code=\"retester_result_archive_path_escape\",\n            )\n            result_file, result_relative = _resolve_inside(\n                launcher.home,\n                results_root / result_archive_name,\n                escape_code=\"retester_result_archive_path_escape\",\n            )\n            if result_file.parent != results_root or not result_file.is_file():\n                raise SqxNativeGatewayError(\n                    \"retester_result_archive_missing\",\n                    \"exact staged Retester result archive is missing\",\n                )\n            observed_result_sha = _sha256_file(result_file)\n            if observed_result_sha != expected_result:\n                raise SqxNativeGatewayError(\n                    \"retester_result_archive_hash_mismatch\",\n                    \"staged Retester result archive changed before native launch\",\n                )\n            observed_result_name = result_archive_name\n            observed_result_relative = result_relative.as_posix()\n\n        engine, engine_relative = _resolve_inside(\n""",
)
replace_once(
    path,
    """            project_relative_path=relative.as_posix(),\n            project_sha256=observed_project,\n            engine_sha256=observed_engine,\n        )\n""",
    """            project_relative_path=relative.as_posix(),\n            project_sha256=observed_project,\n            engine_sha256=observed_engine,\n            result_archive_name=observed_result_name,\n            result_archive_relative_path=observed_result_relative,\n            result_archive_sha256=observed_result_sha,\n        )\n""",
)
replace_once(
    path,
    """            \"project_sha256\": context.project_sha256 if context else None,\n            \"engine_sha256\": context.engine_sha256 if context else None,\n            \"reason_code\": reason_code,\n""",
    """            \"project_sha256\": context.project_sha256 if context else None,\n            \"engine_sha256\": context.engine_sha256 if context else None,\n            \"result_archive_name\": context.result_archive_name if context else None,\n            \"result_archive_relative_path\": context.result_archive_relative_path if context else None,\n            \"result_archive_sha256\": context.result_archive_sha256 if context else None,\n            \"reason_code\": reason_code,\n""",
)
replace_once(
    path,
    """    def launch_retester_task(\n        self,\n        project_name: str,\n        *,\n        expected_project_sha256: str | None,\n        expected_engine_sha256: str | None,\n    ) -> dict[str, object]:\n""",
    """    def launch_retester_task(\n        self,\n        project_name: str,\n        *,\n        expected_project_sha256: str | None,\n        expected_engine_sha256: str | None,\n        result_archive_name: str | None = None,\n        expected_result_archive_sha256: str | None = None,\n    ) -> dict[str, object]:\n""",
)
replace_once(
    path,
    """                context = self._preflight_retester(\n                    project_name,\n                    expected_project_sha256,\n                    expected_engine_sha256,\n                )\n""",
    """                context = self._preflight_retester(\n                    project_name,\n                    expected_project_sha256,\n                    expected_engine_sha256,\n                    result_archive_name,\n                    expected_result_archive_sha256,\n                )\n""",
)
replace_once(
    path,
    """            \"project_sha256\": context.project_sha256,\n            \"engine_sha256\": context.engine_sha256,\n            \"control_requests_submitted\": 1,\n""",
    """            \"project_sha256\": context.project_sha256,\n            \"engine_sha256\": context.engine_sha256,\n            \"result_archive_name\": context.result_archive_name,\n            \"result_archive_relative_path\": context.result_archive_relative_path,\n            \"result_archive_sha256\": context.result_archive_sha256,\n            \"control_requests_submitted\": 1,\n""",
)

# 2. Durable failed-attempt catalog/readback, actual filesystem errors, and baseline binding.
path = "product/tradercockpit/research_robustness.py"
insert_after = """def _completed_proof_records(store: FileResearchCustodyStore) -> list[dict[str, object]]:\n"""
# Inject failed helper after the completed helper by replacing the list function boundary.
replace_once(
    path,
    """def list_native_robustness_results(store: FileResearchCustodyStore) -> dict[str, object]:\n    \"\"\"List completed native robustness proofs from durable Research custody.\"\"\"\n\n    return {\n        \"schema\": ROBUSTNESS_CATALOG_SCHEMA,\n        \"results\": _completed_proof_records(store),\n    }\n""",
    """def _failed_proof_records(store: FileResearchCustodyStore) -> list[dict[str, object]]:\n    results: list[dict[str, object]] = []\n    identity_keys = (\n        \"sqx_build\", \"operation\", \"method\",\n        \"source_historical_result_entity_id\", \"source_historical_result_revision\",\n        \"source_result_archive_ref\", \"source_result_archive_sha256\",\n        \"source_project_ref\", \"source_project_sha256\",\n        \"compiled_project_ref\", \"compiled_project_sha256\", \"configuration_changed\",\n        \"source_task_sha256\", \"compiled_task_sha256\", \"native_settings\",\n        \"engine_ref\", \"engine_sha256\", \"native_project_name\", \"native_project_relative_path\",\n    )\n    for entity in _current_proof_entities(store):\n        revision = store.current(entity)\n        stored = store.read_revision(revision)\n        try:\n            raw = json.loads(store.read_revision_content(revision))\n        except (UnicodeDecodeError, json.JSONDecodeError):\n            continue\n        if not isinstance(raw, dict) or raw.get(\"schema\") != ROBUSTNESS_ATTEMPT_SCHEMA or raw.get(\"state\") != \"failed\":\n            continue\n        if stored.parent_revision is None:\n            raise ResearchRobustnessError(\"robustness_proof_catalog_corrupt\", \"failed robustness proof has no prepared parent\")\n        parent_revision = store.read_revision(stored.parent_revision)\n        try:\n            prepared = json.loads(store.read_revision_content(stored.parent_revision))\n        except (UnicodeDecodeError, json.JSONDecodeError) as exc:\n            raise ResearchRobustnessError(\"robustness_proof_catalog_corrupt\", \"prepared robustness proof is unreadable\") from exc\n        if not isinstance(prepared, dict) or prepared.get(\"schema\") != ROBUSTNESS_ATTEMPT_SCHEMA or prepared.get(\"state\") != \"prepared\":\n            raise ResearchRobustnessError(\"robustness_proof_catalog_corrupt\", \"failed robustness proof parent is not one prepared native attempt\")\n        if any(prepared.get(key) != raw.get(key) for key in identity_keys):\n            raise ResearchRobustnessError(\"robustness_proof_catalog_corrupt\", \"failed robustness proof changed its prepared control identity\")\n        try:\n            prepared_evidence = {\n                EvidenceRef.parse(prepared[\"source_result_archive_ref\"]),\n                EvidenceRef.parse(prepared[\"source_project_ref\"]),\n                EvidenceRef.parse(prepared[\"compiled_project_ref\"]),\n                EvidenceRef.parse(prepared[\"engine_ref\"]),\n            }\n            source_entity = ResearchEntityId.parse(raw[\"source_historical_result_entity_id\"])\n            source_revision = ResearchRevisionRef.parse(raw[\"source_historical_result_revision\"])\n        except (KeyError, TypeError, ResearchCustodyError) as exc:\n            raise ResearchRobustnessError(\"robustness_proof_catalog_corrupt\", \"failed robustness proof identities are invalid\") from exc\n        if source_entity.kind != ResearchKind.HISTORICAL_RESULT or source_revision.kind != ResearchKind.HISTORICAL_RESULT:\n            raise ResearchRobustnessError(\"robustness_proof_catalog_corrupt\", \"failed robustness source is not Historical Result custody\")\n        if set(parent_revision.evidence) != prepared_evidence or set(stored.evidence) != prepared_evidence:\n            raise ResearchRobustnessError(\"robustness_proof_catalog_corrupt\", \"failed robustness proof evidence set is invalid\")\n        if raw.get(\"sqx_build\") != SQX_BUILD or raw.get(\"operation\") != ROBUSTNESS_OPERATION or raw.get(\"method\") != ROBUSTNESS_METHOD_HIGHER_PRECISION:\n            raise ResearchRobustnessError(\"robustness_proof_catalog_corrupt\", \"failed robustness producer identity is invalid\")\n        if type(raw.get(\"partial_side_effect\")) is not bool or not isinstance(raw.get(\"failure_reason_code\"), str) or not raw[\"failure_reason_code\"]:\n            raise ResearchRobustnessError(\"robustness_proof_catalog_corrupt\", \"failed robustness state is invalid\")\n        receipts = raw.get(\"receipts\")\n        if not isinstance(receipts, list) or any(not isinstance(item, dict) for item in receipts):\n            raise ResearchRobustnessError(\"robustness_proof_catalog_corrupt\", \"failed robustness receipts are invalid\")\n        launcher = raw.get(\"launcher_sha256\")\n        if launcher is not None and (not isinstance(launcher, str) or _DIGEST_RE.fullmatch(launcher) is None):\n            raise ResearchRobustnessError(\"robustness_proof_catalog_corrupt\", \"failed robustness launcher identity is invalid\")\n        if raw[\"partial_side_effect\"] and launcher is None:\n            raise ResearchRobustnessError(\"robustness_proof_catalog_corrupt\", \"failed robustness side-effect state lacks launcher custody\")\n        results.append({\n            **raw,\n            \"attempt_ref\": str(stored.content),\n            \"proof_entity_id\": str(entity),\n            \"proof_revision\": str(revision),\n        })\n    return results\n\n\ndef list_native_robustness_results(store: FileResearchCustodyStore) -> dict[str, object]:\n    \"\"\"List completed runs and failed native attempts from durable Research custody.\"\"\"\n\n    return {\n        \"schema\": ROBUSTNESS_CATALOG_SCHEMA,\n        \"results\": _completed_proof_records(store),\n        \"failed_attempts\": _failed_proof_records(store),\n    }\n""",
)
replace_once(
    path,
    """    matches = [item for item in _completed_proof_records(store) if item.get(\"validation_ref\") == str(ref)]\n    if not matches:\n        raise ResearchRobustnessError(\n            \"robustness_proof_required\",\n            \"validation_ref is not registered as the current completed content of a Research proof\",\n        )\n""",
    """    matches = [item for item in _completed_proof_records(store) if item.get(\"validation_ref\") == str(ref)]\n    matches.extend(item for item in _failed_proof_records(store) if item.get(\"attempt_ref\") == str(ref))\n    if not matches:\n        raise ResearchRobustnessError(\n            \"robustness_proof_required\",\n            \"validation_ref is not registered as the current completed or failed content of a Research proof\",\n        )\n""",
)
replace_once(
    path,
    """        receipt = gateway_factory(sqx_home, trusted_launcher_sha256).launch_retester_task(\n            project_name,\n            expected_project_sha256=compiled_project_sha,\n            expected_engine_sha256=engine_sha,\n        )\n""",
    """        receipt = gateway_factory(sqx_home, trusted_launcher_sha256).launch_retester_task(\n            project_name,\n            expected_project_sha256=compiled_project_sha,\n            expected_engine_sha256=engine_sha,\n            result_archive_name=historical[\"result_archive_name\"],\n            expected_result_archive_sha256=source_result_sha,\n        )\n""",
)
replace_once(
    path,
    """        and receipt.get(\"project_relative_path\") == project_relative\n        and isinstance(raw_launcher, str)\n""",
    """        and receipt.get(\"project_relative_path\") == project_relative\n        and receipt.get(\"result_archive_name\") == historical[\"result_archive_name\"]\n        and receipt.get(\"result_archive_sha256\") == source_result_sha\n        and isinstance(receipt.get(\"result_archive_relative_path\"), str)\n        and isinstance(raw_launcher, str)\n""",
)
replace_once(
    path,
    """        and receipt_items[0].get(\"engine_sha256\") == engine_sha\n    )\n""",
    """        and receipt_items[0].get(\"engine_sha256\") == engine_sha\n        and receipt_items[0].get(\"result_archive_name\") == historical[\"result_archive_name\"]\n        and receipt_items[0].get(\"result_archive_sha256\") == source_result_sha\n        and receipt_items[0].get(\"result_archive_relative_path\") == receipt.get(\"result_archive_relative_path\")\n    )\n""",
)
replace_once(
    path,
    """    except ResearchCustodyError as exc:\n        try:\n            _failed_successor(\n""",
    """    except (ResearchCustodyError, OSError) as exc:\n        try:\n            _failed_successor(\n""",
)
replace_once(
    path,
    """        except ResearchCustodyError as failure_exc:\n            raise ResearchRobustnessError(\n                \"robustness_completion_custody_failed\",\n                \"native execution completed, but result custody and failed-state custody could not be persisted\",\n            ) from failure_exc\n        raise ResearchRobustnessError(\"robustness_completion_custody_failed\", exc.detail) from exc\n""",
    """        except (ResearchCustodyError, OSError) as failure_exc:\n            raise ResearchRobustnessError(\n                \"robustness_completion_custody_failed\",\n                \"native execution completed, but result custody and failed-state custody could not be persisted\",\n            ) from failure_exc\n        detail = exc.detail if isinstance(exc, ResearchCustodyError) else str(exc)\n        raise ResearchRobustnessError(\"robustness_completion_custody_failed\", detail) from exc\n""",
)
# The second recovery clause is for record-validation errors; its failed-successor can also hit raw filesystem errors.
replace_once(
    path,
    """        except ResearchCustodyError as failure_exc:\n            raise ResearchRobustnessError(\n                \"robustness_completion_custody_failed\",\n                \"native execution completed, but result validation and failed-state custody could not be persisted\",\n            ) from failure_exc\n""",
    """        except (ResearchCustodyError, OSError) as failure_exc:\n            raise ResearchRobustnessError(\n                \"robustness_completion_custody_failed\",\n                \"native execution completed, but result validation and failed-state custody could not be persisted\",\n            ) from failure_exc\n""",
)

# 3. HTTP read model accepts validated failed attempts as durable proof state.
path = "product/tradercockpit/research_retester_http.py"
replace_once(
    path,
    """    ROBUSTNESS_METHOD_HIGHER_PRECISION,\n    ROBUSTNESS_OPERATION,\n""",
    """    ROBUSTNESS_ATTEMPT_SCHEMA,\n    ROBUSTNESS_METHOD_HIGHER_PRECISION,\n    ROBUSTNESS_OPERATION,\n""",
)
replace_once(
    path,
    """    receipts = record.get(\"receipts\")\n    receipt = receipts[0] if isinstance(receipts, list) and len(receipts) == 1 and isinstance(receipts[0], dict) else None\n    proof_entity_id = record.get(\"proof_entity_id\")\n    proof_revision = record.get(\"proof_revision\")\n    if (\n        record.get(\"schema\") != ROBUSTNESS_RECORD_SCHEMA\n""",
    """    receipts = record.get(\"receipts\")\n    proof_entity_id = record.get(\"proof_entity_id\")\n    proof_revision = record.get(\"proof_revision\")\n    if record.get(\"schema\") == ROBUSTNESS_ATTEMPT_SCHEMA and record.get(\"state\") == \"failed\":\n        launched_states = {\"completed\", \"timeout\", \"rejected\", \"invalid_receipt\"}\n        if (\n            not isinstance(record.get(\"attempt_ref\"), str)\n            or not record[\"attempt_ref\"].startswith(\"tc-evidence:sha256:\")\n            or not isinstance(proof_entity_id, str)\n            or not proof_entity_id.startswith(\"tc-research:proof:v1:\")\n            or not isinstance(proof_revision, str)\n            or not proof_revision.startswith(\"tc-research-revision:proof:sha256:\")\n            or not isinstance(record.get(\"failure_reason_code\"), str)\n            or not record[\"failure_reason_code\"]\n            or type(record.get(\"partial_side_effect\")) is not bool\n            or not isinstance(receipts, list)\n            or any(not isinstance(item, dict) for item in receipts)\n            or (record[\"partial_side_effect\"] and not any(item.get(\"state\") in launched_states for item in receipts))\n        ):\n            raise ResearchRobustnessError(\n                \"robustness_record_corrupt\",\n                \"failed native robustness attempt is not bound to durable Proof custody\",\n            )\n        return record\n\n    receipt = receipts[0] if isinstance(receipts, list) and len(receipts) == 1 and isinstance(receipts[0], dict) else None\n    if (\n        record.get(\"schema\") != ROBUSTNESS_RECORD_SCHEMA\n""",
)
replace_once(
    path,
    """            catalog = list_native_robustness_results(research_store)\n            catalog[\"results\"] = [_verified_robustness_public_record(item) for item in catalog[\"results\"]]\n            return 200, catalog\n""",
    """            catalog = list_native_robustness_results(research_store)\n            catalog[\"results\"] = [_verified_robustness_public_record(item) for item in catalog[\"results\"]]\n            catalog[\"failed_attempts\"] = [_verified_robustness_public_record(item) for item in catalog.get(\"failed_attempts\", [])]\n            return 200, catalog\n""",
)

# 4. Frontend preserves exact failed-attempt readback, clears failed bookmarks, and locks source identity in flight.
path = "web/research-backtest-robustness.mjs"
replace_once(
    path,
    """const ROBUSTNESS_SCHEMA = \"tc.research-native-robustness.v1\";\nconst ROBUSTNESS_CAPABILITIES_SCHEMA = \"tc.research-native-robustness-capabilities.v1\";\n""",
    """const ROBUSTNESS_SCHEMA = \"tc.research-native-robustness.v1\";\nconst ROBUSTNESS_ATTEMPT_SCHEMA = \"tc.research-native-robustness-attempt.v1\";\nconst ROBUSTNESS_CAPABILITIES_SCHEMA = \"tc.research-native-robustness-capabilities.v1\";\n""",
)
replace_once(
    path,
    """export async function fetchRobustnessResult(validationRef, fetchImpl = globalThis.fetch) {\n""",
    """export function robustnessAttemptFromPayload(payload) {\n  if (\n    !payload\n    || payload.schema !== ROBUSTNESS_ATTEMPT_SCHEMA\n    || payload.state !== \"failed\"\n    || payload.sqx_build !== \"144.2953\"\n    || payload.operation !== \"native_retester_cross_check\"\n    || payload.method !== HIGHER_PRECISION_METHOD\n    || typeof payload.attempt_ref !== \"string\"\n    || evidenceDigest(payload.attempt_ref) === \"\"\n    || typeof payload.proof_entity_id !== \"string\"\n    || !/^tc-research:proof:v1:[0-9a-f-]{36}$/.test(payload.proof_entity_id)\n    || typeof payload.proof_revision !== \"string\"\n    || !/^tc-research-revision:proof:sha256:[0-9a-f]{64}$/.test(payload.proof_revision)\n    || typeof payload.failure_reason_code !== \"string\"\n    || !payload.failure_reason_code\n    || typeof payload.partial_side_effect !== \"boolean\"\n    || !Array.isArray(payload.receipts)\n    || typeof payload.source_historical_result_entity_id !== \"string\"\n    || typeof payload.source_historical_result_revision !== \"string\"\n  ) {\n    throw new Error(\"Native robustness failed-attempt custody is invalid\");\n  }\n  return payload;\n}\n\nexport function robustnessReadbackFromPayload(payload) {\n  return payload?.schema === ROBUSTNESS_ATTEMPT_SCHEMA\n    ? robustnessAttemptFromPayload(payload)\n    : robustnessResultFromPayload(payload);\n}\n\nexport async function fetchRobustnessResult(validationRef, fetchImpl = globalThis.fetch) {\n""",
)
replace_once(
    path,
    """  return robustnessResultFromPayload(payload);\n}\n\nexport function robustnessCapabilitiesFromPayload(payload) {\n""",
    """  return robustnessReadbackFromPayload(payload);\n}\n\nexport function robustnessCapabilitiesFromPayload(payload) {\n""",
)
replace_once(
    path,
    """export function robustnessCatalogFromPayload(payload) {\n  if (!payload || payload.schema !== ROBUSTNESS_CATALOG_SCHEMA || !Array.isArray(payload.results)) {\n    throw new Error(\"Native robustness catalog schema is invalid\");\n  }\n  return payload.results.map(robustnessResultFromPayload);\n}\n""",
    """export function robustnessCatalogFromPayload(payload) {\n  if (\n    !payload\n    || payload.schema !== ROBUSTNESS_CATALOG_SCHEMA\n    || !Array.isArray(payload.results)\n    || !Array.isArray(payload.failed_attempts)\n  ) {\n    throw new Error(\"Native robustness catalog schema is invalid\");\n  }\n  return {\n    results: payload.results.map(robustnessResultFromPayload),\n    failedAttempts: payload.failed_attempts.map(robustnessAttemptFromPayload),\n  };\n}\n""",
)
replace_once(
    path,
    """function resultPanel(result) {\n  if (!result) {\n""",
    """function resultPanel(result) {\n  if (result?.schema === ROBUSTNESS_ATTEMPT_SCHEMA) {\n    const receiptState = result.receipts.map((item) => item.state).filter(Boolean).join(\", \") || \"no native receipt\";\n    return `<div data-robustness-attempt=\"${escapeHtml(result.attempt_ref)}\"><div class=\"context-callout\"><span class=\"callout-icon\">!</span><div><span class=\"eyebrow\">Native SQX attempt custody</span><strong>Higher Precision attempt did not complete cleanly</strong><span>This is durable execution-state evidence, not a producer robustness verdict.</span></div></div><div class=\"idea-identity\"><div class=\"stat-row\"><span>Attempt evidence</span><code>${escapeHtml(result.attempt_ref)}</code></div><div class=\"stat-row\"><span>Source Historical Result</span><code>${escapeHtml(result.source_historical_result_revision)}</code></div><div class=\"stat-row\"><span>Failure reason</span><code>${escapeHtml(result.failure_reason_code)}</code></div><div class=\"stat-row\"><span>Possible native side effect</span><code>${result.partial_side_effect ? \"yes\" : \"no\"}</code></div><div class=\"stat-row\"><span>Receipt state</span><code>${escapeHtml(receiptState)}</code></div></div></div>`;\n  }\n  if (!result) {\n""",
)
replace_once(
    path,
    """let state = { phase: \"idle\", results: [], selectedIndex: 0, runtimeReady: false, capabilities: null, catalog: [], validation: null, detail: \"\" };\n""",
    """let state = { phase: \"idle\", results: [], selectedIndex: 0, runtimeReady: false, capabilities: null, catalog: [], failedAttempts: [], validation: null, inFlightSource: null, detail: \"\" };\n""",
)
replace_once(
    path,
    """  const canRun = current.phase !== \"loading\" && current.runtimeReady && higherCapability?.state === \"ready\" && selected;\n""",
    """  const locked = current.phase === \"running\";\n  const canRun = ![\"loading\", \"running\"].includes(current.phase) && current.runtimeReady && higherCapability?.state === \"ready\" && selected;\n""",
)
replace_once(
    path,
    """    ? `<label class=\"field-label\" for=\"robustness-validation-result\">Captured robustness run</label><select id=\"robustness-validation-result\" class=\"idea-editor\">${matchingValidations.length > 1 && !current.validation ? '<option value=\"\" selected>Choose exact robustness run</option>' : \"\"}${matchingValidations.map((item) => `<option value=\"${escapeHtml(item.validation_ref)}\" ${current.validation?.validation_ref === item.validation_ref ? \"selected\" : \"\"}>${escapeHtml(short(item.validation_ref))} · ${escapeHtml(short(item.proof_revision))}</option>`).join(\"\")}</select>`\n""",
    """    ? `<label class=\"field-label\" for=\"robustness-validation-result\">Captured robustness run</label><select id=\"robustness-validation-result\" class=\"idea-editor\" ${locked ? \"disabled\" : \"\"}>${matchingValidations.length > 1 && !current.validation ? '<option value=\"\" selected>Choose exact robustness run</option>' : \"\"}${matchingValidations.map((item) => `<option value=\"${escapeHtml(item.validation_ref)}\" ${current.validation?.validation_ref === item.validation_ref ? \"selected\" : \"\"}>${escapeHtml(short(item.validation_ref))} · ${escapeHtml(short(item.proof_revision))}</option>`).join(\"\")}</select>`\n""",
)
replace_once(
    path,
    """      <select id=\"robustness-source-result\" class=\"idea-editor\" ${completed.length ? \"\" : \"disabled\"}>""",
    """      <select id=\"robustness-source-result\" class=\"idea-editor\" ${completed.length && !locked ? \"\" : \"disabled\"}>""",
)
replace_once(
    path,
    """    <section class=\"panel\" data-accent=\"purple\"><div class=\"panel-heading\"><div><p class=\"eyebrow\">Immutable readback</p><h2>Robustness result custody</h2></div></div>${validationPicker}${resultPanel(current.validation)}</section>\n""",
    """    <section class=\"panel\" data-accent=\"purple\"><div class=\"panel-heading\"><div><p class=\"eyebrow\">Immutable readback</p><h2>Robustness result custody</h2></div></div>${validationPicker}${resultPanel(current.validation)}${current.failedAttempts.length ? `<div class=\"requirement-list\" data-robustness-failed-attempts>${current.failedAttempts.map((item) => `<div class=\"requirement-item\"><div><strong>Failed native attempt</strong><span class=\"status-badge status-unavailable\"><span class=\"status-dot\"></span>${escapeHtml(item.failure_reason_code)}</span></div><p><code>${escapeHtml(short(item.attempt_ref))}</code> · partial side effect ${item.partial_side_effect ? \"possible\" : \"not observed\"}</p></div>`).join(\"\")}</div>` : \"\"}</section>\n""",
)
replace_once(
    path,
    """    const [results, runtime, capabilities, catalog] = await Promise.all([\n""",
    """    const [results, runtime, capabilities, catalogRead] = await Promise.all([\n""",
)
replace_once(
    path,
    """    const completed = results.filter((item) => item.state === \"completed\" && item.execution_completed === true);\n    let selectedIndex = 0;\n    let validation = completed[0] ? robustnessResultForHistorical(catalog, completed[0]) : null;\n""",
    """    const catalog = catalogRead.results;\n    const failedAttempts = catalogRead.failedAttempts;\n    const completed = results.filter((item) => item.state === \"completed\" && item.execution_completed === true);\n    let selectedIndex = 0;\n    let validation = completed[0] ? robustnessResultForHistorical(catalog, completed[0]) : null;\n""",
)
replace_once(
    path,
    """      } catch (error) {\n        detail = `Saved robustness result unavailable: ${error instanceof Error ? error.message : \"readback failed\"}`;\n      }\n""",
    """      } catch (error) {\n        validation = null;\n        clearValidationRef();\n        detail = `Saved robustness result unavailable: ${error instanceof Error ? error.message : \"readback failed\"}`;\n      }\n""",
)
replace_once(
    path,
    """      catalog,\n      validation,\n      detail,\n""",
    """      catalog,\n      failedAttempts,\n      validation,\n      inFlightSource: null,\n      detail,\n""",
)
replace_once(
    path,
    """  if (state.phase === \"loading\" || !state.runtimeReady || higherCapability?.state !== \"ready\") return;\n""",
    """  if ([\"loading\", \"running\"].includes(state.phase) || !state.runtimeReady || higherCapability?.state !== \"ready\") return;\n""",
)
replace_once(
    path,
    """  button.disabled = true;\n  button.textContent = \"Running Higher Precision in SQX…\";\n  try {\n""",
    """  const inFlightSource = { entity_id: selected.entity_id, revision: selected.revision };\n  state = { ...state, phase: \"running\", inFlightSource, validation: null, detail: \"Running Higher Precision in SQX…\" };\n  render(panel(), state);\n  try {\n""",
)
replace_once(
    path,
    """    persistValidationRef(validation.validation_ref);\n    state = { ...state, phase: \"loaded\", validation, catalog: [validation, ...state.catalog.filter((item) => item.validation_ref !== validation.validation_ref)], detail: \"Native Higher Precision result captured. Producer verdict remains unread.\" };\n  } catch (error) {\n    state = { ...state, phase: \"failed\", detail: error instanceof Error ? error.message : \"Native Higher Precision execution failed\" };\n  }\n""",
    """    const completedNow = state.results.filter((item) => item.state === \"completed\" && item.execution_completed === true);\n    const sourceIndex = completedNow.findIndex((item) => item.entity_id === inFlightSource.entity_id && item.revision === inFlightSource.revision);\n    if (sourceIndex < 0) {\n      clearValidationRef();\n      state = { ...state, phase: \"loaded\", validation: null, inFlightSource: null, detail: \"Native result captured, but its source Historical Result is no longer current; receipt was not cross-displayed.\" };\n    } else {\n      persistValidationRef(validation.validation_ref);\n      state = { ...state, phase: \"loaded\", selectedIndex: sourceIndex, validation, inFlightSource: null, catalog: [validation, ...state.catalog.filter((item) => item.validation_ref !== validation.validation_ref)], detail: \"Native Higher Precision result captured. Producer verdict remains unread.\" };\n    }\n  } catch (error) {\n    let failedAttempts = state.failedAttempts;\n    try { failedAttempts = (await fetchRobustnessCatalog()).failedAttempts; } catch {}\n    state = { ...state, phase: \"loaded\", inFlightSource: null, failedAttempts, detail: error instanceof Error ? error.message : \"Native Higher Precision execution failed\" };\n  }\n""",
)
replace_once(
    path,
    """    if (!robustnessRoute()) return;\n    const completed = state.results.filter((item) => item.state === \"completed\" && item.execution_completed === true);\n""",
    """    if (!robustnessRoute() || state.phase === \"running\") return;\n    const completed = state.results.filter((item) => item.state === \"completed\" && item.execution_completed === true);\n""",
)

# 5. Backend tests: exact baseline gateway binding, failed-attempt readback, and real PermissionError.
path = "tests/product/test_sqx_retester_gateway.py"
replace_once(
    path,
    """        project = b\"exact retester project\"\n        (project_root / \"project.cfx\").write_bytes(project)\n        return (\n            root,\n            project_name,\n            sha256(launcher).hexdigest(),\n            sha256(project).hexdigest(),\n            sha256(engine).hexdigest(),\n        )\n""",
    """        project = b\"exact retester project\"\n        (project_root / \"project.cfx\").write_bytes(project)\n        results = project_root / \"databanks/Results\"\n        results.mkdir(parents=True)\n        baseline = b\"exact staged baseline\"\n        (results / \"Baseline.sqx\").write_bytes(baseline)\n        return (\n            root,\n            project_name,\n            sha256(launcher).hexdigest(),\n            sha256(project).hexdigest(),\n            sha256(engine).hexdigest(),\n            sha256(baseline).hexdigest(),\n        )\n""",
)
# update all unpackings with baseline hash using a controlled broad replacement
text = Path(path).read_text(encoding="utf-8")
text = text.replace("home, project_name, launcher_hash, project_hash, engine_hash = self._runtime(Path(tmp))", "home, project_name, launcher_hash, project_hash, engine_hash, baseline_hash = self._runtime(Path(tmp))")
text = text.replace("home, _, launcher_hash, project_hash, engine_hash = self._runtime(Path(tmp))", "home, _, launcher_hash, project_hash, engine_hash, baseline_hash = self._runtime(Path(tmp))")
text = text.replace("            expected_engine_sha256=engine_hash,\n", "            expected_engine_sha256=engine_hash,\n            result_archive_name=\"Baseline.sqx\",\n            expected_result_archive_sha256=baseline_hash,\n")
Path(path).write_text(text, encoding="utf-8")
# add baseline mutation regression before final module guard
replace_once(
    path,
    """\n\nif __name__ == \"__main__\":\n    unittest.main()\n""",
    """\n\n    def test_staged_baseline_change_refuses_before_runner(self) -> None:\n        with TemporaryDirectory() as tmp:\n            home, project_name, launcher_hash, project_hash, engine_hash, baseline_hash = self._runtime(Path(tmp))\n            (home / \"user/projects\" / project_name / \"databanks/Results/Baseline.sqx\").write_bytes(b\"changed baseline\")\n            calls = 0\n\n            def runner(*args, **kwargs):\n                nonlocal calls\n                calls += 1\n                return subprocess.CompletedProcess(args, 0)\n\n            with self.assertRaises(SqxNativeGatewayError) as caught:\n                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_retester_task(\n                    project_name,\n                    expected_project_sha256=project_hash,\n                    expected_engine_sha256=engine_hash,\n                    result_archive_name=\"Baseline.sqx\",\n                    expected_result_archive_sha256=baseline_hash,\n                )\n        self.assertEqual(caught.exception.code, \"retester_result_archive_hash_mismatch\")\n        self.assertEqual(calls, 0)\n\n\nif __name__ == \"__main__\":\n    unittest.main()\n""",
)

path = "tests/product/test_research_robustness.py"
# fake gateway signature and assertions/receipt bindings
text = Path(path).read_text(encoding="utf-8")
text = text.replace(
    "def launch_retester_task(self, project_name, *, expected_project_sha256, expected_engine_sha256):",
    "def launch_retester_task(self, project_name, *, expected_project_sha256, expected_engine_sha256, result_archive_name=None, expected_result_archive_sha256=None):",
)
text = text.replace(
    "                engine = self.home / \"internal/libs/SQTradingLib.jar\"\n                outer.assertEqual(sha256(engine.read_bytes()).hexdigest(), expected_engine_sha256)\n",
    "                engine = self.home / \"internal/libs/SQTradingLib.jar\"\n                outer.assertEqual(sha256(engine.read_bytes()).hexdigest(), expected_engine_sha256)\n                baseline = self.home / \"user/projects\" / project_name / \"databanks/Results\" / result_archive_name\n                outer.assertEqual(sha256(baseline.read_bytes()).hexdigest(), expected_result_archive_sha256)\n",
)
text = text.replace(
    '                    "engine_sha256": expected_engine_sha256,\n                    "control_requests_submitted": 1,',
    '                    "engine_sha256": expected_engine_sha256,\n                    "result_archive_name": result_archive_name,\n                    "result_archive_relative_path": f"user/projects/{project_name}/databanks/Results/{result_archive_name}",\n                    "result_archive_sha256": expected_result_archive_sha256,\n                    "control_requests_submitted": 1,',
)
text = text.replace(
    '                        "engine_sha256": expected_engine_sha256,\n                        "reason_code": None,',
    '                        "engine_sha256": expected_engine_sha256,\n                        "result_archive_name": result_archive_name,\n                        "result_archive_relative_path": f"user/projects/{project_name}/databanks/Results/{result_archive_name}",\n                        "result_archive_sha256": expected_result_archive_sha256,\n                        "reason_code": None,',
)
# failed attempt should now be discoverable and exactly reopenable
text = text.replace(
    '            self.assertEqual(list_native_robustness_results(store)["results"], [])',
    '            catalog = list_native_robustness_results(store)\n            self.assertEqual(catalog["results"], [])\n            self.assertEqual(len(catalog["failed_attempts"]), 1)\n            attempt = catalog["failed_attempts"][0]\n            self.assertEqual(attempt["failure_reason_code"], "sqx_control_timeout")\n            self.assertEqual(read_native_robustness_result(store, attempt["attempt_ref"]), attempt)',
)
# replace synthetic custody exception with one-shot real filesystem exception
text = text.replace(
    '            def flaky_put(value: bytes):\n                if value == result_bytes:\n                    raise ResearchCustodyError("immutable_evidence_corrupt", "simulated completed-result evidence failure")\n                return original_put(value)',
    '            failed_once = False\n\n            def flaky_put(value: bytes):\n                nonlocal failed_once\n                if value == result_bytes and not failed_once:\n                    failed_once = True\n                    raise PermissionError("simulated completed-result filesystem failure")\n                return original_put(value)',
)
Path(path).write_text(text, encoding="utf-8")

# 6. JS tests adopt catalog union and failed-attempt validation.
path = "tests/research-backtest-robustness.test.mjs"
text = Path(path).read_text(encoding="utf-8")
text = text.replace(
    "  robustnessCapabilitiesFromPayload,\n",
    "  robustnessAttemptFromPayload,\n  robustnessCapabilitiesFromPayload,\n",
)
text = text.replace(
    '  const catalogPayload = { schema: "tc.research-native-robustness-catalog.v1", results: [robustness()] };\n  assert.equal(robustnessCatalogFromPayload(catalogPayload)[0].validation_ref, `tc-evidence:sha256:${validationSha}`);',
    '  const failedAttempt = {\n    schema: "tc.research-native-robustness-attempt.v1", state: "failed", sqx_build: "144.2953", operation: "native_retester_cross_check", method: "RetestWithHigherPrecision",\n    attempt_ref: `tc-evidence:sha256:${"0".repeat(64)}`, proof_entity_id: "tc-research:proof:v1:55555555-5555-4555-8555-555555555555", proof_revision: `tc-research-revision:proof:sha256:${"1".repeat(64)}`,\n    source_historical_result_entity_id: historicalEntity, source_historical_result_revision: historicalRevision, failure_reason_code: "sqx_command_timeout", partial_side_effect: true, receipts: [{ state: "timeout" }],\n  };\n  assert.equal(robustnessAttemptFromPayload(failedAttempt).failure_reason_code, "sqx_command_timeout");\n  const catalogPayload = { schema: "tc.research-native-robustness-catalog.v1", results: [robustness()], failed_attempts: [failedAttempt] };\n  const parsedCatalog = robustnessCatalogFromPayload(catalogPayload);\n  assert.equal(parsedCatalog.results[0].validation_ref, `tc-evidence:sha256:${validationSha}`);\n  assert.equal(parsedCatalog.failedAttempts[0].attempt_ref, failedAttempt.attempt_ref);',
)
Path(path).write_text(text, encoding="utf-8")

print("round-three patch applied")
