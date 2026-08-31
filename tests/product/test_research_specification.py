from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from zipfile import ZipFile

from tradercockpit.sqx_builder_config import (
    SQX_BUILDER_PROJECT_RELATIVE_PATH,
    builder_project_config_record,
)


class ResearchSpecificationTests(unittest.TestCase):
    def _runtime(self, root: Path) -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        return root

    def _write_project(self, home: Path, task_xml: str) -> None:
        path = home / SQX_BUILDER_PROJECT_RELATIVE_PATH
        path.parent.mkdir(parents=True, exist_ok=True)
        with ZipFile(path, "w") as archive:
            archive.writestr(
                "config.xml",
                '<Project><Chart symbol="EURUSD_M1_dukas" timeframe="M30"/><InstrumentInfo instrument="EURUSD_dukascopy"/></Project>',
            )
            archive.writestr("Build-Task1.xml", task_xml)

    def _requirements(self, record: dict[str, object]) -> dict[str, dict[str, object]]:
        specification = record["specification"]
        self.assertEqual(specification["schema"], "tc.research-specification.v1")
        return {item["id"]: item for item in specification["requirements"]}

    def test_complete_native_sections_are_read_as_current_selections_without_unlocking_build(self) -> None:
        task = """
        <Task>
          <WhatToBuild>
            <StrategyType type="simple"/>
            <MarketSides type="both"/>
            <BuildMode generationType="random-generation"/>
          </WhatToBuild>
          <Data><Setups><Setup dateFrom="2020.01.01" dateTo="2024.01.01" testPrecision="2" engine="0" slippage="1" minDist="0">
            <Chart symbol="EURUSD_M1_dukas" timeframe="M30" spread="2"/>
            <Commissions><Method use="true"/></Commissions>
          </Setup></Setups></Data>
          <Options><BuildTradingOptions><Option/></BuildTradingOptions></Options>
          <Blocks><BuildingBlocks/><OrderTypes/><ExitTypes/><CustomData/></Blocks>
          <RiskMoneyManagement><MoneyManagement><Method type="FixedSize" use="true"/><InitialCapital>10000</InitialCapital></MoneyManagement><RiskManagement/></RiskMoneyManagement>
          <Rankings><MaxStrategies>500</MaxStrategies><StopCondition type="passed-count" passedStrategies="10"/></Rankings>
          <CrossChecks/>
          <InstrumentInfo instrument="EURUSD_dukascopy"/>
        </Task>
        """
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, task)
            record = builder_project_config_record(home)

        requirements = self._requirements(record)
        self.assertEqual(requirements["strategy_shape"]["state"], "user_selected")
        self.assertEqual(requirements["historical_backtest"]["state"], "user_selected")
        self.assertEqual(requirements["trading_options"]["state"], "user_selected")
        self.assertEqual(requirements["building_blocks"]["state"], "user_selected")
        self.assertEqual(requirements["money_management"]["state"], "user_selected")
        self.assertEqual(requirements["search_build_mode"]["values"]["generation_type"], "random-generation")
        self.assertEqual(requirements["ranking_filters"]["values"]["stop_condition_type"], "passed-count")
        self.assertTrue(record["specification"]["build_gate"]["locked"])
        self.assertIn("exact_native_configuration_not_compiled", record["specification"]["build_gate"]["reason_codes"])

    def test_partial_data_never_becomes_resolved_from_symbol_and_timeframe_alone(self) -> None:
        task = """
        <Task>
          <WhatToBuild><StrategyType type="simple"/><BuildMode generationType="random-generation"/></WhatToBuild>
          <Data><Setups><Setup><Chart symbol="EURUSD_M1_dukas" timeframe="M30"/></Setup></Setups></Data>
          <Rankings><MaxStrategies>500</MaxStrategies><StopCondition type="never"/></Rankings>
          <InstrumentInfo instrument="EURUSD_dukascopy"/>
        </Task>
        """
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, task)
            record = builder_project_config_record(home)

        requirements = self._requirements(record)
        self.assertEqual(requirements["market_identity"]["state"], "user_selected")
        self.assertEqual(requirements["historical_backtest"]["state"], "unresolved")
        self.assertEqual(requirements["trading_options"]["state"], "unresolved")
        self.assertIn("unresolved:historical_backtest", record["specification"]["build_gate"]["reason_codes"])

    def test_proven_defaults_are_evidence_not_silent_current_selections(self) -> None:
        task = '<Task><Chart symbol="EURUSD_M1_dukas" timeframe="M30"/><InstrumentInfo instrument="EURUSD_dukascopy"/></Task>'
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, task)
            specification = builder_project_config_record(home)["specification"]

        requirements = {item["id"]: item for item in specification["requirements"]}
        self.assertEqual(requirements["strategy_shape"]["state"], "unresolved")
        self.assertEqual(requirements["search_build_mode"]["state"], "unresolved")
        defaults = {item["scope"]: item for item in specification["native_defaults"]}
        self.assertEqual(defaults["WhatToBuild"]["status"], "proven_default")
        self.assertEqual(defaults["WhatToBuild"]["values"]["generation_type"], "random-generation")
        self.assertEqual(defaults["Rankings"]["values"]["max_strategies"], 500)


if __name__ == "__main__":
    unittest.main()
