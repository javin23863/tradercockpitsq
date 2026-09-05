"""Native SQX Retester execution and durable historical-result custody.

StrategyQuant X remains the historical-result producer. TraderCockpit binds one exact
Candidate revision to the installed SQX 144.2953 Retester task-1 control, preserves
all executable/native artifact identities, and records immutable prepared/completed/
failed result custody. It does not implement a generic backtester, interpret trading
quality, or claim that execution completion is validation/promotion truth.
"""

from __future__ import annotations

from dataclasses import dataclass
from hashlib import sha256
from io import BytesIO
import json
import os
from pathlib import Path
import re
from uuid import UUID, uuid4
from xml.etree import ElementTree
from zipfile import BadZipFile, ZipFile
import zlib

from tradercockpit.research_candidates import ResearchCandidateError, read_current_candidate
from tradercockpit.research_custody import (
    EvidenceRef,
    FileResearchCustodyStore,
    ResearchCustodyError,
    ResearchEntityId,
    ResearchKind,
    ResearchRevisionRef,
)
from tradercockpit.sqx_gateway import SqxNativeControlGateway, SqxNativeGatewayError, single_retester_task, verified_retester_execution
from tradercockpit.sqx_outputs import SqxOutputError, inspect_sqx_output_bytes
from tradercockpit.sqx_presets import SQX_BUILD, SqxPresetRuntimeError, verified_sqx_home


RETESTER_CONTENT_SCHEMA = "tc.research-historical-result-content.v1"
RETESTER_READ_SCHEMA = "tc.research-historical-result.v1"
RETESTER_CATALOG_SCHEMA = "tc.research-historical-result-catalog.v1"
RETESTER_OPERATION = "native_retester_task_1"
RETESTER_SOURCE_PROJECT = "Retester"
RETESTER_TASK = 1
RETESTER_ENGINE_RELATIVE_PATH = "internal/libs/SQTradingLib.jar"
RETESTER_PROJECT_CONFIG_ENTRY = "config.xml"
RETESTER_PROJECT_TASK_ENTRY = "Retest-Task1.xml"
_CURRENT_POINTER_TEMP_RE = re.compile(
    r"^\.[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.json\.tmp-[0-9]+-[0-9a-f]{32}$"
)
_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")


class ResearchRetesterError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


def _canonical(payload: dict[str, object]) -> bytes:
    return json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def _digest(value: object, code: str) -> str:
    if not isinstance(value, str) or not _DIGEST_RE.fullmatch(value):
        raise ResearchRetesterError(code, "expected a lowercase 64-character SHA-256 digest")
    return value


def _typed_revision(value: str, kind: ResearchKind, code: str) -> ResearchRevisionRef:
    try:
        revision = ResearchRevisionRef.parse(value)
    except ResearchCustodyError as exc:
        raise ResearchRetesterError(code, "revision identity is invalid") from exc
    if revision.kind != kind:
        raise ResearchRetesterError(code, f"revision must be {kind.value} custody")
    return revision


def _historical_entity(value: ResearchEntityId | str) -> ResearchEntityId:
    try:
        entity = value if isinstance(value, ResearchEntityId) else ResearchEntityId.parse(value)
    except ResearchCustodyError as exc:
        raise ResearchRetesterError("historical_result_entity_invalid", "historical-result entity identity is invalid") from exc
    if entity.kind != ResearchKind.HISTORICAL_RESULT:
        raise ResearchRetesterError("historical_result_entity_invalid", "research entity is not a historical result")
    return entity


def _member(snapshot: bytes, name: str) -> bytes:
    try:
        with ZipFile(BytesIO(snapshot)) as archive:
            matches = [entry for entry in archive.infolist() if entry.filename == name]
            if len(matches) != 1:
                raise ResearchRetesterError("retester_result_corrupt", f"result archive must contain exactly one {name}")
            value = archive.read(matches[0])
    except (BadZipFile, RuntimeError, NotImplementedError, EOFError, OSError, zlib.error) as exc:
        raise ResearchRetesterError("retester_result_corrupt", f"result archive member {name} is unreadable") from exc
    if not value:
        raise ResearchRetesterError("retester_result_corrupt", f"result archive member {name} is empty")
    return value


def _sha_file(path: Path) -> str:
    digest = sha256()
    try:
        with path.open("rb") as handle:
            for chunk in iter(lambda: handle.read(1024 * 1024), b""):
                digest.update(chunk)
    except OSError as exc:
        raise ResearchRetesterError("retester_native_file_unreadable", "native Retester file could not be read") from exc
    return digest.hexdigest()


def _local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def _parse_retester_xml(payload: bytes, entry_name: str) -> ElementTree.Element:
    try:
        return ElementTree.fromstring(payload)
    except (ElementTree.ParseError, LookupError, ValueError) as exc:
        raise ResearchRetesterError(
            "retester_source_project_invalid",
            f"native Retester project entry {entry_name!r} is not valid XML",
        ) from exc


