"""Native SQX robustness execution and immutable result custody.

This module deliberately starts with one producer-owned cross-check:
``RetestWithHigherPrecision``. TraderCockpit does not implement the robustness
algorithm. It takes the currently installed Retester project as the executable
specification, requires the native Higher Precision profile to already exist,
enables only that profile when necessary, then runs the existing trusted native
Retester task-1 boundary.

The returned record proves configuration/execution/result provenance. It does
not infer pass/fail from process completion. A producer-backed outcome parser is
a separate readback concern and remains explicit until an authoritative native
seam is observed.
"""

from __future__ import annotations

from hashlib import sha256
from io import BytesIO
import json
from pathlib import Path
import re
from uuid import UUID, uuid4
from xml.etree import ElementTree
from zipfile import BadZipFile, ZipFile

from tradercockpit.research_custody import (
    EvidenceRef,
    FileResearchCustodyStore,
    ResearchCustodyError,
    ResearchEntityId,
    ResearchKind,
    ResearchRevisionRef,
)
from tradercockpit.research_retester import (
    RETESTER_ENGINE_RELATIVE_PATH,
    RETESTER_PROJECT_TASK_ENTRY,
    RETESTER_SOURCE_PROJECT,
    ResearchRetesterError,
    _capture_result,
    _member,
    _read_exact_inside,
    _stage_file,
    _validate_retester_project,
    read_current_historical_result,
)
from tradercockpit.sqx_gateway import SqxNativeControlGateway, SqxNativeGatewayError
from tradercockpit.sqx_outputs import SqxOutputError, inspect_sqx_output_bytes
from tradercockpit.sqx_presets import SQX_BUILD, SqxPresetRuntimeError, verified_sqx_home


ROBUSTNESS_RECORD_SCHEMA = "tc.research-native-robustness.v1"
ROBUSTNESS_METHOD_HIGHER_PRECISION = "RetestWithHigherPrecision"
ROBUSTNESS_OPERATION = "native_retester_cross_check"
ROBUSTNESS_OUTCOME_UNREAD = "producer_result_captured_outcome_unread"
ROBUSTNESS_ATTEMPT_SCHEMA = "tc.research-native-robustness-attempt.v1"
ROBUSTNESS_CATALOG_SCHEMA = "tc.research-native-robustness-catalog.v1"
ROBUSTNESS_CAPABILITIES_SCHEMA = "tc.research-native-robustness-capabilities.v1"
_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")
_CURRENT_POINTER_TEMP_RE = re.compile(
    r"^\.[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.json\.tmp-[0-9]+-[0-9a-f]{32}$"
)


class ResearchRobustnessError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


def _canonical(payload: dict[str, object]) -> bytes:
    return json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def _digest(value: object, code: str) -> str:
    if not isinstance(value, str) or not _DIGEST_RE.fullmatch(value):
        raise ResearchRobustnessError(code, "expected a lowercase 64-character SHA-256 digest")
    return value


def _local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def _exact_child(parent: ElementTree.Element, name: str, code: str) -> ElementTree.Element:
    matches = [child for child in parent if _local_name(child.tag) == name]
    if len(matches) != 1:
        raise ResearchRobustnessError(code, f"native Retester profile requires exactly one {name} element")
    return matches[0]


def _text(child: ElementTree.Element, name: str, code: str) -> str:
    element = _exact_child(child, name, code)
    value = (element.text or "").strip()
    if not value:
        raise ResearchRobustnessError(code, f"native Retester {name} value is empty")
    return value


