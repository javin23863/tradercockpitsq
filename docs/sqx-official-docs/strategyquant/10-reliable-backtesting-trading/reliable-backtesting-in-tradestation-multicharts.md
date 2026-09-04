# Reliable backtesting in Tradestation / MultiCharts

- Source: <https://strategyquant.com/doc/strategyquant/reliable-backtesting-in-tradestation-multicharts/>
- Section: StrategyQuant X › Reliable backtesting & trading
- Fetched: 2026-09-04

---

## Reliability of backtesting in general

First of all, we have to realize that backtesting means testing the strategy on history data. The exact flow of ticks will never repeat in the future, so even if you are using real tick data it doesnt mean that your strategy will behave as same in the future as it behaved in the past.

Secondly, we should realize that backtest cannot be 100% accurate. At best, backtesting offers a close approximation of how trades would be executed in real-time. There are things like widened spreads, requotes, slippage, time delays, network disconnects, VPS failures and so on that affect real live trading.

**The most important property of our strategy should be its robustness.**

We have to make sure we didn’t just curve-fitted our strategy on the existing data so that it works great in our backtests; that it remains profitable despite changes in data, in parameters, when it misses few trades, etc.

StrategyQuant offers  lot of tools (Cross checks) to test strategy for robustness. You can test t on different markets, with variation of parameters, or with variation of random changes in history data using Monte Carlo tests.

## Reliable backtesting between SQ and TS / MC

Reliable backtesting in this sense means that the strategy wil have the same or very similar backtest results in StrategyQuant and Tradestation/ MultiCharts.

If your strategy has totally different results in SQ and in TS/MC, then there is something wrong with your setup and you have to solve it before moving forward.

Backtesting engine in SQ was made to match TS/MC trading engines, so if you see the differences it is most likely in different data or configuration between both programs.

Below we’ll list a few points you should watch for.

### 1. Make sure that you have all the custom indicators imported

This is one of the post-installation steps: <https://strategyquant.com/doc/strategyquant/installation/#steps-after-installation>  
SQ uses some custom indicators and you have to import them to Tradestation / MultiCharts to make it work.

### 2. Use the correct settings and data

Difference in backtests is mostly caused by problem in this area.

There are two ways you can use the data from your TS / MC in SQ:

**Option 1 – Import the exact data from the chart**  
This is most reliable way – export the data directly from your chart in TS / MC and import them into SQ.

In Multicharts – use File -> Export Data  
In Tradestation – open Data Window and use Save functionality there.

This way you can be sure you are testing it on the same data with your actual sessions and other configuration.

So for example, when you test your strategy in TS/MC for example on 15 min chart with some session then export this exact 15 min chart data with the session applied from the trading platform and import it to SQ.

**Important** **– if you use this way you HAVE TO set Session = No Session in Trading options**. It is because the data already include the session, otherwise SQ will try to apply the session again and the result will be incorrect data!

**Option 2 – Import minute data and let SQ compute higher timeframes**

It is more convenient to import minute data from TS, and let SQ compute higher timeframes for any backtest you make.

**Important** **– in this case make sure you use the correct session in SQ, it has to be exactly as same as in Tradestation / Multicharts**. Bars in TS / MC are computed based on this session and if you have it wrong, the bars will be wrongly computed!

If you’ll experience differences in backtest fall back to the previous option and test it on the data exported from chart.

There is a new article describing how to correctly [export the data form Tradestation](exporting-data-from-tradestation-recommendation.md).

### 3. Make sure you use the correct engine and correct bar type in imported data

Choice of engine is obvious – double check that you really use Tradestation or MultiCharts engine in SQ, as it offers a choice of engines.

When you import data form file make sure you use “**Timestamp is end of bar time**” bar data type. This is the data type used by MetaTrader, and it influences how the higher timeframes are computed.

[![](https://strategyquant.com/wp-content/uploads/2019/04/bartype_ts.jpg)](https://strategyquant.com/wp-content/uploads/2019/04/bartype_ts.jpg)

### 4. Use correct time-based Exit trading options

This applies if you have any of the time-based trading options turned on – for example **Exit At End Of Day**, or **Exit On Friday**.

When using any of these two options you have to either set their exit time to 00:00 – SQ will then close trades at the end of day session – or, if you specify exact time you have to make sure it is the time of an existing bar BEFORE the very last bar of the day.

Misconfiguration in this could cause SQ to close trades at the end of day in a different time than the TS / MC platform.

If you’ll experience difference in strategies compare the closing times of trades closed at the end of day between SQ and TS/MC if it behaves the same.

### 5. Restart SQ and clear temporary files when you experience problems

SQ is caching the backtest data in both memory and files on disk, and it could sometimes happen that the data in cache are older (wrong) version and were not updated when you import new ones or modify something.

If you’ll experience differences in backtest try first to exit SQ, delete all files in /internal/testfiles folder and start SQ again.

### 6. Subcharts support is still in development

We focused on single-chart strategies in the implementation of Tradestation / MultiCharts testing engine.  
Strategies that use multiple charts (Data2, Data3 etc. in EasyLanguage) weren’t yet tested in StrategyQuant for match – they might and might not work.  
We will be focusing on this in the next builds.

### 7. Higher testing precision is still in the development

Right now, it is possible to use Tradestation / MultiCharts testing engine with ‘Selected Timeframe’ testing precision.  This is sufficient for all types of orders – market, stop, limit. Tradestation / MultiCharts work reliably with this kind of test precision, and it can be used in live trading.

We are working on adding also better precision modes, they will be added in one of the future builds.

### 8. There are few indicators that haven’t been extensively tested in TS/MC

A very few indicators haven’t been tested in all their possible configurations, and they could cause differences in trading. They are:  
– Pivots  
– Fibo
