from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


path = Path("product/tradercockpit/research_robustness.py")
text = path.read_text(encoding="utf-8")
text = replace_once(text, "from uuid import uuid4\n", "from uuid import UUID, uuid4\n", "uuid import")
text = replace_once(
    text,
    'ROBUSTNESS_OUTCOME_UNREAD = "producer_result_captured_outcome_unread"\n_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")\n',
    'ROBUSTNESS_OUTCOME_UNREAD = "producer_result_captured_outcome_unread"\n'
    'ROBUSTNESS_ATTEMPT_SCHEMA = "tc.research-native-robustness-attempt.v1"\n'
    'ROBUSTNESS_CATALOG_SCHEMA = "tc.research-native-robustness-catalog.v1"\n'
    'ROBUSTNESS_CAPABILITIES_SCHEMA = "tc.research-native-robustness-capabilities.v1"\n'
    '_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")\n'
    '_CURRENT_POINTER_TEMP_RE = re.compile(\n'
    '    r"^\\.[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.json\\.tmp-[0-9]+-[0-9a-f]{32}$"\n'
    ')\n',
    "robustness constants",
)

helpers = r'''

def _current_proof_entities(store: FileResearchCustodyStore) -> tuple[ResearchEntityId, ...]:
    directory = store.base / "current" / ResearchKind.PROOF.value
    if not directory.exists():
        return ()
    if not directory.is_dir():
        raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "robustness proof current-pointer directory is invalid")
    entities: list[ResearchEntityId] = []
    for path in sorted(directory.iterdir(), key=lambda item: item.name):
        if _CURRENT_POINTER_TEMP_RE.fullmatch(path.name):
            continue
        if not path.is_file() or path.suffix != ".json":
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "robustness proof current-pointer directory contains an unexpected entry")
        try:
            value = UUID(path.stem)
        except ValueError as exc:
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "robustness proof current-pointer filename is not a canonical UUID") from exc
        if str(value) != path.stem:
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "robustness proof current-pointer UUID is not canonical")
        entity = ResearchEntityId(ResearchKind.PROOF, value)
        try:
            store.current(entity)
        except ResearchCustodyError as exc:
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", exc.detail) from exc
        entities.append(entity)
    return tuple(entities)


def _failed_successor(
    store: FileResearchCustodyStore,
    entity: ResearchEntityId,
    prepared_revision: ResearchRevisionRef,
    prepared: dict[str, object],
    evidence: tuple[EvidenceRef, ...],
    *,
    reason_code: str,
    launcher_sha256: str | None,
    receipts: tuple[dict[str, object], ...],
    partial_side_effect: bool,
) -> ResearchRevisionRef:
    failed = {
        **prepared,
        "state": "failed",
        "launcher_sha256": launcher_sha256 if isinstance(launcher_sha256, str) and _DIGEST_RE.fullmatch(launcher_sha256) else None,
        "receipts": [dict(item) for item in receipts],
        "partial_side_effect": bool(partial_side_effect),
        "failure_reason_code": reason_code,
    }
    revision = store.create_revision(
        entity,
        _canonical(failed),
        parent_revision=prepared_revision,
        evidence=evidence,
    )
    store.compare_and_set_current(
        entity,
        expected_revision=prepared_revision,
        target_revision=revision.revision,
    )
    return revision.revision


def _completed_proof_records(store: FileResearchCustodyStore) -> list[dict[str, object]]:
    results: list[dict[str, object]] = []
    for entity in _current_proof_entities(store):
        revision = store.current(entity)
        stored = store.read_revision(revision)
        content = store.read_revision_content(revision)
        try:
            raw = json.loads(content)
        except (UnicodeDecodeError, json.JSONDecodeError):
            continue
        if not isinstance(raw, dict) or raw.get("schema") != ROBUSTNESS_RECORD_SCHEMA:
            continue
        record_ref = stored.content
        record = _read_record(store, record_ref)
        if stored.parent_revision is None:
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "completed robustness proof has no prepared parent")
        parent_revision = store.read_revision(stored.parent_revision)
        try:
            prepared = json.loads(store.read_revision_content(stored.parent_revision))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "prepared robustness proof is unreadable") from exc
        if not isinstance(prepared, dict) or prepared.get("schema") != ROBUSTNESS_ATTEMPT_SCHEMA or prepared.get("state") != "prepared":
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "completed robustness proof parent is not one prepared native attempt")
        identity_keys = (
            "sqx_build", "operation", "method",
            "source_historical_result_entity_id", "source_historical_result_revision",
            "source_result_archive_ref", "source_result_archive_sha256",
            "source_project_ref", "source_project_sha256",
            "compiled_project_ref", "compiled_project_sha256", "configuration_changed",
            "source_task_sha256", "compiled_task_sha256", "native_settings",
            "engine_ref", "engine_sha256", "native_project_name", "native_project_relative_path",
        )
        if any(prepared.get(key) != record.get(key) for key in identity_keys):
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "completed robustness proof does not match its prepared control identity")
        try:
            prepared_evidence = {
                EvidenceRef.parse(prepared["source_result_archive_ref"]),
                EvidenceRef.parse(prepared["source_project_ref"]),
                EvidenceRef.parse(prepared["compiled_project_ref"]),
                EvidenceRef.parse(prepared["engine_ref"]),
            }
            completed_evidence = prepared_evidence | {
                EvidenceRef.parse(record["result_archive_ref"]),
                EvidenceRef.parse(record["result_strategy_ref"]),
                EvidenceRef.parse(record["result_settings_ref"]),
            }
        except (KeyError, TypeError, ResearchCustodyError) as exc:
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "robustness proof evidence identities are invalid") from exc
        if set(parent_revision.evidence) != prepared_evidence or set(stored.evidence) != completed_evidence:
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "robustness proof revision evidence set is invalid")
        results.append({
            **record,
            "validation_ref": str(record_ref),
            "proof_entity_id": str(entity),
            "proof_revision": str(revision),
        })
    return results


def list_native_robustness_results(store: FileResearchCustodyStore) -> dict[str, object]:
    """List completed native robustness proofs from durable Research custody."""

    return {
        "schema": ROBUSTNESS_CATALOG_SCHEMA,
        "results": _completed_proof_records(store),
    }


def read_native_robustness_capabilities(sqx_home: Path | str | None) -> dict[str, object]:
    """Read installed SQX producer capability without inventing client-side truth."""

    def unavailable(code: str, detail: str) -> dict[str, object]:
        return {
            "schema": ROBUSTNESS_CAPABILITIES_SCHEMA,
            "sqx_build": SQX_BUILD,
            "methods": [{
                "method": ROBUSTNESS_METHOD_HIGHER_PRECISION,
                "state": "unavailable",
                "reason_code": code,
                "detail": detail,
                "native_settings": None,
                "configuration_changed": None,
                "source_project_sha256": None,
                "compiled_project_sha256": None,
                "engine_sha256": None,
            }],
        }

    try:
        home = verified_sqx_home(sqx_home)
        source_project_bytes, _, _ = _read_exact_inside(
            home,
            f"user/projects/{RETESTER_SOURCE_PROJECT}/project.cfx",
            missing_code="retester_source_project_missing",
            escape_code="retester_source_project_path_escape",
        )
        _, _, engine_sha = _read_exact_inside(
            home,
            RETESTER_ENGINE_RELATIVE_PATH,
            missing_code="retester_engine_missing",
            escape_code="retester_engine_path_escape",
        )
        _, plan = compile_higher_precision_project(source_project_bytes)
    except (SqxPresetRuntimeError, ResearchRetesterError, ResearchRobustnessError) as exc:
        return unavailable(exc.code, exc.detail)

    return {
        "schema": ROBUSTNESS_CAPABILITIES_SCHEMA,
        "sqx_build": SQX_BUILD,
        "methods": [{
            "method": ROBUSTNESS_METHOD_HIGHER_PRECISION,
            "state": "ready",
            "reason_code": None,
            "detail": "Installed SQX Retester project contains one structurally usable Higher Precision profile.",
            "native_settings": plan["native_settings"],
            "configuration_changed": plan["configuration_changed"],
            "source_project_sha256": plan["source_project_sha256"],
            "compiled_project_sha256": plan["compiled_project_sha256"],
            "engine_sha256": engine_sha,
        }],
    }

'''
text = replace_once(text, "\ndef _record_identity(payload: dict[str, object]) -> None:\n", helpers + "\ndef _record_identity(payload: dict[str, object]) -> None:\n", "proof helpers")
text = replace_once(
    text,
    '        or receipt.get("sqx_build") != SQX_BUILD\n        or receipt.get("launcher_sha256") != launcher\n',
    '        or receipt.get("sqx_build") != SQX_BUILD\n        or receipt.get("launcher_sha256") != launcher\n        or receipt.get("project") != payload.get("native_project_name")\n        or receipt.get("project_sha256") != payload.get("compiled_project_sha256")\n        or receipt.get("engine_sha256") != payload.get("engine_sha256")\n',
    "receipt custody binding",
)
old_read = '''def read_native_robustness_result(
    store: FileResearchCustodyStore,
    validation_ref: str | EvidenceRef,
) -> dict[str, object]:
    """Reopen one exact immutable native robustness execution record."""

    try:
        ref = validation_ref if isinstance(validation_ref, EvidenceRef) else EvidenceRef.parse(validation_ref)
    except (ResearchCustodyError, TypeError) as exc:
        raise ResearchRobustnessError("robustness_record_ref_invalid", "validation_ref is not a valid evidence identity") from exc
    payload = _read_record(store, ref)
    return {**payload, "validation_ref": str(ref)}


'''
new_read = '''def read_native_robustness_result(
    store: FileResearchCustodyStore,
    validation_ref: str | EvidenceRef,
) -> dict[str, object]:
    """Reopen one exact immutable native robustness execution record."""

    try:
        ref = validation_ref if isinstance(validation_ref, EvidenceRef) else EvidenceRef.parse(validation_ref)
    except (ResearchCustodyError, TypeError) as exc:
        raise ResearchRobustnessError("robustness_record_ref_invalid", "validation_ref is not a valid evidence identity") from exc
    payload = _read_record(store, ref)
    matches = [item for item in _completed_proof_records(store) if item.get("validation_ref") == str(ref)]
    if len(matches) > 1:
        raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "multiple current robustness proofs reference one validation record")
    return matches[0] if matches else {**payload, "validation_ref": str(ref)}


'''
text = replace_once(text, old_read, new_read, "read robustness result")

