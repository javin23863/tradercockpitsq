"""Retain exact saved Custom Project graphs and selected-bank input inventories.

This review is not an execution approval. Capture coverage, external resources and
the running producer's in-memory bank still need binding before a tracked launch.
"""

from datetime import datetime, timezone
from hashlib import sha256
from io import BytesIO
import json
from uuid import UUID, uuid5, NAMESPACE_URL
from zipfile import ZipFile

from .research_candidates import _canonical, _digest, _native_name
from .research_candidate_memberships import candidate_admission_batch, assert_candidate_membership_action
from .research_custody import EvidenceRef, ResearchCustodyError, ResearchEntityId, ResearchKind
from .sqx_custom_project import SQX_BUILD, _read_topology, _resolved_project_archive, _databank_name, _parse_xml, _task_config_bindings
from .sqx_custom_project_launch import _preflight_launcher, SqxCustomProjectLaunchError
from .sqx_databank_actions import _physical, SqxDatabankActionError
from .sqx_custom_project import SqxCustomProjectTopologyError

API_PATH = "/api/sqx-project-review"
SCHEMA = "tc.research-project-review.v1"


def _snapshot(store, sqx_home, trusted_launcher_sha256, project, databank, inventory):
    home, launcher, launcher_digest = _preflight_launcher(sqx_home, trusted_launcher_sha256)
    _physical(home, home / "sqcli.exe")
    _physical(home, home / "user" / "projects" / _native_name(project) / "project.cfx")
    graph_path = _resolved_project_archive(home, project)
    if graph_path.stat().st_nlink != 1 or launcher.stat().st_nlink != 1:
        raise ResearchCustodyError("project_review_input_invalid", "Review refuses linked graph or launcher files.")
    graph = graph_path.read_bytes()
    _, tasks = _read_topology(graph, omit_building_block_rows=True)
    bank = home / "user" / "projects" / project / "databanks" / _databank_name(databank)
    _physical(home, bank)
    if not bank.is_dir():
        raise ResearchCustodyError("project_review_bank_missing", "Select an existing saved databank.")
    inputs = []
    for path in sorted(bank.glob("*.sqx"), key=lambda item: item.name):
        _physical(home, path)
        if not path.is_file() or path.stat().st_nlink != 1:
            raise ResearchCustodyError("project_review_input_invalid", "Review refuses linked or non-file inputs.")
        raw = path.read_bytes()
        digest = sha256(raw).hexdigest()
        member = inventory.rows.get((project, databank, path.name))
        exact = member is not None and member["archive_sha256"] == digest
        if exact:
            assert_candidate_membership_action(store, member["candidate_entity_id"], action="review")
        inputs.append({"archive": path.name, "archive_sha256": digest,
            "bytes": len(raw),
            "candidate_entity_id": member["candidate_entity_id"] if exact else None,
            "candidate_revision": member["candidate_revision"] if exact else None,
            "membership_revision": member["membership_revision"] if exact else None,
            "archive_ref": member["archive_ref"] if exact else None,
            "binding": "exact" if exact else "changed" if member else "unadmitted"})
    with ZipFile(BytesIO(graph)) as archive:
        members = [{"name": entry.filename, "sha256": sha256(archive.read(entry)).hexdigest()}
                   for entry in archive.infolist() if not entry.is_dir()]
        order = list(_task_config_bindings(_parse_xml(archive.read("config.xml"), "config.xml")))
    if set(order) != {task.entry_name for task in tasks}:
        raise ResearchCustodyError("project_review_graph_invalid", "Saved graph has missing or unbound task entries.")
    tasks = sorted(tasks, key=lambda task: order.index(task.entry_name))
    snapshot = {"project": project, "databank": databank,
        "scope": "saved_graph_and_selected_bank", "source_build": SQX_BUILD,
        "launcher_sha256": launcher_digest, "graph_ref": str(EvidenceRef.from_bytes(graph)),
        "graph_sha256": sha256(graph).hexdigest(), "members": members,
        "tasks": [{"index": task.native_task_index, "entry": task.entry_name,
                   "kind": task.kind, "title": task.title or task.name or task.kind,
                   "active": task.active} for task in tasks], "inputs": inputs,
        "launch_authorized": False,
        "gaps": ["Capture checkpoints and task-visit coverage are not yet verified for this graph.",
                 "External data/resources and the running engine's in-memory inputs are not bound."]}
    if any(row["binding"] != "exact" for row in inputs):
        snapshot["gaps"].append("Some saved inputs have no matching admitted Candidate revision.")
    return snapshot, graph


