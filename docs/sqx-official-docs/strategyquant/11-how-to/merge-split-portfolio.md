# Merge / Split Portfolio

- Source: <https://strategyquant.com/doc/strategyquant/merge-split-portfolio/>
- Section: StrategyQuant X › How to...
- Fetched: 2026-09-04

---

Merge / Split functionality is available as an action above the databank:

[![Merge / Split Portfolio](https://strategyquant.com/wp-content/uploads/2020/04/btn_merge_split.png)](https://strategyquant.com/wp-content/uploads/2020/04/btn_merge_split.png)

# Merge strategies

Merge strategies can combine multiple individual strategies into one using one of the possible ways. To use it, select a few strategies and choose this option.

It will open this dialog:

[![Merge strategies, Ensemble systems](https://strategyquant.com/wp-content/uploads/2020/04/merge-1.jpg)](https://strategyquant.com/wp-content/uploads/2020/04/merge-1.jpg)

You can choose **Name** of your new merged startegy, by default it is Portfolio. You can also choose to save this new portfolio strategy to another databnk than your actual one in **Save to databank** option.

There are three types of merging possible:

## Simulated portfolio

Thsi means that trades of your chosen strategies are merged to one, and portfolio statistics are computed from these merged trades. It simulates how these strategies would trade together – but it considers every strategy to trade independently from the other.

This is an artifical portfolio created only by combining orders of individual strategy components. It cannot be backtested, but it shows portfolio statistics.

## Strategies merged to one (trading in parallel)

EXPERIMENTAL feature. This type of merge will create one compounded strategy that has trading rules of each of the individual strategies.

It can be used in MetaTrader 4/5 to deploy just this one merged strategy to trade multiple individual “strategies” in parallel. You will be able to specify symbols and timeframes for every individual strategy independently.

Note – trading options (Exit on Friday, Limit Trading Range) are global, they are set for the merged strategy and will be valid for all the individual strategies included. So this type of merge wouldn’t wotrk if you’d want to merge strategies that use different trading options.

## Ensemble signals

EXPERIMENTAL feature.  A special type of merge that uses one main strategy, and updates its entry conditions with entry conditions from all other selected strategies using a FUZZY logic.

So this sarategy will be trading only if a given % of all entry signals are valid. In theory this has a potential to improve trading results, it depends very much on the actual strategies.

# Split strategies

Split functionality is simple – it splits an existing portfolio created by Merge to its individual components. Note it will work only on portfolios created in SQ Build 127 or newer.
