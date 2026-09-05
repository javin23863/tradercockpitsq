# Loading history data in snippets

- Source: <https://strategyquant.com/doc/programming-for-sq/loading-history-data-in-snippets/>
- Section: Programming for StrategyQuant X › Coding sessions examples
- Fetched: 2026-09-04

---

**Important notes**

- **This example will work only in SQ Build 136 Dev 2 or later**
- **Data loading will not work for protected data (SQ Futures and SQ Equities), where we are not allowed to give access to the raw data to the users by contract with data provider.**

This example will show how you can access historical data programmatically from your snippets. This is useful if you want to compare for example the resulting orders with some trends in historical data.

This functionality uses a new [HistoryDataLoader](https://strategyquant.com/sqxapi/com/strategyquant/lib/HistoryDataLoader.html) class, that is available only since SQ Build 136 Dev2.

This example is made in a form of Custom Analysis snippet, but you can use this functionality in any snippet. Just realize that loading the data can be slow, the call of the loading function can take several seconds.

So it is not advisable to use it in snippets that are called very often and must be fast, for example databank column metrics.

## How to use HistoryDataLoader to load data

The usage is very simple. The class [HistoryDataLoader](https://strategyquant.com/sqxapi/com/strategyquant/lib/HistoryDataLoader.html) has only one method [get(symbol, timframe, dateFrom, dateTo, session)](https://strategyquant.com/sqxapi/com/strategyquant/lib/HistoryDataLoader.html#get(java.lang.String,java.lang.String,long,long,java.lang.String)).

You just have to call it with the correct parameters and it will return object [HistoryOHLCData](https://strategyquant.com/sqxapi/com/strategyquant/lib/HistoryOHLCData.html) with the requested data.

```
try {
    Log.info("Loading started ...");
    HistoryDataLoader loader = new HistoryDataLoader();

    HistoryOHLCData data = loader.get("EURUSD_M1", TimeframeManager.TF_H1, SQTime.toLong(2007,1, 1), SQTime.toLong(2020, 12, 31), Session.NoSession);

} catch(HistoryDataNotAvailableExeption e) {
    Log.error("Error loading data.", e);
}
```

It is important to surround this method with try-catch, because it can return [HistoryDataNotAvailable](https://strategyquant.com/sqxapi/com/strategyquant/lib/HistoryDataNotAvailableExeption.html) exception.

## Accessing data in HistoryOHLCData object

The data is returned in [HistoryOHLCData](https://strategyquant.com/sqxapi/com/strategyquant/lib/HistoryOHLCData.html) object. It is a simple object, where the data are stored in primitive arrays that you can normally go through.

An example:

```
// we use this to get the length of the data
int dataLength = data.Time.length;

Log.info("Loaded data length: {}", dataLength);

Log.info("Printing out first 5 records: Index, Time, Open, High, Low, Close");
// go through data
for(int i=0; i<dataLength; i++) {
    long time = data.Time[i];
    float open = data.Open[i];
    float high = data.High[i];
    float low = data.Low[i];
    float close = data.Close[i];
    float volume = data.Volume[i];

    if(i<5) {
        Log.info("#{} - {}, {}, {}, {}, {}", i, SQTime.toDateMinuteString(time), open, high, low, close);
    }
}
```

You can see that the data object has arrays for Time, Open, High, Low, Close and Volume. All the arrays are of the same size.

And that’s it – very simple.

## How to get symbol and timeframe from strategy

One useful thing to do is retrieving symbol, timeframe and date ranges from strategy last backtest ([ResultsGroup](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/ResultsGroup.html) object) so that you know what data to load.

It is equally simple, an example:

```
/**
 * Example on how to retrieve symbol, TF and from-to date ranges from strategy.
 * You can then later use it to load history data.
 * @param resultsGroup
 */
private void recognizeSymbolTFFromStrategyExample(ResultsGroup resultsGroup) {
    if(resultsGroup == null) {
        return;
    }

    try {
        SettingsMap settings = resultsGroup.specialValues();

        String symbol = resultsGroup.mainResult().getString(SpecialValues.Symbol, "N/A");
        String tf = resultsGroup.mainResult().getString(SpecialValues.Timeframe, "N/A");

        long from = settings.getLong(SpecialValues.HistoryFrom);
        long to = settings.getLong(SpecialValues.HistoryTo);

        Log.info("\nExample - Strategy last backtest setting: {} / {}, from: {}, to: {}", symbol, tf, SQTime.toDateString(from), SQTime.toDateString(to));
    } catch(Exception e) {
        Log.error("Exception", e);
    }
}
```

You can then use these values in HistoryDataLoader.get() method to retrieve exactly the data that were used in strategy backtest.

## Full code of LoadHistoryData example snippet

```
package SQ.CustomAnalysis;

import com.strategyquant.lib.*;

import java.util.ArrayList;

import com.strategyquant.tradinglib.results.SpecialValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;
import com.strategyquant.datalib.session.*;

public class LoadHistoryData extends CustomAnalysisMethod {

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    
    public LoadHistoryData() {
        super("LoadHistoryData", TYPE_PROCESS_DATABANK);
    }
    
    //------------------------------------------------------------------------
    
    @Override
    public boolean filterStrategy(String project, String task, String databankName, ResultsGroup rg) throws Exception {
        return true;
    }
    
    
    //------------------------------------------------------------------------
    
    @Override
    public ArrayList<ResultsGroup> processDatabank(String project, String task, String databankName, ArrayList<ResultsGroup> databankRG) throws Exception {
        // simple example of using HistoryDataLoader to load historical data for given symbol and timeframe
        try {
            HistoryDataLoader loader = new HistoryDataLoader();

            Log.info("Loading started ...");

            HistoryOHLCData data = loader.get("EURUSD_M1", TimeframeManager.TF_H1, SQTime.toLong(2007,1, 1), SQTime.toLong(2020, 12, 31), Session.NoSession);

            // we use this to get the length of the data
            int dataLength = data.Time.length;

            Log.info("Loaded data length: {}", dataLength);

            Log.info("Printing out first 5 records: Index, Time, Open, High, Low, Close");
            // go through data
            for(int i=0; i<dataLength; i++) {
                long time = data.Time[i];
                float open = data.Open[i];
                float high = data.High[i];
                float low = data.Low[i];
                float close = data.Close[i];
                float volume = data.Volume[i];

                if(i<5) {
                    Log.info("#{} - {}, {}, {}, {}, {}", i, SQTime.toDateMinuteString(time), open, high, low, close);
                }
            }

        } catch(HistoryDataNotAvailableExeption e) {
            Log.error("Error loading data.", e);
        }

        // another useful example - how to get symbol/TF from the strategy ?
        if(databankRG.size() > 0) {
            recognizeSymbolTFFromStrategyExample(databankRG.get(0));
        }

        return databankRG;
    }

    //------------------------------------------------------------------------

    /**
     * Example on how to retrieve symbol, TF and from-to date ranges from strategy.
     * You can then later use it to load history data.
     * @param resultsGroup
     */
    private void recognizeSymbolTFFromStrategyExample(ResultsGroup resultsGroup) {
        if(resultsGroup == null) {
            return;
        }

        try {
            SettingsMap settings = resultsGroup.specialValues();

            String symbol = resultsGroup.mainResult().getString(SpecialValues.Symbol, "N/A");
            String tf = resultsGroup.mainResult().getString(SpecialValues.Timeframe, "N/A");

            long from = settings.getLong(SpecialValues.HistoryFrom);
            long to = settings.getLong(SpecialValues.HistoryTo);

            Log.info("\nExample - Strategy last backtest setting: {} / {}, from: {}, to: {}", symbol, tf, SQTime.toDateString(from), SQTime.toDateString(to));
        } catch(Exception e) {
            Log.error("Exception", e);
        }
    }	
}
```
