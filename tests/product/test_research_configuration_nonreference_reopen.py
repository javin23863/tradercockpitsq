from __future__ import annotations

from hashlib import sha1, sha256
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from zipfile import ZipFile

from tradercockpit.research_configurations import (
    approve_configuration,
    compile_current_builder_configuration,
    read_current_configuration,
)
from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.research_native_jobs import (
    launch_approved_builder_configuration,
    read_current_native_job,
)
from tradercockpit.sqx_builder_config import SQX_BUILDER_PROJECT_RELATIVE_PATH


HISTORICAL_BUILDER_BLOB_SHA1 = "6194322a7a6feab40e02d9d9ed741401749a51d1"
HISTORICAL_BUILDER_SIZE = 47153


def _git_blob_sha1(value: bytes) -> str:
    return sha1(f"blob {len(value)}\0".encode("ascii") + value, usedforsecurity=False).hexdigest()


class ResearchConfigurationNonreferenceReopenTests(unittest.TestCase):
    def _runtime_with_saved_setting_variant(self, root: Path) -> tuple[Path, bytes]:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        project = root / SQX_BUILDER_PROJECT_RELATIVE_PATH
        project.parent.mkdir(parents=True)
        with ZipFile(project, "w") as archive:
            archive.writestr(
                "config.xml",
                '<Project><Chart symbol="EURUSD_M1_dukas" timeframe="M30"/>'
                '<InstrumentInfo instrument="EURUSD_dukascopy"/></Project>',
            )
            archive.writestr(
                "Build-Task1.xml",
                """<Task>
                  <WhatToBuild><StrategyType type="simple"/><MarketSides type="both"/><BuildMode generationType="random-generation"/></WhatToBuild>
                  <Data><Setups><Setup dateFrom="2020.01.01" dateTo="2024.01.01" testPrecision="2" engine="0" slippage="1" minDist="0"><Chart symbol="EURUSD_M1_dukas" timeframe="M30" spread="2"/></Setup></Setups></Data>
                  <Options><BuildTradingOptions/></Options>
                  <Blocks/>
                  <MoneyManagement/>
                  <Rankings><MaxStrategies>501</MaxStrategies><StopCondition type="passed-count"/></Rankings>
                  <CrossChecks use="false"/>
                  <InstrumentInfo instrument="EURUSD_dukascopy"/>
                </Task>""",
            )
        return root, project.read_bytes()

    def test_structurally_valid_saved_variant_compiles_reopens_approves_and_is_launch_eligible(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home, project_bytes = self._runtime_with_saved_setting_variant(root / "sqx")
            self.assertNotEqual(len(project_bytes), HISTORICAL_BUILDER_SIZE)
            self.assertNotEqual(_git_blob_sha1(project_bytes), HISTORICAL_BUILDER_BLOB_SHA1)

            data_root = root / "data"
            compiled = compile_current_builder_configuration(
                FileResearchCustodyStore(data_root),
                home,
            )

            reopened_store = FileResearchCustodyStore(data_root)
            reopened = read_current_configuration(reopened_store, compiled["entity_id"])
            self.assertEqual(reopened["revision"], compiled["revision"])
            approved = approve_configuration(
                reopened_store,
                entity_id=compiled["entity_id"],
                expected_revision=compiled["revision"],
            )

            test_case = self

            class Gateway:
                def __init__(self, sqx_home, trusted_launcher_sha256):
                    self.home = Path(sqx_home)
                    self.launcher_sha256 = trusted_launcher_sha256

                def launch_builder(self, config_path, *, expected_config_sha256):
                    config = Path(config_path)
                    exact = config.read_bytes()
                    test_case.assertEqual(sha256(exact).hexdigest(), expected_config_sha256)
                    relative = config.relative_to(self.home).as_posix()
                    receipts = [
                        {
                            "sequence": index,
                            "action": action,
                            "project": "Builder",
                            "state": "completed",
                            "exit_code": 0,
                            "sqx_build": "144.2953",
                            "launcher_sha256": self.launcher_sha256,
                            "config_sha256": expected_config_sha256,
                            "reason_code": None,
                        }
                        for index, action in enumerate(("loadconfig", "start"), start=1)
                    ]
                    return {
                        "schema": "tc.sqx-native-control.v1",
                        "operation": "builder_loadconfig_start",
                        "project": "Builder",
                        "state": "submitted",
                        "sqx_build": "144.2953",
                        "launcher_sha256": self.launcher_sha256,
                        "config_relative_path": relative,
                        "config_sha256": expected_config_sha256,
                        "receipts": receipts,
                    }

            launched = launch_approved_builder_configuration(
                reopened_store,
                home,
                "a" * 64,
                configuration_entity_id=approved["entity_id"],
                expected_configuration_revision=approved["revision"],
                gateway_factory=Gateway,
            )
            self.assertEqual(launched["state"], "submitted")
            self.assertEqual(launched["configuration_revision"], approved["revision"])
            self.assertEqual(launched["executable_xml_sha256"], approved["executable_xml_sha256"])

            restarted_store = FileResearchCustodyStore(data_root)
            self.assertEqual(
                read_current_configuration(restarted_store, approved["entity_id"])["revision"],
                approved["revision"],
            )
            self.assertEqual(
                read_current_native_job(restarted_store, launched["entity_id"])["revision"],
                launched["revision"],
            )


if __name__ == "__main__":
    unittest.main()
