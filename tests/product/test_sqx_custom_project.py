from __future__ import annotations

from hashlib import sha256
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
from zipfile import ZipFile

import tradercockpit.sqx_custom_project as custom_project
from tradercockpit.sqx_custom_project import (
    SQX_CUSTOM_PROJECT_OBSERVED_TASK_KINDS,
    SqxCustomProjectTopologyError,
    custom_project_topology_record,
    read_sqx_custom_project_topology,
)


class SqxCustomProjectTopologyTests(unittest.TestCase):
    PROJECT = "GOLD BREAKOUT M30 - Dukascopy"

    def _runtime(self, root: Path) -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        return root

    def _write_project(self, home: Path, entries: list[tuple[str, str]], *, project: str | None = None) -> Path:
        name = project or self.PROJECT
        path = home / "user" / "projects" / name / "project.cfx"
        path.parent.mkdir(parents=True, exist_ok=True)
        with ZipFile(path, "w") as archive:
            for entry_name, payload in entries:
                archive.writestr(entry_name, payload)
        return path

    def _reference_entries(self) -> list[tuple[str, str]]:
        entries: list[tuple[str, str]] = [
            ("config.xml", "<Settings><Project/></Settings>"),
            ("Build-Task1.xml", "<Settings><Build/></Settings>"),
        ]
        entries.extend((f"Retest-Task{index}.xml", "<Settings><Retest/></Settings>") for index in range(2, 8))
        entries.extend([
            ("ClearDatabanks-Task8.xml", '<Settings><ClearDatabanks><Databank name="Results"/></ClearDatabanks></Settings>'),
            ("GoToTask-Task9.xml", '<Settings><GoToTask task="Build strategies"><Task/><Conditions/></GoToTask></Settings>'),
        ])
        return entries

    def test_reads_reference_task_topology_without_claiming_execution(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            project_path = self._write_project(home, self._reference_entries())
            expected_digest = sha256(project_path.read_bytes()).hexdigest()
            record = custom_project_topology_record(home, self.PROJECT)

        self.assertEqual(record["schema"], "tc.sqx-custom-project-topology.v1")
        self.assertEqual(record["source_build"], "144.2953")
        self.assertEqual(record["project"], self.PROJECT)
        self.assertEqual(record["source_relative_path"], f"user/projects/{self.PROJECT}/project.cfx")
        self.assertEqual(record["archive_sha256"], expected_digest)
        self.assertNotIn("reference_commit", record)
        self.assertEqual(record["internal_entries"], [name for name, _ in self._reference_entries()])
        self.assertEqual([(task["native_task_index"], task["kind"]) for task in record["tasks"]], [
            (1, "Build"), (2, "Retest"), (3, "Retest"), (4, "Retest"), (5, "Retest"),
            (6, "Retest"), (7, "Retest"), (8, "ClearDatabanks"), (9, "GoToTask"),
        ])
        self.assertEqual(record["tasks"][7]["clear_databanks"], ["Results"])
        self.assertEqual(record["tasks"][8]["goto_target_label"], "Build strategies")
        self.assertIsNone(record["tasks"][0]["name"])
        self.assertIsNone(record["native_setup"])
        self.assertEqual(record["execution"]["supported"], False)
        self.assertEqual(record["execution"]["reason"], "topology_custody_only")
        self.assertEqual(record["execution"]["control"]["available"], False)
        self.assertEqual(record["execution"]["control"]["reason_code"], "trusted_launcher_not_configured")

    def test_digest_and_topology_share_one_archive_snapshot(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            project_path = self._write_project(home, self._reference_entries())
            first_snapshot = project_path.read_bytes()
            original_reader = custom_project._read_topology
            replacement_entries = [
                ("config.xml", "<Settings/>"),
                ("Build-Task1.xml", "<Settings/>"),
                ("GoToTask-Task2.xml", '<Settings><GoToTask task="Other task"/></Settings>'),
            ]

            def replace_after_snapshot(snapshot: bytes, **_kwargs):
                self._write_project(home, replacement_entries)
                return original_reader(snapshot, **_kwargs)

            with patch.object(custom_project, "_read_topology", side_effect=replace_after_snapshot):
                topology = read_sqx_custom_project_topology(home, self.PROJECT)
            replacement_digest = sha256(project_path.read_bytes()).hexdigest()

        self.assertEqual(topology.archive_sha256, sha256(first_snapshot).hexdigest())
        self.assertNotEqual(topology.archive_sha256, replacement_digest)
        self.assertEqual([(task.native_task_index, task.kind) for task in topology.tasks], [
            (1, "Build"), (2, "Retest"), (3, "Retest"), (4, "Retest"), (5, "Retest"),
            (6, "Retest"), (7, "Retest"), (8, "ClearDatabanks"), (9, "GoToTask"),
        ])

    def test_preserves_other_observed_native_task_kinds_opaquely(self) -> None:
        self.assertTrue({"Optimize", "AutomaticPortfolioBuilder"}.issubset(SQX_CUSTOM_PROJECT_OBSERVED_TASK_KINDS))
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, [
                ("config.xml", "<Settings/>"),
                ("Optimize-Task1.xml", "<Settings><Optimize/></Settings>"),
                ("AutomaticPortfolioBuilder-Task2.xml", "<Settings><AutomaticPortfolioBuilder/></Settings>"),
            ])
            topology = read_sqx_custom_project_topology(home, self.PROJECT)

        self.assertEqual([(task.native_task_index, task.kind) for task in topology.tasks], [(1, "Optimize"), (2, "AutomaticPortfolioBuilder")])
        self.assertEqual(topology.tasks[0].clear_databanks, ())
        self.assertIsNone(topology.tasks[0].goto_target_label)
        self.assertEqual(topology.tasks[1].clear_databanks, ())
        self.assertIsNone(topology.tasks[1].goto_target_label)

    def test_preserves_future_canonical_task_kind_without_inventing_semantics(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, [
                ("config.xml", "<Settings/>"),
                ("SomeNativeTask-Task1.xml", "<Settings><Opaque/></Settings>"),
            ])
            topology = read_sqx_custom_project_topology(home, self.PROJECT)

        self.assertEqual(len(topology.tasks), 1)
        self.assertEqual(topology.tasks[0].kind, "SomeNativeTask")
        self.assertEqual(topology.tasks[0].entry_name, "SomeNativeTask-Task1.xml")
        self.assertEqual(topology.tasks[0].clear_databanks, ())
        self.assertIsNone(topology.tasks[0].goto_target_label)

    def test_accepts_source_proven_empty_task_topology(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            project_path = self._write_project(home, [("config.xml", "<Settings><Project><Tasks/></Project></Settings>")], project="PortfolioComposer")
            expected_digest = sha256(project_path.read_bytes()).hexdigest()
            record = custom_project_topology_record(home, "PortfolioComposer")

        self.assertEqual(record["tasks"], [])
        self.assertEqual(record["internal_entries"], ["config.xml"])
        self.assertEqual(record["archive_sha256"], expected_digest)

    def test_rejects_ambiguous_native_task_index(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, [
                ("config.xml", "<Settings/>"),
                ("Build-Task1.xml", "<Settings/>"),
                ("Retest-Task1.xml", "<Settings/>"),
            ])
            with self.assertRaises(SqxCustomProjectTopologyError) as caught:
                read_sqx_custom_project_topology(home, self.PROJECT)
        self.assertEqual(caught.exception.code, "custom_project_task_index_ambiguous")

    def test_rejects_malformed_task_identity(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, [("config.xml", "<Settings/>"), ("Build-Task01.xml", "<Settings/>")])
            with self.assertRaises(SqxCustomProjectTopologyError) as caught:
                read_sqx_custom_project_topology(home, self.PROJECT)
        self.assertEqual(caught.exception.code, "custom_project_task_identity_invalid")

    def test_rejects_missing_control_flow_facts(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, [
                ("config.xml", "<Settings/>"),
                ("Build-Task1.xml", "<Settings/>"),
                ("ClearDatabanks-Task2.xml", "<Settings><ClearDatabanks/></Settings>"),
            ])
            with self.assertRaises(SqxCustomProjectTopologyError) as caught:
                read_sqx_custom_project_topology(home, self.PROJECT)
        self.assertEqual(caught.exception.code, "custom_project_clear_databanks_missing")

    def test_rejects_project_path_escape_and_noncanonical_name(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            for project in ("../Retester", " Retester", "Retester ", "Retester\x00"):
                with self.subTest(project=project):
                    with self.assertRaises(SqxCustomProjectTopologyError) as caught:
                        read_sqx_custom_project_topology(home, project)
                    self.assertEqual(caught.exception.code, "custom_project_name_invalid")

    def test_rejects_missing_config_and_invalid_archive(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, [("Build-Task1.xml", "<Settings/>")])
            with self.assertRaises(SqxCustomProjectTopologyError) as missing:
                read_sqx_custom_project_topology(home, self.PROJECT)
            self.assertEqual(missing.exception.code, "custom_project_config_missing")
            path = home / "user" / "projects" / self.PROJECT / "project.cfx"
            path.write_bytes(b"not-a-zip")
            with self.assertRaises(SqxCustomProjectTopologyError) as invalid:
                read_sqx_custom_project_topology(home, self.PROJECT)
        self.assertEqual(invalid.exception.code, "custom_project_archive_invalid")


class SqxCustomProjectCatalogAndSetupTests(unittest.TestCase):
    def _runtime(self, root: Path) -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        return root

    def _write_project(self, home: Path, name: str, entries: list[tuple[str, str]]) -> Path:
        path = home / "user" / "projects" / name / "project.cfx"
        path.parent.mkdir(parents=True, exist_ok=True)
        with ZipFile(path, "w") as archive:
            for entry_name, payload in entries:
                archive.writestr(entry_name, payload)
        return path

    def test_lists_real_projects_and_skips_builder_without_hard_coding_names(self) -> None:
        from tradercockpit.sqx_custom_project import list_custom_projects

        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, "Builder", [("config.xml", "<Settings/>")])
            self._write_project(
                home,
                "Example Workflow",
                [
                    (
                        "config.xml",
                        '<Settings><Project>'
                        '<Task name="Build strategies" type="Build" active="true" taskXMLFile="Build-Task1.xml"/>'
                        '<Task name="OOS" type="Retest" active="true" taskXMLFile="Retest-Task2.xml"/>'
                        "</Project></Settings>",
                    ),
                    (
                        "Build-Task1.xml",
                        '<Settings><Data><Setups><Setup engine="MetaTrader5" dateFrom="2017.01.03" dateTo="2023.01.01">'
                        '<Chart symbol="ES" timeframe="H1"/></Setup></Setups></Data>'
                        '<WhatToBuild><BuildMode generationType="genetic"/></WhatToBuild>'
                        '<MoneyManagement type="FixedSize" size="0.1"/>'
                        '<CrossChecks use="true"><WhatIf use="false"/><MonteCarlo use="true"/></CrossChecks></Settings>',
                    ),
                    ("Retest-Task2.xml", "<Settings><Retest/></Settings>"),
                ],
            )
            catalog = list_custom_projects(home)

        names = [item["name"] for item in catalog["projects"]]
        self.assertEqual(catalog["schema"], "tc.sqx-custom-projects.v1")
        self.assertEqual(names, ["Example Workflow"])
        self.assertNotIn("Builder", names)
        self.assertEqual(catalog["projects"][0]["task_count"], 2)
        self.assertEqual(catalog["projects"][0]["databank_count"], 0)
        self.assertEqual(catalog["projects"][0]["strategy_count"], 0)
        self.assertEqual(catalog["projects"][0]["engine"], "MetaTrader5")
        self.assertEqual(catalog["projects"][0]["symbol"], "ES")
        self.assertEqual(catalog["projects"][0]["timeframe"], "H1")
        self.assertFalse(catalog["control"]["available"])
        self.assertNotIn("native_tools", catalog["control"])
        self.assertEqual(catalog["control"]["reason_code"], "trusted_launcher_not_configured")
        self.assertNotIn("running", catalog["projects"][0])

    def test_catalog_attaches_running_engine_fields_only_for_active_workers(self) -> None:
        from tradercockpit.sqx_custom_project import custom_project_worker_label, list_custom_projects
        import tradercockpit.sqx_engine_progress as engine_progress

        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(
                home,
                "Example Workflow",
                [
                    (
                        "config.xml",
                        '<Settings><Project>'
                        '<Task name="Build strategies" type="Build" active="true" taskXMLFile="Build-Task1.xml"/>'
                        "</Project></Settings>",
                    ),
                    (
                        "Build-Task1.xml",
                        '<Settings><Data><Setups><Setup engine="MetaTrader5">'
                        '<Chart symbol="ES" timeframe="H1"/></Setup></Setups></Data></Settings>',
                    ),
                ],
            )
            calls: list[str] = []

            def fake_progress(_home, project, **_kwargs):
                calls.append(project)
                return {"percent": 37, "running_status": "Running"}

            original = engine_progress.read_engine_progress
            engine_progress.read_engine_progress = fake_progress
            try:
                idle = list_custom_projects(home, worker_is_active=lambda _label: False)
                running = list_custom_projects(
                    home,
                    worker_is_active=lambda label: label == custom_project_worker_label("Example Workflow"),
                )
            finally:
                engine_progress.read_engine_progress = original

        self.assertNotIn("running", idle["projects"][0])
        self.assertEqual(calls, ["Example Workflow"])
        self.assertTrue(running["projects"][0]["running"])
        self.assertEqual(running["projects"][0]["percent"], 37)
        self.assertEqual(running["projects"][0]["running_status"], "Running")

    def test_catalog_attaches_running_from_official_custom_project_stats(self) -> None:
        from tradercockpit.sqx_custom_project import list_custom_projects
        import tradercockpit.sqx_engine_progress as engine_progress

        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(
                home,
                "Example Workflow",
                [
                    (
                        "config.xml",
                        '<Settings><Project>'
                        '<Task name="Build strategies" type="Build" active="true" taskXMLFile="Build-Task1.xml"/>'
                        "</Project></Settings>",
                    ),
                    (
                        "Build-Task1.xml",
                        '<Settings><Data><Setups><Setup engine="MetaTrader5">'
                        '<Chart symbol="ES" timeframe="H1"/></Setup></Setups></Data></Settings>',
                    ),
                ],
            )
            engine_calls: list[str] = []

            def fake_progress(_home, project, **_kwargs):
                engine_calls.append(project)
                return {"percent": 99, "running_status": "should-not-use"}

            def fake_stats(_home):
                return {
                    "Example Workflow": {
                        "project": "Example Workflow",
                        "running": True,
                        "running_status": "paused",
                        "percent": 12,
                    }
                }

            original_progress = engine_progress.read_engine_progress
            original_stats = engine_progress.read_custom_project_stats
            engine_progress.read_engine_progress = fake_progress
            engine_progress.read_custom_project_stats = fake_stats
            try:
                catalog = list_custom_projects(home, worker_is_active=lambda _label: False)
            finally:
                engine_progress.read_engine_progress = original_progress
                engine_progress.read_custom_project_stats = original_stats

        self.assertEqual(engine_calls, [])
        self.assertTrue(catalog["projects"][0]["running"])
        self.assertEqual(catalog["projects"][0]["percent"], 12)
        self.assertEqual(catalog["projects"][0]["running_status"], "paused")

    def test_reads_task_names_and_native_setup_from_saved_xml(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(
                home,
                "Example Workflow",
                [
                    (
                        "config.xml",
                        '<Settings><Project>'
                        '<Task name="Build strategies" type="Build" active="true" taskXMLFile="Build-Task1.xml"/>'
                        '<Task name="OOS" type="Retest" active="false" taskXMLFile="Retest-Task2.xml"/>'
                        "</Project></Settings>",
                    ),
                    (
                        "Build-Task1.xml",
                        '<Settings><Data><Setups><Setup engine="MetaTrader5" dateFrom="2017.01.03" dateTo="2023.01.01">'
                        '<Chart symbol="ES" timeframe="H1"/></Setup></Setups></Data>'
                        '<CrossChecks use="true"><WhatIf use="false"/></CrossChecks></Settings>',
                    ),
                    ("Retest-Task2.xml", "<Settings><Retest/></Settings>"),
                ],
            )
            record = custom_project_topology_record(home, "Example Workflow")

        self.assertEqual(record["tasks"][0]["name"], "Build strategies")
        self.assertEqual(record["tasks"][1]["name"], "OOS")
        self.assertIs(record["tasks"][1]["active"], False)
        self.assertEqual(record["native_setup"]["engine"], "MetaTrader5")
        self.assertEqual(record["native_setup"]["symbol"], "ES")
        self.assertEqual(record["native_setup"]["cross_checks"], [{"name": "WhatIf", "use": False}])
        self.assertEqual(
            [item["tag"] for item in record["tasks"][0]["settings"]],
            ["Data", "CrossChecks"],
        )

    def test_marks_unreadable_archives_unresolved_instead_of_inventing_rows(self) -> None:
        from tradercockpit.sqx_custom_project import list_custom_projects

        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            path = home / "user" / "projects" / "Broken" / "project.cfx"
            path.parent.mkdir(parents=True)
            path.write_bytes(b"not-a-zip")
            catalog = list_custom_projects(home)

        self.assertEqual(catalog["projects"][0]["name"], "Broken")
        self.assertEqual(catalog["projects"][0]["status"], "unresolved")
        self.assertEqual(catalog["projects"][0]["reason_code"], "custom_project_archive_invalid")

    def test_empty_projects_root_is_a_ready_empty_catalog(self) -> None:
        from tradercockpit.sqx_custom_project import list_custom_projects

        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            catalog = list_custom_projects(home)
        self.assertEqual(catalog["projects"], [])
        self.assertEqual(catalog["status"], "ready")

    def test_lists_real_databank_archives_without_inventing_metrics(self) -> None:
        from tradercockpit.sqx_custom_project import list_custom_project_results, list_custom_projects

        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, "Example Workflow", [("config.xml", "<Settings/>")])
            bank = home / "user" / "projects" / "Example Workflow" / "databanks" / "Results"
            bank.mkdir(parents=True)
            archive_path = bank / "Example.sqx"
            with ZipFile(archive_path, "w") as archive:
                archive.writestr("settings.xml", b"<Settings><Symbol>ES</Symbol></Settings>")
                archive.writestr("strategy_Portfolio.xml", b"<Strategy><Rule>native-sqx</Rule></Strategy>")
                archive.writestr("version.txt", b"144.2953")
            payload = list_custom_project_results(home, "Example Workflow")
            catalog = list_custom_projects(home)

        self.assertEqual(payload["schema"], "tc.sqx-custom-project-results.v1")
        self.assertEqual(payload["project"], "Example Workflow")
        self.assertEqual(payload["databank_count"], 1)
        self.assertEqual(payload["strategy_count"], 1)
        self.assertEqual(payload["projects"][0]["databanks"][0]["name"], "Results")
        strategy = payload["projects"][0]["databanks"][0]["strategies"][0]
        self.assertEqual(strategy["archive"], "Example.sqx")
        self.assertTrue(strategy["inspectable"])
        self.assertEqual(strategy["native_version"], "144.2953")
        self.assertEqual(catalog["projects"][0]["databank_count"], 1)
        self.assertEqual(catalog["projects"][0]["strategy_count"], 1)

    def test_lists_older_databank_archives_that_the_runtime_can_open(self) -> None:
        from tradercockpit.sqx_custom_project import list_custom_project_results

        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, "Example Workflow", [("config.xml", "<Settings/>")])
            bank = home / "user" / "projects" / "Example Workflow" / "databanks" / "Results"
            bank.mkdir(parents=True)
            with ZipFile(bank / "dow 1 hr.sqx", "w") as archive:
                archive.writestr("settings.xml", b"<ResultsGroup ResultName='dow 1 hr'/>")
                archive.writestr("strategy_Portfolio.xml", b"<Strategy><Rule>native-sqx</Rule></Strategy>")
                archive.writestr("version.txt", b"1")
            payload = list_custom_project_results(home, "Example Workflow")
        strategy = payload["projects"][0]["databanks"][0]["strategies"][0]
        self.assertEqual(strategy["archive"], "dow 1 hr.sqx")
        self.assertTrue(strategy["inspectable"])
        self.assertEqual(strategy["native_version"], "1")

    def test_control_fails_closed_without_inventing_mcp(self) -> None:
        from tradercockpit.sqx_custom_project import (
            SqxCustomProjectControlError,
            custom_project_control,
        )

        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, "Example Workflow", [("config.xml", "<Settings/>")])
            with self.assertRaises(SqxCustomProjectControlError) as caught:
                custom_project_control(home, "Example Workflow", "run_project")
            with self.assertRaises(SqxCustomProjectControlError) as invalid:
                custom_project_control(home, "Example Workflow", "launch")
        self.assertEqual(caught.exception.code, "trusted_launcher_not_configured")
        self.assertEqual(invalid.exception.code, "custom_project_action_invalid")

    def test_default_topology_omits_building_block_rows(self) -> None:
        first = (
            '<Block key="B0" use="true">'
            '<Generated weight="1"><Param key="Period" type="int"/></Generated>'
            "</Block>"
        )
        rest = "".join(f'<Block key="B{index}" use="true"/>' for index in range(1, 20))
        blocks = f"<Blocks><BuildingBlocks>{first}{rest}</BuildingBlocks><OrderTypes/></Blocks>"
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(
                home,
                "Example",
                [
                    (
                        "config.xml",
                        '<Settings><Project><Task name="Build" type="Build" active="true" taskXMLFile="Build-Task1.xml"/></Project></Settings>',
                    ),
                    ("Build-Task1.xml", f"<Settings>{blocks}</Settings>"),
                ],
            )
            slim = custom_project_topology_record(home, "Example")
            listed = custom_project_topology_record(home, "Example", include_building_blocks=True)
            opened = custom_project_topology_record(
                home,
                "Example",
                include_building_blocks=True,
                expand_block="Blocks/BuildingBlocks/Block:1",
            )
        building = next(
            child
            for section in slim["tasks"][0]["settings"]
            if section["tag"] == "Blocks"
            for child in section["children"]
            if child["tag"] == "BuildingBlocks"
        )
        self.assertEqual(building["children"], [])
        self.assertEqual(building["child_count"], 20)
        listed_building = next(
            child
            for section in listed["tasks"][0]["settings"]
            if section["tag"] == "Blocks"
            for child in section["children"]
            if child["tag"] == "BuildingBlocks"
        )
        self.assertEqual(len(listed_building["children"]), 20)
        self.assertEqual(listed_building["children"][0]["children"], [])
        self.assertEqual(listed_building["children"][0]["child_count"], 1)
        opened_building = next(
            child
            for section in opened["tasks"][0]["settings"]
            if section["tag"] == "Blocks"
            for child in section["children"]
            if child["tag"] == "BuildingBlocks"
        )
        self.assertEqual(opened_building["children"][0]["children"][0]["tag"], "Generated")
        self.assertEqual(opened_building["children"][0]["children"][0]["children"][0]["attributes"]["key"], "Period")
        self.assertEqual(opened_building["children"][1]["children"], [])

    def test_pause_calls_sqx_project_pause(self) -> None:
        from tradercockpit.sqx_custom_project import custom_project_control

        calls: list[tuple[str, str, dict[str, str] | None]] = []

        def fake_json(_home, path, *, method="GET", fields=None, **_kwargs):
            calls.append((path, method, fields))
            return {"success": True}

        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, "Example Workflow", [("config.xml", "<Settings/>")])
            with patch("tradercockpit.sqx_native_web.sqx_local_json", side_effect=fake_json):
                receipt = custom_project_control(home, "Example Workflow", "pause_project")
        self.assertEqual(receipt["native_action"], "pause")
        self.assertEqual(calls, [("/project/pause", "GET", {"projectName": "Example Workflow"})])

    def test_start_posts_project_start_when_sqx_web_is_open(self) -> None:
        from tradercockpit.sqx_custom_project import custom_project_control

        calls: list[tuple[str, str, dict[str, str] | None]] = []

        def fake_json(_home, path, *, method="GET", fields=None, **_kwargs):
            calls.append((path, method, fields))
            return {"success": "Project execution started."}

        spawned: list[object] = []
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, "Example Workflow", [("config.xml", "<Settings/>")])
            with patch("tradercockpit.sqx_native_web.sqx_local_json", side_effect=fake_json):
                receipt = custom_project_control(
                    home,
                    "Example Workflow",
                    "run_project",
                    trusted_launcher_sha256="ab" * 32,
                    register_worker=lambda *_args, **_kwargs: None,
                    process_factory=lambda *args, **kwargs: spawned.append((args, kwargs)),
                )
        self.assertEqual(receipt["native_action"], "start")
        self.assertEqual(receipt["detail"], "Requested StrategyQuant X project/start.")
        self.assertEqual(calls[-1], ("/project/start", "POST", {"projectName": "Example Workflow"}))
        self.assertEqual(spawned, [])

    def test_start_refuses_when_custom_project_stats_say_running(self) -> None:
        from tradercockpit.sqx_custom_project import (
            SqxCustomProjectControlError,
            custom_project_control,
        )

        spawned: list[object] = []
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, "Example Workflow", [("config.xml", "<Settings/>")])
            with patch(
                "tradercockpit.sqx_engine_progress.read_custom_project_stats",
                return_value={
                    "Example Workflow": {
                        "project": "Example Workflow",
                        "running": True,
                        "running_status": "running",
                    }
                },
            ):
                with self.assertRaises(SqxCustomProjectControlError) as caught:
                    custom_project_control(
                        home,
                        "Example Workflow",
                        "run_project",
                        trusted_launcher_sha256="ab" * 32,
                        register_worker=lambda *_args, **_kwargs: None,
                        process_factory=lambda *args, **kwargs: spawned.append((args, kwargs)),
                    )
        self.assertEqual(caught.exception.code, "native_project_already_running")
        self.assertEqual(spawned, [])

    def test_stop_gets_project_stop_when_sqx_web_is_open(self) -> None:
        from tradercockpit.sqx_custom_project import custom_project_control

        calls: list[tuple[str, str, dict[str, str] | None]] = []

        def fake_json(_home, path, *, method="GET", fields=None, **_kwargs):
            calls.append((path, method, fields))
            return {"success": "Project execution stopped."}

        ran: list[object] = []
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, "Example Workflow", [("config.xml", "<Settings/>")])
            with patch("tradercockpit.sqx_native_web.sqx_local_json", side_effect=fake_json):
                receipt = custom_project_control(
                    home,
                    "Example Workflow",
                    "stop_project",
                    trusted_launcher_sha256="ab" * 32,
                    runner=lambda *args, **kwargs: ran.append((args, kwargs)),
                )
        self.assertEqual(receipt["native_action"], "stop")
        self.assertEqual(calls, [("/project/stop", "GET", {"projectName": "Example Workflow"})])
        self.assertEqual(ran, [])

    def test_start_does_not_fall_back_to_cli_when_web_refuses(self) -> None:
        from tradercockpit.sqx_custom_project import (
            SqxCustomProjectControlError,
            custom_project_control,
        )
        from tradercockpit.sqx_native_web import SqxNativeWebError

        spawned: list[object] = []

        def fake_json(*_args, **_kwargs):
            raise SqxNativeWebError("sqx_web_refused", "StrategyQuant X local web returned HTTP 500.")

        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, "Example Workflow", [("config.xml", "<Settings/>")])
            with patch("tradercockpit.sqx_native_web.sqx_local_json", side_effect=fake_json):
                with self.assertRaises(SqxCustomProjectControlError) as caught:
                    custom_project_control(
                        home,
                        "Example Workflow",
                        "run_project",
                        trusted_launcher_sha256="ab" * 32,
                        register_worker=lambda *_args, **_kwargs: None,
                        process_factory=lambda *args, **kwargs: spawned.append((args, kwargs)),
                    )
        self.assertEqual(caught.exception.code, "sqx_web_refused")
        self.assertEqual(spawned, [])


if __name__ == "__main__":
    unittest.main()
