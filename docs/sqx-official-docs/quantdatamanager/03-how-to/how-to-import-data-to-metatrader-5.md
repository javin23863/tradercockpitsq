# How to export data from Quant Data Manager and import to Metatrader 5

- Source: <https://strategyquant.com/doc/quantdatamanager/how-to-import-data-to-metatrader-5/>
- Section: QuantDataManager › How to...
- Fetched: 2026-09-04

---

Metatrader 5 now allows import custom symbols and import tick and minutes data.

#### STEP 1: Check detailed **[tutorial step by step](https://www.appdemostore.com/demo?id=6129955890003968)**how to download data from Dukascopy.

#### STEP 2: Export data from Quant Data Manager

1. Select symbol
2. click on export data for Metatrader 5

[![](https://strategyquant.com/wp-content/uploads/2018/12/ExportMT51-1-1024x555.png)](https://strategyquant.com/wp-content/uploads/2018/12/ExportMT51-1-1024x555.png)

#### **Export data from**Quant Data Manager

1. Select date range – all time range
2. Set correct spread in our case we are using fixed spread 8 points which is 0.8 pips. You can choose also compute spread from tick data. ***Please, note that in exported data you will see spread value in points not pips.***
3. Choose output path

[![](https://strategyquant.com/wp-content/uploads/2018/12/ExportMT53-1024x554.png)](https://strategyquant.com/wp-content/uploads/2018/12/ExportMT53-1024x554.png)

Please, note that **Metatrader 5 doesn’t support compute higher timeframe from tick data.** If you want to do it backtest with tick data precision, you always need to import both M1 and tick data. ***Compute for higher timeframes is available only from M1 data.***

#### Let’s Import data to Metatrader 5

**Step 1** – open Metatrader 5 and click with right mouse button to the market watch or press shortcut ctrl+U

[![](https://strategyquant.com/wp-content/uploads/2018/12/symbols_mt5.png)](https://strategyquant.com/wp-content/uploads/2018/12/symbols_mt5.png)

**Step 2** – create custom symbol. You can copy symbol setting from your broker.

[![Create custom symbol in Metatrader 5](https://c.mql5.com/2/30/Create_custom_symbol__1.PNG)](https://c.mql5.com/2/30/Create_custom_symbol__1.PNG)

**Step 3** – setup the symbol name and right spread to symbol settings

*note: if you are using variable spread from tick data you have to set up floating spread instead of fixed spread 8 points*

[![Copy symbol settings from broker](https://c.mql5.com/2/30/Create_custom_symbol2__1.PNG)](https://c.mql5.com/2/30/Create_custom_symbol2__1.PNG)

**Step 4 –** click on show symbol

[![show symbol in Metatrader 5](https://c.mql5.com/2/30/show_symbol__1.PNG)](https://c.mql5.com/2/30/show_symbol__1.PNG)

**Step  5 –** click on bars and import bars

[![imort fixed CSV to Metatrader 5](https://c.mql5.com/2/30/import_data__1.PNG)](https://c.mql5.com/2/30/import_data__1.PNG)

**Step 6** – choose CSV file with exported data from Quant Data Manager

[![choose csv file](https://c.mql5.com/2/30/import_data_choose_csv__1.PNG)](https://c.mql5.com/2/30/import_data_choose_csv__1.PNG)

now you can see progress about data importing

[![import progress](https://c.mql5.com/2/30/import_progress__1.PNG)](https://c.mql5.com/2/30/import_progress__1.PNG)

**Step 7** – if you will successful, you will see window with imported data. You should see white window as you can see on this screen.

*note: If the window with imported data will be red, the backtest will not be with 99 % data quality.*

[![imported data](https://c.mql5.com/2/30/imported_data__1.PNG)](https://c.mql5.com/2/30/imported_data__1.PNG)

### That’s it, happy backtesting 🙂

**Backtest from Metatrader 5 with 0 % quality modeling**

[![Backtest from Metatrader 5 with 0 % quliaty modeling](https://c.mql5.com/2/30/MT5_0__1.PNG)](https://c.mql5.com/2/30/MT5_0__1.PNG)

**Backtest from Metatrader 5 with 99 % quality modeling**

[![MT5 99 procent quality modelling backtest](https://c.mql5.com/2/30/MT5_99__1.PNG)](https://c.mql5.com/2/30/MT5_99__1.PNG)
