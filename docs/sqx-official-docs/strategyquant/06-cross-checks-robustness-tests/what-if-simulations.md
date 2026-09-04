# What If simulations

- Source: <https://strategyquant.com/doc/strategyquant/what-if-simulations/>
- Section: StrategyQuant X › Cross checks - robustness tests
- Fetched: 2026-09-04

---

What If simulations is a new powerful yet quick and simple cross check to verify strategy robustness (added into StrategyQuant Build 129).

What If scenarios allow you to simulate scenarios like:

- what if strategy trades only certain days in a week, or certain hours in a day?
- what if we’ll ommit 5% of the most profitable trades?
- etc.

The idea for What If cross check came from our [**QuantAnalyzer**](https://strategyquant.com/quantanalyzer) product, which has this functionality for a long time.

By adding it to StrategyQuant as a cross check you can immediately filter out strategies that will perform significantly worse under your What If scenario.

## How does What If simulation work?

It is simple – every selected What If simulation is applied to the list of orders produced by the standard backtest.

For example, if you use **Trade only in days** simulation and choose to trade only on **Tuesday, Wednesday, Thursday**, it will go through all the trades an dfilter out the ones that were not opened in these three days.

It is important to realize that What If simulation doesn’t backtest the strategy again – it works with existing list of trades from main backtest. Thanks to this, this crosscheck is very fast.

## Configuration

What If is similar to Monte Carlo – you choose one or more scenarios that are applied to strategy results:

[![What If simulations in StrategyQuant cross checks](https://strategyquant.com/wp-content/uploads/2020/08/whatif_conf-1024x525.png)](https://strategyquant.com/wp-content/uploads/2020/08/whatif_conf-1024x525.png)

## Filtering based on What If simulation performance

optionally you can use automatic filtering to filter out strategies whose performance falls under given limit:

[![What If simulation filtering](https://strategyquant.com/wp-content/uploads/2020/08/whatif_filter.png)](https://strategyquant.com/wp-content/uploads/2020/08/whatif_filter.png)

In the picture above we defined that Net profit of What if simulation must be at least 80% of Net profit of normal backtest.

## Viewing results

when you run What If simulation it will create a new result that can be displayed in equity chart, overview or in list of trades.

See an example of difference between backtest equity and What If simulation below:

[![What If simulation result](https://strategyquant.com/wp-content/uploads/2020/08/whatif_result-1024x795.png)](https://strategyquant.com/wp-content/uploads/2020/08/whatif_result-1024x795.png)