def _task_profile(task_bytes: bytes, *, require_enabled: bool) -> tuple[ElementTree.Element, ElementTree.Element, dict[str, str]]:
    try:
        root = ElementTree.fromstring(task_bytes)
    except (ElementTree.ParseError, LookupError, ValueError) as exc:
        raise ResearchRobustnessError(
            "robustness_source_task_invalid",
            "native Retester task-1 settings are not valid XML",
        ) from exc
    if _local_name(root.tag) != "Settings":
        raise ResearchRobustnessError(
            "robustness_source_task_invalid",
            "native Retester task-1 root must be Settings",
        )

    cross_checks = [node for node in root.iter() if _local_name(node.tag) == "CrossChecks"]
    if len(cross_checks) != 1:
        raise ResearchRobustnessError(
            "robustness_crosschecks_invalid",
            "native Retester task must contain exactly one CrossChecks section",
        )
    cross_checks_node = cross_checks[0]
    profiles = [
        child
        for child in cross_checks_node
        if _local_name(child.tag) == ROBUSTNESS_METHOD_HIGHER_PRECISION
    ]
    if len(profiles) != 1:
        raise ResearchRobustnessError(
            "robustness_higher_precision_missing",
            "installed Retester project does not contain one Higher Precision profile; configure/save it in SQX first",
        )
    target = profiles[0]
    use = target.attrib.get("use")
    if use not in {"true", "false"}:
        raise ResearchRobustnessError(
            "robustness_higher_precision_invalid",
            "native Higher Precision profile has an invalid use attribute",
        )
    if require_enabled and use != "true":
        raise ResearchRobustnessError(
            "robustness_compiled_task_invalid",
            "compiled Higher Precision profile is not enabled",
        )

    active_others = [
        _local_name(child.tag)
        for child in cross_checks_node
        if child is not target and child.attrib.get("use") == "true"
    ]
    if active_others:
        raise ResearchRobustnessError(
            "robustness_other_crosscheck_enabled",
            "Higher Precision isolation requires all other native cross-checks disabled: "
            + ", ".join(active_others),
        )

    settings = _exact_child(target, "Settings", "robustness_higher_precision_invalid")
    values = {
        "Precision": _text(settings, "Precision", "robustness_higher_precision_invalid"),
        "Spread": _text(settings, "Spread", "robustness_higher_precision_invalid"),
    }
    return root, target, values


def _rewrite_higher_precision_task(task_bytes: bytes) -> tuple[bytes, dict[str, str], bool]:
    root, target, settings = _task_profile(task_bytes, require_enabled=False)
    if target.attrib.get("use") == "true":
        return task_bytes, settings, False

    target.set("use", "true")
    compiled = ElementTree.tostring(root, encoding="utf-8", xml_declaration=True)
    _task_profile(compiled, require_enabled=True)
    return compiled, settings, True


def _zip_member(project_bytes: bytes, entry_name: str, code: str) -> bytes:
    try:
        with ZipFile(BytesIO(project_bytes)) as archive:
            matches = [entry for entry in archive.infolist() if entry.filename == entry_name]
            if len(matches) != 1:
                raise ResearchRobustnessError(code, f"project must contain exactly one {entry_name}")
            payload = archive.read(matches[0])
    except ResearchRobustnessError:
        raise
    except (BadZipFile, RuntimeError, NotImplementedError, EOFError, OSError) as exc:
        raise ResearchRobustnessError(code, f"project member {entry_name} is unreadable") from exc
    if not payload:
        raise ResearchRobustnessError(code, f"project member {entry_name} is empty")
    return payload


