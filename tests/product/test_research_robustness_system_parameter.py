from __future__ import annotations

from hashlib import sha256
from io import BytesIO
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
from xml.etree import ElementTree
from zipfile import ZipFile

from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.research_robustness import ROBUSTNESS_OUTCOME_UNREAD, ROBUSTNESS_RECORD_SCHEMA, ResearchRobustnessError
from tradercockpit.research_robustness_system_parameter import (
    ROBUSTNESS_METHOD_SYSTEM_PARAMETER_PERMUTATION,
    compile_system_parameter_permutation_project,
    read_native_system_parameter_permutation_result,
    start_native_system_parameter_permutation,
)
from tradercockpit.sqx_outputs import inspect_sqx_output_bytes


class ResearchSystemParameterPermutationTests(unittest.TestCase):
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
    def _task_xml(
        *,
        target_use: str = "false",
        higher_use: str = "false",
        optim_periods: str = "false",
        optim_exit_types: str = "false",
        max_tests: str = "1",
        include_target: bool = True,
    ) -> bytes:
        target = ""
        if include_target:
            target = (
                f'<OptProfileSysParamPermutation use="{target_use}">'
                "<Settings>"
                f"<OptimPeriods>{optim_periods}</OptimPeriods>"
                f"<OptimExitTypes>{optim_exit_types}</OptimExitTypes>"
                f"<MaxTests>{max_tests}</MaxTests>"
                "</Settings>"
                "<AcceptanceSettings/>"
                "</OptProfileSysParamPermutation>"
            )
        return (
            "<Settings><CrossChecks>"
            f'{target}'
            f'<RetestWithHigherPrecision use="{higher_use}"><Settings><Precision>2</Precision><Spread>3</Spread></Settings></RetestWithHigherPrecision>'
            '<MonteCarloManipulation use="false"><Settings/></MonteCarloManipulation>'
            "</CrossChecks></Settings>"
        ).encode()

    @staticmethod
    def _project_bytes(task_xml: bytes, *, opaque: bytes | None = None) -> bytes:
        stream = BytesIO()
        config = (
            '<Project name="Retester" version="144.2953">'
            '<Tasks><Task type="Retest" name="Retest" active="true" taskXMLFile="Retest-Task1.xml"/></Tasks>'
            "</Project>"
        ).encode()
        with ZipFile(stream, "w") as archive:
            archive.writestr("config.xml", config)
            archive.writestr("Retest-Task1.xml", task_xml)
            if opaque is not None:
                archive.writestr("opaque-native.bin", opaque)
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

    def _gateway_factory(self, result_marker: str | None, *, mutate_engine: bool = False):
        outer = self

        class Gateway:
            def __init__(self, sqx_home, trusted_launcher_sha256):
                self.home = Path(sqx_home)
                outer.assertEqual(trusted_launcher_sha256, outer.LAUNCHER_SHA)

            def launch_retester_task(self, project_name, *, expected_project_sha256, expected_engine_sha256):
                project = self.home / "user/projects" / project_name / "project.cfx"
                outer.assertEqual(sha256(project.read_bytes()).hexdigest(), expected_project_sha256)
                engine = self.home / "internal/libs/SQTradingLib.jar"
                outer.assertEqual(sha256(engine.read_bytes()).hexdigest(), expected_engine_sha256)
                with ZipFile(project) as archive:
                    task = ElementTree.fromstring(archive.read("Retest-Task1.xml"))
                target = [node for node in task.iter() if node.tag == "OptProfileSysParamPermutation"]
                outer.assertEqual(len(target), 1)
                outer.assertEqual(target[0].attrib.get("use"), "true")
                settings = {node.tag: (node.text or "").strip() for node in target[0].find("Settings")}
                outer.assertEqual(settings, {"OptimPeriods": "false", "OptimExitTypes": "false", "MaxTests": "1"})
                higher = [node for node in task.iter() if node.tag == "RetestWithHigherPrecision"]
                outer.assertEqual(higher[0].attrib.get("use"), "false")

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

    def test_compiler_enables_existing_native_profile_and_preserves_native_settings(self) -> None:
        source = self._project_bytes(self._task_xml())
        compiled, plan = compile_system_parameter_permutation_project(source)
        self.assertNotEqual(compiled, source)
        self.assertEqual(plan["method"], ROBUSTNESS_METHOD_SYSTEM_PARAMETER_PERMUTATION)
        self.assertEqual(
            plan["native_settings"],
            {"OptimPeriods": "false", "OptimExitTypes": "false", "MaxTests": "1"},
        )
        with ZipFile(BytesIO(compiled)) as archive:
            root = ElementTree.fromstring(archive.read("Retest-Task1.xml"))
        target = [node for node in root.iter() if node.tag == "OptProfileSysParamPermutation"]
        self.assertEqual(target[0].attrib["use"], "true")

    def test_enabled_profile_preserves_exact_project_bytes_including_opaque_members(self) -> None:
        source = self._project_bytes(self._task_xml(target_use="true"), opaque=b"opaque producer bytes")
        compiled, plan = compile_system_parameter_permutation_project(source)
        self.assertEqual(compiled, source)
        self.assertFalse(plan["configuration_changed"])
        self.assertEqual(plan["source_project_sha256"], plan["compiled_project_sha256"])

    def test_compiler_refuses_missing_profile_instead_of_inventing_it(self) -> None:
        source = self._project_bytes(self._task_xml(include_target=False))
        with self.assertRaises(ResearchRobustnessError) as caught:
            compile_system_parameter_permutation_project(source)
        self.assertEqual(caught.exception.code, "robustness_system_parameter_missing")

    def test_compiler_preserves_native_boolean_switches_and_positive_max_tests_without_defaulting(self) -> None:
        source = self._project_bytes(self._task_xml(optim_periods="true", optim_exit_types="TRUE", max_tests="17"))
        _, plan = compile_system_parameter_permutation_project(source)
        self.assertEqual(
            plan["native_settings"],
            {"OptimPeriods": "true", "OptimExitTypes": "TRUE", "MaxTests": "17"},
        )

    def test_compiler_refuses_invalid_native_settings(self) -> None:
        cases = (
            (dict(optim_periods="maybe"), "robustness_system_parameter_invalid"),
            (dict(optim_exit_types="1"), "robustness_system_parameter_invalid"),
            (dict(max_tests="0"), "robustness_system_parameter_invalid"),
            (dict(max_tests="not-int"), "robustness_system_parameter_invalid"),
        )
        for kwargs, code in cases:
            with self.subTest(kwargs=kwargs):
                source = self._project_bytes(self._task_xml(**kwargs))
                with self.assertRaises(ResearchRobustnessError) as caught:
                    compile_system_parameter_permutation_project(source)
                self.assertEqual(caught.exception.code, code)

    def test_compiler_refuses_another_enabled_crosscheck(self) -> None:
        source = self._project_bytes(self._task_xml(higher_use="true"))
        with self.assertRaises(ResearchRobustnessError) as caught:
            compile_system_parameter_permutation_project(source)
        self.assertEqual(caught.exception.code, "robustness_other_crosscheck_enabled")

    def test_exact_historical_result_executes_native_profile_and_reopens(self) -> None:
        source_result = self._archive_bytes("baseline")
        project = self._project_bytes(self._task_xml())
        engine = b"installed engine variant"
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", project, engine)
            store = FileResearchCustodyStore(root / "data")
            historical = self._historical(store, source_result)
            with patch(
                "tradercockpit.research_robustness_system_parameter.read_current_historical_result",
                return_value=historical,
            ):
                result = start_native_system_parameter_permutation(
                    store,
                    home,
                    self.LAUNCHER_SHA,
                    historical_result_entity_id=self.HISTORICAL_ENTITY,
                    expected_historical_result_revision=self.HISTORICAL_REVISION,
                    gateway_factory=self._gateway_factory("system-parameter"),
                )

            self.assertEqual(result["schema"], ROBUSTNESS_RECORD_SCHEMA)
            self.assertEqual(result["method"], ROBUSTNESS_METHOD_SYSTEM_PARAMETER_PERMUTATION)
            self.assertEqual(result["native_settings"]["MaxTests"], "1")
            self.assertEqual(result["execution_state"], "completed")
            self.assertEqual(result["producer_outcome_state"], ROBUSTNESS_OUTCOME_UNREAD)
            self.assertEqual(result["engine_sha256"], sha256(engine).hexdigest())
            self.assertNotEqual(result["result_archive_sha256"], result["source_result_archive_sha256"])
            reopened = read_native_system_parameter_permutation_result(store, result["validation_ref"])
            self.assertEqual(reopened, result)

    def test_revision_substitution_is_refused_before_native_execution(self) -> None:
        source_result = self._archive_bytes("baseline")
        project = self._project_bytes(self._task_xml())
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", project)
            store = FileResearchCustodyStore(root / "data")
            historical = self._historical(store, source_result)
            with patch(
                "tradercockpit.research_robustness_system_parameter.read_current_historical_result",
                return_value=historical,
            ):
                with self.assertRaises(ResearchRobustnessError) as caught:
                    start_native_system_parameter_permutation(
                        store,
                        home,
                        self.LAUNCHER_SHA,
                        historical_result_entity_id=self.HISTORICAL_ENTITY,
                        expected_historical_result_revision=f"tc-research-revision:historical-result:sha256:{'2' * 64}",
                        gateway_factory=lambda *args: self.fail("revision substitution reached gateway"),
                    )
            self.assertEqual(caught.exception.code, "robustness_source_revision_changed")

    def test_unchanged_native_result_is_refused(self) -> None:
        source_result = self._archive_bytes("baseline")
        project = self._project_bytes(self._task_xml())
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", project)
            store = FileResearchCustodyStore(root / "data")
            historical = self._historical(store, source_result)
            with patch(
                "tradercockpit.research_robustness_system_parameter.read_current_historical_result",
                return_value=historical,
            ):
                with self.assertRaises(ResearchRobustnessError) as caught:
                    start_native_system_parameter_permutation(
                        store,
                        home,
                        self.LAUNCHER_SHA,
                        historical_result_entity_id=self.HISTORICAL_ENTITY,
                        expected_historical_result_revision=self.HISTORICAL_REVISION,
                        gateway_factory=self._gateway_factory(None),
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
            with patch(
                "tradercockpit.research_robustness_system_parameter.read_current_historical_result",
                return_value=historical,
            ):
                with self.assertRaises(ResearchRobustnessError) as caught:
                    start_native_system_parameter_permutation(
                        store,
                        home,
                        self.LAUNCHER_SHA,
                        historical_result_entity_id=self.HISTORICAL_ENTITY,
                        expected_historical_result_revision=self.HISTORICAL_REVISION,
                        gateway_factory=self._gateway_factory("system-parameter", mutate_engine=True),
                    )
            self.assertEqual(caught.exception.code, "robustness_engine_changed_during_execution")


if __name__ == "__main__":
    unittest.main()
