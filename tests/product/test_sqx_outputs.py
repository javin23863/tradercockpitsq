from __future__ import annotations

from hashlib import sha256
from io import BytesIO
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from zipfile import ZipFile

from tradercockpit.sqx_outputs import (
    SqxOutputError,
    capture_sqx_output_archive,
    discover_sqx_outputs,
    inspect_sqx_output,
    inspect_sqx_output_bytes,
)


class SqxOutputInspectionTests(unittest.TestCase):
    def _runtime(self, root: Path, *, build: str = "2953") -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text(build, encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        (root / "user/projects/Builder/databanks/Results").mkdir(parents=True)
        return root

    def _archive(
        self,
        home: Path,
        name: str = "Generated 1.sqx",
        *,
        version: str = "1",
        app_version: str = "SQX Build 144.2953",
        complete: bool = True,
    ) -> Path:
        target = home / "user/projects/Builder/databanks/Results" / name
        with ZipFile(target, "w") as archive:
            archive.writestr("settings.xml", b"<Settings><Symbol>ES</Symbol></Settings>")
            archive.writestr("strategy_Portfolio.xml", f'<StrategyFile Version="3.9.133" AppVersion="{app_version}"><Strategy><Rule>native-sqx</Rule></Strategy></StrategyFile>'.encode())
            if complete:
                archive.writestr("version.txt", version.encode("utf-8"))
            archive.writestr("orders.bin", b"native orders")
        return target

    def test_discovery_lists_exact_native_archives_and_exposes_candidate_import(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._archive(home, "B.sqx")
            self._archive(home, "A.sqx")
            payload = discover_sqx_outputs(home)

        self.assertEqual(payload["schema"], "tc.sqx-builder-output-list.v1")
        self.assertTrue(payload["runtime"]["ready"])
        self.assertTrue(payload["import_available"])
        self.assertIsNone(payload["import_reason"])
        self.assertEqual([item["archive"] for item in payload["outputs"]], ["A.sqx", "B.sqx"])
        self.assertTrue(all(item["inspectable"] for item in payload["outputs"]))
        self.assertTrue(all(item["native_version"] == "1" for item in payload["outputs"]))

    def test_exact_snapshot_capture_requires_selected_archive_digest(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            target = self._archive(home, "Native.sqx")
            expected = sha256(target.read_bytes()).hexdigest()
            snapshot, record = capture_sqx_output_archive(
                home,
                "Native.sqx",
                expected_archive_sha256=expected,
            )
            self.assertEqual(snapshot, target.read_bytes())
            self.assertEqual(record["archive_sha256"], expected)
            self.assertEqual(record["relative_path"], "user/projects/Builder/databanks/Results/Native.sqx")
            with self.assertRaises(SqxOutputError) as caught:
                capture_sqx_output_archive(
                    home,
                    "Native.sqx",
                    expected_archive_sha256="0" * 64,
                )
            self.assertEqual(caught.exception.code, "output_digest_mismatch")

    def test_archive_build_mismatch_and_incomplete_archive_fail_closed(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._archive(home, "Wrong.sqx", app_version="SQX Build 143.1")
            self._archive(home, "Incomplete.sqx", complete=False)
            listing = discover_sqx_outputs(home)
            by_name = {item["archive"]: item for item in listing["outputs"]}
            self.assertFalse(by_name["Wrong.sqx"]["inspectable"])
            self.assertEqual(by_name["Wrong.sqx"]["reason_code"], "sqx_output_build_mismatch")
            self.assertFalse(by_name["Incomplete.sqx"]["inspectable"])
            self.assertEqual(by_name["Incomplete.sqx"]["reason_code"], "invalid_sqx_archive")

    def test_archive_format_and_producer_build_are_separate_strict_authorities(self) -> None:
        for version, app_version, expected in (
            ("1", "SQX Build 144.2953", None),
            ("2", "SQX Build 144.2953", "sqx_output_format_unsupported"),
            ("144.2953", "SQX Build 144.2953", "sqx_output_format_unsupported"),
            ("1", "SQX Build 143.1", "sqx_output_build_mismatch"),
            ("1", "", "sqx_output_build_missing"),
            ("1", "144.2953", "sqx_output_build_mismatch"),
        ):
            with self.subTest(version=version, app_version=app_version), TemporaryDirectory() as tmp:
                home = self._runtime(Path(tmp))
                target = self._archive(home, version=version, app_version=app_version)
                snapshot = target.read_bytes()
                if expected:
                    with self.assertRaises(SqxOutputError) as caught:
                        inspect_sqx_output(target)
                    self.assertEqual(caught.exception.code, expected)
                else:
                    record = inspect_sqx_output(target)
                    self.assertEqual(record["archive_format_version"], "1")
                    self.assertEqual(record["sqx_build"], "144.2953")
                    self.assertEqual(record["archive_sha256"], sha256(snapshot).hexdigest())
                if version != "1":
                    with self.assertRaises(SqxOutputError):
                        inspect_sqx_output_bytes(snapshot, archive_name=target.name, require_runtime_build=False)
                elif app_version in ("", "SQX Build 143.1"):
                    record = inspect_sqx_output_bytes(snapshot, archive_name=target.name, require_runtime_build=False)
                    self.assertEqual(record["sqx_build"], "143.1" if app_version else None)

    def test_strategy_root_and_build_cannot_be_substituted_by_nested_metadata(self) -> None:
        for strategy in (b'<Strategy AppVersion="SQX Build 144.2953"/>',
                         b'<StrategyFile><Strategy AppVersion="SQX Build 144.2953"/></StrategyFile>',
                         b'<StrategyFile AppVersion="SQX Build 144.2953" AppVersion="SQX Build 145.0"/>',
                         b'<StrategyFile AppVersion="SQX Build 144.2953">'):
            with self.subTest(strategy=strategy):
                buffer = BytesIO()
                with ZipFile(buffer, "w") as archive:
                    archive.writestr("version.txt", b"1")
                    archive.writestr("settings.xml", b"<Settings/>")
                    archive.writestr("strategy_Portfolio.xml", strategy)
                with self.assertRaises(SqxOutputError):
                    inspect_sqx_output_bytes(buffer.getvalue(), archive_name="Invalid.sqx")

    def test_unsupported_xml_encodings_do_not_abort_other_output_discovery(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._archive(home, "Healthy.sqx")
            for encoding in ("unknown-codec", "UTF-32"):
                target = home / "user/projects/Builder/databanks/Results" / f"{encoding}.sqx"
                with ZipFile(target, "w") as archive:
                    archive.writestr("version.txt", b"1")
                    archive.writestr("settings.xml", b"<Settings/>")
                    archive.writestr("strategy_Portfolio.xml",
                                     f'<?xml version="1.0" encoding="{encoding}"?><StrategyFile AppVersion="SQX Build 144.2953"><Strategy/></StrategyFile>')
            listing = discover_sqx_outputs(home)
            outputs = {row["archive"]: row for row in listing["outputs"]}
            self.assertTrue(listing["import_available"])
            self.assertTrue(outputs["Healthy.sqx"]["inspectable"])
            for encoding in ("unknown-codec", "UTF-32"):
                self.assertFalse(outputs[f"{encoding}.sqx"]["inspectable"])
                self.assertEqual(outputs[f"{encoding}.sqx"]["reason_code"], "invalid_sqx_archive")

    def test_corrupt_archive_fails_closed(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            target = home / "user/projects/Builder/databanks/Results/Corrupt.sqx"
            target.write_bytes(b"not-a-zip")
            with self.assertRaises(SqxOutputError) as caught:
                inspect_sqx_output(target)
            self.assertEqual(caught.exception.code, "invalid_sqx_archive")

    def test_results_databank_symlink_escape_is_refused(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx")
            results = home / "user/projects/Builder/databanks/Results"
            results.rmdir()
            outside = root / "outside-results"
            outside.mkdir()
            try:
                results.symlink_to(outside, target_is_directory=True)
            except OSError as exc:
                self.skipTest(f"directory symlinks unavailable: {exc}")
            payload = discover_sqx_outputs(home)
            self.assertFalse(payload["runtime"]["ready"])
            self.assertEqual(payload["runtime"]["status"], "results_databank_path_escape")

    def test_output_symlink_escape_is_refused(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx")
            outside_dir = root / "outside"
            outside_dir.mkdir()
            outside = outside_dir / "Outside.sqx"
            with ZipFile(outside, "w") as archive:
                archive.writestr("settings.xml", b"<Settings/>")
                archive.writestr("strategy_Portfolio.xml", b'<StrategyFile AppVersion="SQX Build 144.2953"><Strategy/></StrategyFile>')
                archive.writestr("version.txt", b"1")
            alias = home / "user/projects/Builder/databanks/Results/Alias.sqx"
            try:
                alias.symlink_to(outside)
            except OSError as exc:
                self.skipTest(f"symlinks unavailable: {exc}")
            listing = discover_sqx_outputs(home)
            item = listing["outputs"][0]
            self.assertFalse(item["inspectable"])
            self.assertEqual(item["reason_code"], "output_path_escape")

    def test_wrong_runtime_build_refuses_before_output_truth(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp), build="9999")
            self._archive(home)
            payload = discover_sqx_outputs(home)
            self.assertFalse(payload["runtime"]["ready"])
            self.assertEqual(payload["runtime"]["status"], "sqx_build_mismatch")
            self.assertEqual(payload["outputs"], [])
            self.assertFalse(payload["import_available"])
            self.assertEqual(payload["import_reason"], "sqx_build_mismatch")


if __name__ == "__main__":
    unittest.main()
