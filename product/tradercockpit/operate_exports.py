"""Operator export custody after an existing Promotion.

Export is platform-owned Delivery identity. It is not live execution, broker/MT4
export, deployment, paper/prop simulation, or a cockpit verdict. Historical
research never becomes live P&L through this module.
"""

from __future__ import annotations

import json
import re
from threading import Lock
from uuid import UUID

from tradercockpit.operate_promotions import (
    PROMOTION_READ_SCHEMA,
    OperatePromotionError,
    read_current_promotion,
)
from tradercockpit.research_custody import (
    FileResearchCustodyStore,
    ResearchCustodyError,
    ResearchEntityId,
    ResearchKind,
    ResearchRevisionRef,
)
from tradercockpit.sqx_presets import SQX_BUILD


EXPORT_CONTENT_SCHEMA = "tc.operate-export-content.v1"
EXPORT_READ_SCHEMA = "tc.operate-export.v1"
EXPORT_CATALOG_SCHEMA = "tc.operate-export-catalog.v1"
_CURRENT_POINTER_TEMP_RE = re.compile(
    r"^\.[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.json\.tmp-[0-9]+-[0-9a-f]{32}$"
)
_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")
_CREATE_LOCK = Lock()
_CONTENT_KEYS = frozenset(
    {
        "schema",
        "promotion_entity_id",
        "promotion_revision",
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


class OperateExportError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


def _canonical(payload: dict[str, object]) -> bytes:
    return json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def _typed_entity(value: object, kind: ResearchKind, code: str) -> ResearchEntityId:
    if not isinstance(value, str) or not value:
        raise OperateExportError(code, "entity identity is required")
    try:
        entity = ResearchEntityId.parse(value)
    except ResearchCustodyError as exc:
        raise OperateExportError(code, "entity identity is invalid") from exc
    if entity.kind != kind:
        raise OperateExportError(code, f"entity identity must be {kind.value} custody")
    return entity


def _typed_revision(value: object, kind: ResearchKind, code: str) -> ResearchRevisionRef:
    if not isinstance(value, str) or not value:
        raise OperateExportError(code, "revision identity is required")
    try:
        revision = ResearchRevisionRef.parse(value)
    except ResearchCustodyError as exc:
        raise OperateExportError(code, "revision identity is invalid") from exc
    if revision.kind != kind:
        raise OperateExportError(code, f"revision identity must be {kind.value} custody")
    return revision


def _digest(value: object, code: str) -> str:
    if not isinstance(value, str) or _DIGEST_RE.fullmatch(value) is None:
        raise OperateExportError(code, "SHA-256 identity is invalid")
    return value


def _export_payload(promotion: dict[str, object]) -> dict[str, object]:
    if not isinstance(promotion, dict) or promotion.get("schema") != PROMOTION_READ_SCHEMA:
        raise OperateExportError("operate_export_promotion_invalid", "Export requires a current Promotion read model")
    promotion_entity = _typed_entity(
        promotion.get("entity_id"), ResearchKind.PROMOTION, "operate_export_promotion_invalid"
    )
    promotion_revision = _typed_revision(
        promotion.get("revision"), ResearchKind.PROMOTION, "operate_export_promotion_invalid"
    )
    proof_entity = _typed_entity(
        promotion.get("proof_entity_id"), ResearchKind.PROOF, "operate_export_promotion_invalid"
    )
    proof_revision = _typed_revision(
        promotion.get("proof_revision"), ResearchKind.PROOF, "operate_export_promotion_invalid"
    )
    candidate_entity = _typed_entity(
        promotion.get("candidate_entity_id"), ResearchKind.CANDIDATE, "operate_export_promotion_invalid"
    )
    candidate_revision = _typed_revision(
        promotion.get("candidate_revision"), ResearchKind.CANDIDATE, "operate_export_promotion_invalid"
    )
    historical_entity = _typed_entity(
        promotion.get("historical_result_entity_id"),
        ResearchKind.HISTORICAL_RESULT,
        "operate_export_promotion_invalid",
    )
    historical_revision = _typed_revision(
        promotion.get("historical_result_revision"),
        ResearchKind.HISTORICAL_RESULT,
        "operate_export_promotion_invalid",
    )
    archive_name = promotion.get("candidate_archive_name")
    if not isinstance(archive_name, str) or not archive_name:
        raise OperateExportError("operate_export_promotion_invalid", "candidate_archive_name is missing from Promotion")
    archive_sha256 = _digest(promotion.get("candidate_archive_sha256"), "operate_export_promotion_invalid")
    sqx_build = promotion.get("sqx_build")
    if sqx_build != SQX_BUILD:
        raise OperateExportError("operate_export_promotion_invalid", "Promotion producer build is not the authorized SQX build")
    return {
        "schema": EXPORT_CONTENT_SCHEMA,
        "promotion_entity_id": str(promotion_entity),
        "promotion_revision": str(promotion_revision),
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
        raise OperateExportError("operate_export_content_corrupt", "Export content is not valid JSON") from exc
    if not isinstance(payload, dict) or set(payload) != _CONTENT_KEYS:
        raise OperateExportError("operate_export_content_corrupt", "Export content shape is invalid")
    if payload.get("schema") != EXPORT_CONTENT_SCHEMA or payload.get("sqx_build") != SQX_BUILD:
        raise OperateExportError("operate_export_content_corrupt", "Export truth boundary is invalid")
    _typed_entity(payload["promotion_entity_id"], ResearchKind.PROMOTION, "operate_export_content_corrupt")
    _typed_revision(payload["promotion_revision"], ResearchKind.PROMOTION, "operate_export_content_corrupt")
    _typed_entity(payload["proof_entity_id"], ResearchKind.PROOF, "operate_export_content_corrupt")
    _typed_revision(payload["proof_revision"], ResearchKind.PROOF, "operate_export_content_corrupt")
    _typed_entity(payload["candidate_entity_id"], ResearchKind.CANDIDATE, "operate_export_content_corrupt")
    _typed_revision(payload["candidate_revision"], ResearchKind.CANDIDATE, "operate_export_content_corrupt")
    _typed_entity(
        payload["historical_result_entity_id"],
        ResearchKind.HISTORICAL_RESULT,
        "operate_export_content_corrupt",
    )
    _typed_revision(
        payload["historical_result_revision"],
        ResearchKind.HISTORICAL_RESULT,
        "operate_export_content_corrupt",
    )
    if not isinstance(payload.get("candidate_archive_name"), str) or not payload["candidate_archive_name"]:
        raise OperateExportError("operate_export_content_corrupt", "candidate_archive_name is invalid")
    _digest(payload["candidate_archive_sha256"], "operate_export_content_corrupt")
    return payload


def _load_bound_promotion(store: FileResearchCustodyStore, content: dict[str, object]) -> dict[str, object]:
    try:
        promotion = read_current_promotion(store, content["promotion_entity_id"])  # type: ignore[arg-type]
    except (OperatePromotionError, ResearchCustodyError) as exc:
        raise OperateExportError(exc.code, exc.detail) from exc
    expected = _export_payload(promotion)
    if expected != content:
        raise OperateExportError(
            "operate_export_promotion_changed",
            "Current Promotion no longer matches the identities bound by this export",
        )
    return promotion


def _record(
    store: FileResearchCustodyStore,
    entity: ResearchEntityId,
    revision: ResearchRevisionRef,
) -> dict[str, object]:
    stored = store.read_revision(revision)
    if stored.entity_id != entity or entity.kind != ResearchKind.EXPORT or revision.kind != ResearchKind.EXPORT:
        raise OperateExportError("operate_export_revision_invalid", "Export revision identity is invalid")
    if stored.parent_revision is not None or stored.evidence:
        raise OperateExportError("operate_export_content_corrupt", "export must be one immutable root revision")
    content = _parse_content(store.read_revision_content(revision))
    _load_bound_promotion(store, content)
    return {
        "schema": EXPORT_READ_SCHEMA,
        "entity_id": str(entity),
        "revision": str(revision),
        "content_ref": str(stored.content),
        "promotion_entity_id": content["promotion_entity_id"],
        "promotion_revision": content["promotion_revision"],
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


def _current_export_entities(store: FileResearchCustodyStore) -> tuple[ResearchEntityId, ...]:
    directory = store.base / "current" / ResearchKind.EXPORT.value
    if not directory.exists():
        return ()
    if not directory.is_dir():
        raise ResearchCustodyError("current_pointer_corrupt", "Export current-pointer directory is invalid")
    entities: list[ResearchEntityId] = []
    for path in sorted(directory.iterdir(), key=lambda item: item.name):
        if _CURRENT_POINTER_TEMP_RE.fullmatch(path.name):
            continue
        if not path.is_file() or path.suffix != ".json":
            raise ResearchCustodyError(
                "current_pointer_corrupt",
                "Export current-pointer directory contains an unexpected entry",
            )
        try:
            value = UUID(path.stem)
        except ValueError as exc:
            raise ResearchCustodyError("current_pointer_corrupt", "Export current-pointer filename is not a canonical UUID") from exc
        if str(value) != path.stem:
            raise ResearchCustodyError("current_pointer_corrupt", "Export current-pointer UUID is not canonical")
        entities.append(ResearchEntityId(ResearchKind.EXPORT, value))
    return tuple(entities)


def create_export(store: FileResearchCustodyStore, *, promotion_entity_id: str) -> dict[str, object]:
    if not isinstance(store, FileResearchCustodyStore):
        raise OperateExportError("operate_export_store_invalid", "canonical Research custody store is required")
    try:
        promotion = read_current_promotion(store, promotion_entity_id)
    except (OperatePromotionError, ResearchCustodyError) as exc:
        code = exc.code if exc.code == "current_pointer_missing" else "operate_export_promotion_invalid"
        raise OperateExportError(code, exc.detail) from exc
    payload = _export_payload(promotion)
    payload_lock = store._lock_path("operate-export-create", _canonical(payload).decode("utf-8"))

    with _CREATE_LOCK:
        with store._lock(payload_lock):
            for entity in _current_export_entities(store):
                current = store.current(entity)
                existing = _parse_content(store.read_revision_content(current))
                if existing == payload:
                    return {**_record(store, entity, current), "reused": True}

            entity = store.create_entity(ResearchKind.EXPORT)
            revision = store.create_revision(entity, _canonical(payload))
            store.compare_and_set_current(entity, expected_revision=None, target_revision=revision.revision)
            return {**_record(store, entity, revision.revision), "reused": False}


def read_current_export(
    store: FileResearchCustodyStore,
    entity_id: ResearchEntityId | str,
) -> dict[str, object]:
    entity = _typed_entity(str(entity_id), ResearchKind.EXPORT, "operate_export_entity_invalid")
    revision = store.current(entity)
    return _record(store, entity, revision)


def list_current_exports(store: FileResearchCustodyStore) -> dict[str, object]:
    exports = []
    for entity in _current_export_entities(store):
        record = read_current_export(store, entity)
        exports.append(
            {
                "entity_id": record["entity_id"],
                "revision": record["revision"],
                "promotion_entity_id": record["promotion_entity_id"],
                "proof_entity_id": record["proof_entity_id"],
                "candidate_entity_id": record["candidate_entity_id"],
                "candidate_archive_name": record["candidate_archive_name"],
            }
        )
    return {"schema": EXPORT_CATALOG_SCHEMA, "exports": exports}
