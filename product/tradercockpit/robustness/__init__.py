"""Canonical TraderCockpit robustness capability."""

from .service import (
    ROBUSTNESS_IMPLEMENTATION,
    ROBUSTNESS_IMPLEMENTATION_REVISION,
    ROBUSTNESS_RESULT_SCHEMA,
    FileRobustnessCatalog,
    ObservedTradeV1,
    RobustnessError,
    RobustnessExecutionUnavailable,
    RobustnessMetricGateV1,
    RobustnessPlanV1,
    RobustnessServiceV1,
    extract_observed_trades,
)
from .system_parameter_permutation import (
    SQX_SYSTEM_PARAMETER_PERMUTATION_PROFILE,
    SystemParameterPermutationError,
    SystemParameterPermutationSettings,
)
from .trade_manipulation import (
    SQX_RANDOMLY_SKIP_TRADES_CLASS,
    RandomlySkipTradesConfig,
    RobustnessConfigError,
    apply_randomly_skip_trades,
)
from .trade_order import (
    SQX_RANDOMIZE_TRADES_ORDER_CLASS,
    TradeOrderRandomizationError,
    apply_randomize_trades_order,
)

__all__ = [
    "FileRobustnessCatalog",
    "ObservedTradeV1",
    "ROBUSTNESS_IMPLEMENTATION",
    "ROBUSTNESS_IMPLEMENTATION_REVISION",
    "ROBUSTNESS_RESULT_SCHEMA",
    "RandomlySkipTradesConfig",
    "RobustnessConfigError",
    "RobustnessError",
    "RobustnessExecutionUnavailable",
    "RobustnessMetricGateV1",
    "RobustnessPlanV1",
    "RobustnessServiceV1",
    "SQX_RANDOMIZE_TRADES_ORDER_CLASS",
    "SQX_RANDOMLY_SKIP_TRADES_CLASS",
    "SQX_SYSTEM_PARAMETER_PERMUTATION_PROFILE",
    "SystemParameterPermutationError",
    "SystemParameterPermutationSettings",
    "TradeOrderRandomizationError",
    "apply_randomize_trades_order",
    "apply_randomly_skip_trades",
    "extract_observed_trades",
]
