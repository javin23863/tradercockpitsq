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

    def _nested_entries(self) -> list[tuple[str, str]]:
        return [
            (
                "config.xml",
                '<Settings><Project>'
                '<Task name="Build strategies" type="Build" active="true" taskXMLFile="Build-Task1.xml"/>'
                "</Project></Settings>",
            ),
            (
                "Build-Task1.xml",
                '<Settings>'
                '<Rankings><MaxStrategies>1000</MaxStrategies><Conditions>'
                '<Condition use="true"><Left-Side><Column-Value column="ProfitFactor" sampleType="10"/></Left-Side>'
                '<Comparator value=">"/><Right-Side><Numeric-Value value="1.3"/></Right-Side></Condition>'
                '<Condition use="false"><Left-Side><Column-Value column="SharpeRatio" sampleType="10"/></Left-Side>'
                '<Comparator value=">"/><Right-Side><Numeric-Value value="1"/></Right-Side></Condition>'
                '</Conditions></Rankings>'
                '<CrossChecks use="true">'
                '<WalkForwardOptimization use="false"><Settings>'
                '<WalkForward optimization="15" period="10" type="0"><Param1 value="20"/><Param2 value="10"/></WalkForward>'
                '<MaxTests>1000</MaxTests></Settings>'
                '<AcceptanceSettings><Conditions/></AcceptanceSettings>'
                '</WalkForwardOptimization>'
                '<WhatIf use="false"/>'
                '</CrossChecks>'
                '</Settings>',
            ),
        ]

    def test_writes_existing_text_and_indexed_condition_attributes(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, "Example Workflow", self._nested_entries())
            written = update_custom_project_settings(
                home,
                "Example Workflow",
                1,
                [
                    {"path": ["Rankings", "MaxStrategies"], "text": "500"},
                    {
                        "path": ["Rankings", "Conditions", "Condition:1", "Right-Side", "Numeric-Value"],
                        "attribute": "value",
                        "value": "1.5",
                    },
                    {
                        "path": ["CrossChecks", "WalkForwardOptimization", "Settings", "WalkForward", "Param1"],
                        "attribute": "value",
                        "value": "25",
                    },
                    {
                        "path": ["CrossChecks", "WalkForwardOptimization", "Settings", "MaxTests"],
                        "text": "250",
                    },
                ],
            )
            record = custom_project_topology_record(home, "Example Workflow")
            with self.assertRaises(SqxCustomProjectTopologyError) as missing_row:
                update_custom_project_settings(
                    home,
                    "Example Workflow",
                    1,
                    [{"path": ["Rankings", "Conditions", "Condition:3"], "attribute": "use", "value": "true"}],
                )
            with self.assertRaises(SqxCustomProjectTopologyError) as missing_text:
                update_custom_project_settings(
                    home,
                    "Example Workflow",
                    1,
                    [{"path": ["CrossChecks", "WhatIf"], "text": "invented"}],
                )
            with self.assertRaises(SqxCustomProjectTopologyError) as mixed:
                update_custom_project_settings(
                    home,
                    "Example Workflow",
                    1,
                    [{"path": ["Rankings", "MaxStrategies"], "attribute": "use", "value": "true", "text": "9"}],
                )

        self.assertEqual(written["updated"], 4)
        rankings = record["tasks"][0]["settings"][0]
        self.assertEqual(rankings["tag"], "Rankings")
        self.assertEqual(rankings["children"][0]["text"], "500")
        first = rankings["children"][1]["children"][0]
        self.assertEqual(first["path"], ["Rankings", "Conditions", "Condition:1"])
        self.assertEqual(first["children"][2]["children"][0]["attributes"]["value"], "1.5")
        self.assertEqual(first["display"]["column"], "ProfitFactor")
        wfo = record["tasks"][0]["settings"][1]["children"][0]
        self.assertEqual(wfo["children"][0]["children"][0]["children"][0]["attributes"]["value"], "25")
        self.assertEqual(wfo["children"][0]["children"][1]["text"], "250")
        what_if = record["tasks"][0]["settings"][1]["children"][1]
        self.assertEqual(what_if["tag"], "WhatIf")
        self.assertEqual(what_if["children"], [])
        self.assertEqual(missing_row.exception.code, "custom_project_settings_path_missing")
        self.assertEqual(missing_text.exception.code, "custom_project_settings_text_missing")
        self.assertEqual(mixed.exception.code, "custom_project_settings_updates_invalid")


if __name__ == "__main__":
    unittest.main()
