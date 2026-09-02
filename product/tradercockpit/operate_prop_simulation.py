"""Typed fail-closed paper/prop-firm simulation read model.

Historical research never masquerades as simulation-account truth. This record stays explicit
``unavailable`` until a real simulation-account producer exists.
"""

from __future__ import annotations

PROP_SIMULATION_SCHEMA = "tc.prop-simulation.v1"


def prop_simulation_record() -> dict[str, object]:
    return {
        "schema": PROP_SIMULATION_SCHEMA,
        "scope": "simulation_current",
        "historical_fallback": False,
        "producer": None,
        "detail": (
            "Prop-firm / paper simulation is part of Delivery / Simulation after Proof. "
            "Historical backtest statistics are never shown as simulation balance, P&L, or challenge progress."
        ),
        "status": "unavailable",
        "reason_code": "simulation_account_not_connected",
        "account": None,
        "metrics": None,
        "challenge": None,
    }
