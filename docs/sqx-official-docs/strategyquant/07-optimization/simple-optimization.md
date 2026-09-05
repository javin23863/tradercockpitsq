# Simple Optimization

- Source: <https://strategyquant.com/doc/strategyquant/simple-optimization/>
- Section: StrategyQuant X › Optimization
- Fetched: 2026-09-04

---

The idea behind an optimization is simple. First you must have a trading system, this may be a simple moving average crossover for example.

In almost every system there are some parameters (indicator periods, comparative constants, etc.) that decide how given system behave. The optimization means to test the system with different parameter values

to find the optimal values of these parameters (giving highest profit or best Return/DD ratio).

**Optimization example**

**Step 1:  Loading a strategy for optimization**

First, you must switch to Optimizer window and load the strategy you want to optimize.

[![](https://strategyquant.com/wp-content/uploads/2019/03/optimization1.png)](https://strategyquant.com/wp-content/uploads/2019/03/optimization1.png)

For this example we’ll use simple EMA Cross strategy that goes long when faster EMA crosses above slower EMA, and go short when faster EMA crosses below slower EMA.

After you loaded the strategy, it is added also as Original strategy to the Optimization results databank.

You can double click on the Original strategy and then go to Results -> Source code to see its rules.

[![](https://strategyquant.com/wp-content/uploads/2019/03/optimization2.png)](https://strategyquant.com/wp-content/uploads/2019/03/optimization2.png)

Make sure you check the **Put values to variables** checkbox so that you see that variables LongEMAPeriod, LongEMAPeriod2, ShortEMAPeriod, ShortEMAPeriod2 are used to store indicator parameters.

In our optimization we’ll try to find optimal values of these parameters.

There’s still one small problem. We can see that the strategy uses different parameters for long and for short direction.

We can use it like this if we want to find optimal values independently for long and short side, but for our example we’d like to use the same parameter for long and short side.

We can do it using the other checkbox **Generate symmetric variables**

[![](https://strategyquant.com/wp-content/uploads/2019/03/optimization3.png)](https://strategyquant.com/wp-content/uploads/2019/03/optimization3.png)

If you check the checkbox, it will use the same parameters for long and short direction (providing the rules are the same).

[![](https://strategyquant.com/wp-content/uploads/2019/03/optimization4.png)](https://strategyquant.com/wp-content/uploads/2019/03/optimization4.png)

Now you can see that the strategy contains only parameters EMAPeriod, EMAPeriod2 that are used for both directions.

Step 2: Setting optimization values

To set up values that will be optimized we have to go to Settings -> Parameters

**Parameters – automatic**

Once this option is selected all parameters will have the value range generated automatically using a distribution process defined by **Value Distribution (%)**. This value defines how much the parameter value will fluctuate by % increments. **Maximum Steps** option defines how many % increments for the parameter value will be generated.

[![](https://strategyquant.com/wp-content/uploads/2019/03/optimization5.png)](https://strategyquant.com/wp-content/uploads/2019/03/optimization5.png)

**Parameters – manual**

Here you can see the list of all strategy parameters that are available for optimization and manual modification.  
Optimization simply means trying different values of input parameters.

[![](https://strategyquant.com/wp-content/uploads/2019/03/optimization6.png)](https://strategyquant.com/wp-content/uploads/2019/03/optimization6.png)

For every parameter you want to optimize you have to **check** the line of the parameter and choose **Start**, **Stop** and **Step** values. The optimizer will iterate the value from Start to Stop, taking Steps.

Original value is also configurable, it will be used to retest the original strategy. You can use this value to compare performance of new results with the “original” settings.

The **Total Combinations** value shows us how many tests have to be performed to test all the combinations of the values. This can be limited using **Maximum Optimizations.**

***Note!***

*It is possible that your parameters table will contain much more parameters, it could look like this:*

[![](https://strategyquant.com/wp-content/uploads/2019/03/optimization7.png)](https://strategyquant.com/wp-content/uploads/2019/03/optimization7.png)

This is another powerful feature of StrategyQuant. It allows you to optimize not only strategy parameters, but also other trading options, such as how many trades to take per day, or what should be the time range for trading or even indicator shifts.

These settings are normally a part of Strategy Options, but you can also optimize their values.

[![](https://strategyquant.com/wp-content/uploads/2019/03/optimization8.png)](https://strategyquant.com/wp-content/uploads/2019/03/optimization8.png)

The last thing we have to configure is the data that will be used for testing. We can choose EURUSD on H1 timeframe for this example in the **Data** tab.

[![](https://strategyquant.com/wp-content/uploads/2019/03/optimization9.png)](https://strategyquant.com/wp-content/uploads/2019/03/optimization9.png)

**Step 3: Running the optimization**

Before we start the optimization process we need to decide how StrategyQuant will store the results. There are two options for this. StrategyQuant can either **Store all optimizations into databank** or **Store only best** **optimization** which keeps only the best performing modification of the strategy in the databank.

[![](https://strategyquant.com/wp-content/uploads/2019/03/optimization10.png)](https://strategyquant.com/wp-content/uploads/2019/03/optimization10.png)

Now we are ready to run the optimization. We’ll click on **Start** button.

The optimization engine will test all the possible combinations of selected input parameters and store the results for each combination to the databank on the bottom.

[![](https://strategyquant.com/wp-content/uploads/2019/03/optimization11.png)](https://strategyquant.com/wp-content/uploads/2019/03/optimization11.png)

[![](https://strategyquant.com/wp-content/uploads/2019/03/optimization12.png)](https://strategyquant.com/wp-content/uploads/2019/03/optimization12.png)

We can sort the databank by Net Profit and we can see that the best input values in terms of maximum profit are EMAPeriod =  9 and EMAPeriod2 = 40.

***Note!***

*You can see certain results having an exclamation mark shown next to the strategy title. This means for given parameters set the optimizer found the strategy behaving oddly and it needs to be reviewed manually in more detail.*

**Interpreting the results**

Now we’ve got input parameters that were optimized for our given symbol and timeframe.

What we’ve really done is we found out what worked best in the past. We have to be very careful because the parameters might be ideal for the history data,

but there is no guarantee that what worked best on history data will work also in the future.

It is called curve fitting – usually the more parameters the strategy has, the bigger is the danger of curve fitting.

There are two approaches to curve fitting:

- make sure the strategy is robust and not optimizing its values at all
- make sure the strategy benefits from periodic reoptimization

So the question is – will periodical reoptimization improve the results of my strategy?

If yes, how often should I do it?

StrategyQuant can give you answers to these questions using another of its advanced functionality – [Walk-Forward Optimization](simple-optimization.md) and Walk-Forward Matrix.
