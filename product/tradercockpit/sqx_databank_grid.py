"""Native SQX databank grid read model.

Column identity comes from the retained SQX 144.2953 view
``Default with Note - Main data.vw`` (``originalName="Default - Main data"``),
minus the Note column. Cell values come from producer-recorded ``ResultsGroup``
bytes inside each databank ``.sqx``:

- ``SQStats`` version-2 Base64 maps (``StatsKeyCache`` indexes)
- ``Fitnesses`` attributes (``getFitness(sampleType)``)
- Main-result ``ValuesMap`` ``Symbol`` / ``Timeframe``
- ``SpecialValuesMap`` ``FiltersResultFailedReason`` and ``MEC_IS_Main``

This module does not recompute Sharpe, Stability, or other snippet formulas
over ``orders.bin``. Missing producer fields stay null and render as dashes.
"""

from __future__ import annotations

from base64 import b64decode
from io import BytesIO
import json
import math
import re
import struct
from xml.etree import ElementTree
from zipfile import BadZipFile, ZipFile

from .research_verdicts import SAMPLE_IN_SAMPLE, SAMPLE_OUT_OF_SAMPLE, SAMPLE_FULL, round2


SQX_DATABANK_VIEW_NAME = "Default - Main data"
SQX_DATABANK_VIEW_ORIGINAL_NAME = "Default - Main data"
PL_MONEY = 10
DIRECTION_BOTH = 0
FILTERS_PASSED = "Passed"

# stats_[direction=0,pl=10,sample=10] after SQX XML name mangling.
_STATS_TAG_RE = re.compile(
    r"^stats_LQ1_direction_DD_(-?\d+)_L1_pl_DD_(-?\d+)_L1_sample_DD_(-?\d+)_L1__RQ1_$"
)
_SPARKLINE_RE = re.compile(r"\{\{sparklinesWidget data='(\{.*\})'\}\}", re.DOTALL)

