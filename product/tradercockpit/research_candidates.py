"""Immutable Candidate custody for exact native SQX Builder output archives.

StrategyQuant X remains the strategy producer. TraderCockpit records an explicit
operator association between one exact submitted native-job revision and one exact
native Builder Results archive. SQX 144.2953 output archives do not expose a
machine-readable TraderCockpit job id, so this module never claims an inferred
producer-side job->archive relation.
"""

from __future__ import annotations

from dataclasses import dataclass
from io import BytesIO
import json
from pathlib import Path
import re
from uuid import UUID
from zipfile import BadZipFile, ZipFile

from tradercockpit.research_custody import (
    EvidenceRef,
    FileResearchCustodyStore,
    ResearchCustodyError,
    ResearchEntityId,
    ResearchKind,
    ResearchRevisionRef,
)
from tradercockpit.research_native_jobs import read_current_native_job
from tradercockpit.sqx_outputs import (
    SqxOutputError,
    capture_sqx_output_archive,
    inspect_sqx_output_bytes,
)
from tradercockpit.sqx_presets import SQX_BUILD


CANDIDATE_CONTENT_SCHEMA = "tc.research-candidate-content.v1"
CANDIDATE_READ_SCHEMA = "tc.research-candidate.v1"
CANDIDATE_CATALOG_SCHEMA = "tc.research-candidate-catalog.v1"
CANDIDATE_ASSOCIATION_MODE = "operator_selected_exact_native_output"
_CURRENT_POINTER_TEMP_RE = re.compile(
    r"^\.[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.json\.tmp-[0-9]+-[0-9a-f]{32}$"
)
_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")


class ResearchCandidateError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


def _canonical(payload: dict[str, object]) -> bytes:
    return json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def _digest(value: object, *, code: str) -> str:
    if not isinstance(value, str) or not _DIGEST_RE.fullmatch(value):
        raise ResearchCandidateError(code, "expected a lowercase 64-character SHA-256 digest")
    return value


def _candidate_entity(value: ResearchEntityId | str) -> ResearchEntityId:
    try:
        entity = value if isinstance(value, ResearchEntityId) else ResearchEntityId.parse(value)
    except ResearchCustodyError as exc:
        raise ResearchCandidateError("candidate_entity_invalid", "candidate entity identity is invalid") from exc
    if entity.kind != ResearchKind.CANDIDATE:
        raise ResearchCandidateError("candidate_entity_invalid", "research entity is not a candidate")
    return entity


def _parse_typed_revision(value: str, kind: ResearchKind, code: str) -> ResearchRevisionRef:
    try:
        revision = ResearchRevisionRef.parse(value)
    except ResearchCustodyError as exc:
        raise ResearchCandidateError(code, "revision identity is invalid") from exc
    if revision.kind != kind:
        raise ResearchCandidateError(code, f"revision must be {kind.value} custody")
    return revision


def _member(snapshot: bytes, name: str) -> bytes:
    try:
        with ZipFile(BytesIO(snapshot)) as archive:
            matches = [entry for entry in archive.infolist() if entry.filename == name]
            if len(matches) != 1:
                raise ResearchCandidateError("candidate_content_corrupt", f"candidate archive must contain exactly one {name}")
            value = archive.read(matches[0])
    except (BadZipFile, RuntimeError, NotImplementedError, EOFError, OSError) as exc:
        raise ResearchCandidateError("candidate_content_corrupt", f"candidate archive member {name} is unreadable") from exc
    if not value:
        raise ResearchCandidateError("candidate_content_corrupt", f"candidate archive member {name} is empty")
    return value


