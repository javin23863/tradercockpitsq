from __future__ import annotations

from functools import lru_cache
from pathlib import Path
import subprocess
from tempfile import TemporaryDirectory
import unittest
from zipfile import ZipFile

from tradercockpit.sqx_custom_project import list_custom_project_results
from tradercockpit.sqx_databank_grid import (
    DEFAULT_MAIN_DATA_COLUMNS,
    databank_row_from_archive,
    databank_row_from_settings_xml,
    decode_sqstats_v2,
    default_main_data_view,
    empty_databank_row,
)


RETAINED_REFERENCE_HEAD = "958e2fe2910cbf71d51ae29e4951484a86fc4ab6"
RETAINED_RESULTS = (
    "references/strategyquant-x-144.2953/user/projects/Retester/databanks/Results"
)
GBPUSD_ARCHIVE = "GBPUSD_H1_1201332143.sqx"
ES_ARCHIVE = "ES_H1_8101411175.sqx"
EURJPY_ARCHIVE = "EURJPY_H4_2004346173.sqx"


def _git(args: list[str]) -> subprocess.CompletedProcess[bytes]:
    return subprocess.run(
        ["git", *args],
        check=False,
        capture_output=True,
        cwd=Path(__file__).resolve().parents[2],
    )


@lru_cache(maxsize=8)
def _retained_archive(name: str) -> bytes:
    shown = _git(["show", f"{RETAINED_REFERENCE_HEAD}:{RETAINED_RESULTS}/{name}"])
    if shown.returncode != 0 or not shown.stdout:
        fetched = _git(["fetch", "--no-tags", "--depth=1", "origin", RETAINED_REFERENCE_HEAD])
        if fetched.returncode != 0:
            raise AssertionError(fetched.stderr.decode("utf-8", "replace") or "git fetch failed")
        shown = _git(["show", f"{RETAINED_REFERENCE_HEAD}:{RETAINED_RESULTS}/{name}"])
    if shown.returncode != 0 or not shown.stdout:
        raise AssertionError(
            shown.stderr.decode("utf-8", "replace") or f"retained databank archive missing: {name}"
        )
    return shown.stdout


