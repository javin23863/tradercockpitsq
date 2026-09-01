from __future__ import annotations

from hashlib import sha256
from io import BytesIO
import json
import zlib
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Event, Thread
import unittest
from unittest.mock import patch
from xml.etree import ElementTree
from zipfile import ZipFile

from tradercockpit.research_custody import FileResearchCustodyStore, ResearchCustodyError, ResearchEntityId, ResearchRevisionRef
from tradercockpit.research_retester import NativeRetesterContent, ResearchRetesterError, read_historical_result_revision
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
    def _task_xml(*, higher_use: str = "false", other_use: str | None = "false", include_higher: bool = True, include_precision: bool = True) -> bytes:
        higher = ""
        if include_higher:
            precision = "<Precision>2</Precision>" if include_precision else ""
            higher = (
                f'<RetestWithHigherPrecision use="{higher_use}">'
                f"<Settings>{precision}<Spread>3</Spread></Settings>"
                "<AcceptanceSettings/>"
                "</RetestWithHigherPrecision>"
            )
        other_attr = "" if other_use is None else f' use="{other_use}"'
        return (
            "<Settings>"
            "<CrossChecks>"
            f"{higher}"
            f"<MonteCarloManipulation{other_attr}><Settings/></MonteCarloManipulation>"
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
        candidate = self._archive_bytes("historical-candidate")
        candidate_info = inspect_sqx_output_bytes(candidate, archive_name="Candidate.sqx")
        candidate_ref = store.put_evidence(candidate)
        source_project = self._project_bytes(self._task_xml())
        source_project_ref = store.put_evidence(source_project)
        engine = b"historical Retester engine"
        engine_ref = store.put_evidence(engine)
        result_ref = store.put_evidence(source)
        inspected = inspect_sqx_output_bytes(source, archive_name="Baseline.sqx")
        with ZipFile(BytesIO(source)) as archive:
            strategy = archive.read("strategy_Portfolio.xml")
            settings = archive.read("settings.xml")
        strategy_ref = store.put_evidence(strategy)
        settings_ref = store.put_evidence(settings)

        entity = ResearchEntityId.parse(self.HISTORICAL_ENTITY)
        project_name = "TraderCockpit-Retester-22222222222242228222222222222222"
        candidate_entity = "tc-research:candidate:v1:33333333-3333-4333-8333-333333333333"
        candidate_revision = f"tc-research-revision:candidate:sha256:{'4' * 64}"
        prepared = NativeRetesterContent(
            state="prepared",
            candidate_entity_id=candidate_entity,
            candidate_revision=candidate_revision,
            candidate_archive_name="Candidate.sqx",
            candidate_archive_ref=candidate_ref,
            candidate_archive_sha256=candidate_info["archive_sha256"],
            sqx_build="144.2953",
            operation="native_retester_task_1",
            retester_task=1,
            native_project_name=project_name,
            native_project_relative_path=f"user/projects/{project_name}/project.cfx",
            source_project_ref=source_project_ref,
            source_project_sha256=sha256(source_project).hexdigest(),
            engine_ref=engine_ref,
            engine_sha256=sha256(engine).hexdigest(),
            launcher_sha256=None,
            receipts=(),
            partial_side_effect=False,
        )
        prepared_revision = store.create_revision(
            entity,
            prepared.canonical_bytes(),
            evidence=(candidate_ref, source_project_ref, engine_ref),
        )
        store.compare_and_set_current(entity, expected_revision=None, target_revision=prepared_revision.revision)

        completed = NativeRetesterContent(
            state="completed",
            candidate_entity_id=candidate_entity,
            candidate_revision=candidate_revision,
            candidate_archive_name="Candidate.sqx",
            candidate_archive_ref=candidate_ref,
            candidate_archive_sha256=candidate_info["archive_sha256"],
            sqx_build="144.2953",
            operation="native_retester_task_1",
            retester_task=1,
            native_project_name=project_name,
            native_project_relative_path=f"user/projects/{project_name}/project.cfx",
            source_project_ref=source_project_ref,
            source_project_sha256=sha256(source_project).hexdigest(),
            engine_ref=engine_ref,
            engine_sha256=sha256(engine).hexdigest(),
            launcher_sha256=self.LAUNCHER_SHA,
            receipts=({"action": "startOnlyTask", "task": 1, "state": "completed", "project": project_name},),
            partial_side_effect=False,
            result_archive_name="Baseline.sqx",
            result_archive_relative_path=f"user/projects/{project_name}/databanks/Results/Baseline.sqx",
            result_archive_ref=result_ref,
            result_archive_sha256=inspected["archive_sha256"],
            result_strategy_ref=strategy_ref,
            result_strategy_sha256=sha256(strategy).hexdigest(),
            result_settings_ref=settings_ref,
            result_settings_sha256=sha256(settings).hexdigest(),
        )
        completed_revision = store.create_revision(
            entity,
            completed.canonical_bytes(),
            parent_revision=prepared_revision.revision,
            evidence=(candidate_ref, source_project_ref, engine_ref, result_ref, strategy_ref, settings_ref),
        )
        store.compare_and_set_current(
            entity,
            expected_revision=prepared_revision.revision,
            target_revision=completed_revision.revision,
        )
        self.HISTORICAL_REVISION = str(completed_revision.revision)
        return read_historical_result_revision(store, entity, completed_revision.revision)

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

    def test_compiler_treats_any_non_false_other_crosscheck_as_enabled(self) -> None:
        for other_use in (None, "1", "True"):
            with self.subTest(other_use=other_use):
                source = self._project_bytes(self._task_xml(other_use=other_use))
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

            entity = ResearchEntityId.parse(result["proof_entity_id"])
            current_ref = store.current(entity)
            current = store.read_revision(current_ref)
            garbage = store.create_revision(
                entity,
                b"not-json",
                parent_revision=current.parent_revision,
                evidence=current.evidence,
            )
            store.compare_and_set_current(entity, expected_revision=current_ref, target_revision=garbage.revision)
            with self.assertRaises(ResearchRobustnessError) as corrupt_caught:
                list_native_robustness_results(store)
            self.assertEqual(corrupt_caught.exception.code, "robustness_proof_catalog_corrupt")

    def test_proof_readback_requires_existing_historical_source_revision(self) -> None:
        source_result = self._archive_bytes("baseline")
        project = self._project_bytes(self._task_xml())
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", project)
            store = FileResearchCustodyStore(root / "data")
            historical = self._historical(store, source_result)
            with patch("tradercockpit.research_robustness.read_current_historical_result", return_value=historical):
                result = start_native_higher_precision(
                    store, home, self.LAUNCHER_SHA,
                    historical_result_entity_id=self.HISTORICAL_ENTITY,
                    expected_historical_result_revision=self.HISTORICAL_REVISION,
                    gateway_factory=self._gateway_factory(home, "higher-precision"),
                )
            historical_revision = ResearchRevisionRef.parse(self.HISTORICAL_REVISION)
            revision_path = store.base / "revisions" / historical_revision.kind.value / historical_revision.digest[:2] / f"{historical_revision.digest}.json"
            revision_path.unlink()
            with self.assertRaises(ResearchRobustnessError) as caught:
                read_native_robustness_result(store, result["validation_ref"])
            self.assertEqual(caught.exception.code, "robustness_proof_catalog_corrupt")


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
            omitted_home = self._runtime(root / "omitted", self._project_bytes(self._task_xml(other_use=None)))
            omitted = read_native_robustness_capabilities(omitted_home)
            self.assertEqual(omitted["methods"][0]["state"], "unavailable")
            self.assertEqual(omitted["methods"][0]["reason_code"], "robustness_other_crosscheck_enabled")

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
                            "result_archive_name": result_archive_name,
                            "result_archive_relative_path": f"user/projects/{project_name}/databanks/Results/{result_archive_name}",
                            "result_archive_sha256": expected_result_archive_sha256,
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
            self.assertRegex(caught.exception.attempt_ref or "", r"^tc-evidence:sha256:[0-9a-f]{64}$")
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
            self.assertEqual(caught.exception.attempt_ref, attempt["attempt_ref"])
            self.assertEqual(read_native_robustness_result(store, attempt["attempt_ref"]), attempt)

            pointer_path = next((store.base / "current" / "proof").glob("*.json"))
            pointer = json.loads(pointer_path.read_text(encoding="utf-8"))
            current_ref = ResearchRevisionRef.parse(pointer["revision"])
            current = store.read_revision(current_ref)
            forged = dict(json.loads(store.read_revision_content(current_ref)))
            forged["receipts"] = [{**forged["receipts"][0], "engine_sha256": "0" * 64}]
            forged_revision = store.create_revision(
                current.entity_id,
                json.dumps(forged, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8"),
                parent_revision=current.parent_revision,
                evidence=current.evidence,
            )
            store.compare_and_set_current(current.entity_id, expected_revision=current_ref, target_revision=forged_revision.revision)
            with self.assertRaises(ResearchRobustnessError) as forged_caught:
                list_native_robustness_results(store)
            self.assertEqual(forged_caught.exception.code, "robustness_proof_catalog_corrupt")

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


    def test_malformed_success_receipt_is_normalized_to_durable_invalid_receipt(self) -> None:
        source_result = self._archive_bytes("baseline")
        project = self._project_bytes(self._task_xml())
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", project)
            store = FileResearchCustodyStore(root / "data")
            historical = self._historical(store, source_result)
            ValidGateway = self._gateway_factory(home, "higher-precision")

            class MalformedGateway(ValidGateway):
                def launch_retester_task(self, *args, **kwargs):
                    result = super().launch_retester_task(*args, **kwargs)
                    result["launcher_sha256"] = "not-a-digest"
                    return result

            with patch("tradercockpit.research_robustness.read_current_historical_result", return_value=historical):
                with self.assertRaises(ResearchRobustnessError) as caught:
                    start_native_higher_precision(
                        store, home, self.LAUNCHER_SHA,
                        historical_result_entity_id=self.HISTORICAL_ENTITY,
                        expected_historical_result_revision=self.HISTORICAL_REVISION,
                        gateway_factory=MalformedGateway,
                    )
            self.assertEqual(caught.exception.code, "robustness_receipt_invalid")
            catalog = list_native_robustness_results(store)
            self.assertEqual(len(catalog["failed_attempts"]), 1)
            attempt = catalog["failed_attempts"][0]
            self.assertEqual(attempt["state"], "failed")
            self.assertEqual(attempt["failure_reason_code"], "robustness_receipt_invalid")
            self.assertTrue(attempt["partial_side_effect"])
            self.assertEqual(attempt["receipts"][0]["state"], "invalid_receipt")
            self.assertEqual(attempt["receipts"][0]["project_sha256"], attempt["compiled_project_sha256"])
            self.assertEqual(attempt["receipts"][0]["engine_sha256"], attempt["engine_sha256"])
            self.assertEqual(attempt["receipts"][0]["result_archive_sha256"], attempt["source_result_archive_sha256"])
            self.assertEqual(read_native_robustness_result(store, attempt["attempt_ref"]), attempt)


    def test_active_prepared_proof_is_not_reported_as_interrupted(self) -> None:
        source_result = self._archive_bytes("baseline")
        project = self._project_bytes(self._task_xml())
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", project)
            store = FileResearchCustodyStore(root / "data")
            historical = self._historical(store, source_result)
            BaseGateway = self._gateway_factory(home, "higher-precision")
            entered = Event()
            release = Event()

            class BlockingGateway(BaseGateway):
                def launch_retester_task(self, *args, **kwargs):
                    entered.set()
                    if not release.wait(5):
                        raise AssertionError("blocking gateway was not released")
                    return super().launch_retester_task(*args, **kwargs)

            outcome: dict[str, object] = {}

            def worker() -> None:
                try:
                    outcome["result"] = start_native_higher_precision(
                        store, home, self.LAUNCHER_SHA,
                        historical_result_entity_id=self.HISTORICAL_ENTITY,
                        expected_historical_result_revision=self.HISTORICAL_REVISION,
                        gateway_factory=BlockingGateway,
                    )
                except BaseException as exc:  # pragma: no cover - asserted below
                    outcome["error"] = exc

            with patch("tradercockpit.research_robustness.read_current_historical_result", return_value=historical):
                thread = Thread(target=worker)
                thread.start()
                self.assertTrue(entered.wait(5))
                catalog = list_native_robustness_results(store)
                self.assertEqual(catalog["results"], [])
                self.assertEqual(catalog["failed_attempts"], [])
                release.set()
                thread.join(5)
            self.assertFalse(thread.is_alive())
            self.assertNotIn("error", outcome)
            self.assertEqual(len(list_native_robustness_results(store)["results"]), 1)

    def test_prepared_proof_left_by_uncaught_termination_reopens_as_interrupted(self) -> None:
        source_result = self._archive_bytes("baseline")
        project = self._project_bytes(self._task_xml())
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", project)
            store = FileResearchCustodyStore(root / "data")
            historical = self._historical(store, source_result)

            class TerminatingGateway:
                def __init__(self, sqx_home, trusted_launcher_sha256):
                    self.home = Path(sqx_home)

                def launch_retester_task(self, project_name, **kwargs):
                    marker = self.home / "user/projects" / project_name / "native-started.marker"
                    marker.write_text("native launch may have started", encoding="utf-8")
                    raise KeyboardInterrupt("simulated process termination")

            with patch("tradercockpit.research_robustness.read_current_historical_result", return_value=historical):
                with self.assertRaises(KeyboardInterrupt):
                    start_native_higher_precision(
                        store, home, self.LAUNCHER_SHA,
                        historical_result_entity_id=self.HISTORICAL_ENTITY,
                        expected_historical_result_revision=self.HISTORICAL_REVISION,
                        gateway_factory=TerminatingGateway,
                    )
            catalog = list_native_robustness_results(store)
            self.assertEqual(catalog["results"], [])
            self.assertEqual(len(catalog["failed_attempts"]), 1)
            attempt = catalog["failed_attempts"][0]
            self.assertEqual(attempt["state"], "interrupted")
            self.assertEqual(attempt["failure_reason_code"], "robustness_attempt_interrupted")
            self.assertTrue(attempt["partial_side_effect"])
            self.assertIsNone(attempt["launcher_sha256"])
            self.assertEqual(attempt["receipts"], [])
            self.assertEqual(read_native_robustness_result(store, attempt["attempt_ref"]), attempt)

    def test_exact_historical_revision_reader_rejects_minimal_subset_content(self) -> None:
        source = self._archive_bytes("baseline")
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            ref = store.put_evidence(source)
            inspected = inspect_sqx_output_bytes(source, archive_name="Baseline.sqx")
            entity = ResearchEntityId.parse(self.HISTORICAL_ENTITY)
            content = json.dumps({
                "schema": "tc.research-historical-result-content.v1",
                "state": "completed",
                "result_archive_ref": str(ref),
                "result_archive_sha256": inspected["archive_sha256"],
            }, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")
            revision = store.create_revision(entity, content, evidence=(ref,))
            with self.assertRaises(ResearchRetesterError) as caught:
                read_historical_result_revision(store, entity, revision.revision)
            self.assertEqual(caught.exception.code, "historical_result_content_corrupt")

    def test_installed_retester_source_redirect_is_refused_before_native_execution(self) -> None:
        source_result = self._archive_bytes("baseline")
        project = self._project_bytes(self._task_xml())
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", project)
            store = FileResearchCustodyStore(root / "data")
            historical = self._historical(store, source_result)
            alternate = home / "user/projects/Alternate"
            alternate.mkdir(parents=True)
            alternate_file = alternate / "project.cfx"
            alternate_file.write_bytes(project)
            source_file = home / "user/projects/Retester/project.cfx"
            source_file.unlink()
            try:
                source_file.symlink_to(alternate_file)
            except OSError as exc:
                self.skipTest(f"symlink creation unavailable: {exc}")

            capability = read_native_robustness_capabilities(home)
            self.assertEqual(capability["methods"][0]["state"], "unavailable")
            self.assertEqual(capability["methods"][0]["reason_code"], "retester_source_project_path_escape")
            with patch("tradercockpit.research_robustness.read_current_historical_result", return_value=historical):
                with self.assertRaises(ResearchRobustnessError) as caught:
                    start_native_higher_precision(
                        store, home, self.LAUNCHER_SHA,
                        historical_result_entity_id=self.HISTORICAL_ENTITY,
                        expected_historical_result_revision=self.HISTORICAL_REVISION,
                        gateway_factory=lambda *args, **kwargs: self.fail("redirected source reached native gateway"),
                    )
            self.assertEqual(caught.exception.code, "retester_source_project_path_escape")

    def test_compressed_result_read_failure_persists_completed_native_receipt(self) -> None:
        source_result = self._archive_bytes("baseline")
        project = self._project_bytes(self._task_xml())
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", project)
            store = FileResearchCustodyStore(root / "data")
            historical = self._historical(store, source_result)
            with patch("tradercockpit.research_robustness.read_current_historical_result", return_value=historical):
                with patch("tradercockpit.research_retester.inspect_sqx_output_bytes", side_effect=zlib.error("corrupt compressed member")):
                    with self.assertRaises(ResearchRobustnessError) as caught:
                        start_native_higher_precision(
                            store, home, self.LAUNCHER_SHA,
                            historical_result_entity_id=self.HISTORICAL_ENTITY,
                            expected_historical_result_revision=self.HISTORICAL_REVISION,
                            gateway_factory=self._gateway_factory(home, "higher-precision"),
                        )
            self.assertEqual(caught.exception.code, "retester_result_corrupt")
            self.assertRegex(caught.exception.attempt_ref or "", r"^tc-evidence:sha256:[0-9a-f]{64}$")
            failed = self._current_proof_payload(store)
            self.assertEqual(failed["state"], "failed")
            self.assertEqual(failed["failure_reason_code"], "retester_result_corrupt")
            self.assertTrue(failed["partial_side_effect"])
            self.assertEqual(failed["launcher_sha256"], self.LAUNCHER_SHA)
            self.assertEqual(failed["receipts"][0]["state"], "completed")
            reopened = read_native_robustness_result(store, caught.exception.attempt_ref)
            self.assertEqual(reopened["failure_reason_code"], "retester_result_corrupt")
            self.assertEqual(reopened["receipts"][0]["state"], "completed")

    def test_invalid_validation_ref_is_typed(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            with self.assertRaises(ResearchRobustnessError) as caught:
                read_native_robustness_result(store, "not-an-evidence-ref")
            self.assertEqual(caught.exception.code, "robustness_record_ref_invalid")


if __name__ == "__main__":
    unittest.main()
