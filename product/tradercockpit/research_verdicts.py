"""Cockpit validation verdicts over exact native StrategyQuant X trade records.

StrategyQuant X owns the backtest and its native trade records (``orders.bin``).
TraderCockpit owns the validation verdict: it recomputes the SQX databank columns
referenced by the exact native acceptance conditions (Rankings and CrossChecks
``AcceptanceSettings`` of the approved Builder task) with the published SQX column
formulas, evaluates them per native sample type, and applies the cockpit's own
documented stage policy for Golden Validation, Scenario Tests, Stress Tests and
Out-of-Sample.  Nothing here re-runs a strategy; every number is an aggregate of
producer-recorded trades and the verdict is explicitly attributed to the cockpit.
"""

from __future__ import annotations

import json
import math
import os
import random
from datetime import datetime, timezone
from hashlib import sha256
from xml.etree import ElementTree

from tradercockpit.sqx_databank import lookup_databank_column


COCKPIT_VERDICT_SCHEMA = "tc.research-cockpit-verdict.v1"
VERDICT_POLICY_ENV = "TRADERCOCKPIT_VERDICT_POLICY"
VERDICT_AUTHORITY = "tradercockpit"

# SQX SampleTypes as written by the native Builder task conditions (``sampleType``
# attribute) and used by the published ``TotalDataDays`` column: in-sample 10 with
# validation splits 11-19, out-of-sample 20-30, full sample 127.
SAMPLE_FULL = 127
SAMPLE_IN_SAMPLE = 10
SAMPLE_OUT_OF_SAMPLE = 20
_IN_SAMPLE_RANGE = range(10, 20)
_OUT_OF_SAMPLE_RANGE = range(20, 31)

DEFAULT_INITIAL_CAPITAL = 10000.0
EQUITY_POINT_LIMIT = 300

DEFAULT_VERDICT_POLICY: dict[str, float | int] = {
    "golden_min_years": 2,
    "golden_profitable_years_pct": 60,
    "scenario_min_quarters": 4,
    "scenario_profitable_quarters_pct": 50,
    "scenario_min_years_for_concentration": 3,
    "scenario_max_year_profit_share_pct": 60,
    "stress_min_trades": 20,
    "stress_simulations": 200,
    "stress_skip_trades_pct": 10,
    "stress_max_drawdown_multiple": 2.0,
    "stress_max_consecutive_losses": 10,
    "oos_min_trades": 10,
    "oos_min_profit_factor": 1.0,
    "oos_profit_factor_retention_pct": 50,
}

# Columns the cockpit can recompute from native trade rows with the published SQX
# column formulas.  Walk-forward, Monte Carlo confidence, and other producer-only
# columns are evaluated from result-archive SQStats when present; otherwise they
# stay unevaluated.
SUPPORTED_COLUMNS = frozenset({
    "NetProfit", "GrossProfit", "GrossLoss", "NumberOfTrades", "NumberOfProfits", "NumberOfLosses",
    "WinningPct", "ProfitFactor", "Drawdown", "DrawdownPct", "AvgDrawdown", "AvgPctDrawdown",
    "ReturnDDRatio", "AvgTradesPerMonth", "Expectancy", "MaxConsecLosses",
})

STAGE_IDS = ("initial-test", "fast-validation", "golden-validation", "scenario-tests", "stress-tests", "out-of-sample", "evidence")


class ResearchVerdictError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


# ---------------------------------------------------------------------------
# SQX column formulas (published SQ.Columns.Databanks snippets, SQX 144.2953)
# ---------------------------------------------------------------------------


def round2(value: float) -> float:
    """Match Java ``Math.round(value * 100) / 100.0`` (half-up toward +infinity)."""

    if not math.isfinite(value):
        raise ResearchVerdictError("verdict_value_invalid", "statistic is not finite")
    return math.floor(value * 100 + 0.5) / 100


def _safe_divide(numerator: float, denominator: float) -> float:
    return 0.0 if denominator == 0 else numerator / denominator


def _pl(trade: dict[str, object]) -> float:
    value = trade.get("PL")
    if not isinstance(value, (int, float)) or isinstance(value, bool) or not math.isfinite(float(value)):
        raise ResearchVerdictError("verdict_trade_invalid", "native trade row has no finite PL")
    return float(value)


def _time(trade: dict[str, object], key: str) -> int:
    value = trade.get(key)
    if not isinstance(value, int) or isinstance(value, bool):
        raise ResearchVerdictError("verdict_trade_invalid", f"native trade row has no integer {key}")
    return value


def _sample_type(trade: dict[str, object]) -> int:
    value = trade.get("SampleType")
    if not isinstance(value, int) or isinstance(value, bool):
        raise ResearchVerdictError("verdict_trade_invalid", "native trade row has no integer SampleType")
    return value


def select_sample(trades: list[dict[str, object]], sample_type: int) -> list[dict[str, object]]:
    """Return the native trade rows belonging to one SQX sample type family."""

    if sample_type == SAMPLE_FULL:
        return list(trades)
    if sample_type in _IN_SAMPLE_RANGE:
        return [trade for trade in trades if _sample_type(trade) in _IN_SAMPLE_RANGE]
    if sample_type in _OUT_OF_SAMPLE_RANGE:
        return [trade for trade in trades if _sample_type(trade) in _OUT_OF_SAMPLE_RANGE]
    raise ResearchVerdictError("verdict_sample_type_unsupported", f"unsupported native sample type {sample_type}")


