from __future__ import annotations

import importlib.util
import os
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch


_TOOL_PATH = Path(__file__).resolve().parents[2] / "tools" / "install_windows_desktop.py"
_SPEC = importlib.util.spec_from_file_location("tradercockpit_windows_installer", _TOOL_PATH)
assert _SPEC is not None and _SPEC.loader is not None
_INSTALLER = importlib.util.module_from_spec(_SPEC)
_SPEC.loader.exec_module(_INSTALLER)


class WindowsDesktopInstallTests(unittest.TestCase):
    def test_install_dir_is_not_sqx_or_app_data_or_checkout(self) -> None:
        with TemporaryDirectory() as tmp, patch.dict(os.environ, {"LOCALAPPDATA": tmp}, clear=False):
            install_dir = _INSTALLER.default_windows_install_dir()
        self.assertEqual(install_dir, Path(tmp) / "Programs" / "TraderCockpitSQ")
        self.assertNotEqual(install_dir.name, "TraderCockpit")
        self.assertNotIn("StrategyQuant", str(install_dir))

    def test_start_menu_shortcut_is_not_the_apollo_tradercockpit_name(self) -> None:
        with TemporaryDirectory() as tmp, patch.dict(os.environ, {"APPDATA": tmp}, clear=False):
            shortcut = _INSTALLER.default_start_menu_shortcut()
        self.assertEqual(shortcut.name, "TraderCockpitSQ.lnk")
        self.assertEqual(shortcut.parent.name, "TraderCockpitSQ")
        self.assertNotEqual(shortcut.name, "TraderCockpit.lnk")

    def test_refuses_native_sqx_entrypoint(self) -> None:
        with TemporaryDirectory() as tmp:
            fake = Path(tmp) / "StrategyQuantX.exe"
            fake.write_bytes(b"not-tradercockpit")
            with self.assertRaises(ValueError):
                _INSTALLER.install_windows_desktop(fake)

    def test_copies_tradercockpit_exe_and_points_shortcut_at_it(self) -> None:
        if os.name != "nt":
            self.skipTest("Windows shortcut creation is Windows-only")
        with TemporaryDirectory() as tmp:
            source = Path(tmp) / "dist" / "TraderCockpit.exe"
            source.parent.mkdir()
            source.write_bytes(b"tradercockpit-desktop")
            install_dir = Path(tmp) / "Programs" / "TraderCockpitSQ"
            shortcut = Path(tmp) / "Start Menu" / "TraderCockpit.lnk"
            installed = _INSTALLER.install_windows_desktop(
                source,
                install_dir=install_dir,
                shortcut_path=shortcut,
            )
            self.assertEqual(installed, install_dir / "TraderCockpit.exe")
            self.assertEqual(installed.read_bytes(), b"tradercockpit-desktop")
            self.assertTrue(shortcut.is_file())
            target = _read_shortcut_target(shortcut)
            self.assertEqual(Path(target).resolve(), installed.resolve())
            self.assertNotIn("StrategyQuantX.exe", target)


def _read_shortcut_target(link_path: Path) -> str:
    import subprocess

    script = (
        "$s = (New-Object -ComObject WScript.Shell).CreateShortcut({link}); "
        "$s.TargetPath"
    ).format(link=json_dumps(str(link_path)))
    completed = subprocess.run(
        ["powershell", "-NoProfile", "-NonInteractive", "-Command", script],
        check=True,
        capture_output=True,
        text=True,
    )
    return completed.stdout.strip()


def json_dumps(value: str) -> str:
    import json

    return json.dumps(value)


if __name__ == "__main__":
    unittest.main()
