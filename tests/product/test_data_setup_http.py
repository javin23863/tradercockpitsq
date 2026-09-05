from http.server import ThreadingHTTPServer
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Thread
import unittest
from unittest.mock import patch
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from tradercockpit.app_server import MAX_DATA_FILE_BYTES, make_handler


class DataSetupHttpTests(unittest.TestCase):
    def setUp(self):
        self.temp = TemporaryDirectory()
        self.root = Path(self.temp.name)
        (self.root / "index.html").write_text("Data setup", encoding="utf-8")
        self.server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(self.root, None))
        self.thread = Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.origin = f"http://127.0.0.1:{self.server.server_port}"

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=2)
        self.temp.cleanup()

    def request(self, path, body=None, headers=None):
        values = {"Origin": self.origin, "Content-Type": "application/octet-stream"}
        values.update(headers or {})
        request = Request(self.origin + path, data=body, headers=values)
        try:
            response = urlopen(request, timeout=5)
        except HTTPError as error:
            response = error
        with response:
            return response.status, json.loads(response.read())

    def test_file_inspection_works_without_engine_and_does_not_import(self):
        raw = b"time,open,high,low,close\n2026-01-01T00:00:00Z,1,3,1,2\n2026-01-01T01:00:00Z,2,4,1,3\n"
        status, body = self.request("/api/data-setup/inspect", raw)
        self.assertEqual(status, 200, body)
        self.assertEqual(body["schema"], "tc.data-file-inspection.v1")
        self.assertEqual(body["row_count"], 2)
        self.assertFalse(body["native_import_performed"])
        self.assertFalse(body["backtest_ready"])
        self.assertEqual([p.name for p in self.root.iterdir()], ["index.html"])
        status, catalog = self.request("/api/data-setup")
        self.assertEqual(status, 200)
        self.assertEqual(catalog["status"], "unavailable")

    def test_raw_upload_rejects_wrong_origin_type_query_and_oversize(self):
        cases = [
            ("/api/data-setup/inspect", {"Origin": "https://external.example"}, 403),
            ("/api/data-setup/inspect", {"Origin": "null"}, 403),
            ("/api/data-setup/inspect", {"Content-Type": "text/plain"}, 415),
            ("/api/data-setup/inspect?path=C:/private.csv", {}, 400),
            ("/api/data-setup/inspect", {"Content-Length": str(MAX_DATA_FILE_BYTES + 1)}, 413),
        ]
        with patch("tradercockpit.app_server.inspect_csv_data") as inspect:
            for path, headers, expected in cases:
                with self.subTest(headers=headers, path=path):
                    # Header refusals must complete before any file body is sent.
                    status, _ = self.request(path, b"", headers)
                    self.assertEqual(status, expected)
            inspect.assert_not_called()

    def test_catalog_rejects_cross_site_and_remote_clients(self):
        status, _ = self.request("/api/data-setup", headers={"Sec-Fetch-Site": "cross-site"})
        self.assertEqual(status, 403)
        status, _ = self.request("/api/data-setup", headers={"Host": f"external.example:{self.server.server_port}"})
        self.assertEqual(status, 403)
        with patch("tradercockpit.app_server._is_loopback_address", return_value=False):
            status, _ = self.request("/api/data-setup")
        self.assertEqual(status, 403)

    def test_selection_passes_identity_only_to_native_readback(self):
        selection = {"dataset_id": "sqx-data-7", "snapshot_sha256": "a" * 64}
        with patch("tradercockpit.app_server.select_native_data_setup", return_value={"backtest_ready": False}) as select:
            status, result = self.request("/api/data-setup/select", json.dumps(selection).encode(), {"Content-Type": "application/json"})
            self.assertEqual(status, 200)
            self.assertFalse(result["backtest_ready"])
            select.assert_called_once_with(None, selection)

    def test_mt5_discovery_does_not_connect_and_read_forwards_exact_identity(self):
        with patch("tradercockpit.app_server.read_mt5_terminal_catalog", return_value={"status": "no_running_terminal"}) as discover, patch("tradercockpit.app_server.read_mt5_metadata", return_value={"backtest_ready": False}) as read:
            status, result = self.request("/api/data-setup/mt5/terminals")
            self.assertEqual((status, result["status"]), (200, "no_running_terminal"))
            discover.assert_called_once_with(None)
            read.assert_not_called()
            identity = {"terminal_id": "mt5-7", "identity_sha256": "a" * 64, "symbol_filter": "EURUSD"}
            status, result = self.request("/api/data-setup/mt5/read", json.dumps(identity).encode(), {"Content-Type": "application/json"})
            self.assertEqual(status, 200)
            self.assertFalse(result["backtest_ready"])
            read.assert_called_once_with(None, identity, register_worker=None)

    def test_mt5_connection_rejects_cross_origin_and_arbitrary_query_before_worker(self):
        with patch("tradercockpit.app_server.read_mt5_metadata") as read:
            for path, headers, expected in [
                ("/api/data-setup/mt5/read", {"Origin": "https://external.example"}, 403),
                ("/api/data-setup/mt5/read?path=C:/terminal64.exe", {}, 400),
            ]:
                status, _ = self.request(path, b"", headers)
                self.assertEqual(status, expected)
            read.assert_not_called()

    def test_mt5_history_forwards_exact_request_and_export_is_attachment(self):
        payload = {"terminal_id": "mt5-7", "identity_sha256": "a" * 64,
                   "broker_sha256": "b" * 64, "symbol": "EURUSD", "timeframe": "H1",
                   "date_from": "2026-08-31", "date_to": "2026-09-04"}
        with patch("tradercockpit.app_server.read_mt5_history", return_value={"backtest_ready": False}) as read:
            status, result = self.request("/api/data-setup/mt5/history", json.dumps(payload).encode(), {"Content-Type": "application/json"})
            self.assertEqual(status, 200)
            self.assertFalse(result["backtest_ready"])
            read.assert_called_once_with(None, payload, store=None, register_worker=None)
        reference = {"history_ref": "tc-evidence:sha256:" + "c" * 64}
        csv = b"time,open,high,low,close\n2026-08-31T00:00:00Z,1,3,1,2\n"
        with patch("tradercockpit.app_server.read_mt5_history_csv", return_value=csv) as export:
            request = Request(self.origin + "/api/data-setup/mt5/history/export", data=json.dumps(reference).encode(),
                              headers={"Origin": self.origin, "Content-Type": "application/json"})
            with urlopen(request, timeout=5) as response:
                self.assertEqual(response.status, 200)
                self.assertEqual(response.read(), csv)
                self.assertEqual(response.headers["Content-Disposition"], 'attachment; filename="MT5-history.csv"')
                self.assertEqual(response.headers["Cache-Control"], "no-store")
            export.assert_called_once_with(None, reference)

    def test_history_and_export_guard_origin_query_and_json_before_access(self):
        with patch("tradercockpit.app_server.read_mt5_history") as read, patch("tradercockpit.app_server.read_mt5_history_csv") as export:
            for path in ("/api/data-setup/mt5/history", "/api/data-setup/mt5/history/export"):
                for suffix, headers, expected in [
                    ("", {"Origin": "https://external.example"}, 403),
                    ("?path=C:/private", {}, 400),
                    ("", {"Content-Type": "application/json"}, 400),
                ]:
                    status, _ = self.request(path + suffix, b"", headers)
                    self.assertEqual(status, expected)
            read.assert_not_called()
            export.assert_not_called()


if __name__ == "__main__":
    unittest.main()
