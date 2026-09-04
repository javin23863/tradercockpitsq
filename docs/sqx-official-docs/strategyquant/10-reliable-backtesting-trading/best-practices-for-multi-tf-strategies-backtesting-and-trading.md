# Best practices for multi-TF strategies backtesting and trading

- Source: <https://strategyquant.com/doc/strategyquant/best-practices-for-multi-tf-strategies-backtesting-and-trading/>
- Section: StrategyQuant X › Reliable backtesting & trading
- Fetched: 2026-09-04

---

Having one strategy trading on multiple timeframes can be sometimes tricky, there are some best practices to follow, especially on Tradestation and MultiCharts:

## **Make sure you have the setup right before starting long-time-running generation**

**Make sure that you have the same configuration (data, sessions!, commissions etc.)** **in both StrategyQuant and your trading platform.** This is one of the most common issues when the backtests / trading differs.

Generate just one-two strategies and compare its backtests between SQ and you trading platform. If they don’t match look for the differences in configuration.

Start the real generation only after there is no difference in backtests with your given setting (data + sessions).

**Recheck this again when you change symbols or sessions.**

## Always use the lowest timeframe on main chart and higher timeframes on additional charts

It is because otherwise the study is called multiple times on each bar (depending on the lowest TF subchart) and it causes problems.

## Use smaller periods, especially on higher timeframes

Some trading platforms require to reserve enough bars before they start trading, and number of bars reserved should be equal or higher to the biggest period used in any of your indicators.

If you use indicator with period 300 (for example) on a daily chart  it means 300 bars (= 300 days) have to be reserved before the strategy starts trading – that is a whole year !

Another problem is indicators “cool off” – each platform handles the first “reserved” bars differently and there is usually some cooling off period until the indicator values will be exactly matching between SQ and the platform. The longer period you use the longer this period is. It is advantageous to use as small periods as possible, and this is especially important for bigger timeframes, such as daily.

## Be careful of sessions when using Daily timeframes in Tradestation

Daily data (for example ES.D) have their own session hours which might not be configurable. Make sure your session for daily data in SQ matches the one from Tradestation.

## Ideally avoid some building blocks and indicators for sub-charts

1. **Avoid using Volume and AverageVolume blocks in strategies**. It is too data/broker sensitive, different brokers can have different volumes . This is not related only to multi-TF strategies
2. **Be careful when using Fractals indicator.** It outputs zero values which is not good for open price and SL/PT calculations. Fractals indicator is best used for entry conditions, not for computing Stop/Limit price levels.
3. **Is rising / Is falling is currently not working well with Daily/Weekly/Monthly blocks in EasyLanguage** source code. It doesn’t apply shift correctly. We will focus on this in further release.
