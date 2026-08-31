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
    """Typed fail-closed custody error."""

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


def _require_digest(value: str, *, code: str) -> str:
    if (
        not isinstance(value, str)
        or len(value) != 64
        or value != value.lower()
        or any(character not in "0123456789abcdef" for character in value)
    ):
        raise ResearchCustodyError(code, "expected a lowercase 64-character sha256 digest")
    return value


def _sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def _canonical_json_bytes(payload: dict[str, object]) -> bytes:
    return json.dumps(
        payload,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")


@dataclass(frozen=True, slots=True)
class ResearchEntityId:
    kind: ResearchKind
    value: UUID

    @classmethod
    def new(cls, kind: ResearchKind) -> "ResearchEntityId":
        if not isinstance(kind, ResearchKind):
            raise ResearchCustodyError("entity_kind_invalid", "research entity kind is invalid")
        return cls(kind=kind, value=uuid4())

    @classmethod
    def parse(cls, value: str) -> "ResearchEntityId":
        if not isinstance(value, str):
            raise ResearchCustodyError("entity_id_invalid", "research entity id must be a string")
        parts = value.split(":")
        if len(parts) != 4 or parts[0] != "tc-research" or parts[2] != "v1":
            raise ResearchCustodyError("entity_id_invalid", "research entity id has an invalid namespace or version")
        try:
            kind = ResearchKind(parts[1])
            entity_uuid = UUID(parts[3])
        except (ValueError, AttributeError) as exc:
            raise ResearchCustodyError("entity_id_invalid", "research entity id is malformed") from exc
        if str(entity_uuid) != parts[3]:
            raise ResearchCustodyError("entity_id_invalid", "research entity UUID must use canonical lowercase form")
        return cls(kind=kind, value=entity_uuid)

    def __str__(self) -> str:
        return f"tc-research:{self.kind.value}:v1:{self.value}"


@dataclass(frozen=True, slots=True)
class EvidenceRef:
    digest: str

    def __post_init__(self) -> None:
        _require_digest(self.digest, code="evidence_ref_invalid")

    @classmethod
    def parse(cls, value: str) -> "EvidenceRef":
        if not isinstance(value, str):
            raise ResearchCustodyError("evidence_ref_invalid", "evidence ref must be a string")
        prefix = "tc-evidence:sha256:"
        if not value.startswith(prefix):
            raise ResearchCustodyError("evidence_ref_invalid", "evidence ref has an invalid namespace")
        return cls(_require_digest(value[len(prefix):], code="evidence_ref_invalid"))

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
        _require_digest(self.digest, code="revision_ref_invalid")

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
        return cls(kind=kind, digest=_require_digest(parts[3], code="revision_ref_invalid"))

    def __str__(self) -> str:
        return f"tc-research-revision:{self.kind.value}:sha256:{self.digest}"


@dataclass(frozen=True, slots=True)
class ResearchRevision:
    revision: ResearchRevisionRef
    entity_id: ResearchEntityId
    parent_revision: ResearchRevisionRef | None
    content: EvidenceRef
    evidence: tuple[EvidenceRef, ...]

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
    """Public, non-secret descriptor for the implemented custody contract."""

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
        for path in (
            self.base / "evidence" / "sha256",
            self.base / "revisions",
            self.base / "current",
            self.base / "locks",
        ):
            path.mkdir(parents=True, exist_ok=True)

    def create_entity(self, kind: ResearchKind) -> ResearchEntityId:
        return ResearchEntityId.new(kind)

    def _evidence_path(self, ref: EvidenceRef) -> Path:
        return self.base / "evidence" / "sha256" / ref.digest[:2] / f"{ref.digest}.bin"

    def _revision_path(self, ref: ResearchRevisionRef) -> Path:
        return self.base / "revisions" / ref.kind.value / ref.digest[:2] / f"{ref.digest}.json"

    def _current_path(self, entity_id: ResearchEntityId) -> Path:
        return self.base / "current" / entity_id.kind.value / f"{entity_id.value}.json"

    def _lock_path(self, category: str, key: str) -> Path:
        digest = _sha256(f"{category}\0{key}".encode("utf-8"))
        return self.base / "locks" / category / f"{digest}.lock"

    @contextmanager
    def _exclusive_lock(self, path: Path) -> Iterator[None]:
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
        with self._exclusive_lock(self._lock_path("evidence", ref.digest)):
            if path.exists():
                existing = path.read_bytes()
                if _sha256(existing) != ref.digest or existing != data:
                    raise ResearchCustodyError(
                        "immutable_evidence_corrupt",
                        "existing content-addressed evidence does not match its digest",
                    )
                return ref
            self._atomic_write(path, data)
        return ref

    def read_evidence(self, ref: EvidenceRef) -> bytes:
        if not isinstance(ref, EvidenceRef):
            raise ResearchCustodyError("evidence_ref_invalid", "expected EvidenceRef")
        path = self._evidence_path(ref)
        try:
            data = path.read_bytes()
        except FileNotFoundError as exc:
            raise ResearchCustodyError("evidence_missing", "content-addressed evidence is missing") from exc
        if _sha256(data) != ref.digest:
            raise ResearchCustodyError("immutable_evidence_corrupt", "evidence digest verification failed")
        return data

    def create_revision(
        self,
        entity_id: ResearchEntityId,
        content: bytes,
        *,
        parent_revision: ResearchRevisionRef | None = None,
        evidence: tuple[EvidenceRef, ...] = (),
    ) -> ResearchRevision:
        if not isinstance(entity_id, ResearchEntityId):
            raise ResearchCustodyError("entity_id_invalid", "expected ResearchEntityId")
        content_ref = self.put_evidence(content)

        normalized_evidence = tuple(sorted(set(evidence), key=str))
        for item in normalized_evidence:
            if not isinstance(item, EvidenceRef):
                raise ResearchCustodyError("evidence_ref_invalid", "revision evidence must use EvidenceRef")
            self.read_evidence(item)

        if parent_revision is not None:
            parent = self.read_revision(parent_revision)
            if parent.entity_id != entity_id:
                raise ResearchCustodyError(
                    "revision_parent_mismatch",
                    "parent revision belongs to a different research entity",
                )

        envelope = {
            "schema": RESEARCH_REVISION_SCHEMA,
            "entity_id": str(entity_id),
            "kind": entity_id.kind.value,
            "parent_revision": str(parent_revision) if parent_revision else None,
            "content": str(content_ref),
            "evidence": [str(item) for item in normalized_evidence],
        }
        encoded = _canonical_json_bytes(envelope)
        ref = ResearchRevisionRef(entity_id.kind, _sha256(encoded))
        path = self._revision_path(ref)

        with self._exclusive_lock(self._lock_path("revision", str(ref))):
            if path.exists():
                existing = path.read_bytes()
                if _sha256(existing) != ref.digest or existing != encoded:
                    raise ResearchCustodyError(
                        "immutable_revision_corrupt",
                        "existing immutable revision does not match its identity",
                    )
            else:
                self._atomic_write(path, encoded)

        return ResearchRevision(
            revision=ref,
            entity_id=entity_id,
            parent_revision=parent_revision,
            content=content_ref,
            evidence=normalized_evidence,
        )

    def read_revision(self, ref: ResearchRevisionRef) -> ResearchRevision:
        if not isinstance(ref, ResearchRevisionRef):
            raise ResearchCustodyError("revision_ref_invalid", "expected ResearchRevisionRef")
        path = self._revision_path(ref)
        try:
            encoded = path.read_bytes()
        except FileNotFoundError as exc:
            raise ResearchCustodyError("revision_missing", "research revision is missing") from exc
        if _sha256(encoded) != ref.digest:
            raise ResearchCustodyError("immutable_revision_corrupt", "revision digest verification failed")
        try:
            payload = json.loads(encoded.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise ResearchCustodyError("immutable_revision_corrupt", "revision payload is not valid JSON") from exc
        if not isinstance(payload, dict) or payload.get("schema") != RESEARCH_REVISION_SCHEMA:
            raise ResearchCustodyError("immutable_revision_corrupt", "revision schema is invalid")

        entity_id = ResearchEntityId.parse(payload.get("entity_id"))
        if entity_id.kind != ref.kind or payload.get("kind") != ref.kind.value:
            raise ResearchCustodyError("immutable_revision_corrupt", "revision kind binding is invalid")

        parent_value = payload.get("parent_revision")
        parent_revision = ResearchRevisionRef.parse(parent_value) if parent_value is not None else None
        if parent_revision is not None and parent_revision.kind != entity_id.kind:
            raise ResearchCustodyError("immutable_revision_corrupt", "revision parent kind is invalid")

        content = EvidenceRef.parse(payload.get("content"))
        evidence_payload = payload.get("evidence")
        if not isinstance(evidence_payload, list):
            raise ResearchCustodyError("immutable_revision_corrupt", "revision evidence list is invalid")
        evidence = tuple(EvidenceRef.parse(item) for item in evidence_payload)
        if evidence != tuple(sorted(set(evidence), key=str)):
            raise ResearchCustodyError("immutable_revision_corrupt", "revision evidence list is not canonical")

        self.read_evidence(content)
        for item in evidence:
            self.read_evidence(item)

        return ResearchRevision(
            revision=ref,
            entity_id=entity_id,
            parent_revision=parent_revision,
            content=content,
            evidence=evidence,
        )

    def read_revision_content(self, ref: ResearchRevisionRef) -> bytes:
        revision = self.read_revision(ref)
        return self.read_evidence(revision.content)

    def _read_current_unlocked(self, entity_id: ResearchEntityId) -> ResearchRevisionRef | None:
        path = self._current_path(entity_id)
        try:
            encoded = path.read_bytes()
        except FileNotFoundError:
            return None
        try:
            payload = json.loads(encoded.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise ResearchCustodyError("current_pointer_corrupt", "current pointer is not valid JSON") from exc
        if (
            not isinstance(payload, dict)
            or payload.get("schema") != RESEARCH_CURRENT_SCHEMA
            or payload.get("entity_id") != str(entity_id)
        ):
            raise ResearchCustodyError("current_pointer_corrupt", "current pointer identity binding is invalid")
        revision = ResearchRevisionRef.parse(payload.get("revision"))
        if revision.kind != entity_id.kind:
            raise ResearchCustodyError("current_pointer_corrupt", "current pointer kind binding is invalid")
        record = self.read_revision(revision)
        if record.entity_id != entity_id:
            raise ResearchCustodyError("current_pointer_corrupt", "current pointer targets a different entity")
        return revision

    def current(self, entity_id: ResearchEntityId) -> ResearchRevisionRef:
        if not isinstance(entity_id, ResearchEntityId):
            raise ResearchCustodyError("entity_id_invalid", "expected ResearchEntityId")
        revision = self._read_current_unlocked(entity_id)
        if revision is None:
            raise ResearchCustodyError("current_pointer_missing", "research entity has no current revision")
        return revision

    def compare_and_set_current(
        self,
        entity_id: ResearchEntityId,
        *,
        expected_revision: ResearchRevisionRef | None,
        target_revision: ResearchRevisionRef,
    ) -> ResearchRevisionRef:
        if not isinstance(entity_id, ResearchEntityId):
            raise ResearchCustodyError("entity_id_invalid", "expected ResearchEntityId")
        if expected_revision is not None and expected_revision.kind != entity_id.kind:
            raise ResearchCustodyError("current_expected_mismatch", "expected revision kind does not match entity")
        target = self.read_revision(target_revision)
        if target.entity_id != entity_id:
            raise ResearchCustodyError(
                "current_target_mismatch",
                "target revision belongs to a different research entity",
            )

        path = self._current_path(entity_id)
        with self._exclusive_lock(self._lock_path("current", str(entity_id))):
            actual = self._read_current_unlocked(entity_id)
            if actual != expected_revision:
                raise ResearchCustodyError(
                    "current_conflict",
                    "current research revision changed before compare-and-set",
                )
            payload = {
                "schema": RESEARCH_CURRENT_SCHEMA,
                "entity_id": str(entity_id),
                "revision": str(target_revision),
            }
            self._atomic_write(path, _canonical_json_bytes(payload))
            verified = self._read_current_unlocked(entity_id)
            if verified != target_revision:
                raise ResearchCustodyError("current_pointer_corrupt", "current pointer verification failed")
        return target_revision
