# Portfolio Composer

- Source: <https://strategyquant.com/doc/strategyquant/portfolio-composer/>
- Section: StrategyQuant X › New features
- Fetched: 2026-09-04

---

Portfolio Composer is a new major feature and module in StrategyQuant X introduced in Build 141.  
Its main purpose is to allow you to simulate a portfolio of different strategies with the goal of finding the optimal composition of the portfolio for the best results.

In another words – it answers this real trader problem:

**Let’s say you have a broker account with $10,000 (or $30,000 or $100k) and a set of various strategies at your disposal.****Which strategies and with how much weight should you trade to have the best results?**

## What is the difference between Portfolio Composer and Portfolio Master?

They are similar in functionality, but not the same.

Older Portfolio Master is also a module that can simulate portfolio, and create an “optimal” portfolio composition from source strategies, using brute force or genetic approach simply combining source strategies to a portfolio and ranking the results..

But what sets Portfolio Composer apart is that it does not simulate only **portfolio composition** (which strategies should be included in the portfolio) but also their **WEIGHT – how much money to assign to each strategy** for optimal portfolio results.

It will recompute the position sizing according to the given weight of the strategy.

As an example, it can tell you to trade Strategy A with double the risk and Strategy B with half the risk, compared to the original money management.

Portfolio Composer was created with Stockpicker and stock strategies in mind, but it can be used also for other assets.

## How to use Portfolio Composer?

It is simple – go to the Portfolio Composer module in SQX and load the strategies you are considering for the portfolio to the grid on the left panel.

Then you can select which strategies to combine into the portfolio and with what weight.

The weight tells the Composer how to recompute the position size for the strategy.

If weight=100% it means the strategy trades with original money management.  
If weight=200% it trades with double (2x) the original money management.  
If weight=50%, it trades with half the position management.

**Real examples:**

1. You have Strategy A with Money management: Risk 10% of the account. If you use weight=100%, it will risk 10% of the account.  
   If you use weight=200%, it will risk 2×10%= 20% of the account per trade.  
   If you use weight=250%, it will risk 2.5×10%= 25% of the account per trade.

1. You have Strategy A with Money management: Risk $1000 per order. If you use weight=100%, it will risk $1000.  
   If you use weight=200%, it will risk 2x$1000= $2000 of the account per trade.  
   If you use weight=250%, it will risk 2.5x$1000= $2500 of the account per trade.

Recomputing the size according to weight is not all that is done in Portfolio Composer simulation.

It takes into account also your configuration of portfolio **Account balance and leverage**.  
So if you have a broker account with size $10,000 and leverage 2, you can effectively trade with $20,000.

**Most importantly, it also considers free margin in the portfolio simulation.**Every day when new trades are opening it will open only as many trades as your free margin allows.

If you set higher weights – meaning that your strategy will trade with bigger orders – it is possible that **some days it will be not able to open the orders because your account doesn’t have enough free margin** at that given day.  
In this case the strategy will not open new orders at this given day, and a next strategy in the portfolio is considered.

## Portfolio simulation results

After you choose which strategies and with which weights to use, click on the **Recompute portfolio** button and the new portfolio will be simulated.

The new portfolio details will be shown on the right panel – you can see a standard Overview, List of trades and Equity chart.

There is also a special PortfolioComposer Log that shows its exact actions every day – which orders it took, how it modified the trading size, and which orders it skipped because of low free margin.
