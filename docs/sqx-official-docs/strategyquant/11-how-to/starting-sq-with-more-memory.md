# Starting StrategyQuantX with more memory

- Source: <https://strategyquant.com/doc/strategyquant/starting-sq-with-more-memory/>
- Section: StrategyQuant X › How to...
- Fetched: 2026-09-04

---

StrategyQuant determines the maximum amount of RAM memory to use on the first start by its default algorithm – it uses a portion of your available RAM.

Because SQX is based on Java it is unable to allocate more memory than the maximum configured amount, so if you want to give it access to more memory you have to configure this maximum manually.

In case you run into an **Out of memory error** or you want to **manually increase the amount of memory** available to SQX you can do it conveniently from the UI:

1. open the program configuration by clicking on an icon on top right corner
2. then switch to Memory tab and configure the amount of RAM for SQX
3. restart SQ for these settings to take effect

[![](https://strategyquant.com/wp-content/uploads/2021/09/sq-memory-configuration-750x535.png)](https://strategyquant.com/wp-content/uploads/2021/09/sq-memory-configuration-750x535.png)

It is recommended to use 80-100% of your real memory, for example if you have 16 GB RAM you can set 12, 1 or 16 GB for SQ.

StrategyQuant X will not necessarily use all the configured memory, this is only a maximum value.
