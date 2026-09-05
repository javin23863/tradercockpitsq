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
from uuid import UUID, NAMESPACE_URL, uuid5
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
CANDIDATE_CONTENT_SCHEMA_V2 = "tc.research-candidate-content.v2"
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


def _native_name(value: object) -> str:
    # Use the same native name/ZIP boundary as databank actions, without importing
    # their runtime integration while this custody module is being loaded.
    from tradercockpit.sqx_databank_actions import _name
    return _name(value)


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
    native_job_entity_id: str | None
    native_job_revision: str | None
    configuration_entity_id: str | None
    configuration_revision: str | None
    association_mode: str
    archive_name: str
    archive_relative_path: str
    archive_ref: EvidenceRef
    archive_sha256: str
    strategy_ref: EvidenceRef
    strategy_sha256: str
    settings_ref: EvidenceRef
    settings_sha256: str
    sqx_build: str | None
    ml_model_artifact_sha256: str | None = None
    origin: dict[str, object] | None = None

    def __post_init__(self) -> None:
        if self.origin is not None:
            self._validate_origin()
            self._validate_evidence()
            return
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
        self._validate_evidence()

    def _validate_origin(self) -> None:
        origin = self.origin
        required = {"kind", "project", "databank", "history_status", "original_archive_ref", "original_archive_sha256"}
        if not isinstance(origin, dict) or set(origin) != required or origin.get("kind") not in {"user_import", "native_databank"} or origin.get("history_status") != "unknown":
            raise ResearchCandidateError("candidate_origin_invalid", "native/import Candidate origin is invalid")
        for value in (origin["project"], origin["databank"], self.archive_name):
            _native_name(value)
        if not self.archive_name.lower().endswith(".sqx") or self.archive_relative_path != f"user/projects/{origin['project']}/databanks/{origin['databank']}/{self.archive_name}":
            raise ResearchCandidateError("candidate_archive_invalid", "Candidate source must be an exact native databank archive")
        if any(value is not None for value in (self.native_job_entity_id, self.native_job_revision, self.configuration_entity_id, self.configuration_revision)) or self.association_mode != "operator_selected_exact_native_archive":
            raise ResearchCandidateError("candidate_origin_invalid", "native/import origin must not invent a Builder job or configuration")
        if self.sqx_build is not None and (not isinstance(self.sqx_build, str) or not re.fullmatch(r"[0-9]+\.[0-9]+", self.sqx_build)):
            raise ResearchCandidateError("candidate_origin_invalid", "Candidate producer build is invalid")
        if origin["kind"] == "user_import":
            original = EvidenceRef.parse(origin["original_archive_ref"])
            if original.digest != _digest(origin["original_archive_sha256"], code="candidate_origin_invalid"):
                raise ResearchCandidateError("candidate_origin_invalid", "original import evidence digest does not match")
        elif origin["original_archive_ref"] is not None or origin["original_archive_sha256"] is not None:
            raise ResearchCandidateError("candidate_origin_invalid", "existing native archives have no observed desktop import")

    def _validate_evidence(self) -> None:
        bindings = (
            (self.archive_ref, self.archive_sha256, "candidate_archive_invalid"),
            (self.strategy_ref, self.strategy_sha256, "candidate_strategy_invalid"),
            (self.settings_ref, self.settings_sha256, "candidate_settings_invalid"),
        )
        for ref, digest, code in bindings:
            if not isinstance(ref, EvidenceRef) or ref.digest != _digest(digest, code=code):
                raise ResearchCandidateError(code, "candidate evidence reference does not match its digest")
        if self.ml_model_artifact_sha256 is not None:
            _digest(self.ml_model_artifact_sha256, code="candidate_ml_model_invalid")

    def canonical_bytes(self) -> bytes:
        payload = {
            "archive_name": self.archive_name,
            "archive_ref": str(self.archive_ref),
            "archive_relative_path": self.archive_relative_path,
            "archive_sha256": self.archive_sha256,
            "association_mode": self.association_mode,
            "configuration_entity_id": self.configuration_entity_id,
            "configuration_revision": self.configuration_revision,
            "native_job_entity_id": self.native_job_entity_id,
            "native_job_revision": self.native_job_revision,
            "schema": CANDIDATE_CONTENT_SCHEMA_V2 if self.origin is not None else CANDIDATE_CONTENT_SCHEMA,
            "settings_ref": str(self.settings_ref),
            "settings_sha256": self.settings_sha256,
            "sqx_build": self.sqx_build,
            "strategy_ref": str(self.strategy_ref),
            "strategy_sha256": self.strategy_sha256,
        }
        if self.ml_model_artifact_sha256 is not None:
            payload["ml_model_artifact_sha256"] = self.ml_model_artifact_sha256
        if self.origin is not None:
            payload["origin"] = self.origin
        return _canonical(payload)

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
        extra = {"ml_model_artifact_sha256"}
        if isinstance(payload, dict) and payload.get("schema") == CANDIDATE_CONTENT_SCHEMA_V2:
            expected.add("origin")
        if not isinstance(payload, dict) or not expected <= set(payload) or set(payload) - expected - extra or payload.get("schema") not in {CANDIDATE_CONTENT_SCHEMA, CANDIDATE_CONTENT_SCHEMA_V2}:
            raise ResearchCandidateError("candidate_content_corrupt", "candidate content schema is invalid")
        if payload["schema"] == CANDIDATE_CONTENT_SCHEMA_V2 and not isinstance(payload.get("origin"), dict):
            raise ResearchCandidateError("candidate_content_corrupt", "Candidate V2 requires a discriminated origin")
        ml_digest = payload.get("ml_model_artifact_sha256")
        if ml_digest is not None and not isinstance(ml_digest, str):
            raise ResearchCandidateError("candidate_content_corrupt", "candidate ML pointer is invalid")
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
                ml_model_artifact_sha256=ml_digest,
                origin=payload.get("origin"),
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
        if store.deletion_record(entity) is not None:
            continue
        store.current(entity)
        entities.append(entity)
    return tuple(entities)


