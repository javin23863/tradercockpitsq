# Introduction to custom projects

- Source: <https://strategyquant.com/doc/strategyquant/introduction-to-custom-projects/>
- Section: StrategyQuant X › Custom projects and tasks
- Fetched: 2026-09-04

---

Custom projects enable you to **create custom workflows** – series of tasks going one after each other that follow the logic you specify.

For example, you can first build 1000 strategies, then retest them on one market, then retest the remaining ones on a second market, and then run WF Matrix test on the rest. The final few strategies that pass all the tasks can be stored into a final databank and project can end – or it can be restarted again until the final databank is full.

Custom workflows like this are a great way to automatize the whole strategy development and verification process – your workflow can run independently for hours or days and you can look at the final strategies once it finished.

Custom projects are available on the left menu. There you can create a new custom project.

A project consists of tasks – separate “actions” that are performed in the order they are defined. You can choose from these tasks:

[![Custom project tasks](https://strategyquant.com/wp-content/uploads/2020/05/cp_tasks.png)](https://strategyquant.com/wp-content/uploads/2020/05/cp_tasks.png)

We are adding new useful tasks with new builds, our aim is to make it possible to automatiza virtually every idea of workflow you might have with SQX.

# Custom project examples

StrategyQuant comes with two examples of custom project workflows – you can find them on the Getting started page:

[![Custom project examples](https://strategyquant.com/wp-content/uploads/2020/05/cp_examples.png)](https://strategyquant.com/wp-content/uploads/2020/05/cp_examples.png)

There is a custom project for forex and for futures strategies generation. You can use it as a starting point to create more advanced workflows.