def _drawdown_walk(pls: list[float], initial_capital: float) -> tuple[list[float], list[float], list[float]]:
    """Return (equity after each trade, money DD per trade, percent DD per trade) per SQX Drawdown/DrawdownPct."""

    peak = initial_capital
    equity = initial_capital
    equities: list[float] = []
    money_dd: list[float] = []
    pct_dd: list[float] = []
    for pl in pls:
        equity += pl
        dd = peak - equity
        if dd < 0:
            dd = 0.0
        dd = -1 * dd
        if peak <= 0 or dd > peak:
            pct = -1.0
        else:
            pct = dd / (peak / 100)
        if equity > peak:
            peak = equity
        equities.append(equity)
        money_dd.append(dd)
        pct_dd.append(pct)
    return equities, money_dd, pct_dd


_SQX_DATE_FORMATS = ("%Y.%m.%d", "%Y-%m-%d")


def _sqx_date_ms(value: str | None) -> int | None:
    if not isinstance(value, str) or not value.strip():
        return None
    text = value.strip()
    for fmt in _SQX_DATE_FORMATS:
        try:
            parsed = datetime.strptime(text, fmt).replace(tzinfo=timezone.utc)
        except ValueError:
            continue
        return int(parsed.timestamp() * 1000)
    return None


def native_chart_history_ms(settings_xml: bytes | None) -> tuple[int | None, int | None]:
    """Return the union of Setup dateFrom/dateTo from a native settings.xml, in epoch ms."""

    if not settings_xml:
        return None, None
    try:
        root = ElementTree.fromstring(settings_xml)
    except ElementTree.ParseError:
        return None, None
    starts: list[int] = []
    ends: list[int] = []
    for setup in root.iter("Setup"):
        start = _sqx_date_ms(setup.attrib.get("dateFrom"))
        end = _sqx_date_ms(setup.attrib.get("dateTo"))
        if start is not None and end is not None and end > start:
            starts.append(start)
            ends.append(end)
    if not starts:
        return None, None
    return min(starts), max(ends)


def sqx_statistics(
    trades: list[dict[str, object]],
    *,
    initial_capital: float = DEFAULT_INITIAL_CAPITAL,
    chart_from_ms: int | None = None,
    chart_to_ms: int | None = None,
) -> dict[str, object]:
    """Recompute the SQX databank columns used by native acceptance conditions.

    Formulas follow the published SQX column snippets exactly where they depend only on
    the trade list.  ``AvgTradesPerMonth`` uses the native chart history range from
    ``settings.xml`` when that range is present; otherwise the traded span. The basis
    is reported so the value is never mistaken for a silent default.
    """

    if not isinstance(initial_capital, (int, float)) or isinstance(initial_capital, bool) or not math.isfinite(float(initial_capital)):
        raise ResearchVerdictError("verdict_capital_invalid", "initial capital must be finite")
    rows = [trade for trade in trades if isinstance(trade, dict)]
    if len(rows) != len(trades):
        raise ResearchVerdictError("verdict_trade_invalid", "native trade rows must be objects")
    pls = [_pl(trade) for trade in rows]
    count = len(rows)
    net_profit = sum(pls)
    gross_profit = sum(pl for pl in pls if pl > 0)
    gross_loss = sum(abs(pl) for pl in pls if pl < 0)
    profits = sum(1 for pl in pls if pl > 0)
    losses = sum(1 for pl in pls if pl < 0)

    if count == 0:
        profit_factor = 0.0
    elif gross_loss == 0:
        profit_factor = 0.0 if net_profit == 0 else 5.0
    else:
        profit_factor = round2(gross_profit / gross_loss)

    equities, money_dd, pct_dd = _drawdown_walk(pls, float(initial_capital))
    drawdown = round2(abs(min(money_dd))) if money_dd else 0.0
    avg_drawdown = round2(abs(_safe_divide(sum(money_dd), count))) if money_dd else 0.0
    drawdown_pct = round2(abs(min(pct_dd))) if pct_dd else 0.0
    avg_pct_drawdown = round2(abs(_safe_divide(sum(pct_dd), count))) if pct_dd else 0.0

    if count == 0:
        return_dd_ratio = 0.0
    elif drawdown == 0:
        return_dd_ratio = 0.0 if net_profit == 0 else 10.0
    else:
        return_dd_ratio = round2(_safe_divide(net_profit, drawdown))

    first_open = min(_time(trade, "OpenTime") for trade in rows) if rows else None
    last_close = max(_time(trade, "CloseTime") for trade in rows) if rows else None
    chart_ok = (
        isinstance(chart_from_ms, int)
        and isinstance(chart_to_ms, int)
        and chart_to_ms > chart_from_ms
    )
    if chart_ok:
        days = (chart_to_ms - chart_from_ms) // 86_400_000
        months_basis = "native_chart_history"
    else:
        days = 0
        if first_open is not None and last_close is not None and last_close > first_open:
            days = (last_close - first_open) // 86_400_000
        months_basis = "traded_span"
    months = int(days / 30.41)
    if days > 0 and months == 0:
        months = 1

    consecutive = 0
    max_consecutive_losses = 0
    for pl in pls:
        if pl < 0:
            consecutive += 1
            max_consecutive_losses = max(consecutive, max_consecutive_losses)
        else:
            consecutive = 0

    return {
        "NumberOfTrades": count,
        "NumberOfProfits": profits,
        "NumberOfLosses": losses,
        "NetProfit": round2(net_profit),
        "GrossProfit": round2(gross_profit),
        "GrossLoss": round2(gross_loss),
        "WinningPct": round2(_safe_divide(profits, profits + losses) * 100),
        "ProfitFactor": profit_factor,
        "Drawdown": drawdown,
        "AvgDrawdown": avg_drawdown,
        "DrawdownPct": drawdown_pct,
        "AvgPctDrawdown": avg_pct_drawdown,
        "ReturnDDRatio": return_dd_ratio,
        "Expectancy": round2(_safe_divide(net_profit, count)),
        "AvgTradesPerMonth": round2(_safe_divide(count, months)),
        "MaxConsecLosses": max_consecutive_losses,
        "TotalDataDays": int(days),
        "TotalDataMonths": months,
        "first_open_time": first_open,
        "last_close_time": last_close,
        "initial_capital": float(initial_capital),
        "final_equity": round2(equities[-1]) if equities else float(initial_capital),
        "months_basis": months_basis,
        "chart_from_ms": chart_from_ms if chart_ok else None,
        "chart_to_ms": chart_to_ms if chart_ok else None,
    }