def _record(store: FileResearchCustodyStore, entity: ResearchEntityId, revision: ResearchRevisionRef) -> dict[str, object]:
    stored = store.read_revision(revision)
    if stored.entity_id != entity:
        raise ResearchCandidateError("candidate_content_corrupt", "candidate revision custody identity is invalid")
    content = CandidateContent.from_bytes(store.read_revision_content(revision))
    archive = store.read_evidence(content.archive_ref)
    strategy = store.read_evidence(content.strategy_ref)
    settings = store.read_evidence(content.settings_ref)
    inspected = inspect_sqx_output_bytes(archive, archive_name=content.archive_name, require_runtime_build=content.origin is None)
    if (
        inspected["archive_sha256"] != content.archive_sha256
        or inspected["strategy_entry_sha256"] != content.strategy_sha256
        or inspected["settings_entry_sha256"] != content.settings_sha256
        or inspected["sqx_build"] != content.sqx_build
        or _member(archive, "strategy_Portfolio.xml") != strategy
        or _member(archive, "settings.xml") != settings
    ):
        raise ResearchCandidateError("candidate_content_corrupt", "candidate archive/evidence binding is invalid")
    expected_evidence = {content.archive_ref, content.strategy_ref, content.settings_ref}
    if content.origin is not None and content.origin["original_archive_ref"] is not None:
        original_ref = EvidenceRef.parse(content.origin["original_archive_ref"])
        store.read_evidence(original_ref)
        expected_evidence.add(original_ref)
    if set(stored.evidence) != expected_evidence:
        raise ResearchCandidateError("candidate_content_corrupt", "candidate revision evidence set is invalid")
    record = {
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
        "ml_model_artifact_sha256": content.ml_model_artifact_sha256,
    }
    if content.origin is not None:
        record.update(origin=content.origin, history_status="unknown")
    return record


