"""Bounded desktop archive operations through the installed SQX databank owner."""

from hashlib import sha256
from io import BytesIO
import json
from pathlib import Path, PurePosixPath
import re
import zlib
from threading import RLock
from time import sleep
from xml.etree import ElementTree
from xml.parsers import expat
from zipfile import BadZipFile, ZipFile

from .sqx_custom_project import _verified_home, _resolved_project_archive
from .sqx_custom_project_strategy import _resolved_strategy_archive
from .sqx_engine_progress import _poll_custom_project_stats, _poll_engine_channel, custom_project_stat_fields, setup_app_for_project
from .sqx_native_web import SqxNativeWebError, sqx_local_json
from .sqx_outputs import inspect_sqx_output_bytes
from .sqx_results_overview import ensure_databank_result
from .research_custody import EvidenceRef

MAX_DATABANK_ARCHIVE_BYTES = 16 * 1024 * 1024
_MAX_EXPANDED_BYTES = 128 * 1024 * 1024
_DIGEST = re.compile(r"^[0-9a-f]{64}$")
_OPERATION_ID = re.compile(r"^[0-9a-f]{32}$")
# ponytail: serialize this desktop's databank writes; SQX remains the native owner.
_ACTIONS_LOCK = RLock()


class SqxDatabankActionError(RuntimeError):
    def __init__(self, code, detail):
        super().__init__(detail)
        self.code, self.detail = code, detail


def _refuse(code, detail):
    raise SqxDatabankActionError(code, detail)


def _name(value):
    if (not isinstance(value, str) or not 1 <= len(value) <= 160 or value != value.strip()
            or value.endswith((".", " ")) or any(ord(c) < 32 or c in '<>:"/\\|?*,;' for c in value)
            or value in {".", "..", "all"}
            or value.split(".")[0].upper() in {"CON", "PRN", "AUX", "NUL", *(f"COM{i}" for i in range(10)), *(f"LPT{i}" for i in range(10))}):
        _refuse("databank_name_invalid", "Use a single ordinary name without paths, reserved names, or native list separators.")
    return value


def _request(payload, fields):
    if not isinstance(payload, dict) or set(payload) != set(fields):
        _refuse("databank_fields_invalid", "Databank action fields do not match the selected operation.")
    for key in ("project", "databank", "archive", "new_name"):
        if key in payload:
            _name(payload[key])
    if "archive" in payload and not payload["archive"].lower().endswith(".sqx"):
        _refuse("databank_name_invalid", "Select a complete .sqx strategy archive.")
    for key in ("archive_sha256", "source_sha256"):
        if key in payload and (not isinstance(payload[key], str) or not _DIGEST.fullmatch(payload[key])):
            _refuse("databank_digest_invalid", "An exact SHA-256 is required.")
    if "operation_id" in payload and (not isinstance(payload["operation_id"], str) or not _OPERATION_ID.fullmatch(payload["operation_id"])):
        _refuse("databank_operation_invalid", "An exact 32-character lowercase hexadecimal operation ID is required.")


def _reserve_operation(store, home, action, payload):
    """Bind one user intent before native effects; retries keep its exact request."""
    identity = {"action": action, "request": payload, "runtime_home": str(home)}
    reservation = {"schema": "tc.sqx-databank-operation.v1", "operation_id": payload["operation_id"],
        "request_sha256": sha256(json.dumps(identity, sort_keys=True, separators=(",", ":")).encode()).hexdigest()}
    path = _physical(Path(store.root).resolve(), Path(store.root) / "databank-actions" / "requests" / f'{payload["operation_id"]}.json')
    if path.exists():
        try:
            with path.open("rb") as handle:
                previous = json.loads(handle.read(1025))
        except (OSError, ValueError) as exc:
            raise SqxDatabankActionError("databank_operation_corrupt", "The retained operation identity could not be verified.") from exc
        completed_candidate = previous.get("candidate_entity_id") if isinstance(previous, dict) else None
        if completed_candidate is not None:
            from .research_custody import ResearchEntityId, ResearchKind
            entity = ResearchEntityId.parse(completed_candidate)
            if action != "load" or entity.kind != ResearchKind.CANDIDATE:
                _refuse("databank_operation_corrupt", "The retained import identity is invalid.")
            if store.deletion_record(entity) is not None:
                _refuse("entity_deleted", "This import's Candidate was deliberately deleted. Start a new import operation.")
            from .research_candidate_memberships import _purge_path
            if _purge_path(store, completed_candidate).is_file():
                _refuse("candidate_purge_pending", "This retained import is being discarded. Finish that exact deletion before starting a new import.")
        if not isinstance(previous, dict) or {key: value for key, value in previous.items() if key != "candidate_entity_id"} != reservation:
            _refuse("databank_operation_conflict", "This operation ID already belongs to a different exact request. Retry the original request or start a new action.")
    else:
        # Retain only identity metadata here; Candidate journals own archive refs.
        store._atomic_write(path, json.dumps(reservation, sort_keys=True, separators=(",", ":")).encode())


def inspect_databank_upload(raw, archive):
    """Bound every ZIP member before native code sees an uploaded archive."""
    if not isinstance(raw, bytes) or not 0 < len(raw) <= MAX_DATABANK_ARCHIVE_BYTES:
        _refuse("databank_archive_size_invalid", "Select a nonempty .sqx archive up to 16 MiB.")
    try:
        with ZipFile(BytesIO(raw)) as zipped:
            entries = zipped.infolist()
            if not entries or len(entries) > 512 or sum(e.file_size for e in entries) > _MAX_EXPANDED_BYTES:
                _refuse("databank_archive_size_invalid", "The expanded strategy archive exceeds the bounded import size.")
            names = set()
            for entry in entries:
                name = entry.filename
                parts = PurePosixPath(name).parts
                if (name.casefold() in names or not parts or PurePosixPath(name).as_posix() != name.rstrip("/")
                        or name.startswith("/") or "\\" in name or ":" in parts[0]
                        or any(p in {".", ".."} for p in parts) or any(ord(c) < 32 for c in name)
                        or (entry.external_attr >> 16) & 0o170000 == 0o120000
                        or entry.flag_bits & 1 or entry.file_size > 32 * 1024 * 1024):
                    _refuse("databank_archive_invalid", "The strategy ZIP contains unsafe, duplicate, encrypted, or oversized members.")
                names.add(name.casefold())
                # Verify CRC and declared sizes for all native input, without extracting files.
                content = zipped.read(entry)
                if len(content) != entry.file_size:
                    _refuse("databank_archive_invalid", "The strategy ZIP member size changed.")
                if name.lower().endswith(".xml"):
                    parser = expat.ParserCreate()
                    def reject_doctype(*args):
                        _refuse("databank_archive_invalid", "Uploaded XML must not declare document types or entities.")
                    parser.StartDoctypeDeclHandler = reject_doctype
                    try:
                        parser.Parse(content, True)
                    except (expat.ExpatError, LookupError, ValueError) as exc:
                        raise SqxDatabankActionError("databank_archive_invalid", "The strategy contains unreadable XML.") from exc
    except SqxDatabankActionError:
        raise
    except (BadZipFile, RuntimeError, NotImplementedError, OSError, EOFError, zlib.error) as exc:
        raise SqxDatabankActionError("databank_archive_invalid", "The strategy ZIP could not be verified.") from exc
    # Older genuine producer archives may be accepted/migrated by this native runtime.
    return inspect_sqx_output_bytes(raw, archive_name=archive, require_runtime_build=False)


def _physical(home, path):
    current = home
    try:
        for part in path.relative_to(home).parts:
            current = current / part
            if current.is_symlink() or current.is_junction():
                raise ValueError("link")
        if path.resolve() != path.absolute():
            raise ValueError("resolved identity")
        path.resolve().relative_to(home)
    except (ValueError, OSError, RuntimeError) as exc:
        raise SqxDatabankActionError("databank_path_escape", "The selected native path contains a link or escapes its runtime.") from exc
    return path