# StatsKeyCache.defaultKeysMap: (type, index, name). Type 1=int, 2=long, 3=double.
_STATS_KEYS: tuple[tuple[int, int, str], ...] = (
    (1, 0, "NumberOfLosses"),
    (1, 1, "MaxConsecLosses"),
    (1, 2, "TotalTradingMonths"),
    (1, 3, "Stagnation"),
    (1, 4, "MaxConsecWins"),
    (1, 5, "NumberOfProfits"),
    (1, 6, "TotalTradingYears"),
    (1, 7, "NumberOfCanceled"),
    (1, 8, "TotalTradingDays"),
    (1, 9, "DegreesOfFreedom"),
    (1, 10, "NumberOfTrades"),
    (1, 11, "Complexity"),
    (1, 12, "LongestTrade"),
    (1, 13, "ProfitableMonths"),
    (1, 14, "TotalDataDays"),
    (1, 15, "TotalDataMonths"),
    (1, 16, "TotalDataYears"),
    (3, 0, "MaxLoss"),
    (3, 1, "SharpeRatio"),
    (3, 2, "Commission"),
    (3, 3, "AvgConsecLosses"),
    (3, 4, "ZProbability"),
    (3, 5, "ZScore"),
    (3, 6, "RExpectancyscore"),
    (3, 7, "AHPR"),
    (3, 8, "CalmarRatio"),
    (3, 9, "Drawdown"),
    (3, 10, "NetProfit"),
    (3, 11, "Stability"),
    (3, 12, "AvgTradesPerMonth"),
    (3, 13, "DrawdownPips"),
    (3, 14, "AvgProfitPerYear"),
    (3, 15, "GrossLoss"),
    (3, 16, "AvgProfitPerMonth"),
    (3, 17, "AvgTrade"),
    (3, 18, "AvgAbsTrade"),
    (3, 19, "AvgProfitPerDay"),
    (3, 20, "AvgConsecWins"),
    (3, 21, "DrawdownPct"),
    (3, 22, "WinLossRatio"),
    (3, 23, "AnnualPctReturnDDRatio"),
    (3, 24, "AnnualPctReturn"),
    (3, 25, "ReturnDDRatio"),
    (3, 26, "PayoutRatio"),
    (3, 27, "ProfitFactor"),
    (3, 28, "AvgBarsWin"),
    (3, 29, "SQN"),
    (3, 30, "AvgBarsInTrade"),
    (3, 31, "AvgWin"),
    (3, 32, "AvgLoss"),
    (3, 33, "CAGR"),
    (3, 34, "RExpectancy"),
    (3, 35, "RExpectancyScore"),
    (3, 36, "Symmetry"),
    (3, 37, "SQNScore"),
    (3, 38, "AvgTradesPerDay"),
    (3, 39, "AvgTradesPerYear"),
    (3, 40, "Expectancy"),
    (3, 41, "GrossProfit"),
    (3, 42, "AvgBarsLoss"),
    (3, 43, "WinningPct"),
    (3, 44, "StagnationPct"),
    (3, 45, "MaxProfit"),
    (3, 46, "Exposure"),
    (3, 47, "InitialDeposit"),
    (3, 48, "Fitness"),
    (3, 49, "StandardDev"),
    (3, 50, "SHPR"),
    (3, 51, "AmbiguousTrades"),
    (3, 52, "AmbiguousTradesPct"),
    (3, 53, "BacktestDuration"),
    (3, 54, "FiltersResult"),
    (3, 55, "MiniEquityChart"),
    (3, 56, "RSquared"),
    (3, 57, "StabilitySQ3"),
    (3, 58, "TradesSymmetry"),
    (3, 59, "WorstYearProfit"),
    (3, 60, "ActualDD"),
    (3, 61, "ActualDrawdownPct"),
    (3, 62, "NSymmetry"),
    (3, 63, "TimeFrame"),
    (3, 64, "Symbol"),
    (3, 65, "ResultsName"),
    (3, 66, "Note"),
    (3, 67, "WFMaxDDbyRun"),
    (3, 68, "WFMaxPctDDbyRun"),
    (3, 69, "WFMaxProfitByRun"),
    (3, 70, "WFMaxProfitByRunInPct"),
    (3, 71, "WFMaxStagnationInPct"),
    (3, 72, "WFMinTradesInRun"),
    (3, 73, "WFPctOfProfitableRuns"),
    (3, 74, "ExitIndicators"),
    (3, 75, "EntryIndicators"),
    (3, 76, "MagicNumber"),
    (3, 77, "WFScore"),
    (3, 78, "PriceIndicators"),
    (3, 79, "BiggestMAE"),
    (3, 80, "NetProfitInPct"),
    (3, 81, "NetProfitInPips"),
    (3, 82, "AvgPctProfitPerYear"),
    (3, 83, "CommSwapInMoney"),
    (3, 84, "SlippageInMoney"),
    (3, 85, "OpenDrawdown"),
    (3, 86, "ProfitableMonthsPct"),
    (3, 87, "AvgTrStddevRatio"),
    (3, 88, "AvgParametersStability"),
    (3, 89, "WorstParametersStability"),
    (3, 90, "Efficiency"),
    (3, 91, "KellyFormula"),
    (3, 92, "MaxIntradayDrawdown"),
    (3, 93, "TSIndex"),
    (3, 94, "OpenDrawdownPct"),
    (2, 0, "StagnationTo"),
    (2, 1, "StagnationFrom"),
    (2, 2, "DateGenerated"),
    (2, 3, "DateLastModified"),
)
_INT_BY_INDEX = {index: name for kind, index, name in _STATS_KEYS if kind == 1}
_LONG_BY_INDEX = {index: name for kind, index, name in _STATS_KEYS if kind == 2}
_DOUBLE_BY_INDEX = {index: name for kind, index, name in _STATS_KEYS if kind == 3}

