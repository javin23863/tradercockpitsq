# Strategy templates

- Source: <https://strategyquant.com/doc/strategyquant/strategy-templates/>
- Section: StrategyQuant X › Strategy templates, custom blocks and indicators
- Fetched: 2026-09-04

---

One of the main advantages of StrategQuant X is ability to generate strategies with your own custom “format”.

StrategyQuant generates strategies using **strategy templates** – they are strategies that use special placeholder blocks in certain parts (we call these Random placeholders) and StrategyQuant then randomly generates blocks that fill these placeholders.

## Strategy templates and placeholders

Look at the screenshot below, it shows a Signal rule of a standard SQ X strategy template:

[![SQ Strategy Template example](https://strategyquant.com/wp-content/uploads/2020/04/tpl_signal-1024x581.png)](https://strategyquant.com/wp-content/uploads/2020/04/tpl_signal-1024x581.png)

You can see that signals for log & short entry & exit are not defined in the strategy yet – this is because they will be generated randomly by StrategyQuant.

Instead, there are random placeholders:

- **RandomCondition**(*RandomConditionLong*) – this means that SQ will generate random condition(s) in this place. Each random condition has a unique identification, for example *RandomConditionLong*.
- **NegatedCondition**(*RandomConditionLong*) – is a special placeholder that tells SQ X to negate whatever condition it generated for the random condition named *RandomConditionLong* and put it here.  
  So as a result there will be a randomly generated condition for the Long entry signal, and matching negated condition for Short entry signal – and the same for exit signals.

For example, after the generation it will look like this:

SIGNAL – LongEntrySignal:

```
CCI(14)[1] > 0 and RSI(20)[1] > 50
```

SIGNAL – ShortEntrySignal:

```
CCI(14)[1] < 0 and RSI(20)[1] < 50
```

Note that conditions for Short entry have opposite comparison operators – **< (Is Lower)** instead of **> (Is Greater)**, which means they are negations of the Long conditions.

## How exactly are the conditions for RandomCondition placeholder generated?

By default they are generated from the selection of building blocks you have in your Full settings -> Building blocks:

[![SQ Building blocks settings](https://strategyquant.com/wp-content/uploads/2020/04/sq_bb-1024x751.png)](https://strategyquant.com/wp-content/uploads/2020/04/sq_bb-1024x751.png)

StrategyQuant X will use your selection of building blocks and other configuration settings (how many conditions it should generate, the Period and Shift ranges, etc.) to generate the conditions it will put in place of RandomCondition.

**To learn more about strategy template check the following articles on our blog:**

- **[Introduction to the StrategyQuant templating system – Part I](https://strategyquant.com/blog/introduction-strategyquant-templating-system-part/)**
- **[Introduction to the StrategyQuant templating system – Part II](https://strategyquant.com/blog/introduction-strategyquant-templating-system-part-ii/)**
