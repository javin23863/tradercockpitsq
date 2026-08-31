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
        self.assertEqual(record["execution"], {"supported": False, "reason": "topology_custody_only"})

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

            def replace_after_snapshot(snapshot: bytes):
                self._write_project(home, replacement_entries)
                return original_reader(snapshot)

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


if __name__ == "__main__":
    unittest.main()
