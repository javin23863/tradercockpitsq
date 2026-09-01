"""Immutable user-facing Research Proof over the canonical Research chain.

Proof is a custody/readback feature, not a validation engine. It binds one exact
operator-selected Idea revision to one exact completed native Research chain and one
exact producer-backed Higher Precision validation record. The association between the
Idea and the native chain is explicit operator provenance; TraderCockpit does not
invent an Idea -> SQX configuration causality that the producer has not exposed.
"""

from __future__ import annotations

import json
import re
from uuid import UUID

from tradercockpit.research_candidates import ResearchCandidateError, read_current_candidate
from tradercockpit.research_configurations import ResearchConfigurationError, read_current_configuration
from tradercockpit.research_custody import (
    EvidenceRef,
    FileResearchCustodyStore,
    ResearchCustodyError,
    ResearchEntityId,
    ResearchKind,
    ResearchRevisionRef,
)
from tradercockpit.research_ideas import IDEA_READ_SCHEMA, ResearchIdeaContent, ResearchIdeaError
from tradercockpit.research_native_jobs import ResearchNativeJobError, read_current_native_job
from tradercockpit.research_retester import ResearchRetesterError, read_historical_result_revision
from tradercockpit.research_robustness import (
    ROBUSTNESS_METHOD_HIGHER_PRECISION,
    ROBUSTNESS_OPERATION,
    ROBUSTNESS_OUTCOME_UNREAD,
    ResearchRobustnessError,
    read_native_robustness_result,
)
from tradercockpit.research_trades import RESEARCH_TRADES_SCHEMA, ResearchTradesError, read_historical_trades
from tradercockpit.sqx_presets import SQX_BUILD


RESEARCH_PROOF_CONTENT_SCHEMA = "tc.research-proof-content.v1"
RESEARCH_PROOF_READ_SCHEMA = "tc.research-proof.v1"
RESEARCH_PROOF_CATALOG_SCHEMA = "tc.research-proof-catalog.v1"
RESEARCH_PROOF_ASSOCIATION = "operator_selected_exact_idea_revision"
_CURRENT_POINTER_TEMP_RE = re.compile(
    r"^\.[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.json\.tmp-[0-9]+-[0-9a-f]{32}$"
)
_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")


class ResearchProofError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


def _canonical(payload: dict[str, object]) -> bytes:
    return json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def _typed_entity(value: object, kind: ResearchKind, code: str) -> ResearchEntityId:
    if not isinstance(value, str) or not value:
        raise ResearchProofError(code, "entity identity is required")
    try:
        entity = ResearchEntityId.parse(value)
    except ResearchCustodyError as exc:
        raise ResearchProofError(code, "entity identity is invalid") from exc
    if entity.kind != kind:
        raise ResearchProofError(code, f"entity identity must be {kind.value} custody")
    return entity


def _typed_revision(value: object, kind: ResearchKind, code: str) -> ResearchRevisionRef:
    if not isinstance(value, str) or not value:
        raise ResearchProofError(code, "revision identity is required")
    try:
        revision = ResearchRevisionRef.parse(value)
    except ResearchCustodyError as exc:
        raise ResearchProofError(code, "revision identity is invalid") from exc
    if revision.kind != kind:
        raise ResearchProofError(code, f"revision identity must be {kind.value} custody")
    return revision


def _evidence(value: object, code: str) -> EvidenceRef:
    if not isinstance(value, str) or not value:
        raise ResearchProofError(code, "evidence identity is required")
    try:
        return EvidenceRef.parse(value)
    except ResearchCustodyError as exc:
        raise ResearchProofError(code, "evidence identity is invalid") from exc


def _digest(value: object, code: str) -> str:
    if not isinstance(value, str) or _DIGEST_RE.fullmatch(value) is None:
        raise ResearchProofError(code, "SHA-256 identity is invalid")
    return value


def _required_string(record: dict[str, object], key: str, code: str) -> str:
    value = record.get(key)
    if not isinstance(value, str) or not value:
        raise ResearchProofError(code, f"{key} is missing from canonical readback")
    return value


