"""Native SQX Monte Carlo trade-manipulation execution and immutable custody.

TraderCockpit does not implement Monte Carlo behavior. The current installed
Retester ``project.cfx`` is the executable specification. This adapter requires
one producer-owned ``MonteCarloManipulation`` cross-check profile, preserves its
saved methods, parameters, simulation count, and acceptance subtree, enables
only that existing profile in an isolated project snapshot when necessary, and
executes it through the trusted Retester task-1 gateway.

Process completion proves execution and custody only. Until an authoritative
SQX outcome seam is observed, no robustness pass/fail verdict is inferred.
"""

from __future__ import annotations

from hashlib import sha256
from io import BytesIO
import json
from pathlib import Path
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
    _validate_retester_project,
    read_current_historical_result,
)
from tradercockpit.research_robustness import (
    ROBUSTNESS_OPERATION,
    ROBUSTNESS_OUTCOME_UNREAD,
    ROBUSTNESS_RECORD_SCHEMA,
    ResearchRobustnessError,
    _canonical,
    _digest,
    _stage_workspace,
    _zip_member,
)
from tradercockpit.sqx_gateway import SqxNativeControlGateway, SqxNativeGatewayError
from tradercockpit.sqx_outputs import SqxOutputError, inspect_sqx_output_bytes
from tradercockpit.sqx_presets import SQX_BUILD, SqxPresetRuntimeError, verified_sqx_home


ROBUSTNESS_METHOD_MONTE_CARLO_MANIPULATION = "MonteCarloManipulation"


def _local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def _exact_child(parent: ElementTree.Element, name: str, code: str) -> ElementTree.Element:
    matches = [child for child in parent if _local_name(child.tag) == name]
    if len(matches) != 1:
        raise ResearchRobustnessError(code, f"native Monte Carlo profile requires exactly one {name} element")
    return matches[0]


def _text(parent: ElementTree.Element, name: str, code: str) -> str:
    node = _exact_child(parent, name, code)
    value = (node.text or "").strip()
    if not value:
        raise ResearchRobustnessError(code, f"native Monte Carlo {name} value is empty")
    return value


def _method_settings(methods: ElementTree.Element) -> list[dict[str, object]]:
    values: list[dict[str, object]] = []
    for child in methods:
        if _local_name(child.tag) != "Method":
            raise ResearchRobustnessError(
                "robustness_monte_carlo_invalid",
                "native Monte Carlo Methods contains a non-Method element",
            )
        method_type = child.attrib.get("type")
        use = child.attrib.get("use")
        if not isinstance(method_type, str) or not method_type:
            raise ResearchRobustnessError(
                "robustness_monte_carlo_invalid",
                "native Monte Carlo method type is missing",
            )
        if use not in {"true", "false"}:
            raise ResearchRobustnessError(
                "robustness_monte_carlo_invalid",
                "native Monte Carlo method use attribute is invalid",
            )
        params = _exact_child(child, "Params", "robustness_monte_carlo_invalid")
        parsed_params: list[dict[str, str]] = []
        for param in params:
            if _local_name(param.tag) != "Param":
                raise ResearchRobustnessError(
                    "robustness_monte_carlo_invalid",
                    "native Monte Carlo Params contains a non-Param element",
                )
            key = param.attrib.get("key")
            value_type = param.attrib.get("type")
            value = (param.text or "").strip()
            if not isinstance(key, str) or not key or not isinstance(value_type, str) or not value_type or not value:
                raise ResearchRobustnessError(
                    "robustness_monte_carlo_invalid",
                    "native Monte Carlo parameter identity or value is invalid",
                )
            parsed_params.append({"key": key, "type": value_type, "value": value})
        values.append({"type": method_type, "use": use, "params": parsed_params})
    if not values:
        raise ResearchRobustnessError(
            "robustness_monte_carlo_invalid",
            "native Monte Carlo Methods is empty",
        )
    return values