def compile_higher_precision_project(project_bytes: bytes) -> tuple[bytes, dict[str, object]]:
    """Enable only the installed native Higher Precision profile in project.cfx.

    If the current installed Retester profile is already enabled, the exact source
    project bytes are retained. Otherwise only the task XML is reserialized with
    that existing profile enabled; no Precision/Spread default is invented.
    """

    try:
        _validate_retester_project(project_bytes)
    except ResearchRetesterError as exc:
        raise ResearchRobustnessError(exc.code, exc.detail) from exc

    source_task = _zip_member(
        project_bytes,
        RETESTER_PROJECT_TASK_ENTRY,
        "robustness_source_project_invalid",
    )
    compiled_task, settings, changed = _rewrite_higher_precision_task(source_task)
    if not changed:
        compiled_project = project_bytes
    else:
        try:
            source_stream = BytesIO(project_bytes)
            output_stream = BytesIO()
            with ZipFile(source_stream) as source:
                names = source.namelist()
                if len(names) != len(set(names)):
                    raise ResearchRobustnessError(
                        "robustness_source_project_invalid",
                        "native Retester project contains duplicate archive members",
                    )
                with ZipFile(output_stream, "w") as output:
                    output.comment = source.comment
                    for info in source.infolist():
                        payload = compiled_task if info.filename == RETESTER_PROJECT_TASK_ENTRY else source.read(info)
                        output.writestr(info, payload)
            compiled_project = output_stream.getvalue()
        except ResearchRobustnessError:
            raise
        except (BadZipFile, RuntimeError, NotImplementedError, EOFError, OSError) as exc:
            raise ResearchRobustnessError(
                "robustness_source_project_invalid",
                "native Retester project is not a readable project archive",
            ) from exc

    if not compiled_project:
        raise ResearchRobustnessError(
            "robustness_compiled_project_invalid",
            "Higher Precision compilation produced an empty native project snapshot",
        )
    try:
        _validate_retester_project(compiled_project)
    except ResearchRetesterError as exc:
        raise ResearchRobustnessError("robustness_compiled_project_invalid", exc.detail) from exc
    compiled_task_check = _zip_member(
        compiled_project,
        RETESTER_PROJECT_TASK_ENTRY,
        "robustness_compiled_project_invalid",
    )
    _, _, compiled_settings = _task_profile(compiled_task_check, require_enabled=True)
    if compiled_settings != settings:
        raise ResearchRobustnessError(
            "robustness_compiled_project_invalid",
            "compiled Higher Precision settings changed unexpectedly",
        )

    return compiled_project, {
        "method": ROBUSTNESS_METHOD_HIGHER_PRECISION,
        "native_settings": settings,
        "configuration_changed": changed,
        "source_task_sha256": sha256(source_task).hexdigest(),
        "compiled_task_sha256": sha256(compiled_task_check).hexdigest(),
        "source_project_sha256": sha256(project_bytes).hexdigest(),
        "compiled_project_sha256": sha256(compiled_project).hexdigest(),
    }


def _stage_workspace(
    home: Path,
    project_bytes: bytes,
    archive_name: str,
    archive_bytes: bytes,
) -> tuple[str, Path, str]:
    project_name = f"TraderCockpit-Retester-{uuid4().hex}"
    projects_root = home / "user/projects"
    try:
        projects = projects_root.resolve(strict=True)
        projects.relative_to(home)
    except (OSError, RuntimeError, ValueError) as exc:
        raise ResearchRobustnessError(
            "robustness_project_path_escape",
            "SQX project root escapes verified runtime",
        ) from exc
    if not projects.is_dir():
        raise ResearchRobustnessError("retester_projects_missing", "SQX project root is missing")

    project_root = projects / project_name
    results = project_root / "databanks/Results"
    if project_root.exists():
        raise ResearchRobustnessError("robustness_workspace_conflict", "isolated robustness workspace already exists")
    try:
        results.mkdir(parents=True)
        resolved_project = project_root.resolve(strict=True)
        resolved_results = results.resolve(strict=True)
        resolved_project.relative_to(home)
        resolved_results.relative_to(resolved_project)
    except (OSError, RuntimeError, ValueError) as exc:
        raise ResearchRobustnessError(
            "robustness_project_path_escape",
            "isolated robustness workspace escapes verified runtime",
        ) from exc
    if resolved_project.parent != projects:
        raise ResearchRobustnessError(
            "robustness_project_path_escape",
            "isolated robustness project is not one direct SQX project child",
        )
    if Path(archive_name).name != archive_name or not archive_name.lower().endswith(".sqx"):
        raise ResearchRobustnessError("robustness_source_result_invalid", "historical result archive name is invalid")

    project_file = resolved_project / "project.cfx"
    archive_file = resolved_results / archive_name
    try:
        _stage_file(project_file, project_bytes, conflict_code="robustness_workspace_conflict")
        _stage_file(archive_file, archive_bytes, conflict_code="robustness_workspace_conflict")
    except ResearchRetesterError as exc:
        raise ResearchRobustnessError(exc.code, exc.detail) from exc
    if sha256(project_file.read_bytes()).hexdigest() != sha256(project_bytes).hexdigest():
        raise ResearchRobustnessError("robustness_stage_corrupt", "staged robustness project changed after write")
    if sha256(archive_file.read_bytes()).hexdigest() != sha256(archive_bytes).hexdigest():
        raise ResearchRobustnessError("robustness_stage_corrupt", "staged historical result changed after write")
    return project_name, project_file, f"user/projects/{project_name}/project.cfx"



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


