from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
from zipfile import ZipFile
import zlib

from tradercockpit.sqx_builder_config import (
    SQX_BUILDER_PROJECT_RELATIVE_PATH,
    SqxBuilderConfigError,
    read_sqx_builder_project,
)


class SqxBuilderArchiveFailureTests(unittest.TestCase):
    def _runtime(self, root: Path) -> Path:
        project = root / SQX_BUILDER_PROJECT_RELATIVE_PATH
        project.parent.mkdir(parents=True)
        with ZipFile(project, "w") as archive:
            archive.writestr("config.xml", b"<Project><Chart symbol='EURUSD' timeframe='H1'/><InstrumentInfo instrument='EURUSD'/></Project>")
            archive.writestr("Build-Task1.xml", b"<BuildTask><Chart symbol='EURUSD' timeframe='H1'/><InstrumentInfo instrument='EURUSD'/></BuildTask>")
        return project

    def test_initial_encrypted_member_failure_is_normalized(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            self._runtime(root)
            with (
                patch("tradercockpit.sqx_builder_config.verified_sqx_home", return_value=root),
                patch("tradercockpit.sqx_builder_config.ZipFile.read", side_effect=RuntimeError("encrypted")),
            ):
                with self.assertRaises(SqxBuilderConfigError) as caught:
                    read_sqx_builder_project(root)
            self.assertEqual(caught.exception.code, "builder_project_archive_invalid")

    def test_initial_unsupported_compression_failure_is_normalized(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            self._runtime(root)
            with (
                patch("tradercockpit.sqx_builder_config.verified_sqx_home", return_value=root),
                patch("tradercockpit.sqx_builder_config.ZipFile.read", side_effect=NotImplementedError("compression")),
            ):
                with self.assertRaises(SqxBuilderConfigError) as caught:
                    read_sqx_builder_project(root)
            self.assertEqual(caught.exception.code, "builder_project_archive_invalid")

    def test_initial_corrupt_deflate_failure_is_normalized(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            self._runtime(root)
            with (
                patch("tradercockpit.sqx_builder_config.verified_sqx_home", return_value=root),
                patch("tradercockpit.sqx_builder_config.ZipFile.read", side_effect=zlib.error("corrupt deflate stream")),
            ):
                with self.assertRaises(SqxBuilderConfigError) as caught:
                    read_sqx_builder_project(root)
            self.assertEqual(caught.exception.code, "builder_project_archive_invalid")

    def test_initial_truncated_member_failure_is_normalized(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            self._runtime(root)
            with (
                patch("tradercockpit.sqx_builder_config.verified_sqx_home", return_value=root),
                patch("tradercockpit.sqx_builder_config.ZipFile.read", side_effect=EOFError("truncated compressed member")),
            ):
                with self.assertRaises(SqxBuilderConfigError) as caught:
                    read_sqx_builder_project(root)
            self.assertEqual(caught.exception.code, "builder_project_archive_invalid")


if __name__ == "__main__":
    unittest.main()
