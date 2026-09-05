# -tools Tools

- Source: <https://strategyquant.com/doc/cli-command-line/tools-tools/>
- Section: CLI (command line) › Commands (SQ only)
- Fetched: 2026-09-04

---

Arguments:

**action**: Performs the specific action [orderstocsv,orderstoxlsx]

**file**: Path of the file or folder

**usecomma**: (optional) Path of the output file or folder

**data**: (optional) Data [main,all]

Example:

```
sqcli.exe -tools action=orderstocsv file="Strategy 0.1487.sqx"
sqcli.exe -tools action=orderstoxlsx file="Strategy 0.1487.sqx"
sqcli.exe -tools action=orderstocsv file=C:/reports output=C:/trades
sqcli.exe -tools action=orderstocsv file="Strategy 0.1487.sqx" usecomma=true data=main
```
