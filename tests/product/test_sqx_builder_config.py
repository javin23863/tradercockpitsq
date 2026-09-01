from __future__ import annotations

from hashlib import sha256
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
from zipfile import ZipFile

import tradercockpit.sqx_builder_config as builder_config
from tradercockpit.sqx_builder_config import (
    SQX_BUILDER_PROJECT_RELATIVE_PATH,
    SqxBuilderConfigError,
    builder_project_config_record,
    read_sqx_builder_project,
)


_NATIVE_CONFIG_XML = (
    '<Project><Chart symbol="DJ_M1_dukas" timeframe="H1"/>'
    '<InstrumentInfo instrument="USA30.IDX_dukascopy" tickSize="1.0" pointValue="1.0"/>'
    "</Project>"
)

_NATIVE_TASK_TEMPLATE = """<Task>
  <WhatToBuild>
    <StrategyType type="simple"/>
    <MarketSides type="Long+Short"/>
    <BuildMode generationType="{generation_type}" futureFlag="native">
      <GeneticOptions populationSize="123">
        <UnknownNativeSetting mode="opaque">producer-owned</UnknownNativeSetting>
      </GeneticOptions>
    </BuildMode>
  </WhatToBuild>
  <Data>
    <Setups>
      <Setup dateFrom="2020.01.01" dateTo="2025.01.01"
             testPrecision="selected-timeframe-only" engine="metatrader4"
             slippage="0.2" minDist="0.0">
        <Chart symbol="EURUSD_M1_dukas" timeframe="M30" spread="2.5"/>
        <Commissions><Method use="true"/></Commissions>
      </Setup>
    </Setups>
  </Data>
  <Rankings>
    <MaxStrategies>321</MaxStrategies>
    <StopCondition type="90m"/>
  </Rankings>
  <Options><BuildTradingOptions/></Options>
  <Blocks use="true">
    <UnknownNativeFamily mode="opaque" weight="7">
      <FutureParameterShape representation="producer-owned">native-value</FutureParameterShape>
    </UnknownNativeFamily>
  </Blocks>
  <MoneyManagement/>
  <CrossChecks use="true"/>
  <InstrumentInfo instrument="EURUSD_dukascopy" tickSize="1.0E-4" pointValue="100000.0"/>
</Task>"""


