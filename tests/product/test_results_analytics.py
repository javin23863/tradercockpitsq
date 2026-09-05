import unittest
from tradercockpit.results_analytics import analytics


def trade(pl, kind=1, sample=10, day=1):
    return {"PL": pl, "Type": kind, "SampleType": sample, "OpenTime": 1704067200000 + day * 86400000,
            "CloseTime": 1704070800000 + day * 86400000, "MAE": -10, "MFE": 20, "Ticket": day}


class ResultsAnalyticsTests(unittest.TestCase):
    def test_selection_drives_every_series_and_count(self):
        trades = [trade(100), trade(-150, day=2), trade(100, 2, 20, 3), trade(0, 2, 20, 4)]
        full = analytics(trades, 1000)
        self.assertEqual(full["metrics"]["NetProfit"], 50)
        self.assertEqual([p["drawdown"] for p in full["equity"]], [0, -150, -50, -50])
        self.assertEqual(full["metrics"]["Drawdown"], 150)
        self.assertEqual(sum(b["count"] for b in full["distribution"]), 4)
        self.assertEqual(sum(b["count"] for b in full["durations"]), 4)
        for key in ("year", "month", "weekday", "hour"):
            self.assertEqual(sum(p["NetProfit"] for p in full["periods"][key]), 50)
            self.assertEqual(sum(p["NumberOfTrades"] for p in full["periods"][key]), 4)
        oos = analytics(trades, 1000, sample="oos", direction="short", period_by="open_time")
        self.assertEqual(oos["metrics"]["NumberOfTrades"], 2)
        self.assertEqual(oos["metrics"]["NetProfit"], 100)
        self.assertEqual(len(oos["equity"]), 2)
        self.assertEqual(len(oos["profile"]), 2)
        self.assertEqual(oos["breakeven"], 1)
        empty = analytics(trades, None, sample="oos", direction="long")
        self.assertEqual(empty["trades"], [])
        self.assertEqual(empty["metrics"]["NumberOfTrades"], 0)
        self.assertEqual(empty["equity"], [])

    def test_missing_time_capital_and_recovery(self):
        rows = [trade(-10), trade(30, day=2)]
        rows[0]["CloseTime"] = None
        result = analytics(rows, None)
        self.assertIsNone(result["capital"])
        self.assertFalse(result["time_axis_available"])
        self.assertIsNone(result["equity"][0]["time"])
        self.assertEqual(result["missing_time"], 1)
        self.assertEqual(result["missing_duration"], 1)
        self.assertEqual(result["metrics"]["NetProfit"], 20)
        self.assertEqual([p["drawdown"] for p in result["equity"]], [-10, 0])
        self.assertEqual(result["periods"]["year"][0]["NetProfit"], 30)
        with self.assertRaises(ValueError):
            analytics([], 1000, sample="bad")

    def test_drawdown_uses_native_unrounded_walk(self):
        result = analytics([trade(0.004), trade(-0.008, day=2)], 1000)
        self.assertEqual(-min(p["drawdown"] for p in result["equity"]), result["metrics"]["Drawdown"])


if __name__ == "__main__":
    unittest.main()
