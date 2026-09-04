# Using backtest settings in strategy metrics

- Source: <https://strategyquant.com/doc/programming-for-sq/using-backtest-settings-in-strategy-metrics/>
- Section: Programming for StrategyQuant X › Coding sessions examples
- Fetched: 2026-09-04

---

In this example we’ll look at how we can get and use current backtest settings – as an example size of spread – in computation of backtest metrics (databank columns).

This was requested by an user – the exact question was how he can get the spread setting used in the strategy backtest in order to compute a special new metric for the strategy.

## Way 1 – in DatabankColumn getValue() method

In this example you can see how it can work in principle. We can use method [getLastSettings()](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/ResultsGroup.html#getLastSettings()) of [ResultsGroup](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/ResultsGroup.html) that will return a String, which can then be transformed to XML element containing the structure of the backtest settings.

```
            String lastSettingsXml = results.getLastSettings();
            if(lastSettingsXml != null) {
                Element elSettings = XMLUtil.stringToElement(lastSettingsXml);
```

The XML structure of elSettings is beyond this article, you can review the XML in your project config .cfx file or strategy file (.sqx).

Both these files are ZIP archives that can be opened using WinZip or WinRar and you can then look at the project setting defined there.

The part that interests us is here:

```
<Settings>
  <Setups detailed="true">
    <Setup dateFrom="2003.5.5" dateTo="2018.08.30" testPrecision="1" session="No Session" slippage="1" minDist="0" engine="Stockpicker">
      <Chart symbol="EURUSD_M1" timeframe="H1" spread="2" />
      <Commissions>
```

We want to retrieve the spread, and we can do it like this:

```
Element elData = elSettings.getChild("Data");
Element elSetups = elData.getChild("Setups");
Element elSetup = elSetups.getChild("Setup");
Element elChart = elSetup.getChild("Chart");

double spread = Double.parseDouble(elChart.getAttributeValue("spread"));
```

Now that we have spread, we can just simply display it by converting it to String and returning it as a result of our [getValue()](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/DatabankColumn.html#getValue(com.strategyquant.tradinglib.ResultsGroup,java.lang.String,byte,byte,byte)) method – see the full code of the snippet below.

Note that we are also using results.[specialValues()](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/ResultsGroup.html#specialValues()).getDouble() and .set() to “cache” the retrieved spread value – it is so that we don’t need to parse the XML every time this metric is computed. This way it will be parsed only once per strategy.

### The disadvantage

The problem is that we are using a special method [DatabankColumn.getValue()](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/DatabankColumn.html#getValue(com.strategyquant.tradinglib.ResultsGroup,java.lang.String,byte,byte,byte)) and not the standard [DatabankColumn.compute()](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/DatabankColumn.html#compute(com.strategyquant.tradinglib.SQStats,com.strategyquant.tradinglib.StatsTypeCombination,com.strategyquant.tradinglib.OrdersList,com.strategyquant.lib.SettingsMap,com.strategyquant.tradinglib.SQStats,com.strategyquant.tradinglib.SQStats,com.strategyquant.tradinglib.Result)).

The difference is that getValue() method on databank column is called when the value is displayed in SQ UI, AFTER the strategy was inserted into databank, and it returns text, not a number.

This kind of databank column cannot be used in filtering and it cannot be used in computation of other databank columns.

We do it this way because in StrategyQuantX builds before Build 136 Dev 5 (which is released on 3rd November 2022) last settings were saved into the strategy at the time strategy was added to the databank, aftre all its metrics were computed.

Full code of the snippet:

```
package SQ.Columns.Databanks;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

import org.jdom2.Element;

public class Spread1 extends DatabankColumn {
    
    private static final String SpreadKey = "BacktestSpread";
    
    public Spread1() {
        super("Spread1", DatabankColumn.Text, ValueTypes.Minimize, 0, 0, 10000);

        setTooltip("Spread on which the test was made (as text)");
        setWidth(150);
        printsSpecialValue(true);
    }
    
  //------------------------------------------------------------------------

    @Override
    public String getValue(ResultsGroup results, String resultKey, byte direction, byte plType, byte sampleType) throws Exception {		
        try {
            if(results.specialValues().containsKey(SpreadKey)) {
                return results.specialValues().getDouble(SpreadKey)+"";
            }
            
            String lastSettingsXml = results.getLastSettings();
            if(lastSettingsXml != null) {
                Element elSettings = XMLUtil.stringToElement(lastSettingsXml);
                Element elData = elSettings.getChild("Data");
                Element elSetups = elData.getChild("Setups");
                Element elSetup = elSetups.getChild("Setup");
                Element elChart = elSetup.getChild("Chart");

                double spread = Double.parseDouble(elChart.getAttributeValue("spread"));
                results.specialValues().set(SpreadKey, spread);

                return Double.toString(spread);
            }

        } catch(Exception e) {
            Log.error("Exc.", e);
        }
        
        return NOT_AVAILABLE;
    }
}
```

## Way 2 – in DatabankColumn compute() method

**Note – this method will work only in SQX Build 136 Dev 5 (which is released on 3rd November 2022) and later.**

The main principle is the same, we only use [compute()](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/DatabankColumn.html#compute(com.strategyquant.tradinglib.SQStats,com.strategyquant.tradinglib.StatsTypeCombination,com.strategyquant.tradinglib.OrdersList,com.strategyquant.lib.SettingsMap,com.strategyquant.tradinglib.SQStats,com.strategyquant.tradinglib.SQStats,com.strategyquant.tradinglib.Result)) method for the databank column, which is called when strategy metrics are computed. This means that you’ll be able to use this value in filtering and you can get its value in other databank columns – you only need to configure the proper dependency using setDependencies() method.

This way the spread value is retrieved from last backtest settings, and stored in metrics.

Full code of the snippet:

```
package SQ.Columns.Databanks;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

import org.jdom2.Element;

public class Spread2 extends DatabankColumn {
    
  private static final String SpreadKey = "BacktestSpread";

    public Spread2() {
        super("Spread2", 
          DatabankColumn.Decimal2, // value display format
          ValueTypes.Minimize, // whether value should be maximized / minimized / approximated to a value   
          0, // target value if approximation was chosen  
          0, // average minimum of this value
          5); // average maximum of this value
        
    setWidth(80); // defaultcolumn width in pixels
    
        setTooltip("Spread on which the test was made (as number)");  
    }
  
  //------------------------------------------------------------------------

    @Override
    //public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
    public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort, Result result) throws Exception {
    	
        try {
      		if(result == null) {
        	    return -1;
      		}

      		ResultsGroup results = result.getResultsGroup();
      		if(results == null) {
        	    return -2;
      		}

            if(results.specialValues().containsKey(SpreadKey)) {
            return results.specialValues().getDouble(SpreadKey);
            }
            
            String lastSettingsXml = results.getLastSettings();
            if(lastSettingsXml == null) {
        	        return -3;
           	    }

            Element elSettings = XMLUtil.stringToElement(lastSettingsXml);
            Element elData = elSettings.getChild("Data");
            Element elSetups = elData.getChild("Setups");
            Element elSetup = elSetups.getChild("Setup");
            Element elChart = elSetup.getChild("Chart");
        
            double spread = Double.parseDouble(elChart.getAttributeValue("spread")); 
            results.specialValues().set(SpreadKey, spread);
            
            return spread;

        } catch(Exception e) {
        Log.error("Exc.", e);
        }
        
        return -4;
    }	
}
```