def list_current_candidates(store: FileResearchCustodyStore) -> dict[str, object]:
    catalog = {
        "schema": CANDIDATE_CATALOG_SCHEMA,
        "candidates": [
            _record(store, entity, store.current(entity))
            for entity in _candidate_entities(store)
        ],
    }
    deleted = store.base / "deletions" / ResearchKind.CANDIDATE.value
    if deleted.exists():
        catalog["deleted_candidates"] = [store.deletion_record(ResearchEntityId(ResearchKind.CANDIDATE, UUID(path.stem))) for path in sorted(deleted.glob("*.json"))]
    return catalog


def read_current_candidate(store: FileResearchCustodyStore, entity_id: ResearchEntityId | str) -> dict[str, object]:
    entity = _candidate_entity(entity_id)
    return _record(store, entity, store.current(entity))


def read_candidate_revision(
    store: FileResearchCustodyStore,
    entity_id: ResearchEntityId | str,
    revision: ResearchRevisionRef | str,
) -> dict[str, object]:
    entity = _candidate_entity(entity_id)
    try:
        exact_revision = revision if isinstance(revision, ResearchRevisionRef) else ResearchRevisionRef.parse(revision)
    except ResearchCustodyError as exc:
        raise ResearchCandidateError("candidate_revision_invalid", "candidate revision identity is invalid") from exc
    if exact_revision.kind != ResearchKind.CANDIDATE:
        raise ResearchCandidateError("candidate_revision_invalid", "research revision is not a candidate revision")
    return _record(store, entity, exact_revision)


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
        sqx_build=inspected["sqx_build"],
    )
    entity = store.create_entity(ResearchKind.CANDIDATE)
    stored = store.create_revision(
        entity,
        content.canonical_bytes(),
        evidence=(archive_ref, strategy_ref, settings_ref),
    )
    store.compare_and_set_current(entity, expected_revision=None, target_revision=stored.revision)
    return {**_record(store, entity, stored.revision), "reused": False}


