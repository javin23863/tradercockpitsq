"""Research-native Builder job custody and exact approved configuration launch.

This module binds one approved Research configuration revision to the already-proven
StrategyQuant X Builder ``loadconfig -> start`` gateway.  It does not implement a
workflow executor, Builder semantics, candidate generation, or result inference.
"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from hashlib import sha256
from io import BytesIO
import json
import os
from pathlib import Path
import re
import subprocess
from threading import Lock
from typing import Callable
from uuid import UUID, uuid4
from zipfile import ZIP_DEFLATED, BadZipFile, ZipFile, ZipInfo

from tradercockpit.research_configurations import read_current_configuration
from tradercockpit.research_custody import (
    EvidenceRef,
    FileResearchCustodyStore,
    ResearchCustodyError,
    ResearchEntityId,
    ResearchKind,
    ResearchRevisionRef,
)
from tradercockpit.sqx_builder_config import SQX_BUILDER_TASK_ENTRY
from tradercockpit.sqx_gateway import SqxBuilderWorker, SqxNativeControlGateway, SqxNativeGatewayError
from tradercockpit.sqx_presets import SQX_BUILD, SqxPresetRuntimeError, verified_sqx_home


NATIVE_JOB_CONTENT_SCHEMA = "tc.research-native-job-content.v1"
NATIVE_JOB_READ_SCHEMA = "tc.research-native-job.v1"
NATIVE_JOB_CATALOG_SCHEMA = "tc.research-native-job-catalog.v1"
NATIVE_JOB_OPERATION = "builder_loadconfig_start"
NATIVE_JOB_STAGE_RELATIVE_DIR = "user/TraderCockpit/approved-configurations"
NATIVE_JOB_WORKER_LOG_DIR = "native-worker-logs"
NATIVE_JOB_STOP_TIMEOUT_SECONDS = 180.0
# sqcli prints this when a start/stop run finished and it synchronised databanks to files.
_WORKER_FINISHED_MARKER = "All tasks completed"
_WORKER_KEYS = {"pid", "http_port", "log_path", "started_at"}
_COMPLETION_KEYS = {"exit_code", "finished_at", "stop_requested", "log_ref"}
_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")
_CURRENT_POINTER_TEMP_RE = re.compile(
    r"^\.[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.json\.tmp-[0-9]+-[0-9a-f]{32}$"
)
# sqcli 144.2953 loadconfig opens file= as a zip and requires a Task root (observed saveconfig).
_SELF_CLOSING_BUILD_TASK_RE = re.compile(
    rb"<Task\b(?=[^>]*\btaskXMLFile=\"Build-Task1\.xml\")[^>]*/>"
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
    worker: dict[str, object] | None = None
    completion: dict[str, object] | None = None

    def __post_init__(self) -> None:
        if self.state not in {"prepared", "submitted", "running", "completed", "stopped", "failed"}:
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
        prefix = f"{NATIVE_JOB_STAGE_RELATIVE_DIR}/"
        staged = self.staged_config_relative_path
        digest = self.executable_xml_sha256
        # ponytail: pre-cfx jobs staged {digest}.xml; new launches write {digest}.cfx
        if not staged.startswith(prefix) or not (
            staged.endswith(f"/{digest}.cfx") or staged.endswith(f"/{digest}.xml")
        ):
            raise ResearchNativeJobError("native_job_stage_invalid", "native job staged configuration path is invalid")
        if self.launcher_sha256 is not None:
            _digest(self.launcher_sha256, code="native_job_launcher_invalid")
        if not isinstance(self.partial_side_effect, bool):
            raise ResearchNativeJobError("native_job_content_corrupt", "partial side-effect state is invalid")
        if any(not isinstance(item, dict) for item in self.receipts):
            raise ResearchNativeJobError("native_job_content_corrupt", "native control receipts are invalid")
        if self.worker is not None and (
            not isinstance(self.worker, dict)
            or set(self.worker) != _WORKER_KEYS
            or type(self.worker["pid"]) is not int
            or not (self.worker["http_port"] is None or type(self.worker["http_port"]) is int)
            or not isinstance(self.worker["log_path"], str)
            or not isinstance(self.worker["started_at"], str)
        ):
            raise ResearchNativeJobError("native_job_content_corrupt", "native worker handle is invalid")
        if self.completion is not None and (
            not isinstance(self.completion, dict)
            or set(self.completion) != _COMPLETION_KEYS
            or type(self.completion["exit_code"]) is not int
            or not isinstance(self.completion["finished_at"], str)
            or not isinstance(self.completion["stop_requested"], bool)
            or not isinstance(self.completion["log_ref"], str)
        ):
            raise ResearchNativeJobError("native_job_content_corrupt", "native worker completion is invalid")
        if self.state == "prepared":
            if self.receipts or self.partial_side_effect or self.failure_reason_code is not None or self.launcher_sha256 is not None:
                raise ResearchNativeJobError("native_job_content_corrupt", "prepared native job contains execution outcome")
            if self.worker is not None or self.completion is not None:
                raise ResearchNativeJobError("native_job_content_corrupt", "prepared native job contains execution outcome")
        elif self.state in {"submitted", "running", "completed", "stopped"}:
            if self.failure_reason_code is not None or self.partial_side_effect or len(self.receipts) != 2:
                raise ResearchNativeJobError("native_job_content_corrupt", "submitted native job outcome is inconsistent")
            start_state = "running" if self.state == "running" else "completed"
            if (
                self.launcher_sha256 is None
                or self.receipts[0].get("state") != "completed"
                or self.receipts[1].get("state") != start_state
            ):
                raise ResearchNativeJobError("native_job_content_corrupt", "submitted native job receipts are incomplete")
            if self.state == "submitted" and (self.worker is not None or self.completion is not None):
                raise ResearchNativeJobError("native_job_content_corrupt", "synchronous submitted job cannot carry a worker")
            if self.state == "running" and (self.worker is None or self.completion is not None):
                raise ResearchNativeJobError("native_job_content_corrupt", "running native job requires a worker handle")
            if self.state in {"completed", "stopped"} and (self.worker is None or self.completion is None):
                raise ResearchNativeJobError("native_job_content_corrupt", "finished native job requires worker and completion")
            if self.state == "stopped" and self.completion is not None and self.completion["stop_requested"] is not True:
                raise ResearchNativeJobError("native_job_content_corrupt", "stopped native job must record the stop request")
        else:
            if not isinstance(self.failure_reason_code, str) or not self.failure_reason_code or not self.receipts:
                raise ResearchNativeJobError("native_job_content_corrupt", "failed native job requires a structured refusal")

    def canonical_bytes(self) -> bytes:
        payload: dict[str, object] = {
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
        # Pre-worker records omit these keys; keep their canonical bytes (and revision ids) stable.
        if self.worker is not None or self.completion is not None:
            payload["worker"] = None if self.worker is None else dict(self.worker)
            payload["completion"] = None if self.completion is None else dict(self.completion)
        return _canonical(payload)

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
        if (
            not isinstance(payload, dict)
            or set(payload) not in (expected, expected | {"worker", "completion"})
            or payload.get("schema") != NATIVE_JOB_CONTENT_SCHEMA
        ):
            raise ResearchNativeJobError("native_job_content_corrupt", "native job content schema is invalid")
        try:
            return cls(
                worker=payload.get("worker"),
                completion=payload.get("completion"),
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


def builder_loadconfig_cfx(source_project_bytes: bytes, task_xml: bytes) -> bytes:
    """Wrap compiled Build-Task1.xml in the Task-rooted zip sqcli loadconfig accepts.

    Observed on SQX 144.2953: ``saveconfig`` writes a zip whose only member is
    ``config.xml`` rooted at ``<Task ... taskXMLFile="Build-Task1.xml">`` with the
    task ``<Settings>`` as its child. ``project.cfx`` keeps that Task self-closing
    and stores Settings separately; loadconfig of ``project.cfx`` fails with
    ``missing Task element``.
    """

    try:
        with ZipFile(BytesIO(source_project_bytes)) as archive:
            config = archive.read("config.xml")
            archive.getinfo(SQX_BUILDER_TASK_ENTRY)
    except (BadZipFile, KeyError, OSError) as exc:
        raise ResearchNativeJobError(
            "native_job_loadconfig_archive_invalid",
            "compiled Builder archive is not a readable project.cfx with config.xml and Build-Task1.xml",
        ) from exc
    if not task_xml.strip():
        raise ResearchNativeJobError("native_job_loadconfig_task_mismatch", "approved executable XML is empty")
    matches = list(_SELF_CLOSING_BUILD_TASK_RE.finditer(config))
    if len(matches) != 1:
        raise ResearchNativeJobError(
            "native_job_loadconfig_task_element_missing",
            "compiled Builder config.xml does not declare exactly one self-closing Build-Task1 Task",
        )
    open_tag = re.sub(rb"\s*/>$", b">", matches[0].group(0), count=1)
    return _deterministic_cfx(open_tag + task_xml + b"</Task>")


def _deterministic_cfx(config_xml: bytes) -> bytes:
    buffer = BytesIO()
    info = ZipInfo("config.xml")
    info.date_time = (1980, 1, 1, 0, 0, 0)
    info.compress_type = ZIP_DEFLATED
    info.create_system = 0
    with ZipFile(buffer, "w") as archive:
        archive.writestr(info, config_xml)
    return buffer.getvalue()


def _stage_exact_loadconfig_cfx(sqx_home: Path | str | None, cfx_bytes: bytes, digest: str) -> tuple[Path, str]:
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
        if resolved != target or not resolved.is_file() or existing != cfx_bytes:
            raise ResearchNativeJobError("native_job_stage_conflict", "existing staged configuration does not match approved bytes")
        return resolved, relative

    temporary = target.with_name(f".{target.name}.tmp-{os.getpid()}-{uuid4().hex}")
    try:
        with temporary.open("xb") as handle:
            handle.write(cfx_bytes)
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
    if resolved != target or not resolved.is_file() or staged != cfx_bytes:
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
    expected_evidence = {content.executable_xml_ref}
    if content.completion is not None:
        try:
            log_ref = EvidenceRef.parse(str(content.completion["log_ref"]))
        except ResearchCustodyError as exc:
            raise ResearchNativeJobError("native_job_content_corrupt", "native worker log evidence identity is invalid") from exc
        if EvidenceRef.from_bytes(store.read_evidence(log_ref)) != log_ref:
            raise ResearchNativeJobError("native_job_content_corrupt", "native worker log evidence is corrupt")
        expected_evidence.add(log_ref)
    if set(stored.evidence) != expected_evidence:
        raise ResearchNativeJobError("native_job_content_corrupt", "native job revision evidence binding is invalid")
    if content.state == "prepared":
        if stored.parent_revision is not None:
            raise ResearchNativeJobError("native_job_content_corrupt", "prepared native job cannot have a parent revision")
    else:
        if stored.parent_revision is None:
            raise ResearchNativeJobError("native_job_content_corrupt", "native job outcome must preserve its prepared parent")
        parent = NativeBuilderJobContent.from_bytes(store.read_revision_content(stored.parent_revision))
        # running/completed/stopped/failed may follow prepared; finished states may also follow running.
        allowed_parents = {"prepared"} if content.state in {"submitted", "running"} else {"prepared", "running"}
        if (
            parent.state not in allowed_parents
            or (parent.state == "running" and parent.worker != content.worker)
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
        "worker": None if content.worker is None else dict(content.worker),
        "completion": None if content.completion is None else dict(content.completion),
    }


class NativeWorkerRegistry:
    """Process-local registry of running Builder workers keyed by native-job entity id.

    Every registered worker is also handed to ``supervisor_register`` (the desktop
    worker supervisor) so a desktop shutdown stops SQX gracefully; nothing spawned
    through this module runs detached.
    """

    def __init__(self, supervisor_register: Callable[..., None] | None = None) -> None:
        self._workers: dict[str, SqxBuilderWorker] = {}
        self._lock = Lock()
        self._supervisor_register = supervisor_register

    def register(self, job_entity_id: str, worker: SqxBuilderWorker) -> None:
        with self._lock:
            if job_entity_id in self._workers:
                raise ResearchNativeJobError("native_job_duplicate", "native job already owns a running worker")
            self._workers[job_entity_id] = worker
        if self._supervisor_register is not None:
            self._supervisor_register(
                worker,
                label=f"sqx-builder:{job_entity_id}",
                timeout_seconds=NATIVE_JOB_STOP_TIMEOUT_SECONDS,
            )

    def get(self, job_entity_id: str) -> SqxBuilderWorker | None:
        with self._lock:
            return self._workers.get(job_entity_id)

    def forget(self, job_entity_id: str) -> None:
        with self._lock:
            self._workers.pop(job_entity_id, None)

    def running_count(self) -> int:
        with self._lock:
            return sum(1 for worker in self._workers.values() if worker.poll() is None)


def _utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


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
    worker_registry: NativeWorkerRegistry | None = None,
) -> dict[str, object]:
    """Stage and submit one exact approved configuration through the trusted gateway.

    A retry for the same approved revision returns the existing native-job record and
    never submits a second native start sequence. With a ``worker_registry`` the
    Builder start runs as a supervised worker and the job is recorded ``running``;
    without one the legacy bounded synchronous start is used.
    """

    record = read_current_configuration(store, configuration_entity_id)
    if record.get("revision") != expected_configuration_revision:
        raise ResearchCustodyError("current_conflict", "configuration revision changed before native launch")
    if record.get("state") != "approved" or record.get("approval", {}).get("approved") is not True:
        raise ResearchNativeJobError("native_job_configuration_unapproved", "native launch requires the exact current approved configuration")

    existing = _existing_job_for_configuration(store, expected_configuration_revision)
    if existing is not None:
        return {**existing, "reused": True}
    if worker_registry is not None and worker_registry.running_count():
        raise ResearchNativeJobError("native_job_busy", "a native Builder run is already in progress; stop it before launching another")

    executable_ref = EvidenceRef.parse(record["executable_xml_ref"])
    executable_sha = _digest(record["executable_xml_sha256"], code="native_job_executable_invalid")
    xml_bytes = store.read_evidence(executable_ref)
    if EvidenceRef.from_bytes(xml_bytes) != executable_ref or sha256(xml_bytes).hexdigest() != executable_sha:
        raise ResearchNativeJobError("native_job_executable_invalid", "approved executable evidence failed exact-byte verification")
    try:
        source_ref = EvidenceRef.parse(record["source_project_ref"])
    except (KeyError, TypeError, ResearchCustodyError) as exc:
        raise ResearchNativeJobError("native_job_source_invalid", "approved configuration is missing compiled source project evidence") from exc
    source_sha = _digest(record.get("source_project_sha256"), code="native_job_source_invalid")
    source_bytes = store.read_evidence(source_ref)
    if EvidenceRef.from_bytes(source_bytes) != source_ref or sha256(source_bytes).hexdigest() != source_sha:
        raise ResearchNativeJobError("native_job_source_invalid", "compiled source project evidence failed exact-byte verification")

    cfx_bytes = builder_loadconfig_cfx(source_bytes, xml_bytes)
    cfx_sha = sha256(cfx_bytes).hexdigest()
    staged_path, staged_relative = _stage_exact_loadconfig_cfx(sqx_home, cfx_bytes, executable_sha)
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

    gateway = gateway_factory(sqx_home, trusted_launcher_sha256)
    worker_log_path = None
    if worker_registry is not None:
        worker_log_path = store.base / NATIVE_JOB_WORKER_LOG_DIR / f"{job_entity.value}.log"
    try:
        receipt = gateway.launch_builder(
            staged_path,
            expected_config_sha256=cfx_sha,
            worker_log_path=worker_log_path,
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

    running = receipt.get("state") == "running"
    worker_handle = getattr(gateway, "worker", None)
    if (
        receipt.get("schema") != "tc.sqx-native-control.v1"
        or receipt.get("operation") != NATIVE_JOB_OPERATION
        or receipt.get("state") not in {"submitted", "running"}
        or receipt.get("sqx_build") != SQX_BUILD
        or receipt.get("config_sha256") != cfx_sha
        or receipt.get("config_relative_path") != staged_relative
        or not isinstance(receipt.get("launcher_sha256"), str)
        or not isinstance(receipt.get("receipts"), list)
        or (running and (worker_registry is None or not isinstance(worker_handle, SqxBuilderWorker)))
    ):
        raise ResearchNativeJobError("native_job_receipt_invalid", "native gateway returned an invalid success receipt")

    worker_model = None
    if running:
        worker_model = {
            "pid": worker_handle.pid,
            "http_port": worker_handle.http_port,
            "log_path": str(worker_handle.log_path),
            "started_at": _utc_now(),
        }
    submitted = NativeBuilderJobContent(
        state="running" if running else "submitted",
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
        worker=worker_model,
    )
    if running:
        worker_registry.register(str(job_entity), worker_handle)
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


def _finish_running_job(
    store: FileResearchCustodyStore,
    entity: ResearchEntityId,
    record: dict[str, object],
    *,
    exit_code: int,
    stop_requested: bool,
    log_text: str,
) -> dict[str, object]:
    """Persist the terminal revision of a running job from the exited worker's own output."""

    current_revision = ResearchRevisionRef.parse(str(record["revision"]))
    content = NativeBuilderJobContent.from_bytes(store.read_revision_content(current_revision))
    log_ref = store.put_evidence(log_text.encode("utf-8"))
    finished_cleanly = exit_code == 0 and _WORKER_FINISHED_MARKER in log_text
    receipts = tuple(dict(item) for item in content.receipts)
    if finished_cleanly:
        receipts[1]["state"] = "completed"
        receipts[1]["exit_code"] = exit_code
        completion = {
            "exit_code": exit_code,
            "finished_at": _utc_now(),
            "stop_requested": stop_requested,
            "log_ref": str(log_ref),
        }
        terminal = NativeBuilderJobContent(
            state="stopped" if stop_requested else "completed",
            configuration_entity_id=content.configuration_entity_id,
            configuration_revision=content.configuration_revision,
            executable_xml_ref=content.executable_xml_ref,
            executable_xml_sha256=content.executable_xml_sha256,
            sqx_build=content.sqx_build,
            operation=content.operation,
            staged_config_relative_path=content.staged_config_relative_path,
            launcher_sha256=content.launcher_sha256,
            partial_side_effect=False,
            receipts=receipts,
            worker=content.worker,
            completion=completion,
        )
    else:
        receipts[1]["state"] = "rejected"
        receipts[1]["exit_code"] = exit_code
        receipts[1]["reason_code"] = "sqx_worker_exited"
        terminal = NativeBuilderJobContent(
            state="failed",
            configuration_entity_id=content.configuration_entity_id,
            configuration_revision=content.configuration_revision,
            executable_xml_ref=content.executable_xml_ref,
            executable_xml_sha256=content.executable_xml_sha256,
            sqx_build=content.sqx_build,
            operation=content.operation,
            staged_config_relative_path=content.staged_config_relative_path,
            launcher_sha256=content.launcher_sha256,
            partial_side_effect=True,
            receipts=receipts,
            failure_reason_code="sqx_worker_exited",
            worker=content.worker,
            completion={
                "exit_code": exit_code,
                "finished_at": _utc_now(),
                "stop_requested": stop_requested,
                "log_ref": str(log_ref),
            },
        )
    revision = store.create_revision(
        entity,
        terminal.canonical_bytes(),
        parent_revision=current_revision,
        evidence=(content.executable_xml_ref, log_ref),
    )
    store.compare_and_set_current(entity, expected_revision=current_revision, target_revision=revision.revision)
    return _record(store, entity, revision.revision)


