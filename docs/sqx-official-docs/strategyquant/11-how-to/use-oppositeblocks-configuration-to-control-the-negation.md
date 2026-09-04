# Use OppositeBlocks configuration to control the negation

- Source: <https://strategyquant.com/doc/strategyquant/use-oppositeblocks-configuration-to-control-the-negation/>
- Section: StrategyQuant X › How to...
- Fetched: 2026-09-04

---

When StrategyQuant generates strategies, its default configuration is to generate symmetrical Long and Short rule.

For example:

Long: CCI(14) > 0  
Short: CCI(14) < 0

Note that it uses opposite comparisons in each rule. This is what we call negation. StrategyQuant first creates **Long condition CCI(14) > 0** and it negates it to produce **Short condition**.

Every block in StrategyQuant has its corresponding opposite block.

For example:

```
> negates to <
< negates to >
= negates to <>
<> negates to =
```

These opposite blocks are selected right in the block definition (Java snippet code) and they are not configurable using UI.

## How to modify default negate behavior

You can override default negations by creating a file **\user\settings\OppositeBlocks.csv** in your StrategyQuant installation. This file doesn’t exist, there is a **OppositeBlocks\_example.csv** file that shows list of the default negations.

File format is simple, it should contain Block;OppositeBlock separated by lines. The block codes used there are names of block Java snippets – you can see them in the CodeEditor.

An example two lines from the file:

```
Equals;NotEquals
NotEquals;Equals
```

This means that Equals (=) comparison will be negated to NotEquals (<>) and vice versa.

So the conditions generated could look like:

Long: CCI(14) = 0  
Short: CCI(14) <> 0

You can rewrite the file like this:

```
Equals;Equals
NotEquals;NotEquals
```

This will tell SQ to negate Equals (=) comparison to Equals, and NotEquals to NotEquals and it will produce conditions like:

Long: CCI(14) = 0  
Short: CCI(14) = 0

or

Long: CCI(14) <> 0  
Short: CCI(14) <> 0

Note that you have to restart SQ when you change the file for changes to take effect.
