# -data Manage data

- Source: <https://strategyquant.com/doc/cli-command-line/data-manage-data/>
- Section: CLI (command line) › Commands (SQ & QDM)
- Fetched: 2026-09-04

---

Arguments:

**action**: Performs the specific action [update,import,export,clone,timezones]

**symbol**: Symbol to import

**filepath**: Path of file to import

**instrument**: (optional)Symbol instrument

**bartype**: (optional)Bar type [startofbar, endofbar]

**errorhandling**: (optional)Data errors handling [stop,ignore]

**timezone**: (optional) Timezone [Etc/UCT, Europe/London, America/New\_York…]

**timeframe**: (optional) Imported timeframe [auto,Intraday,TICK,M1,M5,M15,M30,H1,H4,D1]

**datefrom**: (optional) Date from in format ‘yyyy.MM.dd’

**dateto**: (optional) Date to in format ‘yyyy.MM.dd’

**outputdir**: (optional) Target directory

**prefix**: (optional) File prefix

**format**: (optional) Format, [#ExportAvailableCsvFormats#] (MetaTrader4 bar format)

**postfix**: (optional) Data postfix (\_{timeframe}\_{cloneTime})

**removeWeekends**: (optional) Remove weekends [true,false] (false)

hours: (optional) Fixed shift in hours [stop,ignore]

Examples:

```
sqcli.exe -data action=update
sqcli.exe -data action=update symbols=GBPUSD_M1
sqcli.exe -data action=import symbol=EURUSD instrument=EURUSD filepath=C:/data/EURUSD.csv
sqcli.exe -data action=export symbols=EURUSD_M1,GBPUSD_M1 timeframe=M1 datefrom=2018.01.01 dateto=2018.12.31 outputdir=C:/data
sqcli.exe -data action=clone symbols=AUDCAD hours=8
sqcli.exe -data action=timezones
```

Note – data work on existing symbols. Before you can import or update data for some symbol you have to create it using the **-symbol** command.
