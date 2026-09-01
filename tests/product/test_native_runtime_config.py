from __future__ import annotations

import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.native_runtime_config import (
    load_native_runtime_config,
    optional_native_runtime_config,
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


if __name__ == "__main__":
    unittest.main()