start_at = text.index("def start_native_higher_precision(")
text = text[:start_at] + r'''def start_native_higher_precision(
    store: FileResearchCustodyStore,
    sqx_home: Path | str | None,
    trusted_launcher_sha256: str | None,
    *,
    historical_result_entity_id: str,
    expected_historical_result_revision: str,
    gateway_factory=SqxNativeControlGateway,
) -> dict[str, object]:
    """Run installed SQX Higher Precision against one exact Historical Result."""

    try:
        historical = read_current_historical_result(store, historical_result_entity_id)
    except (ResearchRetesterError, ResearchCustodyError) as exc:
        raise ResearchRobustnessError(exc.code, exc.detail) from exc
    if historical.get("revision") != expected_historical_result_revision:
        raise ResearchRobustnessError(
            "robustness_source_revision_changed",
            "Historical Result revision changed before robustness execution",
        )
    if historical.get("state") != "completed" or historical.get("execution_completed") is not True:
        raise ResearchRobustnessError(
            "robustness_source_result_incomplete",
            "Higher Precision requires one completed native Historical Result",
        )
    if historical.get("sqx_build") != SQX_BUILD:
        raise ResearchRobustnessError(
            "robustness_source_build_mismatch",
            "Historical Result SQX build does not match the native runtime contract",
        )

    try:
        source_result_ref = EvidenceRef.parse(historical["result_archive_ref"])
        source_result_bytes = store.read_evidence(source_result_ref)
    except (KeyError, ResearchCustodyError, TypeError) as exc:
        raise ResearchRobustnessError("robustness_source_result_invalid", "Historical Result archive evidence is invalid") from exc
    source_result_sha = sha256(source_result_bytes).hexdigest()
    if source_result_sha != historical.get("result_archive_sha256") or source_result_ref.digest != source_result_sha:
        raise ResearchRobustnessError(
            "robustness_source_result_invalid",
            "Historical Result archive evidence binding is invalid",
        )
    try:
        source_info = inspect_sqx_output_bytes(
            source_result_bytes,
            archive_name=historical["result_archive_name"],
        )
    except (KeyError, SqxOutputError, TypeError) as exc:
        detail = getattr(exc, "detail", "Historical Result archive is invalid")
        raise ResearchRobustnessError("robustness_source_result_invalid", str(detail)) from exc
    if source_info["archive_sha256"] != source_result_sha:
        raise ResearchRobustnessError("robustness_source_result_invalid", "Historical Result archive hash is inconsistent")

    try:
        home = verified_sqx_home(sqx_home)
    except SqxPresetRuntimeError as exc:
        raise ResearchRobustnessError(exc.code, exc.detail) from exc
    try:
        source_project_bytes, _, source_project_sha = _read_exact_inside(
            home,
            f"user/projects/{RETESTER_SOURCE_PROJECT}/project.cfx",
            missing_code="retester_source_project_missing",
            escape_code="retester_source_project_path_escape",
        )
        engine_bytes, _, engine_sha = _read_exact_inside(
            home,
            RETESTER_ENGINE_RELATIVE_PATH,
            missing_code="retester_engine_missing",
            escape_code="retester_engine_path_escape",
        )
    except ResearchRetesterError as exc:
        raise ResearchRobustnessError(exc.code, exc.detail) from exc

    compiled_project_bytes, plan = compile_higher_precision_project(source_project_bytes)
    compiled_project_sha = _digest(plan["compiled_project_sha256"], "robustness_compiled_project_invalid")

    source_project_ref = store.put_evidence(source_project_bytes)
    compiled_project_ref = store.put_evidence(compiled_project_bytes)
    engine_ref = store.put_evidence(engine_bytes)
    project_name, project_file, project_relative = _stage_workspace(
        home,
        compiled_project_bytes,
        historical["result_archive_name"],
        source_result_bytes,
    )
    if sha256(project_file.read_bytes()).hexdigest() != compiled_project_sha:
        raise ResearchRobustnessError("robustness_stage_corrupt", "staged compiled project changed before launch")

    proof_entity = store.create_entity(ResearchKind.PROOF)
    prepared_evidence = tuple({source_result_ref, source_project_ref, compiled_project_ref, engine_ref})
    prepared = {
        "schema": ROBUSTNESS_ATTEMPT_SCHEMA,
        "state": "prepared",
        "sqx_build": SQX_BUILD,
        "operation": ROBUSTNESS_OPERATION,
        "method": ROBUSTNESS_METHOD_HIGHER_PRECISION,
        "source_historical_result_entity_id": historical_result_entity_id,
        "source_historical_result_revision": expected_historical_result_revision,
        "source_result_archive_ref": str(source_result_ref),
        "source_result_archive_sha256": source_result_sha,
        "source_project_ref": str(source_project_ref),
        "source_project_sha256": source_project_sha,
        "compiled_project_ref": str(compiled_project_ref),
        "compiled_project_sha256": compiled_project_sha,
        "configuration_changed": plan["configuration_changed"],
        "source_task_sha256": plan["source_task_sha256"],
        "compiled_task_sha256": plan["compiled_task_sha256"],
        "native_settings": plan["native_settings"],
        "engine_ref": str(engine_ref),
        "engine_sha256": engine_sha,
        "native_project_name": project_name,
        "native_project_relative_path": project_relative,
        "launcher_sha256": None,
        "receipts": [],
        "partial_side_effect": False,
        "failure_reason_code": None,
    }
    prepared_revision = store.create_revision(
        proof_entity,
        _canonical(prepared),
        evidence=prepared_evidence,
    )
    store.compare_and_set_current(
        proof_entity,
        expected_revision=None,
        target_revision=prepared_revision.revision,
    )

    try:
        _, _, launch_engine_sha = _read_exact_inside(
            home,
            RETESTER_ENGINE_RELATIVE_PATH,
            missing_code="retester_engine_missing",
            escape_code="retester_engine_path_escape",
        )
    except ResearchRetesterError as exc:
        _failed_successor(
            store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,
            reason_code=exc.code, launcher_sha256=None, receipts=(), partial_side_effect=False,
        )
        raise ResearchRobustnessError(exc.code, exc.detail) from exc
    if launch_engine_sha != engine_sha:
        code = "robustness_engine_changed_before_execution"
        _failed_successor(
            store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,
            reason_code=code, launcher_sha256=None, receipts=(), partial_side_effect=False,
        )
        raise ResearchRobustnessError(
            code,
            "installed SQTradingLib.jar changed before native robustness launch",
        )

    try:
        receipt = gateway_factory(sqx_home, trusted_launcher_sha256).launch_retester_task(
            project_name,
            expected_project_sha256=compiled_project_sha,
            expected_engine_sha256=engine_sha,
        )
    except SqxNativeGatewayError as exc:
        model = exc.read_model()
        receipts = tuple(dict(item) for item in model["receipts"])
        launcher = next((item.get("launcher_sha256") for item in reversed(receipts) if item.get("launcher_sha256")), None)
        _failed_successor(
            store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,
            reason_code=exc.code,
            launcher_sha256=launcher if isinstance(launcher, str) else None,
            receipts=receipts,
            partial_side_effect=bool(model["partial_side_effect"]),
        )
        raise ResearchRobustnessError(exc.code, exc.detail) from exc

    raw_receipts = receipt.get("receipts")
    receipt_items = tuple(dict(item) for item in raw_receipts) if isinstance(raw_receipts, list) and all(isinstance(item, dict) for item in raw_receipts) else ()
    raw_launcher = receipt.get("launcher_sha256")
    receipt_valid = (
        receipt.get("schema") == "tc.sqx-native-control.v1"
        and receipt.get("operation") == "retester_start_task"
        and receipt.get("project") == project_name
        and receipt.get("task") == 1
        and receipt.get("state") == "submitted"
        and receipt.get("sqx_build") == SQX_BUILD
        and receipt.get("project_sha256") == compiled_project_sha
        and receipt.get("engine_sha256") == engine_sha
        and receipt.get("project_relative_path") == project_relative
        and isinstance(raw_launcher, str)
        and _DIGEST_RE.fullmatch(raw_launcher) is not None
        and len(receipt_items) == 1
        and receipt_items[0].get("action") == "startOnlyTask"
        and receipt_items[0].get("project") == project_name
        and receipt_items[0].get("task") == 1
        and receipt_items[0].get("state") == "completed"
        and receipt_items[0].get("sqx_build") == SQX_BUILD
        and receipt_items[0].get("launcher_sha256") == raw_launcher
        and receipt_items[0].get("project_sha256") == compiled_project_sha
        and receipt_items[0].get("engine_sha256") == engine_sha
    )
    if not receipt_valid:
        _failed_successor(
            store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,
            reason_code="robustness_receipt_invalid",
            launcher_sha256=raw_launcher if isinstance(raw_launcher, str) else None,
            receipts=receipt_items,
            partial_side_effect=True,
        )
        raise ResearchRobustnessError(
            "robustness_receipt_invalid",
            "native Retester gateway returned an invalid Higher Precision receipt",
        )
    launcher_sha = _digest(raw_launcher, "robustness_receipt_invalid")
    receipts = receipt_items

    try:
        _, _, completed_engine_sha = _read_exact_inside(
            home,
            RETESTER_ENGINE_RELATIVE_PATH,
            missing_code="retester_engine_missing",
            escape_code="retester_engine_path_escape",
        )
        if completed_engine_sha != engine_sha:
            raise ResearchRobustnessError(
                "robustness_engine_changed_during_execution",
                "installed SQTradingLib.jar changed across native robustness execution",
            )
        result_bytes, result_info = _capture_result(home, project_name)
        if result_info["archive_sha256"] == source_result_sha:
            raise ResearchRobustnessError(
                "robustness_result_unchanged",
                "native Higher Precision execution did not produce a changed SQX result archive",
            )
        result_strategy = _member(result_bytes, "strategy_Portfolio.xml")
        result_settings = _member(result_bytes, "settings.xml")
    except ResearchRetesterError as exc:
        _failed_successor(
            store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,
            reason_code=exc.code, launcher_sha256=launcher_sha, receipts=receipts, partial_side_effect=True,
        )
        raise ResearchRobustnessError(exc.code, exc.detail) from exc
    except ResearchRobustnessError as exc:
        _failed_successor(
            store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,
            reason_code=exc.code, launcher_sha256=launcher_sha, receipts=receipts, partial_side_effect=True,
        )
        raise

    result_ref = store.put_evidence(result_bytes)
    result_strategy_ref = store.put_evidence(result_strategy)
    result_settings_ref = store.put_evidence(result_settings)

    record = {
        "schema": ROBUSTNESS_RECORD_SCHEMA,
        "sqx_build": SQX_BUILD,
        "operation": ROBUSTNESS_OPERATION,
        "method": ROBUSTNESS_METHOD_HIGHER_PRECISION,
        "source_historical_result_entity_id": historical_result_entity_id,
        "source_historical_result_revision": expected_historical_result_revision,
        "source_result_archive_ref": str(source_result_ref),
        "source_result_archive_sha256": source_result_sha,
        "source_project_ref": str(source_project_ref),
        "source_project_sha256": source_project_sha,
        "compiled_project_ref": str(compiled_project_ref),
        "compiled_project_sha256": compiled_project_sha,
        "configuration_changed": plan["configuration_changed"],
        "source_task_sha256": plan["source_task_sha256"],
        "compiled_task_sha256": plan["compiled_task_sha256"],
        "native_settings": plan["native_settings"],
        "engine_ref": str(engine_ref),
        "engine_sha256": engine_sha,
        "launcher_sha256": launcher_sha,
        "native_project_name": project_name,
        "native_project_relative_path": project_relative,
        "receipts": [dict(item) for item in receipts],
        "result_archive_name": result_info["archive"],
        "result_archive_ref": str(result_ref),
        "result_archive_sha256": result_info["archive_sha256"],
        "result_strategy_ref": str(result_strategy_ref),
        "result_strategy_sha256": result_info["strategy_entry_sha256"],
        "result_settings_ref": str(result_settings_ref),
        "result_settings_sha256": result_info["settings_entry_sha256"],
        "execution_state": "completed",
        "producer_outcome_state": ROBUSTNESS_OUTCOME_UNREAD,
    }
    completed_revision = store.create_revision(
        proof_entity,
        _canonical(record),
        parent_revision=prepared_revision.revision,
        evidence=prepared_evidence + (result_ref, result_strategy_ref, result_settings_ref),
    )
    store.compare_and_set_current(
        proof_entity,
        expected_revision=prepared_revision.revision,
        target_revision=completed_revision.revision,
    )
    record_ref = completed_revision.content
    reopened = _read_record(store, record_ref)
    return {
        **reopened,
        "validation_ref": str(record_ref),
        "proof_entity_id": str(proof_entity),
        "proof_revision": str(completed_revision.revision),
    }
'''
path.write_text(text, encoding="utf-8")