def _read_exact_idea(
    store: FileResearchCustodyStore,
    entity_id: object,
    revision_value: object,
) -> dict[str, object]:
    entity = _typed_entity(entity_id, ResearchKind.IDEA, "research_proof_idea_invalid")
    revision = _typed_revision(revision_value, ResearchKind.IDEA, "research_proof_idea_invalid")
    stored = store.read_revision(revision)
    if stored.entity_id != entity:
        raise ResearchProofError("research_proof_idea_invalid", "Idea revision belongs to another entity")
    try:
        content = ResearchIdeaContent.from_bytes(store.read_revision_content(revision))
    except ResearchIdeaError as exc:
        raise ResearchProofError("research_proof_idea_invalid", exc.detail) from exc
    return {
        "schema": IDEA_READ_SCHEMA,
        "entity_id": str(entity),
        "revision": str(revision),
        "content_ref": str(stored.content),
        "text": content.text,
        "source": content.source,
    }


def _source_records(
    store: FileResearchCustodyStore,
    *,
    idea_entity_id: str,
    idea_revision: str,
    historical_result_entity_id: str,
    historical_result_revision: str,
    validation_ref: str,
) -> dict[str, dict[str, object]]:
    idea = _read_exact_idea(store, idea_entity_id, idea_revision)
    historical_entity = _typed_entity(
        historical_result_entity_id,
        ResearchKind.HISTORICAL_RESULT,
        "research_proof_historical_result_invalid",
    )
    historical_revision_ref = _typed_revision(
        historical_result_revision,
        ResearchKind.HISTORICAL_RESULT,
        "research_proof_historical_result_invalid",
    )
    try:
        historical = read_historical_result_revision(store, historical_entity, historical_revision_ref)
    except (ResearchRetesterError, ResearchCustodyError) as exc:
        raise ResearchProofError("research_proof_historical_result_invalid", getattr(exc, "detail", str(exc))) from exc
    if historical.get("state") != "completed" or historical.get("execution_completed") is not True:
        raise ResearchProofError(
            "research_proof_historical_result_incomplete",
            "Proof requires one completed native Retester Historical Result",
        )

    candidate_entity_id = _required_string(historical, "candidate_entity_id", "research_proof_chain_invalid")
    candidate_revision = _required_string(historical, "candidate_revision", "research_proof_chain_invalid")
    try:
        candidate = read_current_candidate(store, candidate_entity_id)
    except (ResearchCandidateError, ResearchCustodyError) as exc:
        raise ResearchProofError("research_proof_chain_invalid", getattr(exc, "detail", str(exc))) from exc
    if candidate.get("revision") != candidate_revision:
        raise ResearchProofError("research_proof_chain_invalid", "Historical Result Candidate revision is no longer the exact bound Candidate")

    native_job_entity_id = _required_string(candidate, "native_job_entity_id", "research_proof_chain_invalid")
    native_job_revision = _required_string(candidate, "native_job_revision", "research_proof_chain_invalid")
    try:
        native_job = read_current_native_job(store, native_job_entity_id)
    except (ResearchNativeJobError, ResearchCustodyError) as exc:
        raise ResearchProofError("research_proof_chain_invalid", getattr(exc, "detail", str(exc))) from exc
    if native_job.get("revision") != native_job_revision or native_job.get("state") != "submitted":
        raise ResearchProofError("research_proof_chain_invalid", "Candidate does not bind the exact submitted native Builder job")

    configuration_entity_id = _required_string(candidate, "configuration_entity_id", "research_proof_chain_invalid")
    configuration_revision = _required_string(candidate, "configuration_revision", "research_proof_chain_invalid")
    try:
        configuration = read_current_configuration(store, configuration_entity_id)
    except (ResearchConfigurationError, ResearchCustodyError) as exc:
        raise ResearchProofError("research_proof_chain_invalid", getattr(exc, "detail", str(exc))) from exc
    if configuration.get("revision") != configuration_revision or configuration.get("state") != "approved":
        raise ResearchProofError("research_proof_chain_invalid", "Candidate does not bind the exact approved native configuration")
    if (
        native_job.get("configuration_entity_id") != configuration_entity_id
        or native_job.get("configuration_revision") != configuration_revision
    ):
        raise ResearchProofError("research_proof_chain_invalid", "native Builder job and Candidate disagree on approved configuration identity")

    try:
        trades = read_historical_trades(
            store,
            historical_result_entity_id=str(historical_entity),
            expected_historical_result_revision=str(historical_revision_ref),
        )
    except (ResearchTradesError, ResearchRetesterError, ResearchCustodyError) as exc:
        raise ResearchProofError("research_proof_trades_invalid", getattr(exc, "detail", str(exc))) from exc
    if (
        trades.get("schema") != RESEARCH_TRADES_SCHEMA
        or trades.get("historical_result_entity_id") != str(historical_entity)
        or trades.get("historical_result_revision") != str(historical_revision_ref)
        or trades.get("candidate_entity_id") != candidate_entity_id
        or trades.get("candidate_revision") != candidate_revision
        or trades.get("result_archive_ref") != historical.get("result_archive_ref")
        or trades.get("result_archive_sha256") != historical.get("result_archive_sha256")
    ):
        raise ResearchProofError("research_proof_trades_invalid", "Trades readback is not bound to the exact Historical Result")

    validation_evidence = _evidence(validation_ref, "research_proof_validation_invalid")
    try:
        validation = read_native_robustness_result(store, str(validation_evidence))
    except (ResearchRobustnessError, ResearchCustodyError) as exc:
        raise ResearchProofError("research_proof_validation_invalid", getattr(exc, "detail", str(exc))) from exc
    if (
        validation.get("validation_ref") != str(validation_evidence)
        or validation.get("method") != ROBUSTNESS_METHOD_HIGHER_PRECISION
        or validation.get("operation") != ROBUSTNESS_OPERATION
        or validation.get("execution_state") != "completed"
        or validation.get("producer_outcome_state") != ROBUSTNESS_OUTCOME_UNREAD
        or validation.get("source_historical_result_entity_id") != str(historical_entity)
        or validation.get("source_historical_result_revision") != str(historical_revision_ref)
        or validation.get("source_result_archive_ref") != historical.get("result_archive_ref")
        or validation.get("source_result_archive_sha256") != historical.get("result_archive_sha256")
    ):
        raise ResearchProofError(
            "research_proof_validation_invalid",
            "Higher Precision validation is not bound to the exact selected Historical Result",
        )

    if any(
        item.get("sqx_build") != SQX_BUILD
        for item in (configuration, native_job, candidate, historical, validation)
    ):
        raise ResearchProofError("research_proof_build_invalid", "Research chain does not agree on the verified SQX build")

    return {
        "idea": idea,
        "configuration": configuration,
        "native_job": native_job,
        "candidate": candidate,
        "historical_result": historical,
        "trades": trades,
        "validation": validation,
    }


