from __future__ import annotations

import base64
from io import BytesIO
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from zipfile import ZipFile

from tradercockpit.sqx_custom_project import SqxCustomProjectTopologyError
from tradercockpit.sqx_custom_project_strategy import inspect_custom_project_strategy


_NATIVE_PORTFOLIO_ORDERS_BIN = base64.b64decode(
    "rO0ABXflABRTUU9yZGVyRmlsZUZvcm1hdDoxMQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEAAAACAAZBQVBMLkQAEE5ldyBTdHJhdGVneSAoMSkBAgIAAAAAAQAAAAABBAsAAABr4plUAAFGKPgAPbhR7AAAAGvimVQAPbhR7AAAAX/8K8AAQyxcKQAAAADMvrwgJP5J42h/RpGKj0aRio9GhpYAgAAAAIAAAACAAAAAgAAAAAFD1QvvQHwsPUnxETZGjqtyRq57gEnkoP9GkYqPAAAAAAAAAAABwIhZjwAAAAD/AAAAAA=="
)


def _runtime(root: Path) -> Path:
    (root / "internal/web/SQUANT").mkdir(parents=True)
    (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
    (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
    return root


def _write_project(home: Path, project: str, entries: list[tuple[str, str]]) -> Path:
    path = home / "user" / "projects" / project / "project.cfx"
    path.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(path, "w") as archive:
        for entry_name, payload in entries:
            archive.writestr(entry_name, payload)
    return path


def _strategy_archive(
    home: Path,
    project: str,
    *,
    databank: str = "Results",
    name: str = "Native.sqx",
    settings: str = '<Settings><RiskMoneyManagement><MoneyManagement><InitialCapital>10000</InitialCapital></MoneyManagement></RiskMoneyManagement></Settings>',
    orders: bytes | None = _NATIVE_PORTFOLIO_ORDERS_BIN,
    extra_entries: list[tuple[str, bytes]] | None = None,
) -> Path:
    bank = home / "user" / "projects" / project / "databanks" / databank
    bank.mkdir(parents=True, exist_ok=True)
    target = bank / name
    with ZipFile(target, "w") as archive:
        archive.writestr("settings.xml", settings.encode("utf-8"))
        archive.writestr("strategy_Portfolio.xml", b"<Strategy><Rule>native-sqx</Rule></Strategy>")
        archive.writestr("version.txt", b"144.2953")
        if orders is not None:
            archive.writestr("orders.bin", orders)
        for entry_name, payload in extra_entries or []:
            archive.writestr(entry_name, payload)
    return target


class SqxCustomProjectStrategyTests(unittest.TestCase):
    def test_inspects_orders_equity_and_refuses_invented_chart(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            _write_project(
                home,
                "Example Workflow",
                [
                    ("config.xml", "<Settings><Project/></Settings>"),
                    (
                        "Build-Task1.xml",
                        "<Settings><RiskMoneyManagement><MoneyManagement>"
                        "<InitialCapital>10000</InitialCapital></MoneyManagement></RiskMoneyManagement>"
                        '<Options><BuildTradingOptions><Params>'
                        '<Param key="StoreChartData">false</Param></Params></BuildTradingOptions></Options>'
                        "</Settings>",
                    ),
                ],
            )
            _strategy_archive(home, "Example Workflow")
            payload = inspect_custom_project_strategy(
                home,
                "Example Workflow",
                "Results",
                "Native.sqx",
                task=1,
            )

        self.assertEqual(payload["schema"], "tc.sqx-custom-project-strategy.v1")
        self.assertEqual(payload["source_build"], "144.2953")
        self.assertEqual(payload["archive"], "Native.sqx")
        self.assertEqual(payload["relative_path"], "user/projects/Example Workflow/databanks/Results/Native.sqx")
        self.assertEqual(payload["orders"]["state"], "available")
        self.assertEqual(payload["orders"]["payload"]["trade_count"], 1)
        self.assertEqual(payload["orders"]["payload"]["trades"][0]["Symbol"], "AAPL.D")
        self.assertEqual(payload["initial_capital"], 10000.0)
        self.assertEqual(payload["equity_basis"], "archive_initial_capital")
        self.assertEqual(len(payload["equity"]), 1)
        self.assertEqual(payload["chart"]["stored"], False)
        self.assertEqual(payload["chart"]["reason_code"], "chart_data_not_stored")
        self.assertNotIn("net_profit", payload)
        self.assertNotIn("NetProfit", payload)

    def test_path_escape_and_missing_archive_fail_closed(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            _write_project(home, "Example Workflow", [("config.xml", "<Settings/>")])
            with self.assertRaises(SqxCustomProjectTopologyError) as escaped:
                inspect_custom_project_strategy(home, "Example Workflow", "../Builder", "Native.sqx")
            with self.assertRaises(SqxCustomProjectTopologyError) as missing:
                inspect_custom_project_strategy(home, "Example Workflow", "Results", "Missing.sqx")
        self.assertEqual(escaped.exception.code, "custom_project_databank_name_invalid")
        self.assertEqual(missing.exception.code, "custom_project_strategy_missing")

    def test_config_diff_only_overlaps_existing_task_fields(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            _write_project(
                home,
                "Example Workflow",
                [
                    ("config.xml", "<Settings><Project/></Settings>"),
                    (
                        "Build-Task1.xml",
                        "<Settings><RiskMoneyManagement><MoneyManagement>"
                        "<InitialCapital>10000</InitialCapital></MoneyManagement></RiskMoneyManagement></Settings>",
                    ),
                ],
            )
            _strategy_archive(
                home,
                "Example Workflow",
                settings=(
                    "<Settings><RiskMoneyManagement><MoneyManagement>"
                    "<InitialCapital>12000</InitialCapital></MoneyManagement></RiskMoneyManagement>"
                    "<Invented><Row>nope</Row></Invented></Settings>"
                ),
            )
            payload = inspect_custom_project_strategy(
                home, "Example Workflow", "Results", "Native.sqx", task=1
            )
        self.assertEqual(payload["config_diff"], [
            {
                "path": ["RiskMoneyManagement", "MoneyManagement", "InitialCapital"],
                "task_value": "10000",
                "archive_value": "12000",
                "text": True,
            }
        ])


if __name__ == "__main__":
    unittest.main()
