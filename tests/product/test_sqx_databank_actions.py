from hashlib import sha256
from http.server import ThreadingHTTPServer
from io import BytesIO
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Thread
from unittest import TestCase
from unittest.mock import patch
from urllib.error import HTTPError
from urllib.parse import quote
from urllib.request import Request, urlopen
from uuid import uuid4
from zipfile import ZipFile, ZIP_DEFLATED

from tradercockpit import sqx_databank_actions as actions
from tradercockpit.app_server import make_handler
from tradercockpit.research_custody import FileResearchCustodyStore, EvidenceRef, ResearchCustodyError


def sqx(extra=None):
    stream = BytesIO()
    with ZipFile(stream, "w", compression=ZIP_DEFLATED) as archive:
        archive.writestr("settings.xml", "<ResultsGroup><ResultsMap><Results/></ResultsMap><SymbolsMap/><SpecialValuesMap><SettingsMap/></SpecialValuesMap></ResultsGroup>")
        archive.writestr("strategy_Portfolio.xml", '<StrategyFile AppVersion="SQX Build 144.2953"><Strategy name="Native"/></StrategyFile>')
        archive.writestr("version.txt", "1")
        archive.writestr("orders.bin", b"native test bytes")
        if extra:
            archive.writestr(*extra)
    return stream.getvalue()


