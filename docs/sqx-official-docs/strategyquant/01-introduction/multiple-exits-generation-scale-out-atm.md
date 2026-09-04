# Multiple exits generation (scale out, ATM)

- Source: <https://strategyquant.com/doc/strategyquant/multiple-exits-generation-scale-out-atm/>
- Section: StrategyQuant X › Introduction
- Fetched: 2026-09-04

---

This feature is added in Build 131, and it is available only in Ultimate edition.

Until now, multiple exits for scaling out a trade (also called ATM, Advanced Trade Management) were available only as defined settings.

You can specify the exact configuration of multiple exits and they will be applied to strategies as they are generated, you can also apply them to existing strategies in **Retester**.

In Build 131 Ultimate edition there is a possibility to generate multiple exits randomly.

Configuration:

[![multiple exits atm settings](https://strategyquant.com/wp-content/uploads/2021/03/atm_gen_config-1024x817.png)](https://strategyquant.com/wp-content/uploads/2021/03/atm_gen_config-1024x817.png)

You must switch config source to **Generate ATM** to see this configuration.

## Exit types to use

You simply choose which types of exits you want to use and their allowed ranges.

## Exit scenarios

Exits are not generated totally randomly, there are 7 most used scenarios to choose from.

## How it works

It is simple, when generating or improving a strategy first a scenario is chosen randomly from allowed scenarios.

Depending on the scenario 2 or 3 exits are then generated randomly from the allowed types.

## Using in Improver

You can use ATM generation also when improving a strategy – there is a new setting ATM in Parts to improve, and it is possible to select only this part.

[![Improver - multiple exits generation](https://strategyquant.com/wp-content/uploads/2021/03/atm_generation_improver.png)](https://strategyquant.com/wp-content/uploads/2021/03/atm_generation_improver.png)

It means that the rest of the strategy will remain intact, and only different ATM exits are generated and tried.

Of course, you can combine improving ATM with improving entry or exit rules.
