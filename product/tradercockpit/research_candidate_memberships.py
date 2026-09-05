"""Native databank locations/history in the existing research custody store.

Native action adapters supply already verified bytes and exact before/after
locations. A location/name is never sufficient to infer Candidate lineage.
"""

from datetime import datetime, timezone
from contextlib import contextmanager, nullcontext
import json
from threading import get_ident

from .research_candidates import _candidate_entity, _canonical, _digest, _native_name, read_candidate_revision
from .research_custody import EvidenceRef, FileResearchCustodyStore, ResearchCustodyError, ResearchEntityId, ResearchKind, ResearchRevisionRef

SCHEMA = "tc.research-candidate-memberships.v1"


class _AdmissionBatch:
    def __init__(self, store):
        from .research_candidates import list_current_candidates
        self.store, self.thread, self.active = store, get_ident(), True
        self.rows = {_key(row): row for row in list_databank_memberships(store)["memberships"]}
        self.legacy = [row for row in list_current_candidates(store)["candidates"] if "origin" not in row]


def _check_admission_batch(store, inventory):
    if not isinstance(inventory, _AdmissionBatch) or inventory.store is not store or inventory.thread != get_ident() or not inventory.active:
        raise ResearchCustodyError("candidate_admission_batch_invalid", "Admission inventory is not active in this store operation.")


@contextmanager
def candidate_admission_batch(store):
    """One verified inventory under the existing membership lock, never a cache."""
    with store._lock(store._lock_path("candidate-memberships", "desktop")):
        inventory = _AdmissionBatch(store)
        try:
            yield inventory
        finally:
            inventory.active = False


def _location(payload, *, digest=False):
    keys = {"project", "databank", "archive"} | ({"archive_sha256"} if digest else set())
    if not isinstance(payload, dict) or set(payload) != keys:
        raise ResearchCustodyError("candidate_membership_invalid", "Select an exact native project, databank and archive.")
    for name in ("project", "databank", "archive"):
        _native_name(payload[name])
    if not payload["archive"].lower().endswith(".sqx"):
        raise ResearchCustodyError("candidate_membership_invalid", "Membership requires a complete .sqx archive.")
    if digest:
        _digest(payload["archive_sha256"], code="candidate_membership_invalid")
    return dict(payload)


def _key(row):
    return tuple(row[key] for key in ("project", "databank", "archive"))


def _membership_entity(candidate):
    return ResearchEntityId(ResearchKind.CANDIDATE_MEMBERSHIP, _candidate_entity(candidate).value)


def assert_candidate_membership_action(store, candidate_entity_id, *, action, source=None):
    """Native adapters call before effects, under their shared native action lock."""
    if store.deletion_record(_candidate_entity(candidate_entity_id)) is not None:
        raise ResearchCustodyError("entity_deleted", "This Candidate was deliberately deleted.")
    if _purge_path(store, candidate_entity_id).is_file():
        intent = read_candidate_purge(store, candidate_entity_id)
        if intent["state"] != "prepared" or action not in {"remove", "reserialize"} or not isinstance(source, dict) or not any(all(row.get(key) == value for key, value in source.items()) for row in confirmed_purge_memberships(store, intent)):
            raise ResearchCustodyError("candidate_purge_pending", "Only confirmed membership removal or explicit storage reconciliation is allowed while Candidate deletion is pending.")
        _location(source, digest=True)


def _read(store, entity, revision):
    envelope = store.read_revision(revision)
    try:
        payload = json.loads(store.read_revision_content(revision))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise ResearchCustodyError("candidate_membership_corrupt", "Membership content is not valid JSON.") from exc
    candidate_id = str(ResearchEntityId(ResearchKind.CANDIDATE, entity.value))
    if envelope.entity_id != entity or not isinstance(payload, dict) or set(payload) != {"schema", "candidate_entity_id", "memberships", "event"} or payload.get("schema") != SCHEMA or payload.get("candidate_entity_id") != candidate_id or not isinstance(payload.get("memberships"), list):
        raise ResearchCustodyError("candidate_membership_corrupt", "Candidate membership custody is invalid.")
    seen, evidence = set(), set()
    for row in payload["memberships"]:
        if not isinstance(row, dict) or set(row) != {"project", "databank", "archive", "archive_sha256", "archive_ref", "candidate_revision"}:
            raise ResearchCustodyError("candidate_membership_corrupt", "Candidate membership row is invalid.")
        _location({key: row[key] for key in ("project", "databank", "archive", "archive_sha256")}, digest=True)
        if _key(row) in seen:
            raise ResearchCustodyError("candidate_membership_corrupt", "Candidate has duplicate native locations.")
        seen.add(_key(row))
        ref = EvidenceRef.parse(row["archive_ref"])
        if ref.digest != row["archive_sha256"]:
            raise ResearchCustodyError("candidate_membership_corrupt", "Membership archive digest is invalid.")
        store.read_evidence(ref)
        evidence.add(ref)
        read_candidate_revision(store, candidate_id, row["candidate_revision"])
    event = payload["event"]
    if not isinstance(event, dict) or set(event) != {"action", "source", "destination", "candidate_revision", "observed_at_utc"} or event["action"] not in {"admit", "rename", "copy", "move", "remove", "reserialize"}:
        raise ResearchCustodyError("candidate_membership_corrupt", "Membership event is invalid.")
    read_candidate_revision(store, candidate_id, event["candidate_revision"])
    try:
        observed = datetime.fromisoformat(event["observed_at_utc"])
        if observed.utcoffset() != timezone.utc.utcoffset(observed):
            raise ValueError()
    except (TypeError, ValueError) as exc:
        raise ResearchCustodyError("candidate_membership_corrupt", "Membership observation time is invalid.") from exc
    for name in ("source", "destination"):
        if event[name] is not None:
            _location(event[name], digest=True)
    if event["action"] == "reserialize":
        if event["source"] is None or event["destination"] is None or _key(event["source"]) != _key(event["destination"]) or envelope.parent_revision is None:
            raise ResearchCustodyError("candidate_membership_corrupt", "Reserialization must retain the exact previous location.")
        parent = _read(store, entity, envelope.parent_revision)
        before = next((row for row in parent["memberships"] if _key(row) == _key(event["source"])), None)
        after = next((row for row in payload["memberships"] if _key(row) == _key(event["destination"])), None)
        if any(row is None or row["candidate_revision"] != event["candidate_revision"] or any(row[key] != value for key, value in location.items())
               for row, location in ((before, event["source"]), (after, event["destination"]))):
            raise ResearchCustodyError("candidate_membership_corrupt", "Reserialization does not bind its exact parent membership.")
        if [row for row in parent["memberships"] if _key(row) != _key(before)] != [row for row in payload["memberships"] if _key(row) != _key(after)]:
            raise ResearchCustodyError("candidate_membership_corrupt", "Reserialization changed unrelated native memberships.")
        old_ref = EvidenceRef.parse(before["archive_ref"])
        _verify_reserialization(store, candidate_id, event["candidate_revision"], store.read_evidence(old_ref), store.read_evidence(EvidenceRef.parse(after["archive_ref"])))
        evidence.add(old_ref)
    if set(envelope.evidence) != evidence:
        raise ResearchCustodyError("candidate_membership_corrupt", "Membership evidence set is invalid.")
    return {**payload, "entity_id": str(entity), "revision": str(revision)}


