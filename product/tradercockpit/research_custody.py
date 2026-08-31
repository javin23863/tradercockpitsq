"""Typed research identity and immutable custody primitives.

This module is deliberately limited to product custody. It does not define strategy
semantics, native execution, backtesting, candidate generation, or result logic.
"""

from __future__ import annotations

from contextlib import contextmanager
from dataclasses import dataclass
from enum import StrEnum
import hashlib
import json
import os
from pathlib import Path
from typing import Iterator
from uuid import UUID, uuid4


RESEARCH_ENTITY_ID_SCHEMA = "tc.research-entity-id.v1"
RESEARCH_REVISION_SCHEMA = "tc.research-revision.v1"
RESEARCH_CURRENT_SCHEMA = "tc.research-current.v1"
EVIDENCE_REF_SCHEMA = "tc.evidence-ref.sha256.v1"


class ResearchCustodyError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


class ResearchKind(StrEnum):
    IDEA = "idea"
    CONFIGURATION = "configuration"
    NATIVE_JOB = "native-job"
    CANDIDATE = "candidate"
    HISTORICAL_RESULT = "historical-result"
    PROOF = "proof"


def _digest(value: str, code: str) -> str:
    if (
        not isinstance(value, str)
        or len(value) != 64
        or value != value.lower()
        or any(char not in "0123456789abcdef" for char in value)
    ):
        raise ResearchCustodyError(code, "expected a lowercase 64-character sha256 digest")
    return value


def _sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def _canonical(payload: dict[str, object]) -> bytes:
    return json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


@dataclass(frozen=True, slots=True)
class ResearchEntityId:
    kind: ResearchKind
    value: UUID

    def __post_init__(self) -> None:
        if not isinstance(self.kind, ResearchKind) or not isinstance(self.value, UUID):
            raise ResearchCustodyError("entity_id_invalid", "research entity requires a typed kind and UUID")

    @classmethod
    def new(cls, kind: ResearchKind) -> "ResearchEntityId":
        if not isinstance(kind, ResearchKind):
            raise ResearchCustodyError("entity_kind_invalid", "research entity kind is invalid")
        return cls(kind, uuid4())

    @classmethod
    def parse(cls, value: str) -> "ResearchEntityId":
        if not isinstance(value, str):
            raise ResearchCustodyError("entity_id_invalid", "research entity id must be a string")
        parts = value.split(":")
        if len(parts) != 4 or parts[0] != "tc-research" or parts[2] != "v1":
            raise ResearchCustodyError("entity_id_invalid", "research entity id has an invalid namespace or version")
        try:
            result = cls(ResearchKind(parts[1]), UUID(parts[3]))
        except (ValueError, AttributeError) as exc:
            raise ResearchCustodyError("entity_id_invalid", "research entity id is malformed") from exc
        if str(result.value) != parts[3]:
            raise ResearchCustodyError("entity_id_invalid", "research entity UUID must use canonical lowercase form")
        return result

    def __str__(self) -> str:
        return f"tc-research:{self.kind.value}:v1:{self.value}"


@dataclass(frozen=True, slots=True)
class EvidenceRef:
    digest: str

    def __post_init__(self) -> None:
        _digest(self.digest, "evidence_ref_invalid")

    @classmethod
    def parse(cls, value: str) -> "EvidenceRef":
        prefix = "tc-evidence:sha256:"
        if not isinstance(value, str) or not value.startswith(prefix):
            raise ResearchCustodyError("evidence_ref_invalid", "evidence ref has an invalid namespace")
        return cls(_digest(value[len(prefix):], "evidence_ref_invalid"))

    @classmethod
    def from_bytes(cls, data: bytes) -> "EvidenceRef":
        if not isinstance(data, bytes):
            raise ResearchCustodyError("evidence_bytes_invalid", "evidence must be bytes")
        return cls(_sha256(data))

    def __str__(self) -> str:
        return f"tc-evidence:sha256:{self.digest}"


