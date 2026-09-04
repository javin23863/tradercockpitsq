from __future__ import annotations

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Thread
from urllib.parse import parse_qs, urlparse
from zipfile import ZipFile
import unittest

from tradercockpit.sqx_custom_project import SqxCustomProjectTopologyError
from tradercockpit.sqx_results_chart import results_chart


def _runtime(root: Path) -> Path:
    (root / "internal/web/SQUANT").mkdir(parents=True)
    (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
    (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
    return root


def _archive(home: Path) -> None:
    bank = home / "user/projects/Example/databanks/Results"
    bank.mkdir(parents=True)
    with ZipFile(bank / "Native.sqx", "w") as handle:
        handle.writestr("strategy_Portfolio.xml", "<StrategyFile><Strategy name='x'/></StrategyFile>")
        handle.writestr("settings.xml", "<Settings/>")
        handle.writestr("version.txt", "1")


class SqxResultsChartTests(unittest.TestCase):
    def test_rejects_invalid_stock(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            _archive(home)
            with self.assertRaises(SqxCustomProjectTopologyError) as raised:
                results_chart(
                    home,
                    project="Example",
                    databank="Results",
                    archive="Native.sqx",
                    stock="x" * 81,
                )
        self.assertEqual(raised.exception.code, "chart_fields_invalid")

    def test_posts_load_chart_data_and_returns_native_indicators(self) -> None:
        captured: dict[str, object] = {}

        class Handler(BaseHTTPRequestHandler):
            def log_message(self, format: str, *args: object) -> None:  # noqa: A003
                return

            def do_GET(self) -> None:  # noqa: N802
                payload = {"success": "Data items listed.", "dataItems": ["Main: ES/H1"]}
                body = json.dumps(payload).encode("utf-8")
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)

            def do_POST(self) -> None:  # noqa: N802
                captured["path"] = urlparse(self.path).path
                captured["token"] = self.headers.get("browserToken")
                length = int(self.headers.get("Content-Length") or "0")
                captured["fields"] = {key: values[0] for key, values in parse_qs(self.rfile.read(length).decode("utf-8")).items()}
                payload = {
                    "success": True,
                    "data": {
                        "chart": {
                            "stocks": ["ES"],
                            "currentStock": "ES",
                            "indicators": [
                                {"id": "rsi", "title": "RSI(14)", "displayOn": "subchart"},
                                {"id": "atr", "title": "ATR(H1,14)", "displayOn": "chart"},
                                {"id": "ema", "title": "EMA(20)", "displayOn": "chart"},
                            ],
                            "charts": [
                                {
                                    "xVals": [1_600_000_000_000],
                                    "yVals": [{"open": 1.0, "high": 2.0, "low": 0.5, "close": 1.5}],
                                }
                            ],
                        }
                    },
                }
                body = json.dumps(payload).encode("utf-8")
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)

        server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        thread = Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            with TemporaryDirectory() as tmp:
                home = _runtime(Path(tmp))
                _archive(home)
                (home / "user/settings").mkdir(parents=True)
                (home / "user/settings/settings.xml").write_text(
                    f"<Settings><WebServerPortUsed>{server.server_port}</WebServerPortUsed><BrowserToken>248158903</BrowserToken></Settings>",
                    encoding="utf-8",
                )
                record = results_chart(
                    home,
                    project="Example",
                    databank="Results",
                    archive="Native.sqx",
                    sleeper=lambda _: None,
                )
        finally:
            server.shutdown()
            server.server_close()
            thread.join()
        self.assertEqual(record["schema"], "tc.sqx-results-chart.v1")
        self.assertEqual(record["producer"], "sqx_local_web")
        self.assertTrue(record["stored"])
        self.assertEqual(captured["path"], "/resultsCharts/loadChartData")
        self.assertEqual(captured["token"], "248158903")
        self.assertEqual(captured["fields"]["strategyName"], "Native")
        self.assertEqual(captured["fields"]["preview"], "true")
        self.assertEqual([item["title"] for item in record["indicators"]], ["RSI(14)", "ATR(H1,14)", "EMA(20)"])
        self.assertEqual([item["show"] for item in record["indicators"]], [True, False, True])
        self.assertEqual(record["bars"]["basis"], "sqx_results_charts")
        self.assertEqual(record["bars"]["bars"][0]["close"], 1.5)
        self.assertNotIn("248158903", json.dumps(record))

    def test_cannot_load_charts_stays_empty_without_invented_series(self) -> None:
        class Handler(BaseHTTPRequestHandler):
            def log_message(self, format: str, *args: object) -> None:  # noqa: A003
                return

            def do_GET(self) -> None:  # noqa: N802
                payload = {"success": "Data items listed.", "dataItems": ["Main: ES/H1"]}
                body = json.dumps(payload).encode("utf-8")
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)

            def do_POST(self) -> None:  # noqa: N802
                payload = json.dumps({"error": "Cannot load charts.<br>"}).encode("utf-8")
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(payload)))
                self.end_headers()
                self.wfile.write(payload)

        server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        thread = Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            with TemporaryDirectory() as tmp:
                home = _runtime(Path(tmp))
                _archive(home)
                (home / "user/settings").mkdir(parents=True)
                (home / "user/settings/settings.xml").write_text(
                    f"<Settings><WebServerPortUsed>{server.server_port}</WebServerPortUsed><BrowserToken>248158903</BrowserToken></Settings>",
                    encoding="utf-8",
                )
                record = results_chart(
                    home,
                    project="Example",
                    databank="Results",
                    archive="Native.sqx",
                    sleeper=lambda _: None,
                )
        finally:
            server.shutdown()
            server.server_close()
            thread.join()
        self.assertEqual(record["producer"], "sqx_local_web")
        self.assertFalse(record["stored"])
        self.assertEqual(record["indicators"], [])
        self.assertEqual(record["bars"]["bars"], [])
        self.assertEqual(record["detail"], "Cannot load charts.")
        self.assertNotIn("<br>", record["detail"])
