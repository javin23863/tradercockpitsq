# Fit strategy to existing portfolio

- Source: <https://strategyquant.com/doc/strategyquant/fit-strategy-to-existing-portfolio/>
- Section: StrategyQuant X › Quick start
- Fetched: 2026-09-04

---

A new functionality in StrategyQuant that allows you to find strategies that will complement your existing portfolio.

The idea behind this functionality is to find the best strategies that complement your existing portfolio and are not correlated with any of your existing strategy.

Note – this feature is available starting with StrategyQuant Build 130.

Its configuration is available in **Settings** -> **Ranking** tab in Builder and there is a new databank named **Existing portfolio**.

[![Fit strategy to existing portfolio](https://strategyquant.com/wp-content/uploads/2020/11/fit_to_ports-1024x766.png)](https://strategyquant.com/wp-content/uploads/2020/11/fit_to_ports-1024x766.png)

To use it, simply load a few of your aready existing strateies into Existing portfolio databank.

Then you can:

## Compute fitness based on your existing portfolio

Configure fitness computation based on your existing portfolio and not the main backtest.

You can do it by selecting Use = Existing portfolio in Fitness configuration.

[![Fit strategy to portfolio fitness](https://strategyquant.com/wp-content/uploads/2020/11/fit_to_port_fitness.png)](https://strategyquant.com/wp-content/uploads/2020/11/fit_to_port_fitness.png)

When configured, it will create a portfolio simulation using your new strategy plus your existing strategies and it will compute fitness of this portfolio.

By computing fitness this way you select to prefer strategies that improve the portfolio as a whole, and not the strategies that are best only on their own.

## Dismiss correlated strategies

Set a filter to dismiss newly generated strategies that have too high correlation with any other strategy in your existing portfolio.

There is a new optional filter on the bottom of the Filtering conditions:

[![Fit strategy to portfolio - correlation filter](https://strategyquant.com/wp-content/uploads/2020/11/fit_to_port_filter.png)](https://strategyquant.com/wp-content/uploads/2020/11/fit_to_port_filter.png)

You can configure the maximum allowed correlation, what kind of correlation to compute  (P/L by day, month, etc.) and a few other properties.

When the filter is active it will compute correlations between newly generated startegy and al the strategies in your **Existing portfolio** databank and dismisses the new strategy if any of these correlations is bigger than allowed boundary.
