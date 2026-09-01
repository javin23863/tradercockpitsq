"""Native SQX robustness execution and immutable result custody.

This module deliberately starts with one producer-owned cross-check:
``RetestWithHigherPrecision``. TraderCockpit does not implement the robustness
algorithm. It takes the currently installed Retester project as the executable
specification, requires the native Higher Precision profile to already exist,
enables only that profile, then runs the existing trusted native Retester task-1
boundary.

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
from uuid import uuid4
from xml.etree import ElementTree
from zipfile import BadZipFile, ZipFile

from tradercockpit.research_custody import EvidenceRef, FileResearchCustodyStore
from tradercockpit.research_retester import (
    RETESTER_ENGINE_RELATIVE_PATH,
    RETESTER_PROJECT_TASK_ENTRY,
    RETESTER_SOURCE_PROJECT,
    ResearchRetesterError,
    _capture_result,
    _member,
    _read_exact_inside,
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


class ResearchRobustnessError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


def _canonical(payload: dict[str, object]) -> bytes:
    return json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


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


def _rewrite_higher_precision_task(task_bytes: bytes) -> tuple[bytes, dict[str, str]]:
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
    if target.attrib.get("use") not in {"true", "false"}:
        raise ResearchRobustnessError(
            "robustness_higher_precision_invalid",
            "native Higher Precision profile has an invalid use attribute",
        )

    # The first bounded run must isolate this one cross-check. We do not silently
    # disable another user-selected native method because that would be a broader
    # configuration mutation than the requested test.
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
    precision = _text(settings, "Precision", "robustness_higher_precision_invalid")
    spread = _text(settings, "Spread", "robustness_higher_precision_invalid")
    target.set("use", "true")

    compiled = ElementTree.tostring(root, encoding="utf-8", xml_declaration=True)
    # Reparse the exact compiled bytes so no malformed serializer result can reach
    # the native launcher.
    try:
        check = ElementTree.fromstring(compiled)
    except ElementTree.ParseError as exc:  # pragma: no cover - defensive serializer guard
        raise ResearchRobustnessError(
            "robustness_compiled_task_invalid",
            "compiled Higher Precision task is not valid XML",
        ) from exc
    compiled_profiles = [
        node
        for node in check.iter()
        if _local_name(node.tag) == ROBUSTNESS_METHOD_HIGHER_PRECISION
    ]
    if len(compiled_profiles) != 1 or compiled_profiles[0].attrib.get("use") != "true":
        raise ResearchRobustnessError(
            "robustness_compiled_task_invalid",
            "compiled Higher Precision profile is not enabled",
        )
    return compiled, {"Precision": precision, "Spread": spread}


def compile_higher_precision_project(project_bytes: bytes) -> tuple[bytes, dict[str, object]]:
    """Enable only the installed native Higher Precision profile in project.cfx."""

    try:
        _validate_retester_project(project_bytes)
    except ResearchRetesterError as exc:
        raise ResearchRobustnessError(exc.code, exc.detail) from exc

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
            source_task = source.read(RETESTER_PROJECT_TASK_ENTRY)
            compiled_task, settings = _rewrite_higher_precision_task(source_task)
            with ZipFile(output_stream, "w") as output:
                output.comment = source.comment
                for info in source.infolist():
                    payload = compiled_task if info.filename == RETESTER_PROJECT_TASK_ENTRY else source.read(info)
                    output.writestr(info, payload)
    except ResearchRobustnessError:
        raise
    except (BadZipFile, RuntimeError, NotImplementedError, EOFError, OSError) as exc:
        raise ResearchRobustnessError(
            "robustness_source_project_invalid",
            "native Retester project is not a readable project archive",
        ) from exc

    compiled_project = output_stream.getvalue()
    if not compiled_project or compiled_project == project_bytes:
        raise ResearchRobustnessError(
            "robustness_compiled_project_invalid",
            "Higher Precision compilation did not create a distinct native project snapshot",
        )
    try:
        _validate_retester_project(compiled_project)
    except ResearchRetesterError as exc:
        raise ResearchRobustnessError("robustness_compiled_project_invalid", exc.detail) from exc

    return compiled_project, {
        "method": ROBUSTNESS_METHOD_HIGHER_PRECISION,
        "native_settings": settings,
        "source_task_sha256": sha256(source_task).hexdigest(),
        "compiled_task_sha256": sha256(compiled_task).hexdigest(),
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
    if project_root.exists():  # practically impossible UUID collision, still fail closed
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

    project_file = resolved_project / "project.cfx"
    archive_file = resolved_results / archive_name
    if Path(archive_name).name != archive_name or not archive_name.lower().endswith(".sqx"):
        raise ResearchRobustnessError("robustness_source_result_invalid", "historical result archive name is invalid")
    try:
        project_file.write_bytes(project_bytes)
        archive_file.write_bytes(archive_bytes)
    except OSError as exc:
        raise ResearchRobustnessError("robustness_stage_failed", "isolated robustness workspace could not be staged") from exc
    if sha256(project_file.read_bytes()).hexdigest() != sha256(project_bytes).hexdigest():
        raise ResearchRobustnessError("robustness_stage_corrupt", "staged robustness project changed after write")
    if sha256(archive_file.read_bytes()).hexdigest() != sha256(archive_bytes).hexdigest():
        raise ResearchRobustnessError("robustness_stage_corrupt", "staged historical result changed after write")
    return project_name, project_file, f"user/projects/{project_name}/project.cfx"


def _read_record(store: FileResearchCustodyStore, record_ref: EvidenceRef) -> dict[str, object]:
    try:
        payload = json.loads(store.read_evidence(record_ref))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise ResearchRobustnessError("robustness_record_corrupt", "robustness record is not valid JSON") from exc
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

    evidence_pairs = (
        ("source_result_archive_ref", "source_result_archive_sha256"),
        ("source_project_ref", "source_project_sha256"),
        ("compiled_project_ref", "compiled_project_sha256"),
        ("engine_ref", "engine_sha256"),
        ("result_archive_ref", "result_archive_sha256"),
        ("result_strategy_ref", "result_strategy_sha256"),
        ("result_settings_ref", "result_settings_sha256"),
    )
    for ref_key, sha_key in evidence_pairs:
        try:
            ref = EvidenceRef.parse(payload[ref_key])
            evidence = store.read_evidence(ref)
        except Exception as exc:
            raise ResearchRobustnessError("robustness_record_corrupt", f"robustness evidence {ref_key} is invalid") from exc
        if sha256(evidence).hexdigest() != payload[sha_key] or ref.digest != payload[sha_key]:
            raise ResearchRobustnessError("robustness_record_corrupt", f"robustness evidence {ref_key} binding is invalid")

    result_ref = EvidenceRef.parse(payload["result_archive_ref"])
    try:
        inspected = inspect_sqx_output_bytes(
            store.read_evidence(result_ref),
            archive_name=payload["result_archive_name"],
        )
    except (SqxOutputError, TypeError) as exc:
        raise ResearchRobustnessError("robustness_record_corrupt", "native robustness result archive is invalid") from exc
    if (
        inspected["archive_sha256"] != payload["result_archive_sha256"]
        or inspected["strategy_entry_sha256"] != payload["result_strategy_sha256"]
        or inspected["settings_entry_sha256"] != payload["result_settings_sha256"]
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
    except Exception as exc:
        raise ResearchRobustnessError("robustness_record_ref_invalid", "validation_ref is not a valid evidence identity") from exc
    payload = _read_record(store, ref)
    return {**payload, "validation_ref": str(ref)}


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
    except ResearchRetesterError as exc:
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
    except Exception as exc:
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
    except (SqxOutputError, TypeError) as exc:
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
    compiled_project_sha = plan["compiled_project_sha256"]
    assert isinstance(compiled_project_sha, str)

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

    # Recheck the installed engine at the execution boundary, matching the baseline
    # Retester trust/custody split.
    try:
        _, _, launch_engine_sha = _read_exact_inside(
            home,
            RETESTER_ENGINE_RELATIVE_PATH,
            missing_code="retester_engine_missing",
            escape_code="retester_engine_path_escape",
        )
    except ResearchRetesterError as exc:
        raise ResearchRobustnessError(exc.code, exc.detail) from exc
    if launch_engine_sha != engine_sha:
        raise ResearchRobustnessError(
            "robustness_engine_changed_before_execution",
            "installed SQTradingLib.jar changed before native robustness launch",
        )

    try:
        receipt = gateway_factory(sqx_home, trusted_launcher_sha256).launch_retester_task(
            project_name,
            expected_project_sha256=compiled_project_sha,
            expected_engine_sha256=engine_sha,
        )
    except SqxNativeGatewayError as exc:
        raise ResearchRobustnessError(exc.code, exc.detail) from exc

    if (
        receipt.get("schema") != "tc.sqx-native-control.v1"
        or receipt.get("operation") != "retester_start_task"
        or receipt.get("project") != project_name
        or receipt.get("task") != 1
        or receipt.get("state") != "submitted"
        or receipt.get("sqx_build") != SQX_BUILD
        or receipt.get("project_sha256") != compiled_project_sha
        or receipt.get("engine_sha256") != engine_sha
        or receipt.get("project_relative_path") != project_relative
        or not isinstance(receipt.get("launcher_sha256"), str)
        or not isinstance(receipt.get("receipts"), list)
    ):
        raise ResearchRobustnessError(
            "robustness_receipt_invalid",
            "native Retester gateway returned an invalid Higher Precision receipt",
        )

    try:
        _, _, completed_engine_sha = _read_exact_inside(
            home,
            RETESTER_ENGINE_RELATIVE_PATH,
            missing_code="retester_engine_missing",
            escape_code="retester_engine_path_escape",
        )
    except ResearchRetesterError as exc:
        raise ResearchRobustnessError(exc.code, exc.detail) from exc
    if completed_engine_sha != engine_sha:
        raise ResearchRobustnessError(
            "robustness_engine_changed_during_execution",
            "installed SQTradingLib.jar changed across native robustness execution",
        )

    try:
        result_bytes, result_info = _capture_result(home, project_name)
    except ResearchRetesterError as exc:
        raise ResearchRobustnessError(exc.code, exc.detail) from exc
    if result_info["archive_sha256"] == source_result_sha:
        raise ResearchRobustnessError(
            "robustness_result_unchanged",
            "native Higher Precision execution did not produce a changed SQX result archive",
        )

    result_strategy = _member(result_bytes, "strategy_Portfolio.xml")
    result_settings = _member(result_bytes, "settings.xml")
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
        "source_task_sha256": plan["source_task_sha256"],
        "compiled_task_sha256": plan["compiled_task_sha256"],
        "native_settings": plan["native_settings"],
        "engine_ref": str(engine_ref),
        "engine_sha256": engine_sha,
        "launcher_sha256": receipt["launcher_sha256"],
        "native_project_name": project_name,
        "native_project_relative_path": project_relative,
        "receipts": [dict(item) for item in receipt["receipts"]],
        "result_archive_name": result_info["archive_name"],
        "result_archive_ref": str(result_ref),
        "result_archive_sha256": result_info["archive_sha256"],
        "result_strategy_ref": str(result_strategy_ref),
        "result_strategy_sha256": result_info["strategy_entry_sha256"],
        "result_settings_ref": str(result_settings_ref),
        "result_settings_sha256": result_info["settings_entry_sha256"],
        "execution_state": "completed",
        "producer_outcome_state": ROBUSTNESS_OUTCOME_UNREAD,
    }
    record_ref = store.put_evidence(_canonical(record))
    reopened = _read_record(store, record_ref)
    return {**reopened, "validation_ref": str(record_ref)}