def _validate_retester_project(snapshot: bytes, *, require_single_task: bool = True) -> None:
    """Require native task 1 and, before execution, safe whole-project topology."""
    if require_single_task:
        try:
            single_retester_task(snapshot)
        except SqxNativeGatewayError as exc:
            raise ResearchRetesterError(exc.code, exc.detail) from exc

    try:
        with ZipFile(BytesIO(snapshot)) as archive:
            names = archive.namelist()
            if len(names) != len(set(names)):
                raise ResearchRetesterError(
                    "retester_source_project_invalid",
                    "native Retester project contains duplicate archive members",
                )
            missing = [
                name
                for name in (RETESTER_PROJECT_CONFIG_ENTRY, RETESTER_PROJECT_TASK_ENTRY)
                if name not in names
            ]
            if missing:
                raise ResearchRetesterError(
                    "retester_source_project_invalid",
                    "native Retester project is missing required task-1 structure: " + ", ".join(missing),
                )
            task_one_entries = [
                name
                for name in names
                if re.fullmatch(r"[A-Za-z][A-Za-z0-9]*-Task1\.xml", name)
            ]
            if task_one_entries != [RETESTER_PROJECT_TASK_ENTRY]:
                raise ResearchRetesterError(
                    "retester_source_project_invalid",
                    "native Retester task 1 is not one unambiguous Retest task",
                )

            config_root = _parse_retester_xml(
                archive.read(RETESTER_PROJECT_CONFIG_ENTRY),
                RETESTER_PROJECT_CONFIG_ENTRY,
            )
            task_root = _parse_retester_xml(
                archive.read(RETESTER_PROJECT_TASK_ENTRY),
                RETESTER_PROJECT_TASK_ENTRY,
            )
            if (
                _local_name(config_root.tag) != "Project"
                or config_root.attrib.get("name") != RETESTER_SOURCE_PROJECT
            ):
                raise ResearchRetesterError(
                    "retester_source_project_invalid",
                    "native Retester config.xml does not declare the Retester project",
                )
            tasks = next(
                (child for child in config_root if _local_name(child.tag) == "Tasks"),
                None,
            )
            declarations = (
                [child for child in tasks if _local_name(child.tag) == "Task"]
                if tasks is not None
                else []
            )
            if not declarations:
                raise ResearchRetesterError(
                    "retester_source_project_invalid",
                    "native Retester config.xml does not declare task 1",
                )
            task_one = declarations[0]
            if (
                task_one.attrib.get("type") != "Retest"
                or task_one.attrib.get("taskXMLFile") != RETESTER_PROJECT_TASK_ENTRY
            ):
                raise ResearchRetesterError(
                    "retester_source_project_invalid",
                    "native Retester task 1 is not declared as Retest bound to Retest-Task1.xml",
                )
            if _local_name(task_root.tag) != "Settings":
                raise ResearchRetesterError(
                    "retester_source_project_invalid",
                    "native Retester task 1 does not contain the producer Settings document",
                )
    except ResearchRetesterError:
        raise
    except (BadZipFile, RuntimeError, NotImplementedError, EOFError, OSError, zlib.error) as exc:
        raise ResearchRetesterError(
            "retester_source_project_invalid",
            "native Retester project is not a readable project archive",
        ) from exc


def _read_exact_inside(home: Path, relative: str, *, missing_code: str, escape_code: str) -> tuple[bytes, Path, str]:
    expected = home / relative
    try:
        before = expected.resolve(strict=True)
        before.relative_to(home)
    except (OSError, RuntimeError, ValueError) as exc:
        raise ResearchRetesterError(escape_code, f"native Retester path escapes verified SQX runtime: {relative}") from exc
    if not before.is_file():
        raise ResearchRetesterError(missing_code, f"native Retester file is missing: {relative}")
    try:
        with before.open("rb") as handle:
            opened = os.fstat(handle.fileno())
            snapshot = handle.read()
    except OSError as exc:
        raise ResearchRetesterError("retester_native_file_unreadable", f"native Retester file is unreadable: {relative}") from exc
    if not snapshot:
        raise ResearchRetesterError("retester_native_file_invalid", f"native Retester file is empty: {relative}")
    try:
        after = expected.resolve(strict=True)
        after_stat = after.stat()
        after.relative_to(home)
    except (OSError, RuntimeError, ValueError) as exc:
        raise ResearchRetesterError("retester_native_file_changed", f"native Retester file changed during capture: {relative}") from exc
    if after != before or (opened.st_dev, opened.st_ino) != (after_stat.st_dev, after_stat.st_ino):
        raise ResearchRetesterError("retester_native_file_changed", f"native Retester file changed during capture: {relative}")
    return snapshot, after, sha256(snapshot).hexdigest()


def _stage_file(path: Path, data: bytes, *, conflict_code: str) -> None:
    if path.exists():
        raise ResearchRetesterError(conflict_code, "isolated Retester workspace already exists")
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(f".{path.name}.tmp-{os.getpid()}-{uuid4().hex}")
    try:
        with temporary.open("xb") as handle:
            handle.write(data)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary, path)
    finally:
        try:
            temporary.unlink()
        except FileNotFoundError:
            pass


def _prepare_workspace(home: Path, entity: ResearchEntityId, project_bytes: bytes, candidate_name: str, candidate_bytes: bytes) -> tuple[str, Path, str, str]:
    project_name = f"TraderCockpit-Retester-{entity.value.hex}"
    projects_root = home / "user/projects"
    try:
        projects_resolved = projects_root.resolve(strict=True)
        projects_resolved.relative_to(home)
    except (OSError, RuntimeError, ValueError) as exc:
        raise ResearchRetesterError("retester_project_path_escape", "SQX project root escapes verified runtime") from exc
    if not projects_resolved.is_dir():
        raise ResearchRetesterError("retester_projects_missing", "SQX project root is missing")
    project_root = projects_resolved / project_name
    if project_root.exists():
        raise ResearchRetesterError("retester_workspace_conflict", "isolated Retester workspace already exists")
    results = project_root / "databanks/Results"
    results.mkdir(parents=True)
    try:
        resolved_project = project_root.resolve(strict=True)
        resolved_results = results.resolve(strict=True)
        resolved_project.relative_to(home)
        resolved_results.relative_to(resolved_project)
    except (OSError, RuntimeError, ValueError) as exc:
        raise ResearchRetesterError("retester_project_path_escape", "isolated Retester workspace escapes verified runtime") from exc
    if resolved_project.parent != projects_resolved:
        raise ResearchRetesterError("retester_project_path_escape", "isolated Retester project is not a direct SQX project child")

    project_file = resolved_project / "project.cfx"
    candidate_file = resolved_results / candidate_name
    _stage_file(project_file, project_bytes, conflict_code="retester_workspace_conflict")
    _stage_file(candidate_file, candidate_bytes, conflict_code="retester_workspace_conflict")
    if _sha_file(project_file) != sha256(project_bytes).hexdigest() or _sha_file(candidate_file) != sha256(candidate_bytes).hexdigest():
        raise ResearchRetesterError("retester_stage_corrupt", "isolated Retester staging failed exact-byte verification")
    return project_name, project_file, f"user/projects/{project_name}/project.cfx", f"user/projects/{project_name}/databanks/Results/{candidate_name}"


