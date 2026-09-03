from __future__ import annotations

from hashlib import sha256
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from zipfile import ZipFile

from tradercockpit.sqx_custom_project import list_custom_projects
from tradercockpit.sqx_run_module import (
    SqxRunModuleError,
    canonical_sqx_module_name,
    read_sqx_run_module,
)


class SqxRunModuleTests(unittest.TestCase):
    def _runtime(self, root: Path) -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        return root

    def _write_project(self, home: Path, project: str, entries: list[tuple[str, str]]) -> Path:
        path = home / "user" / "projects" / project / "project.cfx"
        path.parent.mkdir(parents=True, exist_ok=True)
        with ZipFile(path, "w") as archive:
            for entry_name, payload in entries:
                archive.writestr(entry_name, payload)
        return path

    def test_canonical_names_accept_aliases_and_refuse_invented_modules(self) -> None:
        self.assertEqual(canonical_sqx_module_name("builder"), "Builder")
        self.assertEqual(canonical_sqx_module_name("Data manager"), "Data manager")
        self.assertEqual(canonical_sqx_module_name("algowizard"), "AlgoWizard")
        with self.assertRaises(SqxRunModuleError) as exc:
            canonical_sqx_module_name("Evolutionary Search")
        self.assertEqual(exc.exception.code, "sqx_module_name_invalid")

    def test_builder_is_a_run_module_not_a_custom_project(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            archive = self._write_project(
                home,
                "Builder",
                [
                    ("config.xml", "<Settings><Project/></Settings>"),
                    ("Build-Task1.xml", "<Settings><Build/></Settings>"),
                ],
            )
            (home / "user/projects/Builder/databanks/Results").mkdir(parents=True)
            (home / "user/projects/Builder/databanks/Results/sample.sqx").write_bytes(b"not-a-real-sqx")
            digest = sha256(archive.read_bytes()).hexdigest()
            record = read_sqx_run_module(home, "Builder")
            catalog = list_custom_projects(home)

        self.assertEqual(record["schema"], "tc.sqx-run-module.v1")
        self.assertEqual(record["module"], "Builder")
        self.assertEqual(record["kind"], "run")
        self.assertEqual(record["status"], "ready")
        self.assertEqual(record["project"], "Builder")
        self.assertEqual(record["source_relative_path"], "user/projects/Builder/project.cfx")
        self.assertEqual(record["archive_sha256"], digest)
        self.assertEqual(record["task_count"], 1)
        self.assertEqual(record["databank_count"], 1)
        self.assertEqual(record["control"]["available"], False)
        self.assertNotIn("Builder", [item["name"] for item in catalog["projects"]])

    def test_retester_unavailable_without_inventing_tasks(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            record = read_sqx_run_module(home, "Retester")

        self.assertEqual(record["kind"], "run")
        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "custom_project_missing")
        self.assertIn("Retester", record["detail"])
        self.assertIsNone(record["task_count"])

    def test_algowizard_reports_missing_archive_without_an_editor(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            record = read_sqx_run_module(home, "AlgoWizard")

        self.assertEqual(record["kind"], "inspect")
        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "native_module_archive_missing")
        self.assertEqual(record["editor_wired"], False)
        self.assertIn("block editor", record["detail"])

    def test_data_manager_reports_present_archive_without_a_downloader(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            archive = self._write_project(home, "DataManager", [("config.xml", "<Settings/>")])
            digest = sha256(archive.read_bytes()).hexdigest()
            record = read_sqx_run_module(home, "data-manager")

        self.assertEqual(record["module"], "Data manager")
        self.assertEqual(record["kind"], "inspect")
        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "native_module_editor_unwired")
        self.assertEqual(record["editor_wired"], False)
        self.assertEqual(record["source_relative_path"], "user/projects/DataManager/project.cfx")
        self.assertEqual(record["archive_sha256"], digest)
        self.assertIn("data downloader", record["detail"])
