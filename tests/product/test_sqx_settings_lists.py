from __future__ import annotations

from io import BytesIO
from pathlib import Path
from tempfile import TemporaryDirectory
from urllib.parse import parse_qs, urlparse
from xml.etree import ElementTree
from zipfile import ZipFile
import json
import unittest

from tradercockpit.sqx_native_web import SqxNativeWebError
from tradercockpit.sqx_settings_lists import (
    apply_template_chart_settings,
    list_build_type_files,
    fetch_symbol_data,
    list_commission_methods,
    list_installed_data_symbols,
    list_ranking_fitness_types,
    reload_build_template,
)


TASK_XML = """<Settings>
  <WhatToBuild>
    <StrategyType type="template" templateFile="highest_breakout.sqx" additionalCharts="2"/>
  </WhatToBuild>
  <Data><Setups><Setup engine="MetaTrader5">
    <Chart symbol="EURUSD" timeframe="H1"/>
    <Chart symbol="GBPUSD" timeframe="M15"/>
  </Setup></Setups></Data>
</Settings>"""


def _runtime(root: Path) -> Path:
    (root / "internal/web/SQUANT").mkdir(parents=True)
    (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
    (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
    settings = root / "user/settings/settings.xml"
    settings.parent.mkdir(parents=True)
    settings.write_text(
        "<Settings><WebServerPortUsed>8080</WebServerPortUsed><BrowserToken>tokentokentoken12</BrowserToken></Settings>",
        encoding="utf-8",
    )
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


class _FakeResponse:
    def __init__(self, payload: dict[str, object]) -> None:
        self._payload = json.dumps(payload).encode("utf-8")
        self.status = 200

    def read(self) -> bytes:
        return self._payload

    def __enter__(self) -> "_FakeResponse":
        return self

    def __exit__(self, *_args) -> None:
        return None


def _opener(routes: dict[str, dict[str, object]]):
    def open_request(request, timeout=None):
        parsed = urlparse(request.full_url)
        payload = routes.get(parsed.path)
        if payload is None:
            raise AssertionError(parsed.path)
        return _FakeResponse(payload)

    return open_request


class SqxSettingsListsTests(unittest.TestCase):
    def test_list_files_keeps_official_names_only(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            record = list_build_type_files(
                home,
                opener=_opener({
                    "/buildType/listFiles": {
                        "strategyTemplateFiles": ["highest_breakout.sqx", "trend.sqx"],
                        "strategyFiles": ["Strategy 1.sqx"],
                    }
                }),
            )
        self.assertEqual(record["schema"], "tc.sqx-build-type-files.v1")
        self.assertEqual(record["templates"], ["highest_breakout.sqx", "trend.sqx"])
        self.assertEqual(record["strategies"], ["Strategy 1.sqx"])

    def test_list_files_rejects_invented_payload(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            with self.assertRaises(SqxNativeWebError):
                list_build_type_files(
                    home,
                    opener=_opener({"/buildType/listFiles": {"strategyTemplateFiles": "nope"}}),
                )
            with self.assertRaises(SqxNativeWebError):
                list_build_type_files(
                    home,
                    opener=_opener({
                        "/buildType/listFiles": {
                            "strategyTemplateFiles": [{"name": "trend.sqx"}],
                            "strategyFiles": [],
                        }
                    }),
                )
            with self.assertRaises(SqxNativeWebError):
                list_build_type_files(
                    home,
                    opener=_opener({"/buildType/listFiles": {"strategyFiles": []}}),
                )

    def test_ranking_fitness_types_come_from_producer_list(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            record = list_ranking_fitness_types(
                home,
                opener=_opener({
                    "/fitnessMethodStrategyResult/list": {
                        "types": [
                            {"key": "NetProfit", "name": "Net Profit (Return)"},
                            {"key": "ReturnDDRatio", "name": "Return / Drawdown ratio"},
                            {"key": "RExpectancy", "name": "R Expectancy (Van Tharp)"},
                            {"key": "AnnualPctReturnDDRatio", "name": "Annual Return % / Max DD %"},
                            {"key": "Weighted", "name": "Weighted Fitness (multiple goals)"},
                        ]
                    }
                }),
            )
        self.assertEqual(record["schema"], "tc.sqx-ranking-fitness-types.v1")
        self.assertEqual(record["types"][1]["key"], "ReturnDDRatio")
        self.assertEqual(len(record["types"]), 5)

    def test_installed_data_symbols_come_from_constants_get_all(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            record = list_installed_data_symbols(
                home,
                opener=_opener({
                    "/constants/getAll": {
                        "constants": {
                            "data": [
                                {"symbol": "EURUSD", "timeframe": "M1"},
                                {"symbol": "DJ CFD", "timeframe": "H1"},
                                {"symbol": "EURUSD", "timeframe": "H1"},
                            ]
                        }
                    }
                }),
            )
            full = list_installed_data_symbols(
                home,
                opener=_opener({
                    "/constants/getAll": {
                        "constants": {
                            "data": [{"symbol": "EURUSD", "dataType": 3, "dateFrom": 1483228800000, "dateTo": 1704067200000, "rows": 10, "show": True}],
                            "dataTypes": [{"value": 3, "name": "Forex"}],
                            "sessions": [{"name": "No Session"}, {"name": "London"}],
                            "precisions": [{"value": 1, "name": "Selected timeframe"}, {"value": "2", "name": "1 minute"}],
                            "swapTypes": {"money": "money", "percent": "percent"},
                            "tripleSwapOptions": {"WEDNESDAY": "WEDNESDAY"},
                        }
                    }
                }),
            )
        self.assertEqual(record["schema"], "tc.sqx-installed-data.v1")
        self.assertEqual(record["symbols"], ["EURUSD", "DJ CFD"])
        self.assertEqual(record["sessions"], [])
        self.assertEqual(record["precisions"], [])
        self.assertEqual(record["dataTypes"], [])
        self.assertEqual(record["rows"][0]["symbol"], "EURUSD")
        self.assertEqual(full["sessions"], ["No Session", "London"])
        self.assertEqual(full["precisions"], [
            {"key": "1", "name": "Selected timeframe"},
            {"key": "2", "name": "1 minute"},
        ])
        self.assertEqual(full["dataTypes"], [{"key": "3", "name": "Forex"}])
        self.assertEqual(full["swapTypes"], ["money", "percent"])
        self.assertEqual(full["rows"][0]["dateFrom"], 1483228800000)

    def test_installed_data_rejects_path_escape_and_missing_constants(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            with self.assertRaises(SqxNativeWebError):
                list_installed_data_symbols(
                    home,
                    opener=_opener({"/constants/getAll": {"constants": {"data": [{"symbol": "../EURUSD"}]}}}),
                )
            with self.assertRaises(SqxNativeWebError):
                list_installed_data_symbols(
                    home,
                    opener=_opener({"/constants/getAll": {"data": [{"symbol": "EURUSD"}]}}),
                )
            empty = list_installed_data_symbols(
                home,
                opener=_opener({"/constants/getAll": {"constants": {"data": []}}}),
            )
            with self.assertRaises(SqxNativeWebError) as bad_swap:
                list_installed_data_symbols(
                    home,
                    opener=_opener({
                        "/constants/getAll": {
                            "constants": {
                                "data": [],
                                "swapTypes": {"money": "money", "bad": 3},
                            }
                        }
                    }),
                )
        self.assertEqual(empty["symbols"], [])
        self.assertEqual(empty["sessions"], [])
        self.assertEqual(empty["swapTypes"], [])
        self.assertEqual(bad_swap.exception.code, "installed_data_invalid")

    def test_installed_data_rows_come_from_main_get_data(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            record = list_installed_data_symbols(
                home,
                opener=_opener({
                    "/constants/getAll": {
                        "constants": {
                            "dataTypes": [{"value": 3, "name": "Forex"}],
                            "precisions": [{"value": 1, "name": "Selected timeframe"}],
                            "swapTypes": {"money": "money"},
                            "tripleSwapOptions": {"WEDNESDAY": "WEDNESDAY"},
                        }
                    },
                    "/main/getData": {
                        "success": "Data loaded.",
                        "data": {
                            "action": "all",
                            "data": [
                                {"symbol": "EURUSD", "dataType": 3, "dateFrom": 1483228800000, "dateTo": 1704067200000, "rows": 10, "show": True},
                            ],
                            "symbols": [],
                        },
                        "sessions": [{"name": "No Session"}, {"name": "London"}],
                    },
                }),
            )
        self.assertEqual(record["symbols"], ["EURUSD"])
        self.assertEqual(record["sessions"], ["No Session", "London"])
        self.assertEqual(record["rows"][0]["show"], True)
        self.assertEqual(record["swapTypes"], ["money"])

    def test_commission_methods_come_from_producer_list(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            record = list_commission_methods(
                home,
                opener=_opener({
                    "/constants/listCommissionMethods": {
                        "methods": [
                            {"class": "None", "display": "None"},
                            {"class": "SizeCommission", "display": "Size commission"},
                        ]
                    }
                }),
            )
            with self.assertRaises(SqxNativeWebError):
                list_commission_methods(
                    home,
                    opener=_opener({"/constants/listCommissionMethods": {"methods": [{"class": "../None"}]}}),
                )
        self.assertEqual(record["schema"], "tc.sqx-commission-methods.v1")
        self.assertEqual(record["methods"][0]["key"], "None")
        self.assertEqual(record["methods"][1]["name"], "Size commission")

    def test_symbol_data_removes_producer_offset(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            record = fetch_symbol_data(
                home,
                "2017.01.03",
                "2023.01.01",
                "EURUSD",
                "No Session",
                opener=_opener({
                    "/data/getSymbolData": {
                        "success": "Symbol data loaded.",
                        "data": [[1, 12.0], [2, 10.0], [3, 15.0]],
                    }
                }),
            )
            dated = fetch_symbol_data(
                home,
                "2017.01.03",
                "2023.01.01",
                "EURUSD",
                "No Session",
                opener=_opener({
                    "/data/getSymbolData": {
                        "success": "Symbol data loaded.",
                        "data": [["2017.01.03", 12.0, 1], ["2017.01.04", 10.0, 1], ["2017.01.05", 15.0, 1]],
                    }
                }),
            )
            with self.assertRaises(SqxNativeWebError) as missing:
                fetch_symbol_data(
                    home,
                    "2017.01.03",
                    "2023.01.01",
                    "EURUSD",
                    "No Session",
                    opener=_opener({"/data/getSymbolData": {"success": False, "error": r"C:\SQX\user\data\EURUSD missing"}}),
                )
            with self.assertRaises(SqxNativeWebError) as session:
                fetch_symbol_data(
                    home,
                    "2017.01.03",
                    "2023.01.01",
                    "EURUSD",
                    r"No\Session",
                    opener=_opener({"/data/getSymbolData": {"success": True, "data": [[1, 12.0]]}}),
                )
            with self.assertRaises(SqxNativeWebError) as slash:
                fetch_symbol_data(
                    home,
                    "2017.01.03",
                    "2023.01.01",
                    "EURUSD",
                    "No/Session",
                    opener=_opener({"/data/getSymbolData": {"success": True, "data": [[1, 12.0]]}}),
                )
        self.assertEqual(record["schema"], "tc.sqx-symbol-data.v1")
        self.assertEqual(record["points"], [[1.0, 2.0], [2.0, 0.0], [3.0, 5.0]])
        self.assertEqual(dated["points"], [[1483401600000.0, 2.0], [1483488000000.0, 0.0], [1483574400000.0, 5.0]])
        self.assertEqual(missing.exception.code, "symbol_data_unavailable")
        self.assertEqual(missing.exception.detail, "StrategyQuant X data/getSymbolData failed.")
        self.assertEqual(session.exception.code, "symbol_data_invalid")
        self.assertEqual(slash.exception.code, "symbol_data_invalid")

    def test_reload_applies_additional_chart_from_template(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            _write_project(home)
            calls: list[str] = []

            def opener(request, timeout=None):
                parsed = urlparse(request.full_url)
                calls.append(f"{request.get_method()} {parsed.path}")
                if parsed.path == "/buildType/listFiles":
                    return _FakeResponse({"strategyTemplateFiles": ["highest_breakout.sqx"], "strategyFiles": []})
                if parsed.path == "/buildType/getTemplateConfig":
                    body = parse_qs(request.data.decode("utf-8"))
                    self.assertEqual(body["fileName"], ["highest_breakout.sqx"])
                    self.assertEqual(body["reload"], ["true"])
                    return _FakeResponse({
                        "chartSettings": [
                            {"symbol": "EURUSD", "timeframe": "H1"},
                            {"symbol": "USDJPY", "timeframe": "H4"},
                        ]
                    })
                raise AssertionError(parsed.path)

            record = reload_build_template(home, "Example", 1, "highest_breakout.sqx", opener=opener)
            self.assertEqual(record["updated_charts"], 2)
            self.assertTrue(record["applied"])
            with ZipFile(home / "user/projects/Example/project.cfx") as archive:
                root = ElementTree.fromstring(archive.read("Build-Task1.xml"))
            charts = [child for child in root.find("Data").find("Setups").find("Setup") if child.tag == "Chart"]
            self.assertEqual(charts[0].attrib["symbol"], "EURUSD")
            self.assertEqual(charts[1].attrib["symbol"], "USDJPY")
            self.assertEqual(charts[1].attrib["timeframe"], "H4")
            self.assertIn("GET /buildType/listFiles", calls)
            self.assertIn("POST /buildType/getTemplateConfig", calls)

    def test_reload_does_not_rewrite_when_apply_is_false(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            _write_project(home)
            before = (home / "user/projects/Example/project.cfx").read_bytes()

            def opener(request, timeout=None):
                parsed = urlparse(request.full_url)
                if parsed.path == "/buildType/listFiles":
                    return _FakeResponse({"strategyTemplateFiles": ["highest_breakout.sqx"], "strategyFiles": []})
                if parsed.path == "/buildType/getTemplateConfig":
                    return _FakeResponse({
                        "chartSettings": [
                            {"symbol": "EURUSD", "timeframe": "H1"},
                            {"symbol": "USDJPY", "timeframe": "H4"},
                        ]
                    })
                raise AssertionError(parsed.path)

            record = reload_build_template(
                home, "Example", 1, "highest_breakout.sqx", apply=False, opener=opener
            )
            self.assertEqual(record["updated_charts"], 2)
            self.assertFalse(record["applied"])
            self.assertEqual((home / "user/projects/Example/project.cfx").read_bytes(), before)

    def test_reload_rejects_unlisted_template_name(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            _write_project(home)

            def opener(request, timeout=None):
                parsed = urlparse(request.full_url)
                if parsed.path == "/buildType/listFiles":
                    return _FakeResponse({"strategyTemplateFiles": ["highest_breakout.sqx"], "strategyFiles": []})
                raise AssertionError(parsed.path)

            with self.assertRaises(SqxNativeWebError):
                reload_build_template(home, "Example", 1, "C:/not-listed.sqx", opener=opener)

    def test_apply_template_skips_main_chart_and_missing_attrs(self) -> None:
        root = ElementTree.fromstring(TASK_XML)
        updated = apply_template_chart_settings(
            root,
            [{"symbol": "X", "timeframe": "M1"}, {"symbol": "Y", "timeframe": "M5"}],
        )
        self.assertEqual(updated, 2)
        charts = [child for child in root.find("Data").find("Setups").find("Setup") if child.tag == "Chart"]
        self.assertEqual(charts[0].attrib["symbol"], "EURUSD")
        self.assertEqual(charts[1].attrib["symbol"], "Y")
