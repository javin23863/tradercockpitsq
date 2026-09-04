# Settings – Building blocks

- Source: <https://strategyquant.com/doc/strategyquant/building-blocks/>
- Section: StrategyQuant X › Program screens
- Fetched: 2026-09-04

---

Building blocks are the core components that are combined to create rules and actions for every trading strategy.

You can check each block that you want to use. You can also optionally increase its weight or %:  
Weight is used for blocks that compete with each other – block with bigger weight has bigger chance to be chosen.  
Percentage is used for exits – how big probability is to use given exit method.

This way you can select your favorite indicators, or choose (for example) only price data + operators if you want to generate strategies based only on price.

**Good practice***According to our experience, you can sometimes get better results if you don’t check all the available components, but narrow your choice to a smaller group of indicators or price values.*

[![StrategyQuant Building blocks config](https://strategyquant.com/wp-content/uploads/2019/02/building_blocks.png)](https://strategyquant.com/wp-content/uploads/2019/02/building_blocks.png)

Building blocks are divided into four separate panels:

## Signals, Indicators, Stop/Limit entry blocks

The biggest panel occupying whole left side, subdivided into three sub-sections:

**Signals**sections for signal blocks – they are predefined complete conditions that combine indicators with comparisons or some propeties, for example “**ADX is rising**” or ” **CCI crossed above 0**”

**Indicators**  
section for indicators, price ranges etc. By choosing indicators and operators these selected blocks will be randomly combined to create random conditions.

**Stop / Limit entry blocks**  
These blocks are used in Stop/Limit prices when opening Stop or Limit orders.

## Order types

These are blocks specify which order types can be used – Market, Stop, Limit, Enter/Reverse

[![StrategyQuant order types](https://strategyquant.com/wp-content/uploads/2019/02/order_types.png)](https://strategyquant.com/wp-content/uploads/2019/02/order_types.png)

## Exit types

here you can specify various exit types the strategy can have

[![StrategyQuant exit types](https://strategyquant.com/wp-content/uploads/2019/02/exit_types.png)](https://strategyquant.com/wp-content/uploads/2019/02/exit_types.png)

## Custom data indicators

External indicators imported to StrategyQuant as data. Note that they are valid only for the selected timeframe. Find out more about [Custom data indicators here](../zz-unlisted/custom-data-indicators.md).

[![StrategyQuant custom data indicators](https://strategyquant.com/wp-content/uploads/2019/02/custom_data_indicators.png)](https://strategyquant.com/wp-content/uploads/2019/02/custom_data_indicators.png)

## Advanced – editation of block parameters

There is a possibility to edit parameters for every generated block. By default, it uses Default parameters, you can click on the Default link next to the block to modify its config.

[![SQ Building blocks parameters customization](https://strategyquant.com/wp-content/uploads/2019/02/building_blocks_edit.png)](https://strategyquant.com/wp-content/uploads/2019/02/building_blocks_edit.png)

It opens a dialog where you can review and modify type of generation for every block/indicator parameter and its range.

Let’s open configuration on Parameters tab for CCI as an example.

### Parameter values

In this part you see list of al parameters of given block, and you can modify how they are generated. You can choose the exact range for random generated value, or set some of the parameters fixed.

[![Block parameter values](https://strategyquant.com/wp-content/uploads/2019/02/bb3.png)](https://strategyquant.com/wp-content/uploads/2019/02/bb3.png)

Here you can see list of all parameters used in CCI building block:

**Chart** – every indicator is computed on a given chart. If you use only one chart (simple strategy) it will be computed from data on main chart  
**Computed From** – price data from which CCI is computed  
**Period** – CCI period  
**Shift** – CCI shift value

Every indicator or other building block has its own set of parameters, most of them contain Chart and Shift, plus some number of indicator parameters like periods.

To edit a parameter, double click on it in the grid. It will open parameter editation dialo like this:

[![](https://strategyquant.com/wp-content/uploads/2019/02/bb4.png)](https://strategyquant.com/wp-content/uploads/2019/02/bb4.png)

Here you can choose if the parameter will have fixed value or if it will be randomly generated. For random parameters you can also choose list of values to choose from, or range from-to for numeric parameters.

### Parameter Sets

There is one more possibility to define block parameter ranges. You can create multiple sets of parameters, where in each set some of the parameters could be generated randomly, and other have fixed values.

[![](https://strategyquant.com/wp-content/uploads/2019/02/bb5.png)](https://strategyquant.com/wp-content/uploads/2019/02/bb5.png)

So instead of generating (for example) CCI Period randomly from range between 10 and 50, you can create a few parameter sets, and let SQ X randomly chose CCI period only from values 14 and 30.

Parameter sets compete with normal parameter definitions for which of them will be actually used in the generated block. You can use Weight to increase probability that a particular set is used.

For example, by setting Weight of Parameters to 0, while keeping Weights of parameter sets to 1 you tell SQ to generate parameters only using parameter sets.

### Indicator values

third tab that was added in the most recent SQ builds:

[![SQ block indicator values](https://strategyquant.com/wp-content/uploads/2019/02/block_indicator_values.png)](https://strategyquant.com/wp-content/uploads/2019/02/block_indicator_values.png)

It allows you to specify the expected range of indicator values. This is then used when this indicator is used in random comparison with numbers – so that SQ “knows” what range the indicator normally has.

## Advanced – Indicators calibration

some indicator values are dependent on the market on which they are computed. The value ranges can be very different if computed for EURUSD with value around 1, or for CFD with a value in 10000.

[Please note that we are introducing more advanced and configurable calibration in the build 131.](indicators-calibration.md)

Calibration allows to configure the min, max and step values of all indicators in one place.

[![StrategyQuant indicators auto calibration](https://strategyquant.com/wp-content/uploads/2019/02/indy_calib.png)](https://strategyquant.com/wp-content/uploads/2019/02/indy_calib.png)

**Autocalibration**does this automatically, by computing the indicators on the current market (set in Data) and anaysing the min and max values.
