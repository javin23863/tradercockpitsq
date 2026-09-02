"""Operator promotion custody after an immutable Research Proof.

Promotion is platform-owned Delivery identity. It is not live execution, export,
deployment, paper/prop simulation, or a cockpit verdict. Historical research never
becomes live P&L through this module.
"""

from __future__ import annotations

import json
import re
from threading import Lock
from uuid import UUID

from tradercockpit.research_custody import (
    FileResearchCustodyStore,
    ResearchCustodyError,
    ResearchEntityId,
    ResearchKind,
    ResearchRevisionRef,
)
from tradercockpit.research_proof import ResearchProofError, read_current_research_proof
from tradercockpit.sqx_presets import SQX_BUILD


PROMOTION_CONTENT_SCHEMA = "tc.operate-promotion-content.v1"
PROMOTION_READ_SCHEMA = "tc.operate-promotion.v1"
PROMOTION_CATALOG_SCHEMA = "tc.operate-promotion-catalog.v1"
_CURRENT_POINTER_TEMP_RE = re.compile(
    r"^\.[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.json\.tmp-[0-9]+-[0-9a-f]{32}$"
)
_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")
_CREATE_LOCK = Lock()
_CONTENT_KEYS = frozenset(
    {
        "schema",
        "proof_entity_id",
        "proof_revision",
        "candidate_entity_id",
        "candidate_revision",
        "candidate_archive_name",
        "candidate_archive_sha256",
        "historical_result_entity_id",
        "historical_result_revision",
        "sqx_build",
    }
)


class OperatePromotionError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


def _canonical(payload: dict[str, object]) -> bytes:
    return json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def _typed_entity(value: object, kind: ResearchKind, code: str) -> ResearchEntityId:
    if not isinstance(value, str) or not value:
        raise OperatePromotionError(code, "entity identity is required")
    try:
        entity = ResearchEntityId.parse(value)
    except ResearchCustodyError as exc:
        raise OperatePromotionError(code, "entity identity is invalid") from exc
    if entity.kind != kind:
        raise OperatePromotionError(code, f"entity identity must be {kind.value} custody")
    return entity


def _typed_revision(value: object, kind: ResearchKind, code: str) -> ResearchRevisionRef:
    if not isinstance(value, str) or not value:
        raise OperatePromotionError(code, "revision identity is required")
    try:
        revision = ResearchRevisionRef.parse(value)
    except ResearchCustodyError as exc:
        raise OperatePromotionError(code, "revision identity is invalid") from exc
    if revision.kind != kind:
        raise OperatePromotionError(code, f"revision identity must be {kind.value} custody")
    return revision


def _digest(value: object, code: str) -> str:
    if not isinstance(value, str) or _DIGEST_RE.fullmatch(value) is None:
        raise OperatePromotionError(code, "SHA-256 identity is invalid")
    return value


def _required_string(record: dict[str, object], key: str, code: str) -> str:
    value = record.get(key)
    if not isinstance(value, str) or not value:
        raise OperatePromotionError(code, f"{key} is missing from the Proof read model")
    return value


def _proof_payload(proof: dict[str, object]) -> dict[str, object]:
    if not isinstance(proof, dict) or proof.get("schema") != "tc.research-proof.v1":
        raise OperatePromotionError("operate_promotion_proof_invalid", "Promotion requires a current Research Proof read model")
    candidate = proof.get("candidate")
    historical = proof.get("historical_result")
    if not isinstance(candidate, dict) or not isinstance(historical, dict):
        raise OperatePromotionError("operate_promotion_proof_invalid", "Proof must bind Candidate and Historical Result identities")
    proof_entity = _typed_entity(proof.get("entity_id"), ResearchKind.PROOF, "operate_promotion_proof_invalid")
    proof_revision = _typed_revision(proof.get("revision"), ResearchKind.PROOF, "operate_promotion_proof_invalid")
    candidate_entity = _typed_entity(
        candidate.get("entity_id"), ResearchKind.CANDIDATE, "operate_promotion_proof_invalid"
    )
    candidate_revision = _typed_revision(
        candidate.get("revision"), ResearchKind.CANDIDATE, "operate_promotion_proof_invalid"
    )
    historical_entity = _typed_entity(
        historical.get("entity_id"), ResearchKind.HISTORICAL_RESULT, "operate_promotion_proof_invalid"
    )
    historical_revision = _typed_revision(
        historical.get("revision"), ResearchKind.HISTORICAL_RESULT, "operate_promotion_proof_invalid"
    )
    archive_name = _required_string(candidate, "archive_name", "operate_promotion_proof_invalid")
    archive_sha256 = _digest(candidate.get("archive_sha256"), "operate_promotion_proof_invalid")
    sqx_build = proof.get("sqx_build")
    if sqx_build != SQX_BUILD:
        raise OperatePromotionError("operate_promotion_proof_invalid", "Proof producer build is not the authorized SQX build")
    return {
        "schema": PROMOTION_CONTENT_SCHEMA,
        "proof_entity_id": str(proof_entity),
        "proof_revision": str(proof_revision),
        "candidate_entity_id": str(candidate_entity),
        "candidate_revision": str(candidate_revision),
        "candidate_archive_name": archive_name,
        "candidate_archive_sha256": archive_sha256,
        "historical_result_entity_id": str(historical_entity),
        "historical_result_revision": str(historical_revision),
        "sqx_build": SQX_BUILD,
    }


