# Settings – ATM

- Source: <https://strategyquant.com/doc/strategyquant/settings-atm/>
- Section: StrategyQuant X › Program screens
- Fetched: 2026-09-04

---

These settings allow you to use new feature of StrategyQuant X – Advanced Trade Management – which enables easy configuration of multiple exits of the strategy.

*Note – this new feature is still marked as EXPERIMENTAL, it will be available in new StrategyQuant X Build 130.*

The logic of multiple exits is simple:

- You enter the trade
- You close part of the trade quickly on a small profit to cover the costs and risk
- You then let the trade run to reach more distant target or targets – it is already risk free

This functionality can be achieved manually by editing your strategy (either code or in AlgoWizard), but now it is available in StrategyQuant to be used in a simple way, moreover, it can be applied to the already existing strategies.

Here is how the configuration could look like:

[![advanced-trade-management-in-sq](https://strategyquant.com/wp-content/uploads/2020/11/atm-config-in-sq-1024x609.png)](https://strategyquant.com/wp-content/uploads/2020/11/atm-config-in-sq-1024x609.png)

 The configuration of multiple exits is simple – you just turn it on and define your exits and size constraints required by proper money management.

In the example above we have two exits:

1. Close 50% of the position at 0.5 x original Profit Target size
2. Close remaining 50% of the position at 2 x original Profit Target

 

*Note –  ATM* ***WILL OVERRIDE*** *all other your exits except Stop Loss.* *So, the strategy WILL NOT use the original Profit Target, Trailing Stop or Exit after X bars, it will only have original Stop Loss + the exits you define in ATM.*

*Note 2 – you can use multiples of yor original SL or PT in your exits*

## ATM Size constraints

In order for ATM to know how big position to use for every exit you have to specify some boundaries:

[![](https://strategyquant.com/wp-content/uploads/2020/11/atm-size-constraints.png)](https://strategyquant.com/wp-content/uploads/2020/11/atm-size-constraints.png)

It tells ATM that position has one decimal place and minimum size is 0.1

So when you have two exits, each with 50% of the original position, it will know how to divide the position size computed by Money Management to each of the exits.

Note – ATM will use only so many exits that fit the Money Management size and ATM size constraints.

For example, if you have 0.1 fixed size in MOney Management, two exits each with 50% of position, and ATM minimum size is also 0.1 there is no way how 0.1 size computed by money management can be divided to 50% while keeping minimal size 0.1 for every exit.

So in this case only first exit with size 0.1 will be used.

## ATM config source

When you enable ATM you have two more options of which ATM to use:

[![](https://strategyquant.com/wp-content/uploads/2020/11/atm-config-source.png)](https://strategyquant.com/wp-content/uploads/2020/11/atm-config-source.png)

It can be either ATM saved in strategy (if there is any) or ATM settings defined in the settings on the screen – in this case they will be then saved to the strategy.

## Exit types and configuration

You can configure the size and type of each exit. Each exit can use either multiple of original Stop Loss or Profit Target, some fixed profit or trailing stop or time based target by number of bars.

[![](https://strategyquant.com/wp-content/uploads/2020/11/atm-exit-config.png)](https://strategyquant.com/wp-content/uploads/2020/11/atm-exit-config.png)

The results of using ATM will depend on the exact strategy and ATM settings. You will see different results with different ATM exits or on different strategies.

ATM is not a magic bullet, multiple exits will not always improve your strategy, but the potential is there.

From our preliminary testing it looks that using a very simple ATM configuration like in the examples above can improve 10-30% strategies right away, probably even more if we’d play with the multiplication coefficients.

This feature is new in StrategyQuant and we will be adding more articles and research on how to use it to obtain best results.
