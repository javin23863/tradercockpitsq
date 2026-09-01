from __future__ import annotations

from hashlib import sha256
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from zipfile import ZipFile

from tradercockpit.sqx_builder_config import (
    SQX_BUILDER_MONEY_MANAGEMENT_SCHEMA,
    SQX_BUILDER_PROJECT_RELATIVE_PATH,
    builder_project_config_record,
)


class SqxBuilderMoneyManagementTests(unittest.TestCase):
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

    def test_money_management_preserves_exact_unknown_native_structure(self) -> None:
        task = """<Task>
  <MoneyManagement futureRoot="opaque">
    <UnknownSizingModel kind="producer-owned">
      <FutureParameter representation="native">0.73</FutureParameter>
    </UnknownSizingModel>
  </MoneyManagement>
</Task>"""
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            project = self._write_project(home, task)
            expected_digest = sha256(project.read_bytes()).hexdigest()
            record = builder_project_config_record(home)

        money_management = record["money_management"]
        self.assertEqual(money_management["schema"], SQX_BUILDER_MONEY_MANAGEMENT_SCHEMA)
        self.assertEqual(money_management["authority"], "native_sqx_read_only")
        self.assertEqual(money_management["source"]["source_build"], "144.2953")
        self.assertEqual(money_management["source"]["project"], "Builder")
        self.assertEqual(money_management["source"]["relative_path"], SQX_BUILDER_PROJECT_RELATIVE_PATH)
        self.assertEqual(money_management["source"]["archive_sha256"], expected_digest)
        self.assertEqual(money_management["source"]["member"], "Build-Task1.xml")

        root = money_management["producer_configuration"]
        self.assertEqual(root["tag"], "MoneyManagement")
        self.assertEqual(root["attributes"], {"futureRoot": "opaque"})
        self.assertEqual(root["children"][0]["tag"], "UnknownSizingModel")
        self.assertEqual(root["children"][0]["attributes"], {"kind": "producer-owned"})
        parameter = root["children"][0]["children"][0]
        self.assertEqual(parameter["tag"], "FutureParameter")
        self.assertEqual(parameter["attributes"], {"representation": "native"})
        self.assertEqual(parameter["text"], "0.73")
        self.assertFalse(money_management["semantics"]["interpreted_by_tradercockpit"])
        self.assertEqual(money_management["semantics"]["owner"], "StrategyQuant X")
        self.assertFalse(money_management["execution"]["available"])
        self.assertEqual(
            money_management["execution"]["reason"],
            "native_sqx_builder_owns_money_management_configuration",
        )

        sizing = next(
            item for item in record["specification"]["requirements"]
            if item["id"] == "money_management"
        )
        self.assertEqual(sizing["state"], "producer_configured")
        self.assertTrue(sizing["required"])
        self.assertEqual(sizing["values"], {"section_present": True})

    def test_absent_money_management_remains_unresolved_without_invented_defaults(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, "<Task/>")
            record = builder_project_config_record(home)

        self.assertIsNone(record["money_management"]["producer_configuration"])
        sizing = next(
            item for item in record["specification"]["requirements"]
            if item["id"] == "money_management"
        )
        self.assertEqual(sizing["state"], "unresolved")
        self.assertTrue(sizing["required"])
        self.assertEqual(sizing["values"], {"section_present": False})


if __name__ == "__main__":
    unittest.main()
