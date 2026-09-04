# Comparing and using the same backtest settings

- Source: <https://strategyquant.com/doc/strategyquant/comparing-and-using-the-same-backtest-settings/>
- Section: StrategyQuant X › Reliable backtesting & trading
- Fetched: 2026-09-04

---

One of the most frequent errors when using SQ is making a mistake or difference in backtest settings.

Imagine you generate a strategy in **Builder** and save it to disc. Then you’ll load it to **Retester** or **Optimizer**, configure it the same and retest it, only to find out that the results are different than the original test in Builder.

When this happens it is ALWAYS caused by a change settings or data – you either updated data in the meantime or you didn’t configure Retester in the same way as the strategy was backtested in Builder.

There are two techniques that can help you when you experience this problem:

## Use Apply strategy config

When you load your strategy to databank, then double-click on it to open it in **Results** tab you can see its **Strategy config** tab.

[![Apply strategy config](https://strategyquant.com/wp-content/uploads/2020/11/strategy_config.png)](https://strategyquant.com/wp-content/uploads/2020/11/strategy_config.png)

It shows you the current project configuration sde by side with configuration of strategy last backtest that produced the performance results you see. YOu can easily spot the differences, and you can also very easily apply last strategy settings to your current project – just click on Apply strategy config button.

This way you can be sure that your project is configured exactly as same as the previous strategy backtest.

## Use Compare functionality

This is a new function available since Build 130. It allows you to compare configs of two different strategies from your databank – it can also be two different backtests of the same strategy saved as separate files.

Just choose two strategies in the databank and click on the **Compare** button:

[![Compare strategy settings](https://strategyquant.com/wp-content/uploads/2020/11/Compare_settings.png)](https://strategyquant.com/wp-content/uploads/2020/11/Compare_settings.png)

It will display a window where you’ll see configuration of the last backtest for each of the strategies side by side, with differences in red:

[![Strategy config compare result](https://strategyquant.com/wp-content/uploads/2020/11/compare_result-1024x612.png)](https://strategyquant.com/wp-content/uploads/2020/11/compare_result-1024x612.png)