def _verify_reserialization(store, candidate_id, revision, previous, current):
    from .sqx_candidate_identity import read_candidate_token, verify_native_reserialization, SqxCandidateIdentityError
    candidate = read_candidate_revision(store, candidate_id, revision)
    token = read_candidate_token(store.read_evidence(EvidenceRef.parse(candidate["archive_ref"])))
    if token is None:
        raise SqxCandidateIdentityError("candidate_legacy_reimport_required", "This legacy Candidate has no saved identity marker. Save the current .sqx and load it into a different databank as a new Candidate; existing history remains separate.")
    # A token narrows an already hash-bound location. It never discovers lineage.
    verify_native_reserialization(previous, current, token)


def read_candidate_memberships(store, candidate_entity_id, *, history=False):
    entity = _membership_entity(candidate_entity_id)
    current = store._read_current(entity)
    if current is None:
        return {"schema": SCHEMA, "candidate_entity_id": str(_candidate_entity(candidate_entity_id)), "revision": None, "memberships": [], "history": []}
    result = _read(store, entity, current)
    if history:
        records = []
        revision = current
        while revision is not None:
            records.append(_read(store, entity, revision))
            revision = store.read_revision(revision).parent_revision
        result["history"] = list(reversed(records))
    return result


def list_databank_memberships(store, *, project=None, databank=None):
    for value in (project, databank):
        if value is not None:
            _native_name(value)
    directory = store.base / "current" / ResearchKind.CANDIDATE_MEMBERSHIP.value
    rows = []
    if directory.exists():
        from .research_candidates import _CURRENT_POINTER_TEMP_RE
        from uuid import UUID
        for path in sorted(directory.iterdir()):
            if _CURRENT_POINTER_TEMP_RE.fullmatch(path.name):
                continue
            try:
                identity = UUID(path.stem)
                if path.suffix != ".json" or str(identity) != path.stem or not path.is_file():
                    raise ValueError()
            except ValueError as exc:
                raise ResearchCustodyError("candidate_membership_corrupt", "Membership current pointer is invalid.") from exc
            entity = ResearchEntityId(ResearchKind.CANDIDATE_MEMBERSHIP, identity)
            record = _read(store, entity, store.current(entity))
            rows.extend({**row, "candidate_entity_id": record["candidate_entity_id"], "membership_revision": record["revision"]} for row in record["memberships"] if (project is None or row["project"] == project) and (databank is None or row["databank"] == databank))
    if len({_key(row) for row in rows}) != len(rows):
        raise ResearchCustodyError("candidate_membership_corrupt", "Multiple Candidates claim one active native location.")
    return {"schema": "tc.research-databank-memberships.v1", "memberships": rows}


def associate_databank_results(store, payload):
    """Add optional custody identity only for an exact observed native artifact."""
    from .sqx_custom_project import SQX_CUSTOM_PROJECT_RESULTS_SCHEMA
    from .sqx_candidate_identity import read_candidate_token, SqxCandidateIdentityError
    if not isinstance(payload, dict) or payload.get("schema") != SQX_CUSTOM_PROJECT_RESULTS_SCHEMA or not isinstance(payload.get("projects"), list):
        raise ResearchCustodyError("candidate_membership_invalid", "Native databank results read model is invalid.")
    index = {(*_key(row), row["archive_sha256"]): row for row in list_databank_memberships(store, project=payload.get("project"))["memberships"]}
    locations = {key[:3]: row for key, row in index.items()}
    projects = []
    for project in payload["projects"]:
        banks = []
        for bank in project["databanks"]:
            strategies = []
            for strategy in bank["strategies"]:
                binding = index.get((project["name"], bank["name"], strategy["archive"], strategy.get("archive_sha256"))) if strategy.get("inspectable") is True else None
                association = ({"schema": "tc.research-native-candidate-association.v1",
                                "candidate_entity_id": binding["candidate_entity_id"],
                                "candidate_revision": binding["candidate_revision"],
                                "membership_revision": binding["membership_revision"],
                                "archive_sha256": binding["archive_sha256"]} if binding else None)
                retained = locations.get((project["name"], bank["name"], strategy["archive"])) if strategy.get("inspectable") is True else None
                reconciliation = ({"schema": "tc.research-native-candidate-reconciliation.v1",
                    "candidate_entity_id": retained["candidate_entity_id"], "candidate_revision": retained["candidate_revision"],
                    "membership_revision": retained["membership_revision"], "previous_archive_sha256": retained["archive_sha256"],
                    "archive_sha256": strategy["archive_sha256"]} if retained and binding is None else None)
                if reconciliation:
                    candidate = read_candidate_revision(store, retained["candidate_entity_id"], retained["candidate_revision"])
                    try:
                        if read_candidate_token(store.read_evidence(EvidenceRef.parse(candidate["archive_ref"]))) is None:
                            reconciliation["unavailable_reason"] = "candidate_legacy_reimport_required"
                    except SqxCandidateIdentityError as exc:
                        reconciliation["unavailable_reason"] = exc.code
                strategies.append({**strategy, "candidate_association": association, "candidate_reconciliation": reconciliation})
            banks.append({**bank, "strategies": strategies})
        projects.append({**project, "databanks": banks})
    return {**payload, "projects": projects}


