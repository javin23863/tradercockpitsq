from __future__ import annotations

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Thread
from urllib.parse import parse_qs
from xml.etree import ElementTree
from zipfile import ZipFile
import unittest

from tradercockpit.sqx_calibrate import (
    apply_calibration_results,
    calibrate_indicators,
    calibrate_request_fields,
)
from tradercockpit.sqx_custom_project import SqxCustomProjectTopologyError
from tradercockpit.sqx_native_web import SqxNativeWebError


TASK_XML = """<Settings>
  <Data><Setups><Setup engine="MetaTrader5">
    <Chart symbol="ES" timeframe="H1"/>
    <Chart symbol="Same as main chart" timeframe="M15"/>
  </Setup></Setups></Data>
  <Blocks>
    <Calibration useMaxSteps="true" maxSteps="50" calibrateBeforeStart="true"/>
    <BuildingBlocks>
      <Block key="Indicators.ADX" category="indicators" use="true"/>
      <Block key="Stop/Limit Price Ranges.ATR" category="stopLimitBlocks" use="true"/>
      <Block key="Stop/Limit Price Levels.Ask" category="stopLimitBlocks" use="true"/>
      <Block key="ADXCrossUp" category="signals" use="true">
        <Generated>
          <Param key="#Level#" name="Level" minValue="10" maxValue="20" step="1"/>
          <Param key="#Period#" name="Period" minValue="-1000003" maxValue="-1000004" step="1"/>
        </Generated>
        <Predefined>
          <Params name="Default set 1">
            <Param key="#Level#" generation="random" minValue="10" maxValue="20" step="1"/>
          </Params>
        </Predefined>
      </Block>
      <Block key="MACDCrossUp" category="signals" use="true">
        <Generated>
          <Param key="#Level#" name="Level" minValue="1" maxValue="2" step="0.1"/>
        </Generated>
      </Block>
    </BuildingBlocks>
  </Blocks>
</Settings>"""