path = Path("product/tradercockpit/research_retester_http.py")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    "    ResearchRobustnessError,\n    read_native_robustness_result,\n    start_native_higher_precision,\n",
    "    ResearchRobustnessError,\n    list_native_robustness_results,\n    read_native_robustness_capabilities,\n    read_native_robustness_result,\n    start_native_higher_precision,\n",
    "http robustness imports",
)
marker = '    action = payload.get("action")\n'
actions = '''    action = payload.get("action")
    if action == "read-robustness-capabilities":
        if set(payload) != {"action"}:
            return 400, {
                "error": "invalid_request",
                "reason_code": "robustness_capabilities_invalid",
                "detail": "Robustness capabilities read accepts only action=read-robustness-capabilities.",
            }
        return 200, read_native_robustness_capabilities(sqx_home)

    if action == "list-robustness":
        if set(payload) != {"action"}:
            return 400, {
                "error": "invalid_request",
                "reason_code": "robustness_catalog_invalid",
                "detail": "Robustness catalog read accepts only action=list-robustness.",
            }
        try:
            catalog = list_native_robustness_results(research_store)
            catalog["results"] = [_verified_robustness_public_record(item) for item in catalog["results"]]
            return 200, catalog
        except ResearchRobustnessError as exc:
            return _robustness_error_response(exc)
        except ResearchCustodyError as exc:
            return 409, {"error": "invalid_state", "reason_code": exc.code, "detail": exc.detail}

'''
text = replace_once(text, marker, actions, "http read actions")
text = replace_once(
    text,
    '            "detail": "Historical Result action must be start-retester, start-higher-precision, or read-robustness with its exact identity fields.",\n',
    '            "detail": "Historical Result action must be start-retester or one of the registered robustness read/start actions with its exact identity fields.",\n',
    "http invalid action detail",
)
path.write_text(text, encoding="utf-8")