@dataclass(frozen=True, slots=True)
class ResearchRevisionRef:
    kind: ResearchKind
    digest: str

    def __post_init__(self) -> None:
        if not isinstance(self.kind, ResearchKind):
            raise ResearchCustodyError("revision_ref_invalid", "research revision kind is invalid")
        _digest(self.digest, "revision_ref_invalid")

    @classmethod
    def parse(cls, value: str) -> "ResearchRevisionRef":
        if not isinstance(value, str):
            raise ResearchCustodyError("revision_ref_invalid", "research revision ref must be a string")
        parts = value.split(":")
        if len(parts) != 4 or parts[0] != "tc-research-revision" or parts[2] != "sha256":
            raise ResearchCustodyError("revision_ref_invalid", "research revision ref has an invalid namespace")
        try:
            kind = ResearchKind(parts[1])
        except ValueError as exc:
            raise ResearchCustodyError("revision_ref_invalid", "research revision kind is invalid") from exc
        return cls(kind, _digest(parts[3], "revision_ref_invalid"))

    def __str__(self) -> str:
        return f"tc-research-revision:{self.kind.value}:sha256:{self.digest}"


@dataclass(frozen=True, slots=True)
class ResearchRevision:
    revision: ResearchRevisionRef
    entity_id: ResearchEntityId
    parent_revision: ResearchRevisionRef | None
    content: EvidenceRef
    evidence: tuple[EvidenceRef, ...]

    def __post_init__(self) -> None:
        if self.revision.kind != self.entity_id.kind:
            raise ResearchCustodyError("revision_binding_invalid", "revision and entity kinds do not match")
        if self.parent_revision is not None and self.parent_revision.kind != self.entity_id.kind:
            raise ResearchCustodyError("revision_binding_invalid", "parent revision kind does not match entity")
        if not isinstance(self.content, EvidenceRef) or any(not isinstance(item, EvidenceRef) for item in self.evidence):
            raise ResearchCustodyError("revision_binding_invalid", "revision evidence bindings are invalid")

    def wire_record(self) -> dict[str, object]:
        return {
            "schema": RESEARCH_REVISION_SCHEMA,
            "entity_id": str(self.entity_id),
            "kind": self.entity_id.kind.value,
            "parent_revision": str(self.parent_revision) if self.parent_revision else None,
            "content": str(self.content),
            "evidence": [str(item) for item in self.evidence],
        }


def research_custody_capability_record() -> dict[str, object]:
    return {
        "status": "ready",
        "identity_schema": RESEARCH_ENTITY_ID_SCHEMA,
        "revision_schema": RESEARCH_REVISION_SCHEMA,
        "evidence_schema": EVIDENCE_REF_SCHEMA,
        "current_schema": RESEARCH_CURRENT_SCHEMA,
        "current_update": "compare-and-set",
        "active_subject": False,
        "record_kinds": [kind.value for kind in ResearchKind],
    }