def _capture_result(home: Path, project_name: str) -> tuple[bytes, dict[str, object]]:
    project_root = home / "user/projects" / project_name
    results = project_root / "databanks/Results"
    try:
        root = results.resolve(strict=True)
        root.relative_to(home)
        if root.parent.parent != project_root.resolve(strict=True):
            raise ValueError("unexpected results topology")
    except (OSError, RuntimeError, ValueError) as exc:
        raise ResearchRetesterError("retester_result_path_escape", "Retester Results databank escapes isolated project") from exc
    if not root.is_dir():
        raise ResearchRetesterError("retester_result_missing", "Retester Results databank is missing")
    paths = sorted(root.glob("*.sqx"), key=lambda item: item.name.casefold())
    if len(paths) != 1:
        raise ResearchRetesterError("retester_result_ambiguous", f"Retester produced {len(paths)} result archives; expected exactly one")
    path = paths[0]
    if Path(path.name).name != path.name:
        raise ResearchRetesterError("retester_result_invalid", "Retester result archive name is invalid")
    try:
        before = path.resolve(strict=True)
        before.relative_to(root)
        if before.parent != root or not before.is_file():
            raise ValueError("unexpected result path")
        with before.open("rb") as handle:
            opened = os.fstat(handle.fileno())
            snapshot = handle.read()
        after = path.resolve(strict=True)
        after_stat = after.stat()
        after.relative_to(root)
    except (OSError, RuntimeError, ValueError) as exc:
        raise ResearchRetesterError("retester_result_changed", "Retester result identity changed during capture") from exc
    if not snapshot or after != before or after.parent != root or (opened.st_dev, opened.st_ino) != (after_stat.st_dev, after_stat.st_ino):
        raise ResearchRetesterError("retester_result_changed", "Retester result identity changed during capture")
    try:
        record = inspect_sqx_output_bytes(snapshot, archive_name=path.name)
    except SqxOutputError as exc:
        raise ResearchRetesterError(exc.code, exc.detail) from exc
    except zlib.error as exc:
        raise ResearchRetesterError(
            "retester_result_corrupt",
            "Retester result archive contains unreadable compressed data",
        ) from exc
    record["relative_path"] = f"user/projects/{project_name}/databanks/Results/{path.name}"
    return snapshot, record


