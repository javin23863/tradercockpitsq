# Monte Carlo trades manipulation

- Source: <https://strategyquant.com/doc/strategyquant/monte-carlo-trades-manipulation/>
- Section: StrategyQuant X › Cross checks - robustness tests
- Fetched: 2026-09-04

---

This cross check run simulations where in each simulation it manipulates the existing trades – shuffles them, misses some and so on.

It is very quick, because it doesn’t require running backtests, it works on already existing trades from main backtest.

The idea behind this is to verify how much the strategy equity curve depends on particular order of the trades, and what would happen if some trades are missed.

You can do these trade manipulations in every simulation:

**Randomize Trades Order** – this is the simplest test, it randomly shuffles order of the trades. This doesn’t change the resulting Net Profit, but it is very useful in examining different variations of Drawdown that can be a result of different order of trades.

**Randomly Skip Trades** – it will randomly skip trades with given probability. In real trading you can often miss a trade because of platform or Internet failure, or simply because you paused trading for some time. This test will give you an idea how the equity curve might look like if some trades are randomly skipped.

**Interpreting the results**

Robustness tests output the results as a set of equity charts for each testing run AND a table showing the results of Monte Carlo simulation.

[![](https://strategyquant.com/wp-content/uploads/2019/03/mc1.png)](https://strategyquant.com/wp-content/uploads/2019/03/mc1.png)

In this example we’ve run 100  simuations, with randomly skipped trades.

We can see what would be the equity for each of these simulations and the table on the left provides us the valuable information on the strategy properties during these simulations.

[![](https://strategyquant.com/wp-content/uploads/2019/03/mc2.png)](https://strategyquant.com/wp-content/uploads/2019/03/mc2.png)

**What do these values mean?**

The first row displays values of Net Profit, Maximum % Drawdown etc. of original strategy for comparison.

The rest of the rows display values at different confidence levels.

These numbers are a result of Monte Carlo analysis applied on our 10 random simulations.

For example, values at 80% confidence level mean that there is 20% chance that Net Profit, Drawdown etc. will be worse than the confidence level values.

Values at 90% confidence level mean that there is 10% chance that Net Profit, Drawdown etc. will be worse than the confidence level values.

**Values at 95% confidence level mean that there is only a 5% chance that Net Profit, Drawdown, etc. will be worse than these values.**

So Monte Carlo simulation of our strategy shows us that by skipping 10% of random our Net Profit can decrease from $ 6990 to $ 3943, and Maximum Drawdown can increase from 6.97% to 11.36%.

This means that there is only 5% chance that Net Profit will be lower than $ 3943. By looking at the higher confidence levels we can see that none of our tests had worse results than $ 3943, so the strategy seems to be relatively robust to the changes we exposed it to.

Because Monte Carlo simulations are generated randomly, equity charts and values in the table will slightly differ every time you retest the strategy. Also, the more simulations you’ll run, the bigger statistical significance of this test.