def _task_profile(
    task_bytes: bytes,
    *,
    require_enabled: bool,
) -> tuple[ElementTree.Element, ElementTree.Element, dict[str, object]]:
    try:
        root = ElementTree.fromstring(task_bytes)
    except (ElementTree.ParseError, LookupError, ValueError) as exc:
        raise ResearchRobustnessError(
            "robustness_monte_carlo_source_task_invalid",
            "native Retester task-1 settings are not valid XML",
        ) from exc
    if _local_name(root.tag) != "Settings":
        raise ResearchRobustnessError(
            "robustness_monte_carlo_source_task_invalid",
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
        if _local_name(child.tag) == ROBUSTNESS_METHOD_MONTE_CARLO_MANIPULATION
    ]
    if len(profiles) != 1:
        raise ResearchRobustnessError(
            "robustness_monte_carlo_missing",
            "installed Retester project does not contain one Monte Carlo trade-manipulation profile; configure/save it in SQX first",
        )
    target = profiles[0]
    use = target.attrib.get("use")
    if use not in {"true", "false"}:
        raise ResearchRobustnessError(
            "robustness_monte_carlo_invalid",
            "native Monte Carlo profile has an invalid use attribute",
        )
    if require_enabled and use != "true":
        raise ResearchRobustnessError(
            "robustness_compiled_task_invalid",
            "compiled Monte Carlo trade-manipulation profile is not enabled",
        )

    active_others = [
        _local_name(child.tag)
        for child in cross_checks_node
        if child is not target and child.attrib.get("use") == "true"
    ]
    if active_others:
        raise ResearchRobustnessError(
            "robustness_other_crosscheck_enabled",
            "Monte Carlo trade-manipulation isolation requires all other native cross-checks disabled: "
            + ", ".join(active_others),
        )

    settings = _exact_child(target, "Settings", "robustness_monte_carlo_invalid")
    _exact_child(target, "AcceptanceSettings", "robustness_monte_carlo_invalid")
    methods = _exact_child(settings, "Methods", "robustness_monte_carlo_invalid")
    simulations = _text(settings, "NumberOfSimulations", "robustness_monte_carlo_invalid")
    if not simulations.isascii() or not simulations.isdigit() or int(simulations, 10) < 1:
        raise ResearchRobustnessError(
            "robustness_monte_carlo_invalid",
            "native NumberOfSimulations must be positive unsigned decimal text",
        )
    values: dict[str, object] = {
        "NumberOfSimulations": simulations,
        "Methods": _method_settings(methods),
    }
    return root, target, values


def _rewrite_task(task_bytes: bytes) -> tuple[bytes, dict[str, object], bool]:
    root, target, settings = _task_profile(task_bytes, require_enabled=False)
    if target.attrib.get("use") == "true":
        return task_bytes, settings, False
    target.set("use", "true")
    compiled = ElementTree.tostring(root, encoding="utf-8", xml_declaration=True)
    _, _, compiled_settings = _task_profile(compiled, require_enabled=True)
    if compiled_settings != settings:
        raise ResearchRobustnessError(
            "robustness_compiled_project_invalid",
            "compiled Monte Carlo internal settings changed unexpectedly",
        )
    return compiled, settings, True


def compile_monte_carlo_manipulation_project(project_bytes: bytes) -> tuple[bytes, dict[str, object]]:
    """Enable only the installed native MonteCarloManipulation profile."""

    try:
        _validate_retester_project(project_bytes)
    except ResearchRetesterError as exc:
        raise ResearchRobustnessError(exc.code, exc.detail) from exc

    source_task = _zip_member(project_bytes, RETESTER_PROJECT_TASK_ENTRY, "robustness_source_project_invalid")
    compiled_task, settings, changed = _rewrite_task(source_task)
    if not changed:
        compiled_project = project_bytes
    else:
        try:
            output_stream = BytesIO()
            with ZipFile(BytesIO(project_bytes)) as source:
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
            "Monte Carlo compilation produced an empty native project snapshot",
        )
    try:
        _validate_retester_project(compiled_project)
    except ResearchRetesterError as exc:
        raise ResearchRobustnessError("robustness_compiled_project_invalid", exc.detail) from exc
    compiled_task_check = _zip_member(compiled_project, RETESTER_PROJECT_TASK_ENTRY, "robustness_compiled_project_invalid")
    _, _, compiled_settings = _task_profile(compiled_task_check, require_enabled=True)
    if compiled_settings != settings:
        raise ResearchRobustnessError(
            "robustness_compiled_project_invalid",
            "compiled Monte Carlo settings changed unexpectedly",
        )

    return compiled_project, {
        "method": ROBUSTNESS_METHOD_MONTE_CARLO_MANIPULATION,
        "native_settings": settings,
        "configuration_changed": changed,
        "source_task_sha256": sha256(source_task).hexdigest(),
        "compiled_task_sha256": sha256(compiled_task_check).hexdigest(),
        "source_project_sha256": sha256(project_bytes).hexdigest(),
        "compiled_project_sha256": sha256(compiled_project).hexdigest(),
    }


