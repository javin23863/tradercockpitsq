# Strategy style

- Source: <https://strategyquant.com/doc/strategyquant/strategy-style/>
- Section: StrategyQuant X › Quick start
- Fetched: 2026-09-04

---

StrategyQuant X allows you to choose from 3 different strategy “styles”. By style we mean how the strategy is constructed.

Every trading strategy consists of set of **IF** – **THEN** rules, managing **IF** something happens **THEN** do some action. There are however some differences in how exactly these rules are constructed.

## SQ3 (old) style

In previous version of SQ the generated strategies looked like this:

```
LONG ENTRY RULE:  IF Long Entry Conditions THEN Open Long order

SHORT ENTRY RULE: IF Short Entry Conditions THEN Open Short order

LONG EXIT RULE:   IF Long Exit Conditions THEN Close Long order

SHORT EXIT RULE:  IF Short Exit Conditions THEN Close Short order
```

It is a simple and logical format, but what if both Long and Short conditions are valid at the same time?  
Then you’d have to open both Long and Short order, or the Short order cancels the Long one.

Or what if both Long entry and Long exit conditions are valid at the same time? Then you wouldn’t know whether to entry of exit.

This leads to problems that are covered by the new SQ X architecture.

## SQ X new style

In SQ X new architecture the first rule is a special Signal rule that checks all the trading conditions. The rest of the rules then check the produced trading signals and open or close trades. The strategy looks like this:

```
SIGNAL RULE:                      
LongEntrySignal = Long Entry Conditions
ShortEntrySignal = Short Entry Conditions
LongExitSignal = Long Exit Conditions
ShortExitSignal = Short Exit Conditions

LONG ENTRY RULE:            
IF LongEntrySignal = true and ShortEntrySignal = false
   and LongExitSignal = false
THEN Open Long order

SHORT ENTRY RULE:
IF ShortEntrySignal = true and LongEntrySignal = false
   and ShortExitSignal = false
THEN Open Short order

LONG EXIT RULE: 
IF LongExitSignal = true and LongEntrySignal = false
THEN Close Long order

SHORT EXIT RULE: 
IF ShortExitSignal = true and ShortEntrySignal = false
THEN Close Short order
```

## SQ X new style with Fuzzy Logic

A modification of the new rule is to employ fuzzy logic, the only difference from previous type is in Signal rule. Normally the conditions for the signal are connected with AND and OR.

The typical signal in a standard trading strategy could be something like:

```
SIGNAL RULE:
LongEntrySignal = ((CCI(14) > 0) and (RSI(20) > 50))
                   or
                  ((MACD(10, 20, 30) > 0) and Hammer Candle Pattern))
```

With fuzzy logic we are adding possibility to evaluate all the conditions, and let some of them be wrong, while still having valid signal.

```
SIGNAL FUZZY RULE
LongEntrySignal (70% of the conditions below must be true):
  CCI(14) > 0
  RSI(20) > 50
  MACD(10, 20, 30) > 0
  Hammer Candle Pattern
```

Note that we don’t use any AND or ORs here, all conditions are evaluated.

With fuzzy rules you define one more thing – how big % of all the conditions has to be right to have the whole signal still valid.

In our case we have four conditions, and 70% of them (which means 3 conditions out of 4) have to be true. So if **any three of those four conditions are true**, LongEntrySignal will be true.

Using fuzzy rules makes sense only if you’ll let program generate more than just 2-3 sub-conditions in every signal, it doesn’t make much sense if you’ll have only 2 or 3 conditions in a signal.

But imagine if your LongEntrySignal consists of 10 conditions. Fuzzy logic can be seen as “voting”, and majority of the conditions in the right direction will determine the outcome of the signal.

This opens new possibilities for strategies creation.

## Custom strategy templates

Customization is a big power of StrategyQuant. You are not limited to these three predefined styles. Custom templates allow you to create a “template” of your  strategy architecture, and then generate strategies according to this template.

Please check an article [Developing strategies using custom strategy templates](../zz-unlisted/developing-strategies-using-custom-strategy-templates.md) for more information.
