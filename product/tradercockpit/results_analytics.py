"""Read-only presentation aggregates over recorded native filled trades."""
from datetime import datetime, timezone
from math import isfinite

from .research_verdicts import select_sample, sqx_statistics, equity_points, _drawdown_walk, round2

FILTERS = {"sample": ("full", "is", "oos"), "direction": ("both", "long", "short"), "period_by": ("close_time", "open_time")}
FIELDS = ("NetProfit", "ProfitFactor", "NumberOfTrades", "WinningPct", "Drawdown", "ReturnDDRatio", "NumberOfProfits", "NumberOfLosses", "GrossProfit", "GrossLoss")


def analytics(trades, capital, *, sample="full", direction="both", period_by="close_time"):
    for key, value in (("sample", sample), ("direction", direction), ("period_by", period_by)):
        if value not in FILTERS[key]:
            raise ValueError(f"Invalid Results {key}")
    rows = [t for t in select_sample(trades, {"full": 127, "is": 10, "oos": 20}[sample])
            if t.get("Type") in {"both": (1, 2, 9, 11), "long": (1, 9), "short": (2, 11)}[direction]]
    # Keep native list order: the existing SQX column formulas use this same order.
    def timestamp(t, key):
        value = t.get(key)
        if not isinstance(value, int) or isinstance(value, bool) or value <= 0:
            return None
        try:
            return datetime.fromtimestamp(value / 1000, timezone.utc)
        except (ValueError, OverflowError, OSError):
            return None

    def metrics(items):
        # Time-dependent columns are intentionally excluded when times are missing.
        normalized = [{**t, "OpenTime": t.get("OpenTime") if isinstance(t.get("OpenTime"), int) else 0,
                       "CloseTime": t.get("CloseTime") if isinstance(t.get("CloseTime"), int) else 0} for t in items]
        result = sqx_statistics(normalized, initial_capital=capital if capital is not None else 0)
        return {key: result[key] for key in FIELDS}

    groups = {key: {} for key in ("year", "month", "weekday", "hour")}
    missing_time = 0
    time_key = "OpenTime" if period_by == "open_time" else "CloseTime"
    for trade in rows:
        time = timestamp(trade, time_key)
        if time is None:
            missing_time += 1
            continue
        for key, label in (("year", str(time.year)), ("month", f"{time.month:02}"),
                           ("weekday", str(time.weekday())), ("hour", f"{time.hour:02}")):
            groups[key].setdefault(label, []).append(trade)
    periods = {key: [{"period": label, **metrics(items)} for label, items in sorted(group.items())]
               for key, group in groups.items()}
    sides = [{"period": label, **metrics([t for t in rows if t["Type"] in types])}
             for label, types in (("Long", (1, 9)), ("Short", (2, 11)))]
    symbols = [{"symbol": symbol, **metrics([t for t in rows if t.get("Symbol") == symbol])}
               for symbol in sorted({str(t["Symbol"]) for t in rows if t.get("Symbol")})]
    limits = [(300, "≤5m"), (900, "5–15m"), (3600, "15–60m"), (14400, "1–4h"),
              (86400, "4–24h"), (345600, "1–4d"), (1382400, "4–16d"), (float("inf"), ">16d")]
    durations = {label: 0 for _, label in limits}
    duration_points = []
    missing_duration = 0
    for trade in rows:
        opened, closed = timestamp(trade, "OpenTime"), timestamp(trade, "CloseTime")
        if opened is None or closed is None or closed < opened:
            missing_duration += 1
            continue
        seconds = (closed - opened).total_seconds()
        duration_points.append({"seconds": seconds, "pl": trade["PL"], "ticket": trade.get("Ticket")})
        for limit, label in limits:
            if seconds <= limit:
                durations[label] += 1
                break
    pls = [float(t["PL"]) for t in rows]
    distribution = []
    if pls:
        low, high = min(pls), max(pls)
        count = 1 if low == high else 12
        width = (high - low) / count if count > 1 else 1
        bins = [0] * count
        for value in pls:
            bins[min(count - 1, int((value - low) / width))] += 1
        distribution = [{"from": low + i * width, "to": high if i == count - 1 else low + (i + 1) * width, "count": n} for i, n in enumerate(bins)]
    valid_times = all(timestamp(t, "CloseTime") is not None for t in rows)
    normalized = [{**t, "CloseTime": t.get("CloseTime") if isinstance(t.get("CloseTime"), int) else 0} for t in rows]
    points = equity_points(normalized, initial_capital=capital if capital is not None else 0, limit=max(1, len(rows)))
    _, drawdowns, _ = _drawdown_walk(pls, capital if capital is not None else 0)
    for index, point in enumerate(points):
        point.update({"trade": index + 1, "drawdown": round2(drawdowns[index]),
                      "time": point["time"] if timestamp(rows[index], "CloseTime") else None})
    profile = [{"mae": t["MAE"], "mfe": t["MFE"], "pl": t["PL"]} for t in rows
               if all(isinstance(t.get(k), (int, float)) and isfinite(t[k]) for k in ("MAE", "MFE"))]
    times = [t["CloseTime"] for t in rows if timestamp(t, "CloseTime")]
    return {"sample": sample, "direction": direction, "period_by": period_by,
            "basis": "recorded_native_trades", "capital": capital, "metrics": metrics(rows),
            "trades": rows, "equity": points, "time_axis_available": valid_times,
            "range": [min(times), max(times)] if times else [], "periods": periods, "sides": sides,
            "distribution": distribution, "durations": [{"period": label, "count": count} for label, count in durations.items()],
            "duration_points": duration_points, "missing_time": missing_time, "missing_duration": missing_duration,
            "profile": profile, "symbols": symbols, "breakeven": sum(value == 0 for value in pls)}