_FITNESS_ATTR = {
    10: "IS",
    11: "IST",
    20: "OOS",
    21: "OOS1",
    22: "OOS2",
    23: "OOS3",
    24: "OOS4",
    25: "OOS5",
    26: "OOS6",
    27: "OOS7",
    28: "OOS8",
    29: "OOS9",
    30: "OOS10",
    40: "ISV",
    41: "ISV1",
    42: "ISV2",
    43: "ISV3",
    44: "ISV4",
    45: "ISV5",
    46: "ISV6",
    47: "ISV7",
    48: "ISV8",
    49: "ISV9",
    50: "ISV10",
    127: "FS",
}

# Built-in columns plus the IS half of Default - Main data (sampleType=10).
# Format names match DatabankColumn constructors in SQ.Columns.Databanks.
DEFAULT_MAIN_DATA_COLUMNS: tuple[dict[str, object], ...] = (
    {"class": "ResultsName", "name": "Strategy Name", "sample_type": None, "format": "text"},
    {"class": "FiltersResult", "name": "Filters result", "sample_type": None, "format": "filters"},
    {"class": "Fitness", "name": "Fitness", "sample_type": SAMPLE_IN_SAMPLE, "format": "decimal2"},
    {"class": "Symbol", "name": "Symbol", "sample_type": SAMPLE_IN_SAMPLE, "format": "text"},
    {"class": "TimeFrame", "name": "TimeFrame", "sample_type": SAMPLE_IN_SAMPLE, "format": "text"},
    {"class": "NetProfit", "name": "Net profit", "sample_type": SAMPLE_IN_SAMPLE, "format": "money"},
    {"class": "MiniEquityChart", "name": "Mini equity chart", "sample_type": SAMPLE_IN_SAMPLE, "format": "sparkline"},
    {"class": "NumberOfTrades", "name": "# of trades", "sample_type": SAMPLE_IN_SAMPLE, "format": "integer"},
    {"class": "ProfitFactor", "name": "Profit factor", "sample_type": SAMPLE_IN_SAMPLE, "format": "decimal2"},
    {"class": "SharpeRatio", "name": "Sharpe Ratio", "sample_type": SAMPLE_IN_SAMPLE, "format": "decimal2"},
    {"class": "RExpectancy", "name": "R Expectancy", "sample_type": SAMPLE_IN_SAMPLE, "format": "decimal2"},
    {"class": "AnnualPctReturn", "name": "Annual % Return", "sample_type": SAMPLE_IN_SAMPLE, "format": "percent"},
    {"class": "Stability", "name": "Stability", "sample_type": SAMPLE_IN_SAMPLE, "format": "decimal2"},
    {"class": "Symmetry", "name": "Symmetry", "sample_type": SAMPLE_IN_SAMPLE, "format": "percent"},
    {"class": "Drawdown", "name": "Drawdown", "sample_type": SAMPLE_IN_SAMPLE, "format": "drawdown"},
    {"class": "WinLossRatio", "name": "Win/Loss ratio", "sample_type": SAMPLE_IN_SAMPLE, "format": "decimal2"},
    {"class": "ReturnDDRatio", "name": "Ret/DD Ratio", "sample_type": SAMPLE_IN_SAMPLE, "format": "decimal2"},
    {"class": "AnnualPctReturnDDRatio", "name": "CAGR/Max DD %", "sample_type": SAMPLE_IN_SAMPLE, "format": "decimal2"},
    {"class": "AvgWin", "name": "Avg. Win", "sample_type": SAMPLE_IN_SAMPLE, "format": "money"},
    {"class": "AvgLoss", "name": "Avg. Loss", "sample_type": SAMPLE_IN_SAMPLE, "format": "money"},
    {"class": "AvgBarsWin", "name": "Avg. Bars Win", "sample_type": SAMPLE_IN_SAMPLE, "format": "decimal2"},
    {"class": "AvgBarsLoss", "name": "Avg. Bars Loss", "sample_type": SAMPLE_IN_SAMPLE, "format": "decimal2"},
    {"class": "AvgBarsInTrade", "name": "Avg. Bars in Trade", "sample_type": SAMPLE_IN_SAMPLE, "format": "decimal2"},
    {"class": "Exposure", "name": "Exposure", "sample_type": SAMPLE_IN_SAMPLE, "format": "percent"},
)