def _record(store, entity):
    revision = store.current(entity)
    envelope = store.read_revision(revision)
    payload = json.loads(store.read_revision_content(revision))
    if (envelope.entity_id != entity or payload.get("schema") != SCHEMA
            or payload.get("review_sha256") != sha256(_canonical(payload["snapshot"])).hexdigest()
            or payload["snapshot"]["launch_authorized"] is not False):
        raise ResearchCustodyError("project_review_corrupt", "Saved project review failed custody verification.")
    expected = {EvidenceRef.parse(payload["snapshot"]["graph_ref"])}
    expected.update(EvidenceRef.parse(row["archive_ref"]) for row in payload["snapshot"]["inputs"] if row["archive_ref"])
    if set(envelope.evidence) != expected:
        raise ResearchCustodyError("project_review_corrupt", "Saved review evidence does not match its inventory.")
    for ref in expected:
        store.read_evidence(ref)
    return {**payload, "entity_id": str(entity), "revision": str(revision)}


def project_review_response(store, sqx_home, payload, *, trusted_launcher_sha256=None):
    """One loopback-only API: preview, retain exact preview, or reopen saved reviews."""
    if store is None:
        return 503, {"detail": "Research custody is not configured.", "reason_code": "research_custody_unavailable"}
    action = payload.get("action")
    fields = {"action", "project", "databank"} | ({"expected_review_sha256"} if action == "retain" else set())
    if action not in {"preview", "retain", "list"} or set(payload) != fields:
        return 400, {"detail": "Select preview, retain or list with an exact project and databank.", "reason_code": "project_review_fields_invalid"}
    try:
        project, databank = _native_name(payload["project"]), _native_name(payload["databank"])
        if action == "list":
            rows = []
            for path in (store.base / "current" / ResearchKind.PROJECT_REVIEW.value).glob("*.json"):
                record = _record(store, ResearchEntityId(ResearchKind.PROJECT_REVIEW, UUID(path.stem)))
                if record["snapshot"]["project"] == project and record["snapshot"]["databank"] == databank:
                    rows.append(record)
            return 200, {"schema": SCHEMA, "project": project, "databank": databank,
                         "reviews": sorted(rows, key=lambda row: row["reviewed_at_utc"], reverse=True)}
        with candidate_admission_batch(store) as inventory:
            snapshot, graph = _snapshot(store, sqx_home, trusted_launcher_sha256, project, databank, inventory)
            digest = sha256(_canonical(snapshot)).hexdigest()
            if action == "preview":
                return 200, {"schema": SCHEMA, "review_sha256": digest, "snapshot": snapshot}
            if _digest(payload["expected_review_sha256"], code="project_review_digest_invalid") != digest:
                raise ResearchCustodyError("project_review_changed", "Graph, runtime or input custody changed. Refresh the review before saving.")
            entity = ResearchEntityId(ResearchKind.PROJECT_REVIEW, uuid5(NAMESPACE_URL, f"{SCHEMA}:{digest}"))
            if store.deletion_record(entity) is not None:
                raise ResearchCustodyError("entity_deleted", "This review was deliberately deleted with its Candidate custody.")
            if store._read_current(entity) is None:
                graph_ref = store.put_evidence(graph)
                evidence = {graph_ref} | {EvidenceRef.parse(row["archive_ref"]) for row in snapshot["inputs"] if row["archive_ref"]}
                record = {"schema": SCHEMA, "review_sha256": digest, "snapshot": snapshot,
                          "reviewed_at_utc": datetime.now(timezone.utc).isoformat()}
                revision = store.create_revision(entity, _canonical(record), evidence=tuple(evidence))
                store.compare_and_set_current(entity, expected_revision=None, target_revision=revision.revision)
            return 200, _record(store, entity)
    except (ResearchCustodyError, SqxCustomProjectTopologyError, SqxCustomProjectLaunchError, SqxDatabankActionError) as exc:
        return 409, {"reason_code": exc.code, "detail": exc.detail}
    except (OSError, ValueError, KeyError, TypeError):
        return 409, {"reason_code": "project_review_unreadable", "detail": "Project review is unreadable; refresh the saved project and custody."}