@dataclass(frozen=True, slots=True)
class NativeRetesterContent:
    state: str
    candidate_entity_id: str
    candidate_revision: str
    candidate_archive_name: str
    candidate_archive_ref: EvidenceRef
    candidate_archive_sha256: str
    sqx_build: str
    operation: str
    retester_task: int
    native_project_name: str
    native_project_relative_path: str
    source_project_ref: EvidenceRef
    source_project_sha256: str
    engine_ref: EvidenceRef
    engine_sha256: str
    launcher_sha256: str | None
    receipts: tuple[dict[str, object], ...]
    partial_side_effect: bool
    result_archive_name: str | None = None
    result_archive_relative_path: str | None = None
    result_archive_ref: EvidenceRef | None = None
    result_archive_sha256: str | None = None
    result_strategy_ref: EvidenceRef | None = None
    result_strategy_sha256: str | None = None
    result_settings_ref: EvidenceRef | None = None
    result_settings_sha256: str | None = None
    failure_reason_code: str | None = None

    def __post_init__(self) -> None:
        if self.state not in {"prepared", "completed", "failed"}:
            raise ResearchRetesterError("historical_result_state_invalid", "historical-result state is invalid")
        try:
            candidate = ResearchEntityId.parse(self.candidate_entity_id)
        except ResearchCustodyError as exc:
            raise ResearchRetesterError("historical_result_candidate_invalid", "candidate entity identity is invalid") from exc
        if candidate.kind != ResearchKind.CANDIDATE:
            raise ResearchRetesterError("historical_result_candidate_invalid", "historical result must bind Candidate custody")
        _typed_revision(self.candidate_revision, ResearchKind.CANDIDATE, "historical_result_candidate_invalid")
        if not self.candidate_archive_name or Path(self.candidate_archive_name).name != self.candidate_archive_name or not self.candidate_archive_name.lower().endswith(".sqx"):
            raise ResearchRetesterError("historical_result_candidate_invalid", "candidate archive name is invalid")
        if not isinstance(self.candidate_archive_ref, EvidenceRef) or self.candidate_archive_ref.digest != _digest(self.candidate_archive_sha256, "historical_result_candidate_invalid"):
            raise ResearchRetesterError("historical_result_candidate_invalid", "candidate archive evidence identity is invalid")
        if self.sqx_build != SQX_BUILD or self.operation != RETESTER_OPERATION or self.retester_task != RETESTER_TASK:
            raise ResearchRetesterError("historical_result_control_invalid", "Retester control identity is invalid")
        if not re.fullmatch(r"TraderCockpit-Retester-[0-9a-f]{32}", self.native_project_name):
            raise ResearchRetesterError("historical_result_control_invalid", "native Retester project identity is invalid")
        expected_relative = f"user/projects/{self.native_project_name}/project.cfx"
        if self.native_project_relative_path != expected_relative:
            raise ResearchRetesterError("historical_result_control_invalid", "native Retester project path is invalid")
        for ref, digest, code in (
            (self.source_project_ref, self.source_project_sha256, "historical_result_project_invalid"),
            (self.engine_ref, self.engine_sha256, "historical_result_engine_invalid"),
        ):
            if not isinstance(ref, EvidenceRef) or ref.digest != _digest(digest, code):
                raise ResearchRetesterError(code, "historical-result evidence identity is invalid")
        if self.launcher_sha256 is not None:
            _digest(self.launcher_sha256, "historical_result_launcher_invalid")
        if not isinstance(self.partial_side_effect, bool) or any(not isinstance(item, dict) for item in self.receipts):
            raise ResearchRetesterError("historical_result_content_corrupt", "historical-result receipt state is invalid")

        result_fields = (
            self.result_archive_name,
            self.result_archive_relative_path,
            self.result_archive_ref,
            self.result_archive_sha256,
            self.result_strategy_ref,
            self.result_strategy_sha256,
            self.result_settings_ref,
            self.result_settings_sha256,
        )
        if self.state == "prepared":
            if any(item is not None for item in result_fields) or self.launcher_sha256 is not None or self.receipts or self.partial_side_effect or self.failure_reason_code is not None:
                raise ResearchRetesterError("historical_result_content_corrupt", "prepared historical result contains execution outcome")
        elif self.state == "completed":
            if any(item is None for item in result_fields) or self.failure_reason_code is not None or self.partial_side_effect:
                raise ResearchRetesterError("historical_result_content_corrupt", "completed historical result is incomplete")
            if self.launcher_sha256 is None or len(self.receipts) != 1 or self.receipts[0].get("state") != "completed":
                raise ResearchRetesterError("historical_result_content_corrupt", "completed Retester receipt is invalid")
            if not isinstance(self.result_archive_ref, EvidenceRef) or self.result_archive_ref.digest != _digest(self.result_archive_sha256, "historical_result_content_corrupt"):
                raise ResearchRetesterError("historical_result_content_corrupt", "result archive evidence identity is invalid")
            if not isinstance(self.result_strategy_ref, EvidenceRef) or self.result_strategy_ref.digest != _digest(self.result_strategy_sha256, "historical_result_content_corrupt"):
                raise ResearchRetesterError("historical_result_content_corrupt", "result strategy evidence identity is invalid")
            if not isinstance(self.result_settings_ref, EvidenceRef) or self.result_settings_ref.digest != _digest(self.result_settings_sha256, "historical_result_content_corrupt"):
                raise ResearchRetesterError("historical_result_content_corrupt", "result settings evidence identity is invalid")
            if self.result_archive_sha256 == self.candidate_archive_sha256:
                raise ResearchRetesterError("retester_result_unchanged", "Retester result archive did not change from Candidate input")
        else:
            if any(item is not None for item in result_fields):
                raise ResearchRetesterError("historical_result_content_corrupt", "failed historical result cannot contain accepted result evidence")
            if not isinstance(self.failure_reason_code, str) or not self.failure_reason_code:
                raise ResearchRetesterError("historical_result_content_corrupt", "failed historical result requires a failure reason")

    def canonical_bytes(self) -> bytes:
        return _canonical({
            "candidate_archive_name": self.candidate_archive_name,
            "candidate_archive_ref": str(self.candidate_archive_ref),
            "candidate_archive_sha256": self.candidate_archive_sha256,
            "candidate_entity_id": self.candidate_entity_id,
            "candidate_revision": self.candidate_revision,
            "engine_ref": str(self.engine_ref),
            "engine_sha256": self.engine_sha256,
            "failure_reason_code": self.failure_reason_code,
            "launcher_sha256": self.launcher_sha256,
            "native_project_name": self.native_project_name,
            "native_project_relative_path": self.native_project_relative_path,
            "operation": self.operation,
            "partial_side_effect": self.partial_side_effect,
            "receipts": [dict(item) for item in self.receipts],
            "result_archive_name": self.result_archive_name,
            "result_archive_ref": str(self.result_archive_ref) if self.result_archive_ref else None,
            "result_archive_relative_path": self.result_archive_relative_path,
            "result_archive_sha256": self.result_archive_sha256,
            "result_settings_ref": str(self.result_settings_ref) if self.result_settings_ref else None,
            "result_settings_sha256": self.result_settings_sha256,
            "result_strategy_ref": str(self.result_strategy_ref) if self.result_strategy_ref else None,
            "result_strategy_sha256": self.result_strategy_sha256,
            "retester_task": self.retester_task,
            "schema": RETESTER_CONTENT_SCHEMA,
            "source_project_ref": str(self.source_project_ref),
            "source_project_sha256": self.source_project_sha256,
            "sqx_build": self.sqx_build,
            "state": self.state,
        })

    @classmethod
    def from_bytes(cls, data: bytes) -> "NativeRetesterContent":
        try:
            payload = json.loads(data)
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise ResearchRetesterError("historical_result_content_corrupt", "historical-result content is not valid JSON") from exc
        expected = {
            "candidate_archive_name", "candidate_archive_ref", "candidate_archive_sha256", "candidate_entity_id", "candidate_revision",
            "engine_ref", "engine_sha256", "failure_reason_code", "launcher_sha256", "native_project_name", "native_project_relative_path",
            "operation", "partial_side_effect", "receipts", "result_archive_name", "result_archive_ref", "result_archive_relative_path",
            "result_archive_sha256", "result_settings_ref", "result_settings_sha256", "result_strategy_ref", "result_strategy_sha256",
            "retester_task", "schema", "source_project_ref", "source_project_sha256", "sqx_build", "state",
        }
        if not isinstance(payload, dict) or set(payload) != expected or payload.get("schema") != RETESTER_CONTENT_SCHEMA:
            raise ResearchRetesterError("historical_result_content_corrupt", "historical-result content schema is invalid")
        try:
            return cls(
                state=payload["state"],
                candidate_entity_id=payload["candidate_entity_id"],
                candidate_revision=payload["candidate_revision"],
                candidate_archive_name=payload["candidate_archive_name"],
                candidate_archive_ref=EvidenceRef.parse(payload["candidate_archive_ref"]),
                candidate_archive_sha256=payload["candidate_archive_sha256"],
                sqx_build=payload["sqx_build"],
                operation=payload["operation"],
                retester_task=payload["retester_task"],
                native_project_name=payload["native_project_name"],
                native_project_relative_path=payload["native_project_relative_path"],
                source_project_ref=EvidenceRef.parse(payload["source_project_ref"]),
                source_project_sha256=payload["source_project_sha256"],
                engine_ref=EvidenceRef.parse(payload["engine_ref"]),
                engine_sha256=payload["engine_sha256"],
                launcher_sha256=payload["launcher_sha256"],
                receipts=tuple(dict(item) for item in payload["receipts"]),
                partial_side_effect=payload["partial_side_effect"],
                result_archive_name=payload["result_archive_name"],
                result_archive_relative_path=payload["result_archive_relative_path"],
                result_archive_ref=EvidenceRef.parse(payload["result_archive_ref"]) if payload["result_archive_ref"] else None,
                result_archive_sha256=payload["result_archive_sha256"],
                result_strategy_ref=EvidenceRef.parse(payload["result_strategy_ref"]) if payload["result_strategy_ref"] else None,
                result_strategy_sha256=payload["result_strategy_sha256"],
                result_settings_ref=EvidenceRef.parse(payload["result_settings_ref"]) if payload["result_settings_ref"] else None,
                result_settings_sha256=payload["result_settings_sha256"],
                failure_reason_code=payload["failure_reason_code"],
            )
        except (KeyError, TypeError, ResearchCustodyError, ResearchRetesterError) as exc:
            detail = getattr(exc, "detail", "historical-result content fields are invalid")
            raise ResearchRetesterError("historical_result_content_corrupt", str(detail)) from exc


