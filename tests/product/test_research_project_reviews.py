from hashlib import sha256
from pathlib import Path
from tempfile import TemporaryDirectory
from zipfile import ZipFile
import unittest

from tradercockpit.research_project_reviews import project_review_response
from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.research_candidates import admit_databank_candidate
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

    def admit(self, name="A.sqx"):
        raw = archive_bytes(name)
        (self.bank / name).write_bytes(raw)
        return admit_databank_candidate(self.store, project="P", databank="Results", archive=name, archive_bytes=raw)

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

    def test_candidate_delete_reclaims_exclusive_review_and_retains_shared_review(self):
        first, second = self.admit(), self.admit("B.sqx")
        saved = self.retain()
        for candidate, archive in ((first, "A.sqx"), (second, "B.sqx")):
            identifier = candidate["entity_id"]
            record_databank_membership_operation(self.store, action="remove", candidate_entity_id=identifier,
                expected_membership_revision=read_candidate_memberships(self.store, identifier)["revision"],
                candidate_revision=candidate["revision"], source={"project": "P", "databank": "Results", "archive": archive,
                "archive_sha256": sha256((self.bank / archive).read_bytes()).hexdigest()})
            preview = preview_candidate_purge(self.store, identifier)
            intent = prepare_candidate_purge(self.store, identifier, expected_preview_sha256=preview["intent_id"])
            finish_candidate_purge(self.store, identifier, intent_id=intent["intent_id"])
            reviews = self.call("list")[1]["reviews"]
            self.assertEqual(reviews, [saved] if candidate == first else [])


if __name__ == "__main__":
    unittest.main()