def admit_databank_candidate(
    store: FileResearchCustodyStore,
    *,
    project: str,
    databank: str,
    archive: str,
    archive_bytes: bytes,
    origin_kind: str = "native_databank",
    original_bytes: bytes | None = None,
    admission_inventory=None,
) -> dict[str, object]:
    """Admit exact bytes already read back by the native databank adapter.

    The caller owns physical runtime path verification. This boundary accepts no
    desktop paths and reads no native files; its source is the exact verified
    project/bank/name plus bytes. Original upload bytes remain separate evidence
    when SQX changes an archive while loading it. Prior execution is unknown.
    """
    from tradercockpit.sqx_databank_actions import inspect_databank_upload
    from tradercockpit.research_candidate_memberships import candidate_admission_batch, _check_admission_batch, record_databank_membership_operation

    if admission_inventory is None:
        with candidate_admission_batch(store) as inventory:
            return admit_databank_candidate(store, project=project, databank=databank, archive=archive,
                archive_bytes=archive_bytes, origin_kind=origin_kind, original_bytes=original_bytes, admission_inventory=inventory)
    _check_admission_batch(store, admission_inventory)

    for value in (project, databank, archive):
        _native_name(value)
    if origin_kind not in {"native_databank", "user_import"} or (origin_kind == "user_import") != (original_bytes is not None):
        raise ResearchCandidateError("candidate_origin_invalid", "user imports require exact original archive bytes")
    inspected = inspect_databank_upload(archive_bytes, archive)
    if original_bytes is not None:
        inspect_databank_upload(original_bytes, archive)
    relative = f"user/projects/{project}/databanks/{databank}/{archive}"
    for membership in (admission_inventory.rows.get((project, databank, archive)),):
        if membership is not None:
            if membership["archive_sha256"] != inspected["archive_sha256"]:
                raise ResearchCustodyError("candidate_membership_stale", "Native bytes changed at an admitted location; explicit lineage is required.")
            return {**read_candidate_revision(store, membership["candidate_entity_id"], membership["candidate_revision"]),
                    "membership_revision": membership["membership_revision"], "reused": True}

    def bind_membership(record):
        membership = record_databank_membership_operation(
            store, action="admit", candidate_entity_id=record["entity_id"], candidate_revision=record["revision"],
            destination={"project": project, "databank": databank, "archive": archive}, archive_bytes=archive_bytes,
            _admission_inventory=admission_inventory,
        )
        return {**record, "membership_revision": membership["revision"]}

    legacy = [row for row in admission_inventory.legacy
              if row["archive_relative_path"] == relative and row["archive_sha256"] == inspected["archive_sha256"]]
    if len(legacy) > 1:
        raise ResearchCustodyError("candidate_membership_ambiguous", "Multiple Builder Candidates bind this archive; select its exact Candidate before admission.")
    if legacy:
        return bind_membership({**legacy[0], "reused": True})
    identity = _canonical({"origin": origin_kind, "path": relative, "archive_sha256": inspected["archive_sha256"], "original_sha256": EvidenceRef.from_bytes(original_bytes).digest if original_bytes is not None else None})
    entity = ResearchEntityId(ResearchKind.CANDIDATE, uuid5(NAMESPACE_URL, identity.decode("utf-8")))
    # ponytail: serialize Candidate admission in this local store; deterministic
    # identities make interrupted writes/retries reuse the same immutable record.
    with store._lock(store._lock_path("candidate-admit", str(entity))):
        if store._read_current(entity) is not None:
            return bind_membership({**read_current_candidate(store, entity), "reused": True})
        refs = [store.put_evidence(archive_bytes), store.put_evidence(_member(archive_bytes, "strategy_Portfolio.xml")), store.put_evidence(_member(archive_bytes, "settings.xml"))]
        original_ref = store.put_evidence(original_bytes) if original_bytes is not None else None
        content = CandidateContent(
            native_job_entity_id=None, native_job_revision=None,
            configuration_entity_id=None, configuration_revision=None,
            association_mode="operator_selected_exact_native_archive",
            archive_name=archive, archive_relative_path=relative,
            archive_ref=refs[0], archive_sha256=refs[0].digest,
            strategy_ref=refs[1], strategy_sha256=refs[1].digest,
            settings_ref=refs[2], settings_sha256=refs[2].digest,
            sqx_build=inspected["sqx_build"],
            origin={"kind": origin_kind, "project": project, "databank": databank,
                    "history_status": "unknown", "original_archive_ref": str(original_ref) if original_ref else None,
                    "original_archive_sha256": original_ref.digest if original_ref else None},
        )
        stored = store.create_revision(entity, content.canonical_bytes(), evidence=tuple(refs + ([original_ref] if original_ref else [])))
        store.compare_and_set_current(entity, expected_revision=None, target_revision=stored.revision)
        return bind_membership({**_record(store, entity, stored.revision), "reused": False})


def _import_archive_content(store, *, archive_bytes, archive, project, databank, original_ref):
    from .sqx_databank_actions import inspect_databank_upload
    inspected = inspect_databank_upload(archive_bytes, archive)
    refs = [EvidenceRef.from_bytes(archive_bytes), EvidenceRef.from_bytes(_member(archive_bytes, "strategy_Portfolio.xml")),
            EvidenceRef.from_bytes(_member(archive_bytes, "settings.xml"))]
    return CandidateContent(None, None, None, None, "operator_selected_exact_native_archive", archive,
        f"user/projects/{project}/databanks/{databank}/{archive}", refs[0], refs[0].digest,
        refs[1], refs[1].digest, refs[2], refs[2].digest, inspected["sqx_build"],
        origin={"kind": "user_import", "project": project, "databank": databank, "history_status": "unknown",
                "original_archive_ref": str(original_ref), "original_archive_sha256": original_ref.digest})


def _import_evidence(content):
    return (content.archive_ref, content.strategy_ref, content.settings_ref, EvidenceRef.parse(content.origin["original_archive_ref"]))


def _retain_import_archive(store, archive):
    for raw in (archive, _member(archive, "strategy_Portfolio.xml"), _member(archive, "settings.xml")):
        store.put_evidence(raw)


