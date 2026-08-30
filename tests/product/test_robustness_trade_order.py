import unittest

from tradercockpit.robustness_trade_order import (
    SQX_RANDOMIZE_TRADES_ORDER_CLASS,
    TradeOrderRandomizationError,
    apply_randomize_trades_order,
)


class ScriptedRng:
    def __init__(self, values=()):
        self._values = iter(values)
        self.bounds = []

    def next_int(self, bound):
        self.bounds.append(bound)
        return next(self._values)


class RobustnessTradeOrderTests(unittest.TestCase):
    def test_reconstruction_target_keeps_sqx_method_identifier(self):
        self.assertEqual(SQX_RANDOMIZE_TRADES_ORDER_CLASS, "RandomizeTradesOrder")

    def test_scripted_draws_produce_deterministic_permutation(self):
        trades = ["a", "b", "c", "d"]
        rng = ScriptedRng([1, 0, 1])

        apply_randomize_trades_order(trades, rng)

        self.assertEqual(trades, ["c", "d", "a", "b"])
        self.assertEqual(rng.bounds, [4, 3, 2])

    def test_same_input_and_draws_reproduce_same_order(self):
        left = [1, 2, 3, 4, 5]
        right = list(left)

        apply_randomize_trades_order(left, ScriptedRng([0, 2, 1, 0]))
        apply_randomize_trades_order(right, ScriptedRng([0, 2, 1, 0]))

        self.assertEqual(left, right)

    def test_preserves_length_and_exact_object_identities(self):
        first = ["same"]
        second = ["same"]
        third = ["same"]
        trades = [first, second, third]
        before_ids = {id(trade) for trade in trades}

        apply_randomize_trades_order(trades, ScriptedRng([0, 0]))

        self.assertEqual(len(trades), 3)
        self.assertEqual({id(trade) for trade in trades}, before_ids)

    def test_empty_and_single_trade_sequences_do_not_consume_randomness(self):
        for initial in ([], ["only"]):
            with self.subTest(initial=initial):
                trades = list(initial)
                rng = ScriptedRng()

                apply_randomize_trades_order(trades, rng)

                self.assertEqual(trades, initial)
                self.assertEqual(rng.bounds, [])

    def test_lower_and_upper_valid_bounded_draws_are_accepted(self):
        trades = ["a", "b", "c"]
        rng = ScriptedRng([0, 1])

        apply_randomize_trades_order(trades, rng)

        self.assertEqual(trades, ["c", "b", "a"])
        self.assertEqual(rng.bounds, [3, 2])

    def test_identity_permutation_is_valid_when_rng_selects_current_tail(self):
        trades = ["a", "b", "c", "d"]

        apply_randomize_trades_order(trades, ScriptedRng([3, 2, 1]))

        self.assertEqual(trades, ["a", "b", "c", "d"])

    def test_invalid_rng_values_fail_closed_without_partial_reordering(self):
        invalid_cases = (
            ([0, -1], -1),
            ([0, 3], 3),
            ([0, True], True),
            ([0, 1.0], 1.0),
        )
        for draws, invalid in invalid_cases:
            with self.subTest(invalid=invalid):
                trades = ["a", "b", "c", "d"]
                original = list(trades)

                with self.assertRaises(TradeOrderRandomizationError):
                    apply_randomize_trades_order(trades, ScriptedRng(draws))

                self.assertEqual(trades, original)


if __name__ == "__main__":
    unittest.main()