path = Path("web/research-backtest-robustness.mjs")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    'const ROBUSTNESS_SCHEMA = "tc.research-native-robustness.v1";\nconst HIGHER_PRECISION_METHOD = "RetestWithHigherPrecision";\n',
    'const ROBUSTNESS_SCHEMA = "tc.research-native-robustness.v1";\nconst ROBUSTNESS_CAPABILITIES_SCHEMA = "tc.research-native-robustness-capabilities.v1";\nconst ROBUSTNESS_CATALOG_SCHEMA = "tc.research-native-robustness-catalog.v1";\nconst HIGHER_PRECISION_METHOD = "RetestWithHigherPrecision";\n',
    "frontend constants",
)
insert_before = "export async function startHigherPrecision(historicalResult, fetchImpl = globalThis.fetch) {"
frontend_helpers = r'''export function robustnessCapabilitiesFromPayload(payload) {
  if (
    !payload
    || payload.schema !== ROBUSTNESS_CAPABILITIES_SCHEMA
    || payload.sqx_build !== "144.2953"
    || !Array.isArray(payload.methods)
    || payload.methods.length !== 1
  ) {
    throw new Error("Native robustness capability schema is invalid");
  }
  const method = payload.methods[0];
  if (!method || method.method !== HIGHER_PRECISION_METHOD || !["ready", "unavailable"].includes(method.state)) {
    throw new Error("Native robustness capability identity is invalid");
  }
  if (method.state === "ready") {
    if (
      typeof method.detail !== "string" || !method.detail
      || method.reason_code !== null
      || typeof method.configuration_changed !== "boolean"
      || !method.native_settings || typeof method.native_settings !== "object"
      || typeof method.native_settings.Precision !== "string" || !method.native_settings.Precision
      || typeof method.native_settings.Spread !== "string" || !method.native_settings.Spread
      || !digest(method.source_project_sha256)
      || !digest(method.compiled_project_sha256)
      || !digest(method.engine_sha256)
    ) {
      throw new Error("Ready native robustness capability is inconsistent");
    }
  } else if (
    typeof method.reason_code !== "string" || !method.reason_code
    || typeof method.detail !== "string" || !method.detail
    || method.native_settings !== null
    || method.configuration_changed !== null
  ) {
    throw new Error("Unavailable native robustness capability is inconsistent");
  }
  return payload;
}

export function robustnessCatalogFromPayload(payload) {
  if (!payload || payload.schema !== ROBUSTNESS_CATALOG_SCHEMA || !Array.isArray(payload.results)) {
    throw new Error("Native robustness catalog schema is invalid");
  }
  return payload.results.map(robustnessResultFromPayload);
}

export async function fetchRobustnessCapabilities(fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(HISTORICAL_RESULTS_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({ action: "read-robustness-capabilities" }),
  });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Robustness capability read failed");
  return robustnessCapabilitiesFromPayload(payload);
}

export async function fetchRobustnessCatalog(fetchImpl = globalThis.fetch) {
  const response = await fetchImpl(HISTORICAL_RESULTS_API_PATH, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({ action: "list-robustness" }),
  });
  const payload = await readJson(response);
  if (!response?.ok) throw apiError(response, payload, "Robustness catalog read failed");
  return robustnessCatalogFromPayload(payload);
}

'''
text = replace_once(text, insert_before, frontend_helpers + insert_before, "frontend capability helpers")
proof_check = '''  if (
    evidenceDigest(payload.validation_ref) === ""
'''
proof_new = '''  const hasProofEntity = typeof payload.proof_entity_id === "string" && payload.proof_entity_id.length > 0;
  const hasProofRevision = typeof payload.proof_revision === "string" && payload.proof_revision.length > 0;
  if (
    hasProofEntity !== hasProofRevision
    || (hasProofEntity && !/^tc-research:proof:v1:[0-9a-f-]{36}$/.test(payload.proof_entity_id))
    || (hasProofRevision && !/^tc-research-revision:proof:sha256:[0-9a-f]{64}$/.test(payload.proof_revision))
  ) {
    throw new Error("Native robustness proof custody is inconsistent");
  }
  if (
    evidenceDigest(payload.validation_ref) === ""
'''
text = replace_once(text, proof_check, proof_new, "frontend proof identity")

