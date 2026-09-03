"""Research-native Builder job custody and exact approved configuration launch.

This module binds one approved Research configuration revision to the already-proven
StrategyQuant X Builder ``loadconfig -> start`` gateway using the exact compiled
``project.cfx`` archive.  It does not implement a workflow executor, Builder
semantics, candidate generation, or result inference.
"""

from __future__ import annotations

from dataclasses import dataclass
from hashlib import sha256
import json
import os
from pathlib import Path
import re
from uuid import UUID, uuid4

from tradercockpit.research_configurations import read_current_configuration
from tradercockpit.research_custody import (
    EvidenceRef,
    FileResearchCustodyStore,
    ResearchCustodyError,
    ResearchEntityId,
    ResearchKind,
    ResearchRevisionRef,
)
from tradercockpit.sqx_gateway import SqxNativeControlGateway, SqxNativeGatewayError
from tradercockpit.sqx_presets import SQX_BUILD, SqxPresetRuntimeError, verified_sqx_home


NATIVE_JOB_CONTENT_SCHEMA = "tc.research-native-job-content.v1"
NATIVE_JOB_READ_SCHEMA = "tc.research-native-job.v1"
NATIVE_JOB_CATALOG_SCHEMA = "tc.research-native-job-catalog.v1"
NATIVE_JOB_OPERATION = "builder_loadconfig_start"
NATIVE_JOB_STAGE_RELATIVE_DIR = "user/TraderCockpit/approved-configurations"
_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")
_STAGED_CFX_RE = re.compile(
    rf"^{re.escape(NATIVE_JOB_STAGE_RELATIVE_DIR)}/[0-9a-f]{{2}}/[0-9a-f]{{64}}\.cfx$"
)
_CURRENT_POINTER_TEMP_RE = re.compile(
    r"^\.[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.json\.tmp-[0-9]+-[0-9a-f]{32}$"
)


class ResearchNativeJobError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


def _canonical(payload: dict[str, object]) -> bytes:
    return json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def _digest(value: object, *, code: str) -> str:
    if not isinstance(value, str) or not _DIGEST_RE.fullmatch(value):
        raise ResearchNativeJobError(code, "expected a lowercase 64-character SHA-256 digest")
    return value


def _configuration_revision(value: str) -> ResearchRevisionRef:
    try:
        revision = ResearchRevisionRef.parse(value)
    except ResearchCustodyError as exc:
        raise ResearchNativeJobError("native_job_configuration_invalid", "configuration revision identity is invalid") from exc
    if revision.kind != ResearchKind.CONFIGURATION:
        raise ResearchNativeJobError("native_job_configuration_invalid", "native job must bind a configuration revision")
    return revision


def _job_entity(value: ResearchEntityId | str) -> ResearchEntityId:
    try:
        entity = value if isinstance(value, ResearchEntityId) else ResearchEntityId.parse(value)
    except ResearchCustodyError as exc:
        raise ResearchNativeJobError("native_job_entity_invalid", "native job entity identity is invalid") from exc
    if entity.kind != ResearchKind.NATIVE_JOB:
        raise ResearchNativeJobError("native_job_entity_invalid", "research entity is not a native job")
    return entity


def _job_revision(value: ResearchRevisionRef | str) -> ResearchRevisionRef:
    try:
        revision = value if isinstance(value, ResearchRevisionRef) else ResearchRevisionRef.parse(value)
    except ResearchCustodyError as exc:
        raise ResearchNativeJobError("native_job_revision_invalid", "native job revision identity is invalid") from exc
    if revision.kind != ResearchKind.NATIVE_JOB:
        raise ResearchNativeJobError("native_job_revision_invalid", "research revision is not a native job revision")
    return revision