def _record_identity(payload: dict[str, object]) -> None:
    if payload.get("sqx_build") != SQX_BUILD:
        raise ResearchRobustnessError("robustness_record_corrupt", "robustness SQX build identity is invalid")
    if payload.get("operation") != ROBUSTNESS_OPERATION:
        raise ResearchRobustnessError("robustness_record_corrupt", "robustness operation identity is invalid")
    if payload.get("method") != ROBUSTNESS_METHOD_MONTE_CARLO_MANIPULATION:
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
        "schema", "sqx_build", "operation", "method",
        "source_historical_result_entity_id", "source_historical_result_revision",
        "source_result_archive_ref", "source_result_archive_sha256",
        "source_project_ref", "source_project_sha256", "compiled_project_ref", "compiled_project_sha256",
        "configuration_changed", "source_task_sha256", "compiled_task_sha256", "native_settings",
        "engine_ref", "engine_sha256", "launcher_sha256", "native_project_name", "native_project_relative_path",
        "receipts", "result_archive_name", "result_archive_ref", "result_archive_sha256",
        "result_strategy_ref", "result_strategy_sha256", "result_settings_ref", "result_settings_sha256",
        "execution_state", "producer_outcome_state",
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
        raise ResearchRobustnessError("robustness_record_corrupt", "compiled Monte Carlo settings do not match custody")
    if payload["configuration_changed"] is False and source_project != compiled_project:
        raise ResearchRobustnessError("robustness_record_corrupt", "unchanged configuration does not preserve exact source project bytes")
    if payload["configuration_changed"] is True and source_project == compiled_project:
        raise ResearchRobustnessError("robustness_record_corrupt", "changed configuration is byte-identical to source project")

    try:
        inspected = inspect_sqx_output_bytes(evidence["result_archive_ref"], archive_name=payload["result_archive_name"])
    except (SqxOutputError, TypeError) as exc:
        raise ResearchRobustnessError("robustness_record_corrupt", "native robustness result archive is invalid") from exc
    if (
        inspected["archive_sha256"] != payload["result_archive_sha256"]
        or inspected["strategy_entry_sha256"] != payload["result_strategy_sha256"]
        or inspected["settings_entry_sha256"] != payload["result_settings_sha256"]
        or payload["result_archive_sha256"] == payload["source_result_archive_sha256"]
    ):
        raise ResearchRobustnessError("robustness_record_corrupt", "native robustness result members do not match custody")
    return payload


def read_native_monte_carlo_manipulation_result(
    store: FileResearchCustodyStore,
    validation_ref: str | EvidenceRef,
) -> dict[str, object]:
    try:
        ref = validation_ref if isinstance(validation_ref, EvidenceRef) else EvidenceRef.parse(validation_ref)
    except (ResearchCustodyError, TypeError) as exc:
        raise ResearchRobustnessError("robustness_record_ref_invalid", "validation_ref is not a valid evidence identity") from exc
    payload = _read_record(store, ref)
    return {**payload, "validation_ref": str(ref)}


