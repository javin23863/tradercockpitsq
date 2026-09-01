from __future__ import annotations

from hashlib import sha256
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
from zipfile import ZipFile

from tradercockpit.research_custody import FileResearchCustodyStore, ResearchCustodyError
from tradercockpit.research_retester import (
    RETESTER_CATALOG_SCHEMA,
    RETESTER_READ_SCHEMA,
    ResearchRetesterError,
    list_current_historical_results,
    read_current_historical_result,
    start_native_retester,
)
from tradercockpit.sqx_outputs import inspect_sqx_output_bytes


class ResearchRetesterTests(unittest.TestCase):
    CANDIDATE_ENTITY = "tc-research:candidate:v1:11111111-1111-4111-8111-111111111111"
    CANDIDATE_REVISION = f"tc-research-revision:candidate:sha256:{'1' * 64}"
    LAUNCHER_SHA = "a" * 64

    def _archive_bytes(self, marker: str) -> bytes:
        from io import BytesIO

        stream = BytesIO()
        with ZipFile(stream, "w") as archive:
            archive.writestr("settings.xml", f"<Settings>{marker}</Settings>".encode())
            archive.writestr("strategy_Portfolio.xml", f"<Strategy>{marker}</Strategy>".encode())
            archive.writestr("version.txt", b"144.2953")
            archive.writestr("orders.bin", marker.encode())
        return stream.getvalue()

    @staticmethod
    def _retester_config(*, task_type: str = "Retest", task_file: str = "Retest-Task1.xml") -> bytes:
        return (
            f'<Project name="Retester" version="144.2953">'
            f'<Tasks><Task type="{task_type}" name="Retest" active="true" taskXMLFile="{task_file}"/></Tasks>'
            "</Project>"
        ).encode()

    def _runtime(self, root: Path, engine_bytes: bytes) -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        (root / "internal/libs").mkdir(parents=True)
        (root / "internal/libs/SQTradingLib.jar").write_bytes(engine_bytes)
        (root / "user/projects/Retester").mkdir(parents=True)
        with ZipFile(root / "user/projects/Retester/project.cfx", "w") as archive:
            archive.writestr("config.xml", self._retester_config())
            archive.writestr("Retest-Task1.xml", b"<Settings/>")
        return root

    def _candidate(self, store: FileResearchCustodyStore, archive_bytes: bytes) -> dict[str, object]:
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

    def _gateway_factory(self, home: Path, *, result_marker: str | None):
        outer = self

        class Gateway:
            def __init__(self, sqx_home, trusted_launcher_sha256):
                self.home = Path(sqx_home)
                self.trusted = trusted_launcher_sha256

            def launch_retester_task(
                self,
                project_name,
                *,
                expected_project_sha256,
                expected_engine_sha256,
            ):
                project = self.home / "user/projects" / project_name / "project.cfx"
                engine = self.home / "internal/libs/SQTradingLib.jar"
                outer.assertEqual(sha256(project.read_bytes()).hexdigest(), expected_project_sha256)
                outer.assertEqual(sha256(engine.read_bytes()).hexdigest(), expected_engine_sha256)
                if result_marker is not None:
                    result = self.home / "user/projects" / project_name / "databanks/Results/Survivor.sqx"
                    result.write_bytes(outer._archive_bytes(result_marker))
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
                    "engine_sha256": expected_engine_sha256,
                    "control_requests_submitted": 1,
                    "control_requests_completed": 1,
                    "partial_side_effect": False,
                    "receipts": [{
                        "sequence": 1,
                        "action": "startOnlyTask",
                        "project": project_name,
                        "task": 1,
                        "state": "completed",
                        "exit_code": 0,
                        "sqx_build": "144.2953",
                        "launcher_sha256": outer.LAUNCHER_SHA,
                        "project_sha256": expected_project_sha256,
                        "engine_sha256": expected_engine_sha256,
                        "reason_code": None,
                    }],
                }

        return Gateway

    def test_exact_candidate_executes_to_changed_native_result_and_reopens(self) -> None:
        engine = b"fixture sq trading lib"
        engine_hash = sha256(engine).hexdigest()
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", engine)
            store = FileResearchCustodyStore(root / "data")
            candidate = self._candidate(store, self._archive_bytes("source"))
            with patch("tradercockpit.research_retester.read_current_candidate", return_value=candidate):
                result = start_native_retester(
                    store,
                    home,
                    self.LAUNCHER_SHA,
                    candidate_entity_id=self.CANDIDATE_ENTITY,
                    expected_candidate_revision=self.CANDIDATE_REVISION,
                    gateway_factory=self._gateway_factory(home, result_marker="retested"),
                )

            self.assertEqual(result["schema"], RETESTER_READ_SCHEMA)
            self.assertEqual(result["state"], "completed")
            self.assertTrue(result["execution_completed"])
            self.assertEqual(result["validation_state"], "not_run")
            self.assertEqual(result["candidate_revision"], self.CANDIDATE_REVISION)
            self.assertEqual(result["retester_task"], 1)
            self.assertEqual(result["launcher_sha256"], self.LAUNCHER_SHA)
            self.assertEqual(result["engine_sha256"], engine_hash)
            self.assertNotEqual(result["result_archive_sha256"], result["candidate_archive_sha256"])
            self.assertFalse(result["reused"])

            reopened_store = FileResearchCustodyStore(root / "data")
            reopened = read_current_historical_result(reopened_store, result["entity_id"])
            self.assertEqual(reopened["revision"], result["revision"])
            self.assertEqual(reopened["result_archive_sha256"], result["result_archive_sha256"])
            self.assertEqual(reopened["validation_state"], "not_run")
            catalog = list_current_historical_results(reopened_store, self.CANDIDATE_REVISION)
            self.assertEqual(catalog["schema"], RETESTER_CATALOG_SCHEMA)
            self.assertEqual(len(catalog["results"]), 1)

            with patch("tradercockpit.research_retester.read_current_candidate", return_value=candidate):
                reused = start_native_retester(
                    reopened_store,
                    home,
                    self.LAUNCHER_SHA,
                    candidate_entity_id=self.CANDIDATE_ENTITY,
                    expected_candidate_revision=self.CANDIDATE_REVISION,
                    gateway_factory=lambda *args: self.fail("Retester must not relaunch on exact retry"),
                )
            self.assertTrue(reused["reused"])
            self.assertEqual(reused["entity_id"], result["entity_id"])

    def test_installed_engine_identity_is_captured_as_provenance_without_compiled_in_allowlist(self) -> None:
        engine = b"authorized runtime engine variant"
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", engine)
            store = FileResearchCustodyStore(root / "data")
            candidate = self._candidate(store, self._archive_bytes("source"))
            with patch("tradercockpit.research_retester.read_current_candidate", return_value=candidate):
                result = start_native_retester(
                    store,
                    home,
                    self.LAUNCHER_SHA,
                    candidate_entity_id=self.CANDIDATE_ENTITY,
                    expected_candidate_revision=self.CANDIDATE_REVISION,
                    gateway_factory=self._gateway_factory(home, result_marker="retested"),
                )
            self.assertEqual(result["state"], "completed")
            self.assertEqual(result["engine_sha256"], sha256(engine).hexdigest())

    def test_source_project_must_contain_retest_task_one_before_gateway_or_workspace(self) -> None:
        engine = b"fixture sq trading lib"
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", engine)
            with ZipFile(home / "user/projects/Retester/project.cfx", "w") as archive:
                archive.writestr("config.xml", self._retester_config(task_file="Retest-Task2.xml"))
                archive.writestr("Retest-Task2.xml", b"<Settings/>")
            store = FileResearchCustodyStore(root / "data")
            candidate = self._candidate(store, self._archive_bytes("source"))
            with patch("tradercockpit.research_retester.read_current_candidate", return_value=candidate):
                with self.assertRaises(ResearchRetesterError) as caught:
                    start_native_retester(
                        store,
                        home,
                        self.LAUNCHER_SHA,
                        candidate_entity_id=self.CANDIDATE_ENTITY,
                        expected_candidate_revision=self.CANDIDATE_REVISION,
                        gateway_factory=lambda *args: self.fail("invalid source project reached gateway"),
                    )
            self.assertEqual(caught.exception.code, "retester_source_project_invalid")
            generated = [path for path in (home / "user/projects").iterdir() if path.name.startswith("TraderCockpit-Retester-")]
            self.assertEqual(generated, [])

    def test_source_project_task_one_must_be_declared_as_retest(self) -> None:
        engine = b"fixture sq trading lib"
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", engine)
            with ZipFile(home / "user/projects/Retester/project.cfx", "w") as archive:
                archive.writestr("config.xml", self._retester_config(task_type="Build"))
                archive.writestr("Retest-Task1.xml", b"<Settings/>")
            store = FileResearchCustodyStore(root / "data")
            candidate = self._candidate(store, self._archive_bytes("source"))
            with patch("tradercockpit.research_retester.read_current_candidate", return_value=candidate):
                with self.assertRaises(ResearchRetesterError) as caught:
                    start_native_retester(
                        store,
                        home,
                        self.LAUNCHER_SHA,
                        candidate_entity_id=self.CANDIDATE_ENTITY,
                        expected_candidate_revision=self.CANDIDATE_REVISION,
                        gateway_factory=lambda *args: self.fail("non-Retest task reached gateway"),
                    )
            self.assertEqual(caught.exception.code, "retester_source_project_invalid")

    def test_unsupported_xml_encoding_is_a_typed_source_project_refusal(self) -> None:
        engine = b"fixture sq trading lib"
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", engine)
            with ZipFile(home / "user/projects/Retester/project.cfx", "w") as archive:
                archive.writestr("config.xml", self._retester_config())
                archive.writestr(
                    "Retest-Task1.xml",
                    b'<?xml version="1.0" encoding="x-unsupported-sqx-test"?><Settings/>',
                )
            store = FileResearchCustodyStore(root / "data")
            candidate = self._candidate(store, self._archive_bytes("source"))
            with patch("tradercockpit.research_retester.read_current_candidate", return_value=candidate):
                with self.assertRaises(ResearchRetesterError) as caught:
                    start_native_retester(
                        store,
                        home,
                        self.LAUNCHER_SHA,
                        candidate_entity_id=self.CANDIDATE_ENTITY,
                        expected_candidate_revision=self.CANDIDATE_REVISION,
                        gateway_factory=lambda *args: self.fail("invalid XML reached gateway"),
                    )
            self.assertEqual(caught.exception.code, "retester_source_project_invalid")

    def test_unchanged_native_archive_becomes_durable_failed_result(self) -> None:
        engine = b"fixture sq trading lib"
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", engine)
            store = FileResearchCustodyStore(root / "data")
            candidate = self._candidate(store, self._archive_bytes("source"))
            with patch("tradercockpit.research_retester.read_current_candidate", return_value=candidate):
                with self.assertRaises(ResearchRetesterError) as caught:
                    start_native_retester(
                        store,
                        home,
                        self.LAUNCHER_SHA,
                        candidate_entity_id=self.CANDIDATE_ENTITY,
                        expected_candidate_revision=self.CANDIDATE_REVISION,
                        gateway_factory=self._gateway_factory(home, result_marker=None),
                    )
            self.assertEqual(caught.exception.code, "retester_result_unchanged")
            catalog = list_current_historical_results(store, self.CANDIDATE_REVISION)
            self.assertEqual(len(catalog["results"]), 1)
            failed = catalog["results"][0]
            self.assertEqual(failed["state"], "failed")
            self.assertEqual(failed["failure_reason_code"], "retester_result_unchanged")
            self.assertTrue(failed["partial_side_effect"])
            self.assertEqual(failed["receipts"][0]["state"], "completed")
            self.assertFalse(failed["execution_completed"])
            self.assertEqual(failed["validation_state"], "not_run")

    def test_stale_candidate_revision_refuses_before_native_preflight(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            store = FileResearchCustodyStore(root / "data")
            candidate = self._candidate(store, self._archive_bytes("source"))
            candidate = {**candidate, "revision": f"tc-research-revision:candidate:sha256:{'2' * 64}"}
            with patch("tradercockpit.research_retester.read_current_candidate", return_value=candidate):
                with self.assertRaises(ResearchCustodyError) as caught:
                    start_native_retester(
                        store,
                        root / "missing-sqx",
                        self.LAUNCHER_SHA,
                        candidate_entity_id=self.CANDIDATE_ENTITY,
                        expected_candidate_revision=self.CANDIDATE_REVISION,
                    )
            self.assertEqual(caught.exception.code, "current_conflict")


if __name__ == "__main__":
    unittest.main()