def prepare_databank_import_candidate(store, *, candidate_entity_id, project, databank, archive,
                                      original_bytes, prepared_bytes, token):
    """Retain a new import derivative without publishing a Candidate or native location."""
    from .sqx_candidate_identity import stamp_import_candidate_token
    entity = _candidate_entity(candidate_entity_id)
    for value in (project, databank, archive):
        _native_name(value)
    if prepared_bytes != stamp_import_candidate_token(original_bytes, token):
        raise ResearchCandidateError("candidate_import_derivative_invalid", "Prepared bytes are not the exact new-import token derivative.")
    with store._lock(store._lock_path("candidate-admit", str(entity))):
        content = _import_archive_content(store, archive_bytes=prepared_bytes, archive=archive,
            project=project, databank=databank, original_ref=EvidenceRef.from_bytes(original_bytes))
        # The caller durably reserves the UUID before entering this boundary. A
        # retry may reuse its one immutable root, never attach a different import.
        roots = []
        for path in (store.base / "revisions" / ResearchKind.CANDIDATE.value).rglob("*.json"):
            envelope = store.read_revision(ResearchRevisionRef(ResearchKind.CANDIDATE, path.stem))
            if envelope.entity_id == entity and envelope.parent_revision is None:
                roots.append(envelope)
        if roots:
            if len(roots) != 1 or store.read_revision_content(roots[0].revision) != content.canonical_bytes():
                raise ResearchCandidateError("candidate_import_identity_conflict", "Reserved Candidate already belongs to a different import.")
            return {**_record(store, entity, roots[0].revision), "reused": True}
        if store._read_current(entity) is not None:
            raise ResearchCandidateError("candidate_import_identity_conflict", "Reserved Candidate is already published.")
        store.put_evidence(original_bytes)
        _retain_import_archive(store, prepared_bytes)
        stored = store.create_revision(entity, content.canonical_bytes(), evidence=_import_evidence(content))
        return {**_record(store, entity, stored.revision), "reused": False}


def publish_databank_import_candidate(store, *, candidate_entity_id, prepared_revision, archive_bytes):
    """Publish verified native output as a child of the retained new-import root."""
    from .sqx_candidate_identity import read_candidate_token, stamp_import_candidate_token, verify_native_import
    from .research_candidate_memberships import candidate_admission_batch, record_databank_membership_operation, assert_candidate_membership_action, read_candidate_memberships
    entity = _candidate_entity(candidate_entity_id)
    prepared = _parse_typed_revision(str(prepared_revision), ResearchKind.CANDIDATE, "candidate_revision_invalid")
    read_candidate_revision(store, entity, prepared)
    content = CandidateContent.from_bytes(store.read_revision_content(prepared))
    if store.read_revision(prepared).parent_revision is not None or content.origin is None or content.origin["kind"] != "user_import" or content.ml_model_artifact_sha256 is not None:
        raise ResearchCandidateError("candidate_import_root_invalid", "Publication requires the exact unpublished import root.")
    retained = store.read_evidence(content.archive_ref)
    token = read_candidate_token(retained)
    if retained != stamp_import_candidate_token(store.read_evidence(EvidenceRef.parse(content.origin["original_archive_ref"])), token):
        raise ResearchCandidateError("candidate_import_derivative_invalid", "Import root is not its retained original's token derivative.")
    verify_native_import(retained, archive_bytes, token, content.archive_name)
    with candidate_admission_batch(store) as inventory, store._lock(store._lock_path("candidate-admit", str(entity))):
        assert_candidate_membership_action(store, str(entity), action="admit")
        location = {"project": content.origin["project"], "databank": content.origin["databank"], "archive": content.archive_name}
        existing = inventory.rows.get(tuple(location.values()))
        digest = EvidenceRef.from_bytes(archive_bytes).digest
        if existing is not None and (existing["candidate_entity_id"] != str(entity) or existing["archive_sha256"] != digest):
            raise ResearchCustodyError("candidate_membership_collision", "Import destination already belongs to a different exact artifact.")
        output = _import_archive_content(store, archive_bytes=archive_bytes, **location,
            original_ref=EvidenceRef.parse(content.origin["original_archive_ref"]))
        current = store._read_current(entity)
        membership_before = read_candidate_memberships(store, str(entity))
        if membership_before["revision"] is not None and (current is None or existing is None or existing["candidate_revision"] != str(current)):
            raise ResearchCandidateError("candidate_import_identity_conflict", "Import membership has changed; publication cannot restore or replace later history.")
        if current is not None:
            current_envelope = store.read_revision(current)
            if current_envelope.parent_revision != prepared or store.read_revision_content(current) != output.canonical_bytes():
                raise ResearchCandidateError("candidate_import_identity_conflict", "Candidate has already published different output or history.")
            stored = current_envelope
        else:
            _retain_import_archive(store, archive_bytes)
            stored = store.create_revision(entity, output.canonical_bytes(), parent_revision=prepared, evidence=_import_evidence(output))
            store.compare_and_set_current(entity, expected_revision=None, target_revision=stored.revision)
        membership = record_databank_membership_operation(store, action="admit", candidate_entity_id=str(entity),
            candidate_revision=str(stored.revision), destination=location, archive_bytes=archive_bytes, _admission_inventory=inventory)
        return {**_record(store, entity, stored.revision), "membership_revision": membership["revision"], "reused": current is not None}


