from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from zipfile import ZipFile

import tradercockpit.sqx_outputs as outputs
from tradercockpit.sqx_outputs import SqxOutputError, discover_sqx_outputs, inspect_sqx_output


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
        version: str = "144.2953",
        complete: bool = True,
    ) -> Path:
        target = home / "user/projects/Builder/databanks/Results" / name
        with ZipFile(target, "w") as archive:
            archive.writestr("settings.xml", b"<Settings><Symbol>ES</Symbol></Settings>")
            archive.writestr("strategy_Portfolio.xml", b"<Strategy><Rule>native-sqx</Rule></Strategy>")
            if complete:
                archive.writestr("version.txt", version.encode("utf-8"))
            archive.writestr("orders.bin", b"native orders")
        return target

    def test_discovery_is_read_only_and_lists_exact_native_archives(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._archive(home, "B.sqx")
            self._archive(home, "A.sqx")
            payload = discover_sqx_outputs(home)

        self.assertEqual(payload["schema"], "tc.sqx-builder-output-list.v1")
        self.assertTrue(payload["runtime"]["ready"])
        self.assertFalse(payload["import_available"])
        self.assertEqual(payload["import_reason"], "candidate_custody_not_implemented")
        self.assertEqual([item["archive"] for item in payload["outputs"]], ["A.sqx", "B.sqx"])
        self.assertTrue(all(item["inspectable"] for item in payload["outputs"]))
        self.assertTrue(all(item["native_version"] == "144.2953" for item in payload["outputs"]))
        self.assertFalse(hasattr(outputs, "import_sqx_output"))

    def test_archive_build_mismatch_and_incomplete_archive_fail_closed(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._archive(home, "Wrong.sqx", version="143.1")
            self._archive(home, "Incomplete.sqx", complete=False)
            listing = discover_sqx_outputs(home)
            by_name = {item["archive"]: item for item in listing["outputs"]}
            self.assertFalse(by_name["Wrong.sqx"]["inspectable"])
            self.assertEqual(by_name["Wrong.sqx"]["reason_code"], "sqx_output_build_mismatch")
            self.assertFalse(by_name["Incomplete.sqx"]["inspectable"])
            self.assertEqual(by_name["Incomplete.sqx"]["reason_code"], "invalid_sqx_archive")

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
                archive.writestr("strategy_Portfolio.xml", b"<Strategy/>")
                archive.writestr("version.txt", b"144.2953")
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


if __name__ == "__main__":
    unittest.main()