def _receipt_tuple(value: object) -> tuple[dict[str, object], ...]:
    if not isinstance(value, list):
        raise ResearchNativeJobError("native_job_content_corrupt", "native control receipts are invalid")
    receipts: list[dict[str, object]] = []
    for item in value:
        if not isinstance(item, dict):
            raise ResearchNativeJobError("native_job_content_corrupt", "native control receipt is invalid")
        expected = {
            "sequence",
            "action",
            "project",
            "state",
            "exit_code",
            "sqx_build",
            "launcher_sha256",
            "config_sha256",
            "reason_code",
        }
        if set(item) != expected:
            raise ResearchNativeJobError("native_job_content_corrupt", "native control receipt shape is invalid")
        receipts.append(dict(item))
    return tuple(receipts)


@dataclass(frozen=True, slots=True)
class NativeBuilderJobContent:
    state: str
    configuration_entity_id: str
    configuration_revision: str
    executable_xml_ref: EvidenceRef
    executable_xml_sha256: str
    sqx_build: str
    operation: str
    staged_config_relative_path: str
    launcher_sha256: str | None
    partial_side_effect: bool
    receipts: tuple[dict[str, object], ...]
    failure_reason_code: str | None = None

    def __post_init__(self) -> None:
        if self.state not in {"prepared", "submitted", "failed"}:
            raise ResearchNativeJobError("native_job_state_invalid", "native job state is invalid")
        try:
            entity = ResearchEntityId.parse(self.configuration_entity_id)
        except ResearchCustodyError as exc:
            raise ResearchNativeJobError("native_job_configuration_invalid", "configuration entity identity is invalid") from exc
        if entity.kind != ResearchKind.CONFIGURATION:
            raise ResearchNativeJobError("native_job_configuration_invalid", "native job must bind a configuration entity")
        _configuration_revision(self.configuration_revision)
        if not isinstance(self.executable_xml_ref, EvidenceRef):
            raise ResearchNativeJobError("native_job_executable_invalid", "native job executable evidence is invalid")
        if _digest(self.executable_xml_sha256, code="native_job_executable_invalid") != self.executable_xml_ref.digest:
            raise ResearchNativeJobError("native_job_executable_invalid", "native job executable digest does not match evidence")
        if self.sqx_build != SQX_BUILD or self.operation != NATIVE_JOB_OPERATION:
            raise ResearchNativeJobError("native_job_control_invalid", "native job control identity is invalid")
        if not _STAGED_CFX_RE.fullmatch(self.staged_config_relative_path):
            raise ResearchNativeJobError("native_job_stage_invalid", "native job staged configuration path is invalid")
        if self.launcher_sha256 is not None:
            _digest(self.launcher_sha256, code="native_job_launcher_invalid")
        if not isinstance(self.partial_side_effect, bool):
            raise ResearchNativeJobError("native_job_content_corrupt", "partial side-effect state is invalid")
        if any(not isinstance(item, dict) for item in self.receipts):
            raise ResearchNativeJobError("native_job_content_corrupt", "native control receipts are invalid")
        if self.state == "prepared":
            if self.receipts or self.partial_side_effect or self.failure_reason_code is not None or self.launcher_sha256 is not None:
                raise ResearchNativeJobError("native_job_content_corrupt", "prepared native job contains execution outcome")
        elif self.state == "submitted":
            if self.failure_reason_code is not None or self.partial_side_effect or len(self.receipts) != 2:
                raise ResearchNativeJobError("native_job_content_corrupt", "submitted native job outcome is inconsistent")
            if self.launcher_sha256 is None or any(item.get("state") != "completed" for item in self.receipts):
                raise ResearchNativeJobError("native_job_content_corrupt", "submitted native job receipts are incomplete")
        else:
            if not isinstance(self.failure_reason_code, str) or not self.failure_reason_code or not self.receipts:
                raise ResearchNativeJobError("native_job_content_corrupt", "failed native job requires a structured refusal")

    def canonical_bytes(self) -> bytes:
        return _canonical(
            {
                "configuration_entity_id": self.configuration_entity_id,
                "configuration_revision": self.configuration_revision,
                "executable_xml_ref": str(self.executable_xml_ref),
                "executable_xml_sha256": self.executable_xml_sha256,
                "failure_reason_code": self.failure_reason_code,
                "launcher_sha256": self.launcher_sha256,
                "operation": self.operation,
                "partial_side_effect": self.partial_side_effect,
                "receipts": [dict(item) for item in self.receipts],
                "schema": NATIVE_JOB_CONTENT_SCHEMA,
                "sqx_build": self.sqx_build,
                "staged_config_relative_path": self.staged_config_relative_path,
                "state": self.state,
            }
        )

    @classmethod
    def from_bytes(cls, data: bytes) -> "NativeBuilderJobContent":
        try:
            payload = json.loads(data)
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise ResearchNativeJobError("native_job_content_corrupt", "native job content is not valid JSON") from exc
        expected = {
            "configuration_entity_id",
            "configuration_revision",
            "executable_xml_ref",
            "executable_xml_sha256",
            "failure_reason_code",
            "launcher_sha256",
            "operation",
            "partial_side_effect",
            "receipts",
            "schema",
            "sqx_build",
            "staged_config_relative_path",
            "state",
        }
        if not isinstance(payload, dict) or set(payload) != expected or payload.get("schema") != NATIVE_JOB_CONTENT_SCHEMA:
            raise ResearchNativeJobError("native_job_content_corrupt", "native job content schema is invalid")
        try:
            return cls(
                state=payload["state"],
                configuration_entity_id=payload["configuration_entity_id"],
                configuration_revision=payload["configuration_revision"],
                executable_xml_ref=EvidenceRef.parse(payload["executable_xml_ref"]),
                executable_xml_sha256=payload["executable_xml_sha256"],
                sqx_build=payload["sqx_build"],
                operation=payload["operation"],
                staged_config_relative_path=payload["staged_config_relative_path"],
                launcher_sha256=payload["launcher_sha256"],
                partial_side_effect=payload["partial_side_effect"],
                receipts=_receipt_tuple(payload["receipts"]),
                failure_reason_code=payload["failure_reason_code"],
            )
        except (KeyError, TypeError, ResearchCustodyError, ResearchNativeJobError) as exc:
            detail = getattr(exc, "detail", "native job content fields are invalid")
            raise ResearchNativeJobError("native_job_content_corrupt", str(detail)) from exc