@dataclass(frozen=True, slots=True)
class CandidateContent:
    native_job_entity_id: str
    native_job_revision: str
    configuration_entity_id: str
    configuration_revision: str
    association_mode: str
    archive_name: str
    archive_relative_path: str
    archive_ref: EvidenceRef
    archive_sha256: str
    strategy_ref: EvidenceRef
    strategy_sha256: str
    settings_ref: EvidenceRef
    settings_sha256: str
    sqx_build: str

    def __post_init__(self) -> None:
        try:
            native_job = ResearchEntityId.parse(self.native_job_entity_id)
            configuration = ResearchEntityId.parse(self.configuration_entity_id)
        except ResearchCustodyError as exc:
            raise ResearchCandidateError("candidate_provenance_invalid", "candidate provenance entity identity is invalid") from exc
        if native_job.kind != ResearchKind.NATIVE_JOB or configuration.kind != ResearchKind.CONFIGURATION:
            raise ResearchCandidateError("candidate_provenance_invalid", "candidate provenance entity kind is invalid")
        _parse_typed_revision(self.native_job_revision, ResearchKind.NATIVE_JOB, "candidate_provenance_invalid")
        _parse_typed_revision(self.configuration_revision, ResearchKind.CONFIGURATION, "candidate_provenance_invalid")
        if self.association_mode != CANDIDATE_ASSOCIATION_MODE:
            raise ResearchCandidateError("candidate_association_invalid", "candidate native output association mode is invalid")
        if not isinstance(self.archive_name, str) or not self.archive_name or Path(self.archive_name).name != self.archive_name or not self.archive_name.lower().endswith(".sqx"):
            raise ResearchCandidateError("candidate_archive_invalid", "candidate archive name is invalid")
        expected_relative = f"user/projects/Builder/databanks/Results/{self.archive_name}"
        if self.archive_relative_path != expected_relative:
            raise ResearchCandidateError("candidate_archive_invalid", "candidate archive relative path is invalid")
        if self.sqx_build != SQX_BUILD:
            raise ResearchCandidateError("candidate_archive_invalid", "candidate SQX build identity is invalid")
        bindings = (
            (self.archive_ref, self.archive_sha256, "candidate_archive_invalid"),
            (self.strategy_ref, self.strategy_sha256, "candidate_strategy_invalid"),
            (self.settings_ref, self.settings_sha256, "candidate_settings_invalid"),
        )
        for ref, digest, code in bindings:
            if not isinstance(ref, EvidenceRef) or ref.digest != _digest(digest, code=code):
                raise ResearchCandidateError(code, "candidate evidence reference does not match its digest")

    def canonical_bytes(self) -> bytes:
        return _canonical(
            {
                "archive_name": self.archive_name,
                "archive_ref": str(self.archive_ref),
                "archive_relative_path": self.archive_relative_path,
                "archive_sha256": self.archive_sha256,
                "association_mode": self.association_mode,
                "configuration_entity_id": self.configuration_entity_id,
                "configuration_revision": self.configuration_revision,
                "native_job_entity_id": self.native_job_entity_id,
                "native_job_revision": self.native_job_revision,
                "schema": CANDIDATE_CONTENT_SCHEMA,
                "settings_ref": str(self.settings_ref),
                "settings_sha256": self.settings_sha256,
                "sqx_build": self.sqx_build,
                "strategy_ref": str(self.strategy_ref),
                "strategy_sha256": self.strategy_sha256,
            }
        )

    @classmethod
    def from_bytes(cls, data: bytes) -> "CandidateContent":
        try:
            payload = json.loads(data)
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise ResearchCandidateError("candidate_content_corrupt", "candidate content is not valid JSON") from exc
        expected = {
            "archive_name",
            "archive_ref",
            "archive_relative_path",
            "archive_sha256",
            "association_mode",
            "configuration_entity_id",
            "configuration_revision",
            "native_job_entity_id",
            "native_job_revision",
            "schema",
            "settings_ref",
            "settings_sha256",
            "sqx_build",
            "strategy_ref",
            "strategy_sha256",
        }
        if not isinstance(payload, dict) or set(payload) != expected or payload.get("schema") != CANDIDATE_CONTENT_SCHEMA:
            raise ResearchCandidateError("candidate_content_corrupt", "candidate content schema is invalid")
        try:
            return cls(
                native_job_entity_id=payload["native_job_entity_id"],
                native_job_revision=payload["native_job_revision"],
                configuration_entity_id=payload["configuration_entity_id"],
                configuration_revision=payload["configuration_revision"],
                association_mode=payload["association_mode"],
                archive_name=payload["archive_name"],
                archive_relative_path=payload["archive_relative_path"],
                archive_ref=EvidenceRef.parse(payload["archive_ref"]),
                archive_sha256=payload["archive_sha256"],
                strategy_ref=EvidenceRef.parse(payload["strategy_ref"]),
                strategy_sha256=payload["strategy_sha256"],
                settings_ref=EvidenceRef.parse(payload["settings_ref"]),
                settings_sha256=payload["settings_sha256"],
                sqx_build=payload["sqx_build"],
            )
        except (KeyError, TypeError, ResearchCustodyError, ResearchCandidateError) as exc:
            detail = getattr(exc, "detail", "candidate content fields are invalid")
            raise ResearchCandidateError("candidate_content_corrupt", str(detail)) from exc


