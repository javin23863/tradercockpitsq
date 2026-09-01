from __future__ import annotations

from hashlib import sha256
from io import BytesIO
import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
from xml.etree import ElementTree
from zipfile import ZipFile

from tradercockpit.research_custody import FileResearchCustodyStore, ResearchCustodyError, ResearchRevisionRef
from tradercockpit.research_robustness import (
    ROBUSTNESS_METHOD_HIGHER_PRECISION,
    ROBUSTNESS_OUTCOME_UNREAD,
    ROBUSTNESS_RECORD_SCHEMA,
    ResearchRobustnessError,
    compile_higher_precision_project,
    list_native_robustness_results,
    read_native_robustness_capabilities,
    read_native_robustness_result,
    start_native_higher_precision,
)
from tradercockpit.sqx_gateway import SqxNativeGatewayError
from tradercockpit.sqx_outputs import inspect_sqx_output_bytes


class ResearchRobustnessTests(unittest.TestCase):
    HISTORICAL_ENTITY = "tc-research:historical-result:v1:11111111-1111-4111-8111-111111111111"
    HISTORICAL_REVISION = f"tc-research-revision:historical-result:sha256:{'1' * 64}"
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
    def _task_xml(*, higher_use: str = "false", other_use: str = "false", include_higher: bool = True, include_precision: bool = True) -> bytes:
        higher = ""
        if include_higher:
            precision = "<Precision>2</Precision>" if include_precision else ""
            higher = (
                f'<RetestWithHigherPrecision use="{higher_use}">'
                f"<Settings>{precision}<Spread>3</Spread></Settings>"
                "<AcceptanceSettings/>"
                "</RetestWithHigherPrecision>"
            )
        return (
            "<Settings>"
            "<CrossChecks>"
            f"{higher}"
            f'<MonteCarloManipulation use="{other_use}"><Settings/></MonteCarloManipulation>'
            "</CrossChecks>"
            "</Settings>"
        ).encode()

    @staticmethod
    def _project_bytes(task_xml: bytes) -> bytes:
        stream = BytesIO()
        config = (
            '<Project name="Retester" version="144.2953">'
            '<Tasks><Task type="Retest" name="Retest" active="true" taskXMLFile="Retest-Task1.xml"/></Tasks>'
            "</Project>"
        ).encode()
        with ZipFile(stream, "w") as archive:
            archive.writestr("config.xml", config)
            archive.writestr("Retest-Task1.xml", task_xml)
        return stream.getvalue()

    def _runtime(self, root: Path, project_bytes: bytes, engine: bytes = b"sq engine") -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        (root / "internal/libs").mkdir(parents=True)
        (root / "internal/libs/SQTradingLib.jar").write_bytes(engine)
        (root / "user/projects/Retester").mkdir(parents=True)
        (root / "user/projects/Retester/project.cfx").write_bytes(project_bytes)
        return root

    def _historical(self, store: FileResearchCustodyStore, source: bytes) -> dict[str, object]:
        ref = store.put_evidence(source)
        inspected = inspect_sqx_output_bytes(source, archive_name="Baseline.sqx")
        return {
            "revision": self.HISTORICAL_REVISION,
            "state": "completed",
            "execution_completed": True,
            "sqx_build": "144.2953",
            "result_archive_name": "Baseline.sqx",
            "result_archive_ref": str(ref),
            "result_archive_sha256": inspected["archive_sha256"],
        }

    def _current_proof_payload(self, store: FileResearchCustodyStore) -> dict[str, object]:
        current = store.base / "current" / "proof"
        pointers = sorted(current.glob("*.json"))
        self.assertEqual(len(pointers), 1)
        pointer = json.loads(pointers[0].read_text(encoding="utf-8"))
        revision = ResearchRevisionRef.parse(pointer["revision"])
        return json.loads(store.read_revision_content(revision))

    def _gateway_factory(self, home: Path, result_marker: str | None, *, mutate_engine: bool = False):
        outer = self

        class Gateway:
            def __init__(self, sqx_home, trusted_launcher_sha256):
                self.home = Path(sqx_home)
                outer.assertEqual(trusted_launcher_sha256, outer.LAUNCHER_SHA)

            def launch_retester_task(self, project_name, *, expected_project_sha256, expected_engine_sha256, result_archive_name=None, expected_result_archive_sha256=None):
                project = self.home / "user/projects" / project_name / "project.cfx"
                outer.assertEqual(sha256(project.read_bytes()).hexdigest(), expected_project_sha256)
                engine = self.home / "internal/libs/SQTradingLib.jar"
                outer.assertEqual(sha256(engine.read_bytes()).hexdigest(), expected_engine_sha256)
                baseline = self.home / "user/projects" / project_name / "databanks/Results" / result_archive_name
                outer.assertEqual(sha256(baseline.read_bytes()).hexdigest(), expected_result_archive_sha256)

                with ZipFile(project) as archive:
                    task = ElementTree.fromstring(archive.read("Retest-Task1.xml"))
                profiles = [node for node in task.iter() if node.tag == "RetestWithHigherPrecision"]
                outer.assertEqual(len(profiles), 1)
                outer.assertEqual(profiles[0].attrib.get("use"), "true")
                mc = [node for node in task.iter() if node.tag == "MonteCarloManipulation"]
                outer.assertEqual(len(mc), 1)
                outer.assertEqual(mc[0].attrib.get("use"), "false")

                if result_marker is not None:
                    result = self.home / "user/projects" / project_name / "databanks/Results/Baseline.sqx"
                    result.write_bytes(outer._archive_bytes(result_marker))
                if mutate_engine:
                    engine.write_bytes(b"changed engine")

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
                    "result_archive_name": result_archive_name,
                    "result_archive_relative_path": f"user/projects/{project_name}/databanks/Results/{result_archive_name}",
                    "result_archive_sha256": expected_result_archive_sha256,
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
                        "result_archive_name": result_archive_name,
                        "result_archive_relative_path": f"user/projects/{project_name}/databanks/Results/{result_archive_name}",
                        "result_archive_sha256": expected_result_archive_sha256,
                        "reason_code": None,
                    }],
                }

        return Gateway

    def test_compiler_enables_existing_native_profile_and_preserves_native_settings(self) -> None:
        source = self._project_bytes(self._task_xml())
        compiled, plan = compile_higher_precision_project(source)

        self.assertNotEqual(compiled, source)
        self.assertEqual(plan["method"], ROBUSTNESS_METHOD_HIGHER_PRECISION)
        self.assertEqual(plan["native_settings"], {"Precision": "2", "Spread": "3"})
        self.assertEqual(plan["source_project_sha256"], sha256(source).hexdigest())
        self.assertEqual(plan["compiled_project_sha256"], sha256(compiled).hexdigest())
        self.assertNotEqual(plan["source_task_sha256"], plan["compiled_task_sha256"])

        with ZipFile(BytesIO(compiled)) as archive:
            root = ElementTree.fromstring(archive.read("Retest-Task1.xml"))
        higher = [node for node in root.iter() if node.tag == "RetestWithHigherPrecision"]
        monte_carlo = [node for node in root.iter() if node.tag == "MonteCarloManipulation"]
        self.assertEqual(higher[0].attrib["use"], "true")
        self.assertEqual(monte_carlo[0].attrib["use"], "false")

    def test_compiler_refuses_missing_profile_instead_of_inventing_native_defaults(self) -> None:
        source = self._project_bytes(self._task_xml(include_higher=False))
        with self.assertRaises(ResearchRobustnessError) as caught:
            compile_higher_precision_project(source)
        self.assertEqual(caught.exception.code, "robustness_higher_precision_missing")

    def test_compiler_refuses_another_enabled_crosscheck_instead_of_silently_mutating_it(self) -> None:
        source = self._project_bytes(self._task_xml(other_use="true"))
        with self.assertRaises(ResearchRobustnessError) as caught:
            compile_higher_precision_project(source)
        self.assertEqual(caught.exception.code, "robustness_other_crosscheck_enabled")

    def test_compiler_requires_installed_precision_and_spread_values(self) -> None:
        source = self._project_bytes(self._task_xml(include_precision=False))
        with self.assertRaises(ResearchRobustnessError) as caught:
            compile_higher_precision_project(source)
        self.assertEqual(caught.exception.code, "robustness_higher_precision_invalid")

    def test_exact_historical_result_executes_native_higher_precision_and_reopens(self) -> None:
        source_result = self._archive_bytes("baseline")
        project = self._project_bytes(self._task_xml())
        engine = b"installed engine variant"
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", project, engine)
            store = FileResearchCustodyStore(root / "data")
            historical = self._historical(store, source_result)
            with patch("tradercockpit.research_robustness.read_current_historical_result", return_value=historical):
                result = start_native_higher_precision(
                    store,
                    home,
                    self.LAUNCHER_SHA,
                    historical_result_entity_id=self.HISTORICAL_ENTITY,
                    expected_historical_result_revision=self.HISTORICAL_REVISION,
                    gateway_factory=self._gateway_factory(home, "higher-precision"),
                )

            self.assertEqual(result["schema"], ROBUSTNESS_RECORD_SCHEMA)
            self.assertEqual(result["method"], ROBUSTNESS_METHOD_HIGHER_PRECISION)
            self.assertEqual(result["native_settings"], {"Precision": "2", "Spread": "3"})
            self.assertEqual(result["execution_state"], "completed")
            self.assertEqual(result["producer_outcome_state"], ROBUSTNESS_OUTCOME_UNREAD)
            self.assertEqual(result["source_historical_result_revision"], self.HISTORICAL_REVISION)
            self.assertEqual(result["engine_sha256"], sha256(engine).hexdigest())
            self.assertEqual(result["launcher_sha256"], self.LAUNCHER_SHA)
            self.assertNotEqual(result["result_archive_sha256"], result["source_result_archive_sha256"])
            self.assertTrue(str(result["validation_ref"]).startswith("tc-evidence:sha256:"))
            self.assertTrue(str(result["proof_entity_id"]).startswith("tc-research:proof:v1:"))
            self.assertTrue(str(result["proof_revision"]).startswith("tc-research-revision:proof:sha256:"))

            catalog = list_native_robustness_results(store)
            self.assertEqual(catalog["results"], [result])
            reopened = read_native_robustness_result(store, result["validation_ref"])
            self.assertEqual(reopened, result)

    def test_revision_substitution_is_refused_before_native_execution(self) -> None:
        source_result = self._archive_bytes("baseline")
        project = self._project_bytes(self._task_xml())
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", project)
            store = FileResearchCustodyStore(root / "data")
            historical = self._historical(store, source_result)
            with patch("tradercockpit.research_robustness.read_current_historical_result", return_value=historical):
                with self.assertRaises(ResearchRobustnessError) as caught:
                    start_native_higher_precision(
                        store,
                        home,
                        self.LAUNCHER_SHA,
                        historical_result_entity_id=self.HISTORICAL_ENTITY,
                        expected_historical_result_revision=f"tc-research-revision:historical-result:sha256:{'2' * 64}",
                        gateway_factory=lambda *args: self.fail("revision substitution reached gateway"),
                    )
            self.assertEqual(caught.exception.code, "robustness_source_revision_changed")

    def test_unchanged_native_result_is_not_accepted_as_robustness_output(self) -> None:
        source_result = self._archive_bytes("baseline")
        project = self._project_bytes(self._task_xml())
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", project)
            store = FileResearchCustodyStore(root / "data")
            historical = self._historical(store, source_result)
            with patch("tradercockpit.research_robustness.read_current_historical_result", return_value=historical):
                with self.assertRaises(ResearchRobustnessError) as caught:
                    start_native_higher_precision(
                        store,
                        home,
                        self.LAUNCHER_SHA,
                        historical_result_entity_id=self.HISTORICAL_ENTITY,
                        expected_historical_result_revision=self.HISTORICAL_REVISION,
                        gateway_factory=self._gateway_factory(home, None),
                    )
            self.assertEqual(caught.exception.code, "robustness_result_unchanged")

    def test_engine_change_across_native_execution_is_refused(self) -> None:
        source_result = self._archive_bytes("baseline")
        project = self._project_bytes(self._task_xml())
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", project)
            store = FileResearchCustodyStore(root / "data")
            historical = self._historical(store, source_result)
            with patch("tradercockpit.research_robustness.read_current_historical_result", return_value=historical):
                with self.assertRaises(ResearchRobustnessError) as caught:
                    start_native_higher_precision(
                        store,
                        home,
                        self.LAUNCHER_SHA,
                        historical_result_entity_id=self.HISTORICAL_ENTITY,
                        expected_historical_result_revision=self.HISTORICAL_REVISION,
                        gateway_factory=self._gateway_factory(home, "higher-precision", mutate_engine=True),
                    )
            self.assertEqual(caught.exception.code, "robustness_engine_changed_during_execution")
            failed = self._current_proof_payload(store)
            self.assertEqual(failed["state"], "failed")
            self.assertEqual(failed["failure_reason_code"], "robustness_engine_changed_during_execution")
            self.assertEqual(failed["partial_side_effect"], True)
            self.assertEqual(len(failed["receipts"]), 1)


    def test_capability_read_model_comes_from_current_installed_profile(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            ready_home = self._runtime(root / "ready", self._project_bytes(self._task_xml()))
            ready = read_native_robustness_capabilities(ready_home)
            self.assertEqual(ready["methods"][0]["state"], "ready")
            self.assertEqual(ready["methods"][0]["native_settings"], {"Precision": "2", "Spread": "3"})

            missing_home = self._runtime(root / "missing", self._project_bytes(self._task_xml(include_higher=False)))
            missing = read_native_robustness_capabilities(missing_home)
            self.assertEqual(missing["methods"][0]["state"], "unavailable")
            self.assertEqual(missing["methods"][0]["reason_code"], "robustness_higher_precision_missing")

            conflict_home = self._runtime(root / "conflict", self._project_bytes(self._task_xml(other_use="true")))
            conflict = read_native_robustness_capabilities(conflict_home)
            self.assertEqual(conflict["methods"][0]["state"], "unavailable")
            self.assertEqual(conflict["methods"][0]["reason_code"], "robustness_other_crosscheck_enabled")

    def test_gateway_failure_persists_failed_proof_with_exact_receipt(self) -> None:
        source_result = self._archive_bytes("baseline")
        project = self._project_bytes(self._task_xml())
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", project)
            store = FileResearchCustodyStore(root / "data")
            historical = self._historical(store, source_result)
            outer = self

            class FailingGateway:
                def __init__(self, sqx_home, trusted_launcher_sha256):
                    outer.assertEqual(Path(sqx_home), home)
                    outer.assertEqual(trusted_launcher_sha256, outer.LAUNCHER_SHA)

                def launch_retester_task(self, project_name, *, expected_project_sha256, expected_engine_sha256, result_archive_name=None, expected_result_archive_sha256=None):
                    raise SqxNativeGatewayError(
                        "sqx_control_timeout",
                        "native control timed out",
                        receipts=[{
                            "action": "startOnlyTask",
                            "project": project_name,
                            "task": 1,
                            "state": "timeout",
                            "launcher_sha256": outer.LAUNCHER_SHA,
                            "project_sha256": expected_project_sha256,
                            "engine_sha256": expected_engine_sha256,
                        }],
                    )

            with patch("tradercockpit.research_robustness.read_current_historical_result", return_value=historical):
                with self.assertRaises(ResearchRobustnessError) as caught:
                    start_native_higher_precision(
                        store,
                        home,
                        self.LAUNCHER_SHA,
                        historical_result_entity_id=self.HISTORICAL_ENTITY,
                        expected_historical_result_revision=self.HISTORICAL_REVISION,
                        gateway_factory=FailingGateway,
                    )
            self.assertEqual(caught.exception.code, "sqx_control_timeout")
            failed = self._current_proof_payload(store)
            self.assertEqual(failed["state"], "failed")
            self.assertEqual(failed["failure_reason_code"], "sqx_control_timeout")
            self.assertEqual(failed["partial_side_effect"], True)
            self.assertEqual(failed["receipts"][0]["action"], "startOnlyTask")
            catalog = list_native_robustness_results(store)
            self.assertEqual(catalog["results"], [])
            self.assertEqual(len(catalog["failed_attempts"]), 1)
            attempt = catalog["failed_attempts"][0]
            self.assertEqual(attempt["failure_reason_code"], "sqx_control_timeout")
            self.assertEqual(read_native_robustness_result(store, attempt["attempt_ref"]), attempt)

    def test_completion_custody_failure_persists_failed_proof_after_native_execution(self) -> None:
        source_result = self._archive_bytes("baseline")
        result_bytes = self._archive_bytes("higher-precision")
        project = self._project_bytes(self._task_xml())
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", project)
            store = FileResearchCustodyStore(root / "data")
            historical = self._historical(store, source_result)
            original_put = store.put_evidence

            failed_once = False

            def flaky_put(value: bytes):
                nonlocal failed_once
                if value == result_bytes and not failed_once:
                    failed_once = True
                    raise PermissionError("simulated completed-result filesystem failure")
                return original_put(value)

            with patch("tradercockpit.research_robustness.read_current_historical_result", return_value=historical):
                with patch.object(store, "put_evidence", side_effect=flaky_put):
                    with self.assertRaises(ResearchRobustnessError) as caught:
                        start_native_higher_precision(
                            store,
                            home,
                            self.LAUNCHER_SHA,
                            historical_result_entity_id=self.HISTORICAL_ENTITY,
                            expected_historical_result_revision=self.HISTORICAL_REVISION,
                            gateway_factory=self._gateway_factory(home, "higher-precision"),
                        )
            self.assertEqual(caught.exception.code, "robustness_completion_custody_failed")
            failed = self._current_proof_payload(store)
            self.assertEqual(failed["state"], "failed")
            self.assertEqual(failed["failure_reason_code"], "robustness_completion_custody_failed")
            self.assertTrue(failed["partial_side_effect"])
            self.assertEqual(failed["launcher_sha256"], self.LAUNCHER_SHA)
            self.assertEqual(failed["receipts"][0]["state"], "completed")

    def test_detached_validation_evidence_is_not_reopened_without_current_proof(self) -> None:
        source_result = self._archive_bytes("baseline")
        project = self._project_bytes(self._task_xml())
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", project)
            store = FileResearchCustodyStore(root / "data")
            historical = self._historical(store, source_result)
            with patch("tradercockpit.research_robustness.read_current_historical_result", return_value=historical):
                result = start_native_higher_precision(
                    store,
                    home,
                    self.LAUNCHER_SHA,
                    historical_result_entity_id=self.HISTORICAL_ENTITY,
                    expected_historical_result_revision=self.HISTORICAL_REVISION,
                    gateway_factory=self._gateway_factory(home, "higher-precision"),
                )
            for pointer in (store.base / "current" / "proof").glob("*.json"):
                pointer.unlink()
            with self.assertRaises(ResearchRobustnessError) as caught:
                read_native_robustness_result(store, result["validation_ref"])
            self.assertEqual(caught.exception.code, "robustness_proof_required")

    def test_invalid_validation_ref_is_typed(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            with self.assertRaises(ResearchRobustnessError) as caught:
                read_native_robustness_result(store, "not-an-evidence-ref")
            self.assertEqual(caught.exception.code, "robustness_record_ref_invalid")


if __name__ == "__main__":
    unittest.main()