def record_databank_membership_operation(
    store: FileResearchCustodyStore, *, action: str, candidate_entity_id: str,
    candidate_revision: str, source=None, destination=None, archive_bytes=None,
    expected_membership_revision: str | None = None,
    _admission_inventory=None,
):
    """Record one producer-confirmed operation, never invoke the native producer.

    source = {project, databank, archive, archive_sha256}; destination has the
    three location keys, with its exact persisted archive supplied separately.
    Rename/copy/move bind explicit source custody; removal keeps all old evidence.
    """
    if action not in {"admit", "rename", "copy", "move", "remove", "reserialize"}:
        raise ResearchCustodyError("candidate_membership_invalid", "Unknown native membership operation.")
    if (action == "admit") != (source is None) or (action == "remove") != (destination is None):
        raise ResearchCustodyError("candidate_membership_invalid", "Operation source/destination do not match its action.")
    source = _location(source, digest=True) if source is not None else None
    destination = _location(destination) if destination is not None else None
    if action == "reserialize" and _key(source) != _key(destination):
        raise ResearchCustodyError("candidate_membership_invalid", "Reserialization cannot change the native location.")
    candidate = read_candidate_revision(store, candidate_entity_id, candidate_revision)
    if source is not None and expected_membership_revision is None:
        raise ResearchCustodyError("candidate_membership_invalid", "An exact membership revision is required.")
    if destination is not None:
        from .sqx_databank_actions import inspect_databank_upload
        inspected = inspect_databank_upload(archive_bytes, destination["archive"])
        destination["archive_sha256"] = inspected["archive_sha256"]
        if action == "admit" and destination["archive_sha256"] != candidate["archive_sha256"]:
            raise ResearchCustodyError("candidate_membership_invalid", "Admission bytes differ from the exact Candidate artifact.")
    elif archive_bytes is not None:
        raise ResearchCustodyError("candidate_membership_invalid", "Removal must not supply a new artifact.")
    entity = _membership_entity(candidate_entity_id)
    if _admission_inventory is not None:
        _check_admission_batch(store, _admission_inventory)
    # ponytail: one desktop membership lock protects location uniqueness. Split
    # by project only if measured throughput warrants cross-project transactions.
    with (nullcontext() if _admission_inventory is not None else store._lock(store._lock_path("candidate-memberships", "desktop"))):
        assert_candidate_membership_action(store, candidate_entity_id, action=action, source=source)
        current = store._read_current(entity)
        previous = _read(store, entity, current) if current else None
        rows = list(previous["memberships"]) if previous else []
        event = {"action": action, "source": source, "destination": destination, "candidate_revision": candidate_revision}
        if previous and all(previous["event"][key] == value for key, value in event.items()):
            if action == "reserialize" and str(store.read_revision(current).parent_revision) != expected_membership_revision:
                raise ResearchCustodyError("current_conflict", "Reserialization retry requires its exact original membership revision.")
            return {**previous, "reused": True}
        if source is not None and str(current) != expected_membership_revision:
            raise ResearchCustodyError("current_conflict", "Candidate membership changed before native action readback.")
        if source is not None and not any(row["candidate_revision"] == candidate_revision and all(row[key] == value for key, value in source.items()) for row in rows):
            raise ResearchCustodyError("candidate_membership_stale", "Selected source is not an exact current Candidate membership.")
        if action == "reserialize":
            _verify_reserialization(store, candidate_entity_id, candidate_revision, store.read_evidence(EvidenceRef(source["archive_sha256"])), archive_bytes)
        if destination is not None:
            existing = (_admission_inventory.rows.get(_key(destination)) if _admission_inventory is not None else next((row for row in list_databank_memberships(store)["memberships"] if _key(row) == _key(destination)), None))
            if existing is not None:
                if action == "admit" and existing["candidate_entity_id"] == candidate_entity_id and existing["archive_sha256"] == destination["archive_sha256"]:
                    return {**previous, "reused": True}
                if action == "copy" and existing["candidate_entity_id"] == candidate_entity_id and existing["candidate_revision"] == candidate_revision and existing["archive_sha256"] == destination["archive_sha256"]:
                    return {**previous, "reused": True}
                if not (action == "reserialize" and existing["candidate_entity_id"] == candidate_entity_id and existing["archive_sha256"] == source["archive_sha256"]):
                    raise ResearchCustodyError("candidate_membership_collision", "Destination already belongs to a retained Candidate; reconcile native state before continuing.")
        if action in {"rename", "move", "remove", "reserialize"}:
            rows = [row for row in rows if _key(row) != _key(source)]
        if destination is not None:
            ref = store.put_evidence(archive_bytes)
            rows.append({**destination, "archive_ref": str(ref), "candidate_revision": candidate_revision})
        rows.sort(key=_key)
        payload = {"schema": SCHEMA, "candidate_entity_id": candidate_entity_id, "memberships": rows,
                   "event": {**event, "observed_at_utc": datetime.now(timezone.utc).isoformat()}}
        evidence = {EvidenceRef.parse(row["archive_ref"]) for row in rows}
        if action == "reserialize":
            evidence.add(EvidenceRef(source["archive_sha256"]))
        stored = store.create_revision(entity, _canonical(payload), parent_revision=current, evidence=tuple(evidence))
        store.compare_and_set_current(entity, expected_revision=current, target_revision=stored.revision)
        if _admission_inventory is not None:
            for row in rows:
                _admission_inventory.rows[_key(row)] = {**row, "candidate_entity_id": candidate_entity_id, "membership_revision": str(stored.revision)}
        return {**_read(store, entity, stored.revision), "reused": False}


