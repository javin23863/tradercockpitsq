from __future__ import annotations

import unittest

from tradercockpit.robustness import (
    RandomlySkipTradesConfig,
    RobustnessConfigError,
    SQX_RANDOMLY_SKIP_TRADES_CLASS,
    SQX_RANDOMLY_SKIP_TRADES_NAME,
    apply_randomly_skip_trades,
)


class SequenceIndexSource:
    def __init__(self, indexes: list[int]) -> None:
        self._indexes = iter(indexes)
        self.bounds: list[int] = []

    def next_int(self, bound: int, /) -> int:
        self.bounds.append(bound)
        return next(self._indexes)


class RandomlySkipTradesTests(unittest.TestCase):
    def test_native_identity_and_parameter_default_are_preserved(self) -> None:
        config = RandomlySkipTradesConfig()

        self.assertEqual(SQX_RANDOMLY_SKIP_TRADES_CLASS, "RandomlySkipTrades")
        self.assertEqual(SQX_RANDOMLY_SKIP_TRADES_NAME, "Randomly skip trades")
        self.assertEqual(config.probability_pct, 10)

    def test_probability_must_match_native_integer_bounds(self) -> None:
        for value in (0, 101, -1, True, 10.5):
            with self.subTest(value=value), self.assertRaises(RobustnessConfigError):
                RandomlySkipTradesConfig(value)  # type: ignore[arg-type]

    def test_java_half_rounding_is_not_python_bankers_rounding(self) -> None:
        trades = ["a", "b", "c", "d", "e"]
        rng = SequenceIndexSource([2])

        result = apply_randomly_skip_trades(trades, RandomlySkipTradesConfig(10), rng)

        self.assertIsNone(result)
        self.assertEqual(trades, ["a", "b", "d", "e"])
        self.assertEqual(rng.bounds, [5])

    def test_each_draw_uses_current_shrinking_order_count(self) -> None:
        trades = ["a", "b", "c", "d", "e"]
        rng = SequenceIndexSource([4, 0, 1])

        apply_randomly_skip_trades(trades, RandomlySkipTradesConfig(60), rng)

        self.assertEqual(trades, ["b", "d"])
        self.assertEqual(rng.bounds, [5, 4, 3])

    def test_probability_100_removes_every_trade(self) -> None:
        trades = [1, 2, 3, 4]
        rng = SequenceIndexSource([0, 0, 0, 0])

        apply_randomly_skip_trades(trades, RandomlySkipTradesConfig(100), rng)

        self.assertEqual(trades, [])
        self.assertEqual(rng.bounds, [4, 3, 2, 1])

    def test_empty_orders_do_not_draw_random_index(self) -> None:
        trades: list[str] = []
        rng = SequenceIndexSource([])

        apply_randomly_skip_trades(trades, RandomlySkipTradesConfig(), rng)

        self.assertEqual(trades, [])
        self.assertEqual(rng.bounds, [])

    def test_invalid_bounded_index_fails_closed_before_removal(self) -> None:
        trades = ["a", "b", "c"]
        rng = SequenceIndexSource([3])

        with self.assertRaises(RobustnessConfigError):
            apply_randomly_skip_trades(trades, RandomlySkipTradesConfig(34), rng)

        self.assertEqual(trades, ["a", "b", "c"])


if __name__ == "__main__":
    unittest.main()
