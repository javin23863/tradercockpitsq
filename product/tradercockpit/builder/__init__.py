"""TraderCockpit-owned StrategyQuant X Builder backend contracts."""

from .evolution import (
    SQX_NATIVE_OPERATOR_PIPELINE,
    EvolutionConfig,
    EvolutionConfigError,
    EvolutionKernel,
    IslandPlan,
    SelectedParents,
    VariationDecision,
    VariationResult,
    decide_variation,
    plan_islands,
)

__all__ = [
    "SQX_NATIVE_OPERATOR_PIPELINE",
    "EvolutionConfig",
    "EvolutionConfigError",
    "EvolutionKernel",
    "IslandPlan",
    "SelectedParents",
    "VariationDecision",
    "VariationResult",
    "decide_variation",
    "plan_islands",
]