def equity_points(trades: list[dict[str, object]], *, initial_capital: float, limit: int = EQUITY_POINT_LIMIT) -> list[dict[str, float | int]]:
    """Return a bounded equity series (native list order) from producer-recorded P/L."""

    pls = [_pl(trade) for trade in trades]
    equities, _, _ = _drawdown_walk(pls, float(initial_capital))
    points = [{"time": _time(trade, "CloseTime"), "balance": round2(equity)} for trade, equity in zip(trades, equities)]
    if len(points) <= limit:
        return points
    step = len(points) / limit
    sampled = [points[int(index * step)] for index in range(limit - 1)]
    sampled.append(points[-1])
    return sampled


# ---------------------------------------------------------------------------
# Native acceptance conditions
# ---------------------------------------------------------------------------


def native_node_record(element: ElementTree.Element) -> dict[str, object]:
    text = (element.text or "").strip() or None
    return {
        "tag": element.tag.rsplit("}", 1)[-1],
        "attributes": {str(key): str(value) for key, value in element.attrib.items()},
        "text": text,
        "children": [native_node_record(child) for child in list(element)],
    }


def native_task_sections(task_xml: bytes) -> dict[str, dict[str, object] | None]:
    """Extract Rankings, CrossChecks and MoneyManagement node records from one Builder task XML."""

    try:
        root = ElementTree.fromstring(task_xml)
    except (ElementTree.ParseError, LookupError, ValueError) as exc:
        raise ResearchVerdictError("verdict_task_xml_invalid", "native Builder task XML is not parseable") from exc
    sections: dict[str, dict[str, object] | None] = {"rankings": None, "cross_checks": None, "money_management": None}
    wanted = {"Rankings": "rankings", "CrossChecks": "cross_checks", "MoneyManagement": "money_management"}
    for element in root.iter():
        name = element.tag.rsplit("}", 1)[-1]
        key = wanted.get(name)
        if key is not None and sections[key] is None:
            sections[key] = native_node_record(element)
    return sections


def _children(node: dict[str, object] | None, tag: str) -> list[dict[str, object]]:
    if not isinstance(node, dict):
        return []
    return [child for child in node.get("children", []) if isinstance(child, dict) and child.get("tag") == tag]  # type: ignore[union-attr]


def _first(node: dict[str, object] | None, tag: str) -> dict[str, object] | None:
    matches = _children(node, tag)
    return matches[0] if matches else None


def _attr(node: dict[str, object] | None, name: str) -> str | None:
    if not isinstance(node, dict):
        return None
    attributes = node.get("attributes")
    if not isinstance(attributes, dict):
        return None
    value = attributes.get(name)
    return value if isinstance(value, str) else None


def _iter_conditions(node: dict[str, object]) -> list[dict[str, object]]:
    found: list[dict[str, object]] = []
    stack = [node]
    while stack:
        current = stack.pop()
        if current.get("tag") == "Condition":
            found.append(current)
            continue
        stack.extend(reversed([child for child in current.get("children", []) if isinstance(child, dict)]))  # type: ignore[union-attr]
    return found


def _int_attr(node: dict[str, object] | None, name: str, default: int) -> int:
    raw = _attr(node, name)
    if raw is None:
        return default
    try:
        return int(raw)
    except ValueError:
        return default


def parse_native_conditions(node: dict[str, object] | None) -> list[dict[str, object]]:
    """Return the enabled acceptance conditions of one native Rankings/AcceptanceSettings node."""

    if not isinstance(node, dict):
        return []
    conditions: list[dict[str, object]] = []
    for condition in _iter_conditions(node):
        if (_attr(condition, "use") or "").lower() != "true":
            continue
        left = _first(_first(condition, "Left-Side"), "Column-Value")
        comparator = _attr(_first(condition, "Comparator"), "value")
        right_side = _first(condition, "Right-Side")
        numeric = _first(right_side, "Numeric-Value")
        right_column = _first(right_side, "Column-Value")
        if left is None or comparator is None:
            continue
        threshold: float | None = None
        raw_threshold = _attr(numeric, "value")
        if raw_threshold is not None:
            try:
                threshold = float(raw_threshold)
            except ValueError:
                threshold = None
        conditions.append({
            "column": _attr(left, "column") or _attr(left, "class") or "",
            "name": _attr(left, "name") or _attr(left, "column") or _attr(left, "class") or "",
            "sample_type": _int_attr(left, "sampleType", SAMPLE_FULL),
            "direction": _int_attr(left, "direction", 0),
            "confidence_level": _int_attr(left, "confidenceLevel", 50),
            "comparator": comparator,
            "threshold": threshold,
            "threshold_column": _attr(right_column, "column") if right_column is not None else None,
            "threshold_sample_type": _int_attr(right_column, "sampleType", SAMPLE_FULL) if right_column is not None else None,
            "threshold_direction": _int_attr(right_column, "direction", 0) if right_column is not None else None,
            "threshold_confidence_level": _int_attr(right_column, "confidenceLevel", 50) if right_column is not None else None,
        })
    return conditions