def refresh_native_job(
    store: FileResearchCustodyStore,
    entity_id: ResearchEntityId | str,
    *,
    worker_registry: NativeWorkerRegistry | None,
) -> dict[str, object]:
    """Read one job; for a running job reconcile it with its worker and attach SQX's live status.

    ``live`` is producer text/rows from sqcli's own ``status`` and is never persisted
    as a result. A worker that exited is finalised from its own stdout log. A running
    record whose worker this process does not own (cockpit restarted) is reported as
    ``supervised: false``; it is finalised as failed only when SQX's HTTP API no longer
    answers, because the cockpit cannot see that process's exit code.
    """

    entity = _job_entity(entity_id)
    record = _record(store, entity, store.current(entity))
    if record["state"] != "running":
        return record
    worker = worker_registry.get(str(entity)) if worker_registry is not None else None
    if worker is None:
        http_port = (record.get("worker") or {}).get("http_port")
        stand_in = None
        if type(http_port) is int:
            stand_in = SqxBuilderWorker(_DetachedProcess(), log_path=Path(str(record["worker"]["log_path"])), http_port=http_port)
        try:
            live = stand_in.status() if stand_in is not None else None
        except SqxNativeGatewayError:
            live = None
        if live is None:
            log_text = stand_in.read_log() if stand_in is not None else ""
            return _finish_running_job(store, entity, record, exit_code=-1, stop_requested=False, log_text=log_text)
        return {**record, "supervised": False, "live": live}
    exit_code = worker.poll()
    if exit_code is not None:
        worker_registry.forget(str(entity))
        return _finish_running_job(
            store, entity, record, exit_code=int(exit_code), stop_requested=worker.stop_requested, log_text=worker.read_log(),
        )
    try:
        live = worker.status()
    except SqxNativeGatewayError as exc:
        live = {"raw": "", "rows": {}, "unavailable_reason_code": exc.code}
    return {**record, "supervised": True, "live": live}