def _stage_path(home: Path, digest: str) -> tuple[Path, str]:
    relative = f"{NATIVE_JOB_STAGE_RELATIVE_DIR}/{digest[:2]}/{digest}.cfx"
    target = home / relative
    parent = target.parent
    try:
        parent.mkdir(parents=True, exist_ok=True)
        resolved_parent = parent.resolve(strict=True)
        resolved_parent.relative_to(home)
    except (OSError, RuntimeError, ValueError) as exc:
        raise ResearchNativeJobError("native_job_stage_path_escape", "native staging directory escapes the verified SQX runtime") from exc
    if not resolved_parent.is_dir():
        raise ResearchNativeJobError("native_job_stage_invalid", "native staging parent is not a directory")
    return resolved_parent / target.name, relative


def _stage_exact_approved_xml(sqx_home: Path | str | None, xml_bytes: bytes, digest: str) -> tuple[Path, str]:
    try:
        home = verified_sqx_home(sqx_home)
    except SqxPresetRuntimeError as exc:
        raise ResearchNativeJobError(exc.code, exc.detail) from exc
    target, relative = _stage_path(home, digest)
    if target.exists():
        try:
            resolved = target.resolve(strict=True)
            resolved.relative_to(home)
            existing = resolved.read_bytes()
        except (OSError, RuntimeError, ValueError) as exc:
            raise ResearchNativeJobError("native_job_stage_invalid", "existing native staged configuration is invalid") from exc
        if resolved != target or not resolved.is_file() or sha256(existing).hexdigest() != digest or existing != xml_bytes:
            raise ResearchNativeJobError("native_job_stage_conflict", "existing staged configuration does not match approved bytes")
        return resolved, relative

    temporary = target.with_name(f".{target.name}.tmp-{os.getpid()}-{uuid4().hex}")
    try:
        with temporary.open("xb") as handle:
            handle.write(xml_bytes)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary, target)
        resolved = target.resolve(strict=True)
        resolved.relative_to(home)
        staged = resolved.read_bytes()
    except (OSError, RuntimeError, ValueError) as exc:
        raise ResearchNativeJobError("native_job_stage_failed", "approved configuration could not be staged inside SQX") from exc
    finally:
        try:
            temporary.unlink()
        except FileNotFoundError:
            pass
    if resolved != target or not resolved.is_file() or staged != xml_bytes or sha256(staged).hexdigest() != digest:
        raise ResearchNativeJobError("native_job_stage_corrupt", "staged configuration failed exact-byte verification")
    return resolved, relative