method_start = text.index("function methodRows() {")
method_end = text.index("\n\nlet generation = 0;", method_start)
text = text[:method_start] + r'''function methodRows(capabilities) {
  const nativeLater = [
    ["Additional Markets", "Native cross-market retest — producer path not connected in this slice."],
    ["Monte Carlo · trade manipulation", "Native trade-manipulation family — not executed by TraderCockpit locally."],
    ["Monte Carlo · full retest", "Native full-retest family — producer path not connected in this slice."],
    ["System Parameter Permutation", "Native optimization profile — producer path not connected in this slice."],
    ["Walk-Forward / Matrix", "Native optimization/validation family — producer path not connected in this slice."],
  ];
  const higher = capabilities?.methods?.find((item) => item.method === HIGHER_PRECISION_METHOD) || null;
  const ready = higher?.state === "ready";
  const label = ready ? "Producer capability available" : higher ? "Producer unavailable" : "Checking producer";
  const detail = ready
    ? `Installed SQX owns this profile. Precision ${higher.native_settings.Precision}; Spread ${higher.native_settings.Spread}.`
    : higher?.detail || "Waiting for the backend to inspect the installed SQX Retester project.";
  return `<div class="requirement-list" data-robustness-methods>
    <div class="requirement-item"><div><strong>Higher Precision</strong><span class="status-badge status-${ready ? "ready" : "unavailable"}"><span class="status-dot"></span>${escapeHtml(label)}</span></div><p>${escapeHtml(detail)}</p></div>
    ${nativeLater.map(([name, itemDetail]) => `<div class="requirement-item"><div><strong>${escapeHtml(name)}</strong><span class="status-badge status-unavailable"><span class="status-dot"></span>Not connected</span></div><p>${escapeHtml(itemDetail)}</p></div>`).join("")}
  </div>`;
}''' + text[method_end:]
text = replace_once(
    text,
    'let state = { phase: "idle", results: [], selectedIndex: 0, runtimeReady: false, validation: null, detail: "" };',
    'let state = { phase: "idle", results: [], selectedIndex: 0, runtimeReady: false, capabilities: null, catalog: [], validation: null, detail: "" };',
    "frontend state",
)
text = replace_once(
    text,
    '  const selected = completed[current.selectedIndex] || null;\n  const canRun = current.phase !== "loading" && current.runtimeReady && selected;\n',
    '  const selected = completed[current.selectedIndex] || null;\n  const higherCapability = current.capabilities?.methods?.find((item) => item.method === HIGHER_PRECISION_METHOD) || null;\n  const canRun = current.phase !== "loading" && current.runtimeReady && higherCapability?.state === "ready" && selected;\n',
    "frontend can run",
)
text = replace_once(text, "      ${methodRows()}\n", "      ${methodRows(current.capabilities)}\n", "frontend method rows")
text = replace_once(
    text,
    '      <button class="button button-primary" type="button" data-robustness-action="start" ${canRun ? "" : "disabled"}>${current.runtimeReady ? "Run native Higher Precision" : "Native Retester unavailable"}</button>\n',
    '      <button class="button button-primary" type="button" data-robustness-action="start" ${canRun ? "" : "disabled"}>${canRun ? "Run native Higher Precision" : "Native Higher Precision unavailable"}</button>\n',
    "frontend start button",
)

