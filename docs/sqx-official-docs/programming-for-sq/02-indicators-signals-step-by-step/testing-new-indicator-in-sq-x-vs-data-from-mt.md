# Testing new indicator in SQ X vs data from MT

- Source: <https://strategyquant.com/doc/programming-for-sq/testing-new-indicator-in-sq-x-vs-data-from-mt/>
- Section: Programming for StrategyQuant X › Indicators / Signals step-by-step
- Fetched: 2026-09-04

---

## **Testing new indicator in SQ X vs data from MT**

we just created (or we are in the process of developing) our new indicator. How do we know that we implemented it correctly?

When you created your new indicator in StrategyQuant and it was successfully compiled, it should be also verified if it really works in the same way as in MetaTrader – in other words, if the values computed by it are the same as its values in MT4.

**Note – this article was a part of [Envelopes indicator example](adding-envelopes-indicator-step-by-step.md), so it refers to the Envelopes indicator. But the description of Indicators testing is valid for any indicator implementation.**

For this, we have an Indicators Tester tool in Code Editor. It works simply by comparing the values computed in SQ with values computed in MT4.

In general, it works in a few simple steps:

1. Use helper script to compute and export indicator data in MetaTrader
2. Copy the computed data files to proper location so SQ can find them
3. Configure and run indicator test in SQ

### Use helper script to compute and export indicator data in MetaTrader

As the first step we must prepare MT4 testing data – we must compute the indicator on a number of bars and save its computed values into a file.

For this we provide a simple EA that you can use – it is located in **{****SQ}/custom\_indicators/MetaTrader4/Experts/SqIndicatorValuesExportEA.mq4**

Add this EA to your MetaTrader, modify it to compute and output value of your indicator and run it in MT4 StrategyTester on any data – to make an optimal test it should run on at least 1000 bars.

Because Envelopes has two output buffers we have to run it two times – once for Upper, once for Lower buffer.

Here’s a modified code of this helper export script that computes Envelopes indicator:

```
//+------------------------------------------------------------------+
//|                                   SQ_IndicatorValuesExportEA.mq4 |
//|                                                                  |
//|                    EA to export indicator values from MetaTrader |
//|                Output to: /{Data folder}/tester/files/******.csv |
//+------------------------------------------------------------------+

#property copyright "Copyright © 2019 StrategyQuant"
#property link      "https://strategyquant.com"

string currentTime = "";
string lastTime = "";

//+------------------------------------------------------------------+

int start() {
   currentTime = TimeToStr(Time[1], TIME_DATE|TIME_MINUTES|TIME_SECONDS);
   if(currentTime == lastTime) {
      return(0);
   }
   
   double value;

   // change the file name below
   string fileName = "Envelopes_14_0_0_0_0.1_upper.csv";

   int handle = FileOpen(fileName, FILE_READ | FILE_WRITE, ";");
   if(handle>0) {
      FileSeek(handle,0,SEEK_END);

      // here is the indicator value 
      value = iEnvelopes(NULL, 0 , 14 , 0 , 0 , 0 , 0.1 , 1 , 1); // upper value
      //value = iEnvelopes(NULL, 0 , 14 , 0 , 0 , 0 , 0.1 , 2 , 1); // lower value
      
      FileWrite(handle, TimeToStr(Time[1], TIME_DATE|TIME_MINUTES|TIME_SECONDS), Open[1], High[1], Low[1], Close[1], Volume[1], value);
      FileClose(handle);
   }

   lastTime = currentTime;
   return(0);
}
```

it will compute the Envelopes inicator on MT4 by calling its internal method iEnvelopes() with proper parameters.

Please note that parameters can be configured – you don’t need to use default ones. It is a good practice to incude parameter values also in the name the output data file – as in our case “Envelopes\_14\_0\_0\_0\_0.1\_upper.csv”, so that we know it was generated with these parameters.

Now we can run this script two times in MT Tester:

[![](https://strategyquant.com/wp-content/uploads/2019/03/tester_ea-1024x324.jpg)](https://strategyquant.com/wp-content/uploads/2019/03/tester_ea-1024x324.jpg)

When it finishes it should create data file with computed indicator values as well as Open, High, Low, Close prices for every bar. The file will be saved in MetaTrader4 -> **{Data folder}/tester/files/Your\_FILE\_NAME.csv**

We should see two files there:  
*Envelopes\_14\_0\_0\_0\_0.1\_upper.csv*  
*Envelopes\_14\_0\_0\_0\_0.1\_lower.csv*

### Copy the computed data files to proper location so SQ can find them

Copy these files to a folder **{SQ installation}/tests/Indicators/MetaTrader4**

Create this folder if it doesn’t exists, SQ Indicaotrs Tester looks for files in this folder.

Now we have the data file prepared, let’s start a test in StrategyQuant.

### Configure and run indicator test in SQ

Co to Code Editor and click on Test indicators on the toolbar.

[![](https://strategyquant.com/wp-content/uploads/2019/03/testindy1-1024x625.jpg)](https://strategyquant.com/wp-content/uploads/2019/03/testindy1-1024x625.jpg)

It will open Indicator Tester dialog, click on **Add new test**. Add Envelopes indicators to the test, for both Upper and Lower output.

[![](https://strategyquant.com/wp-content/uploads/2019/03/testindy2.jpg)](https://strategyquant.com/wp-content/uploads/2019/03/testindy2.jpg)

You’ll see it in the table as follows. The last think we need to do is to modify the **Test file name**s according to the actual test data file names created in previous step, and optionally also **Test parameters**, if you used other than default ones:

[![](https://strategyquant.com/wp-content/uploads/2019/03/testindy3.jpg)](https://strategyquant.com/wp-content/uploads/2019/03/testindy3.jpg)

When it is done click on **Start** button to run the tests.

In ideal case tests will be successful and values computed in SQ will match values computed in MT4:

[![](https://strategyquant.com/wp-content/uploads/2019/03/testindy4.jpg)](https://strategyquant.com/wp-content/uploads/2019/03/testindy4.jpg)

If something fails there will be differences between values from SQ and MT4. You can click on the differences mesage to see them in the list:

[![](https://strategyquant.com/wp-content/uploads/2019/03/testindy5.jpg)](https://strategyquant.com/wp-content/uploads/2019/03/testindy5.jpg)

If the test fails, first check if the test data generated in MT4 were using the same indicator parameters, there can be a mistake in that.

If the test data are correct, then there is something wrong with your SQ implementation of your indicator – it works differently than its MT4 counterpart and must be corrected.