class SqxBuilderConfigTests(unittest.TestCase):
    def _runtime(self, root: Path) -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        return root

    def _write_project(self, home: Path, *, config_xml: str, task_xml: str | None) -> Path:
        path = home / SQX_BUILDER_PROJECT_RELATIVE_PATH
        path.parent.mkdir(parents=True, exist_ok=True)
        with ZipFile(path, "w") as archive:
            archive.writestr("config.xml", config_xml)
            if task_xml is not None:
                archive.writestr("Build-Task1.xml", task_xml)
        return path

    def _native_record(self, generation_type: str = "genetic-evolution") -> dict[str, object]:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(
                home,
                config_xml=_NATIVE_CONFIG_XML,
                task_xml=_NATIVE_TASK_TEMPLATE.format(generation_type=generation_type),
            )
            return builder_project_config_record(home)

    def test_reads_exact_native_builder_market_and_timeframe_configuration(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            project_path = self._write_project(
                home,
                config_xml='<Project><Chart symbol="DJ_M1_dukas" timeframe="H1"/><InstrumentInfo instrument="USA30.IDX_dukascopy" tickSize="1.0" pointValue="1.0"/></Project>',
                task_xml='<Task><Chart symbol="EURUSD_M1_dukas" timeframe="M30"/><InstrumentInfo instrument="EURUSD_dukascopy" tickSize="1.0E-4" pointValue="100000.0"/></Task>',
            )
            expected_digest = sha256(project_path.read_bytes()).hexdigest()
            record = builder_project_config_record(home)

        self.assertEqual(record["schema"], "tc.sqx-builder-config.v1")
        self.assertEqual(record["source_build"], "144.2953")
        self.assertEqual(record["archive_sha256"], expected_digest)
        self.assertEqual(record["internal_entries"], ["config.xml", "Build-Task1.xml"])
        self.assertEqual(
            record["charts"],
            [
                {"symbol": "DJ_M1_dukas", "timeframe": "H1"},
                {"symbol": "EURUSD_M1_dukas", "timeframe": "M30"},
            ],
        )
        self.assertFalse(record["execution"]["available"])
        self.assertEqual(record["execution"]["reason"], "specification_read_only_no_native_launch")

    def test_digest_and_parsed_fields_share_one_archive_snapshot(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            project_path = self._write_project(
                home,
                config_xml='<Project><Chart symbol="DJ_M1_dukas" timeframe="H1"/><InstrumentInfo instrument="USA30.IDX_dukascopy"/></Project>',
                task_xml='<Task><Chart symbol="EURUSD_M1_dukas" timeframe="M30"/><InstrumentInfo instrument="EURUSD_dukascopy"/></Task>',
            )
            first_snapshot = project_path.read_bytes()
            original_reader = builder_config._read_project_entries

            def replace_archive_after_snapshot(snapshot: bytes) -> tuple[bytes, ...]:
                self._write_project(
                    home,
                    config_xml='<Project><Chart symbol="GBPUSD_M1_dukas" timeframe="M15"/><InstrumentInfo instrument="GBPUSD_dukascopy"/></Project>',
                    task_xml='<Task><Chart symbol="USDJPY_M1_dukas" timeframe="H4"/><InstrumentInfo instrument="USDJPY_dukascopy"/></Task>',
                )
                return original_reader(snapshot)

            with patch.object(builder_config, "_read_project_entries", side_effect=replace_archive_after_snapshot):
                config = read_sqx_builder_project(home)
            replacement_digest = sha256(project_path.read_bytes()).hexdigest()

        self.assertEqual(config.archive_sha256, sha256(first_snapshot).hexdigest())
        self.assertNotEqual(config.archive_sha256, replacement_digest)
        self.assertEqual(
            [(chart.symbol, chart.timeframe) for chart in config.charts],
            [("DJ_M1_dukas", "H1"), ("EURUSD_M1_dukas", "M30")],
        )

    def test_duplicate_market_facts_are_deduplicated(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            chart = '<Chart symbol="DJ_M1_dukas" timeframe="H1"/>'
            instrument = '<InstrumentInfo instrument="USA30.IDX_dukascopy"/>'
            self._write_project(
                home,
                config_xml=f"<Project>{chart}{instrument}</Project>",
                task_xml=f"<Task>{chart}{instrument}</Task>",
            )
            config = read_sqx_builder_project(home)
        self.assertEqual(len(config.charts), 1)
        self.assertEqual(len(config.instruments), 1)

    def test_saved_builder_project_never_infers_preset_binding(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(
                home,
                config_xml='<Project><Chart symbol="DJ" timeframe="H1"/><InstrumentInfo instrument="USA30"/></Project>',
                task_xml='<Task><Chart symbol="EURUSD" timeframe="M30"/><InstrumentInfo instrument="EURUSD"/></Task>',
            )
            binding = builder_project_config_record(home)["preset_binding"]
        self.assertEqual(binding["status"], "market_proven_preset_unverified")
        self.assertIsNone(binding["preset_id"])
        self.assertFalse(binding["wiring_allowed"])

    def test_native_search_configuration_preserves_exact_build_mode_structure(self) -> None:
        record = self._native_record()
        search = record["search"]

        self.assertEqual(search["schema"], "tc.sqx-builder-search.v1")
        self.assertEqual(search["authority"], "native_sqx_read_only")
        self.assertEqual(search["source"]["project"], "Builder")
        self.assertEqual(search["source"]["relative_path"], SQX_BUILDER_PROJECT_RELATIVE_PATH)
        self.assertEqual(search["source"]["archive_sha256"], record["archive_sha256"])
        self.assertEqual(search["source"]["member"], "Build-Task1.xml")
        self.assertEqual(search["selector"], "genetic-evolution")
        self.assertEqual(
            search["display_mode"],
            {"kind": "genetic_evolution", "label": "Genetic Evolution", "recognized": True},
        )
        build_mode = search["producer_configuration"]
        self.assertEqual(build_mode["tag"], "BuildMode")
        self.assertEqual(build_mode["attributes"]["generationType"], "genetic-evolution")
        self.assertEqual(build_mode["attributes"]["futureFlag"], "native")
        self.assertEqual(build_mode["children"][0]["tag"], "GeneticOptions")
        unknown = build_mode["children"][0]["children"][0]
        self.assertEqual(unknown["tag"], "UnknownNativeSetting")
        self.assertEqual(unknown["attributes"], {"mode": "opaque"})
        self.assertEqual(unknown["text"], "producer-owned")
        self.assertFalse(search["semantics"]["interpreted_by_tradercockpit"])
        self.assertEqual(search["semantics"]["owner"], "StrategyQuant X")
        self.assertFalse(search["execution"]["available"])

    def test_native_blocks_configuration_preserves_exact_unknown_structure(self) -> None:
        record = self._native_record()
        blocks = record["blocks"]

        self.assertEqual(blocks["schema"], "tc.sqx-builder-blocks.v1")
        self.assertEqual(blocks["authority"], "native_sqx_read_only")
        self.assertEqual(blocks["source"]["project"], "Builder")
        self.assertEqual(blocks["source"]["relative_path"], SQX_BUILDER_PROJECT_RELATIVE_PATH)
        self.assertEqual(blocks["source"]["archive_sha256"], record["archive_sha256"])
        self.assertEqual(blocks["source"]["member"], "Build-Task1.xml")
        root = blocks["producer_configuration"]
        self.assertEqual(root["tag"], "Blocks")
        self.assertEqual(root["attributes"], {"use": "true"})
        family = root["children"][0]
        self.assertEqual(family["tag"], "UnknownNativeFamily")
        self.assertEqual(family["attributes"], {"mode": "opaque", "weight": "7"})
        parameter = family["children"][0]
        self.assertEqual(parameter["tag"], "FutureParameterShape")
        self.assertEqual(parameter["attributes"], {"representation": "producer-owned"})
        self.assertEqual(parameter["text"], "native-value")
        self.assertFalse(blocks["semantics"]["interpreted_by_tradercockpit"])
        self.assertEqual(blocks["semantics"]["owner"], "StrategyQuant X")
        self.assertFalse(blocks["execution"]["available"])

    def test_native_search_presentation_labels_do_not_reject_unknown_producer_modes(self) -> None:
        random_search = self._native_record("random-generation")["search"]
        self.assertEqual(random_search["selector"], "random-generation")
        self.assertEqual(random_search["display_mode"]["kind"], "random_discovery")
        self.assertEqual(random_search["display_mode"]["label"], "Random Discovery")
        self.assertTrue(random_search["display_mode"]["recognized"])

        future_search = self._native_record("future-native-search")["search"]
        self.assertEqual(future_search["selector"], "future-native-search")
        self.assertEqual(future_search["display_mode"]["kind"], "native_other")
        self.assertEqual(future_search["display_mode"]["label"], "Other native search mode")
        self.assertFalse(future_search["display_mode"]["recognized"])
        self.assertEqual(
            future_search["producer_configuration"]["attributes"]["generationType"],
            "future-native-search",
        )

    def test_missing_malformed_or_empty_project_fails_closed(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, config_xml="<Project/>", task_xml=None)
            with self.assertRaises(SqxBuilderConfigError) as caught:
                read_sqx_builder_project(home)
            self.assertEqual(caught.exception.code, "builder_project_entries_missing")

        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, config_xml="<Project>", task_xml="<Task/>")
            with self.assertRaises(SqxBuilderConfigError) as caught:
                read_sqx_builder_project(home)
            self.assertEqual(caught.exception.code, "builder_project_xml_invalid")

        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, config_xml="<Project/>", task_xml="<Task/>")
            with self.assertRaises(SqxBuilderConfigError) as caught:
                read_sqx_builder_project(home)
            self.assertEqual(caught.exception.code, "builder_market_configuration_missing")

    def test_builder_project_symlink_escape_is_refused(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx")
            outside_dir = root / "outside-builder"
            outside_dir.mkdir()
            outside = outside_dir / "project.cfx"
            with ZipFile(outside, "w") as archive:
                archive.writestr("config.xml", '<Project><Chart symbol="ES" timeframe="M30"/><InstrumentInfo instrument="ES"/></Project>')
                archive.writestr("Build-Task1.xml", "<Task/>")
            builder_dir = home / "user/projects/Builder"
            builder_dir.parent.mkdir(parents=True)
            try:
                builder_dir.symlink_to(outside_dir, target_is_directory=True)
            except OSError as exc:
                self.skipTest(f"directory symlinks unavailable: {exc}")
            with self.assertRaises(SqxBuilderConfigError) as caught:
                read_sqx_builder_project(home)
            self.assertEqual(caught.exception.code, "builder_project_path_escape")


if __name__ == "__main__":
    unittest.main()
