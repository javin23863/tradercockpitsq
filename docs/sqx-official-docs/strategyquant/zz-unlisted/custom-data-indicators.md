# External indicators

- Source: <https://strategyquant.com/doc/strategyquant/custom-data-indicators/>
- Section: StrategyQuant X
- Fetched: 2026-09-04

---

This functionality is available from Build 127. Please also note that this functionality was renamed from Custom Data Indicators to External Indicators in latest builds.

If you have an indicator you want to use in StrategyQuant, but it is not yet supported there, you have two options:

1. **Implement the custom indicator in Java** in StrategyQuant CodeEditor – this requires some programming knowledge. Indicator added this way will be working just like any other indicator that SQ supports
2. **Import the custom indicator as a data** – this way StrategyQuant can use the indicator without knowing how exactly it computed its values.  
   This is simpler, but it has some limitations – you must export the indicator values from your trading platform using a special script, and then import them to SQ. Moreover, the data will be valid only for the symbol and timeframe you computed them on.For example, if you compute your custom indicator on EURUSD / H1 and you’d want to use it for GBPUSD / H1 or even EURUSD / M30 then you have to reexport the data from your trading platform.

This documentation is about option 2. – importing indicators as data.

***Note!***

*Custom indicators imported as data are computed in MetaTrader, Tradestation or your other trading platform, not in SQ. To get the correct results you must use the same history data in both StrategyQuant and MT4.  
It will not work to compute your custom indicator in MetaTrader with your broker data and then use them in StrategyQuant on data from other source.  Before using custom indicators, synchronize your history data so that both SQ and MT4 use the same history*

In this example we’ll implement RVI indicator which is a standard example indicator in MT5 platform:

