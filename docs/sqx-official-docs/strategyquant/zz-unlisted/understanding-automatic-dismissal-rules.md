# Understanding automatic dismissal rules

- Source: <https://strategyquant.com/doc/strategyquant/understanding-automatic-dismissal-rules/>
- Section: StrategyQuant X
- Fetched: 2026-09-04

---

StrategyQuant4 can be configured to dismiss strategies with “wrong” properties – it is set to dismiss (throw away) these strategies by default.

You can control this behavior in Rankings in Builder by clicking on the link Configure automatic dismissal.

[![](https://strategyquant.com/wp-content/uploads/2017/08/automatic_config-300x195.png)](https://strategyquant.com/wp-content/uploads/2017/08/automatic_config.png)

When you open this dialog you can see that there are 8 different strategy checks, we’ll explain them here:

- No trades – means that the strategy contains no trades
- Too many ambiguous trades – means that strategy contains too many trades that begin and end at the same bar, which means that the backtesting cannot be accurate.
- Too many open trades – if strategy reaches more than 100k open trades in parallel
- No filled trades – if strategy places too many pending trades that are never filled
- Zero PL trades – too many trades that have zero Profit/Loss. It means something is seriously wrong with the strategy.
- Zero duration trades – trades have zero duration, they are closed right after they were opened
- Unfinished trades – trades were never finished and run until the end of test
- Too little trades – if there are less than 20 trades, the results are statistically insignificant.
- **Outlier trade** – one trade had exceptionally big profit, bigger than 2 x second and third best profit combined. This would mean an exceptional event in the market that caused the strategy to catch one big profit. This is not statistically significant.
- **Too many trades closing at the same bar** – these are trades that open and close inside the same bar. It usually is a problem, if you are getting a lot of these strategies consider using smaller timeframe. It doesn’t need to be a problem if you trade for example on D1 with high precision (M1, tick), but it would be very unusual daily strategies.

​These checks are performed 40% ​of the history data are processed, it is made this way to save work processing wrong strategies. The threshold for triggering these rules is 25% of all the placed trades. So for example if at least 25% trades are ambiguous, the “too many ambiguous trades” rule is activated and strategy is dismissed.

​

You might have an impression that too big portion of strategies is dismissedIt is important to realize that these checks have to be done, because random process of strategy creation naturally creates also strategies that don’t make any sense. There might even be a majority of such strategies – depending on your setting. Similar checks had to be made also in SQ3, but they were not configurable or even visible.

You can turn off any of these automatic checks​ and let StrategyQuant save also strategies with some of these “bad” properties, but you should know what you are doing. It is generally not recommended to trade such strategies.

If you have too many strategies that are dismissed automatically check your settings:

- Isn’t your SL too small? It might cause trades to close within the same bar.
- Aren’t you using an incorrect End of day or Trade range setting? This might cause trades to close too early.

### ​Checking your dismissal statistics

​Another nice feature of SQ4 is that you can check what were the dismissal statistics of the whole build process. You can check it by clicking on the link below:

![](https://strategyquant.com/wp-content/uploads/2017/08/stats_link.png)

​It will display the following dialog:

![](https://strategyquant.com/wp-content/uploads/2017/08/stats_dlg.png)

Here you can see how many strategies were dismissed for what reason. Note that in addition to automatic dismissal rules you can see also dismissals caused by your own custom conditions!So you can identify which conditions cause your strategies not to pass.