def _current_entities(store: FileResearchCustodyStore) -> tuple[ResearchEntityId, ...]:
    directory = store.base / "current" / ResearchKind.HISTORICAL_RESULT.value
    if not directory.exists():
        return ()
    if not directory.is_dir():
        raise ResearchCustodyError("current_pointer_corrupt", "historical-result current-pointer directory is invalid")
    entities: list[ResearchEntityId] = []
    for path in sorted(directory.iterdir(), key=lambda item: item.name):
        if _CURRENT_POINTER_TEMP_RE.fullmatch(path.name):
            continue
        if not path.is_file() or path.suffix != ".json":
            raise ResearchCustodyError("current_pointer_corrupt", "historical-result current-pointer directory contains an unexpected entry")
        try:
            value = UUID(path.stem)
        except ValueError as exc:
            raise ResearchCustodyError("current_pointer_corrupt", "historical-result current-pointer filename is not a canonical UUID") from exc
        if str(value) != path.stem:
            raise ResearchCustodyError("current_pointer_corrupt", "historical-result current-pointer UUID is not canonical")
        entity = ResearchEntityId(ResearchKind.HISTORICAL_RESULT, value)
        store.current(entity)
        entities.append(entity)
    return tuple(entities)


def _evidence_set(content: NativeRetesterContent) -> set[EvidenceRef]:
    result = {content.candidate_archive_ref, content.source_project_ref, content.engine_ref}
    if content.result_archive_ref:
        result.add(content.result_archive_ref)
    if content.result_strategy_ref:
        result.add(content.result_strategy_ref)
    if content.result_settings_ref:
        result.add(content.result_settings_ref)
    return result