def _local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def _sample_suffix(sample_type: object) -> str:
    if sample_type == SAMPLE_IN_SAMPLE:
        return " (IS)"
    if sample_type == SAMPLE_OUT_OF_SAMPLE:
        return " (OOS)"
    if sample_type == SAMPLE_FULL:
        return ""
    return ""


def default_main_data_view() -> dict[str, object]:
    """Return the Default - Main data (IS) column contract for the Results grid."""

    columns: list[dict[str, object]] = []
    for item in DEFAULT_MAIN_DATA_COLUMNS:
        sample = item["sample_type"]
        columns.append(
            {
                "class": item["class"],
                "name": item["name"],
                "sample_type": sample,
                "direction": DIRECTION_BOTH,
                "pl_type": PL_MONEY,
                "format": item["format"],
                "header": f"{item['name']}{_sample_suffix(sample)}",
            }
        )
    return {
        "name": SQX_DATABANK_VIEW_NAME,
        "original_name": SQX_DATABANK_VIEW_ORIGINAL_NAME,
        "sample_type": SAMPLE_IN_SAMPLE,
        "direction": DIRECTION_BOTH,
        "pl_type": PL_MONEY,
        "result_type": "main",
        "columns": columns,
    }


def decode_sqstats_v2(payload: str | None) -> dict[str, int | float]:
    """Decode one ``SQStats version=2 e=b64`` blob into named producer values."""

    if not isinstance(payload, str) or not payload.strip():
        return {}
    try:
        data = b64decode(payload.strip(), validate=False)
    except (ValueError, TypeError):
        return {}
    found: dict[str, int | float] = {}
    pos = 0
    length = len(data)
    try:
        while pos < length:
            kind = data[pos]
            pos += 1
            if kind == 1:
                index = data[pos]
                pos += 1
                (value,) = struct.unpack_from(">i", data, pos)
                pos += 4
                found[_INT_BY_INDEX.get(index, f"int[{index}]")] = value
            elif kind == 2:
                index = data[pos]
                pos += 1
                (value,) = struct.unpack_from(">q", data, pos)
                pos += 8
                found[_LONG_BY_INDEX.get(index, f"long[{index}]")] = value
            elif kind == 3:
                index = data[pos]
                pos += 1
                (value,) = struct.unpack_from(">f", data, pos)
                pos += 4
                found[_DOUBLE_BY_INDEX.get(index, f"dbl[{index}]")] = value
            elif kind == 101:
                name, pos = _read_modified_utf(data, pos)
                (value,) = struct.unpack_from(">i", data, pos)
                pos += 4
                found[name] = value
            elif kind == 102:
                name, pos = _read_modified_utf(data, pos)
                (value,) = struct.unpack_from(">q", data, pos)
                pos += 8
                found[name] = value
            elif kind == 103:
                name, pos = _read_modified_utf(data, pos)
                (value,) = struct.unpack_from(">f", data, pos)
                pos += 4
                found[name] = value
            else:
                break
    except (struct.error, IndexError, ValueError, UnicodeDecodeError):
        return found
    return found


def _read_modified_utf(data: bytes, pos: int) -> tuple[str, int]:
    if pos + 2 > len(data):
        raise ValueError("truncated modified UTF-8 length")
    (size,) = struct.unpack_from(">H", data, pos)
    pos += 2
    end = pos + size
    if end > len(data):
        raise ValueError("truncated modified UTF-8 payload")
    return data[pos:end].decode("utf-8"), end


def _parse_stats_tag(tag: str) -> tuple[int, int, int] | None:
    match = _STATS_TAG_RE.match(tag)
    if not match:
        return None
    return int(match.group(1)), int(match.group(2)), int(match.group(3))


