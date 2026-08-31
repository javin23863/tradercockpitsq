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

    def test_concrete_native_scalars_are_read_without_interpreting_opaque_sections(self) -> None:
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
        self.assertEqual(requirements["strategy_shape"]["state"], "unresolved")
        self.assertEqual(requirements["strategy_shape"]["values"]["strategy_type"], "simple")
        self.assertEqual(requirements["strategy_shape"]["values"]["market_sides"], "both")
        self.assertEqual(requirements["historical_backtest"]["state"], "user_selected")
        self.assertEqual(requirements["historical_backtest"]["values"]["setup_count"], 1)
        self.assertEqual(requirements["search_build_mode"]["state"], "user_selected")
        self.assertEqual(requirements["search_build_mode"]["values"]["generation_type"], "random-generation")

        self.assertEqual(requirements["trading_options"]["state"], "unresolved")
        self.assertTrue(requirements["trading_options"]["values"]["section_present"])
        self.assertEqual(requirements["building_blocks"]["state"], "unresolved")
        self.assertTrue(requirements["building_blocks"]["values"]["section_present"])
        self.assertEqual(requirements["money_management"]["state"], "unresolved")
        self.assertTrue(requirements["money_management"]["values"]["section_present"])
        self.assertEqual(requirements["ranking_filters"]["state"], "unresolved")
        self.assertEqual(requirements["ranking_filters"]["values"]["stop_condition_type"], "passed-count")
        self.assertEqual(requirements["validation_profile"]["state"], "not_applicable")
        self.assertTrue(requirements["validation_profile"]["values"]["section_present"])
        self.assertFalse(requirements["validation_profile"]["values"]["enabled"])

        reasons = record["specification"]["build_gate"]["reason_codes"]
        self.assertIn("unresolved:strategy_shape", reasons)
        self.assertIn("unresolved:trading_options", reasons)
        self.assertIn("unresolved:building_blocks", reasons)
        self.assertIn("unresolved:money_management", reasons)
        self.assertIn("unresolved:ranking_filters", reasons)
        self.assertIn("exact_native_configuration_not_compiled", reasons)
        self.assertTrue(record["specification"]["build_gate"]["locked"])

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

    def test_multiple_data_setups_stay_unresolved_instead_of_using_only_the_first(self) -> None:
        complete_setup = """
          <Setup dateFrom="2020.01.01" dateTo="2024.01.01" testPrecision="2" engine="0" slippage="1" minDist="0">
            <Chart symbol="EURUSD_M1_dukas" timeframe="M30" spread="2"/>
            <Commissions><Method use="true"/></Commissions>
          </Setup>
        """
        task = f"""
        <Task>
          <Data><Setups>{complete_setup}<Setup><Chart symbol="DJ_M1_dukas" timeframe="H1"/></Setup></Setups></Data>
          <Chart symbol="EURUSD_M1_dukas" timeframe="M30"/>
          <InstrumentInfo instrument="EURUSD_dukascopy"/>
        </Task>
        """
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, task)
            record = builder_project_config_record(home)

        historical = self._requirements(record)["historical_backtest"]
        self.assertEqual(historical["state"], "unresolved")
        self.assertEqual(historical["values"]["setup_count"], 2)
        self.assertIsNone(historical["values"]["date_from"])

    def test_unknown_generation_type_stays_unresolved(self) -> None:
        task = """
        <Task>
          <WhatToBuild><BuildMode generationType="future-or-typo"/></WhatToBuild>
          <Chart symbol="EURUSD_M1_dukas" timeframe="M30"/>
          <InstrumentInfo instrument="EURUSD_dukascopy"/>
        </Task>
        """
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, task)
            record = builder_project_config_record(home)

        search_mode = self._requirements(record)["search_build_mode"]
        self.assertEqual(search_mode["state"], "unresolved")
        self.assertEqual(search_mode["values"]["generation_type"], "future-or-typo")

    def test_cross_checks_use_flag_controls_applicability_without_interpreting_profile(self) -> None:
        for use_value, expected_state in (("false", "not_applicable"), ("true", "unresolved")):
            with self.subTest(use=use_value):
                task = f"""
                <Task>
                  <CrossChecks use="{use_value}"/>
                  <Chart symbol="EURUSD_M1_dukas" timeframe="M30"/>
                  <InstrumentInfo instrument="EURUSD_dukascopy"/>
                </Task>
                """
                with TemporaryDirectory() as tmp:
                    home = self._runtime(Path(tmp))
                    self._write_project(home, task)
                    record = builder_project_config_record(home)

                profile = self._requirements(record)["validation_profile"]
                self.assertEqual(profile["state"], expected_state)
                self.assertTrue(profile["values"]["section_present"])
                self.assertEqual(profile["values"]["enabled"], use_value == "true")

    def test_commission_configuration_must_be_usable(self) -> None:
        cases = (
            ("<Commissions/>", False),
            ("<Commissions><Method use=\"false\"/><Method use=\"false\"/></Commissions>", False),
            ("<Commissions><Method use=\"true\"/><Method use=\"false\"/></Commissions>", True),
        )
        for commissions, expected in cases:
            with self.subTest(commissions=commissions):
                task = f"""
                <Task>
                  <Data><Setups><Setup dateFrom="2020.01.01" dateTo="2024.01.01" testPrecision="2" engine="0" slippage="1" minDist="0">
                    <Chart symbol="EURUSD_M1_dukas" timeframe="M30" spread="2"/>
                    {commissions}
                  </Setup></Setups></Data>
                  <InstrumentInfo instrument="EURUSD_dukascopy"/>
                </Task>
                """
                with TemporaryDirectory() as tmp:
                    home = self._runtime(Path(tmp))
                    self._write_project(home, task)
                    record = builder_project_config_record(home)

                historical = self._requirements(record)["historical_backtest"]
                self.assertEqual(historical["values"]["has_commissions"], expected)
                self.assertEqual(historical["state"], "user_selected" if expected else "unresolved")

    def test_no_unverified_native_defaults_are_exported(self) -> None:
        task = '<Task><Chart symbol="EURUSD_M1_dukas" timeframe="M30"/><InstrumentInfo instrument="EURUSD_dukascopy"/></Task>'
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, task)
            specification = builder_project_config_record(home)["specification"]

        requirements = {item["id"]: item for item in specification["requirements"]}
        self.assertEqual(requirements["strategy_shape"]["state"], "unresolved")
        self.assertEqual(requirements["search_build_mode"]["state"], "unresolved")
        self.assertNotIn("native_defaults", specification)


if __name__ == "__main__":
    unittest.main()
