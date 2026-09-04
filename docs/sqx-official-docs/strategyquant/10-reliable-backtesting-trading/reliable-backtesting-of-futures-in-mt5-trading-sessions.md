# Reliable backtesting of futures in MT5 – Trading sessions

- Source: <https://strategyquant.com/doc/strategyquant/reliable-backtesting-of-futures-in-mt5-trading-sessions/>
- Section: StrategyQuant X › Reliable backtesting & trading
- Fetched: 2026-09-04

---

Trading and backtesting futures in MetaTarder5 has its own quirks – from unusual tick size (for example 0.25 for ES futures) to Trading session – time when MetaTrader/broker allows trading.

We made a few improvements in the new upcoming StrategyQuant X Build 140 in order to have perfect and matching backtests between SQ and MT5 for futures.

## What is Trade Session in MT5 ?

You can find Trade session in your symbol specification. To get the Specification right-click on the symbol in your Market Watch in MT5, and choose Specification:

[![MT5 symbol specification](https://strategyquant.com/wp-content/uploads/2024/07/specification_menu.jpg)](https://strategyquant.com/wp-content/uploads/2024/07/specification_menu.jpg)

It will open a dialog with detailed specification of all the important parameters of this symbol. When you scroll to the bottom you’ll see Quotes and Trade sessions:

[![mt5 trade session](https://strategyquant.com/wp-content/uploads/2024/07/mt5_trade_session.jpg)](https://strategyquant.com/wp-content/uploads/2024/07/mt5_trade_session.jpg)

In this particular example we can see that symbol ES is not traded full 24 hours. It trades from 01:00-23:15, then from 23:30-24:00.

So there is a gap when MT5/broker does not accept trading, if you’d try to send or modify orders during this time gap you’d get an ‘Market is closed’ error.

The actual trade session can differ by symbol and by broker, you should always check the actual setting of your broker.

## New MarketOpenSession in SQX

Starting with SQX Build 140 we added a new feature that allows you to specify the correct trade session for MetaTrader 5 also in SQX – a new MarketOpenSession trading option, available for MetaTrader5 engines:

[![sq marketopensession](https://strategyquant.com/wp-content/uploads/2024/07/sq_marketopensession-465x750.jpg)](https://strategyquant.com/wp-content/uploads/2024/07/sq_marketopensession-465x750.jpg)

This new option allows us to specify the trade option also in SQX.

The session is defined normally in **Data Manager** -> **Sessions**, like this:

[![SQX session definition](https://strategyquant.com/wp-content/uploads/2024/07/sqx_session_definition.jpg)](https://strategyquant.com/wp-content/uploads/2024/07/sqx_session_definition.jpg)

You can see that we defined session in the same way as in MT5 – the only difference is that we used 23:59 instead of 24:00, as SQX doesn’t allow using 24:00 time.

## What’s the difference?

If you don’t use this new trading option SQX will behave just like before – it would trade also during the time when market is closed in MT5.

If you use it the backtest in SQX will exactly match your MetaTrader5 – providing you use the same data and settings on both platforms.

Below you can see a comparison of trading a sample ES strategy **with and without** MarketOpenSession used:

[![](https://strategyquant.com/wp-content/uploads/2024/07/sqx_result_marketopensession-750x67.jpg)](https://strategyquant.com/wp-content/uploads/2024/07/sqx_result_marketopensession-750x67.jpg)

You can see that the difference can be very significant, so it is highly recommended to use this new feature – trading session (MarketOpenSession) – in SQX from Build 140 up.
