"""Source-proven StrategyQuant X Monte Carlo trade manipulation."""
from __future__ import annotations
from dataclasses import dataclass
from math import floor
from typing import MutableSequence, Protocol, TypeVar

TradeT = TypeVar("TradeT")
SQX_RANDOMLY_SKIP_TRADES_CLASS = "RandomlySkipTrades"
SQX_RANDOMLY_SKIP_TRADES_NAME = "Randomly skip trades"

class RobustnessConfigError(ValueError):
    """Raised when a robustness configuration contract is violated."""

class BoundedIndexSource(Protocol):
    def next_int(self, bound: int, /) -> int: ...

@dataclass(frozen=True, slots=True)
class RandomlySkipTradesConfig:
    probability_pct: int = 10
    def __post_init__(self) -> None:
        if type(self.probability_pct) is not int or not 1 <= self.probability_pct <= 100:
            raise RobustnessConfigError("skip-trades probability must be an integer from 1 to 100")

def _java_round_nonnegative(value: float) -> int:
    return floor(value + 0.5)

def apply_randomly_skip_trades(trades: MutableSequence[TradeT], config: RandomlySkipTradesConfig, rng: BoundedIndexSource) -> None:
    removal_count = _java_round_nonnegative((len(trades) * config.probability_pct) / 100.0)
    for _ in range(removal_count):
        size = len(trades)
        if size == 0:
            break
        index = rng.next_int(size)
        if type(index) is not int or not 0 <= index < size:
            raise RobustnessConfigError(f"bounded index source returned {index!r} for bound {size}")
        del trades[index]