def _parse_content(data: bytes) -> dict[str, object]:
    try:
        payload = json.loads(data)
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise OperatePromotionError("operate_promotion_content_corrupt", "Promotion content is not valid JSON") from exc
    if not isinstance(payload, dict) or set(payload) != _CONTENT_KEYS:
        raise OperatePromotionError("operate_promotion_content_corrupt", "Promotion content shape is invalid")
    if payload.get("schema") != PROMOTION_CONTENT_SCHEMA or payload.get("sqx_build") != SQX_BUILD:
        raise OperatePromotionError("operate_promotion_content_corrupt", "Promotion truth boundary is invalid")
    _typed_entity(payload["proof_entity_id"], ResearchKind.PROOF, "operate_promotion_content_corrupt")
    _typed_revision(payload["proof_revision"], ResearchKind.PROOF, "operate_promotion_content_corrupt")
    _typed_entity(payload["candidate_entity_id"], ResearchKind.CANDIDATE, "operate_promotion_content_corrupt")
    _typed_revision(payload["candidate_revision"], ResearchKind.CANDIDATE, "operate_promotion_content_corrupt")
    _typed_entity(
        payload["historical_result_entity_id"],
        ResearchKind.HISTORICAL_RESULT,
        "operate_promotion_content_corrupt",
    )
    _typed_revision(
        payload["historical_result_revision"],
        ResearchKind.HISTORICAL_RESULT,
        "operate_promotion_content_corrupt",
    )
    if not isinstance(payload.get("candidate_archive_name"), str) or not payload["candidate_archive_name"]:
        raise OperatePromotionError("operate_promotion_content_corrupt", "candidate_archive_name is invalid")
    _digest(payload["candidate_archive_sha256"], "operate_promotion_content_corrupt")
    return payload


def _load_bound_proof(store: FileResearchCustodyStore, content: dict[str, object]) -> dict[str, object]:
    try:
        proof = read_current_research_proof(store, content["proof_entity_id"])  # type: ignore[arg-type]
    except ResearchProofError as exc:
        raise OperatePromotionError("operate_promotion_proof_invalid", exc.detail) from exc
    except ResearchCustodyError as exc:
        raise OperatePromotionError(exc.code, exc.detail) from exc
    expected = _proof_payload(proof)
    if expected != content:
        raise OperatePromotionError(
            "operate_promotion_proof_changed",
            "Current Proof no longer matches the identities bound by this promotion",
        )
    return proof


def _record(
    store: FileResearchCustodyStore,
    entity: ResearchEntityId,
    revision: ResearchRevisionRef,
) -> dict[str, object]:
    stored = store.read_revision(revision)
    if stored.entity_id != entity or entity.kind != ResearchKind.PROMOTION or revision.kind != ResearchKind.PROMOTION:
        raise OperatePromotionError("operate_promotion_revision_invalid", "Promotion revision identity is invalid")
    if stored.parent_revision is not None or stored.evidence:
        raise OperatePromotionError("operate_promotion_content_corrupt", "promotion must be one immutable root revision")
    content = _parse_content(store.read_revision_content(revision))
    _load_bound_proof(store, content)
    return {
        "schema": PROMOTION_READ_SCHEMA,
        "entity_id": str(entity),
        "revision": str(revision),
        "content_ref": str(stored.content),
        "proof_entity_id": content["proof_entity_id"],
        "proof_revision": content["proof_revision"],
        "candidate_entity_id": content["candidate_entity_id"],
        "candidate_revision": content["candidate_revision"],
        "candidate_archive_name": content["candidate_archive_name"],
        "candidate_archive_sha256": content["candidate_archive_sha256"],
        "historical_result_entity_id": content["historical_result_entity_id"],
        "historical_result_revision": content["historical_result_revision"],
        "sqx_build": content["sqx_build"],
    }


