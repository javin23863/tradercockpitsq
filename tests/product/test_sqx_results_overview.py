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
from tradercockpit.sqx_results_overview import overview_html


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


class SqxOverviewTests(unittest.TestCase):
    def test_rejects_unknown_template(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            _archive(home)
            with self.assertRaises(SqxCustomProjectTopologyError) as raised:
                overview_html(
                    home,
                    project="Example",
                    databank="Results",
                    archive="Native.sqx",
                    template="InventedSharpe",
                )
        self.assertEqual(raised.exception.code, "overview_fields_invalid")

    def test_loads_missing_databank_row_then_returns_native_html(self) -> None:
        captured: dict[str, object] = {"items": 0, "loads": 0}

        class Handler(BaseHTTPRequestHandler):
            def log_message(self, format: str, *args: object) -> None:  # noqa: A003
                return

            def do_GET(self) -> None:  # noqa: N802
                parsed = urlparse(self.path)
                query = {key: values[0] for key, values in parse_qs(parsed.query).items()}
                if parsed.path == "/project/getDataItems":
                    captured["items"] = int(captured["items"]) + 1
                    captured["item_query"] = query
                    if captured["loads"]:
                        payload = {"success": "Data items listed.", "dataItems": ["Main: ES/H1"]}
                    else:
                        payload = {"strDoesntExist": True, "error": "Strategy 'Native' doesn't exist."}
                else:
                    captured["overview_query"] = query
                    captured["token"] = self.headers.get("browserToken")
                    payload = {"success": "Html code generated.", "overviewHtml": "<html>SQ Default</html>"}
                body = json.dumps(payload).encode("utf-8")
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)

            def do_POST(self) -> None:  # noqa: N802
                length = int(self.headers.get("Content-Length") or "0")
                body = self.rfile.read(length).decode("utf-8")
                captured["load_fields"] = {key: values[0] for key, values in parse_qs(body).items()}
                captured["loads"] = int(captured["loads"]) + 1
                payload = json.dumps({"success": "Loading strategies."}).encode("utf-8")
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
                record = overview_html(
                    home,
                    project="Example",
                    databank="Results",
                    archive="Native.sqx",
                    template="SQDefault",
                    sample="full",
                    direction="both",
                    sleeper=lambda _: None,
                )
        finally:
            server.shutdown()
            server.server_close()
            thread.join()
        self.assertEqual(record["schema"], "tc.sqx-overview.v1")
        self.assertEqual(record["producer"], "sqx_local_web")
        self.assertEqual(record["overviewHtml"], "<html>SQ Default</html>")
        self.assertEqual(captured["token"], "248158903")
        self.assertEqual(captured["overview_query"]["template"], "SQDefault")
        self.assertEqual(captured["overview_query"]["sampleType"], "127")
        self.assertIn("filePaths[]", captured["load_fields"])
        self.assertIn("Native.sqx", captured["load_fields"]["filePaths[]"])
        self.assertNotIn("248158903", json.dumps(record))
