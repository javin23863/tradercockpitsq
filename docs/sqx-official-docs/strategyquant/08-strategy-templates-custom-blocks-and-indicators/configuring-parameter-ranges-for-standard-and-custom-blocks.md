# Configuring parameter ranges for standard and custom blocks

- Source: <https://strategyquant.com/doc/strategyquant/configuring-parameter-ranges-for-standard-and-custom-blocks/>
- Section: StrategyQuant X › Strategy templates, custom blocks and indicators
- Fetched: 2026-09-04

---

When generating a new block, for example CCI indicator, StrategyQuant also generates the values of its parameters.

## Configuring parameter ranges for standard (build-in) blocks

In case of CCI the most important parameters are Period and Shift.

By default parameter ranges are configured in **Builder** -> **Building block** settings. There you can customize parameter ranges for every block that you want to use:

[![Building blocks parameter ranges configuration](https://strategyquant.com/wp-content/uploads/2020/07/params_config-918x1024.png)](https://strategyquant.com/wp-content/uploads/2020/07/params_config-918x1024.png)

As you can see on the screenshot above, they are configured to use globally set ranges – these are the period and shift ranges that are configured in **Builder** -> **What to build** tab:

[![Period and Shift ranges in What to build tab](https://strategyquant.com/wp-content/uploads/2020/07/wtb_config-1024x654.png)](https://strategyquant.com/wp-content/uploads/2020/07/wtb_config-1024x654.png)

Periods and Shifts are special type of parameters and they are configured globally here by default. You can alternatively switch them to a custom range where you define the range for the given block:

[![Custom parameter ranges for blocks](https://strategyquant.com/wp-content/uploads/2020/07/edit_params.png)](https://strategyquant.com/wp-content/uploads/2020/07/edit_params.png)

## Configuring parameter ranges for custom blocks

[Custom blocks](custom-blocks.md) are special blocks that you can create by yourself using AlgoWizard editor. You can combine some existing blocks to create a new condition.

Every custom block has its own parameters that are externally visible. See below a custom block AATestBlock as an example of the functionality:

[![Custom block example](https://strategyquant.com/wp-content/uploads/2020/07/cust_block_params-1024x539.png)](https://strategyquant.com/wp-content/uploads/2020/07/cust_block_params-1024x539.png)

Note that custom block has parameters Chart, Period, Shift and you are able to define range (set Min, Max and Step) for Period and Shift parameters.

**These values take precedence over parameter settings configured in Building blocks** visible in previous section. So when SQ generates Period parameter for AATestBlock, it will use period in range from 33 to 66 with step 3.

There is a way if you want to change this behavior and use global settings for periods and Shift parameters (the ones from **Builder** -> **What to build** config) – just set all three parameters Min, Max, Step to zero in the custom block configuration:

[![Min, max, step zero for custom block parameters](https://strategyquant.com/wp-content/uploads/2020/07/period_zero.png)](https://strategyquant.com/wp-content/uploads/2020/07/period_zero.png)

This tells StrategyQuant that it should generate these values from configuration in [**What to build**](../05-program-screens/what-to-build.md) tab:

[![Period range What to build](https://strategyquant.com/wp-content/uploads/2020/07/wtb_config2.png)](https://strategyquant.com/wp-content/uploads/2020/07/wtb_config2.png)
