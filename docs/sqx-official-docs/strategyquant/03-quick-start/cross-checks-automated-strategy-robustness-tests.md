# Cross checks – automated strategy robustness tests

- Source: <https://strategyquant.com/doc/strategyquant/cross-checks-automated-strategy-robustness-tests/>
- Section: StrategyQuant X › Quick start
- Fetched: 2026-09-04

---

The biggest danger of strategies generated using any machine learning process is overfitting (or curve-fitting) strategy to the historical data on which it was build is .

During or after developing new strategy you should make sure your strategy is robust – which should increase the probability that it will work also in the future.

## What is robustness?

It is simply the property of strategy of being able to cope with changing conditions:

- First of all, strategy should work on unknown data (if the market characteristics didn’t change) either with or without periodic parameter re-optimization
- It should not break apart if some trades are missed.
- Robust strategy should not be too sensitive to input parameters – it should work even if you slightly change the input parameter values, such as indicator period or some constant or if historical data are slightly changed – spread or slippage is increased, and so on.

the most basic test for robustness is testing the strategy on unknown (Out of Sample) data.

If you run genetic evolution, the strategy is evolved only on the In Sample part of data. The Out of Sample part is “unknown” to the strategy, so it can be used to determine if the strategy performs also on unknown part of data.

[![Avoiding overfitted (curve-fitted) strategies](https://strategyquant.com/wp-content/uploads/2019/03/oos1.png)](https://strategyquant.com/wp-content/uploads/2019/03/oos1.png)

The blue part of each chart is the Out of Sample (unknown) data., We can see that the strategy on the left performs well also on this part, while strategy on the right fails on the unknown data – it is almost certain to be curve fitted.

## Automatic cross checks for robustness in SQ X

Cross checks are optional additional methods that can be applied to every strategy after it is generated and passes the first filters.

Check this article how to simply [Use Cross checks build in Builder and Retester](../06-cross-checks-robustness-tests/use-cross-checks-build-builder-retester.md)

Cross checks can verify strategy robustness from more points of view – by trading it on additional markets, or by using Monte Carlo methods to simulate 100s different equity curves, or even using Walk-Forward optimization or Matrix.

The important thing is that you can use cross check filters to dismiss the strategy if it doesn’t pass the cross-check test. This allows you to create funnels, where strategy is scrutinized by increasingly advanced (and more time demanding) methods, and strategy that fails is automatically dropped.

[![Automated robustness tests funnel in StrategyQuant X](https://strategyquant.com/wp-content/uploads/2019/02/crosscheck.png)](https://strategyquant.com/wp-content/uploads/2019/02/crosscheck.png)

*An example of filtering funnel using cross checks during build*

It is up to you how many cross checks you’ll employ and how you’ll configure their filters.

Cross checks are divided into three groups – Basic, Standard, Extensive – depending on how time consuming they are.

They are also applied from the simple ones to the more complicated ones. So, if strategy doesn’t pass Cross check 1, it is dismissed and not tested by Cross check 2.

|  |
| --- |
| **Note that running cross check on a strategy can take significant time!**  Some cross-check methods make complicated simulations and hundreds or even thousands of backtests of the strategy with different parameters and take thousands time more time than the initial strategy generation and initial backtest.So strategy without any cross-check can be generated (for example) in 0.2 second, but with some cross checks applied it could easily take 10 – 200 seconds per one strategy! |

Cross checks can be used also in Retester (without filtering), so you don’t need to use all cross-checks in build mode.

## Possible usage of cross-checks

One possible cross-check application could be to use:

- Cross check **Retest with higher precision**
- Cross check **Monte Carlo trades manipulation**
- Cross check **Retest on additional markets**
- Optionally cross check **Monte Carlo retest methods**

StrategyQuant will then perform the following steps for every generated strategy:

1. Strategy is randomly generated and tested with fastest “Selected timeframe” precision – this is dependent on your setting, but it is the default
2. Strategy is automatically filtered and thrown away (dismissed) if it doesn’t pass your global filters, for example, if it doesn’t have enough trades or if Net Profit < $1000
3. Cross check **Retest with higher precision** will retest this strategy with minute or even real tick precision – to make sure the strategy was reliably backtested using the basic precision. Note that only strategies that pass point 2. will get here. If strategy doesn’t pass this first cross-check, it is dismissed.
4. Cross check **Monte Carlo trades manipulation** will run number of simulations of different equity curves by manipulating the existing trades – to ensure that the original equity curve wasn’t achieved just a luck. You’ll filter out strategies that don’t pass this Monte Carlo test
5. Cross check **Retest on additional markets** will test the strategy on additional markets or timeframes. If it isn’t profitable on other markets it is filtered out.
6. Optional cross check **Monte Carlo retest methods** will run number of simulations where each simulation is a new backtest of the strategy using small variations in strategy indicator parameters, trading options such as spread, slippage, or in history data.Note that every such simulation is an independent backtest, so if it took 0.2 seconds to backtest the strategy in step 1., it will take 100 x 0.2 s. = 20 seconds to finish this cross check for this one strategy if you’ll use 100 Monte Carlo simulations.  
   If strategy passes also this cross-checks it is saved into databank and you can have quite good confidence that it is robust enough.

You can find more detailed description of cross checks in the section [Cross checks – robustness tests](../06-cross-checks-robustness-tests/use-cross-checks-build-builder-retester.md) section.
