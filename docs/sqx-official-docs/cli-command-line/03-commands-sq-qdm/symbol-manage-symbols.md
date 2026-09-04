# -symbol Manage symbols

- Source: <https://strategyquant.com/doc/cli-command-line/symbol-manage-symbols/>
- Section: CLI (command line) › Commands (SQ & QDM)
- Fetched: 2026-09-04

---

Arguments:

**action**: Performs the specific action [list,add,edit,delete,clear]

**symbols**: List of symbols

**instrument**: (optional) Symbol instrument

**bartype**: (optional) Bar type [startofbar, endofbar] (startofbar)

**datatype**: (optional) Data type, [M1,TICK] (M1)

**datasource**: (optional) Data source, [dukascopy,file,darwinex,crypto,yahoo] (dukascopy)

**exchange**: (optional) Exchange, [getExchanges()] (Binance)

**postfix**: (optional) Data postfix

Examples:

```
sqcli.exe -symbol action=list
sqcli.exe -symbol action=add symbols=EURUSD,GBPUSD datasource=dukascopy datatype=TICK
sqcli.exe -symbol action=edit symbol=EURUSD name=EURUSD_OLD
sqcli.exe -symbol action=delete symbols=EURUSD,GBPUSD
sqcli.exe -symbol action=clear symbols=EURUSD,GBPUSD
```