load_start = text.index("async function load() {")
load_end = text.index("\n\nasync function start(button) {", load_start)
text = text[:load_start] + r'''async function load() {
  const currentGeneration = ++generation;
  const host = panel();
  if (!host) return;
  state = { ...state, phase: "loading", detail: "Loading native robustness custody…" };
  render(host, state);
  try {
    const requestedRef = validationRefFromLocation();
    const [results, runtime, capabilities, catalog] = await Promise.all([
      fetchHistoricalResults(),
      fetchRuntimeStatus(),
      fetchRobustnessCapabilities(),
      fetchRobustnessCatalog(),
    ]);
    let validation = catalog[0] || null;
    let detail = "";
    if (requestedRef) {
      try {
        validation = await fetchRobustnessResult(requestedRef);
      } catch (error) {
        detail = `Saved robustness result unavailable: ${error instanceof Error ? error.message : "readback failed"}`;
      }
    }
    if (currentGeneration !== generation || !robustnessRoute()) return;
    state = {
      phase: "loaded",
      results,
      selectedIndex: 0,
      runtimeReady: retesterRuntimeReady(runtime),
      capabilities,
      catalog,
      validation,
      detail,
    };
  } catch (error) {
    if (currentGeneration !== generation || !robustnessRoute()) return;
    state = { ...state, phase: "failed", detail: error instanceof Error ? error.message : "Native robustness workspace unavailable" };
  }
  render(panel(), state);
}''' + text[load_end:]
text = replace_once(
    text,
    '    state = { ...state, phase: "loaded", validation, detail: "Native Higher Precision result captured. Producer verdict remains unread." };\n',
    '    state = { ...state, phase: "loaded", validation, catalog: [validation, ...state.catalog.filter((item) => item.validation_ref !== validation.validation_ref)], detail: "Native Higher Precision result captured. Producer verdict remains unread." };\n',
    "frontend start catalog update",
)
text = replace_once(
    text,
    '    state = { ...state, selectedIndex, validation: null, detail: "" };\n',
    '    const selected = completed[selectedIndex];\n    const validation = state.catalog.find((item) => item.source_historical_result_revision === selected.revision) || null;\n    state = { ...state, selectedIndex, validation, detail: "" };\n',
    "frontend selection recovery",
)
path.write_text(text, encoding="utf-8")

