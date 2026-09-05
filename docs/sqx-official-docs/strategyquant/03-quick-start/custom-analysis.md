# Custom analysis

- Source: <https://strategyquant.com/doc/strategyquant/custom-analysis/>
- Section: StrategyQuant X › Quick start
- Fetched: 2026-09-04

---

The new Custom analysis feature introduced first in Build 131 Dev 4 allows you (as the name suggests) to perform a custom analysis of the generated / retested /optimized strategies and whole databanks.

This allows you to run your own custom computations and filtering.

Custom analysis classes and methods are implemented as snippet, with a quite simple usage – only three methods are used.

## Usage of custom analysis

It is an addition to all the metrics and filtering done by StrategyQuant to allow adding customizations to the workflow:

- it can be used to compute new metrics that cross the boundaries of a single backtest or crosscheck
- it can be used to implement new metrics that are shown in databank
- it can be used for filtering strategies (by returning false)
- it can call external programs (for example in Python) to perform additional analysis for the strategies

There are two types of custom analysis possible:

## Per strategy

Is performed after the complete backtests and crosschecks were finished and BEFORE saving the strategy into databank. This analysis can check all the backtest and crosscheck results and compute / analyze useful information from it.

It can also be used as a filter – if the custom analysis method returns false, the strategy might optionally be not saved into a databank – depending on if filtering for  custom analysis is turned on in UI.

You can configure custom analysis in the Ranking tab in Builder / Retester / Optimizer projects.

If you enable also Filter, it will filter (dismiss) strategies also according to the result of the custom analysis method for each strategy.

[![Custom analysis ranking](https://strategyquant.com/wp-content/uploads/2021/08/ca_ranking.png)](https://strategyquant.com/wp-content/uploads/2021/08/ca_ranking.png)

## Per databank

Another type of custom analysis is the one that runs on the whole databank.

It will get the array of all strategies in databank which it can then use to compute anything, including comparing values between strategies, running various counts and statistics on all the strategies in databank and even remove strategies from databank.

This custom analysis type can be used only in a custom project, in the new **Custom analysis task**.

The task has 4 possible configurations, it allows you to choose the Source and Target databank, and then 4 custom analysis methods that can be applied following each other:

Per strategy analysis  
Per databank analysis  
Per strategy analysis  
Per databank analysis

[![Custom analysis databank](https://strategyquant.com/wp-content/uploads/2021/08/ca_databank-750x458.png)](https://strategyquant.com/wp-content/uploads/2021/08/ca_databank-750x458.png)

It is like this so that you can run the per strategy analysis first, then per databank analysis, and then again, do the same.
