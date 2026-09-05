from concurrent.futures import ThreadPoolExecutor
from hashlib import sha256
from io import BytesIO
import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
from zipfile import ZipFile

from tradercockpit.research_candidates import CandidateContent, ResearchCandidateError, admit_databank_candidate, list_current_candidates, read_current_candidate, prepare_databank_import_candidate, publish_databank_import_candidate
from tradercockpit.research_candidate_memberships import list_databank_memberships, read_candidate_memberships, record_databank_membership_operation, prepare_candidate_purge, finish_candidate_purge, assert_candidate_membership_action, preview_candidate_purge, associate_databank_results, candidate_admission_batch
from tradercockpit.research_custody import EvidenceRef, FileResearchCustodyStore, ResearchCustodyError, ResearchEntityId, ResearchKind, ResearchRevisionRef


def archive_bytes(name="Original", build="144.2953"):
    output = BytesIO()
    with ZipFile(output, "w") as archive:
        archive.writestr("strategy_Portfolio.xml", f'<StrategyFile AppVersion="SQX Build {build}"><Strategy name="{name}"/></StrategyFile>')
        archive.writestr("settings.xml", '<Settings><Producer>native</Producer></Settings>')
        archive.writestr("version.txt", "1")
        archive.writestr("orders.bin", b"opaque producer bytes")
    return output.getvalue()


def mutation_journal(store, candidate, action, source, destination=None, output=None, phase="completed"):
    request = dict(source)
    if action == "rename":
        request["new_name"] = destination["archive"][:-4]
    elif destination:
        request.update(target_project=destination["project"], target_databank=destination["databank"])
    identity = {"action": action, "request": request, "runtime_home": "fixture-runtime"}
    digest = sha256(json.dumps(identity, sort_keys=True, separators=(",", ":")).encode()).hexdigest()
    ref = store.put_evidence(output) if output else None
    payload = {"schema": "tc.sqx-databank-mutation.v1", **identity, "mutation_id": digest,
        "candidate_entity_id": candidate["entity_id"], "candidate_revision": candidate["revision"],
        "membership_revision": candidate["membership_revision"], "source": source, "destination": destination,
        "source_ref": str(EvidenceRef(source["archive_sha256"])), "phase": phase,
        "output_ref": str(ref) if ref else None, "output_sha256": ref.digest if ref else None,
        "receipt": {"action": action, "source": source, "destination": destination} if phase == "completed" else None}
    path = store.root / "databank-actions" / f"{digest}.json"
    store._atomic_write(path, json.dumps(payload, sort_keys=True, separators=(",", ":")).encode())
    return path


