"""Native SQX Results chart metadata via resultsCharts/loadChartData.

TraderCockpit does not port SQ4.StockChart. This asks the running producer for
indicator titles and stored OHLC after the archive is in the live databank.
"""

from __future__ import annotations

from datetime import datetime, timezone
import re
from time import sleep

from .sqx_custom_project import SqxCustomProjectTopologyError
from .sqx_native_web import SqxNativeWebError, sqx_local_json
from .sqx_presets import SQX_BUILD
from .sqx_results_overview import ensure_databank_result


SQX_RESULTS_CHART_SCHEMA = "tc.sqx-results-chart.v1"
SQX_RESULTS_CHART_API_PATH = "/api/sqx-results-chart"
LOAD_CHART_PATH = "/resultsCharts/loadChartData"
CHART_QUERY = frozenset({"project", "databank", "archive", "stock"})
_MAX_CHART_BARS = 500
_STOCK_MAX = 80
_HTML_TAG = re.compile(r"<[^>]+>")
_BR = re.compile(r"<br\s*/?>", re.IGNORECASE)


def _strategy_name(archive: str) -> str:
    return archive[:-4] if archive.lower().endswith(".sqx") else archive


def _plain_error(value: object) -> str:
    text = _BR.sub(" ", str(value or ""))
    text = _HTML_TAG.sub("", text)
    return " ".join(text.split())


def _stock(value: object) -> str:
    if value is None or value == "":
        return ""
    if not isinstance(value, str) or "\0" in value or len(value) > _STOCK_MAX:
        raise SqxCustomProjectTopologyError(
            "chart_fields_invalid",
            "Chart stock must be one native symbol string.",
        )
    return value


def _empty_bars(*, detail: str, reason_code: str) -> dict[str, object]:
    return {
        "state": "unavailable",
        "reason_code": reason_code,
        "detail": detail,
        "basis": None,
        "symbol": "",
        "timeframe": "",
        "bars": [],
        "source_count": 0,
    }


def _unavailable(*, detail: str, reason_code: str = "chart_data_not_stored") -> dict[str, object]:
    return {
        "schema": SQX_RESULTS_CHART_SCHEMA,
        "source_build": SQX_BUILD,
        "producer": "unavailable",
        "stored": False,
        "reason_code": reason_code,
        "detail": detail,
        "stocks": [],
        "current_stock": None,
        "indicators": [],
        "bars": _empty_bars(detail=detail, reason_code=reason_code),
    }


def _chart_object(payload: dict[str, object]) -> dict[str, object] | None:
    error = payload.get("error")
    if error:
        return None
    if payload.get("success") is False:
        return None
    data = payload.get("data")
    if isinstance(data, dict) and isinstance(data.get("chart"), dict):
        return data["chart"]
    chart = payload.get("chart")
    if isinstance(chart, dict):
        return chart
    if isinstance(data, dict) and ("indicators" in data or "charts" in data):
        return data
    return None


