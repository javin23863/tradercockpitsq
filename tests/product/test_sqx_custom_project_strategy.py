from __future__ import annotations

import base64
from datetime import datetime, timedelta, timezone
from io import BytesIO
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from zipfile import ZipFile

from tradercockpit.sqx_custom_project import SqxCustomProjectTopologyError
from tradercockpit.sqx_custom_project_strategy import (
    _chart_bars_record,
    _window_sidecar_bars,
    inspect_custom_project_strategy,
)
from tradercockpit.research_verdicts import round2


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
    version: str = "144.2953",
) -> Path:
    bank = home / "user" / "projects" / project / "databanks" / databank
    bank.mkdir(parents=True, exist_ok=True)
    target = bank / name
    with ZipFile(target, "w") as archive:
        archive.writestr("settings.xml", settings.encode("utf-8"))
        archive.writestr("strategy_Portfolio.xml", b"<Strategy><Rule>native-sqx</Rule></Strategy>")
        archive.writestr("version.txt", version.encode("utf-8"))
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
        self.assertEqual(payload["chart"]["bars"]["state"], "unavailable")
        self.assertFalse(payload["results_plugins"][0]["installed"])
        self.assertEqual(payload["statistics"]["full"]["all"]["PayoutRatio"], 0.0)
        self.assertNotIn("net_profit", payload)
        self.assertEqual(payload["statistics"]["basis"], "sqx_column_formulas_over_orders.bin")
        self.assertEqual(payload["statistics"]["full"]["all"]["NumberOfTrades"], 1)
        self.assertEqual(
            payload["statistics"]["full"]["all"]["NetProfit"],
            round2(payload["orders"]["payload"]["trades"][0]["PL"]),
        )
        self.assertEqual(payload["source"]["state"], "available")
        self.assertIn("<Strategy>", payload["source"]["text"])

    def test_older_version_txt_stays_inspectable_for_custom_project_databanks(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            _write_project(home, "Example Workflow", [("config.xml", "<Settings><Project/></Settings>")])
            _strategy_archive(home, "Example Workflow", version="1")
            payload = inspect_custom_project_strategy(home, "Example Workflow", "Results", "Native.sqx")
        self.assertEqual(payload["native_version"], "1")
        self.assertEqual(payload["source_build"], "144.2953")
        self.assertEqual(payload["orders"]["state"], "available")
        self.assertEqual(payload["statistics"]["full"]["all"]["NumberOfTrades"], 1)

    def test_tradestation_sidecar_fills_trades_on_chart_without_inventing_candles(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            _write_project(home, "Example Workflow", [("config.xml", "<Settings><Project/></Settings>")])
            _strategy_archive(home, "Example Workflow")
            payload = inspect_custom_project_strategy(home, "Example Workflow", "Results", "Native.sqx")
            trade = payload["orders"]["payload"]["trades"][0]
            opened = datetime.fromtimestamp(int(trade["OpenTime"]) / 1000, tz=timezone.utc)
            closed = datetime.fromtimestamp(int(trade["CloseTime"]) / 1000, tz=timezone.utc)
            price = float(trade["OpenPrice"])
            sidecar = home / "user/projects/Example Workflow/databanks/Results/Native.txt"
            sidecar.write_text(
                "Date,Time,Open,High,Low,Close,Up,Down\n"
                f"{opened.strftime('%m/%d/%Y')},{opened.strftime('%H:%M')},{price-1},{price+1},{price-2},{price},1,1\n"
                f"{closed.strftime('%m/%d/%Y')},{closed.strftime('%H:%M')},{price},{price+2},{price-1},{price+1},1,1\n",
                encoding="utf-8",
            )
            payload = inspect_custom_project_strategy(home, "Example Workflow", "Results", "Native.sqx")
        self.assertEqual(payload["chart"]["stored"], False)
        self.assertEqual(payload["chart"]["bars"]["state"], "available")
        self.assertEqual(payload["chart"]["bars"]["basis"], "databank_sidecar_tradestation_csv")
        self.assertGreaterEqual(len(payload["chart"]["bars"]["bars"]), 1)
        self.assertEqual(payload["chart"]["bars"]["bars"][0]["open_time"][-1], "Z")

    def test_sidecar_window_centers_on_focus_trade_not_span_tail(self) -> None:
        parsed = [{"time_ms": index * 3_600_000} for index in range(600)]
        focused = _window_sidecar_bars(parsed, focus_ms=50 * 3_600_000, limit=500)
        tail = parsed[-500:]
        self.assertEqual(len(focused), 500)
        self.assertEqual(focused[0]["time_ms"], 0)
        self.assertIn(50 * 3_600_000, [bar["time_ms"] for bar in focused])
        self.assertNotIn(50 * 3_600_000, [bar["time_ms"] for bar in tail])

    def test_chart_bars_focus_ticket_keeps_early_trade_on_clipped_sidecar(self) -> None:
        with TemporaryDirectory() as tmp:
            home = Path(tmp)
            archive = home / "Native.sqx"
            archive.write_bytes(b"sqx")
            start = datetime(2020, 1, 1, tzinfo=timezone.utc)
            lines = ["Date,Time,Open,High,Low,Close,Up,Down\n"]
            for index in range(600):
                stamp = start + timedelta(hours=index)
                lines.append(
                    f"{stamp.strftime('%m/%d/%Y')},{stamp.strftime('%H:%M')},1,2,0.5,1.5,1,1\n"
                )
            (home / "Native.txt").write_text("".join(lines), encoding="utf-8")
            first_ms = int(start.timestamp() * 1000)
            last_ms = int((start + timedelta(hours=599)).timestamp() * 1000)
            trades = [
                {"Ticket": 1, "OpenTime": first_ms, "CloseTime": first_ms, "Symbol": "ES"},
                {"Ticket": 2, "OpenTime": last_ms, "CloseTime": last_ms, "Symbol": "ES"},
            ]
            early = _chart_bars_record(
                archive, home, trades, stored=False, store_flag=None, focus_ticket=1
            )
            late = _chart_bars_record(
                archive, home, trades, stored=False, store_flag=None, focus_ticket=2
            )
        self.assertEqual(early["state"], "available")
        self.assertEqual(len(early["bars"]), 500)
        self.assertEqual(early["bars"][0]["open_time"], "2020-01-01T00:00:00Z")
        self.assertNotEqual(late["bars"][0]["open_time"], "2020-01-01T00:00:00Z")

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
