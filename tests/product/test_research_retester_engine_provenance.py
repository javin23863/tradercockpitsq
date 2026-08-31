from __future__ import annotations

from hashlib import sha256
from io import BytesIO
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
from zipfile import ZipFile

from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.research_retester import (
    ResearchRetesterError,
    list_current_historical_results,
    start_native_retester,
)
from tradercockpit.sqx_outputs import inspect_sqx_output_bytes


class RetesterEngineExecutionProvenanceTests(unittest.TestCase):
    CANDIDATE_ENTITY = "tc-research:candidate:v1:11111111-1111-4111-8111-111111111111"
    CANDIDATE_REVISION = f"tc-research-revision:candidate:sha256:{'1' * 64}"
    LAUNCHER_SHA = "a" * 64

    @staticmethod
    def _archive_bytes(marker: str) -> bytes:
        stream = BytesIO()
        with ZipFile(stream, "w") as archive:
            archive.writestr("settings.xml", f"<Settings>{marker}</Settings>".encode())
            archive.writestr("strategy_Portfolio.xml", f"<Strategy>{marker}</Strategy>".encode())
            archive.writestr("version.txt", b"144.2953")
            archive.writestr("orders.bin", marker.encode())
        return stream.getvalue()

    @staticmethod
    def _runtime(root: Path, engine_bytes: bytes) -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        (root / "internal/libs").mkdir(parents=True)
        (root / "internal/libs/SQTradingLib.jar").write_bytes(engine_bytes)
        (root / "user/projects/Retester").mkdir(parents=True)
        with ZipFile(root / "user/projects/Retester/project.cfx", "w") as archive:
            archive.writestr("config.xml", b"<Project/>")
            archive.writestr("Retest-Task1.xml", b"<Task><Retest/></Task>")
        return root

    def _candidate(self, store: FileResearchCustodyStore) -> dict[str, object]:
        archive_bytes = self._archive_bytes("source")
        ref = store.put_evidence(archive_bytes)
        inspected = inspect_sqx_output_bytes(archive_bytes, archive_name="Survivor.sqx")
        return {
            "schema": "tc.research-candidate.v1",
            "entity_id": self.CANDIDATE_ENTITY,
            "revision": self.CANDIDATE_REVISION,
            "archive_name": "Survivor.sqx",
            "archive_ref": str(ref),
            "archive_sha256": inspected["archive_sha256"],
            "strategy_sha256": inspected["strategy_entry_sha256"],
            "settings_sha256": inspected["settings_entry_sha256"],
            "sqx_build": "144.2953",
        }

    def test_engine_change_across_native_launch_fails_durable_result(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", b"captured engine")
            store = FileResearchCustodyStore(root / "data")
            candidate = self._candidate(store)
            outer = self

            class Gateway:
                def __init__(self, sqx_home, trusted_launcher_sha256):
                    self.home = Path(sqx_home)
                    self.launcher_sha256 = trusted_launcher_sha256

                def launch_retester_task(self, project_name, *, expected_project_sha256):
                    project = self.home / "user/projects" / project_name / "project.cfx"
                    outer.assertEqual(sha256(project.read_bytes()).hexdigest(), expected_project_sha256)
                    (self.home / "internal/libs/SQTradingLib.jar").write_bytes(b"changed engine")
                    result = self.home / "user/projects" / project_name / "databanks/Results/Survivor.sqx"
                    result.write_bytes(outer._archive_bytes("retested"))
                    receipt = {
                        "sequence": 1,
                        "action": "startOnlyTask",
                        "project": project_name,
                        "task": 1,
                        "state": "completed",
                        "exit_code": 0,
                        "sqx_build": "144.2953",
                        "launcher_sha256": outer.LAUNCHER_SHA,
                        "project_sha256": expected_project_sha256,
                        "reason_code": None,
                    }
                    return {
                        "schema": "tc.sqx-native-control.v1",
                        "operation": "retester_start_task",
                        "project": project_name,
                        "task": 1,
                        "state": "submitted",
                        "sqx_build": "144.2953",
                        "launcher_sha256": outer.LAUNCHER_SHA,
                        "project_relative_path": f"user/projects/{project_name}/project.cfx",
                        "project_sha256": expected_project_sha256,
                        "control_requests_submitted": 1,
                        "control_requests_completed": 1,
                        "partial_side_effect": False,
                        "receipts": [receipt],
                    }

            with patch("tradercockpit.research_retester.read_current_candidate", return_value=candidate):
                with self.assertRaises(ResearchRetesterError) as caught:
                    start_native_retester(
                        store,
                        home,
                        self.LAUNCHER_SHA,
                        candidate_entity_id=self.CANDIDATE_ENTITY,
                        expected_candidate_revision=self.CANDIDATE_REVISION,
                        gateway_factory=Gateway,
                    )

            self.assertEqual(caught.exception.code, "retester_engine_changed_during_execution")
            catalog = list_current_historical_results(store, self.CANDIDATE_REVISION)
            self.assertEqual(len(catalog["results"]), 1)
            failed = catalog["results"][0]
            self.assertEqual(failed["state"], "failed")
            self.assertEqual(failed["failure_reason_code"], "retester_engine_changed_during_execution")
            self.assertTrue(failed["partial_side_effect"])
            self.assertEqual(failed["engine_sha256"], sha256(b"captured engine").hexdigest())
            self.assertEqual(failed["receipts"][0]["state"], "completed")


if __name__ == "__main__":
    unittest.main()
