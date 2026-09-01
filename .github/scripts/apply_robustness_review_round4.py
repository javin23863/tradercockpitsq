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


# ---------------------------------------------------------------------------
# Native gateway: bind the exact staged baseline archive at launch preflight.
# ---------------------------------------------------------------------------
replace_once(
    "product/tradercockpit/sqx_gateway.py",
    '''class _VerifiedRetesterContext:\n    home: Path\n    launcher: Path\n    launcher_sha256: str\n    project_name: str\n    project_file: Path\n    project_relative_path: str\n    project_sha256: str\n    engine_sha256: str\n''',
    '''class _VerifiedRetesterContext:\n    home: Path\n    launcher: Path\n    launcher_sha256: str\n    project_name: str\n    project_file: Path\n    project_relative_path: str\n    project_sha256: str\n    engine_sha256: str\n    source_result_archive_sha256: str | None\n''',
)
replace_once(
    "product/tradercockpit/sqx_gateway.py",
    '''    def _preflight_retester(\n        self,\n        project_name: str,\n        expected_project_sha256: str | None,\n        expected_engine_sha256: str | None,\n    ) -> _VerifiedRetesterContext:\n''',
    '''    def _preflight_retester(\n        self,\n        project_name: str,\n        expected_project_sha256: str | None,\n        expected_engine_sha256: str | None,\n        expected_result_archive_name: str | None,\n        expected_result_archive_sha256: str | None,\n    ) -> _VerifiedRetesterContext:\n''',
)
replace_once(
    "product/tradercockpit/sqx_gateway.py",
    '''        if observed_engine != expected_engine:\n            raise SqxNativeGatewayError(\n                "retester_engine_hash_mismatch",\n                "installed SQTradingLib.jar changed after execution provenance was captured",\n            )\n\n        return _VerifiedRetesterContext(\n''',
    '''        if observed_engine != expected_engine:\n            raise SqxNativeGatewayError(\n                "retester_engine_hash_mismatch",\n                "installed SQTradingLib.jar changed after execution provenance was captured",\n            )\n\n        observed_result_archive: str | None = None\n        if (expected_result_archive_name is None) != (expected_result_archive_sha256 is None):\n            raise SqxNativeGatewayError(\n                "retester_result_archive_identity_incomplete",\n                "native Retester baseline identity requires both archive name and SHA-256",\n            )\n        if expected_result_archive_name is not None:\n            if (\n                not isinstance(expected_result_archive_name, str)\n                or Path(expected_result_archive_name).name != expected_result_archive_name\n                or not expected_result_archive_name.lower().endswith(".sqx")\n            ):\n                raise SqxNativeGatewayError(\n                    "retester_result_archive_invalid",\n                    "native Retester baseline must be one direct .sqx archive name",\n                )\n            expected_result_archive = _trusted_digest(\n                expected_result_archive_sha256,\n                missing_code="retester_result_archive_identity_not_configured",\n                invalid_code="retester_result_archive_identity_invalid",\n            )\n            results_root, _ = _resolve_inside(\n                launcher.home,\n                project_root / "databanks" / "Results",\n                escape_code="retester_result_archive_path_escape",\n            )\n            archive_file, _ = _resolve_inside(\n                launcher.home,\n                results_root / expected_result_archive_name,\n                escape_code="retester_result_archive_path_escape",\n            )\n            if archive_file.parent != results_root or not archive_file.is_file():\n                raise SqxNativeGatewayError(\n                    "retester_result_archive_missing",\n                    "staged Historical Result baseline archive is missing before native execution",\n                )\n            observed_result_archive = _sha256_file(archive_file)\n            if observed_result_archive != expected_result_archive:\n                raise SqxNativeGatewayError(\n                    "retester_result_archive_hash_mismatch",\n                    "staged Historical Result baseline changed before native execution",\n                )\n\n        return _VerifiedRetesterContext(\n''',
)
replace_once(
    "product/tradercockpit/sqx_gateway.py",
    '''            project_sha256=observed_project,\n            engine_sha256=observed_engine,\n        )\n''',
    '''            project_sha256=observed_project,\n            engine_sha256=observed_engine,\n            source_result_archive_sha256=observed_result_archive,\n        )\n''',
)
replace_once(
    "product/tradercockpit/sqx_gateway.py",
    '''            "engine_sha256": context.engine_sha256 if context else None,\n            "reason_code": reason_code,\n''',
    '''            "engine_sha256": context.engine_sha256 if context else None,\n            "source_result_archive_sha256": context.source_result_archive_sha256 if context else None,\n            "reason_code": reason_code,\n''',
)
replace_once(
    "product/tradercockpit/sqx_gateway.py",
    '''    def launch_retester_task(\n        self,\n        project_name: str,\n        *,\n        expected_project_sha256: str | None,\n        expected_engine_sha256: str | None,\n    ) -> dict[str, object]:\n''',
    '''    def launch_retester_task(\n        self,\n        project_name: str,\n        *,\n        expected_project_sha256: str | None,\n        expected_engine_sha256: str | None,\n        expected_result_archive_name: str | None = None,\n        expected_result_archive_sha256: str | None = None,\n    ) -> dict[str, object]:\n''',
)
replace_once(
    "product/tradercockpit/sqx_gateway.py",
    '''                context = self._preflight_retester(\n                    project_name,\n                    expected_project_sha256,\n                    expected_engine_sha256,\n                )\n''',
    '''                context = self._preflight_retester(\n                    project_name,\n                    expected_project_sha256,\n                    expected_engine_sha256,\n                    expected_result_archive_name,\n                    expected_result_archive_sha256,\n                )\n''',
)
replace_once(
    "product/tradercockpit/sqx_gateway.py",
    '''            "engine_sha256": context.engine_sha256,\n            "control_requests_submitted": 1,\n''',
    '''            "engine_sha256": context.engine_sha256,\n            "source_result_archive_sha256": context.source_result_archive_sha256,\n            "control_requests_submitted": 1,\n''',
)

