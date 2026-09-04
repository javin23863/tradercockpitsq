# Settings – Data

- Source: <https://strategyquant.com/doc/strategyquant/data/>
- Section: StrategyQuant X › Program screens
- Fetched: 2026-09-04

---

This is where you can configure the symbol, timeframe, and time range on which the strategies will be backtested.

[![SQ Data settings](https://strategyquant.com/wp-content/uploads/2019/01/settings_data.png)](https://strategyquant.com/wp-content/uploads/2019/01/settings_data.png)

## Trading Engine

allows you to choose which of the available backtesting engines (MetaTrader4 / MetaTrader 5 etc.) should be used for testing the strategy.

## Backtest data settings

choose the symbol, timeframe and date range on which the strategies will be tested.  
If you use multi-TF or multi-symbol strategies you must select symbol and timeframe for each additional chart:

[![SQ Backtest data settings](https://strategyquant.com/wp-content/uploads/2019/01/backtest_data_setting.png)](https://strategyquant.com/wp-content/uploads/2019/01/backtest_data_setting.png)

## Test parameters

### ![SQ test parameters config](https://strategyquant.com/wp-content/uploads/2019/01/test_params.png)

### Testing precision

means how the price is data simulated during the backtest.  
It is usually sufficient to use Selected Timeframe mode for Market orders, or Tick simulation mode for Stop/Limit orders.  
You can also use faster mode for generation, and then slower mode for strategies retest.

#### Test precision types:

- **Selected Timeframe only**  
  it is the fastest testing mode. It uses only the main timeframe to simulate the prices, it creates four “ticks” – at bar Open, High, Low and Close values.  
  This results in a very fast backtesting with acceptable accuracy for a quick preview. However, for Stop or Limit orders the testing accuracy might not be sufficient, and you should try more precise mode.
- **1 Minute data**slower testing mode, it uses minute data (if available) to simulate price changes during the testing. It creates 4 ticks every minute, and thus simulates also movement within the bar.
- **Real Tick – custom spread**this mode uses Bid value from real tick data (if available) for exact price simulation, but it computes Ask value using the custom spread you specify. It is slower than the other modes and it should be used for final verification of new strategies.
- **Real Tick – real spread**this mode uses real tick data for exact price simulation with real Bid and Ask prices. It is much slower than the other modes and it should be used for final verification of new strategies.

### Spread

You can specify spread used in backtest – it is a difference between Bid and Ask price.

### Slippage

Simulated slippage in pips/ticks – it will add the specified number of pips/ticks to the opening and closing price.

### Min distance

Minimum distance of price – most MetaTrader brokers have limitation how close to the actual price they allow you to place stop or limit pending order

### Commission & swaps

clicking on this link will open a new popup dialog, where you can configure these additional settings.

[![](https://strategyquant.com/wp-content/uploads/2019/01/commissions-swap.png)](https://strategyquant.com/wp-content/uploads/2019/01/commissions-swap.png)

Swaps in SQX can be set in **points, money** or **percent**. Set acording to your broker setting for a specific instrument

Following formulas are used to calculate swap cost in SQX:

**Swap in points:** swap cost = lot size x SQ point value (contract size) x MT point value (digits or SQ pip step) x ***swap rate (in points)** x* days in trade

**Swap in money:** swap cost = lot size x ***swap rate (USD, EUR, etc)*** x days in trade

**Swap in percent:** swap cost = lot size x SQ point value (contract size) x trade entry price x *(**swap rate %** / 100 / 360)* x days in trade

## Data range parts

[![SQ Out of Sample Training Validation data part types](https://strategyquant.com/wp-content/uploads/2019/01/settings_data_parts.png)](https://strategyquant.com/wp-content/uploads/2019/01/settings_data_parts.png)

Allows you to split the history data to multiple parts of different types:

- **In Sample Training (IST)** – this is the same as In Sample that we had until now. Genetic evolution uses this part to determine fitness and rank the strategies in population.
- **In Sample Validation** **(ISV)** – a new part in SQ X that is used to determine if strategy performance in IST part holds also in ISV part.  
  In machine learning it is used to determine if models trained on Training set (IST) holds also in Validation set.  
  In SQ X it can be used to restart genetic evolution when fitness stagnates in this part.
- **Out of sample** – this is as same as before, it represents an “unknown” part of data that was not part of the evolution
- **No Trade**–  special part that means that strategy will not trade in this part. It can be used for example to skip a part in the middle of data that has low volatility.

Check out more about the data parts types in our blog article [Data parts – what they are and how they could be used?](https://strategyquant.com/blog/training-validation-test-periods)
