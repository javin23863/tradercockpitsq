# -instrument Manage instruments

- Source: <https://strategyquant.com/doc/cli-command-line/instrument-manage-instruments/>
- Section: CLI (command line) › Commands (SQ & QDM)
- Fetched: 2026-09-04

---

Arguments:

**action**: Performs the specific action [list,add,edit,delete]

**instrument**: Instrument to add

**description**: (optional) Instrument description

**pointvalue**: (optional) Point value (100000)

**ticksize**: (optional) Pip/Tick size (0.0001)

**tickstep**: (optional) Pip/Tick step (0.00001)

**defaultspread**: (optional) Default spread (2)

**datatype**: (optional) Data type, [stock,futures,forex,cfds,etf,index,crypto] (forex)

Examples:

```
sqcli.exe -instrument action=list
sqcli.exe -instrument action=add instrument=EURUSD
sqcli.exe -instrument action=edit instrument=EURUSD datatype=forex
sqcli.exe -instrument action=delete instruments=EURUSD
```
