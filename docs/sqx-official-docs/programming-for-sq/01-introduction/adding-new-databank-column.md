# Adding databank column / filter

- Source: <https://strategyquant.com/doc/programming-for-sq/adding-new-databank-column/>
- Section: Programming for StrategyQuant X › Introduction
- Fetched: 2026-09-04

---

You might want to extend StrategyQuant by adding new databank column that computes some unique statistics that is important to you.

Every metric like Net profit, Profit factor, Drawdown, Sharpe ratio etc., is implemented as a databank column snippet. The snippet takes care of computing the value and returning it in the proper format to be displayed in the grid.

We’ll use terms stats value, statistical value, databank column in an interchangeable way, they mean the same thing.

There are two basic ways how stats value can be computed:

1. From trades
2. As some ratio between other, already computed stats values

## NetProfit – example of computing stats value from trades

Below is a compute() method of NetProfit snippet that computes total net profit.  
It simply goes through provided list of trades and sums their PL.

```
@Override
public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {

  double netProfit = 0;

  for(int i = 0; i<ordersList.size(); i++) {
    Order order = ordersList.get(i);
    
    if(order.isBalanceOrder()) {
      // don't count in balance orders (deposits, withdrawals)
      continue;
    }
    
    double PL = getPLByStatsType(order, combination); // this returns PL as $, % or pips value
    
    netProfit += PL;
  }

  return round2(netProfit);
}
```

The only “speciality” here is helper method getPLByStatsType(order), which returns PL either in money, pips or %.

## Ret/DD Ratio – example of computing stats value from other values

Some stats values are computed as ratios of other stats values. An example of this is Ret/DD ratio which is simply a ratio between Net profit and Drawdown.

Here is its code:

```
public ReturnDDRatio() {
  super(L.t("Ret/DD Ratio"), DatabankColumn.Decimal2, ValueTypes.Maximize, 0, -20, 20);

  setDependencies("NetProfit", "Drawdown");
  setTooltip(L.t("Return / Drawdown Ratio"));
}

//------------------------------------------------------------------------

@Override
public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
  double netProfit  = stats.getDouble("NetProfit");
  double DD = Math.abs(stats.getDouble("Drawdown"));
  
  return round2(safeDivide(netProfit, DD));
}
```

By calling *setDependencies(“NetProfit”, “Drawdown”);* in the ReturnDDRatio constructor we told StrategyQuant that this value depends on two other starts values that have to be computed first.

In the compute() method we simply retrieved these values and returned net profit divided by drawdown.
