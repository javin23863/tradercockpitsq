from __future__ import annotations

import unittest

from tradercockpit.trade_metrics import expected_value_record, sharpe_record


class ExpectedValueTests(unittest.TestCase):
    def test_empty_series_keeps_the_key_unavailable(self) -> None:
        record = expected_value_record([])
        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "trades_missing")
        self.assertEqual(record["n"], 0)
        self.assertIsNone(record["expected_value"])

    def test_ev_equals_mean_pl_and_is_replicable(self) -> None:
        pnl = [80.0, -40.0, 20.0, -10.0]
        record = expected_value_record(pnl)
        self.assertEqual(record["status"], "available")
        self.assertEqual(record["n"], 4)
        self.assertEqual(record["n_win"], 2)
        self.assertEqual(record["p_win"], 0.5)
        self.assertEqual(record["avg_win"], 50.0)
        self.assertEqual(record["avg_loss"], -25.0)
        self.assertEqual(record["expected_value"], 12.5)
        self.assertEqual(record["mean_pl"], 12.5)
        self.assertTrue(record["identity_ok"])
        self.assertEqual(record["window"], "full")
        reconstructed = record["p_win"] * record["avg_win"] + (1.0 - record["p_win"]) * record["avg_loss"]
        self.assertEqual(reconstructed, record["expected_value"])

    def test_scratches_count_as_losses(self) -> None:
        record = expected_value_record([10.0, 0.0, -5.0])
        self.assertEqual(record["n_win"], 1)
        self.assertEqual(record["avg_loss"], -2.5)
        self.assertTrue(record["identity_ok"])


class SharpeTests(unittest.TestCase):
    def test_single_observation_is_undefined(self) -> None:
        record = sharpe_record([4.0])
        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "sharpe_undefined")
        self.assertEqual(record["n"], 1)
        self.assertEqual(record["mean_return"], 4.0)
        self.assertIsNone(record["sharpe"])
        self.assertEqual(record["ddof"], 1)

    def test_zero_stdev_is_undefined(self) -> None:
        record = sharpe_record([2.0, 2.0, 2.0])
        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "sharpe_undefined")
        self.assertEqual(record["stdev_return"], 0.0)
        self.assertIsNone(record["sharpe"])

    def test_sample_sharpe_is_replicable(self) -> None:
        returns = [2.0, -1.0, 3.0, 0.0]
        record = sharpe_record(returns)
        mean_return = sum(returns) / 4
        variance = sum((value - mean_return) ** 2 for value in returns) / 3
        stdev_return = variance ** 0.5
        self.assertEqual(record["status"], "available")
        self.assertEqual(record["n"], 4)
        self.assertEqual(record["mean_return"], mean_return)
        self.assertAlmostEqual(record["stdev_return"], stdev_return)
        self.assertAlmostEqual(record["sharpe"], mean_return / stdev_return)
        self.assertIsNone(record["risk_free"])
        reconstructed = record["mean_return"] / record["stdev_return"]
        self.assertAlmostEqual(reconstructed, record["sharpe"])


if __name__ == "__main__":
    unittest.main()