def _context(sqx_home, payload, *, worker_is_active=None, native=True):
    home = _verified_home(sqx_home)
    project = payload["project"]
    project_path = _physical(home, home / "user" / "projects" / project / "project.cfx")
    if not project_path.is_file():
        _refuse("databank_project_missing", "Select an existing native project.")
    if _resolved_project_archive(home, project) != project_path:
        _refuse("databank_path_escape", "The selected native project changed.")
    root = _physical(home, project_path.parent / "databanks")
    bank = _physical(home, root / payload["databank"])
    if callable(worker_is_active) and worker_is_active(f"sqx-project-start:{project}"):
        _refuse("databank_project_running", "Stop the selected native project before changing its databank.")
    if native:
        try:
            # Explicit bank actions select the native UI feed. This does not
            # execute a task; acknowledgment alone is never accepted as idle.
            _call(home, "/main/appSwitched", {"productCode": setup_app_for_project(project)}, method="GET")
            row = _poll_custom_project_stats(home).get(project)
            if not isinstance(row, dict):
                engine, _charts = _poll_engine_channel(home, project, timeout=3.0)
                if isinstance(engine, dict) and engine.get("projectName") == project:
                    row = custom_project_stat_fields(engine)
        except (OSError, SqxNativeWebError) as exc:
            raise SqxDatabankActionError("databank_status_unavailable", "Native project idle status could not be verified.") from exc
        if not isinstance(row, dict) or row.get("running_status") not in {"beforeStart", "finished", "stopped", "error"}:
            _refuse("databank_status_unavailable", "Native project idle status is unavailable or the project is active.")
        if row.get("running") is not False:
            _refuse("databank_project_running", "Stop the selected native project before changing its databank.")
    return home, bank


def _call(home, path, fields, *, method="POST"):
    result = sqx_local_json(home, path, method=method, fields=fields)
    if result.get("error") or result.get("canceled") or result.get("success") is False:
        _refuse("databank_native_refused", "StrategyQuant X refused the databank operation.")
    return result


def _banks(home, project):
    result = _call(home, "/project/databankList", {"projectName": project}, method="GET")
    rows = result.get("databanks")
    if not isinstance(rows, list) or any(not isinstance(r, dict) or not isinstance(r.get("name"), str) for r in rows):
        _refuse("databank_readback_invalid", "Native databank names could not be read.")
    return [row["name"] for row in rows]


def _present(home, project, bank, name):
    row = sqx_local_json(home, "/project/getDataItems", fields={"projectName": project, "databankName": bank, "reportName": name})
    if row.get("strDoesntExist") is True:
        return False
    if row.get("error") or not (row.get("success") or row.get("dataItems")):
        _refuse("databank_readback_invalid", "Native strategy identity could not be read.")
    return True


def _snapshot(home, payload):
    path = _resolved_strategy_archive(home, payload["project"], payload["databank"], payload["archive"])
    _physical(home, path)
    with path.open("rb") as handle:
        raw = handle.read(MAX_DATABANK_ARCHIVE_BYTES + 1)
    metadata = inspect_databank_upload(raw, path.name)
    if metadata["archive_sha256"] != payload["archive_sha256"]:
        _refuse("databank_archive_stale", "The selected strategy changed. Refresh the databank and select it again.")
    return raw


def _wait(check, *, sleeper):
    for _ in range(20):
        result = check()
        if result:
            return result
        sleeper(0.2)
    _refuse("databank_persistence_unverified", "Native databank readback did not confirm persisted completion. Refresh before retrying.")


def _record(action, payload, *, archive=None, raw=None):
    return {"schema": "tc.sqx-databank-action.v1", "action": action, "project": payload["project"],
            "databank": payload["databank"], "archive": archive,
            "archive_sha256": sha256(raw).hexdigest() if raw is not None else None,
            "source_sha256": payload.get("source_sha256"), "producer": "sqx_local_web", "persisted": True,
            **({"operation_id": payload["operation_id"]} if "operation_id" in payload else {})}


def _admit(store, project, bank, archive, raw, *, original=None, admission_inventory=None):
    from .research_candidates import admit_databank_candidate
    return admit_databank_candidate(store, project=project, databank=bank, archive=archive,
        archive_bytes=raw, origin_kind="user_import" if original is not None else "native_databank", original_bytes=original,
        admission_inventory=admission_inventory)


def _membership(store, candidate, action, source, *, destination=None, raw=None):
    from .research_candidate_memberships import record_databank_membership_operation
    result = record_databank_membership_operation(store, action=action,
        candidate_entity_id=candidate["entity_id"], candidate_revision=candidate["revision"],
        source=source, destination=destination, archive_bytes=raw,
        expected_membership_revision=candidate["membership_revision"])
    candidate["membership_revision"] = result["revision"]
    return result


def _candidate_preflight(store, candidate, action, source, *, allowed_mutations=(), pending=None):
    if candidate is not None:
        from .research_candidate_memberships import assert_candidate_membership_action
        assert_candidate_membership_action(store, candidate["entity_id"], action=action, source=source)
        for journal in _pending_journals(store) if pending is None else pending:
            if journal["candidate_entity_id"] == candidate["entity_id"] and journal["mutation_id"] not in allowed_mutations:
                _refuse("databank_mutation_pending", "This Candidate has an incomplete native mutation. Resume or reconcile that exact action first.")


def _pending_journals(store):
    rows = []
    for path in (Path(store.root) / "databank-actions").glob("*.json"):
        value = _read_journal(store, path)
        if value.get("phase") != "completed":
            rows.append(value)
    return rows


def read_import_recovery(sqx_home, project, store):
    """Recover user intents from existing custody; never launch or delete on read."""
    from .research_candidate_memberships import read_candidate_purge
    home = _verified_home(sqx_home)
    root = Path(store.root).resolve()
    _physical(root, root / "databank-actions")
    rows = {}
    fields = {"project", "databank", "archive", "source_sha256", "operation_id"}
    for journal in _pending_journals(store):
        if journal["action"] != "load" or journal["runtime_home"] != str(home):
            continue
        request = journal["request"]
        _request(request, fields)
        if project is None or request["project"] == project:
            rows[request["operation_id"]] = {"action": "load", "target": request}
    # A partially completed purge may already have removed the import journal.
    purges = _physical(root, store.base / "candidate-purges")
    for path in purges.glob("*.json"):
        _physical(root, path)
        if path.stat().st_size > 8 * 1024 * 1024:
            _refuse("databank_recovery_unavailable", "A retained deletion record exceeds the recovery read limit.")
        intent = read_candidate_purge(store, f"tc-research:candidate:v1:{path.stem}")
        binding = intent["preview"].get("cancel_import")
        if binding is None:
            continue
        request = binding["request"]
        _request(request, fields)
        identity = {"action": "load", "request": request, "runtime_home": str(home)}
        if binding["mutation_id"] != sha256(json.dumps(identity, sort_keys=True, separators=(",", ":")).encode()).hexdigest():
            continue
        if project is not None and request["project"] != project:
            continue
        if intent["state"] == "completed":
            rows.pop(request["operation_id"], None)
        else:
            rows[request["operation_id"]] = {"action": "load", "target": request, "discard_preview_sha256": intent["intent_id"]}
    return {"status": "ready", "operations": list(rows.values())}


