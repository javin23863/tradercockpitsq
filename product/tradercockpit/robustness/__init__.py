"""StrategyQuant X-aligned robustness primitives."""

from .trade_manipulation import (
    BoundedIndexSource,
    RandomlySkipTradesConfig,
    RobustnessConfigError,
    SQX_RANDOMLY_SKIP_TRADES_CLASS,
    SQX_RANDOMLY_SKIP_TRADES_NAME,
    apply_randomly_skip_trades,
)

__all__ = [
    "BoundedIndexSource",
    "RandomlySkipTradesConfig",
    "RobustnessConfigError",
    "SQX_RANDOMLY_SKIP_TRADES_CLASS",
    "SQX_RANDOMLY_SKIP_TRADES_NAME",
    "apply_randomly_skip_trades",
]