def _compare(value: float, comparator: str, threshold: float) -> bool | None:
    if comparator == ">":
        return value > threshold
    if comparator == ">=":
        return value >= threshold
    if comparator == "<":
        return value < threshold
    if comparator == "<=":
        return value <= threshold
    if comparator in {"=", "=="}:
        return value == threshold
    if comparator in {"!=", "<>"}:
        return value != threshold
    return None


def _sample_label(sample_type: int) -> str:
    if sample_type == SAMPLE_FULL:
        return "full sample"
    if sample_type in _IN_SAMPLE_RANGE:
        return "in-sample"
    if sample_type in _OUT_OF_SAMPLE_RANGE:
        return "out-of-sample"
    return f"sample {sample_type}"


def _databank_value(
    native_columns: list[dict[str, object]] | None,
    column: str,
    *,
    sample_type: int,
    direction: int,
    confidence_level: int,
) -> float | None:
    return lookup_databank_column(
        native_columns,
        column,
        sample_type=sample_type,
        direction=direction,
        confidence_level=confidence_level,
    )


def evaluate_native_conditions(
    conditions: list[dict[str, object]],
    trades: list[dict[str, object]],
    *,
    initial_capital: float,
    chart_from_ms: int | None = None,
    chart_to_ms: int | None = None,
    native_columns: list[dict[str, object]] | None = None,
) -> list[dict[str, object]]:
    """Evaluate native acceptance conditions over native trade rows; unevaluable ones stay explicit."""

    cache: dict[int, dict[str, object]] = {}
    checks: list[dict[str, object]] = []
    for condition in conditions:
        column = str(condition.get("column") or "")
        sample_type = int(condition.get("sample_type", SAMPLE_FULL))
        direction = int(condition.get("direction", 0))
        confidence_level = int(condition.get("confidence_level", 50))
        comparator = str(condition.get("comparator") or "")
        threshold = condition.get("threshold")
        label = f"{column} ({_sample_label(sample_type)}) {comparator} {threshold if threshold is not None else condition.get('threshold_column')}"
        check: dict[str, object] = {
            "label": label,
            "column": column,
            "sample": _sample_label(sample_type),
            "comparator": comparator,
            "threshold": threshold,
            "value": None,
            "state": "unevaluated",
            "detail": None,
            "source": "native_condition",
        }
        needs_producer = (
            column not in SUPPORTED_COLUMNS
            or condition.get("threshold_column") is not None
            or threshold is None
            or direction != 0
            or confidence_level != 50
        )
        if needs_producer:
            left = _databank_value(
                native_columns,
                column,
                sample_type=sample_type,
                direction=direction,
                confidence_level=confidence_level,
            )
            right: float | None = None
            threshold_column = condition.get("threshold_column")
            if isinstance(threshold_column, str) and threshold_column:
                right = _databank_value(
                    native_columns,
                    threshold_column,
                    sample_type=int(condition.get("threshold_sample_type") or SAMPLE_FULL),
                    direction=int(condition.get("threshold_direction") or 0),
                    confidence_level=int(condition.get("threshold_confidence_level") or 50),
                )
                if left is not None and right is not None:
                    outcome = _compare(left, comparator, right)
                    check["value"] = left
                    check["threshold"] = right
                    if outcome is None:
                        check["detail"] = f"Comparator {comparator!r} is not supported."
                    else:
                        check["state"] = "pass" if outcome else "fail"
                        check["detail"] = "Producer databank column-to-column values."
                else:
                    check["detail"] = "Column-to-column acceptance needs native confidence-level results."
            elif left is not None and isinstance(threshold, (int, float)) and not isinstance(threshold, bool):
                outcome = _compare(left, comparator, float(threshold))
                check["value"] = left
                if outcome is None:
                    check["detail"] = f"Comparator {comparator!r} is not supported."
                else:
                    check["state"] = "pass" if outcome else "fail"
                    check["detail"] = "Producer databank column."
            elif column not in SUPPORTED_COLUMNS:
                check["detail"] = f"{column} requires the native producer run; the cockpit does not recompute it."
            elif threshold_column is not None or threshold is None:
                check["detail"] = "Column-to-column acceptance needs native confidence-level results."
            elif direction != 0:
                check["detail"] = "Directional (long/short) acceptance is not split by the cockpit."
            else:
                check["detail"] = "Confidence-level acceptance needs native Monte Carlo results."
        else:
            try:
                rows = select_sample(trades, sample_type)
            except ResearchVerdictError as exc:
                check["detail"] = exc.detail
                checks.append(check)
                continue
            if sample_type not in cache:
                cache[sample_type] = sqx_statistics(
                    rows,
                    initial_capital=initial_capital,
                    chart_from_ms=chart_from_ms,
                    chart_to_ms=chart_to_ms,
                )
            value = cache[sample_type][column]
            outcome = _compare(float(value), comparator, float(threshold))
            check["value"] = value
            if outcome is None:
                check["detail"] = f"Comparator {comparator!r} is not supported."
            else:
                check["state"] = "pass" if outcome else "fail"
        checks.append(check)
    return checks


# ---------------------------------------------------------------------------
# Cockpit policy checks
# ---------------------------------------------------------------------------


