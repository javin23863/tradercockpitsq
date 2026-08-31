from __future__ import annotations

import importlib.util
import os
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest


_TOOL_PATH = Path(__file__).resolve().parents[2] / "tools" / "build_windows_desktop.py"
_SPEC = importlib.util.spec_from_file_location("tradercockpit_windows_packager", _TOOL_PATH)
assert _SPEC is not None and _SPEC.loader is not None
_PACKAGER = importlib.util.module_from_spec(_SPEC)
_SPEC.loader.exec_module(_PACKAGER)


class WindowsDesktopPackagingTests(unittest.TestCase):
    def _fixture(self, root: Path) -> Path:
        entry = root / "product/tradercockpit/desktop.py"
        entry.parent.mkdir(parents=True)
        entry.write_text("raise SystemExit(0)\n", encoding="utf-8")
        web = root / "web"
        web.mkdir()
        (web / "index.html").write_text("<main>TraderCockpit</main>", encoding="utf-8")
        return root

    def test_pyinstaller_contract_freezes_one_windowed_executable_with_canonical_web_tree(self) -> None:
        with TemporaryDirectory() as tmp:
            root = self._fixture(Path(tmp))
            dist = root / "dist"
            work = root / "build"
            args = _PACKAGER.pyinstaller_arguments(root, dist_dir=dist, work_dir=work)

        self.assertIn("--onefile", args)
        self.assertIn("--windowed", args)
        self.assertIn("--name=TraderCockpit", args)
        self.assertIn(f"--paths={root / 'product'}", args)
        self.assertIn(f"--add-data={root / 'web'}{os.pathsep}web", args)
        self.assertEqual(args[-1], str(root / "product/tradercockpit/desktop.py"))
        self.assertFalse(any("web-copy" in item for item in args))

    def test_packaging_contract_refuses_missing_canonical_inputs(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            with self.assertRaises(FileNotFoundError):
                _PACKAGER.pyinstaller_arguments(
                    root,
                    dist_dir=root / "dist",
                    work_dir=root / "build",
                )

    def test_actual_builder_refuses_non_windows_hosts(self) -> None:
        if os.name == "nt":
            self.skipTest("non-Windows refusal is not applicable on Windows")
        with TemporaryDirectory() as tmp:
            root = self._fixture(Path(tmp))
            with self.assertRaisesRegex(RuntimeError, "must run on Windows"):
                _PACKAGER.build_windows_desktop(
                    root,
                    dist_dir=root / "dist",
                    work_dir=root / "build",
                )


if __name__ == "__main__":
    unittest.main()
