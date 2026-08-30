from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from zipfile import ZipFile

from tradercockpit.app_server import sqx_output_import_response
from tradercockpit.domain import CandidateSpecV1, ContentAddress, StrategySpecV1
from tradercockpit.sqx_outputs import (
    SQX_NATIVE_STRATEGY_SCHEMA,
    SqxOutputError,
    discover_sqx_outputs,
    import_sqx_output,
    inspect_sqx_output,
)
from tradercockpit.storage import FileObjectStore


class SqxOutputCustodyTests(unittest.TestCase):
    def _runtime(self, root: Path, *, build: str = "2953") -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text(build, encoding="utf-8")
        (root / "internal").mkdir(exist_ok=True)
        (root / "internal/SQUANT.dat").write_bytes(b"144")
        (root / "user/projects/Builder/databanks/Results").mkdir(parents=True)
        return root

    def _archive(self, home: Path, name: str = "Generated 1.sqx", *, complete: bool = True) -> Path:
        target = home / "user/projects/Builder/databanks/Results" / name
        with ZipFile(target, "w") as archive:
            archive.writestr("settings.xml", b"<Settings><Symbol>ES</Symbol></Settings>")
            archive.writestr("strategy_Portfolio.xml", b"<Strategy><Rule>native-sqx</Rule></Strategy>")
            if complete:
                archive.writestr("version.txt", b"144.2953")
            archive.writestr("orders.bin", b"native orders")
        return target

    def test_discovery_reads_only_verified_builder_results_archives(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._archive(home, "B.sqx")
            self._archive(home, "A.sqx")
            (home / "user/projects/Builder/databanks/Results/not-a-strategy.txt").write_text("ignore", encoding="utf-8")

            payload = discover_sqx_outputs(home)

        self.assertEqual(payload["schema"], "tc.sqx-builder-output-list.v1")
        self.assertTrue(payload["runtime"]["ready"])
        self.assertEqual([item["archive"] for item in payload["outputs"]], ["A.sqx", "B.sqx"])
        self.assertTrue(all(item["importable"] for item in payload["outputs"]))
        self.assertTrue(all(item["native_version"] == "144.2953" for item in payload["outputs"]))

    def test_import_persists_exact_strategy_and_candidate_identities(self) -> None:
        with TemporaryDirectory() as runtime_tmp, TemporaryDirectory() as state_tmp:
            home = self._runtime(Path(runtime_tmp))
            archive = self._archive(home)
            inspected = inspect_sqx_output(archive)
            state_root = Path(state_tmp)

            first = import_sqx_output(home, state_root, archive.name)
            second = import_sqx_output(home, state_root, archive.name)

            self.assertEqual(first, second)
            self.assertEqual(first["custody"], "persisted")
            self.assertFalse(first["run_binding"]["available"])
            self.assertEqual(first["run_binding"]["reason_code"], "evaluator_not_bound")

            strategy_ref = ContentAddress.parse(first["strategy_ref"])
            candidate_ref = ContentAddress.parse(first["candidate_ref"])
            store = FileObjectStore(state_root)
            strategy = store.resolve(strategy_ref)
            candidate = store.resolve(candidate_ref)

            self.assertIsInstance(strategy, StrategySpecV1)
            self.assertEqual(strategy.semantic_schema, SQX_NATIVE_STRATEGY_SCHEMA)
            self.assertEqual(strategy.semantics["archive_sha256"], inspected["archive_sha256"])
            self.assertEqual(strategy.semantics["strategy_entry_sha256"], inspected["strategy_entry_sha256"])
            self.assertEqual(strategy.semantics["settings_entry_sha256"], inspected["settings_entry_sha256"])
            self.assertIsInstance(candidate, CandidateSpecV1)
            self.assertEqual(candidate.strategy_ref, strategy.ref)
            self.assertEqual(candidate.origin, "sqx-builder")

    def test_path_traversal_and_non_sqx_names_fail_closed(self) -> None:
        with TemporaryDirectory() as runtime_tmp, TemporaryDirectory() as state_tmp:
            home = self._runtime(Path(runtime_tmp))
            for invalid in ("../escape.sqx", "folder/escape.sqx", "strategy.xml", ""):
                with self.subTest(invalid=invalid):
                    with self.assertRaises(SqxOutputError) as caught:
                        import_sqx_output(home, state_tmp, invalid)
                    self.assertEqual(caught.exception.code, "invalid_archive_name")

    def test_corrupt_and_incomplete_archives_are_not_imported(self) -> None:
        with TemporaryDirectory() as runtime_tmp, TemporaryDirectory() as state_tmp:
            home = self._runtime(Path(runtime_tmp))
            corrupt = home / "user/projects/Builder/databanks/Results/Corrupt.sqx"
            corrupt.write_bytes(b"not a zip")
            incomplete = self._archive(home, "Incomplete.sqx", complete=False)

            listing = discover_sqx_outputs(home)
            by_name = {item["archive"]: item for item in listing["outputs"]}
            self.assertFalse(by_name[corrupt.name]["importable"])
            self.assertFalse(by_name[incomplete.name]["importable"])

            for name in (corrupt.name, incomplete.name):
                with self.subTest(name=name):
                    with self.assertRaises(SqxOutputError) as caught:
                        import_sqx_output(home, state_tmp, name)
                    self.assertEqual(caught.exception.code, "invalid_sqx_archive")

    def test_wrong_sqx_build_and_missing_state_root_fail_closed(self) -> None:
        with TemporaryDirectory() as runtime_tmp:
            home = self._runtime(Path(runtime_tmp), build="9999")
            archive = self._archive(home)
            listing = discover_sqx_outputs(home)
            self.assertFalse(listing["runtime"]["ready"])
            self.assertEqual(listing["runtime"]["status"], "sqx_build_mismatch")
            with self.assertRaises(SqxOutputError) as caught:
                import_sqx_output(home, None, archive.name)
            self.assertEqual(caught.exception.code, "sqx_build_mismatch")

        with TemporaryDirectory() as runtime_tmp:
            home = self._runtime(Path(runtime_tmp))
            archive = self._archive(home)
            with self.assertRaises(SqxOutputError) as caught:
                import_sqx_output(home, None, archive.name)
            self.assertEqual(caught.exception.code, "state_root_not_configured")

    def test_api_response_preserves_fail_closed_status(self) -> None:
        def refused(_sqx_home, _state_root, _archive):
            raise SqxOutputError("invalid_sqx_archive", "broken native archive")

        status, payload = sqx_output_import_response(None, None, "Broken.sqx", importer=refused)
        self.assertEqual(status, 409)
        self.assertEqual(payload["error"], "invalid_state")
        self.assertEqual(payload["reason_code"], "invalid_sqx_archive")


if __name__ == "__main__":
    unittest.main()
