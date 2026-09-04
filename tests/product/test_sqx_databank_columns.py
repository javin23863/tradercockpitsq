from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.app_server import sqx_databank_columns_response
from tradercockpit.sqx_custom_project import SqxCustomProjectTopologyError
from tradercockpit.sqx_custom_project_strategy import (
    DATABANK_COLUMN_KEYS,
    SQX_DATABANK_COLUMNS_SCHEMA,
    databank_columns_record,
    inspect_custom_project_strategy,
)

try:
    from test_sqx_custom_project_strategy import _runtime, _strategy_archive, _write_project
except ModuleNotFoundError:  # direct module invocation outside discover
    from tests.product.test_sqx_custom_project_strategy import _runtime, _strategy_archive, _write_project


def _project(home: Path) -> None:
    _write_project(home, "Example Workflow", [
        ("config.xml", '<Settings><Project><Tasks><Task name="Build strategies" type="Build" active="true" taskXMLFile="Build-Task1.xml"/></Tasks></Project></Settings>'),
        ("Build-Task1.xml", "<Settings><Build/></Settings>"),
    ])


class DatabankColumnsTests(unittest.TestCase):
    def test_columns_match_strategy_overview_and_mark_unreadable_archives(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            _project(home)
            _strategy_archive(home, "Example Workflow", name="Alpha.sqx")
            _strategy_archive(home, "Example Workflow", name="NoOrders.sqx", orders=None)
            record = databank_columns_record(home, "Example Workflow", "Results")
            detail = inspect_custom_project_strategy(home, "Example Workflow", "Results", "Alpha.sqx")
        self.assertEqual(record["schema"], SQX_DATABANK_COLUMNS_SCHEMA)
        self.assertEqual(record["basis"], "sqx_column_formulas_over_orders.bin")
        self.assertEqual(record["column_keys"], list(DATABANK_COLUMN_KEYS))
        self.assertEqual((record["archive_count"], record["computed_count"], record["truncated"]), (2, 2, False))
        rows = {row["archive"]: row for row in record["rows"]}
        alpha = rows["Alpha.sqx"]
        self.assertEqual(alpha["state"], "ready")
        overview_all = detail["statistics"]["full"]["all"]
        for key in ("NetProfit", "NumberOfTrades", "ProfitFactor", "Drawdown", "WinningPct"):
            self.assertEqual(alpha["columns"][key], overview_all[key], key)
        empty = rows["NoOrders.sqx"]
        self.assertEqual(empty["state"], "unavailable")
        self.assertIsNone(empty["columns"])
        self.assertTrue(empty["reason_code"])

    def test_columns_are_bounded_and_fail_closed_on_bad_identity(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            _project(home)
            for index in range(3):
                _strategy_archive(home, "Example Workflow", name=f"S{index}.sqx")
            bounded = databank_columns_record(home, "Example Workflow", "Results", limit=2)
            self.assertEqual((bounded["archive_count"], bounded["computed_count"], bounded["truncated"]), (3, 2, True))
            with self.assertRaises(SqxCustomProjectTopologyError) as missing:
                databank_columns_record(home, "Example Workflow", "Nope")
            self.assertEqual(missing.exception.code, "custom_project_databank_missing")
            status, payload = sqx_databank_columns_response(home, "Example Workflow", "../etc")
            self.assertEqual(status, 400)
            self.assertEqual(payload["error"], "invalid_request")
            status, payload = sqx_databank_columns_response(home, "Missing Project", "Results")
            self.assertEqual(status, 404)
            status, payload = sqx_databank_columns_response(None, "Example Workflow", "Results")
            self.assertEqual(status, 503)


if __name__ == "__main__":
    unittest.main()
