# Monte Carlo retest methods

- Source: <https://strategyquant.com/doc/strategyquant/monte-carlo-retest-methods/>
- Section: StrategyQuant X › Cross checks - robustness tests
- Fetched: 2026-09-04

---

This is another type of Monte Carlo simulations, in this case it simulates random changes in properties that **require the strategy to be retested** – such as changes in spread, slippage, strategy parameters, or history data.

Because every simulation requires a complete backtest this cross check could take long time.  
It the backtest on main data took let’s say 0.5 seconds, and you want to run 100 simulations in this cross check, you can expect it would take 100 x 0.5 = 50 seconds for every strategy where it is applied.

Some of the methods available are:

**Randomize Starting Bar** – this will test the strategy behavior when the testing starts on a different starting bar. It is obvious that a good strategy cannot be sensitive to which bar you start the test.

**Randomize Strategy Parameters** – every strategy uses parameters, such as period of an indicator or constant that is used in comparison. This test checks the sensitivity of the strategy to a small change of parameter value. Probability of change is a probability that any parameter changes its value. Max parameter change is the maximum percentage to which the parameter changes its value. For example if you set Max parameter change to 10%, then a parameter with value 60 can be randomly changed to a range 54 – 66 (+- 10% of its original value of 60).

**Randomize History Data** – one very common case of curve fitting is when strategy is too dependent on history data. This option checks the behavior of the strategy to a change in history data.

The Probability of change sets for every bar how probable it is that open, high, low or close price will be changed. The Max price change is a percentage value of the change in relation to ATR (Average True Range).

So if for example close price is randomly chosen to be changed, ATR value is 10 pips, and Max price change is 20%, then the price can change by +- 2 pips

**Please, note that you can set only these confidence levels: 50,60,70,80,90,92,95,97,98,99,100, other levels does not produce any results.**
