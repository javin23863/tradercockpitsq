from __future__ import annotations

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Thread
from urllib.parse import parse_qs
from zipfile import ZipFile
import unittest

from tradercockpit.sqx_native_web import SqxNativeWebError
from tradercockpit.sqx_sourcecode import print_sourcecode, save_ea, save_mt_paths, sourcecode_catalog_record, sourcecode_data_path


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


class SqxSourcecodeTests(unittest.TestCase):
    def test_strategy_xml_print_falls_back_to_archive_when_sqx_web_is_down(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            _archive(home)
            (home / "user/settings").mkdir(parents=True)
            (home / "user/settings/settings.xml").write_text(
                "<Settings><WebServerPortUsed>1</WebServerPortUsed><BrowserToken>9</BrowserToken></Settings>",
                encoding="utf-8",
            )
            printed = print_sourcecode(
                home,
                {
                    "project": "Example",
                    "databank": "Results",
                    "archive": "Native.sqx",
                    "type": "Strategy XML",
                },
            )
            with self.assertRaises(SqxNativeWebError) as raised:
                print_sourcecode(
                    home,
                    {
                        "project": "Example",
                        "databank": "Results",
                        "archive": "Native.sqx",
                        "format": "el",
                    },
                )
            catalog = sourcecode_catalog_record(home)
        self.assertEqual(printed["schema"], "tc.sqx-sourcecode.v1")
        self.assertEqual(printed["producer"], "archive")
        self.assertIn("<StrategyFile>", printed["code"])
        self.assertEqual(raised.exception.code, "sqx_web_unavailable")
        self.assertEqual(catalog["producer"], "unavailable")
        self.assertEqual(catalog["generators"][0]["name"], "Strategy XML")
        self.assertFalse(catalog["export_ea"]["available"])

    def test_print_posts_strategy_xml_to_running_sqx_web(self) -> None:
        captured: dict[str, object] = {}

        class Handler(BaseHTTPRequestHandler):
            def log_message(self, format: str, *args: object) -> None:  # noqa: A003
                return

            def do_POST(self) -> None:  # noqa: N802
                captured["token"] = self.headers.get("browserToken")
                length = int(self.headers.get("Content-Length") or "0")
                body = self.rfile.read(length).decode("utf-8")
                captured["fields"] = {key: values[0] for key, values in parse_qs(body).items()}
                payload = json.dumps({"success": "ok", "code": "Inputs: Length(10);", "warning": None}).encode("utf-8")
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(payload)))
                self.end_headers()
                self.wfile.write(payload)

            def do_GET(self) -> None:  # noqa: N802
                payload = json.dumps({
                    "success": "ok",
                    "results": {
                        "sourceCode": {
                            "engineTypes": [{"name": "EasyLanguage for Tradestation / MultiCharts (*.el)", "key": "EasyLanguage"}],
                            "mmTypes": [{"name": "From strategy", "value": "fromStrategy"}],
                        }
                    },
                }).encode("utf-8")
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
                catalog = sourcecode_catalog_record(home)
                printed = print_sourcecode(
                    home,
                    {
                        "project": "Example",
                        "databank": "Results",
                        "archive": "Native.sqx",
                        "format": "el",
                        "mmType": "fromStrategy",
                        "parametrizeType": 0,
                    },
                )
        finally:
            server.shutdown()
            server.server_close()
            thread.join()
        self.assertEqual(catalog["producer"], "sqx_local_web")
        self.assertTrue(catalog["export_ea"]["available"])
        self.assertEqual(printed["producer"], "sqx_local_web")
        self.assertEqual(printed["code"], "Inputs: Length(10);")
        self.assertEqual(captured["token"], "248158903")
        self.assertIn("strategyXML", captured["fields"])
        self.assertEqual(captured["fields"]["type"], "EasyLanguage for Tradestation / MultiCharts (*.el)")
        self.assertNotIn("248158903", json.dumps(printed))
        self.assertNotIn("248158903", json.dumps(catalog))

    def test_save_ea_posts_native_flags_and_keeps_token_off_the_read_model(self) -> None:
        captured: dict[str, object] = {}

        class Handler(BaseHTTPRequestHandler):
            def log_message(self, format: str, *args: object) -> None:  # noqa: A003
                return

            def do_GET(self) -> None:  # noqa: N802
                payload = json.dumps({"success": "Data items listed.", "dataItems": ["Main: ES/H1"]}).encode("utf-8")
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(payload)))
                self.end_headers()
                self.wfile.write(payload)

            def do_POST(self) -> None:  # noqa: N802
                length = int(self.headers.get("Content-Length") or "0")
                body = self.rfile.read(length).decode("utf-8")
                captured[self.path] = {
                    "token": self.headers.get("browserToken"),
                    "fields": {key: values[0] for key, values in parse_qs(body).items()},
                }
                if self.path == "/sourcecode/getDataPath":
                    payload = json.dumps({"success": "Paths saved", "dataPath": r"C:\MT4\MQL4\Experts"}).encode("utf-8")
                else:
                    payload = json.dumps({"success": "EA saved"}).encode("utf-8")
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
                saved = save_ea(
                    home,
                    {
                        "project": "Example",
                        "databank": "Results",
                        "archive": "Native.sqx",
                        "isMT4": True,
                        "type": "Expert Advisor for MetaTrader4 (*.MQ4)",
                    },
                )
                data = sourcecode_data_path(home, {"installPath": r"C:\MT4", "isMT4": True})
                paths = save_mt_paths(
                    home,
                    {
                        "mt4InstallPath": r"C:\MT4",
                        "mt5InstallPath": "",
                        "mt4DataPath": r"C:\MT4\MQL4\Experts",
                        "mt5DataPath": "",
                    },
                )
        finally:
            server.shutdown()
            server.server_close()
            thread.join()
        self.assertTrue(saved["success"])
        self.assertEqual(saved["schema"], "tc.sqx-sourcecode-ea.v1")
        self.assertEqual(captured["/sourcecode/saveEA"]["fields"]["isMT4"], "true")
        self.assertNotIn("type", captured["/sourcecode/saveEA"]["fields"])
        self.assertEqual(data["dataPath"], r"C:\MT4\MQL4\Experts")
        self.assertTrue(paths["success"])
        self.assertNotIn("248158903", json.dumps(saved))

