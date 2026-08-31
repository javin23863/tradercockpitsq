from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from zipfile import ZipFile

from tradercockpit.sqx_custom_project import (
    SqxCustomProjectTopologyError,
    read_sqx_custom_project_topology,
)


class SqxCustomProjectPathSecurityTests(unittest.TestCase):
    def _runtime(self, root: Path) -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        (root / "user/projects").mkdir(parents=True)
        return root

    def test_rejects_direct_project_child_that_resolves_outside_sqx_home(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx")
            outside = root / "outside-project"
            outside.mkdir()
            with ZipFile(outside / "project.cfx", "w") as archive:
                archive.writestr("config.xml", "<Settings/>")

            alias = home / "user/projects/Escape"
            try:
                alias.symlink_to(outside, target_is_directory=True)
            except OSError as exc:
                self.skipTest(f"directory symlinks unavailable on this platform: {exc}")

            with self.assertRaises(SqxCustomProjectTopologyError) as caught:
                read_sqx_custom_project_topology(home, "Escape")

        self.assertEqual(caught.exception.code, "custom_project_path_escape")


if __name__ == "__main__":
    unittest.main()