def verdict_policy(environ: dict[str, str] | None = None) -> dict[str, object]:
    """Return the effective cockpit verdict policy (defaults merged with the JSON override)."""

    env = os.environ if environ is None else environ
    policy: dict[str, float | int] = dict(DEFAULT_VERDICT_POLICY)
    warnings: list[str] = []
    source = "default"
    raw = env.get(VERDICT_POLICY_ENV)
    if raw:
        try:
            override = json.loads(raw)
        except json.JSONDecodeError:
            override = None
            warnings.append(f"{VERDICT_POLICY_ENV} is not valid JSON; defaults apply.")
        if isinstance(override, dict):
            source = "environment"
            for key, value in override.items():
                if key not in DEFAULT_VERDICT_POLICY:
                    warnings.append(f"unknown policy key {key!r} ignored")
                    continue
                if isinstance(value, bool) or not isinstance(value, (int, float)) or not math.isfinite(float(value)) or float(value) < 0:
                    warnings.append(f"policy key {key!r} must be a non-negative number; default applies")
                    continue
                policy[key] = int(value) if isinstance(DEFAULT_VERDICT_POLICY[key], int) else float(value)
        elif override is not None:
            warnings.append(f"{VERDICT_POLICY_ENV} must be a JSON object; defaults apply.")
    return {"values": policy, "source": source, "warnings": warnings}


def _policy_check(label: str, value: object, comparator: str, threshold: object, *, unit: str = "", detail: str | None = None) -> dict[str, object]:
    state = "unevaluated"
    if isinstance(value, (int, float)) and not isinstance(value, bool) and isinstance(threshold, (int, float)):
        outcome = _compare(float(value), comparator, float(threshold))
        if outcome is not None:
            state = "pass" if outcome else "fail"
    return {
        "label": label,
        "column": None,
        "sample": None,
        "comparator": comparator,
        "threshold": threshold,
        "value": value,
        "unit": unit,
        "state": state,
        "detail": detail,
        "source": "cockpit_policy",
    }


def _bucket(trade: dict[str, object], granularity: str) -> str:
    moment = datetime.fromtimestamp(_time(trade, "CloseTime") / 1000, tz=timezone.utc)
    if granularity == "year":
        return f"{moment.year}"
    return f"{moment.year}-Q{(moment.month - 1) // 3 + 1}"


def period_profits(trades: list[dict[str, object]], granularity: str) -> dict[str, float]:
    buckets: dict[str, float] = {}
    for trade in trades:
        key = _bucket(trade, granularity)
        buckets[key] = buckets.get(key, 0.0) + _pl(trade)
    return {key: round2(value) for key, value in sorted(buckets.items())}


def _percentile(values: list[float], percentile: float) -> float:
    ordered = sorted(values)
    if not ordered:
        return 0.0
    rank = max(1, math.ceil(percentile / 100 * len(ordered)))
    return ordered[min(rank, len(ordered)) - 1]


def monte_carlo_trade_manipulation(
    trades: list[dict[str, object]],
    *,
    initial_capital: float,
    simulations: int,
    skip_trades_pct: float,
    seed: int,
) -> dict[str, object]:
    """Trade-order shuffle plus random trade skipping over producer-recorded P/L.

    This resamples the native trade list only (the same manipulation family as the
    native ``MonteCarloManipulation`` RandomizeTradesOrder/RandomlySkipTrades methods);
    it never re-runs the strategy.  The seed is derived from the result archive so the
    verdict is reproducible.
    """

    pls = [_pl(trade) for trade in trades]
    if not pls or simulations <= 0:
        return {"simulations": 0, "net_profit_p5": None, "drawdown_p95": None, "seed": seed}
    rng = random.Random(seed)
    skip = max(0.0, min(float(skip_trades_pct), 100.0)) / 100
    nets: list[float] = []
    drawdowns: list[float] = []
    for _ in range(simulations):
        sample = list(pls)
        rng.shuffle(sample)
        kept = [pl for pl in sample if rng.random() >= skip] if skip > 0 else sample
        if not kept:
            kept = [sample[0]]
        _, money_dd, _ = _drawdown_walk(kept, float(initial_capital))
        nets.append(sum(kept))
        drawdowns.append(abs(min(money_dd)))
    return {
        "simulations": simulations,
        "skip_trades_pct": skip * 100,
        "net_profit_p5": round2(_percentile(nets, 5)),
        "net_profit_median": round2(_percentile(nets, 50)),
        "drawdown_p95": round2(_percentile(drawdowns, 95)),
        "drawdown_median": round2(_percentile(drawdowns, 50)),
        "seed": seed,
    }


def _stage(stage_id: str, state: str, checks: list[dict[str, object]], detail: str, *, source: str, basis: str | None = None) -> dict[str, object]:
    passed = sum(1 for check in checks if check["state"] == "pass")
    return {
        "id": stage_id,
        "state": state,
        "source": source,
        "basis": basis,
        "detail": detail,
        "checks": checks,
        "checks_passed": passed,
        "checks_total": len(checks),
    }


def _state_from_checks(checks: list[dict[str, object]]) -> str:
    if not checks:
        return "incomplete"
    if any(check["state"] == "fail" for check in checks):
        return "fail"
    if any(check["state"] in {"unevaluated", "incomplete"} for check in checks):
        return "incomplete"
    return "pass"


def _initial_capital(money_management: dict[str, object] | None) -> tuple[float, str]:
    node = _first(money_management, "InitialCapital")
    text = node.get("text") if isinstance(node, dict) else None
    if isinstance(text, str):
        try:
            value = float(text)
            if math.isfinite(value) and value > 0:
                return value, "native_money_management"
        except ValueError:
            pass
    return DEFAULT_INITIAL_CAPITAL, "sqx_default"


def verdict_seed(digest: str) -> int:
    return int(sha256(digest.encode("utf-8")).hexdigest()[:16], 16)


def select_additional_market_trades(trades: list[dict[str, object]]) -> list[dict[str, object]]:
    """Keep producer Additional Markets rows (SetupName prefix observed on SQX 144.2953)."""

    return [trade for trade in trades if str(trade.get("SetupName") or "").startswith("AdditionalMarket:")]