def bind_ml_model(
    store: FileResearchCustodyStore,
    *,
    candidate_entity_id: str,
    expected_candidate_revision: str,
    artifact_sha256: str,
) -> dict[str, object]:
    """Attach one fitted Models catalog artifact to an existing native Candidate.

    This writes a new Candidate revision with the same SQX archive/strategy/settings
    evidence. It never creates a Candidate from a pickle and never loads the estimator.
    """

    from tradercockpit.research_models import _load_models

    digest = _digest(artifact_sha256, code="candidate_ml_model_invalid")
    if not any(item.get("artifact_sha256") == digest for item in _load_models(store.root)):
        raise ResearchCandidateError("candidate_ml_model_missing", "fitted model is not in the Models catalog")
    current = read_current_candidate(store, candidate_entity_id)
    if current["revision"] != expected_candidate_revision:
        raise ResearchCustodyError("current_conflict", "candidate revision changed before ML bind")
    if current.get("ml_model_artifact_sha256") == digest:
        return {**current, "reused": True}
    entity = _candidate_entity(candidate_entity_id)
    parent = _parse_typed_revision(current["revision"], ResearchKind.CANDIDATE, "candidate_revision_invalid")
    content = CandidateContent(
        native_job_entity_id=current["native_job_entity_id"],
        native_job_revision=current["native_job_revision"],
        configuration_entity_id=current["configuration_entity_id"],
        configuration_revision=current["configuration_revision"],
        association_mode=current["association_mode"],
        archive_name=current["archive_name"],
        archive_relative_path=current["archive_relative_path"],
        archive_ref=EvidenceRef.parse(current["archive_ref"]),
        archive_sha256=current["archive_sha256"],
        strategy_ref=EvidenceRef.parse(current["strategy_ref"]),
        strategy_sha256=current["strategy_sha256"],
        settings_ref=EvidenceRef.parse(current["settings_ref"]),
        settings_sha256=current["settings_sha256"],
        sqx_build=current["sqx_build"],
        ml_model_artifact_sha256=digest,
        origin=current.get("origin"),
    )
    stored = store.create_revision(
        entity,
        content.canonical_bytes(),
        parent_revision=parent,
        evidence=tuple({content.archive_ref, content.strategy_ref, content.settings_ref} | ({EvidenceRef.parse(content.origin["original_archive_ref"])} if content.origin is not None and content.origin["original_archive_ref"] is not None else set())),
    )
    store.compare_and_set_current(entity, expected_revision=parent, target_revision=stored.revision)
    return {**_record(store, entity, stored.revision), "reused": False}