def _content_payload(records: dict[str, dict[str, object]]) -> dict[str, object]:
    idea = records["idea"]
    configuration = records["configuration"]
    native_job = records["native_job"]
    candidate = records["candidate"]
    historical = records["historical_result"]
    validation = records["validation"]
    return {
        "schema": RESEARCH_PROOF_CONTENT_SCHEMA,
        "association_mode": RESEARCH_PROOF_ASSOCIATION,
        "sqx_build": SQX_BUILD,
        "idea_entity_id": idea["entity_id"],
        "idea_revision": idea["revision"],
        "idea_content_ref": idea["content_ref"],
        "configuration_entity_id": configuration["entity_id"],
        "configuration_revision": configuration["revision"],
        "configuration_source_project_ref": configuration["source_project_ref"],
        "configuration_source_project_sha256": configuration["source_project_sha256"],
        "configuration_executable_xml_ref": configuration["executable_xml_ref"],
        "configuration_executable_xml_sha256": configuration["executable_xml_sha256"],
        "native_job_entity_id": native_job["entity_id"],
        "native_job_revision": native_job["revision"],
        "builder_operation": native_job["operation"],
        "builder_launcher_sha256": native_job["launcher_sha256"],
        "candidate_entity_id": candidate["entity_id"],
        "candidate_revision": candidate["revision"],
        "candidate_archive_ref": candidate["archive_ref"],
        "candidate_archive_sha256": candidate["archive_sha256"],
        "historical_result_entity_id": historical["entity_id"],
        "historical_result_revision": historical["revision"],
        "historical_result_archive_ref": historical["result_archive_ref"],
        "historical_result_archive_sha256": historical["result_archive_sha256"],
        "retester_engine_ref": historical["engine_ref"],
        "retester_engine_sha256": historical["engine_sha256"],
        "retester_launcher_sha256": historical["launcher_sha256"],
        "retester_native_project_name": historical["native_project_name"],
        "trades_schema": RESEARCH_TRADES_SCHEMA,
        "validation_ref": validation["validation_ref"],
        "validation_method": validation["method"],
        "validation_operation": validation["operation"],
        "validation_execution_state": validation["execution_state"],
        "validation_producer_outcome_state": validation["producer_outcome_state"],
        "validation_internal_proof_entity_id": validation["proof_entity_id"],
        "validation_internal_proof_revision": validation["proof_revision"],
        "validation_result_archive_ref": validation["result_archive_ref"],
        "validation_result_archive_sha256": validation["result_archive_sha256"],
    }


