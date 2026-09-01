from __future__ import annotations

from hashlib import sha256
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from zipfile import ZipFile

from tradercockpit.sqx_builder_config import (
    SQX_BUILDER_CROSS_CHECKS_SCHEMA,
    SQX_BUILDER_PROJECT_RELATIVE_PATH,
    builder_project_config_record,
)


class SqxBuilderCrossChecksTests(unittest.TestCase):
    def _runtime(self, root: Path) -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        return root

    def _write_project(self, home: Path, task_xml: str) -> Path:
        path = home / SQX_BUILDER_PROJECT_RELATIVE_PATH
        path.parent.mkdir(parents=True, exist_ok=True)
        with ZipFile(path, "w") as archive:
            archive.writestr(
                "config.xml",
                '<Project><Chart symbol="DJ_M1_dukas" timeframe="H1"/>'
                '<InstrumentInfo instrument="USA30.IDX_dukascopy"/></Project>',
            )
            archive.writestr("Build-Task1.xml", task_xml)
        return path

    def test_cross_checks_preserve_exact_unknown_native_structure(self) -> None:
        task = """<Task>
  <CrossChecks use="true" futureRoot="opaque">
    <UnknownCheck kind="producer-owned">
      <FutureSetting representation="native">abc</FutureSetting>
    </UnknownCheck>
  </CrossChecks>
</Task>"""
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            project = self._write_project(home, task)
            expected_digest = sha256(project.read_bytes()).hexdigest()
            record = builder_project_config_record(home)

        cross_checks = record["cross_checks"]
        self.assertEqual(cross_checks["schema"], SQX_BUILDER_CROSS_CHECKS_SCHEMA)
        self.assertEqual(cross_checks["authority"], "native_sqx_read_only")
        self.assertEqual(cross_checks["source"]["source_build"], "144.2953")
        self.assertEqual(cross_checks["source"]["project"], "Builder")
        self.assertEqual(cross_checks["source"]["relative_path"], SQX_BUILDER_PROJECT_RELATIVE_PATH)
        self.assertEqual(cross_checks["source"]["archive_sha256"], expected_digest)
        self.assertEqual(cross_checks["source"]["member"], "Build-Task1.xml")
        self.assertTrue(cross_checks["enabled"])

        root = cross_checks["producer_configuration"]
        self.assertEqual(root["tag"], "CrossChecks")
        self.assertEqual(root["attributes"], {"use": "true", "futureRoot": "opaque"})
        self.assertEqual(root["children"][0]["tag"], "UnknownCheck")
        self.assertEqual(root["children"][0]["attributes"], {"kind": "producer-owned"})
        self.assertEqual(root["children"][0]["children"][0]["tag"], "FutureSetting")
        self.assertEqual(root["children"][0]["children"][0]["attributes"], {"representation": "native"})
        self.assertEqual(root["children"][0]["children"][0]["text"], "abc")
        self.assertFalse(cross_checks["semantics"]["interpreted_by_tradercockpit"])
        self.assertEqual(cross_checks["semantics"]["owner"], "StrategyQuant X")
        self.assertFalse(cross_checks["execution"]["available"])
        self.assertEqual(
            cross_checks["execution"]["reason"],
            "native_sqx_builder_owns_cross_check_configuration",
        )

        validation = next(
            item for item in record["specification"]["requirements"]
            if item["id"] == "validation_profile"
        )
        self.assertEqual(validation["state"], "producer_configured")
        self.assertTrue(validation["required"])
        self.assertEqual(validation["values"], {"section_present": True, "enabled": True})

    def test_disabled_or_absent_cross_checks_do_not_gain_validation_semantics(self) -> None:
        cases = (
            ("<Task><CrossChecks use=\"false\"><Opaque/></CrossChecks></Task>", True),
            ("<Task/>", False),
        )
        for task, present in cases:
            with self.subTest(present=present), TemporaryDirectory() as tmp:
                home = self._runtime(Path(tmp))
                self._write_project(home, task)
                record = builder_project_config_record(home)

                cross_checks = record["cross_checks"]
                self.assertFalse(cross_checks["enabled"])
                self.assertEqual(cross_checks["producer_configuration"] is not None, present)
                validation = next(
                    item for item in record["specification"]["requirements"]
                    if item["id"] == "validation_profile"
                )
                self.assertEqual(validation["state"], "not_applicable")
                self.assertFalse(validation["required"])
                self.assertEqual(
                    validation["values"],
                    {"section_present": present, "enabled": False},
                )


if __name__ == "__main__":
    unittest.main()