class FileResearchCustodyStore:
    """Filesystem custody with immutable evidence/revisions and atomic CAS pointers."""

    def __init__(self, root: Path | str) -> None:
        self.root = Path(root).expanduser().resolve()
        self.base = self.root / "research" / "v1"
        for relative in ("evidence/sha256", "revisions", "current", "locks"):
            (self.base / relative).mkdir(parents=True, exist_ok=True)

    def create_entity(self, kind: ResearchKind) -> ResearchEntityId:
        return ResearchEntityId.new(kind)

    def _evidence_path(self, ref: EvidenceRef) -> Path:
        return self.base / "evidence" / "sha256" / ref.digest[:2] / f"{ref.digest}.bin"

    def _revision_path(self, ref: ResearchRevisionRef) -> Path:
        return self.base / "revisions" / ref.kind.value / ref.digest[:2] / f"{ref.digest}.json"

    def _current_path(self, entity: ResearchEntityId) -> Path:
        return self.base / "current" / entity.kind.value / f"{entity.value}.json"

    def _lock_path(self, category: str, key: str) -> Path:
        return self.base / "locks" / category / f"{_sha256(f'{category}\0{key}'.encode())}.lock"

    @contextmanager
    def _lock(self, path: Path) -> Iterator[None]:
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("a+b") as handle:
            if os.name == "nt":
                import msvcrt
                handle.seek(0, os.SEEK_END)
                if handle.tell() == 0:
                    handle.write(b"\0")
                    handle.flush()
                    os.fsync(handle.fileno())
                handle.seek(0)
                msvcrt.locking(handle.fileno(), msvcrt.LK_LOCK, 1)
                try:
                    yield
                finally:
                    handle.seek(0)
                    msvcrt.locking(handle.fileno(), msvcrt.LK_UNLCK, 1)
            else:
                import fcntl
                fcntl.flock(handle.fileno(), fcntl.LOCK_EX)
                try:
                    yield
                finally:
                    fcntl.flock(handle.fileno(), fcntl.LOCK_UN)

    def _atomic_write(self, path: Path, data: bytes) -> None:
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

    def put_evidence(self, data: bytes) -> EvidenceRef:
        if not isinstance(data, bytes):
            raise ResearchCustodyError("evidence_bytes_invalid", "evidence must be bytes")
        ref = EvidenceRef.from_bytes(data)
        path = self._evidence_path(ref)
        with self._lock(self._lock_path("evidence", ref.digest)):
            if path.exists():
                existing = path.read_bytes()
                if existing != data or _sha256(existing) != ref.digest:
                    raise ResearchCustodyError("immutable_evidence_corrupt", "existing evidence does not match its digest")
            else:
                self._atomic_write(path, data)
        return ref

    def read_evidence(self, ref: EvidenceRef) -> bytes:
        if not isinstance(ref, EvidenceRef):
            raise ResearchCustodyError("evidence_ref_invalid", "expected EvidenceRef")
        try:
            data = self._evidence_path(ref).read_bytes()
        except FileNotFoundError as exc:
            raise ResearchCustodyError("evidence_missing", "content-addressed evidence is missing") from exc
        if _sha256(data) != ref.digest:
            raise ResearchCustodyError("immutable_evidence_corrupt", "evidence digest verification failed")
        return data

    def create_revision(
        self,
        entity: ResearchEntityId,
        content: bytes,
        *,
        parent_revision: ResearchRevisionRef | None = None,
        evidence: tuple[EvidenceRef, ...] = (),
    ) -> ResearchRevision:
        if not isinstance(entity, ResearchEntityId):
            raise ResearchCustodyError("entity_id_invalid", "expected ResearchEntityId")
        content_ref = self.put_evidence(content)
        if any(not isinstance(item, EvidenceRef) for item in evidence):
            raise ResearchCustodyError("evidence_ref_invalid", "revision evidence must use EvidenceRef")
        attachments = tuple(sorted(set(evidence), key=str))
        for item in attachments:
            self.read_evidence(item)
        if parent_revision is not None:
            if self.read_revision(parent_revision).entity_id != entity:
                raise ResearchCustodyError("revision_parent_mismatch", "parent revision belongs to a different entity")

        envelope: dict[str, object] = {
            "schema": RESEARCH_REVISION_SCHEMA,
            "entity_id": str(entity),
            "kind": entity.kind.value,
            "parent_revision": str(parent_revision) if parent_revision else None,
            "content": str(content_ref),
            "evidence": [str(item) for item in attachments],
        }
        encoded = _canonical(envelope)
        ref = ResearchRevisionRef(entity.kind, _sha256(encoded))
        path = self._revision_path(ref)
        with self._lock(self._lock_path("revision", str(ref))):
            if path.exists():
                existing = path.read_bytes()
                if existing != encoded or _sha256(existing) != ref.digest:
                    raise ResearchCustodyError("immutable_revision_corrupt", "existing revision does not match its identity")
            else:
                self._atomic_write(path, encoded)
        return ResearchRevision(ref, entity, parent_revision, content_ref, attachments)

    def read_revision(self, ref: ResearchRevisionRef) -> ResearchRevision:
        if not isinstance(ref, ResearchRevisionRef):
            raise ResearchCustodyError("revision_ref_invalid", "expected ResearchRevisionRef")
        try:
            encoded = self._revision_path(ref).read_bytes()
        except FileNotFoundError as exc:
            raise ResearchCustodyError("revision_missing", "research revision is missing") from exc
        if _sha256(encoded) != ref.digest:
            raise ResearchCustodyError("immutable_revision_corrupt", "revision digest verification failed")
        try:
            payload = json.loads(encoded)
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise ResearchCustodyError("immutable_revision_corrupt", "revision payload is not valid JSON") from exc
        if not isinstance(payload, dict) or payload.get("schema") != RESEARCH_REVISION_SCHEMA:
            raise ResearchCustodyError("immutable_revision_corrupt", "revision schema is invalid")

        entity = ResearchEntityId.parse(payload.get("entity_id"))
        if entity.kind != ref.kind or payload.get("kind") != ref.kind.value:
            raise ResearchCustodyError("immutable_revision_corrupt", "revision kind binding is invalid")
        parent_value = payload.get("parent_revision")
        parent = ResearchRevisionRef.parse(parent_value) if parent_value is not None else None
        content = EvidenceRef.parse(payload.get("content"))
        raw_evidence = payload.get("evidence")
        if not isinstance(raw_evidence, list):
            raise ResearchCustodyError("immutable_revision_corrupt", "revision evidence list is invalid")
        evidence = tuple(EvidenceRef.parse(item) for item in raw_evidence)
        if evidence != tuple(sorted(set(evidence), key=str)):
            raise ResearchCustodyError("immutable_revision_corrupt", "revision evidence list is not canonical")
        if parent is not None and parent.kind != entity.kind:
            raise ResearchCustodyError("immutable_revision_corrupt", "revision parent kind is invalid")
        self.read_evidence(content)
        for item in evidence:
            self.read_evidence(item)
        return ResearchRevision(ref, entity, parent, content, evidence)

    def read_revision_content(self, ref: ResearchRevisionRef) -> bytes:
        return self.read_evidence(self.read_revision(ref).content)

    def _read_current(self, entity: ResearchEntityId) -> ResearchRevisionRef | None:
        try:
            encoded = self._current_path(entity).read_bytes()
        except FileNotFoundError:
            return None
        try:
            payload = json.loads(encoded)
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise ResearchCustodyError("current_pointer_corrupt", "current pointer is not valid JSON") from exc
        if (
            not isinstance(payload, dict)
            or payload.get("schema") != RESEARCH_CURRENT_SCHEMA
            or payload.get("entity_id") != str(entity)
        ):
            raise ResearchCustodyError("current_pointer_corrupt", "current pointer identity binding is invalid")
        revision = ResearchRevisionRef.parse(payload.get("revision"))
        if revision.kind != entity.kind or self.read_revision(revision).entity_id != entity:
            raise ResearchCustodyError("current_pointer_corrupt", "current pointer target binding is invalid")
        return revision

    def current(self, entity: ResearchEntityId) -> ResearchRevisionRef:
        if not isinstance(entity, ResearchEntityId):
            raise ResearchCustodyError("entity_id_invalid", "expected ResearchEntityId")
        revision = self._read_current(entity)
        if revision is None:
            raise ResearchCustodyError("current_pointer_missing", "research entity has no current revision")
        return revision

    def compare_and_set_current(
        self,
        entity: ResearchEntityId,
        *,
        expected_revision: ResearchRevisionRef | None,
        target_revision: ResearchRevisionRef,
    ) -> ResearchRevisionRef:
        if not isinstance(entity, ResearchEntityId):
            raise ResearchCustodyError("entity_id_invalid", "expected ResearchEntityId")
        if expected_revision is not None and expected_revision.kind != entity.kind:
            raise ResearchCustodyError("current_expected_mismatch", "expected revision kind does not match entity")
        if self.read_revision(target_revision).entity_id != entity:
            raise ResearchCustodyError("current_target_mismatch", "target revision belongs to a different entity")

        path = self._current_path(entity)
        with self._lock(self._lock_path("current", str(entity))):
            if self._read_current(entity) != expected_revision:
                raise ResearchCustodyError("current_conflict", "current revision changed before compare-and-set")
            self._atomic_write(
                path,
                _canonical(
                    {
                        "schema": RESEARCH_CURRENT_SCHEMA,
                        "entity_id": str(entity),
                        "revision": str(target_revision),
                    }
                ),
            )
            if self._read_current(entity) != target_revision:
                raise ResearchCustodyError("current_pointer_corrupt", "current pointer verification failed")
        return target_revision