def _record(store: FileResearchCustodyStore, entity: ResearchEntityId, revision: ResearchRevisionRef) -> dict[str, object]:
    stored = store.read_revision(revision)
    if stored.entity_id != entity:
        raise ResearchRetesterError("historical_result_content_corrupt", "historical-result revision belongs to another entity")
    content = NativeRetesterContent.from_bytes(store.read_revision_content(revision))
    if set(stored.evidence) != _evidence_set(content):
        raise ResearchRetesterError("historical_result_content_corrupt", "historical-result evidence set is invalid")
    candidate_bytes = store.read_evidence(content.candidate_archive_ref)
    try:
        candidate_inspected = inspect_sqx_output_bytes(candidate_bytes, archive_name=content.candidate_archive_name)
    except SqxOutputError as exc:
        raise ResearchRetesterError("historical_result_content_corrupt", exc.detail) from exc
    if candidate_inspected["archive_sha256"] != content.candidate_archive_sha256:
        raise ResearchRetesterError("historical_result_content_corrupt", "candidate archive evidence binding is invalid")
    source_project_bytes = store.read_evidence(content.source_project_ref)
    if sha256(source_project_bytes).hexdigest() != content.source_project_sha256:
        raise ResearchRetesterError("historical_result_content_corrupt", "Retester project evidence binding is invalid")
    try:
        _validate_retester_project(source_project_bytes, require_single_task=False)
    except ResearchRetesterError as exc:
        raise ResearchRetesterError("historical_result_content_corrupt", exc.detail) from exc
    if sha256(store.read_evidence(content.engine_ref)).hexdigest() != content.engine_sha256:
        raise ResearchRetesterError("historical_result_content_corrupt", "Retester engine evidence binding is invalid")

    if content.state == "prepared":
        if stored.parent_revision is not None:
            raise ResearchRetesterError("historical_result_content_corrupt", "prepared historical result cannot have a parent")
    else:
        if stored.parent_revision is None:
            raise ResearchRetesterError("historical_result_content_corrupt", "historical-result outcome requires prepared parent")
        parent = NativeRetesterContent.from_bytes(store.read_revision_content(stored.parent_revision))
        if (
            parent.state != "prepared"
            or parent.candidate_entity_id != content.candidate_entity_id
            or parent.candidate_revision != content.candidate_revision
            or parent.candidate_archive_ref != content.candidate_archive_ref
            or parent.source_project_ref != content.source_project_ref
            or parent.engine_ref != content.engine_ref
            or parent.native_project_name != content.native_project_name
            or parent.native_project_relative_path != content.native_project_relative_path
        ):
            raise ResearchRetesterError("historical_result_content_corrupt", "historical-result outcome does not match prepared control identity")

    if content.state == "completed":
        assert content.result_archive_ref and content.result_strategy_ref and content.result_settings_ref
        result_bytes = store.read_evidence(content.result_archive_ref)
        try:
            result_inspected = inspect_sqx_output_bytes(result_bytes, archive_name=content.result_archive_name or "")
        except SqxOutputError as exc:
            raise ResearchRetesterError("historical_result_content_corrupt", exc.detail) from exc
        if (
            result_inspected["archive_sha256"] != content.result_archive_sha256
            or result_inspected["strategy_entry_sha256"] != content.result_strategy_sha256
            or result_inspected["settings_entry_sha256"] != content.result_settings_sha256
            or _member(result_bytes, "strategy_Portfolio.xml") != store.read_evidence(content.result_strategy_ref)
            or _member(result_bytes, "settings.xml") != store.read_evidence(content.result_settings_ref)
        ):
            raise ResearchRetesterError("historical_result_content_corrupt", "Retester result evidence binding is invalid")

    execution_verified = False
    if content.state == "completed" and content.receipts[0].get("action") == "start":
        receipt = content.receipts[0]
        try:
            execution_verified = (verified_retester_execution(receipt, single_retester_task(source_project_bytes))
                and receipt.get("project") == content.native_project_name
                and receipt.get("project_sha256") == content.source_project_sha256
                and receipt.get("engine_sha256") == content.engine_sha256
                and receipt.get("launcher_sha256") == content.launcher_sha256
                and receipt.get("sqx_build") == content.sqx_build)
        except SqxNativeGatewayError:
            execution_verified = False
        if not execution_verified:
            raise ResearchRetesterError("historical_result_content_corrupt", "native execution proof is not bound to Historical Result custody")

    return {
        "schema": RETESTER_READ_SCHEMA,
        "entity_id": str(entity),
        "revision": str(revision),
        "parent_revision": str(stored.parent_revision) if stored.parent_revision else None,
        "state": content.state,
        "candidate_entity_id": content.candidate_entity_id,
        "candidate_revision": content.candidate_revision,
        "candidate_archive_name": content.candidate_archive_name,
        "candidate_archive_ref": str(content.candidate_archive_ref),
        "candidate_archive_sha256": content.candidate_archive_sha256,
        "sqx_build": content.sqx_build,
        "operation": content.operation,
        "retester_task": content.retester_task,
        "native_project_name": content.native_project_name,
        "native_project_relative_path": content.native_project_relative_path,
        "source_project_ref": str(content.source_project_ref),
        "source_project_sha256": content.source_project_sha256,
        "engine_ref": str(content.engine_ref),
        "engine_sha256": content.engine_sha256,
        "launcher_sha256": content.launcher_sha256,
        "receipts": [dict(item) for item in content.receipts],
        "partial_side_effect": content.partial_side_effect,
        "result_archive_name": content.result_archive_name,
        "result_archive_relative_path": content.result_archive_relative_path,
        "result_archive_ref": str(content.result_archive_ref) if content.result_archive_ref else None,
        "result_archive_sha256": content.result_archive_sha256,
        "result_strategy_ref": str(content.result_strategy_ref) if content.result_strategy_ref else None,
        "result_strategy_sha256": content.result_strategy_sha256,
        "result_settings_ref": str(content.result_settings_ref) if content.result_settings_ref else None,
        "result_settings_sha256": content.result_settings_sha256,
        "failure_reason_code": content.failure_reason_code,
        "execution_completed": execution_verified,
        "execution_verification": "verified" if execution_verified else "unverified",
        "validation_state": "not_run",
    }


def list_current_historical_results(store: FileResearchCustodyStore, candidate_revision: str | None = None) -> dict[str, object]:
    selected = _typed_revision(candidate_revision, ResearchKind.CANDIDATE, "historical_result_candidate_invalid") if candidate_revision else None
    results: list[dict[str, object]] = []
    for entity in _current_entities(store):
        record = _record(store, entity, store.current(entity))
        if selected is not None and record["candidate_revision"] != str(selected):
            continue
        results.append(record)
    return {"schema": RETESTER_CATALOG_SCHEMA, "results": results}


def read_current_historical_result(store: FileResearchCustodyStore, entity_id: ResearchEntityId | str) -> dict[str, object]:
    entity = _historical_entity(entity_id)
    return _record(store, entity, store.current(entity))


def read_historical_result_revision(
    store: FileResearchCustodyStore,
    entity_id: ResearchEntityId | str,
    revision: ResearchRevisionRef | str,
) -> dict[str, object]:
    """Read one exact immutable Historical Result through the canonical validator."""

    entity = _historical_entity(entity_id)
    if isinstance(revision, ResearchRevisionRef):
        if revision.kind != ResearchKind.HISTORICAL_RESULT:
            raise ResearchRetesterError(
                "historical_result_content_corrupt",
                "historical-result revision is not Historical Result custody",
            )
        selected = revision
    else:
        selected = _typed_revision(
            revision,
            ResearchKind.HISTORICAL_RESULT,
            "historical_result_content_corrupt",
        )
    return _record(store, entity, selected)


def _existing_for_candidate(store: FileResearchCustodyStore, candidate_revision: str) -> dict[str, object] | None:
    results = list_current_historical_results(store, candidate_revision)["results"]
    if not isinstance(results, list):
        raise ResearchRetesterError("historical_result_content_corrupt", "historical-result catalog is invalid")
    if len(results) > 1:
        raise ResearchRetesterError("historical_result_duplicate", "multiple baseline Retester results bind the same Candidate revision")
    return results[0] if results else None