def _failed_proof_records(store: FileResearchCustodyStore) -> list[dict[str, object]]:
    results: list[dict[str, object]] = []
    identity_keys = (
        "sqx_build", "operation", "method",
        "source_historical_result_entity_id", "source_historical_result_revision",
        "source_result_archive_ref", "source_result_archive_sha256",
        "source_project_ref", "source_project_sha256",
        "compiled_project_ref", "compiled_project_sha256", "configuration_changed",
        "source_task_sha256", "compiled_task_sha256", "native_settings",
        "engine_ref", "engine_sha256", "native_project_name", "native_project_relative_path",
    )
    required = {
        "schema", "state", *identity_keys,
        "launcher_sha256", "receipts", "partial_side_effect", "failure_reason_code",
    }
    launched_states = {"completed", "timeout", "rejected", "invalid_receipt"}
    allowed_states = launched_states | {"preflight_failed", "launch_failed"}
    for entity in _current_proof_entities(store):
        revision = store.current(entity)
        stored = store.read_revision(revision)
        try:
            raw = json.loads(store.read_revision_content(revision))
        except (UnicodeDecodeError, json.JSONDecodeError):
            continue
        if not isinstance(raw, dict) or raw.get("schema") != ROBUSTNESS_ATTEMPT_SCHEMA:
            continue
        attempt_state = raw.get("state")
        if attempt_state not in {"failed", "prepared"}:
            continue
        if set(raw) != required:
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness proof schema is invalid")
        if attempt_state == "prepared":
            if stored.parent_revision is not None:
                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "prepared robustness proof unexpectedly has a parent")
            parent_revision = stored
            prepared = raw
        else:
            if stored.parent_revision is None:
                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness proof has no prepared parent")
            parent_revision = store.read_revision(stored.parent_revision)
            try:
                prepared = json.loads(store.read_revision_content(stored.parent_revision))
            except (UnicodeDecodeError, json.JSONDecodeError) as exc:
                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "prepared robustness proof is unreadable") from exc
            if not isinstance(prepared, dict) or prepared.get("schema") != ROBUSTNESS_ATTEMPT_SCHEMA or prepared.get("state") != "prepared":
                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness proof parent is not one prepared native attempt")
        if any(prepared.get(key) != raw.get(key) for key in identity_keys):
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness proof changed its prepared control identity")
        try:
            source_entity = ResearchEntityId.parse(raw["source_historical_result_entity_id"])
            source_revision = ResearchRevisionRef.parse(raw["source_historical_result_revision"])
        except (KeyError, TypeError, ResearchCustodyError) as exc:
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness proof source identities are invalid") from exc
        if source_entity.kind != ResearchKind.HISTORICAL_RESULT or source_revision.kind != ResearchKind.HISTORICAL_RESULT:
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness source is not Historical Result custody")
        if raw.get("sqx_build") != SQX_BUILD or raw.get("operation") != ROBUSTNESS_OPERATION or raw.get("method") != ROBUSTNESS_METHOD_HIGHER_PRECISION:
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness producer identity is invalid")
        if type(raw.get("configuration_changed")) is not bool or type(raw.get("partial_side_effect")) is not bool:
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness boolean state is invalid")
        if attempt_state == "failed":
            if not isinstance(raw.get("failure_reason_code"), str) or not raw["failure_reason_code"]:
                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness reason is invalid")
        elif raw.get("failure_reason_code") is not None:
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "prepared robustness proof already claims a failure reason")
        if not isinstance(raw.get("native_project_name"), str) or re.fullmatch(r"TraderCockpit-Retester-[0-9a-f]{32}", raw["native_project_name"]) is None:
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness project identity is invalid")
        if raw.get("native_project_relative_path") != f'user/projects/{raw["native_project_name"]}/project.cfx':
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness project path is invalid")

        evidence_pairs = (
            ("source_result_archive_ref", "source_result_archive_sha256"),
            ("source_project_ref", "source_project_sha256"),
            ("compiled_project_ref", "compiled_project_sha256"),
            ("engine_ref", "engine_sha256"),
        )
        evidence: dict[str, bytes] = {}
        prepared_evidence: set[EvidenceRef] = set()
        for ref_key, digest_key in evidence_pairs:
            try:
                ref = EvidenceRef.parse(raw[ref_key])
                value = store.read_evidence(ref)
            except (KeyError, TypeError, ResearchCustodyError) as exc:
                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", f"failed robustness evidence {ref_key} is invalid") from exc
            digest = _digest(raw.get(digest_key), "robustness_proof_catalog_corrupt")
            if ref.digest != digest or sha256(value).hexdigest() != digest:
                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", f"failed robustness evidence {ref_key} binding is invalid")
            evidence[ref_key] = value
            prepared_evidence.add(ref)
        if set(parent_revision.evidence) != prepared_evidence or set(stored.evidence) != prepared_evidence:
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness proof evidence set is invalid")

        source_project = evidence["source_project_ref"]
        compiled_project = evidence["compiled_project_ref"]
        try:
            _validate_retester_project(source_project)
            _validate_retester_project(compiled_project)
        except ResearchRetesterError as exc:
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", exc.detail) from exc
        source_task = _zip_member(source_project, RETESTER_PROJECT_TASK_ENTRY, "robustness_proof_catalog_corrupt")
        compiled_task = _zip_member(compiled_project, RETESTER_PROJECT_TASK_ENTRY, "robustness_proof_catalog_corrupt")
        if sha256(source_task).hexdigest() != _digest(raw.get("source_task_sha256"), "robustness_proof_catalog_corrupt"):
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness source task identity is invalid")
        if sha256(compiled_task).hexdigest() != _digest(raw.get("compiled_task_sha256"), "robustness_proof_catalog_corrupt"):
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness compiled task identity is invalid")
        _, _, native_settings = _task_profile(compiled_task, require_enabled=True)
        if raw.get("native_settings") != native_settings:
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness compiled settings do not match custody")
        if raw["configuration_changed"] is False and source_project != compiled_project:
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness unchanged configuration does not preserve exact source bytes")
        if raw["configuration_changed"] is True and source_project == compiled_project:
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness changed configuration is byte-identical to source")

        launcher = raw.get("launcher_sha256")
        if launcher is not None and (not isinstance(launcher, str) or _DIGEST_RE.fullmatch(launcher) is None):
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness launcher identity is invalid")
        receipts = raw.get("receipts")
        if not isinstance(receipts, list) or len(receipts) > 1 or any(not isinstance(item, dict) for item in receipts):
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness receipts are invalid")
        for receipt in receipts:
            state = receipt.get("state")
            if state not in allowed_states or receipt.get("action") != "startOnlyTask" or receipt.get("task") != 1 or receipt.get("project") != raw["native_project_name"]:
                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness receipt identity is invalid")
            if receipt.get("launcher_sha256") is not None and receipt.get("launcher_sha256") != launcher:
                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness receipt launcher identity is invalid")
            if receipt.get("project_sha256") is not None and receipt.get("project_sha256") != raw["compiled_project_sha256"]:
                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness receipt project identity is invalid")
            if receipt.get("engine_sha256") is not None and receipt.get("engine_sha256") != raw["engine_sha256"]:
                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness receipt engine identity is invalid")
            if state in launched_states and receipt.get("result_archive_sha256") != raw["source_result_archive_sha256"]:
                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness launched receipt lost staged baseline identity")
        launched = any(item.get("state") in launched_states for item in receipts)
        if launched != raw["partial_side_effect"]:
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness partial-side-effect state contradicts native receipt state")
        invalid_receipt = any(item.get("state") == "invalid_receipt" for item in receipts)
        if raw["partial_side_effect"] and launcher is None and not invalid_receipt:
            raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "failed robustness side-effect state lacks launcher custody")
        exposed = dict(raw)
        if attempt_state == "prepared":
            if launcher is not None or receipts or raw["partial_side_effect"] or raw.get("failure_reason_code") is not None:
                raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "prepared robustness proof already claims execution completion state")
            exposed.update({
                "state": "interrupted",
                "partial_side_effect": True,
                "failure_reason_code": "robustness_attempt_interrupted",
            })
        results.append({
            **exposed,
            "attempt_ref": str(stored.content),
            "proof_entity_id": str(entity),
            "proof_revision": str(revision),
        })
    return results


