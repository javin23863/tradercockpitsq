# Builder – Dismiss similar strategies in databank

- Source: <https://strategyquant.com/doc/strategyquant/builder-dismiss-similar-strategies-in-databank/>
- Section: StrategyQuant X › Program screens
- Fetched: 2026-09-04

---

When Builder generates strategies using genetic programming, or from templates, the strategies are generated from some initial template or population. To avoid databank overflowing with same or very similar strategies there is a (recommended) check **Dismiss similar strategies in databank**.

[![](https://strategyquant.com/wp-content/uploads/2024/09/builder_dismiss_similar_stats.jpg)](https://strategyquant.com/wp-content/uploads/2024/09/builder_dismiss_similar_stats.jpg)

### How exactly does SQX recognize similar strategies?

The method must be fast and reliable. It actually does not compare strategy rules – just a small change in one indicator value can lead to a very different results. What it uses instead are key strategy results (stats).

It creates a “fingerprint” for every strategy based on its three key stats – Number of trades, Net profit, Drawdown – in money value from full sample.

Then it compares fingerprints of every newly generated strategy with existing strategies in databank.

If the fingerprint of new strategy matches some existing other strategy +-5% for ALL these values, the strategy is considered similar.

Then it checks which strategy has better fitness between old and new, and puts the one with bigger fitness into the databank.

### When to turn this filtering off?

There aren’t many cases where you should turn this functionality off – it is highly recommended to keep it on. Otherwise you’ll end up with a few very similar strategies in your databank repeating over and over with very small differences in results.

One possibility when it might make sense is if you generate from strategy template with some limited number of possible variations, and you want to see all the generated strategies.
