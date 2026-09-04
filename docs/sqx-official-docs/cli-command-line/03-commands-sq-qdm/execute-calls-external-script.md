# -execute Calls external script

- Source: <https://strategyquant.com/doc/cli-command-line/execute-calls-external-script/>
- Section: CLI (command line) › Commands (SQ & QDM)
- Fetched: 2026-09-04

---

Command that calls an external script or executable. This external program can then do some processing, produce results that can be then used again by CLI.

It can be combined with **-waitfor** command to wait for some file to be produced.

Arguments:

**file**: Path of the script

Examples:

```
sqcli.exe -execute file=C:/reports/evaluate.bat
sqcli.exe -waitfor
```
