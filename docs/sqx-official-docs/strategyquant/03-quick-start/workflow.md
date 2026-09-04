# Recommended workflows for building strategies

- Source: <https://strategyquant.com/doc/strategyquant/workflow/>
- Section: StrategyQuant X › Quick start
- Fetched: 2026-09-04

---

There is not just one ideal workflow that you should follow. As you’ll learn more about the topic of creating strategies, you’ll begin to see vatious options and paths you can follow.

Please follow our **[Blog](https://strategyquant.com/blog/)** for new articles, recommendations and up-to-date tips, and our **[Shared section](https://strategyquant.com/shared-category/build-configs/)** to try workflow published by somebody else.

## How to start quickly

The easiest way to start is to use one of the **predefined buider configurations**:

[![StrategyQuant predefined configs](https://strategyquant.com/wp-content/uploads/2019/01/sq_predefined_configs-1024x247.png)](https://strategyquant.com/wp-content/uploads/2019/01/sq_predefined_configs-1024x247.png)

A little more complex is using one of the **example custom project workflows** from Geting Started page:

[![StrategyQuant custom project workflow examples](https://strategyquant.com/wp-content/uploads/2019/01/sq_workkflow_samples.png)](https://strategyquant.com/wp-content/uploads/2019/01/sq_workkflow_samples.png)

## A blueprint of a standard workflow

The general flow of work when generating new strategies can be described as a series of the following steps:

1. Import or download data for backtesting
2. Configure Builder options
3. Run build
4. Evaluate generated strategies
5. Retest or optimize, perform more checks

### 1. Import or download data for backtesting

You can use the history data that come with the program, import your own data in various formats or download real tick data from Dukascopy broker.

### 2. Configure Builder options

Go through all the settings and configure strategy type, indicators, and order types to be used for trading rules. Optionally use time constraints to limit trading to a certain time range.

Turn on cross checks (strategy robustness tests) you want to use during strategy generation. Selected cross checks will be applied on every generated strategy and can automatically filter out “bad” strategies. You can learn more about cross checks n the next chapter.

Configure also ranking options – they allow you to select Strategy Selection Criteria – which is how the best strategies are determined.  
You should also set up Custom Conditions to filter only strategies that pass certain criteria.  
It makes sense to dismiss all the strategies that have too little profit or trades, or too small Profit Factor, Return/DD ratio or System Quality Number.

Configuring data – to make the generation phase as fast as possible, you can use Selected timeframe as the test precision option. This allows the program to run quickly and go through as many new strategies as possible. When you’ll find potential good candidates, you can test it with more exact precisions later.

### 3. Run Build

Start the building process. Depending on your settings you can let it run several minutes, several hours or even several days. The more time it will run the more potential strategies it will test.  
The best of them will be always stored into the databank.

### 4. Evaluate generated strategies

Go through the generated strategies and evaluate them. You can either evaluate them visually by checking their equity chart, or by sorting them by their parameters in the Databank.

Choose the best ones to pass to the next step and save them as StrategyQuant file (.SQ X) so that you can work with them later.

### 5. Retest or optimize, perform more checks

**The goal of strategy evaluation is to find strategies that are robust** **and have real edge on the market.**

**It is not difficult to generate strategies that will have great equity curve, because it will be overfitted to the given history data.**

**Robust strategy should work in different conditions and doesn’t break up when there is a small change in parameters or in price data or they miss few trades.**  
Generating good strategy (up to the point 4.) is only half of the job. The other half is to make sure the newfound strategy is “real”, and not overfitted.  
To do that, you can use [Robustness Cross check tests](../06-cross-checks-robustness-tests/use-cross-checks-build-builder-retester.md) as a part of the building process, but also later.

You should retest your strategies on different markets and with different settings, and they should not fail. Only then you can get better assurance that the strategy is robust and will not fail in live testing.

This final step will consist of multiple stages, you can retest your strategies with different settings, on different markets and/or timeframes, or with different spread and slippage, on running Walk-Forward optimization or Walk-Forward matrix, etc.

Optional additional steps are:

### 6. Improve the strategy

You can try to improve the strategy in Improver. You can try to apply different combinations of exit rules or additional conditions to entry rules in search for better performance.

After the improvement you should again run the new strategy variation through robustness tests to make sure it didn’t lose its robustness.

### 7. Optimize the strategy

You can run [simple optimization](../07-optimization/simple-optimization.md) to find better combination of input parameters of your strategy. You can also run [Walk-Forward optimization](../07-optimization/walk-forward-optimization.md) to find out if the strategy would benefit from periodical reoptimization.  
As the last step, you can run [Walk-Forward Matrix](../07-optimization/walk-forward-matrix.md) analysis to determine the best reoptimization period.
