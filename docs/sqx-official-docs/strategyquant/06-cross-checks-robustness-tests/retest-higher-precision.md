# Retest with higher precision

- Source: <https://strategyquant.com/doc/strategyquant/retest-higher-precision/>
- Section: StrategyQuant X › Cross checks - robustness tests
- Fetched: 2026-09-04

---

This test is simple – it backtests the strategy again on the same data, but with higher precision.

It is usually best to make the main test on the fastest Selected timeframe precision, because it can very quickly filter out bad strategies – these that produce no trades or whose Net profit is negative.

Once we have strategy that is profitable, it can be automatically retested with higher precision using this cross check – ensuring that the strategy works also when tested with better precision.

It can happen that strategy that is profitable with Selected timeframe precision ceases to be profitable when retested with higher precision – it is because the lower precision simply is less exact.
