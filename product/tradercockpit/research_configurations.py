"""Research Construct/Build configuration assembly and approval custody.

This module assembles one immutable configuration candidate from the exact native
SQX Builder task bytes already present in the verified Builder project archive.
TraderCockpit owns custody, review, diff, and approval mechanics only. It does not
reinterpret SQX settings, synthesize producer defaults, or launch native compute.
"""

from __future__ import annotations

from dataclasses import dataclass
from hashlib import sha256
from io import BytesIO
import json
import os
import re
from uuid import UUID
from zipfile import BadZipFile, ZipFile
import zlib

from tradercockpit.research_custody import (
    EvidenceRef,
    FileResearchCustodyStore,
    ResearchCustodyError,
    ResearchEntityId,
    ResearchKind,
    ResearchRevisionRef,
)
from tradercockpit.sqx_builder_config import (
    SQX_BUILDER_PROJECT_RELATIVE_PATH,
    SqxBuilderConfigError,
    SqxBuilderProjectConfig,
    _matches_retained_native_reference as _matches_retained_builder_reference,
    builder_project_specification_record,
    read_sqx_builder_project,
    validate_sqx_builder_project_snapshot,
)
from tradercockpit.sqx_presets import SQX_BUILD, verified_sqx_home


CONFIGURATION_CONTENT_SCHEMA = "tc.research-configuration-content.v1"
CONFIGURATION_READ_SCHEMA = "tc.research-configuration.v1"
CONFIGURATION_CATALOG_SCHEMA = "tc.research-configuration-catalog.v1"
CONFIGURATION_SOURCE_ENTRY = "Build-Task1.xml"
CONFIGURATION_ASSEMBLY_MODE = "exact_native_builder_task_snapshot"
_CURRENT_POINTER_TEMP_RE = re.compile(
    r"^\.[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.json\.tmp-[0-9]+-[0-9a-f]{32}$"
)


class ResearchConfigurationError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