def _failed_successor(
    store: FileResearchCustodyStore,
    entity: ResearchEntityId,
    prepared_revision: ResearchRevisionRef,
    prepared: NativeRetesterContent,
    *,
    reason_code: str,
    launcher_sha256: str | None,
    receipts: tuple[dict[str, object], ...],
    partial_side_effect: bool,
) -> ResearchRevisionRef:
    failed = NativeRetesterContent(
        state="failed",
        candidate_entity_id=prepared.candidate_entity_id,
        candidate_revision=prepared.candidate_revision,
        candidate_archive_name=prepared.candidate_archive_name,
        candidate_archive_ref=prepared.candidate_archive_ref,
        candidate_archive_sha256=prepared.candidate_archive_sha256,
        sqx_build=prepared.sqx_build,
        operation=prepared.operation,
        retester_task=prepared.retester_task,
        native_project_name=prepared.native_project_name,
        native_project_relative_path=prepared.native_project_relative_path,
        source_project_ref=prepared.source_project_ref,
        source_project_sha256=prepared.source_project_sha256,
        engine_ref=prepared.engine_ref,
        engine_sha256=prepared.engine_sha256,
        launcher_sha256=launcher_sha256,
        receipts=receipts,
        partial_side_effect=partial_side_effect,
        failure_reason_code=reason_code,
    )
    revision = store.create_revision(
        entity,
        failed.canonical_bytes(),
        parent_revision=prepared_revision,
        evidence=(prepared.candidate_archive_ref, prepared.source_project_ref, prepared.engine_ref),
    )
    store.compare_and_set_current(entity, expected_revision=prepared_revision, target_revision=revision.revision)
    return revision.revision