def start_native_monte_carlo_manipulation(
    store: FileResearchCustodyStore,
    sqx_home: Path | str | None,
    trusted_launcher_sha256: str | None,
    *,
    historical_result_entity_id: str,
    expected_historical_result_revision: str,
    gateway_factory=SqxNativeControlGateway,
) -> dict[str, object]:
    """Run installed SQX Monte Carlo trade manipulation on one Historical Result."""

    try:
        historical = read_current_historical_result(store, historical_result_entity_id)
    except (ResearchRetesterError, ResearchCustodyError) as exc:
        raise ResearchRobustnessError(exc.code, exc.detail) from exc
    if historical.get("revision") != expected_historical_result_revision:
        raise ResearchRobustnessError("robustness_source_revision_changed", "Historical Result revision changed before robustness execution")
    if historical.get("state") != "completed" or historical.get("execution_completed") is not True:
        raise ResearchRobustnessError("robustness_source_result_incomplete", "Monte Carlo requires one completed native Historical Result")
    if historical.get("sqx_build") != SQX_BUILD:
        raise ResearchRobustnessError("robustness_source_build_mismatch", "Historical Result SQX build does not match the native runtime contract")

    try:
        source_result_ref = EvidenceRef.parse(historical["result_archive_ref"])
        source_result_bytes = store.read_evidence(source_result_ref)
    except (KeyError, ResearchCustodyError, TypeError) as exc:
        raise ResearchRobustnessError("robustness_source_result_invalid", "Historical Result archive evidence is invalid") from exc
    source_result_sha = sha256(source_result_bytes).hexdigest()
    if source_result_sha != historical.get("result_archive_sha256") or source_result_ref.digest != source_result_sha:
        raise ResearchRobustnessError("robustness_source_result_invalid", "Historical Result archive evidence binding is invalid")
    try:
        source_info = inspect_sqx_output_bytes(source_result_bytes, archive_name=historical["result_archive_name"])
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

    compiled_project_bytes, plan = compile_monte_carlo_manipulation_project(source_project_bytes)
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
        raise ResearchRobustnessError("robustness_engine_changed_before_execution", "installed SQTradingLib.jar changed before native robustness launch")

    try:
        receipt = gateway_factory(sqx_home, trusted_launcher_sha256).launch_retester_task(
            project_name,
            expected_project_sha256=compiled_project_sha,
            expected_engine_sha256=engine_sha,
        )
    except SqxNativeGatewayError as exc:
        raise ResearchRobustnessError(exc.code, exc.detail) from exc

    receipts = receipt.get("receipts")
    launcher_sha = _digest(receipt.get("launcher_sha256"), "robustness_receipt_invalid")
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
        or not isinstance(receipts, list)
        or len(receipts) != 1
        or not isinstance(receipts[0], dict)
        or receipts[0].get("action") != "startOnlyTask"
        or receipts[0].get("project") != project_name
        or receipts[0].get("task") != 1
        or receipts[0].get("state") != "completed"
        or receipts[0].get("launcher_sha256") != launcher_sha
        or receipts[0].get("project_sha256") != compiled_project_sha
        or receipts[0].get("engine_sha256") != engine_sha
    ):
        raise ResearchRobustnessError("robustness_receipt_invalid", "native Retester gateway returned an invalid Monte Carlo receipt")

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
        raise ResearchRobustnessError("robustness_engine_changed_during_execution", "installed SQTradingLib.jar changed across native robustness execution")

    try:
        result_bytes, result_info = _capture_result(home, project_name)
    except ResearchRetesterError as exc:
        raise ResearchRobustnessError(exc.code, exc.detail) from exc
    if result_info["archive_sha256"] == source_result_sha:
        raise ResearchRobustnessError("robustness_result_unchanged", "native Monte Carlo execution did not produce a changed SQX result archive")

    result_strategy = _member(result_bytes, "strategy_Portfolio.xml")
    result_settings = _member(result_bytes, "settings.xml")
    result_ref = store.put_evidence(result_bytes)
    result_strategy_ref = store.put_evidence(result_strategy)
    result_settings_ref = store.put_evidence(result_settings)

    record = {
        "schema": ROBUSTNESS_RECORD_SCHEMA,
        "sqx_build": SQX_BUILD,
        "operation": ROBUSTNESS_OPERATION,
        "method": ROBUSTNESS_METHOD_MONTE_CARLO_MANIPULATION,
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
    record_ref = store.put_evidence(_canonical(record))
    reopened = _read_record(store, record_ref)
    return {**reopened, "validation_ref": str(record_ref)}