def _current_promotion_entities(store: FileResearchCustodyStore) -> tuple[ResearchEntityId, ...]:
    directory = store.base / "current" / ResearchKind.PROMOTION.value
    if not directory.exists():
        return ()
    if not directory.is_dir():
        raise ResearchCustodyError("current_pointer_corrupt", "Promotion current-pointer directory is invalid")
    entities: list[ResearchEntityId] = []
    for path in sorted(directory.iterdir(), key=lambda item: item.name):
        if _CURRENT_POINTER_TEMP_RE.fullmatch(path.name):
            continue
        if not path.is_file() or path.suffix != ".json":
            raise ResearchCustodyError(
                "current_pointer_corrupt",
                "Promotion current-pointer directory contains an unexpected entry",
            )
        try:
            value = UUID(path.stem)
        except ValueError as exc:
            raise ResearchCustodyError("current_pointer_corrupt", "Promotion current-pointer filename is not a canonical UUID") from exc
        if str(value) != path.stem:
            raise ResearchCustodyError("current_pointer_corrupt", "Promotion current-pointer UUID is not canonical")
        entities.append(ResearchEntityId(ResearchKind.PROMOTION, value))
    return tuple(entities)


def create_promotion(store: FileResearchCustodyStore, *, proof_entity_id: str) -> dict[str, object]:
    if not isinstance(store, FileResearchCustodyStore):
        raise OperatePromotionError("operate_promotion_store_invalid", "canonical Research custody store is required")
    try:
        proof = read_current_research_proof(store, proof_entity_id)
    except ResearchProofError as exc:
        # A missing pointer is 404 and a malformed identity is a 400 client error;
        # only a genuine bad-state Proof collapses to the 409 proof_invalid code.
        if exc.code in {"current_pointer_missing", "research_proof_entity_invalid"}:
            raise OperatePromotionError(exc.code, exc.detail) from exc
        raise OperatePromotionError("operate_promotion_proof_invalid", exc.detail) from exc
    except ResearchCustodyError as exc:
        raise OperatePromotionError(exc.code, exc.detail) from exc
    payload = _proof_payload(proof)
    payload_lock = store._lock_path("operate-promotion-create", _canonical(payload).decode("utf-8"))

    with _CREATE_LOCK:
        with store._lock(payload_lock):
            for entity in _current_promotion_entities(store):
                current = store.current(entity)
                existing = _parse_content(store.read_revision_content(current))
                if existing == payload:
                    return {**_record(store, entity, current), "reused": True}

            entity = store.create_entity(ResearchKind.PROMOTION)
            revision = store.create_revision(entity, _canonical(payload))
            store.compare_and_set_current(entity, expected_revision=None, target_revision=revision.revision)
            return {**_record(store, entity, revision.revision), "reused": False}


def read_current_promotion(
    store: FileResearchCustodyStore,
    entity_id: ResearchEntityId | str,
) -> dict[str, object]:
    entity = _typed_entity(str(entity_id), ResearchKind.PROMOTION, "operate_promotion_entity_invalid")
    revision = store.current(entity)
    return _record(store, entity, revision)


def list_current_promotions(store: FileResearchCustodyStore) -> dict[str, object]:
    promotions = []
    for entity in _current_promotion_entities(store):
        record = read_current_promotion(store, entity)
        promotions.append(
            {
                "entity_id": record["entity_id"],
                "revision": record["revision"],
                "proof_entity_id": record["proof_entity_id"],
                "proof_revision": record["proof_revision"],
                "candidate_entity_id": record["candidate_entity_id"],
                "candidate_revision": record["candidate_revision"],
                "candidate_archive_name": record["candidate_archive_name"],
                "historical_result_entity_id": record["historical_result_entity_id"],
            }
        )
    return {"schema": PROMOTION_CATALOG_SCHEMA, "promotions": promotions}