*![MT5 RVI indicator](https://strategyquant.com/wp-content/uploads/2020/04/mt5_rvi.png)*

## Step 1: Define your new external indicator in StrategyQuant

Go to **Data manager** -> **External indicators** tab and click on **Add new**.

It will open a new indicator dialog where you’ll specify properties the indicator you are adding:

[![Add custom data indicator](https://strategyquant.com/wp-content/uploads/2020/04/ci_add.png)](https://strategyquant.com/wp-content/uploads/2020/04/ci_add.png)

#### **Name**

is the indicator name under which you’ll see it in SQ. It must be unique and cannot contain any special  
characters.  
Because custom indicator is computed specifically for one symbol and timeframe, it is a good practice to set use symbol and TF also in its name, like in our case: RVI\_EURUSD\_H1.

#### **Return type**

is a type of return value indicator computes, it is used by StrategyQuant to properly match this custom indicator to other building blocks in the program, so that it compares price with price, and not for example price with CCI value. It can be either:

- **Number** – if it is indicator such as CCI, RSI, MACD etc.
- **Price** – if the indicator value is price, like moving average or Bollinger Bands.
- **Price range** – if the indicator value is price range (difference between two prices), such as ATR or Bollinger Bands Range.
- **Signal** – if you want to use the indicator as a custom signal block then use this option. You can use signal indicator for example to recognize candle patterns or to implement your own simple trading rules.

#### How to decide the proper return type

generally, if the indicator draws its lines on the same chart as price, then its return type is price.  If it draws its lines into a separate window below the main chart then it is Number, except for special indicators like ATR that compute price difference or range.

#### **Indicator values (lines)**

these are the values indicator outputs. If we’ll check the screenshot of RVI indicator below you can see that it outputs two values in Data window, and they correspond to RVI (green) and Signal (red) line on the chart.

The values must be defined in the same order as they were created in the indicator.

Then there’s an interesting part – you can define the source code for every trading platform for every line. This is not necessary for the indicator working in SQ, but it is required for SQ to know how to generate strategy source code for each platform.

You don’t need to define sources for all three platforms (MT4, MT5, EasyLanguage). If you are using only MT5, it is enough to specify only that code.

The code is how you’d use this indicator in your trading platform code, for MetaTrader it is usually something like:

*iCustom(NULL, 0, “RVI”, 10, 0, #Shift#)*

Note that you can use **#Shift#** constant for MT4 code – it will be replaced by the actual Shift of the building block during code generation.

Shift in MetaTrader5 is used in a different way and doesn’t need to be computed.

Click **OK** and the new indicator will be created:

[![](https://strategyquant.com/wp-content/uploads/2020/04/ci_added.png)](https://strategyquant.com/wp-content/uploads/2020/04/ci_added.png)

It cannot be used yet, because this is only a definition – we need to import data to this indicator so that SQ can use it.

## Step 2: Getting the values from MetaTrader

Custom indicators work in a way that StrategyQuant uses their values computed in another program, for example in MetaTrader. This means we must compute the indicator in MetaTrader and then import its value(s) to SQ.

This is little bit complicated, but on the other hand it allows us to use virtually any indicator available for MetaTrader, even if we don’t know how exactly it is computed.

For computation of indicator values we’ll use a simple EA called SqIndicatorValuesExportEA.

This is custom EA from StrategyQuant that can be used to export indicator values from MetaTrader. You can find this file in folder **{StrategyQuant}custom\_indicatorsMetaTrader5Experts**

In order to use it in your MetaTrader you must copy it there. Start your MetaTrader 5, go to File ->Open Data Folder. This will open MT5 data folder. There go to MQL5Experts copy this EA there.

Then restart your MetaTrader and the new EA will be available.

Before we can use it, we must modify it so that it exports values of indicator **RVI** just like we want.

Open **MT5** -> **Tools** -> **MetaQuotes Language Editor** and find and open this new EA from Experts folder.

The code is relatively simple to use, you must make changes in a few places:

1. In OnInit you have to specify export file name and iCustom code:

```
int OnInit() {
  …

    // this is the export file name. It is a good practice to include also symbol and timeframe
    fileName = "EURUSD_H1_RVI_10.csv"; 
   
    // function to get indicator handle
    indicatorHandle = iCustom(NULL, 0, "ExamplesRVI", 10);
```

2. In OnTick you must update which values (indicator lines) will be computed and exported:

```
void OnTick()
   …
   indicatorBufferIndex = 0;
   if(!FillArraysFromBuffers(IndicatorBuffer, indicatorHandle, 2)) return;

   indicatorBufferIndex = 1;
   if(!FillArraysFromBuffers(SignalBuffer, indicatorHandle, 2)) return;
```

If indicator has multiple lines, like in the case of RVI you must repeat the lines:

```
   indicatorBufferIndex = NUMBER;
   if(!FillArraysFromBuffers(BUFFER_NAME, indicatorHandle, 2)) return;
```

for every line.

Where NUMBER is a number of line starting from 0 and BUFFER\_NAME is name of the buffer array.  
There are two buffer arrays defined in the beginning of the file:

```
double IndicatorBuffer[];
double SignalBuffer[];
```

you can add more if indicator has more than two lines.

3. Further in OnTick you must update which FileWrite method and add the buffers to be exported to the end:

```
FileWrite(handle, currentTime, rt[0].open, rt[0].high, rt[0].low, rt[0].close, rt[0].tick_volume, IndicatorBuffer[0], SignalBuffer[0]);
```

This is all, now you can compile the EA, it is ready to be used.

### Running the export EA in MetaTrader

When the file SqIndicatorValuesExportEA is modified and compiled without problems we can run it to export the values.

Go to MT5 Strategy Tester, choose this Expert and select symbol and timeframe for which you want to export the indicator data.

[![MT5 Indicator Export](https://strategyquant.com/wp-content/uploads/2020/04/ci_mt5_export-1024x271.png)](https://strategyquant.com/wp-content/uploads/2020/04/ci_mt5_export-1024x271.png)

You must choose symbol and timeframe for which the indicator should be computed, as well as the appropriate date range.

Then press **Start** and wait for the export to finish.

Once the EA test finishes, the export file should be available in **MT5 Data Folder**, but there is a trick. The generated file is not in your Terminal folder, but in Tester folder.

When you open Data Folder from MT5, it will show you your Terminal folder:

[![MT5 terminal folder](https://strategyquant.com/wp-content/uploads/2020/04/data_foldeR_terminal.png)](https://strategyquant.com/wp-content/uploads/2020/04/data_foldeR_terminal.png)

But the file is not there. You have to go up to Tester, and there go to your testing agent/MQL5/Files to find the exported file:

[![MT5 Tester Folder](https://strategyquant.com/wp-content/uploads/2020/04/data_folder_tester-1024x108.png)](https://strategyquant.com/wp-content/uploads/2020/04/data_folder_tester-1024x108.png)

Now you can copy & paste the file to some more convenient destination when you’ll find it.

## Step 3: Import indicator values into StrategyQuant

When we have the indicator data ready, we can import them to our custom indicator in StrategyQuant.

Go to **Data manager** -> **External indicators** tab, select your new indicator and click on **Import indicator data**.

There find the file and specify the import columns.

If you’ll review the EA, the indicator data were exported as the last two values:

```
FileWrite(handle, currentTime, rt[0].open, rt[0].high, rt[0].low, rt[0].close, rt[0].tick_volume, IndicatorBuffer[0], SignalBuffer[0]);
```

So choose the last two columns as **Value1** and **Value2**.

Click on **Start import** and indicator should be successfully imported.

[![SQ cusotm indicator imported](https://strategyquant.com/wp-content/uploads/2020/04/ci_imported.png)](https://strategyquant.com/wp-content/uploads/2020/04/ci_imported.png)

Now the indicator is ready to be used in SQ.

## Step 4: Using the external indicator

When he indicator is successfully imported it can be used like any other building block. Go to **Builder** -> **Settings** -> **Building blocks** and you should see our new **RVI** indicator there:

[![SQ cusotm data indicator building block](https://strategyquant.com/wp-content/uploads/2020/04/ci_bb-1024x840.png)](https://strategyquant.com/wp-content/uploads/2020/04/ci_bb-1024x840.png)

**If you don’t see it,** please check if you have the correct timeframe in **Settings** -> **Data**.

*Remember, indicator data are always computed for a specific timeframe, and they cannot be used in another timeframe. If you switch the timeframe for example to M30, you’ll see only external indicators that were imported for M30 data.*

When we have the building blocks configured we can start the build.

[![SQ](https://strategyquant.com/wp-content/uploads/2020/04/sq_builder-1024x867.png)](https://strategyquant.com/wp-content/uploads/2020/04/sq_builder-1024x867.png)

After a while it will fill the databank with new strategies that use our new external indicator.

When we’ll open some strategy, we can check the pseudo code:

[![SQ custom data indicator pseudo code](https://strategyquant.com/wp-content/uploads/2020/04/sq_pseudo.png)](https://strategyquant.com/wp-content/uploads/2020/04/sq_pseudo.png)

We can see that this strategy comparison of our RVI\_EURUSD\_H1 indicators.

When we’ll switch Source code to MetaTrader 5 it will generate valid MT5 code that can be compiled and executed in MT5:

[![SQ custom data indicator MT5 code](https://strategyquant.com/wp-content/uploads/2020/04/sq_mt5.png)](https://strategyquant.com/wp-content/uploads/2020/04/sq_mt5.png)

You can also check step-by-step video where our Senior programmer shows how to code your custom indicator. Video link: <https://www.youtube.com/watch?v=veiqIh0BS10&t=26s&ab_channel=StrategyQuant>