def _finite_number(value: object) -> int | float | None:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        return None
    number = float(value)
    if not math.isfinite(number):
        return None
    if isinstance(value, int) and not isinstance(value, bool):
        return value
    try:
        return round2(number)
    except Exception:
        return None


def _fitness_from_element(element: ElementTree.Element | None, sample_type: int) -> float | None:
    if element is None:
        return None
    attr = _FITNESS_ATTR.get(sample_type)
    if not attr:
        return None
    raw = element.attrib.get(attr)
    try:
        value = float(raw) if raw is not None else None
    except (TypeError, ValueError):
        return None
    if value is None or not math.isfinite(value):
        return None
    # ResultsGroup.getFitness clamps non-positive values to 0 for display.
    if value <= 0.0:
        return 0.0
    return round2(value)


def _settings_map(parent: ElementTree.Element | None) -> dict[str, str]:
    found: dict[str, str] = {}
    if parent is None:
        return found
    for child in parent:
        if _local_name(child.tag) != "SettingsMap":
            continue
        for item in child:
            text = (item.text or "").strip()
            found[_local_name(item.tag)] = text
    return found


def _values_map_strings(values_map: ElementTree.Element | None) -> dict[str, str]:
    found: dict[str, str] = {}
    if values_map is None:
        return found
    for child in values_map:
        name = _local_name(child.tag)
        if name.startswith("stats_"):
            continue
        if (child.attrib.get("type") or "") == "String":
            found[name] = (child.text or "").strip()
    return found


def _values_map_stats(
    values_map: ElementTree.Element | None,
    *,
    direction: int,
    pl_type: int,
    sample_type: int,
) -> dict[str, int | float]:
    if values_map is None:
        return {}
    wanted = (direction, pl_type, sample_type)
    for child in values_map:
        if _parse_stats_tag(_local_name(child.tag)) != wanted:
            continue
        for node in child:
            if _local_name(node.tag) == "SQStats":
                return decode_sqstats_v2(node.text)
    return {}


def _select_main_result(root: ElementTree.Element) -> ElementTree.Element | None:
    results: list[ElementTree.Element] = []
    for element in root.iter():
        if _local_name(element.tag) == "Result":
            results.append(element)
    for element in results:
        key = str(element.attrib.get("resultKey") or "")
        if key.startswith("Main:") and element.attrib.get("special") != "true":
            return element
    for element in results:
        key = str(element.attrib.get("resultKey") or "")
        if key == "Portfolio" or element.attrib.get("special") == "true":
            continue
        return element
    for element in results:
        if str(element.attrib.get("resultKey") or "") == "Portfolio":
            return element
    return results[0] if results else None


def _portfolio_result(root: ElementTree.Element) -> ElementTree.Element | None:
    for element in root.iter():
        if _local_name(element.tag) == "Result" and element.attrib.get("resultKey") == "Portfolio":
            return element
    return None


def _child(parent: ElementTree.Element | None, tag: str) -> ElementTree.Element | None:
    if parent is None:
        return None
    for child in parent:
        if _local_name(child.tag) == tag:
            return child
    return None


def _parse_sparkline(text: str | None) -> dict[str, object] | None:
    if not isinstance(text, str) or not text:
        return None
    match = _SPARKLINE_RE.search(text)
    if not match:
        return None
    try:
        payload = json.loads(match.group(1))
    except json.JSONDecodeError:
        return None
    values = payload.get("values") if isinstance(payload, dict) else None
    if not isinstance(values, list) or not values:
        return None
    points: list[float] = []
    for item in values:
        if not isinstance(item, (int, float)) or isinstance(item, bool) or not math.isfinite(float(item)):
            return None
        points.append(float(item))
    zero = payload.get("zeroPoint", 0)
    zero_point = float(zero) if isinstance(zero, (int, float)) and not isinstance(zero, bool) and math.isfinite(float(zero)) else 0.0
    return {"values": points, "zero_point": zero_point}


