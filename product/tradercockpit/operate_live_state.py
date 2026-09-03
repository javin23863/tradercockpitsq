"""Typed fail-closed live Operate read models for signals, risk, and scoped performance.

Historical research never masquerades as live/current truth. These records stay explicit
``unavailable`` until a real execution or account producer exists.
"""

from __future__ import annotations

LIVE_SIGNALS_SCHEMA = "tc.live-signals.v1"
LIVE_RISK_SCHEMA = "tc.live-risk.v1"
SCOPED_PERFORMANCE_SCHEMA = "tc.scoped-performance.v1"
LIVE_DEPLOYMENT_SCHEMA = "tc.live-deployment.v1"


def _base(scope_detail: str) -> dict[str, object]:
    return {
        "scope": "live_current",
        "historical_fallback": False,
        "producer": None,
        "detail": scope_detail,
    }


def live_signals_record() -> dict[str, object]:
    return {
        "schema": LIVE_SIGNALS_SCHEMA,
        **_base(
            "Live strategy/deployment signals require a connected execution producer. "
            "Historical native signal blocks are never shown as live signals."
        ),
        "status": "unavailable",
        "reason_code": "deployment_not_connected",
        "signals": [],
    }


def live_risk_record(account: dict[str, object] | None = None) -> dict[str, object]:
    if not isinstance(account, dict) or not account:
        return {
            "schema": LIVE_RISK_SCHEMA,
            **_base(
                "Account risk limits and exposure require a connected broker/account producer. "
                "Historical backtest drawdown is never shown as live risk."
            ),
            "status": "unavailable",
            "reason_code": "account_not_connected",
            "limits": None,
        }
    return {
        "schema": LIVE_RISK_SCHEMA,
        "scope": "live_current",
        "historical_fallback": False,
        "producer": {"id": "metatrader5"},
        "detail": "Live account snapshot from the connected MetaTrader 5 terminal.",
        "status": "current",
        "reason_code": None,
        "limits": {
            "balance": account.get("balance"),
            "equity": account.get("equity"),
            "margin": account.get("margin"),
            "margin_free": account.get("margin_free"),
            "profit": account.get("profit"),
            "currency": account.get("currency"),
        },
    }


def scoped_performance_record() -> dict[str, object]:
    return {
        "schema": SCOPED_PERFORMANCE_SCHEMA,
        **_base(
            "Scoped live/current performance requires a connected execution producer. "
            "Historical backtest statistics are never shown as live P&L or drawdown."
        ),
        "status": "unavailable",
        "reason_code": "deployment_not_connected",
        "metrics": None,
    }


def live_deployment_record() -> dict[str, object]:
    return {
        "schema": LIVE_DEPLOYMENT_SCHEMA,
        **_base(
            "Deployment custody records exported identities only. No broker connection, "
            "fills, positions, or P&L are claimed until a real execution producer exists."
        ),
        "status": "unavailable",
        "reason_code": "execution_not_connected",
    }
