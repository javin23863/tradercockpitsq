# List of available commands

- Source: <https://strategyquant.com/doc/quantdatamanager/quant-data-manager-command-line-interface-help/>
- Section: QuantDataManager › Command line
- Fetched: 2026-09-04

---

**Please note – starting with QuantDataManager Build 119 command line interface was updated to the new version.  
You can find new [CLI docummentation here](../../cli-command-line/01-introduction/introduction-to-cli.md).**

The documentation below is valid only for older versions of QuantDataManager.

Command line interface for QuantDataManager and StrategyQuant was developed for automation of data management.

Now you can call QDM from command line with some options (see below) and automatically download or manage symbols.

Note:

This feature is available from build 117. If you are using older build, please upgrade to higher version of  [Quant Data Manager](https://strategyquant.com/quantdatamanager)

**Available commands:**

**-a Add symbols.**  
Options:  
symbols: Symbols to add  
[instrument]: Symbol instrument  
[bartype]: Bar type [startofbar, endofbar] (startofbar)  
[datatype]: Data type, [M1,TICK] (M1)  
[datasource]: Data source, [dukascopy,file,darwinex,crypto,yahoo] (dukascopy)  
[exchange]: Exchange, [Binance,Bitfinex,Coinbase,Poloniex] (Binance)  
[postfix]: Data postfix  
[logfile]: Path of log file  
Example:

```
QDataManager_console.exe -a symbols=EURUSD,GBPUSD datasource=dukascopy datatype=TICK
```

**-e Edit symbol.**  
Options:  
symbol: Symbol to edit  
[name]: New symbol name  
[instrument]: Symbol instrument  
[bartype]: Bar type [startofbar, endofbar]
[logfile]: Path of log file  
Example:

```
QDataManager_console.exe -e symbol=EURUSD name=EURUSD_OLD
```

**-d Delete symbols.**  
Options:  
symbols: Symbols to delete  
[logfile]: Path of log file  
Example:

```
QDataManager_console.exe -d symbols=EURUSD,GBPUSD
```

**-c Clear symbols data.**  
Options:  
symbols: Symbols data to clear  
[logfile]: Path of log file  
Example:

```
	QDataManager_console.exe -c symbols=EURUSD,GBPUSD
```

**-l List symbols.**  
Options:  
csv: Path of csv file to export  
[logfile]: Path of log file  
Example:

```
QDataManager_console.exe -l
```
```
QDataManager_console.exe -l csv=C:/data/symbols.csv
```

**-ia Add instrument.**  
Options:  
instrument: Instrument to add  
[description]: Instrument description (“”)  
[pointvalue]: Point value (100000)  
[ticksize]: Pip/Tick size (0.0001)  
[tickstep]: Pip/Tick step (0.00001)  
[defaultspread]: Default spread (2)  
[datatype]: Data type, [stock,futures,forex,cfds,etf,index,crypto] (forex)  
[logfile]: Path of log file  
Example:

```
QDataManager_console.exe -ia instrument=EURUSD
```

**-ie Edit instrument.**  
Options:  
instrument: Instrument to edit  
[description]: Instrument description (“”)  
[pointvalue]: Point value  
[ticksize]: Pip/Tick size  
[tickstep]: Pip/Tick step  
[defaultspread]: Default spread  
[datatype]: Data type, [stock,futures,forex,cfds,etf,index,crypto]
[logfile]: Path of log file  
Example:

```
QDataManager_console.exe -ie instrument=EURUSD datatype=forex
```

**-id Delete instruments.**  
Options:  
instruments: Instruments to delete  
Example:

```
QDataManager_console.exe -id instruments=EURUSD
```

**-il List instruments.**  
Options:  
symbols: Symbols data to clear  
[logfile]: Path of log file  
Example:

```
QDataManager_console.exe -il
```
```
QDataManager_console.exe -il csv=C:/data/instruments.csv
```

**-u Update all data.**  
Example:

```
QDataManager_console.exe -u
```

**-di Import data from file.**  
Options:  
symbol: Symbol to import  
filepath: Path of file to import  
[instrument]: Symbol instrument  
[bartype]: Bar type [startofbar, endofbar]
[errorhandling]: Data errors handling [stop,ignore]
[timezone]: Imported data timezone. To list the available timezones, use command -tz [Etc/UCT, Europe/London, America/New\_York…]
[timeframe]: Imported timeframe [auto,Intraday,TICK,M1,M5,M15,M30,H1,H4,D1]
[logfile]: Path of log file  
Example:

```
QDataManager_console.exe -di symbol=EURUSD instrument=EURUSD filepath=C:/data/EURUSD.csv
```

**-de Export data to csv file.**  
Options:  
symbols: Symbols to export  
timeframe: Timeframe to export [TICK,M1,M5,M15,M30,H1,H4,D1]
[datefrom]: Date from in format “yyyy.MM.dd”  
[dateto]: Date to in format “yyyy.MM.dd”  
[outputdir]: Target directory (C:/Users/Tomas/workspaceSQ4/SQ4/work\_directory/StrategyQuant/export)  
[prefix]: File prefix (“”)  
[format]: Format, [Generic tick format (comma delimited),Generic bar format (comma delimited),Generic tick format (tab delimited),Generic bar format (tab delimited),MetaTrader4 tick format,MetaTrader4 bar format,Amibroker bar (aqi) format,Amibroker tick (aqi) format,Birt’s CSV2FXT format,Forex Tester bar format,Forex SB bar format,Ninja Trader tick format,Ninja Trader bar format,Neuroshell Trader format,Tradestation bar format] (MetaTrader4 bar format)  
[logfile]: Path of log file  
Example:

```
QDataManager_console.exe -de symbols=EURUSD_M1,GBPUSD_M1 timeframe=M1 datefrom=2018.01.01 dateto=2018.12.31 outputdir=C:/data format="Generic tick format (comma delimited)"
```

**-dc Clone data.**  
Options:  
symbols: Symbols to clone  
[postfix]: Data postfix (\_{timeframe}\_{cloneTime})  
[removeWeekends]: Remove weekends [true,false] (false)  
[timezone]: Timezone to clone. To list the available timezones, use command -tz [Etc/UCT, Europe/London, America/New\_York…]
[hours]: Fixed shift in hours [stop,ignore]
[logfile]: Path of log file  
Example:

```
QDataManager_console.exe -dc symbol=AUDCAD hours=8
```

**-tz List available time zones.**  
Options:  
[logfile]: Path of log file  
Example:

```
QDataManager_console.exe -tz
```

**List of vailable time zones.**

If you are clonning data into diffrent timezone you need to use the second parametr. For example EETUS

Timezone,Timezone name  
(EST+07),EETUS  
(UTC+02),EET  
(UTC),Etc/UCT  
(UTC),Europe/London  
(UTC-05),America/New\_York  
(UTC-12),Etc/GMT+12  
(UTC-11),Etc/GMT+11  
(UTC-10),Pacific/Honolulu  
(UTC-09),America/Anchorage  
(UTC-08),America/Los\_Angeles  
(UTC-08),America/Vancouver  
(UTC-07),America/Phoenix  
(UTC-07),America/Chihuahua  
(UTC-07),America/Denver  
(UTC-06),America/Chicago  
(UTC-06),America/Winnipeg  
(UTC-06),America/Mexico\_City  
(UTC-06),America/Regina  
(UTC-05),America/Bogota  
(UTC-05),America/New\_York  
(UTC-05),America/Indiana/Indianapolis  
(UTC-04:30),America/Caracas  
(UTC-04),America/Asuncion  
(UTC-04),America/Halifax  
(UTC-04),America/Cuiaba  
(UTC-04),America/Manaus  
(UTC-04),America/Santiago  
(UTC-03:30),America/St\_Johns  
(UTC-03),America/Sao\_Paulo  
(UTC-03),America/Argentina/Buenos\_Aires  
(UTC-03),America/Cayenne  
(UTC-03),America/Cayenne  
(UTC-03),America/Montevideo  
(UTC-03),America/Montevideo  
(UTC-02),Etc/GMT+2  
(UTC-01),Atlantic/Azores  
(UTC-01),Atlantic/Cape\_Verde  
(UTC),Africa/Casablanca  
(UTC),Atlantic/Reykjavik  
(UTC+01),Europe/Vienna  
(UTC+01),Europe/Prague  
(UTC+01),Europe/Paris  
(UTC+01),Europe/Warsaw  
(UTC+01),Africa/Brazzaville  
(UTC+01),Africa/Windhoek  
(UTC+02),Asia/Amman  
(UTC+02),Europe/Athens  
(UTC+02),Asia/Beirut  
(UTC+02),Africa/Cairo  
(UTC+02),Asia/Damascus  
(UTC+02),Africa/Harare  
(UTC+02),Europe/Helsinki  
(UTC+02),Europe/Istanbul  
(UTC+02),Asia/Jerusalem  
(UTC+02),Europe/Kaliningrad  
(UTC+02),Africa/Tripoli  
(UTC+03),Asia/Baghdad  
(UTC+03),Asia/Kuwait  
(UTC+03),Europe/Minsk  
(UTC+03),Europe/Moscow  
(UTC+03),Africa/Nairobi  
(UTC+03:30),Asia/Tehran  
(UTC+04),Asia/Muscat  
(UTC+04),Asia/Baku  
(UTC+04),Europe/Samara  
(UTC+04),Asia/Tbilisi  
(UTC+04),Asia/Tbilisi  
(UTC+04),Asia/Yerevan  
(UTC+04:30),Asia/Kabul  
(UTC+05),Asia/Tashkent  
(UTC+05),Asia/Yekaterinburg  
(UTC+05),Asia/Karachi  
(UTC+05:30),Asia/Kolkata  
(UTC+05:30),Asia/Kolkata  
(UTC+05:45),Asia/Kathmandu  
(UTC+06),Asia/Dhaka  
(UTC+06),Asia/Dhaka  
(UTC+06),Asia/Novosibirsk  
(UTC+06:30),Asia/Rangoon  
(UTC+07),Asia/Bangkok  
(UTC+07),Asia/Krasnoyarsk  
(UTC+08),Asia/Urumqi  
(UTC+08),Asia/Irkutsk  
(UTC+08),Asia/Kuala\_Lumpur  
(UTC+08),Australia/Perth  
(UTC+08),Asia/Taipei  
(UTC+08),Asia/Ulaanbaatar  
(UTC+09),Asia/Tokyo  
(UTC+09),Asia/Seoul  
(UTC+09),Asia/Yakutsk  
(UTC+09:30),Australia/Adelaide  
(UTC+09:30),Australia/Darwin  
(UTC+10),Australia/Brisbane  
(UTC+10),Australia/Sydney  
(UTC+10),Pacific/Guam  
(UTC+10),Australia/Hobart  
(UTC+10),Asia/Magadan  
(UTC+10),Asia/Vladivostok  
(UTC+11),Asia/Vladivostok  
(UTC+11),Pacific/Noumea  
(UTC+12),Asia/Anadyr  
(UTC+12),Pacific/Auckland  
(UTC+12),Etc/GMT-12  
(UTC+12),Pacific/Fiji  
(UTC+13),Pacific/Tongatapu  
(UTC+13),Etc/GMT-13  
(UTC+14),Pacific/Kiritimati