def _bound_native_method_checks(
    *,
    method: str,
    label: str,
    cross_checks: dict[str, object] | None,
    trades: list[dict[str, object]] | None,
    detail: str | None,
    stats_kw: dict[str, object],
) -> list[dict[str, object]]:
    if trades is None:
        return []
    conditions = parse_native_conditions(_first(_first(cross_checks, method), "AcceptanceSettings"))
    if not conditions:
        return [{
            "label": f"{label} native acceptance",
            "column": None,
            "sample": None,
            "comparator": None,
            "threshold": None,
            "value": None,
            "unit": "",
            "state": "unevaluated",
            "detail": detail or f"The executed {label} profile defines no enabled acceptance conditions.",
            "source": "native_condition",
        }]
    checks = evaluate_native_conditions(conditions, trades, **stats_kw)
    for check in checks:
        check["label"] = f"{label} · {check['label']}"
    return checks


def _bound_run_checks(
    cross_check_runs: dict[str, dict[str, object]] | None,
    method: str,
    label: str,
    stats_kw: dict[str, object],
) -> list[dict[str, object]]:
    run = (cross_check_runs or {}).get(method)
    if not isinstance(run, dict):
        return []
    native_columns = run.get("native_columns") if isinstance(run.get("native_columns"), list) else None
    return _bound_native_method_checks(
        method=method,
        label=label,
        cross_checks=run.get("cross_checks") if isinstance(run.get("cross_checks"), dict) else None,
        trades=run.get("trades") if isinstance(run.get("trades"), list) else None,
        detail=run.get("detail") if isinstance(run.get("detail"), str) else None,
        stats_kw={**stats_kw, "native_columns": native_columns},
    )


