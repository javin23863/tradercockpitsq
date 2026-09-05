# Main concepts

- Source: <https://strategyquant.com/doc/strategyquant/custom-projects-main-concepts/>
- Section: StrategyQuant X › Custom projects and tasks
- Fetched: 2026-09-04

---

# Multiple databanks

You can create more than one databank in your custom project. Databank is a “database” where tasks can store / retrieve the strategies. You can create an unlimited number of databanks:

[![Custom projetc multiple databanks](https://strategyquant.com/wp-content/uploads/2020/05/cp_databanks.png)](https://strategyquant.com/wp-content/uploads/2020/05/cp_databanks.png)

And you can configure your tasks (for example Retester) to use one databank as a source (to read strategies from) and a target (to save retested strategies to).

[![Tasks databank source target](https://strategyquant.com/wp-content/uploads/2020/05/cp_databanks2.png)](https://strategyquant.com/wp-content/uploads/2020/05/cp_databanks2.png)

# Tasks flow

When you’ll open a custom project you can see the flow of tasks on the left:

[![Custom projects task flow](https://strategyquant.com/wp-content/uploads/2020/05/cp_taskflow.png)](https://strategyquant.com/wp-content/uploads/2020/05/cp_taskflow.png)

This defines the tasks that will be performed from start to finish. In this example we’ll start with clearing all the project databanks, then building a set of strategies, retest them a few times using a different settings. The strategies that pass all the retests will be placed to the Final databank.

# Conditional loops and filtering

The power of custom projects is in making possible loops. For example, you can add a **Go To Task** task to the very end of your custom project with a condition to skip back to **Build strategies** task if the number of strategies in the Final databank is lower than 100.

This way the custom project will repeat the generation and all filtering until there is at least 100 strategies that passed the whole custom project workflow.

[![Go to task](https://strategyquant.com/wp-content/uploads/2020/05/cp_gototask2.png)](https://strategyquant.com/wp-content/uploads/2020/05/cp_gototask2.png)
