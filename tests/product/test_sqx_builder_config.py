from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from zipfile import ZipFile

from tradercockpit.sqx_builder_config import (
    SQX_BUILDER_PROJECT_RELATIVE_PATH,
    SqxBuilderConfigError,
    builder_project_config_record,
    read_sqx_builder_project,
)


class SqxBuilderConfigTests(unittest.TestCase):
    def _runtime(self, root: Path) -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        return root

    def _write_project(
        self,
        home: Path,
        *,
        config_xml: str,
        task_xml: str | None,
    ) -> Path:
        path = home / SQX_BUILDER_PROJECT_RELATIVE_PATH
        path.parent.mkdir(parents=True, exist_ok=True)
        with ZipFile(path, "w") as archive:
            archive.writestr("config.xml", config_xml)
            if task_xml is not None:
                archive.writestr("Build-Task1.xml", task_xml)
        return path

    def test_reads_exact_native_builder_market_and_timeframe_configuration(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(
                home,
                config_xml='''<Project><Chart symbol="DJ_M1_dukas" timeframe="H1"/><InstrumentInfo instrument="USA30.IDX_dukascopy" tickSize="1.0" pointValue="1.0"/></Project>''',
                task_xml='''<Task><Chart symbol="EURUSD_M1_dukas" timeframe="M30"/><InstrumentInfo instrument="EURUSD_dukascopy" tickSize="1.0E-4" pointValue="100000.0"/></Task>''',
            )

            record = builder_project_config_record(home)

        self.assertEqual(record["schema"], "tc.sqx-builder-config.v1")
        self.assertEqual(record["source_build"], "144.2953")
        self.assertEqual(record["project"], "Builder")
        self.assertEqual(record["internal_entries"], ["config.xml", "Build-Task1.xml"])
        self.assertEqual(
            record["charts"],
            [
                {"symbol": "DJ_M1_dukas", "timeframe": "H1"},
                {"symbol": "EURUSD_M1_dukas", "timeframe": "M30"},
            ],
        )
        self.assertEqual(
            [item["instrument"] for item in record["instruments"]],
            ["USA30.IDX_dukascopy", "EURUSD_dukascopy"],
        )

    def test_duplicate_market_facts_are_not_promoted_to_duplicate_configuration(self) -> None:
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

    def test_saved_builder_project_never_infers_a_preset_binding(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(
                home,
                config_xml='<Project><Chart symbol="DJ_M1_dukas" timeframe="H1"/><InstrumentInfo instrument="USA30.IDX_dukascopy"/></Project>',
                task_xml='<Task><Chart symbol="EURUSD_M1_dukas" timeframe="M30"/><InstrumentInfo instrument="EURUSD_dukascopy"/></Task>',
            )
            binding = builder_project_config_record(home)["preset_binding"]

        self.assertEqual(binding["status"], "market_proven_preset_unverified")
        self.assertIsNone(binding["preset_id"])
        self.assertFalse(binding["wiring_allowed"])

    def test_missing_native_builder_task_entry_fails_closed(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(
                home,
                config_xml='<Project><Chart symbol="DJ_M1_dukas" timeframe="H1"/><InstrumentInfo instrument="USA30.IDX_dukascopy"/></Project>',
                task_xml=None,
            )
            with self.assertRaises(SqxBuilderConfigError) as caught:
                read_sqx_builder_project(home)

        self.assertEqual(caught.exception.code, "builder_project_entries_missing")

    def test_malformed_or_semantically_empty_project_fails_closed(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, config_xml="<Project>", task_xml="<Task/>")
            with self.assertRaises(SqxBuilderConfigError) as malformed:
                read_sqx_builder_project(home)
            self.assertEqual(malformed.exception.code, "builder_project_xml_invalid")

        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, config_xml="<Project/>", task_xml="<Task/>")
            with self.assertRaises(SqxBuilderConfigError) as empty:
                read_sqx_builder_project(home)
            self.assertEqual(empty.exception.code, "builder_market_configuration_missing")


if __name__ == "__main__":
    unittest.main()