_PROOF_CONTENT_KEYS = frozenset(_content_payload({
    "idea": {"entity_id": "", "revision": "", "content_ref": ""},
    "configuration": {"entity_id": "", "revision": "", "source_project_ref": "", "source_project_sha256": "", "executable_xml_ref": "", "executable_xml_sha256": ""},
    "native_job": {"entity_id": "", "revision": "", "operation": "", "launcher_sha256": ""},
    "candidate": {"entity_id": "", "revision": "", "archive_ref": "", "archive_sha256": ""},
    "historical_result": {"entity_id": "", "revision": "", "result_archive_ref": "", "result_archive_sha256": "", "engine_ref": "", "engine_sha256": "", "launcher_sha256": "", "native_project_name": ""},
    "validation": {"validation_ref": "", "method": "", "operation": "", "execution_state": "", "producer_outcome_state": "", "proof_entity_id": "", "proof_revision": "", "result_archive_ref": "", "result_archive_sha256": ""},
}).keys())


def _parse_content(data: bytes) -> dict[str, object]:
    try:
        payload = json.loads(data)
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise ResearchProofError("research_proof_content_corrupt", "Proof content is not valid JSON") from exc
    if not isinstance(payload, dict) or set(payload) != _PROOF_CONTENT_KEYS:
        raise ResearchProofError("research_proof_content_corrupt", "Proof content shape is invalid")
    if (
        payload.get("schema") != RESEARCH_PROOF_CONTENT_SCHEMA
        or payload.get("association_mode") != RESEARCH_PROOF_ASSOCIATION
        or payload.get("sqx_build") != SQX_BUILD
        or payload.get("validation_method") != ROBUSTNESS_METHOD_HIGHER_PRECISION
        or payload.get("validation_operation") != ROBUSTNESS_OPERATION
        or payload.get("validation_execution_state") != "completed"
        or payload.get("validation_producer_outcome_state") != ROBUSTNESS_OUTCOME_UNREAD
        or payload.get("trades_schema") != RESEARCH_TRADES_SCHEMA
    ):
        raise ResearchProofError("research_proof_content_corrupt", "Proof truth boundary is invalid")

    _typed_entity(payload["idea_entity_id"], ResearchKind.IDEA, "research_proof_content_corrupt")
    _typed_revision(payload["idea_revision"], ResearchKind.IDEA, "research_proof_content_corrupt")
    _evidence(payload["idea_content_ref"], "research_proof_content_corrupt")
    _typed_entity(payload["configuration_entity_id"], ResearchKind.CONFIGURATION, "research_proof_content_corrupt")
    _typed_revision(payload["configuration_revision"], ResearchKind.CONFIGURATION, "research_proof_content_corrupt")
    _typed_entity(payload["native_job_entity_id"], ResearchKind.NATIVE_JOB, "research_proof_content_corrupt")
    _typed_revision(payload["native_job_revision"], ResearchKind.NATIVE_JOB, "research_proof_content_corrupt")
    _typed_entity(payload["candidate_entity_id"], ResearchKind.CANDIDATE, "research_proof_content_corrupt")
    _typed_revision(payload["candidate_revision"], ResearchKind.CANDIDATE, "research_proof_content_corrupt")
    _typed_entity(payload["historical_result_entity_id"], ResearchKind.HISTORICAL_RESULT, "research_proof_content_corrupt")
    _typed_revision(payload["historical_result_revision"], ResearchKind.HISTORICAL_RESULT, "research_proof_content_corrupt")
    _typed_entity(payload["validation_internal_proof_entity_id"], ResearchKind.PROOF, "research_proof_content_corrupt")
    _typed_revision(payload["validation_internal_proof_revision"], ResearchKind.PROOF, "research_proof_content_corrupt")
    for key in (
        "configuration_source_project_ref",
        "configuration_executable_xml_ref",
        "candidate_archive_ref",
        "historical_result_archive_ref",
        "retester_engine_ref",
        "validation_ref",
        "validation_result_archive_ref",
    ):
        _evidence(payload[key], "research_proof_content_corrupt")
    for key in (
        "configuration_source_project_sha256",
        "configuration_executable_xml_sha256",
        "builder_launcher_sha256",
        "candidate_archive_sha256",
        "historical_result_archive_sha256",
        "retester_engine_sha256",
        "retester_launcher_sha256",
        "validation_result_archive_sha256",
    ):
        _digest(payload[key], "research_proof_content_corrupt")
    for ref_key, digest_key in (
        ("configuration_source_project_ref", "configuration_source_project_sha256"),
        ("configuration_executable_xml_ref", "configuration_executable_xml_sha256"),
        ("candidate_archive_ref", "candidate_archive_sha256"),
        ("historical_result_archive_ref", "historical_result_archive_sha256"),
        ("retester_engine_ref", "retester_engine_sha256"),
        ("validation_result_archive_ref", "validation_result_archive_sha256"),
    ):
        if _evidence(payload[ref_key], "research_proof_content_corrupt").digest != payload[digest_key]:
            raise ResearchProofError("research_proof_content_corrupt", f"{ref_key} does not match {digest_key}")
    for key in ("builder_operation", "retester_native_project_name"):
        if not isinstance(payload.get(key), str) or not payload[key]:
            raise ResearchProofError("research_proof_content_corrupt", f"{key} is invalid")
    return payload


