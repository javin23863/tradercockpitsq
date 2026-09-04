# Test strategy in MetaTrader 4 with tick precision

- Source: <https://strategyquant.com/doc/quantdatamanager/test-strategy-metatrader-4-tick-precision/>
- Section: QuantDataManager › How to...
- Fetched: 2026-09-04

---

New Strategy Quant X Data Manager as well as QuantDataManager now allows you to export special FXT & HST files for MetaTrader 4 that allow you to test your strategies in MT4 with the highest possible modeling quality.

It does it by exporting tick data from StrategyQuant and using them in MT4 Strategy Tester.

## Step 1: Export FXT & HST data to MetaTrader4

Go to Data manager, choose some tick data and click on Export to MT4 (FXT & HST) button on the toolbar.

[![](https://strategyquant.com/wp-content/uploads/2018/12/e1.png)](https://strategyquant.com/wp-content/uploads/2018/12/e1.png)

This will open a new dialog where you must specify more details.

First of all you should specify the date range to be exported.

If you are doing it for the very first time, you also must specify the path to your MetaTrader4 installation. SQ X needs it so that it generates the files in the appropriate folders.

The last thing is to choose the correct MT4 symbol and its setting on the lower half of the dialog.  
The symbol is usually recognized automatically, but if you use some exotic forex symbol you might need to choose the symbol yourself and even perhaps export the actual settings of your MetaTrader. This is explained more in the Troubleshooting section.

[![](https://strategyquant.com/wp-content/uploads/2018/12/e2.png)](https://strategyquant.com/wp-content/uploads/2018/12/e2.png)

When everything is configured properly click on Start export. Data Manager will now start generation of FXT & HST files that will be used in MT4 Strategy Tester to achieve high quality backtest.

> **Note! You must turn off MetaTrader before export, otherwise there might be errors in the exported files.**

## Step 2: Start MetaTrader and run strategy backtest

Start your MetaTrader, open Strategy Tester, choose some strategy to be tested and the symbol you just exported. You can leave the Use date unchecked – it will perform test on the whole exported data.  
When your backtest finishes go to Report and you should see that the backtest was made with 99% modeling quality.

#### Possible issue – Custom MT4 settings or missing symbol

When you use some exotic symbol or you want to use exactly the same trading settings as in your MT4 installation you can use our script to export your MT4 symbols details.

To do that you have to run our specila script that exports MT4 symbol detalt to a file for use in Data manager.

The custom script is in folder **{SQ installation}/custom\_indicators/MetaTrader4/Scripts**, it is named **ExportProperties.mq4**

Copy this script to your **{MT4 Data folder}/MQL4/Scripts** folder and restart MetaTrader.

Now you must start the script on any open chart. You can do this by going back to MetaTrader, finding the script in the Navigator and drag & drop it to some chart.

**IMPORTANT NOTE: Make sure that your MetaTrader is connected to the broker, otherwise it wouldn’t work!**

When the script finishes it will export the properties of configured symbols to a file **{MT4 Data folder}/MQL4/Files/mt4.properties**

Copy this file to some folder where you can easily find it later.

Now in the Step 1 where you configure your export click on the lin**k Load other MT4 data specification file** and find your file.

[![](https://strategyquant.com/wp-content/uploads/2018/12/e4.png)](https://strategyquant.com/wp-content/uploads/2018/12/e4.png)

It will load your newly generated file with the current properties for every symbol in your MetaTrader. Then just chose the right symbol, click Export and it is done.

### Possible issue – Backtest doesn’t produce trades or there are errors in the Journal

Backtest doesn’t produce trades or there are many errors in the Journal.

[![](https://strategyquant.com/wp-content/uploads/2018/12/e5.png)](https://strategyquant.com/wp-content/uploads/2018/12/e5.png)

Most probable reason is that the properties of the symbol used during FXT export don’t match settings of your broker. You have to export your own symbol properties from MT4 and use them in Data manager using the steps described in **Possible problem 1.**

### Possible issue – Modelling quality is n/a

Sometimes the modeling quality in MT4 results page isn’t shown as 99%, but as n/a.

[![](https://strategyquant.com/wp-content/uploads/2018/12/e6.png)](https://strategyquant.com/wp-content/uploads/2018/12/e6.png)

This is less serious than it seems, it means that MetaTrader is unable to determine the modeling quality. We haven’t found a reason why it sometimes happens, it seems it is related to particular MT4 installation.

However, as we see in the picture above, **despite modelling quality being n/a there are no errors, and the whole modeling bar is green.**  
**So it means the backtesting worked on real tick data with no errors,** it is a problem of MetaTrader that it cannot compute the correct modelling quality for some reason.

It is usually an issue in the particular MetaTrader installation, another installation of MetaTrader running on the same FXT data will return 99% modeling quality.

[![](https://strategyquant.com/wp-content/uploads/2018/12/e7.png)](https://strategyquant.com/wp-content/uploads/2018/12/e7.png)

Above is a screenshot of two different MT4 installations testing the same strategy on the same data.  
First has modeling quality 99%, second hs n/a, but as you can see the ticks modelled and strategy results are exactly the same.

To sum it up – **you don’t need to worry about n/a modeling quality if there are no chart errors and the whole bar is green.**
