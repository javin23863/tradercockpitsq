"""TraderCockpit-owned reconstruction of SQX trade-order randomization."""
from __future__ import annotations
from typing import MutableSequence, Protocol, TypeVar

TradeT = TypeVar("TradeT")
SQX_RANDOMIZE_TRADES_ORDER_CLASS = "RandomizeTradesOrder"

class TradeOrderRandomizationError(ValueError):
    pass

class BoundedIndexSource(Protocol):
    def next_int(self, bound: int, /) -> int: ...

def apply_randomize_trades_order(trades: MutableSequence[TradeT], rng: BoundedIndexSource) -> None:
    draws: list[int] = []
    for bound in range(len(trades), 1, -1):
        index = rng.next_int(bound)
        if type(index) is not int or not 0 <= index < bound:
            raise TradeOrderRandomizationError(f"bounded index source returned {index!r} for bound {bound}")
        draws.append(index)
    for end, index in zip(range(len(trades) - 1, 0, -1), draws, strict=True):
        if index != end:
            trades[end], trades[index] = trades[index], trades[end]