def _assert_content_matches_records(content: dict[str, object], records: dict[str, dict[str, object]]) -> None:
    expected = _content_payload(records)
    if content != expected:
        raise ResearchProofError(
            "research_proof_source_changed",
            "Proof source records no longer reproduce the exact immutable identities bound by this Proof",
        )


def _record(
    store: FileResearchCustodyStore,
    entity: ResearchEntityId,
    revision: ResearchRevisionRef,
) -> dict[str, object]:
    stored = store.read_revision(revision)
    if stored.entity_id != entity or entity.kind != ResearchKind.PROOF or revision.kind != ResearchKind.PROOF:
        raise ResearchProofError("research_proof_revision_invalid", "Proof revision identity is invalid")
    if stored.parent_revision is not None or stored.evidence:
        raise ResearchProofError("research_proof_content_corrupt", "user-facing Proof must be one immutable root revision")
    content = _parse_content(store.read_revision_content(revision))
    records = _source_records(
        store,
        idea_entity_id=content["idea_entity_id"],  # type: ignore[arg-type]
        idea_revision=content["idea_revision"],  # type: ignore[arg-type]
        historical_result_entity_id=content["historical_result_entity_id"],  # type: ignore[arg-type]
        historical_result_revision=content["historical_result_revision"],  # type: ignore[arg-type]
        validation_ref=content["validation_ref"],  # type: ignore[arg-type]
    )
    _assert_content_matches_records(content, records)
    return {
        "schema": RESEARCH_PROOF_READ_SCHEMA,
        "entity_id": str(entity),
        "revision": str(revision),
        "content_ref": str(stored.content),
        "association_mode": RESEARCH_PROOF_ASSOCIATION,
        "sqx_build": SQX_BUILD,
        "idea": records["idea"],
        "configuration": records["configuration"],
        "native_job": records["native_job"],
        "candidate": records["candidate"],
        "historical_result": records["historical_result"],
        "trades": records["trades"],
        "validation": records["validation"],
        "truth": {
            "validation_execution_completed": True,
            "producer_validation_outcome": ROBUSTNESS_OUTCOME_UNREAD,
            "producer_verdict_available": False,
        },
    }


