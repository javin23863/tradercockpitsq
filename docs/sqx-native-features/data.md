# Data settings

**Ingested:** 2026-09-04  
**Source:** https://strategyquant.com/doc/strategyquant/data/  
**SQX tab:** Builder › Full settings › Data  
**Kind:** `write`

Configure symbol, timeframe, and date range for strategy backtests.

## From official docs

- **Trading Engine:** choose backtesting engine (MetaTrader 4, MetaTrader 5, etc.).
- Select symbol, timeframe, and date range; multi-TF/multi-symbol strategies need settings per additional chart.

## Integration

- **Typical artifact:** Builder project XML (Data); chart/symbol bindings
- **Widget:** Trading engine, sq-data-box symbol search/type/recent cloud, timeframe, date range (auto-rewritten from installed-data dates when the saved range cannot be used; Reset dates clamps to the symbol span), session, test precision, commission/swap gear dialogs over existing XML, OOS ranges plus `data/getSymbolData` graph when `showGraph` is true. Chart `symbol` and Setup `session` come from official `/main/getData` (`data.data` and `sessions`), the same load Electron uses after `constants/getAll`. Setup `testPrecision`, data types, swap types, and triple-swap days stay on `constants/getAll`. Commission `Method@type` comes from official `constants/listCommissionMethods`. Fail closed when StrategyQuant X web is down. Money-management `Method@type` is not this list. Does not invent Commission Param rows or a missing Swap element.
