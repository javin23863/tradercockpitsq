# Stockpicker Backtest Limitations

- Source: <https://strategyquant.com/doc/strategyquant/sp-backtest-limitations/>
- Section: StrategyQuant X › Reliable backtesting & trading
- Fetched: 2026-09-04

---

Stockpicking and generally trading strategies daily has some specifics that will be explained below.

## Strategy execution timing explained

The first thing is that you can choose time when strategy is triggered (evaluated) from three possible values:

- **Before Bar Open** – evaluates strategy before Market open (for daily strategies)
- **On Bar Open** – evaluates strategy on Market open
- **On Bar Close** – evaluates strategy on (shortly before) Market close

The choice of when the strategy is evaluated then affects which prices are available in both live trading and backtesting.

[![](https://strategyquant.com/wp-content/uploads/2023/01/timing2-750x362.jpg)](https://strategyquant.com/wp-content/uploads/2023/01/timing2-750x362.jpg)

We use parameter named Shift to denote the index of the value, for example Open[0] means Open price with Shift=0, and it means Open price of the current (most recent) bar.  
Open[1] means Open with Shift=1, which means one bar ago.

When you use blocks with Shift=0 (value of current bars) in your strategy you should be extra cautious, because they could mean different thing depending on when you evaluate the strategy.

Once again the three possible evaluation times and their implications:

#### **Before Bar Open**

evaluates strategy before Market open (for daily strategies).

[![](https://strategyquant.com/wp-content/uploads/2023/01/beforebaropen2-750x411.jpg)](https://strategyquant.com/wp-content/uploads/2023/01/beforebaropen2-750x411.jpg)

Platform triggers this a few minutes before actual market open, then the conditions are evaluated and orders are created.  
Trades are executed when market opens.

***Implications***  
Block with Shift=0 returns current value of last finished bar.  
So for example Close[0] returns Close price of the PREVIOUS finished market day, not the market day that will start in a while.  
Open[0] returns Open price of previous market day and so on.  
You can safely use Shift=0 in all the blocks, but they refer to the last closed bar – previoud market day.

#### **On Bar Open**

evaluates strategy on Market open.

[![](https://strategyquant.com/wp-content/uploads/2023/01/onbaropen-750x410.jpg)](https://strategyquant.com/wp-content/uploads/2023/01/onbaropen-750x410.jpg)

Platform triggers this a few minutes after the actual market open to allow for price consolidation.  
Trades are executed immediately.

**Implications**  
Block with Shift=0 returns value of actual open market day.  
So Open[0] returns current market Open price.  
Close[0] returns Open price, because current Close price of the day is not known yet (it is start of market day).  
The same for High[0], Low[0] – at market open the high and low of current trading day is not yet known.  
You can safely use Shift=0 ONLY with Open price blocks, and compute indicators only using Open price.

#### **On Bar Close**

evaluates strategy on Market close.

[![](https://strategyquant.com/wp-content/uploads/2023/01/onbarclose-750x411.jpg)](https://strategyquant.com/wp-content/uploads/2023/01/onbarclose-750x411.jpg)

Platform triggers this a few minutes BEFORE the actual market close to allow for correct executions.  
Trades are executed immediately.

**Implications**  
Block with Shift=0 returns values of actual closing market day.  
Open[0] returns current market Open price.  
Close[0] returns actual realtime price – very close to real Close price.  
You can safely use Shift=0 with all types of prices and indicators. Because the strategy is evaluated very close to actual market close  
the High, Low, Close prices of the current trading day will be very accurate.

#### **Special note about indicators**

the examples above show how simple prices like Open, High, Low, Close are affected when used with Shift=0,  
but you should realize that there are also indicators that can be computed from prices that are not yet known.  
For example, computing EMA from Close with Shift=0 will return incorrect results when used with On Bar Open trigger, because Close[0] values are not yet known then.  
The same for standard indicators like ATR – ATR with Shift[0] is computed using High[0], Low[0] which are again not known at market open.

#### **Weekly and monthly data**

Weekly and monthly data are also available. For the current week and month the closing price is derived simply from the last trading day price. The data for the current week and month are simply referred using bar [0] (e.g. Weekly High[0] or Monthly Close [0] etc.).

## Handling Stop Loss and Profit Targets in backtesting

Backtesting of Stockpicker strategies must be fast – we have to evaluate one strategy on hundreds or thousands of stocks. Because of this we use only OHLC daily prices in the backtest.  
It is not possible to use minute or tick data for Stockpicker engine.

Using daily OHLC data has one disadvantage that cannot be easily resolved – we don’t know the intraday movement of the price.  
It is not a problem in most of the cases, we describe how SQ backtestng engine handles this to make sure backtests are reliable.

The problem could arise when you use SL + PT, or entry at Stop/Limit combined with SL or PT.  
In both cases there is more than one stop or limit price level – entry (stop/limit), SL, PT.

**The problem we are facing is in situations when these levels are so close to each other that they both could be filled intraday in the market day.**  
Without real tick data we cannot know which of these levels will be hit first, so we cannot reliably simulate entry at stop/limit followed by SL or PT at the same bar.

In real live trading SL+PT is added immediately when trade is opened, but for reasons mentioned above we are unable to simulate reliably it in backtester – it would lead to too optimistic and unrealistic backtests.

**To ensure the results are reliable the backtester engine detects this kind of situations and handles them like this:**

- When Entry is Market, only SL or only PT is set – in this case we can evaluate SL or PT immediately, at the same bar
- When entry is at Stop/Limit and SL, PT or both are set – in this case SL & PT are applied only next day, we don’t know if entry or SL/PT price was reached at the open bar so we cannot evaluate it immediately
- When both SL and PT is used, and both could have been filled in one bar – backtester takes SL to make a pessimistic variant, as we don’t know which of them would be filled first.

This situation generally affects strategy performance only if you use very tight Stop Loss or Profit Target – don’t do it and you’ll not have this problem.

## Small differences in execution times between backtest and live trading

Note that in backtester when trade is opened at market open or market close using Enter at Market (or Exit at end of day), it is opened at the Open/Close price from the data – the real market Open or Close price.

In live trading this is not 100% achievable – when you trigger the strategy On Bar open, it will be evaluated and executed a very short time AFTER actual market open. The same with On Bar Close – it is executed very short BEFORE the actual market close.

It is handled like this because it is not possible to trigger your orders exactly at market open or close. Because of this, the “Open” and “Close” market prices taken at these times will not match the Open or Close market prices reported in historical data. The difference will be negligible, and it should not affect your strategy performance, but it will be still there.
