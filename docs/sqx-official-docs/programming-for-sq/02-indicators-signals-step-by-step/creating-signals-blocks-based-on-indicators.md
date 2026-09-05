# ForceIndex Signal blocks

- Source: <https://strategyquant.com/doc/programming-for-sq/creating-signals-blocks-based-on-indicators/>
- Section: Programming for StrategyQuant X › Indicators / Signals step-by-step
- Fetched: 2026-09-04

---

Our new **ForceIndex** indicator is implemented, but as mentioned earlier, it is available only Random Indicators Signals section. This means that it will be randomly combined with all the comparisons and random numerical values.

In StrategyQuant X you should prefer using signals – those are predefined conditions using an indicator, for example:

*Indicator is rising  
Indicator is falling*

*Indicator crosses above level  
Indicator crosses below level*

Using predefined signals like this has many advantages, one of them is that it decreases levels of freedom and allows you to specify conditions that make some real sense in trading. SQ will then use these predefined conditions instead of trying to generate the whole condition randomly.

Below we will show how you can create your own signals that use some already implemented indicator – in our case ForceIndex. For this example we’ll implement two simple signals:

*ForceIndex is higher than zero* – should suggest uptrend  
*ForceIndex is lower than zero* – should suggest downtrend

## Step 1 – Creating new signal in Code Editor

Like indicator, signal is a snippet that must be programmed in SQ. Open CodeEditor, click on Create new and create a new snippet with name **ForceIndexAboveZero**. Choose **Signal** as snippet type, and **ForceIndex** as indicator the signal is based on.

[![](https://strategyquant.com/wp-content/uploads/2019/02/force_signal.jpg)](https://strategyquant.com/wp-content/uploads/2019/02/force_signal.jpg)

This will create a new snippet **ForceIndexAboveZero.java** and places it into SQ -> Blocks -> Indicators -> FoceIndex folder.

When we’ll review the code you can notice the code is much simpler than the code for the indicator itself. It also has parameters, which should match parameters of indicator, and then only one method OnBlockEvaluate() which should return true or false depending on if the signal is valid or false.

Please note that the standard signals template might not match your indicator, as it is in this case. If you’d try to compile it you’ll receive compilation errors, because the call of the indicator Strategy.Indicators.ForceIndex(Input, Period) in OnBlockEvaluate() method doesn’t use all the required parameters.

In the next steps we’ll modify the defaukt template so that it works with ForceIndex

### Implement correct ForceIndex parameters

If you’ll look at the ForceIndex indicator you’ll notice it has parameters: Chart, Period, MAMethod, AppliedPrice.

We must copy the same parameters also to our new signal:

```
public class ForceIndexAboveZero extends ConditionBlock {

    @Parameter
    public ChartData Chart;

    @Parameter(defaultValue="10", isPeriod = true, minValue=2, maxValue=1000, step=1)
    public int Period;

    @Parameter(name="Method", defaultValue="0")
    @Editor(type=Editors.Selection, values="Simple=0,Exponential=1,Smoothed=2,Linear weighted=3")
    public int MAMethod;

    @Parameter(defaultValue="0")
    @Editor(type=Editors.Selection, values="Close=0,Open=1,High=2,Low=3,Median=4,Typical=5,Weighted=6")
    public int AppliedPrice;
```

or alternatively you might want to omit some of the parameters, and use its fixed value instead. Creating a parameter means that it will be editable in Wizard and SQ will be able to generate random values for this parameter.

There is one more parameter that we should add:

```
    @Parameter
    public int Shift;
```

In contrast with indicator snippet, Shift parameter in signals is not used by default and has to be explicitly added.

### Implement OnBlockEvaluate method

Below is an implementation of OnBlockEvaluate() method for our signal. As you can see, it is quite simple – you only have to get the indicator by calling Strategy.Indicator.ForceIndex(…) with proper parameters, and then just get its value at specified Shift and compare it with zero.

```
    @Override
    public boolean OnBlockEvaluate() throws TradingException {

        ForceIndex indicator = Strategy.Indicators.ForceIndex(Chart, Period, MAMethod, AppliedPrice);
        double value = indicator.Value.getRounded(Shift);

        return (value > 0);
    }
```

This is all, our new signal is now finished, we can click on Compile and it should be successfully compiled.

### Implement ForceIndexBelowZero

In the same way we should implement also **ForceIndexBelowZero. The only difference from previous signal is that the comparison in** OnBlockEvaluate() **method will be opposite:**

```
return (value < 0);
```

Now compile also the second signal and then restart SQ. When you’ll look at building blocks you should see a new section for ForceIndex in signals, with our two new signals:

[![](https://strategyquant.com/wp-content/uploads/2019/02/fi_signal_blocks.png)](https://strategyquant.com/wp-content/uploads/2019/02/fi_signal_blocks.png)