def _current_native_job_entities(store: FileResearchCustodyStore) -> tuple[ResearchEntityId, ...]:
    directory = store.base / "current" / ResearchKind.NATIVE_JOB.value
    if not directory.exists():
        return ()
    if not directory.is_dir():
        raise ResearchCustodyError("current_pointer_corrupt", "native-job current-pointer directory is invalid")
    entities: list[ResearchEntityId] = []
    for path in sorted(directory.iterdir(), key=lambda item: item.name):
        if _CURRENT_POINTER_TEMP_RE.fullmatch(path.name):
            continue
        if not path.is_file() or path.suffix != ".json":
            raise ResearchCustodyError("current_pointer_corrupt", "native-job current-pointer directory contains an unexpected entry")
        try:
            value = UUID(path.stem)
        except ValueError as exc:
            raise ResearchCustodyError("current_pointer_corrupt", "native-job current-pointer filename is not a canonical UUID") from exc
        if str(value) != path.stem:
            raise ResearchCustodyError("current_pointer_corrupt", "native-job current-pointer UUID is not canonical")
        entity = ResearchEntityId(ResearchKind.NATIVE_JOB, value)
        store.current(entity)
        entities.append(entity)
    return tuple(entities)


def _record(store: FileResearchCustodyStore, entity: ResearchEntityId, revision: ResearchRevisionRef) -> dict[str, object]:
    stored = store.read_revision(revision)
    if stored.entity_id != entity:
        raise ResearchNativeJobError("native_job_revision_invalid", "native job revision belongs to another entity")
    content = NativeBuilderJobContent.from_bytes(store.read_revision_content(revision))
    xml_bytes = store.read_evidence(content.executable_xml_ref)
    if sha256(xml_bytes).hexdigest() != content.executable_xml_sha256:
        raise ResearchNativeJobError("native_job_content_corrupt", "native job executable evidence identity is invalid")
    if set(stored.evidence) != {content.executable_xml_ref}:
        raise ResearchNativeJobError("native_job_content_corrupt", "native job revision evidence binding is invalid")
    if content.state == "prepared":
        if stored.parent_revision is not None:
            raise ResearchNativeJobError("native_job_content_corrupt", "prepared native job cannot have a parent revision")
    else:
        if stored.parent_revision is None:
            raise ResearchNativeJobError("native_job_content_corrupt", "native job outcome must preserve its prepared parent")
        parent = NativeBuilderJobContent.from_bytes(store.read_revision_content(stored.parent_revision))
        if (
            parent.state != "prepared"
            or parent.configuration_entity_id != content.configuration_entity_id
            or parent.configuration_revision != content.configuration_revision
            or parent.executable_xml_ref != content.executable_xml_ref
            or parent.staged_config_relative_path != content.staged_config_relative_path
        ):
            raise ResearchNativeJobError("native_job_content_corrupt", "native job outcome does not match its prepared control identity")
    return {
        "schema": NATIVE_JOB_READ_SCHEMA,
        "entity_id": str(entity),
        "revision": str(revision),
        "parent_revision": str(stored.parent_revision) if stored.parent_revision else None,
        "state": content.state,
        "configuration_entity_id": content.configuration_entity_id,
        "configuration_revision": content.configuration_revision,
        "executable_xml_ref": str(content.executable_xml_ref),
        "executable_xml_sha256": content.executable_xml_sha256,
        "sqx_build": content.sqx_build,
        "operation": content.operation,
        "staged_config_relative_path": content.staged_config_relative_path,
        "launcher_sha256": content.launcher_sha256,
        "partial_side_effect": content.partial_side_effect,
        "failure_reason_code": content.failure_reason_code,
        "receipts": [dict(item) for item in content.receipts],
    }