path = Path("tests/product/test_research_robustness.py")
text = path.read_text(encoding="utf-8")
text = replace_once(text, "from io import BytesIO\n", "from io import BytesIO\nimport json\n", "test json import")
text = replace_once(
    text,
    "from tradercockpit.research_custody import FileResearchCustodyStore\n",
    "from tradercockpit.research_custody import FileResearchCustodyStore, ResearchRevisionRef\n",
    "test custody import",
)
text = replace_once(
    text,
    "    compile_higher_precision_project,\n    read_native_robustness_result,\n    start_native_higher_precision,\n",
    "    compile_higher_precision_project,\n    list_native_robustness_results,\n    read_native_robustness_capabilities,\n    read_native_robustness_result,\n    start_native_higher_precision,\n",
    "test robustness imports",
)
text = replace_once(
    text,
    "from tradercockpit.sqx_outputs import inspect_sqx_output_bytes\n",
    "from tradercockpit.sqx_gateway import SqxNativeGatewayError\nfrom tradercockpit.sqx_outputs import inspect_sqx_output_bytes\n",
    "test gateway import",
)
helper_anchor = "    def _gateway_factory(self, home: Path, result_marker: str | None, *, mutate_engine: bool = False):\n"
helper = '''    def _current_proof_payload(self, store: FileResearchCustodyStore) -> dict[str, object]:
        current = store.base / "current" / "proof"
        pointers = sorted(current.glob("*.json"))
        self.assertEqual(len(pointers), 1)
        pointer = json.loads(pointers[0].read_text(encoding="utf-8"))
        revision = ResearchRevisionRef.parse(pointer["revision"])
        return json.loads(store.read_revision_content(revision))

'''
text = replace_once(text, helper_anchor, helper + helper_anchor, "test proof helper")
text = replace_once(
    text,
    '            self.assertTrue(str(result["validation_ref"]).startswith("tc-evidence:sha256:"))\n\n            reopened = read_native_robustness_result(store, result["validation_ref"])\n            self.assertEqual(reopened, result)\n',
    '            self.assertTrue(str(result["validation_ref"]).startswith("tc-evidence:sha256:"))\n            self.assertTrue(str(result["proof_entity_id"]).startswith("tc-research:proof:v1:"))\n            self.assertTrue(str(result["proof_revision"]).startswith("tc-research-revision:proof:sha256:"))\n\n            catalog = list_native_robustness_results(store)\n            self.assertEqual(catalog["results"], [result])\n            reopened = read_native_robustness_result(store, result["validation_ref"])\n            self.assertEqual(reopened, result)\n',
    "test success catalog assertions",
)
extra_tests = r'''
    def test_capability_read_model_comes_from_current_installed_profile(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            ready_home = self._runtime(root / "ready", self._project_bytes(self._task_xml()))
            ready = read_native_robustness_capabilities(ready_home)
            self.assertEqual(ready["methods"][0]["state"], "ready")
            self.assertEqual(ready["methods"][0]["native_settings"], {"Precision": "2", "Spread": "3"})

            missing_home = self._runtime(root / "missing", self._project_bytes(self._task_xml(include_higher=False)))
            missing = read_native_robustness_capabilities(missing_home)
            self.assertEqual(missing["methods"][0]["state"], "unavailable")
            self.assertEqual(missing["methods"][0]["reason_code"], "robustness_higher_precision_missing")

            conflict_home = self._runtime(root / "conflict", self._project_bytes(self._task_xml(other_use="true")))
            conflict = read_native_robustness_capabilities(conflict_home)
            self.assertEqual(conflict["methods"][0]["state"], "unavailable")
            self.assertEqual(conflict["methods"][0]["reason_code"], "robustness_other_crosscheck_enabled")

    def test_gateway_failure_persists_failed_proof_with_exact_receipt(self) -> None:
        source_result = self._archive_bytes("baseline")
        project = self._project_bytes(self._task_xml())
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", project)
            store = FileResearchCustodyStore(root / "data")
            historical = self._historical(store, source_result)
            outer = self

            class FailingGateway:
                def __init__(self, sqx_home, trusted_launcher_sha256):
                    outer.assertEqual(Path(sqx_home), home)
                    outer.assertEqual(trusted_launcher_sha256, outer.LAUNCHER_SHA)

                def launch_retester_task(self, project_name, *, expected_project_sha256, expected_engine_sha256):
                    raise SqxNativeGatewayError(
                        "sqx_control_timeout",
                        "native control timed out",
                        receipts=[{
                            "action": "startOnlyTask",
                            "project": project_name,
                            "task": 1,
                            "state": "completed",
                            "launcher_sha256": outer.LAUNCHER_SHA,
                            "project_sha256": expected_project_sha256,
                            "engine_sha256": expected_engine_sha256,
                        }],
                    )

            with patch("tradercockpit.research_robustness.read_current_historical_result", return_value=historical):
                with self.assertRaises(ResearchRobustnessError) as caught:
                    start_native_higher_precision(
                        store,
                        home,
                        self.LAUNCHER_SHA,
                        historical_result_entity_id=self.HISTORICAL_ENTITY,
                        expected_historical_result_revision=self.HISTORICAL_REVISION,
                        gateway_factory=FailingGateway,
                    )
            self.assertEqual(caught.exception.code, "sqx_control_timeout")
            failed = self._current_proof_payload(store)
            self.assertEqual(failed["state"], "failed")
            self.assertEqual(failed["failure_reason_code"], "sqx_control_timeout")
            self.assertEqual(failed["partial_side_effect"], True)
            self.assertEqual(failed["receipts"][0]["action"], "startOnlyTask")
            self.assertEqual(list_native_robustness_results(store)["results"], [])

'''
text = replace_once(text, "    def test_invalid_validation_ref_is_typed(self) -> None:\n", extra_tests + "    def test_invalid_validation_ref_is_typed(self) -> None:\n", "extra robustness tests")
text = replace_once(
    text,
    '            self.assertEqual(caught.exception.code, "robustness_engine_changed_during_execution")\n\n',
    '            self.assertEqual(caught.exception.code, "robustness_engine_changed_during_execution")\n            failed = self._current_proof_payload(store)\n            self.assertEqual(failed["state"], "failed")\n            self.assertEqual(failed["failure_reason_code"], "robustness_engine_changed_during_execution")\n            self.assertEqual(failed["partial_side_effect"], True)\n            self.assertEqual(len(failed["receipts"]), 1)\n\n',
    "engine failure custody assertions",
)
path.write_text(text, encoding="utf-8")

