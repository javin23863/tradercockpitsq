from __future__ import annotations

import importlib.util
import json
import os
from pathlib import Path
from tempfile import TemporaryDirectory
from types import SimpleNamespace
import unittest
from unittest.mock import patch


_TOOL_PATH = Path(__file__).resolve().parents[2] / "tools" / "package_windows_desktop.py"
_SPEC = importlib.util.spec_from_file_location("tradercockpit_package_windows_desktop", _TOOL_PATH)
assert _SPEC is not None and _SPEC.loader is not None
_PACKAGER = importlib.util.module_from_spec(_SPEC)
_SPEC.loader.exec_module(_PACKAGER)


class WindowsPackageOrchestrationTests(unittest.TestCase):
    def test_setup_script_records_version_and_payload_hash(self) -> None:
        script = _PACKAGER._render_setup_script(version="1.2.3", executable_sha256="deadbeef")
        self.assertIn("1.2.3", script)
        self.assertIn("deadbeef", script)
        self.assertIn("install_windows_desktop.py", script)

    def test_package_contract_skips_nsis_when_makensis_missing(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "pyproject.toml").write_text(
                '[project]\nname = "tradercockpit-core"\nversion = "9.9.9"\n',
                encoding="utf-8",
            )
            (root / "tools").mkdir()
            (root / "tools" / "install_windows_desktop.py").write_text("# stub\n", encoding="utf-8")
            (root / "packaging" / "windows").mkdir(parents=True)
            (root / "packaging" / "windows" / "installer.nsi").write_text("; stub\n", encoding="utf-8")

            dist = root / "dist"
            work = root / "build"
            release = dist / "release"

            def fake_build(_root, *, dist_dir, work_dir):
                dist_dir.mkdir(parents=True, exist_ok=True)
                exe = dist_dir / "TraderCockpit.exe"
                exe.write_bytes(b"desktop")
                return exe

            _PACKAGER._load_build_module = lambda: SimpleNamespace(build_windows_desktop=fake_build)

            with patch.dict(os.environ, {}, clear=True), patch.object(_PACKAGER.shutil, "which", return_value=None):
                result = _PACKAGER.package_windows_desktop(
                    root,
                    dist_dir=dist,
                    work_dir=work,
                    release_dir=release,
                    sign=False,
                )
            self.assertEqual(result["version"], "9.9.9")
            self.assertIsNone(result["nsis_installer"])
            manifest = json.loads((release / "release-manifest.json").read_text(encoding="utf-8"))
            self.assertEqual(manifest["schema"], "tc.windows-install.v1")
            self.assertTrue(Path(str(result["zip"])).is_file())


if __name__ == "__main__":
    unittest.main()
