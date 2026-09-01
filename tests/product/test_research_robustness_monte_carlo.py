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
from tradercockpit.research_robustness_monte_carlo import (
    ROBUSTNESS_METHOD_MONTE_CARLO_MANIPULATION,
    compile_monte_carlo_manipulation_project,
    read_native_monte_carlo_manipulation_result,
    start_native_monte_carlo_manipulation,
)
from tradercockpit.sqx_outputs import inspect_sqx_output_bytes


class ResearchMonteCarloManipulationTests(unittest.TestCase):
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
        system_use: str = "false",
        simulations: str = "30",
        include_target: bool = True,
    ) -> bytes:
        target = ""
        if include_target:
            target = (
                f'<MonteCarloManipulation use="{target_use}">'
                "<Settings><Methods>"
                '<Method type="RandomizeTradesOrder" use="true"><Params><Param key="Method" type="String">resampling</Param></Params></Method>'
                '<Method type="RandomlySkipTrades" use="true"><Params><Param key="Probability" type="Integer">10</Param></Params></Method>'
                f"</Methods><NumberOfSimulations>{simulations}</NumberOfSimulations></Settings>"
                '<AcceptanceSettings><Conditions CrossCheck="MonteCarloManipulation"><Condition use="true"/></Conditions></AcceptanceSettings>'
                "</MonteCarloManipulation>"
            )
        return (
            "<Settings><CrossChecks>"
            f'<RetestWithHigherPrecision use="{higher_use}"><Settings><Precision>2</Precision><Spread>3</Spread></Settings></RetestWithHigherPrecision>'
            f'{target}'
            f'<OptProfileSysParamPermutation use="{system_use}"><Settings><OptimPeriods>false</OptimPeriods><OptimExitTypes>false</OptimExitTypes><MaxTests>1</MaxTests></Settings></OptProfileSysParamPermutation>'
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

    def _gateway_factory(self, result_marker: str):
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
                target = [node for node in task.iter() if node.tag == "MonteCarloManipulation"]
                outer.assertEqual(len(target), 1)
                outer.assertEqual(target[0].attrib.get("use"), "true")
                settings = target[0].find("Settings")
                outer.assertEqual((settings.find("NumberOfSimulations").text or "").strip(), "30")
                methods = list(settings.find("Methods"))
                outer.assertEqual([node.attrib for node in methods], [
                    {"type": "RandomizeTradesOrder", "use": "true"},
                    {"type": "RandomlySkipTrades", "use": "true"},
                ])
                cross_checks = next(node for node in task.iter() if node.tag == "CrossChecks")
                outer.assertEqual(
                    [node.attrib.get("use") for node in cross_checks if node is not target[0]],
                    ["false", "false"],
                )

                result = self.home / "user/projects" / project_name / "databanks/Results/Baseline.sqx"
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

    def test_compiler_enables_existing_profile_and_preserves_observed_internal_settings(self) -> None:
        source = self._project_bytes(self._task_xml())
        compiled, plan = compile_monte_carlo_manipulation_project(source)
        self.assertNotEqual(compiled, source)
        self.assertEqual(plan["method"], ROBUSTNESS_METHOD_MONTE_CARLO_MANIPULATION)
        self.assertEqual(plan["native_settings"], {
            "NumberOfSimulations": "30",
            "Methods": [
                {"type": "RandomizeTradesOrder", "use": "true", "params": [{"key": "Method", "type": "String", "value": "resampling"}]},
                {"type": "RandomlySkipTrades", "use": "true", "params": [{"key": "Probability", "type": "Integer", "value": "10"}]},
            ],
        })
        with ZipFile(BytesIO(compiled)) as archive:
            root = ElementTree.fromstring(archive.read("Retest-Task1.xml"))
        target = [node for node in root.iter() if node.tag == "MonteCarloManipulation"]
        self.assertEqual(target[0].attrib.get("use"), "true")
        self.assertEqual(target[0].find("AcceptanceSettings/Conditions").attrib, {"CrossCheck": "MonteCarloManipulation"})

    def test_enabled_profile_preserves_exact_project_bytes(self) -> None:
        source = self._project_bytes(self._task_xml(target_use="true"), opaque=b"opaque producer bytes")
        compiled, plan = compile_monte_carlo_manipulation_project(source)
        self.assertEqual(compiled, source)
        self.assertFalse(plan["configuration_changed"])
        self.assertEqual(plan["source_project_sha256"], plan["compiled_project_sha256"])

    def test_compiler_refuses_missing_profile_or_other_enabled_crosscheck(self) -> None:
        with self.assertRaises(ResearchRobustnessError) as missing:
            compile_monte_carlo_manipulation_project(self._project_bytes(self._task_xml(include_target=False)))
        self.assertEqual(missing.exception.code, "robustness_monte_carlo_missing")
        for kwargs in ({"higher_use": "true"}, {"system_use": "true"}):
            with self.subTest(kwargs=kwargs):
                with self.assertRaises(ResearchRobustnessError) as conflict:
                    compile_monte_carlo_manipulation_project(self._project_bytes(self._task_xml(**kwargs)))
                self.assertEqual(conflict.exception.code, "robustness_other_crosscheck_enabled")

    def test_compiler_accepts_current_native_values_without_retained_value_allowlist(self) -> None:
        source = self._project_bytes(
            self._task_xml(simulations="17").replace(b"resampling", b"permutation").replace(b">10<", b">7<")
        )
        _, plan = compile_monte_carlo_manipulation_project(source)
        self.assertEqual(plan["native_settings"]["NumberOfSimulations"], "17")
        self.assertEqual(plan["native_settings"]["Methods"][0]["params"][0]["value"], "permutation")
        self.assertEqual(plan["native_settings"]["Methods"][1]["params"][0]["value"], "7")

    def test_compiler_refuses_nonpositive_or_nondecimal_simulation_count(self) -> None:
        for value in ("0", "not-int", "+30"):
            with self.subTest(value=value):
                with self.assertRaises(ResearchRobustnessError) as caught:
                    compile_monte_carlo_manipulation_project(self._project_bytes(self._task_xml(simulations=value)))
                self.assertEqual(caught.exception.code, "robustness_monte_carlo_invalid")

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
                "tradercockpit.research_robustness_monte_carlo.read_current_historical_result",
                return_value=historical,
            ):
                result = start_native_monte_carlo_manipulation(
                    store,
                    home,
                    self.LAUNCHER_SHA,
                    historical_result_entity_id=self.HISTORICAL_ENTITY,
                    expected_historical_result_revision=self.HISTORICAL_REVISION,
                    gateway_factory=self._gateway_factory("monte-carlo"),
                )

            self.assertEqual(result["schema"], ROBUSTNESS_RECORD_SCHEMA)
            self.assertEqual(result["method"], ROBUSTNESS_METHOD_MONTE_CARLO_MANIPULATION)
            self.assertEqual(result["native_settings"]["NumberOfSimulations"], "30")
            self.assertEqual(result["execution_state"], "completed")
            self.assertEqual(result["producer_outcome_state"], ROBUSTNESS_OUTCOME_UNREAD)
            self.assertEqual(result["engine_sha256"], sha256(engine).hexdigest())
            self.assertNotEqual(result["result_archive_sha256"], result["source_result_archive_sha256"])
            reopened = read_native_monte_carlo_manipulation_result(store, result["validation_ref"])
            self.assertEqual(reopened, result)

    def test_revision_substitution_is_refused_before_gateway(self) -> None:
        source_result = self._archive_bytes("baseline")
        project = self._project_bytes(self._task_xml())
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", project)
            store = FileResearchCustodyStore(root / "data")
            historical = self._historical(store, source_result)
            with patch("tradercockpit.research_robustness_monte_carlo.read_current_historical_result", return_value=historical):
                with self.assertRaises(ResearchRobustnessError) as caught:
                    start_native_monte_carlo_manipulation(
                        store,
                        home,
                        self.LAUNCHER_SHA,
                        historical_result_entity_id=self.HISTORICAL_ENTITY,
                        expected_historical_result_revision=f"tc-research-revision:historical-result:sha256:{'2' * 64}",
                        gateway_factory=lambda *args: self.fail("revision substitution reached gateway"),
                    )
            self.assertEqual(caught.exception.code, "robustness_source_revision_changed")


if __name__ == "__main__":
    unittest.main()