def list_current_native_jobs(store: FileResearchCustodyStore, configuration_revision: str | None = None) -> dict[str, object]:
    selected_revision = _configuration_revision(configuration_revision) if configuration_revision else None
    jobs: list[dict[str, object]] = []
    for entity in _current_native_job_entities(store):
        record = _record(store, entity, store.current(entity))
        if selected_revision is not None and record["configuration_revision"] != str(selected_revision):
            continue
        jobs.append(record)
    return {"schema": NATIVE_JOB_CATALOG_SCHEMA, "jobs": jobs}


def read_current_native_job(store: FileResearchCustodyStore, entity_id: ResearchEntityId | str) -> dict[str, object]:
    entity = _job_entity(entity_id)
    return _record(store, entity, store.current(entity))


def read_native_job_revision(
    store: FileResearchCustodyStore,
    entity_id: ResearchEntityId | str,
    revision: ResearchRevisionRef | str,
) -> dict[str, object]:
    entity = _job_entity(entity_id)
    exact_revision = _job_revision(revision)
    return _record(store, entity, exact_revision)


def _existing_job_for_configuration(store: FileResearchCustodyStore, configuration_revision: str) -> dict[str, object] | None:
    catalog = list_current_native_jobs(store, configuration_revision)
    jobs = catalog["jobs"]
    if not isinstance(jobs, list):
        raise ResearchNativeJobError("native_job_content_corrupt", "native job catalog is invalid")
    if len(jobs) > 1:
        raise ResearchNativeJobError("native_job_duplicate", "multiple native jobs bind the same approved configuration revision")
    return jobs[0] if jobs else None