def _candidate_entities(store: FileResearchCustodyStore) -> tuple[ResearchEntityId, ...]:
    directory = store.base / "current" / ResearchKind.CANDIDATE.value
    if not directory.exists():
        return ()
    if not directory.is_dir():
        raise ResearchCustodyError("current_pointer_corrupt", "candidate current-pointer directory is invalid")
    entities: list[ResearchEntityId] = []
    for path in sorted(directory.iterdir(), key=lambda item: item.name):
        if _CURRENT_POINTER_TEMP_RE.fullmatch(path.name):
            continue
        if not path.is_file() or path.suffix != ".json":
            raise ResearchCustodyError("current_pointer_corrupt", "candidate current-pointer directory contains an unexpected entry")
        try:
            value = UUID(path.stem)
        except ValueError as exc:
            raise ResearchCustodyError("current_pointer_corrupt", "candidate current-pointer filename is not a canonical UUID") from exc
        if str(value) != path.stem:
            raise ResearchCustodyError("current_pointer_corrupt", "candidate current-pointer UUID is not canonical")
        entity = ResearchEntityId(ResearchKind.CANDIDATE, value)
        store.current(entity)
        entities.append(entity)
    return tuple(entities)


def _record(store: FileResearchCustodyStore, entity: ResearchEntityId, revision: ResearchRevisionRef) -> dict[str, object]:
    stored = store.read_revision(revision)
    if stored.entity_id != entity or stored.parent_revision is not None:
        raise ResearchCandidateError("candidate_content_corrupt", "candidate revision custody identity is invalid")
    content = CandidateContent.from_bytes(store.read_revision_content(revision))
    archive = store.read_evidence(content.archive_ref)
    strategy = store.read_evidence(content.strategy_ref)
    settings = store.read_evidence(content.settings_ref)
    inspected = inspect_sqx_output_bytes(archive, archive_name=content.archive_name)
    if (
        inspected["archive_sha256"] != content.archive_sha256
        or inspected["strategy_entry_sha256"] != content.strategy_sha256
        or inspected["settings_entry_sha256"] != content.settings_sha256
        or _member(archive, "strategy_Portfolio.xml") != strategy
        or _member(archive, "settings.xml") != settings
    ):
        raise ResearchCandidateError("candidate_content_corrupt", "candidate archive/evidence binding is invalid")
    if set(stored.evidence) != {content.archive_ref, content.strategy_ref, content.settings_ref}:
        raise ResearchCandidateError("candidate_content_corrupt", "candidate revision evidence set is invalid")
    return {
        "schema": CANDIDATE_READ_SCHEMA,
        "entity_id": str(entity),
        "revision": str(revision),
        "native_job_entity_id": content.native_job_entity_id,
        "native_job_revision": content.native_job_revision,
        "configuration_entity_id": content.configuration_entity_id,
        "configuration_revision": content.configuration_revision,
        "association_mode": content.association_mode,
        "archive_name": content.archive_name,
        "archive_relative_path": content.archive_relative_path,
        "archive_ref": str(content.archive_ref),
        "archive_sha256": content.archive_sha256,
        "strategy_ref": str(content.strategy_ref),
        "strategy_sha256": content.strategy_sha256,
        "settings_ref": str(content.settings_ref),
        "settings_sha256": content.settings_sha256,
        "sqx_build": content.sqx_build,
    }


