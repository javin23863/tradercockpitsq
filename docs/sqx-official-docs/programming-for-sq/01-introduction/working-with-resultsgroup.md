# Working with ResultsGroup

- Source: <https://strategyquant.com/doc/programming-for-sq/working-with-resultsgroup/>
- Section: Programming for StrategyQuant X › Introduction
- Fetched: 2026-09-04

---

[**ResultsGroup**](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/ResultsGroup.html) is the object that SQX uses to store:

- strategy code/XML
- backtest results
- other results from optimizations and cross checks
- settings used in the last backtests
- other useful custom values in its key-value store

Each strategy you see in the databank is in fact a **[ResultsGroup](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/ResultsGroup.html)** object containing the strategy + results that are then displayed in the databank columns or in trades list, equity chart, etc.

## Getting individual results from group

There can be multiple backtests or optimizations performed for one strategy – you can have main backtest + backtests on additional markets, or additional WF optimizations.

The results of these separate backtests are stored as separate [**Result**](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/Result.html) objects in the ResultsGroup, so ResultsGroup object is in fact a group of Result objects plus few other things.

Individual results in the ResultsGroup are accessible by calling [**subResult**(resultKey)](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/ResultsGroup.html#subResult(java.lang.String)) method, where resultKey is the name (key) of the subresult you want to get.

You can get a list of all result keys by calling [**getResultKeys**()](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/ResultsGroup.html#getResultKeys()), which will return an array of all the keys the ResultGroup contains.

You can also use a special method [**getMainResultKey**()](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/ResultsGroup.html#getMainResultKey()) to get key of the main backtest result.

```
Result mainResult = resultsGroup.subResult( resultsGroup.getMainResultKey() ) // will return the main backtest result.
```

ResultsGroup object name is the strategy name you see in databank, so calling resultsGroup.[getName()](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/ResultsGroup.html#getName()) will return “Strategy 1.2.3” for example

### Portfolio

There is also a special portfolio result that is added automatically if ResultsGroup contains more than one result. You can get portfolio result by simply calling ResultsGroup.[**portfolio**()](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/ResultsGroup.html#portfolio())

Result portfolioResult = resultsGroup.[**portfolio**()](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/ResultsGroup.html#portfolio()).

If there is just one result this call will return the main backtest result.

## Getting metrics from a Result

When you have a Result you can easily get the metrics such as Net profit, SharpeRatio etc. that were computed for this result.

All computed metrics are stored in [**SQStats**](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/SQStats.html) object that you can get by calling Result.**[stats](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/Result.html#stats(byte,byte,byte))**(Direction, PLType, SampelType). Note that the stats() method has three parameters – the stats are computed separately for:

- [every direction](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/Directions.html) (Long, Short, Both),
- [PL type](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/PlTypes.html) (money, percent, pips)
- [sample type](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/SampleTypes.html) (FullSample, InSample, OutOfSample).

```
SQStats statsIS = result.stats(Directions.Both, PlTypes.Money, SampleTypes.InSample);

SQStats statsOOS = result.stats(Directions.Both, PlTypes.Money, SampleTypes.OutOfSample);

SQStats statsLong = result.stats(Directions.Long, PlTypes.Money, SampleTypes.FullSample);
```

The [**SQStats**](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/SQStats.html) object is a simple key-value map that contains the computed metrics for the given stats combination. The metrics are stored under the keys that correspond to the class names of Java snippets, for example “NetProfit”, “Drawdown”, etc.

Once you have SQStats object you can get the value of the metric by simply calling:

```
double netProfit = stats.getDouble("NetProfit");

int noOfTradest = stats.getInt("NumberOfTrades");
```
