from hashlib import sha256
from pathlib import Path
from tempfile import TemporaryDirectory
from zipfile import ZipFile
import json
from uuid import uuid4
import unittest

from tradercockpit.research_project_reviews import project_review_response
from tradercockpit.research_custody import FileResearchCustodyStore, ResearchEntityId, ResearchKind, EvidenceRef
from tradercockpit.research_candidates import admit_databank_candidate, _canonical
from tradercockpit.research_candidate_memberships import (
    preview_candidate_purge, prepare_candidate_purge, finish_candidate_purge,
    record_databank_membership_operation,
    read_candidate_memberships,
)
from test_research_candidate_memberships import archive_bytes


class ProjectReviewTests(unittest.TestCase):
    def setUp(self):
        self.temp = TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        root = Path(self.temp.name)
        self.store = FileResearchCustodyStore(root / "custody")
        self.home = root / "runtime"
        (self.home / "internal/web/SQUANT").mkdir(parents=True)
        (self.home / "internal/web/SQUANT/build.dat").write_text("2953")
        (self.home / "internal/SQUANT.dat").write_bytes(b"144fixture")
        (self.home / "sqcli.exe").write_bytes(b"trusted-fixture")
        self.pin = sha256(b"trusted-fixture").hexdigest()
        self.bank = self.home / "user/projects/P/databanks/Results"
        self.bank.mkdir(parents=True)
        self.graph = self.bank.parent.parent / "project.cfx"
        self.write_graph()

    def write_graph(self, title="Retest"):
        with ZipFile(self.graph, "w") as archive:
            archive.writestr("config.xml", f'<Project><Tasks><Task type="Retest" title="{title}" active="true" taskXMLFile="Retest-Task1.xml"/></Tasks></Project>')
            archive.writestr("Retest-Task1.xml", "<Task/>")

    def call(self, action, **kwargs):
        return project_review_response(self.store, self.home, {"action": action, "project": "P", "databank": "Results", **kwargs}, trusted_launcher_sha256=self.pin)

    def admit(self, name="A.sqx", bank="Results"):
        raw = archive_bytes(name)
        path = self.bank.parent / bank
        path.mkdir(exist_ok=True)
        (path / name).write_bytes(raw)
        return admit_databank_candidate(self.store, project="P", databank=bank, archive=name, archive_bytes=raw)

    def retain(self):
        status, preview = self.call("preview")
        self.assertEqual(status, 200, preview)
        status, saved = self.call("retain", expected_review_sha256=preview["review_sha256"])
        self.assertEqual(status, 200, saved)
        return saved

    def test_exact_review_is_idempotent_reopens_without_runtime_and_never_authorizes_execution(self):
        self.admit()
        before = {p: p.read_bytes() for p in self.home.rglob("*") if p.is_file()}
        saved = self.retain()
        self.assertFalse(saved["snapshot"]["launch_authorized"])
        self.assertEqual(saved["snapshot"]["inputs"][0]["binding"], "exact")
        self.assertEqual(self.retain(), saved)
        store = FileResearchCustodyStore(self.store.root)
        status, listed = project_review_response(store, None, {"action": "list", "project": "P", "databank": "Results"})
        self.assertEqual(status, 200)
        self.assertEqual(listed["reviews"], [saved])
        self.assertEqual(before, {p: p.read_bytes() for p in self.home.rglob("*") if p.is_file()})

    def test_changes_to_graph_and_input_invalidate_preview(self):
        self.admit()
        for mutate in (lambda: self.write_graph("Changed"), lambda: (self.bank / "A.sqx").write_bytes(archive_bytes("different"))):
            _, preview = self.call("preview")
            mutate()
            status, response = self.call("retain", expected_review_sha256=preview["review_sha256"])
            self.assertEqual(status, 409)
            self.assertEqual(response["reason_code"], "project_review_changed")

    def test_unadmitted_and_changed_inputs_are_not_associated_by_name(self):
        self.admit()
        (self.bank / "A.sqx").write_bytes(archive_bytes("changed"))
        (self.bank / "B.sqx").write_bytes(archive_bytes("B"))
        saved = self.retain()
        self.assertEqual([row["binding"] for row in saved["snapshot"]["inputs"]], ["changed", "unadmitted"])
        self.assertTrue(all(row["candidate_entity_id"] is None for row in saved["snapshot"]["inputs"]))

    def test_launcher_mismatch_and_linked_inputs_refuse(self):
        (self.home / "sqcli.exe").write_bytes(b"changed")
        self.assertEqual(self.call("preview")[1]["reason_code"], "sqx_launcher_hash_mismatch")
        (self.home / "sqcli.exe").write_bytes(b"trusted-fixture")
        self.admit()
        (self.bank / "B.sqx").hardlink_to(self.bank / "A.sqx")
        self.assertEqual(self.call("preview")[1]["reason_code"], "project_review_input_invalid")

    def test_empty_bank_and_strict_request_fields(self):
        self.assertEqual(self.retain()["snapshot"]["inputs"], [])
        self.assertEqual(self.call("preview", launch=True)[0], 400)
        self.assertEqual(self.call("approve")[0], 400)
        self.assertEqual(self.call("retain", expected_review_sha256="bad")[0], 409)

    def write_workflow(self):
        with ZipFile(self.graph, "w") as archive:
            archive.writestr("config.xml", '<Project><Databanks><Databank name="Results"/><Databank name="Final"/></Databanks><Tasks><Task type="Filtering" title="Filter" active="true" taskXMLFile="Filtering-Task1.xml"/><Task type="ClearDatabanks" title="Clear" active="false" taskXMLFile="ClearDatabanks-Task2.xml"/></Tasks></Project>')
            archive.writestr("Filtering-Task1.xml", '<Task><Databanks><Databank name="Source" value="Results"/><Databank name="Target" value="Final"/></Databanks></Task>')
            archive.writestr("ClearDatabanks-Task2.xml", '<Task><ClearDatabanks><Databank name="Scratch"/></ClearDatabanks></Task>')

    def test_review_includes_all_project_banks_and_native_task_bindings(self):
        self.write_workflow()
        self.admit()
        final = self.bank.parent / "Final"
        final.mkdir()
        raw = archive_bytes("survivor")
        (final / "A.sqx").write_bytes(raw)
        candidate = admit_databank_candidate(self.store, project="P", databank="Final", archive="A.sqx", archive_bytes=raw)
        # A same-named bank in another project must not enter this review.
        outside = self.home / "user/projects/Other/databanks/Final"
        outside.mkdir(parents=True)
        (outside / "ignored.sqx").write_bytes(raw)
        saved = self.retain()
        snapshot = saved["snapshot"]
        self.assertEqual(snapshot["scope"], "saved_graph_and_project_banks")
        self.assertEqual([bank["name"] for bank in snapshot["banks"]], ["Final", "Results", "Scratch"])
        self.assertEqual(snapshot["banks"][0]["inputs"][0]["candidate_entity_id"], candidate["entity_id"])
        self.assertEqual(snapshot["banks"][2], {"name": "Scratch", "declared": False, "storage": "not_created", "inputs": []})
        self.assertEqual(snapshot["tasks"][0]["banks"], [{"role": "Source", "databank": "Results"}, {"role": "Target", "databank": "Final"}])
        self.assertEqual(snapshot["tasks"][1]["banks"], [{"role": "ClearDatabanks", "databank": "Scratch"}])
        self.assertFalse(snapshot["tasks"][1]["active"])
        self.assertEqual(snapshot["inputs"], snapshot["banks"][1]["inputs"])
        self.assertEqual(self.call("list")[1]["reviews"], [saved])

    def test_other_bank_mutations_invalidate_preview_and_uncreated_bank_is_reviewable(self):
        self.write_workflow()
        final = self.bank.parent / "Final"
        for mutate in (lambda: final.mkdir(),
                       lambda: (final / "A.sqx").write_bytes(archive_bytes("A")),
                       lambda: (final / "A.sqx").write_bytes(archive_bytes("B")),
                       lambda: (final / "A.sqx").unlink(),
                       lambda: (self.bank.parent / "Extra").mkdir()):
            _, preview = self.call("preview")
            mutate()
            status, response = self.call("retain", expected_review_sha256=preview["review_sha256"])
            self.assertEqual((status, response["reason_code"]), (409, "project_review_changed"))
        status, preview = project_review_response(self.store, self.home, {"action": "preview", "project": "P", "databank": "Scratch"}, trusted_launcher_sha256=self.pin)
        self.assertEqual(status, 200, preview)
        self.assertEqual(preview["snapshot"]["inputs"], [])
        self.assertFalse((self.bank.parent / "Scratch").exists())

    def test_other_bank_linked_archive_or_invalid_storage_refuses_review(self):
        self.write_workflow()
        final = self.bank.parent / "Final"
        final.write_bytes(b"not a bank directory")
        self.assertEqual(self.call("preview")[1]["reason_code"], "project_review_input_invalid")
        final.unlink()
        final.mkdir()
        self.admit()
        (final / "linked.sqx").hardlink_to(self.bank / "A.sqx")
        self.assertEqual(self.call("preview")[1]["reason_code"], "project_review_input_invalid")

    def test_native_bank_case_alias_does_not_duplicate_windows_inventory(self):
        self.admit()
        with ZipFile(self.graph, "w") as archive:
            archive.writestr("config.xml", '<Project><Databanks><Databank name="Results"/></Databanks><Tasks><Task type="Retest" active="true" taskXMLFile="Retest-Task1.xml"/></Tasks></Project>')
            archive.writestr("Retest-Task1.xml", '<Task><Databanks><Databank name="Input" value="results"/></Databanks></Task>')
        snapshot = self.retain()["snapshot"]
        self.assertEqual(sum(len(bank["inputs"]) for bank in snapshot["banks"]), 1)
        self.assertEqual(snapshot["tasks"][0]["banks"][0]["databank"], "results")
        status, preview = project_review_response(self.store, self.home, {"action": "preview", "project": "P", "databank": "results"}, trusted_launcher_sha256=self.pin)
        self.assertEqual(status, 200)
        if self.bank == self.bank.with_name("results"):
            self.assertEqual(len(snapshot["banks"]), 1)
            self.assertEqual(preview["snapshot"]["inputs"][0]["binding"], "exact")
        else:
            self.assertEqual(len(snapshot["banks"]), 2)
            self.assertEqual(preview["snapshot"]["inputs"], [])

    def test_candidate_delete_reclaims_exclusive_review_and_retains_shared_review(self):
        first, second = self.admit(), self.admit("B.sqx", bank="Final")
        saved = self.retain()
        for candidate, archive, bank in ((first, "A.sqx", "Results"), (second, "B.sqx", "Final")):
            identifier = candidate["entity_id"]
            record_databank_membership_operation(self.store, action="remove", candidate_entity_id=identifier,
                expected_membership_revision=read_candidate_memberships(self.store, identifier)["revision"],
                candidate_revision=candidate["revision"], source={"project": "P", "databank": bank, "archive": archive,
                "archive_sha256": sha256((self.bank.parent / bank / archive).read_bytes()).hexdigest()})
            preview = preview_candidate_purge(self.store, identifier)
            intent = prepare_candidate_purge(self.store, identifier, expected_preview_sha256=preview["intent_id"])
            finish_candidate_purge(self.store, identifier, intent_id=intent["intent_id"])
            reviews = self.call("list")[1]["reviews"]
            self.assertEqual(reviews, [saved] if candidate == first else [])

    def test_older_selected_bank_review_reopens_without_reinterpreting_scope(self):
        self.admit()
        _, preview = self.call("preview")
        snapshot = preview["snapshot"]
        del snapshot["banks"]
        for task in snapshot["tasks"]:
            del task["banks"]
        snapshot["scope"] = "saved_graph_and_selected_bank"
        entity = ResearchEntityId(ResearchKind.PROJECT_REVIEW, uuid4())
        evidence = {self.store.put_evidence(self.graph.read_bytes())}
        evidence.update(EvidenceRef.parse(row["archive_ref"]) for row in snapshot["inputs"])
        record = {"schema": preview["schema"], "snapshot": snapshot, "review_sha256": sha256(_canonical(snapshot)).hexdigest(), "reviewed_at_utc": "2026-09-05T00:00:00+00:00"}
        revision = self.store.create_revision(entity, _canonical(record), evidence=tuple(evidence))
        self.store.compare_and_set_current(entity, expected_revision=None, target_revision=revision.revision)
        self.assertEqual(self.call("list")[1]["reviews"][0]["snapshot"], snapshot)
        self.assertEqual(json.loads(self.store.read_revision_content(revision.revision)), record)


if __name__ == "__main__":
    unittest.main()
