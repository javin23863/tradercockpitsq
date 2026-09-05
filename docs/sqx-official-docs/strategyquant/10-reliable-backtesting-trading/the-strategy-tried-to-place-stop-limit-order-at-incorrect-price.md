# The strategy tried to place stop/limit order at incorrect price

- Source: <https://strategyquant.com/doc/strategyquant/the-strategy-tried-to-place-stop-limit-order-at-incorrect-price/>
- Section: StrategyQuant X › Reliable backtesting & trading
- Fetched: 2026-09-04

---

**If you are running or backtesting strategie from StrategyQuant X in Metatrader 5, you might can see this info message in your logs:**

020.08.26 00:00:01.102 Strategy 1.4.130 (MYMU20,H1) 2020.08.25 22:00 No pending orders of that type  
2020.08.26 00:00:01.118 Strategy 1.4.130 (MYMU20,H1) —VERBOSE— 2020.08.25 22:00 Based on its logic, the strategy tried to place stop/limit order at incorrect price. Market price: 28220.00000000, min. price allowed: 28220.00000000, stop/limit order price: 27966.00000000

**Is this an error? No. Here is explanation:**

It means the entry conditions of the strategy were met and normally the strategy would open a new order. But in this case the calculated order price was out of the market and the order was skipped because of that.