def _strings(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for key, item in value.items():
            yield from _strings(key)
            yield from _strings(item)
    elif isinstance(value, list):
        for item in value:
            yield from _strings(item)


def _evidence_closure(store, roots):
    """Follow refs in JSON manifests as well as envelope attachment lists."""
    pending, seen, strings = list(roots), set(), set()
    while pending:
        ref = pending.pop()
        if ref in seen:
            continue
        raw = store.read_evidence(ref)
        seen.add(ref)
        try:
            values = set(_strings(json.loads(raw)))
        except (UnicodeDecodeError, json.JSONDecodeError):
            continue
        strings.update(values)
        for value in values:
            if value.startswith("tc-evidence:sha256:"):
                pending.append(EvidenceRef.parse(value))
            elif len(value) == 64 and all(c in "0123456789abcdef" for c in value):
                candidate = EvidenceRef(value)
                if store._evidence_path(candidate).is_file():
                    pending.append(candidate)
    return seen, strings


def _purge_path(store, candidate):
    return store.base / "candidate-purges" / f"{_candidate_entity(candidate).value}.json"


def _physical_store_path(store, relative):
    from pathlib import PurePosixPath
    path = PurePosixPath(relative)
    if not relative or path.is_absolute() or ".." in path.parts or "\\" in relative or ":" in relative:
        raise ResearchCustodyError("purge_path_invalid", "Purge path is outside product custody.")
    target = store.base.joinpath(*path.parts)
    current = store.base
    for part in path.parts:
        current = current / part
        if current.is_symlink() or (hasattr(current, "is_junction") and current.is_junction()):
            raise ResearchCustodyError("purge_path_invalid", "Purge refuses linked custody paths.")
    if target.resolve() != target.absolute() or not target.resolve().is_relative_to(store.base):
        raise ResearchCustodyError("purge_path_invalid", "Purge path escaped product custody.")
    return target


def _cancel_import_journal(store, candidate, descriptor):
    """Cancel only an exact import that has never been submitted to native code."""
    from hashlib import sha256
    from .sqx_databank_actions import _read_journal, _request
    from .sqx_candidate_identity import stamp_import_candidate_token
    if not isinstance(descriptor, dict) or set(descriptor) != {"mutation_id", "journal_sha256", "native_disposition"}:
        raise ResearchCustodyError("candidate_import_cancel_invalid", "Select one exact retained import cancellation.")
    for key in ("mutation_id", "journal_sha256"):
        _digest(descriptor[key], code="candidate_import_cancel_invalid")
    path = store.root / "databank-actions" / f"{descriptor['mutation_id']}.json"
    journal = _read_journal(store, path)
    if (sha256(path.read_bytes()).hexdigest() != descriptor["journal_sha256"]
            or journal["action"] != "load" or journal["candidate_entity_id"] != str(candidate)
            or journal["phase"] != "prepared" or descriptor["native_disposition"] != "not_submitted"
            or store._current_path(candidate).exists() or read_candidate_memberships(store, candidate)["memberships"]):
        raise ResearchCustodyError("candidate_import_cancel_invalid", "Import cancellation no longer matches unpublished, detached custody.")
    request = journal["request"]
    _request(request, {"project", "databank", "archive", "source_sha256", "operation_id"})
    destination = {key: request[key] for key in ("project", "databank", "archive")}
    if (journal["destination"] != destination or journal["source"] != {**destination, "archive_sha256": request["source_sha256"]}
            or journal["membership_revision"] is not None or journal["receipt"] is not None
            or journal["output_ref"] is not None or journal["output_sha256"] is not None
            or journal["candidate_revision"] != journal["prepared_revision"]):
        raise ResearchCustodyError("candidate_import_cancel_invalid", "Canceled import request or revision binding changed.")
    original = store.read_evidence(EvidenceRef.parse(journal["source_ref"]))
    prepared = store.read_evidence(EvidenceRef.parse(journal["prepared_ref"]))
    if stamp_import_candidate_token(original, journal["candidate_token"]) != prepared:
        raise ResearchCustodyError("candidate_import_cancel_invalid", "Canceled import derivative does not bind its original source.")
    if journal["prepared_revision"] is not None:
        root = read_candidate_revision(store, candidate, journal["prepared_revision"])
        envelope = store.read_revision(ResearchRevisionRef.parse(journal["prepared_revision"]))
        if (envelope.parent_revision is not None or root["archive_ref"] != journal["prepared_ref"]
                or root["archive_name"] != request["archive"]
                or root.get("origin", {}).get("kind") != "user_import"
                or root["origin"].get("original_archive_ref") != journal["source_ref"]
                or any(root["origin"].get(key) != value for key, value in destination.items() if key != "archive")):
            raise ResearchCustodyError("candidate_import_cancel_invalid", "Canceled import root does not belong to its exact source.")
    return journal


def _owned_mutation_journals(store, candidate, records, owned_evidence, cancel_import=None):
    from hashlib import sha256
    from .sqx_databank_actions import _read_journal
    rows = []
    for path in sorted((store.root / "databank-actions").glob("*.json")):
        journal = _read_journal(store, path)
        if journal["candidate_entity_id"] != str(candidate):
            continue
        canceled = cancel_import is not None and journal["mutation_id"] == cancel_import["mutation_id"]
        if journal["phase"] != "completed" and not canceled:
            raise ResearchCustodyError("candidate_mutation_pending", "Complete or reconcile this Candidate's native mutation before deleting it.")
        revisions = [] if canceled else [("candidate_revision", candidate), ("membership_revision", _membership_entity(candidate))]
        if journal["action"] == "load" and journal["prepared_revision"] is not None:
            revisions.append(("prepared_revision", candidate))
        for key, entity in revisions:
            record = records.get(journal[key])
            if record is None or record["envelope"].entity_id != entity:
                raise ResearchCustodyError("candidate_mutation_corrupt", "Mutation custody does not belong to this exact Candidate.")
        if journal["action"] == "load" and not canceled:
            root = read_candidate_revision(store, candidate, journal["prepared_revision"])
            child = read_candidate_revision(store, candidate, journal["candidate_revision"])
            if (records[journal["prepared_revision"]]["envelope"].parent_revision is not None
                    or str(records[journal["candidate_revision"]]["envelope"].parent_revision) != journal["prepared_revision"]
                    or root["archive_ref"] != journal["prepared_ref"] or child["archive_ref"] != journal["output_ref"]
                    or root.get("origin", {}).get("kind") != "user_import"
                    or root["origin"].get("original_archive_ref") != journal["source_ref"] or child.get("origin") != root["origin"]):
                raise ResearchCustodyError("candidate_mutation_corrupt", "Import journal does not bind its exact original, prepared root and published output.")
        refs = {EvidenceRef.parse(journal[key]) for key in ("source_ref", "output_ref", "prepared_ref") if journal.get(key) is not None}
        if not refs.issubset(owned_evidence):
            raise ResearchCustodyError("candidate_mutation_corrupt", "Mutation archives are not retained in this Candidate's custody history.")
        raw = path.read_bytes()
        rows.append({"path": path.relative_to(store.root).as_posix(), "sha256": sha256(raw).hexdigest(), "bytes": len(raw),
                     "candidate_entity_id": str(candidate), "candidate_revision": journal["candidate_revision"],
                     "mutation_id": journal["mutation_id"], "action": journal["action"], "source": journal["source"]})
    return rows


def _purge_inventory(store, candidate_entity_id, *, cancel_import=None):
    candidate = _candidate_entity(candidate_entity_id)
    canceled = _cancel_import_journal(store, candidate, cancel_import) if cancel_import is not None else None
    records, by_entity, refs = {}, {}, {}
    for path in sorted((store.base / "revisions").rglob("*.json")):
        _physical_store_path(store, path.relative_to(store.base).as_posix())
        ref = ResearchRevisionRef(ResearchKind(path.parent.parent.name), path.stem)
        envelope = store.read_revision(ref)
        closure, strings = _evidence_closure(store, (envelope.content, *envelope.evidence))
        record = {"envelope": envelope, "closure": closure, "strings": strings}
        records[str(ref)] = record
        by_entity.setdefault(str(envelope.entity_id), []).append(str(ref))
        refs[str(ref)] = str(envelope.entity_id)
    if str(candidate) not in by_entity and canceled is None:
        raise ResearchCustodyError("current_pointer_missing", "Candidate has no retained custody.")
    if canceled is not None and set(by_entity.get(str(candidate), [])) != ({canceled["prepared_revision"]} if canceled["prepared_revision"] else set()):
        raise ResearchCustodyError("candidate_import_cancel_invalid", "Canceled import has additional or unbound Candidate revisions.")
    links = {}
    for entity, revisions in by_entity.items():
        values = set().union(*(records[revision]["strings"] for revision in revisions))
        links[entity] = ({value for value in values if value in by_entity} | {refs[value] for value in values if value in refs}) - {entity}
    candidates = {entity: ({entity} if ResearchEntityId.parse(entity).kind == ResearchKind.CANDIDATE else set()) for entity in by_entity}
    for _ in range(len(by_entity)):
        changed = False
        for entity in by_entity:
            value = candidates[entity] | set().union(*(candidates[other] for other in links[entity]))
            if value != candidates[entity]:
                candidates[entity] = value
                changed = True
        if not changed:
            break
    owned = {str(candidate)}
    membership = str(_membership_entity(candidate))
    if membership in by_entity:
        owned.add(membership)
    owned.update(entity for entity in by_entity if ResearchEntityId.parse(entity).kind in {ResearchKind.HISTORICAL_RESULT, ResearchKind.PROOF, ResearchKind.PROJECT_REVIEW} and candidates[entity] == {str(candidate)})
    # A downstream entity still referenced by an independent entity must remain
    # readable, even when its original Candidate is deliberately deleted.
    retained = set()
    while True:
        shared_entities = {other for entity in by_entity if entity not in owned for other in links[entity] if other in owned and other not in {str(candidate), membership}}
        if not shared_entities:
            break
        owned -= shared_entities
        retained |= shared_entities
    owned_refs = {revision for entity in owned for revision in by_entity.get(entity, [])}
    owned_evidence = set().union(*(records[revision]["closure"] for revision in owned_refs))
    if canceled is not None:
        owned_evidence |= _evidence_closure(store, [EvidenceRef.parse(canceled[key]) for key in ("source_ref", "prepared_ref", "output_ref") if canceled[key] is not None])[0]
    journals = _owned_mutation_journals(store, candidate, records, owned_evidence, cancel_import)
    journal_paths = {store.root / row["path"] for row in journals}
    outside_evidence = set().union(*(record["closure"] for revision, record in records.items() if revision not in owned_refs))
    # Existing product catalogs outside research/v1 may also retain evidence
    # (for example capture manifests). Only read product data-root JSON files.
    for path in store.root.rglob("*.json"):
        if path.is_relative_to(store.base) or path in journal_paths:
            continue
        if path.is_symlink() or (hasattr(path, "is_junction") and path.is_junction()) or not path.resolve().is_relative_to(store.root):
            raise ResearchCustodyError("purge_path_invalid", "A product manifest escapes the data root.")
        try:
            values = set(_strings(json.loads(path.read_bytes())))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise ResearchCustodyError("purge_manifest_invalid", "A product manifest could not be checked for shared evidence.") from exc
        roots = []
        for value in values:
            if value.startswith("tc-evidence:sha256:"):
                roots.append(EvidenceRef.parse(value))
            elif len(value) == 64 and all(c in "0123456789abcdef" for c in value) and store._evidence_path(EvidenceRef(value)).is_file():
                roots.append(EvidenceRef(value))
        outside_evidence |= _evidence_closure(store, roots)[0]
    shared = owned_evidence & outside_evidence
    staging = []
    staging_root = store.root / "databank-imports"
    if staging_root.exists():
        for path in sorted(staging_root.rglob("*.sqx")):
            if path.is_symlink() or path.resolve() != path.absolute() or not path.resolve().is_relative_to(staging_root.resolve()):
                raise ResearchCustodyError("purge_path_invalid", "Import staging contains a linked or escaped file.")
            ref = EvidenceRef.from_bytes(path.read_bytes())
            if ref in owned_evidence - shared:
                if path.parent.name != ref.digest:
                    raise ResearchCustodyError("purge_staging_invalid", "Import staging does not match its source digest.")
                staging.append({"path": path.relative_to(store.root).as_posix(), "sha256": ref.digest, "bytes": path.stat().st_size})
    def artifact(ref):
        return {"ref": str(ref), "bytes": len(store.read_evidence(ref)),
                "owners": [{"entity_id": str(record["envelope"].entity_id), "revision": revision}
                           for revision, record in sorted(records.items()) if ref in record["closure"]]}
    result = {
        "candidate_entity_id": str(candidate),
        "entities": sorted(owned), "shared_entities": sorted(retained),
        "revisions": sorted(owned_refs),
        "artifacts": [artifact(ref) for ref in sorted(owned_evidence - shared, key=str)],
        "shared_artifacts": [artifact(ref) for ref in sorted(shared, key=str)],
        "memberships": read_candidate_memberships(store, str(candidate))["memberships"],
        "staging": staging,
        "mutation_journals": journals,
    }
    if canceled is not None:
        result["cancel_import"] = {**cancel_import, "operation_id": canceled["request"]["operation_id"],
            "request": canceled["request"], "runtime_home": canceled["runtime_home"], "phase": canceled["phase"]}
    return result


def preview_candidate_purge(store, candidate_entity_id, *, cancel_import=None):
    """Read an exact deletion preview without freezing Candidate operations."""
    from hashlib import sha256
    with store._lock(store._lock_path("store", "revision-publication")):
        if cancel_import is None and _purge_path(store, candidate_entity_id).is_file():
            return read_candidate_purge(store, candidate_entity_id)
        preview = _purge_inventory(store, candidate_entity_id, cancel_import=cancel_import)
        return {"schema": "tc.research-candidate-purge.v1", "intent_id": sha256(_canonical(preview)).hexdigest(),
                "state": "preview", "preview": preview}


def prepare_candidate_purge(store, candidate_entity_id, *, expected_preview_sha256, cancel_import=None):
    """Persist the exact preview/confirmation intent, without deleting anything."""
    from hashlib import sha256
    path = _purge_path(store, candidate_entity_id)
    with store._lock(store._lock_path("store", "revision-publication")):
        if path.is_file():
            existing = read_candidate_purge(store, candidate_entity_id)
            if existing["intent_id"] != expected_preview_sha256:
                raise ResearchCustodyError("candidate_purge_conflict", "Confirm the exact Candidate deletion preview.")
            return existing
        if cancel_import is None:
            read_candidate_revision(store, candidate_entity_id, str(store.current(_candidate_entity(candidate_entity_id))))
        preview = _purge_inventory(store, candidate_entity_id, cancel_import=cancel_import)
        intent = {"schema": "tc.research-candidate-purge.v1", "intent_id": sha256(_canonical(preview)).hexdigest(),
                  "state": "prepared", "preview": preview, "deleted_paths": []}
        if intent["intent_id"] != expected_preview_sha256:
            raise ResearchCustodyError("candidate_purge_preview_changed", "Candidate custody changed; inspect its current deletion preview.")
        store._atomic_write(path, _canonical(intent))
        return intent


def read_candidate_purge(store, candidate_entity_id):
    from hashlib import sha256
    try:
        intent = json.loads(_purge_path(store, candidate_entity_id).read_bytes())
    except (FileNotFoundError, UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise ResearchCustodyError("candidate_purge_missing", "No readable Candidate deletion intent exists.") from exc
    if not isinstance(intent, dict) or intent.get("schema") != "tc.research-candidate-purge.v1" or intent.get("state") not in {"prepared", "deleting", "completed"} or intent.get("preview", {}).get("candidate_entity_id") != str(_candidate_entity(candidate_entity_id)) or intent.get("intent_id") != sha256(_canonical(intent["preview"])).hexdigest():
        raise ResearchCustodyError("candidate_purge_corrupt", "Candidate deletion intent is invalid.")
    return intent


def confirmed_purge_memberships(store, intent):
    """Keep confirmed locations/revisions; follow only verified storage reserialization."""
    preview = intent["preview"]
    allowed = list(preview["memberships"])
    history = read_candidate_memberships(store, preview["candidate_entity_id"], history=True)["history"]
    for record in history:
        event = record["event"]
        if record["revision"] in preview["revisions"] or event["action"] != "reserialize":
            continue
        if any(row["candidate_revision"] == event["candidate_revision"] and
               all(row.get(key) == value for key, value in event["source"].items()) for row in allowed):
            # _read verifies the same location, Candidate revision, identity marker,
            # exact old/new evidence and unchanged native strategy/trade members.
            allowed.append({**event["destination"], "candidate_revision": event["candidate_revision"]})
    return allowed


def _retained_evidence_during_purge(store, excluded_revisions, excluded_journals=()):
    from hashlib import sha256
    journal_hashes = {store.root / row["path"]: row["sha256"] for row in excluded_journals}
    roots = []
    for path in (store.base / "revisions").rglob("*.json"):
        _physical_store_path(store, path.relative_to(store.base).as_posix())
        ref = ResearchRevisionRef(ResearchKind(path.parent.parent.name), path.stem)
        if str(ref) not in excluded_revisions:
            envelope = store.read_revision(ref)
            roots.extend((envelope.content, *envelope.evidence))
    for path in store.root.rglob("*.json"):
        if path.is_relative_to(store.base):
            continue
        if path.resolve() != path.absolute() or not path.resolve().is_relative_to(store.root):
            raise ResearchCustodyError("purge_path_invalid", "A product manifest escapes the data root.")
        if path in journal_hashes:
            if sha256(path.read_bytes()).hexdigest() != journal_hashes[path]:
                raise ResearchCustodyError("candidate_purge_file_changed", "A confirmed mutation journal changed during deletion.")
            continue
        try:
            values = set(_strings(json.loads(path.read_bytes())))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise ResearchCustodyError("purge_manifest_invalid", "A product manifest could not be checked for shared evidence.") from exc
        for value in values:
            if value.startswith("tc-evidence:sha256:"):
                roots.append(EvidenceRef.parse(value))
            elif len(value) == 64 and all(c in "0123456789abcdef" for c in value) and store._evidence_path(EvidenceRef(value)).is_file():
                roots.append(EvidenceRef(value))
    return _evidence_closure(store, roots)[0]


def finish_candidate_purge(store, candidate_entity_id, *, intent_id):
    """Finalize an exactly confirmed intent after native membership removal.

    Retries resume only this intent's verified product-owned files. No native or
    original desktop source path is ever accepted by this function.
    """
    from hashlib import sha256
    path = _purge_path(store, candidate_entity_id)
    with store._lock(store._lock_path("store", "revision-publication")):
        intent = read_candidate_purge(store, candidate_entity_id)
        if intent["intent_id"] != intent_id:
            raise ResearchCustodyError("candidate_purge_conflict", "Confirm the exact Candidate deletion preview.")
        if intent["state"] == "completed":
            return intent
        if intent["state"] == "prepared":
            preview = intent["preview"]
            descriptor = preview.get("cancel_import")
            current = _purge_inventory(store, candidate_entity_id, cancel_import={key: descriptor[key] for key in ("mutation_id", "journal_sha256", "native_disposition")} if descriptor is not None else None)
            if current["memberships"]:
                raise ResearchCustodyError("candidate_purge_memberships_remain", "Native memberships must be removed before Candidate custody is deleted.")
            non_membership = lambda rows: {value for value in rows if ResearchRevisionRef.parse(value).kind != ResearchKind.CANDIDATE_MEMBERSHIP}
            if non_membership(current["revisions"]) != non_membership(preview["revisions"]) or current["entities"] != preview["entities"]:
                raise ResearchCustodyError("candidate_purge_preview_changed", "Candidate downstream custody changed after deletion was prepared.")
            previous_journals = {row["path"]: row for row in preview.get("mutation_journals", [])}
            current_journals = {row["path"]: row for row in current["mutation_journals"]}
            allowed_memberships = confirmed_purge_memberships(store, intent)
            if any(current_journals.get(key) != row for key, row in previous_journals.items()) or any(
                row["action"] != "remove" or not any(all(member.get(key) == value for key, value in row["source"].items()) for member in allowed_memberships)
                for key, row in current_journals.items() if key not in previous_journals
            ):
                raise ResearchCustodyError("candidate_purge_preview_changed", "Candidate mutation history changed outside the confirmed membership removals.")
            # Rechecking all retained manifests can retain MORE shared bytes. It
            # must never delete bytes previously disclosed as shared.
            preserve = {row["ref"] for row in preview["shared_artifacts"]} | {row["ref"] for row in current["shared_artifacts"]}
            artifacts = [row for row in current["artifacts"] if row["ref"] not in preserve]
            files = []
            deleted_at = datetime.now(timezone.utc).isoformat()
            for value in current["entities"]:
                entity = ResearchEntityId.parse(value)
                pointer = store._current_path(entity)
                if pointer.exists():
                    files.append({"path": pointer.relative_to(store.base).as_posix(), "sha256": sha256(pointer.read_bytes()).hexdigest()})
            for value in current["revisions"]:
                ref = ResearchRevisionRef.parse(value)
                files.append({"path": store._revision_path(ref).relative_to(store.base).as_posix(), "sha256": ref.digest})
            for row in artifacts:
                ref = EvidenceRef.parse(row["ref"])
                files.append({"path": store._evidence_path(ref).relative_to(store.base).as_posix(), "sha256": ref.digest})
            allowed_staging = {row["path"] for row in current["staging"]}
            intent.update(state="deleting", files=files, staging=[row for row in preview["staging"] if row["path"] in allowed_staging],
                          entities=current["entities"], revisions=current["revisions"], deleted_at_utc=deleted_at,
                          retained_shared_artifacts=sorted(preserve), deleted_paths=[], reclaimed_files=[],
                          mutation_journals=current["mutation_journals"],
                          reclaimed_bytes=0, reclaimed_custody_bytes=0, reclaimed_staging_bytes=0, reclaimed_mutation_journal_bytes=0,
                          reclamation_uncertain_paths=[], reclaimed_byte_measure="file_content_bytes")
            store._atomic_write(path, _canonical(intent))
        def record_reclamation(target, key, scope):
            recorded = any(item["path"] == key and item["scope"] == scope for item in intent["reclaimed_files"])
            if target.exists():
                size = target.stat().st_size
                target.unlink()
                # This journal follows the unlink. A crash in between is not
                # silently credited on retry: the absent path remains uncertain.
                intent["reclaimed_files"].append({"scope": scope, "path": key, "bytes": size})
                intent["reclaimed_bytes"] += size
                intent[f"reclaimed_{scope}_bytes"] += size
            elif not recorded:
                missing = f"{scope}:{key}"
                if missing not in intent["reclamation_uncertain_paths"]:
                    intent["reclamation_uncertain_paths"].append(missing)
            store._atomic_write(path, _canonical(intent))
        # Publication refuses deliberately deleted entities. Tombstones are
        # durable before removing any bytes, allowing interrupted deletion retry.
        for value in intent["entities"]:
            entity = ResearchEntityId.parse(value)
            tombstone = {"schema": "tc.research-deletion.v1", "entity_id": value, "intent_id": intent_id, "deleted_at_utc": intent["deleted_at_utc"]}
            store._atomic_write(store.base / "deletions" / entity.kind.value / f"{entity.value}.json", _canonical(tombstone))
        for value in intent["revisions"]:
            ref = ResearchRevisionRef.parse(value)
            store._atomic_write(store.base / "deleted-revisions" / ref.kind.value / f"{ref.digest}.json", _canonical({"schema": "tc.research-deleted-revision.v1", "revision": value, "intent_id": intent_id}))
        # Recheck on EVERY resume. Another retained record may have acquired a
        # reference while an interrupted purge was waiting to be retried.
        retained = _retained_evidence_during_purge(store, set(intent["revisions"]), intent.get("mutation_journals", []))
        planned_digests = {row["sha256"] for row in intent["files"] if row["path"].startswith("evidence/sha256/")}
        intent["retained_shared_artifacts"] = sorted(set(intent["retained_shared_artifacts"]) | {str(ref) for ref in retained if ref.digest in planned_digests})
        # Completed action receipts belong to this Candidate. Remove them before
        # their evidence so interrupted retries cannot leave dangling manifests.
        for row in intent.get("mutation_journals", []):
            from .sqx_databank_actions import _physical
            target = store.root / "databank-actions" / f"{row['mutation_id']}.json"
            if target.relative_to(store.root).as_posix() != row["path"]:
                raise ResearchCustodyError("purge_path_invalid", "Mutation journal path is not product-owned.")
            _physical(store.root, target)
            if target.exists() and sha256(target.read_bytes()).hexdigest() != row["sha256"]:
                raise ResearchCustodyError("candidate_purge_file_changed", "A confirmed mutation journal changed during deletion.")
            record_reclamation(target, row["path"], "mutation_journal")
        for row in intent["files"]:
            if row["path"].startswith("evidence/sha256/") and EvidenceRef(row["sha256"]) in retained:
                continue
            target = _physical_store_path(store, row["path"])
            if target.exists():
                if not target.is_file() or sha256(target.read_bytes()).hexdigest() != row["sha256"]:
                    raise ResearchCustodyError("candidate_purge_file_changed", "A product custody file changed during deletion.")
            record_reclamation(target, row["path"], "custody")
            if row["path"] not in intent["deleted_paths"]:
                intent["deleted_paths"].append(row["path"])
                store._atomic_write(path, _canonical(intent))
        for row in intent["staging"]:
            if EvidenceRef(row["sha256"]) in retained:
                continue
            from pathlib import PurePosixPath
            relative = PurePosixPath(row["path"])
            if len(relative.parts) != 3 or relative.parts[0] != "databank-imports" or relative.parts[1] != row["sha256"]:
                raise ResearchCustodyError("purge_staging_invalid", "Deletion staging path is not a product-owned import copy.")
            target = store.root.joinpath(*relative.parts)
            if target.resolve() != target.absolute() or not target.resolve().is_relative_to(store.root):
                raise ResearchCustodyError("purge_path_invalid", "Import staging escaped the product data root.")
            if target.exists():
                if sha256(target.read_bytes()).hexdigest() != row["sha256"]:
                    raise ResearchCustodyError("candidate_purge_file_changed", "Import staging changed during deletion.")
            record_reclamation(target, row["path"], "staging")
            try:
                target.parent.rmdir()
            except OSError:
                pass
        intent["state"] = "completed"
        store._atomic_write(path, _canonical(intent))
        return intent
