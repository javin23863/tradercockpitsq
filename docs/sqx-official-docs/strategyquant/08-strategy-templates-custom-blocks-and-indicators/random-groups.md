# Random groups

- Source: <https://strategyquant.com/doc/strategyquant/random-groups/>
- Section: StrategyQuant X › Strategy templates, custom blocks and indicators
- Fetched: 2026-09-04

---

## What are random groups?

Random groups allow to specify the group of blocks that should be used when generating a strategy.

Understanding **Random groups** requires understanding **[Strategy templates](strategy-templates.md)** first. Please check this article first if you don’t know what strategy templates are.

Your choice of Building blocks in StrategyQuant is global – it means that it is used for all the entry & exit conditions.

What if you want to specify more exactly the selection of blocks to choose from for every **RandomCondition** placeholder?

This is where **Random groups** come to play. When you click on RandomCondition in the strategy template in AlgoWizard it will open its configuration:

[![Random group choice](https://strategyquant.com/wp-content/uploads/2020/04/rc_default-1024x662.png)](https://strategyquant.com/wp-content/uploads/2020/04/rc_default-1024x662.png)

You can see that by default no random group is chosen. This means that StrategyQuant should generate conditions from the global building blocks selection.

It is possible to customize it and you can tell SQ X to generate conditions only from your own pre-defined group. Before we do that we have to create some groups.

## How to create and manage Random  block groups?

You can do it in a special **Customize** screen in **AlgoWizard**. Click on **Customize** icon on the top toolbar:

[![](https://strategyquant.com/wp-content/uploads/2020/04/aw_toolbar.png)](https://strategyquant.com/wp-content/uploads/2020/04/aw_toolbar.png)

This will put you to a Random block groups editor where you can create and modify your own random groups:

[![](https://strategyquant.com/wp-content/uploads/2020/04/aw_rg-1024x363.png)](https://strategyquant.com/wp-content/uploads/2020/04/aw_rg-1024x363.png)

By default StrategyQuant doesn’t have any random groups defined, it is up to you to create some groups if you want to use them.

It is simple – click on **Add group** and fill group name and type.

[![](https://strategyquant.com/wp-content/uploads/2020/04/add_rg.png)](https://strategyquant.com/wp-content/uploads/2020/04/add_rg.png)

The type determines what kind of blocks the group can contain and where it can be used:

- **Conditions** – group can be used in RandomCondition placeholder, for creating conditions for trading signals
- **Values** – group can be used in RandomValue placeholder for creating price or numeric values – for example for Stop/Limit entry price
- **Actions** – group can be used in RandomAction placeholder for creating entry order and other actions.

We have created a new group **Group 1**, but it is currently empty:

[![](https://strategyquant.com/wp-content/uploads/2020/04/rg_empty-1024x257.png)](https://strategyquant.com/wp-content/uploads/2020/04/rg_empty-1024x257.png)

To be able to use it we must add some blocks to this group. You can use **Add block** button or **Copy & Paste** some existing blocks from AlgoWizard editor. Choosing blocks works in the same way as in editor:

[![](https://strategyquant.com/wp-content/uploads/2020/04/rg_addblock-1024x573.png)](https://strategyquant.com/wp-content/uploads/2020/04/rg_addblock-1024x573.png)

Just find and choose your signals or combine indicators, price values and comparisons to create your set of conditions.

We can create a group like this:

[![](https://strategyquant.com/wp-content/uploads/2020/04/rg1-1024x512.png)](https://strategyquant.com/wp-content/uploads/2020/04/rg1-1024x512.png)

Our **Group 1** contains only 4 possible conditions – two for Aroon and two for StdDev indicator.

Don’t forget to click on **Save** to save this group.

## Using Random groups in strategy template

Now we can go back to our strategy template. We’ll add one more Random condition that will use the newly created group. Click on Add another condition:

[![](https://strategyquant.com/wp-content/uploads/2020/04/tpl_add.png)](https://strategyquant.com/wp-content/uploads/2020/04/tpl_add.png)

And then find **Random condition** and configure it like this:

[![](https://strategyquant.com/wp-content/uploads/2020/04/tpl_add2.png)](https://strategyquant.com/wp-content/uploads/2020/04/tpl_add2.png)

We’ll name it ***RandomFromGroup1*** and we’ll choose ***Group 1*** for a Random group.

If you did it right it should look like in the image below:

[![](https://strategyquant.com/wp-content/uploads/2020/04/tpl_add3.png)](https://strategyquant.com/wp-content/uploads/2020/04/tpl_add3.png)

We didn’t need to create a new **RandomCondition** – we could have just edit the existing one, but this si only an example. With the current configuration we have two **RandomCondition** placeholders that will be generated randomly in StrategyQuant:

- **RandomCondition**(*RandomConditionLong*) – has no group specified, so it will be generated from blocks configured in Full settings -> Building blocks. Moreover, it could use and/or operators and can contain more than just one condition – depending on your setting in Full settings -> What to build -> # of Conditions, Periods
- **RandomCondition**(*RandomFromGroup1*) – uses Group 1, so it will be generated from our newly defined group. SQ will choose randomly only from these 4 blocks:
  - Aroon(14).Up crosses above Aroon Down
  - Aroon(14).Down crosses below Aroon Up
  - StdDev(14) changes direction downwards
  - StdDev(14) changes direction upwards

These blocks don’t need to be selected in Full settings -> Building blocks, and only one condition will be generated.

We want Short entry signal symmetrical to Long entry so we should add also one NegatedCondition there, so the resulting template will look like this:

[![](https://strategyquant.com/wp-content/uploads/2020/04/tpl_add4.png)](https://strategyquant.com/wp-content/uploads/2020/04/tpl_add4.png)

When we’ll use this template for strategy generation in SQ X it will generate the conditions for entry signals

## Parameters generation in random groups

If you use Random group in your strategy template, its contents takes precedence over your settings in Building blocks. If the given placeholder should be generated using random group then it will be generated using the blocks in that group – and these blocks DON’T NEED to be selected in **Builder settings -> Building blocks**.

Moreover – blocks will be generated in a way they are specified in random group. If you use fixed parameter values in your blocks, then only these fixed values will be used.

An example of random group **MyGroup**:

[![Random group example](https://strategyquant.com/wp-content/uploads/2020/04/rg_params.png)](https://strategyquant.com/wp-content/uploads/2020/04/rg_params.png)

This group contains two blocks:

- CCI > 0
- RSI is rising

You can see that CCI is defined with a fixed period 18, and RSI uses randomly defined period. This means that when you’ll generate strategies using this group:

- whenever SQ chooses to use **CCI > 0** condition, it will always use fixed period 18, so the condition will always be: *CCI(18) > 0*
- whenever SQ chooses to use **RSI is rising** condition, it will generate RSI period randomly using your configuration, so it can generate conditions like: *RSI(20) is rising*, *RSI(50) is rising*, etc.

Note – always set your parameters in blocks in in Random groups to random if you want them to be generated randomly.

## Limitation of Genetic Evolution (Block Control)

A key limitation of the genetic algorithm is that **strict control over block usage cannot be guaranteed beyond the initial population**.

While Random Groups and Custom Blocks define which components are allowed during **initial strategy generation**, this constraint is **not fully enforced in subsequent generations**. As evolution progresses:

- **Crossover** mixes parts of two parent strategies, potentially creating new structural combinations.
- **Mutation** can alter or replace parts of a strategy in a non-deterministic way.

As a result:

- Strategies in later generations may contain **combinations that were not explicitly defined in the original template structure**.
- The system may appear to introduce blocks or configurations that were **not intended from a strict template perspective**.
- This behavior is **inherent to evolutionary algorithms**, where exploration is prioritized over strict rule enforcement.

### Practical Impact

- Reduced determinism in strategy structure over time
- Potential deviation from the original design intent
- Harder reproducibility when strict constraints are required

### Recommendation

If strict control over allowed blocks and structure is required across all generated strategies, it is recommended to use **Random Generation**, which does not rely on crossover or mutation and adheres fully to the template definition.
