"""Mandatory trader-facing Expected value and Sharpe from native trade P&L.

Formulas are the Quant-Guild performance-metrics contract: EV is mean(PL) and
must be replicable from p_win, avg_win, and avg_loss; Sharpe is mean(r) / sample
stdev (ddof=1). Keys stay present when a value cannot be formed.
"""

from __future__ import annotations

from statistics import fmean, stdev
from typing import Iterable

_IDENTITY_EPS = 1e-6


def _floats(values: Iterable[object]) -> list[float]:
    rows: list[float] = []
    for value in values:
        try:
            rows.append(float(value))
        except (TypeError, ValueError) as exc:
            raise ValueError("trade P&L series contains a non-numeric value") from exc
    return rows


def expected_value_record(
    pnl: Iterable[object],
    *,
    window: str = "full",
) -> dict[str, object]:
    """Per-trade expected value from a producer P&L sample."""

    series = _floats(pnl)
    n = len(series)
    if n < 1:
        return {
            "status": "unavailable",
            "reason_code": "trades_missing",
            "n": 0,
            "n_win": None,
            "p_win": None,
            "avg_win": None,
            "avg_loss": None,
            "expected_value": None,
            "mean_pl": None,
            "identity_ok": None,
            "window": window,
        }
    wins = [value for value in series if value > 0]
    losses = [value for value in series if value <= 0]
    n_win = len(wins)
    p_win = n_win / n
    avg_win = fmean(wins) if wins else None
    avg_loss = fmean(losses) if losses else None
    mean_pl = fmean(series)
    if avg_win is None:
        expected_value = (1.0 - p_win) * float(avg_loss)
    elif avg_loss is None:
        expected_value = p_win * avg_win
    else:
        expected_value = p_win * avg_win + (1.0 - p_win) * avg_loss
    return {
        "status": "available",
        "reason_code": None,
        "n": n,
        "n_win": n_win,
        "p_win": p_win,
        "avg_win": avg_win,
        "avg_loss": avg_loss,
        "expected_value": expected_value,
        "mean_pl": mean_pl,
        "identity_ok": abs(expected_value - mean_pl) <= _IDENTITY_EPS,
        "window": window,
    }


def sharpe_record(
    returns: Iterable[object],
    *,
    window: str = "full",
    scale: float = 1.0,
    risk_free: float | None = None,
) -> dict[str, object]:
    """Per-trade Sharpe on a P&L or return sample (excess over 0 unless connected)."""

    series = _floats(returns)
    n = len(series)
    if scale == 0:
        raise ValueError("Sharpe scale must be non-zero")
    scaled = [value / scale for value in series]
    if n < 1:
        return {
            "status": "unavailable",
            "reason_code": "trades_missing",
            "sharpe": None,
            "n": 0,
            "mean_return": None,
            "stdev_return": None,
            "ddof": 1,
            "risk_free": risk_free,
            "scale": scale,
            "window": window,
        }
    mean_return = fmean(scaled)
    if n < 2:
        return {
            "status": "unavailable",
            "reason_code": "sharpe_undefined",
            "sharpe": None,
            "n": n,
            "mean_return": mean_return,
            "stdev_return": None,
            "ddof": 1,
            "risk_free": risk_free,
            "scale": scale,
            "window": window,
        }
    stdev_return = stdev(scaled)
    if stdev_return == 0:
        return {
            "status": "unavailable",
            "reason_code": "sharpe_undefined",
            "sharpe": None,
            "n": n,
            "mean_return": mean_return,
            "stdev_return": stdev_return,
            "ddof": 1,
            "risk_free": risk_free,
            "scale": scale,
            "window": window,
        }
    excess = mean_return if risk_free is None else mean_return - risk_free
    return {
        "status": "available",
        "reason_code": None,
        "sharpe": excess / stdev_return,
        "n": n,
        "mean_return": mean_return,
        "stdev_return": stdev_return,
        "ddof": 1,
        "risk_free": risk_free,
        "scale": scale,
        "window": window,
    }
