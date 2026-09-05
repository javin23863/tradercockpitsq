# Settings – What to build

- Source: <https://strategyquant.com/doc/strategyquant/what-to-build/>
- Section: StrategyQuant X › Program screens
- Fetched: 2026-09-04

---

Here you select what exactly you want to generate. You can generate a new strategy for single chart, for multi-symbol or multi-timeframe, or improve a part of the existing strategy

[![](https://strategyquant.com/wp-content/uploads/2019/01/what_to_build1.png)](https://strategyquant.com/wp-content/uploads/2019/01/what_to_build1.png)

## Strategy type options:

- **Simple strategy** – “standard” simple strategy that trades on one symbol and timeframe
- **Multi-TF or multi-symbol strategy** – strategy that can use multiple additional charts in addition to the main one. For example, it will be trading on EURUSD/H1, but it could be looking also at data for EURUSD/H4 and GBPUSD/H1.  
  You simply must define how many of these additional charts the strategy will use, you’ll then later define which ones they will be exactly in Data settings
- **Strategy from template** – allows you to generate strategy using your own strategy template. You can create strategy template in Wizard, and then choose it here. This allows you to generate strategies with architecture that is different than standard SQ X strategies
- **Improve existing strategy** – you must choose strategy that you want to improve, and then in another settings **Parts to improve** you’ll choose what exactly should be improve in this strategy – it could be only Long entry rule, or only Short Exit rule, or type of order placed (Market / Stop / Limit).

## Trading direction

You can choose to generate strategies that trade only to one direction (Long or Short) or to both directions (which is standard).  
You can also select that you want the entry or exit rules to be symmetrical. If they are symmetrical, then the rules for both directions are the same, only reversed.  
An example of symmetrical rules:

```
Go Long if CCI > 0
Go Short if CCI < 0
```

As an alternative, you can choose to use non-symmetrical rules, in this case the rules for Long and Short sides will be generated independently.  
An example of non-symmetrical rules:

```
Go Long if CCI > 0
Go Short if RSI < 0 and Momentum < 100
```

This setting can be used for both entry and exit rules, for example you can have symmetrical entry rules, but non-symmetrical exit rules, so the strategy will effectively use (for example) different stop loss and profit target for Long and Short orders.

## Strategy style configuration and build mode

There are important configuration options that you can set in addition to the basic strategy types. Clicking on this link will open a new popup dialog. You can choose from three different architecture types, they are described in detail in their own section in this guide.

[![StrategyQuant strategy style config](https://strategyquant.com/wp-content/uploads/2019/01/strategy_style.png)](https://strategyquant.com/wp-content/uploads/2019/01/strategy_style.png)

## Build mode

You can select whether SQ will generate strategies using Genetic evolution or Random generation.

[![Strategyquant build mode config](https://strategyquant.com/wp-content/uploads/2019/01/build_mode-1.png)](https://strategyquant.com/wp-content/uploads/2019/01/build_mode-1.png)

If the genetic evolution option is used a new navigation tab “**Genetic options**” will show up in the builder. All the options available are discussed later in this guide.

[![SQ tab genetic options](https://strategyquant.com/wp-content/uploads/2019/01/tab_genetic_options.png)](https://strategyquant.com/wp-content/uploads/2019/01/tab_genetic_options.png)

## # of Conditions, Periods settings

[![SQ number of conditions, periods, shifts config](https://strategyquant.com/wp-content/uploads/2019/01/n_conditions.png)](https://strategyquant.com/wp-content/uploads/2019/01/n_conditions.png)

### Number of conditions in entry and exit rules

This determines min and max number of conditions that should be generated for one signal.

For example, if you’ll alow only one condition your signal will look like:

```
EntrySignal = CCI > 0
```

If you’ll use three conditions, it could look like:

```
EntrySignal = CCI > 0 and RSI >50 or Momentum < 0
```

Note that there are three different conditions (CCI, RSI, Momentum comparison), connected with and/or.

Setting higher range for number of conditions is especially important for Fuzzy rules strategy, because fuzzy logic will be effective when there are at least 3 or four conditions to evaluate.  
If you are using Fuzzy Logic architecture, make sure you set Conditions to generate Minimum at least to 3 or more.

### Shift (lookback period)

is the number of bars in the past the condition could look into.

Shift=0 means that the condition is evaluated on the current bar, Shift=1 means the condition is evaluated on the previous bar, Shift=2 means it is evaluated on bar before previous one and so on.So here you can define ranges that wil be used when generating strategy conditions.

In general, it isn’t good to allow strategy to look to far into the past. For example, value of CI(14) 10 bars back doesn’t bear too much significance for the current market status. It is recommended to keep this range small, between 0-5.

**Using Minimum Shift = 0 ?**  
When you set minimum Shift = 0 you allow to create conditions that check for the indicator value on the latest current bar. Conditions are usually evaluated on bar open, but most of the indicators use bar close to compute their value. So the value of indicator on bar open (For example CCI(14) could be very different from its value at the end of the bar.

For this reason we think it makes more sense to set Minimum Shift to 1, this way on bar open (when conditions are evaluated) you’ll get indicator values from the previous bar that just finished, and the indicator value was computed and final.

### Indicators period

is min and max range of period value, meaning how big period should be used in indicators generated in StrategyQuant.

Indicators period must be bigger than 1, and ideally smaller than 10o, or even smaller than 50.  
Again, it is recommended to not use too big periods.

If you use more than one chart, you can configure all these settings for each chart separately.

Note that all these settings are ranges from Min or Max. The exact number of conditions, shift and period will be determined randomly when every strategy or condition is generated.

The second part is configuration for every chart that the strategy can access. For simple strategy there will be only one chart, but **if you build multi-TF or multi-symbol strategy, you can configure the generation for every chart separately**.

[![SQ NUmber fo conditions, periods for multi charts](https://strategyquant.com/wp-content/uploads/2019/01/n_cond_multi.png)](https://strategyquant.com/wp-content/uploads/2019/01/n_cond_multi.png)

## Stop Loss & Profit Target Options

These settings allows you to specify whether Stop Loss and Profit Target should be mandatory in the strategy, and what is the minimum and maximum of the SL/PT values in pips. You can also define the desired Risk Reward ratio.

Having defined SL/PT in the strategy is the simplest and many times the most effective approach.

[![SQ Stop Loss range config](https://strategyquant.com/wp-content/uploads/2019/01/sq_config.png)](https://strategyquant.com/wp-content/uploads/2019/01/sq_config.png)

If you unselect the mandatory SL/PT then the randomly generated strategy can (but doesn’t have to) have fixed SL/PT. It is advisable to use different exit rule if you uncheck this setting,for example exit after X bars, otherwise the strategy will have no way to exit the trade.

[![](https://strategyquant.com/wp-content/uploads/2019/01/profit_target_setting.png)](https://strategyquant.com/wp-content/uploads/2019/01/profit_target_setting.png)