def start_native_retester(
    store: FileResearchCustodyStore,
    sqx_home: Path | str | None,
    trusted_launcher_sha256: str | None,
    *,
    candidate_entity_id: str,
    expected_candidate_revision: str,
    gateway_factory=SqxNativeControlGateway,
) -> dict[str, object]:
    """Execute one exact Candidate through installed native Retester task 1."""

    try:
        candidate = read_current_candidate(store, candidate_entity_id)
    except ResearchCandidateError as exc:
        raise ResearchRetesterError(exc.code, exc.detail) from exc
    if candidate.get("revision") != expected_candidate_revision:
        raise ResearchCustodyError("current_conflict", "Candidate revision changed before Retester execution")
    if candidate.get("sqx_build") != SQX_BUILD:
        raise ResearchRetesterError("historical_result_candidate_invalid", "Candidate SQX build does not match Retester runtime")

    existing = _existing_for_candidate(store, expected_candidate_revision)
    if existing is not None:
        if existing.get("state") == "completed" and existing.get("execution_completed") is not True:
            raise ResearchRetesterError("retester_execution_unverified", "The existing Historical Result has no verified native task execution; it cannot be reused as a completed run")
        return {**existing, "reused": True}

    candidate_ref = EvidenceRef.parse(candidate["archive_ref"])
    candidate_sha = _digest(candidate["archive_sha256"], "historical_result_candidate_invalid")
    candidate_bytes = store.read_evidence(candidate_ref)
    if EvidenceRef.from_bytes(candidate_bytes) != candidate_ref or sha256(candidate_bytes).hexdigest() != candidate_sha:
        raise ResearchRetesterError("historical_result_candidate_invalid", "Candidate archive evidence failed exact-byte verification")
    try:
        candidate_info = inspect_sqx_output_bytes(candidate_bytes, archive_name=candidate["archive_name"])
    except SqxOutputError as exc:
        raise ResearchRetesterError(exc.code, exc.detail) from exc
    if (
        candidate_info["archive_sha256"] != candidate_sha
        or candidate_info["strategy_entry_sha256"] != candidate["strategy_sha256"]
        or candidate_info["settings_entry_sha256"] != candidate["settings_sha256"]
    ):
        raise ResearchRetesterError("historical_result_candidate_invalid", "Candidate archive/member custody is inconsistent")

    try:
        home = verified_sqx_home(sqx_home)
    except SqxPresetRuntimeError as exc:
        raise ResearchRetesterError(exc.code, exc.detail) from exc
    project_bytes, _, project_sha = _read_exact_inside(
        home,
        f"user/projects/{RETESTER_SOURCE_PROJECT}/project.cfx",
        missing_code="retester_source_project_missing",
        escape_code="retester_source_project_path_escape",
    )
    _validate_retester_project(project_bytes)
    engine_bytes, _, engine_sha = _read_exact_inside(
        home,
        RETESTER_ENGINE_RELATIVE_PATH,
        missing_code="retester_engine_missing",
        escape_code="retester_engine_path_escape",
    )

    source_project_ref = store.put_evidence(project_bytes)
    engine_ref = store.put_evidence(engine_bytes)
    entity = store.create_entity(ResearchKind.HISTORICAL_RESULT)
    project_name, project_file, project_relative, _ = _prepare_workspace(
        home,
        entity,
        project_bytes,
        candidate["archive_name"],
        candidate_bytes,
    )
    if project_file.read_bytes() != project_bytes:
        raise ResearchRetesterError("retester_stage_corrupt", "staged Retester project changed after write")

    prepared = NativeRetesterContent(
        state="prepared",
        candidate_entity_id=candidate_entity_id,
        candidate_revision=expected_candidate_revision,
        candidate_archive_name=candidate["archive_name"],
        candidate_archive_ref=candidate_ref,
        candidate_archive_sha256=candidate_sha,
        sqx_build=SQX_BUILD,
        operation=RETESTER_OPERATION,
        retester_task=RETESTER_TASK,
        native_project_name=project_name,
        native_project_relative_path=project_relative,
        source_project_ref=source_project_ref,
        source_project_sha256=project_sha,
        engine_ref=engine_ref,
        engine_sha256=engine_sha,
        launcher_sha256=None,
        receipts=(),
        partial_side_effect=False,
    )
    prepared_revision = store.create_revision(
        entity,
        prepared.canonical_bytes(),
        evidence=(candidate_ref, source_project_ref, engine_ref),
    )
    store.compare_and_set_current(entity, expected_revision=None, target_revision=prepared_revision.revision)

    try:
        _, _, launch_engine_sha = _read_exact_inside(
            home,
            RETESTER_ENGINE_RELATIVE_PATH,
            missing_code="retester_engine_missing",
            escape_code="retester_engine_path_escape",
        )
    except ResearchRetesterError as exc:
        _failed_successor(
            store,
            entity,
            prepared_revision.revision,
            prepared,
            reason_code=exc.code,
            launcher_sha256=None,
            receipts=(),
            partial_side_effect=False,
        )
        raise
    if launch_engine_sha != engine_sha:
        code = "retester_engine_changed_before_execution"
        _failed_successor(
            store,
            entity,
            prepared_revision.revision,
            prepared,
            reason_code=code,
            launcher_sha256=None,
            receipts=(),
            partial_side_effect=False,
        )
        raise ResearchRetesterError(
            code,
            "installed SQTradingLib.jar changed after provenance capture and before native Retester launch",
        )

    try:
        receipt = gateway_factory(sqx_home, trusted_launcher_sha256).launch_retester_task(
            project_name,
            expected_project_sha256=project_sha,
            expected_engine_sha256=engine_sha,
        )
    except SqxNativeGatewayError as exc:
        model = exc.read_model()
        receipts = tuple(dict(item) for item in model["receipts"])
        launcher = next((item.get("launcher_sha256") for item in reversed(receipts) if item.get("launcher_sha256")), None)
        _failed_successor(
            store,
            entity,
            prepared_revision.revision,
            prepared,
            reason_code=exc.code,
            launcher_sha256=launcher if isinstance(launcher, str) else None,
            receipts=receipts,
            partial_side_effect=bool(model["partial_side_effect"]),
        )
        raise ResearchRetesterError(exc.code, exc.detail) from exc

    if (
        receipt.get("schema") != "tc.sqx-native-control.v1"
        or receipt.get("operation") != "retester_start_task"
        or receipt.get("project") != project_name
        or receipt.get("task") != RETESTER_TASK
        or receipt.get("state") != "submitted"
        or receipt.get("sqx_build") != SQX_BUILD
        or receipt.get("project_sha256") != project_sha
        or receipt.get("engine_sha256") != engine_sha
        or receipt.get("project_relative_path") != project_relative
        or not isinstance(receipt.get("launcher_sha256"), str)
        or not isinstance(receipt.get("receipts"), list)
        or len(receipt["receipts"]) != 1
        or not verified_retester_execution(receipt["receipts"][0], single_retester_task(project_bytes))
        or any(receipt["receipts"][0].get(key) != receipt.get(key) for key in ("project", "project_sha256", "engine_sha256", "launcher_sha256", "sqx_build"))
    ):
        _failed_successor(
            store,
            entity,
            prepared_revision.revision,
            prepared,
            reason_code="retester_receipt_invalid",
            launcher_sha256=None,
            receipts=(),
            partial_side_effect=True,
        )
        raise ResearchRetesterError("retester_receipt_invalid", "native Retester gateway returned an invalid success receipt")

    launcher_sha = _digest(receipt["launcher_sha256"], "historical_result_launcher_invalid")
    receipts = tuple(dict(item) for item in receipt["receipts"])
    try:
        _, _, completed_engine_sha = _read_exact_inside(
            home,
            RETESTER_ENGINE_RELATIVE_PATH,
            missing_code="retester_engine_missing",
            escape_code="retester_engine_path_escape",
        )
        if completed_engine_sha != engine_sha:
            raise ResearchRetesterError(
                "retester_engine_changed_during_execution",
                "installed SQTradingLib.jar changed across native Retester execution",
            )
        result_bytes, result_info = _capture_result(home, project_name)
        if result_info["archive_sha256"] == candidate_sha:
            raise ResearchRetesterError("retester_result_unchanged", "Retester execution completed but native result archive did not change")
    except ResearchRetesterError as exc:
        _failed_successor(
            store,
            entity,
            prepared_revision.revision,
            prepared,
            reason_code=exc.code,
            launcher_sha256=launcher_sha,
            receipts=receipts,
            partial_side_effect=True,
        )
        raise

    result_strategy = _member(result_bytes, "strategy_Portfolio.xml")
    result_settings = _member(result_bytes, "settings.xml")
    result_ref = store.put_evidence(result_bytes)
    result_strategy_ref = store.put_evidence(result_strategy)
    result_settings_ref = store.put_evidence(result_settings)
    completed = NativeRetesterContent(
        state="completed",
        candidate_entity_id=candidate_entity_id,
        candidate_revision=expected_candidate_revision,
        candidate_archive_name=candidate["archive_name"],
        candidate_archive_ref=candidate_ref,
        candidate_archive_sha256=candidate_sha,
        sqx_build=SQX_BUILD,
        operation=RETESTER_OPERATION,
        retester_task=RETESTER_TASK,
        native_project_name=project_name,
        native_project_relative_path=project_relative,
        source_project_ref=source_project_ref,
        source_project_sha256=project_sha,
        engine_ref=engine_ref,
        engine_sha256=engine_sha,
        launcher_sha256=launcher_sha,
        receipts=receipts,
        partial_side_effect=False,
        result_archive_name=result_info["archive"],
        result_archive_relative_path=result_info["relative_path"],
        result_archive_ref=result_ref,
        result_archive_sha256=result_info["archive_sha256"],
        result_strategy_ref=result_strategy_ref,
        result_strategy_sha256=result_info["strategy_entry_sha256"],
        result_settings_ref=result_settings_ref,
        result_settings_sha256=result_info["settings_entry_sha256"],
    )
    completed_revision = store.create_revision(
        entity,
        completed.canonical_bytes(),
        parent_revision=prepared_revision.revision,
        evidence=(candidate_ref, source_project_ref, engine_ref, result_ref, result_strategy_ref, result_settings_ref),
    )
    store.compare_and_set_current(
        entity,
        expected_revision=prepared_revision.revision,
        target_revision=completed_revision.revision,
    )
    return {**_record(store, entity, completed_revision.revision), "reused": False}
