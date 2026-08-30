"""TraderCockpit reconstruction of SQX trade-order randomization.

The observable StrategyQuant X robustness contract is that this method
randomizes the order of an existing trade sequence. The retained screenshots,
configuration archives, and project evidence establish the capability surface,
but the exact SQX shuffle implementation has not been recovered.

Accordingly, the permutation loop below is a deliberately small
TraderCockpit-owned reconstruction, not a claim about SQX's hidden algorithm.
It preserves the observable contract without inventing additional controls.
"""

from __future__ import annotations

from typing import MutableSequence, Protocol, TypeVar

TradeT = TypeVar("TradeT")

SQX_RANDOMIZE_TRADES_ORDER_CLASS = "RandomizeTradesOrder"


class TradeOrderRandomizationError(ValueError):
    """Raised when the bounded-randomness contract is violated."""


class BoundedIndexSource(Protocol):
    """Minimal bounded-randomness boundary needed by this reconstruction."""

    def next_int(self, bound: int, /) -> int:
        """Return an integer in ``[0, bound)``."""


def apply_randomize_trades_order(
    trades: MutableSequence[TradeT],
    rng: BoundedIndexSource,
) -> None:
    """Randomize ``trades`` while preserving every trade exactly once.

    The observable SQX contract requires a permutation of the existing trade
    sequence; no exposed percentage or alternate mode is evidenced for this
    reconstruction, so none is invented here.

    Internally TraderCockpit uses a descending bounded-swap permutation
    (Fisher-Yates form). All draws are collected and validated before any swap
    is applied. That atomic validation is a TraderCockpit safety property: an
    invalid randomness provider cannot leave the caller with a partially
    reordered trade sequence.
    """

    draws: list[int] = []
    for bound in range(len(trades), 1, -1):
        index = rng.next_int(bound)
        if type(index) is not int or not 0 <= index < bound:
            raise TradeOrderRandomizationError(
                f"bounded index source returned {index!r} for bound {bound}"
            )
        draws.append(index)

    for end, index in zip(range(len(trades) - 1, 0, -1), draws, strict=True):
        if index != end:
            trades[end], trades[index] = trades[index], trades[end]