def cockpit_verdict(
    *,
    historical_trades: list[dict[str, object]],
    higher_precision_trades: list[dict[str, object]] | None,
    rankings: dict[str, object] | None,
    cross_checks: dict[str, object] | None,
    money_management: dict[str, object] | None,
    proof_count: int,
    seed_digest: str,
    native_conditions_state: str,
    native_conditions_detail: str | None = None,
    higher_precision_detail: str | None = None,
    additional_market_trades: list[dict[str, object]] | None = None,
    additional_market_detail: str | None = None,
    additional_market_cross_checks: dict[str, object] | None = None,
    additional_market_native_columns: list[dict[str, object]] | None = None,
    cross_check_runs: dict[str, dict[str, object]] | None = None,
    native_columns: list[dict[str, object]] | None = None,
    higher_precision_native_columns: list[dict[str, object]] | None = None,
    policy: dict[str, object] | None = None,
    chart_from_ms: int | None = None,
    chart_to_ms: int | None = None,
) -> dict[str, object]:
    """Compute the seven-stage cockpit verdict for one completed native Historical Result."""

    effective = policy if policy is not None else verdict_policy()
    values = effective["values"]  # type: ignore[index]
    initial_capital, capital_source = _initial_capital(money_management)
    stats_kw = {
        "initial_capital": initial_capital,
        "chart_from_ms": chart_from_ms,
        "chart_to_ms": chart_to_ms,
    }
    ranking_kw = {**stats_kw, "native_columns": native_columns}
    hp_kw = {**stats_kw, "native_columns": higher_precision_native_columns}
    am_kw = {**stats_kw, "native_columns": additional_market_native_columns}
    stages: list[dict[str, object]] = []
    conditions_missing = native_conditions_state != "available"

    # 1 · Initial Test — native Rankings acceptance conditions on the Retester result.
    ranking_conditions = parse_native_conditions(rankings)
    if conditions_missing:
        stages.append(_stage("initial-test", "incomplete", [], native_conditions_detail or "Native acceptance conditions are not readable from configuration custody.", source="native_condition", basis="historical_result"))
    elif not ranking_conditions:
        stages.append(_stage("initial-test", "incomplete", [], "The approved native task defines no enabled Rankings acceptance conditions.", source="native_condition", basis="historical_result"))
    else:
        checks = evaluate_native_conditions(ranking_conditions, historical_trades, **ranking_kw)
        stages.append(_stage("initial-test", _state_from_checks(checks), checks, "Native Rankings acceptance conditions evaluated by the cockpit over the native trade records.", source="native_condition", basis="historical_result"))

    # 2 · Fast Validation — native Higher Precision acceptance on the Higher Precision result.
    hp_node = _first(cross_checks, "RetestWithHigherPrecision")
    hp_conditions = parse_native_conditions(_first(hp_node, "AcceptanceSettings")) if hp_node is not None else []
    if higher_precision_trades is None:
        stages.append(_stage("fast-validation", "not_run", [], higher_precision_detail or "No completed Higher Precision retest is bound to this result yet.", source="native_condition", basis="higher_precision_result"))
    elif conditions_missing:
        stages.append(_stage("fast-validation", "incomplete", [], native_conditions_detail or "Native acceptance conditions are not readable from configuration custody.", source="native_condition", basis="higher_precision_result"))
    elif not hp_conditions:
        stages.append(_stage("fast-validation", "incomplete", [], "The approved native task defines no enabled Higher Precision acceptance conditions.", source="native_condition", basis="higher_precision_result"))
    else:
        checks = evaluate_native_conditions(hp_conditions, higher_precision_trades, **hp_kw)
        stages.append(_stage("fast-validation", _state_from_checks(checks), checks, "Native RetestWithHigherPrecision acceptance conditions evaluated by the cockpit over the Higher Precision trade records.", source="native_condition", basis="higher_precision_result"))

    # 3 · Golden Validation — Initial Test criteria must survive higher-precision data, and the
    # equity must be consistent across calendar years.
    if higher_precision_trades is None:
        stages.append(_stage("golden-validation", "not_run", [], "Requires the completed Higher Precision retest.", source="cockpit_policy", basis="higher_precision_result"))
    elif conditions_missing or not ranking_conditions:
        stages.append(_stage("golden-validation", "incomplete", [], "Requires readable native Rankings acceptance conditions.", source="cockpit_policy", basis="higher_precision_result"))
    else:
        checks = evaluate_native_conditions(ranking_conditions, higher_precision_trades, **hp_kw)
        for check in checks:
            check["label"] = f"Initial criteria on higher precision · {check['label']}"
        years = period_profits(higher_precision_trades, "year")
        profitable_years = sum(1 for value in years.values() if value > 0)
        years_pct = round2(_safe_divide(profitable_years, len(years)) * 100) if years else 0.0
        checks.append(_policy_check("Calendar years traded", len(years), ">=", values["golden_min_years"]))
        checks.append(_policy_check("Profitable calendar years", years_pct, ">=", values["golden_profitable_years_pct"], unit="%", detail=f"{profitable_years} of {len(years)} years profitable"))
        if additional_market_trades is not None:
            am_source = additional_market_cross_checks if additional_market_cross_checks is not None else cross_checks
            am_conditions = parse_native_conditions(_first(_first(am_source, "RetestOnAdditionalMarkets"), "AcceptanceSettings"))
            if not am_conditions:
                checks.append({
                    "label": "Additional Markets native acceptance",
                    "column": None,
                    "sample": None,
                    "comparator": None,
                    "threshold": None,
                    "value": None,
                    "unit": "",
                    "state": "unevaluated",
                    "detail": additional_market_detail or "The executed Additional Markets profile defines no enabled acceptance conditions.",
                    "source": "native_condition",
                })
            else:
                am_checks = evaluate_native_conditions(am_conditions, additional_market_trades, **am_kw)
                for check in am_checks:
                    check["label"] = f"Additional Markets · {check['label']}"
                checks.extend(am_checks)
        golden_detail = "Cockpit policy: Initial Test criteria re-verified on higher-precision data plus year-over-year consistency."
        if additional_market_trades is not None:
            golden_detail += " Native RetestOnAdditionalMarkets acceptance is evaluated over AdditionalMarket: trade rows when that CrossCheck has completed."
        stages.append(_stage("golden-validation", _state_from_checks(checks), checks, golden_detail, source="cockpit_policy", basis="higher_precision_result"))

    # 4 · Scenario Tests — regime consistency across calendar quarters and profit concentration.
    quarters = period_profits(historical_trades, "quarter")
    years_all = period_profits(historical_trades, "year")
    full_stats = sqx_statistics(historical_trades, **stats_kw)
    checks = [_policy_check("Calendar quarters traded", len(quarters), ">=", values["scenario_min_quarters"])]
    if len(quarters) >= int(values["scenario_min_quarters"]):
        profitable_quarters = sum(1 for value in quarters.values() if value > 0)
        checks.append(_policy_check("Profitable calendar quarters", round2(_safe_divide(profitable_quarters, len(quarters)) * 100), ">=", values["scenario_profitable_quarters_pct"], unit="%", detail=f"{profitable_quarters} of {len(quarters)} quarters profitable"))
        net = float(full_stats["NetProfit"])
        min_years = int(values["scenario_min_years_for_concentration"])
        if net <= 0:
            checks.append(_policy_check("Largest single-year share of net profit", None, "<=", values["scenario_max_year_profit_share_pct"], unit="%", detail="Net profit is not positive; concentration is undefined."))
            checks[-1]["state"] = "fail"
        elif len(years_all) < min_years:
            checks.append(_policy_check("Largest single-year share of net profit", None, "<=", values["scenario_max_year_profit_share_pct"], unit="%", detail=f"Needs at least {min_years} calendar years traded ({len(years_all)} available)."))
        else:
            share = round2(max(years_all.values()) / net * 100)
            checks.append(_policy_check("Largest single-year share of net profit", share, "<=", values["scenario_max_year_profit_share_pct"], unit="%", detail=f"{len(years_all)} calendar years"))
    else:
        checks.append(_policy_check("Profitable calendar quarters", None, ">=", values["scenario_profitable_quarters_pct"], unit="%", detail="Too few calendar quarters traded to assess regimes."))
    checks.extend(_bound_run_checks(cross_check_runs, "WhatIf", "What-If", stats_kw))
    checks.extend(_bound_run_checks(cross_check_runs, "OptProfileSysParamPermutation", "System Parameter Permutation", stats_kw))
    stages.append(_stage("scenario-tests", _state_from_checks(checks), checks, "Cockpit policy: profitability across calendar quarters and single-year profit concentration over the native trade records.", source="cockpit_policy", basis="historical_result"))

    # 5 · Stress Tests — seeded trade-order/skip Monte Carlo over the native trade list.
    trade_count = int(full_stats["NumberOfTrades"])
    checks = [_policy_check("Trades available for resampling", trade_count, ">=", values["stress_min_trades"])]
    monte_carlo: dict[str, object] | None = None
    if trade_count >= int(values["stress_min_trades"]):
        monte_carlo = monte_carlo_trade_manipulation(
            historical_trades,
            initial_capital=initial_capital,
            simulations=int(values["stress_simulations"]),
            skip_trades_pct=float(values["stress_skip_trades_pct"]),
            seed=verdict_seed(seed_digest),
        )
        observed_dd = float(full_stats["Drawdown"])
        dd_limit = round2(observed_dd * float(values["stress_max_drawdown_multiple"]))
        checks.append(_policy_check("Monte Carlo net profit (5th percentile)", monte_carlo["net_profit_p5"], ">", 0, detail=f"{monte_carlo['simulations']} shuffles · {monte_carlo['skip_trades_pct']:.0f}% trades skipped"))
        checks.append(_policy_check("Monte Carlo drawdown (95th percentile)", monte_carlo["drawdown_p95"], "<=", dd_limit, detail=f"limit = {values['stress_max_drawdown_multiple']}× observed drawdown {observed_dd}"))
        checks.append(_policy_check("Max consecutive losses", full_stats["MaxConsecLosses"], "<=", values["stress_max_consecutive_losses"]))
    else:
        checks.append(_policy_check("Monte Carlo net profit (5th percentile)", None, ">", 0, detail="Too few trades for a meaningful resample."))
    checks.extend(_bound_run_checks(cross_check_runs, "MonteCarloRetest", "Monte Carlo retest", stats_kw))
    checks.extend(_bound_run_checks(cross_check_runs, "MonteCarloManipulation", "Monte Carlo manipulation", stats_kw))
    stages.append(_stage("stress-tests", _state_from_checks(checks), checks, "Cockpit policy: seeded trade-order shuffle with random trade skipping over the native trade records.", source="cockpit_policy", basis="historical_result"))

    # 6 · Out-of-Sample — native sample-type 20-30 trades must hold up on their own.
    oos_rows = select_sample(historical_trades, SAMPLE_OUT_OF_SAMPLE)
    is_rows = select_sample(historical_trades, SAMPLE_IN_SAMPLE)
    walk_forward_checks = (
        _bound_run_checks(cross_check_runs, "WalkForwardOptimization", "Walk-Forward", stats_kw)
        + _bound_run_checks(cross_check_runs, "WalkForwardMatrix", "Walk-Forward Matrix", stats_kw)
        + _bound_run_checks(cross_check_runs, "SequentialOptimization", "Sequential Optimization", stats_kw)
    )
    if not oos_rows:
        if walk_forward_checks:
            stages.append(_stage("out-of-sample", _state_from_checks(walk_forward_checks), walk_forward_checks, "Native Walk-Forward CrossChecks are bound; the Historical Result itself has no out-of-sample trades.", source="cockpit_policy", basis="historical_result"))
        else:
            stages.append(_stage("out-of-sample", "not_run", [], "The native result contains no out-of-sample trades (SampleType 20-30); configure an out-of-sample range in the native data setup.", source="cockpit_policy", basis="historical_result"))
    else:
        oos_stats = sqx_statistics(oos_rows, **stats_kw)
        is_stats = sqx_statistics(is_rows, **stats_kw) if is_rows else None
        checks = [
            _policy_check("Out-of-sample trades", oos_stats["NumberOfTrades"], ">=", values["oos_min_trades"]),
            _policy_check("Out-of-sample net profit", oos_stats["NetProfit"], ">", 0),
            _policy_check("Out-of-sample profit factor", oos_stats["ProfitFactor"], ">=", values["oos_min_profit_factor"]),
        ]
        if is_stats is not None and float(is_stats["ProfitFactor"]) > 0:
            retention = round2(float(oos_stats["ProfitFactor"]) / float(is_stats["ProfitFactor"]) * 100)
            checks.append(_policy_check("Profit factor retained vs in-sample", retention, ">=", values["oos_profit_factor_retention_pct"], unit="%", detail=f"in-sample {is_stats['ProfitFactor']} → out-of-sample {oos_stats['ProfitFactor']}"))
        else:
            checks.append(_policy_check("Profit factor retained vs in-sample", None, ">=", values["oos_profit_factor_retention_pct"], unit="%", detail="No in-sample profit factor to compare against."))
        checks.extend(walk_forward_checks)
        stages.append(_stage("out-of-sample", _state_from_checks(checks), checks, "Cockpit policy over the native out-of-sample trade records.", source="cockpit_policy", basis="historical_result"))

    # 7 · Evidence — immutable Research Proof custody.
    if proof_count > 0:
        stages.append(_stage("evidence", "pass", [_policy_check("Research Proofs bound to this result", proof_count, ">=", 1)], "Immutable Research Proof custody binds this result.", source="custody", basis="proof"))
    else:
        stages.append(_stage("evidence", "not_run", [], "Not promoted to evidence yet.", source="custody", basis="proof"))

    states = [stage["state"] for stage in stages]
    if "fail" in states:
        overall, label = "fail", "Rejected"
    elif all(state == "pass" for state in states):
        overall, label = "pass", "Robust & Deployable"
    elif "incomplete" in states:
        overall, label = "incomplete", "Verdict incomplete"
    else:
        overall, label = "in_progress", "Validation in progress"

    return {
        "schema": COCKPIT_VERDICT_SCHEMA,
        "authority": VERDICT_AUTHORITY,
        "detail": "Verdict computed by TraderCockpit over exact native SQX trade records; StrategyQuant X owns the backtest, the cockpit owns the verdict.",
        "policy": effective,
        "initial_capital": initial_capital,
        "initial_capital_source": capital_source,
        "native_conditions": {"state": native_conditions_state, "detail": native_conditions_detail},
        "statistics": {
            "full": full_stats,
            "in_sample": sqx_statistics(is_rows, **stats_kw) if is_rows else None,
            "out_of_sample": sqx_statistics(oos_rows, **stats_kw) if oos_rows else None,
            "higher_precision": sqx_statistics(higher_precision_trades, **stats_kw) if higher_precision_trades is not None else None,
            "additional_markets": sqx_statistics(additional_market_trades, **stats_kw) if additional_market_trades is not None else None,
        },
        "periods": {"years": years_all, "quarters": quarters},
        "monte_carlo": monte_carlo,
        "equity": equity_points(historical_trades, initial_capital=initial_capital),
        "stages": stages,
        "verdict": {
            "state": overall,
            "label": label,
            "stages_passed": sum(1 for state in states if state == "pass"),
            "stages_total": len(stages),
        },
    }
