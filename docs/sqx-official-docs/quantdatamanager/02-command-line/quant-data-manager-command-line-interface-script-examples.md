# Script Examples (Windows)

- Source: <https://strategyquant.com/doc/quantdatamanager/quant-data-manager-command-line-interface-script-examples/>
- Section: QuantDataManager › Command line
- Fetched: 2026-09-04

---

**Please note – starting with QuantDataManager Build 119 command line interface was updated to the new version.  
You can find new [CLI docummentation here](../../cli-command-line/01-introduction/introduction-to-cli.md).**

The documentation below is valid only for older versions of QuantDataManager.

In this article, we would like to show you examples of Quant Data Manager Automation via command line interface. We will show you how to use script for:

Dukascopy quick data start script, update data script, import data from files and clone to your broker timezone.

Note:

This feature is available from build 117. If you are using older build, please upgrade to higher version of  [Quant Data Manager](https://strategyquant.com/quantdatamanager)

**Dukascopy fast setup script will download data and clone to the UTC+2 US DST. All files should be stored in C:QuantDataManager including batch script dukas.bat**

```
@echo off

echo ======================================================================================================

for /F "tokens=1,2,3 delims=;" %%A in (C:QuantDataManagerdukas.txt) do (
   echo %DATE% %TIME%
   echo Adding Symbol : %%A Started
   QDataManager_console.exe -a symbols=%%A datasource=dukascopy datatype=M1
   echo Adding Symbol: %%A Finished 
   echo ===================================
)

QDataManager_console.exe -u

echo Download finished!

REM cloning part

echo ======================================================================================================

for /F "tokens=1 delims=;" %%A in (C:QuantDataManagerdukas.txt) do (
   echo %DATE% %TIME%
   echo Cloning Symbol : %%A Started
   QDataManager_console.exe -dc symbols=%%A timezone=EETUS postfix=_M1_UTC2
   echo Cloning Symbol: %%A Finished 
   echo ===================================
)

echo Clone finished!
```

**Create config file with name dukas.txt in C:QuantDataManager directory with list of symbols which you want to download**

```
AUDCHF
AUDJPY
AUDUSD
CADCHF
CADJPY
CHFJPY
EURAUD
EURCAD
EURCHF
EURGBP
EURJPY
EURUSD
GBPAUD
GBPCAD
GBPCHF
GBPJPY
GBPUSD
USDCAD
USDCHF
USDJPY
```

**Script for updating data:  Create dukas\_update.bat file and  use -u parametr in script.**

```
QuantDataManager_console.exe -u
```

Script will update all Dukascopy imported data.

**Second Example How to import Asirikuy data via command line with adding symbols. All files should be stored in C:QuantDataManager**

```
@echo off

echo ======================================================================================================

for /F "tokens=1,2,3 delims=;" %%A in (C:QuantDataManagerasirikuy.txt) do (
   echo %DATE% %TIME%
   echo Adding Symbol : %%A Started
   QDataManager_console.exe -a symbols=%%A instrument=%%B datasource=file datatype=M1
   echo Adding Symbol: %%A Finished 
   echo ===================================
)

REM import data part
echo ======================================================================================================

for /F "tokens=1,2,3 delims=;" %%A in (C:QuantDataManagerasirikuy.txt) do (
   echo %DATE% %TIME%
   echo Import Symbol : %%A Started
   QDataManager_console.exe -di symbol=%%A instrument=%%B timeframe=M1 timezone=Europe/Prague bartype=startofbar errorhandling=ignore filepath=C:QuantDataManager%%C
   echo Import Symbol: %%A Finished 
   echo ===================================
)

echo Import finished!

REM cloning part into UTC+2 timezone

echo ======================================================================================================

for /F "tokens=1 delims=;" %%A in (C:QuantDataManagerasirikuy.txt) do (
   echo %DATE% %TIME%
   echo Cloning Symbol : %%A Started
   QDataManager_console.exe -dc symbols=%%A timezone=EETUS postfix=_UTC2
   echo Cloning Symbol: %%A Finished 
   echo ===================================
)

echo Clonning finished!
```

**Create config file with name asirikuy.txt in C:QuantDataManager directory:**

In the file there are variables divided by semicolon. First column is name of imported symbol, second column is name of symbol without suffix and last column is the name of imported files in CSV format.

```
AUDCHF_M1_UTC1_as;AUDCHF;AUDCHF_1_MT4.csv
AUDJPY_M1_UTC1_as;AUDJPY;AUDJPY_1_MT4.csv
AUDUSD_M1_UTC1_as;AUDUSD;AUDUSD_1_MT4.csv
CADCHF_M1_UTC1_as;CADCHF;CADCHF_1_MT4.csv
CADJPY_M1_UTC1_as;CADJPY;CADJPY_1_MT4.csv
CHFJPY_M1_UTC1_as;CHFJPY;CHFJPY_1_MT4.csv
EURAUD_M1_UTC1_as;EURAUD;EURAUD_1_MT4.csv
EURCAD_M1_UTC1_as;EURCAD;EURCAD_1_MT4.csv
EURCHF_M1_UTC1_as;EURCHF;EURCHF_1_MT4.csv
EURGBP_M1_UTC1_as;EURGBP;EURGBP_1_MT4.csv
EURJPY_M1_UTC1_as;EURJPY;EURJPY_1_MT4.csv
EURUSD_M1_UTC1_as;EURUSD;EURUSD_1_MT4.csv
GBPAUD_M1_UTC1_as;GBPAUD;GBPAUD_1_MT4.csv
GBPCAD_M1_UTC1_as;GBPCAD;GBPCAD_1_MT4.csv
GBPCHF_M1_UTC1_as;GBPCHF;GBPCHF_1_MT4.csv
GBPJPY_M1_UTC1_as;GBPJPY;GBPJPY_1_MT4.csv
GBPUSD_M1_UTC1_as;GBPUSD;GBPUSD_1_MT4.csv
USDCAD_M1_UTC1_as;USDCAD;USDCAD_1_MT4.csv
USDCHF_M1_UTC1_as;USDCHF;USDCHF_1_MT4.csv
USDJPY_M1_UTC1_as;USDJPY;USDJPY_1_MT4.csv
```
