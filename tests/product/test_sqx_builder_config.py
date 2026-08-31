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