@dataclass(frozen=True, slots=True)
class ResearchConfigurationContent:
    state: str
    sqx_build: str
    source_project_path: str
    source_project_sha256: str
    source_project_ref: EvidenceRef
    source_entry: str
    source_entry_ref: EvidenceRef
    executable_xml_ref: EvidenceRef
    assembly_mode: str
    approved_changes: tuple[str, ...]
    review_summary: str
    approved_from_revision: str | None = None

    def __post_init__(self) -> None:
        if self.state not in {"compiled", "approved"}:
            raise ResearchConfigurationError("configuration_state_invalid", "configuration state is invalid")
        if self.sqx_build != SQX_BUILD:
            raise ResearchConfigurationError("configuration_build_invalid", "configuration must bind the verified SQX build")
        if self.source_project_path != SQX_BUILDER_PROJECT_RELATIVE_PATH:
            raise ResearchConfigurationError("configuration_source_invalid", "configuration must bind the canonical Builder project")
        if len(self.source_project_sha256) != 64 or any(ch not in "0123456789abcdef" for ch in self.source_project_sha256):
            raise ResearchConfigurationError("configuration_source_invalid", "source project SHA-256 is invalid")
        if not isinstance(self.source_project_ref, EvidenceRef) or self.source_project_ref.digest != self.source_project_sha256:
            raise ResearchConfigurationError(
                "configuration_source_invalid",
                "source project evidence must bind the exact source project SHA-256",
            )
        if self.source_entry != CONFIGURATION_SOURCE_ENTRY:
            raise ResearchConfigurationError("configuration_source_invalid", "configuration source entry is invalid")
        if self.executable_xml_ref != self.source_entry_ref:
            raise ResearchConfigurationError(
                "configuration_transform_unsupported",
                "this slice approves only exact native task bytes; transformed XML is not supported",
            )
        if self.assembly_mode != CONFIGURATION_ASSEMBLY_MODE:
            raise ResearchConfigurationError("configuration_assembly_invalid", "configuration assembly mode is invalid")
        if self.approved_changes:
            raise ResearchConfigurationError(
                "configuration_changes_unsupported",
                "typed configuration changes are not enabled in this bounded exact-snapshot slice",
            )
        if not isinstance(self.review_summary, str) or not self.review_summary:
            raise ResearchConfigurationError("configuration_review_invalid", "configuration review summary is required")
        if self.state == "compiled" and self.approved_from_revision is not None:
            raise ResearchConfigurationError("configuration_approval_invalid", "compiled revision cannot contain approval provenance")
        if self.state == "approved" and (
            not isinstance(self.approved_from_revision, str) or not self.approved_from_revision
        ):
            raise ResearchConfigurationError("configuration_approval_invalid", "approved revision must bind its compiled parent")

    def canonical_bytes(self) -> bytes:
        return json.dumps(
            {
                "approved_changes": list(self.approved_changes),
                "approved_from_revision": self.approved_from_revision,
                "assembly_mode": self.assembly_mode,
                "executable_xml_ref": str(self.executable_xml_ref),
                "review_summary": self.review_summary,
                "schema": CONFIGURATION_CONTENT_SCHEMA,
                "source_entry": self.source_entry,
                "source_entry_ref": str(self.source_entry_ref),
                "source_project_path": self.source_project_path,
                "source_project_ref": str(self.source_project_ref),
                "source_project_sha256": self.source_project_sha256,
                "sqx_build": self.sqx_build,
                "state": self.state,
            },
            ensure_ascii=False,
            sort_keys=True,
            separators=(",", ":"),
        ).encode("utf-8")

    @classmethod
    def from_bytes(cls, data: bytes) -> "ResearchConfigurationContent":
        try:
            payload = json.loads(data)
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise ResearchConfigurationError("configuration_content_corrupt", "configuration content is not valid JSON") from exc
        expected = {
            "approved_changes",
            "approved_from_revision",
            "assembly_mode",
            "executable_xml_ref",
            "review_summary",
            "schema",
            "source_entry",
            "source_entry_ref",
            "source_project_path",
            "source_project_ref",
            "source_project_sha256",
            "sqx_build",
            "state",
        }
        if not isinstance(payload, dict) or set(payload) != expected or payload.get("schema") != CONFIGURATION_CONTENT_SCHEMA:
            raise ResearchConfigurationError("configuration_content_corrupt", "configuration content schema is invalid")
        changes = payload.get("approved_changes")
        if not isinstance(changes, list) or any(not isinstance(item, str) for item in changes):
            raise ResearchConfigurationError("configuration_content_corrupt", "approved changes are invalid")
        try:
            return cls(
                state=payload["state"],
                sqx_build=payload["sqx_build"],
                source_project_path=payload["source_project_path"],
                source_project_sha256=payload["source_project_sha256"],
                source_project_ref=EvidenceRef.parse(payload["source_project_ref"]),
                source_entry=payload["source_entry"],
                source_entry_ref=EvidenceRef.parse(payload["source_entry_ref"]),
                executable_xml_ref=EvidenceRef.parse(payload["executable_xml_ref"]),
                assembly_mode=payload["assembly_mode"],
                approved_changes=tuple(changes),
                review_summary=payload["review_summary"],
                approved_from_revision=payload["approved_from_revision"],
            )
        except (ResearchConfigurationError, ResearchCustodyError, KeyError, TypeError) as exc:
            detail = getattr(exc, "detail", "configuration content fields are invalid")
            raise ResearchConfigurationError("configuration_content_corrupt", str(detail)) from exc


def _configuration_entity(value: ResearchEntityId | str) -> ResearchEntityId:
    entity = value if isinstance(value, ResearchEntityId) else ResearchEntityId.parse(value)
    if entity.kind != ResearchKind.CONFIGURATION:
        raise ResearchConfigurationError("configuration_entity_invalid", "research entity is not a configuration")
    return entity


def _configuration_revision(value: ResearchRevisionRef | str) -> ResearchRevisionRef:
    revision = value if isinstance(value, ResearchRevisionRef) else ResearchRevisionRef.parse(value)
    if revision.kind != ResearchKind.CONFIGURATION:
        raise ResearchConfigurationError("configuration_revision_invalid", "research revision is not a configuration revision")
    return revision


def _archive_task_snapshot(archive_snapshot: bytes) -> bytes:
    try:
        with ZipFile(BytesIO(archive_snapshot)) as archive:
            names = archive.namelist()
            if len(names) != len(set(names)):
                raise ResearchConfigurationError(
                    "configuration_source_invalid",
                    "Builder project contains duplicate archive members",
                )
            if CONFIGURATION_SOURCE_ENTRY not in names:
                raise ResearchConfigurationError(
                    "configuration_source_invalid",
                    "Builder project does not contain the native Build task entry",
                )
            task_snapshot = archive.read(CONFIGURATION_SOURCE_ENTRY)
    except ResearchConfigurationError:
        raise
    except (BadZipFile, RuntimeError, NotImplementedError, OSError, EOFError, zlib.error) as exc:
        raise ResearchConfigurationError(
            "configuration_source_invalid",
            "Builder project is not a readable native archive",
        ) from exc
    if not task_snapshot.strip():
        raise ResearchConfigurationError("configuration_source_invalid", "native Build task entry is empty")
    return task_snapshot


