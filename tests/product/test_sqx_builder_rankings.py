from __future__ import annotations

from hashlib import sha256
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from zipfile import ZipFile

from tradercockpit.sqx_builder_config import (
    SQX_BUILDER_PROJECT_RELATIVE_PATH,
    SQX_BUILDER_RANKINGS_SCHEMA,
    builder_project_config_record,
)


class SqxBuilderRankingsTests(unittest.TestCase):
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

    def test_rankings_preserve_exact_unknown_native_structure(self) -> None:
        task = """<Task>
  <Rankings futureRoot="opaque">
    <MaxStrategies>321</MaxStrategies>
    <StopCondition type="90m" futureFlag="native">
      <FutureStopField representation="producer-owned">native-value</FutureStopField>
    </StopCondition>
    <UnknownRankingNode mode="opaque">producer-owned</UnknownRankingNode>
  </Rankings>
</Task>"""
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            project = self._write_project(home, task)
            expected_digest = sha256(project.read_bytes()).hexdigest()
            record = builder_project_config_record(home)

        rankings = record["rankings"]
        self.assertEqual(rankings["schema"], SQX_BUILDER_RANKINGS_SCHEMA)
        self.assertEqual(rankings["authority"], "native_sqx_read_only")
        self.assertEqual(rankings["source"]["source_build"], "144.2953")
        self.assertEqual(rankings["source"]["project"], "Builder")
        self.assertEqual(rankings["source"]["relative_path"], SQX_BUILDER_PROJECT_RELATIVE_PATH)
        self.assertEqual(rankings["source"]["archive_sha256"], expected_digest)
        self.assertEqual(rankings["source"]["member"], "Build-Task1.xml")

        root = rankings["producer_configuration"]
        self.assertEqual(root["tag"], "Rankings")
        self.assertEqual(root["attributes"], {"futureRoot": "opaque"})
        self.assertEqual(root["children"][0]["tag"], "MaxStrategies")
        self.assertEqual(root["children"][0]["text"], "321")
        stop = root["children"][1]
        self.assertEqual(stop["tag"], "StopCondition")
        self.assertEqual(stop["attributes"], {"type": "90m", "futureFlag": "native"})
        self.assertEqual(stop["children"][0]["tag"], "FutureStopField")
        self.assertEqual(stop["children"][0]["attributes"], {"representation": "producer-owned"})
        self.assertEqual(stop["children"][0]["text"], "native-value")
        self.assertEqual(root["children"][2]["tag"], "UnknownRankingNode")
        self.assertEqual(root["children"][2]["text"], "producer-owned")
        self.assertFalse(rankings["semantics"]["interpreted_by_tradercockpit"])
        self.assertEqual(rankings["semantics"]["owner"], "StrategyQuant X")
        self.assertFalse(rankings["execution"]["available"])
        self.assertEqual(
            rankings["execution"]["reason"],
            "native_sqx_builder_owns_ranking_configuration",
        )

    def test_missing_rankings_remain_absent_without_invented_defaults(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self._write_project(home, "<Task/>")
            record = builder_project_config_record(home)

        self.assertIsNone(record["rankings"]["producer_configuration"])
        ranking_requirement = next(
            item for item in record["specification"]["requirements"]
            if item["id"] == "ranking_filters"
        )
        self.assertEqual(ranking_requirement["state"], "unresolved")
        self.assertIsNone(ranking_requirement["values"]["max_strategies"])
        self.assertIsNone(ranking_requirement["values"]["stop_condition_type"])


if __name__ == "__main__":
    unittest.main()
