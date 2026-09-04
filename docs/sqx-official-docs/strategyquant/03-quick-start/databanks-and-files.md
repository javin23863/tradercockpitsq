# Databanks and files

- Source: <https://strategyquant.com/doc/strategyquant/databanks-and-files/>
- Section: StrategyQuant X › Quick start
- Fetched: 2026-09-04

---

Strategies in StrategyQuant are saved in their own proprietary file format (with .SQ X extension) that can be opened only by StrategyQuant X. If you find potentially good strategies you should always save them so that you can work with them later.

## Databank

is the most important concept you should understand when using StrategyQuant. It doesn’t matter which mode you use, strategies are always stored in databanks.

Databanks are on the bottom side of the screen, the view can be minimized or opened.

[![Minimized databank](https://strategyquant.com/wp-content/uploads/2019/01/databank.png)](https://strategyquant.com/wp-content/uploads/2019/01/databank.png)

**![Open databank](https://strategyquant.com/wp-content/uploads/2019/01/open_databank.png)**

All the strategies that program produces or is working with are in one of the databanks. There you can sort the strategies by its properties, load and save them, and when you double click on a row, it will open the strategy test details in the detailed Results window.

## **Running strategies in your trading platform – MetaTrader, Tradestation, etc.**

MetaTrader cannot read the strategy .SQ X files. If you want to test or run your new strategies in MetaTrader you have to export the strategy to MQL source code.

Please check the section [How To … Export strategy from StrategyQuant and test it in MetaTrader](../11-how-to/export-strategy-strategyquant-test-trade-metatrader.md).

Please note that exported (\*.MQ4) files are not readable by StrategyQuant, so make sure you always save your strategy also as a normal strategy file (\*.SQ X)
