# Settings – Trading options

- Source: <https://strategyquant.com/doc/strategyquant/trading-options/>
- Section: StrategyQuant X › Program screens
- Fetched: 2026-09-04

---

These settings allow you to specify the properties of the generated strategies as well as testing conditions.

Trading options govern behavior of the strategy in backtesting engine – for example whether all orders should be closed at the end of day, etc.

[![StrategyQuant Trading options config](https://strategyquant.com/wp-content/uploads/2019/01/trading_options-1.png)](https://strategyquant.com/wp-content/uploads/2019/01/trading_options-1.png)

### Exit At End Of Day, Exit On Friday

if selected, the strategy will close all position at the end of the day or end of the week (Friday) at the specified time. This way you’ll have no position open overnight or during the weekend.

### Limit Time Range, Time Range From, Time Range To, Exit At End Of Range

this limits the hours the strategy is checking for entry signal to a given time range.

If used in combination with Exit At End Of Range then all open positions are closed at the end of the range.

If you don’t check the Exit At End Of Range then the strategy will not open new trades outside the trading range, but the already opened positions will be not closed.

### Maximum Trades Per Day

you can limit maximum trades the strategy takes per day

### Minimum / Maximum SL & PT

allows you to specify minimum and maximum Stop Loss and Profit Target. 0 means unlimited.

When you limit your SL or PT, then whatever value is computed using the strategy, it will be cut to not exceed the given range.

### Session

Configures trading session for futures / equities intraday trading. Sessions themselves can be created and managed in **Data manager**.

### Reserved bars

Bars to reserve before starting trading. This number should be higher than period of any indicator used.

### Realistic gaps handling

Only for MT4 – backtester in MetaTrader4 handles gaps in a way that is different from real trading. When there is a gap, it fills Stop/Limit order at its desired open price, instead of at price after the gap.

In real live trading the order is fillled at price after the gap.

Note that by turning this on your backtest in SQ might differ from backtest in MT4, but it will be more realistic.

[![StrategyQuant realistic gaps handling](https://strategyquant.com/wp-content/uploads/2019/01/realistic_gaps-1024x624.png)](https://strategyquant.com/wp-content/uploads/2019/01/realistic_gaps-1024x624.png)

### Store Chart Data

if checked SQ X will save also complete chart data for the backtest, so you will be able to see history chart with backtest with used all indicators and all orders drawn on chart. This can help you review the strategy and see visually how it is trading.

When this option is turned on, you will be able to see the chart in **Results** -> **Trades on chart** tab:

[![Strategyquant trades on chart](https://strategyquant.com/wp-content/uploads/2019/01/trades_on_chart.png)](https://strategyquant.com/wp-content/uploads/2019/01/trades_on_chart.png)
