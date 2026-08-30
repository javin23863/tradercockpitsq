import unittest

from tradercockpit.robustness import (
    RandomlySkipTradesConfig,
    SystemParameterPermutationSettings,
    apply_randomize_trades_order,
    apply_randomly_skip_trades,
)


class SequenceRng:
    def __init__(self, values):
        self.values = iter(values)
        self.bounds = []

    def next_int(self, bound):
        self.bounds.append(bound)
        return next(self.values)


class RobustnessIngredientTests(unittest.TestCase):
    def test_skip_uses_native_rounding_and_shrinking_bounds(self):
        trades = list(range(50))
        rng = SequenceRng([0] * 15)
        apply_randomly_skip_trades(trades, RandomlySkipTradesConfig(29), rng)
        self.assertEqual(len(trades), 35)
        self.assertEqual(rng.bounds, list(range(50, 35, -1)))

    def test_randomize_order_is_exact_permutation_and_atomic_draw_contract(self):
        trades = ["a", "b", "c", "d"]
        rng = SequenceRng([0, 1, 0])
        apply_randomize_trades_order(trades, rng)
        self.assertEqual(sorted(trades), ["a", "b", "c", "d"])
        self.assertEqual(rng.bounds, [4, 3, 2])

    def test_system_parameter_settings_preserve_native_field_names(self):
        settings = SystemParameterPermutationSettings(
            max_tests=7,
            optim_periods=True,
            optim_exit_types=False,
            enabled=True,
        )
        self.assertEqual(
            settings.as_sqx_settings(),
            {
                "profile": "OptProfileSysParamPermutation",
                "use": True,
                "Settings": {
                    "OptimPeriods": True,
                    "OptimExitTypes": False,
                    "MaxTests": 7,
                },
            },
        )
        self.assertFalse(settings.hidden_execution_semantics_recovered)


if __name__ == "__main__":
    unittest.main()