def _current_user_proof_entities(store: FileResearchCustodyStore) -> tuple[ResearchEntityId, ...]:
    directory = store.base / "current" / ResearchKind.PROOF.value
    if not directory.exists():
        return ()
    if not directory.is_dir():
        raise ResearchCustodyError("current_pointer_corrupt", "Proof current-pointer directory is invalid")
    entities: list[ResearchEntityId] = []
    for path in sorted(directory.iterdir(), key=lambda item: item.name):
        if _CURRENT_POINTER_TEMP_RE.fullmatch(path.name):
            continue
        if not path.is_file() or path.suffix != ".json":
            raise ResearchCustodyError("current_pointer_corrupt", "Proof current-pointer directory contains an unexpected entry")
        try:
            value = UUID(path.stem)
        except ValueError as exc:
            raise ResearchCustodyError("current_pointer_corrupt", "Proof current-pointer filename is not a canonical UUID") from exc
        if str(value) != path.stem:
            raise ResearchCustodyError("current_pointer_corrupt", "Proof current-pointer UUID is not canonical")
        entity = ResearchEntityId(ResearchKind.PROOF, value)
        revision = store.current(entity)
        try:
            raw = json.loads(store.read_revision_content(revision))
        except (UnicodeDecodeError, json.JSONDecodeError):
            # Robustness also uses ResearchKind.PROOF. Its strict validators own its
            # schemas; a malformed foreign Proof must not become user Proof authority.
            continue
        if isinstance(raw, dict) and raw.get("schema") == RESEARCH_PROOF_CONTENT_SCHEMA:
            entities.append(entity)
    return tuple(entities)


def create_research_proof(
    store: FileResearchCustodyStore,
    *,
    idea_entity_id: str,
    idea_revision: str,
    historical_result_entity_id: str,
    historical_result_revision: str,
    validation_ref: str,
) -> dict[str, object]:
    if not isinstance(store, FileResearchCustodyStore):
        raise ResearchProofError("research_proof_store_invalid", "canonical Research custody store is required")
    records = _source_records(
        store,
        idea_entity_id=idea_entity_id,
        idea_revision=idea_revision,
        historical_result_entity_id=historical_result_entity_id,
        historical_result_revision=historical_result_revision,
        validation_ref=validation_ref,
    )
    payload = _content_payload(records)

    for entity in _current_user_proof_entities(store):
        current = store.current(entity)
        existing = _parse_content(store.read_revision_content(current))
        if existing == payload:
            return {**_record(store, entity, current), "reused": True}

    entity = store.create_entity(ResearchKind.PROOF)
    revision = store.create_revision(entity, _canonical(payload))
    store.compare_and_set_current(entity, expected_revision=None, target_revision=revision.revision)
    return {**_record(store, entity, revision.revision), "reused": False}


def read_current_research_proof(
    store: FileResearchCustodyStore,
    entity_id: ResearchEntityId | str,
) -> dict[str, object]:
    entity = _typed_entity(str(entity_id), ResearchKind.PROOF, "research_proof_entity_invalid")
    revision = store.current(entity)
    try:
        payload = json.loads(store.read_revision_content(revision))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise ResearchProofError("research_proof_content_corrupt", "Proof content is not valid JSON") from exc
    if not isinstance(payload, dict) or payload.get("schema") != RESEARCH_PROOF_CONTENT_SCHEMA:
        raise ResearchProofError("research_proof_not_user_proof", "Proof entity belongs to another registered Proof schema")
    return _record(store, entity, revision)


def list_current_research_proofs(store: FileResearchCustodyStore) -> dict[str, object]:
    proofs = []
    for entity in _current_user_proof_entities(store):
        record = read_current_research_proof(store, entity)
        proofs.append(
            {
                "entity_id": record["entity_id"],
                "revision": record["revision"],
                "idea_entity_id": record["idea"]["entity_id"],
                "idea_revision": record["idea"]["revision"],
                "historical_result_entity_id": record["historical_result"]["entity_id"],
                "historical_result_revision": record["historical_result"]["revision"],
                "validation_ref": record["validation"]["validation_ref"],
                "producer_validation_outcome": ROBUSTNESS_OUTCOME_UNREAD,
            }
        )
    return {"schema": RESEARCH_PROOF_CATALOG_SCHEMA, "proofs": proofs}