def _exact_native_task_snapshot(
    config: SqxBuilderProjectConfig,
    sqx_home,
) -> tuple[bytes, bytes]:
    home_before = verified_sqx_home(sqx_home)
    try:
        expected_before = (home_before / SQX_BUILDER_PROJECT_RELATIVE_PATH).resolve(strict=True)
        expected_before.relative_to(home_before)
        resolved_before = config.archive_path.resolve(strict=True)
        resolved_before.relative_to(home_before)
    except (OSError, RuntimeError, ValueError) as exc:
        raise ResearchConfigurationError(
            "configuration_source_moved",
            "Builder project is not the exact verified in-runtime physical path before capture",
        ) from exc
    if resolved_before != expected_before:
        raise ResearchConfigurationError(
            "configuration_source_moved",
            "Builder project is not the exact verified in-runtime physical path before capture",
        )

    try:
        with resolved_before.open("rb") as handle:
            opened_stat = os.fstat(handle.fileno())
            archive_snapshot = handle.read()
    except (OSError, RuntimeError, ValueError) as exc:
        raise ResearchConfigurationError(
            "configuration_source_unreadable",
            "Builder project snapshot could not be read from the inspected physical path",
        ) from exc

    home_after = verified_sqx_home(sqx_home)
    try:
        expected_after = (home_after / SQX_BUILDER_PROJECT_RELATIVE_PATH).resolve(strict=True)
        expected_after.relative_to(home_after)
        resolved_after = config.archive_path.resolve(strict=True)
        resolved_after.relative_to(home_after)
        path_stat = resolved_after.stat()
    except (OSError, RuntimeError, ValueError) as exc:
        raise ResearchConfigurationError(
            "configuration_source_moved",
            "Builder project physical path changed during configuration assembly",
        ) from exc

    if (
        expected_before != expected_after
        or resolved_before != resolved_after
        or resolved_after != expected_after
        or (opened_stat.st_dev, opened_stat.st_ino) != (path_stat.st_dev, path_stat.st_ino)
    ):
        raise ResearchConfigurationError(
            "configuration_source_moved",
            "Builder project no longer matches the exact verified in-runtime physical path",
        )

    observed_sha = sha256(archive_snapshot).hexdigest()
    if observed_sha != config.archive_sha256:
        raise ResearchConfigurationError(
            "configuration_source_moved",
            "Builder project changed between configuration inspection and assembly",
        )
    return archive_snapshot, _archive_task_snapshot(archive_snapshot)


def _approved_identity(content: ResearchConfigurationContent) -> tuple[object, ...]:
    return (
        content.sqx_build,
        content.source_project_path,
        content.source_project_sha256,
        content.source_project_ref,
        content.source_entry,
        content.source_entry_ref,
        content.executable_xml_ref,
        content.assembly_mode,
        content.approved_changes,
        content.review_summary,
    )