class CandidateMembershipTests(unittest.TestCase):
    def pending_import(self, store, *, with_root, operation_id='d' * 32):
        from test_sqx_candidate_identity import archive as native_archive
        from tradercockpit.sqx_candidate_identity import stamp_import_candidate_token
        original = native_archive()
        token = 'a' * 64
        prepared = stamp_import_candidate_token(original, token)
        entity = str(store.create_entity(ResearchKind.CANDIDATE))
        destination = dict(project='P', databank='Input', archive='Imported.sqx')
        root = prepare_databank_import_candidate(store, candidate_entity_id=entity, **destination,
            original_bytes=original, prepared_bytes=prepared, token=token) if with_root else None
        request = {**destination, 'source_sha256': sha256(original).hexdigest(), 'operation_id': operation_id}
        identity = dict(action='load', request=request, runtime_home='fixture-runtime')
        mutation_id = sha256(json.dumps(identity, sort_keys=True, separators=(',', ':')).encode()).hexdigest()
        journal = {'schema': 'tc.sqx-databank-mutation.v1', **identity, 'mutation_id': mutation_id,
            'candidate_entity_id': entity, 'candidate_revision': root['revision'] if root else None,
            'prepared_revision': root['revision'] if root else None, 'membership_revision': None,
            'source': {**destination, 'archive_sha256': request['source_sha256']}, 'destination': destination,
            'source_ref': str(store.put_evidence(original)), 'prepared_ref': str(store.put_evidence(prepared)),
            'candidate_token': token, 'output_ref': None, 'output_sha256': None, 'phase': 'prepared', 'receipt': None}
        path = store.root / 'databank-actions' / f'{mutation_id}.json'
        store._atomic_write(path, json.dumps(journal).encode())
        descriptor = {'mutation_id': mutation_id, 'journal_sha256': sha256(path.read_bytes()).hexdigest(), 'native_disposition': 'not_submitted'}
        return entity, journal, path, descriptor, original, prepared

    def test_cancel_prepared_import_without_publishing_candidate_with_or_without_root(self):
        for with_root in (False, True):
            with self.subTest(with_root=with_root), TemporaryDirectory() as tmp:
                store = FileResearchCustodyStore(Path(tmp) / 'product')
                entity, journal, path, descriptor, original, prepared = self.pending_import(store, with_root=with_root)
                original_file = Path(tmp) / 'Original.sqx'; original_file.write_bytes(original)
                staged = store.root / 'databank-imports' / sha256(prepared).hexdigest() / 'Imported.sqx'
                staged.parent.mkdir(parents=True); staged.write_bytes(prepared)
                with self.assertRaises(ResearchCustodyError):
                    preview_candidate_purge(store, entity)
                preview = preview_candidate_purge(store, entity, cancel_import=descriptor)
                self.assertEqual(preview['preview']['entities'], [entity])
                self.assertEqual(len(preview['preview']['revisions']), int(with_root))
                self.assertEqual(preview['preview']['cancel_import']['operation_id'], journal['request']['operation_id'])
                self.assertFalse((store.base / 'candidate-purges').exists())
                self.assertEqual(list_current_candidates(store)['candidates'], [])
                intent = prepare_candidate_purge(store, entity, expected_preview_sha256=preview['intent_id'], cancel_import=descriptor)
                result = finish_candidate_purge(store, entity, intent_id=intent['intent_id'])
                self.assertEqual(result['state'], 'completed')
                self.assertFalse(path.exists()); self.assertFalse(staged.exists())
                self.assertFalse(store._evidence_path(EvidenceRef.from_bytes(prepared)).exists())
                self.assertEqual(original_file.read_bytes(), original)
                self.assertIsNotNone(store.deletion_record(ResearchEntityId.parse(entity)))
                self.assertFalse(store._current_path(ResearchEntityId.parse(entity)).exists())
                self.assertEqual(finish_candidate_purge(store, entity, intent_id=intent['intent_id']), result)

    def test_cancel_import_refuses_submitted_stale_or_mismatched_custody(self):
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            entity, journal, path, descriptor, original, prepared = self.pending_import(store, with_root=True)
            for changed in ({**descriptor, 'journal_sha256': '0' * 64}, {**descriptor, 'native_disposition': 'confirmed_absent'}):
                with self.subTest(), self.assertRaises(ResearchCustodyError):
                    preview_candidate_purge(store, entity, cancel_import=changed)
            for change in ({'phase': 'load_submitted'}, {'prepared_ref': journal['source_ref']},
                           {'candidate_revision': None}, {'output_ref': journal['prepared_ref'], 'output_sha256': sha256(prepared).hexdigest()}):
                store._atomic_write(path, json.dumps({**journal, **change}).encode())
                updated = {**descriptor, 'journal_sha256': sha256(path.read_bytes()).hexdigest()}
                with self.subTest(change=change), self.assertRaises((ResearchCustodyError, ValueError)):
                    preview_candidate_purge(store, entity, cancel_import=updated)
            store._atomic_write(path, json.dumps(journal).encode())
            preview = preview_candidate_purge(store, entity, cancel_import=descriptor)
            store._atomic_write(path, json.dumps({**journal, 'phase': 'load_submitted'}).encode())
            with self.assertRaises(ResearchCustodyError):
                prepare_candidate_purge(store, entity, expected_preview_sha256=preview['intent_id'], cancel_import=descriptor)

    def test_cancel_import_preserves_shared_bytes_and_resumes_interruption(self):
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            entity, journal, path, descriptor, original, prepared = self.pending_import(store, with_root=False)
            other = store.create_entity(ResearchKind.IDEA)
            shared = EvidenceRef.from_bytes(original)
            revision = store.create_revision(other, json.dumps({'original': str(shared)}).encode())
            store.compare_and_set_current(other, expected_revision=None, target_revision=revision.revision)
            preview = preview_candidate_purge(store, entity, cancel_import=descriptor)
            self.assertIn(str(shared), [row['ref'] for row in preview['preview']['shared_artifacts']])
            intent = prepare_candidate_purge(store, entity, expected_preview_sha256=preview['intent_id'], cancel_import=descriptor)
            actual_unlink = Path.unlink
            def interrupted(target, *args, **kwargs):
                if target == store._evidence_path(EvidenceRef.from_bytes(prepared)):
                    raise OSError('fixture interruption')
                return actual_unlink(target, *args, **kwargs)
            with patch.object(Path, 'unlink', interrupted), self.assertRaises(OSError):
                finish_candidate_purge(store, entity, intent_id=intent['intent_id'])
            self.assertIsNotNone(store.deletion_record(ResearchEntityId.parse(entity)))
            self.assertFalse(path.exists())
            done = finish_candidate_purge(FileResearchCustodyStore(tmp), entity, intent_id=intent['intent_id'])
            self.assertEqual(done['state'], 'completed')
            self.assertEqual(store.read_evidence(shared), original)

    def test_cancel_import_keeps_bytes_owned_by_another_pending_import_journal(self):
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            entity, journal, path, descriptor, original, prepared = self.pending_import(store, with_root=False)
            other, _, other_path, other_descriptor, _, _ = self.pending_import(store, with_root=False, operation_id='e' * 32)
            other_bytes = other_path.read_bytes()
            preview = preview_candidate_purge(store, entity, cancel_import=descriptor)
            self.assertEqual(preview['preview']['artifacts'], [])
            self.assertEqual({row['ref'] for row in preview['preview']['shared_artifacts']}, {journal['source_ref'], journal['prepared_ref']})
            intent = prepare_candidate_purge(store, entity, expected_preview_sha256=preview['intent_id'], cancel_import=descriptor)
            done = finish_candidate_purge(store, entity, intent_id=intent['intent_id'])
            self.assertEqual(done['state'], 'completed')
            self.assertEqual(other_path.read_bytes(), other_bytes)
            self.assertEqual(store.read_evidence(EvidenceRef.from_bytes(original)), original)
            self.assertEqual(store.read_evidence(EvidenceRef.from_bytes(prepared)), prepared)
            self.assertEqual(preview_candidate_purge(store, other, cancel_import=other_descriptor)['state'], 'preview')

    def prepare_import(self, store, original=None, token="a" * 64, entity=None, archive="Imported.sqx"):
        from test_sqx_candidate_identity import archive as native_archive
        from tradercockpit.sqx_candidate_identity import stamp_import_candidate_token
        original = native_archive() if original is None else original
        prepared = stamp_import_candidate_token(original, token)
        entity = entity or str(store.create_entity(ResearchKind.CANDIDATE))
        args = dict(candidate_entity_id=entity, project="P", databank="Input", archive=archive,
                    original_bytes=original, prepared_bytes=prepared, token=token)
        return prepare_databank_import_candidate(store, **args), original, prepared, args

    def test_import_root_unpublished_then_exact_native_child_reopens_and_retries(self):
        from test_sqx_candidate_identity import xml_edit
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            root, original, prepared, args = self.prepare_import(store)
            self.assertEqual(list_current_candidates(store)["candidates"], [])
            self.assertEqual(list_databank_memberships(store)["memberships"], [])
            self.assertEqual(prepare_databank_import_candidate(store, **args)["revision"], root["revision"])
            native = xml_edit(prepared, lambda node: node.find("SpecialValuesMap/SettingsMap").remove(node.find("SpecialValuesMap/SettingsMap/Note")))
            published = publish_databank_import_candidate(store, candidate_entity_id=root["entity_id"], prepared_revision=root["revision"], archive_bytes=native)
            self.assertNotEqual(root["revision"], published["revision"])
            self.assertEqual(store.read_revision(ResearchRevisionRef.parse(published["revision"])).parent_revision, ResearchRevisionRef.parse(root["revision"]))
            self.assertEqual(store.read_evidence(EvidenceRef.parse(published["origin"]["original_archive_ref"])), original)
            self.assertEqual(store.read_evidence(EvidenceRef.parse(root["archive_ref"])), prepared)
            reopened = FileResearchCustodyStore(tmp)
            self.assertEqual(read_current_candidate(reopened, root["entity_id"])["archive_sha256"], sha256(native).hexdigest())
            self.assertEqual(read_candidate_memberships(reopened, root["entity_id"])["memberships"][0]["archive_sha256"], sha256(native).hexdigest())
            retry = publish_databank_import_candidate(reopened, candidate_entity_id=root["entity_id"], prepared_revision=root["revision"], archive_bytes=native)
            self.assertTrue(retry["reused"])
            self.assertEqual(retry["membership_revision"], published["membership_revision"])
            self.assertEqual(prepare_databank_import_candidate(reopened, **args)["revision"], root["revision"])

    def test_import_refuses_foreign_identity_transform_output_and_location_collision(self):
        from test_sqx_candidate_identity import edit
        from tradercockpit.sqx_candidate_identity import SqxCandidateIdentityError, stamp_import_candidate_token
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            root, original, prepared, args = self.prepare_import(store)
            for changed in ({**args, "prepared_bytes": original}, {**args, "archive": "Other.sqx"},
                            {**args, "token": "b" * 64, "prepared_bytes": stamp_import_candidate_token(original, "b" * 64)}):
                with self.assertRaises(ResearchCandidateError):
                    prepare_databank_import_candidate(store, **changed)
            for output in (stamp_import_candidate_token(original, "b" * 64), edit(prepared, "lastSettings.xml", lambda data: data.replace(b'pointValue="1"', b'pointValue="2"'))):
                with self.assertRaises(SqxCandidateIdentityError):
                    publish_databank_import_candidate(store, candidate_entity_id=root["entity_id"], prepared_revision=root["revision"], archive_bytes=output)
            other = admit_databank_candidate(store, project="P", databank="Input", archive="Imported.sqx", archive_bytes=original)
            with self.assertRaisesRegex(ResearchCustodyError, "candidate_membership_collision"):
                publish_databank_import_candidate(store, candidate_entity_id=root["entity_id"], prepared_revision=root["revision"], archive_bytes=prepared)
            with self.assertRaises(ResearchCandidateError):
                publish_databank_import_candidate(store, candidate_entity_id=other["entity_id"], prepared_revision=root["revision"], archive_bytes=prepared)
            self.assertIsNone(store._read_current(ResearchEntityId.parse(root["entity_id"])))

    def test_import_allows_only_selected_filename_label_then_reconciliation_remains_strict(self):
        from test_sqx_candidate_identity import xml_edit
        from tradercockpit.sqx_candidate_identity import SqxCandidateIdentityError
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            root, _, prepared, _ = self.prepare_import(store, archive="Token carrier.sqx")
            args = dict(candidate_entity_id=root["entity_id"], prepared_revision=root["revision"])
            wrong = xml_edit(prepared, lambda node: node.set("ResultName", "Another filename"))
            with self.assertRaises(SqxCandidateIdentityError):
                publish_databank_import_candidate(store, **args, archive_bytes=wrong)
            self.assertEqual(list_current_candidates(store)["candidates"], [])
            native = xml_edit(prepared, lambda node: node.set("ResultName", "Token carrier"))
            candidate = publish_databank_import_candidate(store, **args, archive_bytes=native)
            self.assertEqual(candidate["archive_sha256"], sha256(native).hexdigest())
            self.assertEqual(store.read_evidence(EvidenceRef.parse(root["archive_ref"])), prepared)
            self.assertTrue(publish_databank_import_candidate(store, **args, archive_bytes=native)["reused"])
            location = dict(project="P", databank="Input", archive="Token carrier.sqx")
            with self.assertRaises(SqxCandidateIdentityError):
                record_databank_membership_operation(store, action="reserialize", candidate_entity_id=candidate["entity_id"],
                    candidate_revision=candidate["revision"], source={**location, "archive_sha256": candidate["archive_sha256"]},
                    destination=location, archive_bytes=prepared, expected_membership_revision=candidate["membership_revision"])

    def test_exported_marked_archive_is_new_import_with_unknown_history(self):
        from tradercockpit.sqx_candidate_identity import read_candidate_token
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            first, _, prepared, _ = self.prepare_import(store)
            second, original, derivative, _ = self.prepare_import(store, prepared, "b" * 64, archive="New.sqx")
            self.assertNotEqual(first["entity_id"], second["entity_id"])
            self.assertEqual(original, prepared)
            self.assertEqual(read_candidate_token(derivative), "b" * 64)
            self.assertEqual(second["history_status"], "unknown")
            self.assertIsNone(store.read_revision(ResearchRevisionRef.parse(second["revision"])).parent_revision)

    def test_import_publication_recovers_pointer_before_membership_but_never_revives_removal(self):
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            root, _, prepared, _ = self.prepare_import(store)
            args = dict(candidate_entity_id=root["entity_id"], prepared_revision=root["revision"], archive_bytes=prepared)
            with patch("tradercockpit.research_candidate_memberships.record_databank_membership_operation", side_effect=OSError("interrupted")):
                with self.assertRaises(OSError):
                    publish_databank_import_candidate(store, **args)
            self.assertEqual(list_databank_memberships(store)["memberships"], [])
            published = publish_databank_import_candidate(FileResearchCustodyStore(tmp), **args)
            self.assertTrue(published["reused"])
            record_databank_membership_operation(store, action="remove", candidate_entity_id=root["entity_id"], candidate_revision=published["revision"],
                source=dict(project="P", databank="Input", archive="Imported.sqx", archive_sha256=sha256(prepared).hexdigest()),
                expected_membership_revision=published["membership_revision"])
            with self.assertRaisesRegex(ResearchCandidateError, "candidate_import_identity_conflict"):
                publish_databank_import_candidate(store, **args)
            self.assertEqual(list_databank_memberships(store)["memberships"], [])

    def test_reserialize_retains_both_hashes_and_revalidates_history_after_reopen(self):
        from test_sqx_candidate_identity import xml_edit, edit
        from tradercockpit.sqx_candidate_identity import SqxCandidateIdentityError
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            root, _, prepared, _ = self.prepare_import(store)
            candidate = publish_databank_import_candidate(store, candidate_entity_id=root["entity_id"], prepared_revision=root["revision"], archive_bytes=prepared)
            changed = xml_edit(prepared, lambda node: node.find("SpecialValuesMap/SettingsMap").remove(node.find("SpecialValuesMap/SettingsMap/Note")))
            location = dict(project="P", databank="Input", archive="Imported.sqx")
            args = dict(action="reserialize", candidate_entity_id=candidate["entity_id"], candidate_revision=candidate["revision"],
                source={**location, "archive_sha256": sha256(prepared).hexdigest()}, destination=location,
                expected_membership_revision=candidate["membership_revision"], archive_bytes=changed)
            for altered in ({**args, "destination": {**location, "archive": "Else.sqx"}},
                            {**args, "expected_membership_revision": root["revision"]}):
                with self.assertRaises(ResearchCustodyError):
                    record_databank_membership_operation(store, **altered)
            with self.assertRaises(SqxCandidateIdentityError):
                record_databank_membership_operation(store, **{**args, "archive_bytes": edit(changed, "orders.bin", lambda data: data + b"changed")})
            record = record_databank_membership_operation(store, **args)
            reopened = FileResearchCustodyStore(tmp)
            history = read_candidate_memberships(reopened, candidate["entity_id"], history=True)
            self.assertEqual([row["event"]["action"] for row in history["history"]], ["admit", "reserialize"])
            self.assertEqual(history["memberships"][0]["archive_sha256"], sha256(changed).hexdigest())
            envelope = reopened.read_revision(ResearchRevisionRef.parse(record["revision"]))
            self.assertEqual(set(envelope.evidence), {EvidenceRef.from_bytes(prepared), EvidenceRef.from_bytes(changed)})
            self.assertTrue(record_databank_membership_operation(reopened, **args)["reused"])
            with self.assertRaisesRegex(ResearchCustodyError, "current_conflict"):
                record_databank_membership_operation(reopened, **{**args, "expected_membership_revision": record["revision"]})
            # A direct revision writer cannot publish an unchecked reserialization.
            payload = json.loads(reopened.read_revision_content(envelope.revision))
            bad = edit(changed, "orders.bin", lambda data: data + b"changed")
            bad_ref = reopened.put_evidence(bad)
            payload["memberships"][0].update(archive_ref=str(bad_ref), archive_sha256=bad_ref.digest)
            payload["event"]["destination"]["archive_sha256"] = bad_ref.digest
            forged = reopened.create_revision(envelope.entity_id, json.dumps(payload).encode(), parent_revision=envelope.parent_revision,
                evidence=(EvidenceRef.from_bytes(prepared), bad_ref))
            reopened.compare_and_set_current(envelope.entity_id, expected_revision=envelope.revision, target_revision=forged.revision)
            with self.assertRaises(SqxCandidateIdentityError):
                read_candidate_memberships(reopened, candidate["entity_id"])

    def test_completed_mutation_journals_are_owned_purged_and_counted_for_every_action(self):
        for action in ("rename", "copy", "move", "remove"):
            with self.subTest(action=action), TemporaryDirectory() as tmp:
                store = FileResearchCustodyStore(tmp)
                raw = archive_bytes()
                candidate = admit_databank_candidate(store, project="P", databank="Input", archive="A.sqx", archive_bytes=raw)
                source = dict(project="P", databank="Input", archive="A.sqx", archive_sha256=sha256(raw).hexdigest())
                destination = (dict(project="P", databank="Input", archive="B.sqx") if action == "rename" else dict(project="P", databank="Output", archive="A.sqx")) if action != "remove" else None
                output = archive_bytes("Renamed") if destination else None
                member = record_databank_membership_operation(store, action=action, candidate_entity_id=candidate["entity_id"], candidate_revision=candidate["revision"], source=source,
                    destination=destination, archive_bytes=output, expected_membership_revision=candidate["membership_revision"])
                journal = mutation_journal(store, candidate, action, source, destination, output)
                preview = preview_candidate_purge(store, candidate["entity_id"])
                self.assertEqual(preview["preview"]["shared_artifacts"], [], "Own journals must not make Candidate evidence permanently shared")
                self.assertEqual(preview["preview"]["mutation_journals"][0]["bytes"], journal.stat().st_size)
                intent = prepare_candidate_purge(store, candidate["entity_id"], expected_preview_sha256=preview["intent_id"])
                journals = [journal]
                for row in list(member["memberships"]):
                    source = {key: row[key] for key in ("project", "databank", "archive", "archive_sha256")}
                    before = member["revision"]
                    member = record_databank_membership_operation(store, action="remove", candidate_entity_id=candidate["entity_id"], candidate_revision=candidate["revision"], source=source, expected_membership_revision=before)
                    # Confirm may create these exact, previewed removal receipts.
                    journals.append(mutation_journal(store, {**candidate, "membership_revision": before}, "remove", source))
                expected_bytes = sum(path.stat().st_size for path in journals)
                final = finish_candidate_purge(store, candidate["entity_id"], intent_id=intent["intent_id"])
                self.assertTrue(all(not path.exists() for path in journals))
                self.assertEqual(final["reclaimed_mutation_journal_bytes"], expected_bytes)
                self.assertEqual(final["reclaimed_bytes"], sum(row["bytes"] for row in final["reclaimed_files"]))
                with self.assertRaises(ResearchCustodyError):
                    store.read_evidence(EvidenceRef.parse(candidate["archive_ref"]))

    def test_completed_import_journal_owns_original_derivative_and_native_child(self):
        from test_sqx_candidate_identity import xml_edit
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            root, original, prepared, _ = self.prepare_import(store)
            native = xml_edit(prepared, lambda node: node.find("SpecialValuesMap/SettingsMap").remove(node.find("SpecialValuesMap/SettingsMap/Note")))
            candidate = publish_databank_import_candidate(store, candidate_entity_id=root["entity_id"], prepared_revision=root["revision"], archive_bytes=native)
            location = dict(project="P", databank="Input", archive="Imported.sqx")
            request = {**location, "source_sha256": sha256(original).hexdigest(), "operation_id": "d" * 32}
            identity = dict(action="load", request=request, runtime_home="fixture-runtime")
            mutation_id = sha256(json.dumps(identity, sort_keys=True, separators=(",", ":")).encode()).hexdigest()
            journal = {"schema": "tc.sqx-databank-mutation.v1", **identity, "mutation_id": mutation_id,
                "candidate_entity_id": candidate["entity_id"], "candidate_revision": candidate["revision"],
                "membership_revision": candidate["membership_revision"], "source": {**location, "archive_sha256": request["source_sha256"]},
                "destination": location, "source_ref": root["origin"]["original_archive_ref"],
                "prepared_ref": root["archive_ref"], "prepared_revision": root["revision"], "candidate_token": "a" * 64,
                "output_ref": candidate["archive_ref"], "output_sha256": candidate["archive_sha256"], "phase": "completed", "receipt": {}}
            path = store.root / "databank-actions" / f"{mutation_id}.json"
            store._atomic_write(path, json.dumps(journal).encode())
            preview = preview_candidate_purge(store, candidate["entity_id"])
            self.assertEqual(preview["preview"]["shared_artifacts"], [])
            self.assertEqual(preview["preview"]["mutation_journals"][0]["action"], "load")
            for bad in ({**journal, "phase": "saved"}, {**journal, "prepared_revision": candidate["revision"]}):
                store._atomic_write(path, json.dumps(bad).encode())
                with self.assertRaises(ResearchCustodyError):
                    preview_candidate_purge(store, candidate["entity_id"])
            store._atomic_write(path, json.dumps(journal).encode())
            intent = prepare_candidate_purge(store, candidate["entity_id"], expected_preview_sha256=preview["intent_id"])
            record_databank_membership_operation(store, action="remove", candidate_entity_id=candidate["entity_id"], candidate_revision=candidate["revision"],
                source={**location, "archive_sha256": candidate["archive_sha256"]}, expected_membership_revision=candidate["membership_revision"])
            result = finish_candidate_purge(store, candidate["entity_id"], intent_id=intent["intent_id"])
            self.assertFalse(path.exists())
            self.assertGreater(result["reclaimed_mutation_journal_bytes"], 0)
            for raw in (original, prepared, native):
                self.assertFalse(store._evidence_path(EvidenceRef.from_bytes(raw)).exists())

    def test_pending_mutation_blocks_pure_preview_and_wrong_candidate_binding_is_refused(self):
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            raw = archive_bytes()
            candidate = admit_databank_candidate(store, project="P", databank="Input", archive="A.sqx", archive_bytes=raw)
            source = dict(project="P", databank="Input", archive="A.sqx", archive_sha256=sha256(raw).hexdigest())
            for phase in ("prepared", "rename_submitted", "renamed", "load_submitted", "loaded", "save_submitted", "saved", "copied", "remove_submitted", "source_removed"):
                journal = mutation_journal(store, candidate, "remove", source, phase=phase)
                with self.assertRaises(ResearchCustodyError) as caught:
                    preview_candidate_purge(store, candidate["entity_id"])
                self.assertEqual(caught.exception.code, "candidate_mutation_pending")
                self.assertEqual(list((store.base / "candidate-purges").glob("*.json")), [])
            other = admit_databank_candidate(store, project="Other", databank="Input", archive="A.sqx", archive_bytes=raw)
            mutation_journal(store, candidate, "remove", source)
            payload = json.loads(journal.read_bytes())
            payload["candidate_revision"] = other["revision"]
            journal.write_text(json.dumps(payload))
            with self.assertRaises(ResearchCustodyError) as caught:
                preview_candidate_purge(store, candidate["entity_id"])
            self.assertEqual(caught.exception.code, "candidate_mutation_corrupt")

    def test_journal_change_refuses_and_unlink_interruption_is_uncertain_on_resume(self):
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            raw = archive_bytes()
            candidate = admit_databank_candidate(store, project="P", databank="Input", archive="A.sqx", archive_bytes=raw)
            source = dict(project="P", databank="Input", archive="A.sqx", archive_sha256=sha256(raw).hexdigest())
            record_databank_membership_operation(store, action="remove", candidate_entity_id=candidate["entity_id"], candidate_revision=candidate["revision"], source=source, expected_membership_revision=candidate["membership_revision"])
            journal = mutation_journal(store, candidate, "remove", source)
            original = journal.read_bytes()
            preview = preview_candidate_purge(store, candidate["entity_id"])
            intent = prepare_candidate_purge(store, candidate["entity_id"], expected_preview_sha256=preview["intent_id"])
            journal.write_bytes(original + b"\n")
            with self.assertRaises(ResearchCustodyError) as caught:
                finish_candidate_purge(store, candidate["entity_id"], intent_id=intent["intent_id"])
            self.assertEqual(caught.exception.code, "candidate_purge_preview_changed")
            self.assertEqual(store.read_evidence(EvidenceRef.parse(candidate["archive_ref"])), raw)
            journal.write_bytes(original)
            actual_unlink = Path.unlink
            def interrupted(path, *args, **kwargs):
                result = actual_unlink(path, *args, **kwargs)
                if path == journal:
                    raise OSError("interrupted after journal unlink before progress write")
                return result
            with patch.object(Path, "unlink", interrupted), self.assertRaises(OSError):
                finish_candidate_purge(store, candidate["entity_id"], intent_id=intent["intent_id"])
            final = finish_candidate_purge(FileResearchCustodyStore(tmp), candidate["entity_id"], intent_id=intent["intent_id"])
            self.assertEqual(final["state"], "completed")
            self.assertEqual(final["reclaimed_mutation_journal_bytes"], 0)
            self.assertIn(f"mutation_journal:{journal.relative_to(store.root).as_posix()}", final["reclamation_uncertain_paths"])
            with self.assertRaises(ResearchCustodyError):
                store.read_evidence(EvidenceRef.parse(candidate["archive_ref"]))

    def test_other_candidate_journal_and_shared_bytes_survive_candidate_purge(self):
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            raw = archive_bytes()
            candidates = [admit_databank_candidate(store, project=project, databank="Input", archive="A.sqx", archive_bytes=raw) for project in ("P", "Other")]
            journals = []
            for project, candidate in zip(("P", "Other"), candidates):
                source = dict(project=project, databank="Input", archive="A.sqx", archive_sha256=sha256(raw).hexdigest())
                record_databank_membership_operation(store, action="remove", candidate_entity_id=candidate["entity_id"], candidate_revision=candidate["revision"], source=source, expected_membership_revision=candidate["membership_revision"])
                journals.append(mutation_journal(store, candidate, "remove", source))
            candidate = candidates[0]
            preview = preview_candidate_purge(store, candidate["entity_id"])
            self.assertIn(candidate["archive_ref"], [row["ref"] for row in preview["preview"]["shared_artifacts"]])
            intent = prepare_candidate_purge(store, candidate["entity_id"], expected_preview_sha256=preview["intent_id"])
            finish_candidate_purge(store, candidate["entity_id"], intent_id=intent["intent_id"])
            self.assertFalse(journals[0].exists())
            self.assertTrue(journals[1].exists())
            self.assertEqual(store.read_evidence(EvidenceRef.parse(candidate["archive_ref"])), raw)
            self.assertEqual(read_current_candidate(store, candidates[1]["entity_id"])["revision"], candidates[1]["revision"])

    def test_original_import_and_native_readback_have_distinct_evidence_without_fake_job(self):
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            original, native = archive_bytes(build="143.1"), archive_bytes("Migrated")
            kwargs = dict(project="My project", databank="Results", archive="Migrated.sqx", archive_bytes=native, origin_kind="user_import", original_bytes=original)
            admitted = admit_databank_candidate(store, **kwargs)
            reopened = read_current_candidate(FileResearchCustodyStore(tmp), admitted["entity_id"])
            self.assertEqual(reopened["origin"]["kind"], "user_import")
            self.assertEqual(reopened["history_status"], "unknown")
            self.assertIsNone(reopened["native_job_revision"])
            self.assertIsNone(reopened["configuration_revision"])
            self.assertEqual(store.read_evidence(EvidenceRef.parse(reopened["origin"]["original_archive_ref"])), original)
            self.assertEqual(store.read_evidence(EvidenceRef.parse(reopened["archive_ref"])), native)
            self.assertEqual(admit_databank_candidate(store, **kwargs)["entity_id"], admitted["entity_id"])
            stored = json.loads(store.read_revision_content(ResearchRevisionRef.parse(admitted["revision"])))
            self.assertEqual(stored["schema"], "tc.research-candidate-content.v2")
            stored["native_job_revision"] = "tc-research-revision:native-job:sha256:" + "a" * 64
            with self.assertRaises(ValueError):
                CandidateContent.from_bytes(json.dumps(stored).encode())

    def test_rename_copy_move_remove_keep_candidate_and_original_revision_and_history(self):
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            raw = archive_bytes()
            candidate = admit_databank_candidate(store, project="Pipeline", databank="Input", archive="Original.sqx", archive_bytes=raw)
            source = dict(project="Pipeline", databank="Input", archive="Original.sqx", archive_sha256=sha256(raw).hexdigest())
            renamed_bytes = archive_bytes("Renamed")
            renamed = record_databank_membership_operation(store, action="rename", candidate_entity_id=candidate["entity_id"], candidate_revision=candidate["revision"], source=source,
                destination=dict(project="Pipeline", databank="Input", archive="Renamed.sqx"), archive_bytes=renamed_bytes, expected_membership_revision=candidate["membership_revision"])
            self.assertEqual(read_current_candidate(store, candidate["entity_id"])["revision"], candidate["revision"])
            rediscovered = admit_databank_candidate(store, project="Pipeline", databank="Input", archive="Renamed.sqx", archive_bytes=renamed_bytes)
            self.assertEqual(rediscovered["entity_id"], candidate["entity_id"])
            self.assertEqual(len(list_current_candidates(store)["candidates"]), 1)
            renamed_source = dict(project="Pipeline", databank="Input", archive="Renamed.sqx", archive_sha256=sha256(renamed_bytes).hexdigest())
            args = dict(action="copy", candidate_entity_id=candidate["entity_id"], candidate_revision=candidate["revision"], source=renamed_source,
                destination=dict(project="Pipeline", databank="Stage 2", archive="Renamed.sqx"), archive_bytes=renamed_bytes, expected_membership_revision=renamed["revision"])
            copied = record_databank_membership_operation(store, **args)
            self.assertEqual(len(copied["memberships"]), 2)
            self.assertTrue(record_databank_membership_operation(store, **args)["reused"])
            moved = record_databank_membership_operation(store, **{**args, "action": "move", "expected_membership_revision": copied["revision"], "destination": dict(project="Other project", databank="Archive", archive="Renamed.sqx")})
            removed = record_databank_membership_operation(store, action="remove", candidate_entity_id=candidate["entity_id"], candidate_revision=candidate["revision"], source={**renamed_source, "project": "Other project", "databank": "Archive"}, expected_membership_revision=moved["revision"])
            self.assertEqual(len(removed["memberships"]), 1)
            history = read_candidate_memberships(FileResearchCustodyStore(tmp), candidate["entity_id"], history=True)["history"]
            self.assertEqual([row["event"]["action"] for row in history], ["admit", "rename", "copy", "move", "remove"])
            self.assertEqual(history[0]["memberships"][0]["archive_sha256"], sha256(raw).hexdigest())
            self.assertEqual(store.read_evidence(EvidenceRef.parse(history[1]["memberships"][0]["archive_ref"])), renamed_bytes)

    def test_identical_name_is_not_lineage_changed_bytes_and_stale_source_refused(self):
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            raw = archive_bytes()
            first = admit_databank_candidate(store, project="One", databank="Results", archive="A.sqx", archive_bytes=raw)
            other = admit_databank_candidate(store, project="Two", databank="Results", archive="A.sqx", archive_bytes=raw)
            self.assertNotEqual(first["entity_id"], other["entity_id"])
            with self.assertRaises(ResearchCustodyError):
                admit_databank_candidate(store, project="One", databank="Results", archive="A.sqx", archive_bytes=archive_bytes("changed"))
            with self.assertRaises(ResearchCustodyError):
                record_databank_membership_operation(store, action="remove", candidate_entity_id=first["entity_id"], candidate_revision=first["revision"], source=dict(project="One", databank="Results", archive="A.sqx", archive_sha256="0" * 64), expected_membership_revision=first["membership_revision"])
            self.assertEqual(len(list_databank_memberships(store)["memberships"]), 2)

    def test_concurrent_admission_reopens_one_candidate_and_bad_paths_write_nothing(self):
        with TemporaryDirectory() as tmp:
            raw = archive_bytes()
            args = dict(project="Pipeline", databank="Results", archive="A.sqx", archive_bytes=raw)
            with ThreadPoolExecutor(max_workers=2) as pool:
                results = list(pool.map(lambda _: admit_databank_candidate(FileResearchCustodyStore(tmp), **args), range(2)))
            self.assertEqual(results[0]["entity_id"], results[1]["entity_id"])
            store = FileResearchCustodyStore(tmp)
            self.assertEqual(len(list_current_candidates(store)["candidates"]), 1)
            for bad in ("../Other", "C:\\Other", "CON", "A/B"):
                with self.assertRaises((ValueError, RuntimeError)):
                    admit_databank_candidate(store, **{**args, "project": bad})
            self.assertEqual(len(list_current_candidates(store)["candidates"]), 1)

    def test_purge_owned_results_nested_shared_refs_staging_and_deletion_metadata(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            store = FileResearchCustodyStore(root / "product")
            raw = archive_bytes()
            original = root / "Original.sqx"
            original.write_bytes(raw)
            candidate = admit_databank_candidate(store, project="Pipeline", databank="Results", archive="A.sqx", archive_bytes=raw, origin_kind="user_import", original_bytes=raw)
            staged = store.root / "databank-imports" / sha256(raw).hexdigest() / "A.sqx"
            staged.parent.mkdir(parents=True)
            staged.write_bytes(raw)
            shared = store.put_evidence(b"shared nested statistics bytes")
            exclusive = store.put_evidence(b"solely owned result bytes")
            result = store.create_entity(ResearchKind.HISTORICAL_RESULT)
            result_revision = store.create_revision(result, json.dumps({"candidate_entity_id": candidate["entity_id"], "artifacts": {"shared": str(shared), "exclusive": str(exclusive)}}).encode())
            store.compare_and_set_current(result, expected_revision=None, target_revision=result_revision.revision)
            proof = store.create_entity(ResearchKind.PROOF)
            proof_revision = store.create_revision(proof, json.dumps({"historical_result_revision": str(result_revision.revision)}).encode())
            store.compare_and_set_current(proof, expected_revision=None, target_revision=proof_revision.revision)
            independent = store.create_entity(ResearchKind.IDEA)
            outside = store.create_revision(independent, json.dumps({"manifest": {"artifacts": [str(shared)]}}).encode())
            store.compare_and_set_current(independent, expected_revision=None, target_revision=outside.revision)
            preview = preview_candidate_purge(store, candidate["entity_id"])
            assert_candidate_membership_action(store, candidate["entity_id"], action="rename")
            intent = prepare_candidate_purge(store, candidate["entity_id"], expected_preview_sha256=preview["intent_id"])
            self.assertIn(str(result), intent["preview"]["entities"])
            self.assertIn(str(proof), intent["preview"]["entities"])
            self.assertIn(str(shared), [row["ref"] for row in intent["preview"]["shared_artifacts"]])
            self.assertEqual(intent["preview"]["staging"][0]["bytes"], len(raw))
            with self.assertRaises(ResearchCustodyError) as caught:
                assert_candidate_membership_action(store, candidate["entity_id"], action="rename")
            self.assertEqual(caught.exception.code, "candidate_purge_pending")
            with self.assertRaises(ResearchCustodyError) as caught:
                finish_candidate_purge(store, candidate["entity_id"], intent_id=intent["intent_id"])
            self.assertEqual(caught.exception.code, "candidate_purge_memberships_remain")
            record_databank_membership_operation(store, action="remove", candidate_entity_id=candidate["entity_id"], candidate_revision=candidate["revision"], source=dict(project="Pipeline", databank="Results", archive="A.sqx", archive_sha256=sha256(raw).hexdigest()), expected_membership_revision=candidate["membership_revision"])
            final = finish_candidate_purge(store, candidate["entity_id"], intent_id=intent["intent_id"])
            self.assertEqual(final["state"], "completed")
            self.assertEqual(final["reclaimed_bytes"], sum(row["bytes"] for row in final["reclaimed_files"]))
            self.assertEqual(final["reclaimed_staging_bytes"], len(raw))
            self.assertGreater(final["reclaimed_custody_bytes"], len(raw))
            self.assertEqual(final["reclamation_uncertain_paths"], [])
            self.assertEqual(finish_candidate_purge(store, candidate["entity_id"], intent_id=intent["intent_id"]), final)
            self.assertEqual(store.read_evidence(shared), b"shared nested statistics bytes")
            with self.assertRaises(ResearchCustodyError):
                store.read_evidence(exclusive)
            self.assertEqual(store.read_revision(outside.revision).entity_id, independent)
            self.assertFalse(staged.exists())
            self.assertEqual(original.read_bytes(), raw)
            with self.assertRaises(ResearchCustodyError) as caught:
                read_current_candidate(store, candidate["entity_id"])
            self.assertEqual(caught.exception.code, "entity_deleted")
            self.assertEqual(list_current_candidates(store)["candidates"], [])
            self.assertEqual(list_current_candidates(store)["deleted_candidates"][0]["entity_id"], candidate["entity_id"])

    def test_interrupted_purge_rechecks_new_shared_reference_before_retry(self):
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            raw = archive_bytes()
            candidate = admit_databank_candidate(store, project="P", databank="Results", archive="A.sqx", archive_bytes=raw)
            candidate_archive = EvidenceRef.parse(candidate["archive_ref"])
            preview = preview_candidate_purge(store, candidate["entity_id"])
            assert_candidate_membership_action(store, candidate["entity_id"], action="rename")
            intent = prepare_candidate_purge(store, candidate["entity_id"], expected_preview_sha256=preview["intent_id"])
            record_databank_membership_operation(store, action="remove", candidate_entity_id=candidate["entity_id"], candidate_revision=candidate["revision"], source=dict(project="P", databank="Results", archive="A.sqx", archive_sha256=sha256(raw).hexdigest()), expected_membership_revision=candidate["membership_revision"])
            actual_unlink = Path.unlink
            def interrupted(path, *args, **kwargs):
                if path == store._evidence_path(candidate_archive):
                    raise OSError("simulated process interruption before archive deletion")
                return actual_unlink(path, *args, **kwargs)
            with patch.object(Path, "unlink", interrupted):
                with self.assertRaises(OSError):
                    finish_candidate_purge(store, candidate["entity_id"], intent_id=intent["intent_id"])
            other = store.create_entity(ResearchKind.IDEA)
            revision = store.create_revision(other, json.dumps({"nested": {"archive": str(candidate_archive)}}).encode())
            store.compare_and_set_current(other, expected_revision=None, target_revision=revision.revision)
            completed = finish_candidate_purge(FileResearchCustodyStore(tmp), candidate["entity_id"], intent_id=intent["intent_id"])
            self.assertEqual(completed["state"], "completed")
            self.assertEqual(store.read_evidence(candidate_archive), raw)
            self.assertEqual(store.read_revision(revision.revision).entity_id, other)

    def test_preview_does_not_freeze_and_prepare_refuses_changed_memberships(self):
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            raw = archive_bytes()
            candidate = admit_databank_candidate(store, project="P", databank="Input", archive="A.sqx", archive_bytes=raw)
            preview = preview_candidate_purge(store, candidate["entity_id"])
            copied = record_databank_membership_operation(store, action="copy", candidate_entity_id=candidate["entity_id"], candidate_revision=candidate["revision"], source=dict(project="P", databank="Input", archive="A.sqx", archive_sha256=sha256(raw).hexdigest()), destination=dict(project="P", databank="Copy", archive="A.sqx"), archive_bytes=raw, expected_membership_revision=candidate["membership_revision"])
            self.assertEqual(len(copied["memberships"]), 2)
            with self.assertRaises(ResearchCustodyError) as caught:
                prepare_candidate_purge(store, candidate["entity_id"], expected_preview_sha256=preview["intent_id"])
            self.assertEqual(caught.exception.code, "candidate_purge_preview_changed")
            assert_candidate_membership_action(store, candidate["entity_id"], action="rename")
            updated = preview_candidate_purge(store, candidate["entity_id"])
            self.assertNotEqual(updated["intent_id"], preview["intent_id"])
            self.assertTrue(updated["preview"]["artifacts"][0]["owners"])

    def test_results_association_requires_exact_location_hash_and_inspectable_native_row(self):
        from tradercockpit.sqx_custom_project import SQX_CUSTOM_PROJECT_RESULTS_SCHEMA
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            raw = archive_bytes()
            candidate = admit_databank_candidate(store, project="P", databank="Input", archive="A.sqx", archive_bytes=raw)
            row = {"archive": "A.sqx", "archive_sha256": sha256(raw).hexdigest(), "inspectable": True}
            payload = {"schema": SQX_CUSTOM_PROJECT_RESULTS_SCHEMA, "project": "P", "projects": [{"name": "P", "databanks": [{"name": "Input", "strategies": [row, {**row, "archive": "Other.sqx"}, {**row, "archive_sha256": "a" * 64}, {**row, "inspectable": False}]}, {"name": "Other bank", "strategies": [row]}]}]}
            annotated = associate_databank_results(store, payload)
            rows = annotated["projects"][0]["databanks"][0]["strategies"]
            self.assertEqual(rows[0]["candidate_association"]["candidate_entity_id"], candidate["entity_id"])
            self.assertEqual(rows[0]["candidate_association"]["membership_revision"], candidate["membership_revision"])
            self.assertTrue(all(row["candidate_association"] is None for row in rows[1:]))
            self.assertEqual(rows[2]["candidate_reconciliation"], {
                "schema": "tc.research-native-candidate-reconciliation.v1", "candidate_entity_id": candidate["entity_id"],
                "candidate_revision": candidate["revision"], "membership_revision": candidate["membership_revision"],
                "previous_archive_sha256": sha256(raw).hexdigest(), "archive_sha256": "a" * 64,
                "unavailable_reason": "candidate_token_invalid"})
            self.assertTrue(all(rows[index]["candidate_reconciliation"] is None for index in (0, 1, 3)))
            self.assertEqual(read_candidate_memberships(store, candidate["entity_id"])["revision"], candidate["membership_revision"], "GET annotation must not mutate custody")
            self.assertIsNone(annotated["projects"][0]["databanks"][1]["strategies"][0]["candidate_association"])
            self.assertNotIn("candidate_association", payload["projects"][0]["databanks"][0]["strategies"][0])

    def test_unmarked_legacy_reopen_refuses_reconnection_without_changing_custody(self):
        from test_sqx_candidate_identity import archive, xml_edit
        from tradercockpit.sqx_candidate_identity import SqxCandidateIdentityError
        from tradercockpit.sqx_custom_project import SQX_CUSTOM_PROJECT_RESULTS_SCHEMA
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            original = archive()
            current = xml_edit(original, lambda root: root.find("SpecialValuesMap/SettingsMap").remove(root.find("SpecialValuesMap/SettingsMap/Note")))
            candidate = admit_databank_candidate(store, project="P", databank="Input", archive="Legacy.sqx", archive_bytes=original)
            source = dict(project="P", databank="Input", archive="Legacy.sqx", archive_sha256=sha256(original).hexdigest())
            row = dict(archive="Legacy.sqx", archive_sha256=sha256(current).hexdigest(), inspectable=True)
            payload = dict(schema=SQX_CUSTOM_PROJECT_RESULTS_SCHEMA, project="P", projects=[dict(name="P", databanks=[dict(name="Input", strategies=[row])])])
            before = {p.relative_to(store.root): p.read_bytes() for p in store.root.rglob("*") if p.is_file()}
            for reopened in (store, FileResearchCustodyStore(tmp)):
                result = associate_databank_results(reopened, payload)["projects"][0]["databanks"][0]["strategies"][0]
                self.assertIsNone(result["candidate_association"])
                self.assertEqual(result["candidate_reconciliation"]["unavailable_reason"], "candidate_legacy_reimport_required")
                with self.assertRaises(SqxCandidateIdentityError) as caught:
                    record_databank_membership_operation(reopened, action="reserialize", candidate_entity_id=candidate["entity_id"],
                        candidate_revision=candidate["revision"], source=source, destination={k: source[k] for k in ("project", "databank", "archive")},
                        archive_bytes=current, expected_membership_revision=candidate["membership_revision"])
                self.assertEqual(caught.exception.code, "candidate_legacy_reimport_required")
            self.assertEqual(before, {p.relative_to(store.root): p.read_bytes() for p in store.root.rglob("*") if p.is_file()})

    def test_unlink_before_journal_failure_is_uncertain_not_credited_on_resume(self):
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            raw = archive_bytes()
            candidate = admit_databank_candidate(store, project="P", databank="Results", archive="A.sqx", archive_bytes=raw)
            preview = preview_candidate_purge(store, candidate["entity_id"])
            intent = prepare_candidate_purge(store, candidate["entity_id"], expected_preview_sha256=preview["intent_id"])
            record_databank_membership_operation(store, action="remove", candidate_entity_id=candidate["entity_id"], candidate_revision=candidate["revision"], source=dict(project="P", databank="Results", archive="A.sqx", archive_sha256=sha256(raw).hexdigest()), expected_membership_revision=candidate["membership_revision"])
            actual_write = store._atomic_write
            interrupted_path = []
            def interrupted(path, data):
                if path.parent.name == "candidate-purges":
                    content = json.loads(data)
                    if content.get("reclaimed_files"):
                        interrupted_path.append(content["reclaimed_files"][-1])
                        raise OSError("simulated journal interruption after unlink")
                return actual_write(path, data)
            with patch.object(store, "_atomic_write", interrupted):
                with self.assertRaises(OSError):
                    finish_candidate_purge(store, candidate["entity_id"], intent_id=intent["intent_id"])
            final = finish_candidate_purge(store, candidate["entity_id"], intent_id=intent["intent_id"])
            missing = interrupted_path[0]
            self.assertIn(f"{missing['scope']}:{missing['path']}", final["reclamation_uncertain_paths"])
            self.assertNotIn(missing, final["reclaimed_files"])
            self.assertEqual(final["reclaimed_bytes"], sum(item["bytes"] for item in final["reclaimed_files"]))

    def test_batch_verifies_catalog_once_and_cannot_be_reused_outside_its_operation(self):
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            raw = archive_bytes()
            with patch("tradercockpit.research_candidate_memberships.list_databank_memberships", wraps=list_databank_memberships) as catalog:
                with candidate_admission_batch(store) as inventory:
                    admitted = [admit_databank_candidate(store, project="P", databank="Results", archive=f"A{index}.sqx", archive_bytes=raw, admission_inventory=inventory) for index in range(12)]
                    retried = admit_databank_candidate(store, project="P", databank="Results", archive="A0.sqx", archive_bytes=raw, admission_inventory=inventory)
                self.assertEqual(catalog.call_count, 1)
            self.assertEqual(retried["entity_id"], admitted[0]["entity_id"])
            self.assertEqual(len(list_databank_memberships(FileResearchCustodyStore(tmp))["memberships"]), 12)
            with self.assertRaises(ResearchCustodyError) as caught:
                admit_databank_candidate(store, project="P", databank="Results", archive="A0.sqx", archive_bytes=raw, admission_inventory=inventory)
            self.assertEqual(caught.exception.code, "candidate_admission_batch_invalid")


if __name__ == "__main__":
    unittest.main()
