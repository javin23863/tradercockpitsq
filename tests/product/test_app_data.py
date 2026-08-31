from __future__ import annotations

import os
from pathlib import Path
import sys
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

from tradercockpit.app_data import (
    TRADERCOCKPIT_DATA_ROOT_ENV,
    default_application_data_root,
    resolve_application_data_root,
)


class ApplicationDataRootTests(unittest.TestCase):
    def test_explicit_process_side_override_wins(self) -> None:
        with TemporaryDirectory() as tmp:
            expected = Path(tmp).resolve()
            with patch.dict(os.environ, {TRADERCOCKPIT_DATA_ROOT_ENV: str(Path(tmp) / "env")}, clear=False):
                self.assertEqual(resolve_application_data_root(tmp), expected)

    def test_environment_override_is_used_when_explicit_value_is_absent(self) -> None:
        with TemporaryDirectory() as tmp:
            with patch.dict(os.environ, {TRADERCOCKPIT_DATA_ROOT_ENV: tmp}, clear=False):
                self.assertEqual(resolve_application_data_root(), Path(tmp).resolve())

    def test_invalid_empty_explicit_override_refuses(self) -> None:
        with self.assertRaises(ValueError):
            resolve_application_data_root("   ")

    def test_windows_default_uses_local_app_data(self) -> None:
        with TemporaryDirectory() as tmp, patch("tradercockpit.app_data.sys.platform", "win32"), patch.dict(
            os.environ,
            {"LOCALAPPDATA": tmp},
            clear=False,
        ):
            self.assertEqual(default_application_data_root(), (Path(tmp) / "TraderCockpit").resolve())

    def test_linux_default_uses_xdg_data_home(self) -> None:
        with TemporaryDirectory() as tmp, patch("tradercockpit.app_data.sys.platform", "linux"), patch.dict(
            os.environ,
            {"XDG_DATA_HOME": tmp},
            clear=False,
        ):
            self.assertEqual(default_application_data_root(), (Path(tmp) / "TraderCockpit").resolve())


if __name__ == "__main__":
    unittest.main()
