# Multiple orders to the same direction

- Source: <https://strategyquant.com/doc/strategyquant/multi-orders-to-same-direction/>
- Section: StrategyQuant X › How to...
- Fetched: 2026-09-04

---

This issue is related to manually creating your strategy or editing default strategy template in AlgoWizard.

StrategyQuant by default doesn’t use multiple orders to the same direction in one strategy, but you are able to add multiple EnterAtMarket or EnterAtStop/Limit to your strategy in AlgoWizard editor.

The reason why it is not used in StrategyQuant is that it is complicated to make work correctly and not fully supported on all the trading platfrms.

## MetaTrader support for multiple orders

MetaTrader 4/5 uses concept of MagicNumbers that uniquely identify an order and allow checking and manipulating it. If you use different MagicNumber for every EnterAtXXX order then it will work correctly.

If however you use the same MagicNumber for multiple EnterAtXXX orders then EA cannot recognize which order he exits belong to because there will be multiple orders with the same MagicNumber. Because of this exits for these independent orders will not work correctly because they will be applied from all orders.

So in order to use multiple EnterAtXXX in the same direction in MetaTrader4/5 it is necessary to use unique MagicNumber for every EnterAtXXX action.

## Tradestation / MultiCharts support for multiple orders

Unfortunately, Tradestation / MultiCharts DOESN’T support multiple independent orders to the same direction WITh independent exits. It is possible to name an entry in TS/MC, but it is not possible to manage exits independently for separate entries.

For this reason strategy with multiple EnterAtXXX to the same direction will not work correctly – exits of different orders (in the same direction) will be applied to all the orders. You shouldn’t use multiple entries to the same direction unless you know exactly what you are doing.

## Solution for Scaling In feature in StrategyQuant

Scaling In means opening multiple orders to the same direction. It is a feature that we will be adding to StrategyQuant in the following builds and there will be a special handling that will handle the restrictions described above.
