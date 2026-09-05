# Exporting data from Tradestation – recommendation

- Source: <https://strategyquant.com/doc/strategyquant/exporting-data-from-tradestation-recommendation/>
- Section: StrategyQuant X › Reliable backtesting & trading
- Fetched: 2026-09-04

---

Working with external futures data is tricky because we have to be careful about sessions and other things, especially when you export data from external platform like Tradestation and import it into SQX.

Below is the set of recommendations that ensure the data and thus also backtest results will be the same in SQX and Tradestation.

### Exporting futures data from Tradestation

1. Make sure you export **M1** data
2. Session should be **Regular**
3. Time zone should be **Exchange**
4. For bar building, use: **Session hours**

[![](https://strategyquant.com/wp-content/uploads/2023/03/ts_data_export.jpg)](https://strategyquant.com/wp-content/uploads/2023/03/ts_data_export.jpg)

[![](https://strategyquant.com/wp-content/uploads/2023/03/ts_data_export2.jpg)](https://strategyquant.com/wp-content/uploads/2023/03/ts_data_export2.jpg)

Double check that your chart setting is like this.

If you use different than recommended values for any of these settings it will have important consequences and your backtests between SQX and Tradestation might not match.

Session should be Regular so that the full day data are exported. When you’ll be attaching strategy to chart in Tradestation you can then chose your target timeframe and different session – just make sure you use the same also in SQX..

### Importing data to SQX

When creating symbol in SQX Data Manager for the new data don’t forget to configure the symbol to have **Timestamp as end of bar**:

[![](https://strategyquant.com/wp-content/uploads/2023/03/ts_data_export3.jpg)](https://strategyquant.com/wp-content/uploads/2023/03/ts_data_export3.jpg)

then just import the minute data as usual.

The new data imported to SQX with these recommendation can be the used in SQX normally – you can use them to compute higher timeframes and specify the proper sessions in SQX Trading Options and the bars computation and results between SQX and Tradestation should match.