def _runtime(root: Path) -> Path:
    (root / "internal/web/SQUANT").mkdir(parents=True)
    (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
    (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
    return root


def _write_project(home: Path, project: str) -> None:
    path = home / "user" / "projects" / project / "project.cfx"
    path.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(path, "w") as archive:
        archive.writestr("config.xml", "<Settings><Project/></Settings>")


class SqxDatabankGridTests(unittest.TestCase):
    def test_default_main_data_view_is_native_is_contract(self) -> None:
        view = default_main_data_view()
        self.assertEqual(view["name"], "Default - Main data")
        self.assertEqual(view["sample_type"], 10)
        self.assertEqual(view["result_type"], "main")
        classes = [item["class"] for item in view["columns"]]
        self.assertEqual(classes[0], "ResultsName")
        self.assertEqual(classes[1], "FiltersResult")
        self.assertNotIn("Note", classes)
        self.assertEqual(
            classes[2:],
            [
                "Fitness",
                "Symbol",
                "TimeFrame",
                "NetProfit",
                "MiniEquityChart",
                "NumberOfTrades",
                "ProfitFactor",
                "SharpeRatio",
                "RExpectancy",
                "AnnualPctReturn",
                "Stability",
                "Symmetry",
                "Drawdown",
                "WinLossRatio",
                "ReturnDDRatio",
                "AnnualPctReturnDDRatio",
                "AvgWin",
                "AvgLoss",
                "AvgBarsWin",
                "AvgBarsLoss",
                "AvgBarsInTrade",
                "Exposure",
            ],
        )
        headers = {item["class"]: item["header"] for item in view["columns"]}
        self.assertEqual(headers["NetProfit"], "Net profit (IS)")
        self.assertEqual(headers["ResultsName"], "Strategy Name")
        self.assertEqual(headers["FiltersResult"], "Filters result")
        self.assertEqual(len(DEFAULT_MAIN_DATA_COLUMNS), 24)

    def test_sqstats_v2_decoder_reads_named_producer_keys(self) -> None:
        import base64
        import struct
        from io import BytesIO

        buf = BytesIO()
        buf.write(b"\x01")
        buf.write(b"\x0a")
        buf.write(struct.pack(">i", 786))
        buf.write(b"\x03")
        buf.write(b"\x0a")
        buf.write(struct.pack(">f", 6305.8))
        payload = base64.b64encode(buf.getvalue()).decode("ascii")
        decoded = decode_sqstats_v2(payload)
        self.assertEqual(decoded["NumberOfTrades"], 786)
        self.assertAlmostEqual(float(decoded["NetProfit"]), 6305.8, places=1)

    def test_missing_resultsgroup_is_dashes_not_zeros(self) -> None:
        row = databank_row_from_settings_xml(
            b"<Settings><Symbol>ES</Symbol></Settings>",
            archive_name="Example.sqx",
        )
        self.assertEqual(row["strategy_name"], "Example")
        self.assertIsNone(row["filters_result"])
        self.assertIsNone(row["cells"]["NetProfit"])
        self.assertIsNone(row["cells"]["NumberOfTrades"])
        self.assertIsNone(row["cells"]["Fitness"])
        self.assertEqual(row["cells"]["ResultsName"], "Example")
        empty = empty_databank_row(strategy_name="Example")
        self.assertIsNone(empty["cells"]["Drawdown"])

    def test_gbpusd_retained_archive_matches_producer_main_is_stats(self) -> None:
        snapshot = _retained_archive(GBPUSD_ARCHIVE)
        row = databank_row_from_archive(snapshot, archive_name=GBPUSD_ARCHIVE)
        cells = row["cells"]
        self.assertEqual(row["strategy_name"], "GBPUSD_H1_1201332143")
        self.assertEqual(row["filters_result"], "PASSED")
        self.assertEqual(row["symbol"], "GBPUSD_M1_dukas")
        self.assertEqual(row["timeframe"], "H1")
        self.assertEqual(row["result_key"], "Main: GBPUSD_M1_dukas/H1")
        self.assertEqual(cells["Fitness"], 0.9)
        self.assertEqual(cells["NetProfit"], 6305.8)
        self.assertEqual(cells["NumberOfTrades"], 786)
        self.assertEqual(cells["ProfitFactor"], 1.39)
        self.assertEqual(cells["SharpeRatio"], 0.86)
        self.assertEqual(cells["RExpectancy"], 0.21)
        self.assertEqual(cells["AnnualPctReturn"], 4.2)
        self.assertEqual(cells["Stability"], 0.93)
        self.assertEqual(cells["Symmetry"], 89.91)
        self.assertEqual(cells["Drawdown"], 549.5)
        self.assertEqual(cells["WinLossRatio"], 0.83)
        self.assertEqual(cells["ReturnDDRatio"], 11.48)
        self.assertEqual(cells["AnnualPctReturnDDRatio"], 0.76)
        self.assertEqual(cells["AvgWin"], 63.16)
        self.assertEqual(cells["AvgLoss"], 37.72)
        self.assertEqual(cells["AvgBarsWin"], 18.27)
        self.assertEqual(cells["AvgBarsLoss"], 9.57)
        self.assertEqual(cells["AvgBarsInTrade"], 13.5)
        self.assertEqual(cells["Exposure"], 3.07)
        self.assertIsInstance(row["mini_equity"], dict)
        self.assertGreater(len(row["mini_equity"]["values"]), 10)

    def test_failed_retained_rows_keep_producer_filter_reason(self) -> None:
        es = databank_row_from_archive(_retained_archive(ES_ARCHIVE), archive_name=ES_ARCHIVE)
        self.assertEqual(es["filters_result"], "FAILED")
        self.assertIn("Profit factor", str(es["filters_reason"]))
        self.assertEqual(es["cells"]["NetProfit"], 2895.2)
        self.assertEqual(es["cells"]["NumberOfTrades"], 1001)
        self.assertEqual(es["cells"]["ProfitFactor"], 1.05)
        self.assertEqual(es["cells"]["SharpeRatio"], 0.16)
        self.assertEqual(es["cells"]["Drawdown"], 4110.9)
        self.assertEqual(es["cells"]["Fitness"], 0.48)

        yen = databank_row_from_archive(_retained_archive(EURJPY_ARCHIVE), archive_name=EURJPY_ARCHIVE)
        self.assertEqual(yen["filters_result"], "FAILED")
        self.assertEqual(yen["cells"]["NetProfit"], -249.3)
        self.assertEqual(yen["cells"]["NumberOfTrades"], 1512)
        self.assertEqual(yen["cells"]["ProfitFactor"], 0.99)
        self.assertEqual(yen["cells"]["SharpeRatio"], -0.03)
        self.assertEqual(yen["cells"]["Stability"], -0.32)

    def test_list_results_attaches_default_view_and_producer_row(self) -> None:
        snapshot = _retained_archive(GBPUSD_ARCHIVE)
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            _write_project(home, "Example Workflow")
            bank = home / "user" / "projects" / "Example Workflow" / "databanks" / "Results"
            bank.mkdir(parents=True)
            (bank / GBPUSD_ARCHIVE).write_bytes(snapshot)
            payload = list_custom_project_results(home, "Example Workflow")
        bank_payload = payload["projects"][0]["databanks"][0]
        self.assertEqual(bank_payload["view"]["name"], "Default - Main data")
        self.assertEqual(len(bank_payload["view"]["columns"]), 24)
        row = bank_payload["strategies"][0]["databank_row"]
        self.assertEqual(row["cells"]["NetProfit"], 6305.8)
        self.assertEqual(row["cells"]["NumberOfTrades"], 786)
        self.assertEqual(row["filters_result"], "PASSED")


if __name__ == "__main__":
    unittest.main()
