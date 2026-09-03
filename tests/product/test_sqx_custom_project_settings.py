from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from zipfile import ZipFile

from tradercockpit.sqx_custom_project import (
    SqxCustomProjectTopologyError,
    custom_project_topology_record,
)
from tradercockpit.sqx_custom_project_settings import update_custom_project_settings


class SqxCustomProjectSettingsTests(unittest.TestCase):
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

    def _entries(self) -> list[tuple[str, str]]:
        return [
            (
                "config.xml",
                '<Settings><Project>'
                '<Task name="Build strategies" type="Build" active="true" taskXMLFile="Build-Task1.xml"/>'
                "</Project></Settings>",
            ),
            (
                "Build-Task1.xml",
                '<Settings><Data><Setups><Setup engine="MetaTrader5" dateFrom="2017.01.03" dateTo="2023.01.01">'
                '<Chart symbol="ES" timeframe="H1"/></Setup></Setups></Data>'
                '<WhatToBuild><BuildMode generationType="genetic"/></WhatToBuild>'
                '<MoneyManagement type="FixedSize" size="0.1"/>'
                '<CrossChecks use="true"><WhatIf use="false"/><MonteCarlo use="true"/></CrossChecks>'
                "</Settings>",
            ),
        ]

    def test_extracts_full_settings_panes_from_task_xml(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, "Example Workflow", self._entries())
            record = custom_project_topology_record(home, "Example Workflow")

        tags = [item["tag"] for item in record["tasks"][0]["settings"]]
        self.assertEqual(tags, ["Data", "WhatToBuild", "MoneyManagement", "CrossChecks"])
        setup = record["tasks"][0]["settings"][0]["children"][0]["children"][0]
        self.assertEqual(setup["path"], ["Data", "Setups", "Setup"])
        self.assertEqual(setup["attributes"]["engine"], "MetaTrader5")

    def test_writes_existing_attribute_and_refuses_invented_ones(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, "Example Workflow", self._entries())
            written = update_custom_project_settings(
                home,
                "Example Workflow",
                1,
                [{"path": ["Data", "Setups", "Setup"], "attribute": "engine", "value": "MetaTrader4"}],
            )
            record = custom_project_topology_record(home, "Example Workflow")
            with self.assertRaises(SqxCustomProjectTopologyError) as missing:
                update_custom_project_settings(
                    home,
                    "Example Workflow",
                    1,
                    [{"path": ["Data", "Setups", "Setup"], "attribute": "spread", "value": "2"}],
                )
            with self.assertRaises(SqxCustomProjectTopologyError) as invented:
                update_custom_project_settings(
                    home,
                    "Example Workflow",
                    1,
                    [{"path": ["Rankings"], "attribute": "use", "value": "true"}],
                )

        self.assertEqual(written["updated"], 1)
        self.assertEqual(written["schema"], "tc.sqx-custom-project-settings.v1")
        setup = record["tasks"][0]["settings"][0]["children"][0]["children"][0]
        self.assertEqual(setup["attributes"]["engine"], "MetaTrader4")
        self.assertEqual(record["native_setup"]["engine"], "MetaTrader4")
        self.assertEqual(missing.exception.code, "custom_project_settings_attribute_missing")
        self.assertEqual(invented.exception.code, "custom_project_settings_path_missing")

    def test_writes_existing_task_active_flag_on_config(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, "Example Workflow", self._entries())
            update_custom_project_settings(
                home,
                "Example Workflow",
                1,
                [{"target": "config", "attribute": "active", "value": "false"}],
            )
            record = custom_project_topology_record(home, "Example Workflow")
        self.assertIs(record["tasks"][0]["active"], False)

    def test_boolean_attributes_reject_non_native_strings(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, "Example Workflow", self._entries())
            with self.assertRaises(SqxCustomProjectTopologyError) as caught:
                update_custom_project_settings(
                    home,
                    "Example Workflow",
                    1,
                    [{"path": ["CrossChecks"], "attribute": "use", "value": "yes"}],
                )
        self.assertEqual(caught.exception.code, "custom_project_settings_value_invalid")


if __name__ == "__main__":
    unittest.main()