# ---------------------------------------------------------------------------
# Robustness custody: failed attempts are durable read models; completion I/O
# errors are normalized; exact baseline identity is passed to native preflight.
# ---------------------------------------------------------------------------
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''def _completed_proof_records(store: FileResearchCustodyStore) -> list[dict[str, object]]:\n''',
    '''_PROOF_IDENTITY_KEYS = (\n    "sqx_build", "operation", "method",\n    "source_historical_result_entity_id", "source_historical_result_revision",\n    "source_result_archive_ref", "source_result_archive_sha256",\n    "source_project_ref", "source_project_sha256",\n    "compiled_project_ref", "compiled_project_sha256", "configuration_changed",\n    "source_task_sha256", "compiled_task_sha256", "native_settings",\n    "engine_ref", "engine_sha256", "native_project_name", "native_project_relative_path",\n)\n\n\ndef _prepared_evidence(store: FileResearchCustodyStore, payload: dict[str, object], *, code: str) -> set[EvidenceRef]:\n    pairs = (\n        ("source_result_archive_ref", "source_result_archive_sha256"),\n        ("source_project_ref", "source_project_sha256"),\n        ("compiled_project_ref", "compiled_project_sha256"),\n        ("engine_ref", "engine_sha256"),\n    )\n    evidence: set[EvidenceRef] = set()\n    for ref_key, digest_key in pairs:\n        try:\n            ref = EvidenceRef.parse(payload[ref_key])\n            value = store.read_evidence(ref)\n        except (KeyError, TypeError, ResearchCustodyError) as exc:\n            raise ResearchRobustnessError(code, f"robustness attempt evidence {ref_key} is invalid") from exc\n        digest = _digest(payload.get(digest_key), code)\n        if ref.digest != digest or sha256(value).hexdigest() != digest:\n            raise ResearchRobustnessError(code, f"robustness attempt evidence {ref_key} binding is invalid")\n        evidence.add(ref)\n    return evidence\n\n\ndef _failed_proof_records(store: FileResearchCustodyStore) -> list[dict[str, object]]:\n    results: list[dict[str, object]] = []\n    expected_keys = {\n        "schema", "state", *_PROOF_IDENTITY_KEYS,\n        "launcher_sha256", "receipts", "partial_side_effect", "failure_reason_code",\n    }\n    launched_states = {"completed", "timeout", "rejected", "invalid_receipt"}\n    allowed_states = launched_states | {"preflight_failed", "launch_failed"}\n    for entity in _current_proof_entities(store):\n        revision = store.current(entity)\n        stored = store.read_revision(revision)\n        try:\n            raw = json.loads(store.read_revision_content(revision))\n        except (UnicodeDecodeError, json.JSONDecodeError):\n            continue\n        if not isinstance(raw, dict) or raw.get("schema") != ROBUSTNESS_ATTEMPT_SCHEMA or raw.get("state") != "failed":\n            continue\n        if set(raw) != expected_keys:\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness proof schema is invalid")\n        try:\n            source_entity = ResearchEntityId.parse(raw["source_historical_result_entity_id"])\n            source_revision = ResearchRevisionRef.parse(raw["source_historical_result_revision"])\n        except (TypeError, ResearchCustodyError) as exc:\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness source identity is invalid") from exc\n        if source_entity.kind != ResearchKind.HISTORICAL_RESULT or source_revision.kind != ResearchKind.HISTORICAL_RESULT:\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness source is not Historical Result custody")\n        if raw.get("sqx_build") != SQX_BUILD or raw.get("operation") != ROBUSTNESS_OPERATION or raw.get("method") != ROBUSTNESS_METHOD_HIGHER_PRECISION:\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness producer identity is invalid")\n        if type(raw.get("configuration_changed")) is not bool or type(raw.get("partial_side_effect")) is not bool:\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness boolean state is invalid")\n        if not isinstance(raw.get("failure_reason_code"), str) or not raw["failure_reason_code"]:\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness reason is invalid")\n        if not isinstance(raw.get("native_project_name"), str) or not re.fullmatch(r"TraderCockpit-Retester-[0-9a-f]{32}", raw["native_project_name"]):\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness project identity is invalid")\n        if raw.get("native_project_relative_path") != f'user/projects/{raw["native_project_name"]}/project.cfx':\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness project path is invalid")\n        launcher = raw.get("launcher_sha256")\n        if launcher is not None and (not isinstance(launcher, str) or _DIGEST_RE.fullmatch(launcher) is None):\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness launcher identity is invalid")\n        receipts = raw.get("receipts")\n        if not isinstance(receipts, list) or len(receipts) > 1 or any(not isinstance(item, dict) for item in receipts):\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness receipts are invalid")\n        for receipt in receipts:\n            state = receipt.get("state")\n            if state not in allowed_states or receipt.get("action") != "startOnlyTask" or receipt.get("task") != 1 or receipt.get("project") != raw["native_project_name"]:\n                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness receipt identity is invalid")\n            if receipt.get("launcher_sha256") is not None and receipt.get("launcher_sha256") != launcher:\n                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness receipt launcher is invalid")\n            if receipt.get("project_sha256") is not None and receipt.get("project_sha256") != raw["compiled_project_sha256"]:\n                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness receipt project is invalid")\n            if receipt.get("engine_sha256") is not None and receipt.get("engine_sha256") != raw["engine_sha256"]:\n                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness receipt engine is invalid")\n            if receipt.get("source_result_archive_sha256") is not None and receipt.get("source_result_archive_sha256") != raw["source_result_archive_sha256"]:\n                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness receipt baseline is invalid")\n        if any(item.get("state") in launched_states for item in receipts) and raw["partial_side_effect"] is not True:\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "launched failed robustness receipt lost partial-side-effect truth")\n        if stored.parent_revision is None:\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness proof has no prepared parent")\n        parent_revision = store.read_revision(stored.parent_revision)\n        try:\n            prepared = json.loads(store.read_revision_content(stored.parent_revision))\n        except (UnicodeDecodeError, json.JSONDecodeError) as exc:\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "prepared robustness proof is unreadable") from exc\n        if not isinstance(prepared, dict) or prepared.get("schema") != ROBUSTNESS_ATTEMPT_SCHEMA or prepared.get("state") != "prepared":\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness proof parent is not prepared")\n        if any(prepared.get(key) != raw.get(key) for key in _PROOF_IDENTITY_KEYS):\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness proof does not match its prepared identity")\n        evidence = _prepared_evidence(store, raw, code="robustness_proof_catalog_corrupt")\n        if set(parent_revision.evidence) != evidence or set(stored.evidence) != evidence:\n            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness proof evidence set is invalid")\n        results.append({\n            **raw,\n            "attempt_ref": str(stored.content),\n            "proof_entity_id": str(entity),\n            "proof_revision": str(revision),\n        })\n    return results\n\n\ndef _completed_proof_records(store: FileResearchCustodyStore) -> list[dict[str, object]]:\n''',
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''        identity_keys = (\n            "sqx_build", "operation", "method",\n            "source_historical_result_entity_id", "source_historical_result_revision",\n            "source_result_archive_ref", "source_result_archive_sha256",\n            "source_project_ref", "source_project_sha256",\n            "compiled_project_ref", "compiled_project_sha256", "configuration_changed",\n            "source_task_sha256", "compiled_task_sha256", "native_settings",\n            "engine_ref", "engine_sha256", "native_project_name", "native_project_relative_path",\n        )\n        if any(prepared.get(key) != record.get(key) for key in identity_keys):\n''',
    '''        if any(prepared.get(key) != record.get(key) for key in _PROOF_IDENTITY_KEYS):\n''',
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''    return {\n        "schema": ROBUSTNESS_CATALOG_SCHEMA,\n        "results": _completed_proof_records(store),\n    }\n''',
    '''    return {\n        "schema": ROBUSTNESS_CATALOG_SCHEMA,\n        "results": _completed_proof_records(store),\n        "attempts": _failed_proof_records(store),\n    }\n''',
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''def start_native_higher_precision(\n''',
    '''def read_native_robustness_attempt(\n    store: FileResearchCustodyStore,\n    attempt_ref: str | EvidenceRef,\n) -> dict[str, object]:\n    """Reopen one exact current failed native robustness attempt."""\n\n    try:\n        ref = attempt_ref if isinstance(attempt_ref, EvidenceRef) else EvidenceRef.parse(attempt_ref)\n    except (ResearchCustodyError, TypeError) as exc:\n        raise ResearchRobustnessError("robustness_attempt_ref_invalid", "attempt_ref is not a valid evidence identity") from exc\n    matches = [item for item in _failed_proof_records(store) if item.get("attempt_ref") == str(ref)]\n    if not matches:\n        raise ResearchRobustnessError(\n            "robustness_attempt_required",\n            "attempt_ref is not registered as the current failed content of a Research proof",\n        )\n    if len(matches) > 1:\n        raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "multiple current failed robustness proofs reference one attempt")\n    return matches[0]\n\n\ndef start_native_higher_precision(\n''',
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''        receipt = gateway_factory(sqx_home, trusted_launcher_sha256).launch_retester_task(\n            project_name,\n            expected_project_sha256=compiled_project_sha,\n            expected_engine_sha256=engine_sha,\n        )\n''',
    '''        receipt = gateway_factory(sqx_home, trusted_launcher_sha256).launch_retester_task(\n            project_name,\n            expected_project_sha256=compiled_project_sha,\n            expected_engine_sha256=engine_sha,\n            expected_result_archive_name=historical["result_archive_name"],\n            expected_result_archive_sha256=source_result_sha,\n        )\n''',
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''        and receipt_items[0].get("engine_sha256") == engine_sha\n    )\n''',
    '''        and receipt_items[0].get("engine_sha256") == engine_sha\n        and receipt.get("source_result_archive_sha256") == source_result_sha\n        and receipt_items[0].get("source_result_archive_sha256") == source_result_sha\n    )\n''',
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''        or receipt.get("engine_sha256") != payload.get("engine_sha256")\n    ):\n''',
    '''        or receipt.get("engine_sha256") != payload.get("engine_sha256")\n        or receipt.get("source_result_archive_sha256") != payload.get("source_result_archive_sha256")\n    ):\n''',
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''    except ResearchCustodyError as exc:\n        try:\n            _failed_successor(\n''',
    '''    except (ResearchCustodyError, OSError) as exc:\n        try:\n            _failed_successor(\n''',
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''        except ResearchCustodyError as failure_exc:\n            raise ResearchRobustnessError(\n                "robustness_completion_custody_failed",\n                "native execution completed, but result custody and failed-state custody could not be persisted",\n            ) from failure_exc\n        raise ResearchRobustnessError("robustness_completion_custody_failed", exc.detail) from exc\n''',
    '''        except (ResearchCustodyError, OSError) as failure_exc:\n            raise ResearchRobustnessError(\n                "robustness_completion_custody_failed",\n                "native execution completed, but result custody and failed-state custody could not be persisted",\n            ) from failure_exc\n        detail = exc.detail if isinstance(exc, ResearchCustodyError) else "native robustness completion custody filesystem write failed"\n        raise ResearchRobustnessError("robustness_completion_custody_failed", detail) from exc\n''',
)
replace_once(
    "product/tradercockpit/research_robustness.py",
    '''        except ResearchCustodyError as failure_exc:\n            raise ResearchRobustnessError(\n                "robustness_completion_custody_failed",\n                "native execution completed, but result validation and failed-state custody could not be persisted",\n            ) from failure_exc\n''',
    '''        except (ResearchCustodyError, OSError) as failure_exc:\n            raise ResearchRobustnessError(\n                "robustness_completion_custody_failed",\n                "native execution completed, but result validation and failed-state custody could not be persisted",\n            ) from failure_exc\n''',
)

# ---------------------------------------------------------------------------
# HTTP boundary: expose and verify failed attempts; bind completed receipt to
# the staged baseline identity.
# ---------------------------------------------------------------------------
replace_once(
    "product/tradercockpit/research_retester_http.py",
    '''    read_native_robustness_capabilities,\n    read_native_robustness_result,\n''',
    '''    read_native_robustness_attempt,\n    read_native_robustness_capabilities,\n    read_native_robustness_result,\n''',
)
replace_once(
    "product/tradercockpit/research_retester_http.py",
    '''    not_found = {"robustness_record_ref_invalid", "robustness_proof_required"}\n''',
    '''    not_found = {\n        "robustness_record_ref_invalid", "robustness_proof_required",\n        "robustness_attempt_ref_invalid", "robustness_attempt_required",\n    }\n''',
)
replace_once(
    "product/tradercockpit/research_retester_http.py",
    '''        or receipt.get("launcher_sha256") != record.get("launcher_sha256")\n    ):\n''',
    '''        or receipt.get("launcher_sha256") != record.get("launcher_sha256")\n        or receipt.get("source_result_archive_sha256") != record.get("source_result_archive_sha256")\n    ):\n''',
)
replace_once(
    "product/tradercockpit/research_retester_http.py",
    '''    return record\n\n\ndef historical_result_write_response(\n''',
    '''    return record\n\n\ndef _verified_robustness_public_attempt(record: dict[str, object]) -> dict[str, object]:\n    proof_entity_id = record.get("proof_entity_id")\n    proof_revision = record.get("proof_revision")\n    receipts = record.get("receipts")\n    if (\n        record.get("schema") != "tc.research-native-robustness-attempt.v1"\n        or record.get("state") != "failed"\n        or record.get("operation") != ROBUSTNESS_OPERATION\n        or record.get("method") != ROBUSTNESS_METHOD_HIGHER_PRECISION\n        or not isinstance(record.get("attempt_ref"), str)\n        or not record["attempt_ref"].startswith("tc-evidence:sha256:")\n        or not isinstance(proof_entity_id, str)\n        or not proof_entity_id.startswith("tc-research:proof:v1:")\n        or not isinstance(proof_revision, str)\n        or not proof_revision.startswith("tc-research-revision:proof:sha256:")\n        or type(record.get("partial_side_effect")) is not bool\n        or not isinstance(record.get("failure_reason_code"), str)\n        or not record["failure_reason_code"]\n        or not isinstance(receipts, list)\n    ):\n        raise ResearchRobustnessError("robustness_record_corrupt", "failed robustness attempt public custody is invalid")\n    for receipt in receipts:\n        if not isinstance(receipt, dict):\n            raise ResearchRobustnessError("robustness_record_corrupt", "failed robustness attempt receipt is invalid")\n        if receipt.get("project") != record.get("native_project_name"):\n            raise ResearchRobustnessError("robustness_record_corrupt", "failed robustness attempt receipt project is invalid")\n        if receipt.get("project_sha256") is not None and receipt.get("project_sha256") != record.get("compiled_project_sha256"):\n            raise ResearchRobustnessError("robustness_record_corrupt", "failed robustness attempt receipt project hash is invalid")\n        if receipt.get("engine_sha256") is not None and receipt.get("engine_sha256") != record.get("engine_sha256"):\n            raise ResearchRobustnessError("robustness_record_corrupt", "failed robustness attempt receipt engine hash is invalid")\n        if receipt.get("source_result_archive_sha256") is not None and receipt.get("source_result_archive_sha256") != record.get("source_result_archive_sha256"):\n            raise ResearchRobustnessError("robustness_record_corrupt", "failed robustness attempt receipt baseline hash is invalid")\n    return record\n\n\ndef historical_result_write_response(\n''',
)
replace_once(
    "product/tradercockpit/research_retester_http.py",
    '''            catalog = list_native_robustness_results(research_store)\n            catalog["results"] = [_verified_robustness_public_record(item) for item in catalog["results"]]\n            return 200, catalog\n''',
    '''            catalog = list_native_robustness_results(research_store)\n            catalog["results"] = [_verified_robustness_public_record(item) for item in catalog["results"]]\n            catalog["attempts"] = [_verified_robustness_public_attempt(item) for item in catalog["attempts"]]\n            return 200, catalog\n''',
)
replace_once(
    "product/tradercockpit/research_retester_http.py",
    '''    if action == "read-robustness":\n''',
    '''    if action == "read-robustness-attempt":\n        if set(payload) != {"action", "attempt_ref"}:\n            return 400, {\n                "error": "invalid_request",\n                "reason_code": "robustness_attempt_read_invalid",\n                "detail": "Failed robustness attempt read requires only action=read-robustness-attempt and attempt_ref.",\n            }\n        attempt_ref = payload.get("attempt_ref")\n        if not isinstance(attempt_ref, str) or not attempt_ref:\n            return 400, {\n                "error": "invalid_request",\n                "reason_code": "robustness_attempt_ref_invalid",\n                "detail": "attempt_ref must be a non-empty evidence reference.",\n            }\n        try:\n            return 200, _verified_robustness_public_attempt(read_native_robustness_attempt(research_store, attempt_ref))\n        except ResearchRobustnessError as exc:\n            return _robustness_error_response(exc)\n        except ResearchCustodyError as exc:\n            status = 404 if exc.code in {"evidence_missing", "current_pointer_missing"} else 409\n            return status, {\n                "error": "not_found" if status == 404 else "invalid_state",\n                "reason_code": exc.code,\n                "detail": exc.detail,\n            }\n\n    if action == "read-robustness":\n''',
)

# ---------------------------------------------------------------------------
# Browser: failed-attempt parser/panel, exact bookmark failure behavior, and a
# global running phase that freezes source/receipt selection until reconciliation.
# ---------------------------------------------------------------------------
replace_once(
    "web/research-backtest-robustness.mjs",
    '''const ROBUSTNESS_SCHEMA = "tc.research-native-robustness.v1";\n''',
    '''const ROBUSTNESS_SCHEMA = "tc.research-native-robustness.v1";\nconst ROBUSTNESS_ATTEMPT_SCHEMA = "tc.research-native-robustness-attempt.v1";\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''    || payload.receipts[0]?.launcher_sha256 !== payload.launcher_sha256\n''',
    '''    || payload.receipts[0]?.launcher_sha256 !== payload.launcher_sha256\n    || payload.receipts[0]?.source_result_archive_sha256 !== payload.source_result_archive_sha256\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''export function robustnessCatalogFromPayload(payload) {\n    if (!payload || payload.schema !== ROBUSTNESS_CATALOG_SCHEMA || !Array.isArray(payload.results)) {\n        throw new Error("Native robustness catalog schema is invalid");\n    }\n    return payload.results.map(robustnessResultFromPayload);\n}\n''',
    '''export function robustnessAttemptFromPayload(payload) {\n  const requiredStrings = [\n    "attempt_ref", "proof_entity_id", "proof_revision",\n    "source_historical_result_entity_id", "source_historical_result_revision",\n    "source_result_archive_ref", "source_result_archive_sha256",\n    "source_project_ref", "source_project_sha256",\n    "compiled_project_ref", "compiled_project_sha256",\n    "source_task_sha256", "compiled_task_sha256",\n    "engine_ref", "engine_sha256", "native_project_name", "native_project_relative_path",\n    "failure_reason_code",\n  ];\n  if (\n    !payload\n    || payload.schema !== ROBUSTNESS_ATTEMPT_SCHEMA\n    || payload.state !== "failed"\n    || payload.sqx_build !== "144.2953"\n    || payload.operation !== "native_retester_cross_check"\n    || payload.method !== HIGHER_PRECISION_METHOD\n    || typeof payload.configuration_changed !== "boolean"\n    || typeof payload.partial_side_effect !== "boolean"\n    || requiredStrings.some((key) => typeof payload[key] !== "string" || !payload[key])\n    || !Array.isArray(payload.receipts)\n    || payload.receipts.length > 1\n  ) {\n    throw new Error("Native robustness failed-attempt identity is invalid");\n  }\n  const evidencePairs = [\n    ["source_result_archive_ref", "source_result_archive_sha256"],\n    ["source_project_ref", "source_project_sha256"],\n    ["compiled_project_ref", "compiled_project_sha256"],\n    ["engine_ref", "engine_sha256"],\n  ];\n  if (\n    !/^tc-evidence:sha256:[0-9a-f]{64}$/.test(payload.attempt_ref)\n    || !/^tc-research:proof:v1:[0-9a-f-]{36}$/.test(payload.proof_entity_id)\n    || !/^tc-research-revision:proof:sha256:[0-9a-f]{64}$/.test(payload.proof_revision)\n    || !/^tc-research:historical-result:v1:[0-9a-f-]{36}$/.test(payload.source_historical_result_entity_id)\n    || !/^tc-research-revision:historical-result:sha256:[0-9a-f]{64}$/.test(payload.source_historical_result_revision)\n    || !/^TraderCockpit-Retester-[0-9a-f]{32}$/.test(payload.native_project_name)\n    || payload.native_project_relative_path !== `user/projects/${payload.native_project_name}/project.cfx`\n    || evidencePairs.some(([refKey, digestKey]) => !digest(payload[digestKey]) || evidenceDigest(payload[refKey]) !== payload[digestKey])\n    || (payload.launcher_sha256 !== null && !digest(payload.launcher_sha256))\n  ) {\n    throw new Error("Native robustness failed-attempt custody is inconsistent");\n  }\n  const launchedStates = new Set(["completed", "timeout", "rejected", "invalid_receipt"]);\n  for (const receipt of payload.receipts) {\n    if (\n      !receipt || typeof receipt !== "object"\n      || receipt.action !== "startOnlyTask"\n      || receipt.task !== 1\n      || !new Set(["completed", "timeout", "rejected", "invalid_receipt", "preflight_failed", "launch_failed"]).has(receipt.state)\n      || receipt.project !== payload.native_project_name\n      || (receipt.launcher_sha256 !== null && receipt.launcher_sha256 !== payload.launcher_sha256)\n      || (receipt.project_sha256 !== null && receipt.project_sha256 !== payload.compiled_project_sha256)\n      || (receipt.engine_sha256 !== null && receipt.engine_sha256 !== payload.engine_sha256)\n      || (receipt.source_result_archive_sha256 !== null && receipt.source_result_archive_sha256 !== payload.source_result_archive_sha256)\n    ) {\n      throw new Error("Native robustness failed-attempt receipt is inconsistent");\n    }\n    if (launchedStates.has(receipt.state) && payload.partial_side_effect !== true) {\n      throw new Error("Native robustness failed-attempt side-effect truth is inconsistent");\n    }\n  }\n  return payload;\n}\n\nexport function robustnessCatalogFromPayload(payload) {\n  if (\n    !payload || payload.schema !== ROBUSTNESS_CATALOG_SCHEMA\n    || !Array.isArray(payload.results) || !Array.isArray(payload.attempts)\n  ) {\n    throw new Error("Native robustness catalog schema is invalid");\n  }\n  return {\n    results: payload.results.map(robustnessResultFromPayload),\n    attempts: payload.attempts.map(robustnessAttemptFromPayload),\n  };\n}\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''export function robustnessResultsForHistorical(catalog, historicalResult) {\n  if (!Array.isArray(catalog)) throw new Error("Native robustness catalog is invalid");\n  const source = historicalResultFromPayload(historicalResult);\n  return catalog.filter((item) => (\n''',
    '''export function robustnessResultsForHistorical(catalog, historicalResult) {\n  const results = Array.isArray(catalog) ? catalog : catalog?.results;\n  if (!Array.isArray(results)) throw new Error("Native robustness catalog is invalid");\n  const source = historicalResultFromPayload(historicalResult);\n  return results.filter((item) => (\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''export async function fetchRobustnessCatalog(fetchImpl = globalThis.fetch) {\n''',
    '''export async function fetchRobustnessAttempt(attemptRef, fetchImpl = globalThis.fetch) {\n  if (!/^tc-evidence:sha256:[0-9a-f]{64}$/.test(attemptRef || "")) {\n    throw new Error("Robustness attempt reference is invalid");\n  }\n  const response = await fetchImpl(HISTORICAL_RESULTS_API_PATH, {\n    method: "POST",\n    headers: { accept: "application/json", "content-type": "application/json" },\n    body: JSON.stringify({ action: "read-robustness-attempt", attempt_ref: attemptRef }),\n  });\n  const payload = await readJson(response);\n  if (!response?.ok) throw apiError(response, payload, "Robustness failed-attempt read failed");\n  return robustnessAttemptFromPayload(payload);\n}\n\nexport async function fetchRobustnessCatalog(fetchImpl = globalThis.fetch) {\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''function resultPanel(result) {\n''',
    '''function failedAttemptsPanel(attempts) {\n  if (!Array.isArray(attempts) || attempts.length === 0) return "";\n  return `<div data-robustness-failed-attempts><p class="eyebrow">Failed native attempts</p>${attempts.map((attempt) => `\n    <div class="requirement-item" data-robustness-attempt="${escapeHtml(attempt.attempt_ref)}"><div><strong>${escapeHtml(attempt.failure_reason_code)}</strong><span class="status-badge status-unavailable"><span class="status-dot"></span>Failed attempt</span></div><p>Source ${escapeHtml(short(attempt.source_historical_result_revision))} · Proof ${escapeHtml(short(attempt.proof_revision))} · Partial side effect: ${attempt.partial_side_effect ? "possible" : "not observed before failure"}</p></div>`).join("")}</div>`;\n}\n\nfunction resultPanel(result) {\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''let state = { phase: "idle", results: [], selectedIndex: 0, runtimeReady: false, capabilities: null, catalog: [], validation: null, detail: "" };\n''',
    '''let state = { phase: "idle", results: [], selectedIndex: 0, runtimeReady: false, capabilities: null, catalog: { results: [], attempts: [] }, validation: null, detail: "" };\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''  const canRun = current.phase !== "loading" && current.runtimeReady && higherCapability?.state === "ready" && selected;\n''',
    '''  const canRun = current.phase === "loaded" && current.runtimeReady && higherCapability?.state === "ready" && selected;\n  const running = current.phase === "running";\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''  const validationPicker = matchingValidations.length\n    ? `<label class="field-label" for="robustness-validation-result">Captured robustness run</label><select id="robustness-validation-result" class="idea-editor">${matchingValidations.length > 1 && !current.validation ? '<option value="" selected>Choose exact robustness run</option>' : ""}${matchingValidations.map((item) => `<option value="${escapeHtml(item.validation_ref)}" ${current.validation?.validation_ref === item.validation_ref ? "selected" : ""}>${escapeHtml(short(item.validation_ref))} · ${escapeHtml(short(item.proof_revision))}</option>`).join("")}</select>`\n''',
    '''  const validationPicker = matchingValidations.length\n    ? `<label class="field-label" for="robustness-validation-result">Captured robustness run</label><select id="robustness-validation-result" class="idea-editor" ${running ? "disabled" : ""}>${matchingValidations.length > 1 && !current.validation ? '<option value="" selected>Choose exact robustness run</option>' : ""}${matchingValidations.map((item) => `<option value="${escapeHtml(item.validation_ref)}" ${current.validation?.validation_ref === item.validation_ref ? "selected" : ""}>${escapeHtml(short(item.validation_ref))} · ${escapeHtml(short(item.proof_revision))}</option>`).join("")}</select>`\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''      <select id="robustness-source-result" class="idea-editor" ${completed.length ? "" : "disabled"}>${completed.length ? completed.map((item, index) => `<option value="${index}" ${index === current.selectedIndex ? "selected" : ""}>${escapeHtml(item.result_archive_name)} · ${escapeHtml(short(item.revision))}</option>`).join("") : '<option>No completed Historical Results</option>'}</select>\n''',
    '''      <select id="robustness-source-result" class="idea-editor" ${completed.length && !running ? "" : "disabled"}>${completed.length ? completed.map((item, index) => `<option value="${index}" ${index === current.selectedIndex ? "selected" : ""}>${escapeHtml(item.result_archive_name)} · ${escapeHtml(short(item.revision))}</option>`).join("") : '<option>No completed Historical Results</option>'}</select>\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''      <button class="button button-primary" type="button" data-robustness-action="start" ${canRun ? "" : "disabled"}>${canRun ? "Run native Higher Precision" : "Native Higher Precision unavailable"}</button>\n''',
    '''      <button class="button button-primary" type="button" data-robustness-action="start" ${canRun ? "" : "disabled"}>${running ? "Running Higher Precision in SQX…" : canRun ? "Run native Higher Precision" : "Native Higher Precision unavailable"}</button>\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''    <section class="panel" data-accent="purple"><div class="panel-heading"><div><p class="eyebrow">Immutable readback</p><h2>Robustness result custody</h2></div></div>${validationPicker}${resultPanel(current.validation)}</section>\n''',
    '''    <section class="panel" data-accent="purple"><div class="panel-heading"><div><p class="eyebrow">Immutable readback</p><h2>Robustness result custody</h2></div></div>${validationPicker}${resultPanel(current.validation)}${failedAttemptsPanel(current.catalog?.attempts)}</section>\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''      } catch (error) {\n        detail = `Saved robustness result unavailable: ${error instanceof Error ? error.message : "readback failed"}`;\n      }\n''',
    '''      } catch (error) {\n        validation = null;\n        clearValidationRef();\n        detail = `Saved robustness result unavailable: ${error instanceof Error ? error.message : "readback failed"}`;\n      }\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''async function start(button) {\n  const higherCapability = state.capabilities?.methods?.find((item) => item.method === HIGHER_PRECISION_METHOD) || null;\n  if (state.phase === "loading" || !state.runtimeReady || higherCapability?.state !== "ready") return;\n  const completed = state.results.filter((item) => item.state === "completed" && item.execution_completed === true);\n  const selected = completed[state.selectedIndex];\n  if (!selected) return;\n  button.disabled = true;\n  button.textContent = "Running Higher Precision in SQX…";\n  try {\n    const validation = await startHigherPrecision(selected);\n    if (!robustnessRoute()) return;\n    persistValidationRef(validation.validation_ref);\n    state = { ...state, phase: "loaded", validation, catalog: [validation, ...state.catalog.filter((item) => item.validation_ref !== validation.validation_ref)], detail: "Native Higher Precision result captured. Producer verdict remains unread." };\n  } catch (error) {\n    state = { ...state, phase: "failed", detail: error instanceof Error ? error.message : "Native Higher Precision execution failed" };\n  }\n  render(panel(), state);\n}\n''',
    '''async function start(button) {\n  const higherCapability = state.capabilities?.methods?.find((item) => item.method === HIGHER_PRECISION_METHOD) || null;\n  if (state.phase !== "loaded" || !state.runtimeReady || higherCapability?.state !== "ready") return;\n  const completed = state.results.filter((item) => item.state === "completed" && item.execution_completed === true);\n  const selectedIndex = state.selectedIndex;\n  const selected = completed[selectedIndex];\n  if (!selected) return;\n  const sourceEntity = selected.entity_id;\n  const sourceRevision = selected.revision;\n  state = { ...state, phase: "running", detail: "Running Higher Precision in SQX…" };\n  render(panel(), state);\n  try {\n    const validation = await startHigherPrecision(selected);\n    if (!robustnessRoute()) return;\n    if (\n      validation.source_historical_result_entity_id !== sourceEntity\n      || validation.source_historical_result_revision !== sourceRevision\n    ) {\n      throw new Error("Native Higher Precision response changed the in-flight Historical Result identity");\n    }\n    persistValidationRef(validation.validation_ref);\n    state = {\n      ...state,\n      phase: "loaded",\n      selectedIndex,\n      validation,\n      catalog: {\n        ...state.catalog,\n        results: [validation, ...state.catalog.results.filter((item) => item.validation_ref !== validation.validation_ref)],\n      },\n      detail: "Native Higher Precision result captured. Producer verdict remains unread.",\n    };\n  } catch (error) {\n    let catalog = state.catalog;\n    try { catalog = await fetchRobustnessCatalog(); } catch {}\n    if (!robustnessRoute()) return;\n    state = {\n      ...state,\n      phase: "loaded",\n      selectedIndex,\n      catalog,\n      validation: robustnessResultForHistorical(catalog, selected),\n      detail: error instanceof Error ? error.message : "Native Higher Precision execution failed",\n    };\n  }\n  render(panel(), state);\n}\n''',
)
replace_once(
    "web/research-backtest-robustness.mjs",
    '''  document.addEventListener("change", (event) => {\n    if (!robustnessRoute()) return;\n''',
    '''  document.addEventListener("change", (event) => {\n    if (!robustnessRoute() || state.phase === "running") return;\n''',
)

# ---------------------------------------------------------------------------
# Python regressions: gateway baseline pin, real OSError completion failure,
# durable failed-attempt catalog/exact read.
# ---------------------------------------------------------------------------
replace_once(
    "tests/product/test_research_robustness.py",
    '''    list_native_robustness_results,\n    read_native_robustness_capabilities,\n''',
    '''    list_native_robustness_results,\n    read_native_robustness_attempt,\n    read_native_robustness_capabilities,\n''',
)
replace_once(
    "tests/product/test_research_robustness.py",
    '''            def launch_retester_task(self, project_name, *, expected_project_sha256, expected_engine_sha256):\n''',
    '''            def launch_retester_task(\n                self, project_name, *, expected_project_sha256, expected_engine_sha256,\n                expected_result_archive_name=None, expected_result_archive_sha256=None,\n            ):\n''',
)
replace_once(
    "tests/product/test_research_robustness.py",
    '''                engine = self.home / "internal/libs/SQTradingLib.jar"\n                outer.assertEqual(sha256(engine.read_bytes()).hexdigest(), expected_engine_sha256)\n\n                with ZipFile(project) as archive:\n''',
    '''                engine = self.home / "internal/libs/SQTradingLib.jar"\n                outer.assertEqual(sha256(engine.read_bytes()).hexdigest(), expected_engine_sha256)\n                baseline = self.home / "user/projects" / project_name / "databanks/Results" / expected_result_archive_name\n                outer.assertEqual(sha256(baseline.read_bytes()).hexdigest(), expected_result_archive_sha256)\n\n                with ZipFile(project) as archive:\n''',
)
replace_once(
    "tests/product/test_research_robustness.py",
    '''                    "engine_sha256": expected_engine_sha256,\n                    "control_requests_submitted": 1,\n''',
    '''                    "engine_sha256": expected_engine_sha256,\n                    "source_result_archive_sha256": expected_result_archive_sha256,\n                    "control_requests_submitted": 1,\n''',
)
replace_once(
    "tests/product/test_research_robustness.py",
    '''                        "engine_sha256": expected_engine_sha256,\n                        "reason_code": None,\n''',
    '''                        "engine_sha256": expected_engine_sha256,\n                        "source_result_archive_sha256": expected_result_archive_sha256,\n                        "reason_code": None,\n''',
)
replace_once(
    "tests/product/test_research_robustness.py",
    '''                def launch_retester_task(self, project_name, *, expected_project_sha256, expected_engine_sha256):\n                    raise SqxNativeGatewayError(\n''',
    '''                def launch_retester_task(\n                    self, project_name, *, expected_project_sha256, expected_engine_sha256,\n                    expected_result_archive_name=None, expected_result_archive_sha256=None,\n                ):\n                    raise SqxNativeGatewayError(\n''',
)
replace_once(
    "tests/product/test_research_robustness.py",
    '''                            "engine_sha256": expected_engine_sha256,\n                        }],\n''',
    '''                            "engine_sha256": expected_engine_sha256,\n                            "source_result_archive_sha256": expected_result_archive_sha256,\n                        }],\n''',
)
replace_once(
    "tests/product/test_research_robustness.py",
    '''            self.assertEqual(failed["receipts"][0]["action"], "startOnlyTask")\n            self.assertEqual(list_native_robustness_results(store)["results"], [])\n''',
    '''            self.assertEqual(failed["receipts"][0]["action"], "startOnlyTask")\n            catalog = list_native_robustness_results(store)\n            self.assertEqual(catalog["results"], [])\n            self.assertEqual(len(catalog["attempts"]), 1)\n            attempt = catalog["attempts"][0]\n            self.assertEqual(attempt["state"], "failed")\n            self.assertTrue(attempt["partial_side_effect"])\n            self.assertEqual(attempt["failure_reason_code"], "sqx_control_timeout")\n            self.assertEqual(read_native_robustness_attempt(store, attempt["attempt_ref"]), attempt)\n''',
)
replace_once(
    "tests/product/test_research_robustness.py",
    '''    def test_detached_validation_evidence_is_not_reopened_without_current_proof(self) -> None:\n''',
    '''    def test_real_filesystem_error_after_native_execution_persists_failed_proof(self) -> None:\n        source_result = self._archive_bytes("baseline")\n        result_bytes = self._archive_bytes("higher-precision")\n        project = self._project_bytes(self._task_xml())\n        with TemporaryDirectory() as tmp:\n            root = Path(tmp)\n            home = self._runtime(root / "sqx", project)\n            store = FileResearchCustodyStore(root / "data")\n            historical = self._historical(store, source_result)\n            original_put = store.put_evidence\n            injected = False\n\n            def flaky_put(value: bytes):\n                nonlocal injected\n                if value == result_bytes and not injected:\n                    injected = True\n                    raise PermissionError("simulated filesystem permission failure")\n                return original_put(value)\n\n            with patch("tradercockpit.research_robustness.read_current_historical_result", return_value=historical):\n                with patch.object(store, "put_evidence", side_effect=flaky_put):\n                    with self.assertRaises(ResearchRobustnessError) as caught:\n                        start_native_higher_precision(\n                            store, home, self.LAUNCHER_SHA,\n                            historical_result_entity_id=self.HISTORICAL_ENTITY,\n                            expected_historical_result_revision=self.HISTORICAL_REVISION,\n                            gateway_factory=self._gateway_factory(home, "higher-precision"),\n                        )\n            self.assertEqual(caught.exception.code, "robustness_completion_custody_failed")\n            failed = self._current_proof_payload(store)\n            self.assertEqual(failed["state"], "failed")\n            self.assertTrue(failed["partial_side_effect"])\n            self.assertEqual(failed["receipts"][0]["state"], "completed")\n            self.assertEqual(len(list_native_robustness_results(store)["attempts"]), 1)\n\n    def test_detached_validation_evidence_is_not_reopened_without_current_proof(self) -> None:\n''',
)

# Dedicated gateway baseline-race regression.
replace_once(
    "tests/product/test_sqx_retester_gateway.py",
    '''    def test_nonzero_exit_preserves_exact_retester_receipt(self) -> None:\n''',
    '''    def test_staged_baseline_change_is_refused_before_runner(self) -> None:\n        with TemporaryDirectory() as tmp:\n            home, project_name, launcher_hash, project_hash, engine_hash = self._runtime(Path(tmp))\n            results = home / "user/projects" / project_name / "databanks/Results"\n            results.mkdir(parents=True)\n            baseline = results / "Baseline.sqx"\n            baseline.write_bytes(b"original baseline")\n            expected_baseline = sha256(baseline.read_bytes()).hexdigest()\n            baseline.write_bytes(b"changed baseline")\n            calls = 0\n\n            def runner(*args, **kwargs):\n                nonlocal calls\n                calls += 1\n                return subprocess.CompletedProcess(args, 0)\n\n            with self.assertRaises(SqxNativeGatewayError) as caught:\n                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_retester_task(\n                    project_name,\n                    expected_project_sha256=project_hash,\n                    expected_engine_sha256=engine_hash,\n                    expected_result_archive_name="Baseline.sqx",\n                    expected_result_archive_sha256=expected_baseline,\n                )\n        self.assertEqual(caught.exception.code, "retester_result_archive_hash_mismatch")\n        self.assertEqual(calls, 0)\n\n    def test_nonzero_exit_preserves_exact_retester_receipt(self) -> None:\n''',
)

# HTTP fixture/catalog/exact failed-attempt read.
replace_once(
    "tests/product/test_research_robustness_http_boundary.py",
    '''    PROJECT_SHA = "3" * 64\n''',
    '''    SOURCE_RESULT_SHA = "a" * 64\n    PROJECT_SHA = "3" * 64\n''',
)
replace_once(
    "tests/product/test_research_robustness_http_boundary.py",
    '''            "native_project_name": self.PROJECT_NAME,\n''',
    '''            "native_project_name": self.PROJECT_NAME,\n            "source_result_archive_sha256": self.SOURCE_RESULT_SHA,\n''',
)
replace_once(
    "tests/product/test_research_robustness_http_boundary.py",
    '''                "launcher_sha256": self.LAUNCHER_SHA,\n            }],\n''',
    '''                "launcher_sha256": self.LAUNCHER_SHA,\n                "source_result_archive_sha256": self.SOURCE_RESULT_SHA,\n            }],\n''',
)
replace_once(
    "tests/product/test_research_robustness_http_boundary.py",
    '''    def test_capability_and_catalog_reads_accept_no_browser_injected_settings(self) -> None:\n''',
    '''    def test_failed_attempt_exact_read_uses_durable_attempt_identity(self) -> None:\n        attempt_ref = f"tc-evidence:sha256:{'b' * 64}"\n        attempt = {\n            "schema": "tc.research-native-robustness-attempt.v1",\n            "state": "failed",\n            "attempt_ref": attempt_ref,\n            "proof_entity_id": "tc-research:proof:v1:77777777-7777-4777-8777-777777777777",\n            "proof_revision": f"tc-research-revision:proof:sha256:{'8' * 64}",\n            "operation": "native_retester_cross_check",\n            "method": "RetestWithHigherPrecision",\n            "native_project_name": self.PROJECT_NAME,\n            "compiled_project_sha256": self.PROJECT_SHA,\n            "engine_sha256": self.ENGINE_SHA,\n            "source_result_archive_sha256": self.SOURCE_RESULT_SHA,\n            "partial_side_effect": True,\n            "failure_reason_code": "sqx_command_timeout",\n            "receipts": [{\n                "action": "startOnlyTask", "project": self.PROJECT_NAME, "task": 1, "state": "timeout",\n                "project_sha256": self.PROJECT_SHA, "engine_sha256": self.ENGINE_SHA,\n                "source_result_archive_sha256": self.SOURCE_RESULT_SHA,\n            }],\n        }\n        with TemporaryDirectory() as tmp:\n            server, thread, store = self._server(Path(tmp))\n            endpoint = f"http://127.0.0.1:{server.server_port}/api/research/historical-results"\n            try:\n                with patch("tradercockpit.research_retester_http.read_native_robustness_attempt", return_value=attempt) as reader:\n                    status, payload = self._post(endpoint, {"action": "read-robustness-attempt", "attempt_ref": attempt_ref})\n                    self.assertEqual(status, 200)\n                    self.assertEqual(payload["attempt_ref"], attempt_ref)\n                    reader.assert_called_once_with(store, attempt_ref)\n            finally:\n                server.shutdown(); server.server_close(); thread.join()\n\n    def test_capability_and_catalog_reads_accept_no_browser_injected_settings(self) -> None:\n''',
)
replace_once(
    "tests/product/test_research_robustness_http_boundary.py",
    '''        catalog = {"schema": "tc.research-native-robustness-catalog.v1", "results": []}\n''',
    '''        catalog = {"schema": "tc.research-native-robustness-catalog.v1", "results": [], "attempts": []}\n''',
)

# JS fixtures/tests for baseline receipt binding and failed-attempt catalog model.
replace_once(
    "tests/research-backtest-robustness.test.mjs",
    '''  fetchRobustnessCatalog,\n  fetchRobustnessResult,\n''',
    '''  fetchRobustnessAttempt,\n  fetchRobustnessCatalog,\n  fetchRobustnessResult,\n''',
)
replace_once(
    "tests/research-backtest-robustness.test.mjs",
    '''  robustnessCapabilitiesFromPayload,\n  robustnessCatalogFromPayload,\n''',
    '''  robustnessAttemptFromPayload,\n  robustnessCapabilitiesFromPayload,\n  robustnessCatalogFromPayload,\n''',
)
replace_once(
    "tests/research-backtest-robustness.test.mjs",
    '''      engine_sha256: engineSha,\n    }],\n''',
    '''      engine_sha256: engineSha,\n      source_result_archive_sha256: sourceArchiveSha,\n    }],\n''',
)
replace_once(
    "tests/research-backtest-robustness.test.mjs",
    '''function response(payload, { ok = true, status = 200 } = {}) {\n''',
    '''function failedAttempt(overrides = {}) {\n  return {\n    schema: "tc.research-native-robustness-attempt.v1",\n    state: "failed",\n    attempt_ref: `tc-evidence:sha256:${"b".repeat(64)}`,\n    proof_entity_id: "tc-research:proof:v1:55555555-5555-4555-8555-555555555555",\n    proof_revision: `tc-research-revision:proof:sha256:${"c".repeat(64)}`,\n    sqx_build: "144.2953",\n    operation: "native_retester_cross_check",\n    method: "RetestWithHigherPrecision",\n    source_historical_result_entity_id: historicalEntity,\n    source_historical_result_revision: historicalRevision,\n    source_result_archive_ref: `tc-evidence:sha256:${sourceArchiveSha}`,\n    source_result_archive_sha256: sourceArchiveSha,\n    source_project_ref: `tc-evidence:sha256:${sourceProjectSha}`,\n    source_project_sha256: sourceProjectSha,\n    compiled_project_ref: `tc-evidence:sha256:${compiledProjectSha}`,\n    compiled_project_sha256: compiledProjectSha,\n    configuration_changed: true,\n    source_task_sha256: sourceTaskSha,\n    compiled_task_sha256: compiledTaskSha,\n    native_settings: { Precision: "2", Spread: "3" },\n    engine_ref: `tc-evidence:sha256:${engineSha}`,\n    engine_sha256: engineSha,\n    native_project_name: projectName,\n    native_project_relative_path: `user/projects/${projectName}/project.cfx`,\n    launcher_sha256: launcherSha,\n    receipts: [{\n      action: "startOnlyTask", project: projectName, task: 1, state: "timeout",\n      launcher_sha256: launcherSha, project_sha256: compiledProjectSha, engine_sha256: engineSha,\n      source_result_archive_sha256: sourceArchiveSha,\n    }],\n    partial_side_effect: true,\n    failure_reason_code: "sqx_command_timeout",\n    ...overrides,\n  };\n}\n\nfunction response(payload, { ok = true, status = 200 } = {}) {\n''',
)
replace_once(
    "tests/research-backtest-robustness.test.mjs",
    '''  const catalogPayload = { schema: "tc.research-native-robustness-catalog.v1", results: [robustness()] };\n  assert.equal(robustnessCatalogFromPayload(catalogPayload)[0].validation_ref, `tc-evidence:sha256:${validationSha}`);\n''',
    '''  const catalogPayload = { schema: "tc.research-native-robustness-catalog.v1", results: [robustness()], attempts: [failedAttempt()] };\n  const parsedCatalog = robustnessCatalogFromPayload(catalogPayload);\n  assert.equal(parsedCatalog.results[0].validation_ref, `tc-evidence:sha256:${validationSha}`);\n  assert.equal(parsedCatalog.attempts[0].failure_reason_code, "sqx_command_timeout");\n''',
)
with Path("tests/research-backtest-robustness.test.mjs").open("a", encoding="utf-8") as handle:
    handle.write('''\n\ntest("failed robustness attempt is a durable exact read model", async () => {\n  const parsed = robustnessAttemptFromPayload(failedAttempt());\n  assert.equal(parsed.state, "failed");\n  assert.equal(parsed.partial_side_effect, true);\n  let request;\n  const result = await fetchRobustnessAttempt(parsed.attempt_ref, async (url, options) => {\n    request = { url, options };\n    return response(failedAttempt());\n  });\n  assert.deepEqual(JSON.parse(request.options.body), { action: "read-robustness-attempt", attempt_ref: parsed.attempt_ref });\n  assert.equal(result.proof_revision, parsed.proof_revision);\n});\n\ntest("completed robustness receipt must bind the staged baseline hash", () => {\n  assert.throws(\n    () => robustnessResultFromPayload(robustness({ receipts: [{ ...robustness().receipts[0], source_result_archive_sha256: "0".repeat(64) }] })),\n    /custody is inconsistent/,\n  );\n});\n''')

print("Applied robustness review round four corrections")