def _record(
    store: FileResearchCustodyStore,
    entity: ResearchEntityId,
    revision: ResearchRevisionRef,
) -> dict[str, object]:
    stored = store.read_revision(revision)
    if stored.entity_id != entity:
        raise ResearchConfigurationError("configuration_revision_invalid", "configuration revision belongs to another entity")
    content = ResearchConfigurationContent.from_bytes(store.read_revision_content(revision))

    source_project_bytes = store.read_evidence(content.source_project_ref)
    if sha256(source_project_bytes).hexdigest() != content.source_project_sha256:
        raise ResearchConfigurationError("configuration_content_corrupt", "source project evidence identity is invalid")
    xml_bytes = store.read_evidence(content.executable_xml_ref)
    if EvidenceRef.from_bytes(xml_bytes) != content.executable_xml_ref:
        raise ResearchConfigurationError("configuration_content_corrupt", "executable XML evidence identity is invalid")
    try:
        validate_sqx_builder_project_snapshot(source_project_bytes)
        archived_task = _archive_task_snapshot(source_project_bytes)
    except (ResearchConfigurationError, SqxBuilderConfigError) as exc:
        raise ResearchConfigurationError(
            "configuration_content_corrupt",
            "source project evidence is not a valid native Builder project archive",
        ) from exc
    if archived_task != xml_bytes or EvidenceRef.from_bytes(archived_task) != content.source_entry_ref:
        raise ResearchConfigurationError(
            "configuration_content_corrupt",
            "executable XML evidence does not match Build-Task1.xml in the bound source archive",
        )
    if not _matches_retained_builder_reference(source_project_bytes):
        raise ResearchConfigurationError(
            "configuration_content_corrupt",
            "source project evidence does not match the retained SQX 144.2953 Builder reference",
        )
    if set(stored.evidence) != {content.source_project_ref, content.source_entry_ref}:
        raise ResearchConfigurationError(
            "configuration_content_corrupt",
            "configuration revision evidence does not match its bound source artifacts",
        )

    if content.state == "compiled":
        if stored.parent_revision is not None:
            raise ResearchConfigurationError("configuration_content_corrupt", "compiled configuration cannot have a parent revision")
    else:
        try:
            approved_parent = _configuration_revision(content.approved_from_revision or "")
        except (ResearchConfigurationError, ResearchCustodyError) as exc:
            raise ResearchConfigurationError(
                "configuration_content_corrupt",
                "approved configuration parent identity is invalid",
            ) from exc
        if stored.parent_revision != approved_parent:
            raise ResearchConfigurationError(
                "configuration_content_corrupt",
                "approved configuration provenance does not match its actual parent revision",
            )
        parent_stored = store.read_revision(approved_parent)
        parent_content = ResearchConfigurationContent.from_bytes(store.read_revision_content(approved_parent))
        if (
            parent_stored.entity_id != entity
            or parent_stored.parent_revision is not None
            or parent_content.state != "compiled"
            or parent_content.approved_from_revision is not None
            or parent_stored.evidence != stored.evidence
            or _approved_identity(parent_content) != _approved_identity(content)
        ):
            raise ResearchConfigurationError(
                "configuration_content_corrupt",
                "approved configuration must preserve the exact compiled parent custody and executable identity",
            )

    return {
        "schema": CONFIGURATION_READ_SCHEMA,
        "entity_id": str(entity),
        "revision": str(revision),
        "parent_revision": str(stored.parent_revision) if stored.parent_revision else None,
        "content_ref": str(stored.content),
        "state": content.state,
        "sqx_build": content.sqx_build,
        "source_project_path": content.source_project_path,
        "source_project_sha256": content.source_project_sha256,
        "source_project_ref": str(content.source_project_ref),
        "source_entry": content.source_entry,
        "source_entry_ref": str(content.source_entry_ref),
        "executable_xml_ref": str(content.executable_xml_ref),
        "executable_xml_sha256": content.executable_xml_ref.digest,
        "assembly_mode": content.assembly_mode,
        "approved_changes": list(content.approved_changes),
        "review": {
            "changed": False,
            "summary": content.review_summary,
        },
        "approval": {
            "approved": content.state == "approved",
            "approved_from_revision": content.approved_from_revision,
        },
        "launch": {
            "enabled": False,
            "reason_code": "native_launch_not_in_this_slice",
        },
    }


def _current_configuration_entities(store: FileResearchCustodyStore) -> tuple[ResearchEntityId, ...]:
    directory = store.base / "current" / ResearchKind.CONFIGURATION.value
    if not directory.exists():
        return ()
    if not directory.is_dir():
        raise ResearchCustodyError(
            "current_pointer_corrupt",
            "configuration current-pointer directory is invalid",
        )

    entities: list[ResearchEntityId] = []
    for path in sorted(directory.iterdir(), key=lambda item: item.name):
        if _CURRENT_POINTER_TEMP_RE.fullmatch(path.name):
            continue
        if not path.is_file() or path.suffix != ".json":
            raise ResearchCustodyError(
                "current_pointer_corrupt",
                "configuration current-pointer directory contains an unexpected entry",
            )
        try:
            value = UUID(path.stem)
        except ValueError as exc:
            raise ResearchCustodyError(
                "current_pointer_corrupt",
                "configuration current-pointer filename is not a canonical UUID",
            ) from exc
        if str(value) != path.stem:
            raise ResearchCustodyError(
                "current_pointer_corrupt",
                "configuration current-pointer UUID is not canonical",
            )
        entity = ResearchEntityId(ResearchKind.CONFIGURATION, value)
        store.current(entity)
        entities.append(entity)
    return tuple(entities)