def stop_native_job(
    store: FileResearchCustodyStore,
    entity_id: ResearchEntityId | str,
    *,
    worker_registry: NativeWorkerRegistry | None,
    timeout_seconds: float = NATIVE_JOB_STOP_TIMEOUT_SECONDS,
) -> dict[str, object]:
    """Ask SQX to stop the running Builder gracefully and record the finished job.

    SQX saves the Results databank to files and exits by itself after ``stop``; the
    cockpit only waits for that exit and archives the worker's stdout as evidence.
    """

    entity = _job_entity(entity_id)
    record = _record(store, entity, store.current(entity))
    if record["state"] != "running":
        raise ResearchNativeJobError("native_job_not_running", "only a running native job can be stopped")
    worker = worker_registry.get(str(entity)) if worker_registry is not None else None
    if worker is None:
        raise ResearchNativeJobError("native_job_unsupervised", "this cockpit process does not own the running worker")
    if worker.poll() is None:
        try:
            worker.request_stop()
        except SqxNativeGatewayError as exc:
            raise ResearchNativeJobError(exc.code, exc.detail) from exc
        try:
            worker.wait(timeout=float(timeout_seconds))
        except subprocess.TimeoutExpired as exc:
            raise ResearchNativeJobError("sqx_stop_timeout", "SQX did not exit after the stop request") from exc
    worker_registry.forget(str(entity))
    return _finish_running_job(
        store, entity, record, exit_code=int(worker.poll()), stop_requested=True, log_text=worker.read_log(),
    )


class _DetachedProcess:
    """Stand-in for a worker this cockpit process did not spawn; only HTTP status is observable."""

    pid = 0

    def poll(self) -> int | None:
        return None

    def wait(self, timeout: float | None = None) -> int:
        raise subprocess.TimeoutExpired("detached", timeout or 0)

    def terminate(self) -> None:
        return None

    def kill(self) -> None:
        return None