class DatabankActionsTests(TestCase):
    def test_import_recovery_reads_exact_requests_without_native_calls_or_writes(self):
        path, journal = self.prepared_import()
        before = {p.relative_to(self.store.root): p.read_bytes() for p in self.store.root.rglob("*") if p.is_file()}
        expected = {"status": "ready", "operations": [{"action": "load", "target": journal["request"]}]}
        self.assertEqual(actions.read_import_recovery(self.home, "Example", self.store), expected)
        self.assertEqual(actions.read_import_recovery(self.home, "Other", self.store)["operations"], [])
        with patch.object(actions, "_verified_home", return_value=self.root / "other-runtime"):
            self.assertEqual(actions.read_import_recovery(None, None, self.store)["operations"], [])
        self.assertEqual(before, {p.relative_to(self.store.root): p.read_bytes() for p in self.store.root.rglob("*") if p.is_file()})
        actions._journal_write(self.store, path, {**journal, "phase": "load_submitted"})
        self.assertEqual(actions.read_import_recovery(self.home, None, self.store), expected)
        self.assertEqual(self.calls, [])
        path.write_text("{broken")
        with self.assertRaises(actions.SqxDatabankActionError):
            actions.read_import_recovery(self.home, None, self.store)

    def prepared_import(self):
        with patch.object(actions, "_candidate_preflight", side_effect=OSError("interrupted before native load")), self.assertRaises(actions.SqxDatabankActionError):
            self.mutate()
        path = next((self.store.root / "databank-actions").glob("*.json"))
        journal = json.loads(path.read_bytes())
        self.assertEqual(journal["phase"], "prepared")
        self.assertFalse(any(path == "/project/loadFilesToDatabank" for path, _, _ in self.calls))
        self.calls.clear()
        return path, journal

    def test_import_discard_preview_is_pure_confirm_reclaims_and_reopens_without_native_calls(self):
        from tradercockpit.research_candidates import list_current_candidates
        from tradercockpit.research_custody import ResearchEntityId
        path, journal = self.prepared_import()
        request = journal["request"]
        original = self.root / "Desktop original.sqx"
        original.write_bytes(self.raw)
        before = {p.relative_to(self.store.root): p.read_bytes() for p in self.store.root.rglob("*") if p.is_file()}
        preview = self.mutate("import-discard-preview", request)
        self.assertEqual(preview["state"], "preview")
        self.assertEqual(preview["preview"]["cancel_import"]["request"], request)
        self.assertEqual(preview["preview"]["cancel_import"]["native_disposition"], "not_submitted")
        self.assertEqual(preview["preview"]["memberships"], [])
        self.assertEqual({p.relative_to(self.store.root): p.read_bytes() for p in self.store.root.rglob("*") if p.is_file()}, before)
        confirm = {**request, "expected_preview_sha256": preview["intent_id"]}
        completed = self.mutate("import-discard-confirm", confirm)
        self.assertEqual(completed["state"], "completed")
        self.assertGreater(completed["reclaimed_bytes"], 0)
        self.assertEqual(completed["reclaimed_bytes"], sum(row["bytes"] for row in completed["reclaimed_files"]))
        self.assertFalse(path.exists())
        for key in ("source_ref", "prepared_ref"):
            self.assertFalse(self.store._evidence_path(EvidenceRef.parse(journal[key])).exists())
        self.assertEqual(original.read_bytes(), self.raw)
        self.assertEqual(list_current_candidates(self.store)["candidates"], [])
        self.assertIsNotNone(self.store.deletion_record(ResearchEntityId.parse(journal["candidate_entity_id"])))
        self.store = FileResearchCustodyStore(self.store.root)
        self.assertEqual(self.mutate("import-discard-confirm", confirm), completed)
        self.assertEqual(self.mutate("import-discard-preview", request), completed)
        self.assertEqual(self.calls, [])
        with self.assertRaises(actions.SqxDatabankActionError) as refused:
            self.mutate(payload=request)
        self.assertEqual(refused.exception.code, "entity_deleted")
        self.assertEqual(self.calls, [])

    def test_import_discard_refuses_changed_request_digest_and_submitted_phase(self):
        path, journal = self.prepared_import()
        request = journal["request"]
        preview = self.mutate("import-discard-preview", request)
        before = path.read_bytes()
        for changed in ({"archive": "Other.sqx"}, {"source_sha256": "0" * 64}, {"operation_id": uuid4().hex}):
            with self.subTest(changed=changed), self.assertRaises(actions.SqxDatabankActionError):
                self.mutate("import-discard-preview", {**request, **changed})
        with self.assertRaises(actions.SqxDatabankActionError) as refused:
            self.mutate("import-discard-confirm", {**request, "expected_preview_sha256": "0" * 64})
        self.assertEqual(refused.exception.code, "databank_import_discard_preview_changed")
        self.assertEqual(path.read_bytes(), before)
        actions._journal_write(self.store, path, {**journal, "phase": "load_submitted"})
        for action, payload in (("import-discard-preview", request),
                ("import-discard-confirm", {**request, "expected_preview_sha256": preview["intent_id"]})):
            with self.subTest(action=action), self.assertRaises(actions.SqxDatabankActionError) as refused:
                self.mutate(action, payload)
            self.assertEqual(refused.exception.code, "databank_import_submitted")
        self.assertTrue(path.exists())
        self.assertFalse((self.store.base / "candidate-purges").exists())
        self.assertEqual(self.calls, [])

    def test_import_discard_resumes_cleanup_after_journal_deleted(self):
        from tradercockpit.research_candidates import list_current_candidates
        path, journal = self.prepared_import()
        request = journal["request"]
        preview = self.mutate("import-discard-preview", request)
        confirm = {**request, "expected_preview_sha256": preview["intent_id"]}
        prepared_path = self.store._evidence_path(EvidenceRef.parse(journal["prepared_ref"]))
        unlink = Path.unlink
        def interrupt(target, *args, **kwargs):
            if target == prepared_path:
                raise OSError("interrupted retained file cleanup")
            return unlink(target, *args, **kwargs)
        with patch.object(Path, "unlink", interrupt), self.assertRaises(OSError):
            self.mutate("import-discard-confirm", confirm)
        self.assertFalse(path.exists())
        self.assertTrue(prepared_path.exists())
        self.store = FileResearchCustodyStore(self.store.root)
        self.assertEqual(actions.read_import_recovery(self.home, "Example", self.store)["operations"],
            [{"action": "load", "target": request, "discard_preview_sha256": preview["intent_id"]}])
        with self.assertRaises(actions.SqxDatabankActionError):
            self.mutate(payload=request)
        with self.assertRaises(actions.SqxDatabankActionError):
            self.mutate("import-discard-confirm", {**confirm, "expected_preview_sha256": "0" * 64})
        with self.assertRaises(actions.SqxDatabankActionError):
            self.mutate("import-discard-confirm", {**confirm, "archive": "Other.sqx"})
        result = self.mutate("import-discard-confirm", confirm)
        self.assertEqual(result["state"], "completed")
        self.assertFalse(prepared_path.exists())
        self.assertEqual(actions.read_import_recovery(self.home, "Example", self.store)["operations"], [])
        self.assertEqual(list_current_candidates(self.store)["candidates"], [])
        self.assertEqual(self.calls, [])

    def test_import_discard_without_candidate_root_blocks_resume_during_confirmed_intent(self):
        from tradercockpit.research_custody import ResearchEntityId
        with patch("tradercockpit.research_candidates.prepare_databank_import_candidate", side_effect=OSError("interrupted before root publication")), self.assertRaises(actions.SqxDatabankActionError):
            self.mutate()
        path = next((self.store.root / "databank-actions").glob("*.json"))
        journal = json.loads(path.read_bytes())
        self.assertIsNone(journal["prepared_revision"])
        self.assertEqual(journal["phase"], "prepared")
        self.calls.clear()
        request = journal["request"]
        preview = self.mutate("import-discard-preview", request)
        self.assertEqual(preview["preview"]["revisions"], [])
        confirm = {**request, "expected_preview_sha256": preview["intent_id"]}
        with patch("tradercockpit.research_candidate_memberships.finish_candidate_purge", side_effect=OSError("interrupted before deletion record")), self.assertRaises(OSError):
            self.mutate("import-discard-confirm", confirm)
        self.assertIsNone(self.store.deletion_record(ResearchEntityId.parse(journal["candidate_entity_id"])))
        self.store = FileResearchCustodyStore(self.store.root)
        with self.assertRaises(actions.SqxDatabankActionError) as refused:
            self.mutate(payload=request)
        self.assertEqual(refused.exception.code, "candidate_purge_pending")
        self.assertEqual(self.mutate("import-discard-confirm", confirm)["state"], "completed")
        self.assertFalse(path.exists())
        self.assertEqual(self.calls, [])

    def test_http_import_discard_requires_same_origin_and_exact_confirmation(self):
        path, journal = self.prepared_import()
        web = self.root / "web"
        web.mkdir()
        server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(web, self.home, research_store=self.store))
        thread = Thread(target=server.serve_forever, daemon=True)
        thread.start()
        origin = f"http://127.0.0.1:{server.server_port}"
        def post(action, payload, source=origin):
            request = Request(origin + "/api/sqx-databank/" + action, data=json.dumps(payload).encode(),
                headers={"Origin": source, "Content-Type": "application/json"})
            try:
                response = urlopen(request, timeout=5)
            except HTTPError as error:
                response = error
            with response:
                return response.status, json.loads(response.read())
        try:
            request = journal["request"]
            self.assertEqual(post("import-discard-preview", request, "https://evil.test")[0], 403)
            status, preview = post("import-discard-preview", request)
            self.assertEqual(status, 200, preview)
            self.assertEqual(preview["state"], "preview")
            self.assertTrue(path.exists())
            confirm = {**request, "expected_preview_sha256": preview["intent_id"]}
            self.assertEqual(post("import-discard-confirm", confirm, "https://evil.test")[0], 403)
            self.assertEqual(post("import-discard-confirm", {**confirm, "all": True})[0], 409)
            self.assertEqual(post("import-discard-confirm", {**confirm, "expected_preview_sha256": "0" * 64})[0], 409)
            self.assertTrue(path.exists())
            status, completed = post("import-discard-confirm", confirm)
            self.assertEqual(status, 200, completed)
            self.assertEqual(completed["state"], "completed")
            self.assertEqual(post("import-discard-confirm", confirm), (200, completed))
            self.assertFalse(path.exists())
            self.assertEqual(self.calls, [])
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=2)

    def test_import_recovers_unreceipted_save_after_store_reopen(self):
        from tradercockpit.research_candidates import list_current_candidates
        write = actions._journal_write
        def interrupt(store, path, journal):
            if journal["action"] == "load" and journal["phase"] == "saved":
                raise OSError("crash after native save")
            return write(store, path, journal)
        with patch.object(actions, "_journal_write", side_effect=interrupt), self.assertRaises(actions.SqxDatabankActionError):
            self.mutate()
        self.assertEqual(list_current_candidates(self.store)["candidates"], [])
        retained = json.loads(next((self.store.root / "databank-actions").glob("*.json")).read_bytes())
        self.assertEqual(retained["phase"], "save_submitted")
        self.assertIsNotNone(retained["prepared_revision"])
        self.store = FileResearchCustodyStore(self.store.root)
        resumed = actions.mutate_databank(self.home, "load-resume", retained["request"], store=self.store, sleeper=lambda _: None)
        self.assertEqual(resumed["candidate_entity_id"], retained["candidate_entity_id"])
        self.assertEqual(len(list_current_candidates(self.store)["candidates"]), 1)
        self.assertEqual(sum(path == "/project/loadFilesToDatabank" for path, _, _ in self.calls), 1)
        self.assertEqual(sum(path == "/project/saveReports" for path, _, _ in self.calls), 1)
        self.assertTrue(actions.mutate_databank(self.home, "load-resume", retained["request"], store=self.store, sleeper=lambda _: None)["reused"])
        with self.assertRaises(actions.SqxDatabankActionError):
            actions.mutate_databank(self.home, "load-resume", {**retained["request"], "archive": "Foreign.sqx"}, store=self.store)

    def test_import_rejects_changed_results_and_does_not_publish_or_overwrite(self):
        from tradercockpit.research_candidates import list_current_candidates
        self.output_transform = lambda raw: sqx(("unexpected-result.bin", b"changed"))
        with self.assertRaises(actions.SqxDatabankActionError):
            self.mutate()
        before = (self.bank / "Native.sqx").read_bytes()
        self.assertEqual(list_current_candidates(self.store)["candidates"], [])
        with self.assertRaises(actions.SqxDatabankActionError):
            self.mutate()
        self.assertEqual((self.bank / "Native.sqx").read_bytes(), before)
        self.assertEqual(self.store.read_evidence(EvidenceRef.from_bytes(self.raw)), self.raw)
        self.assertEqual(sum(path == "/project/loadFilesToDatabank" for path, _, _ in self.calls), 1)
        with self.assertRaisesRegex(actions.SqxDatabankActionError, "existing import"):
            self.mutate(payload={**self.payload, "operation_id": uuid4().hex})

    def test_reconcile_requires_exact_prior_membership_and_preserves_history(self):
        from tradercockpit.research_candidate_memberships import read_candidate_memberships
        loaded = self.mutate()
        original = self.loaded_raw
        stream = BytesIO(original)
        with ZipFile(stream, "a") as archive:
            archive.comment = b"normal native shutdown serialization"
        rewritten = stream.getvalue()
        (self.bank / "Native.sqx").write_bytes(rewritten)
        request = {key: loaded[key] for key in ("project", "databank", "archive", "candidate_entity_id", "candidate_revision", "membership_revision")}
        request.update(previous_archive_sha256=loaded["archive_sha256"], archive_sha256=sha256(rewritten).hexdigest())
        with patch.object(actions, "_poll_custom_project_stats", return_value={"Example": {"running": True, "running_status": "running"}}), self.assertRaises(actions.SqxDatabankActionError):
            self.mutate("reconcile", request)
        result = self.mutate("reconcile", request)
        self.assertEqual(result["candidate_entity_id"], loaded["candidate_entity_id"])
        self.assertTrue(self.mutate("reconcile", request)["reused"])
        reopened = read_candidate_memberships(FileResearchCustodyStore(self.store.root), loaded["candidate_entity_id"], history=True)
        self.assertEqual([row["event"]["action"] for row in reopened["history"]], ["admit", "reserialize"])
        self.assertEqual(self.store.read_evidence(EvidenceRef.from_bytes(original)), original)
        self.assertEqual(self.store.read_evidence(EvidenceRef.from_bytes(rewritten)), rewritten)
        with self.assertRaises(ResearchCustodyError):
            self.mutate("reconcile", {**request, "previous_archive_sha256": "0" * 64})
        renamed = self.mutate("rename", {**{key: result[key] for key in ("project", "databank", "archive", "archive_sha256")}, "new_name": "Reopened"})
        self.assertEqual(renamed["candidate_entity_id"], loaded["candidate_entity_id"])

    def test_deleted_import_nonce_cannot_resurrect_candidate(self):
        loaded = self.mutate()
        journal = json.loads(next((self.store.root / "databank-actions").glob("*.json")).read_bytes())
        preview = self.mutate("purge-preview", {"candidate_entity_id": loaded["candidate_entity_id"]})
        self.mutate("purge-confirm", {"candidate_entity_id": loaded["candidate_entity_id"], "expected_preview_sha256": preview["intent_id"]})
        with self.assertRaisesRegex(actions.SqxDatabankActionError, "deliberately deleted"):
            self.mutate(payload=journal["request"])
        again = self.mutate(payload={**self.payload, "operation_id": uuid4().hex})
        self.assertNotEqual(again["candidate_entity_id"], loaded["candidate_entity_id"])

    def setUp(self):
        self.temp = TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.home = self.root / "native"
        (self.home / "internal/web/SQUANT").mkdir(parents=True)
        (self.home / "internal/web/SQUANT/build.dat").write_text("2953")
        (self.home / "internal/SQUANT.dat").write_bytes(b"144fixture")
        self.bank = self.home / "user/projects/Example/databanks/Results"
        self.bank.mkdir(parents=True)
        self.banks = ["Results"]
        self.config()
        self.store = FileResearchCustodyStore(self.root / "custody")
        self.raw = sqx()
        self.payload = {"project": "Example", "databank": "Results", "archive": "Native.sqx", "source_sha256": sha256(self.raw).hexdigest()}
        self.rows = {}
        self.native_rows = {"Results": self.rows}
        self.calls = []
        self.operation_ids = {}
        self.refuse_rename = False
        self.persist = True
        self.output_transform = lambda raw: raw
        self.addCleanup(patch.stopall)
        patch.object(actions, "_poll_custom_project_stats", return_value={"Example": {"running": False, "running_status": "stopped"}}).start()
        patch.object(actions, "_poll_engine_channel", return_value=(None, None)).start()
        patch.object(actions, "sqx_local_json", side_effect=self.native).start()
        patch.object(actions, "ensure_databank_result", side_effect=lambda *a, **kw: self.native_rows.setdefault(a[2], {}).setdefault(a[3][:-4], (self.bank.parent / a[2] / a[3]).read_bytes())).start()

    def config(self):
        xml = '<Project><Databanks>' + ''.join(f'<Databank name="{name}"/>' for name in self.banks) + '</Databanks></Project>'
        with ZipFile(self.bank.parent.parent / "project.cfx", "w") as archive:
            archive.writestr("config.xml", xml)

    def native(self, home, path, *, fields=None, method="GET", **kwargs):
        self.calls.append((path, fields, method))
        rows = self.native_rows.setdefault(fields.get("databankName", "Results"), {})
        bank = self.bank.parent / fields.get("databankName", "Results")
        if path == "/project/databankList":
            return {"success": True, "databanks": [{"name": n, "records": len(self.native_rows.get(n, {}))} for n in self.banks]}
        if path == "/project/getDataItems":
            return {"success": True, "dataItems": ["Main"]} if fields["reportName"] in rows else {"strDoesntExist": True, "error": "missing"}
        if path == "/project/loadFilesToDatabank":
            self.assertEqual(fields["clear"], "false")
            staged = Path(fields["filePaths[]"])
            self.assertTrue(staged.is_relative_to(self.store.root))
            rows[staged.stem] = staged.read_bytes()
        if path == "/databank/rename" and not self.refuse_rename:
            rows[fields["name"]] = rows.pop(fields["strategies"])
        if path == "/project/synchronizeDatabank":
            self.fail("Whole-bank synchronization must never be invoked")
        if path == "/project/saveReports" and self.persist:
            self.assertEqual(fields["extension[name]"], "sqx")
            self.assertEqual(fields["handleMagicNumbers"], "false")
            self.assertEqual(fields["fileName"], fields["strategies"])
            target = Path(fields["folder"]) / (fields["fileName"] + ".sqx")
            self.assertFalse(target.exists(), "Native overwrite dialog must not be triggered")
            target.write_bytes(self.output_transform(rows[fields["strategies"]]))
        if path == "/project/removeReports" and self.persist:
            self.assertNotEqual(fields["strategies"], "all")
            for name in fields["strategies"].split(","):
                rows.pop(name)
        if path == "/project/createDatabank":
            self.banks.append(fields["databankName"])
            if self.persist:
                self.config()
        return {"success": "ok"}

    def mutate(self, action="load", payload=None):
        payload = dict(payload or self.payload)
        if action in {"load", "rename", "copy", "move", "remove", "clear", "reconcile"} and "operation_id" not in payload:
            key = json.dumps([action, payload], sort_keys=True)
            payload["operation_id"] = self.operation_ids.setdefault(key, uuid4().hex)
        result = actions.mutate_databank(self.home, action, payload, raw=self.raw, store=self.store, sleeper=lambda _: None)
        if action == "load":
            self.loaded_raw = (self.bank.parent / payload["databank"] / payload["archive"]).read_bytes()
        return result

    def test_load_native_persist_save_exact_bytes_and_rename(self):
        loaded = self.mutate()
        self.assertTrue(loaded["persisted"])
        self.assertEqual(loaded["source_sha256"], self.payload["source_sha256"])
        self.assertEqual(self.store.read_evidence(EvidenceRef.from_bytes(self.raw)), self.raw)
        selected = {k: loaded[k] for k in ("project", "databank", "archive", "archive_sha256")}
        self.assertEqual(actions.save_databank_archive(self.home, selected), self.loaded_raw)
        renamed = self.mutate("rename", {**selected, "new_name": "Renamed"})
        self.assertEqual(renamed["archive"], "Renamed.sqx")
        self.assertFalse((self.bank / "Native.sqx").exists())
        self.assertEqual((self.bank / "Renamed.sqx").read_bytes(), self.loaded_raw)
        self.assertTrue(any(path == "/databank/rename" for path, _, _ in self.calls))

    def test_create_requires_native_and_saved_config_readback(self):
        record = self.mutate("create", {"project": "Example", "databank": "Imported"})
        self.assertTrue(record["persisted"])
        self.persist = False
        with self.assertRaisesRegex(actions.SqxDatabankActionError, "persisted completion"):
            self.mutate("create", {"project": "Example", "databank": "Unsaved"})

    def test_rename_journal_recovers_after_cleanup_interruption_without_duplicate_candidate(self):
        loaded = self.mutate()
        request = {**{key: loaded[key] for key in ("project", "databank", "archive", "archive_sha256")}, "new_name": "Recovered"}
        write = actions._journal_write
        def interrupt(store, path, journal):
            if journal["phase"] == "source_removed":
                raise OSError("simulated interrupted cleanup receipt")
            return write(store, path, journal)
        with patch.object(actions, "_journal_write", side_effect=interrupt), self.assertRaises(actions.SqxDatabankActionError) as error:
            self.mutate("rename", request)
        self.assertTrue(error.exception.partial_side_effect)
        self.assertFalse((self.bank / "Native.sqx").exists())
        self.assertTrue((self.bank / "Recovered.sqx").exists())
        recovered = self.mutate("rename", request)
        self.assertEqual(recovered["candidate_entity_id"], loaded["candidate_entity_id"])
        self.assertTrue(self.mutate("rename", request)["reused"])
        self.assertEqual(sum(path == "/databank/rename" for path, _, _ in self.calls), 1)

    def test_rename_unreceipted_save_refuses_retry_and_preserves_original(self):
        loaded = self.mutate()
        request = {**{key: loaded[key] for key in ("project", "databank", "archive", "archive_sha256")}, "new_name": "Unreceipted"}
        write = actions._journal_write
        def interrupt(store, path, journal):
            if journal["phase"] == "saved":
                raise OSError("simulated save receipt interruption")
            return write(store, path, journal)
        with patch.object(actions, "_journal_write", side_effect=interrupt), self.assertRaises(actions.SqxDatabankActionError):
            self.mutate("rename", request)
        with self.assertRaises(actions.SqxDatabankActionError) as error:
            self.mutate("rename", request)
        self.assertEqual(error.exception.code, "databank_mutation_ambiguous")
        self.assertTrue(error.exception.partial_side_effect)
        self.assertEqual((self.bank / "Native.sqx").read_bytes(), self.loaded_raw)
        self.assertEqual(sum(path == "/project/saveReports" for path, _, _ in self.calls), 2)

    def test_rename_never_unlinks_changed_source_after_producer_save(self):
        loaded = self.mutate()
        request = {**{key: loaded[key] for key in ("project", "databank", "archive", "archive_sha256")}, "new_name": "Verified target"}
        changed = sqx(("changed.txt", b"different original"))
        def transform(raw):
            (self.bank / "Native.sqx").write_bytes(changed)
            return raw
        self.output_transform = transform
        with self.assertRaises(actions.SqxDatabankActionError) as error:
            self.mutate("rename", request)
        self.assertEqual(error.exception.code, "databank_archive_stale")
        self.assertEqual((self.bank / "Native.sqx").read_bytes(), changed)
        self.assertEqual((self.bank / "Verified target.sqx").read_bytes(), self.loaded_raw)

    def test_registered_empty_bank_load_and_copy_create_only_verified_storage(self):
        self.mutate("create", {"project": "Example", "databank": "Empty"})
        empty = self.bank.parent / "Empty"
        self.assertFalse(empty.exists(), "Native create registers an empty bank without a directory")
        loaded = self.mutate(payload={**self.payload, "databank": "Empty"})
        self.assertTrue(loaded["persisted"])
        self.assertEqual((empty / "Native.sqx").read_bytes(), self.loaded_raw)
        self.mutate("create", {"project": "Example", "databank": "Empty copy"})
        copied = self.mutate("copy", {"project": "Example", "databank": "Empty", "archives": [
            {key: loaded[key] for key in ("archive", "archive_sha256")}],
            "target_project": "Example", "target_databank": "Empty copy"})
        self.assertEqual(copied["results"][0]["archive"], "Native.sqx")
        self.assertEqual((self.bank.parent / "Empty copy/Native.sqx").read_bytes(), self.loaded_raw)
        with self.assertRaises(actions.SqxDatabankActionError):
            self.mutate(payload={**self.payload, "databank": "Unregistered"})
        self.assertFalse((self.bank.parent / "Unregistered").exists())

    def test_native_success_without_rename_or_persistence_is_refused(self):
        loaded = self.mutate()
        selected = {k: loaded[k] for k in ("project", "databank", "archive", "archive_sha256")}
        self.refuse_rename = True
        with self.assertRaisesRegex(actions.SqxDatabankActionError, "persisted completion"):
            self.mutate("rename", {**selected, "new_name": "False success"})
        self.assertTrue((self.bank / "Native.sqx").is_file())
        self.persist = False
        with self.assertRaisesRegex(actions.SqxDatabankActionError, "persisted completion"):
            self.mutate(payload={**self.payload, "archive": "Not persisted.sqx"})
        self.assertEqual(self.store.read_evidence(EvidenceRef.from_bytes(self.raw)), self.raw)

    def test_unsafe_zip_names_sizes_stale_identity_and_paths_refuse_before_native(self):
        for extra in [("../escape", b"x"), ("C:/escape", b"x"), ("settings.xml", b"x"), ("SETTINGS.xml", b"x"), ("a/./b", b"x"), ("extra.xml", b'<!DOCTYPE foo [<!ENTITY bar SYSTEM "file:///secret">]><foo/>'), ("utf16.xml", '<!DOCTYPE Settings [<!ENTITY demo "probe">]><Settings/>'.encode("utf-16")), ("large", b"x" * (32 * 1024 * 1024 + 1))]:
            with self.subTest(entry=extra[0]), self.assertRaises(actions.SqxDatabankActionError):
                actions.inspect_databank_upload(sqx(extra), "Native.sqx")
        for field, value in [("project", "../other"), ("archive", "evil.sqx:ads"), ("databank", "CON"), ("source_sha256", "0" * 64)]:
            with self.subTest(field=field), self.assertRaises(actions.SqxDatabankActionError):
                self.mutate(payload={**self.payload, field: value})
        self.assertEqual(self.calls, [])
        loaded = self.mutate()
        selected = {k: loaded[k] for k in ("project", "databank", "archive", "archive_sha256")}
        with self.assertRaisesRegex(actions.SqxDatabankActionError, "changed"):
            actions.save_databank_archive(self.home, {**selected, "archive_sha256": "0" * 64})
        self.assertTrue(self.mutate()["reused"])

    def test_unknown_active_status_and_link_refuse_without_mutation(self):
        for rows in ({}, {"Example": {"running": True, "running_status": "running"}}):
            with patch.object(actions, "_poll_custom_project_stats", return_value=rows), self.assertRaises(actions.SqxDatabankActionError):
                self.mutate()
        self.assertTrue(all(path == "/main/appSwitched" for path, _, _ in self.calls))
        with patch.object(Path, "is_symlink", return_value=True), self.assertRaisesRegex(actions.SqxDatabankActionError, "link"):
            self.mutate()
        with self.assertRaises(actions.SqxDatabankActionError):
            actions.mutate_databank(self.home, "load", self.payload, raw=self.raw, store=None)

    def test_builtin_project_idle_uses_fresh_native_engine_status(self):
        with patch.object(actions, "_poll_custom_project_stats", return_value={}), patch.object(actions, "_poll_engine_channel", return_value=({"projectName": "Example", "runningStatus": 0}, None)):
            self.assertTrue(self.mutate()["persisted"])

    def test_explicit_mutation_routes_native_feed_but_ack_alone_never_proves_idle(self):
        active = {"module": "RETESTER"}
        def routed_native(home, path, **kwargs):
            if path == "/main/appSwitched":
                self.assertEqual(kwargs["fields"], {"productCode": "TASKMANAGER"})
                active["module"] = "TASKMANAGER"
            return self.native(home, path, **kwargs)
        def fresh_stats(home):
            return {"Example": {"running": False, "running_status": "beforeStart"}} if active["module"] == "TASKMANAGER" else {}
        with patch.object(actions, "sqx_local_json", side_effect=routed_native), \
                patch.object(actions, "_poll_custom_project_stats", side_effect=fresh_stats):
            loaded = self.mutate()
        self.assertEqual(self.calls[0][0], "/main/appSwitched")
        self.calls.clear()
        with patch.object(actions, "_poll_custom_project_stats", return_value={}):
            with self.assertRaises(actions.SqxDatabankActionError) as error:
                self.mutate(payload={**self.payload, "archive": "Unverified.sqx"})
        self.assertEqual(error.exception.code, "databank_status_unavailable")
        self.assertTrue(all(path == "/main/appSwitched" for path, _, _ in self.calls))
        self.calls.clear()
        selection = {key: loaded[key] for key in ("project", "databank", "archive", "archive_sha256")}
        self.assertEqual(actions.save_databank_archive(self.home, selection), self.loaded_raw)
        self.assertEqual(self.calls, [], "Read-only archive Save must not reroute the native feed")

    def test_scoped_load_copy_move_remove_preserve_unrelated_disk_only_archive(self):
        unrelated = sqx(("private-note.txt", b"original"))
        (self.bank / "Unrelated.sqx").write_bytes(unrelated)
        loaded = self.mutate()
        row = {key: loaded[key] for key in ("archive", "archive_sha256")}
        self.mutate("create", {"project": "Example", "databank": "Copies"})
        selection = {"project": "Example", "databank": "Results", "archives": [row]}
        copied = self.mutate("copy", {**selection, "target_project": "Example", "target_databank": "Copies"})
        self.assertEqual(copied["removed_count"], 0)
        self.assertTrue((self.bank / "Native.sqx").exists())
        with self.assertRaisesRegex(actions.SqxDatabankActionError, "already exists"):
            self.mutate("move", {**selection, "target_project": "Example", "target_databank": "Copies"})
        self.mutate("remove", {"project": "Example", "databank": "Copies", "archives": copied["results"]})
        moved = self.mutate("move", {**selection, "target_project": "Example", "target_databank": "Copies"})
        self.assertEqual(moved["removed_count"], 1)
        self.assertFalse((self.bank / "Native.sqx").exists())
        self.assertEqual((self.bank / "Unrelated.sqx").read_bytes(), unrelated)
        self.assertEqual(self.store.read_evidence(EvidenceRef.from_bytes(self.raw)), self.raw)

    def test_clear_freezes_whole_bank_above_100_and_refuses_new_records(self):
        for index in range(130):
            (self.bank / f"Native {index:03}.sqx").write_bytes(self.raw)
        target = {"project": "Example", "databank": "Results"}
        snapshot = self.mutate("snapshot", target)
        self.assertEqual(snapshot["archive_count"], 130)
        (self.bank / "New.sqx").write_bytes(self.raw)
        with self.assertRaisesRegex(actions.SqxDatabankActionError, "changed"):
            self.mutate("clear", {**target, "snapshot_ref": snapshot["snapshot_ref"]})
        self.assertFalse(any(path == "/project/removeReports" for path, _, _ in self.calls))
        snapshot = self.mutate("snapshot", target)
        result = self.mutate("clear", {**target, "snapshot_ref": snapshot["snapshot_ref"]})
        self.assertEqual(result["removed_count"], 131)
        self.assertEqual(result["snapshot_ref"], snapshot["snapshot_ref"])
        self.assertEqual(list(self.bank.glob("*.sqx")), [])
        self.assertEqual(self.store.read_evidence(EvidenceRef.from_bytes(self.raw)), self.raw)

    def test_bundle_contains_only_selected_exact_archives_and_canonical_manifest(self):
        for name in ("One.sqx", "Two.sqx", "Unselected.sqx"):
            (self.bank / name).write_bytes(self.raw)
        selection = {"project": "Example", "databank": "Results", "archives": [
            {"archive": name, "archive_sha256": sha256(self.raw).hexdigest()} for name in ("Two.sqx", "One.sqx")]}
        raw, digest = actions.export_databank_archives(self.home, selection)
        with ZipFile(BytesIO(raw)) as archive:
            self.assertEqual(set(archive.namelist()), {"manifest.json", "Two.sqx", "One.sqx"})
            manifest = archive.read("manifest.json")
            self.assertEqual(sha256(manifest).hexdigest(), digest)
            self.assertEqual(json.loads(manifest), selection)
            for item in selection["archives"]:
                self.assertEqual(archive.read(item["archive"]), self.raw)
        self.assertEqual(self.calls, [])

    def test_serialized_upload_retry_reuses_candidate_and_location_operations_keep_identity(self):
        from tradercockpit.research_candidates import read_current_candidate
        from tradercockpit.research_candidate_memberships import read_candidate_memberships
        def serialize(raw):
            output = BytesIO(raw)
            with ZipFile(output, "a") as archive:
                archive.comment = b"native serialization"
            return output.getvalue()
        self.output_transform = serialize
        first = self.mutate()
        self.assertNotEqual(first["archive_sha256"], first["source_sha256"])
        load_count = sum(path == "/project/loadFilesToDatabank" for path, _, _ in self.calls)
        second = self.mutate()
        self.assertTrue(second["reused"])
        self.assertEqual(second["candidate_entity_id"], first["candidate_entity_id"])
        self.assertEqual(sum(path == "/project/loadFilesToDatabank" for path, _, _ in self.calls), load_count)
        selected = {key: first[key] for key in ("project", "databank", "archive", "archive_sha256")}
        renamed = self.mutate("rename", {**selected, "new_name": "Same Candidate"})
        self.assertEqual(renamed["candidate_entity_id"], first["candidate_entity_id"])
        self.mutate("remove", {"project": "Example", "databank": "Results", "archives": [{key: renamed[key] for key in ("archive", "archive_sha256")}]})
        candidate = read_current_candidate(self.store, first["candidate_entity_id"])
        self.assertEqual(candidate["origin"]["original_archive_sha256"], sha256(self.raw).hexdigest())
        membership = read_candidate_memberships(self.store, candidate["entity_id"], history=True)
        self.assertEqual(membership["memberships"], [])
        self.assertEqual([row["event"]["action"] for row in membership["history"]], ["admit", "rename", "remove"])

    def test_corrupt_deflate_is_typed_refusal_and_failed_move_retains_source(self):
        raw = bytearray(self.raw)
        with ZipFile(BytesIO(raw)) as archive:
            member = archive.infolist()[0]
            offset = member.header_offset + 30 + len(member.filename.encode()) + len(member.extra)
        raw[offset] = 0xff
        with self.assertRaises(actions.SqxDatabankActionError):
            actions.inspect_databank_upload(bytes(raw), "Native.sqx")
        loaded = self.mutate()
        self.mutate("create", {"project": "Example", "databank": "Copies"})
        self.persist = False
        with self.assertRaisesRegex(actions.SqxDatabankActionError, "persisted completion"):
            self.mutate("move", {"project": "Example", "databank": "Results", "archives": [{key: loaded[key] for key in ("archive", "archive_sha256")}], "target_project": "Example", "target_databank": "Copies"})
        self.assertEqual((self.bank / "Native.sqx").read_bytes(), self.loaded_raw)
        self.assertFalse(any(path == "/project/removeReports" for path, _, _ in self.calls))

    def test_bulk_journals_resume_copy_publication_and_move_removal(self):
        from tradercockpit.research_candidate_memberships import read_candidate_memberships
        loaded = self.mutate()
        selection = {"project": "Example", "databank": "Results", "archives": [
            {key: loaded[key] for key in ("archive", "archive_sha256")}]}
        self.mutate("create", {"project": "Example", "databank": "Copies"})
        request = {**selection, "target_project": "Example", "target_databank": "Copies"}
        write = actions._journal_write
        def interrupt_copy(store, path, journal):
            if journal["action"] == "copy" and journal["phase"] == "copied":
                raise OSError("interrupted copy publication receipt")
            return write(store, path, journal)
        with patch.object(actions, "_journal_write", side_effect=interrupt_copy), self.assertRaises(actions.SqxDatabankActionError):
            self.mutate("copy", request)
        copied = self.mutate("copy", request)
        self.assertEqual(self.mutate("copy", request)["results"], copied["results"])
        self.assertEqual(len(read_candidate_memberships(self.store, loaded["candidate_entity_id"])["memberships"]), 2)
        self.mutate("create", {"project": "Example", "databank": "Moved"})
        moved_request = {**selection, "target_project": "Example", "target_databank": "Moved"}
        def interrupt_move(store, path, journal):
            if journal["action"] == "move" and journal["phase"] == "source_removed":
                raise OSError("interrupted move removal receipt")
            return write(store, path, journal)
        with patch.object(actions, "_journal_write", side_effect=interrupt_move), self.assertRaises(actions.SqxDatabankActionError):
            self.mutate("move", moved_request)
        self.assertFalse((self.bank / "Native.sqx").exists())
        self.assertEqual(self.mutate("move", moved_request)["removed_count"], 1)
        self.assertEqual(self.mutate("move", moved_request)["removed_count"], 1)
        locations = read_candidate_memberships(self.store, loaded["candidate_entity_id"])["memberships"]
        self.assertEqual({row["databank"] for row in locations}, {"Copies", "Moved"})

    def test_new_copy_after_removing_previous_copy_is_a_new_user_operation(self):
        loaded = self.mutate()
        self.mutate("create", {"project": "Example", "databank": "Copies"})
        request = {"project": "Example", "databank": "Results", "archives": [
            {key: loaded[key] for key in ("archive", "archive_sha256")}],
            "target_project": "Example", "target_databank": "Copies"}
        copied = self.mutate("copy", request)
        self.mutate("remove", {"project": "Example", "databank": "Copies", "archives": copied["results"]})
        copied_again = self.mutate("copy", {**request, "operation_id": uuid4().hex})
        self.assertEqual(copied_again["results"], copied["results"])
        self.assertNotEqual(copied_again["operation_id"], copied["operation_id"])
        self.assertEqual((self.bank.parent / "Copies/Native.sqx").read_bytes(), self.loaded_raw)

    def test_operation_retry_is_exact_and_changed_request_refuses_before_native_calls(self):
        loaded = self.mutate()
        selected = {key: loaded[key] for key in ("project", "databank", "archive", "archive_sha256")}
        request = {**selected, "new_name": "Renamed", "operation_id": uuid4().hex}
        first = self.mutate("rename", request)
        second = self.mutate("rename", request)
        self.assertEqual(first, {key: value for key, value in second.items() if key != "reused"})
        self.assertTrue(second["reused"])
        self.assertEqual(first["operation_id"], request["operation_id"])
        self.assertEqual(sum(path == "/databank/rename" for path, _, _ in self.calls), 1)
        before = len(self.calls)
        with self.assertRaises(actions.SqxDatabankActionError) as error:
            self.mutate("rename", {**request, "new_name": "Different"})
        self.assertEqual(error.exception.code, "databank_operation_conflict")
        self.assertEqual(len(self.calls), before)
        with self.assertRaises(actions.SqxDatabankActionError) as error:
            self.mutate("remove", {"project": "Example", "databank": "Results", "archives": [
                {key: first[key] for key in ("archive", "archive_sha256")}], "operation_id": request["operation_id"]})
        self.assertEqual(error.exception.code, "databank_operation_conflict")
        self.assertEqual(len(self.calls), before)

    def test_operation_identity_is_mandatory_and_strict(self):
        request = {"project": "Example", "databank": "Results", "archives": [
            {"archive": "Native.sqx", "archive_sha256": self.payload["source_sha256"]}]}
        for extra in ({}, {"operation_id": "A" * 32}, {"operation_id": "f" * 31}, {"operation_id": None}):
            with self.subTest(extra=extra), self.assertRaises(actions.SqxDatabankActionError):
                actions.mutate_databank(self.home, "remove", {**request, **extra}, store=self.store)
        self.assertEqual(self.calls, [])

    def test_pending_mutation_blocks_different_action_and_clear_resumes_exact_missing_rows(self):
        loaded = self.mutate()
        selected = {key: loaded[key] for key in ("project", "databank", "archive", "archive_sha256")}
        write = actions._journal_write
        def interrupt(store, path, journal):
            if journal["phase"] == "saved":
                raise OSError("unreceipted save")
            return write(store, path, journal)
        with patch.object(actions, "_journal_write", side_effect=interrupt), self.assertRaises(actions.SqxDatabankActionError):
            self.mutate("rename", {**selected, "new_name": "Pending"})
        with self.assertRaises(actions.SqxDatabankActionError) as error:
            self.mutate("remove", {"project": "Example", "databank": "Results", "archives": [
                {key: selected[key] for key in ("archive", "archive_sha256")}]})
        self.assertEqual(error.exception.code, "databank_mutation_pending")
        self.assertEqual((self.bank / "Native.sqx").read_bytes(), self.loaded_raw)
        self.mutate("create", {"project": "Example", "databank": "Clear retry"})
        other = self.mutate(payload={**self.payload, "databank": "Clear retry", "archive": "Other.sqx"})
        target = {"project": "Example", "databank": "Clear retry"}
        snapshot = self.mutate("snapshot", target)
        clear = {**target, "snapshot_ref": snapshot["snapshot_ref"]}
        def interrupt_remove(store, path, journal):
            if journal["action"] == "remove" and journal["phase"] == "source_removed":
                raise OSError("interrupted remove receipt")
            return write(store, path, journal)
        with patch.object(actions, "_journal_write", side_effect=interrupt_remove), self.assertRaises(actions.SqxDatabankActionError):
            self.mutate("clear", clear)
        self.assertEqual(self.mutate("clear", clear)["removed_count"], 1)
        self.assertEqual(self.mutate("clear", clear)["snapshot_ref"], snapshot["snapshot_ref"])

    def test_native_result_member_colons_are_safe_without_allowing_drive_paths(self):
        for name in ("Results/Main: GBPUSD_M1_dukas_LOM_H1/dailyEquity.bin",
                     "Results/AdditionalMarket: EURUSD_M1_dukas_LOM_H1:  EURUSD_M1_dukas_LOM_H1/dailyEquity.bin"):
            raw = sqx((name, b"native result bytes"))
            self.assertTrue(actions.inspect_databank_upload(raw, "Native.sqx")["inspectable"])
        with self.assertRaises(actions.SqxDatabankActionError):
            actions.inspect_databank_upload(sqx(("C:/escape.bin", b"x")), "Native.sqx")
        with self.assertRaises(actions.SqxDatabankActionError) as raised:
            actions.inspect_databank_upload(sqx(("large", b"x" * (33 * 1024 * 1024))), "Native.sqx")
        self.assertEqual(raised.exception.code, "databank_archive_invalid")
        self.assertIn("oversized", raised.exception.detail)

    def test_pending_purge_blocks_rename_before_native_mutation(self):
        from tradercockpit.research_candidate_memberships import prepare_candidate_purge, preview_candidate_purge
        from tradercockpit.research_custody import ResearchCustodyError
        loaded = self.mutate()
        preview = preview_candidate_purge(self.store, loaded["candidate_entity_id"])
        prepare_candidate_purge(self.store, loaded["candidate_entity_id"], expected_preview_sha256=preview["intent_id"])
        selected = {key: loaded[key] for key in ("project", "databank", "archive", "archive_sha256")}
        self.calls.clear()
        with self.assertRaises(ResearchCustodyError) as raised:
            self.mutate("rename", {**selected, "new_name": "Blocked"})
        self.assertEqual(raised.exception.code, "candidate_purge_pending")
        self.assertFalse(any(path in {"/databank/rename", "/project/loadFilesToDatabank", "/project/saveReports"} for path, _, _ in self.calls))
        self.assertTrue((self.bank / "Native.sqx").is_file())

    def test_purge_preview_is_pure_confirm_removes_only_bound_locations_and_retry_is_safe(self):
        from tradercockpit.research_candidate_memberships import read_candidate_memberships
        loaded = self.mutate()
        request = {"candidate_entity_id": loaded["candidate_entity_id"]}
        preview = self.mutate("purge-preview", request)
        self.assertEqual(preview["state"], "preview")
        self.assertTrue((self.bank / "Native.sqx").is_file())
        self.mutate("create", {"project": "Example", "databank": "Copies"})
        row = {key: loaded[key] for key in ("archive", "archive_sha256")}
        self.mutate("copy", {"project": "Example", "databank": "Results", "archives": [row],
                            "target_project": "Example", "target_databank": "Copies"})
        from tradercockpit.research_custody import ResearchCustodyError
        with self.assertRaises(ResearchCustodyError):
            self.mutate("purge-confirm", {**request, "expected_preview_sha256": preview["intent_id"]})
        preview = self.mutate("purge-preview", request)
        # Simulate a prior native removal whose membership publication was interrupted.
        (self.bank / "Native.sqx").unlink()
        self.rows.pop("Native")
        unrelated = sqx(("unrelated.txt", b"preserve"))
        (self.bank / "Other.sqx").write_bytes(unrelated)
        confirm = {**request, "expected_preview_sha256": preview["intent_id"]}
        completed = self.mutate("purge-confirm", confirm)
        self.assertEqual(completed["state"], "completed")
        self.assertFalse((self.bank.parent / "Copies/Native.sqx").exists())
        self.assertEqual((self.bank / "Other.sqx").read_bytes(), unrelated)
        self.assertEqual(self.mutate("purge-confirm", confirm), completed)

    def test_partial_purge_allows_explicit_reserialization_then_exact_intent_retry(self):
        from tradercockpit.research_candidate_memberships import read_candidate_memberships
        loaded = self.mutate()
        self.mutate("create", {"project": "Example", "databank": "Copies"})
        self.mutate("copy", {"project": "Example", "databank": "Results", "archives": [
            {key: loaded[key] for key in ("archive", "archive_sha256")}],
            "target_project": "Example", "target_databank": "Copies"})
        request = {"candidate_entity_id": loaded["candidate_entity_id"]}
        preview = self.mutate("purge-preview", request)
        confirm = {**request, "expected_preview_sha256": preview["intent_id"]}
        stream = BytesIO(self.loaded_raw)
        with ZipFile(stream, "a") as archive:
            archive.comment = b"native restart serialization"
        rewritten = stream.getvalue()
        (self.bank / "Native.sqx").write_bytes(rewritten)
        self.rows["Native"] = rewritten
        with self.assertRaises(actions.SqxDatabankActionError) as stale:
            self.mutate("purge-confirm", confirm)
        self.assertEqual(stale.exception.code, "databank_archive_stale")
        self.assertFalse((self.bank.parent / "Copies/Native.sqx").exists())
        self.assertEqual((self.bank / "Native.sqx").read_bytes(), rewritten)
        self.store = FileResearchCustodyStore(self.store.root)
        intent_path = self.store.base / "candidate-purges" / f'{loaded["candidate_entity_id"].rsplit(":", 1)[-1]}.json'
        intent_bytes = intent_path.read_bytes()
        current = read_candidate_memberships(self.store, loaded["candidate_entity_id"])
        reconcile = {**{key: loaded[key] for key in ("project", "databank", "archive", "candidate_entity_id", "candidate_revision")},
            "membership_revision": current["revision"], "previous_archive_sha256": loaded["archive_sha256"],
            "archive_sha256": sha256(rewritten).hexdigest()}
        altered = BytesIO()
        with ZipFile(BytesIO(rewritten)) as source, ZipFile(altered, "w") as target:
            for name in source.namelist():
                target.writestr(name, b"different trades" if name == "orders.bin" else source.read(name))
        (self.bank / "Native.sqx").write_bytes(altered.getvalue())
        with self.assertRaises(actions.SqxDatabankActionError):
            self.mutate("reconcile", {**reconcile, "archive_sha256": sha256(altered.getvalue()).hexdigest()})
        self.assertEqual(intent_path.read_bytes(), intent_bytes)
        (self.bank / "Native.sqx").write_bytes(rewritten)
        result = self.mutate("reconcile", reconcile)
        self.assertEqual(result["candidate_revision"], loaded["candidate_revision"])
        self.assertEqual(intent_path.read_bytes(), intent_bytes)
        recovered = self.mutate("purge-preview", request)
        self.assertEqual(recovered["state"], "prepared")
        self.assertEqual(recovered["intent_id"], preview["intent_id"])
        self.assertEqual(recovered["preview"], preview["preview"])
        completed = self.mutate("purge-confirm", confirm)
        self.assertEqual(completed["state"], "completed")
        self.assertEqual(completed["intent_id"], preview["intent_id"])
        self.assertFalse((self.bank / "Native.sqx").exists())
        self.assertEqual(self.mutate("purge-confirm", confirm), completed)

    def test_remove_unassociated_native_archive_retains_owned_candidate_history(self):
        from tradercockpit.research_candidates import read_current_candidate
        from tradercockpit.research_candidate_memberships import read_candidate_memberships
        (self.bank / "Historical.sqx").write_bytes(self.raw)
        self.mutate("remove", {"project": "Example", "databank": "Results", "archives": [
            {"archive": "Historical.sqx", "archive_sha256": sha256(self.raw).hexdigest()}]})
        from tradercockpit.research_custody import ResearchKind
        pointers = list((self.store.base / "current" / ResearchKind.CANDIDATE.value).glob("*.json"))
        self.assertEqual(len(pointers), 1)
        # The immutable Candidate remains discoverable even with no active bank location.
        content = json.loads(pointers[0].read_bytes())
        candidate = read_current_candidate(self.store, content["entity_id"])
        self.assertEqual(candidate["origin"]["kind"], "native_databank")
        self.assertEqual(candidate["origin"]["history_status"], "unknown")
        history = read_candidate_memberships(self.store, candidate["entity_id"], history=True)
        self.assertEqual(history["memberships"], [])
        self.assertEqual([item["event"]["action"] for item in history["history"]], ["admit", "remove"])

    def test_http_same_origin_raw_upload_and_complete_attachment(self):
        web = self.root / "web"
        web.mkdir()
        server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(web, self.home, research_store=self.store))
        thread = Thread(target=server.serve_forever, daemon=True)
        thread.start()
        origin = f"http://127.0.0.1:{server.server_port}"
        def post(action, raw, headers):
            request = Request(origin + "/api/sqx-databank/" + action, data=raw, headers={"Origin": origin, **headers})
            try:
                response = urlopen(request, timeout=5)
            except HTTPError as error:
                response = error
            with response:
                return response.status, response.headers, response.read()
        try:
            headers = {"Content-Type": "application/octet-stream", "X-TraderCockpit-Target": quote(json.dumps({**self.payload, "operation_id": uuid4().hex}))}
            code, _, _ = post("load", b"", {**headers, "Origin": "https://evil.test"})
            self.assertEqual(code, 403)
            self.assertEqual(self.calls, [])
            with patch("tradercockpit.app_server.mutate_databank", side_effect=lambda *a, **kw: actions.mutate_databank(*a, **kw, sleeper=lambda _: None)):
                code, _, body = post("load", self.raw, headers)
            self.assertEqual(code, 200, body)
            loaded = json.loads(body)
            selected = {k: loaded[k] for k in ("project", "databank", "archive", "archive_sha256")}
            code, headers, body = post("save", json.dumps(selected).encode(), {"Content-Type": "application/json"})
            self.assertEqual(code, 200, body)
            self.assertEqual(body, (self.bank / "Native.sqx").read_bytes())
            self.assertEqual(headers["X-Archive-Sha256"], sha256(body).hexdigest())
            self.assertIn("attachment", headers["Content-Disposition"])
            rename = {**selected, "new_name": "HTTP renamed", "operation_id": uuid4().hex}
            code, _, body = post("rename", json.dumps(rename).encode(), {"Content-Type": "application/json"})
            self.assertEqual(code, 200, body)
            self.assertEqual(json.loads(body)["operation_id"], rename["operation_id"])
            code, _, body = post("rename", json.dumps(rename).encode(), {"Content-Type": "application/json"})
            self.assertEqual(code, 200, body)
            self.assertTrue(json.loads(body)["reused"])
            before = len(self.calls)
            code, _, body = post("rename", json.dumps({**rename, "new_name": "Changed"}).encode(), {"Content-Type": "application/json"})
            self.assertEqual(code, 409, body)
            self.assertEqual(json.loads(body)["reason_code"], "databank_operation_conflict")
            self.assertEqual(len(self.calls), before)
            request = {"candidate_entity_id": loaded["candidate_entity_id"]}
            code, _, body = post("purge-preview", json.dumps(request).encode(), {"Content-Type": "application/json"})
            self.assertEqual(code, 200, body)
            preview = json.loads(body)
            self.assertEqual(preview["state"], "preview")
            self.assertTrue((self.bank / "HTTP renamed.sqx").exists())
            confirm = {**request, "expected_preview_sha256": preview["intent_id"]}
            code, _, body = post("purge-confirm", json.dumps({**confirm, "all": True}).encode(), {"Content-Type": "application/json"})
            self.assertEqual(code, 409, body)
            self.assertTrue((self.bank / "HTTP renamed.sqx").exists())
            code, _, body = post("purge-confirm", json.dumps(confirm).encode(), {"Content-Type": "application/json"})
            self.assertEqual(code, 200, body)
            self.assertEqual(json.loads(body)["state"], "completed")
            self.assertFalse((self.bank / "HTTP renamed.sqx").exists())
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=2)
