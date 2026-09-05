# Settings – Money management

- Source: <https://strategyquant.com/doc/strategyquant/money-management/>
- Section: StrategyQuant X › Program screens
- Fetched: 2026-09-04

---

Money management (or position sizing) specifies how many lots or shares are traded on each trade.

StrategyQuant has flexible and extendable money management options that can be used in the program and later also in real trading in your trading platform.

[![StrategyQuant extendable money management options](https://strategyquant.com/wp-content/uploads/2019/02/money_management.png)](https://strategyquant.com/wp-content/uploads/2019/02/money_management.png)

Brief description of different money management types:

### Fixed size

strategy will trade with fixed number of lots. This is recommended setting when you generate new strategy, because it gives you clear overview of strategy real performance.

### Fixed amount

strategy will risk a fixed amount of money for every trade. This is basic money management without compounding. It can be used to test real performance of strategies where Stop Loss is based on volatility (ATR), or if you want to compare the performance of strategies with different Stop Loss.

### Risk fixed % of account

advanced money management that is recommended for real trading. The strategy will risk a given % of equity on every trade. This is simple, but very effective money management that will allow the strategy to increase number of lots as your account grows. It is generally recommended to risk maximum 2-5% of account equity per one trade.

### Risk fixed % of balance

Like Risk fixed % of account, but it uses account balance instead of equity.  
Account balance illustrates your closed trades Profit/Loss, while Equity is the real-time calculation of Profit/Loss, considering both open and closed. Positions.

### Stocks size by price

This position sizing method is used especially used for stocks. **Use account balance** forces SQ to use the current account balance instead of the initial capital set.

### Crypto size by price

Size computed as Balance / Asset Size. Position sizing method specifically for crypto trading – you have to specify the exact decimal numbers for rounding.

### Simple Martingale MM

Position sizing that uses Martingale approach to increase positions after a loss.

[![Strategyquant Simple Martingale Money Management](https://strategyquant.com/wp-content/uploads/2019/02/simple_martingale_mm.png)](https://strategyquant.com/wp-content/uploads/2019/02/simple_martingale_mm.png)
