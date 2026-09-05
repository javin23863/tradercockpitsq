from __future__ import annotations

from contextlib import closing
from hashlib import sha256
from pathlib import Path
import sqlite3
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

from tradercockpit.data_setup import DataSetupError, inspect_csv_data, read_native_data_setup, select_native_data_setup


class DataSetupTests(unittest.TestCase):
    def runtime(self, root):
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        (root / "user/data").mkdir(parents=True)
        path = root / "user/data/data.db"
        with closing(sqlite3.connect(path)) as connection:
            connection.executescript("""
                CREATE TABLE DATA(ID INTEGER PRIMARY KEY,SOURCEDATA_ID,SYMBOL,INSTRUMENT,TIMEFRAME,TIMEZONE,DATEFROM,DATETO,ROWS,DATATYPE,DECIMALS,SOURCE,BROKER_ID);
                CREATE TABLE INSTRUMENTS(INSTRUMENT TEXT PRIMARY KEY,POINTVALUE,TICKSIZE,TICKSTEP,DEFAULTSPREAD,DEFAULTSLIPPAGE,COMMISSIONS,DATATYPE,BROKER_ID);
                CREATE TABLE BROKER(ID INTEGER PRIMARY KEY,NAME,MT_USE,MT_TIMEZONE,POSTFIX);
                CREATE TABLE SESSIONS(SESSION TEXT PRIMARY KEY,BROKER_ID);
                CREATE TABLE ELEMENTS(ID INTEGER PRIMARY KEY,SESSION,DAYFROM,TIMEFROM,DAYTO,TIMETO,EOD);
                INSERT INTO DATA VALUES(7,0,'MYM','Micro Dow','H1','EETUS',1704067200000,1704153600000,24,2,0,1,3);
                INSERT INTO INSTRUMENTS VALUES('Micro Dow',0.5,1,1,0,0,'<Method type="None"/>',2,3);
                INSERT INTO BROKER VALUES(3,'Native Broker',1,'EETUS','.broker');
                INSERT INTO SESSIONS VALUES('Broker session',3);
                INSERT INTO ELEMENTS VALUES(1,'Broker session',1,0,5,2359,1);
            """)
            connection.commit()
        return root, path

    def test_native_selection_preserves_zero_costs_and_source_metadata_without_execution(self):
        with TemporaryDirectory() as tmp:
            home, path = self.runtime(Path(tmp))
            before = path.read_bytes()
            catalog = read_native_data_setup(home)
            self.assertEqual(catalog["status"], "available")
            self.assertEqual(catalog["datasets"][0]["bar_timestamp_convention"], "end_of_bar")
            self.assertEqual(catalog["datasets"][0]["sessions"][0], "Broker session")
            selected = select_native_data_setup(home, {"dataset_id": "sqx-data-7", "snapshot_sha256": catalog["source"]["snapshot_sha256"]})
            self.assertEqual(selected["status"], "resolved")
            self.assertEqual(selected["fields"]["point_value"]["value"], 0.5)
            self.assertEqual(selected["fields"]["default_spread"]["value"], 0)
            self.assertEqual(selected["fields"]["timezone"]["state"], "observed_native")
            self.assertFalse(selected["backtest_ready"])
            self.assertFalse(selected["native_import_performed"])
            self.assertEqual(path.read_bytes(), before)

    def test_native_wal_changes_invalidate_selection_and_missing_fields_stay_unknown(self):
        with TemporaryDirectory() as tmp:
            home, path = self.runtime(Path(tmp))
            with closing(sqlite3.connect(path)) as writer:
                writer.execute("PRAGMA journal_mode=WAL")
                initial = read_native_data_setup(home)
                writer.execute("UPDATE DATA SET TIMEZONE='Etc/UCT',DATATYPE=0")
                writer.execute("UPDATE INSTRUMENTS SET DEFAULTSPREAD=NULL")
                writer.commit()
                with self.assertRaises(DataSetupError) as stale:
                    select_native_data_setup(home, {"dataset_id": "sqx-data-7", "snapshot_sha256": initial["source"]["snapshot_sha256"]})
                self.assertEqual(stale.exception.code, "native_data_catalog_changed")
                current = read_native_data_setup(home)
                selected = select_native_data_setup(home, {"dataset_id": "sqx-data-7", "snapshot_sha256": current["source"]["snapshot_sha256"]})
                self.assertEqual(selected["status"], "needs_review")
                self.assertIsNone(selected["fields"]["default_spread"]["value"])
                self.assertIsNone(selected["fields"]["bar_timestamp_convention"]["value"])
                self.assertEqual(selected["conflicts"][0]["reason_code"], "dataset_broker_timezone_difference")

    def test_extra_fields_paths_and_unknown_dataset_are_refused(self):
        with TemporaryDirectory() as tmp:
            home, _ = self.runtime(Path(tmp))
            digest = read_native_data_setup(home)["source"]["snapshot_sha256"]
            for payload in ({"dataset_id": "../../data", "snapshot_sha256": digest},
                            {"dataset_id": "sqx-data-7", "snapshot_sha256": digest, "path": "C:/other"},
                            {"dataset_id": "sqx-data-99", "snapshot_sha256": digest}):
                with self.subTest(payload=payload), self.assertRaises(DataSetupError):
                    select_native_data_setup(home, payload)

    def test_native_catalog_path_redirection_is_refused(self):
        with TemporaryDirectory() as tmp:
            home, path = self.runtime(Path(tmp) / "home")
            external = Path(tmp) / "outside.db"
            path.replace(external)
            try:
                path.symlink_to(external)
            except OSError:
                self.skipTest("Host does not permit symlinks")
            self.assertEqual(read_native_data_setup(home)["reason_code"], "data_catalog_path_escape")

    def test_csv_trade_station_detects_columns_but_does_not_infer_timezone(self):
        raw = b'"Date","Time","Open","High","Low","Close","Up","Down"\n07/13/2023,18:00,100,102,99,101,8,9\n07/13/2023,19:00,101,103,100,102,7,4\n'
        result = inspect_csv_data(raw)
        self.assertEqual(result["source_sha256"], sha256(raw).hexdigest())
        self.assertEqual(result["columns"]["up"], "Up")
        self.assertEqual(result["row_count"], 2)
        self.assertEqual(result["observed_interval_seconds"], 3600)
        self.assertIsNone(result["timestamp_timezone"])
        self.assertEqual([issue["code"] for issue in result["issues"]], ["timestamp_timezone_unresolved"])
        self.assertFalse(result["backtest_ready"])
        self.assertNotIn("dataset_id", result)

    def test_mt5_tabs_and_explicit_tradingview_timestamps(self):
        mt5 = '<DATE>\t<TIME>\t<OPEN>\t<HIGH>\t<LOW>\t<CLOSE>\t<TICKVOL>\t<VOL>\t<SPREAD>\n2023.01.13\t12:00:00\t1\t2\t0\t1\t5\t0\t2\n'
        parsed = inspect_csv_data(mt5.encode('utf-16'))
        self.assertEqual(parsed["columns"]["tick_volume"], "<TICKVOL>")
        self.assertIsNone(parsed["timestamp_timezone"])
        for stamps in (("1704067200", "1704070800"), ("2024-01-01T00:00:00Z", "2024-01-01T01:00:00Z")):
            raw = ('time,open,high,low,close\n' + '\n'.join(f'{at},1,2,0,1' for at in stamps)).encode()
            result = inspect_csv_data(raw)
            numeric = stamps[0].isdigit()
            self.assertEqual(result["timestamp_timezone"], None if numeric else "UTC")
            self.assertEqual(result["status"], "needs_review" if numeric else "inspected")
            self.assertEqual(result["observed_interval_seconds"], 3600)

    def test_ambiguous_dates_errors_and_offset_changes_are_not_hidden(self):
        ambiguous = inspect_csv_data(b'Date,Time,Open,High,Low,Close\n01/02/2023,12:00,1,2,0,1\n')
        self.assertIn("ambiguous_date_format", [item["code"] for item in ambiguous["issues"]])
        self.assertIsNone(ambiguous["date_from"])
        raw = b'time,open,high,low,close\n2024-10-27T01:00:00+02:00,1,0,2,1\n2024-10-27T02:00:00+01:00,1,2,0,1\n2024-10-27T02:00:00+01:00,1,2,0,1\n'
        issues = {item["code"] for item in inspect_csv_data(raw)["issues"]}
        self.assertTrue({"invalid_ohlc", "duplicate_timestamps", "explicit_timezone_offset_changes"} <= issues)
        mixed = inspect_csv_data(b'time,open,high,low,close\n2024-01-01T00:00:00Z,1,2,0,1\n2024-01-01T01:00:00,1,2,0,1\n')
        self.assertIsNone(mixed["timestamp_timezone"])
        self.assertIsNone(mixed["date_from"])
        self.assertIsNone(mixed["date_to"])
        self.assertIsNone(mixed["observed_interval_seconds"])

    def test_numeric_datetime_ambiguity_and_nonadjacent_duplicates(self):
        parsed = inspect_csv_data(b'time,open,high,low,close\n2026010100,1,2,0,1\n2026010101,1,2,0,1\n2026010100,1,2,0,1\n')
        self.assertIsNone(parsed["timestamp_timezone"])
        issues = {item["code"]: item["count"] for item in parsed["issues"]}
        self.assertEqual(issues["numeric_timestamp_unit_unconfirmed"], 3)
        self.assertEqual(issues["duplicate_timestamps"], 1)

    def test_competing_clock_columns_are_refused(self):
        raw = b'date,time,timestamp,open,high,low,close\n2020-01-01,00:00,2024-01-01T00:00:00Z,1,2,0,1\n2020-01-01,01:00,2024-01-01T01:00:00Z,1,2,0,1\n'
        with self.assertRaises(DataSetupError) as caught:
            inspect_csv_data(raw)
        self.assertEqual(caught.exception.code, 'data_file_columns_invalid')

    def test_limits_and_missing_native_catalog_do_not_prevent_standalone_csv_inspection(self):
        self.assertEqual(read_native_data_setup(None)["status"], "unavailable")
        for raw in (b'', b'\x00bad', b'unknown,headers\n1,2\n'):
            with self.subTest(raw=raw), self.assertRaises(DataSetupError):
                inspect_csv_data(raw)
        with patch('tradercockpit.data_setup.MAX_DATA_FILE_ROWS', 1), self.assertRaises(DataSetupError) as caught:
            inspect_csv_data(b'time,open,high,low,close\n1704067200,1,2,0,1\n1704070800,1,2,0,1\n')
        self.assertEqual(caught.exception.code, 'data_file_row_limit')


if __name__ == "__main__":
    unittest.main()
