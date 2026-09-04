# Retest on additional markets

- Source: <https://strategyquant.com/doc/strategyquant/retest-additional-markets/>
- Section: StrategyQuant X › Cross checks - robustness tests
- Fetched: 2026-09-04

---

This test for robustness is quite though – it means testing the same strategy on different markets – it means different bol(s) and/or another timeframe(s). Robust strategy should ideally work on multiple symbols/timeframes.

In reality, because each market has its own characteristics, daily volatility, etc., it will be not easy to find a strategy that has the same perfect performance on multiple symbols using just one set of settings.

We can be satisfied if the strategy performs on other markets with at least some degree of profitability, or just slightly losing.

[![](https://strategyquant.com/wp-content/uploads/2019/03/retest_on_a_different_market1.png)](https://strategyquant.com/wp-content/uploads/2019/03/retest_on_a_different_market1.png)

[![](https://strategyquant.com/wp-content/uploads/2019/03/retest_on_a_different_market2.png)](https://strategyquant.com/wp-content/uploads/2019/03/retest_on_a_different_market2.png)In the two charts above you can see test of strategy on EURUSD (red line), GBPUSD (orange line) and portfolio of both (blue line).

While on the left chart the strategy performs well on both currencies, on the right chart you can see that performance on GBPUSD is bad. This strategy is probably not robust enough.