def launch_approved_builder_configuration(
    store: FileResearchCustodyStore,
    sqx_home: Path | str | None,
    trusted_launcher_sha256: str | None,
    *,
    configuration_entity_id: str,
    expected_configuration_revision: str,
    gateway_factory=SqxNativeControlGateway,
    register_worker=None,
) -> dict[str, object]:
    """Stage and submit one exact approved configuration through the trusted gateway.

    A retry for the same approved revision returns the existing native-job record and
    never submits a second native start sequence.
    """

    record = read_current_configuration(store, configuration_entity_id)
    if record.get("revision") != expected_configuration_revision:
        raise ResearchCustodyError("current_conflict", "configuration revision changed before native launch")
    if record.get("state") != "approved" or record.get("approval", {}).get("approved") is not True:
        raise ResearchNativeJobError("native_job_configuration_unapproved", "native launch requires the exact current approved configuration")

    existing = _existing_job_for_configuration(store, expected_configuration_revision)
    if existing is not None:
        return {**existing, "reused": True}

    executable_ref = EvidenceRef.parse(record["executable_xml_ref"])
    executable_sha = _digest(record["executable_xml_sha256"], code="native_job_executable_invalid")
    xml_bytes = store.read_evidence(executable_ref)
    if EvidenceRef.from_bytes(xml_bytes) != executable_ref or sha256(xml_bytes).hexdigest() != executable_sha:
        raise ResearchNativeJobError("native_job_executable_invalid", "approved executable evidence failed exact-byte verification")

    try:
        source_project_ref = EvidenceRef.parse(record["source_project_ref"])
    except (KeyError, TypeError, ResearchCustodyError) as exc:
        raise ResearchNativeJobError(
            "native_job_source_invalid",
            "approved configuration must bind the compiled Builder project.cfx",
        ) from exc
    source_project_sha = _digest(record.get("source_project_sha256"), code="native_job_source_invalid")
    if source_project_ref.digest != source_project_sha:
        raise ResearchNativeJobError("native_job_source_invalid", "compiled project evidence does not match its SHA-256")
    project_bytes = store.read_evidence(source_project_ref)
    if sha256(project_bytes).hexdigest() != source_project_sha:
        raise ResearchNativeJobError("native_job_source_invalid", "compiled project evidence failed exact-byte verification")

    staged_path, staged_relative = _stage_exact_approved_xml(sqx_home, project_bytes, source_project_sha)
    job_entity = store.create_entity(ResearchKind.NATIVE_JOB)
    prepared = NativeBuilderJobContent(
        state="prepared",
        configuration_entity_id=configuration_entity_id,
        configuration_revision=expected_configuration_revision,
        executable_xml_ref=executable_ref,
        executable_xml_sha256=executable_sha,
        sqx_build=record["sqx_build"],
        operation=NATIVE_JOB_OPERATION,
        staged_config_relative_path=staged_relative,
        launcher_sha256=None,
        partial_side_effect=False,
        receipts=(),
    )
    prepared_revision = store.create_revision(job_entity, prepared.canonical_bytes(), evidence=(executable_ref,))
    store.compare_and_set_current(job_entity, expected_revision=None, target_revision=prepared_revision.revision)

    try:
        receipt = gateway_factory(
            sqx_home,
            trusted_launcher_sha256,
            register_worker=register_worker,
        ).launch_builder(
            staged_path,
            expected_config_sha256=source_project_sha,
        )
    except SqxNativeGatewayError as exc:
        error_model = exc.read_model()
        failed = NativeBuilderJobContent(
            state="failed",
            configuration_entity_id=configuration_entity_id,
            configuration_revision=expected_configuration_revision,
            executable_xml_ref=executable_ref,
            executable_xml_sha256=executable_sha,
            sqx_build=record["sqx_build"],
            operation=NATIVE_JOB_OPERATION,
            staged_config_relative_path=staged_relative,
            launcher_sha256=next(
                (item.get("launcher_sha256") for item in reversed(error_model["receipts"]) if item.get("launcher_sha256")),
                None,
            ),
            partial_side_effect=bool(error_model["partial_side_effect"]),
            receipts=tuple(dict(item) for item in error_model["receipts"]),
            failure_reason_code=exc.code,
        )
        failed_revision = store.create_revision(
            job_entity,
            failed.canonical_bytes(),
            parent_revision=prepared_revision.revision,
            evidence=(executable_ref,),
        )
        store.compare_and_set_current(
            job_entity,
            expected_revision=prepared_revision.revision,
            target_revision=failed_revision.revision,
        )
        raise ResearchNativeJobError(exc.code, exc.detail) from exc

    if (
        receipt.get("schema") != "tc.sqx-native-control.v1"
        or receipt.get("operation") != NATIVE_JOB_OPERATION
        or receipt.get("state") != "submitted"
        or receipt.get("sqx_build") != SQX_BUILD
        or receipt.get("config_sha256") != source_project_sha
        or receipt.get("config_relative_path") != staged_relative
        or not isinstance(receipt.get("launcher_sha256"), str)
        or not isinstance(receipt.get("receipts"), list)
    ):
        raise ResearchNativeJobError("native_job_receipt_invalid", "native gateway returned an invalid success receipt")

    submitted = NativeBuilderJobContent(
        state="submitted",
        configuration_entity_id=configuration_entity_id,
        configuration_revision=expected_configuration_revision,
        executable_xml_ref=executable_ref,
        executable_xml_sha256=executable_sha,
        sqx_build=record["sqx_build"],
        operation=NATIVE_JOB_OPERATION,
        staged_config_relative_path=staged_relative,
        launcher_sha256=_digest(receipt["launcher_sha256"], code="native_job_launcher_invalid"),
        partial_side_effect=False,
        receipts=tuple(dict(item) for item in receipt["receipts"]),
    )
    submitted_revision = store.create_revision(
        job_entity,
        submitted.canonical_bytes(),
        parent_revision=prepared_revision.revision,
        evidence=(executable_ref,),
    )
    store.compare_and_set_current(
        job_entity,
        expected_revision=prepared_revision.revision,
        target_revision=submitted_revision.revision,
    )
    return {**_record(store, job_entity, submitted_revision.revision), "reused": False}
