# Calling another indicator from indicator snippet

- Source: <https://strategyquant.com/doc/programming-for-sq/introduction-to-scripter/calling-another-indicator-from-indicator-snippet/>
- Section: Programming for StrategyQuant X › Indicators / Signals step-by-step
- Fetched: 2026-09-04

---

When programming indicators in Strategy Quant X, there may be a situation when you want to call another indicator and get to its values. In the next article we will show how to import and call indicators correctly.

## Step 1 – Create new custom indicator in Code Editor

Open the code editor, create a new snippet and name it IndicatorCalling. This article assumes knowledge of how to create indicators. In the documentation you will find two examples

- [Forceindex indicator](adding-new-indicator-snippet-forceindex.md)
- [Envelopes indicator](adding-envelopes-indicator-step-by-step.md)

## Step 2 – Importing indicators and setting the indicator type

In this step we import 3 indicators. ATR, RSI and DPO. The Indicators class is a cache class that caches all indicators used in a trading setup.  
ATR and RSI are indicators created directly in StrategyQuantX, and DPO is an indicator you can download from the Sharing Server  [here](https://strategyquant.com/codebase/detrended-price-oscillator-dpo/).

```
package SQ.Blocks.Indicators.IndicatorCalling;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;
import SQ.Internal.IndicatorBlock;
import SQ.Internal.IndicatorBlock;
// importing indicators
import SQ.Blocks.Indicators.ATR.ATR;
import SQ.Blocks.Indicators.RSI.RSI;
import SQ.Blocks.Indicators.DPO.DPO;
```

## Step 3 – Parameter Settings

First parameter is important. There are two options:

- Parameter with type **DataSeries .**This is valid for a big portion of indicators that are computed from only one price. For example CCI, RSI, etc. indicators are usually computed from Close price. You can configure them to be computed from different price, for example from Open price, but still it is only one price array.
- Parameter with type **ChartData** type is an object that represents the whole chart – you’ll have access to Open, High, Low, Close, Volume prices in the given chart.

In this case, whenever we want to call another indicator from an indicator, we must choose the ChartData Input parameter type.

An indicator can have several parameters. In our case, we will define the parameters of the indicators we will call. The parameters **DPOPeriod**, **ATRPeriod**, **RSIPeriod** are used to define the periods of the indicators. Also the genetic engine or the optimizer will work with them.

Third variable is **Value**, note that it has different annotation ***@Output***. This means that this variable is not an indicator parameter but its output buffer. Indicators usually have just one output buffer, but they can have more – for example Bollinger band has Upper and Lower buffer.

```
Parameter
public ChartData Input;

@Parameter(defaultValue="10", isPeriod=true, minValue=2, maxValue=1000, step=1)
public int DPOPeriod;

@Parameter(defaultValue="10", isPeriod=true, minValue=2, maxValue=1000, step=1)
public int ATRPeriod;

@Parameter(defaultValue="10", isPeriod=true, minValue=2, maxValue=1000, step=1)
public int RSIPeriod;

@Output
public DataSeries Value;
```

## Step 4 – Implementation OnBarUpdate()method

In this method we call instances of indicators and calculate the resulting value which we store in the object [Dataseries](https://strategyquant.com/sqxapi/com/strategyquant/datalib/DataSeries.html) Value.

```
protected void OnBarUpdate() throws TradingException {
      
      	ATR aTRIndicator = Indicators.ATR(Input, ATRPeriod);
  	  	double atrValue = aTRIndicator.Value.getRounded(0);

      	RSI rsiIndicator = Indicators.RSI(Input.Close, RSIPeriod);
  	  	double rsiValue = rsiIndicator.Value.getRounded(0);

        DPO dpoIndicator = Indicators.DPO(Input, DPOPeriod);
  	  	double dpoValue = dpoIndicator.Value.getRounded(0);
        
        if (CurrentBar < Math.max(ATRPeriod,RSIPeriod)) {
            // what to set when there are less bars than indicator period parameter
            Value.set(0, 0);

        } else {
            double value = rsiValue/atrValue*dpoValue/atrValue;
            Value.set(0, value);
        }
```

double value = rsiValue/atrValue\*dpoValue/atrValue to get the final value of the indicator. It is an example, therefore the calculation has no deeper meaning.

## Full Code

```
package SQ.Blocks.Indicators.IndicatorCalling;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;
import SQ.Internal.IndicatorBlock;
import SQ.Blocks.Indicators.ATR.ATR;
import SQ.Blocks.Indicators.RSI.RSI;
import SQ.Blocks.Indicators.DPO.DPO;
import SQ.Internal.IndicatorBlock;

@BuildingBlock(name="(IC) IndicatorCalling", display="IndicatorCalling(#ATRPeriod#,#RSIPeriod#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("IndicatorCalling help text")
public class IndicatorCalling extends IndicatorBlock {

    @Parameter
    public ChartData Input;

    @Parameter(defaultValue="10", isPeriod=true, minValue=2, maxValue=1000, step=1)
    public int DPOPeriod;

    @Parameter(defaultValue="10", isPeriod=true, minValue=2, maxValue=1000, step=1)
    public int ATRPeriod;

    @Parameter(defaultValue="10", isPeriod=true, minValue=2, maxValue=1000, step=1)
    public int RSIPeriod;

    @Output
    public DataSeries Value;

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------

    @Override
    protected void OnBarUpdate() throws TradingException {
        
      	ATR aTRIndicator = Indicators.ATR(Input, ATRPeriod);
  	  	double atrValue = aTRIndicator.Value.getRounded(0);

      	RSI rsiIndicator = Indicators.RSI(Input.Close, RSIPeriod);
  	  	double rsiValue = rsiIndicator.Value.getRounded(0);

        DPO dpoIndicator = Indicators.DPO(Input, DPOPeriod);
  	  	double dpoValue = dpoIndicator.Value.getRounded(0);
        
        if (CurrentBar < Math.max(ATRPeriod,RSIPeriod)) {
            // what to set when there are less bars than indicator period parameter
            Value.set(0, 0);

        } else {
            double value = rsiValue/atrValue+dpoValue/atrValue;
            Value.set(0, value);
        }

    }
}
```