def list_current_configurations(store: FileResearchCustodyStore) -> dict[str, object]:
    configurations: list[dict[str, object]] = []
    for entity in _current_configuration_entities(store):
        record = read_current_configuration(store, entity)
        configurations.append(
            {
                "entity_id": record["entity_id"],
                "revision": record["revision"],
                "state": record["state"],
                "source_project_sha256": record["source_project_sha256"],
                "executable_xml_sha256": record["executable_xml_sha256"],
            }
        )
    return {
        "schema": CONFIGURATION_CATALOG_SCHEMA,
        "configurations": configurations,
    }


def compile_current_builder_configuration(
    store: FileResearchCustodyStore,
    sqx_home,
) -> dict[str, object]:
    """Assemble exact current native Builder task bytes into immutable custody."""

    config = read_sqx_builder_project(sqx_home)
    specification = builder_project_specification_record(config)
    build_gate = specification.get("build_gate")
    if not isinstance(build_gate, dict) or build_gate.get("locked") is not False:
        reasons = build_gate.get("reason_codes", []) if isinstance(build_gate, dict) else []
        detail = ", ".join(str(item) for item in reasons) or "native validation evidence is unresolved"
        raise ResearchConfigurationError(
            "configuration_specification_locked",
            f"Builder configuration cannot compile while Specification is locked: {detail}",
        )

    archive_snapshot, task_snapshot = _exact_native_task_snapshot(config, sqx_home)
    archive_ref = store.put_evidence(archive_snapshot)
    task_ref = store.put_evidence(task_snapshot)
    entity = store.create_entity(ResearchKind.CONFIGURATION)
    content = ResearchConfigurationContent(
        state="compiled",
        sqx_build=SQX_BUILD,
        source_project_path=SQX_BUILDER_PROJECT_RELATIVE_PATH,
        source_project_sha256=config.archive_sha256,
        source_project_ref=archive_ref,
        source_entry=CONFIGURATION_SOURCE_ENTRY,
        source_entry_ref=task_ref,
        executable_xml_ref=task_ref,
        assembly_mode=CONFIGURATION_ASSEMBLY_MODE,
        approved_changes=(),
        review_summary="Executable candidate is byte-identical to the retained native SQX 144.2953 Build-Task1.xml snapshot; no TraderCockpit changes applied.",
    )
    revision = store.create_revision(
        entity,
        content.canonical_bytes(),
        evidence=(archive_ref, task_ref),
    )
    store.compare_and_set_current(entity, expected_revision=None, target_revision=revision.revision)
    return _record(store, entity, revision.revision)


def read_current_configuration(
    store: FileResearchCustodyStore,
    entity_id: ResearchEntityId | str,
) -> dict[str, object]:
    entity = _configuration_entity(entity_id)
    return _record(store, entity, store.current(entity))


def approve_configuration(
    store: FileResearchCustodyStore,
    *,
    entity_id: ResearchEntityId | str,
    expected_revision: ResearchRevisionRef | str,
) -> dict[str, object]:
    """Approve the exact compiled byte identity with compare-and-set protection."""

    entity = _configuration_entity(entity_id)
    expected = _configuration_revision(expected_revision)
    current = store.current(entity)
    if current != expected:
        raise ResearchCustodyError("current_conflict", "configuration revision changed before approval")
    stored = store.read_revision(expected)
    _record(store, entity, expected)
    content = ResearchConfigurationContent.from_bytes(store.read_revision_content(expected))
    if content.state != "compiled":
        raise ResearchConfigurationError("configuration_already_approved", "only a compiled configuration can be approved")
    approved = ResearchConfigurationContent(
        state="approved",
        sqx_build=content.sqx_build,
        source_project_path=content.source_project_path,
        source_project_sha256=content.source_project_sha256,
        source_project_ref=content.source_project_ref,
        source_entry=content.source_entry,
        source_entry_ref=content.source_entry_ref,
        executable_xml_ref=content.executable_xml_ref,
        assembly_mode=content.assembly_mode,
        approved_changes=content.approved_changes,
        review_summary=content.review_summary,
        approved_from_revision=str(expected),
    )
    revision = store.create_revision(
        entity,
        approved.canonical_bytes(),
        parent_revision=expected,
        evidence=stored.evidence,
    )
    store.compare_and_set_current(entity, expected_revision=expected, target_revision=revision.revision)
    return _record(store, entity, revision.revision)