def empty_databank_row(*, strategy_name: str) -> dict[str, object]:
    """Return a Default - Main data row with producer fields absent (dashes, not zeros)."""

    cells = {item["class"]: None for item in DEFAULT_MAIN_DATA_COLUMNS}
    cells["ResultsName"] = strategy_name
    return {
        "result_key": None,
        "strategy_name": strategy_name,
        "filters_result": None,
        "filters_reason": None,
        "symbol": None,
        "timeframe": None,
        "cells": cells,
        "mini_equity": None,
        "basis": "sqx_results_group_sqstats",
    }


def databank_row_from_settings_xml(settings_xml: bytes | None, *, archive_name: str) -> dict[str, object]:
    """Read Default - Main data IS cells from one archive ``settings.xml``."""

    strategy_name = archive_name[:-4] if archive_name.lower().endswith(".sqx") else archive_name
    empty = empty_databank_row(strategy_name=strategy_name)
    if not settings_xml:
        return empty
    try:
        root = ElementTree.fromstring(settings_xml)
    except (ElementTree.ParseError, LookupError, ValueError):
        return empty
    if _local_name(root.tag) != "ResultsGroup":
        return empty

    named = str(root.attrib.get("ResultName") or "").strip()
    if named:
        strategy_name = named
        empty["strategy_name"] = named
        empty["cells"]["ResultsName"] = named

    special = _settings_map(_child(root, "SpecialValuesMap"))
    reason = special.get("FiltersResultFailedReason")
    if reason == FILTERS_PASSED:
        empty["filters_result"] = "PASSED"
        empty["filters_reason"] = None
        empty["cells"]["FiltersResult"] = "PASSED"
    elif reason:
        empty["filters_result"] = "FAILED"
        empty["filters_reason"] = reason
        empty["cells"]["FiltersResult"] = "FAILED"

    main = _select_main_result(root)
    portfolio = _portfolio_result(root)
    fitness_source = _child(portfolio, "Fitnesses")
    if fitness_source is None:
        fitness_source = _child(main, "Fitnesses")
    fitness = _fitness_from_element(fitness_source, SAMPLE_IN_SAMPLE)
    empty["cells"]["Fitness"] = fitness

    values_map = _child(main, "ValuesMap")
    strings = _values_map_strings(values_map)
    symbol = strings.get("Symbol") or None
    timeframe = strings.get("Timeframe") or strings.get("TimeFrame") or None
    empty["symbol"] = symbol
    empty["timeframe"] = timeframe
    empty["cells"]["Symbol"] = symbol
    empty["cells"]["TimeFrame"] = timeframe
    empty["result_key"] = main.attrib.get("resultKey") if main is not None else None

    stats = _values_map_stats(
        values_map,
        direction=DIRECTION_BOTH,
        pl_type=PL_MONEY,
        sample_type=SAMPLE_IN_SAMPLE,
    )
    numeric_classes = {
        "NetProfit",
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
    }
    for column in numeric_classes:
        empty["cells"][column] = _finite_number(stats.get(column))

    spark_key = "MEC_IS_Main"
    empty["mini_equity"] = _parse_sparkline(special.get(spark_key))
    empty["cells"]["MiniEquityChart"] = None if empty["mini_equity"] is None else "sparkline"
    empty["cells"]["ResultsName"] = strategy_name
    empty["strategy_name"] = strategy_name
    return empty


def databank_row_from_archive(snapshot: bytes, *, archive_name: str) -> dict[str, object]:
    """Read Default - Main data IS cells from one ``.sqx`` snapshot."""

    strategy_name = archive_name[:-4] if archive_name.lower().endswith(".sqx") else archive_name
    try:
        with ZipFile(BytesIO(snapshot)) as handle:
            settings = handle.read("settings.xml")
    except (BadZipFile, KeyError, OSError):
        return empty_databank_row(strategy_name=strategy_name)
    return databank_row_from_settings_xml(settings, archive_name=archive_name)