def _read_journal(store, path):
    from .research_custody import ResearchEntityId, ResearchRevisionRef, ResearchKind
    try:
        with _physical(Path(store.root).resolve(), path).open("rb") as handle:
            raw = handle.read(65537)
        value = json.loads(raw)
        expected = {"schema", "action", "request", "runtime_home", "mutation_id", "candidate_entity_id",
            "candidate_revision", "membership_revision", "source", "destination", "source_ref", "phase",
            "output_ref", "output_sha256", "receipt"}
        if isinstance(value, dict) and value.get("action") == "load":
            expected |= {"prepared_ref", "candidate_token", "prepared_revision"}
        if len(raw) > 65536 or not isinstance(value, dict) or set(value) != expected or value["schema"] != "tc.sqx-databank-mutation.v1":
            raise ValueError("shape")
        if value["action"] not in {"load", "rename", "copy", "move", "remove"} or value["phase"] not in {"prepared", "rename_submitted", "renamed", "load_submitted", "loaded", "save_submitted", "saved", "copied", "remove_submitted", "source_removed", "completed"}:
            raise ValueError("state")
        identity = {key: value[key] for key in ("action", "request", "runtime_home")}
        if not isinstance(value["runtime_home"], str) or value["mutation_id"] != path.stem or sha256(json.dumps(identity, sort_keys=True, separators=(",", ":")).encode()).hexdigest() != path.stem:
            raise ValueError("identity")
        _request(value["source"], {"project", "databank", "archive", "archive_sha256"})
        if EvidenceRef.parse(value["source_ref"]).digest != value["source"]["archive_sha256"]:
            raise ValueError("source evidence")
        if ResearchEntityId.parse(value["candidate_entity_id"]).kind != ResearchKind.CANDIDATE:
            raise ValueError("custody identity")
        for key, kind in (("candidate_revision", ResearchKind.CANDIDATE), ("membership_revision", ResearchKind.CANDIDATE_MEMBERSHIP)):
            if value[key] is None and value["action"] == "load" and value["phase"] != "completed":
                continue
            if ResearchRevisionRef.parse(value[key]).kind != kind:
                raise ValueError("custody revision")
        if value["action"] == "load":
            EvidenceRef.parse(value["prepared_ref"])
            if not isinstance(value["candidate_token"], str) or not _DIGEST.fullmatch(value["candidate_token"]):
                raise ValueError("import token")
            if value["prepared_revision"] is not None and ResearchRevisionRef.parse(value["prepared_revision"]).kind != ResearchKind.CANDIDATE:
                raise ValueError("prepared revision")
            if value["phase"] != "prepared" and value["prepared_revision"] is None:
                raise ValueError("missing prepared revision")
        if value["action"] == "remove":
            if value["destination"] is not None or value["output_ref"] is not None or value["output_sha256"] is not None:
                raise ValueError("remove output")
        else:
            _request(value["destination"], {"project", "databank", "archive"})
            if (value["output_ref"] is None) != (value["output_sha256"] is None):
                raise ValueError("output evidence")
            if value["output_ref"] is not None and EvidenceRef.parse(value["output_ref"]).digest != value["output_sha256"]:
                raise ValueError("output digest")
            if value["phase"] == "completed" and value["output_ref"] is None:
                raise ValueError("missing output")
        return value
    except (OSError, ValueError, TypeError, KeyError, RuntimeError) as exc:
        raise SqxDatabankActionError("databank_mutation_corrupt", "A retained native mutation journal could not be verified.") from exc


def _prior_upload(store, home, payload):
    from .research_candidate_memberships import list_databank_memberships
    from .research_candidates import read_candidate_revision
    for member in list_databank_memberships(store, project=payload["project"], databank=payload["databank"])["memberships"]:
        if member["archive"] != payload["archive"]:
            continue
        candidate = read_candidate_revision(store, member["candidate_entity_id"], member["candidate_revision"])
        origin = candidate.get("origin") or {}
        if origin.get("original_archive_sha256") != payload["source_sha256"]:
            _refuse("databank_collision", "That location belongs to a different original strategy upload.")
        raw = _snapshot(home, {**payload, "archive_sha256": member["archive_sha256"]})
        return {**_record("load", payload, archive=payload["archive"], raw=raw), "reused": True,
                "candidate_entity_id": candidate["entity_id"], "candidate_revision": candidate["revision"],
                "membership_revision": member["membership_revision"]}
    return None


def _scoped_save(home, project, bank, archive, *, sleeper):
    target = _physical(home, bank / archive)
    if target.exists():
        _refuse("databank_collision", "Native save destination already exists; overwrite is not allowed.")
    # Installed ProjectServlet.onSaveReports saves ONLY the selected ResultsGroup.
    # Whole-bank synchronization can delete unrelated disk-only strategies.
    _call(home, "/project/saveReports", {"projectName": project, "databankName": bank.name,
          "strategies": archive[:-4], "folder": str(bank), "fileName": archive[:-4],
          "extension[name]": "sqx", "prefix": "", "sufix": "", "handleMagicNumbers": "false"})
    def persisted():
        _physical(home, target)
        if not target.is_file():
            return None
        with target.open("rb") as handle:
            output = handle.read(MAX_DATABANK_ARCHIVE_BYTES + 1)
        inspect_databank_upload(output, archive)
        return output
    return _wait(persisted, sleeper=sleeper)


def _prepare_bank_storage(home, bank):
    # Native create persists the bank registration but leaves empty storage absent.
    # Call only after native registration and idle checks for an explicit write.
    _physical(home, bank).mkdir(parents=True, exist_ok=True)
    _physical(home, bank)


def _retain_upload(store, archive, raw):
    store.put_evidence(raw)
    root = Path(store.root).resolve()
    staging = _physical(root, root / "databank-imports" / sha256(raw).hexdigest())
    staging.mkdir(parents=True, exist_ok=True)
    staged = _physical(root, staging / archive)
    if staged.exists():
        if staged.read_bytes() != raw:
            _refuse("databank_archive_stale", "The retained upload was modified.")
    else:
        with staged.open("xb") as handle:
            handle.write(raw)
    return staged


def _load_into_bank(home, project, bank, archive, raw, store, *, sleeper):
    staged = _retain_upload(store, archive, raw)
    _call(home, "/project/loadFilesToDatabank", {"projectName": project, "databankName": bank.name,
          "clear": "false", "filePaths[]": str(staged)})
    _wait(lambda: _present(home, project, bank.name, archive[:-4]), sleeper=sleeper)
    return _scoped_save(home, project, bank, archive, sleeper=sleeper)


def save_databank_archive(sqx_home, payload, *, worker_is_active=None):
    _request(payload, {"project", "databank", "archive", "archive_sha256"})
    with _ACTIONS_LOCK:
        home, _bank = _context(sqx_home, payload, worker_is_active=worker_is_active, native=False)
        return _snapshot(home, payload)