path = Path("tests/product/test_research_robustness_http_boundary.py")
text = path.read_text(encoding="utf-8")
http_tests = r'''
    def test_capability_and_catalog_reads_accept_no_browser_injected_settings(self) -> None:
        capabilities = {
            "schema": "tc.research-native-robustness-capabilities.v1",
            "sqx_build": "144.2953",
            "methods": [{
                "method": "RetestWithHigherPrecision",
                "state": "unavailable",
                "reason_code": "runtime_not_configured",
                "detail": "runtime unavailable",
                "native_settings": None,
                "configuration_changed": None,
                "source_project_sha256": None,
                "compiled_project_sha256": None,
                "engine_sha256": None,
            }],
        }
        catalog = {"schema": "tc.research-native-robustness-catalog.v1", "results": []}
        with TemporaryDirectory() as tmp:
            server, thread, store = self._server(Path(tmp))
            endpoint = f"http://127.0.0.1:{server.server_port}/api/research/historical-results"
            try:
                with patch("tradercockpit.research_retester_http.read_native_robustness_capabilities", return_value=capabilities) as capability_reader:
                    status, payload = self._post(endpoint, {"action": "read-robustness-capabilities"})
                    self.assertEqual(status, 200)
                    self.assertEqual(payload, capabilities)
                    capability_reader.assert_called_once_with(None)

                    capability_reader.reset_mock()
                    status, payload = self._post(endpoint, {"action": "read-robustness-capabilities", "Precision": "2"})
                    self.assertEqual(status, 400)
                    self.assertEqual(payload["reason_code"], "robustness_capabilities_invalid")
                    capability_reader.assert_not_called()

                with patch("tradercockpit.research_retester_http.list_native_robustness_results", return_value=catalog) as catalog_reader:
                    status, payload = self._post(endpoint, {"action": "list-robustness"})
                    self.assertEqual(status, 200)
                    self.assertEqual(payload, catalog)
                    catalog_reader.assert_called_once_with(store)

                    catalog_reader.reset_mock()
                    status, payload = self._post(endpoint, {"action": "list-robustness", "latest": True})
                    self.assertEqual(status, 400)
                    self.assertEqual(payload["reason_code"], "robustness_catalog_invalid")
                    catalog_reader.assert_not_called()
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

'''
text = replace_once(text, '\n\nif __name__ == "__main__":\n', "\n" + http_tests + '\nif __name__ == "__main__":\n', "http review tests")
path.write_text(text, encoding="utf-8")

path = Path("tests/research-backtest-robustness.test.mjs")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    "  fetchRobustnessResult,\n  robustnessResultFromPayload,\n  startHigherPrecision,\n",
    "  fetchRobustnessCapabilities,\n  fetchRobustnessCatalog,\n  fetchRobustnessResult,\n  robustnessCapabilitiesFromPayload,\n  robustnessCatalogFromPayload,\n  robustnessResultFromPayload,\n  startHigherPrecision,\n",
    "js test imports",
)
text = replace_once(
    text,
    '    producer_outcome_state: "producer_result_captured_outcome_unread",\n',
    '    producer_outcome_state: "producer_result_captured_outcome_unread",\n    proof_entity_id: "tc-research:proof:v1:33333333-3333-4333-8333-333333333333",\n    proof_revision: `tc-research-revision:proof:sha256:${"a".repeat(64)}`,\n',
    "js robustness proof fields",
)
js_tests = r'''

test("robustness capabilities and catalog are backend read models", async () => {
  const capabilityPayload = {
    schema: "tc.research-native-robustness-capabilities.v1",
    sqx_build: "144.2953",
    methods: [{
      method: "RetestWithHigherPrecision",
      state: "ready",
      reason_code: null,
      detail: "installed producer profile is usable",
      native_settings: { Precision: "4", Spread: "7" },
      configuration_changed: false,
      source_project_sha256: "b".repeat(64),
      compiled_project_sha256: "b".repeat(64),
      engine_sha256: "c".repeat(64),
    }],
  };
  assert.equal(robustnessCapabilitiesFromPayload(capabilityPayload).methods[0].native_settings.Precision, "4");
  assert.throws(
    () => robustnessCapabilitiesFromPayload({ ...capabilityPayload, methods: [{ ...capabilityPayload.methods[0], native_settings: null }] }),
    /inconsistent/,
  );

  let capabilityRequest;
  await fetchRobustnessCapabilities(async (url, options) => {
    capabilityRequest = { url, options };
    return response(capabilityPayload);
  });
  assert.deepEqual(JSON.parse(capabilityRequest.options.body), { action: "read-robustness-capabilities" });

  const catalogPayload = { schema: "tc.research-native-robustness-catalog.v1", results: [robustness()] };
  assert.equal(robustnessCatalogFromPayload(catalogPayload)[0].validation_ref, `tc-evidence:sha256:${validationSha}`);
  let catalogRequest;
  await fetchRobustnessCatalog(async (url, options) => {
    catalogRequest = { url, options };
    return response(catalogPayload);
  });
  assert.deepEqual(JSON.parse(catalogRequest.options.body), { action: "list-robustness" });
});
'''
text = text.rstrip() + js_tests + "\n"
path.write_text(text, encoding="utf-8")

path = Path("tests/run-browser-robustness-regression.mjs")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '  await page.goto(`${baseUrl}/research?stage=backtest&tab=robustness`, { waitUntil: "domcontentloaded" });\n',
    '  const missingValidation = `tc-evidence:sha256:${"f".repeat(64)}`;\n  await page.goto(`${baseUrl}/research?stage=backtest&tab=robustness&validationRef=${missingValidation}`, { waitUntil: "domcontentloaded" });\n',
    "browser missing validation route",
)
text = replace_once(text, "  assert.match(text, /Native execution wired/i);\n", "  assert.match(text, /Producer unavailable/i);\n  assert.doesNotMatch(text, /Native execution wired/i);\n", "browser capability truth")
text = replace_once(text, "  assert.match(text, /No completed Historical Results/i);\n", "  assert.match(text, /No completed Historical Results/i);\n  assert.match(text, /Saved robustness result unavailable/i);\n", "browser receipt recovery")
path.write_text(text, encoding="utf-8")
