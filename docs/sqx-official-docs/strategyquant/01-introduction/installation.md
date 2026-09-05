# Installation

- Source: <https://strategyquant.com/doc/strategyquant/installation/>
- Section: StrategyQuant X › Introduction
- Fetched: 2026-09-04

---

## Installation

**Important – Please DON’T install StrategyQuant to standard C:Program Files directory !**

It might not work correctly because Windows security settings don’t allow the program to write to its data files.

Instead install it to any normal drive or directory on the disk, like C:StrategyQuant  
or C:TradingStrategyQuant

To install StrategyQuant just download the setup program from our **<https://strategyquant.com/download/>** page and follow the instructions.

Alternatively, you can download it as a ZIP archive and just extract it to your selected folder.

## Important – Steps after installation

there are few things you should do after installation depending on trading platform you use.

Some of the technical indicators or functions used in StrategyQuant X are implemented specifically by us to implement them in a standard way to remove some edge cases that caused difference in backtesting.  
All these additional indicators are a part of the installation package, you can find them in **{StrategyQuant}/custom\_indicators** folder.

**It is important to add all SQ X custom indicators to your trading platform !**

### MetaTrader

Simply copy all the \*.mq4 or \*.mq5 files from this folder to your MetaTrader installation.

How to find the correct MetaTrader folder to copy the indicators to:

1. Open MetaTrader
2. Go to File -> Open Data Folder – this will open MT folder in explorer window
3. There go to MQL4 or MQL5, then to Indicators subfolder
4. Copy all SQ custom indicators there
5. SQ indicators will be compiled and available in MetaTrader after you restart it

If you use more than one MetaTrader installations, you have to repeat this step for every MT4 installation you use or will use in the future.

### Tradestation

Open Tradestation, go to **File -> Import/Export EasyLanguage** and choose **Import EasyLangauge File (ELD, ELS or ELA)**.  
then find the file: **{StrategyQuant}/custom\_indicators/Tradestation/SQ.eld** and import it.

This will import all the additional functions StrategyQuant uses, so your new strategies will run in Tradestation.

### MultiCharts

Open MultiCharts PowerLanguage editor, go to **File -> Import..**  
then find the file: **{StrategyQuant}/custom\_indicators/Tradestation/SQ\_MC.pla** and import it.

This will import all the additional functions StrategyQuant uses, so your new strategies will run in MultiCharts.