def export_databank_archives(sqx_home, payload, *, worker_is_active=None):
    """Export exact selected native bytes and their manifest, without native writes."""
    _request(payload, {"project", "databank", "archives"})
    records = _manifest(payload)
    manifest = json.dumps(payload, sort_keys=True, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    with _ACTIONS_LOCK:
        home, _bank = _context(sqx_home, payload, worker_is_active=worker_is_active, native=False)
        output = BytesIO()
        total = 0
        with ZipFile(output, "w") as archive:
            archive.writestr("manifest.json", manifest)
            for row in records:
                raw = _snapshot(home, {"project": payload["project"], "databank": payload["databank"], **row})
                total += len(raw)
                if total > 128 * 1024 * 1024:
                    _refuse("databank_export_size_invalid", "Selected archives exceed the 128 MiB bundle limit. Save a smaller selection.")
                archive.writestr(row["archive"], raw)
        return output.getvalue(), sha256(manifest).hexdigest()


def mutate_databank(sqx_home, action, payload, *, raw=None, store=None, worker_is_active=None, sleeper=sleep):
    if action in {"import-discard-preview", "import-discard-confirm"}:
        return _discard_import(action, payload, store=store)
    if action == "reconcile":
        return _reconcile_databank(sqx_home, payload, store=store, worker_is_active=worker_is_active)
    if action == "load-resume":
        _request(payload, {"project", "databank", "archive", "source_sha256", "operation_id"})
        if store is None:
            _refuse("databank_custody_unavailable", "Import recovery requires its local custody store.")
        identity = {"action": "load", "request": payload, "runtime_home": str(_verified_home(sqx_home))}
        digest = sha256(json.dumps(identity, sort_keys=True, separators=(",", ":")).encode()).hexdigest()
        path = _physical(Path(store.root).resolve(), Path(store.root) / "databank-actions" / f"{digest}.json")
        if not path.is_file():
            _refuse("databank_import_missing", "This exact import has no retained source. Select the original file to retry.")
        journal = _read_journal(store, path)
        if any(journal[key] != value for key, value in identity.items()):
            _refuse("databank_mutation_corrupt", "Import recovery request does not match its retained intent.")
        raw = store.read_evidence(EvidenceRef.parse(journal["source_ref"]))
        action = "load"
    if action in {"purge-preview", "purge-confirm"}:
        return _purge_candidate(sqx_home, action, payload, store=store, worker_is_active=worker_is_active, sleeper=sleeper)
    fields = {"project", "databank"}
    if action in {"copy", "move", "remove", "clear", "snapshot"}:
        return _bulk_databank(sqx_home, action, payload, store=store, worker_is_active=worker_is_active, sleeper=sleeper)
    if action == "load":
        fields |= {"archive", "source_sha256", "operation_id"}
    elif action == "rename":
        fields |= {"archive", "archive_sha256", "new_name", "operation_id"}
    elif action != "create":
        _refuse("databank_action_invalid", "Unknown databank action.")
    _request(payload, fields)
    if action == "load":
        inspect_databank_upload(raw, payload["archive"])
        if sha256(raw).hexdigest() != payload["source_sha256"]:
            _refuse("databank_archive_stale", "Uploaded archive bytes do not match the selected file.")
    if action in {"load", "rename"} and (store is None or not callable(getattr(store, "put_evidence", None))):
        _refuse("databank_custody_unavailable", "Local custody is required to preserve the original strategy archive.")
    with _ACTIONS_LOCK:
        if action in {"load", "rename"}:
            _reserve_operation(store, _verified_home(sqx_home), action, payload)
        home, bank = _context(sqx_home, payload, worker_is_active=worker_is_active)
        project, bank_name = payload["project"], payload["databank"]
        names = _banks(home, project)
        fields = {"projectName": project, "databankName": bank_name}
        if action == "create":
            if bank_name.casefold() in {name.casefold() for name in names} or bank.exists():
                _refuse("databank_collision", "That databank already exists.")
            _call(home, "/project/createDatabank", {**fields, "predefined": "false", "position": str(len(names))}, method="GET")
            def created():
                if bank_name not in _banks(home, project):
                    return False
                project_path = _physical(home, bank.parent.parent / "project.cfx")
                with ZipFile(project_path) as archive:
                    config = ElementTree.fromstring(archive.read("config.xml"))
                return any(node.get("name") == bank_name for node in config.findall("./Databanks/Databank"))
            _wait(created, sleeper=sleeper)
            return _record(action, payload)
        if bank_name not in names:
            _refuse("databank_missing", "Select an existing native databank.")
        if action == "load":
            _prepare_bank_storage(home, bank)
        elif not bank.is_dir():
            _refuse("databank_missing", "Select an existing native databank.")
        if action == "rename":
            return _rename_databank(home, bank, payload, store, sleeper=sleeper)
        return _import_databank(home, bank, payload, raw, store, sleeper=sleeper)


def _journal_write(store, path, journal):
    _physical(Path(store.root).resolve(), path)
    store._atomic_write(path, json.dumps(journal, sort_keys=True, separators=(",", ":")).encode())


def _reconcile_databank(sqx_home, payload, *, store, worker_is_active):
    """An explicit storage observation, never a backtest or a token-only match."""
    from .research_candidates import read_candidate_revision
    from .research_candidate_memberships import record_databank_membership_operation
    from .sqx_candidate_identity import SqxCandidateIdentityError
    _request(payload, {"project", "databank", "archive", "archive_sha256", "previous_archive_sha256",
        "candidate_entity_id", "candidate_revision", "membership_revision", "operation_id"})
    if not isinstance(payload["previous_archive_sha256"], str) or not _DIGEST.fullmatch(payload["previous_archive_sha256"]):
        _refuse("databank_digest_invalid", "The exact previous retained archive digest is required.")
    if store is None:
        _refuse("databank_custody_unavailable", "Candidate reconciliation requires local custody.")
    with _ACTIONS_LOCK:
        _reserve_operation(store, _verified_home(sqx_home), "reconcile", payload)
        home, _bank = _context(sqx_home, payload, worker_is_active=worker_is_active)
        candidate = read_candidate_revision(store, payload["candidate_entity_id"], payload["candidate_revision"])
        source = {key: payload[key] for key in ("project", "databank", "archive")}
        source["archive_sha256"] = payload["previous_archive_sha256"]
        _candidate_preflight(store, candidate, "reserialize", source)
        raw = _snapshot(home, payload)
        try:
            membership = record_databank_membership_operation(store, action="reserialize",
                candidate_entity_id=candidate["entity_id"], candidate_revision=candidate["revision"],
                source=source, destination={key: payload[key] for key in ("project", "databank", "archive")},
                archive_bytes=raw, expected_membership_revision=payload["membership_revision"])
        except SqxCandidateIdentityError as exc:
            raise SqxDatabankActionError(exc.code, exc.detail) from exc
        return {**_record("reconcile", payload, archive=payload["archive"], raw=raw),
            "candidate_entity_id": candidate["entity_id"], "candidate_revision": candidate["revision"],
            "membership_revision": membership["revision"], "reused": membership["reused"]}


def _complete_import_reservation(store, payload, candidate_entity_id):
    path = _physical(Path(store.root).resolve(), Path(store.root) / "databank-actions" / "requests" / f'{payload["operation_id"]}.json')
    reserved = json.loads(path.read_bytes())
    if reserved.get("candidate_entity_id") not in {None, candidate_entity_id}:
        _refuse("databank_operation_corrupt", "The retained operation belongs to a different Candidate.")
    reserved["candidate_entity_id"] = candidate_entity_id
    store._atomic_write(path, json.dumps(reserved, sort_keys=True, separators=(",", ":")).encode())


def _discard_import(action, payload, *, store):
    """Explicit local reclamation only before the native import was submitted."""
    from .research_candidate_memberships import (preview_candidate_purge, prepare_candidate_purge,
        finish_candidate_purge, read_candidate_purge, _purge_path)
    from .research_custody import ResearchEntityId, ResearchKind
    fields = {"project", "databank", "archive", "source_sha256", "operation_id"}
    confirming = action == "import-discard-confirm"
    _request(payload, fields | ({"expected_preview_sha256"} if confirming else set()))
    if store is None:
        _refuse("databank_custody_unavailable", "Discarding an import requires its local custody store.")
    if confirming and (not isinstance(payload["expected_preview_sha256"], str) or not _DIGEST.fullmatch(payload["expected_preview_sha256"])):
        _refuse("databank_digest_invalid", "Confirm the exact retained import deletion preview.")
    request = {key: payload[key] for key in fields}
    root = Path(store.root).resolve()
    with _ACTIONS_LOCK:
        reservation_path = _physical(root, root / "databank-actions" / "requests" / f'{request["operation_id"]}.json')
        try:
            with reservation_path.open("rb") as handle:
                reservation = json.loads(handle.read(1025))
            required = {"schema", "operation_id", "request_sha256"}
            if (not isinstance(reservation, dict) or not required <= set(reservation) or set(reservation) - required - {"candidate_entity_id"}
                    or reservation["schema"] != "tc.sqx-databank-operation.v1" or reservation["operation_id"] != request["operation_id"]
                    or not isinstance(reservation["request_sha256"], str) or not _DIGEST.fullmatch(reservation["request_sha256"])):
                raise ValueError("reservation")
        except (OSError, ValueError) as exc:
            raise SqxDatabankActionError("databank_import_missing", "No readable retained import matches this request.") from exc
        candidate_id = reservation.get("candidate_entity_id")
        if candidate_id is not None:
            if ResearchEntityId.parse(candidate_id).kind != ResearchKind.CANDIDATE:
                _refuse("databank_operation_corrupt", "Retained import Candidate identity is invalid.")
            if _purge_path(store, candidate_id).is_file():
                intent = read_candidate_purge(store, candidate_id)
                binding = intent["preview"].get("cancel_import") or {}
                if binding.get("request") != request or binding.get("mutation_id") != reservation["request_sha256"]:
                    _refuse("databank_operation_conflict", "This request does not match the retained import deletion.")
                if not confirming:
                    return intent
                if intent["intent_id"] != payload["expected_preview_sha256"]:
                    _refuse("candidate_purge_conflict", "Confirm the exact retained import deletion preview.")
                return finish_candidate_purge(store, candidate_id, intent_id=intent["intent_id"])
        path = _physical(root, root / "databank-actions" / f'{reservation["request_sha256"]}.json')
        journal = _read_journal(store, path)
        if journal["action"] != "load" or journal["request"] != request:
            _refuse("databank_operation_conflict", "Select the exact retained import request.")
        if candidate_id is not None and candidate_id != journal["candidate_entity_id"]:
            _refuse("databank_operation_corrupt", "Import reservation and journal disagree about Candidate identity.")
        # Engine idle and a momentarily absent file do not drain an outstanding
        # native HTTP loader. A submitted import must be verified and published
        # before ordinary Candidate deletion; never guess that it has stopped.
        if journal["phase"] != "prepared":
            _refuse("databank_import_submitted", "This import reached the native engine. Resume it, then delete the saved Candidate. Retained files are kept because native work may still be pending.")
        candidate_id = journal["candidate_entity_id"]
        descriptor = {"mutation_id": journal["mutation_id"], "journal_sha256": sha256(path.read_bytes()).hexdigest(),
            "native_disposition": "not_submitted"}
        preview = preview_candidate_purge(store, candidate_id, cancel_import=descriptor)
        if not confirming:
            return preview
        if preview["intent_id"] != payload["expected_preview_sha256"]:
            _refuse("databank_import_discard_preview_changed", "Retained import files changed; review the current deletion preview.")
        # Keep a small reserved identity before full-journal deletion, so an old
        # import request cannot resurrect cancelled content after reclamation.
        _complete_import_reservation(store, request, candidate_id)
        intent = prepare_candidate_purge(store, candidate_id, expected_preview_sha256=preview["intent_id"], cancel_import=descriptor)
        return finish_candidate_purge(store, candidate_id, intent_id=intent["intent_id"])


def _import_databank(home, bank, payload, original, store, *, sleeper):
    """Resume an exact retained import; a filename never establishes ownership."""
    from secrets import token_hex
    from uuid import uuid4
    from .research_candidates import prepare_databank_import_candidate, publish_databank_import_candidate
    from .sqx_candidate_identity import stamp_import_candidate_token, verify_native_import
    identity = {"action": "load", "request": payload, "runtime_home": str(home)}
    digest = sha256(json.dumps(identity, sort_keys=True, separators=(",", ":")).encode()).hexdigest()
    path = _physical(Path(store.root).resolve(), Path(store.root) / "databank-actions" / f"{digest}.json")
    destination = {key: payload[key] for key in ("project", "databank", "archive")}
    target = _physical(home, bank / payload["archive"])
    project, archive = payload["project"], payload["archive"]
    if path.exists():
        journal = _read_journal(store, path)
        if any(journal[key] != value for key, value in identity.items()) or journal["destination"] != destination:
            _refuse("databank_mutation_corrupt", "Import intent differs from this exact request and runtime.")
        if store.read_evidence(EvidenceRef.parse(journal["source_ref"])) != original:
            _refuse("databank_archive_stale", "Retry requires the exact original imported file.")
        prepared = store.read_evidence(EvidenceRef.parse(journal["prepared_ref"]))
        if stamp_import_candidate_token(original, journal["candidate_token"]) != prepared:
            _refuse("databank_mutation_corrupt", "The retained import derivative does not match its original.")
    else:
        for pending in _pending_journals(store):
            if pending["destination"] == destination:
                _refuse("databank_mutation_pending", "Resume the existing import or mutation for this exact destination first.")
        if target.is_file():
            prior = _prior_upload(store, home, payload)
            if prior is not None:
                _complete_import_reservation(store, payload, prior["candidate_entity_id"])
                return prior
        if target.exists() or any(p.name.casefold() == archive.casefold() for p in bank.iterdir()) or _present(home, project, bank.name, archive[:-4]):
            _refuse("databank_collision", "A strategy with that name already exists. Choose a different name.")
        token = token_hex(32)
        prepared = stamp_import_candidate_token(original, token)
        journal = {"schema": "tc.sqx-databank-mutation.v1", **identity, "mutation_id": digest,
            "candidate_entity_id": f"tc-research:candidate:v1:{uuid4()}", "candidate_revision": None,
            "membership_revision": None, "source": {**destination, "archive_sha256": payload["source_sha256"]},
            "destination": destination, "source_ref": str(store.put_evidence(original)),
            "prepared_ref": str(store.put_evidence(prepared)), "candidate_token": token, "prepared_revision": None,
            "phase": "prepared", "output_ref": None, "output_sha256": None, "receipt": None}
        _journal_write(store, path, journal)
    try:
        if journal["phase"] == "completed":
            result = _prior_upload(store, home, payload)
            if result is None or result["candidate_entity_id"] != journal["candidate_entity_id"]:
                _refuse("databank_mutation_stale", "The completed import's Candidate location has changed.")
            return {**result, "mutation_ref": digest}
        if journal["prepared_revision"] is None:
            candidate = prepare_databank_import_candidate(store, candidate_entity_id=journal["candidate_entity_id"],
                **destination, original_bytes=original, prepared_bytes=prepared, token=journal["candidate_token"])
            journal.update(prepared_revision=candidate["revision"], candidate_revision=candidate["revision"])
            _journal_write(store, path, journal)
        candidate = {"entity_id": journal["candidate_entity_id"]}
        _candidate_preflight(store, candidate, "load", journal["source"], allowed_mutations={digest})
        if journal["phase"] == "prepared":
            if target.exists() or _present(home, project, bank.name, archive[:-4]):
                _refuse("databank_collision", "The prepared import destination is no longer empty.")
            journal["phase"] = "load_submitted"
            _journal_write(store, path, journal)
        if journal["phase"] in {"load_submitted", "loaded", "save_submitted"}:
            if target.exists():
                with _physical(home, target).open("rb") as handle:
                    output = handle.read(MAX_DATABANK_ARCHIVE_BYTES + 1)
            else:
                if not _present(home, project, bank.name, archive[:-4]):
                    staged = _retain_upload(store, archive, prepared)
                    _call(home, "/project/loadFilesToDatabank", {"projectName": project, "databankName": bank.name,
                        "clear": "false", "filePaths[]": str(staged)})
                    _wait(lambda: _present(home, project, bank.name, archive[:-4]), sleeper=sleeper)
                journal["phase"] = "save_submitted"
                _journal_write(store, path, journal)
                output = _scoped_save(home, project, bank, archive, sleeper=sleeper)
            # A resumed save is owned only when its reserved token AND all strategy,
            # trade, metric and configuration evidence match the retained derivative.
            verify_native_import(prepared, output, journal["candidate_token"], archive)
            ref = store.put_evidence(output)
            journal.update(phase="saved", output_ref=str(ref), output_sha256=ref.digest)
            _journal_write(store, path, journal)
        if journal["phase"] != "saved":
            _refuse("databank_mutation_corrupt", "Unsupported retained import phase.")
        output = store.read_evidence(EvidenceRef.parse(journal["output_ref"]))
        _snapshot(home, {**destination, "archive_sha256": journal["output_sha256"]})
        candidate = publish_databank_import_candidate(store, candidate_entity_id=journal["candidate_entity_id"],
            prepared_revision=journal["prepared_revision"], archive_bytes=output)
        result = {**_record("load", payload, archive=archive, raw=output), "mutation_ref": digest,
            "candidate_entity_id": candidate["entity_id"], "candidate_revision": candidate["revision"],
            "membership_revision": candidate["membership_revision"]}
        _complete_import_reservation(store, payload, candidate["entity_id"])
        journal.update(phase="completed", receipt=result, candidate_revision=candidate["revision"], membership_revision=candidate["membership_revision"])
        _journal_write(store, path, journal)
        return result
    except (OSError, ValueError, RuntimeError) as exc:
        error = exc if isinstance(exc, SqxDatabankActionError) else SqxDatabankActionError(
            getattr(exc, "code", "databank_mutation_interrupted"), getattr(exc, "detail", "Import is incomplete; its original and prepared Candidate remain retained."))
        error.partial_side_effect = journal["phase"] != "prepared"
        error.mutation_ref, error.mutation_phase = digest, journal["phase"]
        raise error from exc


def _rename_databank(home, bank, payload, store, *, sleeper):
    """Resume only a retained exact rename intent, never infer lineage from names."""
    from .research_candidates import read_candidate_revision
    from .research_candidate_memberships import read_candidate_memberships
    source = {key: payload[key] for key in ("project", "databank", "archive", "archive_sha256")}
    destination = {"project": payload["project"], "databank": bank.name, "archive": payload["new_name"] + ".sqx"}
    identity = {"action": "rename", "request": payload, "runtime_home": str(home)}
    digest = sha256(json.dumps(identity, sort_keys=True, separators=(",", ":")).encode()).hexdigest()
    path = _physical(Path(store.root).resolve(), Path(store.root) / "databank-actions" / f"{digest}.json")
    target = _physical(home, bank / destination["archive"])
    old = _physical(home, bank / source["archive"])
    if path.exists():
        journal = _read_journal(store, path)
        if (not isinstance(journal, dict) or journal.get("schema") != "tc.sqx-databank-mutation.v1"
                or any(journal.get(key) != value for key, value in identity.items())
                or journal.get("mutation_id") != digest or journal.get("source") != source or journal.get("destination") != destination):
            _refuse("databank_mutation_corrupt", "The retained mutation is not bound to this exact request and runtime.")
        original = store.read_evidence(EvidenceRef.parse(journal["source_ref"]))
        if sha256(original).hexdigest() != source["archive_sha256"]:
            _refuse("databank_mutation_corrupt", "The retained source archive failed verification.")
        candidate = read_candidate_revision(store, journal["candidate_entity_id"], journal["candidate_revision"])
        candidate["membership_revision"] = journal["membership_revision"]
    else:
        original = _snapshot(home, source)
        candidate = _admit(store, source["project"], bank.name, source["archive"], original)
        _candidate_preflight(store, candidate, "rename", source)
        if target.exists() or any(p.name.casefold() == target.name.casefold() for p in bank.iterdir()) or _present(home, source["project"], bank.name, target.stem):
            _refuse("databank_collision", "A strategy with that name already exists. Choose a different name.")
        journal = {"schema": "tc.sqx-databank-mutation.v1", **identity, "mutation_id": digest,
            "candidate_entity_id": candidate["entity_id"], "candidate_revision": candidate["revision"],
            "membership_revision": candidate["membership_revision"], "source": source, "destination": destination,
            "source_ref": str(store.put_evidence(original)), "phase": "prepared", "output_ref": None,
            "output_sha256": None, "receipt": None}
        _journal_write(store, path, journal)
    try:
        if journal["phase"] == "completed":
            output = _snapshot(home, {**destination, "archive_sha256": journal["output_sha256"]})
            current = read_candidate_memberships(store, candidate["entity_id"])
            if not any(all(row.get(key) == value for key, value in destination.items()) and row["archive_sha256"] == sha256(output).hexdigest() for row in current["memberships"]):
                _refuse("databank_mutation_stale", "The completed rename's Candidate location has since changed.")
            return {**_record("rename", payload, archive=target.name, raw=output), "mutation_ref": digest,
                "candidate_entity_id": candidate["entity_id"], "candidate_revision": candidate["revision"],
                "membership_revision": current["revision"], "reused": True}
        _candidate_preflight(store, candidate, "rename", source, allowed_mutations={digest})
        current = read_candidate_memberships(store, candidate["entity_id"])
        source_bound = any(row["candidate_revision"] == candidate["revision"] and all(row.get(key) == value for key, value in source.items()) for row in current["memberships"])
        published = (journal["phase"] == "source_removed" and current["event"]["action"] == "rename"
            and current["event"]["source"] == source and current["event"]["destination"] == {**destination, "archive_sha256": journal["output_sha256"]})
        if not published and (not source_bound or current["revision"] != journal["membership_revision"]):
            _refuse("databank_mutation_stale", "Candidate membership changed since this exact rename was prepared.")
        if journal["phase"] in {"prepared", "rename_submitted"}:
            _snapshot(home, source)
            if target.exists() or _present(home, source["project"], bank.name, target.stem):
                _refuse("databank_mutation_ambiguous", "A partial rename has an unreceipted destination. The original archive is retained; reconcile this exact mutation before retrying.")
            ensure_databank_result(home, source["project"], bank.name, source["archive"], sleeper=sleeper)
            journal["phase"] = "rename_submitted"
            _journal_write(store, path, journal)
            _call(home, "/databank/rename", {"projectName": source["project"], "databankName": bank.name,
                "strategies": old.stem, "name": target.stem, "prefix": "", "postfix": ""})
            _wait(lambda: _present(home, source["project"], bank.name, target.stem) and not _present(home, source["project"], bank.name, old.stem), sleeper=sleeper)
            journal["phase"] = "renamed"
            _journal_write(store, path, journal)
        if journal["phase"] in {"renamed", "save_submitted"}:
            if target.exists():
                _refuse("databank_mutation_ambiguous", "Native save created an unreceipted destination. The original archive is retained; reconcile this exact mutation before retrying.")
            if not _present(home, source["project"], bank.name, target.stem):
                _refuse("databank_persistence_unverified", "The renamed native strategy is unavailable; its retained source has not been deleted.")
            journal["phase"] = "save_submitted"
            _journal_write(store, path, journal)
            output = _scoped_save(home, source["project"], bank, target.name, sleeper=sleeper)
            ref = store.put_evidence(output)
            journal.update(phase="saved", output_ref=str(ref), output_sha256=ref.digest)
            _journal_write(store, path, journal)
        if journal["phase"] not in {"saved", "source_removed"}:
            _refuse("databank_mutation_corrupt", "The retained mutation phase is invalid.")
        output = store.read_evidence(EvidenceRef.parse(journal["output_ref"]))
        if sha256(output).hexdigest() != journal["output_sha256"]:
            _refuse("databank_mutation_corrupt", "The retained producer output failed verification.")
        _snapshot(home, {**destination, "archive_sha256": journal["output_sha256"]})
        if not _present(home, source["project"], bank.name, target.stem):
            _refuse("databank_persistence_unverified", "The exact renamed native strategy is not present.")
        if old.exists():
            _snapshot(home, source)
            if _present(home, source["project"], bank.name, old.stem):
                _call(home, "/project/removeReports", {"projectName": source["project"], "databankName": bank.name, "strategies": old.stem})
                _wait(lambda: not _present(home, source["project"], bank.name, old.stem), sleeper=sleeper)
            if old.exists():
                _snapshot(home, source)
                _physical(home, old).unlink()
        if _present(home, source["project"], bank.name, old.stem):
            _refuse("databank_persistence_unverified", "Native source removal has not been confirmed.")
        journal["phase"] = "source_removed"
        _journal_write(store, path, journal)
        _membership(store, candidate, "rename", source, destination=destination, raw=output)
        result = {**_record("rename", payload, archive=target.name, raw=output), "mutation_ref": digest,
            "candidate_entity_id": candidate["entity_id"], "candidate_revision": candidate["revision"],
            "membership_revision": candidate["membership_revision"]}
        journal.update(phase="completed", receipt=result)
        _journal_write(store, path, journal)
        return result
    except (OSError, ValueError, RuntimeError) as exc:
        if isinstance(exc, SqxDatabankActionError):
            error = exc
        else:
            error = SqxDatabankActionError(getattr(exc, "code", "databank_mutation_interrupted"),
                getattr(exc, "detail", "The native rename is incomplete. Its exact source and mutation intent remain retained."))
        error.partial_side_effect = journal["phase"] != "prepared"
        error.mutation_ref, error.mutation_phase = digest, journal["phase"]
        raise error from exc


def _manifest(payload, *, maximum=100):
    records = payload.get("archives")
    if not isinstance(records, list) or not 1 <= len(records) <= maximum:
        _refuse("databank_selection_invalid", f"Select between 1 and {maximum} exact strategy archives.")
    names = set()
    for record in records:
        _request(record, {"archive", "archive_sha256"})
        if record["archive"].casefold() in names:
            _refuse("databank_selection_invalid", "Selected archive names must be unique.")
        names.add(record["archive"].casefold())
    return records


def _disk_manifest(home, bank):
    paths = sorted(bank.glob("*.sqx"), key=lambda path: path.name.casefold())
    if len(paths) > 10000:
        _refuse("databank_selection_limit", "This bank exceeds the 10,000-record snapshot limit. Remove explicit selections first.")
    records = []
    for path in paths:
        _name(path.name)
        _physical(home, path)
        with path.open("rb") as handle:
            raw = handle.read(MAX_DATABANK_ARCHIVE_BYTES + 1)
        metadata = inspect_databank_upload(raw, path.name)
        records.append({"archive": path.name, "archive_sha256": metadata["archive_sha256"]})
    return records


def _bank_count(home, project, bank):
    payload = _call(home, "/project/databankList", {"projectName": project}, method="GET")
    rows = [row for row in payload.get("databanks", []) if isinstance(row, dict) and row.get("name") == bank]
    if len(rows) != 1 or type(rows[0].get("records")) is not int or rows[0]["records"] < 0:
        _refuse("databank_readback_invalid", "Native bank record count is unavailable; an entire-bank action cannot be verified.")
    return rows[0]["records"]


def _bulk_databank(sqx_home, action, payload, *, store, worker_is_active, sleeper):
    expected = {"project", "databank"}
    if action != "snapshot":
        expected.add("operation_id")
    expected |= {"snapshot_ref"} if action == "clear" else ({"archives"} if action != "snapshot" else set())
    if action in {"copy", "move"}:
        expected |= {"target_project", "target_databank"}
    _request(payload, expected)
    if store is None or not callable(getattr(store, "put_evidence", None)):
        _refuse("databank_custody_unavailable", "Local custody is required before changing strategies.")
    if action in {"copy", "move"}:
        _name(payload["target_project"])
        _name(payload["target_databank"])
        if (payload["project"], payload["databank"]) == (payload["target_project"], payload["target_databank"]):
            _refuse("databank_collision", "Select a different destination databank.")
    records = None if action in {"snapshot", "clear"} else _manifest(payload)
    with _ACTIONS_LOCK:
        if action != "snapshot":
            _reserve_operation(store, _verified_home(sqx_home), action, payload)
        home, bank = _context(sqx_home, payload, worker_is_active=worker_is_active)
        project = payload["project"]
        if bank.name not in _banks(home, project) or not bank.is_dir():
            _refuse("databank_missing", "Select an existing native databank.")
        if action == "snapshot":
            records = _disk_manifest(home, bank)
            if not records:
                _refuse("databank_empty", "The selected bank contains no persisted strategies.")
            manifest = {"schema": "tc.sqx-databank-snapshot.v1", "project": project, "databank": bank.name, "archives": records}
            ref = store.put_evidence(json.dumps(manifest, sort_keys=True, separators=(",", ":")).encode())
            return {"schema": manifest["schema"], "project": project, "databank": bank.name, "snapshot_ref": str(ref), "archive_count": len(records)}
        if action == "clear":
            manifest = json.loads(store.read_evidence(EvidenceRef.parse(payload["snapshot_ref"])))
            if (not isinstance(manifest, dict) or set(manifest) != {"schema", "project", "databank", "archives"}
                    or manifest["schema"] != "tc.sqx-databank-snapshot.v1" or manifest["project"] != project or manifest["databank"] != bank.name):
                _refuse("databank_snapshot_invalid", "The bank snapshot is not bound to this selection.")
            records = _manifest(manifest, maximum=10000)
            # Previously removed rows may be absent on a retry; their exact
            # completed journals are checked below. New or changed rows refuse.
            disk = _disk_manifest(home, bank)
            if any(row not in records for row in disk):
                _refuse("databank_archive_stale", "The databank changed after confirmation. Review a fresh bank snapshot.")
        destination = None
        if action in {"copy", "move"}:
            target = {"project": payload["target_project"], "databank": payload["target_databank"]}
            _same_home, destination = _context(home, target, worker_is_active=worker_is_active)
            if destination.name not in _banks(home, target["project"]):
                _refuse("databank_missing", "Select an existing destination databank.")
            _prepare_bank_storage(home, destination)
        results = _journaled_records(home, bank, destination, action, payload, records, store, sleeper=sleeper)
        return {**_record(action, payload), **({"snapshot_ref": payload["snapshot_ref"]} if action == "clear" else {}), "archives": records,
                "target_project": payload.get("target_project"), "target_databank": payload.get("target_databank"),
                "results": results, "removed_count": len(records) if action in {"move", "remove", "clear"} else 0}


def _journaled_records(home, bank, destination, action, payload, records, store, *, sleeper):
    from .research_candidates import read_candidate_revision
    from .research_candidate_memberships import candidate_admission_batch, read_candidate_memberships
    native_action = "remove" if action == "clear" else action
    items, pending = [], _pending_journals(store)
    with candidate_admission_batch(store) as inventory:
        for row in records:
            source = {"project": payload["project"], "databank": bank.name, **row}
            target = ({"project": payload["target_project"], "databank": destination.name, "archive": row["archive"]} if destination is not None else None)
            request = {**source, "operation_id": payload["operation_id"], **({"target_project": target["project"], "target_databank": target["databank"]} if target else {})}
            identity = {"action": native_action, "request": request, "runtime_home": str(home)}
            digest = sha256(json.dumps(identity, sort_keys=True, separators=(",", ":")).encode()).hexdigest()
            path = _physical(Path(store.root).resolve(), Path(store.root) / "databank-actions" / f"{digest}.json")
            if path.exists():
                journal = _read_journal(store, path)
                if (not isinstance(journal, dict) or journal.get("schema") != "tc.sqx-databank-mutation.v1"
                        or any(journal.get(key) != value for key, value in identity.items())
                        or journal.get("mutation_id") != digest or journal.get("source") != source or journal.get("destination") != target):
                    _refuse("databank_mutation_corrupt", "The retained mutation does not match its exact source and destination.")
                raw = store.read_evidence(EvidenceRef.parse(journal["source_ref"]))
                if sha256(raw).hexdigest() != source["archive_sha256"]:
                    _refuse("databank_mutation_corrupt", "The retained source archive failed verification.")
                candidate = read_candidate_revision(store, journal["candidate_entity_id"], journal["candidate_revision"])
                candidate["membership_revision"] = journal["membership_revision"]
            else:
                raw = _snapshot(home, source)
                candidate = _admit(store, source["project"], bank.name, row["archive"], raw, admission_inventory=inventory)
                if target and ((_physical(home, destination / row["archive"])).exists() or _present(home, target["project"], destination.name, row["archive"][:-4])):
                    _refuse("databank_collision", "A destination strategy already exists. No selected records were transferred.")
                journal = {"schema": "tc.sqx-databank-mutation.v1", **identity, "mutation_id": digest,
                    "candidate_entity_id": candidate["entity_id"], "candidate_revision": candidate["revision"],
                    "membership_revision": candidate["membership_revision"], "source": source, "destination": target,
                    "source_ref": str(store.put_evidence(raw)), "phase": "prepared", "output_ref": None,
                    "output_sha256": None, "receipt": None}
            items.append((path, journal, candidate))
    allowed = {journal["mutation_id"] for _, journal, _ in items}
    for path, journal, candidate in items:
        _candidate_preflight(store, candidate, native_action, journal["source"], allowed_mutations=allowed, pending=pending)
        if not path.exists():
            _journal_write(store, path, journal)
    # Complete the native memory inventory before whole-bank clear. Counts are
    # observations only; every persisted record was separately hash verified.
    if action == "clear":
        expected_native = 0
        for _, journal, _ in items:
            source = journal["source"]
            old = _physical(home, bank / source["archive"])
            if journal["phase"] == "prepared":
                _snapshot(home, source)
                ensure_databank_result(home, source["project"], bank.name, old.name, sleeper=sleeper)
            if _present(home, source["project"], bank.name, old.stem):
                expected_native += 1
        if _bank_count(home, payload["project"], bank.name) != expected_native:
            _refuse("databank_archive_stale", "Native memory contains records outside the confirmed bank snapshot.")
    results = []
    for path, journal, candidate in items:
        source, target = journal["source"], journal["destination"]
        old = _physical(home, bank / source["archive"])
        try:
            current = read_candidate_memberships(store, candidate["entity_id"])
            bound_source = any(row["candidate_revision"] == candidate["revision"] and all(row.get(key) == value for key, value in source.items()) for row in current["memberships"])
            bound_target = target is not None and any(row["candidate_revision"] == candidate["revision"] and all(row.get(key) == value for key, value in target.items()) and row["archive_sha256"] == journal["output_sha256"] for row in current["memberships"])
            removed = not bound_source and current["event"]["action"] == "remove" and current["event"]["source"] == source
            if journal["phase"] != "completed" and not bound_source and not (removed and journal["phase"] == "source_removed"):
                _refuse("databank_mutation_stale", "The exact Candidate source membership changed during this mutation.")
            if target:
                target_path = _physical(home, destination / target["archive"])
                if journal["phase"] in {"prepared", "load_submitted"}:
                    _snapshot(home, source)
                    if target_path.exists() or _present(home, target["project"], target["databank"], target_path.stem):
                        _refuse("databank_mutation_ambiguous", "The destination exists without a retained producer receipt. The source is preserved for reconciliation.")
                    raw = store.read_evidence(EvidenceRef.parse(journal["source_ref"]))
                    staged = _retain_upload(store, source["archive"], raw)
                    journal["phase"] = "load_submitted"
                    _journal_write(store, path, journal)
                    _call(home, "/project/loadFilesToDatabank", {"projectName": target["project"], "databankName": target["databank"], "clear": "false", "filePaths[]": str(staged)})
                    _wait(lambda: _present(home, target["project"], target["databank"], target_path.stem), sleeper=sleeper)
                    journal["phase"] = "loaded"
                    _journal_write(store, path, journal)
                if journal["phase"] in {"loaded", "save_submitted"}:
                    if target_path.exists():
                        _refuse("databank_mutation_ambiguous", "The native destination save has no retained digest. The source is preserved for reconciliation.")
                    journal["phase"] = "save_submitted"
                    _journal_write(store, path, journal)
                    output = _scoped_save(home, target["project"], destination, target["archive"], sleeper=sleeper)
                    ref = store.put_evidence(output)
                    journal.update(phase="saved", output_ref=str(ref), output_sha256=ref.digest)
                    _journal_write(store, path, journal)
                output = store.read_evidence(EvidenceRef.parse(journal["output_ref"]))
                if sha256(output).hexdigest() != journal["output_sha256"]:
                    _refuse("databank_mutation_corrupt", "The retained destination failed verification.")
                _snapshot(home, {**target, "archive_sha256": journal["output_sha256"]})
                if not _present(home, target["project"], target["databank"], target["archive"][:-4]):
                    _refuse("databank_persistence_unverified", "The persisted native destination is not present in the databank.")
                if journal["phase"] == "saved":
                    if not bound_target:
                        candidate["membership_revision"] = current["revision"]
                        _membership(store, candidate, "copy", source, destination=target, raw=output)
                    else:
                        candidate["membership_revision"] = current["revision"]
                    journal.update(phase="copied", membership_revision=candidate["membership_revision"])
                    _journal_write(store, path, journal)
                elif not bound_target:
                    _refuse("databank_mutation_stale", "The retained destination no longer belongs to this Candidate.")
                results.append({"archive": target["archive"], "archive_sha256": journal["output_sha256"]})
            if native_action in {"move", "remove"}:
                if journal["phase"] == "completed":
                    if old.exists() or _present(home, source["project"], bank.name, old.stem):
                        _refuse("databank_mutation_stale", "The previously removed location is occupied again; it is not an automatic retry target.")
                else:
                    if journal["phase"] in {"prepared", "copied"}:
                        _snapshot(home, source)
                        ensure_databank_result(home, source["project"], bank.name, old.name, sleeper=sleeper)
                        journal["phase"] = "remove_submitted"
                        _journal_write(store, path, journal)
                    if journal["phase"] not in {"remove_submitted", "source_removed"}:
                        _refuse("databank_mutation_corrupt", "The retained removal phase is invalid.")
                    if _present(home, source["project"], bank.name, old.stem):
                        _snapshot(home, source)
                        _call(home, "/project/removeReports", {"projectName": source["project"], "databankName": bank.name, "strategies": old.stem})
                        _wait(lambda: not _present(home, source["project"], bank.name, old.stem), sleeper=sleeper)
                    if old.exists():
                        _snapshot(home, source)
                        _physical(home, old).unlink()
                    journal["phase"] = "source_removed"
                    _journal_write(store, path, journal)
                    current = read_candidate_memberships(store, candidate["entity_id"])
                    candidate["membership_revision"] = current["revision"]
                    _membership(store, candidate, "remove", source)
            journal.update(phase="completed", membership_revision=candidate["membership_revision"], receipt={"action": native_action, "source": source, "destination": target})
            _journal_write(store, path, journal)
        except (OSError, ValueError, RuntimeError) as exc:
            error = exc if isinstance(exc, SqxDatabankActionError) else SqxDatabankActionError(getattr(exc, "code", "databank_mutation_interrupted"), getattr(exc, "detail", "The exact native mutation is incomplete; its source and intent remain retained."))
            error.partial_side_effect = journal["phase"] != "prepared"
            error.mutation_ref, error.mutation_phase = journal["mutation_id"], journal["phase"]
            raise error from exc
    return results


def _purge_candidate(sqx_home, action, payload, *, store, worker_is_active, sleeper):
    from .research_candidate_memberships import (preview_candidate_purge, prepare_candidate_purge,
        finish_candidate_purge, read_candidate_memberships)
    from .research_candidates import read_candidate_revision
    expected = {"candidate_entity_id"} | ({"expected_preview_sha256"} if action == "purge-confirm" else set())
    _request(payload, expected)
    if store is None:
        _refuse("databank_custody_unavailable", "Candidate deletion requires local custody.")
    if action == "purge-confirm" and (not isinstance(payload["expected_preview_sha256"], str)
            or not _DIGEST.fullmatch(payload["expected_preview_sha256"])):
        _refuse("databank_digest_invalid", "Confirm the exact Candidate deletion preview.")
    candidate_id = payload["candidate_entity_id"]
    # The same desktop lock protects preview/confirmation and native location writes.
    with _ACTIONS_LOCK:
        if action == "purge-preview":
            return preview_candidate_purge(store, candidate_id)
        intent = prepare_candidate_purge(store, candidate_id, expected_preview_sha256=payload["expected_preview_sha256"])
        if intent["state"] != "prepared":
            return finish_candidate_purge(store, candidate_id, intent_id=intent["intent_id"])
        current = read_candidate_memberships(store, candidate_id)
        preview = intent["preview"]["memberships"]
        keys = ("project", "databank", "archive", "archive_sha256", "candidate_revision")
        allowed = {tuple(row[key] for key in keys) for row in preview}
        if any(tuple(row[key] for key in keys) not in allowed for row in current["memberships"]):
            _refuse("candidate_purge_preview_changed", "Candidate locations changed after the deletion preview.")
        for member in current["memberships"]:
            source = {key: member[key] for key in ("project", "databank", "archive", "archive_sha256")}
            _request(source, set(source))
            home, bank = _context(sqx_home, source, worker_is_active=worker_is_active)
            path = _physical(home, bank / source["archive"])
            pending_remove = any(journal["action"] == "remove" and journal["candidate_entity_id"] == candidate_id
                and journal["source"] == source for journal in _pending_journals(store))
            if path.exists() or pending_remove:
                operation_id = sha256(json.dumps({"purge_intent": intent["intent_id"], "source": source}, sort_keys=True, separators=(",", ":")).encode()).hexdigest()[:32]
                mutate_databank(home, "remove", {"project": source["project"], "databank": source["databank"],
                    "operation_id": operation_id,
                    "archives": [{key: source[key] for key in ("archive", "archive_sha256")}]},
                    store=store, worker_is_active=worker_is_active, sleeper=sleeper)
            else:
                # Resume a crash after native deletion but before custody publication.
                # Never delete a surviving native row whose persisted bytes are absent.
                if _present(home, source["project"], source["databank"], source["archive"][:-4]):
                    _refuse("databank_persistence_unverified", "Native strategy still exists without the previewed persisted archive.")
                latest = read_candidate_memberships(store, candidate_id)
                candidate = read_candidate_revision(store, candidate_id, member["candidate_revision"])
                candidate["membership_revision"] = latest["revision"]
                _candidate_preflight(store, candidate, "remove", source)
                _membership(store, candidate, "remove", source)
        return finish_candidate_purge(store, candidate_id, intent_id=intent["intent_id"])