def list_native_robustness_results(store: FileResearchCustodyStore) -> dict[str, object]:
    """List completed runs plus failed/interrupted native attempts from durable Research custody."""

    return {
        "schema": ROBUSTNESS_CATALOG_SCHEMA,
        "results": _completed_proof_records(store),
        "failed_attempts": _failed_proof_records(store),
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


def _record_identity(payload: dict[str, object]) -> None:
    if payload.get("sqx_build") != SQX_BUILD:
        raise ResearchRobustnessError("robustness_record_corrupt", "robustness SQX build identity is invalid")
    if payload.get("operation") != ROBUSTNESS_OPERATION:
        raise ResearchRobustnessError("robustness_record_corrupt", "robustness operation identity is invalid")
    if payload.get("method") != ROBUSTNESS_METHOD_HIGHER_PRECISION:
        raise ResearchRobustnessError("robustness_record_corrupt", "robustness method identity is invalid")
    if payload.get("execution_state") != "completed":
        raise ResearchRobustnessError("robustness_record_corrupt", "robustness execution state is invalid")
    if payload.get("producer_outcome_state") != ROBUSTNESS_OUTCOME_UNREAD:
        raise ResearchRobustnessError("robustness_record_corrupt", "robustness producer outcome state is invalid")
    if type(payload.get("configuration_changed")) is not bool:
        raise ResearchRobustnessError("robustness_record_corrupt", "robustness configuration-change state is invalid")

    try:
        entity = ResearchEntityId.parse(payload["source_historical_result_entity_id"])
        revision = ResearchRevisionRef.parse(payload["source_historical_result_revision"])
    except (KeyError, ResearchCustodyError, TypeError) as exc:
        raise ResearchRobustnessError("robustness_record_corrupt", "source Historical Result identity is invalid") from exc
    if entity.kind != ResearchKind.HISTORICAL_RESULT or revision.kind != ResearchKind.HISTORICAL_RESULT:
        raise ResearchRobustnessError("robustness_record_corrupt", "source robustness identity is not Historical Result custody")

    launcher = _digest(payload.get("launcher_sha256"), "robustness_record_corrupt")
    if launcher != payload["launcher_sha256"]:
        raise ResearchRobustnessError("robustness_record_corrupt", "launcher identity is invalid")
    receipts = payload.get("receipts")
    if not isinstance(receipts, list) or len(receipts) != 1 or not isinstance(receipts[0], dict):
        raise ResearchRobustnessError("robustness_record_corrupt", "native robustness receipt list is invalid")
    receipt = receipts[0]
    if (
        receipt.get("action") != "startOnlyTask"
        or receipt.get("task") != 1
        or receipt.get("state") != "completed"
        or receipt.get("sqx_build") != SQX_BUILD
        or receipt.get("launcher_sha256") != launcher
        or receipt.get("project") != payload.get("native_project_name")
        or receipt.get("project_sha256") != payload.get("compiled_project_sha256")
        or receipt.get("engine_sha256") != payload.get("engine_sha256")
        or receipt.get("result_archive_sha256") != payload.get("source_result_archive_sha256")
    ):
        raise ResearchRobustnessError("robustness_record_corrupt", "native robustness receipt is invalid")


def _read_record(store: FileResearchCustodyStore, record_ref: EvidenceRef) -> dict[str, object]:
    try:
        record_bytes = store.read_evidence(record_ref)
        payload = json.loads(record_bytes)
    except (ResearchCustodyError, UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise ResearchRobustnessError("robustness_record_corrupt", "robustness record is unreadable") from exc
    if EvidenceRef.from_bytes(record_bytes) != record_ref:
        raise ResearchRobustnessError("robustness_record_corrupt", "robustness record content identity is invalid")

    required = {
        "schema",
        "sqx_build",
        "operation",
        "method",
        "source_historical_result_entity_id",
        "source_historical_result_revision",
        "source_result_archive_ref",
        "source_result_archive_sha256",
        "source_project_ref",
        "source_project_sha256",
        "compiled_project_ref",
        "compiled_project_sha256",
        "configuration_changed",
        "source_task_sha256",
        "compiled_task_sha256",
        "native_settings",
        "engine_ref",
        "engine_sha256",
        "launcher_sha256",
        "native_project_name",
        "native_project_relative_path",
        "receipts",
        "result_archive_name",
        "result_archive_ref",
        "result_archive_sha256",
        "result_strategy_ref",
        "result_strategy_sha256",
        "result_settings_ref",
        "result_settings_sha256",
        "execution_state",
        "producer_outcome_state",
    }
    if not isinstance(payload, dict) or set(payload) != required or payload.get("schema") != ROBUSTNESS_RECORD_SCHEMA:
        raise ResearchRobustnessError("robustness_record_corrupt", "robustness record schema is invalid")
    _record_identity(payload)

    evidence_pairs = (
        ("source_result_archive_ref", "source_result_archive_sha256"),
        ("source_project_ref", "source_project_sha256"),
        ("compiled_project_ref", "compiled_project_sha256"),
        ("engine_ref", "engine_sha256"),
        ("result_archive_ref", "result_archive_sha256"),
        ("result_strategy_ref", "result_strategy_sha256"),
        ("result_settings_ref", "result_settings_sha256"),
    )
    evidence: dict[str, bytes] = {}
    for ref_key, sha_key in evidence_pairs:
        try:
            ref = EvidenceRef.parse(payload[ref_key])
            value = store.read_evidence(ref)
        except (ResearchCustodyError, TypeError) as exc:
            raise ResearchRobustnessError("robustness_record_corrupt", f"robustness evidence {ref_key} is invalid") from exc
        digest = _digest(payload.get(sha_key), "robustness_record_corrupt")
        if sha256(value).hexdigest() != digest or ref.digest != digest:
            raise ResearchRobustnessError("robustness_record_corrupt", f"robustness evidence {ref_key} binding is invalid")
        evidence[ref_key] = value

    source_project = evidence["source_project_ref"]
    compiled_project = evidence["compiled_project_ref"]
    try:
        _validate_retester_project(source_project)
        _validate_retester_project(compiled_project)
    except ResearchRetesterError as exc:
        raise ResearchRobustnessError("robustness_record_corrupt", exc.detail) from exc
    source_task = _zip_member(source_project, RETESTER_PROJECT_TASK_ENTRY, "robustness_record_corrupt")
    compiled_task = _zip_member(compiled_project, RETESTER_PROJECT_TASK_ENTRY, "robustness_record_corrupt")
    if sha256(source_task).hexdigest() != payload.get("source_task_sha256"):
        raise ResearchRobustnessError("robustness_record_corrupt", "source task identity is invalid")
    if sha256(compiled_task).hexdigest() != payload.get("compiled_task_sha256"):
        raise ResearchRobustnessError("robustness_record_corrupt", "compiled task identity is invalid")
    _, _, native_settings = _task_profile(compiled_task, require_enabled=True)
    if payload.get("native_settings") != native_settings:
        raise ResearchRobustnessError("robustness_record_corrupt", "compiled Higher Precision settings do not match custody")
    if payload["configuration_changed"] is False and source_project != compiled_project:
        raise ResearchRobustnessError("robustness_record_corrupt", "unchanged configuration does not preserve exact source project bytes")
    if payload["configuration_changed"] is True and source_project == compiled_project:
        raise ResearchRobustnessError("robustness_record_corrupt", "changed configuration is byte-identical to source project")

    result_ref = EvidenceRef.parse(payload["result_archive_ref"])
    try:
        inspected = inspect_sqx_output_bytes(
            store.read_evidence(result_ref),
            archive_name=payload["result_archive_name"],
        )
    except (ResearchCustodyError, SqxOutputError, TypeError) as exc:
        raise ResearchRobustnessError("robustness_record_corrupt", "native robustness result archive is invalid") from exc
    if (
        inspected["archive_sha256"] != payload["result_archive_sha256"]
        or inspected["strategy_entry_sha256"] != payload["result_strategy_sha256"]
        or inspected["settings_entry_sha256"] != payload["result_settings_sha256"]
        or payload["result_archive_sha256"] == payload["source_result_archive_sha256"]
    ):
        raise ResearchRobustnessError("robustness_record_corrupt", "native robustness result members do not match custody")
    return payload


def read_native_robustness_result(
    store: FileResearchCustodyStore,
    validation_ref: str | EvidenceRef,
) -> dict[str, object]:
    """Reopen one exact immutable native robustness execution record."""

    try:
        ref = validation_ref if isinstance(validation_ref, EvidenceRef) else EvidenceRef.parse(validation_ref)
    except (ResearchCustodyError, TypeError) as exc:
        raise ResearchRobustnessError("robustness_record_ref_invalid", "validation_ref is not a valid evidence identity") from exc
    matches = [item for item in _completed_proof_records(store) if item.get("validation_ref") == str(ref)]
    matches.extend(item for item in _failed_proof_records(store) if item.get("attempt_ref") == str(ref))
    if not matches:
        raise ResearchRobustnessError(
            "robustness_proof_required",
            "validation_ref is not registered as the current completed or failed content of a Research proof",
        )
    if len(matches) > 1:
        raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "multiple current robustness proofs reference one validation record")
    return matches[0]


def start_native_higher_precision(
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
            result_archive_name=historical["result_archive_name"],
            expected_result_archive_sha256=source_result_sha,
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
        and receipt.get("result_archive_name") == historical["result_archive_name"]
        and receipt.get("result_archive_sha256") == source_result_sha
        and isinstance(receipt.get("result_archive_relative_path"), str)
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
        and receipt_items[0].get("result_archive_name") == historical["result_archive_name"]
        and receipt_items[0].get("result_archive_sha256") == source_result_sha
        and receipt_items[0].get("result_archive_relative_path") == receipt.get("result_archive_relative_path")
    )
    if not receipt_valid:
        nested_launcher = receipt_items[0].get("launcher_sha256") if len(receipt_items) == 1 else None
        canonical_launcher = next((
            value for value in (raw_launcher, nested_launcher, trusted_launcher_sha256)
            if isinstance(value, str) and _DIGEST_RE.fullmatch(value) is not None
        ), None)
        invalid_receipt = ({
            "action": "startOnlyTask",
            "project": project_name,
            "task": 1,
            "state": "invalid_receipt",
            "sqx_build": SQX_BUILD,
            "launcher_sha256": canonical_launcher,
            "project_sha256": compiled_project_sha,
            "engine_sha256": engine_sha,
            "result_archive_name": historical["result_archive_name"],
            "result_archive_relative_path": f"user/projects/{project_name}/databanks/Results/{historical['result_archive_name']}",
            "result_archive_sha256": source_result_sha,
            "reason_code": "robustness_receipt_invalid",
        },)
        _failed_successor(
            store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,
            reason_code="robustness_receipt_invalid",
            launcher_sha256=canonical_launcher,
            receipts=invalid_receipt,
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

    try:
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
        record_ref = completed_revision.content
        reopened = _read_record(store, record_ref)
        store.compare_and_set_current(
            proof_entity,
            expected_revision=prepared_revision.revision,
            target_revision=completed_revision.revision,
        )
    except (ResearchCustodyError, OSError) as exc:
        try:
            _failed_successor(
                store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,
                reason_code="robustness_completion_custody_failed",
                launcher_sha256=launcher_sha,
                receipts=receipts,
                partial_side_effect=True,
            )
        except (ResearchCustodyError, OSError) as failure_exc:
            raise ResearchRobustnessError(
                "robustness_completion_custody_failed",
                "native execution completed, but result custody and failed-state custody could not be persisted",
            ) from failure_exc
        detail = exc.detail if isinstance(exc, ResearchCustodyError) else str(exc)
        raise ResearchRobustnessError("robustness_completion_custody_failed", detail) from exc
    except ResearchRobustnessError as exc:
        try:
            _failed_successor(
                store, proof_entity, prepared_revision.revision, prepared, prepared_evidence,
                reason_code=exc.code,
                launcher_sha256=launcher_sha,
                receipts=receipts,
                partial_side_effect=True,
            )
        except (ResearchCustodyError, OSError) as failure_exc:
            raise ResearchRobustnessError(
                "robustness_completion_custody_failed",
                "native execution completed, but result validation and failed-state custody could not be persisted",
            ) from failure_exc
        raise

    return {
        **reopened,
        "validation_ref": str(record_ref),
        "proof_entity_id": str(proof_entity),
        "proof_revision": str(completed_revision.revision),
    }
