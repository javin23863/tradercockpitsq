# Reliable backtesting in MetaTrader

- Source: <https://strategyquant.com/doc/strategyquant/reliable-backtesting-in-metatrader/>
- Section: StrategyQuant X › Reliable backtesting & trading
- Fetched: 2026-09-04

---

## Reliability of backtesting in general

First of all, we have to realize that backtesting means testing the strategy on history data. The exact flow of ticks will never repeat in the future, so even if you are using real tick data it doesnt mean that your strategy will behave as same in the future as it behaved in the past.

Secondly, we should realize that backtest cannot be 100% accurate. At best, backtesting offers a close approximation of how trades would be executed in real-time. There are things like widened spreads, requotes, slippage, time delays, network disconnects, VPS failures and so on that affect real live trading.

**The most important property of our strategy should be its robustness.**

We have to make sure we didn’t just curve-fitted our strategy on the existing data so that it works great in our backtests; that it remains profitable despite changes in data, in parameters, when it misses few trades, etc.

StrategyQuant offers  lot of tools (Cross checks) to test strategy for robustness. You can test t on different markets, with variation of parameters, or with variation of random changes in history data using Monte Carlo tests.

## Reliable backtesting between SQ and MT4/5

Reliable backtesting in this sense means that the strategy wil have the same or very similar backtest results in StrategyQuant and MetaTrader.

If your strategy has totally different results in SQ and in MT, then there is something wrong with your setup and you have to solve it before moving forward.

Backtesting engine in SQ was made to match MetaTrader trading engine, so if you see the differences it is most likely in different data or configuration between both programs.

Below we’ll list a few points you should watch for.

### 1. Make sure that you have all the custom indicators imported

This is one of the post-installation steps: <https://strategyquant.com/doc/strategyquant/installation/#steps-after-installation>  
SQ uses some custom indicators and you have to import them to MetaTrader to make it work.

### 2. Import data from your MetaTrader, or ensure that the downloaded Dukascopy data are similar to your broker

SQ offers convenient download of high quality tick and M1 data provided for free by Dukascopy.  
The data itself are reliable, but make sure that you use the same timezone as your broker, expecially if you plan on using features like Exit at end of day / Friday.

The easiest way to verify that your setup is correct is to use the same data in SQ and MT, which means exporting your data from MT and importing it to SQ. Then check if strategy backtest matches on this data – it should. If it doesn’t, check the point 3.

Also, count with the fact that your broker will have slightly different data from Dukascopy and make sure your strategy is robust and can withstand it. Testing your strategy in SQ and then on different data in your MetaTrader is another form of robustness test.  
Just make sure that if there are big differences in data they are not caused you your config, but by real strategy failure.

### 3. Make sure you use the correct engine and correct bar type in imported data

Choice of engine is obvious – double check that you really use MetaTrader 4 or 5 engine in SQ, as it offers a choice of engines.

When you import data form file make sure you use “**Timestamp is start of bar time**” bar data type. This is the data type used by MetaTrader, and it influences how the higher timeframes are computed.

[![](https://strategyquant.com/wp-content/uploads/2019/04/bartype_forex.jpg)](https://strategyquant.com/wp-content/uploads/2019/04/bartype_forex.jpg)

### 4. Configure your MetaTrader StrategyTester properly

Make sure you use the **same spread setting, same date range, etc**. The point is to have the same configuration between SQ and MT.

You should also consider disconnecting MetaTrader from the network, because otherwise it will (by default) use current live spread in the backtest, which means your backtest might be little different every time you run it.

**How to disconnect MetaTrader4**  
First of all, MetaTrader must be online and connected at least for a while, so that it loads the actual spreads. Then you can disconnect it by setting proxy to some dummy value.

Go to Tools → Options, Server tab and check the box Enable proxy server.

[![](https://strategyquant.com/wp-content/uploads/2019/04/mt4_proxy.jpg)](https://strategyquant.com/wp-content/uploads/2019/04/mt4_proxy.jpg)

Then click on the Proxy … button to set up the proxy.

[![](https://strategyquant.com/wp-content/uploads/2019/04/mt4_proxy2.jpg)](https://strategyquant.com/wp-content/uploads/2019/04/mt4_proxy2.jpg)

Put localhost as a Server, and some dummy text into Login and Password. Close the dialog by clicking on OK and close the Options dialog by clicking on OK button again, this will save your settings.

Now you have to restart MetaTrader again and next time you’ll start it you’ll be disconnected from the broker.

[![](https://strategyquant.com/wp-content/uploads/2019/04/mt4_proxy3.jpg)](https://strategyquant.com/wp-content/uploads/2019/04/mt4_proxy3.jpg)

You should see No connection status on the bottom right corner of MetaTrader.

From now on all your tests will be run with the same spreads, and the results will be the same every time you run the test.

### 5. Restart SQ and clear temporary files when you experience problems

SQ is caching the backtest data in both memory and files on disk, and it could sometimes happen that the data in cache are older (wrong) version and were not updated when you import new ones or modify something.

If you’ll experience differences in backtest try first to exit SQ, delete all files in /internal/testfiles folder and start SQ again.
