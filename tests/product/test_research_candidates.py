from __future__ import annotations

from hashlib import sha256
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
from zipfile import ZipFile

from tradercockpit.research_candidates import (
    CANDIDATE_ASSOCIATION_MODE,
    CANDIDATE_CATALOG_SCHEMA,
    CANDIDATE_READ_SCHEMA,
    ResearchCandidateError,
    import_native_candidate,
    list_current_candidates,
    read_current_candidate,
)
from tradercockpit.research_custody import FileResearchCustodyStore


class ResearchCandidateTests(unittest.TestCase):
    def _runtime(self, root: Path) -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        (root / "user/projects/Builder/databanks/Results").mkdir(parents=True)
        return root

    def _output(self, home: Path, name: str = "Survivor.sqx") -> Path:
        target = home / "user/projects/Builder/databanks/Results" / name
        with ZipFile(target, "w") as archive:
            archive.writestr("settings.xml", b"<Settings><Source>native</Source></Settings>")
            archive.writestr("strategy_Portfolio.xml", b"<Strategy><Rule>producer-owned</Rule></Strategy>")
            archive.writestr("version.txt", b"144.2953")
            archive.writestr("orders.bin", b"opaque native payload")
        return target

    def _job(self) -> dict[str, object]:
        return {
            "schema": "tc.research-native-job.v1",
            "entity_id": "tc-research:native-job:v1:11111111-1111-4111-8111-111111111111",
            "revision": f"tc-research-revision:native-job:sha256:{'1' * 64}",
            "state": "submitted",
            "configuration_entity_id": "tc-research:configuration:v1:22222222-2222-4222-8222-222222222222",
            "configuration_revision": f"tc-research-revision:configuration:sha256:{'2' * 64}",
        }

    def test_import_preserves_exact_archive_entries_and_operator_association(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx")
            target = self._output(home)
            store = FileResearchCustodyStore(root / "data")
            job = self._job()
            digest = sha256(target.read_bytes()).hexdigest()
            with patch("tradercockpit.research_candidates.read_current_native_job", return_value=job):
                candidate = import_native_candidate(
                    store,
                    home,
                    native_job_entity_id=job["entity_id"],
                    expected_native_job_revision=job["revision"],
                    archive_name=target.name,
                    expected_archive_sha256=digest,
                )

            self.assertEqual(candidate["schema"], CANDIDATE_READ_SCHEMA)
            self.assertEqual(candidate["native_job_revision"], job["revision"])
            self.assertEqual(candidate["configuration_revision"], job["configuration_revision"])
            self.assertEqual(candidate["association_mode"], CANDIDATE_ASSOCIATION_MODE)
            self.assertEqual(candidate["archive_sha256"], digest)
            self.assertEqual(candidate["archive_name"], "Survivor.sqx")
            self.assertEqual(candidate["archive_relative_path"], "user/projects/Builder/databanks/Results/Survivor.sqx")
            self.assertFalse(candidate["reused"])
            reopened = read_current_candidate(store, candidate["entity_id"])
            self.assertEqual(reopened["revision"], candidate["revision"])
            self.assertEqual(reopened["strategy_sha256"], candidate["strategy_sha256"])
            self.assertEqual(reopened["settings_sha256"], candidate["settings_sha256"])

    def test_retry_same_job_and_archive_reuses_candidate(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx")
            target = self._output(home)
            store = FileResearchCustodyStore(root / "data")
            job = self._job()
            digest = sha256(target.read_bytes()).hexdigest()
            with patch("tradercockpit.research_candidates.read_current_native_job", return_value=job):
                first = import_native_candidate(
                    store,
                    home,
                    native_job_entity_id=job["entity_id"],
                    expected_native_job_revision=job["revision"],
                    archive_name=target.name,
                    expected_archive_sha256=digest,
                )
                second = import_native_candidate(
                    store,
                    home,
                    native_job_entity_id=job["entity_id"],
                    expected_native_job_revision=job["revision"],
                    archive_name=target.name,
                    expected_archive_sha256=digest,
                )

            self.assertFalse(first["reused"])
            self.assertTrue(second["reused"])
            self.assertEqual(second["entity_id"], first["entity_id"])
            self.assertEqual(second["revision"], first["revision"])
            catalog = list_current_candidates(store)
            self.assertEqual(catalog["schema"], CANDIDATE_CATALOG_SCHEMA)
            self.assertEqual(len(catalog["candidates"]), 1)

    def test_unsubmitted_job_and_changed_archive_fail_closed(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx")
            target = self._output(home)
            store = FileResearchCustodyStore(root / "data")
            job = {**self._job(), "state": "failed"}
            digest = sha256(target.read_bytes()).hexdigest()
            with patch("tradercockpit.research_candidates.read_current_native_job", return_value=job):
                with self.assertRaises(ResearchCandidateError) as caught:
                    import_native_candidate(
                        store,
                        home,
                        native_job_entity_id=job["entity_id"],
                        expected_native_job_revision=job["revision"],
                        archive_name=target.name,
                        expected_archive_sha256=digest,
                    )
            self.assertEqual(caught.exception.code, "candidate_native_job_not_submitted")

            submitted = self._job()
            target.write_bytes(target.read_bytes() + b"changed")
            with patch("tradercockpit.research_candidates.read_current_native_job", return_value=submitted):
                with self.assertRaises(ResearchCandidateError) as caught:
                    import_native_candidate(
                        store,
                        home,
                        native_job_entity_id=submitted["entity_id"],
                        expected_native_job_revision=submitted["revision"],
                        archive_name=target.name,
                        expected_archive_sha256=digest,
                    )
            self.assertEqual(caught.exception.code, "output_digest_mismatch")


if __name__ == "__main__":
    unittest.main()
