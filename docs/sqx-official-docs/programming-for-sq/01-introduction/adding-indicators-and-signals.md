# Adding indicators and signals

- Source: <https://strategyquant.com/doc/programming-for-sq/adding-indicators-and-signals/>
- Section: Programming for StrategyQuant X › Introduction
- Fetched: 2026-09-04

---

New indicator and signal blocks can be added to StrategyQuant using a build in **Code Editor**.

[![SQ Code Editor](https://strategyquant.com/wp-content/uploads/2019/01/link_codeeditor.png)](https://strategyquant.com/wp-content/uploads/2019/01/link_codeeditor.png)

Every indicator and signal in StrategyQuant is implemented as a Java snippet, with full source code visible.

Generally speaking:

- every indicator has to implement an ***OnBarUpdate()*** method – this is where the actual indicator algorithm is.
- every signal has to implement an OnBlockEvaluate() method that returns true or false

These methods are called on every bar, and indicator or signal need to compute only its latest latest value(s) and store it to its output buffers.

Indicator and signal blocks have parameters – such as period, or chart. SQ Snippets use @ annotation for them, these parameters are defined as standard Java class variables.

## Block annotation examples

The annotations define block name and type, and which of the variables are indicator parameters and which are buffers for computed values.

### Momentum

```
@BuildingBlock(name="(MO) Momentum", display="Momentum(#Period#)[#Shift#]", returnType = ReturnTypes.Number)
@Indicator(oscillator=true, middleValue=100, min=96, max=104, step=0.1)
public class Momentum extends IndicatorBlock {

    @Parameter(defaultChartIndex=0)
    public DataSeries Input;
    
    @Parameter(defaultValue="14")
    public int Period;

    @Output(name = "Momentum", color = Colors.Red)
    public DataSeries Value;
```

### Momentum is lower than Level (signal block)

```
@BuildingBlock(name="Momentum is lower than Level", display="Momentum(#Period#) < #Level#", returnType = ReturnTypes.Boolean)
public class MomLower extends ConditionBlock {
    
    @Parameter
    public DataSeries Input;
    
    @Parameter(defaultValue="14", minValue=2, maxValue=10000, step=1)
    public int Period;

    @Parameter(defaultValue="0", minValue=0, maxValue=200, step=0.01, builderMinValue=96, builderMaxValue=104, builderStep=0.1)
    public double Level;

    @Parameter
    public int Shift;
```

## Indicators OnBarUpdate() method examples

You can see an example of a few indicators below, in most cases the codes are very simple thanks to the SQ architecture – you don’t need to compute all the values on all bars, but only the value on very last bar.

### Momentum

```
protected void OnBarUpdate() throws TradingException {
    if(CurrentBar == 0) {
        Value.set(0, 0);
    } else {		
        Value.set(0, Input.get(0) * 100.0 / Input.get(Math.min(Period, CurrentBar)));
    }
}
```

### ATR

```
protected void OnBarUpdate() throws TradingException {
    double curHigh = Chart.High.get(0);
    double curLow = Chart.Low.get(0);
    double trueRange = curHigh - curLow;
    
    if (CurrentBar == 0){
        Value.set(0, trueRange);
    }
    else {
        double prevClose = Chart.Close.get(1);
        trueRange = Math.max(Math.abs(curLow - prevClose), Math.max(trueRange, Math.abs(curHigh - prevClose)));
        
        Value.set(0, ((Math.min(CurrentBar + 1, Period) - 1 ) * Value.get(1) + trueRange) / Math.min(CurrentBar + 1, Period));
    }
}
```

### CCI

```
protected void OnBarUpdate() throws TradingException {
    averageCalculator.onBarUpdate(Input.get(0), CurrentBar);
    
    if (CurrentBar == 0) {
        Value.set(0, 0);
    } 
    else {
        double mean = 0;
        double sma = averageCalculator.getValue();
        
        for (int idx = Math.min(CurrentBar, Period - 1); idx >= 0; idx--) {
            mean += Math.abs(Input.get(idx) - sma);
        }
        
        if(mean < 0.0000000001) {
            Value.set(0, 0);
        } else {
            double cci = (Input.get(0) - sma) / (mean == 0 ? 1 : (0.015 * (mean / Math.min(Period, CurrentBar + 1))));
            Value.set(0, cci);
        }
    }
}
```

You can see that CCI uses additional helper class AverageCalculator, which is also available with full source code in snippet SQ/Calculators/AverageCalculator.java

## Signals OnBlockEvaluate() method example

### CCI is higher than level

Code for signal is much simpler than for indicator. It usually consists of getting a value(s) of an indicator and comparing it with somehting else.

```
public boolean OnBlockEvaluate() throws TradingException {
    CCI indicator = Strategy.Indicators.CCI(Input, Period);
    double value1 = indicator.Value.getRounded(Shift, 4);
    
    return (value1 > Level);
}
```

## Step-by-step-examples

Please check the following step-by-step examples to learn more:

- [ForceIndex indicator](https://strategyquant.com/doc/programming-sq/adding-new-indicator-snippet-forceindex/)
- [Signals based on ForceIndex indicator](https://strategyquant.com/doc/programming-sq/creating-signals-blocks-based-on-indicators/)
- [Envelopes indicator](https://strategyquant.com/doc/programming-sq/adding-envelopes-indicator-step-by-step/)
