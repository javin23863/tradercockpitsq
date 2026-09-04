# Recommended optimization parameters

- Source: <https://strategyquant.com/doc/strategyquant/recommended-optimization-parameters/>
- Section: StrategyQuant X › Optimization
- Fetched: 2026-09-04

---

### What are recommended parameters?

You can note that when choosing categories of parameters for optimization the default choice is **Recommended parameters**. This is also the only choice in optimizations used in cross checks.

These are the parameters that “make sense” to be optimized – indicator periods, entry multiplier and used exit parameters such as SL/PT pips / multipliers.

All the other possible parameters are ignored. The reason is simple – virtually every block used in the strategy has multiple possible values. If you’d optimize every one of them you’ll just highly increase risk of curve fitting or over-optimizing your strategy to historical data.

It is highly recommended to keep the number of possible strategy parameters as low as possible – the lower the better.

The lower number of optimizable parameters the strategy has, the more robust and more resistent to curve fitting it will be.

[![](https://strategyquant.com/wp-content/uploads/2019/11/recommended_parameters.png)](https://strategyquant.com/wp-content/uploads/2019/11/recommended_parameters.png)
