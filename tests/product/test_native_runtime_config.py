from __future__ import annotations

from hashlib import sha256
import json
import os
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

from tradercockpit.native_runtime_config import (
    default_sqx_search_roots,
    discover_verified_sqx_home,
    load_native_runtime_config,
    optional_native_runtime_config,
    resolve_process_native_runtime,
    write_native_runtime_config,
)


class NativeRuntimeConfigTests(unittest.TestCase):
    def test_missing_file_is_absent_not_launch(self) -> None:
        with TemporaryDirectory() as tmp:
            self.assertEqual(load_native_runtime_config(tmp), (None, None))

    def test_round_trip_persists_home_and_digest(self) -> None:
        digest = "a" * 64
        with TemporaryDirectory() as tmp:
            home = Path(tmp) / "sqx"
            home.mkdir()
            path = write_native_runtime_config(tmp, sqx_home=home, launcher_sha256=digest)
            loaded_home, loaded_digest = load_native_runtime_config(tmp)
            self.assertEqual(path.name, "native-runtime.json")
            self.assertEqual(loaded_home, home.resolve())
            self.assertEqual(loaded_digest, digest)

    def test_corrupt_schema_refuses(self) -> None:
        with TemporaryDirectory() as tmp:
            Path(tmp, "native-runtime.json").write_text(
                json.dumps({"schema": "nope", "sqx_home": "x"}),
                encoding="utf-8",
            )
            with self.assertRaises(ValueError):
                load_native_runtime_config(tmp)
            self.assertEqual(optional_native_runtime_config(tmp), (None, None))

    def _sqx_home(self, root: Path, launcher: bytes = b"trusted launcher") -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        (root / "sqcli.exe").write_bytes(launcher)
        return root.resolve()

    def test_discover_zero_or_many_stays_unconfigured(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            empty = root / "empty"
            empty.mkdir()
            first = self._sqx_home(root / "SQX_one")
            second = self._sqx_home(root / "SQX_two")
            self.assertIsNone(discover_verified_sqx_home((empty,)))
            self.assertIsNone(discover_verified_sqx_home((first, second)))

    def test_discover_unique_child_under_downloads(self) -> None:
        with TemporaryDirectory() as tmp:
            downloads = Path(tmp) / "Downloads"
            home = self._sqx_home(downloads / "SQX_144_2953_win_20260601")
            (downloads / "notes").mkdir()
            self.assertEqual(discover_verified_sqx_home((downloads,)), home)

    def test_resolve_persists_unique_discovery_and_keeps_explicit_override(self) -> None:
        launcher = b"trusted launcher"
        digest = sha256(launcher).hexdigest()
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            data = root / "data"
            home = self._sqx_home(root / "Downloads" / "SQX_144", launcher)
            other = self._sqx_home(root / "other", b"other launcher")
            found, found_digest = resolve_process_native_runtime(
                data,
                search_roots=(root / "Downloads",),
            )
            self.assertEqual(found, home)
            self.assertEqual(found_digest, digest)
            self.assertEqual(load_native_runtime_config(data), (home, digest))
            remembered, remembered_digest = resolve_process_native_runtime(
                data,
                search_roots=(),
            )
            self.assertEqual((remembered, remembered_digest), (home, digest))
            explicit, explicit_digest = resolve_process_native_runtime(
                data,
                sqx_home=other,
                launcher_sha256="b" * 64,
                search_roots=(root / "Downloads",),
            )
            self.assertEqual(explicit, other)
            self.assertEqual(explicit_digest, "b" * 64)
            self.assertEqual(load_native_runtime_config(data), (home, digest))

    def test_default_search_roots_are_windows_only(self) -> None:
        if os.name == "nt":
            roots = default_sqx_search_roots()
            self.assertIn(Path.home() / "Downloads", roots)
            self.assertIn(Path(r"C:\StrategyQuantX"), roots)
        else:
            self.assertEqual(default_sqx_search_roots(), ())


if __name__ == "__main__":
    unittest.main()
