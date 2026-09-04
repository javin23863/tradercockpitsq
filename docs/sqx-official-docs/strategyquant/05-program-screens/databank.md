# Databanks

- Source: <https://strategyquant.com/doc/strategyquant/databank/>
- Section: StrategyQuant X › Program screens
- Fetched: 2026-09-04

---

**Databank** is a storage place where the generated/tested strategies are stored with their results. You can have more than one databank in every project, and each module (Builder / Retester / Optimizee) has its own independent databanks.

[![StrategyQuant databank](https://strategyquant.com/wp-content/uploads/2019/01/databank-1-1024x180.png)](https://strategyquant.com/wp-content/uploads/2019/01/databank-1-1024x180.png)

Databank cannot keep unlimited number of strategies for memory reasons, instead it stores the selected number of top strategies, for example top 100 or top 1000 strategies.

The configuration how many strategies to store in the Databank and how to sort them can be set in the Settings -> [Ranking options](ranking-options.md)

## Actions over strategies in databank

Every strategy result in the Databank can be viewed in the Results screen by using double-click.

### Load

loads StrategyQuant files (.sqx) nto the databank.

### Save

this way you can save your strategy, you should always save good strategies to a StrategyQuant format (.sqx) so that you can work with them later.

You can also save strategies in multiple other supported formats, you can also export the whole databank table content to a CSV or XLS file

### Delete/Clear all

this will delete selected or all strategies from the databank

### Retest

a special action in Builder – it will copy the selected strategies from Builder to Retester together wiht the exact backtesting configuration so you can retest them with some added options or modifications.

### Edit (Parameters/Strategy)

allows you to edit the selected strategy – either the values of its parameters or the trading rules in the build in AlgoWizard editor.

### Portfolio

it will combine selected strategies to a portfolio – a special report that combines trading results of multiple strategies. You can then check/analyze the results of whole portfolio of strategies. Check more details about this functionality in [Merge / Split Portfolio](../11-how-to/merge-split-portfolio.md) article.

## **Databank views**

Each row in the table represents one strategy with the results of the strategy backtest – number of trades, total profit/loss, Fitness score, etc. are visible in the table columns.

As most other things in StrategyQuant, the data columns you see in the databank are configurable using views. You can switch between views and you can modify or create your own view with the columns that are important to you.

[![StrategyQuant databank views](https://strategyquant.com/wp-content/uploads/2019/01/databank_views.png)](https://strategyquant.com/wp-content/uploads/2019/01/databank_views.png)