def _runtime(root: Path) -> Path:
    (root / "internal/web/SQUANT").mkdir(parents=True)
    (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
    (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
    return root


def _write_project(home: Path, xml: str = TASK_XML) -> None:
    path = home / "user/projects/Example/project.cfx"
    path.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(path, "w") as archive:
        archive.writestr(
            "config.xml",
            '<Settings><Project><Task name="Build" type="Build" active="true" taskXMLFile="Build-Task1.xml"/></Project></Settings>',
        )
        archive.writestr("Build-Task1.xml", xml)


class SqxCalibrateTests(unittest.TestCase):
    def test_request_fields_join_setup_charts_and_max_steps(self) -> None:
        root = ElementTree.fromstring(TASK_XML)
        fields = calibrate_request_fields(root, "Example", "Build")
        self.assertEqual(
            fields,
            {
                "projectName": "Example",
                "taskName": "Build",
                "symbols": "ES,ES",
                "timeframes": "H1,M15",
                "maxSteps": "50",
                "engine": "MetaTrader5",
            },
        )

    def test_apply_writes_existing_level_and_indicator_ranges_only(self) -> None:
        root = ElementTree.fromstring(TASK_XML)
        blocks, params = apply_calibration_results(
            root,
            [
                {"key": "ADX", "ranges": [{"minValue": "14", "maxValue": "33", "step": "0.38"}]},
                {"key": "ATR", "ranges": [{"minValue": "0.5", "maxValue": "4", "step": "0.1"}]},
            ],
        )
        adx = next(item for item in root.iter() if item.attrib.get("key") == "Indicators.ADX")
        atr = next(item for item in root.iter() if item.attrib.get("key") == "Stop/Limit Price Ranges.ATR")
        ask = next(item for item in root.iter() if item.attrib.get("key") == "Stop/Limit Price Levels.Ask")
        signal = next(item for item in root.iter() if item.attrib.get("key") == "ADXCrossUp")
        macd_block = next(item for item in root.iter() if item.attrib.get("key") == "MACDCrossUp")
        level = next(item for item in signal.iter() if item.attrib.get("key") == "#Level#" and item.attrib.get("name") == "Level")
        period = next(item for item in signal.iter() if item.attrib.get("key") == "#Period#")
        macd = next(item for item in macd_block.iter() if item.attrib.get("key") == "#Level#")
        self.assertEqual(blocks, 2)
        self.assertEqual(params, 2)
        self.assertEqual(adx.attrib["indicatorMin"], "14")
        self.assertEqual(atr.attrib["indicatorMax"], "4")
        self.assertNotIn("indicatorMin", ask.attrib)
        self.assertEqual(level.attrib["minValue"], "14")
        self.assertEqual(period.attrib["minValue"], "-1000003")
        self.assertEqual(macd.attrib["minValue"], "1")

    def test_calibrate_posts_servlet_fields_and_applies_results(self) -> None:
        captured: dict[str, object] = {}

        class Handler(BaseHTTPRequestHandler):
            def log_message(self, format: str, *args: object) -> None:  # noqa: A003
                return

            def do_POST(self) -> None:  # noqa: N802
                captured["path"] = self.path
                captured["token"] = self.headers.get("browserToken")
                length = int(self.headers.get("Content-Length") or "0")
                body = self.rfile.read(length).decode("utf-8")
                captured["fields"] = {key: values[0] for key, values in parse_qs(body).items()}
                payload = json.dumps({
                    "calibrationResults": [
                        {"key": "ADX", "ranges": [{"minValue": 14, "maxValue": 33, "step": 0.38}]},
                    ]
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
                _write_project(home)
                (home / "user/settings").mkdir(parents=True)
                (home / "user/settings/settings.xml").write_text(
                    f"<Settings><WebServerPortUsed>{server.server_port}</WebServerPortUsed><BrowserToken>248158903</BrowserToken></Settings>",
                    encoding="utf-8",
                )
                record = calibrate_indicators(home, "Example", 1, apply=True)
                with ZipFile(home / "user/projects/Example/project.cfx") as archive:
                    written = ElementTree.fromstring(archive.read("Build-Task1.xml"))
        finally:
            server.shutdown()
            server.server_close()
            thread.join()

        adx = next(item for item in written.iter() if item.attrib.get("key") == "Indicators.ADX")
        self.assertEqual(record["schema"], "tc.sqx-calibrate.v1")
        self.assertTrue(record["applied"])
        self.assertEqual(record["updated_blocks"], 1)
        self.assertEqual(record["updated_params"], 2)
        self.assertEqual(captured["path"], "/indyTester/calibrate")
        self.assertEqual(captured["token"], "248158903")
        self.assertEqual(captured["fields"]["symbols"], "ES,ES")
        self.assertEqual(captured["fields"]["maxSteps"], "50")
        self.assertEqual(adx.attrib["indicatorMin"], "14")
        self.assertNotIn("248158903", json.dumps(record))

    def test_missing_data_setup_fails_closed(self) -> None:
        root = ElementTree.fromstring("<Settings><Blocks/></Settings>")
        with self.assertRaises(SqxCustomProjectTopologyError) as raised:
            calibrate_request_fields(root, "Example", "Build")
        self.assertEqual(raised.exception.code, "calibrate_data_missing")

    def test_unavailable_sqx_web_fails_closed(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            _write_project(home)
            (home / "user/settings").mkdir(parents=True)
            (home / "user/settings/settings.xml").write_text(
                "<Settings><WebServerPortUsed>1</WebServerPortUsed><BrowserToken>9</BrowserToken></Settings>",
                encoding="utf-8",
            )
            with self.assertRaises(SqxNativeWebError) as raised:
                calibrate_indicators(home, "Example", 1, apply=False)
        self.assertEqual(raised.exception.code, "sqx_web_unavailable")
