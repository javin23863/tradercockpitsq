# -gui Starts webserver to access GUI remotely

- Source: <https://strategyquant.com/doc/cli-command-line/gui-starts-webserver-to-access-gui-remotely/>
- Section: CLI (command line) › Commands (SQ & QDM)
- Fetched: 2026-09-04

---

Arguments: none

Example:

```
sqcli.exe -gui
```

CLI is a fully featured StrategyQuant, only without user interface running. You can use it to run CLI commands .

To access also SQ UI using your browser start this command. You’ll be then able to access SQ UI on your browser using an URL: **http://localhost:8080**

Note

If you run SQX in the CLI mode for the first time (before the license was verified) you need to run it like this:

```
sqcli.exe license=XXXXX
```

where XXXXX is your valid SQX license. This will verify the license using CLI and you can then start SQX normally using -gui command
