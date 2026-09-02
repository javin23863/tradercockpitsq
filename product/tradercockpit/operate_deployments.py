"""Operator deployment custody after an existing Export.

Deployment is platform-owned identity custody. It is not live execution, broker
send, MT4/MT5, positions, or P&L. Historical research never becomes live truth
through this module.
"""

from __future__ import annotations

import json
import re
from threading import Lock
from uuid import UUID

from tradercockpit.operate_exports import (
    EXPORT_READ_SCHEMA,
    OperateExportError,
    read_current_export,
)
from tradercockpit.research_custody import (
    FileResearchCustodyStore,
    ResearchCustodyError,
    ResearchEntityId,
    ResearchKind,
    ResearchRevisionRef,
)
from tradercockpit.sqx_presets import SQX_BUILD


DEPLOYMENT_CONTENT_SCHEMA = "tc.operate-deployment-content.v1"
DEPLOYMENT_READ_SCHEMA = "tc.operate-deployment.v1"
DEPLOYMENT_CATALOG_SCHEMA = "tc.operate-deployment-catalog.v1"
DEPLOYMENT_MODE = "identity_only"
DEPLOYMENT_STATUS = "execution_not_connected"
_CURRENT_POINTER_TEMP_RE = re.compile(
    r"^\.[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.json\.tmp-[0-9]+-[0-9a-f]{32}$"
)
_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")
_CREATE_LOCK = Lock()
_CONTENT_KEYS = frozenset(
    {
        "schema",
        "export_entity_id",
        "export_revision",
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


class OperateDeploymentError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


def _canonical(payload: dict[str, object]) -> bytes:
    return json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def _typed_entity(value: object, kind: ResearchKind, code: str) -> ResearchEntityId:
    if not isinstance(value, str) or not value:
        raise OperateDeploymentError(code, "entity identity is required")
    try:
        entity = ResearchEntityId.parse(value)
    except ResearchCustodyError as exc:
        raise OperateDeploymentError(code, "entity identity is invalid") from exc
    if entity.kind != kind:
        raise OperateDeploymentError(code, f"entity identity must be {kind.value} custody")
    return entity


def _typed_revision(value: object, kind: ResearchKind, code: str) -> ResearchRevisionRef:
    if not isinstance(value, str) or not value:
        raise OperateDeploymentError(code, "revision identity is required")
    try:
        revision = ResearchRevisionRef.parse(value)
    except ResearchCustodyError as exc:
        raise OperateDeploymentError(code, "revision identity is invalid") from exc
    if revision.kind != kind:
        raise OperateDeploymentError(code, f"revision identity must be {kind.value} custody")
    return revision


def _digest(value: object, code: str) -> str:
    if not isinstance(value, str) or _DIGEST_RE.fullmatch(value) is None:
        raise OperateDeploymentError(code, "SHA-256 identity is invalid")
    return value


def _deployment_payload(export: dict[str, object]) -> dict[str, object]:
    if not isinstance(export, dict) or export.get("schema") != EXPORT_READ_SCHEMA:
        raise OperateDeploymentError("operate_deployment_export_invalid", "Deployment requires a current Export read model")
    export_entity = _typed_entity(
        export.get("entity_id"), ResearchKind.EXPORT, "operate_deployment_export_invalid"
    )
    export_revision = _typed_revision(
        export.get("revision"), ResearchKind.EXPORT, "operate_deployment_export_invalid"
    )
    promotion_entity = _typed_entity(
        export.get("promotion_entity_id"), ResearchKind.PROMOTION, "operate_deployment_export_invalid"
    )
    promotion_revision = _typed_revision(
        export.get("promotion_revision"), ResearchKind.PROMOTION, "operate_deployment_export_invalid"
    )
    proof_entity = _typed_entity(
        export.get("proof_entity_id"), ResearchKind.PROOF, "operate_deployment_export_invalid"
    )
    proof_revision = _typed_revision(
        export.get("proof_revision"), ResearchKind.PROOF, "operate_deployment_export_invalid"
    )
    candidate_entity = _typed_entity(
        export.get("candidate_entity_id"), ResearchKind.CANDIDATE, "operate_deployment_export_invalid"
    )
    candidate_revision = _typed_revision(
        export.get("candidate_revision"), ResearchKind.CANDIDATE, "operate_deployment_export_invalid"
    )
    historical_entity = _typed_entity(
        export.get("historical_result_entity_id"),
        ResearchKind.HISTORICAL_RESULT,
        "operate_deployment_export_invalid",
    )
    historical_revision = _typed_revision(
        export.get("historical_result_revision"),
        ResearchKind.HISTORICAL_RESULT,
        "operate_deployment_export_invalid",
    )
    archive_name = export.get("candidate_archive_name")
    if not isinstance(archive_name, str) or not archive_name:
        raise OperateDeploymentError("operate_deployment_export_invalid", "candidate_archive_name is missing from Export")
    archive_sha256 = _digest(export.get("candidate_archive_sha256"), "operate_deployment_export_invalid")
    sqx_build = export.get("sqx_build")
    if sqx_build != SQX_BUILD:
        raise OperateDeploymentError("operate_deployment_export_invalid", "Export producer build is not the authorized SQX build")
    return {
        "schema": DEPLOYMENT_CONTENT_SCHEMA,
        "export_entity_id": str(export_entity),
        "export_revision": str(export_revision),
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
        raise OperateDeploymentError("operate_deployment_content_corrupt", "Deployment content is not valid JSON") from exc
    if not isinstance(payload, dict) or set(payload) != _CONTENT_KEYS:
        raise OperateDeploymentError("operate_deployment_content_corrupt", "Deployment content shape is invalid")
    if payload.get("schema") != DEPLOYMENT_CONTENT_SCHEMA or payload.get("sqx_build") != SQX_BUILD:
        raise OperateDeploymentError("operate_deployment_content_corrupt", "Deployment truth boundary is invalid")
    _typed_entity(payload["export_entity_id"], ResearchKind.EXPORT, "operate_deployment_content_corrupt")
    _typed_revision(payload["export_revision"], ResearchKind.EXPORT, "operate_deployment_content_corrupt")
    _typed_entity(payload["promotion_entity_id"], ResearchKind.PROMOTION, "operate_deployment_content_corrupt")
    _typed_revision(payload["promotion_revision"], ResearchKind.PROMOTION, "operate_deployment_content_corrupt")
    _typed_entity(payload["proof_entity_id"], ResearchKind.PROOF, "operate_deployment_content_corrupt")
    _typed_revision(payload["proof_revision"], ResearchKind.PROOF, "operate_deployment_content_corrupt")
    _typed_entity(payload["candidate_entity_id"], ResearchKind.CANDIDATE, "operate_deployment_content_corrupt")
    _typed_revision(payload["candidate_revision"], ResearchKind.CANDIDATE, "operate_deployment_content_corrupt")
    _typed_entity(
        payload["historical_result_entity_id"],
        ResearchKind.HISTORICAL_RESULT,
        "operate_deployment_content_corrupt",
    )
    _typed_revision(
        payload["historical_result_revision"],
        ResearchKind.HISTORICAL_RESULT,
        "operate_deployment_content_corrupt",
    )
    if not isinstance(payload.get("candidate_archive_name"), str) or not payload["candidate_archive_name"]:
        raise OperateDeploymentError("operate_deployment_content_corrupt", "candidate_archive_name is invalid")
    _digest(payload["candidate_archive_sha256"], "operate_deployment_content_corrupt")
    return payload


def _load_bound_export(store: FileResearchCustodyStore, content: dict[str, object]) -> dict[str, object]:
    try:
        export = read_current_export(store, content["export_entity_id"])  # type: ignore[arg-type]
    except (OperateExportError, ResearchCustodyError) as exc:
        raise OperateDeploymentError(exc.code, exc.detail) from exc
    expected = _deployment_payload(export)
    if expected != content:
        raise OperateDeploymentError(
            "operate_deployment_export_changed",
            "Current Export no longer matches the identities bound by this deployment",
        )
    return export


def _record(
    store: FileResearchCustodyStore,
    entity: ResearchEntityId,
    revision: ResearchRevisionRef,
) -> dict[str, object]:
    stored = store.read_revision(revision)
    if stored.entity_id != entity or entity.kind != ResearchKind.DEPLOYMENT or revision.kind != ResearchKind.DEPLOYMENT:
        raise OperateDeploymentError("operate_deployment_revision_invalid", "Deployment revision identity is invalid")
    if stored.parent_revision is not None or stored.evidence:
        raise OperateDeploymentError("operate_deployment_content_corrupt", "deployment must be one immutable root revision")
    content = _parse_content(store.read_revision_content(revision))
    _load_bound_export(store, content)
    return {
        "schema": DEPLOYMENT_READ_SCHEMA,
        "entity_id": str(entity),
        "revision": str(revision),
        "content_ref": str(stored.content),
        "mode": DEPLOYMENT_MODE,
        "status": DEPLOYMENT_STATUS,
        "export_entity_id": content["export_entity_id"],
        "export_revision": content["export_revision"],
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


def _current_deployment_entities(store: FileResearchCustodyStore) -> tuple[ResearchEntityId, ...]:
    directory = store.base / "current" / ResearchKind.DEPLOYMENT.value
    if not directory.exists():
        return ()
    if not directory.is_dir():
        raise ResearchCustodyError("current_pointer_corrupt", "Deployment current-pointer directory is invalid")
    entities: list[ResearchEntityId] = []
    for path in sorted(directory.iterdir(), key=lambda item: item.name):
        if _CURRENT_POINTER_TEMP_RE.fullmatch(path.name):
            continue
        if not path.is_file() or path.suffix != ".json":
            raise ResearchCustodyError(
                "current_pointer_corrupt",
                "Deployment current-pointer directory contains an unexpected entry",
            )
        try:
            value = UUID(path.stem)
        except ValueError as exc:
            raise ResearchCustodyError("current_pointer_corrupt", "Deployment current-pointer filename is not a canonical UUID") from exc
        if str(value) != path.stem:
            raise ResearchCustodyError("current_pointer_corrupt", "Deployment current-pointer UUID is not canonical")
        entities.append(ResearchEntityId(ResearchKind.DEPLOYMENT, value))
    return tuple(entities)


def create_deployment(store: FileResearchCustodyStore, *, export_entity_id: str) -> dict[str, object]:
    if not isinstance(store, FileResearchCustodyStore):
        raise OperateDeploymentError("operate_deployment_store_invalid", "canonical Research custody store is required")
    try:
        export = read_current_export(store, export_entity_id)
    except (OperateExportError, ResearchCustodyError) as exc:
        # Preserve missing/invalid-identity codes so a malformed or unfindable upstream
        # maps consistently (404/400) on both the read and write paths; only a genuine
        # bad-state export collapses to the 409 export_invalid code.
        if exc.code in {"current_pointer_missing", "operate_export_entity_invalid", "entity_id_invalid", "entity_kind_invalid"}:
            raise OperateDeploymentError(exc.code, exc.detail) from exc
        raise OperateDeploymentError("operate_deployment_export_invalid", exc.detail) from exc
    payload = _deployment_payload(export)
    payload_lock = store._lock_path("operate-deployment-create", _canonical(payload).decode("utf-8"))

    with _CREATE_LOCK:
        with store._lock(payload_lock):
            for entity in _current_deployment_entities(store):
                current = store.current(entity)
                existing = _parse_content(store.read_revision_content(current))
                if existing == payload:
                    return {**_record(store, entity, current), "reused": True}

            entity = store.create_entity(ResearchKind.DEPLOYMENT)
            revision = store.create_revision(entity, _canonical(payload))
            store.compare_and_set_current(entity, expected_revision=None, target_revision=revision.revision)
            return {**_record(store, entity, revision.revision), "reused": False}


def read_current_deployment(
    store: FileResearchCustodyStore,
    entity_id: ResearchEntityId | str,
) -> dict[str, object]:
    entity = _typed_entity(str(entity_id), ResearchKind.DEPLOYMENT, "operate_deployment_entity_invalid")
    revision = store.current(entity)
    return _record(store, entity, revision)


def list_current_deployments(store: FileResearchCustodyStore) -> dict[str, object]:
    deployments = []
    for entity in _current_deployment_entities(store):
        record = read_current_deployment(store, entity)
        deployments.append(
            {
                "entity_id": record["entity_id"],
                "revision": record["revision"],
                "export_entity_id": record["export_entity_id"],
                "candidate_entity_id": record["candidate_entity_id"],
                "candidate_archive_name": record["candidate_archive_name"],
                "mode": record["mode"],
                "status": record["status"],
            }
        )
    return {"schema": DEPLOYMENT_CATALOG_SCHEMA, "deployments": deployments}