def list_current_candidates(store: FileResearchCustodyStore) -> dict[str, object]:
    return {
        "schema": CANDIDATE_CATALOG_SCHEMA,
        "candidates": [
            _record(store, entity, store.current(entity))
            for entity in _candidate_entities(store)
        ],
    }


def read_current_candidate(store: FileResearchCustodyStore, entity_id: ResearchEntityId | str) -> dict[str, object]:
    entity = _candidate_entity(entity_id)
    return _record(store, entity, store.current(entity))


def _existing_candidate(
    store: FileResearchCustodyStore,
    *,
    native_job_revision: str,
    archive_sha256: str,
) -> dict[str, object] | None:
    matches = [
        item
        for item in list_current_candidates(store)["candidates"]
        if item["native_job_revision"] == native_job_revision and item["archive_sha256"] == archive_sha256
    ]
    if len(matches) > 1:
        raise ResearchCandidateError("candidate_duplicate", "multiple candidates bind the same native job revision and archive")
    return matches[0] if matches else None


def import_native_candidate(
    store: FileResearchCustodyStore,
    sqx_home: Path | str | None,
    *,
    native_job_entity_id: str,
    expected_native_job_revision: str,
    archive_name: str,
    expected_archive_sha256: str,
) -> dict[str, object]:
    """Import one exact native Results archive and explicitly bind it to a submitted job."""

    job = read_current_native_job(store, native_job_entity_id)
    if job.get("revision") != expected_native_job_revision:
        raise ResearchCustodyError("current_conflict", "native job revision changed before candidate import")
    if job.get("state") != "submitted":
        raise ResearchCandidateError("candidate_native_job_not_submitted", "candidate import requires an exact submitted native job revision")

    existing = _existing_candidate(
        store,
        native_job_revision=expected_native_job_revision,
        archive_sha256=_digest(expected_archive_sha256, code="candidate_archive_invalid"),
    )
    if existing is not None:
        return {**existing, "reused": True}

    try:
        archive, inspected = capture_sqx_output_archive(
            sqx_home,
            archive_name,
            expected_archive_sha256=expected_archive_sha256,
        )
    except SqxOutputError as exc:
        raise ResearchCandidateError(exc.code, exc.detail) from exc

    strategy = _member(archive, "strategy_Portfolio.xml")
    settings = _member(archive, "settings.xml")
    archive_ref = store.put_evidence(archive)
    strategy_ref = store.put_evidence(strategy)
    settings_ref = store.put_evidence(settings)
    content = CandidateContent(
        native_job_entity_id=native_job_entity_id,
        native_job_revision=expected_native_job_revision,
        configuration_entity_id=job["configuration_entity_id"],
        configuration_revision=job["configuration_revision"],
        association_mode=CANDIDATE_ASSOCIATION_MODE,
        archive_name=archive_name,
        archive_relative_path=inspected["relative_path"],
        archive_ref=archive_ref,
        archive_sha256=inspected["archive_sha256"],
        strategy_ref=strategy_ref,
        strategy_sha256=inspected["strategy_entry_sha256"],
        settings_ref=settings_ref,
        settings_sha256=inspected["settings_entry_sha256"],
        sqx_build=inspected["native_version"],
    )
    entity = store.create_entity(ResearchKind.CANDIDATE)
    stored = store.create_revision(
        entity,
        content.canonical_bytes(),
        evidence=(archive_ref, strategy_ref, settings_ref),
    )
    store.compare_and_set_current(entity, expected_revision=None, target_revision=stored.revision)
    return {**_record(store, entity, stored.revision), "reused": False}