def _open_time(value: object) -> str | None:
    if isinstance(value, str) and value.strip():
        return value.strip()
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        return None
    ms = float(value)
    if ms > 10_000_000_000:
        ms /= 1000.0
    try:
        return datetime.fromtimestamp(ms, tz=timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    except (OverflowError, OSError, ValueError):
        return None


def _ohlc(value: object) -> dict[str, float] | None:
    if isinstance(value, dict):
        try:
            open_px = float(value["open"])
            high = float(value["high"])
            low = float(value["low"])
            close = float(value["close"])
        except (KeyError, TypeError, ValueError):
            return None
    elif isinstance(value, (list, tuple)) and len(value) >= 4:
        try:
            open_px, high, low, close = (float(value[0]), float(value[1]), float(value[2]), float(value[3]))
        except (TypeError, ValueError):
            return None
    else:
        return None
    if not all(item == item and item != float("inf") and item != float("-inf") for item in (open_px, high, low, close)):
        return None
    return {"open": open_px, "high": high, "low": low, "close": close}


def _bars_from_chart(chart: dict[str, object]) -> list[dict[str, object]]:
    charts = chart.get("charts")
    if not isinstance(charts, list) or not charts or not isinstance(charts[0], dict):
        return []
    main = charts[0]
    xs = main.get("xVals")
    ys = main.get("yVals")
    if not isinstance(xs, list) or not isinstance(ys, list):
        return []
    rows: list[dict[str, object]] = []
    for index, y_value in enumerate(ys):
        ohlc = _ohlc(y_value)
        if ohlc is None:
            continue
        open_time = _open_time(xs[index] if index < len(xs) else None)
        if not open_time:
            continue
        rows.append({"open_time": open_time, **ohlc})
    if len(rows) > _MAX_CHART_BARS:
        rows = rows[-_MAX_CHART_BARS:]
    return rows


def _indicator_show(title: str, display_on: str, separate_shown: int) -> tuple[bool, int]:
    is_atr = title.startswith("ATR(") and title.endswith(",14)")
    if is_atr:
        return False, separate_shown
    if display_on == "chart":
        return True, separate_shown
    if separate_shown < 1:
        return True, separate_shown + 1
    return False, separate_shown


def _indicators(chart: dict[str, object]) -> list[dict[str, object]]:
    raw = chart.get("indicators")
    if not isinstance(raw, list):
        return []
    rows: list[dict[str, object]] = []
    separate_shown = 0
    for item in raw:
        if not isinstance(item, dict):
            continue
        title = item.get("title")
        ident = item.get("id")
        if not isinstance(title, str) or not title or not isinstance(ident, (str, int)):
            continue
        display_on = item.get("displayOn")
        display = display_on if isinstance(display_on, str) and display_on else "chart"
        show, separate_shown = _indicator_show(title, display, separate_shown)
        rows.append(
            {
                "id": str(ident),
                "title": title,
                "display_on": display,
                "show": show,
            }
        )
    return rows


def _stocks(chart: dict[str, object]) -> list[str]:
    raw = chart.get("stocks")
    if not isinstance(raw, list):
        return []
    return [item for item in raw if isinstance(item, str) and item and "\0" not in item]


def results_chart(
    sqx_home: object,
    *,
    project: str,
    databank: str,
    archive: str,
    stock: str = "",
    sleeper=sleep,
) -> dict[str, object]:
    chosen = _stock(stock)
    name = _strategy_name(archive)
    fields = {
        "projectName": project,
        "databankName": databank,
        "strategyName": name,
        "preview": "true",
        "requestId": "1",
    }
    if chosen:
        fields["stock"] = chosen
    try:
        ensure_databank_result(sqx_home, project, databank, archive, sleeper=sleeper)
        payload = sqx_local_json(sqx_home, LOAD_CHART_PATH, method="POST", fields=fields)
    except SqxNativeWebError as exc:
        return _unavailable(detail=exc.detail, reason_code=exc.code)
    chart = _chart_object(payload)
    if chart is None:
        detail = _plain_error(payload.get("error")) or (
            "Strategy doesn't have chart data stored. To see chart data, please check "
            "'Store Chart Data' option in Settings - Strategy options and repeat the backtest."
        )
        return {
            "schema": SQX_RESULTS_CHART_SCHEMA,
            "source_build": SQX_BUILD,
            "producer": "sqx_local_web",
            "stored": False,
            "reason_code": "chart_data_not_stored",
            "detail": detail,
            "stocks": [],
            "current_stock": None,
            "indicators": [],
            "bars": _empty_bars(detail=detail, reason_code="chart_data_not_stored"),
        }
    indicators = _indicators(chart)
    bars = _bars_from_chart(chart)
    stocks = _stocks(chart)
    current = chart.get("currentStock")
    current_stock = current if isinstance(current, str) and current else (stocks[0] if stocks else None)
    symbol = current_stock or ""
    if bars:
        bars_record = {
            "state": "available",
            "reason_code": None,
            "detail": f"{len(bars)} bars from StrategyQuant X resultsCharts/loadChartData.",
            "basis": "sqx_results_charts",
            "symbol": symbol,
            "timeframe": "",
            "bars": bars,
            "source_count": len(bars),
        }
    else:
        bars_record = _empty_bars(
            detail="StrategyQuant X returned chart indicators without OHLC bars.",
            reason_code="chart_bars_missing",
        )
    return {
        "schema": SQX_RESULTS_CHART_SCHEMA,
        "source_build": SQX_BUILD,
        "producer": "sqx_local_web",
        "stored": True,
        "reason_code": None,
        "detail": None,
        "stocks": stocks,
        "current_stock": current_stock,
        "indicators": indicators,
        "bars": bars_record,
    }
